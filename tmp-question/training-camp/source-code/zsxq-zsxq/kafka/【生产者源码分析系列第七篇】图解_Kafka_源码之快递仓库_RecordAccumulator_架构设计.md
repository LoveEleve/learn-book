大家好，我是 华仔, 又跟大家见面了。

上篇主要带大家深度剖析了「**Kafka RecordAccmulator 内部三大组件的架构设计**」，今天主要聊聊 「**号称承载 Kafka 客户端消息快递仓库 RecordAccmulator 的架构设计**」，深度剖析下消息是如何被暂存累加的。

![](https://article-images.zsxq.com/FvpevB9CVfaT7JmM-fiWVKyTyaNW)

## **01 总体概述**

通过「**场景驱动**」的方式，来看看消息是如何在客户端被累加和待发送的

在上篇中，我们知道了消息并不是被**立即发送**到 Broker 端的，而是会在客户端进行**暂存累加**的，等满足一定条件后，Sender 子线程再把消息批量发送给 Kafka Broker 端。

接下来我们就来看看，Kafka 高吞吐的核心 「**RecordAccmulator 的源码实现以及消息累计过程**」**，**为了方便大家理解，所有的源码只保留骨干。

## **02** **RecordAccmulator 架构设计**

在 [图解 Kafka 源码实现机制之客户端缓存架构](https://articles.zsxq.com/id_9qqexkgvp93w.html) 这篇中我们得出 RecordAccumulator 类中有三个重要的组件：「**消息批次 ProducerBatch**」、「**自定义 CopyOnWriteMap**」、「**缓存池 BufferPool 机制**」，如下图所示：

  
![](https://article-images.zsxq.com/FnYieCOY-Ci_78lZvR6pWGADdyMz)

我们来看看 **RecordAccmulator 是如何实现累加消息的**。

github 源码地址如下：

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/internals/RecordAccumulator.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/internals/RecordAccumulator.java)

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/KafkaProducer.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/KafkaProducer.java)

## **02.1 关键字段**

public final class RecordAccumulator {

private final Logger log;

// 创建的时候设置为false

private volatile boolean closed;

private final AtomicInteger flushesInProgress;

private final AtomicInteger appendsInProgress;

// 消息批次大小 即 ProducerBatch 最大字节数

private final int batchSize;

// 压缩的类型

private final CompressionType compression;

// 延迟发送，如果batch没满，则消息被累计多久时间才能被触发发送

private final int lingerMs;

// 重试退避时间

private final long retryBackoffMs;

private final int deliveryTimeoutMs;

// 缓存池 free 队列, 用来分配 ByteBuffer 的 BufferPool

private final BufferPool free;

private final Time time;

private final ApiVersions apiVersions;

// 批次消息

private final ConcurrentMap<TopicPartition, Deque<ProducerBatch>> batches;

// 未发送的 ProducerBatch 集合

private final IncompleteBatches incomplete;

// The following variables are only accessed by the sender thread, so we don't need to protect them.

private final Set<TopicPartition> muted;

// 上次发送停止的位置，下次继续从这个位置开始发送

private int drainIndex;

// 事务管理器

private final TransactionManager transactionManager;

private long nextBatchExpiryTimeMs \= Long.MAX\_VALUE; // the earliest time (absolute) a batch will expire.

......

从该类属性字段来看比较多，这里说几个关键字段：

1.  **close**：生产者是否关闭，创建的时候设置为false， 值为 true 标识生产者已经关闭了，通知正在工作的线程停止暂存消息，用 volatile来修饰 支持多线程。
2.  **batchSize**：具体可以看上一篇，每个 ProducerBatch 底层 ByteBuffer 大小。
3.  **flushesInProgress**：记录要求立即发送的线程数，调用一次加1。
4.  **appendsInProgress**：记录往 RecordAccumulator 发送消息的线程数。
5.  **batches**：具体可以看上一篇，这是「**真正消息暂存的集合**」。实例化类型是 CopyOnWriteMap<TopicPartition,Deque<ProducerBatch>> 。
6.  **free**：具体可以看上一篇，池化的 free 队列，其中缓存了指定大小的 ByteBuffer 对象。
7.  **ConcurrentMap** ：具体可以看上一篇，TopicPartition 与 RecordBatch 集合的映射关系，类型时 CopyOnWriteMap，是一个线程安全的集合，但其中的 Deque 是 ArrayDeque 类型，是非线程安全的集合，其中 Key 记录了目标 Partition， Value 记录了发送目标 Partition 的 ProducerBatch 集合。
8.  **incomplete**：底层是 Set 集合类型，当创建 ProducerBatch 时，会加到 Set 集合里，只有真正发送完成才会把 ProducerBatch 从 incomplete 集合中删除。
9.  **drainIndex**：Sender 线程在使用 RecordAccumulator.drain() 方法批量导出 ProducerBatch 时，会使用该字段记录上次发送停止的位置，下次继续从这个位置开始发送。
10.  **nextBatchExpireTimeMs**：下一次 ProducerBatch 的超时时间，如果很久都没有被 Send 线程发送就会超时销毁。

接下来我们看下其初始化方法。

## **02.2 初始化**

public final class RecordAccumulator {

public RecordAccumulator(LogContext logContext,

int batchSize,

CompressionType compression,

int lingerMs,

long retryBackoffMs,

int deliveryTimeoutMs,

Metrics metrics,

String metricGrpName,

Time time,

ApiVersions apiVersions,

TransactionManager transactionManager,

BufferPool bufferPool) {

this.log = logContext.logger(RecordAccumulator.class);

this.drainIndex = 0;

this.closed = false;

this.flushesInProgress = new AtomicInteger(0);

this.appendsInProgress = new AtomicInteger(0);

this.batchSize = batchSize;

this.compression = compression;

this.lingerMs = lingerMs;

this.retryBackoffMs = retryBackoffMs;

this.deliveryTimeoutMs = deliveryTimeoutMs;

this.batches = new CopyOnWriteMap<>();

this.free = bufferPool;

this.incomplete = new IncompleteBatches();

this.muted = new HashSet<>();

this.time = time;

this.apiVersions = apiVersions;

this.transactionManager = transactionManager;

registerMetrics(metrics, metricGrpName);

}

...

}

//而 RecordAccumulator 的初始化是在 KafkaProducer 类初始化的时候触发的。

public class KafkaProducer<K, V> implements Producer<K, V> {

KafkaProducer(Map<String, Object> configs,

Serializer<K> keySerializer,

Serializer<V> valueSerializer,

ProducerMetadata metadata,

KafkaClient kafkaClient,

ProducerInterceptors<K, V> interceptors,

Time time) {

...

this.accumulator = new RecordAccumulator(logContext,

config.getInt(ProducerConfig.BATCH\_SIZE\_CONFIG),

this.compressionType,

lingerMs(config),

retryBackoffMs,

deliveryTimeoutMs,

metrics,

PRODUCER\_METRIC\_GROUP\_NAME,

time,

apiVersions,

transactionManager,

new BufferPool(this.totalMemorySize, config.getInt(ProducerConfig.BATCH\_SIZE\_CONFIG), metrics, time,PRODUCER\_METRIC\_GROUP\_NAME));

}

...

}

通过上面代码可以看出 RecordAccumulator 的初始化是在 KafkaProducer 的构造方法中完成的。

这里有几个关键的字段如下：

1.  **compression**：消息压缩方式，有 none、gzip、snappy、lz4、zstd 4种方式。**默认不进行压缩，如果你的主题占用磁盘空间比较多的话，可以考虑启用压缩，节省资源**。
2.  **retryBackOffMs**：重试退避时间。
3.  **free**：具体可以看上一篇，池化的 free 队列，BufferPool 类型，其中缓存了指定大小的 ByteBuffer 对象，「**目的就是实现内存的高效利用**」。因为 ByteBuffer 的创建和销毁都是比较消耗资源的，Kafka底层提供了 BufferPool 实现 ByteBuffer 的复用和管理。
4.  **lingerMs**：延迟发送，如果batch没满，则消息被累计多久时间才能被触发发送。

接下来我们看看该类的关键方法。

## **02.3 关键方法**

这里我们主要看3个主要方法，**剩下的自行学习，如果有问题可以评论区讨论**。

我们知道 Kafka Producer 发送消息主要有2个线程：

1.  **KafkaProducer 主线程**：加载元数据、发送消息等。
2.  **Sender 子线程**：拉取更新元数据、向对应节点发送消息等，这块会在下一篇中深度剖析讲解，其线程名称：kafka-producer-network-thread + "|" + clientId。 在 KafkaProducer.java 类有常量定义：NETWORK\_THREAD\_PREFIX，并启动**守护线程 KafkaThread**。

这里先看下主线程中使用的方法：

### **02.3.1 append()**

先看下追加消息的方法，该方法代码很长，这里先给出一个完整流程图，帮你更好的理解。

![](https://article-images.zsxq.com/FsQDGTIpFMEMxZqQt-fgw62mbx-X)

接下来，我们看看其代码实现。

public RecordAppendResult append(TopicPartition tp, // 要发送的 Topic 分区

long timestamp, // 发送时的时间戳

byte\[\] key, // 消息的key

byte\[\] value, // 消息的value

Header\[\] headers, // 消息头

Callback callback, // 回调方法

long maxTimeToBlock,

boolean abortOnNewBatch, // 终止新的消息批次

long nowMs) throws InterruptedException { // 当前发送时间

// 统计正在向 RecordAccumulator 中写入的 Record 数量

appendsInProgress.incrementAndGet();

ByteBuffer buffer \= null;

if (headers == null) headers = Record.EMPTY\_HEADERS;

try {

// check if we have an in-progress batch

// 从 batches 集合中查找目标分区对应的 ArrayDeque<ProducerBatch> 集合，

// 如果查找失败，则创建新的 ArrayDeque<ProducerBatch>，并添加到 batches 集合中。

Deque<ProducerBatch> dq = getOrCreateDeque(tp);

// 第一次对 ArrayDeque<ProducerBatch> 加锁，相同的 dq 都会竞争这个锁

synchronized (dq) {

// 判断生产者是否已经关闭了

if (closed)

throw new KafkaException("Producer closed while send in progress");

// 尝试往 batches 里面追加消息

RecordAppendResult appendResult \= tryAppend(timestamp, key, value, headers, callback, dq, nowMs);

// 如果 Deque<ProducerBatch> 最后一个 ProducerBatch 空间够用, 一般都会追加成功，返回 RecordAppendResult

if (appendResult != null)

return appendResult;

}

// we don't have an in-progress record batch try to allocate a new batch

// 如果追加 Record 失败，可能因为当前使用的 ProducerBatch 已经被填满了，会根据abortOnNewBatch 参数决定是否立即返回 RecordAppendResult 结果，返回的 RecordAppendResult 中如果abortForNewBatch 为 true，会再触发一次 append()方法

if (abortOnNewBatch) {

// Return a result that will cause another call to append.

return new RecordAppendResult(null, false, false, true);

}

....

byte maxUsableMagic \= apiVersions.maxUsableProduceMagic();

// 预估size大小，重新分配ProducerBatch

int size \= Math.max(this.batchSize, AbstractRecords.estimateSizeInBytesUpperBound(maxUsableMagic, compression, key, value, headers));

// 从 BufferPool 中分配 ByteBuffer

buffer = free.allocate(size, maxTimeToBlock);

// Update the current time in case the buffer allocation blocked above.

nowMs = time.milliseconds();

// 再次对 ArrayDeque<ProducerBatch> 加锁

synchronized (dq) {

// Need to check if producer is closed again after grabbing the dequeue lock.

if (closed)

throw new KafkaException("Producer closed while send in progress");

// 再次尝试 tryAppend() 方法追加消息

RecordAppendResult appendResult \= tryAppend(timestamp, key, value, headers, callback, dq, nowMs);

if (appendResult != null) {

// Somebody else found us a batch, return the one we waited for! Hopefully this doesn't happen often...

return appendResult;

}

// 将 ByteBuffer 封装成 MemoryRecordsBuilder

MemoryRecordsBuilder recordsBuilder \= recordsBuilder(buffer, maxUsableMagic);

// 使用 BufferPool 新申请的 ByteBuffer 构建 ProducerBatch 对象

ProducerBatch batch \= new ProducerBatch(tp, recordsBuilder, nowMs);

// 通过 tryAppend() 方法将 Record 追加到 ProducerBatch 中

FutureRecordMetadata future \= Objects.requireNonNull(batch.tryAppend(timestamp, key, value, headers,

callback, nowMs));

// 将 ProducerBatch 追加到 ArrayDeque<ProducerBatch> 尾部

dq.addLast(batch);

// 将 ProducerBatch 添加到 IncompleteBatches 中, 待发送完后会从该集合删除。

incomplete.add(batch);

// Don't deallocate this buffer in the finally block as it's being used in the record batch

buffer = null; // 清空 buffer

return new RecordAppendResult(future, dq.size() > 1 || batch.isFull(), true, false);

}

} finally {

// 如果 buffer 不为空，则表示写入过程中出现异常，这里会释放 ByteBuffer

if (buffer != null)

// 释放空间，如果不理解 可以看【生产者源码分析系列第七篇】图解 Kafka 源码实现机制之客户端缓存架构的内容

free.deallocate(buffer);

// 表示当前 Record 已经写入完成，递减 appendsInProgress

appendsInProgress.decrementAndGet();

}

}

/\*\*

\* Get the deque for the given topic-partition, creating it if necessary.

\*/

private Deque<ProducerBatch> getOrCreateDeque(TopicPartition tp) {

Deque<ProducerBatch> d = this.batches.get(tp);

if (d != null)

return d;

d = new ArrayDeque<>();

Deque<ProducerBatch> previous = this.batches.putIfAbsent(tp, d);

if (previous == null)

return d;

else

return previous;

}

/\*\*

\* Try to append to a ProducerBatch. 尝试追加消息

\*/

private RecordAppendResult tryAppend(long timestamp, byte\[\] key, byte\[\] value, Header\[\] headers,

Callback callback, Deque<ProducerBatch> deque, long nowMs) {

// 获取 ArrayDeque<ProducerBatch> 集合中最后一个 ProducerBatch 对象

ProducerBatch last \= deque.peekLast();

if (last != null) {

// 尝试将消息写入 last 这个 ProducerBatch 对象

FutureRecordMetadata future \= last.tryAppend(timestamp, key, value, headers, callback, nowMs);

if (future == null)

// 写入失败，则关闭 last 指向的 ProducerBatch 对象，同时返回 null 表示写入失败

last.closeForRecordAppends();

else

// 写入成功，则返回 RecordAppendResult 对象

return new RecordAppendResult(future, deque.size() > 1 || last.isFull(), false, false);

}

return null;

}

/\*

\* Metadata about a record just appended to the record accumulator

\*/

public final static class RecordAppendResult {

public final FutureRecordMetadata future;

public final boolean batchIsFull;

public final boolean newBatchCreated;

public final boolean abortForNewBatch;

public RecordAppendResult(FutureRecordMetadata future, boolean batchIsFull, boolean newBatchCreated, boolean abortForNewBatch) {

this.future = future;

this.batchIsFull = batchIsFull;

this.newBatchCreated = newBatchCreated;

// 如果abortForNewBatch为 true，会再触发一次append()方法

this.abortForNewBatch = abortForNewBatch;

}

}

代码确实比较长，我们这里分两部分来看下：

**先来看下方法的重点参数**

public RecordAppendResult append( TopicPartition tp, // 要发送的 Topic 分区

long timestamp, // 发送时的时间戳

byte\[\] key, // 消息的key

byte\[\] value, // 消息的value

Header\[\] headers, // 消息头

Callback callback, // 回调方法

long maxTimeToBlock,

boolean abortOnNewBatch, // 终止新的消息批次

long nowMs // 当前发送时间

) throws InterruptedException {

1.  **tp** : 要发送的 Topic 分区。
2.  **timestamp** ：发送时的时间戳。
3.  **key** : 消息的key。
4.  **value** ：消息的value。
5.  **callback** ：回调函数。
6.  **abortOnNewBatch**：终止新的消息批次
7.  **nowMs** ：当前发送时间。

然后再分两步看下方法里面的实现细节：

如果当前 tp 对应的 ProducerBatch 在 RecordAccumulator 中还没有发送出去，那么则尝试将 ProducerRecord 追加到这个 ProducerBatch 中。

// 1. 从 batches 集合中查找目标分区对应的 ArrayDeque<ProducerBatch> 集合，如果查找失败，则创建新的 ArrayDeque<ProducerBatch>，并添加到 batches 集合中。

Deque<ProducerBatch> dq = getOrCreateDeque(tp);

// 2. 第一次对 ArrayDeque<ProducerBatch> 加锁，相同的 dq 都会竞争这个锁

synchronized (dq) {

// 判断生产者是否已经关闭了

if (closed)

throw new KafkaException("Producer closed while send in progress");

// 3. 尝试往 batches 里面追加消息，返回 abortOnNewBatch = false

RecordAppendResult appendResult \= tryAppend(timestamp, key, value, headers, callback, dq, nowMs);

// 4. 如果 Deque<ProducerBatch> 最后一个 ProducerBatch 空间够用, 一般都会追加成功，返回 RecordAppendResult

if (appendResult != null)

return appendResult;

}

这一部分是**第一次往 batches 里追加消息，**分为 4 步：

1.  从 batches 集合中查找目标分区对应的 ArrayDeque<ProducerBatch> 集合，如果查找失败，则创建新的 ArrayDeque<ProducerBatch>，并添加到 batches 集合中。
2.  「**第一次对 ArrayDeque<ProducerBatch> 加锁**」，相同的 dq 都会竞争这个锁，不同 dq 不会受影响，**这里把锁粒度变小有利于提高线程的并发度。**
3.  调用 tryAppend 尝试往 batches 里面追加消息，这里分2种情况：
4.  如果当前 Deque<ProducerBatch> 最后一个 ProducerBatch 没有满，且空间能满足当前消息体大小的存储, 则追加成功。
5.  如果当前 ProducerBatch 已满，则需要创建一个新的 ProducerBatch。
6.  返回 RecordAppendResult。

通过上面 4 个步骤可以得出一个结论：**就是通过找到 tp 对应的 dq，然后对 dq 加锁防止别的线程并发操作影响，最后调用 tryAppend() 尝试追加消息**。

如果上述操作失败了，则可能时因为当前 **tp 对应的 ProducerBatch** 已经被填满了，接下来会判断 **abortOnNewBatch** 是否为 true，如果为 true，则立即返回 **RecordAppendResult**，上层 KafkaProducer.doSend() 方法中会再次尝试 append()。

接下来我们看下第二部分的代码，**它会对 batches 空间进行检测，不够就要创建 ByteBuffer，** 代码如下：

// 在第一部分调用 tryAppend 方法最后返回 abortOnNewBatch 为 false，不需要创建新的批次

// 5. 如果第一次追加 Record 失败，可能因为当前使用的 ProducerBatch 已经被填满了，会根据abortOnNewBatch 参数决定是否立即返回 RecordAppendResult 结果，返回的 RecordAppendResult 中如果abortForNewBatch 为 true，会再触发一次 append()方法

if (abortOnNewBatch) {

// Return a result that will cause another call to append.

return new RecordAppendResult(null, false, false, true);

}

....

byte maxUsableMagic \= apiVersions.maxUsableProduceMagic();

// 预估size大小，重新分配ProducerBatch

int size \= Math.max(this.batchSize, AbstractRecords.estimateSizeInBytesUpperBound(maxUsableMagic, compression, key, value, headers));

// 6. Deque<ProducerBatch> 最后一个 ProducerBatch 不够用时, 从 BufferPool 中分配 ByteBuffer

buffer = free.allocate(size, maxTimeToBlock);

// Update the current time in case the buffer allocation blocked above.

nowMs = time.milliseconds();

// 7. 第二次对 ArrayDeque<ProducerBatch> 加锁

synchronized (dq) {

// Need to check if producer is closed again after grabbing the dequeue lock.

if (closed)

throw new KafkaException("Producer closed while send in progress");

// 8. 第二次尝试 tryAppend() 方法追加消息

RecordAppendResult appendResult \= tryAppend(timestamp, key, value, headers, callback, dq, nowMs);

if (appendResult != null) {

// Somebody else found us a batch, return the one we waited for! Hopefully this doesn't happen often...

return appendResult;

}

// 9. 将 ByteBuffer 封装成 MemoryRecordsBuilder

MemoryRecordsBuilder recordsBuilder \= recordsBuilder(buffer, maxUsableMagic);

// 10. 如果还没有空间就使用 BufferPool 新申请的 ByteBuffer 构建 ProducerBatch 对象

ProducerBatch batch \= new ProducerBatch(tp, recordsBuilder, nowMs);

// 11. 通过 tryAppend() 方法将 Record 追加到 ProducerBatch 中

FutureRecordMetadata future \= Objects.requireNonNull(batch.tryAppend(timestamp, key, value, headers,

callback, nowMs));

// 12. 将 ProducerBatch 追加到 ArrayDeque<ProducerBatch> 尾部

dq.addLast(batch);

// 13. 将 ProducerBatch 添加到 IncompleteBatches 中, 待发送完后会从该集合删除。

incomplete.add(batch);

// Don't deallocate this buffer in the finally block as it's being used in the record batch

// 14. 清空 buffer

buffer = null;

// 15. 返回 RecordAppendResult

return new RecordAppendResult(future, dq.size() > 1 || batch.isFull(), true, false);

}

这一部分主要是**当 batches 空间不够需要增加新空间的时候，如何去申请新的空间，**分为10 步。

1.  在第一部分调用 tryAppend 方法最后返回 「**abortOnNewBatch = false**」，不需要创建新的批次，就会返回给上层调用方「**KafkaProducer.doSend()**」方法，会触发第二次调用。
2.  如果 abortForNewBatch 参数不为 true， 且 Deque<ProducerBatch> 最后一个 ProducerBatch 不够用时, 从 BufferPool 中分配 ByteBuffer，并封装承新的 ProducerBatch 对象。**这个操作比较耗时，这里没有锁**。
3.  「**第二次对 ArrayDeque<ProducerBatch> 加锁**」。
4.  「**第二次尝试调用 tryAppend() 方法追加消息**」。
5.  将 ByteBuffer 封装成 MemoryRecordsBuilder。
6.  如果还没有空间就使用 BufferPool 新申请的 ByteBuffer 构建新的 ProducerBatch 对象。
7.  将新的 ProducerBatch 追加到 「**ArrayDeque<ProducerBatch>**」尾部。
8.  将新的 ProducerBatch 添加到 「**IncompleteBatches**」中, 待发送完后会从该集合删除。
9.  清空 buffer。
10.  返回 RecordAppendResult， RecordAppendResult 返回值中 「**batchIsFull**」字段和 「**newBatchCreated**」字段会作为「**唤醒 Sender 线程**」的条件。

if (result.batchIsFull || result.newBatchCreated) {

// 当此次写入填满一个 ProducerBatch 或有新 ProducerBatch 创建的时候，会唤醒 Sender 线程来进行发送

this.sender.wakeup();

}

这里有2个地方需要**重点注意**下。

**1)、dq 本身是非线程安全的集合，加锁可以理解，为什么要分段加锁，而不是整个代码加一次 dq 锁？**

主要原因是因为在向 BufferPool 申请新 ByteBuffer 的时候，可能会导致阻塞。

这里假设只在一个 synchronized 块中完成上面所有追加操作，线程A发送的消息比较大，需要向 BufferPool 申请新的空间。

**假如此时 BufferPool 空间不足**，线程 A 就会阻塞在 BufferPool 上等待，此时它依然持有对应 Deque 的锁，直到分配完毕。

线程 B 发送的消息较小，与此同时 dq 最后一个 ProducerBatch 剩余空间正好足够写入该消息，但是由于线程 A 还未释放 Deque 锁，所以也需要一起等待，**这就造成线程 B 不必要阻塞，降低了吞吐量**。

因此这块本质就是**通过减少锁的持有时间**进行的优化。

**2)、 为什么第二部分在创建新的 ProducerBatch 之前又调用一次 tryAppend() 操作 ?**

当 Deque 最后一个 ProducerBatch 空间不足时，会申请新的 ProducerBatch。这里再一次调用 tryAppend() 操作主要是**为了防止多个线程并发从 BufferPool 申请空间后，造成内部碎片**。

![](https://article-images.zsxq.com/FrcVvcZoXp-znEUPIKThLTYAXk2N)

在上图这种场景下，线程 A 发现最后一个 ProducerBatch 2 空间不足，然后申请空间并创建一个新 ProducerBatch 3 添加到 ArrayDeque 的尾部，然后线程 B 与线程 A 并发执行，也将新创建一个 ProducerBatch 4 添加到 ArrayDeque 尾部。

从 tryAppend() 方法的逻辑中我们可以看到，**后续写入只会追加到 ArrayDeque 尾部的 ProducerBatch 上**，这样就会导致上图中的 **ProducerBatch 3 不再被写入**，从而出现内部碎片，浪费了内存空间。

另外细心的朋友们可能注意到了代码通过判断 **abortOnNewBatch 为 true，会触发上层二次调用 append(),** 需要注意的是，再次尝试时**会更换一个 Partition**。

// KafkaProducer.java

private Future<RecordMetadata> doSend(ProducerRecord<K, V> record, Callback callback) {

TopicPartition tp \= null;

try {

...

int partition \= partition(record, serializedKey, serializedValue, cluster);

tp = new TopicPartition(record.topic(), partition);

// 第一次调用 append()

RecordAccumulator.RecordAppendResult result \= accumulator.append(tp, timestamp, serializedKey,

serializedValue, headers, interceptCallback, remainingWaitMs, true, nowMs);

// 当 abortForNewBatch 为 true 会触发二次调用

if (result.abortForNewBatch) {

int prevPartition \= partition;

// 更换一个 partition

partitioner.onNewBatch(record.topic(), cluster, prevPartition);

partition = partition(record, serializedKey, serializedValue, cluster);

tp = new TopicPartition(record.topic(), partition);

....

// 再次调用 append() 追加消息 此时 abortForNewBatch 为 false

result = accumulator.append(tp, timestamp, serializedKey,

serializedValue, headers, interceptCallback, remainingWaitMs, false, nowMs);

}

....

return result.future;

}

}

这里总结一下这几次 **tryAppend()** 调用过程时序图：

![](https://article-images.zsxq.com/FqYu4uXmjQddsf8oXb7Nq62L_Zzn)

### **02.3.2 ready()**

public ReadyCheckResult ready(Cluster cluster, long nowMs) {

// 1. 用来记录可以向哪些 Broker Node 节点发送数据

Set<Node> readyNodes = new HashSet<>();

// 记录下次需要调用 ready() 方法的时间间隔

long nextReadyCheckDelayMs \= Long.MAX\_VALUE;

// 记录 Cluster 元数据中找不到 Leader 的 topic

Set<String> unknownLeaderTopics = new HashSet<>();

// 是否有线程在阻塞等待 BufferPool 来释放空间

boolean exhausted \= this.free.queued() > 0;

// 2. 遍历 batches 集合，对其中每个分区的 Leader 所在的 Broker Node 都进行判断

for (Map.Entry<TopicPartition, Deque<ProducerBatch>> entry : this.batches.entrySet()) {

Deque<ProducerBatch> deque = entry.getValue();

// 对 ArrayDeque<ProducerBatch> 加锁

synchronized (deque) {

// 3. 获取 ArrayDeque 中第一个 ProducerBatch 对象，判断是否为空

ProducerBatch batch \= deque.peekFirst();

if (batch != null) {

TopicPartition part \= entry.getKey();

// 4. 查找目标分区的 Leader 所在的节点

Node leader \= cluster.leaderFor(part);

// Leader 找不到，会认为是异常情况，不能发送消息

if (leader == null) {

// This is a partition for which leader is not known, but messages are available to send.

// Note that entries are currently not removed from batches when deque is empty.

unknownLeaderTopics.add(part.topic());

} else if (!readyNodes.contains(leader) && !isMuted(part)) {

long waitedTimeMs \= batch.waitedTimeMs(nowMs);

boolean backingOff \= batch.attempts() > 0 && waitedTimeMs < retryBackoffMs;

// retryBackoffMs:重试阻塞时间（默认：100）

// lingerMs 发送延迟时间

long timeToWaitMs \= backingOff ? retryBackoffMs : lingerMs;

// deque size大于 1 或第一个 batch 是否满了

boolean full \= deque.size() > 1 || batch.isFull();

// 消息在暂存队列里是否超时了

boolean expired \= waitedTimeMs >= timeToWaitMs;

// 5. 通过检查上述五个条件，判断是否可以发送，找到此次能发送的 Node

boolean sendable \= full || expired || exhausted || closed || flushInProgress();

if (sendable && !backingOff) {

// 6. 如果是能发送就将 Leader 加入 readyNodes 集合。

readyNodes.add(leader);

} else {

long timeLeftMs \= Math.max(timeToWaitMs - waitedTimeMs, 0);

// Note that this results in a conservative estimate since an un-sendable partition may have

// a leader that will later be found to have sendable data. However, this is good enough

// since we'll just wake up and then sleep again for the remaining time.

// 记录下次需要调用 ready() 方法检查的时间间隔

nextReadyCheckDelayMs = Math.min(timeLeftMs, nextReadyCheckDelayMs);

}

}

}

}

}

return new ReadyCheckResult(readyNodes, nextReadyCheckDelayMs, unknownLeaderTopics);

}

/\*

\* The set of nodes that have at least one complete record batch in the accumulator

\*/

public final static class ReadyCheckResult {

public final Set<Node> readyNodes;

public final long nextReadyCheckDelayMs;

public final Set<String> unknownLeaderTopics;

// 主要返回 准备好发送的节点集合、找不到 Leader 分区的主题

public ReadyCheckResult(Set<Node> readyNodes, long nextReadyCheckDelayMs, Set<String> unknownLeaderTopics) {

// 准备好发送的节点集合

this.readyNodes = readyNodes;

this.nextReadyCheckDelayMs = nextReadyCheckDelayMs;

// 找不到 Leader 分区的主题

this.unknownLeaderTopics = unknownLeaderTopics;

}

}

该方法主要用来**获取能发送消息的节点集合，**Sender 线程只会关心哪些节点可以进行发送消息，主要做了以下**6件事情**:

1.  首先记录可以向哪些 Broker Node 节点发送数据。
2.  遍历 batches 集合，对其中每个分区的 Leader 所在的 Broker Node 都进行判断。
3.  获取 ArrayDeque 中第一个 ProducerBatch 对象，判断是否为空，**如果为空就没必要发送了，因为这个分区没有要发送的消息**。
4.  查找目标分区的 Leader 所在的节点，如果 Leader 所在的节点不存在就没发送的必要，会放到 unknownLeaderTopics 集合里，**让 Sender 线程去触发更新元数据的请求**。
5.  通过检查五个条件，判断是否可以发送，找到此次能发送的 Node。

> 1)、full : deque size 是否大于 1，或 deque 的第一个 ProducerBatch 是否满了。
> 
> 2)、expired ： ProducerBatch 在 deque 里是否超时。
> 
> 3)、exhausted ：是否有线程在阻塞等待 BufferPool 来释放空间。
> 
> 4)、closed ：生产者是否准备正常关闭了。
> 
> 5)、flushInProgress ： 是否有 flush 操作，把暂存消息要立即发送的标记。

