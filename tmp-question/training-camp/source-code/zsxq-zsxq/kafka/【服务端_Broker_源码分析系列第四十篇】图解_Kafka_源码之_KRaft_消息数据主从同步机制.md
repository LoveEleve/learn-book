大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端** **元数据主从同步机制**」，以「**创建 Topic 场景**」为例来深度剖析 Kafka Kraft 模式下 「**主从之间是如何同步元数据的**」的三个维度，让你更好的理解其处理全流程，今天我们接着来深度聊聊「**Kafka 服务端 KRaft 消息数据主从同步机制**」，看看 Kafka Kraft 模式下「**主从之间是如何同步消息数据的**」。

![](https://article-images.zsxq.com/Foeju74rWDYgFwL5mXUCfYm5J-ZS)

## **01 总体概述**

在 [【服务端 Broker 源码分析系列第三十九篇】图解 Kafka 源码之 KRaft 元数据主从同步机制](https://articles.zsxq.com/id_lgowqtnbcrbw.html) 上一篇第二部分的结尾，我提到了元数据主从同步完成后，元数据的变动会被「**Broker 模块监听处理后**」才能对集群产生影响，今天我们还是以「**创建 Topic 场景**」为例来深度剖析主从之间是如何同步消息数据的。

Kafka 通过「**多副本机制**」来实现故障 Failover，其每个分区都由若干副本组成，包括「**Leader 副本节点**」和 「**Follower 副本节点**」。主从数据同步并不是「**Follower 副本节点**」 把数据复制过来保存就结束了，还需要借助额外的 HW 标识来表明数据的提交状态（Committed/Uncommitted），最后决定消息是否对外可见。

所有副本底层的日志文件结构都是一样的，也就是说每个副本都有自己的 HW，但只有 「**Leader 副本节点**」的 HW 是用来表示整个分区的消息可见性。

主从副本的数据同步就是围绕 HW 展开，并通过 Fetch 请求进行。之前我们剖析过 Zookeeper 模式下副本数据同步，如果忘记了可以点击下面连接进行学习：

[【服务端 Broker 源码分析系列第二十六篇】图解 Kafka 源码之副本同步实现原理（上）](https://articles.zsxq.com/id_j8h0gmrk5i6d.html)

[【服务端 Broker 源码分析系列第二十七篇】图解 Kafka 源码之副本同步实现原理（下）](https://articles.zsxq.com/id_64qw2onjkgpx.html)

本文涉及的源码：

「**KafkaRaftClient**」类源码在 Kafka 源码包的 raft 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/raft/src/main/java/org/apache/kafka/raft/KafkaRaftClient.java](https://github.com/apache/kafka/blob/3.0.0/raft/src/main/java/org/apache/kafka/raft/KafkaRaftClient.java)

「**BrokerMetadataListener**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/metadata/BrokerMetadataListener.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/metadata/BrokerMetadataListener.scala)

「**BrokerMetadataPublisher**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/metadata/BrokerMetadataPublisher.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/metadata/BrokerMetadataPublisher.scala)

「**ReplicaManager**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/ReplicaManager.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/metadata/BrokerMetadataPublisher.scala)

「**ReplicaFetchManager**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/ReplicaFetcherManager.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/AbstractFetcherManager.scala)

「**AbstractFetchManager**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/AbstractFetcherManager.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/AbstractFetcherManager.scala)

「**AbstractFetchThread**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/AbstractFetcherThread.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/AbstractFetcherManager.scala)

「**MetadataDelta**」类源码在 Kafka 源码包的 metadata 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/image/MetadataDelta.java](https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/image/MetadataDelta.java)

「**TopicsDelta**」类源码在 Kafka 源码包的 metadata 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/image/TopicsDelta.java](https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/image/MetadataDelta.java)

## **02 监听器机制**

在 KRaft 模式下，是通过「**监听器机制**」来提交元数据 Record 数据的。

在 KafkaRaftClient 类中有一个监听器实例，如下：

private final Map<Listener<T>, ListenerContext> listenerContexts = new IdentityHashMap<>();

可以看出，它是一个 listenerContexts 实例，内部有两个关键属性：

1.  **Listener**：监听器，它是 RaftClient.Listener 类型，内部的 handleCommit 方法专门用来执行提交 Record 数据。
2.  **nextOffset**：下一个待提交的偏移量，表示它之前的 Record 都已经提交了。

