大家好，我是**华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 消费者组****元数据设计原理剖析**」，了解了消费者组内部是如何实现「**元数据**」的，主要包括「**组成员元数据**」、「**组元数据**」、「**组元数据管理器**」。今天我们开启消费端源码的征程，这是第八篇来深度聊聊「**Coordinator 工作原理**」，看看 Kafka Coordinator 是如何管理消费者的。

![](https://article-images.zsxq.com/FnPE7tUKzvoF8JQXvlfBI5t3jPGL)

## **01 总体概述**

在前面几篇中，我们或多或少都提到过消费者端一个重要角色，那就是「**协调器 Coordinator**」。在消费者拉取消息之前需要先进行确定「**消费者组协调器**」，然后向该 「**消费者组协调器**」发送请求「**加入消费者组**」，此时会进行分区分配。

关于消费者组的相关组件，在 Kafka 中主要有两个，其中在消费者端是：「**消费者组协调器 ConsumerCoordinator**」、在服务端是 「**组协调器 GroupCoordinator**」。

在 KafkaConsumer 初始化的时候主要就是这两个组件进行「**网络请求**」，「**完成消费者加入**」、「**分区消费分配策略制定**」等工作。

本篇我们就来简单的聊聊这两个组件，涉及的源码：

「**ConsumerCoordinator**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java](https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java)

「**AbstractCoordinator**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/AbstractCoordinator.java](https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/AbstractCoordinator.java)

## **02 ConsumerCoordinator 工作原理**

**首先消费者组协调器 ConsumserCoordinator 和服务端 GroupCoordinator 是配合起来使用的，其目的是管理消费者**，主要包括「**管理消费者上下线**」、「**分发消费分区方案**」等等。

不过，我们在剖析消费者协调器之前，先来看几个重要概念。

## **2.1 什么是 Offset 管理**

对于每个 consumer 客户端来说，都会在其内存中保存它消费每个主题分区的「**消费 Offset**」，这里的「**消费 Offset**」表示该 「**消费者组**」 消费到这个主题分区的哪个 offset 了。

那么**为什么要设计这个消费 Offset 呢？**主要因为当某个消费者上下线时会造成该主题分区的消费被切换到别的消费者上面，而新的消费者并不知道主题分区消费到哪个位置了，此时就需要服务端保存「**消费 Offset**」，这样新的消费者才能正确得到「**消费 Offset**」接着消费，就不会造成重复消费和漏掉消费的情况。

也就是说消费者会定期向服务端「**提交 Offset**」，老版本是写入 ZooKeeper 中，显然消费者提交 offset 是个高频操作。而 ZooKeeper 是做分布式协调的，轻量级元数据存储，它并不适合高并发的请求。

