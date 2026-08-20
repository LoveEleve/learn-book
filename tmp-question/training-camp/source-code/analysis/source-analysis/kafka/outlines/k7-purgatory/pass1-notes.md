# K-7 Purgatory — Pass 1 探索笔记 (扫轮廓)

> 🟡 B | 依赖: K-4 ✅ (HW 推进触发) + K-3 ✅ (读路径) | 对照: [[r8-persistence]] (定时任务对照) [[E-3-translog]] (等待机制对照)
> 源码: server-common/org/apache/kafka/server/purgatory/ (DelayedOperation 155 + DelayedOperationPurgatory 423 行) + util/timer/ (TimingWheel 185 + SystemTimer 121 行) + core/server/DelayedProduce.scala (151) + DelayedFetch.scala (195)
> [索引覆盖: Java 端已索引 (codebase-memory); Scala 端未索引 — grep 降级]
> 测试地图: core/src/test/scala/unit/kafka/server/DelayedProduceTest.scala + DelayedOperationPurgatoryTest.java + TimingWheelTest.java (util/timer/)

## 继承树/调用图

```
DelayedOperation (DelayedOperation.java:38, extends TimerTask)
├── forceComplete (DelayedOperation.java:L48-72: 双检锁 + cancel + onComplete — 只完成一次)
├── onExpiration (抽象) / onComplete (抽象)
└── 子类: DelayedProduce (DelayedProduce.scala:57) / DelayedFetch (DelayedFetch.scala:L195)

DelayedOperationPurgatory (DelayedOperationPurgatory.java:38)
├── SHARDS=512 分片 watcher (DelayedOperation.java:L41)
├── tryCompleteElseWatch (DelayedOperationPurgatory.java:L122-170: 注册 watchKeys → safeTryCompleteOrElse)
├── checkAndComplete (条件满足触发)
└── timeoutTimer = SystemTimer (DelayedOperationPurgatory.java:L70) → TimingWheel (TimingWheel.java:97)

TimingWheel (TimingWheel.java:97)
├── tickMs/wheelSize/interval=tickMs*wheelSize (TimingWheel.java:L98-103)
├── overflowWheel 分级 (TimingWheel.java:L108,131-141)
├── add 三分支 (TimingWheel.java:L143-175: 已过期/本层/溢出)
└── advanceClock 级联 (TimingWheel.java:L177-184)

DelayedProduce (DelayedProduce.scala:57) — acks=all 应用
├── tryComplete (DelayedProduce.scala:L89-116): 逐分区 checkEnoughReplicasReachOffset (TimingWheel.java:L101)
└── 全部满足 → forceComplete (DelayedProduce.scala:L112)
```

## 基本元素分解 (原则二)

1. **状态机** — completed volatile + forceComplete 双检锁 (DelayedOperation.java:41,48-72): Pending→Complete/Expired 只完成一次
2. **注册+检查** — tryCompleteElseWatch (DelayedOperationPurgatory.java:122): 先试完成, 不行挂 watchKeys
3. **分级时间轮** — TimingWheel (TimingWheel.java:97): tickMs 递增层级, overflowWheel 溢出
4. **触发条件** — DelayedProduce.tryComplete (DelayedProduce.scala:89): acks=all → ISR 全 ack (K-4)
5. **过期 Reaper** — ExpiredOperationReaper: 超时强制完成
6. **watch 分片** — SHARDS=512 (DelayedOperation.java:L41): 减锁竞争

## 标记问题 (≥5)

1. **Q1: DelayedOperation 状态机?** — completed + forceComplete 一次语义 (DelayedOperation.java:41,48-72)
2. **Q2: 分级时间轮?** — tickMs 层级 + overflowWheel + add 三分支 (TimingWheel.java:97-184)
3. **Q3: tryComplete 触发条件?** — acks=all → checkEnoughReplicasReachOffset (DelayedProduce.scala:89-116)
4. **Q4: 与 K-4 HW 衔接?** — HW 推进 → tryCompleteDelayedRequests (Partition.scala:826) → checkAndComplete
5. **Q5: vs ScheduledThreadPoolExecutor?** — 时间轮 O(1) vs 优先队列 O(log n)
6. **Q6: 与 Netty HashedWheelTimer 对照?** — 同构分级时间轮

## 已读测试 (2 个)

- `DelayedProduceTest`: acks=all 触发/超时场景
- `TimingWheelTest`: 分层/溢出/推进

## 完成检查

- [x] 继承树/调用图已画出
- [x] 基本元素分解 (6 元素, 对应源码位置)
- [x] 6 个标记问题, 每个有源码位置
- [x] 已读 2 个测试文件

## 跨域发现

- 来源: K-7 Pass 1 — tryComplete 的核心判断是 checkEnoughReplicasReachOffset (K-4 篇 4 已铺垫: HW=全 ISR 最小 LEO)
- 发现: 时间轮与 Netty HashedWheelTimer 同构 (netty 已交付: netty/outlines 存在)
- 已对照验证: K-4 Partition.scala:826 tryCompleteDelayedRequests (K-4 pass2 实证)
