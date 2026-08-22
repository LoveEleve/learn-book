大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端 Broker 启动后集群如何感知的**」，通过「**KafkaServer**」启动过程中的各类组件类库，引出「**KafkaController**」的启动过程以及「**集群 Broker 上下线处理过程**」，从今天开始，我们来深度剖析 Kafka「**Controller**」的底层源码实现，这是 Controller 系列第三篇，我们接着来深度聊聊「**Kafka 服务端控制器 Controller 元数据管理**」。

##   
**01 总体概述**

了解 Kafka 的同学，大家都知道在 Kafka 早期的版本中，「**Zookeeper**」存储着整个 Kafka 集群元数据的，那么 「**Controller**」的元数据又是怎么来的呢？

其实，它是承载了 「**Zookeeper**」上的所有元数据而已。对于集群 Broker 来说，它们不会直接与 「**Zookeeper**」通信去获取元数据，而是跟「**Controller**」进行通信去「**获取**」、「**更新**」集群元数据。在 2.8.x 版本之后，「**Controller**」就成了新的获取集群元数据的来源。

说了半天，集群的元数据到底是什么？都定义了什么内容？ 带着这个问题来揭开本文的重点内容。

接下来，我们来深度剖析了 「**元数据类**」的具体实现，本文重点来剖析下「**ControllerContext**」类，它内部封装了元数据信息，该类是「**Controller**」组件的数据容器类。

## **02 ControllerContext**

「**ControllerContext.scala**」类源码为主，其在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/ControllerContext.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/ControllerEventManager.scala)

整个源码文件基本 500 行，最重要的结构就是「**ControllerContext**」类，它内部定义了所有元数据信息，还要许多实用的工具方法。

接下来我们先来看看其定义：

class ControllerContext {

val stats \= new ControllerStats // Controller 状态信息

var offlinePartitionCount \= 0 // 离线分区数量

val shuttingDownBrokerIds \= mutable.Set.empty\[Int\] // 正在关闭中的 Broker 列表

private val liveBrokers \= mutable.Set.empty\[Broker\] // 运行中的 Broker 列表

private val liveBrokerEpochs \= mutable.Map.empty\[Int, Long\] // 运行中的 Broker 的 Epoch

var epoch: Int = KafkaController.InitialControllerEpoch // Controller Epoch

var epochZkVersion: Int = KafkaController.InitialControllerEpochZkVersion // Controller Epoch Znode 版本号

val allTopics \= mutable.Set.empty\[String\] // 集群 Topic 列表

val partitionAssignments \= mutable.Map.empty\[String, mutable.Map\[Int, ReplicaAssignment\]\] // Topic 分区副本列表

private val partitionLeadershipInfo \= mutable.Map.empty\[TopicPartition, LeaderIsrAndControllerEpoch\] // Topic 分区 Leader 信息

val partitionsBeingReassigned \= mutable.Set.empty\[TopicPartition\] // 正在执行重分配的分区列表

val partitionStates \= mutable.Map.empty\[TopicPartition, PartitionState\] // 分区状态信息汇总

val replicaStates \= mutable.Map.empty\[PartitionAndReplica, ReplicaState\] // 副本状态信息汇总

val replicasOnOfflineDirs \= mutable.Map.empty\[Int, Set\[TopicPartition\]\] // 不可用磁盘路径上的副本列表

val topicsToBeDeleted \= mutable.Set.empty\[String\] // 待删除 Topic 列表

val topicsWithDeletionStarted \= mutable.Set.empty\[String\] // 正在删除中的 Topic 列表

val topicsIneligibleForDeletion \= mutable.Set.empty\[String\] // 暂时无法执行删除的 Topic 列表

....

}

这里用一张图来总结下，一一对应上面的定义：

