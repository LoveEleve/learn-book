# E-6 闭环笔记 Q1-Q4: 双 checkpoint/位图水位/globalCheckpoint/复制重试

## Q1: 为什么需要双 checkpoint (processed/persisted)?

假设: processed 是内存处理完 (可参与 globalCheckpoint), persisted 是 fsync 落盘 (崩溃恢复点) — 两者分离因为 translog 异步 sync。

验证过程:
- Read LocalCheckpointTracker (LocalCheckpointTracker.java:13-27): processedSeqNo/persistedSeqNo 两个 CountedBitSet map + processedCheckpoint/persistedCheckpoint 两个 AtomicLong
- 消费路径 (InternalEngine.java:1243-1248): `markSeqNoAsProcessed` (L1243) → 若无 translogLocation (来自 translog/无 seqNo) 立即 `markSeqNoAsPersisted` (L1247)
- translog sync 回调 (InternalEngine.java:255): translog 每次 fsync 后 `tracker.markSeqNoAsPersisted(seqNo)` — E-3 的 persistedSequenceNumberConsumer 衔接
- 语义: processed = 引擎已处理 (versionMap 已更新); persisted = translog 已落盘 (可恢复)

代码类型: Interface (持久化契约)

结论: **双 checkpoint 分离"逻辑处理完成"与"物理持久化完成": processed 推进 globalCheckpoint (复制可用), persisted 只随 translog fsync 推进 (恢复安全) — REQUEST durability 下两者几乎同步, ASYNC 下 persisted 滞后最多 5s**。LocalCheckpointTracker.java:13-27 + InternalEngine.java:1243-1248,255

跨域关联: E-3 Translog (persistedSequenceNumberConsumer) / E-5 Shard (恢复水位)

## Q2: CountedBitSet 乱序水位怎么推进?

假设: 位图记录已处理 seqNo, checkpoint 只在"下一个连续号已处理"时跳跃推进。

验证过程:
- Read markSeqNo (LocalCheckpointTracker.java:112-127): 乱序标记 `bitSet.set(offset)` (LocalCheckpointTracker.java:121) → 仅当 `seqNo == checkPoint.get() + 1` 时 updateCheckpoint (LocalCheckpointTracker.java:124-126)
- Read updateCheckpoint (LocalCheckpointTracker.java:191-218): do-while 连续跳跃 — "keep it simple for now, get the checkpoint one by one" (LocalCheckpointTracker.java:195); 段边界清理 (LocalCheckpointTracker.java:209)
- BIT_SET_SIZE = 1024 (LocalCheckpointTracker.java:25) — 每段 1024 号, 段满即删 (内存回收)
- 测试实证: testSimplePrimaryProcessed (LocalCheckpointTrackerTests.java:45-67): seqNo2 先标 → checkpoint 保持 0; seqNo1 补齐 → checkpoint 跳 2 (LocalCheckpointTrackerTests.java:62-66)

代码类型: Algorithmic (乱序窗口 + 连续水位)

结论: **乱序完成的 seqNo 记录在位图, checkpoint 只在"窗口内连续前缀"完成时一次性跳跃 — 保证"≤checkpoint 的所有 seqNo 必已处理"的不变式; 1024 分段让位图内存随水位滚动释放**。LocalCheckpointTracker.java:112-127,191-218

## Q3: globalCheckpoint 怎么推进?

假设: globalCheckpoint = 所有 in-sync 副本 localCheckpoint 的最小值; pendingInSync 存在时回退。

验证过程:
- Read computeGlobalCheckpoint (ReplicationTracker.java:1349-1370): 遍历 checkpoints, 只取 inSync 副本 (ReplicationTracker.java:1359), min (ReplicationTracker.java:1365); pendingInSync 非空 → 返回 fallback (ReplicationTracker.java:1356-1358); UNASSIGNED 的 in-sync → fallback (ReplicationTracker.java:1360-1362)
- 触发: updateGlobalCheckpointOnPrimary (定义 ReplicationTracker.java:1375) / updateGlobalCheckpointForShard (ReplicationTracker.java:1066) / 副本更新 (ReplicationTracker.java:1042)
- 断言 (ReplicationTracker.java:830-832): "global checkpoint is not up-to-date" — 主模式必须等于计算值
- 关键语义: globalCheckpoint 是"所有活跃副本都确认"的水位 — 高于它的操作可被安全丢弃 (peer recovery 起点)

代码类型: Algorithmic (跨副本 min 聚合)

结论: **globalCheckpoint = min(in-sync 副本 localCheckpoint), 任一 in-sync 副本未初始化 (UNASSIGNED) 或 pendingInSync 时不可推进 — 这是"所有活跃副本一致"的严格保证, 低于它的操作绝不可能丢**。ReplicationTracker.java:1349-1370

跨域关联: E-5 Shard (ReplicationGroup inSync 集合) / E-3 (恢复起点)

## Q4: ReplicationOperation 副本失败怎么处理?

假设: 副本失败分两级 — 瞬时失败重试 (pendingReplicationActions), 永久失败标记 stale。

验证过程:
- Read execute (ReplicationOperation.java:107-126): checkActiveShardCount (ReplicationOperation.java:109) → primary.perform (ReplicationOperation.java:124)
- Read handlePrimaryResult (ReplicationOperation.java:129-176): 复制组确定 (ReplicationOperation.java:136) → 不可用副本标 stale (ReplicationOperation.java:171) → performOnReplicas (ReplicationOperation.java:172)
- Read performOnReplica (ReplicationOperation.java:230-280): onResponse → updateCheckPoints + decPendingAndFinishIfNeeded (ReplicationOperation.java:236-238); onFailure → failShardIfNeeded (ReplicationOperation.java:276) + retryable 判定
- 重试机制: pendingReplicationActions (E-5 组件) 暂存待重试副本操作
- 测试: testRetryTransientReplicationFailure (ReplicationOperationTests.java:162) — 瞬时失败重试成功

代码类型: Implementation (两阶段复制状态机)

结论: **复制 = 主执行 → 组内副本分发 → ack 聚合; 失败副本按异常类型分流 — 瞬时故障 (transport 级) 进 pendingReplicationActions 重试, 数据不一致 (shard 失败) 标记 stale 移出 in-sync 集**。ReplicationOperation.java:107-176,230-280

跨域关联: E-5 Shard (ReplicationGroup/pendingReplicationActions)
