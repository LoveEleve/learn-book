# Kafka-28. Follower 为什么必须先截断再追 leader——AbstractFetcherThread 的四种 epoch 截断路径

> 场景：Kafka-9 已经讲过 follower 复制的第一原则是“先截断、再追 leader”。但当时只是一带而过：`AbstractFetcherThread.doWork()` 为什么把 `maybeTruncate()` 放在 `maybeFetch()` 前面？`OffsetsForLeaderEpoch` 返回的几种路径到底有什么区别？本篇把 follower 的 epoch 截断主链讲透。这是 K-9 ISR 域第 2 篇。

## 先把真正的困惑摆出来：为什么 follower 不能直接从自己的 LEO 开始追

一个 follower 在 leader 切换后，最危险的状态是：**本地比新 leader “多了一截”**。这截多出来的尾巴并不是优势，而是旧 leader 时代留下的脏数据。如果此时 follower 直接从自己的 LEO 开始继续 fetch，只会把错误分叉继续扩大。

```text
旧 leader 日志：A B C D E X Y
新 leader 日志：A B C D E F G
  → follower 直接从 Y 后面继续追
    → A B C D E X Y H I ...
      → 与新 leader 永远分叉
```

所以 follower 的复制主链不是“先 fetch 看看”，而是：**先确定与新 leader 的共同祖先在哪，再决定要删多少本地数据，最后才继续 fetch。**

*关键设计（斜体）：* *AbstractFetcherThread 每次循环先 `maybeTruncate()`，用 `OffsetsForLeaderEpoch` 让 leader 回答“当前 epoch 对应的 end offset 是多少”，再根据 follower 是否认识这个 epoch、是否能找到共同祖先，计算出一个 `OffsetTruncationState`；只有截断完成后才进入 `maybeFetch()`。*[模式: 先找共同祖先 + 再追赶]

## 第一层：`doWork()` 的顺序就是安全保证——先截断，再 fetch

`AbstractFetcherThread.doWork()` 很短：

```text
doWork()
  → maybeTruncate()
  → maybeFetch()
```

但这两个方法的顺序不能交换。

- `maybeTruncate()` 负责把 follower 的日志先收敛到和 leader 的共同祖先；
- `maybeFetch()` 才负责继续从这个共同祖先之后追 leader 的增量数据。

如果把顺序反过来，一旦 follower 带着旧 leader 的尾巴先 fetch 了新 leader 的新数据，本地日志的末尾就会混进两套不同历史的记录，后续再截断会变得极其麻烦。

所以在 Kafka 里，截断不是异常情况的补丁，而是**正常复制主链的第一步**。

## 第二层：有 leader epoch 时，先走 `truncateToEpochEndOffsets`

`maybeTruncate()` 会先按是否有 leader epoch 把分区分成两组：

- **有 leader epoch**：走 `truncateToEpochEndOffsets()`；
- **没有 leader epoch**：走 `truncateToHighWatermark()`。

有 leader epoch 是正常主路径，因为 KRaft/IBP 新版本下 follower 与 leader 都应该携带 epoch 信息。

`truncateToEpochEndOffsets()` 做三件事：

1. 对需要截断的分区，构造 `OffsetsForLeaderEpochRequest`；
2. 向 leader 发送请求，拿到 `EpochEndOffset` 响应；
3. 对每个分区调用 `getOffsetTruncationState()` 计算真正的截断位置。

这意味着：**leader 不会直接告诉 follower “你该删到这个 offset”**，而是先回答“我在你问的 epoch 上，已知的 end offset 是多少”，真正的截断决定还要结合 follower 本地的 epoch 历史来做。

## 第三层：四种路径——`getOffsetTruncationState` 是本篇真正的核心

`getOffsetTruncationState()` 有四条关键分支：

### 路径一：leader 返回 `UNDEFINED_EPOCH_OFFSET`

这表示 leader 在这个 epoch 上无法给出有效 end offset。这里**不是**回退到 `truncateToHighWatermark()` 那条路径，而是：

- 直接使用当前 fetch offset 作为截断点；
- 标记 `truncationCompleted = true`。

