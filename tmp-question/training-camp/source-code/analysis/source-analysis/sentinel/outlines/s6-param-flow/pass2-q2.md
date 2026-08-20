# Pass 2 闭环笔记 Q2: 为什么热点参数限流独立成扩展模块

## 验证过程

- 热点参数限流依赖参数值维度统计（`ParameterMetric` / `ParameterMetricStorage`），这部分统计不是通用资源统计。
- `StatisticSlot` 的通用统计只到资源/origin 维度，无法记录“某个参数值”的流量。
- 因此 Sentinel 通过 `StatisticSlotCallbackRegistry` 的扩展点，由 `ParamFlowStatisticSlotCallbackInit` 注入 entry/exit callback，为热点参数另开一套统计，而不是改动 `StatisticSlot` 主干 (`ParamFlowStatisticSlotCallbackInit.java:30-38`)。
- 模块放在 `sentinel-extension`，也印证了它是可选扩展，不强制所有使用方承担热点统计的开销。

## 结论

热点参数限流独立成扩展模块，本质是因为它需要“参数值维度”的统计，而通用 `StatisticSlot` 只做资源/原点维度。用 callback 注册织入，既能复用主链，又不污染 core 的统计主干。