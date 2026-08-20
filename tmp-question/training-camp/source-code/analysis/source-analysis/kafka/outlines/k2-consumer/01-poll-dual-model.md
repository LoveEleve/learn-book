# K-2 Consumer 篇 1/3 — 谁在拉: poll 双模型与跨线程缓冲

> 前置: [[K-12-fetchsession-01]] (会话拉取) | 复用: — | 对照: [[rm8-push]] (RocketMQ push) | 引出: [[K-2-consumer-02]]
> 🔴 A | 来源: KafkaConsumer.java:532-536 + ConsumerDelegateCreator.java:57-70 + AsyncKafkaConsumer.java:172,299-305 + ClassicKafkaConsumer.java:116,690
> 定位: K-2 卷开篇 — 回答"poll() 怎么工作的? 双模型差在哪?"

**读者处境**: 面试官问 "consumer.poll() 是推还是拉? 内部架构?" 你答 "拉" — 但再问 "KafkaConsumer 和 AsyncKafkaConsumer 什么关系? FetchBuffer 干什么?" 你答不上来。这篇是消费模型的完整答案。

### 1. 问题引入 — 为什么是 pull

场景: 设计文档花一页论证 Push vs pull — 为什么 Kafka 坚定 pull? **不做 broker 推送** (push 会淹没慢消费者)
- 速率自控: push 会淹没慢消费者 (DoS 本质), pull 落后可追 (docs/design/design.md §Push vs pull)
- 自然批处理: pull 一次拉尽当前位置后所有数据
- 本篇问题: 双模型 (Q1) / FetchBuffer (Q2) / 对照 (Q8)

### 2. 双模型 — 门面与两个实现

场景: poll() 内部怎么分流?
- KafkaConsumer 门面 (KafkaConsumer.java:532): delegate (KafkaConsumer.java:L536) + CREATOR (KafkaConsumer.java:L534)
- ConsumerDelegateCreator (ConsumerDelegateCreator.java:L57-70): group.protocol=consumer → **AsyncKafkaConsumer** (L64, 默认 KIP-848) / classic → **ClassicKafkaConsumer** (ClassicKafkaConsumer.java:L66)
- Classic (ClassicKafkaConsumer.java:L116): poll → pollForFetches (ClassicKafkaConsumer.java:L690) 同步
- Async (AsyncKafkaConsumer.java:L172): 事件循环 + CompletableFuture

### 3. FetchBuffer — 跨线程无锁缓冲

场景: 异步模型数据怎么从网络线程到应用线程?
- AsyncKafkaConsumer: fetchBuffer (AsyncKafkaConsumer.java:L304) + fetchCollector (AsyncKafkaConsumer.java:L305)
- 注释 (AsyncKafkaConsumer.java:L299-300): "thread-safe FetchBuffer populated in the ConsumerNetworkThread"
- 构造: 缓冲共享 (AsyncKafkaConsumer.java:L428-429) / collector 应用线程专用 (AsyncKafkaConsumer.java:L486-487)
- ConsumerNetworkThread (AsyncKafkaConsumer.java:L385): 事件循环收发 (K-12 会话)

### 核心悬念
"为什么 4.x 把消费者改成双模型?" — Classic 是调用线程同步拉取 (poll 阻塞); Async 把网络 IO 移到独立 ConsumerNetworkThread + FetchBuffer 跨线程传递 — 与 K-1 Producer 的 Sender 线程同构: 调用线程与 IO 线程解耦, poll 只读缓冲。

### 概念依赖链
Q1 双模型 → Q2 FetchBuffer → Q8 对照 → (02 篇: rebalance+offset)

### 源码锚点清单
- KafkaConsumer.java:532 (类) / 534 (CREATOR) / 536 (delegate)
- ConsumerDelegateCreator.java:57-70 (create) / 64 (Async) / 66 (Classic)
- AsyncKafkaConsumer.java:172 (类) / 299-300 (注释) / 304-305 (fetchBuffer/fetchCollector) / 385 (ConsumerNetworkThread) / 428-429 (共享缓冲) / 486-487 (应用线程专用)
- ClassicKafkaConsumer.java:116 (类) / 690 (pollForFetches)
- docs/design/design.md §Push vs pull (pull 论证)
