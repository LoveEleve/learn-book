# Kafka-20. 两代 Consumer 模型——ClassicKafkaConsumer 的同步 poll 与 AsyncKafkaConsumer 的事件驱动主链

> 场景：Kafka-4 讲 Consumer 的 poll 主链时，我们默认用了 `ClassicKafkaConsumer` 的同步模型。但 Kafka 还有另一套 `AsyncKafkaConsumer`，它把网络 IO 挪到后台专用线程。本篇把两套模型摆到一起对比：它们的 `poll()` 长什么样、线程怎么划分、`FetchBuffer` 怎么跨线程传递结果、以及为什么 Async 是 KIP-848 的路线。这是 K-4 Consumer 域第 2 篇。

## 先把真正的困惑摆出来：都是 poll()，为什么背后有两种实现

用户调用 `consumer.poll(Duration)`，得到一批 `ConsumerRecords`。同一个 API，但 Kafka 内部有两个实现：

- `ClassicKafkaConsumer`：**应用线程**自己完成协调、fetch、等待、收集的同步循环；
- `AsyncKafkaConsumer`：**后台线程**做网络 IO，应用线程通过事件处理器与它通信。

为什么要有两套？因为它们在一条关键问题上选择了不同答案：**poll() 阻塞时，谁来做网络 IO？**

Classic 的做法：poll() 里的所有事（心跳、fetch、等待响应）都在应用线程串行执行。好处是简单，坏处是应用线程被网络等待占用。

Async 的做法：把网络 IO 交给 `ConsumerNetworkThread` 专用线程，应用线程只负责"发出事件、收事件、处理结果"。应用线程不再被网络底层卡住。

```text
Classic：应用线程自己等网络
Async  ：后台线程等网络，应用线程发/收事件
```

如果只有一种实现，主链会先在哪卡住？Classic 在 `poll(timeout)` 里等待 fetch 响应时，如果有其他后台需求（心跳）也要等同一个线程；Async 则把网络等待和业务处理解耦。

*关键设计（斜体）：* *ClassicKafkaConsumer 在应用线程里同步完成"协调 → fetch 发送 → 网络等待 → 结果收集"；AsyncKafkaConsumer 由 `ConsumerNetworkThread` 专用线程做网络 IO，应用线程通过 `ApplicationEventHandler` 与它通信，fetch 结果放入线程安全的 `FetchBuffer` 供应用线程读取。*[模式: 同步循环 vs 事件驱动 + 后台线程]

## 第一层：ClassicKafkaConsumer 的 poll() 是单线程同步循环

`ClassicKafkaConsumer` 的 `poll()` 主循环逻辑很直（`ClassicKafkaConsumer.java:631`）：

```text
poll(timer)
  → updateAssignmentMetadataIfNeeded(timer, false)   // 协调器 + 位置更新
    → pollForFetches(timer)                            // 收集已就绪的 fetch
      → 没有就绪数据？
        → sendFetches()                                // 发新的 fetch
          → client.poll(timer)                         // 网络等待
            → collectFetch()                           // 拿结果
              → 返回 ConsumerRecords
```

这里每一行都在**应用线程**执行。`sendFetches()` 调用 `fetcher.sendFetches()`（`ClassicKafkaConsumer.java:674`），`client.poll()` 是 `ConsumerNetworkClient` 的网络等待，`collectFetch()` 从 `FetchBuffer` 拿数据。

关键点：**Classic 的 fetch 是"你调 poll，我就在当前线程等网络"。** `client.poll(timer)` 会阻塞应用线程直到有响应或超时。

这带来一个行为：如果应用只调用 `poll()` 而没有其他后台线程，心跳、coordinator 协调、offset 提交也都在 poll 里串行做。只要 poll 及时被调用，就都能维持。

## 第二层：Classic 的"同步"不等于"不阻塞"

很多人听到 Classic 是同步循环，以为它是"调一次就返回"。实际不是。

Classic 的 `poll(timeout)` 在超时时间内可能：

