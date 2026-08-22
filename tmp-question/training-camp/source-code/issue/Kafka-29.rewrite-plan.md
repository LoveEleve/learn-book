# Kafka-29 重写规划

> 题目：ISR 为什么不是一个静态数组——leaderIsrUpdateLock、maximalIsr 与 AlterPartition 边界
> 状态：K-9 ISR 域第 3 篇，按"leaderIsrUpdateLock 与 ISR 扩缩边界"展开
> 目标：解释 ISR 在 broker 本地为何不是简单的 `Set[Int]`，以及 `leaderIsrUpdateLock`、`PendingExpandIsr` / `PendingShrinkIsr`、`maximalIsr`、`AlterPartition` 的异步确认如何共同保证“本地先变 / controller 后确认”的一致性主线。覆盖 `maybeExpandIsr`、`maybeShrinkIsr`、`prepareIsrExpand`、`prepareIsrShrink`、`submitAlterPartition`、`handleAlterPartitionError` / `handleAlterPartitionUpdate` 语义。

## 1. 读者困惑

- ISR 为什么不是简单的 `Set[Int]`？
- leader 说“这个 follower 追平了”，为什么不能直接把它塞进 ISR？
- `PendingExpandIsr` / `PendingShrinkIsr` 在等什么？
- `maximalIsr` 和 committed ISR 是什么关系？
- `leaderIsrUpdateLock` 为什么既有读锁又有写锁？
- AlterPartition 被拒绝或返回 `NEW_LEADER_ELECTED` 时，本地状态怎么回滚？

## 2. 一句话顿悟

**Kafka 的 ISR 不是一个静态集合，而是“本地提议 + controller 确认”的双阶段状态机：leader 根据 follower 进度决定要不要扩缩 ISR，但不会直接改 committed ISR，而是先进入 `PendingExpandIsr` / `PendingShrinkIsr`，通过 `AlterPartition` 把提议发给 controller；在收到确认前，本地用 `maximalIsr` 暂时推进 HW/acks 语义，真正 committed 的 ISR 仍保持旧值。**

## 3. 五要素卡片

### 读者问题

一个 follower 刚追平 leader，就算已经属于 ISR 吗？如果 controller 拒绝这个变更，本地状态怎么办？

### 入口

- `leaderIsrUpdateLock`：保护 leader/isr 相关状态
- `maybeExpandIsr` / `maybeShrinkIsr`
- `PendingExpandIsr` / `PendingShrinkIsr` / `CommittedPartitionState`
- `maximalIsr`
- `submitAlterPartition`
- `handleAlterPartitionUpdate` / `handleAlterPartitionError`
- `maybeIncrementLeaderHW`

### 状态核心

- committed ISR：controller 已确认的 ISR
- pending ISR：本地提议但尚未确认的变更
- `maximalIsr`：本地用于 HW/produce 的“有效 ISR”上界
- `leaderEpochStartOffset`：当前 leader epoch 起点
- `isInflight`：是否有未确认的 AlterPartition

### 失败路径

- follower 追平后直接改 committed ISR → controller 拒绝时状态漂移
- shrink 先改 committed ISR 再等确认 → HW 可能被错误推进
- 无 `leaderIsrUpdateLock` → fetch、append、AlterPartition 回调竞争修改 ISR
- 非 retriable 错误后仍重试 AlterPartition → 旧提议反复发送，状态失真

### 连接点

- 前文 `Kafka-9`：Follower 截断与 fetch 先对齐日志。
- 前文 `Kafka-7/24/25`：Controller 侧 `PartitionRegistration` / `PartitionChangeBuilder` 最终确认 ISR 变更。
- 前文 `Kafka-10`：acks=all 会在 DelayedProduce 上等待足够的 ISR 达到 requiredOffset。

## 4. 总图

