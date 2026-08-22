# Kafka-11 重写规划

> 题目：为什么 Kafka 既能防重，又能跨分区一起提交——幂等 Producer、TransactionCoordinator 与 Marker 主链
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：解释 Kafka 如何同时解决两类问题：单分区重试不重复（幂等）与跨分区“要么都可见、要么都不可见”（事务）。主线覆盖客户端 `TransactionManager`、broker 侧 `ProducerStateManager`、事务协调器 `TransactionCoordinator`、以及 `TransactionMarkerChannelManager` 把 COMMIT/ABORT marker 扇出到各分区 leader 的闭环。

## 1. 读者困惑

- producer 重试为什么不会把同一批消息写两次？
- 只靠 producerId + sequence 就够了吗，为什么还要 transactionalId？
- Kafka 所谓“事务”到底在提交什么，为什么不是把所有分区放在一个数据库事务里？
- TransactionCoordinator 在哪、它和普通 broker / group coordinator 是什么关系？
- COMMIT / ABORT 为什么还要写 marker 到每个分区 leader？
- `read_committed` 消费者凭什么知道哪些消息该跳过？

## 2. 一句话顿悟

**Kafka 把“防重”和“跨分区原子可见”拆成两层：幂等层用 `producerId + producerEpoch + sequence` 让 broker 对单分区重复写有能力识别；事务层再用 `transactionalId → TransactionCoordinator → WriteTxnMarkers` 把“本次事务涉及哪些分区、最终是 COMMIT 还是 ABORT”统一决议并广播到各分区 leader，消费者的 `read_committed` 再据此过滤未提交数据。**

## 3. 五要素卡片

### 读者问题

Producer 因网络抖动重试同一批消息时，broker 怎么知道“这是同一批的重发”，而不是一批新消息？如果一笔业务同时写三个分区，Kafka 又如何保证消费者不会只看到其中两个分区的结果？

### 入口

- `TransactionManager`：客户端事务/幂等状态机
- `ProducerStateManager` / `ProducerStateEntry`：broker 侧按分区维护 producer 状态与序号
- `TransactionCoordinator`：事务协调器，维护 `transactionalId` 状态
- `TransactionStateManager` / `TransactionMetadata`：事务日志与事务状态
- `TransactionMarkerChannelManager`：向各分区 leader 发送 commit/abort marker
- `IsolationLevel.READ_COMMITTED`：消费者隔离级别

### 状态核心

- 幂等三元组：`producerId`、`producerEpoch`、`sequence`
- 事务身份：`transactionalId`
- 事务状态：EMPTY / ONGOING / PREPARE_COMMIT / PREPARE_ABORT / COMPLETE_* 等
- marker：`WriteTxnMarkersRequest` + `TransactionResult.COMMIT/ABORT`
- broker 侧事务轨迹：`ongoingTxns` / `unreplicatedTxns`

### 失败路径

- 重试不防重 → 同一批消息落两次
- 只做单分区防重、不做事务协调 → 跨分区一半提交一半丢失
- coordinator 只改内存不写事务日志 → 崩溃后事务结果不可恢复
- marker 不扇出到各分区 → `read_committed` 无法判断最终可见性
- 只靠 consumer 本地猜测未提交数据 → 崩溃恢复后无法收敛

### 连接点

- 前文 `Kafka-3`：`ProducerStateManager` 已经出现过，本篇补上它为何能支撑幂等与事务。
- 前文 `Kafka-10`：Purgatory 负责“等条件满足再返回”，本篇事务提交也会遇到等待 marker / 协调状态推进。
- 前文 `Kafka-4`：`read_committed` 隔离级别的消费者读取边界，在本篇解释来源。

## 4. 总图

```text
客户端
  → TransactionManager
    → initTransactions / beginTransaction
      → send(record) 带 producerId + epoch + sequence

broker 分区 leader
  → ProducerStateManager 检查 sequence / epoch
    → 单分区防重成立

事务层
  → transactionalId 映射到 TransactionCoordinator
    → 记录本次事务涉及的 partitions / offsets
      → EndTxn(COMMIT / ABORT)
        → TransactionMarkerChannelManager
          → 向各分区 leader 发送 WriteTxnMarkers
            → 分区写 ControlBatch marker
              → read_committed 消费者按 marker 过滤可见性
```

## 5. 关键边界

- 本篇把幂等与事务一起讲，但必须清楚分层：幂等只解决“单分区重复写”，事务才解决“跨分区原子可见”。
- 不把 `transactionalId` 写成“幂等开关”：没有它也能启用幂等，有它才有跨分区事务身份。
- 不把 marker 写成“给 coordinator 自己看的日志”：它必须进入每个业务分区，供 `read_committed` 过滤。
- 不展开 GroupCoordinator/ConsumerGroup 细节，本篇只在需要时引用 `read_committed`。

## 6. 失败方案推演

1. **重试时完全相信客户端不会重复发**：网络抖动下同一批消息会被写两次。
2. **每个分区各自提交，不设 coordinator**：跨分区写到一半崩溃，消费者看到部分结果。
3. **coordinator 只记住“事务完成了”，不把 marker 发到业务分区**：消费者永远不知道日志里的事务到底提交还是回滚。
4. **消费者本地缓存“我觉得这个事务成功了”**：broker 重启或 leader 切换后完全失真。

## 7. 误解清单

- “幂等 producer = 事务 producer。”：幂等只防单分区重复，事务才处理跨分区原子可见。
- “transactionalId 就是 producerId。”：前者是稳定事务身份，后者是 broker 分配的写入身份。
- “事务提交时 coordinator 把业务数据一起提交。”：业务数据早就写入分区，提交阶段只是在各分区写 marker 并统一可见性。
- “read_committed 是客户端自己去猜哪些消息未提交。”：它依赖分区里的事务索引/marker。
- “producerEpoch 只是版本号。”：它还承担 zombie producer fencing 语义。

## 8. 证据清单

- `clients/src/main/java/org/apache/kafka/clients/producer/internals/TransactionManager.java:92`：客户端事务/幂等状态机。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:54`：producer 状态与 last appended metadata。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateEntry.java:91`：epoch/sequence 更新。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionCoordinator.scala:83`：事务协调器职责。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionCoordinator.scala:113`：`handleInitProducerId`。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionStateManager.scala`：事务日志状态管理。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionMetadata.scala`：事务状态模型。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionMarkerChannelManager.scala:161`：marker 扇出线程。
- `clients/src/main/java/org/apache/kafka/common/requests/WriteTxnMarkersRequest.java`：commit/abort marker 请求。
- `clients/src/main/java/org/apache/kafka/common/IsolationLevel.java:21`：`READ_COMMITTED` / `READ_UNCOMMITTED`。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦幂等 + 事务主链，不展开 cleaner/compaction 与 Kafka Streams EOS 细节。
- 目标正文：8000~12000 字；核心拆解层覆盖幂等三元组、事务身份、coordinator、marker、read_committed 可见性。

## 10. 本轮重写主线

1. 从“为什么重试不会重写两次、跨分区为什么不会只成一半”开场。
2. 否定“只信客户端不重试”和“每个分区各自提交”两种朴素方案。
3. 解释幂等层：producerId/epoch/sequence 与 ProducerStateManager。
4. 解释事务层：transactionalId、TransactionCoordinator、TransactionStateManager。
5. 解释 EndTxn → TransactionMarkerChannelManager → WriteTxnMarkers 扇出。
6. 解释消费者 `read_committed` 为何必须依赖 marker / 事务索引。
7. 收网：幂等解决“别重复写”，事务解决“写了什么时候算可见”。