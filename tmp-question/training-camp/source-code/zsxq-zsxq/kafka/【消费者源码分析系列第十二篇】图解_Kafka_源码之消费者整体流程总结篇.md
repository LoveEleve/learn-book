大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了「**\_\_consumer\_offsets 探秘**」，了解了「**\_\_consumer\_offsets 探秘**」存储结构以及写入时机等等。今天我们开启消费端源码的征程，这是第十二篇，我们来总结下「**\_消费者整体流程**」。

![](https://article-images.zsxq.com/FofcNUy9O8zkOoDXh4o0XJk0myXd)

## **01 总体概述**

通过「**场景驱动**」的方式，前面十一篇文章，我们已经从消费者启动，网络通信组件的构建，到消费者组如何启动、消费者组协调器确认，消费者组成员加入，再到消费者数据拉取，最后消费完成后进行位移提交等进行了详细的剖析。

今天我们就来总结下这整个过程。

## **02 消费者示例**

当我们启动一个消费者的时候，如下代码：

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

super("KafkaConsumerExample", false);

this.groupId = groupId;

// 1、初始化配置对象和设置配置对象的属性

Properties props \= new Properties();

// 指定 kafka 集群地址

props.put(ConsumerConfig.BOOTSTRAP\_SERVERS\_CONFIG, KafkaProperties.KAFKA\_SERVER\_URL + ":" + KafkaProperties.KAFKA\_SERVER\_PORT);

// 消费者组 id

props.put(ConsumerConfig.GROUP\_ID\_CONFIG, groupId);

instanceId.ifPresent(id -> props.put(ConsumerConfig.GROUP\_INSTANCE\_ID\_CONFIG, id));

// 自动提交偏移量

props.put(ConsumerConfig.ENABLE\_AUTO\_COMMIT\_CONFIG, "true");

// 自动提交偏移量的时间间隔。

props.put(ConsumerConfig.AUTO\_COMMIT\_INTERVAL\_MS\_CONFIG, "1000");

// consumer group多久收不到 consumer 的心跳就认为consumer不在consumer group里了

props.put(ConsumerConfig.SESSION\_TIMEOUT\_MS\_CONFIG, "30000");

// key的反序列化类

props.put(ConsumerConfig.KEY\_DESERIALIZER\_CLASS\_CONFIG, "org.apache.kafka.common.serialization.IntegerDeserializer");

//value的反序列化类

props.put(ConsumerConfig.VALUE\_DESERIALIZER\_CLASS\_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

if (readCommitted) {

props.put(ConsumerConfig.ISOLATION\_LEVEL\_CONFIG, "read\_committed");

}

// 设置重启后从哪里开始消费

props.put(ConsumerConfig.AUTO\_OFFSET\_RESET\_CONFIG, "earliest");

// 2、实例化新的KafkaConsumer类对象。

consumer = new KafkaConsumer<>(props);

// 3、设置用于消费的属性。

// 设置主题

this.topic = topic;

// 设置消费消息的数量

this.numMessageToConsume = numMessageToConsume;

// 剩余消费消息的数量

this.messageRemaining = numMessageToConsume;

// 设置latch，从上层调用者控制消费者

this.latch = latch;

}

KafkaConsumer<Integer, String> get() {

return consumer;

}

@Override

public void doWork() {

// 1、设置订阅的消费主题。

consumer.subscribe(Collections.singletonList(this.topic));

// 2、开始消费主题。

ConsumerRecords<Integer, String> records = consumer.poll(Duration.ofSeconds(1));

// 3、消费到的消息打印出来。

for (ConsumerRecord<Integer, String> record : records) {

System.out.println(groupId + " received message : from partition " + record.partition() + ", (" + record.key() + ", " + record.value() + ") at offset " + record.offset());

}

messageRemaining -= records.count();

// 4、消费完成，上层调用的主线程阻塞解除。

if (messageRemaining <= 0) {

System.out.println(groupId + " finished reading " + numMessageToConsume + " messages");

latch.countDown();

}

}

@Override

public String name() {

return null;

}

@Override

public boolean isInterruptible() {

return false;

}

}

Kafka Consumer 示例代码主要分为两个部分：

