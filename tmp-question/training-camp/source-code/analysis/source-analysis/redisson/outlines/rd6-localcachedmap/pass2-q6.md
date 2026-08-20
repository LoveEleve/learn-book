# 闭环笔记 q6: CacheProvider 双实现 — Caffeine vs Redisson 自研

## 假设
本地缓存实现可选: Caffeine (成熟库, TTL/idle/size 全支持) vs Redisson 自研 (LRU/LFU/Soft/Weak/None)。为什么两套?

## 验证过程
- createCache (LocalCacheView.java:312-350):
  - **CAFFEINE** (L317-326): `Caffeine.newBuilder()` → `expireAfterWrite(TTL)` (L320) / `expireAfterAccess(maxIdle)` (L323) / `maximumSize(cacheSize)` (L326)
  - **REDISSON** (L338-350) 按 EvictionPolicy:
    - LRU → `LRUCacheMap(size, ttl, idle)` (L341)
    - LFU → `LFUCacheMap` (L344)
    - SOFT → `ReferenceCacheMap.soft` (L347)
    - WEAK → `ReferenceCacheMap.weak` (L350)
    - NONE → `NoneCacheMap` (L338)
- 差异:
  - Caffeine: 第三方成熟 (window-tinyLFU/近最优命中率), 依赖 caffeine jar
  - Redisson 自研: 无外部依赖 (SOFT/WEAK 弱引用语义 Caffeine 没有), LRU/LFU 简单实现
- 为什么两套: CacheProvider 可配 — 追求命中率用 Caffeine, 免依赖/弱引用用 Redisson
- cacheSize=0 语义: LRUCacheMap(0) = 无容量限制 (读 L157)

## 代码类型
Interface (策略选择) — 本地缓存实现可插拔

## 跨域关联
- Q1 (读路径) → cache 是此实现
- 面试点: "本地缓存底层是什么?"

## 结论
双 CacheProvider: Caffeine (成熟, window-TinyLFU 高命中) vs Redisson 自研 (LRU/LFU/Soft/Weak, 免依赖+弱引用)。EvictionPolicy 仅 REDISSON 生效; Caffeine 用其原生配置。选型: 生产高命中选 Caffeine, 免依赖选 Redisson。
源码位置: LocalCacheView.java:312-350