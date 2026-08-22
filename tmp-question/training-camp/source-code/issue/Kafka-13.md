# Kafka-13. 一个请求怎么真正进到 Broker——SocketServer 的 Acceptor→Processor→Handler 三层线程模型

> 场景：前面 Kafka-2 到 Kafka-12 讲的全是“请求到了 broker 之后怎么被处理”，但一直没有正面回答一个问题：**请求到底是怎么从网卡走进 broker 的业务层、再走回去的？** 本篇补上这条底层主链：`SocketServer` 的三层线程模型——Acceptor、Processor、Handler。这也是所有后续专题（Producer、Consumer、Controller、事务）共同依赖的网络底座。

## 先把真正的困惑摆出来：一个请求从客户端到 broker，经过了几层线程？

假设客户端向 broker 发送一个 fetch 请求。它会在网络上先建立 TCP 连接，然后发送请求字节流。这些字节到达 broker 所在机器的网卡之后，谁来接收、谁来解析、谁来处理、谁来回复？

一个很直觉的想法是：让 broker 用一个线程循环 accept 连接，然后再用一个线程处理所有请求。可如果只有一个 accept 循环，同时几千个请求一起来，accept 直接成为瓶颈；如果让那个线程既 accept 又读数据又做业务，它很快会被拖垮。

另一个直觉想法是：既然要并行，就让每个连接一个线程，自己负责自己的读写和业务处理。这就是“thread-per-connection”，连接少时没问题，连接一多线程数爆炸，上下文切换开销巨大。

Kafka 的选择是一条更工程化的路：**把“建连”“读写”“业务处理”三个阶段拆给不同的线程层，中间用队列解耦。**

```text
客户端
  → TCP 连接
    → Acceptor 线程（accept + 分发给谁）
      → Processor 线程（NIO 读写 + 解析）
        → RequestChannel 队列
          → Handler 线程（业务处理）
```

整体看起来像一条流水线，每一站只做一件事。本篇就沿着这条流水线走一遍。

*关键设计（斜体）：* *Kafka 的 SocketServer 用三层线程拆开“网络 IO”和“业务处理”：Acceptor 每 listener 一个，只负责 accept 新连接并 round-robin 分配给 Processor；Processor 每 listener 多个、各自持有 Selector，在单线程内完成新建连接注册、poll 读写、解析请求入 RequestChannel、发送响应；Handler 线程池从 RequestChannel 取请求处理，再把响应放回对应 Processor 的 responseQueue。*[模式: 生产者-消费者队列 + NIO 事件驱动 + 线程池]

## 第一层：SocketServer 的顶层结构——每个 listener 有自己的 acceptor-processor 集合

`SocketServer` 不是只有一个 acceptor 和一个 processor 的扁平结构。Kafka 可以配置多个 listener（例如 `PLAINTEXT://host:9092`、`CONTROLLER://host:9093` 等），每个 listener 对应一组 acceptor + processors。

这也是 `SocketServer` 会看到 `dataPlaneAcceptors` 这种按 Endpoint 分组的 Map 的原因：每个 endpoint（listener）都有自己的数据面 acceptor，而每个 acceptor 内部再维护自己的 processors 数组。

```text
SocketServer
  → Listener A
    → Acceptor A
      → Processor A0, A1, A2, ...
  → Listener B
    → Acceptor B
      → Processor B0, B1, B2, ...
```

Processor 的编号在全局是唯一的，但归属仍然按 listener 分开，保证不同协议/端口的连接不会互相干扰。

这一点很关键：**三层模型不是“一个进程里只有一个 acceptor”，而是“每个 listener 一套三层模型”。** 对控制面 listener 和数据面 listener 来说，它们的 acceptor/processor 是并行存在的。

## 第二层：Acceptor 的职责——accept 新连接、做连接准入控制、然后分给 Processor

`Acceptor` 是一个典型的 `Runnable`。它启动后，把 `serverChannel` 注册到自己的 `NSelector` 上，监听 `OP_ACCEPT`：

```text
serverChannel.register(nioSelector, SelectionKey.OP_ACCEPT)
  → 当有客户端尝试连接
    → accept 出新 SocketChannel
```

但 accept 出一个 SocketChannel 之后，Acceptor 并不会直接把它丢给 Processor，还会先做一次**连接准入控制**：

- 用 `connectionQuotas.inc(...)` 累计该来源 IP 的连接数；
- 如果来源 IP 已超过配额，直接拒绝并关闭连接（`TooManyConnectionsException`）；
- 如果连接命中节流（`ConnectionThrottledException`），先放进 `throttledSockets` 延迟关闭。

只有通过准入检查的连接，才会进入分配流程。

