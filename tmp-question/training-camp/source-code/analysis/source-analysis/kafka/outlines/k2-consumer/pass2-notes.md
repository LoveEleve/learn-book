# K-2 闭环笔记 Q1-Q8: 双模型/FetchBuffer/rebalance/offset/拉取/衔接/对照

## Q1: poll 双模型?

假设: KafkaConsumer 门面 + Async (KIP-848) / Classic (旧协议) 双实现。

验证过程:
- KafkaConsumer (KafkaConsumer.java:532) 是**门面**: delegate (KafkaConsumer.java:L536) + CREATOR (KafkaConsumer.java:L534, ConsumerDelegateCreator)
- ConsumerDelegateCreator.create (ConsumerDelegateCreator.java:L57-70): group.protocol=consumer → **AsyncKafkaConsumer** (AsyncKafkaConsumer.java:L64) / 否则 → **ClassicKafkaConsumer** (ClassicKafkaConsumer.java:L66)
- ClassicKafkaConsumer (ClassicKafkaConsumer.java:L116): poll → pollForFetches (ClassicKafkaConsumer.java:L690) — 同步拉取
- AsyncKafkaConsumer (AsyncKafkaConsumer.java:L172): FetchBuffer (AsyncKafkaConsumer.java:L304) + ConsumerNetworkThread (AsyncKafkaConsumer.java:L385) — 异步事件循环
- 规划断言 "双模型并存" ✅; 4.1.2 默认 = Async (KIP-848 新协议)

代码类型: Interface (双实现)

结论: **poll 双模型 = KafkaConsumer 门面 (KafkaConsumer.java:L532, delegate KafkaConsumer.java:L536) + AsyncKafkaConsumer (AsyncKafkaConsumer.java:L172, 默认 KIP-848) / ClassicKafkaConsumer (ClassicKafkaConsumer.java:L116, 旧协议); 分流在 ConsumerDelegateCreator (ConsumerDelegateCreator.java:L64-66)**。KafkaConsumer.java:532-536 + ConsumerDelegateCreator.java:57-70

## Q2: FetchBuffer 跨线程怎么工作?

假设: 网络线程填缓冲, 应用线程 poll 消费。

验证过程:
- AsyncKafkaConsumer 字段: fetchBuffer (AsyncKafkaConsumer.java:L304) + fetchCollector (AsyncKafkaConsumer.java:L305)
- 注释 (AsyncKafkaConsumer.java:L299-300): "thread-safe FetchBuffer for the results that are populated in the ConsumerNetworkThread"
- 构造: fetchBuffer 共享 (AsyncKafkaConsumer.java:L428-429) + fetchCollector 应用线程专用 (AsyncKafkaConsumer.java:L486-487)
- ConsumerNetworkThread (AsyncKafkaConsumer.java:L385): 事件循环收发 (K-12 会话拉取)

代码类型: Implementation (跨线程缓冲)

结论: **FetchBuffer = 网络线程填 (ConsumerNetworkThread AsyncKafkaConsumer.java:L385) / 应用线程 poll 读 (FetchCollector AsyncKafkaConsumer.java:L305, 仅应用线程 AsyncKafkaConsumer.java:L486) — 跨线程无锁共享缓冲 (AsyncKafkaConsumer.java:299-305,428-429)**。AsyncKafkaConsumer.java:299-305

## Q3: rebalance 流程?

假设: ConsumerCoordinator 加入组/分区分配/心跳。

验证过程:
- ConsumerCoordinator (ConsumerCoordinator.java, 1705 行): rebalance 客户端协议
- JoinGroup → SyncGroup → Heartbeat → LeaveGroup (K-6 服务端 GroupCoordinator 同协议两端)
- 4.x: KIP-848 ModernGroup (增量 rebalance) / ClassicGroup (旧四步)
- 触发: subscribe → poll → ensureActiveGroup (K-6 篇展开)

代码类型: Glue (协调协议)

结论: **rebalance = ConsumerCoordinator 客户端协议 (JoinGroup/SyncGroup/Heartbeat, 1705 行) ↔ K-6 服务端 GroupCoordinator — 同一协议两端 (K-6 交付后回补双链)**。ConsumerCoordinator.java

## Q4: offset 管理?

假设: position (消费位置) + commit (检查点) + auto.offset.reset。

