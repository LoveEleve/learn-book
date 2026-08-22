# Kafka-9 重写规划

> 题目：Follower 为什么要先截断、再追 leader——Partition 状态机、ISR 扩缩与 Epoch 截断主链
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：解释 Kafka 在 broker 本地如何把 controller 下发的分区归属真正落成运行时状态：`Partition` 如何在 leader/follower 间切换、ISR 为什么不是一个静态集合、follower 为什么必须先按 epoch 截断日志再 fetch，以及 leader 如何根据 follower 进度扩缩 ISR 并更新高水位。

## 1. 读者困惑

- Controller 已经决定了 leader/ISR，为什么 broker 本地还需要一个复杂的 `Partition` 状态机？
- follower 为什么不能直接 fetch leader 数据，非要先做 epoch 截断？
- ISR 为什么会一会儿扩、一会儿缩，谁来判定 follower 还算不算“同步”？
- `leaderEpoch` 在 broker 本地到底干什么，为什么会出现 `FENCED_LEADER_EPOCH`？
- `leaderIsrUpdateLock`、`PendingExpandIsr`、`PendingShrinkIsr` 这些状态在解决什么一致性问题？
- `acks=all`、HW、minISR 为什么和 ISR 状态机绑得这么紧？

## 2. 一句话顿悟

**Controller 决定“谁应该当 leader”，但 broker 本地还要决定“怎样安全地变成 leader / follower、怎样把日志追到正确位置、什么时候把 follower 算进 ISR”。Kafka 的做法是：LeaderAndIsr 先驱动 `Partition.makeLeader/makeFollower` 切换本地角色，follower 的 `AbstractFetcherThread` 必须先按 leader epoch 截断到共同祖先，再进入 fetch；leader 再根据 follower 的追赶进度触发 `maybeExpandIsr/maybeShrinkIsr`，并以 ISR 为依据推进 HW 与满足 `acks=all`。**

## 3. 五要素卡片

### 读者问题

一个 follower 在 leader 切换后，为什么不能直接从自己当前 LEO 往后追？如果它本地还有旧 leader 时代写下的“脏尾巴”，Kafka 是怎么让它先回到共同祖先、再继续复制的？

### 入口

- `Partition`：broker 本地分区状态机（leader/follower、ISR、HW、epoch）
- `ReplicaManager.becomeLeaderOrFollower`：接收 `LeaderAndIsrRequest`
- `Partition.makeLeader` / `makeFollower`：本地角色切换
- `AbstractFetcherThread`：follower 复制线程，`maybeTruncate` → `maybeFetch`
- `OffsetsForLeaderEpoch`：按 epoch 找共同祖先
- `maybeExpandIsr` / `maybeShrinkIsr`：leader 侧 ISR 扩缩
- `PendingExpandIsr` / `PendingShrinkIsr` / `CommittedPartitionState`

### 状态核心

- `PartitionState`：`CommittedPartitionState` / `PendingExpandIsr` / `PendingShrinkIsr`
- `AssignmentState`：`SimpleAssignmentState` / `OngoingReassignmentState`
- `leaderEpoch` / `partitionEpoch` / `leaderEpochStartOffset`
- `leaderIsrUpdateLock`：保护 ISR 与 leader 切换
- follower 复制阶段：Truncating → Fetching

### 失败路径

- follower 不截断直接 fetch → 带着旧 leader 的脏尾巴继续追，日志分叉
- ISR 不收缩 → 落后副本仍被算作 in-sync，HW/acks=all 失真
- ISR 不扩张 → 已追上的 follower 永远回不来，吞吐与容灾都变差
- leaderEpoch 不校验 → 旧 leader / 旧 fetcher 继续写，脑裂数据落盘
- `makeFollower` 不先清 ISR → 切换过程下出现错误的 under-min-isr / HW 计算

### 连接点

- 前文 `Kafka-7`：Controller 决定 leader/ISR 归属；本篇解释 broker 本地如何执行这个归属。
- 前文 `Kafka-4`：Consumer 看到的 `FENCED_LEADER_EPOCH`、HW，本篇补上 broker 侧来源。
- 后文可以再回 Producer/事务/幂等，复用这里的 HW、ISR、epoch 语义。

## 4. 总图

```text
Controller 下发 LeaderAndIsr
  → ReplicaManager.becomeLeaderOrFollower
    → Partition.makeLeader / makeFollower

若本地变 follower
  → AbstractFetcherThread.maybeTruncate
    → OffsetsForLeaderEpoch 找共同祖先
      → truncate
        → maybeFetch 继续追 leader

若本地变 leader
  → follower fetch 回报进度
    → maybeExpandIsr / maybeShrinkIsr
      → AlterPartition 回报 controller
        → committed ISR 更新
          → HW 推进 / acks=all 满足
```

