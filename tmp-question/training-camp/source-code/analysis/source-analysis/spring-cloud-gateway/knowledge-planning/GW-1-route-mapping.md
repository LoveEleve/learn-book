# GW-1 路由定位 — 知识规划 (KP)

> 域级: 🔴 A | 模块: handler/RoutePredicateHandlerMapping (194) + route/ (15: RouteDefinitionRouteLocator 233/Route 44/Composite/Caching/Repository)
> 日期: 2026-08-16 | 版本: 4.3.2 | Pass 2 闭环: q1-q4 + **q5(DSL) q6(管理端点) — 6/6 全闭环**

## 一、机制提取 (逐源)

### M1 请求映射 (q1)
- getHandlerInternal (RoutePredicateHandlerMapping.java:79-110): management port 隔离 (L81-85) → lookupRoute → 匹配: GATEWAY_ROUTE_ATTR + webHandler (L97-101)
- lookupRoute (L130-140): routeLocator.getRoutes().filterWhen(谓词 apply) — 逐路由过滤 + GATEWAY_PREDICATE_ROUTE_ATTR 诊断; 首匹配胜出
- 无匹配: 清属性/缓存体 → empty → 404 (L104-109)

### M2 装配链 (q2)
- getRoutes (RouteDefinitionRouteLocator.java:109-127): convertToRoute + failOnRouteDefinitionError 容错 (L111-121)
- convertToRoute (L129-135): combinePredicates + getFilters → Route.async().asyncPredicate().replaceFilters()
- combinePredicates (L198-209): 谓词流 reduce(and) — AND 组合; 无谓词全匹配
- lookup (L211-240): 工厂按名 + ConfigurationService 绑定 + applyAsync
- loadGatewayFilters (L145-168): 过滤器工厂 + 绑定 + apply

### M3 定位器家族 (q3)
- CompositeRouteLocator (L30-39): flatMapSequential 保序合并
- CachingRouteLocator (L55-80): **CacheFlux** + fetch sort(AnnotationAwareOrderComparator) — order 序匹配
- RouteRefreshListener (L46-80): ContextRefreshed/RefreshScope/InstanceRegistered/Heartbeat 事件 → RefreshRoutesEvent → 清缓存

### M4 失败路径 (q4)
- empty → WebFlux 404; validateRoute 扩展点 (L168-173); NotFoundException (support/)

### M5 编程式 DSL (q5, v2)
- RouteLocatorBuilder (L33-103): routes()/route(id, fn) (L69)/build() → Flux RouteLocator (L101-103)
- GatewayFilterSpec (L95-134, 1047 行): filter(order/forceOrder) + circuitBreaker (L273)/retry (L710) 类型化方法
- 与配置式汇合到同一 Route 模型

### M6 路由管理端点 (q6, v3.1)
- AbstractGatewayControllerEndpoint (342): save POST /routes/{id} (L238)/refresh (AbstractGatewayControllerEndpoint.java:178-179)/delete (L327)/查询面 (L202-213)
- 动态增删路由 + 手动刷新 + 工厂查询
- empty → WebFlux 404; validateRoute 扩展点 (L168-173); NotFoundException (support/)

## 二、聚合分级

| 级别 | 机制 |
|---|---|
| P1 | M1 首匹配胜出 / M2 声明→运行时装配 (AND 谓词) |
| P2 | M3 缓存+事件刷新 / M4 404 降级 |

## 三、叙事线

场景: 一个请求进来, 网关怎么知道"这是哪个路由"?读者疑问链: 映射器怎么找 (M1) → 配置怎么变成可匹配的 Route (M2) → 路由从哪来/缓存/刷新 (M3) → 找不到呢 (M4)。
