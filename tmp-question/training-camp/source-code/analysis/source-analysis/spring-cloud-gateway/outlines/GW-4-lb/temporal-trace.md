# GW-4 temporal-trace — 时空溯源 (🟡 域, 简版)

> 浅克隆无 git 历史 — 用代码内痕迹。

## 演进痕迹

| 痕迹 | 证据 | 意义 |
|---|---|---|
| "Using deprecated Constructor" | XForwarded (GW-5) 同代 | 配置体系演进 |
| use404 属性 | ReactiveLoadBalancerClientFilter L123 | 无实例失败语义可配演进 (404 vs 503) |
| LoadBalancerServiceInstanceCookieFilter | filter/ 顶层 | 粘性会话后加 (基于 SCC 选择的增强) |
| DiscoveryClientRouteDefinitionLocator | discovery/ | 服务发现路由 (后加, 简化配置) |
| GATEWAY_SCHEME_PREFIX_ATTR | RouteToRequestUrlFilter L76 | 嵌套 scheme 支持演进 |

## 写书建议

LB 面 = "SCC 消费 + 网关增强": 核心 (URL 装配/choose) 委托 SCC, 增强在网关侧 (404 语义/粘性 cookie/服务发现路由)。
