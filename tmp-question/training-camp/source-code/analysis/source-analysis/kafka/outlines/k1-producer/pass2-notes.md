# K-1 闭环笔记 Q1-Q8: 流水线/聚合/内存/线程/acks/幂等/分区/对照

## Q1: send → doSend 完整链?

假设: send 提交异步, doSend 内 metadata→序列化→分区→append。

验证过程:
- send (KafkaProducer.java:940-945) → doSend (KafkaProducer.java:L974-1075) [trace_path 实证 callees 链]
- doSend: throwIfProducerClosed (KafkaProducer.java:L976) → waitOnMetadata (KafkaProducer.java:L990, maxBlockTimeMs 上限) → serialize key/value (KafkaProducer.java:L1004-1016) → partition (KafkaProducer.java:L1021, 可 UNKNOWN_PARTITION) → ensureValidRecordSize (KafkaProducer.java:L1031) → accumulator.append (KafkaProducer.java:L1036)
- 批满/新批 → Sender.wakeup (KafkaProducer.java:L1043-1045)
- 异常分流: ApiException → FutureFailure (KafkaProducer.java:L1050-1059) / InterruptedException/KafkaException (KafkaProducer.java:L1060-1074)

代码类型: Glue (流水线)

结论: **send 链 = send (KafkaProducer.java:L940) → doSend: metadata 等待 (KafkaProducer.java:L990) → 序列化 (KafkaProducer.java:L1004-1016) → 分区 (KafkaProducer.java:L1021) → append (KafkaProducer.java:L1036) → 批满唤醒 Sender (KafkaProducer.java:L1043); 异常按类型分流 (KafkaProducer.java:L1050-1074)**。KafkaProducer.java:940-1075

## Q2: ProducerBatch 怎么聚合?

假设: 每分区一个 Deque<ProducerBatch>, 先试塞现有批, 不行开新批。

验证过程:
- RecordAccumulator.append (RecordAccumulator.java:L275-356): topicInfoMap (RecordAccumulator.java:L276) + 分区 Deque (RecordAccumulator.java:L308)
- 双路径: tryAppend 现有批 (RecordAccumulator.java:L319) / appendNewBatch 新批 (RecordAccumulator.java:L345) — 新批先 free.allocate (RecordAccumulator.java:L330-333, 可能阻塞)
- allBatchesFull → 粘性分区切换 (RecordAccumulator.java:L321-324)
- appendNewBatch → ProducerBatch.tryAppend (RecordAccumulator.java:L387,395)

代码类型: Implementation (批聚合)

结论: **聚合 = 分区 Deque (RecordAccumulator.java:L308) + 双路径: tryAppend 塞现有批 (RecordAccumulator.java:L319) / appendNewBatch 建新批 (RecordAccumulator.java:L345, 先 allocate RecordAccumulator.java:L330); 返回 RecordAppendResult 携带 batchIsFull/newBatchCreated 供 wakeup 判断**。RecordAccumulator.java:275-356

## Q3: BufferPool 内存管理?

假设: free 队列回收 poolableSize 缓冲, 不够时 waiters 阻塞。

验证过程:
- BufferPool (BufferPool.java:49-75): totalMemory (BufferPool.java:L49) / poolableSize (BufferPool.java:L50) / free Deque (BufferPool.java:L52) / waiters Deque (BufferPool.java:L53)
- 设计注释 (BufferPool.java:L39-41): poolableSize 缓冲进 free 列表回收
- allocate (RecordAccumulator.java:L330-333 调用): 不足 maxTimeToBlock 阻塞
- deallocate: 归还 free 或释放

代码类型: Implementation (内存池)

结论: **BufferPool = totalMemory/poolableSize/free 回收队列/waiters 阻塞队列 (BufferPool.java:49-75); append 路径 allocate 阻塞等待 (RecordAccumulator.java:330-333) — 生产者内存有界 (默认 32MB)**。BufferPool.java:49-75

## Q4: Sender 线程怎么 drain?

假设: run 主循环 → ready 检查 → drain 批 → client.send/poll。

验证过程:
- Sender.run (Sender.java:241-258): while running → runOnce (Sender.java:L244-247); 关闭后等未完成 (Sender.java:L256-258)
- runOnce (Sender.java:L344-345): sendProducerData (Sender.java:L344) → client.poll (RecordAccumulator.java:L345)
- sendProducerData (Sender.java:L379-382): accumulator.ready (Sender.java:L382) → 按节点就绪 → client.send
- 批过期: hasReachedDeliveryTimeout (Sender.java:L195) → 过期处理

