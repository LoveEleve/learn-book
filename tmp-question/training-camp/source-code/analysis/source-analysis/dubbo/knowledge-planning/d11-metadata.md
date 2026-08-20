# D-11 元数据/服务发现 — 自描述与应用级迁移

> 项目: Dubbo | 🟡 Deep / 1 篇 | MetadataService+MetadataUtils+MetadataInfo+RevisionResolver+ServiceNameMapping+ServiceDiscoveryRegistry
> 基线: DUBBO-PLAN D-11 (服务发现升级, 77 文件) — 前置: **D-2 (publishServiceDefinition 挂钩) + D-5 (ServiceDiscoveryRegistry 钩子)** — 展开 自描述→发布→应用级模型→发现迁移

---

## §0.8

- 🟡 Deep，1篇 — 自描述(**MetadataService 注释 L40[消费端查 provider 元数据]: getServiceDefinition L161-162[buildKey group/version]+getMetadataInfo[应用级]+exportInstanceMetadata+isMetadataService L195-196[内置 RPC 服务走正常调用链]**) → 发布(**MetadataUtils.publishServiceDefinition L77+[registry/client/metadata/: 无 report 跳过 L79-83+PROVIDER_SIDE→FullServiceDefinition→MetadataReport.storeProviderMetadata L93-97[shouldReportDefinition 可禁用]]; D-2 export/D-3 createProxy 双端挂钩**) → 应用级模型(**MetadataInfo L58-94: revision L65[MD5 变更指纹 RevisionResolver]+services Map+rawMetadataInfo[json]; ServiceInfo 内部类 L495+[name/group/version/protocol/path/params+协议服务键 {group}/{iface}:{version}:{protocol} L242]; ServiceNameMapping @SPI("metadata")[map(url) 接口↔应用映射+buildMappingKey]**) → 发现迁移(**ServiceDiscoveryRegistry L75: 注释 L70-71[register 聚合到 MetadataInfo/subscribe 应用级流程]+shouldRegister L141[仅 Provider]+serviceDiscovery.register; ServiceDiscovery 面[AbstractServiceDiscovery+DefaultServiceInstance[instanceAddressURL 缓存 L77]+InstanceAddressURL]; 应用级目录 ServiceDiscoveryRegistryDirectory L91[getMatchedServiceInfos L532+ServiceInfo 变更检测 L574-580]; InterfaceCompatibleRegistryProtocol L35 双注册模式[迁移期]; MigrationInvoker 三态收尾 FORCE_APPLICATION L205/303**)
- 设计模式: [模式: 自描述+两中心分工+revision 指纹+迁移策略]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| MetadataService.java:40 | 语义 | **消费端查询 provider 元数据** (注释) | High |
| MetadataService.java:195-196 | 内置 | **isMetadataService — 内置 RPC 服务** | High |
| MetadataUtils.java:77-97 | 发布 | **publishServiceDefinition: 无 report 跳过 + PROVIDER→FullServiceDefinition→MetadataReport** | High |
| MetadataInfo.java:58-94 | 模型 | **revision (L65) + services Map + rawMetadataInfo (json)** | High |
| MetadataInfo.java:495+ | ServiceInfo | **name/group/version/protocol/path/params + 协议服务键** (L242 注释) | High |
| RevisionResolver.java | 指纹 | **calRevision = MD5(metadata)** — 变更检测 | High |
| ServiceNameMapping.java | 映射 | **@SPI("metadata") map(url) — 接口↔应用** | High |
| ServiceDiscoveryRegistry.java:70-75 | 发现 | **register 聚合到 MetadataInfo / subscribe 应用级流程** (注释) | High |
| ServiceDiscoveryRegistryDirectory.java:91,532,574 | 目录 | **应用级动态目录: getMatchedServiceInfos + ServiceInfo 变更检测** | High |
| InterfaceCompatibleRegistryProtocol.java:35 | 双注册 | **迁移期接口级+应用级共存** | High |
| MigrationInvoker.java:205,303 | 迁移 | **FORCE_APPLICATION 三态收尾 (D-3 呼应)** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 元数据单机制 (自描述+发布+模型+发现) — 1篇按四段展开; D-2/D-3/D-5 挂钩全部兑现 (全书收尾)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 服务自描述 (内置 RPC) | 🔴 | **为什么🔴**: 元数据基础 |
| P1-2 | 双端发布 (export/refer) | 🔴 | **为什么🔴**: 完整视图 |
| P1-3 | revision MD5 变更检测 | 🔴 | **为什么🔴**: O(1) 检测 |
| P1-4 | 应用级聚合 (注册数据瘦身) | 🔴 | **为什么🔴**: 3.x 核心收益 |
| P2-1 | 服务名映射 (接口↔应用) | 🟡 | **为什么🟡**: 路由面 |
| P2-2 | 应用级目录 + ServiceInfo 变更检测 | 🟡 | **为什么🟡**: 目录面 |
| P2-3 | 迁移三态 + 双注册模式 | 🟡 | **为什么🟡**: 迁移面 |
| P3-1 | InstanceAddressURL 缓存 | 🟢 | **为什么🟢**: 细节 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **自描述+发布** | 🔴 | 主线 |
| B | **应用级模型** | 🔴 | 核心 |
| C | **服务发现** | 🔴 | 应用面 |
| D | **迁移面** | 🟡 | 过渡 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 元数据服务 | MetadataService 是**内置 RPC 服务** (isMetadataService) 走正常调用链; 双模型: 接口级 getServiceDefinition (2.7) + 应用级 getMetadataInfo (3.x) | MetadataService.java:40,195-196 |
| q2 | 发布面 | publishServiceDefinition 双端挂钩 (D-2 export + D-3 createProxy); 无元数据中心可跳过 (shouldReportDefinition); FullServiceDefinition 全量 | MetadataUtils.java:77-97 |
| q3 | 应用级模型 | **MetadataInfo: revision = MD5(元数据) 变更指纹** (消费者/注册中心 O(1) 对比); 应用级聚合 = 每应用一个 MetadataInfo → 注册数据从"每接口"降为"每应用" | MetadataInfo.java:58-94; RevisionResolver.java |
| q4 | 发现迁移 | ServiceDiscoveryRegistry (D-5 钩子兑现): 应用级注册/订阅; 两中心分工 (定义→元数据中心 + 实例→注册中心); **迁移三态 FORCE_INTERFACE→APPLICATION_FIRST (默认共存)→FORCE_APPLICATION — D-3 完整闭环**; 双注册模式 InterfaceCompatibleRegistryProtocol 迁移期兼容 | ServiceDiscoveryRegistry.java:70-75; InterfaceCompatibleRegistryProtocol.java:35; MigrationInvoker.java:205,303 |

→ **全书收尾**: 11 域闭环 — SPI 微内核 → 服务生命周期 → 调用链 → 治理 → 网络 → 协议 → 序列化 → 元数据; 对照 Nacos 服务发现/gRPC。
