# GW-5 请求转发与头处理 — 链尾的最后一跳: 延迟提交与响应回写

> 前置: [[GW-2-过滤器链]] (链执行) | 引出: 应用面过滤器族 (GW-6/7/8/9) | 对照: Netty HttpClient (阶段1) + Envoy 转发 + **GW-4 负载均衡 (lb:// 解析 — 链中流程前位, 非机制依赖)**
> 🔴 A | 6 KP | [模式: 两阶段提交 + scheme 策略 + 可信头]
> Pass 2 闭环: q1-q6 — **6/6 全闭环**

**读者处境**: 过滤器链最后, NettyRoutingFilter 把请求发出去——但它"发完就完事"吗?响应头谁写?body 怎么回来?X-Forwarded-For 怎么追加还不被伪造?WebSocket 请求怎么转发?

### 1. 转发主流程 — 发出请求 + 延迟提交

场景: 过滤器链怎么把请求真正发出去?
源码路径:
- **NettyRoutingFilter** (NettyRoutingFilter.java:72-200): **GATEWAY_REQUEST_URL_ATTR** (L61, RouteToRequestUrlFilter 设置的转发目标, L112 读取) — lb:// 未解析直接 next (L116)
- **请求发出** (L133-147): `httpClient.request(method).uri(url).send(body)` — Reactor Netty 响应式 (body 流式)
- **延迟提交** (L147+): `responseConnection` 注释: "**Defer committing the response until all route filters have run**... write response later NettyWriteResponseFilter" — **头/状态写 exchange, 响应体后写**
关键设计 (q1): **两阶段转发**: ① 发请求 + 响应头/状态写 exchange (延迟) ② NettyWriteResponseFilter 回写 body。**被放弃的方案: 转发器内直接回写** — 后置过滤器 (SetResponseHeader 等) 需能改响应头。 [Reactor Netty] [跨域: GW-2 链内协作]

### 2. HttpClient 装配 — 集中工厂

场景: 转发用的 HttpClient 从哪来?
源码路径:
- **HttpClientFactory** (HttpClientFactory.java:47-140): **createInstance** (L79): `HttpClient.create(connectionProvider)` (L83) — **连接池 (ConnectionProvider)**
- **代理** (L127-140): proxy 配置 → `httpClient.proxy(...)` (L131-132)
- **HttpClientProperties** (586): 连接池/超时/SSL/压缩
关键设计 (q2): **集中装配**: 连接池网关级共享; 过滤器只消费注入实例。**被放弃的方案: 各过滤器自建** — 连接池复用是性能关键。 [Reactor Netty: ConnectionProvider]

### 3. 请求体缓存 — 按需声明

场景: 谓词/过滤器要读请求体, 但 body 只能读一次?
源码路径:
- **AdaptCachedBodyGlobalFilter** (AdaptCachedBodyGlobalFilter.java:37-81): GlobalFilter + **ApplicationListener<EnableBodyCachingEvent>** (L37) + routesToCache 按路由
- **缓存** (L66): cacheRequestBody → **mutate().request(替换请求)** (L71-75)
- **order** (L80): `HIGHEST_PRECEDENCE + 1000` — **链最前置**
关键设计 (q3): **事件声明 + 前置缓存**: 过滤器工厂发布 EnableBodyCachingEvent 声明需求, 前置过滤器按路由缓存。**被放弃的方案: 无条件全缓存** — 内存成本; 按需声明。 [跨域: GW-3 ReadBody 谓词依赖] [内存: 按路由]

### 4. 头传播 — X-Forwarded 五头 + 可信代理

场景: X-Forwarded-For 怎么追加?怎么防伪造?
源码路径:
- **XForwardedHeadersFilter** (XForwardedHeadersFilter.java:42-242): @ConfigurationProperties (L41); **5 头** (L59-71: For/Host/Port/Proto/Prefix) + **5 开关** (L94-105, 追加 vs 覆盖)
- **可信代理** (L242): "match xforwarded for against trusted proxies" — **TrustedProxies 白名单**; 未配置 → 警告 (L115)
- 家族: RemoveHopByHopHeadersFilter/ForwardedHeadersFilter (RFC 7239)/GRPC 头
关键设计 (q4): **可配 + 可信**: 开关控制追加语义; **TrustedProxies 防 IP 伪造** (安全底线)。**被放弃的方案: 无脑追加** — 伪造 X-Forwarded-For 可绕过 IP 谓词。 [安全] [跨域: GW-3 IP 谓词消费]

### 5. 转发变体 — scheme 驱动策略

场景: WebSocket/内部转发怎么走?
源码路径:
- **WebsocketRoutingFilter** (WebsocketRoutingFilter.java:37-41): ws/wss → WebSocketClient + WebSocketService
- **ForwardRoutingFilter** (ForwardRoutingFilter.java:34-43): forward:// → **DispatcherHandler** (MVC 内部转发)
- WebClientHttpRoutingFilter (WebClient 版, 可配替代)/FunctionRoutingFilter/StreamRoutingFilter
关键设计 (q5): **scheme 驱动**: http:// (Netty 默认)/ws:// (升级握手双向流)/forward:// (内部) 各司其职。**被放弃的方案: 单一转发器** — WebSocket 与 HTTP 请求-响应模型不同。 [WebFlux: WebSocket/DispatcherHandler] [跨域: GW-4 lb:// 先解析]

### 6. 响应回写 — 流式与普通

场景: 响应 body 怎么回到客户端?
源码路径:
- **NettyWriteResponseFilter** (NettyWriteResponseFilter.java:44-105): **CLIENT_RESPONSE_CONN_ATTR** (L71) → `connection.inbound().receive().retain()` (L84-88)
- **流式判定** (L96-100): `isStreamingMediaType ? writeAndFlushWith(body.map(Flux::just)) : writeWith(body)` — **SSE/流逐块推, 普通缓冲写**
- **清理** (L101-105): doFinally → cleanup
关键设计 (q6): **连接属性传递 + 流式回写**: 延迟提交的消费端 (q1); 流式媒体类型专用回写路径。**被放弃的方案: 转发器直接回写** — 失去后置过滤器修改机会。**响应缓存对照 (v4 面, cache/ 11 文件)**: LocalResponseCacheGatewayFilterFactory/GlobalLocalResponseCacheGatewayFilter + ResponseCacheManager — 响应在回写前经缓存层 (键生成/大小权重), 缓存命中直接回写不走后端。 [Reactor Netty: 连接生命周期] [跨域: v4 审计扩展面]

### 核心悬念

"转发完成 — 但网关还能在转发前后做什么?限流 (令牌桶)、熔断 (断路器)、路径重写 — 应用面过滤器族。" 下一域 [[GW-6-限流]] → [[GW-7-熔断与重试]] → [[GW-8-路径重写]]。

### 负面空间 (不做)

1. 不写 Reactor Netty HttpClient API 全貌 (Netty 阶段1)
2. 不写 WebSocket 协议细节 (升级握手)
3. 不写 HttpClientProperties 全配置穷举
4. 不写响应缓存面 (cache/ 11, v4) 细节 (对照面)
5. 不写 GRPC 头过滤器细节 (GRPC 专属)
6. 不写 FunctionRoutingFilter/StreamRoutingFilter 细节 (函数/流式对照)
