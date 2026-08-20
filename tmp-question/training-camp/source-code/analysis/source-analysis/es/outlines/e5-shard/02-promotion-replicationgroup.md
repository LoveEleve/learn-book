# E-5 Shard 篇 2/3 — 主升与复制组: 副本怎么接任

> 前置: [[E-5-shard-01]] (状态机) [[E-6-seqno-02]] (复制/globalCheckpoint) | 复用: — | 对照: [[r14-sentinel]] (Redis failover) | 引出: [[E-5-shard-03]] [[E-10-clusterstate]]
> 🔴 A | 来源: IndexShard.java:576,609,748 + ReplicationGroup.java:23-80 + GlobalCheckpointSyncer.java:13-24
> 定位: Shard 卷中篇 — 回答"副本怎么升主? 复制组怎么定目标?"

**读者处境**: 主分片挂了, 副本要接管 — 面试官问 "ES 主分片故障怎么恢复? 副本升主会丢数据吗?" "复制到哪些副本?" 你答 "promotion + replication group" — 这篇是主升与复制组编排的完整答案。

### 1. 问题引入 — 主挂了之后

场景: 主分片所在节点宕机, 副本怎么接任?
- 三件事: term 递增 (E-6 衔接) / 阻塞操作排空 / resync 对齐 + seqNo 补洞
- 复制目标: ReplicationGroup 决定"写操作复制到谁"
- 本篇问题: promotion 四步 (Q3) + 复制组派生 (Q4) + gcp 同步 (Q8)

### 2. 主升 (promotion) — 四步接任

场景: 副本升主具体做什么?
- term+1: "term is only increased as part of primary promotion" (IndexShard.java:576 断言) — E-6 脑裂防护
- 阻塞操作: primaryReplicaResyncInProgress (IndexShard.java:748) CAS 启动 (IndexShard.java:609) — blockOperations 等旧操作排空
- resync: 主副本对齐 — 前主失败后二次 promotion 需恢复 (注释 L630-634)
- seqNo 补洞: testPrimaryFillsSeqNoGapsOnPromotion (IndexShardTests.java:592) — 升主时填充 gap
- 测试: testPrimaryPromotionDelaysOperations (IndexShardTests.java:440) — 升主期间操作延迟

### 3. 复制组 — inSync 集合与派生目标

场景: 写操作复制到哪些副本?
- 输入 (ReplicationGroup.java:23-29): inSyncAllocationIds (已确认同步) + trackedAllocationIds (跟踪中)
- 派生 (ReplicationGroup.java:43-80): unavailableInSyncShards = inSync - promotable (ReplicationGroup.java:43) / replicationTargets / skippedShards
- 消费: E-6 ReplicationOperation.handlePrimaryResult (ReplicationOperation.java:136) → performOnReplicas 用 replicationTargets
- 更新: updateFromMaster (IndexShard.java:526) 从 master 接收 inSync 集合 (E-10 衔接)
- 关键: 复制组是**不可变快照** — 复制过程中路由变化不生效

### 4. globalCheckpoint 同步 — 写入停止的补丁

场景: 写入停了, 副本的 globalCheckpoint 会滞后吗?
- 触发 (GlobalCheckpointSyncer.java:13-24 javadoc): ① 写入停止 (gcp 追上 maxSeqNo 但副本不知道) ② ASYNC durability
- 实现: GlobalCheckpointSyncAction (E-6) — 显式同步补上
- 语义: 没有这个同步, 副本 gcp 永远滞后 → 恢复/租约判断失真

### 5. 收束 — 接任的可靠性

- promotion (term+resync+补洞) + 复制组 (inSync 快照) + gcp 同步 — 三位一体保证"升主不丢已确认数据"
- 对照 Redis: 哨兵 failover 无 term/无 gcp (篇 3 详述)
- 引出: 篇 3 (恢复流程 + Redis 对照)

### 核心悬念
"主挂了, 升主会丢数据吗?" — 不会: globalCheckpoint 是"所有活跃副本一致"的水位, promotion 从它之后重放; term 保证旧主物理无法再写。

### 概念依赖链
Q3 promotion 四步 → Q4 复制组派生 → Q8 gcp 同步 → (E-6 衔接)

### 源码锚点清单
- IndexShard.java:576 (term 递增断言) / 609 (resync CAS) / 630-634 (二次 promotion 注释) / 748 (resync 标志) / 523 (updateFromMaster)
- ReplicationGroup.java:23-29 (双集合 + 派生) / 43 (unavailableInSyncShards) / 84 (getInSyncAllocationIds)
- GlobalCheckpointSyncer.java:13-24 (同步触发场景)
- ReplicationOperation.java:136 (getReplicationGroup 消费)
- IndexShardTests.java:440 (testPrimaryPromotionDelaysOperations) / 592 (testPrimaryFillsSeqNoGapsOnPromotion)
