# Kafka-19. 除了数据，Kafka 还要记住"谁写过、写到哪"——ProducerStateManager 与 snapshot 主链

> 场景：Kafka-11 讲幂等/事务时反复提到 `ProducerStateManager`，但它只是一闪而过。本篇把它补深：除了日志本身，Kafka 还在每个 partition 维护一份独立的"producer 状态"，并用 snapshot 文件持久化，让崩溃恢复不必从 logStartOffset 全量回放。这是 K-3 Log 存储域第 4 篇，也是 Log 存储域收官篇。

## 先把真正的困惑摆出来：broker 重启后，怎么知道 producer 写过什么

假设一个 partition 写了几千万条消息。broker 崩溃重启后，要做的一件重要事是：**恢复每个 producerId 的幂等状态**——最近一次成功写入的 offset、最后的 sequence、当前 epoch、有没有进行中的事务。

为什么这么重要？因为幂等/事务 producer 靠 `<producerId, epoch, sequence>` 去重（Kafka-11）。如果重启后 Kafka 不知道这个 producer 最后写到哪，它就会：

- 把重试的序列号当作新序列号，破坏去重；
- 或出现 UNKNOWN_PRODUCER_ID / OutOfOrderSequence 错误。

一个直觉方案是：重启时把整个 partition 日志从头扫一遍，重建 producer 状态。这可行，但日志很大时恢复时间线性增长，代价难以接受。

另一个方案是：什么都不存，只靠 snapshot。这又不够，因为 snapshot 之后新产生的数据怎么办。

Kafka 的答案是：**用 snapshot + 增量回放**。

```text
恢复 producer 状态
  = 加载最新合法 snapshot
    + 从 lastSnapOffset 之后增量回放日志
      → 重建完整 producer 状态
```

*关键设计（斜体）：* *Kafka 在每个 partition 之外维护 `producerId → ProducerStateEntry` 映射，记录最近一次成功写入的 epoch/sequence/offset/timestamp 与事务状态；它周期性把这份内存状态拍成 snapshot 文件（含 CRC）落盘；崩溃恢复时先加载最新合法 snapshot，再回放其后少量日志补齐，避免全量回放。*[模式: 独立状态 + 快照 + 增量回放]

## 第一层：ProducerStateManager 是分区级的 producer 状态管家

`ProducerStateManager` 的职责很清晰：维护一个 `Map<Long, ProducerStateEntry>`，key 是 producerId，value 是 `ProducerStateEntry`（`ProducerStateManager.java:54`）。

`ProducerStateEntry` 记录一个 producer 在该分区最近一次写入的状态（`ProducerStateEntry.java:35`）：

- `producerEpoch`
- `lastSeq`（最后一次 sequence）
- `lastOffset` / `lastDataOffset`
- `lastTimestamp`
- `currentTxnFirstOffset`（如果正在事务中）

这份映射是**分区本地**的：每个 partition 有自己的 ProducerStateManager，不跨分区共享。这也符合幂等的语义：sequence 是分区内连续的，跨分区没有全局序列号。

写入时，`prepareUpdate()` 取出或创建一个该 producerId 的 entry，`update()` 把它推进到新的 epoch/sequence/offset（`ProducerStateManager.java:376`、`384`）。

这解决了"内存里怎么维护"的问题，但还没解决"怎么持久化"。

## 第二层：snapshot 文件存的是 producer 状态，不是数据

Kafka 不会把整个 payload 写进 snapshot。snapshot 文件只存每个 producer 的元数据：

```text
snapshot 内容（每个 producer 一条）
  → producerId
  → epoch
  → lastSequence
  → lastOffset
  → offsetDelta
  → timestamp
  → coordinatorEpoch
  → currentTxnFirstOffset
```

`writeSnapshot()` 把这些字段序列化成一个 `ProducerSnapshot` 对象，并额外写入 **CRC** 校验（`ProducerStateManager.java:661-692`）。CRC 保证文件在崩溃或部分写入时能被识别为损坏。

