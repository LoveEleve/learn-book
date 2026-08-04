# RocketMQ 源码学习范围规划

> **基准**: RocketMQ 5.3.1
> **数据源**: RocketMQ 仓库 1495 文件逐包扫描（remoting 305 + common 171 + proxy 162 + client 150 + broker 126 + store 126 + namesrv 11 等）
> **边界**: 聚焦 client + broker + store + namesrv + remoting，按需学习 proxy/auth/filter/controller
> **my-xhs 关联**: RocketMQ 是 my-xhs 的消息队列（Feed 推送/订单状态/笔记索引同步等）

---

## 第 1 层：消息发送 + 存储 + 消费（3 🔴）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| R-1 | **消息发送流程（Producer）** | 🔴 | client/producer + client/impl + client/latency | DefaultMQProducer/DefaultMQProducerImpl/MQProducer/MessageQueueSelector/RequestCallback/RequestResponseFuture/ClientRemotingProcessor/MQClientAPIImpl/CommunicationMode/MQClientManager/MQFaultStrategy/LatencyFaultTolerance/LatencyFaultToleranceImpl | `DefaultMQProducerImpl`（`send`→`sendDefaultImpl`→`tryToFindTopicPublishInfo`+`selectOneMessageQueue`）。**`MQFaultStrategy`**（`client/latency/`，源码验证：用`LatencyFaultTolerance`隔离故障 Broker 一段时间）。三种模式：SYNC/ASYNC/ONEWAY。`BackpressureSendCallBack`背压。`MQClientAPIImpl`远程调用。面试必问 |
| R-2 | **消息存储引擎（CommitLog + ConsumeQueue + IndexFile）** | 🔴 | store | CommitLog/ConsumeQueue/DefaultMessageStore/AllocateMappedFileService/CommitLogDispatcher/MappedFile/MappedFileQueue/FlushManager/PutMessageLock/ReputMessageService/ConcurrentReputMessageService/HAService/AutoSwitchHAService/DefaultHAService/MessageStoreConfig/IndexFile/IndexHeader | **CommitLog**（`MappedFileQueue`+`FlushManager`+`PutMessageLock`）。**ConsumeQueue**（`putMessagePositionInfo(offset, size, tagsCode)`）。**`DefaultMessageStore`**（`ReputMessageService`分发+`HAService` SPI加载）。**`MappedFile`**接口（`appendMessage`/`flush`）。**`IndexFile`**（源码验证：`hashSlotNum`+`hashSlotSize=4`+`IndexHeader`+文件大小=header+slots+indexes——哈希槽+索引条目，通过 key hash 定位）。`MessageStoreConfig`（CommitLog 1GB/ConsumeQueue 30 万条/ASYNC_MASTER/ASYNC_FLUSH）。面试必问 |
| R-3 | **消息消费流程（Consumer）** | 🔴 | client/consumer + client/impl/consumer | DefaultMQPushConsumer/DefaultLitePullConsumer/DefaultMQPullConsumer/AllocateMessageQueueStrategy/AllocateMessageQueueAveragely/MessageListenerConcurrently/MessageListenerOrderly/DefaultMQPushConsumerImpl/PullMessageService/RebalanceImpl/RebalancePushImpl/ConsumeMessageService/ConsumeMessagePopService | `DefaultMQPushConsumerImpl`（`RebalancePushImpl`+`ConsumeMessagePopService`5.x Pop）。PushConsumer vs LitePullConsumer。**`AllocateMessageQueueAveragely`**（源码验证：`cidAll.indexOf(currentCID)`获取索引+`mod = mqAll.size() % cidAll.size()`余数+余数分给前几个消费者——平均分配）。`MessageListenerConcurrently`/`MessageListenerOrderly`。面试必问 |

## 第 2 层：NameServer + 通信（2 🔴）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| R-4 | **NameServer 路由注册/发现** | 🔴 | namesrv | NamesrvController/NamesrvStartup/RouteInfoManager/KVConfigManager/processor | `RouteInfoManager`（源码验证：`Map<String, Map<String, QueueData>> topicQueueTable`=ConcurrentHashMap(1024)+`Map<String, BrokerData> brokerAddrTable`=ConcurrentHashMap(128)+`registerBroker`/`unregisterBroker`/`pickupTopicRouteData`）。Producer/Consumer 通过 NameServer 查找 Broker 路由。**NameServer vs ZooKeeper**：无状态+互相独立。面试必问 |
| R-5 | **远程通信（Netty 自定义协议）** | 🔴 | remoting | RemotingCommand/NettyRemotingClient/NettyRemotingServer/NettyRemotingAbstract/InvokeCallback/ChannelEventListener/NettyRequestProcessor | `RemotingCommand`自定义通信协议（requestCode/extFields/body）。**`NettyRemotingClient extends NettyRemotingAbstract`**（源码验证：`invokeSync`→`invokeSyncImpl`+`invokeAsync`+`invokeOneway`三种调用+NioSocketChannel）。**`NettyRemotingServer`**（源码验证：`start()`+Epoll/NIO 选择+`registerProcessor(requestCode, processor, executor)`注册处理器+`registerDefaultProcessor`）。面试会问 |

