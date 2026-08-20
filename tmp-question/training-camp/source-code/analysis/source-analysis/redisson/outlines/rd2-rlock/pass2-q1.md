# 闭环笔记 q1: tryLock 时间会计 — 有界等待的精确性

## 假设
tryLock(waitTime) 的等待是"伪有界": 每次 tryAcquire 和 latch.acquire 前都扣减已耗时间, 保证总等待 ≤ waitTime。lock() (无超时) 则无限等。

## 验证过程
- `tryLock(long waitTime, long leaseTime, TimeUnit unit)` (RedissonLock.java:227-300):
  - L228-230: `time = waitTime; current = now(); threadId = currentThread.getId()`
  - L231-235: **首试** tryAcquire → null = 成功直接 true
  - L237-241: `time -= now - current; if (time<=0) return false` — **扣首试时间**
  - L244-246: subscribe(threadId) + `get(time)` — 订阅等待也有超时
  - L248-257: 订阅超时 → RedisTimeoutException ("Unable to acquire subscription lock... increase subscriptionConnectionPoolSize")
  - L265-270: 订阅后扣时间
  - L272-299: **while(true) 循环**:
    - L275: tryAcquire 再试
    - L287-291: 扣时间, 超时退出
    - L295-299: **waiting for message**: `ttl<time → latch.tryAcquire(ttl)` 等锁剩余 ttl; 否则 `latch.tryAcquire(time)` 等剩余预算 — 然后回到循环
- 语义: (a) 首试失败 → 订阅通道 → 循环"试一次-等信号-再试" (b) 每次等待信号的门票 = min(锁剩余ttl, 超时预算) (c) 总时长被时间会计严格约束到 waitTime
- lock() (无限) 与 lockInterruptibly/leaseTime 变体: 无时间预算 → 无限 latch.acquire (L140-142)

## 代码类型
Implementation (等待协议) — 有界等待的精确时间会计

## 跨域关联
- Q3 (Watchdog) → 获取成功后的续期何时启
- RD-4 (subscribe 是命令) → 订阅走连接池
- 面试点: "tryLock 超时为什么准?靠什么保证不等无限久"

## 结论
tryLock = 首试 + 订阅 + 循环"试-等-试", 时间会计 (每次扣now-current) 保证总等待 ≤ waitTime; 每次信号等待用 min(剩余ttl, 剩余预算)。释放事件由 LockPubSub 唤醒 latch。
源码位置: RedissonLock.java:227-300