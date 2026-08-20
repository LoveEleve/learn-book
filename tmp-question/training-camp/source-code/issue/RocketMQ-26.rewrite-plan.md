# RocketMQ-26 重写规划

> 题目：DLedger 与 Controller 为什么不是一回事——数据面共识与控制面裁决对照
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：把 `DLedgerCommitLog` 与 `Controller/ReplicasManager` 并排对照，解释 RocketMQ 5.x 为什么必须同时拥有“日志复制/提交的数据面”与“主角色裁决/注册/通知的控制面”，并明确它们各自解决什么问题、在哪一层交汇。

## 1. 读者困惑

- DLedger 都已经有 Leader/Follower 了，为什么还要 Controller 再选主一次？
- DLedger leader、Controller leader、Broker master 这三个“主”到底各自回答什么问题？
- 为什么 DLedger 负责 committed 边界，Controller 却负责 brokerId、syncStateSet、masterEpoch 和角色通知？
- 如果只有 DLedger 没有 Controller，会先在哪断掉？反过来如果只有 Controller 没有 DLedger，又会先在哪失真？
- 数据面和控制面在 RocketMQ 5.x 里到底怎样交汇，而不是彼此重复？

## 2. 一句话顿悟

**DLedger 解决的是“这段日志有没有被复制并提交”，Controller 解决的是“这组副本里谁还能继续代表系统对外写”；前者给出消息真相边界，后者给出角色与路由真相边界，RocketMQ 5.x 的可靠性闭环正是建立在这两层分工之上。**

## 3. 五要素卡片

### 读者问题

为什么 RocketMQ 5.x 不把所有一致性问题都塞给 DLedger，而要把日志共识和角色裁决拆成数据面与控制面两层？

### 入口

- `DefaultMessageStore` 选择 `DLedgerCommitLog`
- `DLedgerCommitLog.asyncPutMessage()`：数据面追加与提交入口
- `ControllerManager.initialize()`：控制面服务入口
- `ReplicasManager.start()` / `changeToMaster()` / `changeToSlave()`：Broker 侧消费控制面结果

### 状态核心

- DLedger：entry position、ledger end index、committed index、committedPos
- Controller：controller leader、masterBrokerId、masterEpoch、syncStateSet、syncStateSetEpoch
- Broker：`BrokerRole`、`brokerId`、`masterAddress`
- NameServer：对外暴露的最终主从路由视图

### 失败路径

- 只有 DLedger：日志能提交，但没人统一裁决 broker-set 主角色和对外路由
- 只有 Controller：能裁角色，但没有统一日志复制与 committed 边界支撑消息真相
- 把 DLedger leader 直接当 Broker master：数据面和控制面真相混淆
- 角色切换不等待 DLedger 追平：Broker 对外角色可能快于日志真相边界
- Controller 选主后不落到 ReplicasManager/NameServer：外界看不见新的主视图

### 连接点

- 前文 `RocketMQ-14`：DLedger 数据面
- 前文 `RocketMQ-15`：Controller 控制面
- 前文 `RocketMQ-18`：故障恢复总串联正是两层交汇后的结果
- 本篇作为对照篇，不新增新主链，而是统一收束这两层分工

## 4. 总图

```text
消息发送
  → DLedgerCommitLog 复制并提交日志
    → committedPos 给出数据真相边界

Broker 存活变化
  → Controller electMaster / notifyBrokerRoleChanged
    → ReplicasManager 落角色并注册 NameServer
      → 对外主视图更新

两层交汇：Broker 只有在数据面可承认、控制面也裁定后，才真正“还能继续写”
```

## 5. 关键边界

- 本篇是对照与收束，不重新逐行展开 DLedger 或 Controller 内部实现。
- 不把 DLedger leader、Controller leader、Broker master 混成一个主节点概念。
- 不把 committedPos 和 masterEpoch / syncStateSet 写成同一种边界；一个回答数据真相，一个回答角色真相。
- 不把 NameServer 当成控制面裁决者；它是最终路由暴露层。

## 6. 失败方案推演

1. **只有 DLedger 就够了**：忽略 brokerId 分配、外部路由视图和角色通知。
2. **只有 Controller 就够了**：忽略消息复制与已提交日志边界，角色裁决失去数据真相支撑。
3. **把 DLedger leader 直接等于 Broker master**：会跳过控制面落角色与 NameServer 再暴露。
4. **数据面和控制面各跑各的，不做收口**：Producer/Consumer 看到的世界会分裂。

## 7. 误解清单

- DLedger 不是 RocketMQ 5.x 的全部一致性。
- Controller 不是重复造一个 DLedger；它解的是另一类问题。
- committedPos 不等于 master 身份。
- Broker master 也不是纯控制面布尔值；它必须建立在数据面与控制面同时到位之后。
- NameServer 看见的新主视图是落地结果，不是裁决来源。

## 8. 证据清单

- `store/src/main/java/org/apache/rocketmq/store/dledger/DLedgerCommitLog.java:548`
- `store/src/main/java/org/apache/rocketmq/store/dledger/DLedgerCommitLog.java:620`
- `controller/src/main/java/org/apache/rocketmq/controller/ControllerManager.java:102`
- `controller/src/main/java/org/apache/rocketmq/controller/ControllerManager.java:146`
- `broker/src/main/java/org/apache/rocketmq/broker/controller/ReplicasManager.java:225`
- `broker/src/main/java/org/apache/rocketmq/broker/controller/ReplicasManager.java:321`

## 9. 版本边界与字数预算

- 基线：RocketMQ `5.3.1`。
- 本篇是对照篇，不单独展开 JRaftController、proxy 路由或 NameServer 内部 RouteInfoManager。
- 目标正文：6000~10000 字；重点做概念拆分、失败方案对照和两层交汇收口。

## 10. 本轮重写主线

1. 从“为什么有了 DLedger 还不够”开场。
2. 对照 DLedger 回答的问题与 Controller 回答的问题。
3. 解释三种“主”分别是什么。
4. 解释两层怎样在 ReplicasManager 与 NameServer 再暴露处交汇。
5. 收网：RocketMQ 5.x 的可靠性闭环来自数据真相边界 + 角色真相边界。