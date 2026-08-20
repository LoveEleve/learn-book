# K-8 网络层 — Pass 1 探索笔记 (扫轮廓)

> 🟡 B | 依赖: K-6 ✅ (KafkaApis) | 对照: [[ch3-selector-01]] (NioEventLoop)
> 源码: core/src/main/scala/kafka/network/ (SocketServer 1715 + RequestChannel 503) — Scala 未索引 [标注: grep 降级]
> 测试地图: core/src/test/scala/unit/kafka/network/ (SocketServerTest/ProcessorTest/RequestChannelTest)

## 继承树/调用图

```
SocketServer (SocketServer.scala:72)
├── Acceptor (SocketServer.scala:L474) — 每 listener 1 线程: accept → newConnections.offer
├── Processor (SocketServer.scala:L816) — num.network.threads=3: 六步主循环 (SocketServer.scala:L906-918)
│     └── selector.poll (SocketServer.scala:L1010) — 智能超时 (新连接 0ms / 空闲 300ms)
└── KafkaRequestHandlerPool — num.io.threads=8: 请求 → KafkaApis (K-6 衔接)
RequestChannel (RequestChannel.scala:344) — requestQueue + responseQueue
```

## 基本元素分解 (原则二)

1. **Acceptor** — accept 新连接 → newConnections (ArrayBlockingQueue) (SocketServer.scala:L474)
2. **Processor 六步** — configureNewConnections (SocketServer.scala:L911) → processNewResponses (SocketServer.scala:L913) → processCompletedReceives (SocketServer.scala:L915) → processCompletedSends (SocketServer.scala:L916) → processDisconnected (SocketServer.scala:L917) → closeExcessConnections (SocketServer.scala:L918)
3. **智能 poll** — selector.poll(pollTimeout) (SocketServer.scala:L1010): 新连接 0ms/空闲 300ms
4. **RequestChannel** — 请求队列 (SocketServer.scala:L344): Processor 放 / Handler 取
5. **Handler 池** — KafkaRequestHandlerPool: 处理 → KafkaApis

## 标记问题 (≥5)

1. **Q1: 三层线程模型?** — Acceptor/Processor/Handler 分工 (SocketServer.scala:L72,474,816)
2. **Q2: Processor 六步循环?** — SocketServer.scala:L906-918
3. **Q3: 智能 poll 超时?** — 新连接 0ms/空闲 300ms (SocketServer.scala:L1010)
4. **Q4: RequestChannel?** — 请求队列 (SocketServer.scala:L344)
5. **Q5: 与 KafkaApis 衔接?** — Handler → KafkaApis (K-6)
6. **Q6: 与 Netty 对照?** — ch3-selector-01 (NioEventLoop)

## 已读测试 (2 个)

- `SocketServerTest`: 三层/连接
- `ProcessorTest`: 六步循环

## 完成检查

- [x] 继承树/调用图
- [x] 基本元素分解 (5 元素)
- [x] 6 个标记问题
- [x] 已读 2 个测试

## 跨域发现

- 来源: K-8 Pass 1 — Processor 六步循环与 Netty NioEventLoop (ch2) 同构 (selector 驱动)
- 发现: Acceptor 单一职责 (只 accept) vs Netty boss/worker 分组 — Kafka 简化 (无线程池分组)
- 已对照验证: K-6 KafkaApis (请求处理终点)