snapshot 文件的命名用 offset，代表"这个快照之后的状态起点"。这样恢复时可以精确知道：从这个 snapshot 加载的状态，覆盖到哪个 offset 之前的日志。

所以 snapshot 与日志的关系是互补的：

- 日志存"数据本身"；
- snapshot 存"数据的 producer 状态元数据"；
- 恢复时按 offset 对齐，snapshot 覆盖之前的部分，日志回放覆盖之后的部分。

## 第三层：snapshot 的写入时机与生命周期

`ProducerStateManager` 的 snapshot 不是每条消息都拍，而是按需：

- `roll()` 时拍一次（Kafka-17 讲过）：新 segment 创建时，把 producer 状态落盘，便于恢复时从该 segment 起点重建；
- `deleteSnapshotsBefore(offset)`：logStartOffset 推进后，删除 offset 之前的 snapshot，避免文件膨胀（`ProducerStateManager.java:562`）；
- `removeStraySnapshots`：清理没有对应 segment 的 snapshot。

这些操作共同维护一个"snapshot 列表"（`ConcurrentSkipListMap<Long, SnapshotFile>`，按 offset 排序），方便查找最新/最旧 snapshot。

## 第四层：崩溃恢复的 loadFromSnapshot

重启后，`loadFromSnapshot(logStartOffset, currentTime)` 从最新 snapshot 开始加载（`ProducerStateManager.java:296`）：

```text
loadFromSnapshot()
  → latestSnapshotFile()
    → 读取该 snapshot
      → 校验版本 + CRC
        → 合法：加载全部 producer 状态
          → lastSnapOffset = snapshot.offset
            → 返回
        → 损坏：删除该 snapshot，尝试更早的
  → 没有 snapshot：从 logStartOffset 起跑
```

这里有两个关键点：

1. **优先用最新合法 snapshot**，避免从 logStartOffset 全量回放；
2. **snapshot 损坏时回退到更早的**，而不是直接崩溃。`CorruptSnapshotException` 让 Kafka 删掉坏文件、尝试更早的快照。

`lastSnapOffset` 告诉恢复逻辑"内存状态已经覆盖到这个 offset 之前"，之后从 `lastSnapOffset` 起回放日志，补上 snapshot 之后新增的 producer 状态。

这就是"快照 + 增量回放"的落点：快照覆盖历史，回放覆盖增量，两者按 offset 无缝衔接。

## 第五层：事务状态也在 snapshot 里一起恢复

`ProducerStateManager` 不只是幂等状态，还维护两份事务相关的映射：

- `ongoingTxns`：进行中（未决）的事务，按 firstOffset 排序（`ProducerStateManager.java:90`）；
- `unreplicatedTxns`：已完成但 marker 在 HW 之上的事务（`ProducerStateManager.java:93`）。

这两份状态在快照中如何恢复？snapshot 的每个 producer entry 里有一个 `currentTxnFirstOffset`。加载时，如果某 producer 的 entry 有这个字段，就把对应事务放回 `ongoingTxns`（`ProducerStateManager.java:326`）。

```text
读 snapshot
  → 对每个 producer entry
    → 若 currentTxnFirstOffset 存在
      → ongoingTxns.put(firstOffset, TxnMetadata)
```

这保证了重启后 Kafka 不仅知道 producer 的 sequence，还知道"这个 producer 当时正在写哪个事务"。

`unreplicatedTxns` 则与 `firstUnstableOffset()` 关联：它表示"已完成但还没复制到 HW 的事务"，影响 LSO 的计算（`ProducerStateManager.java:246`）。恢复时这部分状态也随 snapshot 一起重建。

这也解释了为什么 Kafka-11 里 `read_committed` 能工作：它不是靠客户端猜，而是靠 `ProducerStateManager` 在恢复后仍能精确知道事务边界。

