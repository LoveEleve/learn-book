大家好，我是**华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端整体流程总结篇**」，今天我们开启消费端源码的征程，这是第一篇来深度聊聊「**Kafka 消费者初始化的流程**」，看看 Kafka 消费者是如何被初始化出来的。

![](https://article-images.zsxq.com/FrQPr54YT3eXjnSCAgqVygIWj88Z)

## **01 总体概述**

从今天开始我将以「**Kafka 3.0**」版本为主，通过「**场景驱动**」的方式带大家一点点的对 Kafka 源码进行深度剖析，正式开启「**Kafka 消费者源码之旅**」，跟我一起来掌握 Kafka 源码核心架构设计思想吧。

今天这篇我们先来聊聊 Kafka 消费者初始化时用到的核心组件以及消费的核心流程，带你梳理消费者初始化整体的源码分析脉络。

我们都知道在 Kafka 中，我们把消费消息的一方称为「**消费者**」即 Consumer，它是 Kafka 核心组件之一。那么这些消费者消费消息时是如何从 Kafka 服务端拉取数据的呢？初始化过程是怎么样的呢？接下来会逐一讲解说明。

本文涉及的源码：

「**KafkaConsumer**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/KafkaConsumer.java](https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/KafkaConsumer.java)

「**SubscriptionState**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/SubscriptionState.java](https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/SubscriptionState.java)

「**Metadata**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/Metadata.java](https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/Metadata.java)

其关系图如下：

