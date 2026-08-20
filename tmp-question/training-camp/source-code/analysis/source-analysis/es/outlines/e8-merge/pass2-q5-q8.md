# E-8 闭环笔记 Q5-Q8: forceMerge/Redis 对照/reader 影响/租约保护

## Q5: forceMerge — 冷索引优化

假设: forceMerge 强制合并到指定段数 (如 1), 用于冷索引 — 阻塞等待合并完成。

验证过程:
- Read InternalEngine.forceMerge (InternalEngine.java:2389-2430): 三分支 — forceMergeDeletes (L2406, 只清删除) / maxNumSegments<=0 → maybeMerge (InternalEngine.java:2407) / forceMerge(maxNumSegments, blocking) (InternalEngine.java:2409)
- 阻塞语义: `blocks and waits for merges` (InternalEngine.java:2405,2409 注释)
- forceMergeUUID (InternalEngine.java:2410): 标识本次 forceMerge
- flush 可选 (InternalEngine.java:2413-2414)
- 场景: 冷索引 (只读) 合并到 1 段 → 查询更快 (少段)

代码类型: Implementation (强制合并)

结论: **forceMerge 三分支: 清删除 (forceMergeDeletes) / 默认合并 (maybeMerge) / 指定段数 (forceMerge blocking) — 阻塞等合并完, 冷索引优化用 (合并到 1 段)**。InternalEngine.java:2389-2430

## Q6: 与 Redis 淘汰对照 — 删除/清理两范式

假设: Redis maxmemory 淘汰 (LRU/LFU, 内存压力驱动) vs ES 段合并 (删除占比驱动) — 清理触发点不同。

验证过程:
- Redis: evict.c:1-30: maxmemory 超限 → LRU/LFU 淘汰池 (evict.c:23-30) — **内存压力驱动**
- ES: deletesPctAllowed=20% (Q3) — **删除占比驱动**
- 对照维度:
  - 触发: Redis 内存满 / ES 删除超阈值
  - 粒度: Redis 单 key / ES 整个段
  - 数据: Redis 淘汰丢数据 (可选) / ES 只清理已删文档
- 面试记忆点: "Redis 淘汰是内存急救, ES 合并是空间整理"

代码类型: 对照分析

结论: **Redis 淘汰 = 内存压力触发 (LRU/LFU, evict.c:23-30); ES 合并 = 删除占比触发 (20%) — 清理触发点不同源于数据模型: 缓存可丢, 索引只能删已删**。对照锚点: evict.c:1-30 + MergePolicyConfig.java:303

## Q7: 合并与 NRT 的关系

假设: 合并改段结构, reader 通过 refresh 感知 — 合并不阻塞查询。

验证过程:
- 合并后台执行 (ElasticsearchConcurrentMergeScheduler) — 不阻塞写入/查询
- 合并后段变化: refresh 打开新 reader 时可见 (E-1 NRT 衔接)
- 大合并后: shouldPeriodicallyFlushAfterBigMerge (E-1 L2834) — 触发 flush 释放资源
- 查询期间: Lucene 多段 reader 自动合并视角 (段在 reader 内透明)

代码类型: 衔接分析

结论: **合并后台跑 (调度器限流 Q4), 段变化由 refresh 呈现 (E-1 NRT); 大合并后触发 flush (E-1 L2834) 释放内存 — 合并与查询解耦**。衔接锚点: InternalEngine.java:2834 + ElasticsearchConcurrentMergeScheduler.java:95

## Q8: 租约与合并 — CombinedDeletionPolicy

假设: commit 删除策略协调 translog 保留 + soft deletes — 租约保护必要历史不被合并清理。

验证过程:
- Read CombinedDeletionPolicy (CombinedDeletionPolicy.java:34-40): "coordinates between Lucene's commits and the retention of translog generation files" (CombinedDeletionPolicy.java:34-35) — **commit 保留与 translog 删除协调**
- 核心 (CombinedDeletionPolicy.java:37-39): 删除 maxSeqNo ≤ globalCheckpoint 的 commit, 保留最高一个 — **E-6 衔接**
- safeCommit (CombinedDeletionPolicy.java:57): 最安全 commit 点 (maxSeqNo ≤ persisted gcp)
- 与合并: 合并清理 soft-deleted 文档, 但 safeCommit 之前的 translog 必须保留 (恢复需要)

代码类型: Implementation (删除协调)

结论: **CombinedDeletionPolicy 协调两层删除: ① commit 保留到 globalCheckpoint (E-6) ② translog 保留到 safeCommit — 合并只能清理"超过保留点"的 soft deletes, 租约/恢复需要的历史被保护**。CombinedDeletionPolicy.java:34-57

跨域关联: E-6 (globalCheckpoint/safeCommit) / E-3 (translog 保留) — 三域闭环
