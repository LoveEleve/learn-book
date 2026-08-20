# SCC-7 ReactorLoadBalancer 策略 — 知识规划 (KP)

> 域: SCC-7 | 级别: 🔴 | 方案: A | 大纲: outlines/scc7-reactor-lb/outline.md (6 节)

## §01 域定位

ReactorLoadBalancer = 响应式负载均衡策略核心。ReactorLoadBalancer 接口 (choose → Mono\<Response\>) + RoundRobin (AtomicInteger + & MAX_VALUE 循环 + 随机种子) + Random (ThreadLocalRandom) + LoadBalancerClientFactory 每服务隔离。

## §02 源文件清单

| 文件 | 行数 | 职责 | 归属节 |
|:--|:--:|:--|:--:|
| core/ReactorLoadBalancer.java | 45 | 策略接口 | 1 |
| core/RoundRobinLoadBalancer.java | 121 | 轮询策略 + 三态 | 2,3,4 |
| core/RandomLoadBalancer.java | 93 | 随机策略 | 5 |
| core/ReactorServiceInstanceLoadBalancer.java | — | 具体类型标记 | 1 |
| support/LoadBalancerClientFactory.java | — | 每服务工厂 (SCC-13) | 6 |
| core/SelectedInstanceCallback.java | — | 选中回调 (SCC-6) | 3 |

## §05 闭环要点 (Pass 2 内化)

### q1 接口面
choose(Request) → Mono\<Response\> (L39) + choose() default (L41-43, REQUEST)。

### q2 轮询数学
AtomicInteger position (L47) + seedPosition 随机 (L60) + incrementAndGet & MAX_VALUE (L114) + 取模 (L116)。

### q3 三态
空列表 EmptyResponse+warn (L100-103) / 单实例不转 (L106-109) / 多实例取模 (L113-116); SelectedInstanceCallback 回调 (L66-68)。

### q4 每服务隔离
LoadBalancerClientFactory extends NamedContextFactory (L46) + getInstance(serviceId) (L79-80)。

## §06 负面空间 (6 条)

不做加权轮询内建 / 不做粘性会话 / 不做重试 / 不做实例排序 / 不做健康检查 / 不做多策略切换

## §07 交叉引用

- ← SCC-6 Supplier (实例列表) + SCC-13 NamedContextFactory
- → SCC-11 BlockingLoadBalancer (阻塞消费) + SCC-12 扩展策略
- 另见: Netflix ocelli / Ribbon RoundRobinRule
