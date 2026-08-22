大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端大致的启动流程**」，通过「**脚本执行的方式**」一步步引出 「**KafkaServer**」的启动 Broker 所需的各类组件库，从今天开始，我们来深度剖析 Kafka「**Controller**」的底层源码实现，这是 Controller 系列第二篇，我们接着来深度聊聊「**Kafka 服务端 Broker 启动后集群如何感知的**」。

  
![](https://article-images.zsxq.com/FpXiTLstjl6n_fudyVTorWL-5owE)

## **01 总体概述**

在上篇的最后，调用了「**KafkaServer**」的 startup 方法进行了 Broker 的启动过程，这中间启动了各类 Broker 需要的组件类库，接下来我们深入的去剖析了下每个组件类库是如何启动的。

本文还是以「**zk 模式**」启动流程为主，后面抽空补充以 「**raft 模式**」启动流程。

主要还是以「**KafkaServer.scala**」类源码为主，其在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/KafkaServer.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/KafkaServer.scala)

另外 「**raft 模式**」启用的是「**KafkaRaftServer.scala**」类源码为主，其在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/KafkaRaftServer.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/KafkaRaftServer.scala)，后面补充，「**raft 模式**」把 「**Broker**」和 「**Controller**」功能独立分开了。

## **02 ZK 通讯组件初始化**

initZkClient(time)

private def initZkClient(time: Time): Unit = {

// 输出连接ZooKeeper信息

info(s"Connecting to zookeeper on ${config.zkConnect}")

// 定义创建ZooKeeper客户端的方法

def createZkClient(zkConnect: String, isSecure: Boolean) = {

// 根据给定的信息创建KafkaZkClient实例

KafkaZkClient(zkConnect, isSecure, config.zkSessionTimeoutMs, config.zkConnectionTimeoutMs,

config.zkMaxInFlightRequests, time, name = Some("Kafka server"), zkClientConfig = Some(zkClientConfig))

}

// 1、检查配置信息中是否包含chroot，并获取其值

val chrootIndex \= config.zkConnect.indexOf("/")

val chrootOption \= {

if (chrootIndex > 0) Some(config.zkConnect.substring(chrootIndex))

else None

}

// 2、获取是否启用了安全ACL、是否启用了ZooKeeper安全模式的标志

val secureAclsEnabled \= config.zkEnableSecureAcls

val isZkSecurityEnabled \= JaasUtils.isZkSaslEnabled() || KafkaConfig.zkTlsClientAuthEnabled(zkClientConfig)

// 3、若启用了安全ACL，但未正确配置ZooKeeper客户端安全模式，则抛出异常

if (secureAclsEnabled && !isZkSecurityEnabled)

throw new java.lang.SecurityException(s"${KafkaConfig.ZkEnableSecureAclsProp} is true, but ZooKeeper client TLS configuration identifying at least $KafkaConfig.ZkSslClientEnableProp, $KafkaConfig.ZkClientCnxnSocketProp, and $KafkaConfig.ZkSslKeyStoreLocationProp was not present and the " +

s"verification of the JAAS login file failed ${JaasUtils.zkSecuritySysConfigString}")

// 4、确保chroot路径存在

chrootOption.foreach { chroot =>

// 5、创建连接ZooKeeper客户端的实例，并检查chroot路径是否存在，如果不存在，则尝试创建该路径

val zkConnForChrootCreation \= config.zkConnect.substring(0, chrootIndex)

val zkClient \= createZkClient(zkConnForChrootCreation, secureAclsEnabled)

zkClient.makeSurePersistentPathExists(chroot) // 创建路径

info(s"Created zookeeper path $chroot")

zkClient.close()

}

// 6、创建连接ZooKeeper客户端的实例，同时创建ZooKeeper的根路径

\_zkClient = createZkClient(config.zkConnect, secureAclsEnabled)

\_zkClient.createTopLevelPaths()

}

该方法主要用来**初始化 zk 客户端**，步骤如下：

1.  检查配置信息中是否包含 chroot，并获取其值。
2.  获取是否启用了安全 ACL、是否启用了 ZooKeeper 安全模式的标志。
3.  若启用了安全 ACL，但未正确配置 ZooKeeper 客户端安全模式，则抛出异常。
4.  确保 chroot 路径存在。
5.  创建连接 ZooKeeper 客户端的实例，并检查 chroot 路径是否存在，如果不存在，则尝试创建该路径。
6.  记录成功创建路径的日志信息。
7.  关闭客户端实例。
8.  创建连接 ZooKeeper 客户端的实例，同时创建 ZooKeeper 的根路径。当根路径不存在时，方法 createTopLevelPaths 实现了以下操作：
9.  检查 /brokers/topics 路径是否存在，如果不存在则创建该路径。
10.  检查 /admin 路径是否存在，如果不存在则创建该路径。
11.  检查 /config 路径是否存在，如果不存在则创建该路径。
12.  检查 /cluster 路径是否存在，如果不存在则创建该路径。
13.  检查 /controller 路径是否存在，如果不存在则创建该路径。

## **2.1 创建 zk 根目录**

「**KafkaZkClient.scala**」类源码为主，其在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/zk/KafkaZkClient.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/zk/KafkaZkClient.scala)

「**ZkData.scala**」类源码为主，其在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/zk/ZkData.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/zk/ZkData.scala)

/\*\*

\* Pre-create top level paths in ZK if needed.

\*/

def createTopLevelPaths(): Unit = {

// 循环创建持久化路径

  ZkData.PersistentZkPaths.foreach(makeSurePersistentPathExists(\_))

}

// These are persistent ZK paths that should exist on kafka broker startup.

val PersistentZkPaths \= Seq(

ConsumerPathZNode.path, // 旧消费者路径: /consumers

BrokerIdsZNode.path, // Broker的节点路径: /brokers/ids

TopicsZNode.path, // 主题的节点路径: /brokers/topics

ConfigEntityChangeNotificationZNode.path, // 配置实体更改通知的节点路径: /config/changes

DeleteTopicsZNode.path, // 删除主题的节点路径: /admin/delete\_topics

BrokerSequenceIdZNode.path, // Broker 序列ID的节点路径: /brokers/seqid

IsrChangeNotificationZNode.path, // ISR 变更通知的节点路径: /isr\_change\_notification

ProducerIdBlockZNode.path, // 生产者 ID 块的节点路径: /brokers/producerid\_manager

LogDirEventNotificationZNode.path // 日志目录事件通知的节点路径: /brokers/log\_dir\_event\_notification

) ++ ConfigType.all.map(ConfigEntityTypeZNode.path)

ConfigEntityTypeZNode.path

/\*\*

/config/topics for topic configs

/config/brokers for broker configs

/config/clients for client configs

/config/users for user configs

/config/ips for IP configs

....

\*/

## **03 BrokerId 初始化**

/\* generate brokerId \*/

config.brokerId = getOrGenerateBrokerId(preloadedBrokerMetadataCheckpoint)

// 获取或者生成 Broker id

private def getOrGenerateBrokerId(brokerMetadata: RawMetaProperties): Int = {

// 1、首先，获取配置文件中配置的 brokerId。

val brokerId \= config.brokerId

// 2、如果配置的 brokerId 大于等于 0 并且与存储在 meta.properties 文件中的 brokerId 不一致，则抛出异常。

if (brokerId >= 0 && brokerMetadata.brokerId.exists(\_ != brokerId))

throw new InconsistentBrokerIdException(

s"Configured broker.id $brokerId doesn't match stored broker.id ${brokerMetadata.brokerId} in meta.properties. " +

s"If you moved your data, make sure your configured broker.id matches. " +

s"If you intend to create a new broker, you should remove all data in your data directories (log.dirs).")

else if (brokerMetadata.brokerId.isDefined)

// 已定义直接返回

brokerMetadata.brokerId.get

else if (brokerId < 0 && config.brokerIdGenerationEnable) // generate a new brokerId from Zookeeper

// 重新生成

generateBrokerId()

else

brokerId

}

该方法主要用来**初始化 BrokerId**，步骤如下：

