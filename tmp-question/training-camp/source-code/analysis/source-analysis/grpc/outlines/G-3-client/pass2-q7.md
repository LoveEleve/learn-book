# 闭环笔记 Q7 — DelayedClientCall: 缓冲期语义冻结 + 一次放行

假设: 就绪前的调用被 DelayedClientCall 缓冲: 请求操作排队, 回调被 DelayedListener 暂存, 真实 call 注入后一次性放行 (passThrough 永不回头)。

验证过程:
- **start** (DelayedClientCall.java:206-230): listener 记录 (L210); 未放行 → **DelayedListener 包装** (L217, 缓冲 onMessage/onClose 等回调) + startHeaders 暂存 (L218); 已有 error (cancel 过) → `CloseListenerRunnable` 立即失败 (L224-226)
- **cancel** (L232-260): realCall 未建 → `setRealCall(NOOP_CALL)` (L246-247) + error 记录 (L255) + 回调; 已建 → delayOrExecute 转发 (L262-267)
- **delayOrExecute** (L270-278): `!passThrough → pendingRunnables.add` — 排队保序; 放行后直通
- **drainPendingCalls** (L300-320): 断言 realCall 非空 → 循环: 锁内取走 pending 批 → 锁外执行 → **重查队列** ("new Runnables may be added after we drop the lock", L317-319) → 空则 `passThrough = true; pendingRunnables = null` (L307-309) — **一次性切换, 永不回头**
- 放行后: DelayedListener 缓冲的回调按序投递 → 后续直通 realCall
- 测试: DelayedClientCallTest 存在

代码类型: Implementation (缓冲状态机)

结论: 缓冲设计 = **语义冻结 + 原子放行**: 调用方在缓冲期内的所有操作 (start/sendMessage/cancel) 都被记录, 放行后按原序在 realCall 上重放; passThrough 标志原子切换保证"放行后无缓冲"; DelayedListener 保回调顺序。**被放弃的方案: 阻塞调用线程等就绪** — 阻塞浪费线程; 缓冲让调用异步无感。与 ManagedChannelImpl 的 PendingCall (q3) 是同一模式的两层 (Channel 层缓冲 → DelayedTransport 层缓冲)。 [跨域: q3 PendingCall 同模式] [并发: 锁+队列双保险] (DelayedClientCall.java:206-320)
