# 闭环笔记 q5: 订阅协议 — 释放通知的 PublishSubscribe

## 假设
锁等待者不轮询而是订阅 channel (`redisson_lock__channel`): 获取失败 → subscribe(channel) 建立 RedissonLockEntry → 每释放消息 latch.release() 唤醒一个。锁无释放 (ttl) 则 latch 等到超时。

## 验证过程
- 频道名: `RedissonLock.getChannelName()` (RedissonLock.java:71) `prefixName("redisson_lock__channel", getRawName())`
- 订阅: tryLock 失败后 `subscribe(threadId)` (L244) → CompletableFuture<RedissonLockEntry>
- LockPubSub (pubsub/LockPubSub:28-52): extends PublishSubscribe<RedissonLockEntry>
  - createEntry (L34-37): new RedissonLockEntry (内含 Semaphore latch)
  - onMessage (L41-52): **UNLOCK_MESSAGE=0 → tryRunListener + getLatch().release()(唤醒1)**; READ_UNLOCK_MESSAGE=1 → tryRunAllListeners + release(queueLength)(唤醒全部)
- RedissonLockEntry (RedissonLockEntry:31,37): `latch = new Semaphore(0)` — 初始 0 个 permit
- 等待: tryLock 循环 `getLatch().tryAcquire(ttl/time)` (L296-298) — 被 release 唤醒 → 回 tryAcquire 语法再抢
- 循环订阅复用: subscribeFuture 缓存 (同锁第一个等待者建 channel 订阅, 后续复用)
- 释放发布: unlock Lua 内 `redis.call(PUBLISH, channel, 0)` (L339 ARGV[2]; 0=UNLOCK_MESSAGE)

## 代码类型
Implementation (发布订阅唤醒) — Semaphore(0) + 消息驱动

## 跨域关联
- RD-1 (ElementsSubscribeService/PublishSubscribeService) → 宿主
- q1 (tryLock 循环) → latch 是循环的"敲门砖"
- Redis 发布订阅 (r28 pubsub 面) → channel 机制
- 面试点: "分布式锁等待靠什么唤醒?为什么比轮询好?"

## 结论
锁等待 = PublishSubscribe: Semaphore(0) + channel 消息。释放 → PUBLISH 0 → onMessage latch.release() 唤醒并就绪重抢。读锁 1 消息放行全部 (queueLength)。相比轮询, 唤醒即时且零重复请求。
源码位置: LockPubSub.java:28-52, RedissonLockEntry.java:31-37, RedissonLock.java:71,296-298