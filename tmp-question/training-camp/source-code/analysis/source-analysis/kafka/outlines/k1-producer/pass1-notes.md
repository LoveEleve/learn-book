# K-1 Producer — Pass 1 探索笔记 (扫轮廓)

> 🔴 A | 依赖: K-7 ✅ (acks 等待) + K-3/K-4 ✅ (服务端) | 对照: [[r24-string]] (Redis 写入面) [[rd4-command]] (命令批)
> 源码: clients/.../producer/ (KafkaProducer 1622 + RecordAccumulator 1300 + Sender 1143 + BufferPool 356) — Java 端全索引
> 测试地图: clients/src/test/java/org/apache/kafka/clients/producer/ (KafkaProducerTest/RecordAccumulatorTest/BufferPoolTest/SenderTest)

## 继承树/调用图 (codebase-memory trace_path 实证)

```
KafkaProducer.send (KafkaProducer.java:940-945)
└── doSend (KafkaProducer.java:L974-1075) [trace_path 实证 callees]
      ├── waitOnMetadata (KafkaProducer.java:L990) — metadata 等待
      ├── serialize key/value (KafkaProducer.java:L1004-1016)
      ├── partition (KafkaProducer.java:L1021) — 可 UNKNOWN_PARTITION
      ├── ensureValidRecordSize (KafkaProducer.java:L1031)
      └── accumulator.append (KafkaProducer.java:L1036) → Sender.wakeup (KafkaProducer.java:L1043-1045, batch 满/新批)
RecordAccumulator.append (RecordAccumulator.java:L275-356)
├── topicInfoMap + 分区 Deque (RecordAccumulator.java:L276,308)
├── 粘性分区 builtInPartitioner (RecordAccumulator.java:L300-302)
└── tryAppend (RecordAccumulator.java:L319) / appendNewBatch (RecordAccumulator.java:L345) 双路径 + BufferPool.allocate (RecordAccumulator.java:L330)
BufferPool (BufferPool.java:L49-75): totalMemory/poolableSize/free Deque/waiters Deque
Sender.run (Sender.java:L241-258) → runOnce (Sender.java:L344) → sendProducerData (Sender.java:L379: accumulator.ready Sender.java:L382) → client.send/poll
```

## 基本元素分解 (原则二)

1. **异步流水线** — send→doSend→accumulator→Sender: 调用线程只入队, Sender 线程发送
2. **ProducerBatch 聚合** — 每分区 Deque + tryAppend/appendNewBatch (RecordAccumulator.java:308,319,345)
3. **BufferPool 内存池** — free 队列回收 poolableSize 缓冲 (BufferPool.java:49-75)
4. **粘性分区** — UNKNOWN_PARTITION → builtInPartitioner (RecordAccumulator.java:L300-302)
5. **Sender 主循环** — run (Sender.java:L241) + ready/drain/send/poll (Sender.java:L379-382)
6. **幂等/事务** — TransactionManager (K-11 衔接)

## 标记问题 (≥5)

1. **Q1: send 到 doSend 完整链?** — metadata/序列化/分区/append (KafkaProducer.java:940-1075)
2. **Q2: ProducerBatch 怎么聚合?** — 分区 Deque + 双路径 (RecordAccumulator.java:275-356)
3. **Q3: BufferPool 怎么管理内存?** — free 回收 + waiters 阻塞 (BufferPool.java:49-75)
4. **Q4: Sender 线程怎么 drain?** — run 循环 + ready + sendProducerData (Sender.java:241-382)
5. **Q5: acks 语义与 K-7 衔接?** — acks=all → 服务端 DelayedProduce (K-7)
6. **Q6: 幂等与 K-11 衔接?** — producerId+epoch (TransactionManager)
7. **Q7: 分区策略?** — murmur2/粘性 (BuiltInPartitioner)
8. **Q8: 与 RocketMQ Producer 对照?** — RM-7 已交付

## 已读测试 (2 个)

- `KafkaProducerTest`: send/回调/异常
- `RecordAccumulatorTest`: 批聚合/满/超时

## 完成检查

- [x] 继承树/调用图 (trace_path 实证)
- [x] 基本元素分解 (6 元素)
- [x] 8 个标记问题
- [x] 已读 2 个测试

## 跨域发现

- 来源: K-1 Pass 1 — trace_path 实证 doSend 调 Sender.wakeup (批满唤醒) + TransactionManager.maybeAddPartition (幂等)
- 发现: 粘性分区 (RecordAccumulator.java:L300-302) 是 K-8 网络层的客户端对应面 (批大小 vs 连接复用)
- 已对照验证: K-7 DelayedProduce (DelayedProduce.scala:101) / RM-7 (rocketmq/outlines 存在)