随后 `acceptNewConnections()` 会拿到新连接，并用 round-robin 的方式把它分配到某个 Processor。分配的方式不是直接调用 Processor，而是把 SocketChannel 放进 Processor 的 `newConnections` 队列（`ArrayBlockingQueue`）。

```text
Acceptor
  → accept 新连接
    → 连接配额 / 节流检查（不合格直接拒绝或延迟关闭）
      → round-robin 选一个 Processor
        → processor.accept(socketChannel)
          → newConnections.offer(socketChannel)
```

round-robin 实际是“轮询尝试 + 兜底阻塞”的顺序：Kafka 会依次尝试每个 Processor，谁的 `newConnections` 还有空位，就把连接交给谁；只有当所有 Processor 都满了，才会轮到最后一个用阻塞的 `put` 等待槽位。这样既保证连接均匀分布，又不会因为某一个 Processor 短暂满队列就立刻卡死 Accept。

如果 Acceptor 不在 accept 后立刻移交，而是自己把这个连接读起来、处理起来，主链会先在哪失败？Acceptor 线程只有一个，它一旦被某个连接的读取阻塞，后面所有新连接都会排队等 accept，整个 broker 的“接客”能力被单点卡住。所以 Acceptor 只做 accept、准入控制和分发，绝不做业务。

## 第三层：Processor 是三层模型里的“网络 IO 玩家”

Acceptor 只负责“放行新连接”，真正负责连接上读写的是 Processor。

每个 Processor 拥有：

- **一个 Selector**（event-driven NIO）——注意这个 `Selector` 是 Kafka 自己的 `org.apache.kafka.common.network.Selector`，不是 Acceptor 用的 `java.nio.channels.Selector`；它要在 NIO 之上额外处理连接认证、请求大小限制、配额等 server 侧语义；
- **一个 `newConnections` 队列**（来自 Acceptor 的新连接）；
- **一个 `responseQueue`**（来自 Handler 的待发送响应）。

它通过 `newConnections` 从 Acceptor 收连接，通过 `responseQueue` 从 Handler 收响应，通过 Selector 做实际读写。三者协作，让 Processor 在自己单线程内把某个连接的进和出都管起来。

这样，一个处理器实例就能处理成百上千个并发连接的读写，而不是一个连接一个线程。

## 第四层：Processor 主循环的七步——这是本篇最重要的地图

`Processor.run()` 不是“进了循环就 poll”，而是一组有顺序的步骤，每一轮循环执行：

```text
configureNewConnections()   // 1. 从 newConnections 取出新连接，注册到 selector
processNewResponses()       // 2. 从 responseQueue 取出 Handler 放入的响应，交给 selector.send
poll()                      // 3. 等待并处理就绪的读写事件
processCompletedReceives()  // 4. 解析完整接收，放入 RequestChannel
processCompletedSends()     // 5. 处理已发送完成的响应
processDisconnected()       // 6. 处理断开连接
closeExcessConnections()    // 7. 关闭过多连接
```

### 第一步：configureNewConnections

把 Acceptor 放进 `newConnections` 里的 SocketChannel 取出来，用 selector 注册到事件循环中。这步是“新连接真正进入事件驱动世界”的入口。

### 第二步：processNewResponses

从 `responseQueue` 里取 Handler 放好的响应。对 SendResponse，调用 `selector.send()` 把数据交给内核发送；对 NoOpResponse、CloseResponse 等做对应处理。它就是三层模型的“返程”起点。

### 第三步：poll

`poll()` 是等待就绪事件的地方。这里有一个值得注意的智能超时：

```text
val pollTimeout = if (newConnections.isEmpty) 300 else 0
```

- 如果没有待注册的新连接，poll 可以等最多 300ms，减少空转 CPU；
- 如果有新连接要注册，poll 立即返回（timeout=0），优先让新连接尽早进入事件循环，避免连接建立延迟。

同时，Acceptor 把连接放入 `newConnections` 后还会调用一次 `wakeup()`，主动唤醒正在 poll 的 selector。所以“新连接尽快被处理”至少依赖两条路径：一项是 poll 超时归零，另一项是 `wakeup()` 的直接唤醒。

如果你把 poll 超时当成固定值，主链会先在哪出问题？你会漏掉“有新连接时要把超时归零，否则新连接要等最多 300ms 才被注册”。这个细节是连接建立延迟的关键。

### 第四步：processCompletedReceives

poll 之后，selector 可能已经完整接收了某些请求的字节流。Processor 会：

1. 解析请求头；
2. 构造 `RequestContext`；
3. 创建一个 `RequestChannel.Request`；
4. 调用 `requestChannel.sendRequest(req)` 把它放进 Handler 要读的队列。

