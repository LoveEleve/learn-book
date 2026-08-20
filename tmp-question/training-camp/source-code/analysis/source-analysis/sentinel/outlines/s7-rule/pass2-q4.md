# Pass 2 闭环笔记 Q4: property 基础设施与 RuleConstant

## 验证过程

- `SentinelProperty<T>` 是发布-订阅接口：`addListener` / `removeListener` / `updateValue`。`updateValue` 只在值变化时通知 listener (`SentinelProperty.java:24-45`)。
- `DynamicSentinelProperty` 用 `CopyOnWriteArraySet` 存 listener，`addListener` 时立即 `configLoad` 当前值，`updateValue` 时对每个 listener 调 `configUpdate` (`DynamicSentinelProperty.java:25-55`)。
- `NoOpSentinelProperty` 是空实现，`updateValue` 恒返回 true，用于“不需要动态更新”的场景 (`NoOpSentinelProperty.java:20-35`)。
- `SimplePropertyListener` 把 `configLoad` 委托给 `configUpdate`，简化只关心更新的 listener (`SimplePropertyListener.java:20-27`)。
- `RuleConstant` 定义核心常量：
  - grade：`FLOW_GRADE_THREAD=0` / `FLOW_GRADE_QPS=1`
  - degrade：`DEGRADE_GRADE_RT=0` / `EXCEPTION_RATIO=1` / `EXCEPTION_COUNT=2`
  - authority：`AUTHORITY_WHITE=0` / `AUTHORITY_BLACK=1`
  - strategy：`STRATEGY_DIRECT=0` / `RELATE=1` / `CHAIN=2`
  - controlBehavior：`DEFAULT=0` / `WARM_UP=1` / `RATE_LIMITER=2` / `WARM_UP_RATE_LIMITER=3`
  - limitApp：`default` / `other` (`RuleConstant.java:26-63`)

## 结论

property 基础设施是 Sentinel 规则动态更新的统一底座：`SentinelProperty` 定义发布-订阅契约，`DynamicSentinelProperty` 提供默认实现，`NoOpSentinelProperty` 提供空实现。`RuleConstant` 集中定义所有规则类型/策略/行为的数值常量，是各 RuleManager 与 checker 共享的语义字典。