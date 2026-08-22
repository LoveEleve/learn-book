大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端** **KRaft 数据处理机制**」，以「**创建 Topic 场景**」为例来深度剖析 「**Controller 请求处理流程**」的四个维度，让你更好的理解其处理全流程，今天我们接着来深度聊聊「**Kafka 服务端 KRaft 元数据主从同步机制**」，看看 Kafka Kraft 模式下「**主从之间是如何同步元数据的**」。

![](https://article-images.zsxq.com/FhUCn2xLeh_uLS6kB3daaYqsQChB)

## **01 总体概述**

在 [【服务端 Broker 源码分析系列第三十八篇】图解 Kafka 源码之 KRaft 数据处理机制](https://articles.zsxq.com/id_xx081g35rpve.html) 上一篇中，我带大家深度剖析了「**KRaft 模式**」下的 Kafka 集群「**Leader 节点**」更新保存元数据 Record 的流程，而元数据保存下来后肯定要传播到整个 Kafka 集群，使其正常生效，这个传播的过程就是「**元数据的主从同步**」。

在剖析这个过程之前，我们首先要了解 Kafka 保证分区数据可靠性的机制，大致如下图所示：

  
![](https://article-images.zsxq.com/FkwNSu6KDTjTZ9YTzylSnTxG4t4j)

1.  HW(High Watermark) 高水位：用来定义分区中消息的可见性，只有偏移量 Offset 小于 HW 值的消息是已提交的，才可以被消费。
2.  LEO(Log End Offset) 日志末端偏移量：用来指向分区副本底层日志文件将要写入的下一条消息的 Offset 偏移量，也就是日志文件尾部的下一个位置。

Kafka 通过「**多副本机制**」来实现故障 Failover，其每个分区都由若干副本组成，包括「**Leader 副本节点**」和 「**Follower 副本节点**」。主从数据同步并不是「**Follower 副本节点**」 把数据复制过来保存就结束了，还需要借助额外的 HW 标识来表明数据的提交状态（Committed/Uncommitted），最后决定消息是否对外可见。

所有副本底层的日志文件结构都是一样的，也就是说每个副本都有自己的 HW，但只有 「**Leader 副本节点**」的 HW 是用来表示整个分区的消息可见性。

主从副本的数据同步就是围绕 HW 展开，并通过 Fetch 请求进行。之前我们剖析过副本数据同步，如果忘记了可以点击下面连接进行学习：

[【服务端 Broker 源码分析系列第二十六篇】图解 Kafka 源码之副本同步实现原理（上）](https://articles.zsxq.com/id_j8h0gmrk5i6d.html)

[【服务端 Broker 源码分析系列第二十七篇】图解 Kafka 源码之副本同步实现原理（下）](https://articles.zsxq.com/id_64qw2onjkgpx.html)

本文涉及的源码：

「**KafkaRaftManager**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/raft/RaftManager.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/raft/RaftManager.scala)

「**KafkaRaftClient**」类源码在 Kafka 源码包的 raft 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/raft/src/main/java/org/apache/kafka/raft/KafkaRaftClient.java](https://github.com/apache/kafka/blob/3.0.0/raft/src/main/java/org/apache/kafka/raft/KafkaRaftClient.java)

「**LeaderState**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/raft/src/main/java/org/apache/kafka/raft/LeaderState.java](https://github.com/apache/kafka/blob/3.0.0/raft/src/main/java/org/apache/kafka/raft/LeaderState.java)

「**FollowerState**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/raft/src/main/java/org/apache/kafka/raft/FollowerState.java](https://github.com/apache/kafka/blob/3.0.0/raft/src/main/java/org/apache/kafka/raft/FollowerState.java)

「**InterBrokerSendThread**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/common/InterBrokerSendThread.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/raft/RaftManager.scala)

「**ControllerApis**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/ControllerApis.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/ControllerApis.scala)

「**KafkaMetadataLog**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/raft/KafkaMetadataLog.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/raft/KafkaMetadataLog.scala)

## **02 元数据主从同步源码流程**

接下来，我们来深度剖析下「**KRaft 集群**」的「**Follower 副本节点**」从 「**Leader 副本节点**」同步元数据的处理的全过程。

## **2.1 Follower 节点发起 Fetch 请求**

## **2.1.1 poll()**

当 「**KRaft 集群**」选主的流程完成后，集群中的节点都取得了各自角色，并且所有节点都将定时执行 [KafkaRaftClient#poll()](http://kafkaraftclient/#poll\(\)) 方法。

![](https://article-images.zsxq.com/Fr-e8fXVz5LDxH7RODjKyRj1G15s)

## **2.1.2 pollCurrentState()**

接着调用 [KafkaRaftClient#pollCurrentState()](http://kafkaraftclient/#pollCurrentState\(\)) 方法根据节点当前的角色进行相应处理，该方法只是个分发入口，对于「**Follower 状态**」的节点将执行 [KafkaRaftClien#pollFollower()](http://kafkaraftclien/#pollFollower\(\)%20) 方法进入下一层分发，可以看到有投票权的 「**Follower 节点**」将调用 [KafkaRaftClien#pollFollowerAsVoter()](http://kafkaraftclien/#pollFollowerAsVoter\(\)%20) 方法进行发起投票。

![](https://article-images.zsxq.com/FgFdees0IJPsYalHzXioTPWD3REA)

在 [【服务端 Broker 源码分析系列第三十七篇】图解 Kafka 源码之 KRaft Leader 选举机制流程](https://articles.zsxq.com/id_ox3kxhfx7li2.html) 这篇中剖析过，这里再简单的说明下。

## **2.1.3 pollFollowerAsVoter()**

可以看到 [pollFollowerAsVoter()](http://kafkaraftclien/#pollFollowerAsVoter\(\)) 方法中会调用 [maybeSendFetchOrFetchSnapshot()](http://maybesendfetchorfetchsnapshot\(\)/) 方法向「**Leader 节点**」发起同步元数据的 Fetch 请求。

![](https://article-images.zsxq.com/FqJ6Iwk--oMJCYXK1s-uM-KEaH4c)

## **2.1.4 maybeSendFetchOrFetchSnapshot()**

private long maybeSendFetchOrFetchSnapshot(FollowerState state, long currentTimeMs) {

final Supplier<ApiMessage> requestSupplier;

// 1、如果 FollowerState#fetchingSnapshot 属性存在，则说明需要获取数据快照，此时发送 FetchSnapshot 请求

if (state.fetchingSnapshot().isPresent()) {

// 快照对象

RawSnapshotWriter snapshot \= state.fetchingSnapshot().get();

// 快照大小

long snapshotSize \= snapshot.sizeInBytes();

requestSupplier = () -> buildFetchSnapshotRequest(snapshot.snapshotId(), snapshotSize);

} else {

// 否则发送 Fetch 请求，调用 KafkaRaftClien#buildFetchRequest() 方法构建 Fetch 请求。

requestSupplier = this::buildFetchRequest;

}

// 调用 KafkaRaftClien#maybeSendRequest() 方法将异步发送请求

return maybeSendRequest(currentTimeMs, state.leaderId(), requestSupplier);

}

核心步骤如下：

1.  如果 [FollowerState#fetchingSnapshot](http://followerstate/#fetchingSnapshot) 属性存在，则说明需要获取数据快照，此时发送 [FetchSnapshot](http://fetchsnapshot%20/) 请求
2.  否则发送 Fetch 请求，调用 [KafkaRaftClien#buildFetchRequest()](http://kafkaraftclien/#buildFetchRequest\(\)) 方法构建 Fetch 请求。
3.  接着调用 [KafkaRaftClien#maybeSendRequest()](http://kafkaraftclien/#maybeSendRequest\(\)) 方法将异步发送请求。

## **2.1.5 buildFetchRequest()**

我们先来剖析构建 Fetch 请求的方法，源码如下：

private FetchRequestData buildFetchRequest() {

FetchRequestData request \= RaftUtil.singletonFetchRequest(log.topicPartition(), fetchPartition -> {

fetchPartition

.setCurrentLeaderEpoch(quorum.epoch())

.setLastFetchedEpoch(log.lastFetchedEpoch())

// 需要注意的是这里通过 log.endOffset().offset 获取本地日志的末端偏移量 LEO，将其填入 Fetch 请求

.setFetchOffset(log.endOffset().offset);

});

return request

.setMaxBytes(MAX\_FETCH\_SIZE\_BYTES)

.setMaxWaitMs(fetchMaxWaitMs)

.setClusterId(clusterId)

}

该方法比较简单，不过需要注意的是这里通过 [log.endOffset().offset](http://log.endoffset\(\).offset/) 取得本地日志的末端偏移量 LEO，将其填入 Fetch 请求。

## **2.1.6 maybeSendRequest()**

接着来剖析异步发送请求的方法，源码如下：

/\*\*

\* Attempt to send a request. Return the time to wait before the request can be retried.

\*/

private long maybeSendRequest(

long currentTimeMs,

int destinationId,

Supplier<ApiMessage> requestSupplier

) {

// 1、首先调用 RequestManager.java#getOrCreate() 方法根据目标节点的ID 取得 ConnetcionState，接下来检查与目标节点的连接是否正常，连接就绪才进行下一步

ConnectionState connection \= requestManager.getOrCreate(destinationId);

if (connection.isBackingOff(currentTimeMs)) {

long remainingBackoffMs \= connection.remainingBackoffMs(currentTimeMs);

logger.debug("Connection for {} is backing off for {} ms", destinationId, remainingBackoffMs);

return remainingBackoffMs;

}

// 2、如果连接正常，则调用请求构建器构建出对应的请求对象，然后将其封装到 RaftRequest.Outbound 对象内部，并设置请求完成时的回调方法。回调方法的处理是将响应数据封装到 RaftResponse.Inbound 对象，并将其写入到消息队列中异步处理

if (connection.isReady(currentTimeMs)) {

int correlationId \= channel.newCorrelationId();

ApiMessage request \= requestSupplier.get();

// 构建请求对象并将其封装到 RaftRequest.Outbound 对象内部

RaftRequest.Outbound requestMessage \= new RaftRequest.Outbound(

correlationId,

request,

destinationId,

currentTimeMs

);

// 设置请求完成时的回调方法

requestMessage.completion.whenComplete((response, exception) -> {

if (exception != null) {

ApiKeys api \= ApiKeys.forId(request.apiKey());

Errors error \= Errors.forException(exception);

ApiMessage errorResponse \= RaftUtil.errorResponse(api, error);

// 回调方法的处理是将响应数据封装到 RaftResponse.Inbound 对象

response = new RaftResponse.Inbound(

correlationId,

errorResponse,

destinationId

);

}

// 将其写入到消息队列中异步处理

messageQueue.add(response);

});

channel.send(requestMessage);

logger.trace("Sent outbound request: {}", requestMessage);

connection.onRequestSent(correlationId, currentTimeMs);

return Long.MAX\_VALUE;

}

// 3、最后调用 KafkaNetworkChannel.scala#send() 方法将请求投入到底层异步发送

return connection.remainingRequestTimeMs(currentTimeMs);

}

核心步骤如下：

1.  首先调用 [RequestManager.java#getOrCreate](http://requestmanager.java/#getOrCreate)() 方法根据目标节点的ID 取得 [ConnetcionState](http://connetcionstate/)，接下来检查与目标节点的连接是否正常，连接就绪才进行下一步。
2.  如果连接正常，则调用请求构建器构建出对应的请求对象，然后将其封装到 RaftRequest.Outbound 对象内部，并设置请求完成时的回调方法。回调方法的处理是将响应数据封装到 RaftResponse.Inbound 对象，并将其写入到消息队列中异步处理。
3.  最后调用 [KafkaNetworkChannel.scala#send](http://kafkanetworkchannel.scala/#send)() 方法将请求投入到底层异步发送。

至此，「**Follower 节点发起的 Fetch 请求**」的源码流程就剖析完了。

##   
**2.2 Leader 节点处理 Fetch 请求**

当「**Follower 节点**」发送的同步元数据的 Fetch 请求抵达「**Leader** **节点**」，此时会触发

[ControllerApis.scala#handle](http://controllerapis.scala/#handle)() 方法进行分发。

##   
**2.2.1 ControllerApis#handle()**

  
![](https://article-images.zsxq.com/FiTx7Icvv7fKz0yFZE9xFsLd1Jt-)

最终交由 [ControllerApis.scala#handleRaftRequest](http://controllerapis.scala/#handleRaftRequest)() 方法处理。

## **2.2.2 ControllerApis#handleRaftRequest()**

![](https://article-images.zsxq.com/FjsoBwXtIuBNvtGt9UD9CEmURDSR)

从上得出，真正的核心处理逻辑是调用 [KafkaRaftManager.scala#handleRequest](http://kafkaraftmanager.scala/#handleRequest)() 方法。

##   
**2.2.3 KafkaRaftManager#handleRequest()**

override def handleRequest(

header: RequestHeader,

request: ApiMessage,

createdTimeMs: Long

): CompletableFuture\[ApiMessage\] = {

// 1、将接收的请求封装为 RaftRequest.Inbound 对象

val inboundRequest \= new RaftRequest.Inbound(

header.correlationId,

request,

createdTimeMs

)

// 2、调用 KafkaRaftClient.java#handle() 方法将其写入到异步消息队列中

client.handle(inboundRequest)

inboundRequest.completion.thenApply { response =>

response.data

}

}

核心步骤如下：

1.  将接收的请求封装为 RaftRequest.Inbound 对象。
2.  调用 [KafkaRaftClient.java#handle](http://kafkaraftclient.java/#handle)() 方法将其写入到异步消息队列中。

## **2.2.4 KafkaRaftClient#handle()**

![](https://article-images.zsxq.com/FiQt0SvawsTJOACkBA3mPV0v-rI5)

很简单，就是将请求写入到异步消息队列中，接下来会定时执行 [KafkaRaftClient#poll](http://kafkaraftclient.java/#poll)() 来处理请求，而在与队列消息处理相关的方法是 [KafkaRaftClient#handleInboundMessage](http://kafkaraftclient.java/#handleInboundMessage)()。

## **2.2.5 handleInboundMessage()**

private void handleInboundMessage(RaftMessage message, long currentTimeMs) {

logger.trace("Received inbound message {}", message);

// 根据接收到的消息类型来处理消息

if (message instanceof RaftRequest.Inbound) {

RaftRequest.Inbound request \= (RaftRequest.Inbound) message;

// 处理来自其他节点的请求消息

handleRequest(request, currentTimeMs);

} else if (message instanceof RaftResponse.Inbound) {

RaftResponse.Inbound response \= (RaftResponse.Inbound) message;

ConnectionState connection \= requestManager.getOrCreate(response.sourceId());

if (connection.isResponseExpected(response.correlationId)) {

// 处理来自其他节点的响应消息

handleResponse(response, currentTimeMs);

} else {

logger.debug("Ignoring response {} since it is no longer needed", response);

}

} else {

throw new IllegalArgumentException("Unexpected message " + message);

}

}

从上得出，会调用 [KafkaRaftClient#](http://kafkaraftclient.java/#handleInboundMessage)[handleRequest()](http://handlerequest\(\)/) 进行请求分发，最终「**Leader** **节点**」将执行 [KafkaRaftClient#handleFetchRequest](http://kafkaraftclient.java/#handleFetchRequest)() 方法来完成 Fetch 请求处理。

![](https://article-images.zsxq.com/Fofz4ZrnaPvS0Jx51YsT6vivVkoq)

## **2.2.6 handleFetchRequest()**

private CompletableFuture<FetchResponseData> handleFetchRequest(

        RaftRequest.Inbound requestMetadata,

        long currentTimeMs

    ) {

FetchRequestData request \= (FetchRequestData) requestMetadata.data;

// 首先是对请求进行参数有效性检查

if (!hasValidClusterId(request.clusterId())) {

return completedFuture(new FetchResponseData().setErrorCode(Errors.INCONSISTENT\_CLUSTER\_ID.code()));

        }

if (!hasValidTopicPartition(request, log.topicPartition())) {

return completedFuture(new FetchResponseData().setErrorCode(Errors.INVALID\_REQUEST.code()));

        }

        FetchRequestData.FetchPartition fetchPartition \= request.topics().get(0).partitions().get(0);

if (request.maxWaitMs() < 0

            || fetchPartition.fetchOffset() < 0

            || fetchPartition.lastFetchedEpoch() < 0

            || fetchPartition.lastFetchedEpoch() > fetchPartition.currentLeaderEpoch()) {

return completedFuture(buildEmptyFetchResponse(

                Errors.INVALID\_REQUEST, Optional.empty()));

        }

// 调用 KafkaRaftClient#tryCompleteFetchRequest() 尝试完成 Fetch 请求处理，如果这个请求不能立即返回响应，则调用 FuturePurgatory#await() 方法等待

FetchResponseData response \= tryCompleteFetchRequest(request.replicaId(), fetchPartition, currentTimeMs);

        FetchResponseData.PartitionData partitionResponse \=

            response.responses().get(0).partitions().get(0);

if (partitionResponse.errorCode() != Errors.NONE.code()

            || FetchResponse.recordsSize(partitionResponse) > 0

            || request.maxWaitMs() == 0) {

return completedFuture(response);

        }

// 如果这个请求不能立即返回响应，则调用 FuturePurgatory#await() 方法等待

        CompletableFuture<Long> future = fetchPurgatory.await(

            fetchPartition.fetchOffset(),

            request.maxWaitMs());

// 在等待超时结束或者提前结束后，再次调用 KafkaRaftClient#tryCompleteFetchRequest() 尝试完成 Fetch 请求处理

return future.handle((completionTimeMs, exception) -> {

if (exception != null) {

Throwable cause \= exception instanceof ExecutionException ?

                    exception.getCause() : exception;

// If the fetch timed out in purgatory, it means no new data is available,

// and we will complete the fetch successfully. Otherwise, if there was

// any other error, we need to return it.

Errors error \= Errors.forException(cause);

if (error != Errors.REQUEST\_TIMED\_OUT) {

                    logger.debug("Failed to handle fetch from {} at {} due to {}",

                        request.replicaId(), fetchPartition.fetchOffset(), error);

return buildEmptyFetchResponse(error, Optional.empty());

                }

            }

// FIXME: \`completionTimeMs\`, which can be null

            logger.trace("Completing delayed fetch from {} starting at offset {} at {}",

                request.replicaId(), fetchPartition.fetchOffset(), completionTimeMs);

return tryCompleteFetchRequest(request.replicaId(), fetchPartition, time.milliseconds());

        });

}

该方法比较简单，核心步骤如下：

1.  首先是对请求进行参数有效性检查。
2.  调用 [KafkaRaftClient#tryCompleteFetchRequest()](http://kafkaraftclient/#tryCompleteFetchRequest\(\)) 尝试完成 Fetch 请求处理，如果这个请求不能立即返回响应，则调用 [FuturePurgatory#await()](http://futurepurgatory/#await\(\)) 方法等待。
3.  在等待超时结束或者提前结束后，再次调用 [KafkaRaftClient#tryCompleteFetchRequest()](http://kafkaraftclient/#tryCompleteFetchRequest\(\)) 尝试完成 Fetch 请求处理。

## **2.2.7 tryCompleteFetchRequest()**

private FetchResponseData tryCompleteFetchRequest(

int replicaId,

FetchRequestData.FetchPartition request,

long currentTimeMs

) {

try {

Optional<Errors> errorOpt = validateLeaderOnlyRequest(request.currentLeaderEpoch());

if (errorOpt.isPresent()) {

return buildEmptyFetchResponse(errorOpt.get(), Optional.empty());

}

// 1、取出 Fetch 请求携带的参数，调用 ReplicatedLog#validateOffsetAndEpoch() 校验其有效性，如果 Follower 节点与 Leader 节点的集群版本出现分歧，将在该步骤检测出来

long fetchOffset \= request.fetchOffset();

int lastFetchedEpoch \= request.lastFetchedEpoch();

LeaderState<T> state = quorum.leaderStateOrThrow();

ValidOffsetAndEpoch validOffsetAndEpoch \= log.validateOffsetAndEpoch(fetchOffset, lastFetchedEpoch);

final Records records;

// 2、当有效性校验通过，调用 KafkaMetadataLog.scala#read() 方法读取指定 Offset 偏移量之后的元数据消息

if (validOffsetAndEpoch.kind() == ValidOffsetAndEpoch.Kind.VALID) {

LogFetchInfo info \= log.read(fetchOffset, Isolation.UNCOMMITTED);

// 3、接着调用 LeaderState.java#updateReplicaState() 方法尝试更新 Follower 副本的状态，这个过程中如果完成当前节点的 HW 更新则调用 KafkaRaftClient.java#onUpdateLeaderHighWatermark() 方法通知本地监听器

if (state.updateReplicaState(replicaId, currentTimeMs, info.startOffsetMetadata)) {

onUpdateLeaderHighWatermark(state, currentTimeMs);

}

records = info.records;

} else {

records = MemoryRecords.EMPTY;

}

// 4、最后调用 KafkaRaftClient.java#buildFetchResponse() 方法构建 Fetch 响应

return buildFetchResponse(Errors.NONE, records, validOffsetAndEpoch, state.highWatermark());

} catch (Exception e) {

logger.error("Caught unexpected error in fetch completion of request {}", request, e);

return buildEmptyFetchResponse(Errors.UNKNOWN\_SERVER\_ERROR, Optional.empty());

}

}

该方法用来**尝试完成 Fetch 请求的处理**，核心步骤如下：

1.  取出 Fetch 请求携带的参数，调用 [ReplicatedLog#validateOffsetAndEpoch()](http://replicatedlog/#validateOffsetAndEpoch\(\)) 校验其有效性，如果 Follower 节点与 Leader 节点的集群版本出现分歧，将在该步骤检测出来。
2.  当有效性校验通过，调用 [KafkaMetadataLog.scala#read](http://kafkametadatalog.scala/#read)() 方法读取指定 Offset 偏移量之后的元数据消息。
3.  接着调用 [LeaderState.java#updateReplicaState](http://leaderstate.java/#updateReplicaState)() 方法尝试更新 Follower 副本的状态，这个过程中如果完成当前节点的 HW 更新则调用 [KafkaRaftClient.java#onUpdateLeaderHighWatermark](http://kafkaraftclient.java/#onUpdateLeaderHighWatermark)() 方法通知本地监听器。
4.  最后调用 [KafkaRaftClient.java#buildFetchResponse](http://kafkaraftclient.java/#buildFetchResponse)() 方法构建 Fetch 响应。

## **2.2.8 LeaderState#updateReplicaState()**

该方法用来**尝试更新本地目标副本的状态的具体算法实现**，会在文末进行图解，这里就不赘述了。

public boolean updateReplicaState(int replicaId,

long fetchTimestamp,

LogOffsetMetadata logOffsetMetadata) {

// 判断当前节点是否有 controller 角色，没有则不进行更新处理

if (replicaId < 0) {

return false;

}

// 获取副本的状态信息

ReplicaState state \= getReplicaState(replicaId);

state.updateFetchTimestamp(fetchTimestamp);

// 更新 LogEndOffset

return updateEndOffset(state, logOffsetMetadata);

}

// 获取副本的状态信息

private ReplicaState getReplicaState(int remoteNodeId) {

ReplicaState state \= voterStates.get(remoteNodeId);

if (state == null) {

// 如果副本的状态信息不存在，则创建一个新的状态信息并将其放入 observerStates 中

observerStates.putIfAbsent(remoteNodeId, new ReplicaState(remoteNodeId, false));

return observerStates.get(remoteNodeId);

}

return state;

}

// 更新副本的结束偏移量。

private boolean updateEndOffset(ReplicaState state,

LogOffsetMetadata endOffsetMetadata) {

state.endOffset.ifPresent(currentEndOffset -> {

// 如果当前结束偏移量大于要更新的偏移量，则抛出异常或打印警告日志。

if (currentEndOffset.offset > endOffsetMetadata.offset) {

if (state.nodeId == localId) {

throw new IllegalStateException("Detected non-monotonic update of local " +

"end offset: " + currentEndOffset.offset + " -> " + endOffsetMetadata.offset);

} else {

log.warn("Detected non-monotonic update of fetch offset from nodeId {}: {} -> {}",state.nodeId, currentEndOffset.offset, endOffsetMetadata.offset);

}

}

});

// 每当更新结束偏移量时，将标记 hasAcknowledgedLeader 设置为 true。

state.endOffset = Optional.of(endOffsetMetadata);

state.hasAcknowledgedLeader = true;

// 判断当前节点是否是有投票权的节点（配置controller.quorum.voters决定），不是则不进行更新高水位处理

return isVoter(state.nodeId) && updateHighWatermark();

}

// 更新高水位点。

private boolean updateHighWatermark() {

// 将 voterStates 保存的所有副本状态对象按照 LEO 降序排列

List<ReplicaState> followersByDescendingFetchOffset = followersByDescendingFetchOffset();

// 取排序列表中间下标位置的副本的 LEO 作为 newHW

int indexOfHw \= voterStates.size() / 2;

// 根据 newHw 获取高水位点的更新元数据

Optional<LogOffsetMetadata> highWatermarkUpdateOpt = followersByDescendingFetchOffset.get(indexOfHw).endOffset;

if (highWatermarkUpdateOpt.isPresent()) {

LogOffsetMetadata highWatermarkUpdateMetadata \= highWatermarkUpdateOpt.get();

long highWatermarkUpdateOffset \= highWatermarkUpdateMetadata.offset;

// 如果更新的高水位点偏移量大于 epochStartOffset

if (highWatermarkUpdateOffset > epochStartOffset) {

// 如果当前高水位存在

if (highWatermark.isPresent()) {

LogOffsetMetadata currentHighWatermarkMetadata \= highWatermark.get();

// 如果更新的高水位偏移量大于当前高水位点偏移量或者更新的高水位偏移量等于当前高水位偏移量但元数据不相等

if (highWatermarkUpdateOffset > currentHighWatermarkMetadata.offset

|| (highWatermarkUpdateOffset == currentHighWatermarkMetadata.offset &&

!highWatermarkUpdateMetadata.metadata.equals(currentHighWatermarkMetadata.metadata))) {

// 更新高水位

highWatermark = highWatermarkUpdateOpt;

log.trace(

"High watermark updated to {} based on indexOfHw {} and voters {}",

highWatermark,

indexOfHw,

followersByDescendingFetchOffset

);

return true;

// 如果更新的高水位偏移量小于当前高水位偏移量

} else if (highWatermarkUpdateOffset < currentHighWatermarkMetadata.offset) {

log.error("The latest computed high watermark {} is smaller than the current " +

"value {}, which suggests that one of the voters has lost committed data. " +

"Full voter replication state: {}", highWatermarkUpdateOffset,

currentHighWatermarkMetadata.offset, voterStates.values());

return false;

} else {

// 如果更新的高水位偏移量等于当前高水位偏移量且元数据相等

return false;

}

// 如果当前高水位不存在

} else {

highWatermark = highWatermarkUpdateOpt;

log.trace(

"High watermark set to {} based on indexOfHw {} and voters {}",

highWatermark,

indexOfHw,

followersByDescendingFetchOffset

);

return true;

}

}

}

return false;

}

上面这种算法实际上意味着**集群元数据变更只要同步到一半以上的有投票权的节点上就认为集群元数据已经一致了，可以通知监听器元数据变更已生效**，也就是我经常提到的非强一致性要求的体现。

## **2.3 Follower 节点响应 Fetch 处理**

当「**Follower 节点**」收到 Fetch 响应后，将触发 **2.1.6 步骤的第 2 步，**将 Fetch 响应封装起来写入到异步消息队列。

接着「**Follower 节点**」处理异步消息的触发方法还是前面我们剖析过的 [KafkaRaftClient#poll()](http://%20kafkaraftclient.java/#poll\(\))，与「**发起 Fetch 请求**」分发流程类似，Fetch 的响应最终将会流转到 [KafkaRaftClient#handleFetchResponse()](http://kafkaraftclient/#handleFetchResponse\(\)) 方法处理。

## **2.3.1 handleFetchResponse()**

private boolean handleFetchResponse(

RaftResponse.Inbound responseMetadata,

long currentTimeMs

) {

// 获取响应数据

FetchResponseData response \= (FetchResponseData) responseMetadata.data;

... // 异常校验

FetchResponseData.PartitionData partitionResponse \=

response.responses().get(0).partitions().get(0);

FetchResponseData.LeaderIdAndEpoch currentLeaderIdAndEpoch \= partitionResponse.currentLeader();

OptionalInt responseLeaderId \= optionalLeaderId(currentLeaderIdAndEpoch.leaderId());

int responseEpoch \= currentLeaderIdAndEpoch.leaderEpoch();

Errors error \= Errors.forCode(partitionResponse.errorCode());

// 定义回调函数

Optional<Boolean> handled = maybeHandleCommonResponse(

error, responseLeaderId, responseEpoch, currentTimeMs);

if (handled.isPresent()) {

return handled.get();

}

FollowerState state \= quorum.followerStateOrThrow();

if (error == Errors.NONE) {

// 首先检查 Fetch 响应中是否包含集群版本分歧的信息，如果有则可能是发生了 Leader 宕机重选等异常情况，这时需要调用 KafkaMetadataLog.scala#truncateToEndOffset()方法根据当前 Leader 节点版本信息进行日志截断以完成异常恢复，关于异常恢复的机制暂时不做深入讨论

FetchResponseData.EpochEndOffset divergingEpoch \= partitionResponse.divergingEpoch();

if (divergingEpoch.epoch() >= 0) {

// The leader is asking us to truncate before continuing

final OffsetAndEpoch divergingOffsetAndEpoch \= new OffsetAndEpoch(

divergingEpoch.endOffset(), divergingEpoch.epoch());

state.highWatermark().ifPresent(highWatermark -> {

if (divergingOffsetAndEpoch.offset < highWatermark.offset) {

throw new KafkaException("The leader requested truncation to offset " +

divergingOffsetAndEpoch.offset + ", which is below the current high watermark" + " " + highWatermark);

}

});

long truncationOffset \= log.truncateToEndOffset(divergingOffsetAndEpoch);

....

} else if (partitionResponse.snapshotId().epoch() >= 0 ||

partitionResponse.snapshotId().endOffset() >= 0) {

// The leader is asking us to fetch a snapshot

if (partitionResponse.snapshotId().epoch() < 0) {

....

return false;

} else if (partitionResponse.snapshotId().endOffset() < 0) {

....

return false;

} else {

final OffsetAndEpoch snapshotId \= new OffsetAndEpoch(

partitionResponse.snapshotId().endOffset(),

partitionResponse.snapshotId().epoch()

);

state.setFetchingSnapshot(log.storeSnapshot(snapshotId));

}

} else {

// 如果没有异常情况，则首先将 Fetch 响应携带的元数据消息取出来，调用 KafkaRaftClient.java#appendAsFollower()方法将其追加到本地日志文件，然后调用 KafkaRaftClient.java#updateFollowerHighWatermark() 方法尝试更新本地 HW

Records records \= FetchResponse.recordsOrFail(partitionResponse);

if (records.sizeInBytes() > 0) {

// 将其追加到本地日志文件

appendAsFollower(records);

}

OptionalLong highWatermark \= partitionResponse.highWatermark() < 0 ?

OptionalLong.empty() : OptionalLong.of(partitionResponse.highWatermark());

// 尝试更新本地 HW

updateFollowerHighWatermark(state, highWatermark);

}

// 设置拉取超时时间

state.resetFetchTimeout(currentTimeMs);

return true;

} else {

return handleUnexpectedError(error, responseMetadata);

}

}

核心步骤如下：

1.  首先检查 Fetch 响应中是否包含集群版本分歧的信息，如果有则可能是发生了 Leader 宕机重选等异常情况，这时需要调用 [KafkaMetadataLog.scala#truncateToEndOffset](http://kafkametadatalog.scala/#truncateToEndOffset)()方法根据当前 Leader 节点版本信息进行日志截断以完成异常恢复，关于异常恢复的机制暂时不做深入讨论。
2.  如果没有异常情况，则首先将 Fetch 响应携带的元数据消息取出来，调用 [KafkaRaftClient.java#appendAsFollower](http://kafkaraftclient.java/#appendAsFollower)()方法将其追加到本地日志文件，然后调用 [KafkaRaftClient.java#updateFollowerHighWatermark](http://kafkaraftclient.java/#updateFollowerHighWatermark)() 方法尝试更新本地 HW。

接着来继续看更新高水位的细节。

## **2.3.2 KafkaRaftClient#appendAsFollower()**

![](https://article-images.zsxq.com/FvG8VLuPgaSvLaWP7V9zxSa48YvB)

![](https://article-images.zsxq.com/Frq0ILYtH5QRVhtTDi47eqv6hpsd)

该方法主要用来**进行本地日志追加操作**，这是通用流程，如果忘记可以点击 [【服务端 Broker 源码分析系列第九篇】图解 Kafka 源码之 ReplicaManager 日志读写流程](https://articles.zsxq.com/id_5dq174gc0wji.html) 学习。

## **2.3.3 KafkaRaftClient#updateFollowerHighWatermark()**

private void updateFollowerHighWatermark(

FollowerState state,

OptionalLong highWatermarkOpt

) {

highWatermarkOpt.ifPresent(highWatermark -> {

long newHighWatermark = Math.min(endOffset().offset, highWatermark);

// 1、首先调用 FollowerState.java#updateHighWatermark() 方法尝试更新本地 HW

if (state.updateHighWatermark(OptionalLong.of(newHighWatermark))) {

logger.debug("Follower high watermark updated to {}", newHighWatermark);

// 2、如果 HW 更新成功，说明元数据变动开始生效，则需要两步处理：

// 1)、调用 KafkaMetadataLog.scala#updateHighWatermark() 方法更新日志文件的 HW

log.updateHighWatermark(new LogOffsetMetadata(newHighWatermark));

// 2)、调用 KafkaRaftClient#updateListenersProgress() 方法通知本地的元数据监听器。

updateListenersProgress(newHighWatermark);

}

});

}

该方法主要用来**更新 Follower 本地节点的 HW**，核心步骤如下：

1.  首先调用 [FollowerState.java#updateHighWatermark](http://followerstate.java/#updateHighWatermark)() 方法尝试更新本地 HW。
2.  如果 HW 更新成功，说明元数据变动开始生效，则需要两步处理：
3.  调用 [KafkaMetadataLog.scala#updateHighWatermark](http://kafkametadatalog.scala/#updateHighWatermark)() 方法更新日志文件的 HW
4.  调用 [KafkaRaftClient#updateListenersProgress()](http://kafkaraftclient/#updateListenersProgress\(\)) 方法通知本地的 KafkaRaftClient#listenerContexts 元数据监听器。

这里以「**创建 Topic 场景**」为例，HW 更新后，Kafka 集群的所有节点都需要检查自己是否需要负责新 Topic 下的分区的副本，如果是则进行本地日志文件创建等操作，而这些功能的实现就是借助这里的监听器通知机制，会在后面文章单独介绍，这里不再赘述。

## **2.3.4 FollowerState#updateHighWatermark()**

该方法比较简单，只需要注意 HW 是单调递增的，新的 HW 值比旧的 HW 大才能成功更新

![](https://article-images.zsxq.com/Fmps1knjctV_pfw7VjINh8isARxg)

至此，「**Follower 节点响应 Fetch 处理**」的源码流程就剖析完了。

##   
**03 元数据主从同步流程总结**

不知道，大家看完上面的源码是否还是云里雾里，找不着北。不过没关系，这里我通过一个案例场景来图解一下，希望可以让你更容易理解今天的内容，废话不多说，开始正题。

首先你需要了解的是：Kafka 对集群元数据主从同步的管理并不要求强一致性，只要求「**最终一致性**」，当集群内一半以上具有投票权的节点都保存了元数据消息则认为该消息处于 Committed 状态，可以被消费者进行消费。

所以对于「**最终一致性**」，集群元数据分区 HW 的同步可能会出现以下两种情况：

1.  需要两次 Fetch 请求才能完成 HW 同步。
2.  只需要一次 Fetch 请求就能完成 HW 同步。

> 这里需注意的一点是 \_\_cluster\_metadata 这个内部 topic 只有一个分区，而 Kafka 集群所有节点都会保存该分区的副本，不过只有具有投票权的 Controller 节点才能参与维护元数据。

接下来，我们就会挨个图解这两种场景。

## **3.1 需要两次 Fetch 请求才能完成 HW 同步**

![](https://article-images.zsxq.com/lq8u0w0E_PAF58CC1J59YdLl6hSz)

![](https://article-images.zsxq.com/lqvdNGuYOkJJI4Fru5kndtOAMYTz)

如图所示，对于「**Follower(Voter)**」发起两次 Fetch 请求来完成元数据及 HW 更新的过程，大致可以分为以下 4 个阶段：

1.  **初始状态**：当 Kafka 集群处于初始状态时，Leader 节点和所有 Follower 节点保存的元数据都是一致的，此时 HW 与 LEO 都指向同一个位置。**如上图中，Leader 节点和 Follower(Voter) 节点都保存了 Offset = 0 的消息，HW 和 LEO 都指向还未写入的 Offset = 1 的消息位置**。
2.  **当有元数据消息写入时**：Leader 节点接收外部请求，变更集群元数据时，会将元数据消息写入到本地的元数据分区副本，如图 **Leader 节点的 LEO 指向 Offset = 2 的位置**。此时 **Leader 节点还会尝试更新本地目标副本的状态**，其中包括了 HW 高水位更新，不过在当前阶段实际上是不会更新 HW 高水位的。
3.  具体的状态更新具体算法如下：
4.  判断当前节点是否有 controller 角色，没有则不进行更新处理。
5.  判断当前节点是否是有投票权的节点（由配置 controller.quorum.voters 参数来决定），不是则不进行更新处理
6.  将 voterStates 保存的所有副本状态对象按照 LEO 降序排列，取排序列表中间下标位置的副本的 LEO 作为 new HW 与当前的 old HW 比较，如果 new HW > old HW 则更新水位为 new HW。
7.  这里需要注意的是：该算法实际上**意味着集群元数据变更只要同步到一半以上的有投票权的节点上就认为集群元数据已经一致了，可以通知监听器元数据变更已生效**。
8.  **Follower 节点第一次发起 Fetch 请求**：Follower(Voter) 节点会定时发送 Fetch 请求到 Leader 节点同步元数据，**发起的请求中会携带本地分区副本的 LEO = 1**。 Leader 节点接收到请求后，会更新本地 voterStates 中保存的该 Follower 副本状态，**接着尝试更新本地目标副本的状态**。假设此时还没有超过一半以上有投票权的节点保存新消息，那 Leader 节点不会更新本地 HW，只是返回元数据消息。Follower 节点在处理 Fetch 响应时，仅会将元数据消息追加到本地副本，并将 LEO 指向 Offset=2 的位置，等待其他有投票权的节点保存完消息。
9.  **Follower 节点第二次发起 Fetch 请求**：与第一次发起 Fetch 请求类似，只不过此时**发起的请求中会携带本地分区副本的 LEO = 2**。假设此时已经有一半以上的有投票权的节点保存了新消息，那么 Leader 节点在**尝试更新本地目标副本的状态**时会更新本地 HW 指向 Offset=2 的位置，并在 Fetch 响应中将当前 HW 返回给 Follower 节点。Follower 节点会根据 Leader 节点的 HW 值更新本地副本 HW 值指向 Offset=2 的位置，最终完成 HW 更新同步。

## **3.2 只需要一次 Fetch 请求就能完成 HW 同步**

![](https://article-images.zsxq.com/lksqJNwN6hRBIXp0gyzLLoSkJ8gV)

如图所示，对于「**Follower(Voter)**」只发起1次 Fetch 请求来完成元数据及 HW 更新的过程，与发起 2次 Fetch 请求流程类似，可以对比研究。

不过这里需要注意的是：**当**「**Follower(Voter)**」**当节点在元数据变更后第一次发起 Fetch 请求时，如果已经有一半以上的有投票权的节点上保存了新的元数据信息，那么此时 Leader 节点的 HW 已经更新成功**。 「**Follower(Voter)**」节点可以在这第一次 Fetch 响应中感知到新的 HW，直接通过一次请求就完成本地 HW 更新同步。

## **04 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头引出了「**上篇中数据处理机制**」中 Leader 节点生成 Records 元数据，那么 Follower 节点是如何同步元数据的。

2、接着带大家深度剖析了「**元数据主从同步的源码流程**」，包括三个维度：「**Follower 节点发起 Fetch 请求流程**」、「**Leader 节点处理 Fetch 请求流程**」、「**Follower 节点响应 Fetch 处理流程**」。

3、最后通过两种「**元数据主从同步场景**」带大家梳理了整个处理流程，希望让你更好的理解。

下篇我们来深度剖析「**Kraft 消息数据主从同步机制**」，大家期待，我们下期见。