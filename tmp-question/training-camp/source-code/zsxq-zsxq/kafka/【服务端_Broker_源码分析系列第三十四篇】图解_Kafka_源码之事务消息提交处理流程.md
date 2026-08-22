大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端****事务消息发送处理流程**」，了解了 Kafka 中「**事务消息**」的是如何发送到协调者，又是如何处理的，从今天开始，我们接着来深度剖析 Kafka「**Broker**」的底层源码实现，我们接着来深度聊聊「**Kafka 服务端事务消息提交处理流程**」，看看 Kafka 事务消息是如何提交事务的。

![](https://article-images.zsxq.com/FupE6Wuy45uuXeT8MojZGN2Gf6qz)

## **01 总体概述**

了解完了事务消息发送的过程后，如果忘记了可以点击[【服务端 Broker 源码分析系列第三十三篇】图解 Kafka 源码之事务消息发送处理流程](https://articles.zsxq.com/id_dj25ipks0726.html) 这篇进行学习。

你是否有这样问题：当完成发送事务消息后，最后又是如何提交事务的呢？

接下来，带着这个问题开启我们今天的话题。

## **02 事务实例**

Properties props \= new Properties();

props.put("bootstrap.servers", "localhost:9092");

props.put("transactional.id", "my-transactional-id");

Producer<String, String> producer = new KafkaProducer<>(props, new StringSerializer(), new StringSerializer());

producer.initTransactions();

try {

producer.beginTransaction();

for (int i \= 0; i < 100; i++)

producer.send(new ProducerRecord<>("my-topic", Integer.toString(i), Integer.toString(i)));

producer.commitTransaction();

} catch (ProducerFencedException | OutOfOrderSequenceException | AuthorizationException e) {

// We can't recover from these exceptions, so our only option is to close the producer and exit.

producer.close();

} catch (KafkaException e) {

// For all other exceptions, just abort the transaction and try again.

producer.abortTransaction();

}

producer.close();

可以看到，提交事务直接调用 [producer.commitTransaction();](http://producer.committransaction\(\);/)

本文涉及的源码：

「**KafkaProducer**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/clients/src/main/java/org/apache/kafka/clients/producer/KafkaProducer.java](https://github.com/apache/kafka/blob/2.8.0/clients/src/main/java/org/apache/kafka/clients/producer/KafkaProducer.java)

「**TransactionManager**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/clients/src/main/java/org/apache/kafka/clients/producer/internal/TransactionManager.java](https://github.com/apache/kafka/blob/2.8.0/clients/src/main/java/org/apache/kafka/clients/producer/internal/TransactionManager.java)

「**TransactionStateManager**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/coordinator/transaction/TransactionStateManager.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/coordinator/transaction/TransactionStateManager.scala)

「**TransactionCoordinator**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/coordinator/transaction/TransactionCoordinator.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/coordinator/transaction/TransactionCoordinator.scala)

「**TransactionMarkerChannelManager**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/coordinator/transaction/TransactionMarkerChannelManager](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/coordinator/transaction/TransactionMarkerChannelManager.scala)[.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/coordinator/transaction/TransactionMarkerChannelManager.scala)

「**ProducerStateManager**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/log/ProducerStateManager.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/log/ProducerStateManager.scala)

「**InterBrokerSendThread**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/common/InterBrokerSendThread.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/common/InterBrokerSendThread.scala)

## **02 生产者提交事务**