1. 先做 `updateAssignmentMetadataIfNeeded`：如果组需要 rebalance，这里可能先加入组；
2. 再 `sendFetches()`：发出 fetch 请求；
3. 再 `client.poll(...)`：等待网络响应或超时；
4. 如果一直没数据，可能反复 poll 直到超时。

所以"同步"指的是**所有操作都在同一个应用线程里同步地做**，而不是"不等待"。它会阻塞应用线程，等待网络 IO 或超时。

如果应用在 poll 之间隔太久才调用，Classic 的心跳会过期 → broker 认为消费者掉了 → rebalance。这是 Classic 模型下应用必须保持"及时调用 poll"的原因。

## 第三层：AsyncKafkaConsumer 把网络 IO 挪到 ConsumerNetworkThread

`AsyncKafkaConsumer` 引入了一个关键新线程：`ConsumerNetworkThread`（`ConsumerNetworkThread.java:53`）。

它的主循环负责所有网络 IO：

```text
ConsumerNetworkThread.run()
  → 遍历每个 RequestManager
    → requestManager.poll(currentTime)      // 每个 manager 产出待发请求
      → networkClientDelegate.poll(...)      // 统一发网络请求、收响应
```

`RequestManager` 包括：

- 心跳（heartbeat）
- fetch（FetchRequestManager）
- 协调器（coordinator）
- offset 提交（CommitRequestManager）

它们不再由应用线程串行调度，而是由 `ConsumerNetworkThread` 统一 poll 并驱动。应用线程通过 `ApplicationEventHandler` 提交事件（比如 poll、subscribe、commit），后台线程产出事件（比如 fetch 响应、错误、回调）。

这就是 Async 与 Classic 最本质的差别：**网络 IO 的请求/响应发动者，从应用线程变成了专用后台线程。**

## 第四层：Async 的 poll() 变成"处理后台事件 + 收集 fetch 结果"

`AsyncKafkaConsumer.poll()` 的逻辑仍然是"协调 + fetch 收集 + 返回"，但网络动作由后台线程代劳：

```text
poll(timer)
  → ApplicationEventHandler 处理背景事件（错误、rebalance 回调等）
    → updateAssignmentMetadataIfNeeded(timer)
      → pollForFetches(timer)
        → 有已就绪数据？返回
        → 没有？sendFetches（通过 handler 发请求）
          → 等待后台线程产出 fetch 响应
            → 从 FetchBuffer 收集结果
```

区别在于 `sendFetches()` 不再是"在当前线程发完就 poll 网络"，而是通过事件处理器请求 `ConsumerNetworkThread` 去发；数据就绪后放进 `FetchBuffer`，应用线程从那里拿。

`ApplicationEventHandler` 负责把 fetch、commit、subscribe 等请求转为事件，加入后台线程的事件队列，等待 `ConsumerNetworkThread` 执行。

在 Async 模式下，`FetchBuffer` 是在应用线程和 `ConsumerNetworkThread` 之间共享的线程安全结构（`AsyncKafkaConsumer.java:428`）：后台线程（`AbstractFetch`）写入 CompletedFetch，应用线程通过 `collectFetch` 读取消费。注意在 Classic 模式里，FetchBuffer 的写和读都是应用线程自己（单线程内生产者-消费者），不跨线程。

## 第五层：两套模型如何选，Async 为什么代表 KIP-848 方向

`ClassicKafkaConsumer` 和 `AsyncKafkaConsumer` 暴露给用户的是**同一个 `KafkaConsumer` API**，只是内部实现不同。选择哪套由 `ConsumerDelegateCreator` 根据 `group.protocol` 配置决定：`classic`（默认）用 Classic，`consumer` 用 Async（`ConsumerDelegateCreator.java:45-46`）。

- Classic：实现简单直接，适合大多数场景，是当前默认实现（`DEFAULT_GROUP_PROTOCOL = "classic"`）；
- Async：为 KIP-848（new consumer group protocol）和新版事件驱动设计，让网络 IO 不占用应用线程，是未来方向。

