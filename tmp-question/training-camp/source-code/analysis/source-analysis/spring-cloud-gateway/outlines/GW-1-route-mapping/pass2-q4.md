# 闭环笔记 GW-1-q4 — 失败路径: 无匹配 → 404 (WebFlux 兜底)

假设: 无路由匹配时 RoutePredicateHandlerMapping 返回 empty, 404 由 WebFlux 层兜底 (NotFound 异常面)。

验证过程:
- **无匹配处理** (RoutePredicateHandlerMapping.java:104-109): `switchIfEmpty(Mono.empty().then(Mono.fromRunnable(() -> { exchange.getAttributes().remove(GATEWAY_PREDICATE_ROUTE_ATTR); ServerWebExchangeUtils.clearCachedRequestBody(exchange); ... })))` — **返回 empty + 清理 exchange 属性与缓存请求体**
- **404 兜底**: empty handler → WebFlux 默认 404 (无自定义映射); 异常面: support/NotFoundException (网关内部错误面)
- **validateRoute** (L168-173): 默认空实现 — 子类可加前置校验 (如 URL 映射前置条件)
- 管理端口隔离 (L81-85): management port 上的请求不代理 (返回 empty → 管理端点处理)
- 测试: 无匹配用例 (RoutePredicateHandlerMappingTests)

代码类型: Glue (失败兜底)

结论: 无匹配路由 = **优雅降级**: 清属性/缓存体 → empty → WebFlux 404; 不抛异常 (可扩展为自定义 404 处理)。**被放弃的方案: 抛异常走错误处理链** — 404 是正常业务语义, 走 handler 返回值路径更轻。 [跨域: WebFlux 层 (阶段2)] [HTTP: 404 语义] (RoutePredicateHandlerMapping.java:104-109,168-173)
