# E-10 ClusterState 篇 1/2 — 集群大脑的快照: 三层结构/版本化/应用

> 前置: [[E-4-routing-01]] (RoutingTable) [[E-5-shard-01]] (分片状态机) | 复用: — | 对照: [[r21-db]] (键空间 vs 集群状态) | 引出: [[E-10-clusterstate-02]]
> 🟡 B | 来源: ClusterState.java:110,156-179,897-899 + Metadata.java:99,326-353 + MasterService.java:503-524 + ClusterApplierService.java:306,473,539 + CoordinationState.java:370-402
> 定位: E-10 卷开篇 — 回答"集群状态是什么? 版本怎么管? 节点怎么用?"

**读者处境**: 面试官问 "ES 集群怎么知道有哪些分片? 节点怎么保持状态一致?" 你答 "集群状态" — 但再问 "版本怎么递增? diff 怎么发? 节点怎么判新旧?" 你答不上来。这篇是集群状态容器的完整答案。

### 1. 问题引入 — 集群的"大脑快照"

场景: 3 节点集群, 每个节点都要知道: 有哪些索引/分片在哪/哪些节点活着 — 这份快照是什么?
- 每个节点内存中一份, master 协调更新 (ClusterState.java:69 注释)
- 本篇问题: 结构 (Q1) / 版本化 (Q5) / 应用 (Q6)

### 2. 三层结构 — metadata/routing/blocks

场景: 快照里装了什么?
- ClusterState 容器 (ClusterState.java:110): metadata (ClusterState.java:175) + routingTable (ClusterState.java:166) + blocks (ClusterState.java:177) + nodes (ClusterState.java:168) + customs (ClusterState.java:179) + version (ClusterState.java:156) + stateUUID (ClusterState.java:161)
- term 从 coordinationMetadata 来 (ClusterState.java:251-253) — 不属于顶层
- 不可变: 每次更新建新对象 (builder ClusterState.java:747/751)
- 三层职责: metadata=集群配置+索引元数据 (E-7 消费) / routingTable=分片在哪 (E-4 交付) / blocks=全局读写锁

### 3. 版本化 — master 统一 patch

场景: 快照更新后版本怎么递增?
- Builder.incrementVersion (ClusterState.java:897-899): version+1 + 新 uuid
- MasterService.patchVersions (MasterService.java:503-520): 只有 master 控制版本 (MasterService.java:505) — routingTable 变 → 各自递增 (MasterService.java:508) / metadata 变 → 各自递增 (MasterService.java:511)
- Metadata.withIncrementedVersion (Metadata.java:326-353): 全字段复制 version+1 (Metadata.java:330)
- 语义: 三层版本解耦, 同次发布同步推进 — 节点按 version 判新旧
- 设计权衡: 为什么不一个全局版本? — 组件级版本让节点只看变化的部分 (diff 粒度), 全局版本只是发布序号; 代价是 patch 逻辑复杂 (引用比较 MasterService.java:507)

### 4. 节点应用 — 判新旧与 apply 队列

场景: 节点收到新状态怎么办?
- 接受门槛: term 匹配 (CoordinationState.java:372-381) + version 更大 (CoordinationState.java:382-391) — 旧状态拒绝
- diff 应用: 基于上一状态 (PublicationTransportHandler.java:178) — 不兼容回退 full (PublicationTransportHandler.java:154-157)
- apply 串行: ClusterApplierService 单线程队列 (ClusterApplierService.java:336) → ClusterChangedEvent 带前后对比 (ClusterApplierService.java:473) → callClusterStateAppliers (ClusterApplierService.java:524-539)
- 消费: E-5 updateShardState 由集群状态驱动 (IndexShard.java:493)

### 核心悬念
"ES 集群怎么保证每个节点对世界的认知一致?" — 一份不可变的 ClusterState 快照 + master 统一版本化 (version 单调递增) + 两阶段发布同步到每节点 — 快照本身是纯数据, 一致性交给协调层 (下一篇)。

### 概念依赖链
Q1 结构 → Q5 版本化 → Q6 应用 → (E-4/E-5 衔接) → (02 篇: 选举/发布)

### 源码锚点清单
- ClusterState.java:110 (类) / 156 (version) / 161 (stateUUID) / 166 (routingTable) / 168 (nodes) / 175 (metadata) / 177 (blocks) / 179 (customs) / 251-253 (term) / 747,751 (builder) / 897-899 (incrementVersion)
- Metadata.java:99 (类) / 326-353 (withIncrementedVersion) / 330 (version+1)
- MasterService.java:503-520 (patchVersions) / 505 (master 控制版本) / 508 (routingTable 递增) / 511 (metadata 递增) / 522-524 (incrementVersion)
- CoordinationState.java:372-391 (接受门槛) / 398 (setLastAcceptedState)
- PublicationTransportHandler.java:154-157 (无本地状态回退) / 178 (diff.apply) / 186-188 (不兼容回退)
- ClusterApplierService.java:306 (onNewClusterState) / 336 (单线程队列) / 473 (ClusterChangedEvent) / 524-539 (appliers)
