# E-6 闭环笔记 Q5-Q8: primaryTerm/租约/Redis 对照/脑裂

## Q5: 为什么复制需要 (seqNo, primaryTerm) 二元组?

假设: seqNo 单调递增但主分片可能更替 — primaryTerm 标识"第几代主", 两者组合才能唯一定位一次操作。

验证过程:
- Read SequenceNumbers (SequenceNumbers.java:23-32): UNASSIGNED_SEQ_NO=-2 / NO_OPS_PERFORMED=-1 / UNASSIGNED_PRIMARY_TERM=0 — 哨兵值语义
- Read IndexShard.java:231 (pendingPrimaryTerm volatile) + L360-361 (构造时从 metadata 读 primaryTerm)
- E-1 衔接: Engine 生成 seqNo (generateSeqNoForOperationOnPrimary L1105) + translog header 存 primaryTerm (Translog.java:140-142)
- 语义: 新主当选 → primaryTerm+1 (IndexShard.java:569 附近新 primaryTerm 处理) → 旧主的 (seqNo, term) 组合永久失效

代码类型: Interface (分布式标识契约)

结论: **seqNo 是"分片内单调位点", primaryTerm 是"主分片世代号" — 复制/恢复用 (seqNo, primaryTerm) 精确判定操作新旧, 防止旧主脑裂期间的操作污染新主纪元**。SequenceNumbers.java:23-32 + IndexShard.java:231,360

## Q6: RetentionLease (保留租约) 是什么?

假设: soft deletes 会被 merge 清理 — 租约声明"保留到某 seqNo", 供恢复读取历史。

验证过程:
- Read RetentionLease.java:24-27 注释: "all operations with sequence number at least that retaining sequence number will be retained during merge operations"
- 结构 (RetentionLease.java:31-65): id / retainingSequenceNumber / timestamp / source
- ReplicationTracker 管理 (ReplicationTracker.java:222-571): addRetentionLease (ReplicationTracker.java:302) / renewRetentionLease (ReplicationTracker.java:390) / persistRetentionLeases (ReplicationTracker.java:488)
- 关联: soft deletes 保留的删除历史靠租约保护不被 merge 吞掉 (E-8 Merge 衔接)

代码类型: Interface (资源保护契约)

结论: **RetentionLease = "谁需要保留 seqNo ≥ N 的历史"的声明 (peer recovery/CCR 使用), 防止 soft-deleted 操作被 merge 过早清理 — 租约过期后历史才可被合并删除**。RetentionLease.java:24-27,31-65

跨域关联: E-8 Merge (清理保护) / E-5 Shard (recovery 消费)

## Q7: 与 Redis 主从复制对照 — 字节位点 vs 逻辑位点

假设: Redis 用字节偏移 (repl_offset), ES 用逻辑 seqNo — 两种位点设计反映不同复制模型。

验证过程:
- Redis: replication.c:28 (replicationSendAck) — 副本周期性 ACK 自己的 repl_offset (字节流位置)
- ES: seqNo 由 LocalCheckpointTracker.generateSeqNo 分配 (LocalCheckpointTracker.java:83), 每个操作独立编号
- 关键差异:
  - Redis 偏移是**流的物理位置** (断点续传按字节), ES seqNo 是**操作的逻辑序号** (乱序可追踪)
  - Redis 全量重传 (RDB + 增量流), ES peer recovery 按 seqNo 范围 (globalCheckpoint+1 起)
- 面试对比点: "Redis 复制断点怎么定位?" (offset) vs "ES 复制断点?" (globalCheckpoint/seqNo)

代码类型: 对照分析

结论: **字节位点 (Redis) 适合连续流重传, 逻辑位点 (ES seqNo) 支持乱序/部分复制/精确恢复 — 差异源于复制粒度: Redis 复制命令流, ES 复制带序操作**。对照锚点: replication.c:28 vs LocalCheckpointTracker.java:83

跨域关联: [[r9-replication]] (Redis 主从)

## Q8: 脑裂防护 — primaryTerm 与 Redis 对照

假设: 网络分区后旧主可能仍存活 — primaryTerm 递增保证只有新主能写, 旧主操作被拒。

验证过程:
- IndexShard.java:569 (新 primaryTerm 处理) — 旧主恢复连接后发现自己 term 落后 → 降级
- E-1 衔接: planIndexingAsPrimary 中 term 校验 (InternalEngine.java:1358-1367 的 term 冲突分支)
- Redis 对照: Redis 无 term 概念 — 靠哨兵 failover + 旧主降级为副本 (r14-sentinel 交付), 但无"世代号"防旧主继续写
- ES 对照结论: primaryTerm = "分布式锁的 fencing token" 语义 (对照 Redisson FencedLock 思路)

代码类型: 对照分析

结论: **primaryTerm 是 Raft 式 term 在 ES 的落地 — 旧主脑裂期间的写操作因 term 落后被副本/恢复拒绝; Redis 靠哨兵检测但无 term 隔离, 这是两者一致性强度的分水岭**。IndexShard.java:569 + InternalEngine.java:1358-1367

跨域关联: [[r14-sentinel]] (Redis 哨兵) / [[rd2-rlock]] (fencing token)
