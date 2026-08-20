# E-12 闭环笔记 Q5-Q8: composite/reduce/pipeline/HLL 对照

## Q5: composite 聚合 — 多字段组合桶 + after_key

假设: composite 是"多字段组合键"的分桶, 支持 after_key 分页 — 与 terms (单字段 top N) 不同。

验证过程:
- Read CompositeAggregator (CompositeAggregator.java:72-80): 持 rawAfterKey (CompositeAggregator.java:80) — 分页游标
- collect (L596-602): `queue.compareCurrent()` (CompositeAggregator.java:598) 比较当前组合键 → 匹配则收集 (CompositeAggregator.java:602)
- CompositeKey (CompositeKey.java:22-25): 多字段组合键 (Comparable... values)
- after_key 分页 (L203,216): toCompositeKey (CompositeAggregator.java:203) / lastBucket.getRawKey (L216)
- 与 terms 区别: terms 是"每个值一个桶"取 top N; composite 是"多字段组合"流式分页 (无 top N 概念)

代码类型: Algorithmic (组合键分页)

结论: **composite = 多字段组合键桶: queue 维护当前组合键 (CompositeAggregator.java:598), after_key 游标分页 (L80,216) — 适合"扁平化多字段分组"分页遍历; 与 terms (单字段 top N) 是两种桶模型**。CompositeAggregator.java:72-80,203-216,596-602

## Q6: reduce 阶段 — 跨分片合并

假设: 各分片收集完局部桶, reduce 合并为全局 — ForPartial/ForFinal 两阶段。

验证过程:
- Read AggregationReduceContext (AggregationReduceContext.java:23): sealed class permits ForPartial/ForFinal (L23) — **部分 reduce (跨分片) vs 最终 reduce (pipeline)**
- 语义 (AggregationReduceContext.java:77-81): "current reduce phase is the final reduce phase... operations like pipeline aggregations can only be applied during the final reduce phase"
- 流程: 分片 postCollection → 协调节点 reduce (ForPartial 逐层) → 最终 ForFinal (pipeline 应用)

代码类型: Implementation (两阶段归并)

结论: **reduce 分两阶段: ForPartial (跨分片合并局部桶) + ForFinal (最终合并 + pipeline 聚合应用) — pipeline 只在最终阶段跑 (AggregationReduceContext.java:77-81)**。AggregationReduceContext.java:23,77-81

## Q7: pipeline 聚合 — 桶上再聚合

假设: pipeline 聚合 (avg_bucket/max_bucket/bucket_script) 是对"已完成的桶结果"再聚合, 不参与文档收集。

验证过程:
- Read PipelineAggregator (PipelineAggregator.java:24-117): reduce (PipelineAggregator.java:117) `reduce(InternalAggregation, reduceContext)` — **输入是聚合结果而非文档**
- 类型: AvgBucket/CumulativeSum/BucketScript/... (pipeline/ 46 文件)
- 与父聚合区别: 普通聚合收集文档; pipeline 消费父聚合的桶结果 (reduce 阶段执行, Q6 衔接)

代码类型: Interface (结果变换)

结论: **pipeline 聚合 = 桶结果变换: reduce 阶段消费父聚合的 InternalAggregation, 输出新聚合 (avg_bucket 等) — 不参与文档收集, 只在最终 reduce 执行 (Q6 ForFinal)**。PipelineAggregator.java:24,117

## Q8: 与 Redis HLL 对照 — 近似计数两实现

假设: ES cardinality agg 与 Redis PFADD 都是 HyperLogLog — 实现差异 (内存精度权衡)。

验证过程:
- ES: CardinalityAggregator (CardinalityAggregator.java:43-71): HyperLogLogPlusPlus (CardinalityAggregator.java:48) + precision (CardinalityAggregator.java:71) — **可配置精度**
- Redis: hyperloglog.c:44-60: HLL_DENSE/HLL_SPARSE 双编码 (hyperloglog.c:54-55) — 稀疏转稠密
- 对照维度:
  - 数据结构: 都基于 HLL (基数估计, 误差 ~0.81%)
  - ES 可配 precision (内存↔精度); Redis 固定 16384 寄存器
  - Redis sparse 优化小基数内存; ES HyperLogLogPlusPlus 同类
- 面试记忆点: "cardinality agg 就是 Redis PFADD 的 ES 版 — 都是 HLL"

代码类型: 对照分析

结论: **ES cardinality (HyperLogLogPlusPlus, 可配 precision) vs Redis PFADD (HLL_DENSE/SPARSE 双编码) — 同源算法 (HyperLogLog) 两种工程实现: ES 可调精度, Redis 固定 16384 寄存器 + 稀疏优化**。对照锚点: CardinalityAggregator.java:48,71 + hyperloglog.c:44-60

跨域关联: [[r12-hll]] (Redis HLL 域) — 完整对照
