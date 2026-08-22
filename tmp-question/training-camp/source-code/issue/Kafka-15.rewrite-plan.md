# Kafka-15 重写规划

> 题目：Producer 的 send() 为什么不是真的“发出去”——RecordAccumulator、BufferPool 与 ProducerBatch 主链
> 状态：K-2 Producer 域第 2 篇，按“RecordAccumulator/BufferPool/Batch”展开
> 目标：解释 Kafka Producer 的 `send()` 为什么不是直接发到网络，而是先聚成 batch。主线覆盖 `RecordAccumulator.append()` 的按 tp 分区聚合、`BufferPool` 的堆外内存管理、`ProducerBatch` 的生命周期（创建→填充→关闭→drain→发送→回收），以及 `BufferPool` 在内存不足时的公平阻塞分配。

## 1. 读者困惑

- `send()` 调完就返回了，消息到底存哪了？
- 为什么指定 `batch.size=16KB`，但发出去的请求可能比这个值大或小？
- `BufferPool` 和 `SimpleMemoryPool` 有什么区别？
- 当内存不够时，`send()` 会怎样？为什么不会直接把请求丢进网络缓冲区？
- 所谓的“聚批”到底是怎么聚的？按 topic-partition 来有什么区别？
- `Sender` 线程 drain batch 时，按什么顺序从不分 partition 里取 batch？

## 2. 一句话顿悟

**`KafkaProducer.send()` 不是真的发送，而是先把 Record 按 topic-partition 找到对应的 `ProducerBatch`，如果 batch 还能装得下就 append 进去，否则从 `BufferPool` 新分配一块内存创建新 batch 再 append；`Sender` 线程后台 drain 满的或超时的 batch，再通过 `NetworkClient` 真的发出去。**

## 3. 五要素卡片

### 读者问题

`send()` 为什么能毫秒级返回？消息到底存到哪了，等谁来取？

### 入口

- `RecordAccumulator`：按 tp 维护 batch 队列，聚合写入
- `RecordAccumulator.append()`：核心的 append 路径
- `BufferPool`：堆外内存池，支持公平阻塞分配
- `ProducerBatch`：单个 batch 的封装，包含 recordsBuilder、callback、状态
- `Sender.run()`：后台 drain 线程，取满的 batch 发送

### 状态核心

- `TopicInfo.batches`：每个 tp 的 `Deque[ProducerBatch]`
- `BufferPool`：`free`(poolable 大小 ByteBuffer 回收列表) + `waiters`(公平等待队列) + `nonPooledAvailableMemory`
- `ProducerBatch`：`recordsBuilder` + `thunks`(callback) + `finalState`(ABORTED/FAILED/SUCCEEDED)
- `RecordAppendResult`：`future` + `batchIsFull` + `newBatchCreated`

### 失败路径

- BufferPool 内存耗尽 → `send()` 阻塞等待 → 超时抛 `BufferExhaustedException`
- batch 满后不关闭 → 后续消息无法 append 到新 batch
- 不按 tp 聚合 → 每次 send 都发一次请求，吞吐崩溃
- Sender 不 drain → 所有 batch 永远留在 accumulator 里

### 连接点

- 前文 `Kafka-2`：send 主链调用 `accumulator.append()`。
- 前文 `Kafka-14`：broker 侧 `SimpleMemoryPool` 与 client 侧 `BufferPool` 相辅相成。
- 后文：K-2 Producer 域第 3 篇（acks / 重试 / Sender 刷出），把 batch 发送后的确认回执与重试路径接上。

## 4. 总图

```text
KafkaProducer.send()
  → RecordAccumulator.append()
    → 按 tp 查找当前批次
      → batch 有空间？直接 append
      → batch 满了？从 BufferPool 分配新 batch
        → 新 batch 创建 → append
          → 返回 RecordAppendResult(future, batchIsFull, newBatchCreated)

Sender 后台线程
  → drain() 按 node 收集满的 batch
    → 通过 NetworkClient 发送
      → 成功/失败 → 回调 future
        → 释放 ByteBuffer → BufferPool.deallocate()
```

## 5. 关键边界

- 本篇只讲 client 端聚批，不展开网络发送与确认回执（下一篇）。
- 不把 `BufferPool` 与 broker 侧 `SimpleMemoryPool` 混成同一个池。
- 不把 `ProducerBatch` 的 `finalState` 与 `ProduceRequestResult` 混成同一个概念。
- 不把 `linger.ms` 解释成“延迟发送”，它就是“允许 batch 不满时等待的时间”。

## 6. 失败方案推演

1. **每次 send() 都直接发一次网络请求**：高吞吐不可持续，大量小请求浪费带宽。
2. **不按 tp 聚合，全局一个大 batch**：分区归属错乱，无法按 node 发送。
3. **BufferPool 无界**：内存无限增长，OOM。
4. **BufferPool 不公平**：大请求线程永远等不到内存，饿死。

## 7. 误解清单

- “batch.size 是每个请求的大小”：它是每个 batch 的分配大小，实际请求大小可能更大或更小。
- “BufferPool 是 Java 堆内存”：`java.nio.ByteBuffer` 是堆外内存。
- “send() 返回后消息已经发出去了”：只进了 batch，还没到网络。
- “linger.ms 是延迟发送时间”：它是允许 batch 不满时等待的时间，不是强制延迟。
- “BufferPool 满了就丢消息”：`allocate()` 会阻塞等待，超时才抛异常。

## 8. 证据清单

- `clients/src/main/java/org/apache/kafka/clients/producer/internals/RecordAccumulator.java:68`：RecordAccumulator 类注释。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/RecordAccumulator.java:82`：BufferPool 引用。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/RecordAccumulator.java:1278`：TopicInfo 与 batches per tp。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/RecordAccumulator.java:1232`：RecordAppendResult。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/BufferPool.java:45`：BufferPool 类注释（poolable size + 公平分配）。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/BufferPool.java:107`：allocate() 阻塞分配。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/ProducerBatch.java:60`：ProducerBatch 类。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:79`：Sender 后台线程。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦 client 聚批与内存管理，不展开网络发送、acks 确认、重试语义。
- 目标正文：7000~11000 字；核心拆解层覆盖 append 路径、BufferPool 分配、batch 生命周期、Sender drain。

## 10. 本轮重写主线

1. 从“send() 为什么能毫秒级返回”开场。
2. 否定“每次 send 都发一次网络请求”和“batch 只按大小、不分 tp”两种方案。
3. 解释 `RecordAccumulator.append()` 按 tp 找 batch 的路径。
4. 解释 `BufferPool` 的池化分配与公平阻塞。
5. 解释 `ProducerBatch` 从创建到关闭的有限状态机。
6. 解释 `Sender.drain()` 如何按 node 收集 batch。
7. 收网：send() 不是发，而是聚；Sender 才是真的发。