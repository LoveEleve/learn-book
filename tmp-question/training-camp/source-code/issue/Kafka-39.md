# Kafka-39. Broker 启动时发生了什么——KafkaRaftServer 启动流程与组件装配主链

> 场景：前面 38 篇把 Kafka 的各个子系统讲得很细，但一直缺少一个“从 kafka-server-start.sh 到各个组件就绪”的全局视角。本篇补上这个缺口：Kafka v4（KRaft）下一个 broker/controller 节点从启动入口到大组件装配完成，经历了哪些对象和顺序。这也对应 zsxq【服务端_Broker_源码分析系列第十六篇】的主题。

## 先把真正的困惑摆出来：启动一个 broker 到底发生了什么

当执行 `kafka-server-start.sh` 时，你会看到一大堆日志，最后打印 `Kafka Server started`。但在这之前，进程经历了一个非常讲究顺序的装配过程：日志目录校验、元数据 bootstrap、SharedServer、按 roles 构建 controller/broker、再逐层装配 SocketServer、LogManager、ReplicaManager……

本篇沿着这个启动顺序走一遍，回答“为什么 controller 必须先启动”这类问题。

## 第一层：入口与角色——KafkaRaftServer 按 process.roles 决定做什么

`KafkaRaftServer` 是 KRaft 模式的顶层入口（`KafkaRaftServer.scala:47`）。它在构造阶段就做了几件事：

1. `initializeLogDirs(config, ...)`：读取/校验日志目录，生成 `metaPropsEnsemble`（含 clusterId 等）与 `bootstrapMetadata`；
2. 初始化 metrics；
3. 创建 `SharedServer`；
4. 按 `config.processRoles.contains(ProcessRole.BrokerRole)` 决定是否创建 `BrokerServer`；
5. 按 `config.processRoles.contains(ProcessRole.ControllerRole)` 决定是否创建 `ControllerServer`。

```text
process.roles=controller   → 只创建 ControllerServer
process.roles=broker       → 只创建 BrokerServer
process.roles=broker,controller → 两者都创建（combined mode）
```

所以 Kafka 不再是“只有一个 BrokerServer”的模型：用 `process.roles` 区分数据面 broker 和控制面 controller，可合可分。

## 第二层：SharedServer——双方共享的共识与元数据底座

`SharedServer` 不是 broker 专用的东西，而是 controller 和 broker 共同依赖的底层共享组件。

它把这套底座的构造统一放在一个地方：RaftClient、元数据日志、controller 通道、元数据 image 相关能力等。这样无论你是 controller 还是 broker，都能拿到同一个 KRaft 共识/元数据上下文；但像 `Partition` / `ReplicaManager` 这类数据面对象，仍然主要在 broker 侧单独装配。

这也是为什么 `KafkaRaftServer` 先建 SharedServer、再分别建 broker / controller：两者都要从同一个共享底座上“拔出”自己的那一部分服务。

## 第三层：为什么 controller 必须先于 broker 启动

`KafkaRaftServer.startup()` 的注释写得很清楚：

```scala
controller.foreach(_.startup())
broker.foreach(_.startup())
```

原因是 combined mode 下，broker 启动时需要用到控制面的 coordinator / 元数据通道，而 controller 启动时会先把 controller endpoint 等就绪信息传给 KRaft manager。如果 broker 先启动，它引用的控制器相关组件可能还是空的。

```text
SharedServer 就绪
  → ControllerServer.startup()
    → 端点/协调器就绪
      → BrokerServer.startup()
        → 引用已就绪的控制面
```

这也是一个很有意思的点：**roles 的分类不只是“启动哪个”，还决定了“谁先启动”。**

## 第四层：BrokerServer.startup()——组件装配的依赖序

`BrokerServer.startup()`（`BrokerServer.scala:189`）承担数据面 broker 的大装配。它按依赖顺序初始化并连接一大堆组件，核心可归纳为：

- **网络 / 请求入口**：`SocketServer`、`KafkaRequestHandlerPool`
- **磁盘/存储**：`LogManager`、`LogDirFailureChannel`
- **数据面协调**：`ReplicaManager`、`AlterPartitionManager`
- **元数据**：`MetadataCache`
- **协调器**：`GroupCoordinator`、事务协调器
- **生命周期**：`BrokerLifecycleManager`

`KafkaBroker` trait 把这些对象全部暴露成抽象成员，也正是这一点让“KafkaBroker 是谁”在抽象层只有一个统一接口，真正实现则在 BrokerServer / ControllerServer 里。

