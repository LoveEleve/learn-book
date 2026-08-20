# 闭环笔记 GW-6-q4 — KeyResolver: 默认按用户 + 第二实现对照

假设: 限流键由 KeyResolver SPI 决定 — 默认 PrincipalNameKeyResolver (按认证用户); Bucket4jRateLimiter 是第二实现 (bucket4j 库, 对照 Redis)。

验证过程:
- **PrincipalNameKeyResolver** (PrincipalNameKeyResolver.java:23-32): BEAN_NAME = "principalNameKeyResolver" (L28) + `resolve: exchange.getPrincipal().flatMap(p -> Mono.justOrEmpty(p.getName()))` (L31-32) — **默认按认证主体限流**; 无 principal → empty → EMPTY_KEY (q1 处理)
- **自定义**: 实现 KeyResolver 接口 (按 IP/header 等) → 配置 keyResolver
- **Bucket4jRateLimiter** (Bucket4jRateLimiter.java:43-75): **bucket4j 库** (Bandwidth L27/BucketConfiguration L32/**AsyncBucketProxy** L32) — 分布式桶经 AsyncProxyManager (L61,70) — 第二实现 (对照面)
- 工厂选择: getOrDefault(config.rateLimiter, defaultRateLimiter) (q1 L94)

代码类型: Interface (SPI)

结论: 限流键 = **SPI 化**: 默认按用户 (PrincipalName); 自定义按 IP/参数等; 限流器双实现 (Redis Lua/Bucket4j) 可插拔。**被放弃的方案: 固定 IP 限流** — 认证用户限流更贴合业务 (匿名场景可换 IP)。 [跨域: WebFlux Principal; GW-2 工厂体系] [模式: SPI] (PrincipalNameKeyResolver.java:23-32; Bucket4jRateLimiter.java:43-75)
