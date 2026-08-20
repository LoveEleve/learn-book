# E-12 Aggregations — 时空溯源 (v0.90 → v8.12.2)

> 方法: git show 早期 tag + git log commit 日期实证
> 断代锚点: v0.90.0 / v1.0.0.Beta1 / v2.0.0 / v8.12.2

## 演进主线 (4 代)

| 代 | 版本 | 结构 | 关键决策 |
|:--:|---|---|---|
| 1 | v0.90 | `search/facet/` (109 文件): Facet/FacetExecutor/FacetBuilder + DoubleFacetAggregatorBase | **facet 时代**: 手写执行器 (FacetExecutor), 类型少 (terms/range/histogram/statistical) |
| 2 | v1.0 | facet 111 文件 + aggregations 模块引入 (c7f6c5266d1, **2013-11-24**) | **双轨过渡**: 新 aggregations 模块与 facet 并存 |
| 3 | v2.0 | aggregations 320 文件 (v1.4 起 246), facet **完全移除 (ea96359d82a, 2014-08-21)** | **facet → aggregation 换代**: Aggregator 递归组合取代 FacetExecutor; 2014-08-21 一刀切移除 |
| 4 | v8.12 | 516 文件: AggregatorBase + GlobalOrdinalsStringTermsAggregator (974) + CompositeAggregator (633) + MultiBucketConsumerService | **成熟期**: global ordinals 加速 (E-11) + composite/管道聚合扩展 + 桶上限保护 |

## 三个核心设计变迁

### 1. FacetExecutor → Aggregator 递归组合 (2013-2014)

```
v0.90: FacetExecutor (手写执行器) — 每类型一个执行器, 无组合
v2.0:  Aggregator (Aggregator.java:33, v2.0) → AggregatorBase (AggregatorBase.java:35): getLeafCollector 模板委托链
       — 子聚合递归组合, 一次遍历喂整棵树
```
- 设计原因: 聚合嵌套 (terms→date_histogram→avg) 需要递归组合 — FacetExecutor 无法表达

### 2. 双轨 → 单轨 (2014-08-21)

```
v1.0:  facet + aggregations 并存 (111+)
v2.0:  facet 移除 (ea96359d82a) — aggregations 全功能
```
- 设计原因: 双轨维护成本高; aggregations 已覆盖 facet 全部能力 + 嵌套

### 3. 字符串桶 → global ordinals 编号桶 (v5+)

```
早期: terms 聚合逐字符串比较建桶
v8.12: GlobalOrdinalsStringTermsAggregator (GlobalOrdinalsStringTermsAggregator.java:55) — global ordinals 直接编号计数 (E-11)
```
- 设计原因: 高基数 terms 聚合字符串比较太慢; global ordinals 让跨段聚合按 int 编号

## 对照 Redis HLL

- ES cardinality agg: HyperLogLogPlusPlus 可配 precision (CardinalityAggregator.java:48,71)
- Redis PFADD: HLL_DENSE/SPARSE 双编码 (hyperloglog.c:54-60)
- 同源算法 (HLL) 两种工程实现 — E-12 与 [[r12-hll]] 完整对照

## REVIEW 修正记录 (2026-08-14)

- ✅ 聚合引入 2013-11-24 (c7f6c5266d1) / facet 移除 2014-08-21 (ea96359d82a) 日期实证
- ✅ v0.90 facet 109 文件 / v2.0 aggregations 320 文件 实证

## 完成检查

- [x] v0.90 facet 结构已读 (FacetExecutor)
- [x] 引入/移除 commit 日期实证
- [x] 三核心变迁逐代对照
- [x] Redis HLL 对照