```text
leader 观察 follower 进度
  → maybeExpandIsr / maybeShrinkIsr
    → 进入 PendingExpandIsr / PendingShrinkIsr
      → submitAlterPartition(sentLeaderAndIsr)
        → controller 确认 / 拒绝
          → handleAlterPartitionUpdate / Error
            → committed ISR 更新 或 回滚到 lastCommittedState

在确认前
  → maximalIsr 参与 HW / produce 判定
    → committed ISR 仍保持旧值
```

## 5. 关键边界

- 本篇不重复 follower epoch 截断（Kafka-28）或 controller 侧状态机（Kafka-24/25）。
- 不把 `maximalIsr` 写成 committed ISR：它是本地过渡期的有效上界。
- 不把 `PendingExpandIsr` 与 `PendingShrinkIsr` 混成一个逻辑：expand 可以乐观，shrink 必须保守。
- 不把 AlterPartition 回调写成同步：它是异步返回，期间 state 可能已被别的 metadata 覆盖。

## 6. 失败方案推演

1. **follower 追平就直接改 committed ISR**：controller 失败返回后，本地/全局状态漂移。
2. **shrink 也乐观地推进 maximalIsr**：HW 被错误推进，acks=all 误判成功。
3. **没有 inflight 标志**：扩/缩两个请求并发叠加，顺序错乱。
4. **AlterPartition 失败一律重试**：面对 `INVALID_UPDATE_VERSION` 等不可重试错误会无限漂移。

## 7. 误解清单

- “ISR 就是一个 Set[Int]。”：Kafka 实际维护 committed + pending + maximal 三层语义。
- “follower 追上了就已经算 ISR。”：还要经过 controller 确认。
- “expand / shrink 的处理完全对称。”：expand 可以乐观，shrink 必须保守。
- “leaderIsrUpdateLock 只是普通互斥锁。”：读写锁用于区分 hot path 读和偶发状态更新。
- “AlterPartition 被拒绝就重新发就行。”：部分错误必须回滚到 lastCommittedState。

## 8. 证据清单

- `core/src/main/scala/kafka/cluster/Partition.scala:197`：PartitionState / PendingExpandIsr / PendingShrinkIsr / CommittedPartitionState。
- `core/src/main/scala/kafka/cluster/Partition.scala:328`：leaderIsrUpdateLock。
- `core/src/main/scala/kafka/cluster/Partition.scala:1018`：maybeExpandIsr。
- `core/src/main/scala/kafka/cluster/Partition.scala:1152`：maybeIncrementLeaderHW。
- `core/src/main/scala/kafka/cluster/Partition.scala:1231`：maybeShrinkIsr。
- `core/src/main/scala/kafka/cluster/Partition.scala:1747`：prepareIsrExpand。
- `core/src/main/scala/kafka/cluster/Partition.scala:1774`：prepareIsrShrink。
- `core/src/main/scala/kafka/cluster/Partition.scala:1825`：submitAlterPartition。
- `core/src/main/scala/kafka/cluster/Partition.scala:1871`：handleAlterPartitionError。
- `core/src/main/scala/kafka/cluster/Partition.scala:1842`：handleAlterPartitionUpdate。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦 ISR 过渡状态与 AlterPartition 异步确认，不展开 controller 侧 PartitionRegistration 字段细节。
- 目标正文：7000~11000 字。

## 10. 本轮重写主线

1. 从"follower 追平后为什么不能直接算进 ISR"开场。
2. 否定 ISR 只是 Set、expand/shrink 对称、失败一律重试三种方案。
3. 解释 leaderIsrUpdateLock 的存在意义。
4. 解释 maybeExpandIsr 与 `PendingExpandIsr` 的乐观路径。
5. 解释 maybeShrinkIsr 与 `PendingShrinkIsr` 的保守路径。
6. 解释 maximalIsr 如何影响 HW 与 acks=all。
7. 解释 submitAlterPartition 的异步确认与错误处理。
8. 收网：ISR 是本地提议 + controller 确认的双阶段状态机。