# GW-8 temporal-trace — 时空溯源 (🟡 域, 简版)

> 浅克隆无 git 历史 — 用代码内痕迹。

## 演进痕迹

| 痕迹 | 证据 | 意义 |
|---|---|---|
| REGEXP_KEY/REPLACEMENT_KEY | RewritePathGatewayFilterFactory L44-49 | 短路键演进 (v4 配置体系) |
| GATEWAY_ALREADY_PREFIXED_ATTR | PrefixPath L67 | 幂等演进 (防重复前缀) |
| GATEWAY_ORIGINAL_REQUEST_URL_ATTR | ServerWebExchangeUtils L111 | 原始链保留演进 (可观测) |
| RewritePath 双场景 | GW-4 initFilters L72 | 工厂复用演进 (服务发现路由) |
| UriTemplate | PrefixPath L65 | 模板化演进 (变量前缀) |

## 写书建议

路径重写 = "4 种变换 + 治理演进": 核心变换稳定, 演进在幂等标志/原始链/模板化/工厂复用。