1.  如果是能发送就将 Leader 加入 readyNodes 集合，否则记录下次需要调用 ready() 方法检查的时间间隔。

通过以上 6 个步骤，Sender 线程就知道了可以往**哪些 Node** 发送消息了，但是还要知道往**对应 Node 发送哪些消息**，这就是下面 drain() 方法来实现的，我们来看下。

### **02.3.3 drain()**

我们知道在 「**KafkaProducer**」 的上层业务逻辑中，是按照 「**TopicPartition**」方式产生数据的，它只关心可以发送到哪个 「**TopicPartition**」，并不关心这些 「**TopicPartition**」在哪个「**Node 节点**」。

而在网络 I/O 层面的话，生产者是向 「**Node 节点**」发送消息数据，它只建立到「**Node 节点的连接**」并发送对应消息数据，并不关心这些数据是属于哪个 「**TopicPartition**」，因此需要一个方法来做转换，即 drain() 方法的核心功能，就是将 「**TopicPartition**」-> 「**ProducerBatch 集合**」的映射关系转换成 「**Node 节点**」->「**ProducerBatch 集合**」的映射关系，如下图所示：

  
![](https://article-images.zsxq.com/FkrA8D87tUbVOC-vY-kput7Z5GQD)

假如我们有2台机器，主题有5个分区，这样1台机器会分配3个分区，另外一台会分配2个分区，如果按照**分区方式**去发送的话需要**发送5次请求才能完成，**但如果**按照节点方式**去发送的话只需要2次请求就完成。因此**按照节点方式可以大大减少网络的开销，这也是要进行转换的原因**。

因此，当 Sender 子线程会调用 drain() 方法会根据 「**Node 节点集合**」获取要发送的 「**ProducerBatch 集合**」，返回 Map<Integer, List<ProducerBatch>> 集合，其中的 Key 是「**目标 Node 节点Id**」，Value 是此次待发送的 「**ProducerBatch 集合**」。

public Map<Integer, List<ProducerBatch>> drain(Cluster cluster, Set<Node> nodes, int maxSize, long now) {

if (nodes.isEmpty())

return Collections.emptyMap();

// 转换后的结果，Key是目标 Node 的 Id，Value是发送到目标 Node 的 ProducerBatch 集合

Map<Integer, List<ProducerBatch>> batches = new HashMap<>();

for (Node node : nodes) {

// 获取目标 Node 的 ProducerBatch 集合

List<ProducerBatch> ready = drainBatchesForOneNode(cluster, node, maxSize, now);

// 添加到 batches 中

batches.put(node.id(), ready);

}

return batches;

}

// 获取目标 Node 的 ProducerBatch 集合

private List<ProducerBatch> drainBatchesForOneNode(Cluster cluster, Node node, int maxSize, long now) {

int size \= 0;

// 1. 获取当前 Node 上的所有分区集合

List<PartitionInfo> parts = cluster.partitionsForNode(node.id());

// 2. 记录发往目标 Node 的 ProducerBatch 集合

List<ProducerBatch> ready = new ArrayList<>();

/\* to make starvation less likely this loop doesn't start at 0 \*/

// drainIndex 是 batches 的下标，记录上次发送停止时的位置，下次继续从此位置开始发送。如果始终从

// 索引0的队列开始发送，可能会出现一直只发送前几个分区的消息的情况，造成其他分区饥饿。

int start \= drainIndex = drainIndex % parts.size();

do {

// 3.获取partition的元数据

PartitionInfo part \= parts.get(drainIndex);

TopicPartition tp \= new TopicPartition(part.topic(), part.partition());

this.drainIndex = (this.drainIndex + 1) % parts.size();

// Only proceed if the partition has no in-flight batches.

if (isMuted(tp))

continue;

// 4.获取主题分区对应的Deque

Deque<ProducerBatch> deque = getDeque(tp);

if (deque == null)

continue;

synchronized (deque) {

// invariant: !isMuted(tp,now) && deque != null

// 获取ArrayDeque中第一个ProducerBatch对象

ProducerBatch first \= deque.peekFirst();

if (first == null)

continue;

// first != null

// 重试操作的话，需要检查是否已经等待了足够的退避时间

boolean backoff \= first.attempts() > 0 && first.waitedTimeMs(now) < retryBackoffMs;

// Only drain the batch if it is not during backoff period.

if (backoff)

continue;

if (size + first.estimatedSizeInBytes() > maxSize && !ready.isEmpty()) {

// 此次请求要发送的数据量已满，结束循环

break;

} else {

if (shouldStopDrainBatchesForPartition(first, tp))

break;

....

// 5. 获取ArrayDeque中第一个ProducerBatch

ProducerBatch batch \= deque.pollFirst();

if (producerIdAndEpoch != null && !batch.hasSequence()) {

... // 事务相关的处理

}

// 6. 关闭底层输出流，将ProducerBatch设置成只读状态

batch.close();

size += batch.records().sizeInBytes();

// 7. 将 ProducerBatch 记录到 ready 集合中

ready.add(batch);

// 8. 修改 ProducerBatch的drainedMs 标记

batch.drained(now);

}

}

} while (start != drainIndex);

return ready;

}

该方法是 **Sender 线程获取每个 Node 需要待发送消息的集合**，主要调用 drainBatchesForOneNode 方法获取目标 node 的待发送消息，主要做以下**8件事情**：

1.  获取当前 Node 上的所有分区集合。
2.  记录发往目标 Node 的 ProducerBatch 集合。
3.  获取分区 Partition 的元数据。
4.  获取主题分区对应的 Deque。
5.  从 ArrayDeque 队列中获取第一个 ProducerBatch。
6.  关闭底层输出流，将ProducerBatch设置成只读状态。
7.  将 ProducerBatch 记录到 ready 集合中。
8.  修改 ProducerBatch的drainedMs 标记。

drain 方法中先初始化 batches， 然后循环所有的节点获取待发送消息的集合，然后将其添加到 batches 集合中。

**这里有2个需要特别注意的地方**：

1.  通过 drainIndex 来避免 Sender 线程总是获取前面几个分区的消息来发送，而后面的分区无法被轮询到，从而造成饥饿现象出现。
2.  每个分区每次只会从 Deque 队列中获取第一个 ProducerBatch 放到 ready 集合里，这样上层 Sender 线程不用担心 Deque 消费不完的问题，因为会通过 while 循环一直调用这个方法，直到取完为止。

接下来讲3个关于**保证消息顺序性**的方法，它们是用来保证顺序性关键，其主要作用就是将指定的 topicPartition 从 muted 集合中加入和删除。

### **02.3.4 mutePartition ()**

// 对 tp 进行 mute， 保证只有一个 batch 正在发送，来保证顺序性。

public void mutePartition(TopicPartition tp) {

muted.add(tp);

}

如果要求消息保证顺序性，那么这个 tp 对应的 RecordBatch 如果要开始发送，就将这个 tp 加入到 muted 集合中，它会保证只有一个 batch 正在发送。

### **02.3.5 unmutePartition ()**

// 发送完成后进行 unmute, 这样 tp 才能进行下次发送

public void unmutePartition(TopicPartition tp) {

muted.remove(tp);

}

如果 这个 tp 对应的 RecordBatch 发送完成，则 将 tp 从 muted 集合中移除，这样 tp 才能进行下次发送。

### **02.3.6 isMuted ()**

// 判断 muted 集合中是否存在该主题分区

private boolean isMuted(TopicPartition tp) {

return muted.contains(tp);

}

**总结：这里的 muted 就是用来记录对应 tp 是否还有未完成的 RecordBatch。**

### **02.3.6 expiredBatches ()**

/\*\*

\* Get a list of batches which have been sitting in the accumulator too long and need to be expired.

\*/

public List<ProducerBatch> expiredBatches(long now) {

List<ProducerBatch> expiredBatches = new ArrayList<>();

//1. 获取每个分区对应的批次队列

for (Map.Entry<TopicPartition, Deque<ProducerBatch>> entry : this.batches.entrySet()) {

// expire the batches in the order of sending

Deque<ProducerBatch> deque = entry.getValue();

synchronized (deque) {

//2. 如果队列中存在批次对象

while (!deque.isEmpty()) {

// 3. 获取队列中头部的批次对象

// 为什么只取头部批次？因为是按时间顺序进入队列的，

// 如果头部批次没超时，后面的肯定也没有超时)

ProducerBatch batch \= deque.getFirst();

// 4. 判断批次是否超时了

if (batch.hasReachedDeliveryTimeout(deliveryTimeoutMs, now)) {

//如果超时把这个批次从队列中移除

deque.poll();

// 终止批次的写入 (假设批次还未写满)

batch.abortRecordAppends();

// 将这个批次放到过期集合中

expiredBatches.add(batch);

} else {

// 如果没有超时，则更新下个批次超时的具体时间

maybeUpdateNextBatchExpiryTime(batch);

break;

}

}

}

}

return expiredBatches;

}

在剖析完重要方法后，我们来将消息流程串联一下，这样会加深大家对这块源码的理解。

## **03 消息流程串联**

前两部分已经深度剖析了 「**RecordAccmulator 的源码实现以及消息累计过程**」，这一部分会将整个消息流转过程进行一个串联，让大家有个更好的整体认知。

下图展示了 「**调用一次 append() 将消息追加到 ProducerBatch**」的全过程。

![](https://article-images.zsxq.com/liI34NmDAUaLW1769IOibiShX9PO)

调用过程时序图如下：

  
![](https://article-images.zsxq.com/FurlrxxsiRdjDp5q7WV01pnfAHsd)

数据已经写入到对应的 batches 了，那么 Sender 线程是在「**什么时机**」什么时机被触发的呢？

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/KafkaProducer.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/KafkaProducer.java)

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/internals/ProducerBatch.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/producer/internals/ProducerBatch.java)

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/record/MemoryRecordsBuilder.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/record/MemoryRecordsBuilder.java)

