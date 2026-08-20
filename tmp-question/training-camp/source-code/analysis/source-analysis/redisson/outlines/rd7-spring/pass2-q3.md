# 闭环笔记 q3: getCache 双路 — RMap vs RMapCache 按配置选

## 假设
RedissonSpringCacheManager.getCache(name) 按 CacheConfig 决定用 RMap (无 TTL) 还是 RMapCache (有 TTL/maxIdle): createMap vs createMapCache。

## 验证过程
- RedissonSpringCacheManager (RedissonSpringCacheManager.java:45): `implements CacheManager, ResourceLoaderAware, InitializingBean`
- configMap (L59): `Map<String, CacheConfig>` 按 Cache 名配 TTL
- getCache(name) (L216-235): 取 config → 
  - **createMap** (L238): `redisson.getMap(name)` — 无 TTL 的纯分布式 Map
  - **createMapCache** (L259): `getMapCache(name, config)` → RMapCache — 有 TTL/maxIdle
- getMapCache (L278-282): codec 配置 → redisson.getMapCache(name, codec) 或 getMapCache(name)
- 选型: CacheConfig.ttl/maxIdleTime>0 → RMapCache; 否则 RMap
- CacheConfig (CacheConfig.java:38-46): ttl/maxIdleTime/maxSize

## 代码类型
Interface (工厂选择) — 按配置创建对应载体

## 跨域关联
- RD-5 (RMap/RMapCache) → 载体选择
- Q2 (RedissonCache) → 包装对应载体
- s77-boot-cache → CacheManager 由 Boot 装配

## 结论
getCache 双路 = 按 CacheConfig 选载体: ttl/maxIdle>0 → RMapCache (带过期), 否则 RMap (纯分布式)。configMap 按 Cache 名独立配置。Spring Cache 的灵活性 + Redisson 的 TTL。
源码位置: RedissonSpringCacheManager.java:216-282, CacheConfig.java:38-46