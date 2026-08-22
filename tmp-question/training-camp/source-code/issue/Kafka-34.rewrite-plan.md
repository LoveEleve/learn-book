# Kafka-34 重写规划

> 题目：事务的状态存哪、由谁推进——TransactionCoordinator 与 `__transaction_state` 主链
> 状态：K-12 事务幂等域第 3 篇，按"TransactionCoordinator / __transaction_state"展开
> 目标：解释 Kafka 事务协调器如何通过 `TransactionStateManager` 内嵌到 `__transaction_state` 分区，如何按 `transactionalId` 哈希定位事务日志分区，以及 `TransactionCoordinator` 如何推进事务状态机（EMPTY → ONGOING → PREPARE_COMMIT/ABORT → COMPLETE_*）并把每次状态变更写入事务日志。

## 1. 读者困惑

- 事务协调器是独立服务还是嵌在 broker 里？
- 每个 transactionalId 对应哪个协调器，是怎么定位的？
- 事务状态存在哪，怎么能从崩溃中恢复？
- `TransactionCoordinator` 在 InitProducerId / AddPartitions / EndTxn 时怎么流转状态？
- 为什么事务日志也要用 partition 分发，而不是全局一张表？
- `TransactionMetadata` 保存了哪些内容？

## 2. 一句话顿悟

**Kafka 的事务协调器不是独立服务，而是 `__transaction_state` 分区 leader 所在 broker 上的 `TransactionCoordinator` 实例。每个 transactionalId 先哈希到某个事务日志分区，再由该分区 leader 上的 coordinator 主持它的事务状态机；状态不是只在内存，而是通过 `appendTransactionToLog` 写入事务日志，崩溃后从日志重建。**

## 3. 五要素卡片

### 读者问题

一笔事务在 `producer.send()` 之前，需要先给 transactionalId 绑定 producerId 和状态，这个过程到底发生在哪、状态存哪？

### 入口

- `TransactionCoordinator`：事务状态机主持者
- `TransactionStateManager`：事务日志分区 + 状态缓存
- `TransactionMetadata`：单笔事务状态
- `__transaction_state`：事务日志 topic
- `partitionFor(transactionalId)`：transactionalId → 分区
- `appendTransactionToLog`

### 状态核心

- `TransactionState`：EMPTY / ONGOING / PREPARE_COMMIT / PREPARE_ABORT / COMPLETE_COMMIT / COMPLETE_ABORT / DEAD / PREPARE_EPOCH_FENCE
- `TransactionMetadata`：producerId / epoch / partitions / state / lastUpdateTime
- `partitionFor(transactionalId)`：哈希取模
- `coordinatorEpoch`：协调器对分区的任期

### 失败路径

- coordinator 直接改内存不改日志 → 崩溃后事务状态丢失
- 事务日志只在一台 broker，不按分区分发 → 单点
- transaction 状态机跳变非法 → 需要 pending transition 校验
- coordinator epoch 过期仍在写 → 旧协调器覆盖新状态

### 连接点

- 前文 `Kafka-33`：幂等 producer 校验，本篇接上事务身份与状态。
- 前文 `Kafka-26/27`：KRaft 元数据日志与协调器分片机制一脉相承。
- 后文：K-12 第 4 篇（marker / control batch / read_committed）。

## 4. 总图

```text
transactionalId
  → hash → __transaction_state 分区 p
    → 分区 p 的 leader broker
      → 该 broker 上的 TransactionCoordinator 主持
        → TransactionMetadata（producerId/epoch/state/partitions）
          → appendTransactionToLog 写入分区 p
            → 内存 + 日志一致
```

## 5. 关键边界

- 本篇聚焦 coordinator 与事务日志，不展开 marker 扇出（K-12 第 4 篇）。
- 不把 `TransactionStateManager` 与 GroupCoordinator 的 `GroupMetadataManager` 混同。
- 不把事务状态机写成无 pending transition 的单一路径。
- 不把 `__transaction_state` 与 `__consumer_offsets` / metadata log 混成同一 topic。

## 6. 失败方案推演

1. **事务状态只存内存**：coordinator 重启后状态丢失。
2. **不按 transactionalId 分区**：所有事务都由同一协调器承担，无法水平分散。
3. **事务日志不改即推进状态**：崩溃后无法重建到正确迁移点。
4. **忽略 coordinator epoch**：旧协调器可能覆盖新协调器的状态。

## 7. 误解清单

- “事务协调器是单独进程。”：它是 broker 上的一个组件（分片）。
- “transactionalId 直接对应一台固定的 broker。”：它哈希到 `__transaction_state` 分区，分区 leader 承载协调者。
- “TransactionStateManager 是全局单表。”：它按事务日志分区组织。
- “事务状态只往前走不回退。”：pending transition 与 coordinator epoch 会约束迁移顺序。
- “__transaction_state 和 __consumer_offsets 是一个 topic。”：是两个不同内部 topic。

## 8. 证据清单

- `core/src/main/scala/kafka/coordinator/transaction/TransactionStateManager.scala:70`：TransactionStateManager。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionStateManager.scala:444`：partitionFor(transactionalId)。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionStateManager.scala:652`：appendTransactionToLog。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionCoordinator.scala:113`：handleInitProducerId。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionCoordinator.scala:505`：handleEndTransaction。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionCoordinator.scala:91`：TransactionCoordinator 类。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionMetadata.scala`：TransactionMetadata。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionContextManager/Later`：如适用（略）。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦 coordinator 与事务日志，不展开 marker 扇出与 read_committed（K-12 第 4 篇）。
- 目标正文：6000~10000 字。

## 10. 本轮重写主线

1. 从"transactionalId 怎么找到主持人"开场。
2. 否定“独立进程”和“不分区”两种方案。
3. 解释 `partitionFor` 与协调器分片。
4. 解释 `TransactionMetadata` 与状态机。
5. 解释 `handleInitProducerId`。
6. 解释 `appendTransactionToLog` 与恢复。
7. 收网：事务状态 = 内存状态 + 事务日志，由 coordinator 分片主持。