# Kafka-14. 请求在 RequestChannel 里排队时发生了什么——队列背压、内存池与限流主链

> 场景：Kafka-13 已经讲清楚了请求从 Acceptor 到 Processor 再到 Handler 的三层线程模型，但留下了一个关键问题：**如果请求来得太快，broker 靠什么防止自己被撑爆？** 本篇正面回答这个问题：Kafka 的背压不是单一路径，而是三条防线——有界队列、内存池、配额限流。

## 先把真正的困惑摆出来：请求太多，broker 会怎样

假设一个 produce 请求包含 100MB 的数据，或者同一时刻 10000 个客户端同时向 broker 发送请求。如果 broker 没有背压机制，它会：

1. 把请求全放进内存 → 内存耗尽，OOM；
2. 开启大量连接 → 文件描述符耗尽；
3. 处理线程被填满 → 请求排队时间无限增长；
4. 部分请求被执行、部分丢失 → 客户端无法判断哪些成功。

Kafka 的背压不是靠某一种策略，而是靠三条防线共同托底。

```text
请求到达
  → 第一道防线：连接限流（ConnectionQuotas）——accept 时刻
    → 第二道防线：SimpleMemoryPool 内存反压——数据读入时刻
      → 第三道防线：requestQueue / callbackQueue 有界队列——接收处理时刻
        → 处理完成
```

如果某一条防线失守，下一层还能兜住。如果全部失守，broker 仍然可以通过 channel mute 强行停止接收新请求。

*关键设计（斜体）：* *Kafka 的背压由三条按触发时序排列的防线构成：`ConnectionQuotas` 在 accept 时刻限制连接数与连接速率；`SimpleMemoryPool` 在数据读入时刻通过 channel 内存压力自静音形成反压，Selector 用 `outOfMemory` 标志跟踪并在内存恢复后统一 unmute；`requestQueue` / `callbackQueue` 的有界队列（默认 500）在接收处理时刻作为缓冲，队列满时 Processor 阻塞。*[模式: 三防线 + 有界队列 + 内存反压 + 通道静音]

## 第一层：RequestChannel 的三队列结构——不只是 requestQueue

Kafka-13 已经讲过 RequestChannel 是 Processor 与 Handler 之间唯一的握手通道。但它的队列结构比大多数人的理解更复杂：

- `requestQueue`：`ArrayBlockingQueue[BaseRequest]`，容量 = `queued.max.requests`（默认 500），Processor 放请求、Handler 取请求；
- `callbackQueue`：`ArrayBlockingQueue[BaseRequest]`，容量相同，专门承载 `CallbackRequest`（一种需要异步 completion 的请求类型）；
- 每条 Processor 都有自己的 `responseQueue`：`LinkedBlockingDeque[Response]`，Handler 放响应、Processor 取响应。

为什么要有两个入站队列？因为 `CallbackRequest` 不应该被普通请求阻塞。当 Handler 在处理完某个请求、需要异步回调时，它通过 `sendCallbackRequest()` 把回调放进 `callbackQueue`，而不是走 `requestQueue`，避免 callback 等待前面排队的普通请求。

```text
Processor
  → sendRequest → requestQueue.put     // 普通请求
  → Handler 侧 sendCallbackRequest
    → callbackQueue.put                // 回调请求

Handler
  → receiveRequest → requestQueue.poll / callbackQueue.poll
```

`receiveRequest(300)` 会优先处理 callbackQueue，只有在 callbackQueue 为空时才从 requestQueue poll 请求（最多等 300ms）。这样设计是为了保证 completion 回调不被长时间延迟，避免 Purgatory 等机制中的等待链恶性延长。

## 第二层：`requestQueue.put` 是阻塞的——队列满了，Processor 被卡住

`requestQueue` 是 `ArrayBlockingQueue`，它的 `put()` 方法是阻塞的。这意味着：如果 Handler 线程处理不过来，所有 Processor 的 `sendRequest()` 都会阻塞在 `requestQueue.put()` 上。

Processor 在 `processCompletedReceives()` 里调用了 `requestChannel.sendRequest(req)`，如果这个阻塞了，Processor 的整个主循环都会卡住，包括 `processNewResponses`、`poll`、`processCompletedSends` 等步骤。

```text
Processor 主循环
  → processCompletedReceives
    → requestChannel.sendRequest   // 如果 requestQueue 满了，这里阻塞
      → 后续步骤全部暂停
        → 该 Processor 上的所有连接读写都被冻结
```

这听起来很暴力，但它正是背压的第一层含义：**队列满了，生产者（Processor）就被迫停下来。** 如果 Handler 能力不足，Processor 不会继续接收更多请求，而是等待 Handler 处理完任务后再恢复。

