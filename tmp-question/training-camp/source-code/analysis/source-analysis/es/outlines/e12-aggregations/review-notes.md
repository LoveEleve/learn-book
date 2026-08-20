# E-12 Aggregations — 六层深审 REVIEW 记录 (2026-08-14)

> 审查方法: 07 五维度 + 逐锚点 awk/sed 核对 + 裸行号扫描 + 行号上限检查 + 跨域引用核验
> **结论: 深审通过 (1 处方法体内偏差 + 50 裸行号根治)**

## 第一层: 锚点验证 (写后即验, 抓 1 处)

| # | 文件 | 原写 | 实测 | 类型 |
|:--:|---|---|---|---|
| 1 | 02 大纲 | terms collect (L127-139): advanceExact (L133) → ordValue (L134) → collectGlobalOrd (L135) | advanceExact **L128** / ordValue **L131** / collectGlobalOrd **L132** | 方法体内 -5~-3 |

## 第二层: 机制实证 (全过)

- AggregationPhase.preProcess L27-39 / AggregatorBase L35,46,219-232,294-298 ✅
- MultiBucketConsumerService DEFAULT_MAX_BUCKETS L32 / TooManyBuckets L55-60 ✅
- AggregationReduceContext sealed L23 / ForFinal 语义 L77-81 ✅
- terms collect 快路径 L121-135 + 编号计数 L128-132 ✅
- BestBucketsDeferringCollector L37-42 / selectedBuckets L60 ✅
- CompositeAggregator rawAfterKey L80 / compareCurrent L598 / CompositeKey L22-25 ✅
- PipelineAggregator reduce L117 / CardinalityAggregator HLL L48,71 ✅

## 第三层: 编造检查 (零)

- facet→aggregations: 引入 2013-11-24 (c7f6c5266d1) / 移除 2014-08-21 (ea96359d82a) 日期实证 ✅
- v0.90 facet 109 文件 / v2.0 aggregations 320 文件 实证 ✅

## 第四层: 覆盖缺口 (2 项 — completeness ⚠️ 已回补)

- 01-L2 postCollection 语义 (doPostCollection + 桶截断) ✅
- 01-L3 TooManyBuckets 运维 ✅

## 第五层: 裸行号

- 修复前 50 处 → 修复后 **0 残留**
- 行号上限: 10 文件全 OK

## 第六层: 跨域引用核验

- redis/r12-hll ✅ (HLL 完整对照)
- redisson/rd5-rmap ✅
- E-2/E-11 内部衔接 (挂载点/global ordinals 两域闭环) ✅

---

## 第二轮复审 (REVIEW-2, 2026-08-14) — 自查修复后复核

> 目的: 验证自查修复精度 + 抓方法体内行号偏移 (前 8 域经验延续)

### 发现 2 处新偏差 (最少的一轮)

| # | 文件 | 原写 | 实测 | 类型 |
|:--:|---|---|---|---|
| 1 | 01 大纲锚点清单 | postCollection (L96) | **L294-298 是主实现** — L96 是匿名类 badState 占位 | 语义错位 (占位 vs 主实现) |
| 2 | 01 大纲正文 | doPostCollection (L296) / 子聚合递归 (L297) 裸行号 | 补 AggregatorBase.java 前缀 | 格式 |

### 已验证正确 (30+ 项)

- AggregationPhase L23,27 / AggregatorBase L35,46,219-232,294-298 ✅
- MultiBucketConsumerService L32,55 / AggregationReduceContext L23,77-81 ✅
- terms collect L128/131/132 + 快路径 L121-135 ✅
- BestBucketsDeferringCollector L42,60,144 ✅ / CompositeAggregator L72,80,598 ✅
- PipelineAggregator L24,117 / CardinalityAggregator L43,48,71 ✅
- 时空溯源 2 commit 日期复核 (2013-11-24 / 2014-08-21) ✅

### 根因与根治 (确认收敛)

- **根因**: 锚点清单写"方法名"时未区分"匿名类占位实现"与"主类实现" — postCollection 在 AggregatorBase 有两个定义 (L96 匿名 badState, L294 主实现)
- **确认**: 三遍验证闭环持续收敛 — E-12 本轮仅 2 处 (E-11 抓 3 处, E-2 抓 7 处, E-1 抓 10 处) — **逐域递减趋势明显, 方法论已内化**
- harness 13/13 复跑通过 / 裸行号零残留 / 上限 10 文件全 OK / 跨域 2 个 [ -d ] 通过
