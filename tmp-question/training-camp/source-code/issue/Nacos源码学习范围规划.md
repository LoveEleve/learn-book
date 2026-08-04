# Nacos 源码学习范围规划

> **基准**: Nacos 3.0.3
> **数据源**: Nacos 仓库 2006 文件逐包扫描（client 120 + client-basic 51 + api 274 + config 243 + core 264 + consistency 23 等）
> **边界**: 聚焦客户端 SDK（client + client-basic）+ 服务端核心（config + core + consistency），按需学习其他
> **my-xhs 关联**: Nacos 是 my-xhs 的注册中心+配置中心，Spring Cloud Alibaba 底层

---

## 第 1 层：客户端 SDK（3 🔴）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| NC-1 | **NamingService 服务注册发现** | 🔴 | client/naming + client/naming/core | NacosNamingService/NacosNamingMaintainService/NamingClientProxy/NamingClientProxyDelegate/AbstractNamingClientProxy/NamingGrpcClientProxy/ServiceInfoHolder/DiskCache/InstancesDiffer/InstancesChangeEvent/InstancesChangeNotifier/Balancer/NamingServerListManager/ProtectMode/ServiceInfoUpdateService | `NacosNamingService implements NamingService`（源码验证：`registerInstance`+`getAllInstances`多重载+`subscribe`参数）。`NamingGrpcClientProxy`（源码验证：`subscribe`→`doSubscribe`向服务端发送订阅+`onEvent(ServerListChangeEvent)`→`rpcClient.onServerListChange()`）。`ServiceInfoHolder.processServiceInfo`（`InstancesDiff`差异计算）。**`ProtectMode`**保护模式（服务列表为空时保留旧实例避免请求失败）。`ServiceInfoUpdateService`定时更新服务信息。`Balancer`客户端负载均衡。面试必问 |
| NC-2 | **ConfigService 配置中心客户端** | 🔴 | client/config + client/config/impl + api/config | NacosConfigService/ConfigService/ClientWorker/ConfigRpcTransportClient/ConfigTransportClient/LocalConfigInfoProcessor/ConfigFilterChainManager/CacheData/ConfigChangeHandler/Limiter/LocalEncryptedDataKeyProcessor/PropertiesChangeParser/YmlChangeParser/ConfigFuzzyWatchContext/@NacosConfigListener/@NacosConfigurationProperties/@NacosValue/ConfigChangeEvent/ConfigChangeItem | `NacosConfigService implements ConfigService`（源码验证：`getConfig`→`getConfigInner`→`worker.getAgent().queryConfig`）。**`publishConfigInner`**支持普通发布+**CAS 发布**（`publishConfigCas`带`casMd5`乐观锁）。`ClientWorker.ConfigRpcTransportClient`（`listenExecutebell`信号量+`checkLocalConfig`）。`CacheData`/`Limiter`/`LocalEncryptedDataKeyProcessor`。**api/config/annotation**：`@NacosConfigListener`/`@NacosConfigurationProperties`/`@NacosValue`/`@NacosIgnore`/`@NacosProperty` 注解。`ConfigChangeEvent`/`ConfigChangeItem` 变更事件。面试必问 |
| NC-3 | **gRPC 通信 + Redo 重做机制** | 🔴 | client/naming/remote/gprc + client/naming/remote/gprc/redo + client/redo + client-basic/remote + api/remote | NamingGrpcClientProxy/NamingGrpcRedoService/InstanceRedoData/SubscriberRedoData/BatchInstanceRedoData/RpcClient/RpcClientFactory/GrpcClientConfig/GrpcConnection/ConnectionEventListener/AbstractRedoService/AbstractRedoTask/HttpClientManager/ConfigChangeNotifyRequest/ConfigBatchListenRequest/ConfigFuzzyWatchChangeNotifyRequest | **Nacos 3.x 架构**：`NamingGrpcClientProxy`（`RpcClientFactory.createClient(GRPC)`+`doRegisterService`+`subscribe`→`doSubscribe`）。**`NamingGrpcRedoService implements ConnectionEventListener`**（`onConnected`清空重做队列+`onDisconnected`标记需重做+三种 RedoData）。`AbstractRedoService`/`AbstractRedoTask`重做基类。**api/remote**：gRPC 请求协议定义（`ConfigChangeNotifyRequest`/`ConfigBatchListenRequest`/`ConfigFuzzyWatchChangeNotifyRequest`）。面试必问 |

