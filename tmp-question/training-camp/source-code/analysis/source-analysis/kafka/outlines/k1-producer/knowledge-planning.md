# K-1 Producer — 知识规划 (00 §10: 逐源提取→聚合→分类→聚类)

> 2026-08-15 | 源码: clients/src/main/java/org/apache/kafka/clients/producer/ (KafkaProducer 1622 + RecordAccumulator 1300 + Sender 1143 + BufferPool 356) — Java 端全索引 (codebase-memory trace_path 实证 doSend 链)

## 01 逐源提取

| 源文件 | 机制点 |
|---|---|
| KafkaProducer.java | ①send (KafkaProducer.java:L940-945) → doSend (KafkaProducer.java:L974-1075) ②waitOnMetadata (KafkaProducer.java:L990) ③序列化 key/value (KafkaProducer.java:L1004-1016) ④partition (KafkaProducer.java:L1021) ⑤accumulator.append (KafkaProducer.java:L1036) ⑥Sender.wakeup (KafkaProducer.java:L1043-1045) ⑦异常分流 (KafkaProducer.java:L1050-1074) |
| RecordAccumulator.java | ①topicInfoMap + 分区 Deque (RecordAccumulator.java:L276,308) ②粘性分区 builtInPartitioner (RecordAccumulator.java:L300-302) ③tryAppend/appendNewBatch 双路径 (RecordAccumulator.java:L319,345) ④BufferPool allocate (RecordAccumulator.java:L330) |
| Sender.java | ①drain 批处理 ②NetworkClient 发送 ③ack 处理 |
| BufferPool.java | ①allocate/deallocate ②内存池 (poolableSize) ③双缓冲 free/available |

## 02 聚合 (P1/P2/P3)

| 聚合机制 | 来源 | 分级 |
|---|---|---|
| send 异步流水线 (send→doSend→accumulator→sender) | KafkaProducer + Accumulator + Sender | P1 |
| ProducerBatch 聚合 (每分区队列) | RecordAccumulator | P1 |
| BufferPool 内存池 | BufferPool | P1 |
| 粘性分区 (murmur2/可用性感知) | BuiltInPartitioner | P1 |
| Sender 后台线程 drain | Sender | P1 |
| 幂等/事务状态 | TransactionManager (K-11 衔接) | P2 |
| acks 语义 | 服务端 K-7 衔接 | P2 |

## 03 深度分类

- 🔴: 异步流水线 + 批聚合 + 内存池 (Producer 定义特征)
- 🟡: 粘性分区 / Sender 线程 / 幂等
- 🟢: 配置 (buffer.memory=32MB 等)

## 04 聚类 (教学顺序)

```
send() → doSend (metadata/序列化/分区) → RecordAccumulator.append (ProducerBatch 聚合)
  → BufferPool (内存管理) → Sender.drain (后台线程) → NetworkClient (K-8 衔接)
  → acks 语义 (K-7 服务端等待) + 幂等 (K-11)
循环依赖 (02 §1.4): Producer↔Accumulator↔Sender 域内闭环
```

**拆篇建议**: 3 篇 (🔴 A, 8 闭环)
- 01: send 流水线 (send→doSend: metadata/序列化/分区/异常)
- 02: 批聚合与内存 (RecordAccumulator + BufferPool + 粘性分区)
- 03: Sender 线程与语义 (drain/acks/幂等 + 与 K-7/K-11 衔接)
