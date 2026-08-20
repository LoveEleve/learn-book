# RD-8 篇2 — unified-wait: 信号量/闭锁的统一等待模型

> 前置: [[RD-8-篇1]] (命令封装) + [[rd2-rlock]] (等待骨架) | 复用: [[rd1-connection]] (订阅) | 对照: [[rd2-rlock]] (RLock 同构) [[s29-tx-chain]] (并发) | 引出: [[rd0-intro]] (全景闭环)
> 🟡 B | 3 KP | [模式: 统一订阅等待 + 信号量计数 + 门闩开关]
> Pass 2 闭环: q1(统一等待) q4(RSemaphore) q5(RCountDownLatch)

**读者处境**: 你用过 RLock 等待释放。现在 RSemaphore.acquire 和 RCountDownLatch.await 也用同样的等待方式 — "先试→订阅→循环'试-等信号'→退订"。甚至 SemaphorePubSub 直接复用 RLock 的 entry 类。这篇拆三个同步原语的统一等待模型, 以及 Semaphore (计数) vs Latch (门闩) 的本质差异。

### 概念依赖链
q1(统一等待) ← q4(Semaphore) ← q5(Latch) — 先讲共同骨架, 再讲信号量释放, 最后讲门闩开关。

### 核心悬念
"信号量和闭锁的等待, 和 RLock 一模一样 —— 统一骨架怎么承载三种语义?"

### 叙事顺序
1. 问题引入: acquire/await 和 lock 一样的等待
2. 统一模型 (q1) — 先试→订阅→循环试-等→退订
3. RSemaphore (q4) — decrby + 订阅唤醒 (计数)
4. RCountDownLatch (q5) — 门闩 open/close (开关)
5. 收束: "三种语义一个骨架"

### 1. 统一等待模型 — 共享订阅骨架

场景: 三个原语怎么共享等待?
源码路径:
- RSemaphore.acquire (RedissonSemaphore.java:66-98): tryAcquire 先试 (L70) → 失败 `subscribe()` (L76) + timeout → **while 循环** `tryAcquire ? return : latch.acquire()` (L80-85) → finally unsubscribe (L86)
- **与 RLock.tryLock 完全同构** (RD-2): 首试→订阅→循环试-等
- `SemaphorePubSub extends PublishSubscribe<RedissonLockEntry>` (SemaphorePubSub.java:27) — **复用 RLock 的 entry 类!** (L34)
- CountDownLatchPubSub extends PublishSubscribe<RedissonCountDownLatchEntry> (CountDownLatchPubSub.java:27) — 独立 entry 同构
关键设计 (q1): 统一骨架 = [尝试 → 订阅 → 循环(试-等信号) → 退订]; 差异只在 tryAcquire 语义。[模式: 统一订阅等待]
数据流: acquire → tryAcquire? : subscribe → 循环 → 信号唤醒 → 再试。

### 2. RSemaphore — 计数信号量

场景: 许可怎么发放?
源码路径:
- releaseAsync (RedissonSemaphore.java:445-446): Lua (L267-269): `get → decrby(KEYS[1], ARGV[1])` — **许可归还 (计数递增)**
- 发布: release → SemaphorePubSub 广播 → onMessage (SemaphorePubSub.java:42): `latch.release(min(acquired, message))` — 释放 min(已获取, 消息) 个 permit
- 等待者: 循环 tryAcquire 被唤醒再抢 (非 FIFO)
- **非公平**: 无排队, 唤醒后竞争
关键设计 (q4): 信号量 = decrby 服务端计数 + 订阅唤醒 (min 释放); 非公平竞争。[模式: 计数信号量]
数据流: release → decrby → 广播 → 等待者 latch.release → 再 tryAcquire。

### 3. RCountDownLatch — 门闩开关

场景: 倒计数归零怎么放行?
源码路径:
- `RedissonCountDownLatch extends RedissonObject` (RedissonCountDownLatch.java:44) — 非 Expirable
- await (RedissonCountDownLatch.java:69): `entry.getLatch().await()` — 阻塞直到 open
- countDown (RedissonCountDownLatch.java:248-259): `evalWriteNoRetryAsync` → 减计数 → 归零发布 **ZERO_COUNT_MESSAGE** (L259)
- **countDown Lua 语义** (RedissonCountDownLatch.java:253-259): `decr` 减计数 → **`v <= 0 → del(KEYS[1])` (计数归零后清除 Redis key)** → `v == 0` 发布 ZERO_COUNT — 闭锁一次性, 归零即清除
- trySetCount (RedissonCountDownLatch.java:287-296): 设新计数 → **NEW_COUNT_MESSAGE** (L296)
- CountDownLatchPubSub.onMessage (CountDownLatchPubSub.java:40-56): **ZERO → 运行 listeners + `latch.open()`** (L50); **NEW → `latch.close()`** (L53)
关键设计 (q5): 门闩 = 开关 (open 放行全部 / close 重阻塞), 非计数; 归零一次性放行。[模式: 门闩开关]
数据流: countDown → 归零 → ZERO 广播 → open → await 全放行。

### 负面空间 — 同步原语刻意不做的事

- **不做公平队列**: Semaphore 非 FIFO (无等待者排队)
- **不做闭锁复用**: Latch open 后需 trySetCount 才能重新关 (非自动重置)
- **不做超时默认**: await 需显式传超时, 无默认
- **不跨 JVM 单例语义**: 每 Redisson 实例独立 entry (同 name 共享 Redis 状态)
- **不做许可上限**: Semaphore 可超发 (trySetPermits 显式控)
- **独立接口非 JDK 实现**: RSemaphore extends RExpirable (api/RSemaphore.java:29) / RCountDownLatch extends RObject (api/RCountDownLatch.java:29) — **语义近 JDK 但接口独立** (completeness Q32)
- **许可可观测**: availablePermits() (RedissonSemaphore.java:519-524) 读当前许可 — 监控信号量水位 (completeness Q29)
- **等待者共享订阅**: 同锁一样, 等待者复用订阅通道而非每等待者一连接 (completeness Q27)

→ 引出: Redisson 全景 — 数据结构覆盖 Redis 全部类型 → [[rd0-intro]]