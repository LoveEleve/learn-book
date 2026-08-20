# E-12 Aggregations — 全视角提问验证 (completeness)

> 验证时机: 3 篇大纲深审前。身份: 开发者/架构师/性能工程师/SRE/研究者/子系统开发者/学生

| # | 身份 | 子主题 | 问题 | 大纲覆盖 |
|:--:|------|------|------|:--:|
| 1 | 开发者 | 生命周期 | preProcess 什么时候调? (查询前?) | ✅ 01-L1 (AggregationPhase.preProcess L27) |
| 2 | 开发者 | 生命周期 | postCollection 做什么? (排序/截断?) | ⚠️ 01-L2 提了未展开 → 补一句 (桶排序/截断 top) |
| 3 | 开发者 | 桶上限 | max_buckets 能调吗? | ✅ 01-L3 (可配) |
| 4 | 开发者 | terms | collection_mode 参数在哪? | ✅ 02-L3 (terms 参数) |
| 5 | 架构师 | 生命周期 | 为什么 Aggregator 递归组合? | ✅ 01-L2 (一次遍历喂整树) |
| 6 | 架构师 | 桶 | 深度优先 vs 广度优先权衡? | ✅ 02-L3 (内存 vs 遍历) |
| 7 | 架构师 | reduce | ForPartial/ForFinal 为什么分两段? | ✅ 01-L4 (pipeline 只最终) |
| 8 | 性能工程师 | terms | global ordinals 快多少? | ✅ 02-L2 (int 编号 vs 字符串) |
| 9 | 性能工程师 | HLL | precision 与内存关系? | ✅ 03-L3 (可配精度) |
| 10 | SRE | 桶上限 | TooManyBucketsException 线上处理? | ⚠️ 01 未提运维 → 补一句 (调 max_buckets/查高基数) |
| 11 | SRE | HLL | cardinality 误差多大? | ✅ 03-L3 (0.81%) |
| 12 | 研究者 | 对照 | facet → aggregations 演进? | ✅ 01-L5 (时空溯源) |
| 13 | 研究者 | 对照 | ES HLL vs Redis HLL? | ✅ 03-L4 |
| 14 | 子系统开发者 | 衔接 | 聚合 collector 怎么挂查询? (E-2) | ✅ 01-L1 (E-2 验证) |
| 15 | 子系统开发者 | 衔接 | global ordinals 谁构建? (E-11) | ✅ 02-L2 (两域闭环) |
| 16 | 学生 | 概念 | 聚合是什么? | ✅ 01-L1 (字段统计) |
| 17 | 学生 | 桶 | terms 和 composite 区别? | ✅ 02-L4 |
| 18 | 学生 | pipeline | avg_bucket 通俗解释? | ✅ 03-L2 (桶结果再聚合) |

**统计**: ✅ 16 / ⚠️ 2 / ❌ 0 — ⚠️ 全部"补一句"级
→ 回补 2 项: 01-L2 postCollection 语义 / 01-L3 TooManyBuckets 运维

## 回补清单

1. 01-L2: 补 postCollection 做什么 (桶排序/截断 top)
2. 01-L3: 补 TooManyBucketsException 运维 (调 max_buckets / 查高基数字段)