这也是为什么 Kafka 要求 `num.io.threads`（Handler 数量）与 `num.network.threads`（Processor 数量）要配合调整的原因。如果 Handler 太少，requestQueue 频繁满，所有 Processor 都会被阻塞，吞吐反而下降。

## 第三层：`SimpleMemoryPool`——内存不够时，连读都不读了

如果队列满了是背压的第一道，那内存不够就是第二道，而且它能在队列还没满之前就动手。

`SocketServer` 在初始化时，会创建一个 `SimpleMemoryPool`，容量由 `queued.max.bytes` 控制（默认不限制，但可以配置）。Select 在读取请求数据时，channel 需要从 `SimpleMemoryPool` 分配缓冲区。

如果内存池剩余不足，channel 会因内存压力自静音（self-mute），Selector 在 `poll()` 中检测到 channel 静音后记录 `outOfMemory = true`，同时打乱就绪 key 的读取顺序，防止某个连接因内存不足而饿死。当内存恢复后，Selector 在下一轮 `poll()` 中统一 unmute 这些因内存压力自静音的通道。

```text
内存池剩余不足（低于 lowMemThreshold ~10%）
  → channel 自静音
    → Selector 记录 outOfMemory = true
      → 打乱就绪 key 顺序，防饥饿
        → 内存恢复 → Selector 在 poll() 中统一 unmute
```

这意味着 Kafka 的背压能够在“请求还没完全读入内存”的阶段就动手。不是等到队列满了才让 Processor 阻塞，而是直接在 NIO 层面暂停某个连接的读取，让客户端自己感知到 TCP 窗口不再滑动。

## 第四层：`ConnectionQuotas`——从连接入口就拦住

前面两层的背压都是在请求已经进入 Processor 之后才生效的。但 Kafka 还有一层更早的防线：**连接准入控制**。

`ConnectionQuotas` 会根据每个来源 IP 的连接数、连接速率来做限流：

- 如果某个 IP 的连接数超过 `max.connections.per.ip`，新的连接直接被拒绝；
- 如果连接速率过快，抛出 `ConnectionThrottledException`，连接被延迟关闭；
- 如果 broker 的总连接数超过 `max.connections`，`closeExcessConnections()` 会关闭低优先级的连接。

```text
客户端尝试连接
  → connectionQuotas.inc
    → 超限？拒绝 / 节流
      → 通过？进入 Acceptor 分配流程
```

这条防线在 Acceptor 层就生效了，连 Processor 都不用碰。如果 Kafka 只靠队列和内存池做背压，恶意客户端只需要开大量连接就能让 broker 的文件描述符耗尽，根本轮不到请求队列发挥作用。

## 第五层：`ClientQuotaManager`——请求级别的限流

连接限流只能挡住“恶意开连接”的客户端，但挡不住“一个连接上疯狂发请求”的客户端。

`ClientQuotaManager` 管理的是请求级别的配额。当某个客户端在一段时间内产生的请求速率超过配额时，broker 不会直接拒绝请求，而是返回一个 **throttling response**，告诉客户端“你被限流了，等待 throttleTimeMs 后再重试”。

```text
请求处理完成
  → ClientQuotaManager 检查配额
    → 超限？
      → 响应中携带 throttleTimeMs
        → 客户端等待后重试
```

这个限流发生在请求处理之后，而不是之前。因为 Kafka 需要先处理请求才能知道它是否超限。但关键不同在于：**限流响应不是错误，而是携带延迟指示的正常响应。** 客户端按指示等待后重试，不会无脑重试加重负载。

## 第六层：请求生命周期指标——背压的效果看得见

Kafka 为每个请求记录了从入站到出站的完整时间线。`RequestChannel.Request` 类维护了这些时间戳：

- `requestDequeueTimeNanos`：Handler 从队列取出请求的时间；
- `apiLocalCompleteTimeNanos`：本地处理完成的时间；
- `apiRemoteCompleteTimeNanos`：远程处理完成的时间（如等待 follower 确认）；
- `responseCompleteTimeNanos`：响应准备完成的时间；
- `responseDequeueTimeNanos`：Processor 从 responseQueue 取出响应的时间。

通过这些指标，可以计算出：

- **requestQueueTime** = requestDequeueTime - startTime：请求在队列里等了多久；
- **apiLocalTime** = apiLocalCompleteTime - requestDequeueTime：Handler 本地处理时间；
- **apiRemoteTime** = apiRemoteCompleteTime - apiLocalCompleteTime：等待远程（如 ISR）的时间；
- **apiThrottleTime**：配额限流的时间；
- **responseQueueTime** = responseDequeueTime - responseCompleteTime：响应在 responseQueue 里等了多久。

