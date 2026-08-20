# 闭环笔记 q2: Cache 包装 — Spring Cache 语义映射到 RMapCache

## 假设
RedissonCache implements Spring Cache 接口, 内部包装 RMapCache (或 RMap): get→map.get; put→fastPut(ttl); evict→fastRemove; 并处理 NullValue/指标。

## 验证过程
- RedissonCache (redisson-spring-cache/.../RedissonCache.java:41): `implements Cache` + 字段 `RMapCache mapCache` (L43)
- get (L84-89): `map.get(key)` → toValueWrapper
- put (L123-137):
  - allowNullValues=false + null → `map.fastRemove` (L124-127, null 即删)
  - mapCache 存在 → `mapCache.fastPut(key, value, ttl, maxIdle)` (L132, 带 TTL)
  - 否则 → `map.fastPut` (L135)
  - addCachePut() 指标 (L136)
- putIfAbsent (L138-153): mapCache.putIfAbsent(key,value,ttl,maxIdle)
- evict (L156-159): `map.fastRemove(key)` → addCacheEvictions(delta)
- getNativeCache (L76): 返回 RMap (暴露 native)
- NullValue (NullValue.java): null 值包装 (Spring Cache 语义)

## 代码类型
Glue (接口适配) — Spring Cache 抽象 → Redisson RMapCache

## 跨域关联
- RD-5 (RMapCache) → 底层载体 (TTL 五结构)
- s19-cacheable (@Cacheable 原理) → Cache 接口的调用方
- s77-boot-cache (CacheAutoConfiguration) → CacheManager 装配
- 面试点: "@Cacheable 背后的 Cache 是谁实现的?"

## 结论
RedissonCache = Spring Cache 适配器: 包装 RMapCache (TTL 语义), get/put/evict 映射到 map 操作, null 语义 + 指标钩子。让 @Cacheable 无感用上 Redisson 的分布式缓存。
源码位置: RedissonCache.java:41-159, NullValue.java