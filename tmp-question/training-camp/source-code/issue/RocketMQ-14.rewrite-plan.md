# RocketMQ-14 重写规划

> 题目：DLedgerCommitLog 为什么把 RocketMQ 拉进了 Raft 世界
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：解释 RocketMQ 为什么不再沿用传统 HA 的“Master 推字节、Slave 回 offset”，而是把消息追加交给 DLedger，以日志条目、Leader、Follower、quorum commit 和角色回调重组可靠性主链。

## 1. 读者困惑

- `DLedgerCommitLog` 为什么不是给 `CommitLog` 换一个文件实现，而是改变消息写入与确认语义？
- 一条消息从 Broker 写入以后，什么时候只是本地日志，什么时候已经达到 DLedger 的 committed position？
- Producer 为什么要等待 DLedger Future，而不是像传统 HA 那样等 Slave offset？
- DLedger 的 CommitLog 与 RocketMQ 的 ConsumeQueue/Reput 如何衔接？
- Leader/Follower 角色变化为什么会影响 Broker 是否还能接收写请求？

## 2. 一句话顿悟

**DLedgerCommitLog 把 RocketMQ 的消息追加从“Master 向 Slave 复制字节”改造成“Leader 向 Raft 日志追加条目并等待 quorum 结果”；RocketMQ 仍然把消息解析成 CommitLog 语义并交给 Reput，但可靠性确认已经从 HA offset 等待迁移到 DLedger 的提交 Future。**

## 3. 五要素卡片

### 读者问题

传统 Master/Slave 只有复制确认，没有共识提交和统一角色；DLedgerCommitLog 怎样把这两层接到 RocketMQ 的消息存储主链？

### 入口

- `DefaultMessageStore` 根据 `enableDLegerCommitLog` 选择 `DLedgerCommitLog`
- `DLedgerCommitLog` 构造函数把 group、peers、selfId、数据目录和 entry 大小装入 `DLedgerConfig`
- `DLedgerCommitLog.asyncPutMessage()` 序列化消息并调用 `dLedgerServer.handleAppend()`
- `BrokerController.initializeMessageStore()` 注册 `DLedgerRoleChangeHandler`

### 状态核心

- DLedger entry position 与 `DLedgerEntry.BODY_OFFSET`
- `DLedgerServer`、`MemberState` 的 Leader/Follower 角色
- `ledgerEndIndex`、`committedIndex`、`committedPos`
- `AppendFuture` / `AppendEntryResponse` / `DLedgerResponseCode`
- `DLedgerCommitLog.dividedCommitlogOffset` 与新旧 CommitLog 分界
- RocketMQ `AppendMessageResult`、queue offset 与 Reput dispatch offset

### 失败路径

- 非 Leader 写入：`NOT_LEADER` / `INCONSISTENT_LEADER` / `LEADER_NOT_READY`
- quorum 未在窗口内完成：`WAIT_QUORUM_ACK_TIMEOUT` 映射为 `IN_SYNC_REPLICAS_NOT_ENOUGH`
- 磁盘压力或 leader pending：`DISK_FULL`、`LEADER_PENDING_FULL`、`OS_PAGE_CACHE_BUSY`
- DLedger entry 已写但尚未 committed：Producer 不能把本地 entry 位置直接当成成功
- 恢复时 committed position、ConsumeQueue 与 DLedger 文件不一致：需要 recover、截断冗余 CQ 或按提交边界读取
- Leader/Follower 角色切换期间：Broker 等待日志追平和 dispatch 完成后再切换服务角色

### 连接点

- 前文 `RocketMQ-13`：传统 HA 的复制 offset/GroupCommitRequest 被 DLedger Future/quorum 语义替代
- 前文 `RocketMQ-4/5/6`：DLedger 仍提供可解析的物理消息数据，Reput 仍构建 ConsumeQueue/IndexFile
- 后文 `RocketMQ-15`：Controller 选主和角色切换会进一步接管 Broker 的控制面
- 后文 `RocketMQ-18`：故障恢复继续串联 committed position、可见性和重复投递边界

## 4. 总图

