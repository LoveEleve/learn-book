# GW-6 限流 — 知识规划 (KP)

> 域级: 🟡 B | 模块: filter/factory/RequestRateLimiterGatewayFilterFactory + filter/ratelimit/ (6)
> 日期: 2026-08-16 | 版本: 4.3.2 | Pass 2 闭环: q1(编排) q2(令牌桶) q4(KeyResolver) — **3 闭环覆盖 4 问**

## 一、机制提取 (逐源)

### M1 工厂编排 (q1)
- RequestRateLimiterGatewayFilterFactory (93-138): KeyResolver.resolve → EMPTY_KEY 策略 (denyEmpty/emptyKeyStatus, L97-105) → isAllowed (L112) → 头传播 (L115-117) → 429 setComplete (L122); 默认状态码 TOO_MANY_REQUESTS (L138)
- routeId 来源 (L111-116)

### M2 Redis 令牌桶 (q2)
- 三参数 (RedisRateLimiter 241-247): replenishRate/burstCapacity/requestedTokens
- Lua 脚本 (L253-258): redisTemplate.execute — 原子性
- **双重容错降级放行** (L259-261,282-287): "We don't want a hard dependency on Redis to allow traffic... Stripe's observed failure rate is 0.01%"
- 结果 (L262-274): allowed + tokensLeft → Response + 头

### M3 KeyResolver (q4)
- PrincipalNameKeyResolver (23-32): 默认按认证主体; 自定义 SPI
- Bucket4jRateLimiter (43-75): bucket4j 库第二实现 (对照)

## 二、聚合分级

| 级别 | 机制 |
|---|---|
| P1 | M2 Lua 令牌桶 + 故障降级 / M1 编排语义 |
| P2 | M3 KeyResolver SPI |

## 三、叙事线

场景: `RequestRateLimiter` 一行配置, 用户怎么被限流?读者疑问链: 工厂怎么编排 (M1) → Redis 桶怎么算 (M2) → 按什么键 (M3)。
