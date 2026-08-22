大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端延迟机制、时间轮实现原理**」，了解了「**时间轮算法**」的实现原理以及 Kafka 是如何利用「**时间轮算法**」解决「**延迟请求**」的，从今天开始，我们接着来深度剖析 Kafka「**Broker**」的底层源码实现，我们接着来深度聊聊「**Kafka 服务端 Broker 异步更新元数据缓存实现原理**」，看看 Kafka Broker 之间的元数据是如何被更新的。

![](https://article-images.zsxq.com/FokF6wMQMYcMqjtM4NGsm1sJuqGa)

## **01 总体概述**

还记得我们在 [【服务端 Broker 源码分析系列第十八篇】图解 Kafka 源码之控制器 Controller 元数据管理](https://articles.zsxq.com/id_z6wkq8gxjdgk.html) 这篇中已经剖析过 「**Controller**」 端的元数据缓存了，你是否会有这样的疑惑：**我们今天要剖析的又是什么元数据缓存呢？这个异步更新机制是如何实现的呢？**

带着这两个问题，开启我们今天的话题。

## **02 元数据缓存初探**

对于上面第一个问题，其实指的就是 「**Broker**」上的元数据缓存，这些元数据是由「**Controller**」通过发送UpdateMetadataRequest 请求给集群中其他 Broker 的。也就是说在 「**Controller**」端实现了一个「**异步更新机制**」，能够保证将最新的集群信息发送给所有 Broker。

不知道你是否遇到过这样的一种场景：

某时某刻我们在 Kafka 集群中创建了新的 Topic，但是在调用消费者进行消费的时候却报错了，提示「**找不到主题信息**」，但是过会它又恢复正常了。

你有没有深入思考过这里报错的缘故？ 其实很简单，主要因为「**元数据是异步更新的**」，所以在某时刻，某些 Broker 保存的元数据是过期的，还未进行更新元数据，所以就无法识别到最新的 Topic 了。

## **03 MetadataCache 元数据缓存类**

今天的主角是 「**MetadataCache**」。它是每台 Broker 上都会保存的元数据，Kafka 通过「**异步更新机制**」来保证所有Broker 上的元数据缓存实现最终一致性。

「**MetadataCache**」类源码在 Kafka 源码包的 utils 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/MetadataCache.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/MetadataCache.scala)

「**MetadataCache**」的初始化是在 Kafka Broker 启动时完成的，具体是在 KafkaServer#startup 方法中进行实例化，源码如下：

override def startup(): Unit = {

try {

info("starting")

....

metadataCache = MetadataCache.zkMetadataCache(config.brokerId)

....

info("started")

}catch {

....

}

}

一旦「**MetadataCache**」实例被成功创建后，就会被 Kafka 的几个组件使用，如下：

1.  **AlterIsrManager**：它是 Kafka 定义的专门用来变更 ISR 信息的管理器，当 ISR 信息构建后会提交到集合中异步发送给 Controller。
2.  **ReplicaManager**：它是 Kafka 定义的专门用来管理副本的管理器，它需要获取主题分区和 Broker 数据，同时还会更新 MetadataCache。
3.  **AdminManager**：它是 Kafka 定义的专门用来管理主题的管理器，里面定义了很多与 Topic 相关的方法。它会用到 MetadataCache 中的 Topic 信息和 Broker 数据，以获取 Topic 和 Broker 列表。
4.  **TransactionCoordinator**：它是 Kafka 定义的专门用来管理 Kafka 事务的协调者组件，它需要用到 MetadataCache 中的主题分区的 Leader 副本所在的 Broker 数据，向指定 Broker 发送事务标记。
5.  **AutoTopicCreationManager**：它是 Kafka 定义的专门用来自动创建 Topic 的管理器，它需要用到 MetadataCache 中的主题分区数据。
6.  **KafkaApis**：这是源码入口类，它是执行 Kafka 各类请求逻辑的地方。该类大量使用 MetadataCache 中的主题分区和Broker 数据，执行主题相关的判断与比较，以及获取 Broker 信息。

## **3.1 MetadataCache 类定义**

当弄清楚了 「**MetadataCache**」类被创建的时机以及调用方，我们掌握了它的典型使用场景，它保存了集群中关于 Topic 和 Broker 的所有重要元数据。

接下来，我们来看下这些元数据到底都是什么。

class ZkMetadataCache(brokerId: Int) extends MetadataCache with Logging {

private val partitionMetadataLock \= new ReentrantReadWriteLock()

@volatile private var metadataSnapshot: MetadataSnapshot = MetadataSnapshot(partitionStates = mutable.AnyRefMap.empty,topicIds = Map.empty, controllerId = None, aliveBrokers = mutable.LongMap.empty, aliveNodes = mutable.LongMap.empty)

this.logIdent = s"\[MetadataCache brokerId=$brokerId\] "

private val stateChangeLogger \= new StateChangeLogger(brokerId, inControllerContext = false, None)

....

}

重要属性如下：

1.  **brokerId**：构造函数接收的 Broker 的 ID 序号。
2.  **partitionMetadataLock**：它是保护要写入数据的锁对象。
3.  **metadataSnapshot**：它内部保存了实际的元数据信息。这是 MetadataCache 类中最重要的字段。
4.  **logIndent、stateChangeLogger**：它们仅仅用来日志输出。

## **3.2 MetadataSnapshot 类定义**

接着我们来看下 **metadataSnapshot 对应的类 MetadataSnapshot** ，该类是MetadataCache中定义的一个嵌套类，源码如下：

case class MetadataSnapshot(partitionStates: mutable.AnyRefMap\[String, mutable.LongMap\[UpdateMetadataPartitionState\]\],

topicIds: Map\[String, Uuid\],

controllerId: Option\[Int\],

aliveBrokers: mutable.LongMap\[Broker\],

aliveNodes: mutable.LongMap\[collection.Map\[ListenerName, Node\]\])

它是一个元数据快照的 case class，用于保存元数据的快照信息，内部存储了一次元数据快照中的所有信息，方便进行后续操作和信息展示。

重要属性如下：

1.  **partitionStates**：这是一个 Map 类型。其中 Key 是主题名称，Value 是一个 Map 类型，Value 的 Key 是 分区号，Value 的 Value 是一个 [UpdateMetadataPartitionState](http://updatemetadatapartitionstate/) 类型的字段。[UpdateMetadataPartitionState](http://updatemetadatapartitionstate/) 类型是 [UpdateMetadataRequest](http://updatemetadatarequest/) 请求内部所需的数据结构。
2.  **topicIds**：类型为 Map\[String, Uuid\]，表示主题名称和对应的 UUID。
3.  **controllerId**：类型为 Option\[Int\]，它表示Controller所在Broker的ID。如果该值为 None，则表示没有选举出 Controller 节点。
4.  **aliveBrokers**：当前集群中所有活跃的 Broker 对象列表。
5.  **aliveNodes**：当前集群中所有活跃的节点列表。这也是一个Map的Map类型。其Key是Broker ID序号，Value是Map类型，其Key是ListenerName，即Broker监听器类型，而Value是Broker节点对象。

## **3.3 MetadataCache 类重要方法**

该类最重要的方法就是**操作 metadataSnapshot 字段的方法**。所谓元数据缓存是指：[MetadataSnapshot](http://metadatasnapshot/) 类中承载的数据。

根据对源码的梳理，这里我把该类的方法大致分为以下三类：

1.  判断相关方法。
2.  获取相关方法。
3.  更新相关方法。

## **3.4 判断相关方法**

所谓判断相关方法，就是**判断给定主题或者主题分区是否包含在元数据缓存中的相关方法**。该类提供了两个相关方法，方法名都是 **contains**，只是输入参数不同，源码如下：

// 判断给定主题是否包含在元数据缓存中

def contains(topic: String): Boolean = {

metadataSnapshot.partitionStates.contains(topic)

}

该方法用来**判断给定主题是否包含在元数据缓存中**，只需要判断 MetadataSnapshot 数据类型中 partitionStates 字段的所有 Key是否包含指定主题就行了。

// 判断给定主题分区是否包含在元数据缓存中

def contains(tp: TopicPartition): Boolean = getPartitionInfo(tp.topic, tp.partition).isDefined

// 获取给定主题分区的详细数据信息。如果没有找到对应记录，返回None

def getPartitionInfo(topic: String, partitionId: Int): Option\[UpdateMetadataPartitionState\] = {

metadataSnapshot.partitionStates.get(topic).flatMap(\_.get(partitionId))

}

该方法相对复杂一点。它是用来**判断给定主题分区是否包含在元数据缓存中**，首先要从 [metadataSnapshot](http://metadatasnapshot/) 中获取指定主题分区的分区数据信息，然后根据分区数据是否存在，来判断给定主题分区是否包含在元数据缓存中。

## **3.5 获取相关方法**

在 MetadataCache 类中 getXXX 方法很多，这些都属于获取相关的方法，接下来我们挨个来剖析下。

## **3.5.1 getAllTopics()**

def getAllTopics(): Set\[String\] = {

getAllTopics(metadataSnapshot)

}

// 私有方法

private def getAllTopics(snapshot: MetadataSnapshot): Set\[String\] = {

snapshot.partitionStates.keySet

}

该方法用来**获取当前集群元数据缓存中的所有主题的**。可以看到私有 getAllTopics 仅仅是返回 MetadataSnapshot 数据类型中 partitionStates 字段的所有 Key 字段。上面已经剖析过，[partitionStates](http://partitionstates/) 是一个Map 类型，Key 就是主题，很简单吧。

##   
**3.5.2 getAllPartitions()**

private def getAllPartitions(snapshot: MetadataSnapshot): Map\[TopicPartition, UpdateMetadataPartitionState\] = {

snapshot.partitionStates.flatMap { case (topic, partitionStates) =>

partitionStates.map { case (partition, state ) => (new TopicPartition(topic, partition.toInt), state) }

}.toMap

}

该方法用来**获取当前集群元数据缓存中的所有主题分区的**。它会遍历 partitionStates，取出分区后，然后构建TopicPartition 实例，并将其加入到合中返回。

##   
**3.5.3 getNonExistingTopics()**

def getNonExistingTopics(topics: Set\[String\]): Set\[String\] = {

topics.diff(metadataSnapshot.partitionStates.keySet)

}

该方法用来**获取当前集群元数据缓存中的不存在的主题的**，看到它直接使用 [topics.diff](http://topics.diff/) 来找到不存在主题信息。

## **3.5.4 getAliveBroker()**

def getAliveBroker(brokerId: Int): Option\[MetadataBroker\] = {

metadataSnapshot.aliveBrokers.get(brokerId).map(MetadataBroker.apply)

}

该方法用来**获取当前活跃 Broker 列表中指定 Broker 的元数据**，其中 [MetadataBroker.apply](http://metadatabroker.apply/) 是 case class MetadataBroker 的伴生对象中的 apply 方法，用于将 Broker 的元数据信息转换为 MetadataBroker 类型的对象，方便操作和展示。如果找不到该 Broker 节点，则返回 None。

##   
**3.5.5 getAliveBrokers()**

def getAliveBrokers: Seq\[MetadataBroker\] = {

metadataSnapshot.aliveBrokers.values.map(MetadataBroker.apply).toBuffer

}

该方法用来**获取当前活跃 Broker 列表的元数据集合**。

##   
**3.5.6 getPartitionLeaderEndpoint()**

def getPartitionLeaderEndpoint(topic: String, partitionId: Int, listenerName: ListenerName): Option\[Node\] = {

// 使用局部变量获取当前元数据缓存

val snapshot \= metadataSnapshot

// 获取给定主题分区的数据

snapshot.partitionStates.get(topic).flatMap(\_.get(partitionId)) map { partitionInfo =>

// 获取该分区的 Leader 节点ID。

val leaderId \= partitionInfo.leader

// 获取 Leader 节点所在的Broker Id

snapshot.aliveNodes.get(leaderId) match {

// 如果获取成功，则使用 nodeMap.getOrElse(listenerName, Node.noNode) 返回该节点的信息和监听器名称；否则返回 Node.noNode。

case Some(nodeMap) =>

nodeMap.getOrElse(listenerName, Node.noNode)

case None \=\>

Node.noNode

}

}

}

该方法用来**获取指定主题和分区的 Leader Broker 节点信息**，步骤如下：

1.  首先，使用局部变量获取当前的元数据缓存。这样做的好处在于，不需要使用锁技术，但是可能读取的数据已经过期了不过。好在 Kafka 能够自行处理过期元数据的问题。当客户端因为拿到过期元数据而向 Broker 发出错误的指令时，Broker 会显式地通知客户端错误原因。客户端接收到错误后，会尝试再次拉取最新的元数据。这个过程就能够保证，客户端最终可以取得最新的元数据信息。
2.  获取到主题分区数据之后，会接着获取 Leader 节点 ID，再获取 Leader 节点所在的 Broker ID，再根据这个 Broker ID 去获取对应的 Broker 节点信息和监听器名称信息并返回。
3.  最后，如果获取失败返回 Node.noNode。

## **3.5.7 getPartitionReplicaEndpoint()**

def getPartitionReplicaEndpoints(tp: TopicPartition, listenerName: ListenerName): Map\[Int, Node\] = {

// 使用局部变量获取当前元数据缓存

val snapshot \= metadataSnapshot

// 获取给定主题分区的数据

snapshot.partitionStates.get(tp.topic).flatMap(\_.get(tp.partition)).map { partitionInfo =>

// 获取副本Id列表

val replicaIds \= partitionInfo.replicas

replicaIds.asScala

.map(replicaId => replicaId.intValue() -> {

// 获取副本所在的 Broker Id

snapshot.aliveBrokers.get(replicaId.longValue()) match {

case Some(broker) =>

// 根据 Broker Id 去获取对应的 Broker节点对象

broker.getNode(listenerName).getOrElse(Node.noNode())

case None \=\>

// 如果找不到节点

Node.noNode()

}}).toMap

.filter(pair => pair match {

case (\_, node) => !node.isEmpty

})

}.getOrElse(Map.empty\[Int, Node\])

}

该方法用来**获取指定主题和分区的副本 Broker 节点信息**，并按照 Broker ID 进行分组。步骤如下：

1.  首先，使用局部变量获取当前的元数据缓存。这样做的好处在于，不需要使用锁技术，但是可能读取的数据已经过期了不过。好在 Kafka 能够自行处理过期元数据的问题。当客户端因为拿到过期元数据而向 Broker 发出错误的指令时，Broker 会显式地通知客户端错误原因。客户端接收到错误后，会尝试再次拉取最新的元数据。这个过程就能够保证，客户端最终可以取得最新的元数据信息。
2.  获取到主题分区数据之后，会接着获取副本ID列表，再遍历该列表，依次获取每个副本所在的 Broker ID，再根据这个 Broker ID 去获取对应的Broker节点对象。
3.  最后，将这些节点对象封装到返回结果中并返回。

## **3.5.8 getControllerId()**

def getControllerId: Option\[Int\] = metadataSnapshot.controllerId

该方法用来**获取当前集群的控制器ID**。

## **3.5.9 getClusterMetadata()**

def getClusterMetadata(clusterId: String, listenerName: ListenerName): Cluster = {

// 使用局部变量获取当前元数据缓存

val snapshot \= metadataSnapshot

// 获取所有可用的节点信息，并过滤出对应监听器名称的节点信息。

val nodes \= snapshot.aliveNodes.flatMap { case (id, nodesByListener) =>

nodesByListener.get(listenerName).map { node =>

id -> node

}

}

// 根据 ID 在节点映射表中查找对应 Node 信息

def node(id: Integer): Node = {

nodes.getOrElse(id.toLong, new Node(id, "", -1))

}

// 获取所有分区的状态信息，过滤掉 Leader 为删除状态的分区，并使用 PartitionInfo 创建分区信息对象。

val partitions \= getAllPartitions(snapshot)

.filter { case (\_, state) => state.leader != LeaderAndIsr.LeaderDuringDelete }

.map { case (tp, state) =>

new PartitionInfo(tp.topic, tp.partition, node(state.leader),

state.replicas.asScala.map(node).toArray,

state.isr.asScala.map(node).toArray,

state.offlineReplicas.asScala.map(node).toArray)

}

// 定义未授权的主题集合和内部主题集合。

val unauthorizedTopics \= Collections.emptySet\[String\]

// 获取内部主题

val internalTopics \= getAllTopics(snapshot).filter(Topic.isInternal).asJava

// 通过 new Cluster 创建集群对象，其中包括节点信息集合、分区信息集合、未授权主题集合、内部主题集合和 Controller 节点的信息

new Cluster(clusterId, nodes.values.toBuffer.asJava,

partitions.toBuffer.asJava,

unauthorizedTopics, internalTopics,

snapshot.controllerId.map(id => node(id)).orNull)

}

该方法用来**获取当前集群的元数据信息**。

## **3.6 更新相关方法**

在源码中只有一个方法用来更新的：「**updateMetadata**」，它是 Broker 端元数据缓存的更新方法。只有元数据缓存被更新了，才能被读取。

源码比较长，「**updateMetadata**」方法的大体逻辑是**读取 UpdateMetadataRequest 请求中的分区数据，然后更新本地的元数据缓存**。

这里分为三部分来剖析，先来看第一部分：

def updateMetadata(correlationId: Int, updateMetadataRequest: UpdateMetadataRequest): Seq\[TopicPartition\] = {

inWriteLock(partitionMetadataLock) {

// 保存存活 Broker 对象。Key是 Broker ID，Value 是 Broker 对象

val aliveBrokers \= new mutable.LongMap\[Broker\](metadataSnapshot.aliveBrokers.size)

// 保存存活节点对象。Key 是 Broker ID，Value 是监听器->节点对象

val aliveNodes \= new mutable.LongMap\[collection.Map\[ListenerName, Node\]\](metadataSnapshot.aliveNodes.size)

// 从 UpdateMetadataRequest 请求中获取 Controller 所在的 Broker ID

val controllerIdOpt \= updateMetadataRequest.controllerId match {

// 如果当前没有 Controller，赋值为None，否则获取对应id

case id if id < 0 => None

case id \=\> Some(id)

}

// 遍历 UpdateMetadataRequest 请求中的所有存活 Broker 对象

updateMetadataRequest.liveBrokers.forEach { broker =>

// \`aliveNodes\` is a hot path for metadata requests for large clusters, so we use java.util.HashMap which

// is a bit faster than scala.collection.mutable.HashMap. When we drop support for Scala 2.10, we could

// move to \`AnyRefMap\`, which has comparable performance.

val nodes \= new java.util.HashMap\[ListenerName, Node\]

val endPoints \= new mutable.ArrayBuffer\[EndPoint\]

// 遍历它的所有 EndPoint 类型，即 Broker配置的监听器

broker.endpoints.forEach { ep =>

val listenerName \= new ListenerName(ep.listener)

endPoints += new EndPoint(ep.host, ep.port, listenerName, SecurityProtocol.forId(ep.securityProtocol))

// 将 <监听器，Broker节点对象> 对保存起来

nodes.put(listenerName, new Node(broker.id, ep.host, ep.port))

}

// 将 Broker 加入到存活 Broker 对象集合

aliveBrokers(broker.id) = Broker(broker.id, endPoints, Option(broker.rack))

// 将 Broker 节点加入到存活节点对象集合

aliveNodes(broker.id) = nodes.asScala

}

....

}

简单来说，这一部分主要工作是**给下面的操作进行准备数据，也就是 aliveBrokers、aliveNodes 保存的数据**。步骤如下：

1.  首先，创建 aliveBrokers、aliveNodes 两个字段，分别保存存活 Broker 对象和存活节点对象。aliveBrokers 的Key类型是Broker ID，而Value类型是Broker对象；aliveNodes的Key类型也是Broker ID，Value类型是<监听器，节点对象>对。
2.  然后，从 UpdateMetadataRequest 请求中获取 Controller 所在的 Broker ID，并赋值给 controllerIdOpt 字段。如果集群没有 Controller，则赋值该字段为 None。
3.  接着，遍历 UpdateMetadataRequest 请求中的所有存活 Broker 对象。取出它配置的所有 EndPoint 类型，也就是 Broker 配置的所有监听器。
4.  最后，遍历它配置的监听器，并将 <监听器，Broker节点对象> 对保存起来，再将 Broker 加入到存活 Broker对象集合和存活节点对象集合。

再来看第二部分的代码，源码如下：

// 从存活 Broker节点对象中获取当前 Broker 所有的<监听器,节点>对

aliveNodes.get(brokerId).foreach { listenerMap =>

val listeners \= listenerMap.keySet

// 如果发现当前 Broker 配置的监听器与其他 Broker 有不同之处，记录错误日志

if (!aliveNodes.values.forall(\_.keySet == listeners))

error(s"Listeners are not identical across brokers: $aliveNodes")

}

// 从 UpdateMetadataRequest 中读取所有 Topic 状态信息，过滤掉 topicId 为 0 的（即没有生成UUID的），保存在 Map 中。

val newTopicIds \= updateMetadataRequest.topicStates().asScala

.map(topicState => (topicState.topicName(), topicState.topicId()))

.filter(\_.\_2 != Uuid.ZERO\_UUID).toMap

// 新建一个空的可变 Map 对象，用于存放更新后的主题 UUID 信息。

val topicIds \= mutable.Map.empty\[String, Uuid\]

// 使用 ++= 将元数据快照中的 topicIds 信息添加到新建 Map 对象中。

topicIds ++= metadataSnapshot.topicIds

// 然后使用 ++= 将从请求中读取到的新的主题 UUID 信息添加到 Map 对象中，这样如果某个主题更新了 UUID 信息，那么就使用新的 UUID，否则使用元数据快照中的 UUID 信息。

topicIds ++= newTopicIds

// 构造已删除分区数组，将其作为方法返回结果

val deletedPartitions \= new mutable.ArrayBuffer\[TopicPartition\]

// 判断 UpdateMetadataRequest 请求没有携带任何分区信息

if (!updateMetadataRequest.partitionStates.iterator.hasNext) {

// 构造新的 MetadataSnapshot 对象，使用之前的分区信息和新的 Broker 列表信息

metadataSnapshot = MetadataSnapshot(metadataSnapshot.partitionStates, topicIds.toMap, controllerIdOpt, aliveBrokers, aliveNodes)

} else {

....

}

简单来说，这一部分主要工作是**确保集群 Broker 配置了相同的监听器，同时初始化已删除分区的数组对象，等待后续对他进行操作**，步骤如下：

1.  首先从存活 Broker 节点对象中获取当前Broker所有的<监听器,节点>对。
2.  如果发现配置的监听器与其他Broker有不同之处，则记录一条错误日志。
3.  从 UpdateMetadataRequest 中读取所有 Topic 状态信息，过滤掉 topicId 为 0 的（即没有生成UUID的），保存在 Map 中。
4.  另外新建一个空的可变 Map 对象，用于存放更新后的主题 UUID 信息。
5.  接着构造一个已删除分区数组，将其作为方法返回结果。然后判断 UpdateMetadataRequest 请求是否携带了任何分区信息。
6.  如果没有，则构造一个新的 MetadataSnapshot 对象，使用之前的分区信息和新的 Broker 列表信息。
7.  如果有，进入到该方法的最后一个部分。

再来看第三部分的代码，源码如下：

//since kafka may do partial metadata updates, we start by copying the previous state

val partitionStates \= new mutable.AnyRefMap\[String, mutable.LongMap\[UpdateMetadataPartitionState\]\](metadataSnapshot.partitionStates.size)

// 备份现有元数据缓存中的分区数据

metadataSnapshot.partitionStates.forKeyValue { (topic, oldPartitionStates) =>

val copy \= new mutable.LongMap\[UpdateMetadataPartitionState\](oldPartitionStates.size)

copy ++= oldPartitionStates

partitionStates(topic) = copy

}

val traceEnabled \= stateChangeLogger.isTraceEnabled

val controllerId \= updateMetadataRequest.controllerId

val controllerEpoch \= updateMetadataRequest.controllerEpoch

// 获取 UpdateMetadataRequest 请求中携带的所有分区数据

val newStates \= updateMetadataRequest.partitionStates.asScala

// 遍历分区数据

newStates.foreach { state =>

// 获取主题分区对象

val tp \= new TopicPartition(state.topicName, state.partitionIndex)

// 如果分区处于被删除过程中

if (state.leader == LeaderAndIsr.LeaderDuringDelete) {

// 将分区从元数据缓存中移除

removePartitionInfo(partitionStates, topicIds, tp.topic, tp.partition)

....

// 将分区加入到已删除分区列表

deletedPartitions += tp

} else {

// 将分区加入到元数据缓存

addOrUpdatePartitionInfo(partitionStates, tp.topic, tp.partition, state)

....

}

}

val cachedPartitionsCount \= newStates.size - deletedPartitions.size

// 使用更新过的分区元数据，和第一部分计算的存活Broker列表及节点列表，构建最新的元数据缓存

metadataSnapshot = MetadataSnapshot(partitionStates, topicIds.toMap, controllerIdOpt, aliveBrokers, aliveNodes)

}

// 返回已删除分区列表

deletedPartitions

简单来说，这一部分主要工作是**提取 UpdateMetadataRequest 请求中的数据然后填充元数据缓存**，步骤如下：

1.  首先，备份现有元数据缓存中的分区数据到partitionStates的局部变量中。
2.  接着，获取 UpdateMetadataRequest 请求中携带的所有分区数据，并遍历每个分区数据。如果发现分区处于被删除的过程中，就将分区从元数据缓存中移除，并把分区加入到已删除分区列表中。
3.  否则的话，将分区加入到元数据缓存中。
4.  最后，使用更新过的分区元数据，和第一部分计算的存活 Broker 列表及节点列表，构建最新的元数据缓存，然后返回已删除分区列表。

最后我通过一张图来梳理其执行流程：

![](https://article-images.zsxq.com/lo83IeJgcxu53kpoyQFPZrnPAlXh)

## **04 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头从「**Controller**」端元数据缓存引出了「**Broker** 」端元数据又是什么，及它是如何更新的呢？

2、接着带大家剖析了「**元数据缓存初探**」， 了解了它是「**Controller**」端实现了一个「**异步更新机制**」，能够保证将最新的集群信息发送给所有 Broker。

3、接着带大家深度剖析了「**MetadataCache**」类以及重要方法。该类保存了当前集群上的主题分区详细数据和Broker 数据。每台 Broker 都维护了一个 MetadataCache 实例。「**Controller**」通过给 Broker 发送UpdateMetadataRequest 请求的方式，来异步更新这部分缓存数据。

  
下篇我们来深度剖析「**事务设计以及初始化流程**」，大家期待，我们下期见。