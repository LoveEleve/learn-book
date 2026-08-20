# K-11 闭环笔记 Q1-Q6: 幂等/两阶段/Marker/状态机/客户端/衔接

## Q1: 幂等怎么保证? (K-3 衔接)

假设: producerId+epoch+seq, broker 去重。

验证过程:
- 客户端: TransactionManager (TransactionManager.java:L95) — producerId/epoch 状态
- 服务端: ProducerStateManager (K-3 已交付): producers map (ProducerStateManager:L85) + lastSeq 校验 + snapshot
- 语义: 单分区单 producer 有序 + 重试不重 (broker 按 seq 判重)
- enable.idempotence → producerId 分配 (handleInitProducerId TransactionCoordinator.scala:113)

代码类型: 衔接分析

结论: **幂等 = producerId+epoch+seq (TransactionManager L95) + broker 去重 (K-3 ProducerStateManager L85) — K-3 已交付底座, K-11 复用**。TransactionManager.java:95 + ProducerStateManager (K-3)

## Q2: 两阶段提交?

假设: Prepare→Commit/Abort→Complete。

验证过程:
- TransactionCoordinator.handleEndTransaction (ProducerStateManager:L505) → endTransactionWithTV1 (ProducerStateManager:L533)
- 状态迁移: PrepareCommit/PrepareAbort (ProducerStateManager:L562-593) → CompleteCommit/CompleteAbort
- Transaction V2 状态表 (ProducerStateManager:L704-732): Empty/CompleteAbort/CompleteCommit 迁移规则
- 客户端: commitTransaction → beginCommit (TransactionManager.java:360)

代码类型: Implementation (两阶段)

结论: **两阶段 = handleEndTransaction (ProducerStateManager:L505) → Prepare (ProducerStateManager:L562-593) → Complete (V2 状态表 TransactionCoordinator.scala:L704-732); 客户端 beginCommit (TransactionManager.java:360)**。TransactionCoordinator.scala:505-593,704-732

## Q3: Marker 分发?

假设: 协调器向分区 leader 发 WriteTxnMarkers。

验证过程:
- TransactionMarkerChannelManager (ProducerStateManager:L489): markersQueuePerBroker (ProducerStateManager:L176) — 按 broker 分队列
- addTxnMarkersToSend → InterBrokerSendThread 发送 (WriteTxnMarkersRequest + TxnMarkerEntry L30-31)
- 未知 broker 队列 (ProducerStateManager:L178): 等待注册
- 语义: 事务提交后各分区 leader 写 ControlBatch (read_committed 过滤)

代码类型: Implementation (分发)

结论: **Marker = TransactionMarkerChannelManager 按 broker 队列 (ProducerStateManager:L176) + InterBrokerSendThread 发 WriteTxnMarkers (TxnMarkerEntry L30) — 提交结果广播到各分区 leader**。TransactionMarkerChannelManager.scala:176-206

## Q4: 事务状态机?

假设: TransactionStateManager 管理 __transaction_state 记录。

验证过程:
- TransactionStateManager (ProducerStateManager:L869): 事务状态持久化
- __transaction_state topic (内部, K-3 存储)
- 状态迁移表 (TransactionCoordinator.scala:704-732) — V2 状态机
- 恢复: 崩溃后从记录重建 (与 K-6 GroupMetadataManager 同构)

代码类型: Implementation (状态机)

结论: **状态机 = TransactionStateManager (ProducerStateManager:L869) + __transaction_state 记录 + V2 迁移表 (ProducerStateManager:L704-732) — 与 K-6 组记录同构 (元数据即记录)**。TransactionStateManager.scala + TransactionCoordinator.scala:704-732

## Q5: 客户端状态机?

假设: TransactionManager 状态机驱动 init/begin/commit/abort。

验证过程:
- TransactionManager (ProducerStateManager:L95): 状态机
- initTransactions (ProducerStateManager:L329) → beginTransaction (ProducerStateManager:L332) → commitTransaction (ProducerStateManager:L360) → abortTransaction (ProducerStateManager:L372)
- 注释 (ProducerStateManager:L252-255): Producer API ↔ TransactionManager 方法映射
- 幂等/事务共享状态 (producerId+epoch)

代码类型: Implementation (客户端状态机)

结论: **客户端状态机 = TransactionManager (ProducerStateManager:L95): init (ProducerStateManager:L329) → begin (ProducerStateManager:L332) → commit (ProducerStateManager:L360) / abort (ProducerStateManager:L372) — API 门面与状态机解耦 (TransactionManager.java:L252-255 注释)**。TransactionManager.java:95-372

## Q6: 与 K-1/K-3/K-6 衔接?

假设: 事务贯穿客户端/存储/协调三层。

验证过程:
- K-1: doSend 调 maybeAddPartition (trace_path 实证) — 事务分区注册
- K-3: ProducerStateManager snapshot + ControlBatch (read_committed 过滤)
- K-6: GroupCoordinator 完成事务 (WriteTxnMarkers API, GroupCoordinator.java:395 注释)
- K-5: 协调器定位 (transactional.id 哈希 → 分区 leader)

代码类型: 衔接分析

结论: **事务闭环 = K-1 客户端 (maybeAddPartition) → K-3 存储 (snapshot/ControlBatch) → K-11 协调 (两阶段+Marker) → K-6 组完成 (ProducerStateManager:L395) — 三层协议贯穿**。TransactionCoordinator.scala:505 + GroupCoordinator.java:395

跨域关联: K-1 (客户端) / K-3 (存储底座) / K-6 (组协调) / K-5 (协调器定位)
