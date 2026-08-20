# 闭环笔记 GW-6-q2 — RateLimiter SPI + Redis 令牌桶: 三参数 + Lua 脚本

假设: RateLimiter SPI 返回 (allowed + headers); RedisRateLimiter 用 Lua 脚本原子实现令牌桶 — replenishRate (每秒)/burstCapacity (突发)/requestedTokens (每请求), Redis 故障降级放行。

验证过程:
- **SPI** (RateLimiter.java): `isAllowed(routeId, key)` → Response (allowed + headers) — 工厂消费 (q1)
- **三参数** (RedisRateLimiter.java:241-247): 注释: "How many requests per second... replenishRate" / "How much bursting... burstCapacity" / "How many tokens are requested per request... requestedTokens"
- **Lua 执行** (L253-258): `redisTemplate.execute(script, keys, scriptArgs)` — 注释: "allowed, tokens_left = redis.eval(SCRIPT, keys, args)" — **原子性** (Redis 单线程脚本)
- **结果** (L262-274): allowed = results[0]==1L; tokensLeft → Response + headers (剩余额度 X-RateLimit-*)
- **双重容错** (L259-261, L282-287): 脚本异常 → 放行 (1L/-1L); **Redis 不可用 → 放行** — 注释: "We don't want a hard dependency on Redis to allow traffic... Stripe's observed failure rate is 0.01%" — **限流器故障降级放行** (可用性 > 限流精确)
- 负载配置 (L290-292): 按 routeId 加载 (defaultConfig 兜底)
- REDIS_SCRIPT_NAME (L65): RedisScript bean
- 测试: RedisRateLimiterTests.redisRateLimiterWorks (L88)/redisRateLimiterWorksForLowRates (L133)

代码类型: Algorithmic (令牌桶)

结论: Redis 令牌桶 = **Lua 原子脚本 + 三参数**: 每秒补充 + 突发容量 + 每请求消耗; **故障降级是刻意的**: Redis 挂时放行 (注释引用 Stripe 0.01% 失败率) — 网关可用性优先。**被放弃的方案: Java 内存桶** — 多实例网关需共享状态 (Redis); 故障降级 vs 严格限流的权衡选可用性。 [跨域: GW-2 链 (429 中断); Redis] [算法: 令牌桶] (RedisRateLimiter.java:241-292; RateLimiter.java)
