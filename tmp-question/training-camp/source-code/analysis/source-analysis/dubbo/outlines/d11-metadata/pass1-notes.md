# D-11 元数据/服务发现 — Pass 1 轮廓记录 (入口展开追踪 00 §2)

> 日期: 2026-08-16 | 源码: 3.3.7-SNAPSHOT (dubbo-metadata 77 文件)
> 09 域级审计: 执行计划未覆盖 (77 文件) — PLAN 已记录; registry→metadata 22 import 实证 (PLAN §五)

## 入口展开 (Level-1~3, 已读源码)

### Level-1: MetadataService 接口 (服务自描述)

```
MetadataService (dubbo-metadata-api, 注释 L40: "The Consumer queries the metadata information of the
Provider to list the interfaces and each interface's configuration"):
├── getServiceDefinition(interfaceName, version, group) (L161-162 default: buildKey)
├── getMetadataInfo / getMetadataInfos — 应用级元数据
├── exportInstanceMetadata — 实例元数据导出
└── isMetadataService (L195-196) — 内置服务标识
```

### Level-2: publishServiceDefinition (D-2/D-3 挂钩兑现!)

```
MetadataUtils (dubbo-registry-api registry/client/metadata/MetadataUtils.java:77+):
├── 无 MetadataReport → 跳过 (L79-83 注释 "will stop registering service definition")
├── PROVIDER_SIDE → FullServiceDefinition (serviceKey + URL 参数) (L87-92)
└── → MetadataReport 推送 (L93-97, shouldReportDefinition 开关)
D-2 export / D-3 createProxy 调用点 (引用侧也发布 — 消费侧元数据)
```

### Level-3: 应用级模型 + 服务发现

```
MetadataInfo (MetadataInfo.java:58-94) — 应用级元数据聚合:
├── revision (L65, volatile) — ⚠ 变更检测版本 (上报注册中心/元数据中心)
├── services: Map<String, ServiceInfo> (L67)
├── rawMetadataInfo (L71) — json 元数据 (revision 同步更新)
└── appName + 构造 (L91-94)

ServiceNameMapping @SPI("metadata") (APPLICATION 作用域):
├── map(url) — 接口→应用映射注册
├── hasValidMetadataCenter (L)
└── buildMappingKey/buildGroup (L) — 映射键 (接口分组)

ServiceDiscoveryRegistry (D-5 钩子兑现, registry/client/):
├── 注释 (L70-71): "register() aggregates interface level data into MetadataInfo by mainly
│   interacting with MetadataService; subscribe() triggers the whole subscribe process of the
│   application level service discovery model"
├── shouldRegister (L141: 仅 Provider) / shouldSubscribe (L156-157)
└── register → serviceDiscovery.register (L161-172)
```

## 09 域级审计表 (执行计划未覆盖 vs 源码)

| 断言 | grep 证据 | 结论 |
|---|---|---|
| 执行计划未覆盖 | metadata 77 文件: api 48 + processor 23 + report 6 | 接受 (PLAN 已记录, D-11) |
| registry→metadata 22 import | PLAN §五 实证 | 接受 |
| D-2/D-3 挂钩兑现 | publishServiceDefinition (MetadataUtils L77) — 导出/引用双端 | 接受 |
| 执行计划未提: revision 变更检测 | MetadataInfo.revision (L65) | 补锚 |
| 执行计划未提: 服务名映射 | ServiceNameMapping SPI (接口↔应用) | 补锚 |
| 执行计划未提: 内置元数据服务 | isMetadataService (L195) — 元数据作为内置 RPC 服务 | 补锚 |

## 待展开 (下一层)

1. MetadataReport (report/ 15 文件 — 元数据上报抽象 + nacos/zookeeper 实现)
2. ServiceDiscovery 面 (registry/client — ServiceInstance/实例注册)
3. D-3 MigrationInvoker 迁移收尾 (FORCE_APPLICATION 路径)
4. FullServiceDefinition (服务定义模型)
5. revision 计算 (RevisionResolver)
