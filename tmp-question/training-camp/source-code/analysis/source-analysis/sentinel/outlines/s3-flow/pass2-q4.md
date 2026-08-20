# Pass 2 闭环笔记 Q4: PriorityWaitException 为什么不是 BlockException

## 验证过程

- `PriorityWaitException` 是独立异常类，继承 `RuntimeException`，不属于 `BlockException` 体系。
- `DefaultController` 在 prioritized 请求成功占用未来窗口后：
  1. `node.addWaitingRequest(...)`
  2. `node.addOccupiedPass(...)`
  3. sleep 等待
  4. 抛出 `PriorityWaitException` (`DefaultController.java:54-64`)
- `StatisticSlot` 专门 catch 它：只增加 threadNum 与对应 callback，不走 BlockException 分支，也不增加 `addPassRequest` (`StatisticSlot.java:81-95`)。
- `FlowSlot` 位于 `StatisticSlot` 后面，收到这个异常时不会继续 `fireEntry`；但 `CtSph` 的外层会把它视作 entry 成功返回路径，因此它表达的是“已占位、等待后通过”，不是“规则拒绝”。

## 结论
`PriorityWaitException` 是一个内部控制流信号：它把“优先级请求已占未来额度”传回统计槽，避免重复记 pass；它不是拒绝，所以不能继承 `BlockException`，也不应被用户当作普通 block 处理。