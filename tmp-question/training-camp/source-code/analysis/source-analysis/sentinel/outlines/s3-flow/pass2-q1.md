# Pass 2 闭环笔记 Q1: FlowSlot 自己做了什么

## 初始假设
- `FlowSlot` 自己就包含主要限流算法。

## 验证过程
- `FlowSlot.entry(...)` 只有两步：
  1. `checkFlow(...)`
  2. `fireEntry(...)` (`FlowSlot.java:154-160`)
- `checkFlow(...)` 也只是把参数转交给 `FlowRuleChecker.checkFlow(...)` (`FlowSlot.java:162-165`)。
- 它自己唯一持有的状态是一个 `FlowRuleChecker checker` 和一个 `ruleProvider`，后者只是 `FlowRuleManager.getFlowRules(resource)` 的函数包装 (`FlowSlot.java:140-150, 171-177`)。
- 结论就是：`FlowSlot` 不是算法实现层，而是“规则入口槽 + 分发壳”。真正的限流判定、规则选择、cluster/local 分支、controller 调用，全都下沉到了 `FlowRuleChecker`。

## 代码类型
- Glue(入口分发槽)

## 结论
`FlowSlot` 本身几乎不做算法，它只负责把“当前资源 + context + node + count + prioritized”这组现场信息交给 `FlowRuleChecker`，通过后再继续后续槽。