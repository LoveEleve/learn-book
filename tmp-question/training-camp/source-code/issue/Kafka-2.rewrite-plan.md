# Kafka-2 重写规划

> 题目：KafkaProducer.send() 以后，消息为什么不会立刻飞到 Broker —— Producer 聚合与发送主链
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：解释 Kafka Producer 的真正发送主链——`send()` 为什么不是一次同步 socket write，而是“metadata 可用 → 分区决策 → RecordAccumulator 聚合 → Sender 后台刷出 → future 收口”的多阶段过程。

## 1. 读者困惑

- `KafkaProducer.send()` 为什么不是一调用就立刻把消息发到网络上？
- `RecordAccumulator`、`ProducerBatch`、`BufferPool`、`Sender` 分别在补什么缺口？
- 分区决策、metadata 等待和 batch 聚合，到底哪一步才算真正“进入发送主链”？
- `Future<RecordMetadata>` 为什么能先返回，而 Broker 结果要后面才到？
- 为什么 Kafka Producer 的复杂度主要不在 socket，而在发送前的本地聚合与调度？

## 2. 一句话顿悟

**KafkaProducer.send() 的关键不在“立刻写 socket”，而在“先确认 topic metadata、再决定 partition、再把记录压进分区 batch，最后由 Sender 后台线程统一把可发送 batch 刷向 leader broker”；send 返回的 future 只是把这条异步流水线的结果句柄先交给调用方。**

## 3. 五要素卡片

### 读者问题

业务线程调一次 `send()`，为什么消息并不会马上离开客户端，而是先经过一层本地世界的等待、聚合和调度？

### 入口

- `KafkaProducer.send()` / `doSend()`：发送总入口
- `waitOnMetadata()`：topic 元数据与分区存在性前置检查
- `partition(...)`：决定 topic-partition
- `RecordAccumulator.append(...)`：进入本地聚合队列
- `Sender.run()`：后台线程 drain + NetworkClient 发送

### 状态核心

- metadata 是否已有 topic / partition 视图
- serialized key/value / partition 结果
- `ProducerBatch` / `BufferPool`
- `RecordAccumulator.RecordAppendResult`
- `batchIsFull` / `newBatchCreated` / `future`
- `acks` 与真正 broker 应答仍在后续阶段

### 失败路径

- 没有 metadata 就直接发：topic 分区都不确定，消息没法落到具体 leader
- 每条消息都立即发网络：batch、压缩、吞吐、请求合并都失效
- 不做 BufferPool 管理：高并发发送会在内存申请与背压上失控
- 没有 Sender 后台线程：业务线程会被迫承担 drain / 网络 I/O / 响应处理全链路
- send 返回就当 broker 已确认：future 与真实应答边界会被混淆

### 连接点

- 前文 `Kafka-1`：Producer 聚合发送是总图第一层
- 后文 `Kafka-3`：消息进入 Broker 后怎样落成分区日志
- 后文 `Kafka-9/10/12`：acks、ISR、事务幂等是这条发送链的后续确认/可靠性补层

## 4. 总图

```text
业务线程 send(record)
  → waitOnMetadata(topic/partition)
    → 序列化 key/value
      → partition(...) 决定 topic-partition
        → RecordAccumulator.append() 进入 batch 世界
          → 满批次或新建 batch 时唤醒 Sender
            → Sender drain ready batch
              → NetworkClient 发给 leader broker
                → future 最终由 broker 响应完成
```

## 5. 关键边界

- 本篇只讲 Producer 本地发送主链，不展开 Broker 端日志落盘与 ISR。
- 不把 `send()` 返回 future 误写成 broker 已确认；真正结果在 Sender/响应阶段完成。
- 不把 metadata 等待写成附属步骤；没有 topic/partition 视图，Producer 连候选目标都没有。
- 不把 `RecordAccumulator` 写成普通缓存；它是按分区聚合和触发发送的核心调度层。

## 6. 失败方案推演

1. **send 一调用就立刻发 socket**：最直觉，但批量聚合、压缩、吞吐和统一收口都会塌掉。
2. **不等 metadata，边发边说**：topic-partition/leader 都不确定，发送主链没有落点。
3. **没有 Sender，业务线程自己 drain 网络**：调用线程被迫承担异步发送世界全部复杂度。
4. **把 future 返回当成成功**：会误判 broker 确认边界。

## 7. 误解清单

- `send()` 不是同步 socket write。
- `RecordAccumulator` 不只是缓存，它决定批量发送节奏。
- metadata 等待不是额外开销，而是发送落点成立的前提。
- Sender 才是真正把 batch 刷向 broker 的后台执行者。
- future 先返回不等于 broker 已确认写入。

## 8. 证据清单

- `clients/src/main/java/org/apache/kafka/clients/producer/KafkaProducer.java:984`
- `clients/src/main/java/org/apache/kafka/clients/producer/KafkaProducer.java:1017`
- `clients/src/main/java/org/apache/kafka/clients/producer/KafkaProducer.java:1027`
- `clients/src/main/java/org/apache/kafka/clients/producer/KafkaProducer.java:1041`
- `clients/src/main/java/org/apache/kafka/clients/producer/KafkaProducer.java:1093`
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/RecordAccumulator.java:68`
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:382`
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/BufferPool.java:1`

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦 Producer 本地发送主链，不展开 broker 端 `ReplicaManager.appendRecords()` 细节。
- 目标正文：8000~12000 字；核心拆解层覆盖 metadata、分区、accumulator、sender、future 边界。

## 10. 本轮重写主线

1. 从“send 为什么不是立刻发出去”开场。
2. 否定无 metadata 直发、每条都立即发网络、业务线程自己 drain 三种直觉方案。
3. 解释 `waitOnMetadata()` 与 partition 决策先补什么缺口。
4. 解释 `RecordAccumulator`/`BufferPool`/`ProducerBatch` 为什么构成发送前的本地世界。
5. 解释 `Sender` 怎样把 ready batch 真正刷向 broker，并收口 future。
6. 收网：KafkaProducer 的复杂度主要在发送前的本地聚合与调度，而不是 socket 调用本身。