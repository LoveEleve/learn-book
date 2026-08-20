# C-1 CuratorFramework 核心 — 门面之下: 一条连接的重生之路

> 前置: [[Z-7-ClientAPI]] (ZK 客户端 ZooKeeper/ClientCnxn) + [[Z-5-Session]] (会话语义) | 引出: [[C-3-分布式锁]] (ProtectedMode 依赖) + 全部配方 | 对照: ZK 原生客户端 + Redisson
> 🔴 A | 12 KP | [模式: 状态机 + 重试策略 + 事件分发]
> Pass 2 闭环: q1(生命周期) q2(重试) q3(状态机) q4(后台队列)

**读者处境**: `CuratorFrameworkFactory.newClient("host:2181", new ExponentialBackoffRetry(1000, 3))` 一行代码背后发生了什么? 为什么"Session expired"后数据全没了? 异步 `inBackground()` 怎么保证不丢操作? 重试策略怎么选?

### 1. 入口与装配 — Builder 23 个开关与三个"秘密默认值"

场景: 一行 newClient 配出整个客户端; 哪些默认值改了会翻车?
源码路径:
- 三种 newClient 重载全委托 builder (CuratorFrameworkFactory.java:90-138); build() → new CuratorFrameworkImpl(this) (L186-188)
- **Builder 23 个配置字段** (L155-179), 关键默认值: sessionTimeoutMs=**60000** (L60-61, 系统属性 curator-default-session-timeout 可改) / connectionTimeoutMs=**15000** (L62-63) / defaultData=**本机 IP 字节** (L65, 调试友好) / useContainerParentsIfAvailable=**true** (L72, CONTAINER 父节点自动创建) / simulatedSessionExpirationPercent=**100** (L177, 与 M7 会话注入联动)
- connectString 与 ensembleProvider **二选一互斥** (L245-246); zookeeperFactory/threadFactory/runSafeService 三个注入点 (L357-360/L387-390/L517-520)
- buildTemp(): 3 分钟无活动自动关连接 (L70, DEFAULT_INACTIVE_THRESHOLD_MS), 专为 WAN 单次请求设计
关键设计 (q1): **默认值全是"生产安全"取向** — 60s 会话超时兼容慢网络, container 父节点防孤儿; 但 zkClientConfig (L489-492) 与 zookeeperCompatibility (L537-540) 暴露了 5.x 的多 ZK 版本兼容努力。 [模式: Builder 装配]
跨层: [JVM:][System.getProperty 系统属性级默认值]

### 2. 生命周期 — LATENT→STARTED→STOPPED 与 start() 的强制顺序

场景: 为什么 start() 里四步顺序"写死"?
源码路径:
- 三态 AtomicReference (CuratorFrameworkImpl.java:172); start() 单次 CAS, 重复调用抛 IllegalStateException (L284-288)
- **start() 顺序** (L290-323): ① connectionStateManager.start() — 注释 "ordering dependency - must be called before client.start()" (L291) ② 注册 CONNECTED/RECONNECTED 日志监听 ③ client.start() ④ 后台单线程 executor ⑤ ensembleTracker.start()
- close(): closeWithLock() synchronized CAS (L339-343) → CLOSING 事件 → **backgroundOperations.forEach(clearSleep)+drainTo** (L384-390, DelayQueue 必须先 clear 再 drain 的注释) → listeners.clear → 关闭链
关键设计 (q1): **先启状态分发器、后建连接** — 否则连接建立事件到达时无监听者消费, 首连通知丢失; 关闭时先停后台队列防止"关了还提交"的竞态。 [模式: 生命周期状态机]
跨层: [并发:][CAS + synchronized(closeLock) 双层]

### 3. 连接层 — CuratorZookeeperClient 与"每次操作重取句柄"