`AsyncKafkaConsumer` 类注释说得明确：它属于 KIP-848 修订版 consumer group 协议的一部分，不应被直接实例化，用户仍创建 `KafkaConsumer` 即可（`AsyncKafkaConsumer.java:159-170`）。

所以两套模型不是让用户二选一的"两个 API"，而是同一个 API 下两种内部架构：Classic 先保证兼容，Async 朝新协议演进。

## 收网：Classic 在应用线程事必亲为，Async 让网络线程跑腿

把整篇压成一句话：`ClassicKafkaConsumer` 在应用线程里同步完成"协调 → fetch → 网络等待 → 收集"；`AsyncKafkaConsumer` 把网络 IO 交给 `ConsumerNetworkThread` 专用线程，应用线程通过 `ApplicationEventHandler` 收发事件、从线程安全的 `FetchBuffer` 拿结果。两者暴露同一个 `KafkaConsumer` API，Async 是 KIP-848 的演进路线。

```text
ClassicKafkaConsumer
  应用线程：poll → 协调 → sendFetches → client.poll 等网络 → collectFetch

AsyncKafkaConsumer
  ConsumerNetworkThread：requestManager.poll → networkClientDelegate.poll
  应用线程：poll → 处理事件 → 从 FetchBuffer 收集
```

到这里，主线只发生了五件事。

第一，Classic 在应用线程同步完成整条 poll 主链。

第二，Classic 的"同步"不等于"不阻塞"，网络等待仍在应用线程。

第三，Async 用 ConsumerNetworkThread 把网络 IO 挪到后台。

第四，Async 的 poll 变成事件驱动 + 从 FetchBuffer 收集。

第五，两者暴露同一 API，Async 是 KIP-848 方向。

**本篇的一句话困惑**：都是 poll()，Classic 和 Async 有什么区别？

**本篇的一句话顿悟**：Classic 在应用线程同步做所有网络 IO；Async 把网络 IO 交给后台 `ConsumerNetworkThread`，应用线程通过事件与 `FetchBuffer` 交互——同一 API，两种线程模型，Async 是新协议路线。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“ClassicKafkaConsumer 是同步的意味着不阻塞。”** 它同步是"所有操作在同一应用线程"，但会阻塞等待网络。
2. **“AsyncKafkaConsumer 是完全异步的。”** poll() 仍可能阻塞等待数据，只是网络 IO 在后台线程。
3. **“ConsumerNetworkThread 只做 fetch。”** 它还驱动心跳、协调器、offset 提交等 RequestManager。
4. **“两套模型是两个不同 API。”** 它们暴露同一个 `KafkaConsumer` API。
5. **“AsyncKafkaConsumer 是默认实现。”** Classic 仍是默认（`DEFAULT_GROUP_PROTOCOL = "classic"`），Async 是 KIP-848 演进路线。由 `group.protocol` 配置切换。

### 关键证据清单

- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ClassicKafkaConsumer.java:116`：ClassicKafkaConsumer 类。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ClassicKafkaConsumer.java:631`：poll() 主循环。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ClassicKafkaConsumer.java:674`：sendFetches。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ClassicKafkaConsumer.java:679`：updateAssignmentMetadataIfNeeded。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/AsyncKafkaConsumer.java:172`：AsyncKafkaConsumer 类（KIP-848）。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/AsyncKafkaConsumer.java:819`：poll() 主循环。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerNetworkThread.java:53`：ConsumerNetworkThread。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerNetworkThread.java:133`：requestManagers.poll() 主循环。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerNetworkThread.java:164`：networkClientDelegate.poll()。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerDelegateCreator.java:45`：group.protocol 决定 Classic/Async 选择。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦两套 Consumer 线程模型，不展开全部事件类型与 rebalance 协议。
- 不把 Classic 写成"过时的"：它仍是默认实现，Async 是渐进路线。
- 不把 FetchBuffer 的线程安全机制展开为逐字段分析，只说明它是跨线程共享结构。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-4`（Classic poll 主链）、`Kafka-6`（KIP-848 ConsumerGroup）。
- 后续桥接：下一篇进入 K-4 Consumer 域第 3 篇（offset 提交与位置推进）。