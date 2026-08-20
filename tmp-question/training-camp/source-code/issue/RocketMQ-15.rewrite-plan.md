# RocketMQ-15 重写规划

> 题目：Controller、选主和角色切换怎样决定 Broker 谁能继续写
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：解释 RocketMQ 5.x 为什么在 DLedger 数据面之外还要引入 Controller 控制面，以及 Controller 怎样通过 broker 注册、心跳、选主、角色通知和 ReplicasManager 把“哪台 Broker 能继续写”收成一条闭环。

## 1. 读者困惑

- DLedger 已经有 Leader/Follower 了，为什么 RocketMQ 还要单独做 Controller？
- Broker 重启或新副本加入时，谁给它 brokerId、masterId 和当前 syncStateSet？
- 一台 Broker 掉线以后，新的 Master 到底由谁选，依据是什么？
- Controller 选出结果以后，Broker 怎样真正切到 Master/Slave，并对 NameServer 暴露新角色？
- 为什么“数据日志能提交”和“Broker 能继续对外写”不是同一件事？

## 2. 一句话顿悟

**DLedger 解决的是日志复制与提交，Controller 解决的是副本集合、主节点裁决和角色通知；真正决定“谁还能继续写”的，不是某个 Broker 自己觉得自己是 Leader，而是 Controller 按元数据和心跳选出 Master，再由 Broker 侧 `ReplicasManager`/角色切换链把这个结果落成对外服务状态。**

## 3. 五要素卡片

### 读者问题

为什么 DLedger 有共识日志之后，RocketMQ 仍然需要一个独立 Controller 才能把 Broker 角色、注册和选主闭环？

### 入口

- `ControllerManager.initialize()` 选择 `DLedgerController` 或 `JRaftController`
- `ControllerManager.registerProcessor()` 暴露注册、心跳、选主、同步集等控制面请求
- Broker 侧 `ReplicasManager.start()` 启动 controller 地址同步、注册和元数据同步
- `ReplicasManager.brokerElect()`、`changeToMaster()`、`changeToSlave()` 把 Controller 结果落到 Broker

### 状态核心

- Controller leader 状态与 `ControllerManager.onBrokerInactive()` 触发的选主流程
- broker 集合元数据：brokerId、masterBrokerId、masterEpoch、syncStateSet、syncStateSetEpoch
- `ReplicasManager.state` / `registerState` / `masterAddress` / `masterBrokerId`
- `DefaultElectPolicy` 的 `(epoch, maxOffset, electionPriority)` 候选排序
- 角色通知 `RoleChangeNotifyEntry` 与 Broker 本地 `BrokerRole`
- `AutoSwitchHAService` 的主从切换与 syncStateSet

### 失败路径

- Controller 自己不是 leader：只接受有限视图，不负责真正裁决主节点
- Broker 未完成注册或 metadata 不合法：拿不到 brokerId，无法进入正常运行态
- master 心跳失活：Controller 触发重选，但若没有合格副本可能选主失败
- Broker 首次注册完成但尚无明确 master：需要主动 `brokerElect()` 拉起主视图建立
- 候选副本活着但数据/epoch 较落后：可能失去优先级，或在退化到 `allReplicaBrokers` 后只能触发非理想选举
- Broker 收到新 master 结果但本地服务未切换完成：不能立即暴露可写能力
- 角色切换后未重新注册 NameServer：外部路由仍可能看到旧角色视图

### 连接点

- 前文 `RocketMQ-14`：DLedger 提供数据面日志复制与 committed 边界，但不直接等于 Broker 对外可写身份
- 前文 `RocketMQ-2/3`：Broker 注册与路由视图最终还要回到 NameServer/客户端世界
- 后文 `RocketMQ-18`：故障恢复总串联会继续把主切换、可见性和重复投递边界收束
- 对照 `Seata`/`SofaJRaft`：共识层与控制层分离是分布式系统常见结构，但这里只作为导航，不当正文主线

## 4. 总图

