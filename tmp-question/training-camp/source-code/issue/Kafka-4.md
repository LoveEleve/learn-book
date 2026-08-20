# Kafka-4. KafkaConsumer.poll() 为什么不是“拉一批消息回来”——Fetcher、FetchBuffer 与 offset 推进主链

> 场景：开篇总图已经把 Kafka Consumer 放在消息主链的最后一段：Broker 分区日志已经成立，Consumer 接下来要按 offset 拉取，并把自己的消费进度继续推进。到了这里，读者很容易把 `poll()` 理解成一个简单动作：发一个 fetch 请求，拿一批消息回来，然后交给业务。
>
> 本篇只回答一个问题：**KafkaConsumer.poll() 为什么不是“发一个 fetch 请求，拉一批消息回来”这么简单。** 本篇聚焦经典 `ClassicKafkaConsumer` 的拉取与位移推进：assignment/position 准备、Fetcher、FetchBuffer、FetchCollector、`position` 与 `commitSync/commitAsync`；不展开 ConsumerGroup 的完整 rebalance 协议，也不把 AsyncKafkaConsumer 的内部事件模型混进主线。

## 先把真正的困惑摆出来：poll 为什么不是一次同步 fetch RPC

从业务代码看，Kafka Consumer 的使用方式很直接：

```text
while (running) {
    records = consumer.poll(timeout)
    for (record : records) {
        process(record)
    }
}
```

于是最容易形成的直觉就是：每次 `poll()` 只做一件事——向 Broker 发 fetch 请求，等一批数据回来，再返回给业务线程。

但 Kafka 的 Consumer 主链并不是一条“调用一次、同步取一次”的直线。因为 `poll()` 每次进入时，都要同时面对几种可能：

- 本地 `FetchBuffer` 里已经有上一轮返回的数据；
- 当前分区 assignment 还没准备好；
- position 还没有初始化或需要根据 metadata 更新；
- fetch 请求已经在路上，不能重复发；
- Broker 返回的数据已经到达网络线程，但还没有被应用线程收集；
- 业务已经处理过消息，但 committed offset 还没有写入系统。

这意味着 `poll()` 真正管理的不是“一批消息对象”，而是一条持续运行的 Consumer 状态链：

```text
assignment / position
  → fetch request
    → network response
      → FetchBuffer
        → FetchCollector
          → position 前进
            → commitSync / commitAsync
```

如果把 poll 只写成一次同步 RPC，主链会先在哪失真？会看不见：

- Fetcher 为什么可以提前发送下一轮请求；
- FetchBuffer 为什么要在网络线程和应用线程之间承接结果；
- position 为什么和 committed offset 分开存在；
- Consumer 为什么即使已经看见消息，也可能还没有把进度正式写入系统。

所以 Kafka Consumer 的第一性问题不是“这次取了几条消息”，而是：**当前这条分区日志的阅读状态怎样持续向前推进。**

*关键设计（斜体）：* *KafkaConsumer.poll() 的核心不是把一批消息搬回应用，而是把 assignment、fetch、buffer、position 和 commit 这些状态沿着同一条消费链继续推进；消息只是这条状态链上的一个阶段性结果。*[模式: 日志位置推进 + fetch 中间态 + offset 提交]

## 第一层：Consumer 先确认 assignment 与 position，才能知道从哪里 fetch

Consumer 拉取并不是拿着 topic 就能开始。它首先要知道：

- 当前负责哪些 partition；
- 每个 partition 当前应该从哪个 position 读取；
- position 是否已经根据 metadata、已提交 offset 或 `auto.offset.reset` 准备完成。

在 `ClassicKafkaConsumer.poll()` 里，应用线程会先调用 `updateAssignmentMetadataIfNeeded(timer, false)`。这里真正的第一道闸门不是 position，而是 `coordinator.poll(...)`：只有协调器允许这轮消费继续推进，Consumer 才会再进入 `updateFetchPositions(timer)`。也就是说，`poll()` 不是“先随便拉再看组状态”，而是**先过协调前置闸门，再过位置前置闸门，最后才有资格进入 fetch 世界。**

