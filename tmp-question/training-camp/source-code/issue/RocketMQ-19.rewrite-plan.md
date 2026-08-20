# RocketMQ-19 重写规划

> 题目：事务消息为什么不能直接发正式消息 —— 半消息主链
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：解释 RocketMQ 事务消息为什么必须先把消息写成 half message，而不是直接发正式消息；把问题收敛到“先把消息变成可恢复但暂不可见的系统事实”，不提前吞掉回查与二阶段 commit/rollback。

## 1. 读者困惑

- 业务本地事务都还没决定 commit/rollback，为什么 RocketMQ 不能先把正式消息发出去？
- 事务消息为什么要额外引入 half topic，而不是在原 Topic 上打个未提交标记？
- `TransactionMQProducer.sendMessageInTransaction()` 到 Broker 侧 `prepareMessage()` 之间，真正成立的是什么事实？
- half message 和普通消息、延迟消息、重试消息的“暂不可见”边界有什么不同？
- 为什么“先写半消息”是事务恢复的前提，而不是一种多余绕路？

## 2. 一句话顿悟

**RocketMQ 事务消息不能直接发正式消息，因为在本地事务尚未决断之前，Broker 只能先保存一份“可恢复、可回查、但暂时不能进入正式消费世界”的半消息事实；half message 解决的是事务悬而未决时的真相落点，而不是最终投递。**

## 3. 五要素卡片

### 读者问题

为什么事务消息不直接把正式消息写到真实 Topic/Queue，而要先进入 `RMQ_SYS_TRANS_HALF_TOPIC`？

### 入口

- `TransactionMQProducer.sendMessageInTransaction()`：事务发送入口
- `DefaultMQProducerImpl.sendMessageInTransaction()`：客户端发送 + 本地事务执行桥
- `TransactionalMessageService.prepareMessage()` / `asyncPrepareMessage()`：Broker 侧 half message 存储入口
- `TransactionalMessageBridge.putHalfMessage()`：把真实消息改写为 half message

### 状态核心

- `RMQ_SYS_TRANS_HALF_TOPIC` 与真实 topic/queue 的隔离关系
- `MessageSysFlag.TRANSACTION_PREPARED_TYPE`
- `PROPERTY_REAL_TOPIC` / `PROPERTY_REAL_QUEUE_ID` / `PROPERTY_TRANSACTION_PREPARED`
- Producer 的本地事务状态：未知、提交、回滚
- half message 的“已可恢复但不可正式消费”边界

### 失败路径

- 直接发正式消息：业务事务回滚后，Consumer 可能已经看见这条消息
- 不先落盘 half message：Producer 宕机或本地事务卡住后，Broker 连一份可回查的事实都没有
- 只在原 topic 打标记：普通消费主链、索引视图和事务检查世界会互相污染
- 本地事务执行结果迟迟未定：必须保留 half message 作为后续回查与二阶段处理的落点
- half message 存储失败：事务主链在“建立可恢复事实”这一步就中断

### 连接点

- 前文 `RocketMQ-4/5/6`：CommitLog/ConsumeQueue/可见性主链提供“什么叫进入正式消费世界”的前置语义
- 前文 `RocketMQ-18`：half message 之所以必须先落盘，本质上是为了故障后仍有一份可承认的恢复事实
- 后文 `RocketMQ-20`：Producer 本地事务执行完了为什么还要回查
- 后文 `RocketMQ-21`：Broker 二阶段 commit/rollback 怎样把 half message 真正转正或丢弃

## 4. 总图

```text
Producer 发事务消息
  → Broker 不直接投到真实 Topic
    → 先改写为 half message 写入 RMQ_SYS_TRANS_HALF_TOPIC
      → Producer 执行本地事务
        → 在二阶段结果到来前，消息只作为“可恢复但不可正式消费”的事实存在
          → 后续再决定 commit / rollback / check
```

## 5. 关键边界

- 本篇只讲“为什么先写 half message”，不展开回查协议与二阶段转正细节。
- 不把 half message 写成“延迟消息/重试消息的又一种变体”；它服务的是事务悬而未决边界。
- 不把 prepare 成功误写成事务成功；它只代表 half fact 已建立。
- 不把 `RMQ_SYS_TRANS_HALF_TOPIC` 说成简单缓存区；它是事务恢复和回查的系统事实层。

## 6. 失败方案推演

1. **先发正式消息，再看本地事务结果**：最直觉，但业务回滚时正式消息可能已被消费，系统无法补救。
2. **本地事务执行完再发消息**：看似避免半消息，但 Producer 若在本地事务后宕机，Broker 根本不知道这条待决消息存在。
3. **原 Topic 打个未提交标记**：会把事务暂态直接带入普通消费索引和主链，污染可见性边界。
4. **不先落盘，只把事务状态保留在 Producer 本地**：Broker 失去后续回查和恢复的锚点。

## 7. 误解清单

- half message 不是正式消息的“慢一点发送版”。
- prepare 成功不等于事务成功，更不等于 Consumer 已经可见。
- `RMQ_SYS_TRANS_HALF_TOPIC` 不是普通业务 Topic。
- 事务消息真正先保护的不是消费成功，而是“待决状态下仍有系统事实可恢复”。
- 本篇不回答 commit/rollback 怎样落稳，那是下一篇的主题。

## 8. 证据清单

- `client/src/main/java/org/apache/rocketmq/client/producer/TransactionMQProducer.java:82`
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/TransactionalMessageService.java:28`
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageServiceImpl.java:99`
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageServiceImpl.java:104`
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageUtil.java:42`
- `broker/src/main/java/org/apache/rocketmq/broker/processor/EndTransactionProcessor.java:129`
- `broker/src/test/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageServiceImplTest.java:87`
- `broker/src/test/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageUtilTest.java:51`

## 9. 版本边界与字数预算

- 基线：RocketMQ `5.3.1`。
- 本篇聚焦事务消息的 prepare/half-message 阶段；回查与二阶段提交回滚留给 `RocketMQ-20/21`。
- 目标正文：8000~12000 字；核心拆解层覆盖直觉失败方案、half topic 改写、暂不可见边界、恢复锚点和与普通主链的隔离关系。

## 10. 本轮重写主线

1. 从“为什么不能直接发正式消息”开场。
2. 先否定几种朴素方案：先发正式消息、晚点再发、原 Topic 打标记、只靠 Producer 本地记状态。
3. 解释 `TransactionMQProducer` 到 `prepareMessage()` 的 half-message 主链。
4. 解释 `RMQ_SYS_TRANS_HALF_TOPIC`、事务 sysFlag 和 realTopic/realQueue 元数据为什么必须存在。
5. 解释 half message 真正建立的是哪一层系统事实，以及它为什么是后续回查/二阶段的前提。
6. 收网：本篇只立“先写 half message”，下一篇再回答“为什么 Broker 还要回查”。