1.  KafkaConsumer 类的初始化。
2.  KafkaConsumer 类从服务端拉取消息。

接下来下面我分别梳理一下。

## **2.1 KafkaConsumer 类初始化**

1.  初始化配置对象和设置配置对象的属性。包括实例化 Properties，然后配置相关属性。包括：
2.  指定Kafka集群地址，因为消费者要拉取毕竟要从服务端拉取的，所以要配置服务端的地址，包括ip和端口。服务端的地址是可能是多个。格式如下：[node01:9092, node02:9092, node03:9092](http://about:blank)。
3.  消费组id。因为kafka是按照消费组来分配消费分区的，所以要指定消费者所在的消费组id。
4.  是否自动提交偏移量。消费者成功消费完消息后要提交消息在分区的偏移量，这样再次消费时能从已提交偏移量之后继续进行消费。提交偏移量分为两种，自动提交和手动提交，这里设置为true就是自动提交，否则就是手动提交。
5.  自动提交偏移量的时间间隔。如果用户选择了自动提交偏移量那么就可以在这里设置向服务端consumer group自动提交偏移量的事件间隔。如果设置的间隔时间过长会造成consumer group数据滞后造成重复消费。如果设置的间隔时间过短，会造成consumer group压力过大，可以根据实际情况进行设置。
6.  设置key的反序列化类和value的反序列化类。这里的反序列化类要对应生产者发送消息的序列化类否则反序列化会失败。
7.  设置重启后从哪里开始消费。包括earliest,latest,earliest指从分区第一个的offset消费。latest指从分区最后一个offset消费。
8.  根据配置的Properties属性实例化 KafkaConsumer，同时KafkaConsumer会初始化一下核心组件。初始化了哪些组件，组件是如何初始化的，本文后面会给大家介绍。
9.  设置用于消费的一些属性。包括：
10.  配置要消费的主题。
11.  这次要消费的记录数。因为仅仅是个消费实例不需要消费太多的记录。
12.  配置Latch。用于上层调用方线程和消费线程的同步。

![](https://article-images.zsxq.com/lmqbmJFAhOkeCQXWXS3XpebyYhVc)

详细请点击：[【消费者源码分析系列第一篇】图解 Kafka 源码之消费者初始化流程](https://articles.zsxq.com/id_3g80nohn4g6s.html)

初始化大体就剖析完了，我们接着剖析消费者拉取消息的部分。

## **2.2 KafkaConsumer 类开始从服务端拉取数据**

拉取消息源码在 [dowork()](http://dowork\(\)/) 方法里。我把步骤给你说明一下。

1.  设置订阅的主题。
2.  开始消费主题的一批消息。消费者消费一次是一批消费的，这样的好处是减少了网络i/o的负载。
3.  把消费到的消息打印出来。
4.  当拉取的消息的数量达到了设置的消息数，就执行 [latch.countDown()](http://latch.countdown\(\)/) 解除上层的业务主线程的阻塞。

好了，接下来我们将分别剖析 KafkaConsumer 对核心组件的初始化、消费者组协调器确认，消费者拉取消息、消费者组重平衡、以及位移提交等过程。

## **2.3 KafkaConsumer 对核心组件的初始化**

public KafkaConsumer(Map<String, Object> configs,

Deserializer<K> keyDeserializer,

Deserializer<V> valueDeserializer) {

ConsumerConfig config \= new ConsumerConfig(ConsumerConfig.appendDeserializerToConfig(configs, keyDeserializer, valueDeserializer));

try {

GroupRebalanceConfig groupRebalanceConfig \= new GroupRebalanceConfig(config,

GroupRebalanceConfig.ProtocolType.CONSUMER);

// 获取消费组id

this.groupId = Optional.ofNullable(groupRebalanceConfig.groupId);

// 获取消费者id

this.clientId = config.getString(CommonClientConfigs.CLIENT\_ID\_CONFIG);

....

// 设置key和value的反序列化类。

if (keyDeserializer == null) {

this.keyDeserializer = config.getConfiguredInstance(ConsumerConfig.KEY\_DESERIALIZER\_CLASS\_CONFIG, Deserializer.class);

this.keyDeserializer.configure(config.originals(Collections.singletonMap(ConsumerConfig.CLIENT\_ID\_CONFIG, clientId)), true);

} else {

config.ignore(ConsumerConfig.KEY\_DESERIALIZER\_CLASS\_CONFIG);

this.keyDeserializer = keyDeserializer;

}

....

// 设置重启后消费策略。

OffsetResetStrategy offsetResetStrategy \= OffsetResetStrategy.valueOf(config.getString(ConsumerConfig.AUTO\_OFFSET\_RESET\_CONFIG).toUpperCase(Locale.ROOT));

// 初始化订阅状态。

this.subscriptions = new SubscriptionState(logContext, offsetResetStrategy);

ClusterResourceListeners clusterResourceListeners \= configureClusterResourceListeners(keyDeserializer,

valueDeserializer, metrics.reporters(), interceptorList);

// 初始化消费者元数据

this.metadata = new ConsumerMetadata(retryBackoffMs,

config.getLong(ConsumerConfig.METADATA\_MAX\_AGE\_CONFIG),

!config.getBoolean(ConsumerConfig.EXCLUDE\_INTERNAL\_TOPICS\_CONFIG),

config.getBoolean(ConsumerConfig.ALLOW\_AUTO\_CREATE\_TOPICS\_CONFIG),

subscriptions, logContext, clusterResourceListeners);

....

ApiVersions apiVersions \= new ApiVersions();

// 初始化底层通信模块

NetworkClient netClient \= new NetworkClient(

new Selector(config.getLong(ConsumerConfig.CONNECTIONS\_MAX\_IDLE\_MS\_CONFIG), metrics, time, metricGrpPrefix, channelBuilder, logContext),

this.metadata,

clientId,

100, config.getLong(ConsumerConfig.RECONNECT\_BACKOFF\_MS\_CONFIG),

config.getLong(ConsumerConfig.RECONNECT\_BACKOFF\_MAX\_MS\_CONFIG),

config.getInt(ConsumerConfig.SEND\_BUFFER\_CONFIG),

config.getInt(ConsumerConfig.RECEIVE\_BUFFER\_CONFIG),

config.getInt(ConsumerConfig.REQUEST\_TIMEOUT\_MS\_CONFIG),

config.getLong(ConsumerConfig.SOCKET\_CONNECTION\_SETUP\_TIMEOUT\_MS\_CONFIG),

config.getLong(ConsumerConfig.SOCKET\_CONNECTION\_SETUP\_TIMEOUT\_MAX\_MS\_CONFIG),

ClientDnsLookup.forConfig(config.getString(ConsumerConfig.CLIENT\_DNS\_LOOKUP\_CONFIG)),

time,

true,

apiVersions,

throttleTimeSensor,

logContext);

// 初始化消费者通信模块

this.client = new ConsumerNetworkClient(

logContext,

netClient,

metadata,

time,

retryBackoffMs,

config.getInt(ConsumerConfig.REQUEST\_TIMEOUT\_MS\_CONFIG),

heartbeatIntervalMs); //Will avoid blocking an extended period of time to prevent heartbeat thread starvation

this.assignors = getAssignorInstances(config.getList(ConsumerConfig.PARTITION\_ASSIGNMENT\_STRATEGY\_CONFIG), config.originals());

// 初始化协调者模块

// no coordinator will be constructed for the default (null) group id

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

config.getInt(ConsumerConfig.AUTO\_COMMIT\_INTERVAL\_MS\_CONFIG),

this.interceptors,

config.getBoolean(ConsumerConfig.THROW\_ON\_FETCH\_STABLE\_OFFSET\_UNSUPPORTED));

// 初始化获取消息的模块

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

....

}

我给你剖析一下 KafkaConsumer 初始化核心组件的源码：

1.  上一步的初始化过程中我们配置了一些参数数值。这一步根据这些参数真正配置到KafkaConsumer类中。包括获取消费组id、获取消费者id、key 和 value的反序列化类，以及设置重启后消费策略。
2.  根据重启消费策略初始化 [SubscriptionState](http://subscriptionstate/) 订阅状态类。订阅状态类用于保存订阅的主题和分区，以及分区消费的偏移量。
3.  初始化消费者元数据组件。
4.  初始化 [NetworkClient](http://networkclient/) 底层通信模块。因为消费者需要从服务端拉取消息，这样就消费者需要构建这样的组件与服务端进行大量的网络通信。[NetworkClient](http://networkclient/) 初始化底层是封装 Selector 组件的。
5.  初始化 [ConsumerNetworkClient](http://consumernetworkclient/)。[ConsumerNetworkClient](http://consumernetworkclient/) 是基于 [NetworkClient](http://networkclient/) 的有一次封装，消费者直接调用[ConsumerNetworkClient](http://consumernetworkclient/) 就可以完成与服务端的通信。
6.  初始化 [ConsumerCoordinator](http://consumercoordinator/) 对象，服务消费者和服务端的 GroupCoordinator 通信。
7.  初始化 [Fetcher](http://fetcher/) 对象，对 [ConsumerNetworkClient](http://consumernetworkclient/) 进行封装并负责从服务端获取消息。

KafkaConsumer 在初始化过程中就会帮我们把上述的组件都初始化好了。这个初始化的过程涉及的组件比较多，我用下面的一副图来较为形象地展示一下：

![](https://article-images.zsxq.com/FvzfAs0ha_MgPjp0HVLcc5Z8sPAX)

1.  首先，KafkaConsumer 直接调用的组件有两个 ConsumerCoordinator 和 Fetcher。ConsumerCoordinator 用于消费组的管理。Fetcher 用于从服务端拉取消息。
2.  下一层是核心逻辑层，AbstractCoordinator 提供管理消费组的核心逻辑，ConsumerNetworkClient 是为消费组提供网络服务的组件，封装了底层 NetworkClient 类。
3.  再往下一层是网络层，NetworkClient 提供客户端和服务端直接的网络通信。
4.  然后再往下是 Reator 封装，包括前面介绍的组件 Selector、KafkaChannel、TransportLayer。最底层是原生的 java nio。

这里的底层组件跟生产者的组件是公用的，属于 Kafka 网络层组件类。

初始化结束后，KafkaConsumer 需要确认消费者组协调器然后把消息拉取过来进行处理。

## **2.4 消费者组协调器确认**

关于什么是消费者组协调器，下面一张图可以说明，Kafka 集群有 3 个节点，同一个消费者组下有 3 个消费者去消费 Topic：

![](https://article-images.zsxq.com/Fo9D2jBmzHauA-Bk3DKr6EeRfgRS)

详情请点击：[【消费者源码分析系列第四篇】图解 Kafka 源码之初识消费者组及四大请求处理流程](https://articles.zsxq.com/id_b52hrvfqrzp7.html) 、[【消费者源码分析系列第八篇】图解 Kafka 源码之 Coordinator 工作原理](https://articles.zsxq.com/id_5i1rchaq5d5d.html)

## **2.5 KafkaConsumer 拉取消息流程**

拉取消息的过程是调用了 [KafkaConsumer#poll()](http://kafkaconsumer/#poll\(\)%20%E6%96%B9%E6%B3%95) 方法，源码如下：

private ConsumerRecords<K, V> poll(final Timer timer, final boolean includeMetadataInTimeout) {

// 1、KafkaConsumer 是线程不安全的，同时只能一个线程运行，如果有多个线程同时调用 poll() 会抛出异常

acquireAndEnsureOpen();

try {

this.kafkaConsumerMetrics.recordPollStart(timer.currentTimeMs());

// 2、消费者的订阅的主题不能为空，如果没有指定任何主题就抛异常。

if (this.subscriptions.hasNoSubscriptionOrUserAssignment()) {

throw new IllegalStateException("Consumer is not subscribed to any topics or assigned any partitions");

}

// 3、循环拉取消息，直到拉取到了消息或超时。

do {

// 4、可以中断 consumer。

client.maybeTriggerWakeup();

// 5、判断 includeMetadataInTimeout 是 true 还是 false.

if (includeMetadataInTimeout) {

// 6、如果为 true 则更新消费分区的任务元数据，协调器，心跳，消费位置。

updateAssignmentMetadataIfNeeded(timer, false);

} else {

// 7、如果为 false 则更新消费分区的任务元数据，协调器，心跳，消费位置。

while (!updateAssignmentMetadataIfNeeded(time.timer(Long.MAX\_VALUE), true)) {

log.warn("Still waiting for metadata");

}

}

// 7、开始拉取消息，不是真正从 broker 拉取消息，而是从缓存拉取消息。

// 这样设计是一个优化，如果从缓存拉取成功了，consumer 就提前发送下一次的拉取请求，

// 当你的应用在处理刚刚拉取的新纪录的时候，consumer 也同时在后台为你拉取好下一次要用的数据并放在缓存里。等待业务线程拉取。

final Map<TopicPartition, List<ConsumerRecord<K, V>>> records = pollForFetches(timer);

if (!records.isEmpty()) {

if (fetcher.sendFetches() > 0 || client.hasPendingRequests()) {

client.transmitSends();

}

// 8、返回拦截器处理后的消息集合

return this.interceptors.onConsume(new ConsumerRecords<>(records));

}

} while (timer.notExpired());

return ConsumerRecords.empty();

} finally {

release();

this.kafkaConsumerMetrics.recordPollEnd(timer.currentTimeMs());

}

}

我给你剖析一下 poll() 方法的大体步骤：

1.  首先，调用 [acquireAndEnsureOpen()](http://acquireandensureopen\(\)/) 来判断是否有多个线程调用了 KafkaConsumer 类的方法。KafkaConsumer 是一个非线程安全的类，但是有些 poll() 方法不能同时被多个线程访问，因为有些变量是能够在线程间共享的必须保证线程安全。于是 KafkaConsumer 设计了一个轻量级的锁来保证同时只有一个线程进入 KafkaConsumer 类的方法。具体源码如下：

private void acquireAndEnsureOpen() {

// 调用 acquire() 方法尝试获得轻量级锁

acquire();

if (this.closed) {

// 那么 currentThread 什么时候会变成默认值-1L呢？这个是由release()方法控制的，当KafkaConsumer 内的方法结尾的时候都会调用 release()，这个方法首先把 refcount 减一，然后判断 refcount 是否为零，为零说明现在的线程调用K afkaConsumer 内的方法都已经结束了，就把 currentThread 设置为 NO\_CURRENT\_THREAD，这样其他线程才能获得 KafkaConsumer 的轻量级锁。

release();

throw new IllegalStateException("This consumer has already been closed.");

}

}

private void acquire() {

// 获取线程id

long threadId \= Thread.currentThread().getId();

// 这里判断是否抛出多线程访问的异常，同时满足下列两个条件才会抛出异常：

// 1)、判断线程 id 是否是当前线程 id，如果是那么 refcount 加一，表示当前线程又一次访问 poll()方法了。refcount 表示当前线程重入轻量级锁的次数。

// 2)、尝试用 CAS 的方法把当前线程 id 赋值给 currentThread，如果赋值不成功就抛出异常。CAS 的方法的初始值是 NO\_CURRENT\_THREAD 即默认值-1L，也就是说如果 currentThread 已经是线程 id 了是不能赋值成功的。

if (threadId != currentThread.get() && !currentThread.compareAndSet(NO\_CURRENT\_THREAD, threadId))

throw new ConcurrentModificationException("KafkaConsumer is not safe for multi-threaded access");

refcount.incrementAndGet();

}

private void release() {

if (refcount.decrementAndGet() == 0)

currentThread.set(NO\_CURRENT\_THREAD);

}

1.  当线程获得轻量级锁之后，方法会判断消费者是否订阅了主题或分配了分区，如果没有就抛出响应的异常。
2.  循环拉取消息，直到拉取到了消息或超时。
3.  判断是否被中断了，有的方法是可以中断方法的访问的，如close()，如果消费者都关闭了，那么调用poll()方法去拉取消息也就没什么意义了，中断后会抛出异常。
4.  判断 [includeMetadataInTimeout](http://includemetadataintimeout/) 是 true 还是 false。这里一般会设为 true,如果设置了 false,就会走 else 分支，获取元数据时会阻塞一段时间直到元数据返回位置，而如果是 true 获取元数据时不会阻塞。
5.  更新消费分区的任务元数据，协调器，心跳任务已经对应分区提交的偏移量。
6.  从内存中拉取消息。
7.  返回拦截器处理后的消息集合。

整个消费者启动并拉取消息的核心流程可分为以下几步：

1.  首先消费者要确定自己所在的「**消费者组协调器**」的地址并与其建立连接。在这个过程中，如果消费者需要更新 Kafka 集群元数据，则先更新元数据。
2.  接着消费者向「**消费者组协调器**」所在的服务端发送请求「**加入消费者组**」。在这个过程中，「**消费者组协调器**」指定的 Leader 将结合集群元数据与整个消费者组的消费者信息进行分区分配，完成后将分配方案发送给「**消费者组协调器**」。
3.  接着「**消费者组协调器**」将分区分配方案返回给各个消费者，消费者向自己负责的分区所在的 Broker 发起拉消息请求，完成消息消费。
4.  最后消费者会启动一个心跳线程与「**消费者组协调器**」保持连接，如果「**消费者组协调器**」返回消费者组状态变化，则进行重新加入消费者组的重平衡操作。

![](https://article-images.zsxq.com/lkv8LWG903Y2DH4VzWV8E4hupyGm)

整个拉取流程时序图如下：

![](https://article-images.zsxq.com/FmlxKM_HVrJOG_2JOqMB-CwMaNmb)

详情请点击：[【消费者源码分析系列第三篇】图解 Kafka 源码之消费者如何拉取数据的](https://articles.zsxq.com/id_to39nipv3p2g.html)

## **2.6 消费者组状态机**

在消费的过程中，难免会出现消费者离组或者宕机情况出现，势必会造成消费者组状态的流转，因此跟服务端分区和副本状态机类似，在消费者端也有一套状态流转机制，如下图：

![](https://article-images.zsxq.com/Fqq4IxapG4V4bdlMLg8hIubDpmNN)

详情请点击：[【消费者源码分析系列第五篇】图解 Kafka 源码之消费者组状态机流程](https://articles.zsxq.com/id_tpg7iob24t7w.html)

## **2.7 消费者组重平衡机制**

这里我们来剖析下 **Rebalance 的触发条件有三种：**

1.  当 Consumer Group 组成员数量发生变化(主动加入或者主动离组，故障下线等)。
2.  当订阅主题数量发生变化。
3.  当订阅主题的分区数发生变化。

而引发 **Rebalance** 主要有以下 5 个场景：

1.  当有新的消费者加入消费组时。
2.  当有消费者宕机下线时，此时消费者并不一定需要真正下线，例如遇到「**长时间 GC**」、「**网络延迟**」等问题导致消费者长时间没有向 [GroupCoordinator](http://groupcoordinator/) 发送心跳等情况时，[GroupCoordinator](http://groupcoordinator/) 就会认为消费者已经下线。
3.  当有消费者主动退出消费组（发送 LeaveGroupRequest 请求）时。比如：此时客户端调用子 [unsubscrible()](http://unsubscrible\(\)%20/) 方法取消对某些主题的订阅。
4.  当消费组所对应的 [GroupCoorinator](http://groupcoorinator/) 节点发生了变更时。
5.  当消费组内所订阅的任何一个主题或主题分区数量发生了变化时。

详情请点击：[【消费者源码分析系列第九篇】图解 Kafka 源码之消费者重平衡机制流程剖析](https://articles.zsxq.com/id_dh8qrsh4fxud.html)

## **2.8 消费者位移提交**

消费者拉取消息后进行业务逻辑处理，当处理完成后会进行位移提交，这里需要确定两点：

1.  消费者订阅状态。
2.  消费者获取以及更新 Offset。

详情请点击：[【消费者源码分析系列第十篇】图解 Kafka 源码之订阅状态、offset 操作](https://articles.zsxq.com/id_zt5mepqrh1i1.html)

提交位移之后会存储在 Kafka 内部的 Topic 中 「**\_\_consumer\_offsets**」 。

详情请点击： [【消费者源码分析系列第十一篇】图解 Kafka 源码之 \_\_consumer\_offsets 探秘](https://articles.zsxq.com/id_465hdq75igkd.html)