大家好，我是 **华仔**, 又跟大家见面了。

上两篇中，主要带大家深度剖析了 「**Kafka 服务端源码稀疏索引架构设计**」，通过「**场景驱动方式**」，现在消息被封装成批次请求已经从「**生产者**」发送到「**Broker**」，且被「**网络层**」所接收到并准备进行消息数据存储，从今天开始，我们来深度剖析 Kafka 日志系统的底层实现，这是日志系列第八篇，我们回过头来继续来深度聊聊「**Kafka 服务端源码之 Log 日志管理操作**」，看看 Kafka 服务端是如何对日志进行管理操作。

![](https://article-images.zsxq.com/Fr8fKAoy5dFCllhS1xicDPCUZZqW)

## **01 总体概述**

在这篇 [【服务端 Broker 源码分析系列第十篇】图解 Kafka 源码之 LogManager 磁盘文件管理组件](https://articles.zsxq.com/id_fjotrswcfval.html) 的最后，当 「**LogManager**」在初始化的时候启动一些 「**周期性后台任务**」完成日志处理相关的工作。主要是启动五个定时任务以及启动一个后台线程 Cleaner。

今天我们就来深度剖析下这些任务是如何操作和管理日志的。

好了，下面开始剖析其源码实现，让你对整个日志管理有个总体的认知，深度剖析下其内部是如何进行日志管理的，整个流程是怎么样的？那么带着这些问题进入今天的正题。

整个日志管理的相关组件的调用关系图如下：

![](https://article-images.zsxq.com/FgVmpr3JOm-_1PVfCHJC94zu4_tC)

## **02 LogManager 后台任务**

/\*\*

\* Start the background threads to flush logs and do log cleanup

\*/

def startup(): Unit = {

/\* Schedule the cleanup task to delete old logs \*/

// 后台线程池不为空的情况下进行日志管理后台工作的启动，这里会启动五个定时调度的任务。

if (scheduler != null) {

info("Starting log cleanup with a period of %d ms.".format(retentionCheckMs))

// 1、启动 kafka-log-retention 周期性任务，遍历所有的log，对过期或过大的日志文件执行清理工作。

scheduler.schedule("kafka-log-retention",

cleanupLogs \_, // 定时执行的方法

delay = InitialTaskDelayMs,// 启动之后30s开始定时调度

period = retentionCheckMs, // log.retention.check.interval.ms 默认为5分钟。

TimeUnit.MILLISECONDS)

info("Starting log flusher with a default period of %d ms.".format(flushCheckMs))

// 2、启动 kafka-log-flusher 周期性任务，对日志文件执行刷盘操作，定时把内存中的数据刷到磁盘中。

scheduler.schedule("kafka-log-flusher",

flushDirtyLogs \_, // 定时执行的方法

delay = InitialTaskDelayMs, // 启动之后30s开始定时调度

period = flushCheckMs,// log.flush.scheduler.interal.ms 默认值为Long.MaxValue。也就是说默认不刷盘，操作系统自己刷盘。

TimeUnit.MILLISECONDS)

// 3、启动 kafka-recovery-point-checkpoint 周期性任务，更新 kafka-recovery-point-checkpoint 文件。向路径中写入当前的恢复点，避免在重启的时候重新恢复全部数据。

scheduler.schedule("kafka-recovery-point-checkpoint",

checkpointLogRecoveryOffsets \_,// 定时执行的方法

delay = InitialTaskDelayMs,// 启动之后30s开始定时调度

period = flushRecoveryOffsetCheckpointMs,// log.flush.offset.checkpoint.interval.ms 默认为1分钟。

TimeUnit.MILLISECONDS)

// 4、启动 kafka-log-start-offset-checkpoint 周期性任务，更新 kafka-log-start-offset-checkpoint 文件。向日志目录写入当前存储日志中的 start offset，避免读到已经被删除的日志。

scheduler.schedule("kafka-log-start-offset-checkpoint",

checkpointLogStartOffsets \_,// 定时执行的方法

delay = InitialTaskDelayMs,// 启动之后30s开始定时调度

period = flushStartOffsetCheckpointMs, // log.flush.start.offset.checkpoint.interval.ms 默认值为1分钟

TimeUnit.MILLISECONDS)

// 5、启动 kafka-delete-logs 周期性任务，清理已经被标记为删除的日志。

scheduler.schedule("kafka-delete-logs", // will be rescheduled after each delete logs with a dynamic period

deleteLogs \_,// 定时执行的方法

delay = InitialTaskDelayMs,// 启动之后30s开始定时调度

unit = TimeUnit.MILLISECONDS)

}

// 6、日志清理压缩线程启动

if (cleanerConfig.enableCleaner)

cleaner.startup()

}

首先这五个从后台启动的周期性任务是通过 KafkaScheduler 这个 Kafka 基于 java 的ScheduledThreadPoolExecutor 自定义的调度程序。

1.  启动 kafka-log-retention 周期性任务，遍历所有的 log，对过期或过大的日志文件执行清理工作。
2.  清除条件：1.日志超过保留时间。2.日志大小超过保留大小时，会删除最旧的 LogSegment，以控制整个 Log 的大小。
3.  启动 kafka-log-flusher 周期性任务，对日志文件执行刷盘操作，定时把内存中的数据刷到磁盘中。
4.  启动 kafka-recovery-point-checkpoint 周期性任务，更新 kafka-recovery-point-checkpoint 文件。向路径中写入当前的恢复点，避免在重启的时候重新恢复全部数据。
5.  启动 kafka-log-start-offset-checkpoint 周期性任务，更新 kafka-log-start-offset-checkpoint 文件。向日志目录写入当前存储日志中的 start offset，避免读到已经被删除的日志。
6.  启动 kafka-delete-logs 周期性任务，清理已经被标记为删除的日志。
7.  最后启动日志清理压缩线程。

## **2.1 kafka-log-retention**

kafka-log-retention 该任务底层执行的是 cleanupLogs() 方法，主要用来**定时清理过期的日志段文件并维护者日志的大小**，源码如下：

/\*\*

\* 日志清除任务

\* Delete any eligible logs. Return the number of segments deleted.

\* Only consider logs that are not compacted.

\*/

def cleanupLogs(): Unit = {

// 记录日志，表示开始清理操作

debug("Beginning log cleanup...")

// 定义变量 total，用于存储删除的日志总数。

var total \= 0

// 变量 startMs，存储方法开始执行时的时间戳，用于记录操作所需时间

val startMs \= time.milliseconds

// clean current logs.

// 初始化变量 deletableLogs，它是删除操作的目标，没有日志记录状态更改的话先返回 currentLogs，否则调用清理功能，并从中筛选出非压缩日志。

val deletableLogs \= {

if (cleaner != null) {

// prevent cleaner from working on same partitions when changing cleanup policy

// 1、先中断当前 topic-partition 下正在进行执行 cleaner 的线程

cleaner.pauseCleaningForNonCompactedPartitions()

} else {

// 2、过滤掉 cleanup.policy 配置的不是 delete 的 log

currentLogs.filter {

case (\_, log) => !log.config.compact

}

}

}

// 到这里了 deletableLogs 中 log 的 cleanup.policy 配置是 delete，开始进行删除

try {

// 遍历被筛选的删除日志文件列表，执行以下操作。

deletableLogs.foreach {

// 匹配主题和分区，并将其分配给变量 topicPartition 和 log。

case (topicPartition, log) =>

// 记录日志，表示开始清理日志。

debug(s"Garbage collecting '${log.name}'")

// 3、委托给 log.deleteOldSegments() 方法进行删除过期的 \`logSegment\`。

total += log.deleteOldSegments()

// 获取分区主题的日志段，并将其分配给变量 futureLog。

val futureLog \= futureLogs.get(topicPartition)

if (futureLog != null) {

// clean future logs

debug(s"Garbage collecting future log '${futureLog.name}'")

// 调用日志模块的方法 deleteOldSegments() 删除日志段。

total += futureLog.deleteOldSegments()

}

}

} finally {

if (cleaner != null) {

// 如果存在清理功能，则唤醒清理线程。

// 如果在第 1 步中中断了 cleaner 线程，则在这里进行恢复。也就是说在 topic-partition 的同一时刻 只有一个 cleaner 对其进行清理

cleaner.resumeCleaning(deletableLogs.map(\_.\_1))

}

}

debug(s"Log cleanup completed. $total files deleted in " +

(time.milliseconds - startMs) / 1000 + " seconds")

}

清理日志的方法相对也比较简单，**在加载日志的时候会存储到 currentLogs 或者 futureLogs 中**，该方法会遍历所有的 Log 对象，并从两个维度对执行清理工作：

1.  时间维度，即保证 Log 对象中所有的 LogSegment 都是有效的，对于过期的 LogSegment 执行删除操作。
2.  空间维度，既保证 Log 对象不应过大，对于超出的部分会执行删除操作。

如果筛选出有删除的日志对象，则调用 「**LogSegment**」的删除日志段方法，该方法在 [【服务端 Broker 源码分析系列第十三篇】图解 Kafka 源码之日志 Log 对象操作](https://articles.zsxq.com/id_n9jj4wsw958w.html) 中已经剖析过了，如果不理解请自行查阅。

那么我们来剖析了方法内部调用的清理方法。

###   
**2.1.1 pauseCleaningForNonCompactedPartitions**

「**LogCleaner**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/Lo](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogSegment.scala)gCleaner[.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogCleaner.scala)

「**LogCleanerManager**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/Lo](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogSegment.scala)gCleanerManager[.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogCleanerManager.scala)

\# LogCleaner.scala

/\*\*

\* To prevent race between retention and compaction,

\* retention threads need to make this call to obtain:

\* @return A list of log partitions that retention threads can safely work on

\*/

def pauseCleaningForNonCompactedPartitions(): Iterable\[(TopicPartition, Log)\] = {

cleanerManager.pauseCleaningForNonCompactedPartitions()

}

\# LogCleanerManager.scala

/\*\*

\* 暂停非压缩分区的日志清理过程

\*/

def pauseCleaningForNonCompactedPartitions(): Iterable\[(TopicPartition, Log)\] = {

inLock(lock) {

// 1、筛选所有未在进行清理并且未被压缩的日志文件，返回它们的键值对。

val deletableLogs \= logs.filter {

// 选出不需要压缩的日志文件

case (\_, log) => !log.config.compact // pick non-compacted logs

}.filterNot {

// 选出尚未处于日志清理过程中的文件。

case (topicPartition, \_) => inProgress.contains(topicPartition) // skip any logs already in-progress

}

deletableLogs.foreach {

// 2、将分区和对应的暂停清理标志 LogCleaningPaused 存储到 inProgress 集合中

case (topicPartition, \_) => inProgress.put(topicPartition, LogCleaningPaused(1))

}

// 3、被筛选出的可以暂停清理的日志文件及其对应的主题分区

deletableLogs

}

}

该方法主要用来**筛选出可以暂停清理的日志文件以及对应的主题分区信息**。也比较简单，步骤如下：

1.  筛选所有未在进行清理并且未被压缩的日志文件，返回它们的键值对。
2.  选出不需要压缩的日志文件。
3.  选出尚未处于日志清理过程中的文件。
4.  遍历筛选结果将分区和对应的暂停清理标志 LogCleaningPaused 存储到 inProgress 集合中
5.  被筛选出的可以暂停清理的日志文件及其对应的主题分区。

## **2.2 kafka-log-flusher**

kafka-log-flusher 该任务底层执行的是 flushDirtyLogs() 方法，主要用来**定期对日志文件执行刷盘操作**，遍历处理每个 topic 分区对应的 Log 对象，通过记录在 Log 对象中的上次执行 flush 的时间戳与当前时间对比，如果时间差值超过一定的阈值（对应 [flush.ms](http://flush.ms/) 配置），则调用 Log#flush 方法执行刷盘操作。源码如下：

/\*\*

\* Flush any log which has exceeded its flush interval and has unwritten messages.

\*/

private def flushDirtyLogs(): Unit = {

debug("Checking for dirty logs to flush...")

// 1、遍历 currentLogs 和 futureLogs 集合

for ((topicPartition, log) <- currentLogs.toList ++ futureLogs.toList) {

try {

// 2、计算上一次 flush 的时间与当前时间差值

val timeSinceLastFlush \= time.milliseconds - log.lastFlushTime

debug(s"Checking if flush is needed on ${topicPartition.topic} flush interval ${log.config.flushMs}" + s" last flushed ${log.lastFlushTime} time since last flush: $timeSinceLastFlush")

//3、如果满足 flush.ms 配置的时间，则调用 flush 方法刷新到磁盘上

//会把 \[recoverPoint ~ LEO\] 之间的消息数据刷新到磁盘上，并修改 recoverPoint 值

if(timeSinceLastFlush >= log.config.flushMs)

log.flush()

} catch {

case e: Throwable =>

error(s"Error flushing topic ${topicPartition.topic}", e)

}

}

}

该方法非常简单，步骤如下：

1.  遍历 currentLogs 和 futureLogs 集合
2.  计算上一次 flush 的时间与当前时间做差值，看是否满足条件
3.  如果满足 [flush.ms](http://flush.ms/) 配置的时间，则调用 flush 方法 刷新到磁盘上，会把 \[recoverPoint ~ LEO\] 之间的消息数据刷新到磁盘上，并修改 recoverPoint 值。

其中最重要的是「**第二步**」，在 [【服务端 Broker 源码分析系列第十三篇】图解 Kafka 源码之日志 Log 对象操作](https://articles.zsxq.com/id_n9jj4wsw958w.html) 这篇已经剖析过了，如果不理解请自行查阅。

##   
**2.3 kafka-recovery-point-checkpoint**

kafka-recovery-point-checkpoint 该任务底层执行的是 checkpointLogRecoveryOffsets() 方法，主要用来**定时将 recovery-point 写入到 kafka-recovery-point-checkpoint 文件中**。

/\*\*

\* Write out the current recovery point for all logs to a text file in the log directory

\* to avoid recovering the whole log on startup.

\*/

def checkpointLogRecoveryOffsets(): Unit = {

// 1、获取所有主题分区的日志文件，将它们按照目录进行分组，存储在变量 logsByDirCached 中。

val logsByDirCached \= logsByDir

// 2、循环每个主题分区日志存储目录

liveLogDirs.foreach { logDir =>

// 3、获取当前目录下的所有主题分区日志文件。

val logsToCheckpoint \= logsInDir(logsByDirCached, logDir)

// 4、检查点恢复偏移并清理快照文件。

checkpointRecoveryOffsetsAndCleanSnapshotsInDir(

logDir, // 存储日志文件的目录

logsToCheckpoint, // 需要进行检查点恢复偏移的日志文件

logsToCheckpoint.values.toSeq) // 需要清理快照文件的日志文件列表, toSeq 表示将日志文件从 Map 类型转换为序列类型

}

}

其步骤如下：

1.  获取所有主题分区的日志文件，将它们按照目录进行分组，存储在变量 logsByDirCached 中。
2.  循环每个主题分区日志存储目录，执行检查操作。
3.  获取当前目录下的所有主题分区日志文件。
4.  检查点恢复偏移并清理快照文件。

其中最重要的是「**第四步**」，我们继续来看下：

###   
**2.3.1 检查点恢复偏移并清理快照文件**

/\*\*

\* Checkpoint recovery offsets for all the provided logs and clean the snapshots of all the

\* provided logs.

\* @param logDir the directory in which the logs are

\* @param logsToCheckpoint the logs to be checkpointed

\* @param logsToCleanSnapshot the logs whose snapshots will be cleaned

\*/

private def checkpointRecoveryOffsetsAndCleanSnapshotsInDir(logDir: File, logsToCheckpoint: Map\[TopicPartition, Log\], logsToCleanSnapshot: Seq\[Log\]): Unit = {

try {

// 1、查找指定目录中的恢复检查点文件，并对其中的恢复偏移量进行更新。

recoveryPointCheckpoints.get(logDir).foreach { checkpoint =>

// 2、构造一个 Map，将每个日志文件的主题分区和恢复偏移量配对。

val recoveryOffsets \= logsToCheckpoint.map { case (tp, log) => tp -> log.recoveryPoint }

// 3、将 recoveryOffsets 写入恢复检查点文件

checkpoint.write(recoveryOffsets)

}

// 4、循环遍历需要清理快照的日志文件列表，对其进行快照清理。每个日志文件会删除其恢复检查点后的快照文件。

logsToCleanSnapshot.foreach(\_.deleteSnapshotsAfterRecoveryPointCheckpoint())

} catch {

// 如果磁盘发生错误，则记录错误信息。

case e: KafkaStorageException =>

error(s"Disk error while writing recovery offsets checkpoint in directory $logDir: ${e.getMessage}")

// 如果发生输入输出错误，则将该目录标记为离线，并且记录错误信息。

case e: IOException =>

logDirFailureChannel.maybeAddOfflineLogDir(logDir.getAbsolutePath,

s"Disk error while writing recovery offsets checkpoint in directory $logDir: ${e.getMessage}", e)

}

}

步骤如下：

1.  查找指定目录中的恢复检查点文件，并对其中的恢复偏移量进行更新。
2.  构造一个 Map，将每个日志文件的主题分区和恢复偏移量配对。
3.  将 recoveryOffsets 写入恢复检查点文件
4.  循环遍历需要清理快照的日志文件列表，对其进行快照清理。每个日志文件会删除其恢复检查点后的快照文件。
5.  如果磁盘发生错误，则记录错误信息。
6.  如果发生输入输出错误，则将该目录标记为离线，并且记录错误信息。

其中最重要的是「**第三步**」、「**第四步**」，我们继续来看下：

### **2.3.2 将恢复偏移写入检查点文件**

这是 「**CheckPointFile**」类文件方法，「**CheckPointFile**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/checkpoints/CheckpointFile](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/checkpoints/CheckpointFile.scala)[.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogCleanerManager.scala)

// 用于将接收到的数据写入到偏移量检查点文件中。

def write(entries: Iterable\[T\]): Unit = {

// 1、获取该对象的锁，以保证写操作的互斥性和可见性。

lock synchronized {

try {

// write to temp file and then swap with the existing file

// 2、创建一个文件输出流对象，用于写入偏移量检查点文件。

val fileOutputStream \= new FileOutputStream(tempPath.toFile)

// 3、创建一个带有缓冲的输出流，用于写入偏移量检查点文件的每一行。

val writer \= new BufferedWriter(new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF\_8))

try {

// 4、将偏移量检查点文件的版本号写入到文件中。

writer.write(version.toString)

writer.newLine()

// 5、将待写入的数据条数写入到文件中。

writer.write(entries.size.toString)

writer.newLine()

// 6、对于待写入的每一个数据条目，循环使用 formatter 将其转化为文本格式，然后写入到文件中。

entries.foreach { entry =>

writer.write(formatter.toLine(entry))

writer.newLine()

}

// 7、将输出流中的缓存内容刷新到磁盘，确保数据被写入到磁盘中。

writer.flush()

// 8、将文件所在的磁盘缓存与磁盘同步，以避免数据丢失。

fileOutputStream.getFD().sync()

} finally {

writer.close()

}

// 9、将临时文件原子地移动到偏移量检查点文件中，以保证文件的一致性和可用性。

Utils.atomicMoveWithFallback(tempPath, path)

} catch {

case e: IOException =>

val msg \= s"Error while writing to checkpoint file ${file.getAbsolutePath}"

logDirFailureChannel.maybeAddOfflineLogDir(logDir, msg, e)

throw new KafkaStorageException(msg, e)

}

}

}

步骤如下：

1.  获取该对象的锁，以保证写操作的互斥性和可见性。
2.  创建一个文件输出流对象，用于写入偏移量检查点文件。
3.  创建一个带有缓冲的输出流，用于写入偏移量检查点文件的每一行。
4.  将偏移量检查点文件的版本号写入到文件中。
5.  将待写入的数据条数写入到文件中。
6.  对于待写入的每一个数据条目，循环使用 formatter 将其转化为文本格式，然后写入到文件中。
7.  将输出流中的缓存内容刷新到磁盘，确保数据被写入到磁盘中。
8.  将文件所在的磁盘缓存与磁盘同步，以避免数据丢失。
9.  将临时文件原子地移动到偏移量检查点文件中，以保证文件的一致性和可用性。

总结就两步：

1.  先将 log 目录下的所有 recoveryPoint 写到 tmp 临时文件中。
2.  然后用 tmp 文件替换原来的 RecoveryPointCheckpoint 文件中。

### **2.3.3 清理快照文件**

// 用来删除某个日志文件的恢复点检查点后的快照

def deleteSnapshotsAfterRecoveryPointCheckpoint(): Long = {

// 首先计算一个最小的偏移量，该偏移量以及其之前的快照文件将被保留

val minOffsetToRetain \= minSnapshotsOffsetToRetain

// 删除该文件中偏移量小于 minOffsetToRetain 的快照文件

producerStateManager.deleteSnapshotsBefore(minOffsetToRetain)

// 返回 minOffsetToRetain。

minOffsetToRetain

}

// 获取保留快照的最小偏移量，即偏移量之前的快照文件将被保留

private\[log\] def minSnapshotsOffsetToRetain: Long = {

// 获取日志文件的锁

lock synchronized {

// 计算要保留的最小偏移量，该偏移量以及其之前的快照文件将被保留

val twoSegmentsMinOffset \= lowerSegment(activeSegment.baseOffset).getOrElse(activeSegment).baseOffset

// Prefer segment base offset

// 计算恢复点偏移量

val recoveryPointOffset \= lowerSegment(recoveryPoint).map(\_.baseOffset).getOrElse(recoveryPoint)

// 根据最小偏移量和恢复点计算一个偏移量。

math.min(recoveryPointOffset, twoSegmentsMinOffset)

}

}

// 找到最近的存储上给定偏移量的日志段

// 接收一个偏移量并返回其之前的日志段。 它基于 segments，该变量是日志文件中所有日志段的列表，每个日志段具有开始和结束偏移量。因为 segments 是按基础偏移排序的，所以可以使用 lowerEntry 方法来查找最近的日志段。

private def lowerSegment(offset: Long): Option\[LogSegment\] =

// lowerEntry 方法在 segments 列表中查找最后一个元素，其基础偏移量小于指定的偏移量。

Option(segments.lowerEntry(offset)).map(\_.getValue)

这些方法都比较简单，自行研究一下即可，如有问题，可以文末留言沟通。

## **2.4 kafka-log-start-offset-checkpoint**

kafka-log-start-offset-checkpoint 该任务底层执行的是 checkpointLogStartOffsets() 方法，log-start-offset 主要用来标识日志的起始偏移量。各个副本在变动 LEO 和 HW的过程中，logStartOffset 也有可能随之而动，Kafka 也有一个定时任务来**定时更新起始偏移量文件信息**，源码如下：

// 在指定目录中检查点各个主题分区的起始偏移量。

def checkpointLogStartOffsets(): Unit = {

// 它缓存所有主题分区的日志文件，将它们按照目录进行分组，存储在变量 logsByDirCached 中

val logsByDirCached \= logsByDir

liveLogDirs.foreach { logDir =>

// 它对于每个主题分区日志存储目录，调用 checkpointLogStartOffsetsInDir 方法，进行检查点操作

checkpointLogStartOffsetsInDir(logDir, logsInDir(logsByDirCached, logDir))

}

}

// 在指定目录中检查点各个主题分区的起始偏移量，将它们写入检查点文件中。

private def checkpointLogStartOffsetsInDir(logDir: File, logsToCheckpoint: Map\[TopicPartition, Log\]): Unit = {

try {

// 首先查找指定目录中的起始偏移量检查点文件

logStartOffsetCheckpoints.get(logDir).foreach { checkpoint =>

// 收集起始偏移量信息

val logStartOffsets \= logsToCheckpoint.collect {

// 这里构造一个 Map，将每个主题分区和其起始偏移量配对。每个主题分区的起始偏移量为其日志文件的 logStartOffset 属性，但需要保证其不小于第一个日志段的基本偏移量

case (tp, log) if log.logStartOffset > log.logSegments.head.baseOffset => tp -> log.logStartOffset

}

// 将 logStartOffsets 写入起始偏移量检查点文件

checkpoint.write(logStartOffsets)

}

} catch {

// 如果磁盘发生错误，则记录错误信息。

case e: KafkaStorageException =>

error(s"Disk error while writing log start offsets checkpoint in directory $logDir: ${e.getMessage}")

}

}

逻辑同 **kafka-recovery-point-checkpoint**，先找到起始偏移量信息，然后更新其检查点文件。

## **2.5 kafka-delete-logs**

kafka-delete-logs 该任务底层执行的是 deleteLogs() 方法，主要用来**定时将已被标记为 .delete 后缀的日志文件进行删除**，就是不断从 logsToBeDeleted 队列中取出然后删除。

// 异步删除已被标记为过时的日志文件

private def deleteLogs(): Unit = {

var nextDelayMs \= 0L

try {

// 1、计算即将删除的下一个日志文件距离当前时间的时间差，以确定下一次删除的时间间隔。如果有日志文件需要被删除，则返回时间差，否则返回默认值 fileDeleteDelayMs。

def nextDeleteDelayMs: Long = {

if (!logsToBeDeleted.isEmpty) {

// 队列中取出

val (\_, scheduleTimeMs) = logsToBeDeleted.peek()

scheduleTimeMs + currentDefaultConfig.fileDeleteDelayMs - time.milliseconds()

} else

currentDefaultConfig.fileDeleteDelayMs

}

// 2、用于删除已被标记为过时的日志文件。在每次循环中，计算下一次删除日志文件的时间间隔 nextDelayMs，并且如果 nextDelayMs 大于零，则方法会休眠相应的时间。然后从 logsToBeDeleted 队列中取出第一个即将删除的日志文件，并将其从队列中删除。随后删除该日志文件，并打印日志信息

while ({nextDelayMs = nextDeleteDelayMs; nextDelayMs <= 0}) {

// 从队列中取出

val (removedLog, \_) = logsToBeDeleted.take()

if (removedLog != null) {

try {

// 删除

removedLog.delete()

info(s"Deleted log for partition ${removedLog.topicPartition} in ${removedLog.dir.getAbsolutePath}.")

} catch {

case e: KafkaStorageException =>

error(s"Exception while deleting $removedLog in dir ${removedLog.parentDir}.", e)

}

}

}

} catch {

case e: Throwable =>

error(s"Exception in kafka-delete-logs thread.", e)

} finally {

try {

// 3、重新创建一个定时任务

// 使用定时调度器 scheduler 设置下一次删除操作的时间，并把方法自身作为回调函数进行调度。如果定时调度器抛出了异常，则将该方法标记为已停止，会导致该方法不再执行，并打印异常信息。

scheduler.schedule("kafka-delete-logs",

deleteLogs \_,

delay = nextDelayMs,

unit = TimeUnit.MILLISECONDS)

} catch {

case e: Throwable =>

if (scheduler.isStarted) {

// No errors should occur unless scheduler has been shutdown

error(s"Failed to schedule next delete in kafka-delete-logs thread", e)

}

}

}

}

该方法比较简单，定时把.delete后缀的日志删除，此定时任务要与 Kafka-log-retention 区分开来。

1.  Kafka-log-retention 是删除过期的 **logSegment 对象**。
2.  kafka-delete-logs 是删除  **delete 后缀的文件****。**
3.  在 LogManager 初始化的时候 会调用 loadLogs 方法加载日志，如果发现上次 Kafka 关闭的时候没有删除的文件，会加入 logsToBeDeleted 队列中，等待此定时任务来删除
4.  用户进行 move 文件的时候，会把旧的文件标记为 .delete 加入 logsToBeDeleted 队列中，也就是说 Kafka 移动文件并不是移动而是在目标目录下创建并且删除旧文件。

这里的 [file.delete.delay.ms](http://file.delete.delay.ms/) 决定延迟多久删除，默认 60000 毫秒。

简单总结下就是不断从 logsToBeDeleted 队列中取出然后删除。

最后我们来看下清理线程的工作流程和原理。

##   
**03 清理线程**

这是**执行日志压缩的操作线程**，与 kafka-log-retention 定时任务的区别在于:

1.  kafka-log-retention 是根据「**时间**」、「**日志大小**」、「**日志偏移量**」三个维度来删除日志。
2.  cleaner 线程是清理「**墓碑消息**」墓碑消息(key 不为空，value 为空的消息)，以及「**压缩消息**」(相同 key 保留最新的value)。

> 如果消费者只关心key 对应的最新value 值，就可以开启日志压缩功能，以官方的例子说明： key为K1 的offset 0、2、3的三条消息只会保存最新的一条。也就是offset为3的值。

![](https://article-images.zsxq.com/Fnjx0igUP1MyeU9QU8L0zFRAqeSZ)

「**Log**」 在写入消息的时候其实就是将消息加入到 「**activeSegment**」的日志文件的末尾，为了避免 「**activeSegment**」成为热点，「**activeSegment**」不会参与**日志压缩**操作。

在日志压缩的过程中可以启动多条 Cleaner 线程，通过配置进行配置。每个 Log 都可以通过 CleanerPoint 为分隔成两个部分，如下图所示：

![](https://article-images.zsxq.com/FtEPFC1ozzzG599mUlj1hnUZj6MG)

1.  Log Head 部分就是和普通日志一样保存了所有的消息
2.  Log Tail 部分就是经过 Cleaner 线程清理过的部分。
3.  可以通过 [min.cleanable.dirty.ratio](http://min.cleanable.dirty.ratio/) 来配置指定topic 的日志 dirty 部分占比，占比越高那么会优先进行清除，也可以通过 [log.cleaner.min.cleanable.ratio](http://log.cleaner.min.cleanable.ratio/) 配置整个 broker 中的 topic 。
4.  Cleaner 线程通过 dirty 的占比选出需要被清理的 Log 后， 首先会为 dirty 部分的消息建立 key 与其 last\_offset 的映射关系，通过 SkimpyOffsetMap 维护。

## **3.1 日志压缩相关实现类梳理**

1.  **LogCleaner 类**：通过 cleaner 字段管理 CleanerThread 线程，通过 startup() 方法和 shutdown 方法完成 CleanerThread 线程的启动和停止。
2.  **CleanerThread 线程类**：日志压缩的真正逻辑的地方，继承 **ShutdownableThread 抽象线程类**。
3.  **ShutdownableThread 抽象类**：继承 Thread，给 CleanerThread 提供基础方法，比如 initiateShutdown 、awaitShutdown 、 run 等方法。
4.  **Cleaner**：CleanerThread 线程的一些方法都委托给了 Cleaner 。
5.  **LogCleanerManager 管理类**：负责每个 log 的压缩状态管理以及 cleaner checkpoint 信息维护。

「**LogCleaner**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/Lo](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogSegment.scala)gCleaner[.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogCleaner.scala)

「**ShutdownableThread**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/utils/ShutdownableThread.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/utils/ShutdownableThread.scala)

「**LogCleanerManager**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/Lo](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogSegment.scala)gCleanerManager[.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogCleanerManager.scala)

先来看下启动项：

// LogManager#startup 方法

if (cleanerConfig.enableCleaner)

cleaner.startup()

// LogCleaner 线程类

class LogCleaner(initialConfig: CleanerConfig,

val logDirs: Seq\[File\],

val logs: Pool\[TopicPartition, Log\],

val logDirFailureChannel: LogDirFailureChannel,

time: Time = Time.SYSTEM) extends Logging with KafkaMetricsGroup with BrokerReconfigurable

{

/\*\*

\* Start the background cleaning

\*/

def startup(): Unit = {

info("Starting the log cleaner")

(0 until config.numThreads).foreach { i =>

// 启动 cleanerThread 线程。

val cleaner \= new CleanerThread(i)

cleaners += cleaner

cleaner.start()

}

}

....

}

这里默认会启动一个 CleanerThread 线程，可以通过 log.cleaner.threads 进行配置。

## **3.2 CleanerThread 类源码分析**

该类是 **LogCleaner 类的私有子线程类**。

/\*\*

\* The cleaner threads do the actual log cleaning. Each thread processes does its cleaning repeatedly by

\* choosing the dirtiest log, cleaning it, and then swapping in the cleaned segments.

\*/

private\[log\] class CleanerThread(threadId: Int)

extends ShutdownableThread(name = s"kafka-log-cleaner-thread-$threadId", isInterruptible = false) {

protected override def loggerName \= classOf\[LogCleaner\].getName

if (config.dedupeBufferSize / config.numThreads > Int.MaxValue)

warn("Cannot use more than 2G of cleaner buffer space per cleaner thread, ignoring excess buffer space...")

val cleaner \= new Cleaner(id = threadId,

offsetMap = new SkimpyOffsetMap(memory = math.min(config.dedupeBufferSize / config.numThreads, Int.MaxValue).toInt,hashAlgorithm = config.hashAlgorithm),

ioBufferSize = config.ioBufferSize / config.numThreads / 2,

maxIoBufferSize = config.maxMessageSize,

dupBufferLoadFactor = config.dedupeBufferLoadFactor,

throttler = throttler,

time = time,

checkDone = checkDone)

@volatile var lastStats: CleanerStats = new CleanerStats()

@volatile var lastPreCleanStats: PreCleanStats = new PreCleanStats()

....

}

我们先来看下其初始化 Cleaner 对象的重要参数：

1.  **offsetMap**：是一个 SkimpyOffsetMap 类型的对象，是为 dirty 部分的消息建立 key 与 last\_offset 的映射关系。
2.  **ioBufferSize**： 读写 LogSegment 的 byteBuffer 的大小。
3.  **maxIoBufferSizeSegment**：消息的最大长度。
4.  **dupBufferLoadFactor**：指定了 SkimpyOffsetMap 的最大占用比率。
5.  **throttler**：用来限制读写 LogSegment 的速度。
6.  **checkDone**：检查 Log 的压缩状态。

通过继承类传值可以看到该线程类的启动名称为：**kafka-log-cleaner-thread-$threadId**。其中 $threadId 是线程id。

private\[log\] class CleanerThread(threadId: Int)

extends ShutdownableThread(name = s"kafka-log-cleaner-thread-$threadId", isInterruptible = false) {

/\*\*

\* 核心方法

\* The main loop for the cleaner thread

\* Clean a log if there is a dirty log available, otherwise sleep for a bit

\*/

override def doWork(): Unit = {

val cleaned \= tryCleanFilthiestLog()

if (!cleaned)

pause(config.backOffMs, TimeUnit.MILLISECONDS)

}

private def checkDone(topicPartition: TopicPartition): Unit = {

if (!isRunning)

throw new ThreadShutdownException

cleanerManager.checkCleaningAborted(topicPartition)

}

/\*\*

\* Cleans a log if there is a dirty log available

\* @return whether a log was cleaned

\*/

private def tryCleanFilthiestLog(): Boolean = {

try {

cleanFilthiestLog()

} catch {

case e: LogCleaningException =>

warn(s"Unexpected exception thrown when cleaning log ${e.log}. Marking its partition (${e.log.topicPartition}) as uncleanable", e)

cleanerManager.markPartitionUncleanable(e.log.parentDir, e.log.topicPartition)

false

}

}

@throws(classOf\[LogCleaningException\])

private def cleanFilthiestLog(): Boolean = {

val preCleanStats \= new PreCleanStats()

//1、获取需要进行日志压缩的 log

val cleaned \= cleanerManager.grabFilthiestCompactedLog(time, preCleanStats) match {

case None \=\>

false

case Some(cleanable) =>

// there's a log, clean it

this.lastPreCleanStats = preCleanStats

try {

// 2、核心方法 进行清理

cleanLog(cleanable)

true

} catch {

case e @ (\_: ThreadShutdownException | \_: ControlThrowable) => throw e

case e: Exception => throw new LogCleaningException(cleanable.log, e.getMessage, e)

}

}

// 3、清理待删除日志

val deletable: Iterable\[(TopicPartition, Log)\] = cleanerManager.deletableLogs()

try {

deletable.foreach { case (\_, log) =>

try {

// 删除日志段文件

log.deleteOldSegments()

} catch {

case e @ (\_: ThreadShutdownException | \_: ControlThrowable) => throw e

case e: Exception => throw new LogCleaningException(log, e.getMessage, e)

}

}

} finally {

// 当删除任务完成后，无论是成功还是失败，都需要将该任务的状态通知给 cleanerManager，以便它可以将占用的资源释放掉。

cleanerManager.doneDeleting(deletable.map(\_.\_1))

}

cleaned

}

// 用来后台异步执行日志清理任务

private def cleanLog(cleanable: LogToClean): Unit = {

// 获取需要清理的日志文件中第一个脏数据的偏移量。

val startOffset \= cleanable.firstDirtyOffset

// 将其赋值给需要清理的日志文件的最后一个偏移量。

var endOffset \= startOffset

try {

// 使用 cleaner 对指定日志文件进行清理，并返回下一个未清理的脏数据的偏移量及清理统计信息。

val (nextDirtyOffset, cleanerStats) = cleaner.clean(cleanable)

// 将最后一个脏数据的偏移量更新为下一个未清理的脏数据的偏移量。

endOffset = nextDirtyOffset

// 记录清理统计信息。

recordStats(cleaner.id, cleanable.log.name, startOffset, endOffset, cleanerStats)

} catch {

case \_: LogCleaningAbortedException => // task can be aborted, let it go.

case \_: KafkaStorageException => // partition is already offline. let it go.

case e: IOException => // 如果发生输入输出错误，则将相应的目录标记为离线，并在日志中记录错误信息。

val logDirectory \= cleanable.log.parentDir

val msg \= s"Failed to clean up log for ${cleanable.topicPartition} in dir $logDirectory due to IOException"

logDirFailureChannel.maybeAddOfflineLogDir(logDirectory, msg, e)

} finally {

// 当清理任务完成后，无论是成功还是失败，都需要将该任务的状态通知给 cleanerManager，以便它可以将占用的资源释放掉。

cleanerManager.doneCleaning(cleanable.topicPartition, cleanable.log.parentDirFile, endOffset)

}

}

}

线程类执行 doWork() 方法，通过追踪代码，最后调用 cleanFilthiestLog()，其步骤如下：

1.  通过 LogCleanerManager#grabFilthiestCompactedLog 收集需要进行日志压缩清理的 log。
2.  调用开始初始化的 Cleaner对象中的 clean 方法进行压缩清理。
3.  清理待删除日志。
4.  通知给 cleanerManager 进行扫尾回收。

这里我们重点看下 「**第一步**」、「**第二步**」。

##   
**3.2.1 收集需要日志压缩的 Log**

/\*\*

\* Choose the log to clean next and add it to the in-progress set. We recompute this

\* each time from the full set of logs to allow logs to be dynamically added to the pool of logs

\* the log manager maintains.

\*/

def grabFilthiestCompactedLog(time: Time, preCleanStats: PreCleanStats = new PreCleanStats()): Option\[LogToClean\] = {

inLock(lock) {

// 当前时间

val now \= time.milliseconds

// 监控指标 记录 cleaner 线程跑的时间

this.timeOfLastRun = now

val lastClean \= allCleanerCheckpoints

// 1、计算脏日志信息

val dirtyLogs \= logs.filter {

// 1.1、过滤掉 cleaner.policy 不是 compact 的 log

case (\_, log) => log.config.compact // match logs that are marked as compacted

}.filterNot {

case (topicPartition, log) =>

// 1.2、过滤掉已经 in-progress 集合中的不需要清理的 log

inProgress.contains(topicPartition) || isUncleanablePartition(log, topicPartition)

}.map {

case (topicPartition, log) => // create a LogToClean instance for each

try {

val lastCleanOffset \= lastClean.get(topicPartition)

// 1.3、计算出 firstDirtyOffset，firstDirtyOffset 的值可能是 logStartOffset 也可能是 clean checkpoint

val offsetsToClean \= cleanableOffsets(log, lastCleanOffset, now)

// update checkpoint for logs with invalid checkpointed offsets

if (offsetsToClean.forceUpdateCheckpoint)

// 1.4、如果需要强制更新检查点文件则进行更新

updateCheckpoints(log.parentDirFile, partitionToUpdateOrAdd = Option(topicPartition, offsetsToClean.firstDirtyOffset))

val compactionDelayMs \= maxCompactionDelay(log, offsetsToClean.firstDirtyOffset, now)

preCleanStats.updateMaxCompactionDelay(compactionDelayMs)

// 1.5、为每个 Log 创建一个 LogToClean 对象，维护了每个 Log 的 clean 部分的字节数、dirty部分字节数 以及 cleanableRatio。

LogToClean(topicPartition, log, offsetsToClean.firstDirtyOffset, offsetsToClean.firstUncleanableDirtyOffset, compactionDelayMs > 0)

} catch {

case e: Throwable => throw new LogCleaningException(log,

s"Failed to calculate log cleaning stats for partition $topicPartition", e)

}

}.filter(ltc => ltc.totalBytes > 0) // skip any empty logs

// 2、获取 dirtyLogs 集合中 cleanableRatio 的最大值

this.dirtiestLogCleanableRatio = if (dirtyLogs.nonEmpty) dirtyLogs.max.cleanableRatio else 0

// and must meet the minimum threshold for dirty byte ratio or have some bytes required to be compacted

// 3、过滤掉 cleanableRatio 小于配置的 log

val cleanableLogs \= dirtyLogs.filter { ltc =>

(ltc.needCompactionNow && ltc.cleanableBytes > 0) || ltc.cleanableRatio > ltc.log.config.minCleanableRatio

}

if(cleanableLogs.isEmpty) {

None

} else {

// 4、选择要压缩的日志，加入 inProgress 集合中

preCleanStats.recordCleanablePartitions(cleanableLogs.size)

val filthiest \= cleanableLogs.max

inProgress.put(filthiest.topicPartition, LogCleaningInProgress)

Some(filthiest)

}

}

}

该方法主要用来**收集需要日志压缩的 log 并加入到 inProgress 集合中**。步骤如下：

1.  加锁处理，防止并发。
2.  计算脏日志信息。
3.  过滤掉 [cleaner.policy](http://cleaner.policy/) 不是 compact 的 log。
4.  过滤掉已经 in-progress 集合中的不需要清理的 log
5.  针对分区 log 计算出 firstDirtyOffset，firstDirtyOffset 的值可能是 logStartOffset 也可能是 clean checkpoint。
6.  如果需要强制更新检查点文件则进行更新。
7.  为每个 Log 创建一个 LogToClean 对象，维护了每个 Log 的 clean 部分的字节数、dirty部分字节数 以及 cleanableRatio。
8.  获取 dirtyLogs 集合中 cleanableRatio 的最大值。
9.  选择要压缩的日志，加入 inProgress 集合中。

这里我们来看下如何计算 firstDirtyOffset 信息的源码。

## **3.2.1.1 cleanableOffsets()**

// 用于获取可以清理的日志文件偏移量范围。

// 三个参数：需要清理的日志文件 log，最后一次清理的偏移量 lastCleanOffset，以及当前时间戳 now

def cleanableOffsets(log: Log, lastCleanOffset: Option\[Long\], now: Long): OffsetsToClean = {

// 获取第一个脏数据的偏移量 firstDirtyOffset 及是否需要更新检查点标识 forceUpdateCheckpoint。

val (firstDirtyOffset, forceUpdateCheckpoint) = {

// 获取日志文件的起始偏移量。

val logStartOffset \= log.logStartOffset

// 获取上一次清理结束时所记录的偏移量。如果该值不存在，则使用日志文件的起始偏移量。

val checkpointDirtyOffset \= lastCleanOffset.getOrElse(logStartOffset)

// 如果上一次清理结束时记录的值小于日志文件的起始偏移量，则认为检查点无效，重置第一个脏数据的偏移量为起始偏移量。

if (checkpointDirtyOffset < logStartOffset) {

if (!isCompactAndDelete(log))

warn(s"Resetting first dirty offset of ${log.name} to log start offset $logStartOffset " + s"since the checkpointed offset $checkpointDirtyOffset is invalid.")

(logStartOffset, true)

// 如果上一次清理结束时记录的值大于日志文件的结尾偏移量，则暂时认为整个日志文件需要被清理。

} else if (checkpointDirtyOffset > log.logEndOffset) {

warn(s"The last checkpoint dirty offset for partition ${log.name} is $checkpointDirtyOffset, " + s"which is larger than the log end offset ${log.logEndOffset}. Resetting to the log start offset $logStartOffset.")

(logStartOffset, true)

} else {

// 否则，使用上一次清理结束时的偏移量作为第一个脏数据的偏移量。

(checkpointDirtyOffset, false)

}

}

// 获取最小压缩延迟时间（得到配置文件中更大的数和0，防止小于0）。

val minCompactionLagMs \= math.max(log.config.compactionLagMs, 0L)

// find first segment that cannot be cleaned

// neither the active segment, nor segments with any messages closer to the head of the log than the minimum compaction lag time

// may be cleaned

// 创建一个序列，包含了需要跳过的日志段。

val firstUncleanableDirtyOffset: Long = Seq(

// we do not clean beyond the first unstable offset

// 首先，跳过第一个不稳定的偏移量。

log.firstUnstableOffset,

// the active segment is always uncleanable

// 其次，跳过当前活跃日志段。

Option(log.activeSegment.baseOffset),

// the first segment whose largest message timestamp is within a minimum time lag from now

// 如果最小压缩延迟时间大于零，则需要跳过具有时间戳在最小压缩延迟时间之内的脏数据段。

if (minCompactionLagMs > 0) {

// dirty log segments

// 获取从第一个脏数据偏移量开始的所有脏数据日志段。

val dirtyNonActiveSegments \= log.nonActiveLogSegmentsFrom(firstDirtyOffset)

// 在脏数据日志段中查找第一个其最大消息时间戳小于当前时间减去最小压缩延迟时间的日志段，然后将其基础偏移量返回。如果不存在这样的日志段，则返回 None 值。

dirtyNonActiveSegments.find { s =>

val isUncleanable \= s.largestTimestamp > now - minCompactionLagMs

debug(s"Checking if log segment may be cleaned: log='${log.name}' segment.baseOffset=${s.baseOffset} " +

s"segment.largestTimestamp=${s.largestTimestamp}; now - compactionLag=${now - minCompactionLagMs}; " +

s"is uncleanable=$isUncleanable")

isUncleanable

}.map(\_.baseOffset)

} else None

).flatten.min // 如果存在多个需要跳过的日志段，选择最小基础偏移量的日志段。

debug(s"Finding range of cleanable offsets for log=${log.name}. Last clean offset=$lastCleanOffset " +

s"now=$now => firstDirtyOffset=$firstDirtyOffset firstUncleanableOffset=$firstUncleanableDirtyOffset " +

s"activeSegment.baseOffset=${log.activeSegment.baseOffset}")

// 创建一个 OffsetsToClean 对象，封装了可以清理的偏移量范围并返回

OffsetsToClean(firstDirtyOffset, math.max(firstDirtyOffset, firstUncleanableDirtyOffset), forceUpdateCheckpoint)

}

该方法主要通过根据不同情况来返回 firstDirtyOffset 和 forceUpdateCheckpoint 以及需要清理的偏移量范围。

## **3.2.2 进行日志压缩操作**

这里是调用 [cleaner.clean](http://cleaner.clean/)(cleanable) 进行压缩清理，来看下其源码：

/\*\*

\* Clean the given log

\* @param cleanable The log to be cleaned

\* @return The first offset not cleaned and the statistics for this round of cleaning

\*/

private\[log\] def clean(cleanable: LogToClean): (Long, CleanerStats) = {

// figure out the timestamp below which it is safe to remove delete tombstones

// this position is defined to be a configurable time beneath the last modified time of the last clean segment

// 1、计算可以安全删除 ".delete" 的 logSegment (即 value 为空的消息)

val deleteHorizonMs \=

cleanable.log.logSegments(0, cleanable.firstDirtyOffset).lastOption match {

case None \=\> 0L

case Some(seg) => seg.lastModified - cleanable.log.config.deleteRetentionMs

}

// 2、执行清理工作

doClean(cleanable, deleteHorizonMs)

}

private\[log\] def doClean(cleanable: LogToClean, deleteHorizonMs: Long): (Long, CleanerStats) = {

info("Beginning cleaning of log %s.".format(cleanable.log.name))

val log \= cleanable.log

// 清理状态

val stats \= new CleanerStats()

// build the offset map

info("Building offset map for %s...".format(cleanable.log.name))

// 1、确认日志压缩的上限，因为 activeSegment不参与日志压缩，所以可以确定日志压缩的最大上限是 activeSegment.baseOffset。

val upperBoundOffset \= cleanable.firstUncleanableOffset

// 2、填充 offsetMap，确定日志压缩的真正上限

buildOffsetMap(log, cleanable.firstDirtyOffset, upperBoundOffset, offsetMap, stats)

// 计算结束位移

val endOffset \= offsetMap.latestOffset + 1

// 计入完成时间

stats.indexDone()

// determine the timestamp up to which the log will be cleaned

// this is the lower of the last active segment and the compaction lag

// 3、计算出将被清理到的时间戳，这里你可以和 upperBoundOffset 一起理解。 一个是 offset 一个是timestamp。

val cleanableHorizonMs \= log.logSegments(0, cleanable.firstUncleanableOffset).lastOption.map(\_.lastModified).getOrElse(0L)

// group the segments and clean the groups

info("Cleaning log %s (cleaning prior to %s, discarding tombstones prior to %s)...".format(log.name, new Date(cleanableHorizonMs), new Date(deleteHorizonMs)))

val transactionMetadata \= new CleanedTransactionMetadata

// 4、对要压缩的 Segment 进行分区，并且进行分组进行 clean

val groupedSegments \= groupSegmentsBySize(log.logSegments(0, endOffset), log.config.segmentSize,

log.config.maxIndexSize, cleanable.firstUncleanableOffset)

for (group <- groupedSegments)

// 5、循环清理

cleanSegments(log, group, offsetMap, deleteHorizonMs, stats, transactionMetadata)

// record buffer utilization

stats.bufferUtilization = offsetMap.utilization

// 计入结束时间

stats.allDone()

(endOffset, stats)

}

其步骤如下：

1.  确认日志压缩的上限，因为 activeSegment不参与日志压缩，所以可以确定日志压缩的最大上限是 activeSegment.baseOffset。
2.  填充 offsetMap，确定日志压缩的真正上限。
3.  计算出将被清理到的时间戳，这里你可以和 upperBoundOffset 一起理解。 一个是 offset 一个是timestamp。
4.  对要压缩的 Segment 进行分区，并且进行分组进行 clean。
5.  循环清理。

这里我们重点看下 「**第二步**」、「**第四步**」和「**第五步**」。

先来看下第二步：填充 OffsetMap 的流程。

##   
**3.2.2.1 填充 OffsetMap**

/\*\*

\* Build a map of key\_hash => offset for the keys in the cleanable dirty portion of the log to use in cleaning.

\* @param log The log to use

\* @param start The offset at which dirty messages begin

\* @param end The ending offset for the map that is being built

\* @param map The map in which to store the mappings

\* @param stats Collector for cleaning statistics

\*/

private\[log\] def buildOffsetMap(log: Log,

start: Long,end: Long,map: OffsetMap,stats: CleanerStats): Unit = {

map.clear()

// 1、查找从 firstDirtyOffset 至 upperBoundOffset 所有的 LogSegment

val dirty \= log.logSegments(start, end).toBuffer

val nextSegmentStartOffsets \= new ListBuffer\[Long\]

if (dirty.nonEmpty) {

for (nextSegment <- dirty.tail) nextSegmentStartOffsets.append(nextSegment.baseOffset)

nextSegmentStartOffsets.append(end)

}

info("Building offset map for log %s for %d segments in offset range \[%d, %d).".format(log.name, dirty.size, start, end))

// 事务有关

val transactionMetadata \= new CleanedTransactionMetadata

val abortedTransactions \= log.collectAbortedTransactions(start, end)

transactionMetadata.addAbortedTransactions(abortedTransactions)

// Add all the cleanable dirty segments. We must take at least map.slots \* load\_factor,

// but we may be able to fit more (if there is lots of duplication in the dirty section of the log)

var full \= false

// 2、遍历 dirty 集合，循环条件是 offsetMap 未被填满

for ((segment, nextSegmentStartOffset) <- dirty.zip(nextSegmentStartOffsets) if !full) {

// 检查 LogCleanerManager 记录的该分区的压缩状态

checkDone(log.topicPartition)

// 处理单个 logSegment，将消息的 key 和 offset 添加到 OffsetMap 中

full = buildOffsetMapForSegment(log.topicPartition, segment, map, start, nextSegmentStartOffset, log.config.maxMessageSize,

transactionMetadata, stats)

if (full)

debug("Offset map is full, %d segments fully mapped, segment with base offset %d is partially mapped".format(dirty.indexOf(segment), segment.baseOffset))

}

info("Offset map for log %s complete.".format(log.name))

}

// 用来在给定日志段 LogSegment 中构建偏移量-键值对映射表

private def buildOffsetMapForSegment(topicPartition: TopicPartition, // 主题分区

segment: LogSegment, // 待构建映射表的日志段

map: OffsetMap, // 映射表

startOffset: Long, // 起始偏移量

nextSegmentStartOffset: Long, // 下一日志段的起始偏移量

maxLogMessageSize: Int, // 最大日志消息大小

transactionMetadata: CleanedTransactionMetadata, // 已清理事务的元数据

stats: CleanerStats): Boolean = { //清理统计信息

// 1、获取给定偏移量在索引中的位置。需要注意的是，offsetIndex 就是 LogSegment 类的一个成员变量，表示偏移量索引。该变量的数据类型是 OffsetIndex，保存了一组有序的偏移量向量，用于方便地查询特定偏移量所在的文件位置。

var position \= segment.offsetIndex.lookup(startOffset).position

// 2、计算目标映射表大小。map.slots 表示映射表的槽数，this.dupBufferLoadFactor 是一个常量，用于设置映射表占用缓冲区空间的最大比例。两者相乘，将得到目标映射表大小。

val maxDesiredMapSize \= (map.slots \* this.dupBufferLoadFactor).toInt

// 3、遍历 LogSegment

while (position < segment.log.sizeInBytes) {

// 4、检查当前线程是否被中断，如果是则抛出异常 LogCleaningAbortedException。

checkDone(topicPartition)

// 5、清空读缓冲区，准备读入数据。

readBuffer.clear()

try {

// 6、从 LogSegment 中读取数据到缓冲区 readBuffer 中。

segment.log.readInto(readBuffer, position)

} catch {

case e: Exception =>

throw new KafkaException(s"Failed to read from segment $segment of partition $topicPartition " +

"while loading offset map", e)

}

// 7、将缓冲区 readBuffer 中的字节数据解析为多条 MemoryRecords 记录。

val records \= MemoryRecords.readableRecords(readBuffer)

// 8、尝试限制数据读取速率。

throttler.maybeThrottle(records.sizeInBytes)

// 9、记录解析数据前的起始位置。

val startPosition \= position

// 10、循环处理 MemoryRecords 记录中的数据包 RecordBatch。

for (batch <- records.batches.asScala) {

// 如果数据包是一种控制数据包，则将其提交给 transactionMetadata 处理，并记录一条清理统计信息。

if (batch.isControlBatch) {

transactionMetadata.onControlBatchRead(batch)

stats.indexMessagesRead(1)

} else {

// 将该数据范围中所有的消息提交给 transactionMetadata 进一步处理，并记录是否存在事务被清理的标记。

val isAborted \= transactionMetadata.onBatchRead(batch)

if (isAborted) { // 如果存在被终止的事务，则跳过整个数据范围，并记录一条清理统计信息

// If the batch is aborted, do not bother populating the offset map.

// Note that abort markers are supported in v2 and above, which means count is defined.

stats.indexMessagesRead(batch.countOrNull)

} else {

// 获取当前数据包中所有消息的迭代器。

val recordsIterator \= batch.streamingIterator(decompressionBufferSupplier)

try {

// 按顺序处理每条消息。

for (record <- recordsIterator.asScala) {

// 只处理有key的消息，且该消息的偏移量如果大于等于待处理的起始偏移量 startOffset，则将其加入到映射表中。

if (record.hasKey && record.offset >= startOffset) {

// 如果当前映射表大小未超过目标大小

if (map.size < maxDesiredMapSize)

map.put(record.key, record.offset)

else

// 表示映射表已经达到目标大小或超过，返回 true，表示构建偏移量映射表的过程已经结束。

return true

}

// 记录一条处理消息的统计信息。

stats.indexMessagesRead(1)

}

} finally recordsIterator.close()

}

}

// 记录最后一个被处理的消息偏移量，以便下一次继续处理时跳过这些已处理的消息。

if (batch.lastOffset >= startOffset)

map.updateLatestOffset(batch.lastOffset)

}

// 11、记录该 MemoryRecords 记录中所有消息占据的字节数。

val bytesRead \= records.validBytes

// 更新当前读取到的数据的结束位置。

position += bytesRead

// 更新处理字节数量的统计信息。

stats.indexBytesRead(bytesRead)

// 12、如果 position 没有移动表示没有读取到一个完整的 message， 则对读写缓冲区进行扩容

if(position == startPosition)

growBuffersOrFail(segment.log, position, maxLogMessageSize, records)

}

// 13、在处理完当前数据包中的所有消息后，执行跳过该日志段的所有空洞。

map.updateLatestOffset(nextSegmentStartOffset - 1L)

// 14、清空所有的缓冲区资源，以便后续重复使用

restoreBuffers()

false

}

}

该方法步骤比较多，如下：

1.  获取给定偏移量在索引中的位置。需要注意的是，offsetIndex 就是 LogSegment 类的一个成员变量，表示偏移量索引。该变量的数据类型是 OffsetIndex，保存了一组有序的偏移量向量，用于方便地查询特定偏移量所在的文件位置。
2.  计算目标映射表大小。[map.slots](http://map.slots/) 表示映射表的槽数，this.dupBufferLoadFactor 是一个常量，用于设置映射表占用缓冲区空间的最大比例。两者相乘，将得到目标映射表大小。
3.  遍历 LogSegment。
4.  检查当前线程是否被中断，如果是则抛出异常 LogCleaningAbortedException。
5.  清空读缓冲区，准备读入数据。
6.  从 LogSegment 中读取数据到缓冲区 readBuffer 中。
7.  将缓冲区 readBuffer 中的字节数据解析为多条 MemoryRecords 记录。
8.  尝试限制数据读取速率。
9.  记录解析数据前的起始位置。
10.  循环处理 MemoryRecords 记录中的数据包 RecordBatch。
11.  记录该 MemoryRecords 记录中所有消息占据的字节数,更新当前读取到的数据的结束位置。
12.  如果 position 没有移动表示没有读取到一个完整的 message， 则对读写缓冲区进行扩容。
13.  在处理完当前数据包中的所有消息后，执行跳过该日志段的所有空洞。
14.  清空所有的缓冲区资源，以便后续重复使用。

接着看下分组流程。

## **3.2.2.2 对日志段分组**

// 用于根据指定的最大尺寸和索引尺寸，将日志分段，并为每个段创建索引。

private\[log\] def groupSegmentsBySize(segments: Iterable\[LogSegment\], maxSize: Int, maxIndexSize: Int, firstUncleanableOffset: Long): List\[Seq\[LogSegment\]\] = {

// 1、初始化分组结果列表。

var grouped \= List\[List\[LogSegment\]\]() // 2、将迭代器转换成列表 List，以便后续处理。

var segs \= segments.toList

// 3、循环处理每个日志段。

while(segs.nonEmpty) {

// 初始化每个日志段对应的分组列表，将当前日志段加入到列表中。

var group \= List(segs.head)

// 初始化每个分组的字节数，将当前日志段大小加入到字节数中。

var logSize \= segs.head.size.toLong

// 初始化每个分组索引的字节数，将当前日志段索引大小加入到索引字节数中。

var indexSize \= segs.head.offsetIndex.sizeInBytes.toLong

// 初始化每个分组时间索引的字节数，将当前日志段时间索引大小加入到时间索引字节数中。

var timeIndexSize \= segs.head.timeIndex.sizeInBytes.toLong

// 将当前日志段从列表中移除，以便继续处理下一个日志段。

segs = segs.tail

// 循环处理下一个日志段，将符合条件的日志段加入到当前分组中。条件包括：分组的总字节数未达到阈值 maxSize，分组的索引总字节数未达到阈值 maxIndexSize，分组时间索引总字节数未达到阈值maxIndexSize，当前分组的最大偏移量与待加入的日志段的起始偏移量距离不超过整型最大值。

while(segs.nonEmpty &&

logSize + segs.head.size <= maxSize &&

indexSize + segs.head.offsetIndex.sizeInBytes <= maxIndexSize &&

timeIndexSize + segs.head.timeIndex.sizeInBytes <= maxIndexSize &&

lastOffsetForFirstSegment(segs, firstUncleanableOffset) - group.last.baseOffset <= Int.MaxValue) {

group = segs.head :: group // 将当前日志段加入到分组列表的最前面。

logSize += segs.head.size // 将当前日志段的大小加入到字节数中。

indexSize += segs.head.offsetIndex.sizeInBytes // 将当前日志段的索引大小加入到索引字节数中。

timeIndexSize += segs.head.timeIndex.sizeInBytes // 将当前日志段的时间索引大小加入到时间索引字节数中。

segs = segs.tail // 将当前日志段从列表中移除，以便下一次继续处理。

}

grouped ::= group.reverse // 将当前分组添加到分组列表的最前面，并倒序排列，以让最新的分组排在列表顶部。

}

// 最后再将整个分组列表倒序排列，以使最旧的分组排在列表顶部。

grouped.reverse

}

其步骤如下：

1.  初始化分组结果列表。
2.  将迭代器转换成列表 List，以便后续处理。
3.  循环处理每个日志段做分组处理：
4.  初始化每个日志段对应的分组列表，将当前日志段加入到列表中。
5.  初始化每个分组的字节数，将当前日志段大小加入到字节数中。
6.  初始化每个分组索引的字节数，将当前日志段索引大小加入到索引字节数中。
7.  初始化每个分组时间索引的字节数，将当前日志段时间索引大小加入到时间索引字节数中。
8.  将当前日志段从列表中移除，以便继续处理下一个日志段。
9.  循环处理下一个日志段，将符合条件的日志段加入到当前分组中。
10.  将当前分组添加到分组列表的最前面，并倒序排列，以让最新的分组排在列表顶部。
11.  最后再将整个分组列表倒序排列，以使最旧的分组排在列表顶部。

## **3.2.2.3 清理日志段**

// 将日志的多个段进行清理，并写入新的日志段中。

private\[log\] def cleanSegments(log: Log,

segments: Seq\[LogSegment\],

map: OffsetMap,

deleteHorizonMs: Long,

stats: CleanerStats,

transactionMetadata: CleanedTransactionMetadata): Unit = {

// create a new segment with a suffix appended to the name of the log and indexes

// 1、创建 ".clean" 后缀的日志文件和索引文件，文件名是分组中第一个 logSegment 的 baseOffset

val cleaned \= LogCleaner.createNewCleanedSegment(log, segments.head.baseOffset)

// 记录已清理段的事务元数据索引 cleaned.txnIndex

transactionMetadata.cleanedIndex = Some(cleaned.txnIndex)

try {

// clean segments into the new destination segment

// 2、创建迭代器，迭代待清理的日志段。

val iter \= segments.iterator

var currentSegmentOpt: Option\[LogSegment\] = Some(iter.next())

val lastOffsetOfActiveProducers \= log.lastRecordsOfActiveProducers

while (currentSegmentOpt.isDefined) {

// 获取当前日志段。

val currentSegment \= currentSegmentOpt.get

// 获取下一个日志段（如果有的话）

val nextSegmentOpt \= if (iter.hasNext) Some(iter.next()) else None

// 获取当前日志段的起始偏移量。

val startOffset \= currentSegment.baseOffset

// 获取下一个日志段的起始偏移量，如果不存在下一个日志段，则取当前日志段的最大偏移量 + 1，作为上限偏移量。

val upperBoundOffset \= nextSegmentOpt.map(\_.baseOffset).getOrElse(map.latestOffset + 1)

// 获取从当前日志段的起始偏移量到上限偏移量范围内的已中止事务列表。

val abortedTransactions \= log.collectAbortedTransactions(startOffset, upperBoundOffset)

// 将已中止事务添加到事务元数据中。

transactionMetadata.addAbortedTransactions(abortedTransactions)

// 根据当前日志段的修改时间戳与删除时间戳 deleteHorizonMs 的比较结果，确定是否保留删除标志和事务标记。如果当前日志段的修改时间戳晚于删除时间戳，则不进行删除。

val retainDeletesAndTxnMarkers \= currentSegment.lastModified > deleteHorizonMs

info(s"Cleaning $currentSegment in log ${log.name} into ${cleaned.baseOffset} " +

s"with deletion horizon $deleteHorizonMs, " +

s"${if(retainDeletesAndTxnMarkers) "retaining" else "discarding"} deletes.")

try {

// 3、进行日志压缩操作

cleanInto(log.topicPartition, currentSegment.log, cleaned, map, retainDeletesAndTxnMarkers, log.config.maxMessageSize,

transactionMetadata, lastOffsetOfActiveProducers, stats)

} catch {

case e: LogSegmentOffsetOverflowException =>

// Split the current segment. It's also safest to abort the current cleaning process, so that we retry from

// scratch once the split is complete.

info(s"Caught segment overflow error during cleaning: ${e.getMessage}")

log.splitOverflowedSegment(currentSegment)

throw new LogCleaningAbortedException()

}

// 将下一个日志段设置为当前日志段，并继续循环。

currentSegmentOpt = nextSegmentOpt

}

// 4、标记已清理的日志段 cleaned 为不活跃的段。

cleaned.onBecomeInactiveSegment()

// flush new segment to disk before swap

// 5、执行flush 操作，将数据刷新到磁盘上

cleaned.flush()

// update the modification date to retain the last modified date of the original files

// 7、更新最后的修改时间

val modified \= segments.last.lastModified

cleaned.lastModified = modified

// swap in new segment

info(s"Swapping in cleaned segment $cleaned for segment(s) $segments in log $log")

// 将.clean 后缀改为 .swap 后缀

// 将 cleaned 对象加入到 segments 中

// 将分组中的 logSegment 从 segments 中删除

// 最后将文件的 .swap 后缀删除

log.replaceSegments(List(cleaned), segments)

} catch {

case e: LogCleaningAbortedException =>

// 删除之前创建的新日志段 cleaned，并记录异常。

try cleaned.deleteIfExists()

catch {

case deleteException: Exception =>

e.addSuppressed(deleteException)

} finally throw e

}

}

步骤如下：

1.  创建 ".clean" 后缀的日志文件和索引文件，文件名是分组中第一个 logSegment 的 baseOffset。
2.  创建迭代器，迭代待清理的日志段。
3.  进行日志压缩操作。
4.  标记已清理的日志段 cleaned 为不活跃的段。
5.  执行 flush 操作，将数据刷新到磁盘上。
6.  更新最后的修改时间。
7.  替换原始的待清理日志段列表 segments，将其替换为清理后的新日志段 cleaned。同步更新偏移量映射表中的值，保证新旧数据的一致性。

这里重点看下「**第三步**」。

##   
**3.2.2.4 日志压缩操作**

private\[log\] def cleanInto(topicPartition: TopicPartition, // 要清理的日志段所属的主题以及分区。

sourceRecords: FileRecords, // 源日志段，从中读取需要清理的记录。

dest: LogSegment, // 目标日志段，要将需要保留的记录写入其中。

map: OffsetMap, // 负责跟踪源日志段中记录的偏移量的 OffsetMap。

retainDeletesAndTxnMarkers: Boolean, // 是否在清理过程中保留删除记录和事务记录。

maxLogMessageSize: Int, // 日志消息的最大大小。

transactionMetadata: CleanedTransactionMetadata, // 清理过程中用于记录事务元数据的 CleanedTransactionMetadata。

lastRecordsOfActiveProducers: Map\[Long, LastRecord\], // 各个活跃的生产者最后一条记录的偏移量的映射表。

stats: CleanerStats): Unit = { // 清理器统计数据。

val logCleanerFilter: RecordFilter = new RecordFilter {

var discardBatchRecords: Boolean = \_

override def checkBatchRetention(batch: RecordBatch): BatchRetention = {

discardBatchRecords = shouldDiscardBatch(batch, transactionMetadata, retainTxnMarkers = retainDeletesAndTxnMarkers)

def isBatchLastRecordOfProducer: Boolean = {

// We retain the batch in order to preserve the state of active producers. There are three cases:

// 1) The producer is no longer active, which means we can delete all records for that producer.

// 2) The producer is still active and has a last data offset. We retain the batch that contains

// this offset since it also contains the last sequence number for this producer.

// 3) The last entry in the log is a transaction marker. We retain this marker since it has the

// last producer epoch, which is needed to ensure fencing.

lastRecordsOfActiveProducers.get(batch.producerId).exists { lastRecord =>

lastRecord.lastDataOffset match {

case Some(offset) => batch.lastOffset == offset

case None \=\> batch.isControlBatch && batch.producerEpoch == lastRecord.producerEpoch

}

}

}

if (batch.hasProducerId && isBatchLastRecordOfProducer)

BatchRetention.RETAIN\_EMPTY

else if (discardBatchRecords)

BatchRetention.DELETE

else

BatchRetention.DELETE\_EMPTY

}

override def shouldRetainRecord(batch: RecordBatch, record: Record): Boolean = {

// 调用上面的 checkBatchRetention 方法判断是否要丢弃。

if (discardBatchRecords)

// The batch is only retained to preserve producer sequence information; the records can be removed

false

else

// 是否保存这个消息，有三个条件

// 1、此消息是否含有 key

// 2、offsetMap中是否有相同的 key

// 3、value 不为空 或者 value 为空但是现在不可以删除。

Cleaner.this.shouldRetainRecord(map, retainDeletesAndTxnMarkers, batch, record, stats)

}

}

var position \= 0

// 遍历待压缩的 LogSegment

while (position < sourceRecords.sizeInBytes) {

// 检查压缩状态

checkDone(topicPartition)

// read a chunk of messages and copy any that are to be retained to the write buffer to be written out

readBuffer.clear()

writeBuffer.clear()

// 读取消息

sourceRecords.readInto(readBuffer, position)

val records \= MemoryRecords.readableRecords(readBuffer)

// 是否限制读取速率

throttler.maybeThrottle(records.sizeInBytes)

// 过滤结果

val result \= records.filterTo(topicPartition, logCleanerFilter, writeBuffer, maxLogMessageSize, decompressionBufferSupplier)

stats.readMessages(result.messagesRead, result.bytesRead)

stats.recopyMessages(result.messagesRetained, result.bytesRetained)

position += result.bytesRead

// if any messages are to be retained, write them out

val outputBuffer \= result.outputBuffer

if (outputBuffer.position() > 0) {

outputBuffer.flip()

val retained \= MemoryRecords.readableRecords(outputBuffer)

// 添加到目标 logsegment 中

dest.append(largestOffset = result.maxOffset,

largestTimestamp = result.maxTimestamp,

shallowOffsetOfMaxTimestamp = result.shallowOffsetOfMaxTimestamp,

records = retained)

throttler.maybeThrottle(outputBuffer.limit())

}

// if we read bytes but didn't get even one complete batch, our I/O buffer is too small, grow it and try again

// \`result.bytesRead\` contains bytes from \`messagesRead\` and any discarded batches.

if (readBuffer.limit() > 0 && result.bytesRead == 0)

// 未读取一个完整的消息，表示 readBuffer 过小，需要扩容

growBuffersOrFail(sourceRecords, position, maxLogMessageSize, records)

}

// 重置 readBuffer 和 writeBuffer

restoreBuffers()

}

/\*\*

\* Restore the I/O buffer capacity to its original size

\*/

def restoreBuffers(): Unit = {

if(this.readBuffer.capacity > this.ioBufferSize)

this.readBuffer = ByteBuffer.allocate(this.ioBufferSize)

if(this.writeBuffer.capacity > this.ioBufferSize)

this.writeBuffer = ByteBuffer.allocate(this.ioBufferSize)

}

该方法会读取一块源日志段中的记录，并使用提供的过滤器 logCleanerFilter 进行过滤，将需要保留的记录写入目标日志段中。这一过程会不断重复，直到源日志段中的所有记录都被清理完毕。

##   
**04 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头对「**LogManager**」启动时的后台周期性任务和清理线程进行了梳理。

2、带你挨个剖析了「**后台周期性任务**」的源码设计与实现。

3、接着带你深度剖析了「**Cleaner**」清理线程的源码设计与实现，让你大致了解了「**日志清理的工作流程**」。

下篇我们来深度剖析「**Broker 启动如何组成集群**」，大家期待，我们下期见。