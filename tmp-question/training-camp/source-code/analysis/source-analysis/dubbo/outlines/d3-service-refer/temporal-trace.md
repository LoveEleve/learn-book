# D-3 服务引用 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.x | 骨架: ReferenceConfig.get → createProxy → Protocol.refer → Cluster.join (StaticDirectory) + injvm 本地引用 |
| 2.7.x | 启动检查 checkInvokerAvailable (fail-fast) + ConsumerModel/ServiceRepository 服务模型 |
| 3.x | **MigrationInvoker (ServiceDiscoveryMigrationInvoker)** — 接口级→应用级服务发现迁移 (FORCE_INTERFACE/APPLICATION_FIRST/FORCE_APPLICATION) + 多注册中心默认 **ZoneAwareCluster** + Deployer 生命周期 (prepare/start) |
| 3.3.x | ReferenceConfig 910 / RegistryProtocol 稳定; group 合并集群 + interceptInvoker |

## 痕迹证据

- ReferenceConfig.java:189: proxyFactory = ExtensionLoader(ProxyFactory).getAdaptiveExtension (2.x 锚)
- ReferenceConfig.java:692-693: 包装序注释 "ZoneAwareClusterInvoker(StaticDirectory) -> FailoverClusterInvoker(RegistryDirectory, routing happens here)" (3.x 锚)
- ProxyFactory.java:29: @SPI(value="javassist", scope=FRAMEWORK) — scope 参数 3.x 新增 (3.x 锚)
- RegistryProtocol.java:601-609: getMigrationInvoker → ServiceDiscoveryMigrationInvoker (3.x 锚)
- RegistryProtocol.java:830-858: OverrideListener.notify — 配置覆盖监听 (2.x 锚, 历史)
- MigrationInvoker.java: 迁移规则三态枚举 (3.x 锚, 实证)

## 推断标注

- "2.x 骨架" — Dubbo 2.x 公知版本线 (标注)
- "2.7.x ConsumerModel" — ServiceRepository 模型演进推断 (标注)
- "3.x MigrationInvoker" — 枚举/类名实证 (实证)
- **源码 grafted (浅克隆, 单 commit)**: git log 时空考古受限 — 本域以注释锚 + 枚举为主 (降级说明)

## 对照线 (阶段 4.3/5.1 已交付)

- ZK 会话订阅 vs Dubbo RegistryProtocol 订阅 (D-5 深潜) — 注册面对照
- Feign (F-2 契约解析): HTTP 客户端声明 vs Dubbo ReferenceConfig 声明 — 客户端对照
- Spring Bean 获取 (2-1 refresh): 单例容器 vs Dubbo 引用装配 — IoC 对照
