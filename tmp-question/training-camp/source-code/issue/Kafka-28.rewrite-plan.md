# Kafka-28 重写规划

> 题目：Follower 为什么必须先截断再追 leader——AbstractFetcherThread 的四种 epoch 截断路径
> 状态：K-9 ISR 域第 2 篇，按"Follower fetch 与 epoch 截断"展开
> 目标：解释 `AbstractFetcherThread` 在 follower 复制中的核心主链：`maybeTruncate` 为什么先于 `maybeFetch`，`OffsetsForLeaderEpoch` 返回的四种路径如何决定截断策略，`UNDEFINED_EPOCH_OFFSET` 与 `UNDEFINED_EPOCH` 分别代表什么，以及 `divergingEpoch` 响应为什么要求暂停 append 先回退。进一步串联 follower truncate、fenced 处理、out-of-range 重置与 tiered storage 触发。 

## 1. 读者困惑

- follower 为什么不能直接 fetch leader 的新数据？
- `OffsetsForLeaderEpoch` 到底在解决什么问题？
- `UNDEFINED_EPOCH_OFFSET` 与 `UNDEFINED_EPOCH` 的区别是什么？
- leader 返回的 epoch follower 不认识时，为什么还要再发一次 `OffsetsForLeaderEpoch` 请求？
- `divergingEpoch` 为什么会出现在正常 fetch 响应里？
- `FENCED_LEADER_EPOCH` 在 follower fetch 里意味着什么？

## 2. 一句话顿悟

**Follower 复制的第一原则不是“尽快追上 leader”，而是“先和 leader 找到共同祖先”。`AbstractFetcherThread` 每次循环先 `maybeTruncate()`，用 `OffsetsForLeaderEpoch` 让 leader 告诉 follower：当前 leader epoch 对应的 end offset 是多少，再根据 follower 本地是否认识这个 epoch、是否有共同祖先，决定截断策略。只有日志先对齐到共同祖先，后续 fetch / append 才安全。**

## 3. 五要素卡片

### 读者问题

一个 follower 在 leader 切换后，为什么不能直接从自己的 LEO 开始继续 fetch？如果它本地还留着旧 leader 时代的“脏尾巴”，Kafka 是怎么让它先回到共同祖先的？

### 入口

- `AbstractFetcherThread.doWork`：`maybeTruncate` → `maybeFetch`
- `truncateToEpochEndOffsets`：leader epoch 路径
- `truncateToHighWatermark`：无 epoch 路径
- `getOffsetTruncationState`：四种截断分支
- `onPartitionFenced`：`FENCED_LEADER_EPOCH` 处理
- `processFetchRequest`：正常 fetch + diverging epoch 路径
- `handleOutOfRangeError` / `handleOffsetsMovedToTieredStorage`

### 状态核心

- `PartitionFetchState`：`fetchOffset`、`currentLeaderEpoch`、`state`（TRUNCATING / FETCHING）
- `OffsetTruncationState`：`offset` + `truncationCompleted`
- `EpochEndOffset`：leader 返回的 epoch + endOffset
- `UNDEFINED_EPOCH_OFFSET`：leader 无法基于 epoch 给出有效 end offset
- `UNDEFINED_EPOCH`：leader 或 follower 仍在旧协议路径
- `FENCED_LEADER_EPOCH`：follower 的 epoch 落后

### 失败路径

- 直接从本地 LEO 继续 fetch → 日志分叉
- follower 识别不了 leader epoch 但仍盲目截断 → 误删数据
- divergingEpoch 仍继续 append → HW / LEO / epoch 状态全部失真
- 忽略 `FENCED_LEADER_EPOCH` → 旧 leader 数据持续进入 follower

### 连接点

- 前文 `Kafka-9`：Partition 状态机与 broker 本地 leader/follower 切换。
- 前文 `Kafka-24/25`：Controller 侧的 leaderEpoch 与分区状态变更。
- 后文：K-9 第 3 篇（leaderIsrUpdateLock 与状态边界）。

## 4. 总图

