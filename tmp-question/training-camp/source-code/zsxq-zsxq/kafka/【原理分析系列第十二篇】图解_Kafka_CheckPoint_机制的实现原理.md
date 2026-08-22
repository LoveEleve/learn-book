大家好，我是 **华仔**, 又跟大家见面了。

我们都知道 MySQL 中，有 checkpoint 机制，主要作用就是用来将脏页从内存刷新到磁盘中，从今天开始我们讲解 **Kafka CheckPoint 机制的实现原理**，来了解下它在 Kafka 中的应用。

## **01 总体概述**

在 Kafka 中，也是存在多个位移需要进行 checkpoint 的，比如：「**log-start-offset-checkpoint**」、「**recovery-point-offset-checkpoint**」、「**replication-offset-checkpoint**」、「**cleaner-offset-checkpoint**」等。这些位移的 checkpoint 对于「**快速恢复**」、「**副本管理**」等方面非常重要。同时，这些位移也需要与其他元数据信息一起进行 checkpoint，以确保 Kafka 集群的稳定性和可靠性。

如下图所示，在 Kafka 日志目录下，有这 4 种 checkpoint。

![](https://article-images.zsxq.com/FpMwUQED8pletCIBYCrZLBMuX7aQ)

接下来，我们挨个给大家深度剖析下这几个 checkpoint 的实现原理。

##   
**02 log-start-offset-checkpoint**

## **2.1 简介**

在讲解该 checkpoint 之前，首先我们先来了解下什么是 「**Log Start Offset**」，它表示当前 Partition 的最早消息的位移，也可以理解为 Partition 的起始位置。

##   
**2.2 使用场景**

在 Kafka 的「 **消息持久化机制**」中，消息会被写入到批量的 「**Log Segment**」文件中。为了保证消息能够被及时消费，Kafka 需要实时跟踪每个 Partition 的最早消息的 Offset，这个位置就是「**Log Start Offset**」。它的实时更新保证了消费者可以快速获取 Partition 上的新消息。

## **2.3 实现原理**

Kafka 会将该 Offset 保存在 log 日志目录下的 **log-start-offset-checkpoint** 文件中进行 checkpoint，以确保不会丢失这个关键信息，当 Broker 重启或者一个新的 Replica 加入时，它会从 **log-start-offset-checkpoint** 中读取每个分区的 Log Start Offset，然后从这个位移开始恢复消息。这使得 Kafka 能够保证消费者能够从正确的位移处开始消费数据。如下图所示：

![](https://article-images.zsxq.com/FjKyq-glmKWPazyAfed4Y2GHkvDm)

  
![](https://article-images.zsxq.com/FjwSKBY-WLZlEdkiamwQX00jAB4N)

该 Offset 的 checkpoint 机制是由日志清理「**Log Compaction**」来控制的。在进行日志清理之前，Kafka 首先会将「**Log Start Offset**」进行 checkpoint，只有在 checkpoint 成功完成后才进行日志清理。这样才可以保证一旦在日志清理过程中遇到异常时，Kafka 可以快速恢复到上一次 checkpoint 的「**Log Start Offset**」位置。

## **2.4 源码实现**

github 源码地址：[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogManager.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogManager.scala)

val InitialTaskDelayMs \= 30 \* 1000

/\*\* 启动后台线程处理刷新日志操作

\* LogManager.scala

\*/

def startup(): Unit = {

/\* Schedule the cleanup task to delete old logs \*/

if (scheduler != null) {

info("Starting log cleanup with a period of %d ms.".format(retentionCheckMs))

....

scheduler.schedule("kafka-log-start-offset-checkpoint",

checkpointLogStartOffsets \_, // 调用该方法写入 checkpoint

delay = InitialTaskDelayMs, // 执行延迟时间

period = flushStartOffsetCheckpointMs, // 执行周期时间

TimeUnit.MILLISECONDS)

....

}

}

/\*\*

\* 该方法用来进行检查点日志的起始偏移量

\* Write out the current log start offset for all logs to a text file in the log directory

\* to avoid exposing data that have been deleted by DeleteRecordsRequest

\*/

def checkpointLogStartOffsets(): Unit = {

// logsByDirCached 变量对所有目录下的搜索进行缓存

val logsByDirCached \= logsByDir

// 遍历当前活动的日志目录，并对每个目录下的所有日志进行检查点日志的起始偏移量操作。

liveLogDirs.foreach { logDir =>

// logsInDir 函数表示要在指定目录下的所有分区日志进行查找

// 遍历过程中将持久化所有日志的起始偏移量，以便在从头开始检查点时恢复使用，并确保启动不会修改现有检查点文件。

checkpointLogStartOffsetsInDir(logDir, logsInDir(logsByDirCached, logDir))

}

}

/\*\*

\* Checkpoint log start offsets for all the provided logs in the provided directory.

\* 该方法用于对指定目录中的日志起始偏移量进行 checkpoint 操作。

\* @param logDir the directory in which logs are checkpointed 表示指定的目录。

\* @param logsToCheckpoint the logs to be checkpointed 表示需要checkpoint的一组日志，是一个键值对的集合，键是分区，值是对应分区的日志对象。

\*/

private def checkpointLogStartOffsetsInDir(logDir: File, logsToCheckpoint: Map\[TopicPartition, Log\]): Unit = {

try {

// logStartOffsetCheckpoints 表示 Kafka 中用来存储 checkpoint 信息的内存数据结构，通过 get 方法获取到指定目录的 checkpoint。

logStartOffsetCheckpoints.get(logDir).foreach { checkpoint =>

// logStartOffsets 用来存储需要 checkpoint 的日志的起始偏移量，是一个键值对的集合，键是分区，值是对应分区的日志的起始偏移量。

val logStartOffsets \= logsToCheckpoint.collect {

// 通过迭代 logsToCheckpoint 集合，返回其中符合条件的元素组成的集合。这里只需要返回logStartOffset 大于对应日志头部（log.logSegments.head.baseOffset）的偏移量的那些分区及其日志对象的起始偏移量。

case (tp, log) if log.logStartOffset > log.logSegments.head.baseOffset => tp -> log.logStartOffset

}

// 将得到的起始偏移量写入到checkpoint中。

checkpoint.write(logStartOffsets)

}

} catch {

case e: KafkaStorageException =>

error(s"Disk error while writing log start offsets checkpoint in directory $logDir: ${e.getMessage}")

}

}

## **03 recovery-point-offset-checkpoint**

## **3.1 简介**

在讲解该 checkpoint 之前，首先我们先来了解下什么是「**Recovery Point Offset**」，它指的是 Broker 在日志裁剪「**Log Segment Trim**」之后应当停止恢复的位置，也就是当前 Partition 的最后一个完整消息的位移。

## **3.2 使用场景**

由于「**Log Segment**」文件中的消息**不能无限制**地保存，因此 Kafka 需要定期将「**旧的 Log Segment**」删除以释放磁盘空间。而在删除「**旧的 Log Segment**」的过程中，需要保证在异常情况下，Kafka Broker 还能够快速恢复到上一次「**正常**」的工作状态，此时就需要使用到「**Recovery Point Offset**」。

## **3.3 实现原理**

每当 Log Segment 被裁剪「**Trim**」时，「**Recovery Point Offset**」都会被更新，它是指当前 Partition 的最后一个完整消息的位移，也就是 Broker 在**日志裁剪之后应当停止恢复的位置**。为了保证 「**Recovery Point Offset**」可靠地被持久化，Kafka 会将其存储在 **recovery-point-offset-checkpoint** 文件中，并定期进行checkpoint 操作。

这种 checkpoint 机制可以确保在异常情况下，Kafka Broker 可以重新从上一次 checkpoint 的 「**Recovery Point Offset**」位置进行恢复，从而保证消息不会丢失，如下图所示：

  
![](https://article-images.zsxq.com/FrhIcOe-CSISYciP-6Er22sB4a-v)

![](https://article-images.zsxq.com/FiAc8d_4lWePs22mbmY9oVdpfBq4)

看这个图中，topic ：message 有一条 recovery 位移、message2、message3 有2条 recovery 位移，对应的分区如下图，剩余的都是 \_\_consumer\_offsets 的位移。

![](https://article-images.zsxq.com/FqA551MChH-6az_4801KBvdMBNvn)

## **3.4 源码实现**

github 源码地址：[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogManager.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogManager.scala)

val InitialTaskDelayMs \= 30 \* 1000

/\*\* 启动后台线程处理刷新日志操作

\* LogManager.scala

\*/

def startup(): Unit = {

/\* Schedule the cleanup task to delete old logs \*/

if (scheduler != null) {

info("Starting log cleanup with a period of %d ms.".format(retentionCheckMs))

....

scheduler.schedule("kafka-recovery-point-checkpoint",

checkpointLogRecoveryOffsets \_, // 调用该方法写入 checkpoint

delay = InitialTaskDelayMs, // 执行延迟时间

period = flushRecoveryOffsetCheckpointMs, // 执行周期时间

TimeUnit.MILLISECONDS)

....

}

....

}

/\*\*

\* 该方法用来进行检查点日志恢复偏移量。

\* Write out the current recovery point for all logs to a text file in the log directory

\* to avoid recovering the whole log on startup.

\*/

def checkpointLogRecoveryOffsets(): Unit = {

// logsByDirCached 变量对所有目录下的搜索进行缓存

val logsByDirCached \= logsByDir

// 遍历当前活动的日志目录，并对每个目录下的所有日志进行检查点日志恢复偏移量操作和清理快照

liveLogDirs.foreach { logDir =>

// logsToCheckpoint 变量则表示具有相同目录的相同主题分区的日志。

// logsInDir 函数表示要在指定目录下的所有分区日志进行查找。

val logsToCheckpoint \= logsInDir(logsByDirCached, logDir)

// 遍历过程中将使用检查点日志记录的偏移量恢复分区，并在完成后删除旧的快照。

checkpointRecoveryOffsetsAndCleanSnapshotsInDir(logDir, logsToCheckpoint, logsToCheckpoint.values.toSeq)

}

}

/\*\*

\* 该方法是在指定的目录下进行恢复偏移量的检查点操作，并清理快照文件。

\* Checkpoint recovery offsets for all the provided logs and clean the snapshots of all the

\* provided logs.

\* @param logDir the directory in which the logs are

\* @param logsToCheckpoint the logs to be checkpointed 表示需要检查点操作的分区日志。

\* @param logsToCleanSnapshot the logs whose snapshots will be cleaned 表示需要清理快照文件的分区日志列表。

\*/

private def checkpointRecoveryOffsetsAndCleanSnapshotsInDir(logDir: File, logsToCheckpoint: Map\[TopicPartition, Log\],logsToCleanSnapshot: Seq\[Log\]): Unit = {

try {

// 首先遍历检查指定目录下的恢复点检查点

recoveryPointCheckpoints.get(logDir).foreach { checkpoint =>

// 如果存在恢复点检查点,则将日志分区对应的恢复偏移量写入该检查点，完成对恢复点偏移量的持久化工作。

val recoveryOffsets \= logsToCheckpoint.map { case (tp, log) => tp -> log.recoveryPoint }

checkpoint.write(recoveryOffsets)

}

// 遍历需要清理快照文件的分区日志列表，并调用 deleteSnapshotsAfterRecoveryPointCheckpoint 方法来清理它们的所有快照。

logsToCleanSnapshot.foreach(\_.deleteSnapshotsAfterRecoveryPointCheckpoint())

} catch {

// 如果在写入恢复偏移量检查点时发生了磁盘错误，则会在日志中打印相应的警告信息，并在 logDirFailureChannel 中将该目录上报为离线目录。

case e: KafkaStorageException =>

error(s"Disk error while writing recovery offsets checkpoint in directory $logDir: ${e.getMessage}")

case e: IOException =>

logDirFailureChannel.maybeAddOfflineLogDir(logDir.getAbsolutePath,

s"Disk error while writing recovery offsets checkpoint in directory $logDir: ${e.getMessage}", e)

}

}

## **04 replication-offset-checkpoint**

## **4.1 简介**

在讲解该 checkpoint 之前，首先我们先来了解下什么是「**Replication Offset**」，它指的是在进行**数据复制时**的位移。

## **4.2 使用场景**

在 Kafka 中，实现了分布式的**副本机制来确保消息的可靠性**，每个 Partition 都能够在多个 Broker 之间进行副本同步。在副本同步的过程中，需要保证数据的准确性和可靠性，因此 Kafka 需要使用 「**Replication Offset**」来持久化每次副本同步时的位置信息。

## **4.3 实现原理**

在 Kafka 集群中的每个 Broker 都会向其他 Broker 发送副本同步请求，并分别接收其他 Broker 发送过来的同步请求。当副本同步成功之后，当前 Broker 会**增加**其本机的「**Replication Offset**」值。为确保同步的可靠性，Kafka 会将每个 Broker 对其他 Broker 的副本**同步信息存储**到 Zookeeper 节点上，Zookeeper 提供了可靠的持久化存储服务。同时为了**实现故障恢复功能**，Kafka 会将「**Replication Offset**」存储到内置的 「**\_\_consumer\_offsets**」这个 Kafka 内置的 topic 中，以支持消费者确认在 Broker 正常切换后，每个 Partition 的「**Replication Offset**」信息，以实现 checkpoint 机制，如下图所示：

  
![](https://article-images.zsxq.com/FqCTyji_u6GzGSjR_vwYpHz207HY)

![](https://article-images.zsxq.com/Ft2ngqc3ZljjvZU9zzBT_LR167Rw)

## **4.4 源码实现**

// 在 Kafka 中，副本高水位是指已经复制到消费者的最新消息的偏移量，它与消费者的消费位置相关。因此，需要用文件来记录和恢复副本高水位。

// 记录副本高水位的检查点文件名

val HighWatermarkFilename \= "replication-offset-checkpoint"

// 首先，logManager.liveLogDirs 返回一个包含所有存活的日志目录的列表。

// map 方法会遍历每个日志目录，并创建一个 (目录路径, 检查点文件对象) 的元组，其中

// OffsetCheckpointFile 是一个辅助类，用于读写检查点文件。

// 最后，toMap 方法将所有元组组成的集合转换为一个类似于字典的映射，其中键为目录路径，值为检查点文

// 件对象。这个映射存储在 highWatermarkCheckpoints 变量中，并且被声明为 @volatile，因为可能存在多

// 个线程同时更新它。

@volatile var highWatermarkCheckpoints: Map\[String, OffsetCheckpointFile\] = logManager.liveLogDirs.map(dir =>

(dir.getAbsolutePath, new OffsetCheckpointFile(new File(dir, ReplicaManager.HighWatermarkFilename), logDirFailureChannel))).toMap

// 实现了启动高水位检查点线程的函数

  
def startHighWatermarkCheckPointThread(): Unit = {

// 首先判断高水位检查点线程是否已经启动，如果未启动，则执行以下操作：

if (highWatermarkCheckPointThreadStarted.compareAndSet(false, true))

// 使用 scheduler 调度一个任务，该任务的名称为 highwatermark-checkpoint，执行的函数为 checkpointHighWatermarks，调度的执行周期为 config.replicaHighWatermarkCheckpointIntervalMs 毫秒。

// compareAndSet 方法是用来原子性地设置 highWatermarkCheckPointThreadStarted 变量的值为 true，避免多个线程同时启动高水位检查点线程。

scheduler.schedule("highwatermark-checkpoint", checkpointHighWatermarks \_, period = config.replicaHighWatermarkCheckpointIntervalMs, unit = TimeUnit.MILLISECONDS)

}

## **05 cleaner-offset-checkpoint**

## **5.1 简介**

在讲解该 checkpoint 之前，首先我们先来了解下什么是「**Cleaner Offset**」，它表示**已经被清理的日志段**的offset 位移值。

## **5.2 使用场景**

在 Kafka 中，该 checkpoint 主要被用在**已 Kafka 的日志清理场景**。在 Kafka 的日志中，**消息是被一段段地进行存储的**。当消息被消费之后，Kafka **会将该段日志标记为已经删除**。在日志中，这些已经删除的段被称为

「**已经压缩的日志段 cleaned segment**」。

## **5.3 实现原理**

**cleaner-offset-checkpoint** 采用了与 **Consumer Offset** 类似的方式，使用者 offset 的方式记录已经被清理的日志段的offset值。

在这个过程中，Kafka 会将已经压缩的日志段的 offset 记录在 **cleaner-offset-checkpoint** 文件中。**cleaner-offset-checkpoint** 文件中记录的是**已经被清理掉的 Offset**，在读取消息的时候，可以读到这个文件，从而得知哪些日志段已经被清理掉，哪些日志段还需要存储。这些 offset 后面的日志段已经不包含任何消息，可以被删除。

在 Kafka 的日志清理机制中，Kafka 会将多个**已经压缩的日志段合并成一个新的日志段**。这个新的日志段由**多个老的日志段经过压缩之后生成的**。因为这些老的日志段已经被清理掉了，因此需要将它们记录在**cleaner-offset-checkpoint** 文件中，**以便下次再做清理的时候可以知道哪些已经压缩的日志段需要被合并**。

在清理的过程中，Kafka 保证了日志的顺序性。在 **cleaner-offset-checkpoint** 文件中，记录的是以删除 offset 为起点的后面每一个位置已经被删除的日志段的起始位置，如下图所示，我这个目前没有被清理的数据：

![](https://article-images.zsxq.com/Fhq1SAGN0aUH-1KcKkuKQGLhLEHQ)

## **5.4 源码实现**

github 源码地址：[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogCleanerManager.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogCleanerManager.scala)

// package-private for testing 定义 offsetCheckpointFile 变量，表示偏移量检查点文件的文件名。

private\[log\] val offsetCheckpointFile \= "cleaner-offset-checkpoint"

/\* the offset checkpoints holding the last cleaned point for each log \*/

// 定义偏移量检查点的变量 checkpoints，是一个Map，将每个日志目录（logDirs）与其相应的偏移量检查点文件对应起来。具体实现中，遍历每个日志目录，使用 OffsetCheckpointFile 对象创建对应的偏移量检查点文件，并将其加入到 checkpoints Map 中。同时，该变量也被标记为 @volatile，表示多线程并发访问时保证其可见性。

@volatile private var checkpoints \= logDirs.map(dir =>

(dir, new OffsetCheckpointFile(new File(dir, offsetCheckpointFile), logDirFailureChannel))).toMap

/\*\*

\* 根据时间和日志的属性，查找需要进行清理的日志，并返回需要进行清理的 LogToClean 对象的 Option。

\* time 时间，用于计算上次清理时间和是否需要进行清理。

\* preCleanStats 用于收集日志清理的统计信息，可以用默认值

\* Choose the log to clean next and add it to the in-progress set. We recompute this

\* each time from the full set of logs to allow logs to be dynamically added to the pool of logs

\* the log manager maintains.

\*/

def grabFilthiestCompactedLog(time: Time, preCleanStats: PreCleanStats = new PreCleanStats()): Option\[LogToClean\] = {

// 加锁，防止多线程同时清理同一个日志

inLock(lock) {

val now \= time.milliseconds // 获取当前时间戳

this.timeOfLastRun = now // 更新上次清理时间

val lastClean \= allCleanerCheckpoints // 获取上次清理的日志偏移量

val dirtyLogs \= logs.filter { // 获取需要清理的日志

case (\_, log) => log.config.compact // 选择被标记为需要压缩的日志 match logs that are marked as compacted

}.filterNot {

case (topicPartition, log) =>

// skip any logs already in-progress and uncleanable partitions

// 跳过已经在进行中的日志和无法清理的分区

inProgress.contains(topicPartition) || isUncleanablePartition(log, topicPartition)

}.map {

case (topicPartition, log) => // create a LogToClean instance for each 为每个日志创建一个LogToClean 实例

try {

val lastCleanOffset \= lastClean.get(topicPartition) // 获取上次清理的偏移量

val offsetsToClean \= cleanableOffsets(log, lastCleanOffset, now) // 获取需要清理的偏移量

// 如果有需要更新的偏移量，更新日志偏移量 update checkpoint for logs with invalid checkpointed offsets

if (offsetsToClean.forceUpdateCheckpoint)

updateCheckpoints(log.parentDirFile, partitionToUpdateOrAdd = Option(topicPartition, offsetsToClean.firstDirtyOffset))

// 获取最大压缩延迟时间

val compactionDelayMs \= maxCompactionDelay(log, offsetsToClean.firstDirtyOffset, now)

// 更新统计信息

preCleanStats.updateMaxCompactionDelay(compactionDelayMs)

LogToClean(topicPartition, log, offsetsToClean.firstDirtyOffset, offsetsToClean.firstUncleanableDirtyOffset, compactionDelayMs > 0)

} catch {

// 处理清理日志异常

case e: Throwable => throw new LogCleaningException(log,

s"Failed to calculate log cleaning stats for partition $topicPartition", e)

}

}.filter(ltc => ltc.totalBytes > 0) // skip any empty logs 过滤掉空日志

// 更新最脏的日志可清理比率

this.dirtiestLogCleanableRatio = if (dirtyLogs.nonEmpty) dirtyLogs.max.cleanableRatio else 0

// and must meet the minimum threshold for dirty byte ratio or have some bytes required to be compacted

// 选择满足最小 \_dirty\_log\_ratio\_to\_clean 或需要压缩，并且有需要清理的字节码的日志

val cleanableLogs \= dirtyLogs.filter { ltc =>

(ltc.needCompactionNow && ltc.cleanableBytes > 0) || ltc.cleanableRatio > ltc.log.config.minCleanableRatio

}

if(cleanableLogs.isEmpty) { // 如果没有需要清理的日志，则直接返回None

None

} else {

preCleanStats.recordCleanablePartitions(cleanableLogs.size) // 更新统计信息

val filthiest \= cleanableLogs.max // 选择可清理比率最高的日志进行清理

inProgress.put(filthiest.topicPartition, LogCleaningInProgress) // 将需要清理的日志加入进行中的日志列表

Some(filthiest)

}

}

}

/\*\*

\* Update checkpoint file, adding or removing partitions if necessary.

\* 更新检查点文件（checkpoint file），根据需要添加或删除分区。

\* @param dataDir The File object to be updated 要更新的文件对象

\* @param partitionToUpdateOrAdd The \[TopicPartition, Long\] map data to be updated. pass "none" if doing remove, not add 要更新的 \[TopicPartition, Long\] map 数据。如果进行删除而不是添加，则传 none

\* @param topicPartitionToBeRemoved The TopicPartition to be removed 要删除的 TopicPartition

\*/

def updateCheckpoints(dataDir: File, partitionToUpdateOrAdd: Option\[(TopicPartition, Long)\] = None,partitionToRemove: Option\[TopicPartition\] = None): Unit = {

// 加锁，防止多线程同时更新同一个日志

inLock(lock) {

// 检查点文件存储在本地，其路径通过传入的 dataDir 参数指定获取检查点文件 map

val checkpoint \= checkpoints(dataDir)

if (checkpoint != null) {

try {

// 计算当前检查点

val currentCheckpoint \= checkpoint.read().filter { case (tp, \_) => logs.keys.contains(tp) }.toMap

// 如果匹配到则移除分区偏移量

var updatedCheckpoint \= partitionToRemove match {

case Some(topicPartion) => currentCheckpoint - topicPartion

case None \=\> currentCheckpoint

}

// 如果匹配到则更新或添加分区偏移量

updatedCheckpoint = partitionToUpdateOrAdd match {

case Some(updatedOffset) => updatedCheckpoint + updatedOffset

case None \=\> updatedCheckpoint

}

// 写入最终的更新检查点

checkpoint.write(updatedCheckpoint)

} catch {

// 如果有异常发生，该方法会抛出一个 KafkaStorageException 异常，在日志中输出错误信息

case e: KafkaStorageException =>

error(s"Failed to access checkpoint file ${checkpoint.file.getName} in dir ${checkpoint.file.getParentFile.getAbsolutePath}", e)

}

}

}

}