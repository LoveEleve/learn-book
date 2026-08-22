# Kafka-19 重写规划

> 题目：除了数据，Kafka 还要记住"谁写过、写到哪"——ProducerStateManager 与 snapshot 主链
> 状态：K-3 Log 存储域第 4 篇，按"ProducerStateManager snapshot"展开
> 目标：解释 Kafka 如何在每个 partition 的日志之外，单独维护一份"producer 状态"（producerId/epoch/sequence/lastOffset 等），并通过 snapshot 文件把它持久化，使崩溃恢复时不必从零回放全部日志。覆盖 `ProducerStateManager` 的 producers 映射、snapshot 的写入/读取/删除、崩溃恢复的 `loadFromSnapshot`、以及事务相关状态（ongoingTxns/unreplicatedTxns）如何随快照一起恢复。

## 1. 读者困惑

- 区分的 ProducerStateManager 和 ProducerStateEntry 有什么区别？
- 为什么幂等/事务需要"按分区维护 producer 状态"？
- snapshot 文件里存的是什么？为什么不直接存日志？
- 崩溃后怎么用 snapshot 恢复，而不是回放整段日志？
- snapshot 什么时候拍、什么时候删？
- `ongoingTxns` / `unreplicatedTxns` 在快照恢复里怎么处理？

## 2. 一句话顿悟

**Kafka 在每个 partition 之外单独维护一份 `producerId → ProducerStateEntry` 的映射，存最近一次成功写入的 epoch/sequence/offset/timestamp 与事务状态；它周期性拍成 snapshot 文件落盘，崩溃恢复时从最新合法 snapshot 加载，再配合少量日志回放补齐，避免从 logStartOffset 全量回放重建 producer 状态。**

## 3. 五要素卡片

### 读者问题

broker 崩溃重启后，log 里可能有很多条消息。Kafka 怎么知道每个 producerId 最近一次写到哪个 offset、最后 seq 是多少、有没有进行中的事务？

### 入口

- `ProducerStateManager`：分区级 producer 状态管理器
- `ProducerStateEntry`：单个 producerId 的状态（epoch/sequence/offset/timestamp）
- `ProducerSnapshot`：snapshot 文件的 schema（含 CRC）
- `SnapshotFile`：snapshot 文件封装
- `loadFromSnapshot` / `readSnapshot` / `writeSnapshot`
- `removeExpiredProducers` / `deleteSnapshotsBefore`
- `ongoingTxns` / `unreplicatedTxns`：事务相关状态

### 状态核心

- `producers: Map<Long, ProducerStateEntry>`：按 producerId 索引
- `ProducerStateEntry`：producerEpoch、lastSeq、lastOffset、lastTimestamp、currentTxnFirstOffset
- snapshot 文件：offset 命名，`producerId → epoch/seq/offset/...` 列表 + CRC
- `ongoingTxns`：进行中事务（按 firstOffset）
- `unreplicatedTxns`：已完成但 marker 在 HW 之上
- `lastMapOffset` / `lastSnapOffset`

### 失败路径

- 不持久化 producer 状态 → 崩溃后 UNKNOWN_PRODUCER_ID / 幂等失效
- 没有 snapshot → 每次恢复都从 logStartOffset 全量回放
- snapshot 损坏 → 无法加载，只能回退到更早 snapshot 或从头回放
- producer 状态过大 → 内存增长与快照文件膨胀
- 不删除过期 producer → hash map 无限增长

### 连接点

- 前文 `Kafka-11`：幂等/事务的三元组依赖 ProducerStateManager。
- 前文 `Kafka-3`：LogSegment 的 producer 状态在独立 snapshot 文件中。
- 前文 `Kafka-17`：roll 时拍一次快照，为崩溃恢复铺路。

## 4. 总图

```text
每个 partition
  → ProducerStateManager
    → producers: Map<producerId, ProducerStateEntry>
      → snapshot 文件（offset 命名，含 CRC）

写入时
  → prepareUpdate / update 更新内存状态

roll / 周期
  → takeSnapshot 把内存状态写为 snapshot 文件

崩溃恢复
  → loadFromSnapshot
    → 加载最新合法 snapshot
      → 从 lastSnapOffset 之后回放日志补齐
        → 重建 producer_state
```

## 5. 关键边界

- 本篇聚焦 producer 状态的持久化与恢复，不重复幂等三元组机制（Kafka-11）。
- 不把 `ProducerStateManager` 与 `TransactionCoordinator` 混成同一个状态：前者在分区本地，后者在事务协调器。
- 不把 snapshot 文件与 log segment 混成同一文件：它们存的内容不同。
- 不展开 snapshot schema 全部字段，只讲关键字段与 CRC。

## 6. 失败方案推演

1. **不持久化 producer 状态**：broker 重启后幂等 producer 无法继续，出现 UNKNOWN_PRODUCER_ID。
2. **每次恢复全量回放**：partition 日志很大时恢复时间线性增长。
3. **只拍一次 snapshot 不再更新**：snapshot 之后的新 producer 状态丢失。
4. **snapshot 损坏直接崩溃**：应为尝试更早 snapshot 或从零重建。

## 7. 误解清单

- “producer 状态存在日志里”：存在独立的 snapshot 文件与内存映射。
- “snapshot 就是日志的备份”：它只存 producerId → epoch/seq/offset，不是消息数据。
- “恢复必须回放整段日志”：从最新 snapshot 加载后只回放少量增量。
- “ongoingTxns 只在内存里”：也会写入 snapshot 的 producer entry 里（通过 currentTxnFirstOffset）。
- “snapshot 越多越好”：snapshot 需要删旧清，否则文件膨胀。

## 8. 证据清单

- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:54`：类注释与 producers 映射。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:184`：loadSnapshots。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:296`：loadFromSnapshot。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:337`：removeExpiredProducers。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:562`：deleteSnapshotsBefore。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:618`：readSnapshot（版本+CRC）。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:661`：writeSnapshot。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateEntry.java:35`：producer 状态字段。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:90`：ongoingTxns。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:93`：unreplicatedTxns。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦 producer 状态持久化与 snapshot 恢复，不展开事务协调器的完整状态机。
- 目标正文：6000~10000 字。

## 10. 本轮重写主线

1. 从"重启后怎么知道 producer 写过什么"开场。
2. 否定"每次全量回放"和"不持久化"两种方案。
3. 解释 ProducerStateManager 与 ProducerStateEntry。
4. 解释 snapshot 文件的结构（字段 + CRC）。
5. 解释 takeSnapshot / writeSnapshot 生命周期。
6. 解释崩溃恢复的 loadFromSnapshot + 增量回放。
7. 解释事务相关状态（ongoingTxns/unreplicatedTxns）。
8. 解释过期清理与 snapshot 删除。
9. 收网：producer 状态 = 独立快照 + 增量回放。