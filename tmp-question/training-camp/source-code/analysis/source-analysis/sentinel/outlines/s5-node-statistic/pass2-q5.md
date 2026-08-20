# Pass 2 闭环笔记 Q5: DefaultNode 与 ClusterNode 的统计是怎么联动的

## 初始假设
- `DefaultNode` 和 `ClusterNode` 分别维护各自统计，互不影响。

## 验证过程
- `DefaultNode` 继承 `StatisticNode`，但重写了关键写方法：
  - `increaseBlockQps`
  - `increaseExceptionQps`
  - `addRtAndSuccess`
  - `increaseThreadNum`
  - `decreaseThreadNum`
  - `addPassRequest` (`DefaultNode.java:92-123`)
- 这些覆写都是“双写”模式：先 `super.xxx(...)` 写当前 `DefaultNode`，再 `this.clusterNode.xxx(...)` 同步写到全局 `ClusterNode`。
- 所以 `StatisticSlot` 只要对当前 `DefaultNode` 调一次统计写入，`ClusterNode` 会被自动联动更新，不需要在 `StatisticSlot` 里再显式写第二遍。
- 这就是为什么 `StatisticSlot` 在 pass/block/exit 时只拿到 `Node node = context.getCurNode()` 也能同时完成“局部节点 + 全局资源节点”的双视角统计。

## 代码类型
- Implementation(装饰式双写)

## 结论
`DefaultNode` 是统计写入的代理层：局部统计写自己，全局统计顺手透传给 `ClusterNode`。这样 `StatisticSlot` 可以只写当前节点，而不用关心全局汇总细节。