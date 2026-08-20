# GW-5 temporal-trace — 时空溯源 (🔴 域)

> 浅克隆无 git 历史 — 用代码内痕迹。

## 演进痕迹

| 痕迹 | 证据 | 意义 |
|---|---|---|
| @Deprecated 构造 | XForwardedHeadersFilter "Using deprecated Constructor" (L99 区) | 可信代理配置演进 (后加 TrustedProxies) |
| WebClientHttpRoutingFilter | 转发家族成员 | **Netty 默认 + WebClient 可配** 双转发演进 |
| cache/ 响应缓存 (v4) | GlobalLocalResponseCacheGatewayFilter | 响应缓存是后加面 (网关缓存演进) |
| EnableBodyCachingEvent | event/ | body 缓存按需声明演进 |
| ForwardedHeadersFilter (RFC 7239) | headers/ 家族 | 标准 Forwarded 头支持 (X-Forwarded 的标准化演进) |

## 写书建议

转发面 = 核心 (Netty 两阶段) + 演进 (WebClient 替代/响应缓存/可信代理): 呈现"默认路径 + 可配替代"的演化。
