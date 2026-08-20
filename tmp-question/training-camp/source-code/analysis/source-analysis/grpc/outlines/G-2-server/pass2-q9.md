# 闭环笔记 Q9 — 停机语义: shutdown (优雅) vs shutdownNow (立即)

假设: shutdown 拒新连接等旧流完成; shutdownNow 立即取消全部活动调用 (UNAVAILABLE); 有回调完成通知与幂等防护。

验证过程:
- **beginShutdown** (ServerImpl.java:260-268): shutdown 标志 → `transportServer.shutdown()` — 传输层 GOAWAY 拒新 (q6 双 GOAWAY); 若从未 start, 直接标 terminated
- **shutdownNow** (L274-297): 先 `shutdown()` → `Status nowStatus = UNAVAILABLE "Server shutdownNow invoked"` (L277) → 遍历 transports 快照逐个 `transport.shutdownNow(nowStatus)` (L290) — **活动调用立即取消**
- **幂等防护**: `shutdownNowStatus != null → return` (L281-282, "prevents transports from needing to handle multiple shutdownNow invocations") — 只发一次
- **awaitTermination**: 与 checkForTermination 配合 — transports 全部 terminated 后通知
- ServerImplTest 实证: startStopImmediate (L305)/stopImmediate (L319)/shutdownNowAfterSlowShutdown (L419) — 停机期间慢调用的处理顺序
- 与 G-1 关联: 停机时活动调用收 UNAVAILABLE → 客户端 RetriableStream (G-6) 判定可重试状态码

代码类型: Implementation (生命周期状态机)

结论: 停机是两阶段: shutdown 先拒新流 (GOAWAY), shutdownNow 再取消存量; UNAVAILABLE 让客户端 (G-6 重试) 有机会换目标重放; 幂等短路 (shutdownNowStatus) 防多线程重复取消; **被放弃的方案: 停机即杀连接** — 优雅停机保护在途请求 (TCP 连接被服务端主动关闭前有 GOAWAY 通知), 这是分布式优雅下线的核心。 [跨域: UNAVAILABLE → G-6 RetriableStream 判定重试] (ServerImpl.java:260-297)
