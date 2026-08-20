# K-2 Consumer 篇 2/3 — 组的契约: rebalance 与 offset 管理

> 前置: [[K-2-consumer-01]] (双模型) | 复用: — | 对照: [[r14-sentinel]] (故障转移) | 引出: [[K-2-consumer-03]]
> 🔴 A | 来源: ConsumerCoordinator.java (1705 行) + ClassicKafkaConsumer.java:1188 + docs/design/design.md §Consumer Position
> 定位: K-2 卷中篇 — 回答"rebalance 怎么发生? offset 怎么管?"

**读者处境**: 面试官问 "消费者组 rebalance 是什么? offset 存哪?" 你答 "协调器、主题" — 但再问 "JoinGroup/SyncGroup 流程? position 和 commit 区别? auto.offset.reset 三态?" 你答不上来。这篇是组协议与位点的完整答案。

### 1. 问题引入 — 消费组的两件大事

场景: 一个组 5 个消费者订阅 3 个主题 — 分区怎么分? 挂了怎么处理?
- rebalance (分区重分配) + offset (位点检查点)
- 本篇问题: rebalance (Q3) / offset (Q4) / 衔接 (Q6)

### 2. rebalance — 四步协议

场景: 组内消费者怎么达成一致?
- ConsumerCoordinator (ConsumerCoordinator.java:103, extends AbstractCoordinator) — 客户端协议端
- JoinGroup (入组, joinGroupIfNeeded AbstractCoordinator.java:463) → SyncGroup (分配同步) → Heartbeat (活性, pollHeartbeat AbstractCoordinator.java:368) → LeaveGroup (退出)
- 4.x 双协议: KIP-848 ModernGroup (增量) vs ClassicGroup (四步)
- 触发链: subscribe → poll → ensureActiveGroup (AbstractCoordinator.java:400-401) → onJoinComplete (ConsumerCoordinator.java:375)
- 组活性双超时: max.poll.interval.ms 默认 300000 (5min, ConsumerConfig.java:631) — poll 间隔超时被踢出组触发 rebalance; wakeup() 跨线程打断 poll (KafkaConsumer.java:1850)
- 对照 r14: Redis 哨兵故障转移 vs Kafka rebalance — 都是"成员变更重分配"

### 3. offset 管理 — 单整数检查点

场景: 消费到哪里了?
- 设计文档 §Consumer Position: "position is just a single integer, the offset of the next message" — 单整数 (vs broker 逐消息 ack)
- position (ClassicKafkaConsumer.java:L1188 updateFetchPositions): 当前消费位置
- commit: commitSync/commitAsync → __consumer_offsets (K-6) — 周期性检查点
- auto.offset.reset 三态: earliest (起点) / latest (终点) / none (无位置抛异常, ConsumerConfig.java:175-179) — 规划断言两态遗漏 none
- commit 失败: CommitFailedException (组已变更/rebalance 中) — 需重试或重订阅

### 4. 衔接 — K-6 服务端

场景: 客户端协议与服务端怎么对上?
- JoinGroup 请求 → K-6 GroupCoordinator 状态机 → 分配结果回客户端
- K-6 交付后回补双链与细节 (客户端视角本篇讲, 服务端视角 K-6)

### 核心悬念
"为什么 Kafka 的消费位置是一个整数而不是 ack?" — broker 记录逐消息 ack 需要多状态 (sent/consumed) + 未确认处理; Kafka 分区单消费者有序消费 → 位置 = 下一个 offset 单整数, 周期检查点即可 — 设计文档 "makes message acknowledgements very cheap"。

### 概念依赖链
Q3 rebalance → Q4 offset → Q6 衔接 → (03 篇: 拉取链) → (K-6 交付后回补)

### 源码锚点清单
- ConsumerCoordinator.java (1705 行: rebalance 客户端协议)
- ClassicKafkaConsumer.java:1188 (updateFetchPositions)
- docs/design/design.md §Consumer Position (单整数论证)
- KafkaConsumer.java:642-691 (subscribe/assignment 门面)
