# RD-7 篇2 — cache: Spring Cache 的 Redisson 落地

> 前置: [[RD-7-篇1]] (starter) + [[s77-boot-cache]] (CacheAutoConfiguration) | 复用: [[s19-cacheable]] (@Cacheable 原理) | 对照: [[rd5-rmap]] (RMapCache 载体) [[rd6-localcachedmap]] (本地缓存) | 引出: [[RD-7-篇3]] (data/transaction)
> 🟡 B | 2 KP | [模式: 适配器 + 双载体 + 按名配置]
> Pass 2 闭环: q2(Cache 包装) q3(getCache 双路)

**读者处境**: 你在方法上写 `@Cacheable(cacheNames="users")` — 这个 Cache 到底是谁?Boot 默认 ConcurrentMapCache (本地, 重启丢) 或 RedisCache (Lettuce)。配了 RedissonSpringCacheManager 后, @Cacheable 的数据就进了 RMapCache (分布式, 带 TTL)。这篇拆 Cache 接口的 Redisson 适配器 (RedissonCache 包装 RMapCache) 和 CacheManager 的 getCache 双载体 (RMap vs RMapCache)。

### 概念依赖链
q2(Cache 包装) ← q3(getCache 双路) — 先讲单个 Cache 怎么实现, 再讲 Manager 怎么按配置选载体。

### 核心悬念
"@Cacheable 的数据存哪了?Redisson 怎么让它变分布式且带 TTL?"

### 叙事顺序
1. 问题引入: @Cacheable 背后的 Cache
2. Cache 包装 (q2) — RedissonCache 包装 RMapCache
3. getCache 双路 (q3) — config.ttl>0 → RMapCache else RMap
4. 收束: "Spring 缓存抽象 + Redisson 载体"

### 1. Cache 包装 — @Cacheable 的 Redis 后端

场景: Cache 接口怎么用 Redisson?
源码路径:
- `RedissonCache implements Cache` (RedissonCache.java:41) + 字段 `RMapCache mapCache` (L43)
- get (RedissonCache.java:84-89): `map.get(key)` → toValueWrapper
- put (RedissonCache.java:123-137):
  - allowNullValues=false + null → `map.fastRemove` (RedissonCache.java:124-127)
  - mapCache → `mapCache.fastPut(key, value, ttl, maxIdle)` (RedissonCache.java:132, 带 TTL)
  - 否则 → `map.fastPut` (RedissonCache.java:135)
- evict (RedissonCache.java:156-159): `map.fastRemove(key)`
- NullValue (NullValue.java): null 缓存语义
关键设计 (q2): Cache 适配器 = 包装 RMapCache, get/put/evict 映射到 map 操作, TTL 走 fastPut 参数。[模式: 接口适配]
数据流: @Cacheable → Cache.get → mapCache.get → Redis。

### 2. getCache 双路 — 按配置选载体

场景: 每个 Cache 都能单独配 TTL 吗?
源码路径:
- `RedissonSpringCacheManager implements CacheManager` (RedissonSpringCacheManager.java:45) + configMap (L59, Cache 名 → CacheConfig)
- getCache(name) (RedissonSpringCacheManager.java:216-235): 
  - **createMap** (RedissonSpringCacheManager.java:238): `redisson.getMap(name)` — RMap (无 TTL)
  - **createMapCache** (RedissonSpringCacheManager.java:259): `getMapCache(name, config)` → RMapCache — TTL/maxIdle
- CacheConfig (CacheConfig.java:38-46): ttl/maxIdleTime/maxSize
- **configLocation** (RedissonSpringCacheManager.java:62,110): YAML 文件路径加载 CacheConfig (CacheConfigSupport.fromYAML L95-165) — 外部配置缓存 TTL
- **指标计数** (RedissonCache.java:338-355): puts/hits/misses/evictions AtomicLong 计数器 → RedissonCacheMeterBinderProvider (Micrometer 绑定)
- 选型: config.ttl/maxIdle>0 → RMapCache; 否则 RMap
关键设计 (q3): 按名配置: 每个 Cache 独立 CacheConfig (可 YAML 外部加载), 有 TTL → RMapCache, 无 → RMap; 计数供 Micrometer。[模式: 按配置选载体]
数据流: @Cacheable(name="users") → getCache → users 的 config.ttl>0? RMapCache : RMap。

### 负面空间 — Cache 集成刻意不做的事

- **不做多级缓存**: Spring Cache 直接落 RMapCache, 无本地层 (要本地缓存用 RLocalCachedMap 另配)
- **不做方法级过期推导**: TTL 靠 CacheConfig 静态配置, 不解析 @Cacheable 动态 TTL
- **不替代 @CacheEvict 语义**: evict 只清 Cache 对应键, 不联动其他
- **不做缓存预热**: 启动不预载, 首次访问 miss
- **不自动清理 NullValue**: null 条目按 TTL 走 (Config 决定)

→ 引出: RedisTemplate 底层连接和 @Transactional 事务怎么接?→ [[RD-7-篇3]]