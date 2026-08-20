# 闭环笔记 GW-1-q5 — 编程式 DSL: GatewayFilterSpec 与 RouteLocatorBuilder (v2 补充)

假设: DSL 是路由的第二种定义方式 (Java 代码 vs YAML): RouteLocatorBuilder 收集路由构建器, 产出 Flux 型 RouteLocator; GatewayFilterSpec 提供类型化过滤器便捷方法。

验证过程:
- **RouteLocatorBuilder** (RouteLocatorBuilder.java:33): `Builder.routes()` (L45) → **route(id, fn)** (L69): `fn.apply(new RouteSpec(this).id(id))` — 函数式路由定义 (PredicateSpec → Buildable<Route>); route(fn) 随机 id (L81)
- **build()** (RouteLocatorBuilder.java:91-93): `() -> Flux.fromIterable(this.routes).map(routeBuilder -> routeBuilder.build())` — **DSL 产出 = RouteLocator (Flux 流)** → 与配置式路由经 CompositeRouteLocator 合并 (q3)
- **GatewayFilterSpec** (GatewayFilterSpec.java:95): extends UriSpec — **filter(GatewayFilter, order, forceOrder)** (L122-134, 显式 order) + **类型化便捷方法**: circuitBreaker(Consumer<Config>) (L273)/retry(Consumer<RetryConfig>) (L710) — 与工厂 Config 直接绑定 (v3 配置体系消费)
- 规模: GatewayFilterSpec 1047 行 (39 种工厂的便捷方法全在此)
- 对照: YAML 配置 (RouteDefinition) 与 Java DSL (RouteLocator) 是"同一目标的两条路径" — 都会走到 Route 装配 (q2)

代码类型: Interface (DSL 面)

结论: DSL = **类型化路由定义**: 函数式 (PredicateSpec→Route) + 过滤器便捷方法 (类型安全, IDE 提示, 免 YAML 字符串); 产出与配置式同构 (RouteLocator → 合并 → 缓存)。**被放弃的方案: 只支持 YAML** — Java DSL 让复杂路由 (动态逻辑/条件) 可编程; 两条路径汇合到同一 Route 模型 (q2), 无第二条管线。 [跨域: GW-2 过滤器工厂族; v3 配置绑定体系] [模式: DSL/流式构建] (RouteLocatorBuilder.java:45-103; GatewayFilterSpec.java:95-134,273,710)
