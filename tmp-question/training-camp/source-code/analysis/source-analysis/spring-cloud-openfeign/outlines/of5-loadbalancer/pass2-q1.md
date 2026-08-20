# OF-5 负载均衡 — Pass 2 闭环 Q1: 基本版 execute

> 核心: FeignBlockingLoadBalancerClient.execute | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: Client 怎么从注册中心选实例并转发? 无实例怎么办?**

## 机制链 (已实证)

```
implements Client (OF-2 unwrap 目标)
execute (L100-168):
├── serviceId = URI host (L101-102) + Assert.state (host 非空)
├── hint = getHint(serviceId) (L104) — 区域提示 (zone)
├── DefaultRequest<RequestDataContext>(buildRequestData, hint) (L107-108)
├── **LoadBalancerLifecycle.onStart** (L110-116) — 回调 (SCC 抽象)
├── **instance = loadBalancerClient.choose(serviceId, lbRequest)** (L118) ← SCC LoadBalancer
├── **instance == null → 503** (L127-131):
│   ├── warn "Load balancer does not contain an instance for the service X" (L127)
│   └── new Response(503, SERVICE_UNAVAILABLE) (L131)
├── reconstructURI(instance, originalUri) (L135) → buildRequest
└── **transformers 链** (L147-154): LoadBalancerFeignRequestTransformer.transformRequest (逐 transformer 改请求)
    getDelegate (L162): OF-2 unwrap 用
```

## 关键设计 (why)

1. **SCC LoadBalancer 抽象消费**: choose/reconstructURI 来自 spring-cloud-commons — 换 LB 实现不改 Feign
2. **503 显式**: 无实例 → 503 + 警告文案 — 生产可诊断 (不是静默失败)
3. **hint 区域提示**: 同 zone 优先 (SCC 语义)
4. **transformer 链**: 请求可被多个 transformer 增强 (XForwarded 等)
5. **Lifecycle 回调**: onStart — 链路追踪/指标挂钩

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| choose | FeignBlockingLoadBalancerClient.java:118 |
| 503 + 警告 | FeignBlockingLoadBalancerClient.java:127-131 |
| reconstructURI | FeignBlockingLoadBalancerClient.java:135 |
| transformers 链 | FeignBlockingLoadBalancerClient.java:147-154 |
| getDelegate | FeignBlockingLoadBalancerClient.java:162 |

## 负面空间 (Q1 面)

- 不实例缓存 (每次 choose)
- 不自动重试 (基本版单次)
- 不粘滞 (每次调用 choose)