![](https://article-images.zsxq.com/FqMsjrA3qNUc2Xvv-M4ABgkoH70-)

当你掌握了这些元数据之后再去理解 「**MetadataCache**」元数据缓存时，就容易的多了。接下来，我们挑一些重要的元数据进行深度剖析，希望本文可以帮助你更好的理解 Kafka 集群元数据的设计思想。

##   
**2.1 ControllerStats**

其实例化 ControllerStats，该类在 [KafkaContoller.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/KafkaController.scala) 类中，源码定义如下：

// 实例化 ControllerStats 类

val stats \= new ControllerStats

// 该类在 KafkaContoller.scala 类中

private\[controller\] class ControllerStats extends KafkaMetricsGroup {

// 1、统计每秒发生的 Unclean Leader 选举次数

val uncleanLeaderElectionRate \= newMeter("UncleanLeaderElectionsPerSec", "elections", TimeUnit.SECONDS)

// 2、Controller 事件通用的统计速率指标的方法

val rateAndTimeMetrics: Map\[ControllerState, KafkaTimer\] = ControllerState.values.flatMap { state => state.rateAndTimeMetricName.map { metricName =>

state -> new KafkaTimer(newTimer(metricName, TimeUnit.MILLISECONDS, TimeUnit.SECONDS))

}

}.toMap

}

该元数据字段表示 Controller 的一些统计信息。目前，源码中定义了两大类统计指标：UncleanLeaderElectionsPerSec 和 所有 Controller 事件状态的执行速率与时间。

1.  UncleanLeaderElectionsPerSec：**表示计算 Controller 每秒执行的 Unclean Leader 选举数量**，通常情况下，执行 Unclean Leader 选举可能造成数据丢失，**不建议开启它。**一旦开启，你就需要时刻关注这个监控指标的值，确保 Unclean Leader 选举的速率维持在一个很低的水平，否则会出现很多数据丢失的情况。
2.  rateAndTimeMetrics: **表示统计所有 Controller 状态的速率和时间信息，单位毫秒**。ControllerState 的这个指标通过在每个事件名后拼接字符串 RateAndTimeMs 的方式，为每类 Controller 事件都创建了对应的速率监控指标。

这里有一些 Controller 事件是需要你额外关注的。比如：IsrChangeNotification 事件是标志 ISR 列表变更的事件，如果这个事件经常出现，说明副本的 ISR 列表经常发生变化，而这通常被认为是非正常情况，所以你需要时刻关注下这个事件的速率监控指标。

##   
**2.2 OfflinePartitionCount**

该元数据字段用来**统计集群中所有离线/不可用状态的主题分区数量**，这里的离线/不可用是指：Leader = -1 的情况，此时要解决的话，可以通过删除 Zookeeper 的 /controller 节点来让集群重新选举 Controller。一旦 Controller 被选举出来，它就会向所有 Broker 更新集群元数据和分区状态。

updatePartitionStateMetrics 方法会**根据给定主题分区的当前状态和目标状态判断该分区是否处于离线状态**。如果是，则累加 offlinePartitionCount 值，否则递减值。源码如下：

// 更新 offlinePartitionCount 元数据

private def updatePartitionStateMetrics(

partition: TopicPartition, // 分区信息

currentState: PartitionState, // 分区当前状态

targetState: PartitionState): Unit = { // 分区目标状态

// 1、如果该主题当前并未处于删除中状态

if (!isTopicDeletionInProgress(partition.topic)) {

// 2、判断是否要将该主题分区状态转换到离线状态，如果当前状态非 OfflinePartition 且目标状态是离线状态

if (currentState != OfflinePartition && targetState == OfflinePartition) {

// 累加

offlinePartitionCount = offlinePartitionCount + 1

// 3、判断是否要将该主题分区状态转换到非离线状态，如果当前状态已经是离线状态且目标状态非离线状态

} else if (currentState == OfflinePartition && targetState != OfflinePartition) {

// 递减

offlinePartitionCount = offlinePartitionCount - 1

}

}

}

步骤如下：

1.  首先判断该主题**当前并未处理删除中状态**，如果是删除中则不需要做任何操作，直接返回。
2.  否则会先判断是否要将该主题分区状态转换到离线状态，如果当前状态非 OfflinePartition 且目标状态是离线状态，则累加 offlinePartitionCount 值。
3.  判断是否要将该主题分区状态转换到非离线状态，如果当前状态已经是离线状态且目标状态非离线状态，则递减 offlinePartitionCount 值。

## **2.3 shuttingDownBrokerIds**

该元数据字段保存了**所有正在关闭中的 Broker ID 列表**，当「**Controller**」管理集群 Broker 时，会通过该元数据来判断 Broker 当前状态是否「**已关闭**」。

对于已关闭状态的 Broker 是不适合执行某些操作的，比如：「**分区重分配**」、「**主题删除**」等。

Kafka 针对这些「**已关闭**」的 Broker 进行清理工作，源码如下：

/\*

\* KafkaController 方法，用于处理 Kafka 集群中 broker 已关闭事件的回调函数

\*/

private def onBrokerFailure(deadBrokers: Seq\[Int\]): Unit = {

// 记录日志，打印失效的 broker 的 ID 列表

info(s"Broker failure callback for ${deadBrokers.mkString(",")}")

// 1、更新 Controller 元数据信息，将已终止运行的 Broker 从元数据的 replicasOnOfflineDirs 中移除

deadBrokers.foreach(controllerContext.replicasOnOfflineDirs.remove)

// 2、从正在关闭的 broker 列表中删除已经失效的 broker

val deadBrokersThatWereShuttingDown \=

deadBrokers.filter(id => controllerContext.shuttingDownBrokerIds.remove(id))

if (deadBrokersThatWereShuttingDown.nonEmpty)

info(s"Removed ${deadBrokersThatWereShuttingDown.mkString(",")} from list of shutting down brokers.")

// 3、执行副本清扫工作，获取所有在失效 broker 上的 replica 集合，然后调用 onReplicasBecomeOffline 方法进行下线操作

val allReplicasOnDeadBrokers \= controllerContext.replicasOnBrokers(deadBrokers.toSet)

onReplicasBecomeOffline(allReplicasOnDeadBrokers)

// 4、取消指定失效 broker 上注册的 ZooKeeper 的 broker-modifications 监听器

unregisterBrokerModificationsHandler(deadBrokers)

}

该方法主要**对已终止运行的 Broker 列表进行清理工作**，接收一组已终止运行的 Broker ID 列表，步骤如下：

1.  首先更新 Controller 元数据信息，将给定 Broker 从元数据的 replicasOnOfflineDirs 和 shuttingDownBrokerIds 中移除掉。
2.  然后调用 onReplicasBecomeOffline 方法为这组 Broker 执行副本清扫工作。
3.  最后取消指定失效 broker 的 broker-modifications 监听器，防止后续的事件通知操作尝试针对这些 broker 发送消息导致错误。

这里重点步骤是 「**第二步**」、「**第三步**」。 这里我们来看下「**第二步**」的实现，至于「**第二步**」我们后续在副本状态机和分区状态机的时候进行深度剖析，这里就记住它主要是将给定的「**副本**」标记为 Offline 状态。

下面看下 「**第三步**」的清理工作。

## **unregisterBrokerModificationsHandler**

// KafkaController 方法，用于取消指定失效 broker 的 broker-modifications 监听器

private def unregisterBrokerModificationsHandler(brokerIds: Iterable\[Int\]): Unit = {

// 打印日志，记录要注销的 broker ID 列表

debug(s"Unregister BrokerModifications handler for $brokerIds")

// 遍历所有要注销的 broker，从 brokerModificationsHandlers 中移除对应的监听器，并取消注册对应的 ZooKeeper 节点变更通知

brokerIds.foreach { brokerId =>

brokerModificationsHandlers.remove(brokerId).foreach(handler => zkClient.unregisterZNodeChangeHandler(handler.path))

}

}

该方法主要**对已终止运行的 Broker 列表进行清理工作**，步骤如下：

1.  首先会打印日志，记录要注销 broker-modifications 监听器的 broker ID 列表。
2.  接着，会遍历所有要注销的 broker，在 brokerModificationsHandlers 集合中移除对应的监听器。这些监听器在创建时会被添加到 brokerModificationsHandlers 集合中，以便后续对 broker 在 ZK 上的节点状态变化进行监听。
3.  最后，对于每个已经注销的 broker，会取出对应的监听器，并取消注册对应的 ZooKeeper 节点变更通知。这样，当 broker 在 ZK 上的节点状态发生变化时，就不会触发对应的监听器了，从而避免因为失效 broker 导致的一些莫名其妙的错误。

## **2.4 liveBrokers**

该元数据字段保存了**当前所有运行中的 Broker 对象**。每个 Broker 对象就是一个 <Id，EndPoint，机架信息> 的三元组。

在 ControllerContext 类中定义了很多方法来管理该字段，比如如「**addLiveBrokersAndEpochs**」、「**removeLiveBrokers**」 、「**updateBrokerMetadata**」 等。这里我们以「**updateBrokerMetadata**」方法进行说明，源码如下：

def updateBrokerMetadata(oldMetadata: Broker, newMetadata: Broker): Unit = {

liveBrokers -= oldMetadata

liveBrokers += newMetadata

}

代码很简单吧，**每当新增或者移除已有 Broker 时，**ZooKeeper 就会更新其保存的 Broker 数据，从而触发 Controller 修改元数据，此时就会调用该方法来增减 Broker 列表中的对象。

## **2.5 liveBrokerEpochs**

该元数据字段保存了**所有运行中 Broker 的 Epoch 信息**。它是属于 Broker 自己的 Epoch 信息，在 Kafka 中使用 Epoch 数据来防止旧的 Broker 被选举成为 Controller。

在源码中大多使用该字段来获取所有运行中 Broker 的 ID 序号，如下：

def liveBrokerIds: Set\[Int\] = liveBrokerEpochs.keySet.diff(shuttingDownBrokerIds)

// 清空存活 broker 信息

private def clearLiveBrokers(): Unit = {

// 清空 liveBrokers 集合

liveBrokers.clear()

// 清空 liveBrokerEpochs 集合

liveBrokerEpochs.clear()

}

// 添加存活 broker 信息

def addLiveBrokers(brokerAndEpochs: Map\[Broker, Long\]): Unit = {

// 将每个 Broker 以及其对应的 brokerEpoch 添加到 liveBrokers 和 liveBrokerEpochs 集合中

liveBrokers ++= brokerAndEpochs.keySet

liveBrokerEpochs ++= brokerAndEpochs.map { case (broker, brokerEpoch) => (broker.id, brokerEpoch) }

}

// 移除指定 broker 的存活信息

def removeLiveBrokers(brokerIds: Set\[Int\]): Unit = {

// 从 liveBrokers 中移除被删除的 broker

liveBrokers --= liveBrokers.filter(broker => brokerIds.contains(broker.id))

// 从 liveBrokerEpochs 中移除指定的 broker ID

liveBrokerEpochs --= brokerIds

}

liveBrokerEpochs.keySet 方法返回 Broker 序号列表，然后从中移除关闭中的 Broker 序号，剩下的就是运行中的 Broker 序号列表了。

## **2.6 epoch & epochZkVersion**

这里的 **epoch** 元数据字段指的是 **Zookeeper 中的 /controller\_epoch 节点的值，它就是 Controller 在整个 Kafka 集群中的版本号**。

**epochZkVersion** 元数据字段指的是 **Zookeeper 中的 /controller\_epoch 节点的 dataVersion 值**。

在 Kafka 中，会使用 epochZkVersion 字段来判断和防止旧的 Broker 被选举成为 Controller，新的 Controller 的 epochZkVersion 值要比老的大。

此处的 **epoch** 指的是 「**Controller**」侧的数据，而上面的 **liveBrokerEpochs** 指的是所属 Broker 自己的 Epoch 值，源码如下：

var epoch: Int = KafkaController.InitialControllerEpoch

var epochZkVersion: Int = KafkaController.InitialControllerEpochZkVersion

## **2.7 allTopics**

该元数据字段保存了**集群中所有的主题名称**，每当有增减主题时，「**Controller**」都会更新此元数据字段。

这里以 「**KafkaController**」的方法 processTopicChange 为例，它是处理主题变更的。

private def processTopicChange(): Unit = {

// 如果非 Contorller，直接返回

if (!isActive) return

// 从 zk 的 /brokers/topics 节点中获取全量 topic 列表，

val topics = zkClient.getAllTopicsInCluster(true)

// 找出新增的 topic

val newTopics = topics -- controllerContext.allTopics

// 找出删除的 topic

val deletedTopics = controllerContext.allTopics.diff(topics)

// 更新 Controller 元数据

controllerContext.setAllTopics(topics)

// 注册 partition 相关的 hook

registerPartitionModificationsHandlers(newTopics.toSeq)

// 获取新 topic 的 partition replica 分配

val addedPartitionReplicaAssignment \= zkClient.getFullReplicaAssignmentForTopics(newTopics)

// 删除不再存在的 topic

deletedTopics.foreach(controllerContext.removeTopic)

// 更新新 topic 的 partition replica 分配

addedPartitionReplicaAssignment.foreach {

case (topicAndPartition, newReplicaAssignment) => controllerContext.updatePartitionFullReplicaAssignment(topicAndPartition, newReplicaAssignment)

}

info(s"New topics: \[$newTopics\], deleted topics: \[$deletedTopics\], new partition replica assignment " +

s"\[$addedPartitionReplicaAssignment\]")

// 如果有新的 partition 创建，则执行 onNewPartitionCreation 方法

if (addedPartitionReplicaAssignment.nonEmpty)

onNewPartitionCreation(addedPartitionReplicaAssignment.keySet)

}

  
该方法主要用来**当 Kafka 集群中新增或者删除 Topic 时，控制器就会执行该方法来处理 Topic 变更**, 步骤如下：

1.  zkClient.getAllTopicsInCluster(true)：从 zk 中获取整个集群的 topic 列表，并返回一个 Set。
2.  topics -- controllerContext.allTopics：将步骤1中获取的 topic 列表 topics 与控制器中已有的 topic 列表 allTopics 相比较，取出新增的 topic，需要注意的是可能会有重复的新增，但是因为 Set 本身就会去重，所以结果中是不会有重复的新增 topic。
3.  [controllerContext.allTopics.diff](http://controllercontext.alltopics.diff/)(topics)：同上，将 allTopics 中已经删除的 topic 取出，同样可能会有重复的删除，但是结果中不会有重复的删除 topic。
4.  controllerContext.setAllTopics(topics)：将全量 topic 列表存储到 controllerContext 对象中，方便后续使用。
5.  registerPartitionModificationsHandlers(newTopics.toSeq)：为新增的 topic 注册 partition 相关的 hook。
6.  zkClient.getFullReplicaAssignmentForTopics(newTopics)：获取新增的 topic 的 partition 以及 partition replica 分配情况，返回一个 Map 对象。
7.  deletedTopics.foreach(controllerContext.removeTopic)：删除不再存在的 topic。
8.  addedPartitionReplicaAssignment.foreach(...)：更新新增 topic 的 partition 以及 partition replica 分配信息。对于分配信息变化的 partition，会进行 replica 的迁移和数据的重新分布。
9.  如果有新的 partition 创建，则执行 onNewPartitionCreation 方法。

## **2.8 partitionAssignments**

该元数据字段保存**所有主题分区的副本分配情况**，**个人认为这是 Controller 最重要的元数据了**。

以它定义的方法很多，这里以 allPartitions 为例， Kafka 获取某个 Broker 上所有的分区信息。

def allPartitions: Set\[TopicPartition\] = {

// 遍历 partitionAssignments，返回处理后的集合

partitionAssignments.flatMap {

// 对于每一对 (topic, topicReplicaAssignment)，都遍历它的值 topicReplicaAssignment，返回 TopicPartition 实例

case (topic, topicReplicaAssignment) => topicReplicaAssignment.map {

// 对于每个 (partition, \_)，都构造一个新的 TopicPartition 实例，表示主题和分区

case (partition, \_) => new TopicPartition(topic, partition)

}

}.toSet // 最后将处理后的 TopicPartition 实例集合转换为 Set 集合并返回。

}

##   
**2.9 partitionLeadershipInfo**

该元数据字段保存**所有主题分区的 Leader 信息数据**，在 Kafka 集群中，每个分区都会有多个副本(replica)。每个副本可能对应着一个 broker 节点，也可能是多个 broker 节点。但**分区的 Leader 副本在任意时刻都只有一个**， **并负责处理所有的读写请求**。

分区的所有副本信息和其领导者信息都被记录在 Kafka 的元数据(metadata)中。在元数据信息中， 该元数据字段就是记录了每个分区的 leader 副本的信息，包括其所在 broker 节点的 ID、主机名和端口号等。

def partitionLeadershipInfo(partition: TopicPartition): Option\[LeaderIsrAndControllerEpoch\] = {

partitionLeadershipInfo.get(partition)

}

剩余的几个元数据字段就不一一分析了，你可以结合上面的剖析过程来对它们进行学习和掌握。

这里的一个重要的学习方法是：在学习每个元数据字段时，**除了定义之外，最好去搜一下相关方法是如何实现的，这样可以帮助你更好的理解它的作用和使用场景**。

## **03 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头通过剖析元数据引出了「**ControllerContext**」类。

2、带你总结了 「**集群元数据**」一览图，并深度剖析了「**重要元数据**」字段的定义以及使用场景。

下篇我们来深度剖析「**控制器 Controller 选举机制实现原理**」，大家期待，我们下期见。