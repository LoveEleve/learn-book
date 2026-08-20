# K-7 Purgatory — 知识规划 (00 §10: 逐源提取→聚合→分类→聚类)

> 2026-08-15 | 源码: server-common/org/apache/kafka/server/purgatory/ (DelayedOperation 155 + DelayedOperationPurgatory 423) + util/timer/ (TimingWheel 185 + SystemTimer 121) + core/server/DelayedProduce.scala (151) + DelayedFetch.scala (195)
> [索引覆盖: Java 端 (purgatory/timer) 已索引; Scala 端 (DelayedProduce) 未索引 — codebase-memory 验证 Java 端]

## 01 逐源提取

| 源文件 | 机制点 |
|---|---|
| DelayedOperation.java | ①extends TimerTask (DelayedOperation.java:L38) ②completed volatile (DelayedOperation.java:L41) ③forceComplete 双检锁 (DelayedOperation.java:L48-72) ④onExpiration/onComplete 抽象 (DelayedOperation.java:L93-100) ⑤safeTryCompleteOrElse |
| DelayedOperationPurgatory.java | ①SHARDS=512 分片 (DelayedOperation.java:L41) ②tryCompleteElseWatch (DelayedOperationPurgatory.java:L122-170, 死锁注释 DelayedOperationPurgatory.java:L135-154) ③watchForOperation/checkAndComplete ④purgeInterval=1000 (DelayedOperationPurgatory.java:L58,66) ⑤ExpiredOperationReaper |
| TimingWheel.java | ①tickMs/wheelSize/interval (TimingWheel.java:L98-103) ②overflowWheel 分级 (TimingWheel.java:L108,131-141) ③add 三分支 (TimingWheel.java:L143-175) ④advanceClock 级联 (TimingWheel.java:L177-184) |
| SystemTimer.java | ①TimingWheel 组装 (DelayedOperationPurgatory.java:L58-61) ②DelayQueue 驱动 |
| DelayedProduce.scala | ①tryComplete 逐分区检查 (DelayedProduce.scala:L89-116) ②checkEnoughReplicasReachOffset (DelayedProduce.scala:L101, K-4 衔接) ③forceComplete (DelayedProduce.scala:L112) ④onExpiration/onComplete (DelayedProduce.scala:L119-134) |
| DelayedFetch.scala | fetch.min.bytes 等待数据积累 |

## 02 聚合 (P1/P2/P3)

| 聚合机制 | 来源 | 分级 |
|---|---|---|
| DelayedOperation 状态机 (Pending→Complete/Expired) | DelayedOperation | P1 |
| 分级时间轮 (tickMs 递增层级) | TimingWheel | P1 |
| tryCompleteElseWatch 注册+检查 | Purgatory | P1 |
| acks=all 触发条件 | DelayedProduce | P1 |
| watch key 分片 (SHARDS=512) | Purgatory | P2 |
| 过期 Reaper 线程 | Purgatory | P2 |
| vs ScheduledThreadPoolExecutor / Netty HashedWheelTimer | 对照 | P2 |

## 03 深度分类

- 🔴: 时间轮算法 + 状态机 (机制核心) — 域整体 🟡 (等待机制非定义特征)
- 🟡: 分片/Reaper/触发条件
- 🟢: 配置 (purgeInterval)

## 04 聚类 (教学顺序)

```
问题: acks=all 等 ISR ack、fetch.min.bytes 等数据 — 请求不能立即完成 → 延迟操作
状态机: DelayedOperation (completed 标志 + forceComplete 一次语义)
触发: tryCompleteElseWatch (注册 watchKeys → 条件满足 checkAndComplete)
超时: TimingWheel 分级时间轮 (add 三分支/overflowWheel/advanceClock)
应用: DelayedProduce (acks=all → checkEnoughReplicasReachOffset, K-4 衔接)
对照: ScheduledThreadPoolExecutor vs 时间轮; Netty HashedWheelTimer 同构
```

**拆篇建议**: 2 篇 (🟡 B)
- 01: 延迟操作状态机 + 触发注册 (DelayedOperation + Purgatory)
- 02: 分级时间轮 + 应用与对照 (TimingWheel + DelayedProduce + Netty 对照)
