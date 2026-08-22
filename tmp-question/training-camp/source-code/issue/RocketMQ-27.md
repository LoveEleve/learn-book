# RocketMQ-27. Topic 为什么天然分片——Queue 数据分片与路由选择主链

> 场景：前面我们已经补了 Producer 路由发现（`RocketMQ-3`）、NameServer 架构（`RocketMQ-16`）和 Broker 注册（`RocketMQ-17`）。但还有一个关键问题一直没单独讲透：Producer 查到 `TopicRouteData` 以后，为什么最后不是“发给某个 Topic”，而是“发给某个 MessageQueue”？Topic 为什么天然就是多 queue 分片，而不是一个整块概念？本篇把 Topic 分片与选队列主链单独讲透。

## 先把真正的困惑摆出来：为什么 RocketMQ 不是“一个 Topic 一份日志”

如果把 Topic 理解成一个单体概念，就会自然地以为：

- Producer 只要知道 Topic 在哪个 Broker 上；
- 之后直接往那个 Topic 里追加消息；
- 顺序消费也应该天然是“整个 Topic 全局有序”。

但 RocketMQ 并不是这样设计的。Producer 最终选中的不是一个抽象的“Topic”，而是具体的 `MessageQueue`。更准确地说，Topic 的可写/可读并行性，是通过多个 `MessageQueue` 分片暴露出来的，而不是把 Topic 本身直接当成一条独立物理日志来对待。

*关键设计（斜体）：* *RocketMQ 不把 Topic 当作单一日志，而是把它天然拆成多个 queue 分片；NameServer 返回的 `TopicRouteData` 里既有 `QueueData` 也有 `BrokerData`，Producer 再把它们展开成可发送的 `MessageQueue` 列表，并按普通轮询、故障规避或顺序选择器选中某一个 queue。并发、路由、局部有序与故障切换，都是建立在这层 queue 分片之上的。*[模式: TopicRouteData 路由视图 + Queue 分片 + MessageQueue 发送选择]

## 第一层：`TopicRouteData` 给的不是“一个 Topic 对应一个地址”

当 Producer 向 NameServer 查询某个 topic 时，拿到的不是“这个 topic 在 10.0.0.1:10911”这种单值答案。

它拿到的是 `TopicRouteData`：

- `QueueData`：这个 topic 在各 Broker 上配置了多少读写队列；
- `BrokerData`：这些 Broker 的名称、地址、主从关系等。

所以从查询结果开始，RocketMQ 就已经把 Topic 建模成：

```text
topic
  → 多个 QueueData
    → 分布在多个 BrokerData 上
```

这和“单 Topic 单日志”的心智模型完全不同。

## 第二层：`QueueData` 代表的是 Topic 在某个 Broker 上的分片配置

`QueueData` 的价值不是简单保存一个数字，而是表达：

- 某个 topic 在某个 brokerName 上有多少读队列、写队列；
- 这些 queue 是否可读、可写；
- 它属于哪个 Broker 分片承载。

也就是说，Topic 的“分片”不是抽象概念，而是直接体现在 `QueueData` 这层配置里。

所以 RocketMQ 的并发能力，本质上不是“一个 Topic 自己 magically 并发”，而是：

- 一个 topic 被拆成多个 queue；
- queue 分散在不同 Broker；
- Producer/Consumer 在 queue 粒度上并行工作。

## 第三层：Producer 真正发送时使用的是 `MessageQueue`

Producer 不会直接把 `QueueData` 拿来发消息。中间还有一步关键转换：把路由视图展开成可发送的 `MessageQueue` 列表。

`MessageQueue` 可以理解成真正参与发送决策的最小单位，至少包含：

- topic
- brokerName
- queueId

这一步非常重要，因为它把“路由视图”转成了“可选发送目标列表”。

换句话说：

- `TopicRouteData` 解决“这个 topic 的全局路由长什么样”；
- `MessageQueue` 解决“这次具体往哪个分片发”。

## 第四层：普通发送为什么可以并发——因为本质上是在多个 queue 上分摊

普通发送场景下，Producer 会基于 `TopicPublishInfo` 持有可发送的 `MessageQueue` 列表，然后从中选择一个 queue 发送。

这里不能把它偷换成“永远只是简单轮询”。更准确地说：默认发送路径会沿着这份队列列表做选择，而在重试、`lastBrokerName`、故障规避等场景下，还会叠加额外约束。

这意味着 RocketMQ 的发送并发不是“一个 Topic 内部自己扩容”，而是：

- Topic 先拆成多个 queue；
- Producer 再把消息分散到这些 queue 上。

所以如果一个 topic 只有 1 个 queue，再多 producer 也很难获得良好的分片并行性；反过来，queue 数足够时，Producer 才有足够的分发空间。

