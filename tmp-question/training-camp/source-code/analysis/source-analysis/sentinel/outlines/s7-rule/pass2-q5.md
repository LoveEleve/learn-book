# Pass 2 闭环笔记 Q5: 各 RuleManager 的统一模式

## 验证过程

- 对比 `AuthorityRuleManager`、`SystemRuleManager`、`FlowRuleManager`、`DegradeRuleManager`、`ParamFlowRuleManager`，它们共享同一套骨架：
  1. 一个 `RuleManager<R>` 实例存规则
  2. 一个 `RulePropertyListener` 实现 `PropertyListener`
  3. 一个 `SentinelProperty<List<R>> currentProperty`（默认 `DynamicSentinelProperty`）
  4. 静态块里 `currentProperty.addListener(LISTENER)`
  5. `register2Property` 换 property 时先移除旧 listener 再加新 listener
  6. `loadRules` 走 `currentProperty.updateValue(rules)`
- 差异点：
  - `SystemRuleManager` 额外维护系统状态采样线程（`SystemStatusListener` 每秒采样）
  - `DegradeRuleManager` 额外维护 `RuleManager<CircuitBreaker>` 与 `RuleManager<DegradeRule>` 双容器
  - `AuthorityRuleManager` 强制“一资源一规则”
  - `ParamFlowRuleManager` 额外清理参数统计 metric

## 结论

所有 RuleManager 都遵循“property 发布-订阅 + RuleManager 容器 + listener 编译规则”的统一模式。差异只在各自规则的编译逻辑和额外状态（系统采样、断路器、参数统计）上。这是 Sentinel 规则管理高度一致性的来源。