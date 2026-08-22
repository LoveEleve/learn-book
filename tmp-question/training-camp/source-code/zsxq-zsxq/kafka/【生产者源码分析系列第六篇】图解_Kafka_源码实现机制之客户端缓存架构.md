大家好，我是 华仔, 又跟大家见面了。

上篇主要带大家深度剖析了「**Kafka 网络层收发总流程**」，今天主要聊聊 「**Kafka 客户端消息缓存架构设计**」，深度剖析下消息是如何进行缓存的。

![](https://article-images.zsxq.com/FoPuX9FrjgrbIzlS_Fnb9v89X6Nz)

## **01 总体概述**

通过场景驱动的方式，当被发送消息通过网络请求封装、NIO多路复用器监听网络读写事件并进行消息网络收发后，回头来看看消息是如何在客户端缓存的？

大家都知道 Kafka 是一款超高吞吐量的消息系统，主要体现在「**异步发送**」、「**批量发送**」、「**消息压缩**」。

跟本篇相关的是「**批量发送**」即生产者会将消息缓存起来，等满足一定条件后，Sender 子线程再把消息批量发送给 Kafka Broker。

这样好处就是「**尽量减少网络请求次数，提升网络吞吐量**」。

为了方便大家理解，所有的源码只保留骨干。

## **02 消息如何在客户端缓存的**

既然是批量发送，那么消息肯定要进行缓存的，那消息被缓存在哪里呢？又是如何管理的？

通过下面简化流程图可以看出，待发送消息主要被缓存在 RecordAccumulator 里。

![](https://article-images.zsxq.com/FsFhQdSczdg6TcqBSg0YbPZooXdR)

我以一个**真实生活场景**类比解说一下会更好理解。

既然说 **RecordAccumulator 像一个累积消息的仓库**，就拿快递仓库类比。

![](https://article-images.zsxq.com/Fn23QD9zKMcMlwZ6cuM6AyrJg8J-)

上图是一个快递仓库，堆满了货物。可以看到**分拣员**把**不同目的地**的包裹放入**对应目的地的货箱**，每装满一箱就放置在对应的区域。

那么**分拣员**就是指 **RecordAccumulator**，而**货箱以及各自所属的堆放区域**，就是 RecordAccumulator 中**缓存消息的地方**。所有封箱的都会**等待 sender 来取货**发送出去。

如果你看懂了上图，就大概理解了 RecordAccumulator 的架构设计和运行逻辑。

总结下仓库里有什么：

> 分拣员
> 
> 货物
> 
> 目的地
> 
> 货箱
> 
> 堆放区域

记住这些概念，都会体现在源码里，流程如下图所示：

![](https://article-images.zsxq.com/FnYieCOY-Ci_78lZvR6pWGADdyMz)

从上面图中可以看出：

1.  至少有一个业务主线程和一个 sender 线程同时操作 RecordAccumulator，所以它必须是**线程安全**的。
2.  在它里面有一个 ConcurrentMap 集合「**Kafka 自定义的 CopyOnWriteMap**」。key:TopicPartiton， value:Deque<ProducerBatch>，即以主题分区为单元，把消息以 ProducerBatch 为单位累积缓存，多个 ProducerBatch 保存在 Deque 队列中。当 Deque 中最新的 batch 不能容纳消息时，就会创建新的 batch 来继续缓存，并将其加入 Deque。
3.  通过 ProducerBatch 进行缓存数据，为了减少频繁申请销毁内存造成 Full GC 问题，Kafka 设计了经典的「**缓存池 BufferPool 机制**」。

综上可以得出 RecordAccumulator 类中有三个重要的组件：「**消息批次 ProducerBatch**」、「**自定义 CopyOnWriteMap**」、「**缓存池 BufferPool 机制**」。

由于篇幅原因，**RecordAccumulator 类放到下篇来讲解**。

先来看看 ProducerBatch，它是**消息缓存及发送消息的最小单位**。

github 源码地址如下：

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/internals/ProducerBatch.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/internals/ProducerBatch.java)

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/record/MemoryRecordsBuilder.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/record/MemoryRecordsBuilder.java)

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/record/MemoryRecords.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/record/MemoryRecords.java)

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/utils/ByteBufferOutputStream.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/utils/ByteBufferOutputStream.java)

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/internals/ProduceRequestResult.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/internals/ProduceRequestResult.java)

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/internals/FutureRecordMetadata.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/internals/FutureRecordMetadata.java)

