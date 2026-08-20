# N-10 客户端管理面 — 服务端的 clientId 模型与三管理器

> 前置: [[NC-6-服务端核心]] (InstanceOperator 消费) + [[NC-3-gRPC-Redo]] (连接面) | 引出: [[N-11-健康检查]] (客户端状态消费) | 对照: 1.x 无客户端模型 → 3.x clientId 核心
> 🔴 A | 方案 A (全深度) | 闭环: q1(门面) q2(三管理器) q3(客户端模型) q4(操作面)

**读者处境**: 3.x 服务端以 clientId 为核心 — 客户端连接、注册、订阅都挂在 clientId 上。这个模型怎么组织? gRPC 连接客户端 vs ip:port 客户端怎么共存?

### 1. 门面 — ClientService 与 ClientServiceImpl

场景: 客户端信息的查询/释放入口?
源码路径:
- **ClientService** (core/ClientService.java:34): 接口 — **getClientList** + **getClientDetail** + getPublishedServiceList/getSubscribeServiceList + releaseClient
- **ClientServiceImpl** (core/ClientServiceImpl.java:57): 实现 — **委托 ClientManager** (L60-81: clientManager.getClient(clientId)) + **connectionManager.getConnection** (L91: 连接详情) + 发布/订阅列表 (L123-180)
- @Deprecated getPublishedServiceListAdapt (2.x http 遗留)
关键设计 (q1): **"门面委托 = 查询与状态分离"** — ClientService 只做查询组装, 客户端生命周期归 ClientManager; 2.x API 标注 Deprecated。 [模式: 门面委托]

### 2. 三管理器 — ConnectionBased/EphemeralIpPort/PersistentIpPort

场景: 客户端怎么分类管理?
源码路径:
- **ClientManager** (v2/client/manager/ClientManager.java:30): 接口 — **clientConnected(clientId, attributes)** + syncClientConnected + **release** + verify + allClientId
- **ConnectionBasedClientManager** (impl/ConnectionBasedClientManager.java:49): gRPC 长连接客户端 (NC-1 连接面)
- **EphemeralIpPortClientManager** (impl/EphemeralIpPortClientManager.java:53): 临时 ip:port 客户端
- **PersistentIpPortClientManager** (impl/PersistentIpPortClientManager.java:47): 持久 ip:port
- **ClientManagerDelegate** (manager/ClientManagerDelegate.java:40): 委托聚合
关键设计 (q2): **"三管理器 = 连接模型分型"** — gRPC 连接 vs 临时/持久 ip:port 各自管理; delegate 聚合对外统一 — 与 NC-1 双代理路由 (ephemeral/ability) 的服务端对应。 [模式: 管理器分型]

### 3. 客户端模型 — AbstractClient 族与状态

场景: 客户端对象的数据与行为?
源码路径:
- **Client** (v2/client/Client.java:33): 接口
- **AbstractClient** (v2/client/AbstractClient.java:44): 基类 — **generateSyncData** (L138-164: ClientSyncData 集群同步载体) + **release** (L182) + 服务变更日志 (L82/97)
- **ConnectionBasedClient** (impl/ConnectionBasedClient.java:29): gRPC 连接客户端 — **isEphemeral** (L57)
- **IpPortBasedClient** (impl/IpPortBasedClient.java:39): ip:port 客户端
- **ClientAttributes** (v2/client/ClientAttributes.java:28): 客户端属性 (创建因子)
- **ClientSyncData/ClientSyncDatumSnapshot** (v2/client/): 集群同步数据 (Distro 面)
关键设计 (q3): **"客户端 = 状态载体 + 同步载体"** — 客户端对象既管本地状态又生成同步数据 (ClientSyncData); release 生命周期显式。 [模式: 双载体]

### 4. 工厂族 — ClientFactory 三实现

场景: 客户端对象谁创建?
源码路径:
- **ClientFactory** (v2/client/factory/ClientFactory.java:27) + **ClientFactoryHolder** (factory/ClientFactoryHolder.java:32)
- **ConnectionBasedClientFactory** (factory/impl/ConnectionBasedClientFactory.java:31): gRPC 客户端工厂
- **EphemeralIpPortClientFactory** / **PersistentIpPortClientFactory**: ip:port 双工厂
- 按 attributes 分派 (与 ClientManager 三实现对应)
关键设计 (q4): **"工厂族 = 创建与使用分离"** — Manager 管生命周期, Factory 管创建; Holder 提供全局入口。 [模式: 工厂族]

### 5. 实例操作面 — ClientOperationService 接口与双实现

场景: 客户端注册/注销实例的实际落点?
源码路径:
- **ClientOperationService** (v2/service/ClientOperationService.java:36): 接口 — 注册/注销/订阅的 clientId 级管理 (NC-6 InstanceOperatorClientImpl 消费)
- **EphemeralClientOperationServiceImpl** (v2/service/impl/EphemeralClientOperationServiceImpl.java:47) / **PersistentClientOperationServiceImpl** (v2/service/impl/PersistentClientOperationServiceImpl.java:85, 503 行): 临时/持久双实现 (CP 面 RequestProcessor4CP)
关键设计 (q5): **"操作面 = 临时/持久双实现"** — 与一致性双落点 (Distro/JRaft) 对应; 操作挂 clientId。 [模式: 双实现操作]

### 6. 清理/事件/索引面 — cleaner + event + index

场景: 客户端清理与事件?
源码路径:
- **NamingCleaner 族** (v2/cleaner/): EmptyServiceAutoCleanerV2 + ExpiredMetadataCleaner + AbstractNamingCleaner — 过期清理
- **NamingEventPublisher** (v2/event/publisher/NamingEventPublisher.java:39): 事件发布器
- **ClientEvent/ClientOperationEvent** (v2/event/client/): 客户端事件族
- **NamingFuzzyWatchContextService** (v2/index/NamingFuzzyWatchContextService.java:55): 模糊订阅索引
- **NamingMetadataManager** (v2/metadata/NamingMetadataManager.java:44): 元数据管理
关键设计 (q6): **"横切面 = 清理/事件/索引/元数据"** — 客户端生命周期的清理 (cleaner), 变更的事件化 (event), 订阅的索引 (fuzzy), 服务元数据 (metadata)。 [模式: 横切四件]

### 7. 测试与行为锚

场景: 客户端管理的边界?
源码路径:
- 测试: ClientManagerTest / AbstractClientTest (naming test)
- 日志锚: "Client change for service {}" (AbstractClient:82) / "Client remove for service {}" (L97)
- @Deprecated 2.x API (ClientService)
关键设计 (q1): **"日志锚 = 客户端生命周期可观测"** — 变更/移除日志。 [模式: 生命周期日志]
