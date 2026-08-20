# S-5 节点与统计域 — 大纲

## 上篇: 一棵树,一个汇总节点 — `01-node-tree.md`

1. `NodeSelectorSlot`：为什么同一资源会在不同 context 下拆成多个 `DefaultNode`
2. `ClusterBuilderSlot`：为什么必须是原型槽，如何把多棵树绑到同一个 `ClusterNode`
3. `EntranceNode` / `DefaultNode` / `ClusterNode` 三层视角
4. `DefaultNode` 如何通过覆写把统计双写到 `ClusterNode`

## 中篇: 统计真正落在哪 — `02-statistic-slot.md`

1. `StatisticSlot.entry`：pass / block / threadNum 收口
2. `StatisticSlot.exit`：rt / success / exception 收口
3. origin node / `ENTRY_NODE` 两条分叉统计线
4. `StatisticSlotCallbackRegistry`：为什么热点参数统计可以织入而不用改主逻辑

## 下篇: 滑动窗口与借未来 — `03-leap-array.md`

1. `StatisticNode` 为什么维护 second/minute 两套 rolling counter
2. `LeapArray` / `ArrayMetric` / `MetricBucket` 的分层
3. `OccupiableBucketLeapArray` + `FutureBucketLeapArray` 的双数组借位模型
4. `NodeBuilder` 和 `eagleeye`：两个容易误入主线的旁枝/遗留物
