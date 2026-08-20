# E-6 SeqNo 篇 3/3 — 保护与对照: 租约/脑裂/Redis

> 前置: [[E-6-seqno-01]] [[E-6-seqno-02]] | 复用: — | 对照: [[r9-replication]] [[r14-sentinel]] [[rd2-rlock]] | 引出: [[E-5-shard]] [[E-8-merge]]
> 🔴 A | 来源: RetentionLease.java:24-27,31-65 + IndexShard.java:231,360,569 + replication.c:28
> 定位: SeqNo 卷收尾 — 回答"历史怎么保留? 脑裂怎么防? 与 Redis 差在哪?"

**读者处境**: ES 的复制和 Redis 的主从复制到底差在哪? 面试官问 "Redis 复制断点怎么定位? ES 呢?" "ES 怎么防脑裂?" — 前两篇讲完了位点和复制, 这篇收束: 历史保留 (租约)、脑裂防护 (primaryTerm)、Redis 对照 (两种位点哲学)。

### 1. 问题引入 — 复制之外的三个问题

场景: 复制解决了"副本跟上主", 但还有三个问题:
- 已删除文档的历史怎么保留 (恢复要读)?
- 网络分区后旧主还在写怎么办?
- 和 Redis 的复制设计比, ES 强在哪?
- 本篇三个答案: RetentionLease / primaryTerm / 位点哲学

### 2. RetentionLease — 历史保留声明

场景: soft-deleted 文档会被 merge 清理, 但恢复还要读?
- 定义 (RetentionLease.java:24-27): "all operations with sequence number at least that retaining sequence number will be retained during merge operations"
- 结构 (RetentionLease.java:31-65): id / retainingSequenceNumber / timestamp / source — "谁在保留到哪个 seqNo"
- 管理 (ReplicationTracker.java:302 add / L390 renew / L488 persist) — 租约持久化防重启丢失
- 语义: peer recovery/CCR 声明"我还要读 seqNo ≥ N 的历史" → merge 不得清理 (E-8 衔接)
- **过期后果**: 租约过期 → 历史被 merge 清理 → 依赖它的恢复/CCR 失败 (需全量恢复)
- **对照**: Redis 无租约等价物 — 其副本不读历史 (全量 RDB 同步), 无需保留 soft-deleted 数据

### 3. 脑裂防护 — primaryTerm 世代号

场景: 网络分区后旧主恢复了连接, 还能继续写吗?
- 存储 (IndexShard.java:231 pendingPrimaryTerm volatile) — 构造时从 metadata 读 (L360-361)
- 递增: 新主当选 → term+1 (IndexShard.java:569 附近) — 旧主发现 term 落后 → 降级
- E-1 衔接: plan 阶段 term 冲突拒绝 (InternalEngine.java:1358-1367)
- **对照 Redisson FencedLock**: primaryTerm 就是"分布式锁的 fencing token" — 世代号让旧持有者失效 ([[rd2-rlock]])

### 4. 与 Redis 复制对照 — 两种位点哲学

场景: Redis 和 ES 的复制断点定位?
- Redis: 字节偏移 repl_offset (replication.c:28 replicationSendAck) — **物理流位置**
- ES: seqNo + primaryTerm — **逻辑操作位点**
- 对比维度:
  - 断点: Redis 按字节续传 (流式); ES 按 seqNo 范围 (精确到操作)
  - 乱序: Redis 不支持 (顺序流); ES 支持 (位图窗口)
  - 部分复制: Redis 全量+增量; ES peer recovery 从 globalCheckpoint+1
- 脑裂: Redis 靠哨兵检测 ([[r14-sentinel]]), 无 term 隔离; ES primaryTerm 硬隔离
- 结论: 位点设计 = 复制粒度 (命令流 vs 操作集) 的直接结果

### 5. 收束 — SeqNo 域总结

- 位点体系 (篇 1): seqNo 分配 + 双 checkpoint
- 跨副本 (篇 2): globalCheckpoint min 聚合 + 复制状态机
- 保护 (本篇): 租约保历史 + term 防脑裂
- 引出: E-5 Shard (ReplicationGroup 消费 globalCheckpoint) — E-8 Merge (租约保护清理)

### 核心悬念
"Redis 复制比 ES 简单, 为什么 ES 要用 seqNo?" — 因为 ES 复制的是"带序操作集"而非"命令流": 支持乱序、部分恢复、精确断点 — 复杂度换一致性粒度。

### 概念依赖链
Q6 租约 → Q8 脑裂对照 → Q7 Redis 位点对照 → (E-5/E-8 衔接)

### 源码锚点清单
- RetentionLease.java:24-27 (定义注释) / 31-65 (结构)
- ReplicationTracker.java:302 (addRetentionLease) / 390 (renewRetentionLease) / 488 (persistRetentionLeases)
- IndexShard.java:231 (pendingPrimaryTerm) / 360-361 (构造读 term) / 569 (新 term 处理)
- InternalEngine.java:1358-1367 (term 冲突拒绝)
- replication.c:28 (Redis replicationSendAck)
