# Kafka-16. batch 真的发出去之后——acks、Sender 回执处理与重试主链

> 场景：Kafka-15 讲了 batch 被 `Sender` drain 出去前的所有准备，但止步于"交给 `NetworkClient` 发送"。本篇接上后半段：**batch 真的发出去之后，`acks` 决定 broker 要等多少确认，`Sender` 收到 `ProduceResponse` 后如何决定这个 batch 是成功、重试还是失败。** 这也是 K-2 Producer 域第 3 篇，把 Producer 发送主链完整闭环。

## 先把真正的困惑摆出来：batch 发出去了，怎么知道成功

在 Kafka-15 里，batch 被 drain 出来、通过 `NetworkClient` 发到了 broker，`send()` 返回的 future 还悬在那里。现在的问题是：谁、根据什么，把这个 future 置成成功的？

- 是 broker 收到就成功？
- 还是 broker 把数据写入日志才成功？
- 还是所有 ISR 副本都追上了才成功？
- 失败的话，是立刻告诉用户，还是偷偷再试一次？

这就是本篇的核心：`ProducerBatch` 从"被 Sender 发送出去"到"future 完成"，中间经历了 `acks` 语义、`ProduceResponse` 回执、`completeBatch` 分支、`canRetry` 判断这一条完整链路。

```text
batch 被 Sender 发送
  → broker 根据 acks 决定何时回成功
  → Sender 收到 ProduceResponse
    → completeBatch()
      → 成功 / 分裂重发 / 重试 / 失败
        → future 完成
```

*关键设计（斜体）：* *`acks` 决定 broker 要等多少确认才回成功；`Sender` 根据 `ProduceResponse` 的错误码调用 `completeBatch()`：成功直接完成，`MESSAGE_TOO_LARGE` 分裂重发，可重试错误经 `canRetry` 判断后重入队，不可重试则 `failBatch`。重试同时受 `retries` 次数与 `deliveryTimeoutMs` 总时长约束。*[模式: 确认门槛 + 回执分支 + 有限重试]

## 第一层：acks 是 broker 再回成功前要等的确认门槛

`ProducerBatch` 被组装成 `ProduceRequest` 时，会带上 `acks` 字段（`Sender.java:931`）。它告诉 broker：这个 produce 请求要等多少副本确认，才允许给 Producer 回"成功"。

- `acks=0`：不等任何确认。Producer 发出去就当成功，fire-and-forget。
- `acks=1`：leader 写入本地日志就算成功，不等 follower。
- `acks=all`（即 -1）：等 ISR 所有副本都确认写入才算成功。

注意这三者在 client 侧造成的行为差异：

`acks=0` 时，网络层根本等不到响应（`newClientRequest(..., acks != 0, ...)`，`Sender.java:944`）。所以收到"响应"时直接按 `Errors.NONE` 完成所有 batch（`Sender.java:653-656`）。这不代表消息真的写进 broker 日志，只是"我发出去且没等确认"。

`acks=1` 和 `acks=all` 则会真正等到 `ProduceResponse`，由响应里的错误码决定成功还是失败。

所以 `acks` 控制的是 **broker 在回成功之前需要达到的确认程度**，它最终会体现在 Sender 收到响应时的 `errorCode` 上。

## 第二层：Sender 收到 ProduceResponse 后，进入 completeBatch 分支

当 `NetworkClient` 收到 `ProduceResponse` 后，`Sender` 会调用 `handleProduceResponse()`。它先处理三条**前置分支**：请求超时（`wasTimedOut`）、连接断开（`wasDisconnected`）、协议版本不匹配（`versionMismatch`），这些都直接用对应错误码走 `completeBatch`。只有请求真正携带了响应体，`Sender` 才会解析出每个 partition 的错误码，找到对应的 `ProducerBatch`，再调用 `completeBatch(batch, response, ...)`（`Sender.java:634`）。

`completeBatch` 是 batch 结局的唯一裁决点，它有五条主要出路：

```text
completeBatch(batch, response)
  → error == NONE → 成功，完成 future
  → MESSAGE_TOO_LARGE 且可分裂 → splitAndReenqueue（分裂）
  → canRetry() 通过 → reenqueueBatch（重入队）
  → DUPLICATE_SEQUENCE_NUMBER → 当作成功完成
  → else → failBatch（失败）
```

### 成功路径

`error == NONE`：batch 被置为 SUCCEEDED，`produceFuture` 完成，`thunks` 逐一回调，ByteBuffer 归还 `BufferPool`。

### MESSAGE_TOO_LARGE

