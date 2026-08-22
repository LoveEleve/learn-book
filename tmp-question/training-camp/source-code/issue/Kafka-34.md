# Kafka-34. 事务的状态存哪、由谁推进——TransactionCoordinator 与 `__transaction_state` 主链

> 场景：Kafka-11 讲事务时把 TransactionCoordinator 当作一个人人都懂的名词，Kafka-33 又把幂等 producer 的 seq/epoch 校验讲透了。但有一个关键问题还没展开：**一笔事务从 `initTransactions()` 到 `commitTransaction()`，它的状态存在哪、由谁推进？** 本篇正面回答：transactionalId 靠哈希定位到 `__transaction_state` 分区，分区 leader 上的 `TransactionCoordinator` 负责推进状态机，并通过 `appendTransactionToLog` 把每次变更持久化。这是 K-12 事务幂等域第 3 篇。

## 先把真正的困惑摆出来：一笔事务是怎么找到“主持人”的

当你调用 `producer.initTransactions()` 时，客户端只是提供了一个 `transactionalId`。Kafka 怎么知道这一笔该找谁管理状态？

它不能随机找一台 broker，否则同一笔事务的两个请求会到不同地方。它也不能全局轮询，那样形成单点。

答案是**哈希分片**：

```text
transactionalId
  → hashCode % __transaction_state 分区数
    → 得到 partition
      → 该分区 leader 所在 broker
        → 那个 broker 上的 TransactionCoordinator 就是管理者
```

这和你访问一个分区选择 leader 的方式几乎一样，只不过这个“分区”属于内部 topic `__transaction_state`。

*关键设计（斜体）：* *事务协调器不是独立进程，而是 broker 上的组件分片。每个 transactionalId 经 `partitionFor` 哈希到 `__transaction_state` 的某个分区，分区 leader 承载该事务的协调者；`TransactionStateManager` 管理各分区状态缓存，`TransactionCoordinator` 推进状态机，并通过 `appendTransactionToLog` 把每次迁移写入对应分区日志。*[模式: 哈希分片 + 分区 leader 主持 + 状态日志化]

## 第一层：TransactionStateManager 不是全局单表，而是按分区缓存

`TransactionStateManager` 管理事务日志。它并不持有一张“所有事务”的大表，而是按 `__transaction_state` 分区组织状态缓存。

`partitionFor(transactionalId)` 做哈希取模：

```text
Utils.abs(transactionalId.hashCode) % transactionTopicPartitionCount
```

每个事务状态实际落在一个具体的 `__transaction_state` 分区上。这与 Kafka 其它内部协调完全一致：特定 key 一定对应某个分区，桌面不靠全局扫描。

`getTransactionState(transactionalId)` 会：

1. 计算该 transactionalId 所在分区；
2. 在该分区缓存的 leader 里查找对应的 `TransactionMetadata`。

## 第二层：TransactionMetadata 记录一单事务的完整状态

一笔事务对应的 `TransactionMetadata` 包含：

- `producerId` 与 `producerEpoch`
- 当前 `TransactionState`
- 已加入事务的 `topicPartitions`
- `coordinatorEpoch`
- `lastUpdateTimestamp`

`TransactionState` 是一个显式状态机：

```text
EMPTY
  → ONGOING
    → PREPARE_COMMIT / PREPARE_ABORT
      → COMPLETE_COMMIT / COMPLETE_ABORT
        → (后续可重新 EMPTY / 进入 DEAD)
```

## 第三层：handleInitProducerId——从 transactionalId 到 producerId + epoch

`TransactionCoordinator.handleInitProducerId()` 处理 `initTransactions()`。它：

```text
input: transactionalId
  → getTransactionState
    → 不存在？创建 TransactionMetadata（EMPTY）
      → 若协调器 not active → 返回相应错误
    → 校验 epoch / 状态
    → 生成新的 producerId / bump epoch
      → appendTransactionToLog
```

这一步把“稳定的事务身份（transactionalId）”与“broker 分配的写入身份（producerId + epoch）”绑定起来。

如果元数据不存在，coordinator 新建一条 `TransactionMetadata`：state=EMPTY、随 producerIdManager 分配 producerId、epoch 从 NO_PRODUCER_EPOCH 开始。

## 第四层：状态机迁移必须写进事务日志，不能只改内存

无论是一次 InitProducerId、一个 AddPartitionsToTxn，还是 EndTxn，协调器都会生成新的目标状态（`TxnTransitMetadata`），然后调用 `appendTransactionToLog` 把它写入对应分区。

这就是为什么事务状态能恢复：

```text
内存状态
  + appendTransactionToLog
    → 写入 __transaction_state 分区
      → coordinator 崩溃后仍能从日志重建
```

