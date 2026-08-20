# D-3 服务引用 — Pass 2 闭环 Q3: 集群接入面 (Cluster.join 三分支)

> 入口: createInvoker (ReferenceConfig.java:668-714) | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: refer 出来的 invoker 怎么接入集群容错? 单 URL/多注册中心/直连三种场景包装差异是什么?**

## 机制链 (已实证)

```
createInvoker()                              ReferenceConfig.java:668-714
├── urls.size()==1 (单 URL):
│   ├── invoker = protocolSPI.refer(interfaceClass, url)   ← Protocol 自适应 (D-1)
│   └── 非 registry URL && !unloadClusterRelated:
│       invoker = Cluster.DEFAULT.join(new StaticDirectory(url, invokers), true)
├── 多 URL (多注册中心):
│   ├── 循环 protocolSPI.refer (各自 refer, 不做可用性检查)
│   ├── registryUrl != null:
│   │   cluster = CLUSTER_KEY 参数 (默认 ZoneAwareCluster.NAME)
│   │   invoker = Cluster.getCluster(cluster, false).join(new StaticDirectory(registryUrl, invokers), false)
│   │   ← 多订阅默认 zone-aware 策略 (3.x 面!)
│   └── 无 registry → 直连:
│       cluster = CLUSTER_KEY (默认 Cluster.DEFAULT)
│       invoker = Cluster.getCluster(cluster).join(new StaticDirectory(curUrl, invokers), true)
└── 源码注释包装序 (L692-693):
    ZoneAwareClusterInvoker(StaticDirectory) → FailoverClusterInvoker(RegistryDirectory, routing 在此) → Invoker

Cluster.join(directory, buildFilterChain)      Cluster.java:48
└── AbstractCluster (support/wrapper/AbstractCluster.java) → 构建 ClusterInvoker + Filter 链包装 (buildFilterChain 标志)
StaticDirectory(URL, List<Invoker>, RouterChain)  StaticDirectory.java:57-69
├── setInvokers(new BitList<>(invokers))  — 静态 invoker 列表 (非订阅)
└── notify (L113-120): 静态目录的 invoker 直接替换 (不来自注册中心订阅)
```

## 关键设计 (why)

1. **StaticDirectory 静态目录**: 引用侧的 invoker 列表是"静态的" (来自 createInvoker 收集), 与 D-5 的 RegistryDirectory (动态订阅) 形成对照 — 目录模式两种: 静态/动态
2. **buildFilterChain 参数**: join 时是否构建 Filter 链 — false (多注册中心场景) 避免重复包装 (每个子 invoker 已带链)
3. **ZoneAwareCluster 默认 (多注册中心)**: 3.x 多注册中心默认 zone-aware 路由 — 同 zone 优先 (区域亲缘)
4. **Cluster SPI 注入点**: 引用面通过 CLUSTER_KEY URL 参数选集群策略 — D-1 URL 总线驱动在引用面的体现

## 锚点清单

| 锚点 | 位置 |
|:--|:--|
| createInvoker 三分支 | ReferenceConfig.java:668-714 |
| Cluster.join 接口 | dubbo-cluster Cluster.java:48 |
| AbstractCluster 实现 | dubbo-cluster support/wrapper/AbstractCluster.java |
| StaticDirectory 静态目录 | dubbo-cluster directory/StaticDirectory.java:57-69, 113-120 |
| 包装序注释 (ZoneAware→Failover) | ReferenceConfig.java:692-693 |

## 负面空间 (Q3 面)

- 不做逐注册中心可用性检查 (多注册场景, 后变可用也接受)
- 不做动态目录 (本域 StaticDirectory; 动态订阅属 D-5 RegistryDirectory)
- 不展开 Cluster 容错策略细节 (D-7 深入: Failover/Failsafe/...)
