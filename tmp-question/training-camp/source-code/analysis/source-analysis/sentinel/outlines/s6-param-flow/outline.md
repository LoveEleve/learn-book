# S-6 热点参数限流域 — 大纲

## 上篇: 为什么要有独立的热点限流 — 01-param-flow.md

1. 热点参数限流与 FlowSlot 的差异
2. SPI 织入：order=-3000 插在 System 与 Flow 之间
3. `ParamFlowRule` 的 grade/paramIdx/controlBehavior 三要素
4. `paramIdx` 与 `ParamFlowArgument` 如何确定统计 key

## 中篇: 参数值维度的统计 — 02-param-metric.md

1. `ParamMapBucket` / `ParameterMetric` / `ParameterMetricStorage` 分层
2. `CacheMap` 与 `ConcurrentLinkedHashMapWrapper` 的 LRU 淘汰
3. 统计织入：`ParamFlowStatisticSlotCallbackInit` 的 entry/exit callback
4. 规则驱动的统计生命周期

## 下篇: 三种判定路径 — 03-param-checker.md

1. 本地判定：默认令牌桶 / 匀速排队 / 线程计数
2. 特殊热点项（exclusion items）独立阈值
3. 集群模式与回退策略
4. 遗留 `HotParamSlotChainBuilder`
