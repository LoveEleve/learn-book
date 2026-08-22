# Kafka-11. 为什么 Kafka 既能防重，又能跨分区一起提交——幂等 Producer、TransactionCoordinator 与 Marker 主链

> 场景：Kafka-2 里我们已经见过 Producer 的异步发送主链，Kafka-3 里也见过 `ProducerStateManager` 这个名字。但那时还欠两个尖锐问题：**Producer 重试为什么不会把同一批消息写两次？跨分区的事务为什么不会只成功一半？** 本篇把这两条线压到一起：先讲幂等，再讲事务，最后讲 `read_committed` 为什么有能力只看见“已经真正提交”的结果。

## 先把真正的困惑摆出来：重试怎么不重，跨分区怎么不裂

先看最简单的一种失败。

producer 把一批消息发到 partition-0，broker 已经成功写入本地日志，但响应在网络上丢了。客户端以为这批没成功，于是重试一次。如果 broker 毫无防备，它会把同一批消息再写一遍——重复数据就出现了。

再看更复杂的一种失败。

一笔业务同时要写三个分区：

- 订单主记录写 partition-A
- 库存扣减写 partition-B
- 支付流水写 partition-C

前两个分区已经写入成功，第三个分区写到一半 broker 崩了。如果 Kafka 没有事务协调，消费者就会看到“订单已创建、库存已扣减、支付流水没了”这种裂开的中间状态。

```text
send A 成功
send B 成功
send C 失败 / 崩溃
  → 消费者看到 A、B，看不到 C
    → 业务状态撕裂
```

这两个问题看起来像同一类“可靠性”问题，但其实不是一个层次。

- **幂等**解决的是：同一批消息重试时，别再写第二次。
- **事务**解决的是：跨分区一组消息，什么时候才算“整体可见”。

如果把两者混成一句“Kafka 靠事务保证不重复”，主链就会立刻糊掉，因为单分区重试防重和跨分区原子可见，用的是两套不同但串联起来的机制。

*关键设计（斜体）：* *Kafka 先用 `producerId + producerEpoch + sequence` 在分区 leader 上建立单分区幂等边界，再用 `transactionalId → TransactionCoordinator → WriteTxnMarkers` 建立跨分区事务可见性边界：前者回答“这批是不是重复写”，后者回答“这批什么时候才算整体提交”。*[模式: 单分区防重 + 跨分区可见性决议]

## 第一层：幂等不是“客户端说我没重试”，而是 broker 能识别“这批我见过”

如果只靠客户端自觉，重试不重复这件事根本无解。broker 看到第二次发送时，不知道这是“同一批重发”，还是“客户端真的又要写一批新数据”。

Kafka 的幂等做法是让 producer 的每个分区写入都带上三元组：

- `producerId`
- `producerEpoch`
- `sequence`

这三个字段一起回答三个问题：

- **是谁写的**（producerId）
- **是不是这个 writer 的当前合法世代**（producerEpoch）
- **这是这个 writer 在本分区上的第几批**（sequence）

broker 侧的 `ProducerStateManager` 维护的正是这份映射：每个 producerId 最后成功追加到这个分区的 epoch、sequence、last offset、timestamp 等状态。它是**按分区维护**的，不是全局一张表。

```text
partition-0 上的 ProducerStateManager
  → producerId 123
    → producerEpoch = 7
    → lastSeq = 42
    → lastOffset = 10086
```

于是同一批消息重试时，broker 就不再是“盲写”：它会把新请求带来的 epoch/sequence 和这份已知状态比较，判断这是重复、乱序、僵尸 writer，还是合法的新批次。

这一步一定要发生在 broker，不能只在客户端，因为真正掌握“这个分区上最后落盘的写入是谁、写到第几批”的只有 broker。

## 第二层：`producerEpoch` 不是普通版本号，而是用来 fence 僵尸 producer

很多人第一次看到幂等三元组时，会把 `producerEpoch` 理解成“一个普通版本号”。这不够。

它更准确的角色是：**fencing token**。

设想一个最危险的场景：

- 旧 producer 因网络分区短暂失联；
- 新 producer 恢复事务身份，拿到了新的 epoch；
- 旧 producer 又恢复网络，继续发消息。

如果 broker 不检查 epoch，旧 producer 会带着已经过期的 sequence 继续写，形成真正的僵尸 writer。

这里要把两步动作拆开看。

