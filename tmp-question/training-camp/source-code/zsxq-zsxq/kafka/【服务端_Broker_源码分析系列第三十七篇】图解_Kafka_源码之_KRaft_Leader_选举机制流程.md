大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端** **KRaft 请求处理流程**」，了解了 Kafka 中「**KRaft**」状态、请求类型等梳理、另外剖析了「**KRaft**」请求处理流程，今天我们接着来深度聊聊「**Kafka 服务端 KRaft Leader 选举机制流程**」，看看 Kraft Leader 选举机制流程是如何处理的。

![](https://article-images.zsxq.com/Flwa5jAxJVKbqWEgy3_olAUM_KfX)

## **01 总体概述**

在 [【服务端 Broker 源码分析系列第三十五篇】图解 Kafka 源码之 KRaft 模块初探实现原理](https://articles.zsxq.com/id_7fp9s8us807i.html) 这篇中，我们剖析了 基于 Raft 算法 「**Leader 节点**」的选举机制原理，今天我们来剖析下「**KRaft**」集群中是如何利用 Raft 算法来实现选举 「**Leader 节点**」的源码实现。

本文涉及的源码：

「**KafkaRaftManager**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/raft/RaftManager.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/raft/RaftManager.scala)

「**KafkaRaftClient**」类源码在 Kafka 源码包的 raft 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/raft/src/main/java/org/apache/kafka/raft/KafkaRaftClient.java](https://github.com/apache/kafka/blob/3.0.0/raft/src/main/java/org/apache/kafka/raft/KafkaRaftClient.java)

「**KafkaNetworkChannel**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/raft/KafkaNetworkChannel.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/raft/RaftManager.scala)

「**InterBrokerSendThread**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/common/InterBrokerSendThread.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/raft/RaftManager.scala)

##   
**02 前置知识点**

我们直到在「**KRaft**」模式下 Kafka 集群的元数据是由「**Controller 节点**」 来自治的，则在分布式环境下必然要涉及到集群节点的交互，包括 「**集群选主**」、 「**集群元数据同步**」等操作，关键的请求交互如下：

1.  **Vote**：这是由 Candidate 候选者节点来发送的，请求其他节点为自己投票。
2.  **BeginQuorumEpoch**：这是由 Leader 节点来发送的，告知其他节点当前的 Leader 信息。
3.  **EndQuormEpoch**：这是由当前 Leader 退位时发送，触发重新选举。
4.  **Fetch**：这是由 Follower 节点来发送，用来复制 Leader 节点的日志数据，另外通过 Fetch 请求 Follower 也可以完成对 Leader 的探活。

而从集群元数据的维护角度来看，Kafka 集群中的每个节点都会是以下 3 种身份之一：

