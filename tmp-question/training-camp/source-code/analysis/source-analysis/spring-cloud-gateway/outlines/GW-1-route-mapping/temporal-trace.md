# GW-1 temporal-trace — 时空溯源 (🔴 域)

> 浅克隆无 git 历史 — 用代码内痕迹。

## 演进痕迹

| 痕迹 | 证据 | 意义 |
|---|---|---|
| @Deprecated/新 API | Route/RouteLocator 接口族 | 路由模型长期稳定 |
| CachingRouteLocator CacheFlux | reactor.cache | 缓存从手写 Map → Reactor CacheFlux 演进 (反应式缓存) |
| TODO 注释 | "support cors configuration via properties on a route see gh-229" (RoutePredicateHandlerMapping L111) | CORS 路由级配置未实现 — 演进待办 |
| failOnRouteDefinitionError | RouteDefinitionRouteLocator L111 | 容错配置是后加治理 (坏路由不炸网关) |
| RouteRefreshListener 事件面 | RefreshScope/InstanceRegistered/Heartbeat | 刷新触发源持续扩展 (配置→服务发现) |

## 写书建议

路由定位是"稳定骨架 + 治理演进": 核心 (谓词过滤/首匹配) 长期稳定; 缓存 (CacheFlux)、容错 (failOnRouteDefinitionError)、刷新源扩展是后加治理面。
