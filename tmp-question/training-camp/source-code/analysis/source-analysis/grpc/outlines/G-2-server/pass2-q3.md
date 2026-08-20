# 闭环笔记 Q3 — 请求接收链路: 从 HTTP/2 帧到业务方法

假设: 帧 → NettyServerHandler → ServerStream → ServerImpl.streamCreated (传输线程) → JumpToApplicationThreadServerStreamListener (跳应用线程) → serializing executor 排队 → 拦截器包装 → 业务。

验证过程:
- NettyServerHandlerTest 实证帧→监听器转发: inboundDataWithEndStreamShouldForwardToStreamListener (L262)/clientHalfClose (L296)/clientCancel (L314)
- grep `streamCreated` (ServerImpl.java:490 区): ① 解压器协商 `Can't find decompressor for %s → UNIMPLEMENTED` (L490-497) ② `registry.lookupMethod` (L501) ③ **`createContext(headers, statsTraceCtx)`** (L503, Context.CancellableContext — 元数据建可取消上下文)
- **JumpToApplicationThreadServerStreamListener** (L510-513): `stream.setListener(jumpListener)` — 传输线程回调经此跳转到应用 executor
- **MethodLookup** (L524-564): ContextRunnable 在 **serializing executor** 排队 ("so jumpListener.setListener() is called before any callbacks are delivered", L515-517) — 顺序保证: 回调绝不先于 setup 到达
- `wrapMethod(stream, method, statsTraceCtx)` (L561) — **拦截器在此包装 handler**; `maySwitchExecutor` (L562) — executorSupplier 动态换执行器
- 链路终点: wrapMethod 产 ServerCallParameters → HandleServerCall → 生成 ServerCallImpl + ServerCall.Listener (用户回调)

代码类型: Glue (跨线程桥)

结论: 请求链路的灵魂是 **双执行器跳转**: 传输线程 (Netty event loop) 收帧 → serializing executor 上排队 MethodLookup (查找+包装+换执行器) → 应用 executor 执行业务。**顺序保证设计**: serializing executor 排队保证 setListener 先于任何回调 (L515-517), 杜绝竞态; 拦截器包装发生在查找之后、回调之前 (L561)。 **被放弃的方案: 在传输线程直接查找/执行** — 会把用户代码 (可能阻塞) 跑在 Netty event loop 上, 阻塞整个连接。 [跨域: 客户端侧对称双执行器在 G-3 展开] (ServerImpl.java:490-564)
