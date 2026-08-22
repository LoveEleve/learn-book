# Kafka-9. Follower 为什么要先截断、再追 leader——Partition 状态机、ISR 扩缩与 Epoch 截断主链

> 场景：Kafka-7 讲的是 Controller 决定“这个分区应该由谁当 leader、ISR 应该长什么样”；但 Controller 的决定只是元数据。真正把这个决定落成 broker 本地运行时状态的，是 `ReplicaManager`、`Partition` 和 `AbstractFetcherThread`。这也是 Kafka-4 里 `FENCED_LEADER_EPOCH`、Kafka-7 里 `leaderEpoch`、HW、ISR 能最终落到磁盘与网络请求上的地方。

## 先把真正的困惑摆出来：follower 为什么不能直接 fetch leader 的新数据？

假设 broker 0 原来是 leader，broker 1 是 follower。后来 broker 0 挂了，broker 1 被提升为新 leader。再过一会 broker 0 恢复，它现在变成 follower。

一个很直觉的想法是：既然 broker 0 现在是 follower，那它直接从自己本地的 LEO 开始，继续向新 leader fetch 新数据不就行了？

这条路看起来省事，实际上危险得很。因为 broker 0 本地日志末尾，可能还有一段**旧 leader 时期写进去、但新 leader 根本没有的脏尾巴**。如果它不先截断，就直接沿着自己的本地 LEO 往后追，结果不是“补齐”，而是“在错误分叉上继续追加”。

```text
旧 leader broker 0 日志：A B C D E X Y
新 leader broker 1 日志：A B C D E F G
  → broker 0 直接从 Y 后面继续 fetch
    → A B C D E X Y G H ...
      → 日志永远分叉
```

所以 follower 复制的第一原则不是“快点追”，而是“**先确认自己和 leader 的共同祖先在哪**”。在 Kafka 里，这个共同祖先不是只靠 offset 判断，而是靠 **leader epoch** 和 `OffsetsForLeaderEpoch` 主链来找。

*关键设计（斜体）：* *Controller 只决定“你现在应该当 leader 还是 follower”；broker 本地要再跑一遍安全落地流程：`ReplicaManager` 先让 `Partition` 切换角色，follower 端的 `AbstractFetcherThread` 先按 leader epoch 找到共同祖先并截断，再进入 fetch；leader 再根据 follower 的真实追赶进度扩缩 ISR，并以 ISR 为依据推进 HW 与满足 `acks=all`。*[模式: 角色切换 + 先截断后追赶 + ISR 闭环]

## 第一层：Controller 的归属决定，要先落成 broker 本地的 leader / follower 状态

broker 收到 controller 下发的 `LeaderAndIsrRequest` 后，并不会简单地把本地某几个字段改掉就结束。`ReplicaManager.becomeLeaderOrFollower()` 会遍历请求里的分区状态，按“这个 broker 是不是请求中的 leader”把分区分成两批：

- 本地要成为 leader 的 → `Partition.makeLeader`
- 本地要成为 follower 的 → `Partition.makeFollower`

而 `Partition` 本身不是一个“只有 ISR 数组”的薄壳。它同时维护：

- `leaderReplicaIdOpt`：当前 leader 是谁；
- `leaderEpoch` / `partitionEpoch`：当前分区归属与元数据版本；
- `partitionState`：`CommittedPartitionState`、`PendingExpandIsr`、`PendingShrinkIsr`；
- `assignmentState`：普通副本集或重分配中的副本集；
- `leaderEpochStartOffsetOpt`：当前 leader epoch 从哪个 offset 开始。

这就是为什么 Kafka 不能把“Controller 决策”直接等同于“broker 状态已经稳定”。LeaderAndIsrRequest 只是一个目标状态，真正要落地为本地日志、fetcher、ISR、HW 等运行时结构，还要经历 broker 端自己的状态机。

## 第二层：`makeLeader` / `makeFollower` 不是对称切换，而是两条完全不同的本地初始化路径

### 变成 leader：建立新的 epoch 起点

`Partition.makeLeader()` 最关键的动作，是在 leader epoch 变化时记录新的 `leaderEpochStartOffset`：

