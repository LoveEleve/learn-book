# Kafka-1 重写规划

> 题目：一条消息怎样从 Producer 走到 Consumer —— Kafka 主链总图
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：先用一篇总图把 Kafka 的消息主链立住：Producer 聚合与发送、Broker 网络入口、分区日志落盘、ISR/ack 边界、Consumer 拉取与位移推进；不急着钻 KRaft、Compaction、事务幂等细节。

## 1. 读者困惑

- Kafka 为什么不是“Producer 发给 Broker，Consumer 再读出来”这么简单？
- 一条消息在 Kafka 世界里先后会经过哪些层，每层到底解决什么问题？
- append-only log、ISR、fetch、offset commit 分别挂在哪一步？
- 为什么 Kafka 的很多难点不是 API，而是消息在三端之间怎样被聚合、落盘、复制和消费？
- 后面 Producer/Log/ConsumerGroup/KRaft/ISR 为什么必须分篇，而不能都塞进总图里？

## 2. 一句话顿悟

**Kafka 的主链不是一次 socket write，而是一条“Producer 先聚合 batch → Broker 网络入口接请求 → 分区日志顺序追加 → ISR/ack 决定写入何时算数 → Consumer 按 fetch 与 offset 世界持续拉取”的多阶段流水线；后面所有专题，都只是围绕这条主链上某一层边界继续深挖。**

## 3. 五要素卡片

### 读者问题

一条 Kafka 消息从业务线程发出，到最终被 Consumer 看见，中间到底穿过了哪些核心层？

### 入口

- Producer：`KafkaProducer.send()` → `RecordAccumulator` → `Sender`
- Broker：`SocketServer` / `KafkaApis.handleProduceRequest()`
- 存储：`Partition.appendRecords()` / `LogSegment.append()`
- 消费：`KafkaConsumer.poll()` / `Fetcher` / `FetchBuffer`

### 状态核心

- topic-partition / leader broker
- ProducerBatch / BufferPool / acks
- append-only log / offset / HW / ISR
- fetch response / consumer position / committed offset
- `__consumer_offsets` 作为位移提交存储

### 失败路径

- Producer 没有分区目标：消息根本不知道该进哪条日志
- Broker 收到请求但日志/副本边界未立住：写入何时算成功不清楚
- Consumer 不维护 position/commit：能拉到消息但系统不知道消费推进到了哪里
- 把 Controller/KRaft 混到总图里：主链被控制面细节压塌

### 连接点

- 后文 `Kafka-2`：Producer send / accumulator / acks 主链
- 后文 `Kafka-3`：LogSegment / LazyIndex / ProducerStateManager
- 后文 `Kafka-4`：Consumer poll / Fetcher / offset 推进
- 后文 `Kafka-6~8`：ConsumerGroup / Controller / KRaft 作为控制面
- 后文 `Kafka-9~12`：ISR / Purgatory / Compaction / 事务幂等作为可靠性补层

## 4. 总图

```text
业务线程 send(record)
  → Producer 序列化/分区/聚合成 batch
    → Sender 把 batch 发给 leader broker
      → Broker 网络入口接住 produce 请求
        → Partition / LogSegment 顺序追加日志
          → ISR / acks 决定何时对 Producer 算写入成功
            → Consumer poll/fetch 拉取分区日志
              → position 前进 / offset commit 写入 __consumer_offsets
                → 业务 finally 看见消息
```

## 5. 关键边界

- 本篇是总图篇，不单独深挖 KRaft、Compaction、事务标记与 Raft 状态机细节。
- 不把 Consumer 和 ConsumerGroup 混成一层；前者是拉取/位移，后者是成员协调。
- 不把 Log 存储、ISR、副本一致性、事务幂等都压成“可靠性”一句话。
- 不把 `acks=all` 写成事务保证；它只回答当前写入确认边界的一部分。

## 6. 失败方案推演

1. **把 Kafka 理解成一次 Producer→Broker→Consumer 的直线传输**：会看不见 batch、日志、ISR、fetch、offset 世界。
2. **把 Broker 只看成转发节点**：会忽略分区日志与副本边界是主链中心。
3. **把 Consumer 只看成“读消息”**：会漏掉 position / committed offset 这套持续推进语义。
4. **在总图篇就引入 KRaft / Compaction / 事务内部细节**：主链会被控制面和专题层压散。

## 7. 误解清单

- Kafka 主链不是一次同步 socket write。
- append-only log 是主链中心，不是底层实现细节。
- Consumer 看见消息不等于消费状态已经被系统记住。
- Controller/KRaft 不是总图起点，而是后续控制面篇章。
- ISR/acks 只是“写入何时算数”的一层，不等于全套事务保证。

## 8. 证据清单

- `Kafka源码学习范围规划.md:89`
- `Kafka源码学习范围规划.md:92`
- `Kafka源码学习范围规划.md:97`
- `Kafka源码学习范围规划.md:101`
- `Kafka源码学习范围规划.md:108`

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇只立消息主链总图，不单独钻老 ZK 路线。
- 目标正文：7000~11000 字；重点讲多阶段流水线、失败直觉和后续篇章依赖图。

## 10. 本轮重写主线

1. 从“Kafka 为什么不是直线传输”开场。
2. 否定 Producer→Broker→Consumer 的扁平直觉。
3. 先立 Producer 聚合、Broker 落盘、Consumer 拉取三段主链。
4. 再补 ISR/acks 与 offset commit 作为何时算成功/何时算推进的边界。
5. 收网：后续各篇只是总图中某一层的独立放大。