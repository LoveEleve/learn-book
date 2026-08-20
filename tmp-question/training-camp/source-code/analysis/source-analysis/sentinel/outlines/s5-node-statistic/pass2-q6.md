# Pass 2 闭环笔记 Q6: OccupiableBucketLeapArray 与 FutureBucketLeapArray 如何支持抢占式流控

## 初始假设
- 抢占式限流只是普通滑窗多记一个 waiting 计数。

## 验证过程
- `ArrayMetric` 默认使用 `OccupiableBucketLeapArray`；只有显式关闭 occupy 支持时才退回普通 `BucketLeapArray` (`ArrayMetric.java:41-48`)。
- `OccupiableBucketLeapArray` 内部再持有一个 `FutureBucketLeapArray borrowArray` (`OccupiableBucketLeapArray.java:26-33`)。
- 这两个数组分工不同：
  - 当前数组：存“当前窗口真正已经发生”的统计；
  - future 数组：存“借到未来窗口的预占 pass”。
- 当新窗口创建或老窗口滚动到新时间片时，`OccupiableBucketLeapArray` 会先去 `borrowArray.getWindowValue(time)` 拿未来借位数据，再把这部分 pass 预填进当前 bucket (`OccupiableBucketLeapArray.java:35-58`)。
- `currentWaiting()` 统计的是 future 数组所有窗口的 pass 总和；`addWaiting(time, acquireCount)` 则把等待请求写入 future 数组对应时间片 (`OccupiableBucketLeapArray.java:60-74`)。
- `FutureBucketLeapArray` 的关键点在 `isWindowDeprecated`：`return time >= windowWrap.windowStart();`，也就是“只为未来窗口保留，一旦时间走到该窗口起点，它就应被消费/淘汰” (`FutureBucketLeapArray.java:47-50`)。

## 代码类型
- Implementation(双数组借位模型)

## 结论
抢占式流控不是在普通滑窗里塞一个 waiting 字段，而是维护了“两套窗口”：当前窗口记真实流量，future 窗口记借位流量；时间推进时，未来借位会被折算进当前 bucket。