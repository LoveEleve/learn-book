# Pass 2 闭环笔记 Q8: WindowWrap 与 MetricBucket 各解决什么问题

## 验证过程

- `WindowWrap<T>` 解决的是“时间片壳”问题：
  - 持有 `windowLengthInMs`
  - 持有 `windowStart`
  - 持有 bucket 的实际值 `value` (`WindowWrap.java:24-39`)
- 它本身不关心统计事件，只关心“这个值属于哪个时间片”；`isTimeInWindow(timeMillis)` 负责判断某个时间点是否落在该窗口里 (`WindowWrap.java:66-75`)。
- `MetricBucket` 才是真正的“统计值体”：
  - 内部是 `LongAdder[] counters`
  - 每个 `MetricEvent` 占一个槽位 (`MetricBucket.java:29-37`)
  - 同时额外维护 `minRt` (`MetricBucket.java:31-47`)
- `MetricBucket.reset(bucket)` 支持把 future bucket 的 pass 预借值搬运到新 bucket；`reset()` 则清空所有计数 (`MetricBucket.java:39-58`)。

## 结论
`WindowWrap` 是“这个 bucket 属于哪个时间片”的外壳，`MetricBucket` 是“这个时间片里到底记了哪些统计项”的内容体；时间语义和统计语义被明确拆开了。