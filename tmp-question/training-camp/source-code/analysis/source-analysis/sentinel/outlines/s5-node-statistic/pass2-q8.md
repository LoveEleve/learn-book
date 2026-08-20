# Pass 2 闭环笔记 Q8: eagleeye 与 StatisticSlot 的关系

## 初始假设
- `eagleeye` 直接消费统计节点，属于统计主链的一部分。

## 验证过程
- 在 `node/`、`slots/statistic/`、`slots/nodeselector/`、`slots/clusterbuilder/` 范围内 grep `eagleeye`，零命中。
- 这说明 `StatisticSlot` / `StatisticNode` / `NodeSelectorSlot` / `ClusterBuilderSlot` 与 `eagleeye/` 没有直接代码依赖。
- 结构上两者也不同：
  - S-5 主链是“内存统计面”：节点树 + 滑动窗口 + callback registry。
  - `eagleeye/` 是“日志输出面”：`StatLogger` / `Appender` / `RollingFileAppender` / `Daemon` 等。
- 因此它们是并行子系统，而不是主链里谁调用谁的关系；交汇点只在“都描述统计”，不是在对象调用图上直接耦合。

## 代码类型
- Boundary(并行子系统边界)

## 结论
`eagleeye` 不直接消费 `StatisticSlot` 或节点树，它是独立日志面；S-5 主线应聚焦 Node/StatisticSlot/LeapArray，把 `eagleeye` 作为并行附篇处理。