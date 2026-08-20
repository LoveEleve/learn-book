# E-5 闭环笔记 Q1-Q4: 状态机/操作许可/主升/复制组

## Q1: 5 态状态机的迁移规则

假设: 状态迁移集中在 updateShardState + changeState; 非法转移抛特定异常。

验证过程:
- Read IndexShardState (IndexShardState.java:10-17): CREATED(0)/RECOVERING(1)/POST_RECOVERY(2)/STARTED(3)/CLOSED(5); RELOCATED 曾存在 (IndexShardState.java:15 注释 + IDS[4]=STARTED 兼容 L25-26)
- Read updateShardState (IndexShard.java:493-562): 迁移核心 —
  - 主→副本非法 (IndexShard.java:517-524): "trying to move shard from primary mode to replica mode" IllegalArgumentException
  - POST_RECOVERY→STARTED (IndexShard.java:529-536): `changeState(STARTED, "global state is...")` — **集群状态驱动**
  - relocated 保护 (IndexShard.java:537-562): "cannot safely reactivate primary mode without risking two active primaries"
- Read changeState (IndexShard.java:885-892): mutex 保护 + indexEventListener.indexShardStateChanged 通知
- 恢复入口: recoverFromStore (IndexShard.java:2370) → changeState(RECOVERING) (IndexShard.java:744) → postRecovery (IndexShard.java:1704) → changeState(POST_RECOVERY) (IndexShard.java:1723)

代码类型: Implementation (状态机)

结论: **状态迁移 = 集群状态驱动 (updateShardState) + 本地恢复驱动 (recoverFromStore/postRecovery) 双轨; 关键规则: 主→副本非法 (防双主), POST_RECOVERY→STARTED 由 master 的 active 路由触发, RELOCATED 已并入 STARTED (兼容) — changeState 统一在 mutex 下 + 通知监听器**。IndexShard.java:493-562,885-892

跨域关联: E-10 ClusterState (驱动源) / E-5 recovery (本地驱动)

## Q2: 操作许可 (permits) — 正常并发 + 阻塞全占

假设: Semaphore MAX_VALUE 让正常操作几乎不互斥; blockOperations 时 delay 新操作 + 全占信号量。

验证过程:
- Read IndexShardOperationPermits (IndexShardOperationPermits.java:49-50): `TOTAL_PERMITS = Integer.MAX_VALUE` + `Semaphore(MAX_VALUE, true)` (fair 防饿死)
- Read blockOperations (IndexShardOperationPermits.java:82-116): delayOperations (IndexShardOperationPermits.java:83, 排队延迟新操作) → waitUntilBlocked (IndexShardOperationPermits.java:85, executor 异步) → acquireAll (IndexShardOperationPermits.java:110)
- Read acquireAll (IndexShardOperationPermits.java:138-153): `semaphore.tryAcquire(TOTAL_PERMITS, timeout)` (IndexShardOperationPermits.java:145) — **全占 = 无可用 permit = 所有在途操作已完成**; 释放时 release(TOTAL_PERMITS) (IndexShardOperationPermits.java:149)
- 正常获取: 单 permit (IndexShardOperationPermits.java:259-260) — 并发写几乎不互斥
- 测试: testClosesPreventsNewOperations (IndexShardTests.java:335-364): close 后 acquire 全抛 IndexShardClosedException

代码类型: Implementation (信号量协调)

结论: **permit 机制 = 双模式: 正常时 MAX_VALUE 信号量让写操作并发 (每次取 1); 阻塞操作 (close/promotion/recovery) 时先 delay 新操作再全占 (取 MAX_VALUE = 等所有在途完成) — "先排队后全占"保证无新操作插入**。IndexShardOperationPermits.java:49-50,82-153

## Q3: 主升主降 (promotion) — 副本升主的三件事

假设: 副本升主 = term 递增 + 阻塞操作 + resync (补洞)。

验证过程:
- term 递增: updateShardState L576 断言 "term is only increased as part of primary promotion"; IndexShard.java:456-462 (getPendingPrimaryTerm 语义)
- resync 标志: primaryReplicaResyncInProgress (IndexShard.java:748) — promotion 时 CAS 启动 (IndexShard.java:609), 完成后复位 (IndexShard.java:658,664)
- 注释 (IndexShard.java:630-634): "If this shard was serving as a replica shard when another shard was promoted... we have to restore" — 前主失败后二次 promotion 需恢复
- 测试: testPrimaryPromotionDelaysOperations (IndexShardTests.java:440) — 主升时操作被延迟
- 相关: testPrimaryFillsSeqNoGapsOnPromotion (IndexShardTests.java:592) — **升主时补 seqNo 洞** (gap 填充)

代码类型: Implementation (角色切换)

结论: **promotion = term+1 (E-6 衔接) + blockOperations (等旧操作排空) + resync (主副本对齐) + seqNo 补洞 — 四步保证旧主失败后副本安全接任**。IndexShard.java:576,609,630-634,748

跨域关联: E-6 SeqNo (term/gap) / E-4 Routing (分配)

## Q4: ReplicationGroup — inSync 集合与派生目标

假设: inSyncAllocationIds 是"已确认同步"的副本集合; replicationTargets/skippedShards 由路由表派生。

验证过程:
- Read ReplicationGroup (ReplicationGroup.java:23-29): inSyncAllocationIds + trackedAllocationIds (ReplicationGroup.java:24-25) + 派生: unavailableInSyncShards/replicationTargets/skippedShards (ReplicationGroup.java:28-29)
- 派生逻辑 (ReplicationGroup.java:43-80): unavailableInSyncShards = inSync - promotable (ReplicationGroup.java:43); 遍历路由: primary→target (ReplicationGroup.java:51-58 附近); relocationTarget→target (ReplicationGroup.java:63-68); 非 inSync→skipped (ReplicationGroup.java:71-74)
- 消费方: E-6 ReplicationOperation.handlePrimaryResult (ReplicationOperation.java:136) `primary.getReplicationGroup()` → performOnReplicas 用 replicationTargets
- 更新: updateFromMaster (E-5 L523 附近) 从 master 接收 inSync 集合 (E-10 衔接)

代码类型: Implementation (派生快照)

结论: **ReplicationGroup = 复制目标的不可变快照: inSync (数据一致) + tracked (跟踪中) 两个集合输入, replicationTargets (要复制) / skippedShards (跳过) / unavailableInSyncShards (失效) 三个派生输出 — 每次复制操作都基于当前快照, 避免复制中路由变化**。ReplicationGroup.java:23-80

跨域关联: E-6 ReplicationOperation (消费) / E-10 ClusterState (updateFromMaster 来源)