/\*\*

\* KafkaProducer.java

\* Implementation of asynchronously send a record to a topic.

\*/

private Future<RecordMetadata> doSend(ProducerRecord<K, V> record, Callback callback) {

TopicPartition tp \= null;

try {

....省略

// 1.等待元数据更新即确认数据要发送到的 topic 的 metadata 是可用的

clusterAndWaitTime = waitOnMetadata(record.topic(), record.partition(), nowMs, maxBlockTimeMs);

....省略

// 2.序列化 record的key和value

byte\[\] serializedKey;

serializedKey = keySerializer.serialize(record.topic(), record.headers(), record.key());

byte\[\] serializedValue;

serializedValue = valueSerializer.serialize(record.topic(), record.headers(), record.value());

// 3.获取record消息对应的分区

int partition \= partition(record, serializedKey, serializedValue, cluster);

tp = new TopicPartition(record.topic(), partition);

....省略

// 4.验证消息的大小

ensureValidRecordSize(serializedSize);

// 5.组装回调方法和拦截器为一个对象

Callback interceptCallback \= new InterceptorCallback<>(callback, this.interceptors, tp);

....省略

// 6.向 accumulator 中追加数据

RecordAccumulator.RecordAppendResult result \= accumulator.append(tp, timestamp, serializedKey,

serializedValue, headers, interceptCallback, remainingWaitMs, true, nowMs);

// 7.新的批次需要重新进行分区

if (result.abortForNewBatch) {

int prevPartition \= partition;

partitioner.onNewBatch(record.topic(), cluster, prevPartition);

partition = partition(record, serializedKey, serializedValue, cluster);

tp = new TopicPartition(record.topic(), partition);

// producer callback will make sure to call both 'callback' and interceptor callback

interceptCallback = new InterceptorCallback<>(callback, this.interceptors, tp);

result = accumulator.append(tp, timestamp, serializedKey,

serializedValue, headers, interceptCallback, remainingWaitMs, false, nowMs);

}

// 8.如果 batch 已经满了， 则唤醒 sender 线程发送数据

if (result.batchIsFull || result.newBatchCreated) {

log.trace("Waking up the sender since topic {} partition {} is either full or getting a new batch", record.topic(), partition);

this.sender.wakeup();

}

return result.future;

} catch (ApiException e) {

....省略

return new FutureFailure(e);

} catch (InterruptedException e) {

....省略

throw new InterruptException(e);

} catch (KafkaException e) {

....省略

throw e;

} catch (Exception e) {

....省略

throw e;

}

}

