大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka** **消费者初始化的流程**」，今天我们开启消费端源码的征程，这是第三篇来深度聊聊「**Kafka 消费者如何拉取数据的**」，看看 Kafka 消费者是如何跟服务端通信的并进行拉取数据的。

![](https://article-images.zsxq.com/FuKnMyp61ddeyXIAAlfiUP5aZhKt)

## **01 总体概述**

从今天开始我将以「 **Kafka 3.0**」 版本为主，通过「**场景驱动**」的方式带大家一点点的对 Kafka 源码进行深度剖析，正式开启 「**Kafka 消费者源码之旅**」，跟我一起来掌握 Kafka 源码核心架构设计思想吧。

今天这篇我们先来聊聊 Kafka 消费者初始化后拉取数据核心流程，带你梳理消费者拉取数据的整体源码分析脉络，接下来会逐一讲解说明。

本文涉及的源码：

「**KafkaConsumer**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/KafkaConsumer.java](https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/KafkaConsumer.java)

「**ConsumerCoordinator**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java](https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java)

「**AbstractCoordinator**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/AbstractCoordinator.java](https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/AbstractCoordinator.java)

「**ConsumerNetworkClient**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerNetWorkClient.java](https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerNetWorkClient.java)

## **02 消费者拉取消息**

KafkaConsumer 中包含两个关键的组件，一个是负责协调消费者组内消费者分区消费的 ConsumerCoordinator，另一个是负责拉取消息的 Fetcher。

1.  ConsumerCoordinator 会与 Kafka 的服务端进行通信，首先确定一个负责当前消费者组的「**消费者组协调器**」，然后与该协调器进行通信，「**加入消费者组**」并完成整个消费者组的「**消费分区分配**」。
2.  Fetcher 会在「**消费分区分配**」完成后向当前消费者负责的分区所在的 Broker 节点「**发起拉取消息**」的请求。

整个消费者启动并拉取消息的核心流程可分为以下几步：

1.  首先消费者要确定自己所在的「**消费者组协调器**」的地址并与其建立连接。在这个过程中，如果消费者需要更新 Kafka 集群元数据，则先更新元数据。
2.  接着消费者向「**消费者组协调器**」所在的服务端发送请求「**加入消费者组**」。在这个过程中，「**消费者组协调器**」指定的 Leader 将结合集群元数据与整个消费者组的消费者信息进行分区分配，完成后将分配方案发送给「**消费者组协调器**」。
3.  接着「**消费者组协调器**」将分区分配方案返回给各个消费者，消费者向自己负责的分区所在的 Broker 发起拉消息请求，完成消息消费。
4.  最后消费者会启动一个心跳线程与「**消费者组协调器**」保持连接，如果「**消费者组协调器**」返回消费者组状态变化，则进行重新加入消费者组的重平衡操作。

这里先通过一张流程图来梳理下，让你提前了解下，下面剖析源码可以对比来消化。

