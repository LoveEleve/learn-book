大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了「**Kafka Coordinator 工作原理**」，了解了消费者组协调器 ConsumserCoordinator 和服务端 GroupCoordinator 是配合起来使用的，其目的是管理消费者，主要包括「**管理消费者上下线**」、「**分发消费分区方案**」等等。今天我们开启消费端源码的征程，这是第九篇来深度聊聊「**消费者重平衡机制流程**」，看看 Kafka 消费者重平衡机制是如何实现的。

![](https://article-images.zsxq.com/FkqB3p1ePtOAsBySYNG15MtkQWIQ)

## **01 总体概述**

在 [【原理分析系列第十六篇】图解 Kafka Rebalance 流程分析及如何避免](https://articles.zsxq.com/id_n9361w3go1b3.html) 这篇中，我们专门来剖析 kafka

中 Rebalance 的「**发生场景**」、「**流程分析**」以及「**如何避免**」，今天我们从源码的角度来重温整个关键操作，只有理解了其内部实现，才能真正掌握它并在生产环境出现问题后更好的解决。

这里再来剖析下 **Rebalance 的触发条件有三种：**

1.  当 Consumer Group 组成员数量发生变化(主动加入或者主动离组，故障下线等)。
2.  当订阅主题数量发生变化。
3.  当订阅主题的分区数发生变化。

而引发 **Rebalance** 主要有以下 5 个场景：

1.  当有新的消费者加入消费组时。
2.  当有消费者宕机下线时，此时消费者并不一定需要真正下线，例如遇到「**长时间 GC**」、「**网络延迟**」等问题导致消费者长时间没有向 [GroupCoordinator](http://groupcoordinator/) 发送心跳等情况时，[GroupCoordinator](http://groupcoordinator/) 就会认为消费者已经下线。
3.  当有消费者主动退出消费组（发送 LeaveGroupRequest 请求）时。比如：此时客户端调用子 [unsubscrible()](http://unsubscrible\(\)%20/) 方法取消对某些主题的订阅。
4.  当消费组所对应的 [GroupCoorinator](http://groupcoorinator/) 节点发生了变更时。
5.  当消费组内所订阅的任何一个主题或主题分区数量发生了变化时。

## **02 消费者组重平衡源码剖析**

消费者组重平衡总共分为三个阶段：

![](https://article-images.zsxq.com/FtQtXu5s47wqSZTEEXLDpM3OEcF_)

这三个阶段的请求在 [【消费者源码分析系列第三篇】图解 Kafka 源码之消费者如何拉取数据的](https://articles.zsxq.com/id_to39nipv3p2g.html) 以及 [【消费者源码分析系列第四篇】图解 Kafka 源码之初识消费者组及四大请求处理流程](https://articles.zsxq.com/id_b52hrvfqrzp7.html) 这两篇中大体剖析过了，不过没有剖析服务端的处理，今天我们再从客户端发送请求到服务端处理请求整个流程进行源码深度剖析。

## **2.1 发送 FindCooridnator 请求并处理**

当「**确定消费者协调者完成**」后，会发送第一个请求 **FIND\_COORDINATOR**，如果通信一次发现该[GroupCoordinator](http://groupcoordinator/) 的信息还未获取到则继续重试，直到超时。这里的超时时间即为 poll 时传入的超时时间，这个时间会贯穿整个 consumer 的运行周期，源码如下：

protected synchronized boolean ensureCoordinatorReady(final Timer timer) {

// 1、如果还未获取到 Coordinator 需要先与其进行通信

if (!coordinatorUnknown())

return true;

do {

if (fatalFindCoordinatorException != null) {

final RuntimeException fatalException \= fatalFindCoordinatorException;

fatalFindCoordinatorException = null;

throw fatalException;

}

// 2、调用 AbstractCoordinator#lookupCoordinator() 方法生成异步请求，并将其存入到请求队列中，即将请求存入 ConsumerNetworkClient#unsent 队列中，此处设置请求类型 KafkaApis.FIND\_COORDINATOR

final RequestFuture<Void> future = lookupCoordinator();

// 3、调用 ConsumerNetworkClient#poll() 实际发起网络请求，并监听服务端的响应，如异步请求完成则退出循环。此时获取到每一个请求都绑定 SelectionKey.OP\_WRITE 事件

client.poll(future, timer);

// 4、如果还没回调完成则说明是超时的

if (!future.isDone()) {

// ran out of time

break;

}

RuntimeException fatalException \= null;

// 4、异常处理

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

// 5、不断尝试获取 GroupCoordinator 直到与 GroupCoordinator 通信成功或超时时跳出循环。

} while (coordinatorUnknown() && timer.notExpired());

return !coordinatorUnknown();

}

这里我们分步骤剖析一下。

1\. 判断消费者获取的 [GroupCoordinator](http://groupcoordinator/) 是否可用，如果还未获取到 [GroupCoordinator](http://groupcoordinator/) 需要先与其进行通信，源码如下：

public boolean coordinatorUnknown() {

return checkAndGetCoordinator() == null;

}

protected synchronized Node checkAndGetCoordinator() {

// coordinator 不为空且消费者与 GroupCoordinator 连接是否正常。

if (coordinator != null && client.isUnavailable(coordinator)) {

markCoordinatorUnknown(true);

return null;

}

return this.coordinator;

}

从上可以得出 [coordinatorUnknown()](http://coordinatorunknown\(\)/) 方法会调用 [checkAndGetCoordinator()](http://checkandgetcoordinator\(\)/) 方法来检测并获取协调者信息。 通过代码跟踪你可以仔细思考一下，这里的 [coordinator](http://coordinator/) 是指获取到的协调器节点对象，[client.isUnavailable(coordinator)](http://client.isunavailable\(coordinator\)/) 是在与协调器建立连接，每次判断 [coordinator](http://coordinator/) 不为空且 client 与协调器连接失败，则将 [coordinator](http://coordinator/) 置空，**为什么会这样设计呢？**很有可能是请求发送到协调器的信息时发现该节点已下线或者不可用，此时服务端很有可能在进行选举，所以我们需要将 [coordinator](http://coordinator/) 清空，待服务端选举完成后再次通信。

2\. 如果 [GroupCoordinator](http://groupcoordinator/) 不存在，调用查找 [GroupCoordinator](http://groupcoordinator/) 的请求预发送方法 [AbstractCoordinator#lookupCoordinator()](http://abstractcoordinator/#lookupCoordinator\(\)) 生成异步请求来「**寻找消费者协调者**」，并将其存入到请求队列中，即将请求存入 [ConsumerNetworkClient#unsent](http://consumernetworkclient/#unsent) 队列中，此处设置请求类型 [KafkaApis.FIND\_COORDINATOR](http://kafkaapis.find_coordinator/)，最后把请求放入 [NetworkClient#send](http://networkclient/#send) 缓存字段，等待真正的网络发送。

3\. 调用 [ConsumerNetworkClient#poll()](http://consumernetworkclient/#poll\(\)) 方法，同时传入 future 和定时器，调用的方法如下：

public boolean poll(RequestFuture<?> future, Timer timer) {

do {

// ConsumerNetworkClient 会不断调用底层组件 NetworkClient 把 FindCoordinatorRequest 请求发送出去，直到发送成功或超时。

poll(timer, future);

} while (!future.isDone() && timer.notExpired());

return future.isDone();

}

public void poll(Timer timer, PollCondition pollCondition) {

poll(timer, pollCondition, false);

}

// 轮询所有可以发送的请求，真正的发送。此时获取到每一个请求都绑定 SelectorKey.OP\_WRITE 事件

public void poll(Timer timer, PollCondition pollCondition, boolean disableWakeup) {

....

lock.lock();

try {

....

// 发送我们现在可以发送的所有请求

long pollDelayMs \= trySend(timer.currentTimeMs());

....

// 再次发送我们现在可以发送的所有请求

trySend(timer.currentTimeMs());

....

} finally {

lock.unlock();

}

....

}

long trySend(long now) {

long pollDelayMs \= maxPollTimeoutMs;

// 遍历 Broker 节点

for (Node node : unsent.nodes()) {

// 获取该节点的所有待发送的请求，存储在 unsent 队列中的请求

Iterator<ClientRequest> iterator = unsent.requestIterator(node);

if (iterator.hasNext())

// 计算最小过期时间

pollDelayMs = Math.min(pollDelayMs, client.pollDelayMs(node, now));

while (iterator.hasNext()) {

ClientRequest request \= iterator.next();

// 当客户端准备好后

if (client.ready(node, now)) {

// 这里的 client 表示 NetworkClient，其中涉及到 Kafka 网络通讯的内容，可以查看之前的文章，会看到内部绑定 SelectionKey.OP\_WRITE 事件

client.send(request, now);

iterator.remove();

} else {

// try next node when current node is not ready

break;

}

}

}

return pollDelayMs;

}

可以看到 [ConsumerNetworkClient](http://consumernetworkclient/) 会不断调用底层 [NetworkClient](http://networkclient/) 组件，把 **FIND\_COORDINATOR** 请求发送出去，直到发送成功或超时，并监听服务端的响应，如异步请求完成则退出循环。

4\. 判断异步请求失败，如果失败就根据失败的原因做以下处理：

1.  如果有异常，另外如果是 [future.isRetriable()](http://future.isretriable\(\)/) 异常意味着可以重试。其处理方式是等待更新元数据后再次发起请求，否则就抛出异常。
2.  如果 [coordinator](http://coordinator/) 不为 null，说明获得了 [GroupCoordinator](http://groupcoordinator/)，但是网络连接有问题，那么则等待一段时间后元数据更新了再重试。

5\. 判断是否超时，如果超时就跳出循环。

这里我们重点剖析下第二步。

// AbstractCoordinator 类方法

protected synchronized RequestFuture<Void> lookupCoordinator() {

if (findCoordinatorFuture == null) {

// 找一个最小负载节点作为询问协调者

Node node \= this.client.leastLoadedNode();

if (node == null) {

log.debug("No broker available to send FindCoordinator request");

return RequestFuture.noBrokersAvailable();

} else {

// 发送 FIND\_COORDINATOR 请求，其实是将该请求放入未发送队列中 unsent

findCoordinatorFuture = sendFindCoordinatorRequest(node);

}

}

return findCoordinatorFuture;

}

该方法会寻找消费者组协调器，其核心步骤如下：

1.  首先调用 [ConsumerNetworkClient#leastLoadedNode()](http://consumernetworkclient/#leastLoadedNode\(\)) 方法从集群元数据中取得一个负载最小的节点，最终是调用 [NetworkClient#leastLoadedNode()](http://consumernetworkclient/#leastLoadedNode\(\)) 方法，所有的集群节点都保存着集群元数据，我们只要选择一个负载最小的节点获取[GroupCoordinator](http://groupcoordinator/) 就好。
2.  调用 [AbstractCoordinator#sendFindCoordinatorRequest()](http://abstractcoordinator/#sendFindCoordinatorRequest\(\)) 方法生成发送给目标节点的异步请求，即预发送查找[GroupCoordinator](http://groupcoordinator/) 的 [FIND\_COORDINATOR](http://find_coordinator/) 请求，其实是将该请求放入未发送队列中 [unsent](http://unsent/)。

该步骤主要是构建 [FIND\_COORDINATOR](http://find_coordinator/) 请求并暂存到 [ConsumerNetworkClient#unsent](http://consumernetworkclient/#unsent%20) 队列中，最终会暂存到 [NetworkClient#send](http://networkclient/#send) 字段中，关于 [NetworkClient](http://networkclient/) 相关的知识，可以点击这里查看：[【生产者源码分析系列第九篇】图解 Kafka 源码之 NetworkClient 网络通信组件架构设计](https://articles.zsxq.com/id_k2pnfv2xq2wb.html)

如下图，展示如何寻找负载最小的节点：

![](https://article-images.zsxq.com/Fhazq4J_au5vbIRaJW7sf4qEVaBU)

整个寻找协调器的过程如下图：

![](https://article-images.zsxq.com/lgJf13a6G_rlmH5s6PfaMl1W0RIW)

private RequestFuture<Void> sendFindCoordinatorRequest(Node node) {

// 设置此次请求的类型：ApiKeys.FIND\_COORDINATOR

FindCoordinatorRequestData data \= new FindCoordinatorRequestData()

.setKeyType(CoordinatorType.GROUP.id())

.setKey(this.rebalanceConfig.groupId);

// 1、构建查找Group Coordinator节点的请求

FindCoordinatorRequest.Builder requestBuilder \= new FindCoordinatorRequest.Builder(data);

// 2、发送请求，并调用 FindCoordinatorResponseHandler 类对象来处理响应

// 将请求存储在 ConsumerNetworkClient#unsent 队列中队列中

return client.send(node, requestBuilder)

// 调用异步请求 RequestFuture#compose() 方法为其添加监听器，并将服务端响应的回调处理器 FindCoordinatorResponseHandler 封装在监听器中。

// 也就是说当消费者收到响应后，通过 RequestFuture#compose()方法设置FindCoordinatorResponseHandler 的类对象进行处理。

.compose(new FindCoordinatorResponseHandler());

}

至此消费者客户端发送「**FindCoordinator 请求**」剖析完了，接下来我们来剖析服务端如何处理 「**FindCoordinator 请求**」，先来看其请求参数数据结构。

## **2.1.1 FindCoordinatorRequest 数据结构**

通过下图我们来看看该请求都发送了哪些数据，其中 key\_type 有两种枚举，一种是 [GROUP](http://group/)，另一种是[TRANSACTION](http://transaction/)，如果 key\_type 为 GROUP 的话那 key 就是 groupId。

![](https://article-images.zsxq.com/FmF-xqHyr2drsCO7KzT6DrxGz7-N)

## **2.1.2 服务端接收 FindCooridnator 请求并处理**

根据请求传递 API 来调用不同接口，[request.header.apiKey](http://request.header.apikey/) 匹配客户端传来的 [FIND\_COORDINATOR](http://find_coordinator/)。

![](https://article-images.zsxq.com/FkoCaHUH5Lf5wywFkNEPmTPK6GWc)

![](https://article-images.zsxq.com/FhKGP3wiaYGQ9nQz0Dmv6IdkHm22)

private def getCoordinator(request: RequestChannel.Request, keyType: Byte, key: String): (Errors, Node) = {

// 授权失败

if (keyType == CoordinatorType.GROUP.id &&

!authHelper.authorize(request.context, DESCRIBE, GROUP, key))

(Errors.GROUP\_AUTHORIZATION\_FAILED, Node.noNode)

else if (keyType == CoordinatorType.TRANSACTION.id &&

!authHelper.authorize(request.context, DESCRIBE, TRANSACTIONAL\_ID, key))

(Errors.TRANSACTIONAL\_ID\_AUTHORIZATION\_FAILED, Node.noNode)

else {

// 匹配 CoordinatorType 类型，要么是 GROUP 要么是 TRANSACTION

val (partition, internalTopicName) = CoordinatorType.forId(keyType) match {

case CoordinatorType.GROUP =>

// 找到对应的协调者

(groupCoordinator.partitionFor(key), GROUP\_METADATA\_TOPIC\_NAME)

case CoordinatorType.TRANSACTION =>

(txnCoordinator.partitionFor(key), TRANSACTION\_STATE\_TOPIC\_NAME)

}

// 获取对应的元数据

val topicMetadata \= metadataCache.getTopicMetadata(Set(internalTopicName), request.context.listenerName)

// 如果没有相关内部 Topic 的信息

if (topicMetadata.headOption.isEmpty) {

val controllerMutationQuota \= quotas.controllerMutation.newPermissiveQuotaFor(request)

// 创建内部 Topic

autoTopicCreationManager.createTopics(Seq(internalTopicName).toSet, controllerMutationQuota, None)

(Errors.COORDINATOR\_NOT\_AVAILABLE, Node.noNode)

} else {

if (topicMetadata.head.errorCode != Errors.NONE.code) {

(Errors.COORDINATOR\_NOT\_AVAILABLE, Node.noNode)

} else {

// 直接获取 coordinatorEndpoint 节点信息

val coordinatorEndpoint \= topicMetadata.head.partitions.asScala

.find(\_.partitionIndex == partition)

.filter(\_.leaderId != MetadataResponse.NO\_LEADER\_ID)

.flatMap(metadata => metadataCache.

getAliveBrokerNode(metadata.leaderId, request.context.listenerName))

coordinatorEndpoint match {

case Some(endpoint) =>

(Errors.NONE, endpoint)

case \_ \=\>

(Errors.COORDINATOR\_NOT\_AVAILABLE, Node.noNode)

}

}

}

}

}

这里核心步骤有两点：

1.  调用 [groupCoordinator.partitionFor(key)](http://groupcoordinator.partitionfor\(key\)/) 来获取对应的 [coordinator](http://coordinator/) 节点。![](https://article-images.zsxq.com/FphPAjDFqZ2ZO9UZ-jeDbsl4dOKa)
2.  我们知道 consumer 消费后对应的 offset 是保存在 kafka 的内部名为 [\_\_consumer\_offsets](http://__consumer_offsets/) 中，内置 topic 初始化时由[offsets.topic.num.partitions](http://offsets.topic.num.partitions/) 参数来决定分区数，默认值是 50，相同 ConsumerGroup 的 offset 最终会保存在其中一个分区中，而保存在哪个分区就由如上代码来决定，可以看到逻辑很简单，就是取 groupId 的 hashCode，然后对总的分区数取模。比如 groupId 为 consume\_group，最终就会在 34 号分区保存 offset。
3.  如果没有创建内部 Topic，则调用 [autoTopicCreationManager.createTopics()](http://autotopiccreationmanager.createtopics\(\)/) 来创建，如果已创建，则调用

[metadataCache.getAliveBrokerNode()](http://metadatacache.getalivebrokernode\(\)/) 获取协调器节点，即某个特定分区 Leader 副本所在的 Broker 被选定为指定消费者组的 [Coordinator](http://coordinator/)。从这段代码也可以猜想 kafka 内置 topic 的创建原理，其实是一种懒加载的思想，只有第一个 consumer 才会创建对应 topicPartition 文件。

关于创建 Topic 的逻辑处理，可以查看 [【服务端 Broker 源码分析系列第二十二篇】图解 Kafka 源码之 Topic 创建请求处理流程](https://articles.zsxq.com/id_2q1nwfkg3fcb.html) 、[【服务端 Broker 源码分析系列第三十八篇】图解 Kafka 源码之 KRaft 数据处理机制](https://articles.zsxq.com/id_xx081g35rpve.html) 这两篇进行学习。

## **2.1.3** **FindCoordinatorResponse 数据结构**

通过下图我们来看看该请求都返回了哪些数据，可以看到前面取了很多数据，最终拼到返回参数里面的只有 Leader所在的节点信息。

![](https://article-images.zsxq.com/Fjhv8FriiatxMecT9JDwWxrs-gW4)

## **2.1.4 处理响应** **FindCoordinatorResponseHandler**

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

// 5、调用 RequestFuture<Void> 上的监听器

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
5.  调用 [RequestFuture<Void>](http://requestfuturevoid/)上的监听器，完成响应事件的传播。

至此「**消费者组协调者定位**」的源码流程就剖析完了，下面开始剖析第二个阶段。

## **2.2 发送 JoinGroupRequest 请求并处理**

在成功找到对应的 [GroupCoordinator](http://groupcoordinator/) 之后，此时就进入了向 [GroupCoordinator](http://groupcoordinator/) 注册阶段。在这个阶段中，消费者会向 [GroupCoordinator](http://groupcoordinator/) 发送 [JoinGroupRequest](http://joingrouprequest/) 请求表示要「**加入消费者组**」，并处理响应。

此时会调用抽象类 [AbstractCoordinator#joinGroupIfNeeded()](http://abstractcoordinator/#joinGroupIfNeeded\(\)) 方法，该方法会在 while 循环中不断进行加入消费者组的尝试，直至成功，源码如下：

boolean joinGroupIfNeeded(final Timer timer) {

// 1、通过循环来确保成功加入消费者组或重新加入消费者组，直到满足条件或超时为止。

while (rejoinNeededOrPending()) {

// 2、首先确保协调器就绪，其目的就是担心与 GroupCoordinator 的连接出现问题，这样可以及时再获取一次，如果协调器不可用，则返回 false。

if (!ensureCoordinatorReady(timer)) {

return false;

}

// 3、加入组之前的准备工作。

if (needsJoinPrepare) {

// 如果需要准备加入群组，则调用 onJoinPrepare 方法

needsJoinPrepare = false;

onJoinPrepare(generation.generationId, generation.memberId);

}

// 4、初始化加入消费组。包括构建 JoinGroupRequest 请求，预发送 JoinGroupRequest 请求存储到 unsent 队列中，异步请求上加监听器，并通过轮询等待 JoinGroup 响应。

final RequestFuture<ByteBuffer> future = initiateJoinGroup();

// 5、调用 ConsumerNetworkClinet#poll() 方法进行网络阻塞发送，轮询处理待发送队列中的所有可发送的请求，这部分在上一节中有分析，不再赘述

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

// 6、如果加入消费者组的异步请求成功完成，则协调器会将当前消费者负责的分区下发过来，消费者调用 ConsumerCoordinator#onJoinComplete() 执行加入群组后的逻辑

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

在 Kafka 的消费者端，当需要加入消费者组时调用该方法，核心步骤如下：

1.  通过循环来确保成功加入消费者组或重新加入消费者组，直到满足条件或超时为止。
2.  首先确保协调器就绪，其目的就是担心与 [GroupCoordinator](http://groupcoordinator/) 的连接出现问题，这样可以及时再获取一次，如果协调器不可用，则返回 false。
3.  加入组之前的准备工作。准备工作包括：
4.  如果是自动提交 offset，则提交 offset。目的是防止已经消费的 partition 的 offset 换了消费者造成重复消费消息。
5.  调用注册在 [SubscriptionState#ConsumerRebalanceListener](http://subscriptionstate/#ConsumerRebalanceListener) 上的回调方法。
6.  初始化加入消费组。包括构建 [JoinGroupRequest](http://joingrouprequest/) 请求，预发送 [JoinGroupRequest](http://joingrouprequest/) 请求，异步请求上加监听器，并通过轮询等待 JoinGroup 响应。
7.  调用 ConsumerNetworkClinet#poll() 方法进行网络阻塞发送，轮询处理待发送队列中的所有可发送的请求，这部分在上一节中有分析，不再赘述。
8.  根据响应结果的不同情况，执行不同的逻辑：
9.  如果 JoinGroup 请求成功，根据当前的 generation 和 state 数据判断是否需要执行加入消费者组后的逻辑，并调用相应的回调方法。
10.  如果 JoinGroup 请求失败，根据异常的类型决定是否继续尝试或抛出异常。
11.  最后如果成功加入消费者组或重新加入消费者组，则返回 true，否则返回 false。

## **2.2.1 初始化加入消费者组 initiateJoinGroup()**

[AbstractCoordinator#initiateJoinGroup()](http://abstractcoordinator/#initiateJoinGroup\(\)) 一个入口方法：生成加入消费者组的JoinGroup 异步请求，并通过轮询等待响应，源码如下：

private synchronized RequestFuture<ByteBuffer> initiateJoinGroup() {

// 为了避免在调用 poll 方法后被用户唤醒而错误地尝试重新加入消费者组，将 joinFuture 存储起来。

if (joinFuture == null) {

// 1、设置 consumer 的状态为预重平衡

state = MemberState.PREPARING\_REBALANCE;

// 2、如果之前的重平衡失败，可能会连续触发重平衡，这种情况下不更新开始时间。

if (lastRebalanceStartMs == -1L)

lastRebalanceStartMs = time.milliseconds();

// 3、开始做发送 JoinGroupRequest 请求的准备工作。

joinFuture = sendJoinGroupRequest();

// 4、给异步请求加监听器

joinFuture.addListener(new RequestFutureListener<ByteBuffer>() {

@Override

public void onSuccess(ByteBuffer value) {

// joinFuture 成功完成时不做任何操作，所有的处理逻辑已经在 SyncGroupResponseHandler 中了

}

@Override

public void onFailure(RuntimeException e) {

// 请求完成后处理失败的情况

// 如果在唤醒后完成了加入消费者组，则忽略异常，否则重新发起加入消费者组

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
2.  设置 MemberState 为 [PREPARING\_REBALANCE](http://preparing_rebalance/)，表示要准备重平衡操作了。
3.  如果上次重新平衡的开始时间为 -1（即没有记录），则将当前时间设置为上次重新平衡的开始时间。
4.  开始做发送 [JoinGroupRequest](http://joingrouprequest/) 请求的准备工作，然后发送 [JoinGroup](http://joingroup/) 请求，并获取返回的 Future。
5.  为该 Future 添加回调监听器，用于处理成功和失败的情况。对于成功没做任何事，因为发送 [JoinGroupRequest](http://joingrouprequest/) 请求的准备工作的回调对象会完成回调处理。如果在唤醒后完成了加入消费者组，则忽略异常，否则重新发起加入消费者组。
6.  返回 joinFuture。

该方法的作用是在加入消费者组时执行必要的准备操作，并返回 「**JoinGroup 请求**」的 Future。在加入消费者组之前，需要执行一些前置操作，例如「**设置状态**」、「**记录开始时间**」、「**准备发送请求**」等。同时为了「**避免重复发送 JoinGroup 请求**」，在方法中使用「**JoinFeture**」进行了缓存，保证只发送一次 「**JoinGroup 请求**」，并通过监听器处理成功和失败的情况。

接下来，我们还需要重点说说做发送 [JoinGroupRequest](http://joingrouprequest/) 请求的准备工作 [sendJoinGroupRequest()](http://sendjoingrouprequest\(\)/) 方法。

## **2.2.2 发送准备工作 sendJoinGroupRequest()**

RequestFuture<ByteBuffer> sendJoinGroupRequest() {

// 1、如果 coordinator 不可用，则返回 RequestFuture，其状态为未知协调器

if (coordinatorUnknown())

return RequestFuture.coordinatorNotAvailable();

log.info("(Re-)joining group");

// 2、构建加入 GroupCoordinator 的请求

JoinGroupRequest.Builder requestBuilder \= new JoinGroupRequest.Builder(

new JoinGroupRequestData()

// 设置消费者组id

.setGroupId(rebalanceConfig.groupId)

// 客户端与 Broker 最大会话有效期, 如果超过这个时间没有任何心跳则可能发生重平衡，属性 session.timeout.ms；默认10000（10 秒）

.setSessionTimeoutMs(this.rebalanceConfig.sessionTimeoutMs)

// 消费者的成员id， 默认就是空字符串

.setMemberId(this.generation.memberId)

// 这是 2.3 版本引入的新参数. 用户提供的消费者实例的唯一标识符。 如果设置了,消费者则被视为静态成员, 静态成员配以较大的 session 超时设置能够避免因成员临时不可用（比如重启）而引发的Rebalance， 如果不设置, 消费者将作为动态成员，其配置：group.instance.id

.setGroupInstanceId(this.rebalanceConfig.groupInstanceId.orElse(null))

// 协议类型, 这里是 consumer, 可选项还有 connect

.setProtocolType(protocolType())

// 配置的分区分配策略和对应的订阅相关信息的

.setProtocols(metadata())

// 设置重平衡超时时间

.setRebalanceTimeoutMs(this.rebalanceConfig.rebalanceTimeoutMs)

);

log.debug("Sending JoinGroup ({}) to coordinator {}", requestBuilder, this.coordinator);

// 由于重平衡超时时间是协调器可能会阻塞的最长时间，因此我们使用重平衡超时时间来覆盖请求超时时间。我们额外加了 5 秒用于解决可能出现的小延迟。

int joinGroupTimeoutMs \= Math.max(client.defaultRequestTimeoutMs(),

rebalanceConfig.rebalanceTimeoutMs + JOIN\_GROUP\_TIMEOUT\_LAPSE);

// 2、发送请求，并设置处理 ClientResponse 的对象 JoinGroupResponseHandler 处理器

return client.send(coordinator, requestBuilder, joinGroupTimeoutMs)

.compose(new JoinGroupResponseHandler(generation));

}

该方法用于向协调器准备发送 [JoinGroupRequest](http://joingrouprequest/) 请求，并返回一个 RequestFuture，用于处理响应结果。核心步骤如下：

1.  首先通过 [coordinatorUnknown()](http://coordinatorunknown\(\)/) 方法检查 coordinator 是否可用，如果是，则返回一个 RequestFuture，其状态为未知协调器。
2.  构造一个 [JoinGroupRequest](http://joingrouprequest/) 的建造者对象，设置 groupId、sessionTimeoutMs、memberId、groupInstanceId、protocolType、protocols 和 rebalanceTimeoutMs 等参数。
3.  计算请求的超时时间，使用重平衡超时时间来覆盖请求超时时间，以确保请求不会因为协调器阻塞而超时。
4.  调用 [ConsumerNetworkClient#send()](http://consumernetworkclient/#send\(\)) 方法向协调器发送 JoinGroup 请求，返回一个 Future 对象。
5.  使用 [JoinGroupResponseHandler](http://joingroupresponsehandler/) 处理响应结果，并返回一个处理 JoinGroup 请求结果的 RequestFuture。

其作用是发送「**JoinGroup 请求**」到协调器，获取 「**JoinGroup 请求**」的响应结果，并通过 RequestFuture 返回该结果。在发送 JoinGroup 请求时需要构造 JoinGroupRequest 请求对象，并计算请求的超时时间。返回的 RequestFuture 对象用于处理响应结果。

至此消费者客户端发送「**JoinGroup 请求**」剖析完了，接下来我们来剖析服务端如何处理 「**JoinGroup 请求**」，先来看其请求参数数据结构。

## **2.2.3** **正常请求参数**

通常来说，正常一个请求都包含如下数据：

![](https://article-images.zsxq.com/FuF15FQy_aeOja7S9qJtUd1QsV_u)

接下来我们分别来看下该请求的 RequestHeader 以及 RequestBody 的组成部分。

## **2.2.3****.1 JoinGroupRequestHeader**

1.  header\_version：表示请求头版本号, 目前只有 0 和 1; 每个请求对应使用哪个 headerVersion 的映射关系在[ApiMessageType#requestHeaderVersion](http://apimessagetype/#requestHeaderVersion) 中会有体现。
2.  api\_version: 表示请求的标识ID，每个类型的请求都有它对应的唯一ID, 比如这里的 JoinGroupRequest 对应的ID是 11; 映射关系在 [ApiMessageType](http://apimessagetype/)。
3.  api\_version: 该请求的版本号，因为我们可能会对某个请求类型做过改动, 并且改动了请求的 Schemas, 那么每次改动都是一个版本, 比如 [JoinGroupRequest](http://joingrouprequest/) 这个请求总共就有 6 个版本, 那么当前发起的请求的版本号是 ： [Schema.length](http://schema.length/) -1 = 6 - 1 = 5。
4.  下面的 JoinGroupRequest 的 Schemas ,  不同请求类型的 Schemas 不一样, 可以通过 ApiKeys 下面的每个请求查看：

![](https://article-images.zsxq.com/FseAbW536ZmkpnB4yLtz5MPTZqkh)

![](https://article-images.zsxq.com/Fmxq6C_OTmca3T_uJQxDNaSnag4a)

4\. client\_id: 表示客户端ID客户端唯一标识。

5\. correlation\_id: 表示每次发起的请求的唯一标识发起的每次请求的唯一标识, 该值会自增。

## **2.2.3.2 JoinGroupRequestBody 数据结构**

上面的 RequestHeader 基本上都大差不差，但是不同 Request 类型的 RequestBody 是不一样的，对于 [JoinGroupRequest](http://joingrouprequest/) 的属性如下：

![](https://article-images.zsxq.com/llgME8sHAAaUbYM3Cy0r7vKVuW40)

1.  group\_id：表示消费组id，属性 [group.id](http://group.id/) 配置。
2.  session\_timeout\_ms：表示 session 超时时间，属性：[session.timeout.ms](http://session.timeout.ms/)  默认值 10000（10 秒）。消费者定期发送心跳证明自己的存活，如果在这个时间之内 Broker 没收到，那 Broker 就将此消费者从消费者组中移除，进行一次 Reblance。
3.  需要注意的是, 这个值必须在Broker属性 [group.min.session.timeout.ms](http://group.min.session.timeout.ms/) 和 [group.max.session.timeout.ms](http://group.max.session.timeout.ms/) 的值范围中间。
4.  member\_id：表示消费者成员ID, 默认就是空字符串, 客户端不可设置。该值会在后续的请求中返回并被赋值。
5.  group\_instance\_id：表示消费者组实例ID，属性：[group.instance.id](http://group.instance.id/)  默认值 空），从 Kafka 2.3 版本开始引入的新参数，用户提供的消费者实例的唯一标识符。如果设置了消费者则被认为静态成员, 此时会分配较大的 session 超时设置能够避免因成员临时不可用（比如重启）而引发的 Rebalance， 如果不设置, 消费者被认为动态成员。
6.  protocol\_type：表示协议类型，Consumer 发起的协议是 comsumer , 另一个可选项为 connect。
7.  rebalance\_timeout\_ms：表示重平衡的超时时间，属性： [max.poll.interval.ms](http://max.poll.interval.ms/)，默认值300000（5 分钟）。如果消费者两次 poll 的时间超过了此值,那就认为此消费者能力不足，将此消费者的 commit 标记为失败，并将此消费者从消费者组移除，触发一次 Reblance 将该消费者消费的分区分配给其他消费者。
8.  protocols：表示协议映射，该值保存着分区分配策略和对应的订阅元信息。 这里的 name 表示分配策略的 name, 可选值有 \[range、roundrobin、sticky、cooperative-sticky、stream\]。metadata：里面保存着对应的元信息，比如 topics 为订阅的 topic、user\_data 为用户自定义数据等等。 ![](https://article-images.zsxq.com/Ft3Uu721S6cffSJPgWflXpybvlJH)

关于 [protocols](http://protocols%20/) 的计算在下面 [ConsumserCoordinator#metadata()](http://consumsercoordinator/#metadata\(\))，源码如下：

@Override

// ConsumserCoordinator#metadata()

protected JoinGroupRequestData.JoinGroupRequestProtocolCollection metadata() {

log.debug("Joining group with current subscription: {}", subscriptions.subscription());

this.joinedSubscription = subscriptions.subscription();

JoinGroupRequestData.JoinGroupRequestProtocolCollection protocolSet \= new JoinGroupRequestData.JoinGroupRequestProtocolCollection();

List<String> topics = new ArrayList<>(joinedSubscription);

// assignors 是配置 partition.assignment.strategy 的分区分配策略

for (ConsumerPartitionAssignor assignor : assignors) {

Subscription subscription \= new Subscription(topics,

// 分配策略的用户自定义数据,需要实现 subscriptionUserData 方法,默认 range 策略是 null

assignor.subscriptionUserData(joinedSubscription),

// 订阅的所有分区列表

subscriptions.assignedPartitionsList());

// 包含了订阅的Topic、UserData 和经过分组整理的订阅的 Topic 分区列表，并且序列化成ByteBuffer

ByteBuffer metadata \= ConsumerProtocol.serializeSubscription(subscription);

// 组装成每种策略对应的订阅信息数据

protocolSet.add(new JoinGroupRequestData.JoinGroupRequestProtocol()

.setName(assignor.name())

.setMetadata(Utils.toArray(metadata)));

}

return protocolSet;

}

这里需要注意的是: [userData](http://userdata/) 是用户自定义数据， 默认的 range 策略返回的是 null，如果你想设置自己的数据，需要实现方法 [subscriptionUserData()](http://subscriptionuserdata\(\)/)。

## **2.2.4 协调器接收 JoinGroupRequest 请求**

这里的请求最终是发送给协调器节点的，即 [FIND\_COORDINATOR](http://find_coordinator/) 获取的节点，那么协调器接受到客户端发来的JoinGroup 请求进行处理，其入口也是在 [KafkaApis#handle()](http://kafkaapis/#handle\(\)%20) 方法，如下：

  
![](https://article-images.zsxq.com/FsVonIqcQRU417itXPXGICYpPQ5d)

可以看到调用入口是：[KafkaApi#handleJoinGroupRequest()](http://kafkaapi/#handleJoinGroupRequest\(\)) 方法。

def handleJoinGroupRequest(request: RequestChannel.Request, requestLocal: RequestLocal): Unit = {

val joinGroupRequest \= request.body\[JoinGroupRequest\]

// the callback for sending a join-group response

def sendResponseCallback(joinResult: JoinGroupResult): Unit = {

def createResponse(requestThrottleMs: Int): AbstractResponse = {

val protocolName \= if (request.context.apiVersion() >= 7)

joinResult.protocolName.orNull

else

joinResult.protocolName.getOrElse(GroupCoordinator.NoProtocol)

val responseBody \= new JoinGroupResponse(

new JoinGroupResponseData()

.setThrottleTimeMs(requestThrottleMs)

.setErrorCode(joinResult.error.code)

.setGenerationId(joinResult.generationId)

.setProtocolType(joinResult.protocolType.orNull)

.setProtocolName(protocolName)

.setLeader(joinResult.leaderId)

.setMemberId(joinResult.memberId)

.setMembers(joinResult.members.asJava)

)

trace("Sending join group response %s for correlation id %d to client %s."

.format(responseBody, request.header.correlationId, request.header.clientId))

responseBody

}

requestHelper.sendResponseMaybeThrottle(request, createResponse)

}

// 如果入参配置了 group\_instance\_id 并且当前版本小于 2.3,则返回异常 UNSUPPORTED\_VERSION

if (joinGroupRequest.data.groupInstanceId != null && config.interBrokerProtocolVersion < KAFKA\_2\_3\_IV0) {

sendResponseCallback(JoinGroupResult(JoinGroupRequest.UNKNOWN\_MEMBER\_ID, Errors.UNSUPPORTED\_VERSION))

// 如果消费者组授权失败后，则返回异常 GROUP\_AUTHORIZATION\_FAILED

} else if (!authHelper.authorize(request.context, READ, GROUP, joinGroupRequest.data.groupId)) {

sendResponseCallback(JoinGroupResult(JoinGroupRequest.UNKNOWN\_MEMBER\_ID, Errors.GROUP\_AUTHORIZATION\_FAILED))

} else {

val groupInstanceId \= Option(joinGroupRequest.data.groupInstanceId)

// Only return MEMBER\_ID\_REQUIRED error if joinGroupRequest version is >= 4

// and groupInstanceId is configured to unknown.

val requireKnownMemberId \= joinGroupRequest.version >= 4 && groupInstanceId.isEmpty

// let the coordinator handle join-group

val protocols \= joinGroupRequest.data.protocols.valuesList.asScala.map(protocol =>

(protocol.name, protocol.metadata)).toList

// 调用 GroupCoordinator#handleJoinGroup() 方法，来处理消费者组成员发送过来的加入组请求

groupCoordinator.handleJoinGroup(

joinGroupRequest.data.groupId,

joinGroupRequest.data.memberId,

groupInstanceId,

requireKnownMemberId,

request.header.clientId,

request.context.clientAddress.toString,

joinGroupRequest.data.rebalanceTimeoutMs,

joinGroupRequest.data.sessionTimeoutMs,

joinGroupRequest.data.protocolType,

protocols,

sendResponseCallback,

requestLocal)

}

}

## **2.2.4.1 GroupCoordinator#handleJoinGroup()**

def handleJoinGroup(groupId: String, // 消费者组 ID

memberId: String, // 消费者组成员 ID，如果成员是新加入的，那么该字段是空字符串。

groupInstanceId: Option\[String\], // 这是社区于 2.3 版本引入的静态成员字段。静态成员的引入，可以有效避免因系统升级或程序更新而导致的 Rebalance 场景。它属于比较高阶的用法，而且目前还没有被大规模使用，这里你只需要简单了解一下它的作用。另外，后面在讲其他方法时，我会直接省略静态成员的代码，只关注核心逻辑就行了。

requireKnownMemberId: Boolean, // 是否要求成员ID不为空，即是否要求成员必须设置 ID 的布尔字段。这个字段如果为 true 的话，那么 Kafka 要求消费者组成员必须设置 ID。未设置 ID 的成员，会被拒绝加入组。直到它设置了 ID 之后，才能重新加入组。

clientId: String, // client.id 值，Coordinator使用它来生成memberId。memberId的格式是clientId值-UUID。

clientHost: String, // 消费者主机名

rebalanceTimeoutMs: Int, // Rebalance 超时时间,默认是 max.poll.interval.ms 值，如果在这个时间段内，消费者组成员没有完成加入组的操作，就会被禁止入组。

sessionTimeoutMs: Int, // 会话超时时间，如果消费者组成员无法在这段时间内向 Coordinator 汇报心跳，那么将被视为“已过期”，从而引发新一轮 Rebalance。

protocolType: String, // 协议类型

protocols: List\[(String, Array\[Byte\])\], // 按照分配策略分组的订阅分区

responseCallback: JoinCallback, // 完成加入组之后的回调逻辑方法。当消费者组成员成功加入组之后，需要执行该方法。

requestLocal: RequestLocal = RequestLocal.NoCaching): Unit = {

// 1、验证消费者组状态的合法性

validateGroupStatus(groupId, ApiKeys.JOIN\_GROUP).foreach { error =>

responseCallback(JoinGroupResult(memberId, error))

return

}

// 2、确保sessionTimeoutMs介于 \[group.min.session.timeout.ms值，group.max.session.timeout.ms值\] 之间，否则抛出异常，表示超时时间设置无效

if (sessionTimeoutMs < groupConfig.groupMinSessionTimeoutMs ||

sessionTimeoutMs > groupConfig.groupMaxSessionTimeoutMs) {

responseCallback(JoinGroupResult(memberId, Errors.INVALID\_SESSION\_TIMEOUT))

} else {

// 消费者组成员ID是否为空

val isUnknownMember \= memberId == JoinGroupRequest.UNKNOWN\_MEMBER\_ID

// group is created if it does not exist and the member id is UNKNOWN. if member

// is specified but group does not exist, request is rejected with UNKNOWN\_MEMBER\_ID

// 3、获取消费者组信息，如果组不存在，就创建一个新的消费者组

groupManager.getOrMaybeCreateGroup(groupId, isUnknownMember) match {

case None \=\>

responseCallback(JoinGroupResult(memberId, Errors.UNKNOWN\_MEMBER\_ID))

case Some(group) =>

group.inLock {

// 3.1、如果该消费者组已满员

if (!acceptJoiningMember(group, memberId)) {

// 移除该消费者组成员

group.remove(memberId)

// 封装异常表明组已满员

responseCallback(JoinGroupResult(JoinGroupRequest.UNKNOWN\_MEMBER\_ID, Errors.GROUP\_MAX\_SIZE\_REACHED))

// 3.2、如果消费者组成员 ID 为空

} else if (isUnknownMember) {

// 为空ID成员执行加入组操作

doNewMemberJoinGroup(

group,groupInstanceId,requireKnownMemberId,clientId,clientHost,rebalanceTimeoutMs,sessionTimeoutMs,protocolType,protocols,responseCallback,requestLocal

)

} else {

// 为非空ID成员执行加入组操作

doCurrentMemberJoinGroup(

group,memberId,groupInstanceId,clientId,clientHost,rebalanceTimeoutMs,sessionTimeoutMs,protocolType,protocols,responseCallback

)

}

// 如果消费者组正处于 PreparingRebalance 状态

if (group.is(PreparingRebalance)) {

// 放入 Purgatory，等待后面统一延时处理

rebalancePurgatory.checkAndComplete(GroupJoinKey(group.groupId))

}

}

}

}

}

private def acceptJoiningMember(group: GroupMetadata, member: String): Boolean = {

group.currentState match {

// Always accept the request when the group is empty or dead

// 如果是 Empty 或 Dead 状态，肯定不会是满员，直接返回 true，表示可以接纳申请入组的成员。

case Empty | Dead =>

true

// An existing member is accepted if it is already awaiting. New members are accepted

// up to the max group size. Note that the number of awaiting members is used here

// for two reasons:

// 1) the group size is not reliable as it could already be above the max group size

// if the max group size was reduced.

// 2) using the number of awaiting members allows to kick out the last rejoining

// members of the group.

case PreparingRebalance \=\>

// 如果是 PreparingRebalance 状态，那么批准成员入组的条件是必须满足一下两个条件之一：

// 1、该成员是之前已有的成员，且当前正在等待加入组。

// 2、当前等待加入组的成员数小于 Broker 端参数值 group.max.size。

(group.has(member) && group.get(member).isAwaitingJoin) ||

group.numAwaiting < groupConfig.groupMaxSize

// An existing member is accepted. New members are accepted up to the max group size.

// Note that the group size is used here. When the group transitions to CompletingRebalance,

// members which haven't rejoined are removed.

// 如果是其他状态，那么批准加入组的条件是该成员是已有成员或者是当前组总成员数小于 Broker 端参数值 group.max.size

case CompletingRebalance | Stable =>

group.has(member) || group.size < groupConfig.groupMaxSize

}

}

这是处理加入消费组的入口方法，其核心步骤如下：

1.  调用 [validateGroupStatus](http://validategroupstatus/) 方法验证消费者组状态的合法性。所谓的合法性，也就是消费者组名 groupId 不能为空，另外 JoinGroupRequest 请求是否发送给了正确的 [Coordinator](http://coordinator/)，这两者必须同时满足。如果没有通过这些检查，则封装相应的错误，并调用回调函数返回。否则进入下一步。
2.  校验 [sessionTimeoutMs](http://sessiontimeoutms/) 的值是否介于 \[[group.min.session.timeout.ms](http://group.min.session.timeout.ms/)，[group.max.session.timeout.ms](http://group.max.session.timeout.ms/)\]之间，如果不是就认定是非法值，封装对应的异常调用回调函数返回，这两个参数分别表示消费者组允许配置的最小和最大会话超时时间。如果是的话进入下一步。
3.  获取当前成员的ID信息，并查看它是否为空。通过 [GroupMetadataManager](http://groupmetadatamanager/) 获取消费者组的元数据信息，如果该组的元数据信息存在，则进入到下一步；如果不存在，会判断当前成员 ID 是否为空，如果为空，就创建一个空的元数据对象，然后进入到下一步，如果不为空，则返回 None。一旦返回了 None，此时封装「**未知成员ID**」的异常，调用回调函数返回。
4.  通过 [acceptJoiningMember()](http://acceptjoiningmember\(\)/) 方法检查当前消费者组是否已满员。内部根据**消费者组状态**确定是否满员。这里的消费者组状态有三种。
5.  **状态一**：如果是 Empty 或 Dead 状态，肯定不会是满员，直接返回 true，表示可以接纳申请入组的成员。
6.  **状态二**：如果是 PreparingRebalance 状态，那么批准成员入组的条件是必须满足一下两个条件之一。
7.  该成员是之前已有的成员，且当前正在等待加入组。
8.  当前等待加入组的成员数小于 Broker 端参数 [group.max.size](http://group.max.size/) 值。
9.  只要满足这两个条件中的任意一个，当前消费者组成员都会被批准入组。
10.  **状态三**：如果是其他状态，那么，入组的条件是该成员是已有成员，或者是当前组总成员数小于 Broker 端参数 [group.max.size](http://group.max.size/) 值。需要注意的是，这里比较的是**组当前的总成员数**，而不是等待入组的成员数，这是因为，一旦 Rebalance 过渡到 [CompletingRebalance](http://completingrebalance/) 状态之后，没有完成加入组的成员就会被移除。
11.  假如成员不被批准入组，那么需要将该成员从元数据缓存中移除，同时封装「**组已满员**」的异常，并调用回调函数返回。如果成员被批准入组，则根据 Member ID 是否为空，就执行 [doNewMemberJoinGroup](http://donewmemberjoingroup/) 或[doCurrentMemberJoinGroup](http://docurrentmemberjoingroup/) 方法执行加入组的逻辑。
12.  尝试完成 [JoinGroupRequest](http://joingrouprequest/) 请求的处理。如果消费者组处于 [PreparingRebalance](http://preparingrebalance/) 状态，就将该请求放入Purgatory，尝试立即完成。如果是其它状态，则无需将请求放入 Purgatory。毕竟，我们处理的是加入组的逻辑，而此时消费者组的状态应该要变更到 [PreparingRebalance](http://preparingrebalance/) 后，Rebalance 才能完成加入组操作。当然，如果延时请求不能立即完成，则交由 Purgatory 统一进行延时处理。

通过剖析，我们可以得出真正执行加入组逻辑的是 [doNewMemberJoinGroup](http://donewmemberjoingroup/) 和 [doCurrentMemberJoinGroup](http://docurrentmemberjoingroup/) 这两个方法。

接下来，我们就来剖析下这两个方法。

## **2.2.4.2 GroupCoordinator#doNewMemberJoinGroup()**

如果是全新的消费者组成员加入组，此时会调用该方法，因为 Member ID 还未生成。源码如下：

private def doNewMemberJoinGroup(group: GroupMetadata,groupInstanceId: Option\[String\],requireKnownMemberId: Boolean,clientId: String,clientHost: String,rebalanceTimeoutMs: Int,sessionTimeoutMs: Int,protocolType: String,protocols: List\[(String, Array\[Byte\])\], responseCallback: JoinCallback, requestLocal: RequestLocal): Unit = {

group.inLock {

// Dead 状态

if (group.is(Dead)) {

// 封装异常调用回调函数返回

responseCallback(JoinGroupResult(JoinGroupRequest.UNKNOWN\_MEMBER\_ID, Errors.COORDINATOR\_NOT\_AVAILABLE))

} else if (!group.supportsProtocols(protocolType, MemberMetadata.plainProtocolSet(protocols))) {

// 成员配置的协议类型/分区消费分配策略与消费者组的不匹配

responseCallback(JoinGroupResult(JoinGroupRequest.UNKNOWN\_MEMBER\_ID, Errors.INCONSISTENT\_GROUP\_PROTOCOL))

} else {

// 根据规则为该成员创建成员ID

val newMemberId \= group.generateMemberId(clientId, groupInstanceId)

// 如果配置了静态成员

groupInstanceId match {

case Some(instanceId) =>

// 静态新成员加入组

doStaticNewMemberJoinGroup(

group,instanceId,newMemberId,clientId,clientHost,rebalanceTimeoutMs,sessionTimeoutMs,protocolType,protocols,responseCallback,requestLocal

)

case None \=\>

// 动态新成员加入组

doDynamicNewMemberJoinGroup(

group,requireKnownMemberId,newMemberId,clientId,clientHost,rebalanceTimeoutMs,sessionTimeoutMs,protocolType,protocols,responseCallback

)

}

}

}

}

核心步骤如下：

1.  首先，检查消费者组的状态。如果是Dead状态，则封装异常并调用回调函数返回。此时你是否会有疑问，既然是向该组添加成员，为什么消费者组状态还能是 Dead 呢？实际上，这种情况是可能存在的。因为在成员加入消费者组的同时，可能存在另一个线程，已经把消费者组的元数据信息从 Coordinator 中移除了。假如对应的Coordinator 发生了变更，移动到了其他的 Broker 上，此时封装异常返回给消费者，消费者会去寻找最新的Coordinator，然后重新发起加入组操作。
2.  如果状态不是 Dead，就检查该成员的协议类型以及分区消费分配策略，是否与消费者组当前支持的方案匹配，如果不匹配则封装异常并调用回调函数返回。这里的匹配与否，是指成员的协议类型与消费者组的是否一致，以及成员设定的分区消费分配策略是否被消费者组下的其它成员支持。
3.  如果检查都通过，接着会为该成员生成成员 ID，生成规则是 clientId-UUID。这就是 [generateMemberId](http://generatememberid/) 方法做的事情。
4.  如果配置了静态成员，则执行静态新成员加入组操作，否则执行动态新成员加入组操作。

这里分动静态成员加入消费者组操作，我们只看下动态成员加入消费者组操作。

private def doDynamicNewMemberJoinGroup(

group: GroupMetadata,

requireKnownMemberId: Boolean,

newMemberId: String,

clientId: String,

clientHost: String,

rebalanceTimeoutMs: Int,

sessionTimeoutMs: Int,

protocolType: String,

protocols: List\[(String, Array\[Byte\])\],

responseCallback: JoinCallback

): Unit = {

// 如果要求成员ID不为空

if (requireKnownMemberId) {

....

group.addPendingMember(newMemberId)

addPendingMemberExpiration(group, newMemberId, sessionTimeoutMs)

responseCallback(JoinGroupResult(newMemberId, Errors.MEMBER\_ID\_REQUIRED))

} else {

// 添加成员

addMemberAndRebalance(rebalanceTimeoutMs, sessionTimeoutMs, newMemberId, None,

clientId, clientHost, protocolType, protocols, group, responseCallback)

}

}

核心步骤如下：

1.  如果 [requireKnownMemberId](http://requireknownmemberid/) 为 true，则将该成员加入到待添加成员列表 [pendingMembers](http://pendingmembers/) 中，封装异常以及生成好的成员 ID，将该成员的加入消费者组申请「**打回去**」，等分配好了成员 ID 以后再重新申请。
2.  如果 [requireKnownMemberId](http://requireknownmemberid/) 为 false，则直接调用 [addMemberAndRebalance](http://addmemberandrebalance/) 方法将其加入到消费者组中。

通常来说，如果没有启用静态成员机制的话，[requireKnownMemberId](http://requireknownmemberid/) 的值是 true，这是由 [KafkaApis#handleJoinGroupRequest](http://kafkaapis/#handleJoinGroupRequest) 方法的这行代码决定的：

val requireKnownMemberId \= joinGroupRequest.version >= 4 && groupInstanceId.isEmpty

所以如果你使用的是比较新的 Kafka 客户端版本，而且没有配置过 Consumer 端参数 [group.instance.id](http://group.instance.id/) 的话，那么该字段的值就是 true，这也表示了 Kafka 要求消费者成员加入组时，必须要分配好成员 ID 才行。

接着我们来剖析 doCurrentMemberJoinGroup()。

## **2.2.4.3 GroupCoordinator#doCurrentMemberJoinGroup()**

如果不是全新的消费者组成员加入组，此时会调用该方法，因为 Member ID 已经生成。源码如下：

private def doCurrentMemberJoinGroup(group: GroupMetadata,memberId: String,groupInstanceId: Option\[String\], clientId: String,clientHost: String,rebalanceTimeoutMs: Int,sessionTimeoutMs: Int, protocolType: String, protocols: List\[(String, Array\[Byte\])\], responseCallback: JoinCallback ): Unit = {

group.inLock {

// 如果是 Dead 状态，封装 COORDINATOR\_NOT\_AVAILABLE 异常调用回调函数返回

if (group.is(Dead)) {

// if the group is marked as dead, it means some other thread has just removed the group

// from the coordinator metadata; it is likely that the group has migrated to some other

// coordinator OR the group is in a transient unstable phase. Let the member retry

// finding the correct coordinator and rejoin.

responseCallback(JoinGroupResult(memberId, Errors.COORDINATOR\_NOT\_AVAILABLE))

} else if (!group.supportsProtocols(protocolType, MemberMetadata.plainProtocolSet(protocols))) {

// 如果协议类型或分区消费分配策略与消费者组的不匹配, 封装 INCONSISTENT\_GROUP\_PROTOCOL 异常调用回调函数返回

responseCallback(JoinGroupResult(memberId, Errors.INCONSISTENT\_GROUP\_PROTOCOL))

} else if (group.isPendingMember(memberId)) {

// 如果是待加入成员且这次分配了成员 ID，则允许加入组

groupInstanceId.foreach { instanceId =>

throw new IllegalStateException(s"Received unexpected JoinGroup with groupInstanceId=$instanceId " +

s"for pending member with memberId=$memberId")

}

debug(s"Pending dynamic member with id $memberId joins group ${group.groupId} in " +

s"${group.currentState} state. Adding to the group now.")

// 加入组并重分配

addMemberAndRebalance(rebalanceTimeoutMs, sessionTimeoutMs, memberId, None,

clientId, clientHost, protocolType, protocols, group, responseCallback)

} else {

// 校验当前成员

val memberErrorOpt \= validateCurrentMember(

group,

memberId,

groupInstanceId,

operation = "join-group" // 加入消费者组操作

)

memberErrorOpt match {

// 如果有错误，直接返回错误

case Some(error) => responseCallback(JoinGroupResult(memberId, error))

case None \=\> group.currentState match {

// 如果是 PreparingRebalance 状态

case PreparingRebalance \=\>

// 获取该成员的元数据信息

val member \= group.get(memberId)

// 更新成员信息并开始准备Rebalance

updateMemberAndRebalance(group, member, protocols, s"Member ${member.memberId} joining group during ${group.currentState}", responseCallback)

// 如果是 CompletingRebalance 状态

case CompletingRebalance \=\>

val member \= group.get(memberId)

if (member.matches(protocols)) { // 如果成员以前申请过加入组直接返回当前组信息

// member is joining with the same metadata (which could be because it failed to

// receive the initial JoinGroup response), so just return current group information

// for the current generation.

responseCallback(JoinGroupResult(

members = if (group.isLeader(memberId)) {

group.currentMemberMetadata

} else {

List.empty

}, memberId = memberId,generationId = group.generationId,protocolType = group.protocolType,protocolName = group.protocolName,leaderId = group.leaderOrNull,error = Errors.NONE))

} else {

// 否则，更新成员信息并开始准备Rebalance

// member has changed metadata, so force a rebalance

updateMemberAndRebalance(group, member, protocols, s"Updating metadata for member ${member.memberId} during ${group.currentState}", responseCallback)

}

// 如果是 Stable 状态

case Stable \=\>

val member \= group.get(memberId)

// 如果成员是 Leader 成员，或者成员变更了分区分配策略

if (group.isLeader(memberId)) { // 更新成员信息并开始准备Rebalance

// force a rebalance if the leader sends JoinGroup;

// This allows the leader to trigger rebalances for changes affecting assignment

// which do not affect the member metadata (such as topic metadata changes for the consumer)

updateMemberAndRebalance(group, member, protocols, s"Leader ${member.memberId} re-joining group during ${group.currentState}", responseCallback)

} else if (!member.matches(protocols)) {

updateMemberAndRebalance(group, member, protocols, s"Updating metadata for member ${member.memberId} during ${group.currentState}", responseCallback)

} else {

// for followers with no actual change to their metadata, just return group information

// for the current generation which will allow them to issue SyncGroup

responseCallback(JoinGroupResult(

members = List.empty,

memberId = memberId,

generationId = group.generationId,

protocolType = group.protocolType,

protocolName = group.protocolName,

leaderId = group.leaderOrNull,

error = Errors.NONE))

}

// 如果是 Empty 或者 Dead 状态，封装异常调用回调函数返回

case Empty | Dead =>

// Group reaches unexpected state. Let the joining member reset their generation and rejoin.

warn(s"Attempt to add rejoining member $memberId of group ${group.groupId} in " +

s"unexpected group state ${group.currentState}")

responseCallback(JoinGroupResult(memberId, Errors.UNKNOWN\_MEMBER\_ID))

}

}

}

}

}

可以看出 [doCurrentMemberJoinGroup](http://docurrentmemberjoingroup/) 方法开头和 [doNewMemberJoinGroup](http://donewmemberjoingroup/) 非常类似，也是判断是否处于Dead 状态，并且检查协议类型和分区消费分配策略是否与消费者组的相匹配。

1.  不同的是，[doCurrentMemberJoinGroup](http://docurrentmemberjoingroup/) 要判断当前申请入组的成员是否是待加入成员。如果是的话，本次成员已经分配好了成员ID，直接调用 [addMemberAndRebalance](http://addmemberandrebalance/) 方法让其加入消费者组。
2.  如果不是的话，那么处理非待加入成员的入组申请。首先会校验当前成员，如果有错误，直接返回错误。
3.  否则匹配消费者当前状态，这里分 4 种情况：
4.  如果是 [PreparingRebalance](http://preparingrebalance/) 状态，就说明消费者组正要开启 Rebalance 流程，则调用[updateMemberAndRebalance](http://updatememberandrebalance/) 方法更新成员信息，并开始准备 Rebalance。
5.  如果是 [CompletingRebalance](http://completingrebalance/) 状态，那么判断该成员的分区消费分配策略与订阅分区列表是否和已保存记录中的一样，如果一样，就说明该成员已经应该发起过加入组的操作，并且 Coordinator 已经批准了，只是该成员没有收到，针对这种情况构造一个 [JoinGroupResult](http://joingroupresult/) 对象，直接返回当前的组信息给成员。如果 protocols不相同，就说明成员变更了订阅信息或分配策略，就要调用 [updateMemberAndRebalance](http://updatememberandrebalance/) 方法，更新成员信息，并开始准备新一轮 Rebalance。
6.  如果是 Stable 状态，判断该成员是否是 Leader 成员，或者是它的订阅信息或分配策略发生了变更。如果是这种情况，就调用 [updateMemberAndRebalance](http://updatememberandrebalance/) 方法进行一次新的 Rebalance。否则的话返回当前组信息给该成员，通知它们可以发起 Rebalance 的下一步操作。
7.  如果状态是 Empty 或 Dead 状态，封装 UNKNOWN\_MEMBER\_ID 异常，并调用回调函数返回。

可以看到，这里频繁地调用 [updateMemberAndRebalance](http://updatememberandrebalance/) 方法，我们来看下。

## **2.2.4.4 GroupCoordinator#updateMemberAndRebalance()**

private def updateMemberAndRebalance(group: GroupMetadata,

member: MemberMetadata,

protocols: List\[(String, Array\[Byte\])\],

reason: String,

callback: JoinCallback): Unit = {

// 1、调用 GroupMetadata#updateMember 方法来更新消费者组成员；

group.updateMember(member, protocols, callback)

// 2、准备Rebalance：将消费者组状态变更到 PreparingRebalance，然后创建 DelayedJoin 对象，并交由 Purgatory，等待延时处理加入组操作。

maybePrepareRebalance(group, reason)

}

private def maybePrepareRebalance(group: GroupMetadata, reason: String): Unit = {

group.inLock {

if (group.canRebalance)

prepareRebalance(group, reason)

}

}

// 准备进行重新平衡操作，该操作会重新分配消费者组中的分区

private\[group\] def prepareRebalance(group: GroupMetadata, reason: String): Unit {

// 如果有成员正在等待同步，则取消他们的请求并要求重新加入

if (group.is(CompletingRebalance))

resetAndPropagateAssignmentError(group, Errors.REBALANCE\_IN\_PROGRESS)

// 如果有同步超时正在等待处理，则取消它

removeSyncExpiration(group)

// 创建一个延迟的重新加入操作，根据消费者组的当前状态选择不同的延迟策略

val delayedRebalance \= if (group.is(Empty))

new InitialDelayedJoin(this,

rebalancePurgatory,

group,

groupConfig.groupInitialRebalanceDelayMs,

groupConfig.groupInitialRebalanceDelayMs,

max(group.rebalanceTimeoutMs - groupConfig.groupInitialRebalanceDelayMs, 0))

else

new DelayedJoin(this, group, group.rebalanceTimeoutMs)

// 将消费者组状态转换为PreparingRebalance

group.transitionTo(PreparingRebalance)

// 打印日志，说明正在准备重新平衡操作的消费者组的相关信息

info(s"Preparing to rebalance group ${group.groupId} in state ${group.currentState} with old generation " +

s"${group.generationId} (${Topic.GROUP\_METADATA\_TOPIC\_NAME}-${partitionFor(group.groupId)}) (reason: $reason)")

// 根据消费者组的groupId创建GroupJoinKey，并加入延迟队列

val groupKey \= GroupJoinKey(group.groupId)

rebalancePurgatory.tryCompleteElseWatch(delayedRebalance, Seq(groupKey))

}

可以看出，该方法只做了两件事情：

1.  更新组成员信息：调用 [GroupMetadata的updateMember](http://xn--groupmetadataupdatemember-ui55d/) 方法来更新消费者组成员。
2.  准备 Rebalance：将消费者组状态变更到 [PreparingRebalance](http://preparingrebalance/)，然后创建 [DelayedJoin](http://delayedjoin/) 对象，并交由 [Purgatory](http://purgatory/)，等待延时处理加入组操作。

## **2.2.4.5 GroupCoordinator#addMemberAndRebalance()**

可以看到，这里也会调用 [addMemberAndRebalance](http://addmemberandrebalance/) 方法来加入消费者组并启动重平衡操作，我们来看下。

private def addMemberAndRebalance(rebalanceTimeoutMs: Int,

sessionTimeoutMs: Int,

memberId: String,

groupInstanceId: Option\[String\],

clientId: String,

clientHost: String,

protocolType: String,

protocols: List\[(String, Array\[Byte\])\],

group: GroupMetadata,

callback: JoinCallback): Unit = {

// 创建 MemberMetadata 对象实例

val member \= new MemberMetadata(memberId, groupInstanceId, clientId, clientHost,

rebalanceTimeoutMs, sessionTimeoutMs, protocolType, protocols)

// 标识该成员是新成员

member.isNew = true

// 如果消费者组准备开启首次 Rebalance，设置 newMemberAdded 为 True

if (group.is(PreparingRebalance) && group.generationId == 0)

group.newMemberAdded = true

// 将该成员添加到消费者组

group.add(member, callback)

// 设置下次心跳超期时间

completeAndScheduleNextExpiration(group, member, NewMemberJoinTimeoutMs)

// 准备开启 Rebalance

maybePrepareRebalance(group, s"Adding new member $memberId with group instance id $groupInstanceId")

}

核心步骤如下：

1.  根据传入参数创建一个 MemberMetadata 对象实例，并设置 isNew 为 True，标识该成员是新成员。isNew字段与心跳设置相关联，建议你去研究下 [MemberMetadata#hasSatisfiedHeartbeat](http://membermetadata/#hasSatisfiedHeartbeat) 方法的源码，就可以搞明白该字段是如何帮助 Coordinator 来确认消费者组成员心跳的。
2.  判断消费者组是否是首次开启 Rebalance。
3.  如果是的话就把 newMemberAdded 设置为 true。
4.  该字段的作用，是 Kafka 为消费者组 Rebalance 流程做的一个性能优化。即在消费者组首次进行 Rebalance时，让 Coordinator 多等待一段时间，从而让更多的消费者组成员加入到组中，以免后来者申请入组而反复进行 Rebalance。这段多等待的时间，就是 Broker 端参数 [group.initial.rebalance.delay.ms](http://group.initial.rebalance.delay.ms/) 的值。用来判断是否需要多等待这段时间的一个变量。
5.  调用 [GroupMetadata#add](http://groupmetadata/#add) 方法，将新成员信息加入到消费者组元数据中，同时设置该成员的下次心跳超期时间。
6.  最后调用 [maybePrepareRebalance](http://maybepreparerebalance/) 方法，准备开启 Rebalance 操作。

## **2.3 发送 SyncGroupRequest 请求并处理**

在执行完了 [JoinGroupRequest](http://joingrouprequest/) 请求之后, 消费者客户端收到了 Response, 此时组内所有成员会立马向消费者组协调器发起了 [SyncGroupRequest](http://syncgrouprequest/) 请求。

那么 SyncGroup 具体做了哪些事情呢？接下来一起剖析一下！

其响应入口方法是：[JoinGroupResponseHandler#onJoinLeader](http://joingroupresponsehandler/#onJoinLeader) 或者[JoinGroupResponseHandler#onJoinFollow](http://joingroupresponsehandler/#onJoinFollow)。

这两个的区别是：

1.  如果当前成员是 Leder Consumer 则调用的是 onJoinLeader()。否则调用 onJoinFollow()。
2.  onJoinFollow 和 onJoinLeader 区别在于前者不会带上 Assignments 数据，onJoinLeader 会根据[分区分配策略](https://articles.zsxq.com/id_esdb8l8rwxb7.html)计算一下当前的分配情况然后传入请求。

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

## **AbstractCoordinator#SyncGroupRequestAssignment()**

接着剖析 [onJoinLeader()](http://onjoinleader\(\)/) 方法的第二步 [SyncGroupRequestAssignment()](http://syncgrouprequestassignment\(\)/) 同步分配方案给协调器，源码如下：

private RequestFuture<ByteBuffer> sendSyncGroupRequest(SyncGroupRequest.Builder requestBuilder) {

if (coordinatorUnknown())

return RequestFuture.coordinatorNotAvailable();

// 将异步请求入队，核心处理在收到协调器响应后的回调处理器 SyncGroupResponseHandler 中，收到响应后 SyncGroupResponseHandler#handle() 方法将被触发

return client.send(coordinator, requestBuilder)

.compose(new SyncGroupResponseHandler(generation));

}

可以发现这几个请求的套路都类似，都是先将请求入队异步发送，最后通过 ResponseHandler 进行处理。至此消费者客户端发送「**SyncGroup 请求**」剖析完了，接下来我们来剖析服务端如何处理 「**Sync****Group 请求**」，先来看其请求参数数据结构。

## **2.3.1** **正常请求参数**

通常来说，正常一个请求都包含如下数据：

**![](https://article-images.zsxq.com/FuF15FQy_aeOja7S9qJtUd1QsV_u)**

接下来我们分别来看下该请求的 RequestHeader 以及 RequestBody 的组成部分。

## **2.3.1.1 SyncGroupRequestHeader**

1.  header\_version：表示请求头版本号, 目前只有 0 和 1; 每个请求对应使用哪个 headerVersion 的映射关系在[ApiMessageType#requestHeaderVersion](http://apimessagetype/#requestHeaderVersion) 中会有体现。
2.  api\_version: 表示请求的标识ID，每个类型的请求都有它对应的唯一ID, 比如这里的 [SyncGroupRequest](http://syncgrouprequest/) 对应的ID是 14; 映射关系在 [ApiMessageType](http://apimessagetype/)。
3.  api\_version: 该请求的版本号，因为我们可能会对某个请求类型做过改动, 并且改动了请求的 Schemas, 那么每次改动都是一个版本, 比如 [Sync](http://syncgrouprequest/)[GroupRequest](http://syncgrouprequest/) 这个请求总共就有 6 个版本, 那么当前发起的请求的版本号是 ： [Schema.length](http://schema.length/) -1 = 6 - 1 = 5。
4.  下面的 [Sync](http://syncgrouprequest/)[GroupRequest](http://syncgrouprequest/) 的 Schemas ,  不同请求类型的 Schemas 不一样, 可以通过 ApiKeys 下面的每个请求查看：

![](https://article-images.zsxq.com/FseAbW536ZmkpnB4yLtz5MPTZqkh)

public static final Schema\[\] SCHEMAS = new Schema\[\] {

SCHEMA\_0,

SCHEMA\_1,

SCHEMA\_2,

SCHEMA\_3,

SCHEMA\_4,

SCHEMA\_5

};

public static final Schema SCHEMA\_5 \=

new Schema(

new Field("group\_id", Type.COMPACT\_STRING, "The unique group identifier."),

new Field("generation\_id", Type.INT32, "The generation of the group."),

new Field("member\_id", Type.COMPACT\_STRING, "The member ID assigned by the group."),

new Field("group\_instance\_id", Type.COMPACT\_NULLABLE\_STRING, "The unique identifier of the consumer instance provided by end user."),

new Field("protocol\_type", Type.COMPACT\_NULLABLE\_STRING, "The group protocol type."),

new Field("protocol\_name", Type.COMPACT\_NULLABLE\_STRING, "The group protocol name."),

new Field("assignments", new CompactArrayOf(SyncGroupRequestAssignment.SCHEMA\_4), "Each assignment."),

TaggedFieldsSection.of(

)

);

4\. client\_id: 表示客户端ID客户端唯一标识。

5\. correlation\_id: 表示每次发起的请求的唯一标识发起的每次请求的唯一标识, 该值会自增。

## **2.3.1.2 SyncGroupRequestBody 数据结构**

上面的 RequestHeader 基本上都大差不差，但是不同 Request 类型的 RequestBody 是不一样的，对于 [Sync](http://syncgrouprequest/)[GroupRequest](http://syncgrouprequest/) 的属性如下：

![](https://article-images.zsxq.com/Fq0mK7Bf8pk7q_fl8gELlsBw43ci)

1.  group\_id：表示消费组id，属性 [group.id](http://group.id/) 配置。
2.  member\_id：表示消费者成员ID, 默认就是空字符串, 客户端不可设置。该值会在后续的请求中返回并被赋值。
3.  group\_instance\_id：表示消费者组实例ID，属性：[group.instance.id](http://group.instance.id/)  默认值 空），从 Kafka 2.3 版本开始引入的新参数，用户提供的消费者实例的唯一标识符。如果设置了消费者则被认为静态成员, 此时会分配较大的 session 超时设置能够避免因成员临时不可用（比如重启）而引发的 Rebalance， 如果不设置, 消费者被认为动态成员。
4.  generation\_id：表示消费组协调器年代ID，每个消费组协调器的年代 ID，没经过一次变化 这个id就是自增1, 比如新增Member、Leave Member 等等操作都会引起年代的增长。
5.  protocol\_type：表示协议类型，Consumer 发起的协议是 comsumer , 另一个可选项为 connect。
6.  protocol\_name：表示消费组分区分配策略, 比如 range。
7.  assignments：表示每个消费者对应的分配分区，该参数只有是 Leader Member 发起请求的时候才会上。单个 assignment结构如下：

![](https://article-images.zsxq.com/Fhsy7gBoBBtbs4AFYrZVaWBrCLRq)

## **2.3.3 协调器接收 SyncGroupRequest 请求**

这里的请求最终是发送给协调器节点的，即 [FIND\_COORDINATOR](http://find_coordinator/) 获取的节点，那么协调器接受到客户端发来的SyncGroup 请求进行处理，其入口也是在 [KafkaApis#handle()](http://kafkaapis/#handle\(\)%20) 方法，如下：

  
![](https://article-images.zsxq.com/Fq26QmBOzxC0ONBFsZjvTGEFh1dE)

可以看到调用入口是：[KafkaApi#handleSyncGroupRequest()](http://kafkaapi/#handleSyncGroupRequest\(\)) 方法。

// 处理 SyncGroupRequest 请求，该请求用于同步消费者组内成员的分区分配信息

def handleSyncGroupRequest(

request: RequestChannel.Request,

requestLocal: RequestLocal

): CompletableFuture\[Unit\] = {

// 从请求中获取 SyncGroupRequest 对象

val syncGroupRequest \= request.body\[SyncGroupRequest\]

// 如果当前 Broker 的版本低于 2.3，且 SyncGroupRequest 包含了 groupInstanceId 属性，直接返回错误响应

if (syncGroupRequest.data.groupInstanceId != null && config.interBrokerProtocolVersion.isLessThan(IBP\_2\_3\_IV0)) {

requestHelper.sendMaybeThrottle(request, syncGroupRequest.getErrorResponse(Errors.UNSUPPORTED\_VERSION.exception))

CompletableFuture.completedFuture\[Unit\](())

}

// 如果 SyncGroupRequest 缺少 ProtocolType 或 ProtocolName 字段，则返回错误响应

else if (!syncGroupRequest.areMandatoryProtocolTypeAndNamePresent()) {

requestHelper.sendMaybeThrottle(request, syncGroupRequest.getErrorResponse(Errors.INCONSISTENT\_GROUP\_PROTOCOL.exception))

CompletableFuture.completedFuture\[Unit\](())

}

// 如果消费者没有授权 SyncGroupRequest 的操作，则返回错误响应

else if (!authHelper.authorize(request.context, READ, GROUP, syncGroupRequest.data.groupId)) {

requestHelper.sendMaybeThrottle(request, syncGroupRequest.getErrorResponse(Errors.GROUP\_AUTHORIZATION\_FAILED.exception))

CompletableFuture.completedFuture\[Unit\](())

}

// 否则，则通过 GroupCoordinator 对象处理 SyncGroupRequest 请求

else {

groupCoordinator.syncGroup(

request.context,

syncGroupRequest.data,

requestLocal.bufferSupplier

).handle\[Unit\] { (response, exception) =>

// 处理同步请求的结果，并返回相应的响应

if (exception != null) {

requestHelper.sendMaybeThrottle(request, syncGroupRequest.getErrorResponse(exception))

} else {

requestHelper.sendMaybeThrottle(request, new SyncGroupResponse(response))

}

}

}

}

该方法是 Kafka 消费者组协调器处理 [SyncGroupRequest](http://syncgrouprequest/) 请求的核心方法。[SyncGroupRequest](http://syncgrouprequest/) 请求用于同步消费者组内成员的分区分配信息。在该方法内部，首先对请求的合法性进行校验，然后通过 [GroupCoordinator](http://groupcoordinator/) 对象处理请求，并返回对应的响应结果。

真正处理请求的地方：[GroupCoordinator#handleSyncGroup](http://groupcoordinator/#handleSyncGroup)。

## **2.3.3.1 GroupCoordinator#handleSyncGroup()**

def handleSyncGroup(groupId: String, // 消费者组名，标识这个成员属于哪个消费者组

generation: Int, // 消费者组Generation号。Generation类似于任期的概念，标识了Coordinator负责为该消费者组处理的Rebalance次数。每当有新的Rebalance开启时，Generation都会自动加1。

memberId: String, // 消费者组成员ID。该字段由Coordinator根据一定的规则自动生成。具体的规则上节课我们已经学过了，我就不多说了。总体而言，成员ID的值不是由你直接指定的，但是你可以通过client.id参数，间接影响该字段的取值。

protocolType: Option\[String\],// 标识协议类型的字段，这个字段可能的取值有两个：consumer和connect。对于普通的消费者组而言，这个字段的取值就是consumer，该字段是Option类型，因此，实际的取值是Some(“consumer”)；Kafka Connect组件中也会用到消费者组机制，那里的消费者组的取值就是connect。

protocolName: Option\[String\], // 消费者组选定的分区消费分配策略名称。这里的选择方法，就是我们之前学到的GroupMetadata.selectProtocol方法。

groupInstanceId: Option\[String\], // 按照成员ID分组的分配方案。需要注意的是，只有Leader成员发送的

groupAssignment: Map\[String, Array\[Byte\]\], // 按照成员ID分组的分配方案。需要注意的是，只有 Leader 成员发送的 SyncGroupRequest 请求，才包含这个方案，因此，Coordinator 在处理Leader 成员的请求时，该字段才有值。

responseCallback: SyncCallback,

requestLocal: RequestLocal = RequestLocal.NoCaching): Unit = {

// 验证消费者状态及合法性

validateGroupStatus(groupId, ApiKeys.SYNC\_GROUP) match {

// 如果未通过合法性检查，且错误原因是 Coordinator 正在加载

case Some(error) if error \=\= Errors.COORDINATOR\_LOAD\_IN\_PROGRESS =>

// 封装 REBALANCE\_IN\_PROGRESS 异常，并调用回调函数返回

responseCallback(SyncGroupResult(Errors.REBALANCE\_IN\_PROGRESS))

// 如果是其它错误，则封装对应错误，并调用回调函数返回

case Some(error) => responseCallback(SyncGroupResult(error))

case None \=\>

// 获取消费者组元数据

groupManager.getGroup(groupId) match {

// 如果未找到，则封装 UNKNOWN\_MEMBER\_ID 异常，并调用回调函数返回

case None \=\> responseCallback(SyncGroupResult(Errors.UNKNOWN\_MEMBER\_ID))

// 如果找到的话，则调用doSyncGroup方法执行组同步任务

case Some(group) => doSyncGroup(group, generation, memberId, protocolType, protocolName,

groupInstanceId, groupAssignment, requestLocal, responseCallback)

}

}

}

补充流程图，todo。。。

核心步骤：

1.  验证消费者状态及合法性，这些检查项包括：
2.  消费者组名不能为空。
3.  Coordinator 组件处于运行状态。
4.  Coordinator 组件当前没有执行加载过程。当 Coordinator 变更到其他 Broker 上时，需要从内部位移主题\_\_consumer\_offsets 中读取消息数据，并填充到内存上的消费者组元数据缓存，这就是所谓的加载。
5.  1)、如果 Coordinator 变更了，那么，发送给旧 Coordinator 所在 Broker 的请求就失效了，因为它没有通过第 4 个检查项，即发送给正确的 Coordinator。
6.  2)、如果发送给了正确的 Coordinator，但此时 Coordinator 正在执行加载过程，那么它就没有通过第 3 个检查项，因为 Coordinator 还不能对外提供服务，要等加载完成之后才可以。
7.  [SyncGroupRequest](http://syncgrouprequest/) 请求发送给了正确的 Coordinator 组件。
8.  如果未通过合法性检查，且错误原因是 Coordinator 正在加载，则封装 [REBALANCE\_IN\_PROGRESS](http://rebalance_in_progress/) 异常，并调用回调函数返回，因为本次 Rebalance 的所有状态都丢失了，所以需要让消费者重新加入消费者组。
9.  如果是其它错误，则封装对应错误，并调用回调函数返回。
10.  否则，获取消费者组元数据。
11.  如果未找到，则封装 [UNKNOWN\_MEMBER\_ID](http://unknown_member_id/) 异常，并调用回调函数返回。
12.  如果找到的话，则调用 [doSyncGroup](http://dosyncgroup/) 方法执行组同步任务。

## **2.3.3.2 GroupCoordinator#doSyncGroup()**

// 处理SyncGroupRequest请求的具体逻辑

private def doSyncGroup(group: GroupMetadata,

generationId: Int,

memberId: String,

protocolType: Option\[String\],

protocolName: Option\[String\],

groupInstanceId: Option\[String\],

groupAssignment: Map\[String, Array\[Byte\]\],

requestLocal: RequestLocal,

responseCallback: SyncCallback): Unit = {

// 在GroupMetadata对象上加锁，确保同一时间只能有一个线程修改组的状态

group.inLock {

// 校验SyncGroupRequest请求的合法性

val validationErrorOpt \= validateSyncGroup(

group,

generationId,

memberId,

protocolType,

protocolName,

groupInstanceId

)

validationErrorOpt match {

// 如果校验失败，返回带有错误信息的SyncGroupResult响应

case Some(error) => responseCallback(SyncGroupResult(error))

// 否则，根据组的当前状态进行不同的处理

case None \=\> group.currentState match {

// 如果组是Empty状态，返回UNKNOWN\_MEMBER\_ID错误响应

case Empty \=\>

responseCallback(SyncGroupResult(Errors.UNKNOWN\_MEMBER\_ID))

// 如果组处于PreparingRebalance状态，返回REBALANCE\_IN\_PROGRESS错误响应

case PreparingRebalance \=\>

responseCallback(SyncGroupResult(Errors.REBALANCE\_IN\_PROGRESS))

// 如果组处于CompletingRebalance状态，处理组的分配结果并进行下一步的状态转换

case CompletingRebalance \=\>

// 更新成员的awaitingSyncCallback属性

group.get(memberId).awaitingSyncCallback = responseCallback

removePendingSyncMember(group, memberId)

// 如果当前成员是组的Leader成员，则尝试持久化分配结果并转换为Stable状态

if (group.isLeader(memberId)) {

// 打印日志，记录Leader收到的分配结果信息

info(s"Assignment received from leader $memberId for group ${group.groupId} for generation ${group.generationId}. " +

s"The group has ${group.size} members, ${group.allStaticMembers.size} of which are static.")

// 如果有成员没有被分配任何消费方案，则创建一个空的方案

val missing \= group.allMembers.diff(groupAssignment.keySet)

val assignment \= groupAssignment ++ missing.map(\_ -> Array.empty\[Byte\]).toMap

if (missing.nonEmpty) {

warn(s"Setting empty assignments for members $missing of ${group.groupId} for generation ${group.generationId}")

}

// 将分配结果持久化存储，并根据存储结果进行状态转换

groupManager.storeGroup(group, assignment, (error: Errors) => {

group.inLock {

// 可能存在在等待存储结果的过程中，有其他成员加入了组，所以需要确保仍然处于CompletingRebalance状态且处于相同的generationId

if (group.is(CompletingRebalance) && generationId == group.generationId) {

// 如果有错误

if (error != Errors.NONE) {

// 清空分配方案并发送给所有成员

resetAndPropagateAssignmentError(group, error)

// 准备开启新一轮的Rebalance

maybePrepareRebalance(group, s"Error $error when storing group assignment during SyncGroup (member: $memberId)")

} else {

// 在消费者组元数据中保存分配方案并发送给所有成员

setAndPropagateAssignment(group, assignment)

// 变更消费者组状态到 Stable

group.transitionTo(Stable)

}

}

}

}, requestLocal)

groupCompletedRebalanceSensor.record()

}

// 如果组处于Stable状态，则返回当前的分配结果

case Stable \=\>

removePendingSyncMember(group, memberId)

// 返回当前成员的分配结果

val memberMetadata \= group.get(memberId)

responseCallback(SyncGroupResult(group.protocolType, group.protocolName, memberMetadata.assignment, Errors.NONE))

completeAndScheduleNextHeartbeatExpiration(group, group.get(memberId))

// 如果组处于Dead状态，则抛出异常

case Dead \=\>

throw new IllegalStateException(s"Reached unexpected condition for Dead group ${group.groupId}")

}

}

}

}

该方法是Kafka消费者组协调器处理 [SyncGroupRequest](http://syncgrouprequest/) 请求，核心步骤如下：

1.  首先对 [SyncGroupRequest](http://syncgrouprequest/) 的参数进行验证，然后根据组的当前状态进行不同的处理：
2.  如果消费者组为 Empty 状态，返回 [UNKNOWN\_MEMBER\_ID](http://unknown_member_id/) 错误响应。
3.  如果消费者组处于 PreparingRebalance 状态，返回 [REBALANCE\_IN\_PROGRESS](http://rebalance_in_progress/) 错误响应。
4.  如果消费者组处于 CompletingRebalance 状态，将成员的 AwaitingSyncCallback 属性设置为当前的SyncCallback，并处理组的分配结果，尝试将组转换为 Stable 状态。
5.  如果消费者组处于 Stable 状态，从组中获取成员的分配结果，并返回给调用方。
6.  如果消费者组处于 Dead 状态，抛出异常。

## **03 总结**

这里，我们一起来总结一下这篇文章的重点。

1、带你剖析了「**消费者重平衡机制**」的触发条件以及发生场景。

2、带你深度剖析了「**消费者重平衡机制**」的三大阶段的源码实现，从「**客户端发送请求**」到「**服务端协调器接收并处理请求**」。

下篇我们来深度剖析「**订阅状态、offset 操作**」，大家期待，我们下期见。