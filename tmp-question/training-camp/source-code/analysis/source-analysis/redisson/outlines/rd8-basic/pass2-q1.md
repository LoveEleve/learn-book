# 闭环笔记 q1: 统一等待模型 — RLock/RSemaphore/RCountDownLatch 的同构

## 假设
RSemaphore/RCountDownLatch 的等待与 RLock **完全同构**: 先试→订阅→循环"试-等"→退出。SemaphorePubSub 甚至复用 RedissonLockEntry (同 RLock)。

## 验证过程
- RSemaphore.acquire (RedissonSemaphore.java:66-98): tryAcquire 先试 (L70) → 失败 `subscribe()` (L76) + `semaphorePubSub.timeout(future)` → **while 循环** `tryAcquire ? return : entry.getLatch().acquire()` (L80-85) → finally unsubscribe (L86)
- **与 RLock.tryLock 完全同构** (RD-2 q1: 首试→订阅→循环试-等): 只是 tryAcquire 语义不同 (信号量递减 vs 哈希锁)
- SemaphorePubSub (pubsub/SemaphorePubSub.java:27): `extends PublishSubscribe<RedissonLockEntry>` — **复用 RLock 的 entry 类!** (L34 createEntry 返回 RedissonLockEntry)
- CountDownLatchPubSub (L27): extends PublishSubscribe<RedissonCountDownLatchEntry> — 独立 entry 但同构
- 统一模型: [尝试 → 订阅 → 循环(试-等信号) → 取消订阅]

## 代码类型
Interface (统一等待模型) — 三结构共享订阅等待骨架

## 跨域关联
- RD-2 (RLock) → 同构骨架
- RD-1 (ElementsSubscribeService) → 订阅宿主
- 面试点: "信号量/闭锁的等待和锁一样吗?"

## 结论
统一等待模型: RLock/RSemaphore/RCountDownLatch 共享"先试→订阅→循环试-等→退订"骨架; SemaphorePubSub 复用 RedissonLockEntry 是强证据。差异只在 tryAcquire 语义 (哈希/递减/计数判定)。
源码位置: RedissonSemaphore.java:66-98, SemaphorePubSub.java:27-42, CountDownLatchPubSub.java:27-53