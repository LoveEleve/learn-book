# E-4 Cluster Routing — Pass 1 探索笔记 (扫轮廓)

> 🔴 A | 依赖: E-5 ✅ + E-6 ✅ | 对照: [[r14-sentinel]] (Redis 哨兵) [[rd1-connection]] (连接池路由)
> 源码: server/src/main/java/org/elasticsearch/cluster/routing/ (30 文件 8041 行 + allocation/ 子目录)
> 测试地图: server/src/test/.../cluster/routing/ (15+ 文件)

## 继承树/调用图

```
RoutingTable (654 行, L45) — 全集群路由表
├── indicesRouting: ImmutableOpenMap<String, IndexRoutingTable> (RoutingTable.java:52)
├── IndexRoutingTable (704 行) → IndexShardRoutingTable (712 行) — 索引→分片
│     └── ShardRouting (992 行) — 单个分片路由 (primary/replica + 4 状态)
└── version 递增: withIncrementedVersion (RoutingTable.java:59)

ShardRoutingState (枚举, L15-32): UNASSIGNED(1)/INITIALIZING(2)/STARTED(3)/RELOCATING(4)
OperationRouting (L36): 读路由 — getShards (L63-72) + preferenceActiveShardIterator + 自适应副本 (OperationRouting.java:39)

分配 (allocation/):
    AllocationService.reroute → 19 个 AllocationDecider (决策器)
    → BalancedShardsAllocator (1452 行) 平衡分配
```

## 基本元素分解 (原则二)

1. **RoutingTable 三级结构** — RoutingTable (索引级) → IndexRoutingTable → IndexShardRoutingTable (分片副本组) — 版本化 (RoutingTable.java:59)
2. **ShardRouting** — 单个分片: primary/replica + state (4 态) + currentNodeId/relicatingNodeId (ShardRouting.java:50-51)
3. **ShardRoutingState 枚举** — L15-32: UNASSIGNED/INITIALIZING/STARTED/RELOCATING — 与 IndexShardState (E-5, 5 态) 区别: 路由态 vs 分片态
4. **OperationRouting 读路由** — getShards (OperationRouting.java:63): preference/routing 参数 → 目标分片迭代器
5. **19 决策器** — allocation/decider/: DiskThreshold/SameShard/ShardsLimit/... — 每个决策器对"是否允许分配"投票
6. **BalancedShardsAllocator** — 1452 行: 权重平衡 (分片数/磁盘/水印) — 分配的核心算法

## 标记问题 (≥5)

1. **Q1: RoutingTable 版本化怎么工作?** — withIncrementedVersion (RoutingTable.java:59) — 集群状态变更时 +1? 怎么检测变化?
2. **Q2: ShardRouting 4 态 vs IndexShardState 5 态** — 路由态 (UNASSIGNED 等) vs 分片态 (CREATED 等) — 两层状态的关系?
3. **Q3: OperationRouting 怎么选副本?** — preference/routing 参数 → 副本选择策略? 自适应副本 (OperationRouting.java:39) 是什么?
4. **Q4: 19 决策器怎么投票?** — AllocationDecider 接口: canAllocate/canRemain/canRebalance — 决策器组合怎么聚合?
5. **Q5: BalancedShardsAllocator 平衡算法** — 权重函数? 怎么在节点间移动分片?
6. **Q6: 分配流程** — AllocationService.reroute → 决策器 → 分配器 → 路由表更新 — 时序?
7. **Q7: 与 Redis 对照** — Redis 无分片路由 (单机/集群 slot); ES 路由表是分布式索引的坐标
8. **Q8: 写路由 vs 读路由** — 写固定主分片, 读可到副本 — 路由差异?

## 已读测试 (2 个)

- `RoutingTableTests.testAllShards` (RoutingTableTests.java:120): 全分片枚举
- `RoutingTableTests.testActivePrimaryShardsGrouped` (RoutingTableTests.java:184): 活跃主分片分组
- `IndexShardRoutingTableTests` (分片副本组)

## 完成检查

- [x] 继承树/调用图已画出
- [x] 基本元素分解 (6 元素, 对应源码位置)
- [x] 8 个标记问题, 每个有源码位置
- [x] 已读 3 个测试文件

## 跨域发现

- 来源: E-4 Pass 1 — ShardRouting.state (路由态) 与 IndexShard.state (分片态) 两层状态 — E-5 的分片状态机由路由态驱动 (updateShardState)
- 发现: RoutingTable 是 ClusterState 的核心组件 (E-10 衔接) — 路由表随集群状态发布
- 已对照验证: ShardRouting.java:107-127 (状态 case) + E-5 updateShardState (IndexShard.java:493)