这是最特殊的一条：如果 broker 说这个 batch 超过了 `message.max.bytes`，Producer 会尝试用 `splitAndReenqueue(batch)` 把这个 batch **分裂成几个小 batch** 再分别重入队（`Sender.java:687`）。但要注意分裂的条件不只是 "batch 太大"：源码要求 `recordCount > 1 && !batch.isDone() && (batch.magic() >= V2 || batch.isCompressed())`。也就是说，只有"记录不止一条、且消息格式 >= V2 或已压缩"的 batch 才值得分裂；单条记录且旧格式未压缩的 batch 无法再拆，会直接走失败路径。分裂这一步不消耗 retry 次数，因为重发同一个超大 batch 永远会失败。

### 可重试错误

如果错误属于可重试类（`RetriableException`，例如 leader 切换、网络异常、NOT_LEADER_OR_FOLLOWER 等），`canRetry()` 通过后走 `reenqueueBatch()`，把 batch 重新放回 accumulator 等待再次发送（`Sender.java:698`）。

### DUPLICATE_SEQUENCE_NUMBER

幂等 producer 下，如果 broker 认为这个 batch 是重复序号（之前的重试其实已经在 broker 成功过），那么 Producer 直接把这个 batch 标记为成功（`Sender.java:699-705`），即使拿不到准确的 offset/timestamp。这是幂等语义下的特殊成功路径。

### failBatch

如果错误不可重试，或重试次数已耗尽，`failBatch()` 把 future 完成到失败状态，并把异常抛给用户回调（`Sender.java:710`）。

所以 `completeBatch` 不是"二选一成功失败"，而是**依据错误码类型精确路由到成功、分裂、重试、幂等成功、失败五条路径**。

## 第三层：canRetry 是重试的总闸门

决定"能不能重试"的是 `canRetry(batch, response, now)`，它有四个条件：

```text
canRetry()
  = !batch.hasReachedDeliveryTimeout(deliveryTimeoutMs, now)     // 1. 未超总时长
    && batch.attempts() < this.retries                            // 2. 尝试次数未到上限
    && !batch.isDone()                                            // 3. 还没完成
    && (transactionManager == null
        ? response.error.exception() instanceof RetriableException
        : transactionManager.canRetry(response, batch))           // 4. 错误可重试
```

四个条件缺一不可：

1. **总时长没超**：`deliveryTimeoutMs` 是这条消息从 batch 创建（`createdMs`）到成功/失败的绝对上界。超了就不重试，直接失败。
2. **次数没超**：`retries` 限制最多尝试几次。`batch.attempts()` 每次发送 +1。
3. **还没完成**：如果 batch 已经被完成（可能被并发路径处理过），不再重试。
4. **错误可重试**：普通 producer 要求错误是 `RetriableException`；幂等/事务 producer 走 `transactionManager.canRetry()`，约束更多。

如果四个条件都满足，batch 会被 `reenqueueBatch()` 放回 accumulator 队列头部，等待下一次 drain。注意它是回到头部（`deque.addFirst`），保证同一分区的重试 batch 不会被新的 batch 插队打断顺序。

## 第四层：重试的时间维度——retries、deliveryTimeoutMs、backoff

很多人把重试想成"retries 次就完了"，但 Kafka 重试其实受两个独立的上界共同约束：

- **次数上界 `retries`**：最多尝试几次。
- **总时长上界 `deliveryTimeoutMs`**：从 `ProducerBatch` 创建（`createdMs`）那一刻起，无论重试多少次，最多允许活多久。

两个上界取更苛刻者生效。也就是说，即使 `retries` 还有剩余，只要 `deliveryTimeoutMs` 到了，batch 也会被判为超时失败。

此外还有 `retry.backoff.ms`：两次重试之间暂停的间隔，避免失败的风暴立即重打。`RecordAccumulator` 用 `ExponentialBackoff` 实现，并带抖动（jitter），防止所有 producer 同时重试造成同步风暴（`RecordAccumulator.java:135-138`）。

```text
retries = 5
deliveryTimeoutMs = 120000

某次失败
  → canRetry?（次数<5 且 超时未到 且 错误可重试）
    → 等待 retryBackoff
      → 重入队 → 再次 drain → 再发送
        → 直到成功 / 次数耗尽 / 总时长超时
```

这也是 Kafka 为什么强调 deliveryTimeoutMs 的默认值：它不是"每次尝试的 timeout"，而是**整个 delivery 过程的绝对期限**。

## 第五层：幂等/事务 producer 的重试为什么更"克制"

普通 producer 重试失败后，把 batch 重入队就行。但幂等/事务 producer 还背着 sequence 连续的约束：

