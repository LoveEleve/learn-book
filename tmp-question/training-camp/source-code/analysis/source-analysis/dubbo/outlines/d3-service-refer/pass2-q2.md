# D-3 服务引用 — Pass 2 闭环 Q2: 注册中心引用面 (RegistryProtocol)

> 入口: protocolSPI.refer → RegistryProtocol.refer (dubbo-registry-api) | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 引用如何"通过注册中心"拿到 provider? 3.x 的接口级→应用级服务发现迁移是什么?**

## 机制链 (已实证)

```
RegistryProtocol.refer(type, url)            RegistryProtocol.java:555-576
├── getRegistryUrl (URL 规范化) → getRegistry (Registry SPI 自适应)
├── RegistryService 自身 → proxyFactory.getInvoker (注册中心自举)
├── group="a,b"/"*" → MERGEABLE_CLUSTER       ← 分组聚合引用 (多组服务合并)
└── 默认 → Cluster.getCluster(CLUSTER_KEY)
    └── doRefer(cluster, registry, type, url, qs)   L578-595
        ├── consumerUrl 构建 (CONSUMER 协议 + CONSUMER_URL_KEY 属性)
        ├── getMigrationInvoker (L601-609) → ServiceDiscoveryMigrationInvoker
        │   ← 3.x 服务发现迁移核心 (registry/client/migration/MigrationInvoker.java:81-115)
        │   ├── 持有双 invoker: 接口级 (interface registry) + 应用级 (application discovery)
        │   ├── decideInvoker (L314-323): 按迁移规则选 invoker
        │   ├── 迁移规则: FORCE_INTERFACE / FORCE_APPLICATION / APPLICATION_FIRST
        │   └── refreshInterfaceInvoker (L472-497): 规则变化时切换
        └── interceptInvoker (L623-640): **RegistryProtocolListener.onRefer 回调** (监听器集合, 非拦截器链!)
```

## 关键设计 (why)

1. **group 合并集群**: 多分组订阅时用 MergeableCluster 聚合 — 分组路由面
2. **MigrationInvoker = 3.x 迁移面**: 接口级注册 (RegistryDirectory + subscribe) 是 2.x 方式; 应用级服务发现 (ServiceDiscoveryRegistry) 是 3.x 新方式 — 迁移 invoker 让两者共存平滑切换 (FORCE_INTERFACE→APPLICATION_FIRST→FORCE_APPLICATION)
3. **interceptInvoker = RegistryProtocolListener 集合**: 注册中心面的监听器扩展点 (onRefer 回调, 如迁移规则监听) — 非拦截器链, 是 SPI 监听器模式
4. **默认迁移规则 = APPLICATION_FIRST**: 配置中心未配置 → INIT 规则 (RegistryConstants.INIT="INIT"), getStep 解析兜底 "initial step: APPLICATION_FIRST" (MigrationRule.java:167) — 双订阅共存是 3.x 默认
5. **consumerUrl 独立构建**: 消费侧也有自己的 URL 模型 (CONSUMER 协议) — 与 provider 的 PROVIDER URL 对称

## 锚点清单

| 锚点 | 位置 |
|:--|:--|
| refer 入口 + group 合并分支 | RegistryProtocol.java:555-576 |
| doRefer: consumerUrl + MigrationInvoker + intercept | RegistryProtocol.java:578-595 |
| getMigrationInvoker → ServiceDiscoveryMigrationInvoker | RegistryProtocol.java:601-609 |
| MigrationInvoker 双 invoker + decideInvoker | registry/client/migration/MigrationInvoker.java:81-115, 314-323 |
| refreshInterfaceInvoker (规则切换) | MigrationInvoker.java:472-497 |
| interceptInvoker: RegistryProtocolListener.onRefer | RegistryProtocol.java:623-640 |
| INIT 规则默认兜底 → APPLICATION_FIRST | MigrationRuleListener.java:261; MigrationRule.java:167 |
| 接口级兼容 RegistryProtocol 族 | InterfaceCompatibleRegistryProtocol.java:69-79 |

## 负面空间 (Q2 面)

- 不做多注册中心同时强一致 (refer 循环各自 refer, 不做可用性检查 — 可后变为可用)
- 不做迁移规则自动推荐 (默认 APPLICATION_FIRST 需用户/规则中心配置)
- 不做 group 通配符替换 (group="*" 走合并集群, 非逐组订阅)

## 域归属说明

- RegistryDirectory subscribe/notify/路由 (RegistryDirectory.java:129-237) 属 **D-5 注册中心** 深入面 — 本域只到 MigrationInvoker 黑盒
- Cluster.join/ClusterInvoker 装饰链属 **D-7 集群容错** 深入面 — 本域到 Cluster 黑盒
- 3.x 应用级服务发现 (ServiceDiscoveryRegistry) 属 **D-11 元数据/服务发现** — 本域埋钩子
