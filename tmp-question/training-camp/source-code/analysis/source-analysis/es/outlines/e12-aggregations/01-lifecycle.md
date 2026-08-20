# E-12 Aggregations 篇 1/3 — 聚合生命周期: 挂载/组合/桶上限

> 前置: [[E-2-search-01]] (QueryPhase 挂载) [[E-11-fielddata-01]] | 复用: — | 对照: [[rd5-rmap]] (聚合语义) | 引出: [[E-12-aggregations-02]] [[E-12-aggregations-03]]
> 🔴 A | 来源: AggregationPhase.java:27-39 + AggregatorBase.java:35,219-232 + MultiBucketConsumerService.java:32-38,55-60
> 定位: Aggregations 卷开篇 — 回答"聚合怎么挂到查询上? 递归组合怎么工作?"

**读者处境**: 你写 `terms agg → date_histogram → avg` 三层嵌套 — 面试官问 "ES 聚合怎么执行的? 嵌套聚合怎么组织?" 你答 "一次遍历" — 但再问 "桶上限 65536 怎么保护? reduce 分几阶段?" 你卡住了。这篇是聚合生命周期的完整答案。

### 1. 问题引入 — 聚合与查询共享一次遍历

场景: 一次搜索同时要"相关文档 + 各字段统计" — 怎么不查两遍?
- AggregationPhase.preProcess (AggregationPhase.java:27-39): 查询前创建聚合器树
- E-2 衔接: QueryPhase.executeQuery (QueryPhase.java:133) 调用 → 聚合 collector 挂到查询 collector (E-2 已验证)
- 本篇问题: 组合模板 (Q1) / 桶上限 (Q2) / reduce (Q6)

### 2. 递归组合 — getLeafCollector 模板链

场景: 嵌套聚合怎么在一次遍历里全部执行?
- AggregatorBase (AggregatorBase.java:35): getLeafCollector 模板 (AggregatorBase.java:219-232): preGetSubLeafCollectors (AggregatorBase.java:220) → 子聚合 collector (AggregatorBase.java:221) → 父包装 (AggregatorBase.java:222)
- collectableSubAggregators (AggregatorBase.java:46): 子聚合组合
- postCollection (AggregatorBase.java:294-298): doPostCollection (AggregatorBase.java:296) + 子聚合递归 (AggregatorBase.java:297) — 桶排序/截断 top (terms 按 bucketCountThresholds: size/minDocCount)
- 效果: 一次文档遍历逐层喂给整棵聚合树

### 3. 桶上限 — 65536 保护

场景: 高基数 terms 聚合会不会内存爆炸?
- MultiBucketConsumerService (MultiBucketConsumerService.java:32-38): DEFAULT_MAX_BUCKETS=65536 (MultiBucketConsumerService.java:32) 可配
- TooManyBucketsException (MultiBucketConsumerService.java:55-60): 超限抛错拒绝
- 语义: 防"每文档一桶" — 高基数聚合的安全阀
- **运维视角**: 线上 TooManyBucketsException = 桶超 65536 — 调 index.max_buckets / 排查高基数字段 (改 composite 分页)

### 4. reduce — 跨分片合并两阶段

场景: 各分片收集完, 协调节点怎么合并?
- AggregationReduceContext (AggregationReduceContext.java:23): sealed permits ForPartial/ForFinal
- 语义 (AggregationReduceContext.java:77-81): pipeline 只在最终阶段执行
- 流程: 分片 postCollection → ForPartial (跨分片) → ForFinal (pipeline)

### 5. 收束 — 生命周期的骨架

- 挂载 (查询前建树) → 收集 (一次遍历) → reduce (两阶段合并)
- 时空溯源: v0.90 FacetExecutor (手写) → 2013-11-24 aggregations 引入 → 2014-08-21 facet 移除
- 引出: 篇 2 (桶聚合 terms/composite) — 篇 3 (pipeline/HLL 对照)

### 核心悬念
"三层嵌套聚合怎么在一次遍历里全部算完?" — getLeafCollector 模板委托链: 每层聚合先取子层 collector, 再包自己的 — 一次文档遍历逐层喂整棵树。

### 概念依赖链
Q1 递归组合 → Q2 桶上限 → Q6 reduce → (时空溯源)

### 源码锚点清单
- AggregationPhase.java:27-39 (preProcess) / 23 (类)
- AggregatorBase.java:35 (类) / 46 (collectableSubAggregators) / 294-298 (postCollection 主实现) / 219-232 (getLeafCollector 模板)
- MultiBucketConsumerService.java:32 (DEFAULT_MAX_BUCKETS) / 33-35 (MAX_BUCKET_SETTING) / 55-60 (TooManyBucketsException)
- AggregationReduceContext.java:23 (sealed) / 77-81 (ForFinal 语义)
