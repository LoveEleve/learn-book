# K-1 Producer 篇 2/3 — 攒批的艺术: 批聚合与内存池

> 前置: [[K-1-producer-01]] (流水线) | 复用: — | 对照: [[rd4-command]] (命令批) | 引出: [[K-1-producer-03]]
> 🔴 A | 来源: RecordAccumulator.java:275-356 + BufferPool.java:39-75 + BuiltInPartitioner.java:39-58
> 定位: K-1 卷中篇 — 回答"消息怎么攒成批? 内存怎么管?"

**读者处境**: 面试官问 "batch.size 和 linger.ms 干什么? buffer.memory 超了会怎样?" 你答 "批、内存" — 但再问 "每分区几个批? 粘性分区是什么? BufferPool 怎么回收?" 你答不上来。这篇是批聚合与内存的完整答案。

### 1. 问题引入 — 攒批是效率引擎

场景: 设计文档说批聚合是"big drivers of efficiency" — 具体怎么攒? **不做每消息单独发送** (网络往返杀手)
- 每分区一个队列攒批 (RecordAccumulator.java:308)
- 本篇问题: 聚合 (Q2) / 内存 (Q3) / 粘性分区 (Q7)

### 2. 批聚合 — 双路径

场景: 一条消息进来塞哪?
- topicInfoMap (RecordAccumulator.java:276) + 分区 Deque (RecordAccumulator.java:L308)
- tryAppend 塞现有批 (RecordAccumulator.java:L319) / appendNewBatch 建新批 (RecordAccumulator.java:L345, 先 free.allocate RecordAccumulator.java:L330-333)
- 新批大小 = max(batchSize, 消息上限) (RecordAccumulator.java:L327-329)
- 返回 batchIsFull/newBatchCreated → 唤醒 Sender (KafkaProducer.java:1043)
- linger.ms: 批未满时最长等待 (默认 5ms, ProducerConfig.java:397) — 设计文档 10ms 是 2011 年历史示例, 非当前默认

### 3. BufferPool — 有界内存

场景: 内存不够怎么办?
- BufferPool (BufferPool.java:49-75): totalMemory + poolableSize + free 回收队列 + waiters 阻塞队列
- poolableSize 缓冲回收复用 (BufferPool.java:L39-41 注释) — 避免反复 GC
- 默认值: buffer.memory=32MB (ProducerConfig.java:381) / batch.size=16KB (ProducerConfig.java:393) — 规划断言实证
- allocate 阻塞等待 (RecordAccumulator.java:330-333) — 默认 buffer.memory=32MB
- deallocate 归还 (批完成路径, Sender.java:174)

### 4. 粘性分区 — 固定一段时间

场景: 无 key 消息怎么分区?
- UNKNOWN_PARTITION → BuiltInPartitioner (RecordAccumulator.java:300-302)
- 粘性: 一段时间固定一个分区攒大批 (BuiltInPartitioner.java:39) + stickyBatchSize 控制切换 (BufferPool.java:L52-58)
- 切换条件: allBatchesFull (RecordAccumulator.java:321-324)

### 核心悬念
"粘性分区解决什么?" — 无 key 消息若每消息随机分区, 批永远攒不大; 粘性 = 固定分区攒到批满再换 — 从"每分区小批"变成"单分区大批", 批大小与吞吐双赢 (KIP-794 优化核心)。

### 概念依赖链
Q2 聚合 → Q3 内存 → Q7 粘性 → (03 篇: Sender+语义)

### 源码锚点清单
- RecordAccumulator.java:276 (topicInfoMap) / 300-302 (粘性入口) / 308 (分区 Deque) / 319 (tryAppend) / 321-324 (切换条件) / 327-329 (新批大小) / 330-333 (allocate 阻塞) / 345 (appendNewBatch) / 351 (updatePartitionInfo)
- BufferPool.java:39-41 (设计注释) / 49-53 (字段) / 70-75 (构造)
- BuiltInPartitioner.java:39 (类) / 42 (stickyBatchSize) / 52-58 (校验)
- KafkaProducer.java:1043-1045 (wakeup)
- Sender.java:174 (deallocate)
