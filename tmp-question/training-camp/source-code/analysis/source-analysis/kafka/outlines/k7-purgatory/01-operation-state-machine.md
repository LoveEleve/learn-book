# K-7 Purgatory 篇 1/2 — 等一等: 延迟操作状态机与触发

> 前置: [[K-4-isr-04]] (HW 推进) [[K-4-isr-01]] (ISR ack) | 复用: — | 对照: [[E-3-translog]] (等待机制对照) | 引出: [[K-7-purgatory-02]]
> 🟡 B | 来源: DelayedOperation.java:38-100 + DelayedOperationPurgatory.java:38-170 + DelayedProduce.scala:89-134
> 定位: K-7 卷开篇 — 回答"acks=all 怎么等? 延迟操作怎么管理?"

**读者处境**: 面试官问 "acks=all 时请求怎么等 ISR 同步? 用线程阻塞?" 你答 "延迟操作" — 但再问 "状态机什么样? 怎么注册? 怎么触发? 死锁怎么防?" 你答不上来。这篇是延迟操作机制的完整答案。

### 1. 问题引入 — 请求不能立即完成

场景: producer 发 acks=all, ISR 副本还没同步完 — **不做线程阻塞** (吞吐杀手), 也不能丢 — 怎么办?
- 延迟操作: 挂起 → 条件满足/超时 → 完成 (DelayedOperation.java:38)
- 本篇问题: 状态机 (Q1) / 注册触发 (Q3/Q4) / 应用 (DelayedProduce)

### 2. 状态机 — 只完成一次

场景: 延迟操作的生命周期?
- DelayedOperation extends TimerTask (DelayedOperation.java:38) + completed volatile (DelayedOperation.java:L41)
- forceComplete 双检锁 (DelayedOperation.java:L48-72): 已完成直接拒绝 (DelayedOperation.java:L50) → 加锁重检 (DelayedOperation.java:L56) → completed + cancel 取消定时 + onComplete (DelayedOperation.java:L59-62) — **并发只有首个线程成功**
- onExpiration/onComplete 抽象 (DelayedOperation.java:L93-100) — 子类定义行为

### 3. 注册与触发 — tryCompleteElseWatch

场景: 请求怎么挂起, 怎么被唤醒?
- tryCompleteElseWatch (DelayedOperationPurgatory.java:122-170): 先尝试完成 (safeTryCompleteOrElse) → 未完成挂 watchKeys (DelayedOperationPurgatory.java:L156-158) + 超时定时器 (DelayedOperationPurgatory.java:L168) — **条件触发与超时双保险**
- checkAndComplete (DelayedOperationPurgatory.java:L153-165): 条件满足时 (如 HW 推进) 唤醒
- 死锁防护 (DelayedOperationPurgatory.java:L135-154 注释): 注册持锁顺序 + checkAndComplete 无锁调用建议
- 指标: estimatedTotalOperations (DelayedOperationPurgatory.java:46-47) 监控挂起操作量
- 线程面: ExpiredOperationReaper 后台线程循环检查过期 (DelayedOperationPurgatory.java:91,101,409) — 超时兜底的第二执行者
- watch 分片 SHARDS=512 (DelayedOperation.java:L41) — 减锁竞争

### 4. 应用 — DelayedProduce 的 acks=all

场景: 具体怎么用?
- DelayedProduce.tryComplete (DelayedProduce.scala:89-116): 逐分区 checkEnoughReplicasReachOffset (DelayedProduce.scala:L101, K-4) → 全满足 forceComplete (DelayedProduce.scala:L112)
- 触发链: HW 推进 (K-4 Partition.scala:826 tryCompleteDelayedRequests) → checkAndComplete → 完成回调 (TimingWheel.java:L131-134)

### 核心悬念
"acks=all 的请求怎么等? 阻塞线程?" — 不阻塞: DelayedOperation 挂到 Purgatory (条件 watch + 超时定时双保险), ISR 同步完成 (HW 推进) 或超时任一先到即完成 — 请求线程立即释放, 吞吐不因等待下降。

### 概念依赖链
Q1 状态机 → Q3 触发 → Q4 衔接 → (02 篇: 时间轮+对照)

### 源码锚点清单
- DelayedOperation.java:38 (类) / 41 (completed) / 48-72 (forceComplete 双检锁) / 59-62 (完成+cancel+onComplete) / 93-100 (抽象)
- DelayedOperationPurgatory.java:38 (类) / 41 (SHARDS=512) / 122-170 (tryCompleteElseWatch) / 135-154 (死锁注释) / 153-165 (checkAndComplete) / 156-158 (挂 watch) / 168 (超时定时)
- DelayedProduce.scala:57 (类) / 89-116 (tryComplete) / 101 (checkEnoughReplicasReachOffset) / 112 (forceComplete) / 119-134 (onExpiration/onComplete)
- Partition.scala:826-827 (tryCompleteDelayedRequests, K-4)
