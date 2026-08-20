# GW-6 Pass 1 扫描笔记 — 限流

> 日期: 2026-08-16 | 版本: 4.3.2 | 🟡 B | 模块: filter/factory/RequestRateLimiterGatewayFilterFactory + filter/ratelimit/ (6 文件: RateLimiter/AbstractRateLimiter/RedisRateLimiter/Bucket4jRateLimiter/KeyResolver/PrincipalNameKeyResolver)

## 继承树/调用图

```
RequestRateLimiterGatewayFilterFactory (38, 工厂):
  ├── KeyResolver.resolve(exchange) → key (L93) — EMPTY_KEY 处理 (denyEmpty 可配)
  ├── RateLimiter.isAllowed(routeId, key) (L112) → 响应头写 exchange (L115-117)
  ├── 允许 → chain.filter; 拒绝 → setComplete + 状态码 (默认 429, L138)
  └── RateLimiter SPI (ratelimit/):
        ├── RedisRateLimiter (55): RedisScript (L65, SCRIPT) + replenishRate/burstCapacity
        ├── Bucket4jRateLimiter (桶算法, 对照)
        └── AbstractRateLimiter (27): 配置绑定 (ConfigurationService)
```

## 基本元素分解

1. **工厂编排**: KeyResolver → RateLimiter → 允许/拒绝 (429)
2. **RateLimiter SPI**: isAllowed(routeId, key) 返回 Response (allowed + headers)
3. **RedisRateLimiter**: Lua 脚本令牌桶 (replenishRate/burstCapacity)
4. **KeyResolver**: 限流键 (默认 PrincipalNameKeyResolver)

## 标记问题 (4)

1. **Q1 工厂编排**: KeyResolver → limiter → 429/放行; EMPTY_KEY 语义 (denyEmpty/emptyKeyStatus)
2. **Q2 RateLimiter SPI**: isAllowed 契约 (routeId+key → Response)
3. **Q3 RedisRateLimiter**: Lua 令牌桶 (replenishRate/burstCapacity/requestedTokens)
4. **Q4 KeyResolver**: 默认解析器 (PrincipalName) + 自定义

## 已读测试

- RedisRateLimiterTests (L88): redisRateLimiterWorks/redisRateLimiterWorksForLowRates (L133)
- Bucket4jRateLimiterTests/LuaScriptTests 存在
