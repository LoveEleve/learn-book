# D-1 连接池核心 — Lock+Condition 阻塞模型 (借/还/init/shutdown)

> 前置: [[C-11]] (jdbc DataSource) | 复用: [[H-1-hikaricp]] [[H-2-concurrentbag]] [[H-3-acquire]] [[H-4-close]] (两极对照) | 引出: [[D-2-filter-chain]]
> 🔴 Deep | 9 KP | [模式: 门面 + 条件变量 + 组合]
> Pass 2 闭环: q1(createDirect CAS) q2(限流) q3(rollback 顺序) q4(signal 协议) q5(init 双路径) q6(useLocalSessionState) q7(事务跟踪) q8(建连翻译)

**读者处境**: 高峰期 `getConnection()` 卡住时池里在发生什么?`conn.close()` 后脏事务和脏状态怎么处理?为什么 Hikari 无锁而 Druid 敢用一把锁?这篇从"仓库结构 → 借出 → 归还 → 收尾"走完池的生命周期 — 每个设计点都对照 Hikari 讲取舍。

### 1. 架构总览 — 固定数组 + 单锁 + 双 Condition

场景: 池的"仓库"长什么样?多个线程同时借还会不会乱?

源码路径:
- `DruidAbstractDataSource.java:242,243,244,299` — **并发模型**: `ReentrantLock lock`(L242)+`Condition notEmpty/empty`(L243-244), `notEmpty = lock.newCondition()`(L299) — 一把锁护住整个池状态
- `DruidDataSource.java:772` — **存储**: `connections = new DruidConnectionHolder[maxActive]`(L772) — 固定数组+poolingCount 尾指针, 借取尾/还放尾
- `DruidAbstractDataSource.java:68,69,76` — **默认值族**: `DEFAULT_MAX_ACTIVE_SIZE = 8`(L69)/`DEFAULT_WHILE_IDLE = true`(L76, **testWhileIdle 出厂即开**)/周期 60s/驱逐 30min/7h
- `DruidAbstractDataSource.java:1745,1772` — **建连翻译**: `createPhysicalConnection()`(L1704): connectTimeout 按库翻译成驱动属性 — MySQL `connectTimeout`(L1746)/Oracle `oracle.net.CONNECT_TIMEOUT`(L1750)/PG `loginTimeout`(秒, L1755)/SQLServer(L1762); socketTimeout 同理(Oracle 双属性兼容新旧驱动 L1773-1777)
- `DruidDataSource.java:659,779,807,812` — **启动**: `init()`(L659): 校验→数组→初建→三线程(L807)→`initedLatch.await()`(L812)→registerMbean

关键设计: **Why 数组+锁 vs Hikari ConcurrentBag？** 锁让 poolingCount/activeCount/数组三状态原子一致, Condition 内建"池空挂起"语义; 代价高峰锁竞争, 收益状态简单可靠(H-2 对照)。**Why init 双路径？**(闭环 q5): asyncInit+scheduler 并行建连快速返回, 否则同步 while 建到 initialSize; **initedLatch 等的是"线程已启动"非"连接已建完"**(AsyncInitTest 轮询证明)。**Why 池层翻译驱动属性？**(闭环 q8): 用户只配 Druid 的 connectTimeout, 池层按库翻译成 5 种属性名+单位差异 — 屏蔽驱动差异。[模式: 门面 + 条件变量]

数据流: new DruidDataSource() → init(659) → 校验 → 数组(772) → 同步/异步初建(779-805) → 三线程(807) → await latch(812) → 建连时: 翻译超时属性(1745) → 物理连接。

### 2. 借出 — getConnection → getConnectionInternal → pollLast

场景: 池有货直接取; 没货怎么办?等多久?谁来建?

源码路径:
- `DruidDataSource.java:1332,1345,1352` — **入口**: `getConnection()`(L1332)→filter? `createChain().dataSource_connect`(L1345, D-2) : `getConnectionDirect`(L1352)
- `DruidDataSource.java:1565,1569,1583,1596` — **createDirect**: 池空且 scheduler 排队 → 等待线程 CAS `creatingCountUpdater 0→1`(L1569) 自建; 建完锁内复查 `activeCount+poolingCount < maxActive`(L1583), 池满 `JdbcUtils.close` 丢弃白建(L1596)
- `DruidDataSource.java:1613` — **限流**: `maxWaitThreadCount>0 && notEmptyWaitThreadCount > maxWaitThreadCount` → 抛异常(L1613-1618)
- `DruidDataSource.java:2214,2218,2245` — **阻塞取**: `takeLast`(无限)/`pollLast`(超时): `notEmpty.await(estimate)`(L2245-2247)→取尾(L2271)→discard 重试(L1678-1685)
- `DruidDataSource.java:1707,1746,1768` — **结尾**: 超时抛富异常(active/maxActive/creating+运行中 SQL L1746-1757)→`incrementUseCount + new DruidPooledConnection`(L1768-1770)

关键设计: **Why createDirect？**(闭环 q1): 自建省掉"提交→调度→完成"两次切换; CAS 防并发超建, 锁内复查防超 maxActive, 白建即关。**Why 限流拒绝？**(闭环 q2): 等待者超阈值=池过载, 新请求再排恶化 — 快速失败比堆积好; 自建→限流→阻塞三级防御递进。[模式: 快路径旁路 + 熔断]