![](https://article-images.zsxq.com/FkVR9VHo6FkTG4ih0slpDDMEH3Mn)

举例说明下：

假如 [ListenerContext#nextOffset](http://listenercontext/#nextOffset) 为 4， 当 [LeaderState#highWatermark](http://leaderstate/#highWatermark) 变更为 7 时，此时会触发 [ListenerContext](http://listenercontext%20/) 监听器提交偏移量为 4，5，6 三条 Record 记录，并将 [ListenerContext#nextOffset](http://listenercontext/#nextOffset%20) 更新为 7。

在上篇中触发监听器的有两个场景：

1.  当**一半以上的有投票权的节点保存后，**Leader 节点变更 LeaderState#highWatermark 后。
2.  Follower 节点或者 Broker 节点收到 Leader 节点返回的新的 HW 位置后。

## **03 元数据变动发布源码流程**

当 Broker 节点启动时， [BrokerServer#startup()](http://brokerserver/#startup\(\)) 方法会向 [KafkaRaftClient#listenerContexts](http://kafkaraftclient/#listenerContexts) 注册 [BrokerMetadataListener](http://brokermetadatalistener%20/) 监听器，它是通过

[KafkaRaftManager.scala#register](http://kafkaraftmanager.scala/#register)() 方法将 [BrokerMetadataListener](http://brokermetadatalistener/) 注册到 [KafkaRaftClient](http://kafkaraftclient%20/) [](http://kafkaraftclient%20/)中，源码如下：

// BrokerServer.scala

def startup(): Unit = {

if (!maybeChangeStatus(SHUTDOWN, STARTING)) return

try {

info("Starting broker")

....

metadataListener = new BrokerMetadataListener(config.nodeId,

time,

threadNamePrefix,

config.metadataSnapshotMaxNewRecordBytes,

metadataSnapshotter)

// Register a listener with the Raft layer to receive metadata event notifications

raftManager.register(metadataListener)

....

}

[BrokerMetadataListener](http://brokermetadatalistener/) 负责执行 Broker 节点提交 Record 数据，比如：「**修改 Broker 元数据实例**」、「**创建分区对应实例**」、「**成为分区 Leader 副本节点**」、「**成为分区 Follower 副本节点**」。

##   
**3.1 BrokerMetadataListener#handleCommit()**

当元数据分区 HW 更新同步后将回调 [BrokerMetadataListener.scala#handleCommit](http://brokermetadatalistener.scala/#handleCommit)() 方法通知监听器，源码如下：

override def handleCommit(reader: BatchReader\[ApiMessageAndVersion\]): Unit =

eventQueue.append(new HandleCommitsEvent(reader))

该方法比较简单:

1.  首先构建封装元数据消息的异步事件 [HandleCommitsEvent](http://handlecommitsevent%20/) 对象，事件被处理时该对象 [HandleCommitsEvent#run()](http://handlecommitsevent/#run\(\)) 方法将被执行。
2.  调用 [KafkaEventQueue.java#append()](http://kafkaeventqueue.java/#append\(\)) 方法将事件写入到异步队列。

简单来说，就是会在 [BrokerMetadataListener.scala#eventQueue](http://brokermetadatalistener.scala/#eventQueue) 中添加一个 [HandleCommitsEvent](http://handlecommitsevent%20/) 任务，而 [KafkaEventQueue#eventHandlerThread](http://kafkaeventqueue/#eventHandlerThread) 线程会负责执行这些 [HandleCommitsEvent](http://handlecommitsevent%20/) 任务。

## **3.2 HandleCommitsEvent#run()**

// BrokerMetadataListener 类的子类

class HandleCommitsEvent(reader: BatchReader\[ApiMessageAndVersion\])

extends EventQueue.FailureLoggingEvent(log) {

override def run(): Unit = {

val results \= try {

// 1、调用 BrokerMetadataListener.scala#loadBatches() 从 Record 中加载元数据变更的内容，，将其重放出来载入到数据结构 MetadataDelta。

val loadResults \= loadBatches(\_delta, reader)

if (isDebugEnabled) {

debug(s"Loaded new commits: ${loadResults}")

}

loadResults

} finally {

reader.close()

}

// 2、调用 BrokerMetadataListener.scala#publish() 方法使用元数据发布器发布元数据变更内容

\_publisher.foreach(publish(\_, results.highestMetadataOffset))

// 3、根据需要生成一个新的快照

snapshotter.foreach { snapshotter =>

\_bytesSinceLastSnapshot = \_bytesSinceLastSnapshot + results.numBytes

if (shouldSnapshot()) {

if (snapshotter.maybeStartSnapshot(results.highestMetadataOffset,

\_highestEpoch,

\_highestTimestamp,

\_delta.apply())) {

\_bytesSinceLastSnapshot = 0L

}

}

}

}

}

该类就一个 run() 方法，相对比较简单，核心步骤如下：

1.  调用 [BrokerMetadataListener.scala#loadBatches](http://brokermetadatalistener.scala/#loadBatches)() 从 Record 中加载元数据变更的内容，将其重放出来载入到数据结构 MetadataDelta。
2.  调用 [BrokerMetadataListener.scala#publish](http://brokermetadatalistener.scala/#publish)() 方法使用元数据发布器发布元数据变更内容。
3.  根据需要生成一个新的快照。

接下来我们分别来剖析下 「**第一步**」、「**第二步**」。

##   
**3.2 BrokerMetadataListener#loadBatches()**

private def loadBatches(delta: MetadataDelta,

iterator: util.Iterator\[Batch\[ApiMessageAndVersion\]\]): BatchLoadResults = {

// 记录开始时间

val startTimeNs \= time.nanoseconds()

var numBatches \= 0 // 批次计数器

var numRecords \= 0 // 记录计数器

var batch: Batch\[ApiMessageAndVersion\] = null

var numBytes \= 0L // 字节数计数器

// 迭代消息批次

while (iterator.hasNext()) {

batch = iterator.next()

var index \= 0

batch.records().forEach { messageAndVersion => // 处理每条消息

if (isTraceEnabled) {

trace("Metadata batch %d: processing \[%d/%d\]: %s.".format(batch.lastOffset, index + 1,

batch.records().size(), messageAndVersion.message().toString()))

}

// 重放消息

delta.replay(messageAndVersion.message())

numRecords += 1 // 增加记录计数

index += 1

}

numBytes = numBytes + batch.sizeInBytes() // 增加字节数

metadataBatchSizeHist.update(batch.records().size()) // 更新元数据批次大小统计

numBatches = numBatches + 1 // 增加批次计数

}

val newHighestMetadataOffset \= if (batch == null) {

\_highestMetadataOffset

} else {

// 更新最高元数据偏移量、最高纪元和最高时间戳

\_highestMetadataOffset = batch.lastOffset()

\_highestEpoch = batch.epoch()

\_highestTimestamp = batch.appendTimestamp()

batch.lastOffset()

}

// 记录结束时间

val endTimeNs \= time.nanoseconds()

// 计算批次处理时间

val elapsedUs \= TimeUnit.MICROSECONDS.convert(endTimeNs - startTimeNs, TimeUnit.NANOSECONDS)

// 更新批次处理时间统计

batchProcessingTimeHist.update(elapsedUs)

BatchLoadResults(numBatches, numRecords, elapsedUs, numBytes, newHighestMetadataOffset)

}

该方法比较简单，可以看到就是遍历元数据消息列表，循环调用 [MetadataDelta.java#replay](http://metadatadelta.java/#replay)() 方法其重放出来载入到数据结构 [MetadataDelta](http://metadatadelta/)。

##   
**3.3 MetadataDelta#replay()**

该方法将根据元数据记录的类型进行处理分发，新增 Topic 生成的元数据记录类型为 [TOPIC\_RECORD](http://topic_record/)，则将触发[MetadataDelta.java#replay](http://metadatadelta.java/#replay)() 重载方法执行。

public void replay(ApiMessage record) {

MetadataRecordType type \= MetadataRecordType.fromId(record.apiKey());

switch (type) {

case REGISTER\_BROKER\_RECORD:

replay((RegisterBrokerRecord) record);

break;

case UNREGISTER\_BROKER\_RECORD:

replay((UnregisterBrokerRecord) record);

break;

case TOPIC\_RECORD:

replay((TopicRecord) record);

break;

case PARTITION\_RECORD:

replay((PartitionRecord) record);

break;

case CONFIG\_RECORD:

replay((ConfigRecord) record);

break;

case PARTITION\_CHANGE\_RECORD:

replay((PartitionChangeRecord) record);

break;

case FENCE\_BROKER\_RECORD:

replay((FenceBrokerRecord) record);

break;

case UNFENCE\_BROKER\_RECORD:

replay((UnfenceBrokerRecord) record);

break;

case REMOVE\_TOPIC\_RECORD:

replay((RemoveTopicRecord) record);

break;

case FEATURE\_LEVEL\_RECORD:

replay((FeatureLevelRecord) record);

break;

case CLIENT\_QUOTA\_RECORD:

replay((ClientQuotaRecord) record);

break;

case PRODUCER\_IDS\_RECORD:

// Nothing to do.

break;

case REMOVE\_FEATURE\_LEVEL\_RECORD:

replay((RemoveFeatureLevelRecord) record);

break;

case BROKER\_REGISTRATION\_CHANGE\_RECORD:

replay((BrokerRegistrationChangeRecord) record);

break;

default:

throw new RuntimeException("Unknown metadata record type " + type);

}

}

// 对于创建 Topic 来说会执行该方法

public void replay(TopicRecord record) {

if (topicsDelta == null) topicsDelta = new TopicsDelta(image.topics());

// 处理 TOPIC\_RECORD 消息类型的重载方法

    topicsDelta.replay(record);

}

## **3.4 TopicsDelta#replay()**

// 这里存放的是变更的 topic，如果创建操作就是新增的 topic

private final Map<Uuid, TopicDelta> changedTopics = new HashMap<>();

public void replay(TopicRecord record) {

TopicDelta delta \= new TopicDelta(

new TopicImage(record.name(), record.topicId(), Collections.emptyMap()));

// 可以看出这里只是暂存消息中的 Topic 元数据

changedTopics.put(record.topicId(), delta);

}

可以看出这里只是暂存消息中的 Topic 元数据，等待后续处理。

##   
**3.5 BrokerMetadataListener#publish()**

接着我们把视角切回到 **3.2 步骤的第二步**，[BrokerMetadataListener.scala#publish](http://brokermetadatalistener.scala/#publish)() 方法，源码如下：

private def publish(publisher: MetadataPublisher,

newHighestMetadataOffset: Long): Unit = {

val delta \= \_delta

\_image \= \_delta.apply()

\_delta = new MetadataDelta(\_image)

// 通过元数据发布器将元数据发布出来，触发 BrokerMetadataPublisher.scala#publish() 方法执行

publisher.publish(newHighestMetadataOffset, delta, \_image)

}

可以看到该方法会通过元数据发布器将元数据发布出来，触发 [BrokerMetadataPublisher.scala#publish](http://brokermetadatapublisher.scala/#publish)() 方法执行。

至此，「**元数据变动发布源码**」流程基本剖析完了。

## **04 元数据变动消费源码流程**

接着来继续剖析。

## **4.1 BrokerMetadataPublisher#publish()**

/\*\*

\* 元数据变动发布

\* delta：元数据变更内容

\* newImage：新的元数据镜像

\*/

override def publish(newHighestMetadataOffset: Long,

delta: MetadataDelta,

newImage: MetadataImage): Unit = {

try {

// 1、更新 Broker 节点的袁术缓存 BrokerServer#metadataCache，由于 Broker 节点运行时需要实时使用 Kafka 元数据（比如获取 Topic、Broker 相关），所以 Broker 节点会将 Kafka 元数据缓存在内存中，BrokerServer#metadataCache 是一个 KRaftMetadataCache，它是用来缓存 Kafka 元数据的

metadataCache.setImage(newImage)

// 2、如果是第一次发布元数据变更，需要调用 BrokerMetadataPublisher.scala#initializeManagers() 方法进行初始化操作。这一步大多是定时任务的启动，包括日志文件相关的定期刷盘、异常恢复检测，副本管理相关的 ISR 列表过期收缩，以及消费者组协调器删除过期消费者组信息等。

if (\_firstPublish) {

info(s"Publishing initial metadata at offset ${newHighestMetadataOffset}.")

// If this is the first metadata update we are applying, initialize the managers

// first (but after setting up the metadata cache).

initializeManagers()

} else if (isDebugEnabled) {

debug(s"Publishing metadata at offset ${newHighestMetadataOffset}.")

}

....

// Apply topic deltas.

// 3、开始计算元数据的变动，进行相应处理，这里以 Topic 变动触发 ReplicaManager.scala#applyDelta() 方法执行为例，根据主题、分区变更内容执行相应的处理逻辑。

Option(delta.topicsDelta()).foreach { topicsDelta =>

// Notify the replica manager about changes to topics.

replicaManager.applyDelta(newImage, topicsDelta)

....

}

....

if (\_firstPublish) {

finishInitializingReplicaManager(newImage)

}

} catch {

case t: Throwable => error(s"Error publishing broker metadata at ${newHighestMetadataOffset}", t)

throw t

} finally {

\_firstPublish = false

}

}

// 定时任务的启动，包括日志文件相关的定期刷盘、异常恢复检测，副本管理相关的 ISR 列表过期收缩，以及消费者组协调器删除过期消费者组信息

private def initializeManagers(): Unit = {

// Start log manager, which will perform (potentially lengthy)

// recovery-from-unclean-shutdown if required.

logManager.startup(metadataCache.getAllTopics())

// Start the replica manager.

replicaManager.startup()

// Start the group coordinator.

groupCoordinator.startup(() => metadataCache.numPartitions(

Topic.GROUP\_METADATA\_TOPIC\_NAME).getOrElse(conf.offsetsTopicPartitions))

// Start the transaction coordinator.

txnCoordinator.startup(() => metadataCache.numPartitions(

Topic.TRANSACTION\_STATE\_TOPIC\_NAME).getOrElse(conf.transactionTopicPartitions))

}

核心步骤如下：

1.  调用 [metadataCache.setImage()](http://metadatacache.setimage\(\)/) 更新 Broker 节点的袁术缓存 [BrokerServer#metadataCache](http://brokerserver/#metadataCache)，由于 Broker 节点运行时需要实时使用 Kafka 元数据（比如获取 Topic、Broker 相关），所以 Broker 节点会将 Kafka 元数据缓存在内存中，[BrokerServer#metadataCache](http://brokerserver/#metadataCache) 是一个 [KRaftMetadataCache](http://kraftmetadatacache/)，它是用来缓存 Kafka 元数据的。
2.  如果是第一次发布元数据变更，需要调用 [BrokerMetadataPublisher.scala#initializeManagers](http://brokermetadatapublisher.scala/#initializeManagers)() 方法进行初始化操作。这一步大多是定时任务的启动，包括日志文件相关的定期刷盘、异常恢复检测，副本管理相关的 ISR 列表过期收缩，以及消费者组协调器删除过期消费者组信息等。
3.  开始计算元数据的变动，进行相应处理，这里以 Topic 变动触发 [ReplicaManager.scala#applyDelta](http://replicamanager.scala/#applyDelta)() 方法执行为例，根据主题、分区变更内容执行相应的处理逻辑。

## **4.2 ReplicaManager#applyDelta()**

def applyDelta(newImage: MetadataImage, delta: TopicsDelta): Unit = {

// 1、首先调用 TopicsDelta.java#localChanges() 方法计算元数据中的 topic 变动点

val localChanges \= delta.localChanges(config.nodeId)

replicaStateChangeLock.synchronized {

// Handle deleted partitions. We need to do this first because we might subsequently

// create new partitions with the same names as the ones we are deleting here.

if (!localChanges.deletes.isEmpty) {

val deletes \= localChanges.deletes.asScala.map(tp => (tp, true)).toMap

stateChangeLogger.info(s"Deleting ${deletes.size} partition(s).")

stopPartitions(deletes).foreach { case (topicPartition, e) =>

if (e.isInstanceOf\[KafkaStorageException\]) {

stateChangeLogger.error(s"Unable to delete replica ${topicPartition} because " +

"the local replica for the partition is in an offline log directory")

} else {

stateChangeLogger.error(s"Unable to delete replica ${topicPartition} because " +

s"we got an unexpected ${e.getClass.getName} exception: ${e.getMessage}")

}

}

}

// Handle partitions which we are now the leader or follower for.

// 计算出 topic 的变动点后，如果当前节点被分配了充当某些分区的 Leader 副本，那么调用 ReplicaManager.scala#applyLocalLeadersDelta() 方法进行相应处理；如果当前节点还被分配负责某些分区的 Follower 副本，则调用 ReplicaManager.scala#applyLocalFollowersDelta() 进行处理

if (!localChanges.leaders.isEmpty || !localChanges.followers.isEmpty) {

val lazyOffsetCheckpoints \= new LazyOffsetCheckpoints(this.highWatermarkCheckpoints)

val changedPartitions \= new mutable.HashSet\[Partition\]

if (!localChanges.leaders.isEmpty) {

applyLocalLeadersDelta(changedPartitions, delta, lazyOffsetCheckpoints, localChanges.leaders.asScala)

}

if (!localChanges.followers.isEmpty) {

applyLocalFollowersDelta(changedPartitions, newImage, delta, lazyOffsetCheckpoints, localChanges.followers.asScala)

}

maybeAddLogDirFetchers(changedPartitions, lazyOffsetCheckpoints,

name => Option(newImage.topics().getTopic(name)).map(\_.id()))

def markPartitionOfflineIfNeeded(tp: TopicPartition): Unit = {

/\*

\* If there is offline log directory, a Partition object may have been created by getOrCreatePartition()

\* before getOrCreateReplica() failed to create local replica due to KafkaStorageException.

\* In this case ReplicaManager.allPartitions will map this topic-partition to an empty Partition object.

\* we need to map this topic-partition to OfflinePartition instead.

\*/

if (localLog(tp).isEmpty)

markPartitionOffline(tp)

}

localChanges.leaders.keySet.forEach(markPartitionOfflineIfNeeded)

localChanges.followers.keySet.forEach(markPartitionOfflineIfNeeded)

replicaFetcherManager.shutdownIdleFetcherThreads()

replicaAlterLogDirsManager.shutdownIdleFetcherThreads()

}

}

}

该方法主要**根据主题、分区变更内容执行相应的处理逻辑**，核心步骤如下：

1.  首先调用 [TopicsDelta.java#localChanges](http://topicsdelta.java/#localChanges)() 方法计算元数据中的 topic 变动点。
2.  计算出 topic 的变动点后，如果当前节点被分配了充当某些分区的 Leader 副本，那么调用 [ReplicaManager.scala#applyLocalLeadersDelta](http://replicamanager.scala/#applyLocalLeadersDelta)() 方法进行相应处理；如果当前节点还被分配负责某些分区的 Follower 副本，则调用 [ReplicaManager.scala#applyLocalFollowersDelta](http://replicamanager.scala/#applyLocalFollowersDelta)() 进行处理。

## **4.3 TopicsDelta#localChange()**

/\*\*

\* Find the topic partitions that have change based on the replica given.

\* The changes identified are:

\* 1. topic partitions for which the broker is not a replica anymore

\* 2. topic partitions for which the broker is now the leader

\* 3. topic partitions for which the broker is now a follower

\* @param brokerId the broker id

\* @return the list of topic partitions which the broker should remove, become leader or become follower.

\*/

public LocalReplicaChanges localChanges(int brokerId) {

Set<TopicPartition> deletes = new HashSet<>();

Map<TopicPartition, LocalReplicaChanges.PartitionInfo> leaders = new HashMap<>();

Map<TopicPartition, LocalReplicaChanges.PartitionInfo> followers = new HashMap<>();

for (TopicDelta delta : changedTopics.values()) {

LocalReplicaChanges changes \= delta.localChanges(brokerId);

// 计算当前节点需要删除的本地副本

deletes.addAll(changes.deletes());

// 计算当前节点新增的需要维护的 Leader 副本。

leaders.putAll(changes.leaders());

// 计算当前节点新增的需要维护的 Follower 副本。

followers.putAll(changes.followers());

}

// 将所有已删除的主题分区添加到本地已删除的分区集中

deletedTopicIds().forEach(topicId -> {

TopicImage topicImage \= image().getTopic(topicId);

topicImage.partitions().forEach((partitionId, prevPartition) -> {

if (Replicas.contains(prevPartition.replicas, brokerId)) {

deletes.add(new TopicPartition(topicImage.name(), partitionId));

}

});

});

return new LocalReplicaChanges(deletes, leaders, followers);

}

根据方法注释可以看到主要计算以下 3 种需要在本节点的所有 topic 变动，单个 topic 的变动计算逻辑由 [TopicDelta.java#localChanges](http://topicdelta.java/#localChanges)() 实现：

1.  当前节点需要删除的本地副本。
2.  当前节点新增的需要维护的 Leader 副本。
3.  当前节点新增的需要维护的 Follower 副本。

## **4.4 ReplicaManager#applyLocalLeadersDelta()**

当元数据变动计算完毕，将视角切回到 **4.2 节**第 2 步，如果当前节点有新增的需要维护的 Leader 副本，则 [ReplicaManager.scala#applyLocalLeadersDelta](http://replicamanager.scala/#applyLocalLeadersDelta)() 方法将被触发执行。

private def applyLocalLeadersDelta(

changedPartitions: mutable.Set\[Partition\], // 存储已更新的分区列表

delta: TopicsDelta, // Topic 变更内容

offsetCheckpoints: OffsetCheckpoints, // 偏移量检查点

newLocalLeaders: mutable.Map\[TopicPartition, LocalReplicaChanges.PartitionInfo\]

): Unit = {

stateChangeLogger.info(s"Transitioning ${newLocalLeaders.size} partition(s) to " +

"local leaders.")

replicaFetcherManager.removeFetcherForPartitions(newLocalLeaders.keySet)

// 1、遍历新的 Leader 列表，调用 ReplicaManager.scala#getOrCreatePartition() 方法为其创建本地分区 Partition 对象

newLocalLeaders.forKeyValue { case (tp, info) =>

getOrCreatePartition(tp, delta, info.topicId).foreach { case (partition, isNew) =>

try {

val state \= info.partition.toLeaderAndIsrPartitionState(tp, isNew)

// 2、如果当前节点被分配为新分区的 Leader 副本，则调用 Partition.scala#makeLeader() 将新建的 Partition 对象对应的当前节点设置为分区副本 Leader

if (!partition.makeLeader(state, offsetCheckpoints, Some(info.topicId))) {

stateChangeLogger.info("Skipped the become-leader state change for " +

s"${tp} with topic id ${info.topicId} because this partition is " +

"already a local leader.")

}

changedPartitions.add(partition)

} catch {

case e: KafkaStorageException =>

stateChangeLogger.info(s"Skipped the become-leader state change for ${tp} " +

s"with topic id ${info.topicId} due to disk error ${e}")

val dirOpt \= getLogDir(tp)

error(s"Error while making broker the leader for partition ${tp} in dir " +

s"${dirOpt}", e)

}

}

}

}

核心步骤如下：

1.  遍历新的 Leader 列表，调用 [ReplicaManager.scala#getOrCreatePartition](http://replicamanager.scala/#getOrCreatePartition)() 方法为其创建本地分区 Partition 对象。
2.  如果当前节点被分配为新分区的 Leader 副本，则调用 [Partition.scala#makeLeader](http://partition.scala/#makeLeader)() 将新建的 Partition 对象对应的当前节点设置为分区副本 Leader。

关于 [Partition.scala#makeLeader](http://partition.scala/#makeLeader)() 内部源码实现，请点击 [【服务端 Broker 源码分析系列第二十六篇】图解 Kafka 源码之副本同步实现原理（上）](https://articles.zsxq.com/id_j8h0gmrk5i6d.html) 这篇进行学习。

## **4.5 ReplicaManager#applyLocalFollowersDelta()**

当元数据变动计算完毕，将视角切回到 **4.2 节**第 2 步，如果当前节点有新增的需要维护的 Follower 副本，则

[ReplicaManager.scala#applyLocalFollowersDelta](http://replicamanager.scala/#applyLocalFollowersDelta)() 方法将被触发执行。

private def applyLocalFollowersDelta(

changedPartitions: mutable.Set\[Partition\], // 存储已更新的分区列表

newImage: MetadataImage, // Broker元数据Image

delta: TopicsDelta, // Topic 变更内容

offsetCheckpoints: OffsetCheckpoints, // 偏移量检查点

newLocalFollowers: mutable.Map\[TopicPartition, LocalReplicaChanges.PartitionInfo\]

): Unit = {

stateChangeLogger.info(s"Transitioning ${newLocalFollowers.size} partition(s) to " +

"local followers.")

// 是否正在关闭 Broker

val shuttingDown \= isShuttingDown.get()

// 存储待处理的分区和分区对象

val partitionsToMakeFollower \= new mutable.HashMap\[TopicPartition, Partition\]

// 存储待处理新的 Follower 列表

val newFollowerTopicSet \= new mutable.HashSet\[String\]

// 1、遍历新的 Follower 列表，调用 ReplicaManager.scala#getOrCreatePartition() 方法为其创建本地分区 Partition 对象

newLocalFollowers.forKeyValue { case (tp, info) =>

getOrCreatePartition(tp, delta, info.topicId).foreach { case (partition, isNew) =>

try {

// 将待处理的分区信息存储在 newLocalFollowers 中，从而获得对应的 PartitionInfo。

newFollowerTopicSet.add(tp.topic)

if (shuttingDown) {

// 如果正在关闭 Broker，无法从 Leader 开始获取数据

stateChangeLogger.trace(s"Unable to start fetching ${tp} with topic " +

s"ID ${info.topicId} because the replica manager is shutting down.")

} else {

// 获取分区 Leader

val leader \= info.partition.leader

// 如果 Leader 不存在或者它不是当前 Broker 的一部分，则创建本地副本

if (newImage.cluster.broker(leader) == null) {

stateChangeLogger.trace(s"Unable to start fetching $tp with topic ID ${info.topicId} " + s"from leader $leader because it is not alive.")

// Create the local replica even if the leader is unavailable. This is required

// to ensure that we include the partition's high watermark in the checkpoint

// file (see KAFKA-1647).

partition.createLogIfNotExists(isNew, false, offsetCheckpoints, Some(info.topicId))

} else {

// 创建新的 LeaderAndIsrPartitionState

val state \= info.partition.toLeaderAndIsrPartitionState(tp, isNew)

// 2、调用 Partition.scala#makeFollower() 将新建的 Partition 对象设置为分区副本 Follower

if (partition.makeFollower(state, offsetCheckpoints, Some(info.topicId))) {

// 设置成功后将分区添加到待处理的分区集合中

partitionsToMakeFollower.put(tp, partition)

} else {

stateChangeLogger.info("Skipped the become-follower state change after marking its " + s"partition as follower for partition $tp with id ${info.topicId} and partition state $state.")

}

}

}

// 将已更新的分区添加到列表中

changedPartitions.add(partition)

} catch {

case e: Throwable => stateChangeLogger.error(s"Unable to start fetching ${tp} " +

s"with topic ID ${info.topicId} due to ${e.getClass.getSimpleName}", e)

replicaFetcherManager.addFailedPartition(tp)

}

}

}

// 3、停止与待处理的分区对应的 fetcher

replicaFetcherManager.removeFetcherForPartitions(partitionsToMakeFollower.keySet)

stateChangeLogger.info(s"Stopped fetchers as part of become-follower for ${partitionsToMakeFollower.size} partitions")

val listenerName \= config.interBrokerListenerName.value

// 存储待处理分区的 Leader、LeaderEpoch 和 FetchOffset

val partitionAndOffsets \= new mutable.HashMap\[TopicPartition, InitialFetchState\]

partitionsToMakeFollower.forKeyValue { (topicPartition, partition) =>

// 获取 Leader 所在的 Node 节点

val node \= partition.leaderReplicaIdOpt

.flatMap(leaderId => Option(newImage.cluster.broker(leaderId)))

.flatMap(\_.node(listenerName).asScala)

.getOrElse(Node.noNode)

// 获取分区的本地日志对象

val log \= partition.localLogOrException

partitionAndOffsets.put(topicPartition, InitialFetchState(

new BrokerEndPoint(node.id, node.host, node.port),

partition.getLeaderEpoch,

initialFetchOffset(log) // 获取初始的Fetch Offset

))

}

// 4、调用 ReplicaFetcherManager.scala#addFetcherForPartitions() 方法为分区 Follower 副本设置 Fetcher 线程，该线程用于从分区 Leader 副本处同步消息数据

replicaFetcherManager.addFetcherForPartitions(partitionAndOffsets)

stateChangeLogger.info(s"Started fetchers as part of become-follower for ${partitionsToMakeFollower.size} partitions")

// 5、处理所有延迟的 fetch 和 produce 请求

partitionsToMakeFollower.keySet.foreach(completeDelayedFetchOrProduceRequests)

// 更新Metric相关信息

updateLeaderAndFollowerMetrics(newFollowerTopicSet)

}

核心步骤如下：

1.  遍历新的 Follower 列表，调用 [ReplicaManager.scala#getOrCreatePartition](http://replicamanager.scala/#getOrCreatePartition)() 方法为其创建本地分区 Partition 对象。
2.  如果当前节点被分配为新分区的 Follower 副本，则调用 [Partition.scala#makeFollower](http://partition.scala/#makeFollower)() 将新建的 Partition 对象设置为分区副本 Follower。
3.  停止与待处理的分区对应的 fetcher 。
4.  调用 [ReplicaFetcherManager.scala#addFetcherForPartitions](http://replicafetchermanager.scala/#addFetcherForPartitions)() 方法为分区 Follower 副本设置 Fetcher 线程，该线程用于从分区 Leader 副本处同步消息数据。
5.  处理所有延迟的 fetch 和 produce 请求。

关于 [Partition.scala#makeFollower()](http://partition.scala/#makeFollower\(\)) 内部源码实现，请点击 [【服务端 Broker 源码分析系列第二十六篇】图解 Kafka 源码之副本同步实现原理（上）](https://articles.zsxq.com/id_j8h0gmrk5i6d.html) 这篇进行学习。

## **4.6 ReplicaFetcherManager#addFetcherForPartitions()**

val replicaFetcherManager \= createReplicaFetcherManager(metrics, time, threadNamePrefix, quotaManagers.follower)

abstract class AbstractFetcherManager\[T <: AbstractFetcherThread\](val name: String, clientId: String, numFetchers: Int)

extends Logging with KafkaMetricsGroup {

def addFetcherForPartitions(partitionAndOffsets: Map\[TopicPartition, InitialFetchState\]): Unit = {

lock synchronized { // 加锁，保证线程安全

// 将partitionAndOffsets按照fetcher进行分组

val partitionsPerFetcher \= partitionAndOffsets.groupBy {

case (topicPartition, brokerAndInitialFetchOffset) =>

BrokerAndFetcherId(brokerAndInitialFetchOffset.leader, getFetcherId(topicPartition))

}

def addAndStartFetcherThread(brokerAndFetcherId: BrokerAndFetcherId,

brokerIdAndFetcherId: BrokerIdAndFetcherId): T = {

// 创建并启动fetcher线程

val fetcherThread \= createFetcherThread(brokerAndFetcherId.fetcherId, brokerAndFetcherId.broker)

fetcherThreadMap.put(brokerIdAndFetcherId, fetcherThread)

fetcherThread.start()

fetcherThread

}

for ((brokerAndFetcherId, initialFetchOffsets) <- partitionsPerFetcher) {

val brokerIdAndFetcherId \= BrokerIdAndFetcherId(brokerAndFetcherId.broker.id, brokerAndFetcherId.fetcherId)

val fetcherThread \= fetcherThreadMap.get(brokerIdAndFetcherId) match {

// 如果fetcher线程已存在且源broker与当前broker一致，则复用该线程

case Some(currentFetcherThread) if currentFetcherThread.sourceBroker == brokerAndFetcherId.broker =>

currentFetcherThread

case Some(f) =>

// 如果fetcher线程已存在但源broker与当前broker不一致，则关闭该线程，并重新创建并启动一个fetcher线程

f.shutdown()

addAndStartFetcherThread(brokerAndFetcherId, brokerIdAndFetcherId)

case None \=\>

// 如果fetcher线程不存在，则创建并启动一个fetcher线程

addAndStartFetcherThread(brokerAndFetcherId, brokerIdAndFetcherId)

}

// 将分区加入fetcher线程的处理列表中

addPartitionsToFetcherThread(fetcherThread, initialFetchOffsets)

}

}

}

....

}

可以看到 [ReplicaFetcherManager.scala#addFetcherForPartitions()](http://replicafetchermanager.scala/#addFetcherForPartitions\(\)) 实际由其父类 [AbstractFetcherManager.scala#addFetcherForPartitions()](http://abstractfetchermanager.scala/#addFetcherForPartitions\(\)) 来实现，

可以看到这里关键处理是调用内部 [addAndStartFetcherThread()](http://addandstartfetcherthread\(\)/) 方法执行子类 [ReplicaFetcherManager.scala#createFetcherThread()](http://replicafetchermanager.scala/#createFetcherThread\(\)) 方法创建 Fetcher 线程，并将其启动。

## **4.7 ReplicaFetcherManager#createFetcherThread()**

override def createFetcherThread(fetcherId: Int, sourceBroker: BrokerEndPoint): ReplicaFetcherThread = {

val prefix \= threadNamePrefix.map(tp => s"$tp:").getOrElse("")

val threadName \= s"${prefix}ReplicaFetcherThread-$fetcherId-${sourceBroker.id}"

// 将创建 ReplicaFetcherThread 对象作为 Fetcher 线程实例

new ReplicaFetcherThread(threadName, fetcherId, sourceBroker, brokerConfig, failedPartitions, replicaManager,

metrics, time, quotaManager)

}

该方法很简单，就是将创建 ReplicaFetcherThread 对象作为 Fetcher 线程实例。

至此，「**元数据变动消费源码**」流程基本剖析完了。

## **05 主从副本消息数据同步源码流程**

在上一节中，ReplicaFetcherThread  线程对象被创建后会立即启动，触发 [ReplicaFetcherThread.scala#run()](http://replicafetcherthread.scala/#run\(\)) 方法执行。

![](https://article-images.zsxq.com/FmaQL1ezis2xF11fvTFKS1oXtGZa)

![](https://article-images.zsxq.com/Fpzl3H_eOQnvT8E5OSRH9SwOJLyu)

实际最终是由其父类 [ShutdownableThread.scala#run()](http://shutdownablethread.scala/#run\(\)) 方法实现，可以看到核心逻辑就是在 while 循环中不断执行子类实现的 [AbstractFetcherThread.scala#doWork()](http://abstractfetcherthread.scala/#doWork\(\)) 方法。

override def run(): Unit = {

isStarted = true

info("Starting")

try {

while (isRunning)

doWork()

} catch {

....

} finally {

shutdownComplete.countDown()

}

info("Stopped")

}

## **5.1 AbstractFetcherThread#doWork()**

override def doWork(): Unit = {

// 执行副本截断操作

maybeTruncate()

// 执行消息获取操作

maybeFetch()

}

该方法内部只有两个方法调用，其中 [AbstractFetcherThread.scala#maybeTruncate](http://abstractfetcherthread.scala/#maybeTruncate)()方法是在异常恢复时处理日志截断的，[AbstractFetcherThread.scala#maybeFetch](http://abstractfetcherthread.scala/#maybeFetch)() 方法完成 Fetch 请求同步消息数据的。

它是 [AbstractFetcherThread](http://abstractfetcherthread%20/) 类的核心方法，是线程运行的主要逻辑。[AbstractFetcherThread](http://abstractfetcherthread%20/) 线程只要一直处于运行状态，就是会不断地重复这两个操作。

看到这里你是否会有疑惑：**该线程为何要不断尝试的去做截断操作呢？**

其实主要原因就是：**分区的 Leader 副本可能会随时发生变化**。每当有新 Leader 副本被选举产生时，Follower 副本就必须主动去执行截断操作，将自己的本地日志截断成与 Leader 副本一模一样，另外 Leader 副本本身也需要执行截断操作，将 LEO 调整到分区高水位处。

先来看下截断操作。

## **5.2 AbstractFetcherThread#maybeTruncate()**

private def maybeTruncate(): Unit = {

// 1、将所有处于截断中状态的分区依据有无 Leader Epoch 值进行分组

val (partitionsWithEpochs, partitionsWithoutEpochs) = fetchTruncatingPartitions()

// 2、对于有 Leader Epoch 值的分区，将日志截断到 Leader Epoch 值对应的位移值处

if (partitionsWithEpochs.nonEmpty) {

truncateToEpochEndOffsets(partitionsWithEpochs)

}

// 3、对于没有 Leader Epoch 值的分区，将日志截断到高水位值处

if (partitionsWithoutEpochs.nonEmpty) {

truncateToHighWatermark(partitionsWithoutEpochs)

}

}

方法比较简单，步骤如下：

1.  首先是对分区状态进行分组。也就是说该方法操作的**只能是处于截断中状态的分区**。判断这些分区是否存在对应的 Leader Epoch 值，并按照有无 Epoch 值进行分组。
2.  对于 Leader Epoch 机制：**只它是用来替换 HW 值在日志截断中的作用**。典型的应用场景如下：
3.  当分区存在 Leader Epoch 值时，将副本的本地日志截断到 Leader Epoch 对应的最新位移值处，即方法truncateToEpochEndOffsets 将日志截断到 Leader Epoch 值对应的位移值处。
4.  当分区不存在对应的 Leader Epoch 值时，那么就会使用原来的高水位机制，即方法 truncateToHighWatermark 将日志调整到高水位值处。

接着分别来看下这 2 个方法。

// 根据指定 TopicPartition 的 PartitionData，获取每个分区的最新的 offset 信息，然后对所有分区的 Log 进行截断操作。

private def truncateToEpochEndOffsets(latestEpochsForPartitions: Map\[TopicPartition, EpochData\]): Unit = {

// 获取每个分区的最新 offset 信息

val endOffsets \= fetchEpochEndOffsets(latestEpochsForPartitions)

// 加锁，保证截断操作期间不会受到其他线程的干扰

inLock(partitionMapLock) {

//Check no leadership and no leader epoch changes happened whilst we were unlocked, fetching epochs

// 检查在获取 epoch 时，没有发生 Leader 更改

val epochEndOffsets \= endOffsets.filter { case (tp, \_) =>

val curPartitionState \= partitionStates.stateValue(tp)

val partitionEpochRequest \= latestEpochsForPartitions.getOrElse(tp, {

throw new IllegalStateException(

s"Leader replied with partition $tp not requested in OffsetsForLeaderEpoch request")

})

val leaderEpochInRequest \= partitionEpochRequest.currentLeaderEpoch

curPartitionState != null && leaderEpochInRequest == curPartitionState.currentLeaderEpoch

}

// 对指定 TopicPartition 进行截断操作

val ResultWithPartitions(fetchOffsets, partitionsWithError) = maybeTruncateToEpochEndOffsets(epochEndOffsets, latestEpochsForPartitions)

// 处理截断操作中出现的错误

handlePartitionsWithErrors(partitionsWithError, "truncateToEpochEndOffsets")

// 更新 fetchOffset 信息，并在可能的情况下标记截断操作已完成

updateFetchOffsetAndMaybeMarkTruncationComplete(fetchOffsets)

}

}

步骤如下：

1.  首先，获取每个分区的最新 offset 信息。
2.  加锁，保证截断操作期间不会受到其他线程的干扰。
3.  检查在获取 epoch 时，没有发生 Leader 更改。
4.  对指定 TopicPartition 进行截断操作。
5.  处理截断操作中出现的错误。
6.  更新 fetchOffset 信息，并在可能的情况下标记截断操作已完成。

来看下内部的 [maybeTruncateToEpochEndOffsets](http://maybetruncatetoepochendoffsets%20/) 截断方法，看看它做了什么：

// 根据获取的各分区的最新 leader epoch 的 offset 信息，对指定分区进行截断操作。

private def maybeTruncateToEpochEndOffsets(

fetchedEpochs: Map\[TopicPartition, EpochEndOffset\], // 获取的各分区的最新 leader epoch 的 offset 信息

latestEpochsForPartitions: Map\[TopicPartition, EpochData\]): // 指定分区的最新 leader epoch 信息

ResultWithPartitions\[Map\[TopicPartition, OffsetTruncationState\]\] = { // 包含截断操作结果的 ResultWithPartitions 对象

// 保存要截断的分区的 offset 状态信息

val fetchOffsets \= mutable.HashMap.empty\[TopicPartition, OffsetTruncationState\]

// 保存出现错误的分区

val partitionsWithError \= mutable.HashSet.empty\[TopicPartition\]

// 遍历每个分区的最新 epoch offset 信息

fetchedEpochs.forKeyValue { (tp, leaderEpochOffset) =>

// 根据错误码进行处理

Errors.forCode(leaderEpochOffset.errorCode) match {

case Errors.NONE =>

// 获取截断的 offset 状态信息并加入到 fetchOffsets 中

val offsetTruncationState \= getOffsetTruncationState(tp, leaderEpochOffset)

if (doTruncate(tp, offsetTruncationState))

fetchOffsets.put(tp, offsetTruncationState)

// 如果分区已被标记为 fenced，调用 onPartitionFenced 处理

case Errors.FENCED\_LEADER\_EPOCH =>

// 获取分区最新的 leader epoch 信息

val currentLeaderEpoch \= latestEpochsForPartitions.get(tp)

.map(epochEndOffset => Int.box(epochEndOffset.currentLeaderEpoch)).asJava

if (onPartitionFenced(tp, currentLeaderEpoch))

partitionsWithError += tp

// 如果出现其他错误，记录日志并加入到 partitionsWithError 中

case error \=\>

info(s"Retrying leaderEpoch request for partition $tp as the leader reported an error: $error")

partitionsWithError += tp

}

}

// 返回结果

ResultWithPartitions(fetchOffsets, partitionsWithError)

}

该方法主要是**根据获取的各分区的最新 Leader Epoch 的 Offset 信息，对指定分区进行截断操作**，其步骤如下：

1.  初始化保存要截断的分区的 offset 状态信息和保存出现错误的分区。
2.  遍历每个分区的最新 epoch offset 信息，根据错误码进行处理：
3.  获取截断的 offset 状态信息，然后调用 doTruncate 方法执行真正的日志截断操作，加入到 fetchOffsets 中。
4.  如果分区已被标记为 fenced，调用 onPartitionFenced 处理。在 Kafka 中，FENCED\_LEADER\_EPOCH 是在处理 epoch 初始化时使用的错误码。在 Kafka 的实现中，每个分区都有一个 leader epoch。当 broker 上的该分区的 leader 副本被重新选举时，leader epoch 会自增，以确保新的 leader 副本的 leader epoch 大于之前 leader 副本的 leader epoch，避免出现数据不一致的情况。如果 broker 上已经存在一个更高的 leader epoch，那么该分区的 leader 副本将无法被选举或成为 leader 副本。在这种情况下，分区上的所有请求，包括读请求和写请求都会返回 FENCED\_LEADER\_EPOCH 错误码。处理该错误码时，需要根据最新的 leader epoch 信息重新尝试请求，确保分区数据一致性。
5.  如果出现其他错误，记录日志并加入到 partitionsWithError 中。
6.  最后返回结果。

该方法会调用 [getOffsetTruncationState](http://getoffsettruncationstate%20/) 方法来计算截断位置，并调用 [doTruncate](http://dotruncate%20/) 方法来执行截断操作。

我们来看下 [getOffsetTruncationState](http://getoffsettruncationstate%20/) 方法是如何计算截断位置的。

private def getOffsetTruncationState(tp: TopicPartition,

leaderEpochOffset: EpochEndOffset): OffsetTruncationState = inLock(partitionMapLock) {

if (leaderEpochOffset.endOffset == UNDEFINED\_EPOCH\_OFFSET) {

....

OffsetTruncationState(partitionStates.stateValue(tp).fetchOffset, truncationCompleted = true)

} else if (leaderEpochOffset.leaderEpoch == UNDEFINED\_EPOCH) {

....

OffsetTruncationState(min(leaderEpochOffset.endOffset, logEndOffset(tp)), truncationCompleted = true)

} else {

// 获取 follower 副本的 logEndOffset，后面计算的截断位置必须小于或者等于该值

val replicaEndOffset \= logEndOffset(tp)

// 调用 endOffsetForEpoch 方法，在 follower 副本中找到小于或者等于 leader-lastEpoch 的最大 epoch 以及 epoch 结束偏移量，下面称为 follow-lastEpoch、follow-lastEpochEnd。

endOffsetForEpoch(tp, leaderEpochOffset.leaderEpoch) match {

case Some(OffsetAndEpoch(followerEndOffset, followerEpoch)) =>

if (followerEpoch != leaderEpochOffset.leaderEpoch) {

// 如果 follow-lastEpoch 小于 leader-lastEpoch，则截断到 follow-lastEpochEnd 的位置

val intermediateOffsetToTruncateTo \= min(followerEndOffset, replicaEndOffset)

....

OffsetTruncationState(intermediateOffsetToTruncateTo, truncationCompleted = false)

} else {

// 如果 follow-lastEpoch 等于 leader-lastEpoch，则截断到 follow-lastEpochEnd、leader-lastEpochEnd 较小值对应的位置。

val offsetToTruncateTo \= min(followerEndOffset, leaderEpochOffset.endOffset)

OffsetTruncationState(min(offsetToTruncateTo, replicaEndOffset), truncationCompleted = true)

}

case None \=\>

// 如果找不到 follow-lastEpoch，则截断到 leader-lastEpochEnd

OffsetTruncationState(min(leaderEpochOffset.endOffset, replicaEndOffset), truncationCompleted = true)

}

}

}

步骤如下：

1.  获取 follower 副本的 logEndOffset，后面计算的截断位置必须小于或者等于该值。
2.  调用 endOffsetForEpoch 方法，在 follower 副本中找到小于或者等于 leader-lastEpoch 的最大 epoch 以及 epoch 结束偏移量，下面称为 follow-lastEpoch、follow-lastEpochEnd。
3.  如果 follow-lastEpoch 小于 leader-lastEpoch，则截断到 follow-lastEpochEnd 的位置。
4.  如果 follow-lastEpoch 等于 leader-lastEpoch，则截断到 follow-lastEpochEnd、leader-lastEpochEnd 较小值对应的位置。
5.  如果找不到 follow-lastEpoch，则截断到 leader-lastEpochEnd。

接着来看下 [truncateToHighWatermark](http://truncatetohighwatermark%20/) 方法。

private\[server\] def truncateToHighWatermark(partitions: Set\[TopicPartition\]): Unit = inLock(partitionMapLock) {

val fetchOffsets \= mutable.HashMap.empty\[TopicPartition, OffsetTruncationState\]

// 遍历每个要执行截断操作的分区对象

for (tp <- partitions) {

// 获取分区的分区读取状态

val partitionState \= partitionStates.stateValue(tp)

if (partitionState != null) {

// 取出高水位值，分区的最大可读取位移值就是高水位值

val highWatermark \= partitionState.fetchOffset

// 告诉 Kafka 要把指定分区下副本截断到哪个位移值

val truncationState \= OffsetTruncationState(highWatermark, truncationCompleted = true)

info(s"Truncating partition $tp to local high watermark $highWatermark")

// 执行截断到高水位值

if (doTruncate(tp, truncationState))

fetchOffsets.put(tp, truncationState)

}

}

// 更新这组分区的分区读取状态

updateFetchOffsetAndMaybeMarkTruncationComplete(fetchOffsets)

}

该方法主要用来**将日志截断到 Leader Epoch 值对应的位移值处**，步骤如下：

1.  首先遍历每个要执行截断操作的分区对象。
2.  然后获取分区的分区读取状态，判断分区读取状态是否不为空，如果不为空：
3.  依次每个分区获取当前的高水位值，并将其保存在前面提到的分区读取状态类中。
4.  调用 doTruncate 方法执行真正的日志截断操作。
5.  等到将给定的所有分区都执行了对应的操作之后，更新这组分区的分区读取状态。

来看下最后一步，更新这组分区的分区读取状态，看看它做了什么：

private def updateFetchOffsetAndMaybeMarkTruncationComplete(fetchOffsets: Map\[TopicPartition, OffsetTruncationState\]): Unit = {

// 保存更新后的分区 fetch 状态信息

val newStates: Map\[TopicPartition, PartitionFetchState\] = partitionStates.partitionStateMap.asScala

.map { case (topicPartition, currentFetchState) =>

// 匹配是否截断成功

val maybeTruncationComplete \= fetchOffsets.get(topicPartition) match {

case Some(offsetTruncationState) =>

// 获取最新的 leader epoch

val lastFetchedEpoch \= latestEpoch(topicPartition)

val state \= if (isTruncationOnFetchSupported || offsetTruncationState.truncationCompleted)

// 如果支持截断操作或者截断操作已完成，设置状态为 Fetching

Fetching

else

// 否则，设置状态为 Truncating

Truncating

// 构建更新后的分区 fetch 状态

PartitionFetchState(offsetTruncationState.offset, currentFetchState.lag,

currentFetchState.currentLeaderEpoch, currentFetchState.delay, state, lastFetchedEpoch)

case None \=\> currentFetchState

}

(topicPartition, maybeTruncationComplete)

}

// 更新分区 fetch 状态信息

partitionStates.set(newStates.asJava)

}

该方法只有在 truncateToEpochEndOffsets 和 truncateToHighWatermark 方法中最后一步来执行，**它表示 truncate 之后，更新标记截断完成，然后更新 partitionStates 值，如果它们的 offsetTruncationState 截断完成，则将它们标记为截断已完成，表示可以读取了**。接着构建更新后的分区 fetch 状态，最后更新分区 fetch 状态信息。

## **5.3 AbstractFetcherThread#maybeFetch()**

剖析完了截断操作，接着我们回来继续剖析拉取过程。

private def maybeFetch(): Unit = {

val fetchRequestOpt \= inLock(partitionMapLock) {

// 调用子类实现 ReplicaFetcherThread.scala#buildFetch() 方法构建 Fetch 请求，即为 partitionStates 中的分区构造 FetchRequest, partitionStates 中保存的是要去获取消息的分区以及对应的状态

val ResultWithPartitions(fetchRequestOpt, partitionsWithError) = buildFetch(partitionStates.partitionStateMap.asScala)

// 处理出错的分区，处理方式主要是将这个分区加入到有序 LinkedHashMap 末尾等待后续重试

handlePartitionsWithErrors(partitionsWithError, "maybeFetch")

// 如果当前没有可读取的分区，则等待 fetchBackOffMs 时间等候后续重试

if (fetchRequestOpt.isEmpty) {

trace(s"There are no active partitions. Back off for $fetchBackOffMs ms before sending a fetch request")

partitionMapCond.await(fetchBackOffMs, TimeUnit.MILLISECONDS)

}

fetchRequestOpt

}

// 发送 FETCH 请求给 Leader 副本，并处理 Response

fetchRequestOpt.foreach { case ReplicaFetch(sessionPartitions, fetchRequest) =>

processFetchRequest(sessionPartitions, fetchRequest)

}

}

核心步骤如下：

1.  调用子类实现 [ReplicaFetcherThread.scala#buildFetch](http://replicafetcherthread.scala/#buildFetch)() 方法构建 Fetch 请求，即为 partitionStates 中的分区构造 FetchRequest.Builder 对象, partitionStates 中保存的是要去获取消息的分区以及对应的状态。这一步返回的结果如下：
2.  一个对象是ReplicaFetch，即要读取的分区核心信息+ FetchRequest.Builder 对象。而这里的核心信息，就是指要读取哪个分区，从哪个位置开始读，最多读多少字节等等。
3.  另一个对象是 partitionsWithError：它是一组出错分区。
4.  处理这组出错分区。处理方式是将这组分区加入到有序 LinkedHashMap 末尾等待后续重试。
5.  如果发现当前没有任何可读取的分区，会阻塞等待 fetchBackOffMs 时间后续重试。
6.  调用 [AbstractFetcherThread.scala#processFetchRequest](http://abstractfetcherthread.scala/#processFetchRequest)() 方法发送 FETCH 请求给对应的 Leader 副本，并处理响应的 Response，也就是 processFetchRequest 方法要做的事情。

## **5.4 AbstractFetcherThread#buildFetch()**

接着，我们来剖析「**构建拉取消息的请求**」的方法。

override def buildFetch(partitionMap: Map\[TopicPartition, PartitionFetchState\]): ResultWithPartitions\[Option\[ReplicaFetch\]\] = {

// 出现错误的分区列表

val partitionsWithError \= mutable.Set\[TopicPartition\]()

// 构建 fetchSessionHandler Builder 对象

val builder \= fetchSessionHandler.newBuilder(partitionMap.size, false)

// 遍历每个分区

partitionMap.forKeyValue { (topicPartition, fetchState) =>

// We will not include a replica in the fetch request if it should be throttled.

// 分区可读取 && 未被限流

if (fetchState.isReadyForFetch && !shouldFollowerThrottle(quota, fetchState, topicPartition)) {

try {

// 获取日志起始位移值

val logStartOffset \= this.logStartOffset(topicPartition)

val lastFetchedEpoch \= if (isTruncationOnFetchSupported)

fetchState.lastFetchedEpoch.map(\_.asInstanceOf\[Integer\]).asJava

else

Optional.empty\[Integer\]

// 构建读取对象 PartitionData 添加到 builder 后续统一处理

builder.add(topicPartition, new FetchRequest.PartitionData(

fetchState.fetchOffset,

logStartOffset,

fetchSize,

Optional.of(fetchState.currentLeaderEpoch),

lastFetchedEpoch))

} catch {

case \_: KafkaStorageException =>

// 对于有错误的分区加入到出错分区列表

partitionsWithError += topicPartition

}

}

}

// 生成 fetchRequestData

val fetchData \= builder.build()

// 判断 Builder 有无可读取的分区

val fetchRequestOpt \= if (fetchData.sessionPartitions.isEmpty && fetchData.toForget.isEmpty) {

None

} else {

// 构造 FETCH 请求的 Builder 对象

val requestBuilder \= FetchRequest.Builder

.forReplica(fetchRequestVersion, replicaId, maxWait, minBytes, fetchData.toSend)

.setMaxBytes(maxBytes)

.toForget(fetchData.toForget)

.metadata(fetchData.metadata)

Some(ReplicaFetch(fetchData.sessionPartitions(), requestBuilder))

}

// 返回 Builder 对象以及出错分区列表

ResultWithPartitions(fetchRequestOpt, partitionsWithError)

}

该方法主要用来**为一组特定分区构造 FETCH 请求的 Builder 对象然后返回**，步骤如下：

1.  初始化出现错误的分区列表，构建 fetchSessionHandler Builder 对象。
2.  遍历每个分区，判断分区可读取 && 未被限流，如果满足：
3.  获取日志起始位移值， 构建读取对象 PartitionData 添加到 builder 后续统一处理，如果失败则对于有错误的分区加入到出错分区列表。
4.  生成 fetchRequestData 。
5.  判断 Builder 有无可读取的分区，如果无，返回 None。否则构造 FETCH 请求的 Builder 对象。
6.  返回 Builder 对象以及出错分区列表。

> 这里需要注意的是： Fetch 请求中会线程初始化时设置进来的本地日志的 LEO 填充到 fetchOffset 参数。

## **5.5 AbstractFetcherThread#processFetchRequest()**

接着来看处理 Fetch 请求的方法，源码如下：

// 发送 FETCH 请求给对应的 Leader 副本，并处理相应的 Response

private def processFetchRequest(sessionPartitions: util.Map\[TopicPartition, FetchRequest.PartitionData\],fetchRequest: FetchRequest.Builder): Unit = {

val partitionsWithError \= mutable.Set\[TopicPartition\]()

val divergingEndOffsets \= mutable.Map.empty\[TopicPartition, EpochEndOffset\]

var responseData: Map\[TopicPartition, FetchData\] = Map.empty

try {

trace(s"Sending fetch request $fetchRequest")

// 给 Leader 发送 FETCH 请求

responseData = fetchFromLeader(fetchRequest)

} catch {

....

}

// 更新请求发送速率指标

fetcherStats.requestRate.mark()

if (responseData.nonEmpty) {

// process fetched data

inLock(partitionMapLock) {

// 遍历 Fetch 结果

responseData.forKeyValue { (topicPartition, partitionData) =>

Option(partitionStates.stateValue(topicPartition)).foreach { currentFetchState =>

// 获取分区核心信息

val fetchPartitionData \= sessionPartitions.get(topicPartition)

// 处理 Response 的条件：

// 1. 要获取的位移值和之前已保存的下一条待获取位移值相等

// 2. 当前分区处于可获取状态

if (fetchPartitionData != null && fetchPartitionData.fetchOffset == currentFetchState.fetchOffset && currentFetchState.isReadyForFetch) {

// 匹配分区数据的错误信息

partitionData.error match {

// 如果没有错误

case Errors.NONE =>

try {

// 交由子类完成 Response 的处理

// 如果 Fetch 响应中有消息数据，则调用子类实现方法 ReplicaFetcherThread.scala#processPartitionData() 将消息追加到本地日志

val logAppendInfoOpt \= processPartitionData(topicPartition, currentFetchState.fetchOffset, partitionData)

logAppendInfoOpt.foreach { logAppendInfo =>

val validBytes \= logAppendInfo.validBytes

val nextOffset \= if (validBytes > 0) logAppendInfo.lastOffset + 1 else currentFetchState.fetchOffset

val lag \= Math.max(0L, partitionData.highWatermark - nextOffset)

fetcherLagStats.getAndMaybePut(topicPartition).lag = lag

if (validBytes > 0 && partitionStates.contains(topicPartition)) {

val newFetchState \= PartitionFetchState(nextOffset, Some(lag),

currentFetchState.currentLeaderEpoch, state = Fetching,

logAppendInfo.lastLeaderEpoch)

// 将该分区放置在有序 LinkedHashMap 读取顺序的末尾，保证公平性

partitionStates.updateAndMoveToEnd(topicPartition, newFetchState)

fetcherStats.byteRate.mark(validBytes)

}

}

// 检查是否支持基于 FETCH 请求截断某些分区中的消息。在支持截断的情况下，检查 Follower 从分区中获取的 epoch 是否发生了变更，如果 epoch 发生了变更，那么 Follower 获取到的消息 Offset 就不是最新的了，所以需要对截断后的分区数据最新 Offset 进行更新。

// 如果是 Kafka 2.7 机以上版本，则需要获取 Leader 副本返回的 divergingEpochs

if (isTruncationOnFetchSupported) {

// 遍历 divergingEpochs 中的所有元素

partitionData.divergingEpoch.ifPresent { divergingEpoch =>

// 根据 divergingEpoch 的信息，构建出对应的 EpochEndOffset 结构体；将构建出的 EpochEndOffset 结果保存在 divergingEndOffsets 集合中。最终，divergingEndOffsets 集合保存了所有支持截断的分区的 Offset 信息。

divergingEndOffsets += topicPartition -> new EpochEndOffset()

.setPartition(topicPartition.partition)

.setErrorCode(Errors.NONE.code)

.setLeaderEpoch(divergingEpoch.epoch)

.setEndOffset(divergingEpoch.endOffset)

}

}

} catch {

....

}

// 如果读取位移值越界，通常是因为 Leader 发生变更

case Errors.OFFSET\_OUT\_OF\_RANGE =>

// 调整越界，主要办法是做截断

if (handleOutOfRangeError(topicPartition, currentFetchState, fetchPartitionData.currentLeaderEpoch))

// 如果依然不能成功，加入到出错分区列表

partitionsWithError += topicPartition

// 如果 Leader Epoch 值比 Leader 所在 Broker 上的 Epoch 值要新

case Errors.UNKNOWN\_LEADER\_EPOCH =>

debug(s"Remote broker has a smaller leader epoch for partition $topicPartition than " + s"this replica's current leader epoch of ${currentFetchState.currentLeaderEpoch}.")

// 加入到出错分区列表

partitionsWithError += topicPartition

// 如果 Leader Epoch 值比 Leader 所在 Broker 上的 Epoch 值要旧

case Errors.FENCED\_LEADER\_EPOCH =>

if (onPartitionFenced(topicPartition, fetchPartitionData.currentLeaderEpoch))

partitionsWithError += topicPartition

// 如果 Leader 发生变更

case Errors.NOT\_LEADER\_OR\_FOLLOWER =>

debug(s"Remote broker is not the leader for partition $topicPartition, which could indicate " + "that the partition is being moved")

// 加入到出错分区列表

partitionsWithError += topicPartition

// 如果主题、分区不识别

case Errors.UNKNOWN\_TOPIC\_OR\_PARTITION =>

warn(s"Received ${Errors.UNKNOWN\_TOPIC\_OR\_PARTITION} from the leader for partition $topicPartition. " + "This error may be returned transiently when the partition is being created or deleted, but it is not " + "expected to persist.")

// 加入到出错分区列表

partitionsWithError += topicPartition

case \_ \=\>

error(s"Error for partition $topicPartition at offset ${currentFetchState.fetchOffset}", partitionData.error.exception)

// 加入到出错分区列表

partitionsWithError += topicPartition

}

}

}

}

}

}

// 如果 divergingEndOffsets 集合中保存了支持截断操作的分区 Offset 信息，那么就调用 truncateOnFetchResponse 方法来执行相应的处理。

if (divergingEndOffsets.nonEmpty)

// 将遍历 divergingEndOffsets 集合中的所有分区 Offset 信息，然后执行对应的截断操作，以确保Follower 获取的分区数据与 broker 中的数据保持一致

truncateOnFetchResponse(divergingEndOffsets)

if (partitionsWithError.nonEmpty) {

// 处理出错分区列表

handlePartitionsWithErrors(partitionsWithError, "processFetchRequest")

}

}

该方法主要用来**发送 FETCH 请求给对应的 Leader 副本，并处理相应的 Response**，步骤如下：

1.  首先调用子类 [ReplicaFetcherThread.scala#fetchFromLeader](http://replicafetcherthread.scala/#fetchFromLeader)() 方法给 Leader 发送 FETCH 请求，并且这里会阻塞线程等待 Leader 副本返回 Response，然后更新 FETCH请求发送速率的监控指标。
2.  拿到 Response 之后从中取出分区的核心信息，然后比较要读取的位移值，和当前 AbstractFetcherThread 线程缓存的、该分区下一条待读取的位移值是否相等，以及当前分区是否处于可获取状态。
3.  如果不满足这两个条件，说明这个 Request 可能是一个之前等待了许久都未处理的请求，压根就不用处理了。
4.  如果满足这两个条件且 Response 没有错误，即 Fetch 响应中有消息数据，此时提取 Response 中的 Leader Epoch 值，然后交由子类 processPartitionData 实现具体的 Response 处理：将消息追加到本地日志，接着将该分区放置在有序 LinkedHashMap 的末尾以保证公平性。
5.  如果该 Response 有错误，那么就调用对应错误的处理逻辑，然后将出错分区加入到出错分区列表中。
6.  对于 Kafka 2.7 机以上版本，则需要获取 Leader 副本返回的 divergingEpochs。如果 Leader 副本返回了 divergingEpochs 属性，则 Follower 副本需要执行截断操作。
7.  如果当前服务端版本支持在 Fetch 时进行日志截断操作，那么处理 Fetch 响应时会同步收集其携带的版本分歧信息。因为分区主从副本的版本不一致通常是发生了故障恢复，分区 Follower 副本可能需要进行日志截断以保持和 Leader 副本数据一致，这部分最后由 [AbstractFetcherThread.scala#truncateOnFetchResponse()](http://abstractfetcherthread.scala/#truncateOnFetchResponse\(\)%20) 方法处理。
8.  调用 handlePartitionsWithErrors 方法，统一处理上一步处理过程中出现错误的分区。

## **5.6 ReplicaFetcherThread#processPartitionData()**

override def processPartitionData(topicPartition: TopicPartition,

fetchOffset: Long, partitionData: FetchData): Option\[LogAppendInfo\] = {

val logTrace \= isTraceEnabled

// 从副本管理器获取指定主题分区对象

val partition \= replicaMgr.getPartitionOrException(topicPartition)

// 获取日志对象

val log \= partition.localLogOrException

// 将获取到的数据转换成符合格式要求的消息集合

val records \= toMemoryRecords(partitionData.records)

// 校验消息格式

maybeWarnIfOversizedRecords(records, topicPartition)

// 如果要读取的起始位移值不是本地日志 LEO 值则认为是异常情况

if (fetchOffset != log.logEndOffset)

throw new IllegalStateException("Offset mismatch for partition %s: fetched offset = %d, log end offset = %d.".format(topicPartition, fetchOffset, log.logEndOffset))

....

// 写入 Follower 副本本地日志

val logAppendInfo \= partition.appendRecordsToFollowerOrFutureReplica(records, isFuture = false)

....

// 获取 Leader LSO 值

val leaderLogStartOffset \= partitionData.logStartOffset

// 尝试更新 Follower 副本的高水位值

val followerHighWatermark \= log.updateHighWatermark(partitionData.highWatermark)

// 尝试更新 Follower 副本的日志起始位移值

log.maybeIncrementLogStartOffset(leaderLogStartOffset, LeaderOffsetIncremented)

....

// 副本消息拉取限流

if (quota.isThrottled(topicPartition))

quota.record(records.sizeInBytes)

// 更新统计指标值

if (partition.isReassigning && partition.isAddingLocalReplica)

brokerTopicStats.updateReassignmentBytesIn(records.sizeInBytes)

brokerTopicStats.updateReplicationBytesIn(records.sizeInBytes)

// 返回日志写入结果

logAppendInfo

}

该方法主要用来**处理从 Leader 副本获取到的消息，然后写入本地日志中**，步骤如下：

1.  从副本管理器获取指定主题分区对象、日志对象。
2.  将获取到的数据转换成符合格式要求的消息集合。
3.  如果要读取的起始位移值不是本地日志 LEO 值则认为是异常情况，此时会抛异常。
4.  否则，调用 [Partition.scala#appendRecordsToFollowerOrFutureReplica()](http://partition.scala/#appendRecordsToFollowerOrFutureReplica\(\)) 方法写入消息集合到 Follower 副本本地日志中。
5.  消息写入完成后，需要把 Fetch 响应中的 HW 取出来，尝试更新 Follower 副本的高水位值「**将 FETCH 请求 Response 中包含的高水位作为新的高水位**」、日志起始位移值。那为什么 Log Start Offset 值也可能发生变化呢？这是因为 **Leader 的 LogStartOffset 可能发生变化，比如用户手动删除消息的操作等**，Follower 副本的日志需要和 Leader 副本保持严格的一致。
6.  副本消息拉取限流、更新统计指标值。
7.  返回日志写入结果。

## **5.7 Partition#updateFollowerFetchState()**

至于分区 Leader 副本对来自 Follower 的 Fetch 请求的处理，可以查看 [【服务端 Broker 源码分析系列第九篇】图解 Kafka 源码之 ReplicaManager 日志读写流程](https://articles.zsxq.com/id_5dq174gc0wji.html) 3.2 fetchMessages() 节，此时将会触发[ReplicaManager.scala#updateFollowerFetchState()](http://replicamanager.scala/#updateFollowerFetchState\(\)) 方法执行，其核心处理执行 [Partition.scala#updateFollowerFetchState()](http://partition.scala/#updateFollowerFetchState\(\)%20) 方法以便更新 Leader 副本保存的远程副本列表中的 LEO。

![](https://article-images.zsxq.com/FqRY4SQAXIZEjcbk61-E5sETlFNx)

\`\`\`

def updateFollowerFetchState(followerId: Int,

followerFetchOffsetMetadata: LogOffsetMetadata, // follower最新抓取的offset元数据

followerStartOffset: Long, // follower的起始offset

followerFetchTimeMs: Long,

leaderEndOffset: Long): Boolean = {

getReplica(followerId) match {

case Some(followerReplica) =>

val oldLeaderLW \= if (delayedOperations.numDelayedDelete > 0) lowWatermarkIfLeader else -1L

// 1、更新发出 Fetch 请求的 Follower 副本的 LEO 等信息

val prevFollowerEndOffset \= followerReplica.logEndOffset

followerReplica.updateFetchState(

followerFetchOffsetMetadata,

followerStartOffset,

followerFetchTimeMs,

leaderEndOffset)

val newLeaderLW \= if (delayedOperations.numDelayedDelete > 0) lowWatermarkIfLeader else -1L

val leaderLWIncremented \= newLeaderLW > oldLeaderLW // 检查分区的LW是否增加

// 2、如果 followerReplica 处于 ISR 副本中，则检查是否需要将其添加到 ISR 副本中

maybeExpandIsr(followerReplica, followerFetchTimeMs)

// 检查分区的 HW 是否可以增加

val leaderHWIncremented \= if (prevFollowerEndOffset != followerReplica.logEndOffset) {

inReadLock(leaderIsrUpdateLock) {

// 3、调用 Partition.scala#maybeIncrementLeaderHW() 方法在处理 Follower 端 Fetch 请求时尝试更新分区 HW，只有 HW 更新后新消息才算是 committed 状态，可以被消费者消费

leaderLogIfLocal.exists(leaderLog => maybeIncrementLeaderHW(leaderLog, followerFetchTimeMs))

}

} else {

false

}

// 4、如果分区的LW或HW增加，则可能触发之前延迟的请求立即执行

if (leaderLWIncremented || leaderHWIncremented)

tryCompleteDelayedRequests()

debug(s"Recorded replica $followerId log end offset (LEO) position " +

s"${followerFetchOffsetMetadata.messageOffset} and log start offset $followerStartOffset.")

true

case None \=\>

false

}

}

核心步骤如下：

1.  更新发出 Fetch 请求的 Follower 副本的 LEO 等信息。
2.  如果 followerReplica 处于 ISR 副本中，则检查是否需要将其添加到 ISR 副本中。
3.  调用 [Partition.scala#maybeIncrementLeaderHW](http://partition.scala/#maybeIncrementLeaderHW)() 方法在处理 Follower 端 Fetch 请求时尝试更新分区 HW，只有 HW 更新后新消息才算是 committed 状态，可以被消费者消费。
4.  如果分区的LW或HW增加，则可能触发之前延迟的请求立即执行。

至此，「**主从副本消息数据同步源码**」流程基本剖析完了。

## **06 Zookeeper、KRaft 模式创建主题区别**

我们来总结下这两种模式下创建主题的区别，大概有以下两点不同：

1.  在 Zookeeper 模式下创建主题，KafkaController 节点发送 LeaderAndIsr 请求，当 Broker 端收到请求后，回成为分区的 Leader 副本或者 Follower 副本。
2.  而在 KRaft 模式下创建主题，Broker 同步并提交 TopicRecord、PartitionRecord 等数据，发现自己被分配为新分区的副本后，成功该分区的 Leader 副本或者 Follower 副本。

## **07 主从副本消息数据同步流程总结**

不知道，大家看完上面的源码是否还是云里雾里，找不着北。不过没关系，这里我还是通过一个案例场景来图解一下，希望可以让你更容易理解今天的内容，废话不多说，开始正题。

这里还是以「**创建 Topic 场景流程**」为例，从消息数据分区副本主从同步的场景来分析这个过程。当创建 Topic 后，整个消息变更发布与消费的完整流程如下图所示：

  
![](https://article-images.zsxq.com/lvduatKmdIR-Y9QlAUtqdO9P-RJE)

接着来看下消息主从同步的流程，如下图：

![](https://article-images.zsxq.com/lmT6mHBMszfIqUn75wjSJmz02zv4)

![](https://article-images.zsxq.com/lkThR1QblivnZT3-XgwTskq76FTr)

如图所示，对于消息写入后 「**Follower 副本**」通过 Fetch 请求完成消息数据及 HW 更新的过程，大致可以分为以下 4 个阶段：

1.  **初始状态**：当某个分区的 Leader 副本和所有 Follower 副本保存的消息都一致时，HW 与 LEO 指向同一个位置。**如上图中，Leader 副本和 Follower 副本都保存了 Offset = 0 的消息，HW 和 LEO 都指向还未写入的 Offset = 1 的消息位置**。
2.  **当有消息写入时**：Leader 副本所在节点接收生产消息请求，会将消息写入到本地的分区副本，此时 Leader 副本 LEO 指向 Offset=2 的位置。同时 **Leader 副本还会尝试更新本地日志的 HW 高水位**，不过在当前阶段实际不会更新 HW 水位的。
3.  具体的状态更新具体算法如下：
4.  遍历分区内保存远程副本的 remoteReplicasMap，取所有ISR列表及消息数据落后但正在追上 Leader (由配置 [replica.lag.time.max.ms](http://replica.lag.time.max.ms/) 决定)的副本中最小的 LEO 作为新水位候选，即 new HW = min(LEOs)。这里需要注意的是：这种算法实际上意味着对消息数据主从同步的强一致性要求，只有所有活跃的分区副本都保存了消息数据，这条消息才对外可见，才能被消费者消费。
5.  为防止单调递增的分区 HW 降低，取分区 Leader 本地的 old HW 与 new HW 比较，如果 old HW < new HW，则更新分区 HW 为 new HW，否则不更新。
6.  **Follower 节点第一次发起 Fetch 请求**：Follower 副本节点会通过 Fetcher 线程定时发送 Fetch 请求到 Leader 副本同步消息，发起的请求中会携带本地分区副本的 LEO=1。 Leader 副本所在节点接收到请求后，更新目标分区 remoteReplicasMap 中保存的该 Follower 副本状态，尝试更新本地日志HW。**此时只要当前 Follower 副本不是最后一个来同步消息的， Leader 副本就不会更新本地 HW，仅仅返回消息记录**。Follower 副本节点在处理 Fetch 响应时，仅会将消息追加到本地日志，并将 LEO 指向 Offset=2 的位置。
7.  **Follower 节点第二次发起 Fetch 请求**：与第一次 Fetch 请求流程类似，只不过此时 Fetcher 线程发起的请求中会携带本地分区副本的 LEO=2。假设此时已经有所有 Follower 副本都保存了新消息，那么 Leader 副本节点在尝试更新本地日志 HW 时会成功更新本地 HW 指向 Offset=2 的位置，并在 Fetch 响应中将当前 HW 返回给 Follower。Follower 根据 Leader 的 HW 更新本地副本 HW 指向 Offset=2 的位置，最终完成 HW 更新。

最后通过一张源码时序图来梳理整个处理过程，如下图：

![](https://article-images.zsxq.com/Fm0dhcoMmv7Gt8p8ApKGxJNNrbDU)

## **08 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头引出了「**上篇中元数据主从同步机制**」中提到了元数据的变动会被「**Broker 模块监听处理后**」才能对集群产生影响，那么 Leader、Follower 副本是如何同步消息数据的。

2、接着带大家从四个维度「**监听器机制**」、「**元数据变动发布源码流程**」、「**元数据变动消费源码流程**」、「**主从副本消息数据同步源码流程**」深度剖析了主从同步整体源码流程。

3、最后通过两张场景流程图带大家梳理了整个消息数据主从同步处理流程，希望让你更好的理解。

下篇我们来深度剖析「**Kraft 分区副本失败选主机制**」，大家期待，我们下期见。