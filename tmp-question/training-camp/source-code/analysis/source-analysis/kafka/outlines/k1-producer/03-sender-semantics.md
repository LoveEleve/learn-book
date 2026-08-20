# K-1 Producer 篇 3/3 — 后台的引擎: Sender 线程与语义闭环

> 前置: [[K-1-producer-02]] (聚合+内存) [[K-7-purgatory-01]] (acks 等待) | 复用: — | 对照: [[rm7-producer]] (RocketMQ Producer) | 引出: — (K-11 事务交付后补链)
> 🔴 A | 来源: Sender.java:241-382 + DelayedProduce.scala:89-116 + TransactionManager
> 定位: K-1 卷收尾 — 回答"Sender 线程怎么发? acks/幂等怎么闭环?"

**读者处境**: 面试官问 "Sender 线程干什么? acks=all 怎么确认? 幂等怎么保证?" 你答 "发、等、去重" — 但再问 "drain 怎么选批? 批过期怎么办? 幂等和事务什么关系?" 你答不上来。这篇是发送语义的完整答案, 收束 K-1 域。

### 1. 问题引入 — 调用线程之外的引擎

场景: send() 只入队, 谁真正把数据发出去? **不做调用线程发送** (IO 不阻塞业务线程)
- Sender 独立线程 run 主循环 (Sender.java:241)
- 本篇问题: Sender (Q4) / acks (Q5) / 幂等 (Q6) / 对照 (Q8)

### 2. Sender 主循环 — ready/drain/send/poll

场景: Sender 每轮做什么?
- run (Sender.java:241-258): while running → runOnce (Sender.java:L244-247); 关闭后等剩余 (Sender.java:L256-258)
- runOnce (Sender.java:L344-345): sendProducerData (Sender.java:L344) → client.poll (RecordAccumulator.java:L345)
- sendProducerData (Sender.java:L379-382): accumulator.ready 选就绪分区 (Sender.java:L382) → drain → client.send
- 批过期: hasReachedDeliveryTimeout (Sender.java:L195) → 超时放弃 (delivery.timeout.ms)
- 重试: 失败批 canRetry (Sender.java:691,875-881: RetriableException 且未超 delivery.timeout) → 重新入队; retries 默认 Integer.MAX_VALUE (ProducerConfig.java:382) — 重试/幂等/乱序三角的客户端侧

### 3. acks 闭环 — 客户端到服务端

场景: acks=all 怎么确认?
- 客户端请求带 requiredAcks → 服务端 acks=all 走 DelayedProduce (DelayedProduce.scala:89-116)
- checkEnoughReplicasReachOffset (DelayedProduce.scala:L101, K-4) → ISR 全 ack → forceComplete (DelayedProduce.scala:L112)
- 响应回客户端 → Future 完成 (回调/拦截器)
- 闭环: K-1 请求 → K-7 等待 → K-4 HW → 响应回 K-1

### 4. 幂等与对照 — 收束

场景: 幂等怎么保证不重?
- producerId+epoch+seq (TransactionManager, K-11 共享底座) + broker 去重 (K-3 ProducerStateManager)
- 对照 RM-7: Kafka 客户端聚合+acks vs RocketMQ MQFaultStrategy 故障规避
- 面试记忆点: "Kafka 批在客户端攒, 幂等在协议层; RocketMQ 批在发送方封装, 故障在策略层"

### 核心悬念
"acks=all 的一次发送, 数据经过几个环节?" — 客户端入队 (K-1) → Sender 发送 → 服务端 append (K-3) → ISR 复制 (K-4) → DelayedProduce 等待 (K-7) → HW 推进 → ack 回客户端 — 六个域的完整闭环, 这就是为什么 Kafka 的"确认"是分布式一致性的缩影。

### 概念依赖链
Q4 Sender → Q5 acks 闭环 → Q6 幂等 → Q8 对照 → (K-11 交付后回补)

### 源码锚点清单
- Sender.java:174 (deallocate) / 195 (delivery timeout) / 241-258 (run) / 244-247 (runOnce 调用) / 256-258 (关闭等待) / 321 (client.poll) / 344-345 (runOnce) / 379-382 (sendProducerData + ready)
- DelayedProduce.scala:89-116 (tryComplete) / 101 (checkEnoughReplicasReachOffset) / 112 (forceComplete)
- KafkaProducer.java:1056 (maybeTransitionToErrorState)
- K-3: ProducerStateManager (去重) / K-4: HW / K-7: Purgatory