第一步，`ProducerStateEntry.maybeUpdateProducerEpoch()` 的语义是：epoch 一旦变化，就清掉之前那批 batchMetadata，把这个 writer 的“当前合法写入窗口”切到新 epoch 上。

第二步，**旧 epoch 的 writer 真正被拒绝**，不是靠这个方法本身，而是靠 broker 追加路径里的 epoch/sequence 校验（如 `ProducerAppendInfo.checkProducerEpoch()`、`UnifiedLog` 的 append 校验）。也就是说，`maybeUpdateProducerEpoch()` 负责更新状态窗口，真正的 fencing 则发生在后续写入校验里。

所以：

- `sequence` 负责分区内顺序与防重复；
- `producerEpoch` 负责 fencing 僵尸 writer；
- 两者一起才构成真正可用的幂等边界。

如果只有 sequence 没有 epoch，旧 producer 与新 producer 可能都带着看似合法的 sequence 窗口写入同一分区，broker 根本无法判断谁已经过时。

## 第三层：幂等只够解决“单分区不重复”，不够解决“跨分区一起可见”

走到这里，很多读者会有一个自然误解：既然每个分区都能靠 producerId/epoch/sequence 防重，那跨分区事务是不是就自动有了？

答案是否定的。

因为幂等只保证：

- partition-A 上不要重复
- partition-B 上不要重复
- partition-C 上不要重复

它**不保证**：

- A/B/C 这三次写入对消费者“要么一起可见，要么一起不可见”。

换句话说，幂等解决的是“别多写”，事务解决的是“什么时候算提交”。

这就是为什么 Kafka 还需要第二层身份：`transactionalId`。

`producerId` 是 broker 分配的写入身份，主要服务于幂等；`transactionalId` 是客户端稳定提供的事务身份，主要服务于“找回这笔事务是谁的、它现在处在什么状态、它涉及哪些分区”。

没有 `transactionalId`，Kafka 就无法把“这几个分区其实属于同一笔事务”串起来，更不可能在崩溃恢复后继续推进这笔事务。

## 第四层：TransactionCoordinator 负责的不是“写业务数据”，而是“维护事务决议”

`TransactionCoordinator` 的职责不是帮业务分区写消息，业务消息早就已经各自写到分区 leader 上了。它真正维护的是：

- 某个 `transactionalId` 当前归哪个 coordinator 管；
- 这笔事务现在处于什么状态（EMPTY / ONGOING / PREPARE_COMMIT / PREPARE_ABORT / COMPLETE_*）；
- 这笔事务涉及哪些 topic-partitions；
- 最终结果是 COMMIT 还是 ABORT。

当客户端调用 `initTransactions()` 或初始化事务身份时，会先走 `TransactionCoordinator.handleInitProducerId()`。如果 `transactionalId` 不存在，就创建一条新的 `TransactionMetadata`；如果已存在，就取回它的历史状态，再决定是否 bump producer epoch、是否继续沿用事务身份。

所以 coordinator 真正持久化的是**事务状态机**，不是业务消息本身。这也是它为什么要把状态写进事务日志，而不是只改内存：broker 崩溃后，必须还能恢复出“transactionalId = foo 这笔事务曾经写到哪一步”。

```text
transactionalId = order-service-42
  → producerId = 991
  → producerEpoch = 3
  → state = ONGOING
  → topicPartitions = {A, B, C}
```

如果 coordinator 只在内存里记住这些信息，一旦 broker 宕机，事务就会掉进“消息已经写了，决议却丢了”的黑洞。

## 第五层：事务提交不是“把消息一起提交”，而是“把最终结果扇出到每个分区”

这一步最容易误解。

很多人听到“事务提交”时，脑中会自动浮现数据库那种“所有修改统一在提交时一起落盘”的图景。但 Kafka 不是这样。

Kafka 的业务数据在 producer `send()` 时就已经正常写到各个分区了。事务提交阶段做的不是“现在才写业务数据”，而是：**告诉每个涉及的分区 leader，这批已写入的数据最终是 COMMIT 还是 ABORT。**

这个“告诉”的载体就是 **marker**。

`TransactionMarkerChannelManager` 维护了一个专门的线程和按 broker 分桶的 marker queue，把待完成事务变成 `WriteTxnMarkersRequest.TxnMarkerEntry`，扇出到每个相关分区 leader。