![](https://article-images.zsxq.com/FsuPspDF6EBG4C9ZQ5zIrMl2-u9X)

通过调用关系可以看出，ProducerBatch 依赖 MemoryRecordsBuilder，而 MemoryRecordsBuilder 依赖 MemoryRecords 构建，所以 「**MemoryRecords 才是真正用来保存消息的地方**」。

## **02.1 MemoryRecords**

import java.nio.ByteBuffer;

public class MemoryRecords extends AbstractRecords {

public static MemoryRecordsBuilder builder(..){

// 重载builder 

return builder(...);

  }

public static MemoryRecordsBuilder builder(

    ByteBuffer buffer,

    // 消息版本

    byte magic,

    // 消息压缩类型

    CompressionType compressionType,

    // 时间戳

    TimestampType timestampType,

    // 基本位移

    long baseOffset,

    // 日志追加时间

    long logAppendTime,

    // 生产者id

    long producerId,

    // 生产者版本

    short producerEpoch,

    // 批次序列号

    int baseSequence,

    boolean isTransactional,

    // 是否是控制类的批次

    boolean isControlBatch,

    // 分区leader的版本

    int partitionLeaderEpoch) {

// 初始化MemoryRecordsBuilder类

return new MemoryRecordsBuilder(...);

  }

}

该类比较简单，通过 builder 方法可以看出依赖 ByteBuffer 来存储消息。**MemoryRecordsBuilder 类的构建**是通过 MemoryRecords.builder() 来初始化的。

来看看 MemoryRecordsBuilder 类的实现。

## **02.2 MemoryRecordBuilder**

public class MemoryRecordsBuilder implements AutoCloseable {

// 写操作关闭的输出流

private static final DataOutputStream CLOSED\_STREAM \= new DataOutputStream(new OutputStream() {

// 当向某个ByteBuffer关闭输出流写数据时抛异常

public void write(int b) {

throw new ...;

        }

    });

// 日志时间

private final TimestampType timestampType;

// 消息压缩类型

private final CompressionType compressionType;

// kafka对OutputStream接口的实现类，对ByteBuffer实现了自动扩容功能

private final ByteBufferOutputStream bufferStream;

// 消息的版本

private final byte magic;

// ByteBuffer的最初始位置

private final int initialPosition;

// 基本位移

private final long baseOffset;

// 消息追加的时间

private final long logAppendTime;

// 是否是控制类的批次

private final boolean isControlBatch;

// 分区leader的版本

private final int partitionLeaderEpoch;

// 写入上限

private final int writeLimit;

// batch头大小字节数

private final int batchHeaderSizeInBytes;

// 评估压缩率

private float estimatedCompressionRatio \= 1.0F;

// 对bufferStream添加压缩功能

private DataOutputStream appendStream;

// 是否是事务批次

private boolean isTransactional;

// 生产者id

private long producerId;

// 生产者版本

private short producerEpoch;

// 批次序列号

private int baseSequence;

// 压缩前要写入的消息体大小字节数

private int uncompressedRecordsSizeInBytes \= 0; 

// 压缩前写入的记录数（不包括头）

private int numRecords \= 0;

// 实际压缩率

private float actualCompressionRatio \= 1;

// 最大时间戳

private long maxTimestamp \= RecordBatch.NO\_TIMESTAMP;

// 最大时间戳偏移量

private long offsetOfMaxTimestamp \= -1;

// 最后的偏移量

private Long lastOffset \= null;

// 第一次追加消息的时间戳

private Long firstTimestamp \= null;

// 真正保存消息的地方

private MemoryRecords builtRecords;

从该类属性字段来看比较多，这里只讲2个关于字节流的字段。

1.  **CLOSED\_STREAM**：当关闭某个 ByteBuffer 也会把它对应的写操作输出流设置为 CLOSED\_STREAM，**目的就是防止再向该 ByteBuffer 写数据**，否则就抛异常。
2.  **bufferStream**：首先 MemoryRecordsBuilder 依赖 ByteBuffer 来完成消息存储。它会将 ByteBuffer 封装成 ByteBufferOutputStream 并实现了 Java NIO 的 OutputStream，这样就可以按照流的方式写数据了。同时 ByteBufferOutputStream 提供了**自动扩容 ByteBuffer 能力**。

来看看它的初始化构造方法。

public MemoryRecordsBuilder(ByteBuffer buffer,...) {

// 将MemoryRecordsBuilder关联的ByteBuffer封装成ByteBufferOutputStream流

this(new ByteBufferOutputStream(buffer), ...);

}

// 构造方法

public MemoryRecordsBuilder(

    ByteBufferOutputStream bufferStream,

    ...

    int writeLimit) {

        ....

// 初始位置

this.initialPosition = bufferStream.position();

// 1. 根据不同消息版本计算批次Batch头的长度

this.batchHeaderSizeInBytes = AbstractRecords.recordBatchHeaderSizeInBytes(magic, compressionType);

// 2. 调整对应的position

        bufferStream.position(initialPosition + batchHeaderSizeInBytes);

this.bufferStream = bufferStream;

// 3. 在bufferStream流外层套一层压缩流，再套一层DataOutputStream流

this.appendStream = new DataOutputStream(compressionType.wrapForOutput(this.bufferStream, magic));

    }

}

从构造函数可以看出，除了基本字段的赋值之外，会做以下**3件事情**：

1.  根据消息版本、压缩类型来**计算批次 Batch 头的大小长度**。
2.  通过**调整 bufferStream 的 position**，使其跳过 Batch 头部位置，就可以直接写入消息了。
3.  **对 bufferStream 增加压缩功能**。

看到这里，挺有意思的，不知读者是否意识到这里涉及到 「**ByteBuffer**」、「**bufferStream**」 、「**appendStream**」。

三者的关系是通过「**装饰器模式**」实现的，即 bufferStream 对 ByteBuffer 装饰实现扩容功能，而 appendStream 又对 bufferStream 装饰实现压缩功能。

![](https://article-images.zsxq.com/FgDGcsjgz1ozvF0hH3HnR2z8nt4V)

来看看它的核心方法。

### **02.2.1 appendWithOffset()**

// 追加新记录

public Long append(long timestamp, ByteBuffer key, ByteBuffer value, Header\[\] headers) {

return appendWithOffset(nextSequentialOffset(), timestamp, key, value, headers);

}

// 计算下一个连续偏移量

private long nextSequentialOffset() {

// lastOffset用来记录当前写入Record的offset，每次当有新Record写入时，都会递增它。

return lastOffset \=\= null ? baseOffset : lastOffset + 1;

}

// 根据偏移量追加消息

private Long appendWithOffset(

  long offset,

  boolean isControlRecord, 

  long timestamp, 

  ByteBuffer key,

  ByteBuffer value, 

  Header\[\] headers) {

try {

// 检查isControl标志是否一致

if (isControlRecord != isControlBatch)

throw new ...;

// 保证offset是递增的

if (lastOffset != null && offset <= lastOffset)

throw new ...;

// 检查时间戳      

if (timestamp < 0 && timestamp != RecordBatch.NO\_TIMESTAMP)

throw new ...;

// 只有V2版本才有header

if (magic < RecordBatch.MAGIC\_VALUE\_V2 && headers != null && headers.length > 0)

throw new ...;

// 更新firstTimestamp

if (firstTimestamp == null)

           firstTimestamp = timestamp;

// V2版本消息写入

if (magic > RecordBatch.MAGIC\_VALUE\_V1)         {

            appendDefaultRecord(offset, timestamp, key, value, headers);

return null;

        } else {

//V0、V1 版本消息写入(此处不进行剖析)

return appendLegacyRecord(offset, timestamp, key, value, magic);

        }

    } catch (IOException e) {

// 抛异常

    }

}

该方法主要用来**根据偏移量追加写消息**，会根据消息版本来写对应消息，但**需要明确的是 ProducerBatch 对标 V2 版本**。

来看看 V2 版本消息写入逻辑。

private void appendDefaultRecord(

  long offset, 

  long timestamp, 

  ByteBuffer key, 

  ByteBuffer value,

  Header\[\] headers) throws IOException {

// 1. 检查appendStream状态是否可以写

    ensureOpenForRecordAppend();

// 2. 计算写入多少偏移量

int offsetDelta \= (int) (offset - baseOffset);

// 3.计算本次写与第一次写之间时间差

long timestampDelta \= timestamp - firstTimestamp;

// 4.使用DefaultRecord.writeTo()方法会按照V2 版本格式写入appendStream流中，并返回压缩前的消息大小

int sizeInBytes \= DefaultRecord.writeTo(appendStream, offsetDelta, timestampDelta, key, value, headers);

// 5. 消息写入成功后更新RecordBatch的元信息

    recordWritten(offset, timestamp, sizeInBytes);

}

// 判断appendStream状态是否为CLOSED\_STREAM 

private void ensureOpenForRecordAppend() {

if (appendStream == CLOSED\_STREAM)

throw new ...;

}

// 消息写入成功后更新RecordBatch的元信息

private void recordWritten(long offset, long timestamp, int size) {

  ....

// 压缩前写入的记录数 + 1

  numRecords += 1;

// 压缩前要写入的消息体大小字节数 + size

  uncompressedRecordsSizeInBytes += size;

// 最后的偏移量 + offset

  lastOffset = offset;

if (magic > RecordBatch.MAGIC\_VALUE\_V0 && timestamp > maxTimestamp) {

// 赋值最大时间戳

      maxTimestamp = timestamp;

// 赋值最大时间戳偏移量

      offsetOfMaxTimestamp = offset;

  }

}

该方法主要用来**写入 V2 版本消息的**，主要做以下**5件事情**：

1.  **检查是否可写**：判断 appendStream 状态是否为 CLOSED\_STREAM，如果不是就可写，否则抛异常。
2.  计算本次要写入多少偏移量。
3.  计算本次写入和第一次写的时间差。
4.  按照 V2 版本格式**写入 appendStream 流**中，并返回压缩前的消息大小。
5.  成功后**更新 RecordBatch 的元信息**。

关于 [RecordBatch](http://recordbatch/) 的内部结构，可以点击 [Kafka RecordBatch结构图解](https://mp.weixin.qq.com/s/5bsxlZcUdaDXRR72Ovvjww) 这篇进行学习，这里就不赘述了。

### **02.2.2 hasRoomFor()**

public boolean hasRoomFor(long timestamp, ByteBuffer key, ByteBuffer value, Header\[\] headers) {

// 检查两个状态

// (1)appendStream流状态

// (2)当前已经写入的预估字节数是否超过了writeLimit写入上限

if (isFull())

return false;

// 每个RecordBatch至少可以写入一个Record，此时如果一个Record都没有，则可以继续写入

if (numRecords == 0)

return true;

final int recordSize;

if (magic < RecordBatch.MAGIC\_VALUE\_V2) {

// 预估V0、V1旧版本的Record大小

        recordSize = Records.LOG\_OVERHEAD + LegacyRecord.recordSize(magic, key, value);

    } else {

// 预估V2版本写入的Record大小

int nextOffsetDelta \= lastOffset == null ? 0 : (int) (lastOffset - baseOffset + 1);

        ...

        recordSize = DefaultRecord.sizeInBytes(nextOffsetDelta, timestampDelta, key, value, headers);

    }

// 已写入字节数 + 本次写入Record的预估字节数不能超过writeLimit写入上限

return this.writeLimit >= estimatedBytesWritten() + recordSize;

}

public boolean isFull() {

return appendStream \=\= CLOSED\_STREAM || 

      (this.numRecords > 0 && this.writeLimit <= estimatedBytesWritten());

}

该方法主要用来**估计当前 MemoryRecordsBuilder 是否还有空间来容纳要写入的 Record**，会在下面 ProducerBatch.tryAppend() 里面调用。

最后来看看小节开始提到的**自动扩容功能**。

### **02.2.3 expandBuffer()**

public class ByteBufferOutputStream extends OutputStream {

// 扩容因子1.1倍

private static final float REALLOCATION\_FACTOR \= 1.1f;

// 初始容量

private final int initialCapacity;

// 初始位置

private final int initialPosition;

// 计算是否需要扩容

public void ensureRemaining(int remainingBytesRequired) {

// 当写入字节数大于buffer当前剩余字节数就开启扩容

if (remainingBytesRequired > buffer.remaining())

     expandBuffer(remainingBytesRequired);

  }

// 扩容

private void expandBuffer(int remainingRequired) {

// 1. 评估需要多少空间

int expandSize \= Math.max((int) (buffer.limit() \* REALLOCATION\_FACTOR), buffer.position() + remainingRequired);

// 2. 申请新的ByteBuffer

ByteBuffer temp \= ByteBuffer.allocate(expandSize);

// 3. 获取写入上限

int limit \= limit();

// 4. 写状态转换为读状态

    buffer.flip();

// 5. 将buffer读到新申请的temp里

    temp.put(buffer);

// 6. 修改写模式的limit上限

    buffer.limit(limit);

// 7. 更新原来的buffer的position，防止被重复消费

    buffer.position(initialPosition);

// 8. 将引用指向新申请的ByteBuffer

    buffer = temp;

  }

}

该方法主要用来**判断是否需要扩容 ByteBuffer 的**，即当写入字节数大于 buffer 当前剩余字节数就开启扩容，扩容需要做以下**3件事情**：

1.  **评估需要多少空间**: 在「**扩容空间**」、「**真正需要多少字节**」之间取最大值，此处通过「**扩容因子**」来计算主要是因为**扩容是需要消耗系统资源的**，如果每次都按实际数据大小来进行分配空间，会浪费不必要的系统资源。
2.  **申请新的空间**：根据扩容多少申请新的 ByteBuffer，然后将原来的 ByteBuffer 数据拷贝进去，对应源码步骤：「**3 - 7**」。
3.  最后将引用指向新申请的 ByteBuffer。

接下来看看 ProducerBatch 的实现。

## **02.3 ProducerBatch**

public final class ProducerBatch {

// 批次最终状态

private enum FinalState { ABORTED, FAILED, SUCCEEDED }

// 批次创建时间  

final long createdMs;

// 批次对应的主题分区

final TopicPartition topicPartition;

// 请求结果的future

final ProduceRequestResult produceFuture;

// 用来存储消息的callback和响应数据

private final List<Thunk> thunks = new ArrayList<>();

// 封装MemoryRecords对象，用来存储消息的ByteBuffer

private final MemoryRecordsBuilder recordsBuilder;

// batch的失败重试次数

private final AtomicInteger attempts \= new AtomicInteger(0);

// 是否是被分裂的批次

private final boolean isSplitBatch;

// ProducerBatch的最终状态

private final AtomicReference<FinalState> finalState = new AtomicReference<>(null);

// Record个数

int recordCount;

// 最大Record字节数

int maxRecordSize;

// 最后一次失败重试发送的时间戳

private long lastAttemptMs;

// 最后一次向该ProducerBatch追加Record的时间戳

private long lastAppendTime;

// Sender子线程拉取批次的时间

private long drainedMs;

// 是否正在重试过，如果ProducerBatch中的数据发送失败，则会重新尝试发送

private boolean retry;

}

// 构造函数

public ProducerBatch(TopicPartition tp, MemoryRecordsBuilder recordsBuilder, long createdMs, boolean isSplitBatch) {

    ...

// 请求结果的future

this.produceFuture = new ProduceRequestResult(topicPartition);

    ...

}

一个 ProducerBatch 会存放一条或多条消息，通常把它称为「**批次消息**」。

先来看看几个重要字段：

1.  **topicPartition**：批次对应的主题分区，当前 ProducerBatch 中缓存的 Record 都会发送给该 TopicPartition。
2.  **produceFuture**：请求结果的 Future，通过 ProduceRequestResult 类实现。
3.  **thunks**：Thunk 对象集合，用来存储消息的 callback 和每个 Record 关联的 Feture 响应数据。
4.  **recordsBuilder**：封装 MemoryRecords 对象，用来存储消息的 ByteBuffer。
5.  **attemps**：batch 的失败重试次数，通过 **AtomicInteger 提供原子操作**来进行 Integer 的使用，**适合高并发情况下的使用**。
6.  **isSplitBatch**：是否是被分裂的批次，因单个消息过大导致一个 ProducerBatch 存不下，被分裂成多个 ProducerBatch 来存储的情况。
7.  **drainedMs**：Sender 子线程拉取批次的时间。
8.  **retry**：如果 ProducerBatch 中的数据发送失败，则会重新尝试发送。

在构造函数中，有个重要的依赖组件就是 「**ProduceRequestResult**」，而它是「**异步获取消息生产结果的类**」，简单剖析下。

### **02.3.1 ProduceRequestResult 类**

public class ProduceRequestResult {

// 通过一个count为1的CountDownLatch对象间接地实现了Future的功能。

private final CountDownLatch latch \= new CountDownLatch(1);

private final TopicPartition topicPartition;

// 用来记录broker端关联ProducerBatch中第一条Record分配的offset值

// 这样每个Record的真实offset就可以根据自身在ProducerBatch的位置计算出来了(baseOffset + relativeOffset)

private volatile Long baseOffset \= null;

// 构造函数

public ProduceRequestResult(TopicPartition topicPartition) {

this.topicPartition = topicPartition;

    }

// 当等到响应会会调该函数唤醒阻塞的主线程

public void done() {

if (baseOffset == null)

throw new ...;

this.latch.countDown();

    }

// 调用await()方法的线程会被挂起，它会等待直到count值为0才继续执行

public void await() throws InterruptedException {

        latch.await();

    }

// 和await()类似，只不过等待一定的时间后count值还没变为0的话就会继续执行

public boolean await(long timeout, TimeUnit unit) throws InterruptedException {

return latch.await(timeout, unit);

    }

}

该类通过 **CountDownLatch(1) 间接地实现了 Future 功能**，并让其他所有线程都在这个锁上等待，此时只需要**调用一次 countDown() 方法**就可以让其他所有等待的线程同时恢复执行。

![](https://article-images.zsxq.com/FsXfQeuEyN0zr-34m33bV0qQY86B)

当 Producer 发送消息时会间接调用「**[ProduceRequestResult.await](http://producerequestresult.await/)**」，此时线程就会等待服务端的响应。当服务端响应时调用「**[ProduceRequestResult.done](http://producerequestresult.done/)**」，该方法调用了「**CountDownLatch.countDown**」唤醒了阻塞在「**[CountDownLatch.await](http://countdownlatch.await/)**」上的主线程。这些线程后续可以**通过 ProduceRequestResult 的 error 字段**来判断本次请求成功还是失败。

接下来看看 ProducerBatch 类的重要方法。

### **02.3.2 tryAppend()**

public FutureRecordMetadata tryAppend(long timestamp, byte\[\] key, byte\[\] value, Header\[\] headers, Callback callback, long now) {

// 1.检查MemoryRecordsBuilder是否还有空间写入

if (!recordsBuilder.hasRoomFor(timestamp, key, value, headers)) {

return null;

    } else {

// 2.调用append()方法写入Record

Long checksum \= this.recordsBuilder.append(timestamp, key, value, headers);

// 3. 更新最大Record字节数

this.maxRecordSize = Math.max(this.maxRecordSize, AbstractRecords.estimateSizeInBytesUpperBound(magic(),recordsBuilder.compressionType(), key, value, headers));

        ...

// 4.构建FutureRecordMetadata对象

FutureRecordMetadata future \= new FutureRecordMetadata(this.produceFuture, this.recordCount,timestamp, checksum,key == null ? -1 : key.length,value == null ? -1 : value.length, Time.SYSTEM);

// 5. 将Callback和FutureRecordMetadata记录到thunks集合中

        thunks.add(new Thunk(callback, future));

// 6. 更新Record记录数

this.recordCount++;

// 7. 返回FutureRecordMetadata

return future;

    }

}

该方法主要用来**尝试追加写消息的**，主要做以下**6件事情**：

1.  通过 MemoryRecordsBuilder 的 hasRoomFor() **检查当前 ProducerBatch 是否还有足够的空间**来存储此次写入的 Record。
2.  调用 [MemoryRecordsBuilder.append](http://memoryrecordsbuilder.append/)() 方法**将 Record 追加到 ByteBuffer 中**。
3.  **创建 FutureRecordMetadata 对象**，底层继承了 Future 接口，对应此次 Record 的发送。
4.  将 Future 和消息的 callback 回调封装成 Thunk 对象，**放入 thunks 集合中**。
5.  更新 Record 记录数。
6.  返回 FutureRecordMetadata。

**可以看出该方法只是让 Producer 主线程完成了消息的缓存，并没有实现真正的网络发送**。

接下来简单看看 FutureRecordMetadata，它实现了 JDK 中 concurrent 的 Future 接口。

除了维护 ProduceRequestResult 对象外还维护了 relativeOffset 等字段，其中 **relativeOffset 用来记录对应 Record 在 ProducerBatch 中的偏移量**。

该类有2个值得注意的方法，get() 和 value()。

public RecordMetadata get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

    ...

// 依赖ProduceRequestResult的CountDown来实现阻塞等待

boolean occurred \= this.result.await(timeout, unit);

    ...

// 调用value()方法返回RecordMetadata对象

return valueOrError();

}

RecordMetadata valueOrError() throws ExecutionException {

    ...

return value();

}

该方法主要**依赖 ProduceRequestResult 的 CountDown 来实现阻塞等待**，最后调用 value() 返回 RecordMetadata 对象。

RecordMetadata value() {

    ...

// 将 partition、baseOffset、relativeOffset、时间戳（LogAppendTime | CreateTimeStamp）等信息封装成 RecordMetadata 对象返回

return new RecordMetadata(

      result.topicPartition(), 

      ...);

}

private long timestamp() {

return result.hasLogAppendTime() ? result.logAppendTime() : createTimestamp;

}

该方法主要通过各种参数封装成 **RecordMetadata 对象**返回。

了解了 ProducerBatch 是如何写入数据的，我们再来看看 done() 方法。

当 Producer 收到 Broker 端「**正常**」|「**超时**」|「**异常**」|「**关闭生产者**」等响应都会调用 ProducerBatch 的 done()方法。

### **02.3.3 done()**

public boolean done(long baseOffset, long logAppendTime, RuntimeException exception) {

// 1.根据exception决定本次ProducerBatch发送的最终状态

final FinalState tryFinalState \= (exception == null) ? FinalState.SUCCEEDED : FinalState.FAILED;

    ....

// 2.通过CAS操作更新finalState状态，只有第一次更新的时候，才会触发completeFutureAndFireCallbacks()方法

if (this.finalState.compareAndSet(null, tryFinalState)) {

// 3.执行回调

        completeFutureAndFireCallbacks(baseOffset, logAppendTime, exception);

return true;

    }

    ....

return false;

}

该方法主要用来**是否可以执行回调操作**，即当收到该批次响应后，判断批次 Batch 最终状态是否可以执行回调操作。

### **03.3.4 completeFutureAndFireCallbacks()**

private void completeFutureAndFireCallbacks(long baseOffset, long logAppendTime, RuntimeException exception) {

// 1.更新ProduceRequestResult中的相关字段

  produceFuture.set(baseOffset, logAppendTime, exception);

// 2.遍历thunks集合，触发每个Record的Callback回调

for (Thunk thunk : thunks) {

try {

if (exception == null) {

// 3.获取消息元数据

RecordMetadata metadata \= thunk.future.value();

if (thunk.callback != null)

//4.调用回调方法

             thunk.callback.onCompletion(metadata, null);

          } else {

if (thunk.callback != null)

// 4.调用回调方法

                  thunk.callback.onCompletion(null, exception);

          }

      } 

      ....

  }

// 4.调用底层 CountDownLatch.countDown()方法，阻塞在其上的主线程。

  produceFuture.done();

}

该方法主要用来**调用回调方法和完成 future**，主要做以下**3件事情**：

1.  更新 ProduceRequestResult 中的相关字段，包括基本位移、消息追加的时间、异常。
2.  遍历 thunks 集合，触发每个 Record 的 Callback 回调。
3.  调用底层 CountDownLatch.countDown()方法，阻塞在其上的主线程。

至此我们已经讲解了 ProducerBatch 「**如何缓存消息**」、「**如何处理响应**」、「**如何处理回调**」三个最重要方法。

通过一张图来描述下缓存消息的存储结构：

![](https://article-images.zsxq.com/FlE5UeCXStBQtHNM4857fWCGhjmW)

接下来看看 Kafka 生产端最经典的 「**缓冲池架构**」。

## **03 客户端缓存池架构设计**

为什么客户端需要缓存池这个经典架构设计呢？

主要原因就是频繁的创建和释放 ProducerBatch 会导致 Full GC 问题，所以 Kafka 针对这个问题实现了一个非常优秀的机制，就是「**缓存池 BufferPool 机制**」。即每个 Batch 底层都对应一块内存空间，这个内存空间就是专门用来存放消息，用完归还就行。

接下来看看缓存池的源码设计。

## **03.1 BufferPool**

github 源码地址如下：

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/internals/BufferPool.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/internals/BufferPool.java)

public class BufferPool {

// 整个BufferPool总内存大小 默认32M

private final long totalMemory;

// 当前BufferPool管理的单个ByteBuffer大小，16k 

private final int poolableSize;

// 因为有多线程并发分配和回收ByteBuffer，用锁控制并发，保证线程安全。

private final ReentrantLock lock;

// 对应一个ArrayDeque<ByteBuffer> 队列，其中缓存了固定大小的 ByteBuffer 对象

private final Deque<ByteBuffer> free;

// 此队列记录因申请不到足够空间而阻塞的线程对应的Condition 对象

private final Deque<Condition> waiters;

// 非池化可用的内存即totalMemory减去free列表中的全部ByteBuffer的大小

private long nonPooledAvailableMemory;

// 构造函数

public BufferPool(long memory, int poolableSize, Metrics metrics, Time time, String metricGrpName) {

    ...

// 总的内存

this.totalMemory = memory;

// 默认的池外内存，就是总的内存

this.nonPooledAvailableMemory = memory;

  }

}

先来看看上面几个重要字段：

1.  **totalMemory**：整个 BufferPool 内存大小「**[buffer.memory](http://buffer.memory/)**」，默认是32M。
2.  **poolableSize**：池化缓存池一块内存块的大小「**[batch.size](http://batch.size/)**」，默认是16k。
3.  **lock**：当有多线程并发分配和回收 ByteBuffer 时，为了保证线程的安全，使用锁来控制并发。
4.  **free**：池化的 free 队列，其中缓存了指定大小的 ByteBuffer 对象。
5.  **waiters**：阻塞线程对应的 Condition 队列，当有申请不到足够内存的线程时，为了等待其他线程释放内存而阻塞等待，对应的 Condition 对象会进入该队列。
6.  **nonPooledAvailableMemory**：非池化可用内存。

可以看出它只会针对固定大小「**poolableSize 16k**」的 ByteBuffer 进行管理，ArrayDeque 的初始化大小是16，此时 BufferPool 的状态如下图：

![](https://article-images.zsxq.com/Fmpiznz0f97oSRLXsLzmnYnvo2Ze)

接下来看看 BufferPool 的重要方法。

### **03.1.1 allocate()**

// 分配指定空间的缓存，如果缓冲区中没有足够的空闲空间，那么会阻塞线程，直到超时或得到足够空间

public ByteBuffer allocate(int size, long maxTimeToBlockMs) throws InterruptedException {

// 1.判断申请的内存是否大于总内存

if (size > this.totalMemory)

throw new IllegalArgumentException("Attempt to allocate " + size + " bytes, but there is a hard limit of "+ this.totalMemory + " on memory allocations.");

// 初始化buffer

ByteBuffer buffer \= null;

// 2.加锁，保证线程安全。

this.lock.lock();

// 如果当前BufferPool处于关闭状态，则直接抛出异常

if (this.closed) {

this.lock.unlock();

throw new KafkaException("Producer closed while allocating memory");

  }

  ....

try {

// 3.申请内存大小恰好为16k 且free缓存池不为空

if (size == poolableSize && !this.free.isEmpty())

// 从free队列取出一个ByteBuffer

return this.free.pollFirst();

// 对于申请内存大小非16k情况

// 先计算free缓存池总空间大小，判断是否足够

int freeListSize \= freeSize() \* this.poolableSize;

// 4.当前BufferPool能够释放出申请内存大小的空间

if (this.nonPooledAvailableMemory + freeListSize >= size) {

// 5.如果size大于非池化可用内存大小，就循环从free缓存池里释放出来空闲Bytebuffer补充到nonPooledAvailableMemory中，直到满足size大小为止。

          freeUp(size);

// 释放非池化可用内存大小

this.nonPooledAvailableMemory -= size;

      } else {

// 如果当前BufferPool不够提供申请内存大小，则需要阻塞当前线程

// 累计已经释放的内存

int accumulated \= 0;

// 创建对应的Condition，阻塞自己等待别的线程释放内存

Condition moreMemory \= this.lock.newCondition();

try {

// 计算当前线程最大阻塞时长

long remainingTimeToBlockNs \= TimeUnit.MILLISECONDS.toNanos(maxTimeToBlockMs);

// 把自己添加到等待队列中末尾，保持公平性，先来的先获取内存，防止饥饿

this.waiters.addLast(moreMemory);

// 循环等待直到分配成功或超时

while (accumulated < size) {

                  ....

try {

// 当前线程阻塞等待，返回结果为false则表示阻塞超时

                   waitingTimeElapsed = !moreMemory.await(remainingTimeToBlockNs, TimeUnit.NANOSECONDS);

                  } finally {

                      ....

                  }

                  ....   

// 申请内存大小是16k，且free缓存池有了空闲的ByteBuffer

if (accumulated == 0 && size == this.poolableSize && !this.free.isEmpty()) {

// 从free队列取出一个ByteBuffer

                    buffer = this.free.pollFirst();

// 计算累加器

                    accumulated = size;

                  } else {

// 释放空间给非池化可用内存，并继续等待空闲空间，如果分配多了只取够size的空间

                      freeUp(size - accumulated);

int got \= (int) Math.min(size - accumulated, this.nonPooledAvailableMemory);

// 释放非池化可用内存大小

this.nonPooledAvailableMemory -= got;

// 累计分配了多少空间

                      accumulated += got;

                  }

              }

              accumulated = 0;

          } finally {

// 如果循环有异常，将已释放的空间归还给非池化可用内存

this.nonPooledAvailableMemory += accumulated;

//把自己从等待队列中移除并结束

this.waiters.remove(moreMemory);

          }

      }

  } finally {

// 当非池化可用内存有内存或free缓存池有空闲ByteBufer且等待队列里有线程正在等待

try {

if (!(this.nonPooledAvailableMemory == 0 && this.free.isEmpty()) && !this.waiters.isEmpty())

// 唤醒队列里正在等待的线程

this.waiters.peekFirst().signal();

      } finally {

// 解锁

          lock.unlock();

      }

  }

// 说明空间足够，并且有足够空闲的了。可以执行真正的分配空间了。

if (buffer == null)

// 没有正好的buffer，从缓冲区外(JVM Heap)中直接分配内存

return safeAllocateByteBuffer(size);

else

// 直接复用free缓存池的ByteBuffer

return buffer;

}

private ByteBuffer safeAllocateByteBuffer(int size) {

boolean error \= true;

try {

//分配空间

ByteBuffer buffer \= allocateByteBuffer(size);

      error = false;

//返回buffer

return buffer;

  } finally {

if (error) {

//分配失败了, 加锁，操作内存pool

this.lock.lock();

try {

//归还空间给非池化可用内存

this.nonPooledAvailableMemory += size;

if (!this.waiters.isEmpty())

//有其他在等待的线程的话，唤醒其他线程

this.waiters.peekFirst().signal();

        } finally {

// 加锁不忘解锁

this.lock.unlock();

        }

    }

  }

}

protected ByteBuffer allocateByteBuffer(int size) {

// 从JVM Heap中分配空间

return ByteBuffer.allocate(size);

}

// 不断从free队列中释放空闲的ByteBuffer来补充非池化可用内存

private void freeUp(int size) {

while (!this.free.isEmpty() && this.nonPooledAvailableMemory < size)

this.nonPooledAvailableMemory += this.free.pollLast().capacity();

}

该方法主要用来**尝试分配 ByteBuffer**，这里分4种情况说明下：

### **情况1：申请16k且free缓存池有可用内存**

此时会直接从 free 缓存池中获取队首的 ByteBuffer 分配使用，用完后直接将 ByteBuffer 放到 free 缓存池的队尾中，并**调用 clear() 清空数据**，以便下次重复使用。

![](https://article-images.zsxq.com/FvJwmDyyEPp8pvwCoJfR0yhXMv8E)

### **情况2：申请16k且free缓存池无可用内存**

此时 free 缓存池无可用内存，只能从**非池化可用内存中获取16k内存来分配**，用完后直接将 ByteBuffer 放到 free 缓存池的队尾中，并**调用 clear() 清空数据**，以便下次重复使用。

![](https://article-images.zsxq.com/Fml8Da_msa7PGx9bX732XnJ2yk61)

### **情况3：申请非16k且free缓存池无可用内存**

此时 free 缓存池无可用内存，且**申请的是非16k**，只能从**非池化可用内存(空间够分配)中获取一部分内存来分配**，用完后直接将申请到的内存空间释放到非池化可用内存中，后续会**被 GC 掉**。

![](https://article-images.zsxq.com/FrSiL90wEGtxxne1DS7846KMc2mf)

### **情况4：申请非16k且free缓存池有可用内存，但非池化可用内存不够**

此时 free 缓存池有可用内存，但**申请的是非16k**，先尝试从 **free 缓存池中将 ByteBuffer 释放到非池化可用内存中，直到满足申请内存大小(size)，然后从非池化可用内存获取对应内存大小来分配，用完后直接将申请到的内存空间释放到到非池化可用内存中，后续会被 GC 掉**。

![](https://article-images.zsxq.com/Fq83Anm5DA4Dkn5idsvduhfu5Dni)

### **03.1.2 deallocate()**

public void deallocate(ByteBuffer buffer, int size) {

// 1.加锁，保证线程安全。

    lock.lock();

try {

// 2.如果待释放的size大小为16k，则直接放入free队列中

if (size == this.poolableSize && size == buffer.capacity()) {

// 清空buffer

            buffer.clear();

// 释放buffer到free队列里

this.free.add(buffer);

        } else {

//如果非16k，则由JVM GC来回收ByteBuffer并增加非池化可用内存

this.nonPooledAvailableMemory += size;

        }

// 3.唤醒waiters中的第一个阻塞线程

Condition moreMem \= this.waiters.peekFirst();

if (moreMem != null)

            moreMem.signal();

    } finally {

// 释放锁

        lock.unlock();

    }

}  

该方法主要用来**尝试释放 ByteBuffer 空间**，主要做以下几件事情：

1.  先加锁，保证线程安全。
2.  如果待释放的 size 大小为16k，则直接放入 free 队列中。
3.  否则由 JVM GC 来回收 ByteBuffer 并增加 nonPooledAvailableMemory。
4.  当有 ByteBuffer 回收了，唤醒 waiters 中的第一个阻塞线程。

最后来看看 kafka 自定义的支持「**读写分离场景**」CopyOnWriteMap 的实现。

## **03.2 CopyOnWriteMap**

github 源码地址如下：

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/utils/CopyOnWriteMap.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/utils/CopyOnWriteMap.java)

通过 RecordAccumulator 类的属性字段中可以看到，**CopyOnWriteMap 中 key 为主题分区，value 为向这个分区发送的 Deque<ProducerBatch> 队列集合**。

我们知道生产消息时，**要发送的分区是很少变动的**，所以写操作会很少。大部分情况都是先获取分区对应的队列，然后将 ProducerBatch 放入队尾，所以读操作是很频繁的，这就是个典型的「**读多写少**」的场景。

所谓 「**CopyOnWrite**」 就是当写的时候会拷贝一份来进行写操作，写完了再替换原来的集合。

来看看它的源码实现。

public class CopyOnWriteMap<K, V> implements ConcurrentMap<K, V> {

// volatile Map

private volatile Map<K, V> map;

// 构造函数

public CopyOnWriteMap() {

this.map = Collections.emptyMap();

    } 

该类只有一个重要的字段 Map，是通过「**volatile**」来修饰的，目的就是**在多线程的场景下，当 Map 发生变化的时候其他的线程都是可见的**。

接下来看几个重要方法，都比较简单，但是**实现非常经典**。

### **03.2.1 get()**

// 获取集合中队列

public V get(Object k) {

return map.get(k);

}

该方法主要用来**读取集合中的队列**，可以看到读操作并没有加锁，**多线程并发读取的场景并不会阻塞，可以实现高并发读取**。如果队列已经存在了就直接返回即可。

### **03.2.2 putIfAbsent()**

public synchronized V putIfAbsent(K k, V v) {

if (!containsKey(k))

return put(k, v);

else

return get(k);

}

// 判断队列是否存在

public boolean containsKey(Object k) {

return map.containsKey(k);

}   

该方法主要用来**获取或者设置队列**，会被多个线程并发执行，通过「**synchronized**」来修饰可以**保证线程安全的**，除非队列不存在才会去设置。

### **03.2.3 put()**

public synchronized V put(K k, V v) {

    Map<K, V> copy = new HashMap<K, V>(this.map);

V prev \= copy.put(k, v);

this.map = Collections.unmodifiableMap(copy);

return prev;

}

该方法主要用来**设置队列的**， put 时也是通过「**synchronized**」来修饰的，可以保证同一时间只有一个线程会来更新这个值。

那为什么说**写操作不会阻塞读操作**呢？

1.  首先重新创建一个 HashMap 集合副本。
2.  通过「**volatile**」写的方式赋值给对应集合里。
3.  把新的集合设置成「**不可修改的 map**」,并赋值给字段 map。

**这就实现了读写分离**。对于 Producer 最最核心，会出现多线程并发访问的就是缓存池。因此这块的高并发设计相当重要。

## **04 总结**

这里，我们一起来总结一下这篇文章的重点。

1、带你先整体的梳理了 Kafka 客户端消息批量发送的好处。

2、通过一个真实生活场景类比来带你理解 RecordAccumulator 内部构造，并且深度剖析了消息是如何在客户端缓存的，以及内部各组件实现原理。

3、带你深度剖析了 Kafka 客户端非常重要的 BufferPool 、CopyOnWriteMap 的实现原理。

下篇我们来深度剖析「**Kafka 客户端 RecordAccumulator 实现原理**」，大家期待，我们下期见。