关键点在于：**不是简单地把所有组件 new 出来，而是要求它们在正确的依赖顺序下互相 wiring。** 每个 manager 的构造参数往往就是上一批已就绪的组件。

## 第五层：BrokerLifecycleManager 与就绪判定

`BrokerLifecycleManager` 负责 broker 会话 / 心跳，以及 broker 状态在 `NOT_RUNNING → STARTING → RECOVERY → RUNNING` 等之间的迁移。

它要等：

- 元数据 metedata 追到合适位置；
- broker 能向 controller 注册并维持心跳；
- 会话建立后，broker 才算真正“可用”。

这也是为什么打印完 `Kafka Server started` 不代表“立即可服务”，而只代表“所有 startup() 已返回、组件已装配”。broker 是否真的能接流量，还取决于 lifecycle manager 走完状态迁移。

## 收网：启动就是把多分层组件按依赖串起来

把整篇压成一句话：Kafka v4 通过 `KafkaRaftServer` 先初始化日志目录与元数据，构造共享的 `SharedServer`，再按 `process.roles` 决定创建 controller/broker，且 controller 先启动；`BrokerServer.startup()` 随后按依赖顺序装配网络层、存储、副本管理、元数据缓存、协调器等，最后由 `BrokerLifecycleManager` 推动 broker 进入可服务状态。

```text
KafkaRaftServer
  → initializeLogDirs
    → SharedServer
      → roles → controller / broker
        → controller first
          → BrokerServer.startup()
            → SocketServer → LogManager → ReplicaManager → MetadataCache → Coordinators
              → BrokerLifecycleManager → RUNNING
```

到这里，主线只发生了五件事。

第一，`KafkaRaftServer` 是 KRaft 模式顶层入口。

第二，`process.roles` 决定创建 controller / broker / combined。

第三，`SharedServer` 是双方共享的共识与元数据底座。

第四，controller 必须先于 broker 启动。

第五，`BrokerServer.startup()` 按依赖装配多个管理器，`BrokerLifecycleManager` 才把 broker 推入可服务状态。

**本篇的一句话困惑**：启动一个 Kafka broker，从脚本到组件就绪发生了什么？

**本篇的一句话顿悟**：KafkaRaftServer 先初始化目录与元数据，建 SharedServer，按 process.roles 构建 controller/broker（controller 先），再由 BrokerServer.startup 按依赖装配各管理器，最后 BrokerLifecycleManager 把它推入 RUNNING。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“Kafka 只有 BrokerServer。”** 按 `process.roles` 还可启动 ControllerServer 或 combined。
2. **“controller / broker 启动顺序无所谓。”** 必须 controller 先，broker 后。
3. **“SharedServer 是 broker 的。”** 它是 controller 与 broker 共享的共识/元数据底座。
4. **“BrokerServer.startup 只启动 SocketServer。”** 它装配 LogManager、ReplicaManager、Coordinators 等。
5. **“打印 started 就代表可服务。”** 还要等 BrokerLifecycleManager 完成状态迁移。

### 关键证据清单

- `core/src/main/scala/kafka/server/KafkaRaftServer.scala:47`：类与构造。
- `core/src/main/scala/kafka/server/KafkaRaftServer.scala:54`：initializeLogDirs。
- `core/src/main/scala/kafka/server/KafkaRaftServer.scala:63`：SharedServer。
- `core/src/main/scala/kafka/server/KafkaRaftServer.scala:74`：按 roles 建 Broker/Controller。
- `core/src/main/scala/kafka/server/KafkaRaftServer.scala:90`：startup（controller first）。
- `core/src/main/scala/kafka/server/BrokerServer.scala:189`：BrokerServer.startup()。
- `core/src/main/scala/kafka/server/KafkaBroker.scala:76`：KafkaBroker trait 组件列表。

### 版本与实现边界

- 本文以 Kafka v4.x KRaft 为基线。
- 本篇只抓启动装配主线，不展开 Raft / Controller 内部实现。
- 不把 `process.roles` 三种组合写成“总是两者都启动”。
- 不重复 SocketServer 三层线程（Kafka-13）。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-13/14`（网络层）、`Kafka-19/24`（存储与元数据）、`Kafka-8/26/32`（KRaft）。
- 后续桥接：下一篇可进入 Assignor 深讲（Kafka-40），或 MetadataCache 路由（Kafka-41）。