场景: 断网恢复后, 为什么代码里每次操作都要 getZooKeeper()?
源码路径:
- CuratorZookeeperClient.getZooKeeper(): 未启动抛 IllegalStateException (CuratorZookeeperClient.java:179-183); **背景异常队列抛出** (L74-92, ConnectionState.backgroundExceptions 上限 10 L286-292)
- ConnectionState (client 包, L44): 主 watcher 汇聚点; process() 转发 parentWatchers 链 (L139-162); checkState 映射 6 种 KeeperState (L203-250)
- **Expired → handleExpiredSession → reset()** (L274-284): instanceIndex++ → 旧句柄关闭 → 新 ZooKeeper (L168-177)
- 动态 ensemble: handleNewConnectionString — updateServerListEnabled 时 zooKeeper.updateServerList() 否则整体 reset (L252-272)
关键设计 (q2): **每次 getZooKeeper() 都可能拿到新实例** — 会话过期/集群切换后句柄已换, 缓存旧句柄 = 拿过期会话操作; RetryLoop 注释明示 "it's important to re-get the ZK instance" (RetryLoop.java:44)。背景异常排队抛出而非直接抛, 避免事件线程惊扰。 [模式: 句柄管理]
跨层: [ZK 协议:][KeeperState 六态 (阶段4.3 Z-7)]

### 4. RetryLoop — 同步操作的重试骨架

场景: 为什么 Curator 操作能"自动重试", 而原生客户端要手写循环?
源码路径:
- 三方法契约: shouldContinue/markComplete/takeException (RetryLoop.java:60-120); 规范用法注释 (L36-53)
- RetryLoopImpl.takeException (L60-84): **双门控** — ① policy.allowRetry(exception) 按异常类型 ② allowRetry(retryCount++, elapsedTimeMs, sleeper) 按计数/时间
- callWithRetry 静态工具 + ThreadLocalRetryLoop (RetryLoop.java:79-100, 嵌套重试场景线程本地复用)
- 抽象类非接口的原因: 历史兼容, 变接口有 IncompatibleClassChangeError 风险 (L56-58 注释)
关键设计 (q2): **重试的是"调用"而非"语义"** — ConnectionLoss 重试安全, SessionExpired/NoNode 交给上层; 每个操作自己开 loop, 互不污染。 [模式: 模板方法 + 策略]

### 5. RetryPolicy 家族 — 6 种策略与指数退避的真实公式

场景: ExponentialBackoffRetry(1000, 3) 到底怎么睡? 为什么 29 次封顶?
源码路径:
- 接口双方法 (RetryPolicy); 抽象基类 SleepingRetry: retryCount < n 才睡 (SleepingRetry.java:38-49)
- **6 种具体策略**: ExponentialBackoffRetry / BoundedExponentialBackoffRetry (maxSleep 语义) / RetryNTimes / RetryOneTime (=NTimes(1)) / RetryForever (无限) / RetryUntilElapsed (时间窗)
- **指数公式** (ExponentialBackoffRetry.java:66-73): `sleepMs = baseSleepMs * random(1 << (retryCount+1))` — 注释明示复制自 Hadoop RetryPolicies; **maxRetries>29 静默 pin 到 29** (L75-81); maxSleep pin 上限
关键设计 (q2): **随机化而非纯 2^n** — 避免惊群同步重试 (retry stampede); 29 上限防 int 溢出 (1<<30 边界); RetryOneTime 是"失败一次就睡 N 毫秒"的常见误读纠正 — 它重试 **1 次**。 [模式: 策略族]
跨层: [算法:][指数退避 + 抖动]

### 6. ConnectionStateManager — 单线程事件分发与状态去重