## 第五层：顺序消息为什么只能保证“分片内有序”

这也是 `RocketMQ-8` 的根问题来源。

既然 RocketMQ 的发送目标是 `MessageQueue`，顺序消息天然只能落在“某一个选定 queue 内”维护顺序。

所以顺序消息的关键不是 broker 自动帮你做全局排序，而是：

- 相同业务键通过同一套选择逻辑映射到同一个 `MessageQueue`；
- 然后只在这个 queue 内维持顺序。

这就是为什么 RocketMQ 的顺序语义本质上是：**分片内有序，不是 Topic 全局有序。**

## 第六层：故障规避与路由缓存，都是在 queue 分片模型上叠加出来的

前面讲过：Producer 会维护本地路由缓存，也会做故障规避（`MQFaultStrategy`）。

这两者都建立在 queue 分片模型上：

- 路由缓存原始上更接近缓存 `TopicRouteData` / `TopicPublishInfo` 这类结构，最终才会展开成多个 `MessageQueue` 的可发送视图；
- 故障规避不是“整个 Topic 不发了”，而是基于这份队列视图避开某些 Broker/queue，改选别的分片。

所以 queue 分片不是一个孤立实现细节，而是把 NameServer 路由、Producer 发送、顺序语义、故障切换全串起来的基础模型。

## 收网：RocketMQ 的 Topic 从设计上就是多 queue 分片

把整篇压成一句话：RocketMQ 不把 Topic 直接当成单一物理日志来发送，而是通过多个 `MessageQueue` 暴露它的分片并行性；NameServer 通过 `TopicRouteData` 返回 `QueueData` 与 `BrokerData` 路由视图，Producer 再把这份视图组织成 `TopicPublishInfo` 并最终展开为 `MessageQueue` 列表，在默认队列选择的基础上叠加故障规避或顺序选择器等约束来选中具体 queue。并发发送、局部有序与故障切换，都是建立在这层 queue 分片之上的。

```text
NameServer 返回 TopicRouteData
  → QueueData + BrokerData
    → Producer 构造 TopicPublishInfo
      → 展开 MessageQueue 列表
        → 选择某个 queue 发送
          → 普通发送并行 / 顺序发送分片内有序
```

**本篇的一句话困惑**：RocketMQ 为什么不是“一个 Topic 一份日志”，Producer 为什么最终总是在选 `MessageQueue`？

**本篇的一句话顿悟**：RocketMQ 的 Topic 从设计上就是多 queue 分片；`TopicRouteData` 给的是分片路由视图，Producer 再把它展开成 `MessageQueue` 列表做具体发送决策，因此并发、顺序和故障切换都建立在 queue 粒度上。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“Topic = 单一物理日志。”** RocketMQ 天然把 Topic 拆成多个 queue 分片。
2. **“Producer 只需要知道 broker 地址。”** 真正发送决策落在 `MessageQueue` 粒度。
3. **“顺序消息是 broker 自动帮你做 Topic 全局排序。”** 本质上只是同一 queue 内有序。
4. **“QueueData 和路由缓存没关系。”** 路由缓存最终缓存的就是由这些分片信息展开出的可发送视图。
5. **“故障切换是整个 Topic 级别的。”** 更多是在 queue / broker 分片粒度上重新选择。

### 关键证据清单

- `remoting/src/main/java/org/apache/rocketmq/remoting/protocol/route/TopicRouteData.java`：Topic 路由视图。
- `remoting/src/main/java/org/apache/rocketmq/remoting/protocol/route/QueueData.java`：Topic 在 Broker 上的队列配置。
- `remoting/src/main/java/org/apache/rocketmq/remoting/protocol/route/BrokerData.java`：Broker 路由信息。
- `common/src/main/java/org/apache/rocketmq/common/message/MessageQueue.java`：发送目标粒度。
- `client/src/main/java/org/apache/rocketmq/client/impl/producer/TopicPublishInfo.java`：Producer 持有的可发送队列视图。
- `client/src/main/java/org/apache/rocketmq/client/impl/producer/DefaultMQProducerImpl.java`：发送时选择队列。

### 版本与实现边界

- 本文以 RocketMQ `5.x` / `4.x` 通用主链为基线。
- 本篇聚焦 Topic 分片与 Producer 选队列，不展开故障规避细节（见 `RocketMQ-22`）。
- 不把 Queue 分片语义和顺序消息的 broker 侧锁机制混成一层。

### 前置依赖与后续桥接

- 前置依赖：`RocketMQ-3`（路由发现与发送）、`RocketMQ-16/17`（NameServer 与注册）、`RocketMQ-8`（顺序消息）。
- 后续桥接：可继续补 `RocketMQ-28`（存储架构总览）与 `RocketMQ-29`（Consumer 架构总览）。