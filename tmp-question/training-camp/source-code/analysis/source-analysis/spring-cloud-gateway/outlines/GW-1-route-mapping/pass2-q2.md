# 闭环笔记 GW-1-q2 — 装配链: RouteDefinition → Route (谓词 AND 组合 + 过滤器工厂)

假设: 配置形态 (RouteDefinition) 经 RouteDefinitionRouteLocator 转换为运行时 Route: 谓词全部 AND 组合, 过滤器逐个工厂实例化。

验证过程:
- **getRoutes** (RouteDefinitionRouteLocator.java:109-127): routeDefinitions.map(convertToRoute) + **failOnRouteDefinitionError 容错** (L111-121): `onErrorContinue` — 坏配置路由 "will be ignored. Definition has invalid configs" — **单个坏路由不炸整个网关**
- **convertToRoute** (L129-135): `AsyncPredicate predicate = combinePredicates(...); List<GatewayFilter> gatewayFilters = getFilters(...); return Route.async(routeDefinition).asyncPredicate(predicate).replaceFilters(gatewayFilters).build()`
- **combinePredicates** (L198-209): 谓词流 → lookup 每个 → `reduce(AsyncPredicate.from(exchange -> true), AsyncPredicate::and)` — **AND 组合**; 无谓词 → 匹配全部 (L201-203, "very rare case, but possible")
- **lookup** (L211-240): 按名找 RoutePredicateFactory → `configurationService.with(factory)...bind()` (参数绑定, 事件钩子 PredicateArgsEvent) → `factory.applyAsync(config)` — **工厂模式 + 配置绑定**
- **loadGatewayFilters** (L145-168): 过滤器工厂按名找 (找不到 → IllegalArgumentException "Unable to find GatewayFilterFactory with name") → 参数绑定 → `factory.apply(configuration)`
- Route 构建 (Route.java:72-78): builder(RouteDefinition) 拷贝 id/uri/order/metadata

代码类型: Glue (装配转换)

结论: 配置→运行时的**两层模型**: RouteDefinition (声明) vs Route (运行), 转换时谓词 AND 组合 (无谓词 = 全匹配), 过滤器按名工厂实例化; **配置绑定统一走 ConfigurationService** (工厂参数自动绑定 + 事件钩子); 容错设计让单个路由配置错误只告警不瘫痪。**被放弃的方案: 运行期直接读配置** — 预编译为 Route (含 AsyncPredicate/已实例化过滤器) 让请求路径零配置解析。 [跨域: GW-2 过滤器工厂族; GW-3 谓词工厂族] [模式: 声明→运行时] (RouteDefinitionRouteLocator.java:109-240; Route.java:68-78)