这一步是三层模型里“入站请求从网络层流向业务层”的转换点。

注意这里还会 `selector.mute(connectionId)`：连接被暂时静音，限制这个连接上的 inflight 请求数量，配合有界的 requestQueue 形成反压，避免客户端在上一请求还没处理完时就源源不断地下发新请求。unmute 不是“Handler 处理完就立刻生效”，而是要等响应真正发出、通道状态从 `MUTED_AND_RESPONSE_PENDING` 回到 `MUTED`，并且没有处于 throttling 状态时，才会尝试 unmute。

### 第五步：processCompletedSends

`poll` 之后，selector 也可能报告某些响应已经发送完成。Processor 从 `inflightResponses`（按 connectionId 索引的 map）找到对应的响应，执行 onComplete 回调并更新指标，然后尝试 unmute 连接。

### 第六步：processDisconnected

处理 selector 报告的断开连接，清理 `inflightResponses`（按 connectionId 索引的 `Map[String, RequestChannel.Response]`）、更新连接配额、通知 disconnect listeners。

### 第七步：closeExcessConnections

检查当前连接数是否超过 `maxConnectionsPerIp` 或 `maxConnections` 等配额限制，如果超出就关闭多余的连接。这一步不是每轮都会触发，只在连接数达到上限时才工作。

这七步合在一起，就是 Processor 每一轮循环的完整工作。它并不是简单的“收一个发一个”，而是围绕 NIO 事件把完整生命周期管起来。

## 第五层：RequestChannel 是 Processor 与 Handler 之间唯一的“握手”

三层模型中，Processor 和 Handler 不能直接互调，否则就耦合死了。它们通过 `RequestChannel` 传递。

`RequestChannel` 的核心是**两个入站队列**：`requestQueue` 与 `callbackQueue`（都是 `ArrayBlockingQueue[BaseRequest]`，容量同为 `queued.max.requests`，默认 500），外加每个 Processor 一个 `responseQueue`（`LinkedBlockingDeque`）。普通请求走 `requestQueue`，需要异步 completion 的回调请求（`CallbackRequest`）走 `callbackQueue`，两者都是 Handler 侧消费的入站通道。

- **Handler 读** requestQueue / callbackQueue，取出请求做业务；
- **Handler 写** 对应 Processor 的 responseQueue；
- **Processor 写** requestQueue；**Processor 读**自己的 responseQueue。

```text
Processor
  → requestChannel.sendRequest(req)
    → requestQueue.put(req)        // Handler 从这里取

Handler 线程
  → requestChannel.receiveRequest()
    → 从 requestQueue.take()

Handler 处理完成
  → requestChannel.sendResponse(resp)
    → 分发到对应 Processor 的 responseQueue

Processor
  → 自己的 responseQueue.poll()
    → selector.send(...)
```

这样，三层之间共享的不是内存里的共享可变状态，而是标准的生产者-消费者队列。这正是三层线程能独立扩容的原因：想加网络 IO 就加 Processor，想加业务处理就加 Handler。

## 第六层：Handler 线程池——从这里只剩业务处理

Handler 线程由 `KafkaRequestHandlerPool` 管理，数量由 `num.io.threads` 控制。每个 Handler 循环：

```text
val request = requestChannel.receiveRequest()
  → KafkaApis.handle(request)
    → 路由到对应 API 的 handler 方法
      → requestChannel.sendResponse(...)
```

Handler 不再关心 socket、selector、连接状态，它只面对已经解析好的 `RequestChannel.Request` 对象。请求中已经带有 `RequestContext`、已解析的 header、buffer 等。它处理完业务后，把结果封装成 Response，放回 RequestChannel。

这也解释了为什么 Kafka-10 的 Purgatory 能等条件满足：请求可能在 Purgatory 挂很久，等条件达成后再由 Handler 的完成回调产生响应并送回 ResponseChannel。到这一步，请求才真正走向“返程”。

如果 Handler 自己直接去写 socket，主链会先在哪失败？它和 Processor 的 Selector 线程并发操作同一连接，会产生线程安全问题，且网络发送逻辑被分散到多个业务线程。Kafka 把响应统一丢回 responseQueue，正是为了让网络发送始终只在 Processor 线程内发生。

## 收网：三层模型 = 三个不同职守的线程层 + 两个方向的队列

把整篇压成一句话：Kafka 的 SocketServer 用三层线程拆开网络 IO 和业务处理——Acceptor 每 listener 一个，只 accept 并把新连接 round-robin 分给 Processor；Processor 每 listener 多个、各自持有一个 Selector，在单线程内完成新连接注册、poll 读写、请求解析入 RequestChannel、响应从 responseQueue 写出；Handler 线程池从 RequestChannel 取请求做业务，再把响应放回对应 Processor 的 responseQueue。

