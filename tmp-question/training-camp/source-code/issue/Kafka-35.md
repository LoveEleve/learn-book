# Kafka-35. 一条消息到底什么时候才算“真的提交”——marker、control batch 与 read_committed 可见性

> 场景：Kafka-34 讲了 coordinator 如何推进事务状态机并把状态写入事务日志。但事务日志只是 coordinator 的私有记录，业务分区本身并不知道这笔事务是提交了还是中止了。**本篇讲的就是 coordinator 怎么把事务结果扇出到各业务分区，以及 read_committed 消费者怎么利用扇入后的信息过滤可见性。**

## 先把真正的困惑摆出来：coordinator 决定提交了，consumer 怎么知道

coordinator 内存里的事务状态已经改成 `COMPLETE_COMMIT`，但业务分区各有自己的日志，一个挂在业务分区上的 consumer 并不直接读 coordinator 的状态。

如果 coordinator 不通知业务分区，consumer 读业务分区时，看到的只是一串普通数据 batch，甚至连这些数据属于哪笔事务都不知道。

所以事务的最后一步必须是：**coordinator 把“这笔事务是 COMMIT 还是 ABORT”这个结果，变成每个业务分区都能看见的日志记录。**

```text
coordinator 决定 COMMIT
  → 向各业务分区 leader 发送 WriteTxnMarkersRequest
    → 各分区 leader 写入 ControlBatch（COMMIT marker）
      → 消费者读分区时看见 marker → 知道事务已提交
```

*关键设计（斜体）：* *协调器通过 `TransactionMarkerChannelManager` 异步扇出 marker，业务分区 leader 写入 `ControlBatch` 并更新 `ProducerStateManager.completeTxn`，从事务索引与 LSO 共同决定 `read_committed` 的可见边界。*[模式: coordinator 扇出 + 分区写入 + LSO/txnindex 过滤]

## 第一层：TransactionMarkerChannelManager 异步扇出 marker

`TransactionCoordinator` 在 `EndTransaction` 处理流程中，先把事务状态推进到 `PREPARE_COMMIT` / `PREPARE_ABORT` 并写入事务日志，然后立即可以向客户端返回成功。marker 的实际发送由 `TransactionMarkerChannelManager` 异步完成。

`TransactionMarkerChannelManager` 不是一个简单的“发一次请求”的线程，它维护了按目标 broker 分桶的 marker queue。每个 `TxnMarkerEntry` 包含：

- `producerId`
- `producerEpoch`
- `TransactionResult.COMMIT` 或 `TransactionResult.ABORT`
- 该事务涉及的 partition 列表

```text
EndTransaction(COMMIT)
  → coordinator 写 PREPARE_COMMIT 到事务日志
    → 返回客户端成功
      → 异步：TransactionMarkerChannelManager
        → 按目标 broker 分队列
          → WriteTxnMarkersRequest
            → 各业务分区 leader 写入 marker
```

## 第二层：业务分区 leader 写入 ControlBatch

业务分区 leader 收到 `WriteTxnMarkersRequest` 后，在本地日志中追加一条 `ControlBatch`。它不是普通的数据 batch，而是：

- `isControlBatch = true`
- record 中包含 `EndTransactionMarker`，序列化后指定 `TransactionResult.COMMIT` 或 `TransactionResult.ABORT`

`ProducerAppendInfo.append()` 走 `appendEndTxnMarker()` 分支，不触发 `maybeValidateDataBatch`，因为 marker 不需要校验 sequence。

```text
业务分区 leader 收到 marker
  → append(controlBatch)
    → appendEndTxnMarker
      → 写入日志
        → ProducerStateManager.completeTxn
```

## 第三层：completeTxn 从 ongoingTxns 移到 unreplicatedTxns

`ProducerStateManager.completeTxn()` 是标记一笔事务在“本地”结束的关键点：

```text
completeTxn(completedTxn)
  → ongoingTxns.remove(firstOffset)
  → unreplicatedTxns.put(firstOffset, txnMetadata)
```

但注意：**事务只是从“进行中”变成了“已完成但未复制稳定”。** `unreplicatedTxns` 表示这笔事务的 marker 已经写到本分区，但 marker 的 offset 可能还在 HW 之上。也就是说，follower 还没确认这个 marker。

这就是为什么 `read_committed` 不能只看有没有 COMMIT marker：如果 marker 还没复制到 HW，对消费者来说它还不稳定。

## 第四层：firstUnstableOffset 与 LSO

`ProducerStateManager` 通过 `firstUnstableOffset()` 计算“当前第一个未稳定的事务边界”：

```text
firstUnstableOffset
  = min(ongoingTxns.firstOffset, unreplicatedTxns.firstOffset)
```

