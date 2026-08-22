# Kafka-15. Producer 的 send() 为什么不是真的“发出去”——RecordAccumulator、BufferPool 与 ProducerBatch 主链

> 场景：Kafka-2 已经讲过 `KafkaProducer.send()` 的异步主链，但那时只说了 send 返回一个 `Future`，没有深入 answer 一个关键问题：**send() 调完就返回了，消息到底存哪了？** 本篇把这条线补上：`RecordAccumulator` 怎么按 topic-partition 聚成 batch、`BufferPool` 怎么管理堆外内存、`ProducerBatch` 怎么从创建到发送再到回收。这是 Kafka-2 的“send 主链”的下一层，也是 K-2 域第 2 篇。

## 先把真正的困惑摆出来：send() 为什么能毫秒级返回

`KafkaProducer.send()` 的调用者往往看到几乎立即返回，不管消息有多大。这听起来像是把消息写进了什么超级快的缓冲区，然后由某个后台线程取走真正发送。

这个直觉方向是对的，但不准确的地方在于：**不是随便一个缓冲区，而是按 topic-partition 组织的 batch 队列。**

```text
send(record)
  → 序列化
    → 分区
      → 找到这个 tp 对应的 batch 队列
        → 如果当前 batch 还能装，append 进去
          → 如果满了，新分配一个 batch 再 append
            → 返回（不是真的发出去）
```

Sender 后台线程从 accumulator 里 drain 满的 batch，才真正通过网络发送。

*关键设计（斜体）：* *KafkaProducer 把发送分解成“聚”和“发”两个阶段：`RecordAccumulator` 按 tp 维护 batch 队列，`send()` 只把消息聚进 batch；`BufferPool` 管理所有 batch 的堆外内存，内存不足时 `allocate()` 公平阻塞等待；`Sender` 线程后台 drain 满的或超时的 batch，再通过 `NetworkClient` 真正发送。*[模式: 按 tp 聚批 + 池化内存 + 后台 drain]

## 第一层：RecordAccumulator 按 topic-partition 维护 batch 队列

`RecordAccumulator` 内部维护一个 `ConcurrentMap<String, TopicInfo>`，每个 topic 的 `TopicInfo` 里包含 `batches: ConcurrentMap<Integer, Deque<ProducerBatch>>`，按 partition id 索引。

```text
RecordAccumulator
  → topicInfoMap
    → topic "orders"
      → partition 0: Deque[ProducerBatch]
      → partition 1: Deque[ProducerBatch]
    → topic "payments"
      → partition 0: Deque[ProducerBatch]
```

每个 tp 有一个 `Deque[ProducerBatch]`，而不是一个 batch。因为一个 batch 可能正在发送（inflight），而另一个 batch 正在被填充（append）。队列的**尾部**是当前可 append 的 batch，**头部**是已经发走或等待重试的 batch。`RecordAccumulator` 的注释说得很清楚："Only the last batch may be incomplete"（`RecordAccumulator.java:412`）——尾部 batch 可能是唯一 incomplete 的，其余都是 full 或已发送。

当 `append()` 被调用时：

1. 找到 tp 对应的 batch 队列；
2. 检查队列尾部 batch（`peekLast()`）是否还能装下这条 record；
3. 如果能，直接 append 进去；
4. 如果不能，从 `BufferPool` 分配一个新 ByteBuffer，创建新 batch 放在队列尾部，再 append。

如果 append 时发现队列尾部没有 batch 或尾部 batch 已满，就创建一个新的。新 batch 的创建会触发 `BufferPool.allocate()`，可能阻塞。

## 第二层：BufferPool 是堆外内存池，支持公平阻塞分配

`BufferPool` 管理的是 `java.nio.ByteBuffer`（堆外内存），不是 JVM 堆内存。总容量由 `buffer.memory` 控制，默认 32MB。

它的分配策略有几个关键设计：

### poolable size

`BufferPool` 以 `batch.size`（默认 16KB）作为 poolable 大小。当释放一个 `batch.size` 大小的 ByteBuffer 时，它不会归还给 GC，而是放进 `free` 列表（`Deque<ByteBuffer>`）直接复用，避免频繁分配和回收。

### 公平分配

