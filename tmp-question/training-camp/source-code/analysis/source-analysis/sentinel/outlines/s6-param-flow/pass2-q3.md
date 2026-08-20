# Pass 2 闭环笔记 Q3: 参数值维度的统计如何组织

## 验证过程

- `ParameterMetric` 维护三类 map：
  - `ruleTimeCounters`：`(rule, (value, AtomicLong))`，记录每个参数值的时间计数（用于匀速排队）
  - `ruleTokenCounter`：`(rule, (value, AtomicReference<TokenUpdateStatus>))`，令牌计数器
  - `threadCountMap`：`(paramIdx, (value, AtomicInteger))`，按参数下标记录并发线程 (`ParameterMetric.java:46-59`)
- `ParamMapBucket` 是窗口内的参数值聚合桶：按 `RollingParamEvent` 分槽，每个槽是一个 `CacheMap<Object, AtomicInteger>` (`ParamMapBucket.java:31-41`)。
- 二者分工：
  - `ParamMapBucket`：时间窗口内的参数值事件计数（请求/阻塞等）
  - `ParameterMetric`：跨窗口的规则维度索引 + 线程计数 + 令牌/时间计数器

## 结论

热点参数统计不是单一计数器，而是“规则 → 参数值 → 计数”的三层结构。`ParamMapBucket` 管窗口内事件，`ParameterMetric` 管跨窗口的规则索引与线程/令牌计数。