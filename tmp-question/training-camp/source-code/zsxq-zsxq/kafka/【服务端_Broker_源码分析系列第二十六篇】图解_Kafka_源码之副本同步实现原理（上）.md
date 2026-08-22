大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端 分区状态机机制实现原理**」，了解了分区状态都有哪些以及分区状态之间是如何转换的，从今天开始，我们接着来深度剖析 Kafka「**Broker**」的底层源码实现，我们接着来深度聊聊「**Kafka 服务端 副本同步实现原理**」。

  
![](https://article-images.zsxq.com/FuS310mqSNBrYP_EWpvBZLHck5ak)

##   
**01 总体概述**

当 「**KafkaController**」启动之后，就会选举分区 Leader 副本，剩余的就会成为 Follower 副本。其中 Leader 副本负责消息收发，而 Follower 副本只负责拉取 Leader 副本上的消息内容，并保存到自己的 Log 日志中。当 Leader 副本挂了的时候，会从 ISR副本集合列表中选举出一个充当 Leader 副本，继续支持消息的收发功能。

这是一种的典型的主从同步机制，是分布式系统中常用的保证数据安全的机制。

那么 **Kafka 副本之间是如何同步消息的呢？在同步的过程中又是如何保证副本之间数据一致性的呢？**

带着这些问题，我们开启今天的话题，今天先来看下「**副本同步**」的实现机制。

## **02 副本一致性核心概念**

在剖析「**副本同步**」的实现机制之前，我们先来了解几个重要的概念。

## **2.1 ISR**

首先分区中所有的副本称为 「**AR 副本集合**」（Assigned Replicas）。其中所有与 Leader 副本保持一定程度同步的副本（包括Leader 副本）组成「**ISR 副本集合**」（In-Sync Replicas），也就是说 **ISR 是 AR 的一个子集**。

「**Follower 副本**」与「**Leader 副本**」保持一定程度同步，同步期间内「**Follower 副本**」相对于「**Leader 副本**」来说会有一定程度的滞后。

这里所谓「**一定程度的同步**」是指在一定时间范围内从「**Leader 副本**」拉取消息成功的「**Follower 副本**」都可以进入「**ISR 副本集合**」，这个范围可以通过 [replica.lag.time.max.ms](http://replica.lag.time.max.ms/) 参数进行配置默认是 30s。「**Leader 副本**」负责维护和跟踪「**ISR 副本集合**」中所有「**Follower 副本**」的滞后状态，当「**Follower 副本**」落后太多或失效时，「**Leader 副本**」就会把它从「**ISR 副本集合**」中剔除。

![](https://article-images.zsxq.com/FrNQ-D3tDGT7f2DydPlK0io1BXPU)

## **2.2 HW**

为了保证数据的一致性，Kafka 定义了 HW（highwater mark）高水位，每个分区中已提交消息的最高偏移量，表示消费者可安全读取的位置。当消息被成功写入到所有的 ISR（In-Sync Replica，同步副本）中，并且已经被提交，该消息的偏移量就会被包含在高水位中。**其作用就是大于等于 HW 的 Offset 消息对于消费者来说是不能进行消费的**。

简单来说，高水位就是指「**ISR 副本集合**」中同步进度最落后的副本的 LEO 值。

高水位由「**Leader 副本**」负责管理，高水位的取值是所有副本「**Leader 副本 + Follower 副本**」中 LEO 的最小值。「**Leader 副本**」负责更新 HW，会选取所有副本中最小的 LEO 作为 HW，同时「**Follower 副本**」在向「**Leader 副本**」拉取消息时，会把 HW 也返回给 「**Follower 副本**」，而「**Follower 副本**」也会维持一个 HW 的值，目的是防止「**Leader 副本**」挂了以后，「**Follower 副本**」被选举上 Leader 时有 HW 可用。

由于消息已经被存储在 「**ISR 副本集合**」中，所以消费者从 「**ISR 副本集合**」中任意一个副本读取消息的结果都是一样的， Kafka 默认只能从「**Leader 副本**」读取消息，而「**Leader 副本**」默认只能从 「**ISR 副本集合**」中进行选举。这就保证了即使 Kafka 发送了故障转移，消费者读取的结果仍然是一致的，从而保证消费者读取一致性。

下图反映了 HW 和 LEO 的关系。

![](https://article-images.zsxq.com/FkwNSu6KDTjTZ9YTzylSnTxG4t4j)

## **03 ReplicaManager 副本管理器**

在 [【服务端 Broker 源码分析系列第九篇】图解 Kafka 源码之 ReplicaManager 日志读写流程](https://articles.zsxq.com/id_5dq174gc0wji.html) 这篇中，我们深度剖析了 ReplicaManager 类源码中副本管理器是如何执行副本读写操作的。

今天我们继续来剖析 ReplicaManager 类源码，**看看副本管理器是如何管理副本的**。这里的副本包括「**副本、分区对象**」、「**副本位移值**」、「**ISR 管理**」等。

在剖析如何管理副本之前，先来总览下 ReplicaManager 类源码。

## **3.1 ReplicaManager 源码总览**

「**ReplicaManager**」是 kafka 管理副本的组件，用来维护目标 broker 上各个 topic 的副本数据信息。

「**ReplicaManager**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/ReplicaManager.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/ReplicaManager.scala)

接下来，我们就从 Replica 类的定义和重要字段这两个维度来剖析。首先看ReplicaManager类的定义。

class ReplicaManager(val config: KafkaConfig, // 配置管理类

metrics: Metrics, // 监控指标类

time: Time, // 定时器类

val zkClient: Option\[KafkaZkClient\], // ZooKeeper 客户端

scheduler: Scheduler, // Kafka调度器

val logManager: LogManager, // 日志管理器

val isShuttingDown: AtomicBoolean, // 是否已经关闭

quotaManagers: QuotaManagers, // 配额管理器

val brokerTopicStats: BrokerTopicStats, // Broker主题监控指标类

val metadataCache: MetadataCache, // Broker 元数据缓存

logDirFailureChannel: LogDirFailureChannel,

// 处理延时 PRODUCE 请求的 Purgatory

val delayedProducePurgatory: DelayedOperationPurgatory\[DelayedProduce\],

// 处理延时 FETCH 请求的 Purgatory

val delayedFetchPurgatory: DelayedOperationPurgatory\[DelayedFetch\],

// 处理延时 DELETE\_RECORDS 请求的 Purgatory

val delayedDeleteRecordsPurgatory: DelayedOperationPurgatory\[DelayedDeleteRecords\],

// 处理延时 ELECT\_LEADERS 请求的 Purgatory

val delayedElectLeaderPurgatory: DelayedOperationPurgatory\[DelayedElectLeader\],

threadNamePrefix: Option\[String\],

configRepository: ConfigRepository,

val alterIsrManager: AlterIsrManager) extends Logging with KafkaMetricsGroup {

....

}

ReplicaManager 类构造函数的字段非常多，这里我详细解释下几个比较关键的字段，它们是我们理解副本管理器的重要基础。

1.  **logManager**：它是日志管理器，负责创建和管理分区的日志对象。
2.  **metadataCache**：它是 Broker 端的元数据缓存，保存集群上分区的 Leader、ISR 等信息。每台 Broker 上的元数据缓存，是从 Controller 端的元数据缓存异步同步过来的。
3.  **logDirFailureChannel**：它是失效日志路径的处理器类。Kafka 在 1.1 版本支持 Broker 配置多个日志路径，当某个日志路径不可用之后（比如磁盘已满），Broker 仍然能够继续工作。那么就需要一整套机制来保证，在出现磁盘I/O故障时，Broker 的正常磁盘下的副本能够正常提供服务。其中，logDirFailureChannel 是暂存失效日志路径的管理器类。该功能算是Kafka 提升服务器端高可用性的一个改进。有了它之后，即使 Broker 上的单块磁盘坏掉了，整个 Broker 的服务也不会中断。
4.  **Purgatory 相关字段****：**在副本管理过程中，状态的变更大多都会引发对延时请求的处理，此时这些 Purgatory字段就派上用场了。

## **3.1.1 重要属性字段**

接着我们来看下 ReplicaManager 类中有哪些重要属性字段。这里只介绍几个对理解副本管理器至关重要的字段，

结合源码讲解它们的含义级用途。

## **controllerEpoch**

该字段作用是**用来隔离过期 Controller 发送的请求**。即旧的 Controller 发送的请求不能再被继续处理了。至于如何区分是旧 Controller 发送的请求，还是新 Controller 发送的请求，就是**看请求携带的 ControllerEpoch 值，是否等于该字段值**。

源码定义如下：

@volatile private\[server\] var controllerEpoch: Int = KafkaController.InitialControllerEpoch

它表示最新一次变更分区 Leader 的 Controller Epoch 值，其默认值为 0。当 Controller 每发生一次变更，该字段值都会+1。

在 ReplicaManager 类源码中，很多地方都会用它来**判断 Controller 发送过来的控制类请求是否合法**。如果请求中携带的 controllerEpoch 值小于该字段值，说明这个请求是旧 Controller 发出的，所以 ReplicaManager 直接拒绝该请求的处理。

另外，它参数类型是 var 表示该值是能够动态修改的，当 ReplicaManager 在处理控制类请求时会更新它。这里举几个，源码如下：

// becomeLeaderOrFollower方法中处理 LeaderAndIsrRequest 请求时

controllerEpoch = leaderAndIsrRequest.controllerEpoch

// stopReplicas方法处理StopReplicaRequest请求时

this.controllerEpoch = controllerEpoch

// maybeUpdateMetadataCache方法处理 UpdateMetadataRequest 请求时

controllerEpoch = updateMetadataRequest.controllerEpoch

另外该字段**为什么被声明为 volatile 保证其内存可见性**。你是否知道原因呢？其实主要原因就是 Broker 上接收的所有请求都是由 Kafka I/O 线程处理的，而 I/O 线程可能有多个会并发处理。

## **allPartitions**

我们知道 Kafka 中并没有所谓的分区管理器，只不过 ReplicaManager 类承担了一部分分区管理的工作，而这里的allPartitions 字段就承载了 Broker 上保存的所有分区对象数据。

源码定义如下：

protected val allPartitions \= new Pool\[TopicPartition, HostedPartition\](

valueFactory = Some(tp => HostedPartition.Online(Partition(tp, time, configRepository, this)))

)

它是分区 Partition 对象实例的容器，强调作为容器的属性，把所有分区对象汇集在一起，统一放入到一个对象池进行管理。allPartitions 会将所有分区对象初始化成 Online 状态。

其中 HostedPartition 表示 Broker 本地保存的分区对象的状态，可能的状态包括：「**不存在状态 None**」、「**在线状态 Online**」和「**离线状态 Offline**」。

另外 Partition 类是指分区对象。一个 Partition 定义和管理单个分区，它主要是利用 logManager 帮助它完成对分区底层日志的操作。

**所以 ReplicaManager 类是对分区的管理，其实都是通过 Partition 对象来完成的**。因此每个 ReplicaManager 都维护了所在 Broker 上保存的所有分区对象，而每个分区对象 Partition 下面又定义了一组副本对象 Replica。通过这样的层级关系，**ReplicaManager 通过直接操作分区对象来间接管理下属的副本对象**。

对 Broker 来说，它管理内部的分区和副本方式就是要「**确定内部这些副本中哪些是 Leader 副本、哪些是 Follower 副本**」。但是这个关系又是不断变化的，这些变化是通过 Controller 给 Broker 发送[LeaderAndIsrRequest](http://leaderandisrrequest%20/) 请求来实现的。当 Broker 端收到这类请求后，会调用 **ReplicaManager** 的[becomeLeaderOrFollower](http://becomeleaderorfollower/) 方法来处理「**成为 Leader 副本**」、「**成为 Follower 副本**」逻辑。

对于 Partititon 类的源码剖析，后面再进行新增补充吧，目前大纲没有列举。

## **replicaFetcherManager**

它的主要任务是**创建 ReplicaFetcherThread 线程**。接下来会重点剖析该线程，它的主要职责是**帮助 Follower 副本向 Leader 副本拉取消息并写入到本地日志中的一个过程**。

了解完这些基础知识点后，我们就来看看它到底是如何管理副本的。

## **3.2 分区、副本管理**

在 [【服务端 Broker 源码分析系列第九篇】图解 Kafka 源码之 ReplicaManager 日志读写流程](https://articles.zsxq.com/id_5dq174gc0wji.html) 这篇中，我们剖析了副本管理器是如何执行副本读写操作的。

那么今天我们就来剖析下它是如何管理副本的，这里的副本包括「**副本分区对象**」、「**副本位移值**」、「**ISR 管理**」。

说到这里就不得不提 [LeaderAndIsrRequest](http://leaderandisrrequest%20/) 请求，它是告诉接收该请求的Broker：在我发送给你的这些分区中，哪些分区的「**Leader 副本**」和「**Follower 副本**」在你这里。

而 [becomeLeaderOrFollower](http://becomeleaderorfollower%20/) 方法，就是具体处理 LeaderAndIsrRequest 请求的，同时也是副本管理器用来添加分区的方法。

## **3.2.1 becomeLeaderOrFollower()**

对于主从机制来说，首先要解决的问题是：**Broker 节点是怎样成为分区 Leader 副本或者 Follower 副本的？**

方法源码非常长，这里分 3 部分拆解来剖析。

1.  处理 Controller Epoch及其他准备工作。
2.  执行成为 Leader 和 Follower 副本逻辑。
3.  构造 Response。

先来看下第一部分的逻辑，**它是处理 Controller Epoch 及其他准备工作的**。

// Controller 所在 Broker 的ID

val controllerId \= leaderAndIsrRequest.controllerId

val requestPartitionStates \= leaderAndIsrRequest.partitionStates.asScala

val topicIds \= leaderAndIsrRequest.topicIds()

// 如果 LeaderAndIsrRequest 携带的 Controller Epoch 小于当前 Controller Epoch 值

if (leaderAndIsrRequest.controllerEpoch < controllerEpoch) {

stateChangeLogger.warn(s"Ignoring LeaderAndIsr request from controller $controllerId with " + s"correlation id $correlationId since its controller epoch ${leaderAndIsrRequest.controllerEpoch} is old. " + s"Latest known controller epoch is $controllerEpoch")

// 表示 Controller 已经易主，抛出相应异常

leaderAndIsrRequest.getErrorResponse(0, Errors.STALE\_CONTROLLER\_EPOCH.exception)

} else {

val responseMap \= new mutable.HashMap\[TopicPartition, Errors\]

// 更新当前 Controller Epoch 值

controllerEpoch = leaderAndIsrRequest.controllerEpoch

val partitionStates \= new mutable.HashMap\[Partition, LeaderAndIsrPartitionState\]()

// 遍历 LeaderAndIsrRequest 请求中的所有分区

requestPartitionStates.foreach { partitionState =>

val topicPartition \= new TopicPartition(partitionState.topicName, partitionState.partitionIndex)

// 从 allPartitions 中获取对应分区对象

val partitionOpt \= getPartition(topicPartition) match {

// 如果是 Offline 状态

case HostedPartition.Offline =>

stateChangeLogger.warn(s"Ignoring LeaderAndIsr request from " +

s"controller $controllerId with correlation id $correlationId " +

s"epoch $controllerEpoch for partition $topicPartition as the local replica for the " + "partition is in an offline log directory")

// 添加对象异常到 Response

responseMap.put(topicPartition, Errors.KAFKA\_STORAGE\_ERROR)

// 设置分区对象变量 partitionOpt = None

None

// 异常处理

case \_: HostedPartition.Deferred =>

throw new IllegalStateException("We should never be deferring partition metadata changes and becoming a leader or follower when using ZooKeeper")

// 如果是 Online 状态，直接赋值 partitionOpt

case HostedPartition.Online(partition) =>

Some(partition)

// 如果是 None 状态，则表示没有找到分区对象

case HostedPartition.None =>

// 创建新的分区对象

val partition \= Partition(topicPartition, time, configRepository, this)

// 新创建的分区对象加入到 allPartitions 统一管理

allPartitions.putIfNotExists(topicPartition, HostedPartition.Online(partition))

// 赋值 partitionOpt 字段

Some(partition)

}

// 检查分区的 Leader Epoch 值

partitionOpt.foreach { partition =>

val currentLeaderEpoch \= partition.getLeaderEpoch

val requestLeaderEpoch \= partitionState.leaderEpoch

val requestTopicId \= topicIds.get(topicPartition.topic)

// 检查 Topic ID 是否一致，如果不一致，则放入错误响应

if (!partition.checkOrSetTopicId(requestTopicId)) {

responseMap.put(topicPartition, Errors.INCONSISTENT\_TOPIC\_ID)

// 如果请求的 Leader Epoch 大于当前的 Leader Epoch

} else if (requestLeaderEpoch > currentLeaderEpoch) {

// 如果当前副本集合列表中包含本地 Broker Id，则更新 partitionStates，并处理该请求

if (partitionState.replicas.contains(localBrokerId))

partitionStates.put(partition, partitionState)

else {

// 如果当前副本集合列表不包含本地 Broker Id，则打印警告日志，并放入错误响应

stateChangeLogger.warn(s"Ignoring LeaderAndIsr request from controller $controllerId with " +

s"correlation id $correlationId epoch $controllerEpoch for partition $topicPartition as itself is not " +

s"in assigned replica list ${partitionState.replicas.asScala.mkString(",")}")

responseMap.put(topicPartition, Errors.UNKNOWN\_TOPIC\_OR\_PARTITION)

}

} else if (requestLeaderEpoch < currentLeaderEpoch) {

// 如果请求的 Leader Epoch 小于当前的 Leader Epoch，则打印警告日志，并放入错误响应

stateChangeLogger.warn(s"Ignoring LeaderAndIsr request from " +

s"controller $controllerId with correlation id $correlationId " +

s"epoch $controllerEpoch for partition $topicPartition since its associated " +

s"leader epoch $requestLeaderEpoch is smaller than the current " +

s"leader epoch $currentLeaderEpoch")

responseMap.put(topicPartition, Errors.STALE\_CONTROLLER\_EPOCH)

} else {

// 如果请求的 Leader Epoch 等于当前的 Leader Epoch，则打印信息日志，并放入错误响应

stateChangeLogger.info(s"Ignoring LeaderAndIsr request from " +

s"controller $controllerId with correlation id $correlationId " +

s"epoch $controllerEpoch for partition $topicPartition since its associated " +

s"leader epoch $requestLeaderEpoch matches the current leader epoch")

responseMap.put(topicPartition, Errors.STALE\_CONTROLLER\_EPOCH)

}

}

}

....

}

这里我们来总结下这部分的核心逻辑，主要做的事情：「**创建新分区**」、「**更新 Controller Epoch**」、「**校验分区 Leader Epoch**」，步骤如下：

1.  首先如果 [LeaderAndIsrRequest](http://leaderandisrrequest%20/) 携带的 Controller Epoch 小于当前 Controller Epoch 值，说明 Controller已经易主了，则构造一个 [STALE\_CONTROLLER\_EPOCH](http://stale_controller_epoch%20/) 异常并封装进 Response返回。
2.  然后更新当前缓存的 Controller Epoch 值，再取出 LeaderAndIsrRequest 请求中的分区列表，遍历它们执行下面的两步逻辑：
3.  1）、从 allPartitions 中取出对应的分区对象，分别应对这 4 种情况：
4.  如果是 Online 状态的分区，直接将其赋值给 partitionOpt 字段。
5.  如果是 Offline 状态的分区，说明该分区副本所在的 Kafka 日志路径出现I/O故障时（比如磁盘满了），需要构造对应的 [KAFKA\_STORAGE\_ERROR](http://kafka_storage_error%20/) 异常并封装进 Response，同时赋值 partitionOpt 字段为 None。
6.  如果是 None 状态的分区，则创建新分区对象，然后将其加入到 allPartitions 中进行统一管理，并赋值给partitionOpt 字段。
7.  如果是 Deferred状态的分区，则直接抛异常。
8.  2）、检查 partitionOpt 字段表示的分区的 Leader Epoch。检查的原则是要确保请求中携带的 Leader Epoch 值要大于当前缓存的 Leader Epoch，否则就说明是旧 Controller 发送的请求，就直接忽略它。

![](https://article-images.zsxq.com/lvqYQMRRKB7lFCtv9cNaIeu560LS)

这里梳理下 [LeaderAndIsrRequest](http://leaderandisrrequest/) 请求的核心属性，方便你理解：

{

"controllerId" : 0,

"controllerEpoch" : 3,

"topicStates" : \[{

"topicName" : "message3",

"partitionStates" : {

"partitionIndex" : 1,

"leader" : 0,

"leaderEpoch" : 1,

"isr" : \[0, 1\],

"replicas" : \[1, 0, 2\]

},

"liveLeaders" : \[{

"brokerId" : 0,

"hostName" : "xxxxx",

"port" : 9292

}\]

}\]

}

这里的 Partition 实例存储了分区的核心信息，关键属性如下：

1.  log ： Log 实例。
2.  remoteReplicaMap ： 存储该分区的所有副本信息（不包括当前节点）。
3.  isrState : 存储该分区的 ISR 信息。
4.  leaderReplicaIdOpt ： 该分区 Leader 副本所在 Broker 的 ID。
5.  leaderEpoch ： 分区的 LeaderEpoch， 后面篇章再介绍。

接着来看第二部分的逻辑，**它是执行成为 Leader 副本和 Follower 副本的**。

// 确定 Broker 上副本是哪些分区的 Leader 副本

val partitionsToBeLeader \= partitionStates.filter { case (\_, partitionState) =>

partitionState.leader == localBrokerId

}

// 确定 Broker 上副本是哪些分区的 Follower 副本

val partitionsToBeFollower \= partitionStates.filter { case (k, \_) => !partitionsToBeLeader.contains(k) }

val highWatermarkCheckpoints \= new LazyOffsetCheckpoints(this.highWatermarkCheckpoints)

// 调用 makeLeaders 方法为 partitionsToBeLeader 所有分区执行【成为Leader副本】的逻辑

val partitionsBecomeLeader \=

if (partitionsToBeLeader.nonEmpty)

makeLeaders(controllerId, controllerEpoch, partitionsToBeLeader, correlationId, responseMap，highWatermarkCheckpoints)

else

Set.empty\[Partition\]

// 调用 makeFollowers 方法为 partitionsToBeFollower 所有分区执行【成为Follower副本】的逻辑

val partitionsBecomeFollower \=

if (partitionsToBeFollower.nonEmpty)

makeFollowers(controllerId, controllerEpoch, partitionsToBeFollower, correlationId, responseMap, highWatermarkCheckpoints)

else

Set.empty\[Partition\]

val followerTopicSet \= partitionsBecomeFollower.map(\_.topic).toSet

// 移除对应的监控指标

updateLeaderAndFollowerMetrics(followerTopicSet)

// 遍历 leaderAndIsrRequest 请求中的所有分区

leaderAndIsrRequest.partitionStates.forEach { partitionState =>

val topicPartition \= new TopicPartition(partitionState.topicName, partitionState.partitionIndex)

// 如果有分区的本地日志为空，说明底层的日志路径不可用，标记该分区为 Offline 状态

if (localLog(topicPartition).isEmpty)

markPartitionOffline(topicPartition)

}

protected def updateLeaderAndFollowerMetrics(newFollowerTopics: Set\[String\]): Unit = {

// 对于当前 Broker 成为 Leader 副本的主题，移除它们之前的 Follower 副本监控指

val leaderTopicSet \= leaderPartitionsIterator.map(\_.topic).toSet

newFollowerTopics.diff(leaderTopicSet).foreach(brokerTopicStats.removeOldLeaderMetrics)

// 对于当前 Broker 成为 Follower 副本的主题，移除它们之前的 Leader 副本监控指标

leaderTopicSet.diff(newFollowerTopics).foreach(brokerTopicStats.removeOldFollowerMetrics)

}

这里我们来总结下这部分的核心逻辑，步骤如下：

1.  首先确定该 Broker 上副本是哪些分区的 Leader 副本、Follower 副本。判断的依据是看[LeaderAndIsrRequest](http://leaderandisrrequest%20/) 请求中分区的 Leader 信息，是不是与该 BrokerID 相同。如果相同，则表明该Broker是这个分区的Leader，否则表示当前 Broker 是这个分区的 Follower。
2.  一旦确定了这两个分区集合，接着分别调用 [makeLeaders](http://makeleaders%20/) 和 [makeFollowers](http://makefollowers%20/) 方法，正式执行成为 Leader 副本和 成为 Follower 副本。之后，对于那些当前 Broker 成为 Follower 副本的主题，移除它们之前的 Leader副本监控指标，以防出现系统资源泄露的问题。同样地对于那些当前 Broker 成为 Leader 副本的主题，移除它们之前的 Follower 副本监控指标。
3.  最后如果有分区的本地日志为空，说明底层的日志路径不可用，那么标记该分区为 Offline 状态。这里标记为Offline状态，主要是两步：
4.  更新 allPartitions 中分区的状态。
5.  移除对应分区的监控指标。

![](https://article-images.zsxq.com/lgjOw33g0X8CeWA6To-s_H70S3Qk)

接着来看第三部分的逻辑，**它是封装 Response 对象的**。

// 启动高水位检查点专属线程，定期将 Broker 上所有非 Offline 分区的高水位值写入到检查点文件

startHighWatermarkCheckPointThread()

// 添加日志路径数据迁移线程

maybeAddLogDirFetchers(partitionStates.keySet, highWatermarkCheckpoints)

// 关闭空闲副本拉取线程

replicaFetcherManager.shutdownIdleFetcherThreads()

// 关闭空闲日志路径数据迁移线程

replicaAlterLogDirsManager.shutdownIdleFetcherThreads()

// 执行 Leader 变更之后的回调逻辑

onLeadershipChange(partitionsBecomeLeader, partitionsBecomeFollower)

// 构造 LeaderAndIsrRequest 请求的 Response 并返回

if (leaderAndIsrRequest.version() < 5) {

val responsePartitions \= responseMap.iterator.map { case (tp, error) =>

new LeaderAndIsrPartitionError()

          .setTopicName(tp.topic)

          .setPartitionIndex(tp.partition)

          .setErrorCode(error.code)

   }.toBuffer

new LeaderAndIsrResponse(new LeaderAndIsrResponseData()

        .setErrorCode(Errors.NONE.code)

        .setPartitionErrors(responsePartitions.asJava), leaderAndIsrRequest.version())

} else {

val topics \= new mutable.HashMap\[String, List\[LeaderAndIsrPartitionError\]\]

    responseMap.asJava.forEach { case (tp, error) =>

if (!topics.contains(tp.topic)) {

          topics.put(tp.topic, List(

new LeaderAndIsrPartitionError()

                 .setPartitionIndex(tp.partition)

              .setErrorCode(error.code)))

        } else {

          topics.put(tp.topic,

new LeaderAndIsrPartitionError()

            .setPartitionIndex(tp.partition)

               .setErrorCode(error.code)::topics(tp.topic))

        }

    }

val topicErrors \= topics.iterator.map { case (topic, partitionError) =>

new LeaderAndIsrTopicError()

                .setTopicId(topicIds.get(topic))

                .setPartitionErrors(partitionError.asJava)

    }.toBuffer

new LeaderAndIsrResponse(new LeaderAndIsrResponseData()

        .setErrorCode(Errors.NONE.code)

        .setTopics(topicErrors.asJava), leaderAndIsrRequest.version())

    }

这里我们来总结下这部分的核心逻辑，步骤如下：

1.  首先启动一个专属线程来执行高水位值持久化，定期地将 Broker 上所有非 Offline 分区的高水位值写入检查点文件。该线程是个后台线程，默认每5秒执行一次。
2.  同时添加日志路径数据迁移线程。该线程的主要作用是：将路径1上面的数据搬移到路径2上。该功能是 Kafka支持 JBOD（Just a Bunch of Disks）的重要前提。
3.  接着关闭空闲副本拉取线程和空闲日志路径数据迁移线程。判断空闲与否的主要条件是，分区Leader/Follower 角色调整之后，是否存在不再使用的拉取线程了。要确保及时关闭那些不再被使用的线程对象。
4.  执行 LeaderAndIsrRequest 请求的回调处理逻辑。这里的回调逻辑，实际上只是对Kafka两个内部主题（consumer\_offsets 和 transaction\_state）有用，这里可以忽略。
5.  最后最重要的任务：构造 [LeaderAndIsrRequest](http://leaderandisrrequest%20/) 请求的 Response，然后将新创建的 Response 返回。

![](https://article-images.zsxq.com/liAQEBL3ggrjVNQv2rfTvWfggQut)

## **3.2.2 makeLeaders()**

private def makeLeaders(controllerId: Int, // Controller 所在 Broker 的ID,该字段只是用于日志输出，无其他实际用途

controllerEpoch: Int, // Controller Epoch 值，它是 Controller 版本号,该字段用于日志输出使用，无其他实际用途。

partitionStates: Map\[Partition, LeaderAndIsrPartitionState\], // LeaderAndIsrRequest请求中携带的分区信息，包括每个分区的Leader是谁、ISR都有哪些等数据。

correlationId: Int, // 请求的 Correlation 字段，只用于日志调试

responseMap: mutable.Map\[TopicPartition, Errors\], // 按照主题分区分组的异常错误集合

highWatermarkCheckpoints: OffsetCheckpoints): Set\[Partition\] = { // 操作磁盘上高水位检查点文件的工具类

val traceEnabled \= stateChangeLogger.isTraceEnabled

// 1、使用 Errors.NONE 初始化 ResponseMap

partitionStates.keys.foreach { partition =>

if (traceEnabled)

stateChangeLogger.trace(s"Handling LeaderAndIsr request correlationId $correlationId from " + s"controller $controllerId epoch $controllerEpoch starting the become-leader transition for " + s"partition ${partition.topicPartition}")

responseMap.put(partition.topicPartition, Errors.NONE)

}

val partitionsToMakeLeaders \= mutable.Set\[Partition\]()

try {

// 2、停止消息拉取

replicaFetcherManager.removeFetcherForPartitions(partitionStates.keySet.map(\_.topicPartition))

stateChangeLogger.info(s"Stopped fetchers as part of LeaderAndIsr request correlationId $correlationId from " + s"controller $controllerId epoch $controllerEpoch as part of the become-leader transition for " + s"${partitionStates.size} partitions")

// 3、遍历请求中携带的所有分区

partitionStates.forKeyValue { (partition, partitionState) =>

try {

// 真正执行成为 Leader 的操作

if (partition.makeLeader(partitionState, highWatermarkCheckpoints))

// 将分区加入到成为 Leader 副本的分区列表

partitionsToMakeLeaders += partition

else

....

} catch {

case e: KafkaStorageException =>

....

val dirOpt \= getLogDir(partition.topicPartition)

error(s"Error while making broker the leader for partition $partition in dir $dirOpt", e)

// 把 KAFKA\_SOTRAGE\_ERRROR 异常封装到 Response 中

responseMap.put(partition.topicPartition, Errors.KAFKA\_STORAGE\_ERROR)

}

}

} catch {

case e: Throwable =>

....

// Re-throw the exception for it to be caught in KafkaApis

throw e

}

....

// 返回成为 Leader 副本的分区列表

partitionsToMakeLeaders

}

该方法主要**执行成为 Leader 副本的逻辑，**步骤如下：

1.  首先将给定的一组分区的状态全部初始化成Errors.None。
2.  然后，停止为这些分区服务的所有拉取线程。毕竟该 Broker 现在是这些分区的 Leader 副本了，不再是Follower 副本了，所以没有必要再使用拉取线程了。
3.  接着调用 [Partition#makeLeader](http://partition/#makeLeader%20) 方法，去更新给定一组分区的 Leader 分区信息。该方法保存分区的 Leader和 ISR 信息，同时「**创建必要的日志对象**」、「**重置远端 Follower 副本的 LEO 值**」。
4.  最后当 makeLeaders 方法执行完 [Partition#makeLeader](http://partition/#makeLeader) 后，如果当前 Broker 成功地成为了该分区的 Leader 副本，就返回 True，表示新 Leader 已经配置成功，否则表示处理失败。如果成功设置了 Leader，那么就把该分区加入到已成功设置 Leader 的分区列表中，并返回该列表。

这里通过一张图来梳理其流程：

![](https://article-images.zsxq.com/FjxHalxZDQA-PmJ0zp5QNRXbpyE4)

接着来看重点 makeLeader 方法，它是 Partition 类的方法。

## **3.2.3 makeLeader()**

def makeLeader(partitionState: LeaderAndIsrPartitionState,

highWatermarkCheckpoints: OffsetCheckpoints): Boolean = {

// 使用 leaderIsrUpdateLock 加锁，避免多线程同时操作

val (leaderHWIncremented, isNewLeader) = inWriteLock(leaderIsrUpdateLock) {

// 记录做出领导决策的控制器的 epoch，用于后续更新 isr 时在 ZooKeeper 路径中维护决策者控制器的 epoch

controllerEpoch = partitionState.controllerEpoch

// 获取 ISR 副本集合列表

val isr \= partitionState.isr.asScala.map(\_.toInt).toSet

// 将在 AR 副本集合列表的加进来

val addingReplicas \= partitionState.addingReplicas.asScala.map(\_.toInt)

// 将不在 AR 副本集合列表的移除

val removingReplicas \= partitionState.removingReplicas.asScala.map(\_.toInt)

// 更新以及分配副本

updateAssignmentAndIsr(

assignment = partitionState.replicas.asScala.map(\_.toInt),

isr = isr,

addingReplicas = addingReplicas,

removingReplicas = removingReplicas

)

try {

// 如果日志不存在，则创建日志

createLogIfNotExists(partitionState.isNew, isFutureReplica = false, highWatermarkCheckpoints)

} catch {

// 如果创建日志时出现 ZooKeeper 客户端异常，则打印错误日志，并返回 false（表示创建失败）

case e: ZooKeeperClientException =>

stateChangeLogger.error(s"A ZooKeeper client exception has occurred and makeLeader will be skipping the " + s"state change for the partition $topicPartition with leader epoch: $leaderEpoch ", e)

return false

}

// Leader 日志

val leaderLog \= localLogOrException

// Leader LEO

val leaderEpochStartOffset \= leaderLog.logEndOffset

....

// 缓存 Leader Epoch，只有当有日志目录时才持久化

leaderEpoch = partitionState.leaderEpoch

leaderEpochStartOffsetOpt \= Some(leaderEpochStartOffset)

zkVersion = partitionState.zkVersion

// 清除任何挂起的 AlterIsr 请求，并检查副本状态

alterIsrManager.clearPending(topicPartition)

// 在短时间内连续进行多次领导选举时，一个 follower 可能有比新 leader 的日志更晚的 epoch 的记录。为了确保这些 follower 可以截断到正确的偏移量，我们必须缓存新的 leader epoch 和起始偏移量，因为它应该比任何一个 follower 试图查询的 epoch 大。

leaderLog.maybeAssignEpochStartOffset(leaderEpoch, leaderEpochStartOffset)

// 它代表该 Broker 成为分区的新 Leader 副本（分区原 Leader 副本不是当前分区）

val isNewLeader \= !isLeader

val curTimeMs \= time.milliseconds

// 初始化副本的 lastCaughtUpTime、lastFetchTimeMs 和 lastFetchLeaderLogEndOffset

remoteReplicas.foreach { replica =>

val lastCaughtUpTimeMs \= if (isrState.isr.contains(replica.brokerId)) curTimeMs else 0L

replica.resetLastCaughtUpTime(leaderEpochStartOffset, curTimeMs, lastCaughtUpTimeMs)

}

// 判断是否是新的 Leader

if (isNewLeader) {

// 将本地副本标记为 Leader，并重置远程副本的日志结束偏移量

leaderReplicaIdOpt = Some(localBrokerId)

// 重置远端 Follower 副本的 LEO

remoteReplicas.foreach { replica =>

replica.updateFetchState(

followerFetchOffsetMetadata = LogOffsetMetadata.UnknownOffsetMetadata,

followerStartOffset = Log.UnknownOffset,

followerFetchTimeMs = 0L,

leaderEndOffset = Log.UnknownOffset)

}

}

// we may need to increment high watermark since ISR could be down to 1

// 如果满足更新 ISR 的条件，就更新 HW 信息。

(maybeIncrementLeaderHW(leaderLog), isNewLeader)

}

// 一些延迟操作可能在高水位标记改变后被解除阻塞

if (leaderHWIncremented)

tryCompleteDelayedRequests()

// 返回新 Leader

isNewLeader

}

该方法主要用来**更新给定一组分区的 Leader 分区信息的逻辑，**步骤如下：

1.  使用 leaderIsrUpdateLock 加锁，避免多线程同时操作。
2.  首先，获取 ISR 副本集合列表、将在 AR 副本集合列表的加进来、 将不在 AR 副本集合列表的移除。
3.  接着，更新以及分配副本：**它是利用 LeaderAndIsr 请求内容去更新分区信息，包括副本集合 Partition#remoteReplicasMap、ISR 集合 Parititon#isrState**。
4.  如果日志不存在，则创建日志对象。如果创建日志时出现 ZooKeeper 客户端异常，则打印错误日志，并返回 false（表示创建失败）。
5.  缓存 Leader Epoch，只有当有日志目录时才持久化。
6.  清除任何挂起的 AlterIsr 请求，并检查副本状态。
7.  在短时间内连续进行多次领导选举时，一个 follower 可能有比新 leader 的日志更晚的 epoch 的记录。为了确保这些 follower 可以截断到正确的偏移量，我们必须缓存新的 leader epoch 和起始偏移量，因为它应该比任何一个 follower 试图查询的 epoch 大。
8.  判断是否是新的 Leader，如果是：
9.  将本地副本标记为 Leader，并重置远程副本的日志结束偏移量。
10.  重置远端 Follower 副本的 LEO。
11.  如果满足更新 ISR 的条件，就更新 HW 信息。更新成功返回 leaderHWIncremented。
12.  如果更新成功 leaderHWIncremented，则执行一些延迟操作可能在高水位标记改变后被解除阻塞。
13.  返回新 Leader 。

这里简单总结下：该方法保存分区的 Leader和 ISR 信息，同时「**创建必要的日志对象**」、「**重置远端 Follower 副本的 LEO 值**」。

这里的远端 Follower 副本，是什么意思呢？它是指保存在 Leader 副本本地内存中的一组 Follower 副本集合，在源码中用字段 remoteReplicas 来表示。

当 ReplicaManager 在处理 [FETCH](http://fetch%20/) 拉取请求时，会更新 remoteReplicas 中副本对象的 LEO 值。同时，Leader副本会将自己更新后的 LEO 值与 remoteReplicas 中副本的 LEO 值进行比较，来决定是否「**抬高**」高水位值。

而 makeLeader 方法的一个重要步骤，就是要「**重置这组远端 Follower 副本的 LEO 值**」。

这里通过一张图来梳理其流程：

![](https://article-images.zsxq.com/lvymEiKWoQAFJxaL1LtH2B6-oasC)

## **3.2.4 maybeIncrementLeaderHW()**

该方法主要用来**尝试更新 Leader 副本的高水位值**，源码如下：

//

private def maybeIncrementLeaderHW(leaderLog: Log, curTime: Long = time.milliseconds): Boolean = {

// 初始化新的高水位标记为Leader的日志结束偏移量元数据 leaderLog.logEndOffsetMetadata

var newHighWatermark = leaderLog.logEndOffsetMetadata

// 对于每个远程副本 (remoteReplicasMap.values)，进行以下操作：

remoteReplicasMap.values.foreach { replica =>

// 判断副本的日志结束偏移量元数据 replica.logEndOffsetMetadata.messageOffset 是否小于新的高水位标记 newHighWatermark.messageOffset

if (replica.logEndOffsetMetadata.messageOffset < newHighWatermark.messageOffset &&

// 如果满足以上条件，并且副本的最后追赶时间与当前时间的差值小于等于 replicaLagTimeMaxMs，或者副本的brokerId 存在于 isrState.maximalIsr 中

(curTime - replica.lastCaughtUpTimeMs <= replicaLagTimeMaxMs || isrState.maximalIsr.contains(replica.brokerId))) {

// 则将新的高水位标记更新为 replica.logEndOffsetMetadata

newHighWatermark = replica.logEndOffsetMetadata

}

}

// 根据 leaderLog.maybeIncrementHighWatermark(newHighWatermark) 的返回结果进行判断：

leaderLog.maybeIncrementHighWatermark(newHighWatermark) match {

// 如果返回结果是 Some(oldHighWatermark)，则表示高水位标记有更新，打印调试日志，返回 true

case Some(oldHighWatermark) =>

debug(s"High watermark updated from $oldHighWatermark to $newHighWatermark")

true

// 如果返回结果是 None，则表示新的高水位标记未触发更新，如果日志级别是 TRACE，则输出跳过更新高水位标记的详细日志，返回 false

case None =>

def logEndOffsetString: ((Int, LogOffsetMetadata)) => String = {

case (brokerId, logEndOffsetMetadata) => s"replica $brokerId: $logEndOffsetMetadata"

}

if (isTraceEnabled) {

val replicaInfo = remoteReplicas.map(replica => (replica.brokerId, replica.logEndOffsetMetadata)).toSet

val localLogInfo = (localBrokerId, localLogOrException.logEndOffsetMetadata)

trace(s"Skipping update high watermark since new hw $newHighWatermark is not larger than old value. " +

s"All current LEOs are ${(replicaInfo + localLogInfo).map(logEndOffsetString)}")

}

false

}

}

## **3.2.5 makeFollowers()**

上面剖析完了**将当前 Broker 成为给定分区的 Leader 副本**，接着我们来看下**将当前 Broker 成为给定分区的 Follower 副本**。

private def makeFollowers(controllerId: Int, // Controller 所在 Broker 的 Id

controllerEpoch: Int, // Controller Epoch值

partitionStates: Map\[Partition, LeaderAndIsrPartitionState\], // 当前 Broker 是Follower 副本的所有分区的详细信息

correlationId: Int, // 连接请求与响应的关联字段

responseMap: mutable.Map\[TopicPartition, Errors\], // 封装 LeaderAndIsrRequest 请求处理结果的字段

highWatermarkCheckpoints: OffsetCheckpoints) : Set\[Partition\] = { // 操作高水位检查点文件的工具类

val traceLoggingEnabled \= stateChangeLogger.isTraceEnabled

// 遍历 partitionStates 所有分区

partitionStates.forKeyValue { (partition, partitionState) =>

....

// 将所有分区的处理结果的状态初始化为 Errors.NONE

responseMap.put(partition.topicPartition, Errors.NONE)

}

val partitionsToMakeFollower: mutable.Set\[Partition\] = mutable.Set()

try {

// TODO: Delete leaders from LeaderAndIsrRequest

// 遍历 partitionStates 所有分区

partitionStates.forKeyValue { (partition, partitionState) =>

// 拿到分区的 Leader Broker ID

val newLeaderBrokerId \= partitionState.leader

try {

// 在元数据缓存中找到 Leader Broke 对象

metadataCache.getAliveBrokers.find(\_.id == newLeaderBrokerId) match {

// Only change partition state when the leader is available

// 如果 Leader 确实存在

case Some(\_) =>

// 真正执行成为 Follower 的操作

if (partition.makeFollower(partitionState, highWatermarkCheckpoints))

// 将分区加入到成为 Followers 副本的分区列表

partitionsToMakeFollower += partition

else

// 如果失败，打印错误日志

....

// 如果 Leader 不存在

case None \=\>

....

// 依然创建出分区 Follower 副本的日志对象

// Create the local replica even if the leader is unavailable. This is required to ensure that we include

// the partition's high watermark in the checkpoint file (see KAFKA-1647)

partition.createLogIfNotExists(isNew = partitionState.isNew, isFutureReplica = false, highWatermarkCheckpoints)

}

} catch {

case e: KafkaStorageException =>

....

val dirOpt \= getLogDir(partition.topicPartition)

error(s"Error while making broker the follower for partition $partition with leader " + s"$newLeaderBrokerId in dir $dirOpt", e)

responseMap.put(partition.topicPartition, Errors.KAFKA\_STORAGE\_ERROR)

}

}

// 移除现有 Fetcher 线程

replicaFetcherManager.removeFetcherForPartitions(partitionsToMakeFollower.

map(\_.topicPartition))

....

// 尝试完成延迟请求

partitionsToMakeFollower.foreach { partition =>

completeDelayedFetchOrProduceRequests(partition.topicPartition)

}

if (isShuttingDown.get()) {

....

} else {

// 为需要将当前 Broker 设置为 Follower 副本的分区确定 Leader Broker 和起始读取位移值fetchOffset

val partitionsToMakeFollowerWithLeaderAndOffset \= partitionsToMakeFollower.map { partition =>

val leader \= metadataCache.getAliveBrokers.find(\_.id == partition.leaderReplicaIdOpt.get).get

.brokerEndPoint(config.interBrokerListenerName)

val log \= partition.localLogOrException

val fetchOffset \= initialFetchOffset(log)

partition.topicPartition -> InitialFetchState(leader, partition.getLeaderEpoch, fetchOffset)

}.toMap

// 使用上一步确定的 Leader Broker 和 fetchOffset 添加新的 Fetcher 线程

replicaFetcherManager.addFetcherForPartitions(partitionsToMakeFollowerWithLeaderAndOffset)

}

} catch {

case e: Throwable =>

....

// Re-throw the exception for it to be caught in KafkaApis

throw e

}

....

// 返回需要将当前 Broker 设置为 Follower 副本的分区列表

partitionsToMakeFollower

}

总体来看，该方法可以分为两大步：

1.  遍历 partitionStates 中的所有分区，然后执行【成为 Follower 副本】的操作。
2.  执行其他动作，主要包括重建 Fetcher 线程、完成延时请求、为需要将当前 Broker 设置为 Follower 副本的分区确定 Leader Broker 和起始读取位移值fetchOffset并对其添加新的 Fetcher 线程等。

整体步骤如下：

1.  所有分区的处理结果状态初始化为Errors.NONE。
2.  遍历 partitionStates 中的所有分区，依次为每个分区执行以下逻辑：
3.  从分区的详细信息中获取分区的 Leader Broker ID。
4.  拿着上一步获取的 Broker ID，去 Broker 元数据缓存中找到 Leader Broker 对象。
5.  如果 Leader 对象存在，则执行 [Partition#makeFollower](http://%20partition/#makeFollower%20) 方法将当前 Broker 配置成该分区的 Follower 副本。如果执行成功，就说明当前 Broker 被成功配置为指定分区的 Follower 副本，那么将该分区加入到结果返回集中。
6.  如果 Leader 对象不存在，依然创建出分区 Follower 副本的日志对象。
7.  移除现有 Fetcher 线程。因为 Leader 可能已经更换了，所以要读取的 Broker 以及要读取的位移值都可能随之发生变化。
8.  为需要将当前 Broker 设置为 Follower 副本的分区，确定 Leader Broker 和起始读取位移值 fetchOffset。这些信息都已经在 LeaderAndIsrRequest 中了。
9.  使用上一步确定的 Leader Broker 和 fetchOffset 添加新的 Fetcher 线程。
10.  最后返回需要将当前 Broker 设置为 Follower 副本的分区列表。

这里通过一张图来梳理其流程：

![](https://article-images.zsxq.com/lsrPyt6v4i-kZ8_yYHuZVMlPnA5i)

接着来看重点 makeFollower 方法，它是 Partition 类的方法。

## **3.2.6 makeFollower()**

def makeFollower(partitionState: LeaderAndIsrPartitionState,

highWatermarkCheckpoints: OffsetCheckpoints): Boolean = {

// 使用 leaderIsrUpdateLock 加锁，避免多线程同时操作

inWriteLock(leaderIsrUpdateLock) {

// 分区 Leader 所在 Broker id

val newLeaderBrokerId \= partitionState.leader

// 旧 LeaderEpoch

val oldLeaderEpoch \= leaderEpoch

// 记录控制器作出领导权决策的 epoch。在更新 isr 时保留决策者的 epoch 值，有助于维护 zookeeper 路径中的正确值。

controllerEpoch = partitionState.controllerEpoch

// 更新以及分配副本

updateAssignmentAndIsr(

assignment = partitionState.replicas.asScala.iterator.map(\_.toInt).toSeq,

isr = Set.empty\[Int\], // 清空 ISR

addingReplicas = partitionState.addingReplicas.asScala.map(\_.toInt),

removingReplicas = partitionState.removingReplicas.asScala.map(\_.toInt)

)

try {

// 如果日志不存在，则创建日志

createLogIfNotExists(partitionState.isNew, isFutureReplica = false, highWatermarkCheckpoints)

} catch {

// 如果创建日志时出现 ZooKeeper 客户端异常，则打印错误日志，并返回 false（表示创建失败）

case e: ZooKeeperClientException =>

....

return false

}

// Follower 日志

val followerLog \= localLogOrException

val leaderEpochEndOffset \= followerLog.logEndOffset

....

// 记录新的 Leader epoch，清除前任 Leader 信息，更新 zkVersion 和 isLeader 属性

leaderEpoch = partitionState.leaderEpoch

leaderEpochStartOffsetOpt \= None

zkVersion \= partitionState.zkVersion

// 清除任何挂起的 AlterIsr 请求，并检查副本状态

alterIsrManager.clearPending(topicPartition)

// 如果该副本之前就是 Leader，且被迫放弃领导权，返回 false；否则返回 true。

if (leaderReplicaIdOpt.contains(newLeaderBrokerId) && leaderEpoch == oldLeaderEpoch) {

false

} else {

// 重置 Leader 副本的 Broker ID

leaderReplicaIdOpt = Some(newLeaderBrokerId)

true

}

}

}

该方法主要用来**更新给定一组分区的 Follower 分区信息的逻辑，**步骤如下：

1.  使用 leaderIsrUpdateLock 加锁，避免多线程同时操作。
2.  首先，记录新 Leader 的 ID 和旧 Leader 的 epoch， 记录控制器作出领导权决策的 epoch。在更新 isr 时保留决策者的 epoch 值，有助于维护 zookeeper 路径中的正确值。
3.  更新以及分配副本。
4.  如果日志不存在，则创建日志，如果创建日志时出现 ZooKeeper 客户端异常，则打印错误日志，并返回 false（表示创建失败）。
5.  记录新的 Leader epoch，清除前任 Leader 信息，更新 zkVersion 和 isLeader 属性。
6.  清除任何挂起的 AlterIsr 请求，并检查副本状态。
7.  如果该副本之前就是 Leader，且被迫放弃领导权，返回 false；否则返回 true。

这里通过一张图来梳理其流程：

![](https://article-images.zsxq.com/Fhp6ZLuGZhn_OAODkJqGfPE8bJRk)

## **3.2.7 HW 和 LEO 更新机制总结**

通过上面的剖析，现在我们知道了每个副本对象都保存了一组 HW 值和 LEO 值，另外在 Leader 副本所在的 Broker 上，还保存了其他 Follower 副本的 LEO 值，如下图：

  
![](https://article-images.zsxq.com/FiOlPeuIi1PNgCYuY04NvsN0O1uk)

如图所示，我们可以看到，Broker 0 上保存了某分区的 Leader 副本和所有 Follower 副本的 LEO 值，而 Broker 1 上仅仅保存了该分区的某个 Follower 副本。**分Kafka 副本机制在运行过程中，会更新 Broker 1 上 Follower 副本的高水位和 LEO 值，同时也会更新 Broker 0 上 Leader 副本的高水位和 LEO 以及所有 Follower 副本的 LEO，但它不会更新所有 Follower 的高水位值，也就是图中标记为粉红色的部分**。

**看到这里你是否有以下困惑？**

1.  为什么 Leader 副本所在的 Broker 上，还保存了其他 Follower 副本的 LEO 值？
2.  为什么 Leader 副本所在的 Broker 上不会更新 Follower 副本 HW？

别着急，华仔带你看下源码，为你解惑。在上面剖析的 [Partition#makeLeader](http://kafka.cluster.partition/#makeLeader) 方法中有这一段源码:

![](https://article-images.zsxq.com/FkpwUWE1XicX0BGsd4V1-zCjKRft)

Leader 副本所在的 Broker 上只有重置更新远程副本的 LEO，并没有远程副本的 HW。

**此时你是否又有以下困惑？**

1.  为什么要在 Broker 0 上保存这些远程副本呢？
2.  Broker 0 不会更新远程副本 HW，那远程副本的 HW 的更新机制又是怎样的呢？

第一个问题，首先 Broker 0 上保存这些远程副本的主要作用是：**帮助 Leader 副本来确定其高水位值，即分区的高水位**。

第二个问题我们直接来看下 HW 和 LEO 被更新的时机：

![](https://article-images.zsxq.com/ljnI57W6laGAA5RdUYGzzKCm4dGo)

接下来,我们从 Leader 副本 和 Follower 副本两个维度，来总结一下高水位和 LEO 的更新机制。

## **Leader 副本**

处理生产者请求的逻辑如下：

1.  写入消息到本地磁盘
2.  更新分区高水位值
3.  获取 Leader 副本所在 Broker 端保存的所有远程副本 LEO 值（LEO-1，LEO-2，……，LEO-n）
4.  获取 Leader 副本高水位值：currentHW
5.  更新 currentHW = max{currentHW, min（LEO-1, LEO-2, ……，LEO-n）}

处理 Follower 副本拉取消息的逻辑如下：

1.  读取磁盘（或页缓存）中的消息数据。
2.  使用 Follower 副本发送请求中的位移值更新远程副本 LEO 值。
3.  更新分区高水位值（具体步骤与处理生产者请求的步骤相同）。

## **Follower 副本**

从 Leader 副本拉取消息的处理逻辑逻辑如下：

1.  写入消息到本地磁盘。
2.  更新 LEO 值。
3.  更新高水位值。
4.  获取 Leader 发送的高水位值：currentHW
5.  获取步骤 2 中更新过的 LEO 值：currentLEO
6.  更新高水位为 min(currentHW, currentLEO)

首先 Follower 在从 Leader 拉取数据的同时会带上自己的 LEO 的值，可是在**实际情况下有可能 P0 的副本可能会有多个**。它们也从 Leader 副本同步数据并带上自己的LEO。**Leader 副本就会记录这些 Follower 同步过来的 LEO，然后取最小的 LEO 值作为 HW 值** **。**

之所以要这么做是为了保证如果 Leader 副本宕机时，集群会从其它的 Follower 副本里面选举出一个新的 Leader 副本。这时候无论选举了哪一个节点作为 Leader，都能保证存在当前待消费的数据，从而保证数据的安全性。

那么此时 Follower 自身 HW 的值如何确定，即 **帮 Follower 获取数据时也会带上 Leader 副本的 HW 值，然后和自身的 LEO 值取一个较小的值作为自身的 HW 值** 。

![](https://article-images.zsxq.com/Fl-4x--93LT1GWaRLYdDt4XMx2je)

接着我们来 ISR 时如何管理的。

## **3.3 ISR 副本集合列表管理**

副本管理器另外一个重要的功能，那就是管理 ISR 副本集合列表。

多个 Follower 副本向 Leader 副本拉取消息的过程中，每个 Follower 副本根据自身状况拉取的进度都不太一样，有的 Follower 副本的消息同步会逐渐落后，此时应该把这个 Follower 副本踢出 ISR 副本集合列表。但是，过段时间这个 Follower 副本同步消息又赶上来了，满足进入 ISR 副本集合列表的标准（[replica.lag.time.max.ms](http://replica.lag.time.max.ms/) 默认是 30s）。

ISR 副本集合列表的同步分为三个阶段：「**ISR 信息构造**」、「**ISR 副本集合列表收缩**」、「**ISR 信息发送和响应处理**」。

## **3.3.1 ISR 信息构造**

在 [【服务端 Broker 源码分析系列第九篇】图解 Kafka 源码之 ReplicaManager 日志读写流程](https://articles.zsxq.com/id_5dq174gc0wji.html) 介绍了ReplicaManager 处理获取消息的过程，这个方法同时支持消费者和 Follower 副本向 Leader 副本拉取消息。

对于 Follower 副本来说会**根据获取消息的情况判断是否加入 ISR 副本集合列表** 。如下：

  
![](https://article-images.zsxq.com/FsD5V33zFXsCXDymkPMnoVuoeDla)

当 Follower 副本发送获取消息的请求到 Leader 副本时，Leader 副本会调用图中红框的方法。因为**对于 kafka 来说，只有 Leader 副本才能处理读写请求**，**即只有 Leader 副本才有管理 ISR 副本集合的权限**。

private def updateFollowerFetchState(followerId: Int,

readResults: Seq\[(TopicPartition, LogReadResult)\]): Seq\[(TopicPartition, LogReadResult)\] = {

// 遍历每个读取结果

readResults.map { case (topicPartition, readResult) =>

// 判断是否有异常

val updatedReadResult \= if (readResult.error != Errors.NONE) {

....

// 如果读取结果出错，则跳过更新 follower 的 fetch 状态

readResult

} else {

// 判断此副本是否在在线副本集合里。

onlinePartition(topicPartition) match {

//如果在在线副本集合里就更新 Follower 副本拉取状态。

case Some(partition) =>

if (partition.updateFollowerFetchState(followerId,

followerFetchOffsetMetadata = readResult.info.fetchOffsetMetadata,

followerStartOffset = readResult.followerLogStartOffset,

followerFetchTimeMs = readResult.fetchTimeMs,

leaderEndOffset = readResult.leaderLogEndOffset)) {

readResult

} else {

....

// 如果更新失败，则记录日志，并返回空的fetch信息

readResult.withEmptyFetchInfo

}

case None \=\>

...

// 如果分区不存在，则记录日志，并返回原始的读取结果

readResult

}

}

topicPartition -> updatedReadResult

}

}

从源码可以看到如果没有异常错误的话，会判断此副本是否在在线副本集合里。如果在在线副本集里，就说明这个副本在 ISR 副本集合列表里，然后调用 [partition#updateFollowerFetchState](http://%20partition/#updateFollowerFetchState) 来进一步做处理，最终会到 [partition#expandIsr()](http://partition/#expandIsr\(\)%20) 方法。

![](https://article-images.zsxq.com/FkwWiOg4bDGcA7S5tL1jMi42I2tA)

![](https://article-images.zsxq.com/Fguk0G5wWKLr0gbfH9mpeux-9cKe)

这里就是 ISR 副本集合扩容的方法。

// 把 isr 的改变通知给 Controller 节点，然后 Controller 节点再把 isr 的信息通知给其他的 broker节点

private\[cluster\] def expandIsr(newInSyncReplica: Int): Unit = {

// This is called from maybeExpandIsr which holds the ISR write lock

// 首先确定没有发送中的 isr 变更请求，然后调用 sendAlterIsrRequest() 方法。

if (!isrState.isInflight) {

// When expanding the ISR, we can safely assume the new replica will make it into the ISR since this puts us in

// a more constrained state for advancing the HW.

sendAlterIsrRequest(PendingExpandIsr(isrState.isr, newInSyncReplica))

} else {

trace(s"ISR update in-flight, not adding new in-sync replica $newInSyncReplica")

}

}

好，接着我们继续看看 [sendAlterIsrRequest()](http://sendalterisrrequest\(\)%20/) 做了什么工作：

private def sendAlterIsrRequest(proposedIsrState: IsrState): Unit = {

// 构建要发送的 isr 信息集合

val isrToSend: Set\[Int\] = proposedIsrState match {

case PendingExpandIsr(isr, newInSyncReplicaId) => isr + newInSyncReplicaId

case PendingShrinkIsr(isr, outOfSyncReplicaIds) => isr -- outOfSyncReplicaIds

case state \=\>

isrChangeListener.markFailed()

throw new IllegalStateException(s"Invalid state $state for ISR change for partition $topicPartition")

}

// 需要把 Leader 信息和 isr 信息整合在一起发送

val newLeaderAndIsr \= new LeaderAndIsr(localBrokerId, leaderEpoch, isrToSend.toList, zkVersion)

// 进一步完善发送信息，增加 isr 对应的主题分区

val alterIsrItem \= AlterIsrItem(topicPartition, newLeaderAndIsr, handleAlterIsrResponse(proposedIsrState), controllerEpoch)

val oldState \= isrState

isrState \= proposedIsrState

// 把待发送的信息放到一个集合中，等着异步进行发送。

if (!alterIsrManager.submit(alterIsrItem)) {

// If the ISR manager did not accept our update, we need to revert back to previous state

isrState = oldState

isrChangeListener.markFailed()

throw new IllegalStateException(s"Failed to enqueue ISR change state $newLeaderAndIsr for partition $topicPartition")

}

debug(s"Enqueued ISR change to state $newLeaderAndIsr after transition to $proposedIsrState")

}

步骤如下：

1.  构建要发送的 isr 信息集合。
2.  需要把 Leader 信息和 isr 信息整合在一起发送。
3.  进一步完善发送信息，如增加 isr 对应的主题分区。
4.  把待发送的信息放到一个 map 集合 unsentIsrUpdates 中，等着异步进行发送。
5.  接着我们就要等着定时任务去消费集合 unsentIsrUpdates 中的 isr 发送信息了。

## **3.3.2 ISR 副本集合收缩**

ISR 收缩是指：把 ISR 副本集合列表中那些与 Leader 差距过大的副本移除的过程。

其中所谓的差距过大，就是 ISR 副本集合列表中 Follower 副本滞后 Leader 副本的时间，超过了 Broker 端参数[replica.lag.time.max.ms](http://replica.lag.time.max.ms/) 值的 1.5 倍。

那么为什么是 1.5 倍呢？源码如下：

def startup(): Unit = {

scheduler.schedule("isr-expiration", maybeShrinkIsr \_, period = config.replicaLagTimeMaxMs / 2, unit = TimeUnit.MILLISECONDS)

....

}

在 [ReplicaManager#startup](http://replicamanager/#startup%20) 方法在被调用时会创建一个异步线程，定时查看是否有 ISR 需要进行收缩。

这里的定时频率是 replicaLagTimeMaxMs 值的二分之一，而判断 Follower 副本是否需要被移除 ISR 的条件是，滞后程度是否超过了 replicaLagTimeMaxMs 值，即参数 [replica.lag.time.max.ms](http://replica.lag.time.max.ms/) 值。

**因此从理论上来说，滞后程度如果小于 1.5 倍 replicaLagTimeMaxMs 值的 Follower 副本，依然可能在 ISR 副本集合列表中而不会被移除**。

接下来我们来看下是否收缩的方法。

private def maybeShrinkIsr(): Unit = {

trace("Evaluating ISR list of partitions to see which replicas can be removed from the ISR")

// Shrink ISRs for non offline partitions

allPartitions.keys.foreach { topicPartition =>

onlinePartition(topicPartition).foreach(\_.maybeShrinkIsr())

}

}

可以看出，该方法会遍历该副本管理器上所有分区对象，依次为这些分区中状态为 Online 的分区，执行 [partition#maybeShrinkIsr](http://partition/#maybeShrinkIsr) 方法。这个方法的源码如下：

def maybeShrinkIsr(): Unit = {

// 判断是否需要执行 ISR 收缩

val needsIsrUpdate \= !isrState.isInflight && inReadLock(leaderIsrUpdateLock) {

needsShrinkIsr()

}

val leaderHWIncremented \= needsIsrUpdate && inWriteLock(leaderIsrUpdateLock) {

// 如果是 Leader 副本

leaderLogIfLocal.exists { leaderLog =>

// 获取不同步的副本 Id 列表

val outOfSyncReplicaIds \= getOutOfSyncReplicas(replicaLagTimeMaxMs)

// 如果存在不同步的副本 Id 列表

if (outOfSyncReplicaIds.nonEmpty) {

// 获取不同步的的副本日志

val outOfSyncReplicaLog \= outOfSyncReplicaIds.map { replicaId =>

s"(brokerId: $replicaId, endOffset: ${getReplicaOrException(replicaId).logEndOffset})"

}.mkString(" ")

// 计算收缩之后的 ISR 列表:把它们从当前 ISR 中剔除出去，然后计算得出最新的 ISR 列表

val newIsrLog \= (isrState.isr -- outOfSyncReplicaIds).mkString(",")

info(s"Shrinking ISR from ${isrState.isr.mkString(",")} to $newIsrLog. " +

s"Leader: (highWatermark: ${leaderLog.highWatermark}, endOffset: ${leaderLog.logEndOffset}). " +

s"Out of sync replicas: $outOfSyncReplicaLog.")

// 更新 ZooKeeper 中分区的 ISR 数据以及 Broker 的元数据缓存中的数据

shrinkIsr(outOfSyncReplicaIds)

// 尝试更新 Leader 副本的高水位值

maybeIncrementLeaderHW(leaderLog)

} else {

// 如果没有不同步的副本 Id 列表，什么都不做

false

}

}

}

// 如果 Leader 副本的高水位值已经抬高了

if (leaderHWIncremented)

// 尝试解锁一下延迟请求

tryCompleteDelayedRequests()

}

private def needsShrinkIsr(): Boolean = {

// 获取与 Leader 不同步的副本

leaderLogIfLocal.exists { \_ => getOutOfSyncReplicas(replicaLagTimeMaxMs).nonEmpty }

}

private\[cluster\] def shrinkIsr(outOfSyncReplicas: Set\[Int\]): Unit = {

// This is called from maybeShrinkIsr which holds the ISR write lock

// 首先确定没有发送中的 isr 变更请求

if (!isrState.isInflight) {

// When shrinking the ISR, we cannot assume that the update will succeed as this could erroneously advance the HW

// We update pendingInSyncReplicaIds here simply to prevent any further ISR updates from occurring until we get

// the next LeaderAndIsr

// 发送 AlterIsr 请求

sendAlterIsrRequest(PendingShrinkIsr(isrState.isr, outOfSyncReplicas))

} else {

trace(s"ISR update in-flight, not removing out-of-sync replicas $outOfSyncReplicas")

}

}

步骤如下：

1.  判断是否需要执行 ISR 收缩。主要的方法是，调用 needShrinkIsr 方法来获取与 Leader 不同步的副本。**如果存在这样的副本，说明需要执行 ISR 收缩**。
2.  再次获取与 Leader 不同步的副本列表，并把它们从当前 ISR 中剔除出去，然后计算得出最新的 ISR 列表。
3.  去更新 ZooKeeper 上分区的 ISR 数据以及 Broker 中元数据缓存。
4.  尝试更新 Leader 分区的高水位值。**这里很有必要检查一下是否可以抬高高水位值：如果 ISR 收缩后只剩下 Leader 副本，那么高水位的更新就不受那么多限制了**。
5.  最后，根据上一步的结果，来尝试解锁之前不满足条件的延迟操作。

这里通过一张图来梳理其流程：

![](https://article-images.zsxq.com/lqVXjP0Bl9WcokcxxDUU-xhoW4Ej)

##   
**3.3.3 ISR 信息发送和响应处理**

ISR 收缩完成之后，还需要将这个操作的结果传递给集群的其他 Broker 以同步结果。这是由 ISR 通知事件来完成的。

  
「**AlterIsrManager****.scala**」源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/AlterIsrManager.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/AlterIsrManager.scala)

在上面构造完 ISR 信息后会提交到集合中异步发送：

alterIsrManager.submit(alterIsrItem)

通过跟踪源码，它会调用 [DefaultAlterIsrManager#submit](http://defaultalterisrmanager/#submit%20) 方法，源码如下：

// Used to allow only one pending ISR update per partition (visible for testing)

private\[server\] val unsentIsrUpdates: util.Map\[TopicPartition, AlterIsrItem\] = new ConcurrentHashMap\[TopicPartition, AlterIsrItem\]()

// 提交更改 ISR 请求，将请求加入未发送队列中。如果成功将请求加入未发送队列中，则返回 true，否则返回 false

override def submit(alterIsrItem: AlterIsrItem): Boolean = {

// 将请求加入未发送队列中

val enqueued \= unsentIsrUpdates.putIfAbsent(alterIsrItem.topicPartition, alterIsrItem) == null

// 尝试立即发送 ISR 变更请求

maybePropagateIsrChanges()

// 最后返回加入结果

enqueued

}

接着来剖析下 maybePropagateIsrChanges 方法。

/\*\*

\* 如果目前没有正在发送的请求，尝试发送未发送的 ISR 变更请求。

\* 如果未发送队列不为空且没有请求正在发送中，将未发送的 ISR 变更请求加入发送队列并发送。

\*/

private\[server\] def maybePropagateIsrChanges(): Unit = {

// 判断未发送队列不为空且没有请求正在发送中

if (!unsentIsrUpdates.isEmpty && inflightRequest.compareAndSet(false, true)) {

// 复制当前未发送的 ISR 变更请求，但并没有删除，删除的时机在发送成功后收到响应的时候

val inflightAlterIsrItems \= new ListBuffer\[AlterIsrItem\]()

// 取出所有集合中要发送的 isr 信息。

unsentIsrUpdates.values().forEach(item => inflightAlterIsrItems.append(item))

// 发送请求

sendRequest(inflightAlterIsrItems.toSeq)

}

}

尝试立即发送 ISR 变更请求的步骤如下：

1.  判断未发送队列不为空且没有请求正在发送中。
2.  取出所有集合中要发送的 isr 信息，这时可以看到从集合 unsentIsrUpdates 拿到待发送的 isr 信息，但并没有删除，删除的时机在发送成功后收到响应的时候。
3.  真正的发送，核心方法是 sendRequest()。

接着我们来剖析下真正发送的方法。

private def sendRequest(inflightAlterIsrItems: Seq\[AlterIsrItem\]): Unit = {

// 1、构建发送 isr 信息

val message \= buildRequest(inflightAlterIsrItems)

debug(s"Sending AlterIsr to controller $message")

// We will not timeout AlterISR request, instead letting it retry indefinitely

// until a response is received, or a new LeaderAndIsr overwrites the existing isrState

// which causes the response for those partitions to be ignored.

// 2、向 controller 发送 isr 信息。

controllerChannelManager.sendRequest(new AlterIsrRequest.Builder(message),

new ControllerRequestCompletionHandler {

// 完成发送后的响应回调函数

override def onComplete(response: ClientResponse): Unit = {

debug(s"Received AlterIsr response $response")

val error \= try {

// 如果无权限

if (response.authenticationException != null) {

// For now we treat authentication errors as retriable. We use the

// \`NETWORK\_EXCEPTION\` error code for lack of a good alternative.

// Note that \`BrokerToControllerChannelManager\` will still log the

// authentication errors so that users have a chance to fix the problem.

Errors.NETWORK\_EXCEPTION

// 如果版本不匹配

} else if (response.versionMismatch != null) {

Errors.UNSUPPORTED\_VERSION

} else {

// 封装响应信息

val body \= response.responseBody().asInstanceOf\[AlterIsrResponse\]

// 处理响应信息

handleAlterIsrResponse(body, message.brokerEpoch, inflightAlterIsrItems)

}

} finally {

// clear the flag so future requests can proceed

// 清空发送中请求

clearInFlightRequest()

}

// check if we need to send another request right away

// 匹配错误

error match {

case Errors.NONE =>

// In the normal case, check for pending updates to send immediately

// 尝试发送

maybePropagateIsrChanges()

case \_ \=\>

// If we received a top-level error from the controller, retry the request in the near future

// 每次都会间隔50毫秒发送 isr 信息

scheduler.schedule("send-alter-isr", () => maybePropagateIsrChanges(), 50, -1, TimeUnit.MILLISECONDS)

}

}

override def onTimeout(): Unit = {

throw new IllegalStateException("Encountered unexpected timeout when sending AlterIsr to the controller")

}

})

}

真正发送 ISR 请求的步骤如下：

1.  构建发送 isr 信息。
2.  向 controller 发送 isr 信息
3.  响应处理：首先封装响应信息，然后处理响应信息。

到这个时候，我们已经把要发送的信息交给服务端网络层去做真正的网络请求。

## **04 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头从「**KafkaController**」启动会选举「**Leader 副本**」，Follower 副本会同步消息，文章开头提出了两个：**副本之间是如何同步消息的呢？在同步的过程中又是如何保证副本之间数据一致性的呢？**

2、本文先回答了第二个问题，先带大家对「**副本一致性核心概念**」的梳理。

3、接着带大家深度剖析了「**分区、副本的管理**」，让你了解副本管理器是如何管理副本和分区的，以及 HW 和 LEO 的更新机制。

4、最后带大家深度剖析了「**ISR 副本集合列表管理**」，让你了解 ISR 是如何被管理的，分为三个阶段：「**ISR 信息构造**」、「**ISR 副本集合列表收缩**」、「**ISR 信息发送和响应处理**」。

下篇我们来深度剖析「**副本同步实现原理（下）**」，来聊聊副本之间到底是如何同步消息的，大家期待，我们下期见。