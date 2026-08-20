# Pass 1 探索笔记: RD-8 基础数据结构

> 方案 B (🟡) | 源码: `/data/workspace/source-code/code/spring/redisson` (4.6.2-SNAPSHOT)
> 域规模: RedissonBucket/RedissonAtomicLong/RedissonSemaphore/RedissonCountDownLatch/RedissonBitSet → 2 篇

## Pass 0 上下文吸收

- 基础结构 = 基本命令的 Java 封装: RBucket (String) / RAtomicLong (计数器) / RSemaphore (信号量) / RCountDownLatch (倒计数) / RBitSet (位图)
- **关键洞察**: RSemaphore/RCountDownLatch 的等待机制与 RD-2 RLock **完全同构** — SemaphorePubSub/CountDownLatchPubSub 都是 PublishSubscribe + Semaphore/门闩
- 全部 extends RedissonExpirable 或 RedissonObject (过期语义)
- 命令面: RBucket→GET/SET/GETSET/GETEX; AtomicLong→INCRBY/DECR; BitSet→BITFIELD; Semaphore→GET/DECRBY

## 继承树/调用图

```
RedissonExpirable (过期基类)
├── RedissonBucket (L43)     RBucket<V>: get→GET / set→SET / getSet→GETSET / getAndExpire→GETEX
├── RedissonAtomicLong (L43) RAtomicLong: addAndGet→INCRBY / decrementAndGet→DECR
├── RedissonSemaphore (L46)  RSemaphore: acquire/release + SemaphorePubSub 订阅
│    ├── release Lua: get→decrby (L267-269)
│    └── SemaphorePubSub (pubsub): onMessage→latch.release(min(acquired,msg))
├── RedissonBitSet (L39)     RBitSet: BITFIELD (L85-94 任意位宽)
└── RedissonCountDownLatch (L44) RCountDownLatch: await + CountDownLatchPubSub
     └── CountDownLatchPubSub (pubsub): onMessage→latch.open()/close() (门闩)
```

## 基本元素分解

1. **RedissonBucket** (L43) — String 封装: GET/SET/GETSET/GETEX (L107-147)
2. **RedissonAtomicLong** (L43) — 计数器: INCRBY/DECR (L91/134)
3. **RedissonSemaphore** (L46) — 信号量: 订阅+Semaphore 等待, release Lua decrby
4. **RedissonCountDownLatch** (L44) — 倒计数: 订阅+门闩 (open/close)
5. **RedissonBitSet** (L39) — 位图: BITFIELD (任意位宽读写)
6. **SemaphorePubSub** (L27) — 信号量释放通知 (复用 RedissonLockEntry!)
7. **CountDownLatchPubSub** (L27) — 倒计数归零通知 (门闩 open/close)

## 标记问题 (6 个)

1. **Q1 统一等待模型**: RSemaphore/RCountDownLatch 与 RLock 的等待同构?SemaphorePubSub 复用 RedissonLockEntry 意味着什么?
2. **Q2 RBucket 命令面**: get/set/getSet/getAndExpire 各用哪个命令?为什么 GETEX 是新命令?
3. **Q3 RAtomicLong 原子性**: addAndGet→INCRBY 的原子性来源?CAS 变体 (compareAndSet) 用什么?
4. **Q4 RSemaphore 公平性**: acquire 的顺序?release decrby 后怎么唤醒等待者?
5. **Q5 RCountDownLatch 门闩**: open/close 语义?await 怎么阻塞/唤醒?
6. **Q6 RBitSet BITFIELD**: 为什么用 BITFIELD 而非 SETBIT/GETBIT?任意位宽语义?

## 已读测试 (2 个)

- RedissonBucketTest (顶层): get/set 验证
- RedissonAtomicLongTest: 计数器验证

## 完成检查

- [x] 继承树/调用图已画出 (含 PubSub 同构)
- [x] 基本元素分解 7 项有源码位置
- [x] 6 个标记问题有源码位置
- [x] 已读测试 (RedissonBucketTest/RedissonAtomicLongTest)
- [x] 方案 B: 无 harness/时空溯源