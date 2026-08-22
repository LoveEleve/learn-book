大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了「**真正进行网络 I/O 的 NetworkClient 的架构设计**」，深度剖析了消息是如何被发送出去的，今天主要来深度聊聊 「**kafka 客户端是如何选择合适网络进行请求**」。

  
![](https://article-images.zsxq.com/Fl2_pe3d1dbkdL0acewxAHpCZcyT)

## **01 总体概述**

在上篇中，我们了解了 **NetworkClient 为**「**生产者**」、「**消费者**」、「**服务端**」**等上层业务提供了网络I/O的能力**。

通过调用 NetworkClient 实例的 send 方法，可以发起网络请求。具体来说，发起网络请求的步骤如下：

1.  **构建请求对象**：根据请求类型和参数，构建对应的请求对象，例如 FetchRequest、MetadataRequest 等。
2.  **封装请求对象**：将请求对象封装成 Kafka 协议格式的字节流。
3.  **发送请求**：调用 NetworkClient 的 send 方法，并将封装好的字节流作为参数传入。
4.  **接收响应**：NetworkClient 发送请求后会立即返回一个 Future 对象，通过监听该对象的状态，可以得知网络请求是否成功以及返回的响应内容。
5.  **处理响应**：如果请求成功，得到的响应也是一个 Kafka 协议格式的字节流，需要根据请求类型进行解析，并将结果返回给上层调用者。

详情请点击 [【生产者源码分析系列第九篇】图解 Kafka 源码之 NetworkClient 网络通信组件架构设计](https://articles.zsxq.com/id_k2pnfv2xq2wb.html)

接下来继续通过「**场景驱动**」的方式，来看看客户端时如何选择合适的网络进行请求的。

## **02 构建并发起请求**

## **2.1、构建请求的关键类**

客户端发起请求涉及到的几个关键类：

### **2.1.1、****NetworkSend 类**

github地址：

[https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/common/network/NetworkSend.java](https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/common/network/NetworkSend.java)

[https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/common/network/ByteBufferSend.java](https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/common/network/ByteBufferSend.java)

**NetworkSend 类**继承自 **ByteBufferSend 类**, 有以下几个接口：

public class ByteBufferSend implements Send {

public ByteBufferSend(String destination, ByteBuffer... buffers)

// 要把数据写入目标的 channel id

public String destination()

// 要发送的数据是否发送完了

public boolean completed()

// 发送数据的大小

public long size()

// 写入缓存数据到 KafkaChannel 的 send

public long writeTo(GatheringByteChannel channel) throws IOException;

// ByteBuffer数组所有的ByteBuffer 还剩多少字节没有写完。

public long remaining();

}

该类的作用主要是用来缓存待发送的数据的。具体的实现请点击 [](https://articles.zsxq.com/id_f95tmqef8ueb.html)[【生产者源码分析系列第三篇】图解 Kafka 网络层实现机制之NIO](https://articles.zsxq.com/id_f95tmqef8ueb.html)

### **2.1.2、****NetworkClientUtils 类**

这是客户端的工具类, 只要构建好了 **NetworkClient**, 就可以用这个工具类来发送请求。

github 地址：

[https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/clients/NetworkClientUtils.java](https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/clients/NetworkClientUtils.java)

### **2.1.3、****NetworkClient 类**

上一篇分析的源码类，主要用来构建异步请求/响应网络 I/O 的网络客户端。

这里涉及到的关于 Broker 的配置有以下三个：

1.  **request.timeout.ms**：用来配置客户端等待请求响应的最长时间，默认值：30000（30 秒）。如果在超时之前没有收到响应，客户端将在必要时重新发送请求。如果重试次数用尽，则请求最终失败。
2.  **socket.connection.setup.timeout.ms**：用来配置客户端等待套接字连接建立的时间，默认值：10000（10 秒）。如果在超时之前没有建立连接，客户端将关闭套接字通道。
3.  **socket.connection.setup.timeout.max.ms**：用来配置客户端等待建立套接字连接的最长时间，默认值：127000（127 秒）。连接设置超时时间将随着每一次连续的连接失败而成倍增加，直到这个最大值。为了避免连接风暴，超时时间将被应用一个0.2的随机因子，导致计算值在20%以下和20%以上的随机范围。

## **2.2、更新元数据**

这里通过3个问题来梳理一下元数据的更新流程：

1.  metadata是什么？内部如何设计？
2.  Kafka Producer 更新 metadata 的流程。
3.  Kafka Producer 更新 metadata 时机。

我们先来看第一个问题。

### **2.2.1、metadata 是什么**

Metadata 是指 Kafka 集群的元数据，包含了 Kafka 集群的各种信息。具体的源码实现请点击 [【生产者源码分析系列第二篇】图解 Kafka 生产者元数据拉取管理全流程](https://articles.zsxq.com/id_hqqk9de6rfwx.html) 。

通过源码我们大概总结下包含哪些信息：

1.  kafka 集群中有哪些节点。
2.  kafka 集群中有哪些 Topic，这些 Topic 又有哪些 partition。
3.  每个 Partition 的 Leader 副本分配在哪个节点上，Follower 副本分配在哪些节点上。
4.  每个 partition 的 AR 有哪些副本，ISR 有哪些副本。

接下来看下第二个问题。

### **2.2.2、Kafka Producer 更新 metadata 流程**

我们知道 Metadata 在 Kafka 中是非常重要的，场景如下：

1.  Kafka Producer 发送一条消息到对应的 Topic 中，首先需要知道分区的数量，发送的目标分区，目标分区的 Leader，Leader 所在的节点等，都要从 Metadata 中获取才能知道。
2.  当 Kafka 集群中发生了 Leader 选举，节点中 Partition 或副本发生了变化等，都需要更新 Metadata 中的数据。

接下来我们来分析下更新 Metadata 元数据的流程。

在 [](https://articles.zsxq.com/id_8nv25wvidj96.html)[【生产者源码分析系列第一篇】图解 Kafka 生产者初始化核心流程](https://articles.zsxq.com/id_8nv25wvidj96.html) [](https://articles.zsxq.com/id_8nv25wvidj96.html)中，我们了解到 kafka Producer 在调用 dosend() 方法时，第一步就是通过 waitOnMetadata 方法获取该 topic 的 metadata 信息。

private Future<RecordMetadata> doSend(ProducerRecord<K, V> record, Callback callback) {

try {

...

try {

clusterAndWaitTime = waitOnMetadata(record.topic(), record.partition(), nowMs, maxBlockTimeMs);

} catch (KafkaException e) {}

...

return result.future;

} catch (ApiException e) {

...

} catch (InterruptedException e) {

...

} catch (KafkaException e) {

...

} catch (Exception e) {

...

}

}

private ClusterAndWaitTime waitOnMetadata(String topic, Integer partition, long nowMs, long maxWaitMs) throws InterruptedException {

// 1. 从元数据缓存中获取元数据

Cluster cluster \= metadata.fetch();

// 2. 判断该主题是否是无效的主题

if (cluster.invalidTopics().contains(topic))

throw new InvalidTopicException(topic);

// 3. 将该主题放入元数据主题列表中

metadata.add(topic, nowMs);

// 4. 从元数据缓存中获取主题对应的分区数

Integer partitionsCount \= cluster.partitionCountForTopic(topic);

// 5. 满足这些条件后就不用拉取元数据，直接从元数据缓存中返回

if (partitionsCount != null && (partition == null || partition < partitionsCount))

// 返回元数据信息

return new ClusterAndWaitTime(cluster, 0);

long remainingWaitMs \= maxWaitMs;

long elapsed \= 0;

// 6. 不停的轮询唤醒 sender 线程更新元数据

do {

// 7. 将主题 topic及过期时间添加到元数据主题列表中

metadata.add(topic, nowMs + elapsed);

// 8. 标记元数据更新标识，获取元数据版本号

int version \= metadata.requestUpdateForTopic(topic);

// 9. 唤醒 sender 子线程

sender.wakeup();

try {

// 10. 阻塞线程等待元数据更新成功

metadata.awaitUpdate(version, remainingWaitMs);

} catch (TimeoutException ex) {

...

}

// 11. 到这里，应该是当前cluster更新成功了，再次获取元数据

cluster = metadata.fetch();

// 12. 计算等待更新完成元数据消耗时间

elapsed = time.milliseconds() - nowMs;

// 13. 如果超时，抛超时异常

if (elapsed >= maxWaitMs) {

...

}

metadata.maybeThrowExceptionForTopic(topic);

remainingWaitMs = maxWaitMs - elapsed;

// 14. 获取元数据分区数

partitionsCount = cluster.partitionCountForTopic(topic);

} while (partitionsCount == null || (partition != null && partition >= partitionsCount));

// 15. 返回元数据以及消耗时间

return new ClusterAndWaitTime(cluster, elapsed);

}

/\*\*

\* Wait for metadata update until the current version is larger than the last version we know of

\*/

public synchronized void awaitUpdate(final int lastVersion, final long timeoutMs) throws InterruptedException {

long currentTimeMs \= time.milliseconds();

// 超时时间

long deadlineMs \= currentTimeMs + timeoutMs < 0 ? Long.MAX\_VALUE : currentTimeMs + timeoutMs;

// 通过调用 time.waitObject 来实现线程阻塞

time.waitObject(this, () -> {

// Throw fatal exceptions, if there are any. Recoverable topic errors will be handled by the caller.

maybeThrowFatalException();

return updateVersion() > lastVersion || isClosed();

}, deadlineMs);

if (isClosed())

throw new KafkaException("Requested metadata update after close");

}

public void waitObject(Object obj, Supplier<Boolean> condition, long deadlineMs) throws InterruptedException {

synchronized (obj) {

while (true) {

// 判断条件是否满足即元数据是否更新成功，成功直接返回

if (condition.get())

return;

long currentTimeMs \= milliseconds();

// 超时抛出异常

if (currentTimeMs >= deadlineMs)

throw new TimeoutException("Condition not satisfied before deadline");

// 调用 wait 阻塞线程

obj.wait(deadlineMs - currentTimeMs);

}

}

}

拉取流程如下图所示：

![](https://article-images.zsxq.com/FsEBQ5CUqMp2LjD0KIkCG6ucZnly)

综上，首先在 Metadata.awaitUpdate() 方法中，线程会阻塞在 while 循环中，直到 metadata 更新成功或者超时退出。因此 Kafka Producer 主线程会**阻塞在两个 while 循环中**，直到 metadata 信息更新成功，**那么 metadata 是如何被更新的呢？**

它主要是通过 [sender.wakeup](http://sender.wakeup/)() 来唤醒 sender 线程，来间接唤醒 NetworkClient 线程，NetworkClient 线程来负责发送 Metadata 请求，并处理 Broker 端的响应。

在 NetworkClient 中真正处理请求的是 [NetworkClient.poll](http://networkclient.poll/)() 方法，而在 poll() 方法中会调用 metadataUpdater.maybeUpdate()。来看看是如何更新元数据的

public List<ClientResponse> poll(long timeout, long now) {

...

// 判断是否需要更新元数据，如果需要则更新，返回可以等待更新的时间

long metadataTimeout \= metadataUpdater.maybeUpdate(now); ...

}

// 更新元数据

public long maybeUpdate(long now) {

// should we update our metadata?

// 获取下一次更新的时间，如果needupdate=true，则返回0即马上更新；否则返回剩余的过期时间

long timeToNextMetadataUpdate \= metadata.timeToNextUpdate(now);

// 计算需要等待的时间，如果有正在处理的请求，则返回默认的请求间隔时间，否则返回0

long waitForMetadataFetch \= hasFetchInProgress() ? defaultRequestTimeoutMs : 0;

long metadataTimeout \= Math.max(timeToNextMetadataUpdate, waitForMetadataFetch);

// 如果大于0说明还需要等待一定时间才能更新

if (metadataTimeout > 0) {

return metadataTimeout;

}

// 获取一个最小负载节点

Node node \= leastLoadedNode(now);

if (node == null) {

// 返回等待创建连接所需时间

return reconnectBackoffMs;

}

return maybeUpdate(now, node);

}

private boolean hasFetchInProgress() {

return inProgress != null;

}

如何获取一个最小负载节点呢？

**LeastLoadedNode** 指 Kafka 集群中所有 Node 中负载最小的那一个 Node，它是由每个 Node 在 InFlightRequests 中还未处理的请求数决定的，未处理的请求越少则负载越小。如下图所示，Node2 即为 LeastLoadedNode。

![](https://article-images.zsxq.com/Fhazq4J_au5vbIRaJW7sf4qEVaBU)

// 计算下次更新元数据的时间

public synchronized long timeToNextUpdate(long nowMs) {

long timeToExpire \= updateRequested() ? 0 : Math.max(this.lastSuccessfulRefreshMs + this.metadataExpireMs - nowMs, 0);

return Math.max(timeToExpire, timeToAllowUpdate(nowMs));

}

public synchronized long timeToAllowUpdate(long nowMs) {

return Math.max(this.lastRefreshMs + this.refreshBackoffMs - nowMs, 0);

}

**计算下次更新元数据信息的时间**：当前 metadata 信息即将到期的时间即 timeToExpire 和 距离允许更新 metadata 信息的时间 即 timeToAllowUpdate 中的最大值。

1.  **timeToExpire**：needUpdate 为 true，表示需要强制更新，此时该值为 0；否则就按照定时更新时间，即元数据信息过期时间（默认是 300000 ms 即 5 分钟）进行周期性更新。
2.  **timeToAllowUpdate**：默认就是 refreshBackoffMs 的默认值，即 100 ms。

我们来继续看下最后调用的 maybeUpdate()。

/\*\*

\* Add a metadata request to the list of sends if we can make one

\* 判断是否可以发送请求，如果可以的话将metadata请求加入发送列表

\*/

private long maybeUpdate(long now, Node node) {

String nodeConnectionId \= node.idString();

// 1、如果 client 与 当前 Node 是否已经 ready 了，并且支持发送更多的请求

if (canSendRequest(nodeConnectionId, now)) {

// 更新所有的 topic 还是更新 metadata 中的 topic

Metadata.MetadataRequestAndVersion requestAndVersion \= metadata.newMetadataRequestAndVersion(now);

// 创建 metadata 请求

MetadataRequest.Builder metadataRequest \= requestAndVersion.requestBuilder;

// 调用 NetworkClient 的 doSend 方法，发送更新 metadata 请求

sendInternalMetadataRequest(metadataRequest, nodeConnectionId, now);

inProgress = new InProgressData(requestAndVersion.requestVersion, requestAndVersion.isPartialUpdate);

return defaultRequestTimeoutMs;

}

// 2、如果 client 与 Node 正在连接中， 则直接返回

if (isAnyNodeConnecting()) {

// Strictly the timeout we should return here is "connect timeout", but as we don't

// have such application level configuration, using reconnect backoff instead.

return reconnectBackoffMs;

}

// 3、如果 client 与 Node 还未建立连接，则进行初始化连接

if (connectionStates.canConnect(nodeConnectionId, now)) {

// We don't have a connection to this node right now, make one

log.debug("Initialize connection to node {} for sending metadata request", node);

initiateConnect(node, now);

return reconnectBackoffMs;

}

// connected, but can't send more OR connecting

// In either case, we just need to wait for a network event to let us know the selected

// connection might be usable again.

return Long.MAX\_VALUE;

}

void sendInternalMetadataRequest(MetadataRequest.Builder builder, String nodeConnectionId, long now) {

ClientRequest clientRequest \= newClientRequest(nodeConnectionId, builder, now, true);

doSend(clientRequest, true, now);

}

从上面源码中可以看出，当每次 kafka producer 请求更新 metadata 时，会有以下几种情况：

1.  如果 client 与 当前 Node 是否已经 ready 了，并且支持发送更多的请求，那么就直接发送请求。
2.  如果 client 与 Node 正在建立连接，则直接返回。
3.  如果 client 与 Node 还没建立连接，则进行初始化连接。

综上得出 Kafka Producer 主线程一直是阻塞在两个 while 循环中的，直到 metadata 更新或者超时退出：

1.  Sender 子线程第一次调用 poll() 初始化与 node 的连接。
2.  Sender 子线程第二次调用 poll() 发送 metadata 请求。
3.  Sender 子线程第三次调用 poll() 获取 metadataResponse，并更新 metadata。

剖析完更新元数据的流程后，我们来看看最后一个问题。

### **2.2.3、Kafka Producer 更新 metadata 时机**

这里总结下，Metadata 会在下面两种情况下进行更新：

1.  **强制更新**：调用 Metadata.requestUpdate() 将 needUpdate 置成了 true 来强制更新。
2.  **周期性更新**：通过 Metadata 的 lastSuccessfulRefreshMs 和 metadataExpireMs 来实现，一般情况下，默认周期时间就是 metadataExpireMs，即 5 分钟。

当 NetworkClient 的 poll() 方法被调用时，就会去检查这两种更新机制，只要达到其中一种，就行触发更新操作，Metadata 的强制更新会在以下几种情况下进行：

1.  initiateConnect() 方法调用时，进行初始化连接。
2.  poll() 方法中对 handleDisconnections() 方法调用来处理连接断开的情况，这时会触发强制更新。
3.  poll() 方法中对 handleTimedOutRequests() 来处理请求超时时会触发强制更新。
4.  发送消息时，如果无法找到 Partition 对应的 Leader 时会触发强制更新。
5.  处理 handleProduceResponse() 响应，如果返回关于 Metadata 过期的异常时触发强制更新。

## **2.3、发起网络请求**

剖析完元数据的更新流程以及更新机制后，我们来看下请求是如何发起的，这块可以点击查看 [【生产者源码分析系列第九篇】图解 Kafka 源码之 NetworkClient 网络通信组件架构设计](https://articles.zsxq.com/id_k2pnfv2xq2wb.html) 最后一部分的流程串联。

这里再总结下整个流程：

1.  生成 **clientRequest请求**，这里需要注意的是 brokerNode 就是指具体的 Broker 的 EndPoint，一个 Broker 可能会有多个 EndPoint，选择哪个是由调用者决定的。
2.  执行发送请求流程。
3.  调用 **canSendRequest()** 校验是否可以发送 Request 请求，只针对外部请求，判断条件为： 连接状态Ready && channel 通道 Ready && 正在发送中的请求数量 < maxInFlightRequestsPerConnection。
4.  如果可以发送 Request 请求，则开始构建 **NetworkSend**，然后底层调用 **Selector.send(send)** 开始发送，发送之前先将请求保存到 **InFlightRequests** 中，用于后面判断请求数是否超过阈值。其实这个过程就是在注册 **SelectionKey.OP\_WRITE 事件**。
5.  最后循环遍历 **NetworkClient.poll()** ，获取对应的 Response 并处理响应。

## **2.4、 解析命中的 Broker 列表**

通过元数据更新器 **MetadataUpdater** 来发起更新请求。我们来看看获取元数据后，如何解析 Broker 集群列表的。

github地址：

[https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/clients/NetworkClient.java](https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/clients/NetworkClient.java)

[https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/clients/Metadata.java](https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/clients/Metadata.java)

[https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/common/requests/MetadataResponse.java](https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/common/requests/MetadataResponse.java)

/\*\*

\* NetworkClient#handleCompletedReceives

\* 完成接收的handler，处理 completedReceives 队列

\*/

private void handleCompletedReceives(List<ClientResponse> responses, long now) {

for (NetworkReceive receive : this.selector.completedReceives()) {

...

if (req.isInternalRequest && response instanceof MetadataResponse)

// 处理元数据请求响应

metadataUpdater.handleSuccessfulResponse(req.header, now, (MetadataResponse) response);

...

}

}

/\*\*

\* metadataUpdater#handleSuccessfulResponse

\* 处理元数据请求响应

\*/

public void handleSuccessfulResponse(RequestHeader requestHeader, long now, MetadataResponse response) {

...

// broker 节点集合为空

if (response.brokers().isEmpty()) {

// 失败更新

this.metadata.failedUpdate(now);

} else {

// 成功更新

this.metadata.update(inProgress.requestVersion, response, inProgress.isPartialUpdate, now);

}

inProgress = null;

}

/\*\*

\* Metadata#update

\* 更新元数据

\*/

public synchronized void update(int requestVersion, MetadataResponse response, boolean isPartialUpdate, long nowMs) {

...

this.cache = handleMetadataResponse(response, isPartialUpdate, nowMs);

...

}

/\*\*

\* Transform a MetadataResponse into a new MetadataCache instance.

\*/

private MetadataCache handleMetadataResponse(MetadataResponse metadataResponse, boolean isPartialUpdate, long nowMs) {

// All encountered topics.

Set<String> topics = new HashSet<>();

// Retained topics to be passed to the metadata cache.

Set<String> internalTopics = new HashSet<>();

Set<String> unauthorizedTopics = new HashSet<>();

Set<String> invalidTopics = new HashSet<>();

....

// 返回的 Broker 节点集合

Map<Integer, Node> nodes = metadataResponse.brokersById();

// 判断是否部分主题更新

if (isPartialUpdate)

// 如果是则与现在的元数据缓存合并在一起

return this.cache.mergeWith(metadataResponse.clusterId(), nodes, partitions,

unauthorizedTopics, invalidTopics, internalTopics, metadataResponse.controller(),

(topic, isInternal) -> !topics.contains(topic) && retainTopic(topic, isInternal, nowMs));

else

// 如果是全部主题更新的话，就重新初始化元数据缓存

return new MetadataCache(metadataResponse.clusterId(), nodes, partitions,

unauthorizedTopics, invalidTopics, internalTopics, metadataResponse.controller());

}

public Map<Integer, Node> brokersById() {

return holder().brokers;

}

通过上面源码可以看出，在发送请求完成后，会处理响应结果并更新元数据，在更新过程中，会返回 Broker 集群的节点集合。

客户端在发起请求时候，会根据 **bootstrap.servers = xxx:port** 配置来命中对应的监听器，然后根据命中的监听器 ListenerName，来过滤集群中其他 Broker 配置同样监听器名称的 EndPoint，最终返回符合条件的 EndPoint。

举例，Kafka 集群网络监听器配置如下：

\# server0.properties

listeners = PLAINTEXT://localhost:9092

\# server1.properties

listeners = PLAINTEXT://localhost:9092,TEXT://localhost:9093

listener.security.protocol.map=PLAINTEXT:PLAINTEXT,SSL:SSL,SASL\_PLAINTEXT:SASL\_PLAINTEXT,SASL\_SSL:SASL\_SSL,TEXT:PLAINTEXT

#server2.properties

listeners = PLAINTEXT://localhost:9092

此时，启动 KafkaProducer 客户端时，配置 **bootstrap.servers = localhost:9092**，这样会命中 PLAINTEXT 这个监听器，上面看到配置中每个 Broker 都配置了 PLAINTEXT 监听器，所以最终会返回 3 个 EndPoint。

## **03 总结**

这里，我们一起来总结一下这篇文章的重点。

1、开篇总述发送网络消息的整个流程，引出 kafka 客户端是如何选择「**合适网络进行请求的**」。

2、带你深度剖析如何「**构建 Request 请求**」 、「**更新元数据流程和时机**」、「**解析 Broker 集群列表**」的实现细节。

3、最后带你如何剖析出符合条件的 Broker EndPoint，让你有个更好的整体认知。