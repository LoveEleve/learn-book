# D-11 元数据/服务发现 — 自描述→发布→应用级模型→迁移收尾

> 前置: [[D-2-服务导出]] [[D-5-注册中心]] | 引出: 无 (全书收尾) | 对照: Nacos 服务发现 (NC) + Eureka 认知
> 🟡 B | 8 KP | [模式: 自描述 + 两中心分工 + 迁移]
> Pass 2 闭环: q1(元数据服务) q2(发布面) q3(应用级模型) q4(发现+迁移)

**读者处境**: 3.x 应用级服务发现是什么? 为什么注册中心数据量骤降? 与 2.x 接口级怎么迁移? 这篇拆 MetadataService + publishServiceDefinition + MetadataInfo + ServiceDiscoveryRegistry。

### 1. 元数据服务面 — MetadataService 自描述

场景: 服务怎么描述自己?
源码路径:
- **MetadataService 接口** (注释 L40: 消费端查询 provider 元数据): **getServiceDefinition** (L161-162) / getMetadataInfo (应用级) / exportInstanceMetadata
- **isMetadataService** (L195-196) — 内置服务标识: MetadataService 本身是 RPC 服务 (走 D-2/D-3 导出/引用)
关键设计 (q1): **服务自描述 + 内置 RPC 服务 + 双模型 (接口级/应用级)**。[模式: 自描述面]

### 2. 发布面 — publishServiceDefinition (D-2/D-3 挂钩兑现)

场景: 服务定义什么时候发?
源码路径:
- **MetadataUtils.publishServiceDefinition** (registry/client/metadata/:77+): 无 report 跳过 (L79-83) → PROVIDER_SIDE → FullServiceDefinition → **MetadataReport.storeProviderMetadata** (L93-97, shouldReportDefinition 可禁用)
- 挂钩: **D-2 export + D-3 createProxy 双端发布** (跨域实证)
关键设计 (q2): **双端发布 + 可禁用降级 + 全量定义**。[模式: 发布面]

### 3. 应用级模型面 — MetadataInfo + ServiceNameMapping

场景: 应用级元数据怎么组织?
源码路径:
- **MetadataInfo** (L58-94): **revision (L65)** + services Map + rawMetadataInfo (json); ⚠ **ServiceInfo 内部类** (L495+: name/group/version/protocol/path/params + consumerParams/methodParams) — 协议服务键定位 ({group}/{interface}:{version}:{protocol}, L242 注释)
- **RevisionResolver.calRevision = MD5(metadata)** — 变更指纹
- **ServiceNameMapping @SPI("metadata")**: map(url) 接口→应用映射 + buildMappingKey
关键设计 (q3): **revision MD5 变更检测 + 应用级聚合 (注册数据骤降) + 接口↔应用映射**。[模式: 模型面]

### 4. 服务发现与迁移收尾面 — ServiceDiscoveryRegistry

场景: 应用级发现怎么工作? 迁移?
源码路径:
- **ServiceDiscoveryRegistry** (L75, D-5 钩子): 注释 L70-71 register 聚合到 MetadataInfo / subscribe 应用级流程; shouldRegister (L141 仅 Provider) → **serviceDiscovery.register**
- **ServiceDiscovery 面**: AbstractServiceDiscovery + **DefaultServiceInstance** (serviceName/host/port/metadata + **instanceAddressURL 缓存** L77) + **InstanceAddressURL** (携带 MetadataInfo)
- ⚠ **双注册模式 (迁移期)**: **InterfaceCompatibleRegistryProtocol extends RegistryProtocol** (L35) — 接口级+应用级同时注册 (3.x 迁移期兼容面, D-3 MigrationInvoker 的底层)
- ⚠ **应用级动态目录**: **ServiceDiscoveryRegistryDirectory extends DynamicDirectory** (L91) — **instanceAddressURL.getMetadataInfo().getMatchedServiceInfos(protocolServiceKey)** (L532) 匹配服务 + **ServiceInfo 变更检测** (L574-580: old vs new equals — 增量更新判断; ServiceInfo 是 MetadataInfo 内部类)
- **迁移收尾** (D-3 呼应): FORCE_APPLICATION (L205/303) / APPLICATION_FIRST 默认共存
关键设计 (q4): **两中心分工 (定义→元数据中心 + 实例→注册中心) + 数据瘦身 + 迁移三态闭环**。[模式: 发现面]

## 代码类型
Architecture (自描述 + 两中心) + Concurrency (revision 并发更新)

## 负面空间 (D-11, 6 条)

| 不做 | 说明 |
|:--|:--|
| 不元数据缓存 | 每次远程查询 (q1) |
| 不强制元数据中心 | 可跳过降级 (q2) |
| 不增量发布 | 全量 FullServiceDefinition (q2) |
| 不增量 revision | 全量 MD5 (q3) |
| 不强制迁移 | APPLICATION_FIRST 共存 (q4) |
| 不跨注册中心应用同步 | 每注册中心独立 (q4) |

## 结尾桥 OUTBOUND — 全书收尾

- → **Dubbo 阶段收尾**: 11 域闭环 — SPI 微内核 (D-1) → 服务生命周期 (D-2/D-3) → 调用链 (D-4) → 治理 (D-5/D-6/D-7) → 网络 (D-8a/D-8b) → 协议 (D-9) → 序列化 (D-10) → 元数据 (D-11) — **从"框架如何装配"到"大规模服务发现"的完整认知**
- → 阶段 5.3 gRPC (G-1~G-3): 协议面对照
- → 阶段 5.9 Sentinel: 治理面对照
