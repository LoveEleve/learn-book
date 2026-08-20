# NC-3 gRPC 通信 + Redo 重做 — 长连接底座与断线自愈

> 前置: [[NC-1-Naming]] + [[NC-2-Config]] (双主线共用底座) | 引出: 服务端 remote 面 (NC-6) | 对照: 规划声称 client-basic/remote (09 审计修正为 common/remote)
> 🔴 A | 方案 A (全深度) | 闭环: q1(连接内核) q2(状态机) q3(重做四态) q4(定时扫描)

**读者处境**: 注册/订阅操作"先登记后发送"的 redo 队列怎么工作?断线重连后注册状态怎么恢复?RpcClient 的状态机怎么流转?为什么重做数据有 4 种状态组合?

### 1. 连接内核 — RpcClient 在 common 模块 (09 审计路径修正)

场景: gRPC 客户端内核在哪? 规划说的 client-basic/remote 对不对?
源码路径:
- **09 审计**: RpcClient/RpcClientFactory/GrpcConnection/GrpcClientConfig 全在 **common/remote/client/** (common/src/main/java/com/alibaba/nacos/common/remote/client/, 4368 行) — **规划声称 client-basic/remote 只有 HttpClientManager (100 行)** → 路径修正
- **RpcClient** (RpcClient.java:70, 1046 行): 连接门面 — 状态 (L78-79 AtomicReference<RpcClientStatus>) + **reconnectionSignal BlockingQueue<ReconnectContext>(1)** (L83) + 事件队列 (L86)
- **start 双消费者** (L242+): ① 事件消费者: eventLinkedBlockingQueue → **notifyConnected/notifyDisConnected** (L253-257) ② 重连循环: reconnectionSignal.poll(keepAlive) → 超时 **healthCheck** (L275) → 失败 → UNHEALTHY (L284-290) → 重连
- **onServerListChange** (L216-231): 当前连接服务器不在最新列表 → **switchServerAsync**
- 变体: GrpcClient/GrpcSdkClient/GrpcClusterClient (grpc/ 子包)
关键设计 (q1): **"双队列驱动 = 事件队列 + 重连信号量"** — 连接事件异步消费 (不阻塞 IO), 重连靠信号量唤醒 + keepAlive 超时健康检查兜底 — 与 NC-2 的 listenExecutebell 信号量同构。 [模式: 队列驱动]

### 2. 连接监听器 — ConnectionEventListener 的钩子面

场景: 谁在感知连接建立/断开?
源码路径:
- **ConnectionEventListener** (common/remote/client/ConnectionEventListener.java:24): onConnected(Connection) / onDisConnect(Connection)
- 注册: NamingGrpcClientProxy.registerConnectionListener(redoService) (NC-1 L129) / 其他模块各自注册
- **NamingGrpcRedoService implements ConnectionEventListener** (L52)
关键设计 (q2): **"监听器 = 断线感知的标准钩子"** — 所有依赖连接状态的面 (redo/订阅) 通过同一接口感知, 避免各自轮询。 [模式: 事件钩子]

### 3. 重做四态机 — RedoData 的 registered/unregistering/expectedRegistered

场景: 重做数据的三种布尔怎么组合出四种操作?
源码路径:
- **RedoData<T>** (client/redo/data/RedoData.java:27): **expectedRegistered (期望终态)** + **registered (已注册)** + **unregistering (注销中)** (L26-36)
- **getRedoType 四组合** (L97-113, 注释四行语义表): ① registered && !unregistering → **NONE** (已注册无需动作) 或 UNREGISTER (期望注销) ② registered && unregistering → **UNREGISTER** ③ !registered && !unregistering → **REGISTER** ④ 其他 → REGISTER 或 REMOVE
- **registered()/unregistered()** (L87-94): 状态转移方法
- isNeedRedo (L97-98): `!RedoType.NONE.equals(getRedoType())`
- 三数据族: InstanceRedoData/SubscriberRedoData/BatchInstanceRedoData (naming/remote/gprc/redo/data/)
关键设计 (q3): **"三布尔四组合 = 期望与现实的差"** — expectedRegistered (用户意图) vs registered (服务端确认) vs unregistering (注销中) — 组合出"要不要重做、重做什么" — 注释用四行表格固化语义。 [模式: 期望-现实状态机]

### 4. 双 Map 重做仓库 — NamingGrpcRedoService

场景: 哪些注册/订阅需要重做? 状态怎么维护?
源码路径:
- **NamingGrpcRedoService** (naming/remote/gprc/redo/NamingGrpcRedoService.java:52): 双 Map — **registeredInstances (InstanceRedoData)** (L60) + **subscribes (SubscriberRedoData)** (L62) + **connected 标志** (L68)
- **onDisConnect 全标记** (L100-113): 所有 redoData.setRegistered(false) + fuzzy 重置 — "mark to redo"
- **生命周期方法族** (L122-208): cacheInstanceForRedo (登记) / instanceRegistered (成功标记) / instanceDeregister (注销中标记) / instanceDeregistered / removeInstanceForRedo (期望未注册才删)
- **findInstanceRedoData** (L215-225): isNeedRedo 过滤 → 待重做集合
- 订阅对称 (L234-308): cacheSubscriberForRedo / subscriberRegistered / subscriberDeregister / removeSubscriberForRedo / isSubscriberRegistered (NC-1 的 isSubscribed 门控数据源)
- **shutdown 清理** (L339-344)
- TODO 注释 (L47-48): "refactor to extends from AbstractRedoService" — 通用基类在 client/redo/service (config 面复用)
关键设计 (q4): **"登记-确认-注销三态 API"** — 每个操作都有对应 redo 状态方法; remove 只在期望未注册时执行 (防误删已注册数据); 双 Map 隔离实例与订阅。 [模式: 状态 API 化]

### 5. 定时重做 — RedoScheduledTask 的扫描执行

场景: 断线重连后谁把未完成操作补发?
源码路径:
- **RedoScheduledTask** (naming/remote/gprc/redo/RedoScheduledTask.java:34): 构造注入 clientProxy + redoService
- **run 三守卫** (L27-37): ① **connected 检查** (L28-30: 断线直接跳过) ② redoForInstances + redoForSubscribes ③ 异常 catch 不中断
- **redoForInstance switch redoType** (L44-66): REGISTER → processRegisterRedoType (isClientDisabled 守卫) / UNREGISTER → ... — 按类型分发重做
- 调度: NamingGrpcRedoService 构造 (L74-76): **scheduleWithFixedDelay(redoDelayTime)** — 默认常量 DEFAULT_REDO_DELAY_TIME
- 配置: REDO_DELAY_TIME / REDO_DELAY_THREAD_COUNT (L79-83)
关键设计 (q5): **"定时扫描 + 类型分派"** — 周期性 (可配延迟) 扫描 isNeedRedo 数据, 按 RedoType 执行对应重做; connected 门控避免断线空转。 [模式: 定时补发]

### 6. 通用基类 — AbstractRedoService 与 AbstractRedoTask

场景: 重做机制只有命名服务用?
源码路径:
- **AbstractRedoService** (client/redo/service/AbstractRedoService.java:42): implements ConnectionEventListener + Closeable — **redoDataMap (Class → Map<String, RedoData<?>>)** + 通用 onDisConnect 标记 + startRedoTask 模板
- **AbstractRedoTask** (client/redo/service/AbstractRedoTask.java:28): 重做任务基类
- 模块参数: REDO_THREAD_NAME_PATTERN = "com.alibaba.nacos.client.%s.redo" (L17)
- NamingGrpcRedoService TODO 注释指向它 (L48) — **配置模块的未来复用路径**
关键设计 (q6): **"基类抽象 = 双模块统一"** — 命名服务先实现, 基类已为 config 面铺路 (RedoData 泛型化); TODO 注释暴露演进意图。 [模式: 渐进抽象]

### 7. 测试与行为锚

场景: 重做机制的边界行为?
源码路径:
- 测试: NamingGrpcRedoServiceTest / RedoDataTest (client/src/test)
- 语义表注释 (RedoData:100-110): 四组合行为固化 — 官方语义文档
- 日志锚: "Grpc connection disconnect, mark to redo" (NamingGrpcRedoService:102) / "Redo instance operation {}" (RedoScheduledTask:46)
关键设计 (q1): **"语义表注释 = 状态机文档"** — 四行表格即状态机规格, 测试对齐。 [模式: 注释即规格]
