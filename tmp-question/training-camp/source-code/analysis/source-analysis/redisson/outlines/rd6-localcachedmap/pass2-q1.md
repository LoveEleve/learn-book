# 闭环笔记 q1: 双层级读路径 — 本地 miss 的两种回填

## 假设
get 先查本地 (LocalCacheView), hit 零网络返回; miss 按 StoreMode 分两路: LOCALCACHE (loader 回填/无则 null) vs LOCALCACHE_REDIS (查 Redis 回填)。

## 验证过程
- `getAsync(key, threadId)` (RedissonLocalCachedMap.java:285-317):
  - L288-289: `cacheKey = toCacheKey(key); cacheValue = cache.get(cacheKey)` — **本地 cache 优先**
  - L290-292: hit → `cacheValue.getValue()` 直接返回 (零网络)
  - L294-306 **LOCALCACHE 模式**: `hasNoLoader() → null`; 否则 `loadValue(key)` + 回填 cachePut
  - L309-315 **LOCALCACHE_REDIS 模式**: `super.getAsync` (查 Redis) → 回填 cachePut
- StoreMode 枚举 (LocalCachedMapOptions.java:121-131): LOCALCACHE (只本地) / LOCALCACHE_REDIS (双存)
- cachePut 回填 (L302/312): miss 值写入本地缓存
- storeCacheMiss 选项: 连 miss 也缓存 (防穿透)

## 代码类型
Implementation (双层级读) — 本地优先 + miss 回填

## 跨域关联
- RD-5 (RMap) → super.getAsync 走 RMap 命令面
- Q6 (CacheProvider) → cache 是 Caffeine/LRU
- 面试点: "本地缓存 miss 了怎么办?"

## 结论
读路径 = 本地优先: hit 零网络返回; miss 按 StoreMode: LOCALCACHE (loader) / LOCALCACHE_REDIS (Redis 回填)。storeCacheMiss 可缓存 miss 防穿透。这是"near-cache"的核心读语义。
源码位置: RedissonLocalCachedMap.java:285-317, LocalCachedMapOptions.java:121-131