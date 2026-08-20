# NC-1 NamingService 注册发现 — 客户端门面下的双代理与订阅缓存

> 前置: [[ALI-A3-NacosDiscoveryClient]] (集成层消费方) | 引出: [[NC-3-gRPC-Redo]] (通信内核) + [[NC-4-容灾缓存]] (failover 面) | 对照: 执行计划 5.8 1.x 语义已过时 (BeatReactor 不存在于 3.0.3)
> 🔴 A | 方案 A (全深度) | 闭环: q1(门面漏斗) q2(双代理路由) q3(订阅链路) q4(差异缓存)

**读者处境**: Spring Cloud Alibaba 的 NacosServiceRegistry.register 最终调用 namingService.registerInstance — 这一行背后经过多少层?为什么有的实例走 gRPC 有的走 HTTP?服务端推送的实例列表怎么进缓存、怎么触发监听器、怎么落盘?

### 1. 门面与重载漏斗 — NacosNamingService 的 init 与多重载收敛

场景: NamingService 接口 20+ 重载怎么收敛到几个核心调用?
源码路径:
- **NacosNamingService** (naming/NacosNamingService.java:75, 682 行): implements NamingService
- **init** (L111-132): PreInitUtils 预加载 → 命名空间 (L116) → **NotifyCenter 注册** (L122-124: InstancesChangeEvent 发布器 + InstancesChangeNotifier 订阅器) → **ServiceInfoHolder** (L125, 缓存+故障转移) → **NamingClientProxyDelegate** (L130-131, 双代理)
- **registerInstance 重载漏斗** (L140-175): 6 重载 (ip+port → +clusterName → +groupName → Instance 对象) → **最终收敛 registerInstance(serviceName, groupName, instance)** (L171-175): NamingUtils.checkInstanceIsLegal + clientProxy.registerService
- **deregister/getAllInstances/selectInstances/subscribe** 同样 4-6 重载收敛 (L231-280/454-481)
关键设计 (q1): **"重载漏斗 = 参数默认值填充"** — 接口的 n 重载只是不同缺省组合 (DEFAULT_GROUP/DEFAULT_CLUSTER_NAME/空 clusters/subscribe=true), 最终汇成一个核心方法 — 与 ALI-A3 的端口仲裁式收敛同构。 [模式: 重载漏斗]

### 2. 双代理路由 — NamingClientProxyDelegate 的 ephemeral 分叉

场景: 什么时候走 gRPC, 什么时候走 HTTP?
源码路径:
- **NamingClientProxyDelegate** (naming/remote/NamingClientProxyDelegate.java:55): 双代理 — httpClientProxy (L63) + grpcClientProxy (L65)
- **getExecuteClientProxy 路由** (L197-203): **`instance.isEphemeral() || grpcClientProxy.isAbilitySupportedByServer(...)` → grpcClientProxy / 否则 httpClientProxy** (L198-202) — **临时实例短路 gRPC; 持久实例在服务端支持 gRPC 能力时也走 gRPC, 仅老服务端 (无 gRPC) 时才回退 HTTP**
- **batch 强制 gRPC** (L102-121): batchRegisterService/batchDeregisterService 直连 grpcClientProxy
- **订阅面强制 gRPC** (L166-189): subscribe/unsubscribe/isSubscribed/queryInstancesOfService/getServiceList 全走 grpc
- serverHealthy 双检 (L194): grpc || http
关键设计 (q2): **"能力路由 = gRPC 优先 + 老服务端兼容"** — 3.x 主线 gRPC, 但兼容无 gRPC 能力的旧服务端 (持久实例回退 HTTP); 订阅和批量是 gRPC 专属能力 (长连接推送需要) — 双协议并存是升级期兼容策略。 [模式: 能力路由]

### 3. gRPC 订阅链路 — NamingGrpcClientProxy 的 redo 前置

场景: subscribe 怎么变成服务端长连接订阅?
源码路径:
- **NamingGrpcClientProxy** (naming/remote/gprc/NamingGrpcClientProxy.java:90, 558 行): extends AbstractNamingClientProxy
- **构造** (L107-135): **RpcClientFactory.createClient(uuid, GRPC, config)** (L122) → **NamingGrpcRedoService** (L119) → **rpcClient.registerConnectionListener(redoService)** (L129 — 断线重做钩子) → **registerServerRequestHandler(NamingPushRequestHandler)** (L131 — 服务端推送处理器, 写 ServiceInfoHolder) → rpcClient.start (L134)
- **subscribe 三连** (L392-410): ① **redoService.cacheSubscriberForRedo** (L395 — 先登记重做) ② **doSubscribe** (L397-406): SubscribeServiceRequest → requestToServer → SubscribeServiceResponse ③ redoService.subscriberRegistered (L404)
- **unsubscribe 对称** (L416-424): subscriberDeregister → doUnsubscribe (L434-439: subscribe=false 请求 + removeSubscriberForRedo)
- **服务端列表变更** (L139-146): onEvent(ServerListChangeEvent) → **rpcClient.onServerListChange()**
关键设计 (q3): **"redo 前置 = 先登记后发送"** — 每个操作先写入重做队列 (断线后自动补发), 成功才标记 registered; subscribe 的"注册+订阅+推送"三态由 redoService 统一管理 — 与 NC-3 深度衔接。 [模式: redo 前置]

