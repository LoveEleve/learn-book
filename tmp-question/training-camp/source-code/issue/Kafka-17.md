# Kafka-17. 日志不能无限增长——Log retention、activeSegment 滚动与 segment 删除主链

> 场景：Kafka-3 已经讲过 LogSegment 的四文件布局和索引结构，但留下了一个问题：**日志会持续增长，Kafka 怎么在磁盘有限的情况下决定哪些数据可以删，怎么安全地删？** 本篇回答这个问题：Log retention 的三种删除条件、activeSegment 的滚动条件、以及 roll 时为什么必须同步拍 ProducerStateManager 快照。

## 先把真正的困惑摆出来：配置 retention.ms=7 天，到底删什么

假设你配置 `retention.ms=604800000`（7天）。一个 segment 里包含 6 天前到 8 天前的消息。最老的那批消息已经超过 7 天了，但最新那批还在 7 天内。Kafka 会怎么做？

- 删 segment 内过期消息？不行，因为 segment 是文件粒度的，不能只删一部分。
- 整段保留？那 8 天前的消息就永远删不掉了。
- 整段删除？那 6 天前的消息就丢了。

Kafka 的选择是：**按 segment 整段删，判定依据是 segment 里最新（largestTimestamp）消息的时间。** 如果 segment 里**最新**的消息都已经超过 retention 边界，说明整个 segment 全部过期，可以整段删。如果最新消息还没过期，那即使部分旧消息已经过期，也不能删整段。

```text
segment-1: 8 天前 ~ 6 天前 → 最新消息 6 天前，未到 7 天 → 保留
segment-2: 10 天前 ~ 8 天前 → 最新消息 8 天前，超过 7 天 → 整段删
```

所以 retention 不是消息级别的精确过期货架，而是 **segment 级别的整段淘汰**，且判定用的是 segment 里的最大时间戳，不是最小。

*关键设计（斜体）：* *Kafka 的日志保留按 segment 整段删除：`deleteOldSegments` 依次检查 retention 时间、retention 大小、logStartOffset 三条条件，满足就整段删；`maybeRoll` 在 segment 大小、时间、索引容量任一超标时切换新 activeSegment，并同步拍一次 ProducerStateManager 快照。*[模式: 整段淘汰 + 多条件检查 + 滚动时拍快照]

## 第一层：deleteOldSegments 的三种删除条件顺序执行

`deleteOldSegments()` 是 log retention 的入口（`UnifiedLog.java:1894`）。它不是选一种条件，而是**三种条件依次执行，每种条件独立删除**：

```text
deleteOldSegments()
  → 如果 config.delete == true
    → deleteLogStartOffsetBreachedSegments()
    → deleteRetentionSizeBreachedSegments()
    → deleteRetentionMsBreachedSegments()
  → 如果 config.delete == false（compact 模式）
    → deleteLogStartOffsetBreachedSegments()  // 只执行这一个
```

注意 compact 模式下只检查 logStartOffset，不检查时间/大小。因为 compact 模式的清理策略是 Log Compaction（Kafka-12），不是 retention delete。

### 按 logStartOffset 删除

`deleteLogStartOffsetBreachedSegments()` 的判定依据是**下一个 segment 的 baseOffset**：如果下一个 segment 的 baseOffset 仍 `<= logStartOffset`，说明当前 segment 整体都落在 logStartOffset 之前，可以删；当下一个 segment 的 baseOffset 已经高于 logStartOffset 时，就停下来，保留那个"包含 logStartOffset"的 segment。

```text
logStartOffset = 150

segment-A base=100, 下一个 base=200 → 200 <= 150 ? 否 → 保留 A（包含 150）
segment-B base=0,   下一个 base=100 → 100 <= 150 ? 是 → 可删 B
```

所以 logStartOffset 是日志可读的绝对下界，低于它的 segment 即使包含数据，对消费者也无意义；但 Kafka 会保留那个横跨 logStartOffset 的 segment。

