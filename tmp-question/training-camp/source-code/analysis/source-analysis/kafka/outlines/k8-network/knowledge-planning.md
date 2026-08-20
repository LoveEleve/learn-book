# K-8 网络层 — 知识规划 (00 §10: 逐源提取→聚合→分类→聚类)

> 2026-08-15 | 源码: core/src/main/scala/kafka/network/ (SocketServer 1715 + RequestChannel 503) — Scala 未索引 [标注]

## 01 逐源提取

| 源文件 | 机制点 |
|---|---|
| SocketServer.java | ①class (SocketServer.scala:L72) ②Acceptor (SocketServer.scala:L474) ③Processor (SocketServer.scala:L816) ④run 六步主循环 (SocketServer.scala:L906-918) ⑤selector.poll (SocketServer.scala:L1010) ⑥processCompletedReceives (SocketServer.scala:L1019) ⑦KafkaRequestHandlerPool |
| RequestChannel.java | ①class (SocketServer.scala:L344) ②请求队列 (queueSize) ③responseQueue |

## 02 聚合 (P1/P2/P3)

| 聚合机制 | 来源 | 分级 |
|---|---|---|
| Acceptor→Processor→Handler 三层 | SocketServer | P1 |
| Processor 六步主循环 | SocketServer.scala:L906-918 | P1 |
| 智能 poll 超时 (0/300ms) | SocketServer.scala:L1010 | P1 |
| RequestChannel 队列 | RequestChannel SocketServer.scala:L344 | P1 |
| 与 KafkaApis 衔接 | KafkaRequestHandlerPool | P2 |

## 03 深度分类

- 🔴: 三层线程模型 + 六步循环 (网络层核心)
- 🟡: 队列/超时策略
- 🟢: 配置 (num.network.threads=3)

## 04 聚类 (教学顺序)

```
Acceptor (accept → newConnections 队列)
  → Processor (六步循环: 配置连接→响应→接收→发送→断开→超额)
  → RequestChannel (请求队列)
  → KafkaRequestHandlerPool (Handler 处理 → KafkaApis → K-6 衔接)
```

**拆篇建议**: 2 篇 (🟡 B, 6 闭环)
- 01: 三层线程模型 (Acceptor/Processor/Handler)
- 02: Processor 循环与衔接 (六步/智能超时/KafkaApis/Netty 对照)
