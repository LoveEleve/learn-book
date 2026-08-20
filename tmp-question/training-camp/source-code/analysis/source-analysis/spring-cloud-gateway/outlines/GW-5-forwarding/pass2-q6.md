# 闭环笔记 GW-5-q6 — 响应回写: NettyWriteResponseFilter (延迟提交的消费端)

假设: NettyWriteResponseFilter 读 CLIENT_RESPONSE_CONN_ATTR (NettyRoutingFilter 存的连接), 把响应体流式回写; 流式媒体类型走 writeAndFlushWith。

验证过程:
- **NettyWriteResponseFilter** (NettyWriteResponseFilter.java:44-105): GlobalFilter + Ordered; 注释 (L66): "NOTICE: nothing in 'pre' filter stage as CLIENT_RESPONSE_CONN_ATTR is not added" — **只有后置阶段**
- **回写** (L71-100): `connection = exchange.getAttribute(CLIENT_RESPONSE_CONN_ATTR)` (L71) → `body = connection.inbound().receive().retain().map(wrap)` (L84-88) → **流式判定** (L96-100): `isStreamingMediaType(contentType) ? response.writeAndFlushWith(body.map(Flux::just)) : response.writeWith(body)` — **流式用 writeAndFlushWith (逐块刷), 普通用 writeWith (缓冲)**
- **清理** (L101-105): doFinally CANCEL/ON_ERROR → cleanup — 连接释放
- 分工: NettyRoutingFilter 存连接 (q1 延迟提交), WriteResponseFilter 消费回写 — **读写分离两过滤器**

代码类型: Implementation (响应回写)

结论: 响应回写 = **连接属性传递 + 流式回写**: CLIENT_RESPONSE_CONN_ATTR 承载响应连接; 流式媒体 (SSE/流) 用 writeAndFlushWith 逐块推, 普通缓冲写; 异常清理。**被放弃的方案: 转发过滤器内直接回写** — 延迟到链尾让后置过滤器 (SetResponseHeader 等) 能改响应头 (q1 注释: "Defer committing the response until all route filters have run")。 [跨域: GW-2 链协作模式] [Reactor Netty: 连接生命周期] (NettyWriteResponseFilter.java:44-105)