## 第六层：过期 producer 的清理

`ProducerStateManager` 还负责 `removeExpiredProducers()`（`ProducerStateManager.java:337`）：

```text
removeExpiredProducers(now)
  → 遍历 producers 映射
    → 没有进行中事务 && now - lastTimestamp >= producerIdExpirationMs
      → 移除该 producer
```

默认的 `producer.id.expiration.ms` 是 24h。如果不清理，producer 状态映射会随着时间无限增长。这也是 snapshot 生命周期的一部分：清理后内存变小，后续 snapshot 也更小。

## 收网：producer 状态 = 独立快照 + 增量回放

把整篇压成一句话：Kafka 在每个 partition 维护 `producerId → ProducerStateEntry` 状态，记录最近一次 epoch/sequence/offset/timestamp 与事务状态；它周期性把这份状态拍成含 CRC 的 snapshot 文件；崩溃恢复时先加载最新合法 snapshot，再回放其后日志补齐，避免从 logStartOffset 全量回放；事务的 ongoingTxns/unreplicatedTxns 也随 snapshot 一起重建。

```text
写入 → ProducerStateManager.update（内存状态）

roll / 周期 → writeSnapshot（落盘，含 CRC）

崩溃恢复
  → loadFromSnapshot（最新合法 snapshot）
    → 从 lastSnapOffset 回放增量
      → 完整重建 producer 状态
        → ongoingTxns / unreplicatedTxns 一起恢复

清理
  → removeExpiredProducers（24h 过期）
  → deleteSnapshotsBefore（logStartOffset 推进后删旧）
```

到这里，主线只发生了六件事。

第一，ProducerStateManager 维护分区本地的 producer 状态映射。

第二，snapshot 存 producer 元数据，不是数据。

第三，snapshot 带 CRC，损坏时回退更早的。

第四，崩溃恢复 = 加载最新 snapshot + 增量回放。

第五，事务状态随 snapshot 一起恢复。

第六，过期 producer 定期清理，防止状态无限增长。

**本篇的一句话困惑**：broker 重启后，怎么知道每个 producer 写过什么？

**本篇的一句话顿悟**：Kafka 用"独立 producer 状态 + snapshot 落盘 + 增量回放"——先加载最新合法 snapshot，再回放其后日志，避免全量回放；事务状态也随快照一起恢复。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“producer 状态存在日志里。”** 存在独立的 snapshot 文件与内存映射。
2. **“snapshot 是日志的备份。”** 它只存 producerId → epoch/seq/offset，不是消息数据。
3. **“恢复必须回放整段日志。”** 从最新 snapshot 加载后只回放少量增量。
4. **“ongoingTxns 只在内存里。”** 通过 producer entry 的 currentTxnFirstOffset 写入 snapshot。
5. **“snapshot 越多越好。”** snapshot 需要删除旧文件，否则膨胀。

### 关键证据清单

- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:54`：类注释与 producers 映射。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:296`：loadFromSnapshot。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:337`：removeExpiredProducers。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:562`：deleteSnapshotsBefore。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:618`：readSnapshot（版本+CRC）。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:661`：writeSnapshot。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateEntry.java:35`：producer 状态字段。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:90`：ongoingTxns。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:93`：unreplicatedTxns。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦 producer 状态持久化与 snapshot 恢复，不展开事务协调器的完整状态机。
- 本篇不把 `ProducerStateManager` 与 `TransactionCoordinator` 混成同一个状态：前者分区本地，后者事务协调器。
- 不展开 snapshot schema 全部字段细节，只讲关键字段与 CRC。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-3`（LogSegment 存储）、`Kafka-11`（幂等/事务三元组）、`Kafka-17`（roll 时拍快照）。
- 后续桥接：K-3 Log 存储域 4 篇收官，下一篇可进入 K-4 Consumer 域第 2 篇（Classic vs Async 模型），或切到其他域。