这里必须控制本篇边界：我们不在这里展开 JoinGroup、SyncGroup、ClassicGroup、ModernGroup 的完整协议；本篇只需要看清一个事实：**Consumer 不能在消费组前置状态、分区归属和读取位置都没准备好的时候，直接向 Broker 发 fetch。**

如果没有这层前置准备，主链会先在哪失败？会失败在“请求发出去了，但没有明确的日志位置”：

- 不知道应该向哪个 partition 拉；
- 不知道应该从哪个 offset 开始；
- rebalance 或 metadata 变化后，旧 position 可能已经不再合法。

所以 Consumer 拉取的第一层不是 Fetcher，而是 assignment/position 前置状态。

## 第二层：`pollForFetches()` 先消费已有结果，再决定是否发送新 fetch

准备好 assignment 和位置以后，`poll()` 不会无条件地先发一轮 fetch。它会进入 `pollForFetches(timer)`，先尝试从已有的 fetch 结果里收集数据：

```text
pollForFetches()
  → fetcher.collectFetch()
    → 如果已有可返回数据，直接返回
    → 否则才 sendFetches()
```

这个顺序很关键。因为网络请求可能早就已经发出，Broker 响应也可能已经进入 Consumer 的中间缓冲。如果每次 poll 都不看已有结果、只顾着重新发请求，主链会产生重复请求和无意义等待。

如果本地已有 fetch 结果却仍然先发新请求，主链会先在哪浪费？会浪费在：

- 已经到达的数据不能及时交给应用；
- 对同一 partition 产生额外 fetch 压力；
- Consumer 的 poll 延迟被不必要地拉长。

所以 `pollForFetches()` 的第一条纪律是：**先收集已有结果，再决定是否需要发新请求。**

## 第三层：Fetcher 负责发 fetch 请求，但不直接把记录交给业务线程

当 `collectFetch()` 没有可返回数据时，Consumer 才会调用 `sendFetches()`，进入 `Fetcher.sendFetches()`。

`Fetcher` 的职责可以拆成两段：

- 根据当前 assignment、position 和每个 Broker 的 partition 集合准备 FetchRequest；
- 通过 `ConsumerNetworkClient` 把请求发送出去，并为成功/失败响应注册处理器。

它的核心不是“拉数据”，而是把 Consumer 当前的逻辑阅读状态翻译成网络请求：

```text
partition position map
  → FetchRequest
    → ConsumerNetworkClient.send()
      → broker response callback
```

而且 Fetcher 还有一个重要并发边界：请求和响应可能由不同线程处理，单个 node 同时最多保持一个 pending fetch。这样做是为了让处理响应时更新的 epoch、session 和 partition 状态，能在下一轮请求构造时被正确看到。

如果 Fetcher 直接把响应交给业务线程，主链会先在哪失控？网络线程就会被迫承担反序列化、消费记录组装、应用回调和业务节奏管理，Consumer 的网络推进与应用处理会互相耦合。

Kafka 选择的是：Fetcher 接收和处理 fetch 响应，但先把结果放进 `FetchBuffer`，再由 `poll()` 所在的应用线程收集。

## 第四层：FetchBuffer 是网络线程与应用线程之间的消费承接平面

`FetchBuffer` 的职责在类注释里已经非常明确：它缓冲 Broker 响应形成的 `CompletedFetch`，由后台/网络侧生产，再由应用侧消费。

它不是消息长期存储，也不是 Consumer offset 提交表，而是一个中间承接平面：

```text
Fetcher response callback
  → FetchBuffer.add(CompletedFetch)
    → poll() / FetchCollector.collectFetch()
      → 应用线程取出并转换成 ConsumerRecords
```

这个中间层补了两个现实问题：

### 1. 网络到达时间和业务 poll 时间不一致

Broker 响应可能在业务线程下一次调用 `poll()` 之前就到达。没有 FetchBuffer，网络线程就没有地方安全地放下已完成的 fetch 结果。

### 2. 一个 fetch 响应还不是最终业务记录

`CompletedFetch` 还需要经过 `FetchCollector` 处理，按当前 position、分区状态、反序列化和可见性规则转换为应用能消费的 `ConsumerRecords`。

