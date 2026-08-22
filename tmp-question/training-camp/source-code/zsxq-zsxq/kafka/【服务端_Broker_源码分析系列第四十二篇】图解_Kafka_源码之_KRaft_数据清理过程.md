大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端** **KRaft 节点监控与故障转移流程**」，深度剖析Kafka Kraft 模式下「**节点故障如何转移的**」的两个维度，让你更好的理解其处理全流程，今天我们接着来深度聊聊「**Kafka 服务端 KRaft 数据清理过程**」，看看 Kafka Kraft 模式下「**快照是如何管理的以及历史数据是如何清理的**」。

![](https://article-images.zsxq.com/FkIoExySOJJrwSI4iiLn3WJWFvi8)

## **01 总体概述**

本篇我们来剖析一下「**Kafka KRaft 模块**」的另一个重要功能：快照管理以及历史数据清理操作。

前面几篇我们都讲解提交元数据变更数据会存储到 Kafka 内部 Topic 「**\_\_cluster\_metadata**」中，所以它存储了集群中元数据的所有变更操作，但是在集群运行过程中可能存在大量的冗余数据，比如：

1.  已删除的主题信息。
2.  Broker 节点上重复上下线信息。

所以 KRaft 模块需要清除这些冗余数据，从而减少内部 Topic 「**\_\_cluster\_metadata**」的占用空间。

本文涉及的源码：

「**ControllerServer**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/ControllerServer.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/ControllerServer.scala)

「**KafkaMetadataLog**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/KafkaMetadataLog.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/KafkaMetadataLog.scala)

「**KafkaConfig**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/KafkaConfig.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/KafkaConfig.scala)

「**QuorumController**」类源码在 Kafka 源码包的 metadata 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/controller/QuorumController.java](https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/image/MetadataDelta.java)

「**ControllerPurgatory**」类源码在 Kafka 源码包的 metadata 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/controller/ControllerPurgatory.java](https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/controller/ControllerPurgatory.java)

「**SnapshotRegistry**」类源码在 Kafka 源码包的 metadata 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/timeline/SnapshotRegistry.java](https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/timeline/SnapshotRegistry.java)

「**SnapshotRegistry**」类源码在 Kafka 源码包的 raft 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/raft/src/main/java/org/apache/kafka/raft/KafkaRaftClient.java](https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/timeline/SnapshotRegistry.java)

## **02 快照文件清理规则**

KRaft 模块会定时将所有的元数据写入一个快照文件中，并将当前「**\_\_cluster\_metadata**」的 endOffset 记录下来，这里我们称它为「**快照结束偏移量**」，该偏移量之前的 Record 数据都已经存储到快照文件中。

当生成快照文件后，KRaft 模块就可以清理该「**快照结束偏移量**」之前的 Record 数据了。

##   
**03 QuorumMetaLogListener 监听器**

