# E-8 闭环笔记 Q1-Q4: 分层合并/参数/删除/调度

## Q1: TieredMergePolicy — 按大小分层

假设: 段按大小分"层" (tier), 每层段数超阈值才合并 — 避免大段反复重写。

验证过程:
- Read MergePolicyConfig (MergePolicyConfig.java:104-105): `tieredMergePolicy = new TieredMergePolicy()` (MergePolicyConfig.java:105) — Lucene 分层策略
- 注释 (MergePolicyConfig.java:177): "LogByteSizeMergePolicy is similar to TieredMergePolicy, as it also tries to organize segments into tiers of"
- 核心思想: 相似大小的段归一层, 每层段数 > segmentsPerTier 时合并 → 新段进入上层
- 效果: 大段不参与频繁合并 (只与同层合并), 避免"写放大"

代码类型: Algorithmic (分层合并)

结论: **TieredMergePolicy = 按大小分层: 相似大小段成 tier, 超阈值合并; 新段进上层 — 大段少合并防写放大, 小段快速合并**。MergePolicyConfig.java:104-105

## Q2: 5 参数语义 — 重叠与差异

假设: maxMergeAtOnce/segmentsPerTier/mergeFactor 看似重叠, 实际分工不同。

验证过程:
- 默认值 (MergePolicyConfig.java:119-150): MAX_MERGE_AT_ONCE=10 / SEGMENTS_PER_TIER=10.0 / MERGE_FACTOR=32 / DELETES_PCT=20%
- adjustMaxMergeAtOnceIfNeeded (MergePolicyConfig.java:367-380): segmentsPerTier < maxMergeAtOnce → 强制 maxMergeAtOnce=segmentsPerTier (MergePolicyConfig.java:369-374) — **maxMergeAtOnce 必须 ≤ segmentsPerTier**
- **关键发现** (MergePolicyConfig.java:326-330): `setMergeFactor` 注释 "TieredMergePolicy ignores this setting, it configures a number of segments per tier instead" (MergePolicyConfig.java:327) — **mergeFactor 只作用于 timeBasedMergePolicy, Tiered 用 segmentsPerTier**
- 语义: maxMergeAtOnce = 一次合并最多段数; segmentsPerTier = 每层目标段数; mergeFactor = 时间基策略的段数 (Tiered 忽略)

代码类型: Interface (参数契约)

结论: **5 参数分工: maxMergeAtOnce (一次合并段数上限, 必须 ≤ segmentsPerTier) / segmentsPerTier (分层目标, Tiered 的核心) / mergeFactor (仅 timeBased 用, Tiered 忽略 L327) / maxMergedSegment (单段上限) / deletesPctAllowed (删除占比)**。MergePolicyConfig.java:119-150,326-330,367-380

## Q3: 删除段清理 — deletesPctAllowed 与 soft deletes

假设: 删除占比超 20% 才合并清理 — soft deletes (E-1) 保留历史与合并清理的平衡。

验证过程:
- Read MergePolicyConfig (MergePolicyConfig.java:303): `setDeletesPctAllowed(deletesPctAllowed)` → tieredMergePolicy (MergePolicyConfig.java:362-363)
- 默认 20% (MergePolicyConfig.java:150)
- 衔接: InternalEngine softDeletesField (InternalEngine.java:182) / softDeletesPolicy (InternalEngine.java:183) — soft-deleted 文档保留供恢复
- 语义: 段内删除占比 < 20% → 不合并 (保留 soft deletes 历史); > 20% → 合并清理
- 与 RetentionLease (E-6) 协同: 租约保护的段不得清理

代码类型: Implementation (删除阈值)

结论: **deletesPctAllowed=20%: 删除占比低不合并 (soft deletes 历史保留供恢复), 超阈值合并物理清理 — 与 RetentionLease (E-6) 协同保护必要历史**。MergePolicyConfig.java:303,362-363 + InternalEngine.java:182-183

跨域关联: E-1 soft deletes / E-6 RetentionLease (保护)

## Q4: 调度器限制 — 防 IO 过载

假设: 合并线程数/排队数限制, 防合并占用过多 IO 影响写入。

验证过程:
- Read MergeSchedulerConfig (MergeSchedulerConfig.java:45-54): MAX_THREAD_COUNT_SETTING (MergeSchedulerConfig.java:45) / MAX_MERGE_COUNT_SETTING (MergeSchedulerConfig.java:52-54) **默认 = maxThreadCount + 5** (MergeSchedulerConfig.java:54)
- 应用 (MergeSchedulerConfig.java:71-72): maxThread/maxMerge 从 settings 读
- ElasticsearchConcurrentMergeScheduler (ElasticsearchConcurrentMergeScheduler.java:211): `setMaxMergesAndThreads(config.getMaxMergeCount(), config.getMaxThreadCount())`
- 统计: doMerge (ElasticsearchConcurrentMergeScheduler.java:95) 追踪合并耗时/段数

代码类型: Implementation (资源限制)

结论: **调度限制: maxThreadCount (合并线程) + maxMergeCount (排队合并数, 默认线程+5) — 防止合并风暴占满 IO; 追踪统计供监控 (doMerge L95)**。MergeSchedulerConfig.java:45-54,71-72 + ElasticsearchConcurrentMergeScheduler.java:211