```text
Producer
  → Broker MessageStore 选择 DLedgerCommitLog
    → MessageSerializer 编码 RocketMQ 消息
      → DLedgerServer.handleAppend()
        → Leader 追加 entry 并复制到 quorum
          → AppendFuture 返回 DLedgerResponseCode
            → SUCCESS 才映射为 PUT_OK
              → DLedgerCommitLog 数据可被 Reput 解析
                → ConsumeQueue / IndexFile 派生
```

## 5. 关键边界

- 本篇聚焦 RocketMQ 5.3.1 中 `DLedgerCommitLog` 与 DLedger 存储 API 的桥接，不写成完整 DLedger 库源码教程。
- 只解释 Leader/Follower、entry、quorum、committed position 与 RocketMQ 存储层的连接；Controller 的独立选主与控制面放到 RQ-15。
- 不把 entry position、ledger end index、committed index、committed position 和 ConsumeQueue offset 混成一个 offset。
- 不把单节点测试的 `PUT_OK` 误写成多副本 quorum 已验证；测试证据必须区分单节点和多节点。

## 6. 失败方案推演

1. 继续沿用传统 Master/Slave：能复制，但角色与提交边界分散，无法用统一日志和 quorum 处理 Leader 失效。
2. `handleAppend()` 返回本地 entry position 就立即 `PUT_OK`：本地条目可能尚未复制/提交，Leader 故障后不能把它当成稳定事实。
3. DLedger 直接替代 RocketMQ 消息存储：会丢失 queue offset、消息格式、Reput 与消费索引桥，因此必须保留 `DLedgerCommitLog` 适配层。
4. 只看 DLedger committed index，不等 Reput：日志已提交不代表 ConsumeQueue 已经完成派生，消费可见性仍有自己的追赶阶段。

## 7. 误解清单

- DLedgerCommitLog 不是普通 CommitLog 的文件路径替换。
- DLedger 的 entry position 不是 RocketMQ 消息的 queue offset。
- Leader 本地 entry 追加不是 Producer 成功；成功由 Future 的响应码决定。
- quorum commit 不等于 ConsumeQueue 已经可读。
- DLedger 自己的角色变化与 RocketMQ Broker 对外角色服务切换不是同一个动作。

## 8. 证据清单

- `store/src/main/java/org/apache/rocketmq/store/DefaultMessageStore.java:227`
- `store/src/main/java/org/apache/rocketmq/store/dledger/DLedgerCommitLog.java:88`
- `store/src/main/java/org/apache/rocketmq/store/dledger/DLedgerCommitLog.java:137`
- `store/src/main/java/org/apache/rocketmq/store/dledger/DLedgerCommitLog.java:548`
- `store/src/main/java/org/apache/rocketmq/store/dledger/DLedgerCommitLog.java:620`
- `store/src/main/java/org/apache/rocketmq/store/dledger/DLedgerCommitLog.java:821`
- `broker/src/main/java/org/apache/rocketmq/broker/BrokerController.java:783`
- `broker/src/main/java/org/apache/rocketmq/broker/dledger/DLedgerRoleChangeHandler.java:56`
- `store/src/main/java/org/apache/rocketmq/store/dledger/DLedgerCommitLog.java:293`
- `store/src/test/java/org/apache/rocketmq/store/dledger/DLedgerCommitlogTest.java:120`

## 9. 版本边界与字数预算

- 基线：RocketMQ `5.3.1`，使用仓库内 DLedger 依赖的 API 语义。
- 本篇不展开 Controller、JRaftController、DLedgerController 的选主实现；只保留 Broker 角色回调作为边界证据。
- 目标正文：8000~12000 字；核心拆解层覆盖存储选择、消息序列化、entry append、Future 响应、提交/可见性和恢复边界。

## 10. 本轮重写主线

1. 从传统 Master/Slave 的“复制完成但提交与角色分散”切入。
2. 解释 `DefaultMessageStore` 为什么改选 `DLedgerCommitLog`，以及构造阶段如何建立 DLedger 配置。
3. 解释消息如何从 RocketMQ 对象编码成 DLedger entry，并由 `handleAppend()` 进入 Leader/quorum 链。
4. 解释为什么 `AppendFuture` 返回码才是 Producer 结果边界，逐个澄清非 Leader、quorum timeout 和磁盘压力。
5. 解释 committed position、Reput、ConsumeQueue 与 DLedger 文件如何衔接。
6. 解释恢复与角色变化的边界，为下一篇 Controller/选主留下入口。
