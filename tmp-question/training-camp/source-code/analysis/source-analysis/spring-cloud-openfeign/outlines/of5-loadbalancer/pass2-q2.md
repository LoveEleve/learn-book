# OF-5 负载均衡 — Pass 2 闭环 Q2: 装配与生命周期面

> 核心: FeignLoadBalancerAutoConfiguration + LoadBalancerLifecycle | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 什么条件下装配 LB Client? 生命周期回调有哪些?**

## 机制链 (已实证)

```
FeignLoadBalancerAutoConfiguration (L47-62):
├── **@ConditionalOnClass(Feign.class)** (L47)
├── **@ConditionalOnBean({LoadBalancerClient, LoadBalancerClientFactory})** (L48)
│   ← SCC LoadBalancer 存在才装配!
│   ← **OF-2 生产陷阱的根源**: 无 starter-loadbalancer → 无 LoadBalancerClient
│   → 无此配置类 → 无 Client Bean → loadBalance() 抛 "Did you forget..."
├── XForwardedHeadersTransformer @ConditionalOnMissingBean (L61-62)
└── (Client Bean: FeignBlockingLoadBalancerClient 或 Retryable 版)

LoadBalancerLifecycle (SCC 抽象, 交叉引用 SCC-PLAN):
├── onStart(lbRequest) — 调用开始 (L110-116 消费)
├── onComplete/onError — 完成/异常回调
└── LoadBalancerLifecycleValidator: 类型匹配过滤 (RequestDataContext/ResponseData/ServiceInstance)
```

## 关键设计 (why)

1. **条件装配 = 生产陷阱根源**: @ConditionalOnBean(LoadBalancerClient) — 依赖缺失时整个 Client 装配消失, 且错误在代理创建期 (OF-2) 才暴露
2. **Lifecycle 回调**: onStart/onComplete/onError — 指标/追踪/日志挂钩点 (SCC 标准)
3. **类型过滤**: getSupportedLifecycleProcessors 按上下文类型匹配 — 只调能处理的
4. **Retryable 版条件**: OnRetryNotEnabledCondition — retry 开关控制版本

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| @ConditionalOnBean(LoadBalancer) | FeignLoadBalancerAutoConfiguration.java:47-48 |
| XForwarded 条件 | FeignLoadBalancerAutoConfiguration.java:61-62 |
| onStart 消费 | FeignBlockingLoadBalancerClient.java:110-116 |
| LoadBalancerLifecycleValidator | FeignBlockingLoadBalancerClient.java:111-115 |

## 负面空间 (Q2 面)

- 不 Lifecycle 自动发现 (显式实例)
- 不条件放宽 (严格 LoadBalancer Bean 依赖)
- 不做多 LB 客户端 (单 Client Bean)
