# D-11 元数据/服务发现 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.x | 接口级注册: 每接口每节点写注册中心 (数据量大); 无元数据中心 |
| 2.7.x | **MetadataService + 元数据中心引入**: publishServiceDefinition (服务定义上报) — 迁移预备 |
| 3.0 | **应用级服务发现**: ServiceDiscoveryRegistry + MetadataInfo (应用聚合) + revision 变更检测 + ServiceNameMapping (接口↔应用) + InstanceAddressURL |
| 3.x | MigrationInvoker 迁移三态 (D-3); 双注册中心模式 (接口级+应用级共存); metadata-report nacos/zookeeper |
| 3.3.x | dubbo-metadata 77 文件稳定 (registry→metadata 22 import) |

## 痕迹证据

- MetadataService.java:40: "The Consumer queries the metadata information of the Provider to list the interfaces and each interface's configuration" (2.7+ 注释锚)
- ServiceDiscoveryRegistry.java:70-71: "register() aggregates interface level data into MetadataInfo... subscribe() triggers the whole subscribe process of the application level service discovery model" (3.0 锚)
- MetadataInfo.java:58-94: revision (L65) + rawMetadataInfo (L71) (3.0 锚)
- RevisionResolver.java: calRevision = MD5 (3.0 锚)
- ServiceNameMapping.java: @SPI("metadata") APPLICATION 作用域 (3.0 锚)
- MetadataUtils.java:79-83: 无 report 跳过注释 (2.7+ 锚)

## 推断标注

- "2.x 无元数据中心" — 公知版本线 (标注)
- "2.7 元数据引入" — MetadataService 注释锚 (实证)
- "3.0 应用级" — ServiceDiscoveryRegistry 注释实证 (实证)
- **源码 grafted (浅克隆, 单 commit)**: git log 时空考古受限 — 以注释锚 + 类结构为主 (降级说明)

## 对照线 (已交付/待交付)

- Nacos (NC-1/NC-2): 服务注册/发现 vs Dubbo ServiceDiscoveryRegistry — 双视角对照
- Eureka: 纯 AP 应用级注册中心 vs Dubbo 应用级 — 认知对照
- ZK (4.3): 数据树 vs 两中心分工 — 存储对照