```text
EndTxn(COMMIT)
  → TransactionCoordinator 先把事务状态推进到 PREPARE_COMMIT / PREPARE_ABORT
    → 向客户端返回 EndTxn 成功
      → TransactionMarkerChannelManager 异步扇出 marker
        → WriteTxnMarkersRequest
          → 各分区 leader 写入 COMMIT / ABORT marker
            → coordinator 再推进 COMPLETE_* 状态
```

ABORT 也是同样的流程，只不过 marker 的结果是 `TransactionResult.ABORT`。

所以事务提交的本质不是“把消息一起写下去”，而是“**把事务最终结果写进每个业务分区**”。还要注意一个常被忽略的边界：客户端拿到 EndTxn 成功，并不等于所有业务分区上的 marker 已经同步写完；它表示 coordinator 已把事务推进到可继续发送 marker 的阶段，而 marker 本身仍由 `TransactionMarkerChannelManager` 异步扇出。只有这些 marker 真正进入各分区，消费者才有可能在读取时知道：这段事务消息已经提交，还是应该丢弃。

## 第六层：为什么必须把 marker 扇出到每个业务分区

既然 coordinator 已经知道“这笔事务提交了”，为什么不让消费者直接去问 coordinator？

因为消费者读取消息时，面对的是**分区日志**，不是事务协调器的内存状态。它需要在“读这个 partition 的同时”就知道某一段事务数据该不该可见。

如果事务结果不进入业务分区，而只存在 coordinator 侧：

- broker 读取分区时需要额外远程查询 coordinator；
- coordinator 崩溃或 leader 切换后，可见性判断会抖动；
- partition 离线恢复时，单靠本地日志根本无法自洽地重建可见性。

这就是 marker 必须扇出到每个分区的根本原因：**可见性判断必须跟着分区日志走，不能依赖一个外部“我记得这笔事务成功了”的中心缓存。**

`TransactionMarkerChannelManager` 之所以存在，就是为了把“coordinator 的全局事务决议”变成“每个业务分区本地都看得见的控制记录”。

## 第七层：`read_committed` 看的不是“客户端猜测”，而是分区里的事务边界

消费者侧只需要两个隔离级别：

- `READ_UNCOMMITTED`
- `READ_COMMITTED`

问题在于：`READ_COMMITTED` 凭什么知道哪些消息该跳过？

答案不是“消费者本地记忆谁提交过”，而是：broker 在读取日志时先看 **LSO（last stable offset）**，把可见边界截在 `min(HW, firstUnstableOffset)` 这一层；随后再结合事务 marker 与事务索引，把已 abort 的事务消息过滤掉，只把真正 committed 的部分交给消费者。

这也是为什么 Kafka-3 里 `LogSegment` 除了 `.log`、`.index`、`.timeindex` 之外，还有 `.txnindex`。事务不是独立漂浮在外面，而是已经嵌进了分区日志的读取结构里。

所以 `read_committed` 能成立，不是因为 consumer 更聪明，而是因为 broker 已经把“事务最终结果”落进了日志：先用 LSO 卡住“尚不稳定”的事务尾巴，再用 marker/索引裁掉不该可见的 abort 事务。

如果没有这层日志内事务边界，consumer 就只能依赖外部状态猜测“这批消息应不应该看见”，在崩溃恢复与 leader 切换后几乎不可能保持一致。

## 第八层：ProducerStateManager 为什么同时维护 `ongoingTxns` 与 `unreplicatedTxns`

回到 broker 本地，`ProducerStateManager` 除了 producerId → state 映射，还维护了两组非常关键的事务轨迹：

- `ongoingTxns`：按 firstOffset 排序的进行中事务；
- `unreplicatedTxns`：已经结束，但 marker 还在 HW 之上的事务。

这两个集合回答的是：

- 哪些事务还没收尾？
- 哪些事务虽然已经写了 marker，但 marker 还在 HW 之上，尚未进入稳定复制边界？

这说明 Kafka 对事务可见性的判断，不是单看“有没有 COMMIT marker”，还要看这个 marker 是否已经跨过稳定复制边界。`unreplicatedTxns` 本身不是“消费者一定不可见”的最终判定器，但它会影响 `firstUnstableOffset / LSO` 的计算，从而间接决定 `read_committed` 的可见上界。

于是 Kafka 的完整链条变成：

```text
幂等层
  → producerId + epoch + sequence 防重
事务层
  → transactionalId + coordinator 决议
marker 层
  → 写入各业务分区
可见性层
  → read_committed 按 marker / txn index / HW 过滤
```

