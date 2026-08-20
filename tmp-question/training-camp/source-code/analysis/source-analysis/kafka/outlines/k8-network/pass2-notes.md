# K-8 闭环笔记 Q1-Q6: 三层模型/六步循环/智能超时/队列/衔接/对照

## Q1: 三层线程模型?

假设: Acceptor (accept) → Processor (NIO 事件) → Handler (业务处理)。

验证过程:
- SocketServer (SocketServer.scala:L72) + Acceptor (SocketServer.scala:L474) + Processor (SocketServer.scala:L816)
- Acceptor.run (SocketServer.scala:L591): acceptNewConnections 循环 → assignNewConnection round-robin (SocketServer.scala:L728)
- Processor: num.network.threads=3; Handler 池: num.io.threads=8
- 分工: Acceptor 只 accept (单一职责) / Processor selector 事件 / Handler 业务

代码类型: Implementation (线程架构)

结论: **三层 = Acceptor accept (SocketServer.scala:L591, round-robin SocketServer.scala:L728) → Processor NIO 事件 (SocketServer.scala:L816) → Handler 业务 — 每层独立线程, 队列解耦 (SocketServer.scala:72,474,816)**。SocketServer.scala:591-728

## Q2: Processor 六步循环?

假设: 每轮 poll 后处理六类事件。

验证过程:
- Processor.run (SocketServer.scala:L906-918): configureNewConnections (SocketServer.scala:L911) → processNewResponses (SocketServer.scala:L913) → processCompletedReceives (SocketServer.scala:L915) → processCompletedSends (SocketServer.scala:L916) → processDisconnected (SocketServer.scala:L917) → closeExcessConnections (SocketServer.scala:L918)
- processCompletedReceives (SocketServer.scala:L1019): 请求解析 → RequestChannel
- processNewResponses (SocketServer.scala:L950): 响应发送

代码类型: Implementation (事件循环)

结论: **Processor 六步 = 配置连接→新响应→接收→发送→断开→超额 (SocketServer.scala:L906-918), 与 Netty NioEventLoop 同构 (selector 驱动)**。SocketServer.scala:906-918

## Q3: 智能 poll 超时?

假设: 有新连接立即处理 (0ms), 空闲 300ms 省 CPU。

验证过程:
- poll (SocketServer.scala:L1010-1012): `val pollTimeout = if (newConnections.isEmpty) 300 else 0` — 新连接 0ms 立即 / 队列空 300ms
- 注释 (SocketServer.scala:L1009-1011): 新连接在队列时立即处理
- 规划断言 (R6: "智能 poll 超时: 有新连接时 timeout=0(立即处理), 队列空时 timeout=300ms(省 CPU)") ✅ 实证

代码类型: Implementation (超时策略)

结论: **智能超时 = newConnections 非空 0ms / 空 300ms (SocketServer.scala:L1011) — 新连接立即响应, 空闲省 CPU (规划 R6 断言实证)**。SocketServer.scala:1010-1012

## Q4: RequestChannel 队列?

假设: 请求队列解耦 Processor 与 Handler。

验证过程:
- RequestChannel (RequestChannel.scala:344, queueSize) — requestQueue
- Processor 放 (processCompletedReceives SocketServer.scala:L1019) / Handler 取
- responseQueue: Handler 响应 → Processor 发送
- 队列有界: queued.max.requests=500

代码类型: Implementation (队列)

结论: **RequestChannel = 请求/响应双队列 (SocketServer.scala:L344): Processor 放请求收响应, Handler 取请求放响应 — 有界 (500) 背压**。RequestChannel.scala:344

## Q5: 与 KafkaApis 衔接?

假设: Handler 从队列取请求 → KafkaApis 分发。

验证过程:
- KafkaRequestHandlerPool: num.io.threads=8 从 requestQueue 取
- KafkaApis.handle() → 路由 (handleProduceRequest/handleFetchRequest...)
- K-6 衔接: joinGroup/heartbeat 请求经此路径 (K-6 篇 1)
- 响应回 Processor responseQueue + wakeup

代码类型: Glue (请求链)

结论: **衔接 = Handler 池取请求 (num.io.threads=8) → KafkaApis.handle() 路由 → 响应回队列 — 网络层与业务层解耦点 (K-6 请求入口)**。SocketServer.scala (KafkaRequestHandlerPool) + KafkaApis

## Q6: 与 Netty 对照?

假设: Kafka 网络层与 Netty NioEventLoop 同构。

验证过程:
- Netty (ch3-selector-01): NioEventLoop = selector 驱动事件循环 + boss/worker 分组 (ch9-bootstrap-01)
- Kafka: Processor = selector 驱动六步循环 (SocketServer.scala:L906-918); Acceptor 简化 (无 boss/worker 分组, 单 accept 线程)
- 差异: Netty 用线程池分组 (boss 分配), Kafka 用 round-robin (SocketServer.scala:L728); Netty Reactor 多路复用 vs Kafka 每 listener 独立
- 面试点: "Kafka 是简化 Reactor (无 boss 池), Netty 是完整 Reactor (boss/worker)"

代码类型: 对照分析

结论: **Kafka Processor 六步循环 = Netty NioEventLoop 同构 (ch3-selector-01); 差异: Acceptor 简化 (round-robin vs boss 池 ch9-bootstrap-01)**。SocketServer.scala:906-918 + ch3-selector-01 对照

跨域关联: K-6 (KafkaApis) / ch3-selector-01 (Netty 对照) / K-1/K-2 (客户端网络)
