# Pass 2 闭环笔记 Q3: StatisticSlot 具体在哪些行更新 pass/block/rt/exception/threadNum

## 初始假设
- `StatisticSlot` 只是一个总入口，真正的统计写入都在 `StatisticNode` 里分散完成。

## 验证过程
- `StatisticSlot.entry` 在放行后立刻更新通过统计：
  - `node.increaseThreadNum()`
  - `node.addPassRequest(count)` (`StatisticSlot.java:57-58`)
- 如果有 origin node，同步更新 origin 维度的 thread/pass (`StatisticSlot.java:60-64`)。
- 如果是 IN 流量，同步更新全局入口节点 `Constants.ENTRY_NODE` 的 thread/pass (`StatisticSlot.java:66-70`)。
- 若抛 `PriorityWaitException`，只加 thread，不加 pass request，因为请求已经被未来窗口借位处理 (`StatisticSlot.java:73-88`)。
- 若抛 `BlockException`：
  - `context.getCurEntry().setBlockError(e)`
  - `node.increaseBlockQps(count)`
  - origin node / ENTRY_NODE 同步加 block (`StatisticSlot.java:90-108`)
- `StatisticSlot.exit` 在未 block 的情况下计算：
  - `completeTimestamp`
  - `rt = complete - create`
  - `error = curEntry.getError()` (`StatisticSlot.java:123-129`)
- 然后 `recordCompleteFor(...)` 对 node / origin node / ENTRY_NODE 三个视角统一写入：
  - `node.addRtAndSuccess(rt, batchCount)`
  - `node.decreaseThreadNum()`
  - `error != null && !(error instanceof BlockException)` 时 `node.increaseExceptionQps(batchCount)` (`StatisticSlot.java:146-154`)

## 代码类型
- Glue(统一收口统计)

## 结论
`StatisticSlot` 是统计收口点：entry 侧写 thread/pass/block，exit 侧写 rt/success/exception，并同时扇出到三种视角——当前 `DefaultNode`、origin node、全局 `ENTRY_NODE`。