![](https://article-images.zsxq.com/lk1l5MFGAA4WxzvMvPaEpzxKRDiC)

接下来会通过源码来剖析以上流程。

## **2.1 拉取消息的入口**

拉取消息前，消费者首先要声明自己订阅的主题，则 [KafkaConsumer#subscribe()](http://kafkaconsumer/#subscribe\(\)) 将被调用，在上篇已经剖析过。该方法中其实主要涉及一些属性的设置，大致为以下几步：

1.  考虑到多次订阅主题不一致的情况，调用 [Fetcher#clearBufferedDataForUnassignedTopics()](http://fetcher/#clearBufferedDataForUnassignedTopics\(\)) 将已经接收到的不在本次订阅的 topic 列表中的数据清除掉。
2.  调用 [SubscriptionState#subscribe()](http://subscriptionstate/#subscribe\(\)) 方法重置订阅的 topic 列表。
3.  调用 [Metadata#requestUpdateForNewTopics()](http://metadata/#requestUpdateForNewTopics\(\)) 方法设置更新元数据的标识位 [needPartialUpdate](http://needpartialupdate/) 为 true，则后续消费者将发送更新元数据请求。

## **KafkaConsumer#poll()**

当订阅主题后，就会调用 [KafkaConsumer#poll()](http://kafkaconsumer/#poll\(\)) 方法进入拉取消息的流程，此处是消息消费的入口，源码如下：

@Override

public ConsumerRecords<K, V> poll(final Duration timeout) {

return poll(time.timer(timeout), true);

}

private ConsumerRecords<K, V> poll(final Timer timer, final boolean includeMetadataInTimeout) {

acquireAndEnsureOpen();

try {

this.kafkaConsumerMetrics.recordPollStart(timer.currentTimeMs());

if (this.subscriptions.hasNoSubscriptionOrUserAssignment()) {

throw new IllegalStateException("Consumer is not subscribed to any topics or assigned any partitions");

}

do {

client.maybeTriggerWakeup();

if (includeMetadataInTimeout) {

// 1、调用 KafkaConsumer#updateAssignmentMetadataIfNeeded() 方法进入消费者组的初始化的流程，并进行消费者分区分配。

updateAssignmentMetadataIfNeeded(timer, false);

} else {

while (!updateAssignmentMetadataIfNeeded(time.timer(Long.MAX\_VALUE), true)) {

log.warn("Still waiting for metadata");

}

}

// 2、等以上步骤完成后，调用 KafkaConsumer#pollForFetches() 方法拉取消息

final Map<TopicPartition, List<ConsumerRecord<K, V>>> records = pollForFetches(timer);

if (!records.isEmpty()) {

// before returning the fetched records, we can send off the next round of fetches

// and avoid block waiting for their responses to enable pipelining while the user

// is handling the fetched records.

//

// NOTE: since the consumed position has already been updated, we must not allow

// wakeups or any other errors to be triggered prior to returning the fetched records.

if (fetcher.sendFetches() > 0 || client.hasPendingRequests()) {

client.transmitSends();

}

return this.interceptors.onConsume(new ConsumerRecords<>(records));

}

} while (timer.notExpired());

return ConsumerRecords.empty();

} finally {

release();

this.kafkaConsumerMetrics.recordPollEnd(timer.currentTimeMs());

}

}

核心步骤如下：

1.  调用 [KafkaConsumer#updateAssignmentMetadataIfNeeded()](http://kafkaconsumer/#updateAssignmentMetadataIfNeeded\(\)) 方法进入消费者分区分配的流程。
2.  等以上步骤完成后，调用 [KafkaConsumer#pollForFetches()](http://kafkaconsumer/#pollForFetches\(\)) 方法拉取消息。

来看下「**第一步**」的方法。

## **KafkaConsumer#updateAssignmentMetaIfNeeded()**

boolean updateAssignmentMetadataIfNeeded(final Timer timer, final boolean waitForJoinGroup) {

// 调用 ConsumerCoordinator#poll() 方法与服务端协调器交互确定当前消费者负责的分区

if (coordinator != null && !coordinator.poll(timer, waitForJoinGroup)) {

return false;

}

return updateFetchPositions(timer);

}

这里核心就是调用 [ConsumerCoordinator#poll()](http://consumercoordinator/#poll\(\)) 方法与服务端协调器交互确定当前消费者负责的分区。

在该方法中调用了 [coordinator.poll](http://coordinator.poll/)() 方法，poll() 方法里面又调用了 client.ensureFreshMetadata() 方法，在 client.ensureFreshMetadata() 方法中又调用了 [client.poll](http://client.poll/)() 方法，实现了与 Cluster 通信，在 Coordinator 上注册 Consumer 并拉取和更新元数据。

关于「**第一步**」的拉取消息的方法，我们会在文章最后一部分进行剖析，这里就忽略了。

至此「**消费者拉取消息的入口**」的源码流程就剖析完了。

## **2.2 消费者与协调器交互**

## **2.2.1 定位消费者组协调器**

**ConsumerCoordinator#poll()**

接着我们来剖析下上面没有剖析的 [ConsumerCoordinator#poll()](http://consumercoordinator/#poll\(\)) 方法，源码如下：

/\*\*

\* Check the status of the heartbeat thread (if it is active) and indicate the liveness

\* of the client. This must be called periodically after joining with {@link #ensureActiveGroup()}

\* to ensure that the member stays in the group. If an interval of time longer than the

\* provided rebalance timeout expires without calling this method, then the client will proactively

\* leave the group.

\*

\* @param now current time in milliseconds

\* @throws RuntimeException for unexpected errors raised from the heartbeat thread

\*/

public boolean poll(Timer timer, boolean waitForJoinGroup) {

// 更新订阅的分区元数据

maybeUpdateSubscriptionMetadata();

// 调用已完成的位移提交回调函数

invokeCompletedOffsetCommitCallbacks();

if (subscriptions.hasAutoAssignedPartitions()) {

if (protocol == null) {

// 检查用户配置的分区分配策略是否为空，如果为空则抛出异常

throw new IllegalStateException("User configured " + ConsumerConfig.PARTITION\_ASSIGNMENT\_STRATEGY\_CONFIG +

" to empty while trying to subscribe for group protocol to auto assign partitions");

}

// 发送心跳请求

pollHeartbeat(timer.currentTimeMs());

// 如果当前还没有找到消费者所在的消费者组协调器，则需调用父类 AbstractCoordinator#ensureCoordinatorReady() 方法确保和协调器建立连接

if (coordinatorUnknown() && !ensureCoordinatorReady(timer)) {

return false;

}

// 如果需要重新加入消费者组（rebalance）

if (rejoinNeededOrPending()) {

if (subscriptions.hasPatternSubscription()) {

// 处理模式订阅

if (this.metadata.timeToAllowUpdate(timer.currentTimeMs()) == 0) {

this.metadata.requestUpdate();

}

if (!client.ensureFreshMetadata(timer)) {

return false;

}

// 更新的订阅元数据

maybeUpdateSubscriptionMetadata();

}

// 如果消费者还没有加入消费者组或者通过心跳监听到消费者组状态有变化，则需要调用父类 AbstractCoordinator#ensureActiveGroup() 方法等待消费者组分区分配完成

if (!ensureActiveGroup(waitForJoinGroup ? timer : time.timer(0L))) {

// 由于在被调用的方法中可能使用了不同的计时器，因此仍然需要更新原始计时器的当前时间

timer.update(time.milliseconds());

return false;

}

}

} else {

// 处理手动分配分区

if (metadata.updateRequested() && !client.hasReadyNodes(timer.currentTimeMs())) {

client.awaitMetadataUpdate(timer);

}

}

// 最后无论以上处理是否执行，都调用 ConsumerCoordinator#maybeAutoCommitOffsetsAsync() 方法检查自动提交是否开启，如果开启则需要提交上一次拉取消费的各个分区的消息

maybeAutoCommitOffsetsAsync(timer.currentTimeMs());

return true;

}

核心步骤如下：

1.  更新订阅的分区元数据。
2.  调用已完成的位移提交回调函数。
3.  如果已经自动分配了分区，则执行以下操作：
4.  检查用户配置的分区分配策略是否为空，如果为空则抛出异常。
5.  发送心跳请求。
6.  如果当前还没有找到消费者所在的消费者组协调器，则需调用父类 [AbstractCoordinator#ensureCoordinatorReady()](http://abstractcoordinator/#ensureCoordinatorReady\(\)) 方法确保和协调器建立连接。
7.  如果消费者还没有加入消费者组或者通过心跳监听到消费者组状态有变化，则需要调用父类 [AbstractCoordinator#ensureActiveGroup()](http://abstractcoordinator/#ensureActiveGroup\(\)) 方法等待消费者组分区分配完成。
8.  如果没有分配分区，则处理手动分配分区。
9.  最后无论以上处理是否执行，都调用 [ConsumerCoordinator#maybeAutoCommitOffsetsAsync()](http://consumercoordinator/#maybeAutoCommitOffsetsAsync\(\)) 方法检查自动提交是否开启，如果开启则需要提交上一次拉取消费的各个分区的消息。

## **AbstractCoordinator#pollHeartbeat()**

接着我们来剖析下「**发送心跳**」的 [AbstractCoordinator#pollHeartbeat()](http://abstractcoordinator/#pollHeartbeat\(\)) 方法，源码如下：

protected synchronized void pollHeartbeat(long now) {

if (heartbeatThread != null) {

if (heartbeatThread.hasFailed()) {

// 如果心跳线程发生故障，将其设置为 null 并抛出异常。如果用户捕获了异常，则下一次调用 ensureActiveGroup() 将会创建一个新的 heartbeat 线程。

RuntimeException cause = heartbeatThread.failureCause();

heartbeatThread = null;

throw cause;

}

// 如果需要唤醒心跳线程，则调用 notify() 方法唤醒它

if (heartbeat.shouldHeartbeat(now)) {

notify();

}

// 执行心跳操作

heartbeat.poll(now);

}

}

核心步骤如下：

1.  如果心跳线程发生故障，将其设置为 null 并抛出异常。如果用户捕获了异常，则下一次调用 [ensureActiveGroup()](http://ensureactivegroup\(\)/) 将会创建一个新的 heartbeat 线程。
2.  如果需要唤醒心跳线程，则调用 notify() 方法唤醒它。
3.  执行心跳操作。

## **AbstractCoordinator#ensureCoordinatorReady()**

接着我们来剖析下「**确定消费者协调者完成**」的 [AbstractCoordinator#ensureCoordinatorReady()](http://abstractcoordinator/#ensureCoordinatorReady\(\)) 方法，源码如下：

protected synchronized boolean ensureCoordinatorReady(final Timer timer) {

if (!coordinatorUnknown())

return true;

do {

if (fatalFindCoordinatorException != null) {

final RuntimeException fatalException = fatalFindCoordinatorException;

fatalFindCoordinatorException = null;

throw fatalException;

}

// 调用 AbstractCoordinator#lookupCoordinator() 方法生成异步请求，并将其存入到请求队列中，即将请求存入 ConsumerNetworkClient#unsent 队列中，此处设置请求类型 KafkaApis.FIND\_COORDINATOR

final RequestFuture<Void> future = lookupCoordinator();

// 调用 ConsumerNetworkClient#poll() 实际发起网络请求，并监听服务端的响应，如异步请求完成则退出循环

client.poll(future, timer);

if (!future.isDone()) {

// ran out of time

break;

}

RuntimeException fatalException = null;

if (future.failed()) {

if (future.isRetriable()) {

log.debug("Coordinator discovery failed, refreshing metadata", future.exception());

client.awaitMetadataUpdate(timer);

} else {

fatalException = future.exception();

log.info("FindCoordinator request hit fatal exception", fatalException);

}

} else if (coordinator != null && client.isUnavailable(coordinator)) {

// we found the coordinator, but the connection has failed, so mark

// it dead and backoff before retrying discovery

markCoordinatorUnknown("coordinator unavailable");

timer.sleep(rebalanceConfig.retryBackoffMs);

}

clearFindCoordinatorFuture();

if (fatalException != null)

throw fatalException;

} while (coordinatorUnknown() && timer.notExpired());

return !coordinatorUnknown();

}

该方法会在 while 循环中不断向 Kafka 服务端发起请求确定消费者组协调器，其核心步骤如下：

1.  调用 [AbstractCoordinator#lookupCoordinator()](http://abstractcoordinator/#lookupCoordinator\(\)) 方法寻找消费者协调器，生成异步请求，并将其存入到请求队列中。即将请求存入 [ConsumerNetworkClient#unsent](http://consumernetworkclient/#unsent) 队列中，此处设置请求类型 [KafkaApis.FIND\_COORDINATOR](http://kafkaapis.find_coordinator/)。
2.  调用 [ConsumerNetworkClient#poll()](http://consumernetworkclient/#poll\(\)) 实际发起网络请求，并监听服务端的响应，如异步请求完成则退出循环。

## **AbstractCoordinator#lookupCoordinator()**

接着我们来剖析下「**寻找消费者协调者**」的 [AbstractCoordinator#lookupCoordinator()](http://abstractcoordinator/#ensureCoordinatorReady\(\)) 方法，源码如下：

protected synchronized RequestFuture<Void> lookupCoordinator() {

if (findCoordinatorFuture == null) {

// 首先调用 ConsumerNetworkClient#leastLoadedNode() 方法从集群元数据中找一个最小负载节点作为询问协调者

Node node \= this.client.leastLoadedNode();

if (node == null) {

log.debug("No broker available to send FindCoordinator request");

return RequestFuture.noBrokersAvailable();

} else {

// 调用 AbstractCoordinator#sendFindCoordinatorRequest() 方法生成发送给目标节点的异步请求，发送 FIND\_COORDINATOR 请求，其实是将该请求放入未发送队列中 unsent

findCoordinatorFuture = sendFindCoordinatorRequest(node);

}

}

return findCoordinatorFuture;

}

该方法会寻找消费者组协调器，其核心步骤如下：

1.  首先调用 [ConsumerNetworkClient#leastLoadedNode()](http://consumernetworkclient/#leastLoadedNode\(\)) 方法从集群元数据中取得一个负载最小的节点，最终是调用 [NetworkClient#leastLoadedNode()](http://consumernetworkclient/#leastLoadedNode\(\)) 方法。
2.  调用 [AbstractCoordinator#sendFindCoordinatorRequest()](http://abstractcoordinator/#sendFindCoordinatorRequest\(\)) 方法生成发送给目标节点的异步请求，即发送 [FIND\_COORDINATOR](http://find_coordinator/) 请求，其实是将该请求放入未发送队列中 unsent。

如下图，寻找负载最小的节点：

![](https://article-images.zsxq.com/Fhazq4J_au5vbIRaJW7sf4qEVaBU)

## **AbstractCoordinator#sendFindCoordinatorRequest()**

接着我们来剖析「**发送寻找消费者协调者**」的 [AbstractCoordinator#](http://abstractcoordinator/#ensureCoordinatorReady\(\))[sendFindCoordinatorRequest(](http://abstractcoordinator/#sendFindCoordinatorRequest\(\))[)](http://abstractcoordinator/#ensureCoordinatorReady\(\)) 方法，源码如下：

private RequestFuture<Void> sendFindCoordinatorRequest(Node node) {

// initiate the group metadata request

log.debug("Sending FindCoordinator request to broker {}", node);

// 构建寻找消费者协调器请求数据

FindCoordinatorRequestData data \= new FindCoordinatorRequestData()

.setKeyType(CoordinatorType.GROUP.id())

.setKey(this.rebalanceConfig.groupId);

// 里面设置此次请求的类型：ApiKeys.FIND\_COORDINATOR

FindCoordinatorRequest.Builder requestBuilder \= new FindCoordinatorRequest.Builder(data);

// 调用 ConsumerNetworkClient#send() 方法生成一个 FindCoordinator 异步请求并入队，将请求存储在 unsent 中

return client.send(node, requestBuilder)

// 调用异步请求 RequestFuture#compose() 方法为其添加监听器，并将服务端响应的回调处理器 FindCoordinatorResponseHandler 封装在监听器中

.compose(new FindCoordinatorResponseHandler());

}

核心步骤如下：

1.  调用 [ConsumerNetworkClient#send()](http://consumernetworkclient/#send\(\)%20) 方法生成一个 [FindCoordinator](http://findcoordinator%20/) 异步请求并入队，将请求存储在 unsent 中。
2.  调用异步请求 [RequestFuture#compose()](http://requestfuture/#compose\(\)) 方法为其添加监听器，并将服务端响应的回调处理器 [FindCoordinatorResponseHandler](http://findcoordinatorresponsehandler/) 封装在监听器中。

关于 [ConsumerNetworkClient#send()](http://consumernetworkclient/#send\(\)) 以及 [RequestFuture#compose()](http://requestfuture/#compose\(\)) 在上篇已经剖析过了，这里就不赘述，可以直接点击 [【消费者源码分析系列第二篇】图解 Kafka 源码之消费者网络通信组件架构设计](https://articles.zsxq.com/id_mvh8k9jtwuh0.html) 。

另外关于如何寻找协调器需要从 [ConsumerNetworkClient#poll()](http://consumernetworkclient/#poll\(\)%20) 方法，请移步 [【消费者源码分析系列第二篇】图解 Kafka 源码之消费者网络通信组件架构设计](https://articles.zsxq.com/id_mvh8k9jtwuh0.html) 这里学习。

这里只剖析下 [ConsumerNetworkClient#poll()](http://consumernetworkclient/#poll\(\)) 方法的最后一步， [ConsumerNetworkClient#firePendingCompletedRequests()](http://consumernetworkclient/#firePendingCompletedRequests\(\)) 方法将依次回调响应的回调函数，最终底层数据回调的链路比较绕，这里我大致梳理如下，最终将回调到 [FindCoordinatorResponseHandler#onSuccess()](http://findcoordinatorresponsehandler/#onSuccess\(\)) 方法。

![](https://article-images.zsxq.com/Fq3r3coGHLB3aOA8pyWMEXgiVZT7)

寻找流程如下：

![](https://article-images.zsxq.com/luJZxwKkXbjYsYz4x5YxG-qoNesy)

## **FindCoordinatorResponseHandler#onSuccess()**

最后剖析下 [FindCoordinatorResponseHandler#onSuccess()](http://findcoordinatorresponsehandler/#onSuccess\(\)) 方法，源码如下：

private class FindCoordinatorResponseHandler extends RequestFutureAdapter<ClientResponse, Void> {

@Override

public void onSuccess(ClientResponse resp, RequestFuture<Void> future) {

log.debug("Received FindCoordinator response {}", resp);

// 1、解析 FindCoordinator 响应中的 Coordinator 数据

List<Coordinator> coordinators = ((FindCoordinatorResponse) resp.responseBody()).coordinators();

if (coordinators.size() != 1) {

// 如果响应中包含多个 Coordinator，则抛出异常

log.error("Group coordinator lookup failed: Invalid response containing more than a single coordinator");

future.raise(new IllegalStateException("Group coordinator lookup failed: Invalid response containing more than a single coordinator"));

}

Coordinator coordinatorData \= coordinators.get(0);

Errors error \= Errors.forCode(coordinatorData.errorCode());

if (error == Errors.NONE) {

synchronized (AbstractCoordinator.this) {

// 使用 MAX\_VALUE - node.id 作为 Coordinator 的连接 ID，以便在底层网络客户端层为 Coordinator 与其他连接隔离

int coordinatorConnectionId \= Integer.MAX\_VALUE - coordinatorData.nodeId();

// 2、首先根据服务端响应数据构建协调器 Coordinator 节点信息

AbstractCoordinator.this.coordinator = new Node(

coordinatorConnectionId,

coordinatorData.host(),

coordinatorData.port());

log.info("Discovered group coordinator {}", coordinator);

// 3、调用 ConsumerNetworkClient#tryConnect()方法尝试与协调器 Coordinator 节点建立连接

client.tryConnect(coordinator);

// 4、重置心跳的会话超时时间

heartbeat.resetSessionTimeout();

}

future.complete(null);

} else if (error == Errors.GROUP\_AUTHORIZATION\_FAILED) {

// 如果出现 GROUP\_AUTHORIZATION\_FAILED 错误，则抛出 GroupAuthorizationException 异常

future.raise(GroupAuthorizationException.forGroupId(rebalanceConfig.groupId));

} else {

log.debug("Group coordinator lookup failed: {}", coordinatorData.errorMessage());

future.raise(error);

}

}

@Override

public void onFailure(RuntimeException e, RequestFuture<Void> future) {

log.debug("FindCoordinator request failed due to {}", e.toString());

if (!(e instanceof RetriableException)) {

// 如果异常不可重试，则记住该异常，确保在主线程中抛出

fatalFindCoordinatorException = e;

}

super.onFailure(e, future);

}

}

核心步骤如下：

1.  解析 FindCoordinator 响应中的 Coordinator 数据，执行以下操作。
2.  首先根据服务端响应数据构建协调器 Coordinator 节点信息。
3.  调用 [ConsumerNetworkClient#tryConnect()](http://%20consumernetworkclient/#tryConnect\(\)) 方法尝试与协调器 Coordinator 节点建立连接。
4.  重置心跳的会话超时时间。

至此「**消费者组协调者定位**」的源码流程就剖析完了。

##   
**2.2.2 消费者加入消费者组**

## **AbstractCoordinator#ensureActiveGroup()**

经过以上这些步骤的调用，此时「**消费者已经定位到消费者组协调者**」并与其建立了连接，接下来回到 **2.2.1** [ConsumerCoordinator#poll()](http://consumercoordinator/#poll\(\)) 方法的第 3 步关于 [AbstractCoordinator#ensureActiveGroup()](http://abstractcoordinator/#ensureActiveGroup\(\)) 方法的调用，源码如下：

boolean ensureActiveGroup(final Timer timer) {

// 1、发送 FIND\_COORDINATOR 请求给 Broker，获取该消费者组对应的 Broker 服务节点

if (!ensureCoordinatorReady(timer)) {

return false;

}

// 2、调用 AbstractCoordinator#startHeartbeatThreadIfNeeded() 方法尝试启动心跳线程，与消费者组协调器建立心跳连接，关于心跳线程的逻辑在下面详细剖析

startHeartbeatThreadIfNeeded();

// 3、调用 AbstractCoordinator#joinGroupIfNeeded() 方法尝试进入消费者加入消费者组的流程

return joinGroupIfNeeded(timer);

}

private synchronized void startHeartbeatThreadIfNeeded() {

if (heartbeatThread == null) {

heartbeatThread = new HeartbeatThread();

heartbeatThread.start();

}

}

核心步骤如下：

1.  发送 [FIND\_COORDINATOR](http://find_coordinator/) 请求给 Broker，获取该消费者组对应的 Broker 服务节点。
2.  调用 [AbstractCoordinator#startHeartbeatThreadIfNeeded()](http://abstractcoordinator/#startHeartbeatThreadIfNeeded\(\)) 方法尝试启动发送心跳线程，每隔3s 发送 [HEARTBEAT](http://heartbeat/) 请求与消费者组协调器建立心跳连接，关于心跳线程的逻辑在下面详细剖析。
3.  调用 [AbstractCoordinator#joinGroupIfNeeded()](http://abstractcoordinator/#joinGroupIfNeeded\(\)) 方法尝试进入消费者加入消费者组的流程。即发送 [JOIN\_GROUP](http://join_group/) 请求给 [FIND\_COORDINATOR](http://find_coordinator%20/) ，请求返回的 Borker 节点加入到 消费者组中 ，[SYNC\_GROUP](http://sync_group/) 请求是在 [JOIN\_GROUP](http://join_group/) 回调处理器中发送，并且是只有当选 Leader 的消费者才发送。

可以得出 ensureActiveGroup() 方法内部可以很清晰看到发送 [FIND\_COORDINATOR](http://find_coordinator/)、[HEARTBEAT](http://heartbeat/)、[JOIN\_GROUP](http://join_group/) 这三个请求的方法，其实 [SYNC\_GROUP](http://sync_group%20/) 请求是在 [joinGroupIfNeeded()](http://joingroupifneeded\(\)/) 方法内部，在 [JOIN\_GROUP](http://join_group/) 请求发送成功后，回调函数内再次发送 [SYNC\_GROUP](http://sync_group/) 请求。

## **AbstractCoordinator#joinGroupIfNeeded()**

接着我们来剖析「**消费者加入消费者组**」的方法，该方法会在 while 循环中不断进行加入消费者组的尝试，直至成功，源码如下：

boolean joinGroupIfNeeded(final Timer timer) {

// 如果需要重新加入或正在等待重新加入，则继续循环

while (rejoinNeededOrPending()) {

if (!ensureCoordinatorReady(timer)) {

// 确保协调器就绪，如果协调器不可用，则返回 false

return false;

}

if (needsJoinPrepare) {

// 如果需要准备加入群组，则调用 onJoinPrepare 方法

needsJoinPrepare = false;

onJoinPrepare(generation.generationId, generation.memberId);

}

// 1、准备请求，准备 JOIN\_GROUP 类型请求，存储到 unsent 队列中，调用 AbstractCoordinator#initiateJoinGroup() 方法生成加入消费者组的 JoinGroup 异步请求，并通过轮询等待响应

final RequestFuture<ByteBuffer> future = initiateJoinGroup();

// 2、调用 ConsumerNetworkClinet#poll() 方法发送请求，轮询处理待发送队列中的所有可发送的请求，这部分在上一节中有分析，不再赘述

client.poll(future, timer);

if (!future.isDone()) {

// 如果等待超时，则返回 false

return false;

}

if (future.succeeded()) {

// 如果 JoinGroup 请求成功

Generation generationSnapshot;

MemberState stateSnapshot;

synchronized (AbstractCoordinator.this) {

// 复制当前的 generation 和 state 数据，以防止在回调期间被并发修改

generationSnapshot = this.generation;

stateSnapshot = this.state;

}

if (!generationSnapshot.equals(Generation.NO\_GENERATION) && stateSnapshot == MemberState.STABLE) {

// 如果当前的 generation 和 state 数据有效

ByteBuffer memberAssignment \= future.value().duplicate();

// 3、如果加入消费者组的异步请求成功完成，则协调器会将当前消费者负责的分区下发过来，消费者调用 ConsumerCoordinator#onJoinComplete() 执行加入群组后的逻辑

onJoinComplete(generationSnapshot.generationId, generationSnapshot.memberId, generationSnapshot.protocolName, memberAssignment);

resetJoinGroupFuture();

needsJoinPrepare = true;

} else {

// 如果当前的 generation 或 state 数据无效，则重新加入群组

final String reason \= String.format("rebalance failed since the generation/state was " + "modified by heartbeat thread to %s/%s before the rebalance callback triggered",generationSnapshot, stateSnapshot);

resetStateAndRejoin(reason);

resetJoinGroupFuture();

}

} else {

// 如果 JoinGroup 请求失败

final RuntimeException exception \= future.exception();

resetJoinGroupFuture();

rejoinNeeded = true;

if (exception instanceof UnknownMemberIdException ||

exception instanceof IllegalGenerationException ||

exception instanceof RebalanceInProgressException ||

exception instanceof MemberIdRequiredException) {

// 如果出现某些特定的异常，例如 UnknownMemberIdException、IllegalGenerationException、

// RebalanceInProgressException、MemberIdRequiredException，则继续下一次循环

continue;

} else if (!future.isRetriable()) {

// 如果不可重试的异常，则抛出异常

throw exception;

}

timer.sleep(rebalanceConfig.retryBackoffMs);

}

}

return true;

}

在 Kafka 的消费者端，当需要加入消费者组时，调用该方法，核心步骤如下：

1.  通过循环来确保成功加入消费者组或重新加入消费者组，直到满足条件或超时为止；
2.  首先确保协调器就绪，如果协调器不可用，则返回 false；
3.  然后发送 JoinGroup 请求，并通过轮询等待 JoinGroup 响应；
4.  根据响应结果的不同情况，执行不同的逻辑：
5.  如果 JoinGroup 请求成功，根据当前的 generation 和 state 数据判断是否需要执行加入消费者组后的逻辑，并调用相应的回调方法；
6.  如果 JoinGroup 请求失败，根据异常的类型决定是否继续尝试或抛出异常。
7.  如果成功加入消费者组或重新加入消费者组，则返回 true，否则返回 false。

## **AbstractCoordinator#initiateJoinGroup()**

[AbstractCoordinator#initiateJoinGroup()](http://abstractcoordinator/#initiateJoinGroup\(\)) 一个入口方法：生成加入消费者组的JoinGroup 异步请求，并通过轮询等待响应，源码如下：

private synchronized RequestFuture<ByteBuffer> initiateJoinGroup() {

// 1、为了避免在调用 poll 方法后被用户唤醒而错误地尝试重新加入消费者组，将 joinFuture 存储起来。

if (joinFuture == null) {

state = MemberState.PREPARING\_REBALANCE;

// 2、如果之前的重平衡失败，可能会连续触发重平衡，这种情况下不更新开始时间。

if (lastRebalanceStartMs == -1L)

lastRebalanceStartMs = time.milliseconds();

joinFuture = sendJoinGroupRequest();

joinFuture.addListener(new RequestFutureListener<ByteBuffer>() {

@Override

public void onSuccess(ByteBuffer value) {

// joinFuture 成功完成时不做任何操作，所有的处理逻辑已经在 SyncGroupResponseHandler 中了

}

@Override

public void onFailure(RuntimeException e) {

// 请求完成后处理失败的情况

// 如果在唤醒后完成了加入消费者组，则忽略异常，重新加入群组

synchronized (AbstractCoordinator.this) {

sensors.failedRebalanceSensor.record();

}

}

});

}

return joinFuture;

}

该方法用于初始化并返回 JoinGroup 请求的 Future，核心步骤如下：

1.  在加入消费者组时，首先判断 joinFuture 是否为 null，如果为 null，则执行下列逻辑：
2.  设置 MemberState 为 PREPARING\_REBALANCE；
3.  如果上次重新平衡的开始时间为 -1（即没有记录），则将当前时间设置为上次重新平衡的开始时间；
4.  发送 JoinGroup 请求，并获取返回的 Future；
5.  为该 Future 添加回调监听器，用于处理成功和失败的情况。
6.  返回 joinFuture。

该方法的作用是在加入消费者组时执行必要的准备操作，并返回 「**JoinGroup 请求**」的 Future。在加入消费者组之前，需要执行一些前置操作，例如「**设置状态**」、「**记录开始时间**」等。同时为了「**避免重复发送 JoinGroup 请求**」，在方法中使用「**JoinFeture**」进行了缓存，保证只发送一次 「**JoinGroup 请求**」，并通过监听器处理成功和失败的情况。

## **AbstractCoordinator#sendJoinGroupRequest()**

当准备工作做好后，就可以开始发送「**JoinGroup 请求**」了，源码如下：

RequestFuture<ByteBuffer> sendJoinGroupRequest() {

// 如果 coordinator 不可用，则返回 RequestFuture，其状态为未知协调器

if (coordinatorUnknown())

return RequestFuture.coordinatorNotAvailable();

// 向协调器发送 JoinGroup 请求

log.info("(Re-)joining group");

JoinGroupRequest.Builder requestBuilder \= new JoinGroupRequest.Builder(

new JoinGroupRequestData()

.setGroupId(rebalanceConfig.groupId)

.setSessionTimeoutMs(this.rebalanceConfig.sessionTimeoutMs)

.setMemberId(this.generation.memberId)

.setGroupInstanceId(this.rebalanceConfig.groupInstanceId.orElse(null))

.setProtocolType(protocolType())

.setProtocols(metadata())

.setRebalanceTimeoutMs(this.rebalanceConfig.rebalanceTimeoutMs)

);

log.debug("Sending JoinGroup ({}) to coordinator {}", requestBuilder, this.coordinator);

// 由于重平衡超时时间是协调器可能会阻塞的最长时间，因此我们使用重平衡超时时间来覆盖请求超时时间。我们额外加了 5 秒用于解决可能出现的小延迟。

int joinGroupTimeoutMs \= Math.max(client.defaultRequestTimeoutMs(),

rebalanceConfig.rebalanceTimeoutMs + JOIN\_GROUP\_TIMEOUT\_LAPSE);

return client.send(coordinator, requestBuilder, joinGroupTimeoutMs)

.compose(new JoinGroupResponseHandler(generation));

}

该方法用于向协调器发送 JoinGroup 请求，并返回一个 RequestFuture，用于处理响应结果。核心步骤如下：

1.  首先通过 [coordinatorUnknown()](http://coordinatorunknown\(\)/) 方法检查 coordinator 是否可用，如果是，则返回一个 RequestFuture，其状态为未知协调器。
2.  构造一个 [JoinGroupRequest](http://joingrouprequest/) 的建造者对象，设置 groupId、sessionTimeoutMs、memberId、groupInstanceId、protocolType、protocols 和 rebalanceTimeoutMs 等参数。
3.  计算请求的超时时间，使用重平衡超时时间来覆盖请求超时时间，以确保请求不会因为协调器阻塞而超时。
4.  调用 [ConsumerNetworkClient#send()](http://consumernetworkclient/#send\(\)) 方法向协调器发送 JoinGroup 请求，返回一个 Future 对象。
5.  使用 [JoinGroupResponseHandler](http://joingroupresponsehandler/) 处理响应结果，并返回一个处理 JoinGroup 请求结果的 RequestFuture。

其作用是发送「**JoinGroup 请求**」到协调器，获取 「**JoinGroup 请求**」的响应结果，并通过 RequestFuture 返回该结果。在发送 JoinGroup 请求时需要构造 JoinGroupRequest 请求对象，并计算请求的超时时间。返回的 RequestFuture 对象用于处理响应结果。

当发送完成「**JoinGroup 请求**」到协调器并获取其响应结果后，接下来会处理其响应并发送给上层应用，即方法 [JoinGroupResponseHandler](http://joingroupresponsehandler/) 响应处理器的相关源码。

该方法会处理多种异常情况，核心就是根据协调器返回的消费者组 leader 的 id 判断自身是否是消费者组 leader，如是的话则调用 [AbstractCoordinator#onJoinLeader()](http://abstractcoordinator/#onJoinLeader\(\)) 进行分区分配。

// 请求 JOIN\_GROUP 的响应处理器

private class JoinGroupResponseHandler extends CoordinatorResponseHandler<JoinGroupResponse, ByteBuffer> {

private JoinGroupResponseHandler(final Generation generation) {

super(generation);

}

@Override

public void handle(JoinGroupResponse joinResponse, RequestFuture<ByteBuffer> future) {

Errors error \= joinResponse.error();

if (error == Errors.NONE) {

if (isProtocolTypeInconsistent(joinResponse.data().protocolType())) {

log.error("JoinGroup failed: Inconsistent Protocol Type, received {} but expected {}",

joinResponse.data().protocolType(), protocolType());

future.raise(Errors.INCONSISTENT\_GROUP\_PROTOCOL);

} else {

// 成功分支，处理加入组成功的情况

log.debug("Received successful JoinGroup response: {}", joinResponse);

sensors.joinSensor.record(response.requestLatencyMs());

synchronized (AbstractCoordinator.this) {

// 修改状态，完成 再平衡

if (state != MemberState.PREPARING\_REBALANCE) {

// if the consumer was woken up before a rebalance completes, we may have already left

// the group. In this case, we do not want to continue with the sync group.

future.raise(new UnjoinedGroupException());

} else {

state = MemberState.COMPLETING\_REBALANCE;

// we only need to enable heartbeat thread whenever we transit to

// COMPLETING\_REBALANCE state since we always transit from this state to STABLE

if (heartbeatThread != null)

heartbeatThread.enable();

AbstractCoordinator.this.generation = new Generation(

joinResponse.data().generationId(),

joinResponse.data().memberId(), joinResponse.data().protocolName());

log.info("Successfully joined group with generation {}", AbstractCoordinator.this.generation);

// 判断自己是不是 leader

if (joinResponse.isLeader()) {

// 根据协调器返回的消费者组 leader 的 id 判断自身是否是消费者组 leader，如是的话则调用 AbstractCoordinator#onJoinLeader() 进行指定分区分配，并再次发送 SYNC\_GROUP 请求

onJoinLeader(joinResponse).chain(future);

} else {

onJoinFollower().chain(future);

}

}

}

}

} else if (error == Errors.COORDINATOR\_LOAD\_IN\_PROGRESS) {

// 处理协调器正在加载组的情况

...

} else if (error == Errors.UNKNOWN\_MEMBER\_ID) {

// 处理未知成员ID的情况

...

} else if (error == Errors.COORDINATOR\_NOT\_AVAILABLE || error == Errors.NOT\_COORDINATOR) {

// 处理协调器不可用或者不是协调器的情况

...

} else if (error == Errors.FENCED\_INSTANCE\_ID) {

// 处理组的实例ID被另一个实例锁定的情况

...

} else if (error == Errors.INCONSISTENT\_GROUP\_PROTOCOL || error == Errors.INVALID\_SESSION\_TIMEOUT

|| error == Errors.INVALID\_GROUP\_ID || error == Errors.GROUP\_AUTHORIZATION\_FAILED

|| error == Errors.GROUP\_MAX\_SIZE\_REACHED) {

// 处理组协议不一致、会话超时无效、组ID无效、组授权失败、组达到最大大小的情况

...

} else if (error == Errors.UNSUPPORTED\_VERSION) {

// 处理不支持的版本错误的情况

...

} else if (error == Errors.MEMBER\_ID\_REQUIRED) {

// 处理需要成员ID的情况

...

} else if (error == Errors.REBALANCE\_IN\_PROGRESS) {

// 处理正在进行重新平衡的情况

...

} else {

// 处理其他未预期的错误

...

}

}

}

## **AbstractCoordinator#onJoinLeader()**

接下来我们来剖析下是「**如何进行分区分配并将分配信息同步给协调者**」，源码如下：

private RequestFuture<ByteBuffer> onJoinLeader(JoinGroupResponse joinResponse) {

try {

// 1、调用 ConsumerCoordinator#performAssignment() 进行分区分配

Map<String, ByteBuffer> groupAssignment = performAssignment(joinResponse.data().leader(), joinResponse.data().protocolName(),

joinResponse.data().members());

List<SyncGroupRequestData.SyncGroupRequestAssignment> groupAssignmentList = new ArrayList<>();

for (Map.Entry<String, ByteBuffer> assignment : groupAssignment.entrySet()) {

// 生成SyncGroup 异步请求，调用 AbstractCoordinator#sendSyncGroupRequest() 方法将分配方案同步给协调器

groupAssignmentList.add(new SyncGroupRequestData.SyncGroupRequestAssignment()

.setMemberId(assignment.getKey())

.setAssignment(Utils.toArray(assignment.getValue()))

);

}

// ApiKeys.SYNC\_GROUP 类型请求，给服务端发送制定完成的分区分配策略

SyncGroupRequest.Builder requestBuilder \=

new SyncGroupRequest.Builder(

new SyncGroupRequestData()

.setGroupId(rebalanceConfig.groupId)

.setMemberId(generation.memberId)

.setProtocolType(protocolType())

.setProtocolName(generation.protocolName)

.setGroupInstanceId(this.rebalanceConfig.groupInstanceId.orElse(null))

.setGenerationId(generation.generationId)

.setAssignments(groupAssignmentList)

);

log.debug("Sending leader SyncGroup to coordinator {} at generation {}: {}", this.coordinator, this.generation, requestBuilder);

// 准备请求 存储到 unsent 队列

return sendSyncGroupRequest(requestBuilder);

} catch (RuntimeException e) {

return RequestFuture.failure(e);

}

}

该方法主要用于处理消费者成为组的 Leader 的情况，执行 Leader 同步，并将分配信息发送至协调器。在进行Leader 同步时，需要从组中的其他成员中选择一个 Leader，并执行 Leader 同步。最终成功同步后需要根据组协议和 [GroupMemberMetadata](http://groupmembermetadata/) 将分配信息填充到 [SyncGroupRequest](http://syncgrouprequest/) 中，然后返回 [RequestFuture](http://requestfuture/) 对象。

##   
**ConsumerCoordinator#performAssignment()**

先来剖析下「**分区分配**」的方法 [ConsumerCoordinator#performAssignment()](http://consumercoordinator/#performAssignment\(\))，源码如下：

@Override

protected Map<String, ByteBuffer> performAssignment(String leaderId,

String assignmentStrategy, List<JoinGroupResponseData.JoinGroupResponseMember> allSubscriptions) {

// 1、首先调用 ConsumerCoordinator#lookupAssignor() 找到指定分配策略的分配器

ConsumerPartitionAssignor assignor \= lookupAssignor(assignmentStrategy);

if (assignor == null)

throw new IllegalStateException("Coordinator selected invalid assignment protocol: " + assignmentStrategy);

Set<String> allSubscribedTopics = new HashSet<>();

Map<String, Subscription> subscriptions = new HashMap<>();

// collect all the owned partitions

Map<String, List<TopicPartition>> ownedPartitions = new HashMap<>();

for (JoinGroupResponseData.JoinGroupResponseMember memberSubscription : allSubscriptions) {

Subscription subscription \= ConsumerProtocol.deserializeSubscription(ByteBuffer.wrap(memberSubscription.metadata()));

subscription.setGroupInstanceId(Optional.ofNullable(memberSubscription.groupInstanceId()));

subscriptions.put(memberSubscription.memberId(), subscription);

allSubscribedTopics.addAll(subscription.topics());

ownedPartitions.put(memberSubscription.memberId(), subscription.ownedPartitions());

}

// the leader will begin watching for changes to any of the topics the group is interested in,

// which ensures that all metadata changes will eventually be seen

updateGroupSubscription(allSubscribedTopics);

isLeader = true;

log.debug("Performing assignment using strategy {} with subscriptions {}", assignor.name(), subscriptions);

// 2、调用分配器 ConsumerPartitionAssignor#assign() 方法执行分区分配，并获取分配结果。

// Leadre Consumer 负责制定消费方案：完成 50 个 Conusmer 到 50 个 Partition 消费关系的映射，RoundRobin, Range, Sticky, ....

Map<String, Assignment> assignments = assignor.assign(metadata.fetch(), new GroupSubscription(subscriptions)).groupAssignment();

// 如果使用的协议为RebalanceProtocol.COOPERATIVE，则会校验协作式分配的结果。

if (protocol == RebalanceProtocol.COOPERATIVE) {

validateCooperativeAssignment(ownedPartitions, assignments);

}

Set<String> assignedTopics = new HashSet<>();

// 通过两次比较，检查是否有订阅的主题未被成功分配和是否有未订阅的主题被成功分配，并根据情况更新订阅的主题。

for (Assignment assigned : assignments.values()) {

for (TopicPartition tp : assigned.partitions())

assignedTopics.add(tp.topic());

}

if (!assignedTopics.containsAll(allSubscribedTopics)) {

Set<String> notAssignedTopics = new HashSet<>(allSubscribedTopics);

notAssignedTopics.removeAll(assignedTopics);

log.warn("The following subscribed topics are not assigned to any members: {} ", notAssignedTopics);

}

if (!allSubscribedTopics.containsAll(assignedTopics)) {

Set<String> newlyAddedTopics = new HashSet<>(assignedTopics);

newlyAddedTopics.removeAll(allSubscribedTopics);

log.info("The following not-subscribed topics are assigned, and their metadata will be " + "fetched from the brokers: {}", newlyAddedTopics);

allSubscribedTopics.addAll(assignedTopics);

updateGroupSubscription(allSubscribedTopics);

}

assignmentSnapshot = metadataSnapshot;

log.info("Finished assignment for group at generation {}: {}", generation().generationId, assignments);

Map<String, ByteBuffer> groupAssignment = new HashMap<>();

for (Map.Entry<String, Assignment> assignmentEntry : assignments.entrySet()) {

ByteBuffer buffer \= ConsumerProtocol.serializeAssignment(assignmentEntry.getValue());

groupAssignment.put(assignmentEntry.getKey(), buffer);

}

// 最后，将分配结果序列化为ByteBuffer对象，并返回包含分配结果的Map<String, ByteBuffer>。

return groupAssignment;

}

该方法主要用来协调器执行消费者组的分区分配过程。其中，[ConsumerPartitionAssignor](http://consumerpartitionassignor/) 对象根据指定的分配策略和协议执行具体的分区分配算法，然后将分配结果返回给协调器。在执行分配过程中，需要根据消费者订阅的主题和分区信息进行分配，并根据协议类型校验分配结果的合法性。最终，成功完成分配后，将分配结果序列化并返回。

##   
**AbstractCoordinator#SyncGroupRequestAssignment()**

接着剖析 [onJoinLeader()](http://onjoinleader\(\)/) 方法的第二步 [SyncGroupRequestAssignment()](http://syncgrouprequestassignment\(\)/) 同步分配方案给协调器，源码如下：

private RequestFuture<ByteBuffer> sendSyncGroupRequest(SyncGroupRequest.Builder requestBuilder) {

if (coordinatorUnknown())

return RequestFuture.coordinatorNotAvailable();

// 将异步请求入队，核心处理在收到协调器响应后的回调处理器 SyncGroupResponseHandler 中，收到响应后 SyncGroupResponseHandler#handle() 方法将被触发

return client.send(coordinator, requestBuilder)

.compose(new SyncGroupResponseHandler(generation));

}

而 [SyncGroupResponseHandler#handle()](http://syncgroupresponsehandler/#handle\(\)) 方法的核心是将协调器响应中的分区分配方案通过 [RequestFuture#complete()](http://requestfuture/#complete\(\)) 方法回调传递给上层，源码如下：

private class SyncGroupResponseHandler extends CoordinatorResponseHandler<SyncGroupResponse, ByteBuffer> {

private SyncGroupResponseHandler(final Generation generation) {

super(generation);

}

@Override

public void handle(SyncGroupResponse syncResponse,

RequestFuture<ByteBuffer> future) {

Errors error \= syncResponse.error();

// 处理 SyncGroup 响应结果

if (error == Errors.NONE) {

// 检查协议类型是否一致

if (isProtocolTypeInconsistent(syncResponse.data().protocolType())) {

log.error("SyncGroup failed due to inconsistent Protocol Type, received {} but expected {}",

syncResponse.data().protocolType(), protocolType());

future.raise(Errors.INCONSISTENT\_GROUP\_PROTOCOL);

} else {

log.debug("Received successful SyncGroup response: {}", syncResponse);

sensors.syncSensor.record(response.requestLatencyMs());

synchronized (AbstractCoordinator.this) {

// 如果世代不为空并且状态为 COMPLETING\_REBALANCE

if (!generation.equals(Generation.NO\_GENERATION) && state == MemberState.COMPLETING\_REBALANCE) {

// 仅当世代未重置时检查协议名称

final String protocolName \= syncResponse.data().protocolName();

final boolean protocolNameInconsistent \= protocolName != null &&

!protocolName.equals(generation.protocolName);

// 如果协议名称不一致，则标记错误，并抛出异常

if (protocolNameInconsistent) {

log.error("SyncGroup failed due to inconsistent Protocol Name, received {} but expected {}",

protocolName, generation.protocolName);

future.raise(Errors.INCONSISTENT\_GROUP\_PROTOCOL);

} else {

// 同步组成功，更新状态和相关属性，并返回分配结果

log.info("Successfully synced group in generation {}", generation);

state = MemberState.STABLE;

rejoinNeeded = false;

lastRebalanceEndMs = time.milliseconds();

sensors.successfulRebalanceSensor.record(lastRebalanceEndMs - lastRebalanceStartMs);

lastRebalanceStartMs = -1L;

future.complete(ByteBuffer.wrap(syncResponse.data().assignment()));

}

} else {

// 如果在收到 SyncGroup 响应之前心跳线程将世代数据清除或状态不为 COMPLETING\_REBALANCE，则标记错误并抛出异常

log.info("Generation data was cleared by heartbeat thread to {} and state is now {} before " +

"receiving SyncGroup response, marking this rebalance as failed and retry",

generation, state);

future.raise(Errors.ILLEGAL\_GENERATION);

}

}

}

} else {

// 处理错误情况

if (error == Errors.GROUP\_AUTHORIZATION\_FAILED) {

future.raise(GroupAuthorizationException.forGroupId(rebalanceConfig.groupId));

} else if (error == Errors.REBALANCE\_IN\_PROGRESS) {

log.info("SyncGroup failed: The group began another rebalance. Need to re-join the group. " +

"Sent generation was {}", sentGeneration);

future.raise(error);

} else if (error == Errors.FENCED\_INSTANCE\_ID) {

log.error("SyncGroup failed: The group instance id {} has been fenced by another instance. " +

"Sent generation was {}", rebalanceConfig.groupInstanceId, sentGeneration);

future.raise(error);

} else if (error == Errors.UNKNOWN\_MEMBER\_ID

|| error == Errors.ILLEGAL\_GENERATION) {

log.info("SyncGroup failed: {} Need to re-join the group. Sent generation was {}",

error.message(), sentGeneration);

if (generationUnchanged())

resetGenerationOnResponseError(ApiKeys.SYNC\_GROUP, error);

future.raise(error);

} else if (error == Errors.COORDINATOR\_NOT\_AVAILABLE

|| error == Errors.NOT\_COORDINATOR) {

log.info("SyncGroup failed: {} Marking coordinator unknown. Sent generation was {}",

error.message(), sentGeneration);

markCoordinatorUnknown(error);

future.raise(error);

} else {

future.raise(new KafkaException("Unexpected error from SyncGroup: " + error.message()));

}

}

}

}

## **ConsumerCoordinator#onJoinComplete()**

当请求处理完毕后，回到本节 [joinGroupIfNeeded()](http://joingroupifneeded\(\)/) 的第 3 步， [ConsumerCoordinator#onJoinComplete()](http://consumercoordinator/#onJoinComplete\(\)) 方法源码如下：

@Override

protected void onJoinComplete(int generation,

String memberId,

String assignmentStrategy,

ByteBuffer assignmentBuffer) {

log.debug("Executing onJoinComplete with generation {} and memberId {}", generation, memberId);

// Only the leader is responsible for monitoring for metadata changes (i.e. partition changes)

if (!isLeader)

assignmentSnapshot = null;

// 查找与 assignmentStrategy 对应的 ConsumerPartitionAssignor 实例

ConsumerPartitionAssignor assignor \= lookupAssignor(assignmentStrategy);

if (assignor == null)

throw new IllegalStateException("Coordinator selected invalid assignment protocol: " + assignmentStrategy);

// 创建 ConsumerGroupMetadata 对象，用于记录消费者组的元数据信息

groupMetadata = new ConsumerGroupMetadata(rebalanceConfig.groupId, generation, memberId, rebalanceConfig.groupInstanceId);

Set<TopicPartition> ownedPartitions = new HashSet<>(subscriptions.assignedPartitions());

// 检查 assignmentBuffer 内剩余的字节数是否足够解析出 Assignment 对象

if (assignmentBuffer.remaining() < 2)

throw new IllegalStateException("There are insufficient bytes available to read assignment from the sync-group response (" +

"actual byte size " + assignmentBuffer.remaining() + ") , this is not expected; " + "it is possible that the leader's assign function is buggy and did not return any assignment for this member, " + "or because static member is configured and the protocol is buggy hence did not get the assignment for this member");

// 将assignmentBuffer解析为Assignment对象

Assignment assignment \= ConsumerProtocol.deserializeAssignment(assignmentBuffer);

Set<TopicPartition> assignedPartitions = new HashSet<>(assignment.partitions());

// 检查分配的分区是否与订阅的主题和分区匹配

if (!subscriptions.checkAssignmentMatchedSubscription(assignedPartitions)) {

final String reason \= String.format("received assignment %s does not match the current subscription %s; " + "it is likely that the subscription has changed since we joined the group, will re-join with current subscription",assignment.partitions(), subscriptions.prettyString());

// 请求重新加入消费者组

requestRejoin(reason);

return;

}

final AtomicReference<Exception> firstException = new AtomicReference<>(null);

Set<TopicPartition> addedPartitions = new HashSet<>(assignedPartitions);

addedPartitions.removeAll(ownedPartitions);

if (protocol == RebalanceProtocol.COOPERATIVE) {

Set<TopicPartition> revokedPartitions = new HashSet<>(ownedPartitions);

revokedPartitions.removeAll(assignedPartitions);

log.info("Updating assignment with\\n" +

"\\tAssigned partitions: {}\\n" +

"\\tCurrent owned partitions: {}\\n" +

"\\tAdded partitions (assigned - owned): {}\\n" +

"\\tRevoked partitions (owned - assigned): {}\\n",

assignedPartitions,

ownedPartitions,

addedPartitions,

revokedPartitions

);

if (!revokedPartitions.isEmpty()) {

// 撤销之前拥有但现在不再分配的分区

firstException.compareAndSet(null, invokePartitionsRevoked(revokedPartitions));

// 如果撤销了分区，则需要重新加入消费者组

final String reason \= String.format("need to revoke partitions %s as indicated " +

"by the current assignment and re-join", revokedPartitions);

requestRejoin(reason);

}

}

// 如果分配的分区与订阅的主题和分区匹配，则更新joined subscriptions

maybeUpdateJoinedSubscription(assignedPartitions);

// 调用ConsumerPartitionAssignor的onAssignment方法，更新内部状态并返回第一个发生的异常（如果有）

firstException.compareAndSet(null, invokeOnAssignment(assignor, assignment));

// 重新调度自动提交的定时器

if (autoCommitEnabled)

this.nextAutoCommitTimer.updateAndReset(autoCommitIntervalMs);

// 将分配的分区设置为已分配，并将新增的分区设置为已分配

subscriptions.assignFromSubscribed(assignedPartitions);

// 处理新增的分区

firstException.compareAndSet(null, invokePartitionsAssigned(addedPartitions));

// 如果存在异常，则抛出异常

if (firstException.get() != null) {

if (firstException.get() instanceof KafkaException) {

throw (KafkaException) firstException.get();

} else {

throw new KafkaException("User rebalance callback throws an error", firstException.get());

}

}

}

该方法的作用是在加入消费者组成功后进行后续处理，核心步骤如下：

1.  首先检查是否是 Leader 角色，如果不是则将assignmentSnapshot 设置为null。
2.  然后根据 [assignmentStrategy](http://assignmentstrategy/) 查找 [ConsumerPartitionAssignor](http://consumerpartitionassignor/) 实例。
3.  接着创建 [ConsumerGroupMetadata](http://consumergroupmetadata/) 对象，用于记录消费者组的元数据信息，然后获取当前已分配的分区。
4.  接下来解析 [assignmentBuffer](http://assignmentbuffer%20/) 为 Assignment 对象。之后检查分配的分区是否与订阅的主题和分区匹配：
5.  如果不匹配则重新加入消费者组。如果是 COOPERATIVE 协议，将撤销之前拥有但现在不再分配的分区，并调用 [invokePartitionsRevoked](http://invokepartitionsrevoked%20/) 方法。然后更新 joined subscriptions 并调用 [ConsumerPartitionAssignor](http://consumerpartitionassignor/) #[onAssignment](http://onassignment/) 方法。重新调度自动提交的定时器。将分配的分区设置为已分配，并处理新增的分区。
6.  最后如果存在异常则抛出异常。

简单来说就是使用协调器下发的分区更新本地订阅数据，以便在拉取数据时直接请求分区所在的服务端节点。

至此，「**消费者加入消费者组**」的源码流程就剖析完了。

## **2.2.3 消费者心跳处理**

在 **2.2.2** 节中提到了消费者心跳的启动 [startHeartbeatThreadIfNeeded](http://startheartbeatthreadifneeded/)，那么 [HeartbeatThread#run()](http://heartbeatthread/#run\(\)) 方法将被触发调用，源码如下：

// 定义一个私有的内部类HeartbeatThread，继承自KafkaThread类，并实现AutoCloseable接口。

用于实现一个心跳线程

private class HeartbeatThread extends KafkaThread implements AutoCloseable {

// 表示心跳线程是否启用的标志。

private boolean enabled \= false;

// 表示心跳线程是否已关闭的标志。

private boolean closed \= false;

// 用于保存心跳线程失败时抛出的RuntimeException异常。

private final AtomicReference<RuntimeException> failed = new AtomicReference<>(null);

// 构造函数，初始化心跳线程对象。

private HeartbeatThread() {

super(HEARTBEAT\_THREAD\_PREFIX + (rebalanceConfig.groupId.isEmpty() ? "" : " | " + rebalanceConfig.groupId), true);

}

@Override

// 重写Runnable接口中的run()方法，实现心跳线程的逻辑。

public void run() {

try {

log.debug("Heartbeat thread started");

while (true) {

// 获取 AbstractCoordinator 对象的同步锁，确保线程安全。

synchronized (AbstractCoordinator.this) {

if (closed) // 如果心跳线程已关闭，则直接返回。

return;

if (!enabled) { // 如果心跳线程未启用，则线程进入等待状态。

AbstractCoordinator.this.wait();

continue;

}

// 如果当前线程尚未加入到消息分组中或者已发生失败情况，则禁用当前线程并继续下一个循环。

if (state.hasNotJoinedGroup() || hasFailed()) {

disable();

continue;

}

// 调用客户端的 pollNoWakeup() 方法，进行轮询消息。

client.pollNoWakeup();

long now \= time.milliseconds();

if (coordinatorUnknown()) { // 如果协调器未知，则进行协调器的查找。

if (findCoordinatorFuture != null) {

clearFindCoordinatorFuture();

AbstractCoordinator.this.wait(rebalanceConfig.retryBackoffMs);

} else {

// 如果消费者协调器失连，则调用 AbstractCoordinator#lookupCoordinator() 尝试重新连接

lookupCoordinator();

}

} else if (heartbeat.sessionTimeoutExpired(now)) {

// 如果心跳超时，则将协调器设置为未知。

markCoordinatorUnknown("session timed out without receiving a "

\+ "heartbeat response");

} else if (heartbeat.pollTimeoutExpired(now)) {

log.warn("consumer poll timeout has expired. This means the time between subsequent calls to poll() " + "was longer than the configured max.poll.interval.ms, which typically implies that " + "the poll loop is spending too much time processing messages. You can address this " + "either by increasing max.poll.interval.ms or by reducing the maximum size of batches " + "returned in poll() with max.poll.records.");

// 如果心跳轮询超时，则调用 AbstractCoordinator#maybeLeaveGroup() 方法向协调器发送 LeaveGroup 请求，离开消费者组

maybeLeaveGroup("consumer poll timeout has expired.");

} else if (!heartbeat.shouldHeartbeat(now)) {

// 如果不需要进行心跳，则线程进入等待状态。

AbstractCoordinator.this.wait(rebalanceConfig.retryBackoffMs);

} else {

// 发送请求前的准备，更新时间，心跳间隔等，正常发送心跳请求通过 AbstractCoordinator#sendHeartbeatRequest() 方法触发，并在其回调处理器中做相应处理

heartbeat.sentHeartbeat(now);

final RequestFuture<Void> heartbeatFuture = sendHeartbeatRequest();

heartbeatFuture.addListener(new RequestFutureListener<Void>() {

@Override

public void onSuccess(Void value) {

synchronized (AbstractCoordinator.this) {

heartbeat.receiveHeartbeat();

}

}

@Override

public void onFailure(RuntimeException e) {

synchronized (AbstractCoordinator.this) {

if (e instanceof RebalanceInProgressException) {

heartbeat.receiveHeartbeat();

} else if (e instanceof FencedInstanceIdException) {

log.error("Caught fenced group.instance.id {} error in heartbeat thread", rebalanceConfig.groupInstanceId);

heartbeatThread.failed.set(e);

} else {

heartbeat.failHeartbeat();

// wake up the thread if it's sleeping to reschedule the heartbeat

AbstractCoordinator.this.notify();

}

}

}

});

}

}

}

} catch (AuthenticationException e) {

log.error("An authentication error occurred in the heartbeat thread", e);

this.failed.set(e);

} catch (GroupAuthorizationException e) {

log.error("A group authorization error occurred in the heartbeat thread", e);

this.failed.set(e);

} catch (InterruptedException | InterruptException e) {

Thread.interrupted();

log.error("Unexpected interrupt received in heartbeat thread", e);

this.failed.set(new RuntimeException(e));

} catch (Throwable e) {

log.error("Heartbeat thread failed due to unexpected error", e);

if (e instanceof RuntimeException)

this.failed.set((RuntimeException) e);

else

this.failed.set(new RuntimeException(e));

} finally {

log.debug("Heartbeat thread has closed");

}

}

}

从源码可以看到消费者通过心跳可以做不少的事情，如下：

1.  如果消费者协调器失连，则调用 [AbstractCoordinator#lookupCoordinator()](http://abstractcoordinator/#lookupCoordinator\(\)) 尝试重新连接。
2.  如果心跳轮询超时，则调用 [AbstractCoordinator#maybeLeaveGroup()](http://abstractcoordinator/#maybeLeaveGroup\(\)) 方法向协调器发送 LeaveGroup 请求，离开消费者组
3.  正常发送心跳请求通过 [AbstractCoordinator#sendHeartbeatRequest()](http://abstractcoordinator/#sendHeartbeatRequest\(\)) 方法触发，并在其回调处理器中做相应处理。

## **AbstractCoordinator#sendHeartbeatRequest()**

当正常发送心跳请求通过后，就可以设置「**心跳发送响应处理器**」了，源码如下：

// HEARTBEAT 请求已就绪，存储在了 unsent 队列中

synchronized RequestFuture<Void> sendHeartbeatRequest() {

log.debug("Sending Heartbeat request with generation {} and member id {} to coordinator {}", generation.generationId, generation.memberId, coordinator);

// 看到这里，是不是很熟悉，前面发送 FIND\_COORDINATOR 也是类似处理流程，这里心跳请求类型：ApiKeys.HEARTBEAT

HeartbeatRequest.Builder requestBuilder \=

new HeartbeatRequest.Builder(new HeartbeatRequestData()

.setGroupId(rebalanceConfig.groupId)

.setMemberId(this.generation.memberId)

.setGroupInstanceId(this.rebalanceConfig.groupInstanceId.orElse(null))

.setGenerationId(this.generation.generationId));

// 设置 HeartbeatResponseHandler 为心跳响应处理器,当协调器响应心跳请求时，HeartbeatResponseHandler#handle() 方法将被触发执行。

// 封装请求为 ClientRequest，在存入 unsent 队列中, coordinator是经过 FIND\_COORDINATOR 请求获取到得目标 Broker 节点

return client.send(coordinator, requestBuilder)

.compose(new HeartbeatResponseHandler(generation));

}

核心步骤就是设置 [HeartbeatResponseHandler](http://heartbeatresponsehandler/) 为心跳响应处理器，当协调器响应心跳请求时，[HeartbeatResponseHandler#handle()](http://heartbeatresponsehandler/#handle\(\)) 方法将被触发执行。

// 心跳响应处理类

private class HeartbeatResponseHandler extends CoordinatorResponseHandler<HeartbeatResponse, Void> {

// 构造函数，接收一个 Generation 对象作为参数。

private HeartbeatResponseHandler(final Generation generation) {

super(generation);

}

@Override

// 处理心跳响应的方法，接收一个 HeartbeatResponse 对象和一个 RequestFuture<Void> 对象作为参数。

public void handle(HeartbeatResponse heartbeatResponse, RequestFuture<Void> future) {

// 记录心跳请求的延迟时间。

sensors.heartbeatSensor.record(response.requestLatencyMs());

// 获取心跳响应的错误类型。

Errors error \= heartbeatResponse.error();

if (error == Errors.NONE) { // 如果错误类型为 Errors.NONE，表示心跳响应成功。

log.debug("Received successful Heartbeat response");

future.complete(null); // 完成 RequestFuture 对象，表示成功完成了心跳请求。

} else if (error == Errors.COORDINATOR\_NOT\_AVAILABLE

|| error == Errors.NOT\_COORDINATOR) { // 表示协调器不可用或者不是协调器。

log.info("Attempt to heartbeat failed since coordinator {} is either not started or not valid", coordinator());

markCoordinatorUnknown(error); // 标记协调器状态为未知，并传入错误类型进行标记。

future.raise(error); // 抛出 RequestFuture 异常，用于表示心跳请求失败。

} else if (error == Errors.REBALANCE\_IN\_PROGRESS) {

// 表示正在重新平衡中

// 对 AbstractCoordinator 对象进行同步操作。

synchronized (AbstractCoordinator.this) {

if (state == MemberState.STABLE) { // 如果状态为 MemberState.STABLE，表示成员状态已稳定。

requestRejoin("group is already rebalancing"); // 发送重新加入群组的请求，并传入一个提示信息。

future.raise(error); // 抛出 RequestFuture 异常，用于表示心跳请求失败。

} else { // 否则，表示在其他状态下，忽略错误并完成 RequestFuture。

log.debug("Ignoring heartbeat response with error {} during {} state", error, state);

future.complete(null); // 完成 RequestFuture 对象，表示成功完成了心跳请求。

}

}

} else if (error == Errors.ILLEGAL\_GENERATION ||

error == Errors.UNKNOWN\_MEMBER\_ID ||

error == Errors.FENCED\_INSTANCE\_ID) {

if (generationUnchanged()) {

log.info("Attempt to heartbeat with {} and group instance id {} failed due to {}, resetting generation",

sentGeneration, rebalanceConfig.groupInstanceId, error);

resetGenerationOnResponseError(ApiKeys.HEARTBEAT, error);

future.raise(error);

} else {

// if the generation has changed, then ignore this error

log.info("Attempt to heartbeat with stale {} and group instance id {} failed due to {}, ignoring the error",

sentGeneration, rebalanceConfig.groupInstanceId, error);

future.complete(null);

}

} else if (error == Errors.GROUP\_AUTHORIZATION\_FAILED) {

future.raise(GroupAuthorizationException.forGroupId(rebalanceConfig.groupId));

} else {

future.raise(new KafkaException("Unexpected error in heartbeat response: " + error.message()));

}

}

}

可以看到该方法会处理各种返回码，其中对 [Errors.REBALANCE\_IN\_PROGRESS](http://errors.rebalance_in_progress/) 的处理是调用 [AbstractCoordinator#requestRejoin()](http://abstractcoordinator/#requestRejoin\(\)) 重置标识位 [rejoinNeeded](http://%20rejoinneeded/) 为 true，则消费者下次进行拉取消息的动作时会触发重新加入消费者组的流程，从而完成消费者组的重平衡。

至此，「**消费者心跳处理**」的源码流程就剖析完了。

##   
**2.3 消费者拉取消息**

当 「**消费者组协调者定位**」、「**消费者加入消费者组**」以及「**消费者心跳处理**」剖析完后，我们来剖析今天的重点「**消费者拉取消息**」的源码流程。

##   
**KafkaConsumer#pollForFetches()**

经过以上流程，此时消费者已经知道自己负责消费的 topic 分区了，然后调用 [KafkaConsumer#pollForFetches()](http://kafkaconsumer/#pollForFetches\(\))拉取消息，源码如下：

private Map<TopicPartition, List<ConsumerRecord<K, V>>> pollForFetches(Timer timer) {

// 计算 poll 的等待时间

long pollTimeout \= coordinator == null ? timer.remainingMs() :

Math.min(coordinator.timeToNextPoll(timer.currentTimeMs()), timer.remainingMs());

// 1、首先调用 Fetcher#fetchedRecords() 从缓存中获取分区的消息的集合，如果不为空则直接返回

final Map<TopicPartition, List<ConsumerRecord<K, V>>> records = fetcher.fetchedRecords();

if (!records.isEmpty()) {

return records;

}

// 2、调用 Fetcher#sendFetches() 生成一个新的 Fetch 异步请求，准备拉取请求

fetcher.sendFetches();

// 如果无法获取所有消费者订阅的分区的拉取位置并且 poll 的等待时间超过 retryBackoffMs，则将等待时间设置为 retryBackoffMs

// 注意：使用 cachedSubscriptionHashAllFetchPositions 意味着在调用此方法之前必须调用 updateAssignmentMetadataIfNeeded 方法

if (!cachedSubscriptionHashAllFetchPositions && pollTimeout > retryBackoffMs) {

pollTimeout = retryBackoffMs;

}

log.trace("Polling for fetches with timeout {}", pollTimeout);

Timer pollTimer \= time.timer(pollTimeout);

// 3、调用 ConsumerNetworkClient#poll() 发送拉取消息的请求，并将底层响应通过回调处理器传递到上层

client.poll(pollTimer, () -> {

// 因为获取消息可能被后台线程完成了，为了提升业务线程的效率，就不应该阻塞没有必要的poll()

return !fetcher.hasAvailableFetches();

});

timer.update(pollTimer.currentTimeMs());

// 4、调用 Fetcher#fetchedRecords() 再次尝试从缓存中获取消息，返回消息记录

return fetcher.fetchedRecords();

}

核心步骤如下：

1.  首先计算本次拉取的超时时间，其计算逻辑如下：
2.  如果协调器为空，则返回当前定时器剩余时间即可。
3.  如果协调器不为空，其逻辑较为复杂，为下面返回的超时间与当前定时器剩余时间相比取最小值。
4.  如果不开启自动提交位移并且未加入消费组，则超时时间为Long.MAX\_VALUE。
5.  如果不开启自动提交位移并且已加入消费组，则返回距离下一次发送心跳包还剩多少时间。
6.  如果开启自动提交位移，则返回 距离下一次自动提交位移所需时间 与 距离下一次发送心跳包所需时间 之间的最小值。
7.  接着调用 [Fetcher#fetchedRecords()](http://fetcher/#fetchedRecords\(\)) 从缓存中获取分区的消息的集合，也就是说，消费者消费消息的时候，并不是直接从Broker 拉取，而是从消费者的缓存中拉取的。如果不为空则直接返回。
8.  接着调用 [Fetcher#sendFetches()](http://fetcher/#sendFetches\(\)) 生成一个新的 Fetch 异步请求，准备拉取请求。
9.  接着调用 [ConsumerNetworkClient#poll()](http://consumernetworkclient/#poll\(\)) 发送拉取消息的请求，并将底层响应通过回调处理器传递到上层。同时还判断如果缓存里有消息就调用不阻塞的 poll() 方法，否则调用有阻塞时间的 poll() 方法。如果有响应，消息会保存在消费者的缓存中。
10.  最后调用 [Fetcher#fetchedRecords()](http://fetcher/#fetchedRecords\(\)) 再次尝试从缓存中拉取数据。因为此时消费者的缓存可能已经有消息了。

![](https://article-images.zsxq.com/lhQRVDn1OVWpvSsv2xLKbhYcNT-0)

为什么消费者不是直接从 Broker 拉取消息，而是先把消息拉取过来放入缓存再等着获取呢？

  
![](https://article-images.zsxq.com/FnkrX_MlKwqgDr60tDqPcSpRkvoa)

综上，「**拉取消息任务**」和「**网络 I/O 任务**」是解耦的，「**网络 I/O 任务**」会事先把消息拉取到消费者缓存里，然后等待拉取消息任务读取缓存里的消息。**当拉取消息任务拉取消息的时候不会造成 I/O 阻塞，从而提高了拉取消息任务的效率**。

我们先来了解下 Fetcher 组件类的实现。

## **Fetch 类详解**

Fetcher 组件封装消息拉取的方法，可以看成是消息拉取的门面类。

## **Fetch 类图关系**

![](https://article-images.zsxq.com/FodZagT3rW0AzPhCSWWMInju1qft)

我们首先一一介绍一下 Fetcher 的核心属性与拉取方法。

1.  ConsumerNetworkClient client 消费端网络客户端，Kafka 负责网络通讯实现类。
2.  int minBytes 一次消息拉取需要拉取的最小字节数，如果不组，会阻塞，默认值为1字节，如果增大这个值会增大吞吐，但会增加延迟，可以通参数 fetch.min.bytes 改变其默认值。
3.  int maxBytes 一次消息拉取允许拉取的最大字节数，但这不是绝对的，如果一个分区的第一批记录超过了该值，也会返回。默认为50M,可通过参数 fetch.max.bytes 改变其默认值。同时不能超过 broker的配置参数(message.max.bytes) 和 主题级别的配置(max.message.bytes)。
4.  int maxWaitMs 在 broker 如果符合拉取条件的数据小于 minBytes 时阻塞的时间，默认为 500ms ，可通属性 fetch.max.wait.ms 进行定制。
5.  int fetchSize 每一个分区返回的最大消息字节数，如果分区中的第一批消息大于 fetchSize 也会返回。
6.  long retryBackoffMs 失败重试后需要阻塞的时间，默认为 100 ms，可通过参数 retry.backoff.ms 定制。
7.  long requestTimeoutMs 客户端向 broker 发送请求最大的超时时间，默认为 30s，可以通过 request.timeout.ms 参数定制。
8.  int maxPollRecords 单次拉取返回的最大记录数，默认值 500，可通过参数 max.poll.records 进行定制。
9.  boolean checkCrcs 是否检查消息的 crcs 校验和，默认为 true，可通过参数 check.crcs 进行定制。
10.  Metadata metadata 元数据。
11.  FetchManagerMetrics sensors 消息拉取的统计服务类。
12.  SubscriptionState subscriptions 订阅信息状态。
13.  ConcurrentLinkedQueue< CompletedFetch> completedFetches 已完成的 Fetch 的请求结果，待消费端从中取出数据。
14.  Deserializer< K> keyDeserializer key 的反序列化器。
15.  Deserializer< V> valueDeserializer value 的饭序列化器。
16.  IsolationLevel isolationLevel Kafka的隔离级别（与事务消息相关），后续在研究其事务相关时再进行探讨。
17.  Map<Integer, FetchSessionHandler> sessionHandlers 拉取会话监听器。

## **FetchRequest 请求的准备工作**

接下来重点剖析这三步的方法，先来看第一个「**从缓存队列中获取消息记录**」，源码如下：

public synchronized int sendFetches() {

// Update metrics in case there was an assignment change

sensors.maybeUpdateAssignment(subscriptions);

// 1、调用 Fetcher#prepareFetchRequests() 方法封装 node 节点对应的 fetch 请求的 map。

Map<Node, FetchSessionHandler.FetchRequestData> fetchRequestMap = prepareFetchRequests();

// 遍历列表 按 node 节点分别做发送请求的准备工作。

for (Map.Entry<Node, FetchSessionHandler.FetchRequestData> entry : fetchRequestMap.entrySet()) {

// 获取fetch目标节点

final Node fetchTarget \= entry.getKey();

// 获取fetch请求数据

final FetchSessionHandler.FetchRequestData data \= entry.getValue();

// 构建 fetch 请求

final FetchRequest.Builder request \= FetchRequest.Builder

.forConsumer(this.maxWaitMs, this.minBytes, data.toSend())

.isolationLevel(isolationLevel)

.setMaxBytes(this.maxBytes)

.metadata(data.metadata())

.toForget(data.toForget())

.rackId(clientRackId);

if (log.isDebugEnabled()) {

log.debug("Sending {} {} to broker {}", isolationLevel, data.toString(), fetchTarget);

}

// 2、调用 ConsumerNetworkClient#send() 方法把请求放入 ConsumerNetworkClient 的缓存中

RequestFuture<ClientResponse> future = client.send(fetchTarget, request);

// 3、设置响应的回调处理将服务端返回的数据通过 completedFetches.add() 缓存到队列中

// 在添加 listener 之前，将节点添加到具有待处理 fetch 请求的节点集中

// 因为 future 可能在其他线程上被完成（例如，在心跳线程处理断开连接时），这将导致listener 同步触发

this.nodesWithPendingFetchRequests.add(entry.getKey().id());

// 4、配置响应的监听器。

future.addListener(new RequestFutureListener<ClientResponse>() {

@Override

public void onSuccess(ClientResponse resp) {

synchronized (Fetcher.this) {

try {

// 处理fetch响应

FetchResponse response \= (FetchResponse) resp.responseBody();

FetchSessionHandler handler \= sessionHandler(fetchTarget.id());

if (handler == null) {

log.error("Unable to find FetchSessionHandler for node {}. Ignoring fetch response.",

fetchTarget.id());

return;

}

if (!handler.handleResponse(response)) {

return;

}

// 提取响应中的分区数据，并处理每个分区

Set<TopicPartition> partitions = new HashSet<>(response.responseData().keySet());

FetchResponseMetricAggregator metricAggregator \= new FetchResponseMetricAggregator(sensors, partitions);

for (Map.Entry<TopicPartition, FetchResponseData.PartitionData> entry : response.responseData().entrySet()) {

TopicPartition partition \= entry.getKey();

FetchRequest.PartitionData requestData \= data.sessionPartitions().get(partition);

if (requestData == null) {

String message;

if (data.metadata().isFull()) {

message = MessageFormatter.arrayFormat(

"Response for missing full request partition: partition={}; metadata={}",

new Object\[\]{partition, data.metadata()}).getMessage();

} else {

message = MessageFormatter.arrayFormat(

"Response for missing session request partition: partition={}; metadata={}; toSend={}; toForget={}",

new Object\[\]{partition, data.metadata(), data.toSend(), data.toForget()}).getMessage();

}

// 收到了缺失的分区的fetch响应

throw new IllegalStateException(message);

} else {

long fetchOffset \= requestData.fetchOffset;

FetchResponseData.PartitionData partitionData \= entry.getValue();

log.debug("Fetch {} at offset {} for partition {} returned fetch data {}",isolationLevel, fetchOffset, partition, partitionData);

Iterator<? extends RecordBatch\> batches = FetchResponse.recordsOrFail(partitionData).batches().iterator();

short responseVersion \= resp.requestHeader().apiVersion();

// 将完成的fetch保存到completedFetches集合中

completedFetches.add(new CompletedFetch(partition, partitionData,metricAggregator, batches, fetchOffset, responseVersion));

}

}

// 记录fetch请求的latency

sensors.fetchLatency.record(resp.requestLatencyMs());

} finally {

// 在处理完成后，从节点集合中移除该节点

nodesWithPendingFetchRequests.remove(fetchTarget.id());

}

}

}

@Override

public void onFailure(RuntimeException e) {

synchronized (Fetcher.this) {

try {

FetchSessionHandler handler \= sessionHandler(fetchTarget.id());

if (handler != null) {

// 处理fetch出错的情况

handler.handleError(e);

}

} finally {

// 在处理完成后，从节点集合中移除该节点

nodesWithPendingFetchRequests.remove(fetchTarget.id());

}

}

}

});

}

// 返回发送的fetch请求的数量

return fetchRequestMap.size();

}

核心步骤：

1.  调用 [Fetcher#prepareFetchRequests()](http://fetcher/#prepareFetchRequests\(\)) 方法封装 node 节点对应的 fetch 请求的 map。
2.  调用 [ConsumerNetworkClient#send()](http://consumernetworkclient/#send\(\)) 方法把请求放入 ConsumerNetworkClient 的缓存中。
3.  设置响应的回调处理将服务端返回的数据通过 [completedFetches.add](http://completedfetches.add/)() 缓存到队列中，等待消费者从completedFetches集合拉取。

## **发送拉取消息的请求 client#poll()**

client.poll(pollTimer, () -> {

// 因为获取消息可能被后台线程完成了，为了提升业务线程的效率，就不应该阻塞没有必要的 poll()。

return !fetcher.hasAvailableFetches();

});

可以看到这是个匿名方法：[() -> { return !fetcher.hasAvailableFetches(); }](http://\(\)%20-%20{%20%20%20%20%20return%20!fetcher.hasavailablefetches\(\);%20}/)。

当缓存中的消息对应的分区都不能获取时，[Selector.poll](http://selector.poll/)() 方法就不会阻塞，因为消费者都无法获取所有分区的消息那么拉取也就没意义了。分区是否能拉取是由 [SubscriptionState](http://subscriptionstate/) 类里的属性控制的，后面单独剖析。

## **Fetcher#prepareFetchRequests()**

这里先来剖析下「**确定消费者应该发送请求的目标节点**」的方法，源码如下：

private Map<Node, FetchSessionHandler.FetchRequestData> prepareFetchRequests() {

// 定义一个有序的 Map，按照添加的顺序依次发送到指定的节点

Map<Node, FetchSessionHandler.Builder> fetchable = new LinkedHashMap<>();

// 验证分配给该 ConsumerGroup 的每个分区的元数据是否发生了变化

validatePositionsOnMetadataChange();

// 获取当前时间，用于后面选择读取副本时判断优先级

long currentTimeMs \= time.milliseconds();

// 遍历可以拉取数据的分区

for (TopicPartition partition : fetchablePartitions()) {

// 获取该分区的最新读取位置

FetchPosition position \= this.subscriptions.position(partition);

if (position == null) {

throw new IllegalStateException("Missing position for fetchable partition " + partition);

}

// 获取该分区当前的 Leader，如果没有 Leader，则请求更新元数据信息

Optional<Node> leaderOpt = position.currentLeader.leader;

if (!leaderOpt.isPresent()) {

log.debug("Requesting metadata update for partition {} since the position {} is missing the current leader node", partition, position);

metadata.requestUpdate();

continue;

}

// 选择该分区的读取副本节点

Node node \= selectReadReplica(partition, leaderOpt.get(), currentTimeMs);

// 如果该节点不可用，则会进行权限认证失败重试

if (client.isUnavailable(node)) {

client.maybeThrowAuthFailure(node);

log.trace("Skipping fetch for partition {} because node {} is awaiting reconnect backoff", partition, node);

} else if (this.nodesWithPendingFetchRequests.contains(node.id())) {

// 如果指定的节点有尚未处理的 Fetch 请求，则不再发送该请求

log.trace("Skipping fetch for partition {} because previous request to {} has not been processed", partition, node);

} else {

// 如果指定节点有 Leader，又没有正在进行的 Fetch 请求，则需要构建新的 Fetch 请求

FetchSessionHandler.Builder builder \= fetchable.get(node);

if (builder == null) {

int id \= node.id();

FetchSessionHandler handler \= sessionHandler(id);

// 如果还没有创建与该节点的Fetch会话处理器，先创建

if (handler == null) {

handler = new FetchSessionHandler(logContext, id);

sessionHandlers.put(id, handler);

}

// 为该节点新建Fetch请求构建器

builder = handler.newBuilder();

// 将节点及其对应的Fetch请求构建器存入fetchable Map中

fetchable.put(node, builder);

}

// 向Fetch请求构建器中添加该分区的拉取请求

builder.add(partition, new FetchRequest.PartitionData(position.offset,

FetchRequest.INVALID\_LOG\_START\_OFFSET, this.fetchSize,

position.currentLeader.epoch, Optional.empty()));

log.debug("Added {} fetch request for partition {} at position {} to node {}", isolationLevel,

partition, position, node);

}

}

// 将fetchable Map中的fetch请求数据一一构建出来放入返回的Map中

Map<Node, FetchSessionHandler.FetchRequestData> reqs = new LinkedHashMap<>();

for (Map.Entry<Node, FetchSessionHandler.Builder> entry : fetchable.entrySet()) {

reqs.put(entry.getKey(), entry.getValue().build());

}

return reqs;

}

简单来说，该方法会根据分配给当前消费者的分区信息以及集群元数据，来确定消费者应该发送请求的目标节点。

从每个节点获取每个分区数据的请求构建。其中还包括选择读取副本，验证分配给该 ConsumerGroup 的每个分区的元数据是否发生了变化等。

当元数据信息更新时，验证分配给该 ConsumerGroup 的每个分区的读取位置是否正确，源码如下：

/\*\*

\* 当元数据信息更新时，验证分配给该ConsumerGroup的每个分区的读取位置是否正确

\*/

private void validatePositionsOnMetadataChange() {

// 获取最新的元数据版本

int newMetadataUpdateVersion \= metadata.updateVersion();

// 如果元数据版本更新，则需要重新验证每个分区的读取位置

if (metadataUpdateVersion.getAndSet(newMetadataUpdateVersion) != newMetadataUpdateVersion) {

// 遍历ConsumerGroup分配的每个分区

subscriptions.assignedPartitions().forEach(topicPartition -> {

// 获取该分区当前的Leader和Epoch

ConsumerMetadata.LeaderAndEpoch leaderAndEpoch \= metadata.currentLeader(topicPartition);

// 如果当前的Leader已经无法处理该分区的数据，则需要切换到新的Leader

subscriptions.maybeValidatePositionForCurrentLeader(apiVersions, topicPartition, leaderAndEpoch);

});

}

}

如果当前分区可以拉取数据，尝试选择要读取的副本节点，源码如下：

/\*\*

\* 选择读取副本节点（如果设置了优先读取副本），否则返回Leader副本节点

\*/

Node selectReadReplica(TopicPartition partition, Node leaderReplica, long currentTimeMs) {

// 获取该分区设置的首选读取副本节点ID（如果设置了）

Optional nodeId \= subscriptions.preferredReadReplica(partition, currentTimeMs);

// 如果分区设置了首选读取副本，则如果该副本在线且存在于元数据中，选择首选副本作为读取节点

if (nodeId.isPresent()) {

Optional node \= nodeId.flatMap(id -> metadata.fetch().nodeIfOnline(partition, id));

if (node.isPresent()) {

return node.get();

} else {

// 如果首选副本不在线或不在元数据中，则清除首选副本的设置，改为使用Leader副本

log.trace("Not fetching from {} for partition {} since it is marked offline or is missing from our metadata," +

" using the leader instead.", nodeId, partition);

subscriptions.clearPreferredReadReplica(partition);

return leaderReplica;

}

} else {

// 如果没有设置首选副本，则使用Leader副本作为读取节点

return leaderReplica;

}

}

最后回到本小节 [KafkaConsumer#pollForFetches()](http://kafkaconsumer/#pollForFetches\(\)%20) 方法的最后一步的方法 [Fetcher#fetchedRecords()](http://fetcher/#fetchedRecords\(\)%20) 获取消息记录，源码如下：

/\*\*

\* 获取已获取的消费记录（按分区存储）

\*/

public Map<TopicPartition, List<ConsumerRecord<K, V>>> fetchedRecords() {

// 1、构建 map 类型的变量 fetched，map 里保存了分区和分区对应的消费到的消息列表，该方法的返回值就是fetched。

Map<TopicPartition, List<ConsumerRecord<K, V>>> fetched = new HashMap<>();

// 存储暂停的已完成拉取任务

Queue<CompletedFetch> pausedCompletedFetches = new ArrayDeque<>();

// 2、定义变量 recordsRemaining，表示一次获取多少个消息，也就是 fetched 变量里的消息数量。这个参数可以通过配置文件进行修改，对应配置文件的参数是 max.poll.records，默认值是 500。

int recordsRemaining \= maxPollRecords;

try {

// 3、通过 while 不断从缓存中获取消息，直到获取的消息为 recordsRemaining 设定的消息量为止。

while (recordsRemaining > 0) {

// 4、判断当前获取消息拉取任务的 nextInLineFetch 是否为空，如果为空则需要从 completedFetches 集合获取消息。nextInLineFetch 是一个 CompletedFetch 类型，指的是一个分区的消息集合。

if (nextInLineFetch == null || nextInLineFetch.isConsumed) {

// 5、判断 completedFetches 集合是否为空，如果为空说明缓存里没有消息直接返回空的集合。如果completedFetches 集合不为空就把集合中一个类型为 CompletedFetch 的元素给 nextInLineFetch。

CompletedFetch records \= completedFetches.peek();

if (records == null) break;

// 如果待处理的拉取任务未初始化，则初始化该任务

if (records.notInitialized()) {

try {

// 调用 Fetcher#initializeCompletedFetch() 方法将服务端返回的数据初始化为一个 CompletedFetch 对象，并更新本地缓存的元数据中 topic 分区偏移量 offset 信息

nextInLineFetch = initializeCompletedFetch(records);

} catch (Exception e) {

FetchResponseData.PartitionData partition \= records.partitionData;

// 在遇到异常的情况下，如果该任务没有记录并且之前没有其他有内容的拉取任务记录，则从已完成的拉取任务队列中移除该任务。

if (fetched.isEmpty() && FetchResponse.recordsOrFail(partition).sizeInBytes() == 0) {

completedFetches.poll();

}

throw e;

}

} else {

nextInLineFetch = records;

}

// 从已完成的拉取任务队列中移除该任务

completedFetches.poll();

} else if (subscriptions.isPaused(nextInLineFetch.partition)) {

// 如果分区已暂停，则将拉取的记录放回已完成的拉取任务队列，以便在后续的轮询中重新返回这些记录

log.debug("Skipping fetching records for assigned partition {} because it is paused", nextInLineFetch.partition);

pausedCompletedFetches.add(nextInLineFetch);

nextInLineFetch = null;

} else {

// 6、如果 nextInLineFetch 不为空，就从 nextInLineFetch 取出消息

// 调用 Fetcher#fetchRecords() 方法从分区缓存中获取指定数量的消息

List<ConsumerRecord<K, V>> records = fetchRecords(nextInLineFetch, recordsRemaining);

// 7、把获取的消息进行封装

if (!records.isEmpty()) {

TopicPartition partition \= nextInLineFetch.partition;

List<ConsumerRecord<K, V>> currentRecords = fetched.get(partition);

if (currentRecords == null) {

fetched.put(partition, records);

} else {

// 处理极少出现的情况：同一分区同时存在多个拉取任务，将新拉取的记录加入到已有的记录列表中

List<ConsumerRecord<K, V>> newRecords = new ArrayList<>(records.size() + currentRecords.size());

newRecords.addAll(currentRecords);

newRecords.addAll(records);

fetched.put(partition, newRecords);

}

// 更新剩余可拉取的记录数

recordsRemaining -= records.size();

}

}

}

} catch (KafkaException e) {

// 如果没有获取到有效的消费记录，只抛出KafkaException异常

if (fetched.isEmpty())

throw e;

} finally {

// 将暂停分区的已拉取完成任务重新添加到已完成的拉取任务队列中，以便在下一次轮询时重新评估

completedFetches.addAll(pausedCompletedFetches);

}

// 8、返回 fetched 集合

return fetched;

}

这里如果没有拉取完，则会一直拉取，核心步骤如下：

1.  构建 map 类型的变量 fetched，map 里保存了分区和分区对应的消费到的消息列表，该方法的返回值就是fetched。
2.  定义变量 recordsRemaining，表示一次获取多少个消息，也就是 fetched 变量里的消息数量。这个参数可以通过配置文件进行修改，对应配置文件的参数是 [max.poll.records](http://max.poll.records/)，默认值是 500。
3.  通过 while 不断从缓存中获取消息，直到获取的消息为 recordsRemaining 设定的消息量为止。
4.  判断当前获取消息拉取任务的 nextInLineFetch 是否为空，如果为空则需要从 completedFetches 集合获取消息。nextInLineFetch 是一个 CompletedFetch 类型，指的是一个分区的消息集合。
5.  判断 completedFetches 集合是否为空，如果为空说明缓存里没有消息直接返回空的集合。如果 completedFetches 集合不为空就把集合中一个类型为 CompletedFetch 的元素给 nextInLineFetch。
6.  如果 nextInLineFetch 不为空，就从 nextInLineFetch 取出消息。
7.  把获取的消息进行封装。
8.  返回 fetched 集合。

![](https://article-images.zsxq.com/libn-Z8IzRTH1-MyN7d6X_XgM0uO)

至此，「**消费者拉取消息**」的源码流程就剖析完了。

最后通过一张时序图来总结下整个「**消费者拉取消息**」的流程：

![](https://article-images.zsxq.com/FmlxKM_HVrJOG_2JOqMB-CwMaNmb)

##   
**03 总结**

这里，我们一起来总结一下这篇文章的重点。

1、通过「**场景驱动**」的方式从消费者调用出发，抛出消费者初始化后是如何拉取数据的?

2、带你剖析了「**消费者拉取消息**」的整个流程，从「**消费者拉取消息的入口**」、「**消费者组协调者定位**」、「**消费者加入消费者组**」、「**消费者心跳处理**」、「**消费者拉取消息**」五个维度进行剖析。

3、最后通过了一张「**消费者拉取消息**」的时序图来总结整体流程。

下篇我们来深度剖析「**初识消费者组**」，大家期待，我们下期见。