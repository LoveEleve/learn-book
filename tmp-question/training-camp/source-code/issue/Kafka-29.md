# Kafka-29. ISR 为什么不是一个静态数组——leaderIsrUpdateLock、maximalIsr 与 AlterPartition 边界

> 场景：Kafka-9 讲了 follower 为什么要先截断、再追 leader，Kafka-24/25 讲了 Controller 眼中的 leader/ISR 状态和 broker 状态变化。但有一个关键点一直没正面展开：**leader 观察到 follower 追平或落后后，为什么不能直接把 ISR 改掉？** 本篇把这条本地主态机讲清：leader 侧先提议 ISR 扩/缩，进入 pending state，异步通过 `AlterPartition` 让 controller 确认，期间 `maximalIsr` 参与 HW / acks 的局部判定。这是 K-9 ISR 域第 3 篇。

## 先把真正的困惑摆出来：follower 追平了，为什么还不能直接算进 ISR？

你可能会觉得：ISR 就是一组同步中的副本，leader 明明已经看到某个 follower 追平了，它为什么不能直接把那个 brokerId 加进 ISR？

直觉方案是：

```text
follower LEO 追到 leader HW
  → leader 本地直接 ISR += follower
    → 立刻用新的 ISR 推进 HW / 满足 acks=all
```

这条路在单机思维里成立，但在 Kafka 里危险得很：Controller 才是全局元数据的权威，leader 只是先观察到“这个 follower 看起来追平了”。如果 leader 直接改 committed ISR，而随后 `AlterPartition` 被 controller 拒绝（比如 partition epoch 已过期、leader epoch 旧了），本地和全局状态就漂了。

Kafka 的答案不是“不要改”，而是：**先进入 pending state，乐观或保守地更新本地的有效 ISR 语义，等 controller 确认后再落成 committed ISR。**

*关键设计（斜体）：* *ISR 不是一个静态 `Set[Int]`，而是 committed ISR + pending 扩/缩状态 + maximalIsr 的组合。leader 通过 `maybeExpandIsr` / `maybeShrinkIsr` 观察 follower 进度，构造 `PendingExpandIsr` / `PendingShrinkIsr`，再异步 `submitAlterPartition` 给 controller；在确认前，`maximalIsr` 用于本地 HW / produce 判定，但 committed ISR 仍保持旧值。*[模式: 本地提议 + 异步确认 + 双阶段状态机]

## 第一层：`leaderIsrUpdateLock` 是 ISR 状态机的护栏

`Partition` 里最重要的一把锁之一是 `leaderIsrUpdateLock`。它不是普通互斥锁，而是一个读写锁：

- **读锁**：热路径（例如 follower fetch 到来时的 `updateFollowerFetchState`）可以并发读取 ISR / leader 状态；
- **写锁**：真的需要修改 ISR、leaderEpoch、partitionState 时，独占推进状态。

为什么需要读写锁，而不是一把粗暴的 `synchronized`？因为 ISR 状态和 leader 相关的读操作非常频繁，而写操作（扩 ISR / 缩 ISR / 处理 AlterPartition 回调）相对少。用读写锁可以让“经常读、少量写”的热点路径不至于全阻塞。

如果没有这把锁，下面这些竞争会同时发生：

- follower fetch 更新 `Replica.stateSnapshot`
- leader 发现 follower 追平，准备扩 ISR
- `AlterPartition` 回调回来，准备把 pending 状态落成 committed
- `maybeIncrementLeaderHW` 根据 `maximalIsr` 算 HW

这些步骤若乱序交错，就会导致 ISR 与 HW 的关系瞬间不一致。

## 第二层：扩 ISR 为什么可以“乐观”——`PendingExpandIsr`

`maybeExpandIsr` 的触发条件是：

- 当前没有 inflight ISR 变更；
- follower 还不在 ISR 里；
- follower 已经符合加入 ISR 的资格（未 fenced、未 controlled shutdown、broker epoch 匹配）；
- 最重要的：**follower 的 LEO >= 当前 HW，且已经进入当前 leader epoch 分支**。

满足条件后，leader 不会直接改 committed ISR，而是进入 `prepareIsrExpand()`：

```text
CommittedPartitionState
  → PendingExpandIsr(newInSyncReplicaId, sentLeaderAndIsr, lastCommittedState)
```

注意 `prepareIsrExpand` 的关键一句注释：**当扩 ISR 时，leader 会“假设这个副本会成功进 ISR”，让 `maximalIsr` 先把它算进去。** 这样做的目的是：即使 controller 确认稍微晚一点，leader 也能尽早按更严格的副本集合推进 HW，保证 acks=all 的语义尽量保守。