### 4. 缓存与差异 — ServiceInfoHolder.processServiceInfo 四步

场景: 服务端推送的 JSON 怎么变成监听器能感知的变更?
源码路径:
- **ServiceInfoHolder** (naming/cache/ServiceInfoHolder.java:46): ConcurrentMap<String, ServiceInfo> serviceInfoMap (L48) + FailoverReactor (L50) + **pushEmptyProtection 开关** (L52/L87-95)
- **processServiceInfo 四步** (L124-164): ① **空/错误推送忽略** (L132-137: isEmptyOrErrorPush — hosts null 或 pushEmptyProtection 且校验失败 → return oldService) ② put 缓存 (L138) ③ **InstancesDiffer.doDiff 差异计算** (L139 → InstancesDiffer.java:39) ④ **差异时**: 非 failover 状态 → **NotifyCenter.publishEvent(InstancesChangeEvent)** (L156-160) + **DiskCache.write 落盘** (L161)
- **doDiff** (InstancesDiffer.java:39): old null → 全部 added; **lastRefTime 过期数据忽略** (L48-54: old.t > new.t → warn + 空 diff); toInetAddr 为 key 分组 → added/modified/removed 三集合
- 启动可加载磁盘缓存 (L65-66: NAMING_LOAD_CACHE_AT_START)
关键设计 (q4): **"忽略+差异+事件+落盘 四步"** — 空推送静默 (防服务端误推送清空列表, pushEmptyProtection 可控), 差异计算只发变更 (避免全量通知), 事件驱动监听器, 落盘供下次启动 (NC-4 面)。 [模式: 差异事件缓存]

### 5. 发现三路 — getServiceInfo 的 failover → 缓存 → 直查

场景: getAllInstances 的数据从哪来? 优先级?
源码路径:
- **getServiceInfo** (NacosNamingService.java:351-361): ① **failover 开关优先** (L353-360: isFailoverSwitch && 数据非空 → 返回 failover 数据) ② 否则 getServiceInfoBySubscribe
- **getServiceInfoBySubscribe** (L366-376): subscribe=true → **serviceInfoHolder 缓存 + tryToSubscribe 补订阅**; subscribe=false → **clientProxy.queryInstancesOfService 直查** (L373)
- **selectInstances 过滤** (L331-345): `healthy != instance.isHealthy() || !isEnabled() || weight <= 0 → remove` — 与 ALI-A3 hostToServiceInstance 的同语义二次过滤 (服务端已滤)
关键设计 (q5): **"三路数据源 = 容灾 > 缓存 > 直查"** — 故障转移数据最优先 (NC-4), 订阅缓存次之 (实时性靠推送), 非订阅直查兜底; selectInstances 过滤与集成层 (ALI-A3) 语义一致。 [模式: 数据源优先级]

### 6. 订阅门面与通知 — changeNotifier 的注册/去注册

场景: 业务监听器挂在谁身上? 何时真正取消服务端订阅?
源码路径:
- **doSubscribe** (NacosNamingService.java:486-528): **changeNotifier.registerListener** (L516) + notifyIfSubscribed (L517) + **clientProxy.subscribe** (L518)
- **doUnsubscribe** (L530-539): changeNotifier.deregisterListener (L531) + **`!changeNotifier.isSubscribed(groupName, serviceName)` → clientProxy.unsubscribe** (L532-534) — 无监听器才真正取消服务端订阅
- InstancesChangeNotifier: 本地事件分发器 (NotifyCenter 体系)
关键设计 (q6): **"本地注册先行, 服务端订阅后置"** — 监听器全挂本地 notifier; 服务端订阅按需 (最后一个监听器注销才 unsubscribe) — 避免频繁 gRPC 订阅往返。 [模式: 本地注册+按需远端订阅]

### 7. 保护与定时更新 — ProtectMode 与 ServiceInfoUpdateService

场景: 服务端异常时客户端怎么自我保护? 订阅的兜底刷新?
源码路径:
- **ProtectMode** (naming/core/ProtectMode.java:24): **protectThreshold 默认 0.8F** — 保护阈值语义 (服务端 NC-6 面消费)
- **ServiceInfoUpdateService** (naming/core/ServiceInfoUpdateService.java:47): 订阅后兜底 — **scheduleUpdateIfAbsent 双检** (L102-116: futureMap 判空 + synchronized 双检 → schedule UpdateTask) — 定时查询防推送丢失
关键设计 (q7): **"兜底轮询 = 推送的保险丝"** — 推送为主 (gRPC 长连接), 定时查询兜底 (UpdateTask); 双检防重复调度。 [模式: 推送+轮询双保险]

### 8. 测试与行为锚

场景: 注册/发现的边界行为?
源码路径:
- 测试: NacosNamingServiceTest / ServiceInfoHolderTest / InstancesDifferTest (client/src/test)
- 注释锚: "each Naming service should have different namespace" (L83-85) / "out of date data received" (InstancesDiffer L49)
- 日志锚: "[GRPC-SUBSCRIBE]" (L393) / "[GRPC-UNSUBSCRIBE]" (L417) — 订阅生命周期可观测
关键设计 (q1): **"日志锚 = 链路可观测"** — 每次订阅/取消有 GRPC 前缀日志, 排障靠它。 [模式: 日志锚]
