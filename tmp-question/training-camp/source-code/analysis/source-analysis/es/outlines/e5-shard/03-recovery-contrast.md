# E-5 Shard 篇 3/3 — 恢复与对照: 分片怎么从崩溃回来

> 前置: [[E-5-shard-01]] [[E-5-shard-02]] [[E-3-translog-02]] (恢复回放) | 复用: — | 对照: [[r14-sentinel]] [[r8-persistence]] | 引出: [[E-10-clusterstate]]
> 🔴 A | 来源: IndexShard.java:2370-2380,1704,1748 + StoreRecovery + InternalEngine.java:1358-1367
> 定位: Shard 卷收尾 — 回答"分片怎么从崩溃恢复? 与 Redis 哨兵差在哪?"

**读者处境**: 节点重启, 分片要恢复 — 面试官问 "ES 分片恢复流程? 从哪恢复? 恢复后多久能写?" "和 Redis 的主从切换比, ES 强在哪?" 你答 "store + translog 回放" — 这篇是恢复编排 + Redis 对照的完整答案, 收束 Shard 域。

### 1. 问题引入 — 崩溃后的三阶段

场景: 节点重启, 分片从磁盘恢复要经历什么?
- 阶段 1: Store 打开已提交段 (Lucene)
- 阶段 2: translog 回放未提交操作 (E-3 衔接)
- 阶段 3: POST_RECOVERY → 等集群 active → STARTED
- 本篇问题: 编排细节 (Q5) + Redis 对照 (Q7)

### 2. 恢复流程 — Store + Translog 两段

场景: recoverFromStore 具体做什么?
- 入口: recoverFromStore (IndexShard.java:2370-2380): `new StoreRecovery(shardId, logger)` (IndexShard.java:2375) 异步编排
- 恢复范围: Store 段 (已 commit, Lucene) + translog 回放 (未 commit, E-3 newSnapshot L657)
- recoverLocallyUpToGlobalCheckpoint (IndexShard.java:1748): 本地恢复到 globalCheckpoint 水位 (E-6 衔接)
- postRecovery (IndexShard.java:1704): 完成后 → changeState(POST_RECOVERY) (IndexShard.java:1723)
- 激活等待: updateShardState (IndexShard.java:529-536) — master 确认 active 才 STARTED
- **运维视角**: 分片卡在 POST_RECOVERY = master 未确认 active (集群状态未更新) — 检查 master 健康/路由发布, 而非分片自身问题

### 3. 与 Redis 哨兵对照 — 故障处理的哲学差异

场景: Redis 主从切换和 ES 分片恢复差在哪?
- Redis ([[r14-sentinel]]): 独立哨兵进程检测 → 选举副本升主 → 旧主恢复后降级 (配置重写); 无 term 无 globalCheckpoint
- ES: 状态机在分片自身 + master 驱动; term 硬隔离 (InternalEngine.java:1358-1367 旧主写被拒); globalCheckpoint 保证不丢已确认数据
- 对比维度:
  - 监督者: Redis 外部哨兵 / ES 分片自含 + 集群状态
  - 双主防护: Redis 配置降级 (软) / ES term (硬, 物理拒写)
  - 数据安全: Redis async 复制可能丢窗口 / ES globalCheckpoint 保证
- 面试记忆点: "Redis 切换靠哨兵+配置, ES 靠 term+水位 — 一个管进程, 一个管数据"

### 4. 收束 — Shard 域总结

- 生命周期 (篇 1): 5 态 + permits 双模式
- 接任 (篇 2): promotion 四步 + 复制组
- 恢复 (本篇): Store+translog 两段 + 集群确认
- 终极结论: 分片 = 状态机 (何时能做什么) + 组件 (Engine/Store/Translog/ReplicationTracker) + 编排 (恢复/升主/关闭)
- 引出: E-10 ClusterState (集群驱动源, 收束全卷)

### 核心悬念
"节点重启后分片多久能恢复?" — 取决于 Store 段 + translog 未提交量: 段直接打开, translog 回放未提交操作, 然后等 master 确认 active — 恢复与激活分离让数据恢复先于流量恢复。

### 概念依赖链
Q5 恢复两段 → Q7 Redis 对照 → (E-3/E-10 衔接)

### 源码锚点清单
- IndexShard.java:2370-2380 (recoverFromStore) / 2375 (StoreRecovery) / 1704 (postRecovery) / 1723 (POST_RECOVERY) / 1748 (recoverLocallyUpToGlobalCheckpoint) / 529-536 (STARTED 激活)
- Translog.java:657 (newSnapshot 回放, E-3 交付)
- InternalEngine.java:1358-1367 (旧主 term 冲突拒绝)
- StoreRecovery.java (616 行, 段恢复编排)
