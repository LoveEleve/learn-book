# K-1 Producer 篇 1/3 — 异步流水线: send 到 doSend

> 前置: — (客户端侧, 服务端 K-3/K-4/K-7 已铺垫) | 复用: — | 对照: [[r24-string]] (Redis 写入面) | 引出: [[K-1-producer-02]]
> 🔴 A | 来源: KafkaProducer.java:940-1075 + docs/design/design.md §The Producer
> 定位: K-1 卷开篇 — 回答"KafkaProducer.send 为什么快? 内部几步?"

**读者处境**: 面试官问 "KafkaProducer.send() 是同步还是异步? 内部做什么?" 你答 "异步" — 但再问 "metadata 什么时候等? 序列化哪步? 异常怎么分流?" 你答不上来。这篇是发送流水线的完整答案。

### 1. 问题引入 — send 为什么立即返回

场景: send() 返回 Future, 主线程继续 — 中间发生了什么? **不做同步阻塞等待** (吞吐), 完成走回调
- 设计文档 §The Producer: 直连 leader 无路由层 + key 哈希分区 + 异步批 (64k/10ms)
- 本篇问题: 流水线 (Q1) / 分区 (Q7)

### 2. 流水线 — doSend 的六步

场景: send 内部的具体步骤?
- send (KafkaProducer.java:940-945) → doSend (KafkaProducer.java:L974-1075)
- ①关闭检查 (KafkaProducer.java:L976) ②waitOnMetadata (KafkaProducer.java:L990, maxBlockTimeMs 上限) ③序列化 key/value (KafkaProducer.java:L1004-1016) ④partition (KafkaProducer.java:L1021) ⑤ensureValidRecordSize (KafkaProducer.java:L1031) ⑥accumulator.append (KafkaProducer.java:L1036)
- 批满/新批 → Sender.wakeup (KafkaProducer.java:L1043-1045) — 唤醒发送线程
- 回调链: AppendCallbacks 包装用户回调+拦截器 (KafkaProducer.java:978-980) → 完成 onCompletion / 失败 onSendError (KafkaProducer.java:L1052-1058) — 异步结果的两种去向

### 3. 异常分流 — 三类错误

场景: 出错怎么处理?
- ApiException → FutureFailure 返回 (KafkaProducer.java:L1050-1059) — 请求级失败不抛
- InterruptedException → 抛 InterruptException (KafkaProducer.java:L1060-1062)
- KafkaException/其他 → 抛 (KafkaProducer.java:L1063-1074)
- 幂等开启时 → transactionManager.maybeTransitionToErrorState (KafkaProducer.java:L1056)

### 4. 分区 — key 哈希与指定分区

场景: 分区怎么定?
- 指定分区直用 (KafkaProducer.java:1021)
- 无 key → UNKNOWN_PARTITION → 粘性分区 (RecordAccumulator.java:300-302, 篇 2 展开); key 哈希 murmur2 (BuiltInPartitioner.java:330)
- 设计文档: key 哈希实现消费 locality (docs/design/design.md §Load balancing)

### 核心悬念
"send() 为什么能立即返回?" — 异步流水线: 调用线程只做 metadata 等待+序列化+入队 (accumulator.append), 真正的网络发送在 Sender 线程 — 主线程不被 IO 阻塞, 批聚合让网络往返数降低一个量级。

### 概念依赖链
Q1 流水线 → Q7 分区 → (02 篇: 聚合+内存) → (03 篇: Sender+语义)

### 源码锚点清单
- KafkaProducer.java:940-945 (send) / 974-1075 (doSend) / 976 (关闭检查) / 990 (waitOnMetadata) / 1004-1016 (序列化) / 1021 (partition) / 1031 (ensureValidRecordSize) / 1036 (accumulator.append) / 1043-1045 (Sender.wakeup) / 1050-1059 (ApiException 分流) / 1060-1074 (其他异常)
- RecordAccumulator.java:300-302 (粘性分区入口)
- docs/design/design.md §The Producer (直连/分区/异步批)