所以后来的版本放弃 ZooKeeper 做「**消费 Offset**」的保存而改用内部主题 「**\_\_consumser\_offsets**」，其 key 是 [group.id+topic+分区号](http://group.xn--id+topic+-en4o29h2yc/) ，value 是当前消费的 offset 值。每隔一段时间「**\_\_consumser\_offsets**」 会把 key重复的历史数据删除，也就是说只保留最新的一条 key 值。内部主题「**\_\_consumser\_offsets**」的分区数是50，这样设计的好处就是能很好地抵挡高并发的请求。

![](https://article-images.zsxq.com/Fg733zQqaMY-DrG3bAOOc98KKuql)

## **2.2 什么是 GroupCoordinator**

对于每个消费者组来说，都会选择一个 Broker 来作为自己的 [GroupCoordinator](http://groupcoordinator/)，它主要负责监控消费者组里各个消费者的心跳来判断其是否宕机或者运行异常，然后开启 Rebalance 操作重新把分区分配给各个消费者。

消费者组中的消费者刚启动的时候，就会跟对应 [GroupCoordinator](http://groupcoordinator/) 所在的 Broker 建立通信，[GroupCoordinator](http://groupcoordinator/) 会分配主题分区给这个消费者消费。[GroupCoordinator](http://groupcoordinator/) 会尽量均匀地分配分区给各个消费者进行消费。

关于分区分配策略可以点击：[【原理分析系列第十四篇】图解 Kafka 消费者分区分配策略](https://articles.zsxq.com/id_esdb8l8rwxb7.html)

那么如何找到这个 **GroupCoordinator** 呢？

[GroupCoordinator](http://groupcoordinator/) 组件是唯一操作位移主题的组件，它在内部对位移主题进行读写操作。每个 Broker 在启动时，都会启动 [GroupCoordinator](http://groupcoordinator/) 组件，但是一个消费者组只能被一个 [GroupCoordinator](http://groupcoordinator/) 组件所管理。

位移主题某个特定分区 Leader 副本所在的 Broker 被选定为指定消费者组的 Coordinator。每个消费者都有一个消费者组id，其规则就是：消费者首先按照消费者组 ID 的 [hashCode](http://hashcode/) 取模后除以 [\_\_consumer\_offsets](http://__consumer_offsets/) 的分区个数，计算得到 [BrokerId](http://brokerid/) ，那么该 Broker 上的 [GroupCoordinator](http://groupcoordinator/) 负责为该消费者组进行提供服务，主要负责接收和管理对应消费者组提交的「**消费 Offset**」。

![](https://article-images.zsxq.com/FphPAjDFqZ2ZO9UZ-jeDbsl4dOKa)

def partitionFor(groupId: String): Int = Utils.abs(groupId.hashCode) % groupMetadataTopicPartitionCount

## **2.3 什么是消费者重平衡 Rebalance**

[ConsumerCoordinator](http://consumercoordinator/) 和 [GroupCoordinator](http://groupcoordinator/) 之间最重要的职责就是「**负载执行消费者重平衡的操作**」。消费者重平衡是指在分区或消费者有变动的时候，需要重新给消费者内的消费者分配要消费的分区。

**Rebalance 触发条件如下：**

1.  当 Consumer Group 组成员数量发生变化(主动加入或者主动离组，故障下线等)。
2.  当订阅主题数量发生变化。
3.  当订阅主题的分区数发生变化。

好了，接下来我们来剖析下 [ConsumerCoordinator](http://consumercoordinator/) 和 [GroupCoordinator](http://groupcoordinator/) 的工作流程。

##   
**2.4 协调者工作原理**

如下图所示，Kafka 集群有 3 个节点，同一个消费者组下有 3 个消费者去消费 Topic。

![](https://article-images.zsxq.com/FtEPGp3IqYoMHuA98VsoAk6af7tp)

流程如下：

1.  消费者启动的时候，根据元数据和消费者组 id 得到对应的 GroupCoordinator 对应的 Broker 后，向这个 Broker发送JoinGroupRequest 请求。
2.  GroupCoordinator 会选出一个 consumer 作为消费者组的 Leader，然后响应这些消费者是否注册成功，消费者会收到 JoinGroupResponse 响应。在响应中对于 consumer leader 还会把它是 Leader 的信息发给它。
3.  consumer leader 根据服务端的响应信息指定消费的分配方案。
4.  consumer leader 把分配方案发送给 GroupCoordinator。
5.  GroupCoordinator 把分配方案发送给消费者组下的所有消费者。
6.  消费者开始根据收到的消费方案找到对应的 Leader Partition 所在的 Broker 消费。
7.  消费者处理完消息后会把消费的 Offset 发给 GroupCoordinator，GroupCoordinator 会将 Offset 保存在 \_\_consumer\_offsets 里。

## **03 类定义源码实现**

[ConsumerCoordinator](http://consumercoordinator/) 类组件实现了与服务端的 [GroupCoordinator](http://groupcoordinator/) 进行交互，[ConsumerCoordinator](http://consumercoordinator/) 类继承了抽象类 [AbstractCoordinator](http://abstractcoordinator/)，我们先学习抽象类 [AbstractCoordinator](http://abstractcoordinator/) 的源码。

我们这篇只涉及 [AbstractCoordinator](http://abstractcoordinator/) 类、 [ConsumerCoordinator](http://consumercoordinator/) 类的核心字段的剖析，其关键方法会放在后面章节进行剖析。

## **3.1 AbstractCoordinator 类定义**

public abstract class AbstractCoordinator implements Closeable {

public static final String HEARTBEAT\_THREAD\_PREFIX \= "kafka-coordinator-heartbeat-thread";

public static final int JOIN\_GROUP\_TIMEOUT\_LAPSE \= 5000;

// 消费者成员状态

protected enum MemberState {

UNJOINED, // the client is not part of a group

PREPARING\_REBALANCE, // the client has sent the join group request, but have not received response

COMPLETING\_REBALANCE, // the client has received join group response, but have not received assignment

STABLE; // the client has joined and is sending heartbeats

public boolean hasNotJoinedGroup() {

return equals(UNJOINED) || equals(PREPARING\_REBALANCE);

}

}

private final Logger log;

// 心跳对象

private final Heartbeat heartbeat;

// 消费者协调器的指标信息

private final GroupCoordinatorMetrics sensors;

// 消费者组重分配配置

private final GroupRebalanceConfig rebalanceConfig;

protected final Time time;

// 负责网络通信

protected final ConsumerNetworkClient client;

// Node类型，保存着服务端 GroupCoordinator 的节点。

private Node coordinator \= null;

// 标记重新发送 JoinGroupRequest 请求。

private boolean rejoinNeeded \= true;

// 是否重新发送 JoinGroupRequest 请求。

private boolean needsJoinPrepare \= true;

// 维持心跳的线程

private HeartbeatThread heartbeatThread \= null;

// 加入 GroupCoordinator 的异步请求类对象

private RequestFuture<ByteBuffer> joinFuture = null;

// 客户端查找 GroupCoordinator 的异步请求类对象。

private RequestFuture<Void> findCoordinatorFuture = null;

// 查找协调器失败异常

private volatile RuntimeException fatalFindCoordinatorException \= null;

// 服务端 GroupCoordinator 的年代信息

private Generation generation \= Generation.NO\_GENERATION;

// 最后一次 rebalance 的开始时间

private long lastRebalanceStartMs \= -1L;

// 最后一次 rebalance 的结束时间

private long lastRebalanceEndMs \= -1L;

private long lastTimeOfConnectionMs \= -1L; // starting logging a warning only after unable to connect for a while

// 初始成员状态

protected MemberState state \= MemberState.UNJOINED;

....

}

我们来剖析下该类的重要字段：

1.  MemberState 是一个枚举，列举了消费者和服务端 GroupCoordinator 交互的 4 个状态。
2.  UNJOINED：表示消费者还没加入到服务端 GroupCoordinator 中。
3.  PREPARING\_REBALANCE：表示消费者发出了加入 GroupCoordinator 的请求，但是还没收到响应。
4.  COMPLETING\_REBALANCE：表示消费者收到了加入 GroupCoordinator 的响应。
5.  STABLE：表示消费者发出了加入 GroupCoordinator 中，并正在向 GroupCoordinator 发送心跳。
6.  heartbeat：它是 Heartbeat 类对象，用来保存与 GroupCoordinator 维持心跳的一些配置数据，比如：最近发送心跳的时间，下次要发送心跳的时间等。
7.  rebalanceConfig：它是消费者组重分配配置。
8.  client：它是 ConsumerNetworkClient 类对象，提供底层通讯服务。
9.  coordinator：它是 Node 类对象，保存着 GroupCoordinator 的节点信息。
10.  rejoinNeeded：bool 类型，是否重新发送 JoinGroupRequest 请求。
11.  needsJoinPrepare：bool 类型，是否重新发送 JoinGroupRequest 请求。
12.  heartbeatThread：维持心跳的线程。加入到GroupCoordinator后就会发起定时的心跳线程。
13.  joinFuture：加入 GroupCoordinator 的异步请求类对象。
14.  findCoordinatorFuture：客户端查找 GroupCoordinator 的异步请求类对象。
15.  generation：服务端 GroupCoordinator 发过来的 Rebalance 年代。为了区分上次 Rebalance 延迟造成新的 Rebalance 被覆盖，造成数据不一致。
16.  lastRebalanceStartMs：最后一次 rebalance 的开始时间。
17.  lastRebalanceEndMs：最后一次 rebalance 的结束时间。
18.  state：初始成员状态。

抽象类 [AbstractCoordinator](http://abstractcoordinator/) 核心字段剖析完了，下面开始剖析 [ConsumerCoordinator](http://consumercoordinator/) 类的核心字段。

## **3.2 ConsumerCoordinator 类定义**

public final class ConsumerCoordinator extends AbstractCoordinator {

// 消费者组重平衡的配置信息

private final GroupRebalanceConfig rebalanceConfig;

private final Logger log;

// 订阅分区任务列表

private final List<ConsumerPartitionAssignor> assignors;

// 消费者元数据

private final ConsumerMetadata metadata;

// 消费者协调器的指标信息

private final ConsumerCoordinatorMetrics sensors;

// 订阅状态，保存了主题分区和 offset 的对应关系。

private final SubscriptionState subscriptions;

// 默认的偏移量提交回调

private final OffsetCommitCallback defaultOffsetCommitCallback;

// 是否启用自动偏移量提交

private final boolean autoCommitEnabled;

// 自动偏移量提交的间隔时间。

private final int autoCommitIntervalMs;

// 消费者拦截器

private final ConsumerInterceptors<?, ?> interceptors;

// 待处理的异步提交偏移量的数量计数器

private final AtomicInteger pendingAsyncCommits;

// 异步提交偏移量完成的队列，线程安全的

private final ConcurrentLinkedQueue<OffsetCommitCompletion> completedOffsetCommits;

// 是否为消费者组的 Leader

private boolean isLeader \= false;

// 加入消费者组的订阅信息

private Set<String> joinedSubscription;

// 元数据的快照，保存分区信息，用来监控分区信息是否变了。

private MetadataSnapshot metadataSnapshot;

// 分配结果的快照，保存分区分配结果。

private MetadataSnapshot assignmentSnapshot;

// 下一次自动提交偏移量的定时器

private Timer nextAutoCommitTimer;

// 标识异步提交偏移量是否被取消。

private AtomicBoolean asyncCommitFenced;

// 消费组元数据

private ConsumerGroupMetadata groupMetadata;

private final boolean throwOnFetchStableOffsetsUnsupported;

// 等待处理的提交偏移量请求。

private PendingCommittedOffsetRequest pendingCommittedOffsetRequest \= null;

....

}

该类用于管理消费者的协调，包括分区分配、偏移量提交和心跳等操作，我们来剖析下该类的重要字段：

1.  assignors：订阅分区任务列表，列表的元素是 ConsumerPartitionAssignor。ConsumerPartitionAssignor 里是发送JoinGroupRequest 请求中消费者支持的分区算法等信息。GroupCoordinator 会从所有消费者都支持的算法中选择一个，并通知 leader consumer 使用这个分区算法进行分配。一个消费可以包含多个分区算法，可以在[partition.assignment.strategy](http://partition.assignment.strategy/) 参数中配置。
2.  metadata：ConsumerMetadata 类对象。保存着消费者消费的元数据，如分区和 offset 的对应关系，要消费的主题等。
3.  subscriptions：SubscriptionState 类对象。 保存着消费者消费的分区和 offset 的对应关系。
4.  autoCommitEnabled：bool类型，是否自动提交 offset。
5.  completedOffsetCommits：[ConcurrentLinkedQueue<OffsetCommitCompletion>](http://concurrentlinkedqueueoffsetcommitcompletion/) 类对象。这是一个线程安全的队列，队列的元素是提交 offset 后处理响应的回调对象。OffsetCommitCompletion 是发出提交 offset 的请求后，会把处理响应的回调对象放到这个队列里。
6.  isLeader：是否被 GroupCoordinator 选为 Leader consumer。
7.  metadataSnapshot：元数据的快照，保存分区信息，用来监控分区信息是否变了，如果变了就需要重新分配主题分区，也就是 Rebalance。
8.  assignmentSnapshot：分配结果的快照，保存分区分配结果。
9.  groupMetadata：保存着消费者组的id、消费者的id等元数据。

## **04 总结**

这里，我们一起来总结一下这篇文章的重点。

1、通过「**场景驱动**」的方式从消费者调用出发，抛出「**消费者组协调器 ConsumerCoordinator**」、「**组协调器 GroupCoordinator**」，在 KafkaConsumer 初始化的时候主要就是这两个组件进行「**网络请求**」，「**完成消费者加入**」、「**分区消费分配策略制定**」等工作。

2、带你剖析了「**消费者组协调器 ConsumerCoordinator**」的工作原理，从「**什么是 Offset 管理**」、「**什么是 GroupCoordinator**」、「**什么是消费者重平衡 Rebalance**」、「**协调者工作原理**」四个维度进行剖析和梳理。

3、最后带你剖析了 「**消费者组协调器 ConsumerCoordinator**」以及 「**其抽象父类 AbstractCoordinator**」的核心字段。

下篇我们来深度剖析「**消费者重平衡机制流程剖析**」，大家期待，我们下期见。