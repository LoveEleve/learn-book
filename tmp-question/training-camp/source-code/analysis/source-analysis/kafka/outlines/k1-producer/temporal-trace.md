# K-1 Producer — 时空溯源 (2026-08-15)

> 🔴 A 域强制 | 方法: git log --diff-filter=A 找引入 commit + git log -1 日期实证

## 断代链

| 时间 | commit | 里程碑 | 演进 |
|---|---|---|---|
| 2011-08-01 | Initial checkin (Apache SVN) | **Producer.scala 初始** | LinkedIn 时代同步 Producer (阻塞发送) |
| 2014-02-06 | Rename client package | **KafkaProducer.java 定型** | kafka.* → org.apache.kafka (Java 客户端, 异步流水线: KafkaProducer/RecordAccumulator/BufferPool/Sender 四件套) |
| 2017-04-27 | KAFKA-4818 | **幂等+事务** | TransactionManager 引入 (producerId+epoch, exactly-once 事务) |
| 2019-08-01 | KAFKA-8601 (KIP-480) | **粘性分区** | keyless 消息粘性分区 — 攒大批 (stickyBatchSize) |
| 2020+ | KIP-794/adaptive | **自适应粘性** | 按 broker 负载切换分区 (BuiltInPartitioner 演进) |

## 关键观察

1. **2014 Java 客户端是异步化分水岭**: 从 Scala 同步 Producer 到 Java 四件套 (异步流水线 + 批聚合 + 内存池) — 批聚合成为效率核心 (设计文档 "Batching is one of the big drivers of efficiency")
2. **2017 KAFKA-4818 语义升级**: 幂等 (producerId+seq 去重) + 事务 (两阶段) — 客户端从"尽力发送"到"精确一次"
3. **2019 KIP-480 粘性分区**: 无 key 消息固定分区攒大批 — 批大小与吞吐双赢 (K-1 篇 2 核心悬念)
4. 对照 K-3 (存储层 2012 定型/K-4 复制层 2017 定型): **客户端层 2014 定型后 2017/2019 两次语义升级** — 客户端演进节奏与服务端错开 (服务端先稳定, 客户端后增强)

## 对照锚点

- RocketMQ RM-7 (Producer 发送): RM 早期就异步 (2016 开源时即 DefaultMQProducer), Kafka 2014 Java 化 — 两家异步化时间线接近
- ES E-1 Engine (写入路径): ES InternalEngine 2014 更名定型 vs Kafka 2014 Java 客户端 — 同期架构定型