/\*\*

\* ProducerBatch.java

\* 上面方法返回的result.batchIsFull 判断批次是否已满

\*/

public boolean isFull() {

return recordsBuilder.isFull();

}

/\*\*

\* 消息批次是否已满

\*/

public boolean isFull() {

// note that the write limit is respected only after the first record is added which ensures we can always

// create non-empty batches (this is used to disable batching when the producer's batch size is set to 0).

return appendStream \=\= CLOSED\_STREAM || (this.numRecords > 0 && this.writeLimit <= estimatedBytesWritten());

}

/\*\*

\* Get an estimate of the number of bytes written (based on the estimation factor hard-coded in {@link CompressionType}.

\* 计算写入的字节数

\* @return The estimated number of bytes written

\*/

private int estimatedBytesWritten() {

if (compressionType == CompressionType.NONE) {

return batchHeaderSizeInBytes + uncompressedRecordsSizeInBytes;

} else {

// estimate the written bytes to the underlying byte buffer based on uncompressed written bytes

return batchHeaderSizeInBytes + (int) (uncompressedRecordsSizeInBytes \* estimatedCompressionRatio \* COMPRESSION\_RATE\_ESTIMATION\_FACTOR);

}

}

从源码中**第八步**可以看出，当 batch 已经满了，或者创建了新的 batch， 则会触发唤醒 sender 线程发送数据。

## **04 总结**

这里，我们一起来总结一下这篇文章的重点。

1、开篇总述 Kafka 实现高吞吐的核心是「**客户端缓存**」和 「**批量发送**」，从而引出「**RecordAccmulator**」。

2、带你深度剖析了「**RecordAccmulator**」 的三个关键方法实现:「**追加消息 append()** 」、「**获取能发送消息的节点集合** **ready()** 」、「**获取每个 Node 需要待发送消息的集合 drain()** 」。

3、最后带你串联了整个消息写入和发送的流程，让你有个更好的整体认知。