验证过程:
- ClassicKafkaConsumer.updateFetchPositions (ClassicKafkaConsumer.java:L1188) — poll 时同步位置
- commit: commitSync/commitAsync → __consumer_offsets compact topic (K-6 衔接)
- auto.offset.reset 三态 (ConsumerConfig.java:175-179): earliest/latest/none — 规划断言缺 none
- 设计文档 §Consumer Position: "position is just a single integer... periodically checkpointed" — 单整数 + 检查点 (vs broker 记录 ack)

代码类型: Implementation (位点管理)

结论: **offset = position 单整数 (设计文档 §Consumer Position) + updateFetchPositions (ClassicKafkaConsumer.java:1188) + commit 检查点 (K-6 __consumer_offsets) + auto.offset.reset 三态**。ClassicKafkaConsumer.java:1188 + docs/design/design.md §Consumer Position

## Q5: Fetcher 拉取链?

假设: Fetcher 走 K-12 会话增量拉取。

验证过程:
- Fetcher (Fetcher.java:59, extends AbstractFetch) — 拉取门面
- 实际拉取: FetchRequestManager (FetchRequestManager.java:L183) + FetchCollector (385 行)
- 会话: FetchSessionHandler (K-12 已交付) — 客户端会话侧
- 链路: poll → pollForFetches → sendFetches → K-12 会话 → 服务端 FetchSession 增量

代码类型: Glue (拉取链)

结论: **拉取链 = poll → pollForFetches (ClassicKafkaConsumer.java:L690) → FetchRequestManager (FetchRequestManager.java:L183) → K-12 FetchSessionHandler 会话增量 → 服务端 FetchSession — K-2 消费拉取是 K-12 会话的消费者 (衔接闭环)**。Fetcher.java:59 + FetchRequestManager.java

## Q6: 与 K-6 Group 衔接?

假设: 客户端 rebalance 协议与服务端 GroupCoordinator 是两端。

验证过程:
- 客户端: ConsumerCoordinator (rebalance/commit/heartbeat)
- 服务端: K-6 GroupCoordinator (ClassicGroup/ModernGroup) — 未交付, 交付后回补
- 4.x 双协议: KIP-848 ModernGroup (增量) vs ClassicGroup (四步)
- 衔接点: JoinGroup 请求 → 服务端组状态机 → 分区分配结果回客户端

代码类型: 衔接分析

结论: **K-2 客户端协议 ↔ K-6 服务端协调: JoinGroup/SyncGroup/Heartbeat 两端 (K-6 交付后回补双链与细节)**。ConsumerCoordinator.java (客户端) + K-6 (服务端)

## Q7: 与 K-12 FetchSession 衔接?

假设: 消费拉取复用会话增量机制。

验证过程:
- FetchSessionHandler (K-12 交付): sessionId/nextMetadata (FetchSessionHandler.java:76-77)
- AsyncKafkaConsumer: ConsumerNetworkThread 发 fetch (携带会话) → FetchBuffer 收
- 服务端: FetchSession 缓存 (K-12) — 增量响应
- 闭环: K-2 poll → K-12 会话 → 服务端增量 → FetchBuffer → 应用线程

代码类型: 衔接分析

结论: **K-2 消费拉取 = K-12 会话协议的客户端消费面: ConsumerNetworkThread 经 FetchSessionHandler (K-12) 发增量 fetch, FetchBuffer 收 (AsyncKafkaConsumer.java:304) — 双缓存面闭环 (客户端缓冲 + 服务端会话)**。AsyncKafkaConsumer.java:304 + FetchSessionHandler.java:60

## Q8: 与 RocketMQ 消费对照?

假设: RM-8 push 模型 vs Kafka pull。

验证过程:
- RocketMQ: DefaultMQPushConsumer (broker 推送, RM-8)
- Kafka: pull (消费者主动拉, 设计文档 §Push vs pull 论证)
- 对照维度: 模型 (push vs pull) / 位点 (RM 消费进度 vs Kafka offset) / 重平衡 (RM 无组协调 vs Kafka rebalance)
- 面试记忆点: "Kafka 为什么 pull? — 消费速率自控 (防 push 淹没) + 自然批处理"

代码类型: 对照分析

结论: **Kafka pull (速率自控+自然批, 设计文档论证) vs RocketMQ push (broker 推送, RM-8) — 消费模型两范式**。docs/design/design.md §Push vs pull + RM-8

跨域关联: K-6 (Group) / K-12 (FetchSession) / K-3/K-4 (服务端) / RM-8 (对照)
