# 闭环笔记 GW-7-q4 — 熔断+重试组合: 链内顺序语义

假设: 限流/熔断/重试是独立过滤器, 链内按 order 组合: 重试 (RetryGatewayFilterFactory) 在熔断外层还是内层?组合语义是什么?

验证过程:
- **独立过滤器**: RequestRateLimiter (GW-6)/CircuitBreaker (GW-7)/Retry — 各自 GatewayFilterFactory, 链内按 order 排序 (GW-2 q1 合并排序)
- **组合语义**: 配置 `- CircuitBreaker=...` + `- Retry=...` — 顺序决定语义:
  - Retry 在外 (order 小): 重试包含熔断触发 → 熔断打开后重试无意义
  - CircuitBreaker 在外: 熔断包含重试 → 熔断判定基于"重试后结果" — **更合理** (一次调用含重试)
- **Retry 内部** (RetryGatewayFilterFactory.java:78-229): RetryConfig (retries/statuses/methods/backoff 嵌套, L73-74) + **Backoff.exponential** (L221-222) + exceedsMaxIterations (L229)
- **顺序建议**: 官方文档惯例: CircuitBreaker 在最外 (fallback 兜底整个调用)
- 与 gRPC 对照: gRPC RetriableStream (G-6) 是调用内重试; 网关是过滤器级组合

代码类型: Glue (组合语义)

结论: 组合 = **过滤器顺序即语义**: 推荐 CircuitBreaker 外层 (熔断兜底含重试的调用); Retry 的 backoff (exponential) 与 gRPC G-6 对照。**被放弃的方案: 内置组合过滤器** — 独立过滤器让组合自由 (限流+熔断+重试任意排)。 [跨域: GW-2 链排序; GW-6 限流组合; gRPC G-6 对照] [Reactor: Backoff] (RetryGatewayFilterFactory.java:78-229; SpringCloudCircuitBreakerFilterFactory.java:100-143)
