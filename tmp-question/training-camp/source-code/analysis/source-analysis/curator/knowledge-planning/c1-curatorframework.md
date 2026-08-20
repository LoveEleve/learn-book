# C-1 CuratorFramework 核心+客户端层 — 知识规划 (KP)

> 域级: 🔴 A | 模块: curator-client (43 文件) + curator-framework (185 文件)
> 日期: 2026-08-15 | 版本: 5.8.0
> 来源: CuratorFrameworkFactory (675) / CuratorFrameworkImpl (881) / CuratorZookeeperClient (425) / ConnectionState-client (293) / ConnectionStateManager (329) / RetryLoopImpl (85) / SleepingRetry (52) / ExponentialBackoffRetry (82) / NamespaceImpl (93) / CreateBuilderImpl (1283) / CuratorMultiTransactionImpl / FailedDeleteManager (33) / ProtectedUtils (153) / EnsembleTracker (208)

## 一、机制提取 (逐源)

### M1 入口与装配: CuratorFrameworkFactory.Builder
- 工厂三种 newClient 重载 (CuratorFrameworkFactory.java:90-138), 全部委托 builder 链
- **Builder 23 个配置字段** (L155-179): ensembleProvider/sessionTimeoutMs (默认 60000, L60-61)/connectionTimeoutMs (默认 15000, L62-63)/retryPolicy/threadFactory/namespace/authInfos/defaultData (默认本机 IP 字节)/compressionProvider (默认 Gzip)/aclProvider/canBeReadOnly/useContainerParentsIfAvailable (默认 true)/connectionStateErrorPolicy (默认 Standard)/schemaSet/waitForShutdownTimeoutMs/runSafeService/simulatedSessionExpirationPercent (默认 100)/zkClientConfig/zookeeperCompatibility
- connectString() → FixedEnsembleProvider (L251-254); ensembleProvider() 二选一互斥 (L245-246 注释)
- build() → new CuratorFrameworkImpl(this) (L186-188); buildTemp() → CuratorTempFrameworkImpl (L212-214, 3 分钟无活动自动关连接 L70)

### M2 生命周期状态机: CuratorFrameworkImpl
- 三态 LATENT→STARTED→STOPPED (CuratorFrameworkState), state AtomicReference (L172); start() CAS LATENT→STARTED, 重复 start 抛 IllegalStateException (L284-288)
- **start() 顺序敏感** (L290-323): ① connectionStateManager.start() (注释 "ordering dependency - must be called before client.start()") ② 注册日志监听 ③ client.start() ④ executorService 单线程后台循环 ⑤ ensembleTracker.start()
- close(): closeWithLock() synchronized 下 CAS STARTED→STOPPED (L339-343) → CLOSING 事件广播 → executorService.shutdownNow + awaitTermination(maxCloseWaitMs) → ensembleTracker.close → **backgroundOperations.forEach(clearSleep) + drainTo 逐个 closeOperation** (L384-390, 注释: DelayQueue 必须先行 clear) → listeners.clear → connectionStateManager.close → client.close
- 状态校验: checkState() (CuratorFrameworkBase) — 操作前必须 STARTED

### M3 后台操作队列: backgroundOperationsLoop
- backgroundOperations = **DelayQueue**<OperationAndData> (L158) + forcedSleepOperations = LinkedBlockingQueue (L159)
- queueOperation(): synchronized(closeLock) 内 STARTED 才 offer, 否则 closeOperation (L597-606)
- 单线程循环 backgroundOperationsLoop() (L786-805): take → performBackgroundOperation
- performBackgroundOperation (L807-846): 不需连接或已连接 → 直接执行; 未连接 → client.getZooKeeper() 触发连接, elapsed < connectionTimeout → **sleepAndQueueOperation 睡 1 秒重排** (L851-856, CURATOR-52 注释); 超时 → 构造 CONNECTIONLOSS 事件走 checkBackgroundRetry
- unSleepBackgroundOperations(): 重连成功唤醒全部沉睡操作 (L858-863, validateConnection SyncConnected 分支)
- processBackgroundOperation (L476-503): 事件回传双路径 — 可重试 (allowRetry) → queueOperation; 有 callback → sendToBackgroundCallback; 否则 processEvent
- checkBackgroundRetry (L690-737): allowRetry(count, elapsed) 决定; 耗尽 → retriesExhausted callback → validateConnection(codeToState) → logError
- **关闭竞态处理**: requeueSleepOperation (L576-591) — remove/offer 重排序 (DelayQueue 特性注释)

