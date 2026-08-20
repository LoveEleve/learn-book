# D-5 维护体系 — shrink 四阶段 + 创建/销毁线程 + removeAbandoned

> 前置: [[D-1-core-architecture]] (数组+Condition) | 复用: [[D-7-validation]] (validateConnection) | 对照: [[H-6-housekeeper]] | 引出: [[D-8-pscache]]
> 🔴 Deep | 8 KP | [模式: 定时器 + 批量回收 + 兜底回收]
> Pass 2 闭环: q1(两级驱逐边界) q2(keepAlive 回池细节) q3(fatalError 隔离) q4(固定间隔重试) q5(running 跟踪) q6(emptyWait 语义)

**读者处境**: `timeBetweenEvictionRunsMillis`、`minEvictableIdleTimeMillis`、`maxEvictableIdleTimeMillis` 三个时间配置 — 每个管什么?数据库故障一次, 池怎么自动"隔离污染"又自动"恢复"?removeAbandoned 为什么文档警告别在生产开?这篇拆开 shrink 算法和后台线程骨架。

### 1. 线程骨架 — 谁在驱动维护

场景: 池就绪后, 谁周期性地检查连接?谁负责补建?

源码路径:
- `DruidDataSource.java:2724,2769` — **Creator**: `CreateConnectionThread`(L2724): `empty.await()`(L2769) — 按需创建
- `DruidDataSource.java:2846,2869,2878` — **Destroyer**: `DestroyConnectionThread`(L2846): `sleep(timeBetweenEvictionRunsMillis)`(L2869)→`destroyTask.run()`(L2878) — 周期驱动
- `DruidDataSource.java:2887,2893,2895` — **DestroyTask**: `shrink(true, keepAlive)`(L2893)+`removeAbandoned()`(L2895, 开启时)
- `DruidDataSource.java:2560,2643` — **并行创建**: `CreateConnectionTask`(L2560): scheduler 任务; 错误重试 `schedule(this, timeBetweenConnectErrorMillis)`(L2643)
- `DruidDataSource.java:2902` — **LogStatsThread**: 周期 logStats(L2912), 可选

关键设计: **Why 两个创建通道？** 常驻线程(await 按需) vs scheduler 并行任务 — 有 createScheduler 用并行扩池(更快), 无则单线程省资源。**Why 固定间隔重试？**(闭环 q4): errorCount > connectionErrorRetryAttempts(默认 1) 后 `setFailContinuous`+固定间隔(默认 500ms)重排 — 建连失败恢复周期秒级, 指数退避收益低, failFast 让等待者快速失败。[模式: 定时器 + 双通道创建]

数据流: init(D-1) → 三线程(L807-809) → Destroyer: sleep(周期) → destroyTask.run(2878) → shrink(2893) + removeAbandoned(2895) → Creator: 借出 emptySignal → empty.await 唤醒 → 建连 → put。

### 2. shrink 核心 — 四阶段与两级驱逐边界

场景: 每次 shrink, 池内连接怎么被分类处置?min/maxEvictable 怎么分工?

源码路径:
- `DruidDataSource.java:3069,3084` — **①fatalError 增量**: `fatalErrorCount - fatalErrorCountLastShrink`(L3084)
- `DruidDataSource.java:3100` — **②故障隔离**: `(onFatalError || 增量>0) && lastFatalErrorTimeMillis > connectTimeMillis` → keepAlive 列表(L3100-3103) — **故障前建的老连接不信任, 重新验证**
- `DruidDataSource.java:3092,3121` — **③两级驱逐**: `checkCount = poolingCount - minIdle`(L3092): `i < checkCount`(超额区) 且 idle>=minEvictable → 逐(L3121-3123); `i >= checkCount`(保底区) 且 idle>maxEvictable → 逐(L3125-3127) — **minEvictable 管收缩, maxEvictable 管老化**
- `DruidDataSource.java:3131,3151` — **④紧凑**: keepAlive 收集(L3131-3133)+`System.arraycopy` 保留段前移+nullConnections 清尾(L3151-3162)
- `DruidDataSource.java:3172,3183` — **处理**: evict 段逐个 close(L3172-3181); keepAlive 段验证(L3183)

关键设计: **Why 两级边界？**(闭环 q1): 超额区连接按 minEvictable(短阈值)逐 — 池收缩到 minIdle 即止; 保底区连接只有超 maxEvictable(长阈值)才逐 — 防"长时间占用但池没满"的连接永久驻留。DruidDataSourceShrinkTest 实证: shrink(false,false) 后 poolingCount 收回 minIdle。**Why 老连接进保活而非直接驱逐？**(闭环 q3): 故障可能短暂, 连接未必坏 — "验证即过滤": 通过回池省重建, 失败 discard+补连; 故障隔离粒度是连接不是整个池。[模式: 批量回收 + 边界策略]

