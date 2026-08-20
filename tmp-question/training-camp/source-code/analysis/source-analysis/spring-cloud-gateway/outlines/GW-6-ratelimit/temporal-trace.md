# GW-6 temporal-trace — 时空溯源 (🟡 域, 简版)

> 浅克隆无 git 历史 — 用代码内痕迹。

## 演进痕迹

| 痕迹 | 证据 | 意义 |
|---|---|---|
| REDIS_SCRIPT_NAME bean | RedisRateLimiter.java:65 | Lua 脚本 bean 化 (脚本外置演进) |
| Header 可配 | replenishRateHeader (L111-117) | 响应头可配置化演进 |
| Bucket4jRateLimiter | ratelimit/ 第二实现 | 限流器多实现演进 (bucket4j 库) |
| PrincipalNameKeyResolver BEAN_NAME | L28 | KeyResolver 默认 bean 化 |
| "Stripe's observed failure rate 0.01%" | L284 | 故障降级是引用行业数据的刻意设计 |

## 写书建议

限流面 = "SPI 编排 + 多实现演进": 核心 (编排/Lua) 稳定, 演进在实现族 (Bucket4j)/可配化 (头/脚本)。
