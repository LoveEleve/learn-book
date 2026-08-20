# K-11 事务与幂等 篇 2/2 — 跨分区原子: 两阶段与 Marker

> 前置: [[K-11-transaction-01]] (幂等+状态机) | 复用: — | 对照: [[rd2-rlock]] (fencing) [[r16-multi]] (Redis 事务) | 引出: — (K-9 交付后补链)
> 🟡 B | 来源: TransactionCoordinator.scala:505-593,704-732 + TransactionMarkerChannelManager.scala:176-206 + TransactionStateManager.scala
> 定位: K-11 卷收尾 — 回答"跨分区原子怎么保证? Marker 怎么分发?"

**读者处境**: 面试官问 "Kafka 事务怎么保证跨分区原子? 提交后怎么通知?" 你答 "两阶段、标记" — 但再问 "Prepare/Complete 状态? Marker 发给谁? 和 Redis MULTI 差在哪?" 你答不上来。这篇是两阶段提交的完整答案, 收束 K-11 域。

### 1. 问题引入 — 多分区要么全成要么全败

场景: 一个事务写 3 个分区 — 怎么保证原子? **不做单机排队事务** (跨 broker 两阶段)
- TransactionCoordinator 两阶段 (TransactionCoordinator.scala:505)
- 本篇问题: 两阶段 (Q2) / Marker (Q3) / 状态机 (Q4) / 对照 (Q6)

### 2. 两阶段 — Prepare→Complete

场景: 提交流程?
- handleEndTransaction (TransactionCoordinator.scala:L505) → endTransactionWithTV1 (TransactionCoordinator.scala:L533)
- PrepareCommit/PrepareAbort (TransactionCoordinator.scala:L562-593) → CompleteCommit/CompleteAbort
- V2 状态迁移表 (TransactionCoordinator.scala:L704-732): Empty/CompleteAbort/CompleteCommit 规则
- 客户端 commitTransaction → beginCommit (TransactionManager.java:360)

### 3. Marker — 结果广播

场景: 提交后各分区怎么知道?
- TransactionMarkerChannelManager (TransactionMarkerChannelManager.scala:L489): markersQueuePerBroker (TransactionMarkerChannelManager.scala:L176) 按 broker 分队列
- InterBrokerSendThread 发 WriteTxnMarkersRequest (TransactionMarkerChannelManager.scala:167 extends InterBrokerSendThread "TxnMarkerSenderThread", TxnMarkerEntry L30-31)
- 分区 leader 写 ControlBatch (UnifiedLog.java:1421 isControlBatch 过滤) → read_committed 消费者只读到 LSO (isolation.level 配置 ConsumerConfig.java:364-369, LSO=LSO 前一个 offset ConsumerConfig.java:L368)
- 对照 rd2: producer epoch 递增防僵尸 (fencing 语义)

### 4. 状态机与对照 — 收束

场景: 状态存哪? 与 Redis 差在哪?
- TransactionStateManager (TransactionStateManager.scala:L869) + __transaction_state 记录 (与 K-6 组记录同构)
- 代价: 每事务 Marker 广播 (跨分区 RTT) + 状态写 __transaction_state — 高频小事务慎用
- 协调器故障: 客户端重试 (transactional.id 重新定位, K-5 衔接)
- 对照 r16: Redis MULTI/EXEC 单机原子 vs Kafka 跨 broker 两阶段 — 原子粒度差异
- 面试记忆点: "Redis 事务=命令排队, Kafka 事务=跨分区两阶段"

### 核心悬念
"为什么需要 Marker 分发这一环?" — 协调器决定提交/中止, 但数据在各分区 leader 上 — Marker 把决定广播给所有涉及分区, leader 写 ControlBatch 标记 (read_committed 据此过滤) — 两阶段之外的关键第三环 (决定→分发→标记)。

### 概念依赖链
Q2 两阶段 → Q3 Marker → Q4 状态机 → Q6 对照 → (K-9 待补链)

### 源码锚点清单
- TransactionCoordinator.scala:505 (handleEndTransaction) / 533 (endTransactionWithTV1) / 562-593 (Prepare 迁移) / 704-732 (V2 状态表)
- TransactionMarkerChannelManager.scala:176 (markersQueuePerBroker) / 178 (未知 broker 队列) / 206 (queueForUnknownBroker) / L30-31 (TxnMarkerEntry)
- TransactionStateManager.scala (状态机)
- GroupCoordinator.java:395 (WriteTxnMarkers 完成事务, K-6 衔接)
