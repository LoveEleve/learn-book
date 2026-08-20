# 闭环笔记 GW-1-q3 — 定位器家族: 组合 + 缓存 + 事件刷新

假设: 路由定位分两层 (Definition/Route), RouteLocator 家族提供组合 (Composite) 与缓存 (Caching), 刷新由事件驱动 (RefreshRoutesEvent)。

验证过程:
- **CompositeRouteLocator** (CompositeRouteLocator.java:30-39): `delegates.flatMapSequential(RouteLocator::getRoutes)` — 多定位器**保序合并** (编程式 + 配置式路由共存)
- **CachingRouteLocator** (CachingRouteLocator.java:55-71): **CacheFlux.lookup(cache, CACHE_KEY, Route.class).onCacheMissResume(this::fetch)** — Reactor 缓存; `fetch(): delegate.getRoutes().sort(AnnotationAwareOrderComparator.INSTANCE)` (L62-63) — **路由按 order 排序 → 匹配顺序 = order 序** (与 q1 首匹配胜出衔接); refresh(): cache.remove (L79-80)
- **RouteRefreshListener** (RouteRefreshListener.java:46-80): ContextRefreshedEvent (排除 management/LoadBalancerClientFactory 上下文, L48-53) / **RefreshScopeRefreshedEvent** (配置刷新) / **InstanceRegisteredEvent + HeartbeatEvent** (服务发现心跳 → resetIfNeeded, L57-64) → 统一 `publishEvent(new RefreshRoutesEvent)` (L75) → CachingRouteLocator 清缓存 → **下次请求重新装配**
- 定义层: CompositeRouteDefinitionLocator/CachingRouteDefinitionLocator + InMemoryRouteDefinitionRepository/RedisRouteDefinitionRepository (RouteDefinition 存储)

代码类型: Glue (定位器组合)

结论: 路由定位是**两级装饰链**: 定义层 (Repository → DefinitionLocator) + 运行层 (RouteLocator → CachingRouteLocator 排序缓存); 刷新全走事件 (RefreshRoutesEvent), 触发源包括配置刷新与服务发现心跳 — **网关路由动态更新** (配置/服务变更自动生效)。**被放弃的方案: 每次请求实时装配路由** — 缓存 + 事件刷新让请求路径零装配开销。 [跨域: GW-2 过滤器链消费缓存路由; SCC 服务发现心跳联动] [Reactor: CacheFlux] (CompositeRouteLocator.java:30-39; CachingRouteLocator.java:55-80; RouteRefreshListener.java:46-80)
