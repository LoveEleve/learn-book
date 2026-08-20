# 闭环笔记 Q2 — blocking V1/V2 两代: Iterator 泄漏 → BlockingClientCall

假设: 1.83 的 blocking V2 (BlockingClientCall) 是为了修复 V1 Iterator API 的流泄漏与缺乏控制的问题。

验证过程:
- grep `blockingUnaryCall` → 两种重载: (call, req) 版本 `getUnchecked(futureUnaryCall(call, req))` (L140-142); (channel, method, callOptions, req) 版本 **ThreadlessExecutor + STUB_TYPE_OPTION + futureUnaryCall + waitAndDrain 循环** (L155-183) — 阻塞调用不占线程: 回调在调用线程执行; 中断时 `call.cancel("Thread interrupted", e)` 后**等待 onClose** (L170, 拦截器清理)
- grep `blockingV2UnaryCall` → 仅把 StatusRuntimeException 转 checked StatusException (L192-200) — **V2 语义 = 受检异常 API**
- grep `blockingServerStreamingCall` 警告 → "Warning: the iterator can result in leaks if not completely consumed" (L224) — V1 Iterator 的流泄漏问题实证
- grep `blockingBidiStreamingCall` → ThreadSafeThreadlessExecutor + `new BlockingClientCall<>(call, executor)` + start + request(1) (L300-312) — V2 返回可读/写/取消的 call 对象
- grep `@ExperimentalApi` → `@ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")` (L247) — V2 演进有 issue 追踪
- grep `STUB_TYPE_OPTION` → blocking 调用设置 `STUB_TYPE_OPTION, StubType.BLOCKING` (L160) — 供拦截器识别调用类型

代码类型: Implementation (阻塞语义实现)

结论: V1 三问题: ① Iterator 未消费完 → 流泄漏 (L224 自述) ② 无法 cancel ③ 无双向读写能力。V2 BlockingClientCall 对象语义 (read/write/cancel/hasNext, BlockingClientCall.java:63) + ThreadlessExecutor 不占线程 + 中断安全 (等 onClose)。unary V2 只改异常类型 (checked StatusException), 是 API 风格演进 (L192-200)。 (ClientCalls.java:140-142,155-183,224,247-312)
