# Pass 2 闭环笔记 Q7: StatisticSlotCallbackRegistry 的 entry/exit callback 时序

## 初始假设
- callback 只是个附加钩子，时序不重要。

## 验证过程
- `StatisticSlotCallbackRegistry` 维护两张表：
  - `entryCallbackMap<String, ProcessorSlotEntryCallback<DefaultNode>>`
  - `exitCallbackMap<String, ProcessorSlotExitCallback>` (`StatisticSlotCallbackRegistry.java:34-37`)
- `StatisticSlot.entry` 有三种 callback 触发点：
  - 正常放行：在 thread/pass 写入之后，遍历 `handler.onPass(...)` (`StatisticSlot.java:71-74`)
  - `PriorityWaitException`：在增加 thread 之后，也走 `handler.onPass(...)` (`StatisticSlot.java:86-89`)
  - `BlockException`：在 block 计数写入之后，走 `handler.onBlocked(...)` (`StatisticSlot.java:102-110`)
- `StatisticSlot.exit` 的 callback 在 `recordCompleteFor(...)` 之后触发：
  - 先算 rt/success/exception
  - 再遍历 `handler.onExit(...)`
  - 最后才 `fireExit(...)` 传给后续槽 (`StatisticSlot.java:123-143`)
- 这意味着 callback 看到的是“统计已经写完”的状态，而不是原始进入状态。
- 这也解释了为什么 S-6 的 `ParamFlowStatisticSlotCallbackInit` 能靠 callback 织入统计，而不用改 `StatisticSlot` 主逻辑。

## 代码类型
- Glue(统计完成后的织入点)

## 结论
`StatisticSlot` 的 callback 不是旁路日志，而是正式扩展点：entry 侧在 pass/block 统计写完后触发，exit 侧在 rt/exception/thread 收尾完成后触发，再继续后续槽。