数据流: getConnection(1332) → getConnectionDirect(1352) → getConnectionInternal(1543) → 池空+scheduler 排队: CAS 自建(1569)→池满复查(1583) → 否则 pollLast(2218) → await(2245) → 取尾(2271) → discard 重试(1678) → 超时: 富异常(1707) → 成功: new DruidPooledConnection(1770)。

### 3. 归还与事务 — close → recycle → putLast

场景: 业务 `conn.close()` 后连接怎么"洗干净"?用户侧的事务操作如何被池跟踪?

源码路径:
- `DruidPooledConnection.java:236,271,281,343` — **入口**: `close()`(L236)→filter? `dataSource_recycle`(L276, D-2) : `recycle()`(L281)→`holder.dataSource.recycle(this)`(L343)
- `DruidDataSource.java:1938,1943,2029` — **核心**: `recycle()`(L1894): **先 rollback 未提交事务**(L1938-1940)→`holder.reset()` 四态(L1943)→useCount/密码版本检查(L1959)→testOnReturn(L1979)→`putLast`(L2029)
- `DruidConnectionHolder.java:371` — **复位**: `reset()`(L371-418): readOnly/Holdability/isolation/autoCommit 四态+清监听+关 statementTrace
- `DruidPooledConnection.java:720,725` — **会话优化**: `setAutoCommit`(L720): `useLocalSessionState`(默认 true) 且本地状态一致 → **直接返回不发 SQL**(L725-727)
- `DruidPooledConnection.java:745,792,844` — **事务跟踪**: `transactionRecord`(L745): 非 autoCommit 首次 SQL 创建 transactionInfo+计数; `rollback()`(L792): **无事务直接跳过**(L793-794, 不发 SQL); `handleEndTransaction`(L844): 耗时直方图+日志
- `DruidDataSource.java:2029,2031,2039` — **放回**: `putLast`(L2029): 尾插+`notEmpty.signal()`(L2208) — 一次放回精确唤醒一个; **失败边界**: putLast 返回 false(池满/discard/已关 L2195)→`JdbcUtils.close(holder.conn)`+日志 "pool is full"(L2031-2044)

关键设计: **Why rollback 先于 reset？**(闭环 q3): rollback=数据正确性(脏事务不落库), reset=复用正确性(干净状态) — 先 reset 会抹掉 rollback 需要的事务现场; readOnly 跳过 rollback 省一次往返。**Why 会话/事务优化？**(闭环 q6/q7): useLocalSessionState 让重复 setAutoCommit 不发 SQL(本地快照一致即跳过); 事务跟踪是"池推断式"(非 autoCommit 首次 SQL=起点), 无事务 rollback 零成本跳过 — 三处都是"无状态不发指令"哲学。[模式: 状态复位 + 精确信号 + 会话快照]

数据流: conn.close()(236) → recycle(281) → dataSource.recycle(1894) → 非 autoCommit 非 readOnly: rollback(1938) → reset 四态(371) → 超 useCount: discard → testOnReturn 失败: 丢弃 → putLast(2029) → notEmpty.signal(2208)。使用侧: setAutoCommit 一致即跳过(724) → 事务首 SQL 建 transactionInfo(746) → commit/rollback 结束事务记直方图(813-818)。

### 4. 收尾 — shutdown + 异常联动

场景: 应用停机或数据库故障, 池怎么优雅关闭/自愈?

源码路径:
- `DruidDataSource.java:2081,2098,2110,2136,2139,2147` — **关闭**: `close()`(L2081): interrupt 三线程(L2098-2108)→cancel futures(L2110-2116)→关池内 PSCache+物理连接(L2118-2134)→`unregisterMbean()`(L2136)→`enable=false + notEmpty.signalAll()`(L2138-2139)→`filter.destroy()`(L2147)
- `DruidDataSource.java:1773,1797,1807` — **异常联动**: `handleConnectionException`(L1773): 广播事件(L1791-1794)→`exceptionSorter.isExceptionFatal`(L1797)→`handleFatalError`(L1798, 记 fatalErrorCount+时间戳)→shrink 驱逐联动(D-5)

关键设计: **Why signalAll 而非 signal？**(闭环 q4): 关闭瞬间所有等待者必须退出 — 逐个 signal 会饿死队列; signalAll 全唤醒, 各自检查 enable(L2255-2262) 抛 DataSourceDisableException。三处唤醒协议完整: 归还 signal(精确配对)/关闭 signalAll(广播退出)/中断传播 signal(L2266, 不让线程卡死)。**Why 异常联动？** 故障时不能再借坏连接 — 借出检查 onFatalError(L1620)+shrink 驱逐(D-5), 隔离闭环。[模式: 广播唤醒 + 故障隔离]

数据流: close(2081) → interrupt 三线程(2098-2108) → cancel futures(2110-2116) → 关连接(2118-2134) → unregisterMbean(2136) → enable=false+signalAll(2138-2139) → 等待者苏醒抛异常 → filter.destroy(2147)。异常侧: SQLException(1773) → isExceptionFatal(1797) → handleFatalError(1798) → 借出受限(1620) + shrink 驱逐(D-5)。

→ 引出 D-2: Filter 拦截链 — 借/还入口的 filter 分支 (createChain→dataSource_connect/recycle), 递归链机制+代理对象模型。