代码类型: Implementation (后台线程)

结论: **Sender = 独立线程 run 主循环 (Sender.java:L241-258) → runOnce: sendProducerData (Sender.java:L344, accumulator.ready Sender.java:L382) → client.poll (Sender.java:L345) — 调用线程与发送线程解耦 (异步流水线核心)**。Sender.java:241-382

## Q5: acks 语义与 K-7 衔接?

假设: acks=0/1/all, all 时服务端等 ISR (K-7 DelayedProduce)。

验证过程:
- 客户端: acks 配置 → 请求包含 requiredAcks (服务端判断)
- 服务端: acks=all → DelayedProduce.tryComplete → checkEnoughReplicasReachOffset (DelayedProduce.scala:101, K-7 实证)
- 设计文档: acks=0 不等 / 1 leader 写入 / all 全 ISR
- 衔接: K-1 请求语义 ↔ K-7 服务端等待 ↔ K-4 HW

代码类型: 衔接分析

结论: **acks 三态 (0/1/all): all → 服务端 DelayedProduce 等待 ISR (DelayedProduce.scala:101, K-7) → HW 推进完成 (K-4) — 客户端语义在服务端落地 (K-1↔K-7↔K-4 闭环)**。DelayedProduce.scala:89-116

## Q6: 幂等与 K-11 衔接?

假设: enable.idempotence → producerId+epoch+seq, broker 去重。

验证过程:
- doSend: transactionManager.maybeAddPartition (trace_path 实证 callee)
- TransactionManager: producerId/epoch 状态 (K-11 展开)
- 服务端: ProducerStateManager 去重 (K-3 篇 4 已实证: producers map + snapshot)
- 语义: 单分区有序 + 重试不重 (broker 按 seq 判重)

代码类型: 衔接分析

结论: **幂等 = producerId+epoch+seq (TransactionManager, trace_path 实证) + broker 侧 ProducerStateManager 去重 (K-3) — 与 K-11 事务共享状态底座**。KafkaProducer.java:974 (maybeAddPartition callee) + K-3 ProducerStateManager

## Q7: 分区策略?

假设: key 哈希 (murmur2) + 无 key 粘性分区。

验证过程:
- doSend partition (KafkaProducer.java:1021) — 指定分区直接用
- UNKNOWN_PARTITION → BuiltInPartitioner (RecordAccumulator.java:300-302)
- BuiltInPartitioner (BuiltInPartitioner.java:39): stickyBatchSize 控制切换 (BuiltInPartitioner.java:L52-58) + updatePartitionInfo (RecordAccumulator.java:323,351)
- 设计文档: key 哈希实现 locality (docs/design/design.md §Load balancing)

代码类型: Algorithmic (分区)

结论: **分区 = 指定分区直用 (KafkaProducer.java:1021) / 无 key 走 BuiltInPartitioner 粘性分区 (RecordAccumulator.java:300-302, BuiltInPartitioner.java:39) — 粘性 = 一段时间固定分区提高批大小**。BuiltInPartitioner.java:39-58

## Q8: 与 RocketMQ Producer 对照?

假设: RM-7 已交付, 对照异步/批/失败语义。

验证过程:
- RocketMQ: DefaultMQProducer.send → MQFaultStrategy 故障规避 (RM-7)
- Kafka: 无故障规避层 (NetworkClient 重连), 靠批聚合 + acks
- 对照维度: 批量 (RM 批量消息 vs Kafka ProducerBatch) / 故障 (MQFaultStrategy vs 重试) / 事务 (RM 半消息 vs K-11 两阶段)
- 面试记忆点: "Kafka 批在客户端聚合, RocketMQ 批在发送方封装"

代码类型: 对照分析

结论: **Kafka Producer (客户端聚合+acks 语义) vs RocketMQ Producer (MQFaultStrategy 故障规避+批量消息) — 批聚合策略与故障处理两范式 (RM-7 对照)**。rocketmq RM-7 对照

跨域关联: K-7 (acks 等待) / K-3/K-4 (服务端) / K-11 (幂等事务) / RM-7 (RocketMQ 对照)