`BufferPool` 维护一个 `waiters: Deque<Condition>`，保证公平。当内存不足时，调用线程会被放入 `waiters` 队列末尾，等待前一个线程释放内存后按顺序被唤醒。这样可以避免大请求线程永远等不到内存的饥饿问题。

```text
allocate(size)
  → 锁定
    → 有空闲 poolable 或 nonPooled 足够？
      → 是：直接分配
      → 否：放入 waiters 队列
        → await 等待
          → 被唤醒后再试
            → 超时 → BufferExhaustedException
```

### 非池化内存

如果请求的 size 不等于 poolableSize，分配时先从 `nonPooledAvailableMemory` 扣除，释放时归还到 `nonPooledAvailableMemory`，不进入 `free` 列表。

所以 `BufferPool` 不是简单的“分配一堆 ByteBuffer”，而是**按大小分类管理 + 公平等待 + 块分配**。如果 Producer 不管理内存，直接让每次 send 都分配新的 ByteBuffer，GC 压力和内存碎片都会失控。

## 第三层：ProducerBatch 是 append 的载体，不是简单的字节数组

每个 `ProducerBatch` 封装一个 `MemoryRecordsBuilder`，负责把 record 序列化成 Kafka 的二进制格式。

```text
ProducerBatch
  → recordsBuilder: MemoryRecordsBuilder
  → thunks: List<(Callback, RecordMetadata)>
  → produceFuture: ProduceRequestResult
  → attempts: AtomicInteger
  → finalState: ABORTED / FAILED / SUCCEEDED
  → inflight: boolean
  → retry: boolean
```

`ProducerBatch.append()` 调用 `recordsBuilder.append()` 把 record 写进底层的 ByteBuffer。如果写入成功，还会记录 `thunk`（callback + 预计的 metadata），这样当 batch 发送完成时，可以通过 `thunks` 依次回调每个 record 的 callback。

batch 的状态比较简单：

- 创建后处于可 append 状态；
- 关闭后（`close()` 或 `closeForRecordAppends()`）不能再 append；
- 被 Sender 线程 drain 出来后，在组装 `ProduceRequest` 时才被标记为 inflight（`batch.setInflight(true)`）；
- 收到响应后，标记 finalState 为 SUCCEEDED 或 FAILED，唤醒 `produceFuture`，通过 `thunks` 逐一回调；
- 回调完成后，释放 ByteBuffer 回 `BufferPool`。

## 第四层：Sender 线程的 drain 按 node 收 batch，不是按 tp

`RecordAccumulator` 按 tp 存 batch，但发送时是按 broker node 发送的。`Sender` 线程的 `drain()` 方法负责把 accumulator 中的 batch 按目标 node 分好，归到一起。

```text
drain()
  → 遍历所有有数据的 tp
    → 判断该 tp 的 leader 在哪个 node
      → 把该 tp 的 batch 放入该 node 的发送列表
        → 组合成 ProduceRequest
          → 通过 NetworkClient 发送
```

drain 时还会考虑：

- batch 是否已满（`batch.isFull()`）；
- batch 是否已超时（`linger.ms` 已到）；
- batch 是否还未准备好（刚创建，还在等更多数据）；
- 该 node 是否有其他未完成的请求限制。

注意 drain 从 deque 的**头部** `pollFirst()` 取 batch（`RecordAccumulator.java:893`），正好和 "尾部 append、头部 drain"的语义呼应：头部是最老、应该最先发出的 batch。

这也解释了为什么 `send()` 返回后消息并没有真的发出去：它还在 batch 里等着被 drain。只有 `Sender` 线程 drain 到它，它才真正进入网络层。

## 第五层：BufferPool 满了怎么办——阻塞等待，不是丢消息

`BufferPool.allocate()` 在内存不足时会阻塞等待，而不是直接抛异常或返回 null。阻塞的最大时间由 `max.block.ms`（默认 60s）控制，超时后抛 `BufferExhaustedException`。

阻塞等待期间，其他线程释放内存后（batch 发送完成、ByteBuffer 被归还），`BufferPool` 会按顺序唤醒等待队列中的线程。

```text
send() 时 BufferPool 不够
  → allocate() 阻塞等待
    → 其他线程释放内存
      → 按 waiters 队列顺序唤醒
        → 分配成功，继续 append
          → 超时 → BufferExhaustedException
```

