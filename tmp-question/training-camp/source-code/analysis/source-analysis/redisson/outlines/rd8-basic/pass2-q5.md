# 闭环笔记 q5: RCountDownLatch 门闩 — open/close 的开关语义

## 假设
RCountDownLatch 的 latch 是"开关" (门闩) 非计数信号量: 计数归零→open (放行全部 await), 设新计数→close (重新阻塞)。

## 验证过程
- RedissonCountDownLatch (RedissonCountDownLatch.java:44): extends RedissonObject (非 Expirable!)
- await (L69): `entry.getLatch().await()` — 阻塞直到 open
- countDown (L248-259): countDownAsync → `evalWriteNoRetryAsync` → 减计数 → 归零发布 **ZERO_COUNT_MESSAGE** (L259)
- trySetCount (L287-296): 设新计数 → 发布 **NEW_COUNT_MESSAGE** (L296)
- CountDownLatchPubSub.onMessage (L40-56):
  - **ZERO_COUNT_MESSAGE** → 运行所有 listeners + `getLatch().open()` (L50, 放行)
  - **NEW_COUNT_MESSAGE** → `getLatch().close()` (L53, 重新阻塞)
- await(timeout) (L115-143): `latch.await(remainTime)` 有界等待
- 门闩语义: 开关 (open 一次全放行) vs Semaphore (计数释放指定量)

## 代码类型
Implementation (门闩协议) — open/close 开关

## 跨域关联
- q1 (统一等待模型) → 骨架
- RD-2 (RLock) → 对比: 锁用 Semaphore 计数, 闭锁用门闩开关
- 面试点: "倒计数锁和信号量区别?"

## 结论
RCountDownLatch = 门闩开关: 计数归零 → open (全放行), 设新计数 → close (重阻塞); await 阻塞等待 open。与 Semaphore (计数释放) 本质不同 — 闭锁是"一次性门闩"。countDown 用 noRetry (幂等减计数)。
源码位置: RedissonCountDownLatch.java:248-296, CountDownLatchPubSub.java:40-56