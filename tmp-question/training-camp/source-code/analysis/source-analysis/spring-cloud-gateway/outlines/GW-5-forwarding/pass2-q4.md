# 闭环笔记 GW-5-q4 — 头传播: X-Forwarded 五头 + TrustedProxies

假设: XForwardedHeadersFilter 按配置追加 5 个 X-Forwarded-* 头 (For/Host/Port/Proto/Prefix), 且**只信可信代理** (TrustedProxies 匹配, 防伪造 IP)。

验证过程:
- **XForwardedHeadersFilter** (XForwardedHeadersFilter.java:42-110): @ConfigurationProperties("spring.cloud.gateway.server.webflux.x-forwarded") (L41) — **全开关可配**; **5 个常量头** (L59-71): X-Forwarded-For/Host/Port/Proto/Prefix; 5 个 append 开关 (L94-105, 列表追加 vs 覆盖)
- **可信代理** (L242): "match xforwarded for against trusted proxies" — **TrustedProxies 匹配才追加** (防客户端伪造 X-Forwarded-For 注入); 未配置 → 日志警告 (L115: "trusted-proxies is not set. Using deprecated Constructor. Untrusted hosts might be added")
- **HttpHeadersFilter** (接口): 转发前/响应后的头处理 SPI (NettyRoutingFilter 注入 L90)
- 家族: ForwardedHeadersFilter (RFC 7239 Forwarded)/RemoveHopByHopHeadersFilter (Connection/Keep-Alive 等逐跳头)/GRPCRequestHeadersFilter (GRPC 专属头)
- 关联: XForwardedRemoteAddrRoutePredicateFactory (GW-3) 消费 X-Forwarded-For

代码类型: Implementation (头处理)

结论: 头传播 = **可配 + 可信**: 5 头按开关追加 (列表 vs 覆盖), **TrustedProxies 白名单**防 IP 伪造 (安全面); hop-by-hop 移除保证协议正确。**被放弃的方案: 无脑追加** — 伪造 X-Forwarded-For 可绕过 IP 谓词; 可信代理校验是安全底线。 [跨域: GW-3 IP 谓词消费; 安全面] [协议: RFC 7239/逐跳头] (XForwardedHeadersFilter.java:41-110,242)
