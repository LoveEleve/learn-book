# 闭环笔记 Q4 — 对冲机制: 定时开新流, 赢家 commit

假设: Hedging = 每 hedgingDelayNanos 开一个并行流, 先完成者 commit, 其余取消; 节流器冻结对冲。

验证过程:
- **HedgingRunnable** (RetriableStream.java:463-520): run → `createSubstream(state.hedgingAttemptCount, false, true)` (L477, 新对冲流) → 锁内检查 `scheduledHedgingRef.isCancelled()` (L484, 已 commit 则取消) → `state.addActiveHedge(newSubstream)` (L489) → **hasPotentialHedging** (L811-819: winningSubstream==null && hedgingAttemptCount < maxAttempts && !hedgingFrozen) && 节流器阈值 → 调度下一个 HedgingRunnable (L512-517, `hedgingPolicy.hedgingDelayNanos`)
- **取消路径**: commit 后未取消的对冲流 → `newSubstream.stream.cancel(CANCELLED "Unneeded hedging")` (L509)
- **drain(newSubstream)** (L519): 新流立即排空缓冲消息 (q2 缓冲重放)
- **freezeHedging** (L823-835): 节流器冻结 → 取消未来调度
- 字段: scheduledHedging (FutureCanceller, L122)

代码类型: Implementation (并行抢先协议)

结论: 对冲 = **定时并行 + 抢先定案**: 每个 hedgingDelayNanos 开新流, 赢家 (先完成) commit 取消其余; hasPotentialHedging 是"还能不能开新流"的守卫 (attempt 上限/已冻结); throttle 冻结防对冲风暴。**被放弃的方案: 一次性全开 N 流** — 流量放大 N 倍; 定时错开让"慢请求"才有对冲价值。与 Retry (串行) 形成两种故障策略: 重试等失败, 对冲赌延迟。 [跨域: q1 commit 协议 / G-2 服务端限流响应] [并发: 定时调度 + 锁] (RetriableStream.java:463-520,811-835)
