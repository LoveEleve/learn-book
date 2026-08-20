# 闭环笔记 GW-5-q5 — 转发变体: 四种 scheme 四种语义

假设: 转发家族按 scheme 分工: lb:// (LB 解析)/http(s):// (Netty 默认)/ws+wss:// (WebSocket)/forward:// (MVC 内部)/函数/流式。

验证过程:
- **NettyRoutingFilter**: http/https 默认转发 (q1)
- **WebClientHttpRoutingFilter** (WebClientHttpRoutingFilter.java:49-51): WebClient 版 (替代 Netty, 可配)
- **WebsocketRoutingFilter** (WebsocketRoutingFilter.java:37-41): ws/wss → **WebSocketClient + WebSocketService** (Spring WebFlux 反应式 WebSocket)
- **ForwardRoutingFilter** (ForwardRoutingFilter.java:34-43): forward:// → **DispatcherHandler** (MVC 内部转发, 网关内路由到本地 handler)
- **FunctionRoutingFilter/StreamRoutingFilter**: 函数路由/流式转发
- 分工: scheme 决定转发器 (路由的 uri scheme); lb:// 先经 GW-4 LB 解析 (ReactiveLoadBalancerClientFilter)

代码类型: Implementation (策略族)

结论: 转发家族 = **scheme 驱动的策略选择**: 同一 GlobalFilter 接口, 不同 scheme 不同转发器; 内部转发 (forward://)/WebSocket (ws://)/HTTP (http://) 各司其职。**被放弃的方案: 单一转发器处理全部 scheme** — WebSocket 需要升级握手/双向流, 与 HTTP 请求-响应模型不同。 [跨域: GW-2 链 (order 分工); GW-4 lb:// 解析] [WebFlux: WebSocket/DispatcherHandler] (WebsocketRoutingFilter.java:37-41; ForwardRoutingFilter.java:34-43)
