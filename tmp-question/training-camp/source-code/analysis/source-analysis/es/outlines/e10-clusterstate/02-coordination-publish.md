# E-10 ClusterState 篇 2/2 — Raft 风格协调: 选举/持久化/两阶段发布

> 前置: [[E-10-clusterstate-01]] [[E-6-seqno-01]] (term 语义) | 复用: — | 对照: [[r14-sentinel]] (故障转移) [[rd2-rlock]] (fencing token) [[r28-networking]] (协议对照) | 引出: [[s88-boot-elasticsearch]] (承接回应)
> 🟡 B | 来源: CoordinationState.java:28-31,168-294,303-456,464-512,533-586 + Coordinator.java:830-928,1498-1586 + Publication.java:30-62,252-302 + ElectionStrategy.java:20-68 + CoordinationMetadata.java:325-353 + ClusterBootstrapService.java:46,105-106 + MasterService.java:204-234
> 定位: E-10 卷收尾 — 回答"master 怎么选出来? 集群状态怎么发布?" (全书收束域)

**读者处境**: 面试官问 "ES 怎么选 master? 集群状态怎么同步?" 你答 "投票" — 但再问 "term 怎么递增? 为什么持久化只存两样? 两阶段发布怎么走?" 你答不上来。这篇是协调协议的完整答案, 收束 E-10 域与全书。

### 1. 问题引入 — 从快照到一致

场景: 上一篇有了一份快照 — 但多节点怎么保证快照一致? 谁拍板? 挂了一个怎么办?
- CoordinationState 纯状态机 (CoordinationState.java:28-31, TLA+ 形式化模型)
- 本篇问题: 选举 (Q2) / 持久化 (Q3) / 两阶段发布 (Q4) / 路由衔接 (Q7) / s88 承接 (Q8)

### 2. 选举 — term + join 投票

场景: master 挂了, 谁当新 master?
- term 递增: handleStartJoin (CoordinationState.java:168-194) — 拒绝旧 term (CoordinationState.java:169-178) → 写新 term (CoordinationState.java:194)
- 投票: handleJoin (CoordinationState.java:219-294) — 三重校验 (CoordinationState.java:222/L234-264) → addJoinVote (CoordinationState.java:275) → 法定人数 (CoordinationState.java:277)
- 双配置 quorum: lastCommitted + lastAccepted 都要过半数 (ElectionStrategy.java:40-60); hasQuorum = `votedNodesCount*2 > size` (CoordinationMetadata.java:347-353)
- 模式切换: becomeCandidate (Coordinator.java:830) → becomeLeader (Coordinator.java:872, 选举获胜后) / becomeFollower (Coordinator.java:928)
- 初识配置: ClusterBootstrapService initialMasterNodes (ClusterBootstrapService.java:105-106) — 集群首次形成

### 3. 持久化 — 只存两样

场景: 崩溃重启, 什么必须留下来?
- PersistedState 接口 (CoordinationState.java:533-586): currentTerm + lastAcceptedState (CoordinationState.java:538-557)
- 默认实现 InMemoryPersistedState (InMemoryPersistedState.java:12, 两个字段 InMemoryPersistedState.java:14-15)
- 为什么够: term 防旧主复活 (fencing, 与 rd2-rlock 同源) / lastAccepted 保证新主不落后 (handleJoin 校验 L234-264)
- 写点: term 递增 (CoordinationState.java:194) / 接受发布 (CoordinationState.java:398) / 提交 (CoordinationState.java:510)

### 4. 两阶段发布 — PublishRequest → ApplyCommitRequest

场景: master 算出新快照, 怎么让大家都认?
- 阶段一 (接受): master 广播 PublishRequest (Publication.java:252, 逐目标) → 各节点校验+持久化 (CoordinationState.java:370-402) → 回 PublishResponse
- 法定人数: 双配置投票 (CoordinationState.java:413-456, isPublishQuorum ElectionStrategy.java:62-68) → ApplyCommitRequest (CoordinationState.java:452)
- 阶段二 (提交): 广播 ApplyCommitRequest (Publication.java:284) → 各节点 handleCommit (CoordinationState.java:464-512, markCommitted CoordinationState.java:510) → apply (Coordinator.java:398-416)
- 主节点特例: 发布收尾才 apply (Coordinator.java:406-408)
- 失败处理: 节点故障/超时 → onPossibleCompletion (Publication.java:96-122); 更高 term 出现 → updateMaxTermSeen (Coordinator.java:506-517) 发布结束后 bump term 重新选举; 序列化/上下文构造失败 → becomeCandidate (Coordinator.java:1546)

### 5. 路由衔接 + s88 承接 — 收束全链路

场景: 前面 11 域怎么都汇到这里?
- 发布内容 = RoutingTable + Metadata (Q7, MasterService.java:507-511) — E-4 的路由随集群状态到达每节点
- 发布传输 = full/diff 双模式 (PublicationTransportHandler.java:340-370) — 新节点 full / 已知节点 diff
- s88 承接: "连接/协议深入在阶段3 ES" — 服务端节点间发布面在此展开 (Q8, 对照 r28 RESP)
- 全链路: E-1~E-9 的写/读/复制都在集群状态发布的覆盖下 — E-10 是 ES 分布式面收束

### 核心悬念
"ES 的 master 选举和 Raft 有什么相同与不同?" — 相同: term 单调递增 + 投票法定人数 + 日志 (状态) 复制两阶段; 不同: 投票对象是 join 而非 vote 请求, quorum 是双配置过半数, 状态本身全量+diff 广播而非 Raft 的逐条 append — "Raft 风格"而非 Raft。

### 概念依赖链
Q2 选举 → Q3 持久化 → Q4 发布 → (Q7/Q8 衔接) → (E-4/E-5/E-6/s88 收束)

### 源码锚点清单
- CoordinationState.java:28-31 (TLA+ 模型) / 168-194 (handleStartJoin) / 219-294 (handleJoin) / 277 (electionWon) / 303-361 (handleClientValue) / 370-402 (handlePublishRequest) / 413-456 (handlePublishResponse) / 446 (quorum) / 452 (ApplyCommitRequest) / 464-512 (handleCommit) / 510 (markCommitted) / 533-586 (PersistedState) / 565 (markLastAcceptedStateAsCommitted)
- Coordinator.java:830 (becomeCandidate) / 872 (becomeLeader) / 928 (becomeFollower) / 1498-1586 (publish) / 1543 (publicationContext) / 1555 (handleClientValue) / 398-416 (handleApplyCommit)
- Publication.java:30 (类) / 54 (start) / 61 (逐目标) / 96-122 (onPossibleCompletion) / 252 (sendPublishRequest) / 284 (sendApplyCommit)
- ElectionStrategy.java:20 (类) / 40-60 (isElectionQuorum) / 62-68 (isPublishQuorum)
- CoordinationMetadata.java:325 (VotingConfiguration) / 347-353 (hasQuorum)
- ClusterBootstrapService.java:46 (类) / 105-106 (initialMasterNodes)
- MasterService.java:204-234 (executeAndPublishBatch) / 230-233 (新状态计算)
- PublicationTransportHandler.java:340-370 (full/diff 选择)
