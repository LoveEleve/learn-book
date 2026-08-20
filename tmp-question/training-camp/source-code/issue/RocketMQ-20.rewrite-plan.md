# RocketMQ-20 重写规划

> 题目：Producer 本地事务执行完了，Broker 为什么还要回查
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：解释 RocketMQ 明明已经让 Producer 执行了本地事务，为什么 Broker 仍要在 half message 上做事务状态回查；把问题收敛到“Broker 不能盲信客户端一次性回执，而要为未知、超时、丢失和失活场景保留主动确认能力”。

## 1. 读者困惑

- Producer 本地事务不是已经执行了吗，Broker 为什么不直接相信客户端那次 `endTransaction()`？
- half message 都已经存在了，为什么还需要专门的检查线程和 `CHECK_TRANSACTION_STATE` 请求？
- 哪些情况下会让事务状态从 Broker 视角重新变回“未知”？
- 回查到底查的是什么：查 half message、查 Broker 状态，还是查 Producer 本地事务结果？
- 为什么回查是事务消息正确性的必要补层，而不是多余保险？

## 2. 一句话顿悟

**RocketMQ 不能把事务正确性押在 Producer 一次性的 `endTransaction()` 上，因为在本地事务结果与 Broker 最终看到的结果之间，仍然可能插入崩溃、超时、丢包和未知状态；所以 Broker 必须围绕 half message 保留主动回查能力，再次向 Producer 确认本地事务究竟是 commit、rollback 还是仍然未知。**

## 3. 五要素卡片

### 读者问题

既然客户端已经执行过本地事务，为什么 Broker 还要维护一套主动检查 half message 的链路？

### 入口

- `TransactionalMessageCheckService.run()/onWaitEnd()`：Broker 周期性触发事务检查
- `TransactionalMessageService.check(...)`：遍历 half queue 的主入口
- `AbstractTransactionalMessageCheckListener.sendCheckMessage()`：把 half message 改回真实 Topic/Queue 语义并发起检查
- 客户端 `ClientRemotingProcessor.checkTransactionState()` → `DefaultMQProducerImpl.checkTransactionState()`：Producer 侧接收回查并执行 `checkLocalTransaction()`

### 状态核心

- half message 当前 offset、op queue offset、removeMap
- `LocalTransactionState`：COMMIT / ROLLBACK / UNKNOW
- `PROPERTY_TRANSACTION_CHECK_TIMES`
- `PROPERTY_CHECK_IMMUNITY_TIME_IN_SECONDS`
- Producer group → available channel 的映射
- `fromTransactionCheck=true` 的 `EndTransactionRequestHeader`

### 失败路径

- Producer 本地事务执行后没来得及 `endTransaction()` 就崩溃
- 网络/超时导致 Broker 没看到客户端的最终 commit/rollback 回执
- Producer 回答 `UNKNOW`：Broker 不能擅自把待决消息转正或回滚
- Producer group 对应 channel 不可用：Broker 无法立即回查
- half message 被检查过多次仍未知：进入 discard/兜底逻辑
- 自定义免检时间未过：Broker 不能过早打扰本地事务尚未完成的 Producer

### 连接点

- 前文 `RocketMQ-19`：half message 已经提供“待决事实层”，本篇围绕它做状态再次确认
- 后文 `RocketMQ-21`：Broker 拿到最终 commit/rollback 结论后怎样把半消息真正落稳
- 前文 `RocketMQ-18`：事务回查本质上也是恢复语义的一部分——不能让待决事实长期悬空

## 4. 总图

```text
half message 留在 RMQ_SYS_TRANS_HALF_TOPIC
  → Broker 周期扫描 half queue
    → 发现消息仍未被明确 commit/rollback
      → 构造 CHECK_TRANSACTION_STATE 请求
        → Producer 执行 checkLocalTransaction()
          → 返回 COMMIT / ROLLBACK / UNKNOW
            → Broker 再决定是否进入下一步 endTransaction
```

## 5. 关键边界

- 本篇只讲“为什么要回查、回查链怎么形成”，不展开 commit/rollback 真正如何转正或删除 half message。
- 不把回查写成“重复执行本地事务”；Producer 侧执行的是状态确认，不是再次做业务动作。
- 不把 `UNKNOW` 当成失败；它表示当前仍不能下结论。
- 不把事务检查链等同于普通 Consumer 拉取链；它是 Broker 主动触发的事务控制链。

## 6. 失败方案推演

1. **只要 Producer 当时执行过本地事务，就永远相信那次回执**：一旦回执丢失或 Producer 崩溃，Broker 就再也无法知道最终结论。
2. **Broker 自己根据 half message 年龄推断 commit/rollback**：没有本地事务结果证据，容易把待决消息错误转正或回滚。
3. **无限等待，不做回查**：half message 会无限悬挂，事务消息世界失去收敛能力。
4. **回查时再次执行本地事务**：会把幂等和业务副作用推回客户端，语义更糟。

## 7. 误解清单

- 回查不是对事务的“不信任”，而是对单次回执链路的不信任。
- `UNKNOW` 不是立即失败，也不是立即回滚。
- 回查不是普通消费，它是 Broker 主动发起的事务控制请求。
- Producer 侧 `checkLocalTransaction()` 是状态确认，不是重做业务事务。
- 本篇只讲回查必要性和链路，不讲最终 commit/rollback 存储落点。

## 8. 证据清单

- `broker/src/main/java/org/apache/rocketmq/broker/transaction/TransactionalMessageCheckService.java:43`
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/TransactionalMessageService.java:68`
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageServiceImpl.java:162`
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/AbstractTransactionalMessageCheckListener.java:51`
- `client/src/main/java/org/apache/rocketmq/client/impl/ClientRemotingProcessor.java:100`
- `client/src/main/java/org/apache/rocketmq/client/impl/producer/DefaultMQProducerImpl.java:360`
- `broker/src/test/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageServiceImplTest.java:131`
- `broker/src/test/java/org/apache/rocketmq/broker/transaction/queue/DefaultTransactionalMessageCheckListenerTest.java:70`

## 9. 版本边界与字数预算

- 基线：RocketMQ `5.3.1`。
- 本篇聚焦事务状态回查，不展开最终 commit/rollback 消息重投与删除逻辑。
- 目标正文：8000~12000 字；核心拆解层覆盖单次回执为什么不够、half queue 检查链、Producer 侧 check 回调、未知/超时/丢失边界。

## 10. 本轮重写主线

1. 从“本地事务都执行了，为什么还要回查”开场。
2. 先否定几种朴素方案：完全信任一次回执、Broker 自己猜、无限挂起、重做本地事务。
3. 解释 Broker 侧 `TransactionalMessageCheckService -> TransactionalMessageService.check()` 的扫描链。
4. 解释 `sendCheckMessage()` 怎样把 half message 改回真实 Topic/Queue 语义发给 Producer。
5. 解释客户端 `checkLocalTransaction()` 的角色，以及 `UNKNOW`/checkTimes/immunityTime 的边界。
6. 收网：回查是为了让待决 half fact 最终收敛，下一篇再讲 Broker 怎样把最终结果真正落稳。