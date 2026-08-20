# E-10 闭环笔记 Q1-Q4: 三层结构/CoordinationState/持久化/两阶段发布

## Q1: ClusterState 三层结构怎么组织?

假设: ClusterState = version + metadata + routingTable + blocks + nodes + customs, 版本单调递增。

验证过程:
- Read ClusterState (ClusterState.java:110,1195 行): 字段 version (ClusterState.java:156) / stateUUID (ClusterState.java:161) / routingTable (ClusterState.java:166) / nodes (ClusterState.java:168) / metadata (ClusterState.java:175) / blocks (ClusterState.java:177) / customs (ClusterState.java:179) — 构造器 (ClusterState.java:205-235)
- term() 来自 coordinationMetadata (ClusterState.java:251-253) — term 归属 CoordinationMetadata
- Builder.incrementVersion (ClusterState.java:897-899): `version + 1` + uuid 重置 UNKNOWN_UUID (ClusterState.java:899) — 每个新状态换新 uuid
- builder 入口: builder(ClusterName) (ClusterState.java:747) / builder(ClusterState) (ClusterState.java:751)
- 组件版本化: MasterService.patchVersions (MasterService.java:503-520) — 只有 master 控制版本 (MasterService.java:505): routingTable 变了 → withIncrementedVersion (MasterService.java:508); metadata 变了 → withIncrementedVersion (MasterService.java:511)

代码类型: Implementation (不可变状态容器)

结论: **ClusterState 是三层不可变容器: metadata (ClusterState.java:175) + routingTable (ClusterState.java:166) + blocks (ClusterState.java:177), 外加 nodes (ClusterState.java:168)/customs (ClusterState.java:179); version 单调递增 (incrementVersion ClusterState.java:897-899), 由 master 统一 patch (MasterService.java:503-520)**。ClusterState.java:110,156-179

## Q2: CoordinationState 的 Raft 风格 — term/选举/投票怎么工作?

假设: CoordinationState 是纯函数状态机 (无 IO), 选举靠 join 投票法定人数, term 单调递增防脑裂。

验证过程:
- 类注释 (CoordinationState.java:28-31): "directly implementing the formal model" (TLA+ ZenWithTerms)
- term 递增: handleStartJoin (CoordinationState.java:168-210) — 拒绝 term ≤ currentTerm (CoordinationState.java:169-178) → setCurrentTerm (CoordinationState.java:194) → 重置 joinVotes/publishVotes (CoordinationState.java:200-201) → 返回 Join (CoordinationState.java:203-209, 携带 term/lastAcceptedTerm/lastAcceptedVersion)
- 选举: handleJoin (CoordinationState.java:219-294) — 三重校验: term 匹配 (CoordinationState.java:222) / joiner lastAcceptedTerm 不优于自己 (CoordinationState.java:234-247) / 同 term 下 version 不优于自己 (CoordinationState.java:249-264) → addJoinVote (CoordinationState.java:275) → isElectionQuorum (CoordinationState.java:277) → electionWon (CoordinationState.java:289-292)
- 法定人数: ElectionStrategy.isElectionQuorum (ElectionStrategy.java:40-60) — 两个配置都要 quorum: lastCommitted (ElectionStrategy.java:49) + lastAccepted (ElectionStrategy.java:50)
- hasQuorum 定义 (CoordinationMetadata.java:347-353): `votedNodesCount * 2 > nodeIds.size()` — 严格多数
- 纯函数: 无 IO/无线程 — 持久化全部委托 PersistedState 接口 (CoordinationState.java:41,533)

代码类型: Algorithmic (选举算法)

结论: **CoordinationState = 纯函数状态机 (TLA+ 形式化模型, CoordinationState.java:28-31): term 严格递增 (handleStartJoin CoordinationState.java:168-194) → join 投票三重校验 (handleJoin CoordinationState.java:219-264) → 双配置法定人数 (ElectionStrategy.java:40-60, `n*2>N` CoordinationMetadata.java:347-353) → electionWon (CoordinationState.java:289)**。CoordinationState.java:28-31,168-294

## Q3: PersistedState 持久化什么?

假设: 只持久化 currentTerm + lastAcceptedState — 这两个是崩溃恢复的全部需要。