每一层都在补上一层做不到的东西。

## 收网：幂等解决“别重复写”，事务解决“什么时候算可见”

把整篇压成一句话：Kafka 先用 `producerId + producerEpoch + sequence` 让每个分区 leader 有能力识别重试、拒绝僵尸 writer，解决“别重复写”；再用 `transactionalId → TransactionCoordinator → WriteTxnMarkers` 把跨分区事务的最终结果统一决议并扇出到每个业务分区，解决“什么时候才算整体可见”；消费者的 `read_committed` 再沿着分区日志里的 marker 和事务索引过滤未提交数据。

```text
send(record)
  → broker 分区 leader 用 ProducerStateManager 防重
    → transactionalId 把多个分区绑成一笔事务
      → coordinator 决议 COMMIT / ABORT
        → marker 扇出到各业务分区
          → read_committed 按 marker 过滤可见性
```

到这里，主线只发生了八件事。

第一，幂等和事务不是一层机制，先后解决不同问题。

第二，幂等三元组是 `producerId + producerEpoch + sequence`。

第三，`producerEpoch` 负责 fencing 僵尸 writer，不是普通版本号。

第四，`transactionalId` 给跨分区事务一个可恢复的稳定身份。

第五，TransactionCoordinator 维护的是事务状态机，不是业务数据本身。

第六，事务提交不是“现在才写数据”，而是把 COMMIT/ABORT marker 扇出到各分区。

第七，`read_committed` 依赖分区日志里的 marker / 事务索引，不靠客户端猜测。

第八，ProducerStateManager 同时跟踪进行中事务与尚未复制稳定的事务边界。

**本篇的一句话困惑**：为什么 Kafka 重试不会重写两次，跨分区又不会只成功一半？

**本篇的一句话顿悟**：因为 Kafka 先在分区 leader 上用幂等三元组解决“别重复写”，再用 `transactionalId → coordinator → marker` 解决“这批写什么时候才算整体可见”。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“幂等 producer = 事务 producer。”** 幂等只防单分区重复，事务才处理跨分区原子可见。
2. **“transactionalId 就是 producerId。”** 前者是稳定事务身份，后者是 broker 分配的写入身份。
3. **“事务提交时 coordinator 把业务数据一起提交。”** 业务数据早就已经写进分区，提交阶段写的是 marker。
4. **“read_committed 是客户端自己推断哪些消息未提交。”** 它依赖分区里的 marker / 事务索引。
5. **“producerEpoch 只是一个数字版本。”** 它同时承担 zombie producer fencing 语义。

### 关键证据清单

- `clients/src/main/java/org/apache/kafka/clients/producer/internals/TransactionManager.java:92`：客户端事务/幂等状态机。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:54`：producer 状态与 last appended metadata。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateEntry.java:91`：epoch/sequence 状态更新。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerAppendInfo.java:96`：旧 epoch / sequence 在追加时被真正校验拒绝。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionCoordinator.scala:83`：事务协调器职责。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionCoordinator.scala:113`：`handleInitProducerId`。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionCoordinator.scala:505`：`handleEndTransaction` 的 PREPARE → marker → COMPLETE 主链。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionStateManager.scala`：事务日志状态管理。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionMetadata.scala`：事务状态模型。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionMarkerChannelManager.scala:161`：marker 扇出线程。
- `clients/src/main/java/org/apache/kafka/common/requests/WriteTxnMarkersRequest.java`：commit/abort marker 请求。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java:676`：`lastStableOffset()` / LSO。
- `clients/src/main/java/org/apache/kafka/common/IsolationLevel.java:21`：`READ_COMMITTED` / `READ_UNCOMMITTED`。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦幂等 + 事务主链，不展开 cleaner/compaction、Streams EOS 与 coordinator 迁移细节。
- 本篇把 marker / transaction index 作为可见性边界来讲，不回头展开 consumer fetch 主链。
- 不把 ProducerStateManager、TransactionCoordinator、TransactionMarkerChannelManager 三者混成一个状态机：它们分别解决分区防重、事务决议、marker 扇出。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-2`（Producer 发送链）、`Kafka-3`（ProducerStateManager/txnindex）、`Kafka-4`（read_committed）、`Kafka-10`（等待条件满足的统一框架）。
- 后续桥接：下一篇可以补 Kafka 事务 / 幂等的深度 review，或者回到 compaction / cleaner，把墓碑、txn 边界与日志清理接起来。