这就是“乐观”的含义：扩张比 shrink 更安全——把更多副本算进来，只会让 HW 推进更慢，不会让它错误推进得更快。

## 第三层：缩 ISR 为什么必须“保守”——`PendingShrinkIsr`

`maybeShrinkIsr` 的触发条件比扩张更严格。它会：

1. 在读锁下快速判断是否有 out-of-sync replicas；
2. 在写锁下再次确认，拿到 `outOfSyncReplicaIds`；
3. 构造 `PendingShrinkIsr`，但此时 **`maximalIsr` 仍然等于当前 committed ISR**。

这一步的注释非常关键：**shrink 不能像 expand 一样乐观推进 `maximalIsr`，否则如果 controller 最终拒绝了这次 shrink，leader 可能提前用过小的 ISR 推进了 HW，导致 acks=all 被错误满足。**

所以：

- expand：可以先把新副本算进 `maximalIsr`；
- shrink：绝不能先把旧副本踢出 `maximalIsr`。

这就是为什么 `PendingShrinkIsr` 的 `maximalIsr` 仍然是当前 ISR，它必须保守到 controller 确认后再生效。

## 第四层：`maximalIsr` 如何影响 HW 与 acks=all

`maybeIncrementLeaderHW()` 计算新的 high watermark 时，并不是只看 committed ISR。它会看：

- committed ISR 中的副本；
- 以及那些虽然还没被 controller 正式确认，但本地已经“足够追平、可认为马上能进 ISR”的副本（通过 `maximalIsr` 或 `shouldWaitForReplicaToJoinIsr`）。

```text
maybeIncrementLeaderHW
  → 遍历 remote replicas
    → committed ISR 中的副本肯定要等
    → maximalIsr 中的副本也要等
      → 取最小 LEO 作为新 HW
```

这一步直接影响两件事：

1. **HW 推进**：HW 只推到“最慢的有效 ISR 副本”位置。
2. **acks=all**：`checkEnoughReplicasReachOffset(requiredOffset)` 用 `partitionState.maximalIsr` 判断是否有足够副本达到 requiredOffset。

因此 ISR 不只是一个“副本同步名单”，它直接控制 producer 看见的确认语义。把某个副本过早踢出或过早加进来，都会影响 `acks=all`。

## 第五层：AlterPartition 是异步确认，不是同步函数调用

`submitAlterPartition()` 会把 leader 本地的 ISR 提议（`PendingExpandIsr` / `PendingShrinkIsr`）包装成 `LeaderAndIsr`，发给 `AlterPartitionManager`，返回一个 `CompletableFuture<LeaderAndIsr>`。

它的回调有三类结果：

### 1. 成功返回 `leaderAndIsr`

调用 `handleAlterPartitionUpdate()`，把 pending state 转成 committed state。此时 ISR 变更正式生效，如果 HW 推进了，就 `tryCompleteDelayedRequests()`。

### 2. 非重试错误

`handleAlterPartitionError()` 会根据错误码决定：

- `OPERATION_NOT_ATTEMPTED` / `INELIGIBLE_REPLICA`：明确告诉我们 controller 没接受这次提议，可以安全回滚到 `lastCommittedState`；
- `FENCED_LEADER_EPOCH` / `INVALID_UPDATE_VERSION` / `UNKNOWN_TOPIC_OR_PARTITION`：本地状态可能已经过期，保持 pending，等新的 LeaderAndIsr 或 metadata 覆盖；
- `NEW_LEADER_ELECTED`：说明请求其实成功了，但本地这位 leader 可能已被移出副本集合，此时保持 pending，等待新 metadata 来清理。

### 3. 可重试错误

只有真正可重试的情况才递归再次 `submitAlterPartition(proposedIsrState)`。

这一步体现了 Kafka 的谨慎：**不是所有失败都自动重试，也不是所有失败都立即回滚。** 要看 controller 返回的错误码语义。

## 第六层：为何 `PendingExpandIsr` / `PendingShrinkIsr` 必须保留 `lastCommittedState`

无论扩张还是收缩，pending state 都会保存：

- `sentLeaderAndIsr`：本地送给 controller 的提议；
- `lastCommittedState`：发送提议之前最后一次被确认的 ISR 状态。

这个 `lastCommittedState` 是出错时回滚的唯一安全落点。

