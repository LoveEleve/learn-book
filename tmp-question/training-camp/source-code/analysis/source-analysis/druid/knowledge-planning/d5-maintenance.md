# D-5 维护体系 — shrink 四阶段 + 创建/销毁线程 + removeAbandoned

> 项目: Druid (JDBC 连接池) | 🔴 Deep / 1 篇 | shrink(~200行)+CreateConnectionThread+DestroyConnectionThread+CreateConnectionTask+DestroyTask+LogStatsThread+removeAbandoned (全部 DruidDataSource 内嵌类)
> 基线: DRUID-PLAN D-5 (维护体系) — 前置: **D-1(数组+Condition 已分析) + D-7(validateConnection 已分析)** — 展开维护算法; 对照 Hikari H-6(HouseKeeper 定时淘汰)

---

## §0.8

- 🔴 Deep，1篇 — 线程体系(**CreateConnectionThread[L2724]: empty.await 按需建连, notEmptyWaitThreadCount 阈值停止, errorCount>connectionErrorRetryAttempts→failContinuous+固定间隔休眠; DestroyConnectionThread[L2846]: sleep(timeBetweenEvictionRunsMillis)→destroyTask.run(); DestroyTask[L2887]: shrink(true,keepAlive)+removeAbandoned; LogStatsThread[L2902]: 周期 logStats**; CreateConnectionTask[L2560]: createScheduler 并行, 同阈值+initTask 例外, 错误后 schedule(this, timeBetweenConnectErrorMillis) 重排[L2643], 非指数退避) → **shrink 四阶段[L3069]**: ①fatalError 增量检测(fatalErrorCount-fatalErrorCountLastShrink[L3084], fatalError 后的老连接→keepAliveConnections[L3100-3103]) ②空闲驱逐(checkTime: phyTimeoutMillis 物理超时[L3106-3112]→idle<minEvictable 且 <keepAliveBetween 则 break[L3114-3119]→minEvictable 且 i<checkCount(池>minIdle 部分) 或 >maxEvictableIdle 驱逐[L3121-3129]) ③keepAlive 收集(idle>=keepAliveBetweenTimeMillis 且距 lastKeepTime 也够→keepAliveConnections[L3131-3133]) ④System.arraycopy 紧凑+nullConnections 清理[L3151-3162]→evictCount>0: JdbcUtils.close+destroyCount++[L3172-3181]→keepAliveCount>0: 逆序 validateConnection(调 D-7), 成功 put+lastKeepTimeMillis 更新, 失败 discard[L3183-3250]→needFill: emptySignal(fillCount)[L3252-3260] / fatalErrorIncrement>0: emptySignal()[L3260-3267]) → **removeAbandoned[L2925]**: activeConnections 超 removeAbandonedTimeoutMillis 强收(JdbcUtils.close+abandond+栈日志[L2976-3002])
- 设计模式: [模式: 定时器]—DestroyConnectionThread 周期驱动; [模式: 批量回收]—shrink 数组紧凑; [模式: 兜底回收]—removeAbandoned

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| DruidDataSource.java:2724,2765,2799 | 创建线程 | **CreateConnectionThread.run(L2736): emptyWait 判断→poolingCount>=notEmptyWaitThreadCount 且非 keepAlive 缺口→empty.await(L2769); 建连错误 errorCount++>connectionErrorRetryAttempts→failContinuous(L2801)+breakAfterAcquireFailure 判断→sleep(timeBetweenConnectErrorMillis)(L2816)** | High |
| DruidDataSource.java:2846,2878 | 销毁线程 | **DestroyConnectionThread.run(L2858): sleep(timeBetweenEvictionRunsMillis)(L2869)→destroyTask.run()(L2878)** | High |
| DruidDataSource.java:2887,2893 | 周期任务 | **DestroyTask.run(L2892): shrink(true, keepAlive)(L2893)→isRemoveAbandoned 则 removeAbandoned()(L2895-2897)** | High |
| DruidDataSource.java:2560,2621,2643 | 并行创建 | **CreateConnectionTask.runInternal: 同阈值+initTask 例外(L2579-2591); 错误 errorCount>connectionErrorRetryAttempts→createScheduler.schedule(this, timeBetweenConnectErrorMillis)(L2643)** | High |
| DruidDataSource.java:3069,3084,3100 | shrink① | **fatalError 增量: fatalErrorCount-fatalErrorCountLastShrink(L3084); (onFatalError||增量>0) 且连接建立早于 lastFatalErrorTimeMillis→keepAlive 列表(L3100-3103)** | High |
| DruidDataSource.java:3106,3114,3121 | shrink② | **驱逐: phyTimeoutMillis 物理超时(L3106-3112); idle<minEvictable 且 <keepAliveBetween→break(L3114-3119); idle>=minEvictable: i<checkCount(池超出 minIdle 部分) 或 >maxEvictableIdleTimeMillis→evict(L3121-3129)** | High |
| DruidDataSource.java:3131,3151,3172 | shrink③④ | **keepAlive 收集(L3131-3133)→arraycopy 紧凑+nullConnections 清理(L3151-3162)→evict 段 close(L3172-3181)** | High |
| DruidDataSource.java:3183,3205,3252 | keepAlive 段 | **逆序 validateConnection(L3192, D-7)→成功 put+lastKeepTime 更新(L3204-3205), 失败 discard→needFill: emptySignal(fillCount)(L3252-3260)/fatalErrorIncrement>0: emptySignal()(L3260-3267)** | High |
| DruidDataSource.java:2925,2949,2971 | 泄漏回收 | **removeAbandoned(L2925): isRunning 跳过(L2943)→超 removeAbandonedTimeoutMillis(L2949)→JdbcUtils.close+abandond(L2971-2972)+isLogAbandoned 栈日志(L2976-3002)** | High |
| DruidDataSource.java:3905,3909 | 补连信号 | **emptySignal()(L3905): 无 scheduler→empty.signal(); 有→submitCreateTask(fillCount 次, 上限 maxCreateTaskCount)(L3909-3929)** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 维护体系是一个完整后台子系统 — 1篇 (~60行) 按"线程骨架→shrink 算法→keepAlive 与创建→泄漏回收"展开; 验证细节引用 D-7(已分析), 借还接口引用 D-1(已分析)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 后台线程骨架 (Creator/Destroy/LogStats+Task) | 🔴 | **为什么🔴**: 谁驱动维护 |
| P1-2 | shrink 驱逐算法 (checkCount/minEvictable/maxEvictable) | 🔴 | **为什么🔴**: 池收缩核心 |
| P1-3 | fatalError 增量驱逐 | 🔴 | **为什么🔴**: 故障隔离 |
| P1-4 | keepAlive 验证段 (validateConnection+put) | 🔴 | **为什么🔴**: 空闲保活 |
| P1-5 | removeAbandoned 泄漏回收 | 🔴 | **为什么🔴**: 连接泄漏兜底 |
| P2-1 | CreateConnectionTask 并行创建+重试 | 🟡 | **为什么🟡**: 并行扩池 |
| P2-2 | emptySignal 补连信号双路径 | 🟡 | **为什么🟡**: 唤醒策略 |
| P3-1 | 与 Hikari H-6 HouseKeeper 对照 | 🟢 | **为什么🟢**: 两种维护哲学 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **线程骨架** | 🔴 | 驱动层 |
| B | **shrink 四阶段** | 🔴 | 核心算法 |
| C | **keepAlive+创建+补连** | 🔴 | 保活与扩容 |
| D | **removeAbandoned** | 🔴 | 兜底回收 |

