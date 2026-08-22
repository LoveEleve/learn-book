# Kafka-46 重写规划

> 题目：消息为什么会“丢”——从 acks、maximalIsr、HW、LSO 到 marker 的排查路径
> 状态：源码 + 运维桥接篇。不是单纯讲“怎么配参数”，而是把消息丢失/不可见/重复感知这几类常见故障，沿 broker 与 consumer 的源码边界串成可落地的排查路径。

## 1. 读者困惑
- producer 明明 send 成功了，为什么 consumer 没看到？
- `acks=all` 了，为什么还是感觉像“丢消息”？
- 是真的丢了，还是只对 `read_committed` 不可见？
- 是 producer 重试重复、事务未提交、还是副本未稳定？

## 2. 一句话顿悟
**Kafka 里“消息丢失”经常不是单点问题，而是写入确认、ISR 判定、HW/LSO 可见性、事务 marker、消费隔离级别这几层边界里某一层没有满足；排查必须沿“producer 成功语义 → broker enough-replicas → log 可见边界 → consumer isolation”逐层缩小。**

## 3. 失败方案推演
- 只看 producer 端日志，以为 send success = consumer 必见
- 只看 ISR/HW，不看 `read_committed` 与事务 marker
- 把“没看到”误判成“丢了”，实际上是 LSO/abort 过滤
- 把重复重试误判成“丢一半”，实际上是幂等/epoch 问题

## 4. 章节问题
- producer 成功语义在什么条件下成立？
- leader 侧真正等待的 enough-replicas 条件是什么？
- HW、LSO、logEndOffset 谁决定“当前能读到哪”？
- `read_committed` 为什么会看不见已经 append 的数据？
- 怎么把“真丢失 / 暂不可见 / 被过滤 / 被重试覆盖感知”分开？

## 5. 至少要排除的误解
- `acks=all` 就等于绝不会丢
- producer send 成功 = 任意 consumer 立刻可见
- HW 和 LSO 是一回事
- 事务消息 append 成功后就对 `read_committed` 可见
- “没读到”一定是 broker 丢了

## 6. 关键证据清单
- `core/src/main/scala/kafka/server/DelayedProduce.scala`
- `core/src/main/scala/kafka/cluster/Partition.scala`
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerAppendInfo.java`
- `storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java`
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java`
- `core/src/main/scala/kafka/coordinator/transaction/TransactionMarkerChannelManager.scala`

## 7. 版本与实现边界
- Kafka v4.x
- 这是源码 + 运维桥接篇：会给排查路径，但不扩成部署手册
- 聚焦 broker / log / consumer 可见性，不展开网络抖动与客户端业务重试框架

## 8. 字数预算
- 7000~10000 字