```text
客户端连接
  → Acceptor（只 accept + round-robin）
    → Processor（NIO 读写 + 解析）
      → RequestChannel.requestQueue
        → Handler（KafkaApis 业务处理）
          → RequestChannel.sendResponse
            → Processor.responseQueue
              → selector 写回客户端
```

到这里，主线只发生了六件事。

第一，每个 listener 有一套独立的 acceptor-processor 集合。

第二，Acceptor 只做三件事：accept 新连接、做连接准入控制、round-robin 分给 Processor。

第三，Processor 是网络 IO 核心，持有 Selector、newConnections、responseQueue。

第四，Processor 主循环有七个明确步骤，poll 超时会根据是否有新连接动态切换。

第五，RequestChannel 用 requestQueue + 每-Processor responseQueue 解耦两层。

第六，Handler 线程池只做业务，响应统一放回 responseQueue。

**本篇的一句话困惑**：一个请求从客户端到 broker，到底经过了几层线程？

**本篇的一句话顿悟**：经过三层——Acceptor 只放行连接，Processor 只做 NIO 读写与解析，Handler 只做业务；中间用 RequestChannel 的 requestQueue 与每个 Processor 的 responseQueue 解耦。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“Acceptor 负责处理请求。”** Acceptor 只 accept 新连接、做连接准入控制并 round-robin 分发给 Processor。
2. **“Processor 只有一个。”** 每个 listener 通常有多个 Processor。
3. **“Handler 直接回复客户端。”** Handler 把响应放回对应 Processor 的 responseQueue，由 Processor 写回。
4. **“RequestChannel 只有一个队列。”** 它有一个 requestQueue，外加每个 Processor 各自的 responseQueue。
5. **“Processor 的 poll 超时是固定的。”** 有新连接时 0ms，无新连接时 300ms。

### 关键证据清单

- `core/src/main/scala/kafka/network/SocketServer.scala:62`：SocketServer 类注释，三层线程模型。
- `core/src/main/scala/kafka/network/SocketServer.scala:474`：Acceptor 抽象类。
- `core/src/main/scala/kafka/network/SocketServer.scala:591`：Acceptor.run() 主循环（acceptNewConnections）。
- `core/src/main/scala/kafka/network/SocketServer.scala:687`：连接配额累加（too many connections / throttled 准入）。
- `core/src/main/scala/kafka/network/SocketServer.scala:653`：acceptNewConnections 轮询尝试 + 兜底阻塞分配。
- `core/src/main/scala/kafka/network/SocketServer.scala:816`：Processor 类。
- `core/src/main/scala/kafka/network/SocketServer.scala:844`：newConnections 与 responseQueue。
- `core/src/main/scala/kafka/network/SocketServer.scala:906`：Processor.run() 主循环。
- `core/src/main/scala/kafka/network/SocketServer.scala:1009`：poll 超时智能切换。
- `core/src/main/scala/kafka/network/SocketServer.scala:1019`：processCompletedReceives 解析请求入 RequestChannel。
- `core/src/main/scala/kafka/network/SocketServer.scala:1121`：closeExcessConnections 关闭超出配额的低优先级连接。
- `core/src/main/scala/kafka/network/RequestChannel.scala:351`：requestQueue。
- `core/src/main/scala/kafka/network/RequestChannel.scala:353`：callbackQueue。
- `core/src/main/scala/kafka/server/KafkaConfig.scala:304`：queuedMaxRequests（默认 500）。
- `core/src/main/scala/kafka/network/RequestChannel.scala:378`：sendRequest 入队。
- `core/src/main/scala/kafka/network/RequestChannel.scala:467`：receiveRequest 出队。
- `core/src/main/scala/kafka/server/KafkaRequestHandler.scala:103`：Handler 线程 run 循环。
- `clients/src/main/java/org/apache/kafka/common/network/KafkaChannel.java:71`：ChannelMuteState / ChannelMuteEvent 状态机。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦三层线程模型主链，不展开 RequestChannel 背压细节、quota/throttling、KafkaApis 内部路由。
- 本篇把 Acceptor/Processor/Handler 作为三个职守清晰的线程层，不把它们的内部 metric 或监控细节当作主线。
- 每个 listener 独立一套 acceptor-processor，控制面与数据面并存。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-1`（总图，明确三层在整体架构中的位置）。
- 后续桥接：下一篇可进入 K-1 域第 2 篇（RequestChannel 与队列背压），解释队列容量、拒绝策略、限流与背压如何影响三层吞吐。