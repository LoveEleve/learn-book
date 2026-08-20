# K-8 网络层 篇 2/2 — 事件循环: Processor 六步与跨域对照

> 前置: [[K-8-network-01]] (三层模型) | 复用: — | 对照: [[ch3-selector-01]] (NioEventLoop 同构) [[ch9-bootstrap-01]] (boss/worker) | 引出: — (K-11/K-10/K-9 交付后补链)
> 🟡 B | 来源: SocketServer.scala:906-918,950,1010-1012,1019 + KafkaApis
> 定位: K-8 卷收尾 — 回答"Processor 每轮做什么? 和 Netty 差在哪?"

**读者处境**: 面试官问 "Processor 主循环干什么? poll 超时怎么定?" 你答 "处理事件" — 但再问 "六步是什么? 为什么 0/300ms? 和 Netty EventLoop 什么关系?" 你答不上来。这篇是事件循环的完整答案, 收束 K-8 域。

### 1. 问题引入 — 一秒钟的一轮

场景: Processor 线程每轮 poll 后做什么? **不做业务处理** (只做 IO, 业务交给 Handler 池)
- 六步主循环 (SocketServer.scala:906-918)
- 本篇问题: 六步 (Q2) / 智能超时 (Q3) / 对照 (Q6)

### 2. 六步主循环 — 事件处理流水线

场景: 每轮的具体步骤?
- run (SocketServer.scala:L906-918): configureNewConnections (SocketServer.scala:L911, NIO 注册+限流) → processNewResponses (SocketServer.scala:L913, 响应发送) → processCompletedReceives (SocketServer.scala:L915, 请求解析入队列) → processCompletedSends (SocketServer.scala:L916, 发送完成) → processDisconnected (SocketServer.scala:L917, 断开清理) → closeExcessConnections (SocketServer.scala:L918, 超额关闭)
- selector.poll (SocketServer.scala:L1010): 事件等待
- 顺序固定: 配置→响应→接收→发送→断开→超额

### 3. 智能 poll 超时 — 0 还是 300

场景: poll 等多久?
- poll (SocketServer.scala:L1010-1012): `if (newConnections.isEmpty) 300 else 0` — 新连接 0ms 立即处理, 空闲 300ms 省 CPU
- 权衡: 新连接延迟敏感 (立即) vs 空闲轮询浪费 (300ms 兜底)
- 规划 R6 断言实证

### 4. 与 Netty 对照 — 简化 Reactor

场景: 和 Netty 什么关系?
- Netty NioEventLoop (ch3-selector-01): selector 驱动事件循环 — 同构
- 差异: Acceptor round-robin (SocketServer.scala:L728) vs Netty boss 线程池分组 (ch9-bootstrap-01); 每 listener 独立 Processor vs Netty 共享 EventLoopGroup
- 面试点: "Kafka 是简化 Reactor (无 boss 池), Netty 是完整 Reactor"

### 核心悬念
"Kafka 为什么不用 Netty?" — 历史原因 (2012 前 Netty 未成熟) + 自研简化: 三层模型已满足 broker 场景 (连接数远小于客户端), 自研免依赖; 但网络栈思想与 Netty 同源 (Reactor 模式) — 面试对照: 同一模式的两种实现。

### 概念依赖链
Q2 六步 → Q3 智能超时 → Q6 对照 → (K-11/K-10/K-9 待补链)

### 源码锚点清单
- SocketServer.scala:906-918 (run 六步) / 911 (configureNewConnections) / 913 (processNewResponses) / 915 (processCompletedReceives) / 916 (processCompletedSends) / 917 (processDisconnected) / 918 (closeExcessConnections) / 950 (processNewResponses 定义) / 1010-1012 (智能 poll) / 1019 (processCompletedReceives 定义)
- KafkaApis (K-6 衔接)
