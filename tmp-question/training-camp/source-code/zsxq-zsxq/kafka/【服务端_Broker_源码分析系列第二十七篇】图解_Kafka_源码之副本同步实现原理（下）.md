大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端 副本同步实现原理**」的上半部分，了解了副本同步是如何做到一致性的，从今天开始，我们接着来深度剖析 Kafka「**Broker**」的底层源码实现，我们接着来深度聊聊「**Kafka 服务端 副本同步实现原理**」的下半部分，看看 Kafka 副本之间是如何同步消息的。

  
![](https://article-images.zsxq.com/Fm17Jay2pitYJqKlI11eie2dKXlv)

## **01 总体概述**

当 「**KafkaController**」启动之后，就会选举分区 Leader 副本，剩余的就会成为 Follower 副本。其中 Leader 副本负责消息收发，而 Follower 副本只负责拉取 Leader 副本上的消息内容，并保存到自己的 Log 日志中。当 Leader 副本挂了的时候，会从 ISR副本集合列表中选举出一个充当 Leader 副本，继续支持消息的收发功能。

这是一种的典型的主从同步机制，是分布式系统中常用的保证数据安全的机制。

那么 **Kafka 副本之间是如何同步消息的呢？在同步的过程中又是如何保证副本之间数据一致性的呢？**

带着第一个问题，我们开启今天的话题，继续来聊聊「**副本同步**」的实现机制。

##   
**02 拉取线程抽象基类 AbstractFetcherThread**

在 Kafka 中，Follower 副本从 Leader 副本拉取数据是由一个拉取线程来完成的，它的名字叫：[ReplicaFetcherThread](http://replicafetcherthread/)，今天话题的主角就是它。

先来看看它的基类：[AbstractFetcherThread](http://abstractfetcherthread/)。它内部定义和实现了很多重要的字段和方法，是我们剖析[ReplicaFetcherThread](http://replicafetcherthread%20/) 源码的基础。同时，[AbstractFetcherThread](http://abstractfetcherthread/) 类的源码给出了很多子类需要实现的方法。

「**AbstractFetcherThread**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/AbstractFetcherThread.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/AbstractFetcherThread.scala)

## **2.1 类定义以及重要属性**

先来看看其类定义以及重要属性，源码如下：

/\*\*

\* Abstract class for fetching data from multiple partitions from the same broker.

\*/

abstract class AbstractFetcherThread(name: String, // 线程名称

clientId: String, // Client Id，用于日志输出

val sourceBroker: BrokerEndPoint, // 数据源 Broker 地址

failedPartitions: FailedPartitions, // 处理过程中出现失败的分区

fetchBackOffMs: Int = 0, // 获取操作重试间隔

isInterruptible: Boolean = true, // 线程是否允许被中断

val brokerTopicStats: BrokerTopicStats) // Broker端主题监控指标

extends ShutdownableThread(name, isInterruptible) {

// 获取的消息数据

type FetchData \= FetchResponse.PartitionData\[Records\]

// Leader Epoch 数据

type EpochData \= OffsetForLeaderEpochRequestData.OffsetForLeaderPartition

// 分区状态

private val partitionStates \= new PartitionStates\[PartitionFetchState\]

// 分区 ReentrantLock

protected val partitionMapLock \= new ReentrantLock

private val partitionMapCond \= partitionMapLock.newCondition()

来看下构造函数的参数：

1.  name: 拉取线程名称。
2.  clientId：用于日志输出。
3.  sourceBroker：数据源 Broker 地址。比较重要，**因为它决定了 Follower 副本从哪个 Broker 上拉取数据，即 Leader 副本所在的 Broker 地址**。
4.  failedPartitions：处理过程中出现失败的分区。
5.  fetchBackOffMs：获取操作重试间隔。
6.  isInterruptible：线程是否允许被中断。
7.  brokerTopicStats：Broker端主题监控指标。

除了这几个字段外，还定义了两个 type 类型。用关键字 type 定义一个类型，属于 Scala 中比较高阶的语法特性。从某种程度上，你可以把它当成一个快捷方式，比如 FetchData 这句：

type FetchData \= FetchResponse.PartitionData\[Records\]

它类似于一个快捷方式：以后凡是源码中需要用到 [FetchResponse.PartitionData\[Records\]](http://FetchResponse.PartitionData%5BRecords%5D%20) 的地方，都可以简单地使用 FetchData 替换掉，非常简洁方便。另外一个 [EpochData](http://epochdata%20/) 也是同样的用法。

FetchData 定义里的 PartitionData 类型，**它是客户端 Clients 工程中 FetchResponse 类定义的嵌套类**。

其中 [FetchResponse](http://fetchresponse%20/) 类封装的是 [FETCH](http://fetch%20/) 请求的 Response 对象，里面的 [PartitionData](http://partitiondata%20/) 类保存的是 Response 中单个分区数据拉取的各项数据信息，包括「**从该分区的 Leader 副本拉取的消息**」、「**该分区的 HW 值**」、「**该分区的日志起始位移值 LogStartOffset**」等。

接着来看看 PartitionData。

public static final class PartitionData<T extends BaseRecords\> {

private final FetchResponseData.FetchablePartitionResponse partitionResponse;

// Derived fields

// 期望的 Read Replica，在 KAFKA 2.4 版本之后支持部分 Follower 副本可以对外提供读服务

private final Optional<Integer> preferredReplica;

// 该分区对应的已终止事务列表

private final List<AbortedTransaction> abortedTransactions;

// 错误码

private final Errors error;

public PartitionData(Errors error,

long highWatermark, // 高水位值

long lastStableOffset, // 最新 LSO 值

long logStartOffset, // 最新 Log Start Offset 值

Optional<Integer> preferredReadReplica,

List<AbortedTransaction> abortedTransactions,

Optional<FetchResponseData.EpochEndOffset> divergingEpoch,

T records) { // 消息集合，最重要的字段

this.preferredReplica = preferredReadReplica;

this.abortedTransactions = abortedTransactions;

this.error = error;

}

....

}

这些字段中，除了我们已经非常熟悉的 highWatermark 和 logStartOffset 等字段外，还有一些属于比较高阶的用法：

1.  preferredReadReplica：用于指定可对外提供读服务的 Follower 副本。
2.  abortedTransactions：用于保存该分区当前已终止事务列表。
3.  lastStableOffset：是最新的 LSO 值，属于 Kafka 事务的概念。

在该类中，最需要你重点关注的字段是 **records** 。因为它保存实际的消息集合，是我们最关心的数据。

实际上，**在 Kafka 源码中，有很多名为 PartitionData 的嵌套类**。很多请求类型中的数据都是按分区层级进行分组的，因此源码很自然地在这些请求类中创建了同名的嵌套类。我们在研究源码时，一定要注意区分 PartitionData嵌套类是定义在哪类请求中的，不同类型请求中的 PartitionData 类字段是完全不同的。

## **2.2 分区、副本读取状态类**

接着，我们把视角拉回 AbstractFetcherThread，我们看到它还封装了一个名为[PartitionStates\[PartitionFetchState\]](http://PartitionStates%5BPartitionFetchState%5D) 类型的字段。

来看下其泛型的参数类型 PartitionFetchState 类。它是指**分区的读取状态**，**内部保存了分区的已读位移值和对应的副本状态**。

这里有两个状态，「**分区读取状态**」、「**副本读取状态**」。

而「**副本读取状态**」是由 ReplicaState 接口来表示，源码如下：

sealed trait ReplicaState

// 截断中

case object Truncating extends ReplicaState

// 获取中

case object Fetching extends ReplicaState

从源码中可以得出，副本读取状态有「**截断中**」和「**获取中**」两个：当副本执行截断操作时，副本状态被设置成Truncating，而当副本被读取时，副本状态被设置成 Fetching。

接着来看「**分区读取状态**」，它有三个状态，分别是：

1.  **可获取**：表示副本获取线程当前能够读取数据。
2.  **截断中**：表示分区副本正在执行截断操作（比如该副本刚刚成为 Follower 副本就需要截断操作）。
3.  **被推迟**：表示副本获取线程获取数据时出现错误，需要等待一段时间后重试。

不过需要注意的是，「**分区读取状态**」中的可获取、截断中跟「**副本读取状态**」中的获取中、截断中非严格对应的，**本对于分区来说，它是否能够被获取的条件要比副本更加严格**。

case class PartitionFetchState(fetchOffset: Long,

lag: Option\[Long\],

currentLeaderEpoch: Int,

delay: Option\[DelayedItem\],

state: ReplicaState,

lastFetchedEpoch: Option\[Int\]) {

// 分区可获取的条件是副本处于 Fetching 且未被推迟执行

def isReadyForFetch: Boolean = state == Fetching && !isDelayed

// 副本处于 ISR 的条件：没有 lag

def isReplicaInSync: Boolean = lag.isDefined && lag.get <= 0

// 分区处于截断中状态的条件：副本处于 Truncating 状态且未被推迟执行

def isTruncating: Boolean = state == Truncating && !isDelayed

// 分区被推迟获取数据的条件：存在未过期的延迟任务

def isDelayed: Boolean = delay.exists(\_.getDelay(TimeUnit.MILLISECONDS) > 0)

....

}

1.  这里你只需要重点了解下 [isReadyForFetch](http://isreadyforfetch%20/) 和 [isTruncating](http://istruncating%20/) 这两个方法。**对于副本获取线程做的事情就是**：「**日志截断**」、「**消息获取**」。
2.  isReplicaInSync：**它常被用在副本限流的场景中**。
3.  isDelayed：它是用来判断是否需要推迟获取对应分区的消息。

源码会不断地调整那些不需要推迟的分区的读取顺序，以保证读取的公平性。这个公平性是在 [PartitionStates](http://partitionstates%20/) 类中实现的，它本质上会接收一组要读取的主题分区，然后以轮询的方式依次读取这些分区以确保公平性。

public class PartitionStates<S> {

private final LinkedHashMap<TopicPartition, S> map = new LinkedHashMap<>();

private final Set<TopicPartition> partitionSetView = Collections.unmodifiableSet(map.keySet());

......

public void updateAndMoveToEnd(TopicPartition topicPartition, S state) {

// 先移除分区

map.remove(topicPartition);

// 再插入进去，此时该分区就排在最后一位了

map.put(topicPartition, state);

// 更新集合大小

updateSize();

}

......

}

上面说，该类是用**轮询的方式来处理要读取的多个分区，那它是怎么实现的呢？**

简单来说，它就是通过 [LinkedHashMap](http://linkedhashmap%20/) 数据结构来保存所有的主题分区。我们都知道在 [LinkedHashMap](http://linkedhashmap%20/) 中的**元素有明确的迭代顺序，通常就是元素被插入的顺序**。

说了这么多，可能你还比较迷糊，这里我举例说明下：

假如 Kafka 要读取某个主题上的不同分区上的消息：P0、P1、P2、P3、P4。 如果插入的顺序也是 P0、P1、P2、P3、P4。 那么**首先会读取分区 p0**，一旦当 p0 被读取后，**为了确保其他各个分区都有机会被读取到，需要将 p0 插入到分区列表的最后一位**，这就是上面 updateAndMoveToEnd 方法要做的事情。

## **2.3 分区集合锁**

// 分区 ReentrantLock

protected val partitionMapLock \= new ReentrantLock

private val partitionMapCond \= partitionMapLock.newCondition()

它主要被用在获取和截断操作中，在这 2 个操作期间不会受到其他线程的干扰。关于 ReentrantLock 锁和 Synchronized 的区别，可以看下面的文章 [https://blog.csdn.net/m0\_53077601/article/details/122819671](https://blog.csdn.net/m0_53077601/article/details/122819671)。

## **2.4 重要方法**

该类中大概有 40 个方法，非常多。这里我们选几个重要的方法来剖析，也是拉取线程所做的最重要的事情：「**构建 FETCH 请求**」、「**执行截断操作**」、「**处理拉取后结果**」。

接下来我们挨个看下。

##   
**2.4.1 processPartitionData**

// process fetched data

protected def processPartitionData(

topicPartition: TopicPartition ,// 读取哪个分区的数据

fetchOffset: Long, // 读取到的最新位移值

partitionData: FetchData // 读取到的分区消息数据

): Option\[LogAppendInfo\] // 写入已读取消息数据前的元数据

该方法是**用来处理读取回来的消息集合**。它是个抽象方法，因此需要子类实现它的逻辑。对于 Follower副本，它是由 [ReplicaFetcherThread](http://replicafetcherthread%20/) 类实现的，接下来会深度剖析。

这里我们需要重点关注的字段是其返回值 [Option\[LogAppendInfo\]](http://Option%5BLogAppendInfo%5D)：

1.  对于 Follower 副本读消息写入自己日志来说，它会返回具体的 LogAppendInfo 实例，而不是 None。
2.  LogAppendInfo 类，在[【服务端 Broker 源码分析系列第十三篇】图解 Kafka 源码之日志 Log 对象操作](https://articles.zsxq.com/id_n9jj4wsw958w.html) 中已经介绍过了。它封装了很多消息数据被写入到日志前的重要元数据信息，比如「**首条消息的位移值**」、「**最后一条消息的位移值**」、「**最大时间戳**」等。

## **2.4.2 truncate**

protected def truncate(

topicPartition: TopicPartition, // 要对哪个分区下副本执行截断操作

truncationState: OffsetTruncationState // Offset + 截断状态

): Unit

case class OffsetTruncationState(

offset: Long, // 位移值

truncationCompleted: Boolean) { // 截断是否完成状态值

def this(offset: Long) = this(offset, true)

override def toString: String = "offset:%d-truncationCompleted:%b".format(offset, truncationCompleted)

}

这里重点方法是 [OffsetTruncationState](http://offsettruncationstate/)，其主要作用是：**告诉 Kafka 要把指定分区下副本截断到哪个位移值**。

## **2.4.3 buildFetch**

protected def buildFetch(

// 一组要读取的分区列表，分区是否可读取取决于 PartitionFetchState 中的状态

partitionMap: Map\[TopicPartition, PartitionFetchState\]):

ResultWithPartitions\[Option\[ReplicaFetch\]\] // 用来封装 FetchRequest.Builder 对象

buildFetch 本质是：为指定分区构建对应的 [FetchRequest.Builder](http://fetchrequest.builder%20/) 对象，而该对象是构建 [FetchRequest](http://fetchrequest%20/) 的核心组件。**在 Kafka 中任何类型的消息读取都是通过给指定 Broker 发送 FetchRequest 请求来完成的**。

## **2.4.4 doWork**

override def doWork(): Unit = {

// 执行副本截断操作

maybeTruncate()

// 执行消息获取操作

maybeFetch()

}

该方法是 [AbstractFetcherThread](http://abstractfetcherthread%20/) 类的核心方法，是线程运行的主要逻辑。AbstractFetcherThread 线程只要一直处于运行状态，就是会不断地重复这两个操作。

看到这里你是否会有疑惑：**该线程为何要不断尝试的去做截断操作呢？**

其实主要原因就是：**分区的 Leader 副本可能会随时发生变化**。每当有新 Leader 副本被选举产生时，Follower 副本就必须主动去执行截断操作，将自己的本地日志截断成与 Leader 副本一模一样，另外 Leader副本本身也需要执行截断操作，将 LEO 调整到分区高水位处。

先来看下截断操作。

## **2.4.4.1 maybeTruncate**

private def maybeTruncate(): Unit = {

// 1、将所有处于截断中状态的分区依据有无 Leader Epoch 值进行分组

val (partitionsWithEpochs, partitionsWithoutEpochs) = fetchTruncatingPartitions()

// 2、对于有 Leader Epoch 值的分区，将日志截断到 Leader Epoch 值对应的位移值处

if (partitionsWithEpochs.nonEmpty) {

truncateToEpochEndOffsets(partitionsWithEpochs)

}

// 3、对于没有 Leader Epoch 值的分区，将日志截断到高水位值处

if (partitionsWithoutEpochs.nonEmpty) {

truncateToHighWatermark(partitionsWithoutEpochs)

}

}

方法比较简单，步骤如下：

1.  首先是对分区状态进行分组。也就是说该方法操作的**只能是处于截断中状态的分区**。判断这些分区是否存在对应的 Leader Epoch 值，并按照有无 Epoch 值进行分组。
2.  对于 Leader Epoch 机制：**只它是用来替换 HW 值在日志截断中的作用**。典型的应用场景如下：
3.  当分区存在 Leader Epoch 值时，将副本的本地日志截断到 Leader Epoch 对应的最新位移值处，即方法truncateToEpochEndOffsets 将日志截断到 Leader Epoch 值对应的位移值处。
4.  当分区不存在对应的 Leader Epoch 值时，那么就会使用原来的高水位机制，即方法 truncateToHighWatermark 将日志调整到高水位值处。

接着分别来看下这 2 个方法。

// 根据指定 TopicPartition 的 PartitionData，获取每个分区的最新的 offset 信息，然后对所有分区的 Log 进行截断操作。

private def truncateToEpochEndOffsets(latestEpochsForPartitions: Map\[TopicPartition, EpochData\]): Unit = {

// 获取每个分区的最新 offset 信息

val endOffsets \= fetchEpochEndOffsets(latestEpochsForPartitions)

// 加锁，保证截断操作期间不会受到其他线程的干扰

inLock(partitionMapLock) {

//Check no leadership and no leader epoch changes happened whilst we were unlocked, fetching epochs

// 检查在获取 epoch 时，没有发生 Leader 更改

val epochEndOffsets \= endOffsets.filter { case (tp, \_) =>

val curPartitionState \= partitionStates.stateValue(tp)

val partitionEpochRequest \= latestEpochsForPartitions.getOrElse(tp, {

throw new IllegalStateException(

s"Leader replied with partition $tp not requested in OffsetsForLeaderEpoch request")

})

val leaderEpochInRequest \= partitionEpochRequest.currentLeaderEpoch

curPartitionState != null && leaderEpochInRequest == curPartitionState.currentLeaderEpoch

}

// 对指定 TopicPartition 进行截断操作

val ResultWithPartitions(fetchOffsets, partitionsWithError) = maybeTruncateToEpochEndOffsets(epochEndOffsets, latestEpochsForPartitions)

// 处理截断操作中出现的错误

handlePartitionsWithErrors(partitionsWithError, "truncateToEpochEndOffsets")

// 更新 fetchOffset 信息，并在可能的情况下标记截断操作已完成

updateFetchOffsetAndMaybeMarkTruncationComplete(fetchOffsets)

}

}

步骤如下：

1.  首先，获取每个分区的最新 offset 信息。
2.  加锁，保证截断操作期间不会受到其他线程的干扰。
3.  检查在获取 epoch 时，没有发生 Leader 更改。
4.  对指定 TopicPartition 进行截断操作。
5.  处理截断操作中出现的错误。
6.  更新 fetchOffset 信息，并在可能的情况下标记截断操作已完成。

来看下内部的 maybeTruncateToEpochEndOffsets 截断方法，看看它做了什么：

// 根据获取的各分区的最新 leader epoch 的 offset 信息，对指定分区进行截断操作。

private def maybeTruncateToEpochEndOffsets(

fetchedEpochs: Map\[TopicPartition, EpochEndOffset\], // 获取的各分区的最新 leader epoch 的 offset 信息

latestEpochsForPartitions: Map\[TopicPartition, EpochData\]): // 指定分区的最新 leader epoch 信息

ResultWithPartitions\[Map\[TopicPartition, OffsetTruncationState\]\] = { // 包含截断操作结果的 ResultWithPartitions 对象

// 保存要截断的分区的 offset 状态信息

val fetchOffsets \= mutable.HashMap.empty\[TopicPartition, OffsetTruncationState\]

// 保存出现错误的分区

val partitionsWithError \= mutable.HashSet.empty\[TopicPartition\]

// 遍历每个分区的最新 epoch offset 信息

fetchedEpochs.forKeyValue { (tp, leaderEpochOffset) =>

// 根据错误码进行处理

Errors.forCode(leaderEpochOffset.errorCode) match {

case Errors.NONE =>

// 获取截断的 offset 状态信息并加入到 fetchOffsets 中

val offsetTruncationState \= getOffsetTruncationState(tp, leaderEpochOffset)

if (doTruncate(tp, offsetTruncationState))

fetchOffsets.put(tp, offsetTruncationState)

// 如果分区已被标记为 fenced，调用 onPartitionFenced 处理

case Errors.FENCED\_LEADER\_EPOCH =>

// 获取分区最新的 leader epoch 信息

val currentLeaderEpoch \= latestEpochsForPartitions.get(tp)

.map(epochEndOffset => Int.box(epochEndOffset.currentLeaderEpoch)).asJava

if (onPartitionFenced(tp, currentLeaderEpoch))

partitionsWithError += tp

// 如果出现其他错误，记录日志并加入到 partitionsWithError 中

case error \=\>

info(s"Retrying leaderEpoch request for partition $tp as the leader reported an error: $error")

partitionsWithError += tp

}

}

// 返回结果

ResultWithPartitions(fetchOffsets, partitionsWithError)

}

该方法主要是**根据获取的各分区的最新 Leader Epoch 的 Offset 信息，对指定分区进行截断操作**，其步骤如下：

1.  初始化保存要截断的分区的 offset 状态信息和保存出现错误的分区。
2.  遍历每个分区的最新 epoch offset 信息，根据错误码进行处理：
3.  获取截断的 offset 状态信息，然后调用 doTruncate 方法执行真正的日志截断操作，加入到 fetchOffsets 中。
4.  如果分区已被标记为 fenced，调用 onPartitionFenced 处理。在 Kafka 中，FENCED\_LEADER\_EPOCH 是在处理 epoch 初始化时使用的错误码。在 Kafka 的实现中，每个分区都有一个 leader epoch。当 broker 上的该分区的 leader 副本被重新选举时，leader epoch 会自增，以确保新的 leader 副本的 leader epoch 大于之前 leader 副本的 leader epoch，避免出现数据不一致的情况。如果 broker 上已经存在一个更高的 leader epoch，那么该分区的 leader 副本将无法被选举或成为 leader 副本。在这种情况下，分区上的所有请求，包括读请求和写请求都会返回 FENCED\_LEADER\_EPOCH 错误码。处理该错误码时，需要根据最新的 leader epoch 信息重新尝试请求，确保分区数据一致性。
5.  如果出现其他错误，记录日志并加入到 partitionsWithError 中。
6.  最后返回结果。

该方法会调用 getOffsetTruncationState 方法来计算截断位置，并调用 doTruncate 方法来执行截断操作。

我们来看下 getOffsetTruncationState 方法是如何计算截断位置的。

private def getOffsetTruncationState(tp: TopicPartition,

leaderEpochOffset: EpochEndOffset): OffsetTruncationState = inLock(partitionMapLock) {

if (leaderEpochOffset.endOffset == UNDEFINED\_EPOCH\_OFFSET) {

....

OffsetTruncationState(partitionStates.stateValue(tp).fetchOffset, truncationCompleted = true)

} else if (leaderEpochOffset.leaderEpoch == UNDEFINED\_EPOCH) {

....

OffsetTruncationState(min(leaderEpochOffset.endOffset, logEndOffset(tp)), truncationCompleted = true)

} else {

// 获取 follower 副本的 logEndOffset，后面计算的截断位置必须小于或者等于该值

val replicaEndOffset \= logEndOffset(tp)

// 调用 endOffsetForEpoch 方法，在 follower 副本中找到小于或者等于 leader-lastEpoch 的最大 epoch 以及 epoch 结束偏移量，下面称为 follow-lastEpoch、follow-lastEpochEnd。

endOffsetForEpoch(tp, leaderEpochOffset.leaderEpoch) match {

case Some(OffsetAndEpoch(followerEndOffset, followerEpoch)) =>

if (followerEpoch != leaderEpochOffset.leaderEpoch) {

// 如果 follow-lastEpoch 小于 leader-lastEpoch，则截断到 follow-lastEpochEnd 的位置

val intermediateOffsetToTruncateTo \= min(followerEndOffset, replicaEndOffset)

....

OffsetTruncationState(intermediateOffsetToTruncateTo, truncationCompleted = false)

} else {

// 如果 follow-lastEpoch 等于 leader-lastEpoch，则截断到 follow-lastEpochEnd、leader-lastEpochEnd 较小值对应的位置。

val offsetToTruncateTo \= min(followerEndOffset, leaderEpochOffset.endOffset)

OffsetTruncationState(min(offsetToTruncateTo, replicaEndOffset), truncationCompleted = true)

}

case None \=\>

// 如果找不到 follow-lastEpoch，则截断到 leader-lastEpochEnd

OffsetTruncationState(min(leaderEpochOffset.endOffset, replicaEndOffset), truncationCompleted = true)

}

}

}

步骤如下：

1.  获取 follower 副本的 logEndOffset，后面计算的截断位置必须小于或者等于该值。
2.  调用 endOffsetForEpoch 方法，在 follower 副本中找到小于或者等于 leader-lastEpoch 的最大 epoch 以及 epoch 结束偏移量，下面称为 follow-lastEpoch、follow-lastEpochEnd。
3.  如果 follow-lastEpoch 小于 leader-lastEpoch，则截断到 follow-lastEpochEnd 的位置。
4.  如果 follow-lastEpoch 等于 leader-lastEpoch，则截断到 follow-lastEpochEnd、leader-lastEpochEnd 较小值对应的位置。
5.  如果找不到 follow-lastEpoch，则截断到 leader-lastEpochEnd。

接着来看下 truncateToHighWatermark 方法。

private\[server\] def truncateToHighWatermark(partitions: Set\[TopicPartition\]): Unit = inLock(partitionMapLock) {

val fetchOffsets \= mutable.HashMap.empty\[TopicPartition, OffsetTruncationState\]

// 遍历每个要执行截断操作的分区对象

for (tp <- partitions) {

// 获取分区的分区读取状态

val partitionState \= partitionStates.stateValue(tp)

if (partitionState != null) {

// 取出高水位值，分区的最大可读取位移值就是高水位值

val highWatermark \= partitionState.fetchOffset

// 告诉 Kafka 要把指定分区下副本截断到哪个位移值

val truncationState \= OffsetTruncationState(highWatermark, truncationCompleted = true)

info(s"Truncating partition $tp to local high watermark $highWatermark")

// 执行截断到高水位值

if (doTruncate(tp, truncationState))

fetchOffsets.put(tp, truncationState)

}

}

// 更新这组分区的分区读取状态

updateFetchOffsetAndMaybeMarkTruncationComplete(fetchOffsets)

}

该方法主要用来**将日志截断到 Leader Epoch 值对应的位移值处**，步骤如下：

1.  首先遍历每个要执行截断操作的分区对象。
2.  然后获取分区的分区读取状态，判断分区读取状态是否不为空，如果不为空：
3.  依次每个分区获取当前的高水位值，并将其保存在前面提到的分区读取状态类中。
4.  调用 doTruncate 方法执行真正的日志截断操作。
5.  等到将给定的所有分区都执行了对应的操作之后，更新这组分区的分区读取状态。

来看下最后一步，更新这组分区的分区读取状态，看看它做了什么：

private def updateFetchOffsetAndMaybeMarkTruncationComplete(fetchOffsets: Map\[TopicPartition, OffsetTruncationState\]): Unit = {

// 保存更新后的分区 fetch 状态信息

val newStates: Map\[TopicPartition, PartitionFetchState\] = partitionStates.partitionStateMap.asScala

.map { case (topicPartition, currentFetchState) =>

// 匹配是否截断成功

val maybeTruncationComplete \= fetchOffsets.get(topicPartition) match {

case Some(offsetTruncationState) =>

// 获取最新的 leader epoch

val lastFetchedEpoch \= latestEpoch(topicPartition)

val state \= if (isTruncationOnFetchSupported || offsetTruncationState.truncationCompleted)

// 如果支持截断操作或者截断操作已完成，设置状态为 Fetching

Fetching

else

// 否则，设置状态为 Truncating

Truncating

// 构建更新后的分区 fetch 状态

PartitionFetchState(offsetTruncationState.offset, currentFetchState.lag,

currentFetchState.currentLeaderEpoch, currentFetchState.delay, state, lastFetchedEpoch)

case None \=\> currentFetchState

}

(topicPartition, maybeTruncationComplete)

}

// 更新分区 fetch 状态信息

partitionStates.set(newStates.asJava)

}

该方法只有在 truncateToEpochEndOffsets 和 truncateToHighWatermark 方法中最后一步来执行，**它表示 truncate 之后，更新标记截断完成，然后更新 partitionStates 值，如果它们的 offsetTruncationState 截断完成，则将它们标记为截断已完成，表示可以读取了**。接着构建更新后的分区 fetch 状态，最后更新分区 fetch 状态信息。

## **2.4.4.2 maybeFetch**

private def maybeFetch(): Unit = {

val fetchRequestOpt \= inLock(partitionMapLock) {

// 为 partitionStates 中的分区构造 FetchRequest, partitionStates 中保存的是要去获取消息的分区以及对应的状态

val ResultWithPartitions(fetchRequestOpt, partitionsWithError) = buildFetch(partitionStates.partitionStateMap.asScala)

// 处理出错的分区，处理方式主要是将这个分区加入到有序 LinkedHashMap 末尾等待后续重试

handlePartitionsWithErrors(partitionsWithError, "maybeFetch")

// 如果当前没有可读取的分区，则等待 fetchBackOffMs 时间等候后续重试

if (fetchRequestOpt.isEmpty) {

trace(s"There are no active partitions. Back off for $fetchBackOffMs ms before sending a fetch request")

partitionMapCond.await(fetchBackOffMs, TimeUnit.MILLISECONDS)

}

fetchRequestOpt

}

// 发送 FETCH 请求给 Leader 副本，并处理 Response

fetchRequestOpt.foreach { case ReplicaFetch(sessionPartitions, fetchRequest) =>

processFetchRequest(sessionPartitions, fetchRequest)

}

}

步骤如下：

1.  为 partitionStates 中的分区构造 FetchRequest.Builder 对象, partitionStates 中保存的是要去获取消息的分区以及对应的状态。这一步返回的结果如下：
2.  一个对象是ReplicaFetch，即要读取的分区核心信息+ FetchRequest.Builder 对象。而这里的核心信息，就是指要读取哪个分区，从哪个位置开始读，最多读多少字节等等。
3.  另一个对象是 partitionsWithError：它是一组出错分区。
4.  处理这组出错分区。处理方式是将这组分区加入到有序 LinkedHashMap 末尾等待后续重试。
5.  如果发现当前没有任何可读取的分区，会阻塞等待 fetchBackOffMs 时间后续重试。
6.  发送 FETCH 请求给对应的 Leader 副本，并处理相应的 Response，也就是 processFetchRequest 方法要做的事情。

这里重点看下「**第二步**」、「**第四步**」的方法。

先来看 「**第二步**」方法。

private def handlePartitionsWithErrors(partitions: Iterable\[TopicPartition\], methodName: String): Unit = {

if (partitions.nonEmpty) {

debug(s"Handling errors in $methodName for partitions $partitions")

// 延迟重试处理分区

delayPartitions(partitions, fetchBackOffMs)

}

}

  
![](https://article-images.zsxq.com/FhNgUCUpXjkqRFdG444j8JOvcX0d)

接着来看 「**第四步**」方法。

// 发送 FETCH 请求给对应的 Leader 副本，并处理相应的 Response

private def processFetchRequest(sessionPartitions: util.Map\[TopicPartition, FetchRequest.PartitionData\],fetchRequest: FetchRequest.Builder): Unit = {

val partitionsWithError \= mutable.Set\[TopicPartition\]()

val divergingEndOffsets \= mutable.Map.empty\[TopicPartition, EpochEndOffset\]

var responseData: Map\[TopicPartition, FetchData\] = Map.empty

try {

trace(s"Sending fetch request $fetchRequest")

// 给 Leader 发送 FETCH 请求

responseData = fetchFromLeader(fetchRequest)

} catch {

....

}

// 更新请求发送速率指标

fetcherStats.requestRate.mark()

if (responseData.nonEmpty) {

// process fetched data

inLock(partitionMapLock) {

// 遍历 Fetch 结果

responseData.forKeyValue { (topicPartition, partitionData) =>

Option(partitionStates.stateValue(topicPartition)).foreach { currentFetchState =>

// 获取分区核心信息

val fetchPartitionData \= sessionPartitions.get(topicPartition)

// 处理 Response 的条件：

// 1. 要获取的位移值和之前已保存的下一条待获取位移值相等

// 2. 当前分区处于可获取状态

if (fetchPartitionData != null && fetchPartitionData.fetchOffset == currentFetchState.fetchOffset && currentFetchState.isReadyForFetch) {

// 匹配分区数据的错误信息

partitionData.error match {

// 如果没有错误

case Errors.NONE =>

try {

// 交由子类完成 Response 的处理

val logAppendInfoOpt \= processPartitionData(topicPartition, currentFetchState.fetchOffset, partitionData)

logAppendInfoOpt.foreach { logAppendInfo =>

val validBytes \= logAppendInfo.validBytes

val nextOffset \= if (validBytes > 0) logAppendInfo.lastOffset + 1 else currentFetchState.fetchOffset

val lag \= Math.max(0L, partitionData.highWatermark - nextOffset)

fetcherLagStats.getAndMaybePut(topicPartition).lag = lag

if (validBytes > 0 && partitionStates.contains(topicPartition)) {

val newFetchState \= PartitionFetchState(nextOffset, Some(lag),

currentFetchState.currentLeaderEpoch, state = Fetching,

logAppendInfo.lastLeaderEpoch)

// 将该分区放置在有序 LinkedHashMap 读取顺序的末尾，保证公平性

partitionStates.updateAndMoveToEnd(topicPartition, newFetchState)

fetcherStats.byteRate.mark(validBytes)

}

}

// 检查是否支持基于 FETCH 请求截断某些分区中的消息。在支持截断的情况下，检查 Follower 从分区中获取的 epoch 是否发生了变更，如果 epoch 发生了变更，那么 Follower 获取到的消息 Offset 就不是最新的了，所以需要对截断后的分区数据最新 Offset 进行更新。

// 如果是 Kafka 2.7 机以上版本，则需要获取 Leader 副本返回的 divergingEpochs

if (isTruncationOnFetchSupported) {

// 遍历 divergingEpochs 中的所有元素

partitionData.divergingEpoch.ifPresent { divergingEpoch =>

// 根据 divergingEpoch 的信息，构建出对应的 EpochEndOffset 结构体；将构建出的 EpochEndOffset 结果保存在 divergingEndOffsets 集合中。最终，divergingEndOffsets 集合保存了所有支持截断的分区的 Offset 信息。

divergingEndOffsets += topicPartition -> new EpochEndOffset()

.setPartition(topicPartition.partition)

.setErrorCode(Errors.NONE.code)

.setLeaderEpoch(divergingEpoch.epoch)

.setEndOffset(divergingEpoch.endOffset)

}

}

} catch {

....

}

// 如果读取位移值越界，通常是因为 Leader 发生变更

case Errors.OFFSET\_OUT\_OF\_RANGE =>

// 调整越界，主要办法是做截断

if (handleOutOfRangeError(topicPartition, currentFetchState, fetchPartitionData.currentLeaderEpoch))

// 如果依然不能成功，加入到出错分区列表

partitionsWithError += topicPartition

// 如果 Leader Epoch 值比 Leader 所在 Broker 上的 Epoch 值要新

case Errors.UNKNOWN\_LEADER\_EPOCH =>

debug(s"Remote broker has a smaller leader epoch for partition $topicPartition than " + s"this replica's current leader epoch of ${currentFetchState.currentLeaderEpoch}.")

// 加入到出错分区列表

partitionsWithError += topicPartition

// 如果 Leader Epoch 值比 Leader 所在 Broker 上的 Epoch 值要旧

case Errors.FENCED\_LEADER\_EPOCH =>

if (onPartitionFenced(topicPartition, fetchPartitionData.currentLeaderEpoch))

partitionsWithError += topicPartition

// 如果 Leader 发生变更

case Errors.NOT\_LEADER\_OR\_FOLLOWER =>

debug(s"Remote broker is not the leader for partition $topicPartition, which could indicate " + "that the partition is being moved")

// 加入到出错分区列表

partitionsWithError += topicPartition

// 如果主题、分区不识别

case Errors.UNKNOWN\_TOPIC\_OR\_PARTITION =>

warn(s"Received ${Errors.UNKNOWN\_TOPIC\_OR\_PARTITION} from the leader for partition $topicPartition. " + "This error may be returned transiently when the partition is being created or deleted, but it is not " + "expected to persist.")

// 加入到出错分区列表

partitionsWithError += topicPartition

case \_ \=\>

error(s"Error for partition $topicPartition at offset ${currentFetchState.fetchOffset}", partitionData.error.exception)

// 加入到出错分区列表

partitionsWithError += topicPartition

}

}

}

}

}

}

// 如果 divergingEndOffsets 集合中保存了支持截断操作的分区 Offset 信息，那么就调用 truncateOnFetchResponse 方法来执行相应的处理。

if (divergingEndOffsets.nonEmpty)

// 将遍历 divergingEndOffsets 集合中的所有分区 Offset 信息，然后执行对应的截断操作，以确保Follower 获取的分区数据与 broker 中的数据保持一致

truncateOnFetchResponse(divergingEndOffsets)

if (partitionsWithError.nonEmpty) {

// 处理出错分区列表

handlePartitionsWithErrors(partitionsWithError, "processFetchRequest")

}

}

该方法主要用来**发送 FETCH 请求给对应的 Leader 副本，并处理相应的 Response**，步骤如下：

1.  调用 fetchFromLeader 方法给 Leader 发送 FETCH 请求，并且这里会阻塞线程等待 Leader 副本返回 Response，然后更新 FETCH请求发送速率的监控指标。
2.  拿到 Response 之后从中取出分区的核心信息，然后比较要读取的位移值，和当前 AbstractFetcherThread 线程缓存的、该分区下一条待读取的位移值是否相等，以及当前分区是否处于可获取状态。
3.  如果不满足这两个条件，说明这个 Request 可能是一个之前等待了许久都未处理的请求，压根就不用处理了。
4.  如果满足这两个条件且 Response 没有错误，此时提取 Response 中的 Leader Epoch 值，然后交由子类 processPartitionData 实现具体的 Response 处理。将该分区放置在有序 LinkedHashMap 的末尾以保证公平性。
5.  如果该 Response 有错误，那么就调用对应错误的处理逻辑，然后将出错分区加入到出错分区列表中。
6.  如果是 Kafka 2.7 机以上版本，则需要获取 Leader 副本返回的 divergingEpochs。如果 Leader 副本返回了 divergingEpochs 属性，则 Follower 副本需要执行截断操作。
7.  调用 handlePartitionsWithErrors 方法，统一处理上一步处理过程中出现错误的分区。

这里通过一张图来梳理其流程：

![](https://article-images.zsxq.com/lrQ8WdHUJzoPgKGY3KUkxYzFjpvW)

## **02 拉取线程实现类 ReplicaFetcherThread**

上面说过了，[ReplicaFetcherThread](http://replicafetcherthread%20/) 类继承了 [AbstractFetcherThread](http://abstractfetcherthread%20/) 类。[ReplicaFetcherThread](http://replicafetcherthread%20/) 是 Follower副本创建的线程，用于向 Leader 副本拉取消息数据。

「**ReplicaFetcherThread**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/ReplicaFetcherThread.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/ReplicaFetcherThread.scala)

## **3.1 类定义以及重要属性**

先来看看其类定义以及重要属性，源码如下：

class ReplicaFetcherThread(name: String,

fetcherId: Int,

sourceBroker: BrokerEndPoint,

brokerConfig: KafkaConfig,

failedPartitions: FailedPartitions,

replicaMgr: ReplicaManager,

metrics: Metrics,

time: Time,

quota: ReplicaQuota,

leaderEndpointBlockingSend: Option\[BlockingSend\] = None)

extends AbstractFetcherThread(name = name,

clientId = name,

sourceBroker = sourceBroker,

failedPartitions,

fetchBackOffMs = brokerConfig.replicaFetchBackoffMs,

isInterruptible = false,

replicaMgr.brokerTopicStats) {

// 副本 Id 就是副本所在 Broker 的 Id

private val replicaId \= brokerConfig.brokerId

private val logContext \= new LogContext(s"\[ReplicaFetcher replicaId=$replicaId, leaderId=${sourceBroker.id}, " +

s"fetcherId=$fetcherId\] ")

this.logIdent = logContext.logPrefix

// 用于执行请求发送的类

private val leaderEndpoint \= leaderEndpointBlockingSend.getOrElse(

new ReplicaFetcherBlockingSend(sourceBroker, brokerConfig, metrics, time, fetcherId,

s"broker-$replicaId-fetcher-$fetcherId", logContext))

....

// Follower 发送的 FETCH 请求被处理返回前的最长等待时间

private val maxWait \= brokerConfig.replicaFetchWaitMaxMs

// 每个 FETCH Response 返回前必须要累积的最少字节数

private val minBytes \= brokerConfig.replicaFetchMinBytes

// 每个合法 FETCH Response 的最大字节数

private val maxBytes \= brokerConfig.replicaFetchResponseMaxBytes

// 单个分区能够获取到的最大字节数

private val fetchSize \= brokerConfig.replicaFetchMaxBytes

// 是否支持 leaderEpoch

override protected val isOffsetForLeaderEpochSupported: Boolean = brokerConfig.interBrokerProtocolVersion >= KAFKA\_0\_11\_0\_IV2

// 是否支持截断操作

override protected val isTruncationOnFetchSupported \= ApiVersion.isTruncationOnFetchSupported(brokerConfig.interBrokerProtocolVersion)

// 维持某个 Broker 连接上获取会话状态的类

val fetchSessionHandler \= new FetchSessionHandler(logContext, sourceBroker.id)

该类的定义中的大部分字段在上面我们已经剖析过了。现在我们只要剖析该类特有的几个字段：

1.  **fetcherId**：Follower 拉取的线程 Id。单台 Broker 上，允许存在多个 ReplicaFetcherThread 线程。Broker 端参数[num.replica.fetchers](http://num.replica.fetchers/)：决定了 Kafka 到底创建多少个 Follower 拉取线程。
2.  **brokerConfig**：KafkaConfig 类实例。它封装了 Broker 端所有的参数信息。ReplicaFetcherThread 类也是通过它来获取 Broker 端指定参数的值。
3.  **replicaMgr**：副本管理器。该线程类通过副本管理器来获取分区对象、副本对象以及它们下面的日志对象。
4.  **quota**：主要用做限流，是用作 Follower 副本拉取速度控制就行了。
5.  **leaderEndPointBlockingSend**：这是用于实现同步发送请求的类。所谓的同步发送，是指该线程使用它给指定 Broker 发送请求，然后线程处于阻塞状态，直到接收到 Broker 返回的 Response。

另外还定义了与消息获取息息相关的 4 个字段：

1.  **maxWait**：Follower 发送的 FETCH 请求被处理返回前的最长等待时间。它是 Broker 端参数 [replica.fetch.wait.max.ms](http://replica.fetch.wait.max.ms/) 的值。
2.  **minBytes**：每个 FETCH Response 返回前必须要累积的最少字节数。它是Broker端参数 [replica.fetch.min.bytes](http://replica.fetch.min.bytes/) 的值。
3.  **maxBytes**：每个合法 FETCH Response 的最大字节数。它是 Broker 端参数 [replica.fetch.response.max.bytes](http://replica.fetch.response.max.bytes/) 的值。
4.  **fetchSize**：单个分区能够获取到的最大字节数。它是 Broker 端参数 [replica.fetch.max.bytes](http://replica.fetch.max.bytes/) 的值。

上面这 4 个参数都是 FETCH 请求的参数，主要控制了 Follower 副本拉取 Leader 副本消息的行为，比如一次请求到底能够获取多少字节的数据，或者当未达到累积阈值时，FETCH 请求等待多长时间等。

## **3.2 重要方法**

我们接着剖析该类的重要方法：processPartitionData、buildFetch、truncate。主要是它们代表了 Follower 副本拉取线程要做的最重要的三件事：「**处理拉取的消息**」、「**构建拉取消息的请求**」、「**执行截断日志操作**」。

接下来我们挨个看下，先来剖析下 「**处理拉取的消息**」的方法。

## **3.2.1 processPartitionData**

在上面将基类源码时，AbstractFetcherThread 线程从 Leader 副本拉取回消息后，需要调用该方法进行后续操作，源码如下：

override def processPartitionData(topicPartition: TopicPartition,

fetchOffset: Long, partitionData: FetchData): Option\[LogAppendInfo\] = {

val logTrace \= isTraceEnabled

// 从副本管理器获取指定主题分区对象

val partition \= replicaMgr.getPartitionOrException(topicPartition)

// 获取日志对象

val log \= partition.localLogOrException

// 将获取到的数据转换成符合格式要求的消息集合

val records \= toMemoryRecords(partitionData.records)

// 校验消息格式

maybeWarnIfOversizedRecords(records, topicPartition)

// 如果要读取的起始位移值不是本地日志 LEO 值则认为是异常情况

if (fetchOffset != log.logEndOffset)

throw new IllegalStateException("Offset mismatch for partition %s: fetched offset = %d, log end offset = %d.".format(topicPartition, fetchOffset, log.logEndOffset))

....

// 写入 Follower 副本本地日志

val logAppendInfo \= partition.appendRecordsToFollowerOrFutureReplica(records, isFuture = false)

....

// 获取 Leader LSO 值

val leaderLogStartOffset \= partitionData.logStartOffset

// 尝试更新 Follower 副本的高水位值

val followerHighWatermark \= log.updateHighWatermark(partitionData.highWatermark)

// 尝试更新 Follower 副本的日志起始位移值

log.maybeIncrementLogStartOffset(leaderLogStartOffset, LeaderOffsetIncremented)

....

// 副本消息拉取限流

if (quota.isThrottled(topicPartition))

quota.record(records.sizeInBytes)

// 更新统计指标值

if (partition.isReassigning && partition.isAddingLocalReplica)

brokerTopicStats.updateReassignmentBytesIn(records.sizeInBytes)

brokerTopicStats.updateReplicationBytesIn(records.sizeInBytes)

// 返回日志写入结果

logAppendInfo

}

这里通过一张图来梳理其流程：

![](https://article-images.zsxq.com/lhWUkOfu6Zidz6QLvw_Fbktdgok1)

该方法主要用来**第处理从 Leader 副本获取到的消息，然后写入本地日志中**，步骤如下：

1.  从副本管理器获取指定主题分区对象、日志对象。
2.  将获取到的数据转换成符合格式要求的消息集合。
3.  如果要读取的起始位移值不是本地日志 LEO 值则认为是异常情况，此时会抛异常。
4.  否则，写入消息集合到 Follower 副本本地日志中。
5.  尝试更新 Follower 副本的高水位值「**将 FETCH 请求 Response 中包含的高水位作为新的高水位**」、日志起始位移值。那为什么 Log Start Offset 值也可能发生变化呢？这是因为 **Leader 的 LogStartOffset 可能发生变化，比如用户手动删除消息的操作等**，Follower 副本的日志需要和 Leader 副本保持严格的一致。
6.  副本消息拉取限流、更新统计指标值。
7.  返回日志写入结果。

我们来看下「**第四步**」：

![](https://article-images.zsxq.com/FuniPkYdncXYmmRHynOijZMY3s4X)

![](https://article-images.zsxq.com/FhFaanfyp400tj86-cSWfTDiYSqE)

## **3.2.2 buildFetch**

接着，我们来剖析「**构建拉取消息的请求**」的方法。

override def buildFetch(partitionMap: Map\[TopicPartition, PartitionFetchState\]): ResultWithPartitions\[Option\[ReplicaFetch\]\] = {

// 出现错误的分区列表

val partitionsWithError \= mutable.Set\[TopicPartition\]()

// 构建 fetchSessionHandler Builder 对象

val builder \= fetchSessionHandler.newBuilder(partitionMap.size, false)

// 遍历每个分区

partitionMap.forKeyValue { (topicPartition, fetchState) =>

// We will not include a replica in the fetch request if it should be throttled.

// 分区可读取 && 未被限流

if (fetchState.isReadyForFetch && !shouldFollowerThrottle(quota, fetchState, topicPartition)) {

try {

// 获取日志起始位移值

val logStartOffset \= this.logStartOffset(topicPartition)

val lastFetchedEpoch \= if (isTruncationOnFetchSupported)

fetchState.lastFetchedEpoch.map(\_.asInstanceOf\[Integer\]).asJava

else

Optional.empty\[Integer\]

// 构建读取对象 PartitionData 添加到 builder 后续统一处理

builder.add(topicPartition, new FetchRequest.PartitionData(

fetchState.fetchOffset,

logStartOffset,

fetchSize,

Optional.of(fetchState.currentLeaderEpoch),

lastFetchedEpoch))

} catch {

case \_: KafkaStorageException =>

// 对于有错误的分区加入到出错分区列表

partitionsWithError += topicPartition

}

}

}

// 生成 fetchRequestData

val fetchData \= builder.build()

// 判断 Builder 有无可读取的分区

val fetchRequestOpt \= if (fetchData.sessionPartitions.isEmpty && fetchData.toForget.isEmpty) {

None

} else {

// 构造 FETCH 请求的 Builder 对象

val requestBuilder \= FetchRequest.Builder

.forReplica(fetchRequestVersion, replicaId, maxWait, minBytes, fetchData.toSend)

.setMaxBytes(maxBytes)

.toForget(fetchData.toForget)

.metadata(fetchData.metadata)

Some(ReplicaFetch(fetchData.sessionPartitions(), requestBuilder))

}

// 返回 Builder 对象以及出错分区列表

ResultWithPartitions(fetchRequestOpt, partitionsWithError)

}

该方法主要用来**为一组特定分区构造 FETCH 请求的 Builder 对象然后返回**，步骤如下：

1.  初始化出现错误的分区列表，构建 fetchSessionHandler Builder 对象。
2.  遍历每个分区，判断分区可读取 && 未被限流，如果满足：
3.  获取日志起始位移值， 构建读取对象 PartitionData 添加到 builder 后续统一处理，如果失败则对于有错误的分区加入到出错分区列表。
4.  生成 fetchRequestData 。
5.  判断 Builder 有无可读取的分区，如果无，返回 None。否则构造 FETCH 请求的 Builder 对象。
6.  返回 Builder 对象以及出错分区列表。

最后我们来剖析下最后一个方法，「**执行截断日志操作**」的方法。

## **3.2.3 truncate**

override def truncate(tp: TopicPartition, offsetTruncationState: OffsetTruncationState): Unit = {

// 获取分区对象

val partition \= replicaMgr.getPartitionOrException(tp)

// 获取分区本地日志

val log \= partition.localLogOrException

// 执行截断操作，截断到的位置由 offsetTruncationState 的 offset 来指定

partition.truncateTo(offsetTruncationState.offset, isFuture = false)

if (offsetTruncationState.offset < log.highWatermark)

warn(s"Truncating $tp to offset ${offsetTruncationState.offset} below high watermark " +

s"${log.highWatermark}")

// mark the future replica for truncation only when we do last truncation

if (offsetTruncationState.truncationCompleted)

replicaMgr.replicaAlterLogDirsManager.markPartitionsForTruncation(brokerConfig.brokerId, tp,

offsetTruncationState.offset)

}

该方法主要目的是**根据 Leader 副本返回的位移值和 Epoch 值执行本地日志截断操作**。总体来说，该方法利用给定的 [offsetTruncationState](http://offsettruncationstate%20/) 的 offset值，对给定分区的本地日志进行截断操作。该操作由 [Partition#truncateTo](http://partition/#truncateTo) 方法完成，但实际上底层调用的是 [Log#truncateTo](http://log/#truncateTo) 方法。而 truncateTo 方法的主要作用：是将日志截断到小于给定值的最大位移值处。

##   
**04 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头从「**KafkaController**」启动会选举「**Leader 副本**」，Follower 副本会同步消息，文章开头提出了两个：**副本之间是如何同步消息的呢？在同步的过程中又是如何保证副本之间数据一致性的呢？**本文主要解决第一个问题。

3、接着带大家深度剖析了「**拉取线程基类**」的源码总览以及重要方法，doWork 方法把 3 个重要方法全部连接在一起，共同完整了拉取线程要执行的逻辑，即日志截断（truncate）+日志获取（buildFetch）+日志处理（processPartitionData）。

4、最后带大家深度剖析了「**拉取线程实现子类**」的源码总览以及重要方法，总结：Follower 副本利用ReplicaFetcherThread 线程实时地从 Leader 副本拉取消息并写入到本地日志，从而实现了与 Leader 副本之间的同步。

最后通过一张图来说明 「**Leader 副本**」、「**Follower 副本**」同步流程图：

  
![](https://article-images.zsxq.com/lpc_a8I6R2WdT19bg5m8AgnUuOoR)

下篇我们来深度剖析「**Leader 副本更新 ISR、HW 流程**」，大家期待，我们下期见。