# K-2 Consumer — 时空溯源 (2026-08-15)

> 🔴 A 域强制 | 方法: git log --diff-filter=A 找引入 commit + git log -1 日期实证

## 断代链

| 时间 | commit | 里程碑 | 演进 |
|---|---|---|---|
| 2014-05-20 | KAFKA-1328 | **KafkaConsumer.java 诞生** | "New consumer APIs" — Java 消费者 (取代旧 Scala 高层消费者) |
| 2015-10-21 | KAFKA-2464 | **ConsumerCoordinator** | 客户端侧 rebalance 分配 — JoinGroup/SyncGroup 协议 |
| 2022-11-09 | KAFKA-14363 | **KIP-848 启动** | 新 group-coordinator 模块 — 新消费者组协议 (增量 rebalance) |
| 2023-02-09 | KAFKA-14048 | **KIP-848 落盘** | __consumer_offsets 新记录格式 |
| 2023-09-16 | KAFKA-14274 | **FetchBuffer** | 异步消费者基础设施 (跨线程缓冲) |
| 2023-11-15 | KAFKA-15277 | **AsyncKafkaConsumer** | 异步消费者 — ConsumerNetworkThread 事件循环 + CompletableFuture |
| 2024-07-29 | KAFKA-17060 | **ClassicKafkaConsumer 更名** | LegacyKafkaConsumer → Classic (旧协议实现命名稳定) |

## 关键观察

1. **2014-2015 是经典消费者定型期**: Java API + 客户端侧 rebalance — 此后 8 年结构稳定 (poll 同步模型)
2. **2022-2024 KIP-848 是第二次架构升级**: 增量 rebalance (ModernGroup) + 异步消费者 (AsyncKafkaConsumer) — 与 K-1 Producer 2017/2019 语义升级同期错开
3. **门面化 (4.x)**: KafkaConsumer 变门面 (delegate KafkaConsumer.java:L536), Async 默认 / Classic 旧协议并存 — 向后兼容 + 渐进迁移
4. 对照 K-1 (Producer): Producer 2014 定型→2017 幂等→2019 粘性; Consumer 2014 定型→2015 rebalance→2023 异步化 — **两边都 2014 定型, 但 Consumer 的第二次升级比 Producer 晚 4-6 年** (消费模型复杂度更高)

## 对照锚点

- K-1 Producer: 门面化同构 (KafkaProducer 门面 vs KafkaConsumer 门面) — 双客户端对称设计
- RM-8: RocketMQ 消费 (push) — Kafka pull vs RM push 是 2014 定型的模型分叉, 至今未变
