大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端****Broker 异步更新元数据缓存实现原理**」，了解了「**Broker 元数据**」是什么以及如何被更新的，从今天开始，我们接着来深度剖析 Kafka「**Broker**」的底层源码实现，我们接着来深度聊聊「**Kafka 服务端事务设计以及初始化流程**」，看看 Kafka 事务机制是如何设计的。

![](https://article-images.zsxq.com/FoV-5OKN71TxQHdZjJhKi_CcysL6)

## **01 总体概述**

在 MySQL 等传统数据库中，「**事务机制**」是非常重要的额，它可以保证一个业务内的多个写入操作的原子性。比如典型的转账操作，「**转出**」、「**转入**」等操作涉及的数据库操作应该放在一个事务中，从而保证这两个操作要么执行成功、要么都不执行。

而在消息中间件中，典型的事务操作就是 Flink 等流式计算框架的 consume-process-produce 的场景：首先从 Kafka 的源主题中获取数据，经过计算处理后，最后将结果写入目标主题中。

在这个场景中，需要保证「**消费源主题**」、「**提交偏移量**」、「**发送结果到目标主题**」等操作的原子性，要么执行成功，要么都不执行。

那么 Kafka 的「**事务机制**」是如何设计的，它的初始化又做了哪些事情呢？

带着这两个问题，开启我们今天的话题。

## **02 Kafka 事务设计**

在 Kafka 0.11 版本中引入了 「**事务机制**」，它可以提供以下保证：

1.  支持多个操作的「**原子性**」执行：要么执行成功、要么不执行，且这些操作支持「**跨主题**」、「**跨分区**」。
2.  「**精确一次**」的语义保证，它可以保证事务中的操作只会执行一次。![](https://article-images.zsxq.com/FpZxhp18Z3Jwje_tRf3mmwtU_Efs)
3.  执行消费者中不同的隔离级别：「**UNCOMMITTED**」隔离级别可以读取未提交的事务消息；而「**COMMITTED**」隔离级别只能读取已提交的事务消息。

由于 Kafka 是分布式系统，不同的主题、分区数据存储在不同的 Broker 节点上。所以要支持「**跨主题**」、「**跨分区**」的原子性操作，就必须实现分布式事务。Kafka 底层使用类似 「**2PC**」的协议来实现分布式事务。

## **2.1 生产者幂等发送机制**

Kafka 的「**事务机制**」是在生产者「**幂等发送机制**」之上实现的。为了实现生产者的「**幂等发送机制**」,Kafka 引入了 「**ProducerId**」 和 「**消息批次序号 Sequence Number**」。

生成过程如下：在生产者初始化时，要求 Broker 给它分配一个唯一的 「**ProducerId**」，并且生产者给每个分区维护一个从0开始单调递增的 「**消息批次序号 Sequence Number**」。

> 这里需要注意的是：ProducerId 不会暴露给外部，对于外部来说是透明的。

当生产者发送消息时，会将 「**ProducerId**」、「**消息批次序号 Sequence Number**」作为消息批次的属性一次性发送给 Broker 端，并且每发送一次消息批次后就会将该序列号 + 1。

然后 Broker 端会记录所有生产者下每个分区的最新的「**消息批次序号 Sequence Number**」。当 Broker 收到消息批次时，会使用 「**ProducerId**」查找对应的生产者，并获取分区的最新 「**消息批次序号 Sequence Number**」，然后跟生产者发送过来的消息批次的「**消息批次序号 Sequence Number**」进行对比：

1.  如果生产者发送的消息批次序号比 Broker 端记录的最新消息批次序号超过 1 ，则表示中间有数据未写入，此时会乱序， Broker 就会拒绝该消息。
2.  如果生产者发送的消息批次序号小于或者等于 Broker 端记录的最新消息批次序号，则表示该消息已被保存，为重复消息，此时 Broker 就会直接丢弃该消息。

> Kafka 使用生产者提供的配置项 enable.idempotence 来启动生产者幂等发送机制。

## **2.2 Transaction ID 机制**

生产者要使用事务，都必须设置 [transaction.id](http://transaction.id/) 配置项来指定一个唯一的「**Transaction Id**」。为了生产者重启后继续提交或者回滚事务，「**Transaction Id**」必须要保证重启后不变，这里将 「**Transaction Id**」称为事务ID。

> Kafka 生产者设置了 transaction.id 配置项后，自动也会启动生产者幂等发送机制。

为了保证新生产者启动后，具有相同 「**Transaction Id**」的旧生产者会立即失效，每次生产者初始化时，还会生成一个单调递增的 Produce Epoch。然后生产者发送请求中会携带该 Epoch 信息，由于旧生产者的 Epoch 要小于新生产者的 Epoch， Kafka 就可以直接抛弃旧生产者的请求了。

## **2.3 事务协调者**

跟消费者组协调者一样，「**事务机制**」也有事务协调者，它会从 Broker 节点中选出一个事务协调者，来协同 Kafka 集群完成事务的提交、回滚等操作。

## **2.4 事务消息存储**

跟消费者提交位移一样，在 Kafka 会有一个内部主题来存储事务消息，名为 \_\_transaction\_state，从名字看，你可以暂且叫它 「**事务状态主题**」。

## **2.5 协调者 failover**

当事务进入 「**PREPARE\_COMMIT**」状态后，协调者已经将事务分区、偏移量等内容存储到事务状态主题中，然后需要由协调者来完成事务。如果在完成事务前， 该协调者挂了，那么 Kafka 会重新选举新的协调者，然后从日志中重新加载这些处于「**PREPARE\_COMMIT**」状态的事务，继续尝试完成它们。

## **03 事务初始化处理流程**

这里将「**事务处理流程**」分为三部分来拆解剖析，分别是：「**初始化事务**」、「**处理事务消息**」、「**提交事务**」。

由于内容比较多，本文只剖析第一部分。剩余两部分会放到下篇进行剖析。

## **3.1 事务初始化处理流程**

这个相对比较简单，步骤如下：

1.  生产者会选择一个「**当前待完成请求最少**」的 Broker 节点来发送 「**FindCoordinator**」请求来查找事务协调者，然后 Broker 会将协调者发送给生产者。
2.  接着生产者发送 「**InitProducerId**」 请求给协调者，然后协调者会生成 「**ProducerId**」、「**Producer Epoch**」并返回给生产者。
3.  最后生产者启动事务。

流程图如下：

![](https://article-images.zsxq.com/Fk2Bd0VnL50QUSU6DD3yFbXV5ZmS)

## **3.2 事务初始化源码实现**

## **3.2.1 事务定义**

### **生产者事务定义**

「**KafkaProducer**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/clients/src/main/java/org/apache/kafka/clients/producer/KafkaProducer.java](https://github.com/apache/kafka/blob/2.8.0/clients/src/main/java/org/apache/kafka/clients/producer/KafkaProducer.java)

在生产者中，kafkaProducer#transactionManager 是一个事务管理器实例，它主要用来负责管理事务。如下：

![](https://article-images.zsxq.com/Fs7HxM7Ru011J7KbdBGCWvFBrM5H)

在「**KafkaProducer**」中事务相关的关键属性如下图：

![](https://article-images.zsxq.com/FtPJEwKpTEgqU2MQmf38H_1K2kSR)

从上图可以看出，这里有 2 个协调者：

1.  consumerGroupCoordinator：消费者协调者，生产者会将 ACK 偏移量发送给消费者组协调者。
2.  transactionCoordinator：事务协调者。

另外我们来看下事务管理器中的 CurrentState，它存储了生产者的事务状态，如下：

![](https://article-images.zsxq.com/FjM0zu4ej20Arc12D1JysjBBdvvl)

### **协调者事务定义**

「**TransactionCoordinator**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/coordinator/transaction/TransactionCoordinator.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/coordinator/transaction/TransactionCoordinator.scala)

「**TransactionStateManager**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/coordinator/transaction/TransactionStateManager.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/coordinator/transaction/TransactionStateManager.scala)

「**TransactionMetadata**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/coordinator/transaction/TransactionMetadata.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/coordinator/transaction/TransactionMetadata.scala)

在协调者中，「**TransactionCoordinator**」代表一个协调者，其关键属性如下图：

  
![](https://article-images.zsxq.com/FpPq-7orJxCQBIVFnDT_UIz-LJTV)

「**TransactionMetadata**」中存储了一个事务的元数据，[TransactionMetadata#state](http://transactionmetadata/#state) 属性存储了协调器的事务状态。

![](https://article-images.zsxq.com/FhpVL1jcm9My-9ZKQzI4qOYPW1KN)

## **3.2.2 生产者初始化事务**

## **3.2.2.1 在生产者中查找协调器**

「**TransactionManager**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/clients/src/main/java/org/apache/kafka/clients/producer/internal/TransactionManager.java](https://github.com/apache/kafka/blob/2.8.0/clients/src/main/java/org/apache/kafka/clients/producer/internal/TransactionManager.java)

// 优先级队列 用来存储待处理的事务请求

private final PriorityQueue<TxnRequestHandler> pendingRequests;

[TransactionManager#pendingRequest](http://transactionmanager/#pendingRequest) 是一个待处理请求的队列，用来存储待发送的事务请求。 该队列的元素类型是 TxnRequestHandler，代表一个待处理的事务相关操作，可以生成一个事务请求。

**既然有存储待发送的请求队列，那么究竟是如何被发送出去的呢？**

不知道你还记不记得，当时在剖析生产者发送线程的处理流程呢？ 如果你已经忘记或者没有学习过的话可以直接点击 [【生产者源码分析系列第八篇】图解 Kafka 源码之 Sender 线程架构设计](https://articles.zsxq.com/id_4xz5pgx1wi2g.html) 这篇文章进行学习。

在 [Sender#runOnce](http://sender/#runOnce) 方法中调用 [Sender#maybeSendAndPollTransactionalRequest](http://sender/#maybeSendAndPollTransactionalRequest) 方法来处理，源码如下：

private boolean maybeSendAndPollTransactionalRequest() {

// 1、如果存在未完成的事务请求，等待其返回

if (transactionManager.hasInFlightRequest()) {

client.poll(retryBackoffMs, time.milliseconds());

return true;

}

// 2、如果事务管理器存在可中止的错误或者正在中止中，且累加器包含未完成的消息批次，则终止未完成的消息批次

if (transactionManager.hasAbortableError() || transactionManager.isAborting()) {

if (accumulator.hasIncomplete()) {

// 尝试获取导致事务中止的最后一个错误

RuntimeException exception \= transactionManager.lastError();

// 如果没有错误但是仍在中止中，

// 则这很可能是没有致命错误的情况

if (exception == null) {

exception = new TransactionAbortedException();

}

//

accumulator.abortUndrainedBatches(exception);

}

}

// 3、如果事务管理器正在完成事务且累加器没有正在进行的刷新操作，开始执行刷新操作

if (transactionManager.isCompleting() && !accumulator.flushInProgress()) {

// 可能仍有请求在重试中。由于我们不知道它们是否已经成功添加到代理程序日志中，

// 我们必须重发它们，直到其最终状态明确为止。

// 如果它们已经被添加但我们没有接收到错误，那么我们的序列号将不再正确，这将导致OutOfSequenceException。

accumulator.beginFlush();

}

// 4、transactionManager.nextRequest 方法会从待处理请求队列中获取下一个 TxnRequestHandler

TransactionManager.TxnRequestHandler nextRequestHandler \= transactionManager.nextRequest(accumulator.hasIncomplete());

if (nextRequestHandler == null)

return false;

// 生成一个 requestBuilder，该 requestBuilder 负责生成下一个事务请求

AbstractRequest.Builder<?> requestBuilder = nextRequestHandler.requestBuilder();

Node targetNode \= null;

try {

// 5、nextRequestHandler.CoordinatorType 方法返回下一个事务请求的目标节点类型

FindCoordinatorRequest.CoordinatorType coordinatorType \= nextRequestHandler.coordinatorType();

// 这里分为 3 种情况：

targetNode = coordinatorType != null ?

transactionManager.coordinator(coordinatorType) :

client.leastLoadedNode(time.milliseconds());

// 6、如果目标节点未准备好，或者目标节点为空，则表示需要查找目标节点，此时调用 maybeFindCoordinatorAndRetry 方法生成一个 FindCoordinatorHandler 放入待处理队列，并将上面获取到的 TxnRequestHandler 重新放入待处理请求队列，以便获取目标节点成功后再处理该 handler。

if (targetNode != null) {

if (!awaitNodeReady(targetNode, coordinatorType)) {

log.trace("Target node {} not ready within request timeout, will retry when node is ready.", targetNode);

// 目标节点未就绪，等待一段时间后重试

maybeFindCoordinatorAndRetry(nextRequestHandler);

return true;

}

} else if (coordinatorType != null) {

log.trace("Coordinator not known for {}, will retry {} after finding coordinator.", coordinatorType, requestBuilder.apiKey());

// 协调器未被发现，先发起协调器查找请求，然后重试

maybeFindCoordinatorAndRetry(nextRequestHandler);

return true;

} else {

log.trace("No nodes available to send requests, will poll and retry when until a node is ready.");

// 没有可用的节点，等待一段时间后重试

transactionManager.retry(nextRequestHandler);

client.poll(retryBackoffMs, time.milliseconds());

return true;

}

if (nextRequestHandler.isRetry())

time.sleep(nextRequestHandler.retryBackoffMs());

// 7、

long currentTimeMs \= time.milliseconds();

ClientRequest clientRequest \= client.newClientRequest(targetNode.idString(), requestBuilder, currentTimeMs,

true, requestTimeoutMs, nextRequestHandler);

log.debug("Sending transactional request {} to node {} with correlation ID {}", requestBuilder, targetNode, clientRequest.correlationId());

client.send(clientRequest, currentTimeMs);

transactionManager.setInFlightCorrelationId(clientRequest.correlationId());

client.poll(retryBackoffMs, time.milliseconds());

return true;

} catch (IOException e) {

log.debug("Disconnect from {} while trying to send request {}. Going " +

"to back off and retry.", targetNode, requestBuilder, e);

// 发送请求时与目标节点断开连接，直接发起协调器查找请求，然后重试

maybeFindCoordinatorAndRetry(nextRequestHandler);

return true;

}

}

private void maybeFindCoordinatorAndRetry(TransactionManager.TxnRequestHandler nextRequestHandler) {

if (nextRequestHandler.needsCoordinator()) {

// 如果下一个请求处理需要协调器，则发起协调器查找请求

transactionManager.lookupCoordinator(nextRequestHandler);

} else {

// 对于非协调器请求，睡眠一段时间以避免没有可用节点时出现紧密循环

time.sleep(retryBackoffMs);

// 发送元数据更新请求以便获取最新的集群中的节点信息

metadata.requestUpdate();

}

// 对下一个请求的处理进行重试

transactionManager.retry(nextRequestHandler);

}

该方法主要用来**专门负责发送待处理请求中的事务请求的相关操作的**，步骤如下：

1.  如果存在未完成的事务请求，等待其返回。
2.  如果事务管理器存在可中止的错误或者正在中止中，且累加器包含未完成的消息批次，则终止未完成的消息批次。
3.  如果事务管理器正在完成事务且累加器没有正在进行的刷新操作，开始执行刷新操作。
4.  从待处理请求队列中获取下一个 [TxnRequestHandler](http://txnrequesthandler/)，并生成一个 [requestBuilder](http://requestbuilder/)，该 [requestBuilder](http://requestbuilder%20/) 负责生成下一个事务请求。
5.  [nextRequestHandler.coordinatorType()](http://nextrequesthandler.coordinatortype\(\)/) 方法返回下一个事务请求的目标节点类型，这里分 3 种情况：
6.  如果是 [FindCoordinatorRequest.CoordinatorType](http://findcoordinatorrequest.coordinatortype/) 即查找事务协调者，则返回 Null，此时调用 [client.leastLoadedNode(time.milliseconds())](http://client.leastloadednode\(time.milliseconds\(\)\)%20/) 方法查找一个未完成请求最少的 Broker 节点并将其作为目标节点。
7.  如果是 [TxnRequestHandler](http://txnrequesthandler/)，那么该方法会返回 [TRANSCATION](http://transcation/) 类型，则目标节点为事务协调者，即 [TransactionManager#transactionCoordinator](http://transactionmanager/#transactionCoordinator)。
8.  如果是 [txnOffsetCommitHandler](http://txnoffsetcommithandler/) 即提交消费者组偏移量操作，则该方法返回 GROUP 类型，此时目标节点为消费组协调者，即 [TransactionManager#consumerGroupCoordinator](http://transactionmanager/#transactionCoordinator)。
9.  如果目标节点未准备好，或者目标节点为空，则表示需要查找目标节点，此时调用 [maybeFindCoordinatorAndRetry](http://maybefindcoordinatorandretry/) 方法生成一个 FindCoordinatorHandler 放入待处理队列，并将上面获取到的 [TxnRequestHandler](http://txnrequesthandler/) 重新放入待处理请求队列，以便获取目标节点成功后再处理该 handler。
10.  执行到这里，说明已经找到了目标节点，发送请求到目标节点。

综上，可以得出，该方法会完成「**完成查找协调者**」、「**发送事务请求**」等工作。

那么生产者初始化事务时首先需要「**查找协调者**」，其执行步骤如下：

1.  [FindCoordinatorHandler](http://findcoordinatorhandler/) 发送 [FindCoordinator](http://findcoordinator/) 请求给未完成请求最少的 Broker 节点上。
2.  Broker 接收到请求后，调用 [KafkaApis#handleFindCoordinatorRequest](http://kafkaapis/#handleFindCoordinatorRequest) 方法处理该请求。

case ApiKeys.FIND\_COORDINATOR => handleFindCoordinatorRequest(request)

1.  计算该事务再事务状态主题中的存储分区，计算规则为 Hash(transactionId)%partitionNum。源码图如下：

![](https://article-images.zsxq.com/FvIrtKYmsmsfCTnrUcypy8VBV027)

![](https://article-images.zsxq.com/FmDpJmK3mcpjIRHEszb6QYF3VP5K)

![](https://article-images.zsxq.com/FlnB9kWb_vVNHQlzbS1ypgLUkWgt)

1.  通过上图可以得出分区的 Leader 副本，将这个 Leader 副本作为该事务的事务协调者。 ![](https://article-images.zsxq.com/FstyW1mRrv9Nzw413Qj0J02aQdAt)
2.  最后返回事务协调者给生产者。

3\. 此时生产者调用 FindCoordinatorHandler#handleResponse 方法来处理 FindCoordinator 响应，将协调者存储在 TransactionManager#transactionCoordinator，方便后续给协调者发送信息。![](https://article-images.zsxq.com/FmMiAwtlEn45gayUv6pLM3Ac8tjd)

## **3.2.2.2 生产者初始化事务流程**

当「**查找协调者**」完成后，生产者就可以开启初始化了，我们来剖析下。

1、首先，在「**生产者**」中会先调用 [KafkaProducer#initTransactions](http://kafkaproducer/#initTransactions) 来进行初始化。

public void initTransactions() {

// 检查是否存在TransactionManager

throwIfNoTransactionManager();

// 检查Producer是否已关闭，如果关闭则抛出异常

throwIfProducerClosed();

// 1、初始化事务并返回初始化结果

TransactionalRequestResult result \= transactionManager.initializeTransactions();

// 2、唤醒Producer线程

sender.wakeup();

// 3、等待初始化结果，等待时间不超过最大阻塞时间

result.await(maxBlockTimeMs, TimeUnit.MILLISECONDS);

}

我们来继续剖析下「**第一步**」，它是用来给协调者发送 [InitProducerIdRequest](http://initproduceridrequest/) 请求，并将事务 ID 发送给协调者来初始化事务。

  
![](https://article-images.zsxq.com/FrGg1YNNER5dKPTe7ncOYG8c2fYz)

2、接着，当「**协调者**」收到「**InitProducerIdRequest**」请求后，会调用 [TransactionCoordinator#handleInitProducerId](http://transactioncoordinator/#handleInitProducerId) 来处理该请求。

def handleInitProducerId(transactionalId: String,

transactionTimeoutMs: Int,

expectedProducerIdAndEpoch: Option\[ProducerIdAndEpoch\],

responseCallback: InitProducerIdCallback): Unit = {

// 如果 transactionalId 为 null，生成一个新的 producerId，并返回结果给客户端

if (transactionalId == null) {

val producerId \= producerIdGenerator.generateProducerId()

responseCallback(InitProducerIdResult(producerId, producerEpoch = 0, Errors.NONE))

}

// 如果 transactionalId 为空字符串，返回 INVALID\_REQUEST 错误给客户端

else if (transactionalId.isEmpty) {

responseCallback(initTransactionError(Errors.INVALID\_REQUEST))

}

// 检查 transactionTimeoutMs 是否有效

else if (!txnManager.validateTransactionTimeoutMs(transactionTimeoutMs)) {

responseCallback(initTransactionError(Errors.INVALID\_TRANSACTION\_TIMEOUT))

}

// 处理正常情况

else {

// 1、获取 transactionalId 对应的协调者状态和元数据

val coordinatorEpochAndMetadata \= txnManager.getTransactionState(transactionalId).flatMap {

case None \=\>

// 2、如果不存在该 transactionalId 对应的状态和元数据，则生成一个新的 producerId

val producerId \= producerIdGenerator.generateProducerId()

// 3、构建事务元数据

val createdMetadata \= new TransactionMetadata(transactionalId = transactionalId,

producerId = producerId,

lastProducerId = RecordBatch.NO\_PRODUCER\_ID,

producerEpoch = RecordBatch.NO\_PRODUCER\_EPOCH,

lastProducerEpoch = RecordBatch.NO\_PRODUCER\_EPOCH,

txnTimeoutMs = transactionTimeoutMs,

state = Empty,

topicPartitions = collection.mutable.Set.empty\[TopicPartition\],

txnLastUpdateTimestamp = time.milliseconds())

// 添加新的状态和元数据到事务管理器

txnManager.putTransactionStateIfNotExists(createdMetadata)

case Some(epochAndTxnMetadata) => Right(epochAndTxnMetadata)

}

// 准备 InitProducerId 操作，并返回结果

val result: ApiResult\[(Int, TxnTransitMetadata)\] = coordinatorEpochAndMetadata.flatMap {

existingEpochAndMetadata =>

val coordinatorEpoch \= existingEpochAndMetadata.coordinatorEpoch

val txnMetadata \= existingEpochAndMetadata.transactionMetadata

txnMetadata.inLock {

prepareInitProducerIdTransit(transactionalId, transactionTimeoutMs, coordinatorEpoch, txnMetadata, expectedProducerIdAndEpoch)

}

}

// 4、处理 InitProducerId 的结果

result match {

case Left(error) =>

responseCallback(initTransactionError(error))

case Right((coordinatorEpoch, newMetadata)) =>

// 如果新的 metadata 的状态是 PrepareEpochFence，终止正在进行中的 transaction

if (newMetadata.txnState == PrepareEpochFence) {

// 终止 transaction，并返回 CONCURRENT\_TRANSACTIONS 给客户端

def sendRetriableErrorCallback(error: Errors): Unit = {

if (error != Errors.NONE) {

responseCallback(initTransactionError(error))

} else {

responseCallback(initTransactionError(Errors.CONCURRENT\_TRANSACTIONS))

}

}

endTransaction(transactionalId,

newMetadata.producerId,

newMetadata.producerEpoch,

TransactionResult.ABORT,

isFromClient = false,

sendRetriableErrorCallback)

}

// 如果新的 metadata 的状态不是 PrepareEpochFence，将 metadata 添加到事务日志中，并返回结果给客户端

else {

def sendPidResponseCallback(error: Errors): Unit = {

if (error == Errors.NONE) {

info(s"Initialized transactionalId $transactionalId with producerId ${newMetadata.producerId} and producer " + s"epoch ${newMetadata.producerEpoch} on partition " + s"${Topic.TRANSACTION\_STATE\_TOPIC\_NAME}-${txnManager.partitionFor(transactionalId)}")

responseCallback(initTransactionMetadata(newMetadata))

} else {

info(s"Returning $error error code to client for $transactionalId's InitProducerId request")

responseCallback(initTransactionError(error))

}

}

// 将 metadata 添加到事务日志中

txnManager.appendTransactionToLog(transactionalId, coordinatorEpoch, newMetadata, sendPidResponseCallback)

}

}

}

}

这里简单总结下其步骤：

1.  生成 [producerId](http://producerid/)、[producerEpoch](http://producerepoch%20/) 信息。
2.  创建事务元数据 [TransactionMetadata](http://transactionmetadata%20/) 实例并存储到事务元数据集中。
3.  最后返回 [producerId](http://producerid/)、[producerEpoch](http://producerepoch%20/) 信息给生产者，并初始化协调者事务状态为 [Empty](http://empty/)。

3、最后，当「**生产者**」收到「**协调者**」返回响应后，就会调用 [InitProducerIdHandler#handleReponse](http://initproduceridhandler/#handleReponse) 处理响应，如下图：

  
![](https://article-images.zsxq.com/FkhMnJ6uT3lmd10sIltQm15vahvD)

至此，「**生产者**」和 「**Broker 端协调者**」都已经初始化完成了。

> 注意：这里协调者保证 ProducerId 是一个唯一的递增整数，它是通过 Zookeeper 来生成的 ProducerId，并且每次会申请 1000 个 id，再将最新的 ProducerId 信息写回到 ZK 节点 “/lastest\_producer\_id\_block”

  
![](https://article-images.zsxq.com/Fkw0_Fd82l89v7ki57AiAGXG24ht)

## **3.2.2.3 生产者启动事务**

当 「**生产者**」和 「**Broker 端协调者**」初始化完成后，此时生产者会调用 [KafkaProducer#beginTransaction](http://kafkaproducer/#beginTransaction) 方法来启动事务。

/\*\*

\* Should be called before the start of each new transaction. Note that prior to the first invocation

\* of this method, you must invoke {@link #initTransactions()} exactly one time.

\*

\* @throws IllegalStateException if no transactional.id has been configured or if {@link #initTransactions()}

\* has not yet been invoked

\* @throws ProducerFencedException if another producer with the same transactional.id is active

\* @throws org.apache.kafka.common.errors.InvalidProducerEpochException if the producer has attempted to produce with an old epoch

\* to the partition leader. See the exception for more details

\* @throws org.apache.kafka.common.errors.UnsupportedVersionException fatal error indicating the broker

\* does not support transactions (i.e. if its version is lower than 0.11.0.0)

\* @throws org.apache.kafka.common.errors.AuthorizationException fatal error indicating that the configured

\* transactional.id is not authorized. See the exception for more details

\* @throws KafkaException if the producer has encountered a previous fatal error or for any other unexpected error

\*/

public void beginTransaction() throws ProducerFencedException {

throwIfNoTransactionManager();

throwIfProducerClosed();

// 调用事务管理器来启动事务

transactionManager.beginTransaction();

}

接着我们再来剖析下 [transactionManager.beginTransaction](http://transactionmanager.begintransaction/) 的方法都做了什么。

  
![](https://article-images.zsxq.com/FiseF-7rMETOJjIjIbCdZcIhA0NM)

很简单，它就是将生产者事务状态变更为 [State.IN\_TRANSACTION](http://state.in_transaction/)，此时生产者就可以发送事务消息了。

至此整个「**事务机制**」的初始化流程就带你剖析完成了。

## **04 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头从「**转账**」业务剖析引出了「**事务机制** 」，保证操作要么执行成功、要么都不执行。

2、接着带大家剖析了「**Kafka 事务机制**」提供了哪些保证， 了解了「**生产者幂等发送机制**」、「**Transaction ID 机制**」、「**事务协调者**」、「**事务存储**」、「**事务存储**」、「**协调者 failover**」等设计思想。

3、接着带大家深度剖析了「**事务初始化处理流程**」，让你了解了一个事务消息是如何被初始化的。

下篇我们来深度剖析「**事务消息发送以及提交处理流程**」，大家期待，我们下期见。