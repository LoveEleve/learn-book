# OF-5 负载均衡 — Pass 1 轮廓记录 (入口展开追踪 00 §2)

> 日期: 2026-08-16 | 源码: 4.3.2 (FeignBlockingLoadBalancerClient 168 / RetryableFeignBlockingLoadBalancerClient 287 / loadbalancer/ 12 文件)
> 09 域级审计: OPENFEIGN-PLAN OF-5 (接受, 合并 F-4) — 断言 "choose/reconstructURI/503 + Spring Retry" 已 grep 验证

## 入口展开 (Level-1~3, 已读源码)

### Level-1: FeignBlockingLoadBalancerClient.execute (L100-168)

```
implements Client (of2 unwrap 目标)
execute (L100-168):
├── serviceId = URI.create(request.url()).getHost() (L101-102)
│   └── Assert.state (host 非空)
├── hint = getHint(serviceId) (L104)
├── DefaultRequest<RequestDataContext>(RequestDataContext(buildRequestData, hint)) (L107-108)
├── **LoadBalancerLifecycle 处理器** (L110-116): getSupportedLifecycleProcessors + forEach(onStart)
├── **instance = loadBalancerClient.choose(serviceId, lbRequest)** (L118) ← SCC LoadBalancer 抽象
├── **instance == null → 503** (L131): "Load balancer does not contain an instance for the service X" (L127-131)
├── reconstructURI(instance, originalUri) → buildRequest (L135+)
└── **transformers 链** (L147-154): LoadBalancerFeignRequestTransformer.transformRequest — 逐 transformer
    getDelegate (L162): unwrap 用 (OF-2)
```

### Level-2: RetryableFeignBlockingLoadBalancerClient (L129-160) — Spring Retry

```
extends FeignBlockingLoadBalancerClient
execute (L129-160):
├── **LoadBalancedRetryPolicy = loadBalancedRetryFactory.createRetryPolicy(serviceId, loadBalancerClient)** (L133-134)
├── **RetryTemplate = buildRetryTemplate(serviceId, request, retryPolicy)** (L135)
├── retryTemplate.execute(context -> ...) (L137):
│   ├── **RetryableRequestContext** (L148-150) — 可重试上下文
│   ├── LoadBalancerLifecycle (Retryable 版)
│   └── 重试循环内 choose (策略在上下文选实例)
└── OnRetryNotEnabledCondition: 开关 (retry 未启用 → 用基本版)
```

### Level-3: 配置与链路面

```
FeignLoadBalancerAutoConfiguration: 自动装配 (Client Bean 选择)
Default/HttpClient5/Http2/OkHttp FeignLoadBalancerConfiguration: 底层 Client 组合 (OF-8 关联)
LoadBalancerFeignRequestTransformer + **XForwardedHeadersTransformer** (L36-56):
└── X-Forwarded-Host/Proto 添加 (链路追踪/网关)
LoadBalancerResponseStatusCodeException: 响应状态码异常 (重试判定用)
LoadBalancerUtils: 工具
```

## 09 域级审计表 (OPENFEIGN-PLAN OF-5 断言 vs 源码)

| 断言 | grep 证据 | 结论 |
|---|---|---|
| "choose (L118)" | loadBalancerClient.choose(serviceId, lbRequest) | 接受 |
| "503 SERVICE_UNAVAILABLE" | instance==null → 503 (L131) | 接受 (补锚: 警告文案 L127) |
| "reconstructURI (L135)" | 存在 (L135) | 接受 |
| "Spring Retry 版" | LoadBalancedRetryPolicy + RetryTemplate (L133-137) | 接受 (补锚: RetryableRequestContext L148-150) |
| "XForwardedHeadersTransformer" | L36-56 (X-Forwarded-Host/Proto) | 接受 |
| 补锚: LoadBalancerLifecycle 回调 | onStart (L110-116) | 补锚 |
| 补锚: transformer 链 | transformRequest (L147-154) | 补锚 |
| 补锚: OnRetryNotEnabledCondition | 开关 (retry 未启用 → 基本版) | 补锚 |

## 待展开 (下一层)

1. buildRetryTemplate 细节 (BackOffPolicy 配置)
2. LoadBalancedRetryPolicy 语义 (重试判定/超时)
3. buildRequestData (Request → RequestData 转换)
4. FeignLoadBalancerAutoConfiguration 条件 (Client Bean 选择逻辑)
5. LoadBalancerLifecycle 完整回调 (onStart/onComplete/onError)
