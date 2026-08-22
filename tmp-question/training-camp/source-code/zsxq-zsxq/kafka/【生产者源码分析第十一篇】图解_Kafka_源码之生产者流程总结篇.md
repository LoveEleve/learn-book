大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了「**kafka 客户端是如何选择合适网络进行请求**」，深度剖析如何「**构建 Request 请求**」 、「**更新元数据流程和时机**」、「**解析 Broker 集群列表**」的实现细节，并最终剖析符合条件的 Broker EndPoint，今天主要来总结下 「**kafka 生产者整个发送流程**」。

  
![](https://article-images.zsxq.com/FiPTzi1gjUjteBeYCeh8kLWRtVzZ)

## **01 总体概述**

通过「**场景驱动**」的方式，前面十篇文章，我们已经从一条消息的构造，Sender 子线程启动，到如何在客户端缓存，再到最后构造 Request 请求并通过网络组件 NetworkClient 将消息发送出去整个过程的源码进行了详细的剖析。

今天我们就来总结下这整个过程。

1.  生产者初始化核心流程。
2.  生产者元数据拉取流程。
3.  多路复用 Selector 网络收发消息全过程。
4.  消息快递仓库 RecordAccmulator 缓存过程。
5.  Sender 子线程收发消息全过程。
6.  整个发送流程总结。

## **02 生产者初始化核心流程**

详情请点击 [【生产者源码分析系列第一篇】图解 Kafka 生产者初始化核心流程](https://articles.zsxq.com/id_8nv25wvidj96.html)

## **2.1 构造 KafkaProducer**

Properties properties \= new Properties();

//构造 KafkaProducer

KafkaProducer producer = new KafkaProducer(properties);

//调用send异步回调发送

producer.send(record,new DemoCallBack(record.topic(), record.key(), record.value()));

KafkaProducer 首先通过解析 producer.propeties 文件里面的属性来构造自己。例如 ：设置分区器、Key 、Value序列化器、拦截器、RecordAccumulator 消息累加器 、元数据更新器，启动发送请求的Sender线程等。

##   
**2.2 生产者分区器**

设置对应的分区器**（partitioner）**，支持自定义，用来将消息分配给某个主题的某个分区的。

this.partitioner = config.getConfiguredInstance(ProducerConfig.PARTITIONER\_CLASS\_CONFIG, Partitioner.class);

/\*\* <code>partitioner.class</code> \*/

public static final String PARTITIONER\_CLASS\_CONFIG \= "partitioner.class";

.define(PARTITIONER\_CLASS\_CONFIG,Type.CLASS,DefaultPartitioner.class,Importance.MEDIUM, PARTITIONER\_CLASS\_DOC)

相关配置如下：

属性：partitioner.class

描述：消息的分区分配策略

默认值：org.apache.kafka.clients.producer.internals.DefaultPartitioner

## **2.3 生产者拦截器**

设置生产者拦截器**（ProducerInterceptor）**，拦截器的主要作用是在消息发送之前按照一定的规则统一对消息进行处理。例如：按照某个规则过滤某条消息。

List<ProducerInterceptor<K, V>> interceptorList = (List) configWithClientId.getConfiguredInstances(

ProducerConfig.INTERCEPTOR\_CLASSES\_CONFIG, ProducerInterceptor.class);

/\*\* <code>interceptor.classes</code> \*/

public static final String INTERCEPTOR\_CLASSES\_CONFIG \= "interceptor.classes";

.define(INTERCEPTOR\_CLASSES\_CONFIG,Type.LIST,Collections.emptyList(),new ConfigDef.NonNullValidator(),Importance.LOW,INTERCEPTOR\_CLASSES\_DOC)

相关配置如下：

属性：interceptor.classes

描述：生产者拦截器配置,填写全路径类名,可用逗号隔开配置多个,执行顺序就是配置的顺序。

默认值：空

## **2.4 设置消息缓冲区**

this.totalMemorySize = config.getLong(ProducerConfig.BUFFER\_MEMORY\_CONFIG);

/\*\* <code>buffer.memory</code> \*/

public static final String BUFFER\_MEMORY\_CONFIG \= "buffer.memory";

// 32M

.define(BUFFER\_MEMORY\_CONFIG, Type.LONG, 32 \* 1024 \* 1024L, atLeast(0L), Importance.HIGH, BUFFER\_MEMORY\_DOC)

相关配置如下：

属性：buffer.memory

描述：发送消息的缓冲区的大小。

默认值：32M

## **2.5 生产者元数据更新器**

我们知道在客户端都会保存集群的元信息。例如：生产者的元数据是 ProducerMetadata。而消费组的元数据是ConsumerMetadata 。

// 初始化 Kafka 集群元数据，元数据会保存到客户端中，并与服务端元数据保持一致

if (metadata != null) {

this.metadata = metadata;

} else {

// 初始化集群元数据

this.metadata = new ProducerMetadata(retryBackoffMs,

// 元数据过期时间：默认5分钟

config.getLong(ProducerConfig.METADATA\_MAX\_AGE\_CONFIG),

// topic最大空闲时间，如果在规定时间没有被访问，将从缓存删除，下次访问时强制获取元数据

config.getLong(ProducerConfig.METADATA\_MAX\_IDLE\_CONFIG),

logContext,

clusterResourceListeners,

Time.SYSTEM);

// 启动metadata的引导程序

this.metadata.bootstrap(addresses);

}

public static final String METADATA\_MAX\_AGE\_CONFIG \= "metadata.max.age.ms";

.define(METADATA\_MAX\_AGE\_CONFIG, Type.LONG, 5 \* 60 \* 1000, atLeast(0), Importance.LOW, METADATA\_MAX\_AGE\_DOC)

/\*\* <code>metadata.max.idle.ms</code> \*/

public static final String METADATA\_MAX\_IDLE\_CONFIG \= "metadata.max.idle.ms";

.define(METADATA\_MAX\_IDLE\_CONFIG,Type.LONG, 5 \* 60 \* 1000,atLeast(5000),Importance.LOW,METADATA\_MAX\_IDLE\_DOC)

相关配置如下：

属性：metadata.max.age.ms

描述：强制刷新元数据的时间段（以毫秒为单位）。

默认值：300000(5分钟)

属性：metadata.max.idle.ms

描述：topics中保存Topic的有效期时间（以毫秒为单位）。

默认值：300000(5分钟)

属性：retry.backoff.ms

描述：两次重试之间的时间间隔。

默认值：100 毫秒

这里需要注意的是：**Producer 元数据虽然会定时自动更新，但也可能会出现发送时某个 TopicPartition 不存在的情况，此时就需要立刻发起新的元数据更新请求**。

## **2.6 初始化 Sender 线程**

Sender 子线程是专门负责将消息发送到 Broker的 I/O 线程。

// 初始化 Sender 发送线程类，并同时初始化

NetworkClientthis.sender = newSender(logContext, kafkaClient, this.metadata);

// Sender 线程名称

String ioThreadName \= NETWORK\_THREAD\_PREFIX + " | " + clientId;

// 用 ioThread 线程来封装 Sender 线程类，使用 demon 守护线程方式来启动 Sender 线程类

this.ioThread = new KafkaThread(ioThreadName, this.sender, true);

this.ioThread.start();

## **2.7 发送请求**

producer.send(record,new CallBack(record.topic(), record.key(), record.value()));

### **2.7.1 执行拦截器**

发送消息的第一步就是执行拦截器。

// 向 topic 异步地发送数据，当发送确认后唤起回调函数

public Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback) {

// 执行拦截器

ProducerRecord<K, V> interceptedRecord = this.interceptors.onSend(record);

return doSend(interceptedRecord, callback);

}

public ProducerRecord<K, V> onSend(ProducerRecord<K, V> record) {

ProducerRecord<K, V> interceptRecord = record;

for (ProducerInterceptor<K, V> interceptor : this.interceptors) {

try {

// 循环执行拦截器，且拦截器中修改的消息体会被传递到下一个拦截器

interceptRecord = interceptor.onSend(interceptRecord);

} catch (Exception e) {

// 拦截器抛出异常会被捕获,并打印日志

if (record != null)

log.warn("Error executing interceptor onSend callback for topic: {}, partition: {}", record.topic(), record.partition(), e);

else

log.warn("Error executing interceptor onSend callback", e);

}

}

return interceptRecord;

}

一般情况下可能不需要拦截器, 但是当我们需要用拦截器的时候按照下面操作执行:

1.  在配置文件中配置属性 interceptor.classes=拦截器1,拦截器2,拦截器3....， **这里可以配置多个拦截器，用逗号隔开，按照配置的顺序执行的**。
2.  实现接口 [org.apache.kafka.clients.producer.ProducerInterceptor](http://org.apache.kafka.clients.producer.producerinterceptor/)<K, V>。

### **2.7.2 更新元数据 waitOnMetaData**

在发送消息之前需要先获取一下将要发送的 Topic 分区的元数据信息。**获取元数据请求也是通过唤醒 Sender 线程进行发送的**。

private Future<RecordMetadata> doSend(ProducerRecord<K, V> record, Callback callback) {

TopicPartition tp \= null;

try {

....省略

// 等待元数据更新即确认数据要发送到的 topic 的 metadata 是可用的

clusterAndWaitTime = waitOnMetadata(record.topic(), record.partition(), nowMs, maxBlockTimeMs);

}

....省略

}

1.  ProducerMetadata 元数据主题集合 Map<String, Long\> topics = new HashMap<>(), 保存 Topic 的有效期时间，由 [metadata.max.idle.ms](http://metadata.max.idle.ms/) 控制，默认300000 （5分钟）。
2.  ProducerMetadata 元数据信息 Set<String\> newTopics = new HashSet<>() 中保存所有的 Topic。
3.  如果没有获取到元数据，则会一直等待，最大等待时间由 max.block.ms 决定，默认值：1分钟。如果等待超过该时间，很可能出现网络异常，此时会抛超时异常。
4.  如果此时指定了分区号，但不存在的话会一直发起获取元数据请求，直到超时后抛异常。

### **2.7.3 key、value 序列化**

主要将key、Value 序列化。Kafka客户端提供了很多种序列化器供我们选择, 如果**自定义序列化器则**需要实现 [org.apache.kafka.common.serialization.Serializer](http://org.apache.kafka.common.serialization.serializer/)接口。

// 序列化 record的key和value

byte\[\] serializedKey;

serializedKey = keySerializer.serialize(record.topic(), record.headers(), record.key());

byte\[\] serializedValue;

serializedValue = valueSerializer.serialize(record.topic(), record.headers(), record.value());

### **2.7.4 计算分区号**

将序列化后的key、 value 调用合适的分区器选择将要发送的分区号。

// 设置对应的分区器

this.partitioner = config.getConfiguredInstance(ProducerConfig.PARTITIONER\_CLASS\_CONFIG, Partitioner.class);

// 获取record消息对应的分区

int partition \= partition(record, serializedKey, serializedValue, cluster);

### **2.7.5 初始化并追加到消息累加器**

// 初始化消息累加器---缓冲区

this.accumulator = new RecordAccumulator(logContext,

config.getInt(ProducerConfig.BATCH\_SIZE\_CONFIG),

this.compressionType,

lingerMs(config),

retryBackoffMs,

deliveryTimeoutMs,

metrics,

PRODUCER\_METRIC\_GROUP\_NAME,

time,

apiVersions,

transactionManager,

new BufferPool(this.totalMemorySize, config.getInt(ProducerConfig.BATCH\_SIZE\_CONFIG), metrics, time, PRODUCER\_METRIC\_GROUP\_NAME));

// 向 accumulator 中追加数据

RecordAccumulator.RecordAppendResult result \= accumulator.append(tp, timestamp, serializedKey,

serializedValue, headers, interceptCallback, remainingWaitMs, true, nowMs);

### **2.7.6 唤醒 Sender 线程**

// 如果 batch 已经满了， 则唤醒 sender 线程发送数据

if (result.batchIsFull || result.newBatchCreated) {

log.trace("Waking up the sender since topic {} partition {} is either full or getting a new batch", record.topic(), partition);

// 唤醒 Sender 线程

this.sender.wakeup();

}

## **03 生产者元数据拉取流程**

详情请点击 [【生产者源码分析系列第二篇】图解 Kafka 生产者元数据拉取管理全流程](https://articles.zsxq.com/id_hqqk9de6rfwx.html)

第一部分详细剖析了生产者初始化和唤醒 Sender 线程准备发送，但是发送的前需要获取元数据信息，这里再说明下整个元数据过程。

## **3.1 主线程加载元数据**

整个加载元数据源码过程如下图所示：

![](https://article-images.zsxq.com/FoIHN8zi2dNyRZy31YZZ6OKXswyq)

初始化元数据以及各对象关系如下：

// 初始化 Kafka 集群元数据，元数据会保存到客户端中，并与服务端元数据保持一致

if (metadata != null) {

this.metadata = metadata;

} else {

// 初始化集群元数据

this.metadata = new ProducerMetadata(retryBackoffMs,

// 元数据过期时间：默认5分钟

config.getLong(ProducerConfig.METADATA\_MAX\_AGE\_CONFIG),

// topic最大空闲时间，如果在规定时间没有被访问，将从缓存删除，下次访问时强制获取元数据

config.getLong(ProducerConfig.METADATA\_MAX\_IDLE\_CONFIG),

logContext,

clusterResourceListeners,

Time.SYSTEM);

// 启动metadata的引导程序

this.metadata.bootstrap(addresses);

}

![](https://article-images.zsxq.com/FgZJ0QkQk_iGn-fCYGrM-ukFhmck)

从上图可以得出生产者中获取元数据都是基于 Topic 的，**主要原因就是对于生产者来说，没必要拉取全部的元数据，只拉取自己需要的主题元数据就可以了**。

## **3.2 Sender 子线程拉取元数据**

Sender 子线程拉取元数据源码过程如下图所示：

![](https://article-images.zsxq.com/FkgIqreOwUo4t3srIdskoJFT7BTW)

Sender 线程执行如下：

public class Sender implements Runnable {

public void run() {

// running 字段用来标识当前 Sender 线程是否正常执行

while (running) {

try {

// 如果正常运行，则执行运行周期

runOnce();

} catch (Exception e) {

log.error("Uncaught error in kafka producer I/O thread: ", e);

}

}

}

void runOnce() {

..... // 此处省略事务消息相关的处理逻辑

long currentTimeMs \= time.milliseconds();

// 创建发送到 kafka 集群的请求

long pollTimeout \= sendProducerData(currentTimeMs);

// 真正执行网络IO的地方，会将请求发送出去，并处理收到的响应

client.poll(pollTimeout, currentTimeMs);

}

}

## **3.3 生产者元数据拉取全流程**

![](https://article-images.zsxq.com/FvPTbatt0zGXNd5iyolMgXm5PXVV)

##   
**04 多路复用器 Selector 网络收发消息全过程**

详情请点击

[【生产者源码分析系列第三篇】图解 Kafka 网络层实现机制之NIO](https://articles.zsxq.com/id_f95tmqef8ueb.html)

[【生产者源码分析系列第四篇】图解 Kafka 网络层实现机制之Selector 多路复用器](https://articles.zsxq.com/id_r3td0dogfg0v.html)

[【生产者源码分析系列第五篇】图解 Kafka 网络层源码实现机制之收发消息全过程](https://articles.zsxq.com/id_6smxecrcegul.html)

Kafka 作为一款高性能、高并发、高吞吐量的消息系统，一定会在网络层做优化，业界网络通信的主流做法是通过 **NIO 多路复用器 Selector** 来实现高效处理，Kafka 也是这么做的，它通过对 Java NIO 进行封装实现。

1.  SocketChannel 封装类 TransportLayer 实现了最基础的网络连接、网络读、网络写操作。其中，负责明文传输的是 PlaintextTransportLayer 类。
2.  对 ByteBuffer 的封装主要是分为两部分：写 Buffer 的封装 NetworkSend 和读 Buffer 的封装 NetworkReceive。通过与 PlaintextTransportLayer 类的配合，实现从 Channel 把数据读到 NetworkReceive 缓存类，以及把数据从 NetworkSend 缓存类写到 Channel。
3.  对 NIO组件 Selector的封装，即 KSelector，实现**网络预发送和真正网络发送**。
4.  KafkaChannel 针对上述封装为上层提供了更加友好的网络连接、读写操作。

NetworkClient、Selector、kafkaChannel是如何初始化、kafka如何封装、以及与原生Java NIO的Selector、Channel的关系如何，如下图所示：

![](https://article-images.zsxq.com/FsCL9BC3stCAbESz09Lom1wKY70W)

## **4.1 消息发送流程**

消息发送源码流程如下图所示:

![](https://article-images.zsxq.com/FuyZEkS4i2LOqH7PP0nP07uefXH6)

## **4.2 消息接收响应流程**

接收响应源码流程如下图所示:

![](https://article-images.zsxq.com/Fs24T0fokE9RbsFPoiHvKISTsZjs)

## **05 消息快递仓库 RecordAccmulator 缓存过程**

详情请点击

[【生产者源码分析系列第六篇】图解 Kafka 源码实现机制之客户端缓存架构](https://articles.zsxq.com/id_9qqexkgvp93w.html)

[【生产者源码分析系列第七篇】图解 Kafka 源码之快递仓库 RecordAccumulator 架构设计](https://articles.zsxq.com/id_struwj49p3u8.html)

这里先回顾下 RecordAccumulator 的结构图：

  
![](https://article-images.zsxq.com/FnYieCOY-Ci_78lZvR6pWGADdyMz)

我们都知道每个 TopicPartition 的消息都会被暂存在 ProducerBatch Deque 阻塞队列中的其中一个 ProducerBatch 中,每个ProducerBatch 都存放着一条或者多条消息。而 Sender 线程负责从 RecordAccumulator 里面获取准备好的数据进行发送。

## **5.1 如何判断满足发送条件的 Batch**

首先遍历每个Topic 分区里面的 Deque 队列, 然后获取队列中的第一个 ProducerBatch，如果该 TopicPartition 存在所在 Leader，则进行判断是否满足发送条件：

1.  如果该批次 Batch 满了，或者 Batch 所在的 Deque 队列数量 > 1 满足发送条件。
2.  如果 RecordAccumulator 中的内存用完了，有线程阻塞等待写入到 RecordAccumulator ，也满足发送条件。
3.  该批次 Batch 超过了 linger.ms 的时间也满足发送条件。

## **5.2 获取可发送请求的 ReadyNodes**

上一步选择出满足条件的 Batch，但是**真正发送的时候并不是以 Batch 维度来判断发送的，而是以 Node 维度来发送的**，如下图所示：

![](https://article-images.zsxq.com/FgUhZzY_QQi2JWUhvuB9QNgYb9q_)

这样我们知道了哪些 Batch 能够被发送，然后就可以得出 Batch 对应的 TopicPartition 所在的 Broker 节点，当有了这些可发送的 Broker 节点，再遍历符合条件的 Broker 节点上的每个 TopicPartition 中的 First Batch 进行打包请求。

![](https://article-images.zsxq.com/FrsY9coqv6LzbbZEu03Z2h-ZDSpm)

从上图中可以得出：

1.  Topic1Parititon1、Topic1Parititon2、Topic2Parititon1 这三个的 Leader 都存在于 Broker0 中，虽然 Topic2Parititon1 队列中不满足发送条件，但是跟它同一个 Broker 中有其他的队列满足发送条件，所以它最终也是满足发送条件的。
2.  Topic2Parititon1 Leader 在 Broker1 中，但是它不满足发送条件，且当前 Broker 中也没有其他满足条件的，所以 Broker1 不满足发送条件。
3.  Topic1Parititon3 Leader 在 Broker 2中，且满足发送条件，则 Broker2 满足发送条件。

综上，可以得出满足条件的 ReadyNodes 就是 **Broker0、Broker2**。

## **5.3 过滤未准备好连接的 ReadyNodes**

上一步得到符合条件的 ReadyNodes，那么在**真正向对应 ReadyNodes 发起请求之前，我们还需要判断一下，客户端是否已经准备好跟 ReadyNodes 发起请求了**。

生产者客户端在最开始的时候都没有跟任何 Node 建立连接的，当尝试发送之前会去检测一下连接是否建立成功，如果未建立的话，则会尝试进行建立连接，**并过滤该Node**，会等待到下一次循环的时候，再检测是否建立连接成功。

## **5.4 打包 Batch**

至此，我们已经得到了可以发送请求的 ReadyNodes 了，那么接下来就是解析这些 ReadyNodes 上能够发送的 Batch 进行打包发送了。

这里根据 「**Node 节点集合**」获取要发送的 「**ProducerBatch 集合**」，返回 Map<Integer, List<ProducerBatch>> 集合，其中的 Key 是「**目标 Node 节点Id**」，Value 是此次待发送的 「**ProducerBatch 集合**」。

public Map<Integer, List<ProducerBatch>> drain(Cluster cluster, Set<Node> nodes, int maxSize, long now) {

if (nodes.isEmpty())

return Collections.emptyMap();

// 转换后的结果，Key是目标 Node 的 Id，Value是发送到目标 Node 的 ProducerBatch 集合

Map<Integer, List<ProducerBatch>> batches = new HashMap<>();

for (Node node : nodes) {

// 获取目标 Node 的 ProducerBatch 集合

List<ProducerBatch> ready = drainBatchesForOneNode(cluster, node, maxSize, now);

// 添加到 batches 中

batches.put(node.id(), ready);

}

return batches;

}

那么应该选择哪些批次来发送呢？

**聪明的读者可能已经知道了，就是遍历每个 ReadyNodes 节点下面的每个 TopicParititon 队列的首个 Batch**。

1.  如果首个 Batch 属于重试状态，并且还没有达到重试间隔时间 retry.backoff.ms，则该 TopicParititon 队列会被忽略掉。
2.  如果 首个 Batch 为空的话，则该 TopicParititon 队列也会被忽略掉。
3.  如果该批次中的总 Batch 大小 > max.request.size，则会终止此次遍历，并记录当前遍历到的位置，等下次再次发送的时候从上一次结束的位置进行遍历。
4.  一次 Request 最多只会完整的遍历一遍，但是遍历完一遍所有的 TopicParititon 队列之后还没有写满 max.request.size，那么也不会再重新遍历。

遍历源码如下：

// 获取目标 Node 的 ProducerBatch 集合

private List<ProducerBatch> drainBatchesForOneNode(Cluster cluster, Node node, int maxSize, long now) {

int size \= 0;

// 1. 获取当前 Node 上的所有分区集合

List<PartitionInfo> parts = cluster.partitionsForNode(node.id());

// 2. 记录发往目标 Node 的 ProducerBatch 集合

List<ProducerBatch> ready = new ArrayList<>();

/\* to make starvation less likely this loop doesn't start at 0 \*/

// drainIndex 是 batches 的下标，记录上次发送停止时的位置，下次继续从此位置开始发送。如果始终从

// 索引0的队列开始发送，可能会出现一直只发送前几个分区的消息的情况，造成其他分区饥饿。

int start \= drainIndex = drainIndex % parts.size();

do {

// 3.获取partition的元数据

PartitionInfo part \= parts.get(drainIndex);

TopicPartition tp \= new TopicPartition(part.topic(), part.partition());

this.drainIndex = (this.drainIndex + 1) % parts.size();

// Only proceed if the partition has no in-flight batches.

if (isMuted(tp))

continue;

// 4.获取主题分区对应的Deque

Deque<ProducerBatch> deque = getDeque(tp);

if (deque == null)

continue;

synchronized (deque) {

// invariant: !isMuted(tp,now) && deque != null

// 获取ArrayDeque中第一个ProducerBatch对象

ProducerBatch first \= deque.peekFirst();

if (first == null)

continue;

// first != null

// 重试操作的话，需要检查是否已经等待了足够的退避时间

boolean backoff \= first.attempts() > 0 && first.waitedTimeMs(now) < retryBackoffMs;

// Only drain the batch if it is not during backoff period.

if (backoff)

continue;

if (size + first.estimatedSizeInBytes() > maxSize && !ready.isEmpty()) {

// 此次请求要发送的数据量已满，结束循环

break;

} else {

if (shouldStopDrainBatchesForPartition(first, tp))

break;

....

// 5. 获取ArrayDeque中第一个ProducerBatch

ProducerBatch batch \= deque.pollFirst();

if (producerIdAndEpoch != null && !batch.hasSequence()) {

... // 事务相关的处理

}

// 6. 关闭底层输出流，将ProducerBatch设置成只读状态

batch.close();

size += batch.records().sizeInBytes();

// 7. 将 ProducerBatch 记录到 ready 集合中

ready.add(batch);

// 8. 修改 ProducerBatch的drainedMs 标记

batch.drained(now);

}

}

} while (start != drainIndex);

return ready;

}

## **5.5 选择负载最小的 Node**

最后在调用 NetworkClient.poll() 里面 maybeUpdate() 方法会获取当前 Node 中负载最小的节点进行发起网络请求，如果所有节点都满负载的话则请求不会被发起。

**那么如何判断哪个节点负载最小呢？**

这里通过每个节点的 InflightRequests 集合里面的最小数量进行判断，它表示当前正在发起的请求，但还没有收到响应的请求数量，如下图所示：

![](https://article-images.zsxq.com/Fhazq4J_au5vbIRaJW7sf4qEVaBU)

它是通过配置 [max.in.flight.requests.per.connection](http://max.in.flight.requests.per.connection/) 进行设置的，其默认值为5。 也就是说每个客户端对每个Node 最多同时能发起 5 个未完成的请求。 如果超时这个数量就会等待请求完成并释放额度后才可以发起新的请求。

相关配置如下：

属性：max.in.flight.requests.per.connection

描述：每个客户端对每个Node发起请求的最大并发数。

默认值：5

## **5.6 处理响应 Response**

上面几个步骤得到了 Map<Integer, List<ProducerBatch\>> batches ，即 NodeId 和对应要发送到该 Node 节点的 Request 请求对应的 ProducerBatch 列表，发送成功之后，会返回 Response，根据 Response 情况进行不同的处理。

##   
**06 Sender 子线程收发消息全过程**

详情请点击

[【生产者源码分析系列第八篇】图解 Kafka 源码之 Sender 线程架构设计](https://articles.zsxq.com/id_4xz5pgx1wi2g.html)

[【生产者源码分析系列第九篇】图解 Kafka 源码之 NetworkClient 网络通信组件架构设计](https://articles.zsxq.com/id_k2pnfv2xq2wb.html)

[【生产者源码分析第十篇】图解 Kafka 源码之客户端如何选择合适的网络进行请求](https://articles.zsxq.com/id_n5c1gmsa5zlx.html)

Sender 子线程的处理流程可以分为两大部分：「**发送请求**」、「**接收响应结果**」。

## **06.1 发送请求**

从 runOnce 方法可以得出发送请求也分两步：「**消息预发送**」、「**真正的网络发送**」。

void runOnce() {

// 1. 把消息缓存在 KafkaChannel 的 Send 字段里。

long pollTimeout \= sendProducerData(currentTimeMs);

// 2. 读取消息实现真正的网络发送

client.poll(pollTimeout, currentTimeMs);

}

## **06.2 接收响应结果**

等 Sender 线程收到 Broker 端的响应结果后，会根据响应结果分情况进行处理。

![](https://article-images.zsxq.com/Fjgcq-3FbA05SJ2982lmOLfAJDKH)

## **06.3 收发流程时序图**

![](https://article-images.zsxq.com/FgploeCGlHYusKHPNfRyFB_9QLmH)

## **07 整体发送流程总结**

最后通过一张图来总结下 KafkaProducer 的初始化和整个发送流程：

  
![](https://article-images.zsxq.com/Fq0RByMxbY5V8OMQjmn-bxQm9w4I)