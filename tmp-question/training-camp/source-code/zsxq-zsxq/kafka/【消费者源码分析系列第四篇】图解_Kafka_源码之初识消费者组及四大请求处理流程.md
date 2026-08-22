大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka** **消费者如何拉取数据的**」，了解了 Kafka 消费者是如何跟服务端通信的并进行拉取数据的，今天我们开启消费端源码的征程，这是第四篇来深度聊聊「**Kafka 消费者组**」，看看消费者组是做什么的，内部又有什么组件。

![](https://article-images.zsxq.com/Fqhil3-XpFEnXHdkWtQVa8VMjl8p)

## **01 总体概述**

我们知道 Kafka 是一款「**高吞吐量**」、「**低延迟**」、「**高并发**」、「**高可扩展性**」的消息队列产品， 那么如果某个 Topic 拥有数百万到数千万的数据量， 仅仅依靠 Consumer 进程消费， 消费速度可想而知， 所以需要一个扩展性较好的机制来保障消费进度， 这个时候 Consumer Group 应运而生， **Consumer Group 是 Kafka 提供的可扩展且具有容错性的消费者机制****。**

这里我们来回顾下整个 Kafka 消费模型的几个特点：

1.  Kafka 的每个 Consumer（消费者）实例属于一个 ConsumerGroup（消费组），每个 Consumer Group 拥有一个公共且唯一的 Group ID。
2.  在消费时，ConsumerGroup 中的每个 Consumer 会独占一个或多个 Partition（分区）。
3.  对于每个 ConsumerGroup，在任意时刻，每个 Partition 至多有 1 个 Consumer 能消费。
4.  每个 ConsumerGroup 都有一个 Coordinator(协调者）负责分配 Consumer 和 Partition 的对应关系，当 Partition 或是 Consumer 发生变更是，会触发 rebalance（重新分配）过程，重新分配 Consumer 与 Partition 的对应关系。
5.  Consumer 维护与 Coordinator 之间的心跳，这样 Coordinator 就能感知到 Consumer 的状态，在 Consumer 故障的时候及时触发 rebalance。

本文涉及的源码：

「**KafkaConsumer**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/KafkaConsumer.java](https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/KafkaConsumer.java)

「**ConsumerCoordinator**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java](https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java)

「**AbstractCoordinator**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/AbstractCoordinator.java](https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/AbstractCoordinator.java)

## **02 初识消费者组**

