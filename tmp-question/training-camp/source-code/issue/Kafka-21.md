# Kafka-21. Consumer 怎么记住上次读到哪——offset 提交、位置推进与 auto-commit 主链

> 场景：Kafka-4 讲 Consumer 的 poll() 主链时，我们默认消费者已经知道从哪里开始读。但"从哪里开始读"本身就是一个需要解决的问题：**初次消费、崩溃重启、手动 seek，分别对应不同的初始位置确定逻辑。** 本篇补上 Consumer 的 offset 管理：`position` 与 `committed` 的区别、`commitAsync`/`commitSync` 的差异、auto-commit 的触发时机，以及初次消费时 offset 如何确定。这是 K-4 Consumer 域第 3 篇。

## 先把真正的困惑摆出来：崩溃重启后，怎么知道上次读到哪

假设 Consumer 已经消费了 offset 0~999 的消息，处理完并提交了 offset 1000。然后崩了。重启后，Kafka 怎么知道它上次读到哪？

答案不是"从本地文件读"，也不是"问 broker 上次返回的最大 offset"。答案是：**Consumer 把已处理的 offset 提交到 `__consumer_offsets`，重启后从那里恢复。**

但这里立刻引出几个问题：

- 提交的 offset 和当前正在读的 offset 是同一个吗？——不是。`position` 是本地内存里"下一条要读的 offset"，`committed` 是持久化到 `__consumer_offsets` 的"已确认安全的位置"。
- 什么时候提交？每次 poll 都提交？还是定时提交？
- 提交失败了怎么办？重试还是放弃？
- 如果不提交，下次启动时从哪开始？

```text
position  = 当前本地读取指针（每次 poll 推进）
committed = 已持久化到 __consumer_offsets 的安全位置（供崩溃恢复）
```

*关键设计（斜体）：* *Consumer 在内存中维护 `position`（当前读取指针），在 `__consumer_offsets` 中持久化 `committed`（已确认的安全位置）。auto-commit 在 `poll()` 循环中定时触发（`auto.commit.interval.ms`，默认 5s），`commitAsync` 不阻塞应用线程，`commitSync` 等待 broker 确认。初次消费时没有 committed offset，由 `auto.offset.reset` 决定起始位置。*[模式: 内存指针 + 持久化恢复点 + 定时提交]

## 第一层：position 是本地指针，committed 是持久化恢复点

`SubscriptionState` 维护了 Consumer 的每分区状态，包括 `position` 和 `committed`（`SubscriptionState.java`）。

- **position**：本地内存中"下一条要 poll 的消息的 offset"。每次 `poll()` 返回一批消息后，position 自动推进到这批消息的最后一个 offset 之后。
- **committed**：已通过 `OffsetCommitRequest` 提交到 `__consumer_offsets` 的 offset，代表"我已经处理到这个位置了"。

两者差在哪里？## 应用语义

- position 是"你当前正在读的位置"；
- committed 是"你已经确认安全的位置"。

如果 Consumer 在读到 offset 500 时崩溃，但 committed 还是 300，那么重启后从 300 开始读，300~500 的消息会被重复处理。这就是 Kafka 的"至少一次"语义：可能重复，但不会丢。

```text
poll() 返回 0~99 → position=100 → 未提交
再 poll() 返回 100~199 → position=200 → 提交 committed=200
崩溃 → 重启 → 从 committed=200 开始读
```

## 第二层：commitAsync 与 commitSync——提交的两种方式

Consumer 提交 offset 有两种方式：

### commitAsync

`commitAsync` 发送 `OffsetCommitRequest` 后立即返回，不等待 broker 确认。回调用于处理失败，但默认回调只日志不重试。不阻塞应用线程，适合在 poll 循环中定期调用。

### commitSync

`commitSync` 在 Classic 模型下阻塞应用线程直到 broker 确认提交成功。在 Async 模型下，`CommitRequestManager` 仍然会重试，但调用者可以通过 `Future` 等待完成。

`CommitRequestManager.commitSync()` 会一直重试直到成功或遇到不可重试错误（`CommitRequestManager.java:454`）。`commitAsync` 则发一次请求，回调告知结果，不自动重试（`CommitRequestManager.java:392`）。

所以在 Classic 下，commitSync 会阻塞应用线程，适合在关闭前确保 offset 已提交；commitAsync 适合在 poll 循环中定期提交，不阻塞正常消费。

## 第三层：auto-commit 的触发时机

auto-commit 不是"每次 poll 都提交"，而是**每隔 `auto.commit.interval.ms`（默认 5s）提交一次**。

在 Classic 模型下，`ConsumerCoordinator` 有一个 `nextAutoCommitTimer`。每次 `poll()` 都检查这个定时器是否到期，如果到期就触发一次 `maybeAutoCommitOffsetsAsync()`（`ConsumerCoordinator.java:762`）。

在 Async 模型下，`CommitRequestManager` 的 `AutoCommitState` 维护一个 `timer`，`maybeAutoCommitAsync()` 检查是否到时间（`CommitRequestManager.java:268`）。如果到时间，就把当前所有已消费的 offset 提交。

