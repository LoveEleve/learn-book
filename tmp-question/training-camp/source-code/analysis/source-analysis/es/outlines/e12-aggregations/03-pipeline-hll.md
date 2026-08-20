# E-12 Aggregations 篇 3/3 — 扩展与对照: pipeline/HLL

> 前置: [[E-12-aggregations-01]] [[E-12-aggregations-02]] | 复用: — | 对照: [[r12-hll]] (Redis HLL) [[rd5-rmap]] | 引出: [[E-10-clusterstate]]
> 🔴 A | 来源: PipelineAggregator.java:24,117 + CardinalityAggregator.java:43-71,148 + hyperloglog.c:44-60
> 定位: Aggregations 卷收尾 — 回答"pipeline 是什么? 和 Redis HLL 差在哪?"

**读者处境**: 面试官问 "pipeline 聚合是什么? avg_bucket 怎么算?" 你答 "桶上再聚合" — 但再问 "cardinality agg 和 Redis PFADD 什么关系? 精度多少?" 你答不上来。这篇是扩展聚合 + HLL 对照的完整答案, 收束 Aggregations 域。

### 1. 问题引入 — 聚合的两类扩展

场景: 桶算完了还想算"桶的平均" (avg_bucket)? 统计唯一值 (cardinality)?
- pipeline: 消费桶结果再聚合 (reduce 阶段)
- cardinality: 近似基数统计 (HLL)
- 本篇问题: pipeline 机制 (Q7) + HLL 对照 (Q8)

### 2. pipeline — 桶结果变换

场景: avg_bucket 怎么工作?
- PipelineAggregator (PipelineAggregator.java:24-117): reduce (PipelineAggregator.java:117) `reduce(InternalAggregation, reduceContext)` — **输入是聚合结果而非文档**
- 类型: AvgBucket/CumulativeSum/BucketScript (pipeline/ 46 文件)
- 执行时机: 最终 reduce (篇 1 Q6 ForFinal)
- 与父聚合区别: 普通聚合收集文档; pipeline 消费桶结果

### 3. cardinality — HyperLogLog 近似计数

场景: 统计 1000 万唯一值, 内存怎么省?
- CardinalityAggregator (CardinalityAggregator.java:43-71): HyperLogLogPlusPlus (CardinalityAggregator.java:48) + precision (CardinalityAggregator.java:71) — **可配精度**
- 原理: HLL 哈希 + 寄存器最大前导零 — 误差 ~0.81%
- 与精确计数 (terms cardinality) 权衡: 内存 vs 精度

### 4. 与 Redis HLL 对照 — 同源两实现

场景: ES cardinality 和 Redis PFADD 什么关系?
- ES: HyperLogLogPlusPlus 可配 precision (CardinalityAggregator.java:48,71)
- Redis: HLL_DENSE/SPARSE 双编码 (hyperloglog.c:54-60) + 16384 寄存器
- 对照维度: 同源算法 (HLL); ES 可调精度; Redis 固定 + 稀疏优化
- 面试记忆点: "cardinality agg = Redis PFADD 的 ES 版"

### 5. 收束 — Aggregations 域总结

- 生命周期 (篇 1): 挂载/组合/桶上限/reduce
- 桶聚合 (篇 2): terms global ordinals + 遍历策略 + composite
- 扩展 (本篇): pipeline 结果变换 + HLL 近似计数
- 终极结论: 聚合 = "一次遍历收集 + 两阶段 reduce + 结果变换" 的三层架构
- 引出: E-10 ClusterState (聚合配置随集群发布)

### 核心悬念
"统计 1 亿唯一值只花几 MB 内存?" — HLL: 每个值哈希进 16384 寄存器 (Redis) 或可配精度寄存器 (ES), 只记"最大前导零"不记值 — 误差 0.81%, 内存恒定。

### 概念依赖链
Q7 pipeline → Q8 HLL 对照 → (E-10 衔接)

### 源码锚点清单
- PipelineAggregator.java:24 (类) / 117 (reduce)
- CardinalityAggregator.java:43 (类) / 48 (HyperLogLogPlusPlus) / 71 (precision) / 148 (clone)
- hyperloglog.c:44-60 (HLL 双编码, Redis)
