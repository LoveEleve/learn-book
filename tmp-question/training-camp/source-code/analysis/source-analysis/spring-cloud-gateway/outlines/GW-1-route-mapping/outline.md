# GW-1 路由定位 — 一个请求怎么找到它的路由: 谓词过滤与首匹配胜出

> 前置: 无 (叶子域, WebFlux 底座) | 引出: [[GW-3-路由谓词]] (谓词 SPI) + [[GW-2-过滤器链]] (匹配后执行) | 对照: Spring MVC HandlerMapping (阶段2)
> 🔴 A | 6 KP | [模式: 声明→运行时 + 责任链匹配 + 事件刷新 + DSL]
> Pass 2 闭环: q1(映射) q2(装配) q3(家族) q4(404) q5(DSL) q6(管理端点)

**读者处境**: `spring.cloud.gateway.routes[0].id=user-service, uri=lb://user-service, predicates: Path=/user/**` — 配置一行, 请求 `/user/123` 进来怎么被认领?多个路由谁先试?配置改了什么时候生效?网关的"认领"机制全貌在此。

### 1. 请求映射 — 首匹配胜出

场景: 请求进来, 网关怎么知道"这是哪个路由"?
源码路径:
- **getHandlerInternal** (RoutePredicateHandlerMapping.java:79-110): **management port 隔离** (L81-85, 管理端口请求不代理) → Mono.deferContextual → lookupRoute
- **匹配结果落属性** (L97-101): `exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, r); return webHandler` — 匹配的 Route 存 exchange, 处理器返回 FilteringWebHandler
- **lookupRoute** (L130-140): `routeLocator.getRoutes().filterWhen(route -> route.getPredicate().apply(exchange))` — **逐路由谓词过滤**, 测试前记录 GATEWAY_PREDICATE_ROUTE_ATTR (诊断)
- **首匹配语义**: filterWhen 第一个通过者胜出 (路由序 = order 序, q3)
- 无匹配 → 清属性/缓存体 → empty → **WebFlux 404** (L104-109)
关键设计 (q1): **顺序谓词过滤**: 请求逐一过路由谓词, 首个匹配胜出。**被放弃的方案: 最长前缀匹配 (servlet 风格)** — 谓词 SPI 让匹配逻辑完全可配置。 [模式: 责任链式匹配] [跨域: GW-3 谓词 SPI]

### 2. 装配链 — 声明 → 运行时

场景: YAML 里的路由配置怎么变成能跑的路由对象?
源码路径:
- **getRoutes** (RouteDefinitionRouteLocator.java:109-127): convertToRoute + **failOnRouteDefinitionError 容错** (L111-121): 坏配置路由 "will be ignored. Definition has invalid configs" — **单条坏路由不炸网关**
- **convertToRoute** (L129-135): `combinePredicates + getFilters → Route.async(routeDefinition).asyncPredicate(predicate).replaceFilters(filters).build()`
- **combinePredicates** (L198-209): 谓词流 → `reduce(AsyncPredicate.from(true), AsyncPredicate::and)` — **AND 组合**; 无谓词 = 全匹配
- **工厂+绑定** (L211-240): 按名找 RoutePredicateFactory → ConfigurationService 参数绑定 → `factory.applyAsync(config)`
关键设计 (q2): **两层模型**: RouteDefinition (声明) vs Route (运行时, 含预编译 AsyncPredicate + 实例化过滤器); 请求路径零配置解析。**被放弃的方案: 请求时实时解析配置** — 预编译让热路径纯执行。 [模式: 声明→运行时] [跨域: GW-2/GW-3 工厂族]

### 3. 定位器家族 — 组合、缓存与事件刷新

场景: 路由从哪来?改配置后什么时候生效?
源码路径:
- **CompositeRouteLocator** (CompositeRouteLocator.java:30-39): `delegates.flatMapSequential(RouteLocator::getRoutes)` — 编程式 + 配置式路由**保序合并**
- **CachingRouteLocator** (L55-80): **CacheFlux.lookup().onCacheMissResume(fetch)** — Reactor 缓存; `fetch(): ...sort(AnnotationAwareOrderComparator.INSTANCE)` (L62-63) — **匹配顺序 = order 序**
- **RouteRefreshListener** (RouteRefreshListener.java:46-80): **ContextRefreshed / RefreshScopeRefreshed (配置刷新) / InstanceRegistered + HeartbeatEvent (服务发现心跳)** (L57-64) → 发布 **RefreshRoutesEvent** (L75) → 清缓存 → 下次请求重装配
关键设计 (q3): **两级装饰链** (DefinitionLocator + RouteLocator) + 事件驱动刷新 — 配置/服务变更自动生效。**被放弃的方案: 每次请求实时装配** — 缓存让热路径零装配。 [Reactor: CacheFlux] [跨域: SCC 服务发现心跳联动]

