# Kafka-36 重写规划

> 题目：TimingWheel 为什么比优先队列省时间——分层时间轮与 DelayedOperation 完成条件深讲
> 状态：K-10 Purgatory 域第 2 篇；覆盖 TimingWheel 分层、bucket 到期、advanceClock，以及 DelayedProduce/DelayedFetch 完成条件深挖。

## 1. 读者困惑

- 为什么超时任务不用优先队列而用时间轮？
- 时间轮怎么分层，bucket 到期时发生什么？
- advanceClock 是怎么推进的？
- DelayedProduce 的条件为什么依赖 maximalIsr？
- DelayedFetch 的 minBytes 是怎么算的？

## 2. 一句话顿悟

**Kafka 的时间轮用分层降低超时任务插入成本，bucket 到期后降级重插到更细粒度轮或直接执行；DelayedProduce 依赖 maximalIsr 判断 enough replicas，DelayedFetch 按可读字节累计到底 minBytes 才触发。**

## 3. 失败方案推演

- 每次插入用 O(log n) 优先队列 → 大量任务成本高
- bucket 到期不降级 → 高粒度任务永远执行不了
- produce 用 committed ISR 判断 → 延迟/误判
- fetch 只看 minBytes 不比较可读量 → 触不及时

## 4. 误解清单

- "时间轮在超时场景更快"：在大量提前完成任务场景插入成本更低
- "bucket 到期直接执行"：先降级到细粒度轮
- "DelayedProduce 只看 committed ISR"：用 maximalIsr
- "DelayedFetch 等 minBytes 就全行"：要比较可读量是否达标

## 5. 证据清单

- `server-common/src/main/java/org/apache/kafka/server/util/timer/TimingWheel.java:22`：分层设计。
- `server-common/src/main/java/org/apache/kafka/server/util/timer/TimingWheel.java:143`：add。
- `server-common/src/main/java/org/apache/kafka/server/util/timer/TimingWheel.java:177`：advanceClock。
- `core/src/main/scala/kafka/server/DelayedProduce.scala:89`：tryComplete。
- `core/src/main/scala/kafka/server/DelayedFetch.scala:77`：tryComplete。

## 6. 版本边界与字数预算

- 基线 v4.x；目标 6000~10000 字。