```text
poll()
  → 处理返回的数据
    → 是否到 auto-commit 时间？
      → 是：提交当前所有已读 offset
        → 重置定时器
          → 继续 poll
```

所以 auto-commit 不是"消费完一批就提交"，而是"每隔一段时间把当前已读的位置提交一次"。这意味着两次提交之间处理的 offset 在崩溃时会丢失，但最多丢失一个 `auto.commit.interval.ms` 窗口内的数据。

## 第四层：初次消费时 offset 怎么确定

如果 Consumer 是新 group，或者 `committed` 不存在（第一次启动），那 `position` 从哪开始？

`updateFetchPosition` 在 `committed` 不存在时，会检查 `auto.offset.reset` 配置。它有三个值：

- `earliest`：从分区的最早 offset 开始；
- `latest`：从分区的最新 offset 开始（只消费之后的新消息）；
- `none`：如果没 committed offset，抛 `NoOffsetForPartitionException`。

`OffsetFetcher` 和 `OffsetsRequestManager` 负责向 broker 拉取最早的 offset 或最新的 offset，然后设置 `position`。

这也是为什么新 group 启动时，如果你不设置 `auto.offset.reset=earliest`，可能一条消息都看不到——因为默认是 `latest`。

## 第五层：提交到哪里——`__consumer_offsets`

所有 offset 提交最终都写入 `__consumer_offsets` 这个内部 compact topic（Kafka-6 已讲过它的结构）。它的 key 是 `<groupId, topic, partition>`，value 是 `OffsetAndMetadata`（包含 offset 和元数据）。

因为它是 compacted topic，所以每个 key 只保留最新的 value。这意味着：

- 提交多次只会保留最后一次提交的 offset；
- 压缩后存储空间不大；
- 所有 broker 上的 `__consumer_offsets` 分区 leader 负责处理该分区的 offset 读写。

## 收网：position 是本地指针，committed 是持久化恢复点

把整篇压成一句话：Consumer 在内存中维护 `position`（当前读取指针），在 `__consumer_offsets` 中持久化 `committed`（已确认的安全位置）。auto-commit 在 `poll()` 循环中每隔 `auto.commit.interval.ms` 触发一次，`commitAsync` 不阻塞，`commitSync` 等待 broker 确认。初次消费时没有 committed offset，由 `auto.offset.reset` 决定起始位置。

```text
poll()
  → 拿数据 → 推进 position
    → auto-commit 定时器到？
      → 提交当前所有 position → committed
        → 下次重启从 committed 开始

初次消费
  → 无 committed offset
    → auto.offset.reset（earliest/latest/none）
      → 确定起始 position
```

到这里，主线只发生了五件事。

第一，position 是本地内存指针，committed 是持久化恢复点。

第二，commitAsync 不阻塞，commitSync 等待 broker 确认。

第三，auto-commit 每隔 `auto.commit.interval.ms` 触发一次，不是每次 poll。

第四，初次消费无 committed offset 时由 `auto.offset.reset` 决定。

第五，所有提交写入 `__consumer_offsets` compacted topic。

**本篇的一句话困惑**：Consumer 崩溃重启后，怎么知道上次读到哪？

**本篇的一句话顿悟**：Consumer 把已处理的 offset 提交到 `__consumer_offsets`，重启后从 committed 恢复；position 是本地内存指针，committed 是持久化恢复点，两者差一个窗口，这就是"至少一次"语义的来源。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“position 就是 committed。”** position 是本地内存指针，committed 是持久化恢复点。
2. **“commitAsync 保证不丢 offset。”** 回调可能失败，需要用户处理或重试。
3. **“auto-commit 每次 poll 都提交。”** 每隔 `auto.commit.interval.ms` 才提交一次。
4. **“commitSync 在 Async 下也阻塞应用线程。”** Async 模型下 commitSync 通过 Future 等待，但网络 IO 在后台线程。
5. **“初次消费一定从 latest 开始。”** 由 `auto.offset.reset` 决定。

### 关键证据清单

- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/CommitRequestManager.java:75`：CommitRequestManager 类。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/CommitRequestManager.java:392`：commitAsync。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/CommitRequestManager.java:420`：commitSync。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/CommitRequestManager.java:268`：maybeAutoCommitAsync。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java:762`：Classic auto-commit 触发。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/SubscriptionState.java`：position / committed。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/OffsetFetcher.java`：拉取 committed offset。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦 offset 提交与 position 推进，不展开 `__consumer_offsets` 存储结构。
- 不把 `position` 与 `committed` 混成一个概念。
- 不把 commitAsync 写成"一定成功"。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-4`（Consumer poll 主链）、`Kafka-6`（ConsumerGroup 协调与 `__consumer_offsets`）。
- 后续桥接：K-4 Consumer 域 3 篇收官，下一篇可进入 K-6 ConsumerGroup 域第 2 篇（ClassicGroup 协议），或切到其他域。