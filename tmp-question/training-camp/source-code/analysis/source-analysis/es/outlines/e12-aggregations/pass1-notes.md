# E-12 Aggregations — Pass 1 探索笔记 (扫轮廓)

> 🔴 A | 依赖: E-7 ✅ + E-11 ✅ (docValues/global ordinals) + E-2 ✅ (挂载点) | 对照: [[r12-hll]] (Redis HLL) [[rd5-rmap]] (聚合语义)
> 源码: server/src/main/java/org/elasticsearch/search/aggregations/ (516 文件; bucket 257 + metrics 143 + pipeline 46 + support 27)
> 测试地图: server/src/test/.../search/aggregations/ (60+ 文件)

## 继承树/调用图

```
Aggregator (抽象, L33) → AggregatorBase (360 行, L35)
├── BucketsAggregator (桶聚合基类)
│   ├── AbstractStringTermsAggregator → GlobalOrdinalsStringTermsAggregator (974 行, L55)
│   └── CompositeAggregator (633 行, L72)
└── metrics 聚合 (Sum/Min/Max/Avg...) — 143 文件

挂载 (E-2 衔接): QueryPhase.executeQuery (QueryPhase.java:133) → AggregationPhase.preProcess (AggregationPhase.java:27-39)
    → createAggregators → AggregatorCollectorManager (查询+聚合同遍历)
    → 收集 → postCollection → reduce (AggregationReduceContext)

桶上限: MultiBucketConsumerService.DEFAULT_MAX_BUCKETS = 65536 (MultiBucketConsumerService.java:32)
```

## 基本元素分解 (原则二)

1. **Aggregator 生命周期** — AggregatorBase: getLeafCollector (AggregatorBase.java:219-222 模板: 子聚合委托链) + postCollection (AggregatorBase.java:96) — 递归组合
2. **挂载点** — AggregationPhase.preProcess (AggregationPhase.java:27-39): 查询前创建聚合器, 查询时同遍历收集 (E-2 衔接)
3. **桶上限保护** — MultiBucketConsumerService: DEFAULT_MAX_BUCKETS=65536 (MultiBucketConsumerService.java:32) + TooManyBucketsException (L55-60)
4. **terms 加速** — GlobalOrdinalsStringTermsAggregator (GlobalOrdinalsStringTermsAggregator.java:55): global ordinals 直接按全局编号建桶 (E-11 衔接)
5. **composite 聚合** — CompositeAggregator (CompositeAggregator.java:72): 多字段组合桶 + after_key 分页
6. **延迟桶 (深度优先)** — BestBucketsDeferringCollector (BestBucketsDeferringCollector.java:42): 先收集候选桶再二次遍历 — 深度优先 vs 广度优先

## 标记问题 (≥5)

1. **Q1: Aggregator 生命周期怎么组织?** — getLeafCollector 模板方法 (子聚合委托链) + postCollection — 递归组合怎么工作?
2. **Q2: 桶上限怎么保护?** — DEFAULT_MAX_BUCKETS=65536 + TooManyBucketsException — 超限抛什么?
3. **Q3: terms 怎么用 global ordinals 加速?** — GlobalOrdinalsStringTermsAggregator: 直接按全局编号建桶 vs 字符串比较
4. **Q4: 深度优先 vs 广度优先** — BestBucketsDeferringCollector (延迟桶) 是什么? 什么时候用?
5. **Q5: composite 聚合** — 多字段组合桶 + after_key 分页 — 与 terms 区别?
6. **Q6: reduce 阶段 (跨分片合并)** — AggregationReduceContext 怎么合并各分片桶?
7. **Q7: pipeline 聚合** — 46 文件: 桶上再聚合 (avg_bucket/max_bucket...) — 与父聚合区别?
8. **Q8: 与 Redis HLL 对照** — ES cardinality agg vs Redis HLL (r12-hll) — 近似计数两实现

## 已读测试 (2 个)

- `GlobalOrdinalsStringTermsAggregatorTests` 相关 (terms 桶构建)
- `CompositeAggregatorTests` (composite 分页)
- `MultiBucketConsumerServiceTests` (桶上限)

## 完成检查

- [x] 继承树/调用图已画出
- [x] 基本元素分解 (6 元素, 对应源码位置)
- [x] 8 个标记问题, 每个有源码位置
- [x] 已读 3 个测试文件

## 跨域发现

- 来源: E-12 Pass 1 — AggregationPhase.preProcess (AggregationPhase.java:27) 是 E-2 QueryPhase 的挂载点 (已在 E-2 验证 QueryPhase.java:133)
- 发现: terms 聚合是 global ordinals 主要消费者 (E-11 Q8 衔接) — 两个域闭环
- 已对照验证: QueryPhase.java:133 (E-2 交付) + GlobalOrdinalsStringTermsAggregator.java:55