## 第 3 层：Broker + 特性（5 🟡）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| R-6 | **Broker 核心架构 + 请求处理器** | 🟡 | broker + broker/processor | BrokerController/BrokerStartup/LongPollingService/failover/SendMessageProcessor/PullMessageProcessor/AckMessageProcessor/EndTransactionProcessor/PeekMessageProcessor/ClientManageProcessor/ConsumerManageProcessor/AdminBrokerProcessor/NotificationProcessor/ChangeInvisibleTimeProcessor | `BrokerController`（`initialize()`多步初始化）。**`SendMessageProcessor`**（源码验证：`processRequest`→`buildMsgContext`+`executeSendMessageHookBefore`+`sendMessage`/`sendBatchMessage`单条/批量）。**`PullMessageProcessor`**（源码验证：`processRequest`+`brokerAllowSuspend`挂起+`brokerAllowFlowCtrSuspend`流控+`PullMessageRequestHeader`解码）。`LongPollingService`/`PopLongPollingService`长轮询。10+ 请求处理器。面试会问 |
| R-7 | **事务消息** | 🟡 | client + broker/processor + broker/transaction | TransactionMQProducer/TransactionListener/LocalTransactionState/EndTransactionProcessor/TransactionalMessageService/TransactionalMessageBridge | 半消息机制（源码验证）：`TransactionalMessageService.prepareMessage()`存储半消息→Producer 本地事务→`EndTransactionProcessor.processRequest()`处理 commit/rollback→委托`commitMessage(requestHeader)`。**回查机制**（接口注释："Traverse uncommitted/unroll back half message and send check back request to producer"——遍历未提交半消息向 Producer 发回查）。面试会问 |
| R-8 | **顺序消息 + 延迟消息** | 🟡 | client + broker/schedule + store | MessageQueueSelector/MessageListenerOrderly/ConsumeQueue/ScheduleMessageService/DelayLevel | **顺序消息**：`MessageQueueSelector`保证同一 key 进同一队列+`MessageListenerOrderly`顺序消费。**延迟消息**：`ScheduleMessageService extends ConfigManager`（源码验证：`ConcurrentSkipListMap<Integer,Long> delayLevelTable`延迟级别表+`ScheduledExecutorService deliverExecutorService`定时扫描+`delayLevel2QueueId(level)=level-1`级别转队列ID+`deliverPendingTable`投递待处理）。在`broker/schedule/`包。面试会问 |
| R-9 | **高可用（Master/Slave + DLedger + JRaft + Controller）** | 🟡 | broker/dledger + store/dledger + controller | DLedgerCommitLog/DLedgerServer/ReplicasManager/syncStateSet/ControllerManager/ControllerStartup/BrokerHeartbeatManager/BrokerHousekeepingService/DLedgerController/JRaftController/ElectPolicy | **DLedger**（`DLedgerCommitLog extends CommitLog`+`DLedgerServer` Raft）。**Controller 5.x**（`ReplicasManager` syncStateSet+角色切换）。**Controller 两种 Raft 实现**（源码验证：`DLedgerController`+`DLedgerControllerStateMachine` AND `JRaftController`+`JRaftControllerStateMachine`——之前漏了 JRaft）。`BrokerHeartbeatManager`心跳管理+`BrokerHousekeepingService`清理+`ElectPolicy`选主策略。面试会问 |
| R-10 | **Proxy（5.x）+ 消息过滤 + 分级存储** | 🟡 | proxy + filter + tieredstore | ProxyMode/ProxyStartup/RemotingProxyOutClient/grpc/remoting/SelectorParser/SqlFilter/FilterFactory/FilterSpi/BinaryExpression/ComparisonExpression/FlatCommitLogFile/FlatConsumeQueueFile/FlatFileStore/MessageStoreDispatcher/MessageStoreFetcher/IndexService | **Proxy**（两种协议 grpc+remoting+ProxyMode）。**消息过滤底层**（源码验证：`SelectorParser`JavaCC 生成+`expression`体系 BinaryExpression/ComparisonExpression/EvaluationContext+`SqlFilter`+`FilterFactory`/`FilterSpi`）。**5.x 分级存储**（`tieredstore` 44 文件：`FlatCommitLogFile`/`FlatConsumeQueueFile`/`FlatFileStore`扁平化文件+`core`MessageStoreDispatcher/Fetcher+`index`IndexService）|

---

## 淘汰清单

| 子模块 | 文件数 | 理由 |
|---|---|---|
| auth（52 文件） | 52 | 认证授权，按需 |
| acl（20 文件） | 20 | ACL，按需 |
| openmessaging（16 文件） | 16 | OpenMessaging 规范，非主流 |
| container（10 文件） | 10 | 容器化，按需 |
| srvutil（4 文件） | 4 | 工具 |
| example（57 文件） | 57 | 示例 |
| test/dev/docs/distribution/style/bazel | — | 测试/文档/构建 |
| tieredstore | — | 分级存储，按需 |
| tools | — | 运维工具 |
| coldctr | — | 冷数据控制，按需 |

---

## 统计

| | 数量 |
|---|---|
| 🔴 核心域 | **5** |
| 🟡 重要域 | **5** |
| 总计 | **10 域** |
| 预计产出文章 | 5 篇（🟡 子域在对应 🔴 中附带）|
| 核心子模块覆盖 | client(150) + broker(126) + store(126) + namesrv(11) + remoting(305) + common(171) = 889 文件 |

## 与 my-xhs 的关联

| my-xhs 场景 | RocketMQ 关联 |
|---|---|
| Feed 推送 | Producer 发送 FEED_TOPIC → Consumer 消费 |
| 订单状态变更 | 事务消息保证一致性 |
| 笔记索引同步 | Canal → RocketMQ → Consumer → ES |
| 消息幂等 | Consumer 端 MessageIdempotentHelper |

## 与 Spring Cloud Alibaba 的关联

| Alibaba 域 | RocketMQ 关联 |
|---|---|
| A-10 RocketMQ Stream Binder | RocketMQMessageChannelBinder 用 client 模块的 Producer/Consumer |