验证过程:
- interface PersistedState (CoordinationState.java:533-586): getCurrentTerm/setCurrentTerm (CoordinationState.java:538-556) + getLastAcceptedState/setLastAcceptedState (CoordinationState.java:543-557) + default markLastAcceptedStateAsCommitted (CoordinationState.java:565-579)
- InMemoryPersistedState (InMemoryPersistedState.java:12): 两个字段 currentTerm (InMemoryPersistedState.java:14) + acceptedState (InMemoryPersistedState.java:15), 构造断言 lastAcceptedTerm ≤ currentTerm (InMemoryPersistedState.java:23-24)
- 为什么只要这两个: 恢复后 ①知道自己的 term — 防旧 leader 复活 (E-6 fencing token 同源语义); ②知道最后接受的状态 — handleJoin 校验 joiner 不优于自己 (CoordinationState.java:234-264) 保证新 leader 有最新状态
- 何时写: handleStartJoin 写 term (CoordinationState.java:194) / handlePublishRequest 接受时写 state (CoordinationState.java:398) / handleCommit 标记 committed (CoordinationState.java:510)

代码类型: Interface (可插拔持久化)

结论: **PersistedState 只存两样: currentTerm + lastAcceptedState (CoordinationState.java:533-557, InMemoryPersistedState.java:14-15) — 崩溃后: term 防旧主复活 (fencing), lastAccepted 保证接续发布; 写点在 term 递增 (CoordinationState.java:194)/接受发布 (CoordinationState.java:398)/提交 (CoordinationState.java:510)**。CoordinationState.java:533,565

## Q4: 两阶段发布流程 — publish 到 apply 全链路

假设: master 计算新状态 → 广播 PublishRequest → 收集 PublishResponse 法定人数 → 广播 ApplyCommitRequest → 各节点 apply。

验证过程:
- 计算: MasterService.executeAndPublishBatch (MasterService.java:204-234): executeTasks 产生 newClusterState (MasterService.java:230-233) → patchVersions (MasterService.java:230,503-520) → publishClusterStateUpdate (MasterService.java:308)
- 发布: Coordinator.publish (Coordinator.java:1498-1606): mutex 内校验 leader+term (Coordinator.java:1505-1518) → newPublicationContext (Coordinator.java:1543) → handleClientValue (Coordinator.java:1555, CoordinationState.java:303-361) → CoordinatorPublication (Coordinator.java:1572) → publication.start (Coordinator.java:1586)
- 广播: Publication.start (Publication.java:54-62) → 逐目标 sendPublishRequest (Publication.java:61,252-260) — mastersFirstStream 主节点优先 (Publication.java:51)
- 接受: 接收方 handlePublishRequest (Coordinator.java:432-489): term 对齐 ensureTermAtLeast (Coordinator.java:478) → CoordinationState.handlePublishRequest 接受+持久化 (CoordinationState.java:370-402, setLastAcceptedState CoordinationState.java:398) → PublishWithJoinResponse (Coordinator.java:487)
- 投票: CoordinationState.handlePublishResponse (CoordinationState.java:413-456): 校验 term/version (CoordinationState.java:418-437) → addVote (CoordinationState.java:445) → isPublishQuorum (CoordinationState.java:446, ElectionStrategy.java:62-68 双配置) → ApplyCommitRequest (CoordinationState.java:452)
- 提交: sendApplyCommit (Publication.java:284-302) → handleApplyCommit (Coordinator.java:398-416) → CoordinationState.handleCommit (CoordinationState.java:464-512, markLastAcceptedStateAsCommitted CoordinationState.java:510) → 非主节点 clusterApplier.onNewClusterState (Coordinator.java:410)
- 失败处理: 节点故障/超时 → onPossibleCompletion (Publication.java:96-122: 全部 target 不活跃或 commit 未到 → onCompletion(false) L113); 更高 term → updateMaxTermSeen (Coordinator.java:506-517: maxTermSeen > currentTerm → 发布进行中则排队 bump L513-515, 否则 ensureTermAtLeast + startElection); 序列化/构造失败 → becomeCandidate (Coordinator.java:1546)
- apply: ClusterApplierService.onNewClusterState (ClusterApplierService.java:306-319) → 单线程 queue (ClusterApplierService.java:336) → ClusterChangedEvent (ClusterApplierService.java:473) → callClusterStateAppliers (ClusterApplierService.java:524-539, applier.applyClusterState ClusterApplierService.java:539)

代码类型: Glue (跨组件编排)

结论: **两阶段发布 = master 计算 (MasterService.java:230-233) → PublishRequest 广播逐节点 (Publication.java:252) → 接受+持久化 (CoordinationState.java:370-402) → PublishResponse 双配置法定人数 (CoordinationState.java:413-452) → ApplyCommitRequest 广播 (Publication.java:284) → 各节点 apply (Coordinator.java:398-416 → ClusterApplierService.java:306-539); 主节点在发布收尾时才 apply (Coordinator.java:406-408)**。Coordinator.java:1498-1586

跨域关联: E-4 RoutingTable (routingTable 随 ClusterState 发布) / E-5 updateShardState (集群状态驱动) / E-6 ReplicationTracker.updateFromMaster
