# RocketMQ-18 重写规划

> 题目：消息系统真正难的不是“能发出去”，而是失败以后怎样恢复 —— RocketMQ 故障恢复总串联
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：把前面的 CommitLog、Reput、传统 HA、DLedger、Controller/ReplicasManager 收成一条“故障以后系统怎样重新承认哪些消息、怎样截断错误视图、怎样恢复主角色与消费可见性”的总恢复链。

## 1. 读者困惑

- RocketMQ 真正难的为什么不是正常发送，而是故障后的恢复？
- Broker 异常退出、半消息、脏 ConsumeQueue、主切换后，系统凭什么知道哪些消息还能算数？
- 为什么恢复时总在围绕 `confirmOffset`、`processOffset`、`dispatchBehindBytes()`、`truncateDirtyLogicFiles()` 打转？
- Master/Slave、DLedger、Controller 模式下，恢复边界为什么不一样？
- 恢复完成以后，Producer 成功语义、Consumer 可见性和 Broker 主角色怎样重新闭环？

## 2. 一句话顿悟

**RocketMQ 的故障恢复，本质上是在重建“系统此刻还承认哪一段日志”为真相：先恢复 CommitLog，再用 confirm/committed 边界裁掉超前的 ConsumeQueue 与脏视图，再等 Reput 追平，最后在主从/Controller 角色稳定后把新的事实重新暴露给客户端。**

## 3. 五要素卡片

### 读者问题

消息已经发过、日志也写过，为什么故障恢复时还要重新扫描、截断、回放和等 dispatch 追平？

### 入口

- `DefaultMessageStore.recover(lastExitOK)`：总恢复入口
- `CommitLog.recoverNormally()` / `recoverAbnormally()`：传统 CommitLog 恢复
- `DLedgerCommitLog.recoverNormally()` / `recoverAbnormally()`：DLedger 恢复
- `DefaultMessageStore.doRecheckReputOffsetFromCq()`：启动期对 Reput/ConsumeQueue 再校正
- `ReplicasManager.changeToMaster()` / `changeToSlave()`、`AutoSwitchHAService.changeToMaster()`：主切换后的恢复收口

### 状态核心

- `lastExitOK`：正常退出还是异常退出
- `confirmOffset` / `committedPos` / `processOffset` / `maxPhyOffsetOfConsumeQueue`
- `dispatchBehindBytes()` 与 `reputFromOffset`
- `truncateDirtyLogicFiles()` 触发点
- `syncStateSet`、`masterEpoch`、`brokerRole`
- NameServer 路由重新注册后的对外主视图

### 失败路径

- CommitLog 尾部半消息或损坏：只承认最后完整消息前的边界
- ConsumeQueue/IndexFile 超前于可承认的物理日志：必须截断脏逻辑视图
- DLedger 恢复边界与旧 CommitLog/混合模式不一致：需要按有效物理位置重建分界
- 主切换后 Reput 尚未追平：角色虽然切了，但消费视图还不能算完全恢复
- Controller/AutoSwitch 角色已经变化但 NameServer 未重新注册：客户端仍看见旧主视图
- Producer 曾收到成功，但恢复后相关消息不在最终承认边界内：暴露“成功语义”和“稳定真相”并非同一时刻

### 连接点

- 前文 `RocketMQ-4/5/6`：CommitLog 真相层、Reput、ConsumeQueue 可见性
- 前文 `RocketMQ-13`：传统 HA 的副本确认与 `FLUSH_SLAVE_TIMEOUT`
- 前文 `RocketMQ-14`：DLedger 的 committed 边界与恢复位置
- 前文 `RocketMQ-15`：Controller/ReplicasManager 的角色切换与 NameServer 再暴露
- 后文 `RocketMQ-19~21`：事务消息恢复是另一条更高层的恢复链，不在本篇吞掉

## 4. 总图

