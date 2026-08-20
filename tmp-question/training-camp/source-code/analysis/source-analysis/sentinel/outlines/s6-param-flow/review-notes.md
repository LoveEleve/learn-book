# S-6 热点参数限流域 — 审查记录

## Pass 1 / Pass 2

- 确认 `ParamFlowSlot` 是 `@Spi(order=-3000)` 的扩展槽，位于 System 与 Flow 之间。
- 确认热点限流独立成 extension 的原因：需要参数值维度统计，不属于 core 通用统计。
- 确认统计三层结构：`ParameterMetricStorage` → `ParameterMetric` → 参数值计数。
- 确认 `CacheMap`/`ConcurrentLinkedHashMapWrapper` 用 LRU 限容，防止海量参数值撑爆内存。
- 确认统计织入靠 `ParamFlowStatisticSlotCallbackInit` 注册 entry/exit callback。
- 确认判定按 grade + controlBehavior 分派：默认令牌桶 / 匀速排队 / 线程计数。
- 确认集群模式与普通流控共用 TokenService，`requestParamToken` 携带参数值维度。
- 确认 `HotParamSlotChainBuilder` 是槽 SPI 化前的兼容遗留空类。

## 深审修正

1. 上篇锚点修正：`@Spi` 在 34，`checkFlow` 在 61-88，补充 `ParamFlowArgument.java:26`。
2. 中篇锚点修正：`ParameterMetric` 容量常量在 38-40，三张 map 在 49-58，`addThreadCount` 在 187，`decreaseThreadCount` 在 128，`ParamFlowRuleManager` 清理在 114-132。
3. 下篇锚点修正：`passCheck` 在 45，`passDefaultLocalCheck` 在 124-194，`passThrottleLocalCheck` 在 196-268，`passClusterCheck` 在 270-295。

## 三篇正文

- `01-param-flow.md`：为什么独立、SPI 织入、参数值来源、无参数放行
- `02-param-metric.md`：三层统计、LRU 限容、callback 织入、规则驱动生命周期
- `03-param-checker.md`：三种判定路径、特殊热点项、集群回退、遗留 builder

## 遗留

1. `passDefaultLocalCheck` 的简化令牌桶补充公式（`passTime * tokenCount / durationInSec`）只描述了行为，未逐项推导与标准令牌桶的差异。
2. `ParamFlowRuleUtil.buildParamRuleMap` 的规则解析细节（如 `paramFlowItemList` 如何转 `hotItems`）未单独展开。
3. 集群模式下 `requestParamToken` 的传输协议属于 S-9，本域只验证判定分支与回退。
4. 线程级热点限流不支持集群模式，正文已如实说明，未深挖原因。
