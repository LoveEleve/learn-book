# 闭环笔记 GW-9-q1 — CORS 双映射器: 静态资源 + 路由映射

假设: 网关 CORS 配置有两处装配: SimpleUrlHandlerMapping (静态资源) 与 RoutePredicateHandlerMapping (路由) — 前者自动配置直接设置, 后者由事件监听器设置。

验证过程:
- **静态映射器** (SimpleUrlHandlerMappingGlobalCorsAutoConfiguration.java:35-45): `@PostConstruct config(): simpleUrlHandlerMapping.setCorsConfigurations(globalCorsProperties.getCorsConfigurations())` (L45) — **全局 CORS 直接注入静态资源映射器**
- **路由映射器**: RoutePredicateHandlerMapping 的 CORS 由 CorsGatewayFilterApplicationListener 在 RefreshRoutesResultEvent 时设置 (L110: routePredicateHandlerMapping.setCorsConfigurations) — **事件驱动** (路由变化时重建)
- **GlobalCorsProperties**: 全局 CORS 配置源 (两处共用)
- 关系: 静态资源 (SimpleUrlHandlerMapping) vs 路由请求 (RoutePredicateHandlerMapping) — 两类请求的 CORS 覆盖

代码类型: Glue (装配)

结论: CORS 双装配 = **静态直接 + 路由事件**: 全局配置一份, 两个映射器各自消费; 路由映射器的 CORS 与路由定义耦合 (路由变化 → 事件 → 重建)。**被放弃的方案: 单一 CORS 过滤器** — 映射器级配置让 CORS 与路由/资源路径天然对齐。 [跨域: GW-1 路由映射; WebFlux CORS] (SimpleUrlHandlerMappingGlobalCorsAutoConfiguration.java:35-45; CorsGatewayFilterApplicationListener.java:110)
