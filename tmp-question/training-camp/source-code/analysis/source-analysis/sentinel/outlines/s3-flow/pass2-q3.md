# Pass 2 闭环笔记 Q3: 四种 TrafficShapingController 的行为差异

## 验证过程

- `DefaultController`: 超过阈值时默认直接拒绝；若 `prioritized=true`，会尝试 `node.tryOccupyNext(...)`，成功则写 waiting/occupied 统计、sleep 后抛 `PriorityWaitException` (`DefaultController.java:49-64`)。
- `WarmUpController`: 根据当前 passQps 计算 warningQps 与阈值，低于 warning 区间直接放行，进入预热区时逐步收紧，超过 count 才拒绝 (`WarmUpController.java:114-143`)。
- `ThrottlingController`: 把每个请求转换成固定时间成本，用 CAS 推进 `latestPassedTime`；如果排队时间超过 `maxQueueingTimeMs` 就拒绝，否则 sleep 到自己的时间点再放行 (`ThrottlingController.java:70-152`)。
- `WarmUpRateLimiterController`: 继承 `WarmUpController`，先根据 warmingQps/count 计算等待成本，再以 warm-up 阶段的速率排队；它把预热和匀速排队组合在一个 controller 里 (`WarmUpRateLimiterController.java:43-84`)。

## 结论
四种行为不是四个独立的 FlowSlot，而是同一条规则通过 `FlowRule.getRater()` 选择不同 controller：直接拒绝、预热、匀速排队、预热+匀速排队。