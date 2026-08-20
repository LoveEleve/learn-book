# OF-5 负载均衡 — Pass 2 闭环 Q3: Retryable 版 (Spring Retry)

> 核心: RetryableFeignBlockingLoadBalancerClient | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 重试版怎么重试? 策略怎么选?**

## 机制链 (已实证)

```
extends FeignBlockingLoadBalancerClient (287 行)
execute (L129-160):
├── **LoadBalancedRetryPolicy = loadBalancedRetryFactory.createRetryPolicy(serviceId, loadBalancerClient)** (L133-134)
├── **RetryTemplate = buildRetryTemplate(serviceId, request, retryPolicy)** (L135)
└── retryTemplate.execute(context -> ...) (L137):
    ├── **RetryableRequestContext** (L148-150) — 可重试上下文 (含请求数据)
    ├── LoadBalancerLifecycle (Retryable 版, 类型过滤)
    └── 重试循环内: 策略从上下文选实例 + choose

buildRetryTemplate (L227-238) — 策略三态:
├── BackOffPolicy = loadBalancedRetryFactory.createBackOffPolicy (L229)
│   └── null → **NoBackOffPolicy** (L230)
├── retry 未启用 (properties.getRetry().isEnabled()==false) 或策略 null →
│   **NeverRetryPolicy** (L237-238) ← 开关关闭 = 不重试
└── 启用 → **InterceptorRetryPolicy(toHttpRequest(request), retryPolicy, ...)** (L238)
    ← 拦截式重试 (HTTP 请求重试语义)
```

## 关键设计 (why)

1. **Spring Retry 而非 Feign Retryer**: RetryTemplate + BackOffPolicy — Spring 生态重试 (对比 feign 本体 Retryer)
2. **策略三态**: 可配 BackOff / 关闭 Never / 启用 Interceptor — 开关语义清晰
3. **RetryableRequestContext**: 重试上下文携带请求数据 — 每次重试可重建请求
4. **LoadBalancedRetryPolicy**: 跨实例重试策略 (SCC 抽象) — 同服务/换服务重试配置

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| LoadBalancedRetryPolicy 创建 | RetryableFeignBlockingLoadBalancerClient.java:133-134 |
| RetryTemplate.execute | RetryableFeignBlockingLoadBalancerClient.java:137 |
| RetryableRequestContext | RetryableFeignBlockingLoadBalancerClient.java:148-150 |
| 策略三态 | RetryableFeignBlockingLoadBalancerClient.java:227-238 |

## 负面空间 (Q3 面)

- 不无限制重试 (策略限次)
- 不背压退避 (BackOff 可配但默认 No)
- 不重试业务响应 (仅网络/状态码判定)