```text
成为新 leader
  → 以当前 logEndOffset 作为 leaderEpochStartOffset
    → assignEpochStartOffset(leaderEpoch, startOffset)
      → 重置远端副本状态
        → 后续 follower 用它来找共同祖先
```

为什么要记这个起点？因为短时间内连续换 leader 时，某个 follower 的日志里可能含有“比现任 leader 更高 epoch”的尾部。新 leader 必须明确告诉世界：**我这个 epoch 是从哪个 offset 开始写的。** 否则 follower 在对齐 epoch 时根本不知道应该截断到哪。

### 变成 follower：先清本地 leader 视角

`Partition.makeFollower()` 的重点则完全不同：它先更新 `leaderReplicaIdOpt`、`leaderEpoch`、`partitionEpoch`，然后**把 ISR 清空**，再把本地角色切成 follower。

这里清 ISR 不是随便的“重置”，而是为了避免切换过程中出现错误指标和错误语义。注释写得很直接：leader 要先更新，再清 ISR，否则切换瞬间可能误报 under-min-isr 等状态。

所以 leader/follower 切换虽然都由同一个 LeaderAndIsrRequest 驱动，但 broker 本地做的是两套完全不同的准备工作：leader 在建立新 epoch 的起点，follower 在丢掉旧 leader 视角并准备从别人那里追日志。

如果把这两条路径都简化成“改 leaderId + 改 ISR”，主链会先在哪失败？leader 端没有 epoch 起点，follower 端没有干净切换，后面的截断、ISR 扩缩、HW 推进都会失去依据。

## 第三层：为什么 follower 必须先截断——`maybeTruncate` 先于 `maybeFetch`

`AbstractFetcherThread.doWork()` 的顺序非常关键：

```text
doWork()
  → maybeTruncate()
    → maybeFetch()
```

也就是说，follower 复制不是“直接 fetch”，而是先看自己是否处在 **Truncating** 阶段。如果是，就先做截断，只有截断完成后才进入真正的数据抓取。

`maybeTruncate()` 又分成两条路：

- **有 leader epoch 可用** → `truncateToEpochEndOffsets`
- **没有 leader epoch** → `truncateToHighWatermark`

这就是本篇最重要的一条认知分水岭：**epoch 截断是主路径，按 HW 截断只是兼容/退化路径。**

为什么不能反过来先 fetch 再决定要不要截断？因为一旦你在错误的日志分叉上追加了新数据，后面就不是“追赶”而是“混账”。Kafka 把截断放在 fetch 前，正是在制度上杜绝这种情况。

## 第四层：`OffsetsForLeaderEpoch` 真正回答的是“共同祖先在哪”

`truncateToEpochEndOffsets()` 的主线是：

1. 对 Truncating 分区，拿本地已知的 `latestEpoch`；
2. 向 leader 发送 `OffsetsForLeaderEpochRequest`；
3. leader 返回“这个 epoch 在我这里对应的 end offset”；
4. follower 用它和自己本地的 epoch/offset 历史一起算出真正该截断到哪。

核心并不是“leader 给个 offset，follower 照着 truncate”，而是 `getOffsetTruncationState()` 里那套**按 epoch 找共同祖先**的推理。

### 路径一：leader 返回 `UNDEFINED_EPOCH_OFFSET`

说明当前无法基于 epoch 给出有效 end offset。这里**不是回退到 high watermark 路径**，而是保留当前 fetch offset，构造一个“截断已完成”的状态继续往下走。真正按本地 HW 截断的是上一层说的“本地根本拿不到 epoch”的那条独立兼容路径。

### 路径二：leader 返回了 offset，但 leader epoch 是 `UNDEFINED_EPOCH`

这说明 leader 或 follower 还在旧协议兼容路径里，epoch 语义不完整。此时 Kafka 只能保守地按 `min(leaderOffset, logEndOffset)` 截断。

### 路径三：follower 本地能找到同一个 epoch 的 end offset

这是最理想情况：

- 如果 follower 找到的 epoch 与 leader 返回的 epoch 一致，说明双方已经在共同祖先上对齐，截断到 `min(followerEndOffset, leaderEndOffset)` 即可；
- 如果 follower 本地查到的是**更老 epoch**，说明共同祖先比 leader 返回的 epoch 更早，此时 Kafka 会直接按这个更老 epoch 在 follower 本地对应的 `followerEndOffset` 截断，而不是模糊地“继续回退”。

