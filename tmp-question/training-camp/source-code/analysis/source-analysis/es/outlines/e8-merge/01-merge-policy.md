# E-8 Merge Policy 篇 1/2 — 合并策略: 分层与参数

> 前置: [[E-1-engine-03]] (段管理) | 复用: — | 对照: [[r23-evict]] (Redis 淘汰) | 引出: [[E-8-merge-02]] [[E-9-bulk]]
> 🟡 B | 来源: MergePolicyConfig.java:104-105,119-150,278-305,326-330,367-380 + MergeSchedulerConfig.java:45-54,71-72
> 定位: Merge 卷开篇 — 回答"段怎么合并? 5 参数怎么调?"

**读者处境**: 面试官问 "ES 段合并是什么? 怎么防写放大?" 你答 "TieredMergePolicy" — 但再问 "maxMergeAtOnce 和 segmentsPerTier 什么关系? mergeFactor 干嘛的?" 你答不上来。这篇是合并策略的完整答案。

### 1. 问题引入 — 段碎片与合并

场景: 每次 refresh 产生新段 → 几百小段 → 查询慢 — 合并怎么解决?
- TieredMergePolicy (MergePolicyConfig.java:105): 按大小分层合并
- 本篇问题: 分层逻辑 (Q1) / 参数语义 (Q2) / 删除清理 (Q3) / 调度限制 (Q4)

### 2. 分层合并 — TieredMergePolicy

场景: 为什么按大小分层?
- 分层思想: 相似大小段成 tier, 超阈值合并 → 新段进上层
- 效果: 大段少合并防写放大, 小段快速合并
- **调参影响**: segmentsPerTier 调小 → 每层更快触发合并 (更多小段合并, 段更整齐但合并频繁); 调大 → 合并少但段多
- 对照 LogByteSizeMergePolicy (MergePolicyConfig.java:177 注释)

### 3. 5 参数 — 分工与约束

场景: 参数看着重叠, 怎么配?
- 默认 (MergePolicyConfig.java:119-150): MAX_MERGE_AT_ONCE=10 / SEGMENTS_PER_TIER=10.0 / MERGE_FACTOR=32 / DELETES_PCT=20%
- **约束** (MergePolicyConfig.java:367-380): maxMergeAtOnce 必须 ≤ segmentsPerTier (MergePolicyConfig.java:369-374)
- **关键** (MergePolicyConfig.java:326-330): mergeFactor 只作用于 timeBasedMergePolicy, **Tiered 忽略它** (MergePolicyConfig.java:327) — 用 segmentsPerTier
- 语义: maxMergeAtOnce (一次最多段数) / segmentsPerTier (每层目标) / maxMergedSegment (单段上限)

### 4. 删除清理 — deletesPctAllowed

场景: 删除的文档什么时候物理清除?
- setDeletesPctAllowed (MergePolicyConfig.java:303) → tieredMergePolicy (MergePolicyConfig.java:362-363)
- 默认 20% (MergePolicyConfig.java:150): 删除占比低不合并 (保留 soft deletes 历史), 超阈值合并清理
- 与 RetentionLease (E-6) 协同 (篇 2 展开)

### 5. 调度限制 — 防合并风暴

场景: 合并会不会占满 IO?
- MergeSchedulerConfig (MergeSchedulerConfig.java:45-54): maxThreadCount + maxMergeCount (默认 = 线程+5, L54)
- 应用: setMaxMergesAndThreads (ElasticsearchConcurrentMergeScheduler.java:211)
- 追踪: doMerge (ElasticsearchConcurrentMergeScheduler.java:95) 统计

### 核心悬念
"为什么 mergeFactor=32 但 Tiered 策略忽略它?" — 因为 TieredMergePolicy 用 segmentsPerTier 替代了传统的 merge_factor 语义 (按大小分层而非按数量), mergeFactor 只留给时间基策略兼容。

### 概念依赖链
Q1 分层 → Q2 参数 → Q3 删除 → Q4 调度

### 源码锚点清单
- MergePolicyConfig.java:104-105 (类+策略) / 119-150 (默认值) / 177 (LogByteSize 注释) / 278-305 (setMergePolicy) / 303 (setDeletesPctAllowed) / 326-330 (mergeFactor 忽略) / 362-363 (deletesPctAllowed 应用) / 367-380 (adjustMaxMergeAtOnceIfNeeded)
- MergeSchedulerConfig.java:45-54 (线程/合并数设置) / 54 (maxMergeCount=线程+5) / 71-72 (读取)
- ElasticsearchConcurrentMergeScheduler.java:95 (doMerge) / 211 (setMaxMergesAndThreads)
