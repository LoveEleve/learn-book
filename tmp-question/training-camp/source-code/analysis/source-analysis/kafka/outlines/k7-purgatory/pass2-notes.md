# K-7 闭环笔记 Q1-Q6: 状态机/时间轮/触发/衔接/对照

## Q1: DelayedOperation 状态机?

假设: completed 标志 + forceComplete 双检锁, 只完成一次。

验证过程:
- class DelayedOperation extends TimerTask (DelayedOperation.java:38), completed volatile (DelayedOperation.java:L41), ReentrantLock (DelayedOperation.java:L43)
- forceComplete (DelayedOperation.java:48-72): 已完成直接 false (DelayedOperation.java:L50-52) → 加锁 → 重检 (DelayedOperation.java:L56) → completed=true + cancel() 取消定时器 + onComplete() (DelayedOperation.java:L59-62) — **只完成一次, 并发只有首个线程成功**
- onExpiration (DelayedOperation.java:L93-96) / onComplete (DelayedOperation.java:L98-100) 抽象 — 子类定义 (DelayedProduce.scala:L119-134)

代码类型: Implementation (状态机)

结论: **DelayedOperation = volatile completed + 双检锁 forceComplete (DelayedOperation.java:41,48-72): 完成/过期二选一, cancel 取消定时, onComplete 恰好一次 — 并发安全的状态机**。DelayedOperation.java:38-100

## Q2: 分级时间轮?

假设: tickMs 递增的层级轮, 溢出升层, 桶按 delayQueue 驱动。

验证过程:
- TimingWheel (TimingWheel.java:97): tickMs/wheelSize/interval=tickMs*wheelSize (TimingWheel.java:L98-103) + buckets[] (TimingWheel.java:L103) + currentTimeMs (TimingWheel.java:L104)
- 分级: overflowWheel volatile (TimingWheel.java:L108, DCL 注释 TimingWheel.java:L106-107) + addOverflowWheel 升层 (TimingWheel.java:L131-141: tickMs=interval)
- add 三分支 (TimingWheel.java:L143-175): 已取消/已过期 false (TimingWheel.java:L146-151) / 本层 bucketId = virtualId % wheelSize (TimingWheel.java:L152-169) / 溢出 → overflowWheel.add (TimingWheel.java:L171-173)
- advanceClock (TimingWheel.java:L177-184): currentTimeMs 推进 + overflowWheel 级联 (TimingWheel.java:L182)
- SystemTimer 组装 (SystemTimer.java:58-61) + DelayQueue 驱动

代码类型: Algorithmic (时间轮)

结论: **分级时间轮 = tickMs 层级 (TimingWheel.java:98-103) + overflowWheel 升层 (TimingWheel.java:L108,131-141) + add 三分支 (TimingWheel.java:L143-175) + advanceClock 级联 (TimingWheel.java:L177-184) — O(1) 添加, 桶经 DelayQueue 驱动过期**。TimingWheel.java:97-184

## Q3: tryComplete 触发条件?

假设: acks=all → 逐分区 checkEnoughReplicasReachOffset, 全满足 forceComplete。

验证过程:
- DelayedProduce.tryComplete (DelayedProduce.scala:89-116): 逐分区 acksPending 检查 (DelayedOperation.java:L93) → partition.checkEnoughReplicasReachOffset(status.requiredOffset) (TimingWheel.java:L101) → 满足置 acksPending=false (TimingWheel.java:L106-108)
- 全部无 acksPending → forceComplete (DelayedProduce.scala:L112-113)
- checkEnoughReplicasReachOffset (Partition.scala:1089-1092, K-4 篇 4 已讲: ISR 副本是否到 requiredOffset)
- onExpiration (DelayedProduce.scala:L119-124) / onComplete 回调响应 (TimingWheel.java:L131-134)

代码类型: Implementation (触发条件)

