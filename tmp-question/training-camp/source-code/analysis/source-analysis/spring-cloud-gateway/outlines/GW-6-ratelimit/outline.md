# GW-6 限流 — 429 的分界线: KeyResolver 与 Redis 令牌桶

> 前置: [[GW-2-过滤器链]] (工厂体系) | 引出: [[GW-7-熔断与重试]] (容错面) | 对照: Sentinel (阶段5.9) + Redis 令牌桶
> 🟡 B | 3 KP | [模式: SPI 编排 + 原子脚本 + 故障降级]
> Pass 2 闭环: q1(编排) q2(令牌桶) q4(KeyResolver) — 4/4 全闭环

**读者处境**: `filters: - RequestRateLimiter=redis-rate-limiter, 10, 20` — 一行配置: 每秒 10 个, 突发 20。请求怎么被计数?超过怎么办?Redis 挂了会怎样?按什么维度限流?

### 1. 工厂编排 — KeyResolver → RateLimiter → 429

场景: 限流过滤器怎么工作?
源码路径:
- **编排** (RequestRateLimiterGatewayFilterFactory.java:93-130): `resolver.resolve(exchange).defaultIfEmpty(EMPTY_KEY)` (L99) → 查限流器
- **EMPTY_KEY 策略** (L97-105): **denyEmpty 默认 true** (L55) → 空键拒绝 (**emptyKeyStatus 默认 403 FORBIDDEN**, L57-58) 或放行
- **判定** (L112-122): `isAllowed(routeId, key)` (L112) → 响应头写 exchange (L115-117) → `response.isAllowed()` (L118) → 放行或 **429 setComplete** (L122, 默认状态码 L138)
关键设计 (q1): **两段编排**: key 解析 → 限流判定; 空键独立策略; **429 中断链** (GW-2 语义: 不调下游)。**被放弃的方案: 工厂内嵌算法** — SPI 让 Redis/Bucket4j 可插拔。 [HTTP: 429]

### 2. Redis 令牌桶 — Lua 原子脚本 + 故障降级

场景: 令牌桶怎么算?Redis 挂了?
源码路径:
- **三参数** (RedisRateLimiter.java:241-247): replenishRate (每秒补充)/burstCapacity (突发容量)/requestedTokens (每请求消耗)
- **Lua 脚本** (L253-258): `redisTemplate.execute(script, keys, args)` — "allowed, tokens_left = redis.eval(...)" — **Redis 单线程保证原子性**;脚本核心 (request_rate_limiter.lua): `filled_tokens = min(capacity, last + delta*rate)` (L20) / `allowed = filled_tokens >= requested` (L21) / **拒绝不扣减** (L22) / **TTL = fill_time*2 键过期** (L14-15,24-26, 内存治理) / `redis.replicate_commands()` (L1, 脚本确定性)
- **结果** (L262-274): allowed + tokensLeft → Response (**4 个 X-RateLimit 头**, L70-85: Remaining/Replenish-Rate/Burst-Capacity/Requested-Tokens)
- **故障降级** (L282-287): **Redis 不可用 → 放行** — 注释: "We don't want a hard dependency on Redis to allow traffic... Stripe's observed failure rate is 0.01%" — **可用性 > 限流精确**
关键设计 (q2): **Lua 原子令牌桶**: 跨实例共享状态 (多网关一致性); **故障降级是刻意设计** (注释引用 Stripe 数据); **默认 bean 无内置速率** (3 参构造, GatewayRedisAutoConfiguration.java:71 — replenishRate/burstCapacity 必须配置)。**被放弃的方案: Java 内存桶** — 多实例不一致。 [算法: 令牌桶] [跨域: 对照 Sentinel]

### 3. KeyResolver — 按什么限流

场景: 按用户还是 IP?
源码路径:
- **PrincipalNameKeyResolver** (PrincipalNameKeyResolver.java:23-32): BEAN_NAME (L28) + `getPrincipal().map(getName)` (L31-32) — **默认按认证用户**
- **自定义**: KeyResolver SPI (按 IP/header)
- **第二实现**: Bucket4jRateLimiter (Bucket4jRateLimiter.java:43-75, bucket4j 库 AsyncBucketProxy)
关键设计 (q4): **限流键 SPI 化**: 默认按用户 (业务语义); 匿名场景换 IP。**被放弃的方案: 固定 IP** — 认证限流更贴合业务。 [模式: SPI]

### 核心悬念

"限流挡住过量 — 上游故障呢?熔断 (断路器) 和重试 (Retry) 怎么组合?" 下一域 [[GW-7-熔断与重试]]。

### 负面空间 (不做)

1. 不写 Lua 脚本逐行 (令牌桶算法细节)
2. 不写 Bucket4j 库细节 (对照面)
3. 不写 Redis 连接/集群细节
4. 不写 KeyResolver 自定义示例穷举
5. 不写 X-RateLimit 头协议规范
6. 不写 Sentinel 对照细节 (阶段5.9)
