大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了「**号称承载 Kafka 客户端消息快递仓库 RecordAccmulator 的架构设计**」，消息被暂存到累加器中，今天主要聊聊 「**发送网络 I/O 的 Sender 线程的架构设计**」，深度剖析下消息是如何被发送出去的。

![](https://article-images.zsxq.com/FgUAVz-L42sBkbHpylVdr6AHOkMQ)

## **01 总体概述**

通过「**场景驱动**」的方式，来看看消息是如何从客户端发送出去的。

在上篇中，我们知道了消息被**暂存到 Deque<ProducerBatch> 的 batches 中**，等「**批次已满**」或者「**有新批次被创建**」后，唤醒 Sender 子线程将消息批量发送给 Kafka Broker 端。

  
![](https://article-images.zsxq.com/FnYieCOY-Ci_78lZvR6pWGADdyMz)

接下来我们就来看看，「**Sender 线程的架构实现以及发送处理流程**」，为了方便大家理解，所有的源码只保留骨干。

## **02 Sender 线程架构设计**

在 [图解 Kafka 源码之生产者初始化核心流程](https://t.zsxq.com/0bfMHL2pk) [](https://t.zsxq.com/0bfMHL2pk)这篇中我们知道 KafkaProducer 会启动一个后台守护进程，其线程名称：kafka-producer-network-thread + "|" + clientId。

在 [KafkaProducer.java](http://kafkaproducer.java/) 类有常量定义：NETWORK\_THREAD\_PREFIX，并启动 **守护线程 KafkaThread 即 ioThread，**如果不主动关闭 Sender 线程会一直执行下去。

github 源码地址如下：

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java)

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/internals/ProducerBatch.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/internals/ProducerBatch.java)

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/RequestCompletionHandler.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/RequestCompletionHandler.java)

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/KafkaProducer.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/KafkaProducer.java)

public class KafkaProducer<K, V> implements Producer<K, V> {

public static final String NETWORK\_THREAD\_PREFIX \= "kafka-producer-network-thread";

// visible for testing

@SuppressWarnings("unchecked")

KafkaProducer(Map<String, Object> configs,Serializer<K> keySerializer,

Serializer<V> valueSerializer,ProducerMetadata metadata,

KafkaClient kafkaClient,ProducerInterceptors<K, V> interceptors,Time time) {

try {

...

this.sender = newSender(logContext, kafkaClient, this.metadata);

String ioThreadName \= NETWORK\_THREAD\_PREFIX + " | " + clientId;

this.ioThread = new KafkaThread(ioThreadName, this.sender, true);

this.ioThread.start();

...

log.debug("Kafka producer started");

} catch (Throwable t) {

...

}

}

}

从上面得出 Sender 类是一个线程类， 我们来看看 **Sender 线程的重要字段和方法，并讲解其是如何发送消息和处理消息响应的**。

## **02.1 关键字段**

/\*\*

\* The background thread that handles the sending of produce requests to the Kafka cluster. This thread makes metadata

\* requests to renew its view of the cluster and then sends produce requests to the appropriate nodes.

\*/