```text
Broker 启动
  → ReplicasManager 同步 controller 地址
    → 申请/注册 brokerId 与副本元数据
      → 周期心跳上报 epoch/maxOffset/confirmOffset
        → Controller 监测失活并触发 electMaster
          → DefaultElectPolicy 选出新 master
            → Controller 通知 broker 角色变化
              → ReplicasManager / AutoSwitchHAService 切换 Master/Slave
                → 重新注册 NameServer，对外暴露谁还能继续写
```

## 5. 关键边界

- 本篇主题是 **控制面裁决与角色落地**，不是重讲 DLedger 日志复制。
- 只讲 `ControllerManager`/`DLedgerController`/`ReplicasManager` 主链；JRaftController 仅作为“另一种 controller 实现”边界提示。
- 不把 DLedger leader、Controller leader、Broker master 三个角色混成一个“主节点”。
- 不把 `syncStateSet`、`allReplicaBrokers`、NameServer 路由视图当成同一份集合。

## 6. 失败方案推演

1. **让每个 Broker 自己依据本地 DLedger 角色对外宣布我是 Master**：最直觉，但会把控制面裁决拆散，外部世界无法获得统一主视图。
2. **只靠 NameServer 存主从信息**：NameServer 不维护这套选主与副本元数据状态机，无法承担角色裁决。
3. **Controller 只选主，不通知 Broker 落角色**：会出现“控制面已经变更，Broker 服务状态和 NameServer 路由还停留在旧视图”的断裂。
4. **谁活着就直接选谁**：忽略 epoch、offset、priority 和 syncStateSet，会把落后的副本随意扶正。

## 7. 误解清单

- DLedger leader 不等于 Broker 一定已经是可写 Master。
- Controller leader 也不等于业务 Broker master；它只是裁决者。
- `syncStateSet` 不是全部副本集合，更不是客户端路由视图。
- 选出 master 结果以后，Broker 还要切服务、改角色、再注册 NameServer。
- Controller 不只是“心跳转发器”，它维护的是副本元数据和选主裁决。

## 8. 证据清单

- `controller/src/main/java/org/apache/rocketmq/controller/ControllerManager.java:102`
- `controller/src/main/java/org/apache/rocketmq/controller/ControllerManager.java:146`
- `controller/src/main/java/org/apache/rocketmq/controller/ControllerManager.java:211`
- `controller/src/main/java/org/apache/rocketmq/controller/impl/DLedgerController.java:187`
- `controller/src/main/java/org/apache/rocketmq/controller/elect/impl/DefaultElectPolicy.java:38`
- `broker/src/main/java/org/apache/rocketmq/broker/controller/ReplicasManager.java:137`
- `broker/src/main/java/org/apache/rocketmq/broker/controller/ReplicasManager.java:225`
- `broker/src/main/java/org/apache/rocketmq/broker/controller/ReplicasManager.java:375`
- `broker/src/main/java/org/apache/rocketmq/broker/controller/ReplicasManager.java:401`
- `controller/src/test/java/org/apache/rocketmq/controller/ControllerManagerTest.java:184`

## 9. 版本边界与字数预算

- 基线：RocketMQ `5.3.1`。
- 本篇聚焦默认 `DLedgerController` 路线下的控制面；`JRaftController` 只作为结构边界，不深入实现。
- 目标正文：8000~12000 字；核心拆解层覆盖 controller 初始化、broker 注册、心跳失活、选主策略、角色通知与 Broker 落地。

## 10. 本轮重写主线

1. 从“DLedger 已经有 leader，为什么还不够”切入。
2. 解释 Controller 与 Broker 各自维护什么元数据，为什么需要独立控制面。
3. 解释 Broker 启动后怎样通过 `ReplicasManager` 完成注册、申请 brokerId、拿 master 视图。
4. 解释失活检测与 `electMaster` 主链，重点讲 `DefaultElectPolicy` 的排序依据。
5. 解释角色通知怎样落到 `changeToMaster` / `changeToSlave`，并重新注册 NameServer。
6. 收网：真正决定“谁还能继续写”的，是 Controller 裁决 + Broker 落地，不是单个 DLedger leader 自证。