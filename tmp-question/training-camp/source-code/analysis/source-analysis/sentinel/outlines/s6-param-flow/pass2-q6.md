# Pass 2 闭环笔记 Q6: 统计织入的时序与存储生命周期

## 验证过程

- `ParamFlowStatisticSlotCallbackInit` 是 `InitFunc`，启动时把 entry/exit 两个 callback 注册进 `StatisticSlotCallbackRegistry` (`ParamFlowStatisticSlotCallbackInit.java:30-38`)。
- entry callback 在 `StatisticSlot` 的 pass 路径写 thread 计数；exit callback 在 `StatisticSlot` 的 exit 路径减 thread 计数，且只有 entry 没有 blockError 时才减 (`ParamFlowStatisticEntryCallback.java:32-41`, `ParamFlowStatisticExitCallback.java:27-38`)。
- `ParameterMetricStorage` 是 `ConcurrentHashMap<String, ParameterMetric>`，按 resource name 存；`initParamMetricsFor` 双检锁创建并 `metric.initialize(rule)` (`ParameterMetricStorage.java:32-54`)。
- `ParamFlowRuleManager.aggregateAndPrepareParamRules` 在规则更新时：没有规则则清空全部 metric；资源被删则清该资源 metric；单条规则被删则 `parameterMetric.clearForRule(rule)` (`ParamFlowRuleManager.java:101-125`)。

## 结论

热点参数统计的生命周期由规则驱动：有规则才初始化 metric，规则删除则同步清理。callback 只是把 `ParameterMetric` 的线程计数挂到 `StatisticSlot` 的统计生命周期上，本身不负责窗口/规则判定。