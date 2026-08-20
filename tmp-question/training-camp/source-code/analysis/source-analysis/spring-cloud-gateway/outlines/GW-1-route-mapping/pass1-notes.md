# GW-1 Pass 1 扫描笔记 — 路由定位

> 日期: 2026-08-16 | 版本: 4.3.2 | 🔴 A | 模块: handler/RoutePredicateHandlerMapping (194) + route/ (15 文件)

## 继承树/调用图

```
GatewayAutoConfiguration → RoutePredicateHandlerMapping (194, extends AbstractHandlerMapping)
  ├── getHandlerInternal (L79): management port 检查 → lookupRoute
  │     └── lookupRoute (L130): routeLocator.getRoutes().filterWhen(predicate.apply)
  │           → 匹配: GATEWAY_ROUTE_ATTR + 返回 webHandler (FilteringWebHandler)
  │           → 不匹配: 清缓存 + empty (404)
  ├── RouteLocator 家族 (route/ 15):
  │     ├── RouteDefinitionRouteLocator (233): getRoutes → convertToRoute
  │     ├── CachingRouteLocator / CompositeRouteLocator
  │     ├── CachingRouteDefinitionLocator / CompositeRouteDefinitionLocator
  │     ├── InMemoryRouteDefinitionRepository / RedisRouteDefinitionRepository
  │     └── RouteRefreshListener (event/)
  └── Route (44, implements Ordered): id/uri/predicate/filters/order/metadata
```

## 基本元素分解

1. **RoutePredicateHandlerMapping**: WebFlux 处理器映射 — 请求 → 谓词匹配 → Route → FilteringWebHandler
2. **Route**: 路由定义 (id/uri/predicate/filters/order) — builder(RouteDefinition) 转换
3. **RouteLocator 家族**: 定义→路由的装配与缓存 (Definition vs Runtime 两层)
4. **RouteDefinition**: 配置形态 (YAML/properties → RouteDefinition)

## 标记问题 (4)

1. **Q1 映射流程**: getHandlerInternal → lookupRoute 的谓词过滤 — 请求怎么被匹配到 Route?多路由顺序?
2. **Q2 装配链**: RouteDefinition → Route 转换 (convertToRoute/combinePredicates/过滤器装配)
3. **Q3 定位器家族**: CompositeRouteLocator/CachingRouteLocator 组合与缓存; RouteRefreshListener 刷新机制
4. **Q4 失败路径**: 无匹配路由 → 404 细节 (NoRouteFound/GatewayProperties fallback)

## 已读测试

- RoutePredicateHandlerMappingTests: lookupRouteFromSyncPredicates (L43)/lookupRouteFromAsyncPredicates (L66)