### M4 客户端连接层: CuratorZookeeperClient + ConnectionState (client 包)
- CuratorZookeeperClient: ZK 句柄唯一管理者; getZooKeeper() 前置 started 校验 (L179-183); isConnected() 门控 (L210-212); newRetryLoop() (L190-192); blockUntilConnectedOrTimedOut 内部 1s 分片等待 (L399-424)
- ConnectionState (client): implements Watcher — **主 watcher 汇聚点** (L44); process() 转发 parentWatchers (L139-162); checkState 映射 KeeperState→isConnected (L203-250): SyncConnected/ConnectedReadOnly→true, Disconnected/AuthFailed/Expired→false
- **Expired → handleExpiredSession → reset()** (L274-284, L168-177): instanceIndex++ → closeAndReset → 新 ZooKeeper
- backgroundExceptions 队列 (上限 10, L286-292): 后台异常排队, getZooKeeper() 时抛出
- ensemble 动态切换: checkNewConnectionString → handleNewConnectionString — updateServerListEnabled → zooKeeper.updateServerList() 否则 reset (L252-272)

### M5 RetryLoop: 同步重试骨架
- RetryLoop 抽象类 (121 行): shouldContinue/markComplete/takeException 三方法 + callWithRetry 静态工具 (L79-100, ThreadLocalRetryLoop 线程本地复用)
- RetryLoopImpl (L29-84): takeException → ① policy.allowRetry(exception) 类型门控 ② allowRetry(retryCount++, elapsed, sleeper) 计数门控 → 不通过 rethrow
- 规范用法注释: 每次循环**重新 getZooKeeper()** (RetryLoop.java:44)

### M6 RetryPolicy 体系: 接口 + 6 策略
- RetryPolicy 接口双方法: allowRetry(Exception) + allowRetry(int, long, RetrySleeper)
- **SleepingRetry 抽象** (L26-52): retryCount < n 才 sleep+true
- **6 种具体策略**: ExponentialBackoffRetry (L30-82: 公式 **baseSleepMs * random(1 << (retryCount+1))** 复制自 Hadoop RetryPolicies L66-67, maxSleep pin 上限 L68-72, maxRetries 上限 29 pin L33/L75-81) / BoundedExponentialBackoffRetry (maxSleep 语义) / RetryNTimes / RetryOneTime (=RetryNTimes(1)) / RetryForever (无限) / RetryUntilElapsed (时间窗)

### M7 ConnectionStateManager: 连接状态分发器 (framework/state)
- 单线程 ExecutorService + **ArrayBlockingQueue(25)** eventQueue (L60, QUEUE_SIZE 系统属性可调 L56-59)
- addStateChange (L181-203): 状态去重 (currentConnectionState); **负消息** (LOST/SUSPENDED/READ_ONLY) 直发; 首个正消息 → CONNECTED (initialConnectMessageSent L196-198)
- setToSuspended (L159-172): 仅非 LOST/SUSPENDED 时
- processEvents 主循环 (L241-279): **poll 超时 = sessionTimeout - 已挂起时长** (L244-250); 超时且 sessionExpirationPercent>0 → checkSessionExpiration
- **会话过期注入** (L281-313): SUSPENDED 超时 → getTestable().injectSessionExpiration() (CURATOR-405); 上次注入无效 (同 instanceIndex) → client.getZookeeperClient().reset() (CURATOR-561); **LOST 但 isConnected → 强制 RECONNECTED** (L263-272, CURATOR-525 race)
- blockUntilConnected (L205-224): synchronized wait/notify
- sessionExpirationPercent 语义 (CuratorFrameworkFactory.java:449-487 注释): Disconnect 后计时, 超过 negotiated session × percent → 模拟过期

### M8 连接状态 5 态与错误策略
- ConnectionState 枚举 (framework/state/ConnectionState.java:29-94): CONNECTED/SUSPENDED/RECONNECTED/LOST/READ_ONLY + isConnected() 抽象
- StandardConnectionStateErrorPolicy: SUSPENDED+LOST 为错误态 (L28-29); SessionConnectionStateErrorPolicy: 仅 LOST (L27-28)
- validateConnection (CuratorFrameworkImpl L642-655): Disconnected→suspendConnection / Expired→LOST / SyncConnected→checkNewConnection+RECONNECTED+unSleep / ConnectedReadOnly→READ_ONLY

### M9 namespace 门面
- NamespaceImpl (L31-93): fixForNamespace 路径前缀 (L64-88, ZKPaths.fixForNamespace), unfixForNamespace 剥离 (L54-62); **ensurePathNeeded 懒创建 namespace 根** (L65-85, RetryLoop.callWithRetry 内 ZKPaths.mkdirs)
- NamespaceFacadeCache: usingNamespace() 按名缓存门面 (CuratorFrameworkImpl L402-406); nonNamespaceView()

