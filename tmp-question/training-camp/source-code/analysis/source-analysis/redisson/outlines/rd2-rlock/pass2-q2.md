# 闭环笔记 q2: LockEntry 语义 — 一把锁多线程重入的持有记录

## 假设
LockEntry 记录"一把锁被哪些线程持有" (重入): addThreadId/removeThreadId/getFirstThreadId。一个锁只有一个分布式 key, 但多个线程可重入 (每线程一个 hash 字段)。

## 验证过程
- LockEntry (renewal/LockEntry:29-61): `addThreadId` (L43) / `getFirstThreadId` (L57) / `removeThreadId` (L61) — 线程 ID → lockName (hash 字段) 的映射
- 语义: 锁 key (`lock:xxx`) 是 hash: {field=uuid:threadId, count=N}. 同锁可被多线程持有 (如主线程拿锁 + 异步回调线程又拿 — 同 JVM 内多线程重入需要记录全部)
- RenewalTask.cancelExpirationRenewal (RenewalTask:97-121): threadId != null → task.removeThreadId; **hasNoThreads → 从 slot2names 移除 + 返回 null (锁释放)** — 全部线程退出才停心跳
- getFirstThreadId: 续期时每个锁只取第一个持有者做 hexists 校验 (L60-70 LockTask buildChunk)
- 为什么需要多线程: 同一把锁在 JVM 内被多个业务线程同时持有 (各自 getLock 同一 name), 全部释放才真释放 — 续期也要覆盖全部持有者的 field

## 代码类型
Implementation (重入记账) — 一把锁多持者的持有表

## 跨域关联
- Q3 (批量续期) → buildChunk 按 getFirstThreadId 续
- Q4 (Watchdog 开关) → hasNoThreads → stop
- Redis hash 语义: 分布式锁是 hash (不是简单 key)

## 结论
LockEntry = 锁的持有者账本: 同锁多线程各自记账 (threadId→lockName), getFirstThreadId 取代表者做续期校验, hasNoThreads 决定心跳停止。这是"可重入 + 多持者"的双重保证。
源码位置: LockEntry.java:29-61, RenewalTask.java:97-121