场景: 连接闪断 10 次, 监听器会被刷屏吗?
源码路径:
- 单线程 ExecutorService + ArrayBlockingQueue(25) (ConnectionStateManager.java:60-67); 队列满丢弃最旧 (L235-239)
- addStateChange 去重 (L181-203): 同状态忽略; **负消息** (LOST/SUSPENDED/READ_ONLY) 直发, 首个正消息 → CONNECTED (initialConnectMessageSent, L196-198)
- setToSuspended 双条件 (L159-172); blockUntilConnected wait/notify (L205-224)
- processEvents 主循环 (L241-279): **poll 超时 = sessionTimeout − 已挂起时长**; 超时空转 → checkSessionExpiration
关键设计 (q3): **状态合并成"有意义的最小序列"** — 闪断只产生 SUSPENDED→RECONNECTED 两个事件; 首连单独叫 CONNECTED 让应用区分"首次可用"与"恢复"。 [模式: 单线程事件循环]

### 7. 会话过期注入 — Curator 比 ZK 多做的一件事

场景: 服务器端会话过期了, 客户端却还显示 SUSPENDED — 怎么办?
源码路径:
- checkSessionExpiration (L281-313): SUSPENDED 时长 ≥ adjusted session timeout → **injectSessionExpiration()** (L299, ZK Testable 接口); 上次注入没生效 (instanceIndex 相同) → client.getZookeeperClient().reset() (CURATOR-561)
- sessionExpirationPercent 语义 (CuratorFrameworkFactory.java:449-487): Disconnect 后计时, 超过 negotiated×percent → 模拟过期; **0 = 关闭注入**
- **LOST 但 isConnected()==true → 强制 RECONNECTED** (L263-272, CURATOR-525 race)
关键设计 (q3): **客户端自证过期** — 网络分区时 ZK 事件根本送不过来, 客户端不能靠"等 Expired 事件"识别会话死亡; 用计时器主动注入过期 (测试接口下毒), 让 ephemeral 语义尽早收敛。百分比的由来: 客户端无法精确复刻服务端时钟 (注释 L470-474)。 [模式: 故障注入]
跨层: [ZK 内核:][ExpiryQueue 会话过期 (阶段4.3 Z-5)]

### 8. 连接五态 — CONNECTED/SUSPENDED/RECONNECTED/LOST/READ_ONLY

场景: 两个错误策略 (Standard vs Session) 差在哪?
源码路径:
- 枚举 5 态 + isConnected() 抽象 (framework/state/ConnectionState.java:29-94); LOST 的判定注记: 会话过期/实例重建 (L61-69)
- StandardConnectionStateErrorPolicy: SUSPENDED+LOST 都算错误 (StandardConnectionStateErrorPolicy.java:28-29); SessionConnectionStateErrorPolicy: 仅 LOST (SessionConnectionStateErrorPolicy.java:27-28)
- validateConnection 事件→状态映射 (CuratorFrameworkImpl.java:642-655): Disconnected→suspend / Expired→LOST / SyncConnected→RECONNECTED+unSleep / ConnectedReadOnly→READ_ONLY
关键设计 (q3): **SUSPENDED 是可恢复的 (断网), LOST 是不可恢复的 (会话死)**; Session 策略下 SUSPENDED 不触发配方让位, 避免"短闪断就丢领导权" (C-2 连接联动基础)。 [模式: 状态枚举]

### 9. 后台操作队列 — DelayQueue 上的异步引擎

场景: inBackground() 提交的操作, 断连时去哪了?
源码路径:
- backgroundOperations = DelayQueue (CuratorFrameworkImpl.java:158); 单线程循环 (L786-805)
- queueOperation: synchronized(closeLock) STARTED 才入队 (L597-606)
- 断连处理: performBackgroundOperation — 未连接且 elapsed < connectionTimeout → **sleepAndQueueOperation 睡 1 秒重排** (L851-856, CURATOR-52); 超时 → CONNECTIONLOSS 事件走重试策略 (L824-841)
- **重连唤醒**: validateConnection SyncConnected → unSleepBackgroundOperations 全部 requeue (L858-863, requeueSleepOperation 需 remove/offer 重排序 L576-591)
关键设计 (q4): **延迟队列让"等连接"不阻塞别的操作** — 睡着的操作让出事件循环; 重连一次性唤醒; close 时先 clearSleep 再 drain, 否则 DelayQueue 的 getDelay 会卡住 drain (L384-390 注释)。 [模式: 延迟队列]

