# RM-5 Broker 启动 — 装配枢纽与生命周期

> 前置: [[RM-1-remoting]] [[RM-2-存储底层]] [[RM-3-commitlog]] [[RM-4-延迟]] | 引出: [[RM-6-过滤]] [[RM-7-生产]] [[RM-8-消费]] [[RM-12-HA]] [[RM-14]] | 对照: [[R-20-server]] (Redis server 骨架)
> 🔴 A | 6 KP | [模式: 三阶段装配 + 处理器注册 + 定时任务 + 生命周期]
> Pass 2 闭环: q1(启动链) q2(三阶段) q3(处理器) q4(定时) q5(5.x 新面) q6(关闭+测试)

**读者处境**: broker 启动时都干什么? 48 个处理器注册给谁? 10 个定时任务保什么? 这篇拆 BrokerController: 三阶段装配、双 remoting、命令面注册、周期调度。

### 1. 启动链 — main → initialize → start

场景: broker 进程怎么活起来?
源码路径:
- **main** (BrokerStartup:51) → createBrokerController (L245: 参数装配 -c/-n/环境 → 三配置) → **initialize() 失败 → exit(-1)** → start
- **start** (L1705): shouldStartTime (**disappearTimeAfterStart 默认 -1 禁用**, 启动防风暴) → brokerOuterAPI → **startBasicService** (store→timer→**replicasManager**→remoting (startLatch 同步)→fast→**pop 三服务**→ackRevive) → registerBrokerAll + **10s 后每 10-60s 重注册** (namesrv 周期)
- **隔离面**: slave-act-master → isIsolated (从库代主不注册)
关键设计 (q1): **装配失败即亡** (信任启动期校验); namesrv 周期注册保路由新鲜。[模式: 启动管线]

### 2. 三阶段初始化 — Metadata/Store/Recover

场景: initialize 内部怎么排?
源码路径:
- **initializeMetadata** (L773): **6+1 个 configManager.load** (topic/queueMapping/offset/subscriptionGroup/filter/order + topicConfig 特殊路径标注)
- **initializeMessageStore** (L783): 双实现 (Default/**RocksDB**) + **DLedger 角色注册** (RM-12) + **存储插件链** (MessageStoreFactory) + **CalcBitMap 分发前置** (RM-6) + **TimerWheel** (5.x)
- **recoverAndInitService** (L849): **ReplicasManager fenced** (5.x controller) → store.load → schedule.load (RM-4) → 插件 → 服务链 (remoting→资源→处理器→定时→**事务 (SPI 加载 TransactionalMessageBridge, RM-10)**→ACL→RPC→**认证管线 (Authorization/AuthenticationPipeline, RM-13)**→TLS 热加载)
关键设计 (q2): **恢复序严格** (store→schedule→插件 — 依赖序); 服务注册链后置 (恢复成功才开面)。[模式: 三阶段装配]

### 3. 处理器注册 — 26 请求码 × 双服务

场景: 命令面怎么挂到网络层?
源码路径:
- **双服务**: remotingServer (主端口) + **fastRemotingServer (listenPort-2)**: 20 码 (发送/ACK/心跳/查询/END_TRANSACTION — **高频生产+消费管理通道**, 非纯发送); 主端口独有 POP 拉取族
- **46 注册** (python 精确计数): **20 码 × 双服务 + 6 码单注册** (PULL/LITE_PULL/PEEK/POP/NOTIFICATION/POLLING_INFO — **POP 族仅主端口**, 无 fast 面); 26 唯一码覆盖 SEND/ACK/REPLY/QUERY/心跳/管理
- **分组线程池**: sendMessageProcessor / clientManageProcessor (独立 heartbeatExecutor) / pull / query / admin — 各绑独立 executor (RM-1); **5.x POP 族** (popMessageProcessor + PopLongPollingService/PopBufferMergeService/QueueLockManager, RM-8 交叉)
关键设计 (q3): **发送流量与主面分离** (fast 端口); 按功能分组隔离。[模式: 命令面注册]

### 4. 定时任务 — 8 核心 + 周期注册

场景: 后台周期保什么?
源码路径:
- **8 核心** (L608-731): brokerStats.record (**每日零点对齐**, computeNextMorningTimeMillis) / offset+filter+order persist (flushConsumerOffsetInterval) / **protectBroker = 慢消费者自动禁用** (fallBehind > ConsumerFallbehindThreshold → subscriptionGroupManager.disableConsume, L1202-1220) / printWaterMark / **dispatchBehindBytes 积压监控** / **syncAll 主从全量** + syncTimerCheckPoint / printMasterAndSlaveDiff
- **条件任务** (L732-770): fetchNameServerAddr / updateNamesrvAddr (按周期配置) / controller 模式
- **namesrv 注册** (start L1724): 10s 初 + 10-60s 周期 (registerNameServerPeriod 钳制)
关键设计 (q4): **持久化保崩溃窗口 + 主从保一致 + 积压保可观测**; 周期钳制防风暴。[模式: 周期调度]

### 5. 5.x 新面 — controller/RocksDB/插件/TimerWheel

场景: 5.x 装配多了什么?
源码路径:
- **fast 端口** (发送专用, L478-486)
- **ReplicasManager fenced=true** (L855-858 — 初始隔离, controller 确认后解封, RM-14)
- **RocksDBMessageStore + CQ 双写** (L787-794, RM-16)
- **存储插件链** (MessageStoreFactory.build L809) + **附件插件** (brokerAttachedPlugins L867)
- **TimerWheel** (L813-819, Timer 消息)
关键设计 (q5): **5.x 装配即插即用** (RocksDB/插件/Timer 全在装配期注入)。[模式: 插件化装配]

### 6. 关闭面与测试

- shutdown 链: **shutdownBasicService (unregisterBrokerAll → 双 remoting → metrics×2 → housekeeping → pullRequestHold)** → scheduledFutures 取消 → brokerOuterAPI; 收尾持久化 (offset/filter/order/schedule)
- JVM shutdownHook (BrokerStartup:224)
- 测试: testBrokerRestart (重启幂等) / testHeadSlowTimeMills (心跳慢任务)

### 负面空间 — Broker 启动刻意不做的事

- **不做热配置全面化**: 部分配置重启生效 (插件/RocksDB 切换)
- **不做灰度启动**: 无多版本共存 (对照 5.x 从库代主是另路)
- **不做启动自检深度**: 依赖 load 返回值短路, 无健康探针内建
- **不做模块热插拔**: 插件加载期固定, 运行期不改

→ 引出: 过滤怎么挂在发送/消费链? → [[RM-6-过滤]]
