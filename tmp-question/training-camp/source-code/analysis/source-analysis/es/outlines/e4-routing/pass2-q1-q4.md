# E-4 闭环笔记 Q1-Q4: 版本化/两层状态/读路由/决策器

## Q1: RoutingTable 版本化 — 集群变更递增

假设: 每次集群状态变更 (分片移动) 路由表 version+1, 节点据此判断新旧。

验证过程:
- Read RoutingTable (RoutingTable.java:45-60): version 字段 (RoutingTable.java:49) + withIncrementedVersion (RoutingTable.java:59-60) `new RoutingTable(version + 1, indicesRouting)`
- 递增入口: MasterService (MasterService.java:508) `builder.routingTable(newClusterState.routingTable().withIncrementedVersion())` — **master 发布前递增**
- 对照: Metadata.withIncrementedVersion (Metadata.java:326) / IndexMetadata (IndexMetadata.java:960) — 同类模式
- 语义: 节点收到低版本路由表 → 丢弃 (过时); 高版本 → 应用

代码类型: Interface (版本契约)

结论: **路由表 version 由 master 在发布集群状态时递增 (MasterService.java:508) — 节点按版本判新旧丢弃过时更新; RoutingTable/Metadata/IndexMetadata 三类版本化同构**。RoutingTable.java:59 + MasterService.java:508

跨域关联: E-10 ClusterState (版本发布) — 衔接点

## Q2: 两层状态 — ShardRoutingState vs IndexShardState

假设: 路由态 (UNASSIGNED 等, 集群视角) 与分片态 (CREATED 等, 本地视角) 两层 — 路由态驱动分片态迁移。

验证过程:
- Read ShardRoutingState (ShardRoutingState.java:15-32): UNASSIGNED(1)/INITIALIZING(2)/STARTED(3)/RELOCATING(4) — **4 态集群视角**
- 对照 E-5 IndexShardState (5 态: CREATED/RECOVERING/POST_RECOVERY/STARTED/CLOSED) — 本地视角
- 关系: ShardRouting.initialize (ShardRouting.java:479) → 分片开始 RECOVERING; moveToStarted (ShardRouting.java:600) → STARTED; moveToUnassigned (ShardRouting.java:447) → 移除
- 驱动: E-5 updateShardState (IndexShard.java:493) 由路由态变化触发

代码类型: Interface (双层状态)

结论: **路由态 (ShardRouting, 集群权威) vs 分片态 (IndexShard, 本地执行): master 改路由态 → 发布 → 分片 updateShardState 响应迁移 (E-5) — 两层状态解耦"决策"与"执行"**。ShardRoutingState.java:15-32 + ShardRouting.java:447,479,600

## Q3: OperationRouting — 读路由与偏好

假设: 读操作按 preference 选择副本 (本地优先/随机/指定节点), 自适应副本按响应时间。

验证过程:
- Read OperationRouting (OperationRouting.java:36-77): getShards (OperationRouting.java:63-72) → preferenceActiveShardIterator (OperationRouting.java:72)
- preference 解析 (OperationRouting.java:206-240): 空 → 默认; '_' 前缀 → Preference.parse (OperationRouting.java:214) — _only_nodes/_local/_shards 等
- 自适应副本 (OperationRouting.java:39): `cluster.routing.use_adaptive_replica_selection` — 按响应时间/队列长度选副本
- 返回 ShardIterator: 候选副本迭代器 (遍历直到成功)

代码类型: Implementation (路由策略)

结论: **读路由 = preference 驱动的副本选择: 默认随机/轮询, _only_nodes/_local 等偏好 (OperationRouting.java:214), 自适应副本按响应时间选 (OperationRouting.java:39) — 写固定主分片 (E-6 复制), 读可到任意副本**。OperationRouting.java:39,63-77,206-240

跨域关联: E-6 ReplicationOperation (写路由) — 读写路由对照 (Q8)

## Q4: 19 决策器 — 分配投票

假设: 每个决策器对"能否分配"投票 (YES/NO/THROTTLE), 组合成最终决策。

验证过程:
- Read AllocationDecider (AllocationDecider.java:32-91): canRebalance (AllocationDecider.java:32) / canAllocate (AllocationDecider.java:40,56) / canRemain (AllocationDecider.java:48) / shouldAutoExpandToNode (OperationRouting.java:72) — **5 类判定接口**
- 决策器清单 (19 个): DiskThreshold/SameShard/ShardsLimit/NodeVersion/Throttling...
- 聚合: AllocationDeciders 组合各决策器结果 (Decision.Type: YES/NO/THROTTLE)
- 语义: 任一 NO → 拒绝; 全 YES → 允许; 有 THROTTLE 无 NO → 限速

代码类型: Interface (投票协议)

结论: **分配决策 = 19 决策器投票: 每个决策器独立判定 (canAllocate/canRemain/canRebalance), 聚合规则 = 任一 NO 拒绝, 全 YES 允许, THROTTLE 限速 — 磁盘/同分片/数量限制等约束可插拔**。AllocationDecider.java:32-91 + allocation/decider/ (19 文件)

跨域关联: E-10 ClusterState (分配结果发布)
