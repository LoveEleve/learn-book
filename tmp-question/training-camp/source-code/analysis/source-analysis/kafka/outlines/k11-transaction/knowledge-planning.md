# K-11 事务与幂等 — 知识规划 (00 §10: 逐源提取→聚合→分类→聚类)

> 2026-08-15 | 源码: clients TransactionManager (1968, Java 已索引) + core/scala/kafka/coordinator/transaction/ (TransactionCoordinator 1090 + TransactionMarkerChannelManager 489 + TransactionStateManager 869) + K-3 ProducerStateManager (已交付)
> [索引覆盖: Java 端已索引; Scala 端未索引 [标注]]

## 01 逐源提取

| 源文件 | 机制点 |
|---|---|
| TransactionManager.java (clients) | ①class (ProducerStateManager:L95) ②initTransactions (ProducerStateManager:L329) ③beginTransaction (ProducerStateManager:L332) ④commitTransaction→beginCommit (ProducerStateManager:L360) ⑤abortTransaction (ProducerStateManager:L372) ⑥状态机 |
| TransactionCoordinator.scala | ①handleInitProducerId ②handleEndTxn ③两阶段 (Prepare→Commit/Abort) |
| TransactionMarkerChannelManager.scala | ①WriteTxnMarkers 发送 (InterBrokerSendThread) ②TxnMarkerEntry |
| TransactionStateManager.scala | ①事务状态机 ②__transaction_state 记录 |
| ProducerStateManager (K-3) | ①lastSeq/epoch 幂等去重 ②snapshot |

## 02 聚合 (P1/P2/P3)

| 聚合机制 | 来源 | 分级 |
|---|---|---|
| 幂等 (producerId+epoch+seq) | TransactionManager + ProducerStateManager (K-3) | P1 |
| 两阶段提交 (Prepare→Commit/Abort) | TransactionCoordinator | P1 |
| Marker 分发 | TransactionMarkerChannelManager | P1 |
| 事务状态机 | TransactionStateManager | P1 |
| 客户端状态机 | TransactionManager | P2 |
| Epoch 防僵尸 | producerEpoch (K-3) | P2 |

## 03 深度分类

- 🔴: 幂等 + 两阶段 + Marker 分发 (事务核心)
- 🟡: 客户端状态机 / 状态机记录
- 🟢: 配置 (transaction.timeout 等)

## 04 聚类 (教学顺序)

```
幂等 (K-3 ProducerStateManager: producerId+seq 去重)
  → 事务 (跨分区): 客户端 TransactionManager 状态机
  → TransactionCoordinator 两阶段 (Prepare→Commit/Abort)
  → Marker 分发 (WriteTxnMarkers → 分区 leader)
  → 事务状态机 (__transaction_state)
```

**拆篇建议**: 2 篇 (🟡 B, 6 闭环)
- 01: 幂等与事务客户端 (TransactionManager + K-3 衔接)
- 02: 服务端两阶段与 Marker (TransactionCoordinator + MarkerChannelManager + 状态机)
