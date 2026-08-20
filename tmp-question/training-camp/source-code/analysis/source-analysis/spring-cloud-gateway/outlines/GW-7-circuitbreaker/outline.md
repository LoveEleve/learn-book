# GW-7 熔断与重试 — 下游故障的缓冲垫: 断路器与重试的编排

> 前置: [[GW-2-过滤器链]] (工厂体系) + [[GW-6-限流]] (容错面) | 引出: [[GW-8-路径重写]] | 对照: Resilience4J + gRPC G-6 (重试)
> 🟡 B | 3 KP | [模式: 包装器 + 状态码语义扩展 + 组合]
> Pass 2 闭环: q1(熔断) q3(SCC) q4(组合) — 4/4 全闭环

**读者处境**: `filters: - CircuitBreaker=myCircuitBreaker, fallbackUri=forward:/fallback` — 一行配置。下游挂了: 请求进断路器, 打开后直接走 fallback。这个"缓冲垫"怎么包住过滤器链?5xx 算失败吗?和 Retry 怎么排?

### 1. 熔断编排 — 链包装 + 状态码熔断

场景: 断路器怎么"包住"过滤器链?
源码路径:
- **工厂** (SpringCloudCircuitBreakerFilterFactory.java:55-95): 抽象类; `reactiveCircuitBreakerFactory.create(config.getId())` (L95) — SCC 熔断工厂
- **状态码熔断** (L100-109): `cb.run(chain.filter(exchange).doOnSuccess(v -> { if (statuses.contains(statusCode)) throw new CircuitBreakerStatusCodeException(...); }), ...)` — **成功但响应码在配置集 (如 500/502) → 视为失败** — 成功语义扩展; **fallback 配置自动启用 body 缓存** (L92: enableBodyCaching(routeId) — 内部转发需重放请求体, GW-5 联动)
- **失败分支** (L110-143): fallbackUri → 重构请求 URI (L124-131) → GATEWAY_REQUEST_URL_ATTR → **addExceptionDetails + reset(exchange)** (L136-139, 清路由属性让请求重路由) → **handle(DispatcherHandler)** (L142) — **fallback 走 DispatcherHandler 直转, 不经转发过滤器链** (与 GW-5 ForwardRoutingFilter 的链内 forward:// 是两条路径); 无 fallback → resumeWithoutError (**默认 false** = 抛错, L183 区, 可配放行)
- Config (L169-177): name/fallbackUri/statusCodes
关键设计 (q1): **链包装 + 状态码语义扩展**: 断路器管执行, 状态码白名单把"业务失败"纳入熔断判定。**被放弃的方案: 只看异常** — 网关场景下游 5xx 是常态。 [模式: 包装器] [跨域: SCC **SCC-10** 断路器抽象 (ReactiveCircuitBreaker 8 类)]

### 2. SCC 交叉 — 断路器谁提供

场景: 断路器实现哪来的?
源码路径:
- **接口** (SCC ReactiveCircuitBreaker.java:29-37): `run(Mono)` (L31) + `run(Mono, Function<Throwable, Mono> fallback)` (L37) — GW-7 消费契约
- **工厂** (L95): reactiveCircuitBreakerFactory.create(id) — 配置隔离
- **Resilience4J 版** (SpringCloudCircuitBreakerResilience4JFilterFactory.java:32-37): 继承 + SCC Resilience4J 工厂
关键设计 (q3): **SCC 能力消费**: 断路器实现 (Resilience4J)/配置全在 SCC; 网关只做包装与编排。**被放弃的方案: 网关自实现** — 跨组件统一熔断。 [架构: 分层消费] [跨域: SCC **SCC-10**]

### 3. 熔断+重试组合 — 链顺序即语义

场景: CircuitBreaker 和 Retry 怎么排?
源码路径:
- **独立过滤器**: 各 GatewayFilterFactory, 链内 order 排序 (GW-2)
- **组合语义**: CircuitBreaker 在外层 → 熔断兜底"含重试的调用" (重试后仍失败才熔断); Retry 在外 → 熔断打开后重试无意义
- **Retry** (RetryGatewayFilterFactory.java:78-229): RetryConfig (retries/statuses/methods/backoff 嵌套 L73-74) + **Backoff.exponential** (L221-222) + exceedsMaxIterations (L229); **安全默认值** (L305-315): **retries=3 + series=[SERVER_ERROR] + methods=[GET] + exceptions=[IOException, TimeoutException]** — **默认只重试幂等 GET + 5xx** (series 匹配: statusCode.series() 比对, L95-104), 非幂等方法需显式配置 (与 gRPC G-6 的 retryableStatusCodes 必填对照)
关键设计 (q4): **过滤器顺序即语义**: 推荐 CircuitBreaker 外层。**被放弃的方案: 内置组合过滤器** — 独立过滤器自由组合 (限流+熔断+重试任意排)。**配套: FallbackHeadersGatewayFilterFactory** — 熔断异常经 CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR (addExceptionDetails L162) 存 exchange, fallback 路由内此过滤器把异常类型/消息写响应头 (L48-54) — 熔断详情传递闭环。 [跨域: GW-2 链排序; gRPC G-6 退避对照] [Reactor: Backoff]

### 核心悬念

"容错面 (限流+熔断+重试) 完成 — 路径处理: RewritePath/StripPrefix 等 4 种路径操作。" 下一域 [[GW-8-路径重写]]。

### 负面空间 (不做)

1. 不写 Resilience4J 内部 (SCC 域)
2. 不写断路器状态机细节 (CLOSED/OPEN/HALF_OPEN — SCC 面)
3. 不写 Retry 的 Reactor Retry/Repeat API 全貌
4. 不写 fallback 内部转发的 DispatcherHandler 细节 (WebFlux 面)
5. 不写 CircuitBreakerStatusCodeException 层次
6. 不写各熔断实现 (Hystrix/Resilience4J) 对比 (SCC 面)
