# RD-6 篇1 — local-read: 双层级缓存与读路径

> 前置: [[rd5-rmap]] (RMap 载体) + [[rd4-command]] (写失效钩子) | 复用: [[r22-expire]] (TTL 语义) | 对照: [[m7-cache]] (MyBatis 本地缓存) | 引出: [[RD-6-篇2]] (一致性广播) + [[rd7-spring]]
> 🔴 A | 3 KP | [模式: 双层级 + 本地优先 + miss 回填 + 可插拔实现]
> Pass 2 闭环: q1(双层级读) q6(CacheProvider)

**读者处境**: 你听说过"near cache"但觉得复杂?RLocalCachedMap 让你把热数据放进程内, 读 100 万次只有首次走网络。它怎么做到"本地 hit 零网络, miss 自动回填"?本地缓存底层用什么实现 (Caffeine 还是自研)?这篇拆本地缓存的骨架: RMap 远程载体 + LocalCacheView 本地层, 双 StoreMode, 以及 Caffeine/自研双实现。

### 概念依赖链
q1(双层级读) ← q6(CacheProvider) — 先讲"读怎么走", 再讲"本地用什么实现"。

### 核心悬念
"get 100 万次只回源 1 次 —— 本地缓存怎么做到?底层是 Caffeine 还是自研?"

### 叙事顺序
1. 问题引入: 热数据放进程内, 读零网络
2. 双层级 (q1) — RMap 远程 + LocalCacheView 本地
3. 读路径 (q1) — 本地 hit 零网络; miss 按 StoreMode 回填
4. StoreMode (q1) — LOCALCACHE / LOCALCACHE_REDIS
5. CacheProvider (q6) — Caffeine vs 自研 LRU/LFU/Soft/Weak
6. 收束: "near-cache 读语义"

### 1. 双层级 — RMap 是远程骨架

场景: RLocalCachedMap 和 RMap 什么关系?
源码路径:
- `RedissonLocalCachedMap extends RedissonMap` (RedissonLocalCachedMap.java:44) — **继承 RMap, 远程载体复用**
- `LocalCacheView` (RedissonLocalCachedMap.java:84) + `cache` (RedissonLocalCachedMap.java:85) + `cacheKeyMap` (RedissonLocalCachedMap.java:86) — 本地层
- 双结构: 远程 Redis hash (RMap) + 进程内本地缓存
- 为什么继承: 远程语义 (持久化/分布式) 全部复用 RMap, 本地层是增强
关键设计 (q1): 双层级 = RMap 提供分布式存储, 本地缓存提供进程内加速。[模式: 远程载体 + 本地增强]
数据流: RLocalCachedMap → LocalCacheView (本地) + RMap (远程)。

### 2. 读路径 — 本地优先, miss 自动回填

场景: get 一次怎么走?
源码路径:
- `getAsync` (RedissonLocalCachedMap.java:285-317):
  - L288-289: `cache.get(cacheKey)` — **本地优先**
  - L290-292: hit → 直接返回 (零网络)
  - L294-306 **LOCALCACHE**: 无 loader → null; 有 → `loadValue` + cachePut
  - L309-315 **LOCALCACHE_REDIS**: `super.getAsync` (查 Redis) → cachePut 回填
- `storeCacheMiss` (RedissonLocalCachedMap.java:290/301/311): miss 也缓存 (防穿透)
关键设计 (q1): 读 = 本地 hit 直返 + miss 回填 (loader 或 Redis); 可缓存 miss。[模式: 本地优先 + 回填]
数据流: get → 本地 hit? 返回 : (LOCALCACHE? loader : Redis) → 回填 → 返回。

### 3. StoreMode — 只本地 vs 双存

场景: 数据一定要进 Redis 吗?
源码路径:
- StoreMode 枚举 (LocalCachedMapOptions.java:121-131): **LOCALCACHE** (只本地) / **LOCALCACHE_REDIS** (本地+Redis)
- LOCALCACHE 模式 (RedissonLocalCachedMap.java:294-306): 纯本地缓存 + loader, 不碰 Redis
- LOCALCACHE_REDIS (RedissonLocalCachedMap.java:309-315): 读 miss 查 Redis 回填
- 注释 (RedissonLocalCachedMap.java:371-372): "LOCALCACHE - store data in local cache only; LOCALCACHE_REDIS - store data in both"
关键设计 (q1): StoreMode = 本地缓存是否落 Redis 的开关; 纯本地=会话级/临时, 双存=分布式缓存。[模式: 存储模式开关]
数据流: LOCALCACHE → loader 回填; LOCALCACHE_REDIS → Redis 回填。

### 4. CacheProvider — Caffeine 还是自研

场景: 本地缓存底层?
源码路径:
- `createCache` (LocalCacheView.java:312-350):
  - **CAFFEINE** (LocalCacheView.java:317-326): `Caffeine.newBuilder()` → expireAfterWrite(TTL) (LocalCacheView.java:320) / expireAfterAccess(maxIdle) (LocalCacheView.java:323) / maximumSize(size) (LocalCacheView.java:326) — 成熟 window-TinyLFU
  - **REDISSON** (LocalCacheView.java:338-350) 按 **EvictionPolicy 枚举** (LocalCachedMapOptions.java:83-110: NONE/LRU/LFU/SOFT/WEAK): LRUCacheMap (LocalCacheView.java:341) / LFUCacheMap (LocalCacheView.java:344) / ReferenceCacheMap.soft (LocalCacheView.java:347) / ReferenceCacheMap.weak (LocalCacheView.java:350) / NoneCacheMap (LocalCacheView.java:338)
- 差异: Caffeine 高命中 (window-TinyLFU); 自研免依赖 + SOFT/WEAK 弱引用
- 选型: 命中率优先 Caffeine; 免依赖/弱引用 Redisson
关键设计 (q6): CacheProvider 可插拔: Caffeine 成熟高命中 vs 自研免依赖 (LRU/LFU/Soft/Weak)。[模式: 可插拔缓存实现]
数据流: CacheProvider=CAFFEINE → Caffeine builder; =REDISSON → EvictionPolicy 选型。

### 负面空间 — 本地缓存刻意不做的事

- **不保证强一致**: 本地缓存是 near-cache (最终一致), 订阅消息有延迟窗口
- **不做全量本地预载**: 默认惰性回填, loadAll 需显式
- **不跨实例共享本地**: 每 JVM 独立 LocalCacheView
- **不做本地缓存持久化**: 本地层纯内存, 重启清空
- **不自带 TTL 清理**: 依赖 CacheProvider (Caffeine expireAfter / 自研 eviction)
- **不做防击穿**: 并发 miss 会同时回源 (无 single-flight 合并) — 高并发下需外部兜底 (completeness Q11)

→ 引出: 写之后其他实例怎么知道?INVALIDATE 还是 UPDATE?→ [[RD-6-篇2]]