# D-11 元数据/服务发现 — Pass 2 闭环 Q2: 发布面 (publishServiceDefinition)

> 核心: MetadataUtils (D-2/D-3 挂钩兑现) | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 服务定义什么时候发布? 发到哪? 不发会怎样?**

## 机制链 (已实证)

```
MetadataUtils.publishServiceDefinition (registry/client/metadata/MetadataUtils.java:77+):
├── getMetadataReports 空 → 跳过 (L79-83): "Remote Metadata Report Server is not provided or
│   unavailable, will stop registering service definition to remote center"
├── PROVIDER_SIDE → serviceKey → FullServiceDefinition (L87-92)
│   ├── serviceDefinition.setParameters(url.getParameters()) (L92)
│   └── 循环 MetadataReport → storeProviderMetadata (L93-97)
│       └── shouldReportDefinition 开关 (L96) — 可禁用
└── CONSUMER_SIDE → 消费侧也发布 (D-3 createProxy 实证)

MetadataReport (report/MetadataReport.java):
├── storeProviderMetadata(MetadataIdentifier, ServiceDefinition) (L37)
└── shouldReportDefinition() (L95)

挂钩点 (跨域实证):
├── D-2: ServiceConfig export 时发布
└── D-3: ReferenceConfig createProxy 时发布 (消费侧)
```

## 关键设计 (why)

1. **发布 = 服务自描述上送**: 定义 → 元数据中心 → 消费者可查 (服务发现的前提)
2. **双端发布**: Provider (服务定义) + Consumer (消费元数据) — 完整视图
3. **可禁用**: shouldReportDefinition — 无元数据中心可降级 (直连模式)
4. **FullServiceDefinition**: 接口/方法/参数/URL 全量 — 迁移面 (2.7 接口级)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| publishServiceDefinition 流程 | registry/client/metadata/MetadataUtils.java:77-97 |
| 无 report 跳过注释 | MetadataUtils.java:79-83 |
| MetadataReport 接口 | report/MetadataReport.java:33-95 |
| D-2 挂钩 (export) | ServiceConfig.java (D-2 实证) |
| D-3 挂钩 (createProxy) | ReferenceConfig.java:517 (D-3 实证) |

## 负面空间 (Q2 面)

- 不强制元数据中心 (可跳过)
- 不增量发布 (全量 FullServiceDefinition)
- 不发布失败重试 (随 FailbackRegistry 语义)
