# Pass 2 闭环笔记 Q7: LeapArray 如何推进滑动窗口

## 验证过程

- 构造时先校验：bucket 数量和总 interval 都必须为正，interval 必须能被 sampleCount 整除；`windowLengthInMs = intervalInMs / sampleCount` (`LeapArray.java:55-70`)。
- `calculateTimeIdx(timeMillis)` 先把时间除以 bucket 长度得到 `timeId`，再对环形数组长度取模 (`LeapArray.java:100-104`)。
- `currentWindow(timeMillis)` 的核心是四态循环 (`LeapArray.java:121-208`)：
  1. 数组位置为空：创建新 `WindowWrap`，CAS 放入；竞争失败则 yield 重试。
  2. 当前位置窗口起点相同：直接复用。
  3. 当前时间领先旧窗口：用条件 `updateLock` 获取短锁，调用 `resetWindowTo(old, windowStart)` 重置。
  4. 当前时间反而落在旧窗口之前：返回临时新窗口，不改数组。
- 读取旧窗口时，`isWindowDeprecated` 用 `time - windowStart > intervalInMs` 判断是否已经完整滑出统计区间 (`LeapArray.java:266-273`)。
- `values(timeMillis)` 遍历环形数组，只收集未过期 bucket；所以 QPS/窗口聚合不需要先手动清空所有旧格 (`LeapArray.java:329-343`)。

## 结论
`LeapArray` 的核心不是一个定时线程，而是“访问时推进”：请求访问当前时间片时，通过索引定位、CAS 创建、短锁重置来惰性推进窗口；读取时再按过期条件过滤旧 bucket。