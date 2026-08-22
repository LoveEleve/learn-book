# Kafka-24 重写规划

> 题目：分区在 Controller 眼里是什么状态——PartitionRegistration、BrokersToIsrs 与 Epoch 状态机主链
> 状态：K-7 Controller 域第 2 篇，按"分区状态机"展开
> 目标：解释 Kafka Controller 在 KRaft 下如何管理分区的完整状态：`PartitionRegistration` 的 leader/ISR/replicas/epochs 字段、`BrokersToIsrs` 跟踪副本与 ISR 的映射、`PartitionChangeBuilder` 如何根据 broker 状态变化生成分区变更记录、以及 `PartitionRegistration.merge()` 如何在重放日志时推进分区状态。覆盖 ELR/LastKnown ELR 的边界。

## 1. 读者困惑

- `PartitionRegistration` 里那么多字段，哪些是核心状态，哪些是辅助？
- Broker 被 fenced 后，Controller 怎么知道它影响了哪些分区？
- `PartitionRegistration.merge()` 是什么时候调用的，怎么推进 epoch？
- ELR 和 LastKnownELR 有什么作用，和 ISR 有什么区别？
- `BrokersToIsrs` 维护的是什么映射，有什么用？
- 分区重分配（reassignment）时，`PartitionRegistration` 怎么变化？

## 2. 一句话顿悟

**Controller 用 `PartitionRegistration` 维护每个分区的完整状态（leader/ISR/replicas/epochs），用 `BrokersToIsrs` 反向追踪"哪些分区在某个 broker 的 ISR 里"，以便 broker 变化时快速定位受影响的分区；`PartitionChangeBuilder` 根据 broker 状态变化生成 `PartitionChangeRecord`，`PartitionRegistration.merge()` 在重放时按记录推进 leaderEpoch 与 partitionEpoch。**

## 3. 五要素卡片

### 读者问题

broker 1 被 fenced 后，Controller 怎么知道它影响了哪些分区的 ISR 和 leader？

### 入口

- `PartitionRegistration`：分区状态（leader/isr/replicas/epochs）
- `PartitionRegistration.merge()`：按记录推进状态
- `BrokersToIsrs`：broker → ISR 反向映射
- `PartitionChangeBuilder`：生成变更记录
- `PartitionChangeRecord`：变更记录
- `ReplicationControlManager`：管理分区生命周期

### 状态核心

- `PartitionRegistration` 字段：leader、isr、replicas、leaderEpoch、partitionEpoch、elr、lastKnownElr
- `BrokersToIsrs`：`brokerId → Set<TopicIdPartition>`
- `PartitionChangeBuilder.Election`：PREFERRED / ONLINE / UNCLEAN

### 失败路径

- 没有反向映射 → broker 变化时全局扫描所有分区，O(n) 性能差
- merge 不校验 epoch → 旧记录覆盖新状态
- 不维护 ELR → 不干净选举时没有可选的 leader 候选
- 重分配记录丢失 → 新副本无法加入 ISR

### 连接点

- 前文 `Kafka-7`：Controller 决策 leader/ISR 归属，本篇是决策的数据结构底座。
- 前文 `Kafka-9`：broker 本地 Partition 状态机与 Controller 的 PartitionRegistration 对应。
- 前文 `Kafka-8`：KRaft 元数据日志，PartitionRegistration 记录通过日志复制。

## 4. 总图

```text
PartitionRegistration
  leader | isr | replicas | leaderEpoch | partitionEpoch | elr | lastKnownElr

BrokersToIsrs
  brokerId → { TopicIdPartition ... }

Broker 被 fenced
  → BrokersToIsrs.partitionsWithBrokerInIsr(brokerId)
    → 遍历这些分区
      → PartitionChangeBuilder 生成新 leader/ISR
        → PartitionChangeRecord
          → PartitionRegistration.merge() 推进状态
```

## 5. 关键边界

- 本篇聚焦 Controller 侧的分区状态数据结构，不重复 broker 侧 Partition 状态机（Kafka-9）。
- 不把 PartitionRegistration 与 broker 侧 Partition 混成同一个对象。
- 不展开 Assignor 算法细节。
- 不把 ELR 与 ISR 混成同一概念：ISR 是 assured 同步，ELR 是候选人。

## 6. 失败方案推演

1. **没有反向映射（BrokersToIsrs）**：broker 变化时扫描全部分区，O(n) 性能差。
2. **没有 partitionEpoch**：旧记录与新记录无法区分，回放顺序错乱。
3. **没有 leaderEpoch**：follower 无法判断是否应该截断。
4. **没有 ELR**：ISR 全空时无法选举，分区不可用。

## 7. 误解清单

- "PartitionRegistration 就是 broker 侧的 Partition"：Controller 侧是注册状态，broker 侧是运行时状态。
- "leaderEpoch 只在 leader 变更时 +1"：每次 leader 变更 +1，partitionEpoch 每变一次 +1。
- "ISR 和 ELR 一样"：ISR 是已同步副本，ELR 是候选人（可升级为 ISR 但当前不在 ISR 中）。
- "merge 只在 leader 变更时调用"：每次收到 PartitionChangeRecord 都调用。
- "BrokersToIsrs 只用于 ISR"：它也是 leader 选举时定位"哪些分区受影响"的入口。

## 8. 证据清单

- `metadata/src/main/java/org/apache/kafka/metadata/PartitionRegistration.java:39`：PartitionRegistration 类。
- `metadata/src/main/java/org/apache/kafka/metadata/PartitionRegistration.java:240`：merge() 推进 epoch。
- `metadata/src/main/java/org/apache/kafka/controller/BrokersToIsrs.java:51`：BrokersToIsrs 类。
- `metadata/src/main/java/org/apache/kafka/controller/BrokersToIsrs.java:281`：partitionsWithBrokerInIsr。
- `metadata/src/main/java/org/apache/kafka/controller/BrokersToIsrs.java:277`：partitionsWithNoLeader。
- `metadata/src/main/java/org/apache/kafka/controller/PartitionChangeBuilder.java:50`：PartitionChangeBuilder。
- `metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java:1362`：handleBrokerFenced 使用 BrokersToIsrs。
- `metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java:1974`：generateLeaderAndIsrUpdates。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦 Controller 侧分区状态数据结构，不展开 broker 侧 Partition 状态机。
- 目标正文：6000~10000 字。

## 10. 本轮重写主线

1. 从"一个分区在 Controller 里存了什么"开场。
2. 否定"没有反向映射"和"没有 epoch"两种方案。
3. 解释 PartitionRegistration 的字段。
4. 解释 BrokersToIsrs 的反向映射。
5. 解释 PartitionChangeBuilder 如何生成变更记录。
6. 解释 PartitionRegistration.merge() 推进 epoch。
7. 解释 ELR/LastKnownELR 的作用。
8. 收网：PartitionRegistration 是状态，BrokersToIsrs 是索引，merge 是推进器。