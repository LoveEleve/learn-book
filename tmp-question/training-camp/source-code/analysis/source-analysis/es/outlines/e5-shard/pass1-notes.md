# E-5 Shard 生命周期 — Pass 1 探索笔记 (扫轮廓)

> 🔴 A | 依赖: E-3/E-7/E-1/E-6 ✅ 全部就绪 | 对照: [[r14-sentinel]] (Redis 哨兵状态机) [[r20-server]] (服务端骨架)
> 源码: server/src/main/java/org/elasticsearch/index/shard/ (41 文件 10505 行)
> 测试地图: server/src/test/.../index/shard/ (IndexShardTests 30+ 测试)

## 继承树/调用图

```
IndexShard (4242 行) — 分片核心门面
├── 状态机: IndexShardState (5 态: CREATED/RECOVERING/POST_RECOVERY/STARTED/CLOSED, L10-17)
├── 组件: Store (IndexShard.java:195) / Engine (AtomicReference L232) / MapperService (L193) / ReplicationTracker (IndexShard.java:223)
│         + IndexShardOperationPermits (信号量) + GlobalCheckpointSyncer + RetentionLeaseSyncer
├── 路由: shardRouting (IndexShard.java:227 volatile) / state (IndexShard.java:228 volatile)
└── 操作许可: acquirePrimaryOperationPermit / acquireReplicaOperationPermit
             (IndexShardOperationPermits: Semaphore TOTAL_PERMITS=MAX_VALUE, L49-50)

调用链 (写入):
    TransportShardBulkAction → IndexShard.applyIndexOperationOnPrimary (IndexShard.java:894)
    → acquirePrimaryOperationPermit → engine.index (E-1) → replication (E-6)

调用链 (状态):
    ClusterService → IndexShard.updateShardState (IndexShard.java:493) → 状态迁移
    → recoverFromStore (IndexShard.java:2370) → postRecovery (IndexShard.java:1704) → STARTED
```

## 基本元素分解 (原则二)

1. **5 态状态机** — IndexShardState (IndexShardState.java:10-17): CREATED→RECOVERING→POST_RECOVERY→STARTED/CLOSED; updateShardState (IndexShard.java:493) 集中迁移
2. **操作许可信号量** — IndexShardOperationPermits: Semaphore (L50, fair) + TOTAL_PERMITS=MAX_VALUE (L49) — 正常并发, blockOperations 时全占 (IndexShardOperationPermits.java:82-116)
3. **ReplicationGroup** — inSyncAllocationIds/trackedAllocationIds (ReplicationGroup.java:24-25) + replicationTargets/skippedShards 派生 (ReplicationGroup.java:28-29)
4. **主升主降 (promotion)** — updateShardState 中 primary 判定 (IndexShard.java:516-517 附近) + term 处理
5. **全局检查点同步** — GlobalCheckpointSyncer (GlobalCheckpointSyncer.java:24) 定期推 globalCheckpoint 到副本
6. **恢复编排** — recoverFromStore (IndexShard.java:2370) → StoreRecovery → postRecovery (IndexShard.java:1704) → STARTED

## 标记问题 (≥5)

1. **Q1: 5 态状态机的迁移规则** — updateShardState 里哪些转移合法? (POST_RECOVERY→STARTED / 迁移 / 关闭?) 非法转移抛什么?
2. **Q2: 操作许可 (permits) 机制** — Semaphore MAX_VALUE 怎么做到"正常并发 + 阻塞时全占"? blockOperations 的目的? (testClosesPreventsNewOperations L335)
3. **Q3: 主升主降 (promotion/demotion)** — 副本升主时怎么处理? term 递增 + 阻塞操作 + seqNo 补洞? (testPrimaryPromotion L440)
4. **Q4: ReplicationGroup 的 inSync 集合维护** — 谁增删 inSyncAllocationIds? 新副本怎么进 inSync? (E-6 globalCheckpoint 依赖)
5. **Q5: 恢复流程** — recoverFromStore → StoreRecovery → translog 回放 → postRecovery — 各阶段做什么?
6. **Q6: 关闭语义** — close 后所有操作拒绝 (testClosesPreventsNewOperations L335: acquire*Permit 全抛 IndexShardClosedException)
7. **Q7: 与 Redis 哨兵对照** — Redis 主从 failover vs ES 分片主升主降 — 状态机设计差异
8. **Q8: globalCheckpoint 同步链路** — GlobalCheckpointSyncer 怎么触发? (E-6 衔接)

## 已读测试 (2 个)

- `IndexShardTests.testClosesPreventsNewOperations` (IndexShardTests.java:335-364): close 后 acquire 全抛 IndexShardClosedException
- `IndexShardTests.testPrimaryPromotionDelaysOperations` (IndexShardTests.java:440): 主升时操作被延迟
- `IndexShardTests.testWriteShardState` (IndexShardTests.java:220): 状态持久化

## 完成检查

- [x] 继承树/调用图已画出
- [x] 基本元素分解 (6 元素, 对应源码位置)
- [x] 8 个标记问题, 每个有源码位置
- [x] 已读 3 个测试文件

## 跨域发现

- 来源: E-5 Pass 1 — IndexShard.updateShardState (IndexShard.java:493) 是集群状态应用的入口 (E-10 ClusterState 衔接)
- 发现: replicationTracker.updateFromMaster (IndexShard.java:526 附近) — 主分片从 master 接收 inSync 集合
- 已对照验证: IndexShard.java:523 (updateFromMaster 调用)
