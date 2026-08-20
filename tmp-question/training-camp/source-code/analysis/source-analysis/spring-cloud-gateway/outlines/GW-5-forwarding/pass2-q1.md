# 闭环笔记 GW-5-q1 — Netty 转发主流程: 发出请求 + 延迟提交响应

假设: NettyRoutingFilter 是默认转发 — 读 GATEWAY_REQUEST_URL_ATTR 发请求, 响应头/状态写 exchange 属性后**延迟提交** (等路由过滤器改响应), 由 NettyWriteResponseFilter 回写。

验证过程:
- **入口** (NettyRoutingFilter.java:72-112): implements GlobalFilter + Ordered; **GATEWAY_REQUEST_URL_ATTR** (L61, RouteToRequestUrlFilter 设置的转发目标, L112 `getRequiredAttribute`) — scheme 检查 (lb:// 未解析 → 直接 next, L116)
- **请求发出** (L133-147): `httpClient.headers(host).request(method).uri(url).send((req, nettyOutbound) -> nettyOutbound.send(request.getBody().map(this::getByteBuf)))` — **Reactor Netty 响应式发送** (body 流式)
- **响应延迟提交** (L147-200 区): `responseConnection((res, connection) -> { ... res.responseHeaders().forEach(headers::add); setResponseStatus(res, response); ... })` — 注释: "**Defer committing the response until all route filters have run**... write response later NettyWriteResponseFilter" — **头/状态先写 exchange, 响应体后写**
- 收尾: `responseFlux.then(chain.filter(exchange))` (L200) — 转发完成才继续链 (下游 = 后置过滤器)
- 关联: RouteToRequestUrlFilter (链前置, 设 GATEWAY_REQUEST_URL_ATTR)

代码类型: Implementation (转发)

结论: 转发 = **两阶段**: ① NettyRoutingFilter 发请求 + 响应头/状态写 exchange (延迟提交 — 让后续路由过滤器能改响应头) ② NettyWriteResponseFilter 回写响应体; Reactor Netty 全程响应式 (body 流式, 无整包缓冲)。**被放弃的方案: 同步阻塞转发** — 反应式让高并发低成本 (Netty 线程不阻塞)。 [跨域: GW-2 链执行 (延迟提交 = 链内协作)] [Reactor Netty: HttpClient] (NettyRoutingFilter.java:72-200)