```text
AbstractFetcherThread.doWork()
  → maybeTruncate()
    → 有 leader epoch？
      → truncateToEpochEndOffsets()
        → leader.fetchEpochEndOffsets()
          → getOffsetTruncationState() 决定怎么截断
      → 没有 leader epoch？
        → truncateToHighWatermark()
  → maybeFetch()
    → 正常 fetch / 处理 divergingEpoch / FENCED_LEADER_EPOCH
```

## 5. 关键边界

- 本篇只聚焦 follower 的 epoch 截断，不重复 Controller 侧 leader 选举（Kafka-7/24/25）。
- 不把 `UNDEFINED_EPOCH_OFFSET` 和 `UNDEFINED_EPOCH` 混成一类退化路径。
- 不把 `truncateToHighWatermark` 写成“所有无 epoch 情况都走这里”。
- 不把 divergingEpoch 写成“fetch 失败”，它是要求切回截断主线的正常响应分支。

## 6. 失败方案推演

1. **follower 直接从自己的 LEO 开始继续 fetch**：脏尾巴继续扩散，日志分叉。
2. **只按 leader 的返回 offset 盲目截断**：不知道该 offset 是否在本地 epoch 链上，可能误删。
3. **收到 divergingEpoch 还继续 append**：HW/LEO/epoch 语义失真。
4. **收到 FENCED_LEADER_EPOCH 不回退 state**：旧 epoch 持续使用，跟新 leader 永远不收敛。

## 7. 误解清单

- “maybeFetch 才是核心，截断只是异常情况。”：截断是正常复制主链第一步。
- “UNDEFINED_EPOCH_OFFSET = UNDEFINED_EPOCH。”：一个是 leader 无法给出有效 end offset，一个是旧协议没有 epoch 概念。
- “follower 认识不到 leader 的 epoch 就直接失败。”：会先截到更老共同祖先，再继续请求。
- “divergingEpoch 就是错误码。”：它是 fetch 响应里的一种正常分支，要求回到截断主线。
- “truncateToHighWatermark 是通用退化路径。”：只在本地根本拿不到 epoch 时才走。

## 8. 证据清单

- `core/src/main/scala/kafka/server/AbstractFetcherThread.scala:174`：maybeTruncate。
- `core/src/main/scala/kafka/server/AbstractFetcherThread.scala:211`：truncateToEpochEndOffsets。
- `core/src/main/scala/kafka/server/AbstractFetcherThread.scala:244`：truncateToHighWatermark。
- `core/src/main/scala/kafka/server/AbstractFetcherThread.scala:262`：maybeTruncateToEpochEndOffsets。
- `core/src/main/scala/kafka/server/AbstractFetcherThread.scala:604`：getOffsetTruncationState。
- `core/src/main/scala/kafka/server/AbstractFetcherThread.scala:276`：FENCED_LEADER_EPOCH 分支。
- `core/src/main/scala/kafka/server/AbstractFetcherThread.scala:318`：processFetchRequest。
- `core/src/main/scala/kafka/server/AbstractFetcherThread.scala:356`：divergingEpoch 分支。
- `core/src/main/scala/kafka/server/AbstractFetcherThread.scala:740`：handleOutOfRangeError。
- `core/src/main/scala/kafka/server/AbstractFetcherThread.scala:780`：handleOffsetsMovedToTieredStorage。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦 follower epoch 截断主链，不展开 broker 侧 partition 状态机或 tiered storage 内部实现。
- 目标正文：7000~11000 字。

## 10. 本轮重写主线

1. 从"为什么 follower 不能直接继续 fetch"开场。
2. 否定直接从 LEO 开始和只看 leader offset 两种方案。
3. 解释 `maybeTruncate` 先于 `maybeFetch` 的设计。
4. 解释 `OffsetsForLeaderEpoch` 请求/响应。
5. 解释 `getOffsetTruncationState` 的四种分支。
6. 解释 `divergingEpoch` / `FENCED_LEADER_EPOCH` / out-of-range 的回退路径。
7. 收网：先找共同祖先，再追 leader。