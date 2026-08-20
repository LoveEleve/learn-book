# K-2 Consumer — 知识规划 (00 §10: 逐源提取→聚合→分类→聚类)

> 2026-08-15 | 源码: clients/consumer/ (KafkaConsumer 1870 + AsyncKafkaConsumer + ConsumerCoordinator 1705 + FetchBuffer/FetchCollector/ConsumerNetworkThread) — Java 端全索引 (189 文件)

## 01 逐源提取

| 源文件 | 机制点 |
|---|---|
| KafkaConsumer.java | ①Classic poll (KafkaConsumer.java:L532 类) ②poll→ensureActiveGroup→Fetcher ③position/commit ④ConsumerCoordinator ⑤ConsumerNetworkThread |
| AsyncKafkaConsumer.java | ①新异步模型 (AsyncKafkaConsumer.java:L172) ②FetchBuffer 跨线程缓冲 (AsyncKafkaConsumer.java:L304-305) ③FetchCollector 应用线程收集 (AsyncKafkaConsumer.java:L305) ④CompletableFuture API |
| ConsumerCoordinator.java | ①rebalance (JoinGroup/SyncGroup) ②offset commit ③heartbeat |
| FetchBuffer/FetchCollector | ①网络线程填充/应用线程消费 ②记录解码 |
| ConsumerNetworkThread.java | ①事件循环 ②发 fetch 收数据 (K-12 会话衔接) |

## 02 聚合 (P1/P2/P3)

| 聚合机制 | 来源 | 分级 |
|---|---|---|
| poll 双模型 (Classic vs Async) | KafkaConsumer + AsyncKafkaConsumer | P1 |
| FetchBuffer 跨线程模型 | FetchBuffer + ConsumerNetworkThread | P1 |
| rebalance 协议 | ConsumerCoordinator (K-6 衔接) | P1 |
| offset 管理 (position/commit) | KafkaConsumer + Coordinator | P1 |
| Fetcher 拉取链 | Fetcher + K-12 会话 | P2 |
| heartbeat | ConsumerCoordinator | P2 |
| 对照 (RocketMQ 消费) | RM-8 push | P2 |

## 03 深度分类

- 🔴: poll 模型 + FetchBuffer 跨线程 + rebalance + offset (Consumer 定义特征)
- 🟡: Fetcher 拉取 / heartbeat
- 🟢: 配置 (auto.offset.reset 等)

## 04 聚类 (教学顺序)

```
poll() → 双模型 (Classic 同步 vs Async 异步)
  → FetchBuffer (网络线程↔应用线程跨线程缓冲)
  → ConsumerCoordinator (rebalance/offset commit)
  → Fetcher (K-12 FetchSession 会话拉取)
  → 与 K-6 Group 衔接 (rebalance 协议在服务端)
```

**拆篇建议**: 3 篇 (🔴 A, 8 闭环)
- 01: poll 双模型 (Classic vs Async + FetchBuffer)
- 02: rebalance 与 offset (ConsumerCoordinator)
- 03: 拉取链与对照 (Fetcher + K-12 会话 + RM 对照)