public class Sender implements Runnable {

/\* the state of each nodes connection \*/

private final KafkaClient client; // 为 Sender 线程提供管理网络连接进行网络读写

/\* the record accumulator that batches records \*/

private final RecordAccumulator accumulator; // 消息仓库累加器

/\* the metadata for the client \*/

private final ProducerMetadata metadata; // 生产者元数据

/\* the flag indicating whether the producer should guarantee the message order on the broker or not. \*/

private final boolean guaranteeMessageOrder; // 是否保证消息在 broker 端的顺序性

/\* the maximum request size to attempt to send to the server \*/

private final int maxRequestSize; //发送消息最大字节数。

/\* the number of acknowledgements to request from the server \*/

private final short acks; // 生产者的消息发送确认机制

/\* the number of times to retry a failed request before giving up \*/

private final int retries; // 发送失败后的重试次数，默认为0次

/\* true while the sender thread is still running \*/

private volatile boolean running; // Sender 线程是否还在运行中

/\* true when the caller wants to ignore all unsent/inflight messages and force close. \*/

private volatile boolean forceClose; // 是否强制关闭，此时会忽略正在发送中的消息。

/\* the max time to wait for the server to respond to the request\*/

private final int requestTimeoutMs; // 等待服务端响应的最大时间,默认30s

/\* The max time to wait before retrying a request which has failed \*/

private final long retryBackoffMs; // 失败重试退避时间

/\* current request API versions supported by the known brokers \*/

private final ApiVersions apiVersions; // 所有 node 支持的 api 版本

/\* all the state related to transactions, in particular the producer id, producer epoch, and sequence numbers \*/

private final TransactionManager transactionManager; // 事务管理，这里忽略 后续会有专门一篇讲解事务相关的

// A per-partition queue of batches ordered by creation time for tracking the in-flight batches

private final Map<TopicPartition, List<ProducerBatch>> inFlightBatches; // 正在执行发送相关的消息批次集合， key为分区，value是 list<ProducerBatch> 。

从该类属性字段来看比较多，这里说几个关键字段：

1.  **client**：KafkaClient 类型，是一个接口类，[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/KafkaClient.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/KafkaClient.java) Sender 线程主要用它来实现真正的网络I/O，即 NetworkClient。该字段主要为 Sender 线程提供了网络连接管理和网络读写操作能力。
2.  **accumulator**：RecordAccumulator类型，上一篇的内容 [图解 Kafka 源码之快递仓库 RecordAccumulator 架构](https://t.zsxq.com/0bhs5MZwd)设计，Sender 线程用它获取待发送的 node 节点及批次消息等能力。
3.  **metadata**：ProducerMetadata类型，生产者元数据。因为发送消息时要知道分区 Leader 在哪些节点，以及节点的地址、主题分区的情况等。可以看下之前的文章：[图解 Kafka 源码之生产者元数据拉取管理全流程](https://t.zsxq.com/0b3pq62D6) [](https://t.zsxq.com/0bfMHL2pk)。
4.  **guaranteeMessageOrder**：是否保证消息在 broker 端的顺序性，参数： [max.in.flight.requests.per.connection](http://max.in.flight.requests.per.connection=1/)。
5.  **maxRequestSize**：单个请求发送消息最大字节数，默认为1M，它限制了生产者在单个请求发送的记录数，以避免发送大量请求。
6.  **acks**：生产者的消息发送确认机制。有3个可选值：0，1，-1/all。![](https://article-images.zsxq.com/Fs2_HTCR5Rogt1HSWlzbEWOTptoi)
7.  **retries** ：生产者发送失败后的重试次数。默认是0次。
8.  **running**：Sender线程是否还在运行中。
9.  **forceClose**: 是否强制关闭，此时会忽略正在发送中的消息。
10.  **requestTimeoutMs**：生产者发送请求后等待服务端响应的最大时间。如果超时了且配置了重试次数，会再次发送请求，待重试次数用完后在这个时间范围内返回响应则认为请求最终失败，默认 30 秒。
11.  **retryBackoffMs**：生产者在发送请求失败后可能会重新发送失败的请求，其目的就是防止重发过快造成服务端压力过大。默认100 ms。
12.  **apiVersions**：ApicVersions类对象，保存了每个node所支持的api版本。
13.  **inFlightBatches**：正在执行发送相关的消息批次集合， key为分区，value是 list<ProducerBatch>。

## **02.2 关键方法**

Sender 类的方法也不少，这里针对关键方法逐一讲解下。

### **02.2.1 run()**

Sender 线程实现了 Runnable 接口，会不断的调用 runOnce()，这是一个典型的循环事件机制。

/\*\*

\* The main run loop for the sender thread

\*/

@Override

public void run() {

log.debug("Starting Kafka producer I/O thread.");

// 这里使用 volatile boolean 类型的变量 running，判断 Sender 线程是否在运行状态中。

// 1. 如果 Sender 线程在运行状态即 running=true，则一直循环调用 runOnce() 方法。

while (running) {

try {

// 将缓冲区的消息发送到 broker。

runOnce();

} catch (Exception e) {

log.error("Uncaught error in kafka producer I/O thread: ", e);

}

}

log.debug("Beginning shutdown of Kafka producer I/O thread, sending remaining records.");

// 2. 如果（没有强制关闭 && ((消息累加器中还有剩余消息待发送 || 还有等待未响应的消息 ) || 还有事务请求未完成))，则继续发送剩下的消息。

while (!forceClose && ((this.accumulator.hasUndrained() || this.client.inFlightRequestCount() > 0) || hasPendingTransactionalRequests())) {

try {

// 继续执行将剩余的消息发送完毕

runOnce();

} catch (Exception e) {

log.error("Uncaught error in kafka producer I/O thread: ", e);

}

}

// 3. 对进行中的事务进行中断，则继续发送剩下的消息。

while (!forceClose && transactionManager != null && transactionManager.hasOngoingTransaction()) {

if (!transactionManager.isCompleting()) {

log.info("Aborting incomplete transaction due to shutdown");

transactionManager.beginAbort();

}

try {

runOnce();

} catch (Exception e) {

log.error("Uncaught error in kafka producer I/O thread: ", e);

}

}

// 4. 如果强制关闭，则关闭事务管理器、终止消息的追加并清空未完成的批次

if (forceClose) {

if (transactionManager != null) {

log.debug("Aborting incomplete transactional requests due to forced shutdown");

// 关闭事务管理器

transactionManager.close();

}

log.debug("Aborting incomplete batches due to forced shutdown");

// 终止消息的追加并清空未完成的批次

this.accumulator.abortIncompleteBatches();

}

// 5. 关闭 NetworkClient

try {

this.client.close();

} catch (Exception e) {

log.error("Failed to close network client", e);

}

log.debug("Shutdown of Kafka producer I/O thread has completed.");

}

当 Sender 线程启动后会直接运行 run() 方法，**该方法有以下接种情况会一直循环调用去发送消息到 Broker**。

1.  如果 Sender 线程在运行状态即 running = true，则一直循环调用 runOnce() 方法。
2.  如果满足以下几个条件，则一直循环调用 runOnce() 方法继续发送剩下的消息。
3.  **forceClose****：** 用于标识是否强制关闭。
4.  **this.accumulator.hasUndrained():** 检查消息累加器批次队列中是否还有剩余消息待发送。
5.  **this.client.inFlighRequestCount()>0:** 检查是否还有等待未响应的消息。
6.  **runhasPendingTranscriptionalRequest():** 检查是否有未完成的事务请求。
7.  对进行中的事务进行中断，则一直循环调用 runOnce() 方法继续发送剩下的消息。
8.  如果强制关闭，则关闭事务管理器、终止消息的追加并清空未完成的批次。
9.  最后关闭 NetworkClient。

### **02.2.2 runOnce()**

我们来看看这个执行业务处理的方法，关于事务的部分后续专门文章讲解。

/\*\*

\* Run a single iteration of sending

\*/

void runOnce() {

if (transactionManager != null) {

... //事务处理方法 后续文章专门讲解

}

// 1. 获取当前时间的时间戳。

long currentTimeMs \= time.milliseconds();

// 2. 调用 sendProducerData 发送消息,但并非真正的发送，而是把消息缓存在 把消息缓存在 KafkaChannel 的 Send 字段里。下一篇会讲解 NetworkClient。

long pollTimeout \= sendProducerData(currentTimeMs);

// 3. 读取消息实现真正的网络发送

client.poll(pollTimeout, currentTimeMs);

}

该方法比较简单，主要做了3件事情：

1.  获取当前时间的时间戳。
2.  调用 sendProducerData 发送消息,但并非真正的发送，而是把消息缓存在 NetworkClient 的 Send 字段里。下一篇会讲解 NetworkClient。
3.  读取 NetworkClient 的 send 字段消息实现真正的网络发送。

### **02.2.3 sendProducerData()**

该方法主要是获取已经准备好的节点上的批次数据并过滤过期的批次集合，最后暂存消息。

private long sendProducerData(long now) {

// 1. 获取元数据

Cluster cluster \= metadata.fetch();

// get the list of partitions with data ready to send

// 2. 获取已经准备好的节点以及找不到 Leader 分区对应的节点的主题

RecordAccumulator.ReadyCheckResult result \= this.accumulator.ready(cluster, now);

// if there are any partitions whose leaders are not known yet, force metadata update

// 3. 如果主题 Leader 分区对应的节点不存在，则强制更新元数据

if (!result.unknownLeaderTopics.isEmpty()) {

// 添加 topic 到没有拉取到元数据的 topic 集合中，并标识需要更新元数据

for (String topic : result.unknownLeaderTopics)

this.metadata.add(topic, now);

...

// 针对这个 topic 集合进行元数据更新

this.metadata.requestUpdate();

}

// 4. 循环 readyNodes 并检查客户端与要发送节点的网络是否已经建立好了

Iterator<Node> iter = result.readyNodes.iterator();

long notReadyTimeout \= Long.MAX\_VALUE;

while (iter.hasNext()) {

Node node \= iter.next();

// 检查客户端与要发送节点的网络是否已经建立好了

if (!this.client.ready(node, now)) {

// 移除对应节点

iter.remove();

notReadyTimeout = Math.min(notReadyTimeout, this.client.pollDelayMs(node, now));

}

}

// create produce requests

// 5. 获取上面返回的已经准备好的节点上要发送的 ProducerBatch 集合

Map<Integer, List<ProducerBatch>> batches = this.accumulator.drain(cluster, result.readyNodes, this.maxRequestSize, now);

// 6. 将从消息累加器中读取的数据集，放入正在执行发送相关的消息批次集合中

addToInflightBatches(batches);

// 7. 要保证消息的顺序性，即参数 max.in.flight.requests.per.connection=1

if (guaranteeMessageOrder) {

// Mute all the partitions drained

for (List<ProducerBatch> batchList : batches.values()) {

for (ProducerBatch batch : batchList)

// 对 tp 进行 mute，保证只有一个 batch 正在发送

this.accumulator.mutePartition(batch.topicPartition);

}

}

// 重置下一批次的过期时间

accumulator.resetNextBatchExpiryTime();

// 8. 从正在执行发送数据集合 inflightBatches 中获取过期集合

List<ProducerBatch> expiredInflightBatches = getExpiredInflightBatches(now);

// 9. 从 batches 中获取过期集合

List<ProducerBatch> expiredBatches = this.accumulator.expiredBatches(now);

// 10. 从 inflightBatches 与 batches 中查找已过期的消息批次(ProducerBatch)，判断批次是否过 期是根据系统当前时间与 ProducerBatch 创建时间之差是否超过120s，过期时间可以通过参数 delivery.timeout.ms 设置。

expiredBatches.addAll(expiredInflightBatches);

// 如果过期批次不为空 则输出对应日志

if (!expiredBatches.isEmpty())

log.trace("Expired {} batches in accumulator", expiredBatches.size());

// 11. 处理已过期的消息批次，通知该批消息发送失败并返回给客户端

for (ProducerBatch expiredBatch : expiredBatches) {

// 处理当前过期ProducerBatch的回调结果 ProduceRequestResult,并且设置超时异常 new TimeoutException(errorMessage)

String errorMessage \= "Expiring " + expiredBatch.recordCount + " record(s) for " + expiredBatch.topicPartition

\+ ":" + (now - expiredBatch.createdMs) + " ms has passed since batch creation";

// 通知该批消息发送失败并返回给客户端

failBatch(expiredBatch, -1, NO\_TIMESTAMP, new TimeoutException(errorMessage), false);

// ... 事务管理器的处理忽略

}

// 收集统计指标，后续会专门对 Kafka 的 Metrics 进行分析

sensors.updateProduceRequestMetrics(batches);

// 设置下一次的发送延时

long pollTimeout \= Math.min(result.nextReadyCheckDelayMs, notReadyTimeout);

pollTimeout = Math.min(pollTimeout, this.accumulator.nextExpiryTimeMs() - now);

pollTimeout = Math.max(pollTimeout, 0);

if (!result.readyNodes.isEmpty()) {

log.trace("Nodes with data ready to send: {}", result.readyNodes);

pollTimeout = 0;

}

// 12. 发送消息暂存到 NetworkClient send 字段里。

sendProduceRequests(batches, now);

return pollTimeout;

}

该方法主要做了12件事情，逐一说明下：

1.  首先获取元数据，这里主要是根据元数据的更新机制来保证数据的准确性。
2.  获取已经准备好的节点。accumulator#reay() 方法会通过发送记录对应的节点和元数据进行比较，返回结果中包括两个重要的集合:「**准备好发送的节点集合 readyNodes**」、「**找不到 Leader 分区对应节点的主题 unKnownLeaderTopic**」。
3.  如果主题 Leader 分区对应的节点不存在，则强制更新元数据。
4.  循环 readyNodes 并检查客户端与要发送节点的网络是否已经建立好了。在 NetworkClient 中维护了客户端与所有节点的连接，这样就可以通过连接的状态判断是否连接正常。
5.  获取上面返回的已经准备好的节点上要发送的 ProducerBatch 集合。accumulator#drain() 方法就是将 「**TopicPartition**」-> 「**ProducerBatch 集合**」的映射关系转换成 「**Node 节点**」->「**ProducerBatch 集合**」的映射关系，如下图所示，这样的话**按照节点方式只需要2次就完成，大大减少网络的开销**。![](https://article-images.zsxq.com/FkrA8D87tUbVOC-vY-kput7Z5GQD)
6.  将从消息累加器中读取的数据集，放入正在执行发送相关的消息批次集合中。
7.  要保证消息的顺序性，即参数 [max.in.flight.requests.per.connection=1](http://max.in.flight.requests.per.connection=1/)，会添加到 **muted 集合**，保证只有一个 batch 在发送。
8.  从正在执行发送数据集合 inflightBatches 中获取过期集合。
9.  从 accumulator 累加器的 batches 中获取过期集合，可以看上一篇的对应内容。
10.  从 inflightBatches 与 batches 中查找已过期的消息批次(ProducerBatch)，判断批次是否过期是根据系统当前时间与 ProducerBatch 创建时间之差是否超过120s，过期时间可以通过参数 [delivery.timeout.ms](http://delivery.timeout.ms/) 设置。
11.  处理已过期的消息批次，通知该批消息发送失败并返回给客户端。
12.  发送消息暂存到 NetworkClient send 字段里。

接下来会挨个讲解 **sendProducerData 方法** 中调用到的 Sender 线程类中的方法。

### **02.2.4 addToInflightBatches()**

public void addToInflightBatches(Map<Integer, List<ProducerBatch>> batches) {

// 循环批次集合数据

for (List<ProducerBatch> batchList : batches.values()) {

// 挨个将批次添加到正在执行发送批次数据集合中

addToInflightBatches(batchList);

}

}

private void addToInflightBatches(List<ProducerBatch> batches) {

for (ProducerBatch batch : batches) {

List<ProducerBatch> inflightBatchList = inFlightBatches.get(batch.topicPartition);

if (inflightBatchList == null) {

inflightBatchList = new ArrayList<>();

inFlightBatches.put(batch.topicPartition, inflightBatchList);

}

inflightBatchList.add(batch);

}

}

该方法主要是将从消息累加器中获取到的 list<ProducerBatch> 加入到 inFlightBatches 中，该属性是按照创建时间排序的每个分区的批次队列，用来存储正在执行发送的批次数据。

对应的参数是 [max.in.flight.requests.per.connection](http://max.in.flight.requests.per.connection/)，用来控制积压的最大数量。

### **02.2.5 getExpiredInflightBatches()**

/\*\*

\* Get the in-flight batches that has reached delivery timeout.

\*/

private List<ProducerBatch> getExpiredInflightBatches(long now) {

// 1.创建过期批次集合

List<ProducerBatch> expiredBatches = new ArrayList<>();

// 2.遍历 inFlightBatches

for (Iterator<Map.Entry<TopicPartition, List<ProducerBatch>>> batchIt = inFlightBatches.entrySet().iterator(); batchIt.hasNext();) {

// 获取每个分区对应的 List<ProducerBatch> 集合

Map.Entry<TopicPartition, List<ProducerBatch>> entry = batchIt.next();

// 获取指定分区正在发送的批次数据

List<ProducerBatch> partitionInFlightBatches = entry.getValue();

// 不为空说明存在正在发送的批次数据

if (partitionInFlightBatches != null) {

Iterator<ProducerBatch> iter = partitionInFlightBatches.iterator();

// 3. 遍历所有的批次

while (iter.hasNext()) {

ProducerBatch batch \= iter.next();

// 4.判断 batch 是否投递超时。默认消息投递过期时间是120s

// 超时的标准：now - createMs > deliveryTimeoutMS

// 当前时间 - 批次的创建时间 > 投递过期时间 (默认120秒)

if (batch.hasReachedDeliveryTimeout(accumulator.getDeliveryTimeoutMs(), now)) {

// 如果该批次已经超时了，就从 inFlightBatches 集合中移除

iter.remove();

// 如果 done 方法还未执行，即还未标记该批次是成功还是失败

if (!batch.isDone()) {

// 添加到超时批次到集合中

expiredBatches.add(batch);

} else {

throw new IllegalStateException(...);

}

} else {

// 如果没有超时，则更新下一个 batch 的超时的具体时间

accumulator.maybeUpdateNextBatchExpiryTime(batch);

break;

}

}

if (partitionInFlightBatches.isEmpty()) {

batchIt.remove();

}

}

}

return expiredBatches;

}

// ProducerBatch.java 判断投递是否超时

boolean hasReachedDeliveryTimeout(long deliveryTimeoutMs, long now) {

// 超时的标准：now - createMs > deliveryTimeoutMS

// 当前时间 - 批次的创建时间 > 投递过期时间(默认120秒)

return deliveryTimeoutMs <= now - this.createdMs;

}

该方法主要用来从 inFlightBatches 集合中获取过期的批次集合，结构如下图所示：

![](https://article-images.zsxq.com/Fk0QU3qd6bH0JGTKfOBZ5vhV-wi8)

主要分为五个步骤：

1.  创建过期批次集合。
2.  遍历 inFlightBatches 。
3.  遍历指定分区正在发送的批次数据中的所有批次
4.  判断批次是否超时调用的是 **ProducerBatch#hasReachedDeliveryTimeout** 方法。**如果当前时间-批次的创建时间> deliveryTimeoutMs，则认为超时**。deliveryTimeoutMs的默认值为120秒。
5.  如果批次超时，则从 inFlightBatches 中将该批次移除，同时将该批次放入 expiredBatches 集合，然后如果 done 方法还未执行，即还未标记该批次是成功还是失败，则将批次添加到超时批次到集合中 。
6.  如果没有超时，则更新下一个 batch 的超时的具体时间 nextBatchExpiryTimeMs。

5\. 最后返回 expiredBatches 集合。

### **02.2.7 failBatch()**

private void failBatch(ProducerBatch batch,

ProduceResponse.PartitionResponse response,

RuntimeException exception,

boolean adjustSequenceNumbers) {

failBatch(batch, response.baseOffset, response.logAppendTime, exception, adjustSequenceNumbers);

}

private void failBatch(ProducerBatch batch,

long baseOffset,long logAppendTime,RuntimeException exception,

boolean adjustSequenceNumbers) {

// ...事务管理器的处理忽略

// 收集指标错误信息

this.sensors.recordErrors(batch.topicPartition.topic(), batch.recordCount);

if (batch.done(baseOffset, logAppendTime, exception)) {

// 从 inflightBatches 中删除批次并释放缓存批次空间

maybeRemoveAndDeallocateBatch(batch);

}

}

private void maybeRemoveAndDeallocateBatch(ProducerBatch batch) {

//从 inflightBatches 中删除批次

maybeRemoveFromInflightBatches(batch);

// 释放累加器批次空间

this.accumulator.deallocate(batch);

}

private void maybeRemoveFromInflightBatches(ProducerBatch batch) {

// 根据 batch.tp 从 inFlightBatches 集合中获取匹配到的 batches

List<ProducerBatch> batches = inFlightBatches.get(batch.topicPartition);

// 假如 batches 不为空

if (batches != null) {

// 则将对应 batch 从 batches 中移除掉

batches.remove(batch);

// 移除完后批次为空的话

if (batches.isEmpty()) {

// 则将其从 inFlightBatches 集合中删除

inFlightBatches.remove(batch.topicPartition);

}

}

}

该方法主要是对过期批次的处理，最关键的代码就是调用了 **batch.done(...)** 。判断批次 Batch 最终状态是否可以执行回调操作，执行成功后删除批次并释放批次所占用的空间。

### **02.2.8 sendProduceRequests()**

private void sendProduceRequests(Map<Integer, List<ProducerBatch>> collated, long now) {

for (Map.Entry<Integer, List<ProducerBatch>> entry : collated.entrySet())

sendProduceRequest(now, entry.getKey(), acks, requestTimeoutMs, entry.getValue());

}

该方法通过轮询已过滤的批次集合，调用 sendProduceRequest() 。

参数 batches 的结构是 Map<Integer,List<ProducerBatch\>\> ，即 key 是 「**nodeId**」brokerId， value 是每个节点待发送的数据集。

因此会按照 「**nodeId**」brokerId 方式来构建发送请求，即每一个 broker 会将多个 ProducerBatch 一起封装成一个请求进行发送，同一时间，每一个与 broker 连接**只能发送一个请求**。**.**

注意，这里只是构建一个请求，并最终会通过 NetworkClient#send 方法，将该批数据设置到 NetworkClient 的待发送数据中，此时并没有触发真正的网络调用。

接下来我们看看 sendProduceRequest() 的实现。

/\*\*

\* Create a produce request from the given record batches

\*/

private void sendProduceRequest(long now, int destination, short acks, int timeout, List<ProducerBatch> batches) {

if (batches.isEmpty())

return;

// 1. 初始化2个集合，produceRecordsByPartition 用于构建发送请求

Map<TopicPartition, MemoryRecords> produceRecordsByPartition = new HashMap<>(batches.size());

// recordsByPartition 用于构建回调方法, 这里不仅仅有 MemoryRecords，还有回调函数等等

final Map<TopicPartition, ProducerBatch> recordsByPartition = new HashMap<>(batches.size());

// find the minimum magic version used when creating the record sets

byte minUsedMagic \= apiVersions.maxUsableProduceMagic();

for (ProducerBatch batch : batches) {

if (batch.magic() < minUsedMagic)

minUsedMagic = batch.magic();

}

// 2. 按分区方式填充 produceRecordsByPartition 和 recordsByPartition 这两个集合。

for (ProducerBatch batch : batches) {

// 批次对应的主题分区

TopicPartition tp \= batch.topicPartition;

// 从 recordsBuilder 中取出 MemoryRecords

MemoryRecords records \= batch.records();

if (!records.hasMatchingMagic(minUsedMagic))

records = batch.records().downConvert(minUsedMagic, 0, time).records();

produceRecordsByPartition.put(tp, records);

recordsByPartition.put(tp, batch);

}

....

// 3. 创建 requestBuilder 对象

ProduceRequest.Builder requestBuilder \= ProduceRequest.Builder.forMagic(minUsedMagic, acks, timeout,produceRecordsByPartition, transactionalId);

// 4. 创建回调

RequestCompletionHandler callback \= response -> handleProduceResponse(response, recordsByPartition, time.milliseconds());

String nodeId \= Integer.toString(destination);

// 5. 创建 clientRequest

ClientRequest clientRequest \= client.newClientRequest(nodeId, requestBuilder, now, acks != 0,requestTimeoutMs, callback);

// 6. 把 clientRequest 发送给 NetworkClient，完成消息的预发送

client.send(clientRequest, now);

log.trace("Sent produce request to {}: {}", nodeId, requestBuilder);

}

该方法主要是把 ProducerBatch 类型转换成 ClientRequest，并把 clientRequest 放到 NetworkClient 的缓存里，分为以下六个步骤：

1.  初始化2个集合，**produceRecordsByPartition** 用于构建发送请求，**recordsByPartition** 用于构建回调方法。
2.  按分区方式填充 **produceRecordsByPartition** 和 **recordsByPartition** 这两个集合。
3.  创建 requestBuilder 对象。
4.  创建 callback 回调对象， 它其实是实例化回调接口 RequestCompletionHandler， 如下代码：

// RequestCompletionHandler.java

public interface RequestCompletionHandler {

public void onComplete(ClientResponse response);

}

// 接口 RequestCompletionHandler 就一个方法 onComplete()，上面 callback 对 onComplete() 的实现主要是用返回值 response 当参数调用 handleProduceResponse()方法。消息发送的时候并不会把 callback 方法发送到 Broker 端，而是把 callback 放在 NetworkClient 里等待响应回来后根据响应去调用 callback。

1.  创建clientRequest。用 **produceRecordsByPartition** 集合构建 **clientRequest**，也就是说一个 **clientRequest** 对象里有多个消息批次。
2.  把clientRequest交给NetworkClient。 下一篇会讲解 NetworkClient。

至此，消息的预发送已经全部介绍完毕。

最后再讲解一下 **收到响应后 Sender 线程是如何处理的？**

**这里主要涉及 3 个方法：**

### **02.2.9 handleProduceResponse()**

/\*\*

\* Handle a produce response

\*/

private void handleProduceResponse(ClientResponse response, Map<TopicPartition, ProducerBatch> batches, long now) {

RequestHeader requestHeader \= response.requestHeader();

int correlationId \= requestHeader.correlationId();

// 连接失败的异常处理

if (response.wasDisconnected()) {

log.trace("Cancelled request with header {} due to node {} being disconnected",

requestHeader, response.destination());

for (ProducerBatch batch : batches.values())

//

completeBatch(batch, new ProduceResponse.PartitionResponse(Errors.NETWORK\_EXCEPTION), correlationId, now);

} else if (response.versionMismatch() != null) {

// 版本不匹配的异常处理

log.warn("Cancelled request {} due to a version mismatch with node {}",

response, response.destination(), response.versionMismatch());

for (ProducerBatch batch : batches.values())

completeBatch(batch, new ProduceResponse.PartitionResponse(Errors.UNSUPPORTED\_VERSION), correlationId, now);

} else {

// 正常请求处理

log.trace("Received produce response from node {} with correlation id {}", response.destination(), correlationId);

// if we have a response, parse it

// 当请求有响应结果

if (response.hasResponse()) {

ProduceResponse produceResponse \= (ProduceResponse) response.responseBody();

for (Map.Entry<TopicPartition, ProduceResponse.PartitionResponse> entry : produceResponse.responses().entrySet()) {

TopicPartition tp \= entry.getKey();

ProduceResponse.PartitionResponse partResp \= entry.getValue();

ProducerBatch batch \= batches.get(tp);

completeBatch(batch, partResp, correlationId, now);

}

this.sensors.recordLatency(response.destination(), response.requestLatencyMs());

} else {

// this is the acks = 0 case, just complete all requests

// 当请求无响应结果，这里针对 ack=0 时的处理

for (ProducerBatch batch : batches.values()) {

completeBatch(batch, new ProduceResponse.PartitionResponse(Errors.NONE), correlationId, now);

}

}

}

}

该方法主要是客户端接到 Broker 端响应后根据 response 结果分情况处理：completeBatch()。

1.  首先会处理两个失败的场景:
2.  如果连接失败会设定异常：**Eorros.NETWORK\_EXCEPTION**。
3.  如果客户端和 Broker 端 api 版本不匹配会设定异常：**Eorros.UNSUPPORTED\_VERSION**。
4.  正常响应也分两种情况:
5.  对应响应有返回值。根据响应集合里的分区找到 recordsByPartition 集合里对应的 batch，然后把 batch 和响应信息当参数调用 completeBatch() 方法。
6.  对应响应无返回值的场景是，当发送时设定 ack=0 时，生产端只管发送而不去管响应结果，所以不用考虑响应的返回值，也是调用 completeBatch() 方法。

**02.2.10 completeBatch()**

private void completeBatch(ProducerBatch batch, ProduceResponse.PartitionResponse response, long correlationId,long now) {

Errors error \= response.error;

//当消息过长，会把单条消息分成多个 batch 发送。

if (error == Errors.MESSAGE\_TOO\_LARGE && batch.recordCount > 1 && !batch.isDone() &&

(batch.magic() >= RecordBatch.MAGIC\_VALUE\_V2 || batch.isCompressed())) {

...

this.accumulator.splitAndReenqueue(batch);

// 从 inflightBatches 中删除批次并释放缓存批次空间

maybeRemoveAndDeallocateBatch(batch);

this.sensors.recordBatchSplit();

} else if (error != Errors.NONE) {

// 能否再次发送

if (canRetry(batch, response, now)) {

...

// 重新入队

reenqueueBatch(batch, now);

} else if (error == Errors.DUPLICATE\_SEQUENCE\_NUMBER) {

// 针对重复发送情况

completeBatch(batch, response);

} else {

final RuntimeException exception;

// topic 授权失败

if (error == Errors.TOPIC\_AUTHORIZATION\_FAILED)

exception = new TopicAuthorizationException(Collections.singleton(batch.topicPartition.topic()));

else if (error == Errors.CLUSTER\_AUTHORIZATION\_FAILED)

// 集群授权失败

exception = new ClusterAuthorizationException("The producer is not authorized to do idempotent sends");

else

exception = error.exception(response.errorMessage);

// 通知该批消息发送失败并返回给客户端

failBatch(batch, response, exception, batch.attempts() < this.retries);

}

// 无效的元数据异常

if (error.exception() instanceof InvalidMetadataException) {

if (error.exception() instanceof UnknownTopicOrPartitionException) {

...

} else {

...

}

// 更新元数据信息

metadata.requestUpdate();

}

} else {

//正常情况执行回调

completeBatch(batch, response);

}

// Unmute the completed partition.

if (guaranteeMessageOrder)

// 要保证消息顺序性，在发送完成后将 tp 从 muted 集合中移除，这样 tp 才能进行下次发送。

this.accumulator.unmutePartition(batch.topicPartition);

}

//重新入队

private void reenqueueBatch(ProducerBatch batch, long currentTimeMs) {

// 重新添加批次到队列中

this.accumulator.reenqueue(batch, currentTimeMs);

// 从 inflightBatches 中删除批次

maybeRemoveFromInflightBatches(batch);

this.sensors.recordRetries(batch.topicPartition.topic(), batch.recordCount);

}

// RecordAccumulator.java

public void reenqueue(ProducerBatch batch, long now) {

// 批次重新入队

batch.reenqueued(now);

// 获取 ArrayDeque<ProducerBatch>

Deque<ProducerBatch> deque = getOrCreateDeque(batch.topicPartition);

synchronized (deque) {

if (transactionManager != null)

insertInSequenceOrder(deque, batch);

else

// 添加到队首

deque.addFirst(batch);

}

}

接下来我们讲解下处理**正常响应**的方法 。

**02.2.11 completeBatch(batch，response)**

private void completeBatch(ProducerBatch batch, ProduceResponse.PartitionResponse response) {

// 事务管理器的忽略

if (transactionManager != null) {

transactionManager.handleCompletedBatch(batch, response);

}

// 执行回调，从 inflightBatches 中删除批次并释放缓存批次空间

if (batch.done(response.baseOffset, response.logAppendTime, null)) {

maybeRemoveAndDeallocateBatch(batch);

}

}

至此， Sender 线程**从发送消息到处理最终处理响应的**主要方法都讲解完了。

## **03 Sender 发送流程分析**

通过前两部分的源码解读和剖析，我们可以得出 Sender 线程的处理流程可以分为两大部分：「**发送请求**」、「**接收响应结果**」。

## **03.1 发送请求**

从 runOnce 方法可以得出发送请求也分两步：「**消息预发送**」、「**真正的网络发送**」。

void runOnce() {

// 1. 把消息缓存在 KafkaChannel 的 Send 字段里。

long pollTimeout \= sendProducerData(currentTimeMs);

// 2. 读取消息实现真正的网络发送

client.poll(pollTimeout, currentTimeMs);

}

## **03.2 接收响应结果**

等 Sender 线程收到 Broker 端的响应结果后，会根据响应结果分情况进行处理。

## **03.3 流程时序图**

![](https://article-images.zsxq.com/FgploeCGlHYusKHPNfRyFB_9QLmH)

## **04 总结**

这里，我们一起来总结一下这篇文章的重点。

1、开篇总述消息被**暂存到 Deque<ProducerBatch> 的 batches 中**，等「**批次已满**」或者「**有新批次被创建**」后，唤醒 Sender 子线程将消息批量发送给 Kafka Broker 端，从而引出「**Sender 线程**」。

2、带你深度剖析了「**Sender 线程**」 的发送消息以及响应处理的相关方法。

3、最后带你串联了整个消息发送的流程，让你有个更好的整体认知。