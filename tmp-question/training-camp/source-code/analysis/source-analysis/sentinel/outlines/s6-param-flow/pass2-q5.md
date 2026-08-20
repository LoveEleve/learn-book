# Pass 2 闭环笔记 Q5: ParamFlowChecker 的判定路径

## 验证过程

- `passCheck` 先取 `paramIdx`，若参数个数不足或参数值为 null，直接放行；若参数值实现 `ParamFlowArgument`，取其 `paramFlowKey()` 作为统计 key (`ParamFlowChecker.java:50-78`)。
- 判定分本地/集群：
  - 集群模式 + QPS → `passClusterCheck`
  - 其余 → `passLocalCheck`
- `passLocalCheck` 对 Collection/数组参数逐元素判定，普通值走单值判定 (`ParamFlowChecker.java:81-106`)。
- 单值判定按 grade 分：
  - QPS + RATE_LIMITER → `passThrottleLocalCheck`（匀速排队）
  - QPS + 默认 → `passDefaultLocalCheck`（简化令牌桶）
  - THREAD → 直接比较并发线程数，并支持 `parsedHotItems` 特殊阈值 (`ParamFlowChecker.java:108-130`)

## 结论

热点参数限流的判定不是单一算法，而是“按 grade + controlBehavior”分派：默认走简化令牌桶，匀速排队走时间线，线程级走并发计数。参数值为 null 或越界时放行，体现“无参数不拦截”。