数据流: shrink 开始 → fatalErrorIncrement(3084) → 循环分类: fatalError 老连接→keepAlive(3100); phyTimeout→evict; idle 判断→evict/break(3121); keepAlive 条件→列表(3131) → arraycopy 紧凑(3151) → 锁外: evict 段 close(3172) → keepAlive 段验证(3183)。

### 3. keepAlive 回池与补连 — 保活闭环

场景: keepAlive 列表里的连接验证后去哪?池收缩后怎么补回 minIdle?

源码路径:
- `DruidDataSource.java:3183,3185,3192` — **验证**: 逆序遍历 keepAliveConnections(L3185)→`validateConnection`(L3192, D-7)
- `DruidDataSource.java:3204,3205` — **回池**: 成功: `lastKeepTimeMillis` 更新(L3204)+`put(holder, 0L, true)`(L3205, **checkExists 防重复**); 失败: 关闭+discard(L3211-3233)
- `DruidDataSource.java:2514,2526,2547` — **put 细节**: `checkExists` 遍历防同 holder 双份入池(L2526-2531, 锁外验证期间的并发放回); 放回后 `poolingCount+createTaskCount < notEmptyWaitThreadCount → emptySignal()`(L2547-2549, 归还侧补建钩子)
- `DruidDataSource.java:3252,3260` — **补连**: `needFill` → `emptySignal(fillCount)`(L3256); `fatalErrorIncrement>0` → `emptySignal()`(L3263)
- `DruidDataSource.java:3905,3909` — **emptySignal**: 无 scheduler → `empty.signal()`(L3908); 有 → submitCreateTask×fillCount(上限 maxCreateTaskCount)

关键设计: **Why 逆序 + checkExists？**(闭环 q2): 逆序回池让最新验证者靠近数组尾(借出取尾优先借刚验证的); checkExists=true 防"验证期间被并发放回"造成同 holder 双份入池 — 池容量虚高且可能借到同一连接两次。[模式: 保活闭环 + 唯一性保护]

数据流: keepAliveCount>0 → 逆序: validateConnection(3192) → 成功: lastKeepTime(3204)+put(checkExists)(3205) → 失败: 关+discard(3213) → needFill: emptySignal(fillCount)(3256) → Creator 唤醒建连 → 池回 minIdle。

### 4. removeAbandoned — 泄漏兜底回收

场景: 业务忘 close() 导致连接越借越少, 池怎么自救?为什么默认不开?

源码路径:
- `DruidDataSource.java:2925,2943,2949` — **扫描**: `removeAbandoned()`(L2925): `isRunning()` 跳过(L2943, 执行中不算泄漏)→`(now - connectedTimeNano) >= removeAbandonedTimeoutMillis`(L2949)→标记
- `DruidPooledConnection.java:1239,1249` — **running 联动**: `beforeExecute()/afterExecute()`(L1239/1249): **仅 removeAbandoned 开启时置位**(L1242/L1247, 零开销默认) — DruidPooledStatement 执行前后调用(DruidPooledStatement:298/315), 执行期间 running=true 防误杀慢查询(闭环 q5)
- `DruidDataSource.java:2971,2972` — **强收**: `JdbcUtils.close(pooledConnection)`(L2971)+`abandond()`(L2972)+计数
- `DruidDataSource.java:2976,2984` — **栈追踪**: `isLogAbandoned` → 打印借出点栈+ownerThread 当前栈(L2984-3001) — 定位泄漏者

关键设计: **Why 主动强收且默认关？** 泄漏不干预池会耗尽; 强收会中断可能的慢查询 — 误杀风险换来兜底能力, 故默认关+文档警告。**Why isRunning 跳过？** 正在执行 SQL 的连接不可能是"忘关", 跳过防误杀。[模式: 兜底回收 + 栈追踪]

数据流: removeAbandoned(2925) → activeConnections 遍历 → isRunning 跳过(2943) → 超时(2949): 移出集合 → disable 检查(2964) → close+abandond(2971-2972) → isLogAbandoned: 两段栈(2984-3001)。

→ 引出 D-8: PreparedStatementPool — shrink/close 时 statementPool 清理 (close L2121-2124 关池内 PSCache), LRU 缓存机制。
