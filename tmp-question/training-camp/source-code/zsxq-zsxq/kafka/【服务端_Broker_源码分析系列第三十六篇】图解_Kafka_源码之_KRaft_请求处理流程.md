大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端** **KRaft 模块初探**」，了解了 Kafka 中「**KRaft**」模块部署、「**KRaft**」算法细节原理剖析，我们接着来深度聊聊「**Kafka 服务端 KRaft 请求处理流程**」，看看 Kafka Kraft 模块请求是如何处理的。

![](https://article-images.zsxq.com/FiC-tpdbCwdLiRO2cMHKSjCv7Zyd)

## **01 总体概述**

「**KRaft**」模块中存在两个角色：「**Controller**」、「**Broker**」。

我们将「**Broker**」角色的节点称为 「**Broker 节点**」，将「**Controller**」角色的节点称为 「**Controller 节点**」。「**KRaft**」模式下的 「**Broker 节点**」与 Zookeeper 模式的 Broker 基本一致，之前选举「**Controller 节点**」 以及 「**分区 Leader 副本**」都是借助 Zookeeper 来实现的。

而在 「**KRaft**」模式下「**Controller 节点**」会实现如下功能：

1.  「**Controller 节点**」之间都使用 「**Raft**」算法来实现一个「**强一致**」的分布式存储系统，复制存储 Kafka 中的元数据，功能类似 Zookeeper，后面将 「**Controller 节点**」组成的集群称为 「**Raft**」集群。
2.  负责维护和管理 Kafka 集群中的「**主题**」、「**Broker**」等，协同完成创建主题的工作、监控 Broker 节点并进行故障转移等。

> KRaft 模块的节点可以同时启动 Controller、Broker 两种角色，为了方便理解，这里假设 KRaft 模块的一个节点只启动一个角色，该节点可以是 Controller、也可以是 Broker。

##   
**02 初探 KRaft 请求处理流程**

在上一篇 [【服务端 Broker 源码分析系列第三十五篇】图解 Kafka 源码之 KRaft 模块初探实现原理](https://articles.zsxq.com/id_7fp9s8us807i.html) 中，我带大家简单的剖析了「**KRaft**」模块来启动 Kafka 集群的方式，解析来我们重点剖析「**KRaft**」模块的启动流程。

在「**KRaft**」模式下，内部是使用 「**KafkaRaftServer**」来启动 Kafka 服务的。

本文涉及的源码：

「**KafkaRaftManager**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/raft/RaftManager.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/raft/RaftManager.scala)

「**KafkaRaftServer**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/KafkaRaftServer.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/KafkaRaftServer.scala)

「**ControllerServer**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/ControllerServer.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/ControllerServer.scala)

「**BrokerServer**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/BrokerServer.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/BrokerServer.scala)

「**KafkaRaftClient**」类源码在 Kafka 源码包的 raft 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/raft/src/main/java/org/apache/kafka/raft/KafkaRaftClient.java](https://github.com/apache/kafka/blob/3.0.0/raft/src/main/java/org/apache/kafka/raft/KafkaRaftClient.java)

「**ControllerApis**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/ControllerApis.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/BrokerServer.scala)

「**KafkaNetworkChannel**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/raft/KafkaNetworkChannel.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/raft/RaftManager.scala)

KafkaRaftServer 类关系属性如下：

