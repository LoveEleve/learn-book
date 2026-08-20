# SCC-5 @LoadBalanced 客户端 — 时空溯源 (@since 注释锚, git shallow)

> git shallow (单提交) 无法考古; 溯源以 @since 注释 + 类名演进为锚。

## 演进链 (@since 实证)

| 版本 | 事件 | 证据 |
|:--:|:--|:--|
| 1.x | **@LoadBalanced + LoadBalancerInterceptor 诞生** (Ribbon 时代): URL host 即服务名约定 | LoadBalanced.java:34 + LoadBalancerInterceptor.java:33 |
| 3.0.0 | CompletionContext (负载均衡生命周期上下文) | CompletionContext.java:26 (@since 3.0.0) |
| 3.1.2 | BlockingLoadBalancerRequest / HttpRequestLoadBalancerRequest (请求包装族) | BlockingLoadBalancerRequest.java:30 + HttpRequestLoadBalancerRequest.java:25 (@since 3.1.2) |
| 4.1.0 | BlockingRestClassesPresentCondition (阻塞类存在条件) | BlockingRestClassesPresentCondition.java:28 (@since 4.1.0) |
| **4.1.2** | **BlockingLoadBalancerInterceptor + DeferringLoadBalancerInterceptor** — 时序问题解决 (RestTemplate 早于 LoadBalancer) | BlockingLoadBalancerInterceptor.java:26 + DeferringLoadBalancerInterceptor.java:34 (@since 4.1.2) |
| **4.2.0** | AbstractLoadBalancerBlockingBuilderBeanPostProcessor (Builder 后处理器族) | AbstractLoadBalancerBlockingBuilderBeanPostProcessor.java:30 (@since 4.2.0) |

## 版本相关性结论

- **"URL host 即服务名"约定自 1.x 稳定** — @LoadBalanced 的核心契约从未变
- **4.1.2 是时序问题的分水岭** — DeferringLoadBalancerInterceptor 解决"RestTemplate 创建早于 LoadBalancer 可用" (延迟解析)
- **4.2.0 Builder 后处理器** — RestTemplateBuilder/RestClientBuilder 场景的拦截器注入 (新客户端构建方式)
- **Ribbon → LoadBalancerClient 换代** — 新一代抽象 (loadbalancer 模块) 取代 Ribbon, @LoadBalanced 注解面保持兼容