### 路径四：本地根本找不到这个 epoch

说明 follower 的本地历史更乱，连对应 epoch 都查不到了。这时 Kafka 才退化为更保守的 offset 路径，用 `min(leaderEndOffset, replicaEndOffset)` 做截断。

```text
leader epoch 请求
  → leader 返回 (epoch, endOffset)
    → follower 本地查 endOffsetForEpoch(epoch)
      → 找到共同 epoch？
        → 是：truncate 到共同祖先
        → 否：保守退化
```

这四类路径的本质，是用 epoch 历史去定位“我们最后一次还一致时停在哪”。offset 只是最后的截断位置，epoch 才是找祖先的索引。再强调一次：`truncateToHighWatermark()` 只发生在 follower **本地根本拿不到 epoch 信息** 的兼容路径；而 `UNDEFINED_EPOCH_OFFSET` 是 leader 已响应了 epoch 查询、但无法给出有效 epoch end offset 的另一条退化分支，二者不是一回事。

如果只按 leader LEO 截断，主链会先在哪失败？旧 leader 时代残留的尾部里，offset 可能“更大”，但那恰恰是脏数据。你要删的是“错误分叉”，不是“落后多少”。所以 follower 截断的核心问题从来不是“我比 leader 少多少”，而是“我和 leader 从哪里开始分叉”。

## 第五层：fetch 响应里也可能带着“你先去截断”的命令

即便 follower 已经进入 `maybeFetch()`，也不代表后面就一路 append 到底。`processFetchRequest()` 里还有一个很关键的分支：如果 leader 在 fetch 响应里带回 `divergingEpoch`，说明 leader 发现你当前 fetch offset 对应的日志历史已经分叉，必须先回去截断。

这时 Kafka 不会立刻处理 partitionData、也不会先调用 `processPartitionData` 去更新分区状态，而是把 `divergingEndOffsets` 收集起来，后续再次走截断逻辑。原因很简单：**日志还没回到共同祖先前，任何新的 HW / LEO 推进都不可靠。**

所以 follower 复制并不是一次性的“先截断一次就永远安全”，而是整个 fetch 生命周期里始终携带 epoch 校验。一旦发现分叉，就回退到截断主线。

## 第六层：ISR 不是静态数组，而是 committed + pending 的过渡状态机

很多人把 ISR 理解成 controller 发下来的一组 brokerId，但 `PartitionState` 明确告诉你：broker 本地眼里，ISR 不只是一个集合，而是一个**状态机**。

- `CommittedPartitionState`：当前已提交的 ISR；
- `PendingExpandIsr`：准备把某个 follower 加回 ISR，但 controller 还没确认；
- `PendingShrinkIsr`：准备把某些 follower 踢出 ISR，但 controller 还没确认。

为什么要有 pending 状态？因为 leader broker 只能**提出** ISR 变化，真正 committed 的 ISR 还要等 `AlterPartition` 往 controller 提交、controller 确认后才算数。在确认前，Kafka 既不能把它完全当旧 ISR，也不能当新 ISR 已正式提交，所以需要一个中间态。

更微妙的是 `maximalIsr`：在 `PendingExpandIsr` 时，它会把“将要扩进来的副本”先算进去，用来驱动 leader **本地过渡期** 的 HW / produce 判定，但 committed ISR 仍保持旧值。也就是说，Kafka 允许 broker 本地先按“更大的有效 ISR”推进部分复制语义，但这绝不等于对外宣布 ISR 已正式扩张；最终权威仍要等 controller 确认。

如果把三个层次压成一句话：

- **committed ISR**：controller 已确认、对外正式成立的 ISR；
- **maximal/effective ISR**：broker 本地在 pending expand 阶段临时采用的更大集合，用于过渡期的 HW / produce 语义；
- **pending ISR state**：连接两者的中间态，保证“本地复制逻辑继续前进”和“最终权威仍以 controller 为准”可以同时成立。

## 第七层：leader 根据 follower 真实追赶进度扩张 ISR——`maybeExpandIsr`

`updateFollowerFetchState()` 每次更新 follower 的 fetch 状态后，都会调用 `maybeExpandIsr(replica)`。这说明 ISR 扩张不是定时批处理，而是**跟着 follower 的复制进度实时判断**。

