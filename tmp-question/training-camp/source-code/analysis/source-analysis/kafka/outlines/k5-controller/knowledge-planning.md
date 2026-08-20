# K-5 Controller — 知识规划 (00 §10: 逐源提取→聚合→分类→聚类)

> 2026-08-15 | 源码: metadata/src/main/java/org/apache/kafka/controller/ (QuorumController 2172 + ClusterControlManager + BrokerHeartbeatManager + 30+ 管理类) + core/scala/kafka/controller/ (旧 KafkaController ZK 时代)
> [索引覆盖: Java 端 (metadata/controller) 已索引; Scala 端 (旧 KafkaController) 未索引]

## 01 逐源提取

| 源文件 | 机制点 |
|---|---|
| QuorumController.java | ①class (QuorumController.java:L174) ②appendWriteEvent (QuorumController.java:L931-954: 操作转事件入队) ③QuorumMetaLogListener.handleCommit (QuorumController.java:L956-985: Raft commit → active 推进/standby 回放) ④ControllerResult 生成 (QuorumController.java:L729-831) ⑤快照 |
| ClusterControlManager | ①broker 注册 ②ClusterControlState 状态机 |
| BrokerHeartbeatManager | ①broker 心跳活性 (KRaft 替代 ZK 临时节点) ②BrokersToIsrs (ISR 映射) |
| PartitionStateMachine/ReplicaStateMachine | ①分区状态 (Online/Offline/New) ②副本状态 (NewReplica/OnlineReplica/OfflineReplica) |
| KafkaController.scala (旧) | ZK 时代控制器 (对照, 未索引) |

## 02 聚合 (P1/P2/P3)

| 聚合机制 | 来源 | 分级 |
|---|---|---|
| 写事件队列 (appendWriteEvent) | QuorumController | P1 |
| Raft log 元数据复制 | QuorumController + raft/ (K-9 衔接) | P1 |
| broker 心跳活性 | BrokerHeartbeatManager | P1 |
| 分区/副本状态机 | Partition/ReplicaStateMachine | P1 |
| 快照/回放 | QuorumController + metadata/ | P2 |
| 旧 ZK 控制器对照 | KafkaController.scala | P2 |

## 03 深度分类

- 🔴: 写事件队列 + Raft 复制 + 状态机 (Controller 定义特征)
- 🟡: 心跳活性 / 快照
- 🟢: 配置

## 04 聚类 (教学顺序)

```
元数据操作 → appendWriteEvent (写事件队列) → Raft log (K-9) → commit
  → active: 推进 purgatory / standby: 回放记录
  → broker 心跳活性 (KRaft 替代 ZK 临时节点)
  → 分区/副本状态机 (Leader 选举/重分配)
  → 与 K-6 元数据衔接 (组协调记录同源)
```

**拆篇建议**: 2 篇 (🔴 A, 6 闭环)
- 01: QuorumController 写事件与 Raft 复制 (appendWriteEvent/commit/回放)
- 02: 活性与状态机 (broker 心跳/分区副本状态机/Leader 选举 + 旧 ZK 对照)