1.  **Leader**：整个 Kafka 集群的主节点，由具有 controller 角色并在 [controller.quorum.voters](http://controller.quorum.voters/) 配置的列表中的节点来担任，负责维护元数据的读写。
2.  **Follower（Voter）**：具有投票权的从节点，由具有 controller 角色并在 [controller.quorum.voters](http://controller.quorum.voters/) 配置的列表中的节点来担任，从 Leader 节点处同步集群元数据，并负责处理部分来自 Follower(Observer) 的集群元数据读请求。
3.  **Follower（Observer）**：没有投票权的从节点，从 Leader/Follower(Voter) 节点处同步元数据，包含以下两类节点：
4.  只具有 broker 角色的节点，需注意 broker 角色功能模块将通过监听集群元数据变化来进行对应创建分区等操作，负责消息数据的读写。
5.  具有 controller 角色但不在 [controller.quorum.voters](http://controller.quorum.voters/) 列表中的节点。

##   
**02 初始化 KRaft 状态**

在「**KRaft**」模式下选举「**Leader 节点**」主要有两个场景，接下来我们挨个剖析。

## **2.1 当集群刚启动时**

当集群刚启动时，需要选举一个「**Leader 节点**」。当「**KRaft**」模块启动时，会创建并初始化 「**KafkaRaftClient**」。

  
![](https://article-images.zsxq.com/FrZTLd561ew8A26Pxszf8Klfx7jy)

然后从 Cluster 主题「**\_\_cluster\_metadata**」中获取该节点的 Raft 状态，这个会在后面篇章单独剖析。

如果获取不到之前的状态，则将 Raft 状态先初始化为 「**UnattachedState**」状态，这在上一篇 [【服务端 Broker 源码分析系列第三十六篇】图解 Kafka 源码之 KRaft 请求处理流程](https://articles.zsxq.com/id_eal5tbquenj6.html) 中，我们已经剖析过了。接着调用 [KafkaRaftClient#pollUnattached](http://kafkaraftclient/#pollUnattached) 方法来处理。

## **2.1.1 pollUnattached()**

private long pollUnattached(long currentTimeMs) {

// 首先获取投票状态

UnattachedState state = quorum.unattachedStateOrThrow();

// 判断当前节点是否是 Controller 角色，以及是否在属性 controller.quorum.voters 配置的有选举权的节点列表中，如果存在的话则调用 KafkaRaftClient.java#pollUnattachedAsVoter() 方法，则按 Voter 的方式处理

if (quorum.isVoter()) { // quorum.isVoter 为 true 表示当前节点是 Controller 节点

return pollUnattachedAsVoter(state, currentTimeMs);

} else {

// 否则表示当前节点是 Broker 节点，则调用 KafkaRaftClient.java#pollUnattachedAsObserver() 方法，则按 Observer 的方式处理

return pollUnattachedAsObserver(state, currentTimeMs);

}

}

这里解释下在 Kafka Raft 协议中，节点分为两类：Voter 和 Observer。

1.  Voter 是有投票权的节点，它们可以参与 Leader 的选举，以及参与 Raft 协议的正常操作。在 Raft 协议中，Voter 有一定的能力来对集群进行操作，包括提交新的记录和对日志进行删减。
2.  Observer 是无投票权的节点，它们不参与 Leader 的选举。Observer 节点只能被动地同步复制 Leader 节点的日志，并不能主动对集群进行操作。
3.  Voter 和 Observer 节点的区别在于其是否参与 Leader 的选举以及日志的写入，Observer 节点通常用于读取集群数据，可以有效地减轻 Voter 节点的压力。

这里我们重点来剖析下跟我们 Leader 选举有关的 [pollUnattachedAsVoter](http://pollunattachedasvoter/) 的源码实现。

## **2.1.2 pollUnattachedAsVoter()**

private long pollUnattachedAsVoter(UnattachedState state, long currentTimeMs) {

// 如果存在正在进行的优雅关闭操作，则立即关闭

GracefulShutdown shutdown \= this.shutdown.get();

if (shutdown != null) {

// If shutting down, then remain in this state until either the

// shutdown completes or an epoch bump forces another state transition

return shutdown.remainingTimeMs();

} else if (state.hasElectionTimeoutExpired(currentTimeMs)) {

// 如果选举时间已超时，则转换为 Candidate 状态

transitionToCandidate(currentTimeMs);

return 0L;

} else {

return state.remainingElectionTimeMs(currentTimeMs);

}

}

该方法非常简单，可以看到核心其实是调用 [UnattachedState#hasElectionTimeoutExpired()](http://unattachedstate/#hasElectionTimeoutExpired\(\)) 方法判断当前节点的选举时间是否超时，如果超时则调用 [KafkaRaftClient.java#transitionToCandidate()](http://kafkaraftclient.java/#transitionToCandidate\(\)) 方法将当前节点的角色切换为「**候选者**」，在切换的过程中当前节点会给自己投一票。

## **2.1.3 transitionToCandidate()**

private void transitionToCandidate(long currentTimeMs) {

// 写入状态到本地 quorum-state 文件存储中

quorum.transitionToCandidate();

// 尝试 Leader 变更

maybeFireLeaderChange();

// 成为候选者状态

onBecomeCandidate(currentTimeMs);

}

private void onBecomeCandidate(long currentTimeMs) {

// 获取 quorum 的 CandidateState

CandidateState state \= quorum.candidateStateOrThrow();

// 如果无法转换为 Leader 状态，则重置所有连接并更新选举开始时间

if (!maybeTransitionToLeader(state, currentTimeMs)) {

resetConnections();

kafkaRaftMetrics.updateElectionStartMs(currentTimeMs);

}

}

其中调用 quorum.transitionToCandidate() 将 KafkaRaftClient#quorum 切换到新状态后，再将新状态的任期 + 1，则进入新的任期。

![](https://article-images.zsxq.com/FpT5WLmBVMemKwbTWgsfY1aW5IK2)

![](https://article-images.zsxq.com/FtBkVLh26RiNu7Ne48O5LJFQ6gHh)

当节点状态变更为「**候选者**」后，下一轮 [KafkaRaftClient.java#poll()](http://kafkaraftclient.java/#poll\(\)) 方法调用最终将触发 [KafkaRaftClient.java#pollCandidate()](http://kafkaraftclient.java/#pollCandidate\(\)) 方法。

![](https://article-images.zsxq.com/Fi-Bk2c6Ar5jlVS3EcFfbgFn3UHM)

这里总结下 **pollUnattached** 方法的核心逻辑如下：

1.  如果该节点是「**Controller 节点**」，则会等待「**Leader 节点**」的 **BeginQuorumEpoch** 请求，如果收到该请求后，则会切换到「**Follower**」状态，如果等待超时，则切换到 「**Candidate**」状态，发起选举流程。
2.  如果该节点是「**Broker 节点**」，则给任意一个「**Controller 节点**」发送 Fetch 请求，该「**Controller 节点**」会返回「**Leader 节点**」信息，该节点收到返回的 「**Leader 节点**」信息后，将切换到 「**Follower**」状态。

## **2.2 当 Leader 节点下线时**

当「**Leader 节点**」下线时，需要在「**Follower**」节点 选举一个新的「**Leader 节点**」。

假如此时「**Controller 节点**」处于「**Follower**」状态时，会调用 KafkaRaftClient#pollFollower 方法来执行对应的逻辑，源码如下：

private long pollFollower(long currentTimeMs) {

FollowerState state \= quorum.followerStateOrThrow();

// quorum.isVoter 为 true 表示当前节点是 Controller 节点则调用 KafkaRaftClient.java#pollFollowerAsVoter() 方法同步数据或者发起投票

if (quorum.isVoter()) {

// 1、同步数据或者发起投票

return pollFollowerAsVoter(state, currentTimeMs);

} else {

// 2、否则表示当前节点是 Broker 节点，则调用 KafkaRaftClient.java#pollFollowerAsObserver() 方法同步数据

return pollFollowerAsObserver(state, currentTimeMs);

}

}

该方法与 **pollUnattached** 方法逻辑类似，步骤如下：

1.  如果 [quorum.isVoter](http://quorum.isvoter/) 为 true 表示当前节点是 Controller 节点，如果存在的话则调用 [KafkaRaftClient.java#pollFollowerAsVoter](http://kafkaraftclient.java/#pollFollowerAsVoter)() 方法同步数据或者发起投票。
2.  否则表示当前节点是 Broker 节点，则调用 [KafkaRaftClient.java#pollFollowerAsObserver](http://kafkaraftclient.java/#pollFollowerAsObserver)() 方法同步数据。

## **2.2.1 pollFollowerAsVoter()**

private long pollFollowerAsVoter(FollowerState state, long currentTimeMs) {

// 如果存在正在进行的优雅关闭操作，则立即关闭

GracefulShutdown shutdown \= this.shutdown.get();

if (shutdown != null) {

// If we are a follower, then we can shutdown immediately. We want to

// skip the transition to candidate in any case.

return 0;

} else if (state.hasFetchTimeoutExpired(currentTimeMs)) {

// 1、首先是进行 Leader 节点探活，检查本节点上一次向 Leader 节点发起的 Fetch 请求响应时间是否已经超时过期，如果超时过期则 Leader 节点可能挂掉了，当前 Follower(Voter) 节点需进入 Candidate 状态。在 FollowerState#fetchTimer 中记录了 Leader 节点上次响应该节点 Fetch 请求的时间，根据该属性与当前时间的差可以判断出 Leader 节点是否超时了。

logger.info("Become candidate due to fetch timeout");

transitionToCandidate(currentTimeMs);

return 0L;

} else {

// 2、如果跟 Leader 节点连接正常，则调用 maybeSendFetchOrFetchSnapshot 方法向 Leader 节点发起同步元数据的 Fetch 请求。

long backoffMs \= maybeSendFetchOrFetchSnapshot(state, currentTimeMs);

return Math.min(backoffMs, state.remainingFetchTimeMs(currentTimeMs));

}

}

该方法与 **pollUnattachedAsVoter** 方法类似，步骤如下：

1.  首先是进行 Leader 节点探活，检查本节点上一次向 Leader 节点发起的 Fetch 请求响应时间是否已经超时过期，如果超时过期则 Leader 节点可能挂掉了，当前 Follower(Voter) 节点需进入 Candidate 状态。在 FollowerState#fetchTimer 中记录了 Leader 节点上次响应该节点 Fetch 请求的时间，根据该属性与当前时间的差可以判断出 Leader 节点是否超时了。
2.  如果跟 Leader 节点连接正常，则调用 maybeSendFetchOrFetchSnapshot 方法向 Leader 节点发起同步元数据的 Fetch 请求。

这里需要注意的是：「**Broker 节点**」与「**Controller 节点**」的处理逻辑的区别，「**Controller 节点**」可以切换到「**Candidate**」状态并发起选举流程，而「**Broker 节点**」只能主动查找「**Leader 节点**」，切换到 「**Follower**」状态，之后一直保持「**Follower**」状态。

##   
**03 发送投票请求**

当一个 「**Follower 节点**」进入 「**Candidate**」状态，该节点将发送 [VoteRequest](http://voterequest/) 请求给集群的其他节点，要求它们给自己投票，下面是源码流程。

## **3.1 pollCandidate()**

![](https://article-images.zsxq.com/FiY5Pd9Z9zBsKTFgfCf8HhbWmFuF)

步骤如下：

1.  如果 [state.isBackingOff()](http://state.isbackingoff\(\)/) 为 true，则表示该节点当前处于 [BackingOff](http://backingoff/) 阶段，则需要等待指定时间后再发起新的选举。
2.  如果 [state.hasElectionTimeoutExpired(currentTimeMs)](http://state.haselectiontimeoutexpired\(currenttimems\)/) 为 true，则表示该节点已经选举超时了(可能是选举瓜分等原因导致选举失败)，则该节点进入 [BackingOff](http://backingoff/) 阶段。
3.  最后正常情况下，发送 [VoteRequest](http://voterequest/) 请求，要求其他 [Controller](http://controller%20/) 节点给自己投票。

## **3.2 maybeSendVoteRequests()**

private long maybeSendVoteRequests(

CandidateState state,

long currentTimeMs

) {

// 只要仍然有机会赢得选举，就继续发送投票请求

if (!state.isVoteRejected()) {

return maybeSendRequests(

currentTimeMs,

// Vote 请求会被发送给每一个有投票权的节点

state.unrecordedVoters(),

this::buildVoteRequest

);

}

return Long.MAX\_VALUE;

}

private long maybeSendRequests(

long currentTimeMs,

Set<Integer> destinationIds,

Supplier<ApiMessage> requestSupplier

) {

long minBackoffMs \= Long.MAX\_VALUE;

// 遍历目标节点，尝试发送请求

for (Integer destinationId : destinationIds) {

long backoffMs \= maybeSendRequest(currentTimeMs, destinationId, requestSupplier);

if (backoffMs < minBackoffMs) {

minBackoffMs = backoffMs;

}

}

// 最小退避时间

return minBackoffMs;

}

private long maybeSendRequest(

long currentTimeMs,

int destinationId,

Supplier<ApiMessage> requestSupplier

) {

ConnectionState connection \= requestManager.getOrCreate(destinationId);

// 如果连接正在进行退避，则返回剩余的退避时间

if (connection.isBackingOff(currentTimeMs)) {

long remainingBackoffMs \= connection.remainingBackoffMs(currentTimeMs);

logger.debug("Connection for {} is backing off for {} ms", destinationId, remainingBackoffMs);

return remainingBackoffMs;

}

// 如果连接已准备就绪，则发送请求

if (connection.isReady(currentTimeMs)) {

int correlationId \= channel.newCorrelationId();

ApiMessage request \= requestSupplier.get();

// 生成 RaftRequest.Outbound 请求出站对象

RaftRequest.Outbound requestMessage \= new RaftRequest.Outbound(

correlationId,

request,

destinationId,

currentTimeMs

);

// 当请求完成时的回调处理逻辑

requestMessage.completion.whenComplete((response, exception) -> {

if (exception != null) {

ApiKeys api \= ApiKeys.forId(request.apiKey());

Errors error \= Errors.forException(exception);

ApiMessage errorResponse \= RaftUtil.errorResponse(api, error);

response = new RaftResponse.Inbound(

correlationId,

errorResponse,

destinationId

);

}

// 写入响应到消息队列中

messageQueue.add(response);

});

// 发送请求消息到通道，调用 KafkaNetworkChannel.scala#send() 方法将出站请求写入到队列中

channel.send(requestMessage);

logger.trace("Sent outbound request: {}", requestMessage);

connection.onRequestSent(correlationId, currentTimeMs);

return Long.MAX\_VALUE;

}

// 如果连接尚未准备就绪，则返回剩余的请求时间

return connection.remainingRequestTimeMs(currentTimeMs);

}

从上面源码可以看到经过层层检查，Vote 请求会被发送给每一个有投票权的节点，最终调用到的[maybeSendRequest](http://maybesendrequest/) 方法的核心逻辑如下：

1.  首先生成 [RaftRequest.Outbound](http://raftrequest.outbound/) 请求出站对象，并且设置这个请求完成后的回调方法，可以看到其回调中会把对端的响应包装成 [RaftResponse.Inbound](http://raftresponse.inbound/) 响应入站对象，并通过 [messageQueue.add()](http://messagequeue.add\(\)/) 调用将响应写入到消息队列中。
2.  发送请求消息到通道，调用 [KafkaNetworkChannel.scala#send](http://kafkanetworkchannel.scala/#send)() 方法将出站请求写入到队列中。

## **3.3 KafkaNetworkChannel#send()**

![](https://article-images.zsxq.com/Fk2LnDY7nW2zliNb7tIRnPb9RKFq)

这里需要注意的是，该方法定义异步请求的回调函数，当请求完成拿到对端响应后将会回调到此，最后通知[maybeSendRequest](http://maybesendrequest/) 方法的第一步。

## **3.4 RaftSendThread#sendRequest()**

这是 [KafkaNetworkChannel](http://kafkanetworkchannel/) 类的子类的方法。

def sendRequest(request: RequestAndCompletionHandler): Unit = {

// 写入请求到队列中

queue.add(request)

// 唤醒线程

wakeup()

}

该方法只是个请求入队的操作，在上篇中，我们提到了[RaftSendThread](http://raftsendthread%20/) [](http://raftsendthread%20/)的继承结构，可以知道这个线程启动后，核心逻辑在于 [InterBrokerSendThread.scala#doWor](http://interbrokersendthread.scala/#doWork)[k()](http://k\(\)/)。

## **3.5 InterBrokerSendThread#doWork()**

override def doWork(): Unit = {

pollOnce(Long.MaxValue)

}

protected def pollOnce(maxTimeoutMs: Long): Unit = {

try {

// 1、生成协议要求的请求对象

drainGeneratedRequests()

var now \= time.milliseconds()

// 2、将请求写入到底层网络客户端 NetworkClient 的发送缓冲区

val timeout \= sendRequests(now, maxTimeoutMs)

// 3、触发底层网络数据的收发

networkClient.poll(timeout, now)

now = time.milliseconds()

// 检测是否连接断开

checkDisconnects(now)

// 处理失败超时请求

failExpiredRequests(now)

unsentRequests.clean()

} catch {

....

}

}

可以看到 [doWork()](http://dowork\(\)/) 方法处理关键是调用 [pollOnce()](http://pollonce\(\)/) 方法，核心步骤如下：

1.  调用 [InterBrokerSendThread.scala#drainGeneratedRequests()](http://interbrokersendthread.scala/#drainGeneratedRequests\(\)%20) 方法生成协议要求的请求对象。
2.  调用 [InterBrokerSendThread.scala#sendRequests()](http://interbrokersendthread.scala/#sendRequests\(\)) 方法将请求投入到底层网络客户端 NetworkClient 的发送缓冲区。
3.  调用 [NetworkClient.java#poll()](http://networkclient.java/#poll\(\)) 方法触发底层网络数据的收发。

## **3.6 InterBrokerSendThread#drainGeneratedRequests()**

private def drainGeneratedRequests(): Unit = {

// 将上层发送队列中的请求数据存入 Buffer 对象

generateRequests().foreach { request =>

// 将请求添加到待发送请求队列中

unsentRequests.put(request.destination,

// 将上层请求转化为网络客户端请求对象 ClientRequest，并将其存入集合中

networkClient.newClientRequest(

request.destination.idString,

request.request,

request.creationTimeMs,

true,

requestTimeoutMs,

request.handler

))

}

}

该方法关键处理分为两步：

1.  调用子类 [RaftSendThread.scala#generateRequests()](http://raftsendthread.scala/#generateRequests\(\)) 将上层发送队列中的请求数据存入 Buffer 对象。
2.  调用 [NetworkClient.java#newClientRequest()](http://networkclient.java/#newClientRequest\(\)) 方法将上层请求转化为网络客户端请求对象 [ClientRequest](http://clientrequest/)，并将其存入集合中。

## **3.7 RaftSendThread#generateRequests()**

这是 [KafkaNetworkChannel](http://kafkanetworkchannel/) 类的子类的方法，源码非常简单，就是不断地出队请求最后添加到 Buffer 对象中。

def generateRequests(): Iterable\[RequestAndCompletionHandler\] = {

val buffer \= mutable.Buffer\[RequestAndCompletionHandler\]()

while (true) {

// 不停的出队请求

val request \= queue.poll()

if (request == null) {

return buffer

} else {

// 最终将请求添加到 Buffer 对象中

buffer += request

}

}

buffer

}

## **3.8 InterBrokerSendThread#sendRequests()**

接着我们回头来剖析下 **3.5** 的第二步的方法，源码如下：

private def sendRequests(now: Long, maxTimeoutMs: Long): Long = {

var pollTimeout \= maxTimeoutMs

// 循环从待发送请求集合中取出请求

for (node <- unsentRequests.nodes.asScala) {

val requestIterator \= unsentRequests.requestIterator(node)

while (requestIterator.hasNext) {

// 迭代下一个请求

val request \= requestIterator.next

if (networkClient.ready(node, now)) {

// 接着通过 NetworkClient.java#send() 方法将其存入网络客户端的发送缓冲区

networkClient.send(request, now)

// 发送完成进行移除

requestIterator.remove()

} else

pollTimeout = Math.min(pollTimeout, networkClient.connectionDelay(node, now))

}

}

pollTimeout

}

至此 VoteRequest 投票请求就已经发送出去了。

##   
**04 初探投票流程**

当 「**Controller 节点**」收到 [VoteRequest](http://voterequest%20/) 请求后，是如何发送请求的节点投票的呢？接下来我们就来揭晓。

## **4.1 handleVoteRequest()**

此时 [KafkaRaftClient#handleInboundMessage](http://kafkaraftclient/#handleInboundMessage\(\))[()](http://kafkaraftclient/#handleInboundMessage\(\)) 方法会调用 [KafkaRaftClient#handleVoteRequest](http://kafkaraftclient/#handleVoteRequest\(\))[()](http://kafkaraftclient/#handleVoteRequest\(\)) 方法来处理 [VoteRequest](http://voterequest%20/) 请求，源码如下：

  
![](https://article-images.zsxq.com/FvHqBKhMY8J39pH_RXXp60NW78Qp)

private VoteResponseData handleVoteRequest(

RaftRequest.Inbound requestMetadata

) {

// 1、获取投票请求，校验请求携带过来的参数，如果不合法直接响应返回

VoteRequestData request \= (VoteRequestData) requestMetadata.data;

if (!hasValidClusterId(request.clusterId())) {

return new VoteResponseData().setErrorCode(Errors.INCONSISTENT\_CLUSTER\_ID.code());

}

if (!hasValidTopicPartition(request, log.topicPartition())) {

// Until we support multi-raft, we treat individual topic partition mismatches as invalid requests

return new VoteResponseData().setErrorCode(Errors.INVALID\_REQUEST.code());

}

// 获取请求中 PartitionData 的信息

VoteRequestData.PartitionData partitionRequest \=

request.topics().get(0).partitions().get(0);

// 2、获取请求中的 candidateId 、任期 candidateEpoch 、最新日志的任期 lastEpoch、偏移量 lastEpochEndOffset

int candidateId \= partitionRequest.candidateId();

int candidateEpoch \= partitionRequest.candidateEpoch();

int lastEpoch \= partitionRequest.lastOffsetEpoch();

long lastEpochEndOffset \= partitionRequest.lastOffset();

if (lastEpochEndOffset < 0 || lastEpoch < 0 || lastEpoch >= candidateEpoch) {

return buildVoteResponse(Errors.INVALID\_REQUEST, false);

}

Optional<Errors> errorOpt = validateVoterOnlyRequest(candidateId, candidateEpoch);

if (errorOpt.isPresent()) {

return buildVoteResponse(errorOpt.get(), false);

}

// 3、如果发送请求节点的任期大于当前节点的任期，则当前节点切换到 Unattached 状态，表示当前节点状态已失效。

if (candidateEpoch > quorum.epoch()) {

transitionToUnattached(candidateEpoch);

}

OffsetAndEpoch lastEpochEndOffsetAndEpoch \= new OffsetAndEpoch(lastEpochEndOffset, lastEpoch);

// 3、判断是否可以给请求节点进行投票，这里调用 UnattachedState#canGrantVote，如果 VoteRequest 请求的 lastEpoch、lastEpochEndOffet 参数大于或等于当前节点最新日志的任期、偏移量，则当前节点同意给发送请求的节点进行投票。

boolean voteGranted \= quorum.canGrantVote(candidateId, lastEpochEndOffsetAndEpoch.compareTo(endOffset()) >= 0);

// 4、如果当前节点同意投票，则切换到 Vote 状态，表示当前节点已经给其他节点投票了，所以当前节点当前不会发起选举流程，避免【选票瓜分】导致选举失败，一直等到当前的选举超时（当前节点切换到 Candidate 状态），或者收到新的心跳请求【当前节点成为 follower 节点】

if (voteGranted && quorum.isUnattached()) {

transitionToVoted(candidateId, candidateEpoch);

}

logger.info("Vote request {} with epoch {} is {}", request, candidateEpoch, voteGranted ? "granted" : "rejected");

// 5、最后返回投票结果，voteGranted 标识表示单曲节点同意给请求节点进行投票

return buildVoteResponse(Errors.NONE, voteGranted);

}

该方法主要用来**发送 VoteRequest 投票请求的**，步骤如下：

1.  获取投票请求，校验请求携带过来的参数，如果不合法直接响应返回。
2.  获取请求中的 candidateId 、任期 candidateEpoch 、最新日志的任期 lastEpoch、偏移量 lastEpochEndOffset 。
3.  如果发送请求节点的任期大于当前节点的任期，则当前节点切换到 Unattached 状态，表示当前节点状态已失效。**需注意，这是回退机制的重要基础**。
4.  判断是否可以给请求节点进行投票，这里调用 UnattachedState#canGrantVote，如果 VoteRequest 请求的 lastEpoch、lastEpochEndOffet 参数大于或等于当前节点最新日志的任期、偏移量，则当前节点同意给发送请求的节点进行投票。
5.  **如果当前节点同意进行投票，则切换到 Vote 状态**，表示当前节点已经给其他节点投票了，所以当前节点当前不会发起选举流程，避免【选票瓜分】导致选举失败，一直等到当前的选举超时（当前节点切换到 Candidate 状态），或者收到新的心跳请求【当前节点成为 follower 节点】。
6.  最后返回投票结果，voteGranted 标识表示单曲节点同意给请求节点进行投票。

## **4.2 handleVoteResponse()**

接下来我们剖析 **当选 Leader 节点** 的源码流程。

我们将视角切回到发送 [VoteRequest](http://voterequest/) 请求的节点，当节点处理完 [VoteRequest](http://voterequest/) 请求，其响应将被 **3.2 节**提到的请求完成回调投入到消息队列中，并最终触发当前节点的 [KafkaRaftClient#handleVoteResponse](http://kafkaraftclient.java/#handleVoteResponse)() 方法来处理其他节点返回的投票响应，源码如下：

  
![](https://article-images.zsxq.com/Fpk4Qrwl9EvTXIPX5rCzSR431gEL)

private boolean handleVoteResponse(

RaftResponse.Inbound responseMetadata,

long currentTimeMs

) {

// 获取远端节点id

int remoteNodeId \= responseMetadata.sourceId();

// 获取投票响应

VoteResponseData response \= (VoteResponseData) responseMetadata.data;

// 校验响应是否有错误

Errors topLevelError \= Errors.forCode(response.errorCode());

if (topLevelError != Errors.NONE) {

return handleTopLevelError(topLevelError, responseMetadata);

}

// 检查请求中的 TopicPartition 是否与日志中的相匹配

if (!hasValidTopicPartition(response, log.topicPartition())) {

return false;

}

VoteResponseData.PartitionData partitionResponse \=

response.topics().get(0).partitions().get(0);

Errors error \= Errors.forCode(partitionResponse.errorCode());

// 获取响应 Leader 节点id

OptionalInt responseLeaderId \= optionalLeaderId(partitionResponse.leaderId());

// 获取 LeaderEpoch 信息

int responseEpoch \= partitionResponse.leaderEpoch();

// 定义响应回调

Optional<Boolean> handled = maybeHandleCommonResponse(

error, responseLeaderId, responseEpoch, currentTimeMs);

if (handled.isPresent()) {

return handled.get();

} else if (error == Errors.NONE) {

if (quorum.isLeader()) {

logger.debug("Ignoring vote response {} since we already became leader for epoch {}", partitionResponse, quorum.epoch());

// 1、如果执行到这里，表示其他节点返回了正常的投票响应

} else if (quorum.isCandidate()) {

// 获取 candidate 状态

CandidateState state \= quorum.candidateStateOrThrow();

// 如果其他节点返回了 voteGranted 标识，则标识其他节点给当前节点投的是赞成票

if (partitionResponse.voteGranted()) {

// 调用 CandidateState#recordGrantedVote 方法记录返回该投票响应的节点

state.recordGrantedVote(remoteNodeId);

// 调用 maybeTransitionToLeader 方法统计当前获取的投票数量，如果收到超过半数 Controller 节点的投票，则当前节点可以切换到 Leader 状态，成为集群的 Leader 节点。

maybeTransitionToLeader(state, currentTimeMs);

} else {

// 如果其他节点给当前节点投的是拒绝票，则同样记录拒绝投票的节点。

state.recordRejectedVote(remoteNodeId);

// 如果拒绝投票的节点超过半数 Controller 节点，则表示当前节点无法成为 Leader 节点，则调用 CandidateState#startBackingOff() 方法通过回退避免多个候选者的选举僵局，并且当前节点进入 BackingOff 阶段。

if (state.isVoteRejected() && !state.isBackingOff()) {

logger.info("Insufficient remaining votes to become leader (rejected by {}). " + "We will backoff before retrying election again", state.rejectingVoters());

state.startBackingOff(

currentTimeMs,

binaryExponentialElectionBackoffMs(state.retries())

);

}

}

} else {

logger.debug("Ignoring vote response {} since we are no longer a candidate in epoch {}", partitionResponse, quorum.epoch());

}

return true;

} else {

return handleUnexpectedError(error, responseMetadata);

}

}

该方法主要用来**响应 VoteRequest 投票结果的**，步骤如下：

1.  如果 [quorum.isCandidate()](http://quorum.iscandidate\(\)/) 为 true，表示其他节点返回了正常的投票响应，则执行以下操作：
2.  获取 [candidate](http://candidate/) 状态。
3.  如果其他节点返回了 [voteGranted](http://votegranted/) 标识，则标识其他节点给当前节点投的是赞成票。首先调用 CandidateState#recordGrantedVote 方法记录返回该投票响应的节点。接着调用 maybeTransitionToLeader 方法统计当前获取的投票数量，如果收到超过半数 Controller 节点的投票，则当前节点可以切换到 Leader 状态，成为集群的 Leader 节点。
4.  如果其他节点给当前节点投的是拒绝票，则同样记录拒绝投票的节点。另外如果拒绝投票的节点超过半数 Controller 节点，则表示当前节点无法成为 Leader 节点，则调用 CandidateState#startBackingOff() 方法通过回退避免多个候选者的选举僵局，并且当前节点进入 BackingOff 阶段。

## **4.3 pollLeader()**

当某个 「**Candidate**」状态的节点切换到「**Leader 节点**」后，将调用 [KafkaRaftClient#pollLeader()](http://kafkaraftclient/#pollLeader\(\)) 方法执行 「**Leader 节点**」的相关逻辑，源码如下：

private long pollLeader(long currentTimeMs) {

// 获取 Leader 状态

LeaderState<T> state = quorum.leaderStateOrThrow();

maybeFireLeaderChange(state);

if (shutdown.get() != null || state.isResignRequested()) {

transitionToResigned(state.nonLeaderVotersByDescendingFetchOffset());

return 0L;

}

// 1、将 LeaderState 暂存器的内容写入到 Cluster Topic中，后面篇章单独剖析

long timeUntilFlush \= maybeAppendBatches(

state,

currentTimeMs

);

// 2、调用 LeaderState#nonAcknowledgingVoters 方法获取未返回 BeginQuorumEpoch 响应的 Controller 节点，然后给这些节点发送 BeginQuorumEpochRequest 请求，通知它们当前节点被选举为 Leaer 节点

long timeUntilSend \= maybeSendRequests(

currentTimeMs,

state.nonAcknowledgingVoters(),

this::buildBeginQuorumEpochRequest

);

return Math.min(timeUntilFlush, timeUntilSend);

}

## **4.4 maybeAppendBatches()**

接着来剖析下将 LeaderState 暂存器的内容写入到 Cluster Topic 中，源码比较简单，如下：

private long maybeAppendBatches(

LeaderState<T> state,

long currentTimeMs

) {

// 计算距离下次批量发送的时间

long timeUntilDrain \= state.accumulator().timeUntilDrain(currentTimeMs);

// 如果距离下次批量发送的时间小于等于0，说明可以进行批量发送

if (timeUntilDrain <= 0) {

// 调用 state.accumulator().drain() 从暂存器中获取已完成的批次，并调用 BatchAccumulator#drain() 方法获取批量消息列表

List<BatchAccumulator.CompletedBatch<T>> batches = state.accumulator().drain();

Iterator<BatchAccumulator.CompletedBatch<T>> iterator = batches.iterator();

try {

// 迭代器遍历批量消息列表，依次调用 KafkaRaftClient#appendBatch() 方法开始将其追加到本地 Log 文件

while (iterator.hasNext()) {

BatchAccumulator.CompletedBatch<T> batch = iterator.next();

appendBatch(state, batch, currentTimeMs);

}

// 等以上处理完成后，调用 KafkaRaftClient#flushLeaderLog() 方法将元数据消息刷盘，并更新 Leader 节点上保存元数据的分区 Leader 副本状态，其中包括尝试更新 HW 的动作

flushLeaderLog(state, currentTimeMs);

} finally {

// 释放和丢弃未能追加的批次

while (iterator.hasNext()) {

iterator.next().release();

}

}

}

return timeUntilDrain;

}

private void flushLeaderLog(LeaderState<T> state, long currentTimeMs) {

// We update the end offset before flushing so that parked fetches can return sooner.

updateLeaderEndOffsetAndTimestamp(state, currentTimeMs);

// flush 日志到磁盘

log.flush();

}

步骤如下：

1.  计算距离下次批量发送的时间。
2.  如果距离下次批量发送的时间小于等于0，说明可以进行批量发送。
3.  调用 [state.accumulator().drain()](http://state.accumulator\(\).drain\(\)/) 从暂存器中获取已完成的批次，并调用 BatchAccumulator#drain() 方法获取批量消息列表。
4.  迭代器遍历批量消息列表，依次调用 [KafkaRaftClient#appendBatch()](http://kafkaraftclient/#appendBatch\(\)) 方法开始将其追加到本地 Log 文件。
5.  获取当前Leader的epoch。
6.  调用 appendAsLeader 方法将批次数据追加到日志中，并获取最新的 Offset 和 Epoch 信息。
7.  在 appendPurgatory 中注册 Offset+1 的 Future。
8.  注册 whenComplete 回调，用于处理 commit 的结果。
9.  更新统计信息、触发 HandleCommit 回调。
10.  最后释放已完成的批次。
11.  等以上处理完成后，调用 [KafkaRaftClient#flushLeaderLog()](http://kafkaraftclient/#flushLeaderLog\(\)%20) 方法将元数据消息刷盘，并更新 Leader 节点上保存元数据的分区 Leader 副本状态，其中包括尝试更新 HW 的动作。
12.  如果失败则释放和丢弃未能追加的批次。

private void appendBatch(

LeaderState<T> state,

BatchAccumulator.CompletedBatch<T> batch,

long appendTimeMs

) {

try {

// 获取当前Leader的epoch

int epoch \= state.epoch();

// 调用 appendAsLeader 方法将批次数据追加到日志中，并获取最新的 Offset 和 Epoch 信息

LogAppendInfo info \= appendAsLeader(batch.data);

OffsetAndEpoch offsetAndEpoch \= new OffsetAndEpoch(info.lastOffset, epoch);

// 在 appendPurgatory 中注册 Offset+1 的 Future

CompletableFuture<Long> future = appendPurgatory.await(

offsetAndEpoch.offset + 1, Integer.MAX\_VALUE);

// 注册 whenComplete 回调，用于处理commit的结果

future.whenComplete((commitTimeMs, exception) -> {

if (exception != null) {

logger.debug("Failed to commit {} records at {}", batch.numRecords, offsetAndEpoch, exception);

} else {

// 更新统计信息

long elapsedTime \= Math.max(0, commitTimeMs - appendTimeMs);

double elapsedTimePerRecord \= (double) elapsedTime / batch.numRecords;

kafkaRaftMetrics.updateCommitLatency(elapsedTimePerRecord, appendTimeMs);

logger.debug("Completed commit of {} records at {}", batch.numRecords, offsetAndEpoch);

// 触发 HandleCommit 回调

batch.records.ifPresent(records -> {

maybeFireHandleCommit(batch.baseOffset, epoch, batch.appendTimestamp(), batch.sizeInBytes(), records);

});

}

});

} finally {

// 释放已完成的批次

batch.release();

}

}

这里 [KafkaRaftClient#appendBatch()](http://kafkaraftclient/#appendBatch\(\)) 方法的核心处理是调用 [KafkaRaftClient#appendAsLeader()](http://kafkaraftclient.java/#appendAsLeader\(\)) 方法进行写入，此处最终将通过 [ReplicatedLog.appendAsLeader()](http://replicatedlog.appendasleader\(\)/) 接口调用到其实现 [KafkaMetadataLog#appendAsLeader()](http://kafkametadatalog.scala/#appendAsLeader\(\)) 方法。

## **4.5 handleBeginQuorumEpochRequest()**

当选举成功了「**Leader 节点**」，会广播选举为 Leader 事件，并发送 [BeginQuorumEpoch](http://beginquorumepoch/) 请求，等其他「**Controller 节点**」收到 [BeginQuorumEpoch](http://beginquorumepoch/) 请求后，会调用 [KafkaRaftClient#handleBeginQuorumEpochRequest()](http://kafkaraftclient/#handleBeginQuorumEpochRequest\(\)) 方法切换到「**Follower**」状态，成为「**Follower**」节点，这样就确立了主从关系。

  
![](https://article-images.zsxq.com/FrzH5lHyJQKljhXsUXV0OLnt6a1O)

private BeginQuorumEpochResponseData handleBeginQuorumEpochRequest(

RaftRequest.Inbound requestMetadata,

long currentTimeMs

) {

// 获取 BeginQuorumEpochRequest 数据

BeginQuorumEpochRequestData request \= (BeginQuorumEpochRequestData) requestMetadata.data;

// 检查集群ID是否一致

if (!hasValidClusterId(request.clusterId())) {

return new BeginQuorumEpochResponseData().setErrorCode(Errors.INCONSISTENT\_CLUSTER\_ID.code());

}

// 检查请求中的 TopicPartition 是否与日志中的相匹配

if (!hasValidTopicPartition(request, log.topicPartition())) {

// 在不支持多个 Raft 集群的情况下，将 TopicPartition 不匹配的请求视为无效请求

return new BeginQuorumEpochResponseData().setErrorCode(Errors.INVALID\_REQUEST.code());

}

// 获取请求中 PartitionData 的信息

BeginQuorumEpochRequestData.PartitionData partitionRequest \=

request.topics().get(0).partitions().get(0);

// 获取 Leader 节点id 和 Epoch 信息

int requestLeaderId \= partitionRequest.leaderId();

int requestEpoch \= partitionRequest.leaderEpoch();

// 验证 Voter-only 请求的合法性

Optional<Errors> errorOpt = validateVoterOnlyRequest(requestLeaderId, requestEpoch);

if (errorOpt.isPresent()) {

return buildBeginQuorumEpochResponse(errorOpt.get());

}

// 根据请求的 LeaderId 和 Epoch 进行状态转换

maybeTransition(OptionalInt.of(requestLeaderId), requestEpoch, currentTimeMs);

// 最后构建 BeginQuorumEpoch 响应

return buildBeginQuorumEpochResponse(Errors.NONE);

}

// 尝试转化状态

private void maybeTransition(

OptionalInt leaderId,

int epoch,

long currentTimeMs

) {

// 如果 leaderId 和 epoch 不一致，则抛出 IllegalStateException 异常

if (!hasConsistentLeader(epoch, leaderId)) {

throw new IllegalStateException("Received request or response with leader " + leaderId + " and epoch " + epoch + " which is inconsistent with current leader " +

quorum.leaderId() + " and epoch " + quorum.epoch());

// 如果 epoch 大于集群当前的 epoch，则根据 leaderId 执行转换操作

} else if (epoch > quorum.epoch()) {

if (leaderId.isPresent()) {

// 如果 leaderId 不为空，转换为 follower 状态

transitionToFollower(epoch, leaderId.getAsInt(), currentTimeMs);

} else {

// 如果 leaderId 为空，转换为 Unattached 状态

transitionToUnattached(epoch);

}

} else if (leaderId.isPresent() && !quorum.hasLeader()) {

// 如果传入的 leaderId 和当前集群没有 leader，则转换为 follower 状态

transitionToFollower(epoch, leaderId.getAsInt(), currentTimeMs);

}

}

步骤如下：

1.  获取 BeginQuorumEpochRequest 数据。
2.  检查集群ID是否一致以及检查请求中的 TopicPartition 是否与日志中的相匹配。
3.  获取请求中 PartitionData 的信息。
4.  获取 Leader 节点id 和 Epoch 信息。
5.  验证 Voter-only 请求的合法性。
6.  根据请求的 LeaderId 和 Epoch 进行状态转换。
7.  最后构建 BeginQuorumEpoch 响应。

## **4.6 handleBeginQuorumEpochResponse()**

我们将视角切回到发送 [BeginQuorumEpoch](http://beginquorumepoch/) 请求的节点，当节点处理完 [BeginQuorumEpoch](http://beginquorumepoch/) 请求，触发当前节点的 [KafkaRaftClient#handleBeginQuorumEpochResponse](http://kafkaraftclient.java/#handleVoteResponse)() 方法来处理其他节点返回的响应，源码如下：

  
![](https://article-images.zsxq.com/FpBN1_ky4hnPWhdhkl_U0q7Ikic0)

private boolean handleBeginQuorumEpochResponse(

RaftResponse.Inbound responseMetadata,

long currentTimeMs

) {

// 获取远端节点id

int remoteNodeId \= responseMetadata.sourceId();

// 获取 BeginQuorumEpoch 响应数据

BeginQuorumEpochResponseData response \= (BeginQuorumEpochResponseData) responseMetadata.data;

Errors topLevelError \= Errors.forCode(response.errorCode());

if (topLevelError != Errors.NONE) {

return handleTopLevelError(topLevelError, responseMetadata);

}

// 检查响应中的 TopicPartition 是否与日志中的相匹配

if (!hasValidTopicPartition(response, log.topicPartition())) {

return false;

}

// 获取响应中 PartitionData 的信息

BeginQuorumEpochResponseData.PartitionData partitionResponse \=

response.topics().get(0).partitions().get(0);

Errors partitionError \= Errors.forCode(partitionResponse.errorCode());

OptionalInt responseLeaderId \= optionalLeaderId(partitionResponse.leaderId());

int responseEpoch \= partitionResponse.leaderEpoch();

// 处理常见的响应情况

Optional<Boolean> handled = maybeHandleCommonResponse(

partitionError, responseLeaderId, responseEpoch, currentTimeMs);

if (handled.isPresent()) {

return handled.get();

} else if (partitionError == Errors.NONE) {

// 如果当前节点是 Leader，将远程节点添加到 Leader 的已确认的节点列表中

if (quorum.isLeader()) {

LeaderState<T> state = quorum.leaderStateOrThrow();

state.addAcknowledgementFrom(remoteNodeId);

} else {

logger.debug("Ignoring BeginQuorumEpoch response {} since " +

"this node is not the leader anymore", response);

}

return true;

} else {

// 处理意外错误

return handleUnexpectedError(partitionError, responseMetadata);

}

}

## **4.7 handleEndQuorumEpochRequest()**

当节点发生异常，并从 Leader 位置退位后，会触发该方法进行处理 [EndQuorumEpoch](http://endquorumepoch/) 请求。

![](https://article-images.zsxq.com/FhMCduBRkH6sH1mN4BR_Ibta4cig)

private EndQuorumEpochResponseData handleEndQuorumEpochRequest(

RaftRequest.Inbound requestMetadata,

long currentTimeMs

) {

// 获取 EndQuorumEpochRequest 数据

EndQuorumEpochRequestData request \= (EndQuorumEpochRequestData) requestMetadata.data;

// 检查集群 ID 是否一致

if (!hasValidClusterId(request.clusterId())) {

return new EndQuorumEpochResponseData().setErrorCode(Errors.INCONSISTENT\_CLUSTER\_ID.code());

}

// 检查请求中的 TopicPartition 是否与日志中的相匹配

if (!hasValidTopicPartition(request, log.topicPartition())) {

// 在不支持多个Raft集群的情况下，将TopicPartition不匹配的请求视为无效请求

return new EndQuorumEpochResponseData().setErrorCode(Errors.INVALID\_REQUEST.code());

}

// 获取请求中 PartitionData 的信息

EndQuorumEpochRequestData.PartitionData partitionRequest \=

request.topics().get(0).partitions().get(0);

int requestEpoch \= partitionRequest.leaderEpoch();

int requestLeaderId \= partitionRequest.leaderId();

// 验证 Voter-only 请求的合法性

Optional<Errors> errorOpt = validateVoterOnlyRequest(requestLeaderId, requestEpoch);

if (errorOpt.isPresent()) {

return buildEndQuorumEpochResponse(errorOpt.get());

}

// 根据请求的 LeaderId 和 Epoch 进行状态转换

maybeTransition(OptionalInt.of(requestLeaderId), requestEpoch, currentTimeMs);

// 如果当前节点是 Follower 且请求中的 LeaderId 与当前节点的 LeaderId 相同

if (quorum.isFollower()) {

FollowerState state \= quorum.followerStateOrThrow();

if (state.leaderId() == requestLeaderId) {

List<Integer> preferredSuccessors = partitionRequest.preferredSuccessors();

// 则根据请求中的 preferredSuccessors 计算新的选举超时

long electionBackoffMs \= endEpochElectionBackoff(preferredSuccessors);

logger.debug("Overriding follower fetch timeout to {} after receiving " +

"EndQuorumEpoch request from leader {} in epoch {}", electionBackoffMs,

requestLeaderId, requestEpoch);

// 设置选举超时时间

state.overrideFetchTimeout(currentTimeMs, electionBackoffMs);

}

}

// 构建 EndQuorumEpochResponse

return buildEndQuorumEpochResponse(Errors.NONE);

}

步骤如下：

1.  获取 EndQuorumEpochRequest 数据。
2.  检查集群ID是否一致以及检查请求中的 TopicPartition 是否与日志中的相匹配。
3.  获取请求中 PartitionData 的信息。
4.  获取 Leader 节点id 和 Epoch 信息。
5.  验证 Voter-only 请求的合法性。
6.  根据请求的 LeaderId 和 Epoch 进行状态转换。
7.  如果当前节点是 Follower 且请求中的 LeaderId 与当前节点的 LeaderId 相同，则根据请求中的 preferredSuccessors 计算新的选举超时并设置。
8.  最后构建 EndQuorumEpochResponse 响应。

## **4.8 handleEndQuorumEpochResponse()**

我们将视角切回到发送 [EndQuorumEpoch](http://beginquorumepoch/) 请求的节点，当节点处理完 [EndQuorumEpoch](http://beginquorumepoch/) 请求，触发当前节点的 [KafkaRaftClient#handleEndQuorumEpochResponse](http://kafkaraftclient.java/#handleVoteResponse)() 方法来处理其他节点返回的响应，源码如下：

  
![](https://article-images.zsxq.com/FhXlOhIMzZLrYLZTCYXd94BhZZoe)

private boolean handleEndQuorumEpochResponse(

RaftResponse.Inbound responseMetadata,

long currentTimeMs

) {

// 获取 EndQuorumEpochResponse 数据。

EndQuorumEpochResponseData response \= (EndQuorumEpochResponseData) responseMetadata.data;

Errors topLevelError \= Errors.forCode(response.errorCode());

if (topLevelError != Errors.NONE) {

return handleTopLevelError(topLevelError, responseMetadata);

}

// 检查响应中的 TopicPartition 是否与日志中的相匹配

if (!hasValidTopicPartition(response, log.topicPartition())) {

return false;

}

// 获取响应中 PartitionData 的信息

EndQuorumEpochResponseData.PartitionData partitionResponse \=

response.topics().get(0).partitions().get(0);

Errors partitionError \= Errors.forCode(partitionResponse.errorCode());

OptionalInt responseLeaderId \= optionalLeaderId(partitionResponse.leaderId());

int responseEpoch \= partitionResponse.leaderEpoch();

// 处理常见的响应情况

Optional<Boolean> handled = maybeHandleCommonResponse(

partitionError, responseLeaderId, responseEpoch, currentTimeMs);

if (handled.isPresent()) {

return handled.get();

} else if (partitionError == Errors.NONE) {

// 如果没有错误，先获取 resignedState，然后将远程节点添加到 resignedState 的已确认的节点列表中

ResignedState resignedState \= quorum.resignedStateOrThrow();

resignedState.acknowledgeResignation(responseMetadata.sourceId());

return true;

} else {

// 处理意外情况

return handleUnexpectedError(partitionError, responseMetadata);

}

}

## **05 选举流程梳理**

这里我通过一张时序图来梳理整个 Raft 模块启动以及 Raft Leader 选举流程。

![](https://article-images.zsxq.com/Fj--rRJW81qgvqYahE9yJs22O46H)

## **5.1 正常选举流程**

这里我们来梳理下正常的集群选主流程，如下图所示：

![](https://article-images.zsxq.com/lrgvwHdTTyFInrqHvYposrAdWn2R)

## **5.2 选举回退机制流程**

这里你可以想象一下如果集群中有「**多个投票节点同时启动**」，并且都是「**初次启动**」，那么很可能这些节点都会切换到「**Candidate**」状态。此时它们就算收到了其他「**候选者**」的 Vote 投票请求，也不会为其他候选者投票，选举就陷入了失败的僵局。对于这种情况， Kafka 引入了「**回退机制**」进行处理，大致流程如下图所示：

  
![](https://article-images.zsxq.com/loFwXNbwv6r4eIfxB3S8ZUBqnqsa)

回退机制的核心在于使用 [controller.quorum.election.backoff.max.ms](http://controller.quorum.election.backoff.max.ms/) 配置设置一个随机的回退超时时间，[KafkaRaftClient#pollCandidate()](http://kafkaraftclient/#pollCandidate\(\)) 方法会检查「**候选者**」节点是否处于「**回退状态**」，回退状态的候选者将不再发送 Vote 请求。

一旦回退的超时时间到达，最早退出「**回退状态**」的「**候选者**」节点将重新发起 [VoteRequest](http://voterequest/) 投票请求，此时投票请求中携带的集群 epoch 增加了一个版本，收到请求的其他候选者会因为版本落后而回退到 [UnattachedState](http://unattachedstate/) 状态，此时可以顺利地投「**赞成票**」，选举僵局解除。

## **06 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头引出了「**Kraft**」中「**Leader 节点选举**」。

2、接着带大家「**梳理了 Kraft 选举的前置知识点**」，了解了请求分类以及身份分类。

3、接着带大家剖析了「**初始化 KRaft 状态**」的两种场景源码流程：「**集群启动时**」、「**Leader 下线重新选举时**」。

4、接着带大家剖析了「**发送投票请求**」的源码流程。

5、最后通过三张流程图带大家梳理了整个选举流程，希望通过这三张图让你更好的理解。

下篇我们来深度剖析「**Kraft 数据处理机制**」，大家期待，我们下期见。