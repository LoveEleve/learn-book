# OF-5 负载均衡 — LB 装饰与 Spring Retry

> 前置: [[OF-2-代理创建与装配]] (loadBalance 路径) | 引出: [[OF-8-HTTP客户端与压缩]] | 对照: SCC (C-7 LoadBalancer) + Feign 本体 Client
> 🔴 A | 8 KP | [模式: 装饰器 + 条件装配 + 重试模板]
> Pass 2 闭环: q1(基本版) q2(装配生命周期) q3(Retryable 版) q4(链路配置)

**读者处境**: 无 url 的 @FeignClient 调用时, 请求怎么选实例? 无实例怎么办? 重试怎么配? 这篇拆 FeignBlockingLoadBalancerClient (168) + Retryable 版 (287)。

### 1. 基本版 execute — choose → 503 → reconstructURI

场景: Client 怎么转发?
源码路径:
- **execute** (L100-168): serviceId = URI host → hint → LoadBalancerLifecycle.onStart → **choose(serviceId, lbRequest)** (L118)
- **instance == null → 503** (L127-131): warn "does not contain an instance for the service X"
- reconstructURI (L135) → **transformers 链** (L147-154: transformRequest 逐增强) + getDelegate (L162, OF-2 unwrap); ⚠ **onComplete 回调** (L127: CompletionContext) + **统一执行封装**: LoadBalancerUtils.buildRequestData (L80-83: Request→RequestData) + **executeWithLoadBalancerLifecycleProcessing** (L77/186: 执行+Lifecycle 统一处理)
关键设计 (q1): **SCC LoadBalancer 抽象消费 + 503 显式 + transformer 链**。[模式: 基本版]

### 2. 装配与生命周期面 — 条件 + 回调

场景: 什么条件下装配? 回调?
源码路径:
- **FeignLoadBalancerAutoConfiguration** (L47-62): **@ConditionalOnBean({LoadBalancerClient, LoadBalancerClientFactory})** (L48) — **OF-2 生产陷阱根源!** (无 starter-loadbalancer → 无 Client → loadBalance 抛错)
- **LoadBalancerLifecycle**: onStart (L110-116) / onComplete / onError + LoadBalancerLifecycleValidator 类型过滤
关键设计 (q2): **条件装配缺失即陷阱 + Lifecycle 回调挂钩**。[模式: 装配面]

### 3. Retryable 版 — Spring Retry

场景: 重试怎么配?
源码路径:
- **execute** (L129-160): **LoadBalancedRetryPolicy** (L133-134) + **RetryTemplate** (L135) → execute 重试循环 (**RetryableRequestContext** L148-150)
- **buildRetryTemplate 三态** (L227-238): BackOffPolicy (可配/null→NoBackOff) / **NeverRetryPolicy** (未启用) / **InterceptorRetryPolicy** (启用)
关键设计 (q3): **Spring Retry 而非 Feign Retryer + 策略三态开关**。[模式: 重试面]

### 4. 链路与配置面 — XForwarded + 装饰链

场景: 链路增强? 底层组合?
源码路径:
- **XForwardedHeadersTransformer** (L36-70): **xForwarded.isEnabled() 开关** (L52) → X-Forwarded-Host/Proto
- **装饰链** (HttpClient5FeignLoadBalancerConfiguration L53-64): delegate = ApacheHttp5Client → **new FeignBlockingLoadBalancerClient(delegate)** (L64) — LB 装饰底层 (OF-8); ⚠ **三底层组合**: HttpClient5 (L53-64) / **OkHttp** (OkHttpFeignLoadBalancerConfiguration L58-73, 含 Retryable 版 L72) / **Http2** (@ConditionalOnClass({Http2Client, HttpClient}) L45-57)
- **OnRetryNotEnabledCondition** (L34-50): retry.enabled=false → 基本版; **RetryableStatusCodeException** (L31) 可重试判定; ⚠ **重试版从 LoadBalancedRetryContext 选实例** (L150-168: debug/warn 日志 + reconstructURI)
关键设计 (q4): **XForwarded 可开关 + 装饰链职责分离 + retry 开关**。[模式: 链路面]

## 代码类型
Architecture (装饰) + 重试语义

## 负面空间 (OF-5, 6 条)

| 不做 | 说明 |
|:--|:--|
| 不实例缓存 | 每次 choose (q1) |
| 不自动重试 | 基本版单次 (q1) |
| 不粘滞 | 每次调用 choose (q1) |
| 不 Lifecycle 自动发现 | 显式实例 (q2) |
| 不无限制重试 | 策略限次 (q3) |
| 不 XForwarded 默认开 | 需配置 (q4) |

## 结尾桥 OUTBOUND

- → [[OF-8-HTTP客户端与压缩]]: 底层 Client 组合 (HttpClient5/Apache/OkHttp)
- → 对照: SCC C-7 LoadBalancer (choose/reconstructURI 抽象) / Feign 本体 Client 接口
