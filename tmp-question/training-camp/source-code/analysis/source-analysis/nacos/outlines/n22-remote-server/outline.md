# N-22 远程服务端面 — gRPC 双服务器、请求注册表与连接管理

> 前置: [[NC-3-gRPC-Redo]] (客户端内核) + [[N-17-配置远程]] (handler 消费) | 对照: 服务端接收面的完整闭环
> 🔴 A | 方案 A (全深度) | 闭环: q1(双服务器) q2(请求注册表) q3(连接管理)

**读者处境**: 客户端 gRPC 连接的服务端接收面怎么组织? SDK 与集群双服务器? 请求怎么路由到 handler? 连接怎么管理?

### 1. 双服务器 — GrpcSdkServer 与 GrpcClusterServer

场景: gRPC 服务端怎么分?
源码路径:
- **BaseGrpcServer** (grpc/BaseGrpcServer.java:64) / **BaseRpcServer** (remote/BaseRpcServer.java:34): 基类
- **GrpcSdkServer** (grpc/GrpcSdkServer.java:46): SDK 客户端服务器 (业务请求)
- **GrpcClusterServer** (grpc/GrpcClusterServer.java:46): 集群服务器 (节点间)
- **GrpcBiStreamRequestAcceptor** (grpc/GrpcBiStreamRequestAcceptor.java:52): 双向流接收
- 过滤器/拦截器: NacosGrpcServerTransportFilter / NacosGrpcServerInterceptor + ServiceLoader 族
- 协议协商: AbstractProtocolNegotiatorBuilderSingleton 族
关键设计 (q1): **"双服务器 = SDK/集群隔离"** — 业务与节点间流量分开, 各有拦截器链; 双向流与单向流双接收。 [模式: 双服务器]

### 2. 请求注册表 — RequestHandlerRegistry 与 RequestHandler

场景: gRPC 请求怎么路由?
源码路径:
- **RequestHandler** (remote/RequestHandler.java:33): 处理器接口
- **RequestHandlerRegistry** (remote/RequestHandlerRegistry.java:45): implements ApplicationListener<ContextRefreshedEvent> — **getByRequestType** (L57: 按类型查) — Bean 收集注册
- **GrpcRequestAcceptor** (grpc/GrpcRequestAcceptor.java:56): extends RequestGrpc.RequestImplBase — **requestHandlerRegistry.getByRequestType(type)** (L114) + null 处理 (L116)
- 业务 handler: Naming/Config 各模块注册 (N-10/N-17 已见)
关键设计 (q2): **"注册表 = 类型路由"** — 上下文刷新时收集全部 RequestHandler Bean; acceptor 按请求类型查表分发; 未知类型兜底。 [模式: 请求注册表]

### 3. 连接管理 — ConnectionManager

场景: 连接生命周期怎么管理?
源码路径:
- **ConnectionManager** (remote/ConnectionManager.java:57): **connections Map** (L93: checkValid) + **register(connectionId, connection)** (L102) + **连接限制规则** (L80-81: ControlManagerCenter.getConnectionLimitRule)
- **Connection** (remote/Connection.java:32) + **ConnectionMeta** (remote/ConnectionMeta.java:38): 连接与元数据
- **ClientConnectionEventListenerRegistry** (remote/ClientConnectionEventListenerRegistry.java:32): 连接事件监听注册
- **RemotingHeartBeatEvent** (event/RemotingHeartBeatEvent.java:26): 心跳事件
关键设计 (q3): **"连接管理 = 注册 + 限制 + 心跳"** — 连接注册表 + 控制中心限流规则 + 心跳事件驱动健康。 [模式: 连接生命周期]

### 4. 测试与行为锚

场景: 远程面边界?
源码路径:
- 测试: RequestHandlerRegistryTest / ConnectionManagerTest (core test)
- 日志锚: 连接注册/断开日志
关键设计 (q1): **"注册表上下文刷新收集"** — 事件驱动收集避免硬编码。 [模式: 事件收集]