### 4. 失败路径 — 优雅 404

场景: 没有路由匹配, 会怎样?
源码路径:
- **无匹配** (L104-109): `switchIfEmpty(empty.then(fromRunnable(清属性 + clearCachedRequestBody)))` — 清理后返回 empty
- **404 兜底**: empty handler → WebFlux 默认 404
- **validateRoute 扩展点** (L168-173): 默认空, 子类可加前置校验; 自定义 404 可经 GATEWAY_ROUTE_ATTR 区分 (无此属性 = 网关层 404, 非后端返回)
关键设计 (q4): 无匹配 = **优雅降级** (非异常路径): 清 exchange 属性/缓存体 → empty → 404。**被放弃的方案: 抛异常** — 404 是正常业务语义, 返回值路径更轻。 [HTTP: 404] [跨域: WebFlux 底座]

### 5. 编程式 DSL — 路由的第二种定义方式 (v2 补充)

场景: 不想写 YAML, 能用 Java 代码定义路由吗?
源码路径:
- **RouteLocatorBuilder** (RouteLocatorBuilder.java:33): `Builder.routes()` (L45) → **route(id, fn)** (L69): `fn.apply(new RouteSpec(this).id(id))` — 函数式定义; route(fn) 随机 id (L81)
- **build()** (RouteLocatorBuilder.java:91-93): `() -> Flux.fromIterable(routes).map(routeBuilder -> routeBuilder.build())` — **DSL 产出 = RouteLocator (Flux)** → 与配置式经 CompositeRouteLocator 合并 (q3)
- **GatewayFilterSpec** (GatewayFilterSpec.java:95): filter(GatewayFilter, order, forceOrder) (L122-134) + **类型化便捷方法**: circuitBreaker(Consumer<Config>) (L273)/retry(Consumer<RetryConfig>) (L710) — 1047 行全在
关键设计 (q5): **同一目标两条路径**: YAML (RouteDefinition) 与 Java DSL (RouteLocator) 汇合到同一 Route 模型 (q2), 无第二管线。**被放弃的方案: 只支持 YAML** — DSL 让复杂/动态路由可编程。 [模式: DSL] [跨域: GW-2 工厂族]

### 6. 路由管理端点 — 一行 curl 动态加路由 (v3.1 补充)

场景: 线上怎么临时加/删路由而不重启?
源码路径:
- **save** (AbstractGatewayControllerEndpoint.java:238): `@PostMapping("/routes/{id}")` + @RequestBody RouteDefinition — 运行时保存; 类注释示例 (L232): "POST :8080/admin/gateway/routes/apiaddreqhead uri=..."
- **refresh** (L178-179): `@PostMapping("/refresh")` — 手动触发 RefreshRoutesEvent (q3)
- **delete** (L327): `@DeleteMapping("/routes/{id}")`; **查询面** (L202-213): globalfilters/routefilters/routepredicates/combinedfilters
- 实现: GatewayControllerEndpoint (101) + Legacy (113) extends Abstract (342)
关键设计 (q6): **运行时路由治理**: 动态增删/刷新/查询 — 零停机流量调整 (灰度/应急)。**被放弃的方案: 只靠配置重启**。 [HTTP: REST 管理面] [对照: Envoy admin API]

### 核心悬念

"路由认领了请求 — 接下来呢?Route 里装配的过滤器链 (GatewayFilter) 与全局过滤器 (GlobalFilter) 怎么执行?" 下一域 [[GW-2-过滤器链]]。

### 负面空间 (不做)

1. 不写 WebFlux HandlerMapping 基类机制 (阶段2 已讲)
2. 不写 RouteDefinitionRepository 存储细节 (Redis 版对照)
3. 不写管理端口/actuator 端点细节
4. 不写 GatewayProperties 配置项穷举
5. 不写谓词工厂实现细节 (GW-3 专属)
6. 不写服务发现路由 (DiscoveryClientRouteDefinitionLocator, discovery/ — GW-4 关联)
