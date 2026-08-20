# E-5 闭环笔记 Q5-Q8: 恢复/关闭/哨兵对照/gcp 同步

## Q5: 恢复流程 — recoverFromStore → StoreRecovery → postRecovery

假设: 恢复 = Store (Lucene 段) 打开 + translog 回放 + 状态推进到 POST_RECOVERY。

验证过程:
- Read IndexShard.recoverFromStore (IndexShard.java:2370-2380): `new StoreRecovery(shardId, logger)` (IndexShard.java:2375) → 异步恢复
- StoreRecovery (616 行) — 段恢复编排
- postRecovery (IndexShard.java:1704): 恢复完成后 → changeState(POST_RECOVERY) (IndexShard.java:1723) — 等 master 确认 active 后 updateShardState 推 STARTED (IndexShard.java:529-536)
- recoverLocallyUpToGlobalCheckpoint (IndexShard.java:1748): 本地恢复到 globalCheckpoint 水位
- 恢复范围: Store 段 (已 commit) + translog 回放 (未 commit, E-3 衔接: newSnapshot L657)

代码类型: Implementation (恢复编排)

结论: **恢复两段: Store 打开已提交段 (Lucene) → translog 回放未提交操作 (E-3) → POST_RECOVERY; 之后等集群状态 active 才 STARTED — 本地恢复与集群确认分离**。IndexShard.java:2370-2380,1704,1723 + StoreRecovery

## Q6: 关闭语义 — close 后全拒

假设: close = 状态 CLOSED + 引擎关闭 + permits 关闭; 之后所有 acquire 抛 IndexShardClosedException。

验证过程:
- Read close (IndexShard.java:1672-1690): changeState(CLOSED) (IndexShard.java:1676) → flushEngine 时 engine.flushAndClose (IndexShard.java:1682-1683) → IOUtils.close(engine, listeners, pendingActions) (IndexShard.java:1688) → indexShardOperationPermits.close() (IndexShard.java:1689)
- 测试: testClosesPreventsNewOperations (IndexShardTests.java:335-364): acquirePrimaryOperationPermit / acquireAllPrimaryOperationsPermits / acquireReplicaOperationPermit 全抛 IndexShardClosedException
- delayOperations (IndexShardOperationPermits.java:128-136): closed 检查 (L130) "throw new IndexShardClosedException(shardId)" — permits 关闭后拒绝排队

代码类型: Implementation (生命周期终态)

结论: **close 是"先拒新操作 (permits.close) 再排空旧操作 (engine close) 最后通知" 的顺序关闭 — CLOSED 是唯一不可逆终态**。IndexShard.java:1672-1690 + IndexShardOperationPermits.java:136-140

## Q7: 与 Redis 哨兵对照 — 状态机设计差异

假设: Redis failover (哨兵选举) vs ES 分片主升 — 都解决"主故障后谁来接管", 但机制不同。

验证过程:
- Redis: 哨兵检测主挂 → 选举副本升主 → 旧主恢复后降级为副本 (r14-sentinel 已交付); 无 term 概念
- ES: 副本升主 = term+1 (E-6) + resync + seqNo 补洞 (Q3); 旧主恢复 → term 落后 → 拒绝写 (InternalEngine.java:1358-1367)
- 关键差异:
  - Redis 升主后旧主靠"配置重写 + 降级命令", ES 靠 term 硬隔离 (旧主物理无法写)
  - ES 有 globalCheckpoint (所有副本一致水位) 保证升主后不丢已确认数据; Redis 无等价物 (可能丢 async 复制数据)
- 面试对比点: "Redis 主从切换可能丢多少数据?" (async 复制窗口) vs "ES 升主丢数据吗?" (globalCheckpoint 保证)

代码类型: 对照分析

结论: **两者都做"主故障接管", 但 ES 的 term + globalCheckpoint 提供"物理防双主 + 不丢已确认数据"的更强保证 — 差异根因: ES 复制带逻辑位点 (E-6), Redis 复制字节流**。对照锚点: [[r14-sentinel]] + ReplicationTracker.java:1349-1370

## Q8: globalCheckpoint 同步链路

假设: 写入停止时副本的 globalCheckpoint 可能滞后 — 显式同步补上。

验证过程:
- Read GlobalCheckpointSyncer 接口 javadoc (GlobalCheckpointSyncer.java:13-24): "used when indexing traffic stops and the primary's global checkpoint reaches the max seqno... replicas will have an older global checkpoint... may not receive any further updates without the explicit sync"
- 两个触发场景 (javadoc): ① 写入停止 (gcp 追上 maxSeqNo 但副本不知道) ② ASYNC durability (索引流量不推进持久化 gcp)
- 实现: GlobalCheckpointSyncAction (E-6 交付) — 生产环境触发
- 消费者: E-6 maybeSyncGlobalCheckpoint (IndexShard.java:2825-2837 之前验证过)

代码类型: Interface (同步契约)

结论: **globalCheckpoint 同步是"写入驱动"之外的补丁 — 写入流量本身会带 gcp 推进, 但流量停止/ASYNC 模式下副本 gcp 滞后, 需显式 sync 兜底 (GlobalCheckpointSyncer) → GlobalCheckpointSyncAction**。GlobalCheckpointSyncer.java:13-24

跨域关联: E-6 SeqNo (gcp 语义 + sync action)
