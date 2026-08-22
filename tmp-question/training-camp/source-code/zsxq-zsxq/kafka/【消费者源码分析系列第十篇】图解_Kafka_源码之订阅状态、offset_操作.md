大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了「**消费者重平衡机制流程**」，了解了消费者组重平衡机制是如何实现的。包括「**发生条件**」、「**发生场景**」、「**源码流程分析**」等等。今天我们开启消费端源码的征程，这是第十篇来深度聊聊「**订阅状态、offset 操作**」，看看 Kafka 消费者订阅状态以及 Offset 是如何设计和操作的。

![](https://article-images.zsxq.com/FuuHli1_E8PFEn-0A6Ubn_i16azF)

## **01 总体概述**

在上一篇中我们剖析了消费者组重平衡机制，即「**消费者加入消费者组 JoinGroup**」以及 「**分区分配方案 SyncGroup**」的源码。那么**我们知道了消费者的分区是不是就能直接消费了呢？**其实不是的，此时我们还不知道消费者的「**订阅状态信息**」，比如：消费者订阅的主题是否发生变化，具体消费到分区的哪个 offset 了等等，这些订阅信息我们需要进行管理。

同时在「**消费者开始消费过程中**」和「**Rebalance 操作之前**」都要提交一次 offset。在消费者刚开始消费某个 Partition 的时候也需要获取分区的 offset，这就涉及到了 offset 的提交和获取操作。

那么接下来我们就来深度剖析下消费者者订阅状态以及消费者是如何提交和获取 offset 的。

本文涉及的源码：

「**SubscriptionState**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/SubscriptionState.java](https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/SubscriptionState.java)

「**ConsumerCoordinator**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java](https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java)

## **02 消费者订阅状态**

