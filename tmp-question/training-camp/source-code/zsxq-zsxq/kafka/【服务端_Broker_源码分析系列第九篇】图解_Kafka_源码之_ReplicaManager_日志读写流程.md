大家好，我是 **华仔**, 又跟大家见面了。

上一篇中，主要带大家深度剖析了 「**Kafka 服务端日志存储机制及核心对象管理**」，通过「**场景驱动方式**」，现在消息被封装成批次请求已经从「**生产者**」发送到「**Broker**」，且被「**网络层**」所接收到并准备进行消息数据存储，从今天开始，我们来深度剖析 Kafka 日志系统的底层实现，这是日志系列第二篇，我们先来深度聊聊「**Kafka 服务端源码 ReplicaManger 日志读写流程**」，看看 Kafka 服务端是如何真正存储消息数据的。

![](https://article-images.zsxq.com/FpowTH-OhE9c2_p5tuskC9xmsA9L)

## **01 总体概述**

我们知道在 Producer 端，会维护一个 ConcurrentMap > batches 的变量以消息批次为数据单元存储消息数据，然后会根据 topic-partition 的 Leader 信息，将 Leader 在同一台 Broker 机器上的 batch 放在一个 request 中，发送到 Broker 端，这样可以节省很多网络开销，提高发送效率。

在上一篇中，我们 [【服务端 Broker 源码分析系列第八篇】图解 Kafka 源码之日志存储机制介绍以及核心对象管理梳理](https://articles.zsxq.com/id_rqum80f450hc.html) 引出了服务端日志存储的最上层 「**ReplicaManager**」对象。

接下来我们来对 「**ReplicaManager**」对象一探究竟，从名字上看就知道它是「**副本管理组件**」，其作用主要是负责管理一个 Broker 上的所有分区副本。

好了，下面开始剖析其源码实现，让你对整个日志管理有个总体的认知，深度剖析下其内部是如何进行日志读写的，整个流程是怎么样的？那么带着这些问题进入今天的正题。

「**ReplicaManager**」类中最重要的两个功能是 「**读取请求**」、「**写入日志**」。这里我们先从服务端接到读写日志请求的地方开始讲解。

整个日志管理的相关组件的调用关系图如下，下面会分别展开对这些组件的剖析。

  
![](https://article-images.zsxq.com/FgVmpr3JOm-_1PVfCHJC94zu4_tC)

## **02 写消息流程**

当生产者发送消息批次后，服务端是如何处理的呢？其实整个处理过程肯定离不开 Kafka 源码中「**功能最全**」、「**大名鼎鼎**」的工具类 「**KafkaApis**」。

KafkaApis 类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/KafkaApis.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/KafkaApis.scala)

我们还是从该类的 handle 方法开始讲起，如下图：

  
![](https://article-images.zsxq.com/FuI2oK-fn96xSF5XTXgRZzAN16ux)

从上图可以看出，对应 ApiKeys.PRODUCE 请求调用的方法 handleProduceRequest(request)，那么该方法内部都做了什么呢？我们来一探究竟。

##   
**2.1 handleProduceRequest()**

/\*\*

\* Handle a produce request

\*/

def handleProduceRequest(request: RequestChannel.Request): Unit = {

// 获取生产着发送请求信息，把 RequestChannel.Request 转换为 ProduceRequest 类型

val produceRequest \= request.body\[ProduceRequest\]

// 计算需要写入数据的大小

val numBytesAppended \= request.header.toStruct.sizeOf + request.sizeOfBodyInBytes

// 前置检查，判断 ProduceRequest 中是否含有事务消息，或者幂等消息。

if (produceRequest.hasTransactionalRecords) { // 事务消息

val isAuthorizedTransactional \= produceRequest.transactionalId != null &&

authorize(request.context, WRITE, TRANSACTIONAL\_ID, produceRequest.transactionalId)

// 如果事务未开启授权

if (!isAuthorizedTransactional) {

sendErrorResponseMaybeThrottle(request, Errors.TRANSACTIONAL\_ID\_AUTHORIZATION\_FAILED.exception)

return

}

// Note that authorization to a transactionalId implies ProducerId authorization

// 如果 produceRequest 含有幂等消息，但会话没有开启授权

} else if (produceRequest.hasIdempotentRecords && !authorize(request.context, IDEMPOTENT\_WRITE, CLUSTER, CLUSTER\_NAME)) {

sendErrorResponseMaybeThrottle(request, Errors.CLUSTER\_AUTHORIZATION\_FAILED.exception)

return

}

// 客户端消息数据 scala格式

val produceRecords \= produceRequest.partitionRecordsOrFail.asScala

// 未授权topic的 response

val unauthorizedTopicResponses \= mutable.Map\[TopicPartition, PartitionResponse\]()

// 不存在topic的 response

val nonExistingTopicResponses \= mutable.Map\[TopicPartition, PartitionResponse\]()

// 无效请求的 response

val invalidRequestResponses \= mutable.Map\[TopicPartition, PartitionResponse\]()

// 授权的请求消息，每个 topicPartation 对应的需要写入的日志数据

val authorizedRequestInfo \= mutable.Map\[TopicPartition, MemoryRecords\]()

// 授权的 topics

val authorizedTopics \= filterByAuthorized(request.context, WRITE, TOPIC, produceRecords)(\_.\_1.topic)

// 迭代 produceRecords 中的 partitionRecords

for ((topicPartition, memoryRecords) <- produceRecords) {

// 如果判断 topic 未授权，生成未授权 topic 的 response

if (!authorizedTopics.contains(topicPartition.topic))

unauthorizedTopicResponses += topicPartition -> new PartitionResponse(Errors.TOPIC\_AUTHORIZATION\_FAILED)

// 如果判断 broker 的元数据缓存不包含该 topicPartition，生成不存在 topic 的 response

else if (!metadataCache.contains(topicPartition))

nonExistingTopicResponses += topicPartition -> new PartitionResponse(Errors.UNKNOWN\_TOPIC\_OR\_PARTITION)

else

try {

// 校验消息格式

ProduceRequest.validateRecords(request.header.apiVersion, memoryRecords)

// 如果 broker 的元数据缓存包含该 topicPartition，将该二元组添加到 authorizedRequestInfo的 map 集合中。

authorizedRequestInfo += (topicPartition -> memoryRecords)

} catch {

// 抛异常

case e: ApiException =>

invalidRequestResponses += topicPartition -> new PartitionResponse(Errors.forException(e))

}

}

// the callback for sending a produce response

def sendResponseCallback(responseStatus: Map\[TopicPartition, PartitionResponse\]): Unit = {

val mergedResponseStatus \= responseStatus ++ unauthorizedTopicResponses ++ nonExistingTopicResponses ++ invalidRequestResponses

var errorInResponse \= false

mergedResponseStatus.forKeyValue { (topicPartition, status) =>

if (status.error != Errors.NONE) {

errorInResponse = true

debug("Produce request with correlation id %d from client %s on partition %s failed due to %s".format(

request.header.correlationId,

request.header.clientId,

topicPartition,

status.error.exceptionName))

}

}

.....

if (authorizedRequestInfo.isEmpty)

sendResponseCallback(Map.empty)

else {

// 是否允许向内部主题写入消息判断

val internalTopicsAllowed \= request.header.clientId == AdminUtils.AdminClientId

/\*\*

\* 调用 replicaManager 组件的方法，消息追加到磁盘的入口

\*/

// call the replica manager to append messages to the replicas

replicaManager.appendRecords(

timeout = produceRequest.timeout.toLong,

requiredAcks = produceRequest.acks,

internalTopicsAllowed = internalTopicsAllowed,

origin = AppendOrigin.Client,

entriesPerPartition = authorizedRequestInfo,

// 最后返回响应结果，把响应添加到响应队列中。

responseCallback = sendResponseCallback,

// 消息格式转换操作的回调统计逻辑，主要用于统计消息格式转换操作过程中的一些数据指标，比如总共转换了多少条消息，花费了多长时间。

recordConversionStatsCallback = processingStatsCallback)

// 帮助 GC 回收内存

produceRequest.clearPartitionRecords()

}

}

该方法源码比较长，中间省略了部分代码，主要**先过滤掉那些该客户端无权限的 topic，还有那些不存在的 topic(可能已经被删除)，将过滤后的所有消息交给 ReplicaManager 来处理**，大体流程如下：

1.  前置检查，判断 ProduceRequest 中是否含有事务消息，或者幂等消息，且未授权则返回错误。
2.  开始迭代 ProduceRequest 的 partitionRecord 分区消息，它是一个 (topicPartition, memoryRecords) 的二元组。
3.  如果判断 topic 未授权，生成未授权 topic 的 response。
4.  如果 broker 的元数据缓存不包含该 topicPartition，生成不存在 topic 的 response。
5.  如果 broker 的元数据缓存包含该 topicPartition，将该二元组添加到 authorizedRequestInfo 的 map 集合中。
6.  最后调用 ReplicaManager#appendRecords 方法，将 authorizedRequestInfo 追加到 Leader 副本的磁盘文件中。
7.  当追加完成后，调用 sendResponseCallback() 方法执行回调。

在整个流程中，最主要的是 「**第三步**」，那么我们来看看 ReplicaManager#appendRecords 方法内部做了什么？

##   
**2.2 appendRecords()**

「**ReplicaManager**」是 kafka 管理副本的组件，用来维护目标 broker 上各个 topic 的副本数据信息。

「**ReplicaManager**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/ReplicaManager.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/ReplicaManager.scala)

// 追加日志消息

def appendRecords(

timeout: Long, // 请求处理超时时间。对于生产者来说，它就是 request.timeout.ms 参数值。

requiredAcks: Short, // 是否需要等待其他副本写入。对于生产者来说，它就是 acks 参数的值。

internalTopicsAllowed: Boolean, // 是否允许向内部主题写入消息。对于普通的生产者而言，该字段是 false，即不允许写入内部主题。而对于 Coordinator 组件，特别是消费者组 GroupCoordinator 组件来说，它的职责之一就是向内部位移主题写入消息，此时该字段值是 true。

origin: AppendOrigin, // 是一个接口，表示写入方来源。当前，它定义了3类写入方，分别是Replication、Coordinator和Client

entriesPerPartition: Map\[TopicPartition, MemoryRecords\], // 按照分区分组的实际要写入的消息二元组集合

responseCallback: Map\[TopicPartition, PartitionResponse\] => Unit, // 写入成功后的回调方法

delayedProduceLock: Option\[Lock\] = None, // 专门用来保护消费者组操作线程安全的锁对象

recordConversionStatsCallback: Map\[TopicPartition, RecordConversionStats\] => Unit = \_ => ()

): Unit = {

// 1、校验 ack 合法值，如果非法直接返回错误响应

if (isValidRequiredAcks(requiredAcks)) {

val sTime \= time.milliseconds

// 2、调用 appendToLocalLog 方法写入消息到本地日志文件中，返回写入结果

val localProduceResults \= appendToLocalLog(internalTopicsAllowed = internalTopicsAllowed,

origin, entriesPerPartition, requiredAcks)

debug("Produce to local log in %d ms".format(time.milliseconds - sTime))

// 3、根据写日志返回的结果，封装返回给客户端的响应结果。

val produceStatus \= localProduceResults.map { case (topicPartition, result) =>

topicPartition ->

ProducePartitionStatus(

// 计算出下一条待写入消息的位移值

result.info.lastOffset + 1, // required offset

// 构建 PartitionResponse 封装写入结果

new PartitionResponse(result.error, result.info.firstOffset.getOrElse(-1), result.info.logAppendTime,

result.info.logStartOffset, result.info.recordErrors.asJava, result.info.errorMessage)) // response status

}

// 尝试触发操作

actionQueue.add {

() =>

localProduceResults.foreach {

case (topicPartition, result) =>

val requestKey \= TopicPartitionOperationKey(topicPartition)

result.info.leaderHwChange match {

case LeaderHwChange.Increased =>

// 一些延迟操作或许因为 HW 更改而被解除阻塞

delayedProducePurgatory.checkAndComplete(requestKey)

delayedFetchPurgatory.checkAndComplete(requestKey)

delayedDeleteRecordsPurgatory.checkAndComplete(requestKey)

case LeaderHwChange.Same =>

// 一些跟随者获取请求可能会因为日志末尾偏移量更新而解除阻塞

delayedFetchPurgatory.checkAndComplete(requestKey)

case LeaderHwChange.None =>

// 什么也不做

}

}

}

// 尝试更新消息格式转换的指标数据

recordConversionStatsCallback(localProduceResults.map { case (k, v) => k -> v.info.recordConversionStats })

// 4、根据 acks 的值，判断是否需要延迟响应客户端。通常当 acks = -1 时，需要通知其他 replica 复制消息，保证复制完了才会响应客户端，这时候就需要使用延迟操作，并构建对应的 DelayedProduce 对象

if (delayedProduceRequestRequired(requiredAcks, entriesPerPartition, localProduceResults)) {

// 构造 ProduceMetadata 对象，记录要写入的消息状态

val produceMetadata \= ProduceMetadata(requiredAcks, produceStatus)

// 构造 DelayedProduce 延迟请求对象，对写入结果进行异步等待。超过timeout时间后这个操作会被认定为超时，并立刻返回，发送响应给客户端。

val delayedProduce \= new DelayedProduce(timeout, produceMetadata, this, responseCallback, delayedProduceLock)

// 构建一个由 (topic, partition) 对列表，用来作为 DelayedProduce 延时请求对象的 key，然后遍历有哪些 topic 需要检查延迟操作是否完成。

val producerRequestKeys \= entriesPerPartition.keys.map(TopicPartitionOperationKey(\_)).toSeq

// 尝试立即响应请求，否则将其压入 Purgatory 中等待唤醒后处理

delayedProducePurgatory.tryCompleteElseWatch(delayedProduce, producerRequestKeys)

} else {

// 不需要等待响应立刻发送响应给客户端，直接将 produceStatus 转换成 response 内存块返回接收到的请求线程

val produceResponseStatus \= produceStatus.map { case (k, status) => k -> status.responseStatus }

// 调用回调方法

responseCallback(produceResponseStatus)

}

} else {

// 如果 required.acks 的值非法，说明客户端使用异常，直接返回错误响应

val responseStatus \= entriesPerPartition.map { case (topicPartition, \_) =>

topicPartition -> new PartitionResponse(Errors.INVALID\_REQUIRED\_ACKS,

LogAppendInfo.UnknownLogAppendInfo.firstOffset.getOrElse(-1),

RecordBatch.NO\_TIMESTAMP, LogAppendInfo.UnknownLogAppendInfo.logStartOffset)

}

// 调用回调方法

responseCallback(responseStatus)

}

}

我们先来看看该方法的重要字段：

1.  **timeout**：请求处理超时时间。对于生产者来说，它就是 [request.timeout.ms](http://request.timeout.ms/) 参数值。
2.  **requiredAcks**：对于生产者来说，即 acks 参数的值。而在其他场景中，Kafka 默认使用 -1| all，表示等待其他副本全部写入成功再返回，目的是保证消息的可靠性。
3.  **internalTopicsAllowed**：是否允许写入内部主题，所谓内部主题是为了维护消费者消费的\_\_consumer\_offsets 。
4.  对于普通的生产者来说，该字段是 false，即不允许写入内部主题。
5.  对于 Coordinator 组件，特别是消费者组 GroupCoordinator 组件来说，它的职责之一就是向内部位移主题写入消息，此时该字段值是 true。
6.  **AppendOrigin**：写入方来源，分别是 Replication、Coordinator 和 Client 3 类来源。
7.  Client：表示本次写入由客户端发起。
8.  Replication：表示写入请求是由 Follower 副本发出的，它要将从 Leader 副本获取到的消息写入到底层的消息日志中。
9.  Coordinator：表示这些写入由 Coordinator 发起，它既可以是管理消费者组的 GroupCooridnator，也可以是管理事务的 TransactionCoordinator。
10.  **entriesPerPartition**：按照分区分组的实际要写入的消息二元组集合, 在客户端发送批次时消息格式已确定，服务端直接存储。TopicPartition 指要追加的分区，MemoryRecords 指要追加的批次消息。
11.  **responseCallback**：写入成功后的回调方法。

**recordConversionStatsCallback**：消息格式转换操作的回调统计逻辑，主要用于统计消息格式转换操作过程中的一些数据指标，比如总共转换了多少条消息，花费了多长时间。

该方法主要用来**将 Producer 端请求中的消息批次追加到对应 TopicPartition 的日志中**，其步骤如下：

1.  校验 requiredAcks 合法性，其取值是-1、0、1，否则视为非法，因为生产者只会发送这三个参数，如果不是这三个参数说明有问题。
2.  调用 appendToLocalLog 方法写入消息集合到本地日志文件中，返回写入结果。
3.  根据写日志返回的结果，封装返回给客户端的响应结果。响应包括「**下一条待写入消息的位移**」、「**消息写入对应分区的结果类 PartitionResponse**」。
4.  根据 acks 的值，判断是否需要延迟响应客户端。通常当 acks = -1 时，需要通知其他 replica 复制消息，保证复制完了才会响应客户端，这时候就需要使用延迟操作，并构建对应的 DelayedProduce 对象。Kafka 服务端在处理客户端的一些请求时，如果不能及时返回响应结果给客户端，会在服务端创建一个延迟操作的对象（DelayedOperation），并放在延迟缓存中（DelayedOperationPurgatory），然后创建 DelayedProduce 对象，超过 timeout 时间后这个操作会被认定为超时，并立刻返回，发送响应给客户端。
5.  构建一个由 (topic, partition) 对列表，用来作为 DelayedProduce 延时请求对象的 key，然后遍历有哪些 topic 需要检查延迟操作是否完成。尝试立即响应请求，否则将其压入 Purgatory 中等待唤醒后处理
6.  调用回调方法，完成整个消息写入的流程。

在整个流程中，最主要的是 「**第二步**」，我们来重点看看**消息是如何被写入到 Leader 分区副本的****本地****日志中的**。对应的方法为 appendToLocalLog()。

## **2.3 appendToLocalLog()**

/\*\*

\* Append the messages to the local replica logs

\* 将消息追加到本地 Leader 副本的日志中

\*/

private def appendToLocalLog(internalTopicsAllowed: Boolean, // 是否允许写入内部主题

origin: AppendOrigin, // 写入来源

entriesPerPartition: Map\[TopicPartition, MemoryRecords\], // 按照分区分组的实际要写入的消息二元组集合

requiredAcks: Short): Map\[TopicPartition, LogAppendResult\] = {

val traceEnabled \= isTraceEnabled

// 当追加日志操作失败，则记录日志、发送统计数据，并返回产生错误的分区的起始偏移量

def processFailedRecord(topicPartition: TopicPartition, t: Throwable) = {

val logStartOffset \= getPartition(topicPartition) match {

case HostedPartition.Online(partition) => partition.logStartOffset

case HostedPartition.None | HostedPartition.Offline => -1L

}

brokerTopicStats.topicStats(topicPartition.topic).failedProduceRequestRate.mark()

brokerTopicStats.allTopicsStats.failedProduceRequestRate.mark()

error(s"Error processing append operation on partition $topicPartition", t)

logStartOffset

}

if (traceEnabled)

trace(s"Append \[$entriesPerPartition\] to local log")

// 1、遍历 entriesPerPartition 分区中的每个分区对应的消息数据 (topicPartition, records) 对象，然后一个个分区写消息集合。

entriesPerPartition.map { case (topicPartition, records) =>

// 统计请求的产生总量、失败总量

brokerTopicStats.topicStats(topicPartition.topic).totalProduceRequestRate.mark()

brokerTopicStats.allTopicsStats.totalProduceRequestRate.mark()

// reject appending to internal topics if it is not allowed

// 2、如果当前分区 topicPartition 是系统内部主题 \_\_consumer\_offsets，且在配置中被设置不允许追加操作，则追加操作被拒绝

// 内部主题是：topic:\_\_consumer\_offsets 和 \_\_transaction\_state

if (Topic.isInternal(topicPartition.topic) && !internalTopicsAllowed) {

(topicPartition, LogAppendResult(

LogAppendInfo.UnknownLogAppendInfo,

// 直接返回客户端错误信息

Some(new InvalidTopicException(s"Cannot append to internal topic ${topicPartition.topic}"))))

} else {

// 3、正常业务的 topic 执行逻辑

try {

// 获取 topicPartition 对应的日志分区对象 partition

val partition \= getPartitionOrException(topicPartition)

// 4、调用 partition 类的方法向 Leader 分区对象写入消息集合

val info \= partition.appendRecordsToLeader(records, origin, requiredAcks)

// 计算写入的消息数量

val numAppendedMessages \= info.numMessages

// update stats for successfully appended bytes and messages as bytesInRate and messageInRate

// 5、根据追加的数据计算更新部分统计信息，例如每秒追加的字节数 bytesInRate、每秒追加的消息总量 messagesInRate。

brokerTopicStats.topicStats(topicPartition.topic).bytesInRate.mark(records.sizeInBytes)

brokerTopicStats.allTopicsStats.bytesInRate.mark(records.sizeInBytes)

brokerTopicStats.topicStats(topicPartition.topic).messagesInRate.mark(numAppendedMessages)

brokerTopicStats.allTopicsStats.messagesInRate.mark(numAppendedMessages)

if (traceEnabled)

trace(s"${records.sizeInBytes} written to log $topicPartition beginning at offset " +

s"${info.firstOffset.getOrElse(-1)} and ending at offset ${info.lastOffset}")

// 6、返回封装追加结果 LogAppendResult 对象

(topicPartition, LogAppendResult(info))

} catch {

// 7、处理已知异常，例如未找到分区、非 Leader 副本、记录过大、批次条目过大、记录校验失败、记录数据损坏等异常，一旦追加失败，将为异常记录统计数据、打印日志，并返回相应的 LogAppendResult 对象

case e@ (\_: UnknownTopicOrPartitionException |

\_: NotLeaderOrFollowerException |

\_: RecordTooLargeException |

\_: RecordBatchTooLargeException |

\_: CorruptRecordException |

\_: KafkaStorageException) =>

(topicPartition, LogAppendResult(LogAppendInfo.UnknownLogAppendInfo, Some(e)))

case rve: RecordValidationException =>

val logStartOffset \= processFailedRecord(topicPartition, rve.invalidException)

val recordErrors \= rve.recordErrors

(topicPartition, LogAppendResult(LogAppendInfo.unknownLogAppendInfoWithAdditionalInfo(

logStartOffset, recordErrors, rve.invalidException.getMessage), Some(rve.invalidException)))

case t: Throwable =>

val logStartOffset \= processFailedRecord(topicPartition, t)

(topicPartition, LogAppendResult(LogAppendInfo.unknownLogAppendInfoWithLogStartOffset(logStartOffset), Some(t)))

}

}

}

}

该方法主要用来**将 Producer 端请求中的消息批次追加到本地 Leader 分区副本的****日志中**，从一个 Map 中取出多个 TopicPartition 和 MemoryRecords 对象，根据 requiredAcks 的要求通过 appendRecordsToLeader() 方法追加到相应的 TopicPartition 中，最后返回一个包含对每个 TopicPartition 的追加结果的 Map。其步骤如下：

1.  Broker 读取消息批次解析为 MemoryRecords 并存储到 entriesPerPartition 参数中，然后遍历 entriesPerPartition 分区中的每个分区对应的消息数据 (topicPartition, records) 对象，然后一个个分区写消息集合。
2.  如果当前分区 topicPartition 是系统内部主题 \_\_consumer\_offsets，且在配置中被设置不允许追加操作，则追加操作被拒绝。
3.  正常业务的 topic 执行逻辑，调用 getPartitionOrException 方法从 ReplicaManager#allPartitions 中找到消息对应的分区。
4.  向 Leader 分区对象写入消息集合，一个分区一个分区的写。
5.  根据追加的数据计算更新部分统计信息，例如每秒追加的字节数 bytesInRate、每秒追加的消息总量 messagesInRate。
6.  返回封装追加结果 LogAppendResult 对象。
7.  处理已知异常，例如未找到分区、非 Leader 副本、记录过大、批次条目过大、记录校验失败、记录数据损坏等异常，一旦追加失败，将为异常记录统计数据、打印日志，并返回相应的 LogAppendResult 对象。

在整个流程中，最主要的是 「**第四步**」，我们来重点看看**消息是如何被真正写入到对应分区的 Leader 副本的日志中的**。对应的方法为 Partition#appendRecordsToLeader()。

## **2.4 appendRecordsToLeader()**

这是「**Partition**」组件类的相关方法，「**Partition**」组件是 topic 在某个 broker 上一个副本的抽象。**每个 Parititon 对象都会维护一个 Replica 对象，而 Replica 对象中又会维护 Log 对象，也就是日志数据目录的抽象**。

「**Partition**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/cluster/Partition.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/cluster/Partition.scala)

这里我们先来了解一个重要集合「**allPartition**」, 它是一个 Partition 集合，存储了该 Broker 负责的所有副本的分区信息，包括「**Leader 副本**」和 「**Follower 副本**」的所有分区。

def appendRecordsToLeader(records: MemoryRecords, origin: AppendOrigin, requiredAcks: Int): LogAppendInfo = {

// 读锁

val (info, leaderHWIncremented) = inReadLock(leaderIsrUpdateLock) {

// 1、写之前先判断该 replica 是否是 Leader，如果不是 Leader 则没有写权限

leaderLogIfLocal match {

case Some(leaderLog) =>

// 最小 isr 同步副本集合

val minIsr \= leaderLog.config.minInSyncReplicas

// 当前 isr 同步副本集合数

val inSyncSize \= isrState.isr.size

// Avoid writing to leader if there are not enough insync replicas to make it safe

// 2、如果请求的 acks = -1，但是当前的 ISR 比配置的 minInSyncReplicas 还小，那要抛出错误，表示当前 ISR 不足。

if (inSyncSize < minIsr && requiredAcks == -1) {

throw new NotEnoughReplicasException(s"The size of the current ISR ${isrState.isr} " +

s"is insufficient to satisfy the min.isr requirement of $minIsr for partition $topicPartition")

}

// 3、消息通过 log 对象写入磁盘

val info \= leaderLog.appendAsLeader(records, leaderEpoch = this.leaderEpoch, origin,

interBrokerProtocolVersion)

// we may need to increment high watermark since ISR could be down to 1

// 4、变更该分区的高水位

(info, maybeIncrementLeaderHW(leaderLog))

case None \=\>

throw new NotLeaderOrFollowerException("Leader not local for partition %s on broker %d"

.format(topicPartition, localBrokerId))

}

}

info.copy(leaderHwChange = if (leaderHWIncremented) LeaderHwChange.Increased else LeaderHwChange.Same)

}

该方法比较简单，当「**Partition**」组件从 「**ReplicaManager**」拿到消息后：

1.  调用 leaderLogIfLocal 获取该 Log 实例，先判断当前 Broker 是否是分区的 Leader 副本，只有 Leader 副本才可以接收 producer 请求且有权限写数据。
2.  判断当前的 ISR 数量是否比 minInSyncReplicas 还小，如果 ISR 数量小于 minInSyncReplicas 就抛出异常。
3.  最后把消息交给自己管理的 Log 日志组件进行处理。
4.  变更该分区的高水位。

## **2.5 appendAsLeader()**

这是「**Log**」组件类的相关方法，执行到这里，已经定位到分区对于的「**Log**」实例。「**Log**」组件对象是对 「**Parititon**」数据目录的抽象。管理着某个 「**Topic**」在某个「**Broker**」的一个 「**Parititon**」,它可能是一个「**Leader**」，也可能是「**Replica**」。同时，「**Log**」组件对象还同时管理着多个 「**LogSegment**」即日志分段。

「**Log**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/Log.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/Log.scala)

/\*\*

\* 用来写 Leader 副本的，底层都调用了 append 方法。

\*/

def appendAsLeader(records: MemoryRecords,

leaderEpoch: Int,

origin: AppendOrigin = AppendOrigin.Client,

interBrokerProtocolVersion: ApiVersion = ApiVersion.latestVersion): LogAppendInfo = {

// assignOffsets = true 表示是 Leader 副本追加需要分配 offset

append(records, origin, interBrokerProtocolVersion, assignOffsets = true, leaderEpoch, ignoreRecordSize = false)

}

/\*\*

\* 注意：assignOffsets 是否需要分配位移，leader 副本需要，follwer 副本则不需要

\*/

private def append(records: MemoryRecords,

origin: AppendOrigin,

interBrokerProtocolVersion: ApiVersion,

assignOffsets: Boolean,

leaderEpoch: Int,

ignoreRecordSize: Boolean): LogAppendInfo = {

maybeHandleIOException(s"Error while appending records to $topicPartition in dir ${dir.getParent}") {

// 1、判断消息格式是否正确，并返回校验结果, 生成对应的 LogAppendInfo 对象

val appendInfo \= analyzeAndValidateRecords(records, origin, ignoreRecordSize)

// return if we have no valid messages or if this is a duplicate of the last appended entry

// 如果没有一条消息格式正确，直接返回

if (appendInfo.shallowCount == 0)

return appendInfo

// trim any invalid bytes or partial messages before appending it to the on-disk log

// 2、在将追加到磁盘上的日志分段之前，进行消息格式规整，修剪任何无效的字节或部分消息

var validRecords \= trimInvalidBytes(records, appendInfo)

// they are valid, insert them in the log

// 3、加锁处理消息，避免多个生产者同时写入该 Log 文件，如果需要进行记录偏移的分配，通过「消息验证器」的辅助，分配消息记录的偏移

lock synchronized {

// 确保 Log 对象未关闭

checkIfMemoryMappedBufferClosed()

// 如果需要给消息分配 offset的话，leader 副本需要，follwer 副本则不需要

if (assignOffsets) {

// assign offsets to the message set

// 计算第一条消息的 offset：使用当前 LEO 值作为待写入消息集合中第一条消息的位移值

val offset \= new LongRef(nextOffsetMetadata.messageOffset)

appendInfo.firstOffset = Some(offset.value)

val now \= time.milliseconds

val validateAndOffsetAssignResult \= try {

// 验证消息记录并分配偏移，给每一条消息设置 offset，并且找出 maxTimestamp 以及 maxTimestamp 对应的 offset

LogValidator.validateMessagesAndAssignOffsets(validRecords,

topicPartition,

offset,

time,

now,

appendInfo.sourceCodec,

appendInfo.targetCodec,

config.compact,

config.messageFormatVersion.recordVersion.value,

config.messageTimestampType,

config.messageTimestampDifferenceMaxMs,

leaderEpoch,

origin,

interBrokerProtocolVersion,

brokerTopicStats)

} catch {

case e: IOException =>

throw new KafkaException(s"Error validating messages while appending to log $name", e)

}

// 获取有效的记录，然后根据这些记录设置响应的返回内容

validRecords = validateAndOffsetAssignResult.validatedRecords

// 消息的最大时间戳和配置的 messageTimestampType 有关系。当前获取消息maxTimestamp时间戳的方式有两种。

appendInfo.maxTimestamp = validateAndOffsetAssignResult.maxTimestamp

// 根据消息的 timestamp 来设置时间戳

appendInfo.offsetOfMaxTimestamp = validateAndOffsetAssignResult.shallowOffsetOfMaxTimestamp

appendInfo.lastOffset = offset.value - 1

appendInfo.recordConversionStats = validateAndOffsetAssignResult.recordConversionStats

// 根据消息的写入时间来设置时间戳即当前时间

if (config.messageTimestampType == TimestampType.LOG\_APPEND\_TIME)

appendInfo.logAppendTime = now

// 4、由于前面的操作可能导致消息压缩格式改变以及消息格式改变，再次验证消息大小不超限，即检查当前的每条消息大小是否超过 maxMessageSize 的配置大小

if (!ignoreRecordSize && validateAndOffsetAssignResult.messageSizeMaybeChanged) {

for (batch <- validRecords.batches.asScala) {

if (batch.sizeInBytes > config.maxMessageSize) {

brokerTopicStats.topicStats(topicPartition.topic).bytesRejectedRate.mark(records.sizeInBytes)

brokerTopicStats.allTopicsStats.bytesRejectedRate.mark(records.sizeInBytes)

throw new RecordTooLargeException(s"Message batch size is ${batch.sizeInBytes} bytes in append to" +

s"partition $topicPartition which exceeds the maximum configured size of ${config.maxMessageSize}.")

}

}

}

}else {

// 接收客户端分配的偏移

if (!appendInfo.offsetsMonotonic) {

throw new OffsetsOutOfOrderException(s"Out of order offsets found in append to $topicPartition: " + records.records.asScala.map(\_.offset))

}

// 如果第一批消息的第一个或最后一个偏移值小于下一个消息的偏移值，则抛出异常

if (appendInfo.firstOrLastOffsetOfFirstBatch < nextOffsetMetadata.messageOffset) {

val firstOffset \= appendInfo.firstOffset match {

case Some(offset) => offset

case None \=\> records.batches.asScala.head.baseOffset()

}

val firstOrLast \= if (appendInfo.firstOffset.isDefined) "First offset" else "Last offset of the first batch"

throw new UnexpectedAppendOffsetException(

s"Unexpected offset in append to $topicPartition. $firstOrLast " +

s"${appendInfo.firstOrLastOffsetOfFirstBatch} is less than the next offset ${nextOffsetMetadata.messageOffset}. " +

s"First 10 offsets in append: ${records.records.asScala.take(10).map(\_.offset)}, last offset in" +

s" append: ${appendInfo.lastOffset}. Log start offset = $logStartOffset",

firstOffset, appendInfo.lastOffset)

}

}

// update the epoch cache with the epoch stamped onto the message by the leader

// 5、将每一条消息记录的 Leader Epoch 更新到 Leader Epoch Cache 缓存中

validRecords.batches.forEach { batch =>

if (batch.magic >= RecordBatch.MAGIC\_VALUE\_V2) {

maybeAssignEpochStartOffset(batch.partitionLeaderEpoch, batch.baseOffset)

} else {

// 确保 Leader Epoch 一致

leaderEpochCache.filter(\_.nonEmpty).foreach { cache =>

warn(s"Clearing leader epoch cache after unexpected append with message format v${batch.magic}")

cache.clearAndFlush()

}

}

}

// check messages set size may be exceed config.segmentSize

// 6、确保消息大小是否超过 config.segmentSize 的设置。即 1G 大小

// config.segmentSize = kafka.server.Defaults.LogSegmentBytes = 1 \* 1024 \* 1024 \* 1024

if (validRecords.sizeInBytes > config.segmentSize) {

throw new RecordBatchTooLargeException(s"Message batch size is ${validRecords.sizeInBytes} bytes in append " +

s"to partition $topicPartition, which exceeds the maximum configured segment size of ${config.segmentSize}.")

}

// maybe roll the log if this segment is full

// 7、写入日志分段，检查是否需要切换到新的日志段，如果当前日志分段已满，则创建新的日志段，替换当前写入的旧日志段。

// 当前日志段剩余容量可能无法容纳新消息集合，因此有必要创建一个新的日志段来保存待写入的所有消息

val segment \= maybeRoll(validRecords.sizeInBytes, appendInfo)

// 8、构建日志分段元数据（即写入的消息记录的元数据）

val logOffsetMetadata \= LogOffsetMetadata(

messageOffset = appendInfo.firstOrLastOffsetOfFirstBatch,

segmentBaseOffset = segment.baseOffset,

relativePositionInSegment = segment.size)

// 验证并更新 Producer 状态，主要关注事务 ID 和幂等 ID 的状态

val (updatedProducers, completedTxns, maybeDuplicate) = analyzeAndValidateProducerState(

logOffsetMetadata, validRecords, origin)

// 9、验证是否存在重复消息记录

maybeDuplicate.foreach { duplicate =>

appendInfo.firstOffset = Some(duplicate.firstOffset)

appendInfo.lastOffset = duplicate.lastOffset

appendInfo.logAppendTime = duplicate.timestamp

appendInfo.logStartOffset = logStartOffset

return appendInfo

}

// 10、执行真正的消息写入操作，主要调用日志段对象的 append 方法实现

segment.append(largestOffset = appendInfo.lastOffset,

largestTimestamp = appendInfo.maxTimestamp,

shallowOffsetOfMaxTimestamp = appendInfo.offsetOfMaxTimestamp,

records = validRecords)

// 11、更新 Log#nextOffsetMetadata 即下一条消息的偏移量

updateLogEndOffset(appendInfo.lastOffset + 1)

// 更新 Producer 状态

for (producerAppendInfo <- updatedProducers.values) {

producerStateManager.update(producerAppendInfo)

}

// 更新事务索引状态

for (completedTxn <- completedTxns) {

val lastStableOffset \= producerStateManager.lastStableOffset(completedTxn)

segment.updateTxnIndex(completedTxn, lastStableOffset)

producerStateManager.completeTxn(completedTxn)

}

// 总是更新最后的 Producer ID 映射偏移量，以便快照反映当前偏移量状态

producerStateManager.updateMapEndOffset(appendInfo.lastOffset + 1)

// 更新不稳定的首个偏移量（用于计算 LSO）

maybeIncrementFirstUnstableOffset()

trace(s"Appended message set with last offset: ${appendInfo.lastOffset}, " +

s"first offset: ${appendInfo.firstOffset}, " +

s"next offset: ${nextOffsetMetadata.messageOffset}, " +

s"and messages: $validRecords")

// 12、如果未刷新的消息超过刷新间隔，则执行强制刷新

if (unflushedMessages >= config.flushInterval)

flush()

// 返回追加的消息

appendInfo

}

}

}

该方法相对复杂，当「**Log**」日志组件从 「**Partition**」分区拿到消息后，调用底层的 append 方法进行追加：

1.  判断消息格式是否正确，并返回校验结果, 生成对应的 LogAppendInfo 对象。
2.  在将追加到磁盘上的日志分段之前，进行消息格式规整，修剪任何无效的字节或部分消息。
3.  加锁处理消息，避免多个生产者同时写入该 Log 文件，如果需要进行记录偏移的分配，通过「消息验证器」的辅助，分配消息记录的偏移。
4.  确保 Log 对象未关闭。
5.  如果需要给消息分配 offset的话，leader 副本需要，follwer 副本则不需要。
6.  计算第一条消息的 offset：使用当前 LEO 值作为待写入消息集合中第一条消息的位移值
7.  验证消息记录并分配偏移。
8.  由于前面的操作可能导致消息压缩格式改变以及消息格式改变，再次验证消息大小不超限，即检查当前的每条消息大小是否超过 maxMessageSize 的配置大小。
9.  将每一条消息记录的 Leader Epoch 更新到 Leader Epoch Cache 缓存中。
10.  确保消息大小是否超过 config.segmentSize 的设置，即 1G 大小 。
11.  写入日志分段，检查是否需要切换到新的日志段，如果当前日志分段已满，则创建新的日志段，替换当前写入的旧日志段。
12.  构建日志分段元数据（即写入的消息记录的元数据）。
13.  执行真正的消息写入操作，主要调用日志段对象的 append 方法实现。
14.  更新 Log#nextOffsetMetadata，即下一条消息的偏移量、更新 Producer 状态、更新事务索引状态。
15.  每当写入的未刷新的消息量超过刷新间隔 config.flushInterval 时，则执行强制刷新。其中 config.flushInterval 由 Broker 配置的参数 flush.messages 来指定，默认不开启。

## **2.6 重要字段说明**

这里有两个重要字段需要说明下：

首先 **Log#nextOffsetMetadata** 指向的是 「**Log**」文件当前最新消息的下一个消息的偏移量，也被称作为 「**LogEndOffset**」，简称 「**LEO**」。

  
![](https://article-images.zsxq.com/FkwNSu6KDTjTZ9YTzylSnTxG4t4j)

其次，针对上面 append 方法中如下代码，**检查是否需要切换到新的日志段**，如果当前日志分段已满，则创建新的日志段，替换当前写入的旧日志段。

val segment \= maybeRoll(validRecords.sizeInBytes, appendInfo)

在 Kafka 中，是根据 Broker 的配置项来决定是否切换日志段的，有以下三个：

1.  **log.roll.hours**: 如果日志文件创建时间达到该配置项值，则切换日志段，默认值为 168 小时，即 7天。
2.  **log.segment.bytes**: 如果日志文件达到该配置项值，则切换日志段，默认值为 1G。
3.  **log.index.size.max.bytes**: 如果索引文件达到该配置项值，则切换日志段，默认值为 10 M。

## **2.7 maybeRoll()**

private def maybeRoll(messagesSize: Int, appendInfo: LogAppendInfo): LogSegment = {

// 1、获取当前活跃的日志段

val segment \= activeSegment

// 2、获取当前系统时间

val now \= time.milliseconds

// 3、获取消息中的最大时间戳和最大偏移量

val maxTimestampInMessages \= appendInfo.maxTimestamp

val maxOffsetInMessages \= appendInfo.lastOffset

// 4、判断是否需要创建一个新日志段

if (segment.shouldRoll(RollParams(config, appendInfo, messagesSize, now))) {

// 打印调试信息

debug(s"Rolling new log segment (log\_size = ${segment.size}/${config.segmentSize}}, " +

s"offset\_index\_size = ${segment.offsetIndex.entries}/${segment.offsetIndex.maxEntries}, " +

s"time\_index\_size = ${segment.timeIndex.entries}/${segment.timeIndex.maxEntries}, " +

s"inactive\_time\_ms = ${segment.timeWaitedForRoll(now, maxTimestampInMessages)}/${config.segmentMs - segment.rollJitterMs}).")

/\*

\* maxOffsetInMessages - Integer.MAX\_VALUE is a heuristic value for the first offset in the set of messages.

\* Since the offset in messages will not differ by more than Integer.MAX\_VALUE, this is guaranteed <= the real

\* first offset in the set. Determining the true first offset in the set requires decompression, which the follower

\* is trying to avoid during log append. Prior behavior assigned new baseOffset = logEndOffset from old segment.

\* This was problematic in the case that two consecutive messages differed in offset by

\* Integer.MAX\_VALUE.toLong + 2 or more. In this case, the prior behavior would roll a new log segment whose

\* base offset was too low to contain the next message. This edge case is possible when a replica is recovering a

\* highly compacted topic from scratch.

\* Note that this is only required for pre-V2 message formats because these do not store the first message offset

\* in the header.

\*/

appendInfo.firstOffset match {

// 如果消息中有第一个偏移量，就使用它来创建新日志段

case Some(firstOffset) => roll(Some(firstOffset))

// 如果消息中不存在第一个偏移量，则使用一个启发式值来创建新日志段

case None \=\> roll(Some(maxOffsetInMessages - Integer.MAX\_VALUE))

}

} else {

// 如果不需要创建新日志段，则返回当前活跃的日志段

segment

}

}

该方法主要用来**写入日志段消息**，如果当前日志分段已满，则可能触发日志切分。步骤如下：

1.  获取当前活跃的日志段。
2.  获取当前系统时间。
3.  获取消息中的最大时间戳和最大偏移量。
4.  判断是否需要创建一个新日志段。
5.  如果消息中有第一个偏移量，就使用它来创建新日志段。
6.  如果消息中不存在第一个偏移量，则使用一个启发式值来创建新日志段。
7.  如果不需要创建新日志段，则直接返回当前活跃的日志段。

在上面追加日志整个流程中，最主要的是 「**第九步**」，我们来重点看看**消息是如何被真正写入到日志段中的**。对应的方法为 LogSegment#append()。

##   
**2.8 LogSegment.append()**

这是「**LogSegment**」组件类的相关方法，执行到这里，已经定位到具体的日志段「**LogSegment**」。「**LogSegment**」组件对象是对 「**Parititon**」数据目录中数据段的抽象。kafka 会将一个副本中日志根据配置大小进行分段。这个 「**LogSegment**」对象维护数据文件以及索引文件的信息。

「**LogSegment**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogSegmen](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogSegment.scala)[t.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/Log.scala)

/\*\*

\* Append the given messages starting with the given offset. Add

\* an entry to the index if needed.

\*

\* It is assumed this method is being called from within a lock.

\*

\* @param largestOffset The last offset in the message set

\* @param largestTimestamp The largest timestamp in the message set.

\* @param shallowOffsetOfMaxTimestamp The offset of the message that has the largest timestamp in the messages to append.

\* @param records The log entries to append.

\* @return the physical position in the file of the appended records

\* @throws LogSegmentOffsetOverflowException if the largest offset causes index offset overflow

\*/

@nonthreadsafe

def append(largestOffset: Long, // 最大位移

largestTimestamp: Long, // 最大时间戳

shallowOffsetOfMaxTimestamp: Long, // 最大时间戳对应消息的位移

records: MemoryRecords): Unit = { // 真正要写入的消息集合

if (records.sizeInBytes > 0) {

trace(s"Inserting ${records.sizeInBytes} bytes at end offset $largestOffset at position ${log.sizeInBytes} " +

s"with largest timestamp $largestTimestamp at shallow offset $shallowOffsetOfMaxTimestamp")

// 1、判断该日志段是否为空

val physicalPosition \= log.sizeInBytes()

if (physicalPosition == 0)

// 记录要写入消息集合的最大时间戳，并将其作为后面新增日志段倒计时的依据

rollingBasedTimestamp = Some(largestTimestamp)

// 2、确保输入参数最大位移值是合法的

ensureOffsetInRange(largestOffset)

// append the messages

// 3、这里是一个 FileRecords 实例，调用其 append 方法将内存中的消息对象写入到操作系统的页缓存。

val appendedBytes \= log.append(records)

trace(s"Appended $appendedBytes to ${log.file} at end offset $largestOffset")

// Update the in memory max timestamp and corresponding offset.

// 4、更新日志段的最大时间戳以及最大时间戳所属消息的位移值属性

if (largestTimestamp > maxTimestampSoFar) {

// 最大时间戳

maxTimestampSoFar = largestTimestamp

// 最大时间戳对应消息的位移

offsetOfMaxTimestampSoFar = shallowOffsetOfMaxTimestamp

}

// append an entry to the index (if needed)

// 5、更新索引项和写入的字节数，kafka 保证时间戳索引项保存时间戳与消息位移的对应关系

// 索引是稀疏哈希索引，不是每条消息都对应一条索引。写 4K 即4096字节，会更新一次索引。

if (bytesSinceLastIndexEntry > indexIntervalBytes) {

// 追加索引项

offsetIndex.append(largestOffset, physicalPosition)

// 更新 timeIndex

timeIndex.maybeAppend(maxTimestampSoFar, offsetOfMaxTimestampSoFar)

// 索引写完后重置，继续 4K 字节写索引

bytesSinceLastIndexEntry = 0

}

// 6、计算累计写入 .log 文件的消息大小

bytesSinceLastIndexEntry += records.sizeInBytes

}

}

该方法主要用来**将消息写入到日志目录下的物理文件中，并更新索引项**。大体步骤如下：

1.  判断该日志段是否为空。
2.  确保输入参数最大位移值是合法的。
3.  这里是一个 FileRecords 实例，调用其 append 方法将内存中的消息对象写入到操作系统的页缓存。
4.  更新日志段的最大时间戳以及最大时间戳所属消息的位移值属性。
5.  更新索引项和写入的字节数，kafka 保证时间戳索引项保存时间戳与消息位移的对应关系。bytesSinceLastIndexEntry 记录了上次写入索引信息后该 Log 日志新写入的消息大小，当其大于 LogSegment#indexIntervalBytes 时，则需要写入新的索引信息。**LogSegment#indexIntervalBytes** 属性是由 Broker 端配置项 log.index.interval.bytes 来指定的，默认值为 4 KB。
6.  追加日志索引项。
7.  更新 timeIndex。
8.  索引写完后重置，继续 4K 字节写索引。
9.  这三块到哈希索引架构篇再详细讲解，位置索引跟时间索引都用到了「**mmap**」机制，将文件直接映射到内存，对内存的修改最终由操作系统同步到文件中 。
10.  计算累计写入 .log 文件的消息大小。

在上面追加日志目录整个流程中，最主要的是 「**第三步**」、「**第五步**」，我们来重点看看第三步**消息是如何被写入到操作系统的页缓存中的**。对应的方法为 FileRecords#append()。

## **2.9 FileRecords.append()**

Kafka 底层的写数据是根据 「**fileChannel**」来写的，它写的时候不会「**立刻刷盘**」，而是开启了一个「**定时任务根据策略去刷盘**」。但是在默认情况下，这个定时任务又是不刷盘的（刷盘策略都不满足），Kafka 把刷盘的时机交给「**操作系统**」来掌控。

/\*\*

\* Append a set of records to the file. This method is not thread-safe and must be

\* protected with a lock.

\* 将内存中的消息对象写入到操作系统的页缓存中。

\* @param records The records to append

\* @return the number of bytes written to the underlying file

\*/

public int append(MemoryRecords records) throws IOException {

// 1、检查待写入的消息记录长度是否超过了当前日志段剩余的容量

if (records.sizeInBytes() > Integer.MAX\_VALUE - size.get())

throw new IllegalArgumentException("Append of size " + records.sizeInBytes() +

" bytes is too large for segment with current file position at " + size.get());

// 2、将消息记录写入到文件通道中，并返回实际写入的字节数

int written \= records.writeFullyTo(channel);

// 3、更新日志段的大小

size.getAndAdd(written);

// 4、返回写入的字节数

return written;

}

该方法主要用来**将指定的消息记录（MemoryRecords）写入到日志段的文件通道中，并返回字节数**。其中 MemoryRecords 是内存的消息，channel 对应的是「**FileChannel**」属于操作系统的 「**PageCache**」。这里最重要的是「**第二步**」，底层调用「**FileChannel**」写数据。这里的「**FileChannel**」可以认为是操作系统中的内核缓冲区。

## **2.10 MemoryRecords.writeFullyTo()**

/\*\*

\* Write all records to the given channel (including partial records).

\* @param channel The channel to write to

\* @return The number of bytes written

\* @throws IOException For any IO errors writing to the channel

\*/

public int writeFullyTo(GatheringByteChannel channel) throws IOException {

// 标记一下 position 的位置

buffer.mark();

int written \= 0;

//数据必须写完，才能出 while 循环

while (written < sizeInBytes())

//调用 FileChannel 写数据

written += channel.write(buffer);

// 恢复标记的位置

buffer.reset();

return written;

}

该方法比较简单，主要就是循环调用 「**FileChannel**」进行写数据，它可以将直接内存数据写入到文件中，避免在 JVM 中进行复制数据，提高 I/O 效率。从 「**MemroyRecords**」到 「**FileChannel**」的流程如下图：

![](https://article-images.zsxq.com/FpX7g_4XUstj7Tmt1zsKnXCX1pQ3)

这里我们看下 Kafka 中日志数据的存储格式如下：

\# /tmp/kafka-logs/ 日志存储目录，根据自己情况配置

\# message3-1-0 topic - parititon - replica

bin/kafka-dump-log.sh --files /tmp/kafka-logs/message3-1-0/00000000000000000000.log --print-data-log

![](https://article-images.zsxq.com/FioAQhHxW_GJ13YVrYu7cv-CQmbd)

从上面命令打印的内容可以看出，存储的消息批次中包含以下内容：

1.  **header**：消息批次的头部信息，包括「**消息批次的第一个消息偏移量 baseOffset**」、「**最后一个消息偏移量 lastOffset**」、「**消息数量 count**」、「**生产者ID producerId**」、「**分区 partitionLeaderEpoch**」、「**是否为事务消息 isTransactional**」、「**物理位置 position**」、「**批次大小 size**」、「**压缩类型 compresscodec**」、「**校验码 crc**」等。
2.  **payload**：消息内容，如 payload：message3-1。

至此，我们将「**消息写入**」的全流程就剖析完了，下面通过一张图来从整体上来梳理下：

![](https://article-images.zsxq.com/li9CNnd6Mb43PhS4-HU_nKnFZQ9Q)

## **03 读消息流程**

当消费者或者 follower 副本发起读取消息请求后，服务端是如何处理的呢？其实整个处理过程肯定还是离不开 Kafka 源码中「**功能最全**」、「**大名鼎鼎**」的工具类 「**KafkaApis**」。

KafkaApis 类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/KafkaApis.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/KafkaApis.scala)

我们还是从该类的 handle 方法开始讲起，如下图：

![](https://article-images.zsxq.com/FuI2oK-fn96xSF5XTXgRZzAN16ux)

从上图可以看出，对应 ApiKeys.FETCH 请求调用的方法 handleFetchRequest(request)，那么该方法内部都做了什么呢？我们来一探究竟。

##   
**3.1 handleFetchRequest()**

/\*\*

\* 处理获取消息的请求

\*/

def handleFetchRequest(request: RequestChannel.Request): Unit = {

// 1、获取请求的 API 版本和客户端 ID，以及请求的数据（FetchRequest）

val versionId \= request.header.apiVersion

val clientId \= request.header.clientId

// 把 RequestChannel.Request 转换为 FetchRequest 类型

val fetchRequest \= request.body\[FetchRequest\]

// 2、根据FetchRequest 构建 fetch 上下文对象 FetchContext并设置客户端元数据

val fetchContext \= fetchManager.newContext(

fetchRequest.metadata,

fetchRequest.fetchData,

fetchRequest.toForget,

fetchRequest.isFromFollower)

// 构建请求 Clients 的元数据

val clientMetadata: Option\[ClientMetadata\] = if (versionId >= 11) {

// Fetch API 版本 >= 11 时才有一个新的 preferred replica 逻辑，需要设置 clientId 和其他相关信息

Some(new DefaultClientMetadata(

fetchRequest.rackId,

clientId,

request.context.clientAddress,

request.context.principal,

request.context.listenerName.value))

} else {

None

}

// 3、创建错误响应，如果需要进行记录或者出错时使用

def errorResponse\[T >: MemoryRecords <: BaseRecords\](error: Errors): FetchResponse.PartitionData\[T\] = {

// error: 错误代码

// highWatermark / lastStableOffset / logStartOffset: 长整型偏移值

// preferredReadReplica / abortedTransactions / divergingEpoch: 相关信息

// records: 消息记录数据

new FetchResponse.PartitionData\[T\](error, FetchResponse.INVALID\_HIGHWATERMARK, FetchResponse.INVALID\_LAST\_STABLE\_OFFSET,

FetchResponse.INVALID\_LOG\_START\_OFFSET, null, MemoryRecords.EMPTY)

}

// 4、错误返回，分别记录需要跳过和需要处理的分区信息，需要分情况处理

val erroneous \= mutable.ArrayBuffer\[(TopicPartition, FetchResponse.PartitionData\[Records\])\]()

// 需要拉取消息的 TopicPartition

val interesting \= mutable.ArrayBuffer\[(TopicPartition, FetchRequest.PartitionData)\]()

// 如果是 follower 发起的 Fetch 请求

if (fetchRequest.isFromFollower) {

if (authorize(request.context, CLUSTER\_ACTION, CLUSTER, CLUSTER\_NAME)) {

// 首先需要授权才允许获取数据

fetchContext.foreachPartition { (topicPartition, data) =>

if (!metadataCache.contains(topicPartition))

// 如果对应分区不存在，则记录下该分区对应的错误代码

erroneous += topicPartition -> errorResponse(Errors.UNKNOWN\_TOPIC\_OR\_PARTITION)

else

// 否则加入到需要处理的列表中

interesting += (topicPartition -> data)

}

} else {

// 如果鉴权失败，则记录下该分区对应的错误代码

fetchContext.foreachPartition { (part, \_) =>

erroneous += part -> errorResponse(Errors.TOPIC\_AUTHORIZATION\_FAILED)

}

}

} else {

// 如果是普通的消费者，则需要通过授权来决定是否需要获取数据

val partitionDatas \= new mutable.ArrayBuffer\[(TopicPartition, FetchRequest.PartitionData)\]

fetchContext.foreachPartition { (topicPartition, partitionData) =>

partitionDatas += topicPartition -> partitionData

}

val authorizedTopics \= filterByAuthorized(request.context, READ, TOPIC, partitionDatas)(\_.\_1.topic)

partitionDatas.foreach { case (topicPartition, data) =>

if (!authorizedTopics.contains(topicPartition.topic))

// 如果对应主题没有被授权，则记录下该分区对应的错误代码

erroneous += topicPartition -> errorResponse(Errors.TOPIC\_AUTHORIZATION\_FAILED)

else if (!metadataCache.contains(topicPartition))

// 如果对应分区不存在，则记录下该分区对应的错误代码

erroneous += topicPartition -> errorResponse(Errors.UNKNOWN\_TOPIC\_OR\_PARTITION)

else

// 否则加入到需要处理的列表中，构建需要拉取消息的<topicPartition, FetchRequest.PartitionData)>

interesting += (topicPartition -> data)

}

}

// 5、下面是具体的数据处理逻辑，首先需要处理下版本不兼容的情况，之后进行数据转换

def maybeDownConvertStorageError(error: Errors, version: Short): Errors = {

// 如果请求版本是 Fetch API V5 或更早的版本，则客户端不能识别错误代码 KafkaStorageException

// 这时需要将该错误码转换为 NotLeaderForPartitionException，从而让客户端更新元数据并重新尝试

if (error == Errors.KAFKA\_STORAGE\_ERROR && versionId <= 5) {

Errors.NOT\_LEADER\_OR\_FOLLOWER

} else {

error

}

}

// 6、下面是具体的数据格式转换逻辑

def maybeConvertFetchedData(tp: TopicPartition,

partitionData: FetchResponse.PartitionData\[Records\]): FetchResponse.PartitionData\[BaseRecords\] = {

val logConfig \= replicaManager.getLogConfig(tp)

// 检查消息是否被 ZStandard 压缩，如果是，且客户端版本不支持，则返回 UNSUPPORTED\_COMPRESSION\_TYPE 错误码

if (logConfig.exists(\_.compressionType == ZStdCompressionCodec.name) && versionId < 10) {

trace(s"Fetching messages is disabled for ZStandard compressed partition $tp. Sending unsupported version response to $clientId.")

errorResponse(Errors.UNSUPPORTED\_COMPRESSION\_TYPE)

} else {

// 判断是否需要进行降级转换

val unconvertedRecords \= partitionData.records

val downConvertMagic \=

logConfig.map(\_.messageFormatVersion.recordVersion.value).flatMap { magic =>

if (magic > RecordBatch.MAGIC\_VALUE\_V0 && versionId <= 1 && !unconvertedRecords.hasCompatibleMagic(RecordBatch.MAGIC\_VALUE\_V0))

Some(RecordBatch.MAGIC\_VALUE\_V0)

else if (magic > RecordBatch.MAGIC\_VALUE\_V1 && versionId <= 3 && !unconvertedRecords.hasCompatibleMagic(RecordBatch.MAGIC\_VALUE\_V1))

Some(RecordBatch.MAGIC\_VALUE\_V1)

else

None

}

downConvertMagic match {

case Some(magic) =>

// 如果保存的消息版本比客户端支持的版本高，则需要进行降级转换

if (!fetchRequest.isFromFollower && !logConfig.forall(\_.messageDownConversionEnable)) {

// 普通客户端不支持降级转换则返回 UNSUPPORTED\_VERSION 错误码

trace(s"Conversion to message format ${downConvertMagic.get} is disabled for partition $tp. Sending unsupported version response to $clientId.")

errorResponse(Errors.UNSUPPORTED\_VERSION)

} else {

// 进行消息转换

try {

trace(s"Down converting records from partition $tp to message format version $magic for fetch request from $clientId")

// 需要注意延迟加载为了避免内存占用过大的问题

val error \= maybeDownConvertStorageError(partitionData.error, versionId)

new FetchResponse.PartitionData\[BaseRecords\](error, partitionData.highWatermark,

partitionData.lastStableOffset, partitionData.logStartOffset,

partitionData.preferredReadReplica, partitionData.abortedTransactions,

new LazyDownConversionRecords(tp, unconvertedRecords, magic, fetchContext.getFetchOffset(tp).get, time))

} catch {

case e: UnsupportedCompressionTypeException =>

// 返回 UNSUPPORTED\_COMPRESSION\_TYPE 错误码

trace("Received unsupported compression type error during down-conversion", e)

errorResponse(Errors.UNSUPPORTED\_COMPRESSION\_TYPE)

}

}

case None \=\>

// 如果不需要转换，则直接返回原始数据

val error \= maybeDownConvertStorageError(partitionData.error, versionId)

new FetchResponse.PartitionData\[BaseRecords\](error,

partitionData.highWatermark, partitionData.lastStableOffset,

partitionData.logStartOffset, partitionData.preferredReadReplica,

partitionData.abortedTransactions, partitionData.divergingEpoch, unconvertedRecords)

}

}

}

// 7、处理 Fetch 响应的回调函数，在执行流控之前调用

def processResponseCallback(responsePartitionData: Seq\[(TopicPartition, FetchPartitionData)\]): Unit = {

// 构建一个 LinkedHashMap 来存储所有分区的数据

val partitions \= new util.LinkedHashMap\[TopicPartition, FetchResponse.PartitionData\[Records\]\]

// 构建一个可变的 Set 来存储正在重新分配的分区

val reassigningPartitions \= mutable.Set\[TopicPartition\]()

// 遍历分区处理响应数据

responsePartitionData.foreach { case (tp, data) =>

// 获取该分区的错误信息，如果没有错误则为 NONE

val error \= maybeDownConvertStorageError(data.error, versionId)

// 将分区数据转换为 FetchResponse.PartitionData 格式

partitions.put(tp, new FetchResponse.PartitionData(

error,

data.highWatermark,

data.lastStableOffset.getOrElse(FetchResponse.INVALID\_LAST\_STABLE\_OFFSET),

data.logStartOffset,

data.preferredReadReplica.map(int2Integer).asJava,

data.abortedTransactions.map(\_.asJava).orNull,

data.divergingEpoch.asJava,

data.records))

// 如果该分区是正在重新分配的分区，则保存到 reassigningPartitions 中

if (data.isReassignmentFetch)

reassigningPartitions.add(tp)

}

// 处理异常数据的情况

erroneous.foreach { case (tp, data) => partitions.put(tp, data) }

// 创建一个空的 FetchResponse

var unconvertedFetchResponse: FetchResponse\[Records\] = null

// 8、创建 Fetch 响应对象的函数

def createResponse(throttleTimeMs: Int): FetchResponse\[BaseRecords\] = {

// 新建一个 LinkedHashMap 来保存转换后的分区数据

val convertedData \= new util.LinkedHashMap\[TopicPartition, FetchResponse.PartitionData\[BaseRecords\]\]

// 遍历所有分区的未转换数据

unconvertedFetchResponse.responseData.forEach { (tp, unconvertedPartitionData) =>

// 如果该分区有错误，则打印日志

if (unconvertedPartitionData.error != Errors.NONE)

debug(s"Fetch request with correlation id ${request.header.correlationId} from client $clientId " +

s"on partition $tp failed due to ${unconvertedPartitionData.error.exceptionName}")

// 将该分区的未转换数据进行转换

convertedData.put(tp, maybeConvertFetchedData(tp, unconvertedPartitionData))

}

// 9、构建一个新的 FetchResponse

val response \= new FetchResponse(unconvertedFetchResponse.error, convertedData, throttleTimeMs,

unconvertedFetchResponse.sessionId)

// 更新日志统计信息

response.responseData.forEach { (tp, data) =>

brokerTopicStats.updateBytesOut(tp.topic, fetchRequest.isFromFollower, reassigningPartitions.contains(tp), data.records.sizeInBytes)

}

response

}

// 10、更新记录数据转换统计信息的函数

def updateConversionStats(send: Send): Unit = {

send match {

case send: MultiRecordsSend if send.recordConversionStats != null =>

send.recordConversionStats.asScala.toMap.foreach {

case (tp, stats) => updateRecordConversionStats(request, tp, stats)

}

case \_ \=\>

}

}

// 如果 Fetch 请求来自 Follower 节点，则不需要流控，直接返回响应数据

if (fetchRequest.isFromFollower) {

unconvertedFetchResponse = fetchContext.updateAndGenerateResponseData(partitions)

val responseSize \= sizeOfThrottledPartitions(versionId, unconvertedFetchResponse, quotas.leader)

quotas.leader.record(responseSize)

trace(s"Sending Fetch response with partitions.size=${unconvertedFetchResponse.responseData.size}, " +

s"metadata=${unconvertedFetchResponse.sessionId}")

sendResponseExemptThrottle(request, createResponse(0), Some(updateConversionStats))

} else {

// 限流的最大字节数取决于请求的 maxBytes 和配额窗口内的最大字节数的最小值

val maxQuotaWindowBytes \= if (fetchRequest.isFromFollower)

Int.MaxValue

else

quotas.fetch.getMaxValueInQuotaWindow(request.session, clientId).toInt

//确定拉取消息的大小

val fetchMaxBytes \= Math.min(Math.min(fetchRequest.maxBytes, config.fetchMaxBytes), maxQuotaWindowBytes)

val fetchMinBytes \= Math.min(fetchRequest.minBytes, fetchMaxBytes)

if (interesting.isEmpty)

processResponseCallback(Seq.empty)

else {

// 调用 ReplicaManager 从本地的 Replica 副本中获取消息

replicaManager.fetchMessages(

fetchRequest.maxWait.toLong,

fetchRequest.replicaId,

fetchMinBytes,

fetchMaxBytes,

versionId <= 2,

// 规定了读取分区的信息，比如要读取哪些分区、从这些分区的哪个位移值开始读、最多可以读多少字节，等等

interesting,

// 配额控制类，读取消息过程是否需要流量控制

replicationQuota(fetchRequest),

// 消息拉取完成后的回调函数

processResponseCallback,

//事务消息

fetchRequest.isolationLevel,

// 客户端元数据

clientMetadata)

}

}

}

// 如果没有分区被关注，则无需进行处理

if (interesting.isEmpty)

processResponseCallback(Seq.empty)

else {

// 从 Fetch 请求中获取需要从哪些分区获取消息

// (topicPartition, partitionData) => topicPartition表示分区，partitionData表示请求参数

val fetchMetadata \= interesting.map(tp => tp -> new FetchRequest.PartitionData(fetchOffset(tp), fetchRequest.fetchSize(tp))).toMap

// 11、调用 ReplicaManager 从本地的 Replica 副本中获取消息

replicaManager.fetchMessages(

fetchRequest.maxWait.toLong,

fetchRequest.replicaId,

fetchRequest.minBytes,

fetchRequest.maxBytes,

versionId <= 2,

fetchMetadata,

replicationQuota(fetchRequest),

processResponseCallback,

fetchRequest.isolationLevel,

clientMetadata)

}

}

  
该方法源码比较长，中间省略了部分代码，主要**用对 Fetch 请求的各种情况的处理，最后调用 ReplicaManager#fetchMessages 来读取**，在整个流程中，最主要的是 「**第十一步**」，那么我们来看看此方法内部做了什么？

日志的读写操作是 Kafka 存储层最重要的内容，这里以 Server 端处理 Fetch 请求的过程为入口，一步步深入到底层的 Log 实例部分。与 Produce 请求不一样的地方是，对于 Fetch 请求，是有两种不同的来源：「**Consumer**」和 「**Follower**」，consumer 读取数据与副本同步数据都是通过向 Leader 发送 Fetch 请求来实现的，**在对这两种不同情况处理的过程中，其地产实现是统一的，只是实现方法的参数不同而已，下面会详细对这两种不同情况的处理进行说明**。

Consumers 和 Follower 副本，拉取消息的都是向 Broker发送FETCH请求，Broker端接收到该请求后，调用fetchMessages 方法从底层的 Leader 副本读取消息。

Fetch 请求（读请求）的处理与 Produce 请求（写请求）的整体流程非常类似，读和写由最上面的抽象层做入口，最终还是在存储层的 Log 对象实例进行真正的读写操作，在这一点上，Kafka 封装的非常清晰，这样的系统设计是非常值得学习的。

## **3.2 fetchMessages()**

「**ReplicaManager**」是 kafka 管理副本的组件，用来维护目标 broker 上各个 topic 的副本数据信息。

「**ReplicaManager**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/ReplicaManager.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/ReplicaManager.scala)

/\*\*

\* Broker 端接收到该请求后，调用 fetchMessages 方法从底层的 Leader 副本取出消息。

\* 需要等待足够的数据被获取后返回，回调函数将在超时或满足所需的拉取信息时触发。消费者可以从任何副本中拉取消息，但 follower 者只能从 Leader 拉取。

\* Fetch messages from a replica, and wait until enough data can be fetched and return;

\* the callback function will be triggered either when timeout or required fetch info is satisfied.

\* Consumers may fetch from any replica, but followers can only fetch from the leader.

\*/

def fetchMessages(timeout: Long, // 拉取处理超时时间，对于消费者而言，该值就是 request.timeout.ms 参数值。对于 Follower 副本而言，该值是 Broker 端参数 replica.fetch.wait.max.ms 的值。

replicaId: Int, // 副本ID。对于消费者而言，该参数值是-1；对于 Follower 副本而言，该值就是 Follower 副本所在的Broker ID。

fetchMinBytes: Int, // 能够拉取请求的最小字节数。对于消费者而言，对应于 Consumer 端参数 fetch.min.bytes 值；对于 Follower 副本而言，对应于 Broker 端参数replica.fetch.min.bytes 值。

fetchMaxBytes: Int, // 能够拉取请求的最大字节数。对于消费者而言，对应于 Consumer 端参数 fetch.max.bytes 值；对于 Follower 副本而言，对应于 Broker 端参数replica.fetch.max.bytes 值。

hardMaxBytesLimit: Boolean, // 是否严格限制 fetchMaxBytes。

fetchInfos: Seq\[(TopicPartition, PartitionData)\], // 拉取请求的详细信息（包括拉取哪些 Partition、从这些分区的哪个起始偏移量开始读、最大可以拉取多少字节数等）。

quota: ReplicaQuota, // 配额控制类，主要是为了判断是否需要在读取的过程中做限速控制

responseCallback: Seq\[(TopicPartition, FetchPartitionData)\] => Unit, // 回调逻辑函数，当请求被处理完成后，调用该方法执行收尾逻辑。

isolationLevel: IsolationLevel, // 用于设置消息读取的事务隔离级别。

clientMetadata: Option\[ClientMetadata\]): Unit = { // 用于判断消息拉取是基于消费端还是基于 Leader 端。

// 判断该读取请求是否来自于 Follower 副本或 Consumer

val isFromFollower \= Request.isValidBrokerId(replicaId)

val isFromConsumer \= !(isFromFollower || replicaId == Request.FutureLocalReplicaId)

// 1、fetchIsolation 即读取隔离级别，根据请求发送方判断可读取范围

val fetchIsolation \= if (!isFromConsumer)

FetchLogEnd // 如果请求来自于Follower副本，那么可以读到LEO值，即从分区最大的 Offset 开始拉取

else if (isolationLevel == IsolationLevel.READ\_COMMITTED)

FetchTxnCommitted // 如果请求是普通消费者,且来自于配置了READ\_COMMITTED的消费者，那么可以读到Log Stable Offset值，即从已提交的事务开始拉取

else

FetchHighWatermark // 其他消费者，那么可以读到高水位值前的消息

// Restrict fetching to leader if request is from follower or from a client with older version (no ClientMetadata)

// 如果是从节点或者消息客户端版本太老，就限制只能从 Leader 获取数据（fetchOnlyFromLeader），否则可以从任意节点拉取消息数据，读取的过程中会记录剩余配额和统计 Kafka 服务器的相关指标。

val fetchOnlyFromLeader \= isFromFollower || (isFromConsumer && clientMetadata.isEmpty)

// 2、定义读取本地日志中的消息的方法。

def readFromLog(): Seq\[(TopicPartition, LogReadResult)\] = {

// 从本地副本中读取指定 Partition 的消息数据

val result \= readFromLocalLog(

replicaId = replicaId,

fetchOnlyFromLeader = fetchOnlyFromLeader,

fetchIsolation = fetchIsolation,

fetchMaxBytes = fetchMaxBytes,

hardMaxBytesLimit = hardMaxBytesLimit,

readPartitionInfo = fetchInfos,

quota = quota,

clientMetadata = clientMetadata)

// 如果拉取方是 follower 副本节点，则可能需要更新分区的高水位

if (isFromFollower) updateFollowerFetchState(replicaId, result)

// 否则直接返回读取结果。

else result

}

// 3、调用读取本地日志的方法，并返回读取日志的结果。

// logReadResults 是 Seq\[(TopicPartition, LogReadResult)\]

val logReadResults \= readFromLog()

// 对读取结果进行一些统计，如统计不同 Topic 的拉取速度，记录已读取的字节数等。

// check if this fetch request can be satisfied right away

var bytesReadable: Long = 0

var errorReadingData \= false

var hasDivergingEpoch \= false

val logReadResultMap \= new mutable.HashMap\[TopicPartition, LogReadResult\]

logReadResults.foreach { case (topicPartition, logReadResult) =>

brokerTopicStats.topicStats(topicPartition.topic).totalFetchRequestRate.mark()

brokerTopicStats.allTopicsStats.totalFetchRequestRate.mark()

if (logReadResult.error != Errors.NONE)

errorReadingData = true

if (logReadResult.divergingEpoch.nonEmpty)

hasDivergingEpoch = true

bytesReadable \= bytesReadable + logReadResult.info.records.sizeInBytes

logReadResultMap.put(topicPartition, logReadResult)

}

// respond immediately if 1) fetch request does not want to wait

// 2) fetch request does not require any data

// 3) has enough data to respond

// 4) some error happens while reading data

// 5) we found a diverging epoch

// 4、根据读取日志的结果，判断是否要立即返回给客户端

// 如果符合以下任一条件：1）不需要等待即 fetch 请求未设置等待时间；2）fetch 请求内容为空；3）已经读取到的数据量达到了 fetchMinBytes 即读取数据量满足 fetch 请求 ；4）在读取数据时出现了错误；5）发现了新的 LeaderEpoch 时，那么就立即将拉取到的消息数据封装成 FetchPartitionData 对象，并回调 responseCallback 函数返回给消费方。否则，构建一个延迟拉取对象 delayedFetch，将拉取请求暂时放到等待队列中，等待符合上述任一条件时再将消息数据返回给消费方。

if (timeout <= 0 || fetchInfos.isEmpty || bytesReadable >= fetchMinBytes || errorReadingData || hasDivergingEpoch) {

val fetchPartitionData \= logReadResults.map { case (tp, result) =>

val isReassignmentFetch \= isFromFollower && isAddingReplica(tp, replicaId)

tp -> FetchPartitionData(

result.error,

result.highWatermark,

result.leaderLogStartOffset,

result.info.records,

result.divergingEpoch,

result.lastStableOffset,

result.info.abortedTransactions,

result.preferredReadReplica,

isReassignmentFetch)

}

// 执行回调

responseCallback(fetchPartitionData)

} else {

// 5、如果无法立即完成请求,就要延迟处理了

// construct the fetch results from the read results

// 构建 fetchPartitionStatus 数据结构，并将每个 Partition 的起始偏移量等信息和 PartitionData 数据结构一起打包（例如 topic，partition，maxBytes 等），存储在 SFetchMetadata 中。

val fetchPartitionStatus \= new mutable.ArrayBuffer\[(TopicPartition, FetchPartitionStatus)\]

fetchInfos.foreach { case (topicPartition, partitionData) =>

logReadResultMap.get(topicPartition).foreach(logReadResult => {

val logOffsetMetadata \= logReadResult.info.fetchOffsetMetadata

fetchPartitionStatus += (topicPartition -> FetchPartitionStatus(logOffsetMetadata, partitionData))

})

}

val fetchMetadata: SFetchMetadata = SFetchMetadata(fetchMinBytes, fetchMaxBytes, hardMaxBytesLimit,

fetchOnlyFromLeader, fetchIsolation, isFromFollower, replicaId, fetchPartitionStatus)

val delayedFetch \= new DelayedFetch(timeout, fetchMetadata, this, quota, clientMetadata,

responseCallback)

// 构建一组 Topic / Partition 键值对，用于在延迟拉取队列中暂存该拉取请求。如果此时还能够满足消费端的拉取请求，则会直接返回数据；否则，会将请求进一步延迟，直到能够获取到符合条件的消息数据后才会返回给消费端。

// create a list of (topic, partition) pairs to use as keys for this delayed fetch operation

val delayedFetchKeys \= fetchPartitionStatus.map { case (tp, \_) => TopicPartitionOperationKey(tp) }

// try to complete the request immediately, otherwise put it into the purgatory;

// this is because while the delayed fetch operation is being created, new requests

// may arrive and hence make this operation completable.

delayedFetchPurgatory.tryCompleteElseWatch(delayedFetch, delayedFetchKeys)

}

}

该方法主要用来**处理 fetch 请求的, 不管是消费者还是 Follower 副本，它们拉取消息都是需要向 Broker 发起拉取请求**。大体流程如下：

1.  获取消费者的读取位置上限，这里分三种情况：
2.  如果请求是 follower 副本消费者，则可以读取最新的写入消息，即 FetchLogEnd。
3.  如果是普通消费者，且事务隔离级别设置为 IsolationLevel.READ\_COMMITTED， 则消费者只能读取已提交事务的消息，即 FetchTxnCommitted。
4.  其他消费者可以读取到分区高水位前的消息，即 FetchHighWatermark。由于分区高水位是 ISR 中同步最落后的副本的 LEO，所以消费者不能读取分区高水位指向的消息，只能读取分区高水位之前的消息。

> 由于分区高水位是 ISR 中同步最落后的副本的 LEO，所以消费者不能读取分区高水位指向的消息，只能读取分区高水位前的消息。

1.  定义读取本地日志的方法。这里的 fetchInfos 是一个 Seq\[(TopicPartition, PartitionData)\] 实例，而 PartitionData 包括了该分区读取位置 fetchOffset，Broker 从 fetchOffset 位置开始读取消息。
2.  从本地副本中读取指定 Partition 的消息数据。
3.  如果拉取方是 follower 副本节点，则可能需要更新分区的高水位。
4.  否则直接返回读取结果。

> fetchInfos 参数是一个 Seq\[(TopicPartition, PartitionData)\] 实例，而 PartititonData 包括了该分区读取位置 fetchOffset，Broker 从fetchOffset 位置开始读取消息。

1.  调用读取本地日志的方法，并返回读取日志的结果。
2.  根据读取日志的结果，判断是否要立即返回给客户端，即调用 KafkaApis#responseCallback 回调方法。
3.  如果符合以下任一条件：1）不需要等待即 fetch 请求未设置等待时间；2）fetch 请求内容为空；3）已经读取到的数据量达到了 fetchMinBytes 即读取数据量满足 fetch 请求 ；4）在读取数据时出现了错误；5）发现了新的 LeaderEpoch 时，那么就立即将拉取到的消息数据封装成 FetchPartitionData 对象，并回调 responseCallback 函数返回给消费方。否则，构建一个延迟拉取对象 delayedFetch，将拉取请求暂时放到等待队列中，等待符合上述任一条件时再将消息数据返回给消费方。
4.  如果无法立即完成请求,就要延迟处理了。
5.  构建 fetchPartitionStatus 数据结构，并将每个 Partition 的起始偏移量等信息和 PartitionData 数据结构一起打包（例如 topic，partition，maxBytes 等），存储在 SFetchMetadata 中。
6.  构建一组 Topic / Partition 键值对，用于在延迟拉取队列中暂存该拉取请求。如果此时还能够满足消费端的拉取请求，则会直接返回数据；否则，会将请求进一步延迟，直到能够获取到符合条件的消息数据后才会返回给消费端。

接下来，我们看看消息是如何从本地 Leader 副本进行读取的？即「**第二步**」，那么我们来看看 ReplicaManager#readFromLocalLog 方法内部做了什么？

## **3.3 readFromLocalLog()**

/\*\*

\* Read from multiple topic partitions at the given offset up to maxSize bytes

\* 用给定分区的 offset，从多个主题分区读取消息，直到消息的大小满足设定的最大的值 maxSize。

\*/

def readFromLocalLog(replicaId: Int, // 要读取的replicaId。

fetchOnlyFromLeader: Boolean, // 是否只从 leader 分区读取。

fetchIsolation: FetchIsolation,// 获取隔离级别

fetchMaxBytes: Int,// 获取消息的大小

hardMaxBytesLimit: Boolean,

readPartitionInfo: Seq\[(TopicPartition, PartitionData)\], //读哪些分区，从哪个offset开始读取

quota: ReplicaQuota,

clientMetadata: Option\[ClientMetadata\]): Seq\[(TopicPartition, LogReadResult)\] = {

val traceEnabled = isTraceEnabled

// 真正负责读取的内部方法，每个分区的读取逻辑

/\*\*

\* read 方法，核心是调用 partition.readRecords 拉取消息

\* @param tp 拉取消息TopicPartition信息

\* @param fetchInfo 拉取消息的元数据信息（与tp相对应）

\* @param limitBytes 拉取消息的最大值

\* @param minOneMessage 对能否超过最大字节数做硬限制

\* @return

\*/

def read(tp: TopicPartition, fetchInfo: PartitionData, limitBytes: Int, minOneMessage: Boolean): LogReadResult = {

// 1、获取要获取消息的关键参数

val offset \= fetchInfo.fetchOffset // 读取的分区偏移量

val partitionFetchSize \= fetchInfo.maxBytes // 该次读取的最大字节数

val followerLogStartOffset \= fetchInfo.logStartOffset // follower 的起始偏移量 LSO

// limitBytes值：Math.min(Math.min(fetchRequest.maxBytes, config.fetchMaxBytes), maxQuotaWindowBytes)

val adjustedMaxBytes \= math.min(fetchInfo.maxBytes, limitBytes) // 最大字节数，和传入的最大字节数、该次读取的最大字节数取最小值

try {

if (traceEnabled) // 如果日志跟踪可用

trace(s"Fetching log segment for partition $tp, offset $offset, partition fetch size $partitionFetchSize, " +

s"remaining response limit $limitBytes" +

(if (minOneMessage) s", ignoring response/partition size limits" else "")) // 输出日志

// 2、根据 tp 获取 partation 信息即从 broker 的所有分区中找到与传入的分区相匹配的分区

val partition \= getPartitionOrException(tp)

val fetchTimeMs \= time.milliseconds // 当前时间的时间戳

// 3、判断哪个副本是首选副本。kafka 从 2.4 版本开始支持从读取效果最好的分区（也包括follower）读取消息，这样能提升读取效率。

val preferredReadReplica \= clientMetadata.flatMap(

metadata => findPreferredReadReplica(partition, metadata, replicaId, fetchInfo.fetchOffset, fetchTimeMs))

// 4、如果已经有首选副本了，则直接返回该副本的信息

if (preferredReadReplica.isDefined) {

replicaSelectorOpt.foreach { selector =>

debug(s"Replica selector ${selector.getClass.getSimpleName} returned preferred replica " + s"${preferredReadReplica.get} for $clientMetadata")

}

// 如果有首选副本，则直接返回信息

val offsetSnapshot \= partition.fetchOffsetSnapshot(fetchInfo.currentLeaderEpoch, fetchOnlyFromLeader = false)

LogReadResult(info = FetchDataInfo(LogOffsetMetadata.UnknownOffsetMetadata, MemoryRecords.EMPTY),

divergingEpoch = None,

highWatermark = offsetSnapshot.highWatermark.messageOffset,

leaderLogStartOffset = offsetSnapshot.logStartOffset,

leaderLogEndOffset = offsetSnapshot.logEndOffset.messageOffset,

followerLogStartOffset = followerLogStartOffset,

fetchTimeMs = -1L,

lastStableOffset = Some(offsetSnapshot.lastStableOffset.messageOffset),

preferredReadReplica = preferredReadReplica,

exception = None)

} else { // 5、如果没有首选副本，则从分区中读取本地消息数据。

val readInfo: LogReadInfo = partition.readRecords(

lastFetchedEpoch = fetchInfo.lastFetchedEpoch,

fetchOffset = fetchInfo.fetchOffset,

currentLeaderEpoch = fetchInfo.currentLeaderEpoch,

maxBytes = adjustedMaxBytes,

fetchIsolation = fetchIsolation,

fetchOnlyFromLeader = fetchOnlyFromLeader,

minOneMessage = minOneMessage)

// 根据分区的限制信息，修正读取到的数据

val fetchDataInfo \= if (shouldLeaderThrottle(quota, partition, replicaId)) {

// 如果分区达到了限制，直接返回空

FetchDataInfo(readInfo.fetchedData.fetchOffsetMetadata, MemoryRecords.EMPTY)

} else if (!hardMaxBytesLimit && readInfo.fetchedData.firstEntryIncomplete) {

// 如果读取的消息是不完整的，将其设为 empty

FetchDataInfo(readInfo.fetchedData.fetchOffsetMetadata, MemoryRecords.EMPTY)

} else {

readInfo.fetchedData

}

// 6、构建日志读取结构，返回读取结果

LogReadResult(info = fetchDataInfo,

divergingEpoch = readInfo.divergingEpoch,

highWatermark = readInfo.highWatermark,

leaderLogStartOffset = readInfo.logStartOffset,

leaderLogEndOffset = readInfo.logEndOffset,

followerLogStartOffset = followerLogStartOffset,

fetchTimeMs = fetchTimeMs,

lastStableOffset = Some(readInfo.lastStableOffset),

preferredReadReplica = preferredReadReplica,

exception = None)

}

} catch {

// 处理各种异常

// 一些异常是预期之内的，不应该计入“失败的抓取请求计数指标”

// 比如 UnknownTopicOrPartitionException、NotLeaderOrFollowerException、UnknownLeaderEpochException 等

case e@ (\_: UnknownTopicOrPartitionException |

\_: NotLeaderOrFollowerException |

\_: UnknownLeaderEpochException |

\_: FencedLeaderEpochException |

\_: ReplicaNotAvailableException |

\_: KafkaStorageException |

\_: OffsetOutOfRangeException) =>

// 处理异常时的行为，返回错误信息

LogReadResult(info = FetchDataInfo(LogOffsetMetadata.UnknownOffsetMetadata, MemoryRecords.EMPTY),

divergingEpoch = None,

highWatermark = Log.UnknownOffset,

leaderLogStartOffset = Log.UnknownOffset,

leaderLogEndOffset = Log.UnknownOffset,

followerLogStartOffset = Log.UnknownOffset,

fetchTimeMs = -1L,

lastStableOffset = None,

exception = Some(e))

case e: Throwable =>

brokerTopicStats.topicStats(tp.topic).failedFetchRequestRate.mark()

brokerTopicStats.allTopicsStats.failedFetchRequestRate.mark()

val fetchSource \= Request.describeReplicaId(replicaId)

error(s"Error processing fetch with max size $adjustedMaxBytes from $fetchSource " +

s"on partition $tp: $fetchInfo", e)

LogReadResult(info = FetchDataInfo(LogOffsetMetadata.UnknownOffsetMetadata, MemoryRecords.EMPTY),

divergingEpoch = None,

highWatermark = Log.UnknownOffset,

leaderLogStartOffset = Log.UnknownOffset,

leaderLogEndOffset = Log.UnknownOffset,

followerLogStartOffset = Log.UnknownOffset,

fetchTimeMs = -1L,

lastStableOffset = None,

exception = Some(e))

}

}

var limitBytes \= fetchMaxBytes // 设定当前的最大读取字节数

// topic-partation类型的数组

val result \= new mutable.ArrayBuffer\[(TopicPartition, LogReadResult)\]

var minOneMessage \= !hardMaxBytesLimit // 是否至少读取一个 message，初始化为 true

// 在待读取分区上循环调用其日志对象的 read 方法执行实际的消息读取

// readPartitionInfo 来自消费者，读取分区的信息

readPartitionInfo.foreach { case (tp, fetchInfo) =>

// 遍历 TopicPartition，并进行数据读取

val readResult \= read(tp, fetchInfo, limitBytes, minOneMessage) // 读取分区数据

val recordBatchSize \= readResult.info.records.sizeInBytes

if (recordBatchSize > 0)

minOneMessage = false // 如果分区返回了数据，则将 minOneMessage 设为 false，不再忽略限制

limitBytes = math.max(0, limitBytes - recordBatchSize)

result += (tp -> readResult)

}

result

}

该方法主要用来**在多个待读取分区上循环依次调用其日志对象的 read 方法执行实际的消息读取的操作**。大体流程如下：

1.  设定当前的最大读取字节数。
2.  定义 topic-partation 类型的数组，用来存储最终的读取结果。
3.  在待读取分区上循环调用其日志对象的 read 方法执行实际的消息读取。
4.  获取要获取消息的关键参数包括：读取的分区偏移量、该次读取的最大字节数、 follower 的起始偏移量 LSO。
5.  根据 tp 获取 partation 信息即从 broker 的所有分区中找到与传入的分区相匹配的分区。
6.  判断哪个副本是首选副本。kafka 从 2.4 版本开始支持从读取效果最好的分区（也包括follower）读取消息，这样能提升读取效率。
7.  如果已经有首选副本了，则直接返回该副本的信息。
8.  如果没有首选副本，则从分区中读取本地消息数据。
9.  构建日志读取结构，返回读取结果。
10.  最后处理各种异常。

这里需要注意的是**选择一个消费者首选副本的机制**：

在 Kafka 集群中会出现多机架的情况，即不同的副本可以存储在不同的机架上，如果此时消费者与 「**Leader 副本**」不在同一个机架上，那么消费者只能「**跨机架**」读取消息数据，网络传输的成本会非常高，尤其是云服务环境下。此时， Kafka 则允许消费者从同一个机架上的「**Follower 副本**」中读取消息数据，那么此时 ReplicaManager#readFromLocalLog 方法不会读取消息，而是会调用 ReplicaManager#findPerferredReadReplica 方法，进行选择一个最合适的副本，即「**首选副本**」，并将该副本信息返回给消费者，让消费者重新给该副本发起 Fetch 请求。

在上面整个流程中，最重要的是「**第三步**」，这里分两种情况，如果有「**首选副本**」，则直接返回该副本信息，即从快照进行读取。如果没有「**首选副本**」，则从分区中读取本地消息数据，我们主要来看看第二种情况，在待读取分区上循环调用底层 Partition 类的 readRecords 方法执行实际的消息读取。

## **3.4 readRecords()**

这是「**Partition**」组件类的相关方法，「**Partition**」组件是 topic 在某个 broker 上一个副本的抽象。**每个 Parititon 对象都会维护一个 Replica 对象，而 Replica 对象中又会维护 Log 对象，也就是日志数据目录的抽象**。

「**Partition**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/cluster/Partition.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/cluster/Partition.scala)

def readRecords(lastFetchedEpoch: Optional\[Integer\], // 上一次获取的 epoch

fetchOffset: Long, // 获取记录时的偏移量

currentLeaderEpoch: Optional\[Integer\], // 当前 leader epoch

maxBytes: Int, // 最大获取字节数

fetchIsolation: FetchIsolation, // 消息隔离级别（读取未提交消息或全部消息）

fetchOnlyFromLeader: Boolean, // 是否只从 leader 读取

minOneMessage: Boolean) // 是否至少获取一条消息

: LogReadInfo = inReadLock(leaderIsrUpdateLock) { // 这里通过读锁来读取

// decide whether to only fetch from leader

// 1、选择是否只从 leader 获取数据

val localLog \= localLogWithEpochOrException(currentLeaderEpoch, fetchOnlyFromLeader)

// Note we use the log end offset prior to the read. This ensures that any appends following

// the fetch do not prevent a follower from coming into sync.

// 初始化位移值，注意，我们在读取记录之前使用日志结束偏移量 LEO。这将确保随后的追加操作不会阻止追随者同步。

val initialHighWatermark \= localLog.highWatermark // 获取本地记录的 high watermark

val initialLogStartOffset \= localLog.logStartOffset // 获取本地记录的起始偏移量

val initialLogEndOffset \= localLog.logEndOffset // 获取本地记录的结束偏移量

val initialLastStableOffset \= localLog.lastStableOffset// 获取本地记录的最后稳定偏移量，这里主要用于事务消息

// 2、lastFetchedEpoch 表示上一次同步条目的 epoch，如果指定了 lastFetchedEpoch，则查询该 epoch 的最后一个偏移量

lastFetchedEpoch.ifPresent { fetchEpoch =>

// 获取该 epoch 的最后一个偏移量信息

val epochEndOffset \= lastOffsetForLeaderEpoch(currentLeaderEpoch, fetchEpoch, fetchOnlyFromLeader = false)

// 如果有错误，则抛出异常

if (epochEndOffset.error != Errors.NONE) {

throw epochEndOffset.error.exception()

}

// 如果最后一个偏移量未定义，则抛出 OffsetOutOfRangeException 异常

if (epochEndOffset.hasUndefinedEpochOrOffset) {

throw new OffsetOutOfRangeException("Could not determine the end offset of the last fetched epoch " +

s"$lastFetchedEpoch from the request")

}

// 如果返回的 leader epoch 小于 fetchEpoch 或者返回的偏移量小于 fetchOffset，则返回空记录

if (epochEndOffset.leaderEpoch < fetchEpoch || epochEndOffset.endOffset < fetchOffset) {

// 定义空记录数据结构

val emptyFetchData \= FetchDataInfo(

fetchOffsetMetadata = LogOffsetMetadata(fetchOffset),

records = MemoryRecords.EMPTY,

firstEntryIncomplete = false,

abortedTransactions = None

)

// 生成分界点信息

val divergingEpoch \= new FetchResponseData.EpochEndOffset()

.setEpoch(epochEndOffset.leaderEpoch)

.setEndOffset(epochEndOffset.endOffset)

// 返回 LogReadInfo，包含空记录和分界点信息等

return LogReadInfo(

fetchedData = emptyFetchData,

divergingEpoch = Some(divergingEpoch),

highWatermark = initialHighWatermark,

logStartOffset = initialLogStartOffset,

logEndOffset = initialLogEndOffset,

lastStableOffset = initialLastStableOffset)

}

}

// 3、读取 Log 日志消息

val fetchedData \= localLog.read(fetchOffset, maxBytes, fetchIsolation, minOneMessage)

// 4、返回 LogReadInfo，包含已读记录和位移值信息等

LogReadInfo(

fetchedData = fetchedData,

divergingEpoch = None,

highWatermark = initialHighWatermark,

logStartOffset = initialLogStartOffset,

logEndOffset = initialLogEndOffset,

lastStableOffset = initialLastStableOffset)

}

该方法相对也比较简单，主要用来**读取分区消息的**，大体流程如下：

1.  选择是否只从 leader 获取数据。
2.  lastFetchedEpoch 表示上一次同步条目的 epoch，如果指定了 lastFetchedEpoch，则查询该 epoch 的最后一个偏移量。
3.  先获取该 epoch 的最后一个偏移量信息。
4.  如果有错误，则抛出异常。
5.  如果最后一个偏移量未定义，则抛出 OffsetOutOfRangeException 异常。
6.  如果返回的 leader epoch 小于 fetchEpoch 或者返回的偏移量小于 fetchOffset，则返回空记录。
7.  读取 Log 日志消息。
8.  返回 LogReadInfo，包含已读记录和位移值信息等。

在上面整个流程中，最重要的是「**第三步**」，从本地日志中进行读取，也就是从「**Log**」日志中进行读取，我们来看下是如何进行读取的。

## **3.5 Log#read()**

这是「**Log**」组件类的相关方法，「**Log**」组件对象是对 「**Parititon**」数据目录的抽象。管理着某个 「**Topic**」在某个「**Broker**」的一个 「**Parititon**」,它可能是一个「**Leader**」，也可能是「**Replica**」。同时，「**Log**」组件对象还同时管理着多个 「**LogSegment**」即日志分段。

「**Log**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/Log.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/Log.scala)

/\*\*

\* Read messages from the log.

\* 先找到 segment（包含log index timeindex三个文件），再从segment里面拉取消息。

\* @param startOffset The offset to begin reading at

\* @param maxLength The maximum number of bytes to read

\* @param isolation The fetch isolation, which controls the maximum offset we are allowed to read

\* @param minOneMessage If this is true, the first message will be returned even if it exceeds \`maxLength\` (if one exists)

\* @throws OffsetOutOfRangeException If startOffset is beyond the log end offset or before the log start offset

\* @return The fetch data information including fetch starting offset metadata and messages read.

\*/

def read(startOffset: Long, // 从 Log 对象的哪个位移值开始读消息

maxLength: Int,// 最多能读取多少字节。

isolation: FetchIsolation,//设置读取隔离级别，主要控制能够读取的最大位移值，多用于 Kafka 事务

minOneMessage: Boolean): FetchDataInfo = { // 是否允许至少读一条消息。设想如果消息很大，超过了maxLength，正常情况下read方法永远不会返回任何消息。但如果设置了该参数为 true，read方法就保证至少能够返回一条消息。

maybeHandleIOException(s"Exception while reading from $topicPartition in dir ${dir.getParent}") {

trace(s"Reading maximum $maxLength bytes at offset $startOffset from log with " +

s"total length $size bytes")

val includeAbortedTxns \= isolation == FetchTxnCommitted

// Because we don't use the lock for reading, the synchronization is a little bit tricky.

// 这里读取消息时我们没有使用 Monitor 锁同步机制，用本地变量的方式把LEO对象保存起来，避免争用（race condition）

// 获取下一个偏移元数据信息

val endOffsetMetadata \= nextOffsetMetadata

// 获取下一个偏移量

val endOffset \= endOffsetMetadata.messageOffset

// 获取指定偏移量所对应的日志段，找到startOffset值所在的日志段对象。注意要使用floorEntry方法

var segmentEntry \= segments.floorEntry(startOffset)

// return error on attempt to read beyond the log end offset or read below log start offset

// 1、判断要读取的起始偏移量是否越界，满足以下条件之一将被视为消息越界，即你要读取的消息不在该Log对象中，则返回 OffsetOutOfRangeException 异常：

// 1. 要读取的消息位移超过了LEO值

// 2. 没找到对应的日志段对象

// 3. 要读取的消息在Log Start Offset之下，同样是对外不可见的消息

if (startOffset > endOffset || segmentEntry == null || startOffset < logStartOffset)

throw new OffsetOutOfRangeException(s"Received request for offset $startOffset for partition $topicPartition, " +

s"but we only have log segments in the range $logStartOffset to $endOffset.")

// 2、根据读取隔离级别设置，得出能读到最大offset的值，这里分三种情况。

val maxOffsetMetadata \= isolation match {

// 1.Follower副本消费者能够看到\[Log Start Offset，LEO)之间的消息

case FetchLogEnd \=\> endOffsetMetadata

// 2.普通消费者能够看到\[Log Start Offset, 高水位值)之间的消息

case FetchHighWatermark \=\> fetchHighWatermarkMetadata

// 3.事务型消费者只能看到\[Log Start Offset, Log Stable Offset\]之间的消息。Log Stable Offset(LSO)是比LEO值小的位移值，主要用在 Kafka事务消息。

case FetchTxnCommitted \=\> fetchLastStableOffsetMetadata

}

// 3、如果要读取的起始位置超过了能读取的最大偏移量位置，则返回空的消息集合，因为没法读取任何消息

if (startOffset == maxOffsetMetadata.messageOffset) {

return emptyFetchDataInfo(maxOffsetMetadata, includeAbortedTxns)

} else if (startOffset > maxOffsetMetadata.messageOffset) {

val startOffsetMetadata \= convertToOffsetMetadataOrThrow(startOffset)

return emptyFetchDataInfo(startOffsetMetadata, includeAbortedTxns)

}

// Do the read on the segment with a base offset less than the target offset

// but if that segment doesn't contain any messages with an offset greater than that

// continue to read from successive segments until we get some messages or we reach the end of the log

// 从以前的日志段中读取数据

// 如果日志段中没有任何消息的偏移量大于当前的偏移量，就继续在后续日志段中读取数据，

// 直到最后一次读取或者遇到日志末尾

// 4、开始遍历日志段对象，直到读出消息来或者读到日志末尾

while (segmentEntry != null) {

val segment \= segmentEntry.getValue

// 5、计算最大位置

// maxOffsetMetadata.relativePositionInSegment: 保存该位移值所在日志段的物理磁盘位置

val maxPosition \= {

// Use the max offset position if it is on this segment; otherwise, the segment size is the limit.

// 如果当前 segment 的 baseOffset 等于 maxOffsetMetadata 的最大值，则 maxPosition 就是日志段的物理磁盘位置

if (maxOffsetMetadata.segmentBaseOffset == segment.baseOffset) {

maxOffsetMetadata.relativePositionInSegment

} else {

// 否则，限制长度为当前日志段的大小

segment.size

}

}

// 6、调用日志段对象的 read 方法执行真正的读取消息操作

val fetchInfo \= segment.read(startOffset, maxLength, maxPosition, minOneMessage)

// 如果读取数据为空，则读取下一个日志段中的数据

if (fetchInfo == null) {

segmentEntry = segments.higherEntry(segmentEntry.getKey)

} else {

return if (includeAbortedTxns)

addAbortedTransactions(startOffset, segmentEntry, fetchInfo)

else

fetchInfo

}

}

// okay we are beyond the end of the last segment with no data fetched although the start offset is in range,

// this can happen when all messages with offset larger than start offsets have been deleted.

// In this case, we will return the empty set with log end offset metadata

// 7、读取数据为空，但指定的偏移量在日志的合法范围内，说明再次读取后已经读取到了日志的末尾，

// 此时如果没有数据被读取，说明所有偏移量都已被删除，因此返回空日志记录

FetchDataInfo(nextOffsetMetadata, MemoryRecords.EMPTY)

}

}

该方法相对也比较简单，主要用来**计算读取位置并找到 segment (包含 log、index、timeindex 三个文件)，然后再从 segment 日志段拉取消息**，大体流程如下：

1.  判断要读取的起始偏移量是否越界，满足以下条件之一将被视为消息越界，即你要读取的消息不在该Log对象中，则返回 OffsetOutOfRangeException 异常：
2.  要读取的消息位移超过了LEO值
3.  没找到对应的日志段对象
4.  要读取的消息在Log Start Offset之下，同样是对外不可见的消息
5.  根据读取隔离级别设置，得出能读到最大offset的值，这里分三种情况。
6.  Follower副本消费者能够看到\[Log Start Offset，LEO)之间的消息。
7.  普通消费者能够看到\[Log Start Offset, 高水位值)之间的消息。
8.  事务型消费者只能看到\[Log Start Offset, Log Stable Offset\]之间的消息。Log Stable Offset(LSO)是比LEO值小的位移值，主要用在 Kafka事务消息。
9.  如果要读取的起始位置超过了能读取的最大偏移量位置，则返回空的消息集合，因为没法读取任何消息
10.  开始遍历日志段对象，直到读出消息来或者读到日志末尾。
11.  计算最大位置。
12.  调用日志段对象的 read 方法执行真正的读取消息操作
13.  读取数据为空，但指定的偏移量在日志的合法范围内，说明再次读取后已经读取到了日志的末尾，此时如果没有数据被读取，说明所有偏移量都已被删除，因此返回空日志记录。

在上面整个流程中，最重要的是「**第六步**」，即调用日志段的方法进行读取，也就是从「**LogSegment**」日志中进行读取，我们来看下是如何进行读取的。

## **3.6 LogSegment.read()**

这是「**LogSegment**」组件类的相关方法，「**LogSegment**」组件对象是对 「**Parititon**」数据目录中数据段的抽象。kafka 会将一个副本中日志根据配置大小进行分段。这个 「**LogSegment**」对象维护数据文件以及索引文件的信息。

「**LogSegment**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogSegmen](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogSegment.scala)[t.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/Log.scala)

/\*\*

\* Read a message set from this segment beginning with the first offset >= startOffset. The message set will include

\* no more than maxSize bytes and will end before maxOffset if a maxOffset is specified.

\*

\* @param startOffset A lower bound on the first offset to include in the message set we read

\* @param maxSize The maximum number of bytes to include in the message set we read

\* @param maxPosition The maximum position in the log segment that should be exposed for read

\* @param minOneMessage If this is true, the first message will be returned even if it exceeds \`maxSize\` (if one exists)

\*

\* @return The fetched data and the offset metadata of the first message whose offset is >= startOffset,

\* or null if the startOffset is larger than the largest offset in this log

\*/

@threadsafe

def read(startOffset: Long, // 要读取的第一条消息的位移

maxSize: Int,// 能读取的最大字节数

maxPosition: Long = size,// 能读到的最大文件位置

minOneMessage: Boolean = false): FetchDataInfo = { // 是否允许在消息体过大时至少返回第一条消息

if (maxSize < 0)

throw new IllegalArgumentException(s"Invalid max size $maxSize for log read from segment $log")

// 1、根据索引信息找到对应的物理文件位置

val startOffsetAndSize \= translateOffset(startOffset)

// if the start position is already off the end of the log, return null

if (startOffsetAndSize == null)

return null

// 起始物理位置

val startPosition \= startOffsetAndSize.position

// 位移元数据

val offsetMetadata \= LogOffsetMetadata(startOffset, this.baseOffset, startPosition)

// 物理文件最大值和要查询的最大值，取最大的

val adjustedMaxSize \=

if (minOneMessage) math.max(maxSize, startOffsetAndSize.size)

else maxSize

// return a log segment but with zero size in the case below

// 如果指定的最大字节数为0，则返回一个空的日志记录

if (adjustedMaxSize == 0)

return FetchDataInfo(offsetMetadata, MemoryRecords.EMPTY)

// calculate the length of the message set to read based on whether or not they gave us a maxOffset

// 假设 maxSize=100，maxPosition=300，startPosition=250，那么 read 方法只能读取 50 字节，

// 因为 maxPosition - startPosition = 50。我们把它和 maxSize 参数相比较，其中的最小值就是最终能够读取的总字节数

val fetchSize: Int = min((maxPosition - startPosition).toInt, adjustedMaxSize)

// 2、调用 FileRecords 的 slice 方法，从指定位置读取指定大小的消息集合

FetchDataInfo(offsetMetadata, log.slice(startPosition, fetchSize),

firstEntryIncomplete = adjustedMaxSize < startOffsetAndSize.size)

}

该方法相对也比较简单，主要用来**计算索引对应的物理文件位置，并计算可读最大字节数，然后调用底层 FileRecords#slice 方法读取**，大体流程如下：

1.  根据索引信息找到对应的物理文件位置，并计算可读最大字节数。
2.  调用 FileRecords 的 slice 方法，从指定位置读取指定大小的消息集合。

这里最后并没有**读取消息内容，而是返回了 FetchDataInfo 实例，**FetchDataInfo 实例仅仅记录了找到的消息批次的「**物理文件**」、「**物理位置**」等。

{

"records" : {

"size": 98,

"file": "/tmp/kafka-logs/message3-1-0/00000000000000000000.log",

"start": 123,

"end" : 456

}

}

接下来，我们先来看看如何从索引文件中查找对应的物理文件位置，对应的方法为 **LogSegment#translateOffset()**。

## **3.7 LogSegment.translateOffset()**

/\*\*

\* Find the physical file position for the first message with offset >= the requested offset.

\*

\* The startingFilePosition argument is an optimization that can be used if we already know a valid starting position

\* in the file higher than the greatest-lower-bound from the index.

\*

\* @param offset The offset we want to translate

\* @param startingFilePosition A lower bound on the file position from which to begin the search. This is purely an optimization and

\* when omitted, the search will begin at the position in the offset index.

\* @return The position in the log storing the message with the least offset >= the requested offset and the size of the

\* message or null if no message meets this criteria.

\*/

@threadsafe

private\[log\] def translateOffset(offset: Long, startingFilePosition: Int = 0): LogOffsetPosition = {

// 在 .index 索引文件中，查找 relativeOffset 对应的 <relativeOffset,position>

val mapping \= offsetIndex.lookup(offset)

// 在 .log 日志文件中，查找对应的是 position 物理位置对应的消息，与 relativeOffset 进行匹配

log.searchForOffsetWithSize(offset, max(mapping.position, startingFilePosition))

}

**偏移量索引文件查找原理**，以查找offset为 268 的消息过程为例：

1.  首先在根据 segments.floorEntry(startOffset) 找到 baseOffset 为 230 的segment（小于等于230的最大值），计算relativeOffset : 13 = 230 - 217。
2.  在. index 索引文件，根据改进的二分查找算法，查找不大于 13 的最大索引项是 12，对应 position 是 456。
3.  在 .log 日志文件查找 position 对应的物理位置往后找，即找到 largeOffset = 13，对应 offset 是 230 的消息，然后从这个位置往后最终找到 offset = 268 的消息。

![](https://article-images.zsxq.com/ltA9pr0SZgN68CDQOln0b7PZ_yOW)

最后，我们来看看上面 **LogSegment#read()** 最后是从 FileRecords 内的指定位置读取指定大小的消息集合的，对应的方法为 **FileRecords#slice()**。

## **3.9 FileRecords.slice()**

/\*\*

\* Return a slice of records from this instance, which is a view into this set starting from the given position

\* and with the given size limit.

\*

\* If the size is beyond the end of the file, the end will be based on the size of the file at the time of the read.

\*

\* If this message set is already sliced, the position will be taken relative to that slicing.

\*

\* @param position The start position to begin the read from

\* @param size The number of bytes after the start position to include

\* @return A sliced wrapper on this message set limited based on the given position and size

\*/

public FileRecords slice(int position, int size) throws IOException {

// Cache current size in case concurrent write changes it

// 缓存当前文件记录大小以防并发写入改变它

int currentSizeInBytes \= sizeInBytes();

if (position < 0)

throw new IllegalArgumentException("Invalid position: " + position + " in read from " + this);

// position必须不小于0，且不能大于存储文件记录的大小。

if (position > currentSizeInBytes - start)

throw new IllegalArgumentException("Slice from position " + position + " exceeds end position of " + this);

if (size < 0)

throw new IllegalArgumentException("Invalid size: " + size + " in read from " + this);

// 如果end计算的长度超过了文件记录实际长度，则只设置到文件的末尾

int end \= this.start + position + size;

if (end < 0 || end > start + currentSizeInBytes)

end = start + currentSizeInBytes;

// 创建一个新的 FileRecords 对象，包括原文件路径，存储文件的 channel 通道，以及切分后文件记录的起始位置和终止位置

return new FileRecords(file, channel, this.start + position, end, true);

}

至此，我们将「**消息读取**」的全流程就剖析完了，下面通过一张图来从整体上来梳理下：

![](https://article-images.zsxq.com/lrmO8iXvE0VmJPtyitsyL1OiMGOj)

## **04 总结**

这里，我们一起来总结一下这篇文章的重点。

1、通过 KafkaApis 的入口 handle 方法引出了服务端日志存储的最上层 「**ReplicaManager**」对象。

2、带你深度剖析了 **Kafka 日志读写源码全流程**，从最上层的「**ReplicaManager**」一直剖析到最底层的 「**FileRecords**」 。

下篇我们来深度剖析「**LogManager 磁盘文件管理组件**」，大家期待，我们下期见。