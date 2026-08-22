大家好，我是 **华仔**, 又跟大家见面了。

上一篇中，主要带大家深度剖析了 「**Kafka 服务端源码 LogSegment 日志段架构设计**」，通过「**场景驱动方式**」，现在消息被封装成批次请求已经从「**生产者**」发送到「**Broker**」，且被「**网络层**」所接收到并准备进行消息数据存储，从今天开始，我们来深度剖析 Kafka 日志系统的底层实现，这是日志系列第五篇，我们先来深度聊聊「**Kafka 服务端源码之日志 Log 架构设计**」，看看 Kafka 服务端是如何真正存储消息数据的。

![](https://article-images.zsxq.com/Fhs8oPX29v76OPFhpztNmfXMSRPh)

## **01 总体概述**

在上一篇中，我们 [【服务端 Broker 源码分析系列第十一篇】图解 Kafka 源码之日志段 LogSegment 架构设计](https://articles.zsxq.com/id_t4f0eut7oe94.html) 引出了服务端日志的管理对象之 「**Log**」。

在我看来，「**Log**」对象是 Kafka Broker 端源码最重要核心的部分，没有之一。所以我们一定要掌握这部分源码的实现，对解决线上问题很有帮助。

接下来我们来对 「**Log**」对象一探究竟，从名字上看就知道它是「**日志管理组件**」，**日志是日志段的管理容器**，内部定义了很多关于日志段的管理操作。每个日志段文件以分段文件内「**第一个消息的偏移量**」为基础偏移量「**baseOffset**」，并以此作为日志段文件命名规则。

好了，下面开始剖析其源码实现，让你对整个日志管理有个总体的认知，深度剖析下其内部是如何进行日志管理的，整个流程是怎么样的？那么带着这些问题进入今天的正题。

整个日志管理的相关组件的调用关系图如下：

![](https://article-images.zsxq.com/FgVmpr3JOm-_1PVfCHJC94zu4_tC)

## **02 Log 源码总览**

「**Log**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/Log](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogSegment.scala)[.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/Log.scala)，所有跟日志相关的源码都在 kafka/log 这个包下面。

![](https://article-images.zsxq.com/FtxQxY9_dOpm1ustlXpuQb3WBW2e)

「**Log**」类源码非常长，从上图看大概2700多行。这在整个 Kafka 源码中非常少见的，**足以说明其重要性**。下面通过一张图来汇总下。

![](https://article-images.zsxq.com/Fj2Xs87Roefh-c3P7zFpDgen-rFz)

今天我们主要来看下「**Object Log**」和 「**Class Log**」。

##   
**03 Object Log 源码**

先来看下「**Object Log**」的源码，其用来保存静态变量和静态方法。

/\*\*

\* Helper functions for logs

\*/

object Log {

/\*\* a log file \*/

val LogFileSuffix \= ".log"

/\*\* an index file \*/

val IndexFileSuffix \= ".index"

/\*\* a time index file \*/

val TimeIndexFileSuffix \= ".timeindex"

val ProducerSnapshotFileSuffix \= ".snapshot"

/\*\* an (aborted) txn index \*/

val TxnIndexFileSuffix \= ".txnindex"

/\*\* a file that is scheduled to be deleted \*/

val DeletedFileSuffix \= ".deleted"

/\*\* A temporary file that is being used for log cleaning \*/

val CleanedFileSuffix \= ".cleaned"

/\*\* A temporary file used when swapping files into the log \*/

val SwapFileSuffix \= ".swap"

/\*\* Clean shutdown file that indicates the broker was cleanly shutdown in 0.8 and higher.

\* This is used to avoid unnecessary recovery after a clean shutdown. In theory this could be

\* avoided by passing in the recovery point, however finding the correct position to do this

\* requires accessing the offset index which may not be safe in an unclean shutdown.

\* For more information see the discussion in PR#2104

\*/

val CleanShutdownFile \= ".kafka\_cleanshutdown"

/\*\* a directory that is scheduled to be deleted \*/

val DeleteDirSuffix \= "-delete"

/\*\* a directory that is used for future partition \*/

val FutureDirSuffix \= "-future"

private\[log\] val DeleteDirPattern \= Pattern.compile(s"^(\\\\S+)-(\\\\S+)\\\\.(\\\\S+)$DeleteDirSuffix")

private\[log\] val FutureDirPattern \= Pattern.compile(s"^(\\\\S+)-(\\\\S+)\\\\.(\\\\S+)$FutureDirSuffix")

val UnknownOffset \= -1L

....

}

上面源码中定义了好多常量，主要是「**日志文件类型**」。下面挨个解释下：

1.  **.log**: Log 的目录，用于存储日志文件。
2.  **.index**: Log 的目录，用于存储索引文件。
3.  **.timeindex**: Log 的目录，用于存储时间索引文件。
4.  **.snapshot**: Kafka 为幂等型或事务型 Producer 所做的快照文件。
5.  **.txnindex**: Kafka 为事务消息所做的已终止事务索引文件。
6.  **.deleted**: 删除日志段操作创建的文件。目前删除日志段文件是**异步任务操作**，Broker 端把日志段文件从 .log 后缀修改为 .deleted 后缀。
7.  **.cleaned**: Compaction 操作的产物，后面会讲。
8.  **.swap**: Compaction 操作的产物，后面会讲。
9.  **.kafka\_cleanshutdown**: Clean shutdown文件，存在表示 kafka 正在做清理性的停机工作。
10.  **\-delete**: 主要用在文件夹中的。当你删除一个主题的时候，主题对应分区的文件夹会被加上该后缀。
11.  **\-feture**: 主要用在变更主题分区文件夹地址的。

接下来看下其中的方法，相对都比较简单，这里列一下：

// 用于创建一个 Log 实例

def apply(dir: File, // Log 的目录，用于存储日志文件。

config: LogConfig, // Log 的配置信息。

logStartOffset: Long, // 该 Log 起始的 offset 值。

recoveryPoint: Long, // 该 Log 的恢复点，表示之前的数据可以丢弃。

scheduler: Scheduler, // 用于调度一些异步任务的 scheduler。

brokerTopicStats: BrokerTopicStats, // 记录 kafka Broker 级别的一些统计信息。

time: Time = Time.SYSTEM, // kafka 时间类库，用于获取当前时间等操作。

maxProducerIdExpirationMs: Int, // 设置 Producer ID 的最大过期时间。

producerIdExpirationCheckIntervalMs: Int, // 设置检查 Producer ID 过期的时间间隔。

logDirFailureChannel: LogDirFailureChannel): Log = { // Kafka 监测 Log 目录故障的通道。

// 解析出 Log 存储的 topic 和 partition。

val topicPartition \= Log.parseTopicPartitionName(dir)

// 创建 ProducerStateManager 实例，用于维护 producerId 和 producerEpoch 在消息可靠性保证方面的作用。

val producerStateManager \= new ProducerStateManager(topicPartition, dir, maxProducerIdExpirationMs)

// 使用给定的参数创建一个 Log 对象，该对象代表了一个 Log 目录。

new Log(dir, config, logStartOffset, recoveryPoint, scheduler, brokerTopicStats, time, maxProducerIdExpirationMs,

producerIdExpirationCheckIntervalMs, topicPartition, producerStateManager, logDirFailureChannel)

}

/\*\*

\* 通过给定的位移值计算出对应的日志段文件名。这仅仅是用 0 填充偏移量数字，以使得 ls 命令按数值顺序排列。

\* @param offset 文件名中要包含的偏移量

\* @return 文件名

\*/

def filenamePrefixFromOffset(offset: Long): String = {

val nf \= NumberFormat.getInstance()

// Kafka 日志文件固定是 20 位的长度

nf.setMinimumIntegerDigits(20)

// 设置用前面补 0 的方式把给定位移值扩充成一个固定 20 位长度的字符串。

nf.setMaximumFractionDigits(0)

nf.setGroupingUsed(false)

nf.format(offset)

}

/\*\*

\* 用给定的偏移量、指定的后缀构建在指定目录中的日志文件名

\* @param dir 日志文件所在的目录

\* @param offset 文件名中的起始偏移量

\* @param suffix 要附加的后缀名称（例如“”，“.deleted”，“.cleaned”，“.swap”等）

\*/

def logFile(dir: File, offset: Long, suffix: String = ""): File =

new File(dir, filenamePrefixFromOffset(offset) + LogFileSuffix + suffix)

/\*\*

\* 返回一个目录名称，用于重命名日志目录以进行异步删除。

\* 名称将采用以下格式："topic-partitionId.uniqueId-delete"。

\* 如果主题名称太长，则将其截断以防止总名称超过 255 个字符。

\*/

def logDeleteDirName(topicPartition: TopicPartition): String = {

val uniqueId \= java.util.UUID.randomUUID.toString.replaceAll("-", "")

val suffix \= s"-${topicPartition.partition()}.${uniqueId}${DeleteDirSuffix}"

val prefixLength \= Math.min(topicPartition.topic().size, 255 - suffix.size)

s"${topicPartition.topic().substring(0, prefixLength)}${suffix}"

}

/\*\*

\* 返回为给定主题分区的未来目录名称。

\* 名称将采用以下格式：topic-partition.uniqueId-future，其中 topic、partition 和 uniqueId 是变量。

\*/

def logFutureDirName(topicPartition: TopicPartition): String = {

logDirNameWithSuffix(topicPartition, FutureDirSuffix)

}

private def logDirNameWithSuffix(topicPartition: TopicPartition, suffix: String): String = {

val uniqueId \= java.util.UUID.randomUUID.toString.replaceAll("-", "")

s"${logDirName(topicPartition)}.$uniqueId$suffix"

}

/\*\*

\* 返回给定主题分区的目录名称。名称将采用以下格式："topic-partition"，其中 topic 和 partition 是变量。

\*/

def logDirName(topicPartition: TopicPartition): String = {

s"${topicPartition.topic}-${topicPartition.partition}"

}

/\*\*

\* 在给定的目录中使用给定的起始偏移量和给定的后缀名称构建索引文件名

\* @param dir 日志所在的目录

\* @param offset 文件名中的起始偏移量

\* @param suffix 要附加的后缀名称（“”、“.deleted”、“.cleaned”、“.swap”等）

\*/

def offsetIndexFile(dir: File, offset: Long, suffix: String = ""): File =

new File(dir, filenamePrefixFromOffset(offset) + IndexFileSuffix + suffix)

/\*\*

\* 在给定的目录中使用给定的起始偏移量和给定的后缀名称构建时间索引文件名

\* @param dir 日志所在的目录

\* @param offset 文件名中的起始偏移量

\* @param suffix 要附加的后缀名称（“”、“.deleted”、“.cleaned”、“.swap”等）

\*/

def timeIndexFile(dir: File, offset: Long, suffix: String = ""): File =

new File(dir, filenamePrefixFromOffset(offset) + TimeIndexFileSuffix + suffix)

/\*\*

\* 删除文件（如果存在）并附加后缀。suffix 默认为 ""，也可指定其他后缀。

\* @param file 要删除的文件

\* @param suffix 要添加的后缀（默认为 ""）

\*/

def deleteFileIfExists(file: File, suffix: String = ""): Unit =

Files.deleteIfExists(new File(file.getPath + suffix).toPath)

/\*\*

\* 使用给定的偏移量构建生产者 ID 快照文件名称。

\* @param dir 日志所在的目录

\* @param offset 快照中包含的最后一个偏移量（独占）。

\*/

def producerSnapshotFile(dir: File, offset: Long): File =

new File(dir, filenamePrefixFromOffset(offset) + ProducerSnapshotFileSuffix)

/\*\*

\* 在给定的目录中使用给定的起始偏移量和给定的后缀名称构建事务索引文件名

\* @param dir 日志所在的目录

\* @param offset 文件名中的起始偏移量

\* @param suffix 要附加的后缀名称（“”、“.deleted”、“.cleaned”、“.swap”等）

\*/

def transactionIndexFile(dir: File, offset: Long, suffix: String = ""): File =

new File(dir, filenamePrefixFromOffset(offset) + TxnIndexFileSuffix + suffix)

// 从文件名中获取位移信息

def offsetFromFileName(filename: String): Long = { // filename 表示待解析的日志文件名

// 该方法首先使用 indexOf 方法查找文件名中第一个点号的位置，然后使用 substring 方法获取点号之前的字符串部分，即 offset 的值，最后使用 toLong 方法将其转换为 Long 类型并返回。例如，对于文件名 "00000000000000000000.log"，该方法将返回 Long 类型的 0。

filename.substring(0, filename.indexOf('.')).toLong

}

// 从文件中获取位移信息

def offsetFromFile(file: File): Long = { // file 表示待解析的日志文件

// 该方法首先调用 file.getName 方法获取文件名，然后调用 offsetFromFileName 方法解析文件名，并返回对应的 offset 值。

offsetFromFileName(file.getName)

}

/\*\*

\* 计算一个日志的大小（以字节为单位），基于其日志段。

\*

\* @param segments 要计算大小的日志段。

\* @return 日志段大小（以字节为单位）的总和。

\*/

def sizeInBytes(segments: Iterable\[LogSegment\]): Long =

segments.map(\_.size.toLong).sum

/\*\*

\* 在日志目录名称中解析出主题和分区。

\*/

def parseTopicPartitionName(dir: File): TopicPartition = {

if (dir == null)

throw new KafkaException("dir should not be null")

// 异常处理

def exception(dir: File): KafkaException = {

new KafkaException(s"Found directory ${dir.getCanonicalPath}, '${dir.getName}' is not in the form of " +

"topic-partition or topic-partition.uniqueId-delete (if marked for deletion).\\n" +

"Kafka's log directories (and children) should only contain Kafka topic data.")

}

// 目录名

val dirName \= dir.getName

if (dirName == null || dirName.isEmpty || !dirName.contains('-'))

throw exception(dir)

// 匹配判断,不符合抛异常

if (dirName.endsWith(DeleteDirSuffix) && !DeleteDirPattern.matcher(dirName).matches ||

dirName.endsWith(FutureDirSuffix) && !FutureDirPattern.matcher(dirName).matches)

throw exception(dir)

// 获取目录名

val name: String =

if (dirName.endsWith(DeleteDirSuffix) || dirName.endsWith(FutureDirSuffix)) dirName.substring(0, dirName.lastIndexOf('.'))

else dirName

val index \= name.lastIndexOf('-')

val topic \= name.substring(0, index)

val partitionString \= name.substring(index + 1)

if (topic.isEmpty || partitionString.isEmpty)

throw exception(dir)

val partition \=

try partitionString.toInt

catch { case \_: NumberFormatException => throw exception(dir) }

new TopicPartition(topic, partition)

}

private def isIndexFile(file: File): Boolean = {

val filename \= file.getName

filename.endsWith(IndexFileSuffix) || filename.endsWith(TimeIndexFileSuffix) || filename.endsWith(TxnIndexFileSuffix)

}

private def isLogFile(file: File): Boolean =

file.getPath.endsWith(LogFileSuffix)

上面这段代码主要定义了一些文件名解析或辅助处理的函数。这些函数可用于一些日志相关操作，例如：「**生成日志文件名称**」、「**中断操作时异步重命名日志目录**」、「**删除日志文件**」、「**计算日志大小**」、「**从日志目录名中解析主题和分区**」等，大家自行学习下，如果有不理解的可以沟通。

接下来，我们来看看本篇的**重头戏**重头戏：「**Log**」类源码。

##   
**04 Class Log 源码**

先来看下「**Class Log**」的定义。

@threadsafe

class Log(@volatile private var \_dir: File, // 主题分区所在的磁盘文件夹目录。

@volatile var config: LogConfig, // Log 的配置信息

@volatile var logStartOffset: Long, // 日志的当前最早位移

@volatile var recoveryPoint: Long, // 该 Log 的恢复点，表示之前的数据可以丢弃。

scheduler: Scheduler, // 用于调度一些异步任务的 scheduler。

brokerTopicStats: BrokerTopicStats, // 记录 kafka Broker 级别的一些统计信息。

val time: Time, // kafka 时间类库，用于获取当前时间等操作。

val maxProducerIdExpirationMs: Int, // 设置 Producer ID 的最大过期时间。

val producerIdExpirationCheckIntervalMs: Int,// 设置检查 Producer ID 过期的时间间隔。

val topicPartition: TopicPartition, // 主题分区

val producerStateManager: ProducerStateManager, // 生产者状态管理器

logDirFailureChannel: LogDirFailureChannel) extends Logging with KafkaMetricsGroup { // Kafka 监测 Log 目录故障的通道。

import kafka.log.Log.\_

this.logIdent = s"\[Log partition=$topicPartition, dir=${dir.getParent}\] "

/\* A lock that guards all modifications to the log \*/

private val lock \= new Object

// The memory mapped buffer for index files of this log will be closed with either delete() or closeHandlers()

// After memory mapped buffer is closed, no disk IO operation should be performed for this log

@volatile private var isMemoryMappedBufferClosed \= false

// Cache value of parent directory to avoid allocations in hot paths like ReplicaManager.checkpointHighWatermarks

@volatile private var \_parentDir: String = dir.getParent

/\* last time it was flushed \*/

private val lastFlushedTime \= new AtomicLong(time.milliseconds)

@volatile private var nextOffsetMetadata: LogOffsetMetadata = \_

/\* The earliest offset which is part of an incomplete transaction. This is used to compute the

\* last stable offset (LSO) in ReplicaManager. Note that it is possible that the "true" first unstable offset

\* gets removed from the log (through record or segment deletion). In this case, the first unstable offset

\* will point to the log start offset, which may actually be either part of a completed transaction or not

\* part of a transaction at all. However, since we only use the LSO for the purpose of restricting the

\* read\_committed consumer to fetching decided data (i.e. committed, aborted, or non-transactional), this

\* temporary abuse seems justifiable and saves us from scanning the log after deletion to find the first offsets

\* of each ongoing transaction in order to compute a new first unstable offset. It is possible, however,

\* that this could result in disagreement between replicas depending on when they began replicating the log.

\* In the worst case, the LSO could be seen by a consumer to go backwards.

\*/

@volatile private var firstUnstableOffsetMetadata: Option\[LogOffsetMetadata\] = None

/\* Keep track of the current high watermark in order to ensure that segments containing offsets at or above it are

\* not eligible for deletion. This means that the active segment is only eligible for deletion if the high watermark

\* equals the log end offset (which may never happen for a partition under consistent load). This is needed to

\* prevent the log start offset (which is exposed in fetch responses) from getting ahead of the high watermark.

\*/

@volatile private var highWatermarkMetadata: LogOffsetMetadata = LogOffsetMetadata(logStartOffset)

/\* the actual segments of the log \*/

private val segments: ConcurrentNavigableMap\[java.lang.Long, LogSegment\] = new ConcurrentSkipListMap\[java.lang.Long, LogSegment\]

// Visible for testing

@volatile var leaderEpochCache: Option\[LeaderEpochFileCache\] = None

....

}

我们来梳理下其重要字段：

1.  **\_dri**：主题分区所在的磁盘文件夹目录。
2.  **logStartOffset**：它是日志当前**对外可见**的最早位移。当前最早位移的作用是：
3.  为了删除文件，baseOffset 小于 logStartOffset 的日志分段应该删除。
4.  返回客户端最早的偏移量，避免了高水位的位移比日志当前最早位移还早。
5.  **recoveryPoint**：该 Log 的恢复点，表示之前的数据可以丢弃。
6.  **lock**：当有多个 handler 线程并发写入一个 Log 去追加消息时，需要锁确保安全。
7.  **isMemoryMappedBufferClosed**：索引文件的内存映射缓冲区是否关闭了。
8.  以下两种情况会关闭：
9.  1）当映射的文件删除了。这时就不需要文件的映射了，也就避免浪费缓存了。
10.  2）关闭文件句柄。因为关闭文件句柄后就无法访问文件了，所以内存映射也就没有意义了。
11.  **nextOffsetMetadata**：封装了下一条待插入消息的位移值，也是当前副本的 LEO 值。
12.  **highWatermarkMetadata**：类对象，包括几个字段，比如高水位的偏移量，高水位所在分段文件的基础偏移量，分段文件的偏移量数量即消息数量。作用是追踪当前高水位，以保证包含高水位位移或高于高水位位移的 segments 不会被删除。
13.  **segments**：我个人认为这是 Log 类中最重要的属性。它保存了分区日志下所有的日志段信息，用 ConcurrentNavigableMap\[[java.lang.Long](http://java.lang.long/), LogSegment\] 的**跳表数据结构**来保存日志段对象。Map 的 Key 值是日志段的起始位移值 baseOffset，Value 则是日志段对象本身。这样就可以很轻松地利用该类提供的线程安全和各种支持排序的方法，来管理所有日志段对象。也能保证安全且很方便的查找相邻的日志段，基于跳表实现的并发安全的集合。**其目的就是根据 Offset 快速定位到 LogSegment**。
14.  **leaderEpochCache 对象**：Leader Epoch 是社区于 0.11.0.0 版本引入源码中的，主要是用来判断出现 Failure 时是否执行日志截断操作。**对在这之前是靠高水位来判断的，可能会造成副本间数据不一致**。这里的 leaderEpochCache 是一个缓存类数据，里面保存了分区 Leader 的 Epoch 值与对应位移值的映射关系。

下面通过一张图来说明下「**LogStartOffset**」、「**HighWatermark**」、「**LogEndOffset**」的关系。

![](https://article-images.zsxq.com/FisQwAEVweBmbw0yfApCBu8ts0bJ)

上图中，绿色的位移值 2 表示日志的起始位移「**LogStartOffset**」，而位移值 12 表示日志的末端位移 「**LogEndOffset**」。另外位移值 6 是高水位「**HighWatermark**」，它正好是区分「**已提交消息**」和「**未提交消息**」的分水岭。

## **4.1 Class Log 初始化**

接下来我们来看看「**Log**」类的初始化逻辑：

locally {

// create the log directory if it doesn't exist

// 1、创建 Log 对象对应的分区日志路径。

Files.createDirectories(dir.toPath)

// 2、初始化 Leader Epoch 缓存

initializeLeaderEpochCache()

// 3、加载所有日志段对象，并返回该 Log 对象下一条消息的位移值

val nextOffset \= loadSegments()

/\* Calculate the offset of the next message \*/

// 4、初始化 LEO 元数据对象

nextOffsetMetadata = LogOffsetMetadata(nextOffset, activeSegment.baseOffset, activeSegment.size)

// 5、更新 LeaderEpoch 缓存，清除大于等于 LEO 值的所有无效缓存项

leaderEpochCache.foreach(\_.truncateFromEnd(nextOffsetMetadata.messageOffset))

// 6、更新 LogStartOffset

updateLogStartOffset(math.max(logStartOffset, segments.firstEntry.getValue.baseOffset))

// The earliest leader epoch may not be flushed during a hard failure. Recover it here.

// 7、更新 LeaderEpoch 缓存，清除小于等于 LSO 值的所有无效缓存项

leaderEpochCache.foreach(\_.truncateFromStart(logStartOffset))

// Any segment loading or recovery code must not use producerStateManager, so that we can build the full state here

// from scratch.

if (!producerStateManager.isEmpty)

throw new IllegalStateException("Producer state must be empty during log initialization")

loadProducerState(logEndOffset, reloadFromCleanShutdown = hasCleanShutdownFile)

}

这里梳理下其步骤：

1.  创建 Log 对象对应的分区日志路径。后续所有的「**日志文件**」、「**日志索引文件**」及 「**其他文件**」等都会保存在该路径下。
2.  初始化 Leader Epoch 缓存。Leader Epoch 是**日用来保证副本间数据一致性的**，后面篇章深入讲解。
3.  调用方法 loadSegments() 加载所有日志段对象，加载所有日志段对象，并返回该 Log 对象下一条消息的位移值。
4.  初始化 LEO 元数据对象，这里有三个参数，分别如下：
5.  参数 nextOffset 值为上一步加载日志段对象获取的位移值。
6.  参数起始位移 activeSegment.baseOffset 值是 Active Segment 的起始位移值。
7.  参数日志段大小 activeSegment.size 是 Active Segment 的大小。
8.  我们知道首先向 Log 中写入消息是顺序写入的。**但是只有最后一个 LogSegement 才能执行写入操作**，之前的所有 LogSegement 都不能执行写入操作。为了更好理解这个概念，我们将最后一个 LogSegement 称为**"activeSegement"，即表示当前活跃的日志分段**。随着消息的不断写入，当 activeSegement 满足一定的条件时，就需要创建新的 activeSegement，之后再追加的消息会写入新的 activeSegement。如下图所示：![](https://article-images.zsxq.com/Fh7U8LWufSRa2ZlDJ6qH3QaE6rkt)
9.  更新 LeaderEpoch 缓存，清除大于等于 LEO 值的所有无效缓存项。
10.  更新 LogStartOffset，下面列举一些更新时机：
11.  **当 Log 对象初始化时**：将第一个日志段的起始位移值赋值给 Log Start Offset。
12.  **当日志截断时**：一旦日志中的部分消息被删除，可能会导致 Log Start Offset 发生变化，必须更新该值。
13.  **当 Follower 副本同步时**：一旦 Leader 副本的 Log 对象的 Log Start Offset 值发生变化。为了保证和 Leader 副本的一致性，Follower 副本也需要去更新该值。
14.  **当删除日志段时**：凡是涉及消息删除的操作都有可能导致 Log Start Offset 值的变化。
15.  **当删除消息时**：在 Kafka 中，删除消息就是通过提高 Log Start Offset 值来实现的，因此，删除消息时必须要更新该值。
16.  更新 LeaderEpoch 缓存，清除小于等于 LSO 值的所有无效缓存项。

用一张图来说明它到底做了什么？

![](https://article-images.zsxq.com/FvUDZANX65yP0fJfSRRVGfGPEK5P)

上图我特意将第三步用黄色来标注，说明此步骤比较重要，接下来就来深度剖析下：

## **4.2 loadsegment()**

/\*\*

\* 从磁盘上的日志文件中加载日志段并返回下一个偏移量。此方法不需要将 IOException 转换为 KafkaStorageException，因为它只在加载所有日志之前调用。

\* 如果遇到具有溢出索引偏移的消息的 .swap 文件，则抛出 LogSegmentOffsetOverflowException 异常；或者当我们发现具有溢出的 .log 文件的数量时抛出异常

\*/

private def loadSegments(): Long = {

// 1、首先对日志目录中的文件进行遍历，并清理上次 Failure 遗留下来的各种临时文件（包括以".delete"、".cleaned"、".swap" 结尾的文件），收集Swap文件并查找任何中断的 swap 操作。

val swapFiles \= removeTempFilesAndCollectSwapFiles()

// 2、现在进行第二次遍历，并加载所有的日志和索引文件

// 我们可能会遇到具有偏移量溢出的旧日志段（KAFKA-6264）。我们需要拆分这样的段。当遇到这种情况时，重新从头开始加载段文件。

retryOnOffsetOverflow {

// 如果遇到具有偏移量溢出的段，重试逻辑将对其进行拆分，然后我们需要重试加载段文件。

// 在发起此次 loadSegmentFiles() 调用之前需要关闭所有被遗留的日志段

logSegments.foreach(\_.close())

// 清空所有日志段对象

segments.clear()

// 再次遍历分区日志路径，载入 Segment 和 Index 文件，将 Segment依次加入 cache 中

loadSegmentFiles()

}

// 3、待执行完上面两次遍历后，根据 swap 文件恢复完成所有被中断的操作。载入 SwapSegment 并替换对应的 Segment，为了保证安全，被 swap 段取代的日志文件应该在恢复 swap 文件作为新段文件之前将其重命名为 .deleted，后面的定时任务或者下次的系统重启会删除。

completeSwapOperations(swapFiles)

// 4、如果当前目录是标准目录而不是被标记为“.deleted”，则恢复日志段对象、重置当前活跃日志段的索引大小、返回恢复之后的分区日志 LEO 值。

if (!dir.getAbsolutePath.endsWith(Log.DeleteDirSuffix)) {

val nextOffset \= retryOnOffsetOverflow {

// 根据 snapshot 恢复 Segment 各种缓存，根据 record 进行事务处理初始化等工作

recoverLog()

}

// 重置当前活跃日志段的索引大小，以允许更多的条目。

activeSegment.resizeIndexes(config.maxIndexSize)

// 返回恢复之后的分区日志 LEO 值。

nextOffset

} else {

// 如果日志目录下没有 segment 文件，就创建一个 activeSegment 作为起始，需要保证 Log 中至少有一个 LogSegment。

if (logSegments.isEmpty) {

addSegment(LogSegment.open(dir = dir,

baseOffset = 0,

config,

time = time,

fileAlreadyExists = false,

initFileSize = this.initFileSize,

preallocate = false))

}

// 目录被标记为“.deleted”时，将下一个偏移量设置为 0

0

}

}

/\*\*

\* Add the given segment to the segments in this log. If this segment replaces an existing segment, delete it.

\* @param segment The segment to add

\*/

@threadsafe

def addSegment(segment: LogSegment): LogSegment = this.segments.put(segment.baseOffset, segment)

该方法用来**加载所有日志段对象**，会对分区日志路径**遍历两次**，加载过程如下：

1.  首先对日志目录中的文件进行遍历，并清理上次 Failure 遗留「**这里主要指 Broker 端遇到的任何失败**」下来的各种临时文件（包括以".delete"、".cleaned"、".swap" 结尾的文件），收集Swap文件并查找任何中断的 swap 操作。
2.  现在进行第二次遍历，并加载所有的日志和索引文件。
3.  如果遇到具有偏移量溢出的段，重试逻辑将对其进行拆分，然后我们需要重试加载段文件。 在发起此次 loadSegmentFiles() 调用之前需要关闭所有被遗留的日志段。
4.  **清空所有日志段对象**。
5.  **再次遍历分区日志路径，载入日志段 Segment 和 Index 索引文件，并将 Segment 依次加入 Cache 中**。
6.  **待执行完上面两次遍历后**，根据 swap 文件恢复完成所有被中断的操作。载入 SwapSegment 并替换对应的 Segment，为了保证安全，被 swap 段取代的日志文件应该在恢复 swap 文件作为新段文件之前将其重命名为 .deleted，后面的定时任务或者下次的系统重启会删除。
7.  如果当前目录是标准目录而不是被标记为“.deleted”，则**恢复日志段对象、重置当前活跃日志段的索引大小、返回恢复之后的分区日志 LEO 值**。
8.  否则如果日志目录下没有 segment 文件，就创建一个 activeSegment 作为起始，**重需要保证 Log 中至少有一个 LogSegment**。
9.  最后如果目录被标记为“.deleted”时，将下一个偏移量设置为 0。

最后用一张图来说明下说明它到底做了什么，这样会更加清晰地帮助你理解。

![](https://article-images.zsxq.com/FpV6nDaLfodIIG-AHzP3jFkhF6xQ)

如果到这里你还是比较懵逼，没有理解上面的操作的话，也没关系接下来我们更细粒度的拆解下。**这部分代码非常重要,希望你多花点时间去研究和理解下**，这样你就大致可以搞清楚大部分的分区日志操作了。废话不多说，接着拆分。

## **4.3 removeTempFilesAndCollectSwapFiles()**

/\*\*

\* Removes any temporary files found in log directory, and creates a list of all .swap files which could be swapped

\* in place of existing segment(s). For log splitting, we know that any .swap file whose base offset is higher than

\* the smallest offset .clean file could be part of an incomplete split operation. Such .swap files are also deleted

\* by this method.

\* @return Set of .swap files that are valid to be swapped in as segment files

\*/

private def removeTempFilesAndCollectSwapFiles(): Set\[File\] = {

// 用来删除日志文件对应的 offset、time 或者 transaction 索引文件

def deleteIndicesIfExist(baseFile: File, suffix: String = ""): Unit = {

info(s"Deleting index files with suffix $suffix for baseFile $baseFile")

val offset \= offsetFromFile(baseFile)

Files.deleteIfExists(Log.offsetIndexFile(dir, offset, suffix).toPath)

Files.deleteIfExists(Log.timeIndexFile(dir, offset, suffix).toPath)

Files.deleteIfExists(Log.transactionIndexFile(dir, offset, suffix).toPath)

}

// 定义一个空的可修改的 Set 对象 swapFiles，用于收集 swap 文件。

val swapFiles \= mutable.Set\[File\]()

// 定义一个空的可修改的 Set 对象 cleanFiles，用于收集清理后的 Log 日志文件。

val cleanFiles \= mutable.Set\[File\]()

// 定义一个 Long 类型的变量 minCleanedFileOffset，表示上一次清理的最小 offset 值，默认值为最大值。

var minCleanedFileOffset \= Long.MaxValue

// 1、遍历日志路径下的所有文件

for (file <- dir.listFiles if file.isFile) {

// 如果不可读，则直接抛异常

if (!file.canRead)

throw new IOException(s"Could not read file $file")

// 获取文件名

val filename \= file.getName

// 如果文件名以 .deleted 结尾

if (filename.endsWith(DeletedFileSuffix)) {

debug(s"Deleting stray temporary file ${file.getAbsolutePath}")

// 存在则说明是上次 Failure 遗留下来的临时文件，直接删除。

Files.deleteIfExists(file.toPath)

} else if (filename.endsWith(CleanedFileSuffix)) { // 如果文件名以 .cleaned 结尾

// 从文件名 filename 上获取到 minCleanedFileOffset，此处如果修改了文件名的 offset 后会导致 Broker 崩溃无法启动，这里需要特别注意下。

minCleanedFileOffset = Math.min(offsetFromFileName(filename), minCleanedFileOffset)

cleanFiles += file

} else if (filename.endsWith(SwapFileSuffix)) { // 如果文件名以 .swap 结尾

// we crashed in the middle of a swap operation, to recover:

// if a log, delete the index files, complete the swap operation later

// if an index just delete the index files, they will be rebuilt

val baseFile \= new File(CoreUtils.replaceSuffix(file.getPath, SwapFileSuffix, ""))

info(s"Found file ${file.getAbsolutePath} from interrupted swap operation.")

// 如果该 swap 文件对应的是索引文件

if (isIndexFile(baseFile)) {

// 删除原来的索引文件

deleteIndicesIfExist(baseFile)

} else if (isLogFile(baseFile)) { // 如果该 swap 文件对应的是 log 文件

// 删除原来的索引文件

deleteIndicesIfExist(baseFile)

// 加入待恢复的.swap文件集合中

swapFiles += file

}

}

// KAFKA-6264: Delete all .swap files whose base offset is greater than the minimum .cleaned segment offset. Such .swap

// files could be part of an incomplete split operation that could not complete. See Log#splitOverflowedSegment

// for more details about the split operation.

// 2、从待恢复 swap 集合中找出那些起始位移值大于 minCleanedFileOffset 值的文件，然后遍历删掉这些无效的 .swap 文件

val (invalidSwapFiles, validSwapFiles) = swapFiles.partition(file => offsetFromFile(file) >= minCleanedFileOffset)

invalidSwapFiles.foreach { file =>

debug(s"Deleting invalid swap file ${file.getAbsoluteFile} minCleanedFileOffset: $minCleanedFileOffset")

val baseFile \= new File(CoreUtils.replaceSuffix(file.getPath, SwapFileSuffix, ""))

deleteIndicesIfExist(baseFile, SwapFileSuffix)

Files.deleteIfExists(file.toPath)

}

// Now that we have deleted all .swap files that constitute an incomplete split operation, let's delete all .clean files

// 3、清除所有待删除文件集合中的文件

cleanFiles.foreach { file =>

debug(s"Deleting stray .clean file ${file.getAbsolutePath}")

Files.deleteIfExists(file.toPath)

}

// 4、最后返回当前有效的.swap文件集合

validSwapFiles

}

该方法相对比较简单，主要用来**清理 Log 目录中所有的临时文件**，**并收集 swap 文件**，**以便后续的恢复操作**。**同时**，**也在该方法中删除了所有的索引文件**，**这是由于在 swap 文件操作失败并需要恢复时**，**索引文件可能是不完整或者损坏的**，**需要重新生成**，主要步骤如下：

1.  遍历日志路径下的所有文件。
2.  如果不可读，则直接抛异常。
3.  如果文件名以 .deleted 结尾，则表示是上次 Failure 遗留下来的临时文件，直接删除。
4.  如果文件名以 .cleaned 结尾，则表示该文件已被清理，将其加入 cleanFiles 集合等待后续删除，并记录最小 offset 值。
5.  如果文件名以 .swap 结尾，会根据文件类型来删除：
6.  如果该 swap 文件对应的是索引文件，则删除与之对应的索引文件。
7.  如果该 swap 文件对应的是 Log 文件，则删除与之对应的索引文件并将其加入待恢复的 swapFiles 集合。
8.  从待恢复 swap 集合中找出那些起始位移值大于 minCleanedFileOffset 值的文件，然后遍历删掉这些无效的 .swap 文件。
9.  清除所有待删除文件集合中的文件。
10.  最后返回当前有效的.swap文件集合。

执行完清理和收集工作后，接下来开始**清空已有日志段对象集合**，**并重新加载日志段文件**。我们来看下重载日志段的源码实现。

##   
**4.4 loadSegmentFiles()**

/\*\*

\* This method does not need to convert IOException to KafkaStorageException because it is only called before all logs are loaded

\* It is possible that we encounter a segment with index offset overflow in which case the LogSegmentOffsetOverflowException

\* will be thrown. Note that any segments that were opened before we encountered the exception will remain open and the

\* caller is responsible for closing them appropriately, if needed.

\* @throws LogSegmentOffsetOverflowException if the log directory contains a segment with messages that overflow the index offset

\*/

private def loadSegmentFiles(): Unit = {

// 1、遍历 Log 目录下的所有文件，按照文件名排序后处理，只处理文件类型为 file 的文件。

for (file <- dir.listFiles.sortBy(\_.getName) if file.isFile) {

// 2、如果是索引文件

if (isIndexFile(file)) {

// 获取 Log 文件名对应的 offset 值。

val offset \= offsetFromFile(file)

// 获取对应的 Log 文件。

val logFile \= Log.logFile(dir, offset)

// 如果对应的 Log 文件不存在，则警告并删除该索引文件。

if (!logFile.exists) {

warn(s"Found an orphaned index file ${file.getAbsolutePath}, with no corresponding log file.")

Files.deleteIfExists(file.toPath)

}

// 3、如果文件是 Log 文件

} else if (isLogFile(file)) {

// if it's a log file, load the corresponding log segment

// 获取 Log 文件名对应的 offset 值。

val baseOffset \= offsetFromFile(file)

// 判断该 Log 文件对应的 timeIndex 文件是否已存在，如果不存在则认为新创建文件。

val timeIndexFileNewlyCreated \= !Log.timeIndexFile(dir, baseOffset).exists()

// 创建对应的 LogSegment 实例。

val segment \= LogSegment.open(dir = dir,

baseOffset = baseOffset,

config,

time = time,

fileAlreadyExists = true)

// 对 LogSegment 进行检查，如果存在问题则抛出异常。

try segment.sanityCheck(timeIndexFileNewlyCreated)

catch {

case \_: NoSuchFileException =>

error(s"Could not find offset index file corresponding to log file ${segment.log.file.getAbsolutePath}, " +

"recovering segment and rebuilding index files...")

recoverSegment(segment)

case e: CorruptIndexException =>

warn(s"Found a corrupted index file corresponding to log file ${segment.log.file.getAbsolutePath} due " +

s"to ${e.getMessage}}, recovering segment and rebuilding index files...")

recoverSegment(segment)

}

// 4、将该 LogSegment 添加到 Log 实例的 segments 列表中。

addSegment(segment)

}

}

}

/\*\*

\* Add the given segment to the segments in this log. If this segment replaces an existing segment, delete it.

\* @param segment The segment to add

\*/

@threadsafe

def addSegment(segment: LogSegment): LogSegment = this.segments.put(segment.baseOffset, segment)

该方法也相对比较简单，主要作用是**加载 Log 目录下的所有日志段文件**，**并检查每个 LogSegment 是否存在问题**。如果文件存在问题，则尝试恢复该文件，并重新构建对应的索引文件。如果所有 LogSegment 加载完成且没有发现问题，则将它们添加到 Log 实例的 segments 列表中，步骤如下：

1.  遍历 Log 目录下的所有文件，按照文件名排序后处理，只处理文件类型为 file 的文件。
2.  如果是索引文件
3.  获取 Log 文件名对应的 offset 值。
4.  获取对应的 Log 文件。
5.  如果对应的 Log 文件不存在，则警告并删除该索引文件。
6.  如果文件是 Log 文件
7.  获取 Log 文件名对应的 offset 值。
8.  判断该 Log 文件对应的 timeIndex 文件是否已存在，如果不存在则认为新创建文件。
9.  创建对应的 LogSegment 实例。
10.  对 LogSegment 进行检查，如果存在问题则抛出异常。
11.  最后将该 LogSegment 添加到 Log 实例的 segments 列表中。

执行完加载日志段工作后，接下来处理上面**返回有效 .swap 文件集合**，**并根据 swap 文件恢复完成所有被中断的操作**。我们来看下其源码实现。

## **4.5 completeSwapOperations()**

/\*\*

\* This method does not need to convert IOException to KafkaStorageException because it is only called before all logs

\* are loaded.

\* @throws LogSegmentOffsetOverflowException if the swap file contains messages that cause the log segment offset to overflow. Note that this is currently a fatal exception as we do not have

\* a way to deal with it. The exception is propagated all the way up to KafkaServer#startup which will cause the broker to shut down if we are in this situation. This is expected to be an extremely rare scenario in practice, and manual intervention might be required to get out of it.

\*/

private def completeSwapOperations(swapFiles: Set\[File\]): Unit = {

// 1、遍历所有有效的 .swap 文件

for (swapFile <- swapFiles) {

val logFile \= new File(CoreUtils.replaceSuffix(swapFile.getPath, SwapFileSuffix, ""))

// 获取日志文件的起始位移值

val baseOffset \= offsetFromFile(logFile)

// 2、创建对应的LogSegment实例

val swapSegment \= LogSegment.open(swapFile.getParentFile,

baseOffset = baseOffset,

config,

time = time,

fileSuffix = SwapFileSuffix)

info(s"Found log file ${swapFile.getPath} from interrupted swap operation, repairing.")

// 3、执行日志段恢复操作

recoverSegment(swapSegment)

// We create swap files for two cases:

// (1) Log cleaning where multiple segments are merged into one, and

// (2) Log splitting where one segment is split into multiple.

//

// Both of these mean that the resultant swap segments be composed of the original set, i.e. the swap segment

// must fall within the range of existing segment(s). If we cannot find such a segment, it means the deletion

// of that segment was successful. In such an event, we should simply rename the .swap to .log without having to

// do a replace with an existing segment.

// 4、确认之前删除日志段是否成功，是否还存在旧的日志段文件

val oldSegments \= logSegments(swapSegment.baseOffset, swapSegment.readNextOffset).filter { segment => segment.readNextOffset > swapSegment.baseOffset

}

// 5、如果存在的话直接把 .swap 文件重命名成 .log

replaceSegments(Seq(swapSegment), oldSegments.toSeq, isRecoveredSwapFile = true)

}

}

/\*\*

\* 用于恢复给定的日志段

\* 该方法的作用是恢复给定的 LogSegment，并处理其中的数据。针对 ProducerStateManager 对象，恢复操作的具体实现是根据缓存或者文件中的状态重建生产者状态，并处理所有持久化到日志目录中的数据，以确保系统中的所有状态都正确地保存到了磁盘中。最后对 ProducerStateManager 进行快照以便于后续的恢复操作。同时，对于 leaderEpochCache 参数，该方法用它来更新消息的 leader 选举期，并将其存储到磁盘中。

\*/

private def recoverSegment(segment: LogSegment,

leaderEpochCache: Option\[LeaderEpochFileCache\] = None): Int = lock synchronized {

// 1、创建 ProducerStateManager 对象，用于管理生产者状态。

val producerStateManager \= new ProducerStateManager(topicPartition, dir, maxProducerIdExpirationMs)

// 2、使用 ProducerStateManager 对象重建生产者状态，根据 segment 的 baseOffset 从缓存或者文件恢复状态。

rebuildProducerState(segment.baseOffset, reloadFromCleanShutdown = false, producerStateManager)

// 3、调用 LogSegment 实例的 recover 方法来从 .log 文件和 .index 文件中读取数据，并将其加入到缓存中。对于 LeaderEpochFileCache 参数，这个实例可以用来更新日志段中消息的 leader 选举期存储。

val bytesTruncated \= segment.recover(producerStateManager, leaderEpochCache)

// once we have recovered the segment's data, take a snapshot to ensure that we won't

// need to reload the same segment again while recovering another segment.

// 4、存储 ProducerStateManager 的快照，以便于后续恢复操作

producerStateManager.takeSnapshot()

// 5、返回从该日志段截断的字节数

bytesTruncated

}

/\*\*

\* 该方法的作用是用新的日志段文件替换旧的日志段文件。该方法将旧的日志段文件删除，并将新的日志段文 \* 件添加到 Log 实例的 segments 列表中。当然，该方法还保证了异步删除旧的日志段文件，同时实现了 crash safe。

\*/

private\[log\] def replaceSegments(newSegments: Seq\[LogSegment\], // 表示新的日志段文件序列

oldSegments: Seq\[LogSegment\], // 表示需要替换的旧的日志段文件序列

isRecoveredSwapFile: Boolean = false): Unit = { // 表示该操作是否针对恢复的 swap 文件

lock synchronized {

// 1、将新的日志段文件按照 baseOffset 排序，存储到 sortedNewSegments 变量中。

val sortedNewSegments \= newSegments.sortBy(\_.baseOffset)

// Some old segments may have been removed from index and scheduled for async deletion after the caller reads segments

// but before this method is executed. We want to filter out those segments to avoid calling asyncDeleteSegment()

// multiple times for the same segment.

// 2、将需要替换的旧的日志段文件按照 baseOffset 排序，并过滤掉已被 asyncDelete 标记的文件。

val sortedOldSegments \= oldSegments.filter(seg => segments.containsKey(seg.baseOffset)).sortBy(\_.baseOffset)

checkIfMemoryMappedBufferClosed()

// need to do this in two phases to be crash safe AND do the delete asynchronously

// if we crash in the middle of this we complete the swap in loadSegments()

if (!isRecoveredSwapFile)

// 3、将所有新的日志段文件的后缀名从 Cleaned 修改为 Swap。

sortedNewSegments.reverse.foreach(\_.changeFileSuffixes(Log.CleanedFileSuffix, Log.SwapFileSuffix))

// 4、将所有新的日志段文件添加到 Log 实例的 segments 列表中。

// 这里的 reverse 意义是为了确保 segments 中的日志段能否覆盖包含的 offset 值，因为可能其他线程也要访问 segments

sortedNewSegments.reverse.foreach(addSegment(\_))

// delete the old files

// 5、遍历需要替换的旧的日志段文件。

for (seg <- sortedOldSegments) {

// remove the index entry

// 如果该日志段文件不是最新的日志段文件，则从 segments 列表中移除该日志段文件。

if (seg.baseOffset != sortedNewSegments.head.baseOffset)

segments.remove(seg.baseOffset)

// delete segment files

// 删除该日志段文件，如果 asyncDelete 为 true，则异步删除该文件。

deleteSegmentFiles(List(seg), asyncDelete = true)

}

// okay we are safe now, remove the swap suffix

// 将所有新的日志段文件的后缀名从 Swap 修改为空。这里是因为当做完了 swap 后就要移除 swap 后缀

sortedNewSegments.foreach(\_.changeFileSuffixes(Log.SwapFileSuffix, ""))

}

}

/\*\*

\* 修改文件后缀

\* Change the suffix for the index and log files for this log segment

\* IOException from this method should be handled by the caller

\*/

def changeFileSuffixes(oldSuffix: String, newSuffix: String): Unit = {

log.renameTo(new File(CoreUtils.replaceSuffix(log.file.getPath, oldSuffix, newSuffix)))

lazyOffsetIndex.renameTo(new File(CoreUtils.replaceSuffix(lazyOffsetIndex.file.getPath, oldSuffix, newSuffix)))

lazyTimeIndex.renameTo(new File(CoreUtils.replaceSuffix(lazyTimeIndex.file.getPath, oldSuffix, newSuffix)))

txnIndex.renameTo(new File(CoreUtils.replaceSuffix(txnIndex.file.getPath, oldSuffix, newSuffix)))

}

该方法相对也比较简单，主要作用是**根据 swap 文件恢复完成所有被中断的操作**。步骤如下：

1.  遍历所有有效的 .swap 文件。
2.  创建对应的 LogSegment 实例。
3.  执行日志段恢复操作。
4.  确认之前删除日志段是否成功，是否还存在旧的日志段文件。
5.  如果存在的话直接把 .swap 文件重命名成 .log。

执行完加载日志段工作后，最后我们来看下**恢复日志段对象**。我们来看下其源码实现。

## **4.6 recoverLog()**

/\*\*

\* Recover the log segments and return the next offset after recovery.

\* This method does not need to convert IOException to KafkaStorageException because it is only called before all

\* logs are loaded.

\* @throws LogSegmentOffsetOverflowException if we encountered a legacy segment with offset overflow

\*/

private def recoverLog(): Long = {

// if we have the clean shutdown marker, skip recovery

// 1、如果不存在以 .kafka\_cleanshutdown 结尾的文件，执行恢复

if (!hasCleanShutdownFile) {

// okay we need to actually recover this log

// 2、获取到上次恢复点以外的所有 unflushed 日志段对象

val unflushed \= logSegments(this.recoveryPoint, Long.MaxValue).iterator

var truncated \= false

// 3、遍历 unflushed 日志段

while (unflushed.hasNext && !truncated) {

val segment \= unflushed.next()

info(s"Recovering unflushed segment ${segment.baseOffset}")

val truncatedBytes \=

try {

// 4、执行恢复日志段操作

recoverSegment(segment, leaderEpochCache)

} catch {

case \_: InvalidOffsetException =>

val startOffset \= segment.baseOffset

warn("Found invalid offset during recovery. Deleting the corrupt segment and " +

s"creating an empty one with starting offset $startOffset")

segment.truncateTo(startOffset)

}

// 5、如果有无效的消息导致被截断的字节数不为0，直接删除剩余的日志段对象

if (truncatedBytes > 0) {

// we had an invalid message, delete all remaining log

warn(s"Corruption found in segment ${segment.baseOffset}, truncating to offset ${segment.readNextOffset}")

removeAndDeleteSegments(unflushed.toList,

asyncDelete = true,

reason = LogRecovery)

truncated = true

}

}

}

// 6、上面这些都执行完之后，如果日志段集合不为空了

if (logSegments.nonEmpty) {

val logEndOffset \= activeSegment.readNextOffset

// 验证分区日志的LEO值不能小于Log Start Offset值，否则删除这些日志段对象

if (logEndOffset < logStartOffset) {

warn(s"Deleting all segments because logEndOffset ($logEndOffset) is smaller than logStartOffset ($logStartOffset). " +

"This could happen if segment files were deleted from the file system.")

removeAndDeleteSegments(logSegments,

asyncDelete = true,

reason = LogRecovery)

}

}

// 7、上面这些都执行完之后，如果日志段集合为空了

if (logSegments.isEmpty) {

// no existing segments, create a new mutable segment beginning at logStartOffset

// 至少创建一个新的日志段，以logStartOffset为日志段的起始位移，并加入日志段集合中

addSegment(LogSegment.open(dir = dir,

baseOffset = logStartOffset,

config,

time = time,

fileAlreadyExists = false,

initFileSize = this.initFileSize,

preallocate = config.preallocate))

}

// 8、更新上次恢复点属性，并返回

recoveryPoint = activeSegment.readNextOffset

recoveryPoint

}

该方法相对也比较简单，主要作用是**恢复日志段对象**。步骤如下：

1.  如果不存在以 .kafka\_cleanshutdown 结尾的文件，执行恢复。
2.  获取到上次恢复点以外的所有 unflushed 日志段对象，**unflushed 记录的是未写入到检查点文件的日志段**。
3.  遍历 unflushed 日志段。
4.  执行恢复日志段操作。
5.  如果有无效的消息导致被截断的字节数不为0，直接删除剩余的日志段对象。
6.  等上面这些都执行完之后，如果日志段集合不为空了，则验证分区日志的LEO值不能小于Log Start Offset值，否则删除这些日志段对象。
7.  等上面这些都执行完之后，如果日志段集合为空了，则至少创建一个新的日志段，以logStartOffset为日志段的起始位移，并加入日志段集合中。
8.  更新上次恢复点属性，并返回。

至此，我们就剖析完了**加载所有日志段对象**的所有流程，剩下的方法等下篇 Log 相关操作的时候再进行剖析。

## **05 总结**

这里，我们一起来总结一下这篇文章的重点。

1、通过上一篇的分析我们引出了服务端日志的管理对象之 「**Log**」。

2、带你剖析了 **Log 源码总览**。

3、接下来分两部分讲解了「**Log**」相关的属性以及初始化逻辑：「**Object Log**」、「**Class Log**」。

4、接着带你深度剖析了「**加载日志段**」的过程以及内部相关调用方法的源码。

下篇我们来深度剖析「**Log 对象操作**」，大家期待，我们下期见。