一个 follower 要想被重新拉回 ISR，至少要满足两个条件：

1. 它的 `followerEndOffset >= leaderLog.highWatermark`；
2. 它已经追到了当前 `leaderEpochStartOffset` 之后。

第二个条件非常关键。仅仅追到 HW 还不够，因为它可能只追上了旧 epoch 的公共部分，却没有进入当前 leader epoch 的合法区间。Kafka 明确要求：**你不仅要看起来“不落后”，还得证明自己已经站在当前 leader epoch 的分支上。**

满足条件后，leader 会构造 `PendingExpandIsr`，并通过 `submitAlterPartition()` 把这个扩张提案提交给 controller。controller 确认后，本地状态才会转成新的 `CommittedPartitionState`。

如果 follower 一追上 HW 就自动算进 ISR，主链会先在哪失败？它可能只是在旧 epoch 的公共部分追平了，却还没真正接上当前 leader 的分支。把它太早拉回 ISR，会污染 HW 和 `acks=all` 的语义。

## 第八层：leader 也会主动缩 ISR——`maybeShrinkIsr`

ISR 扩张讲的是“谁配回来”，ISR 收缩讲的是“谁该出去”。

`maybeShrinkIsr()` 会先检查当前有没有 inflight ISR 变更；如果没有，再计算 `getOutOfSyncReplicas(replicaLagTimeMaxMs)`。Kafka 判断 out-of-sync 副本的逻辑有两类：

- **stuck follower**：LEO 很久没变；
- **slow follower**：在 `maxLagMs` 时间窗口内没追上 leader LEO。

这两个判断最终都归结为 `lastCaughtUpTimeMs`：只要某个 follower 太久没有“真正追平 leader”，它就该从 ISR 里出去。

leader 发现这些副本后，不是立刻本地生效，而是构造 `PendingShrinkIsr`，记录 `outOfSyncReplicaIds`，然后通过 `submitAlterPartition()` 把 shrink 请求发给 controller。

```text
leader 观察 follower 进度
  → getOutOfSyncReplicas
    → PendingShrinkIsr
      → AlterPartition 提交 controller
        → committed ISR 更新
```

Kafka 之所以把 shrink 设计成“没有 inflight ISR 更新时才做”，是为了避免 leader 在 controller 还没确认上一次 ISR 变化时，又叠加新的本地判断，导致 committed 与 local state 失控。

## 第九层：HW、minISR、`acks=all` 都建在 ISR 语义上

为什么 ISR 状态机这么敏感？因为它直接决定三件大事：

1. **HW 怎么推进**：leader 只有在 ISR 副本都复制到某个位置后，才能把 HW 推到那里。
2. **`acks=all` 什么时候满足**：生产请求只有在 ISR 副本都确认后才算真正完成。
3. **`min.insync.replicas` 是否满足**：如果 ISR 太小，`requiredAcks = -1` 的写入会直接抛 `NotEnoughReplicasException`。

`appendRecordsToLeader()` 里就有这个判断：如果当前 ISR 大小小于 `effectiveMinIsr`，而客户端要求 `acks=all`，leader 直接拒绝写入。

所以 ISR 从来不只是“副本同步情况的一个观察指标”，它是 Kafka 写可用性与一致性边界的硬门槛。ISR 进错一个副本、少退一个副本，后面影响的是 HW、`acks=all` 和 `minISR` 三个核心语义。

## 收网：Controller 决定归属，Partition 状态机保证这个归属在 broker 本地安全落地

把整篇压成一句话：Controller 只决定“这个分区现在该由谁当 leader、ISR 应该是什么样”，broker 本地的 `Partition` 状态机再把这个决定安全落地：`ReplicaManager` 驱动 `makeLeader/makeFollower` 切换角色，follower 的 `AbstractFetcherThread` 必须先按 leader epoch 截断到共同祖先再 fetch，leader 再根据 follower 的真实追赶进度构造 pending ISR 扩缩并通过 AlterPartition 让 controller 确认，最终 HW、`acks=all`、minISR 才有可靠含义。

```text
LeaderAndIsr
  → makeLeader / makeFollower
    → follower: maybeTruncate → maybeFetch
      → epoch 找共同祖先 → truncate
    → leader: maybeExpandIsr / maybeShrinkIsr
      → Pending ISR → AlterPartition → controller 确认
        → committed ISR
          → HW / acks=all / minISR
```

