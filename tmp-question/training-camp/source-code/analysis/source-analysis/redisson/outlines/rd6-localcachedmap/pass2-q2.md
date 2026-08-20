# 闭环笔记 q2: SyncStrategy 双消息 — INVALIDATE vs UPDATE 带宽权衡

## 假设
SyncStrategy 决定写时广播什么: INVALIDATE 只广播 keyHash (接收者清本地), UPDATE 广播 key+value (接收者直接更新本地)。INVALIDATE 省带宽, UPDATE 省接收端回源。

## 验证过程
- SyncStrategy 枚举 (LocalCachedMapOptions.java:64-79): NONE / INVALIDATE / UPDATE
- invalidateEntryOnChange (RedissonLocalCachedMap.java:59,103-107): NONE→0 / INVALIDATE→1 / UPDATE→1 (LOAD 重连时=2)
- broadcastLocalCacheStore (L116-125): 
  - **UPDATE → `new LocalCachedMapUpdate(instanceId, mapKey, mapValue)`** (带值)
  - **INVALIDATE → `new LocalCachedMapInvalidate(instanceId, cacheKey.getKeyHash())`** (只 hash)
- 接收端 onMessage (LocalCacheListener.java:276-315):
  - Invalidate → `cache.remove(keyHash)` — 本地移除, 下次读 miss→回源 (Redis)
  - Update → `updateCache(keyBuf, valueBuf)` — **直接更新本地值, 无回源**
- 权衡: 
  - **INVALIDATE**: 广播字节少 (hash 32B), 但接收端 miss 后要回源 (Redis) — 适合低频/大值
  - **UPDATE**: 广播带全值, 接收端零回源 — 适合高频/小值/多实例都要新值
- 默认: 需显式配 (LocalCachedMapOptions 默认?)

## 代码类型
Interface (一致性策略) — 失效 vs 更新的带宽/回源权衡

## 跨域关联
- Q3 (excludedId) → 发送者排除
- Q4 (重连) → LOAD 时 invalidateEntryOnChange=2
- 面试点: "INVALIDATE 和 UPDATE 区别?选哪个?"

## 结论
SyncStrategy = 一致性的两档: INVALIDATE 广播 hash (接收者清+回源, 省带宽) vs UPDATE 广播全值 (接收者直接更新, 省回源)。选型: 值小/多读选 UPDATE, 值大/少读选 INVALIDATE。
源码位置: LocalCachedMapOptions.java:64-79, RedissonLocalCachedMap.java:103-125, LocalCacheListener.java:276-315