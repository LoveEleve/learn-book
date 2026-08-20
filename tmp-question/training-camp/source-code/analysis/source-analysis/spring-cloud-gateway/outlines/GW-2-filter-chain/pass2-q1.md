# 闭环笔记 GW-2-q1 — 链装配: 全局+路由过滤器统一排序

假设: 执行链 = globalFilters + 路由过滤器合并, 统一按 order 排序; GlobalFilter 经适配器与 OrderedGatewayFilter 包装进链。

验证过程:
- **FilteringWebHandler** (FilteringWebHandler.java:55-79): implements WebHandler + **ApplicationListener<RefreshRoutesEvent>** (L56 — 刷新联动); 构造时 `loadFilters(globalFilters)` (L76)
- **loadFilters** (L79-93): GlobalFilter → **GatewayFilterAdapter** (L81) + **Ordered 提取**: `filter instanceof Ordered` → order (L82-84) / 否则 @Order 注解 (L86-89) → **OrderedGatewayFilter** 包装 (L88,93) — 统一 order 语义
- **handle** (L104-111): `getRequiredAttribute(GATEWAY_ROUTE_ATTR)` (L105) → getCombinedFilters → `new DefaultGatewayFilterChain(combined).filter(exchange)` (L110)
- **getAllFilters** (L124-130): `combined = new ArrayList<>(globalFilters); combined.addAll(gatewayFilters); AnnotationAwareOrderComparator.sort(combined)` — **全局+路由统一排序**

代码类型: Glue (链装配)

结论: 装配 = **两集合合并 + 统一排序**: 全局过滤器 (框架级) 与路由过滤器 (配置/DSL 级) 进同一条链, order 决定执行序; GlobalFilter 经适配器获得 GatewayFilter 形态。**被放弃的方案: 两条独立链** — 合并排序让全局与路由过滤器可交错 (如限流在转发前)。 [跨域: GW-1 route.getFilters 装配; v3 配置绑定] [模式: 装饰器+排序] (FilteringWebHandler.java:55-130)
