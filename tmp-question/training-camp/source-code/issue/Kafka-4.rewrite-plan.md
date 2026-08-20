# Kafka-4 重写规划

> 题目：KafkaConsumer.poll() 为什么不是“拉一批消息回来”——Fetcher、FetchBuffer 与 offset 推进主链
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：解释 Kafka Consumer 的拉取主链——`poll()` 为什么不是一次同步 RPC，而是“组状态/分区位置确认 → Fetcher 发送 fetch → FetchBuffer 暂存结果 → collectFetch 推进 position → commitSync/commitAsync 落 __consumer_offsets”的持续推进过程，不提前吞入 ConsumerGroup 细节。

## 1. 读者困惑

- `KafkaConsumer.poll()` 为什么不是“发个 fetch 请求、拿一批消息回来”这么简单？
- `Fetcher`、`FetchBuffer`、`position`、committed offset 分别在补哪一层缺口？
- 为什么拉到消息不等于消费状态已经被系统记住？
- `commitSync/commitAsync` 为什么不能和 `poll()` 混成一个动作？
- Consumer 和 ConsumerGroup 为什么必须分篇，而不能都压在 poll 里？

## 2. 一句话顿悟

**KafkaConsumer.poll() 的关键不在“这次取到几条消息”，而在“当前分区位置是否已准备好、fetch 请求是否已发出、FetchBuffer 是否已有结果、position 是否已经推进、commit 是否已把推进结果正式写入 `__consumer_offsets`”；Consumer 先管理的是日志阅读状态，Group 协调只是这条状态链的前置条件之一。**

## 3. 五要素卡片

### 读者问题

为什么 Kafka Consumer 不是一次性取消息 API，而是一条围绕分区位置、fetch 缓冲和 offset 提交持续推进的状态链？

### 入口

- `ClassicKafkaConsumer.poll()`：经典消费入口
- `updateAssignmentMetadataIfNeeded()`：分区位置/组状态前置准备
- `pollForFetches()`：决定是否需要发 fetch 与等待响应
- `Fetcher.sendFetches()`：向 broker 发 fetch 请求
- `FetchBuffer` / `FetchCollector.collectFetch()`：缓冲并返回可消费记录
- `commitSync()` / `commitAsync()`：把消费推进写入 `__consumer_offsets`

### 状态核心

- assigned partitions / fetch positions
- `position` vs committed offset
- `FetchBuffer` / completed fetches / nextInLineFetch
- `sendFetches()` 与 `collectFetch()` 的前后顺序
- `subscriptions.allConsumed()`
- `__consumer_offsets` 作为位移提交事实层

### 失败路径

- 把 poll 理解成同步 RPC：看不见已有缓冲、预取与后台请求状态
- 只拉消息不维护 position：下一轮不知道从哪里继续
- 把 position 当成 committed offset：进程重启或 rebalance 后会失去恢复基准
- commit 和 poll 混成一个动作：无法看见“已看见消息”和“系统已记住推进”是两层事实
- 把 ConsumerGroup 协调细节塞进本篇：会压塌 Fetcher/FetchBuffer 主线

### 连接点

- 前文 `Kafka-1`：总图中的“Consumer 拉取与位移推进”层
- 后文 `Kafka-5`：FetchSession 作为 fetch 主链的性能补层
- 后文 `Kafka-6`：ConsumerGroup 协调成员与分区分配，不与 Consumer 拉取混写
- 后文 `Kafka-9/12`：ISR、事务与 read_committed 会继续影响 fetch 可见性边界

## 4. 总图

```text
poll(timeout)
  → 确认 assignment / fetch position 已准备好
    → 先尝试 collectFetch(FetchBuffer)
      → 没有结果时 sendFetches()
        → broker 返回 fetch response
          → FetchBuffer 缓冲 CompletedFetch
            → collectFetch() 取出记录并推进 position
              → commitSync/Async 才把推进写入 __consumer_offsets
```

## 5. 关键边界

- 本篇只讲 Consumer 拉取与位移推进，不展开 ConsumerGroup rebalance 协议细节。
- 不把 poll 返回记录写成 committed；position 前进和 offset 提交是两层事实。
- 不把 FetchBuffer 写成普通缓存，它是 fetch 响应在应用线程和网络线程之间的中间状态平面。
- 不把 commitSync/commitAsync 看成“顺手动作”；它们决定系统是否真正记住消费进度。

## 6. 失败方案推演

1. **poll 就是一轮同步 fetch RPC**：忽略已有缓冲、预取和下一轮 pipeline。
2. **看到消息就算消费推进完成**：重启/再均衡后没有 committed offset 作为恢复事实。
3. **每次 poll 都从头找位置**：没有稳定 position/commit 链，拉取无法持续推进。
4. **把 ConsumerGroup 混进 fetch 主链**：成员协调细节会压塌 poll/Fetcher/offset 这条线。

## 7. 误解清单

- `poll()` 不是简单 fetch RPC 包装。
- `Fetcher` 负责发请求和收集结果，`FetchBuffer` 负责跨线程/阶段承接结果。
- position 不等于 committed offset。
- commit 不是“可选小动作”，而是消费世界的系统事实写入。
- Consumer 和 ConsumerGroup 是前后相邻但不同层的问题。

## 8. 证据清单

- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ClassicKafkaConsumer.java:624`
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ClassicKafkaConsumer.java:679`
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ClassicKafkaConsumer.java:690`
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ClassicKafkaConsumer.java:741`
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/Fetcher.java:105`
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/Fetcher.java:145`
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/FetchBuffer.java:39`
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/FetchCollector.java:92`
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/SubscriptionState.java:627`

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦 `ClassicKafkaConsumer` 主链，`AsyncKafkaConsumer` 只作为后续边界提示。
- 目标正文：8000~12000 字；核心拆解层覆盖 poll、fetch、buffer、position、commit 五段主链。

## 10. 本轮重写主线

1. 从“poll 为什么不是拉一批消息回来”开场。
2. 否定同步 RPC、看到消息就算提交、poll/commit 不分三种直觉方案。
3. 解释 assignment/position 前置准备与 `pollForFetches()`。
4. 解释 `Fetcher.sendFetches()`、`FetchBuffer`、`collectFetch()` 怎样承接 fetch 世界。
5. 解释 position 前进与 committed offset 写入的分层。
6. 收网：Kafka Consumer 管理的首先是日志阅读状态，而不是一批消息对象。