- 每个 partition 的 batch 有严格递增的 sequence；
- broker 靠 `<producerId, epoch, sequence>` 去重校验；
- 如果重试打乱了发送顺序，broker 会拒绝 out-of-order 的 batch。

所以在幂等/事务 producer 下：

- `canRetry` 走 `transactionManager.canRetry()`，会额外检查 sequence 状态是否允许重试；
- 重入队必须用 `insertInSequenceOrder` 保持顺序，而不是普通 producer 的无序 `addFirst`；
- 收到 `DUPLICATE_SEQUENCE_NUMBER` 说明"这个 seq 已经在 broker 成功过"，Producer 视作成功收尾。

这就是 Kafka-11 讲的：幂等层用 producerId/epoch/sequence 防重，但它同时**约束了 producer 的重试行为**——不能破坏序号，否则重试本身会造成更严重的错误。

## 收网：acks 定门槛，completeBatch 定结局，canRetry 定要不要再来

把整篇压成一句话：`acks` 决定 broker 回成功前要等多少副本确认；`Sender` 收到 `ProduceResponse` 后调用 `completeBatch()` 依据错误码裁决 batch 结局——成功、MESSAGE_TOO_LARGE 分裂重发、可重试错误经 `canRetry` 后重入队、幂等重复序号视作成功、否则失败；`canRetry` 同时受 `retries` 次数、`deliveryTimeoutMs` 总时长与错误类型约束；幂等/事务 producer 的重试还受 sequence 连续限制。

```text
ProduceRequest（带 acks）
  → broker 按 acks 确认
    → ProduceResponse / 超时 / 断连
      → completeBatch()
        → NONE → 成功
        → MESSAGE_TOO_LARGE → 分裂重发（有条件）
        → canRetry → 等待 backoff → 重入队
        → DUPLICATE_SEQ → 幂等成功
        → else → 失败
```

到这里，主线只发生了五件事。

第一，`acks` 是 broker 回成功前的确认门槛，client 侧以 errorCode 体现。

第二，`completeBatch` 是 batch 结局的裁决点，有成功/分裂/重试/幂等成功/失败五路。

第三，`canRetry` 四条件同时受次数、总时长、完成状态、错误可重试性约束。

第四，`deliveryTimeoutMs` 是总时长上界，与 `retries` 次数上界共同作用。

第五，幂等/事务 producer 重试受 sequence 连续约束，不能破坏顺序。

**本篇的一句话困惑**：batch 发出去之后，Producer 怎么知道成功，失败又凭什么决定要不要再试？

**本篇的一句话顿悟**：acks 定了确认门槛，completeBatch 按错误码裁决结局，canRetry 用次数+总时长+错误类型共同决定要不要再来；幂等 producer 还必须在 sequence 连续的前提下重试。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“acks=0 表示消息真的写入成功。”** 它只是 fire-and-forget，不表示 broker 已写入。
2. **“MESSAGE_TOO_LARGE 是重发同一 batch。”** 是分裂后分别重发，且分裂有条件（recordCount>1 且 magic>=V2||isCompressed），否则直接失败。
3. **“retries 是重试的总时长。”** 次数与 `deliveryTimeoutMs` 总时长是两个独立上界。
4. **“所有错误都会重试。”** 只有 `RetriableException` 才会走 canRetry。
5. **“幂等 producer 重试和普通一样。”** 受 sequence 连续约束，重入队必须保持顺序。

### 关键证据清单

- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:99`：acks 字段。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:310`：runOnce() 主循环。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:579`：handleProduceResponse 前置超时/断连分支。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:675`：MESSAGE_TOO_LARGE 分裂条件（recordCount>1 && magic>=V2 || isCompressed）。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:895`：sendProduceRequest 构造请求。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:600`：收到 ProduceResponse 处理。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:670`：completeBatch 分支。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:691`：canRetry 重入队。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:875`：canRetry 条件。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:687`：splitAndReenqueue。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:750`：reenqueueBatch。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/ProducerBatch.java:441`：hasReachedDeliveryTimeout（计量起点 createdMs）。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦 client 侧 acks/回执/重试，不展开事务完整语义与 `TransactionManager` 内部。
- 本篇不把 `retries`、`deliveryTimeoutMs`、`retry.backoff.ms` 混成同一个时间概念。
- broker 侧 DelayedProduce 的 acks=all 确认细节已在 Kafka-10 讲清，本篇不再重复。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-15`（batch 生命周期前半段）、`Kafka-11`（幂等 sequence）、`Kafka-10`（broker 侧 acks=all 等待）。
- 后续桥接：下一篇可回到 Kafka 各域的聚合 review，或继续补 Producer 剩余子专题/网络层对照。