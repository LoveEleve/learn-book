# E-12 Aggregations — harness 验证记录 (MiniAggregations 13/13)

> 跑法: `javac MiniAggregations.java MiniAggregationsTest.java && java MiniAggregationsTest` (JDK 21)
> 结果: **13/13 PASS** (首跑全绿)

## 验证矩阵

| # | 机制 | 验证点 | 源码对照 | 结果 |
|:--:|---|---|---|:--:|
| A1-A4 | 递归组合 | terms+嵌套 avg 一次遍历全算完 | AggregatorBase.java:219-232 (getLeafCollector 模板链) | PASS |
| B1-B3 | terms 桶计数 | 按值建桶计数 (global ordinals 简化) | GlobalOrdinalsStringTermsAggregator.java:127-139 | PASS |
| C1-C4 | 桶上限 | 超限标记 + 默认 65536 | MultiBucketConsumerService.java:32-38 | PASS |
| D1-D2 | 拒绝语义 | 超限后停止收集 + 桶数=上限 | MultiBucketConsumerService.java:55-60 (TooManyBucketsException) | PASS |

## 验证意义

- 聚合递归组合 (一次遍历喂整树) / terms 编号计数 / 桶上限 65536 保护 — 3 大机制全部可复现
- **未验证面**: global ordinals 真实构建 (需跨段 OrdinalMap)、深度优先延迟桶重放、composite after_key 分页、reduce ForPartial/ForFinal、pipeline reduce 链
- 结论: "组合收集 + 桶计数 + 上限保护" 的聚合核心理解验证到位
