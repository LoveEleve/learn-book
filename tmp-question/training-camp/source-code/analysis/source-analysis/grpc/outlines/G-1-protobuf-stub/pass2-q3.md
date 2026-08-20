# 闭环笔记 Q3 — 适配器矩阵: StreamObserver ↔ ClientCall.Listener 双向桥

假设: ClientCalls 用两个适配器完成用户 StreamObserver 与底层 ClientCall.Listener 的双向转换, 并内置状态机防护 (冻结/多响应检测)。

验证过程:
- grep `CallToStreamObserverAdapter` → `extends ClientCallStreamObserver<ReqT>` (L443) — 用户写方向: onNext→`call.sendMessage(value)` (L468), onError→`call.cancel(...)` (L473), onCompleted→`call.halfClose()` (L479)
- **frozen 状态机** (L460-462): `freeze()` 在 start 时调用 (L553) — start 后 setOnReadyHandler/disableAutoRequestWithInitial 抛 IllegalStateException (L490-492, L504-507), 提示 "Use ClientResponseObserver" — 配置窗口只在 beforeStart
- **ClientResponseObserver.beforeStart 钩子** (L547-552): 用户可在 start 前拿到 adapter 配置流控
- **unary 流控特例**: `if (!streamingResponse && count == 1) call.request(2)` (L515-518) — "Initially ask for two responses from flow-control so that if a misbehaving server sends more than one response, we can catch it" — unary 请求 2 个响应额度来抓违规服务器
- grep `StreamObserverToCallListenerAdapter` → `extends StartableListener<RespT>` (L535) — 响应方向: onMessage→observer.onNext; **firstResponseReceived 多响应检测** (L562): `if (firstResponseReceived && !adapter.streamingResponse) throw Status.INTERNAL` — unary 收第二个消息直接抛 INTERNAL
- StartableListener (L439): onStart 钩子 (start 回调)
- 另一面: GrpcFuture (L648) 用 UnaryStreamToFuture (L603) 收集单响应 → Future; BlockingResponseStream (L684) 阻塞队列 → Iterator

代码类型: Glue + Implementation (桥接 + 状态机)

结论: 适配器不是简单转发 — 每个都内嵌防护状态机: 冻结窗口 (防 start 后改配置)、unary 双响应检测 (防服务器违规)、aborted/completed 检查 (防流终止后继续写, L466-467)。双响应额度 (request(2)) 是 gRPC 对 "unary = 恰好一条响应" 协议不变式的流量控制级执行。 (ClientCalls.java:443-553,515-518,562)
