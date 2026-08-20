# 闭环笔记 Q7 — ProtocolNegotiators: 协商 = 管道替换

假设: 服务端协议协商是 Netty pipeline 动态替换 — TLS 握手 + ALPN 检查成功后把 SslHandler 换成 HTTP/2 处理器, 安全属性经事件传播。

验证过程:
- **工厂面**: serverPlaintext (ProtocolNegotiators.java:339)/serverTlsFactory (L359, SslContext)/ServerFactory 接口 (L316-337, newNegotiator(executorPool)) — NettyServerBuilder 持工厂, buildTransportServers 时 newNegotiator (NettyServerBuilder.java:720-721)
- **ServerTlsHandler** (L424-487): handlerAdded 时 `sslContext.newEngine` → pipeline 插 SslHandler (L443-449); **userEventTriggered**: SslHandshakeCompletionEvent 成功 → **ALPN 检查** `sslContext.applicationProtocolNegotiator().protocols().contains(sslHandler.applicationProtocol())` (L463-469) — 协商出的协议不在配置列表 → "Failed protocol negotiation: Unable to find compatible protocol"; 成功 → `ctx.pipeline().replace(ctx.name(), null, next)` (L471) — **SslHandler 替换为 HTTP/2 handler**
- **安全属性传播**: fireProtocolNegotiationEvent (L476-487): ProtocolNegotiationEvent 携带 **SecurityLevel.PRIVACY_AND_INTEGRITY** + **Grpc.TRANSPORT_ATTR_SSL_SESSION** (L482-485) → 上层从 Attributes 读 (传输属性 → ServerCall.getAttributes, G-2 链路)
- httpProxy (L489-496+): ProxyNegotiator 先做 HTTP CONNECT 再交给内层协商 — 组合式

代码类型: Implementation (pipeline 动态化)

结论: 协商本质是 **pipeline 运行时重排**: 明文路径直接挂 HTTP/2; TLS 路径 SslHandler 前置, 握手+ALPN 通过后整体替换; ALPN 是 TLS 1.3 协议协商通道 (h2/h2c 选择); 安全结果 (SecurityLevel/SSLSession) 以事件属性形式注入 Attributes — 应用代码零改动感知"这条连接是加密的"; **被放弃的方案: 固定 pipeline (SslHandler 常驻)** — 握手后替换省去每帧 SslHandler 检查开销; 事件传递而非直接调用让协商与处理解耦。 [跨域: G-3 客户端协商对称 (tlsClientFactory L793)] (ProtocolNegotiators.java:339,359,424-487)