> **Cluster A (§1)**: 三线程+两 Task+emptySignal
> **Cluster B (§2)**: shrink ①fatalError ②驱逐 ③keepAlive 收集 ④紧凑
> **Cluster C (§3)**: keepAlive 验证段+CreateConnectionTask+emptySignal 补连
> **Cluster D (§4)**: removeAbandoned 超时回收+栈追踪

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 两级驱逐边界 | checkCount=poolingCount-minIdle: 超额区按 minEvictable 逐(管收缩), 保底区按 maxEvictable 逐(管老化) | DruidDataSource.java:3092, 3114-3129 |
| q2 | keepAlive 回池 | 逆序 put 让最新验证者靠尾(借出优先); put 第三参=checkExists 防锁外并发放回的双份入池; put 内部有归还侧补建钩子 | L2514-2553, 3183-3205 |
| q3 | fatalError 隔离 | 故障前老连接进 keepAlive 而非驱逐 — "验证即过滤": 恢复省重建, 仍坏则 discard+补连 | L3084-3103, 3183-3267 |
| q4 | 固定间隔重试 | errorCount>connectionErrorRetryAttempts(默认 1) 后固定间隔(默认 500ms)重排+failContinuous — 非指数退避(基线文档同误) | L2620-2657, Abstract:78,162,173 |
| q5 | running 跟踪 | **removeAbandoned 开启才置位**(零开销默认): DruidPooledStatement 执行前后调 beforeExecute/afterExecute(L298/315) 置 running=true/false — removeAbandoned 的 isRunning 跳过=执行中连接不回收(防误杀慢查询) | DruidPooledConnection.java:1239-1250, DruidPooledStatement.java:298,315 |
| q6 | emptyWait 语义 | CreateConnectionThread 建连前判 emptyWait(createError==null‖池非空‖discard 变化才等) — **建连失败后不再盲目等, 有 discard 变化或池空需求才继续建**; asyncInit 且未到 initialSize 时不等待 | DruidDataSource.java:2754-2771 |

→ 引出 D-8: PreparedStatementPool — shrink 驱逐的 PSCache 清理关联 (close 时 statementPool 关闭), LRU 缓存机制
