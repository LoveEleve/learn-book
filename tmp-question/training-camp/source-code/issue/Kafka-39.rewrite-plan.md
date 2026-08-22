# Kafka-39 重写规划

> 题目：Broker 启动时发生了什么——KafkaRaftServer 启动流程与组件装配主链
> 状态：补充篇，覆盖 zsxq【服务端_Broker_源码分析系列第十六篇】启动流程主题。按 Kafka v4.x KRaft 展开 Broker 启动、config/process.roles、sharedServer、controller-first-startup、组件装配。

## 1. 读者困惑

- Kafka 启动入口在哪？`kafka-server-start.sh` 到底调到谁？
- KRaft 模式下 controller 和 broker 怎么区分（process.roles）？
- 为什么 controller 必须在 broker 之前启动？
- SharedServer 是什么，分别共享给 controller/broker 什么？
- BrokerServer.startup() 里组件的装配顺序和依赖是什么？
- “Kafka Server started” 什么时候打印？

## 2. 一句话顿悟

**Kafka v4 的启动入口是 `KafkaRaftServer`：先初始化日志目录与集群元数据，构造共享的 `SharedServer`，然后按 `process.roles` 决定创建 ControllerServer 还是 BrokerServer；启动时 controller 先于 broker，BrokerServer.startup() 再按依赖顺序装配 SocketServer、LogManager、ReplicaManager、MetadataCache、GroupCoordinator 等组件。**

## 3. 五要素卡片

### 读者问题

启动一个 Kafka broker 时，从脚本到组件就绪，中间经历了哪些对象和顺序？

### 入口

- `KafkaRaftServer`
- `SharedServer`
- `BrokerServer`
- `ControllerServer`
- `BrokerServer.startup()`
- `ProcessRole`（process.roles）

### 状态核心

- `process.roles`：broker / controller / broker+controller
- `metaPropsEnsemble` / `bootstrapMetadata`：日志目录与初识元数据
- `SharedServer`：两个 server 共享的共识/元数据底座
- `BrokerLifecycleManager`：broker 会话/心跳与状态迁移

### 失败路径

- broker 先于 controller 启动（combined 模式时 raft 未就绪）→ 元数据不可用
- 日志目录无有效 meta.properties → 启动失败
- 组件依赖顺序错误 → 引用空组件

### 连接点

- 前文 `Kafka-8/26/32`：KRaft 元数据与 Raft。
- 前文 `Kafka-13/14`：SocketServer / 网络层。
- 前文 `Kafka-19/24`：LogManager 与 metadata 状态。

## 4. 总图

```text
kafka-server-start.sh
  → KafkaRaftServer(config, time)
    → initializeLogDirs：metaProps + bootstrapMetadata
      → SharedServer
        → process.roles
          → ControllerServer（先启动）
          → BrokerServer（后启动）
            → BrokerServer.startup()
              → 按依赖装配组件
                → "Kafka Server started"
```

## 5. 关键边界

- 本篇只讲启动装配主线，不展开 Raft/Controller 内部实现。
- 不把 `process.roles` 三种组合写成“总是两个都要启动”。
- 不把 SharedServer 写成“broker 独有”。
- 不重复 SocketServer 三层线程（Kafka-13）。

## 6. 失败方案推演

1. broker 与 controller 同时乱序启动 → 部分依赖未就绪。
2. 不校验 meta.properties 就直接装配 → 启动后才发现元数据不可用。
3. 把所有组件都放在共享层 → broker/controller 职责混乱。

## 7. 误解清单

- “Kafka 只有 BrokerServer。”：按 roles 还可启动 ControllerServer 或 combined。
- “controller/broker 启动顺序无所谓。”：必须 controller 先。
- “SharedServer 是 broker 的。”：它是二者共享的共识与元数据底座。
- “BrokerServer.startup 只启动 SocketServer。”：要按依赖装配多个管理器。
- “STARTED_MESSAGE 一打印就代表全部就绪。”：只是所有 startup() 返回后的日志。

## 8. 证据清单

- `core/src/main/scala/kafka/server/KafkaRaftServer.scala:47`：类与构造。
- `core/src/main/scala/kafka/server/KafkaRaftServer.scala:54`：initializeLogDirs。
- `core/src/main/scala/kafka/server/KafkaRaftServer.scala:63`：SharedServer。
- `core/src/main/scala/kafka/server/KafkaRaftServer.scala:74`：按 roles 建 Broker/Controller。
- `core/src/main/scala/kafka/server/KafkaRaftServer.scala:90`：startup（controller first）。
- `core/src/main/scala/kafka/server/BrokerServer.scala:189`：BrokerServer.startup()。
- `core/src/main/scala/kafka/server/KafkaBroker.scala:76`：KafkaBroker trait 组件列表。

## 9. 版本边界与字数预算

- 基线 Kafka v4.x KRaft。
- 目标 5000~8000 字；只抓启动装配主线。

## 10. 本轮重写主线

1. 从"启动一个 broker 到底发生了什么"开场。
2. 入口 → 日志目录/meta props → SharedServer。
3. roles 分类。
4. controller-first 的原因。
5. BrokerServer.startup 组件装配。
6. 收网：启动即把多分层组件按依赖串起来。