1.  首先，获取配置文件中配置的 brokerId。
2.  如果配置的 brokerId 大于等于 0 并且与存储在 meta.properties 文件中的 brokerId 不一致，则抛出InconsistentBrokerIdException 异常。这表示配置的 brokerId 与存储的 brokerId 不匹配，可能是由于数据迁移而导致的。如果要创建一个新的 broker，应该删除数据目录（[log.dirs](http://log.dirs/)）中的所有数据。
3.  如果在 meta.properties 文件中已经定义了 brokerId，则返回该 brokerId。
4.  如果配置的 brokerId 小于 0 并且启用了配置项 config.brokerIdGenerationEnable，则从 Zookeeper 中生成一个新的brokerId。
5.  否则，返回配置文件中配置的 brokerId。

## **3.1 重新生成 BrokerId**

/\*\*

\* Return a sequence id generated by updating the broker sequence id path in ZK.

\* Users can provide brokerId in the config. To avoid conflicts between ZK generated

\* sequence id and configured brokerId, we increment the generated sequence id by KafkaConfig.MaxReservedBrokerId.

\*/

private def generateBrokerId(): Int = {

try {

// 使用 zkClient 对象从 ZooKeeper 中获取 Broker ID 序列号，其中 zkClient 是 ZkClient 类的实例。将获取的序列号与配置文件中的 maxReservedBrokerId 相加，得到新的 Broker ID。

zkClient.generateBrokerSequenceId() + config.maxReservedBrokerId

} catch {

// 如果在获取序列号过程中抛出异常，则记录错误信息并抛出GenerateBrokerIdException异常。

case e: Exception =>

error("Failed to generate broker.id due to ", e)

throw new GenerateBrokerIdException("Failed to generate broker.id", e)

}

}

## **04 集群管控组件之 KafkaController**

上一篇中，我们剖析了 Kafka 启动过程中的一些组件加载，也知道了 Broker 可以分为很多的 Partition，每个Partition 内部也可以分为 Leader 和 Follower，主从之间有数据的复制。

那么这么多 Partition 是谁在管理？Broker内部有没有主从之分？这就是 KafkaController，下面细细道来。

## **4.1 KafkaController 入口**

KafkaController 的启动入口很简洁，在 [KafkaServer#startup](http://kafkaserver/#startup) 方法中。

/\* start kafka controller \*/

kafkaController = new KafkaController(config, zkClient, time, metrics, brokerInfo, brokerEpoch, tokenManager, brokerFeatures, featureCache, threadNamePrefix)

// 启动 KafkaController

kafkaController.startup()

首先实例化一个 KafkaController 对象，之后启动了这个 Controller。

/\*\*

\* config：Kafka配置信息，通过它，你能拿到Broker端所有参数的值。

\* zkClient：ZooKeeper客户端，Controller与 ZooKeeper 的所有交互均通过该属性完成。

\* time：提供时间服务(如获取当前时间)的工具类。

\* metrics：实现指标监控服务(如创建监控指标)的工具类。

\* initialBrokerInfo：Broker节点信息，包括主机名、端口号，所用监听器等。

\* initialBrokerEpoch：Broker Epoch值，用于隔离老Controller发送的请求。

\* tokenManager：实现Delegation token管理的工具类。Delegation token是一种轻量级的认证机制。

\* threadNamePrefix：Controller端事件处理线程名字前缀。

\*/

class KafkaController(val config: KafkaConfig,

zkClient: KafkaZkClient,

time: Time,

metrics: Metrics,

initialBrokerInfo: BrokerInfo,

initialBrokerEpoch: Long,

tokenManager: DelegationTokenManager,

brokerFeatures: BrokerFeatures,

featureCache: FinalizedFeatureCache,

threadNamePrefix: Option\[String\] = None)

extends ControllerEventProcessor with Logging with KafkaMetricsGroup {

KafkaController 实现了 ControllerEventProcessor 接口，也就实现了处理 Controller 事件的 process 方法。

这里比较重要的字段有 3 个：

1.  **Config** ：KafkaConfig 类实例，里面封装了 Broker 端所有参数的值。
2.  **ZkClient**：ZooKeeper 客户端类，定义了与 ZooKeeper 交互的所有方法。
3.  **initialBrokerEpoch**：Controller 所在 Broker 的 Epoch 值。Kafka 使用它来确保 Broker 不会处理老 Controller 发来的请求。

##   
**4.2 KafkaController 模块组成**

KafkaController 是 **Kafka 集群的控制管理模块**，**且一个集群只有一个 Leader**。其主要通过向 ZK 注册各种监听事件来管理整个集群节点、分区的 Leader 的选举、再平衡等问题。

「**KafkaController.scala**」类源码为主，其在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/KafkaController.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/KafkaController.scala)

### **KafkaController 组成部分：**

![](https://article-images.zsxq.com/lms8kiPpjlYF4GkzjH20v0XMzCrG)

// 控制器日志行中使用的前缀，包含了该控制器所属的Broker ID

this.logIdent = s"\[Controller id=${config.brokerId}\] "

// 保存了该 Broker 的元数据信息，\_brokerEpoch 则保存了该 Broker 的 Epoch。

@volatile private var brokerInfo \= initialBrokerInfo

@volatile private var \_brokerEpoch \= initialBrokerEpoch

// 标志表明是否开启了修改 ISR 过程，这个设置依赖于 Broker 之间通信的协议版本。

private val isAlterIsrEnabled \= config.interBrokerProtocolVersion.isAlterIsrSupported

// stateChangeLogger 用于记录控制器状态变化的日志。

private val stateChangeLogger \= new StateChangeLogger(config.brokerId, inControllerContext = true, None)

// 集群元数据类，保存集群所有元数据，包括 Broker、Topic、Partition、Replica 等元数据信息

val controllerContext \= new ControllerContext

// 控制器通道管理器，用于管理控制器和 Broker 之间创建和维护通信连接。

var controllerChannelManager \= new ControllerChannelManager(controllerContext, config, time, metrics,stateChangeLogger, threadNamePrefix)

// Kafka 内置的定时任务线程调度器，当前唯一负责定期执行 Leader 重选举

private\[controller\] val kafkaScheduler \= new KafkaScheduler(1)

// 控制器事件管理器，负责管理事件处理线程，如添加、删除Broker、Topic等。

private\[controller\] val eventManager \= new ControllerEventManager(config.brokerId, this, time,

controllerContext.stats.rateAndTimeMetrics)

// 用于批量处理来自 Broker 的请求。

private val brokerRequestBatch \= new ControllerBrokerRequestBatch(config, controllerChannelManager,eventManager, controllerContext, stateChangeLogger)

// 副本状态机，负责副本状态转换，比如处理 ISR 集合的变化

val replicaStateMachine: ReplicaStateMachine = new ZkReplicaStateMachine(config, stateChangeLogger, controllerContext, zkClient,new ControllerBrokerRequestBatch(config, controllerChannelManager, eventManager, controllerContext, stateChangeLogger))

// 分区状态机，负责 Partition 状态的转换

val partitionStateMachine: PartitionStateMachine = new ZkPartitionStateMachine(config, stateChangeLogger, controllerContext, zkClient,new ControllerBrokerRequestBatch(config, controllerChannelManager, eventManager, controllerContext, stateChangeLogger))

// 主题删除管理器，负责删除主题及日志

val topicDeletionManager \= new TopicDeletionManager(config, controllerContext, replicaStateMachine,partitionStateMachine, new ControllerDeletionClient(this, zkClient))

// 控制器变更事件处理器

private val controllerChangeHandler \= new ControllerChangeHandler(eventManager)

// Broker 变更事件处理器

private val brokerChangeHandler \= new BrokerChangeHandler(eventManager)

// 针对特定 Broker 变更事件处理器

private val brokerModificationsHandlers: mutable.Map\[Int, BrokerModificationsHandler\] = mutable.Map.empty

// 主题变更事件处理器

private val topicChangeHandler \= new TopicChangeHandler(eventManager)

// 主题删除事件处理器

private val topicDeletionHandler \= new TopicDeletionHandler(eventManager)

// 针对特定 Partition 变更事件处理器。

private val partitionModificationsHandlers: mutable.Map\[String, PartitionModificationsHandler\] = mutable.Map.empty

// 分区重分配监听器

private val partitionReassignmentHandler \= new PartitionReassignmentHandler(eventManager)

// 优选 replica 选举监听器

private val preferredReplicaElectionHandler \= new PreferredReplicaElectionHandler(eventManager)

// isr 变化通知监听器

private val isrChangeNotificationHandler \= new IsrChangeNotificationHandler(eventManager)

// 文件目录事件通知监听器

private val logDirEventNotificationHandler \= new LogDirEventNotificationHandler(eventManager)

1.  **ControllerEventManager**：Controller事件管理器。KafkaController监听zk节点变化事件，会将事件放入ControllerEventManager的事件队列，而ControllerEventManager内部的事件处理线程会从队列中获取事件，并调用KafkaController的process()处理事件。
2.  **PartitionStateMachine**：分区状态机，定义及管理分区的状态。
3.  **ReplicaStateMachine**：副本状态机，定义及管理副本的状态。
4.  **TopicDeletionManager**：TopicDeletionManager 负责对管理员指定的 topic 执行删除操作，它定义了 DeleteTopicsThread 线程，采用异步的方式删除待删除的 topic 集合。
5.  **xxxHandler**：各种事件处理器。KafkaController 在初始化的时候，会监听zk节点的事件，而不同节点会绑定不同的事件处理器。

## **4.3 KafkaController ZK 事件监听**

熟悉 zookeeper 的人应该都比较清楚，zookeeper 是基于事件监听模式，通过监控目录（zNode）的变化，来执行事件对应的回调函数。

那么我们就来总结下 kafka Zookeeper 事件监听，如下图：

  
![](https://article-images.zsxq.com/lipPsGxlEeioRGz4QCTUFEgr2N18)

这些 ZooKeeper 监听器的作用如下：

1.  **ControllerChangeManager**：监听 /controller 节点变更的。这种变更包括节点创建、删除以及数据变更。
2.  **brokerModificationsHandlers**：监听 Broker 的数据变更，比如 Broker 的配置信息发生的变化。
3.  **preferredReplicaElectionHandler**：监听 Preferred Leader 选举任务。一旦发现新提交的任务，就为目标主题执行 Preferred Leader 选举。
4.  **partitionModificationsHandlers**：监控主题分区数据变更的监听器，比如，新增加了副本、分区更换了 Leader 副本。
5.  **partitionReassignmentHandler**：监听分区副本重分配任务。一旦发现新提交的任务，就为目标分区执行副本重分配。
6.  **PartitionReassignmentIsrChangeHandler****:** 监听分区副本重分配ISR变更监听器。
7.  **brokerChangeHandler**：监听 Broker 的数量变化。
8.  **topicChangeHandler**：监控主题数量变更。
9.  **topicDeletionHandler**：监听主题删除节点 /admin/delete\_topics 的子节点数量变更。
10.  **isrChangeNotificationHandler**：监听 ISR 副本集合变更。一旦被触发，就需要获取 ISR 发生变更的分区列表，然后更新 Controller 端对应的 Leader 和 ISR 缓存元数据。
11.  **logDirEventNotificationHandler**：监听日志路径变更。一旦被触发，需要获取受影响的 Broker 列表，然后处理这些 Broker 上失效的日志路径。

## **4.4 KafkaController 启动**

Kafka 的每台 Broker 在启动过程中，都会启动 Controller 服务，初始化的入口依然在 KafkaServer 的 startup 函数中，接着 KafkaController 调用 startup 函数向 zookeeper 注册，并向事件管理器中添加一个启动事件，源码如下：

def startup() = {

// 1、注册 ZooKeeper 状态变更监听器，当 zk 重新初始化时会调用，它是用于监听 Zookeeper 会话过期的

zkClient.registerStateChangeHandler(new StateChangeHandler {

override val name: String = StateChangeHandlers.ControllerHandler

// zk 初始化之后调用

override def afterInitializingSession(): Unit = {

//设置 RegisterBrokerAndReelect 事件，重新选举 Controller 的 Leader

eventManager.put(RegisterBrokerAndReelect)

}

// zk 初始化之前调用

override def beforeInitializingSession(): Unit = {

val queuedEvent \= eventManager.clearAndPut(Expire)

// Block initialization of the new session until the expiration event is being handled,

// which ensures that all pending events have been processed before creating the new session

//等待controller中的事件处理完

queuedEvent.awaitProcessing()

}

})

// 2、写入 Startup 事件到事件管理器，eventManager 会调用 KafkaController#process() 处理 Startup 事件， Startup 事件中会进行 controller 的选举等初始化处理

eventManager.put(Startup)

// 3、启动 ControllerEventThread 线程，开始处理事件队列中的 ControllerEvent

eventManager.start()

}

该方法主要用来**启动 KafkaController**，步骤如下：

1.  注册 ZooKeeper 状态变更监听器，当 zk 重新初始化时会调用，它是用于监听 Zookeeper 会话过期的。
2.  zk 初始化之前调用，等待 controller 中的事件处理完成。
3.  zk 初始化之后调用，设置 RegisterBrokerAndReelect 事件，重新选举 Controller 的 Leader。
4.  写入 Startup 事件到事件管理器，eventManager 会调用 KafkaController#process() 处理 Startup 事件， Startup 事件中会进行 controller 的选举等初始化处理。
5.  启动 ControllerEventThread 线程，开始处理事件队列中的 ControllerEvent。

![](https://article-images.zsxq.com/lvBm4LW-GtS6LXRn3gpeteT20UZv)

##   
**4.5 ControllerEventManager**

最后事件管理器启动后，内部交由 ControllerEventThread 调度。首先，从事件队列中取出事件，由于队列调用时take() 函数时阻塞的，直到队列中有事件才执行后续逻辑。

「**ControllerEventManager.scala**」类源码为主，其在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/ControllerEventManager.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/ControllerEventManager.scala)

class ControllerEventThread(name: String) extends ShutdownableThread(name = name, isInterruptible = false) {

logIdent = s"\[ControllerEventThread controllerId=$controllerId\] "

override def doWork(): Unit = {

// 从事件队列中获取待处理的Controller事件，否则等待

val dequeued \= pollFromEventQueue()

dequeued.event match {

// 如果是关闭线程事件，什么都不用做。关闭线程由外部来执行

case ShutdownEventThread \=\> // The shutting down of the thread has been initiated at this point. Ignore this event.

case controllerEvent \=\>

\_state = controllerEvent.state

// 更新对应事件在队列中保存的时间

eventQueueTimeHist.update(time.milliseconds() - dequeued.enqueueTimeMs)

try {

// 定义一个事件处理函数，调用ControllerEventProcessor类的process()函数执行逻辑

def process(): Unit = dequeued.process(processor)

// 处理事件，同时计算处理速

rateAndTimeMetrics.get(state) match {

// 延迟调度

case Some(timer) => timer.time { process() }

case None \=\> process()

}

} catch {

case e: Throwable => error(s"Uncaught error processing event $controllerEvent", e)

}

\_state = ControllerState.Idle

}

}

}

// 从事件队列中获取待处理的Controller事件

private def pollFromEventQueue(): QueuedEvent = {

val count \= eventQueueTimeHist.count()

if (count != 0) {

val event \= queue.poll(eventQueueTimeTimeoutMs, TimeUnit.MILLISECONDS)

if (event == null) {

eventQueueTimeHist.clear()

// 由于队列调用时take() 函数时阻塞的，直到队列中有事件才执行后续逻辑。

queue.take()

} else {

event

}

} else {

// 由于队列调用时take() 函数时阻塞的，直到队列中有事件才执行后续逻辑。

queue.take()

}

}

}

## **4.6 KafkaController 事件处理方法**

由于前面在启动时，放入了一个 **Startup** 事件到队列中，所以最终会调用 process() 函数。

override def process(event: ControllerEvent): Unit = {

try {

event match {

case event: MockEvent =>

// Used only in test cases

// 用于测试目的的模拟事件，调用 process() 方法处理。

event.process()

case ShutdownEventThread \=\>

// 接收到此事件表示控制器线程收到了应该由ControllerEventThread处理的事件，产生错误日志。

error("Received a ShutdownEventThread event. This type of event is supposed to be handle by ControllerEventThread")

case AutoPreferredReplicaLeaderElection \=\>

// 处理自动首选副本领导者选举事件。

processAutoPreferredReplicaLeaderElection()

case ReplicaLeaderElection(partitions, electionType, electionTrigger, callback) =>

// 处理副本领导者选举事件，其中包含要选举的分区、选举类型、选举触发器和回调函数。

processReplicaLeaderElection(partitions, electionType, electionTrigger, callback)

case UncleanLeaderElectionEnable \=\>

// 处理脏选举的副本选举事件。

processUncleanLeaderElectionEnable()

case TopicUncleanLeaderElectionEnable(topic) =>

// 处理脏选举的副本选举事件，针对特定Topic。

processTopicUncleanLeaderElectionEnable(topic)

case ControlledShutdown(id, brokerEpoch, callback) =>

// 处理受控关机事件，其中包括Broker的ID、Broker的Epoch和回调函数。

processControlledShutdown(id, brokerEpoch, callback)

case LeaderAndIsrResponseReceived(response, brokerId) =>

// 处理Leader和ISR响应接收事件，其中包括响应消息和Broker的ID。

processLeaderAndIsrResponseReceived(response, brokerId)

case UpdateMetadataResponseReceived(response, brokerId) =>

// 处理更新元数据响应接收事件，其中包括响应消息和Broker的ID。

processUpdateMetadataResponseReceived(response, brokerId)

case TopicDeletionStopReplicaResponseReceived(replicaId, requestError, partitionErrors) =>

// 处理主题删除停止副本响应接收事件，其中包括副本的ID、请求错误和分区错误。

processTopicDeletionStopReplicaResponseReceived(replicaId, requestError, partitionErrors)

case BrokerChange \=\>

// 处理Broker变化事件。

processBrokerChange()

case BrokerModifications(brokerId) =>

// 处理特定Broker的修改事件。

processBrokerModification(brokerId)

case ControllerChange \=\>

// 处理控制器变化事件。

processControllerChange()

case Reelect \=\>

// 处理重新选举事件。

processReelect()

case RegisterBrokerAndReelect \=\>

// 处理注册Broker并重新选举事件。

processRegisterBrokerAndReelect()

case Expire \=\>

// 处理控制器过期事件。

processExpire()

case TopicChange \=\>

// 处理主题变化事件。

processTopicChange()

case LogDirEventNotification \=\>

// 处理日志目录事件通知。

processLogDirEventNotification()

case PartitionModifications(topic) =>

// 处理特定主题的分区修改事件。

processPartitionModifications(topic)

case TopicDeletion \=\>

// 处理主题删除事件。

processTopicDeletion()

case ApiPartitionReassignment(reassignments, callback) =>

// 处理API分区重分配事件，其中包括分区重新分配和回调函数。

processApiPartitionReassignment(reassignments, callback)

case ZkPartitionReassignment \=\>

// 处理ZooKeeper分区重分配事件。

processZkPartitionReassignment()

case ListPartitionReassignments(partitions, callback) =>

// 处理列出分区重分配事件，其中包括分区列表和回调函数。

processListPartitionReassignments(partitions, callback)

case UpdateFeatures(request, callback) =>

// 处理更新功能事件，其中包括请求和回调函数。

processFeatureUpdates(request, callback)

case PartitionReassignmentIsrChange(partition) =>

// 处理分区重新分配中ISR集合的变化事件。

processPartitionReassignmentIsrChange(partition)

case IsrChangeNotification \=\>

// 处理ISR集合变化通知事件。

processIsrChangeNotification()

case AlterIsrReceived(brokerId, brokerEpoch, isrsToAlter, callback) =>

// 处理Alter ISR事件，其中包括Broker的ID、Broker的Epoch、要更改的ISR集合和回调函数。

processAlterIsr(brokerId, brokerEpoch, isrsToAlter, callback)

case Startup \=\>

// 处理 Startup 事件

processStartup()

}

} catch {

// 在处理事件时，可能会抛出ControllerMovedException异常，表示控制器已经转移到了另一台Broker。在捕获到异常后，输出错误日志并执行maybeResign()方法尝试重新成为控制器。

case e: ControllerMovedException =>

info(s"Controller moved to another broker when processing $event.", e)

maybeResign()

case e: Throwable =>

error(s"Error processing event $event", e)

} finally {

// 无论事件的处理结果如何，最终会调用updateMetrics()方法更新指标

updateMetrics()

}

}

该方法主要用来**事件处理的**，处理 Startup 事件的是最后一个 case，接下来来看下 Startup 事件处理过程。

## **4.7 Startup 事件处理**

private def processStartup(): Unit = {

// 1、注册【/controller】节点变更监听器并检查节点是否存在

zkClient.registerZNodeChangeHandlerAndCheckExistence(controllerChangeHandler)

// 2、选举处理，在 controller 不存在的情况下选举 controller，存在的话，就是从 zk 获取当前的 controller 节点信息

elect()

}

该方法主要用来处理「**注册 /controller 节点监听**」、「**选举控制器**」等工作。 「**选举控制器**」我们会在后面篇章进行深度剖析，这里就先略过了。

##   
**4.8 控制器变更处理器**

KafkaController 定义了十几种 ZooKeeper 监听器。和 Controller 相关的监听器是 ControllerChangeHandler，用于监听 Controller 的变更，源码如下：

class ControllerChangeHandler(eventManager: ControllerEventManager) extends ZNodeChangeHandler {

override val path: String = ControllerZNode.path

//当节点被创建，表明其他某个 broker 被选举为 Leader，需处理 ControllerChange 事件

override def handleCreation(): Unit = eventManager.put(ControllerChange)

//当节点被删除，表明当前无 Leader节点，处理 Reelect 事件，进行重新选举

override def handleDeletion(): Unit = eventManager.put(Reelect)

//当节点数据变更，表明 Leader 可能变更，需处理 ControllerChange 事件

override def handleDataChange(): Unit = eventManager.put(ControllerChange)

}

该方法为「**/controller 节点变更处理器**」，当节点被创建/删除/修改时，会生成对应的事件。

该监听器接收 ControllerEventManager 实例，实现了 ZNodeChangeHandler 接口的三个方法：

1.  handleCreation()。
2.  handleDeletion()。
3.  handleDataChange()。

该监听器下的 [ControllerZNode.path](http://controllerznode.path/) 变量，实际上就是「**/controller**」，表示它监听 ZooKeeper 的这个节点。

这 3 个方法都是用来监听「**/controller**」节点变更的，但是实现的细节稍有不同：

1.  handleCreation 和 handleDataChange 的处理方式是向事件队列写入 ControllerChange 事件。
2.  handleDeletion 的处理方式是向事件队列写入 Reelect 事件，它除了 Broker 执行卸任逻辑之外，还要求 Broker 参与到重新选举中来。

## **4.9 节点监听器及事件处理**

![](https://article-images.zsxq.com/Fp7FpFIZHSsP-kSDhVah5sJhsXAH)

## **05 Broker 启动集群如何感知**

当「**KafkaController**」初始化完成之后，某个 Broker 就会被选举为「**Controller**」了，此时就会行驶它作为控制器的重要权利了，主要职能包括：

1.  **集群成员管理**：对集群成员数量管理以及成员信息管理。
2.  **Broker 状态管理**：Controller 会跟踪集群中所有 Broker 的在线状态，并在 Broker 宕机或者恢复时更新集群的状态。
3.  **主题管理**：就是对所有主题进行管理，主要包括主题的创建、变更与删除。
4.  **分区状态管理**：当新的 Topic 被创建，或者已有的 Topic 被删除时，Controller 会负责管理这些变化，并更新集群的状态。
5.  **分区领导者选举**：当一台 Broker 节点宕机时，并且宕机的机器上包含分区领导者副本时，Controller 会负责对其上的所有Partition 进行新的领导者选举。
6.  **副本状态管理**：Controller 负责管理 Partition 的 ISR 列表，当 Follower 副本无法及时跟随 Leader 副本时，Controller 会将其从 ISR 列表中移除。
7.  **分区重平衡**：当添加或删除 Broker 节点时，Controller 会负责对 Partition 的分布进行重平衡，以确保数据的均匀分布。
8.  **存储集群元数据**：Controller 保存了集群中最全的元数据信息，并通过发送请求同步到其他 Broker。

当「**KafkaBroker**」上下线时，集群是如何感知的？这是本文的重点，下面我们来重点剖析下？

## **5.1 ZK 事件监听注册**

Kafka 控制器（Controller）在进行主备切换（Controller Failover）时执行方法 onControllerFailover()。

private def onControllerFailover(): Unit = {

// 1、检查是否需要设置特性版本。在 Kafka 中，不同版本的 Kafka Brokers 和 Kafka Controller 可能支持不同的特性。因此在进行主备切换时，需要确保控制器上的特性版本与集群中的 Broker 版本匹配。

maybeSetupFeatureVersioning()

info("Registering handlers")

// before reading source of truth from zookeeper, register the listeners to get broker/topic callbacks

// 2、注册子节点变化的监听器

val childChangeHandlers \= Seq(

brokerChangeHandler, // Broker 变更监听器

topicChangeHandler, // Topic 变更监听器

topicDeletionHandler, // Topic 删除监听器

logDirEventNotificationHandler, // 日志目录变化通知的监听器

isrChangeNotificationHandler) // ISR（In-Sync Replica）变更通知的监听器

childChangeHandlers.foreach(zkClient.registerZNodeChildChangeHandler)

val nodeChangeHandlers \= Seq(

preferredReplicaElectionHandler,

partitionReassignmentHandler

)

nodeChangeHandlers.foreach(zkClient.registerZNodeChangeHandlerAndCheckExistence)

// 3、删除旧的日志目录事件通知和 ISR 变化通知。

info("Deleting log dir event notifications")

zkClient.deleteLogDirEventNotifications(controllerContext.epochZkVersion)

info("Deleting isr change notifications")

zkClient.deleteIsrChangeNotifications(controllerContext.epochZkVersion)

// 4、初始化控制器上下文（Controller Context）。Controller Context 包含了控制器运行时的一些状态和信息。在进行主备切换时，需要确保 Controller Context 被正确初始化。

info("Initializing controller context")

initializeControllerContext()

// 5、获取正在进行的 Topic 删除和无法删除的 Topic 列表。

info("Fetching topic deletions in progress")

val (topicsToBeDeleted, topicsIneligibleForDeletion) = fetchTopicDeletionsInProgress()

// 6、初始化 Topic 删除管理器（topicDeletionManager）。Topic 删除管理器用于协调处理正在删除的 Topic。

info("Initializing topic deletion manager")

topicDeletionManager.init(topicsToBeDeleted, topicsIneligibleForDeletion)

// 7、发送 UpdateMetadataRequest。控制器会向集群中的所有 Broker 发送 UpdateMetadataRequest，以便让 Broker 更新自身的元数据（Metadata）信息，包括 Leader、ISR 等。

info("Sending update metadata request")

sendUpdateMetadataRequest(controllerContext.liveOrShuttingDownBrokerIds.toSeq, Set.empty)

// 8、启动 Replica State Machine 和 Partition State Machine。Replica State Machine 用于处理副本相关的操作，Partition State Machine 用于处理分区相关的操作。

replicaStateMachine.startup()

partitionStateMachine.startup()

info(s"Ready to serve as the new controller with epoch $epoch")

// 9、初始化分区重新分配（partition reassignments）。分区重新分配是指在主备切换后，重新分配分区的 Leader。这一步骤是为了确保切换后的集群状态恢复正常。

initializePartitionReassignments()

// 10、尝试进行 Topic 删除。

topicDeletionManager.tryTopicDeletion()

// 11、获取待处理的 Preferred Replica Election（首选副本选举）任务列表，并触发首选副本选举。

val pendingPreferredReplicaElections \= fetchPendingPreferredReplicaElections()

onReplicaElection(pendingPreferredReplicaElections, ElectionType.PREFERRED, ZkTriggered)

// 12、启动定时任务调度器（kafkaScheduler）。

info("Starting the controller scheduler")

kafkaScheduler.startup()

// 13、根据配置的自动 Leader 重平衡使能（autoLeaderRebalanceEnable）情况，调度自动 Leader 重平衡任务。

if (config.autoLeaderRebalanceEnable) {

scheduleAutoLeaderRebalanceTask(delay = 5, unit = TimeUnit.SECONDS)

}

// 14、如果启用了令牌鉴权（token authentication），则启动令牌过期检查调度器（tokenCleanScheduler），并设置定期检查过期令牌并删除。

if (config.tokenAuthEnabled) {

info("starting the token expiry check scheduler")

tokenCleanScheduler.startup()

tokenCleanScheduler.schedule(name = "delete-expired-tokens",

fun = () => tokenManager.expireTokens(),

period = config.delegationTokenExpiryCheckIntervalMs,

unit = TimeUnit.MILLISECONDS)

}

}

在上面 onControllerFailover 方法中，会向 ZK 中注册一个 brokerChangeHandler，它主要监听 [/brokers/ids](http://brokers/ids%20) 下的子节点变化事件，我们知道该节点下就是每一个 Broker 的 id，里面的数据是 Broker 的 ip 端口，协议等信息。

## **5.2 Broker 集群成员管理**

每个 Broker 在启动的时候，都会在 ZooKeeper 的 [/brokers/ids](http://brokers/ids) 节点下创建一个名为 [broker.id](http://broker.id/) 的临时节点。而当该 Broker 正常关闭或意外退出时，ZooKeeper 上对应的临时节点会自动消失。

正是基于这种「**临时节点**」机制，Controller 定义了 BrokerChangeHandler 监听器，专门负责监听 [/brokers/ids](http://brokers/ids) 下的子节点数量变化。

一旦发现有「**新增 Broker**」或「**删除 Broker**」时，[/brokers/ids](http://brokers/ids) 下的「**子节点**」数目一定会发生变化。就会被 Controller 检测到，从而会触发 BrokerChangeHandler 的处理方法，即 handleChildChange 方法，源码如下：

private val brokerChangeHandler \= new BrokerChangeHandler(eventManager)

// 监听 Broker 节点的子节点变化，当有 Broker 加入或退出集群时，将 BrokerChange 事件添加到事件管理器中，以触发对应的事件监听器并进行相应的处理。

class BrokerChangeHandler(eventManager: ControllerEventManager) extends ZNodeChildChangeHandler {

override val path: String = BrokerIdsZNode.path

// 当 Broker 节点（BrokerIdsZNode）的子节点发生变化时，ZooKeeper 会通知 BrokerChangeHandler 执行 handleChildChange 方法。该方法会向 eventManager 添加一个 BrokerChange 事件。在 Kafka 控制器上下文中，事件管理器用于分发不同类型的事件给对应的事件监听器（ControllerListener），以实现事件驱动编程。

override def handleChildChange(): Unit = {

eventManager.put(BrokerChange)

}

}

该方法主要作用就是**向 Controller 事件队列写入一个 BrokerChange 事件**。Controller 定义的所有 Handler 的处理逻辑，都是向事件队列写入相应的 ControllerEvent，而真正的事件处理逻辑在 KafkaController 的 **process** 方法中。

## **5.2.1 事件处理逻辑**

处理 **BrokerChange** 事件的处理逻辑实际上是在 BrokerChange 类中，这里需要注意的是：**Broker 的上下线事件只能由 Controller 来处理，其他 Broker 虽然也会监听** [/brokers/ids](http://brokers/ids) **节点，但不会做任何处理**。

private def processBrokerChange(): Unit = {

// 1、如果该 Broker 非 Controller，直接返回

if (!isActive) return

// 2、从 ZooKeeper 中获取集群 Broker 列表 A

val curBrokerAndEpochs \= zkClient.getAllBrokerAndEpochsInCluster

val curBrokerIdAndEpochs \= curBrokerAndEpochs map

{ case (broker, epoch) => (broker.id, epoch) }

val curBrokerIds \= curBrokerIdAndEpochs.keySet

// 3、从 ControllerContext 获取当前保存的 Broker 列表 B

val liveOrShuttingDownBrokerIds \= controllerContext.liveOrShuttingDownBrokerIds

// 4、获取新增 Broker 列表、待移除 Broker 列表、已重启 Broker 列表和当前运行中的Broker列表

val newBrokerIds \= curBrokerIds.diff(liveOrShuttingDownBrokerIds)

val deadBrokerIds \= liveOrShuttingDownBrokerIds.diff(curBrokerIds)

val bouncedBrokerIds \= (curBrokerIds & liveOrShuttingDownBrokerIds)

.filter(brokerId => curBrokerIdAndEpochs(brokerId) > controllerContext.liveBrokerIdAndEpochs(brokerId))

val newBrokerAndEpochs \= curBrokerAndEpochs.filter { case (broker, \_) => newBrokerIds.contains(broker.id) }

val bouncedBrokerAndEpochs \= curBrokerAndEpochs.filter { case (broker, \_) => bouncedBrokerIds.contains(broker.id) }

val newBrokerIdsSorted \= newBrokerIds.toSeq.sorted

val deadBrokerIdsSorted \= deadBrokerIds.toSeq.sorted

val liveBrokerIdsSorted \= curBrokerIds.toSeq.sorted

val bouncedBrokerIdsSorted \= bouncedBrokerIds.toSeq.sorted

info(s"Newly added brokers: ${newBrokerIdsSorted.mkString(",")}, " +

s"deleted brokers: ${deadBrokerIdsSorted.mkString(",")}, " +

s"bounced brokers: ${bouncedBrokerIdsSorted.mkString(",")}, " +

s"all live brokers: ${liveBrokerIdsSorted.mkString(",")}")

// 5、为每个新增 Broker 创建与之连接的通道管理器和底层的请求发送线程（RequestSendThread）

newBrokerAndEpochs.keySet.foreach(controllerChannelManager.addBroker)

// 6、为每个已重启的 Broker 移除它们现有的通道管理器、请求发送线程 RequestSendThread 等，并重新添加它们

bouncedBrokerIds.foreach(controllerChannelManager.removeBroker)

bouncedBrokerAndEpochs.keySet.foreach(controllerChannelManager.addBroker)

// 7、为每个待移除 Broker 移除它们现有的通道管理器、请求发送线程 RequestSendThread 等

deadBrokerIds.foreach(controllerChannelManager.removeBroker)

// 8、为新增 Broker 执行更新 Controller 元数据和 Broker 启动逻辑

if (newBrokerIds.nonEmpty) {

val (newCompatibleBrokerAndEpochs, newIncompatibleBrokerAndEpochs) =

partitionOnFeatureCompatibility(newBrokerAndEpochs)

if (!newIncompatibleBrokerAndEpochs.isEmpty) {

warn("Ignoring registration of new brokers due to incompatibilities with finalized features: " +

newIncompatibleBrokerAndEpochs.map { case (broker, \_) => broker.id }.toSeq.sorted.mkString(","))

}

controllerContext.addLiveBrokers(newCompatibleBrokerAndEpochs)

onBrokerStartup(newBrokerIdsSorted)

}

// 9、为已重启 Broker 执行重添加逻辑，包括更新 ControllerContext、执行 Broker 重启动逻辑

if (bouncedBrokerIds.nonEmpty) {

controllerContext.removeLiveBrokers(bouncedBrokerIds)

onBrokerFailure(bouncedBrokerIdsSorted)

val (bouncedCompatibleBrokerAndEpochs, bouncedIncompatibleBrokerAndEpochs) =

partitionOnFeatureCompatibility(bouncedBrokerAndEpochs)

if (!bouncedIncompatibleBrokerAndEpochs.isEmpty) {

warn("Ignoring registration of bounced brokers due to incompatibilities with finalized features: " +

bouncedIncompatibleBrokerAndEpochs.map { case (broker, \_) => broker.id }.toSeq.sorted.mkString(","))

}

controllerContext.addLiveBrokers(bouncedCompatibleBrokerAndEpochs)

onBrokerStartup(bouncedBrokerIdsSorted)

}

// 10、为待移除 Broker 执行移除 ControllerContext 和 Broker 终止逻辑

if (deadBrokerIds.nonEmpty) {

controllerContext.removeLiveBrokers(deadBrokerIds)

onBrokerFailure(deadBrokerIdsSorted)

}

if (newBrokerIds.nonEmpty || deadBrokerIds.nonEmpty || bouncedBrokerIds.nonEmpty) {

info(s"Updated broker epochs cache: ${controllerContext.liveBrokerIdAndEpochs}")

}

}

该方法比较复杂，其步骤如下：

1.  首先判断如果该 Broker 是不是 Controller，如果不是，直接返回无权处理。
2.  从 ZooKeeper 中获取集群 Broker 列表， A。
3.  从 ControllerContext 获取当前保存的 Broker 列表 B。
4.  获取以下 4 个 Broker 列表：
5.  新增 Broker 列表：A - B 表示新增的 Broker 。
6.  待移除 Broker 列表：B - A 表示待移除的 Broker。
7.  已重启 Broker 列表：比较复杂，它判断的是 A ^ B 集合中 Epoch 发生变更了的 Broker。因为 Epoch 发生了变更，表示 Broker 发生了重启行为，这里的 Epoch 可以理解为 Broker 的版本或者重启次数。
8.  当前运行中的 Broker 列表：A 排序后的结果。
9.  当拿到这些集合之后，Controller 会分别为这 4 个 Broker 列表执行相应的操作，即 5~10 步做的工作，这里总结如下：
10.  为每个新增 Broker 创建与之连接的通道管理器和底层的请求发送线程（RequestSendThread）。
11.  为每个已重启的 Broker 移除它们现有的通道管理器、请求发送线程 RequestSendThread 等，并重新添加它们。
12.  为每个待移除 Broker 移除它们现有的通道管理器、请求发送线程 RequestSendThread 等。
13.  执行元数据更新操作：调用 ControllerContext 类的各个方法，更新不同的集群元数据信息。比如需要将新增 Broker 加入到集群元数据，将待移除 Broker 从元数据中移除等。
14.  执行 Broker 上线操作：为已重启 Broker 和新增 Broker 调用 onBrokerStartup 方法。
15.  执行 Broker 下线操作：为待移除 Broker 和已重启 Broker 调用 onBrokerFailure 方法。

## **5.2.2 Broker 上线处理过程**

一台 Broker 上线主要有以下两步：

controllerChannelManager.addBroker

onBrokerStartup(newBrokerIdsSorted)

1.  在 ControllerChannelManager 中添加该 Broker 节点，主要包括：Controller 建立与该 Broker 的连接、初始化相应的请求发送线程。
2.  调用 Controller 的 onBrokerStartup() 方法上线该节点。

上面源码中 **controllerChannelManager**，它是 Controller 节点与其它 Broker 之间网络通信管理器。

「**controllerChannelManager****.scala**」类源码为主，其在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/ControllerChannelManager.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/ControllerChannelManager.scala)

「**Broker 上线**」：建立网络连接，并启动请求发送线程，用于处理 LeaderAndIsrRequest，stopReplicaRequest，updateMetadataRequest 三类请求。

def addBroker(broker: Broker): Unit = {

// be careful here. Maybe the startup() API has already started the request send thread

brokerLock synchronized {

if (!brokerStateInfo.contains(broker.id)) {

// 添加新的 Broker

addNewBroker(broker)

// 启动请求发送线程

startRequestSendThread(broker.id)

}

}

}

我们先来看第一步：添加新的 Broker 节点，源码如下：

private def addNewBroker(broker: Broker): Unit = {

// 创建用于存储消息的队列

val messageQueue \= new LinkedBlockingQueue\[QueueItem\]

// 打印调试信息

debug(s"Controller ${config.brokerId} trying to connect to broker ${broker.id}")

// 获取控制器与 Broker 之间的网络监听器名称和安全协议

val controllerToBrokerListenerName \= config.controlPlaneListenerName.getOrElse(config.interBrokerListenerName)

val controllerToBrokerSecurityProtocol \= config.controlPlaneSecurityProtocol.getOrElse(config.interBrokerSecurityProtocol)

// 构建与 Broker 的连接

val brokerNode \= broker.node(controllerToBrokerListenerName)

val logContext \= new LogContext(s"\[Controller id=${config.brokerId}, targetBrokerId=${brokerNode.idString}\] ")

// 构建网络客户端和可重配置的通道构建器

val (networkClient, reconfigurableChannelBuilder) = {

// 创建通道构建器

val channelBuilder \= ChannelBuilders.clientChannelBuilder(

controllerToBrokerSecurityProtocol,

JaasContext.Type.SERVER,

config,

controllerToBrokerListenerName,

config.saslMechanismInterBrokerProtocol,

time,

config.saslInterBrokerHandshakeRequestEnable,

logContext

)

// 如果通道构建器支持重新配置，则将其添加到 Controller 的可重配置列表中

val reconfigurableChannelBuilder \= channelBuilder match {

case reconfigurable: Reconfigurable =>

config.addReconfigurable(reconfigurable)

Some(reconfigurable)

case \_ \=\> None

}

// 创建网络选择器和网络客户端

val selector \= new Selector(

NetworkReceive.UNLIMITED,

Selector.NO\_IDLE\_TIMEOUT\_MS,

metrics,

time,

"controller-channel",

Map("broker-id" -> brokerNode.idString).asJava,

false,

channelBuilder,

logContext

)

val networkClient \= new NetworkClient(

selector,

new ManualMetadataUpdater(Seq(brokerNode).asJava),

config.brokerId.toString,

1,

0,

0,

Selectable.USE\_DEFAULT\_BUFFER\_SIZE,

Selectable.USE\_DEFAULT\_BUFFER\_SIZE,

config.requestTimeoutMs,

config.connectionSetupTimeoutMs,

config.connectionSetupTimeoutMaxMs,

ClientDnsLookup.USE\_ALL\_DNS\_IPS,

time,

false,

new ApiVersions,

logContext

)

(networkClient, reconfigurableChannelBuilder)

}

// 构建用于发送请求的线程名称

val threadName \= threadNamePrefix match {

case None \=\> s"Controller-${config.brokerId}-to-broker-${broker.id}-send-thread"

case Some(name) => s"$name:Controller-${config.brokerId}-to-broker-${broker.id}-send-thread"

}

// 构建请求发送线程，并设置为非守护线程

val requestRateAndQueueTimeMetrics \= newTimer(

RequestRateAndQueueTimeMetricName, TimeUnit.MILLISECONDS, TimeUnit.SECONDS, brokerMetricTags(broker.id)

)

val requestThread \= new RequestSendThread(

config.brokerId,

controllerContext,

messageQueue,

networkClient,

brokerNode,

config,

time,

requestRateAndQueueTimeMetrics,

stateChangeLogger,

threadName

)

requestThread.setDaemon(false)

// 创建队列大小指标

val queueSizeGauge \= newGauge(QueueSizeMetricName, () => messageQueue.size, brokerMetricTags(broker.id))

// 将 Broker 的相关信息存储到 brokerStateInfo 中

brokerStateInfo.put(broker.id, ControllerBrokerStateInfo(networkClient, brokerNode, messageQueue,

requestThread, queueSizeGauge, requestRateAndQueueTimeMetrics, reconfigurableChannelBuilder))

}

接下来第二步：启动发送请求线程，源码如下：

protected def startRequestSendThread(brokerId: Int): Unit = {

// 获取指定 brokerid 的请求发送线程

val requestThread \= brokerStateInfo(brokerId).requestSendThread

// 如果请求发送线程的状态为刚创建的，则启动线程

if (requestThread.getState == Thread.State.NEW)

// 启动发送请求线程。

requestThread.start()

}

最后来看下处理 Broker 上线操作过程，源码如下：

private def onBrokerStartup(newBrokers: Seq\[Int\]): Unit = {

info(s"New broker startup callback for ${newBrokers.mkString(",")}")

// 1、移除新增 Broker 在元数据缓存中对应的副本集合信息

newBrokers.foreach(controllerContext.replicasOnOfflineDirs.remove)

val newBrokersSet \= newBrokers.toSet

val existingBrokers \= controllerContext.liveOrShuttingDownBrokerIds.diff(newBrokersSet)

// Send update metadata request to all the existing brokers in the cluster so that they know about the new brokers

// via this update. No need to include any partition states in the request since there are no partition state changes.

// 2、给集群现有 Broker 发送元数据更新请求，使它们感知到新增 Broker 的存在

sendUpdateMetadataRequest(existingBrokers.toSeq, Set.empty)

// Send update metadata request to all the new brokers in the cluster with a full set of partition states for initialization.

// In cases of controlled shutdown leaders will not be elected when a new broker comes up. So at least in the

// common controlled shutdown case, the metadata will reach the new brokers faster.

// 3、给新增 Broker 发送元数据更新请求，使它们同步集群当前的所有分区数据

sendUpdateMetadataRequest(newBrokers, controllerContext.partitionsWithLeaders)

// the very first thing to do when a new broker comes up is send it the entire list of partitions that it is

// supposed to host. Based on that the broker starts the high watermark threads for the input list of partitions

val allReplicasOnNewBrokers \= controllerContext.replicasOnBrokers(newBrokersSet)

// 4、将新增 Broker 上的所有副本设置为 Online 可用状态

replicaStateMachine.handleStateChanges(allReplicasOnNewBrokers.toSeq, OnlineReplica)

// when a new broker comes up, the controller needs to trigger leader election for all new and offline partitions

// to see if these brokers can become leaders for some/all of those

partitionStateMachine.triggerOnlinePartitionStateChange()

// check if reassignment of some partitions need to be restarted

// 5、重启之前暂停的副本迁移操作

maybeResumeReassignments { (\_, assignment) =>

assignment.targetReplicas.exists(newBrokersSet.contains)

}

// check if topic deletion needs to be resumed. If at least one replica that belongs to the topic being deleted exists

// on the newly restarted brokers, there is a chance that topic deletion can resume

val replicasForTopicsToBeDeleted \= allReplicasOnNewBrokers.filter(p => topicDeletionManager.isTopicQueuedUpForDeletion(p.topic))

// 6、重启之前暂停的主题删除操作

if (replicasForTopicsToBeDeleted.nonEmpty) {

info(s"Some replicas ${replicasForTopicsToBeDeleted.mkString(",")} for topics scheduled for deletion " + s"${controllerContext.topicsToBeDeleted.mkString(",")} are on the newly restarted brokers " + s"${newBrokers.mkString(",")}. Signaling restart of topic deletion for these topics")

topicDeletionManager.resumeDeletionForTopics(replicasForTopicsToBeDeleted.map(\_.topic))

}

// 7、为新增 Broker 注册 BrokerModificationsHandler 监听器

registerBrokerModificationsHandler(newBrokers)

}

该方法主要用来**处理上线的 Broker**，其步骤如下：

1.  移除新增 Broker 在元数据缓存中对应的副本集合信息。此处的新增 Broker 仅仅表示新启动的 Broker，不一定是全新的 Broker
2.  给集群现有 Broker 发送元数据更新请求，使它们感知到新增 Broker 的存在。
3.  给集群新增 Broker 发送元数据更新请求，使它们同步集群当前的所有分区数据。这样，整个集群中的 Broker 就可以互相感知到彼此，其最终所有的 Broker 都能保存相同的分区数据。
4.  将新增 Broker 上的所有副本设置为 Online 可用状态。**这里的 Online 状态表示这些副本正常提供服务，即 Leader 副本对外提供读写服务， Follower 副本自动向 Leader 副本去同步消息**。
5.  重启之前暂停的副本迁移操作。
6.  重启之前暂停的主题删除操作。
7.  为所有新增 Broker 注册 BrokerModificationsHandler 监听器，允许 Controller 监控它们在 ZooKeeper 上的节点的数据变更。

## **5.2.3 Broker 下线处理过程**

一台 Broker 下线主要有也以下两步：

controllerChannelManager.removeBroker

onBrokerFailure(deadBrokerIdsSorted)

上面源码中 **controllerChannelManager**，它是 Controller 节点与其它 Broker 之间网络通信管理器。

「**controllerChannelManager****.scala**」类源码为主，其在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/ControllerChannelManager.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/ControllerChannelManager.scala)

「**Broker 下线**」：关闭网络连接，中断(interrupt)请求发送线程，清空请求队列。

/\*\*

\* 删除 Broker

\*/

def removeBroker(brokerId: Int): Unit = {

// 对 Broker 进行同步操作

brokerLock synchronized {

// 删除已存在的 Broker

removeExistingBroker(brokerStateInfo(brokerId))

}

}

我们来看下：删除已存在 Broker 节点，源码如下：

/\*\*

\* 删除已存在的 Broker

\*/

private def removeExistingBroker(brokerState: ControllerBrokerStateInfo): Unit = {

try {

// 在关闭 NetworkClient 之前关闭 RequestSendThread 以避免在 KAFKA-4959 中描述的非线程安全类的并发使用。

// ShutdownableThread.shutdown()方法中的 shutdownLatch.await() 调用充当同步屏障，它将NetworkClient 从 RequestSendThread 移交给了 ZkEventThread。

// 如果 Broker 状态具有可重配置通道构建器，则从配置中删除该构建器

brokerState.reconfigurableChannelBuilder.foreach(config.removeReconfigurable)

// 关闭请求发送线程

brokerState.requestSendThread.shutdown()

// 关闭NetworkClient

brokerState.networkClient.close()

// 清空消息队列

brokerState.messageQueue.clear()

// 删除指定 Broker 的队列大小指标和请求速率与队列时间指标

removeMetric(QueueSizeMetricName, brokerMetricTags(brokerState.brokerNode.id))

removeMetric(RequestRateAndQueueTimeMetricName, brokerMetricTags(brokerState.brokerNode.id))

// 从 Broker 状态信息中移除该 Broker

brokerStateInfo.remove(brokerState.brokerNode.id)

} catch {

case e: Throwable => error("Error while removing broker by the controller", e)

}

}

最后我们来看下处理 Broker 下线操作过程，源码如下：

/\*

\* This callback is invoked by the replica state machine's broker change listener with the list of failed brokers

\* as input. It will call onReplicaBecomeOffline(...) with the list of replicas on those failed brokers as input.

\*/

private def onBrokerFailure(deadBrokers: Seq\[Int\]): Unit = {

info(s"Broker failure callback for ${deadBrokers.mkString(",")}")

// 1、为每个待移除 Broker，删除元数据对象中的相关项

deadBrokers.foreach(controllerContext.replicasOnOfflineDirs.remove)

// 2、将待移除 Broker 从元数据对象中处于已关闭状态 的Broker 列表中去除

val deadBrokersThatWereShuttingDown \=

deadBrokers.filter(id => controllerContext.shuttingDownBrokerIds.remove(id))

if (deadBrokersThatWereShuttingDown.nonEmpty)

info(s"Removed ${deadBrokersThatWereShuttingDown.mkString(",")} from list of shutting down brokers.")

// 3、找出待移除 Broker 上的所有副本对象，将其置为 Offline 不可用状态

val allReplicasOnDeadBrokers \= controllerContext.replicasOnBrokers(deadBrokers.toSet)

onReplicasBecomeOffline(allReplicasOnDeadBrokers)

// 4、注销之前注册的 BrokerModificationsHandler 监听器

unregisterBrokerModificationsHandler(deadBrokers)

}

该方法主要用来**处理下线的 Broker**，相对上线来说比较简单，其步骤如下：

1.  为每个待移除 Broker 删除元数据对象中的相关所有项。
2.  将待移除 Broker 从元数据对象中处于已关闭状态的 Broker 列表中去除。
3.  找出待移除 Broker 上的所有副本对象，将其置为 Offline 不可用状态 。
4.  最后注销之前注册的 BrokerModificationsHandler 监听器。

## **5.3 成员信息管理**

上面剖析完了 Controller 管理集群成员机制之后，接下来，我们重点剖析下 Controller 如何监听 Broker 端信息的变更，以及具体的操作。

同样，**Controller 也是通过 Zookeeper 监听器方式来应对 Broker 的变化**。这个监听器就是 [BrokerModificationsHandler](http://brokermodificationshandler/)。

一旦 Broker 的信息发生变更，该监听器的 handleDataChange 方法就会被调用，向事件队列写入 [BrokerModifications](http://brokermodifications%20/) 事件。

在 KafkaController#processBrokerModification 方法用来**负责处理该类事件**，源码如下：

private def processBrokerModification(brokerId: Int): Unit = {

if (!isActive) return

// 1、获取目标 Broker 的详细数据，包括每套监听器配置的主机名、端口号以及所使用的安全协议等

val newMetadataOpt \= zkClient.getBroker(brokerId)

// 2、从元数据缓存中获得目标 Broker 的详细数据

val oldMetadataOpt \= controllerContext.liveOrShuttingDownBroker(brokerId)

if (newMetadataOpt.nonEmpty && oldMetadataOpt.nonEmpty) {

val oldMetadata \= oldMetadataOpt.get

val newMetadata \= newMetadataOpt.get

// 3、如果两者不相等，表示 Broker 数据发生了变更。则更新元数据缓存，以及执行 onBrokerUpdate方法处理 Broker 更新的逻辑

if (newMetadata.endPoints != oldMetadata.endPoints || !oldMetadata.features.equals(newMetadata.features)) {

info(s"Updated broker metadata: $oldMetadata -> $newMetadata")

controllerContext.updateBrokerMetadata(oldMetadata, newMetadata)

onBrokerUpdate(brokerId)

}

}

}

步骤如下：

1.  获取目标 Broker 的详细数据，包括每套监听器配置的主机名、端口号以及所使用的安全协议等。
2.  从元数据缓存中获得目标 Broker 的详细数据。
3.  如果两者不相等，表示 Broker 数据发生了变更。则更新元数据缓存，以及执行 onBrokerUpdate 方法处理 Broker 更新的逻辑。

接下来我们来看下 onBrokerUpdate 方法的逻辑，源码如下：

private def onBrokerUpdate(updatedBrokerId: Int): Unit = {

info(s"Broker info update callback for $updatedBrokerId")

// 给集群所有 Broker 发送 UpdateMetadataRequest，让它们去更新元数据

sendUpdateMetadataRequest(controllerContext.liveOrShuttingDownBrokerIds.toSeq, Set.empty)

}

该方法很简单，可以看到它就是**向集群所有 Broker 发送更新元数据信息请求，把变更信息都广播出去**。

最后来看下 sendUpdateMetadataRequest 方法的实现，源码如下：

private\[controller\] def sendUpdateMetadataRequest(brokers: Seq\[Int\], partitions: Set\[TopicPartition\]): Unit = {

try {

// 1、创建一个新的请求批次

brokerRequestBatch.newBatch()

// 2、将请求添加到指定 Broker 的请求列表中，请求中包含需要更新的分区信息

brokerRequestBatch.addUpdateMetadataRequestForBrokers(brokers, partitions)

// 3、将批量请求发给指定的 Broker。其中的 epoch 参数是用来实现幂等请求。

brokerRequestBatch.sendRequestsToBrokers(epoch)

} catch {

case e: IllegalStateException =>

handleIllegalState(e)

}

}

简单来说，该方法主要用来**封装 UpdateMetadataRequest 请求，将请求加入请求批次并发送给指定的 Broker，用来告知 Broker 上下文元数据的更新**。而 epoch 参数的作用是实现幂等性。

## **06 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头通过对上一篇「**kafkaServer**」启动流程进行了拆解剖析，抽离出跟 Broker 启动集群感知相关的源码结构。

2、深度剖析了「**ZK 通讯组件初始化**」的全过程，了解内部主要做了对根目录以及跟 Broker 相关节点目录的创建。

3、接着深度剖析了 「**BrokerID 初始化**」的实现逻辑。

4、接着深度剖析了 「**集群管控组件之 Controller**」的方方面面。

5、最后给大家深度剖析了 「**Broker 启动集群感知**」的处理流程。

下篇我们来深度剖析「**控制器 Controller 元数据管理**」，大家期待，我们下期见。