# Kafka-38. 一条消息的可靠之旅——acks=all、maximalIsr、Purgatory、LSO 与事务 marker 的共同边界

> 这篇不是新专题，而是把 Kafka 里和“可靠性”相关的几个关键概念真正串成一条链：producer 为什么不是收到 append 成功就万事大吉；`acks=all` 等的到底是不是“静态 ISR”；幂等为什么能防重试重复；事务 marker 与 `read_committed` 又为什么会影响“消费者何时真正看见这条消息”。

## 先把真正的困惑摆出来：Kafka 的“可靠”到底卡在哪几道门上

很多人把 Kafka 的可靠性理解成一句话：

> `acks=all` 就可靠了。

这句话只说对了一层，而且还不够精确。Kafka 的可靠性至少叠了四道门：

1. append 之前，幂等校验先防重复与乱序；
2. append 之后，`acks=all` 还要等 enough-replicas 判定成立；
3. 如果是事务消息，还要等事务状态推进与 marker 落盘；
4. 对 `read_committed` 消费者来说，还要等 LSO 与事务索引把可见性边界收敛出来。

*关键设计（斜体）：* *Kafka 不把“可靠”压缩成单个确认点，而是沿着 produce → replica condition → transaction completion → consumer visibility 这条链逐层收紧边界：`ProducerAppendInfo` 负责写入前防重，`DelayedProduce` + `checkEnoughReplicasReachOffset` 负责副本确认，`TransactionMarkerChannelManager` 与 `ProducerStateManager.completeTxn` 负责事务完成边界，`read_committed` 则通过 LSO 与 abort 索引决定消费者真正可见的数据。*[模式: 写入前防重 + 写入后副本确认 + 事务完成 + 可见性收敛]

## 第一层：`acks=all` 等的不是“一个抽象成功”，而是 enough-replicas 判定

produce 请求写入 leader 本地日志后，如果是 `acks=all`，不会立刻返回，而是进入 `DelayedProduce` 等待。

真正的等待点是：

```text
DelayedProduce.tryComplete()
  → partition.checkEnoughReplicasReachOffset(requiredOffset)
```

这说明 `acks=all` 不是一句口号，而是落到 `checkEnoughReplicasReachOffset` 这套判定逻辑上。

## 第二层：这里看的关键视图是 `maximalIsr`，不是“静态 ISR”

如果把 `acks=all` 解释成“等待 controller 已确认的 ISR 全部追上”，就会把语义讲迟钝。

在 leader 本地 enough-replicas 判定里，关键视图是 `partitionState.maximalIsr`。这意味着：leader 在某些 ISR 变化尚未完全提交到 controller 之前，也会基于本地更及时的有效 ISR 视图做判定。

所以更准确的说法不是“`acks=all` 等静态 ISR”，而是：**它等 leader 侧 enough-replicas 条件成立，而这套条件的核心成员视图是 `maximalIsr`。**

## 第三层：幂等把“重复成功”挡在 append 之前

只有副本确认还不够，因为 producer 可能重试、乱序、甚至带着旧 epoch 再来写。

这时 broker 的 `ProducerAppendInfo` 会在 append 前校验：

- 旧 epoch → `InvalidProducerEpochException`
- sequence 乱序 / 重复异常 → `OutOfOrderSequenceException`

这层的价值是：**可靠性不只是“写进去别丢”，还包括“重试别多写、乱序别混进来”。**

## 第四层：事务把“broker 已写入”与“消费者可见”拆成两步

如果 producer 开启事务，消息即使已经通过幂等校验、也已经满足 enough-replicas，并不等于 `read_committed` 消费者立刻可见。

事务层至少还要做三件事：

1. coordinator 决定事务最终是 COMMIT 还是 ABORT；
2. `TransactionMarkerChannelManager` 把 marker 扇出到相关业务分区；
3. `ProducerStateManager.completeTxn` 把事务从 `ongoingTxns` 移到 `unreplicatedTxns`，把“已决定、但复制尚未完全稳定”的事务单独管理起来。

