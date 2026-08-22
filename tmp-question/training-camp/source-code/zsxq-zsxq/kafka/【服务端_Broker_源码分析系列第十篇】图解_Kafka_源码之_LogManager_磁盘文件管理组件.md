大家好，我是 **华仔**, 又跟大家见面了。

上一篇中，主要带大家深度剖析了 「**Kafka 服务端源码 ReplicaManger 日志读写流程**」，通过「**场景驱动方式**」，现在消息被封装成批次请求已经从「**生产者**」发送到「**Broker**」，且被「**网络层**」所接收到并准备进行消息数据存储，从今天开始，我们来深度剖析 Kafka 日志系统的底层实现，这是日志系列第三篇，我们先来深度聊聊「**Kafka 服务端源码 LogManger 磁盘文件管理组件**」，看看 Kafka 服务端是如何真正存储消息数据的。

![](https://article-images.zsxq.com/FosBxuEKgk4wSgf19twZPuuO259X)

## **01 总体概述**

在上一篇中，我们 [【服务端 Broker 源码分析系列第九篇】图解 Kafka 源码之 ReplicaManager 日志读写流程](https://articles.zsxq.com/id_5dq174gc0wji.html) 引出了服务端日志存储的管理对象之 「**LogManager**」。

接下来我们来对 「**LogManager**」对象一探究竟，从名字上看就知道它是「**日志管理组件**」，其作用主要是负责管理一个 Broker 上的所有「**Log**」，主要功能包括 「**加载 Log**」、「**创建 Log**」、「**删除 Log**」、「**查询 Log**」，所有的读写操作都委托给 「**Log**」去完成，并且在「**LogManager**」初始化的时候启动一些 「**周期性后台任务**」完成日志处理相关的工作。主要是启动五个定时任务以及启动一个后台线程 Cleaner。

本篇主要讲解前 4 个部分，最后一个等后面篇章再详细剖析。

好了，下面开始剖析其源码实现，让你对整个日志管理有个总体的认知，深度剖析下其内部是如何进行日志管理的，整个流程是怎么样的？那么带着这些问题进入今天的正题。

整个日志管理的相关组件的调用关系图如下：

![](https://article-images.zsxq.com/FgVmpr3JOm-_1PVfCHJC94zu4_tC)

## **02 LogManager 启动**

在 Kafka 服务启动初始化即 「**kafkaServer#startup**」 启动时会初始化一个 「**LogManager#startup**」方法进行启动。该模块为 Kafka Broker 上其中的一个后台线程，用于日志的管理操作，完成包含「**日志删除**」、「**日志检查点写入文件**」等工作。KafkaServer 后面会单独文章讲解。

「**KafkaServer**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/KafkaServer.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/KafkaServer.scala)

class KafkaServer(val config: KafkaConfig, time: Time = Time.SYSTEM, threadNamePrefix: Option\[String\] = None,

kafkaMetricsReporters: Seq\[KafkaMetricsReporter\] = List()) extends Logging with KafkaMetricsGroup {

var logManager: LogManager = null

def startup(): Unit = {

try {

....

// 初始化一个线程池用于处理后台工作，该线程池的前缀为 kafka-scheduler-,具体工作线程为KafkaThread

kafkaScheduler = new KafkaScheduler(config.backgroundThreads)

kafkaScheduler.startup()

....

// 初始化 LogManager 对象，参数包括配置项、已下线的分区、ZooKeeper客户端、当前的Broker状态、调度器、时间对象、Broker主题状态和日志目录错误处理器

logManager = LogManager(config, initialOfflineDirs, zkClient, brokerState, kafkaScheduler, time, brokerTopicStats, logDirFailureChannel)

// 启动 LogManager 日志管理模块

logManager.startup()

...

} catch {

case e: Throwable =>

// 打印异常日志，标记状态变量

fatal("Fatal error during KafkaServer startup. Prepare to shutdown", e)

isStartingUp.set(false)

// 关闭服务器

shutdown()

throw e

}

}

}

在 「**kafkaServer**」启动过程中，构建一个**线程池**用于后台线程的执行。并将**该线程池**作为参数注入到「**LogManager**」中生成 logManager，初始化完毕之后调用 #startup 方法进行启动。

## **03 LogManager 初始化**

「**LogManager**」初始化的操作在构造函数中完成，首选会「**检查 Server 端配置的日志目录信息**」，然后会「**加载日志目录下的所有分区的日志**」。

「**LogManager**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogManager.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogManager.scala)

「**LogManager**」这个类文件里面定义了两个对象：

  
![](https://article-images.zsxq.com/Fu_UQXGqYZgylxu74M9wbyc7dGQj)

说明：scala 语法中，允许 scala 中包含同名的 class 和 object，这种用法称之为伴生（Companion）, class 对象称之为伴生类，和 java 语法中的类是一样的。而 object是一个单例对象，里面包含静态方法和变量，用java比喻时，object相当于java的utils工具类。

先来看下 object LogManager 单例对象的源码。

object LogManager {

// 检查点表示日志已经刷新到磁盘的位置，主要是用于数据恢复

val RecoveryPointCheckpointFile \= "recovery-point-offset-checkpoint"

val LogStartOffsetCheckpointFile \= "log-start-offset-checkpoint"

val ProducerIdExpirationCheckIntervalMs \= 10 \* 60 \* 1000

// 在 scala 中 该方法相当于构造函数，用于创建 LogManager 实例

def apply(config: KafkaConfig,

initialOfflineDirs: Seq\[String\],

zkClient: KafkaZkClient,

brokerState: BrokerState,

kafkaScheduler: KafkaScheduler,

time: Time,

brokerTopicStats: BrokerTopicStats,

logDirFailureChannel: LogDirFailureChannel): LogManager = {

// 将主配置转换为日志配置

val defaultProps \= KafkaServer.copyKafkaConfigToLog(config)

// 验证默认配置中的值，以使用正确的类型和最小/最大值替换设置的非法值

LogConfig.validateValues(defaultProps)

// 加载日志配置

val defaultLogConfig \= LogConfig(defaultProps)

// read the log configurations from zookeeper

// 从 ZooKeeper 中读取所有日志的配置，包括主题和分区的配置信息，并返回失败列表。

// 这个方法提供了直接到读取到的日志配置数据集的连续迭代，以及它们读取失败的各种错误。

val (topicConfigs, failed) = zkClient.getLogConfigs(

zkClient.getAllTopicsInCluster(),

defaultProps

)

if (!failed.isEmpty) throw failed.head.\_2

// 通过 LogCleaner 对象构建 LogCleanerConfig 对象

val cleanerConfig \= LogCleaner.cleanerConfig(config)

// 实例化 LogManager 对象

new LogManager(logDirs = config.logDirs.map(new File(\_).getAbsoluteFile),

initialOfflineDirs = initialOfflineDirs.map(new File(\_).getAbsoluteFile),

topicConfigs = topicConfigs,

initialDefaultConfig = defaultLogConfig,

cleanerConfig = cleanerConfig,

recoveryThreadsPerDataDir = config.numRecoveryThreadsPerDataDir,

flushCheckMs = config.logFlushSchedulerIntervalMs,

flushRecoveryOffsetCheckpointMs = config.logFlushOffsetCheckpointIntervalMs,

flushStartOffsetCheckpointMs = config.logFlushStartOffsetCheckpointIntervalMs,

retentionCheckMs = config.logCleanupIntervalMs,

maxPidExpirationMs = config.transactionalIdExpirationMs,

scheduler = kafkaScheduler,

brokerState = brokerState,

brokerTopicStats = brokerTopicStats,

logDirFailureChannel = logDirFailureChannel,

time = time)

}

}

**3.1 LogManager 参数与属性讲解**

log.dirs 目录是由 「**LogManager**」负责管理，「**LogManager**」在启动时会校验 log.dirs 配置。每个 log 目录下包含多个 topic 分区目录，每个 topic 分区目录由一个 Log 类对象对其进行管理，「**LogManager**」 会记录每个 topic 分区对象及其对应的 Log 之间的映射关系。

/\*\*

\* The entry point to the kafka log management subsystem. The log manager is responsible for log creation, retrieval, and cleaning.

\* All read and write operations are delegated to the individual log instances.

\*

\* The log manager maintains logs in one or more directories. New logs are created in the data directory

\* with the fewest logs. No attempt is made to move partitions after the fact or balance based on

\* size or I/O rate.

\*

\* A background thread handles log retention by periodically truncating excess log segments.

\*/

@threadsafe

class LogManager(logDirs: Seq\[File\],

initialOfflineDirs: Seq\[File\],

val topicConfigs: Map\[String, LogConfig\], // 一组主题和其对应的日志配置。注：此配置只在创建 LogManager 时使用，创建后不再更新。

val initialDefaultConfig: LogConfig, // 默认的日志配置

val cleanerConfig: CleanerConfig, // 清理策略相关配置

recoveryThreadsPerDataDir: Int, // 恢复线程数

val flushCheckMs: Long, // 日志刷盘间隔时间（毫秒）

val flushRecoveryOffsetCheckpointMs: Long, // 恢复偏移量检查点刷盘间隔时间（毫秒）

val flushStartOffsetCheckpointMs: Long, // 日志起始偏移量检查点刷盘间隔时间（毫秒）

val retentionCheckMs: Long, // 日志保留时间检查间隔时间（毫秒）

val maxPidExpirationMs: Int, // 事务分区id的有效时间

scheduler: Scheduler, // 调度器

val brokerState: BrokerState, // broker状态

brokerTopicStats: BrokerTopicStats, // broker主题状态

logDirFailureChannel: LogDirFailureChannel, // 日志目录错误信息处理器

time: Time) extends Logging with KafkaMetricsGroup {

import LogManager.\_

val LockFile \= ".lock"

val InitialTaskDelayMs \= 30 \* 1000 // 日志从磁盘加载和初始化的延迟时间（毫秒）

private val logCreationOrDeletionLock \= new Object // 创建或删除 Log 时的锁对象

private val currentLogs \= new Pool\[TopicPartition, Log\]() // 当前正在使用的日志池

private val futureLogs \= new Pool\[TopicPartition, Log\]() // 预备日志池

private val logsToBeDeleted \= new LinkedBlockingQueue\[(Log, Long)\]() // 待删除的日志队列

// 检查日志目录

private val \_liveLogDirs: ConcurrentLinkedQueue\[File\] = createAndValidateLogDirs(logDirs, initialOfflineDirs)

@volatile private var \_currentDefaultConfig \= initialDefaultConfig // 当前默认日志配置对象

@volatile private var numRecoveryThreadsPerDataDir \= recoveryThreadsPerDataDir // 恢复线程数

// 持有正在加载和初始化的分区对象，并且在配置更新后触发重新加载。参见KAFKA-8813以了解更多详情。

private\[log\] val partitionsInitializing \= new ConcurrentHashMap\[TopicPartition, Boolean\]().asScala

// 更新默认配置对象

def reconfigureDefaultLogConfig(logConfig: LogConfig): Unit = {

this.\_currentDefaultConfig = logConfig

}

// 获取当前默认配置对象

def currentDefaultConfig: LogConfig = \_currentDefaultConfig

// 获取当前所有的在线日志目录

def liveLogDirs: Seq\[File\] = {

if (\_liveLogDirs.size == logDirs.size)

logDirs

else

\_liveLogDirs.asScala.toBuffer

}

// 用于锁定每个目录结构的文件

private val dirLocks \= lockLogDirs(liveLogDirs)

// 每个数据目录都有一个检查点文件,存储这个数据目录下所有分区的检查点信息

@volatile private var recoveryPointCheckpoints \= liveLogDirs.map(dir =>

(dir, new OffsetCheckpointFile(new File(dir, RecoveryPointCheckpointFile), logDirFailureChannel))).toMap

// 在日志目录中存储日志起始偏移量的文件名

@volatile private var logStartOffsetCheckpoints \= liveLogDirs.map(dir =>

(dir, new OffsetCheckpointFile(new File(dir, LogStartOffsetCheckpointFile), logDirFailureChannel))).toMap

// 用于记录哪些分区应该在清理时被放置在哪个日志目录中

private val preferredLogDirs \= new ConcurrentHashMap\[TopicPartition, String\]()

....

// 加载所有的日志

loadLogs()

// 如果启用Cleaner，则创建LogCleaner对象，否则设为null

private\[kafka\] val cleaner: LogCleaner =

if (cleanerConfig.enableCleaner)

new LogCleaner(cleanerConfig, liveLogDirs, currentLogs, logDirFailureChannel, time = time)

else

null

// 监控已下线的日志目录数量

newGauge("OfflineLogDirectoryCount", () => offlineLogDirs.size)

// 监控日志目录的离线状态，并将其信息输出到 Graphite，以便后续监控使用

for (dir <- logDirs) {

newGauge("LogDirectoryOffline",

() => if (\_liveLogDirs.contains(dir)) 0 else 1,

Map("logDirectory" -> dir.getAbsolutePath))

}

.....

}

### **3.1.1 LogManager 参数**

我们来看下其重要参数字段：

1.  **logDirs**：log 目录集合，在 server.properties 配置文件中通过 [log.dirs](http://log.dirs/) 项指定的多个目录。每个log目录下可以创建多个分区目录，每个「**Log**」都有自己对应的目录，「**LogManager**」在创建「**Log**」 时会选择 「**Log**」 最少的 log目录创建「**Log**」。
2.  **topicConfigs**：topic 的一些信息，目前版本还是从 zk 中读取。Kafka 目前元数据存取以及 KafkaController选举都是通过zk的，但是 kafka 3.x 已经计划替换掉 zk 了，详情可以查看此连接 [KIP-500](https://cwiki.apache.org/confluence/display/KAFKA/KIP-500%3A+Replace+ZooKeeper+with+a+Self-Managed+Metadata+Quorum)。

// read the log configurations from zookeeper

val (topicConfigs, failed) = zkClient.getLogConfigs(

zkClient.getAllTopicsInCluster(),

defaultProps

)

1.  **initialDefaultConfig**：Kafka 关于 Log 的一些配置比如：分段大小，flush 间隔时间，日志清理的配置等。

// 将主配置转换为日志配置

val defaultProps \= KafkaServer.copyKafkaConfigToLog(config)

// 验证默认配置中的值，以使用正确的类型和最小/最大值替换设置的非法值

LogConfig.validateValues(defaultProps)

// 加载日志配置

val defaultLogConfig \= LogConfig(defaultProps)

1.  **cleanerConfig**：cleaner 线程的配置。
2.  **recoveryThreadsPerDataDir**：在 Kafka 启动的时候处理日志恢复，关闭的时候处理日志 flush 的线程数量。默认为1个，可以通过 [num.recovery.threads.per.data.dir](http://num.recovery.threads.per.data.dir/) 进行配置。
3.  **flushCheckMs**：从内存刷新到磁盘的间隔时间，通过 [log.flush.scheduler.interval.ms](http://log.flush.scheduler.interval.ms/) 进行配置，默认 Long.MaxValue。
4.  **flushRecoveryOffsetCheckpointMs**：更新日志恢复点的频率，通过 [log.flush.offset.checkpoint.interval.ms](http://log.flush.offset.checkpoint.interval.ms/) 进行配置，默认 60000ms。
5.  **flushStartOffsetCheckpointMs**：更新日志Log Start Offset 的频率，通过[log.flush.start.offset.checkpoint.interval.ms](http://log.flush.start.offset.checkpoint.interval.ms/) 进行配置，默认60000ms。
6.  **retentionCheckMs**：检查日志是否有过期的频率，通过 [log.retention.check.interval.ms](http://log.retention.check.interval.ms/) 进行配置，默认 5 \* 60 \* 1000Lms
7.  **maxPidExpiationMs**：transactional id 过期的时间，通过 [transactional.id.expiration.ms](http://transactional.id.expiration.ms/) 进行配置，默认7天。
8.  **scheduler**：定时器，用的是Java中的 ScheduledThreadPoolExecutor 对象
9.  **brokerState**：broker 的状态，有 NotRunning、Starting、RecoveringFromUncleanShutdown、RunningAsBroker、PendingControlledShutdown、BrokerShuttingDown 这几种状态。
10.  **brokerTopicStats**：broker 中 topic 的状态。
11.  **logDirFailureChannel**：访问日志目录失败的，会加入此对象中的 offlineLogDirs 和 offlineLogDirQueue。

// https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/LogDirFailureChannel.scala

class LogDirFailureChannel(logDirNum: Int) extends Logging {

private val offlineLogDirs \= new ConcurrentHashMap\[String, String\]

private val offlineLogDirQueue \= new ArrayBlockingQueue\[String\](logDirNum)

....

}

### **3.1.2 LogManager 属性**

我们来看下其重要属性字段：

1.  **InitialTaskDelayMs**：定时任务启动的延迟时间，val InitialTaskDelayMs = 30 \* 1000ms。
2.  **logCreationOrDeletionLock**：创建或删除 Log 时的锁对象。
3.  **currentLogs**：用于管理 TopicAndPartition 与 Log 之间的对应关系。使用的是 Kafka 自定义的 pool 类型对象，底层是 jdk 提供的 ConcurrentHashMap。

private val currentLogs \= new Pool\[TopicPartition, Log\]()

class Pool\[K,V\](valueFactory: Option\[K => V\] = None) extends Iterable\[(K, V)\] {

private val pool: ConcurrentMap\[K, V\] = new ConcurrentHashMap\[K, V\]

}

1.  **futureLogs**：用于存放 -future 后缀的日志，当用户想在相同的 broker 移动到另外一个 replica 中的时候会创建 -future 的日志。使用的是 Kafka 自定义的 pool 类型对象，底层是 jdk 提供的 ConcurrentHashMap。

private val futureLogs = new Pool\[TopicPartition, Log\]()

1.  **logsToBeDeleted**：需要被删除的日志的集合，使用 jdk 提供的 LinkedBlockingQueue。

private val logsToBeDeleted \= new LinkedBlockingQueue\[(Log, Long)\]()

1.  **partitionsInitializing**：用于解决 [KAFKA-8813](https://github.com/apache/kafka/pull/7305) 这个 bug。

Topic-Partition 下的 log 初始化有以下几个步骤

1、先从 zk 中拉取 log 的配置。

2、然后调用 LogManager.getOrCreateLog 方法来创建 log 对象。

3、最后注册 log 对象。

但是如果在第二步创建完 log 对象之后，如果用户修改了配置，那么这个配置将无法更新。

所以通过在 LogManager 初始化的时候，在 partitionsInitializing 集合中，插入 Topic-Partition = false。

如果有人修改了配置，那么该 Topic-Partition 的值设置为 true。

在初始化完结束后检查是否为 true 如果是的话，重新从 zk 中拉取配置，重新创建 log 对象。

1.  **\_liveLogDirs**：类型为 ConcurrentLinkedQueue\[File\]，log目录的集合。创建或者获取 [log.dirs](http://log.dirs/) 项指定的多个目录，必须没有重复，都是可读的，通过 ConcurrentLinkedQueue 存储。
2.  **dirLocks**：类型为 Seq\[FileLock\]，FileLock集合。用来在文件系统层面为每个 log 目录加文件锁。在LogManager 对象初始化时，就会将所有的log目录加锁。

private val dirLocks \= lockLogDirs(liveLogDirs)

/\*\*

\* Lock all the given directories

\* 锁定所有给定的目录

\*/

private def lockLogDirs(dirs: Seq\[File\]): Seq\[FileLock\] = {

// 对所有的目录进行遍历和处理

dirs.flatMap { dir =>

try {

// 在每个目录中创建文件锁对象 FileLock

val lock \= new FileLock(new File(dir, LockFile))

// 尝试获取锁资源，如果成功，则返回 Some(lock)

if (!lock.tryLock())

throw new KafkaException("Failed to acquire lock on file .lock in " + lock.file.getParent + ". A Kafka instance in another process or thread is using this directory.")

Some(lock)

} catch {

// 如果出现异常，则将该目录的异常添加到通道，以便将来进行重试

case e: IOException =>

logDirFailureChannel.maybeAddOfflineLogDir(dir.getAbsolutePath, s"Disk error while locking directory $dir", e)

None

}

}

}

1.  **recoveryPointCheckpoints**： 类型为 Map\[File,OffsetCheckpoint\]。在 LogManager 对象初始化时，会在每个目录下创建一个对应的 RecoveryPointCheckpoint 文件。此 map 的 value 是 offsetCheckpoint 对象。其中封装了对应 log 目录下的 RecoveryPointCheckpoint 文件，并提供对 RecoveryPointCheckpoint 文件的读写操作。Recovery 文件中则记录了该log目录下的所有 Log 的 recoveryPoint 。
2.  **logStartOffsetCheckpoints**：用于管理每个log目录与其下的 LogStartOffsetCheckpoint 文件之间的映射关系，Map(File,OffsetCheckpointFile) 类型。
3.  **perferredLogDirs**：用于管理每个log目录与其下的 preferredLog 之间的映射关系
4.  **cleaner**：cleaner 线程。
5.  **scheduler**：KafkaScheduler 对象，用于执行周期任务的线程池。

## **04 LogManager 重要方法**

接下来我们来看看该类的重要方法。「**LogManager**」初始化主要调用了 **createAndValidateLogDirs()** 方法和**loadLogs()** 方法。初始化过程中还生成了一个 **Cleaner 后台线程**用于日志的**压缩清理**工作，这个部分本篇暂时不展开描述，后面会专门写一章来描述工作过程。下面来逐一进行跟踪其他初始化部分的源码，这里我们先从「**创建 Log**」、「**加载 Log**」目录开始分析。

## **4.1 createAndValidateLogDirs()**

/\*\*

\* 用于创建并验证日志目录的合法性

\*/

private def createAndValidateLogDirs(dirs: Seq\[File\], initialOfflineDirs: Seq\[File\]): ConcurrentLinkedQueue\[File\] = {

//1、初始化当前存在的日志目录集合，创建 ConcurrentLinkedQueue 对象，用于保存可用的日志目录

val liveLogDirs \= new ConcurrentLinkedQueue\[File\]()

// 用于保存日志目录的规范路径

val canonicalPaths \= mutable.HashSet.empty\[String\]

// 2、遍历所有传入的日志目录 如：log.dirs=/tmp/kafka-logs

for (dir <- dirs) {

try {

// liveLogDirs与offline日志目录不能有重叠情况，否则抛异常

if (initialOfflineDirs.contains(dir))

throw new IOException(s"Failed to load ${dir.getAbsolutePath} during broker startup")

// 3、如果日志目录不存在，则尝试创建目录

if (!dir.exists) {

info(s"Log directory ${dir.getAbsolutePath} not found, creating it.")

// 创建目录

val created \= dir.mkdirs()

if (!created)

throw new IOException(s"Failed to create data directory ${dir.getAbsolutePath}")

}

// 4、确保每个日志目录是个目录且有可读权限

if (!dir.isDirectory || !dir.canRead)

throw new IOException(s"${dir.getAbsolutePath} is not a readable log directory.")

// 获取目录的规范路径。

// 如果给出的 File 对象表示的路径包含符号链接，则此方法会解析符号链接后返回路径名

// 注意，如果一个文件系统查询失败或路径是无效的（例如包含空字符'\\0'），那么这个方法就会抛出 IOException 异常。

if (!canonicalPaths.add(dir.getCanonicalPath))

throw new KafkaException(s"Duplicate log directory found: ${dirs.mkString(", ")}")

// 5、把创建好的日志目录加到集合里。

liveLogDirs.add(dir)

} catch {

// 如果出现异常，则将该目录添加到通道，以便将来进行重试

case e: IOException =>

logDirFailureChannel.maybeAddOfflineLogDir(dir.getAbsolutePath, s"Failed to create or validate data directory ${dir.getAbsolutePath}", e)

}

}

// 6、如果没有可用的目录，则打印错误日志，退出程序。

if (liveLogDirs.isEmpty) {

fatal(s"Shutdown broker because none of the specified log dirs from ${dirs.mkString(", ")} can be created or validated")

Exit.halt(1)

}

// 7、返回可用目录的 liveLogDirs 集合

liveLogDirs

}

该方法比较简单，从名字看就知道用来**创建并验证日志目录的合法性的**，大体步骤如下：

1.  初始化当前存在的日志目录集合，创建 ConcurrentLinkedQueue 对象，用于保存可用的日志目录
2.  遍历所有传入的 Log 目录集合 如：[log.dirs=/tmp/kafka-logs](http://log.dirs=/tmp/kafka-logs)
3.  如果日志目录不存在，则尝试创建目录
4.  如果目录不是目录或者不可读，则抛出异常
5.  把创建好的日志目录加到集合里。
6.  如果没有可用的目录，则打印错误日志，退出程序。
7.  返回可用目录的 liveLogDirs 集合。

「**LogManager**」初始化时，通过 **createAndValidateLogDirs()** 方法进行创建和验证 **liveLogDir** 的正确性，要求 **liveLogDir** 不能有重复元素，且与 **OfflineDir** 不能有重叠，有可读权限。当目录不存在时则创建出来。

在**初始化完各种缓存信息**，**构建完各种目录结构**之后就调用 **loadLogs()** 方法载入日志，只有载入完毕之后，Broker 才能正常对外提供服务，后台的日志管理定时任务也才能开始进行工作，接下来我们来看看如何加载 Log 日志目录的？

##   
**4.2 loadLogs()**

private def loadLogs(): Unit = {

info(s"Loading logs from log dirs $liveLogDirs")

val startMs \= time.hiResClockMs()

// 1、创建线程池数组。

val threadPools \= ArrayBuffer.empty\[ExecutorService\]

val offlineDirs \= mutable.Set.empty\[(String, IOException)\]

val jobs \= mutable.Map.empty\[File, Seq\[Future\[\_\]\]\]

var numTotalLogs \= 0

// 2、遍历每个数据目录

for (dir <- liveLogDirs) {

val logDirAbsolutePath \= dir.getAbsolutePath

try {

// 3、对每个日志目录创建 numRecoveryThreadsPerDataDir 个线程组成的线程池，并加入threadPools 线程池组中，这里 numRecoveryThreadsPerDataDir 默认为 1。

val pool \= Executors.newFixedThreadPool(numRecoveryThreadsPerDataDir)

threadPools.append(pool)

// 4、检测上次的节点关闭是否是正常关闭。

val cleanShutdownFile \= new File(dir, Log.CleanShutdownFile)

// 5、检查 .kafka\_cleanshutdown 文件是否存在，存在则表示 kafka 正在做清理性的停机工作，此时跳过从本文件夹恢复日志

if (cleanShutdownFile.exists) {

info(s"Skipping recovery for all logs in $logDirAbsolutePath since clean shutdown file was found")

} else {

// log recovery itself is being performed by \`Log\` class during initialization

info(s"Attempting recovery for all logs in $logDirAbsolutePath since no clean shutdown file was found")

brokerState.newState(RecoveringFromUncleanShutdown)

}

var recoveryPoints \= Map\[TopicPartition, Long\]()

try {

// 6、加载每个日志目录的 recoveryPoints。

/\*\*

\* 从检查点文件读取 topic 对应的恢复点 offset 信息

\* 其文件为 recovery-point-offset-checkpoint

\* checkpoint file format:

\* line1 : version

\* line2 : expectedSize

\* nlines: (tp, offset)

\*/

recoveryPoints = this.recoveryPointCheckpoints(dir).read()

} catch {

case e: Exception =>

warn(s"Error occurred while reading recovery-point-offset-checkpoint file of directory " + s"$logDirAbsolutePath, resetting the recovery checkpoint to 0", e)

}

/\*\*

\* 7、加载每个日志目录的 logStartOffsetPoints。

\* 从检查点文件读取 topic 对应的 startoffset 信息

\* 其文件为 log-start-offset-checkpoint

\* checkpoint file 格式:

\* line1 : version

\* line2 : expectedSize

\* nlines: (tp, startoffset)

\*/

var logStartOffsets \= Map\[TopicPartition, Long\]()

try {

logStartOffsets = this.logStartOffsetCheckpoints(dir).read()

} catch {

case e: Exception =>

warn(s"Error occurred while reading log-start-offset-checkpoint file of directory " +

s"$logDirAbsolutePath, resetting to the base offset of the first segment", e)

}

// 8、要加载的日志文件夹。

val logsToLoad \= Option(dir.listFiles).getOrElse(Array.empty).filter(\_.isDirectory)

val numLogsLoaded \= new AtomicInteger(0)

numTotalLogs += logsToLoad.length

// 9、为每个日志子目录生成一个线程池的具体 job 任务，并提交到线程池中，其中每个 job 的主要任务是通过 loadLog() 方法加载日志

val jobsForDir \= logsToLoad.map { logDir =>

val runnable: Runnable = () => {

try {

debug(s"Loading log $logDir")

val logLoadStartMs \= time.hiResClockMs()

// 10、每个日志子目录都生成一个 job 任务去加载 log，创建 Log 对象。根据读取的recoveryPoints，logStartOffsets 进行加载，并把 log 对象加入到 logs 集合中。

val log \= loadLog(logDir, recoveryPoints, logStartOffsets)

val logLoadDurationMs \= time.hiResClockMs() - logLoadStartMs

val currentNumLoaded \= numLogsLoaded.incrementAndGet()

info(s"Completed load of $log with ${log.numberOfSegments} segments in ${logLoadDurationMs}ms " +

s"($currentNumLoaded/${logsToLoad.length} loaded in $logDirAbsolutePath)")

} catch {

case e: IOException =>

offlineDirs.add((logDirAbsolutePath, e))

error(s"Error while loading log dir $logDirAbsolutePath", e)

}

}

runnable

}

// 11、把 job 任务交给线程池进行处理,jobsForDir 是 List\[Runnable\] 类型

jobs(cleanShutdownFile) = jobsForDir.map(pool.submit)

} catch {

case e: IOException =>

offlineDirs.add((logDirAbsolutePath, e))

error(s"Error while loading log dir $logDirAbsolutePath", e)

}

}

try {

// 12、阻塞等待上面提交的日志加载任务执行完成，即等待所有 log 目录下 topic 分区对应的目录文件加载完成，删除对应的cleanShutdownFile

for ((cleanShutdownFile, dirJobs) <- jobs) {

dirJobs.foreach(\_.get)

try {

// 删除对应的 .kafka\_cleanshutdown 文件

cleanShutdownFile.delete()

} catch {

case e: IOException =>

offlineDirs.add((cleanShutdownFile.getParent, e))

error(s"Error while deleting the clean shutdown file $cleanShutdownFile", e)

}

}

offlineDirs.foreach { case (dir, e) =>

logDirFailureChannel.maybeAddOfflineLogDir(dir, s"Error while deleting the clean shutdown file in dir $dir", e)

}

} catch {

case e: ExecutionException =>

error(s"There was an error in one of the threads during logs loading: ${e.getCause}")

throw e.getCause

} finally {

// 13、遍历关闭线程池

threadPools.foreach(\_.shutdown())

}

info(s"Loaded $numTotalLogs logs in ${time.hiResClockMs() - startMs}ms.")

}

该方法主要用来**循环异步创建并加载日志目录数据的**，完成后删除 对应的 .kafka\_cleanshutdown 文件，大体步骤如下：

1.  创建线程池数组，这个线程组里的线程都是用来存放为每个 Log 目录保存的线程。即会为每个 Log 目录分配一个线程来维持 Log 目录。
2.  然后遍历每个数据目录进行加载。
3.  对每个日志目录创建 numRecoveryThreadsPerDataDir 个线程组成的线程池，并加入 threadPools 线程池组中，这里 numRecoveryThreadsPerDataDir 默认为 1。
4.  检测上次的节点关闭是否是正常关闭。
5.  检查 .kafka\_cleanshutdown 文件是否存在，存在则表示 kafka 正在做清理性的停机工作，此时跳过从本文件夹恢复日志，**循因为启动一个进程同时另外一个进程可能正在做关闭中的清理性工作，这样就不要再做恢复工作直接跳过**。
6.  加载每个日志目录的 recoveryPoints。
7.  加载每个日志目录的 logStartOffsets。
8.  要加载的日志文件夹。
9.  为每个日志子目录生成一个线程池的具体 job 任务，并提交到线程池中，其中每个 job 的主要任务是通过 loadLog() 方法加载日志。
10.  每个日志子目录都生成一个 job 任务去加载 log，创建 Log 对象。根据读取的 recoveryPoints，logStartOffsets 进行加载，并把 log 对象加入到 logs 集合中。
11.  把 job 任务交给线程池进行处理，jobsForDir 是 List\[Runnable\] 类型。
12.  阻塞等待上面提交的日志加载任务执行完成，即等待所有 log 目录下 topic 分区对应的目录文件加载完成，删除对应的cleanShutdownFile。
13.  最后遍历关闭线程池。

总体来说，该方法就是**根据已经存在的日志目录来创建 Log 类对象**，而且每个 Log 类对象都是**由一个线程异步去创建的**。之所以要用多线程去创建 log 对象是因为这样做能够**提升创建的速度，减少启动的耗时**。

下面来看看线程池里面的 job 任务是如何工作的？

## **4.3 loadLog()**

private def loadLog(logDir: File,

recoveryPoints: Map\[TopicPartition, Long\],

logStartOffsets: Map\[TopicPartition, Long\]): Log = {

// 解析日志目录的TopicPartition名称

val topicPartition \= Log.parseTopicPartitionName(logDir)

// 获取 Topic 的配置，如果存在则返回对应的配置，否则返回默认配置

val config \= topicConfigs.getOrElse(topicPartition.topic, currentDefaultConfig)

// 获取 Topic 的恢复点，如果存在则返回对应的恢复点，否则返回0

val logRecoveryPoint \= recoveryPoints.getOrElse(topicPartition, 0L)

// 获取Topic的日志起始偏移量，如果存在则返回对应的日志起始偏移量，否则返回0

val logStartOffset \= logStartOffsets.getOrElse(topicPartition, 0L)

// 1、初始化 Log 对象

val log \= Log(

dir = logDir, // 日志目录

config = config, // 配置

logStartOffset = logStartOffset,// 日志起始偏移量

recoveryPoint = logRecoveryPoint,// 恢复点

maxProducerIdExpirationMs = maxPidExpirationMs,// 最大生产者ID过期时间

producerIdExpirationCheckIntervalMs = LogManager.ProducerIdExpirationCheckIntervalMs,// 生产者ID过期检查时间间隔

scheduler = scheduler,// 调度器

time = time,

brokerTopicStats = brokerTopicStats, // BrokerTopic统计信息

logDirFailureChannel = logDirFailureChannel) // 日志目录失败通道

// 2、如果以'-delete'为后缀，加入addLogToBeDeleted队列等待删除的定时任务

if (logDir.getName.endsWith(Log.DeleteDirSuffix)) {

addLogToBeDeleted(log)

} else {

val previous \= {

if (log.isFuture)

this.futureLogs.put(topicPartition, log)

else

this.currentLogs.put(topicPartition, log)

}

if (previous != null) {

if (log.isFuture)

throw new IllegalStateException(s"Duplicate log directories found: ${log.dir.getAbsolutePath}, ${previous.dir.getAbsolutePath}")

else

throw new IllegalStateException(s"Duplicate log directories for $topicPartition are found in both ${log.dir.getAbsolutePath} " +

s"and ${previous.dir.getAbsolutePath}. It is likely because log directory failure happened while broker was " +

s"replacing current replica with future replica. Recover broker from this failure by manually deleting one of the two directories " +

s"for this partition. It is recommended to delete the partition in the log directory that is known to have failed recently.")

}

}

3、返回日志对象

log

}

该方法比较简单，主要就是**根据一些参数来对每个 TopicPartition 生成 Log 对象实例**，用来对每个 TopicPartition 的日志的操作。

接下来，我们简单看下 「**Log**」对象是如何被初始化出来的，底层又做了哪些事情？

## **4.4 Log 类初始化**

「**Log**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/Log.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/Log.scala)

@threadsafe

class Log(@volatile private var \_dir: File, // 当前日志目录

@volatile var config: LogConfig, // 当前日志的配置信息

@volatile var logStartOffset: Long, // 当前日志起始偏移量

@volatile var recoveryPoint: Long, // 需要恢复的数据的位置偏移量

scheduler: Scheduler,

brokerTopicStats: BrokerTopicStats,

val time: Time,

val maxProducerIdExpirationMs: Int,

val producerIdExpirationCheckIntervalMs: Int,

val topicPartition: TopicPartition, // 当前日志所属的主题分区

val producerStateManager: ProducerStateManager, // 生产者状态管理器

logDirFailureChannel: LogDirFailureChannel) extends Logging with KafkaMetricsGroup {

// 导入一些 Log 静态的常量和方法

import kafka.log.Log.\_

this.logIdent = s"\[Log partition=$topicPartition, dir=${dir.getParent}\] "

// 创建一个锁对象

private val lock \= new Object

// 是否已经关闭了内存映射的缓冲区，默认否

@volatile private var isMemoryMappedBufferClosed \= false

// 记录父目录路径的字符串形式

@volatile private var \_parentDir: String = dir.getParent

// 上一次flush的时间，用于日志管理中定期将内存数据刷入磁盘，这里使用 AtomicLong 以实现线程安全

private val lastFlushedTime \= new AtomicLong(time.milliseconds)

// 下一个消息起始位置的元数据信息，使用 volatile 修饰，同时使用 LogOffsetMetadata 包含了当前的三个重要属性值

@volatile private var nextOffsetMetadata: LogOffsetMetadata = \_

// 第一个不稳定的位置元数据信息，使用 Option 包裹，为空值时表示没有不稳定的位置

@volatile private var firstUnstableOffsetMetadata: Option\[LogOffsetMetadata\] = None

// 高水位元数据信息

@volatile private var highWatermarkMetadata: LogOffsetMetadata = LogOffsetMetadata(logStartOffset)

// Kafka 的 Log 由顺序的 Segment 日志段组成，这里使用 ConcurrentSkipListMap 存储，并且会基于消息的偏移量进行排序

private val segments: ConcurrentNavigableMap\[java.lang.Long, LogSegment\] = new ConcurrentSkipListMap\[java.lang.Long, LogSegment\]

// 为 TopicPartition 构建 LeaderEpoch => Offset 映射缓存

@volatile var leaderEpochCache: Option\[LeaderEpochFileCache\] = None

// 文件夹的初始化操作

locally {

// create the log directory if it doesn't exist

// 如果目录不存在的话则创建目录

Files.createDirectories(dir.toPath)

// 初始化 LeaderEpochCache

initializeLeaderEpochCache()

// 加载所有的日志段，得到下一个消息的起始位置

val nextOffset \= loadSegments()

// 初始化 nextOffsetMetadata 元数据信息

nextOffsetMetadata = LogOffsetMetadata(nextOffset, activeSegment.baseOffset, activeSegment.size)

// 重置 leaderEpochCache，从末尾开始截取

leaderEpochCache.foreach(\_.truncateFromEnd(nextOffsetMetadata.messageOffset))

// 更新日志起始位置

updateLogStartOffset(math.max(logStartOffset, segments.firstEntry.getValue.baseOffset))

// 重置 leaderEpochCache，从头开始截取

leaderEpochCache.foreach(\_.truncateFromStart(logStartOffset))

// 加载生产者状态，如果存在异常的状态则会抛出异常信息

if (!producerStateManager.isEmpty)

throw new IllegalStateException("Producer state must be empty during log initialization")

loadProducerState(logEndOffset, reloadFromCleanShutdown = hasCleanShutdownFile)

}

....

}

/\*\*

\* 从磁盘上的日志文件中加载日志段并返回下一个偏移量。此方法不需要将 IOException 转换为 KafkaStorageException，因为它只在加载所有日志之前调用。

\* 如果遇到具有溢出索引偏移的消息的 .swap 文件，则抛出 LogSegmentOffsetOverflowException 异常；或者当我们发现具有溢出的 .log 文件的数量时抛出异常

\*/

private def loadSegments(): Long = {

// 1、首先对日志目录中的文件进行遍历，并清理上次 Failure 遗留下来的各种临时文件（包括以".delete"、".cleaned"、".swap" 结尾的文件），收集Swap文件并查找任何中断的 swap 操作。

val swapFiles = removeTempFilesAndCollectSwapFiles()

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

// 3、待执行完上面两次遍历后，完成恢复过程中发现任何中断的 swap 操作。载入 SwapSegment 并替换对应的 Segment，为了保证安全，被 swap 段取代的日志文件应该在恢复 swap 文件作为新段文件之前将其重命名为 .deleted，后面的定时任务或者下次的系统重启会删除。

completeSwapOperations(swapFiles)

// 4、如果当前目录是标准目录而不是被标记为“.deleted”，则恢复日志段对象、重置当前活跃日志段的索引大小、返回恢复之后的分区日志 LEO 值。

if (!dir.getAbsolutePath.endsWith(Log.DeleteDirSuffix)) {

val nextOffset = retryOnOffsetOverflow {

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

从上面初始化源码可以得出， 「**Log**」对象初始化时会先「**创建目录**」、「**初始化 LeaderEpochCache**」、「**加载日志段**」获取下一个消息的起始位置、「**更新日志起始位置 LogStartOffset**」、「**重置 LeaderEpochCache**」等等一系列操作。

这里我们来梳理下「**LogSegment**」日志段加载的过程。

1.  首先对日志目录中的文件进行遍历，并清理上次 Failure 遗留下来的各种临时文件（包括以".delete"、".cleaned"、".swap" 结尾的文件），收集Swap文件并查找任何中断的 swap 操作。
2.  现在进行第二次遍历，并加载所有的日志和索引文件。
3.  如果遇到具有偏移量溢出的段，重试逻辑将对其进行拆分，然后我们需要重试加载段文件。 在发起此次 loadSegmentFiles() 调用之前需要关闭所有被遗留的日志段。
4.  **清空所有日志段对象**。
5.  **再次遍历分区日志路径，载入日志段 Segment 和 Index 索引文件，并将 Segment 依次加入 Cache 中**。
6.  **待执行完上面两次遍历后**，完成恢复过程中发现任何中断的 swap 操作。载入 SwapSegment 并替换对应的 Segment，为了保证安全，被 swap 段取代的日志文件应该在恢复 swap 文件作为新段文件之前将其重命名为 .deleted，后面的定时任务或者下次的系统重启会删除。
7.  如果当前目录是标准目录而不是被标记为“.deleted”，则**恢复日志段对象、重置当前活跃日志段的索引大小、返回恢复之后的分区日志 LEO 值**。
8.  否则如果日志目录下没有 segment 文件，就创建一个 activeSegment 作为起始，**重需要保证 Log 中至少有一个 LogSegment**。
9.  最后如果目录被标记为“.deleted”时，将下一个偏移量设置为 0。

这里有三种文件，我们来分析下：

1.  **deleted 文件**：标识需要被删除的 log 文件和 index 文件。
2.  **cleaned 文件**：在执行日志压缩过程中如果宕机，文件中的数据状态不明确，无法正确恢复的文件。
3.  **swap 文件**：完成执行日志压缩后的临时文件，但是在替换原文件时宕机。

针对 「**.deleted**」 和 「**.cleaned**」 文件**直接删除即可**，但对于 swap 文件来说，**因为其中的数据是完整的**，**直s所以可以继续使用**，**只需要再次完成 swap 操作即可**。

Kafka 针对 swap 文件的处理策略为：

1.  如果 swap 文件是 log 文件，则删除对应的 index 文件，稍后的 swap 操作会重建索引。
2.  如果 swap 文件是 index 文件，则直接删除，后续加载 log 文件时会重建索引。

最后我们总结下日志加载的大体流程，等下一篇再详细剖析：

1.  首先删除所有后缀名为 「**.deleted**」 和 「**.cleaned**」的文件。
2.  对于 .swp 结尾的文件，如果是 .log 文件则直接恢复，去掉 .swp 变为 .log；如果是 index 文件直接删掉，然后重新构建 index 文件。
3.  对于 .index 文件，如果没有对应的 .log 文件，同一个 logSement 其 index 和 log 的主文件名相同, 则删除该index文件；
4.  对于 .log 文件，加载到内存中。如果其没有对应的 .index 文件（可能在第 2 步中被删除), 重新恢复其 index文件。
5.  假设到这一步为止 Kafka 还没有加载到「**LogSegment**」, 说明该 partition log 目录下为空，一个新的 「**LogSegment**」对象会被创建在内存，反之则转向第6步。
6.  如果 Kafka 已经加载到 log, 最会开始 recover log segments。
7.  至于为什么要 recover log segments, 是因为大多数情况下，recover 的目的就是检查 Kafka 上次关闭时是不是 cleanShutDown （可通过检查 partition log 目录下是不是有后缀名为 .kafka\_cleanshutdown 的文件确定）。
8.  如果是 cleanShutDown (后缀名为 .kafka\_cleanshutDown 的文件存在），则无需 recover log segment。
9.  如果不是 cleanShutDown, 则需要 recover log segments。
10.  这里解释下什么是 recover a log segment?
11.  在非 cleanShutDown 情况下， 一个 「**LogSegment**」的 log 及 index 文件末尾可能有一些不合法的数据(invalid), 我们需要把它们截掉。
12.  首先要做的最简单检查，是 log 或 index 文件大小不能超过配置中设定的值（比方说一个 .log 文件中被设定最多保存 10000 条消息，超过 10000 条的都要抛弃掉）。     
13.  最后做 sanityCheck, 主要是检查每个「**LogSegment**」 的 index 文件，确保不会加载一个出错的「**LogSegment**」。

至此，初始化 LogManger 基本剖析完成，代码有两个主要方法：

1.  **createAndValidateLogDirs()**：创建指定的数据目录，并做相应的检查： 1.确保数据目录中没有重复的数据目录、2.数据目录不存在的话就创建相应的目录；3. 检查每个目录路径是否是可读的；
2.  **loadLogs()**：加载所有的日志分区，而每个日志也会调用 loadSegments() 方法加载该分区所有的 segment 文件，过程比较慢，所以 LogManager 使用线程池的方式，为每个日志的加载都会创建一个单独的线程。

这里加载虽然使用的是**线程池提交任务**，并发进行 load 分区日志，但这个任务本身是**阻塞式**的，**只有当所有的分区日志都加载完成**，**才能调用** 「**startup()**」 **启动** 「**LogManager**」**线程**。

经过上面这一番过程，在日志目录的所有分区日志都加载完成后，KafkaServer 调用 「**startup()**」 方法启动 「**LogManager**」线程，「**LogManager**」终于启动成功了，在这个过程中设置了五个定时任务，分别有「**cleanupLogs**」、，「**flushDirtyLogs**」、「**checkpointLogRecoveryOffsets**」、「**checkpointLogStartOffsets**」、「**deleteLogs**」等后台任务。

##   
**05 LogManager 后台任务**

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

首先这五个从后台启动的周期性任务是通过KafkaScheduler这个Kafka基于java的ScheduledThreadPoolExecutor自定义的调度程序。

1.  启动 kafka-log-retention 周期性任务，遍历所有的 log，对过期或过大的日志文件执行清理工作。
2.  清除条件：1.日志超过保留时间。2.日志大小超过保留大小时，会删除最旧的 LogSegment，以控制整个 Log 的大小。
3.  启动 kafka-log-flusher 周期性任务，对日志文件执行刷盘操作，定时把内存中的数据刷到磁盘中。
4.  启动 kafka-recovery-point-checkpoint 周期性任务，更新 kafka-recovery-point-checkpoint 文件。向路径中写入当前的恢复点，避免在重启的时候重新恢复全部数据。
5.  启动 kafka-log-start-offset-checkpoint 周期性任务，更新 kafka-log-start-offset-checkpoint 文件。向日志目录写入当前存储日志中的 start offset，避免读到已经被删除的日志。
6.  启动 kafka-delete-logs 周期性任务，清理已经被标记为删除的日志。
7.  最后启动日志清理压缩线程。

这里 deleteLogs 定时任务比较特殊，并未直接设置调度间隔。执行这部分的后台线程是由前一次后台任务完成时动态进行调度的。

对里面的单一定时任务，今天先暂时不展开分析，后面会单独篇章进行剖析，最后通过一张图来总结启动流程。

  
![](https://article-images.zsxq.com/lkU8ZDtCQxSupyzl9aOPnX11UmqJ)

##   
**06 总结**

这里，我们一起来总结一下这篇文章的重点。

1、通过上一篇的分析我们引出了服务端日志存储的管理对象之 「**LogManager**」。

2、带你深度剖析了 「**LogManager**」初始化以及重要参数、属性的分析 。

3、接着带你深度剖析了 「**LogManager**」的重要方法，包括「**createAndValidateLogDirs**」、「**loadLogs**」、「**loadLog**」以及 「**Log 类初始化**」、「**加载 LogSegment**」。

4、最后剖析了启动的后台周期性任务。

下篇我们来深度剖析「**LogSegment 日志段架构设计**」，大家期待，我们下期见。