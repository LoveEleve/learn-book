大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端****事务设计以及初始化流程**」，了解了 Kafka 中「**事务消息**」的实现机制以及「**事务消息**」是如何初始化的，从今天开始，我们接着来深度剖析 Kafka「**Broker**」的底层源码实现，我们接着来深度聊聊「**Kafka 服务端事务消息发送处理流程**」，看看 Kafka 事务消息是如何处理的。

![](https://article-images.zsxq.com/FopHh8oruMyajaSXNg51VGZ4PoBR)

## **01 总体概述**

了解完了事务初始化的过程后，如果忘记了可以点击 [【服务端 Broker 源码分析系列第三十二篇】图解 Kafka 源码之事务设计以及初始化流程](https://articles.zsxq.com/id_7rg312ke2fl7.html) 这篇进行学习。

你是否有以下问题：

1.  事务消息是如何发送到事务协调者的呢？
2.  Broker 端协调者是如何处理事务消息的呢？
3.  处理完成后是如何处理 ACK 偏移量的呢？

接下来，带着这些问题开启我们今天的话题。

## **02 事务消息发送与处理流程**

我们先来剖析事务消息发送与处理流程，看看「**生产者**」是如何发送事务消息以及 Broker 端「**协调者**」是如何处理这些事务消息的。

在「**生产者**」发送消息到 Broker 端前，需要将消息的「**分区**」发送给「**协调者**」，「**协调者**」会将这些事务分区消息存储到 Kafka 内部主题之事务状态主题「**\_\_transaction\_state**」中。

本文涉及的源码：

「**KafkaProducer**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/clients/src/main/java/org/apache/kafka/clients/producer/KafkaProducer.java](https://github.com/apache/kafka/blob/2.8.0/clients/src/main/java/org/apache/kafka/clients/producer/KafkaProducer.java)

「**TransactionManager**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/clients/src/main/java/org/apache/kafka/clients/producer/internal/TransactionManager.java](https://github.com/apache/kafka/blob/2.8.0/clients/src/main/java/org/apache/kafka/clients/producer/internal/TransactionManager.java)

「**TransactionStateManager**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/coordinator/transaction/TransactionStateManager.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/coordinator/transaction/TransactionStateManager.scala)

「**ProducerStateManager**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/log/ProducerStateManager.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/log/ProducerStateManager.scala)

## **2.1 生产者发送事务分区信息**

