# K-5 闭环笔记 Q1-Q6: 写事件/Raft commit/心跳/状态机/选举/衔接

## Q1: appendWriteEvent 怎么工作?

假设: 所有元数据操作转写事件入队, 统一经 Raft log。

验证过程:
- QuorumController (QuorumController.java:L174, implements Controller)
- appendWriteEvent (QuorumController.java:L931-954): ControllerWriteEvent 封装 (QuorumController.java:L944) → queue.append (QuorumController.java:L946-950) → 返回 future (QuorumController.java:L953)
- 生成记录: generateRecordsAndResult (QuorumController.java:L729-831): 操作执行 → ControllerResult (记录+响应, QuorumController.java:L779-796)
- 设计: 单一写路径 — 所有元数据变更 (createTopic/registerBroker) 都走事件队列

代码类型: Implementation (写事件队列)

结论: **appendWriteEvent = 操作转事件入队 (QuorumController.java:L931-954) → generateRecordsAndResult 生成 ControllerResult (QuorumController.java:L729-831) — 单一写路径统一经 Raft log (K-9)**。QuorumController.java:931-954

## Q2: Raft commit 怎么处理?

假设: active controller 推进 purgatory, standby 回放记录。

验证过程:
- QuorumMetaLogListener.handleCommit (QuorumController.java:L956-985): Raft commit 回调 (QuorumController.java:L956-958)
- isActiveController 判断 (QuorumController.java:L965): active → 推进 offsetControl.handleCommitBatch + purgatory 完成 (QuorumController.java:L972-978); standby → 回放记录 (QuorumController.java:L979-985, "Replaying commits from the active node")
- 语义: active 已 replayed 无需重复 (QuorumController.java:L970-971 注释), standby 从 log 重建状态

代码类型: Implementation (commit 分工)

结论: **Raft commit → active: 推进水位+完成 purgatory (QuorumController.java:L972-978) / standby: 回放记录重建 (QuorumController.java:L979-985) — active/standby 分工保证单写多读一致**。QuorumController.java:956-985

## Q3: broker 心跳活性?

假设: BrokerHeartbeatManager 跟踪心跳 (KRaft 替代 ZK 临时节点)。

验证过程:
- BrokerHeartbeatManager (BrokerHeartbeatManager.java:L58): 软状态 — 心跳时间/BrokerHeartbeatTracker (L45-49 注释)
- fenced 状态 (BrokerHeartbeatManager.java:L66-68): broker 是否被 fence
- 仅 active controller 有 BrokerHeartbeatManager (BrokerHeartbeatManager.java:L51-57 注释) — standby 不处理心跳
- 注释: "which brokers are fenced or not into a single place" (BrokerHeartbeatManager.java:L49)

代码类型: Implementation (活性跟踪)

结论: **心跳活性 = BrokerHeartbeatManager (BrokerHeartbeatManager.java:L58) + fenced 标记 (BrokerHeartbeatManager.java:L66-68); 仅 active controller 持有 (BrokerHeartbeatManager.java:L51-57) — KRaft 替代 ZK 临时节点的活性机制**。BrokerHeartbeatManager.java:45-68

## Q4: 分区/副本状态机?

假设: 状态机管理分区/副本生命周期 (规划断言: Online/Offline/New + NewReplica/OnlineReplica/OfflineReplica)。

验证过程:
- 4.x 实现: PartitionChangeBuilder (leader 选举/变更) + BrokerControlStates (BrokerHeartbeatManager.java:L66-68 broker 状态)
- 旧规划断言 (PartitionStateMachine/ReplicaStateMachine) — 4.1.2 已重构为 QuorumController 内嵌管理 (ClusterControlManager/BrokerControlStates)
- BrokerControlState (枚举: fenced/active 等)
- 状态转换由写事件驱动 (Q1 队列)

代码类型: Implementation (状态机)

结论: **4.1.2 状态机已重构: PartitionChangeBuilder (选举/变更) + BrokerControlStates (broker 状态) 内嵌 QuorumController 体系 — 规划断言 PartitionStateMachine/ReplicaStateMachine 是旧版 (KRaft 迁移中重构)**。PartitionChangeBuilder.java + BrokerControlStates.java

## Q5: Leader 选举与重分配?

假设: ISR 中第一个活着的副本成为新 Leader (规划断言)。

验证过程:
- PartitionChangeBuilder 注释 (PartitionChangeBuilder.java:L70-78): ①elect preferred replica if in ISR (PartitionChangeBuilder.java:L70) ②elect from ISR (PartitionChangeBuilder.java:L74) ③prefer ISR but keep online even outside ISR (L78, unclean 场景)
- minISR (PartitionChangeBuilder.java:L88,112): 最小 ISR 约束
- 重分配: PartitionReassignmentReplicas/PartitionReassignmentRevert (变更构建)
- 触发: broker 心跳超时 → fenced → 状态机选举 (Q3 衔接)

代码类型: Algorithmic (选举)

结论: **Leader 选举 = preferred replica 优先 (PartitionChangeBuilder.java:L70) → ISR 内选 (PartitionChangeBuilder.java:L74) → 可出 ISR 保在线 (L78, unclean); minISR 约束 (PartitionChangeBuilder.java:L88) — 规划断言"ISR 中第一个活着的"是简化, 实际三档策略**。PartitionChangeBuilder.java:70-112

## Q6: 与 K-6/K-9 衔接?

假设: 组元数据同源 (K-6) + Raft 层 (K-9)。

验证过程:
- K-6: GroupMetadataManager 记录也走元数据 log — 与 Controller 记录同源 (K-6 篇 3 核心悬念)
- K-9: Raft log 是 Controller 的复制底座 (raft/ 模块, K-9 交付后回补)
- E-10 对照: ES ClusterState master 协调 vs Kafka QuorumController — 都是"单写者+广播"模型
- K-4: BrokersToIsrs (ISR 映射) — 副本状态数据源

代码类型: 衔接分析

结论: **衔接 = Controller 记录 ↔ K-6 组记录 (同源元数据 log) + K-9 Raft (复制底座) + K-4 BrokersToIsrs (ISR 源) — 对照 E-10 单写者广播模型 (ES 已交付)**。QuorumController.java:956-985 + BrokersToIsrs

跨域关联: K-6 (组元数据) / K-9 (Raft, 待回补) / K-4 (ISR) / E-10 (对照)