## 5. 关键边界

- 本篇聚焦 broker 本地 Partition/ISR 运行时状态，不回头展开 KRaft controller 选主（Kafka-8）或 Consumer fetch 语义（Kafka-4）。
- 不把 ISR 当静态集合：本篇必须讲清 committed ISR 与 pending ISR 之间的过渡态。
- 不把 epoch 截断讲成“简单 truncate 到 leader LEO”：核心是按 leader epoch 找共同祖先，LEO/HW 只是退化路径或边界条件。
- 不把 HW 与 LEO 混成一个概念：LEO 是日志末尾，HW 是可见/已复制边界。

## 6. 失败方案推演

1. **follower 直接从本地 LEO 继续 fetch**：旧 leader 残留的尾部数据可能比新 leader 更“长”，形成分叉。
2. **只按 HW 截断，不看 epoch**：能保守地安全，但恢复过慢，且丢失可保留的数据。
3. **ISR 永不收缩**：落后副本继续参与 HW 计算，`acks=all` 被拖慢甚至失真。
4. **ISR 永不扩张**：已经追上的副本也回不到 ISR，minISR 长期吃紧。

## 7. 误解清单

- “LeaderAndIsr 一到，本地状态就瞬间稳定了”：还要经历 makeLeader/makeFollower、本地 epoch 切换与 fetcher 截断。
- “ISR 就是 controller 发来的静态数组”：broker 本地还有 pending expand/shrink 过渡态。
- “epoch 截断就是 truncate 到 leader 最新 offset”：核心是先找共同 epoch 的 end offset。
- “HW 就是日志末尾”：HW 只到 ISR 已复制边界，通常小于等于 LEO。
- “follower 追上 leader 就自动算进 ISR”：还要满足 leader 的扩张条件并经过 AlterPartition 确认。

## 8. 证据清单

- `core/src/main/scala/kafka/cluster/Partition.scala:197`：`PartitionState` / `PendingExpandIsr` / `PendingShrinkIsr` / `CommittedPartitionState`
- `core/src/main/scala/kafka/cluster/Partition.scala:328`：`leaderIsrUpdateLock`
- `core/src/main/scala/kafka/cluster/Partition.scala:733`：`makeLeader`
- `core/src/main/scala/kafka/cluster/Partition.scala:839`：`makeFollower`
- `core/src/main/scala/kafka/cluster/Partition.scala:1018`：`maybeExpandIsr`
- `core/src/main/scala/kafka/cluster/Partition.scala:1231`：`maybeShrinkIsr`
- `core/src/main/scala/kafka/cluster/Partition.scala:1361`：`appendRecordsToLeader` 与 minISR/acks=all
- `core/src/main/scala/kafka/server/ReplicaManager.scala:1992`：`becomeLeaderOrFollower`
- `core/src/main/scala/kafka/server/AbstractFetcherThread.scala:174`：`maybeTruncate`
- `core/src/main/scala/kafka/server/AbstractFetcherThread.scala:211`：`truncateToEpochEndOffsets`
- `core/src/main/scala/kafka/server/AbstractFetcherThread.scala:605`：`getOffsetTruncationState`
- `core/src/main/scala/kafka/server/AbstractFetcherThread.scala:318`：`processFetchRequest`

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦 broker 本地 Partition/ISR 状态机、epoch 截断与 follower 复制；不展开 controller 选主、事务、日志清理。
- 目标正文：8000~12000 字；核心拆解层覆盖 leader/follower 切换、epoch 截断四种路径、ISR 扩缩、HW/minISR。

## 10. 本轮重写主线

1. 从“follower 为什么不能直接 fetch”开场，引出 broker 本地状态机的必要性。
2. 否定“直接从本地 LEO 继续追”和“只按 HW 截断”两种朴素方案。
3. 解释 `ReplicaManager.becomeLeaderOrFollower` → `Partition.makeLeader/makeFollower`。
4. 解释 `AbstractFetcherThread` 的 `maybeTruncate` → `maybeFetch` 两阶段。
5. 解释 epoch 截断规则与 `OffsetsForLeaderEpoch` 的作用。
6. 解释 `maybeExpandIsr/maybeShrinkIsr`、pending ISR 状态与 AlterPartition 确认。
7. 收网：Controller 决定归属，Partition 状态机保证这个归属在 broker 本地安全落地。