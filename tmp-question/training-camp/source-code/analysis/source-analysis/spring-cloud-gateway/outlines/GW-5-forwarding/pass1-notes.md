# GW-5 Pass 1 扫描笔记 — 请求转发与头处理

> 日期: 2026-08-16 | 版本: 4.3.2 | 🔴 A | 模块: filter/ 转发家族 (6 种) + config/HttpClientFactory + AdaptCachedBodyGlobalFilter + filter/headers/ (16) + filter/factory/cache/ (11, v4)

## 继承树/调用图

```
转发家族 (GlobalFilter 实现):
  ├── NettyRoutingFilter (72, 默认): GATEWAY_REQUEST_URL_ATTR (L112) → HttpClient (Reactor Netty)
  │     ├── 请求: headers(host) → request(method).uri(url).send(body) (L133-147)
  │     └── 响应: responseConnection → 头/状态写 exchange → 延迟提交 (NettyWriteResponseFilter)
  ├── WebClientHttpRoutingFilter / WebsocketRoutingFilter
  ├── ForwardRoutingFilter (forward://) / FunctionRoutingFilter / StreamRoutingFilter
  └── NettyWriteResponseFilter / WebClientWriteResponseFilter (响应回写)
HttpClientFactory (47): HttpClient.create(connectionProvider) (L83) + proxy 配置 (L127-140)
AdaptCachedBodyGlobalFilter (37): GlobalFilter + EnableBodyCachingEvent 监听 → cacheRequestBody (L66)
头传播面 (filter/headers/ 16):
  └── XForwardedHeadersFilter (42): X-Forwarded-For/Host/Port/Proto/Prefix (L59-71)
      ├── ForwardedHeadersFilter / RemoveHopByHopHeadersFilter / GRPC 头
      └── TrustedProxies / HttpHeadersFilter
响应缓存面 (v4, cache/ 11): GlobalLocalResponseCacheGatewayFilter / LocalResponseCacheGatewayFilterFactory / ResponseCacheManager
```

## 基本元素分解

1. **NettyRoutingFilter**: 默认转发 — 请求发出 + 响应头/状态延迟提交 (exchange 属性)
2. **HttpClientFactory**: Reactor Netty HttpClient 装配 (连接池/代理)
3. **转发变体**: WebClient/Websocket/Forward/Function/Stream
4. **请求体缓存**: AdaptCachedBodyGlobalFilter + EnableBodyCachingEvent
5. **头传播**: X-Forwarded 族 + hop-by-hop 移除 + TrustedProxies
6. **响应缓存** (v4): 本地响应缓存

## 标记问题 (6)

1. **Q1 Netty 转发主流程**: GATEWAY_REQUEST_URL → HttpClient 请求 → 响应延迟提交 (为何延迟?NettyWriteResponseFilter 分工)
2. **Q2 HttpClientFactory**: Reactor Netty 装配 (连接池/代理/SSL)
3. **Q3 请求体缓存**: AdaptCachedBodyGlobalFilter — cacheRequestBody + EnableBodyCachingEvent 触发链
4. **Q4 头传播**: XForwardedHeadersFilter — 5 个 X-Forwarded 头的追加语义 + TrustedProxies
5. **Q5 转发变体**: WebClient/Websocket/Forward 各自的 scheme 语义
6. **Q6 响应回写**: NettyWriteResponseFilter — 延迟提交的消费端

## 已读测试

- NettyRoutingFilterTests/NettyRoutingFilterIntegrationTests 存在 (filter/ 测试族)
