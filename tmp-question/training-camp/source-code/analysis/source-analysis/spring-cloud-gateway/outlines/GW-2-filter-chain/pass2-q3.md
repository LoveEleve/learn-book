# 闭环笔记 GW-2-q3 — 缓存与刷新: routeFilterMap + RefreshRoutesEvent

假设: 路由→过滤器链有缓存 (routeFilterMap), 刷新事件清空 — 与 GW-1 的 CachingRouteLocator 联动。

验证过程:
- **缓存结构** (FilteringWebHandler.java:61): `ConcurrentHashMap<Route, List<GatewayFilter>> routeFilterMap` — 路由→已排序过滤器链
- **routeFilterCacheEnabled** (L76-77, 构造参数): 默认 false (L71-72 @Deprecated 构造); 开启后 getCombinedFilters 走 computeIfAbsent (L115-119)
- **onApplicationEvent** (L97-101): `if (routeFilterCacheEnabled) routeFilterMap.clear()` — **RefreshRoutesEvent 清链缓存** (GW-1 q3 的刷新事件消费端)
- **getAllFilters** (L124-130): 缓存 miss 时合并排序 (q1)
- 联动链: 路由变更 → RefreshRoutesEvent → CachingRouteLocator 清路由缓存 (GW-1) + FilteringWebHandler 清链缓存 (GW-2) — **同一事件双端消费**

代码类型: Implementation (缓存)

结论: 链缓存 = **路由级缓存 + 事件失效**: 缓存 miss 才合并排序 (q1); RefreshRoutesEvent 同时清 GW-1 路由缓存与本域链缓存 — 配置变更全链路一致。**被放弃的方案: 每次请求重排** — 排序 O(n log n) 每请求做; 缓存让热路径 O(1) 取链。 [跨域: GW-1 q3 事件面; 一致性设计] [并发: ConcurrentHashMap] (FilteringWebHandler.java:61,89-93,115-130)
