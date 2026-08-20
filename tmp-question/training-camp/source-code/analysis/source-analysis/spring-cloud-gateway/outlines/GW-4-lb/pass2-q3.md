# 闭环笔记 GW-4-q3 — SCC 交叉: ReactiveLoadBalancerClient 消费

假设: Gateway 的 LB 是 SCC 的 ReactiveLoadBalancerClient 消费端 — choose 委托 SCC (LoadBalancerClientFactory), 默认策略 (RoundRobin) 由 SCC 提供。

验证过程:
- **依赖实证** (v2 审计): gateway→SCC import 9 文件 — ReactiveLoadBalancerClientFilter/LoadBalancerServiceInstanceCookieFilter/装配 4/刷新 2
- **工厂注入** (ReactiveLoadBalancerClientFilter.java:80-86): `LoadBalancerClientFactory clientFactory` 构造注入 — **SCC 的工厂** (SCC-PLAN **SCC-7** 交叉: ReactorLoadBalancer 策略 — LoadBalancerClientFactory/RoundRobin)
- **choose** (L118): `ReactiveLoadBalancerClient` — SCC 抽象, 默认 **RoundRobinLoadBalancer** (SCC) / 可配服务级策略
- **粘性会话** (LoadBalancerServiceInstanceCookieFilter.java:45-56): 选中实例写 cookie (粘同一实例) — SCC ServiceInstance 消费
- 交叉引用原则: SCC 是另一 AI 并行域 — Gateway 侧只声明消费, 不重复探索 SCC 内部 (以 SCC-PLAN.md 锚点为基准)

代码类型: Glue (SPI 消费)

结论: Gateway 的 LB = **SCC 能力的消费面**: choose (每请求)/粘性 cookie/策略选择全委托 SCC; 网关只做"URL 装配 + 404 语义 + 转发衔接"。**被放弃的方案: 网关自实现 LB** — 复用 Spring Cloud 统一负载均衡 (跨组件一致)。 [跨域: SCC SCC-7 (LoadBalancerClientFactory/RoundRobin); GW-5 转发衔接] [架构: 分层消费] (ReactiveLoadBalancerClientFilter.java:80-118; LoadBalancerServiceInstanceCookieFilter.java:45-56)
