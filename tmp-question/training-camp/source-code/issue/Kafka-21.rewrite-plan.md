# Kafka-21 重写规划

> 题目：Consumer 怎么记住上次读到哪——offset 提交、位置推进与 auto-commit 主链
> 状态：K-4 Consumer 域第 3 篇，按"offset 提交与位置推进"展开
> 目标：解释 Kafka Consumer 的 offset 管理：`position` 是当前读取位置，`committed` 是已提交到 `__consumer_offsets` 的稳定位置；`commitAsync` 与 `commitSync` 的差异；auto-commit 在 `poll()` 循环中的触发时机；`CommitRequestManager` 在 Async 模型下的实现；以及 `position` 初始化（`updateFetchPosition`）如何从 committed offset 或 auto.offset.reset 决定从哪里开始消费。

## 1. 读者困惑

- `position()` 和 `committed()` 有什么区别？
- `commitAsync` 和 `commitSync` 到底怎么选，为什么 Async 不会阻塞？
- auto-commit 到底什么时候提交？是每次 poll 都提交吗？
- 初次消费时，offset 是怎么确定的——从 committed 读，还是从 earliest/latest 开始？
- `__consumer_offsets` 是怎样的 topic？谁写谁读？
- `CommitRequestManager` 在 Async 模型下怎么处理 retry 和 backoff？

## 2. 一句话顿悟

**Consumer 的 position 是本地内存中的"当前读取指针"，每次 `poll()` 推进；committed 是已持久化到 `__consumer_offsets` 的稳定位置，用于崩溃恢复。auto-commit 在 `poll()` 循环中定时触发，`commitAsync` 不阻塞应用线程，`commitSync` 在 Classic 下阻塞直到 broker 确认。**

## 3. 五要素卡片

### 读者问题

Consumer 崩溃重启后，怎么知道上次读到哪？如果读了十条消息就崩溃，已经处理的消息在下次启动时会不会被重复消费？

### 入口

- `SubscriptionState`：position / committed / assignment 等消费者状态
- `commitAsync` / `commitSync`：提交 offset 的两种方式
- `CommitRequestManager`：Async 模型下的 offset 提交管理器
- `ConsumerCoordinator`：Classic 模型下的 offset 管理
- `OffsetFetcher`：拉取 committed offset
- `OffsetsRequestManager`：请求指定位置的 offset 值
- `__consumer_offsets`：内部 compact topic，用于存储 offset

### 状态核心

- `position`：本地内存中已读取的下一个 offset
- `committed`：已提交到 `__consumer_offsets` 的 offset
- `autoCommitEnabled`：是否自动提交
- `autoCommitInterval`：自动提交间隔（默认 5s）
- `nextAutoCommitTimer`：Classic 下的自动提交计时器
- `AutoCommitState`：Async 下的自动提交状态

### 失败路径

- 不提交 → 崩溃后从上次提交处重新消费，重复处理
- 提交太频繁 → 给 `__consumer_offsets` 造成写入压力
- 提交但未处理完就崩溃 → 写入 offset 后消息未处理完，丢失
- commitAsync 回调失败不重试 → 用户没检查异常，offset 丢失
- 无 committed offset → 需要 `auto.offset.reset` 决定从哪里开始

### 连接点

- 前文 `Kafka-4`：Consumer poll 主链中 position 推进。
- 前文 `Kafka-6`：ConsumerGroup 协调与 offset 提交的关系。
- 前文 `Kafka-20`：Async 模型下的 CommitRequestManager 在后台线程执行。

## 4. 总图

```text
poll()
  → 获取数据 → 推进 position
    → 定时器触发 auto-commit
      → commitAsync（不阻塞）/ commitSync（阻塞）
        → 发送 OffsetCommitRequest 到 coordinator
          → 写入 __consumer_offsets
            → 下次启动时从 committed 恢复

初次消费
  → 没有 committed offset？
    → auto.offset.reset（earliest/latest/none）
      → 确定起始 position
```

## 5. 关键边界

- 本篇不重复 `__consumer_offsets` 的存储细节（Kafka-6 已讲），只讲 offset 提交与 position 推进。
- 不把 `position` 与 `committed` 混成一个概念：前者是本地内存指针，后者是持久化恢复点。
- 不把 `commitAsync` 写成"一定成功"：它可能失败，回调需要处理。

## 6. 失败方案推演

1. **每次读取都提交**：`__consumer_offsets` 写入压力大，不必要的开销。
2. **从不提交**：崩溃后全部重读，可能重复处理大量消息。
3. **只看 committed 不看 position**：读取位置和提交位置解耦，不能混。
4. **commitAsync 失败不重试**：offset 丢失，崩溃后重复消费。

## 7. 误解清单

- “position 就是 committed”：position 是本地内存指针，committed 是持久化恢复点。
- “commitAsync 保证不丢 offset”：回调可能失败，需要用户处理。
- “auto-commit 每次 poll 都提交”：每隔 `auto.commit.interval.ms` 才提交一次。
- “commitSync 在 Async 下也阻塞”：Async 模型下 commitSync 也是异步的，但会等待 Future 完成。
- “初次消费一定从 latest 开始”：由 `auto.offset.reset` 决定。

## 8. 证据清单

- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/CommitRequestManager.java:75`：CommitRequestManager 类。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/CommitRequestManager.java:392`：commitAsync。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/CommitRequestManager.java:420`：commitSync。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/CommitRequestManager.java:268`：maybeAutoCommitAsync。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java:113`：autoCommitEnabled。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java:762`：Classic auto-commit 触发。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/SubscriptionState.java`：position / committed。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦 offset 提交与 position 推进，不展开 `__consumer_offsets` 存储结构。
- 目标正文：6000~10000 字。

## 10. 本轮重写主线

1. 从"崩溃重启后怎么知道上次读到哪"开场。
2. 否定"每次都提交"和"从不提交"两种方案。
3. 解释 position 与 committed 的区别。
4. 解释 commitAsync 与 commitSync 的差异。
5. 解释 auto-commit 的触发时机。
6. 解释初次消费时的 offset 确定。
7. 收网：position 是本地指针，committed 是持久化恢复点，两者配合使用。