```text
UNDEFINED_EPOCH_OFFSET
  → offset = current fetchOffset
  → truncationCompleted = true
```

这是说：leader 没法用 epoch 告诉你更好的祖先，那你就先别动自己的当前位置，继续走后续流程。

### 路径二：leader 返回了 offset，但 epoch 是 `UNDEFINED_EPOCH`

这说明 leader 或 follower 还在旧协议路径，epoch 信息不完整。此时 Kafka 退化为：

```text
offset = min(leaderEndOffset, followerLogEndOffset)
truncationCompleted = true
```

也就是保守地按 offset 截断，避免盲目前进。

### 路径三：follower 本地能找到同一个 epoch 的 end offset

这是最理想的情况：follower 用 `endOffsetForEpoch()` 查本地历史，找到了这个 epoch 对应的 end offset。再分两种：

- 如果 follower 找到的 epoch 与 leader 返回的一致，说明双方在这个 epoch 上达成了共同祖先，直接截到 `min(followerEndOffset, leaderEndOffset)`；
- 如果 follower 本地查到的是**更老的 epoch**，说明 follower 没有 leader 返回的那个 epoch，就先截到这个“更老已知 epoch”的 end offset，然后再发下一轮 `OffsetsForLeaderEpoch` 请求继续回退。

这一步非常关键：**Kafka 不是一跳就退到终点，而是可以分多轮逐步回退，直到双方 epoch 对齐。**

### 路径四：follower 本地根本找不到这个 epoch

这时 follower 的本地 epoch 历史里没有 leader 提供的 epoch，只能退化到：

```text
offset = min(leaderEndOffset, followerLogEndOffset)
truncationCompleted = true
```

也就是更保守地按 offset 截断。

## 第四层：`UNDEFINED_EPOCH_OFFSET` 与 `UNDEFINED_EPOCH` 不是一回事

这两个名字很像，语义完全不同：

- **`UNDEFINED_EPOCH_OFFSET`**：leader 在给定 epoch 上无法返回有效 end offset，Kafka 保持当前 fetchOffset，认为这轮截断已完成；
- **`UNDEFINED_EPOCH`**：协议里根本没有 epoch 概念，说明双方在旧版本兼容路径，只能退化为 offset 截断。

如果把这两个混成一个“都走 HW 截断”的兜底路径，就会看不懂为什么 Kafka 还要保留 `fetchOffset` 作为截断点，也会误解 epoch 与 offset 回退的边界。

## 第五层：`FENCED_LEADER_EPOCH`——旧 leader 时代的 fetcher 必须停下来

`maybeTruncateToEpochEndOffsets()` 处理 leader 的响应时，如果错误码是 `FENCED_LEADER_EPOCH`，会调用 `onPartitionFenced()`：

- 如果请求里的 leader epoch 等于当前 fetchState 的 leader epoch，说明这个 fetcher 线程带着**旧 epoch** 请求新 leader，分区必须标记失败，等待新的 LeaderAndIsrState 才能恢复；
- 否则说明 fetcher 线程里的 epoch 比 leader 新，可能只是需要稍后重试。

这一步的意义是：**旧 leader 时代的 fetcher 线程不能继续按旧 epoch 追数据。** 一旦被 fenced，它必须停下来，等新的 controller 下发新 leader 状态后再恢复。

## 第六层：正常 fetch 里也可能要求“先回去截断”——divergingEpoch

即使 maybeTruncate 已经跑过，正常 `maybeFetch()` 的响应里也可能发现分叉。`processFetchRequest()` 处理每个分区响应时，如果 `FetchResponse.isDivergingEpoch(partitionData)` 为 true：

- 并不立刻把数据交给 `processPartitionData()`；
- 而是把 `divergingEndOffsets` 记下来；
- 后续再次走截断路径。

这说明：**epoch 对齐不是“进程启动时做一次就永远结束”，而是整个 follower fetch 生命周期里持续生效的安全阀。**

只要 leader 在 fetch 响应里发现你当前 offset 已经分叉，就要求你先退回共同祖先，再继续追。

