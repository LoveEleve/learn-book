# E-10 ClusterState — Pass 1 探索笔记 (扫轮廓)

> 🟡 B | 依赖: E-4 ✅ (RoutingTable) + E-5 ✅ (updateShardState) + E-6 ✅ (ReplicationTracker) | 对照: [[r9-replication]] (复制协议) [[r14-sentinel]] (故障转移) [[rd2-rlock]] (term 语义)
> 源码: server/src/main/java/org/elasticsearch/cluster/ (ClusterState 1195 行) + metadata/ (Metadata 2857 行) + coordination/ (62 文件)
> 测试地图: server/src/test/.../cluster/coordination/ (30+ 文件: CoordinationStateTests/PublicationTests/CoordinatorTests/ClusterBootstrapServiceTests)

## 继承树/调用图

```
ClusterState (ClusterState.java:110) — 三层: Metadata + RoutingTable (E-4) + ClusterBlocks + DiscoveryNodes + customs
├── Metadata (Metadata.java:99) — 集群级元数据 (clusterUUID/coordinationMetadata/settings/templates/indices)
│     └── CoordinationMetadata (CoordinationMetadata.java:30) — term + VotingConfiguration (CoordinationMetadata.java:325)
├── RoutingTable (E-4 交付, ClusterState.java:166 持有) — 分片路由
└── ClusterBlocks (ClusterState.java:177) — 读写全局块

Coordinator (Coordinator.java:108, 2158 行) — 集群状态机中枢, implements ClusterStatePublisher
├── becomeCandidate (Coordinator.java:830) / becomeLeader (Coordinator.java:872) / becomeFollower (Coordinator.java:928)
├── publish (Coordinator.java:1498) — ClusterStatePublisher 接口实现 (ClusterStatePublisher.java:49)
├── handleApplyCommit (Coordinator.java:398) — commit 后触发 apply
└── handlePublishRequest (Coordinator.java:432) — 接收方接受发布

CoordinationState (CoordinationState.java:32, 656 行) — 纯函数式协调算法 (TLA+ formal model)
├── handleStartJoin (CoordinationState.java:168) / handleJoin (CoordinationState.java:219) — 选举
├── handleClientValue (CoordinationState.java:303) → PublishRequest
├── handlePublishRequest (CoordinationState.java:370) / handlePublishResponse (CoordinationState.java:413) — 发布投票
├── handleCommit (CoordinationState.java:464) — 提交
└── interface PersistedState (CoordinationState.java:533) — 持久化: currentTerm + lastAcceptedState

Publication (Publication.java:30) — 发布编排 (每个目标 PublicationTarget Publication.java:234)
├── start (Publication.java:54) → sendPublishRequest (Publication.java:252)
└── handlePublishResponse (Publication.java:262) → sendApplyCommit (Publication.java:284)

PublicationTransportHandler (PublicationTransportHandler.java:70) — 序列化: full state vs Diff (PublicationTransportHandler.java:126)
MasterService (MasterService.java:78) — 任务批量执行 → executeAndPublishBatch (MasterService.java:204) → publish
ClusterApplierService (ClusterApplierService.java:67 CLUSTER_UPDATE_THREAD_NAME) — apply 单线程队列
ElectionStrategy (ElectionStrategy.java:20) — 选举法定人数扩展点 (DEFAULT_INSTANCE ElectionStrategy.java:22)
InMemoryPersistedState (InMemoryPersistedState.java:12) — 测试/默认持久化 (内存)
```

## 基本元素分解 (原则二)

