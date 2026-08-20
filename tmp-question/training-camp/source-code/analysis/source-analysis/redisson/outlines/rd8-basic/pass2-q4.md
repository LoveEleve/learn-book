# 闭环笔记 q4: RSemaphore 公平性 — 信号量的释放与唤醒

## 假设
RSemaphore 不保证公平: 多个等待者靠"释放消息 + 信号量"竞争, 无 FIFO。release decrby 服务端递减, 发布消息唤醒等待者再抢。

## 验证过程
- releaseAsync (RedissonSemaphore.java:445-446): releaseAsync(1) → Lua (L267-269): `get value → decrby(KEYS[1], ARGV[1])` — **信号量计数递增** (许可归还)
- 发布消息: release 后 SemaphorePubSub 广播 → onMessage (SemaphorePubSub.java:42): `getLatch().release(min(acquired, message))` — 释放 min(已获取, 消息数) 个 permit
- 等待者: acquire 循环 (q1) `tryAcquire ? return : latch.acquire()` — **被 release 唤醒后重新 tryAcquire (非 FIFO 保证)**
- 公平性: 与 RLock 类似, 非公平 (先唤醒的未必先得, 靠 tryAcquire 竞争)
- trySetPermits: 设置新许可数 (发布消息唤醒)

## 代码类型
Implementation (信号量协议) — decrby 服务端 + 订阅唤醒

## 跨域关联
- q1 (统一等待模型) → 等待骨架
- RD-2 (RLock) → 同构 release 语义
- 面试点: "Redisson 信号量公平吗?"

## 结论
RSemaphore = 信号量协议: release→decrby (服务端计数递增) + 订阅广播→latch.release(min(acquired,msg)) 唤醒; acquire 循环试-等。**非公平** (无 FIFO, 唤醒后竞争)。与 RLock 同构但 tryAcquire 是递减许可。
源码位置: RedissonSemaphore.java:267-269,445-446, SemaphorePubSub.java:42