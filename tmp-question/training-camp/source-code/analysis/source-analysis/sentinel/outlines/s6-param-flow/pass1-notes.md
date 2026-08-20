# S-6 热点参数限流域 — Pass 1 轮廓记录

> 日期: 2026-08-17 | 范围: `sentinel-extension/sentinel-parameter-flow-control/` (35 文件)

## 模块结构

- 规则与判定：`ParamFlowRule` / `ParamFlowRuleManager` / `ParamFlowRuleUtil` / `ParamFlowItem` / `ParamFlowClusterConfig` / `ParamFlowChecker` / `ParamFlowSlot` / `ParamFlowException` / `ParamFlowArgument` / `TokenUpdateStatus` / `RollingParamEvent`
- 参数统计：`ParameterMetric` / `ParameterMetricStorage` / `ParamMapBucket` / `CacheMap` / `ConcurrentLinkedHashMapWrapper`
- 织入统计槽：`ParamFlowStatisticSlotCallbackInit` / `ParamFlowStatisticEntryCallback` / `ParamFlowStatisticExitCallback`
- 兼容旧版：`HotParamSlotChainBuilder`
- command handler：`GetParamFlowRulesCommandHandler` / `ModifyParamFlowRulesCommandHandler`
- 测试：`ParamFlowCheckerTest` / `ParamFlowDefaultCheckerTest` / `ParamFlowThrottleRateLimitingCheckerTest` / `ParamFlowSlotTest` / `ParamFlowPartialIntegrationTest` / `ParameterMetricTest` / `ParameterMetricStorageTest` / `ParamMapBucketTest`

## Pass 1 观察

- 本模块在 `sentinel-extension` 下，不在 `sentinel-core`。热点参数限流是 SPI 织入的扩展，不是 core 内置槽。
- 判定与统计分离：判定在 `ParamFlowChecker` / `ParamFlowSlot`；统计在 `ParameterMetric` / `ParameterMetricStorage`。
- 统计织入靠 `ParamFlowStatisticSlotCallbackInit` 注册 entry/exit callback 到 `StatisticSlot`（S-5 已确认 callback registry 是扩展点）。
- 参数统计可能按“参数值”做维度，用 `ParamMapBucket` / `CacheMap` 保存不同热点 key 的计数。
- 三种测试 checker 暗示有 direct / default / throttle-rate-limiting 三种判定路径。

## 标记问题

1. `ParamFlowSlot` 在 SPI 链里的 order 是多少？它与 `FlowSlot` 的相对位置如何？
2. 热点参数限流为何不放在 `FlowSlot` 内部，而要独立成扩展模块？
3. `ParamFlowRule` 的 `paramIdx` / `count` / `grade` / `controlBehavior` 如何组合？
4. `ParamFlowChecker` 如何拿到“当前请求的某个参数值”？
5. `ParameterMetric` / `ParameterMetricStorage` 如何按参数值聚合统计？
6. `ParamMapBucket` 与普通 `MetricBucket` 的区别？
7. `CacheMap` 和 `ConcurrentLinkedHashMapWrapper` 的淘汰策略是什么？
8. 统计织入的 entry/exit callback 分别在什么时候触发，统计什么？
9. `HotParamSlotChainBuilder` 为什么是 `@Deprecated`，它与默认链构建器是什么关系？
10. 热点限流与 cluster 模式(`ParamFlowClusterConfig`)如何协同？
11. `ParamFlowRuleManager` 是否也走 property/listener 更新模式？
12. 判定时的 `count`(阈值)是 QPS 还是并发线程，是否支持匀速排队？