![](https://article-images.zsxq.com/Fh4mTNGKH--y9fXASoMvaOmu17co)

## **02消费者初始化核心组件及流程剖析**

我们先从消费者客户端构造KafkaConsumer开始讲起：

public class Consumer extends ShutdownableThread {

private final KafkaConsumer<Integer, String> consumer;

private final String topic;

private final String groupId;

private final int numMessageToConsume;

private int messageRemaining;

private final CountDownLatch latch;

public Consumer(final String topic,

final String groupId,

final Optional<String> instanceId,

final boolean readCommitted,

final int numMessageToConsume,

final CountDownLatch latch) {

this.groupId = groupId;

// 1、创建消费者的配置对象和设置配置对象的属性

Properties props \= new Properties();

// 2、kafka集群服务器地址和端口

props.put(ConsumerConfig.BOOTSTRAP\_SERVERS\_CONFIG, KafkaProperties.KAFKA\_SERVER\_URL + ":" + KafkaProperties.KAFKA\_SERVER\_PORT);

// 3、配置消费者组id， 必须

props.put(ConsumerConfig.GROUP\_ID\_CONFIG, groupId);

instanceId.ifPresent(id -> props.put(ConsumerConfig.GROUP\_INSTANCE\_ID\_CONFIG, id));

// 4、自动提交偏移量,每次在调用 KafkaConsumer.poll 方法时都会检测是否需要自动提交，并提交上次 poll 方法返回的最后一个消息的 offset

props.put(ConsumerConfig.ENABLE\_AUTO\_COMMIT\_CONFIG, "true");

// 5、反序列化，必须

props.put(ConsumerConfig.KEY\_DESERIALIZER\_CLASS\_CONFIG, "org.apache.kafka.common.serialization.IntegerDeserializer");

props.put(ConsumerConfig.VALUE\_DESERIALIZER\_CLASS\_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

if (readCommitted) {

props.put(ConsumerConfig.ISOLATION\_LEVEL\_CONFIG, "read\_committed");

}

props.put(ConsumerConfig.AUTO\_OFFSET\_RESET\_CONFIG, "earliest");

// 重点1 这次重点分析这块 创建一个 KafkaConsumer 对象，这个对象负责与 Kafka 集群进行通信。配置 KafkaConsumer 对象，包括指定 Kafka 集群的地址、消费者组ID、序列化器和反序列化器等参数。

consumer = new KafkaConsumer<>(props);

}

public void doWork() {

try {

// 订阅一个或多个主题，通过调用 KafkaConsumer.subscribe() 方法并指定需要订阅的主题名称列表来实现。这个方法会发送一次订阅请求到Kafka集群，Kafka会返回订阅成功的主题列表。

consumer.subscribe(Collections.singletonList(this.topic), this);

// 开始拉取数据。这个方法会向 Kafka 集群发送拉取请求，然后等待 Kafka 返回数据。返回的数据将被存储在内存中的缓冲区中，等待消费者处理。

ConsumerRecords<Integer, String> records = consumer.poll(Duration.ofSeconds(1));

for (ConsumerRecord<Integer, String> record : records) {

System.out.println(groupId + " received message : from partition " + record.partition() + ", (" + record.key() + ", " + record.value() + ") at offset " + record.offset());

}

messageRemaining -= records.count();

if (messageRemaining <= 0) {

System.out.println(groupId + " finished reading " + numMessageToConsume + " messages");

latch.countDown();

}

} catch (Exception e) {

// ....

} finally {

//shutdown();

}

}

上面代码主要做了3件事情：

1.  初始化 [KafkaConsumer](http://kafkaconsumer/) 实例。
2.  调用 [KafkaConsumer.subscribe()](http://kafkaconsumer.subscribe\(\)/) 订阅主题。
3.  调用 [KafkaConsumer.poll()](http://kafkaconsumer.poll\(\)/) 拉取数据。

以上的消费者客户端是 Kafka 新版 API 通过 Java 重构的设计，老版是基于 Scala 的不稳定，后续都通过 Java 进行了重构。

![](https://article-images.zsxq.com/FgUAGZWk9IPmC7d3zIG8Lwr2V5_W)

![](https://article-images.zsxq.com/Fr13HwRz3b4fNlybUB-mLflNF7sv)

## **2.1消费者初始化入口**

待构造完[KafkaConsumer](http://kafkaconsumer/)就正式进入消费者源码的入口了，如下图所示：

  
![](https://article-images.zsxq.com/FvOkp8Ql6f8KMefWM-aKcg2iYAHR)

接下来我们分析一下[KafkaConsumer](http://kafkaconsumer/)的源码， 先看下该类里面的「**重要字段**」：

  
![](https://article-images.zsxq.com/FjxvZVh4VUFsCiCTHE-6Jtt_ML9w)

public class KafkaConsumer<K, V> implements Consumer<K, V> {

private static final String CLIENT\_ID\_METRIC\_TAG \= "client-id";

private static final long NO\_CURRENT\_THREAD \= -1L;

private static final String JMX\_PREFIX \= "kafka.consumer";

// 默认关闭超时时间为 30 秒

static final long DEFAULT\_CLOSE\_TIMEOUT\_MS \= 30 \* 1000;

// Visible for testing

final Metrics metrics;

// 消费端监控

final KafkaConsumerMetrics kafkaConsumerMetrics;

private Logger log;

// 消费者客户端Id

private final String clientId;

// 消费者组id

private final Optional<String> groupId;

// 消费者组协调器

private final ConsumerCoordinator coordinator;

// key的反序列化器

private final Deserializer<K> keyDeserializer;

// value的序列化器

private final Deserializer<V> valueDeserializer;

// 消息获取器

private final Fetcher<K, V> fetcher;

// 消费者拦截器

private final ConsumerInterceptors<K, V> interceptors;

// 事务隔离级别

private final IsolationLevel isolationLevel;

private final Time time;

// 消费者网络客户端

private final ConsumerNetworkClient client;

// 订阅状态

private final SubscriptionState subscriptions;

// 消费者元数据

private final ConsumerMetadata metadata;

// 重试间隔时间

private final long retryBackoffMs;

// 请求超时时间

private final long requestTimeoutMs;

// 默认 API 超时时间

private final int defaultApiTimeoutMs;

// 消费者是否已关闭

private volatile boolean closed \= false;

// 消费者分配器列表

private List<ConsumerPartitionAssignor> assignors;

// 记录当前访问 KafkaConsumer 的线程 ID

private final AtomicLong currentThread \= new AtomicLong(NO\_CURRENT\_THREAD);

// 允许获取 currentThread 的线程进行重入访问次数

private final AtomicInteger refcount \= new AtomicInteger(0);

// 在元数据更新期间缓存订阅哈希值和所有抓取位置，以避免重复扫描订阅项

private boolean cachedSubscriptionHashAllFetchPositions;

....

}

**重要且核心字段含义如下：**  

1.  **clientId**：消费者客户端Id。
2.  **groupId**：消费者组 Id。
3.  **coordinator**：消费者组协调器，负责分配 Consumer 和 Partition 的对应关系，当 Partition 或是 Consumer 发生变更时，会触发 rebalance（重分配）过程，重新分配 Consumer 与 Partition 的对应关系。
4.  **keyDeserializer**: key的反序列化器。
5.  **valueDeserializer**： value 的反序列化器。
6.  **fetcher**： 消息获取器，主要功能是发送 FetchRequest 请求，获取指定的消息集合，处理 FetchResponse，更新消费位置。
7.  **interceptors**： 消费者拦截器。
8.  **isolationLevel**：事务隔离级别。
9.  **consumerNetworkClient**： 消费者网络客户端，负责消费者和集群中的各个Node节点之间的连接。
10.  **subscriptionState**：订阅状态，SubscriptionState 维护了消费者的消费状态，用来跟踪 TopicPartition 和 offset 对应的关系。
11.  **metadata**：消费者元数据，ConsumerMetadata 记录了整个 Kafka 集群的元信息。
12.  **retryBackoffMs**：重试间隔时间。
13.  **defaultApiTimeoutMs**：默认 API 超时时间。
14.  **consumerPartitionAssignor**：消费者分区分配策略。
15.  **currentThread**：记录当前访问 KafkaConsumer 的线程 ID。

可以看到 [KafkaConsumer](http://kafkaconsumer/) 实现了 [Consumer](http://consumer/) 接口，[Consumer](http://consumer/) 接口有以下 6 个行为:

1.  subscribe 方法: 订阅指定的 Topic，为消费者自动分配分区。
2.  assign 方法: 用户手动订阅指定的 Topic，并且指定消费的分区。此方法与 subscribe 方法互斥，在后面会详细介绍是如何实现互斥的。
3.  commit 方法: 提交消贵者已经消费完成的 offset。
4.  seek 方法: 指定消费者起始消费的位置。
5.  poll 方法: 负责从服务端获取消息。
6.  pause、resume 方法: 暂停/ 继续 Consumer，暂停后 poll 方法会返回空。

接下来我们看下[KafkaConsumer](http://kafkaconsumer/) 的构造初始化方法，来剖析消费者消费消息的过程中涉及到的「**核心组件**」。

KafkaConsumer(ConsumerConfig config, Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer) {

try {

// 1、初始化消费组的平衡配置类

GroupRebalanceConfig groupRebalanceConfig \= new GroupRebalanceConfig(config,

GroupRebalanceConfig.ProtocolType.CONSUMER);

// 2、获取消费组id

this.groupId = Optional.ofNullable(groupRebalanceConfig.groupId);

// 3、获取消费者客户端Id

this.clientId = config.getString(CommonClientConfigs.CLIENT\_ID\_CONFIG);

// 4、初始化日志

LogContext logContext;

// If group.instance.id is set, we will append it to the log context.

if (groupRebalanceConfig.groupInstanceId.isPresent()) {

logContext = new LogContext("\[Consumer instanceId=" + groupRebalanceConfig.groupInstanceId.get() + ", clientId=" + clientId + ", groupId=" + groupId.orElse("null") + "\] ");

} else {

logContext = new LogContext("\[Consumer clientId=" + clientId + ", groupId=" + groupId.orElse("null") + "\] ");

}

this.log = logContext.logger(getClass());

// 5、覆盖自动提交 offset

boolean enableAutoCommit \= config.maybeOverrideEnableAutoCommit();

groupId.ifPresent(groupIdStr -> {

if (groupIdStr.isEmpty()) {

log.warn("Support for using the empty group id by consumers is deprecated and will be removed in the next major release.");

}

});

log.debug("Initializing the Kafka consumer");

// 7、配置超时时间和监控信息

this.requestTimeoutMs = config.getInt(ConsumerConfig.REQUEST\_TIMEOUT\_MS\_CONFIG);

this.defaultApiTimeoutMs = config.getInt(ConsumerConfig.DEFAULT\_API\_TIMEOUT\_MS\_CONFIG);

this.time = Time.SYSTEM;

this.metrics = buildMetrics(config, time, clientId);

// 8、重试退避时间

this.retryBackoffMs = config.getLong(ConsumerConfig.RETRY\_BACKOFF\_MS\_CONFIG);

// 9、拦截器

List<ConsumerInterceptor<K, V>> interceptorList = (List) config.getConfiguredInstances(

ConsumerConfig.INTERCEPTOR\_CLASSES\_CONFIG,

ConsumerInterceptor.class,

Collections.singletonMap(ConsumerConfig.CLIENT\_ID\_CONFIG, clientId));

this.interceptors = new ConsumerInterceptors<>(interceptorList);

// 10、序列化

if (keyDeserializer == null) {

this.keyDeserializer = config.getConfiguredInstance(ConsumerConfig.KEY\_DESERIALIZER\_CLASS\_CONFIG, Deserializer.class);

this.keyDeserializer.configure(config.originals(Collections.singletonMap(ConsumerConfig.CLIENT\_ID\_CONFIG, clientId)), true);

} else {

config.ignore(ConsumerConfig.KEY\_DESERIALIZER\_CLASS\_CONFIG);

this.keyDeserializer = keyDeserializer;

}

if (valueDeserializer == null) {

this.valueDeserializer = config.getConfiguredInstance(ConsumerConfig.VALUE\_DESERIALIZER\_CLASS\_CONFIG, Deserializer.class);

this.valueDeserializer.configure(config.originals(Collections.singletonMap(ConsumerConfig.CLIENT\_ID\_CONFIG, clientId)), false);

} else {

config.ignore(ConsumerConfig.VALUE\_DESERIALIZER\_CLASS\_CONFIG);

this.valueDeserializer = valueDeserializer;

}

// 11、位移重置的策略

OffsetResetStrategy offsetResetStrategy \= OffsetResetStrategy.valueOf(config.getString(ConsumerConfig.AUTO\_OFFSET\_RESET\_CONFIG).toUpperCase(Locale.ROOT));

this.subscriptions = new SubscriptionState(logContext, offsetResetStrategy);

// 集群资源监听器

ClusterResourceListeners clusterResourceListeners \= configureClusterResourceListeners(keyDeserializer,

valueDeserializer, metrics.reporters(), interceptorList);

// 12、消费者的元数据

this.metadata = new ConsumerMetadata(retryBackoffMs,

config.getLong(ConsumerConfig.METADATA\_MAX\_AGE\_CONFIG),

!config.getBoolean(ConsumerConfig.EXCLUDE\_INTERNAL\_TOPICS\_CONFIG),

config.getBoolean(ConsumerConfig.ALLOW\_AUTO\_CREATE\_TOPICS\_CONFIG),

subscriptions, logContext, clusterResourceListeners);

// 13、集群 broker 地址

List<InetSocketAddress> addresses = ClientUtils.parseAndValidateAddresses(

config.getList(ConsumerConfig.BOOTSTRAP\_SERVERS\_CONFIG), config.getString(ConsumerConfig.CLIENT\_DNS\_LOOKUP\_CONFIG));

this.metadata.bootstrap(addresses);

String metricGrpPrefix \= "consumer";

FetcherMetricsRegistry metricsRegistry \= new FetcherMetricsRegistry(Collections.singleton(CLIENT\_ID\_METRIC\_TAG), metricGrpPrefix);

// 14、ChannelBuilder

ChannelBuilder channelBuilder \= ClientUtils.createChannelBuilder(config, time, logContext);

// 15、事务隔离级别

this.isolationLevel = IsolationLevel.valueOf(config.getString(ConsumerConfig.ISOLATION\_LEVEL\_CONFIG).toUpperCase(Locale.ROOT));

Sensor throttleTimeSensor \= Fetcher.throttleTimeSensor(metrics, metricsRegistry);

// 16、心跳时间

int heartbeatIntervalMs \= config.getInt(ConsumerConfig.HEARTBEAT\_INTERVAL\_MS\_CONFIG);

ApiVersions apiVersions \= new ApiVersions();

// 17、网络通讯客户端

NetworkClient netClient \= new NetworkClient(

new Selector(config.getLong(ConsumerConfig.CONNECTIONS\_MAX\_IDLE\_MS\_CONFIG), metrics, time, metricGrpPrefix, channelBuilder, logContext),

this.metadata,

clientId,

100, // a fixed large enough value will suffice for max in-flight requests

config.getLong(ConsumerConfig.RECONNECT\_BACKOFF\_MS\_CONFIG),

config.getLong(ConsumerConfig.RECONNECT\_BACKOFF\_MAX\_MS\_CONFIG),

config.getInt(ConsumerConfig.SEND\_BUFFER\_CONFIG),

config.getInt(ConsumerConfig.RECEIVE\_BUFFER\_CONFIG),

config.getInt(ConsumerConfig.REQUEST\_TIMEOUT\_MS\_CONFIG),

config.getLong(ConsumerConfig.SOCKET\_CONNECTION\_SETUP\_TIMEOUT\_MS\_CONFIG),

config.getLong(ConsumerConfig.SOCKET\_CONNECTION\_SETUP\_TIMEOUT\_MAX\_MS\_CONFIG),

time,

true,

apiVersions,

throttleTimeSensor,

logContext);

this.client = new ConsumerNetworkClient(

logContext,

netClient,

metadata,

time,

retryBackoffMs,

config.getInt(ConsumerConfig.REQUEST\_TIMEOUT\_MS\_CONFIG),

heartbeatIntervalMs); //Will avoid blocking an extended period of time to prevent heartbeat thread starvation

// 18、消费者分区分配策略

this.assignors = ConsumerPartitionAssignor.getAssignorInstances(

config.getList(ConsumerConfig.PARTITION\_ASSIGNMENT\_STRATEGY\_CONFIG),

config.originals(Collections.singletonMap(ConsumerConfig.CLIENT\_ID\_CONFIG, clientId))

);

// 19、消费者组协调器

this.coordinator = !groupId.isPresent() ? null :

new ConsumerCoordinator(groupRebalanceConfig,

logContext,

this.client,

assignors,

this.metadata,

this.subscriptions,

metrics,

metricGrpPrefix,

this.time,

enableAutoCommit,

// 自动提交偏移量的时间间隔。

config.getInt(ConsumerConfig.AUTO\_COMMIT\_INTERVAL\_MS\_CONFIG),

this.interceptors,

config.getBoolean(ConsumerConfig.THROW\_ON\_FETCH\_STABLE\_OFFSET\_UNSUPPORTED));

// 20、拉取器(消息拉取、位移、元数据)

this.fetcher = new Fetcher<>(

logContext,

this.client,

config.getInt(ConsumerConfig.FETCH\_MIN\_BYTES\_CONFIG),

config.getInt(ConsumerConfig.FETCH\_MAX\_BYTES\_CONFIG),

config.getInt(ConsumerConfig.FETCH\_MAX\_WAIT\_MS\_CONFIG),

config.getInt(ConsumerConfig.MAX\_PARTITION\_FETCH\_BYTES\_CONFIG),

config.getInt(ConsumerConfig.MAX\_POLL\_RECORDS\_CONFIG),

config.getBoolean(ConsumerConfig.CHECK\_CRCS\_CONFIG),

config.getString(ConsumerConfig.CLIENT\_RACK\_CONFIG),

this.keyDeserializer,

this.valueDeserializer,

this.metadata,

this.subscriptions,

metrics,

metricsRegistry,

this.time,

this.retryBackoffMs,

this.requestTimeoutMs,

isolationLevel,

apiVersions);

this.kafkaConsumerMetrics = new KafkaConsumerMetrics(metrics, metricGrpPrefix);

config.logUnused();

AppInfoParser.registerAppInfo(JMX\_PREFIX, clientId, metrics, time.milliseconds());

log.debug("Kafka consumer initialized");

} catch (Throwable t) {

// call close methods if internal objects are already constructed; this is to prevent resource leak. see KAFKA-2121

// we do not need to call \`close\` at all when \`log\` is null, which means no internal objects were initialized.

if (this.log != null) {

close(0, true);

}

// now propagate the exception

throw new KafkaException("Failed to construct kafka consumer", t);

}

}

下面通过一张图来描述KafkaConsumer的初始化源码过程：

  
![](https://article-images.zsxq.com/lnbdw9omMolWYt9lDjK5jbbBqZws)

对比一下采用新版本构造的「**生产者**」和「**消费者**」客户端，可以发现两者共同点是都有「**元数据 Metadata**」和 「**网络客户端 NetworkClient**」。

关于生产者初始化流程：[【生产者源码分析系列第一篇】图解 Kafka 生产者初始化核心流程](https://articles.zsxq.com/id_8nv25wvidj96.html)

不同的是「**生产者**」有「**消息累加器 RecordAccumulator**」、「**发送线程 Sender**」、「**分区器 Partitioner**」，而「**消费者**」有 「**订阅状态 SubscriptionState**」、「**拉取线程 Fetcher**」、「**分区分配 ConsumerPartitionAssignor**」、「**消费者协调器 ConsumerCoordinator**」。

可以看到「**生产者**」和「**消费者**」都使用了同一套「**网络客户端 NetworkClient**」通信机制，即采用了基于选择器模式的网络客户端。它会分别用于生产者的「**发送线程 Sender**」和消费者的「**拉取线程 Fetcher**」， 「**生产者**」在创建 KafkaProducer 时就立即启动「**发送线程 Sender**」，但「**消费者**」创建 KafkaConsumer 时并不会立即启动「**拉取线程 Fetcher**」，主要是因为 「**拉取线程 Fetcher**」需要有分区才可以正常运行。

「**生产者**」发送生产请求和「**消费者**」发送拉取请求，最后都是通过「**网络客户端 NetworkClient**」的选择器轮询将客户端请求发送给服务端。

另外「**生产者**」和「**消费者**」还要在本地保存服务端集群的「**元数据 Metadata**」（其内部保存了集群的节点列表、主题、分区的对应关系），否则在需要这些信息时就只能通过向服务端发送相关请求来完成。

  
![](https://article-images.zsxq.com/lvSD08r1_wzKYrPmMc5RHEzP72gG)

## **2.2消费者订阅主题**

// 设置订阅的消费主题。

consumer.subscribe(Collections.singletonList(this.topic));

可以看到直接调用 [KafkaConsumer.subscribe()](http://kafkaconsumer.subscribe\(\)/) 来订阅主题，如下：

@Override

public void subscribe(Collection<String> topics) {

subscribe(topics, new NoOpConsumerRebalanceListener());

}

@Override

public void subscribe(Collection<String> topics, ConsumerRebalanceListener listener) {

acquireAndEnsureOpen();

try {

maybeThrowInvalidGroupIdException();

// 订阅的主题为 null，直接抛异常

if (topics == null)

throw new IllegalArgumentException("Topic collection to subscribe to cannot be null");

if (topics.isEmpty()) {

// treat subscribing to empty topic list as the same as unsubscribing

this.unsubscribe();

} else {

for (String topic : topics) {

// 如果为空，抛异常

if (Utils.isBlank(topic))

throw new IllegalArgumentException("Topic collection to subscribe to cannot contain null or empty topic");

}

throwIfNoAssignorsConfigured();

// 考虑到多次订阅主题不一致的情况，调用 Fetcher#clearBufferedDataForUnassignedTopics() 将已经接收到的不在本次订阅的 topic 列表中的数据清除掉。

fetcher.clearBufferedDataForUnassignedTopics(topics);

log.info("Subscribed to topic(s): {}", Utils.join(topics, ", "));

// 调用 SubscriptionState#subscribe() 方法重置订阅的 topic 列表（判断是否需要更新订阅的主题，如果更新主题，则更新元数据信息，监听器）

if (this.subscriptions.subscribe(new HashSet<>(topics), listener))

// 订阅和以前不一致，则调用 Metadata#requestUpdateForNewTopics() 方法设置更新元数据的标识位 needPartialUpdate 为 true，则后续消费者将发送更新元数据请求

metadata.requestUpdateForNewTopics();

}

} finally {

release();

}

}

可以看到重点会调用 [this.subscriptions.subscribe()](http://this.subscriptions.subscribe\(\)/) 来订阅主题，如下：

// SubscriptionState.java

public class SubscriptionState {

/\* the type of subscription \*/

private SubscriptionType subscriptionType;

/\* User-provided listener to be invoked when assignment changes \*/

private ConsumerRebalanceListener rebalanceListener;

public synchronized boolean subscribe(Set<String> topics, ConsumerRebalanceListener listener) {

// 注册重平衡监听器

registerRebalanceListener(listener);

// 按照设置的主题开始订阅，自动分配分区

setSubscriptionType(SubscriptionType.AUTO\_TOPICS);

// 如果订阅的主题和以前订阅的一致，就不需要修改订阅信息。如果不一致，就需要修改

return changeSubscription(topics);

}

private void registerRebalanceListener(ConsumerRebalanceListener listener) {

if (listener == null)

throw new IllegalArgumentException("RebalanceListener cannot be null");

// 注册重平衡监听器

this.rebalanceListener = listener;

}

private void setSubscriptionType(SubscriptionType type) {

if (this.subscriptionType == SubscriptionType.NONE)

// 按照设置的主题开始订阅，自动分配分区

this.subscriptionType = type;

else if (this.subscriptionType != type)

throw new IllegalStateException(SUBSCRIPTION\_EXCEPTION\_MESSAGE);

}

private boolean changeSubscription(Set<String> topicsToSubscribe) {

// 如果订阅的主题和以前订阅的一致，就不需要修改订阅信息。

if (subscription.equals(topicsToSubscribe))

return false;

// 如果不一致，就需要修改

subscription = topicsToSubscribe;

return true;

}

}

当订阅关系有变化时会调用 [metadata.requestUpdateForNewTopics()](http://metadata.requestupdatefornewtopics\(\)/) 更新元数据信息。

// metadata.java

public synchronized int requestUpdateForNewTopics() {

// Override the timestamp of last refresh to let immediate update.

this.lastRefreshMs = 0;

this.needPartialUpdate = true;

this.requestVersion++;

return this.updateVersion;

}

关于拉取和处理我们会放到下篇进行剖析，这里就不再赘述。

最后附上一张 Kafka consumer 消费流程图如下：

  
![](https://article-images.zsxq.com/FpUV19120W9HGW8At8AA9Ksmb38o)

## **03总结**

这里，我们一起来总结一下这篇文章的重点。

1、通过「**场景驱动**」的方式从消费者调用出发，抛出初始化流程是怎样的?

2、带你梳理了「**Kafka Consumer 初始化源码全貌**」。

3、最后通过一张整体消费流程图来勾勒出消费者消费消息的全貌。