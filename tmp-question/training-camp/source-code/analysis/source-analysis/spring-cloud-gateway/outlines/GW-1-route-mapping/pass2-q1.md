# 闭环笔记 GW-1-q1 — 请求路由: 谓词过滤 + 首匹配胜出

假设: 请求到达后, RoutePredicateHandlerMapping 遍历所有路由, 用每个路由的谓词过滤, **第一个匹配的路由胜出**并转交 FilteringWebHandler。

验证过程:
- **getHandlerInternal** (RoutePredicateHandlerMapping.java:79-110): management port 检查 (L81-85, 管理端口不代理) → Mono.deferContextual → lookupRoute → 匹配: `exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, r); return webHandler` (L97-101) — **匹配路由存 exchange 属性, 返回 FilteringWebHandler**
- **lookupRoute** (L130-140): `routeLocator.getRoutes().filterWhen(route -> { exchange.getAttributes().put(GATEWAY_PREDICATE_ROUTE_ATTR, route.getId()); try { return route.getPredicate().apply(exchange); } ... })` — **逐路由谓词过滤; 测试前记录当前路由 id (诊断)**; 异常 → 记日志 + false
- **首匹配语义**: filterWhen 后 Flux 第一个元素即匹配路由 (路由序 = 定义序, getRoutes 保持序)
- 不匹配: switchIfEmpty 清理 + "No RouteDefinition found" 日志 (L104-109)
- 测试: lookupRouteFromSyncPredicates (RoutePredicateHandlerMappingTests.java:43)

代码类型: Implementation (映射器)

结论: 路由映射 = **顺序谓词过滤**: 路由按定义序排列, 请求逐一过谓词, 首个匹配胜出; 匹配结果 (Route) 存 exchange 属性 (后续过滤器链读取 GATEWAY_ROUTE_ATTR); **被放弃的方案: 最长前缀匹配 (类似 servlet path mapping)** — 谓词 SPI 让匹配逻辑完全可配置 (路径/头/cookie 任意组合)。 [跨域: GW-3 谓词 SPI 消费] [模式: 责任链式匹配] (RoutePredicateHandlerMapping.java:79-140)