所以 FetchBuffer 不是“多了一层队列”，而是把两个不同节奏的世界隔开：

```text
Broker 响应可能先被非应用线程承接
应用线程：按 poll 节奏收集与返回记录
```

这里也要把话收紧：Fetcher 注释强调的是“响应可能跨线程处理”，而不是每次都固定有一个独立响应线程。更准确的说法是，fetch 结果可能先被非应用线程承接，而应用线程再按 poll 节奏收集。没有这层承接平面，主链会先在哪出现线程边界问题？要么响应处理路径被业务消费拖住，要么应用线程只能同步等网络响应，Kafka 的异步 fetch/prefetch 能力就会被削弱。

## 第五层：FetchCollector 把“收到 fetch 响应”翻译成 position 前进

FetchBuffer 里有了 `CompletedFetch`，还不等于 Consumer 已经真正推进了读取状态。

`FetchCollector.collectFetch(fetchBuffer)` 还要做一层重要翻译：

```text
CompletedFetch
  → 解析 record batch
    → 按 TopicPartition 检查当前 position
      → 生成 ConsumerRecord
        → 更新下一次 position
```

这一步把“网络上收到了一段数据”转换成“Consumer 的日志阅读位置已经向前移动”。

为什么不能在 Fetcher 收到响应时就直接更新 position？因为响应到达不等于记录已经按照应用线程当前的消费语义被收集：

- 可能存在 rebalance 后已失效的 partition；
- 可能需要跳过不再属于当前 assignment 的数据；
- 可能需要按当前 position 过滤已经不适用的记录；
- 可能有反序列化或可见性边界需要在收集时处理。

所以 Consumer 的 position 前进应该发生在“记录被正确收集并准备交给应用”的语义层，而不是简单发生在网络响应到达那一刻。

这也解释了为什么 Kafka Consumer 的 position 是一个持续变化的本地阅读状态，而不是 Broker 返回的某个静态字段。

## 第六层：position 前进不等于 committed offset 已经写入系统

这是 Consumer 主链里最重要的边界之一。

Consumer 至少有两种不同的进度：

### position

表示 Consumer 当前下一次准备从哪里继续读取。它随着 `FetchCollector` 收集记录而前进。

### committed offset

表示 Consumer 已经通过 `commitSync()` 或 `commitAsync()` 把进度正式送入 Kafka 的位移提交链。对消费组模式来说，这条链后续会进入 `__consumer_offsets` 所代表的位移存储世界，但这一篇先只守住“本地阅读进度”与“系统正式记住的进度”是两层事实，不提前展开消费组位移存储细节。 

两者之间可以存在窗口：

```text
消息已经返回给业务
  → position 已经前进
    → 业务处理完成
      → commitSync / commitAsync
        → committed offset 才更新
```

如果把 position 直接当 committed offset，主链会先在哪失真？会在故障恢复和重新分配时失真：

- Consumer 可能已经拉到并处理了消息，但还没有 commit；
- 进程崩溃后，系统只能从上一次 committed offset 恢复；
- 这可能导致重复消费，也可能让业务误以为消息已经被系统永久记住。

所以 Kafka 的 Consumer 主链不能只写“拉到消息以后 position 前进”，还必须明确：**本地阅读进度和系统持久化的消费进度是两层事实。**

## 第七层：commitSync/commitAsync 是位移提交链，不是 poll 的附属返回值

`ClassicKafkaConsumer.commitSync()` 会从订阅状态里拿到已消费的 offset，再通过 `ConsumerCoordinator.commitOffsetsSync(...)` 提交；`commitAsync()` 则把提交请求异步交给 coordinator。

这两个动作都建立在 position/已消费状态已经存在的前提上，但它们不属于 `poll()` 的同一个阶段：

- `poll()` 负责拉取、收集和推进本地 position；
- `commitSync/commitAsync` 负责把某个消费进度写进系统位移存储。

为什么必须拆开？因为应用处理消息的业务事务和 Kafka 位移提交之间，本来就存在一个需要由业务决定的边界：

```text
先处理业务
  → 再提交 offset
```