### 按 retention 大小删除

`deleteRetentionSizeBreachedSegments()` 检查 `log size > retentionSize` 时，从最早 segment 开始依次删除，直到总大小降到 retentionSize 以下。注意它计算的是累计大小差，而不是每次删除一个 segment 就重新计算。

### 按 retention 时间删除

`deleteRetentionMsBreachedSegments()` 用 `segment.largestTimestamp` 与当前时间比较，如果 `now - largestTimestamp > retentionMs`，说明这个 segment 里**最新**的消息（`largestTimestamp`）也已经过期，整个 segment 可以删。如果 segment 没有 `largestTimestamp`，则用 `lastModified` 代替。所以它判的是 segment 的"最大时间戳"，不是最早消息的时间。

三种条件互不排斥：一个 segment 可能同时满足多种条件，也可能只满足其中一种。谁先触发的删除就由谁执行。

## 第二层：activeSegment 受 HW 边界保护，正常情况下不会参与删除

这是 retention 的一条重要语义：**activeSegment 正常情况下不会被 `deleteOldSegments` 删除。** 但源码不是显式"跳过 active"，而是通过 `deletableSegments` 里的一道护栏：`highWatermark() >= upperBoundOffset`（`UnifiedLog.java:1822`）。其中 active（最后）segment 的 upperBound 是 `logEndOffset`。正常时 HW < LEO，所以 active 不满足删除条件；只有 HW 追平 LEO 且 retention 条件也满足时，才理论上有被删的可能。

所以一个 segment 的生命周期是：

```text
创建（activeSegment，可写）
  → 写入数据
    → maybeRoll 触发（roll 出新 segment）
      → 旧 segment 变为非活跃（不可写，但可读）
        → 等 retention 条件满足
          → 被 deleteOldSegments 删除
```

## 第三层：maybeRoll 在四种条件下触发

`maybeRoll(messagesSize, appendInfo)` 在每次写入后被调用。它检查当前 activeSegment 是否需要退役，条件包括：

```text
segment.shouldRoll(rollParams)
  → 大小超标？segment.size > segmentSize
  → 时间超标？当前时间 - segment 创建时间 > segmentMs
  → offset index 满了？index.entries > index.maxEntries
  → time index 满了？timeIndex.entries > index.maxEntries
  → offset 超出相对偏移上限？!canConvertToRelativeOffset(maxOffset)
  → 任一超标 → 需要 roll
```

这五个条件任何一个满足，`maybeRoll` 就会调用 `roll()` 创建新 segment。其中最常见的是大小超标：默认 `segment.bytes=1GB`，达到这个值就切段。最后一个条件 `canConvertToRelativeOffset` 和 Kafka-12 里 Cleaner 分组时的 offset 范围约束呼应：如果 segment 内 offset 差值超过 Integer.MAX_VALUE，索引就无法用相对偏移表达，必须提前滚动。

注意 `roll()` 是基于 `segment.shouldRoll()` 的返回值，而 `shouldRoll` 的参数包括 `messagesSize`（当前写入的字节数）、`appendInfo.maxTimestamp`、`appendInfo.lastOffset`。

## 第四层：roll() 不只是切段，还要同步拍 ProducerStateManager 快照

`roll()` 创建新 segment 后，还会做一件非常重要的事：**同步更新 ProducerStateManager 并拍快照。**

```text
roll()
  → localLog.roll(nextOffset) 创建新 activeSegment
    → producerStateManager.updateMapEndOffset(newSegment.baseOffset())
      → producerStateManager.takeSnapshot(false)
        → 调度异步 flush 旧 segment
```

`updateMapEndOffset` 把 producer state 的 end offset 与 segment base offset 对齐。`takeSnapshot` 把当前 producer 状态（producerId/epoch/sequence/lastOffset 等）落盘，方便崩溃恢复时从该 segment 的位置开始重建。

如果 `roll()` 不做这个快照，broker 崩溃后，整个日志的 producer 状态都要从零开始回放，恢复时间随日志增长而线性增长。

