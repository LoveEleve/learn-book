# SCC-5 @LoadBalanced 客户端 — 知识规划 (KP)

> 域: SCC-5 | 级别: 🔴 | 方案: A | 大纲: outlines/scc5-loadbalanced/outline.md (6 节)

## §01 域定位

@LoadBalanced = RestTemplate 负载均衡的拦截魔法。@Qualifier 标记 + LoadBalancerInterceptor (URL host 即服务名) + LoadBalancerClient 双 execute + BlockingLoadBalancerClient (阻塞包装响应式) + 装配收集。

## §02 源文件清单

| 文件 | 职责 | 归属节 |
|:--|:--|:--:|
| loadbalancer/LoadBalanced.java | @Qualifier 标记注解 | 1 |
| loadbalancer/LoadBalancerInterceptor.java | 拦截 + 服务名提取 | 2 |
| loadbalancer/LoadBalancerRequestFactory.java | 请求包装 + transformers | 3 |
| loadbalancer/LoadBalancerClient.java | 双 execute + reconstructURI | 4 |
| loadbalancer/ServiceInstanceChooser.java | choose 契约 | 4 |
| loadbalancer/blocking/client/BlockingLoadBalancerClient.java | 阻塞包装响应式 | 5 |
| loadbalancer/LoadBalancerAutoConfiguration.java | 收集 + 延迟拦截器 | 6 |

## §05 闭环要点 (Pass 2 内化)

### q1 注解面
@Qualifier 标记 (L38) + 收集方 @LoadBalanced List\<RestTemplate\> (L60-62)。

### q2 拦截链
intercept: serviceName = host (L53) + Assert (L54) + execute (L55); RequestFactory 包装 + transformers。

### q3 阻塞包装
BlockingLoadBalancerClient: choose = Mono.from(...).block() (L163); reconstructURI → LoadBalancerUriTools (L148-149)。

### q4 装配面
@Conditional(BlockingRestClassesPresentCondition) + SmartInitializingSingleton customizer 注入 (L68-77) + DeferringLoadBalancerInterceptor (4.1.2 时序解决)。

## §06 负面空间 (6 条)

不做 Ribbon 兼容 / 不做实例缓存 / 不做重试内建 / 不做异步支持 / 不做协议校验 / 不做重放保护

## §07 交叉引用

- ← SCC-3 服务发现 (实例来源) + SCC-13 NamedContextFactory
- → SCC-11 BlockingLoadBalancer 重试 + SCC-7 ReactorLoadBalancer (策略核心)
- 另见: Ribbon (历史对照) / WebClient 响应式
