# Kafka-13 重写规划

> 题目：一个请求怎么真正进到 Broker——SocketServer 的 Acceptor→Processor→Handler 三层线程模型
> 状态：K-1 域第 1 篇，按“Acceptor→Processor→Handler 主链”展开
> 目标：解释 Kafka broker 在收到客户端的 TCP 连接后，请求怎样从网卡经过三层线程到达业务处理层（KafkaApis），以及响应怎样从业务层返回网卡。重点回答：Acceptor 为什么只做 accept、Processor 的主循环为什么是 `configureNewConnections → processNewResponses → poll → processCompletedReceives → processCompletedSends → processDisconnected`、Handler 为什么通过 RequestChannel 与 Processor 的解耦模型。

## 1. 读者困惑

- 客户端发一个 fetch 请求，broker 从内核到应用层走过了哪些线程？
- 为什么说 Acceptor 只做 accept，不做任何业务处理？
- Processor 的 `poll()` 超时为什么有时 0ms 有时 300ms？
- 为什么 Handler 不直接回复客户端，而是要把响应放回 RequestChannel？
- `RequestChannel` 的两个队列分别是谁读谁写？
- 一个请求从入站到出站，经历了哪些队列和线程切换？

## 2. 一句话顿悟

**Kafka 的 SocketServer 用三层线程把“网络 IO”和“业务处理”彻底拆开：Acceptor 只负责 accept 新连接、round-robin 分给 Processor；Processor 各自持有一个 Selector，在单线程内完成连接注册、读写、协议解析和响应发送，完成后把解析好的请求放进 RequestChannel；Handler 线程池从 RequestChannel 取请求处理，再把响应放回 Processor 的 responseQueue，由 Processor 写回客户端。**

## 3. 五要素卡片

### 读者问题

一个 produce 请求走了 Acceptor、Processor、Handler 三个线程，为什么不是直接在 Handler 里连接读写？

### 入口

- `SocketServer`：管理所有 listener 的 acceptor 与 processor
- `Acceptor`：每个 listener 一个，accept 新连接
- `Processor`：每个 listener 多个，NIO selector 单线程处理读写
- `RequestChannel`：Processor 与 Handler 之间的队列通道
- `KafkaRequestHandlerPool`：Handler 线程池
- `KafkaApis`：请求路由到具体 handler 方法

### 状态核心

- Acceptor：`NSelector` + `ServerSocketChannel` 注册 OP_ACCEPT
- Processor：`newConnections`(ArrayBlockingQueue) + `Selector` + `responseQueue`(LinkedBlockingDeque)
- Processor 主循环：`configureNewConnections → processNewResponses → poll → processCompletedReceives → processCompletedSends → processDisconnected`
- RequestChannel：`requestQueue`(ArrayBlockingQueue) + `responseQueue`(per processor, LinkedBlockingDeque)

### 失败路径

- Acceptor 自己处理请求 → 新连接 accept 被阻塞，拒绝服务
- Handler 直接写 socket → 与 Processor 的 selector 竞争，线程不安全
- Processor 只 poll 不处理超时 → 新连接一直不被注册，连接建立延迟
- 没有背压 → 请求队列满时拒绝策略不当，OOM 或请求丢失

### 连接点

- 前文 `Kafka-7`：Controller 的请求也要经过这套三层模型。
- 前文 `Kafka-10`：Purgatory 中等待的请求最终通过 Handler 完成回调，再通过三层的反向路径返回客户端。
- 后文：K-1 域第 2 篇（RequestChannel 与队列背压）。

## 4. 总图

```text
客户端 TCP 连接
  → Acceptor（1 个/ listener）
    → accept 新 SocketChannel
      → round-robin 分配给 Processor
        → Processor.newConnections 队列

Processor 主循环
  → configureNewConnections（注册新连接到 selector）
    → processNewResponses（从 responseQueue 取响应写入 selector.send）
      → selector.poll（读写事件）
        → processCompletedReceives（解析请求头，放入 RequestChannel.requestQueue）
          → processCompletedSends（发送完成后的清理）
            → processDisconnected（断开连接处理）

Handler 线程池
  → RequestChannel.requestQueue.take()
    → KafkaApis.handle()
      → RequestChannel.sendResponse(response)
        → Processor.responseQueue
```

## 5. 关键边界

- 本篇只讲三层线程模型，不展开所有 API 错误码与请求类型。
- 本篇不把 Processor 与 Selector 混成同一个概念：Processor 是线程+队列的管理者，Selector 是 NIO 事件驱动核心。
- 不把 RequestChannel 写成“请求的存储层”：它只是 Processor 与 Handler 之间的队列通道。
- 后文第 2 篇再展开背压与限流。

## 6. 失败方案推演

1. **Acceptor 直接处理请求**：accept 慢、新连接溢出。
2. **Handler 直接读写 socket**：与 Processor 的 selector 线程冲突。
3. **Processor 一直 poll 不处理新连接**：连接建立延迟，guest 感觉到连接慢。
4. **没有 RequestChannel，Handler 直接回调**：没有了三层解耦，线程模型退化。

## 7. 误解清单

- “Acceptor 负责处理请求”：Acceptor 只 accept 新连接。
- “Processor 只有一个”：每个 listener 通常有多个 Processor。
- “Handler 直接回复客户端”：Handler 把响应放到 Processor 的 responseQueue。
- “RequestChannel 只有一个队列”：它有一个 requestQueue 和多个 responseQueue（每个 processor 一个）。
- “Processor 的 poll 超时固定”：有新连接时 0ms，无新连接时 300ms。

## 8. 证据清单

- `core/src/main/scala/kafka/network/SocketServer.scala:62`：SocketServer 类注释，三层线程模型。
- `core/src/main/scala/kafka/network/SocketServer.scala:474`：Acceptor 抽象类。
- `core/src/main/scala/kafka/network/SocketServer.scala:591`：Acceptor.run() 主循环。
- `core/src/main/scala/kafka/network/SocketServer.scala:816`：Processor 类。
- `core/src/main/scala/kafka/network/SocketServer.scala:844`：newConnections 与 responseQueue。
- `core/src/main/scala/kafka/network/SocketServer.scala:906`：Processor.run() 主循环。
- `core/src/main/scala/kafka/network/SocketServer.scala:1009`：poll 超时智能切换。
- `core/src/main/scala/kafka/network/SocketServer.scala:1019`：processCompletedReceives 解析请求入 RequestChannel。
- `core/src/main/scala/kafka/network/RequestChannel.scala:351`：requestQueue。
- `core/src/main/scala/kafka/network/RequestChannel.scala:378`：sendRequest 入队。
- `core/src/main/scala/kafka/network/RequestChannel.scala:467`：receiveRequest 出队。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦三层线程模型主链，不展开 RequestChannel 背压细节、quota/throttling、KafkaApis 路由。
- 目标正文：7000~11000 字；核心拆解层覆盖 Acceptor、Processor 主循环、RequestChannel、Handler 线程池。

## 10. 本轮重写主线

1. 从“一个请求从客户端到 broker，经过几个线程”开场。
2. 否定“Acceptor 直接处理请求”和“Handler 直接读写 socket”两种直觉方案。
3. 解释 Acceptor 的 accept 与 round-robin。
4. 解释 Processor 主循环六步。
5. 解释 RequestChannel 的双队列结构。
6. 解释 Handler 线程池如何取请求、处理、放回响应。
7. 收网：三层模型把网络 IO 和业务处理彻底解耦，Acceptor 只放行连接，Processor 只做 IO 读写，Handler 只做业务处理。