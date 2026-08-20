# NC-6 服务端核心 — 注册/健康检查/推送/配置存储的服务端全貌

> 前置: [[NC-1-Naming]] + [[NC-2-Config]] (客户端对面) + [[NC-5-一致性]] (存储底座) | 引出: [[NC-7-安全监控]] (横切面) | 对照: 执行计划 5.8 NC-5/NC-6 (健康检查/服务端)
> 🟡 B | 方案 B (重要域) | 闭环: q1(入口链) q2(健康检查) q3(推送面) q4(配置存储)

**读者处境**: 客户端 registerInstance 到达服务端后走哪条链? 心跳/健康检查的调度怎么组织? 服务端怎么把变更推给订阅者? 配置存储的 DB/文件双写?

### 1. 入口链 — NamingApp 与 InstanceController → Operator → ClientService

场景: 注册请求的服务端入口与转发?
源码路径:
- **NamingApp** (naming/NamingApp.java:30): @EnableScheduling + @SpringBootApplication(scanBasePackages = naming+core)
- **InstanceController** (controllers/InstanceController.java:90): **registerInstance** (L127: `getInstanceOperator().registerInstance(namespaceId, serviceName, instance)`) + updateInstance (L180) + 注销发布 **DeregisterInstanceTraceEvent** (L155-156, trace 事件)
- **InstanceOperatorClientImpl** (core/InstanceOperatorClientImpl.java:73): implements InstanceOperator — registerInstance (L106-113: **clientOperationService.registerInstance(service, instance, clientId)** — 3.x 客户端模型) + deregister/update
- **ClientServiceImpl** (core/ClientServiceImpl.java:57): 客户端生命周期管理 (registerClient/connectionDisconnect)
关键设计 (q1): **"Controller → Operator → ClientService 三层"** — 入口/业务/客户端管理分离; 3.x 以 clientId 为核心 (gRPC 长连接绑定)。 [模式: 分层入口]

### 2. 健康检查 — HealthCheckReactor 的调度面

场景: 心跳与健康检查的调度怎么组织?
源码路径:
- **HealthCheckReactor** (healthcheck/HealthCheckReactor.java:36): **scheduleCheck(v2)** (L48-54: HealthCheckTaskV2 + HealthCheckTaskInterceptWrapper + scheduleNamingHealth) / **scheduleCheck(BeatCheckTask)** (L56-64: **futureMap.computeIfAbsent + 5000ms 周期** — 心跳检查) / **cancelCheck** (L66-77: cancel + remove)
- **NacosHealthCheckTask** (healthcheck/NacosHealthCheckTask.java:26): 心跳检查任务 (instance 状态)
- **v2/HealthCheckTaskV2**: 3.x 新健康检查
- healthcheck/ 包: HealthCheckStatus/RsInfo/heartbeat/ 子包
关键设计 (q2): **"futureMap + computeIfAbsent = 任务去重"** — 每实例一个周期任务, 防重复调度; 5s 周期心跳检查。 [模式: 任务注册表]

### 3. 推送面 — UdpPushService 遗留与 v2 新通道

场景: 服务端怎么推送变更?
源码路径:
- **UdpPushService** (push/UdpPushService.java:49): **1.x 遗留 UDP 推送**
- **v2/** (push/v2): gRPC 推送面
- **NamingSubscriberService** (push/NamingSubscriberService.java:29): 订阅服务接口 + NamingSubscriberServiceLocalImpl (本地) + **NamingSubscriberServiceAggregationImpl** (聚合)
- **NamingFuzzyWatchChangeNotifier**: 模糊订阅通知
关键设计 (q3): **"双推送通道 = UDP 遗留 + gRPC 新面"** — 3.x 主线 gRPC, UDP 兼容老客户端; 订阅服务本地/聚合双实现。 [模式: 双通道推送]

### 4. 一致性落点 — ephemeral/persistent 双存储

场景: 注册数据怎么进一致性协议?
源码路径:
- **consistency/ephemeral/distro/**: 临时实例 → **Distro** (AP, NC-5)
- **consistency/persistent/impl/**: 持久实例 → **JRaft** (CP, NC-5)
- **Datum/KeyBuilder** (consistency/Datum.java:29 + KeyBuilder.java:27): 数据与键构造
关键设计 (q4): **"ephemeral→Distro / persistent→JRaft 双落点"** — 临时实例可丢 (AP), 持久实例强一致 (CP) — 面试 "Nacos 的 AP 和 CP 怎么选" 的代码答案。 [模式: 双存储路由]

### 5. 配置服务端 — config/server 的控制器与存储

场景: 配置读写请求的服务端处理?
源码路径:
- **ConfigController** (config/server/controller/ConfigController.java:113): 配置读写入口 (publishConfig/getConfig/removeConfig)
- **CommunicationController** (controller/CommunicationController.java:58): 长轮询/心跳通信
- **ListenerController**: 监听管理
- config/server/service: 存储服务 (ConfigInfoService 等)
- config/server/remote: gRPC 面
关键设计 (q5): **"配置面 = 控制器 + 存储服务分离"** — HTTP 控制器与 gRPC 面并存, 存储服务统一。 [模式: 面存储分离]

### 6. 测试与行为锚

场景: 服务端的边界行为?
源码路径:
- 测试: InstanceControllerTest / HealthCheckReactorTest (各模块 test)
- 日志锚: [CANCEL-CHECK] (HealthCheckReactor:70) / [NA] (naming)
- trace 事件: DeregisterInstanceTraceEvent (L155)
关键设计 (q1): **"trace 事件 = 注册变更可观测"** — 注销发布 trace 事件, 全链路追踪。 [模式: trace 可观测]
