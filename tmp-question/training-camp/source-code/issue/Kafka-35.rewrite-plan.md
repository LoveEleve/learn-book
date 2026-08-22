# Kafka-35 重写规划

> 题目：一条消息到底什么时候才算“真的提交”——marker、control batch 与 read_committed 可见性
> 状态：K-12 事务幂等域第 4 篇，按"marker / control batch / read_committed"展开
> 目标：解释事务的最后一步——协调器如何通过 `TransactionMarkerChannelManager` 把 COMMIT / ABORT marker 扇出到所有业务分区，业务分区如何写入 `ControlBatch`，以及 `read_committed` 消费者如何通过事务索引与 LSO 过滤未提交/已 abort 数据。

## 1. 读者困惑

- 协调器决定“提交”之后，为什么还需要向业务分区发送 marker？
- control batch 在业务分区日志里长什么样？怎么区分 COMMIT 和 ABORT？
- `read_committed` 消费者到底是怎么知道哪些消息该跳过？
- 为什么 `read_committed` 不能只看有没有 COMMIT marker？
- LSO（last stable offset）和这些 marker 是什么关系？

## 2. 一句话顿悟

**协调器决定事务结果后，通过 `TransactionMarkerChannelManager` 异步向每个涉及的业务分区发送 `WriteTxnMarkersRequest`，包含 `TxnMarkerEntry`（producerId + epoch + TransactionResult.COMMIT / ABORT）。业务分区 leader 把 marker 写入 `ControlBatch`，同时更新 `ProducerStateManager.completeTxn`，把事务从 `ongoingTxns` 移到 `unreplicatedTxns`。`read_committed` 消费者在读取时先按 LSO（min(HW, firstUnstableOffset)）截断可见边界，再通过事务索引过滤已 abort 的事务。**

## 3. 五要素卡片

### 读者问题

`read_committed` 消费者凭什么知道某段事务数据是提交了还是应该跳过？

### 入口

- `TransactionMarkerChannelManager`：marker 扇出线程
- `WriteTxnMarkersRequest` / `TxnMarkerEntry`
- `ControlBatch` / `EndTransactionMarker`
- `UnifiedLog` / `LocalLog` 对 control batch 的处理
- `ProducerStateManager.completeTxn` / `ongoingTxns` / `unreplicatedTxns`
- `firstUnstableOffset` / `lastStableOffset`
- `AbortedTransaction` / `TransactionIndex`

### 状态核心

- `ongoingTxns`：进行中事务
- `unreplicatedTxns`：已完成但 marker 在 HW 之上
- `firstUnstableOffset`：第一个未完成或未复制稳定的事务边界
- `LSO` = `min(HW, firstUnstableOffset)`
- `TransactionIndex`：aborted 事务索引

### 失败路径

- marker 不扇出 → read_committed 无法判断事务边界
- marker 已写出但未复制到 HW 就发成功 → 消费者看不到正确结果
- 事务索引不完整 → 消费者过滤 aborted 事务出错
- ongoingTxns 和 unreplicatedTxns 在崩溃恢复时未重建 → LSO 计算错误

### 连接点

- 前文 `Kafka-33`：幂等 producer 校验。
- 前文 `Kafka-34`：coordinator 状态机与事务日志。
- 前文 `Kafka-11`：事务总览中的 marker 部分。
- 前文 `Kafka-19`：ProducerStateManager 的 snapshot 与 ongoingTxns/unreplicatedTxns。

## 4. 总图

```text
coordinator 决定 COMMIT / ABORT
  → TransactionMarkerChannelManager
    → WriteTxnMarkersRequest
      → 各业务分区 leader
        → 写入 ControlBatch
          → ProducerStateManager.completeTxn
            → ongoingTxns → unreplicatedTxns
              → firstUnstableOffset / LSO

read_committed 消费者
  → 读取时 LSO 截断
    → 通过事务索引过滤 aborted
      → 只返回 committed 数据
```

## 5. 关键边界

- 本篇不重复 coordinator 状态机（K-12 第 3 篇）。
- 不把 LSO 当作“HW 下所有已提交数据”：它代表尚不稳定的事务边界，不是直接可见门槛。
- 不把 marker 写入与业务数据写入混成同一阶段的 append。
- 不展开 `TransactionIndex` 全部内部结构，只讲它如何参与过滤。

## 6. 失败方案推演

1. **coordinator 决定提交后不写 marker**：消费者永远无法知道事务结果。
2. **只写 marker 不更新 firstUnstableOffset**：LSO 无法推进，read_committed 卡住。
3. **read_committed 只看 HW 不看 LSO**：未完成的事务数据也被可见。
4. **不建事务索引**：aborted 事务的数据无法被过滤。

## 7. 误解清单

- “marker 是发给 coordinator 的确认。”：marker 是 coordinator 发给业务分区 leader 的。
- “read_committed 就是只看 COMMIT marker。”：还要看 abort 事务索引过滤。
- “LSO 就是 HW。”：LSO ≤ HW。
- “ongoingTxns 和 unreplicatedTxns 不重要。”：它们决定 LSO 的边界。
- “control batch 和普通 batch 一样处理。”：它有专门的 `isControlBatch` 标记。

## 8. 证据清单

- `core/src/main/scala/kafka/coordinator/transaction/TransactionMarkerChannelManager.scala:161`：marker 扇出线程。
- `clients/src/main/java/org/apache/kafka/common/requests/WriteTxnMarkersRequest.java`：marker 请求。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:545`：completeTxn。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:246`：firstUnstableOffset。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java:676`：lastStableOffset。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/LocalLog.java:540`：aborted transactions 收集。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦 marker 扇出与 read_committed 可见性，不重复 coordinator 状态机。
- 目标正文：6000~10000 字。

## 10. 本轮重写主线

1. 从"coordinator 说提交了，consumer 怎么知道"开场。
2. 否定“不写 marker”“只看 HW”“不建索引”三种方案。
3. 解释 marker 扇出流程。
4. 解释 control batch 写入。
5. 解释 completeTxn：ongoing → unreplicated。
6. 解释 firstUnstableOffset / LSO。
7. 解释 read_committed 读取与事务索引过滤。
8. 收网：marker / LSO / txnindex 共同决定可见性。