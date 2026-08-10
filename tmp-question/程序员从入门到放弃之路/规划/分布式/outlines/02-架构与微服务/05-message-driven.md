# 秒杀了, 1秒10万下单请求涌进服务器 — 你怎么不让MySQL被打死?

> Cluster B: 12 KPs | 依赖: 04-caching-strategy | 读者基线: 了解消息队列基本概念(生产者/消费者/主题)

---

### 1. 同步处理10万请求 — 线程池满了, CPU满了, 然后挂了
  用户点"立即购买"→前端先查库存→扣库存→生成订单→扣积分→发通知 — 同步调用, 每个步骤都阻塞直到完成
  - 为什么要异步解耦: 同步链中任意一环节慢→所有调用者线程阻塞→线程池耗尽→整个服务雪崩 (B3 Ch7 §4)
  - B3 Ch7 §4.1: MQ削峰 — 请求先进MQ→消费者以自己速率拉取处理, 下游保护自己不被压垮 (B3 Ch7 §4.1)
  - 削峰效果: 10万请求/秒写入MQ→消费者1000 QPS消费→100秒处理完→DB平稳每秒5000条写入无压力, 代价=用户等待100秒(异步感知) (B2 Ch8 §3.3)
  - 关键设计: MQ削峰的核心=用时间换稳定 — 不是"快", 是"不崩", 用户等30秒比服务器宕机30分钟代价小得多

### 2. Kafka架构深度 — 为什么它比RabbitMQ快一个数量级?
  Kafka不是Queue, 是"分布式提交日志" — 理解了这一点就理解了Kafka的全部设计
  - B4 Ch3 §6.3-6.4: Kafka核心: 分区日志(分区内严格有序, 分区间无序) + 消费者offset(自管理, 回退/重置) + 顺序写(append-only, 磁盘顺序写~600MB/s) + 零拷贝(sendfile, 数据不经过用户态, PageCache→Socket Buffer→NIC)
  - B1 Ch7 §2.4: Topic→Partition(并行度单位)→Segment(分段日志文件→.log索引+.index偏移量+.timeindex时间) → 磁盘顺序写+页缓存命中→高吞吐 (B4 Ch3 §6.5)
  - B4 Ch3 §6.6: 不丢消息 — replication.factor>=3, min.insync.replicas>=2, acks=all(-1), 生产者retries+idempotent; 消费者手动提交offset(读完处理完再commit) (B2 Ch11 §1.1)
  - 关键设计: Kafka顺序写+零拷贝+PageCache — 内存不是存储而是缓存(PaceCache), 所有数据在磁盘, 消费者拉到最近数据的PageCache命中率≈100%

### 3. Kafka vs RocketMQ vs RabbitMQ — 三种引擎, 三种设计哲学
  你团队有人用RabbitMQ, 换到Kafka后说"怎么消息不保证顺序了?!" → 分区内有序, 全局无序
  - B1 Ch7 §2.1-2.3: RabbitMQ(Exchange+Queue+Binding, 灵活路由/支持AMQP/管理界面丰富, 适合复杂路由+低吞吐) vs Kafka(分区日志+高吞吐+顺序写/消费者拉, 适合日志/流处理)
  - RocketMQ: 阿里系, 借鉴Kafka设计(CommitLog+ConsumeQueue), 支持事务消息(半消息+check本地事务→commit/rollback), 延迟消息(18级), 适合阿里云生态 (B1 Ch7 §3.1-3.3)
  - 选型决策表: 高吞吐/日志/事件溯源→Kafka; 复杂路由/低延迟/金融消息→RabbitMQ; 阿里云/JVM生态/事务消息→RocketMQ [案例: LinkedIn用Kafka处理每天7万亿条消息, 写到commit log后消费者任意回放offset]
  - 关键设计: RabbitMQ(推) vs Kafka(拉) — 推模式(服务端控制发送速率, 消费者被动接收) vs 拉模式(消费者自己控制处理速率, 更自主)

### 4. 事件驱动架构 — MQ不只是削峰, 它在重塑架构
  下单→OrderCreated事件→库存服务扣减→积分服务增加→通知服务发送 → 这是事件驱动的核心: 服务不需要"知道"彼此
  - B2 Ch8 §3.3: 异步化不仅是削峰 — 事件发布(OrderService只发OrderCreated事件, 不关心谁消费)→事件订阅(Inventory/Point/Notify各自订阅)→最终一致性(MQ at-least-once+消费者幂等处理)
  - 顺序保证: 同用户事件到同一分区(partition key=userId)→分区内有序→消费者单线程处理该分区→该用户的所有操作顺序正确 (B4 Ch3 §6.5)
  - B4 Ch6 §6: MQ提升写性能实践 — 日志先入MQ→批量写ES(每秒5000条批量→1次bulk API→10ms), 而非单条insert 5000次→50000ms
  - 关键设计: 事件驱动=服务发现的反模式 — 不是"我需要调谁"而是"谁在乎我发出了什么", 耦合从代码层移到数据层(事件Schema)

### 5. 收束 — 回到秒杀场景
  - 10万请求/秒→MQ→1000 QPS消费→100秒内全部处理完→DB每秒5000写入, 平稳
  - 引入MQ的三步骤: (1)解耦同步依赖→事件驱动 (2)削峰→异步队列 (3)保证最终一致→消费者幂等+重试
  - MQ不是万能药 — 带来最终一致性、消息回溯、重复消费的复杂度

---

### 核心悬念
**"MQ帮你处理了瞬间的写请求, 但用户的订单数据存储在哪? 单库10TB, 查询超30秒——分库分表是什么, 为什么它比MQ更复杂?"**

→ 引出 存储架构: 分库分表+读写分离+分片扩容的全部"坑" (06-storage-architecture)