或者业务也可能选择先提交再处理，但那会带来不同的重复/丢失语义。Kafka 客户端不能把这个业务边界偷偷合并进 poll。

所以 commit API 的存在不是“poll 返回后顺手保存一下”，而是 Kafka 把消费位置正式写入系统的独立控制动作。

## 收网：Kafka Consumer 管理的首先不是消息对象，而是分区日志阅读状态

如果把整篇压成一句话，KafkaConsumer.poll() 不是一次同步 fetch RPC，而是一条围绕 assignment、position、Fetcher、FetchBuffer、FetchCollector 和 committed offset 持续推进的状态链：Consumer 先准备从哪个 partition/offset 读，再发 fetch、承接响应、推进本地 position，最后由 commitSync/commitAsync 把进度正式写入 `__consumer_offsets`。

```text
poll(timeout)
  → assignment / position 准备
    → 先收集 FetchBuffer 已有结果
      → 没有结果才 sendFetches()
        → Fetcher 发请求并接收响应
          → FetchBuffer 承接 CompletedFetch
            → FetchCollector 生成记录并推进 position
              → commitSync/Async 写入 committed offset
```

到这里，主线只发生了五件事。

第一，Consumer 先准备 assignment 与 position，才知道 fetch 从哪里开始。

第二，`poll()` 先消费已有 fetch 结果，再决定是否发送新 fetch。

第三，Fetcher 负责网络请求与响应承接，FetchBuffer 把网络线程和应用线程隔开。

第四，FetchCollector 把 CompletedFetch 翻译成记录和 position 前进。

第五，position 前进不等于 committed offset 更新，commit 是独立的消费进度持久化动作。

**本篇的一句话困惑**：KafkaConsumer.poll() 为什么不是“拉一批消息回来”这么简单？

**本篇的一句话顿悟**：因为 Kafka Consumer 真正管理的不是一批消息对象，而是分区日志上的持续阅读状态；poll 只是驱动 assignment、fetch、buffer、position 和 commit 这条状态链继续向前。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“poll() 就是一轮同步 fetch RPC。”** 它会先消费已有 buffer，并协调持续的 fetch 状态。
2. **“Fetcher 收到响应就等于业务拿到记录。”** 结果还要进入 FetchBuffer 与 FetchCollector。
3. **“position 就是 committed offset。”** position 是本地阅读位置，commit 才是系统位移事实。
4. **“commitSync/commitAsync 是 poll 的返回附属动作。”** 它们是独立的 offset 提交链。
5. **“ConsumerGroup 和 Consumer 拉取是同一篇主题。”** assignment 是前置条件，但成员协调是后续控制面专题。

### 关键证据清单

- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ClassicKafkaConsumer.java:624`：poll 总入口。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ClassicKafkaConsumer.java:640`：assignment/position 前置更新与 poll 循环。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ClassicKafkaConsumer.java:690`：先 collectFetch，再 sendFetches 与等待。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ClassicKafkaConsumer.java:726`：commitSync 入口；本篇先把它作为位移提交链入口证据，不继续把内部存储世界展开到 `__consumer_offsets` 细节。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/Fetcher.java:105`：发送 FetchRequest。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/Fetcher.java:145`：从 FetchBuffer 收集 fetch 结果。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/FetchBuffer.java:39`：Broker 响应与应用线程之间的 CompletedFetch 承接平面。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/FetchCollector.java:92`：收集 fetch 并推进消费位置。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/SubscriptionState.java:627`：position 状态访问入口。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 路线为基线。
- 本篇聚焦 `ClassicKafkaConsumer` 的拉取/位移主链；`AsyncKafkaConsumer` 只作为后续实现边界提示。
- 本文不展开 ConsumerGroup 的 JoinGroup/SyncGroup/ModernGroup 细节，也不展开 FetchSession。
- 本文把 position、FetchBuffer、committed offset 分成不同状态层，不把它们压成一个“消费 offset”。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-1` 的总主链、`Kafka-3` 的分区日志与 offset 存储。
- 后续桥接：下一篇可进入 `Kafka-5`，补 FetchSession 的 Full Fetch / Incremental Fetch 与 CachedPartition。