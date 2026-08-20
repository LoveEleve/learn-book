# 闭环笔记 GW-5-q2 — HttpClientFactory: Reactor Netty 装配 (连接池/代理/SSL)

假设: HttpClient 由 HttpClientFactory 统一装配 — ConnectionProvider 连接池 + 代理/SSL/编解码配置。

验证过程:
- **工厂** (HttpClientFactory.java:47-83): extends AbstractFactoryBean<HttpClient>; **createInstance** (L79): `HttpClient.create(connectionProvider)` (L83) — **Reactor Netty HttpClient + 自定义 ConnectionProvider**
- **代理** (L127-140): `properties.getProxy()` 非空 → `httpClient.proxy(proxySpec -> configureProxyProvider(...))` (L131-132) — ProxyProvider (type/host/port 等 L140)
- **配置面**: HttpClientProperties (586 行) — 连接池 (maxConnections/pendingAcquireTimeout)/超时 (connectTimeout/responseTimeout)/SSL/压缩/流控
- 事件循环: 共享 Netty event loop (网关统一线程模型)
- 关系: NettyRoutingFilter 注入 HttpClient (L90 构造) — **装配与使用分离** (v2 审计路径修正: 在 config/ 非 client/)

代码类型: Glue (装配工厂)

结论: HttpClient 装配 = **集中工厂**: 连接池 (ConnectionProvider)/代理/超时/SSL 全在 HttpClientFactory 一处; 过滤器只消费注入的实例。**被放弃的方案: 各过滤器自建 HttpClient** — 连接池共享 (网关级复用) 是性能关键。 [跨域: GW-5 q1 消费; Netty 阶段1 线程模型] [Reactor Netty: ConnectionProvider] (HttpClientFactory.java:47-140)