## 第 2 层：服务端核心（4 🟡）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| NC-4 | **本地缓存 + 故障转移 + 地址管理** | 🟡 | client/naming/cache + client/naming/backups + client/config + client-basic/address | DiskCache/ServiceInfoHolder/InstancesDiffer/LocalConfigInfoProcessor/FailoverData/FailoverDataSource/FailoverReactor/FailoverSwitch/NamingFailoverData/AbstractServerListManager/EndpointServerListProvider/PropertiesListProvider/ServerListProvider/ServerListChangeEvent | `DiskCache`磁盘缓存。`ServiceInfoHolder`内存+磁盘。`LocalConfigInfoProcessor`配置故障转移。**`client/naming/backups`命名故障转移体系**（源码验证：`FailoverReactor`故障转移反应器+`FailoverDataSource`/`FailoverData`/`NamingFailoverData`数据+`FailoverSwitch`开关）。**`ServerListProvider`体系**（`AbstractServerListManager`+`EndpointServerListProvider`+`PropertiesListProvider`）替代 1.x ServerListManager |
| NC-5 | **一致性协议（Distro + SOFA-JRaft）** | 🟡 | consistency + core/distributed/distro + core/distributed/raft | ConsistencyProtocol/APProtocol/CPProtocol/RequestProcessor4AP/RequestProcessor4CP/DistroCallback/DistroComponentHolder/DistroDataProcessor/DistroDataStorage/DistroModuleStateBuilder/JRaftProtocol/JRaftServer/NacosStateMachine/NacosClosure/JSnapshotOperation/NoLeaderException | AP 模式 **Distro 协议**（在`core/distributed/distro/`，`DistroDataProcessor`+`DistroDataStorage`+`DistroCallback`——最终一致，各节点负责部分数据，主动同步）。CP 模式 **SOFA-JRaft**（源码验证：`JRaftProtocol`+`JRaftServer`+`NacosStateMachine`+`NacosClosure`+`JSnapshotOperation`+`NoLeaderException`——用 SOFA-JRaft 框架实现 Raft，不是自己实现）。`ConsistencyProtocol`统一接口。面试会问"Nacos 的 AP 和 CP 怎么选" |
| NC-6 | **服务端核心（配置存储 + 集群管理 + 分布式协议）** | 🟡 | core + config + api + naming | core/distributed/distro + core/cluster + core/controller + config/server + api + naming/controllers + naming/core + naming/cluster + naming/consistency + naming/healthcheck | `core`子模块（264 文件）：`distributed/`（Distro+Raft 协议实现）+`cluster/`（集群管理）+`controller/`（控制器）。**`naming`子模块（271 文件，服务端命名服务）**（源码验证：`NamingApp`启动类+`controllers/`（InstanceController/ServiceController/ClusterController/HealthController/CatalogController+v2/v3）+`core/`（ClientServiceImpl/DistroMapper/InstanceOperatorClientImpl/CatalogService）+`cluster/`（ServerStatusManager）+`consistency/`（ephemeral/persistent+Datum/KeyBuilder）+`healthcheck/`（HealthCheckReactor/NacosHealthCheckTask/heartbeat）+`push/`+`remote/`）。`config/server`配置存储。面试会问 |
| NC-7 | **安全认证 + 监控 + 限流/加密** | 🟡 | auth + client/security + client/config/impl | AuthManager/PermissionManager/UserManager/SecurityProxy/MetricsMonitor/ClientMetrics/Limiter/LocalEncryptedDataKeyProcessor/ConfigEncryptionFilter | `auth`认证授权（用户/权限管理）。`SecurityProxy`客户端安全代理。`MetricsMonitor`监控指标（连接健康/请求统计/`getServiceInfoMapSizeMonitor`）。**`Limiter`**回调限流（`@NacosConfigListener`回调频率限制）。**`LocalEncryptedDataKeyProcessor`**配置加密（KMS 集成）。**修正之前误判**：Nacos 客户端已有监控+限流+加密机制 |

---

## 淘汰清单

| 子模块 | 文件数 | 理由 |
|---|---|---|
| console（79 文件） | 79 | 管理控制台，前端为主 |
| console-ui | — | 前端 UI |
| istio（40 文件） | 40 | Istio 集成，不学 |
| ai（31 文件） | 31 | AI 特性，新功能按需 |
| k8s-sync（4 文件） | 4 | K8s 同步，不学 |
| address（8 文件） | 8 | 地址管理，合并到 NC-6 |
| cmdb（9 文件） | 9 | CMDB，不学 |
| lock（20 文件） | 20 | 分布式锁，按需 |
| logger-adapter-impl | — | 日志适配，不学 |
| maintainer-client | 26 | 运维客户端，不学 |
| mcp-registry-adaptor | 6 | MCP 适配，不学 |
| persistence（36 文件） | 36 | 持久化，按需 |
| prometheus（7 文件） | 7 | Prometheus，不学 |
| server（6 文件） | 6 | 启动入口，合并到 NC-6 |
| sys（25 文件） | 25 | 系统，不学 |
| auth（28 文件） | 28 | 认证，合并到 NC-7 |
| bootstrap（1 文件） | 1 | 启动 |
| example（5 文件） | 5 | 示例 |
| distribution/doc | — | 文档/构建/启动 |

---

## 统计

| | 数量 |
|---|---|
| 🔴 核心域 | **3** |
| 🟡 重要域 | **4** |
| 总计 | **7 域** |
| 预计产出文章 | 3 篇（🟡 子域在对应 🔴 中附带）|
| 核心子模块覆盖 | client(120) + client-basic(51) + config(243) + core(264) + consistency(23) + api(274) |

## 与 Spring Cloud Alibaba 的关联

| Alibaba 域 | Nacos 关联 | 关系 |
|---|---|---|
| A-1 NacosPropertySourceLocator | NC-2 ConfigService | Alibaba 用 NacosConfigService 拉取配置 |
| A-2 Nacos 配置动态刷新 | NC-2 addListener | Alibaba 用 ConfigService.addListener 监听变更 |
| A-3 NacosDiscoveryClient + NacosServiceRegistry | NC-1 NamingService | Alibaba 用 NamingService.registerInstance/getInstances |

## Nacos 3.x 架构变化（重要）

| 1.x | 3.x | 变化 |
|---|---|---|
| BeatReactor HTTP 心跳 | NamingGrpcClientProxy gRPC 长连接 | 完全替代 |
| ServerListManager HTTP 轮询 | client-basic/address + RpcClient | 重构 |
| HTTP 短连接 | gRPC 长连接 + Redo 重做 | 通信协议升级 |

**注意**：BeatReactor/ServerListManager 在 Nacos 3.x 中**已不存在**。之前记忆中的这些类是 1.x 时代的，需要更新。