1. **ClusterState 三层结构** — metadata (ClusterState.java:175) + routingTable (ClusterState.java:166) + blocks (ClusterState.java:177) + nodes (ClusterState.java:168) + customs (ClusterState.java:179), version 单调递增 (ClusterState.java:156)
2. **CoordinationState 状态机** — term 递增 (handleStartJoin CoordinationState.java:168) → join 投票 (handleJoin CoordinationState.java:219) → electionWon (CoordinationState.java:277) → 发布两阶段 (CoordinationState.java:303/370/413/464)
3. **投票法定人数** — VotingConfiguration.hasQuorum (CoordinationMetadata.java:347-353): `votedNodesCount * 2 > nodeIds.size()` — 严格多数
4. **两阶段发布** — MasterService 计算新状态 (MasterService.java:230-233) → Coordinator.publish (Coordinator.java:1498) → Publication 逐目标发送 (Publication.java:252) → 法定人数后 ApplyCommitRequest (Publication.java:269-276) → handleApplyCommit (Coordinator.java:398) → ClusterApplierService apply
5. **diff 增量发布** — PublicationTransportHandler (PublicationTransportHandler.java:126-205): full state 或 Diff 两种模式, 版本不兼容回退 full (PublicationTransportHandler.java:157)
6. **Metadata 版本化** — withIncrementedVersion (Metadata.java:326) — version+1 不变其他
7. **选举三模式** — Mode.CANDIDATE/LEADER/FOLLOWER (Coordinator.java:830/872/928) + PreVoteCollector (PreVoteCollector.java:23) + ElectionScheduler

## 标记问题 (≥5)

1. **Q1: ClusterState 三层怎么组织?** — version/metadata/routing/blocks 谁持有谁? version 怎么递增? (ClusterState.java:156,166-179 + Builder incrementVersion ClusterState.java:899)
2. **Q2: CoordinationState Raft 风格怎么工作?** — term/选举/投票 — handleStartJoin (CoordinationState.java:168) → handleJoin (CoordinationState.java:219) → isElectionQuorum (CoordinationState.java:277) — 纯状态机无 IO?
3. **Q3: PersistedState 持久化什么?** — currentTerm + lastAcceptedState (CoordinationState.java:533-586 + InMemoryPersistedState.java:12-47) — 为什么只存这两个?
4. **Q4: 两阶段发布流程?** — publish (Coordinator.java:1498) → PublishRequest 广播 (Publication.java:252) → PublishResponse 收集 → 法定人数 → ApplyCommitRequest (Publication.java:269) → handleApplyCommit (Coordinator.java:398) → ClusterApplierService apply (ClusterApplierService.java:539)
5. **Q5: Metadata 版本化** — withIncrementedVersion (Metadata.java:326-353) — 与 ClusterState version 什么关系?
6. **Q6: 节点怎么应用新状态?** — handlePublishRequest (Coordinator.java:432) → handlePublishRequest 接受 (CoordinationState.java:370) → handleApplyCommit (Coordinator.java:398) → clusterApplier.onNewClusterState (Coordinator.java:410) — version 判新旧 (CoordinationState.java:382-391)
7. **Q7: 与 E-4 RoutingTable 衔接** — routingTable 是 ClusterState 组件 (ClusterState.java:166) — MasterService 计算时怎么一起递增? (E-4 已见 MasterService.java:508)
8. **Q8: 与 s88 承接 + diff 传输** — PublicationTransportHandler (PublicationTransportHandler.java:126): full vs diff — 节点间连接面 (s88 承接点)

## 已读测试 (2 个)

- `CoordinationStateTests`: testStartJoinAfterBootstrap / testJoinWithHigherAcceptedTerm / testJoinWithLowerLastAcceptedTermWinsElection — 状态机边界
- `PublicationTests`: testSimpleClusterStatePublishing / testClusterStatePublishingWithFaultyNodeBeforeCommit — 发布编排

## 完成检查

- [x] 继承树/调用图已画出
- [x] 基本元素分解 (7 元素, 对应源码位置)
- [x] 8 个标记问题, 每个有源码位置
- [x] 已读 2 个测试文件

## 跨域发现

- 来源: E-10 Pass 1 — ClusterState 是 E-4 RoutingTable 的容器 (ClusterState.java:166), MasterService 计算时 routingTable 随 version 递增
- 发现: CoordinationState 是纯状态机 (无 IO, 全内存方法), 持久化委托 PersistedState 接口 — 可插拔 (InMemory 测试 vs 真实 gateway 磁盘)
- 已对照验证: CoordinationState.java:533 + E-4 RoutingTable.java:59 withIncrementedVersion
