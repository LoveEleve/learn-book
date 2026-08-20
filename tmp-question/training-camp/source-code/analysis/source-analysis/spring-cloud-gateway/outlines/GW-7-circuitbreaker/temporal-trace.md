# GW-7 temporal-trace — 时空溯源 (🟡 域, 简版)

> 浅克隆无 git 历史 — 用代码内痕迹。

## 演进痕迹

| 痕迹 | 证据 | 意义 |
|---|---|---|
| 抽象工厂 | SpringCloudCircuitBreakerFilterFactory (L55, 抽象) | **多实现演进**: 抽象基类 + 具体实现 (Resilience4J 版) — Hystrix 时代遗留 |
| "TODO: copied from RouteToRequestUrlFilter" | L122 注释 | fallback URL 重构复用代码 (演进债务) |
| resumeWithoutError | Config/L143 | 失败策略可配演进 |
| statusCodes 白名单 | Config (L177) | 状态码熔断是后加语义 (先只看异常) |
| RetryGatewayFilterFactory 535 行 | GW-2 实证 | 重试独立工厂 (与熔断分离演进) |

## 写书建议

熔断面 = "抽象工厂 + 多实现 + 语义扩展": 核心 (包装链) 稳定, 演进在状态码熔断/fallback 可配/Retry 独立化。
