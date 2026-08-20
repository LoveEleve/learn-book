# 闭环笔记 GW-9-q2 — 路由级 CORS: 元数据 + 路径谓词提取

假设: 路由可在元数据声明 CORS (metadata.cors), 监听器提取路径谓词 pattern 作为 CORS 路径, 路由级与全局合并 (路由优先)。

验证过程:
- **触发** (CorsGatewayFilterApplicationListener.java:84-110): RefreshRoutesResultEvent → routeLocator.getRoutes() → 每路由: getCorsConfiguration (L87) → 有则 getPathPredicate + put (L88-91)
- **元数据读取** (L125-135): `route.getMetadata().get("cors")` (L126) → CorsConfiguration 构建 (allowCredentials 等, L133-135)
- **路径提取** (L109-124): getPathPredicate — **取第一个 Path 谓词的第一个 pattern** (L113-118: p.getConfig() instanceof PathRoutePredicateFactory.Config) → 无则 /** (L121)
- **合并** (L97-101): 路由级先行, **全局配置补缺** (L98-100: 路由未覆盖的路径用全局)
- **应用** (L110): routePredicateHandlerMapping.setCorsConfigurations
- 联动: 路由刷新 (GW-1) → RefreshRoutesResultEvent → CORS 重建 — **路由级 CORS 随路由定义自动更新**

代码类型: Implementation (元数据消费)

结论: 路由级 CORS = **元数据驱动 + 事件重建**: 路由 metadata.cors 声明 → 路径谓词 pattern 定位 → 与全局合并 (路由优先); 路由变更自动同步。**被放弃的方案: 独立 CORS 过滤器** — 元数据让 CORS 与路由声明同处 (配置内聚)。 [跨域: GW-1 路由模型/刷新事件; GW-3 Path 谓词消费] [HTTP: CORS 语义] (CorsGatewayFilterApplicationListener.java:84-135)
