大家好，我是 **华仔**, 又跟大家见面了。

上一篇中，主要带大家深度剖析了 「**Kafka 服务端源码 LogManger 日志管理对象**」，通过「**场景驱动方式**」，现在消息被封装成批次请求已经从「**生产者**」发送到「**Broker**」，且被「**网络层**」所接收到并准备进行消息数据存储，从今天开始，我们来深度剖析 Kafka 日志系统的底层实现，这是日志系列第四篇，我们先来深度聊聊「**Kafka 服务端源码 LogSegment 日志段架构设计**」，看看 Kafka 服务端是如何真正存储消息数据的。

![](https://article-images.zsxq.com/FokM1vOs3xxTvPC1RLe4dWntdwUC)

## **01 总体概述**

在上一篇中，我们 [【服务端 Broker 源码分析系列第十篇】图解 Kafka 源码之 LogManager 磁盘文件管理组件](https://articles.zsxq.com/id_fjotrswcfval.html) 引出了服务端日志段的管理对象之 「**LogSegment**」。

接下来我们来对 「**LogSegment**」对象一探究竟，从名字上看就知道它是「**日志段组件**」，日志段相关源码是 Kafka 服务端源码最重要的组件之一，你可能非常想知道，在 Kafka 中消息是如何被保存和组织的。

你一定很好奇，比如下图中的日志文件是如何被命名生成的， 学完本篇后，我相信对于这个问题一定会迎刃而解的。

![](https://article-images.zsxq.com/FjoICmaX479SzgHwU6okodtGIZZ3)

图中的一串数字 0 是该日志段的起始位移值（Base Offset），也就是该日志段中所存的第一条消息的位移值。

好了，下面开始剖析其源码实现，进入今天的正题。

整个日志管理的相关组件的调用关系图如下：

![](https://article-images.zsxq.com/FgVmpr3JOm-_1PVfCHJC94zu4_tC)

## **02 Kafka 日志结构概览**

Kafka 日志在磁盘上的组织结构如下图所示：

![](https://article-images.zsxq.com/FnDPoaG_9_LVjOlLMYsxj-t1zvVp)

总体来说，Kafka 日志对象是由多个「**LogSegment**」日志段对象组成，而每个「**LogSegment**」日志段对象会在磁盘上创建一组文件，如上图，包括「**消息日志文件 .log**」、「**位移索引文件 .index**」、「**时间索引文件 .timeindex**」、如果有使用 Kafka 事务，还会存在「**已终止事务索引文件 .txnindex**」。

一般情况下，一个 Kafka 主题 Topic 会有很多分区，每个分区就对应一个 「**Log**」对象，在**物理磁盘上则对应一个子目录**。

例如下图你创建了一个2分区的主题 message3，那么，Kafka 在磁盘上会创建两个子目录：message3-0 和 message3-1。而在服务器端，这就是两个 Log 对象。每个子目录下存在多组日志段，也就是多组「**消息日志文件 .log**」、「**位移索引文件 .index**」、「**时间索引文件 .timeindex**」 文件组合，只不过文件名不同，因为每个日志段的起始位移不同，初始位移都是0。

![](https://article-images.zsxq.com/Fry0mSPbIZb3hZrgZl49IjZfbcJ-)

![](https://article-images.zsxq.com/FtBoNxLCO_uKh35rbF7DGZ7e3bHp)

## **03 LogSegment 源码总览**

「**LogSegment**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogSegmen](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogSegment.scala)[t.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/Log.scala)，所有跟日志相关的源码都在 kafka/log 这个包下面。

![](https://article-images.zsxq.com/FklFfATk93AbxCrfw9pwZ9vvx8io)

如上图所示，「**LogSegment**」类中定义了三个对象：

1.  LogSegment class。
2.  LogSegment object。
3.  LogFlushStats object。从名字上可以得出，其主要负责日志落盘计时统计的。

// 负责为日志落盘进行计时

object LogFlushStats extends KafkaMetricsGroup {

val logFlushTimer \= new KafkaTimer(newTimer("LogFlushRateAndTimeMs", TimeUnit.MILLISECONDS, TimeUnit.SECONDS))

}

这里我需要关心前 2 个，在 Scala 语言里，在一个源代码文件中同时定义相同名字的 class 和 object 的用法被称为伴生（Companion）。Class 对象被称为伴生类，它和 Java 中的类是一样的；而 Object 对象是一个单例对象，用于保存一些静态变量或静态方法。如果用 Java 来做类比的话，我们必须要编写两个类才能实现，这两个类也就是 LogSegment 和 LogSegmentUtils。在 Scala 中，你直接使用伴生就可以了。

##   
**04 LogSegment 初始化**

/\*\*

\* A segment of the log. Each segment has two components: a log and an index. The log is a FileRecords containing

\* the actual messages. The index is an OffsetIndex that maps from logical offsets to physical file positions. Each

\* segment has a base offset which is an offset <= the least offset of any message in this segment and > any offset in

\* any previous segment.

\*

\* A segment with a base offset of \[base\_offset\] would be stored in two files, a \[base\_offset\].index and a \[base\_offset\].log file.

\*

\* @param log The file records containing log entries

\* @param lazyOffsetIndex The offset index

\* @param lazyTimeIndex The timestamp index

\* @param txnIndex The transaction index

\* @param baseOffset A lower bound on the offsets in this segment

\* @param indexIntervalBytes The approximate number of bytes between entries in the index

\* @param rollJitterMs The maximum random jitter subtracted from the scheduled segment roll time

\* @param time The time instance

\*/

@nonthreadsafe

class LogSegment private\[log\] (val log: FileRecords,

val lazyOffsetIndex: LazyIndex\[OffsetIndex\],

val lazyTimeIndex: LazyIndex\[TimeIndex\],

val txnIndex: TransactionIndex,

val baseOffset: Long,

val indexIntervalBytes: Int,

val rollJitterMs: Long,

val time: Time) extends Logging {

def offsetIndex: OffsetIndex = lazyOffsetIndex.get

def timeIndex: TimeIndex = lazyTimeIndex.get

//当前logSegment的创建时间

private var created \= time.milliseconds

//自上次添加索引项之后，在log文件中累计加入的消息字节数

private var bytesSinceLastIndexEntry \= 0

/\* The timestamp we used for time based log rolling \*/

private var rollingBasedTimestamp: Option\[Long\] = None

//已追加消息的最大时间戳

@volatile private var maxTimestampSoFar: Long = timeIndex.lastEntry.timestamp

//已追加的具备最大时间戳的消息对应的offset

@volatile private var offsetOfMaxTimestamp: Long = timeIndex.lastEntry.offset

....

}

kafka 非常棒的一点就是它的注释写的非常详细，从注释中你就可以得出该类的主要作用以及做的事情都有哪些。这里我们先来看看 「**LogSegment**」类注释的翻译，如下：

> 它是日志的一段。每个日志段有两个组成部分：一个日志和一个索引。该日志是一个 FileRecords，里面包含了实际的消息。索引是从逻辑偏移映射到物理文件位置的偏移索引。每个段有一个起始位移值（Base Offset），而该位移值是此日志段所有消息中最小的位移值，同时，该值却又比前面任何日志段中消息的位移值都大。

我想当你看完这个注释，就能够快速地了解「**LogSegment**」类其实就是管理 segment 文件的，在 「**LogSegment**」中封装了「**FileRecords**」、「**OffsetIndex**」、「**TimeIndex**」 对象，提供了日志文件和索引文件的读写功能。

接下来，我们来看看其重要字段说明。

## **4.1 LogSegment 重要字段**

1.  **log**：用于操作对应日志文件，实际保存 Kafka 消息的对象，下面会剖析。
2.  **lazyOffsetIndex**：用于操作对应索引文件的 OffsetIndex 对象。
3.  **lazyTimeIndex**: 用于操作对应时间索引文件的 TimeIndex 对象。
4.  这里用到了延迟初始化的原理，降低了初始化的时间成本。
5.  **txnIndex**：事务索引。
6.  **baseOffset**：logSegment 第一个消息的 offset 值。**这个是非常重要的属性**！事实上，你在磁盘上看到的文件名就是 baseOffset 的值。每个 LogSegment 对象实例一旦被创建，它的起始位移就是固定的了，不能再被更改。
7.  **indexIntervalBytes**：索引项之间间隔的最小字节数。对应 Broker 端参数 log.index.interval.bytes 值。它控制了**日志段对象新增索引项的频率**。默认情况下，日志段至少新写入 4KB 的消息数据才会新增一条索引项。
8.  **rollJitterMs**：日志段对象新增倒计时的"扰动值"。因为目前 Broker 端日志段新增倒计时是**全局设置**，即在未来的某个时刻可能同时创建多个日志段对象，这将极大地增加物理磁盘 I/O 压力。所以有了 rollJitterMs 值的干扰，每个新增日志段在创建时会彼此分隔一小段时间，这样可以缓解物理磁盘的 I/O 负载瓶颈。

## **4.2 核心类关系梳理**

Kafka 为了防止日志 Log 文件过大，将**逻辑上的 Log 文件**切分成多个**物理上的日志文件**，每个日志文件对应多个 「**LogSegment**」。从上面初始化中可以看出「**LogSegment**」底层是通过「**FileRecords**」来映射到一个物理文件的。

因此「**Log**」、「**LogSegment**」、「**FileRecords**」三者的关系图如下：

  
![](https://article-images.zsxq.com/Fh85T3uiy2yv8Y40M8ZJNcOB8BlX)

## **05 LogSegment 重要方法**

接下来我们来看看其重要方法。对于日志段来说，最重要的就是「**追加消息**」、「**读取消息**」、「**恢复消息**」、「**是否需要新建日志段**」、「**切割日志段**」、「**刷新日志段**」等等。

##   
**5.1 append()**

先来看看**向日志段中追加消息**的方法，源码如下：

/\*\*

\* Append the given messages starting with the given offset. Add

\* an entry to the index if needed.

\*

\* It is assumed this method is being called from within a lock.

\*

\* @param largestOffset The last offset in the message set

\* @param largestTimestamp The largest timestamp in the message set.

\* @param shallowOffsetOfMaxTimestamp The offset of the message that has the largest timestamp in the messages to append.

\* @param records The log entries to append.

\* @return the physical position in the file of the appended records

\* @throws LogSegmentOffsetOverflowException if the largest offset causes index offset overflow

\*/

@nonthreadsafe

def append(largestOffset: Long, // 待写入批次中消息的最大位移值

largestTimestamp: Long, // 待写入批次中消息的最大时间戳

shallowOffsetOfMaxTimestamp: Long, // 待写入批次中消息的最大时间戳对应的位移

records: MemoryRecords): Unit = { // 真正写入的消息集合

// 1、判断消息大小

if (records.sizeInBytes > 0) {

// 打印日志

trace(s"Inserting ${records.sizeInBytes} bytes at end offset $largestOffset at position ${log.sizeInBytes} " +

s"with largest timestamp $largestTimestamp at shallow offset $shallowOffsetOfMaxTimestamp")

// 2、计算当前消息偏移量所对应的物理地址，即获取 FileRecords 文件的末尾，它就是本次消息要写入的物理地址。

val physicalPosition \= log.sizeInBytes() // 确认当前log文件已经写到的位置

if (physicalPosition == 0) // 如果日志位置为空

rollingBasedTimestamp = Some(largestTimestamp) // 更新用于日志段切分的时间戳

// 3、确保当前消息偏移量不超出日志段范围，就是看它与日志段起始位移的差值是否在整数范围内，即 largestOffset - baseOffset 的值是不是介于 \[0，Int.MAXVALUE\] 之间

ensureOffsetInRange(largestOffset)

// 4、将消息追加到日志段末尾，并记录追加的字节数

val appendedBytes \= log.append(records)

trace(s"Appended $appendedBytes to ${log.file} at end offset $largestOffset")

// 5、更新日志段的最大时间戳以及最大时间戳所属消息的位移值。

if (largestTimestamp > maxTimestampSoFar) {

maxTimestampSoFar = largestTimestamp

offsetOfMaxTimestampSoFar \= shallowOffsetOfMaxTimestamp

}

// 广播日志

if (config.interBrokerProtocolVersion >= KAFKA\_0\_11\_0\_IV0)

// send the global timestamp if required

interBrokerPartitionIndex.append(largestOffset, timestampAppendMs)

// 6、检查是否需要新增索引项，因为日志索引被设计成稀疏索引，所以要判断这个消息是否有索引。

// indexIntervalBytes默认是4096个字节,即每次写了4096个字节的消息就写一次索引。

if (bytesSinceLastIndexEntry > indexIntervalBytes) { // 如果达到写入的字节数，新增索引项，并清空字节数，以便下次达到指定字节数进行新增索引项

// 写入新位移索引，位移索引保存消息位移值与物理文件写入位置的对应关系

offsetIndex.append(largestOffset, physicalPosition)

// 写入新时间戳索引，时间戳索引项保存时间戳与消息位移的对应关系

timeIndex.maybeAppend(maxTimestampSoFar, offsetOfMaxTimestampSoFar)

// 清空已写入字节，重新开始

bytesSinceLastIndexEntry = 0

}

// 7、如果不需要写索引时，将这批数据追加到 bytesSinceLastIndexEntry 写入字节数中,以便下次重新累计计算

bytesSinceLastIndexEntry += records.sizeInBytes

}

}

// 检查偏移量是否越界

private def ensureOffsetInRange(offset: Long): Unit = {

if (!canConvertToRelativeOffset(offset))

throw new LogSegmentOffsetOverflowException(this, offset)

}

/\*\*

\* 检查给定偏移量是否可以转换为相对于基础偏移量的整数偏移量

\*/

def canConvertToRelativeOffset(offset: Long): Boolean = {

// 检查索引是否可以追加给定的偏移量

offsetIndex.canAppendOffset(offset)

}

接下来我们梳理下该方法的执行步骤：

1.  判断要追加的消息大小是否大于零，否则没有意义。
2.  计算当前消息偏移量所对应的物理地址，即获取 FileRecords 文件的末尾，它就是本次消息要写入的物理地址。这里调用 log.sizeInBytes 方法判断**该日志段是否为空**，如果是空的话， Kafka 需要记录要写入消息集合的最大时间戳，并将其作为后面新增日志段倒计时的依据。
3.  调用 ensureOffsetInRange 方法**确保当前消息偏移量不超出日志段范围，即合法**。那怎么判断是不是合法呢？就是看它与日志段起始位移的差值是否在整数范围内，即 largestOffset - baseOffset 的值是不是介于 \[0，Int.MAXVALUE\] 之间。**如果你碰到这个问题，需要做的就是升级你的 Kafka 版本就行了，因为这是由已知 bug 导致的**。
4.  将消息追加到日志段末尾，并记录追加的字节数。底层调用 FileRecords 类的 append() 方法来实现，将内存中的消息对象写入到操作系统的页缓存 PageCache 中。
5.  更新日志段的最大时间戳以及最大时间戳所属消息的位移值， 其中位移索引 offsetIndex 记录的是消息位移和物理文件写入位置的对应关系， 时间索引 timeIndex 记录的是最大时间戳和对应的消息位移值。
6.  比如此时我们只想保留最近 7 天的日志，那么判断的依据就是当前最大时间戳，而最大时间戳对应的消息的位移值则用于时间戳索引项。**会在时间戳索引项中保存时间戳和消息位移的对应关系**。
7.  检查是否需要新增索引项，因为日志索引被设计成稀疏索引，所以要判断这个消息是否有索引。indexIntervalBytes 默认是4096个字节, 即**日志段每写入 4 KB 数据就要写入一个索引项**。当已写入字节数超过了 4KB 之后调用索引对象的 append 方法新增索引项，同时清空已写入字节数，以备下次重新累积计算。
8.  最后，如果不需要写索引时，将这批数据追加到 bytesSinceLastIndexEntry 写入字节数中，以便下次重新累计计算。

> 注意：这个方法不是线程安全的，需要用锁保证一致性。

![](https://article-images.zsxq.com/Fl0ss56isLPY54VsEk6MXQfgYH9K)

## **5.2 read()**

接下来，我们来看看**读取消息**的方法，源码如下：

/\*\*

\* Read a message set from this segment beginning with the first offset >= startOffset. The message set will include

\* no more than maxSize bytes and will end before maxOffset if a maxOffset is specified.

\*

\* @param startOffset A lower bound on the first offset to include in the message set we read

\* @param maxSize The maximum number of bytes to include in the message set we read

\* @param maxPosition The maximum position in the log segment that should be exposed for read

\* @param minOneMessage If this is true, the first message will be returned even if it exceeds \`maxSize\` (if one exists)

\*

\* @return The fetched data and the offset metadata of the first message whose offset is >= startOffset,

\* or null if the startOffset is larger than the largest offset in this log

\*/

@threadsafe

def read(startOffset: Long, // 要读取的第一条消息的位移，如消费者拉取消息时，要给出开始拉取消息的偏移量。

maxSize: Int, // 能读取的最大字节数，默认是1M

maxPosition: Long = size, // 能读取的最大位置

minOneMessage: Boolean = false): FetchDataInfo = { // 是否允许在消息体过大时至少返回第一条消息，引入这个参数主要是为了确保不出现消费饿死的情况。

if (maxSize < 0)

throw new IllegalArgumentException(s"Invalid max size $maxSize for log read from segment $log")

// 1、根据给定的消息的 offset，查询索引文件，得到对应的物理位置，其过程是查询索引文件，根据返回的索引项里的消息物理位置再去消息文件中顺序查找。

val startOffsetAndSize \= translateOffset(startOffset) // 搜索 index 文件，获取符合条件的第一条消息的三要素： 位移值，消息大小，消息的物理文件位置。

// 如果起始偏移量已经超出了日志段范围，返回空

if (startOffsetAndSize == null)

return null

val startPosition \= startOffsetAndSize.position // 获取消息的物理文件位置

val offsetMetadata \= LogOffsetMetadata(startOffset, this.baseOffset, startPosition) // 构造 LogOffsetMetadata 对象，获取日志位移的元数据，记录偏移量和位于日志段列表中的索引位置

// 2、读取最大的消息数

val adjustedMaxSize \=

if (minOneMessage) math.max(maxSize, startOffsetAndSize.size) // 如果要求至少返回一个消息，则最大字节数应该为 \`maxSize\` 和起始偏移量所在消息集合大小的最大值

else maxSize // 如果为 false,直接取 maxSize

// 如果待读取的数据大小为 0，则返回空数据

if (adjustedMaxSize == 0)

return FetchDataInfo(offsetMetadata, MemoryRecords.EMPTY)

// 3、确定最终能够读取的总字节数

val fetchSize: Int = min((maxPosition - startPosition).toInt, adjustedMaxSize)

// 4、调用 FileRecords#slice 方法获取从指定位置读取指定大小的消息集合。

FetchDataInfo(offsetMetadata, log.slice(startPosition, fetchSize),

firstEntryIncomplete = adjustedMaxSize < startOffsetAndSize.size) // 构造 FetchDataInfo 对象，并记录待读取的记录是否完整

}

该方法相对比较简单，接下来我们梳理下该方法的执行步骤：

1.  根据给定的消息的 offset，查询索引文件，得到对应的物理位置，其过程是**查询索引文件**，**件根据返回的索引项里的消息物理文件位置再去消息文件中顺序二分查找**。这里主要时调用 translateOffset 方法定位要读取的起始文件位置 （startPosition）, 参数 startOffset 只是位移值，Kafka 底层需要根据**索引信息找到对应的为物理文件位置才能开始读取消息**。
2.  当确定了读取的起始文件位置 （startPosition）后，来确定能读取到的最大消息数。如果待读取的数据大小为 0，则返回空数据。
3.  接着来确定最终能够读取的总字节数。举个例子，假设 maxSize=100，maxPosition=300，startPosition=230，那么 read 方法只能读取 70 字节，因为 maxPosition - startPosition = 70。我们把它和 maxSize 参数相比较，**取最小值就是最终能读取的总字节数**，这里就是70。
4.  最后调用 FileRecords#slice 方法获取从指定位置读取指定大小的消息集合。

## **5.3 translateOffset()**

接下来，我们来看看**读取消息方法中查询索引文件确定物理文件位置**的方法，源码如下：

/\*\*

\* Find the physical file position for the first message with offset >= the requested offset.

\* 确定给定偏移量的位置，从而进行消息读取。

\* The startingFilePosition argument is an optimization that can be used if we already know a valid starting position

\* in the file higher than the greatest-lower-bound from the index.

\*

\* @param offset The offset we want to translate

\* @param startingFilePosition A lower bound on the file position from which to begin the search. This is purely an optimization and

\* when omitted, the search will begin at the position in the offset index.

\* @return The position in the log storing the message with the least offset >= the requested offset and the size of the

\* message or null if no message meets this criteria.

\*/

@threadsafe

private\[log\] def translateOffset(offset: Long, startingFilePosition: Int = 0): LogOffsetPosition = {

// 1、根据二分查找法找到索引项，得到索引文件中索引项的偏移量 offset 和索引项所处的物理文件位置的映射关系。

val mapping \= offsetIndex.lookup(offset)

// 2、根据索引文件搜出来的索引，进一步在日志文件里搜索。

log.searchForOffsetWithSize(offset, max(mapping.position, startingFilePosition))

}

我们在 [【服务端 Broker 源码分析系列第八篇】图解 Kafka 源码之日志存储机制介绍以及核心对象管理梳理](https://articles.zsxq.com/id_rqum80f450hc.html) 这篇中知道底层是基于「**稀疏哈希索引**」的，要找的偏移量不一定在索引里，所以要确定最近哪个偏移量存在索引，然后再根据索引找到偏移量，最后再顺序查找到要找的偏移量，如下图所示：

  
![](https://article-images.zsxq.com/FsmRpfy6l37q-eKJEGYnHAfttpIm)

## **5.4 recover()**

接下来，我们来看看**恢复日志段**的方法。那么**什么是恢复日志段呢**？简单来说就是，Kafka Broker 在启动时会从磁盘上加载所有日志段信息到内存中，并创建相应的 「**LogSegment**」对象实例并重建索引。源码如下：

/\*\*

\* Run recovery on the given segment. This will rebuild the index from the log file and lop off any invalid bytes

\* from the end of the log and index.

\*

\* @param producerStateManager Producer state corresponding to the segment's base offset. This is needed to recover

\* the transaction index.

\* @param leaderEpochCache Optionally a cache for updating the leader epoch during recovery.

\* @return The number of bytes truncated from the log

\* @throws LogSegmentOffsetOverflowException if the log segment contains an offset that causes the index offset to overflow

\*/

@nonthreadsafe

def recover(producerStateManager: ProducerStateManager, leaderEpochCache: Option\[LeaderEpochFileCache\] = None): Int = {

// 1、调用索引对象的reset方法清空所有的索引文件

offsetIndex.reset()

timeIndex.reset()

txnIndex.reset()

// 重置字节数

var validBytes \= 0

// 重置索引记录

var lastIndexEntry \= 0

// 重置最大时间戳

maxTimestampSoFar = RecordBatch.NO\_TIMESTAMP

try {

// 2、遍历日志段文件中所有消息集合

for (batch <- log.batches.asScala) {

batch.ensureValid() // 检查消息集合内容是否符合kafka的二进制格式

ensureOffsetInRange(batch.lastOffset) // 检查消息位移值合法性，确保该集合中最后一条消息的位移值不能越界，即它与日志段起始位移的差值必须是一个正整数值。

// 3、更新最大时间戳及所属消息的位移值，后续用于时间戳索引

if (batch.maxTimestamp > maxTimestampSoFar) {

// 最大时间戳

maxTimestampSoFar = batch.maxTimestamp

// 所属消息的位移值

offsetOfMaxTimestampSoFar = batch.lastOffset

}

// 4、重建位移索引，如果大于指定的字节大小，则新增索引项

if (validBytes - lastIndexEntry > indexIntervalBytes) {

// 更新位移索引记录的位移值和物理文件位置（字节数）的对应关系

offsetIndex.append(batch.lastOffset, validBytes)

timeIndex.maybeAppend(maxTimestampSoFar, offsetOfMaxTimestampSoFar)

lastIndexEntry = validBytes

}

// 5、累计当前已读取的消息字节数

validBytes += batch.sizeInBytes()

// 6、处理生产者状态信息（如果消息格式为 V2 或以上版本）

if (batch.magic >= RecordBatch.MAGIC\_VALUE\_V2) {

// 更新 Leader Epoch 缓存

leaderEpochCache.foreach { cache =>

if (batch.partitionLeaderEpoch >= 0 && cache.latestEpoch.forall(batch.partitionLeaderEpoch > \_))

cache.assign(batch.partitionLeaderEpoch, batch.baseOffset)

}

// 更新事务型 Producer 的状态

updateProducerState(producerStateManager, batch)

}

}

} catch {

// 如果在处理批次记录时出现无效记录，则记录一条警告信息

case e@ (\_: CorruptRecordException | \_: InvalidRecordException) =>

warn("Found invalid messages in log segment %s at byte offset %d: %s. %s"

.format(log.file.getAbsolutePath, validBytes, e.getMessage, e.getCause))

}

// 7、此时 Kafka 会将日志段当前总字节数减去刚刚累加的已读取字节数，如果大于 0 说明日志段写入了一些非法无效消息，需要执行截断操作，将日志段大小调整回合法的数值。同时调整索引文件的大小。

val truncated \= log.sizeInBytes - validBytes

if (truncated > 0)

debug(s"Truncated $truncated invalid bytes at the end of segment ${log.file.getAbsoluteFile} during recovery")

// 消息本体截断

log.truncateTo(validBytes)

// 位移索引对应截断

offsetIndex.trimToValidSize()

// A normally closed segment always appends the biggest timestamp ever seen into log segment, we do this as well.

// 确保更新时间戳索引

timeIndex.maybeAppend(maxTimestampSoFar, offsetOfMaxTimestampSoFar, skipFullCheck = true)

// 时间戳索引截断

timeIndex.trimToValidSize()

truncated

}

接下来我们梳理下该方法的执行步骤：

1.  清空索引文件。
2.  遍历日志段中所有消息集合。
3.     校验消息集合。
4.     保存最大时间戳和所属消息位移。
5.     更新索引项。
6.     更新总消息字节数。
7.     更新事务 producer 状态和 Leader\_Epoch 缓存。
8.  执行消息日志索引文件截断。

> 注意：这个方法不是线程安全的，需要用锁保证一致性。

![](https://article-images.zsxq.com/Fqb-JH6FOZg1YuhQTI7Y3qfl4Xp-)

上面流程中，最终要的是 「**第四步**」和 「**第七步**」，我们在后面章节来剖析。

## **5.5 shouldRoll()**

接下来，我们来看看**是否需要新建日志段**的方法。源码如下：

def shouldRoll(rollParams: RollParams): Boolean = {

// 1、计算多久没有新建日志分段文件了。

val reachedRollMs \= timeWaitedForRoll(rollParams.now, rollParams.maxTimestampInMessages) > rollParams.maxSegmentMs - rollJitterMs

// 2、需要新建日志段的条件

size > rollParams.maxSegmentBytes - rollParams.messagesSize || // 当前日志段大小超过了阈值

(size > 0 && reachedRollMs) || // 当前时间已经超过了 log.roll.hours

offsetIndex.isFull || // 偏移量索引已满

timeIndex.isFull || // 时间戳索引已满

!canConvertToRelativeOffset(rollParams.maxOffsetInMessages) // 无法将 maxOffsetInMessages 转换为相对偏移量

}

/\*\*

\* 计算检查点上次写入时间以来经过了多长时间

\* @param now 当前时间

\* @param messageTimestamp 所有消息记录中的最新时间戳

\* @return 经过的时间

\*/

def timeWaitedForRoll(now: Long, messageTimestamp: Long) : Long = {

// 载入第一条记录的时间戳

loadFirstBatchTimestamp()

rollingBasedTimestamp match {

case Some(t) if t >= 0 => messageTimestamp - t // 根据记录的滚动时间计算

case \_ \=\> now - created // 如果没有记录的滚动时间，则使用文件创建时间计算

}

}

/\*\*

\* 检查给定偏移量是否可以转换为基础偏移量的相对偏移量

\*

\* @param offset 给定的偏移量

\* @return 如果可以，则返回 true；否则返回 false

\*/

def canConvertToRelativeOffset(offset: Long): Boolean = {

offsetIndex.canAppendOffset(offset) // 检查偏移量是否可以追加到偏移量索引中

}

该方法比较简单，需要满足以下「**5 个**」条件就可以创建日志段了：

1.  当前日志段大小超过了阈值，默认 1个 G，就需要新建一个 LogSegment。
2.  日志段有数据且距离上次创建日志段的时间达到了一个阈值（[log.roll.hours](http://log.roll.hours/) 默认7天）。
3.  索引文件满了（默认10 M）。
4.  时间索引文件满了（默认10 M）。
5.  无法将 maxOffsetInMessages 转换为相对偏移量，如果相对偏移量超过了正整数的阈值，即追加的消息的偏移量与当前日志段的偏移量之间的差值大于 Integer.MAX\_VALUE。因为相对偏移量是 4 个字节，对应int类型也是 4 个字节，这样再大就超出了。

## **5.5 truncateTo()**

接下来，我们来看看**切割日志段**的方法。源码如下：

/\*\*

\* Truncate off all index and log entries with offsets >= the given offset.

\* If the given offset is larger than the largest message in this segment, do nothing.

\* 将日志段截断到指定偏移量

\* @param offset The offset to truncate to offset 指定的偏移量

\* @return The number of log bytes truncated 被截断的字节数

\*/

@nonthreadsafe

def truncateTo(offset: Long): Int = {

// 1、根据位移查找索引

val mapping \= translateOffset(offset)

// 2、处理位移量索引、时间戳索引和事务信息索引

offsetIndex.truncateTo(offset)

timeIndex.truncateTo(offset)

txnIndex.truncateTo(offset)

// 3、然后分配更多空间给位移量索引和时间戳索引

offsetIndex.resize(offsetIndex.maxIndexSize)

timeIndex.resize(timeIndex.maxIndexSize)

// 4、紧接着，利用偏移量索引来截断日志文件

val bytesTruncated \= if (mapping == null) 0 else log.truncateTo(mapping.position)

// 5、如果日志文件为空，则重置日志段相关变量

if (log.sizeInBytes == 0) {

created = time.milliseconds

rollingBasedTimestamp \= None

}

bytesSinceLastIndexEntry = 0

// 重新载入最大时间戳

if (maxTimestampSoFar >= 0)

loadLargestTimestamp()

bytesTruncated // 6、返回被截断的字节数

}

private def loadLargestTimestamp(): Unit = {

// 获取时间戳索引中最后一个条目，如果时间戳索引为空，则返回 (-1, 基础偏移量)

val lastTimeIndexEntry \= timeIndex.lastEntry

maxTimestampSoFar \= lastTimeIndexEntry.timestamp

offsetOfMaxTimestampSoFar \= lastTimeIndexEntry.offset

// 查找当前时间戳最大的那条消息的 offset 位置

val offsetPosition \= offsetIndex.lookup(lastTimeIndexEntry.offset)

// 在该消息后面扫描剩余消息，看是否存在时间戳更大的消息

val maxTimestampOffsetAfterLastEntry \= log.largestTimestampAfter(offsetPosition.position)

if (maxTimestampOffsetAfterLastEntry.timestamp > lastTimeIndexEntry.timestamp) {

maxTimestampSoFar = maxTimestampOffsetAfterLastEntry.timestamp

offsetOfMaxTimestampSoFar \= maxTimestampOffsetAfterLastEntry.offset

}

}

接下来我们梳理下该方法的执行步骤：

1.  根据位移查找索引。
2.  截断位移量索引、时间戳索引和事务信息索引，这里的方法后面章节会剖析。
3.  然后分配更多空间给位移量索引和时间戳索引。
4.  紧接着，利用偏移量索引来截断日志文件。
5.  如果日志文件为空，则重置日志段相关变量。
6.  最后返回被截断的字节数。

> 注意：这个方法不是线程安全的，需要用锁保证一致性。

## **5.6 flush()**

最后我们来看看**刷新日志段**的方法，源码如下：

/\*\*

\* Flush this log segment to disk

\*/

@threadsafe

def flush(): Unit = {

LogFlushStats.logFlushTimer.time {

// 分别刷入日志、位移量索引、时间戳索引和事务信息索引

log.flush()

offsetIndex.flush()

timeIndex.flush()

txnIndex.flush()

}

}

该方法非常简单，就是直接刷新日志、位移量索引、时间戳索引和事务信息索引。

至此，「**LogSegment**」类的方法大致就带大家剖析完了，那么接下来我们回过头来，看看 「**FileRecords**」类的相关属性和方法，了解真正实际日志消息的底层实现。

##   
**06 FileRecords 初始化**

「**FileRecords**」类源码在 Kafka 源码包的 clients包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/common/record/FileRecords.java](https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/common/record/FileRecords.java)。

「**FileRecords**」类是用来描述和管理**日志段**文件数据，对应一个 log 文件。它是**日志段**文件上的原始消息视图。一个**日志段**会对应一个**原始消息视图**（日志追加可变）。同时可以设置 start、end，就可以提供不可变的消息视图。如果 isSlice 为 false，说明消息视图是原始消息视图，否则如果为 true 是消息视图。

我们先来看看其初始化，源码如下：

public class FileRecords extends AbstractRecords implements Closeable {

private final boolean isSlice; // 是否为分片。isSlice为false，说明文件是原始的日志文件，是可以追加的；如果isSlice为true，那么就是截取日志的一个片段。

private final int start; // 分片的开始位置。

private final int end; // 分片的结束位置。

private final Iterable<FileLogInputStream.FileChannelRecordBatch> batches; // 组成 FileRecords的消息批次。

// mutable state

private final AtomicInteger size; // 如果是分片，则表示分片的大小（end - start）；如果不是分片，则表示整个日志文件的大小。

private final FileChannel channel; // FileChannel 类型，用于读写对应的日志文件。

private volatile File file; // 磁盘上的日志文件。

/\*\*

\* The {@code FileRecords.open} methods should be used instead of this constructor whenever possible.

\* The constructor is visible for tests.

\*/

FileRecords(File file,

FileChannel channel,

int start,

int end,

boolean isSlice) throws IOException {

this.file = file;

this.channel = channel;

this.start = start;

this.end = end;

this.isSlice = isSlice;

this.size = new AtomicInteger();

if (isSlice) {

// don't check the file size if this is just a slice view

size.set(end - start);

} else {

if (channel.size() > Integer.MAX\_VALUE)

throw new KafkaException("The size of segment " + file + " (" + channel.size() +

") is larger than the maximum allowed segment size of " + Integer.MAX\_VALUE);

int limit \= Math.min((int) channel.size(), end);

size.set(limit - start);

// if this is not a slice, update the file pointer to the end of the file

// set the file position to the last byte in the file

channel.position(limit);

}

batches = batchesFrom(start);

}

....

}

我们来梳理下其重要字段，如下：

1.  **isSlice**：是否为分片。isSlice 为 false，说明文件是原始的日志文件，是可以追加的；如果为 true，那么就是截取日志的一个片段。
2.  **start**：分片的开始位置。
3.  **end**：分片的结束位置。
4.  **batches**：组成 FileRecords 的消息批次。
5.  **size**：如果是分片，则表示分片的大小（end - start）；如果不是分片，则表示整个日志文件的大小。
6.  **channel**：FileChannel 类型，用于读写对应的日志文件。
7.  **file**：磁盘上的日志文件。

## **07 FileRecords 重要方法**

## **7.1 append()**

先来看看**把消息批次从内存写到日志文件中**的方法，源码如下：

/\*\*

\* Append a set of records to the file. This method is not thread-safe and must be

\* protected with a lock.

\*

\* @param records The records to append

\* @return the number of bytes written to the underlying file

\*/

public int append(MemoryRecords records) throws IOException {

if (records.sizeInBytes() > Integer.MAX\_VALUE - size.get())

throw new IllegalArgumentException("Append of size " + records.sizeInBytes() +

" bytes is too large for segment with current file position at " + size.get());

// 1、把内存中的消息数据写入 channel，即写入 PageCache 中，需要后面刷盘

int written \= records.writeFullyTo(channel);

// 2、计算写入了多少字节的消息

size.getAndAdd(written);

return written;

}

该方法比较简单，就两步：

1.  把内存中的消息数据写入 channel，即写入「**PageCache**」中，需要后面刷盘。
2.  计算写入了多少字节的消息，并返回。

## **7.2 flush()**

接下来看看**把消息刷写到磁盘**的方法，源码如下：

/\*\*

\* Commit all written data to the physical disk

\*/

public void flush() throws IOException {

channel.force(true);

}

上面的 append 方法并没有把消息写入到磁盘文件中，只是先写到操作系统的 「**PageCache**」中，而是通过 flush 方法将消息刷到磁盘里的。

flush 底层采用了 「**FileChannel**」来实现，主要考虑到性能问题，操作系统会将数据缓存到内存中「**PageCache**」，所以无法保证写入到 「**FileChannel**」里的数据一定及时地写到磁盘上，要保证写到磁盘就要调用 force 方法强制刷盘。

##   
**7.3 slice()**

接下来看看**获取从指定位置读取指定大小的消息集合**的方法，源码如下：

/\*\*

\* 从给定位置以及给定大小创建一个新的 FileRecords 对象，表示原记录对象的前缀

\*

\* @param position 关于原始数据的偏移量

\* @param size 被选中数据集的大小

\* @throws IOException

\* @returns 从偏移量开始的给定大小的记录对象

\*/

public FileRecords slice(int position, int size) throws IOException {

// 1、先缓存当前对象的大小，以防写操作更改它

int currentSizeInBytes \= sizeInBytes();

// 2、校验位置和大小合法性

if (position < 0)

throw new IllegalArgumentException("Invalid position: " + position + " in read from " + this);

if (position > currentSizeInBytes - start)

throw new IllegalArgumentException("Slice from position " + position + " exceeds end position of " + this);

if (size < 0)

throw new IllegalArgumentException("Invalid size: " + size + " in read from " + this);

// 3、计算新文件记录对象的结束位置

int end \= this.start + position + size;

// 如果结束位置超过了文件末尾，则将其持平

if (end < 0 || end > start + currentSizeInBytes)

end = start + currentSizeInBytes;

// 4、创建新的记录对象

return new FileRecords(file, channel, this.start + position, end, true);

}

该方法也比较简单，就四步：

1.  先缓存当前对象的大小，以防写操作更改它。
2.  校验位置和大小合法性，否则抛异常。
3.  计算新文件记录对象的结束位置，如果结束位置超过了文件末尾，则将其持平。
4.  创建新的记录对象并返回。

## **7.4 searchForOffsetWithSize()**

接下来看看**获取从指定位置线性扫码日志文件**的方法，源码如下：

/\*\*

\* 在指定的位置开始扫描数据并线性搜索偏移量不小于目标偏移量的第一条记录批次，

\* 并返回该记录批次的日志位移量和其在文件中的位置

\*

\* @param targetOffset 目标偏移量

\* @param startingPosition 指定扫描的起始位置

\* @return 日志位移量和记录批次的文件绝对位置

\*/

public LogOffsetPosition searchForOffsetWithSize(long targetOffset, int startingPosition) {

//从指定位置开始，线性搜索数据

for (FileChannelRecordBatch batch : batchesFrom(startingPosition)) {

long offset \= batch.lastOffset(); //当前批次的最后一个偏移量

if (offset >= targetOffset) //偏移量不小于目标偏移量

return new LogOffsetPosition(offset, batch.position(), batch.sizeInBytes()); //返回此时的偏移量及位置

}

return null; //未找到偏移量不小于目标偏移量的记录批次时，返回 \`null\`

}

## **7.5 truncateTo()**

最后，我们来看看**截取日志**的方法，源码如下：

/\*\*

\* 将日志段截断至指定大小

\*

\* @param targetSize 截断后目标大小

\* @throws IOException

\* @returns 返回截断前和截断后的大小差值

\*/

public int truncateTo(int targetSize) throws IOException {

int originalSize \= sizeInBytes();

// 1、目标大小不能超过原始大小或小于0

if (targetSize > originalSize || targetSize < 0)

throw new KafkaException("Attempt to truncate log segment " + file + " to " + targetSize + " bytes failed, " + " size of this log segment is " + originalSize + " bytes.");

// 2、如果目标大小小于文件大小，则截断文件

if (targetSize < (int) channel.size()) {

channel.truncate(targetSize);

size.set(targetSize); // 更新记录中的文件大小属性

}

return originalSize - targetSize; // 3、返回截断前和截断后的大小差值

}

该方法也比较简单，就三步：

1.  目标大小不能超过原始大小或小于0，否则抛异常。
2.  如果目标大小小于文件大小，则截断文件。这里调用 「**FileChannel**」的 truncate 方法进行切割。
3.  最后返回截断前和截断后的大小差值。

## **08 总结**

这里，我们一起来总结一下这篇文章的重点。

1、通过上一篇的分析我们引出了服务端日志段的管理对象之 「**LogSegment**」。

2、带你深度剖析了日志在物理存储上最小的单位日志段文件 「**LogSegment**」初始化以及重要参数 。

3、接着带你深度剖析了日志在物理存储上最小的单位日志段文件「**LogSegment**」的重要方法。

4、最后剖析了映射日志分段文件的类「**FileRecords**」的重要方法。

下篇我们来深度剖析「**Log 日志架构设计**」，大家期待，我们下期见。