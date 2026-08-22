大家好，我是 **华仔**, 又跟大家见面了。

上一篇中，主要带大家深度剖析了 「**Kafka 服务端源码 LogSegment 日志段架构设计**」，通过「**场景驱动方式**」，现在消息被封装成批次请求已经从「**生产者**」发送到「**Broker**」，且被「**网络层**」所接收到并准备进行消息数据存储，从今天开始，我们来深度剖析 Kafka 日志系统的底层实现，这是日志系列第六篇，我们继续来深度聊聊「**Kafka 服务端源码之日志 Log 对象操作**」，看看 Kafka 服务端是如何真正存储消息数据的。

![](https://article-images.zsxq.com/FnviCeGz-UAUYa6JGLT7E-ljOBn-)

## **01 总体概述**

在上上一篇中，我们 [【服务端 Broker 源码分析系列第十一篇】图解 Kafka 源码之日志段 LogSegment 架构设计](https://articles.zsxq.com/id_t4f0eut7oe94.html) 引出了服务端日志的管理对象之 「**Log**」。

在我看来，「**Log**」对象是 Kafka Broker 端源码最重要核心的部分，没有之一。所以我们一定要掌握这部分源码的实现，对解决线上问题很有帮助。

接下来我们来对 「**Log**」对象一探究竟，从名字上看就知道它是「**日志管理组件**」，**日志是日志段的管理容器**，内部定义了很多关于日志段的管理操作。每个日志段文件以分段文件内「**第一个消息的偏移量**」为基础偏移量「**baseOffset**」，并以此作为日志段文件命名规则。

好了，下面开始剖析其源码实现，让你对整个日志管理有个总体的认知，深度剖析下其内部是如何进行日志管理的，整个流程是怎么样的？那么带着这些问题进入今天的正题。

整个日志管理的相关组件的调用关系图如下：

![](https://article-images.zsxq.com/FgVmpr3JOm-_1PVfCHJC94zu4_tC)

在上篇[【服务端 Broker 源码分析系列第十二篇】图解 Kafka 源码之日志 Log 架构设计](https://articles.zsxq.com/id_ph1gx4aq04sq.html)中，我们剖析了 「**Log**」类的部分源码。今天我们来剖析了 「**Log**」对象的常见操作以及源码实现。

## **02 Log 对象常见操作梳理**

在上一篇的学习中，在 「**Log**」类对象初始化流程中，我们了解到了其内部包含一些关键属性。主要包括「**LogStartOffset 起始位移**」、「**HighWatermark 高水位**」、「**LogEndOffset 末端位移**」，另外还剖析了 「**LogSegment**」日志段加载过程，既然是管理日志段，势必会有「**日志读写操作**」。所以我们可以将 Log 对象操作大致分为 4 大部分，下面通过一张图来说明下：

![](https://article-images.zsxq.com/FqemdSIkkB25bj6LBgtoLwpq-a6I)

接下来我们就会按照上面的顺序挨个来讲解，这里你需要特别注意的是「**高水位管理**」。

我们知道在 Kafka 的版本变迁的过程中，日志功能的很多改进都是基于「**高水位机制**」来演进的。比如 Kafka 中的 KIP-101 提案正式引入大名鼎鼎的 「**LeaderEpoch 机制**」就是为了**替代日志截断操作中的高水位的**。所以为了后面篇章更好的学习 「**LeaderEpoch 机制**」，你需要了解当前「**高水位机制**」的弊端是什么。

## **03 高水位管理操作**

在剖析高水位管理操作之前，我们得先了解下什么是「**高水位**」，来看看它的定义：

/\*\*

\* Keep track of the current high watermark in order to ensure that segments containing offsets at or above it are

\* not eligible for deletion. This means that the active segment is only eligible for deletion if the high watermark

\* equals the log end offset (which may never happen for a partition under consistent load). This is needed to

\* prevent the log start offset (which is exposed in fetch responses) from getting ahead of the high watermark.

\*/

@volatile private var highWatermarkMetadata: LogOffsetMetadata = LogOffsetMetadata(logStartOffset)

从上面定义可以得出：

1.  首先，「**高水位**」值是通过关键词 [volatile](http://volatile/) 来修饰的，说明可能同时会有多个线程来读取它，用此关键词来保证其「**内存可见性**」。
2.  高水位的初始值是「**LogStartOffset 起始位移**」，这是因为每个 「**Log**」对象都会维护一个「**LogStartOffset**」，当然「**高水位**」也不例外，所以当首次构建「**高水位**」时，它会被赋值成 「**LogStartOffset**」。

另外在初始化时，有个 「**LogOffsetMetadata**」对象，它又是干什么的呢？我们一起来看下：

「**LogOffsetMetadata**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/Lo](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LogSegment.scala)gOffsetMetadata[.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/LogOffsetMetadata.scala)，所有跟日志相关的源码都在 kafka/log 这个包下面。

/\*

\* A log offset structure, including:

\* 1. the message offset

\* 2. the base message offset of the located segment

\* 3. the physical position on the located segment

\*/

case class LogOffsetMetadata(messageOffset: Long,

segmentBaseOffset: Long = Log.UnknownOffset,

relativePositionInSegment: Int = LogOffsetMetadata.UnknownFilePosition) {

....

}

从类定义上看，有**三个非常重要**的变量：

1.  **messageOffset**：这是最重要的字段信息，它指的是：**消息位移值**。我们所说的高水位值，其实指的就是该变量的值。
2.  **segmentBaseOffset**：**它保存了该位移值所在日志段的起始位移**，另外它还用来判断两条消息是否**在同一个日志段中**。这个日志段起始位移值其实是用来计算两条消息在物理磁盘文件中实际位置的差值，即中间隔了多少字节，所以必须在同一个日志段对象上计算才有意义。
3.  **relativePositionInSegment**：**它保存了该位移值所在日志段的实际物理磁盘的位置信息**。同上一个字段，它是用来计算两个位移值之间的物理磁盘位置差值的。这个差值的作用在读取日志的时候就会被用到，需要关心两个位移之间所有消息的总字节数是否超过了要读取的最大大小。

这个类的所有方法都是围绕这 3 个变量来展开的，我们来看下其方法，都比较简单。

// check if this offset is already on an older segment compared with the given offset

// 判断当前 LogOffsetMetadata 实例是不是比参数 代表的 LogOffsetMetadata 实例对应的日志段更旧

def onOlderSegment(that: LogOffsetMetadata): Boolean = {

if (messageOffsetOnly)

throw new KafkaException(s"$this cannot compare its segment info with $that since it only has message offset info")

this.segmentBaseOffset < that.segmentBaseOffset

}

// check if this offset is on the same segment with the given offset

// 判断当前 LogOffsetMetadata 实例是不是和参数代表的 LogOffsetMetadata 实例处在同一个日志段中。

def onSameSegment(that: LogOffsetMetadata): Boolean = {

if (messageOffsetOnly)

throw new KafkaException(s"$this cannot compare its segment info with $that since it only has message offset info")

this.segmentBaseOffset == that.segmentBaseOffset

}

// compute the number of messages between this offset to the given offset

// 计算当前 LogOffsetMetadata 实例和参数代表的 LogOffsetMetadata 实例之间的消息数量差。

def offsetDiff(that: LogOffsetMetadata): Long = {

this.messageOffset - that.messageOffset

}

// compute the number of bytes between this offset to the given offset

// if they are on the same segment and this offset precedes the given offset

// 如果当前 LogOffsetMetadata 实例和参数代表的 LogOffsetMetadata 实例处在同一个日志段中且当前实例代表的消息发生在那之前，就计算它们之间的字节位置偏移量

def positionDiff(that: LogOffsetMetadata): Int = {

if(!onSameSegment(that))

throw new KafkaException(s"$this cannot compare its segment position with $that since they are not on the same segment")

if(messageOffsetOnly)

throw new KafkaException(s"$this cannot compare its segment position with $that since it only has message offset info")

this.relativePositionInSegment - that.relativePositionInSegment

}

// decide if the offset metadata only contains message offset info

// 判断当前 LogOffsetMetadata 实例是否仅包含消息偏移量信息

def messageOffsetOnly: Boolean = {

segmentBaseOffset == Log.UnknownOffset && relativePositionInSegment == LogOffsetMetadata.UnknownFilePosition

}

override def toString \= s"(offset=$messageOffset segment=\[$segmentBaseOffset:$relativePositionInSegment\])"

接下来，我们把视角切回「**高水位**」，来看下高水位管理的相关操作。

## **3.1 获取高水位**

获取「**高水位**」非常简单，就是从「**高水位**」元数据中获取消息的位移值。

// 读取高水位的位移值

def highWatermark: Long = highWatermarkMetadata.messageOffset

## **3.2 设置高水位元数据**

// 设置高水位元数据

private def updateHighWatermarkMetadata(newHighWatermark: LogOffsetMetadata): Unit = {

// 1、判断高水位值是不是负数，否则抛异常

if (newHighWatermark.messageOffset < 0)

throw new IllegalArgumentException("High watermark offset should be non-negative")

// 2、保护 Log 对象修改的 Monitor 锁

lock synchronized {

// 赋值新的高水位值

highWatermarkMetadata = newHighWatermark

// 通知生产者的状态管理器更新高水位偏移量

producerStateManager.onHighWatermarkUpdated(newHighWatermark.messageOffset)

// 尝试增加第一个不稳定偏移量的值。以便在后续的操作中保证事务在提交前同步到磁盘上的数据不会被回滚。该方法首先检查是否关闭了内存映射缓冲，然后获取 ProducerStateManager 实例中缓存的第一个不稳定偏移量元数据，并根据当前日志段的起始偏移量对该元数据进行更新。最后更新 firstUnstableOffsetMetadata 元数据缓存。

maybeIncrementFirstUnstableOffset()

}

trace(s"Setting high watermark $newHighWatermark")

}

## **3.3 更新高水位**

在源码中定义了两种更新高水位的方法，我们来分别看下就了解了为什么会定义两个方法。

### **3.3.1 updateHighWatermark()**

该方法主要用在**Follower 副本从 Leader 副本获取到消息后更新高水位值**。当一旦拿到新的消息，就必须要更新高水位值。

// 更新高水位值，新高水位值一定介于\[Log Start Offset，Log End Offset\]之间

def updateHighWatermark(hw: Long): Long = {

// 如果新的高水位偏移量小于当前日志段的起始偏移量，则将新的高水位偏移量设为当前日志段的起始偏移量。

val newHighWatermark \= if (hw < logStartOffset)

logStartOffset

// 如果新的高水位偏移量大于当前日志段的末端偏移量，则将新的高水位偏移量设为当前日志段的末端偏移量。

else if (hw > logEndOffset)

logEndOffset

// 否则，将新的高水位偏移量设为输入值 hw。

else

hw

// 以新的高水位偏移量为参数更新高水位元数据

updateHighWatermarkMetadata(LogOffsetMetadata(newHighWatermark))

// 最后返回新高水位值

newHighWatermark

}

### **3.3.2 maybeIncrementHighWatermark()**

该方法主要是用在**更新 Leader 副本的高水位值**，需要注意的是，**Leader 副本高水位值更新是需要条件的**。当 Producer 端向 Leader 副本写入消息时，分区的高水位值就可能不需要更新，因为它可能需要等待其他 Follower 副本同步的进度。

// 尝试增加高水位偏移量元数据，如果成功增加了高水位偏移量元数据，则返回先前的高水位元数据

def maybeIncrementHighWatermark(newHighWatermark: LogOffsetMetadata): Option\[LogOffsetMetadata\] = {

// 新高水位值不能越过 LogEndOffset，否则抛异常

if (newHighWatermark.messageOffset > logEndOffset)

throw new IllegalArgumentException(s"High watermark $newHighWatermark update exceeds current " + s"log end offset $logEndOffsetMetadata")

// 保护 Log 对象修改的 Monitor 锁

lock.synchronized {

// 获取旧的高水位值

val oldHighWatermark \= fetchHighWatermarkMetadata

// 首先，如果新的高水位偏移量大于旧的高水位偏移量，则更新高水位元数据为新的高水位元数据，并返回旧的高水位元数据

// 其次，如果旧的高水位元数据与新的高水位元数据相等并且旧的高水位元数据比新的高水位元数据旧，则更新高水位元数据为新的高水位元数据，并返回旧的高水位元数据

if (oldHighWatermark.messageOffset < newHighWatermark.messageOffset ||

(oldHighWatermark.messageOffset == newHighWatermark.messageOffset && oldHighWatermark.onOlderSegment(newHighWatermark))) {

// 更新高水位元数据

updateHighWatermarkMetadata(newHighWatermark)

// 返回老的高水位值

Some(oldHighWatermark)

} else {

// 如果新的高水位元数据不大于旧的高水位元数据，则返回 None

None

}

}

}

##   
**3.4 读取高水位**

/\*\*

\* Get the offset and metadata for the current high watermark. If offset metadata is not

\* known, this will do a lookup in the index and cache the result.

\*/

private def fetchHighWatermarkMetadata: LogOffsetMetadata = {

// 1、读取时确保日志不能被关闭

checkIfMemoryMappedBufferClosed()

// 2、保存当前高水位值到本地变量，避免多线程访问干扰

val offsetMetadata \= highWatermarkMetadata

// 3、当没有获得到完整的高水位元数据

if (offsetMetadata.messageOffsetOnly) {

lock.synchronized {

// 通过读日志文件的方式把完整的高水位元数据信息拉出来

val fullOffset \= convertToOffsetMetadataOrThrow(highWatermark)

// 然后再更新一下高水位对象

updateHighWatermarkMetadata(fullOffset)

// 返回完整的高水位元数据信息

fullOffset

}

} else {

// 否则直接返回

offsetMetadata

}

}

/\*\*

\* 将指定的偏移量转换为对应的 offset 元数据

\* Given a message offset, find its corresponding offset metadata in the log.

\* If the message offset is out of range, throw an OffsetOutOfRangeException

\*/

private def convertToOffsetMetadataOrThrow(offset: Long): LogOffsetMetadata = {

// 从偏移量处读取消息数据，并从读取的数据中提取出 offset 元数据

val fetchDataInfo \= read(offset,

maxLength = 1, // maxLength 参数表示最多读取的字节数

isolation = FetchLogEnd, // isolation 参数表示读取数据时应如何隔离消息

minOneMessage = false) // minOneMessage 参数表示是否最好检索出至少一个消息，如果没有则抛出异常

// 从读取到的数据中获取 offset 元数据，并返回该值。

fetchDataInfo.fetchOffsetMetadata

}

这里不仅仅只获取高水位值，还要获取高水位的其他元数据信息，比如：「**日志段起始位移**」、「**日志段物理位置信息**」。

##   
**04 关键位移值管理操作**

「**Log**」对象内部维护了一些关键位移值数据，主要包括「**LogStartOffset 起始位移**」、「**HighWatermark 高水位**」、「**LogEndOffset 末端位移**」。高水位在上一小节已经剖析了，我们看下另外 2 个位移值。

还记得上一篇中，关于「**LogStartOffset 起始位移**」、「**LogEndOffset 末端位移**」的区别吗？这里再借用下。

  
![](https://article-images.zsxq.com/FoCEiVVg1n9HKd722ahrD1qfZnCB)

从上图得出，位移值 12 的虚线方框，表示：**Log 对象中的 LEO 值永远指向下一条待写入消息**，**简单说 LEO 上是没有消息的**。来看下其源码定义：

@volatile private var nextOffsetMetadata: LogOffsetMetadata = \_

这里的 「**nextOffsetMetadata**」可以认为就是所说的 「**LogEndOffset**」简称 「**LEO**」，其类型为 LogOffsetMetadata 对象。

在上一篇 [【服务端 Broker 源码分析系列第十二篇】图解 Kafka 源码之日志 Log 架构设计](https://articles.zsxq.com/id_ph1gx4aq04sq.html) 中 Log 对象初始化的时候，会加载所有日志段对象，会计算出当前 Log 的下一条消息位移值。之后 Log 对象将此位移值赋值给「**LEO**」，源码如下：

locally {

// 创建 Log 对象对应的分区日志路径，保存 Log 对象磁盘文件

Files.createDirectories(dir.toPath)

// 初始化Leader Epoch缓存

initializeLeaderEpochCache()

// 加载所有日志段对象，并返回该 Log 对象下一条消息的位移值

val nextOffset \= loadSegments()

// 初始化 LEO 元数据对象，LEO 值为上一步获取的位移值

nextOffsetMetadata = LogOffsetMetadata(nextOffset, activeSegment.baseOffset, activeSegment.size)

....

}

## **4.1 updateLogEndOffset()**

// 更新日志段的末端偏移量，并同时更新其他相关元数据，例如高水位偏移量和恢复点偏移量等

private def updateLogEndOffset(offset: Long): Unit = {

// 构造一个新的 offset 元数据 LogOffsetMetadata 对象，参数分别是：给定的偏移量、当前活动日志段的基本偏移量 baseOffset、当前活动日志段的大小。

nextOffsetMetadata = LogOffsetMetadata(offset, activeSegment.baseOffset, activeSegment.size)

// 如果已知的高水位偏移量大于等于给定的偏移量，则更新高水位元数据信息

if (highWatermark >= offset) {

updateHighWatermarkMetadata(nextOffsetMetadata)

}

// 如果恢复点偏移量大于给定的偏移量，则将恢复点更新为给定的偏移量。

if (this.recoveryPoint > offset) {

this.recoveryPoint = offset

}

}

这里需要注意的是，**如果在更新过程中发现高水位值大于新的 LEO 值**，**那么此时 Kafka 还会更新高水位值**，**这主要是因为对于同一个 Log 对象来说**，**其高水位值是永远不能超过 LEO 值的**。切记再切记！

## **4.2 updateLogStartOffset()**

// 更新日志段的起始偏移量，并同时更新其他相关元数据，例如高水位偏移量和恢复点偏移量等

private def updateLogStartOffset(offset: Long): Unit = {

// 将日志段的起始偏移量设置为给定的偏移量。

logStartOffset = offset

// 如果已有的高水位偏移量小于给定的偏移量，则更新高水位。

if (highWatermark < offset) {

updateHighWatermark(offset)

}

// 如果恢复点偏移量小于给定的偏移量，则将恢复点更新为给定的偏移量

if (this.recoveryPoint < offset) {

this.recoveryPoint = offset

}

}

当更新 「**LSO**」时，如果高水位小于当前给定的位移值时，也是需要更新高水位的，切记此时高水位一定要比当前位移值大。

接下来，我们来总结下， 「**Log**」对象何时会更新 「**LEO**」和 「**LSO**」呢？这是我们学习源码时的重点。

##   
**4.3 更新 LEO 时机**

通过追踪源码，「**LEO**」 对象被更新的时机大概有以下 4 个。

1.  **当**「**Log**」**对象初始化时**：必须要创建一个 「**LEO**」 对象，并对其进行初始化。![](https://article-images.zsxq.com/FsaZAytUs4SMySmlkj3oY1o8kE-c)
2.  **当写入新的日志消息时**：这个很容易理解。当不断向「**Log**」对象插入新的日志消息时，「**LEO**」 值需要不断地增加。![](https://article-images.zsxq.com/FtMsd2bK3NHCYibHIbE7siy8h9L9)
3.  **当**「**Log**」**对象发生日志切分时**：那么什么是日志切分呢？说白了就是创建了一个**全新的日志段对象且关闭当前写入的日志段对象**，**这个通常发生在当前日志段对象已经写满时**。一旦发生日志切分，说明「**Log**」对象切换了 Active Segment，那么此时「**LEO**」中的起始位移值和段大小数据都需要被更新，所以此时我们必须要更新「**LEO**」对象。![](https://article-images.zsxq.com/Fm6ahu0mc6PQ_p0_4hPiSqXvSJjP)
4.  **当**「**Log**」**对象发生日志截断时**：一旦日志中的部分消息被删除，可能会导致 「**LEO**」发生变化，因此有必要更新「**LEO**」对象。![](https://article-images.zsxq.com/FvMpDRx4OHPtN5Z83uhVgrCAxunR)

## **4.4 更新 LSO 时机**

从 4.2 小节更新「**LSO**」源码可以得出其不是一个对象，就是一个长整型值而已，所以它的更新相对要比更新「**LEO**」简单的多。

通过追踪源码，「**LSO**」 对象被更新的时机大概也有以下 5 个。

1.  **当**「**Log**」**对象初始化时**：和「**LEO**」类似， 「**Log**」对象初始化时要给 LogStartOffset 赋值。 必须要创建一个 「**LEO**」 对象，并对其进行初始化。![](https://article-images.zsxq.com/FhGEvEhJn7MTXlxChdmeeORd84Kf)
2.  **当**「**Log**」**对象发生日志截断时**：一旦日志中的部分消息被删除，可能会导致 「**LEO**」发生变化，因此有必要更新「**LEO**」对象。![](https://article-images.zsxq.com/Ft8wIPZ_rouJhQBzb2a4VY3vj3HK)
3.  **当 Follower 副本同步时**：一旦 Leader 副本的 Log 对象的 Log Start Offset 值发生变化。为了保证和 Leader 副本的一致性，Follower 副本也需要去更新该值。![](https://article-images.zsxq.com/Fq91fE8hMtPO96DUh5OZ97haGfJ8)
4.  **当删除日志段时**：这个和日志截断是类似的。凡是涉及消息删除的操作都有可能导致 Log Start Offset 值的变化。![](https://article-images.zsxq.com/Fqs8RvJP4PxrUhXrtEcDXq7t_XOw)
5.  **当删除消息时**：严格来说，这个更新时机有点本末倒置了。在 Kafka 中，删除消息就是通过提高 Log Start Offset 值来实现的，因此，删除消息时必须要更新该值。

## **05 日志段管理操作**

在上一篇中，我们剖析了日志段「**LogSegment**」的加载过程，今天我们来看看日志段的管理操作。首先你要记住日志「**Log**」是日志段「**LogSegment**」的容器，那么它底层到底是如何承担起容器职责的呢？

/\* the actual segments of the log \*/

private val segments: ConcurrentNavigableMap\[java.lang.Long, LogSegment\] = new ConcurrentSkipListMap\[java.lang.Long, LogSegment\]

从上面定义可以看到，源码使用 Java 的 [ConcurrentSkipListMap](http://concurrentskiplistmap%20/) 类来保存所有日志段对象。

ConcurrentSkipListMap 有 2 个明显的优势，如下：

1.  **首先它是线程安全的**，这样 Kafka 源码不需要自行确保日志段操作过程中的线程安全。
2.  **其次它的键值是可排序**，Kafka 将每个日志段的起始位移值「**LogStartOffset**」作为 Key，这样就可以很方便地根据所有日志段的起始位移值来排序和比较，同时又能快速地找到与给定位移值相近的前后两个日志段的位置。

## **5.1 新增日志段**

/\*\*

\* Add the given segment to the segments in this log. If this segment replaces an existing segment, delete it.

\* @param segment The segment to add

\*/

@threadsafe

def addSegment(segment: LogSegment): LogSegment = this.segments.put(segment.baseOffset, segment)

该方法超级简单，就是调用 Map 的 put 方法将给定的日志段对象添加到 segments 中，另外它是线程安全的，无需担心**多线程并发写入会出问题**。

## **5.2 修改日志段**

其实源码里并没有修改日志段对象的方法，所谓的修改或更新就是**复用 addSegment** 方法来实现替换已有日志段的方法，用新的日志段对象替换老的日志段对象。

## **5.3 读取日志段**

源码中查询的方法有很多，底层主要就是利用了 [ConcurrentSkipListMap](http://concurrentskiplistmap/) 的现成方法，都比较简单。

// 获取第一个日志段对象

segments.firstEntry

// 获取第一个日志段对象的baseOffset

segments.firstEntry.getValue.baseOffset

// 获取最后一个日志段对象，即 Active Segment。

def activeSegment \= segments.lastEntry.getValue

// 获取第一个起始位移值≥给定 Key 值的日志段对象

segments.higherEntry(segmentEntry.getKey)

// 获取最后一个起始位移值≤给定 Key 值的日志段对象。

segments.floorEntry(offset)

## **5.4 删除日志段**

删除操作相对来说会复杂一点。我们都知道 Kafka 有很多留存策略，包括基于「**时间维度**」、基于「**空间维度**」、基于「**LogStartOffset 维度**」三种。

那么究竟什么是「**留存策略**」呢？说白了，它就是**根据一定的规则决定哪些日志段可以删除**。

从源码角度来看，「**Log**」中删除操作的总入口是 deleteOldSegments 无参方法，如下：

/\*\*

\* If topic deletion is enabled, delete any log segments that have either expired due to time based retention

\* or because the log size is > retentionSize.

\* Whether or not deletion is enabled, delete any log segments that are before the log start offset

\*/

def deleteOldSegments(): Int = {

if (config.delete) {

// 基于「时间维度」+ 基于「空间维度」+ 基于「LogStartOffset 维度」

deleteRetentionMsBreachedSegments() + deleteRetentionSizeBreachedSegments() + deleteLogStartOffsetBreachedSegments()

} else {

// 基于「LogStartOffset 维度」

deleteLogStartOffsetBreachedSegments()

}

}

// 基于「时间维度」删除策略

private def deleteRetentionMsBreachedSegments(): Int = {

if (config.retentionMs < 0) return 0

val startMs \= time.milliseconds

def shouldDelete(segment: LogSegment, nextSegmentOpt: Option\[LogSegment\]): Boolean = {

// 如果当前时间 减去 日志段的最大时间 大于配置的最大时间间隔，则删除

startMs - segment.largestTimestamp > config.retentionMs

}

// 删除日志段，带参数版

deleteOldSegments(shouldDelete, RetentionMsBreach)

}

// 基于「空间维度」删除策略

private def deleteRetentionSizeBreachedSegments(): Int = {

if (config.retentionSize < 0 || size < config.retentionSize) return 0

// 计算超过最大大小多少字节

var diff \= size - config.retentionSize

def shouldDelete(segment: LogSegment, nextSegmentOpt: Option\[LogSegment\]): Boolean = {

if (diff - segment.size >= 0) {

diff -= segment.size

true

} else {

false

}

}

// 删除日志段，带参数版

deleteOldSegments(shouldDelete, RetentionSizeBreach)

}

// 基于「LogStartOffset 维度」删除策略

private def deleteLogStartOffsetBreachedSegments(): Int = {

def shouldDelete(segment: LogSegment, nextSegmentOpt: Option\[LogSegment\]): Boolean = {

nextSegmentOpt.exists(\_.baseOffset <= logStartOffset)

}

// 删除日志段，带参数版

deleteOldSegments(shouldDelete, StartOffsetBreach)

}

下面这张图展示了 Kafka 当前的三种日志留存策略，以及底层涉及到日志段删除的所有方法：

![](https://article-images.zsxq.com/FrgXRzZeurmh_ExRuNPsuH-seXbY)

从上面源码可以看到，三种维度底层都调用了**带参数版的** deleteOldSegments 方法，我们来看下：

private def deleteOldSegments(predicate: (LogSegment, Option\[LogSegment\]) => Boolean,

reason: SegmentDeletionReason): Int = {

lock synchronized {

// 1、使用传入的函数计算哪些日志段对象能够被删除

val deletable \= deletableSegments(predicate)

// 2、如果存在可删除的日志段对象

if (deletable.nonEmpty)

// 调用 deleteSegments 方法删除这些日志段。

deleteSegments(deletable, reason)

else

0

}

}

该方法也非常简单，两个步骤：

1.  使用传入的函数计算哪些日志段对象能够被删除。
2.  如果存在可删除的日志段对象调用 deleteSegments 方法删除这些日志段。

接下来，我们来看下第一步调用的 deletableSegments 这个方法的源码。

private def deletableSegments(predicate: (LogSegment, Option\[LogSegment\]) => Boolean): Iterable\[LogSegment\] = {

// 如果当前压根就没有任何日志段对象，直接返回

if (segments.isEmpty) {

Seq.empty

} else {

val deletable \= ArrayBuffer.empty\[LogSegment\]

// 获取第一个日志段对象

var segmentEntry \= segments.firstEntry

// 从具有最小起始位移值的日志段对象开始遍历，直到满足以下条件之一便停止遍历：

// 1. 扫描到包含Log对象高水位值所在的日志段对象

// 2. 测定条件函数predicate = false

// 3. 最新的日志段对象不包含任何消息

// 最新日志段对象是 segments 中Key值最大对应的那个日志段，也就是我们常说的 Active Segment。完全为空的 Active Segment 如果被允许删除，后面还要重建它，故代码这里不允许删除大小为空的Active Segment。

while (segmentEntry != null) {

// 获取日志段数据

val segment \= segmentEntry.getValue

// 第一个起始位移值≥给定 Key 值的日志段对象

val nextSegmentEntry \= segments.higherEntry(segmentEntry.getKey)

val (nextSegment, upperBoundOffset, isLastSegmentAndEmpty) = if (nextSegmentEntry != null)

(nextSegmentEntry.getValue, nextSegmentEntry.getValue.baseOffset, false)

else

(null, logEndOffset, segment.size == 0)

// 必须满足这三个条件才能删除

if (highWatermark >= upperBoundOffset && predicate(segment, Option(nextSegment)) && !isLastSegmentAndEmpty) {

deletable += segment

segmentEntry \= nextSegmentEntry

} else {

segmentEntry = null

}

}

deletable

}

}

接下来，我们来看下第二步调用的 deleteSegments 执行真正的日志段删除操作。

private def deleteSegments(deletable: Iterable\[LogSegment\], reason: SegmentDeletionReason): Int = {

maybeHandleIOException(s"Error while deleting segments for $topicPartition in dir ${dir.getParent}") {

val numToDelete \= deletable.size

if (numToDelete > 0) {

// we must always have at least one segment, so if we are going to delete all the segments, create a new one first

// 不允许删除所有日志段对象。如果一定要做，先创建出一个新的来，然后再把前面N个删掉

if (segments.size == numToDelete)

roll()

lock synchronized {

// 确保 Log 对象没有被关闭

checkIfMemoryMappedBufferClosed()

// remove the segments for lookups

// 删除给定的日志段对象以及底层的物理文件

removeAndDeleteSegments(deletable, asyncDelete = true, reason)

// 尝试更新日志的Log Start Offset值

maybeIncrementLogStartOffset(segments.firstEntry.getValue.baseOffset, SegmentDeletion)

}

}

// 返回删除的日志段

numToDelete

}

}

这里我稍微解释一下，为什么要在删除日志段对象之后，尝试更新「**LogStartOffset**」值。「**LogStartOffset**」值是**整个 Log 对象对外可见消息的最小位移值**。如果我们删除了日志段对象，很有可能对外可见消息的范围发生了变化，所以需要看下是否更新 Log Start Offset 值。

## **5.5 切分日志段**

先来看下入口方法：maybeRoll()，这个方法的功能是：**如果需要的话**，**就滚动建立一个日志段文件**，**无论是否新建文件都返回当前的 Segment**。

private def maybeRoll(messagesSize: Int, appendInfo: LogAppendInfo): LogSegment = {

// 1、活跃的 Segment，也就是最后一个 Segment。

val segment \= activeSegment

val now \= time.milliseconds

val maxTimestampInMessages \= appendInfo.maxTimestamp

val maxOffsetInMessages \= appendInfo.lastOffset

// 2、是否需要新建一个 LogSegment 对象。

if (segment.shouldRoll(RollParams(config, appendInfo, messagesSize, now))) {

debug(s"Rolling new log segment (log\_size = ${segment.size}/${config.segmentSize}}, " +

s"offset\_index\_size = ${segment.offsetIndex.entries}/${segment.offsetIndex.maxEntries}, " +

s"time\_index\_size = ${segment.timeIndex.entries}/${segment.timeIndex.maxEntries}, " +

s"inactive\_time\_ms = ${segment.timeWaitedForRoll(now, maxTimestampInMessages)}/${config.segmentMs - segment.rollJitterMs}).")

/\*

maxOffsetInMessages - Integer.MAX\_VALUE is a heuristic value for the first offset in the set of messages.

Since the offset in messages will not differ by more than Integer.MAX\_VALUE, this is guaranteed <= the real

first offset in the set. Determining the true first offset in the set requires decompression, which the follower

is trying to avoid during log append. Prior behavior assigned new baseOffset = logEndOffset from old segment.

This was problematic in the case that two consecutive messages differed in offset by

Integer.MAX\_VALUE.toLong + 2 or more. In this case, the prior behavior would roll a new log segment whose

base offset was too low to contain the next message. This edge case is possible when a replica is recovering a

highly compacted topic from scratch.

Note that this is only required for pre-V2 message formats because these do not store the first message offset

in the header.

\*/

appendInfo.firstOffset match {

// 3、新建 LogSegment。

case Some(firstOffset) => roll(Some(firstOffset))

case None \=\> roll(Some(maxOffsetInMessages - Integer.MAX\_VALUE))

}

} else {

// 4、如果不用新建就用现在的日志分段文件。

segment

}

}

该方法总共分为 4 个步骤来判断：

1.  取到当前活跃的 Segment，即最后一个 Segment。如果不需要新建 segment，就返回当前的 segment。
2.  是否需要新建一个 LogSegment 对象。具体是怎么判断的呢？后面进行剖析。
3.  如果满足条件就调用 roll() 方法新建 LogSegment 。
4.  如果不用新建就用现在的日志分段文件。

这 4 步骤中，最重要的是 「**第二步**」和 「**第四步**」，我们来挨个剖析下。

##   
**5.5.1 shouldRoll()**

def shouldRoll(rollParams: RollParams): Boolean = {

val reachedRollMs \= timeWaitedForRoll(rollParams.now, rollParams.maxTimestampInMessages) > rollParams.maxSegmentMs - rollJitterMs

// 1、如果加上现在消息的大小这个 segment 超过 1G，就需要新建一个 segment。

size > rollParams.maxSegmentBytes - rollParams.messagesSize ||

// 2、日志段有数据，并且距离上次创建日志段的时间达到了一个阈值（log.roll.hours默认7天）。

(size > 0 && reachedRollMs) ||

// 3、索引文件满了（默认10m）log.index.size.max.bytes。

offsetIndex.isFull ||

// 4、时间索引文件满了（默认10m）。

timeIndex.isFull ||

// 5、根据最大的 offset

!canConvertToRelativeOffset(rollParams.maxOffsetInMessages)

}

该方法主要是判断**是否需要新建 LogSegment**，需要满足以下五个条件之一才行：

1.  **首先根据大小判断**：如果加上现在消息的大小，这个 segment 超过 1G，就需要新建一个segment。
2.  **然后根据时间判断**：距离上次创建日志段的时间达到了一个阈值（[log.roll.hours](http://log.roll.hours/)默认7天），并且日志段有数据。
3.  **其次根据偏移量索引文件大小判断**：偏移量索引文件到一定大小的时候也需要做分段，默认10m。
4.  **再次根据实际索引文件大小判断**：时间索引文件到一定大小的时候也需要做分段，默认10m。
5.  **再最后根据最大的 offset 判断**：其相对偏移量超过了正整数的阈值，追加的消息的偏移量与当前日志段的偏移量之间的差值大于Integer.MAX\_VALUE。因为相对偏移量是 4 个字节，对应int类型也是 4 个字节，这样再大就超出了，就不能满足了。

## **5.5.2 roll()**

接下来我们再看看 roll() 方法是如何新建日志分段文件的。

/\*\*

\* Roll the log over to a new active segment starting with the current logEndOffset.

\* This will trim the index to the exact size of the number of entries it currently contains.

\* @return The newly rolled segment

\*/

def roll(expectedNextOffset: Option\[Long\] = None): LogSegment = {

maybeHandleIOException(s"Error while rolling log segment for $topicPartition in dir ${dir.getParent}") {

val start \= time.hiResClockMs()

lock synchronized {

checkIfMemoryMappedBufferClosed()

// 1、获取 LEO 值。

val newOffset \= math.max(expectedNextOffset.getOrElse(0L), logEndOffset)

// 2、基于 LEO 值生成日志分段文件。

val logFile \= Log.logFile(dir, newOffset)

// 3、判断日志段是否存在。

if (segments.containsKey(newOffset)) {

// segment with the same base offset already exists and loaded

// 4、判断是否有相同 baseffset 的 segment 已经加载进内存了。

if (activeSegment.baseOffset == newOffset && activeSegment.size == 0) {

// We have seen this happen (see KAFKA-6388) after shouldRoll() returns true for an

// active segment of size zero because of one of the indexes is "full" (due to \_maxEntries == 0).

warn(s"Trying to roll a new log segment with start offset $newOffset " +

s"=max(provided offset = $expectedNextOffset, LEO = $logEndOffset) while it already " +

s"exists and is active with size 0. Size of time index: ${activeSegment.timeIndex.entries}," +

s" size of offset index: ${activeSegment.offsetIndex.entries}.")

// 存在就删除

removeAndDeleteSegments(Seq(activeSegment), asyncDelete = true, LogRoll)

} else {

throw new KafkaException(s"Trying to roll a new log segment for topic partition $topicPartition with start offset $newOffset" +

s" =max(provided offset = $expectedNextOffset, LEO = $logEndOffset) while it already exists. Existing " +

s"segment is ${segments.get(newOffset)}.")

}

} else if (!segments.isEmpty && newOffset < activeSegment.baseOffset) {

throw new KafkaException(

s"Trying to roll a new log segment for topic partition $topicPartition with " +

s"start offset $newOffset =max(provided offset = $expectedNextOffset, LEO = $logEndOffset) lower than start offset of the active segment $activeSegment")

} else {

// 5、基于 LEO 值生成一个日志索引文件

val offsetIdxFile \= offsetIndexFile(dir, newOffset)

// 6、基于 LEO 值生成一个时间索引文件

val timeIdxFile \= timeIndexFile(dir, newOffset)

// 7、基于 LEO 值生成一个事务索引文件

val txnIdxFile \= transactionIndexFile(dir, newOffset)

for (file <- List(logFile, offsetIdxFile, timeIdxFile, txnIdxFile) if file.exists) {

warn(s"Newly rolled segment file ${file.getAbsolutePath} already exists; deleting it first")

// 删除文件

Files.delete(file.toPath)

}

Option(segments.lastEntry).foreach(\_.getValue.onBecomeInactiveSegment())

}

// take a snapshot of the producer state to facilitate recovery. It is useful to have the snapshot

// offset align with the new segment offset since this ensures we can recover the segment by beginning

// with the corresponding snapshot file and scanning the segment data. Because the segment base offset

// may actually be ahead of the current producer state end offset (which corresponds to the log end offset),

// we manually override the state offset here prior to taking the snapshot.

producerStateManager.updateMapEndOffset(newOffset)

producerStateManager.takeSnapshot()

// 7、新建一个 LogSegment 对象并加入集合中

val segment \= LogSegment.open(dir,

baseOffset = newOffset,

config,

time = time,

fileAlreadyExists = false,

initFileSize = initFileSize,

preallocate = config.preallocate)

// 8、加入 segment 集合中。

addSegment(segment)

// We need to update the segment base offset and append position data of the metadata when log rolls.

// The next offset should not change.

// 9、更新 LEO 值。

updateLogEndOffset(nextOffsetMetadata.messageOffset)

// schedule an asynchronous flush of the old segment

// 10、异步执行 flush 操作，从恢复点到最新的 offset 都需要 flush。

scheduler.schedule("flush-log", () => flush(newOffset), delay = 0L)

info(s"Rolled new log segment at offset $newOffset in ${time.hiResClockMs() - start} ms.")

// 11、返回 segment

segment

}

}

}

该方法的主要作用是**是滚动新建一个活跃的 LogSegment 文件**，文件以「**LogEndOffset**」值来命名。

1.  获取当前 LEO 值。
2.  基于 LEO 值生成日志分段文件，新的分段文件都是以 LEO 这个偏移量来命名的。
3.  判断日志段文件是否存在。
4.  判断是否有相同 baseoffset 的 segment 已经加载进内存了。如果内存存在就在 segments 集合和物理磁盘同时删除文件。
5.  基于 LEO 值生成一个日志索引文件。除了日志文件之外，还要新建索引文件。
6.  基于 LEO 值生成一个时间索引文件和事务索引文件。
7.  新建一个 LogSegment 对象。
8.  将 LogSegment 对象加入 segment 集合中。
9.  更新 LEO 值。
10.  异步执行 flush 操作，从恢复点到最新的offset都需要flush。
11.  返回 segment。

![](https://article-images.zsxq.com/Fo7I9t3X44sA0INhBd42wn6fV6L8)

## **06 日志读写操作**

最后，我们来看看日志的读写相关操作。

## **6.1 日志写操作**

在 Log 中，涉及写操作的方法有 3 个：appendAsLeader、appendAsFollower 和 append。它们的调用关系如下图所示：

  
![](https://article-images.zsxq.com/Fov0Xc9nrgC38ugZ1WVgMT9QCydv)

// 作为 Leader 分区向 Leader 分区追加日志，底层调用了 append 方法。

def appendAsLeader(records: MemoryRecords,

leaderEpoch: Int,

origin: AppendOrigin = AppendOrigin.Client,

interBrokerProtocolVersion: ApiVersion = ApiVersion.latestVersion): LogAppendInfo = {

append(records,

origin,

interBrokerProtocolVersion,

assignOffsets = true, // assignOffsets = true 表示是leader副本追加需要分配offset

leaderEpoch,

ignoreRecordSize = false)

}

// 作为 Follower 分区向 Follower分区追加日志，即副本数据同步的，底层调用了 append 方法。

def appendAsFollower(records: MemoryRecords): LogAppendInfo = {

append(records,

origin = AppendOrigin.Replication,

interBrokerProtocolVersion = ApiVersion.latestVersion,

assignOffsets = false, // 注意：assignOffsets是否需要分配位移，leader副本需要，fllower副本则不需要

leaderEpoch = -1,

// disable to check the validation of record size since the record is already accepted by leader.

ignoreRecordSize = true)

}

接下来，我们来重点剖析了 append() 方法。这个方法步骤非常多，我们一步步来拆解看看。

private def append(records: MemoryRecords,

origin: AppendOrigin,

interBrokerProtocolVersion: ApiVersion,

assignOffsets: Boolean,

leaderEpoch: Int,

ignoreRecordSize: Boolean): LogAppendInfo = {

maybeHandleIOException(s"Error while appending records to $topicPartition in dir ${dir.getParent}") {

// 1、分析和验证待写入消息集合，并返回校验结果，生成对应的 LogAppendInfo 对象

val appendInfo \= analyzeAndValidateRecords(records, origin, ignoreRecordSize)

// 2、如果没有有效消息，直接返回

if (appendInfo.shallowCount == 0)

return appendInfo

// 3、消息格式规整，即删除无效格式消息或无效字节

var validRecords \= trimInvalidBytes(records, appendInfo)

// they are valid, insert them in the log

lock synchronized {

// 确保 Log 对象未关闭

checkIfMemoryMappedBufferClosed()

// 判断是否需要分配 offsets，默认是需要的。

// 需要分配位移,leader副本需要分配位移，follwer副本不需要分配

if (assignOffsets) {

// 4、使用当前 LEO 值作为待写入消息集合中第一条消息的位移值，也是绝对位移值。

val offset \= new LongRef(nextOffsetMetadata.messageOffset)

appendInfo.firstOffset = Some(offset.value)

val now \= time.milliseconds

// 5、对 message 做进一步的验证：消息格式转换、调整 magic 的值、修改时间戳等操作，并为message 分配 offset。

val validateAndOffsetAssignResult \= try {

LogValidator.validateMessagesAndAssignOffsets(validRecords,

topicPartition,

offset,

time,

now,

appendInfo.sourceCodec,

appendInfo.targetCodec,

config.compact,

config.messageFormatVersion.recordVersion.value,

config.messageTimestampType,

config.messageTimestampDifferenceMaxMs,

leaderEpoch,

origin,

interBrokerProtocolVersion,

brokerTopicStats)

} catch {

case e: IOException =>

throw new KafkaException(s"Error validating messages while appending to log $name", e)

}

// 更新校验结果对象类 LogAppendInfo

validRecords = validateAndOffsetAssignResult.validatedRecords

// 获取最大的时间戳

appendInfo.maxTimestamp = validateAndOffsetAssignResult.maxTimestamp

// 最大时间戳的 offset

appendInfo.offsetOfMaxTimestamp = validateAndOffsetAssignResult.shallowOffsetOfMaxTimestamp

// 更新 lastoffset

appendInfo.lastOffset = offset.value - 1

appendInfo.recordConversionStats = validateAndOffsetAssignResult.recordConversionStats

// 在新版本的 kafka 中，每条 msg 都有一个对应的时间戳记录，producer 端可以设置这个字段message.timestamp.type 来选择 timestamp 类型。

if (config.messageTimestampType == TimestampType.LOG\_APPEND\_TIME)

// 设置日志追加的时间

appendInfo.logAppendTime = now

// 6、验证消息，确保消息大小不超限

if (!ignoreRecordSize && validateAndOffsetAssignResult.messageSizeMaybeChanged) {

for (batch <- validRecords.batches.asScala) {

if (batch.sizeInBytes > config.maxMessageSize) {

brokerTopicStats.topicStats(topicPartition.topic).bytesRejectedRate.mark(records.sizeInBytes)

brokerTopicStats.allTopicsStats.bytesRejectedRate.mark(records.sizeInBytes)

throw new RecordTooLargeException(s"Message batch size is ${batch.sizeInBytes} bytes in append to" +

s"partition $topicPartition which exceeds the maximum configured size of ${config.maxMessageSize}.")

}

}

}

} else {

// 直接使用给定的位移值，无需自己分配位移值

// 确保消息位移值的单调递增性，不是递增就抛出异常

if (!appendInfo.offsetsMonotonic)

throw new OffsetsOutOfOrderException(s"Out of order offsets found in append to $topicPartition: " + records.records.asScala.map(\_.offset))

if (appendInfo.firstOrLastOffsetOfFirstBatch < nextOffsetMetadata.messageOffset) {

val firstOffset \= appendInfo.firstOffset match {

case Some(offset) => offset

case None \=\> records.batches.asScala.head.baseOffset()

}

val firstOrLast \= if (appendInfo.firstOffset.isDefined) "First offset" else "Last offset of the first batch"

throw new UnexpectedAppendOffsetException(

s"Unexpected offset in append to $topicPartition. $firstOrLast " +

s"${appendInfo.firstOrLastOffsetOfFirstBatch} is less than the next offset ${nextOffsetMetadata.messageOffset}. " +

s"First 10 offsets in append: ${records.records.asScala.take(10).map(\_.offset)}, last offset in" +

s" append: ${appendInfo.lastOffset}. Log start offset = $logStartOffset",

firstOffset, appendInfo.lastOffset)

}

}

// 7、更新 LeaderEpoch 缓存

validRecords.batches.forEach { batch =>

if (batch.magic >= RecordBatch.MAGIC\_VALUE\_V2) {

maybeAssignEpochStartOffset(batch.partitionLeaderEpoch, batch.baseOffset)

} else {

leaderEpochCache.filter(\_.nonEmpty).foreach { cache =>

warn(s"Clearing leader epoch cache after unexpected append with message format v${batch.magic}")

cache.clearAndFlush()

}

}

}

// 8、确保消息大小不超限

if (validRecords.sizeInBytes > config.segmentSize) {

throw new RecordBatchTooLargeException(s"Message batch size is ${validRecords.sizeInBytes} bytes in append " +

s"to partition $topicPartition, which exceeds the maximum configured segment size of ${config.segmentSize}.")

}

// 9、如果当前日志段剩余容量可能无法容纳新消息集合，执行日志切分，创建一个新的日志段来保存待写入的所有消息。如果当前日志段剩余容量能够容纳新消息集合，就直接返回当前的segment

val segment \= maybeRoll(validRecords.sizeInBytes, appendInfo)

val logOffsetMetadata \= LogOffsetMetadata(

messageOffset = appendInfo.firstOrLastOffsetOfFirstBatch,

segmentBaseOffset = segment.baseOffset,

relativePositionInSegment = segment.size)

// 10、验证事务状态

val (updatedProducers, completedTxns, maybeDuplicate) = analyzeAndValidateProducerState(

logOffsetMetadata, validRecords, origin)

maybeDuplicate.foreach { duplicate =>

appendInfo.firstOffset = Some(duplicate.firstOffset)

appendInfo.lastOffset = duplicate.lastOffset

appendInfo.logAppendTime = duplicate.timestamp

appendInfo.logStartOffset = logStartOffset

return appendInfo

}

// 11、执行真正的消息写入操作，主要调用日志段对象的append方法实现

segment.append(largestOffset = appendInfo.lastOffset,

largestTimestamp = appendInfo.maxTimestamp,

shallowOffsetOfMaxTimestamp = appendInfo.offsetOfMaxTimestamp,

records = validRecords)

// 12、更新 LEO 对象，其中 LEO 值是消息集合中最后一条消息位移值+1，LEO 值永远指向下一条不存在的消息

updateLogEndOffset(appendInfo.lastOffset + 1)

// 13、更新事务状态

for (producerAppendInfo <- updatedProducers.values) {

producerStateManager.update(producerAppendInfo)

}

// update the transaction index with the true last stable offset. The last offset visible

// to consumers using READ\_COMMITTED will be limited by this value and the high watermark.

for (completedTxn <- completedTxns) {

val lastStableOffset \= producerStateManager.lastStableOffset(completedTxn)

segment.updateTxnIndex(completedTxn, lastStableOffset)

producerStateManager.completeTxn(completedTxn)

}

// always update the last producer id map offset so that the snapshot reflects the current offset

// even if there isn't any idempotent data being written

producerStateManager.updateMapEndOffset(appendInfo.lastOffset + 1)

// update the first unstable offset (which is used to compute LSO)

maybeIncrementFirstUnstableOffset()

trace(s"Appended message set with last offset: ${appendInfo.lastOffset}, " +

s"first offset: ${appendInfo.firstOffset}, " +

s"next offset: ${nextOffsetMetadata.messageOffset}, " +

s"and messages: $validRecords")

// 14、是否需要手动落盘。一般情况下我们不需要设置Broker端参数log.flush.interval.messages

// 落盘操作交由操作系统来完成。但某些情况下，可以设置该参数来确保高可靠性

if (unflushedMessages >= config.flushInterval)

flush()

// 15、返回写入结果

appendInfo

}

}

}

该方法比较复杂，下面通过一张图来梳理下流程：

![](https://article-images.zsxq.com/FhqwqU5bayEdX_xvQDbJn7gmVCWv)

在上面整个流程中，重要步骤有 「**第一步**」、「**第九步**」、「**第十一步**」、「**第十二步**」、「**第十四步**」，除了「**第一步**」、「**第十四步**」，其他步骤都在前面章节已经剖析过，自己自行学习，如果有找不到的可留言给我。

这里我们继续来看下「**第一步**」是如何校验日志消息的呢？我们需要重点看下**针对不同消息格式版本**，**Kafka 是如何做校验的**。

再看消息校验前，我们先来看看 [LogAppendInfo](http://logappendinfo%20/) 类，里面几乎保存了待写入消息集合的所有信息。

case class LogAppendInfo(var firstOffset: Option\[Long\], // 消息集合第一条消息的位移值

var lastOffset: Long, // 消息集合最后一条消息的位移值

var maxTimestamp: Long, // 消息集合最大消息时间戳

var offsetOfMaxTimestamp: Long, // 消息集合最大消息时间戳所属消息的位移值

var logAppendTime: Long, // 写入消息时间戳

var logStartOffset: Long, // 消息集合首条消息的位移值

var recordConversionStats: RecordConversionStats, // 消息转换统计类，里面记录了执行了格式转换的消息数等数据

sourceCodec: CompressionCodec, // 消息集合中消息使用的压缩器（Compressor）类型，比如是Snappy还是LZ4

targetCodec: CompressionCodec, // 写入消息时需要使用的压缩器类型

shallowCount: Int,// 消息批次数，每个消息批次下可能包含多条消息

validBytes: Int,// 写入消息总字节数

offsetsMonotonic: Boolean,// 消息位移值是否是顺序增加的

lastOffsetOfFirstBatch: Long,// 首个消息批次中最后一条消息的位移

recordErrors: Seq\[RecordError\] = List(),// 写入消息时出现的异常列表

errorMessage: String = null,// 错误码

leaderHwChange: LeaderHwChange = LeaderHwChange.None) {

/\*\*

\* Get the first offset if it exists, else get the last offset of the first batch

\* For magic versions 2 and newer, this method will return first offset. For magic versions

\* older than 2, we use the last offset of the first batch as an approximation of the first

\* offset to avoid decompressing the data.

\*/

def firstOrLastOffsetOfFirstBatch: Long = firstOffset.getOrElse(lastOffsetOfFirstBatch)

/\*\*

\* Get the (maximum) number of messages described by LogAppendInfo

\* @return Maximum possible number of messages described by LogAppendInfo

\*/

def numMessages: Long = {

firstOffset match {

case Some(firstOffsetVal) if (firstOffsetVal >= 0 && lastOffset >= 0) => (lastOffset - firstOffsetVal + 1)

case \_ \=\> 0

}

}

}

大部分字段的含义很明确，这里我稍微提一下 「**lastOffset**」、「**lastOffsetOfFirstBatch**」。

我们都知道 Kafka 消息格式经历了两次大的变迁，目前是「**0.11.0.0**」版本引入的「**Version 2**」 消息格式。我们没有必要详细了解这些格式的变迁，你只需要知道，在「**0.11.0.0**」版本之后，「**lastOffset**」、「**lastOffsetOfFirstBatch**」 都是**指向消息集合的最后一条消息即可**。它们之间的区别主要体现在「**0.11.0.0**」版本之前，后面我们通过一篇来讲解 Kafka 消息格式变迁史。

在 append 方法「**第一步**」中调用 [analyzeAndValidateRecords](http://analyzeandvalidaterecords/) 方法对消息集合进行校验，并生成对应的 [LogAppendInfo](http://logappendinfo/) 对象，其源码如下：

private def analyzeAndValidateRecords(records: MemoryRecords,

origin: AppendOrigin,ignoreRecordSize: Boolean): LogAppendInfo = {

var shallowMessageCount \= 0

var validBytesCount \= 0

var firstOffset: Option\[Long\] = None

var lastOffset \= -1L

var sourceCodec: CompressionCodec = NoCompressionCodec

var monotonic \= true

var maxTimestamp \= RecordBatch.NO\_TIMESTAMP

var offsetOfMaxTimestamp \= -1L

var readFirstMessage \= false

var lastOffsetOfFirstBatch \= -1L

// 1、遍历 MemoryRecords 内的所有的batch。浅层遍历

for (batch <- records.batches.asScala) {

// 消息格式Version 2 的消息批次，起始位移值必须从 0 开始

if (batch.magic >= RecordBatch.MAGIC\_VALUE\_V2 && origin == AppendOrigin.Client && batch.baseOffset != 0)

throw new InvalidRecordException(s"The baseOffset of the record batch in the append to $topicPartition should " +

s"be 0, but it is ${batch.baseOffset}")

if (!readFirstMessage) {

if (batch.magic >= RecordBatch.MAGIC\_VALUE\_V2)

firstOffset = Some(batch.baseOffset)

lastOffsetOfFirstBatch = batch.lastOffset

readFirstMessage \= true

}

// 2、一旦出现当前 lastOffset 不小于下一个 batch 的 lastOffset，说明上一个 batch 中有消息的位移值大于后面 batch 的消息，这违反了位移值单调递增性

if (lastOffset >= batch.lastOffset)

monotonic = false

// 使用当前 batch 最后一条消息的位移值去更新 lastOffset

lastOffset = batch.lastOffset

// 3、检查消息批次总字节数大小是否超限，即是否大于 Broker 端参数 max.message.bytes 值

val batchSize \= batch.sizeInBytes

if (!ignoreRecordSize && batchSize > config.maxMessageSize) {

brokerTopicStats.topicStats(topicPartition.topic).bytesRejectedRate.mark(records.sizeInBytes)

brokerTopicStats.allTopicsStats.bytesRejectedRate.mark(records.sizeInBytes)

throw new RecordTooLargeException(s"The record batch size in the append to $topicPartition is $batchSize bytes " +

s"which exceeds the maximum configured value of ${config.maxMessageSize}.")

}

// 4、执行消息批次校验，包括格式是否正确以及CRC校验

if (!batch.isValid) {

brokerTopicStats.allTopicsStats.invalidMessageCrcRecordsPerSec.mark()

throw new CorruptRecordException(s"Record is corrupt (stored crc = ${batch.checksum()}) in topic partition $topicPartition.")

}

// 更新maxTimestamp字段和offsetOfMaxTimestamp

if (batch.maxTimestamp > maxTimestamp) {

maxTimestamp = batch.maxTimestamp

offsetOfMaxTimestamp \= lastOffset

}

// 累加消息批次计数器以及有效字节数，更新shallowMessageCount字段

shallowMessageCount += 1

validBytesCount += batchSize

// 从消息批次中获取压缩器类型

val messageCodec \= CompressionCodec.getCompressionCodec(batch.compressionType.id)

if (messageCodec != NoCompressionCodec)

sourceCodec = messageCodec

}

// 获取 Broker 端设置的压缩器类型，即 Broker 端参数 compression.type 值。

// 该参数默认值是 producer，表示 sourceCodec 用的什么压缩器，targetCodec 就用什么

val targetCodec \= BrokerCompressionCodec.getTargetCompressionCodec(config.compressionType, sourceCodec)

// 5、最后生成LogAppendInfo对象并返回

LogAppendInfo(firstOffset, lastOffset, maxTimestamp, offsetOfMaxTimestamp, RecordBatch.NO\_TIMESTAMP, logStartOffset,

RecordConversionStats.EMPTY, sourceCodec, targetCodec, shallowMessageCount, validBytesCount, monotonic, lastOffsetOfFirstBatch)

}

这里我们来描述一下**验证消息的过程**:

1.  首先遍历 MemoryRecords 内所有的 batch，浅层遍历。因为 MemoryRecords 里面可能会有多个 batch，batch 里面才是具体的 record，所以 batch 这层是第一层遍历。
2.  下一步，一旦出现当前 lastOffset 不小于下一个batch的 lastOffset，说明上一个 batch 中有消息的位移值大于后面 batch 的消息，这违反了位移值单调递增性。
3.  再下一步，检查消息批次总字节数大小是否超限，即是否大于Broker端参数 [max.message.bytes](http://max.message.bytes/) 值。也就是说这里的消息大小超限是针对消息日志 batch 大小的。
4.  然后执行消息批次校验，包括格式是否正确以及 CRC 校验，更新 maxTimestamp 字段和 offsetOfMaxTimestamp，更新shallowMessageCount 字段，并更新有效的字节数。
5.  最后，生成 LogAppendInfo 类对象 appendInfo 并返回，对象内包含第一个消息的 offset、最后一个消息的offset。

在 append 方法「**第十一步**」中调用 [LogSegment#append()](http://logsegment/#append\(\)) 方法进行顺序追加写。

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

def append(largestOffset: Long, //最大位移

largestTimestamp: Long,//最大时间戳

shallowOffsetOfMaxTimestamp: Long,//最大时间戳对应消息的位移

records: MemoryRecords): Unit = {//真正要写入的消息集合

if (records.sizeInBytes > 0) {

trace(s"Inserting ${records.sizeInBytes} bytes at end offset $largestOffset at position ${log.sizeInBytes} " +

s"with largest timestamp $largestTimestamp at shallow offset $shallowOffsetOfMaxTimestamp")

// 第一步：判断该日志段是否为空

val physicalPosition \= log.sizeInBytes()

if (physicalPosition == 0)

// 记录要写入消息集合的最大时间戳，并将其作为后面新增日志段倒计时的依据

rollingBasedTimestamp = Some(largestTimestamp)

// 第二步：确保输入参数最大位移值是合法的

ensureOffsetInRange(largestOffset)

// 第三步：将内存中的消息对象写入到操作系统的页缓存

val appendedBytes \= log.append(records)

trace(s"Appended $appendedBytes to ${log.file} at end offset $largestOffset")

// Update the in memory max timestamp and corresponding offset.

// 第四步：更新日志段的最大时间戳以及最大时间戳所属消息的位移值属性

if (largestTimestamp > maxTimestampSoFar) {

maxTimestampAndOffsetSoFar = TimestampOffset(largestTimestamp, shallowOffsetOfMaxTimestamp)

}

// append an entry to the index (if needed)

// 第五步：更新索引项和写入的字节数，kafka 保证时间戳索引项保存时间戳与消息位移的对应关系

// 索引是稀疏索引，不是每条消息都对应一条索引。写4K即4096字节，会更新一次索引

if (bytesSinceLastIndexEntry > indexIntervalBytes) {

// indexIntervalBytes:默认4KB，即4KB的消息建一条索引

offsetIndex.append(largestOffset, physicalPosition)

// 更新timeIndex

timeIndex.maybeAppend(maxTimestampSoFar, offsetOfMaxTimestampSoFar)

// 索引写完后，重置，继续4K字节写索引

bytesSinceLastIndexEntry = 0

}

// 写入.log文件的消息大小累加

bytesSinceLastIndexEntry += records.sizeInBytes

}

}

这里的 [log.append()](http://%20log.append\(\)/) 其实是 [FileRecords#append()](http://filerecords/#append\(\)) ，将内存中的消息对象写入到操作系统的页缓存。[MemoryRecords](http://memoryrecords/) 是内存的消息，[channel](http://channel%20/) 对应的是 [FileChannel](http://filechannel/) 属于操作系统的 [PageCache](http://pagecache/)。[FileChannel](http://filechannel/) 可以认为是操作系统中的内核缓冲区。

public int append(MemoryRecords records) throws IOException {

if (records.sizeInBytes() > Integer.MAX\_VALUE - size.get())

throw new IllegalArgumentException("Append of size " + records.sizeInBytes() +

" bytes is too large for segment with current file position at " + size.get());

int written \= records.writeFullyTo(channel);

size.getAndAdd(written);

return written;

}

[MemoryRecords](http://memoryrecords%20/) 类 [writeFullyTo](http://writefullyto/) 方法作用：Write all records to the given channel (including partial records)。

public int writeFullyTo(GatheringByteChannel channel) throws IOException {

// 标记一下position的位置

buffer.mark();

int written \= 0;

// 数据必须写完，才能出while循环

while (written < sizeInBytes())

// 调用FileChannel写数据

written += channel.write(buffer);

// 恢复标记的位置

buffer.reset();

return written;

}

Kafka 底层采用的是 [FileChannel.wrtieFullyTo](http://filechannel.wrtiefullyto/) 进行数据的写入，写的时候并不是直接写入文件，而是写入[ByteBuffer](http://bytebuffer/)，然后当缓冲区满了，再将数据顺序写入文件，无需定位文件中的某一个位置进行写入，那么就减少了磁盘查询，数据定位的过程。所以性能要比随机写入，效率高得多。

官网有数据表明，同样的磁盘，顺序写能到 600 M/s，而随机写只有100 K/s。这与磁盘的机械结构有关，顺序写之所以快，是因为其省去了大量磁头寻址的时间。

![](https://article-images.zsxq.com/Fuq5BW_C9knvKqLWR2bsV4MvLmd9)

在 append 方法「**第十四步**」中调用 [flush](http://flush/) 方法对消息集合进行刷盘操作，其源码如下：

/\*\*

\* Flush log segments for all offsets up to offset-1

\*

\* @param offset The offset to flush up to (non-inclusive); the new recovery point

\*/

def flush(offset: Long): Unit = {

maybeHandleIOException(s"Error while flushing log for $topicPartition in dir ${dir.getParent} with offset $offset") {

if (offset <= this.recoveryPoint)

return

debug(s"Flushing log up to offset $offset, last flushed: $lastFlushTime, current time: ${time.milliseconds()}, " +

s"unflushed: $unflushedMessages")

//1、遍历所有 logSegment 对象，在（recoveryPoint，offset）区间内的 offset 都要刷盘。

for (segment <- logSegments(this.recoveryPoint, offset))

segment.flush() // 调用了 LogSegment 的 flush 方法。

// 2、修改 recoveryPoint 和 lastFlushedTime 的值

lock synchronized {

checkIfMemoryMappedBufferClosed()

if (offset > this.recoveryPoint) {

this.recoveryPoint = offset // 更新 recoveryPoint.

lastFlushedTime.set(time.milliseconds) // 修改 lastFlushedTime.

}

}

}

}

该方法的主要作用是**刷新指定偏移量内所有相关的内存数据到磁盘 LogSegment 文件里**。一般不会主动刷新磁盘，因为这样就会造成操作系统通过页缓存优化磁盘 IO 的优化失效。只有在新建 **LogSegment** 文件的时候才会主动调用 flush() 来刷新，一般高频次的读写不会调用 flush()，主要有以下 2 步：

1.  遍历所有 logSegment 对象，在（recoveryPoint，offset）区间内的 offset 都要刷盘。
2.  加锁修改 recoveryPoint 和 lastFlushedTime 的值。

## **6.2 日志读操作**

上面剖析完写操作 append 方法，下面我们聊聊 read 方法。

read 方法的流程相对要简单一些，来看下源码：

def read(startOffset: Long,

maxLength: Int,

isolation: FetchIsolation,

minOneMessage: Boolean): FetchDataInfo = {

maybeHandleIOException(s"Exception while reading from $topicPartition in dir ${dir.getParent}") {

trace(s"Reading maximum $maxLength bytes at offset $startOffset from log with " +

s"total length $size bytes")

val includeAbortedTxns \= isolation == FetchTxnCommitted

// 1、读取消息时没有使用 Monitor 锁同步机制，因此这里取巧了，用本地变量的方式把 LEO 对象保存起来，避免争用（race condition）,不会造成因为没加锁造成的多线程数据安全问题。

val endOffsetMetadata \= nextOffsetMetadata

// 取出LEO的值

val endOffset \= endOffsetMetadata.messageOffset

// 2、找到 startOffset 值所在的日志段对象。注意要使用 floorEntry 方法，表示要找 baseOffset 小于 startOffset 里最大的 baseOffset 的 segment。

var segmentEntry \= segments.floorEntry(startOffset)

// 3、判断消息越界，满足以下条件之一将被视为消息越界，即你要读取的消息不在该Log对象中：

// 1、要读取的消息位移超过了 LEO 值

// 2、没找到对应的日志段对象

// 3、要读取的消息在Log Start Offset之下，同样是对外不可见的消息

if (startOffset > endOffset || segmentEntry == null || startOffset < logStartOffset)

throw new OffsetOutOfRangeException(s"Received request for offset $startOffset for partition $topicPartition, " +

s"but we only have log segments in the range $logStartOffset to $endOffset.")

// 4、查看一下读取隔离级别设置,不同类型的读请求对应的 maxOffsetMetadata 是不一样的。

// 普通消费者能够看到\[Log Start Offset, LEO)之间的消息

// Follower 副本消费者能够看到 \[Log Start Offset，High WaterMark\] 之间的消息

// 事务型消费者只能看到\[Log Start Offset, Log Stable Offset\]之间的消息。Log Stable Offset(LSO)是比LEO值小的位移值，为Kafka事务使用

val maxOffsetMetadata \= isolation match {

case FetchLogEnd \=\> endOffsetMetadata

case FetchHighWatermark \=\> fetchHighWatermarkMetadata

case FetchTxnCommitted \=\> fetchLastStableOffsetMetadata

}

// 如果要读取的起始位置超过了能读取的最大位置，返回空的消息集合，因为没法读取任何消息

if (startOffset == maxOffsetMetadata.messageOffset) {

return emptyFetchDataInfo(maxOffsetMetadata, includeAbortedTxns)

} else if (startOffset > maxOffsetMetadata.messageOffset) {

val startOffsetMetadata \= convertToOffsetMetadataOrThrow(startOffset)

return emptyFetchDataInfo(startOffsetMetadata, includeAbortedTxns)

}

// 5、开始遍历日志段对象，直到读出东西来或者读到日志末尾

while (segmentEntry != null) {

val segment \= segmentEntry.getValue

val maxPosition \= {

if (maxOffsetMetadata.segmentBaseOffset == segment.baseOffset) {

maxOffsetMetadata.relativePositionInSegment

} else {

segment.size

}

}

// 6、调用日志段对象的 read 方法执行真正的读取消息操作

val fetchInfo \= segment.read(startOffset, maxLength, maxPosition, minOneMessage)

if (fetchInfo == null) {

// 如果没有返回任何消息，去下一个日志段对象试试

segmentEntry = segments.higherEntry(segmentEntry.getKey)

} else { // 否则返回

return if (includeAbortedTxns)

addAbortedTransactions(startOffset, segmentEntry, fetchInfo)

else

// 7、返回数据

fetchInfo

}

}

// 已经读到日志末尾还是没有数据返回，只能返回空消息集合

FetchDataInfo(nextOffsetMetadata, MemoryRecords.EMPTY)

}

}

它接收 4 个参数，含义如下：

1.  **startOffset**：从 Log 对象的哪个位移值开始读消息。
2.  **maxLength**：最多能读取多少字节。
3.  **isolation**：设置读取隔离级别，主要控制能够读取的最大位移值，多用于 Kafka 事务。
4.  **minOneMessage**：是否允许至少读一条消息。设想如果消息很大，超过了 maxLength，正常情况下 read 方法永远不会返回任何消息。但如果设置了该参数为 true，read 方法就保证至少能够返回一条消息。

步骤如下：

1.  把 LEO 赋值给本地变量。读取消息时没有使用 Monitor 锁同步机制，因此这里取巧了，用本地变量的方式把LEO 对象保存起来，避免争用（race condition），不会造成因为没加锁而引起的多线程数据安全问题。
2.  找到 startOffset 值所在的日志段对象。注意要使用 floorEntry 方法，表示要找 baseOffset 小于 startOffset里最大的 baseOffset 的 segment。
3.  然后判断是否是越界消息。满足下面三点中的一个条件就认为是越界了：
4.  要读取的消息位移超过了LEO值；
5.  没找到对应的日志段对象；
6.  要读取的消息在Log Start Offset之前，同样是对外不可见的消息。
7.  查看一下读取隔离级别设置，不同类型的读请求对应的 maxOffsetMetadata 也是不一样的。
8.  普通消费者能够看到 \[Log Start Offset, LEO) 之间的消息。
9.  Follower 副本消费者能够看到 \[Log Start Offset，High WaterMark\] 之间的消息。
10.  事务型消费者只能看到 \[Log Start Offset, Log Stable Offset\] 之间的消息。Log Stable Offset（LSO）是比 LEO 值小的位移值，为 Kafka 事务使用。
11.  开始遍历日志段对象，直到读出对应的偏移量的消息来或者读到日志末尾。具体是调用日志段对象的 read 方法执行真正的读取消息操作。
12.  调用日志段对象的 read 方法执行真正的读取消息操作。
13.  返回读到的消息。

## **08 总结**

这里，我们一起来总结一下这篇文章的重点。

1、对「**Log**」类对象常见操作进行梳理。

2、带你深度剖析了「**高水位管理操作**」，Log 对象定义了高水位对象以及管理它的各种操作包括更新和读取。

3、接着带你深度剖析了「**关键位移值管理操作**」，主要涉及对 「**LogStartOffset**」和 「**LogEndOffset**」的管理。这两个位移值是 Log 对象非常关键的字段。比如，副本管理、状态机管理等高阶功能都要依赖于它们。

4、再接着带你深度剖析了「**日志段管理操作**」，作为日志段的容器，Log 对象保存了很多日志段对象。你需要重点掌握这些日志段对象被组织在一起的方式以及 Kafka Log 对象是如何对它们进行管理的。

4、最后剖析了「**日志读写操作**」，是实现 Kafka 消息引擎基本功能的基石。

下篇我们来深度剖析「**稀疏索引架构设计**」，大家期待，我们下期见。