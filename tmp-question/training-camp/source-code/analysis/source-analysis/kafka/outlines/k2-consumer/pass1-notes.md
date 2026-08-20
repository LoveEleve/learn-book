# K-2 Consumer — Pass 1 探索笔记 (扫轮廓)

> 🔴 A | 依赖: K-3 ✅ / K-4 ✅ / K-12 ✅ (FetchSession) / K-7 ✅ | 对照: [[rm8-push]] (RocketMQ 消费) [[r9-replication]] (消费位点)
> 源码: clients/consumer/ (189 文件: KafkaConsumer 1870 + AsyncKafkaConsumer + ConsumerCoordinator 1705 + FetchBuffer/FetchCollector/ConsumerNetworkThread 385)
> 测试地图: clients/src/test/java/org/apache/kafka/clients/consumer/ (KafkaConsumerTest/AsyncKafkaConsumerTest/ConsumerCoordinatorTest/FetcherTest)

## 继承树/调用图

```
Consumer (接口)
├── KafkaConsumer (KafkaConsumer.java:532) — Classic 模型
│     └── poll → ensureActiveGroup → Fetcher → ConsumerCoordinator
└── AsyncKafkaConsumer (AsyncKafkaConsumer.java:172) — 新异步模型
      ├── ConsumerNetworkThread (AsyncKafkaConsumer.java:L385: 网络线程事件循环)
      ├── FetchBuffer (AsyncKafkaConsumer.java:L304-305: 跨线程缓冲, 网络线程填/应用线程读)
      └── FetchCollector (AsyncKafkaConsumer.java:L305: 应用线程收集解码)
ConsumerCoordinator (ConsumerCoordinator.java, 1705 行)
├── rebalance (JoinGroup/SyncGroup, K-6 服务端衔接)
├── offset commit (position 检查点)
└── heartbeat
Fetcher (Fetcher.java:59, extends AbstractFetch) — K-12 FetchSession 会话拉取
```

## 基本元素分解 (原则二)

1. **poll 双模型** — Classic (同步阻塞) vs Async (CompletableFuture 异步)
2. **FetchBuffer 跨线程** — 网络线程填数据, 应用线程 poll 消费 (AsyncKafkaConsumer.java:304-305)
3. **rebalance** — ConsumerCoordinator: 加入组/分区分配 (K-6)
4. **offset 管理** — position (消费位置) + commit (检查点)
5. **Fetcher 拉取** — K-12 FetchSession 会话 (增量)
6. **heartbeat** — 组活性

## 标记问题 (≥5)

1. **Q1: poll 双模型差异?** — Classic vs Async (KafkaConsumer.java:532 vs AsyncKafkaConsumer.java:172)
2. **Q2: FetchBuffer 跨线程怎么工作?** — ConsumerNetworkThread 填 / FetchCollector 收 (AsyncKafkaConsumer.java:L304-305,385)
3. **Q3: rebalance 流程?** — JoinGroup/SyncGroup (ConsumerCoordinator, K-6 衔接)
4. **Q4: offset 管理?** — position/commit/auto.offset.reset
5. **Q5: Fetcher 拉取链?** — K-12 会话衔接
6. **Q6: 与 K-6 Group 衔接?** — 客户端 rebalance 协议 vs 服务端 GroupCoordinator
7. **Q7: 与 K-12 FetchSession 衔接?** — 消费拉取走会话
8. **Q8: 与 RocketMQ 消费对照?** — RM-8 push 模型

## 已读测试 (2 个)

- `KafkaConsumerTest`: poll/rebalance/offset
- `ConsumerCoordinatorTest`: rebalance 协议

## 完成检查

- [x] 继承树/调用图
- [x] 基本元素分解 (6 元素)
- [x] 8 个标记问题
- [x] 已读 2 个测试

## 跨域发现

- 来源: K-2 Pass 1 — AsyncKafkaConsumer 的 FetchBuffer 是跨线程模型 (网络线程↔应用线程) — 与 K-12 服务端会话缓存构成"客户端-服务端双缓存"面
- 发现: rebalance 客户端协议 (ConsumerCoordinator) 与服务端 GroupCoordinator (K-6) 是同一协议两端
- 已对照验证: K-12 FetchSessionHandler (FetchSessionHandler.java:60) — 客户端会话侧
