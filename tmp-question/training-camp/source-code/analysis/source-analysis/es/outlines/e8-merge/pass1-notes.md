# E-8 Merge Policy — Pass 1 探索笔记 (扫轮廓)

> 🟡 B | 依赖: E-1 ✅ (Engine 段管理) + E-6 ✅ (租约保护) | 对照: [[r23-evict]] (Redis 淘汰) [[r8-persistence]] (快照)
> 源码: server/src/main/java/org/elasticsearch/index/MergePolicyConfig.java (368 行) + engine/ElasticsearchConcurrentMergeScheduler.java + index/merge/
> 测试地图: server/src/test/.../index/MergePolicyConfigTests.java

## 继承树/调用图

```
MergePolicyConfig (MergePolicyConfig.java:104) — 段合并策略配置
├── TieredMergePolicy (Lucene, MergePolicyConfig.java:105) — 分层合并
│     ├── setMaxMergesAtOnce (MergePolicyConfig.java:299) / setMaxMergedSegment (L300) / setSegmentsPerTier (L301)
│     └── setMergeFactor (MergePolicyConfig.java:302) / setDeletesPctAllowed (MergePolicyConfig.java:303) — 5 参数
├── 默认值: MAX_MERGE_AT_ONCE=10 (L119) / SEGMENTS_PER_TIER=10.0 (L141) / MERGE_FACTOR=32 (L149) / DELETES_PCT=20% (MergePolicyConfig.java:150)

ElasticsearchConcurrentMergeScheduler (ElasticsearchConcurrentMergeScheduler.java:38) — 合并调度器 (追踪统计)
    doMerge (ElasticsearchConcurrentMergeScheduler.java:95): 合并执行 + 统计
    由 InternalEngine.mergeScheduler (InternalEngine.java:136) 使用

调用链: InternalEngine.forceMerge (InternalEngine.java:2389) → indexWriter.maybeMerge (L2407)
```

## 基本元素分解 (原则二)

1. **TieredMergePolicy 分层合并** — 按大小分层: 相似大小段合为一层 (MergePolicyConfig.java:105) — 避免大段反复重写
2. **5 参数配置** — maxMergeAtOnce (一次合并段数) / maxMergedSegment (最大段) / segmentsPerTier (每层段数) / mergeFactor / deletesPctAllowed (删除占比触发)
3. **默认值** — 10/10/32/20% (MergePolicyConfig.java:119-150) — mergeFactor 32 是 Lucene 兼容值
4. **ConcurrentMergeScheduler 调度** — ElasticsearchConcurrentMergeScheduler (ElasticsearchConcurrentMergeScheduler.java:38): maxThreadCount/maxMergeCount + 统计追踪
5. **forceMerge API** — InternalEngine.forceMerge (InternalEngine.java:2389) → maybeMerge (L2407) — 冷索引优化
6. **删除段处理** — deletesPctAllowed (MergePolicyConfig.java:303) — 删除占比超阈值强制合并清理

## 标记问题 (≥5)

1. **Q1: TieredMergePolicy 分层逻辑** — "按大小分层"具体怎么分? 为什么避免大段反复合并?
2. **Q2: 5 参数语义** — maxMergeAtOnce/segmentsPerTier/mergeFactor 区别? (10/10/32 看起来重叠)
3. **Q3: 删除段清理** — deletesPctAllowed=20% 触发什么? soft deletes (E-1) 与合并的关系?
4. **Q4: 调度器限制** — maxThreadCount/maxMergeCount 怎么防 IO 过载?
5. **Q5: forceMerge 场景** — 什么时候用? 代价? (冷索引)
6. **Q6: 与 Redis 淘汰对照** — Redis LRU 淘汰 vs ES 段合并清理 — 删除语义两范式
7. **Q7: 合并与 refresh 的关系** — 合并后段变化怎么影响 reader? (E-1 NRT 衔接)
8. **Q8: 租约与合并** — RetentionLease (E-6) 怎么阻止合并清理 soft-deleted 历史?

## 已读测试 (2 个)

- `MergePolicyConfigTests.testTieredMergePolicySettingsUpdate` (MergePolicyConfigTests.java:125): 参数更新
- `MergePolicyConfigTests.testNoMerges` (MergePolicyConfigTests.java:70): 禁用合并
- `MergePolicyConfigTests.testDefaultMaxMergedSegment` (MergePolicyConfigTests.java:398)

## 完成检查

- [x] 继承树/调用图已画出
- [x] 基本元素分解 (6 元素, 对应源码位置)
- [x] 8 个标记问题, 每个有源码位置
- [x] 已读 3 个测试文件

## 跨域发现

- 来源: E-8 Pass 1 — InternalEngine.mergeScheduler (InternalEngine.java:136) 是 E-1 的组件; forceMerge (L2389) 由 E-1 暴露
- 发现: deletesPctAllowed 与 soft deletes (E-1 L182) 协同 — 删除占比超阈值才合并清理
- 已对照验证: InternalEngine.java:136,2389,2407
