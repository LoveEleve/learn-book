大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端 Leader 副本更新 ISR、HW 流程**」，了解了 Leader 副本是如何处理 Follower 副本发来的 Fetch 请求的，从今天开始，我们接着来深度剖析 Kafka「**Broker**」的底层源码实现，我们接着来深度聊聊「**Kafka 服务端 LeaderEpoch 机制实现原理**」，看看 Kafka 是如何利用「**LeaderEpoch 机制**」来解决副本之间的数据一致性的。

![](https://article-images.zsxq.com/FpqmAWotoek5NulNfQ9cx_cPvsmi)

## **01 总体概述**

在 [【服务端 Broker 源码分析系列第二十六篇】图解 Kafka 源码之副本同步实现原理（上）](https://articles.zsxq.com/id_j8h0gmrk5i6d.html) 这篇中，我们提出 2 个重要概念：「**ISR**」、「**HW**」。

在 [Partition#Partition](http://partition/#Partition) 中存储了分区的 「**ISR 信息**」，而 [Partition#remoteReplicasMap](http://partition/#remoteReplicasMap) 集合中存储了分区所有的 「**Follower 副本**」信息，其中包括 「**Follower 副本**」的 LogEndOffset 信息，利用这两个属性，「**Leader 副本**」就可以计算出分区的高水位。

但是只通过这两个关键概念，在副本之间同步数据的过程中，还是会出现因截断导致部分数据丢失的问题，Kafka 为了解决这个问题，在 0.11 版本中引入了 「**LeaderEpoch 机制**」，在数据截断前，判断该数据是否有效。

接下来我们会先通过示例来剖析为什么会出现截断丢失问题，在看看「**LeaderEpoch 机制**」是如何解决该问题的。

##   
**02 副本同步流程梳理**

[【服务端 Broker 源码分析系列第二十六篇】图解 Kafka 源码之副本同步实现原理（上）](https://articles.zsxq.com/id_j8h0gmrk5i6d.html)

[【服务端 Broker 源码分析系列第二十七篇】图解 Kafka 源码之副本同步实现原理（下）](https://articles.zsxq.com/id_64qw2onjkgpx.html)

这两篇主要剖析了副本之间同步的主要流程以及源码实现，如果你还没有掌握，可以再点开进行学习。

这里总结下：

「**Leader 副本**」、「**Follower 副本**」主从同步过程中，除了需要同步分区的消息数据之外，还需要同步「**分区的 HW**」、「**LogEndOffset**」等关键属性，如下：

## **2.1 Leader 副本关键属性**

1.  LogEndOffset：它是 「**Leader 副本**」的日志结束位置，每次写入消息后更新。
2.  RemoteReplicasMap：它是「**Leader 副本**」的 [Partition#remoteReplicasMap](http://partition/#remoteReplicasMap) 会维护分区所有 「**Follower 副本**」的相关信息，包括「**Follower 副本**」的 [LogEndOffset](http://logendoffset/)、[LogStartOffset](http://logstartoffset%20/) 等属性。
3.  HW：「**Leader 副本**」的 [Log#highWatermarkMetadata](http://log/#highWatermarkMetadata) 属性存储了 Leader 副本的高水位。

## **2.2 Follower 副本关键属性**

1.  LogEndOffset：它是「**Follower 副本**」的日志结束位置，从「**Leader 副本**」同步数据后更新。
2.  HW：「**Follower 副本**」的 高水位，与「**Leader 副本**」同步。

## **2.3 主从同步示例**

比如现在某 Topic 分区有一个 「**Leader 副本**」、一个「**Follower 副本**」，当前 「**Leader 副本**」、「**Follower 副本**」分区信息如下：

1.  Leader-LogEndOffset：3。
2.  Leader-RemoteReplicasMap：follow:3 表示 「**Follower 副本**」的 LogEndOffset 为 3，由于 「**Leader 副本**」利用该集合 「**Follower 副本**」的 LogEndOffset 来计算 HW，这里我们只关注 「**Follower 副本**」的 LogEndOffset 属性值。
3.  Leader-HW：3。
4.  Follower-LogEndOffset：3。
5.  Follower-HW：3。

此时 「**Leader 副本**」写入新的消息 M3，offset = 3，当消息写入成功后，Leader-LogEndOffset = 4，如下图：

![](https://article-images.zsxq.com/Fu_zm6tzJZ6_Uz8jO97VankzQOrD)

从上图来看数据同步似乎很完美，依托于 HW 和 LEO ，Kafka 既完美实现了消息的对外可见性，又实现了异步的副本同步机制。但是从上图得出，**Follower 副本的 HW 更新是需要一轮额外的拉取请求才能实现**。如果把上图中例子扩展到更多个 Follower 副本，也许需要更多轮拉取请求。也就是说，Leader 副本 HW 更新和 Follower 副本 HW 更新在时间上是存在错配的。**这种错配是很多 “数据丢失”或者 “数据不一致”问题的根源**。因此社区在 0.11 版本正式引入了「**LeaderEpoch 机制**」，来规避因 HW 更新错配导致的各种不一致问题。

##   
**2.4 无 Leader Epoch 机制导致的数据丢失问题**

接下来,我们先看一个无 Leader Epoch 机制造成数据丢失的场景图, 如下:

  
![](https://article-images.zsxq.com/FiF2KEUwgdDSrtYh03Iy8qiEpxyH)

正如上图描述的，单纯依靠 HW 是怎么造成数据丢失的。一开始时，副本 A 和 副本  B 都处于正常状态，A 是 Leader 副本。现在我们假设 Leader 副本和 Follower 副本都写入了这4条消息，而且 Leader 副本的 HW 已经更新了，但 Follower 副本的 HW 还未更新(这种情况是可能能会发生的)。上面说过，Follower 的 HW 更新与 Leader 的 HW 是存在时间错配的。**如果此时副本 B 所在的 Broker 宕机了，当他重启回来后，副本 B 会执行日志截断操作，将 LEO 值调整为之前的 HW 值，也就是 3。**即位移值为 3 的那条消息被副本 B 从磁盘中删除，此时副本 B 的底层磁盘文件中只保存有 3 条消息，即位移值为 \[0,1,2\] 对应的消息。

当执行完截断操作后，副本 B 开始从副本 A 拉取消息，执行正常的消息同步。**如果此时副本 A 所在的 Broker 宕机了，那么 Kafka 就很无奈，只能让副本 B 成为新的 Leader**，此时当 副本 A 回来后，需要执行相同的日志截断操作，即将 HW 调整为与 副本 B 相同的值，也就是 3。

这样操作之后，位移值为 3 的那条消息就从这两个副本中被永远地抹掉了。这就是上图要展示的数据丢失场景。

##   
**03 Leader Epoch 机制**

## **3.1 Leader Epoch 是什么**

所谓Leader Epoch，我们大致可以认为是Leader版本，它实际上是一个键值对集合，键值对内容为[<epoch,offset>](http://epoch,offset/)。它由两部分数据组成：

1.  Epoch：它表示 Leader 副本的版本，是一个单调增加的版本号，从 0 开始，每当副本领导权发生变更时，都会增加该版本号。小版本号的 Leader 被认为是过期Leader，不能再行使 Leader 权力。
2.  Offset：Leader 副本在该 Epoch 值上写入的第一条消息的位移。

![](https://article-images.zsxq.com/luI8n-nTJr8JZ4LB3gCMdw6Pw2_l)

如上来说明一下 Leader Epoch。假设现在有五个 Leader Epoch <0, 0>、<1, 120>、<2, 329>、<3, 596>、<4, 748>，那么，第一个 Leader Epoch 表示版本号是 0，这个版本的 Leader 副本从位移 0 开始保存消息，一共保存了 120 条消息。之后当 Leader 副本发生了变更，版本号增加到 1，新版本的起始位移是 120。

##   
**3.2 Leader Epoch 如何解决上面丢失问题**

Kafka Broker 会在**内存中为每个分区都缓存 Leader Epoch 数据**，同时它还会定期地将这些信息**持久化到一个 checkpoint 文件中**。当 Leader 副本写入消息到磁盘时，Broker 会尝试更新这部分缓存。如果该 Leader 是首次写入消息，那么 Broker 会向缓存中增加一个 Leader Epoch 条目，否则就不做更新。这样，每次有 Leader 变更时，新的 Leader 副本会查询这部分缓存，取出对应的 Leader Epoch 的起始位移，以避免数据丢失和不一致的情况。

严格来说，上面丢失数据场景发生的前提必须是 **Broker 端参数** [min.insync.replicas](http://min.insync.replicas/) **设置为 1**。此时一旦消息被写入到 Leader 副本的磁盘，就会被认为是「**已提交状态**」，但因存在「**时间错配**」问题导致 Follower 的 HW 更新是有滞后的。如果在这个短暂的滞后时间内，接连发生 Broker 宕机，那么这类数据的丢失就是无法避免的。

接下来, 我们来看下如何利用 Leader Epoch 机制来规避这种数据丢失。如下图所示:

  
![](https://article-images.zsxq.com/FpFMHtlHR1VdxlwyFcJq8v4ibzlv)

与上面场景类似，只不过当引入「**LeaderEpoch 机制**」后，Follower 副本 B 重启回来后，需要向副本 A 发送一个特殊的请求去获取 Leader 副本的 LEO 值。在这个例子中，该值为 4。当获知到 Leader LEO = 4 后，副本 B 发现该 LEO 值不比它自己的 LEO 值小，而且缓存中也没有保存任何起始位移值 > 4 的 Epoch 条目，因此副本 B 无需执行任何日志截断操作。这是对「**HW 机制**」的一个明显改进，即**副本是否执行日志截断操作不再依赖高水位值来进行判断**。

现在，副本A 宕机了，副本 B 成为 Leader 副本。同样当副本 A 重启回来后，执行与副本 B 相同的逻辑判断，发现自己也不用执行日志截断，至此位移值为 3 的那条消息在两个副本中均得到保留。后面再向副本 B 写入新消息时，副本 B 所在的 Broker 缓存中，会生成新的 Leader Epoch 条目：[\[Epoch=1, Offset=4\]](http://%5BEpoch=1,%20Offset=4%5D)。之后，副本 B 会使用这个条目来帮助判断后续是否执行日志截断操作。

这样，通过「**LeaderEpoch 机制**」，Kafka 完美地规避了上面场景中数据丢失的问题。

## **3.3 Leader Epoch 源码总览**

首先，leaderEpochCache 是一个 LeaderEpochFileCache 对象实例，它负责存储「**LeaderEpoch**」，它将 「**LeaderEpoch**」信息存储在下面介质中：

1.  [LeaderEpochFileCache#epochs](http://leaderepochfilecache/#epochs) 是一个 Map，用来缓存分区所有的 LeaderEpoch。
2.  LeaderEpochFileCache 可以将「**LeaderEpoch**」信息进行持久化，存储在分区目录下的 [leader-epoch-checkpoint](http://leader-epoch-checkpoint/) 文件中。

「**LeaderEpochFileCache**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/epoch/LeaderEpochFileCache.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/AbstractFetcherThread.scala)

![](https://article-images.zsxq.com/FpBquwHYF5z1jPCtpeeyxX1w1aJz)

## **3.4 加载 Leader Epoch 流程**

当 Broker 启动时，会在 LogManager#loadLog 方法里面去实例化 Log 对象。

![](https://article-images.zsxq.com/Fu28Z2wrSVWVaoc1Vq-zaR8OZlMy)

当「**Log**」对象初始化时会先「**创建目录**」、「**初始化 LeaderEpochCache**」、「**加载日志段**」获取下一个消息的起始位置、「**更新日志起始位置 LogStartOffset**」、「**加载 LeaderEpochCache**」等等一系列操作。

  
![](https://article-images.zsxq.com/FkTZYuwMipfUpw4YMN-u-kb-lS5a)

在 Kafka 2.8 版本中的实现如下：

// 从 leader-epoch-checkpoint 文件中加载 LeaderEpoch 信息并存储到 LeaderEpochFileCache#epochs 中。

private def initializeLeaderEpochCache(): Unit = lock synchronized {

// 创建用于存储 Leader Epoch 的检查点文件

val leaderEpochFile \= LeaderEpochCheckpointFile.newFile(dir)

// 创建新的 Leader Epoch 文件缓存

def newLeaderEpochFileCache(): LeaderEpochFileCache = {

val checkpointFile \= new LeaderEpochCheckpointFile(leaderEpochFile, logDirFailureChannel)

new LeaderEpochFileCache(topicPartition, () => logEndOffset, checkpointFile)

}

// 如果记录版本在 V2 之前，则删除旧的 Leader Epoch 缓存，并删除检查点文件

if (recordVersion.precedes(RecordVersion.V2)) {

val currentCache \= if (leaderEpochFile.exists())

Some(newLeaderEpochFileCache())

else

None

// 如果当前缓存非空，则警告删除非空的 Leader Epoch 缓存，因为与不兼容的消息格式(recordVersion)有关

if (currentCache.exists(\_.nonEmpty))

warn(s"Deleting non-empty leader epoch cache due to incompatible message format $recordVersion")

Files.deleteIfExists(leaderEpochFile.toPath)

leaderEpochCache = None

} else {

// 创建新的 Leader Epoch 缓存

leaderEpochCache = Some(newLeaderEpochFileCache())

}

}

在 Kafka 3.x 版本中的实现如下：

// 在 object Log#apply 方法进行调用

private def initializeLeaderEpochCache(): Unit = lock synchronized {

// 从 leader-epoch-checkpoint 文件中加载 LeaderEpoch 信息并存储到 LeaderEpochFileCache#epochs 中。

leaderEpochCache = Log.maybeCreateLeaderEpochCache(dir, topicPartition, logDirFailureChannel, recordVersion, logIdent)

}

def maybeCreateLeaderEpochCache(dir: File,

topicPartition: TopicPartition,

logDirFailureChannel: LogDirFailureChannel,

recordVersion: RecordVersion,

logPrefix: String): Option\[LeaderEpochFileCache\] = {

// 创建用于存储 Leader Epoch 的检查点文件

val leaderEpochFile \= LeaderEpochCheckpointFile.newFile(dir)

// 创建新的 Leader Epoch 文件缓存

def newLeaderEpochFileCache(): LeaderEpochFileCache = {

val checkpointFile \= new LeaderEpochCheckpointFile(leaderEpochFile, logDirFailureChannel)

new LeaderEpochFileCache(topicPartition, checkpointFile)

}

// 如果记录版本在 V2 之前，则删除旧的 Leader Epoch 缓存，并删除检查点文件

if (recordVersion.precedes(RecordVersion.V2)) {

val currentCache \= if (leaderEpochFile.exists())

Some(newLeaderEpochFileCache())

else

None

// 如果当前缓存非空，则警告删除非空的 Leader Epoch 缓存，因为与不兼容的消息格式(recordVersion)有关

if (currentCache.exists(\_.nonEmpty))

warn(s"${logPrefix}Deleting non-empty leader epoch cache due to incompatible message format $recordVersion")

Files.deleteIfExists(leaderEpochFile.toPath)

None

} else {

// 创建新的 Leader Epoch 缓存

Some(newLeaderEpochFileCache())

}

}

从上面源码可以看出，都是从 [leader-epoch-checkpoint](http://leader-epoch-checkpoint%20/) 文件中加载 LeaderEpoch 信息并存储到 [LeaderEpochFileCache#epochs](http://leaderepochfilecache/#epochs) 中。

## **3.5 更新与存储 Leader Epoch 流程**

当 KafkaController 在为分区进行选举 Leader 副本时会执行以下操作：

1.  首先将 Leader 副本的 epoch + 1。
2.  将 epoch 存储到 Zookeeper 中，ZK 节点的路径为 /brokers/topics/{topic}/partitions/{partition}/{partition}/state。
3.  通过 LeaderAndIsr 请求将新的 LeaderEpoch 发送给 Broker。
4.  当 Broker 收到 LeaderAndIsr 请求后，会调用 Log#maybeAssignEpochsStartOffset 方法来记录最小的 LeaderEpoch 信息，并进行持久化存储。

## **3.6 借助 Leader Epoch 完成数据截断**

接下来我们来剖析下 Kafka 是如何利用「**LeaderEpoch 机制**」来判断 「**Follower 副本**」是否需要执行截断操作。

1.  首先「**Follower 副本**」、「**Leader 副本**」都存储了分区的 LeaderEpoch 信息。
2.  「**Follower 副本**」发送的 Fetch 请求中携带了 「**Follower 副本**」最新的 LeaderEpoch 信息， 「**Leader 副本**」会根据该 LeaderEpoch 来判断 「**Follower 副本**」是否需要截断，并将判断结果返回给 「**Follower 副本**」，「**Follower 副本**」再根据 「**Leader 副本**」返回的响应来执行对应的操作。

在 [【服务端 Broker 源码分析系列第九篇】图解 Kafka 源码之 ReplicaManager 日志读写流程](https://articles.zsxq.com/id_5dq174gc0wji.html) 这篇中，「**Leader 副本**」最终会调用 Partition#readRecords 方法来处理 Fetch 请求。

这是「**Partition**」组件类的相关方法，「**Partition**」组件是 topic 在某个 broker 上一个副本的抽象。**每个 Parititon 对象都会维护一个 Replica 对象，而 Replica 对象中又会维护 Log 对象，也就是日志数据目录的抽象**。

「**Partition**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/cluster/Partition.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/cluster/Partition.scala)

def readRecords(lastFetchedEpoch: Optional\[Integer\], // 上一次获取的 epoch

fetchOffset: Long, // 获取记录时的偏移量

currentLeaderEpoch: Optional\[Integer\], // 当前 leader epoch

maxBytes: Int, // 最大获取字节数

fetchIsolation: FetchIsolation, // 消息隔离级别（读取未提交消息或全部消息）

fetchOnlyFromLeader: Boolean, // 是否只从 leader 读取

minOneMessage: Boolean) // 是否至少获取一条消息

: LogReadInfo = inReadLock(leaderIsrUpdateLock) { // 这里通过读锁来读取

....

// 1、如果 Follower 副本发送的 Fetch 请求中包含 lastFetchedEpoch，则表示 Leader 副本需要判断该 Follower 副本是否需要执行截断操作，其中 lastFetchedEpoch 参数是 Follower 副本中最新的 LeaderEpoch 的 epoch。

lastFetchedEpoch.ifPresent { fetchEpoch =>

// 2、调用 lastOffsetForLeaderEpoch 获取 lastFetchedEpoch 参数在 Leader 副本中对应的 LeaderEpoch，以及该 LeaderEpoch 结束偏移量（该 LeaderEpoch 的最后一条消息偏移量）。如果 Leader 副本中 lastFetchedEpoch 参数对应的 LeaderEpoch 不存在，则返回 Leader 副本最新的 LeaderEpoch 及其结束偏移量。

val epochEndOffset \= lastOffsetForLeaderEpoch(currentLeaderEpoch, fetchEpoch, fetchOnlyFromLeader = false)

....

// 3、如果 fetchEpoch、fetchOffset 大于 leader-epochEndOffset、leader-epochEndOffset，则表示 Follower 副本中存在 Leader 副本中不存在的数据，则需要执行截断操作。此时 Leader 副本不会读取数据，而是将 leader-epochEndOffset、leader-epochEndOffset 作为 divergingEpoch 属性返回给 Follower 副本,Follower 副本收到这些属性后，便会执行截断操作。

if (epochEndOffset.leaderEpoch < fetchEpoch || epochEndOffset.endOffset < fetchOffset) {

// 定义空记录数据结构

val emptyFetchData \= FetchDataInfo(

fetchOffsetMetadata = LogOffsetMetadata(fetchOffset),

records = MemoryRecords.EMPTY,

firstEntryIncomplete = false,

abortedTransactions = None

)

....

// 返回 LogReadInfo，包含空记录和分界点信息等

return LogReadInfo(

fetchedData = emptyFetchData,

divergingEpoch = Some(divergingEpoch),

highWatermark = initialHighWatermark,

logStartOffset = initialLogStartOffset,

logEndOffset = initialLogEndOffset,

lastStableOffset = initialLastStableOffset)

}

}

....

}

步骤如下：

1.  如果 Follower 副本发送的 Fetch 请求中包含 lastFetchedEpoch，则表示 Leader 副本需要判断该 Follower 副本是否需要执行截断操作，其中 lastFetchedEpoch 参数是 Follower 副本中最新的 LeaderEpoch 的 epoch。
2.  调用 lastOffsetForLeaderEpoch 获取 lastFetchedEpoch 参数在 Leader 副本中对应的 LeaderEpoch，以及该 LeaderEpoch 结束偏移量（该 LeaderEpoch 的最后一条消息偏移量）。如果 Leader 副本中 lastFetchedEpoch 参数对应的 LeaderEpoch 不存在，则返回 Leader 副本最新的 LeaderEpoch 及其结束偏移量。
3.  如果 fetchEpoch、fetchOffset 大于 leader-epochEndOffset、leader-epochEndOffset，则表示 Follower 副本中存在 Leader 副本中不存在的数据，则需要执行截断操作。此时 Leader 副本不会读取数据，而是将 leader-epochEndOffset、leader-epochEndOffset 作为 divergingEpoch 属性返回给 Follower 副本,Follower 副本收到这些属性后，便会执行截断操作。

## **04 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头从「**Kafka 副本同步**」中引出的一致性重要概念「**ISR**」、「**HW**」，在副本之间同步数据的过程中，还是会出现因截断导致部分数据丢失的问题，Kafka 为了解决这个问题，在 0.11 版本中引入了 「**LeaderEpoch 机制**」，在数据截断前，判断该数据是否有效。

2、接着带大家深度剖析了「**副本同步流程**」以及 「**无 LeaderEpoch 机制导致的数据问题**」。

3、接着带大家深度剖析了「**LeaderEpoch 机制**」是什么，以及它是如何解决上面的数据丢失问题的。

4、最后带大家深度剖析了「**LeaderEpoch 机制**」的源码实现，是如何加载、更新、存储、以及完成截断操作的。

下篇我们来深度剖析「**延迟机制、 时间轮实现原理**」，大家期待，我们下期见。