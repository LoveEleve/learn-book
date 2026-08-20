# 闭环笔记 GW-2-q5 — 过滤器工厂族: SPI + Retry 实证

假设: GatewayFilterFactory 是工厂 SPI (apply(config) → GatewayFilter); Retry 过滤器实证工厂模式 (Config 绑定 + Reactor Backoff)。

验证过程:
- **工厂 SPI**: GatewayFilterFactory (config → GatewayFilter); OrderedGatewayFilter (OrderedGatewayFilter.java:27-44) — GatewayFilter + 显式 order 包装 (L33)
- **RetryGatewayFilterFactory** (RetryGatewayFilterFactory.java:54-301): extends AbstractGatewayFilterFactory<RetryConfig> (L54); **短路字段** (L73-74): "retries", "statuses", "methods", "backoff.firstBackoff", "backoff.maxBackoff", "backoff.factor", "backoff.basedOnPreviousValue", "jitter.randomFactor", "timeout" — **嵌套绑定 (backoff.*) 实证 v4 配置体系**
- **apply** (L78): 构建重试过滤器 — **Backoff.exponential(firstBackoff, maxBackoff, factor, basedOnPreviousValue)** (L221-222, Reactor 退避 — 对照 gRPC G-6 的 1.6x 指数)
- **exceedsMaxIterations** (L229): 尝试次数上限检查
- RetryConfig (L301): implements HasRouteId — 短路绑定目标
- 39 种工厂分类: 值操作 (Add/Set/Remove Request/Response Header 等 13 种)/路径 (4)/状态 (SetStatus/RedirectTo)/安全 (SecureHeaders 416)/容错 (Retry 535/CircuitBreaker)/限流 (RequestRateLimiter)

代码类型: Implementation (工厂族)

结论: 工厂族 = **统一 SPI + 类型化 Config**: 每个工厂声明 Config 类 + 短路字段; Retry 实证三层: 工厂 (apply)→ 配置 (RetryConfig 嵌套绑定)→ Reactor 退避 (Backoff.exponential)。**被放弃的方案: 过滤器直接 new** — 工厂+绑定让 YAML/DSL 统一实例化路径。 [跨域: v4 配置绑定; gRPC G-6 退避对照 (1.6x vs factor)] [Reactor: Backoff] (RetryGatewayFilterFactory.java:54-229; OrderedGatewayFilter.java:27-44)