这个设计保证了 Producer 不会因为内存耗尽而 OOM，也不会因为内存不足而静默丢消息。代价是 `send()` 可能被阻塞，这就是为什么 `max.block.ms` 应该被合理配置。

## 收网：send() 不是发，而是聚；Sender 才是真的发

把整篇压成一句话：`KafkaProducer.send()` 不是真的发送，而是先把 record 按 topic-partition 聚进对应的 `ProducerBatch`；batch 从 `BufferPool` 分配堆外内存（默认 32MB），按 poolable size 分类回收；`Sender` 线程后台 drain 满的或超时的 batch，按目标 node 收集后通过 `NetworkClient` 发送；内存不足时 `BufferPool.allocate()` 公平阻塞等待，超时抛 `BufferExhaustedException`。

```text
send(record)
  → RecordAccumulator.append()
    → 按 tp 找 batch 队列
      → 尾部 batch 还能装？append
      → 尾部 batch 满了？BufferPool.allocate() 新 batch
        → 返回 RecordAppendResult

Sender 后台线程
  → drain() 按 node 收 batch
    → NetworkClient 发送
      → 成功/失败 → 回调 thunks → 释放 ByteBuffer
```

到这里，主线只发生了五件事。

第一，`RecordAccumulator` 按 tp 维护 batch 队列，不是全局只一个队列。

第二，`BufferPool` 管理堆外内存，支持 poolable 回收与公平阻塞分配。

第三，`ProducerBatch` 封装 recordsBuilder、thunks、produceFuture，是 append 和发送的载体。

第四，`Sender.drain()` 按 node 收集满的 batch，不是按 tp 单独发送。

第五，内存不足时 `allocate()` 阻塞等待，超时才抛异常，不会静默丢消息。

**本篇的一句话困惑**：`KafkaProducer.send()` 调完就返回了，消息到底存哪了，什么时候才真的发出去？

**本篇的一句话顿悟**：send() 只是把消息按 topic-partition 聚进 `ProducerBatch`，batch 从 `BufferPool` 分配堆外内存；`Sender` 线程后台 drain 满的或超时的 batch，按 node 收集后通过 `NetworkClient` 真正发送。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“batch.size 是每个请求的大小。”** 它是每个 batch 的分配大小，实际请求可能更大（多个 batch 合并）或更小（batch 不满就发）。
2. **“BufferPool 是 Java 堆内存。”** `java.nio.ByteBuffer` 是堆外内存。
3. **“send() 返回后消息已经发出去了。”** 只进了 batch，还没到网络。
4. **“linger.ms 是延迟发送时间。”** 它是允许 batch 不满时等待的时间，不是强制延迟。
5. **“BufferPool 满了就丢消息。”** `allocate()` 会阻塞等待，超时才抛 `BufferExhaustedException`。

### 关键证据清单

- `clients/src/main/java/org/apache/kafka/clients/producer/internals/RecordAccumulator.java:68`：RecordAccumulator 类注释。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/RecordAccumulator.java:1278`：TopicInfo 与 batches per tp。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/RecordAccumulator.java:1232`：RecordAppendResult。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/BufferPool.java:45`：BufferPool 类注释。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/BufferPool.java:107`：allocate() 阻塞分配。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/ProducerBatch.java:60`：ProducerBatch 类。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:79`：Sender 后台线程。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/RecordAccumulator.java:412`：`allBatchesFull`——"Only the last batch may be incomplete"。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/RecordAccumulator.java:893`：drain 从 deque 头部 pollFirst。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:919`：组装 ProduceRequest 时 setInflight(true)。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦 client 聚批与内存管理，不展开网络发送、acks 确认、重试语义。
- 本篇不把 `BufferPool` 与 broker 侧 `SimpleMemoryPool` 混成同一个池。
- 本篇不把 `ProducerBatch` 的 `finalState` 与 `ProduceRequestResult` 混成同一个概念。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-2`（send 主链）、`Kafka-14`（broker 侧 SimpleMemoryPool 与 BufferPool 形成对照）。
- 后续桥接：下一篇进入 K-2 Producer 域第 3 篇（acks / 重试 / Sender 刷出），把 batch 发送后的确认回执与重试路径接上。