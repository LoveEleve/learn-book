# K-11 事务与幂等 篇 1/2 — 不重不漏: 幂等与客户端状态机

> 前置: [[K-3-log-04]] (ProducerStateManager) [[K-1-producer-03]] (客户端语义) | 复用: — | 对照: [[r9-replication]] (位点对照) | 引出: [[K-11-transaction-02]]
> 🟡 B | 来源: TransactionManager.java:95,252-255,329-372 + K-3 ProducerStateManager
> 定位: K-11 卷开篇 — 回答"幂等怎么保证? 客户端事务状态机?"

**读者处境**: 面试官问 "enable.idempotence 干什么? 事务怎么开始?" 你答 "去重、状态" — 但再问 "producerId 哪来? 状态机怎么转? 和 K-3 什么关系?" 你答不上来。这篇是幂等与客户端状态的完整答案。

### 1. 问题引入 — 重试会怎样

场景: 网络重试 — 同一条消息发两次会重复吗? **不做应用层去重** (broker 协议层判重)
- 幂等: producerId+epoch+seq → broker 去重 (K-3 ProducerStateManager)
- 本篇问题: 幂等 (Q1) / 客户端状态机 (Q5)

### 2. 幂等 — 三层协作

场景: 幂等怎么实现?
- 客户端: TransactionManager (TransactionManager.java:95) 持有 producerId/epoch
- 服务端: K-3 ProducerStateManager (producers map + lastSeq + snapshot) — 已交付底座
- producerId 分配: handleInitProducerId (TransactionCoordinator.scala:113)
- 语义: 重试不重 (broker 按 seq 判重) — 单分区有序

### 3. 客户端状态机 — 五态流转

场景: init/begin/commit/abort 怎么组织?
- TransactionManager (ProducerStateManager:L95) 状态机 + API 映射注释 (ProducerStateManager:L252-255)
- initTransactions (ProducerStateManager:L329) → beginTransaction (ProducerStateManager:L332) → commitTransaction→beginCommit (ProducerStateManager:L360) / abortTransaction (ProducerStateManager:L372)
- 幂等与事务共享 producerId+epoch 状态

### 核心悬念
"幂等和事务什么关系?" — 幂等是事务的基石: 幂等只保证单分区不重 (producerId+seq), 事务在此基础上加跨分区原子性 (两阶段, 篇 2) — 面试递进: 先答幂等再答事务。

### 概念依赖链
Q1 幂等 → Q5 状态机 → (02 篇: 两阶段+Marker)

### 源码锚点清单
- TransactionManager.java:95 (类) / 252-255 (API 映射注释) / 329 (initTransactions) / 332 (beginTransaction) / 360 (commitTransaction) / 372 (abortTransaction)
- TransactionCoordinator.scala:113 (handleInitProducerId)
- K-3: ProducerStateManager (producers map L85 / snapshot ProducerStateManager:L428)
