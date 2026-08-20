# K-5 Controller — Pass 1 探索笔记 (扫轮廓)

> 🔴 A | 依赖: K-6 ✅ (组元数据) + K-3 ✅ | 对照: [[E-10-clusterstate]] (ES 集群协调) [[rd2-rlock]] (fencing)
> 源码: metadata/controller/ (QuorumController 2172 + ClusterControlManager + BrokerHeartbeatManager + 30+ 类) + core/scala/kafka/controller/ (旧)
> [索引覆盖: Java 端已索引; Scala 旧控制器未索引]
> 测试地图: metadata/src/test/ (QuorumControllerTest/ClusterControlManagerTest/BrokerHeartbeatManagerTest)

## 继承树/调用图 (codebase-memory 可验证)

```
QuorumController (QuorumController.java:174, implements Controller)
├── appendWriteEvent (QuorumController.java:L931-954) — 元数据操作转写事件入队
│     └── ControllerWriteEvent → queue → 生成记录 (QuorumController.java:L779-831) → Raft log
├── QuorumMetaLogListener.handleCommit (QuorumController.java:L956-985) — Raft commit 回调
│     ├── active controller: 推进 purgatory (QuorumController.java:L972-978)
│     └── standby controller: 回放记录 (QuorumController.java:L979-985)
├── ClusterControlManager — broker 注册/状态
├── BrokerHeartbeatManager — broker 心跳活性 (KRaft 替代 ZK)
└── BrokersToIsrs — ISR 映射 (K-4 衔接)
```

## 基本元素分解 (原则二)

1. **写事件队列** — appendWriteEvent: 所有元数据操作转事件 (QuorumController.java:L931-954)
2. **Raft 复制** — 记录写 Raft log → commit → 回调 (QuorumController.java:L956-985)
3. **active/standby 分工** — active 推进 / standby 回放 (QuorumController.java:L972-985)
4. **broker 心跳活性** — BrokerHeartbeatManager (KRaft 替代 ZK 临时节点)
5. **状态机** — 分区/副本 (PartitionStateMachine/ReplicaStateMachine)
6. **ISR 映射** — BrokersToIsrs (K-4 衔接)

## 标记问题 (≥5)

1. **Q1: appendWriteEvent 怎么工作?** — 操作转事件入队 (QuorumController.java:L931-954)
2. **Q2: Raft commit 怎么处理?** — active/standby 分工 (QuorumController.java:L956-985)
3. **Q3: broker 心跳活性?** — BrokerHeartbeatManager (KRaft 替代 ZK)
4. **Q4: 分区/副本状态机?** — Partition/ReplicaStateMachine (规划断言)
5. **Q5: Leader 选举与重分配?** — 状态机触发 (规划断言: ISR 中第一个活着的副本)
6. **Q6: 与 K-6/K-9 衔接?** — 组元数据同源 + raft/ (K-9)

## 已读测试 (2 个)

- `QuorumControllerTest`: 写事件/回放
- `BrokerHeartbeatManagerTest`: 心跳活性

## 完成检查

- [x] 继承树/调用图
- [x] 基本元素分解 (6 元素)
- [x] 6 个标记问题
- [x] 已读 2 个测试

## 跨域发现

- 来源: K-5 Pass 1 — QuorumController 的写事件+Raft 复制与 K-6 GroupMetadataManager 同构 (元数据即记录)
- 发现: 旧 KafkaController (ZK) 在 core/scala — KRaft 时代已退役 (规划断言: Kafka 3.3+ 用 KRaft)
- 已对照验证: K-6 GroupMetadataManager (记录状态机) / E-10 (ES ClusterState 协调, 已交付)
