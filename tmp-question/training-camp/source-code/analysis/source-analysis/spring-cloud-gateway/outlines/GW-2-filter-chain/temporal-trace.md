# GW-2 temporal-trace — 时空溯源 (🔴 域)

> 浅克隆无 git 历史 — 用代码内痕迹。

## 演进痕迹

| 痕迹 | 证据 | 意义 |
|---|---|---|
| @Deprecated 构造 | FilteringWebHandler(List) (L71) | 缓存开关演进 (routeFilterCacheEnabled 后加) |
| @since 0.1 | 类注释 (L52) | 过滤器链是**网关首发核心** (0.1 时代) |
| ConfigurationService | support/ (256) | 配置绑定体系后加统一 (短路语法成熟) |
| routeFilterCacheEnabled | 构造参数 | 性能治理演进 (链缓存开关) |
| GatewayFilterAdapter | L172 | GlobalFilter/GatewayFilter 统一形态演进 |

## 写书建议

过滤器链 = 首发核心 (0.1) + 治理演进: 链机制长期稳定; 演进在配置绑定 (ConfigurationService/ShortcutType) 与性能 (链缓存开关)。