到这里，主线只发生了九件事。

第一，Controller 的元数据决定要先落成本地 leader/follower 角色切换。

第二，`makeLeader` 与 `makeFollower` 是两套不同的本地初始化路径。

第三，follower 复制永远是先截断、后 fetch。

第四，`OffsetsForLeaderEpoch` 回答的是“共同祖先在哪”，不是“你比 leader 落后多少”。

第五，fetch 响应本身也可能让 follower 回到截断主线。

第六，ISR 是 committed + pending 的过渡状态机，不是静态数组。

第七，ISR 扩张要求 follower 追到 HW 且进入当前 leader epoch 分支。

第八，ISR 收缩基于真实追赶进度，并且一次只处理一轮 inflight 变更。

第九，HW、`acks=all`、minISR 都建立在 ISR 语义正确的前提上。

**本篇的一句话困惑**：follower 为什么不能直接从本地 LEO 继续追 leader，而要先截断？

**本篇的一句话顿悟**：因为 follower 要先按 leader epoch 找到与新 leader 的共同祖先，把旧 leader 留下的脏尾巴截掉；之后 leader 再用 ISR 状态机决定谁算真正同步、谁可以参与 HW 与 `acks=all`。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“LeaderAndIsr 一到，本地状态就立刻稳定。”** broker 还要经历 `makeLeader/makeFollower`、epoch 切换、fetcher 截断和 ISR 过渡态。
2. **“ISR 就是 controller 发来的静态数组。”** broker 本地还有 `PendingExpandIsr` / `PendingShrinkIsr` 过渡态。
3. **“epoch 截断就是 truncate 到 leader 最新 offset。”** 关键是先按 epoch 找共同祖先。
4. **“HW 就是日志末尾。”** HW 通常小于等于 LEO，它只代表 ISR 已复制边界。
5. **“追上 leader 就自动回到 ISR。”** 还要满足扩张条件并经过 AlterPartition/controller 确认。

### 关键证据清单

- `core/src/main/scala/kafka/cluster/Partition.scala:197`：`PartitionState` / `PendingExpandIsr` / `PendingShrinkIsr` / `CommittedPartitionState`
- `core/src/main/scala/kafka/cluster/Partition.scala:328`：`leaderIsrUpdateLock`
- `core/src/main/scala/kafka/cluster/Partition.scala:733`：`makeLeader`
- `core/src/main/scala/kafka/cluster/Partition.scala:839`：`makeFollower`
- `core/src/main/scala/kafka/cluster/Partition.scala:1018`：`maybeExpandIsr`
- `core/src/main/scala/kafka/cluster/Partition.scala:1231`：`maybeShrinkIsr`
- `core/src/main/scala/kafka/cluster/Partition.scala:1361`：`appendRecordsToLeader` 与 minISR / `acks=all`
- `core/src/main/scala/kafka/server/ReplicaManager.scala:1992`：`becomeLeaderOrFollower`
- `core/src/main/scala/kafka/server/AbstractFetcherThread.scala:174`：`maybeTruncate`
- `core/src/main/scala/kafka/server/AbstractFetcherThread.scala:211`：`truncateToEpochEndOffsets`
- `core/src/main/scala/kafka/server/AbstractFetcherThread.scala:605`：`getOffsetTruncationState`
- `core/src/main/scala/kafka/server/AbstractFetcherThread.scala:318`：`processFetchRequest`

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦 broker 本地 Partition/ISR 状态机、epoch 截断与 follower 复制；不回头展开 controller 选主、事务、日志清理。
- 本篇把 HW、ISR、leaderEpoch 放在 broker 本地运行时视角解释，不展开客户端可见 API 语义的全部细节。
- 不把 controller 的 committed ISR 与 broker 本地 pending ISR 混成一个层次。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-7`（Controller 决定 leader/ISR 归属）、`Kafka-8`（KRaft 保证谁能成为唯一 controller）。
- 后续桥接：下一篇可进入 `Kafka-10`，把 Producer/Consumer 两边都会碰到的 Purgatory（DelayedProduce/DelayedFetch）作为“等待条件满足”的统一机制继续展开。