### 10. namespace 门面 — 一条路径的三重变换

场景: 多团队共用一个集群, 怎么互不干扰?
源码路径:
- NamespaceImpl: fixForNamespace 前缀拼接 (NamespaceImpl.java:64-88), unfixForNamespace 剥离 (L54-62); **namespace 根懒创建** — ensurePathNeeded 首次操作时 RetryLoop 内 mkdirs (L65-85)
- NamespaceFacadeCache: usingNamespace() 缓存门面 (CuratorFrameworkImpl.java:402-406); nonNamespaceView() 绕过
- 校验: 非法 namespace 构造即抛 (NamespaceImpl.java:36-43)
关键设计 (q4): **路径隔离在客户端做, 服务端无感知**; 懒创建把 namespace 根目录的建造成本摊到首次操作; 事件回传时 unfix 保证回调里看到的是逻辑路径而非物理路径。 [模式: 门面]

### 11. fluent API 与 ProtectedMode — 1283 行的创建门面

场景: `withProtectedEphemeralSequential()` 到底保护什么?
源码路径:
- CuratorFramework 接口 15+ 操作门面 (create/delete/checkExists/getData/setData/getChildren/.../transaction, CuratorFramework.java:76-166)
- CreateBuilderImpl (1283 行): Backgrounding (inBackground 任意步插入) / ACLing / 压缩 / orSetData (幂等创建 L141-153) / withTtl / creatingParentContainersIfNeeded (L249-253)
- **ProtectedMode** (ProtectedUtils.java:53-101): 节点名 = `_c_` + **GUID** + `_` + 原名 (L53-54); 响应丢失后凭 GUID 找回自己创建的节点
关键设计 (q4): **保护模式解决"创建成功但响应丢了"** — 重试创建会拿到不同序号节点, 凭 GUID 前缀幂等恢复; 这是 C-3 锁配方安全释放的前置 (锁节点必须能证明"是我的")。 [模式: fluent + 幂等]

### 12. 事务与动态集群 — multi() 原子提交与 EnsembleTracker

场景: 三个写操作要么全成要么全不, 怎么做到?
源码路径:
- CuratorMultiTransactionImpl.forOperations → RetryLoop 内 zooKeeper.multi() (CuratorMultiTransactionImpl.java:109-115, 195-215); CuratorMultiTransactionRecord 记录+回填
- 后台事务: OperationAndData<CuratorMultiTransactionRecord> 走 M9 队列 (L160-193)
- EnsembleTracker (208): 监控 /zookeeper/config 动态节点, getCurrentConfig() 返回 QuorumVerifier (CuratorFrameworkImpl.java:220-222)
关键设计 (q4): **事务是 ZK multi 的薄封装, 重试语义不变** — 前台重试走 RetryLoop; 事务内操作可混合 create/delete/setData 不同模式。 [模式: 事务记录]
跨层: [ZK 协议:][multi-op (阶段4.3 Z-4)]

## 代码类型
Architecture (客户端框架核心)

## 负面空间 — Curator 框架层刻意不做的事

- **不做连接池/句柄复用**: 单会话单句柄, Expired 即换新 (对比 HikariCP 连接池)
- **不做本地数据缓存**: 缓存是 C-5 配方的职责; 框架层保证"每次操作到达 ZK"
- **不重试"不确定结果"**: RetryLoop 只重试调用; 写操作重试的幂等靠 withProtection/版本号 (C-3)
- **不做会话保活**: 客户端不做心跳, 全委托 ZK 协议层 (对比 Seata S-8 SessionMode)
- **不保证后台操作相对顺序**: DelayQueue 可能重排, 同路径语义由配方自行约束
- **不做 TLS/鉴权默认开启**: auth 需显式 authorization() 配置

→ 引出: 锁配方怎么用顺序节点+保护模式? → C-3 分布式锁