结论: **acks=all 触发 = 逐分区 checkEnoughReplicasReachOffset (DelayedProduce.scala:101, K-4 实证 Partition.scala:1089) → 全满足 forceComplete (DelayedProduce.scala:L112) — 提交语义 (HW) 与延迟完成在此汇合**。DelayedProduce.scala:89-134

## Q4: 与 K-4 HW 衔接?

假设: HW 推进 → tryCompleteDelayedRequests → checkAndComplete 完成等待的请求。

验证过程:
- K-4: Partition.tryCompleteDelayedRequests (Partition.scala:826-827, 锁外) → delayedOperations.checkAndCompleteAll
- Purgatory.checkAndComplete (DelayedOperationPurgatory.java:153-165): watcherList(key) 分片 (DelayedOperationPurgatory.java:L154) → watchers.tryCompleteWatched (DelayedOperationPurgatory.java:L160) → 完成计数
- tryCompleteElseWatch 注册侧 (DelayedOperationPurgatory.java:L122-170): 先 safeTryCompleteOrElse → 未完成挂 watchKeys (DelayedOperationPurgatory.java:L156-158) + timeoutTimer (DelayedOperationPurgatory.java:L168) — 双保险: 条件触发或超时
- 死锁防护注释 (DelayedOperationPurgatory.java:L135-154): checkAndComplete 应无锁调用 (DelayedOperationPurgatory.java:L149-154)

代码类型: Glue (触发链)

结论: **触发链 = HW 推进 (K-4 Partition.scala:826) → checkAndComplete (DelayedOperationPurgatory.java:153) → tryCompleteWatched; 注册侧 tryCompleteElseWatch 双保险 (条件 DelayedOperationPurgatory.java:L156-158 + 超时 DelayedOperationPurgatory.java:L168) — 条件触发与超时二选一完成**。DelayedOperationPurgatory.java:122-170

## Q5: vs ScheduledThreadPoolExecutor?

假设: 时间轮 O(1) 添加 vs 优先队列 O(log n)。

验证过程:
- 时间轮: add 是桶内链表 + 桶经 DelayQueue (TimingWheel.java:143-169) — O(1) 添加, 过期由 DelayQueue 头驱动
- ScheduledThreadPoolExecutor: 延迟队列 (DelayedWorkQueue 堆) — 添加 O(log n), 每任务独立调度
- Kafka 场景: 大量短延迟任务 (acks=all 等待通常 <1s) — 批量桶共享队列, 减少调度开销
- 对比点: 时间轮 tick 粒度 (tickMs) 决定精度上限; 堆更精确但开销高

代码类型: 设计权衡分析

结论: **时间轮 vs 调度堆: O(1) 添加 + 桶批量 vs O(log n) + 逐任务; Kafka 选择时间轮因为延迟任务量大且精度要求不苛刻 (tickMs 粒度) — 与 Netty 同选择**。TimingWheel.java:143-169

## Q6: 与 Netty HashedWheelTimer 对照?

假设: 两者同构 (分级时间轮), 差异在驱动与精度。

验证过程:
- Netty HashedWheelTimer (netty 已交付): 同样 tickMs/wheelSize/溢出轮转 — 同构算法
- 差异: Kafka 用 DelayQueue 驱动桶过期 (TimingWheel.java:101,166) vs Netty worker 线程轮询推进; Kafka 有 DelayedOperation 状态机层 (Purgatory) vs Netty 纯定时器
- 场景差异: Netty 定时器面向 IO 超时; Kafka 时间轮只是 Purgatory 的超时兜底 (主路径是条件触发 tryComplete)

代码类型: 对照分析

结论: **Netty HashedWheelTimer 与 Kafka TimingWheel 同构 (分级时间轮); 差异: 驱动 (DelayQueue vs worker 轮询) 与定位 (纯定时器 vs 超时兜底, 主路径是条件触发)**。TimingWheel.java:97-184 + netty HashedWheelTimer 对照

跨域关联: K-4 (HW 触发链) / Netty (HashedWheelTimer 对照) / K-3 (DelayedFetch 等读)
