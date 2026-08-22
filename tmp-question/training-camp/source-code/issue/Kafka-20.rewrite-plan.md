# Kafka-20 重写规划

> 题目：两代 Consumer 模型——ClassicKafkaConsumer 的同步 poll 与 AsyncKafkaConsumer 的事件驱动主链
> 状态：K-4 Consumer 域第 2 篇，按"Classic vs Async 模型"展开
> 目标：解释 Kafka 的两种 Consumer 实现：`ClassicKafkaConsumer` 的同步 `poll()` 循环（应用线程自己做网络 IO、协调器、fetch），与 `AsyncKafkaConsumer` 的事件驱动模型（`ConsumerNetworkThread` 后台线程做网络 IO，`ApplicationEventHandler` 在应用线程处理事件）。覆盖两套线程模型的设计差异、SharedFetchBuffer 的线程安全、以及为什么 AsyncKafkaConsumer 是 KIP-848 的默认选择。

## 1. 读者困惑

- 为什么 `KafkaConsumer.poll()` 既能同步等待又能异步返回？
- Classic 和 Async 两种 Consumer 有什么区别？
- 为什么 AsyncKafkaConsumer 需要 `ConsumerNetworkThread`，而 Classic 不需要？
- `FetchBuffer` 在两种模型下分别是谁读谁写？
- 两套模型到底怎么选？AsyncKafkaConsumer 会取代 Classic 吗？

## 2. 一句话顿悟

**ClassicKafkaConsumer 在应用线程的 `poll()` 里同步完成"协调器维护 → fetch 发送 → 响应收集 → 数据处理"；AsyncKafkaConsumer 则把网络 IO 与后台事件交给 `ConsumerNetworkThread` 专用线程，应用线程通过 `ApplicationEventHandler` 收发事件，网络 IO 不阻塞应用线程。**

## 3. 五要素卡片

### 读者问题

同一个 KafkaConsumer API，背后为什么有两种实现？它们对应用线程的阻塞行为有什么不同？

### 入口

- `ClassicKafkaConsumer`：poll() 同步循环
- `AsyncKafkaConsumer`：事件驱动模型
- `ConsumerNetworkThread`：后台网络 IO 线程
- `ApplicationEventHandler`：应用线程事件处理器
- `FetchBuffer` / `SharedFetchBuffer`：线程安全的 fetch 结果缓冲区

### 状态核心

- Classic：`poll()` → `updateAssignmentMetadataIfNeeded` → `sendFetches()` → `client.poll()` → `collectFetch()`
- Async：`poll()` → `ApplicationEventHandler` 处理事件 → `ConsumerNetworkThread` 做网络 IO → `FetchBuffer` 跨线程传递
- `ConsumerNetworkThread` 主循环：`requestManagers.poll()` → `networkClientDelegate.poll()`

### 失败路径

- Classic 的 poll 阻塞太长 → 消费者心跳超时 → rebalance
- Async 的 applicationEventHandler 阻塞 → 后台事件无法处理
- FetchBuffer 无锁 → 跨线程访问竞争
- 两套模型 API 不一致 → 用户困惑

### 连接点

- 前文 `Kafka-4`：Consumer poll 主链，用 Classic 模型讲解。
- 前文 `Kafka-6`：ConsumerGroup 协调，Async 模型是 KIP-848 的默认实现。
- 后文：K-4 Consumer 域第 3 篇（offset 提交与位置推进）。

## 4. 总图

```text
ClassicKafkaConsumer
  poll()
    → updateAssignmentMetadataIfNeeded（协调器）
      → sendFetches()（网络 IO 在当前线程）
        → client.poll()（等待响应）
          → collectFetch()（拿结果）

AsyncKafkaConsumer
  poll()
    → ApplicationEventHandler 处理事件
      → ConsumerNetworkThread（后台线程）
        → requestManagers.poll()
          → networkClientDelegate.poll()
            → 结果写入 FetchBuffer
              → 应用线程读取
```

## 5. 关键边界

- 本篇不重复 ConsumerGroup 协调细节（Kafka-6），只讲两套模型差异。
- 不把 AsyncKafkaConsumer 写成"完全异步非阻塞"：poll() 仍然可以阻塞等待数据。
- 不把 ClassicKafkaConsumer 写成"过时的"：它仍然是默认 Consumer 实现，Async 是逐步取代。
- 不展开 ApplicationEventHandler 全部事件类型。

## 6. 失败方案推演

1. **所有网络 IO 都在应用线程**：poll 阻塞时心跳也阻塞，导致 rebalance。
2. **所有网络 IO 都在后台线程**：应用线程无法控制 fetch 时机。
3. **没有 FetchBuffer 隔离**：后台线程写结果时应用线程读，线程安全问题。
4. **API 层不统一**：用户需要选择不同的 Consumer 实现。

## 7. 误解清单

- “ClassicKafkaConsumer 是同步的”：它每步都在应用线程执行，但 poll 内部会等待网络 IO。
- “AsyncKafkaConsumer 是完全异步的”：poll() 仍然可以阻塞等待数据。
- “ConsumerNetworkThread 只做 fetch”：它还处理心跳、协调器、offset 提交等。
- “FetchBuffer 是线程安全的”：通过 volatile 和原子操作保证跨线程可见性。
- “AsyncKafkaConsumer 是默认的”：Classic 仍然是默认，Async 是 KIP-848 路线。

## 8. 证据清单

- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ClassicKafkaConsumer.java:116`：ClassicKafkaConsumer 类。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ClassicKafkaConsumer.java:631`：poll() 主循环。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ClassicKafkaConsumer.java:674`：sendFetches。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ClassicKafkaConsumer.java:679`：updateAssignmentMetadataIfNeeded。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/AsyncKafkaConsumer.java:172`：AsyncKafkaConsumer 类。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/AsyncKafkaConsumer.java:819`：poll() 主循环。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerNetworkThread.java:53`：ConsumerNetworkThread。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerNetworkThread.java:133`：requestManagers.poll() 主循环。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerNetworkThread.java:164`：networkClientDelegate.poll()。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦两套 Consumer 线程模型，不展开全部事件类型与 rebalance 协议。
- 目标正文：6000~10000 字。

## 10. 本轮重写主线

1. 从"为什么同是 poll() 背后有两种实现"开场。
2. 否定"全在应用线程"和"全在后台线程"两种极端方案。
3. 解释 ClassicKafkaConsumer 的同步 poll 主链。
4. 解释 AsyncKafkaConsumer 的事件驱动 + 后台线程。
5. 对比两套模型的线程模型差异。
6. 解释 FetchBuffer 的跨线程安全。
7. 收网：Classic 适合简单场景，Async 是 KIP-848 路线。