# GW-3 temporal-trace — 时空溯源 (🔴 域)

> 浅克隆无 git 历史 — 用代码内痕迹。

## 演进痕迹

| 痕迹 | 证据 | 意义 |
|---|---|---|
| TODO 注释 | "make this a generic Choose out of group predicate?" (WeightRoutePredicateFactory.java:40) | Weight 谓词的泛化演进方向 (通用 Choose) |
| CloudFoundryRouteService | 平台专属谓词 | 历史遗留 (Cloud Foundry 集成) |
| ReadBody 异步化 | applyAsync 覆写 (L62) | 谓词从纯同步 → 异步演进 (请求体谓词) |
| ShortcutConfigurable | RoutePredicateFactory extends (L34) | 短路语法是后加统一配置机制 |
| XForwardedRemoteAddr | 头传播面演进 (GW-5) | 代理感知谓词 |

## 写书建议

谓词是"稳定 SPI + 渐进增强": SPI (apply/applyAsync) 长期稳定; 增强在实现族 (异步/短路配置/代理感知)。
