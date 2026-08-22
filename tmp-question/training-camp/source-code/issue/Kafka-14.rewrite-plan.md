# Kafka-14 重写规划

> 题目：请求在 RequestChannel 里排队时发生了什么——队列背压、内存池与限流主链
> 状态：K-1 域第 2 篇，按“RequestChannel 与队列背压”展开
> 目标：解释 Kafka 的请求在进入 RequestChannel 后，如何通过队列容量、内存池和配额限流三条路径形成背压，防止请求堆积击垮 broker。覆盖 requestQueue / callbackQueue / responseQueue 的三队列结构、SimpleMemoryPool 的内存反压、connectedQuotas 与 clientRequestQuota 的限流，以及请求生命周期中各个时间段的度量。

## 1. 读者困惑

- Producer 发请求太快，会不会把 broker 内存撑爆？
- RequestChannel 为什么要有两个入站队列（requestQueue + callbackQueue）？
- 为什么 Processor 收到了请求还要丢进队列，而不是直接交给 Handler 处理？
- 背压靠什么实现？队列满了、内存不够、连接超限分别对应什么策略？
- 请求从入站到出站，broker 记录了哪些时间指标？

## 2. 一句话顿悟

**Kafka 的背压不是单一路径，而是三条防线：`requestQueue` / `callbackQueue` 的有界队列作为第一道，`SimpleMemoryPool` 的内存不敷分配作为第二道，`ConnectionQuotas` 与 `ClientQuotaManager` 的限流作为第三道。三者共同决定：当一个请求到来时，是被接受、被排队、被节流，还是被直接拒绝。**

## 3. 五要素卡片

### 读者问题

客户端短时间发送大量请求，broker 靠什么防止自己先被这些请求吞掉？

### 入口

- `RequestChannel`：requestQueue + callbackQueue + per-Processor responseQueue
- `SimpleMemoryPool`：内存池，分配失败时 channel 被 mute
- `ConnectionQuotas`：连接数/连接速率限制
- `ClientQuotaManager`：请求处理速率配额
- `RequestMetrics`：请求生命周期时间指标

### 状态核心

- `requestQueue`：`ArrayBlockingQueue(queueSize)`，阻塞 put
- `callbackQueue`：`ArrayBlockingQueue(queueSize)`，阻塞 put
- `responseQueue`：per-Processor `LinkedBlockingDeque`
- `SimpleMemoryPool`：`availableMemory` CAS 分配，耗尽时暂停接收
- `queued.max.requests`：默认 500
- `queued.max.bytes`：默认不限制
- 请求时间：requestQueueTime / apiLocalTime / apiRemoteTime / apiThrottleTime / responseQueueTime

### 失败路径

- 队列无界 → 请求堆积 → OOM
- 内存池无限 → 网络层持续接收 → 内存耗尽
- 无连接限流 → 大量连接涌入 → 文件描述符耗尽
- 无配额限流 → 大 client 挤占小 client

### 连接点

- 前文 `Kafka-13`：三层线程模型与 RequestChannel 的关系。
- 前文 `Kafka-10`：Purgatory 等条件满足才返回，延长了 responseQueueTime 但不影响背压本身。
- 后文：K-2 Producer 第 2 篇 (RecordAccumulator/BufferPool) 与这里的 MemoryPool 一脉相承。

## 4. 总图

```text
Processor 接收请求
  → SimpleMemoryPool 分配缓冲区
    → 分配成功 → 解析请求 → requestQueue.put（或 callbackQueue.put）
    → 分配失败 → channel 被 mute，等待内存释放

Handler 处理
  → receiveRequest / receiveCallbackRequest
    → 业务处理
      → sendResponse 到 Processor.responseQueue

限流器
  → ConnectionQuotas（连接数硬限制）
  → ClientQuotaManager（请求速率配额）
    → 超限时返回 throttling response
```

## 5. 关键边界

- 本篇不重复三层线程模型的结构（Kafka-13 已讲），只展开队列与背压。
- 不把 MemoryPool 与 BufferPool（Producer 侧）混成同一个池。
- 不把 `callbackQueue` 写成普通请求队列：它只承载 `CallbackRequest`，用于某些异步 completion 路径。
- 不展开 ClientQuotaManager 的具体算法，只讲背压因果关系。

## 6. 失败方案推演

1. **队列无界**：请求堆积直到内存耗尽，OOM。
2. **只靠队列、不靠内存池**：队列没满但内存已被请求缓冲区耗尽，下层还是 OOM。
3. **只靠队列和内存池、不靠连接限流**：恶意 client 开大量连接耗尽文件描述符。
4. **只靠连接限流、不靠请求配额**：一个连接上的请求速率可以无限大。

## 7. 误解清单

- “RequestChannel 只有一个 requestQueue”：还有 callbackQueue 和每个 Processor 的 responseQueue。
- “队列满了以后请求自动重试”：`requestQueue.put` 会阻塞，不是立即返回错误。
- “内存池满了就拒绝请求”：channel 被 mute，不读新请求，但不会丢旧请求。
- “限流就是拒绝请求”：限流（throttle）返回 throttling response，client 可以重试。
- “背压只在队列上”：内存池、连接数、请求配额共同构成三层防线。

## 8. 证据清单

- `core/src/main/scala/kafka/network/RequestChannel.scala:351`：requestQueue。
- `core/src/main/scala/kafka/network/RequestChannel.scala:353`：callbackQueue。
- `core/src/main/scala/kafka/network/RequestChannel.scala:377`：sendRequest（阻塞 put）。
- `core/src/main/scala/kafka/network/RequestChannel.scala:497`：sendCallbackRequest（阻塞 put）。
- `core/src/main/scala/kafka/network/RequestChannel.scala:463`：receiveRequest 从两个队列 poll。
- `core/src/main/scala/kafka/network/SocketServer.scala:96`：SimpleMemoryPool 创建与内存反压。
- `core/src/main/scala/kafka/network/SocketServer.scala:1054`：sendRequest 后 mute 连接。
- `core/src/main/scala/kafka/network/SocketServer.scala:1455`：连接限流（throttle）。
- `clients/src/main/java/org/apache/kafka/common/memory/SimpleMemoryPool.java:33`：CAS 内存分配。
- `core/src/main/scala/kafka/server/ClientQuotaManager.scala`：请求配额限流。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦背压与队列，不展开 ClientQuotaManager 内部算法、KafkaApis 路由。
- 目标正文：6000~10000 字；核心拆解层覆盖三队列、内存池、配额定限流三层防线。

## 10. 本轮重写主线

1. 从“请求太快会不会撑爆 broker”开场。
2. 否定“队列无界”、“只靠队列”、“只靠内存池”三种直觉方案。
3. 解释 requestQueue / callbackQueue / responseQueue 的三队列结构。
4. 解释 SimpleMemoryPool 与 channel mute 的内存反压。
5. 解释 ConnectionQuotas / ClientQuotaManager 的限流。
6. 解释请求生命周期指标。
7. 收网：背压三防线 + 队列解耦。