没有它，一旦 `AlterPartition` 返回 `INELIGIBLE_REPLICA` 或 `OPERATION_NOT_ATTEMPTED`，leader 根本不知道应该回退到哪个 committed ISR。保留 `lastCommittedState` 就是为了让本地状态机在异步失败场景下仍然有“确定性”。

## 收网：ISR 是本地提议 + controller 确认的双阶段状态机

把整篇压成一句话：Kafka 的 ISR 不是一个简单的 `Set[Int]`，而是 committed ISR、pending expand/shrink state 和 `maximalIsr` 三层语义的组合；leader 观察 follower 进度后先进入 `PendingExpandIsr` / `PendingShrinkIsr`，再异步 `AlterPartition` 请求 controller 确认。在确认前，`maximalIsr` 可以保守地影响 HW 和 acks=all，但 committed ISR 仍保持旧值；回调成功才落成 committed，失败则根据错误码回滚或等待新 metadata 覆盖。

```text
follower 进度变化
  → maybeExpandIsr / maybeShrinkIsr
    → PendingExpandIsr / PendingShrinkIsr
      → submitAlterPartition
        → controller 确认/拒绝
          → handleAlterPartitionUpdate / Error
            → committed ISR 更新或回滚
              → HW / acks=all
```

到这里，主线只发生了六件事。

第一，`leaderIsrUpdateLock` 保护了 ISR/leader 状态的热点读与少量写。

第二，ISR 扩张可以乐观把新副本计入 `maximalIsr`。

第三，ISR 收缩必须保守，直到 controller 确认前 `maximalIsr` 仍保持旧值。

第四，`maximalIsr` 直接影响 HW 与 acks=all 的本地判定。

第五，`AlterPartition` 是异步确认路径，不是同步函数调用。

第六，pending state 必须保留 `lastCommittedState` 才能安全回滚。

**本篇的一句话困惑**：follower 追平了，为什么 leader 还不能直接把它塞进 ISR？

**本篇的一句话顿悟**：因为 leader 只能先提出 ISR 变化，真正 committed 的 ISR 还要等 controller 确认；在确认前，Kafka 用 pending state + maximalIsr 让本地复制继续推进，但不冒险提前改变全局事实。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“ISR 就是一个静态 Set[Int]。”** Kafka 实际维护 committed + pending + maximal 三层语义。
2. **“follower 追上了就已经算 ISR。”** 还要经过 controller 确认。
3. **“expand / shrink 处理完全对称。”** expand 可以乐观，shrink 必须保守。
4. **“AlterPartition 被拒绝就重新发就行。”** 有些错误必须回滚或等待 metadata 覆盖。
5. **“HW 只看 committed ISR。”** `maximalIsr` 也参与本地 HW/acks 判定。

### 关键证据清单

- `core/src/main/scala/kafka/cluster/Partition.scala:1005`：maybeExpandIsr 注释。
- `core/src/main/scala/kafka/cluster/Partition.scala:1018`：maybeExpandIsr。
- `core/src/main/scala/kafka/cluster/Partition.scala:1052`：isFollowerInSync 条件。
- `core/src/main/scala/kafka/cluster/Partition.scala:1089`：checkEnoughReplicasReachOffset。
- `core/src/main/scala/kafka/cluster/Partition.scala:1152`：maybeIncrementLeaderHW。
- `core/src/main/scala/kafka/cluster/Partition.scala:1231`：maybeShrinkIsr。
- `core/src/main/scala/kafka/cluster/Partition.scala:1747`：prepareIsrExpand。
- `core/src/main/scala/kafka/cluster/Partition.scala:1774`：prepareIsrShrink。
- `core/src/main/scala/kafka/cluster/Partition.scala:1825`：submitAlterPartition。
- `core/src/main/scala/kafka/cluster/Partition.scala:1871`：handleAlterPartitionError。
- `core/src/main/scala/kafka/cluster/Partition.scala:1842`：handleAlterPartitionUpdate。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦 ISR 过渡状态与 AlterPartition 异步确认，不展开 controller 侧 PartitionRegistration 字段细节。
- 不把 `maximalIsr` 当 committed ISR 使用者可见的全局事实。
- 不展开 follower epoch 截断（Kafka-28 已讲）。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-9`（broker 侧 Partition 状态机）、`Kafka-24/25`（controller 侧分区状态与 broker 状态变化）。
- 后续桥接：K-9 ISR 域 3 篇收官。下一篇可回到 K-8 / K-11 继续扩展，或做全篇一致性收束。