# E-4 闭环笔记 Q5-Q8: 平衡算法/分配流程/Redis 对照/读写路由

## Q5: BalancedShardsAllocator — 权重平衡

假设: 节点权重 = shard 平衡 + index 平衡 加权, 低于阈值才移动分片。

验证过程:
- Read BalancedShardsAllocator 注释 (BalancedShardsAllocator.java:65-77): "balance is defined by four parameters" — shard/index/threshold
- 设置 (BalancedShardsAllocator.java:84-112): `balance.shard` (BalancedShardsAllocator.java:91) / `balance.index` (BalancedShardsAllocator.java:84) / `balance.threshold` (BalancedShardsAllocator.java:112)
- WeightFunction (BalancedShardsAllocator.java:76-77): "calculation of node weights... re-balance shards based on global as well as per-index factors"
- 阈值 (BalancedShardsAllocator.java:123,145-156): threshold volatile + ensureValidThreshold (BalancedShardsAllocator.java:155-156, ≥1)

代码类型: Algorithmic (权重平衡)

结论: **平衡 = 节点权重 (shard 数权重 + 每索引权重) 方差最小化; 仅当不平衡超阈值 (balance.threshold, 默认 ≥1) 才移动分片 — 防止频繁抖动**。BalancedShardsAllocator.java:65-77,84-112,145-156

## Q6: 分配流程 — reroute 时序

假设: 节点增删/分片变化 → reroute: 释放死节点分片 → 分配未分配 → 平衡 → 更新路由表。

验证过程:
- Read AllocationService.reroute (AllocationService.java:388-410): shardsAllocator.allocate (AllocationService.java:392) — 入口
- executeWithRoutingAllocation (AllocationService.java:404-410): 包装 reroute 策略 (RerouteStrategy)
- 未分配处理 (AllocationService.java:541-553): allocateUnassigned (AllocationService.java:541 主 / L553 副本) — 主先分配副本后
- 时序: 死节点检测 → 分配器 → 决策器投票 (Q4) → 平衡 (Q5) → 新路由表发布 (E-10)

代码类型: Implementation (分配编排)

结论: **reroute = 死节点分片释放 → 未分配分片分配 (主先副后 L541/553) → 决策器投票 (Q4) → 平衡器 (Q5) → 路由表更新发布 — 每次集群拓扑变化触发**。AllocationService.java:388-410,541-553

跨域关联: E-10 ClusterState (发布) / E-5 Shard (状态迁移)

## Q7: 与 Redis 对照 — 分片路由 vs 无路由

假设: Redis Cluster 用 slot 哈希 (16384), ES 用路由表 + 分配器 — 两种分布式坐标。

验证过程:
- Redis Cluster: 16384 slots 哈希到节点 (CRC16) — 固定哈希无移动
- ES: RoutingTable 显式路由 (索引→分片→节点) + 分配器动态移动
- 对照维度:
  - 定位: Redis CRC16(slot) 确定性; ES 路由表显式
  - 移动: Redis slot 迁移 (手动/reshard); ES 自动平衡 (Q5)
  - 副本: Redis 主从 (replicaof); ES 副本组 (IndexShardRoutingTable)
- 面试记忆点: "Redis 用哈希找位置, ES 用表找位置 — 一个算出来, 一个查出来"

代码类型: 对照分析

结论: **Redis Cluster = 确定性哈希 (CRC16 slot, 无表); ES = 显式路由表 + 动态分配 (平衡/决策器) — 哈希定位简单但移动难, 路由表灵活但需协调 (E-10)**。对照锚点: RoutingTable.java:45 + Redis cluster.c

## Q8: 写路由 vs 读路由 — 对称性

假设: 写固定主分片 (复制到副本), 读可到任意活跃副本 (preference 控制)。

验证过程:
- 写: E-6 ReplicationOperation 主执行 → 副本复制 — 写路由 = 主分片 (哈希 id → 主)
- 读: OperationRouting.getShards (OperationRouting.java:63-72) → preferenceActiveShardIterator — 读路由 = preference 选择
- 非对称原因: 写必须单一决策点 (防冲突), 读可分布式 (负载均衡)
- 自适应副本 (OperationRouting.java:39): 读优化 (响应时间), 写不能自适应

代码类型: 对照分析

结论: **写路由固定主分片 (E-6 单一决策点), 读路由 preference 自由选择 (OperationRouting.java:63-72) — 非对称是分布式系统的必然: 写要一致, 读要均衡**。OperationRouting.java:39,63-77

跨域关联: E-6 ReplicationOperation (写) — 读写对照闭环
