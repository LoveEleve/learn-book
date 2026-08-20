# GW-2 Pass 1 扫描笔记 — 过滤器链

> 日期: 2026-08-16 | 版本: 4.3.2 | 🔴 A | 模块: handler/FilteringWebHandler (196) + filter/ (GlobalFilter 18 实现 + GatewayFilterChain) + filter/factory (39 种) + support/ConfigurationService (256) + ShortcutConfigurable (306)

## 继承树/调用图

```
FilteringWebHandler (55, WebHandler + ApplicationListener<RefreshRoutesEvent>)
  ├── handle (L104): GATEWAY_ROUTE_ATTR → getCombinedFilters → DefaultGatewayFilterChain.filter
  ├── getAllFilters (L124-130): globalFilters + route.getFilters() 合并 + AnnotationAwareOrderComparator.sort
  ├── routeFilterMap (L61): Route→过滤器缓存 (刷新事件清空 L89-93)
  ├── DefaultGatewayFilterChain (L132-168): 不可变索引递归链 + Mono.defer 惰性
  └── GatewayFilterAdapter (L172-185): GlobalFilter→GatewayFilter 适配
GatewayFilterFactory 族 (39 种) + ConfigurationService (256, Binder) + ShortcutConfigurable (306, ShortcutType)
```

## 基本元素分解

1. **FilteringWebHandler**: 全局+路由过滤器合并排序 → 链执行
2. **DefaultGatewayFilterChain**: 反应式责任链 (递归 + Mono.defer)
3. **GatewayFilterAdapter**: GlobalFilter 适配
4. **配置绑定体系**: ConfigurationService (Binder) + ShortcutConfigurable (短路语法)
5. **缓存**: routeFilterMap + RefreshRoutesEvent

## 标记问题 (5)

1. **Q1 链装配**: 全局+路由过滤器合并/排序 (AnnotationAwareOrderComparator) — 顺序语义 (OrderedGatewayFilter)
2. **Q2 链执行**: DefaultGatewayFilterChain 递归 + Mono.defer — 反应式责任链机制
3. **Q3 缓存与刷新**: routeFilterMap 缓存 + RefreshRoutesEvent 联动 (GW-1 q3)
4. **Q4 配置绑定**: ConfigurationService (Binder) + ShortcutConfigurable ShortcutType (DEFAULT/GATHER_LIST)
5. **Q5 工厂族**: GatewayFilterFactory SPI + OrderedGatewayFilter + 39 种分类

## 已读测试

- FilteringWebHandlerCacheEnabledIntegrationTests: filteringWebHandlerCacheEnabledWorks (L55)
- filter/ 测试族: NettyRoutingFilterTests/ForwardRoutingFilterTests 等
