# OF-5 负载均衡 — Pass 2 闭环 Q4: 链路与配置面

> 核心: XForwarded + 底层组合 + 开关 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 转发链路怎么增强? 底层 Client 怎么组合? retry 开关?**

## 机制链 (已实证)

```
XForwardedHeadersTransformer (L36-70):
├── **xForwarded.isEnabled() 开关** (L52: LoadBalancerProperties.XForwarded)
├── instance null → 原样返回 (L46-48)
└── X-Forwarded-Host/Proto 添加 (L55-56, 从 URI) — 链路追踪/网关

底层 Client 组合 (HttpClient5FeignLoadBalancerConfiguration L53-64):
├── @Import(HttpClient5FeignConfiguration) (L53) — OF-8 客户端配置
├── delegate = new ApacheHttp5Client(httpClient5) (L63)
└── **new FeignBlockingLoadBalancerClient(delegate, ...)** (L64) — LB 装饰底层 Client
    ← 装饰链: FeignBlockingLoadBalancerClient → ApacheHttp5Client (HttpClient5)

开关与异常:
├── **OnRetryNotEnabledCondition** (L34-50): spring.cloud.loadbalancer.retry.enabled=false
│   → 用基本版 (不重试) — 条件注释 L29
└── **LoadBalancerResponseStatusCodeException extends RetryableStatusCodeException** (L31)
    → 可重试状态码判定 (重试策略消费)
```

## 关键设计 (why)

1. **XForwarded 可开关**: xForwarded.enabled — 链路信息传递 (网关场景)
2. **装饰链**: LB Client 装饰底层 HTTP Client (HttpClient5/Apache/OkHttp) — 职责分离 (LB 选实例 + 底层发请求)
3. **retry.enabled 开关**: false → 基本版 (无重试) — 配置驱动版本选择
4. **RetryableStatusCodeException**: 状态码可重试性判定 (5xx/超时) — 重试语义基础

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| xForwarded 开关 | XForwardedHeadersTransformer.java:52 |
| X-Forwarded 添加 | XForwardedHeadersTransformer.java:55-56 |
| delegate 组合 | HttpClient5FeignLoadBalancerConfiguration.java:53-64 |
| retry.enabled 条件 | OnRetryNotEnabledCondition.java:34-50 |
| RetryableStatusCodeException | LoadBalancerResponseStatusCodeException.java:31 |

## 负面空间 (Q4 面)

- 不 XForwarded 默认开 (需配置)
- 不透明转发 (transformers 显式)
- 不自动选底层 Client (配置类选择)