如果 requestQueueTime 持续上升，说明 Handler 处理能力不足；如果 responseQueueTime 持续上升，说明 Processor 处理能力不足。这些指标可以直接用来判断背压发生在哪一层。

## 收网：背压三防线 + 队列解耦

把整篇压成一句话：Kafka 的请求背压由三条按触发时序排列的防线构成——`ConnectionQuotas` 在 accept 时刻拦住连接；`SimpleMemoryPool` 在数据读入时刻通过 channel 自静音、Selector `outOfMemory` 反压，让请求还没完全读入内存就被暂停；`requestQueue` / `callbackQueue` 的有界队列（默认 500）在接收处理时刻形成缓冲，队列满时 Processor 阻塞。

```text
客户端请求
  → ConnectionQuotas（连接数/速率限制）
    → SimpleMemoryPool（内存分配反压）
      → requestQueue.put（有界队列，满时阻塞）
        → Handler 处理
          → ClientQuotaManager（请求速率限流）
            → responseQueue（发出响应）
```

到这里，主线只发生了六件事。

第一，RequestChannel 有三条队列：requestQueue、callbackQueue、per-Processor responseQueue。

第二，`requestQueue.put` 是阻塞的，队列满时 Processor 暂停。

第三，`SimpleMemoryPool` 在内存不足时让 channel 自静音，Selector 用 outOfMemory 统一反压。

第四，`ConnectionQuotas` 在 Acceptor 层就拒绝或节流连接。

第五，`ClientQuotaManager` 在请求处理完成后返回 throttle 指示。

第六，请求生命周期指标可以准确定位背压发生在哪一层。

**本篇的一句话困惑**：请求太多时，Kafka 靠什么防止自己先被撑爆？

**本篇的一句话顿悟**：Kafka 的背压是三条防线——连接限流挡住入口，有界队列让 Processor 在队列满时阻塞，内存池在 NIO 层面直接 mute 连接让数据读不进；三层共同保护 broker 不被请求洪峰冲垮。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“RequestChannel 只有一个 requestQueue。”** 还有 callbackQueue 和每个 Processor 的 responseQueue。
2. **“队列满了以后请求自动重试。”** `requestQueue.put` 会阻塞，不是立即返回错误。
3. **“内存池满了就拒绝请求。”** channel 被 mute，不读新请求，但不会丢旧请求。
4. **“限流就是拒绝请求。”** 限流返回 throttling response，client 可以等待后重试。
5. **“背压只在队列上。”** 内存池、连接数、请求配额共同构成三层防线。

### 关键证据清单

- `core/src/main/scala/kafka/network/RequestChannel.scala:351`：requestQueue。
- `core/src/main/scala/kafka/network/RequestChannel.scala:353`：callbackQueue。
- `core/src/main/scala/kafka/network/RequestChannel.scala:377`：sendRequest（阻塞 put）。
- `core/src/main/scala/kafka/network/RequestChannel.scala:497`：sendCallbackRequest（阻塞 put）。
- `core/src/main/scala/kafka/network/RequestChannel.scala:463`：receiveRequest(300) 优先 poll callbackQueue。
- `core/src/main/scala/kafka/network/SocketServer.scala:96`：SimpleMemoryPool 创建与内存反压。
- `core/src/main/scala/kafka/network/SocketServer.scala:1054`：sendRequest 后 mute 连接。
- `core/src/main/scala/kafka/network/SocketServer.scala:1455`：连接限流（throttle）。
- `clients/src/main/java/org/apache/kafka/common/memory/SimpleMemoryPool.java:33`：CAS 内存分配。
- `clients/src/main/java/org/apache/kafka/common/network/Selector.java:668`：内存低时打乱就绪 key；outOfMemory 跟踪与 unmute。
- `core/src/main/scala/kafka/network/RequestChannel.scala:65`：Request 的请求时间戳字段。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦背压与队列，不展开 ClientQuotaManager 内部算法、KafkaApis 路由。
- 本篇把三防线作为独立层，不把 Processor 的 mute 与 Handler 的限流混成同一概念。
- 本篇不把 `SimpleMemoryPool` 与 `BufferPool`（Producer 端）混同一个池。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-13`（三层线程模型与 RequestChannel 结构）。
- 后续桥接：下一篇可进入 K-2 Producer 域第 2 篇（RecordAccumulator / BufferPool / Batch），与这里的背压一脉相承。