在 [【服务端 Broker 源码分析系列第四十篇】图解 Kafka 源码之 KRaft 消息数据主从同步机制](https://articles.zsxq.com/id_3o7516uxtd4b.html) 这篇中，我们剖析了 [KafkaRaftClient#listenerContexts](http://kafkaraftclient/#listenerContexts) 中的  [BrokerMetadataListener](http://brokermetadatalistener/) 监听器，今天我们来剖析另外一个 [QuorumMetaLogListener](http://quorummetaloglistener%20/) 监听器，它是执行 Controller 节点的相关逻辑。

当 Controller 节点启动时，[ControllerServer](http://controllerserver%20/) 会构建 [QuorumController](http://quorumcontroller/)， 而在 [QuorumController](http://quorumcontroller/) 的构造方法会向 [KafkaRaftClient#listenerContexts](http://kafkaraftclient/#listenerContexts) 注册 [QuorumMetaLogListener](http://brokermetadatalistener%20/) 监听器，源码如下：

![](https://article-images.zsxq.com/FnipQDhHABDqf6h7DFoCIbiTi2Sg)

[QuorumMetaLogListener](http://brokermetadatalistener%20/) 监听器负责执行 Controller 节点提交 Record 数据的相关逻辑，比如：完成「**完成延迟任务**」、「**生成快照**」等等。

## **3.1 QuorumMetaLogListener#handleCommit()**

@Override

public void handleCommit(BatchReader<ApiMessageAndVersion> reader) {

// 1、在待处理任务列表 QuorumController#queue 添加一个任务，异步执行后续逻辑

appendRaftEvent("handleCommit\[baseOffset=" + reader.baseOffset() + "\]", () -> {

try {

boolean isActiveController \= curClaimEpoch != -1;

long processedRecordsSize \= 0;

while (reader.hasNext()) {

Batch<ApiMessageAndVersion> batch = reader.next();

long offset \= batch.lastOffset();

int epoch \= batch.epoch();

List<ApiMessageAndVersion> messages = batch.records();

if (isActiveController) {

// 2、触发 QuorumController#purgatory 中的延迟任务，当 Leader 节点处理 Controller 请求后，会在 QuorumController#purgatory 中添加延迟任务，当完成这些任务后将 Controller请求返回给客户端。

purgatory.completeUpTo(offset);

// 3、删除不再需要的快照

snapshotRegistry.deleteSnapshotsUpTo( snapshotGeneratorManager.snapshotLastOffsetFromLog().orElse(offset));

} else {

// If the controller is a standby, replay the records that were

// created by the active controller.

if (log.isDebugEnabled()) {

if (log.isTraceEnabled()) {

log.trace("Replaying commits from the active node up to " +

"offset {} and epoch {}: {}.", offset, epoch, messages.stream()

.map(ApiMessageAndVersion::toString)

.collect(Collectors.joining(", ")));

} else {

log.debug("Replaying commits from the active node up to " +

"offset {} and epoch {}.", offset, epoch);

}

}

for (ApiMessageAndVersion messageAndVersion : messages) {

replay(messageAndVersion.message(), Optional.empty(), offset);

}

}

lastCommittedOffset = offset;

lastCommittedEpoch = epoch;

lastCommittedTimestamp = batch.appendTimestamp();

processedRecordsSize += batch.sizeInBytes();

}

// 4、根据需要生成新的快照

maybeGenerateSnapshot(processedRecordsSize);

} finally {

reader.close();

}

});

}

核心步骤如下：

1.  在待处理任务列表 [QuorumController#queue](http://quorumcontroller/#queue) 添加一个任务，异步执行后续逻辑。
2.  触发 [QuorumController#purgatory](http://quorumcontroller/#purgatory) 中的延迟任务，当 Leader 节点处理 Controller 请求后，会在 [QuorumController#purgatory](http://quorumcontroller/#purgatory) 中添加延迟任务，当完成这些任务后将 Controller 请求返回给客户端。
3.  删除不再需要的快照。
4.  根据需要生成新的快照。

##   
**04 快照数据管理**

所有的快照信息都存储在 [KafkaMetadataLog#snapshots](http://kafkametadatalog/#snapshots) 中，如下：

![](https://article-images.zsxq.com/FoxxnNqQsSC26LcSoj_ULwpApbf6)

可以看出，该属性时一个 Map 实例，键值对内容为<快照结束偏移量，快照实例>。

在 **3.1 节中**的最后一步会调用 [QuorumController#maybeGenerateSnapshot()](http://quorumcontroller/#maybeGenerateSnapshot\(\)) 根据需要生成新的快照，条件如下：如果上次生成快照后，新写入的数据量大于 [KafkaConfig#metadataSnapshotMaxNewRecordBytes](http://kafkaconfig/#metadataSnapshotMaxNewRecordBytes) 属性，则生成一个新的数据快照。

![](https://article-images.zsxq.com/FiQGhdVg-oAtVo89f5xt4uLKUiTh)

// KafkaConfig.scala

// metadata.log.max.record.bytes.between.snapshots 配置项可以设置 KafkaConfig#MetadataSnapshotMaxNewRecordBytes 属性，默认值为 20 M

val MetadataSnapshotMaxNewRecordBytesProp \= "metadata.log.max.record.bytes.between.snapshots"

val MetadataSnapshotMaxNewRecordBytes \= 20 \* 1024 \* 1024

## **4.1 QuorumController#maybeGenerateSnapshot()**

生成新的快照数据的源码如下：

private void maybeGenerateSnapshot(long batchSizeInBytes) {

// 更新 newBytesSinceLastSnapshot 字段的值

newBytesSinceLastSnapshot += batchSizeInBytes;

// 如果大于等于 KafkaConfig#metadataSnapshotMaxNewRecordBytes，且还没有创建快照生成器，则进行处理

if (newBytesSinceLastSnapshot >= snapshotMaxNewRecordBytes &&

snapshotGeneratorManager.generator == null

) {

boolean isActiveController \= curClaimEpoch != -1;

// 如果不是活跃控制器，则只有在需要创建快照时才会进行创建，此时调用 getOrCreateSnapshot 方法先创建内存的快照

if (!isActiveController) {

snapshotRegistry.getOrCreateSnapshot(lastCommittedOffset);

}

log.info("Generating a snapshot that includes (epoch={}, offset={}) after {} committed bytes since the last snapshot.",

lastCommittedEpoch, lastCommittedOffset, newBytesSinceLastSnapshot);

// 创建快照生成器

snapshotGeneratorManager.createSnapshotGenerator(lastCommittedOffset, lastCommittedEpoch, lastCommittedTimestamp);

// 重置 newBytesSinceLastSnapshot 字段的值

newBytesSinceLastSnapshot = 0;

}

}

核心步骤如下：

1.  更新 [newBytesSinceLastSnapshot](http://newbytessincelastsnapshot%20/) 字段的值。
2.  如果大于等于 [KafkaConfig#metadataSnapshotMaxNewRecordBytes](http://kafkaconfig/#metadataSnapshotMaxNewRecordBytes)，且还没有创建快照生成器，则进行处理。
3.  如果不是活跃控制器，则只有在需要创建快照时才会进行创建，此时调用 [getOrCreateSnapshot](http://getorcreatesnapshot%20/) 方法先创建内存的快照。
4.  创建快照生成器。
5.  生成完成后重置 [newBytesSinceLastSnapshot](http://newbytessincelastsnapshot/)。

## **4.2 SnapshotRegistry#getOrCreateSnapshot()**

public Snapshot getOrCreateSnapshot(long epoch) {

// 获取最新的快照

Snapshot last \= head.prev();

// 如果最新快照的 epoch 大于要创建的 epoch，则抛出异常

if (last.epoch() > epoch) {

throw new RuntimeException("Can't create a new snapshot at epoch " + epoch +

" because there is already a snapshot with epoch " + last.epoch());

// 如果最新快照的 epoch 等于要创建的 epoch，则直接返回最新快照

} else if (last.epoch() == epoch) {

return last;

}

// 创建新的快照

Snapshot snapshot \= new Snapshot(epoch);

// 将新的快照添加到链表的末尾

last.appendNext(snapshot);

// 将新的快照添加到 snapshots 对应的哈希表中

snapshots.put(epoch, snapshot);

log.debug("Creating snapshot {}", epoch);

// 返回新的快照

return snapshot;

}

方法很简单，可以看到是根据需要是否创建快照，如果不需要则之间返回，如果需要则创建快照。

## **4.3 SnapshotGeneratorManager#createSnapshotGenerator()**

// QuorumController.java 子类

class SnapshotGeneratorManager implements Runnable {

private final ExponentialBackoff exponentialBackoff \= new ExponentialBackoff(10, 2, 5000, 0);

private SnapshotGenerator generator \= null;

void createSnapshotGenerator(long committedOffset, int committedEpoch, long committedTimestamp) {

// 检查是否已存在快照生成器

if (generator != null) {

throw new RuntimeException("Snapshot generator already exists.");

}

// 检查快照注册表中是否存在指定的 committedOffset 对应的快照

if (!snapshotRegistry.hasSnapshot(committedOffset)) {

throw new RuntimeException(

String.format(

"Cannot generate a snapshot at committed offset %s because it does not exists in the snapshot registry.",

committedOffset

)

);

}

// 创建快照写入器

Optional<SnapshotWriter<ApiMessageAndVersion>> writer = raftClient.createSnapshot(

committedOffset,

committedEpoch,

committedTimestamp

);

// 如果快照写入器存在，则创建快照生成器并进行调度

if (writer.isPresent()) {

// 创建快照生成器

generator = new SnapshotGenerator(

logContext,

writer.get(),

MAX\_BATCHES\_PER\_GENERATE\_CALL,

exponentialBackoff,

Arrays.asList(

new Section("features", featureControl.iterator(committedOffset)),

new Section("cluster", clusterControl.iterator(committedOffset)),

new Section("replication", replicationControl.iterator(committedOffset)),

new Section("configuration", configurationControl.iterator(committedOffset)),

new Section("clientQuotas", clientQuotaControlManager.iterator(committedOffset)),

new Section("producerIds", producerIdControlManager.iterator(committedOffset))

)

);

// 添加一个任务到 QuorumController#queue 队列中并调度

reschedule(0);

} else {

log.info(

"Skipping generation of snapshot for committed offset {} and epoch {} since it already exists", committedOffset, committedEpoch);

}

}

void cancel() {

if (generator == null) return;

log.error("Cancelling snapshot {}", generator.lastContainedLogOffset());

generator.writer().close();

generator = null;

// 当取消任务时，清理快照结束偏移量之前的快照数据

snapshotRegistry.deleteSnapshotsUpTo(lastCommittedOffset);

queue.cancelDeferred(GENERATE\_SNAPSHOT);

}

void reschedule(long delayNs) {

ControlEvent event \= new ControlEvent(GENERATE\_SNAPSHOT, this);

// 添加一个任务到 QuorumController#queue 队列中

queue.scheduleDeferred(event.name,

new EarliestDeadlineFunction(time.nanoseconds() + delayNs), event);

}

// 当 QuorumController#queue 队列中有任务时会异步执行 run 方法

@Override

public void run() {

// 如果没有快照生成器，则直接返回

if (generator == null) {

log.debug("No snapshot is in progress.");

return;

}

// 生成下一批快照数据，并返回下次调度的延迟时间

OptionalLong nextDelay;

try {

nextDelay = generator.generateBatches();

} catch (Exception e) {

// 如果在生成快照时抛出异常，则关闭快照写入器并清空快照生成器

log.error("Error while generating snapshot {}", generator.lastContainedLogOffset(), e);

generator.writer().close();

generator = null;

return;

}

// 如果不需要延迟，则表示快照数据已生成完成，关闭快照写入器，并清空快照生成器

if (!nextDelay.isPresent()) {

log.info("Finished generating snapshot {}.", generator.lastContainedLogOffset());

generator.writer().close();

generator = null;

// 删除所有已提交的偏移量之前的内存快照，因为这些快照对于当前快照生成完成后已经不再需要

snapshotRegistry.deleteSnapshotsUpTo(lastCommittedOffset);

return;

}

// 根据返回的下次调度的延迟时间进行调度

reschedule(nextDelay.getAsLong());

}

OptionalLong snapshotLastOffsetFromLog() {

if (generator == null) {

return OptionalLong.empty();

}

return OptionalLong.of(generator.lastContainedLogOffset());

}

}

可以看到 [SnapshotGeneratorManager#createSnapshotGenerator](http://snapshotgeneratormanager/#createSnapshotGenerator) 方法会创建一个快照生成器，并添加一个任务到 [QuorumController#queue](http://quorumcontroller/#queue) 中并进行调度。

当队列中存在任务后会异步调用 [SnapshotGeneratorManager#run()](http://snapshotgeneratormanager/#run\(\)) 方法来生成快照实例，核心步骤如下：

1.  利用 Controller 节点的数据试图来生成快照文件，文件名命名规则：[{offset}-{leaderEpoch}.checkpoint](http://{offset}-{leaderepoch}.checkpoint/)，其中 offset 为快照结束偏移量，leaderEpoch 为 [LeaderState#epoch](http://leaderstate/#epoch) 属性。
2.  创建快照实例并添加到 [KafkaMetadataLog#snapshots](http://kafkametadatalog/#snapshots) 中。

## **05 清理历史数据**

在 [KafkaRaftClient](http://kafkaraftclient%20/) 中，[snapshotCleaner](http://snapshotcleaner/) 属性时一个 [RaftMetadataLogCleanerManager](http://raftmetadatalogcleanermanager/) 实例，负责清理

「**\_\_cluster\_metadata**」主题数据，如下：

![](https://article-images.zsxq.com/FnXm7rH21yh68X2DRpsJgwCW1tCQ)

![](https://article-images.zsxq.com/Fo4a_dPj3F1GsUL5UtzuVBeGqF-D)

在 [【服务端 Broker 源码分析系列第三十六篇】图解 Kafka 源码之 KRaft 请求处理流程](https://articles.zsxq.com/id_eal5tbquenj6.html) 这篇中，在 [KafkaRaftServer](http://kafkaraftserver/) 的启动时会启动 [raftIoThread](http://raftiothread/) 线程，[raftIoThread](http://raftiothread/) 线程就会不断的调用 [KafkaRaftClient#poll](http://kafkaraftclient/#poll) 方法处理 Raft 请求。

![](https://article-images.zsxq.com/FhqHRZ3Hmv-e25JmTyVgm3D7Whzw)

默认调用 [RaftMetadataLogCleanerManager#maybeClean](http://%20raftmetadatalogcleanermanager/#maybeClean) 方法来清理历史数据。

private static class RaftMetadataLogCleanerManager {

private final Logger logger;

private final Timer timer;

private final long delayMs;

private final Runnable cleaner;

RaftMetadataLogCleanerManager(Logger logger, Time time, long delayMs, Runnable cleaner) {

this.logger = logger;

this.timer = time.timer(delayMs);

this.delayMs = delayMs;

this.cleaner = cleaner;

}

// 默认调用这里清理历史数据

public long maybeClean(long currentTimeMs) {

timer.update(currentTimeMs);

if (timer.isExpired()) {

try {

cleaner.run();

} catch (Throwable t) {

logger.error("Had an error during log cleaning", t);

}

timer.reset(delayMs);

}

return timer.remainingMs();

}

}

当该方法发现上次数据清理时间距离当前时间超过 [60000](http://0.0.234.96/) ms，则会调用 [KafkaMetadataLog#maybeClean](http://kafkametadatalog/#maybeClean) 方法，源码如下：

override def maybeClean(): Boolean = {

snapshots synchronized {

var didClean \= false

// 1、如果当前日志的大小加上当前所有快照的总大小仍大于 metadata.max.retention.bytes 配置项指定大小，则需要清理该快照，以及对应的 cluster 日志段文件，直到数据量满足要求为止。

didClean |= cleanSnapshotsRetentionSize()

// 2、如果快照创建时间距离当前时间超过 metadata.log.segment.ms 配置项指定时间，将删除这些快照文件，以及对应的 cluster 日志段文件。

didClean |= cleanSnapshotsRetentionMs()

didClean

}

}

private def cleanSnapshotsRetentionSize(): Boolean = {

// 检查是否设置了快照保留的最大字节数，如果没有设置则返回 false

if (config.retentionMaxBytes < 0)

return false

// 加载快照的大小信息，并转化为 Map

val snapshotSizes \= loadSnapshotSizes().toMap

// 计算当前所有快照的总大小

var snapshotTotalSize: Long = snapshotSizes.values.sum

// 判断是否需要清理指定的快照

def shouldClean(snapshotId: OffsetAndEpoch): Boolean = {

snapshotSizes.get(snapshotId).exists { snapshotSize =>

// 如果当前日志的大小加上当前所有快照的总大小仍大于 metadata.max.retention.bytes 配置项指定大小，则需要清理该快照

if (log.size + snapshotTotalSize > config.retentionMaxBytes) {

snapshotTotalSize -= snapshotSize

true

} else {

false

}

}

}

// 清理满足条件的快照

cleanSnapshots(shouldClean)

}

private def cleanSnapshotsRetentionMs(): Boolean = {

// 检查是否设置了快照保留的最大时间，如果没有设置则返回 false

if (config.retentionMillis < 0)

return false

// 判断是否需要清理指定的快照

def shouldClean(snapshotId: OffsetAndEpoch): Boolean = {

val now \= time.milliseconds()

readSnapshotTimestamp(snapshotId).exists { timestamp =>

// 如果快照创建时间距离当前时间超过 metadata.log.segment.ms 配置项指定时间，则需要清理该快照

if (now - timestamp > config.retentionMillis) {

true

} else {

false

}

}

}

// 清理满足条件的快照

cleanSnapshots(shouldClean)

}

private def cleanSnapshots(predicate: (OffsetAndEpoch) => Boolean): Boolean = {

// 如果快照数量少于 2，则返回 false

if (snapshots.size < 2)

return false

var didClean \= false

// 遍历快照，从第二个快照开始

snapshots.keys.toSeq.sliding(2).foreach {

case Seq(snapshot: OffsetAndEpoch, nextSnapshot: OffsetAndEpoch) =>

// 如果满足清理条件，并且在当前快照之前的快照都成功删除，则将 didClean 置为 true

if (predicate(snapshot) && deleteBeforeSnapshot(nextSnapshot)) {

didClean = true

} else {

return didClean

}

case \_ \=\> false

}

didClean

}

可以看到该方法就是根据快照大小或者时长来清理快照，核心步骤如下：

1.  如果当前日志的大小加上当前所有快照的总大小仍大于 [metadata.max.retention.bytes](http://metadata.max.retention.bytes/) 配置项指定大小，则需要清理该快照，以及对应的 cluster 日志段文件，直到数据量满足要求为止。
2.  如果快照创建时间距离当前时间超过 [metadata.log.segment.ms](http://metadata.log.segment.ms/) 配置项指定时间，将删除这些快照文件，以及对应的 cluster 日志段文件。

## **05 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头抛出「**Kafka KRaft 模块**」的另一个重要功能：快照管理以及历史数据清理操作。

2、接着带大家从四个维度「**快照清理规则**」、「**QuorumMetaLogListener 监听器**」、「**快照数据管理**」、「**清理历史数据**」深度剖析了是如何对快照进行管理以及如何对历史数据进行清理。

下篇我们来深度剖析「**服务端整体流程总结篇**」，大家期待，我们下期见。