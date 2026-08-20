# Pass 2 闭环笔记 Q4: ResponseTimeCircuitBreaker 如何定义慢调用

## 验证过程

- 构造时要求规则 grade 是 RT，读取：
  - `maxAllowedRt = round(rule.count)`
  - `maxSlowRequestRatio`
  - `minRequestAmount` (`ResponseTimeCircuitBreaker.java:35-50`)
- 请求完成时从 entry 读取 `completeTimestamp`；如果没有完成时间，则用当前时间补齐。RT = completeTimestamp - createTimestamp (`ResponseTimeCircuitBreaker.java:56-70`)。
- 当 `rt > maxAllowedRt` 时，`slowCount + 1`；每个完成请求都 `totalCount + 1`。
- CLOSED 状态下遍历滑窗，先要求 totalCount 达到 minRequestAmount，再计算 slowCount / totalCount；比例严格超过阈值时 OPEN。阈值为 1.0 且比例恰好等于 1.0 时也会 OPEN，这是代码中的边界特判 (`ResponseTimeCircuitBreaker.java:72-109`)。
- HALF_OPEN 时只检查探测请求的 rt：慢则重新 OPEN，否则 CLOSED。

## 结论
慢调用的边界是严格 `rt > maxAllowedRt`，不是大于等于；熔断触发是慢调用比例超过阈值，并对 100% 慢调用的等值边界做了额外处理。