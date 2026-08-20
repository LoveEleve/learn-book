# 闭环笔记 Q1 — 服务端装配链: Builder 委托 + 传输注入

假设: NettyServerBuilder 不直接建 Server — 委托 ServerImplBuilder, 传输 (NettyServer) 经回调接口注入 ServerImpl。

验证过程:
- grep `new ServerImplBuilder` → NettyServerBuilder.java:176 `serverImplBuilder = new ServerImplBuilder(new NettyClientTransportServersBuilder())` — **委托模式**: NettyServerBuilder extends ForwardingServerBuilder, 配置转发给 core 的 ServerImplBuilder, 传输工厂以回调接口 (ClientTransportServersBuilder, ServerImplBuilder.java:109) 注入
- grep `buildTransportServers` → L710-741: `new NettyServer(listenAddresses, channelFactory, ..., negotiator, ...)` — NettyServer 组装 (协议协商器是参数)
- grep `Server build()` → ServerImplBuilder.java:257-263: `new ServerImpl(this, clientTransportServersBuilder.buildClientTransportServers(...), Context.ROOT)` — **ServerImpl 是主体, 传输是构造参数**
- grep `transportServer.start` → ServerImpl.java:187 `transportServer.start(listener)` — ServerImpl 把 ServerListenerImpl 传给传输; NettyServer.start (NettyServer.java:218-240) 配 Netty ServerBootstrap (boss/worker event loop 分组 + channelFactory)
- ServerImpl 字段: `InternalServer transportServer` (L117) — 传输接口抽象 (netty/inprocess/binder 各自实现)

代码类型: Glue + Implementation (装配回调)

结论: 服务端装配是**依赖注入式**的: NettyServerBuilder (用户面) 委托 ServerImplBuilder (core 面), 传输工厂回调返回 NettyServer (netty 面), ServerImpl 持有 InternalServer 接口 — 三层解耦让 inprocess/binder 等传输零改动复用 ServerImpl 全部逻辑 (注册表/生命周期/调用链)。**被放弃的方案: NettyServerBuilder 直接 extends ServerImplBuilder** — 那会把 netty 依赖打进 core; 委托 + 回调接口保持 core 纯净。 [跨域: inprocess/binder 传输复用同一 ServerImpl (传输抽象实例)] (NettyServerBuilder.java:176,710-741; ServerImplBuilder.java:109,257-263; ServerImpl.java:117,187)
