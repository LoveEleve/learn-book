# D-4 RPC 调用 — Pass 2 闭环 Q2: Filter 链构建与消费端实例族

> 核心: DefaultFilterChainBuilder.buildInvokerChain (dubbo-cluster!) | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 消费端过滤器链怎么构建? @Activate 怎么筛选过滤器? 有哪些消费端过滤器实例?**

## 机制链 (已实证)

```
ProtocolFilterWrapper.refer (cluster/filter/ProtocolFilterWrapper.java:66-73)  ← 3.x 在 dubbo-cluster!
├── registry URL → 直接透传 protocol.refer (注册中心引用不过滤器)
└── 非 registry → buildInvokerChain(protocol.refer(url), REFERENCE_FILTER_KEY, CONSUMER)

DefaultFilterChainBuilder.buildInvokerChain (L43-77)
├── getActivateExtension(url, key=reference.filter, group=CONSUMER)  ← D-1 激活面消费!
│   ├── @Activate 注解: group=CONSUMER 才入选 + value 条件对 (URL 参数匹配, OR 语义)
│   └── 多 ModuleModel → 合并 + sortingAndDeduplication (去重保序)
├── 倒序包装: for (i = filters.size()-1 → 0) last = new CopyOfFilterChainNode(original, next, filter)
└── CallbackRegistrationInvoker 收尾 (filter 回调注册)
    → 责任链: FilterN → ... → Filter1 → 协议 invoker (DubboInvoker)

⚠ 3.x 双链层次 (二次深审发现):
├── 普通链: buildInvokerChain (协议级, ProtocolFilterWrapper.refer 织)
└── **集群链: buildClusterInvokerChain (FilterChainBuilder.java:53) — AbstractCluster L92 join 时织入**
    ├── ClusterFilterChainNode (L162) / CopyOfClusterFilterChainNode (L394, 注释 "replace ... when proved stable")
    └── 集群 invoker 上的过滤器 (如 adaptiveLoadBalance 面)

消费端 Filter 实例族 (18 个, META-INF/dubbo/internal/org.apache.dubbo.rpc.Filter 实证)
echo / generic / genericimpl / token / accesslog / classloader / classloader-callback
context / exception / executelimit / deprecated / compatible / timeout / tps
profiler-server / adaptiveLoadBalance / active-limit / rpc-exception
```

## 关键设计 (why)

1. **ProtocolFilterWrapper = Protocol 的 Wrapper (D-1 Wrapper 织入!)**: 协议 SPI 的 wrapper 在 refer/export 时织入 Filter 链 — 这是 D-1 Wrapper 机制在调用链的具体实例
2. **registry URL 透传**: 注册中心引用 (RegistryProtocol) 不需要消费过滤器 — 只对实际协议调用 (dubbo/triple/injvm) 织链
3. **CONSUMER group 参数**: 同一批 @Activate 过滤器按 group 分流 — 消费端/服务端链不同 (服务端 PROVIDER group, D-2 面)
4. **倒序包装 = 责任链**: 最后一个过滤器先执行 (LIFO), 外层 filter 包内层 — 经典装饰器链
5. **过滤=配置**: 过滤器激活由 URL 参数驱动 (D-1 URL 总线) — 动态可配 (如 timeout.enabled)

## 锚点清单

| 锚点 | 位置 |
|:--|:--|
| ProtocolFilterWrapper.refer (registry 透传 + CONSUMER) | dubbo-cluster cluster/filter/ProtocolFilterWrapper.java:66-73 |
| ProtocolFilterWrapper.export (服务端面) | 同上 L52-59 |
| buildInvokerChain (getActivateExtension + 倒序包装) | DefaultFilterChainBuilder.java:43-77 |
| 消费端 Filter 注册表 (18 实例) | dubbo-rpc-api resources META-INF/dubbo/internal/org.apache.dubbo.rpc.Filter |

## 负面空间 (Q2 面)

- 不做过滤器热加载 (链在 refer 时一次性构建)
- 不做消费端/服务端混合链 (group 严格分流)
- 不做注册中心引用织链 (registry URL 透传)
