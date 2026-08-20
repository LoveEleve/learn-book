# D-7 集群容错 — Pass 2 闭环 Q1: 集群装配面 (Cluster SPI + join 双层包装)

> 核心: AbstractCluster.join | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 集群容错怎么装配? join 时发生了什么? 集群级过滤器/拦截器在哪织入?**

## 机制链 (已实证)

```
Cluster @SPI(Cluster.DEFAULT) — 默认 failover (Cluster.java:34)
SPI 注册表 (11 项 = 9 实现 + 2 装饰):
├── support/: failover / failfast / failsafe / failback / forking / available / mergeable / broadcast
├── support/registry/: zone-aware=ZoneAwareCluster (3.x)
└── support/wrapper/: mock=MockClusterWrapper + scope=ScopeClusterWrapper (D-1 Wrapper 织入)

AbstractCluster.join (support/wrapper/AbstractCluster.java:57-64):
└── buildFilterChain=true → buildClusterInterceptors(doJoin(directory))
    ├── 1) ClusterFilterInvoker (L82-108): 构造时遍历 FilterChainBuilder
    │   → builder.buildClusterInvokerChain(tmpInvoker, REFERENCE_FILTER_KEY, CONSUMER)
    │   ← D-4 三次 REVIEW 发现的集群级 Filter 链在此兑现!
    ├── 2) buildInterceptorInvoker (L66-75): InvocationInterceptorBuilder.getActivateExtensions
    │   → new InvocationInterceptorInvoker (3.x invocation 拦截器面)
    └── 3) CLUSTER_INTERCEPTOR_COMPATIBLE_KEY=true → build27xCompatibleClusterInterceptors (2.7 兼容)
doJoin (L77) 抽象 → FailoverCluster.doJoin → new FailoverClusterInvoker(directory)
```

## 关键设计 (why)

1. **join = 三层包装**: doJoin (容错策略) → ClusterFilterInvoker (集群 Filter 链) → InvocationInterceptorInvoker (3.x 拦截器) — 容错与横切面分离
2. **ClusterFilterInvoker 在构造时织链**: 与 ProtocolFilterWrapper (协议级) 双链层次完整闭环 (D-4 双链发现)
3. **Wrapper 装饰 (mock/scope)**: MockClusterWrapper/ScopeClusterWrapper 是 D-1 Wrapper 织入的实例 — SPI 实现也可以被包装
4. **2.7 兼容开关**: CLUSTER_INTERCEPTOR_COMPATIBLE_KEY 控制新旧拦截器行为 — 迁移面

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| @SPI 默认 failover | Cluster.java:34 |
| SPI 注册表 11 项 | dubbo-cluster resources META-INF/dubbo/internal/org.apache.dubbo.rpc.cluster.Cluster |
| join + buildClusterInterceptors | AbstractCluster.java:45-64 |
| ClusterFilterInvoker (集群 Filter 链) | AbstractCluster.java:82-108 |
| buildInterceptorInvoker (3.x 拦截器) | AbstractCluster.java:66-75 |
| MockClusterWrapper | support/wrapper/MockClusterWrapper.java:28-38 |

## 负面空间 (Q1 面)

- 不动态换策略 (join 时定死, 换需重建引用)
- 不做多策略组合 (单一 Cluster SPI, 组合用 wrapper)
- 不自动选策略 (CLUSTER_KEY 参数显式指定)