这里最关键的不是“挪了个集合”，而是：**它在为 LSO 和最终可见性边界服务。**

## 第五层：`read_committed` 真正看的不是 HW，而是 LSO + 事务索引

对 `read_committed` 消费者来说，“这条消息能不能读到”不是简单看 leader 有没写进去，也不是只看 HW。

真正的边界是：

- `LastStableOffset`（LSO）限制当前最多能读到哪；
- abort 事务还要通过事务索引进一步过滤。

所以 `read_committed` 的可见性不是“等 COMMIT marker 到了就行”，而是：**LSO 先截断可读上界，再结合事务索引过滤 abort 数据。**

## 收网：Kafka 的可靠性是四层边界叠起来的

把整篇压成一句话：Kafka 的可靠性不是单靠 `acks=all`，而是 append 前由 `ProducerAppendInfo` 用 epoch/sequence 防重防乱序，append 后由 `DelayedProduce` 调 `checkEnoughReplicasReachOffset` 等待 enough replicas（其关键成员视图是 `maximalIsr`），事务消息再由 marker 扇出与 `ProducerStateManager.completeTxn` 推进事务完成边界，最终 `read_committed` 消费者通过 LSO 与事务索引决定何时真正可见。

```text
producer.send()
  → ProducerAppendInfo 校验 epoch / sequence
    → leader append
      → acks=all ? DelayedProduce 等 enough replicas
        → 事务性消息 ? marker 扇出 + completeTxn
          → read_committed 通过 LSO + txn index 判定可见性
```

**本篇的一句话困惑**：一条 Kafka 消息到底要经过哪些边界，才能从“发送成功”变成“消费者真正可靠可见”？

**本篇的一句话顿悟**：Kafka 的可靠性沿着“写入前防重 → 写入后副本确认 → 事务完成 → 消费者可见性”四层边界逐步收紧；`acks=all` 只是其中一层，而且它真正等待的是基于 `maximalIsr` 的 enough-replicas 判定。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“可靠性就是 `acks=all`。”** 还叠着幂等、防乱序、事务完成、LSO 可见性等多层边界。
2. **“`acks=all` 等的是静态 ISR。”** leader 侧关键视图是 `maximalIsr`。
3. **“幂等和事务是一回事。”** 幂等解决单 producer 写入重复/乱序，事务解决跨分区原子完成与可见性。
4. **“`completeTxn` 只是换个集合位置。”** 它服务于事务完成边界和 LSO 推进。
5. **“`read_committed` 只看 COMMIT marker。”** 还要看 LSO 与 abort 事务索引过滤。

### 关键证据清单

- `core/src/main/scala/kafka/server/DelayedProduce.scala:89`：`DelayedProduce.tryComplete()`。
- `core/src/main/scala/kafka/cluster/Partition.scala:1089`：`checkEnoughReplicasReachOffset(...)`。
- `core/src/main/scala/kafka/cluster/Partition.scala:1093`：读取 `partitionState.maximalIsr`。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerAppendInfo.java:107`：`InvalidProducerEpochException`。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerAppendInfo.java:116`：`OutOfOrderSequenceException`。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionCoordinator.scala:67`：创建 `TransactionMarkerChannelManager`。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:545`：`completeTxn(...)`。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java:646`：`fetchLastStableOffsetMetadata()`。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java:1612`：事务完成状态与 LSO 读取关联。

### 版本与实现边界

- 本文以 Kafka `v4.x` 为基线。
- 本篇是跨域可靠性串联，不重复展开每个子系统内部全部细节。
- 不把 `acks=all`、幂等、事务、`read_committed` 压成同一层语义。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-29`（maximalIsr）、`Kafka-33/34/35`（幂等/事务）、`Kafka-10/36`（Purgatory）。
- 后续桥接：可继续写“消息丢失排查”，把这几层边界变成故障定位路径。