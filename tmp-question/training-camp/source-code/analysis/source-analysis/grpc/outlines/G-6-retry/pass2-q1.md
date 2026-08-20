# 闭环笔记 Q1 — 双模式互斥与 commit 赢家协议

假设: RetriableStream 是 Retry/Hedging 的统一实现, 两者互斥; commit 是"赢家定案"协议 — 取消所有其他尝试。

验证过程:
- **互斥构造** (RetriableStream.java:146-148): `"Should not provide both retryPolicy and hedgingPolicy"` — 一个流只能一种模式; isHedging = hedgingPolicy != null
- **commit** (L153-230): 锁内 `if (state.winningSubstream != null) return null` (L158-160, 只允许一次) → `savedDrainedSubstreams` (L162, 已发消息的子流集合) → `state.committed(winningSubstream)` (L166) → 释放 perRpcBufferUsed (L168) → **取消已调度 retry/hedging future** (L173-188) → **CommitTask: 取消其他非赢家 substream** — `substream.stream.cancel(CANCELLED_BECAUSE_COMMITTED)` (L191, 常量定义 L64, 注释: "For hedging only, not needed for normal retry")
- 运行约束: postCommit 必须在 callExecutor 跑 (L227-229, "must be run on the callExecutor to prevent deadlocks between multiple stream transports")

代码类型: Implementation (并发协议)

结论: 双模式统一在 RetriableStream: **Retry = 串行重试** (失败后重放), **Hedging = 并行抢先** (定时开新流, 先完成者 commit); commit 是原子的"定案" — 取消所有竞争流 (CANCELLED_BECAUSE_COMMITTED) 与未执行的定时器, 保证"恰好一个结果到达用户"。**被放弃的方案: 两套独立实现** — 缓冲/commit/取消逻辑高度重叠; 统一实现让透明重试语义一致。 [跨域: G-3 ClientCallImpl 内嵌; 取消码 CANCELLED 语义] [并发: 锁 + callExecutor 防死锁] (RetriableStream.java:146-230)