在上一篇 [【消费者源码分析系列第三篇】图解 Kafka 源码之消费者如何拉取数据的](https://articles.zsxq.com/id_to39nipv3p2g.html) 中，在消费者拉取消息之前需要先进行确定「**消费者组协调器**」，然后向该 「**消费者组协调器**」发送请求「**加入消费者组**」，此时会进行分区分配。

关于消费者组的相关组件，在 Kafka 中主要有两个，其中在消费者端是：「**消费者组协调器 ConsumerCoordinator**」、在服务端是 「**组协调器 GroupCoordinator**」。

在 KafkaConsumer 初始化的时候主要就是这两个组件进行「**网络请求**」，「**完成消费者加入**」、「**分区消费分配策略制定**」等工作。

## **2.1 消费者以及消费者组初始化**

这里通过一张图来说明其初始化流程。

![](https://article-images.zsxq.com/Fte6QNbB1JccyD3kFuP1t2Wp6VSD)

整个流程如下：

1.  每个 KafkaConsumer 刚启动的时候，并不知道整个消费者组到底有多少个消费者，为了能让所有的消费者都找到同一个 [GroupCoodinator](http://groupcoodinator/) 进行报到，通过一次网络请求（发送 [FIND\_COORDINATOR](http://find_coordinator/) 给我自己认为的负载最小的 Broker）找到目标 [GroupCoodinator](http://groupcoodinator/)。
2.  每个 Consumer 启动的时候，内部会启动一个 [ConsumerCoordinator](http://consumercoordinator/) 组件，负责和 Broker 中的 [GroupCoodinator](http://groupcoodinator%20/) 进行通信。比如发送 [JOIN\_GROUP](http://join_group/) 请求给目标 [GroupCoodinator](http://groupcoodinator/) 用来请求加入「**消费者组**」。
3.  当 Consumer 发送 [JOIN\_GROUP](http://join_group/) 给 Broker，Broker 上的 KafkaApis 会把请求转交给 GroupCoodinator 来处理。[GroupCoodinator](http://groupcoodinator/) 处理完之后，会返回一个 [JoinGroupResult](http://joingroupresult/) 响应给 [ConsumerCoordinator](http://consumercoordinator/) ，[JoinGroupResult](http://joingroupresult%20/) 会**标识当前 Consumer 是否被选中了 Leader 了，如果是 Leader 的话，则负责制定该消费者组的消费方案**。
4.  [GroupCoodinator](http://groupcoodinator/) 返回要被消费的 Topic 的元数据给 [Leader Consumer](http://leader%20consumer/) 。
5.  [Leader Consumer](http://leader%20consumer/) 会为该消费者组制定消费方案，核心就是调用：[PartitionAssignor#assign()](http://partitionassignor/#assign\(\)) 方法来完成消费方案的制定，最后生成一个 [GroupAssignment](http://groupassignment/)。
6.  [Leader Consumer](http://leader%20consumer/) 会将最终制定的消费方案 [List<SyncGroupRequestData.SyncGroupRequestAssignment>](http://listsyncgrouprequestdata.syncgrouprequestassignment/) 通过发送 [SYNC\_GROUP](http://sync_group/) 请求给发送给 [GroupCoordinator](http://groupcoordinator/)。
7.  [GroupCoodinator](http://groupcoodinator%20/) 接下来就会把收到的消费方案，发送给这个消费者组的各个 Consumer，这样每个消费者就清楚知道自己要去消费哪个分区了。

## **2.2 两个要点细节**

在整个消费者与「**消费者组协调器 ConsumerCoordinator**」以及「**组协调器 GroupCoordinator**」交互流程中有两个要点细节需要你理解的，如下：

1\. 对于同一个消费者组下的所有消费者发送的 [JONG\_GROUP](http://jong_group/) 请求，是如何做到都发给同一个 [GroupCoordinator](http://groupcoordinator%20/) 的，或者说如何确定哪台 Broker 上的 [GroupCoordinator](http://groupcoordinator/) 组件为该消费者组服务呢？

这个问题在之前的原理篇 [【原理分析系列第五篇】图解 Kafka Consumer 架构设计](https://articles.zsxq.com/id_xqh2l0kkhc7a.html) 中就已经剖析过，这里再来说一下。

[GroupCoordinator](http://groupcoordinator/) 组件是唯一操作位移主题的组件，它在内部对位移主题进行读写操作。每个 Broker 在启动时，都会启动 [GroupCoordinator](http://groupcoordinator/) 组件，但是一个消费者组只能被一个 [GroupCoordinator](http://groupcoordinator/) 组件所管理。

位移主题某个特定分区 Leader 副本所在的 Broker 被选定为指定消费者组的 Coordinator。其规则就是：按照消费者组 ID 的 [hashCode](http://hashcode/) 取模后除以 [\_\_consumer\_offsets](http://__consumer_offsets/) 的分区个数，计算得到 [BrokerId](http://brokerid/) ，那么该 Broker 上的 [GroupCoordinator](http://groupcoordinator/) 负责为该消费者组进行提供服务。

![](https://article-images.zsxq.com/FrQQ0pMFvKp_Q7t2cTOqxS_qDQ3W)

def partitionFor(groupId: String): Int = Utils.abs(groupId.hashCode) % groupMetadataTopicPartitionCount

2\. 如果该消费者组中有消费者宕机，那么就会导致有 Partition 不会被消费，此时应该如何做容错呢？

也很简单，其实在每个消费者的内部，都会专门启动一个 [HeartbeatThread](http://heartbeatthread/) 心跳线程，负责每隔 [heartbeat.interval.ms = 3000](http://heartbeat.interval.ms%20=%203000/) 发送 [HEARTBEAT](http://heartbeat%20/) 请求给 [GroupCoodinator](http://groupcoodinator/) 告知自己还存活。如果该消费者和 Broker 之间的 Session 过期了，即超过了 [session.timeout.ms = 45000](http://session.timeout.ms%20=%2045000/) ，则该消费者会被 [GroupCoodinator](http://groupcoodinator/) 移除。从而触发 [Rebalance](http://rebalance/)。如果消费者拉取数据超时，即超过了 [max.poll.interval.ms = 300000 (5 分钟)](http://max.poll.interval.xn--ms%20=%20300000%20\(5%20\)-2693b8268g/) ，那么该消费者也会被 [GroupCoodinator](http://groupcoodinator%20/) 移除，从而触发 Rebalance。

## **2.3 四个请求**

在整个消费者与「**消费者组协调器 ConsumerCoordinator**」以及「**组协调器 GroupCoordinator**」交互流程中涉及到了 4 种请求：

1.  首先当 Consumer 启动的时候，先发送 [FIND\_COORDINATOR](http://find_coordinator/) 请求给负载最小的 Broker，该 Broker 会根据 group.id 计算得到为该 [ConsumerGroup](http://consumergroup%20/) 提供服务的 [GroupCoordinator](http://groupcoordinator%20/) 所在的 [BrokerId](http://brokerid/)，返回给 Consumer。
2.  接着当 Consumer 经过第一次请求获取到目标 BrokerId 后，向 [GroupCoordinator](http://groupcoordinator/) 发送 [JONG\_GROUP](http://jong_group%20/) 请求加入消费者组。
3.  接着当 Consumer 制定完消费方案之后，发送 [SYNC\_GROUP](http://sync_group/) 将消费方案发送给 [GroupCoodinator](http://groupcoodinator/)，然后 [GroupCoodinator](http://groupcoodinator%20/) 会发送消费方案给每个消费者。
4.  另外在 Consumer 内启动的 [HeartbeatThread](http://heartbeatthread%20/) 会每隔 [heartbeat.interval.ms = 3000](http://heartbeat.interval.ms%20=%203000/) 发送 [HEARTBEAT](http://heartbeat%20/) 心跳请求给 [GroupCoodinator](http://groupcoodinator/) 告知自己还存活。

## **03 消费者组四大请求源码流程**

**这部分的源码在上篇中或多或少都已经剖析过，这里再来总结梳理下，帮助你更好的理解其流程**。

相信大家看完上篇后会感觉源码很多，这里我就不列那么多，只是列出骨干代码。

## **3.1 入口**

我们在调用 KafkaConsumer API 进行拉取数据时，调用了 [KafkaConsumer.poll](http://kafkaconsumer.poll/)() 方法，该方法内部开始发请求「**注册自己**」以及「**加入消费者组**」等。

public class KafkaConsumer<K, V> implements Consumer<K, V> {

public ConsumerRecords<K, V> poll(final Duration timeout) {

return poll(time.timer(timeout), true);

}

private ConsumerRecords<K, V> poll(final Timer timer, final boolean includeMetadataInTimeout) {

acquireAndEnsureOpen();

try {

....

do {

....

if (includeMetadataInTimeout) {

// 消费者组的初始化

updateAssignmentMetadataIfNeeded(timer, false);

}

....

} while (timer.notExpired()); // 只要不超出过期时间就一直拉取

return ConsumerRecords.empty();

} finally {

....

}

}

}

在 [poll()](http://poll\(\)/) 方法内部会循环拉取数据，直到超出过期时间结束。在该方法中，我们重点关注 [updateAssignmentMetadataIfNeeded(timer, false)](http://updateassignmentmetadataifneeded\(timer,%20false\)/) 方法，其内部实现了「**消费者组初始化工作**」。

// kafkaConsumer 类方法

boolean updateAssignmentMetadataIfNeeded(final Timer timer, final boolean waitForJoinGroup) {

// 这里调用 ConsumerCoordinator 来继续处理

if (coordinator != null && !coordinator.poll(timer, waitForJoinGroup)) {

return false;

}

return updateFetchPositions(timer);

}

// ConsumerCoordinator.java

public boolean poll(Timer timer, boolean waitForJoinGroup) {

....

if (subscriptions.hasAutoAssignedPartitions()) {

....

// 发送心跳请求，这里会检测是否需要发送心跳，

pollHeartbeat(timer.currentTimeMs());

// 如果当前还没有找到消费者所在的消费者组协调器，则需调用父类 AbstractCoordinator#ensureCoordinatorReady() 方法确保和协调器建立连接

if (coordinatorUnknown() && !ensureCoordinatorReady(timer)) {

return false;

}

// 如果需要重新加入消费者组（rebalance）

if (rejoinNeededOrPending()) {

....

// 如果消费者还没有加入消费者组或者通过心跳监听到消费者组状态有变化，则需要调用父类 AbstractCoordinator#ensureActiveGroup() 方法等待消费者组分区分配完成

if (!ensureActiveGroup(waitForJoinGroup ? timer : time.timer(0L))) {

....

}

}

}

....

return true;

}

// AbstractCoordinator 类方法

boolean ensureActiveGroup(final Timer timer) {

// 1、发送 FIND\_COORDINATOR 请求给 Broker，获取该消费者组对应的 Broker 服务节点

if (!ensureCoordinatorReady(timer)) {

return false;

}

// 2、启动发送心跳的线程，每隔 3s 发送 HEARTBEAT 请求给目标 GroupCoordinator 所在的 Broker

startHeartbeatThreadIfNeeded();

// 3、发送 JOIN\_GROUP 请求给 FIND\_COORDINATOR 请求返回的 Borker 节点加入到消费者组中

// 4、SYNC\_GROUP 请求是在 JOIN\_GROUP 回调处理器中发送，并且是只有当选 Leader 的消费者才发送

return joinGroupIfNeeded(timer);

}

protected synchronized void pollHeartbeat(long now) {

if (heartbeatThread != null) {

if (heartbeatThread.hasFailed()) {

RuntimeException cause \= heartbeatThread.failureCause();

heartbeatThread = null;

throw cause;

}

// 如果需要发送心跳，则调用 AbstractCoordinator#notify 方法来唤醒 HeartbeatThread 线程发送心跳。

if (heartbeat.shouldHeartbeat(now)) {

notify();

}

heartbeat.poll(now);

}

}

可以得出 [ensureActiveGroup()](http://ensureactivegroup\(\)/) 方法内部可以很清晰看到发送 [FIND\_COORDINATOR](http://find_coordinator/)、[HEARTBEAT](http://heartbeat/)、[JOIN\_GROUP](http://join_group/) 这三个请求的方法，其实 [SYNC\_GROUP](http://sync_group%20/) 请求是在 [joinGroupIfNeeded()](http://joingroupifneeded\(\)/) 方法内部，在 [JOIN\_GROUP](http://join_group/) 请求发送成功后，回调函数内再次发送 [SYNC\_GROUP](http://sync_group/) 请求。

## **3.2 发送第一个请求 FIND\_COORDINATOR**

当「**确定消费者协调者完成**」后，会发送第一个请求 **FIND\_COORDINATOR**，如果通信一次发现该[GroupCoordinator](http://groupcoordinator/) 的信息还未获取到则继续重试，直到超时。这里的超时时间即为 poll 时传入的超时时间，这个时间会贯穿整个 consumer 的运行周期，源码如下：

// AbstractCoordinator 类方法

protected synchronized boolean ensureCoordinatorReady(final Timer timer) {

// 如果还未获取到 Coordinator 需要先与其进行通信

if (!coordinatorUnknown())

return true;

do {

....

// 1、将请求存入 ConsumerNetworkClient 内部的 unsent 队列中, 这里的请求类型是KafkaApis.FIND\_COORDINATOR

final RequestFuture<Void> future = lookupCoordinator();

// 2、轮询所有可以发送的请求，真正的发送。此时获取到每一个请求都绑定 SelectionKey.OP\_WRITE 事件

client.poll(future, timer);

// 如果还没回调完成则说明是超时的

if (!future.isDone()) {

// ran out of time

break;

}

....

// //如果与消费者组通信成功则会跳出循环

} while (coordinatorUnknown() && timer.notExpired());

return !coordinatorUnknown();

}

public boolean coordinatorUnknown() {

return checkAndGetCoordinator() == null;

}

从上可以得出 [coordinatorUnknown()](http://coordinatorunknown\(\)/) 方法会调用 [checkAndGetCoordinator()](http://checkandgetcoordinator\(\)/) 方法来检测并获取协调者信息。 通过代码跟踪你可以仔细思考一下，这里的 [coordinator](http://coordinator/) 是指获取到的协调器节点对象，[client.isUnavailable(coordinator)](http://client.isunavailable\(coordinator\)/) 是在与协调器建立连接，每次判断 [coordinator](http://coordinator/) 不为空且 client 与协调器连接失败，则将 [coordinator](http://coordinator/) 置空，**为什么会这样设计呢？**很有可能是请求发送到协调器的信息时发现该节点已下线或者不可用，此时服务端很有可能在进行选举，所以我们需要将 [coordinator](http://coordinator/) 清空，待服务端选举完成后再次通信。

protected synchronized Node checkAndGetCoordinator() {

if (coordinator != null && client.isUnavailable(coordinator)) {

markCoordinatorUnknown(true);

return null;

}

return this.coordinator;

}

这里先寻找的负载最小节点，然后与该节点通信获取协调者节点的信息。

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

private RequestFuture<Void> sendFindCoordinatorRequest(Node node) {

// 设置此次请求的类型：ApiKeys.FIND\_COORDINATOR

FindCoordinatorRequestData data \= new FindCoordinatorRequestData()

.setKeyType(CoordinatorType.GROUP.id())

.setKey(this.rebalanceConfig.groupId);

FindCoordinatorRequest.Builder requestBuilder \= new FindCoordinatorRequest.Builder(data);

// 将请求存储在 ConsumerNetworkClient#unsent 队列中队列中

return client.send(node, requestBuilder)

// 调用异步请求 RequestFuture#compose() 方法为其添加监听器，并将服务端响应的回调处理器 FindCoordinatorResponseHandler 封装在监听器中

.compose(new FindCoordinatorResponseHandler());

}

该步骤主要是构建 [FIND\_COORDINATOR](http://find_coordinator/) 请求并暂存到 [ConsumerNetworkClient#unsent](http://consumernetworkclient/#unsent%20) 队列中，我们继续来看第二步。

// 轮询所有可以发送的请求，真正的发送。此时获取到每一个请求都绑定 SelectorKey.OP\_WRITE 事件

public void poll(Timer timer, PollCondition pollCondition, boolean disableWakeup) {

....

lock.lock();

try {

....

// 发送我们现在可以发送的所有请求

long pollDelayMs \= trySend(timer.currentTimeMs());

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

可以看到第二步主要是立刻轮询所有可发送的请求，在 channel 上绑定 [SelectionKey.OP\_WRITE](http://selectionkey.op_write/) 事件，服务端自然会获取到该请求。

关于 NetworkClient 相关的知识，可以点击这里查看：[【生产者源码分析系列第九篇】图解 Kafka 源码之 NetworkClient 网络通信组件架构设计](https://articles.zsxq.com/id_k2pnfv2xq2wb.html)

另外第一步中的最后回调处理器 [FindCoordinatorResponseHandler.onSuccess()](http://findcoordinatorresponsehandler.onsuccess\(\)/) 方法内部处理了**成功的响应**，并将获取到得目标 [GroupCoordinator](http://groupcoordinator%20/) 所在的节点信息。

整个寻找协调器的过程如下图：

![](https://article-images.zsxq.com/lvXHu8x8Y8N6mgNq7tL5ovbNzNq5)

## **3.3 发送第二个请求 HEARTBEAT**

当你了解了第一个请求 **FIND\_COORDINATOR** 的流程之后，其实在 KafkaConsumer 中所有的请求发送流程几乎都一致，即调用统一的接口进行处理，不同的是每次请求 [ClientRequest](http://clientrequest%20/) 的实现不一样。

在 Consumer 客户端启动的时候，就会构建心跳监测线程 [HeartbeatThread](http://heartbeatthread/) 并启动。其中心跳监测线程名：[kafka-coordinator-heartbeat-thread|group.id](http://kafka-coordinator-heartbeat-thread%7Cgroup.id)，比如：[kafka-coordinator-heartbeat-thread | consumer0](http://kafka-coordinator-heartbeat-thread%20%7C%20consumer0)。

private synchronized void startHeartbeatThreadIfNeeded() {

if (heartbeatThread == null) {

heartbeatThread = new HeartbeatThread();

heartbeatThread.start();

}

}

虽然这里启动了, 但是在 run 方法里面有个逻辑标志位 [enabled=false](http://enabled=false/)，实际上这个时候并不会发出心跳监测的。

其实它会根据整个消费组的状态变化而变化。

## **3.3.1 启动心跳线程**

其实启动心跳是在消费者客户端发送第三个请求 **JOIN\_GROUP** 成功回调时设置 [enabled=true](http://enabled=true/) 的，如下可以看到 [JoinGroupRequest](http://joingrouprequest/) 回调的时候把客户端的状态流转为了 [COMPLETING\_REBALANCE](http://completing_rebalance/)，并启动心跳监测线程。

![](https://article-images.zsxq.com/FhGJ8vxjFK2ciuO8w4H74XbDH6aP)

## **3.3.2 发起心跳线程**

接着我们来看下该心跳线程内部究竟做了什么，是怎么运行以及发起心跳请求的，源码如下：

// 定义一个私有的内部类HeartbeatThread，继承自KafkaThread类，并实现AutoCloseable接口，用于实现一个心跳线程

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

while (true) {

// 获取 AbstractCoordinator 对象的同步锁，确保线程安全。

synchronized (AbstractCoordinator.this) {

....

// 如果还没有到发送 Heartbeat 请求的时间，则调用 AbstractCoordinator#wait 来阻塞 HeartbeatThread 线程。在上面 ConsumerCoordinator#poll 方法中会调用 AbstractCoordinator#pollHeartbeat 方法来检测是否需要发送心跳，如果需要则调用 AbstractCoordinator#notify 方法来唤醒 HeartbeatThread 线程发送心跳。

if (!enabled) {

AbstractCoordinator.this.wait();

continue;

}

// 如果到了发送 Heartbeat 请求的时间，则进行发送请求前的准备，更新时间，心跳间隔等，正常发送心跳请求通过 AbstractCoordinator#sendHeartbeatRequest() 方法触发，并在其回调处理器中做相应处理

heartbeat.sentHeartbeat(now);

// 1、HEARTBEAT 请求已就绪，存储在了 unsent 队列中

final RequestFuture<Void> heartbeatFuture = sendHeartbeatRequest();

....

}

}

}

....

}

}

// AbstractCoordinator#sendHeartbeatRequest

synchronized RequestFuture<Void> sendHeartbeatRequest() {

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

当你仔细观察这个过程会发现，相对第一个 [FIND\_COORDINATOR](http://find_coordinator/) 请求少了个 [client.poll()](http://client.poll\(\)/) 轮询的方法，也就是说在这些请求中 [HEARTBEAT](http://heartbeat/) 没有 [FIND\_COORDINATOR](http://find_coordinator/) 、[JOIN\_GROUP](http://join_group/)、[SYNC\_GROUP](http://sync_group/) 重要。

##   
**3.4 发送第三个请求 JOIN\_GROUP**

当「**消费者**」发送心跳请求保活，接下来会向协调器 [coordinator](http://coordinator/) 发起一个 **JOIN\_GROUP** 请求表示要「**加入消费者组**」，此时会调用 [joinGroupIfNeeded()](http://joingroupifneeded\(\)/)，该方法会在 while 循环中不断进行加入消费者组的尝试，直至成功，源码如下：

boolean joinGroupIfNeeded(final Timer timer) {

// 如果需要重新加入或正在等待重新加入，则继续循环

while (rejoinNeededOrPending()) {

....

// 1、准备请求，准备 JOIN\_GROUP 类型请求，存储到 unsent 队列中，调用 AbstractCoordinator#initiateJoinGroup() 方法生成加入消费者组的 JoinGroup 异步请求，并通过轮询等待响应

final RequestFuture<ByteBuffer> future = initiateJoinGroup();

// 2、调用 ConsumerNetworkClinet#poll() 方法发送请求，轮询处理待发送队列中的所有可发送的请求，这部分在上一节中有分析，不再赘述

client.poll(future, timer);

....

}

return true;

}

此时会先初始化 **JOIN\_GROUP** 请求，该请求触发在 [AbstractCoordinator#sendJoinGroupRequest()](http://abstractcoordinator/#sendJoinGroupRequest\(\))，如下：

private synchronized RequestFuture<ByteBuffer> initiateJoinGroup() {

// 1、为了避免在调用 poll 方法后被用户唤醒而错误地尝试重新加入消费者组，将 joinFuture 存储起来。

if (joinFuture == null) {

state = MemberState.PREPARING\_REBALANCE;

// 2、如果之前的重平衡失败，可能会连续触发重平衡，这种情况下不更新开始时间。

if (lastRebalanceStartMs == -1L)

lastRebalanceStartMs = time.milliseconds();

// 3、发送 JoinGroup 请求

joinFuture = sendJoinGroupRequest();

joinFuture.addListener(....);

}

return joinFuture;

}

RequestFuture<ByteBuffer> sendJoinGroupRequest() {

// 如果 coordinator 不可用，则返回 RequestFuture，其状态为未知协调器

if (coordinatorUnknown())

return RequestFuture.coordinatorNotAvailable();

// 向协调器发送 JoinGroup 请求，即构建请求参数

log.info("(Re-)joining group");

JoinGroupRequest.Builder requestBuilder \= new JoinGroupRequest.Builder(

new JoinGroupRequestData()

.setGroupId(rebalanceConfig.groupId)

// 客户端与 Broker 最大会话有效期, 如果超过这个时间没有任何心跳则可能发生重平衡，其配置属性：session.timeout.ms；默认10000（10 秒）

.setSessionTimeoutMs(this.rebalanceConfig.sessionTimeoutMs)

// 消费者的成员 Id， 默认就是空字符串

.setMemberId(this.generation.memberId)

// 从 Kafka 2.3 版本开始引入的新参数. 用户提供的消费者实例的唯一标识符。如果设置了,消费者则被认为静态成员, 此时会配较大的 session 超时设置能够避免因成员临时不可用（比如重启）而引发的 Rebalance，如果不设置, 消费者被认为动态成员。其配置属性：group.instance.id

.setGroupInstanceId(this.rebalanceConfig.groupInstanceId.orElse(null))

// 协议类型, 这里是 consumer, 可选项还有 connect

.setProtocolType(protocolType())

// 配置的分区分配策略和对应的订阅相关信息的

.setProtocols(metadata())

.setRebalanceTimeoutMs(this.rebalanceConfig.rebalanceTimeoutMs)

);

log.debug("Sending JoinGroup ({}) to coordinator {}", requestBuilder, this.coordinator);

// 由于重平衡超时时间是协调器可能会阻塞的最长时间，因此我们使用重平衡超时时间来覆盖请求超时时间。我们额外加了 5 秒用于解决可能出现的小延迟。即 joinGroup 请求的超时时间在 (request.timeout.ms 默认 30000（30 秒）) 和(max.poll.interval.ms 默认 30s + 5s ）中取最大值

int joinGroupTimeoutMs \= Math.max(client.defaultRequestTimeoutMs(),

rebalanceConfig.rebalanceTimeoutMs + JOIN\_GROUP\_TIMEOUT\_LAPSE);

// 向指定的 coordinator 节点发起请求

return client.send(coordinator, requestBuilder, joinGroupTimeoutMs)

.compose(new JoinGroupResponseHandler(generation));

}

## **3.4.1 请求参数**

通常来说，正常一个请求都包含如下数据：

![](https://article-images.zsxq.com/Fin2m_8Fb4L8tuglvFOWwrUC_45K)

接下来我们分别来看下 RequestHeader 以及 RequestBody 的组成部分。

## **3.4.1.1 RequestHeader**

1.  header\_version：表示请求头版本号, 目前只有 0 和 1; 每个请求对应使用哪个 headerVersion 的映射关系在[ApiMessageType#requestHeaderVersion](http://apimessagetype/#requestHeaderVersion) 中会有体现。
2.  2\. api\_version: 表示请求的标识ID，每个类型的请求都有它对应的唯一ID, 比如这里的 JoinGroupRequest 对应的ID是 11; 映射关系在 [ApiMessageType](http://apimessagetype/)。
3.  3\. api\_version: 该请求的版本号，因为我们可能会对某个请求类型做过改动, 并且改动了请求的 Schemas, 那么每次改动都是一个版本, 比如 [JoinGroupRequest](http://joingrouprequest/) 这个请求总共就有 6 个版本, 那么当前发起的请求的版本号是 ： Schema.length -1 = 6 - 1 = 5。
4.  下面的 JoinGroupRequest 的 Schemas ,  不同请求类型的 Schemas 不一样, 可以通过 ApiKeys 下面的每个请求查看：

![](https://article-images.zsxq.com/FhM_X0AyP5qy8ZoHt8KOdb6SIjRm)

![](https://article-images.zsxq.com/FvR5RsViTUyIyvdv7ZGE-5yAEaIO)

4\. client\_id: 表示客户端ID客户端唯一标识。

5\. correlation\_id: 表示每次发起的请求的唯一标识发起的每次请求的唯一标识, 该值会自增。

## **3.4.1.2 RequestBody**

上面的 RequestHeader 基本上都大差不差，但是不同 Request 类型的 RequestBody 是不一样的，对于 [JoinGroupRequest](http://joingrouprequest/) 的属性如下：

![](https://article-images.zsxq.com/lhlx8UJPJK8dHFiptt4ItAd_YnNP)

1.  group\_id：表示消费组id，属性 group.id 配置。
2.  session\_timeout\_ms：表示 session 超时时间，属性：session.timeout.ms  默认值 10000（10 秒）。消费者定期发送心跳证明自己的存活，如果在这个时间之内 Broker 没收到，那 Broker 就将此消费者从消费者组中移除，进行一次 Reblance。
3.  需要注意的是, 这个值必须在Broker属性 group.min.session.timeout.ms 和 group.max.session.timeout.ms 的值范围中间。
4.  member\_id：表示消费者成员ID, 默认就是空字符串, 客户端不可设置。该值会在后续的请求中返回并被赋值。
5.  group\_instance\_id：表示消费者组实例ID，属性：group.instance.id  默认值 空），从 Kafka 2.3 版本开始引入的新参数，用户提供的消费者实例的唯一标识符。如果设置了消费者则被认为静态成员, 此时会分配较大的 session 超时设置能够避免因成员临时不可用（比如重启）而引发的 Rebalance， 如果不设置, 消费者被认为动态成员。
6.  protocol\_type：表示协议类型，Consumer 发起的协议是 comsumer , 另一个可选项为 connect。
7.  rebalance\_timeout\_ms：表示重平衡的超时时间，属性： max.poll.interval.ms，默认值300000（5 分钟）。如果消费者两次 poll 的时间超过了此值,那就认为此消费者能力不足，将此消费者的 commit 标记为失败，并将此消费者从消费者组移除，触发一次 Reblance 将该消费者消费的分区分配给其他消费者。
8.  protocols：表示协议映射，该值保存着分区分配策略和对应的订阅元信息。 这里的name 表示分配策略的 name, 可选值有 \[range、roundrobin、sticky、cooperative-sticky、stream\]。metadata：里面保存着对应的元信息，比如 topics 为订阅的 topic、user\_data 为用户自定义数据等等。 ![](https://article-images.zsxq.com/Fqohi8xa0deP73N-3RD5N0Q5eNm2)

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

> 这里需要注意的是: userData 是用户自定义数据， 默认的range策略返回的是 null, 如果你想设置自己的数据, 需要实现方法 subscriptionUserData()。

## **3.4.2 向协调器发起请求**

既然要发起请求，那么究竟发往是哪个节点呢？之前客户端向集群发起请求的计算方式一般都是获取「**最小负载的节点**」发起请求。但是这里不一样，这里是向「**协调器 Coordinator**」发起请求。

那么问题来了，谁是「**协调器 Coordinator**」？「**协调器 Coordinator**」节点是哪个？

这个答案在文章开头的时候已经讲过，这里就不赘述，可以翻到上面查看。

> 这里需要注意的是：一个 Broker 可能有多个 Node, 使用哪个 Node 取决于客户端发起请求时候使用的是哪个listener。客户端发起请求对应的 listener 就对应着相应的 Node。如果 Leader 不存在或者不在线,会提示异常：The coordinator is not available。

这里需要注意的是，[JOIN\_GROUP](http://join_group/) 请求回调处理器是 [JoinGroupResponseHandler](http://joingroupresponsehandler/)，在该 handler 的 handle() 方法内部直接解析请求响应 [JoinGroupResponse](http://joingroupresponse/)，判断出当前消费者是不是当选为 [LeaderConsumer](http://leaderconsumer/)，如果是还需要制定分区分配策略，并发送 [SYNC\_GROUP](http://sync_group/) 请求给 [GROUP\_COORDINATOR](http://group_coordinator/) 所在的节点。

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

....

} else {

// 成功分支，处理加入组成功的情况

sensors.joinSensor.record(response.requestLatencyMs());

synchronized (AbstractCoordinator.this) {

// 修改状态，完成 再平衡

if (state != MemberState.PREPARING\_REBALANCE) {

future.raise(new UnjoinedGroupException());

} else {

state = MemberState.COMPLETING\_REBALANCE;

if (heartbeatThread != null)

heartbeatThread.enable();

AbstractCoordinator.this.generation = new Generation(

joinResponse.data().generationId(),

joinResponse.data().memberId(), joinResponse.data().protocolName());

// 判断自己是不是 leader

if (joinResponse.isLeader()) {

// 根据协调器返回的消费者组 leader 的 id 判断自身是否是消费者组 leader，如是当选 Leader 的话则调用 AbstractCoordinator#onJoinLeader() 进行指定分区分配策略，并再次发送 SYNC\_GROUP 请求

onJoinLeader(joinResponse).chain(future);

} else {

onJoinFollower().chain(future);

}

}

}

}

}

}

}

}

可以看到这里重点是调用 [onJoinLeader(joinResponse)](http://onjoinleader\(joinresponse\)/) 方法来当选 Leader，如果当选后需要发送第四个请求 [SYNC\_GROUP](http://sync_group/)。

##   
**3.5 发送第四个请求 SYNC\_GROUP**

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

// 2、准备请求 存储到 unsent 队列

return sendSyncGroupRequest(requestBuilder);

} catch (RuntimeException e) {

return RequestFuture.failure(e);

}

}

@Override

protected Map<String, ByteBuffer> performAssignment(String leaderId,

String assignmentStrategy, List<JoinGroupResponseData.JoinGroupResponseMember> allSubscriptions) {

// 首先调用 ConsumerCoordinator#lookupAssignor() 找到指定分配策略的分配器

ConsumerPartitionAssignor assignor \= lookupAssignor(assignmentStrategy);

....

isLeader = true;

// 调用分配器 ConsumerPartitionAssignor#assign() 方法执行分区分配，并获取分配结果。

// Leadre Consumer 负责制定消费方案：完成 50 个 Conusmer 到 50 个 Partition 消费关系的映射，RoundRobin, Range, Sticky, ....

Map<String, Assignment> assignments = assignor.assign(metadata.fetch(), new GroupSubscription(subscriptions)).groupAssignment();

....

// 最后，将分配结果序列化为ByteBuffer对象，并返回包含分配结果的Map<String, ByteBuffer>。

return groupAssignment;

}

这里第一步会制定分区分配策略，在 Kafka 2.8 版本之后默认使用 [RangeAssignor](http://rangeassignor/) 作为分区分配策略。

关于分区分配策略，可以点击查看： [【原理分析系列第十四篇】图解 Kafka 消费者分区分配策略](https://articles.zsxq.com/id_esdb8l8rwxb7.html)

第二步主要就是发送 [SYNC\_GROUP](http://sync_group/) 请求了，当请求发送之后，每个消费者就会在第三个请求 [JOIN\_GROUP](http://join_group/) 的响应中获取到最终分配给自己的消费分配策略。

最终完成整个分配方案的分发以及加入消费者组的流程。

##   
**04 总结**

这里，我们一起来总结一下这篇文章的重点。

1、通过「**场景驱动**」的方式从消费者调用出发，抛出消费者初始化后是如何加入消费者组以及如何进行分配分区策略的?

2、带你剖析了「**加入消费者组以及分配方案**」的整个流程，从「**发送第一个请求 FIND\_COORDINATOR**」、「**发送第二个请求 HEARTBEAT**」、「**发送第三个请求 JOIN\_GROUP**」、「**发送第四个请求 SYNC\_GROUP**」四个维度进行剖析和梳理。

下篇我们来深度剖析「**消费者组状态机流程**」，大家期待，我们下期见。