`UnifiedLog.lastStableOffset()` 把这个值加工成对消费者可见的边界：

```text
LSO = min(HW, firstUnstableOffset)
```

- 如果 firstUnstableOffset 小于 HW，说明 HW 之前存在未稳定的事务边界，LSO 卡在 firstUnstableOffset；
- 如果 firstUnstableOffset 大于等于 HW（即所有事务都已经稳定，或者根本没事务），LSO = HW。

```text
read_committed 可见边界
  = LSO
    = min(HW, firstUnstableOffset)
```

所以消费者不是读到 HW 就停，而是停到 LSO 为止。LSO 之后的数据，即使物理上已经写进日志，对 `read_committed` 消费者来说也是不可见的——因为它们可能属于未完成的事务。

## 第五层：read_committed 读取流程——先按 LSO 截断，再按事务索引过滤

`read_committed` 消费者的读取流程是：

1. 从 fetch offset 开始读，但不能超过 `LSO`；
2. 对 LSO 之前的数据，还要检查 `AbortedTransaction` 列表；
3. 如果某个段属于已 abort 的事务，需要跳过其中对应的数据。

`LocalLog` 的读取路径会收集 `AbortedTransaction` 列表：

```text
read(log, ...)
  → 收集 abortedTransactions
    → 返回给消费者
      → 消费者跳过这些区间
```

所以 `read_committed` 不是简单的“只看 HW”、“只看 LSO”或“只看 marker”，而是**同时用了 LSO 截断和事务索引过滤两层**。

## 第六层：为什么需要事务索引

事务索引的存在是为了在读取时快速定位 abort 事务的边界。每个 segment 有一个 `.txnindex`，记录该 segment 内已 abort 的事务起始 offset 和结束 offset。

当 `read_committed` 消费者读取时，broker 的读取路径会遍历这些 abort 事务，把它们的 offset 区间告诉消费者，由消费者跳过这些区间。

如果事务索引不完整，abort 事务的数据就会被消费到——这违反了 `read_committed` 语义。

## 收网：marker 扇出 + LSO 截断 + txnindex 过滤 = read_committed

把整篇压成一句话：coordinator 通过 `TransactionMarkerChannelManager` 异步扇出 marker，业务分区 leader 写入 `ControlBatch` 并调用 `ProducerStateManager.completeTxn` 把事务从 ongoing 移到 unreplicated；`firstUnstableOffset` 与 `HW` 一起决定 `LSO`，`read_committed` 消费者先按 LSO 截断可见边界，再通过事务索引过滤已 abort 的事务。

```text
coordinator COMMIT/ABORT
  → marker 扇出
    → 分区写入 ControlBatch
      → completeTxn（ongoing → unreplicated）
        → LSO = min(HW, firstUnstableOffset)
          → read_committed：LSO 截断 + txnindex 过滤
```

到这里，主线只发生了五件事。

第一，marker 由 `TransactionMarkerChannelManager` 异步扇出。

第二，业务分区写入 `ControlBatch` 并调用 `completeTxn`。

第三，`completeTxn` 把事务从 ongoing 移到 unreplicated。

第四，`LSO = min(HW, firstUnstableOffset)` 决定可见边界。

第五，`read_committed` 先按 LSO 截断，再按事务索引过滤 abort。

**本篇的一句话困惑**：coordinator 说提交了，consumer 怎么知道这笔事务真的可以读到？

**本篇的一句话顿悟**：coordinator 通过 marker 扇出让业务分区写入 ControlBatch，ProducerStateManager 把事务从 ongoing 推进到 unreplicated，LSO 与事务索引共同决定 read_committed 的可见边界。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“marker 是 coordinator 自己看的日志。”** marker 要写入各业务分区，供 `read_committed` 过滤。
2. **“read_committed 就是只看有没有 COMMIT marker。”** 还要按 LSO 截断、按事务索引过滤 abort。
3. **“LSO 就是 HW。”** LSO ≤ HW，取 `min(HW, firstUnstableOffset)`。
4. **“unreplicatedTxns 不可见。”** 它只表示 marker 未复制稳定，影响 LSO，不等于最终不可见。
5. **“control batch 和普通 batch 一样。”** 它有专门的 `isControlBatch` 标记与事务处理路径。

### 关键证据清单

- `core/src/main/scala/kafka/coordinator/transaction/TransactionMarkerChannelManager.scala:161`：marker 扇出线程。
- `clients/src/main/java/org/apache/kafka/common/requests/WriteTxnMarkersRequest.java`：marker 请求。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:545`：completeTxn。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:246`：firstUnstableOffset。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java:676`：lastStableOffset。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/LocalLog.java:540`：aborted transactions 收集。