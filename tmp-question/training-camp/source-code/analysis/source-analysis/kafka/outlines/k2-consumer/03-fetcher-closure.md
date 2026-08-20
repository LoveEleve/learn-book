# K-2 Consumer 篇 3/3 — 拉取的闭环: Fetcher 链与跨域对照

> 前置: [[K-2-consumer-02]] (rebalance+offset) [[K-12-fetchsession-01]] (会话) | 复用: — | 对照: [[rm8-push]] (RocketMQ push) [[r9-replication]] (位点对照) | 引出: — (K-6 Group 交付后补链)
> 🔴 A | 来源: Fetcher.java:59 + FetchRequestManager.java + ClassicKafkaConsumer.java:690 + AsyncKafkaConsumer.java:304
> 定位: K-2 卷收尾 — 回答"数据怎么从 broker 到应用线程? 与 K-12 怎么闭环?"

**读者处境**: 面试官问 "poll 返回的数据从哪来? Fetcher 干什么?" 你答 "拉取" — 但再问 "拉取走会话吗? 网络线程还是调用线程? 和 RocketMQ push 差在哪?" 你答不上来。这篇是拉取闭环的完整答案, 收束 K-2 域。

### 1. 问题引入 — 从 offset 到记录

场景: poll() 拿到 1000 条记录 — 中间链路? **不做调用线程网络 IO** (Async 模型)
- 双模型各自拉取: Classic pollForFetches (ClassicKafkaConsumer.java:L690) / Async ConsumerNetworkThread
- 本篇问题: 拉取链 (Q5) / 衔接 (Q7) / 对照 (Q8)

### 2. 拉取链 — 会话增量

场景: 拉取怎么走?
- Fetcher (Fetcher.java:59, extends AbstractFetch) — 门面
- FetchRequestManager (FetchRequestManager.java:L183): 请求构造
- FetchCollector: 记录解码收集
- 会话: FetchSessionHandler (K-12) — 增量 fetch (1000 分区只发变更)
- 链路: poll → pollForFetches (ClassicKafkaConsumer.java:L690) → 会话增量 → 服务端 FetchSession → FetchBuffer

### 3. 闭环 — 客户端缓冲 + 服务端会话

场景: K-2 与 K-12 怎么构成双缓存面?
- 客户端: FetchBuffer (AsyncKafkaConsumer.java:304) 网络线程填/应用线程读
- 服务端: FetchSession (K-12) 分区参数缓存
- 双缓存面: 客户端缓冲 (数据传递) + 服务端会话 (元数据增量) — 各解决各的问题
- 面试记忆点: "客户端缓存数据, 服务端缓存元数据"

### 4. 对照 — pull vs push 收束

场景: 与 RocketMQ 消费差在哪?
- Kafka pull: 速率自控 + 自然批 (设计文档论证)
- RocketMQ push (RM-8): broker 推送, DefaultMQPushConsumer
- 位点: Kafka offset 客户端检查点 vs RM 消费进度
- 收束: K-2 是消费面终点, 与 K-1 生产面对称 (双客户端)

### 核心悬念
"pull 和 push 的终极差异?" — push 的 broker 无法知道消费者处理能力 (快则空转慢则淹没); pull 让消费者自控速率 (落后可追) + 每次拉尽当前位置后所有数据 (自然批) — 用"消费者可能忙等"换"永不淹没", long poll 参数缓解忙等 (设计文档三连论证)。

### 概念依赖链
Q5 拉取链 → Q7 会话闭环 → Q8 对照 → (K-6 交付后回补)

### 源码锚点清单
- Fetcher.java:59 (类, extends AbstractFetch)
- FetchRequestManager.java (请求构造)
- ClassicKafkaConsumer.java:690 (pollForFetches)
- AsyncKafkaConsumer.java:304 (fetchBuffer)
- FetchSessionHandler.java:60 (K-12 客户端会话)
- docs/design/design.md §Push vs pull (pull 论证) + §The Consumer (long poll)