```text
Broker 重启 / 主切换 / 异常退出
  → 恢复 CommitLog 有效物理边界
    → 校正 confirmOffset / committedPos / processOffset
      → truncateDirtyLogicFiles() 裁掉超前 ConsumeQueue
        → Reput 从有效边界继续追平
          → dispatchBehindBytes() 归零
            → 主从/Controller 角色稳定并重新注册 NameServer
              → Producer / Consumer 重新看到新的系统真相
```

## 5. 关键边界

- 本篇是恢复总串联，不再逐类重讲 Producer、HA、DLedger、Controller 的正常主链。
- 只讲“哪些状态被系统重新承认”，不展开事务消息的半消息恢复细节。
- 不把“Producer 曾收到成功”“日志文件里曾经存在过字节”“Consumer 现在还能读到”三者混成同一事实。
- 不把正常退出恢复、异常退出恢复、主切换恢复、混合 CommitLog/DLedger 恢复写成同一个分支。

## 6. 失败方案推演

1. 只要磁盘上有字节就都承认：会把半消息、脏尾巴和未确认日志重新暴露给系统。
2. 只恢复 CommitLog 不校正 ConsumeQueue：逻辑索引可能指向已经不被承认的物理日志。
3. 主切换后立即对外宣布恢复完成：如果 `dispatchBehindBytes()` 还没归零，消费视图可能仍落后。
4. 只看 Controller 角色变化，不重新注册 NameServer：客户端仍持有旧主路由。

## 7. 误解清单

- 正常发送成功不等于故障后一定仍被系统承认。
- CommitLog 恢复完成不等于 ConsumeQueue/IndexFile 已恢复完成。
- `confirmOffset` / `committedPos` / `processOffset` 不是同一个边界。
- `dispatchBehindBytes()==0` 是恢复闭环的重要信号，不是无关监控值。
- 主切换完成不等于客户端已经看见新主；还要经过 NameServer 路由再暴露。

## 8. 证据清单

- `store/src/main/java/org/apache/rocketmq/store/DefaultMessageStore.java:1890`
- `store/src/main/java/org/apache/rocketmq/store/DefaultMessageStore.java:436`
- `store/src/main/java/org/apache/rocketmq/store/CommitLog.java:321`
- `store/src/main/java/org/apache/rocketmq/store/CommitLog.java:695`
- `store/src/main/java/org/apache/rocketmq/store/dledger/DLedgerCommitLog.java:293`
- `store/src/main/java/org/apache/rocketmq/store/ha/autoswitch/AutoSwitchHAService.java:143`
- `broker/src/main/java/org/apache/rocketmq/broker/controller/ReplicasManager.java:237`
- `broker/src/main/java/org/apache/rocketmq/broker/controller/ReplicasManager.java:321`
- `store/src/test/java/org/apache/rocketmq/store/dledger/MixCommitlogTest.java:35`
- `store/src/test/java/org/apache/rocketmq/store/dledger/DLedgerCommitlogTest.java:120`

## 9. 版本边界与字数预算

- 基线：RocketMQ `5.3.1`。
- 本篇聚焦普通消息存储/复制/角色恢复，不展开事务消息的半消息回查恢复。
- 目标正文：9000~13000 字；核心拆解层覆盖恢复入口、物理边界承认、逻辑索引截断、Reput 追平、主切换与路由再暴露。

## 10. 本轮重写主线

1. 从“真正难的是失败以后还认哪些消息”为问题开场。
2. 解释 `DefaultMessageStore.recover()` 怎样把恢复分成 CommitLog、ConsumeQueue、offsetTable 三层。
3. 解释传统 CommitLog 的正常/异常恢复，以及 `confirmOffset` 如何约束可承认边界。
4. 解释 DLedger/混合 CommitLog 的恢复边界与 `truncateDirtyLogicFiles()`。
5. 解释 `doRecheckReputOffsetFromCq()`、`dispatchBehindBytes()` 和消费视图追平。
6. 解释主切换/Controller/NameServer 怎样把恢复后的新真相重新暴露给外界。
7. 收网：RocketMQ 恢复的核心不是“把服务重新拉起”，而是“重新承认一段一致的日志真相”。