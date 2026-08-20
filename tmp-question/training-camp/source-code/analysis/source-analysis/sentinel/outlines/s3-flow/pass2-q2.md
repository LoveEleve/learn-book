# Pass 2 闭环笔记 Q2: FlowRuleChecker 怎么选规则并调用 controller

## 初始假设
- `FlowRuleChecker` 会挑一条最合适的规则来判定。

## 验证过程
- `checkFlow(...)` 先按 resource 名从 `ruleProvider` 取出该资源对应的规则集合，然后**顺序遍历全部规则** (`FlowRuleChecker.java:42-53`)。
- 对每条规则，它调用 `canPassCheck(rule, context, node, count, prioritized)`；只要有一条返回 false，就立即 `throw new FlowException(rule.getLimitApp(), rule)` (`FlowRuleChecker.java:48-51`)。
- 本地模式下，真正的判定路径是：
  1. `selectNodeByRequesterAndStrategy(...)` 选出应该观测哪个 `Node`
  2. `rule.getRater().canPass(selectedNode, acquireCount, prioritized)` 调用规则内置的 `TrafficShapingController` (`FlowRuleChecker.java:73-81`)。
- 所以“规则选择”不是选一条，而是**全量遍历 + 任一命中即拒绝**；“controller 选择”也不是在 checker 里 switch，而是早就封装在 `FlowRule.getRater()` 返回的 controller 里。

## 代码类型
- Glue(规则遍历) + Dispatch(controller 下沉)

## 结论
`FlowRuleChecker` 的策略不是“挑最佳规则”，而是“把该资源的所有流控规则逐条过一遍，只要任一条 controller 判不通过，就抛 `FlowException`”。