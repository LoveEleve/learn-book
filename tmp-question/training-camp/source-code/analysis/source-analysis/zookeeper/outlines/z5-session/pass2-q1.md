# 闭环笔记 q1: 结构 — sessionsById + ExpiryQueue + 三态

## 假设
会话表 (O(1) 查找) + 过期桶 (批量清扫) 双结构。

## 验证过程
- **SessionTrackerImpl** (359): **sessionsById (ConcurrentHashMap<Long, SessionImpl>, L49)** + **sessionExpiryQueue = new ExpiryQueue<>(tickTime)** (L51,113) + sessionsWithTimeout (ConcurrentMap)
- **SessionImpl 三态** (L56-78): `isClosing` boolean (L61) + isActive/isExpired 派生 (isClosing ? expired : active) — **closing 是显式标志, active/expired 派生**
- **宽限期注释** (L41-42): "rounds up the tick interval to provide a sort of grace period... expired in batches"
- **ExpiryQueue** (L35-140): **expiryMap (ConcurrentHashMap<Long expiryTime, Set<E>>)** + **elemMap (CHM<E, Long>)** + **nextExpirationTime (AtomicLong, L45)** + expirationInterval (L48)
- **构造恢复** (L110-117): sessionsWithTimeout 重放 → trackSession 重建

## 代码类型
Data Structure (双索引)

## 跨域关联
- Z-3: ephemerals 关联 (killSession 面)
- Z-4: checkSession (Prep 校验)

## 结论
会话 = CHM 会话表 + 桶队列 (tickTime 粒度) 双索引; 三态 (active/closing/expired 派生); 宽限期防临界抖动。
源码位置: SessionTrackerImpl.java:41-78,110-117; ExpiryQueue.java:35-50
