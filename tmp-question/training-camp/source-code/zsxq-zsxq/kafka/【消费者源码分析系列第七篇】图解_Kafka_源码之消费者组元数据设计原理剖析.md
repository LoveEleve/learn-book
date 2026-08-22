大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 消费者组****管理全流程剖析**」，了解了消费者组内部是如何对「**组状态**」、「**组成员**」、「**位移**」、「**分区分配策略**」进行管理的。今天我们开启消费端源码的征程，这是第七篇来深度聊聊「**消费者组元数据设计原理**」，看看 Kafka 是如何对消费者组元数据进行设计以及管理的。

![](https://article-images.zsxq.com/FhndRYD2oWlAd2Wb5YhOBhxLrFVl)

## **01 总体概述**

对 Kafka 来说，元数据无处不在，为了提高系统性能，服务端引入了元数据以及元数据缓存，对于消费者来说，有两个元数据类组合来完成元数据的工作。

在上两篇中，我们已经剖析了 [GroupMetadata](http://groupmetadata/) 的部分消费者组元数据管理，包括「**组状态**」、「**组成员**」、「**位移**」、「**分区分配策略**」，可以点击 [【消费者源码分析系列第六篇】图解 Kafka 源码之消费者组管理全流程剖析](https://articles.zsxq.com/id_7zcwmwa6wvot.html)、[【消费者源码分析系列第五篇】图解 Kafka 源码之消费者组状态机流程](https://articles.zsxq.com/id_tpg7iob24t7w.html) 学习。

还有一个是消费者组成员的元数据，对应的是 [MemberMetadata](http://membermetadata/)，如下：

![](https://article-images.zsxq.com/Fv-kag2VXbsWPX_s8cXwBFArA6Io)

接下来我们就来重点看看这两三个类中究竟都定义了哪些元数据，以及是如何管理的。

本文涉及的源码：

「**GroupMetadata**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/coordinator/group/GroupMetadata.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/coordinator/group/GroupMetadata.scala)

「**MemberMetadata**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/coordinator/group/MemberMetadata.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/coordinator/group/MemberMetadata.scala)

「**GroupMetadataManager**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/coordinator/group/GroupMetadataManager.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/coordinator/group/GroupMetadataManager.scala)

## **02 消费者组成员元数据**

该类比较短小精悍，包括注释总共 154 行代码，内部包含了三个类和对象：

1.  MemberSummary 类：消费组成员概要数据，内部提取了最核心的元数据信息。
2.  Object MemberMetadata 伴生对象：仅仅定义了一个工具方法，供上层调用。
3.  Class MemberMetadata 类：消费者组成员的元数据。

具体如下：

![](https://article-images.zsxq.com/FqzzE-P6mOoQIx97Gqh1NorBbWmw)

接下来，我们从上到下挨个来梳理下。

## **2.1 MemberSummary**

MemberSummary 类就是组成员元数据的一个概要数据类，其本质上是一个 POJO 类，仅仅承载数据，没有定义任何逻辑。

case class MemberSummary(memberId: String,

groupInstanceId: Option\[String\],

clientId: String,

clientHost: String,

metadata: Array\[Byte\],

assignment: Array\[Byte\])

这里重点解释一下这6个字段：

1.  memberId：用来标识消费者组成员的 ID，它是由 Kafka 自动生成的，规则是 [consumer-组ID-<序号>-](http://xn--consumer-id---fh8vj67iby4h/)。目前是硬编码的，无法自己设置。
2.  groupInstanceId：用来标识消费者组静态成员的 ID。「**静态成员机制**」的引入能够规避不必要的消费者组Rebalance 操作。。
3.  clientId：用来标识消费者组成员配置的 [client.id](http://client.id/) 参数。由于 memberId 是硬编码的无法被设置，所以你可以用该字段来区分消费者组下的不同成员。
4.  clientHost：用来标识运行消费者主机名。它记录了该客户端是从哪台机器发出的消费请求。
5.  metadata：用来标识消费者组成员分区分配策略的字节数组，它是由消费者端参数 [partition.assignment.strategy](http://partition.assignment.strategy/) 值来设定，默认分区分配策略为： [RangeAssignor](http://rangeassignor%20/) 策略。
6.  assignment：用来保存分配给该成员的订阅分区。在前面讲过每个消费者组都要选出一个 Leader 消费者组成员负责给所有成员分配消费方案。该字段就是用来使 Kafka 将制定好的分配方案序列化成字节数组，随后分发给各个组成员。

## **2.2 MemberMetadata 伴生类对象**

该 Object 类只定义了一个 [plainProtocolSet](http://plainprotocolset/) 方法供上层调用，如下：

private object MemberMetadata {

// 提取分区分配策略集合

def plainProtocolSet(supportedProtocols: List\[(String, Array\[Byte\])\]) = supportedProtocols.map(\_.\_1).toSet

}

可以看到该方法就是从一组给定的分区分配策略详情中提取出分区分配策略的名称，并将其封装成一个集合对象并返回。

这里我举例说明下，如果此时消费者组下有 3 个成员：

1.  成员1 的 [partition.assignment.strategy](http://partition.assignment.strategy/) 参数为 [RangeAssignor](http://rangeassignor/)。
2.  成员2 的 [partition.assignment.strategy](http://partition.assignment.strategy/) 参数也为 [RangeAssignor](http://rangeassignor/)。
3.  成员3 的 [partition.assignment.strategy](http://partition.assignment.strategy/) 参数为 [RoundRobinAssignor](http://roundrobinassignor/)。

最终该方法的返回值就是集合 \[RangeAssignor，RoundRobinAssignor\]。它通常被用来统计一个消费者组下的成员到底配置了多少种分区分配策略。

## **2.3 MemberMetadata 类**

其定义如下：

@nonthreadsafe

private\[group\] class MemberMetadata(var memberId: String, // 用来标识消费者组成员的 ID

val groupInstanceId: Option\[String\], // 用来标识消费者组静态成员的 ID

val clientId: String, // 用来标识消费者组成员配置的 client.id 参数

val clientHost: String, // 用来标识运行消费者主机名

val rebalanceTimeoutMs: Int, // Rebalane 操作超时时间

val sessionTimeoutMs: Int, // 会话超时时间

val protocolType: String, // 对消费者组来说就是是 "consumer"

var supportedProtocols: List\[(String, Array\[Byte\])\], // 成员配置的多套分区分配策略

var assignment: Array\[Byte\] = Array.empty\[Byte\]) { // 分区分配方案

// 组成员是否正在等待加入消费者组

var awaitingJoinCallback: JoinGroupResult => Unit = \_

// 组成员是否正在等待 GroupCoordinator 发送分配方案

var awaitingSyncCallback: SyncGroupResult => Unit = \_

// 是否是消费者组下的新成员

var isNew: Boolean = false

// 是否是静态成员

def isStaticMember: Boolean = groupInstanceId.isDefined

// 当心跳过期时设置为false,接收到心跳设置为true

var heartbeatSatisfied: Boolean = false

// 是否正在等待加入消费者组

def isAwaitingJoin: Boolean = awaitingJoinCallback != null

// 是否正在等待 GroupCoordinator 发送分配方案

def isAwaitingSync: Boolean = awaitingSyncCallback != null

....

}

在上面剖析了 MemberSummary 中的 6 个字段，这里里重点解释一下剩余字段：

1.  rebalanceTimeoutMs：用来标识 Rebalance 重分配操作的超时时间，即一次 Rebalance 操作必须在这个时间内完成，否则认为超时。由 Consumer 端参数 [max.poll.interval.ms](http://max.poll.interval.ms/) 的值指定。
2.  sessionTimeoutMs：用来标识会话超时时间，当前消费者组成员依靠心跳机制来保活。如果在会话超时时间之内未能成功发送心跳，此时就被判定成下线，从而触发新一轮的 Rebalance 重分配。由 Consumer 端参数 [session.timeout.ms](http://session.timeout.ms/) 的值指定。
3.  protocolType：用来标识协议类型，即使用场景：如果是普通消费者组使用，其值是 consumer；如果是 Kafka Connect 组件中的消费者使用，其值是 connect。
4.  supportedProtocols：用来标识成员配置的多组分区分配策略。由 Consumer 端参数 [partition.assignment.strategy](http://partition.assignment.strategy/) 指定。
5.  assignment：用来保存分配给该成员的分区分配方案。

除了上面这些字段之外，该类还定义了几个额外的字段，用于保存元数据以及判断状态。其中几个是 var 型变量，说明它们的值是可以变更的，所以 [MemberMetadata](http://membermetadata/) 会依靠这些字段来不断地调整组成员的元数据信息和状态。

1.  awaitingJoinCallback：表示组成员是否正在等待加入消费者组。
2.  awaitingSyncCallback：表示组成员是否正在等待 GroupCoordinator 发送分配方案。
3.  isLeaving：表示组成员是否发起“退出组”的操作。
4.  isNew：表示是否是消费者组下的新成员。
5.  isStaticMember：是否是静态成员。
6.  heartbeatSatisfied：当心跳过期时设置为 false，接收到心跳设置为 true。
7.  isAwaitingJoin：是否正在等待加入消费者组。
8.  isAwaitingSync：是否正在等待 GroupCoordinator 发送分配方案。

剖析完重要属性字段后，我们来剖析下该类下的重要方法，都比较简单。

## **2.3.1 metadata()**

// 用来根据提供的协议获取相应的元数据

def metadata(protocol: String): Array\[Byte\] = {

// 从该成员配置的分区分配方案列表中寻找给定策略的详情

supportedProtocols.find(\_.\_1 == protocol) match {

// 如果找到就返回字节数组数据

case Some((\_, metadata)) => metadata

// 否则，就抛出异常

case None \=\>

throw new IllegalArgumentException("Member does not support protocol")

}

}

## **2.3.2 hasSatisfiedHeartbeat()**

// 用来检查成员是否满足预期的心跳

def hasSatisfiedHeartbeat: Boolean = {

// 如果成员状态为 isNew，则首先检查 heartbeatSatisfied 是否为 true。

if (isNew) {

// New members can be expired while awaiting join, so we have to check this first

heartbeatSatisfied

// 如果成员状态为 isAwaitingJoin 或 isAwaitingSync，则自动满足预期的心跳要求，返回 true。

} else if (isAwaitingJoin || isAwaitingSync) {

// Members that are awaiting a rebalance automatically satisfy expected heartbeats

true

} else {

// Otherwise we require the next heartbeat

// 否则需要下一个心跳才能满足预期，返回 heartbeatSatisfied 的值。

heartbeatSatisfied

}

}

## **2.3.3 matches()**

// 用来检查提供的协议元数据是否与当前存储的元数据匹配

def matches(protocols: List\[(String, Array\[Byte\])\]): Boolean = {

// 首先检查提供的协议元数据的 size 是否与当前支持的协议元数据的 size 相等，如果不相等则返回false。

if (protocols.size != this.supportedProtocols.size)

return false

// 逐个比较提供的协议元数据和当前支持的协议元数据

for (i <- protocols.indices) {

val p1 \= protocols(i)

val p2 \= supportedProtocols(i)

// 如果协议名称不匹配，或者协议元数据内容不相等则返回 false。

if (p1.\_1 != p2.\_1 || !util.Arrays.equals(p1.\_2, p2.\_2))

return false

}

// 如果上述比较都通过，则说明提供的协议元数据匹配当前存储的元数据返回 true。

true

}

## **2.3.4 summary()**

def summary(protocol: String): MemberSummary = {

// 根据提供的协议，创建成员的摘要信息并返回。摘要信息包括成员的ID、群组实例ID、客户端ID、客户端主机、协议对应的元数据和分配。

MemberSummary(memberId, groupInstanceId, clientId, clientHost, metadata(protocol), assignment)

}

## **2.3.5 summaryNoMetadata()**

def summaryNoMetadata(): MemberSummary = {

// 创建没有元数据的成员摘要信息并返回。摘要信息包括成员的ID、群组实例ID、客户端ID、客户端主机，但不包含任何元数据和分配。

MemberSummary(memberId, groupInstanceId, clientId, clientHost, Array.empty\[Byte\], Array.empty\[Byte\])

}

## **2.3.6 vote()**

// 用来为潜在的组协议进行投票。根据支持的协议的顺序和候选协议的集合，确定协议首选项，并返回第一个同时在集合中的协议

def vote(candidates: Set\[String\]): String = {

// 在支持的协议中查找第一个同时出现在候选协议集合中的协议

supportedProtocols.find({ case (protocol, \_) => candidates.contains(protocol)}) match {

// 如果找到了匹配的协议，则返回该协议。

case Some((protocol, \_)) => protocol

// 如果找不到匹配的协议，则抛出异常。说明成员不支持任何候选协议。

case None \=\>

throw new IllegalArgumentException("Member does not support any of the candidate protocols")

}

}

该方法主要根据成员所支持和优先选择的协议，选择一个潜在的组协议进行投票。

剖析完组成员元数据之后，接下来我们来剖析下消费者组元数据。

## **03 消费者组元数据**

这里通过一张图来梳理该类的组成部分，大致分为 6 个部分，如下：

![](https://article-images.zsxq.com/FmxKHCGY5jGAJ3QV256R_E6pc3nJ)

![](https://article-images.zsxq.com/FhapgvFhXsFSwH1VI5a_REprQ0IW)

关于 GroupState 类以及 GroupMetadata 类的组管理在前面两篇已经剖析过了，可以点击 [【消费者源码分析系列第五篇】图解 Kafka 源码之消费者组状态机流程](https://articles.zsxq.com/id_tpg7iob24t7w.html) 、[【消费者源码分析系列第六篇】图解 Kafka 源码之消费者组管理全流程剖析](https://articles.zsxq.com/id_7zcwmwa6wvot.html)。

接下来我们剖析下剩余的几个组成部分的源码。

## **3.1 GroupMetadata 类定义**

![](https://article-images.zsxq.com/FpGG5xw0jkqAcfGP4Xu7_pqVr4Yp)

可以看出 [GroupMetadata](http://groupmetadata/) 消费者组元数据保存的数据是最全的，这里剖析下该类最重要的字段：

1.  state：消费者组状态。
2.  currentStateTimestamp：记录最近一次状态变更的时间戳，用于确定位移主题中的过期消息。
3.  generationId：消费组 Generation 号。Generation 等同于消费者组执行过 Rebalance 操作的次数，每次执行 Rebalance时，Generation 数都要加 1。
4.  leaderId：消费者组中 Leader 成员的 Member ID 信息。当消费者组执行 Rebalance 过程时，需要选举一个成员作为Leader，负责为所有成员制定分区分配方案。在 Rebalance 早期阶段，这个 Leader 可能尚未被选举出来则为 None。
5.  members：保存消费者组下所有成员的元数据列表信息。
6.  staticMembers：静态成员 Id 列表。
7.  pendingMembers：待加入的成员 Id 列表。
8.  numMembersAwaitingJoin：正在等待加入 Group 的成员数量。
9.  offsets：保存按照主题分区分组的位移主题消息位移值的 HashMap。其中 Key 是主题分区，Value 是[CommitRecordMetadataAndOffset](http://commitrecordmetadataandoffset/) 类型。当消费者组成员向 Kafka 提交位移时，插入对应的记录到该字段。
10.  subscribedTopics：保存消费者组订阅的主题列表，用于帮助从offsets字段中过滤订阅主题分区的位移值。
11.  supportedProtocols：保存分区分配策略的支持票数。它是一个 HashMap 类型，其中，Key 是分配策略的名称，Value是 支持的票数。
12.  pendingOffsetCommits：保存待提交的偏移量。
13.  receivedConsumerOffsetCommits：标志是否已接收到消费者的提交的偏移量。
14.  pendingSyncMembers： 保存正在等待同步的成员 Id。

关于该类的重要方法在上篇中已经剖析过了，这里就不赘述了。 可以点击 [【消费者源码分析系列第六篇】图解 Kafka 源码之消费者组管理全流程剖析](https://articles.zsxq.com/id_7zcwmwa6wvot.html) 学习。

## **04 消费者组概览类**

这两个类非常简单，我们来看下。

## **4.1 GroupOverview 类**

这是一个消费者组概览信息的类。当我们在命令行执行 [kafka-consumer-groups.sh –list](http://kafka-consumer-groups.xn--sh%20list-u89d/) 的时候，Kafka 就会创建GroupOverview 实例返回给命令行，关于这个命令可以查看 [【核心运维脚本详解第七篇】Kafka 消费者组管理脚本](https://articles.zsxq.com/id_d3vqptzbfvz1.html) 学习。

case class GroupOverview(

groupId: String, // 组ID信息，即group.id参数值

protocolType: String, // 消费者组的协议类型

state: String) // 消费者组的状态

GroupOverview 类封装了最基础的组数据，包括组ID、协议类型和状态信息。

## **4.2 GroupSummary 类**

该类和 GroupOverview 类非常相似，只不过它保存的数据要稍微多一点，源码如下：

case class GroupSummary(

state: String, // 消费者组状态

protocolType: String, // 协议类型

protocol: String, // 消费者组选定的分区分配策略

members: List\[MemberSummary\]) // 成员元数据

这里你需要关注的是 members 字段，它是一个 MemberSummary 类型的列表，里面保存了消费者组所有成员的元数据信息，通过这个字段得知**消费者组元数据和组成员元数据是1对多的关系**。

## **05 消费者组元数据管理器**

有元数据就有元数据管理器，对于元数据管理器来说，我们重点要剖析的就是如何管理消费者组的，以及它对内部位移主题 \_\_consumer\_offsets 的管理，关于后者我们会在后面篇章进行剖析，这里只剖析前者。

先来剖析下该类的定义和关键字段。

## **5.1 GroupMetadataManager 类定义**

class GroupMetadataManager(brokerId: Int, // 所在 Broker Id，即 broker.id 参数值

interBrokerProtocolVersion: ApiVersion, // 保存 Broker 间通讯使用的请求版本。它是 Broker 端参数 inter.broker.protocol.version 值，主要为了确定位移主题消息格式的版本。

config: OffsetConfig, // 内部位移主题 \_\_consumer\_offsets 配置类，包含了与位移管理相关的重要参数比如：位移主题日志段大小设置、位移主题备份因子、位移主题分区数配置等。

val replicaManager: ReplicaManager, // 副本管理器类，用来获取分区对象、日志对象以及写入分区消息的目的。

time: Time,

metrics: Metrics) extends Logging with KafkaMetricsGroup {

// 压缩器类型，主要向位移主题 \_\_consumer\_offsets 写入消息时执行压缩操作

private val compressionType: CompressionType = CompressionType.forId(config.offsetsTopicCompressionCodec.codec)

// 消费者组元数据容器，内部保存 Broker 管理的所有消费者组的数据

private val groupMetadataCache \= new Pool\[String, GroupMetadata\]

/\* lock protecting access to loading and owned partition sets \*/

private val partitionLock \= new ReentrantLock()

// 位移主题 \_\_consumer\_offsets 下正在执行加载操作的分区

/\* partitions of consumer groups that are being loaded, its lock should be always called BEFORE the group lock if needed \*/

private val loadingPartitions: mutable.Set\[Int\] = mutable.Set()

// 位移主题 \_\_consumer\_offsets 下完成加载操作的分区

/\* partitions of consumer groups that are assigned, using the same loading partition lock \*/

private val ownedPartitions: mutable.Set\[Int\] = mutable.Set()

/\* shutting down flag \*/

private val shuttingDown \= new AtomicBoolean(false)

// 位移主题 \_\_consumer\_offsets 总分区数

/\* number of partitions for the consumer metadata topic \*/

@volatile private var groupMetadataTopicPartitionCount: Int = \_

/\* single-thread scheduler to handle offset/group metadata cache loading and unloading \*/

private val scheduler \= new KafkaScheduler(threads = 1, threadNamePrefix = "group-metadata-manager-")

....

}

这里我们来重点剖析下关键属性：

1.  brokerId: 所在 Broker Id，即 broker.id 参数值
2.  interBrokerProtocolVersion: 它是 Broker 端参数 inter.broker.protocol.version 值，主要为了确定位移主题消息格式的版本。
3.  config: 内部位移主题 \_\_consumer\_offsets 配置类，包含了与位移管理相关的重要参数比如：位移主题日志段大小设置、位移主题备份因子、位移主题分区数配置等。
4.  replicaManager: 副本管理器类，用来获取分区对象、日志对象以及写入分区消息的目的。
5.  compressionType：压缩器类型，主要向位移主题 \_\_consumer\_offsets 写入消息前可以选择压缩操作，是否压缩由 Broker 端参数 [offsets.topic.compression.codec](http://offsets.topic.compression.codec/) 值来决定，默认不进行压缩。如果位移主题占用的磁盘空间比较多的话，可以考虑启用压缩，以节省资源。
6.  groupMetadataCache：个人认为这是最重要的属性，消费者组元数据容器，内部保存 Broker 上 GroupCoordinator 组件管理的所有消费者组的数据。其中 Key 为消费者组名称，Value 为消费者组元数据 GroupMetadata。通过该字段实现对消费者组的添加、删除和遍历操作。
7.  loadingPartitions：位移主题 \_\_consumer\_offsets 下正在执行加载操作的分区集合。所谓的加载是指读取位移主题消息数据，填充 GroupMetadataCache 字段的操作。
8.  ownedPartitions：位移主题 \_\_consumer\_offsets 下完成加载操作的分区集合。
9.  scheduler：消费者组元数据调度器。
10.  groupMetadataTopicPartitionCount: 位移主题 \_\_consumer\_offsets 总分区数。它是 Broker 端参数[offsets.topic.num.partitions](http://offsets.topic.num.partitions/) 的值，默认是 50 个分区。

在这些字段中，个人认为 [groupMetadataCache](http://groupmetadatacache/) 是最重要的，该类大量使用该字段实现对消费者组的管理。

接下来我们来剖析该类的重要方法，主要剖析对消费者组元数据的管理。

## **5.2 启动方法**

/\* single-thread scheduler to handle offset/group metadata cache loading and unloading \*/

private val scheduler \= new KafkaScheduler(threads = 1, threadNamePrefix = "group-metadata-manager-")

// 启动时执行必要的初始化操作，包括获取主题分区数和启动元数据过期功能。

def startup(retrieveGroupMetadataTopicPartitionCount: () => Int, enableMetadataExpiration: Boolean): Unit = {

// 获取组元数据主题分区数，并将其赋值给 groupMetadataTopicPartitionCount。

groupMetadataTopicPartitionCount = retrieveGroupMetadataTopicPartitionCount()

// 启动消费者组元数据管理调度器。

scheduler.startup()

// 如果启用了元数据过期功能

if (enableMetadataExpiration) {

// 使用调度器定期执行 cleanupGroupMetadata() 方法，以清理过期的组元数据。config.offsetsRetentionCheckIntervalMs：配置的偏移量保留检查间隔时间。

scheduler.schedule(name = "delete-expired-group-metadata",

// 调度器会按照设定的时间间隔执行 cleanupGroupMetadata() 方法。

fun = () => cleanupGroupMetadata(),

period = config.offsetsRetentionCheckIntervalMs,

unit = TimeUnit.MILLISECONDS)

}

}

## **5.3 查询消费者组元数据**

/\*\*

\* 返回给定消费者组的元数据信息，若该组信息不存在，返回 None

\*/

def getGroup(groupId: String): Option\[GroupMetadata\] = {

Option(groupMetadataCache.get(groupId))

}

/\*\*

\* 返回给定消费者组的元数据信息，若不存在，则需要根据 createIfNotExist 参数值决定是否需要添加该消费者组

\*/

def getOrMaybeCreateGroup(groupId: String, createIfNotExist: Boolean): Option\[GroupMetadata\] = {

if (createIfNotExist)

// 若不存在且允许添加，则添加一个状态是 Empty 的消费者组元数据对象

Option(groupMetadataCache.getAndMaybePut(groupId, new GroupMetadata(groupId, Empty, time)))

else

Option(groupMetadataCache.get(groupId))

}

对于 [GroupCoordinator](http://groupcoordinator/) 来说，会大量使用这两个方法来获取给定消费者组的元数据。

1.  getGroup()：如果该消费者组信息不存在，就返回 None，这表明消费者组确实不存在，或者该消费者组对应的 [GroupCoordinator](http://groupcoordinator/) 组件变更到其他 Broker 上了。
2.  getOrMaybeCreateGroup()：如果该消费者组信息不存在，会根据 [createIfNotExist](http://createifnotexist/) 参数值决定是否需要添加该消费者组。所以该方法是**消费者组第一个成员加入消费者组时被调用的，用于创建消费者组**。

## **5.4 添加消费者组元数据**

def addGroup(group: GroupMetadata): GroupMetadata = {

// 调用 putIfNotExists 将给定组添加到 groupMetadataCache 中

val currentGroup \= groupMetadataCache.putIfNotExists(group.groupId, group)

if (currentGroup != null) {

currentGroup

} else {

group

}

}

可以看到很简单吧，就是通过调用 [putIfNotExists](http://putifnotexists/) 方法将给定组添加进 [groupMetadataCache](http://groupmetadatacache/) 中。

## **5.4 删除消费者组元数据**

当 Broker 卸任某些消费者组的 [GroupCoordinator](http://groupcoordinator/) 角色时，它需要将这些消费者组从 [groupMetadataCache](http://groupmetadatacache/) 中全部移除掉，源码如下：

def removeGroupsForPartition(offsetsPartition: Int,

coordinatorEpoch: Option\[Int\],

onGroupUnloaded: GroupMetadata => Unit): Unit = {

// Kafka 位移主题分区

val topicPartition \= new TopicPartition(Topic.GROUP\_METADATA\_TOPIC\_NAME, offsetsPartition)

info(s"Scheduling unloading of offsets and group metadata from $topicPartition")

// 创建异步任务，移除组信息和位移信息

scheduler.schedule(topicPartition.toString, () => removeGroupsAndOffsets(topicPartition, coordinatorEpoch, onGroupUnloaded))

}

// 用来移除消费者组信息和位移信息

private \[group\] def removeGroupsAndOffsets(topicPartition: TopicPartition,

coordinatorEpoch: Option\[Int\],

onGroupUnloaded: GroupMetadata => Unit): Unit = {

val offsetsPartition \= topicPartition.partition

if (maybeUpdateCoordinatorEpoch(offsetsPartition, coordinatorEpoch)) {

// 移除位移数量

var numOffsetsRemoved \= 0

// 移除消费者组数量

var numGroupsRemoved \= 0

debug(s"Started unloading offsets and group metadata for $topicPartition for " +

s"coordinator epoch $coordinatorEpoch")

inLock(partitionLock) {

// we need to guard the group removal in cache in the loading partition lock

// to prevent coordinator's check-and-get-group race condition

// 移除位移主题 \_\_consumer\_offsets 下完成加载操作的分区集合中特定位移主题分区记录

ownedPartitions.remove(offsetsPartition)

// 移除位移主题 \_\_consumer\_offsets 下正在执行加载操作的分区集合中特定位移主题分区记录

loadingPartitions.remove(offsetsPartition)

// 遍历所有消费者组信息

for (group <- groupMetadataCache.values) {

// 如果该组信息保存在特定位移主题分区中

if (partitionFor(group.groupId) == offsetsPartition) {

// 执行组卸载逻辑

onGroupUnloaded(group)

// 将组信息从 groupMetadataCache 中移除

groupMetadataCache.remove(group.groupId, group)

// 把消费者组从 producer 对应的消费者组集合中移除

removeGroupFromAllProducers(group.groupId)

// 递增已移除组计数器

numGroupsRemoved += 1

// 递增已移除位移值计数器

numOffsetsRemoved += group.numOffsets

}

}

}

info(s"Finished unloading $topicPartition for coordinator epoch $coordinatorEpoch. " +

s"Removed $numOffsetsRemoved cached offsets and $numGroupsRemoved cached groups.")

} else {

info(s"Not removing offsets and group metadata for $topicPartition " +

s"in epoch $coordinatorEpoch since current epoch is ${epochForPartitionId.get(topicPartition.partition)}")

}

}

![](https://article-images.zsxq.com/liRb9FHVgMDQTQnA8knjZ7lbVa0N)

## **5.5 加载消费者组元数据**

private def loadGroup(group: GroupMetadata, offsets: Map\[TopicPartition, CommitRecordMetadataAndOffset\],

pendingTransactionalOffsets: Map\[Long, mutable.Map\[TopicPartition, CommitRecordMetadataAndOffset\]\]): Unit = {

// offsets are initialized prior to loading the group into the cache to ensure that clients see a consistent

// view of the group's offsets

trace(s"Initialized offsets $offsets for group ${group.groupId}")

// 初始化消费者组的位移信息，将位移值添加到 offsets 字段标识的消费者组提交位移元数据中，实现加载消费者组订阅分区提交位移的目的。

group.initializeOffsets(offsets, pendingTransactionalOffsets.toMap)

// 调用 addGroup 方法添加消费者组，将该消费者组元数据对象添加到消费者组元数据缓存，实现加载消费者组元数据的目的。

val currentGroup \= addGroup(group)

if (group != currentGroup)

debug(s"Attempt to load group ${group.groupId} from log with generation ${group.generationId} failed " +

s"because there is already a cached group with generation ${currentGroup.generationId}")

}

## **06 总结**

这里，我们一起来总结一下这篇文章的重点。

1、通过「**场景驱动**」的方式从消费者组拉取数据过程中需要获取元数据出发，抛出「**消费者组元数据**」、「**消费者组成员元数据**」两种元数据管理。

2、带你分别剖析了「**消费者组成员元数据**」、「**消费者组元数据**」、「**消费者组元数据管理器**」的内部实现细节。

下篇我们来深度剖析「**Coordinator 工作原理**」，大家期待，我们下期见。