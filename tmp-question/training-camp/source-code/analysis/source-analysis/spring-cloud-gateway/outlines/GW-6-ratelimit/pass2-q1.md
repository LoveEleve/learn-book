# 闭环笔记 GW-6-q1 — 工厂编排: KeyResolver → RateLimiter → 429/放行

假设: 限流工厂是编排器: 解析 key → 查限流器 → 允许放行 / 拒绝 429 (可配状态码); EMPTY_KEY 有独立策略。

验证过程:
- **编排** (RequestRateLimiterGatewayFilterFactory.java:93-130): `resolver.resolve(exchange).defaultIfEmpty(EMPTY_KEY).flatMap(key -> ...)` (L99)
- **EMPTY_KEY 策略** (L97-105): `denyEmpty` (可配) → setResponseStatus(emptyKeyStatus) + setComplete (L99-101); 否则直接放行 (L103) — **空键独立处理**
- **限流判定** (L112-122): `limiter.isAllowed(routeId, key)` (L112) → **响应头写 exchange** (L115-117) → `response.isAllowed()` (L118) → chain.filter 或 **setResponseStatus + setComplete** (L122)
- **默认状态码 429** (L138): `HttpStatus.TOO_MANY_REQUESTS` (Config 默认)
- routeId 来源 (L111-116): config 或 GATEWAY_ROUTE_ATTR
- 响应头传播: 限流器返回的 headers 全量写入 (剩余额度等)

代码类型: Glue (编排)

结论: 限流工厂 = **两段编排**: key 解析 (EMPTY_KEY 独立策略) → 限流判定 (429 中断链 + 头传播); 限流器实现细节 SPI 化。**被放弃的方案: 工厂内嵌算法** — SPI 让 Redis/Bucket4j 可插拔。 [跨域: GW-2 链中断语义 (setComplete 不调下游)] [HTTP: 429] (RequestRateLimiterGatewayFilterFactory.java:93-138)
