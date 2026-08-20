# D-11 元数据/服务发现 — Pass 2 闭环 Q4: 服务发现与迁移收尾面

> 核心: ServiceDiscoveryRegistry + ServiceDiscovery + 迁移 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 应用级服务发现怎么工作? 与接口级 (D-5) 怎么迁移?**

## 机制链 (已实证)

```
ServiceDiscoveryRegistry (registry/client/ServiceDiscoveryRegistry.java:75, D-5 钩子兑现):
├── 注释 (L70-71): "register() aggregates interface level data into MetadataInfo by mainly
│   interacting with MetadataService; subscribe() triggers the whole subscribe process of the
│   application level service discovery model"
├── shouldRegister (L141): 仅 Provider 注册; shouldSubscribe (L156-157): 反向
├── register → serviceDiscovery.register(url) (L161-172)
└── extends FailbackRegistry (D-5 重试继承)

ServiceDiscovery 面 (registry/client/):
├── AbstractServiceDiscovery + AbstractServiceDiscoveryFactory (SPI 工厂)
├── **DefaultServiceInstance** — 应用实例模型 (appName/address/port)
├── **InstanceAddressURL** — 实例地址 URL (应用级地址封装)
└── NopServiceDiscovery (空实现, 无注册中心场景)

迁移收尾 (D-3 MigrationInvoker 呼应):
├── MigrationStep.FORCE_APPLICATION (MigrationInvoker L205/303) — 强制应用级
├── APPLICATION_FIRST (默认) — 双订阅共存
└── D-3 decideInvoker → 应用级 invoker = ServiceDiscoveryRegistry 路径
```

## 关键设计 (why)

1. **数据面瘦身**: 注册中心从"每接口每节点" → "每应用" — 大规模集群注册数据量骤降 (3.x 核心收益)
2. **元数据面分离**: 服务定义进元数据中心 (MetadataReport) + 注册中心只存实例/revision — 两中心分工
3. **迁移三态收尾**: FORCE_INTERFACE → APPLICATION_FIRST (默认) → FORCE_APPLICATION — D-3 迁移 invoker 完整闭环
4. **InstanceAddressURL**: 应用级地址封装 — 元数据解析入口

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| ServiceDiscoveryRegistry + 注释 | registry/client/ServiceDiscoveryRegistry.java:70-75 |
| shouldRegister/Subscribe | 同上 L141-157 |
| DefaultServiceInstance | registry/client/DefaultServiceInstance.java |
| InstanceAddressURL | registry/client/InstanceAddressURL.java |
| FORCE_APPLICATION (迁移) | registry/client/migration/MigrationInvoker.java:205,303 |

## 负面空间 (Q4 面)

- 不强制迁移 (APPLICATION_FIRST 共存, 用户可配置)
- 不做实例级元数据 (应用级粒度)
- 不做跨注册中心应用同步 (每注册中心独立)