在 Kafka 消费者中是使用 [SubscriptionState.java](http://subscriptionstate.java/) 类来「**保存消费者订阅的主题**」，并「**跟踪 TopicPartition 与 offset 对应关系**」。

接下来，我们先来剖析一下 [SubscriptionState.java](http://subscriptionstate.java/) 类的初始化入口、类定义和重要方法。

##   
**2.1 消费者订阅状态初始化入口**

在消费者第一篇 [【消费者源码分析系列第一篇】图解 Kafka 源码之消费者初始化流程](https://articles.zsxq.com/id_3g80nohn4g6s.html) 中，我们了解到在 consumer 启动时会初始化 [SubscriptionState](http://subscriptionstate.java/) ，如下：

KafkaConsumer(ConsumerConfig config, Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer) {

try {

....

LogContext logContext;

OffsetResetStrategy offsetResetStrategy = OffsetResetStrategy.valueOf(config.getString(ConsumerConfig.AUTO\_OFFSET\_RESET\_CONFIG).toUpperCase(Locale.ROOT));

this.subscriptions = new SubscriptionState(logContext, offsetResetStrategy);

....

} catch (Throwable t) {

....

}

}

## **2.2 消费者订阅状态类定义**

public class SubscriptionState {

private static final String SUBSCRIPTION\_EXCEPTION\_MESSAGE \=

"Subscription to topics, partitions and pattern are mutually exclusive";

private final Logger log;

// 表示订阅 Topic 的模式，分以下四类：

private enum SubscriptionType {

NONE, //初始值。

AUTO\_TOPICS, //按指定的 Topic 进行订阅，自动分配分区。

AUTO\_PATTERN, //按正则表达式匹配的 topic 进行订阅，自动分配分区。

USER\_ASSIGNED //用户自己定制消费者要消费的 topic 和分区。

}

/\* the type of subscription \*/

private SubscriptionType subscriptionType;

/\* 用来过滤 topic 的正则表达式 \*/

private Pattern subscribedPattern;

/\* 用户手动写的要订阅的 topic \*/

private Set<String> subscription;

/\* 消费组订阅的所有 topic \*/

private Set<String> groupSubscription;

/\* 记录消费者里主题分区的状态集合 \*/

private final PartitionStates<TopicPartitionState> assignment;

/\* Default offset reset strategy \*/

private final OffsetResetStrategy defaultResetStrategy;

/\* User-provided listener to be invoked when assignment changes \*/

private ConsumerRebalanceListener rebalanceListener;

private int assignmentId \= 0;

....

// 构造方法

public SubscriptionState(LogContext logContext, OffsetResetStrategy defaultResetStrategy) {

this.log = logContext.logger(this.getClass());

this.defaultResetStrategy = defaultResetStrategy;

this.subscription = new HashSet<>();

this.assignment = new PartitionStates<>();

this.groupSubscription = new HashSet<>();

this.subscribedPattern = null;

// 默认设置订阅模式为 NONE。

this.subscriptionType = SubscriptionType.NONE;

}

....

}

这里我们来重点剖析下这些重要字段：

1.  SubscriptionType：它是枚举类。表示订阅 Topic 的模式，有以下四类。
2.  NONE：初始值。
3.  AUTO\_TOPICS：按指定的 Topic 进行订阅，自动分配分区。
4.  AUTO\_PATTERN：按正则表达式匹配的 topic 进行订阅，自动分配分区。
5.  USER\_ASSIGNED：用户自己指定消费者要消费的 topic 和分区。
6.  subscribedPattern：用来过滤主题的正则表达式，符合这个正则表达式的主题都会成为订阅主题。
7.  subscription：Set 集合，用户手动指定的订阅主题。
8.  groupSubscription：消费组订阅的所有 topic。当这个消费者被服务端 [groupCoordinator](http://groupcoordinator/) 选为 [leader consumer](http://leader%20consumer/) 的时候，这个字段是消费组所有消费者订阅的主题，用来监控消费组主题相关的元数据的变化，以满足消费组重平衡时为消费组制定分区方案的需要。而如果是 [follower consumer](http://follower%20consumer/)，由于不涉及为消费组全体消费者指定分区方案，这个字段只会保存本消费者订阅的主题。
9.  assignment：记录消费者里主题分区的状态集合。集合元素是 [TopicPartitionState](http://topicpartitionstate/)，记录着每一个主题分区的消费情况，包括消费到分区的哪个位置了。
10.  defaultResetStrategy：默认重置策略。所谓重置策略就是当消费者重启的时候会使用什么策略进行消费，消费策略有两种：
11.  [LATEST](http://latest/)：从分区最后位置消费。
12.  [EARLIEST](http://earliest/)：从分区最一开始的位置消费。
13.  rebalanceListener：[ConsumerRebalanceListener](http://consumerrebalancelistener/) 类的对象，用来监听重平衡后消费者要消费分区的变化。

## **2.3 消费者订阅状态重要方法**

该类的方法都比较简单，这里我就挑选几个方法剖析一下。

##   
**2.3.1 subscribe()**

该方法主要用来让用户指定要订阅的主题，源码如下：

public synchronized boolean subscribe(Set<String> topics, ConsumerRebalanceListener listener) {

// 注册重平衡监听器

registerRebalanceListener(listener);

// 设置订阅 Topic 模式， 这里设置的是 AUTO\_TOPICS，表示按指定的 Topic 进行订阅，自动分配分区。

setSubscriptionType(SubscriptionType.AUTO\_TOPICS);

// 如果订阅的主题和以前订阅的一致，就不需要修改订阅信息。如果不一致，就需要修改

return changeSubscription(topics);

}

该方法比较简单，核心步骤如下：

1.  注册重平衡监听器。
2.  设置订阅 Topic 模式， 这里设置的是 AUTO\_TOPICS，表示按指定的 Topic 进行订阅，自动分配分区。
3.  如果订阅的主题和以前订阅的一致，就不需要修改订阅信息。如果不一致，就需要修改。

我们再来来看下每一步的方法。

## **2.3.2 registerRebalanceListener()**

该方法用来注册重平衡监听器，源码如下：

private void registerRebalanceListener(ConsumerRebalanceListener listener) {

if (listener == null)

throw new IllegalArgumentException("RebalanceListener cannot be null");

// 注册重平衡监听器

this.rebalanceListener = listener;

}

## **2.3.3 setSubscriptionType()**

该方法用来设置订阅 Topic 模式，源码如下：

private void setSubscriptionType(SubscriptionType type) {

if (this.subscriptionType == SubscriptionType.NONE)

// 按照设置的主题开始订阅，自动分配分区

this.subscriptionType = type;

else if (this.subscriptionType != type)

// 否则抛异常

throw new IllegalStateException(SUBSCRIPTION\_EXCEPTION\_MESSAGE);

}

## **2.3.4 changeSubscription()**

该方法用来修改订阅信息，源码如下：

private boolean changeSubscription(Set<String> topicsToSubscribe) {

// 如果订阅的主题和以前订阅的一致，就不需要修改订阅信息。

if (subscription.equals(topicsToSubscribe))

return false;

// 如果不一致，就需要修改

subscription = topicsToSubscribe;

return true;

}

## **2.4 ACK 管理**

在 Kafka 中，每个消息都存在一个偏移量。

1.  在消费者中，[ConsumerCoordinator.subscriptions.assginment.position](http://consumercoordinator.subscriptions.assginment.position/) 中存储了每个分区的 ACK 偏移量，该偏移量有以下 2 个作用：
2.  表示该位置之前的消息都已经消费成功了。我们知道，消费者会定时将 ACK 偏移量提交给 Broker，或者由消费者程序调用 [Consumer#commitSync](http://consumer/#commitSync) 方法来提交 ACK 偏移量。
3.  可以作为消息读取位置，当下一次消费者拉取数据时，会从该位置开始读取消息。
4.  协调者 Coordinator 中存储了每个消费者组下每个分区的 ACK 偏移量。当分区的消费者变更后，新的消费者将从该 ACK 偏移量位置开始消费。协调者将 ACK 偏移量保存在以下两个位置：
5.  消费者组元数据 GroupMetadata#offsets 属性，该属性时一个 Map 实例，存储了每个分区及该分区已提交的 ACK 偏移量。
6.  内部主题 「**\_\_consumer\_offsets**」，以「**groupid-topic-parititon -> offset**」为维度进行存储。

比如消费者订阅了主题 message4，并且该主题分区 0 当前的 ACK 偏移量为 20， 则说明偏移量 20 之前的消息都已经成功消费，而消费者下次发送 Fetch 请求，会将读取偏移量 offset 设置为 20，则会要求 Broker 端从该偏移量开始读取消息。

## **2.4.1 消费者初始化偏移量**

这里我们来分析下消费者中如何初始化 ACK 偏移量。

首先 [KafkaConsumer#updateFetchPositions](http://kafkaconsumer/#updateFetchPositions) 方法来初始化 ACK 偏移量。

// 初始化 ACK 偏移量

private boolean updateFetchPositions(final Timer timer) {

// 如果有任何分区由于leader更改而被截断，则需要验证它们的offset是否有效

fetcher.validateOffsetsIfNeeded();

// 如果所有的分区都已经有了有效的位置，则返回true

cachedSubscriptionHashAllFetchPositions = subscriptions.hasAllFetchPositions();

if (cachedSubscriptionHashAllFetchPositions)

return true;

// 如果存在没有有效位置且不在等待重置状态的分区，则需要拉取已提交的offsets

// 如果已分配的分区没有初始位置，则始终需要进行协调者查找以确保指定的分区有初始位置

if (coordinator != null && !coordinator.refreshCommittedOffsetsIfNeeded(timer))

return false;

// 如果有分区仍然需要位置并且已定义重置策略，则使用默认策略请求重置

// 如果没有定义重置策略并且存在没有有效位置的分区，则会抛出异常

subscriptions.resetInitializingPositions();

// 最后，异步请求查找和更新任何等待重置的分区的位置

fetcher.resetOffsetsIfNeeded();

return true;

}

## **03 消费者提交与获取 Offset**

在前面剖析消费者组的重平衡的时候，我们提到在重平衡消费者组之前，消费者要向从属的 [GroupCoordinator](http://groupcoordinator/) 提交 offset 来记录当前消费的位置。

其实不仅仅在重平衡场景要提交 offset，更重要的是消费者组在正常消费的过程中也是要向服务端提交 offset 的，因为如果消费者挂掉了，别的消费者要根据分区现在的 offset 来继续消费。

## **3.1 消费者提交 offset**

在剖析方法之前，我们先来剖析一下位移提交的两个重要的数据结构：「**OffsetCommitRequest**」、「**OffsetCommitResponse**」的消息体格式。

##   
**3.1.1 OffsetCommitRequest 消息体**

![](https://article-images.zsxq.com/lpWTG9_1ebSbBbe-bFHhm1BIyoy-)

这里剖析下各字段的含义：

1.  group\_id：表示 [ConsumerGroup](http://consumergroup/) 的 Id。
2.  group\_generation\_id：表示消费者保存的年代信息。
3.  member\_id：表示 [GroupCoordinator](http://groupcoordinator/) 分配给消费者的 Id。
4.  retention\_time：表示此 offset 的最长保存时间。
5.  topic：表示 topic 名称。
6.  partition：表示分区编号。
7.  offset：表示提交的信息 offset。
8.  metadata：表示任何希望与 offset 一起保存的自定义数据。

## **3.1.2 OffsetCommitResponse 消息体**

![](https://article-images.zsxq.com/FpvGYl8_tX656P5wuK1EkL97_S48)

这里剖析下各字段的含义：

1.  topic：表示 topic 名称。
2.  partition：表示分区编号。
3.  error\_code：表示错误码。

## **3.1.3 提交 Offset**

剖析完位移提交的请求和响应数据结构之后，我们来剖析下提交 Offset 的相关方法，这里主要包括两个：

1.  **异步提交**：指业务线程提交 offset 的时候不用等待服务端发来的响应，业务线程可以继续做接下来的工作，而响应结果由回调对象处理。其好处是：由于线程没有阻塞，提升线程的并发度有所提升。但是如果线程后面的逻辑对于消费者是否成功消费敏感就不适用了。
2.  **同步提交**：与异步提交相反，业务线程必须等待 offset 提交完才能往下执行。这种场景会造成线程的阻塞，影响线程的并发性，但是适用于线程后面的逻辑对消费者是否成功消费敏感。

本篇只剖析「**异步提交**」的相关方法，关于「**异步提交**」的逻辑差不多，自行研究，如果有疑问可以评论区沟通。

首先我们先来看下「**自动提交 offset**」的入口方法。

##   
**3.1.4 maybeAutoCommitOffsetAsync()**

public void maybeAutoCommitOffsetsAsync(long now) {

// 是否开启了偏移量

if (autoCommitEnabled) {

// 记录当前提交偏移量的时间，目的是为了计时。

nextAutoCommitTimer.update(now);

// 超时判断，其超时时间是 autoCommitIntervalMs。

if (nextAutoCommitTimer.isExpired()) {

// 重置下次过期时间

nextAutoCommitTimer.reset(autoCommitIntervalMs);

// 处理异步提交

doAutoCommitOffsetsAsync();

}

}

}

核心步骤如下：

1.  首先根据参数 [autoCommitEnabled](http://autocommitenabled/) 判断是否是自动提交。这里 [autoCommitEnabled](http://autocommitenabled/) 对应配置文件的参数是[auto.commit.interval.ms](http://auto.commit.interval.ms/)，默认是 5000 ms。
2.  如果是自动提交就更新提交偏移量的时间，然后判断当定时器过时的时候重置下次提交 offset 的时间。
3.  最后调用方法 [doAutoCommitOffsetsAsync()](http://doautocommitoffsetsasync\(\)/) 处理提交 offset 操作。

最重要的就是最后一步，我们来看下。

## **3.1.5 doAutoCommitOffsetsAsync()**

private void doAutoCommitOffsetsAsync() {

// 获取当前已消费的所有偏移量 allConsumedOffsets。

Map<TopicPartition, OffsetAndMetadata> allConsumedOffsets = subscriptions.allConsumed();

log.debug("Sending asynchronous auto-commit of offsets {}", allConsumedOffsets);

// 调用 commitOffsetsAsync 方法进行异步地提交偏移量，并传入回调函数。

commitOffsetsAsync(allConsumedOffsets, (offsets, exception) -> {

if (exception != null) {

// 如果出现异常，则根据异常类型执行相应的处理。如果是可重试的提交失败异常，记录日志，并根据配置的重试间隔时间更新并重置下一次自动提交的定时器。否则，记录警告日志。

if (exception instanceof RetriableCommitFailedException) {

log.debug("Asynchronous auto-commit of offsets {} failed due to retriable error: {}", offsets, exception);

nextAutoCommitTimer.updateAndReset(rebalanceConfig.retryBackoffMs);

} else {

log.warn("Asynchronous auto-commit of offsets {} failed: {}", offsets, exception.getMessage());

}

} else {

// 如果没有异常，记录日志，指示已完成异步自动提交的偏移量。

log.debug("Completed asynchronous auto-commit of offsets {}", offsets);

}

});

}

  
该方法的目的是「**定期异步提交消费者偏移量**」。在 Kafka 消费者中，可以选择启用自动偏移量提交，并通过设置自动提交的时间间隔来控制偏移量的提交频率。异步提交偏移量可以提高消费者的性能，避免阻塞消费者线程。在偏移量提交失败的情况下，采取不同的处理策略以确保偏移量已经得到提交，并进行重试。

##   
**3.1.6 commitOffsetsAsync()**

public void commitOffsetsAsync(final Map<TopicPartition, OffsetAndMetadata> offsets, final OffsetCommitCallback callback) {

invokeCompletedOffsetCommitCallbacks();

// 检查协调器是否已知，如果已知则执行偏移量提交

if (!coordinatorUnknown()) {

doCommitOffsetsAsync(offsets, callback);

} else {

// 如果协调器未知，则增加待处理的异步提交偏移量计数器，并在协调器查找完成后执行偏移量提交

pendingAsyncCommits.incrementAndGet();

// 添加一个监听器到查找协调器的请求中，等待协调器查找完成的回调。

lookupCoordinator().addListener(new RequestFutureListener<Void>() {

@Override

// 监听成功回调

public void onSuccess(Void value) {

// 减少待处理的异步提交偏移量计数器

pendingAsyncCommits.decrementAndGet();

// 执行偏移量提交

doCommitOffsetsAsync(offsets, callback);

// 执行网络 I/O 操作

client.pollNoWakeup();

}

@Override

// 监听失败回调

public void onFailure(RuntimeException e) {

// 减少待处理的异步提交偏移量计数器

pendingAsyncCommits.decrementAndGet();

// 将偏移量提交异常信息添加到已完成偏移量提交的队列中

completedOffsetCommits.add(new OffsetCommitCompletion(callback, offsets,

new RetriableCommitFailedException(e)));

}

});

}

// 执行网络 I/O 操作，发送请求并接收响应

client.pollNoWakeup();

}

该方法用来「**发送异步提交请求给协调器**」。根据协调器的情况，可能会进行协调器查找和重新分配操作。一旦协调器已知，就会执行偏移量的提交，并执行相应的回调函数。如果协调器未知，则会先进行协调器的查找，并在查找完成后再执行偏移量的提交。这样可以确保偏移量提交的可靠性，并处理提交失败的情况。通过异步提交偏移量，可以提高消费者的性能，避免阻塞消费者线程。

接下来，我们来看下最关键的方法，处理异步提交位移操作方法。

##   
**3.1.6 doCommitOffsetsAsync()**

private void doCommitOffsetsAsync(final Map<TopicPartition, OffsetAndMetadata> offsets, final OffsetCommitCallback callback) {

// 1、创建并缓存 OffsetCommiteRequest 请求

RequestFuture<Void> future = sendOffsetCommitRequest(offsets);

// 2、获取回调对象

final OffsetCommitCallback cb \= callback == null ? defaultOffsetCommitCallback : callback;

// 3、给异步请求加监听器

future.addListener(new RequestFutureListener<Void>() {

@Override

public void onSuccess(Void value) {

// 执行拦截器的偏移量提交回调

if (interceptors != null)

interceptors.onCommit(offsets);

// 把完成的 offset 提交加到完成队列中

completedOffsetCommits.add(new OffsetCommitCompletion(cb, offsets, null));

}

@Override

public void onFailure(RuntimeException e) {

Exception commitException \= e;

// 如果失败异常是可重试异常，则将异常封装成可重试的提交失败异常

if (e instanceof RetriableException) {

commitException = new RetriableCommitFailedException(e);

}

// 将已完成的偏移量提交添加到完成队列中，并传入提交失败的异常

completedOffsetCommits.add(new OffsetCommitCompletion(cb, offsets, commitException));

// 如果发生了 FencedInstanceIdException 异常，表明发生了实例失效，设置异步提交失败标志

if (commitException instanceof FencedInstanceIdException) {

asyncCommitFenced.set(true);

}

}

});

}

该方法用来「**处理具体的异步偏移量提交操作**」，核心步骤如下：

1.  调用方法 [sendOffsetCommitRequest()](http://sendoffsetcommitrequest\(\)/) 创建 [OffsetCommiteRequest](http://offsetcommiterequest/) 请求，并把请求缓存到 [ConsumerNetworkClient #unsent](http://consumernetworkclient%20/#unsent) 集合里。
2.  获取完成提交 offset 的回调对象。
3.  给异步请求加监听器，值得注意的是，这里的监听器对应成功的响应的处理是把回调方法放到一个队列里让上层逻辑调用。

## **3.1.7 sendOffsetCommitRequest()**

RequestFuture<Void> sendOffsetCommitRequest(final Map<TopicPartition, OffsetAndMetadata> offsets) {

if (offsets.isEmpty())

return RequestFuture.voidSuccess();

// 1、获取 GroupCoordinator

Node coordinator \= checkAndGetCoordinator();

if (coordinator == null)

return RequestFuture.coordinatorNotAvailable();

// 2、开始创建 offset 提交的请求。创建要发送的 key 为主题，value 为这个主题下的分区要提交offset 信息的 map 集合。

Map<String, OffsetCommitRequestData.OffsetCommitRequestTopic> requestTopicDataMap = new HashMap<>();

for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : offsets.entrySet()) {

....

// 3、填充集合

requestTopicDataMap.put(topicPartition.topic(), topic);

}

final Generation generation;

// 4、如果是自动分配分区方案

if (subscriptions.hasAutoAssignedPartitions()) {

generation = generationIfStable();

// 如果 generation 为空，就认为这个消费者没有加入一个激活的消费组。

// 要做的事情是提交 offset 失败，然后让消费者重新加入组。

if (generation == null) {

log.info("Failing OffsetCommit request since the consumer is not part of an active group");

if (rebalanceInProgress()) {

// 如果消费者在重平衡的过程中，那么就返回正在重平衡的异常

return RequestFuture.failure(new RebalanceInProgressException("Offset commit cannot be completed since the " + "consumer is undergoing a rebalance for auto partition assignment. You can try completing the rebalance " + "by calling poll() and then retry the operation."));

} else {

// 如果消费者不在重平衡的过程中，那么就返回消费者不在激活的消费组异常。

return RequestFuture.failure(new CommitFailedException("Offset commit cannot be completed since the " + "consumer is not part of an active group for auto partition assignment; it is likely that the consumer " + "was kicked out of the group."));

}

}

} else {

// 如果是指定消费分区就不考虑

generation = Generation.NO\_GENERATION;

}

// 5、构造提交 offset 的请求。

OffsetCommitRequest.Builder builder \= new OffsetCommitRequest.Builder(

new OffsetCommitRequestData()

.setGroupId(this.rebalanceConfig.groupId)

.setGenerationId(generation.generationId)

.setMemberId(generation.memberId)

.setGroupInstanceId(rebalanceConfig.groupInstanceId.orElse(null))

.setTopics(new ArrayList<>(requestTopicDataMap.values()))

);

log.trace("Sending OffsetCommit request with {} to coordinator {}", offsets, coordinator);

// 6、把请求保存在 client 的缓存中，同时配置回调对象。

return client.send(coordinator, builder)

.compose(new OffsetCommitResponseHandler(offsets, generation));

}

该方法用来「**创建并缓存 OffsetCommitRequest 请求**」，核心步骤如下：

1.  获取 [GroupCoordinator](http://groupcoordinator/)，当 [GroupCoordinator](http://groupcoordinator/) 为 null 时，我们认为消费者连从属的 [GroupCoordinator](http://groupcoordinator/) 都找不到，那么提交 offset 就无从谈起了，于是返回 [GroupCoordinator](http://groupcoordinator/) 不可用的结果。
2.  创建 map 集合 [requestTopicDataMap](http://requesttopicdatamap/)，里面的 entry 的 key 为主题，value 为主题下分区的提交 offset 的信息。
3.  填充集合 requestTopicDataMap。
4.  判断消费者的分区方案是自动的还是手动的。如果是自动的，要判断 generation 是否为空，如果为空我们认为消费者没有加入到活跃的 [GroupCoordinator](http://groupcoordinator/) 中，如果一个消费者没有加入到活跃的 [GroupCoordinator](http://groupcoordinator/) 中那么我们认为它的分区分配方案是有问题的。那么提交 offset 请求也应该拦截，对于提交 offset 请求的拦截有两个场景：
5.  加入的消费组在重平衡过程中，这是我们可以把这种情况合并为消费组正在重平衡的异常，并抛出消费组正在重平衡的异常。
6.  加入的消费组不在重平衡过程中，我们抛出提交 offset 的异常。
7.  构建提交 offset 的请求。
8.  把请求保存在 client 的缓存中，同时配置 offset 提交请求回调对象。

最后我们再来剖析下 [OffsetCommitResponseHandler#handle()](http://offsetcommitresponsehandler/#handle\(\))。

## **3.1.8 OffsetCommitResponseHandler#handle()**

private class OffsetCommitResponseHandler extends CoordinatorResponseHandler<OffsetCommitResponse, Void> {

private final Map<TopicPartition, OffsetAndMetadata> offsets;

private OffsetCommitResponseHandler(Map<TopicPartition, OffsetAndMetadata> offsets, Generation generation) {

super(generation);

this.offsets = offsets;

}

@Override

public void handle(OffsetCommitResponse commitResponse, RequestFuture<Void> future) {

sensors.commitSensor.record(response.requestLatencyMs());

Set<String> unauthorizedTopics = new HashSet<>();

// 1、遍历已提交的所有 offset 的信息

for (OffsetCommitResponseData.OffsetCommitResponseTopic topic : commitResponse.data().topics()) {

for (OffsetCommitResponseData.OffsetCommitResponsePartition partition : topic.partitions()) {

TopicPartition tp \= new TopicPartition(topic.name(), partition.partitionIndex());

OffsetAndMetadata offsetAndMetadata \= this.offsets.get(tp);

long offset \= offsetAndMetadata.offset();

Errors error \= Errors.forCode(partition.errorCode());

if (error == Errors.NONE) {

log.debug("Committed offset {} for partition {}", offset, tp);

} else {

....

}

}

}

if (!unauthorizedTopics.isEmpty()) {

log.error("Not authorized to commit to topics {}", unauthorizedTopics);

future.raise(new TopicAuthorizationException(unauthorizedTopics));

} else {

// 2、把成功提交的事件传播出去。

future.complete(null);

}

}

}

这个方法比较简单，就是遍历提交 offset 请求的响应，如果有错误就根据错误类型分别处理，如何没有错误就把提交 offset 成功的事件传播出去。

## **3.2 消费者获取 offset**

**消费者获取 offset 的目的就是获取新分配的分区的消费 offset**。比如：消费者刚刚启动后 [GroupCoordinator](http://groupcoordinator/) 给它分配了要消费的分区，但是消费者不知道从哪个 offset 消费。这就需要消费者向 [GroupCoordinator](http://groupcoordinator/) 发送请求获取分区对应的 offset。

在剖析方法之前，我们先来剖析一下消费者获取 offset 的两个重要的数据结构：「**OffsetFetchRequest**」、「**OffsetFetchResponse**」的消息体格式。

##   
**3.2.1 OffsetFetchRequest 消息体**

![](https://article-images.zsxq.com/FvsVD-A3PAFCpDkylcpA1K1Zqqo5)

这里剖析下各字段的含义：

1.  group\_id：表示 [ConsumerGroup](http://consumergroup/) 的 Id。
2.  topic：表示 topic 名称。
3.  partition：表示分区编号。

## **3.2.2 OffsetFetchResponse 消息体**

![](https://article-images.zsxq.com/luKoQaquSZMzYDcVFe541WAeud0-)

这里剖析下各字段的含义：

1.  partition：表示分区编号。
2.  offset：表示已提交位移值。
3.  metadata：表示任何希望与 offset 一起保存的自定义元数据。
4.  error\_code：表示错误码。

接下来我们来剖析获取 offset 的相关方法的源码，获取 offset 的方法是在 [ConsumerCoordinator](http://consumercoordinator/) 类中，入口方法是 [refreshCommittedOffsetsIfNeeded()](http://refreshcommittedoffsetsifneeded\(\)/)。

## **3.2.3 refreshCommittedOffsetsIfNeeded()**

public boolean refreshCommittedOffsetsIfNeeded(Timer timer) {

// 1、获取处于初始化阶段的分区

final Set<TopicPartition> initializingPartitions = subscriptions.initializingPartitions();

// 2、向 GroupCoordinator 阻塞发送获取 offset 的请求

final Map<TopicPartition, OffsetAndMetadata> offsets = fetchCommittedOffsets(initializingPartitions, timer);

if (offsets == null) return false;

// 3、循环获取分区对应的 offset

for (final Map.Entry<TopicPartition, OffsetAndMetadata> entry : offsets.entrySet()) {

final TopicPartition tp \= entry.getKey();

final OffsetAndMetadata offsetAndMetadata \= entry.getValue();

if (offsetAndMetadata != null) {

// first update the epoch if necessary

entry.getValue().leaderEpoch().ifPresent(epoch -> this.metadata.updateLastSeenEpochIfNewer(entry.getKey(), epoch));

// 4、判断这个分区是否给这个消费者分配了。

if (this.subscriptions.isAssigned(tp)) {

// 获取分区 Leader Epoch 信息

final ConsumerMetadata.LeaderAndEpoch leaderAndEpoch \= metadata.currentLeader(tp);

// 从 subscriptions 对应的分区 offset 位置

final SubscriptionState.FetchPosition position \= new SubscriptionState.FetchPosition(offsetAndMetadata.offset(), offsetAndMetadata.leaderEpoch(), leaderAndEpoch);

// 更新 subscriptions 对应的分区 offset

this.subscriptions.seekUnvalidated(tp, position);

log.info("Setting offset for partition {} to the committed offset {}", tp, position);

} else {

log.info("Ignoring the returned {} since its partition {} is no longer assigned",offsetAndMetadata, tp);

}

}

}

return true;

}

该方法用来「**刷新已提交位移的信息的**」，核心步骤如下：

1.  获取 offset 状态是初始化状态的分区集合，因为只有初始化状态的分区获取已提交 offset 才有意义。
2.  调用 [fetchCommittedOffsets()](http://fetchcommittedoffsets\(\)/) 方法向 [GroupCoordinator](http://groupcoordinator/) 发送获取 offset 的请求，并返回分区对应的已提交 offset 的map 集合。
3.  循环获取每个分区对应的 offset 元数据信息，更新 subscriptions 中对应分区已经提交的 offset。
4.  判断这个分区是否是消费者要消费的分区。因为如果重平衡后这个消费者不在消费这个分区，那就没必要再更新分区的已提交 offset 了。
5.  如果这个分区是消费者要消费的分区，就更新 subscriptions 中对应分区已提交 offset。

这里重点是「**第二步**」，用来发送获取已提交 offset 请求的，我们来剖析下。

## **3.2.4 fetchCommittedOffsets()**

public Map<TopicPartition, OffsetAndMetadata> fetchCommittedOffsets(final Set<TopicPartition> partitions,

final Timer timer) {

if (partitions.isEmpty()) return Collections.emptyMap();

final Generation generationForOffsetRequest \= generationIfStable();

// 1、判断是否已经发送过了相同的请求，如果不相同，先清空待提交位移请求

if (pendingCommittedOffsetRequest != null &&

!pendingCommittedOffsetRequest.sameRequest(partitions, generationForOffsetRequest)) {

// if we were waiting for a different request, then just clear it.

pendingCommittedOffsetRequest = null;

}

do {

// 2、判断 Coordinator 是否可用。

if (!ensureCoordinatorReady(timer)) return null;

// contact coordinator to fetch committed offsets

final RequestFuture<Map<TopicPartition, OffsetAndMetadata>> future;

if (pendingCommittedOffsetRequest != null) {

future = pendingCommittedOffsetRequest.response;

} else {

// 3、缓存获取 offset 的请求，并配置回调对象。

future = sendOffsetFetchRequest(partitions);

pendingCommittedOffsetRequest = new PendingCommittedOffsetRequest(partitions, generationForOffsetRequest, future);

}

// 4、网络发送获取 offset 的请求。

client.poll(future, timer);

// 5、阻塞等待响应处理完成。

if (future.isDone()) {

// 把 pendingCommittedOffsetRequest 置为空，这样下个获取已提交 offset 的请求就能执行了

pendingCommittedOffsetRequest = null;

// 如果成功，返回响应数据

if (future.succeeded()) {

return future.value();

// 如果重试失败，返回异常

} else if (!future.isRetriable()) {

throw future.exception();

} else {

// 设置重试时间

timer.sleep(rebalanceConfig.retryBackoffMs);

}

} else {

return null;

}

} while (timer.notExpired());

return null;

}

该方法用来「**发送获取 offset 请求**」，核心步骤如下：

1.  判断是否已经发送过了同样的请求。因为有可能已经发送了获取已提交 offset 的请求但还没有返回。如果为 ture，我们就把[pendingCommittedOffsetRequest](http://pendingcommittedoffsetrequest/) 赋值为 null。判断为 ture 的条件是下面两个条件必须同时满足：
2.  [pendingCommittedOffsetRequest != null](http://pendingcommittedoffsetrequest%20!=%20null%20/) ：[pendingCommittedOffsetRequest](http://pendingcommittedoffsetrequest/) 不为空。[pendingCommittedOffsetRequest](http://pendingcommittedoffsetrequest/) 是正在获取但还没返回的已提交offset的请求。
3.  [!pendingCommittedOffsetRequest.sameRequest(partitions, generationForOffsetRequest)](http://!pendingcommittedoffsetrequest.samerequest\(partitions,%20generationforoffsetrequest\)/)：判断现在的请求是否和上次的一样。
4.  确保 [GroupCoordinator](http://groupcoordinator/) 是否可用，如果可用直接返回，如果不可用就要查找 [GroupCoordinator](http://groupcoordinator/) 并创建连接。
5.  根据 [pendingCommittedOffsetRequest](http://pendingcommittedoffsetrequest/) 的值是否为空采用不同的处理逻辑。如果不为空，说明跟上次的请求是一个请求，就不用再发送网络请求了，直接把上个请求 future 赋给现在的请求 future。如果为空，我们再缓存获取offset的请求，并配置回调。
6.  网络发送获取已提交 offset 的请求。
7.  阻塞等待已提交 offset 的请求的响应。如果 future 完成了，则 [pendingCommittedOffsetRequest](http://pendingcommittedoffsetrequest/) 置为null。这样下个相同获取已提交 offset 的请求就不用在缓存获取 offset 的请求了。

最后我们来看下处理响应的回调方法，回调方法是 [OffsetFetchResponseHandler#handle()](http://offsetfetchresponsehandler/#handle\(\)) 方法。

##   
**3.2.5 OffsetFetchResponseHandler#handle()**

private class OffsetFetchResponseHandler extends CoordinatorResponseHandler<OffsetFetchResponse, Map<TopicPartition, OffsetAndMetadata>> {

private OffsetFetchResponseHandler() {

super(Generation.NO\_GENERATION);

}

@Override

public void handle(OffsetFetchResponse response, RequestFuture<Map<TopicPartition, OffsetAndMetadata>> future) {

// 处理偏移量获取响应

Errors responseError \= response.groupLevelError(rebalanceConfig.groupId);

if (responseError != Errors.NONE) {

// 如果存在响应错误，根据错误类型进行处理

log.debug("Offset fetch failed: {}", responseError.message());

if (responseError == Errors.COORDINATOR\_LOAD\_IN\_PROGRESS) {

// 如果是协调器加载中错误，重试

future.raise(responseError);

} else if (responseError == Errors.NOT\_COORDINATOR) {

// 如果不是协调器错误，重新发现协调器并重试

markCoordinatorUnknown(responseError);

future.raise(responseError);

} else if (responseError == Errors.GROUP\_AUTHORIZATION\_FAILED) {

// 如果是授权错误，抛出GroupAuthorizationException异常

future.raise(GroupAuthorizationException.forGroupId(rebalanceConfig.groupId));

} else {

// 其他未知错误，抛出KafkaException异常

future.raise(new KafkaException("Unexpected error in fetch offset response: " + responseError.message()));

}

return;

}

Set<String> unauthorizedTopics = null;

// 解析响应中的偏移量数据

Map<TopicPartition, OffsetFetchResponse.PartitionData> responseData = response.partitionDataMap(rebalanceConfig.groupId);

Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>(responseData.size());

Set<TopicPartition> unstableTxnOffsetTopicPartitions = new HashSet<>();

for (Map.Entry<TopicPartition, OffsetFetchResponse.PartitionData> entry : responseData.entrySet()) {

TopicPartition tp \= entry.getKey();

OffsetFetchResponse.PartitionData partitionData \= entry.getValue();

if (partitionData.hasError()) {

// 如果存在偏移量获取错误，根据错误类型进行处理

Errors error \= partitionData.error;

log.debug("Failed to fetch offset for partition {}: {}", tp, error.message());

if (error == Errors.UNKNOWN\_TOPIC\_OR\_PARTITION) {

// 如果是未知分区或主题错误，抛出KafkaException异常

future.raise(new KafkaException("Topic or Partition " + tp + " does not exist"));

return;

} else if (error == Errors.TOPIC\_AUTHORIZATION\_FAILED) {

// 如果是未授权错误，记录未授权的主题

if (unauthorizedTopics == null) {

unauthorizedTopics = new HashSet<>();

}

unauthorizedTopics.add(tp.topic());

} else if (error == Errors.UNSTABLE\_OFFSET\_COMMIT) {

// 如果是不稳定的偏移量提交错误，记录相应的分区

unstableTxnOffsetTopicPartitions.add(tp);

} else {

// 其他未知错误，抛出KafkaException异常

future.raise(new KafkaException("Unexpected error in fetch offset response for partition " + tp + ": " + error.message()));

return;

}

} else if (partitionData.offset >= 0) {

// 如果偏移量有效，记录偏移量及相关的元数据

offsets.put(tp, new OffsetAndMetadata(partitionData.offset, partitionData.leaderEpoch, partitionData.metadata));

} else {

// 如果没有已提交的偏移量，记录为null

log.info("Found no committed offset for partition {}", tp);

offsets.put(tp, null);

}

}

if (unauthorizedTopics != null) {

// 如果存在未授权主题，抛出TopicAuthorizationException异常

future.raise(new TopicAuthorizationException(unauthorizedTopics));

} else if (!unstableTxnOffsetTopicPartitions.isEmpty()) {

// 如果存在不稳定的偏移量提交，抛出UnstableOffsetCommitException异常

log.info(

"The following partitions still have unstable offsets " +

"which are not cleared on the broker side: {}" +

", this could be either " +

"transactional offsets waiting for completion, or " +

"normal offsets waiting for replication after appending to local log",

unstableTxnOffsetTopicPartitions);

future.raise(new UnstableOffsetCommitException("There are unstable offsets for the requested topic partitions"));

} else {

// 响应处理完成，将得到的偏移量数据传递给future对象

future.complete(offsets);

}

}

}

该方法用来「**处理具体的偏移量获取响应的**」。根据响应中的错误类型进行相应的处理，如重试、重新发现协调器、抛出异常等。然后解析响应中的偏移量数据，并根据分区状态和错误类型进行相应的处理，如记录未授权的主题、记录不稳定的偏移量提交等。最后，将解析后的偏移量数据传递给 future 对象，完成偏移量获取操作。

##   
**04 总结**

这里，我们一起来总结一下这篇文章的重点。

1、带你剖析了「**消费者订阅状态**」的工作原理以及重要方法。

2、最后带你剖析了 「**消费者提交与获取 Offset**」的请求与响应的数据结构以及相关方法。

下篇我们来深度剖析「**\_\_consumer\_offsets 探秘**」，大家期待，我们下期见。