![](https://article-images.zsxq.com/FgCQob9hgwGH4lKmrcTV_Twzjm7i)

## **2.1 KafkaRaftServer#starup 启动**

先来剖析下 KafkaRaftServer 的启动方法都做了什么，源码如下：

override def startup(): Unit = {

Mx4jLoader.maybeLoad()

// 启动 Raft 算法实现类，内部启动 KafkaRaftManager 的 netChannel 和 raftIoThread 线程，用来处理 Raft 请求

raftManager.startup()

// 启动 Controller 实例（如果存在的话）

controller.foreach(\_.startup())

// 启动Broker

broker.foreach(\_.startup())

// 注册 Kafka 应用信息到 MetricRegistry 中

AppInfoParser.registerAppInfo(Server.MetricsPrefix, config.brokerId.toString, metrics, time.milliseconds())

// 打印 KafkaBroker 启动信息

info(KafkaBroker.STARTED\_MESSAGE)

}

通过上面的源码，可以看出 [KafkaRaftServer#startup](http://kafkaraftserver/#startup) 方法启动了以下组件：

1.  **KafkaRaftManager**：这是 Raft 算法实现类，内部会启动 **KafkaRaftManager** 的 netChannel 和 raftIoThread 线程，用来处理 Raft 请求。
2.  **BrokerServer**：这是负责实现 Broker 角色服务组件类，其属性 [process.roles](http://process.roles/) 指定了 Broker 角色时才被创建，处理消息类请求，例如消息的生产消费，其供外部请求的端口由配置 listeners 与 [controller.listener.names](http://controller.listener.names/) 的差集决定，内部会启动 **BrokerServer** 的 socketServer、KafkaScheduler、logManager、alterIsrManager、groupCoordinator 等组件。
3.  **ControllerServer**：这是负责实现 Controller 角色服务组件类，其属性 [process.roles](http://process.roles/) 指定了 Controller 角色时才被创建，处理元数据类请求，包括 topic 创建删除等，由配置 [controller.listener.names](http://controller.listener.names/) 指定供外部请求的端口等信息，内部会启动 **ControllerServer** 的 socketServer，提供 Controller 节点的网络服务。

## **2.2 KafkaManager#starup 启动**

从上一步得出 **KafkaRaftManager** 组件初始化的触发点在 **KafkaRaftServer** 实例的创建过程中，可以看到其初始化过程中的关键处理如下：

class KafkaRaftManager\[T\](

metaProperties: MetaProperties,

config: KafkaConfig,

recordSerde: RecordSerde\[T\],

topicPartition: TopicPartition,

topicId: Uuid,

time: Time,

metrics: Metrics,

threadNamePrefixOpt: Option\[String\],

val controllerQuorumVotersFuture: CompletableFuture\[util.Map\[Integer, AddressSpec\]\]

) extends RaftManager\[T\] with Logging {

// raft 配置

private val raftConfig \= new RaftConfig(config)

// 线程前缀

private val threadNamePrefix \= threadNamePrefixOpt.getOrElse("kafka-raft")

private val logContext \= new LogContext(s"\[RaftManager nodeId=${config.nodeId}\] ")

this.logIdent = logContext.logPrefix()

// 定时器

private val scheduler \= new KafkaScheduler(threads = 1, threadNamePrefix + "-scheduler")

scheduler.startup()

// 创建数据目录

private val dataDir \= createDataDir()

// 副本日志

override val replicatedLog: ReplicatedLog = buildMetadataLog()

// 构建底层网络通信组件

private val netChannel \= buildNetworkChannel()

// 构建 raft 集群客户端

override val client: KafkaRaftClient\[T\] = buildRaftClient()

// 实例化 raftIoThread 线程用来不断进行本地网络通信客户端请求响应处理

private val raftIoThread \= new RaftIoThread(client, threadNamePrefix)

def startup(): Unit = {

// 更新投票者的端点地址信息，使用RaftConfig中的地址信息更新

val voterAddresses: util.Map\[Integer, AddressSpec\] = raftConfig.quorumVoterConnections

for (voterAddressEntry <- voterAddresses.entrySet.asScala) {

voterAddressEntry.getValue match {

// 如果地址类型是 InetAddressSpec，则更新用于指定目标的地址信息

case spec: InetAddressSpec =>

netChannel.updateEndpoint(voterAddressEntry.getKey, spec)

// 如果地址类型是 UnknownAddressSpec，则跳过更新目标信息，因为这个地址不可达

case \_: UnknownAddressSpec =>

logger.info(s"Skipping channel update for destination ID: ${voterAddressEntry.getKey} " + s"because of non-routable endpoint: ${NON\_ROUTABLE\_ADDRESS.toString}")

// 如果地址类型不是 InetAddressSpec 也不是 UnknownAddressSpec，则记录一个警告日志

case invalid: AddressSpec =>

logger.warn(s"Unexpected address spec (type: ${invalid.getClass}) for channel update for " + s"destination ID: ${voterAddressEntry.getKey}")

}

}

// 启动网络请求通道

netChannel.start()

// 启动 Controller 请求处理线程

raftIoThread.start()

}

...

}

通过上面的源码，首先调用 [RaftManager.scala#buildNetworkChannel](http://raftmanager.scala/#buildNetworkChannel)() 方法创建底层网络通信组件 [KafkaNetworkChannel](http://kafkanetworkchannel/)，接着调用 [RaftManager.scala#buildRaftClient](http://raftmanager.scala/#buildRaftClient)() 方法创建 Raft 集群客户端 [KafkaRaftClient](http://kafkaraftclient/)，最后创建 [RaftIoThread](http://raftiothread%20/) 线程用来不断进行本地网络通信客户端请求响应处理。

从启动方法可以看出 [KafkaRaftManager#startup](http://kafkaraftserver/#startup) 方法启动了以下组件：

1.  启动网络请求通道，[KafkaNetworkChannel.scala#start](http://kafkanetworkchannel.scala/#start)() 方法其实就是启动 [RaftSendThread](http://raftsendthread/) 线程，这个线程类继承于 [](http://%20interbrokersendthread/)[InterBrokerSendThread](http://%20interbrokersendthread/)，[InterBrokerSendThread](http://interbrokersendthread/) 又继承于 [ShutdownableThread](http://shutdownablethread/)。

def start(): Unit = {

requestThread.start()

}

1.  启动 Controller 请求处理线程。这步比较关键，从流程上看，它实际上是整个 [KafkaRaftManager](http://kafkaraftmanager/) 组件开始工作的入口。[RaftIoThread](http://raftiothread/) 间接继承了 [Thread](http://thread/)，实际上线程启动后应该会调用 [RaftIoThread.scala#run()](http://raftiothread.scala/#run\(\)) 方法，而这个方法由其父类 [ShutdownableThread.scala#run()](http://shutdownablethread.scala/#run\(\)) 实现。可以看到， ShutdownableThread.scala#run() 的核心逻辑其实是循环调用子类 [RaftIoThread.scala#doWork()](http://raftiothread.scala/#doWork\(\)) 方法，该方法会在下面进行剖析。

override def run(): Unit = {

isStarted = true

info("Starting")

try {

while (isRunning)

doWork()

} catch {

} finally {

}

info("Stopped")

}

另外，[RaftManager.scala#buildNetworkChannel](http://raftmanager.scala/#buildNetworkChannel)() 方法的核心为以下两步：

private def buildNetworkChannel(): KafkaNetworkChannel = {

// 1、 调用 RaftManager.scala#buildNetworkClient() 方法创建网络客户端，可以看到这个方法创建了 NetworkClient 实例用于底层网络连接的监听处理

val netClient \= buildNetworkClient()

// 2、使用上一步创建的 NetworkClient 实例新建 KafkaNetworkChannel 对象，给上层提供集群交互请求的发送入口

new KafkaNetworkChannel(time, netClient, config.quorumRequestTimeoutMs, threadNamePrefix)

}

private def buildNetworkClient(): NetworkClient = {

// 监听器

val controllerListenerName \= new ListenerName(config.controllerListenerNames.head)

// 安全协议

val controllerSecurityProtocol \= config.listenerSecurityProtocolMap.getOrElse(controllerListenerName, SecurityProtocol.forName(controllerListenerName.value()))

// 构建渠道

val channelBuilder \= ChannelBuilders.clientChannelBuilder(

controllerSecurityProtocol,

JaasContext.Type.SERVER,

config,

controllerListenerName,

config.saslMechanismControllerProtocol,

time,

config.saslInterBrokerHandshakeRequestEnable,

logContext

)

val metricGroupPrefix \= "raft-channel"

val collectPerConnectionMetrics \= false

// 实例化 Selector 多路复用

val selector \= new Selector(

NetworkReceive.UNLIMITED,

config.connectionsMaxIdleMs,

metrics,

time,

metricGroupPrefix,

Map.empty\[String, String\].asJava,

collectPerConnectionMetrics,

channelBuilder,

logContext

)

val clientId \= s"raft-client-${config.nodeId}"

val maxInflightRequestsPerConnection \= 1

val reconnectBackoffMs \= 50

val reconnectBackoffMsMs \= 500

val discoverBrokerVersions \= true

// 最后实例化网络客户端通信组件

new NetworkClient(

selector,

new ManualMetadataUpdater(),

clientId,

maxInflightRequestsPerConnection,

reconnectBackoffMs,

reconnectBackoffMsMs,

Selectable.USE\_DEFAULT\_BUFFER\_SIZE,

config.socketReceiveBufferBytes,

config.quorumRequestTimeoutMs,

config.connectionSetupTimeoutMs,

config.connectionSetupTimeoutMaxMs,

time,

discoverBrokerVersions,

new ApiVersions,

logContext

)

}

接着来剖析实例化 [KafkaNetworkChannel](http://kafkanetworkchannel/) 实例。

class KafkaNetworkChannel(

time: Time,

client: KafkaClient,

requestTimeoutMs: Int,

threadNamePrefix: String

) extends NetworkChannel with Logging {

import KafkaNetworkChannel.\_

type ResponseHandler \= AbstractResponse => Unit

private val correlationIdCounter \= new AtomicInteger(0)

private val endpoints \= mutable.HashMap.empty\[Int, Node\]

private val requestThread \= new RaftSendThread(

name = threadNamePrefix + "-outbound-request-thread",

networkClient = client,

requestTimeoutMs = requestTimeoutMs,

time = time,

isInterruptible = false

)

....

}

可以看出 [KafkaNetworkChannel](http://kafkanetworkchannel/) 实例创建过程中关键组件也会被创建，其中就包括 [RaftSendThread](http://raftsendthread/) 请求发送线程。[RaftSendThread](http://raftsendthread/) 继承自 [InterBrokerSendThread](http://interbrokersendthread/)，其用途将在后文分析。

## **2.3 BrokerServer#starup 启动**

def startup(): Unit = {

// 将状态从 SHUTDOWN 转变为 STARTING，如果状态已经不是 SHUTDOWN，则直接返回

if (!maybeChangeStatus(SHUTDOWN, STARTING)) return

try {

info("Starting broker")

/\* start scheduler \*/

// 初始化定时任务调度器

kafkaScheduler = new KafkaScheduler(config.backgroundThreads)

kafkaScheduler.startup()

/\* register broker metrics \*/

\_brokerTopicStats = new BrokerTopicStats

// 初始化配额管理器

quotaManagers = QuotaFactory.instantiate(config, metrics, time, threadNamePrefix.getOrElse(""))

quotaCache = new ClientQuotaCache()

// 用于保证kafka-log数据目录的存在

logDirFailureChannel = new LogDirFailureChannel(config.logDirs.size)

// 初始化日志管理器，kafka的消息以日志形式存储

logManager = LogManager(config, initialOfflineDirs, configRepository, kafkaScheduler, time,

brokerTopicStats, logDirFailureChannel, keepPartitionMetadataFile = true)

// 初始化元数据缓存

metadataCache = MetadataCache.raftMetadataCache(config.nodeId)

// Enable delegation token cache for all SCRAM mechanisms to simplify dynamic update.

// This keeps the cache up-to-date if new SCRAM mechanisms are enabled dynamically.

tokenCache = new DelegationTokenCache(ScramMechanism.mechanismNames)

credentialProvider = new CredentialProvider(ScramMechanism.mechanismNames, tokenCache)

// 控制器节点

val controllerNodes \= RaftConfig.quorumVoterStringsToNodes(controllerQuorumVotersFuture.get()).asScala

val controllerNodeProvider \= RaftControllerNodeProvider(metaLogManager, config, controllerNodes)

val forwardingChannelManager \= BrokerToControllerChannelManager(

controllerNodeProvider,

time,

metrics,

config,

channelName = "forwarding",

threadNamePrefix,

retryTimeoutMs = 60000

)

forwardingManager = new ForwardingManagerImpl(forwardingChannelManager)

forwardingManager.start()

val apiVersionManager \= ApiVersionManager(

ListenerType.BROKER,

config,

Some(forwardingManager),

brokerFeatures,

featureCache

)

// Create and start the socket server acceptor threads so that the bound port is known.

// Delay starting processors until the end of the initialization sequence to ensure

// that credentials have been loaded before processing authentications.

// 启动 socket，监听 9092 端口，等待接收客户端请求

socketServer = new SocketServer(config, metrics, time, credentialProvider, apiVersionManager)

socketServer.startup(startProcessingRequests = false)

// ISR 信息同步通道管理器

val alterIsrChannelManager \= BrokerToControllerChannelManager(

controllerNodeProvider,

time,

metrics,

config,

channelName = "alterisr",

threadNamePrefix,

retryTimeoutMs = Long.MaxValue

)

// 启动 ISR 信息同步通道管理器

alterIsrManager = new DefaultAlterIsrManager(

controllerChannelManager = alterIsrChannelManager,

scheduler = kafkaScheduler,

time = time,

brokerId = config.nodeId,

brokerEpochSupplier = () => lifecycleManager.brokerEpoch()

)

alterIsrManager.start()

// 初始化副本管理器，高可用相关

this.replicaManager = new RaftReplicaManager(config, metrics, time,

kafkaScheduler, logManager, isShuttingDown, quotaManagers,

brokerTopicStats, metadataCache, logDirFailureChannel, alterIsrManager,

configRepository, threadNamePrefix)

/\* start token manager \*/

if (config.tokenAuthEnabled) {

throw new UnsupportedOperationException("Delegation tokens are not supported")

}

// 启动 token 管理器

tokenManager = new DelegationTokenManager(config, tokenCache, time , null)

tokenManager.startup() // does nothing, we just need a token manager in order to compile right now...

// Create group coordinator, but don't start it until we've started replica manager.

// Hardcode Time.SYSTEM for now as some Streams tests fail otherwise, it would be good to fix the underlying issue

// 初始化消费者组协调器

groupCoordinator = GroupCoordinator(config, replicaManager, Time.SYSTEM, metrics)

// Create transaction coordinator, but don't start it until we've started replica manager.

// Hardcode Time.SYSTEM for now as some Streams tests fail otherwise, it would be good to fix the underlying issue

// 初始化事务协调器

transactionCoordinator = TransactionCoordinator(config, replicaManager,

new KafkaScheduler(threads = 1, threadNamePrefix = "transaction-log-manager-"),

createTemporaryProducerIdManager, metrics, metadataCache, Time.SYSTEM)

val autoTopicCreationChannelManager \= BrokerToControllerChannelManager(controllerNodeProvider,

time, metrics, config, "autocreate", threadNamePrefix, 60000)

autoTopicCreationManager = new DefaultAutoTopicCreationManager(

config, Some(autoTopicCreationChannelManager), None, None,

groupCoordinator, transactionCoordinator)

autoTopicCreationManager.start()

/\* Add all reconfigurables for config change notification before starting the metadata listener \*/

config.dynamicConfig.addReconfigurables(this)

val clientQuotaMetadataManager \= new ClientQuotaMetadataManager(

quotaManagers, socketServer.connectionQuotas, quotaCache)

// 初始化 broker 端元数据监听器

brokerMetadataListener = new BrokerMetadataListener(

config.nodeId,

time,

metadataCache,

configRepository,

groupCoordinator,

replicaManager,

transactionCoordinator,

threadNamePrefix,

clientQuotaMetadataManager)

// 启动网络组件监听器

val networkListeners \= new ListenerCollection()

config.advertisedListeners.foreach { ep =>

networkListeners.add(new Listener().

setHost(ep.host).

setName(ep.listenerName.value()).

setPort(socketServer.boundPort(ep.listenerName)).

setSecurityProtocol(ep.securityProtocol.id))

}

lifecycleManager.start(() => brokerMetadataListener.highestMetadataOffset(),

BrokerToControllerChannelManager(controllerNodeProvider, time, metrics, config,

"heartbeat", threadNamePrefix, config.brokerSessionTimeoutMs.toLong),

metaProps.clusterId, networkListeners, supportedFeatures)

// Register a listener with the Raft layer to receive metadata event notifications

metaLogManager.register(brokerMetadataListener)

val endpoints \= new util.ArrayList\[Endpoint\](networkListeners.size())

var interBrokerListener: Endpoint = null

networkListeners.iterator().forEachRemaining(listener => {

val endPoint \= new Endpoint(listener.name(),

SecurityProtocol.forId(listener.securityProtocol()),

listener.host(), listener.port())

endpoints.add(endPoint)

if (listener.name().equals(config.interBrokerListenerName.value())) {

interBrokerListener = endPoint

}

})

if (interBrokerListener == null) {

throw new RuntimeException("Unable to find inter-broker listener " +

config.interBrokerListenerName.value() + ". Found listener(s): " +

endpoints.asScala.map(ep => ep.listenerName().orElse("(none)")).mkString(", "))

}

val authorizerInfo \= ServerInfo(new ClusterResource(clusterId),

config.nodeId, endpoints, interBrokerListener)

/\* Get the authorizer and initialize it if one is specified.\*/

authorizer = config.authorizer

authorizer.foreach(\_.configure(config.originals))

val authorizerFutures: Map\[Endpoint, CompletableFuture\[Void\]\] = authorizer match {

case Some(authZ) =>

authZ.start(authorizerInfo).asScala.map { case (ep, cs) =>

ep -> cs.toCompletableFuture

}

case None \=\>

authorizerInfo.endpoints.asScala.map { ep =>

ep -> CompletableFuture.completedFuture\[Void\](null)

}.toMap

}

// 初始化副本拉取管理器

val fetchManager \= new FetchManager(Time.SYSTEM,

new FetchSessionCache(config.maxIncrementalFetchSessionCacheSlots,

KafkaServer.MIN\_INCREMENTAL\_FETCH\_SESSION\_EVICTION\_MS))

// Start processing requests once we've caught up on the metadata log, recovered logs if necessary,

// and started all services that we previously delayed starting.

val raftSupport \= RaftSupport(forwardingManager, metadataCache)

// 数据面请求分发处理器

dataPlaneRequestProcessor = new KafkaApis(socketServer.dataPlaneRequestChannel, raftSupport,

replicaManager, groupCoordinator, transactionCoordinator, autoTopicCreationManager,

config.nodeId, config, configRepository, metadataCache, metrics, authorizer, quotaManagers,

fetchManager, brokerTopicStats, clusterId, time, tokenManager, apiVersionManager)

// 数据面请求I/O 线程池

dataPlaneRequestHandlerPool = new KafkaRequestHandlerPool(config.nodeId, socketServer.dataPlaneRequestChannel, dataPlaneRequestProcessor, time,

config.numIoThreads, s"${SocketServer.DataPlaneMetricPrefix}RequestHandlerAvgIdlePercent", SocketServer.DataPlaneThreadPrefix)

socketServer.controlPlaneRequestChannelOpt.foreach { controlPlaneRequestChannel =>

// 控制面请求分发处理器

controlPlaneRequestProcessor = new KafkaApis(controlPlaneRequestChannel, raftSupport,

replicaManager, groupCoordinator, transactionCoordinator, autoTopicCreationManager,

config.nodeId, config, configRepository, metadataCache, metrics, authorizer, quotaManagers,

fetchManager, brokerTopicStats, clusterId, time, tokenManager, apiVersionManager)

// 控制面请求I/O 线程池

controlPlaneRequestHandlerPool = new KafkaRequestHandlerPool(config.nodeId, socketServer.controlPlaneRequestChannelOpt.get, controlPlaneRequestProcessor, time,

1, s"${SocketServer.ControlPlaneMetricPrefix}RequestHandlerAvgIdlePercent", SocketServer.ControlPlaneThreadPrefix)

}

// Block until we've caught up on the metadata log

lifecycleManager.initialCatchUpFuture.get()

// Start log manager, which will perform (potentially lengthy) recovery-from-unclean-shutdown if required.

// 启动日志管理器

logManager.startup(metadataCache.getAllTopics())

// Start other services that we've delayed starting, in the appropriate order.

// 启动副本管理器和高水位检测点线程

replicaManager.startup()

replicaManager.startHighWatermarkCheckPointThread()

// 启动消费者组协调器

groupCoordinator.startup(() => metadataCache.numPartitions(Topic.GROUP\_METADATA\_TOPIC\_NAME).

getOrElse(config.offsetsTopicPartitions))

// 启动事务协调器

transactionCoordinator.startup(() => metadataCache.numPartitions(Topic.TRANSACTION\_STATE\_TOPIC\_NAME).

getOrElse(config.transactionTopicPartitions))

// Apply deferred partition metadata changes after starting replica manager and coordinators

// so that those services are ready and able to process the changes.

replicaManager.endMetadataChangeDeferral(

RequestHandlerHelper.onLeadershipChange(groupCoordinator, transactionCoordinator, \_, \_))

socketServer.startProcessingRequests(authorizerFutures)

// We're now ready to unfence the broker.

lifecycleManager.setReadyToUnfence()

// 最后将状态从 STARTING 转变为 STARTED

maybeChangeStatus(STARTING, STARTED)

} catch {

case e: Throwable =>

maybeChangeStatus(STARTING, STARTED)

fatal("Fatal error during broker startup. Prepare to shutdown", e)

shutdown()

throw e

}

}

看到上面的源码是不是很亲切，它跟我们前面 [【服务端 Broker 源码分析系列第十六篇】图解 Kafka 源码之服务端启动流程](https://articles.zsxq.com/id_mj6vi10s49c1.html) 这篇剖析服务端启动源码类似。

![](https://article-images.zsxq.com/FkusPk6Wd3iMpJ6HwUVjV5tLn89S)

通过上面的源码，可以看出 [Broker#startup](http://broker/#startup) 方法启动了以下组件：启动 BrokerServer 的 socketServer、KafkaScheduler、logManager、alterIsrManager、groupCoordinator 等组件。

## **2.4 Controller#starup 启动**

def startup(): Unit = {

// 将状态从 SHUTDOWN 转变为 STARTING，如果状态已经不是 SHUTDOWN，则直接返回

if (!maybeChangeStatus(SHUTDOWN, STARTING)) return

try {

info("Starting controller")

// 将状态从 STARTING 转变为 STARTED

maybeChangeStatus(STARTING, STARTED)

// 初始化日志记录器的前缀

this.logIdent = new LogContext(s"\[ControllerServer id=${config.nodeId}\] ").logPrefix()

// 注册 Kafka Broker的 ClusterId 和 yammer-metrics-count 到 MetricRegistry 中

newGauge("ClusterId", () => clusterId)

newGauge("yammer-metrics-count", () => KafkaYammerMetrics.defaultRegistry.allMetrics.size)

// 创建Linux Io Metrics Collector

linuxIoMetricsCollector = new LinuxIoMetricsCollector("/proc", time, logger.underlying)

if (linuxIoMetricsCollector.usable()) {

newGauge("linux-disk-read-bytes", () => linuxIoMetricsCollector.readBytes())

newGauge("linux-disk-write-bytes", () => linuxIoMetricsCollector.writeBytes())

}

// 将Java Controller监听器转换为Scala中的Endpoint，再转换为Java中的Endpoint，并配置授权器

val javaListeners \= config.controllerListeners.map(\_.toJava).asJava

authorizer \= config.authorizer

authorizer.foreach(\_.configure(config.originals))

// 授权相关

val authorizerFutures: Map\[Endpoint, CompletableFuture\[Void\]\] = authorizer match {

case Some(authZ) =>

// It would be nice to remove some of the broker-specific assumptions from

// AuthorizerServerInfo, such as the assumption that there is an inter-broker

// listener, or that ID is named brokerId.

val controllerAuthorizerInfo \= ServerInfo(

new ClusterResource(clusterId), config.nodeId, javaListeners, javaListeners.get(0))

authZ.start(controllerAuthorizerInfo).asScala.map { case (ep, cs) =>

ep -> cs.toCompletableFuture

}.toMap

case None \=\>

javaListeners.asScala.map {

ep => ep -> CompletableFuture.completedFuture\[Void\](null)

}.toMap

}

val apiVersionManager \= new SimpleApiVersionManager(ListenerType.CONTROLLER)

tokenCache = new DelegationTokenCache(ScramMechanism.mechanismNames)

credentialProvider = new CredentialProvider(ScramMechanism.mechanismNames, tokenCache)

// 初始化 socketServer

socketServer = new SocketServer(config,

metrics,

time,

credentialProvider,

apiVersionManager)

// 启动 socketServer

socketServer.startup(startProcessingRequests = false, controlPlaneListener = None, config.controllerListeners)

socketServerFirstBoundPortFuture.complete(socketServer.boundPort(

config.controllerListeners.head.listenerName))

// 构造到 Kafka Broker 配置文件元数据和 Kafka 日志元数据的 ConfigDefs 实例

val configDefs \= Map(ConfigResource.Type.BROKER -> KafkaConfig.configDef,

ConfigResource.Type.TOPIC -> LogConfig.configDefCopy).asJava

// 创建 QuorumController 实例

val threadNamePrefixAsString \= threadNamePrefix.getOrElse("")

controller = new QuorumController.Builder(config.nodeId).

setTime(time).

setThreadNamePrefix(threadNamePrefixAsString).

setConfigDefs(configDefs).

setLogManager(metaLogManager).

setDefaultReplicationFactor(config.defaultReplicationFactor.toShort).

setDefaultNumPartitions(config.numPartitions.intValue()).

setSessionTimeoutNs(TimeUnit.NANOSECONDS.convert(config.brokerSessionTimeoutMs.longValue(),

TimeUnit.MILLISECONDS)).

setMetrics(new QuorumControllerMetrics(KafkaYammerMetrics.defaultRegistry())).

build()

// 创建Quota Manager 实例

quotaManagers = QuotaFactory.instantiate(config, metrics, time, threadNamePrefix.getOrElse(""))

// 从 Raft 配置中获取所有 Controller 节点信息，然后将其转换为Scala的节点列表

val controllerNodes \=

RaftConfig.quorumVoterStringsToNodes(controllerQuorumVotersFuture.get()).asScala

// 创建 ControllerApis，并赋值给 controllerApis 变量

controllerApis = new ControllerApis(socketServer.dataPlaneRequestChannel,

authorizer,

quotaManagers,

time,

supportedFeatures,

controller,

raftManager,

config,

metaProperties,

controllerNodes.toSeq,

apiVersionManager)

// 创建 ControllerApis 的请求处理池

controllerApisHandlerPool = new KafkaRequestHandlerPool(config.nodeId,

socketServer.dataPlaneRequestChannel,

controllerApis,

time,

config.numIoThreads,

s"${SocketServer.DataPlaneMetricPrefix}RequestHandlerAvgIdlePercent",

SocketServer.DataPlaneThreadPrefix)

// 启动 SocketServer 的数据处理并设置授权器启动的 CompletableFuture

socketServer.startProcessingRequests(authorizerFutures)

} catch {

case e: Throwable =>

maybeChangeStatus(STARTING, STARTED)

fatal("Fatal error during controller startup. Prepare to shutdown", e)

shutdown()

throw e

}

}

通过上面的源码，可以看出 [Controller#startup](http://broker/#startup) 方法启动了以下组件：启动 ControllerServer 的 socketServer、KafkaRequestHandlerPool 等组件。

综上，「**Broker 节点**」和 「**Controller 节点**」都会提供独立的网络服务。其中 「**Broker 节点**」提供 Kafka 数据服务，比如读写消息等，「**Controller 节点**」则提供管理 Kafka 的服务，比如创建主题等。

下面通过一张图来说明其流程：

![](https://article-images.zsxq.com/Fnr6V4A8CezKlzuVOoDsZAkLhEjK)

> 如上图：Kafka 集群中的节点能够同时承担两种角色，一个是作为 Broker 处理外部请求，另一个是作为 Controller 管理整个集群元数据以及其他管理操作。
> 
> 2.8 版本以前
> 
> Kafka 依赖 zk 管理集群元数据，元数据包括 topic 信息、各个节点的连接信息等。此时 Controller 节点无法指定，一个 Kafka 节点成功在 zk 中 建立 /controller 节点即成为 Controller，存在很大的随机性。
> 
> 2.8 版本以后
> 
> Kafka 移除 zk 依赖，通过配置文件中的 process.roles 属性指定节点角色，一个节点可被直接指定为 Controller 节点，Controller 不再需要和 zk 通信管理集群元数据。 整个 Kafka 集群中 Controller 节点可以存在多个，共同组成 Controller 集群，负责处理集群元数据。

上图是基于 Kafka 3.0 版本 Broker 服务端完成请求处理的基本架构，其事件驱动框架基于 「**主从 Reactor 多线程**」实现，从图中可以看到 「**KafkaRaftServer**」内部可能会启动两个处理不同请求的服务端「**Broker 节点**」和 「**Controller 节点**」，不过二者的组成基本类似，都包含一个底层的网络服务器 [SocketServer](http://socketserver/) 和一个I/O业务线程请求处理器 [KafkaRequestHandlerPool](http://kafkarequesthandlerpool/)。

关于这两个组件可以点击：[【服务端Broker源码分析系列第七篇】图解Kafka源码之网络层请求处理全流程总结](https://articles.zsxq.com/id_xtza6mo3tkfb.html) 学习。

> Kafka 服务端网络通信的架构目前大致经历过 3 个阶段的演进：
> 
> 1、早期的设计是任意节点都可能成为 Controller，同时集群中每个节点都需要暴露端口给 Controller 进行连接方便控制，所以节点使用同一个端口监听处理所有网络请求
> 
> 2、当到了 2.2 版本开始将请求分流，Kafka 节点分别用数据面 DataPlane 和控制面 ControlPlane 来对应处理数据类请求 和 控制类请求(来自集群内部Controller的控制请求)，二者区分出不同的端口(控制类请求的处理端口由 control.plane.listener.name 配置)，且同时存在于同一个 KafkaServer 中。如感兴趣可以参考 Kafka 社区记录KAFKA-4453 add request prioritization
> 
> 3、到了 2.8 版本，Kafka 推出了剔除 zk 的 KRaft 模式。在该模式下 KafkaRaftServer 分别抽象出对应Controller 角色的 ControllerServer 和对应 Broker 角色的 BrokerServer，消息生产之类的请求只会被 BrokerServer 处理，元数据类请求则由 ControllerServer 处理，一个节点可同时充当两种角色。在 KRaft 模式下，Kaka 节点已经不支持控制类请求， control.plane.listener.name 配置在 3.0 版本后的 KRaft 模式下将导致异常。

##   
**03 KRaft 状态梳理**

首先，在「**KafkaRaftManager**」中会构建 raft 客户端，源码如下：

private val raftClient \= buildRaftClient()

def kafkaRaftClient: KafkaRaftClient\[T\] = raftClient

// 构建 raft 客户端

private def buildRaftClient(): KafkaRaftClient\[T\] = {

val expirationTimer \= new SystemTimer("raft-expiration-executor")

val expirationService \= new TimingWheelExpirationService(expirationTimer)

val quorumStateStore \= new FileBasedStateStore(new File(dataDir, "quorum-state"))

// 实例化一个 raft 客户端对象

val client \= new KafkaRaftClient(

recordSerde,

netChannel,

metadataLog,

quorumStateStore,

time,

metrics,

expirationService,

logContext,

metaProperties.clusterId.toString,

OptionalInt.of(config.nodeId),

raftConfig

)

// 进行初始化

client.initialize()

client

}

// 这是 KafkaRaftClient.java 的方法

@Override

public void initialize() {

// 使用当前日志的最后一条偏移量和最后抓取的epoch初始化quorum

quorum.initialize(new OffsetAndEpoch(log.endOffset().offset, log.lastFetchedEpoch()));

// 获取当前时间

long currentTimeMs \= time.milliseconds();

// 如果quorum是Leader，则抛出异常，因为Voter不能作为Leader进行初始化

if (quorum.isLeader()) {

throw new IllegalStateException("Voter cannot initialize as a Leader");

} else if (quorum.isCandidate()) {

// 如果quorum是Candidate，则执行onBecomeCandidate方法

onBecomeCandidate(currentTimeMs);

} else if (quorum.isFollower()) {

// 如果quorum是Follower，则执行onBecomeFollower方法

onBecomeFollower(currentTimeMs);

}

// 当只有一个voter时，立即成为Candidate

if (quorum.isVoter()

&& quorum.remoteVoters().isEmpty()

&& !quorum.isCandidate()) {

transitionToCandidate(currentTimeMs);

}

}

从上得出，[KafkaRaftClient.java#initialize](http://kafkaraftclient.java/#initialize)() 方法的核心其实是调用 [QuorumState.java#initialize](http://quorumstate.java/#initialize)() 方法去初始化当前节点在集群中所处的角色状态。

  
[QuorumState.java#initialize](http://quorumstate.java/#initialize)() 方法比较长，不过核心只有两点：

1.  首先调用 [QuorumStateStore#readElectionState()](http://quorumstatestore/#readElectionState\(\)) 从本地 [quorum-state](http://quorum-state/) 文件读取选举状态记录，此处主要是为了覆盖节点重启的场景。
2.  根据选举状态初始化节点的集群状态，初次启动的节点将被设置为 [UnattachedState](http://unattachedstate/) 状态。

public void initialize(OffsetAndEpoch logEndOffsetAndEpoch) throws IllegalStateException {

// We initialize in whatever state we were in on shutdown. If we were a leader

// or candidate, probably an election was held, but we will find out about it

// when we send Vote or BeginEpoch requests.

ElectionState election;

try {

// 从本地 quorum-state 文件读取选举状态记录

election = store.readElectionState();

if (election == null) {

election = ElectionState.withUnknownLeader(0, voters);

}

} catch (final UncheckedIOException e) {

// For exceptions during state file loading (missing or not readable),

// we could assume the file is corrupted already and should be cleaned up.

log.warn("Clearing local quorum state store after error loading state {}",

store.toString(), e);

store.clear();

election = ElectionState.withUnknownLeader(0, voters);

}

final EpochState initialState;

// 根据选举状态初始化节点的集群状态

if (!election.voters().isEmpty() && !voters.equals(election.voters())) {

throw new IllegalStateException("Configured voter set: " + voters

\+ " is different from the voter set read from the state file: " + election.voters()

\+ ". Check if the quorum configuration is up to date, "

\+ "or wipe out the local state file if necessary");

} else if (election.hasVoted() && !isVoter()) {

String localIdDescription \= localId.isPresent() ?

localId.getAsInt() + " is not a voter" :

"is undefined";

throw new IllegalStateException("Initialized quorum state " + election

\+ " with a voted candidate, which indicates this node was previously "

\+ " a voter, but the local id " + localIdDescription);

} else if (election.epoch < logEndOffsetAndEpoch.epoch) {

log.warn("Epoch from quorum-state file is {}, which is " +

"smaller than last written epoch {} in the log",

election.epoch, logEndOffsetAndEpoch.epoch);

initialState = new UnattachedState(

time,

logEndOffsetAndEpoch.epoch,

voters,

Optional.empty(),

randomElectionTimeoutMs(),

logContext

);

} else if (localId.isPresent() && election.isLeader(localId.getAsInt())) {

// If we were previously a leader, then we will start out as resigned

// in the same epoch. This serves two purposes:

// 1. It ensures that we cannot vote for another leader in the same epoch.

// 2. It protects the invariant that each record is uniquely identified by

// offset and epoch, which might otherwise be violated if unflushed data

// is lost after restarting.

initialState = new ResignedState(

time,

localId.getAsInt(),

election.epoch,

voters,

randomElectionTimeoutMs(),

Collections.emptyList(),

logContext

);

} else if (localId.isPresent() && election.isVotedCandidate(localId.getAsInt())) {

initialState = new CandidateState(

time,

localId.getAsInt(),

election.epoch,

voters,

Optional.empty(),

1,

randomElectionTimeoutMs(),

logContext

);

} else if (election.hasVoted()) {

initialState = new VotedState(

time,

election.epoch,

election.votedId(),

voters,

Optional.empty(),

randomElectionTimeoutMs(),

logContext

);

} else if (election.hasLeader()) {

initialState = new FollowerState(

time,

election.epoch,

election.leaderId(),

voters,

Optional.empty(),

fetchTimeoutMs,

logContext

);

} else {

// 初次启动的节点将被设置为 UnattachedState 状态。

initialState = new UnattachedState(

time,

election.epoch,

voters,

Optional.empty(),

randomElectionTimeoutMs(),

logContext

);

}

transitionTo(initialState);

}

另外「**KafkaRaftManager#client**」是一个「**KafkaRaftClient**」实例，而 「**KafkaRaftClient**」负责实现具体的 Raft 算法，其关键属性如下:

![](https://article-images.zsxq.com/FhMFHSbKINFJ7xrVMxxca91uMPgU)

从上图源码得出，在 「**KafkaRaftClient#quorum**」属性中存储了节点当前的 Raft 状态，以及节点当前任期号。这是 Raft 中非常关键的属性，在「**KafkaRaft**」模块下存在以下 Raft 状态：

1.  **CandidateState** ：它是 candidate 候选者状态。
2.  **LeaderState**：它是 leader 领导者状态。
3.  **FollowerState**：它是 follower 追随者状态。
4.  **ResignedState**：它是用来 Leader 节点优雅下线的。
5.  **UnattachedState**：当一个 Controller 节点收到比自己任期大的投票信息时，会先转换为 [unattached](http://unattached/) 状态，代表该节点当前状态「**已失效**」。当进入该状态后，如果超时未收到来自其他新的「**Leader 节点**」的请求时，则该节点就会转化为 **Candidate** 状态**。**
6.  **VoteState**：当收到其他节点的投票请求后，如果当前节点同意给请求投票，则切换到该状态。当进入该状态后，如果超时未选出新的「**Leader 节点**」，则该节点就会重新切换到 **Candidate** 状态**。**

综上，除了基础的 Raft 状态外，新增了 **ResignedState**，它是用来实现 Leader 节点优雅下线的。另外 **UnattachedState**、**VoteState** 主要是 KRaft 为了减少出现 「**选票瓜分**」导致选举失败的情况而新增的中间处理状态。

另外 KafkaRaftClient 集群客户端的构造方法如下，需要关注的点有以下几个:

1.  调用 [RaftConfig.java#quorumVoterIds](http://raftconfig.java/#quorumVoterIds)() 方法获取配置文件中 [controller.quorum.voters](http://controller.quorum.voters/) 属性配置的有选举权的节点的 id 列表。
2.  创建代表自身在集群中的角色状态的 [QuorumState](http://quorumstate/) 实例。
3.  调用 [KafkaNetworkChannel.java#updateEndpoint](http://kafkanetworkchannel.java/#updateEndpoint)() 方法将其它可以投票的节点连接地址信息更新到集群请求发送组件内部。

KafkaRaftClient(

RecordSerde<T> serde,

NetworkChannel channel,

RaftMessageQueue messageQueue,

ReplicatedLog log,

QuorumStateStore quorumStateStore,

MemoryPool memoryPool,

Time time,

Metrics metrics,

ExpirationService expirationService,

int fetchMaxWaitMs,

String clusterId,

OptionalInt nodeId,

LogContext logContext,

Random random,

RaftConfig raftConfig

) {

this.serde = serde;

this.channel = channel;

this.messageQueue = messageQueue;

this.log = log;

this.memoryPool = memoryPool;

this.fetchPurgatory = new ThresholdPurgatory<>(expirationService);

this.appendPurgatory = new ThresholdPurgatory<>(expirationService);

this.time = time;

this.clusterId = clusterId;

this.fetchMaxWaitMs = fetchMaxWaitMs;

this.logger = logContext.logger(KafkaRaftClient.class);

this.random = random;

this.raftConfig = raftConfig;

this.snapshotCleaner = new RaftMetadataLogCleanerManager(logger, time, 60000, log::maybeClean);

Set<Integer> quorumVoterIds = raftConfig.quorumVoterIds();

this.requestManager = new RequestManager(quorumVoterIds, raftConfig.retryBackoffMs(),

raftConfig.requestTimeoutMs(), random);

this.quorum = new QuorumState(

nodeId,

quorumVoterIds,

raftConfig.electionTimeoutMs(),

raftConfig.fetchTimeoutMs(),

quorumStateStore,

time,

logContext,

random);

this.kafkaRaftMetrics = new KafkaRaftMetrics(metrics, "raft", quorum);

kafkaRaftMetrics.updateNumUnknownVoterConnections(quorum.remoteVoters().size());

// Update the voter endpoints with what's in RaftConfig

Map<Integer, RaftConfig.AddressSpec> voterAddresses = raftConfig.quorumVoterConnections();

voterAddresses.entrySet().stream()

.filter(e -> e.getValue() instanceof RaftConfig.InetAddressSpec)

.forEach(e -> this.channel.updateEndpoint(e.getKey(), (RaftConfig.InetAddressSpec) e.getValue()));

}

##   
**04 KRaft 请求类型梳理**

通过前面的启动方法剖析，ControllerServer 使用 SocketServer 组件来提供网络服务，并使用 ControllerApis 来处理请求，它主要负责处理所有 KRaft 模块的请求。

1.  Raft 请求，如下：
2.  **VoteRequest**：Controller 节点进入 Candidate 状态后会发送该请求，要求其他 Controller 节点给自己投票。
3.  **BeginQuorumEpochRequest**：当某个 Controller 节点刚刚选举为 Leader 节点时会发送该请求给其他 Controller 节点。需要 Leader 节点「**主动**」发送该请求，来告诉其他节点自己已经被选举为 Leader 节点。
4.  **EndQuorumEpochRequest**：Leader 节点优雅退出时，会发送该请求给其他 Controller 节点，其他 Controller 节点收到该请求后即可开始新的 Epoch 选举。
5.  **FetchRequest**：这个类似于 Kafka 中的 Fetch 请求，用来 Follower 节点同步 Leader 节点的消息数据。
6.  **FetchSnapRequestData**：如果 Follower 节点的 endOffset 小于 Leader 的 startOffset，则 Follower 节点将发送该请求获取 Leader 节点的数据快照，并使用快照来同步数据。
7.  Contoller 请求，主要是管理类请求，如下图： ![](https://article-images.zsxq.com/Fl7IkpVOVPrJp2gVdynQJIjtFi2-)

## **05 KRaft 请求处理流程**

接下来，我们重点来剖析下 Raft 请求处理的流程，在前面初始化启动时，可以看到如下组件：

![](https://article-images.zsxq.com/Fj5DYOmvlbJ0WhLSROM0US6JNF-I)

## **5.1 ControllerApis#handleRaftRequest()**

![](https://article-images.zsxq.com/FiW1w4X-cCPyug2d7-Yyq0yRA9iZ)

// 这是 KafkaRaftManager 类的方法

override def handleRequest(

header: RequestHeader,

request: ApiMessage,

createdTimeMs: Long

): CompletableFuture\[ApiMessage\] = {

// 创建一个传入的 Raft 请求对象，包括请求头、请求消息和创建时间

val inboundRequest \= new RaftRequest.Inbound(

header.correlationId,

request,

createdTimeMs

)

// 调用 raftClient#handle 方法处理传入的请求

raftClient.handle(inboundRequest)

// 返回一个 CompletableFuture，当处理完成时会得到响应结果

inboundRequest.completion.thenApply { response =>

response.data

}

}

// KafkaRaftClient 类属性

private final RaftMessageQueue messageQueue;

/\*\*

\* KafkaRaftClient 类方法

\* Handle an inbound request. The response will be returned through

\* {@link RaftRequest.Inbound#completion}.

\* @param request The inbound request

\*/

public void handle(RaftRequest.Inbound request) {

// 很简单就是直接将请求写入到队列中

messageQueue.add(Objects.requireNonNull(request));

}

## **5.2 raftIoThread()**

在前面启动方法中会启动 [raftIoThread](http://raftiothread/) 线程，它主要是用来处理 Raft 请求的，所以当 Raft 请求被写入到 [messageQueue](http://messagequeue/) 后，[raftIoThread](http://raftiothread/) 线程就会不断的调用 [KafkaRaftClient#poll](http://kafkaraftclient/#poll) 方法，至此集群组件 [KafkaRaftManager](http://kafkaraftmanager/) 的初始化结束。如下：

object KafkaRaftManager {

class RaftIoThread(

client: KafkaRaftClient\[\_\],

threadNamePrefix: String

) extends ShutdownableThread(

// 处理线程名称

name = threadNamePrefix + "-io-thread",

isInterruptible = false

) {

override def doWork(): Unit = {

// 不断调用 KafkaRaftClient#poll 方法去处理 Raft 请求

client.poll()

}

....

}

class KafkaRaftManager\[T\](

metaProperties: MetaProperties,

config: KafkaConfig,

recordSerde: RecordSerde\[T\],

topicPartition: TopicPartition,

time: Time,

metrics: Metrics,

threadNamePrefixOpt: Option\[String\]

) extends RaftManager\[T\] with Logging {

private val raftIoThread \= new RaftIoThread(raftClient, threadNamePrefix)

def startup(): Unit = {

....

// 启动 raft I/O 线程

raftIoThread.start()

}

}

## **5.3 KafkaRaftClient#poll()**

接下来，我们来剖析下重点方法 poll()。

public void poll() {

// 1、处理监听器已添加的事件

pollListeners();

// 获取当前时间

long currentTimeMs \= time.milliseconds();

// 检查是否需要完成 shutdown 流程，如果需要就直接返回

if (maybeCompleteShutdown(currentTimeMs)) {

return;

}

// 2、根据当前的 Raft 状态，来执行不同的处理逻辑

long pollStateTimeoutMs \= pollCurrentState(currentTimeMs);

// 3、尝试清理快照，返回清理的超时时间

long cleaningTimeoutMs \= snapshotCleaner.maybeClean(currentTimeMs);

// 取 pollStateTimeoutMs和cleaningTimeoutMs 之间的最小值为pollTimeoutMs

long pollTimeoutMs \= Math.min(pollStateTimeoutMs, cleaningTimeoutMs);

// 更新 kafkaRaftMetrics 的 pollStart 时间

kafkaRaftMetrics.updatePollStart(currentTimeMs);

// 4、从 messageQueue 中获取 Raft 请求。

RaftMessage message \= messageQueue.poll(pollTimeoutMs);

// 获取处理完成时间

currentTimeMs = time.milliseconds();

// 更新 kafkaRaftMetrics 的 pollEnd 时间

kafkaRaftMetrics.updatePollEnd(currentTimeMs);

// 5、如果获取到了消息，则调用 handleInboundMessage 方法进行消息处理

if (message != null) {

handleInboundMessage(message, currentTimeMs);

}

}

该方法时用来**处理 Raft 请求的**，步骤如下：

1.  处理监听器已添加的事件。
2.  根据当前的 Raft 状态，来执行不同的处理逻辑。
3.  尝试清理快照，返回清理的超时时间（后面篇章会单独剖析）。
4.  从 messageQueue 中获取 Raft 请求。
5.  如果获取到了消息，则调用 [handleInboundMessage](http://handleinboundmessage%20/) 方法进行消息处理。

这里我们重点剖析下 「**第一步**」、「**第二步**」、「**第五步**」。

## **5.4 pollListeners()**

接下来，先来剖析下 「**第一步**」，源码如下：

private void pollListeners() {

// 处理所有未完成注册的事件监听器

while (true) {

Registration<T> registration = pendingRegistrations.poll();

if (registration == null) {

break;

}

// 处理注册事件

processRegistration(registration);

}

// 检查监听器是否有读操作没有完成，并更新进度

quorum.highWatermark().ifPresent(highWatermarkMetadata -> {

updateListenersProgress(highWatermarkMetadata.offset);

});

}

该方法主要用来**处理监听器已添加的事件并更新进度的**。

接着来剖析下 「**第二步**」，方法比较简单，但里面的处理逻辑本篇先不挨个剖析，会在后面具体场景中再进行对应剖析。

## **5.5 pollCurrentState()**

// 根据 quorum 的状态调用相应的 poll 方法

private long pollCurrentState(long currentTimeMs) {

if (quorum.isLeader()) {

return pollLeader(currentTimeMs);

} else if (quorum.isCandidate()) {

return pollCandidate(currentTimeMs);

} else if (quorum.isFollower()) {

return pollFollower(currentTimeMs);

} else if (quorum.isVoted()) {

return pollVoted(currentTimeMs);

} else if (quorum.isUnattached()) {

return pollUnattached(currentTimeMs);

} else if (quorum.isResigned()) {

return pollResigned(currentTimeMs);

} else {

throw new IllegalStateException("Unexpected quorum state " + quorum);

}

}

该方法主要用来**执行 Raft 算法的具体逻辑**，跟上面「**03 部分**」的 Raft 状态一一对应。

## **5.6 handleInboundMessage()**

private void handleInboundMessage(RaftMessage message, long currentTimeMs) {

logger.trace("Received inbound message {}", message);

// 根据接收到的消息类型来处理消息

if (message instanceof RaftRequest.Inbound) {

RaftRequest.Inbound request \= (RaftRequest.Inbound) message;

// 处理来自其他节点的请求消息

handleRequest(request, currentTimeMs);

} else if (message instanceof RaftResponse.Inbound) {

RaftResponse.Inbound response \= (RaftResponse.Inbound) message;

ConnectionState connection \= requestManager.getOrCreate(response.sourceId());

if (connection.isResponseExpected(response.correlationId)) {

// 处理来自其他节点的响应消息

handleResponse(response, currentTimeMs);

} else {

logger.debug("Ignoring response {} since it is no longer needed", response);

}

} else {

throw new IllegalArgumentException("Unexpected message " + message);

}

}

该方法主要用来**根据消息的类型来进行不同处理**，如果是请求类型则调用 handleRequest，如果是响应类型则调用handleResponse。

## **06 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头引出了「**Kraft**」中的角色之分：「**Controller**」、「**Broker**」。

2、接着带大家「**初探 KRaft 请求处理流程**」，剖析了几个相关类的启动方法都做了什么。

3、接着带大家「**梳理了 Raft 状态**」都有哪些。

4、接着带大家「**梳理了 KRaft 请求类型**」都有哪些。

4、最后带大家剖析了「**Kraft 请求处理流程**」。

下篇我们来深度剖析「**Kraft Leader 选举机制**」，大家期待，我们下期见。