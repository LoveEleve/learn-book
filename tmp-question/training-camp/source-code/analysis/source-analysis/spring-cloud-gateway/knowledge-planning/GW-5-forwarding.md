# GW-5 请求转发与头处理 — 知识规划 (KP)

> 域级: 🔴 A | 模块: filter/ 转发家族 + config/HttpClientFactory + AdaptCachedBodyGlobalFilter + filter/headers/ (16) + cache/ (11, v4)
> 日期: 2026-08-16 | 版本: 4.3.2 | Pass 2 闭环: q1(转发主流程) q2(HttpClient) q3(body 缓存) q4(头传播) q5(转发变体) q6(响应回写) — **6/6 全闭环**

## 一、机制提取 (逐源)

### M1 Netty 转发主流程 (q1)
- NettyRoutingFilter (72-200): GATEWAY_REQUEST_URL_ATTR (L112) → httpClient.request(method).uri(url).send(body) (L133-147)
- **延迟提交** (L147+): responseConnection → 头/状态写 exchange — "Defer committing the response until all route filters have run" → NettyWriteResponseFilter 回写

### M2 HttpClient 装配 (q2)
- HttpClientFactory (47-140): HttpClient.create(connectionProvider) (L83) + proxy (L127-140)
- HttpClientProperties (586): 连接池/超时/SSL

### M3 请求体缓存 (q3)
- AdaptCachedBodyGlobalFilter (37-81): ApplicationListener<EnableBodyCachingEvent> + routesToCache → cacheRequestBody (L66) + mutate 替换请求 (L71-75)
- order = HIGHEST_PRECEDENCE + 1000 (L80)

### M4 头传播 (q4)
- XForwardedHeadersFilter (42-242): 5 头常量 (L59-71) + 5 append 开关 (L94-105) + **TrustedProxies 匹配** (L242)
- RemoveHopByHopHeadersFilter/ForwardedHeadersFilter/GRPCRequestHeadersFilter

### M5 转发变体 (q5)
- WebClientHttpRoutingFilter (WebClient 版)/WebsocketRoutingFilter (ws, WebSocketClient)/ForwardRoutingFilter (forward://, DispatcherHandler)

### M6 响应回写 (q6)
- NettyWriteResponseFilter (44-105): CLIENT_RESPONSE_CONN_ATTR (L71) → inbound receive → 流式 writeAndFlushWith vs writeWith (L96-100) + cleanup (L101-105)

## 二、聚合分级

| 级别 | 机制 |
|---|---|
| P1 | M1 延迟提交两阶段 / M3 body 缓存按需 / M4 TrustedProxies |
| P2 | M2 连接池 / M5 scheme 策略 / M6 流式回写 |

## 三、叙事线

场景: 过滤器链走到转发 — 请求怎么发出去?响应怎么回来?读者疑问链: 转发主流程 (M1) → HttpClient 哪来 (M2) → body 怎么缓存 (M3) → 头怎么传播 (M4) → 其他 scheme (M5) → 响应回写 (M6)。
