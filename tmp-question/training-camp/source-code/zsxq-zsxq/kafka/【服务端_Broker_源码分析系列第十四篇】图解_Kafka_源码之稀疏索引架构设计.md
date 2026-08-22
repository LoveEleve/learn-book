大家好，我是 **华仔**, 又跟大家见面了。

上两篇中，主要带大家深度剖析了 「**Kafka 服务端源码 Log 日志架构设计和日志操作梳理**」，通过「**场景驱动方式**」，现在消息被封装成批次请求已经从「**生产者**」发送到「**Broker**」，且被「**网络层**」所接收到并准备进行消息数据存储，从今天开始，我们来深度剖析 Kafka 日志系统的底层实现，这是日志系列第七篇，我们继续来深度聊聊「**Kafka 服务端源码之稀疏索引架构设计**」，看看 Kafka 服务端是如何对日志进行构建索引和高效查询的。

![](https://article-images.zsxq.com/FisVS_v5TS4OxwkBmkijc0rH_7gM)

## **01 总体概述**

在上上篇中，我们 [【服务端 Broker 源码分析系列第十二篇】图解 Kafka 源码之日志 Log 架构设计](https://articles.zsxq.com/id_ph1gx4aq04sq.html) 「**Log**」初始化时引出了服务端各种日志索引文件之 「**xxxIndex**」。

在之前原理篇中，我们 [【原理分析系列第六篇】图解 Kafka 的存储架构](https://articles.zsxq.com/id_d912swekk0we.html) 通过分析得知 Kafka 底层是基于「**稀疏索引**」的方法来存储和高效查询的。

  
![](https://article-images.zsxq.com/FjoICmaX479SzgHwU6okodtGIZZ3)

在 Kafka 中，索引类型有三大类：「**位移索引**」、「**时间索引**」、「**已终止事务索引**」。相比于最后一类索引，前两类索引的出镜率更高一些。

如上图 Kafka 的数据存储路径下，你肯定看到过很多 「**.index**」和「**.timeindex**」 后缀的文件。不知你是否有过这样的疑问：**这些文件都是用来做什么的呢？它们究竟是如何定义和生成的呢？如何向索引中写入索引项呢？如何高效查询索引呢？各自又有什么特点和区别呢？**

今天我们就带着这些问题，来看看 Kafka 索引部分的源码，相信你学完本篇后，这些问题一定会迎刃而解的。

好了，下面开始剖析其源码实现，让你对整个日志管理有个总体的认知，深度剖析下其内部是如何进行日志管理的，整个流程是怎么样的？那么带着这些问题进入今天的正题。

整个日志管理的相关组件的调用关系图如下：

![](https://article-images.zsxq.com/FgVmpr3JOm-_1PVfCHJC94zu4_tC)

## **02 索引重要性**

索引对于我们来说并不陌生，每一本书籍的目录就是索引在现实生活中的应用。通过寥寥几页纸就得以让我等快速查找需要的内容。

冗余了几页纸，缩短了查阅的时间。**空间和时间上的互换**，包含着宇宙的哲学。工程领域上数据库的索引更是不可或缺，没有索引很难想象如此庞大的数据该如何检索。

明确了索引的重要性，接下来看看索引在 Kafka 里是如何实现的。

## **03 日志索引介绍**

为了提高查询消息的效率，每个日志段文件都对应一个索引文件，这个索引文件并没有为每条消息都建立索引项，而是使用「**稀疏索引**」方式为日志文件中的**部分消息建立了索引**，如下图所示：

  
![](https://article-images.zsxq.com/Foe82Ih_LMCzW73fuG_BZfgeykaX)

  
从图中可以得出，并不是每个 offset 都建立了索引，而是隔着一些消息才会给某条消息的偏移量做索引项。索引项分为两个部分。一个部分是「**offset 偏移量**」，另一个部分是「**offset 对应的物理位置**」。

Kafka 内部使用「**稀疏索引**」的方式来构建消息的索引，它不保证每个消息在索引文件中都有对应的索项，这算是「**磁盘空间**」、「**内存空间**」、「**查询时间**」等多方面的性能折中。

所谓「**稀疏索引**」，其实就是应用了耳熟能详的「**二分查找算法**」来快速定位索引项。而不断减小索引文件大小的目的是**将索引文件映射到内存中**，这样做的好处是**提升追加和查询索引的效率**。

虽然是「**二分查找算法**」，但是 Kafka 社区针对 Kafka 自身的特点对其进行了改良，接下来就让我们来揭开它的神秘面纱吧。

## **04 索引在 Kafka 中的源码实践**

在 Kafka 源码中，跟索引相关的源码文件有 5 个，它们都位于 core 包的 /src/main/scala/kafka/log 路径下。如下图，我们一一来看下：

  
![](https://article-images.zsxq.com/FqrSkZ_PnaulIxnX_UUEcpgId1ya)

1.  [AbstractIndex.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/AbstractIndex.scala)：它定义了**索引文件的内存映射最顶层**的抽象类，该类封装了**所有索引类型文件的公共操作方法**。
2.  [LazyIndex.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/LazyIndex.scala)：它定义了 AbstractIndex 上的一个包装类，实现**索引项延迟加载功能**。该类主要是为了提高性能而设计的。
3.  [OffsetIndex.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/OffsetIndex.scala)：其定义了**位移索引**，内部保存了索引项< 位移值，文件磁盘物理位置 > 键值对关系。
4.  [TimeIndex.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/TimeIndex.scala)：其定义了**时间戳索引**，内部保存了索引项< 时间戳，对应位移值 > 键值对关系。
5.  [TranscationIndex.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/log/TranscationIndex.scala)：其定义了**事务索引**，为已中止事务（Aborted Transcation）保存重要的元数据信息。**只有启用 Kafka 事务后**，**这个索引才有可能会出现**。

其关系图如下：

![](https://article-images.zsxq.com/FjLlEDm3Fk459KtYWNKYo1qypP39)

从图中可以看出，OffsetIndex、TimeIndex 和 TransactionIndex 都**继承**了 AbstractIndex 类，而上层的 LazyIndex 仅仅是包装了一个 AbstractIndex 的实现类，用来**索引项延迟加载功能**。就像我之前说的，LazyIndex 的作用仅仅是为了提升性能而已。

所以，我们先来了解抽象类 AbstractIndex 的源码。

## **05 AbstractIndex 源码分析**

先来看下其定义：

abstract class AbstractIndex(@volatile private var \_file: File, // 索引对应的文件

val baseOffset: Long, // 索引文件的起始位移值

val maxIndexSize: Int = -1, // 索引文件的最大长度，对应 segment.index.bytes 默认 10MB

val writable: Boolean) extends Closeable { // 索引文件打开方式，false 表示只读打开

import AbstractIndex.\_

@volatile

private var \_length: Long = \_

protected def entrySize: Int

protected def \_warmEntries: Int = 8192 / entrySize

protected val lock \= new ReentrantLock

....

}

AbstractIndex 抽象类定义了 4 个属性字段，Kafka 所有类型的索引对象都定义了这 4 个属性。

1.  **file 索引文件**：每个索引对象在磁盘上都对应了一个索引文件。字段是 var 型，说明它是可以被修改的，Kafka 允许迁移底层的日志路径，所以索引文件自然是可以更换的。
2.  **baseOffset 起始位移值**：索引对象对应日志段对象的起始位移值。举个例子，如果你查看 Kafka 日志路径的话，日志文件是 [00000000000000000123.log](http://00000000000000000123.log/)。这里的 **123** 就是这组文件的起始位移值，也就是 baseOffset 值。
3.  **maxIndexSize 索引文件最大字节数**：它控制索引文件的最大长度，该参数的默认值是 Broker 端参数 [segment.index.bytes](http://segment.index.bytes/) 的值，即 10MB。这就是在默认情况下，所有 Kafka 索引文件大小都是 10MB 的原因。
4.  **writable 索引文件打开方式**：True 表示以**读写**方式打开，False 表示以**只读**方式打开。

AbstractIndex 是抽象的索引对象类。它是**承载索引项的容器**，而每个继承它的子类负责定义具体的索引项结构。

索引项：

1.  OffsetIndex 的索引项是 < 位移值，文件磁盘物理位置 > 键值对，
2.  TimeIndex 的索引项是 < 时间戳，位移值 > 键值对。

基于这样的设计理念，AbstractIndex 类中定义了一个抽象方法 entrySize 来表示每个索引项的大小，每个索引项的大小是固定的，如下所示：

protected def entrySize: Int

位移索引和时间索引子类实现该方法时需要给定自己索引项的大小，如下所示：

// offsetIndex 是 8 个字节

override def entrySize \= 8

// timeIndex 是 12 个字节

override def entrySize \= 12

看到这里，你可能会有疑问，**为什么会是 8 和 12 呢**，**而不是 12 和 16 呢？**

在 **OffsetIndex** 中，每个索引项存储了「**位移值**」和「**对应的磁盘物理位置**」，因此 4 + 4 = 8，但是不对啊，磁盘物理位置是整型没问题，但是 **AbstractIndex** 的定义 **baseOffset** 来看，位移值是长整型（Long），不应该占用 8 个字节才对么？

在保存索引项时，Kafka 做了一些优化。**每个 OffsetIndex 对象在创建时**，**都已经保存了对应日志段对象的起始位移**，因此 OffsetIndex 索引项没必要保存完整的 8 字节位移值，它**每存储的位移值实际上是相对位移值**，即**真实位移值 - baseOffset 的值**。

相对位移用整型存储够么？够，因为Broker 端日志段文件大小的参数 [log.segment.bytes](http://log.segment.bytes/) 是整型，说明 Kafka 中每个日志段文件的大小不会超过 2^32，即 4GB，这也说明了**同一个日志段文件上的位移值减去 baseOffset 的差值一定在整数范围内**，因此只需要 4 个字节保存就行了。**这样的设计可以让 OffsetIndex 每个索引项都可以节省 4 个字节**，所以使用**相对位移值可以有效节省磁盘空间**。

所以在 OffsetIndex 中，位移值用 4 个字节来表示，物理磁盘位置也用 4 个字节来表示，总共是 8 个字节。同理，TimeIndex 中的时间戳类型是长整型，占用 8 个字节，位移占用 4 个字节，总共需要 12 个字节。

## **5.1 为什么要这么麻烦，还要存储差值？**

1.  为了节省磁盘空间，假设一个索引项节省了 4 字节，一个索引文件保存了 1000 个索引项，使用相对位移值就能节省大约 4MB 的空间，如果保存很多的索引项的话，想想那些日消息处理数万亿的公司，节约空间还是非常可观的。
2.  系统内存资源是非常宝贵的，索引项越短，内存中能存储的索引项就越多，索引项多了直接命中的概率就高了。这其实和 MySQL InnoDB 为何建议主键不宜过长一样。每个辅助索引都会存储主键的值，主键越长，每条索引项占用的内存就越大，缓存页一次从磁盘获取的索引数就越少，一次查询需要访问磁盘次数就可能变多。而磁盘访问我们都知道很慢。

##   
**5.2 内存映射如何实现？**

那么你知道 Kafka 索引底层的实现原理吗？其实就是**内存映射文件**，即 Java 中的 **MappedByteBuffer**。

使用**内存映射文件**的主要优势在于，它有很高的 I/O 性能，特别是对于索引这样的小文件来说，由于文件内存被直接映射到一段虚拟内存上，访问内存映射文件的速度要快于普通的读写文件速度。

在 AbstractIndex 中，这个 MappedByteBuffer 就是名为 mmap 的变量，我们来看下它的实现，源码如下：

@volatile

protected var mmap: MappedByteBuffer = {

// 1、尝试创建索引文件，如果没创建就新创建，然后返回 true，否则返回 false。

val newlyCreated \= file.createNewFile()

// 2、以 writable 指定的方式（读写方式或只读方式）来打开索引文件

val raf \= if (writable) new RandomAccessFile(file, "rw") else new RandomAccessFile(file, "r")

try {

/\* pre-allocate the file if necessary \*/

if(newlyCreated) {

// 3、预设的索引文件大小不能太小，如果连一个索引项都保存不了，直接抛出异常

if(maxIndexSize < entrySize)

throw new IllegalArgumentException("Invalid max index size: " + maxIndexSize)

// 4、设置索引文件长度，roundDownToExactMultiple 计算的是不超过 maxIndexSize 的最大整数倍entrySize（8个字节）

// 举例：maxIndexSize = 1234567 字节，entrySize = 8字节，那么调整后的文件长度为 1234560 字节

raf.setLength(roundDownToExactMultiple(maxIndexSize, entrySize))

}

/\* memory-map the file \*/

// 5、更新索引文件长度字段 \_length

\_length = raf.length()

// 6、创建 MappedByteBuffer 对象，把索引文件映射到内存中。

val idx \= {

if (writable)

raf.getChannel.map(FileChannel.MapMode.READ\_WRITE, 0, \_length)

else

raf.getChannel.map(FileChannel.MapMode.READ\_ONLY, 0, \_length)

}

/\* set the position in the index for the next entry \*/

// 7、如果是新创建的索引文件，将 MappedByteBuffer 对象的当前位置设置成 0

// 如果索引文件已存在，将 MappedByteBuffer 对象的当前位置设置成最后一个索引项所在的位置

if(newlyCreated)

idx.position(0)

else

// if this is a pre-existing index, assume it is valid and set position to last entry

idx.position(roundDownToExactMultiple(idx.limit(), entrySize))

// 8、返回创建的 MappedByteBuffer 对象

idx

} finally {

CoreUtils.swallow(raf.close(), AbstractIndex)

}

}

该方法有几个重要字段，这里先说明下：

1.  **entrySize**：一个索引项的大小，正如上面分析的：OffsetIndex 是 8 个字节，TimeIndex 是 12 个字节。
2.  **\_length**：索引文件的长度。
3.  **mmap**：创建索引文件并把索引文件映射到内存中。

该方法最重要的就是创建 **mmap 对象**，整体的内存映射步骤如下：

1.  尝试创建索引文件，如果没创建就新创建，然后返回 true，否则返回 false。
2.  以 writable 指定的方式（读写方式或只读方式）来打开索引文件。
3.  预设的索引文件大小不能太小，如果连一个索引项都保存不了，直接抛出异常。
4.  设置索引文件长度，roundDownToExactMultiple 计算的是不超过 maxIndexSize 的最大整数倍entrySize（8个字节）。比如，maxIndexSize=1234567字节，entrySize=8字节，那么调整后的文件长度为1234560字节。
5.  更新索引文件长度字段 \_length。
6.  创建 MappedByteBuffer 对象，把索引文件映射到内存中。
7.  如果是新创建的索引文件，将 MappedByteBuffer 对象的当前位置设置成 0。如果索引文件已存在，将 MappedByteBuffer 对象的当前位置设置成最后一个索引项所在的位置
8.  返回创建的 MappedByteBuffer 对象。

用一张图来梳理下整个流程：

![](https://article-images.zsxq.com/Fq0q-NK3or_IMEHUfggRr8K8kp_x)

讲到这里，这里有几个算式需要我们来了解下，如下：

如果我们要计算索引对象中**当前有多少个索引项时**，只要通过下面算式就可以得出：

/\*\* The number of entries in this index \*/

@volatile

// 如果我们要计算索引对象中当前有多少个索引项，只需要执行下列计算：

protected var \_entries: Int = mmap.position() / entrySize

如果我们要计算索引文件**最多能容纳多少个索引项**，只要通过下面算式就可以得出：

/\*\*

\* The maximum number of entries this index can hold

\* 如果我们要计算索引文件最多能容纳多少个索引项，只要定义下面的变量就行了：

\*/

@volatile

private\[this\] var \_maxEntries: Int = mmap.limit() / entrySize

有了上面这两个字段后，我们就能够很容易的计算出**当前索引文件是否已经写满了**：

/\*\*

\* True iff there are no more slots available in this index

\* 再进一步，有了这两个变量，我们就能够很容易地编写一个方法，来判断当前索引文件是否已经写满：

\*/

def isFull: Boolean = \_entries >= \_maxEntries

总之，AbstractIndex 中最重要的就是这个 **mmap** 变量了。其子类的相关操作也都是和 **mmap** 有关。

接下来我们来看下其子类的相关实现。文章开头已经给出了日志索引类型，一种是**基于偏移量的索引**，一种是基于**基于时间戳的索引**，还有一种是**基于已终止事务的索引**，这个后续有时间再进行补充。

**基于时间戳的索引**的基础是**基于偏移量的索引**，了解了偏移量的索引就了解了基于时间戳的索引，我们挨个来看下。

##   
**06 OffsetIndex 源码分析**

**偏移量索引把偏移量映射到一个特定的日志段物理文件中**。这个索引就是「**稀疏索引**」，即只索引日志中部分消息。

这里有下面几个点需要你了解下：

1.  索引是保存在物理文件中的，即.index 文件且每个索引项都是固定大小：8 个字节。
2.  索引支持针对在物理文件的内存映射的高效查找。查找是通过二分查找法去定位小于等于目标偏移量中最大的偏移量/位置对。
3.  索引文件有两种打开方式。
4.  第一种是按可变索引文件打开，允许消息日志追加。
5.  第二种是按不可变只读索引文件打开，这种文件是以前填充完的索引文件，当新的索引文件创建后，旧的索引文件会从可变索引文件转换为不可变索引文件，并删除多余的字节。
6.  索引文件是一系列的条目，格式为 < 4 字节相对偏移量，对应偏移量的 4 字节文件磁盘物理位置 > 键值对。
7.  所有对外提供的API会把**相对偏移量转换为绝对偏移量**。

## **6.1 重要字段**

先来看下类定义：

class OffsetIndex(\_file: File, baseOffset: Long, maxIndexSize: Int = -1, writable: Boolean = true)

extends AbstractIndex(\_file, baseOffset, maxIndexSize, writable) {

override def entrySize \= 8

/\* the last offset in the index \*/

private\[this\] var \_lastOffset \= lastEntry.offset

def lastOffset: Long = \_lastOffset

....

}

1.  **file 索引文件**：每个索引对象在磁盘上都对应了一个索引文件。
2.  **baseOffset 起始位移值**：对应日志文件中第一个消息的offset。
3.  **maxIndexSize 索引文件最大字节数**：它控制索引文件的最大长度，这里为 -1。
4.  **writable 索引文件打开方式**：这里为 true 表示以**读写**方式打开。
5.  **mmap**：用来操作索引文件的 MappedByteBuffer。
6.  **lock**：ReentrantLock 对象，在对 mmap 进行操作时，需要加锁保护。
7.  **entries**：当前索引文件中的索引项个数。
8.  **maxEntries**：当前索引文件中最多能够保存的索引项个数。
9.  **lastOffset**：保存最后一个索引项的 offset。

对于位移索引文件来说，主要操作方法就是「**写入索引项**」、「**查询索引项**」，先来看下这两个方法。

##   
**6.2 写入索引项**

/\*\*

\* Append an entry for the given offset/location pair to the index. This entry must have a larger offset than all subsequent entries.

\* @throws IndexOffsetOverflowException if the offset causes index offset to overflow

\*/

def append(offset: Long, position: Int): Unit = {

inLock(lock) {

// 1、判断索引文件未写满

require(!isFull, "Attempt to append to a full index (size = " + \_entries + ").")

// 2、必须满足以下条件之一才允许写入索引项：

// 条件1：当前索引文件为空

// 条件2：要写入的位移大于当前所有已写入的索引项的位移 —— Kafka 规定索引项中的位移值必须是单调增加的

if (\_entries == 0 || offset > \_lastOffset) {

trace(s"Adding index entry $offset => $position to ${file.getAbsolutePath}")

// 3、向 mmap 中写入相对位移值

mmap.putInt(relativeOffset(offset))

// 3、向 mmap 中写入物理位置信息

mmap.putInt(position)

// 4、更新其他元数据统计信息，当前索引项计数器\_entries 和当前索引项最新位移值\_lastOffset

\_entries += 1

\_lastOffset = offset

// 5、执行校验。写入的索引项格式必须符合要求，即索引项个数\*单个索引项占用字节数匹配当前文件物理大小，否则说明文件已损坏

require(\_entries \* entrySize == mmap.position(), s"$entries entries but file position in index is ${mmap.position()}.")

} else {

// 如果第 2 步中两个条件都不满足，不能执行写入索引项操作，抛出异常

throw new InvalidOffsetException(s"Attempt to append an offset ($offset) to position $entries no larger than" +

s" the last offset appended (${\_lastOffset}) to ${file.getAbsolutePath}.")

}

}

}

该方法主要用来**写入索引项**的，步骤如下：

1.  判断索引文件未写满，否则就抛异常。
2.  必须满足以下条件之一才允许写入索引项：
3.  当前索引文件为空。
4.  要写入的偏移量大于当前所有已写入的索引项的偏移量——Kafka规定索引项中的位移值必须是单调增加的。
5.  向索引文件对应的 mmap 中写入索引项。包括**写入相对位移值和对应的日志文件的物理位置**。
6.  更新其他元数据统计信息，当前索引项计数器 \_entries 和当前索引项最新位移值 \_lastOffset。
7.  执行校验。写入的索引项格式必须符合要求，即索引项个数乘以单个索引项占用字节数匹配当前文件物理大小，否则说明文件已损坏。

下面通过一张图来梳理下写入流程：

![](https://article-images.zsxq.com/Fgv9xk4z9WY-dFf7sphtFTs61hRc)

写入索引项逻辑相对比较简单，难点的是如何高效查找索引项，接下来我们来看下。

## **6.3 查询索引项**

AbstractIndex 定义了抽象方法 parseEntry 用来查找给定的索引项，源码如下：

/\*\*

\* To parse an entry in the index.

\*

\* @param buffer the buffer of this memory mapped index.

\* @param n the slot

\* @return the index entry stored in the given slot.

\*/

protected def parseEntry(buffer: ByteBuffer, n: Int): IndexEntry

// 接口

sealed trait IndexEntry {

// We always use Long for both key and value to avoid boxing.

def indexKey: Long

def indexValue: Long

}

/\*\*

\* The mapping between a logical log offset and the physical position

\* in some log file of the beginning of the message set entry with the

\* given offset.

\*/

case class OffsetPosition(offset: Long, position: Int) extends IndexEntry {

override def indexKey \= offset

override def indexValue \= position.toLong

}

参数里的 **n** 表示要查找给定 ByteBuffer 中保存的第 n 个索引项。「**IndexEntry**」是源码定义的一个接口，里面有两个方法：「**indexKey**」 和「**indexValue**」，分别返回不同类型索引的 <Key，Value> 对。

我们来看下 「**OffsetIndex**」类实现 「**parseEntry**」的源码如下：

// 查找指定索引项 n 表示第几个索引项

override protected def parseEntry(buffer: ByteBuffer, n: Int): OffsetPosition = {

// 转换真实的 Offset

// 计算绝对位移值 baseOffset + relativeOffset(buffer, n)

// 计算物理位置 physical(buffer, n)

OffsetPosition(baseOffset + relativeOffset(buffer, n), physical(buffer, n))

}

private def toRelative(offset: Long): Option\[Int\] = {

// 真实位移值转换相对位移值

val relativeOffset \= offset - baseOffset

if (relativeOffset < 0 || relativeOffset > Int.MaxValue)

None

else

Some(relativeOffset.toInt)

}

/\*\*

\* Get the nth offset mapping from the index

\* @param n The entry number in the index

\* @return The offset/position pair at that entry

\*/

def entry(n: Int): OffsetPosition = {

maybeLock(lock) {

if (n >= \_entries)

throw new IllegalArgumentException(s"Attempt to fetch the ${n}th entry from index ${file.getAbsolutePath}, " +

s"which has size ${\_entries}.")

parseEntry(mmap, n)

}

}

通过上面的源码，可以看出 「**OffsetPosition**」是 「**IndexEntry**」的实现类，其中 Key 是位移值， Value 是物理磁盘位置。所以，这里你能看到代码调用了 [relativeOffset(buffer, n) + baseOffset](http://relativeoffset\(buffer,%20n\)%20+%20baseoffset/) 计算出相对位移值，之后调用 [physical(buffer, n)](http://physical\(buffer,%20n\)/) 计算物理磁盘位置，最后将它们合在一起作为一个索引项进行返回。

接下来我们分别来看下这两个方法是如何实现的。

##   
**6.4 计算相对位移值**

// n \* entrySize 表示相对位移 \* 索引项大小

private def relativeOffset(buffer: ByteBuffer, n: Int): Int = buffer.getInt(n \* entrySize)

该方法主要是**根据索引文件和索引项编号来找到相对位移值**。通过 entrySize 和 buffer.getInt 方法能够轻松地计算出第 n 个索引项所处的物理文件位置，最后读出消息在索引文件的**相对位移值**。

##   
**6.5 计算物理位置**

private def physical(buffer: ByteBuffer, n: Int): Int = buffer.getInt(n \* entrySize + 4)

  
该方法主要是**从索引文件中找到消息在物理文件中的位置**。通过 entrySize 和 buffer.getInt 方法能够轻松地计算出第 n 个索引项所处的物理文件位置然后加 4，可以读出消息在日志文件的物理位置。

有了上面这几个方法，我们就**能够根据给定的 n 来查找索引项和物理位置了**。但是还有个问题需要解决：**我们如何来定位要找的索引项在第 n 个槽中呢？**也就是如何从一组已排序的数中快速定位符合条件的那个数，接下来该二分查找隆重登场了，先来看下槽位如何定位？

## **6.6 定位槽位**

def lookup(targetOffset: Long): OffsetPosition = {

// 1、尝试加锁

maybeLock(lock) {

// 2、用私有变量做一个 mmap 的镜像，防止有新的索引进来，影响一致性。

val idx \= mmap.duplicate

// 3、使用了改进版的二分查找算法寻找对应的槽位

val slot \= largestLowerBoundSlotFor(idx, targetOffset, IndexSearchType.KEY)

// 4、如果没找到，返回一个空的位置，即物理文件位置从0开始，表示从头读日志文件，否则返回 slot 槽对应的索引项

if(slot == -1)

OffsetPosition(baseOffset, 0)

else

parseEntry(idx, slot)

}

}

/\*\*

\* Execute the given function in a lock only if we are running on windows or z/OS. We do this

\* because Windows or z/OS won't let us resize a file while it is mmapped. As a result we have to force unmap it

\* and this requires synchronizing reads.

\*/

protected def maybeLock\[T\](lock: Lock)(fun: => T): T = {

if (OperatingSystem.IS\_WINDOWS || OperatingSystem.IS\_ZOS)

lock.lock()

try fun

finally {

if (OperatingSystem.IS\_WINDOWS || OperatingSystem.IS\_ZOS)

lock.unlock()

}

}

该方法主要用来根据**改进版的二分查找方法来定位对应槽位**，步骤如下：

1.  尝试加锁。
2.  用私有变量做一个 mmap 的镜像，防止有新的索引进来，影响一致性。
3.  使用了改进版的二分查找算法寻找对应的槽位。
4.  如果没找到，返回一个空的位置，即物理文件位置从 0 开始，表示从头读日志文件，否则返回 slot 槽对应的索引项。

上面最重要的是 「**第二步**」，这是抽象类的方法，接下来我们来深度剖析下。

##   
**6.7 \_warmEntries 是什么？**

其定义如下：

protected def \_warmEntries: Int = 8192 / entrySize

那么这个字段是做什么用的呢？

通过上面的学习，这里你思考下，我们能通过「**索引项**」快速找到日志段中的消息，但是我们**如何快速找到我们想要的索引项呢？**

一个索引文件默认大小为 10 MB，一个索引项为 8 Byte，因此一个日志段文件可能包含100 多W条索引项。

不论是「**消息**」还是「**索引**」，其实都是「**单调递增**」的，并且都是「**尾部追加写入**」的，因此数据都是「**有序**」的。在有序的集合中快速查询，脑海中突现的就是「**二分查找**」了！

这里我们先来个二分查找的源码看下：

def binarySearch(begin: Int, end: Int) : (Int, Int) = {

// binary search for the entry

var lo \= begin

var hi \= end

while(lo < hi) {

val mid \= (lo + hi + 1) >>> 1

val found \= parseEntry(idx, mid)

val compareResult \= compareIndexEntry(found, target, searchEntity)

if(compareResult > 0)

hi = mid - 1

else if(compareResult < 0)

lo = mid

else

return (mid, mid)

}

(lo, if (lo == \_entries - 1) -1 else lo + 1)

}

那么这和 **\_warmEntries** 有什么关系？首先你想想二分查找有什么问题？

这是因为 Kafka 写入索引文件的方式是**在文件末尾追加写入**，并且一般写入的数据立马就会被读取。所以几乎所有的索引查询都**集中在索引的尾部**。并且操作系统基本上都是**用页为单位缓存和管理内存的**，**内存又是有限的**，因此会通过「**LRU 机制**」来淘汰内存。所以「**LRU 机制**」是非常适合 Kafka 的索引访问场景的。

但是当 Kafka 在查询索引的时候，「**原版二分查找算法**」并没有考虑到**缓存情况**，很可能会导致一些不必要的缺页中断（Page Fault），毕竟**二分是跳着来访问的**。此时 Kafka 线程会被阻塞，**等待对应的索引项从物理磁盘中读出并放入到页缓存中**。

这里要特意说一下 kafka 的注释写的真是清晰啊，来看看注释是怎么说的：

when looking up index, the standard binary search algorithm is not cache friendly, and can cause unnecessary page faults (the thread is blocked to wait for reading some index entries from hard disk, as those entries are not cached in the page cache)

翻译下：当我们查找索引的时候，标准的二分查找对缓存不友好，可能会造成不必要的缺页中断(线程被阻塞等待从磁盘加载没有被缓存到 page cache 的数据)。

注释还友好的给出了例子：

![](https://article-images.zsxq.com/FjWcsgAIHc0hBWE-296ELiXav3rh)

##   
**6.8 改进版二分查找**

/\*\*

\* Find the slot in which the largest entry less than or equal to the given target key or value is stored.

\* The comparison is made using the \`IndexEntry.compareTo()\` method.

\*

\* @param idx The index buffer

\* @param target The index key to look for

\* @return The slot found or -1 if the least entry in the index is larger than the target key or the index is empty

\*/

protected def largestLowerBoundSlotFor(idx: ByteBuffer, target: Long, searchEntity: IndexSearchEntity): Int =

indexSlotRangeFor(idx, target, searchEntity).\_1

可以看到内部调用了「**indexSlotRangeFor**」方法来进行查找，到目前为止，从已排序数组中寻找某个数字最快速的算法就是「**二分查找**」了，它能做到 O(lgN) 的时间复杂度。Kafka 的索引组件也使用了二分查找算法。

下面举个例子，简单的来讲，假设某索引占用了操作系统的 「**PageCache**」**13** 页，此时数据已经写到了 **12** 页。按照 kafka 访问的特性，此时访问的数据都在第 **12** 页。

根据「**二分查找**」算法的特性，此时缓存页的访问顺序依次是 **0**，**6**，**9**，**11**，**12**。因为频繁被访问，所以这几页一定存在「**PageCache**」中。具体推演过程如下：

![](https://article-images.zsxq.com/FjU2u8u8O5i_lNtyOiIsR402LzD3)

通常来说，一个页上保存了成百上千的索引项数据。**随着索引文件不断被写入**，**当上图第 12 页不断被填充**，满了之后会申请新页第 **13** 页保存「**索引项**」，而按照「**原版二分查找算法**」的特性，此时缓存页的访问顺序依次是：**0**，**7**，**10**，**12**，**13**。

此时，问题来了，Page #7 和 Page #10 已经很久没被访问到了，很可能已经不再「**PageCache**」中了，然后需要从磁盘上读取数据。

注释说：**在他们的测试中**，**原这会导致至少会产生从几毫秒跳到1秒的延迟**。

基于以上问题，Kafka 使用了「**改进版二分查找算法**」，改的不是二分查找的内部，而且把所有**索引项分为热区和冷区**，然后有条件地在不同区域执行普通的二分查找算法罢了。

实际上，这个改进版算法提供了一个重要的保证：**它能保证那些经常被访问的 Page 组合是固定的**。

由于 kafka 消息写入是顺序追加写的，所以读的时候一般在索引的尾部。也就是说大部分查询集中在索引项的尾部，所以把尾部的 8192 字节设置为热区，其余的部分为冷区，分别进行二分查找。

这个改进版算法的最大好处在于：**它查询最热的那部分数据所遍历的 Page 永远是固定的**，**因此大概率会在页缓存中**，这样能避免缺页中断。

看到这里其实我想到了**一致性 hash**，**一致性 hash相对于普通的 hash 不就是在 node 新增的时候缓存的访问固定**，**或者只需要迁移少部分数据即可**。

好了，让我们先看看源码是如何做的：

/\*\*

\* Lookup lower and upper bounds for the given target.

\*/

private def indexSlotRangeFor(idx: ByteBuffer, target: Long, searchEntity: IndexSearchEntity): (Int, Int) = {

// 1、如果索引为空，直接返回 <-1,-1> 对

if(\_entries == 0)

return (-1, -1)

// 内部封装了原版的二分查找算法

def binarySearch(begin: Int, end: Int) : (Int, Int) = {

// binary search for the entry

var lo \= begin

var hi \= end

while(lo < hi) {

val mid \= (lo + hi + 1) >>> 1

val found \= parseEntry(idx, mid)

val compareResult \= compareIndexEntry(found, target, searchEntity)

if(compareResult > 0)

hi = mid - 1

else if(compareResult < 0)

lo = mid

else

return (mid, mid)

}

(lo, if (lo == \_entries - 1) -1 else lo + 1)

}

// 2、确认热区首个索引项位于哪个槽。\_warmEntries 就是所谓的分割线，目前源码写死为 8192 字节处

// 对于 OffsetIndex 来说，\_warmEntries = 8192 / 8 = 1024，即第 1024 个槽

// 对于 TimeIndex 来说，\_warmEntries = 8192 / 12 = 682，即第 682 个槽

val firstHotEntry \= Math.max(0, \_entries - 1 - \_warmEntries)

// 3、判断 target 位移值在热区还是冷区

if(compareIndexEntry(parseEntry(idx, firstHotEntry), target, searchEntity) < 0) {

return binarySearch(firstHotEntry, \_entries - 1)

}

// 4、确保 target 位移值不能小于当前最小位移值

if(compareIndexEntry(parseEntry(idx, 0), target, searchEntity) > 0)

return (-1, 0)

// 5、如果在冷区，则搜索冷区

binarySearch(0, firstHotEntry)

}

// 该方法的作用是比较索引项与目标的大小，这是基于目标值和索引项中的索引键或索引值进行比较。在执行搜索操作时，常常会用到这个方法。

// 返回值为比较结果的整数，如果目标值小于索引项值则返回负数，如果目标值等于索引项值则返回零，如果目标值大于索引项值则返回正数。

private def compareIndexEntry(indexEntry: IndexEntry, target: Long, searchEntity: IndexSearchEntity): Int = { // 表示要比较的索引项类型，是索引键还是索引值

searchEntity match {

// 将索引项的索引键与目标值进行比较

case IndexSearchType.KEY => java.lang.Long.compare(indexEntry.indexKey, target)

// 将索引项的索引值与目标值进行比较

case IndexSearchType.VALUE => java.lang.Long.compare(indexEntry.indexValue, target)

}

}

该方法主要用来根据「**改进版的二分查找算法**」来定位，步骤如下：

1.  如果索引为空，直接返回 <-1,-1> 对。
2.  确认热区首个索引项位于哪个槽。\_warmEntries 就是所谓的分割线，目前源码写死为 8192 字节处。
3.  对于 OffsetIndex 来说，\_warmEntries = 8192 / 8 = 1024，即第 1024 个槽。
4.  对于 TimeIndex 来说，\_warmEntries = 8192 / 12 = 682，即第 682 个槽。
5.  判断 target 位移值在热区还是冷区。
6.  确保 target 位移值不能小于当前最小位移值。
7.  如果在冷区，则搜索冷区。

下面通过一张图来梳理下：

![](https://article-images.zsxq.com/FhzWG1jkXUJDe_q0qRcygBnBslB1)

所以 Kafka 这个改进版**能够有效地提升页缓存的使用率**，**从而在整体上降低物理 I/O**，**理缓解系统负载瓶颈**。

这里简单总结一下：

> 在 Kafka 索引中使用普通二分搜索会出现缺页中断的现象，造成延迟，且结合查询大多集中在尾部的情况，通过将索引区域划分为热区和冷区，分别搜索，将尽可能保证热区中的页在 page cache 中，从而避免缺页中断。

接下来，我们看下另外一个常见操作：截断操作。

## **6.9 截断操作**

// 覆写 AbstractIndex 类的 truncate 方法，将索引文件截断至第一条索引记录之前，即清空索引文件。

override def truncate() = truncateToEntries(0)

/\*\*

\* 根据索引条目数截断索引文件

\*/

private def truncateToEntries(entries: Int): Unit = {

// 1、使用锁确保线程安全。

inLock(lock) {

// 2、将索引文件内存映射到指定大小。

\_entries = entries

// 3、将文件指针设置到索引区的末尾。

mmap.position(\_entries \* entrySize)

// 4、更新索引文件中的 \_lastOffset 字段，将其设置为当前索引中的最后一个条目的偏移量。

\_lastOffset = lastEntry.offset

// 记录日志。该方法的作用是根据指定的索引条目数截断索引文件，并更新相应的元数据信息。

debug(s"Truncated index ${file.getAbsolutePath} to $entries entries;" +

s" position is now ${mmap.position()} and last offset is now ${\_lastOffset}")

}

}

截断操作就是指，**将索引文件内容直接裁剪掉一部分**。比如，OffsetIndex 索引文件中当前保存了 100 个索引项，我想只保留最开始的 40 个索引项。步骤如下：

1.  使用锁确保线程安全。
2.  将索引文件内存映射到指定大小。
3.  将文件指针设置到索引区的末尾。
4.  更新索引文件中的 \_lastOffset 字段，将其设置为当前索引中的最后一个条目的偏移量。

这个方法接收 entries 参数，表示**要截取到哪个槽**，主要的逻辑实现是调用 mmap.position 方法。源码中的 \_entries \* entrySize 就是 **mmap 要截取到的字节处**。

至此，**OffsetIndex** 类源码已经剖析完了，接下来我们来看下 **Timendex** 的源码。

##   
**07 TimeIndex 源码分析**

与 OffsetIndex 不同的是，TimeIndex 保存的是 < 时间戳，相对位移值 > 键值对。时间戳需要一个长整型来保存，相对位移值使用 Integer 来保存。因此，**Timendex** **单个索引项需要占用 12 个字节**。这样说明：**在保存同等数量索引项的基础上**，TimeIndex 会比 OffsetIndex 占用更多的磁盘空间。

## **7.1 重要字段**

先来看下类定义：

class TimeIndex(\_file: File, baseOffset: Long, maxIndexSize: Int = -1, writable: Boolean = true)

extends AbstractIndex(\_file, baseOffset, maxIndexSize, writable) {

@volatile private var \_lastEntry \= lastEntryFromIndexFile

override def entrySize \= 12

// We override the full check to reserve the last time index entry slot for the on roll call.

override def isFull: Boolean = entries >= maxEntries - 1

def lastEntry: TimestampOffset = \_lastEntry

....

}

1.  **file 索引文件**：每个索引对象在磁盘上都对应了一个索引文件。
2.  **baseOffset 起始位移值**：对应日志文件中第一个消息的offset。
3.  **maxIndexSize 索引文件最大字节数**：它控制索引文件的最大长度，这里为 -1。
4.  **writable 索引文件打开方式**：这里为 true 表示以**读写**方式打开。
5.  **mmap**：用来操作索引文件的 MappedByteBuffer。
6.  **lock**：ReentrantLock 对象，在对 mmap 进行操作时，需要加锁保护。
7.  **lastEntries**：当前索引文件中最后一个索引项。

对于时间索引文件来说，主要操作方法就是「**写入索引项**」、「**查询索引项**」，先来看下这两个方法。

## **7.2 写入索引项**

/\*\*

\* Execute the given function inside the lock

\*/

def inLock\[T\](lock: Lock)(fun: => T): T = {

lock.lock()

try {

fun

} finally {

lock.unlock()

}

}

def maybeAppend(timestamp: Long, offset: Long, skipFullCheck: Boolean = false): Unit = {

inLock(lock) {

// 1、如果索引文件已写满，抛出异常

if (!skipFullCheck)

require(!isFull, "Attempt to append to a full time index (size = " + \_entries + ").")

// 2、确保索引单调增加性

if (\_entries != 0 && offset < lastEntry.offset)

throw new InvalidOffsetException(s"Attempt to append an offset ($offset) to slot ${\_entries} no larger than" + s" the last offset appended (${lastEntry.offset}) to ${file.getAbsolutePath}.")

// 3、确保时间戳的单调增加性

if (\_entries != 0 && timestamp < lastEntry.timestamp)

throw new IllegalStateException(s"Attempt to append a timestamp ($timestamp) to slot ${\_entries} no larger" + s" than the last timestamp appended (${lastEntry.timestamp}) to ${file.getAbsolutePath}.")

// 符合单调递增性

if (timestamp > lastEntry.timestamp) {

trace(s"Adding index entry $timestamp => $offset to ${file.getAbsolutePath}.")

// 4、向 mmap 写入时间戳

mmap.putLong(timestamp)

// 5、向 mmap 写入相对位移值

mmap.putInt(relativeOffset(offset))

// 6、更新其他元数据统计信息，当前索引项计数器 \_entries 和 当前条目设置为最新的条目\_lastEntry。

\_entries += 1

\_lastEntry = TimestampOffset(timestamp, offset)

require(\_entries \* entrySize == mmap.position(), s"${\_entries} entries but file position in index is ${mmap.position()}.")

}

}

}

该方法主要用来**写入索引项**的，步骤如下：

1.  判断索引文件未写满，否则就抛异常。
2.  确保索引单调增加性，必须满足以下条件之一才允许写入索引项：
3.  当前索引文件不为空。
4.  要写入的偏移量小于最后一个写入的索引项的偏移量——Kafka规定索引项中的位移值必须是单调增加的。
5.  确保时间戳的单调增加性，也必须满足以下条件之一才允许写入索引项：
6.  当前时间索引文件不为空。
7.  要写入的时间戳小于最后一个写入的索引项的时间戳。
8.  向索引文件对应的 mmap 中写入索引项。包括**写入时间戳和相对位移值**。
9.  更新其他元数据统计信息，当前索引项计数器 \_entries 和当前条目设置为最新的条目\_lastEntry。
10.  执行校验。写入的索引项格式必须符合要求，即索引项个数乘以单个索引项占用字节数匹配当前文件物理大小，否则说明文件已损坏。

下面通过一张图来梳理下写入流程：

![](https://article-images.zsxq.com/FjpMeDpwu_7tkLgGMb48d4IqLXeT)

和 OffsetIndex 类似，向 TimeIndex 写入索引项的主体逻辑，是向 mmap 分别写入时间戳和相对位移值。只不过，**除了校验位移值的单调递增性之外**，**TimeIndex 还要保证顺序写入的时间戳也是单调递增的**。

## **7.3 查询索引项**

AbstractIndex 定义了抽象方法用来查找给定的索引项，源码如下：

/\*\*

\* To parse an entry in the index.

\*

\* @param buffer the buffer of this memory mapped index.

\* @param n the slot

\* @return the index entry stored in the given slot.

\*/

protected def parseEntry(buffer: ByteBuffer, n: Int): IndexEntry

// 接口

sealed trait IndexEntry {

// We always use Long for both key and value to avoid boxing.

def indexKey: Long

def indexValue: Long

}

/\*\*

\* The mapping between a timestamp to a message offset. The entry means that any message whose timestamp is greater

\* than that timestamp must be at or after that offset.

\* @param timestamp The max timestamp before the given offset.

\* @param offset The message offset.

\*/

case class TimestampOffset(timestamp: Long, offset: Long) extends IndexEntry {

override def indexKey \= timestamp

override def indexValue \= offset

}

参数里的 **n** 表示要查找给定 ByteBuffer 中保存的第 n 个索引项。「**IndexEntry**」是源码定义的一个接口，里面有两个方法：「**indexKey**」 和「**indexValue**」，分别返回不同类型索引的 <Key，Value> 对。

我们来看下 「**TimeIndex**」类实现 「**parseEntry**」的源码如下：

// 查找指定索引项 n 表示第几个时间戳索引项

override def parseEntry(buffer: ByteBuffer, n: Int): TimestampOffset = {

// 转换真实的 Offset

// 计算时间戳 timestamp(buffer, n)

// 计算绝对位移值 baseOffset + relativeOffset(buffer, n)

TimestampOffset(timestamp(buffer, n), baseOffset + relativeOffset(buffer, n))

}

/\*\*

\* Get the nth timestamp mapping from the time index

\* @param n The entry number in the time index

\* @return The timestamp/offset pair at that entry

\*/

def entry(n: Int): TimestampOffset = {

maybeLock(lock) {

if(n >= \_entries)

throw new IllegalArgumentException(s"Attempt to fetch the ${n}th entry from time index ${file.getAbsolutePath} " +

s"which has size ${\_entries}.")

parseEntry(mmap, n)

}

}

通过上面的源码，可以看出 「**TimestampOffset**」是实现 「**IndexEntry**」的实现类，其中 Key 是时间戳， Value 是相对位移值。所以，这里你能看到代码调用 timestamp(buffer, n) 计算时间戳，之后调用了 relativeOffset(buffer, n) + baseOffset 计算出相对位移值，最后将它们合在一起作为一个时间戳索引项进行返回。

## **7.4 计算时间戳**

// 计算时间戳

private def timestamp(buffer: ByteBuffer, n: Int): Long = buffer.getLong(n \* entrySize)

该方法主要是**从时间戳索引文件中找到消息时间戳**。通过 entrySize 和 buffer.getLong 方法能够轻松地计算出第 n 个时间索引项所处的时间戳。

## **7.5 计算相对位移值**

// n \* entrySize 表示相对位移 \* 索引项大小

private def relativeOffset(buffer: ByteBuffer, n: Int): Int = buffer.getInt(n \* entrySize + 8)

该方法主要是**根据时间戳索引文件和索引项编号来找到相对位移值**。通过 entrySize 和 buffer.getInt 方法能够轻松地计算出第 n 个时间索引项所处的**相对位移值**然后加 8。

有了上面这几个方法，我们就**能够根据给定的 n 来查找时间戳索引项和相对位移值了**。但是还有个问题需要解决：**我们如何来定位要找的时间戳索引项在第 n 个槽中呢？**

## **7.6 定位槽位**

def lookup(targetTimestamp: Long): TimestampOffset = {

// 1、尝试加锁

maybeLock(lock) {

// 2、用私有变量做一个 mmap 的镜像，防止有新的索引进来，影响一致性。

val idx \= mmap.duplicate

// 3、使用了改进版的二分查找算法寻找对应的槽位

val slot \= largestLowerBoundSlotFor(idx, targetTimestamp, IndexSearchType.KEY)

// 4、如果没找到，返回一个 -1 的时间戳，否则返回 slot 槽对应的时间戳索引项

if (slot == -1)

TimestampOffset(RecordBatch.NO\_TIMESTAMP, baseOffset)

else

parseEntry(idx, slot)

}

}

该方法主要用来根据**改进版的二分查找方法来定位对应槽位**，步骤如下：

1.  尝试加锁。
2.  用私有变量做一个 mmap 的镜像，防止有新的索引进来，影响一致性。
3.  使用了改进版的二分查找算法寻找对应的槽位。
4.  如果没找到，返回一个 -1 的时间戳，否则返回 slot 槽对应的索引项。

其底层也是调用上一节剖析了「**改进版二分查找算法**」，这里就跳过了，请翻到上面查看。

接下来，我们看下另外一个常见操作：截断操作。

## **7.7 截断操作**

// 覆写 AbstractIndex 类的 truncate 方法，将时间戳索引文件截断至第一条索引记录之前，即清空索引文件。

override def truncate() = truncateToEntries(0)

/\*\*

\* 根据索引条目数截断索引文件

\*/

private def truncateToEntries(entries: Int): Unit = {

// 1、使用锁确保线程安全。

inLock(lock) {

// 2、将时间戳索引文件内存映射到指定大小。

\_entries = entries

// 3、将文件指针设置到时间戳索引区的末尾。

mmap.position(\_entries \* entrySize)

// 4、更新索引文件中的 \_lastEntry 字段，将其设置为最后一个索引项的偏移量。

\_lastEntry = lastEntryFromIndexFile

// 记录日志。该方法的作用是根据指定的索引条目数截断索引文件，并更新相应的元数据信息。

debug(s"Truncated index ${file.getAbsolutePath} to $entries entries; position is now ${mmap.position()} and last entry is now ${\_lastEntry}")

}

}

截断操作就是指，**将索引文件内容直接裁剪掉一部分**。比如，TimeIndex 索引文件中当前保存了 100 个时间戳索引项，我想只保留最开始的 40 个时间戳索引项。步骤如下：

1.  使用锁确保线程安全。
2.  将时间戳索引文件内存映射到指定大小。
3.  将文件指针设置到时间戳索引区的末尾。
4.  更新索引文件中的 \_lastEntry 字段，将其设置为最后一个索引项的偏移量。

这个方法接收 entries 参数，表示**要截取到哪个槽**，主要的逻辑实现是也是调用 mmap.position 方法。源码中的 \_entries \* entrySize 就是 **mmap 要截取到的字节处**。

至此，**TimeIndex** 类源码已经剖析完了。

##   
**08 总结**

这里，我们一起来总结一下这篇文章的重点。

1、对「**xxxIndex**」索引类对象进行梳理，包括顶层抽象设计和几个子类索引文件。

2、带你深度剖析了「**索引重要性**」和 「**日志索引介绍**」以及 「**索引在 kafka 中的源码实践**」。

3、接着带你深度剖析了「**AbstractIndex**」抽象类源码设计，了解「**索引项大小**」和 「**内存映射**」的实现和原因。

4、再接着带你深度剖析了「**OffsetIndex**」的源码设计，主要分为「**写入索引项**」、「**查询索引项**」、「**计算相对位移**」、「**计算物理位置**」、「**定位槽位**」、「**了解 \_warmEntries 是什么**」、「**改进版二分查找**」、「**截断操作**」等。

4、最后剖析了「**TimeIndex**」的源码设计，主要分为「**写入索引项**」、「**查询索引项**」、「**计算相对位移**」、「**计算时间戳**」、「**定位槽位**」、「**截断操作**」等。

下篇我们来深度剖析「**日志管理操作**」，大家期待，我们下期见。