首先，会在生产者中调用 [producer.commitTransaction();](http://producer.committransaction\(\);/) 来负责提交事务，即 [KafkaProducer#commitTransaction](http://kafkaproducer/#commitTransaction) 方法。

public void commitTransaction() throws ProducerFencedException {

throwIfNoTransactionManager();

throwIfProducerClosed();

// 1、发送 EndTxn 请求给协调者，EndTxn 请求中携带了 committed 标志，表示提交事务或者回滚事务，这里只关注提交事务处理过程

TransactionalRequestResult result \= transactionManager.beginCommit();

// 2、唤醒 Sender 线程，将 EndTxn 请求发送给协调者。

sender.wakeup();

// 3、阻塞生产者，等待协调者返回 EndTxn 响应。

result.await(maxBlockTimeMs, TimeUnit.MILLISECONDS);

}

从源码来看，提交很简单，就三步：

1.  发送 EndTxn 请求给协调者，EndTxn 请求中携带了 committed 标志，表示提交事务或者回滚事务，这里只关注提交事务处理过程。
2.  唤醒 Sender 线程，将 EndTxn 请求发送给协调者。
3.  阻塞生产者，等待协调者返回 EndTxn 响应。

这样就可以把请求提交给「**协调者**」了。

## **03 协调者完成事务**

当事务消息提交出去后，「**协调者**」就会处理这些消息。

Broker 根据请求传递 API 来调用不同接口，[request.header.apiKey](http://request.header.apikey/) 匹配客户端传来的 [END\_TXN](http://end_txn/)。

case ApiKeys.END\_TXN => handleEndTxnRequest(request)

def handleEndTxnRequest(request: RequestChannel.Request): Unit = {

ensureInterBrokerVersion(KAFKA\_0\_11\_0\_IV0) // 确保中间件版本符合要求

val endTxnRequest \= request.body\[EndTxnRequest\]

val transactionalId \= endTxnRequest.data.transactionalId

if (authHelper.authorize(request.context, WRITE, TRANSACTIONAL\_ID, transactionalId)) {

// 如果权限验证通过

def sendResponseCallback(error: Errors): Unit = {

def createResponse(requestThrottleMs: Int): AbstractResponse = {

val finalError \=

if (endTxnRequest.version < 2 && error == Errors.PRODUCER\_FENCED) {

// 对于较旧的客户端，它们无法理解新的PRODUCER\_FENCED错误码，

// 因此我们需要返回INVALID\_PRODUCER\_EPOCH以保持相同的客户端处理逻辑。

Errors.INVALID\_PRODUCER\_EPOCH

} else {

error

}

val responseBody \= new EndTxnResponse(new EndTxnResponseData()

.setErrorCode(finalError.code)

.setThrottleTimeMs(requestThrottleMs))

trace(s"Completed ${endTxnRequest.data.transactionalId}'s EndTxnRequest " +

s"with committed: ${endTxnRequest.data.committed}, " +

s"errors: $error from client ${request.header.clientId}.")

responseBody

}

requestHelper.sendResponseMaybeThrottle(request, createResponse)

}

// 处理结束事务请求

txnCoordinator.handleEndTransaction(endTxnRequest.data.transactionalId,

endTxnRequest.data.producerId,

endTxnRequest.data.producerEpoch,

endTxnRequest.result(),

sendResponseCallback)

} else

requestHelper.sendResponseMaybeThrottle(request, requestThrottleMs =>

new EndTxnResponse(new EndTxnResponseData()

.setErrorCode(Errors.TRANSACTIONAL\_ID\_AUTHORIZATION\_FAILED.code)

.setThrottleTimeMs(requestThrottleMs))

)

}

def handleEndTransaction(transactionalId: String,

producerId: Long,

producerEpoch: Short,

txnMarkerResult: TransactionResult,

responseCallback: EndTxnCallback): Unit = {

// 可以看到最终会调用 endTransaction 来完成事务

endTransaction(transactionalId,

producerId,

producerEpoch,

txnMarkerResult,

isFromClient = true,

responseCallback)

}

接下来我们重点来剖析下 [endTransaction](http://endtransaction/) 这个方法究竟都做了哪些事情。

它是用来**处理 EndTxn 请求的**，如下：

private def endTransaction(transactionalId: String,

producerId: Long,

producerEpoch: Short,

txnMarkerResult: TransactionResult,

isFromClient: Boolean,

responseCallback: EndTxnCallback): Unit = {

var isEpochFence \= false

if ....

else {

// 1、处理正常情况下的逻辑，检查协调者事务状态当前是否可以切换到 PrepareCommit 状态。

val preAppendResult: ApiResult\[(Int, TxnTransitMetadata)\] = txnManager.getTransactionState(transactionalId).flatMap {

....

case Some(epochAndTxnMetadata) =>

val txnMetadata \= epochAndTxnMetadata.transactionMetadata

val coordinatorEpoch \= epochAndTxnMetadata.coordinatorEpoch

txnMetadata.inLock {

....

else txnMetadata.state match {

// 如果事务正在进行中

case Ongoing \=\>

// 判断结果是准备提交还是准备回滚

val nextState \= if (txnMarkerResult == TransactionResult.COMMIT)

PrepareCommit

else

PrepareAbort

....

Right(coordinatorEpoch, txnMetadata.prepareAbortOrCommit(nextState, time.milliseconds()))

case CompleteCommit \=\>

....

case CompleteAbort \=\>

....

case PrepareCommit \=\>

....

case PrepareAbort \=\>

....

case Empty \=\>

....

case Dead | PrepareEpochFence =>

....

}

}

}

// 匹配状态结果

preAppendResult match {

case Left(err) =>

....

case Right((coordinatorEpoch, newMetadata)) =>

// 2、如果是执行到这里了，表示协调者事务状态可以切换到 PrepareCommit 状态，先定义sendTxnMarkersCallback 回调函数

def sendTxnMarkersCallback(error: Errors): Unit = {

if (error == Errors.NONE) {

// 4、检查协调者事务状态当前是否可以切换到 CompleteCommit 状态

val preSendResult: ApiResult\[(TransactionMetadata, TxnTransitMetadata)\] = txnManager.getTransactionState(transactionalId).flatMap {

....

case Some(epochAndMetadata) =>

if (epochAndMetadata.coordinatorEpoch == coordinatorEpoch) {

val txnMetadata \= epochAndMetadata.transactionMetadata

txnMetadata.inLock {

if ....

else txnMetadata.state match {

....

case PrepareCommit \=\>

if (txnMarkerResult != TransactionResult.COMMIT)

logInvalidStateTransitionAndReturnError(transactionalId, txnMetadata.state, txnMarkerResult)

else

Right(txnMetadata, txnMetadata.prepareComplete(time.milliseconds()))

case PrepareAbort \=\>

if (txnMarkerResult != TransactionResult.ABORT)

logInvalidStateTransitionAndReturnError(transactionalId, txnMetadata.state, txnMarkerResult)

else

Right(txnMetadata, txnMetadata.prepareComplete(time.milliseconds()))

case Dead | PrepareEpochFence =>

....

}

}

} else {

....

}

}

preSendResult match {

case Left(err) =>

....

case Right((txnMetadata, newPreSendMetadata)) =>

// 5、如果执行到这里，表示事务可以正常完成了，则会调用 responseCallback 返回成功响应给客户端。生产者收到响应后就可以认为事务已经执行完成（但是还未完成，后续需要协调者来保证完成该事务）。

responseCallback(Errors.NONE)

// 6、接着调用 txnMarkerChannelManager#addTxnMarkersToSend 方法为该事务所有事务分区创建一个 TxnIdAndMarkerEntry 实例，它表示一个待发送的 WriteTxnMarkers 请求，目标节点为分区Leader 副本。然后添加到 transactionMarkerChannelManager#markersQueuePerBroker 中。

txnMarkerChannelManager.addTxnMarkersToSend(coordinatorEpoch, txnMarkerResult, txnMetadata, newPreSendMetadata)

}

} else {

....

}

}

// 3、将协调者事务状态切换到 PrepareCommit 状态，并将事务的 PrepareCommit 状态写入事务状态主题中，写入成功后会调用 sendTxnMarkersCallback 回调函数执行后续逻辑

txnManager.appendTransactionToLog(transactionalId, coordinatorEpoch, newMetadata, sendTxnMarkersCallback)

}

}

}

该方法源码比较长，这里只保留重点逻辑，步骤如下：

1.  处理正常情况下的逻辑，检查协调者事务状态当前是否可以切换到 [PrepareCommit](http://preparecommit%20/) 状态。
2.  匹配状态结果，如果是执行到这里了，表示协调者事务状态可以切换到 [PrepareCommit](http://preparecommit%20/) 状态，先定义[sendTxnMarkersCallback](http://sendtxnmarkerscallback%20/) 回调函数。
3.  将协调者事务状态切换到 [PrepareCommit](http://preparecommit%20/) 状态，并将事务的 [PrepareCommit](http://preparecommit%20/) 状态写入事务状态主题中，写入成功后会调用 [sendTxnMarkersCallback](http://sendtxnmarkerscallback%20/) 回调函数执行后续逻辑。
4.  检查协调者事务状态当前是否可以切换到 [CompleteCommit](http://completecommit%20/) 状态。
5.  如果执行到这里，表示事务可以正常完成了，则会调用 [responseCallback](http://responsecallback%20/) 返回成功响应给客户端。生产者收到响应后就可以认为事务已经执行完成（但是还未完成，后续需要协调者来保证完成该事务）。
6.  接着调用 [txnMarkerChannelManager#addTxnMarkersToSend](http://%20txnmarkerchannelmanager/#addTxnMarkersToSend) 方法为该事务所有事务分区创建一个 [TxnIdAndMarkerEntry](http://txnidandmarkerentry/) 实例，它表示一个待发送的 [WriteTxnMarkers](http://writetxnmarkers%20/) 请求，**目标节点为分区 Leader 副本**。然后添加到 [transactionMarkerChannelManager#markersQueuePerBroker](http://transactionmarkerchannelmanager/#markersQueuePerBroker) 中。

接着来剖析里面的关键方法，「**第一步**」、「**第三步**」、「**第六步**」。

先来剖析下 「**第一步**」，比较简单，源码如下：

// TransactionStateManager.scala 方法

def getTransactionState(transactionalId: String): Either\[Errors, Option\[CoordinatorEpochAndTxnMetadata\]\] = {

getAndMaybeAddTransactionState(transactionalId, None)

}

private def getAndMaybeAddTransactionState(transactionalId: String,

createdTxnMetadataOpt: Option\[TransactionMetadata\]): Either\[Errors, Option\[CoordinatorEpochAndTxnMetadata\]\] = {

// 在读锁中执行以下代码块

inReadLock(stateLock) {

// 获取分区ID

val partitionId \= partitionFor(transactionalId)

// 如果正在加载事务分区，则返回 COORDINATOR\_LOAD\_IN\_PROGRESS 错误

if (loadingPartitions.exists(\_.txnPartitionId == partitionId))

Left(Errors.COORDINATOR\_LOAD\_IN\_PROGRESS)

else {

transactionMetadataCache.get(partitionId) match {

case Some(cacheEntry) =>

// 检查缓存中是否已经存在该事务ID的元数据

val txnMetadata \= Option(cacheEntry.metadataPerTransactionalId.get(transactionalId)).orElse {

// 如果缓存中不存在，则使用 createdTxnMetadataOpt 创建新的事务元数据，并将其添加到缓存中

createdTxnMetadataOpt.map { createdTxnMetadata =>

Option(cacheEntry.metadataPerTransactionalId.putIfNotExists(transactionalId, createdTxnMetadata)).getOrElse(createdTxnMetadata)

}

}

// 返回事务的元数据和协调器的轮次（如果存在）

Right(txnMetadata.map(CoordinatorEpochAndTxnMetadata(cacheEntry.coordinatorEpoch, \_)))

case None \=\>

// 如果该分区不是协调器，则返回NOT\_COORDINATOR错误

Left(Errors.NOT\_COORDINATOR)

}

}

}

}

该方法主要**步用来获取事务状态的**，步骤如下:

1.  首先，使用事务ID计算出对应的分区ID。
2.  然后，检查是否正在加载该事务分区，如果是，则返回COORDINATOR\_LOAD\_IN\_PROGRESS错误。
3.  如果不是加载状态，则尝试从事务元数据缓存中获取与事务ID相关的元数据。
4.  如果缓存中已经存在该事务ID的元数据，则直接返回。
5.  如果缓存中不存在，则使用输入的 createdTxnMetadataOpt 创建新的事务元数据，并将其添加到缓存中。
6.  最后，返回事务元数据和协调器的轮次（如果存在）。如果该分区不是协调器，则返回NOT\_COORDINATOR错误。

「**第三步**」上一篇已经剖析过，点击 [【服务端 Broker 源码分析系列第三十三篇】图解 Kafka 源码之事务消息发送处理流程](https://articles.zsxq.com/id_dj25ipks0726.html) 进行学习。

接着来剖析「**第六步**」，源码如下：

  
![](https://article-images.zsxq.com/FgWKOT21wlMksnKWCxB8DlQSdAxt)

  
![](https://article-images.zsxq.com/Fu9Cu-2FVtUrDumDUj5fVbeV0O0n)

def addTxnMarkersToBrokerQueue(transactionalId: String,

producerId: Long,

producerEpoch: Short,

result: TransactionResult,

coordinatorEpoch: Int,

topicPartitions: immutable.Set\[TopicPartition\]): Unit = {

// 1、获取事务主题分区

val txnTopicPartition \= txnStateManager.partitionFor(transactionalId)

// 2、将主题分区按照目标节点分组

val partitionsByDestination: immutable.Map\[Option\[Node\], immutable.Set\[TopicPartition\]\] = topicPartitions.groupBy { topicPartition: TopicPartition =>

// 获取主题分区的 Leader 副本节点

metadataCache.getPartitionLeaderEndpoint(topicPartition.topic, topicPartition.partition, interBrokerListenerName)

}

// 3、遍历按目标节点分组后的主题分区集合

for ((broker: Option\[Node\], topicPartitions: immutable.Set\[TopicPartition\]) <- partitionsByDestination) {

broker match {

case Some(brokerNode) =>

// 创建事务标记对象

val marker \= new TxnMarkerEntry(producerId, producerEpoch, coordinatorEpoch, result, topicPartitions.toList.asJava)

val txnIdAndMarker \= TxnIdAndMarkerEntry(transactionalId, marker)

if (brokerNode == Node.noNode) {

// 如果分区的 Leader 节点已知但不可用，将事务标记添加到未知节点的队列中，等待发送线程查找可用节点并进行迁移

markersQueueForUnknownBroker.addMarkers(txnTopicPartition, txnIdAndMarker)

} else {

// 4、将事务标记添加到对应的节点的队列中

addMarkersForBroker(brokerNode, txnTopicPartition, txnIdAndMarker)

}

case None \=\>

txnStateManager.getTransactionState(transactionalId) match {

case Left(error) =>

// 获取事务元数据失败，取消向分区 Leader 副本节点发送事务标记

info(s"Encountered $error trying to fetch transaction metadata for $transactionalId with coordinator epoch $coordinatorEpoch; cancel sending markers to its partition leaders")

transactionsWithPendingMarkers.remove(transactionalId)

case Right(Some(epochAndMetadata)) =>

if (epochAndMetadata.coordinatorEpoch != coordinatorEpoch) {

// 当准备发送事务标记时，缓存的元数据已经发生变化，取消向分区 Leader 副本节点发送事务标记

info(s"The cached metadata has changed to $epochAndMetadata (old coordinator epoch is $coordinatorEpoch) since preparing to send markers; cancel sending markers to its partition leaders")

transactionsWithPendingMarkers.remove(transactionalId)

} else {

// 分区 Leader 副本节点未知，跳过发送事务标记，因为该分区很可能已被删除

info(s"Couldn't find leader endpoint for partitions $topicPartitions while trying to send transaction markers for " +

s"$transactionalId, these partitions are likely deleted already and hence can be skipped")

val txnMetadata \= epochAndMetadata.transactionMetadata

txnMetadata.inLock {

// 从事务元数据中移除对应的主题分区

topicPartitions.foreach(txnMetadata.removePartition)

}

maybeWriteTxnCompletion(transactionalId)

}

case Right(None) =>

// 协调器仍然拥有事务分区，但缓存中没有元数据，这是不正常的情况

val errorMsg \= s"The coordinator still owns the transaction partition for $transactionalId, but there is " +

s"no metadata in the cache; this is not expected"

fatal(errorMsg)

throw new IllegalStateException(errorMsg)

}

}

}

// 唤醒发送线程

wakeup()

}

该方法主要用来**将事务标记添加到队列中，以准备发送给分区 Leader 副本节点**。步骤如下：

1.  首先，根据事务ID计算出事务主题分区。
2.  将主题分区按照目标节点分组。
3.  遍历目标节点分组后的主题分区集合。
4.  对于已知的目标节点，创建事务标记并添加到对应节点的队列中。
5.  如果目标节点是未知的（Node.noNode），则将事务标记添加到未知节点的队列中等待后续的处理。
6.  如果没有目标节点，需要根据不同的情况进行处理：
7.  如果获取事务元数据失败，取消发送事务标记并记录日志。
8.  如果缓存的元数据已经发生了变化（协调器轮次不一致），取消发送事务标记并记录日志。
9.  如果目标节点未知，跳过发送事务标记，并从事务元数据中移除对应的主题分区。
10.  如果缓存中没有元数据，抛出异常。
11.  最后，唤醒发送线程以便处理事务标记。

在 [transactionMarkerChannelManager#markersQueuePerBroker](http://transactionmarkerchannelmanager/#markersQueuePerBroker) 中存储了每个 Broker 节点与待发送给该 Broker 的 [WriteTxnMarkers](http://writetxnmarkers/) 请求。

// 存储了每个 Broker 节点与待发送给该 Broker 的 WriteTxnMarkers 请求

private val markersQueuePerBroker: concurrent.Map\[Int, TxnMarkerQueue\] = new ConcurrentHashMap\[Int, TxnMarkerQueue\]().asScala

private\[transaction\] def addMarkersForBroker(broker: Node, txnTopicPartition: Int, txnIdAndMarker: TxnIdAndMarkerEntry): Unit = {

val brokerId \= broker.id

// 获取指定 broker 的事务标记队列，如果不存在则创建一个新的队列

val brokerRequestQueue \= CoreUtils.atomicGetOrUpdate(markersQueuePerBroker, brokerId, new TxnMarkerQueue(broker))

brokerRequestQueue.destination = broker

// 将事务标记添加到指定 broker 的队列中

brokerRequestQueue.addMarkers(txnTopicPartition, txnIdAndMarker)

// 记录日志，表示成功添加了事务标记

trace(s"Added marker ${txnIdAndMarker.txnMarkerEntry} for transactional id ${txnIdAndMarker.txnId} to destination broker $brokerId")

}

[TransactionMarkerChannelManager](http://transactionmarkerchannelmanager/) 类继承于 [InterBrokerSendThread](http://interbrokersendthread/)， 而 [InterBrokerSendThread](http://interbrokersendthread/) 线程是个抽象线程类，会不断的调用 [InterBrokerSendThread#pollOnce](http://interbrokersendthread/#pollOnce) [](http://pollonce%20/)方法处理网络请求。

protected def pollOnce(maxTimeoutMs: Long): Unit = {

try {

// 1、遍历生成的请求并将其添加到未发送请求队列中

drainGeneratedRequests()

var now \= time.milliseconds()

// 2、发送请求并设置超时时间

val timeout \= sendRequests(now, maxTimeoutMs)

// 3、网络客户端轮询

networkClient.poll(timeout, now)

now = time.milliseconds()

// 4、检查连接断开

checkDisconnects(now)

// 5、处理超时的请求

failExpiredRequests(now)

// 6、清除已发送和处理完成的请求

unsentRequests.clean()

} catch {

case \_: DisconnectException if !networkClient.active() =>

// NetworkClient#initiateClose被调用时，DisconnectException是预期的

case e: FatalExitError => throw e

case t: Throwable =>

error(s"unhandled exception caught in InterBrokerSendThread", t)

// 抛出未处理的异常，作为FatalExitError，使JVM终止运行

// 这是因为可能处于未知状态，可能有一些请求被丢弃且无法继续进行。

// 已知和预期的错误应该在先前已经处理。

throw new FatalExitError()

}

}

接下来剖析注释中的「**第一步**」、「**第二步**」、「**第四步**」、「**第五步**」。

// 第一步 遍历生成的请求并将其添加到待发送请求队列中

private def drainGeneratedRequests(): Unit = {

// 遍历生成的请求，并添加到待发送请求队列 unsentRequests 中

generateRequests().foreach { request =>

unsentRequests.put(request.destination, // 目标节点

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

// 第二步 发送请求并设置超时时间

private def sendRequests(now: Long, maxTimeoutMs: Long): Long = {

var pollTimeout \= maxTimeoutMs

// 遍历未发送请求队列

for (node <- unsentRequests.nodes.asScala) {

val requestIterator \= unsentRequests.requestIterator(node)

while (requestIterator.hasNext) {

val request \= requestIterator.next

if (networkClient.ready(node, now)) {

// 发送请求

networkClient.send(request, now)

// 移除请求

requestIterator.remove()

} else

// 更新最小的轮询超时时间

pollTimeout = Math.min(pollTimeout, networkClient.connectionDelay(node, now))

}

}

// 返回最小轮询超时时间

pollTimeout

}

// 第四步 检查连接断开

private def checkDisconnects(now: Long): Unit = {

// 检查连接断开，对于已经传输的请求，将由NetworkClient处理。

// 这里只需检查未发送请求中是否有连接断开的情况，

// 如果有，就完成相应的future，并在ClientResponse中设置断开标志。

val iterator \= unsentRequests.iterator()

while (iterator.hasNext) {

val entry \= iterator.next

val (node, requests) = (entry.getKey, entry.getValue)

// 连接失败进行移除

if (!requests.isEmpty && networkClient.connectionFailed(node)) {

iterator.remove()

for (request <- requests.asScala) {

val authenticationException \= networkClient.authenticationException(node)

if (authenticationException != null)

error(s"Failed to send the following request due to authentication error: $request")

// 请求超时或认证失败，完成相应的 future，并在 ClientResponse 中设置断开标志

completeWithDisconnect(request, now, authenticationException)

}

}

}

}

// 第五步 处理超时的请求

private def failExpiredRequests(now: Long): Unit = {

// 清除所有已超时的未发送请求

val timedOutRequests \= unsentRequests.removeAllTimedOut(now)

for (request <- timedOutRequests.asScala) {

debug(s"Failed to send the following request after ${request.requestTimeoutMs} ms: $request")

// 请求超时，完成相应的 future，并在 ClientResponse 中设置断开标志

completeWithDisconnect(request, now, null)

}

}

综上，我们来总结下 pollOnce 方法处理逻辑：

1.  首先，调用 [InterBrokerSendThread#drainGeneratedRequests](http://interbrokersendthread/#drainGeneratedRequests) 方法遍历 [TransactionMarkerChannelManage](http://transactionmarkerchannelmanager/)[r#markersQueuePerBroker](http://r/#markersQueuePerBroker) 队列，生成 WriteTxnMarkers 请求并存储在 [InterBrokerSendThread#](http://interbrokersendthread/#drainGeneratedRequests)[unsentRequests](http://unsentrequests/) 待发送请求队列中。
2.  接着调用 [sendRequests](http://sendrequests%20/) 方法发送 [InterBrokerSendThread#](http://interbrokersendthread/#drainGeneratedRequests)[unsentRequests](http://unsentrequests/) 待发送请求队列中的请求，将 [WriteTxnMarkers](http://writetxnmarkers%20/) 请求发送给对应的 Broker 节点。在 Broker 节点中，会调用 [KafkaApis#handleWriteTxnMarkersRequest](http://kafkaapis/#handleWriteTxnMarkersRequest) 方法来处理 [WriteTxnMarkers](http://writetxnmarkers%20/) 请求来提交事务消息，在上篇中已经剖析过了。
3.  接着调用 [NetworkClient#poll](http://networkclient/#poll) 方法来处理网络事件，而 [TransactionMarkerRequestCompletionHandler#onComlete](http://transactionmarkerrequestcompletionhandler/#onComlete) 方法负责处理 Broker 节点返回的 [WriteTxnMarkers](http://writetxnmarkers%20/) 响应。
4.  接着检查连接断开、处理超时的请求。
5.  最后清除已发送和处理完成的请求。

接着我们来剖析下是如何处理 Broker 节点返回的 [WriteTxnMarkers](http://writetxnmarkers/) 响应的。

class TransactionMarkerRequestCompletionHandler(brokerId: Int,

txnStateManager: TransactionStateManager,

txnMarkerChannelManager: TransactionMarkerChannelManager,

txnIdAndMarkerEntries: java.util.List\[TxnIdAndMarkerEntry\]) extends RequestCompletionHandler with Logging {

// 设置日志标识

this.logIdent = "\[Transaction Marker Request Completion Handler " + brokerId + "\]: "

override def onComplete(response: ClientResponse): Unit = {

val requestHeader \= response.requestHeader

val correlationId \= requestHeader.correlationId

// 如果响应中断开了连接

if (response.wasDisconnected) {

// 取消请求，并处理其关联的事务标记

trace(s"Cancelled request with header $requestHeader due to node ${response.destination} being disconnected")

for (txnIdAndMarker <- txnIdAndMarkerEntries.asScala) {

val transactionalId \= txnIdAndMarker.txnId

val txnMarker \= txnIdAndMarker.txnMarkerEntry

// 获取当前事务的状态

txnStateManager.getTransactionState(transactionalId) match {

case Left(Errors.NOT\_COORDINATOR) =>

// 当前节点不再是该事务的协调器，取消发送事务标记

info(s"I am no longer the coordinator for $transactionalId; cancel sending transaction markers $txnMarker to the brokers")

txnMarkerChannelManager.removeMarkersForTxnId(transactionalId)

case Left(Errors.COORDINATOR\_LOAD\_IN\_PROGRESS) =>

// 加载包含该事务的分区，取消发送事务标记

info(s"I am loading the transaction partition that contains $transactionalId which means the current markers have to be obsoleted; " +

s"cancel sending transaction markers $txnMarker to the brokers")

txnMarkerChannelManager.removeMarkersForTxnId(transactionalId)

case Left(unexpectedError) =>

// 处理其他未处理的错误

throw new IllegalStateException(s"Unhandled error $unexpectedError when fetching current transaction state")

case Right(None) =>

// 协调器仍然拥有该事务分区，但缓存中没有元数据，这是不应该发生的

throw new IllegalStateException(s"The coordinator still owns the transaction partition for $transactionalId, but there is " +

s"no metadata in the cache; this is not expected")

case Right(Some(epochAndMetadata)) =>

if (epochAndMetadata.coordinatorEpoch != txnMarker.coordinatorEpoch) {

// 协调器的epoch已经改变，取消发送事务标记

info(s"Transaction coordinator epoch for $transactionalId has changed from ${txnMarker.coordinatorEpoch} to " +

s"${epochAndMetadata.coordinatorEpoch}; cancel sending transaction markers $txnMarker to the brokers")

txnMarkerChannelManager.removeMarkersForTxnId(transactionalId)

} else {

// 使用可能有新目标代理的方式重新排队标记

trace(s"Re-enqueuing ${txnMarker.transactionResult} transaction markers for transactional id $transactionalId " +

s"under coordinator epoch ${txnMarker.coordinatorEpoch}")

txnMarkerChannelManager.addTxnMarkersToBrokerQueue(transactionalId,

txnMarker.producerId,

txnMarker.producerEpoch,

txnMarker.transactionResult,

txnMarker.coordinatorEpoch,

txnMarker.partitions.asScala.toSet)

}

}

}

} else {

// 接收到写入事务标记的响应

debug(s"Received WriteTxnMarker response $response from node ${response.destination} with correlation id $correlationId")

val writeTxnMarkerResponse \= response.responseBody.asInstanceOf\[WriteTxnMarkersResponse\]

val responseErrors \= writeTxnMarkerResponse.errorsByProducerId;

for (txnIdAndMarker <- txnIdAndMarkerEntries.asScala) {

val transactionalId \= txnIdAndMarker.txnId

val txnMarker \= txnIdAndMarker.txnMarkerEntry

val errors \= responseErrors.get(txnMarker.producerId)

if (errors == null)

throw new IllegalStateException(s"WriteTxnMarkerResponse does not contain expected error map for producer id ${txnMarker.producerId}")

txnStateManager.getTransactionState(transactionalId) match {

case Left(Errors.NOT\_COORDINATOR) =>

// 当前节点不再是该事务的协调器，取消发送事务标记

info(s"I am no longer the coordinator for $transactionalId; cancel sending transaction markers $txnMarker to the brokers")

txnMarkerChannelManager.removeMarkersForTxnId(transactionalId)

case Left(Errors.COORDINATOR\_LOAD\_IN\_PROGRESS) =>

// 加载包含该事务的分区，取消发送事务标记

info(s"I am loading the transaction partition that contains $transactionalId which means the current markers have to be obsoleted; " +

s"cancel sending transaction markers $txnMarker to the brokers")

txnMarkerChannelManager.removeMarkersForTxnId(transactionalId)

case Left(unexpectedError) =>

// 处理其他未处理的错误

throw new IllegalStateException(s"Unhandled error $unexpectedError when fetching current transaction state")

case Right(None) =>

// 协调器仍然拥有该事务分区，但缓存中没有元数据，这是不应该发生的

throw new IllegalStateException(s"The coordinator still owns the transaction partition for $transactionalId, but there is " +

s"no metadata in the cache; this is not expected")

case Right(Some(epochAndMetadata)) =>

val txnMetadata \= epochAndMetadata.transactionMetadata

val retryPartitions: mutable.Set\[TopicPartition\] = mutable.Set.empty\[TopicPartition\]

var abortSending: Boolean = false

if (epochAndMetadata.coordinatorEpoch != txnMarker.coordinatorEpoch) {

// 协调器的epoch已经改变，取消发送事务标记

info(s"Transaction coordinator epoch for $transactionalId has changed from ${txnMarker.coordinatorEpoch} to " +

s"${epochAndMetadata.coordinatorEpoch}; cancel sending transaction markers $txnMarker to the brokers")

txnMarkerChannelManager.removeMarkersForTxnId(transactionalId)

abortSending = true

} else {

txnMetadata.inLock {

for ((topicPartition, error) <- errors.asScala) {

error match {

case Errors.NONE =>

// 从事务元数据中移除该分区

txnMetadata.removePartition(topicPartition)

case Errors.CORRUPT\_MESSAGE |

Errors.MESSAGE\_TOO\_LARGE |

Errors.RECORD\_LIST\_TOO\_LARGE |

Errors.INVALID\_REQUIRED\_ACKS => // 这些都是意外的且致命的错误

throw new IllegalStateException(s"Received fatal error ${error.exceptionName} while sending txn marker for $transactionalId")

case Errors.UNKNOWN\_TOPIC\_OR\_PARTITION |

Errors.NOT\_LEADER\_OR\_FOLLOWER |

Errors.NOT\_ENOUGH\_REPLICAS |

Errors.NOT\_ENOUGH\_REPLICAS\_AFTER\_APPEND |

Errors.REQUEST\_TIMED\_OUT |

Errors.KAFKA\_STORAGE\_ERROR => // 这些是可重试的错误

info(s"Sending $transactionalId's transaction marker for partition $topicPartition has failed with error ${error.exceptionName}, retrying " +

s"with current coordinator epoch ${epochAndMetadata.coordinatorEpoch}")

retryPartitions += topicPartition

case Errors.INVALID\_PRODUCER\_EPOCH |

Errors.TRANSACTION\_COORDINATOR\_FENCED => // producer或coordinator的epoch已经改变，可以忽略该事务

info(s"Sending $transactionalId's transaction marker for partition $topicPartition has permanently failed with error ${error.exceptionName} " +

s"with the current coordinator epoch ${epochAndMetadata.coordinatorEpoch}; cancel sending any more transaction markers $txnMarker to the brokers")

txnMarkerChannelManager.removeMarkersForTxnId(transactionalId)

abortSending = true

} else {

txnMetadata.inLock {

for ((topicPartition, error) <- errors.asScala) {

error match {

case Errors.NONE =>

txnMetadata.removePartition(topicPartition)

case Errors.CORRUPT\_MESSAGE |

Errors.MESSAGE\_TOO\_LARGE |

Errors.RECORD\_LIST\_TOO\_LARGE |

Errors.INVALID\_REQUIRED\_ACKS => // these are all unexpected and fatal errors

throw new IllegalStateException(s"Received fatal error ${error.exceptionName} while sending txn marker for $transactionalId")

case Errors.UNKNOWN\_TOPIC\_OR\_PARTITION |

Errors.NOT\_LEADER\_OR\_FOLLOWER |

Errors.NOT\_ENOUGH\_REPLICAS |

Errors.NOT\_ENOUGH\_REPLICAS\_AFTER\_APPEND |

Errors.REQUEST\_TIMED\_OUT |

Errors.KAFKA\_STORAGE\_ERROR => // these are retriable errors

info(s"Sending $transactionalId's transaction marker for partition $topicPartition has failed with error ${error.exceptionName}, retrying " +

s"with current coordinator epoch ${epochAndMetadata.coordinatorEpoch}")

retryPartitions += topicPartition

case Errors.INVALID\_PRODUCER\_EPOCH |

Errors.TRANSACTION\_COORDINATOR\_FENCED => // producer or coordinator epoch has changed, this txn can now be ignored

info(s"Sending $transactionalId's transaction marker for partition $topicPartition has permanently failed with error ${error.exceptionName} " +

s"with the current coordinator epoch ${epochAndMetadata.coordinatorEpoch}; cancel sending any more transaction markers $txnMarker to the brokers")

txnMarkerChannelManager.removeMarkersForTxnId(transactionalId)

abortSending = true

case Errors.UNSUPPORTED\_FOR\_MESSAGE\_FORMAT |

Errors.UNSUPPORTED\_VERSION =>

// The producer would have failed to send data to the failed topic so we can safely remove the partition

// from the set waiting for markers

info(s"Sending $transactionalId's transaction marker from partition $topicPartition has failed with " +

s" ${error.name}. This partition will be removed from the set of partitions" +

s" waiting for completion")

txnMetadata.removePartition(topicPartition)

case other \=\>

throw new IllegalStateException(s"Unexpected error ${other.exceptionName} while sending txn marker for $transactionalId")

}

}

}

}

if (!abortSending) {

if (retryPartitions.nonEmpty) {

debug(s"Re-enqueuing ${txnMarker.transactionResult} transaction markers for transactional id $transactionalId " +

s"under coordinator epoch ${txnMarker.coordinatorEpoch}")

// re-enqueue with possible new leaders of the partitions

txnMarkerChannelManager.addTxnMarkersToBrokerQueue(

transactionalId,

txnMarker.producerId,

txnMarker.producerEpoch,

txnMarker.transactionResult,

txnMarker.coordinatorEpoch,

retryPartitions.toSet)

} else {

txnMarkerChannelManager.maybeWriteTxnCompletion(transactionalId)

}

}

}

}

}

}

这里简单说下执行逻辑：

1.  如果 Broker 返回处理成功的响应，则执行以下逻辑：
2.  调用 [TransactionMetadata#removePartition](http://transactionmetadata/#removePartition) 方法将处理成功的事务分区从该事务元数据 [TransactionMetadata#topicP](http://transactionmetadata/#removePartition)[artitions](http://artitions/) 中移除。
3.  调用 [TransactionMarkerChannelManager#maybeWriteTxnCompletion](http://transactionmetadata/#removePartition) 方法，如果该方法发现事务元数据 [TransactionMetadata#topi](http://transactionmetadata/#removePartition)[cPartitions](http://cpartitions/) 中的事务分区已清空，则说明事务已完成，这时将协调者事务主题切换到 CompleteCommit 状态，并将该状态写入事务状态主题，该事务提交完成。
4.  如果 Broker 返回处理失败的响应，则协调者重新生成 TxnIdAndMarkerEntry 实例并添加到[TransactionMarkerChannelManager#ma](http://transactionmetadata/#removePartition)[rkersQueuePerBroker](http://rkersqueueperbroker/) 队列中，后续协调者会重新发送 [WriteTxnMarkers](http://writetxnmarkers/) 请到该 Broker，直到事务中所有的 Broker 都处理成功，该事务才能提交完成。

## **04 ACK 偏移量提交处理流程**

当「**协调者**」收到「**生产者**」发送过来的 [AddOffsetsToTxn](http://addoffsetstotxn/) 请求，会生产一个偏移量主题分区，分区下标即消费组偏移量存储分区的下标，该分区的 Leader 副本就是「**消费组协调者**」，所以当调用 [TransactionCoordinator#endTransaction](http://transactioncoordinator/#endTransaction) 方法处理 EndTxn 请求时，也会给「**消费组协调者**」发送[WriteTxnMarkers](http://writetxnmarkers/) 请求，此时要求「**消费组协调者**」来提交 ACK 偏移量。

当「**消费组协调者**」收到该请求时，处理流程跟上一篇的第四部分的剖析流程一致，但最hi偶「**消费组协调者**」会调用 [GroupCoordinator#sheduleHandleTxnCompletion](http://groupcoordinator/#sheduleHandleTxnCompletion) 方法来提交事务中的 ACK 偏移量。

关于「**消费组协调者**」我们会在后面消费者源码部分进行剖析，这里了解即可。

## **05 事务回退机制**

最后我们来说说事务回退机制，在 LogSegment#txnIndex 中存储了所有回退事务的消息，其中包括「**事务 firstOffset**」、「**事务 lastOffset**」、「**事务 producerId**」。

  
![](https://article-images.zsxq.com/Fpl7gSt4AZ2R2VJOmaPU-B7dTfn3)

当 [Log#read](http://log/#read) 读取消息时，会调用 [Log#addAbortedTransaction](http://log/#addAbortedTransaction) 方法将读取的消息对应范围的回退事务新返回给消费者。

![](https://article-images.zsxq.com/FhOGpLhRBfXS7X6tU6R-Kc28Q8TL)

/ 添加未提交的事务到 FetchDataInfo 中

private def addAbortedTransactions(

startOffset: Long, // 当前拉取的起始偏移量

segmentEntry: JEntry\[JLong, LogSegment\], // 当前段的JEntry对象，包含段的元数据和实例对象

fetchInfo: FetchDataInfo): FetchDataInfo = { // 当前拉取的数据信息

// 获取当前拉取的消息大小

val fetchSize \= fetchInfo.records.sizeInBytes

// 获取拉取的起始偏移量的位置信息

val startOffsetPosition \= OffsetPosition(fetchInfo.fetchOffsetMetadata.messageOffset,

fetchInfo.fetchOffsetMetadata.relativePositionInSegment)

// 计算拉取数据的上界偏移量，不得超过段的末尾。如果超过了末尾，则使用下一个段的基准偏移量作为拉取的上界偏移量

val upperBoundOffset \= segmentEntry.getValue.fetchUpperBoundOffset(startOffsetPosition, fetchSize).getOrElse {

val nextSegmentEntry \= segments.higherEntry(segmentEntry.getKey)

if (nextSegmentEntry != null)

nextSegmentEntry.getValue.baseOffset

else

logEndOffset

}

// 创建一个 ListBuffer，用来存储未提交的事务

val abortedTransactions \= ListBuffer.empty\[AbortedTransaction\]

// 定义一个累加器函数，将 AbortedTxn 转换为 AbortedTransaction 并添加到 abortedTransactions 中

def accumulator(abortedTxns: List\[AbortedTxn\]): Unit = abortedTransactions ++= abortedTxns.map(\_.asAbortedTransaction)

// 收集起始偏移量和上界偏移量之间的未提交事务

collectAbortedTransactions(startOffset, upperBoundOffset, segmentEntry, accumulator)

// 构造并返回包含未提交事务的FetchDataInfo对象

FetchDataInfo(fetchOffsetMetadata = fetchInfo.fetchOffsetMetadata,

records = fetchInfo.records,

firstEntryIncomplete = fetchInfo.firstEntryIncomplete,

abortedTransactions = Some(abortedTransactions.toList))

}

最后当「**消费者**」收到消息后，会根据这些回滚事务信息过滤被回滚的事务消息。

到这里你是否会有疑问：**为什么要将回滚事务信息返回给消费者进行过滤，而不是在 Broker 端进行过滤呢？**

不知道你是否还记得 Kafka 服务端的高性能机制之 sendfile，并不会将数据复制到 Kafka 服务端的内存中，所以 Broker 端无法对消息进行过滤，只能将过滤操作转移到消费者端中处理。

另外，Kafka 会在每个日志段中生成一个 「**已回滚事务索引文件**」，文件名为：[{baseOffset}.txnindex](http://{baseoffset}.txnindex/)。这么做的目的是为了**避免重启 Broker 导致 LogSegment#txnIndex 内数据丢失**，Broker 会将该属性数据存储到「**已回滚事务索引文件**」中。

至此整个「**事务消息**」的提交处理流程就带你剖析完成。

##   
**06 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头引出了「**事务提交**」中遇到的问题。

2、接着带大家剖析了「**事务消息提交处理流程**」的方方面面，首先「**生产者提交事务**」、接着「**协调者完成事务**」、接着「**ACK 偏移量提交处理流程**」、最后剖析了「**事务回滚机制**」。

下篇我们来深度剖析「**Kraft 模块初探**」，大家期待，我们下期见。