## 第七层：out-of-range 与 tiered storage 路径也是 epoch 校验的延伸

`handleOutOfRangeError()` 和 `handleOffsetsMovedToTieredStorage()` 看似是另外两类异常，实际上也都围绕“当前 offset 还能不能安全继续用”展开：

- offset out-of-range 时，调用 `fetchOffsetAndTruncate()`，可能直接按 leader 最新 offset 截断；
- tiered storage 场景下，可能要通过 `fetchTierStateMachine` 重新定位位置；
- 两者都在 `FencedLeaderEpochException` 时回到 `onPartitionFenced()`。

这说明 follower 复制的所有边界——epoch 分叉、offset 越界、远端 tiered storage——最终都汇合到同一个设计原则：**当前位置不可信时，先重新定位或截断，再继续 fetch。**

## 收网：Follower 复制不是“尽快追”，而是“先对齐，再追”

把整篇压成一句话：Follower 的复制主链是 `maybeTruncate()` 先于 `maybeFetch()`；有 leader epoch 时，`truncateToEpochEndOffsets()` 用 `OffsetsForLeaderEpoch` 让 leader 提供 epoch 对应的 end offset，再由 `getOffsetTruncationState()` 走四种分支（未知 offset、未知 epoch、找到共同 epoch、完全不认识）；收到 `FENCED_LEADER_EPOCH` 或 `divergingEpoch` 时都要停下当前 fetch，回到截断主线。只有日志先对齐到共同祖先，后续 fetch / append 才安全。

```text
maybeTruncate
  → truncateToEpochEndOffsets
    → leader.fetchEpochEndOffsets
      → getOffsetTruncationState（4 路分支）
        → truncate 完成？
          → maybeFetch
            → divergingEpoch / fenced → 回到截断主线
```

到这里，主线只发生了六件事。

第一，`maybeTruncate` 必须先于 `maybeFetch`。

第二，正常主路径是 `OffsetsForLeaderEpoch`，不是直接按 HW 或 LEO 回退。

第三，`UNDEFINED_EPOCH_OFFSET` 与 `UNDEFINED_EPOCH` 的退化路径不同。

第四，follower 认识不到 leader 的 epoch 时，会分多轮逐步回退到共同祖先。

第五，`FENCED_LEADER_EPOCH` 意味着当前 fetcher 的 epoch 已失效，必须等新状态。

第六，`divergingEpoch` 说明正常 fetch 过程中也可能发现分叉，需要重新截断。

**本篇的一句话困惑**：为什么 follower 不能直接从自己的 LEO 开始继续追 leader？

**本篇的一句话顿悟**：因为 follower 的第一任务不是“追上 leader”，而是“找回与 leader 的共同祖先”；只有 epoch 先对齐，后续 fetch 才有意义。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“maybeFetch 才是核心，截断只是异常情况。”** 截断是正常复制主链的第一步。
2. **“UNDEFINED_EPOCH_OFFSET 和 UNDEFINED_EPOCH 一样。”** 一个是 leader 无法提供 end offset，一个是协议没有 epoch 概念。
3. **“收到 divergingEpoch 就是 fetch 失败。”** 它是正常 fetch 响应的一种分支，要求回到截断主线。
4. **“follower 认识不到 leader 的 epoch 就直接失败。”** 先回退到更老共同祖先，再继续请求。
5. **“truncateToHighWatermark 是通用兜底。”** 只有本地根本拿不到 epoch 时才走。 

### 关键证据清单

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

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦 follower epoch 截断主链，不展开 broker 侧 partition 状态机或 tiered storage 内部实现。
- 不把 `UNDEFINED_EPOCH_OFFSET` 与 `truncateToHighWatermark` 混成一条路径。
- 不把 divergingEpoch 误写成 error code，它是 fetch 响应中的正常分支。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-9`（Partition 状态机）、`Kafka-24/25`（Controller 侧 leaderEpoch / 选举）。
- 后续桥接：下一篇可进入 K-9 ISR 域第 3 篇（leaderIsrUpdateLock 与 ISR 扩缩边界），或切回 K-8 / K-11 子专题。