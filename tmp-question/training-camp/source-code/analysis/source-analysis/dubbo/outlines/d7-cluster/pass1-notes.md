# D-7 集群容错 — Pass 1 轮廓记录 (入口展开追踪 00 §2)

> 日期: 2026-08-16 | 源码: 3.3.7-SNAPSHOT (dubbo-cluster support/ + wrapper/)
> 09 域级审计: 执行计划 DB-7 断言 "Cluster—Failover/Failfast/Failsafe/Failback/Forking/Broadcast/Available/Mergeable" (8) + PLAN 修正 (→9, +ZoneAware) — 已穷举, 见文末审计表

## 入口展开 (Level-1~3, 已读源码)

### Level-1: Cluster SPI + AbstractCluster.join

```
Cluster @SPI(Cluster.DEFAULT) — 默认 failover (Cluster.java:34)
SPI 注册表穷举 (11 项 = 9 实现 + 2 装饰):
├── failover / failfast / failsafe / failback / forking / available / mergeable / broadcast (support/)
├── zone-aware=ZoneAwareCluster (support/registry/, 3.x)
└── mock=MockClusterWrapper + scope=ScopeClusterWrapper (wrapper 装饰, D-1 织入!)

AbstractCluster.join (support/wrapper/AbstractCluster.java:57-64):
└── buildFilterChain → buildClusterInterceptors(doJoin(directory))
    ├── ClusterFilterInvoker (L82+): 构造时 FilterChainBuilder.buildClusterInvokerChain
    │   ← D-4 三次 REVIEW 集群级 Filter 链钩子在此兑现!
    ├── buildInterceptorInvoker (L66-75): InvocationInterceptorBuilder.getActivateExtensions
    │   → InvocationInterceptorInvoker (3.x 拦截器面)
    └── CLUSTER_INTERCEPTOR_COMPATIBLE_KEY → build27xCompatibleClusterInterceptors (2.7 兼容)
doJoin (L77) 抽象 → 各 Cluster 实现: FailoverCluster.doJoin → new FailoverClusterInvoker(directory)
```

### Level-2: AbstractClusterInvoker.select — 粘滞 + 重选

```
select (support/AbstractClusterInvoker.java:155-185):
├── sticky 粘滞: CLUSTER_STICKY_KEY (方法级, DEFAULT_CLUSTER_STICKY)
│   ├── stickyInvoker 失效: invokers 不再包含 → 置空
│   └── sticky 命中且可用 → 直接返回 (同节点连续调用)
├── doSelect(loadbalance, invocation, invokers, selected) — D-6 负载均衡调用点
└── sticky 时记录 stickyInvoker
注释 (L140-141): a) 先 loadbalance 选 → selected 列表/不可用 → b) reselect (L254-328)
```

### Level-3: FailoverClusterInvoker.doInvoke — 重试核心 (L57-127)

```
FailoverClusterInvoker.doInvoke:
├── calculateInvokeTimes(methodName) — 重试次数 (L129-142)
├── 重试循环 (for i < len):
│   ├── i > 0: checkWhetherDestroyed → list(invocation) 重新拉目录 (invokers 可能变)
│   ├── invoker = select(loadbalance, invocation, copyInvokers, invoked) ← D-6 selected 钩子!
│   ├── invoked.add + RpcContext.setInvokers
│   ├── invokeWithContext(invoker, invocation) — 上下文传播
│   ├── 业务异常 e.isBiz() → 直接抛 (不重试)
│   └── 失败 → providers.add (收集失败 provider)
└── 全部失败 → RpcException "Tried N times of the providers..."
```

### Level-4: 9 策略差异速览 (穷举实证)

```
Failover: 重试 N 次 (默认 retries, 逐个换节点) ← 默认
Failfast: 一次失败立即抛 (不重试)
Failsafe: 失败吞掉, 返回空结果 (静默)
Failback: 失败异步重试 (后台任务, 同 FailbackRegistry 思路)
Forking: 并行调用 forks 个节点, 先到先得 (L72 doInvoke)
Broadcast: 广播调所有节点, 任一失败记错 (L54)
Available: 遍历找第一个可用节点 (L39)
Mergeable: 合并多节点结果 (group 场景, 如多个 provider 分片)
ZoneAware (3.x): ZoneDetector + preferred=true 最高优先 (L46 注释) — 区域亲缘
```

## 09 域级审计表 (执行计划 DB-7 断言 vs 源码)

| 断言 | grep 证据 | 结论 |
|---|---|---|
| "Failover/Failfast/Failsafe/Failback/Forking/Broadcast/Available/Mergeable" (8) | SPI 注册表 9 实现 (含 zone-aware) | 修正: 8→9 (PLAN 已记录) |
| 执行计划未提: 默认策略 | @SPI(Cluster.DEFAULT) = failover | 补锚 |
| 执行计划未提: mock/scope 装饰 | MockClusterWrapper/ScopeClusterWrapper (wrapper 织入) | 补锚 |
| 执行计划未提: 集群级 Filter 链 | ClusterFilterInvoker.buildClusterInvokerChain (D-4 钩子兑现) | 补锚 |
| 执行计划未提: 粘滞 | select sticky (CLUSTER_STICKY_KEY) | 补锚 |
| 执行计划未提: 业务异常不重试 | e.isBiz() → 直接抛 | 补锚 |

## 待展开 (下一层)

1. calculateInvokeTimes (重试次数来源 — retries 参数/幂等判断)
2. reselect (L254-328 完整逻辑 — selected > available 规则)
3. Router 链 (RegistryDirectory 预路由 → 实际路由决策 — router/ 包)
4. MockCluster (降级面)
5. invokeWithContext (上下文传播细节)