## 第五层：checkpoint 文件记录删除边界

Kafka 在 LogManager 层面维护了两个 checkpoint 文件：

- `recovery-point-offset-checkpoint`：记录每个 partition 已清理到哪个 offset（即最后成功 flush 的位置）；
- `log-start-offset-checkpoint`：记录每个 partition 的 logStartOffset。

这些 checkpoint 文件帮助 broker 重启时快速定位：哪些 segment 已经稳定（不需要回放），logStartOffset 从哪里开始。

## 收网：retention 按 segment 整段删，roll 按条件切，roll 时同步拍快照

把整篇压成一句话：Kafka 的日志保留不是按消息精确截止，而是按 segment 整段删除——`deleteOldSegments` 依次检查 logStartOffset、retention 大小、retention 时间三条条件，每一条独立执行；`maybeRoll` 在 segment 大小、时间、索引容量任一超标时切换新 activeSegment；`roll()` 创建新 segment 后同步拍一次 ProducerStateManager 快照，为崩溃恢复打下基础。

```text
写入记录
  → maybeRoll 检查
    → 需要 roll？创建新 segment + 拍快照
      → 旧 segment 变为非活跃
        → 定时/触发 deleteOldSegments
          → 按 logStartOffset / 大小 / 时间 整段删除
```

到这里，主线只发生了五件事。

第一，retention 按 segment 整段删，不按消息粒度。

第二，activeSegment 受 HW 边界保护，正常不会参与删除。

第三，`deleteOldSegments` 有三种条件，按 logStartOffset / 大小 / 时间顺序执行。

第四，`maybeRoll` 在大小、时间、索引容量任一超标时触发。

第五，`roll()` 创建新 segment 后同步拍 ProducerStateManager 快照。

**本篇的一句话困惑**：配置 retention.ms=7 天，到底删什么？

**本篇的一句话顿悟**：Kafka 按 segment 整段删——判定依据是 segment 里最新消息（largestTimestamp）超出 retention 边界才整段删；activeSegment 受 HW 边界保护；roll 时同步拍 ProducerStateManager 快照。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“retention.ms 精确到消息级别。”** 按 segment 整段删，判定用 segment 最大时间戳，不是逐消息的过期。
2. **“retention.bytes 是最新 N 字节的保留。”** 删除最早 segment 直到总大小达标。
3. **“activeSegment 会被删除。”** 正常情况下受 HW 边界保护不会删，保护不是显式跳过 active，而是 `highWatermark() >= upperBoundOffset` 检查。
4. **“roll 就是删除旧 segment。”** 只是创建新 activeSegment，旧 segment 转为非活跃，等 retention 条件才删。
5. **“deleteOldSegments 只检查一种条件。”** 按 logStartOffset、大小、时间依次检查。

### 关键证据清单

- `storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java:1894`：deleteOldSegments 入口。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java:1908`：deleteRetentionMsBreachedSegments。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java:1946`：deleteRetentionSizeBreachedSegments。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java:1977`：deleteLogStartOffsetBreachedSegments。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java:2052`：maybeRoll。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/LogSegment.java:167`：shouldRoll 条件，含 canConvertToRelativeOffset。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java:2107`：roll() 创建新 segment 并拍快照。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/LogManager.java:37`：checkpoint 文件。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦 retention delete 与 activeSegment 滚动，不展开 Log Compaction（Kafka-12）与 tiered storage。
- 不把 `roll()` 写成“立即删除旧 segment”：旧 segment 转为非活跃，等 retention 条件满足才删。
- 不把 retention delete 与 compact 混成同一策略。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-3`（LogSegment 四文件布局）、`Kafka-12`（Log Compaction 对照）。
- 后续桥接：下一篇可继续 K-3 Log 存储域第 3 篇（LazyIndex 与 ProducerStateManager snapshot 深讲），或进入 K-4 Consumer 子专题。