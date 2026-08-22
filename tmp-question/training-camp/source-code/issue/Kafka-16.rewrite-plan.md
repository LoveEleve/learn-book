# Kafka-16 重写规划

> 题目：batch 真的发出去之后——acks、Sender 回执处理与重试主链
> 状态：K-2 Producer 域第 3 篇，按“acks / 重试 / Sender 刷出”展开
> 目标：解释 `ProducerBatch` 被 drain 出去之后的生命周期到成为 FutureResult 或失败为止：`acks=0/1/all` 三种语义在 client 侧分别意味着什么、Sender 收到 `ProduceResponse` 后如何走 `completeBatch`（成功 / MESSAGE_TOO_LARGE 分裂重发 / canRetry 重入队 / 非重试失败）、`deliveryTimeoutMs` 与 `retries` 如何决定能否重试，以及幂等/事务下 retry 的约束。

## 1. 读者困惑

- `acks=0/1/all` 在 Producer 侧到底控制什么，三者怎么落到 `send()` 返回的 future 上？
- `acks=1` 时，Producer 收到什么才算成功？
- MESSAGE_TOO_LARGE 时，为什么 Producer 不是重发，而是“分裂重发”？
- `retries`、`retry.backoff.ms`、`delivery.timeout.ms` 三者如何共同决定一次重试？
- Sender 收到响应后，`completeBatch` 到底是怎样分支的？
- 幂等/事务 producer 的重试和普通 producer 有什么不同？

## 2. 一句话顿悟

**`acks` 决定 broker 要等多少确认才回成功，最终以 `ProduceResponse` 的错误码落到 `Sender.completeBatch()`：成功直接完成，MESSAGE_TOO_LARGE 分裂重发，可重试错误进入 `canRetry` 判断（受 `retries`、`deliveryTimeoutMs`、错误类型约束）后重入队，不可重试则 `failBatch`。幂等/事务 producer 因为要维护 sequence，重试不能破坏序号连续。**

## 3. 五要素卡片

### 读者问题

一个 batch 被 Sender drain 出去、通过 `NetworkClient` 发到 broker 后，Producer 怎么知道它成功还是失败？如果失败，凭什么决定“再试一次”还是“告诉用户失败”？

### 入口

- `Sender.runOnce()`：每次循环的发送主链
- `Sender.sendProduceRequest()`：构造 ProduceRequest（带 acks）
- `Sender.completeBatch()`：成功/重试/失败的分支
- `Sender.canRetry()`：重试条件
- `ProducerBatch.attempts` / `hasReachedDeliveryTimeout`
- `ReenqueueBatch` → `accumulator.reenqueue`
- `deliveryTimeoutMs` / `retries` / `retry.backoff.ms`

### 状态核心

- `acks`：0 / 1 / -1(all)
- `ProducerBatch.attempts`：尝试次数
- `finalState`：SUCCEEDED / FAILED / ABORTED
- `canRetry`：`!deliveryTimeout && attempts < retries && !done && retriable`
- `MESSAGE_TOO_LARGE`：分裂（`splitAndReenqueue`）而不是重发

### 失败路径

- acks=all 时 ISR 不足 → broker 延迟或拒绝，Producer 重试
- MESSAGE_TOO_LARGE 不分裂 → 永远重发同一超大 batch，死循环
- deliveryTimeout 到了还重试 → 反复重试直到超时才 fail，理论上总是成功
- 不检查 retries 上限 → 无限重试
- 幂等 producer 破坏 sequence → broker 返回 duplicate/out-of-order

### 连接点

- 前文 `Kafka-15`：batch 被 drain 出去后的生命周期，本篇接它的后半段。
- 前文 `Kafka-10`：broker 侧 `acks=all` 用 DelayedProduce 等 ISR。
- 前文 `Kafka-11`：幂等/事务 producer 的 sequence 约束与 retry 的关系。

## 4. 总图

```text
Sender.runOnce()
  → 找 ready 的 batch（drain）
    → sendProduceRequest（携带 acks）

NetworkClient 发送 → 收到 ProduceResponse
  → completeBatch(batch, response)
    → error == NONE → 成功完成 future
    → MESSAGE_TOO_LARGE → splitAndReenqueue（分裂重入队）
    → canRetry() == true → reenqueueBatch（重入队）
    → else → failBatch（失败完成 future）
```

## 5. 关键边界

- 本篇只讲 client 侧发送与回执，不重讲 broker 侧（Kafka-10 已讲 DelayedProduce）。
- `acks` 的语义：0 表示不等响应、1 表示 leader 写入、all 表示 ISR 确认，对应不同 ProduceRequest 行为。
- 不把 `retries` 与 `deliveryTimeoutMs` 混成同一概念：前者限制尝试次数，后者限制总时长。
- 幂等 producer 下 retry 约束更多，本篇点到 sequence 关联即可，详细留给事务专题。

## 6. 失败方案推演

1. **MESSAGE_TOO_LARGE 直接重发**：同一个超大 batch 反复失败，永远发不出去。
2. **只看 retries 不看 deliveryTimeout**：重试次数没到但已超时也继续拖，延迟不可控。
3. **只看 deliveryTimeout 不看 retries**：无限重试在窗口内刷爆。
4. **幂等 producer 重试不维护 sequence**：破坏序号连续，broker 拒绝。

## 7. 误解清单

- “acks=0 表示不等 broker 响应，所以 future 立刻成功”：它不等响应，但 future 由 Sender 本地直接标成功，不代表真被写入。
- “MESSAGE_TOO_LARGE 是重发同一 batch”：是分裂再重发，避免死循环。
- “retries 和 deliveryTimeoutMs 换算简单”：deliveryTimeoutMs 是总时长上界，retries 是次数上界，取更苛刻者。
- “所有错误都可重试”：只有可重试错误（RetriableException）才走 canRetry。
- “幂等 producer 重试和普通一样”：重试受 sequence 连续约束。

## 8. 证据清单

- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:99`：acks 字段。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:310`：runOnce() 主循环。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:895`：sendProduceRequest 构造请求。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:600`：收到 ProduceResponse 处理。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:670`：completeBatch 分支。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:691`：canRetry 走重入队。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:875`：canRetry 条件。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:750`：reenqueueBatch。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java:687`：splitAndReenqueue。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦 client 侧 acks/回执/重试，不展开事务完整语义。
- 目标正文：6000~10000 字；核心拆解层覆盖 acks 语义、completeBatch 分支、canRetry、MESSAGE_TOO_LARGE 分裂。

## 10. 本轮重写主线

1. 从“batch 发出去后怎么知道成功”开场。
2. 否定“MESSAGE_TOO_LARGE 重发同一 batch”和“只重试不看超时”两种方案。
3. 解释 acks=0/1/all 在 client 侧的差异。
4. 解释 Sender 收到 ProduceResponse 后的 completeBatch 四分支。
5. 解释 canRetry 的四个条件。
6. 解释 deliveryTimeoutMs/retries/backoff 的时间维度。
7. 收网：acks 决定 broker 确认门槛，completeBatch 决定 batch 结局，canRetry 决定要不要再来一次。