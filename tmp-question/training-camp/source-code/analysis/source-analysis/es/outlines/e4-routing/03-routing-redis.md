# E-4 Cluster Routing 篇 3/3 — 路由与对照: 读写路由/Redis Cluster

> 前置: [[E-4-routing-01]] [[E-4-routing-02]] [[E-6-seqno-02]] (复制) | 复用: — | 对照: [[r14-sentinel]] [[rd1-connection]] | 引出: [[E-10-clusterstate]]
> 🔴 A | 来源: OperationRouting.java:36-77,206-240 + RoutingTable.java:45 + E-6 ReplicationOperation
> 定位: Routing 卷收尾 — 回答"读写路由怎么选? 和 Redis Cluster 差在哪?"

**读者处境**: 路由表建好了 (篇 1), 分片分配完了 (篇 2) — 面试官问 "一次读写请求怎么路由到分片? preference 是什么? 自适应副本?" 你答 "哈希+随机" — 但再问 "和 Redis Cluster 的 slot 有什么本质区别?" 你答不上来。这篇是路由使用 + Redis 对照的完整答案, 收束 Routing 域。

### 1. 问题引入 — 地图建好了怎么用

场景: 一次 GET /index/doc — 请求怎么到正确的分片?
- 哈希: id → shard (Murmur3)
- 路由表: shard → 节点 (篇 1)
- 本篇问题: 读路由偏好 (Q3) / Redis 对照 (Q7) / 读写对称 (Q8)

### 2. 读路由 — preference 驱动的副本选择

场景: 主分片和副本都在, 读哪个?
- OperationRouting.getShards (OperationRouting.java:63-77): preference 参数 → preferenceActiveShardIterator (OperationRouting.java:72)
- preference 解析 (OperationRouting.java:206-240): 空 → 默认; '_' 前缀 → Preference.parse (OperationRouting.java:218) — _only_nodes/_local/_shards
- 自适应副本 (OperationRouting.java:39): use_adaptive_replica_selection — 按响应时间选副本
- 返回 ShardIterator: 候选副本迭代器 (遍历直到成功)

### 3. 写路由 — 固定主分片

场景: 写操作去哪?
- 写固定主分片: id 哈希 → 主分片 → E-6 ReplicationOperation 复制到副本
- 非对称原因: 写要单一决策点 (防冲突), 读可分布式 (负载均衡)
- 对照: 读可到任意活跃副本, 写只能主分片

### 4. 与 Redis Cluster 对照 — 哈希 vs 路由表

场景: Redis Cluster 和 ES 的路由有什么本质区别?
- Redis: CRC16(slot) 确定性哈希 (16384) — 算出来, 无表
- ES: RoutingTable 显式 (索引→分片→节点) — 查出来, 动态
- 对照维度:
  - 定位: Redis 哈希计算 / ES 表查找
  - 移动: Redis slot 迁移 (手动/reshard) / ES 自动平衡 (篇 2)
  - 副本: Redis 主从 (replicaof) / ES 副本组 (IndexShardRoutingTable)
- 面试记忆点: "Redis 用哈希找位置, ES 用表找位置 — 一个算出来, 一个查出来"

### 5. 收束 — Routing 域总结

- 路由表 (篇 1): 三级结构 + 版本化 + 两层状态
- 分配 (篇 2): 决策器投票 + 平衡器优化
- 路由 (本篇): 读写 preference + Redis 对照
- 引出: E-10 ClusterState (路由表随集群状态发布 — 收束全卷)

### 核心悬念
"Redis 和 ES 都是分布式, 路由方式为什么不同?" — Redis 数据无 schema 可哈希定位 (slot 固定); ES 分片有大小/状态/副本, 需要显式表 + 动态分配 — "算出来" vs "查出来" 是两种数据模型的自然结果。

### 概念依赖链
Q3 读路由 → Q8 读写对照 → Q7 Redis 对照 → (E-10 衔接)

### 源码锚点清单
- OperationRouting.java:36 (类) / 39 (use_adaptive_replica_selection) / 63-77 (getShards 双重载) / 72 (preferenceActiveShardIterator) / 206-240 (preference 解析) / 218 (Preference.parse)
- RoutingTable.java:45 (类, 对照锚点)
- E-6 ReplicationOperation.java:107 (写路由消费)
