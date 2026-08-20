# Pass 2 闭环笔记 Q6: FlowRuleManager 的规则更新模式

## 初始假设
- `FlowRuleManager` 只是一个静态 Map，规则直接 replace 即可。

## 验证过程
- `FlowRuleManager` 持有：
  - `RuleManager<FlowRule> flowRules`
  - `currentProperty`
  - `FlowPropertyListener LISTENER` (`FlowRuleManager.java:46-49`)
- 静态块里先 `currentProperty.addListener(LISTENER)`，再启动 `MetricTimerListener` (`FlowRuleManager.java:55-58`)。
- 外部接入新数据源时走 `register2Property(...)`：
  - 先从旧 property 移除监听器
  - 给新 property 加监听器
  - 再把 `currentProperty` 指向新 property (`FlowRuleManager.java:82-90`)
- 直接加载规则时并不直接改 map，而是调用 `currentProperty.updateValue(rules)` (`FlowRuleManager.java:104-106`)。
- 真正落库发生在 `FlowPropertyListener.configUpdate/configLoad`：
  - `FlowRuleUtil.buildFlowRuleMap(value)` 把 List 按 resource 分组
  - `flowRules.updateRules(rules)` 原子替换 (`FlowRuleManager.java:127-139`)
- 这和 `AuthorityRuleManager` / `SystemRuleManager` 的 property listener 模式是同一套路：**管理器不直接收规则，规则必须经 property 事件流进入**。

## 代码类型
- Infrastructure(Property Listener)

## 结论
`FlowRuleManager` 不是裸 Map，而是标准的 `SentinelProperty + Listener + RuleManager` 三件套：外部数据源推送到 property，listener 再把规则编译成按 resource 分组的 map 交给 `RuleManager`。