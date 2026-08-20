# E-8 Merge Policy 篇 2/2 — 合并的边界: forceMerge/删除/租约

> 前置: [[E-8-merge-01]] [[E-6-seqno-03]] (租约) [[E-1-engine-02]] (NRT) | 复用: — | 对照: [[r8-persistence]] (快照) | 引出: [[E-10-clusterstate]]
> 🟡 B | 来源: InternalEngine.java:2389-2430 + CombinedDeletionPolicy.java:34-57 + evict.c:1-30
> 定位: Merge 卷收尾 — 回答"什么时候强制合并? 删除/租约怎么保护?"

**读者处境**: 面试官问 "forceMerge 什么时候用? 合并会把删除的历史清掉吗?" 你答 "冷索引优化" — 但再问 "CombinedDeletionPolicy 协调什么? 租约怎么阻止合并?" 你答不上来。这篇是合并边界的完整答案, 收束 Merge 域。

### 1. 问题引入 — 合并的边界条件

场景: 合并不是想合就合 — 有强制场景, 也有保护边界
- forceMerge: 冷索引优化 (Q5)
- 删除保护: 租约阻止清理必要历史 (Q8)
- 本篇问题: forceMerge (Q5) / Redis 对照 (Q6) / NRT 关系 (Q7) / 租约保护 (Q8)

### 2. forceMerge — 冷索引优化

场景: 什么时候强制合并?
- InternalEngine.forceMerge (InternalEngine.java:2389-2430): 三分支 — forceMergeDeletes (InternalEngine.java:2405 清删除) / maybeMerge (InternalEngine.java:2407) / forceMerge(maxNumSegments, blocking) (InternalEngine.java:2409)
- 阻塞语义 (InternalEngine.java:2405,2409): 等合并完成
- 场景: 冷索引合并到 1 段 → 查询快; forceMergeUUID (InternalEngine.java:2410) 标识

### 3. 与 Redis 淘汰对照 — 两范式

场景: Redis 淘汰和 ES 合并都是"清理", 差在哪?
- Redis: maxmemory 超限 → LRU/LFU 淘汰 (evict.c:23-30) — 内存压力驱动, 可丢数据
- ES: 删除占比触发 (20%) — 空间整理, 只清已删
- 对照: "Redis 是急救 (内存), ES 是整理 (空间)"

### 4. 合并与 NRT — 解耦

场景: 合并影响查询吗?
- 后台执行 (调度器限流) — 不阻塞写/查
- 段变化由 refresh 呈现 (E-1 NRT)
- 大合并后: shouldPeriodicallyFlushAfterBigMerge (E-1 L2834) 触发 flush

### 5. 租约保护 — CombinedDeletionPolicy

场景: 合并会不会把恢复需要的历史清掉?
- CombinedDeletionPolicy (CombinedDeletionPolicy.java:34-40): "coordinates between Lucene's commits and the retention of translog" (CombinedDeletionPolicy.java:34-35)
- 核心 (CombinedDeletionPolicy.java:37-39): 删除 maxSeqNo ≤ globalCheckpoint 的 commit, 保留最高 — E-6 衔接
- safeCommit (CombinedDeletionPolicy.java:57): 最安全 commit (maxSeqNo ≤ persisted gcp)
- 效果: 合并只清"超过保留点"的 soft deletes; 租约/恢复需要的历史被保护

### 核心悬念
"合并会不会把 soft-deleted 历史全清掉, 导致恢复失败?" — 不会: CombinedDeletionPolicy 保证 safeCommit (≤ persisted globalCheckpoint) 之前的 translog/commit 必须保留, 合并只能清理超过保留点的删除 — 三层保护 (deletesPctAllowed + safeCommit + RetentionLease)。

### 概念依赖链
Q5 forceMerge → Q6 Redis 对照 → Q7 NRT → Q8 租约保护 → (E-6/E-3 三域闭环)

### 源码锚点清单
- InternalEngine.java:2389-2430 (forceMerge) / 2406 (forceMergeDeletes) / 2408 (maybeMerge) / 2410 (forceMerge blocking) / 2411 (forceMergeUUID) / 2834 (大合并后 flush)
- CombinedDeletionPolicy.java:34-40 (协调注释) / 37-39 (commit 删除规则) / 57 (safeCommit)
- evict.c:1-30 (Redis 淘汰对照)