事务日志的分区本身也有多副本，分区 leader 宕机后重新选举，新 leader 上的 coordinator 用日志重建事务状态。

如果只改内存不写日志，coordinator 一旦宕机，内存里所有事务状态都会消失——既无法判断哪些事务准备提交，也无法判断哪些还在进行。

## 第五层：为什么 EndTxn 前的 PREPARE_COMMIT / PREPARE_ABORT 那么重要

当客户端调用 `commitTransaction()` 或 `abortTransaction()` 时，`handleEndTransaction()` 会把状态从 `ONGOING` 推进到 `PREPARE_COMMIT` 或 `PREPARE_ABORT`。

这个“PREPARE_”阶段的意义在于：**它先做“我准备结束这笔事务”，把意向记入事务日志，真正的 COMPLETE 等所有分区 marker 都写完后再推进。**

也就是说，事务状态机在结束前特意留了一个“决策已定但尚未最终生效”的中转态，配合事务日志保证：即使 `EndTxn` 请求后协调器立刻崩溃，下一次恢复也知道“这笔事务打算提交/中止”，从而能继续推进。

## 第六层：coordinator epoch 防止旧协调器覆盖状态

事务日志分区可能因为 leader 切换而换协调者。为区分“当前真正主持这个分区的协调者”，协调器带一个 `coordinatorEpoch`。

`TransactionStateManager` 在写日志或改内存前会校验 epoch：

- 如果请求里的 coordinatorEpoch 与当前分区 epoch 不一致，说明发起请求的协调器已经过期，拒绝写入；
- 这正是 Kafka-7/26 里“只认最新代次”的同一思路，只是用在了事务日志层。

## 收网：事务状态 = 哈希分区 + 内存缓存 + 事务日志

把整篇压成一句话：一笔事务通过 `partitionFor(transactionalId)` 哈希到 `__transaction_state` 的某个分区，由该分区 leader 上的 `TransactionCoordinator` 主持；协调器维护 `TransactionMetadata` 状态机，并通过 `appendTransactionToLog` 把每次状态迁移写入分区日志；这样即使 broker 崩溃、分区 leader 切换，新协调器也能从日志重建事务状态。

```text
transactionalId
  → partitionFor()
    → __transaction_state 分区
      → 分区 leader = coordinator
        → TransactionMetadata（state / producerId / partitions）
          → appendTransactionToLog
            → 崩溃后从日志重建
```

到这里，主线只发生了六件事。

第一，事务协调器是 broker 上的分片组件，不是独立进程。

第二，transactionalId 通过哈希定位到 `__transaction_state` 分区。

第三，TransactionMetadata 记录单笔事务的完整状态。

第四，handleInitProducerId 把 transactionalId 与 producerId/epoch 绑定。

第五，状态迁移必须写进事务日志，不能只改内存。

第六，coordinator epoch 防止旧协调器覆盖最新状态。

**本篇的一句话困惑**：transactionalId 怎么找到该由谁管理，事务状态又存不存得下？

**本篇的一句话顿悟**：transactionalId 哈希到 __transaction_state 分区，分区 leader 上的 coordinator 是主持人；每次状态迁移都通过 appendTransactionToLog 写日志，崩溃后仍能重建。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“事务协调器是独立进程。”** 它是 broker 上的分片组件。
2. **“transactionalId 永远对应同一台固定 broker。”** 它哈希到 `__transaction_state` 分区，随分区 leader 迁移。
3. **“TransactionStateManager 是全局单表。”** 它按事务日志分区缓存。
4. **“__transaction_state 和 __consumer_offsets 是一个 topic。”** 是两个不同内部 topic。
5. **“事务状态只存内存。”** 每次迁移都会 appendTransactionToLog 持久化。

### 关键证据清单

- `core/src/main/scala/kafka/coordinator/transaction/TransactionStateManager.scala:70`：TransactionStateManager。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionStateManager.scala:444`：partitionFor。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionStateManager.scala:652`：appendTransactionToLog。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionCoordinator.scala:113`：handleInitProducerId。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionCoordinator.scala:505`：handleEndTransaction。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionCoordinator.scala:91`：TransactionCoordinator。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionMetadata.scala`：TransactionMetadata。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦 coordinator 与事务日志，不展开 marker 扇出 / read_committed（K-12 第 4 篇）。
- 不把事务日志分区与 group 协调器的 `__consumer_offsets` 分区混同。
- 不在本文重复 marker / control batch 细节。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-11`（事务总览）、`Kafka-33`（幂等 producer）。
- 后续桥接：下一篇进入 K-12 第 4 篇（marker / control batch / read_committed）。