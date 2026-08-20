# RocketMQ-21 重写规划

> 题目：Broker 二阶段 commit/rollback 怎样把事务消息真正落稳
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：解释 Broker 在拿到 Producer 的 commit/rollback 结论后，怎样校验 prepare message、还原真实消息、重新写入最终 Topic/Queue，或删除 half message，从而把待决事实收束成最终系统事实。

## 1. 读者困惑

- Producer 已经给出 commit/rollback 结论以后，Broker 还要再做哪些校验和转换？
- 为什么 commit 不是简单把 half message 改个状态，而是重新构造最终消息并重新写入？
- rollback 为什么不需要投递真实消息，却仍然要删除 prepare message？
- `fromTransactionCheck=true` 和普通 `endTransaction()` 在 Broker 二阶段路径里有什么边界差别？
- 什么情况下 Broker 会拒绝 commit/rollback，而不是直接照单全收？

## 2. 一句话顿悟

**RocketMQ 事务消息的二阶段，不是给 half message 改个标签，而是把待决事实重新裁决成最终事实：commit 要校验 prepare message、还原真实 Topic/Queue 并重新写入正式消息；rollback 则要确认身份正确后删除 prepare message，让待决事实彻底消失。**

## 3. 五要素卡片

### 读者问题

Broker 拿到事务最终结论以后，怎样把 half message 真正收束成“正式成立”或“彻底回滚”？

### 入口

- `EndTransactionProcessor.processRequest()`：二阶段总入口
- `TransactionalMessageService.commitMessage()` / `rollbackMessage()`：从 half world 找回 prepare message
- `checkPrepareMessage()` / `rejectCommitOrRollback()`：身份与时序校验
- `endMessageTransaction()` / `sendFinalMessage()`：构造并落最终正式消息
- `deletePrepareMessage()`：清掉已完成的 half fact

### 状态核心

- `EndTransactionRequestHeader.commitOrRollback`
- `fromTransactionCheck=true/false`
- prepare message 的 producerGroup / queueOffset / commitLogOffset / realTopic / realQueueId
- `TRANSACTION_COMMIT_TYPE` / `TRANSACTION_ROLLBACK_TYPE`
- `PROPERTY_TRANSACTION_PREPARED` 清理前后
- 正式消息写入结果与 half message 删除结果

### 失败路径

- Broker 是 slave：直接拒绝 end transaction
- prepare message 校验失败：producerGroup、queueOffset、commitLogOffset 对不上
- commit/rollback 请求晚于自定义 first check 窗口：返回 `ILLEGAL_OPERATION`
- commit 写正式消息失败：不能先删 half message
- rollback 删除 prepare 失败：待决事实不能算真正收束
- 回查结果是 `TRANSACTION_NOT_TYPE`：Broker 继续保留待决状态，不进入最终裁决

### 连接点

- 前文 `RocketMQ-19`：half message 提供待决事实与真实 Topic/Queue 恢复坐标
- 前文 `RocketMQ-20`：回查链负责把状态推进到“有资格裁决”的阶段
- 前文 `RocketMQ-18`：二阶段落稳也是恢复语义的一部分——最终事实必须能重新被系统承认

## 4. 总图

```text
Broker 收到 endTransaction(commit / rollback)
  → 先找到并校验 prepare message
    → commit: 还原真实 Topic/Queue → 重写正式消息 → 成功后删除 half message
    → rollback: 不写正式消息 → 直接删除 half message
      → 待决事实被收束成最终存在或最终消失
```

## 5. 关键边界

- 本篇只讲二阶段落稳，不回头重讲“为什么先写 half message”或“为什么还要回查”。
- 不把 commit 写成“原地修改 half message”；源码是重新构造正式消息再写一次。
- rollback 不等于什么都不做；它仍要完成对 prepare message 的确认与清理。
- `UNKNOW` 在本篇不是 commit/rollback 的一种结果，而是上一篇回查后仍不进入二阶段的待决状态。

## 6. 失败方案推演

1. **commit 直接把 half message 标成已提交**：真实 Topic/Queue、正常消费索引和消息属性恢复都无从成立。
2. **rollback 什么都不做**：half fact 会永远残留，系统无法知道这条事务已经结束。
3. **不校验 prepare message 身份就接受客户端结论**：可能让错误 producerGroup 或错误 offset 篡改他人的待决事务。
4. **先删 half message 再尝试写正式消息**：一旦正式消息写失败，系统会同时丢失待决事实和最终事实。

## 7. 误解清单

- commit 不是修改 half message，而是重写正式消息。
- rollback 不是“忽略这条消息”，而是显式收束并删除待决事实。
- `fromTransactionCheck=true` 不是普通客户端提交，它代表由回查链触发的最终裁决。
- 准备消息校验失败时，Broker 不会盲信客户端请求。
- 本篇讲的是二阶段收束，不是事务回查本身。

## 8. 证据清单

- `broker/src/main/java/org/apache/rocketmq/broker/processor/EndTransactionProcessor.java:57`
- `broker/src/main/java/org/apache/rocketmq/broker/processor/EndTransactionProcessor.java:129`
- `broker/src/main/java/org/apache/rocketmq/broker/processor/EndTransactionProcessor.java:199`
- `broker/src/main/java/org/apache/rocketmq/broker/processor/EndTransactionProcessor.java:220`
- `broker/src/main/java/org/apache/rocketmq/broker/processor/EndTransactionProcessor.java:250`
- `broker/src/main/java/org/apache/rocketmq/broker/processor/EndTransactionProcessor.java:275`
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageServiceImpl.java:597`
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageServiceImpl.java:634`
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageServiceImpl.java:639`
- `broker/src/test/java/org/apache/rocketmq/broker/processor/EndTransactionProcessorTest.java:97`

## 9. 版本边界与字数预算

- 基线：RocketMQ `5.3.1`。
- 本篇聚焦 Broker 二阶段 commit/rollback 落地，不再展开回查线程与 Producer `checkLocalTransaction()`。
- 目标正文：8000~12000 字；核心拆解层覆盖入口校验、commit 重写、rollback 清理、拒绝场景与最终事实收束。

## 10. 本轮重写主线

1. 从“Broker 拿到最终结论以后，为什么还不能一句话结束”开场。
2. 先否定几种朴素方案：改标签、直接删、先删再写、不校验身份。
3. 解释 `EndTransactionProcessor` 怎样在 commit/rollback 前先校验 prepare message。
4. 解释 commit 如何通过 `endMessageTransaction()` + `sendFinalMessage()` 重建正式事实。
5. 解释 rollback 如何通过 `deletePrepareMessage()` 让待决事实彻底消失。
6. 收网：事务消息真正落稳，不在 half message 存在本身，而在二阶段把待决事实明确收束成“正式存在”或“正式消失”。