# D-11 元数据/服务发现 — Pass 2 闭环 Q3: 应用级模型面 (MetadataInfo + ServiceNameMapping)

> 核心: MetadataInfo revision + 服务名映射 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 应用级元数据怎么组织? revision 怎么检测变更? 接口怎么映射到应用?**

## 机制链 (已实证)

```
MetadataInfo (MetadataInfo.java:58-94) — 应用级元数据聚合:
├── revision (L65, volatile) — ⚠ 变更检测版本 (上报注册中心/元数据中心)
├── services: Map<String, ServiceInfo> (L67) — 接口 → 服务信息
├── rawMetadataInfo (L71) — json 格式元数据 (与 revision 同步更新, 注释 L63/71)
└── 构造 (L91-94): appName + revision + services

RevisionResolver (RevisionResolver.java):
└── calRevision(metadata) = **MD5(metadata)** — revision 计算 (变更 → MD5 变 → 消费者对比)

ServiceNameMapping @SPI("metadata") (APPLICATION 作用域):
├── map(url) — 接口→应用映射注册
├── hasValidMetadataCenter — 元数据中心有效性
├── buildMappingKey(url) → buildGroup(serviceInterface) — 映射键
└── 实现: AbstractServiceNameMapping (dubbo-metadata-api)
```

## 关键设计 (why)

1. **revision = MD5 变更指纹**: 元数据变 → MD5 变 → 消费者/注册中心对比检测 — O(1) 变更检测
2. **应用级聚合**: 一个应用一个 MetadataInfo (多接口合并) — 注册中心数据量从"每接口"降为"每应用" (3.x 服务发现核心收益!)
3. **服务名映射**: 接口 → 应用映射 (ServiceNameMapping) — 接口级查询路由到应用
4. **revision 上报**: 注册中心只存 revision — 元数据变更不用全量推送

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| MetadataInfo revision/services/raw | MetadataInfo.java:58-94 |
| RevisionResolver MD5 | RevisionResolver.java |
| ServiceNameMapping SPI + 键 | ServiceNameMapping.java |
| AbstractServiceNameMapping | AbstractServiceNameMapping.java |

## 负面空间 (Q3 面)

- 不做增量 revision (全量 MD5)
- 不做跨应用共享元数据 (每应用独立)
- 不做映射缓存失效通知 (订阅驱动)
