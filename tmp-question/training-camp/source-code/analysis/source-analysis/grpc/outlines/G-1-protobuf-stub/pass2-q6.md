# 闭环笔记 Q6 — ServerCalls 服务端分派: 延迟调用 + 对称防护

假设: 服务端分派的核心设计是"onHalfClose 才调用业务方法" + 与客户端完全对称的双响应防护。

验证过程:
- grep `asyncUnaryCall` → UnaryServerCallHandler (ServerCalls.java:49-52, L112)
- **startCall** (L125-136): `call.request(2)` — 注释实证: "We expect only 1 request, but we ask for 2 requests here so that if a misbehaving client sends more than 1 requests, ServerCall will catch it" (L131-133) — **与客户端 ClientCalls 的 request(2) 完全对称** (q3)
- **延迟调用** (L164-167): onMessage 只存 request, **onHalfClose 才 method.invoke(request, responseObserver)** (L182) — 注释: "We delay calling method.invoke() until onHalfClose() to make sure the client half-closes" — 保证服务端业务看到完整的客户端语义 (unary = 一请求一半关闭)
- **防护**: 二次 onMessage → `call.close(INTERNAL TOO_MANY_REQUESTS)` + canInvoke=false (L155-161); halfClose 时无 request → `MISSING_REQUEST` (L174-179)
- **冻结对称**: invoke 后 `responseObserver.freeze()` (L184) — 业务代码执行中禁止改流控配置 (与客户端 adapter.freeze() q3 对称)
- **onReady 补偿** (L185-189): "Since we are calling invoke in halfClose we have missed the onReady event from the transport so recover it here" — 延迟调用错过 onReady, wasReady 缓存补偿
- **取消/完成钩子** (L193-215): onCancel→onCancelHandler/cancelled 标志; onComplete→onCloseHandler
- StreamingServerCallHandler (L219-233): client-streaming + bidi 共用, bidi 布尔区分; 4 种 Method 接口 (L87-110) 是编译器 MethodHandlers 实现的目标

代码类型: Implementation (服务端状态机)

结论: 服务端 unary 是一个三阶段状态机: onMessage (存请求, 防多请求) → onHalfClose (校验+invoke+freeze) → onCancel/onComplete (钩子)。设计亮点: ① 延迟到 halfClose 调用业务 — 客户端语义完整性 ② request(2) 双响应防护与客户端对称 ③ 冻结窗口保护配置一致性。 (ServerCalls.java:49-52,125-190)