### M10 fluent API 面 (api/ 88 文件)
- CuratorFramework 接口 15+ 操作门面 (create/delete/checkExists/getData/setData/getChildren/getACL/setACL/reconfig/getConfig/sync/watches/transaction/transactionOp...)
- CreateBuilderImpl (1283): ProtectedMode/Backgrounding/ACLing/压缩/orSetData(idempotent)/TTL/creatingParentContainersIfNeeded/withProtectedEphemeralSequential (L243-258)
- **ProtectedMode** (ProtectedUtils L53-101): 保护节点 = "_c_" + GUID + "_" + 原节点名 — create 响应丢失后可凭 GUID 幂等恢复 (C-3 锁配方依赖)
- **FailedDeleteManager** (33 行): guaranteed 删除失败 → 重连后后台重删; FailedRemoveWatchManager 同理

### M11 事务面 (api/transaction 15 文件)
- CuratorMultiTransactionImpl: forOperations (L109-115) → 前台 RetryLoop 内 zooKeeper.multi() (L195-215); 后台 → OperationAndData<CuratorMultiTransactionRecord> 执行 (L160-193)
- CuratorMultiTransactionRecord: 记录 ops + setResult 回填; CuratorTransactionResult (path/type/stat/result)
- 同 ZK multi 语义: 原子提交, 全部成功或全部失败

### M12 EnsembleTracker (208)
- 监控 /zookeeper/config 动态 ensemble 变更 (builder.withEnsembleTracker 默认 true, Factory L72); getCurrentConfig() QuorumVerifier

## 二、聚合与分级 (P1/P2/P3)

| 机制 | 级别 | 理由 |
|---|---|---|
| M2 生命周期 CAS + start 顺序 | P1 | 一切配方的前提; 顺序错误 = 事件丢失 |
| M3 后台操作 DelayQueue + 重连唤醒 | P1 | 异步面核心; 与 M7 重连协同 |
| M4 连接状态机 + reset/Expired 处理 | P1 | 会话过期语义的源头 |
| M5/M6 RetryLoop + 6 策略 | P1 | 面试高频 (重试策略选择); 数学公式精确 |
| M7 ConnectionStateManager 单线程分发 | P1 | 事件串行化 + 会话过期注入是 Curator 独有设计 |
| M8 5 态 + 错误策略 | P1 | recipes 连接处理的基础 (C-2~C-8 复用) |
| M1 Builder 装配 | P2 | 配置面; 默认值须精确 |
| M9 namespace | P2 | 多租户路径隔离; 面试中频 |
| M10 fluent API/ProtectedMode | P2 | 接口面大; ProtectedMode 为 C-3 前置 |
| M11 事务 | P2 | ZK multi 封装 |
| M12 EnsembleTracker | P3 | 动态集群变更, 低频 |

## 三、依赖与教学顺序 (域内)

C-1 内部: M1 装配 → M2 生命周期 → M4 连接层 → M5/M6 重试 → M7 状态分发 → M8 状态语义 → M3 后台操作 → M9 namespace → M10 fluent/保护 → M11 事务 → M12 tracker
教学: 先"怎么建/怎么启", 再"连不上怎么办" (重试+状态), 再"异步怎么跑" (后台队列), 最后"面" (namespace/fluent/事务)。

## 四、负面空间 (刻意不做)

- **不做连接池**: 单 ZooKeeper 句柄管理 (HandleHolder), 会话不复用
- **不做协议层**: 全部委托 ZK 客户端 (watcher/序列化/传输)
- **不做本地缓存**: 每次操作直连 ZK (缓存是 C-5 配方的事)
- **不做写事务重试语义保证**: RetryLoop 重试的是"调用"不是"提交结果" (幂等由配方 withProtection/版本号负责)
- **不自动重连会话**: Expired 后旧会话不可恢复, reset 开新会话 (LOST 语义)
- **不保证后台操作顺序**: DelayQueue 重排, 同路径操作靠用户/配方约束

## 五、时空溯源线索

- 本地 git 浅克隆无历史; 代码内证据: CURATOR-52 (连接超时重排), CURATOR-405 (会话过期注入), CURATOR-525 (LOST race 强制 RECONNECTED), CURATOR-561 (注入失效 reset), CURATOR-724 (latch 重建)
- @since 标记: waitForShutdownTimeoutMs (4.0.2), connectionStateListenerManagerFactory (4.2.0), simulatedSessionExpirationPercent (5.0), zkClientConfig (5.1.1)
- RetryLoop 类注释: 抽象类而非接口 "for historical reasons... risk IncompatibleClassChangeError" (RetryLoop.java:56-58)