当启动事务后，会调用 [KafkaProducer#doSend](http://kafkaproducer/#send) 方法进行发送消息。

private Future<RecordMetadata> doSend(ProducerRecord<K, V> record, Callback callback) {

TopicPartition tp \= null;

try {

....

// 获取主题对应的分区

int partition \= partition(record, serializedKey, serializedValue, cluster);

tp = new TopicPartition(record.topic(), partition);

....

// 将分区信息存储到 transactionManager#newParitionsInTransaction 中

if (transactionManager != null && transactionManager.isTransactional())

transactionManager.maybeAddPartitionToTransaction(tp);

// 如果 batch 已经满了， 则唤醒 sender 线程发送数据

if (result.batchIsFull || result.newBatchCreated) {

log.trace("Waking up the sender since topic {} partition {} is either full or getting a new batch", record.topic(), partition);

// 唤醒 Sender 线程

this.sender.wakeup();

}

return result.future;

} catch (ApiException e) {

....

}

....

}

可以看到，发送方法会先获取主题的分区信息，然后调用 [transactionManager#maybeAddPartitionToTransaction](http://transactionmanager/#maybeAddPartitionToTransaction) 方法将分区信息存储到 [transactionManager#newParitionsInTransaction](http://transactionmanager/#newParitionsInTransaction) 中，如下：

![](https://article-images.zsxq.com/Fv65H58VhNP2X72xU7wUYpDBB5BQ)

当分区被存储到 [transactionManager#newParitionsInTransaction](http://transactionmanager/#newParitionsInTransaction) 中以后，此时**不知道你是否还记得在上篇中我们剖析过的** [transactionManager#nextRequest](http://transactionmanager/#nextRequest) **作用是什么呢？其实它就是负责获取待处理请求队列中的下一个待处理的 TxnRequestHandler**。

那么它们是如何被关联起来的呢？我们继续来剖析。

当「**消息批次**」满了以后会唤醒 sender 线程发送数据，Sender 线程实现了 Runnable 接口，会不断的调用 runOnce()，这是一个典型的循环事件机制。

![](https://article-images.zsxq.com/FtcaEVfqJnfCA-wp_m-fR3I8HCvO)

在 runOnce 方法中会处理事务消息，如果事务消息满足发送条件，则会调用[maybeSendAndPollTransactionalRequest](http://maybesendandpolltransactionalrequest%20/) 方法来尝试发送事务请求并处理响应，如果有请求在等待发送或正在处理，则返回。如下：

  
![](https://article-images.zsxq.com/FgyOOxzb02GgdcPRiRESOVvFLoRD)

完整源码如下：

private boolean maybeSendAndPollTransactionalRequest() {

// 1、如果有未完成的事务请求正在进行中，则等待它们返回

if (transactionManager.hasInFlightRequest()) {

client.poll(retryBackoffMs, time.milliseconds());

return true;

}

// 2、如果遇到可中止的错误或正在进行事务终止操作

if (transactionManager.hasAbortableError() || transactionManager.isAborting()) {

if (accumulator.hasIncomplete()) {

// 获取触发事务终止的最后一个错误

RuntimeException exception \= transactionManager.lastError();

// 如果没有错误发生，但仍然在进行事务终止操作，则异常默认为TransactionAbortedException

if (exception == null) {

exception = new TransactionAbortedException();

}

accumulator.abortUndrainedBatches(exception);

}

}

// 3、如果事务正在完成并且没有正在进行的flush操作

if (transactionManager.isCompleting() && !accumulator.flushInProgress()) {

// 可能还有重试的请求，在我们不知道它们是否已经成功附加到broker日志的情况下，必须重新发送，直到它们的最终状态清楚。

// 如果它们已经附加并且我们没有收到错误，则我们的序列号将不再正确，这将导致OutOfSequenceException。

accumulator.beginFlush();

}

// 4、获取下一个要发送的事务请求处理器

TransactionManager.TxnRequestHandler nextRequestHandler \= transactionManager.nextRequest(accumulator.hasIncomplete());

if (nextRequestHandler == null)

return false;

// 5、构建请求

AbstractRequest.Builder<?> requestBuilder = nextRequestHandler.requestBuilder();

Node targetNode \= null;

try {

// 查找事务协调者

FindCoordinatorRequest.CoordinatorType coordinatorType \= nextRequestHandler.coordinatorType();

targetNode = coordinatorType != null ?

transactionManager.coordinator(coordinatorType) :

client.leastLoadedNode(time.milliseconds());

if (targetNode != null) {

// 如果目标节点不可用，则等待

if (!awaitNodeReady(targetNode, coordinatorType)) {

log.trace("Target node {} not ready within request timeout, will retry when node is ready.", targetNode);

maybeFindCoordinatorAndRetry(nextRequestHandler);

return true;

}

} else if (coordinatorType != null) {

// 如果没有找到协调者，则重新尝试查找协调者

log.trace("Coordinator not known for {}, will retry {} after finding coordinator.", coordinatorType, requestBuilder.apiKey());

maybeFindCoordinatorAndRetry(nextRequestHandler);

return true;

} else {

// 如果没有可用的节点，则重新尝试

log.trace("No nodes available to send requests, will poll and retry when until a node is ready.");

transactionManager.retry(nextRequestHandler);

client.poll(retryBackoffMs, time.milliseconds());

return true;

}

// 如果是重试请求，则进行休眠

if (nextRequestHandler.isRetry())

time.sleep(nextRequestHandler.retryBackoffMs());

long currentTimeMs \= time.milliseconds();

// 创建ClientRequest

ClientRequest clientRequest \= client.newClientRequest(targetNode.idString(), requestBuilder, currentTimeMs,

true, requestTimeoutMs, nextRequestHandler);

log.debug("Sending transactional request {} to node {} with correlation ID {}", requestBuilder, targetNode, clientRequest.correlationId());

// 发送请求并等待响应

client.send(clientRequest, currentTimeMs);

transactionManager.setInFlightCorrelationId(clientRequest.correlationId());

client.poll(retryBackoffMs, time.milliseconds());

return true;

} catch (IOException e) {

log.debug("Disconnect from {} while trying to send request {}. Going " +

"to back off and retry.", targetNode, requestBuilder, e);

// 当遇到IOException时，进行重试

maybeFindCoordinatorAndRetry(nextRequestHandler);

return true;

}

}

步骤如下：

1.  如果有未完成的事务请求正在进行中，则等待它们返回。
2.  如果遇到可中止的错误或正在进行事务终止操作。
3.  如果事务正在完成并且没有正在进行的flush操作。
4.  获取下一个要发送的事务请求处理器。
5.  构建请求并获取目标节点。
6.  最后创建 [ClientRequest](http://clientrequest%20/) 并发送请求。

从上步骤可以看出「**第四步**」是用来获取**待处理请求队列中的下一个待处理的 TxnRequestHandler**，源码如下：

synchronized TxnRequestHandler nextRequest(boolean hasIncompleteBatches) {

// 如果有新的分区需要添加到事务中，则先将添加分区的请求加入请求队列中

if (!newPartitionsInTransaction.isEmpty())

enqueueRequest(addPartitionsToTransactionHandler());

// 获取下一个要处理的请求处理器

TxnRequestHandler nextRequestHandler \= pendingRequests.peek();

if (nextRequestHandler == null)

return null;

// 如果下一个请求是EndTxn请求，并且存在未完成的批次，则暂时不发送EndTxn请求

if (nextRequestHandler.isEndTxn() && hasIncompleteBatches)

return null;

// 从等待处理的请求队列中移除下一个请求处理器

pendingRequests.poll();

// 如果下一个请求处理器在处理过程中发生了错误，则不发送该请求，返回null

if (maybeTerminateRequestWithError(nextRequestHandler)) {

log.trace("Not sending transactional request {} because we are in an error state",

nextRequestHandler.requestBuilder());

return null;

}

// 如果下一个请求是EndTxn请求，并且事务没有开始，则完成EndTxn请求并处理事务的完成

if (nextRequestHandler.isEndTxn() && !transactionStarted) {

nextRequestHandler.result.done();

if (currentState != State.FATAL\_ERROR) {

log.debug("Not sending EndTxn for completed transaction since no partitions " +

"or offsets were successfully added");

completeTransaction();

}

nextRequestHandler = pendingRequests.poll();

}

// 打印下一个要发送的请求日志

if (nextRequestHandler != null)

log.trace("Request {} dequeued for sending", nextRequestHandler.requestBuilder());

return nextRequestHandler;

}

步骤如下：

1.  首先该方法会先判断待发送分区集合中是否存在数据，如果不为空则创建一个 [addPartitionsToTransactionHandler](http://addpartitionstotransactionhandler/) 实例并添加到生产者的待处理请求队列 [pendingRequests](http://pendingrequests/) 中。![](https://article-images.zsxq.com/FhnjIZxuukSDgCXWzJAZOzPaGjV6)
2.  接着 [addPartitionsToTransactionHandler](http://addpartitionstotransactionhandler/) 会通过 [addPartitionsToTransaction](http://addpartitionstotransactionhandler/) 请求将分区信息发送给协调者。![](https://article-images.zsxq.com/FpQiqKh0pucv2SO9cegDQOZpfNQX)
3.  从等待处理的请求队列中移除下一个请求处理器。
4.  如果下一个请求处理器在处理过程中发生了错误，则不发送该请求，返回 null。
5.  如果下一个请求是 EndTxn 请求，并且事务没有开始，则完成 EndTxn 请求并处理事务的完成。
6.  最后返回下一个请求处理器。

## **2.2 协调者处理事务分区信息**

当事务消息发送出去后，「**协调者**」就会处理这些消息。

Broker 根据请求传递 API 来调用不同接口，[request.header.apiKey](http://request.header.apikey/) 匹配客户端传来的 [ADD\_PARTITIONS\_TO\_TXN](http://add_partitions_to_txn/)。

// handle 方法

case ApiKeys.ADD\_PARTITIONS\_TO\_TXN => handleAddPartitionToTxnRequest(request)

// 处理 AddPartitionToTxnRequest 请求

def handleAddPartitionToTxnRequest(request: RequestChannel.Request): Unit = {

// 确保消息中间件版本兼容KAFKA\_0\_11\_0\_IV0版本

    ensureInterBrokerVersion(KAFKA\_0\_11\_0\_IV0)

// 获取请求中的 AddPartitionsToTxnRequest 对象

val addPartitionsToTxnRequest \= request.body\[AddPartitionsToTxnRequest\]

// 获取事务ID

val transactionalId \= addPartitionsToTxnRequest.data.transactionalId

// 获取要添加的分区列表

val partitionsToAdd \= addPartitionsToTxnRequest.partitions.asScala

if (!authHelper.authorize(request.context, WRITE, TRANSACTIONAL\_ID, transactionalId))

// 如果请求的客户端未经授权，则发送错误响应

      requestHelper.sendResponseMaybeThrottle(request, requestThrottleMs =>

        addPartitionsToTxnRequest.getErrorResponse(requestThrottleMs, Errors.TRANSACTIONAL\_ID\_AUTHORIZATION\_FAILED.exception))

else {

// 未授权的主题分区错误映射

val unauthorizedTopicErrors \= mutable.Map\[TopicPartition, Errors\]()

// 不存在的主题分区错误映射

val nonExistingTopicErrors \= mutable.Map\[TopicPartition, Errors\]()

// 授权的分区集合

val authorizedPartitions \= mutable.Set\[TopicPartition\]()

// 过滤出授权的主题

val authorizedTopics \= authHelper.filterByAuthorized(request.context, WRITE, TOPIC,

        partitionsToAdd.filterNot(tp => Topic.isInternal(tp.topic)))(\_.topic)

for (topicPartition <- partitionsToAdd) {

if (!authorizedTopics.contains(topicPartition.topic))

          unauthorizedTopicErrors += topicPartition -> Errors.TOPIC\_AUTHORIZATION\_FAILED

else if (!metadataCache.contains(topicPartition))

          nonExistingTopicErrors += topicPartition -> Errors.UNKNOWN\_TOPIC\_OR\_PARTITION

else

          authorizedPartitions.add(topicPartition)

      }

if (unauthorizedTopicErrors.nonEmpty || nonExistingTopicErrors.nonEmpty) {

// 如果存在未授权的主题分区或不存在的主题分区，则发送相应的错误响应

val partitionErrors \= unauthorizedTopicErrors ++ nonExistingTopicErrors ++

          authorizedPartitions.map(\_ -> Errors.OPERATION\_NOT\_ATTEMPTED)

        requestHelper.sendResponseMaybeThrottle(request, requestThrottleMs =>

new AddPartitionsToTxnResponse(requestThrottleMs, partitionErrors.asJava))

      } else {

// 定义发送回调函数

        def sendResponseCallback(error: Errors): Unit = {

          def createResponse(requestThrottleMs: Int): AbstractResponse = {

val finalError \=

if (addPartitionsToTxnRequest.version < 2 && error == Errors.PRODUCER\_FENCED)

{

// 对于旧版本的客户端，它们无法理解新的PRODUCER\_FENCED错误码，

// 所以我们需要返回旧INVALID\_PRODUCER\_EPOCH错误码以保持与客户端处理逻辑一致。

                Errors.INVALID\_PRODUCER\_EPOCH

              } else {

                error

              }

// 创建响应体对象

            val responseBody: AddPartitionsToTxnResponse = new AddPartitionsToTxnResponse(requestThrottleMs,

              partitionsToAdd.map{tp => (tp, finalError)}.toMap.asJava)

            trace(s"Completed $transactionalId's AddPartitionsToTxnRequest with partitions $partitionsToAdd: errors: $error from client ${request.header.clientId}")

            responseBody

          }

// 发送响应

          requestHelper.sendResponseMaybeThrottle(request, createResponse)

        }

// 处理添加分区到事务的逻辑操作

        txnCoordinator.handleAddPartitionsToTransaction(transactionalId,

          addPartitionsToTxnRequest.data.producerId,

          addPartitionsToTxnRequest.data.producerEpoch,

          authorizedPartitions,

          sendResponseCallback)

      }

  }

}

步骤如下：

1.  确保消息中间件版本兼容KAFKA\_0\_11\_0\_IV0版本。
2.  获取请求中的 AddPartitionsToTxnRequest 对象、事务ID、要添加的分区列表。
3.  如果请求的客户端未经授权，则发送错误响应。否则过滤出授权的主题。
4.  如果存在未授权的主题分区或不存在的主题分区，则发送相应的错误响应。否则 定义发送回调函数。
5.  发送响应。
6.  处理添加分区到事务的逻辑操作。

接下来，我们来继续剖析下 「**第六步**」到底做了哪些事情。

// 处理向事务添加分区的请求

def handleAddPartitionsToTransaction(transactionalId: String, // 事务ID

producerId: Long, // 生产者ID

producerEpoch: Short, // 生产者Epoch

partitions: collection.Set\[TopicPartition\], // 要添加的分区集合

responseCallback: AddPartitionsCallback): Unit = { // 添加分区回调函数

// 如果事务ID为空，则返回INVALID\_REQUEST错误码

if (transactionalId == null || transactionalId.isEmpty) {

debug(s"Returning ${Errors.INVALID\_REQUEST} error code to client for $transactionalId's AddPartitions request")

responseCallback(Errors.INVALID\_REQUEST)

} else {

// 尝试更新事务元数据并将更新后的元数据附加到事务日志上；

// 如果不存在此类元数据，则将其视为无效的生产者ID映射错误。

val result: ApiResult\[(Int, TxnTransitMetadata)\] = txnManager.getTransactionState(transactionalId).flatMap {

case None \=\> Left(Errors.INVALID\_PRODUCER\_ID\_MAPPING)

case Some(epochAndMetadata) =>

val coordinatorEpoch \= epochAndMetadata.coordinatorEpoch

val txnMetadata \= epochAndMetadata.transactionMetadata

// 生成新的添加分区元数据

txnMetadata.inLock {

if (txnMetadata.producerId != producerId) {

// 如果生产者ID不匹配，则返回INVALID\_PRODUCER\_ID\_MAPPING错误码

Left(Errors.INVALID\_PRODUCER\_ID\_MAPPING)

} else if (txnMetadata.producerEpoch != producerEpoch) {

// 如果生产者周期不匹配，则返回PRODUCER\_FENCED错误码

Left(Errors.PRODUCER\_FENCED)

} else if (txnMetadata.pendingTransitionInProgress) {

// 如果有挂起的事务，则返回CONCURRENT\_TRANSACTIONS错误码

Left(Errors.CONCURRENT\_TRANSACTIONS)

} else if (txnMetadata.state == PrepareCommit || txnMetadata.state == PrepareAbort) {

// 如果当前正在处理提交或中止操作，则返回CONCURRENT\_TRANSACTIONS错误码

Left(Errors.CONCURRENT\_TRANSACTIONS)

} else if (txnMetadata.state == Ongoing && partitions.subsetOf(txnMetadata.topicPartitions)) {

// 如果分区已经在元数据中，则返回NONE错误码（优化）

Left(Errors.NONE)

} else {

// 正常情况下，生成新的（添加分区）元数据

Right(coordinatorEpoch, txnMetadata.prepareAddPartitions(partitions.toSet, time.milliseconds()))

}

}

}

result match {

// 如果处理出错，则返回相应的错误码回调客户端

case Left(err) =>

debug(s"Returning $err error code to client for $transactionalId's AddPartitions request")

responseCallback(err)

// 如果处理成功，则将新的元数据附加到事务日志中，并回调客户端

case Right((coordinatorEpoch, newMetadata)) =>

txnManager.appendTransactionToLog(transactionalId, coordinatorEpoch, newMetadata, responseCallback)

}

}

}

该方法主要用来**处理向事务添加分区的请求逻辑**，步骤如下：

1.  如果事务ID为空，则返回INVALID\_REQUEST错误码。
2.  尝试更新事务元数据并将更新后的元数据附加到事务日志上， 如果不存在此类元数据，则将其视为无效的生产者ID映射错误。
3.  如果存在元数据，则生成新的添加分区元数据 transactionMetadata。
4.  判断元数据各种错误判断，如果没有错误则生成新的（添加分区）元数据。
5.  如果上面处理结果出错，则返回相应的错误码回调客户端。
6.  否则如果处理成功，则将新的元数据附加到事务日志中，并回调客户端。

从上面方法源码中可以看到，协调者会检查该生产者当前的事务状态，如果发现当前事务处于 [PrepareCommit](http://preparecommit/)、[PrepareAbort](http://prepareabort%20/) 状态时则表示「**该事务的前一轮次未完成**」，则返回异常，不会接受生产者提交的事务分区，此时生产者会等待一段时间再重新提交分区信息。

if (txnMetadata.state == PrepareCommit || txnMetadata.state == PrepareAbort) {

// 如果当前正在处理提交或中止操作，则返回CONCURRENT\_TRANSACTIONS错误码

Left(Errors.CONCURRENT\_TRANSACTIONS)

}

接下来，我们来继续剖析下 「**第六步**」到底做了哪些事情。

//将事务元数据追加到事务日志中

def appendTransactionToLog(transactionalId: String, // 事务ID

coordinatorEpoch: Int, // 协调者 epoch

newMetadata: TxnTransitMetadata, // 新的事务元数据

responseCallback: Errors => Unit, // 回调函数，用于处理追加日志的响应

retryOnError: Errors => Boolean = \_ => false): Unit = { // 出错时是否重试的函数，默认为不重试

// 为该事务元数据生成消息

val keyBytes \= TransactionLog.keyToBytes(transactionalId)

val valueBytes \= TransactionLog.valueToBytes(newMetadata)

val timestamp \= time.milliseconds()

val records \= MemoryRecords.withRecords(TransactionLog.EnforcedCompressionType, new SimpleRecord(timestamp, keyBytes, valueBytes))

val topicPartition \= new TopicPartition(Topic.TRANSACTION\_STATE\_TOPIC\_NAME, partitionFor(transactionalId))

val recordsPerPartition \= Map(topicPartition -> records)

// 设置回调函数，在追加日志完成后更新缓存中的事务状态

def updateCacheCallback(responseStatus: collection.Map\[TopicPartition, PartitionResponse\]): Unit = {

// 追加日志的响应应只包含一个主题分区

if (responseStatus.size != 1 || !responseStatus.contains(topicPartition))

throw new IllegalStateException("Append status %s should only have one partition %s"

.format(responseStatus, topicPartition))

val status \= responseStatus(topicPartition)

var responseError \= if (status.error == Errors.NONE) {

Errors.NONE

} else {

....

// 将日志追加错误码转换为对应的协调者错误码

status.error match {

case Errors.UNKNOWN\_TOPIC\_OR\_PARTITION | Errors.NOT\_ENOUGH\_REPLICAS

| Errors.NOT\_ENOUGH\_REPLICAS\_AFTER\_APPEND

| Errors.REQUEST\_TIMED\_OUT => // 对于超时的请求，返回NOT\_AVAILABLE错误码以让客户端重试

Errors.COORDINATOR\_NOT\_AVAILABLE

case Errors.NOT\_LEADER\_OR\_FOLLOWER | Errors.KAFKA\_STORAGE\_ERROR =>

Errors.NOT\_COORDINATOR

case Errors.MESSAGE\_TOO\_LARGE

| Errors.RECORD\_LIST\_TOO\_LARGE =>

Errors.UNKNOWN\_SERVER\_ERROR

case other \=\>

other

}

}

if (responseError == Errors.NONE) {

// 现在尝试更新缓存：我们需要原地更新状态，而不是覆盖整个对象以确保同步

getTransactionState(transactionalId) match {

case Left(err) =>

....

responseError = err

case Right(Some(epochAndMetadata)) =>

val metadata \= epochAndMetadata.transactionMetadata

metadata.inLock {

if (epochAndMetadata.coordinatorEpoch != coordinatorEpoch) {

....

responseError = Errors.NOT\_COORDINATOR

} else {

// 将新的元数据保存到事务元数据集中来替换旧的元数据

metadata.completeTransitionTo(newMetadata)

....

}

}

case Right(None) =>

// 该事务ID不再存在，可能对应的分区已经迁移出去了。返回NOT\_COORDINATOR让客户端重新发现事务协调者

....

responseError = Errors.NOT\_COORDINATOR

}

} else {

// 返回错误时重置待定状态，因为此时事务ID没有活动的事务

getTransactionState(transactionalId) match {

case Right(Some(epochAndTxnMetadata)) =>

val metadata \= epochAndTxnMetadata.transactionMetadata

metadata.inLock {

....

}

case Right(None) =>

// 此时不做任何操作，因为我们希望将原始追加错误返回给用户

case Left(error) =>

// 此时不做任何操作，因为我们希望将原始追加错误返回给用户

}

}

responseCallback(responseError)

}

inReadLock(stateLock) {

// 在追加到本地日志返回之前，我们需要持有事务元数据缓存的读锁；这是为了避免在检查后追加记录之前完成的移出和移入操作，因为在这两个事件之间，使用更高的协调者epoch追加到日志中的条目可能已经通过副本成功复制，并使日志处于错误状态。

getTransactionState(transactionalId) match {

case Left(err) =>

responseCallback(err)

case Right(None) =>

//协调者元数据已被删除，立即回复客户端 NOT\_COORDINATOR

responseCallback(Errors.NOT\_COORDINATOR)

case Right(Some(epochAndMetadata)) =>

val metadata \= epochAndMetadata.transactionMetadata

val append: Boolean = metadata.inLock {

if (epochAndMetadata.coordinatorEpoch != coordinatorEpoch) {

// 协调者epoch已更改，立即回复客户端 NOT\_COORDINATOR

responseCallback(Errors.NOT\_COORDINATOR)

false

} else {

// 不需要检查元数据对象本身，因为在相同的协调者epoch下，没有并发线程能够修改它，因此可以直接追加到事务日志中

true

}

}

if (append) {

// 将新的事务元数据保存到事务状态主题中

replicaManager.appendRecords(

newMetadata.txnTimeoutMs.toLong,

TransactionLog.EnforcedRequiredAcks,

internalTopicsAllowed = true,

origin = AppendOrigin.Coordinator,

recordsPerPartition,

// 回调

updateCacheCallback)

}

}

}

}

该方法比较长，这里重点总结两步：

1.  上面代码结尾会调用 [replicaManager#appendRecords](http://replicamanager/#appendRecords) 方法将新的事务元数据保存到事务状态主题中。

在第一步的调用参数中会传一个回调函数 updateCacheCallback，我们切到这个回调函数，它会在追加日志完成后会调用[epochAndMetadata.transactionMetadata#completeTransitionTo](http://epochandmetadata.transactionmetadata/#completeTransitionTo) 完成一个事务状态的转移，将新的元数据保存到事务元数据集中来替换旧的元数据。

> 这里会将协调者事务状态切换到 Ongoing 状态，协调者事务状态可以从 Empty/CompleteAbort/CompleteCommit 切换到 Ongoing 状态，所以生产者提交/回滚事务完成后，可以调用 KafkaProducer#beginTransaction 方法重新开启该事务。

// 完成一个事务状态的转移 transitMetadata 事务转移所需要的元数据

def completeTransitionTo(transitMetadata: TxnTransitMetadata): Unit = {

// 获取挂起状态

val toState \= pendingState.getOrElse {

// 如果不存在挂起状态，则抛出异常

....

}

// 如果目标状态与挂起状态不一致，则抛出异常

if (toState != transitMetadata.txnState) {

throwStateTransitionFailure(transitMetadata)

} else {

// 根据目标状态执行不同的操作

toState match {

case Empty \=\> // 从initPid转移而来

....

case Ongoing \=\> // 从addPartitions转移而来

....

case PrepareAbort | PrepareCommit => // 从endTxn转移而来

....

case CompleteAbort | CompleteCommit => // 从write markers转移而来

....

case PrepareEpochFence \=>

// 我们永远不应该到达此处，因为一旦我们准备给出时期隔离，我们立即将挂起状态设置为PrepareAbort，然后，在标记被写入之后，将其转换为完整的Abort。因此，我们永远不应该再转移到PrepareEpochFence，因为它不是任何其他状态的有效前一个状态，因此无法转移出它。

throwStateTransitionFailure(transitMetadata)

case Dead \=>

// 正在过期transactionalId。操作的完成应该导致将元数据从缓存中删除，因此我们实际上不能转移到死状态。

}

// 完成状态转移

txnLastUpdateTimestamp = transitMetadata.txnLastUpdateTimestamp

pendingState \= None

state \= toState

}

}

> 这里需要注意的是：一个事务可以重复开启，因此为了避免同一个事务不同轮次【这里将事务从开启到提交完成的过程称为一个轮次】的消息出现混淆，在生产者进入下一轮次事务前，协调者需要保证该一个事务前一次轮次已经完成。

## **03 生产者发送事务消息**

当「**生产者**」给「**协调者**」发送事务分区消息之后，就可以发送消息了。

我们来看下「**生产者**」是如何为消息批次生成序号的。

在 「**生产者**」的 TransactionManager.java 类中，有一个是用来存储主题分区的，如下：

private final TopicPartitionBookkeeper topicPartitionBookkeeper;

private static class TopicPartitionBookkeeper {

// 存储每个TopicPartition对应的TopicPartitionEntry

private final Map<TopicPartition, TopicPartitionEntry> topicPartitions = new HashMap<>();

// 获取给定TopicPartition对应的TopicPartitionEntry

private TopicPartitionEntry getPartition(TopicPartition topicPartition) {

TopicPartitionEntry ent \= topicPartitions.get(topicPartition);

if (ent == null)

throw new IllegalStateException("Trying to get the sequence number for " + topicPartition + ", but the sequence number was never set for this partition.");

return ent;

}

// 获取或创建给定TopicPartition对应的TopicPartitionEntry

private TopicPartitionEntry getOrCreatePartition(TopicPartition topicPartition) {

TopicPartitionEntry ent \= topicPartitions.get(topicPartition);

if (ent == null) {

ent = new TopicPartitionEntry();

topicPartitions.put(topicPartition, ent);

}

return ent;

}

// 添加给定的TopicPartition

private void addPartition(TopicPartition topicPartition) {

this.topicPartitions.putIfAbsent(topicPartition, new TopicPartitionEntry());

}

// 判断TopicPartition对应的TopicPartitionEntry是否存在

private boolean contains(TopicPartition topicPartition) {

return topicPartitions.containsKey(topicPartition);

}

// 重置TopicPartitionBookkeeper，清空所有的TopicPartitionEntry

private void reset() {

topicPartitions.clear();

}

// 获取给定TopicPartition的最新确认的偏移量

private OptionalLong lastAckedOffset(TopicPartition topicPartition) {

TopicPartitionEntry entry \= topicPartitions.get(topicPartition);

if (entry != null && entry.lastAckedOffset != ProduceResponse.INVALID\_OFFSET)

return OptionalLong.of(entry.lastAckedOffset);

else

return OptionalLong.empty();

}

// 获取给定TopicPartition的最新确认的序列号

private OptionalInt lastAckedSequence(TopicPartition topicPartition) {

TopicPartitionEntry entry \= topicPartitions.get(topicPartition);

if (entry != null && entry.lastAckedSequence != NO\_LAST\_ACKED\_SEQUENCE\_NUMBER)

return OptionalInt.of(entry.lastAckedSequence);

else

return OptionalInt.empty();

}

// 以给定的ProducerIdAndEpoch为起点，重置TopicPartition对应的序列号、确认的序列号和ProducerIdAndEpoch

private void startSequencesAtBeginning(TopicPartition topicPartition, ProducerIdAndEpoch newProducerIdAndEpoch) {

final PrimitiveRef.IntRef sequence \= PrimitiveRef.ofInt(0);

TopicPartitionEntry topicPartitionEntry \= getPartition(topicPartition);

// 重置序列号

topicPartitionEntry.resetSequenceNumbers(inFlightBatch -> {

inFlightBatch.resetProducerState(newProducerIdAndEpoch, sequence.value, inFlightBatch.isTransactional());

sequence.value += inFlightBatch.recordCount;

});

topicPartitionEntry.producerIdAndEpoch = newProducerIdAndEpoch;

topicPartitionEntry.nextSequence = sequence.value;

topicPartitionEntry.lastAckedSequence = NO\_LAST\_ACKED\_SEQUENCE\_NUMBER;

}

}

可以看出，[TransactionManager#topicPartitionBookkeeper](http://transactionmanager/#topicPartitionBookkeeper) 属性是一个 [TopicPartitionBookkeeper](http://topicpartitionbookkeeper/) 对象实例。[TopicPartitionBookkeepe](http://topicpartitionbookkeeper/)[r#topicPartitions](http://r/#topicPartitions) 属性是一个 HashMap，键值对内容为：[Map<TopicPartition, TopicPartitionEntry>](http://maptopicpartition,%20topicpartitionentry/), 而 [TopicPartitionEntry](http://maptopicpartition,%20topicpartitionentry/) 中存储了分区的 [ProducerIdAndEpoch](http://produceridandepoch/)、[nextSequence](http://nextsequence/)（下一个序号）、[lastAckedSequence](http://lastackedsequence/)（最后确认的序号）等信息，生产者使用这些信息来生成「**消息批次序号**」，如下：

private static class TopicPartitionEntry {

// 用于给定分区的ProducerIdAndEpoch

private ProducerIdAndEpoch producerIdAndEpoch;

// 下一个批次到达给定分区的基本序列号

private int nextSequence;

// 给定分区的最后一个确认的批次的序列号。当没有待处理的请求时，lastAckedSequence = nextSequence - 1。

private int lastAckedSequence;

// 按序列号有序存储给定分区的待处理批次。这有助于在主节点故障切换期间，确保按序列号对批次进行排序。

// 当一个批次被发送后，将其添加到队列中，当批次完成时（成功或失败）将其从队列中移除。

private SortedSet<ProducerBatch> inflightBatchesBySequence;

// 记录给定分区的最后一个已确认的偏移量，用于区分由于保留期已到期而导致的UnknownProducer响应和由于实际丢失的数据而导致的UnknownProducer响应。

private long lastAckedOffset;

TopicPartitionEntry() {

this.producerIdAndEpoch = ProducerIdAndEpoch.NONE;

this.nextSequence = 0;

this.lastAckedSequence = NO\_LAST\_ACKED\_SEQUENCE\_NUMBER;

this.lastAckedOffset = ProduceResponse.INVALID\_OFFSET;

// 使用基于序列号的Comparator创建按序列号有序的TreeSet

this.inflightBatchesBySequence = new TreeSet<>(Comparator.comparingInt(ProducerBatch::baseSequence));

}

// 重置待处理批次的序列号

void resetSequenceNumbers(Consumer<ProducerBatch> resetSequence) {

TreeSet<ProducerBatch> newInflights = new TreeSet<>(Comparator.comparingInt(ProducerBatch::baseSequence));

for (ProducerBatch inflightBatch : inflightBatchesBySequence) {

// 重置批次的序列号

resetSequence.accept(inflightBatch);

newInflights.add(inflightBatch);

}

inflightBatchesBySequence = newInflights; // 替换为新的有序批次集合

}

}

当生成完「**消息批次序号**」之后，不知道你是否还记得在「**生产者**」源码中剖析过，在「**生产者**」发送消息时，[Sender](http://sender/) 线程会调用 [RecordAccumulator#drainBatchesForOneNode](http://recordaccumulator/#drainBatchesForOneNode) 方法从「**生产者**」消息累加器中来获取消息批次。如果忘记了可以点击 [【生产者源码分析系列第七篇】图解 Kafka 源码之快递仓库 RecordAccumulator 架构设计](https://articles.zsxq.com/id_struwj49p3u8.html) 这篇进行学习。

这里再来剖析下，这里只展示事务相关的源码：

// 获取目标 Node 的 ProducerBatch 集合

private List<ProducerBatch> drainBatchesForOneNode(Cluster cluster, Node node, int maxSize, long now) {

int size \= 0;

List<PartitionInfo> parts = cluster.partitionsForNode(node.id());

List<ProducerBatch> ready = new ArrayList<>();

/\* to make starvation less likely this loop doesn't start at 0 \*/

int start \= drainIndex = drainIndex % parts.size();

do {

PartitionInfo part \= parts.get(drainIndex);

TopicPartition tp \= new TopicPartition(part.topic(), part.partition());

this.drainIndex = (this.drainIndex + 1) % parts.size();

....

synchronized (deque) {

....

// 1、获取最新的消息批次，并判断是否为事务中的消息。

boolean isTransactional \= transactionManager != null && transactionManager.isTransactional();

ProducerIdAndEpoch producerIdAndEpoch \=

transactionManager != null ? transactionManager.producerIdAndEpoch() : null;

ProducerBatch batch \= deque.pollFirst();

if (producerIdAndEpoch != null && !batch.hasSequence()) {

// 2、生产者开启了幂等发送机制，检查 topicPartitionBookkeeper 中是否存在该分区信息，如果不存在则初始化该分区信息。

transactionManager.maybeUpdateProducerIdAndEpoch(batch.topicPartition);

// 3、利用 topicPartitionBookkeeper 中的该分区的 nextSequence 给消息批次设置序号，这里也给消息批次设置了事务消息标志 isTransactional，

batch.setProducerState(producerIdAndEpoch, transactionManager.sequenceNumber(batch.topicPartition), isTransactional);

// 4、增加 topicPartitionBookkeeper 中该分区的序号 nextSequence 。

transactionManager.incrementSequenceNumber(batch.topicPartition, batch.recordCount);

// 5、将批次按其序列号添加到给定分区的待处理批次集合中

transactionManager.addInFlightBatch(batch);

}

// 关闭底层输出流，将 ProducerBatch 设置成只读状态

batch.close();

size += batch.records().sizeInBytes();

// 将 ProducerBatch 记录到 ready 集合中

ready.add(batch);

// 修改 ProducerBatch 的 drainedMs 标记

batch.drained(now);

}

}

} while (start != drainIndex);

return ready;

}

该方法是 **Sender 线程获取每个 Node 需要待发送消息的集合**，主要调用 drainBatchesForOneNode 方法获取目标 node 的待发送消息，步骤如下：

1.  获取最新的消息批次，并判断是否为事务中的消息。
2.  生产者开启了幂等发送机制，检查 topicPartitionBookkeeper 中是否存在该分区信息，如果不存在则初始化该分区信息。
3.  利用 topicPartitionBookkeeper 中的该分区的 nextSequence 给消息批次设置序号，这里也给消息批次设置了事务消息标志 isTransactional。
4.  增加 topicPartitionBookkeeper 中该分区的序号 nextSequence 。
5.  将批次按其序列号添加到给定分区的待处理批次集合中。

> 注意：序列号、事务消息标志会存储在消息属性：baseSequence、isTransactional 中。

synchronized public void maybeUpdateProducerIdAndEpoch(TopicPartition topicPartition) {

if (hasStaleProducerIdAndEpoch(topicPartition) && !hasInflightBatches(topicPartition)) {

// 如果此分区的 ProducerIdAndEpoch 已经过期，且没有待处理的批次，那么将该分区的序列号重置为 0，以便下一个批次（基于新的 producerId 及其epoch）从0开始

topicPartitionBookkeeper.startSequencesAtBeginning(topicPartition, this.producerIdAndEpoch);

log.debug("ProducerId of partition {} set to {} with epoch {}. Reinitialize sequence at beginning.",topicPartition, producerIdAndEpoch.producerId, producerIdAndEpoch.epoch);

}

}

如果当前分区的 [ProducerIdAndEpoch](http://produceridandepoch/) 已过期，而且在该分区上没有任何待处理的批次（已经全部被处理完毕），那么会调用 [topicPartitionBookkeeper#startSequencesAtBeginning](http://topicpartitionbookkeeper/#startSequencesAtBeginning) 方法来重置该分区的序列号，以便下一个批次（基于新的 producerId 和 producerEpoch ）从 0 开始。这是同步方法，以确保线程安全。

synchronized Integer sequenceNumber(TopicPartition topicPartition) {

// 获取给定分区的下一个序列号，如果分区不存在则创建之

return topicPartitionBookkeeper.getOrCreatePartition(topicPartition).nextSequence;

}

synchronized void incrementSequenceNumber(TopicPartition topicPartition, int increment) {

// 获取当前序列号

Integer currentSequence \= sequenceNumber(topicPartition);

// 增加指定的序列号

currentSequence = DefaultRecordBatch.incrementSequence(currentSequence, increment);

// 更新给定分区的下一个序列号

topicPartitionBookkeeper.getPartition(topicPartition).nextSequence = currentSequence;

}

[sequenceNumber](http://sequencenumber/) 方法返回给定分区的下一个序列号，如果该分区还不存在，则创建该分区并返回序列号 0。该方法使用 [synchronized](http://synchronized%20/) 关键字来保证线程安全。

而 [incrementSequenceNumber](http://incrementsequencenumber/) 方法使用 [sequenceNumber](http://sequencenumber/) 方法获取当前序列号，然后使用指定的增量值对序列号进行增加。更新 [topicPartitionBookkeeper](http://topicpartitionbookkeeper/) 中给定分区的下一个序列号。也使用 synchronized 的关键字来确保线程安全。

synchronized void addInFlightBatch(ProducerBatch batch) {

if (!batch.hasSequence())

throw new IllegalStateException("Can't track batch for partition " + batch.topicPartition + " when sequence is not set.");

// 将批次按其序列号添加到给定分区的待处理批次集合中

topicPartitionBookkeeper.getPartition(batch.topicPartition).

inflightBatchesBySequence.add(batch);

}

该方法将给定批次按其序列号添加到给定分区的待处理批次集合中。如果该批次没有设置序列号，则会抛出一个[IllegalStateException](http://illegalstateexception/) 异常。它也是使用 [synchronized](http://synchronized/) 关键字来确保线程安全。

综上，「**生产者**」发送事务消息之前，会先给消息批次设置「**序号**」、「**事务消息标志**」，等这些工作完成后，「**生产者**」就可以正常发送事务消息了。

##   
**04 Broker 协调者处理事务消息**

当 「**生产者**」通过 Produce 请求将消息发送给「**Broker 端**」，此时「**Broker 端**」就要处理消息了，你可能会有以下疑问？

1.  Borker 端是如何利用消息批次序号处理重复消息批次？
2.  Broker 端是如何对事务消息进行处理的呢？

带着这 2 个问题，我们来继续剖析下 Broker 端协调者是如何处理事务消息的。

首先，Broker 根据请求传递 API 来调用不同接口，[request.header.apiKey](http://request.header.apikey/) 匹配客户端传来的 [PRODUCE](http://produce/) 请求。

// handle 方法

case ApiKeys.PRODUCE => handleProduceRequest(request)

看到 [handleProduceRequest](http://handleproducerequest/) 方法最后会调用 [replicaManager#appendRecords](http://replicamanager/#appendRecords) 来处理消息。

![](https://article-images.zsxq.com/Fvf5tjjZqQJbNlmbJGAwo-TIGCIu)

另外，当「**生产者**」调用 [commitTransaction](http://committransaction/) 方法提交事务时，它会发送一个 [EndTransaction](http://endtransaction/) 请求到「**协调者**」，此时「**协调者**」将事务状态切换到 [PrepareCommit](http://preparecommit/) 状态后就会给事务分区的「**Leader 副本**」发送 [WriteTxnMarkers](http://writetxnmarkers/) 请求，要求「**Leader 副本**」提交事务中的消息，「**Leader 副本**」会发送 [WRITE\_TXN\_MARKERS](http://write_txn_markers/) 请求。

// handle 方法

case ApiKeys.WRITE\_TXN\_MARKERS => handleWriteTxnMarkersRequest(request)

![](https://article-images.zsxq.com/FpJR63dyZ8earwemHly-zvSULNEz)

![](https://article-images.zsxq.com/FoXnDiOKnGu1NpJC4Nju2GvxaStn)

由于篇幅过长，提交事务我会放到下篇进行剖析。

关于 [appendRecords](http://replicamanager/#appendRecords%20) 方法点击 [服务端 Broker 源码分析系列第九篇】图解 Kafka 源码之 ReplicaManager 日志读写流程](https://articles.zsxq.com/id_5dq174gc0wji.html) 学习，最终消息会写入到「**Leader 副本**」的本地日志中。

在 Broker 端，Log 日志类型中定义了关于事务相关的属性：

![](https://article-images.zsxq.com/FuIR2vo1sucilgpDatgyBar-9M_5)

这里需要注意下：

1.  [Log#](http://log/#firstUnstableOffsetMetadata)[firstUnstableOffsetMetadata](http://log/#firstUnstableOffsetMetadata) 记录了该分区第一个未提交事务消息的偏移量，用来计算最新稳定消息偏移量 LSO。LSO 取分区高水位和 [Log#](http://log/#firstUnstableOffsetMetadata)[firstUnstableOffsetMetadata](http://log/#firstUnstableOffsetMetadata) 中的较小者，如果消费组启动了 [COMMITTED](http://committed/) 隔离级别，则只能读取 LSO 前面的数据。
2.  [ProducerStateManager#ongoingTxns](http://producerstatemanager/#ongoingTxns%C2%A0%C2%A0) 存储了正在进行中的事务，键值对为：[<offset, TxnMetadata>](http://offset,%20txnmetadata/)。其中 offset 是该事务第一个消息的偏移量，而 [TxnMetadata](http://txnmetadata/) 存储了事务的基础信息。另外 [unreplicatedTxns](http://unreplicatedtxns/) 存储了已完成的事务，但是还未同步给 Follower 副本。

在写入「**Leader 副本**」本地日志时会调用 [Log#append](http://log/#append) 写入消息内容，可以看到在写入消息之前会调用 [Log#](http://log/#analyzeAndValidateProducerState)[analyzeAndValidateProducerState](http://log/#analyzeAndValidateProducerState) 方法来校验消息批次。

![](https://article-images.zsxq.com/FkOu4Oy173zNOFLYUgJQsnXpFmVM)

private def analyzeAndValidateProducerState(appendOffsetMetadata: LogOffsetMetadata,

records: MemoryRecords,

origin: AppendOrigin):

(mutable.Map\[Long, ProducerAppendInfo\], List\[CompletedTxn\], Option\[BatchMetadata\]) = {

val updatedProducers \= mutable.Map.empty\[Long, ProducerAppendInfo\]

val completedTxns \= ListBuffer.empty\[CompletedTxn\]

var relativePositionInSegment \= appendOffsetMetadata.relativePositionInSegment

// 1、遍历所有的消息批次进行处理

records.batches.forEach { batch =>

if (batch.hasProducerId) {

// if this is a client produce request, there will be up to 5 batches which could have been duplicated.

// If we find a duplicate, we return the metadata of the appended batch to the client.

if (origin == AppendOrigin.Client) {

// 2、获取该批次的生产者元数据 producerStateEntry

val maybeLastEntry \= producerStateManager.lastEntry(batch.producerId)

// 调用 findDuplicateBatch 来查找重复的消息批次

maybeLastEntry.flatMap(\_.findDuplicateBatch(batch)).foreach { duplicate =>

return (updatedProducers, completedTxns.toList, Some(duplicate))

}

}

val firstOffsetMetadata \= if (batch.isTransactional)

Some(LogOffsetMetadata(batch.baseOffset, appendOffsetMetadata.segmentBaseOffset, relativePositionInSegment))

else

None

// 3、调用 updateProducers 方法，以消息生产者为维度，将消息的事务信息分组汇总存储到不同的 ProducerAppendInfo 中。

val maybeCompletedTxn \= updateProducers(producerStateManager, batch, updatedProducers, firstOffsetMetadata, origin)

// 4、将上一步得到的 completedTxn 实例添加到 completedTxns 集合中。

maybeCompletedTxn.foreach(completedTxns += \_)

}

relativePositionInSegment += batch.sizeInBytes

}

(updatedProducers, completedTxns.toList, None)

}

该方法主要用来**校验消息批次的**，步骤如下：

1.  遍历所有的消息批次进行处理。
2.  获取该批次的生产者元数据 [producerStateEntry](http://producerstateentry/)，接着调用 [findDuplicateBatch](http://findduplicatebatch/) 来查找重复的消息批次。
3.  接着调用 [updateProducers](http://updateproducers/) 方法，以消息生产者为维度，将消息的事务信息分组汇总存储到不同的 [ProducerAppendInfo](http://producerappendinfo/) 中。
4.  将上一步得到的 [completedTxn](http://completedtxn/) 实例添加到 [completedTxns](http://completedtxns%20/) 集合中。

接着我们剖析 「**第二步**」、「**第三步**」、「**第四步**」的处理逻辑。

先来看 「**第二步**」做了什么事情：

// 这是 ProducerStateManager.scala 方法

def findDuplicateBatch(batch: RecordBatch): Option\[BatchMetadata\] = {

if (batch.producerEpoch != producerEpoch)

None

else

// 比对消息是否重复

batchWithSequenceRange(batch.baseSequence, batch.lastSequence)

}

// Return the batch metadata of the cached batch having the exact sequence range, if any.

def batchWithSequenceRange(firstSeq: Int, lastSeq: Int): Option\[BatchMetadata\] = {

// 在 ProducerStateEntry#batchMetadata 中存储了该生产者最新发生的 5 个消息批次的序号

val duplicate \= batchMetadata.filter { metadata =>

// 如果该集合中存在某个消息批次与请求的消息批次的 baseSequence 即第一条消息的序号，lastSequence 即最后一条的消息的序号相等，则可以认为生产者发送的消息批次重复发送，然后忽略即可。

firstSeq == metadata.firstSeq && lastSeq == metadata.lastSeq

}

duplicate.headOption

}

其判断逻辑如下：

1.  在 [ProducerStateEntry#batchMetadata](http://producerstateentry/#batchMetadata) 中存储了该生产者最新发生的 5 个消息批次的序号。
2.  如果该集合中存在某个消息批次与请求的消息批次的 [baseSequence](http://basesequence%20/) 即第一条消息的序号，[lastSequence](http://lastsequence/) 即最后一条的消息的序号相等，则可以认为生产者发送的消息批次重复发送，然后忽略即可。

private def updateProducers(producerStateManager: ProducerStateManager,

batch: RecordBatch,

producers: mutable.Map\[Long, ProducerAppendInfo\],

firstOffsetMetadata: Option\[LogOffsetMetadata\],

origin: AppendOrigin): Option\[CompletedTxn\] = {

// 获取生产者id

val producerId \= batch.producerId

// 从 updateProducers 集合中查找该消息批次生产者对应的 ProducerAppendInfo 实例，如果不存在则创建

val appendInfo \= producers.getOrElseUpdate(producerId, producerStateManager.prepareUpdate(producerId, origin))

// 调用 ProducerAppendInfo#append 方法将该消息批次的事务消息添加到 ProducerAppendInfo 中

appendInfo.append(batch, firstOffsetMetadata)

}

// 这是 ProducerStateManager.scala 方法

def append(batch: RecordBatch, firstOffsetMetadataOpt: Option\[LogOffsetMetadata\]): Option\[CompletedTxn\] = {

// 如果是控制批次

if (batch.isControlBatch) {

// 获取批次中的消息迭代器。

val recordIterator \= batch.iterator

// 检查消息迭代器是否还有下一个元素。

if (recordIterator.hasNext) {

// 如果有下一个元素，获取下一个元素作为记录。

val record \= recordIterator.next()

// 将记录反序列化为结束事务标记对象。

val endTxnMarker \= EndTransactionMarker.deserialize(record)

// 根据结束事务标记的信息，执行将标记追加到日志中的操作。

appendEndTxnMarker(endTxnMarker, batch.producerEpoch, batch.baseOffset, record.timestamp)

} else {

// 如果控制批次为空，则表示整个事务已从日志中清除，无需追加任何内容，

None

}

} else { // 如果不是控制批次

// 获取第一个偏移量元数据信息，如果参数 firstOffsetMetadataOpt 中没有值，则使用批次的baseOffset 作为默认值。

val firstOffsetMetadata \= firstOffsetMetadataOpt.getOrElse(LogOffsetMetadata(batch.baseOffset))

// 执行将数据批次追加到日志中的操作

appendDataBatch(batch.producerEpoch, batch.baseSequence, batch.lastSequence, batch.maxTimestamp,

firstOffsetMetadata, batch.lastOffset, batch.isTransactional)

None

}

}

// 添加消息批次

def appendDataBatch(epoch: Short,

firstSeq: Int,

lastSeq: Int,

lastTimestamp: Long,

firstOffsetMetadata: LogOffsetMetadata,

lastOffset: Long,

isTransactional: Boolean): Unit = {

val firstOffset \= firstOffsetMetadata.messageOffset

// 校验批次

maybeValidateDataBatch(epoch, firstSeq, firstOffset)

// 添加批次

updatedEntry.addBatch(epoch, lastSeq, lastOffset, (lastOffset - firstOffset).toInt, lastTimestamp)

// 匹配该批次是否是当前事务第一条消息批次

updatedEntry.currentTxnFirstOffset match {

// 如果 isTransactional = false 直接抛异常

case Some(\_) if !isTransactional =>

// Received a non-transactional message while a transaction is active

throw new InvalidTxnStateException(s"Expected transactional write from producer $producerId at " +

s"offset $firstOffsetMetadata in partition $topicPartition")

// 如果 isTransactional = true 表示属于事务消息

case None if isTransactional \=\>

// Began a new transaction

// 将该消息批次的第一条消息偏移量存储到 ProducerStateEntry#currentTxnFirstOffset 中

updatedEntry.currentTxnFirstOffset = Some(firstOffset)

// 创建该事务对应的 TxnMetadata 实例并添加到 ProducerAppendInfo#transactions 中

transactions += TxnMetadata(producerId, firstOffsetMetadata)

case \_ \=\> // nothing to do

}

}

// 追加结束事务标记

def appendEndTxnMarker(

endTxnMarker: EndTransactionMarker,

producerEpoch: Short,

offset: Long,

timestamp: Long

): Option\[CompletedTxn\] = {

checkProducerEpoch(producerEpoch, offset)

checkCoordinatorEpoch(endTxnMarker, offset)

// 创建并返回一个 completedTxn 实例，completedTxn 定义了 producerId（事务生产者id）、firstOffset、lastOffset（事务消息偏移量范围）等，用来存储该事务的信息

val completedTxn \= updatedEntry.currentTxnFirstOffset.map { firstOffset =>

CompletedTxn(producerId, firstOffset, offset, endTxnMarker.controlType == ControlRecordType.ABORT)

}

updatedEntry.maybeUpdateProducerEpoch(producerEpoch)

updatedEntry.currentTxnFirstOffset = None

updatedEntry.coordinatorEpoch = endTxnMarker.coordinatorEpoch

updatedEntry.lastTimestamp = timestamp

// 返回

completedTxn

}

这里举一个例子来说明这两个步骤：

假设生产者 [producerId = 3](http://producerid%20=%201/) 在一个事务中发送了 3 个消息批次 \[20, 30\]、\[40，50\]、\[60，70\]，此时会追加批次，生成一个 [TxnMetadata](http://txnmetadata/) 实例，并设置 [ProducerStateEntry#currentTxnFirstOffset](http://producerstateentry/#currentTxnFirstOffset) 为 20，当 Broker 端收到协调者发送的该事务的 [WriteTxnMarkers](http://writetxnmarkers/) 请求，并且该请求生产的消息偏移量为 80 时，则此时会生成一个 [completedTxn](http://completedtxn/) 实例，属性 [firstOffset = 20](http://firstoffset%20=%2020/)、[lastOffset = 60](http://lastoffset%20=%2060/)、[producerId = 3](http://producerid%20=%203/) ，最终偏移量范围 \[20,80) 内 [producerId = 3](http://producerid%20=%203/) 的消息就是该事务的消息。

综上，当 [Log#](http://log/#analyzeAndValidateProducerState)[analyzeAndValidateProducerState](http://log/#analyzeAndValidateProducerState) 方法执行完成后，会返回以下数据：

1.  **updateProducers**：在 ProducerAppendInfo 集合的 transactions 携带了新的事务信息。
2.  **completedTxns**：已完成的事务。
3.  **maybeDuplicate**：重复的消息批次。

我们接着来看 Log#append 方法会对这些数据进行处理，源码如下：

// 2.8 版本 源码跟 2.7 版本稍有不同

// 获取校验消息批次返回对应数据

val (updatedProducers, completedTxns, maybeDuplicate) = analyzeAndValidateProducerState(

logOffsetMetadata, validRecords, origin)

maybeDuplicate match {

// 如果重复了 直接忽略

case Some(duplicate) =>

appendInfo.firstOffset = Some(LogOffsetMetadata(duplicate.firstOffset))

appendInfo.lastOffset = duplicate.lastOffset

appendInfo.logAppendTime = duplicate.timestamp

appendInfo.logStartOffset = logStartOffset

case None \=\>

// Before appending update the first offset metadata to include segment information

appendInfo.firstOffset = appendInfo.firstOffset.map { offsetMetadata =>

offsetMetadata.copy(segmentBaseOffset = segment.baseOffset, relativePositionInSegment = segment.size)

}

// 1、执行真正的消息写入操作，主要调用日志段对象的 append 方法实现

segment.append(largestOffset = appendInfo.lastOffset,

largestTimestamp = appendInfo.maxTimestamp,

shallowOffsetOfMaxTimestamp = appendInfo.offsetOfMaxTimestamp,

records = validRecords)

updateLogEndOffset(appendInfo.lastOffset + 1)

// 2、 调用 producerStateManager.update 方法将 ProducerAppendInfo#transactions 中的事务转移到 producerStateManager#ongoingTxns 中，表示这些事务正在进行中

updatedProducers.values.foreach(producerAppendInfo => producerStateManager.update(producerAppendInfo))

// 3、提交已完成事务的消息

completedTxns.foreach { completedTxn =>

val lastStableOffset \= producerStateManager.lastStableOffset(completedTxn)

segment.updateTxnIndex(completedTxn, lastStableOffset)

producerStateManager.completeTxn(completedTxn)

}

....

// 4、 尝试增加第一个不稳定偏移量的值。

maybeIncrementFirstUnstableOffset()

步骤如下：

1.  执行真正的消息写入操作，主要调用日志段对象的 append 方法实现。
2.  调用 [producerStateManager.update](http://producerstatemanager.update/) 方法将 [ProducerAppendInfo#transactions](http://producerappendinfo/#transactions) 中的事务转移到 [producerStateManager#ongoingTxns](http://producerstatemanager/#ongoingTxns) 中，表示这些事务正在进行中。
3.  提交已完成事务的消息。
4.  尝试增加第一个不稳定偏移量的值。

接着我们剖析 「**第三步**」、「**第四步**」的处理逻辑。

先来看「**第三步**」：

// 会调用 logSegment#updateTxnIndex 方法

segment.updateTxnIndex(completedTxn, lastStableOffset)

def updateTxnIndex(completedTxn: CompletedTxn, lastStableOffset: Long): Unit = {

// 如果事务被回滚，则将事务消息追加到 LogSegment#txnIndex 中

if (completedTxn.isAborted) {

trace(s"Writing aborted transaction $completedTxn to transaction index, last stable offset is $lastStableOffset")

txnIndex.append(new AbortedTxn(completedTxn, lastStableOffset))

}

}

// 会调用 producerStateManager#completeTxn 方法

producerStateManager.completeTxn(completedTxn)

// 将事务从 ongoingTxns 转移到 unreplicatedTxns 中，表示事务已经完成，等待从节点同步复制

def completeTxn(completedTxn: CompletedTxn): Unit = {

// 从 ongoingTxns 将已完成事务的首条偏移移除

val txnMetadata \= ongoingTxns.remove(completedTxn.firstOffset)

if (txnMetadata == null)

throw new IllegalArgumentException(s"Attempted to complete transaction $completedTxn on

partition $topicPartition " +s"which was not started")

// 计算最后一条偏移量

txnMetadata.lastOffset = Some(completedTxn.lastOffset)

// 给 unreplicatedTxns 添加已完成事务的首条偏移

unreplicatedTxns.put(completedTxn.firstOffset, txnMetadata)

}

接着来看「**第四步**」：

maybeIncrementFirstUnstableOffset()

private def maybeIncrementFirstUnstableOffset(): Unit = lock synchronized {

checkIfMemoryMappedBufferClosed()

val updatedFirstStableOffset \= producerStateManager.firstUnstableOffset match {

case Some(logOffsetMetadata) if logOffsetMetadata.messageOffsetOnly || logOffsetMetadata.messageOffset < logStartOffset =>

val offset \= math.max(logOffsetMetadata.messageOffset, logStartOffset)

Some(convertToOffsetMetadataOrThrow(offset))

case other \=\> other

}

// 更新 Log#firstUnstableOffsetMetadata，它的取值规则：取生产者管理状态中的第一个不稳定偏移量的偏移量 logOffsetMetadata.messageOffset 和日志的基准偏移量 logStartOffset 之间较大的那个。

if (updatedFirstStableOffset != this.firstUnstableOffsetMetadata) {

debug(s"First unstable offset updated to $updatedFirstStableOffset")

this.firstUnstableOffsetMetadata = updatedFirstStableOffset

}

}

当事务消息被 「**Follower 副本**」同步复制后，此时会调用 [Log#updateHighWatermarkMetadata](http://log/#updateHighWatermarkMetadata) 方法来更新高水位，另外还会移除 producerStateManager#unreplicatedTxns 中已同步数据的事务属性，并更新 [Log#firstUnstableOffsetMetadata](http://log/#firstUnstableOffsetMetadata) 属性。 **这样就可以保证这些已经同步完成的事务消息就可以被所有的消费者读取到了**。

private def updateHighWatermarkMetadata(newHighWatermark: LogOffsetMetadata): Unit = {

if (newHighWatermark.messageOffset < 0)

throw new IllegalArgumentException("High watermark offset should be non-negative")

lock synchronized {

if (newHighWatermark.messageOffset < highWatermarkMetadata.messageOffset) {

warn(s"Non-monotonic update of high watermark from $highWatermarkMetadata to $newHighWatermark")

}

// 赋值高水位元数据

highWatermarkMetadata = newHighWatermark

// 这里重点

producerStateManager.onHighWatermarkUpdated(newHighWatermark.messageOffset)

maybeIncrementFirstUnstableOffset()

}

trace(s"Setting high watermark $newHighWatermark")

}

def onHighWatermarkUpdated(highWatermark: Long): Unit = {

// 移除已同步数据的事务属性

removeUnreplicatedTransactions(highWatermark)

}

private def removeUnreplicatedTransactions(offset: Long): Unit = {

val iterator \= unreplicatedTxns.entrySet.iterator

while (iterator.hasNext) {

val txnEntry \= iterator.next()

val lastOffset \= txnEntry.getValue.lastOffset

if (lastOffset.exists(\_ < offset))

// 迭代移除

iterator.remove()

}

}

## **05 ACK 偏移量发送与处理流程**

最后我们来聊聊 ACK 偏移量的处理流程。

首先在「**生产者**」中，会先调用 [KafkaProducer#sendOffsetsToTransaction](http://kafkaproducer/#sendOffsetsToTransaction) 方法发送 ACK偏移量到事务中。

![](https://article-images.zsxq.com/FiNjCHlG9nTwFPLuKGwT6ZgLbBFH)

![](https://article-images.zsxq.com/Fs6CwlZpFYMATcELmR2n-mV1zWsv)

最后 [AddOffsetsToTxnHandler](http://addoffsetstotxnhandler/) 会构建并发送 [AddOffsetsToTxn](http://addoffsetstotxn/) 请求给 「**协调者**」。

  
当事务消息发送出去后，「**协调者**」就会处理 [AddOffsetsToTxn](http://addoffsetstotxn/) 请求。

Broker 根据请求传递 API 来调用不同接口，[request.header.apiKey](http://request.header.apikey/) 匹配客户端传来的

[ADD\_OFFSETS\_TO\_TXN](http://add_offsets_to_txn/)。

// handle 方法

case ApiKeys.ADD\_OFFSETS\_TO\_TXN => handleAddOffsetsToTxnRequest(request)

def handleAddOffsetsToTxnRequest(request: RequestChannel.Request): Unit = {

// 确保 broker 版本达到 KAFKA\_0\_11\_0\_IV0，否则抛出异常

ensureInterBrokerVersion(KAFKA\_0\_11\_0\_IV0)

// 从请求体中提取出 AddOffsetsToTxnRequest

val addOffsetsToTxnRequest \= request.body\[AddOffsetsToTxnRequest\]

// 获取 transactionalId 和 groupId

val transactionalId \= addOffsetsToTxnRequest.data.transactionalId

val groupId \= addOffsetsToTxnRequest.data.groupId

// 根据 groupId 获取 TopicPartition

val offsetTopicPartition \= new TopicPartition(GROUP\_METADATA\_TOPIC\_NAME, groupCoordinator.partitionFor(groupId))

// 检查是否有对 transactionalId 进行 WRITE 权限

if (!authHelper.authorize(request.context, WRITE, TRANSACTIONAL\_ID, transactionalId))

// 没有权限则发送权限验证失败的响应

requestHelper.sendResponseMaybeThrottle(request, requestThrottleMs =>

new AddOffsetsToTxnResponse(new AddOffsetsToTxnResponseData()

.setErrorCode(Errors.TRANSACTIONAL\_ID\_AUTHORIZATION\_FAILED.code)

.setThrottleTimeMs(requestThrottleMs)))

// 检查是否有对 groupId 进行 READ 权限

else if (!authHelper.authorize(request.context, READ, GROUP, groupId))

// 没有权限则发送权限验证失败的响应

requestHelper.sendResponseMaybeThrottle(request, requestThrottleMs =>

new AddOffsetsToTxnResponse(new AddOffsetsToTxnResponseData()

.setErrorCode(Errors.GROUP\_AUTHORIZATION\_FAILED.code)

.setThrottleTimeMs(requestThrottleMs)))

else {

// 如果权限检查都通过，则定义发送响应的回调函数

def sendResponseCallback(error: Errors): Unit = {

// 定义创建响应的函数

def createResponse(requestThrottleMs: Int): AbstractResponse = {

// 如果请求版本小于 2，并且错误是 PRODUCER\_FENCED，则返回 INVALID\_PRODUCER\_EPOCH，

// 因为旧版本客户端无法理解新的 PRODUCER\_FENCED 错误码

val finalError \=

if (addOffsetsToTxnRequest.version < 2 && error == Errors.PRODUCER\_FENCED) {

Errors.INVALID\_PRODUCER\_EPOCH

} else {

error

}

// 创建响应

val responseBody: AddOffsetsToTxnResponse = new AddOffsetsToTxnResponse(

new AddOffsetsToTxnResponseData()

.setErrorCode(finalError.code)

.setThrottleTimeMs(requestThrottleMs))

// 打印请求日志，用于跟踪

trace(s"Completed $transactionalId's AddOffsetsToTxnRequest for group $groupId on partition " +

s"$offsetTopicPartition: errors: $error from client ${request.header.clientId}")

responseBody

}

// 发送响应

requestHelper.sendResponseMaybeThrottle(request, createResponse)

}

// 调用 TxnCoordinator 的 handleAddPartitionsToTransaction 方法加入事务

txnCoordinator.handleAddPartitionsToTransaction(transactionalId,

addOffsetsToTxnRequest.data.producerId,

addOffsetsToTxnRequest.data.producerEpoch,

Set(offsetTopicPartition),

sendResponseCallback)

}

}

def handleAddPartitionsToTransaction(transactionalId: String,

producerId: Long,

producerEpoch: Short,

partitions: collection.Set\[TopicPartition\],

responseCallback: AddPartitionsCallback): Unit = {

if (transactionalId == null || transactionalId.isEmpty) {

// 如果事务ID为空，则返回无效请求错误给客户端

debug(s"Returning ${Errors.INVALID\_REQUEST} error code to client for $transactionalId's AddPartitions request")

responseCallback(Errors.INVALID\_REQUEST)

} else {

// 尝试更新事务元数据并将更新后的元数据追加到事务日志；

// 如果不存在此类元数据，则将其视为无效的生产者ID映射错误。

val result: ApiResult\[(Int, TxnTransitMetadata)\] = txnManager.getTransactionState(transactionalId).flatMap {

case None \=\> Left(Errors.INVALID\_PRODUCER\_ID\_MAPPING)

case Some(epochAndMetadata) =>

val coordinatorEpoch \= epochAndMetadata.coordinatorEpoch

val txnMetadata \= epochAndMetadata.transactionMetadata

// 生成添加分区后的新事务元数据

txnMetadata.inLock {

if (txnMetadata.producerId != producerId) {

Left(Errors.INVALID\_PRODUCER\_ID\_MAPPING) // 若生产者ID不匹配，则返回无效的生产者ID映射错误

} else if (txnMetadata.producerEpoch != producerEpoch) {

Left(Errors.PRODUCER\_FENCED) // 若生产者已被禁用，则返回生产者已被禁用错误

} else if (txnMetadata.pendingTransitionInProgress) {

Left(Errors.CONCURRENT\_TRANSACTIONS) // 若已有正在进行的过渡事务，则返回并发事务错误

} else if (txnMetadata.state == PrepareCommit || txnMetadata.state == PrepareAbort) {

Left(Errors.CONCURRENT\_TRANSACTIONS) // 若事务处于准备提交或准备中止状态，则返回并发事务错误

} else if (txnMetadata.state == Ongoing && partitions.subsetOf(txnMetadata.topicPartitions)) {

// 优化：如果分区已经在元数据中，立即返回成功

Left(Errors.NONE)

} else {

Right(coordinatorEpoch, txnMetadata.prepareAddPartitions(partitions.toSet, time.milliseconds())) // 返回新的事务元数据

}

}

}

result match {

case Left(err) =>

// 返回错误码给客户端

debug(s"Returning $err error code to client for $transactionalId's AddPartitions request")

responseCallback(err)

case Right((coordinatorEpoch, newMetadata)) =>

// 将事务追加到日志

txnManager.appendTransactionToLog(transactionalId, coordinatorEpoch, newMetadata, responseCallback)

}

}

}

简化步骤如下：

1.  生成一个分区信息，该分区的主题即偏移量主题，分区下标即消费者偏移量存储分区的下标。
2.  将该分区信息添加到事务元数据中，并将新的事务元数据写入事务状态主题中。
3.  返回响应给生产者。

![](https://article-images.zsxq.com/FlYYjBbVHzuBpURpmUEfuxJzA9js)

最后「**生产者**」会触发 [AddOffsetsToTxnHandler#handleResponse](http://addoffsetstotxnhandler/#handleResponse) 方法收到「**协调者**」返回的 [AddOffsetsToTxn](http://addoffsetstotxn/) 成功响应后，会添加一个 [TxnOffsetCommitHandler](http://txnoffsetcommithandler/) 到待处理请求队列 [pendingRequests](http://pendingrequests%20/) 中，[TxnOffsetCommitHandler](http://txnoffsetcommithandler/) 会构建 [TxnOffsetCommit](http://txnoffsetcommithandler/) 请求，将 ACK 偏移量发送给「**消费组协调者**」。

![](https://article-images.zsxq.com/Fi8hjC_RLXzqvzIw6NuOvWEFICJJ)

![](https://article-images.zsxq.com/FlHKs8fWt8N4XprWbOtKPCBuGpui)

接着「**消费组协调者**」收到该请求后，会将偏移量信息存储到偏移量主题中。

> 注意：此时消费组协调者并不会提交这些 ACK 偏移量，也就是说不会将这些偏移量添加到 GroupMetadata#offsets 中。

至此整个「**事务消息**」的初发送处理流程就带你剖析完成了，事务消息太绕了，抽空补图。

## **04 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头从「**3 个问题**」引出了「**事务消息发送**」中遇到的问题。

2、接着带大家剖析了「**事务消息发送与处理流程**」的方方面面，首先「**生产者发送事务分区到协调者**」、当完成后「**生产者开始发送事务消息到协调者处理**」、最后「**ACK 偏移量发送与处理流程**」。

下篇我们来深度剖析「**事务消息提交处理流程**」，大家期待，我们下期见。