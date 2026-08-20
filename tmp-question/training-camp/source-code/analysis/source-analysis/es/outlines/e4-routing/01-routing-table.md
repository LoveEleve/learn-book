# E-4 Cluster Routing 篇 1/3 — 路由表: 三级结构与版本化

> 前置: [[E-5-shard-01]] (分片状态) [[E-10-clusterstate]] | 复用: — | 对照: [[r21-db]] (键空间) | 引出: [[E-4-routing-02]] [[E-4-routing-03]]
> 🔴 A | 来源: RoutingTable.java:45-60 + IndexShardRoutingTable.java:45-79 + ShardRouting.java:447,479,600 + ShardRoutingState.java:15-32
> 定位: Routing 卷开篇 — 回答"路由表怎么组织? 版本怎么递增? 两层状态啥关系?"

**读者处境**: 面试官问 "ES 怎么知道分片在哪个节点? 路由表是什么?" 你答 "RoutingTable" — 但再问 "ShardRouting 和 IndexShard 的状态有什么区别? 路由表版本谁递增?" 你卡住了。这篇是路由表结构的完整答案。

### 1. 问题引入 — 分布式索引的坐标

场景: 100 个分片分布在 10 个节点 — 怎么知道"哪个分片在哪"?
- RoutingTable (RoutingTable.java:45): 全集群路由表 (索引→分片→节点)
- 本篇问题: 三级结构 (Q1) / 两层状态 (Q2)

### 2. 三级结构 — RoutingTable → IndexRoutingTable → ShardRouting

场景: 路由表怎么分层?
- RoutingTable (L45-60): indicesRouting: ImmutableOpenMap<String, IndexRoutingTable> (RoutingTable.java:52)
- IndexRoutingTable (704 行) → IndexShardRoutingTable (IndexShardRoutingTable.java:45-79): primary (IndexShardRoutingTable.java:50) + replicas (IndexShardRoutingTable.java:51) — **一个分片的所有副本**
- ShardRouting (992 行): 单个分片路由 — primary/replica + 状态 + 节点
- 版本化: withIncrementedVersion (RoutingTable.java:59-60)
- **重建时机**: 每次分配/节点变化后由 AllocationService 重建路由表 (主先副后, 篇 2) + 版本+1 发布

### 3. 版本递增 — master 驱动

场景: 路由表什么时候变?
- withIncrementedVersion (RoutingTable.java:59): version+1
- 入口: MasterService (MasterService.java:508) 发布前递增 — 节点按版本判新旧
- 对照: Metadata (Metadata.java:326) / IndexMetadata (IndexMetadata.java:960) 同构
- 语义: 低版本路由表 → 丢弃 (过时)

### 4. 两层状态 — 路由态 vs 分片态

场景: ShardRouting 状态和 IndexShard 状态什么关系?
- 路由态 (ShardRoutingState.java:15-32): UNASSIGNED(1)/INITIALIZING(2)/STARTED(3)/RELOCATING(4) — 集群视角
- 分片态 (E-5 IndexShardState): CREATED/RECOVERING/POST_RECOVERY/STARTED/CLOSED — 本地视角
- 关系: initialize (ShardRouting.java:479) → 分片 RECOVERING; moveToStarted (ShardRouting.java:600) → STARTED
- 驱动: E-5 updateShardState 由路由态变化触发

### 5. 收束 — 路由表的定位

- 三级结构 + 版本化 = 集群的"地图 + 版本"
- 两层状态 = "决策 (集群) vs 执行 (本地)"
- 引出: 篇 2 (分配: 地图怎么生成) — 篇 3 (路由: 地图怎么用)

### 核心悬念
"ShardRouting 和 IndexShard 的状态为什么是两套?" — 路由态是集群的"应该在哪" (master 决策), 分片态是本地的"实际到哪步" (执行进度) — 解耦决策与执行, 网络分区时两者可短暂不一致。

### 概念依赖链
Q1 三级结构 → Q2 两层状态 → (E-5/E-10 衔接)

### 源码锚点清单
- RoutingTable.java:45 (类) / 52 (indicesRouting) / 59-60 (withIncrementedVersion)
- IndexShardRoutingTable.java:45-79 (primary/replicas) / 50 (primary) / 51 (replicas)
- ShardRouting.java:447 (moveToUnassigned) / 479 (initialize) / 600 (moveToStarted)
- ShardRoutingState.java:15-32 (4 态枚举)
- MasterService.java:508 (版本递增入口)
