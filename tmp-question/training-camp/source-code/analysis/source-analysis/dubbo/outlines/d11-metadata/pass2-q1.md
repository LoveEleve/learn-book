# D-11 元数据/服务发现 — Pass 2 闭环 Q1: 元数据服务面 (MetadataService)

> 核心: MetadataService 接口 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 元数据服务暴露什么能力? 为什么它是"内置 RPC 服务"?**

## 机制链 (已实证)

```
MetadataService (dubbo-metadata-api):
├── 注释 (L40): "The Consumer queries the metadata information of the Provider to list the
│   interfaces and each interface's configuration" — 消费端查 provider 元数据
├── getServiceDefinition(interfaceName, version, group) (L161-162 default: buildKey 组合)
├── getMetadataInfo / getMetadataInfos — 应用级元数据 (3.x)
├── exportInstanceMetadata — 实例元数据导出
├── version() — 服务版本
└── **isMetadataService** (L195-196): 接口名 = MetadataService.class.getName() — 内置服务标识

关键认知: MetadataService 本身是一个 RPC 服务 (通过 D-2/D-3 导出/引用) —
消费者远程调用它获取 provider 的服务定义 (自描述)
```

## 关键设计 (why)

1. **服务自描述**: Provider 把自己的服务定义 (接口/方法/配置) 作为元数据暴露 — 消费者动态发现接口细节
2. **内置 RPC 服务**: MetadataService 走正常 Dubbo 调用链 (导出/引用) — 无特殊通道
3. **双模型**: 接口级 (getServiceDefinition — 2.7 迁移面) + 应用级 (getMetadataInfo — 3.x)
4. **group/version 组合键**: buildKey(interfaceName, group, version) — 服务变体区分

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| 接口注释 (消费端查询语义) | MetadataService.java:40 |
| getServiceDefinition default | MetadataService.java:161-162 |
| isMetadataService | MetadataService.java:195-196 |
| getMetadataInfo (应用级) | MetadataService.java:167 |

## 负面空间 (Q1 面)

- 不做元数据缓存 (每次远程查询)
- 不做定义版本协商 (revision 由调用方对比)
- 不做元数据鉴权 (随调用链 Filter)
