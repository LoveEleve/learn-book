# E-6 SeqNo 篇 2/3 — 跨副本: globalCheckpoint 与复制

> 前置: [[E-6-seqno-01]] (位点体系) | 复用: — | 对照: [[r9-replication]] (Redis 主从) | 引出: [[E-6-seqno-03]] [[E-5-shard]] (ReplicationGroup)
> 🔴 A | 来源: ReplicationTracker.java:147,1349-1370 + ReplicationOperation.java:107-176,230-280
> 定位: SeqNo 卷中篇 — 回答"globalCheckpoint 怎么聚合? 副本失败怎么处理?"

**读者处境**: 你有 3 个副本, 每个有自己的 localCheckpoint — 面试官问 "globalCheckpoint 是什么? 谁算的? 副本挂了怎么办?" 你答 "所有副本 local 的最小值" — 但为什么 pendingInSync 时不能推进? 副本失败重试机制是什么? 这篇是跨副本协调的完整答案。

### 1. 问题引入 — 从单分片到多副本

场景: 主分片处理了 seqNo 100, 但副本只到 90 — 数据安全水位在哪?
- localCheckpoint = 单分片处理水位 (篇 1)
- globalCheckpoint = 所有活跃副本都确认的水位 (ReplicationTracker.java:147 volatile)
- 语义: 低于 globalCheckpoint 的操作绝不可能丢 (所有副本都有)

### 2. globalCheckpoint 计算 — min of in-sync

场景: 谁算 globalCheckpoint? 怎么算?
- computeGlobalCheckpoint (ReplicationTracker.java:1349-1370): 遍历副本 CheckpointState, 只取 inSync (ReplicationTracker.java:1359), min (ReplicationTracker.java:1365)
- 两个回退 (ReplicationTracker.java:1356-1362): pendingInSync 非空 → fallback; in-sync 副本 UNASSIGNED → fallback — **不完整就不推进**
- 触发点: updateGlobalCheckpointOnPrimary (定义 ReplicationTracker.java:1375, 触发调用 L1098) / updateGlobalCheckpointForShard (ReplicationTracker.java:1066) / 副本上报 (ReplicationTracker.java:1042)
- 断言 (ReplicationTracker.java:830-832): 主模式 globalCheckpoint 必须等于计算值

### 3. ReplicationOperation — 两阶段复制

场景: 一次写操作怎么复制到副本?
- execute (ReplicationOperation.java:107-126): checkActiveShardCount (ReplicationOperation.java:109) → primary.perform (ReplicationOperation.java:124)
- handlePrimaryResult (ReplicationOperation.java:129-176): 主执行完成 → 确定复制组 (ReplicationOperation.java:136) → 不可用副本标 stale (ReplicationOperation.java:171) → performOnReplicas (ReplicationOperation.java:172)
- performOnReplica (ReplicationOperation.java:230-280): onResponse → updateCheckPoints + decPendingAndFinishIfNeeded (ReplicationOperation.java:236-238)
- 复制组来源: primary.getReplicationGroup (ReplicationOperation.java:136) — E-5 的 ReplicationGroup (inSync 集合)

### 4. 失败处理 — 重试 vs stale

场景: 副本写失败了?
- onFailure (ReplicationOperation.java:251-280): 非 shard 不可用异常记录 failure → failShardIfNeeded (ReplicationOperation.java:276) → 可重试的进 pendingReplicationActions
- **瞬时失败重试**: transport 级异常 (网络抖动) → pendingReplicationActions 暂存, 副本恢复后补发
- **永久失败 stale**: shard 数据不一致 → 标记 stale, 移出 in-sync 集 → 后续经 peer recovery 从 globalCheckpoint+1 重放补齐 (E-5 展开)
- 测试: testRetryTransientReplicationFailure (ReplicationOperation.java:162)

### 5. 收束 — 一致性的锚点

- globalCheckpoint = "所有活跃副本一致"的严格保证 — peer recovery 从它 +1 开始
- 对照 Redis: 主从异步复制无全局水位 (篇 3 详述)
- 引出: 篇 3 (租约保护历史 / 脑裂防护 / Redis 对照)

### 核心悬念
"三个副本一个慢了, globalCheckpoint 还推进吗?" — 不推进 (min 聚合), 直到慢副本追平或移出 in-sync — 这就是"所有活跃副本一致"的代价。

### 概念依赖链
Q3 globalCheckpoint min 聚合 → Q4 两阶段复制 + 失败分流 → (E-5 ReplicationGroup 衔接)

### 源码锚点清单
- ReplicationTracker.java:147 (globalCheckpoint volatile) / 1349-1370 (computeGlobalCheckpoint) / 1356-1362 (双回退) / 1375 (updateGlobalCheckpointOnPrimary 定义, 调用 L1098) / 1066 (updateGlobalCheckpointForShard) / 830-832 (断言)
- ReplicationOperation.java:107-126 (execute) / 129-176 (handlePrimaryResult) / 172 (performOnReplicas) / 230-280 (performOnReplica + onFailure) / 276 (failShardIfNeeded)
- ReplicationOperationTests.java:162 (testRetryTransientReplicationFailure)
