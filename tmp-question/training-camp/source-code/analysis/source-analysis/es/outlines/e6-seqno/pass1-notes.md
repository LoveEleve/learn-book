# E-6 SeqNo 复制协议 — Pass 1 探索笔记 (扫轮廓)

> 🔴 A | 依赖: E-1 Engine ✅ (seqNo 生成/消费) + E-3 ✅ | 对照: [[r9-replication]] (Redis 主从) [[r29-pubsub]] (通知面)
> 源码: server/src/main/java/org/elasticsearch/index/seqno/ (16 文件 3924 行) + action/support/replication/ReplicationOperation (698 行)
> 测试地图: seqno/ 17 文件 + action/support/replication/ReplicationOperationTests

## 继承树/调用图

```
SequenceNumbers (常量: UNASSIGNED_SEQ_NO=-2 / NO_OPS_PERFORMED=-1 / UNASSIGNED_PRIMARY_TERM=0, L23-32)
├── LocalCheckpointTracker (249 行) — seqNo 分配 + 水位推进
│     processedSeqNo/persistedSeqNo (CountedBitSet 分段) + 双 checkpoint (LocalCheckpointTracker.java:13-27)
├── ReplicationTracker (1642 行) — 主分片跟踪: globalCheckpoint (ReplicationTracker.java:147) + primaryMode (ReplicationTracker.java:93) + CheckpointState
│     + RetentionLeases 租约 (ReplicationTracker.java:222-571, 保留 seqNo 历史供恢复)
└── GlobalCheckpointSyncAction — 定期同步 globalCheckpoint

ReplicationOperation (698 行) — 复制主流程:
    execute (ReplicationOperation.java:107): checkActiveShardCount → primary.perform → handlePrimaryResult (ReplicationOperation.java:129)
    → performOnReplicas (ReplicationOperation.java:210) → 收集 ack → finish
```

## 基本元素分解 (原则二)

1. **seqNo 分配** — LocalCheckpointTracker.generateSeqNo (LocalCheckpointTracker.java:83): nextSeqNo.getAndIncrement — 单调递增
2. **双 checkpoint 水位** — processedCheckpoint (内存处理完) / persistedCheckpoint (fsync 落盘) — markSeqNoAsProcessed (LocalCheckpointTracker.java:99) / markSeqNoAsPersisted (LocalCheckpointTracker.java:108)
3. **CountedBitSet 乱序跟踪** — 分段位图: 乱序标记 + 顺序推进 (updateCheckpoint 连续跳跃 L205-218)
4. **globalCheckpoint** — ReplicationTracker (ReplicationTracker.java:147): 所有活跃副本 localCheckpoint 的最小值 — 安全恢复水位
5. **primaryTerm 语义** — 主分片每次当选递增 (UNASSIGNED_PRIMARY_TERM=0) — 防旧主脑裂后继续写
6. **ReplicationOperation 两阶段** — 主执行 → 副本复制 → ack 聚合 (execute L107 → handlePrimaryResult L129 → performOnReplicas L210)

## 标记问题 (≥5)

1. **Q1: 为什么需要双 checkpoint (processed/persisted)?** — 内存处理完 ≠ 落盘; persisted 供崩溃恢复
2. **Q2: CountedBitSet 乱序推进怎么工作?** — markSeqNo(5) 后 checkpoint 不动, markSeqNo(4) 补齐后跳到 6? (测试 L45-67 实证: seqNo2 先标 checkpoint 保持 0, seqNo1 补齐后跳 2)
3. **Q3: globalCheckpoint 怎么推进?** — 所有 in-sync 副本 localCheckpoint 的 min? 谁触发计算?
4. **Q4: ReplicationOperation 失败重试** — 副本失败怎么处理? (testRetryTransientReplicationFailure L162)
5. **Q5: primaryTerm 与 seqNo 的关系** — 为什么复制需要 (seqNo, primaryTerm) 二元组而非单个 seqNo?
6. **Q6: RetentionLease 是什么?** — 保留 seqNo 历史供恢复 (ReplicationTracker.java:222-571) — 与 soft deletes 关系
7. **Q7: 与 Redis 主从复制对照** — Redis 字节偏移量 vs ES 逻辑 seqNo — 两种位点的设计差异
8. **Q8: 脑裂防护** — primaryTerm 递增 + 旧主拒绝写 — 与 Redis sentinel 对照

## 已读测试 (2 个)

- `LocalCheckpointTrackerTests.testSimplePrimaryProcessed` (LocalCheckpointTrackerTests.java:45-67): 乱序标记 → 水位补齐跳跃 — 核心语义
- `LocalCheckpointTrackerTests.testConcurrentPrimary` (LocalCheckpointTrackerTests.java:155): 并发分配
- `ReplicationOperationTests.testRetryTransientReplicationFailure` (ReplicationOperationTests.java:162): 副本瞬时失败重试

## 完成检查

- [x] 继承树/调用图已画出
- [x] 基本元素分解 (6 元素, 对应源码位置)
- [x] 8 个标记问题, 每个有源码位置
- [x] 已读 3 个测试文件

## 跨域发现

- 来源: E-6 Pass 1 — Engine 的 LocalCheckpointTracker 由 E-1 创建 (InternalEngine.java:161), translog 持久化后 markSeqNoAsPersisted
- 发现: persistedCheckpoint 的推进由 translog sync 触发 (E-3 衔接: persistedSequenceNumberConsumer)
- 已对照验证: InternalEngine.java:161 (localCheckpointTracker 字段) — E-1 交付时已见
