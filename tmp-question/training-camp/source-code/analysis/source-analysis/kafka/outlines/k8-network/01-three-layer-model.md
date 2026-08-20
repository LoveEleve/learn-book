# K-8 网络层 篇 1/2 — 三层流水线: Acceptor/Processor/Handler

> 前置: [[K-6-group-01]] (请求入口) | 复用: — | 对照: [[ch3-selector-01]] (NioEventLoop 事件循环) | 引出: [[K-8-network-02]]
> 🟡 B | 来源: SocketServer.scala:72,474,591-728,816 + RequestChannel.scala:344
> 定位: K-8 卷开篇 — 回答"broker 网络层怎么组织线程?"

**读者处境**: 面试官问 "broker 怎么接请求? 几个线程?" 你答 "三层" — 但再问 "Acceptor 干什么? Processor 几个? Handler 池多大? 队列在哪?" 你答不上来。这篇是网络线程架构的完整答案。

### 1. 问题引入 — 连接进来的第一站

场景: 客户端连接到达 broker — 谁接? 谁读? 谁处理?
- SocketServer (SocketServer.scala:72) — 网络层中枢
- 本篇问题: 三层模型 (Q1) / 队列 (Q4)

### 2. 三层模型 — 各司其职

场景: 三层怎么分工?
- Acceptor (SocketServer.scala:L474): accept 新连接 → newConnections 队列 → **round-robin 分配** (assignNewConnection SocketServer.scala:L728)
- Processor (SocketServer.scala:L816): num.network.threads=3, NIO selector 事件处理
- KafkaRequestHandlerPool: num.io.threads=8, 业务处理
- 分工哲学: Acceptor 单一职责 (只 accept), Processor 只 IO, Handler 只业务 — 互不阻塞

### 3. 队列解耦 — RequestChannel

场景: Processor 和 Handler 怎么对接?
- RequestChannel (RequestChannel.scala:344): 请求/响应双队列 — requestQueue=ArrayBlockingQueue (RequestChannel.scala:L351) / responseQueue=LinkedBlockingDeque (SocketServer.scala:846)
- Processor 放请求 (processCompletedReceives) → Handler 取 → 响应回队列 + wakeup (SocketServer.scala:742 nioSelector.wakeup 唤醒阻塞 poll)
- 有界 (queued.max.requests=500, SocketServerConfigs.java:146): 背压保护
- 与 K-6 衔接: joinGroup/heartbeat 请求经此入 KafkaApis

### 核心悬念
"为什么 Acceptor 不做 NIO?" — Acceptor 只 accept (阻塞 IO) 是刻意的: accept 频率低, 单线程足够; NIO 事件处理交给 Processor 池 (3 线程) — 连接建立与数据读写分离, 各自独立扩缩 (对比 Netty boss/worker 分组 ch9-bootstrap-01, 但 Kafka 无 boss 池)。

### 概念依赖链
Q1 三层 → Q4 队列 → (02 篇: 循环+衔接)

### 源码锚点清单
- SocketServer.scala:72 (类) / 474 (Acceptor) / 591 (Acceptor.run) / 728 (assignNewConnection round-robin) / 816 (Processor)
- RequestChannel.scala:344 (类, queueSize)
