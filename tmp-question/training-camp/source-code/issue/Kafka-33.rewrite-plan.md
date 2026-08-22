# Kafka-33 重写规划

> 题目：幂等 producer 到底靠什么防重——producerId / producerEpoch / sequence 与 append 校验主链
> 状态：K-12 事务幂等域第 2 篇，按"幂等 producer 与 seq/epoch 深讲"展开
> 目标：正面讲透幂等 producer 的单分区防重机制：`ProducerAppendInfo` 如何校验 epoch/sequence、`ProducerStateEntry` 如何维护 lastSeq / batchMetadata、`OutOfOrderSequenceException` / `InvalidProducerEpochException` 何时抛出、以及重复序列号为什么能被 `findDuplicateBatch` 直接容忍。这是 K-12 事务幂等域第 2 篇。

## 1. 读者困惑

- `producerId + producerEpoch + sequence` 三角色分别解决什么问题？
- broker 追加时如果 `sequence` 不连续，抛什么异常？
- `producerEpoch` 变小时代表什么，为什么是 `InvalidProducerEpochException`？
- 同一批重试后 sequence 相同，broker 怎么判断是重复而不是乱序？
- 为什么 offset commit 这类 append 只校验 epoch，不校验 sequence？
- 崩溃后 producer 状态怎么恢复，才能继续正确校验？

## 2. 一句话顿悟

**幂等 producer 的防重发生在 broker 的 `ProducerAppendInfo` 追加校验阶段：`producerId` 标识 writer、`producerEpoch` 阻止过期 writer、`sequence` 保证分区内严格连续。如果 sequence 不是 `lastSeq+1`（或环绕到 0），就抛 `OutOfOrderSequenceException`；如果 epoch 小于已知值，就抛 `InvalidProducerEpochException`。重复的序列号则通过 `ProducerStateEntry.findDuplicateBatch` 识别并容忍，直接视为已成功的重试。**

## 3. 五要素卡片

### 读者问题

同样是发一条 seq=5，为什么乱序重试会被拒，而重试同一批会被容忍？

### 入口

- `ProducerAppendInfo`：追加前校验与事务元数据累积
- `ProducerAppendInfo.checkSequence` / `checkProducerEpoch`
- `ProducerStateEntry`：lastSeq / batchMetadata
- `BatchMetadata`：单批的 seq 范围
- `findDuplicateBatch`：识别重复序列号
- `OutOfOrderSequenceException` / `InvalidProducerEpochException`

### 状态核心

- `producerId`：writer 身份
- `producerEpoch`：writer 代次（fencing）
- `sequence`：分区内连续序号
- `inSequence(lastSeq, nextSeq)`：判断连续性
- `batchMetadata`：最近几批的 seq 范围

### 失败路径

- sequence 乱序 → `OutOfOrderSequenceException`
- epoch 过期 → `InvalidProducerEpochException`
- txn v2 idempotent 无状态但首个 seq 非 0 → `OutOfOrderSequenceException`
- 重复批未被识别 → 被当成乱序拒绝，客户端无谓崩溃

### 连接点

- 前文 `Kafka-2` / `Kafka-16`：Producer 端 seq/重试。
- 前文 `Kafka-11`：幂等/事务总览。
- 前文 `Kafka-19`：ProducerStateManager 持久化恢复后，正文在这里讲校验逻辑。

## 4. 总图

```text
append(batch)
  → ProducerAppendInfo.append
    → maybeValidateDataBatch(epoch, firstSeq)
      → checkProducerEpoch
        → checkSequence（CLIENT origin）
          → 通过后 appendDataBatch
            → 更新 ProducerStateEntry.lastSeq / batchMetadata
```

## 5. 关键边界

- 本篇只讲幂等部分，不展开事务状态机（K-12 第 3 篇）。
- 不把 `producerEpoch` 与 `leaderEpoch` / consumer generation 混同。
- 不把校验仅限定为客户端：REPLICATION origin 只校验 epoch，不校验 sequence。
- 不把重复批识别写成唯一的完整去重机制，但它是核心容忍分支。

## 6. 失败方案推演

1. **只靠客户端不重发**：网络重试无法避免。
2. **只校验 epoch，不校验 sequence**：同一 writer 内部分区顺序无法保障。
3. **把 sequence 乱序一律拒绝**：网络重试会被误判为乱序。
4. **重复批也当乱序**：客户端频繁崩溃，幂等失去意义。

## 7. 误解清单

- “producerEpoch 和 leaderEpoch 是同一个。”：前者是 writer 代次，后者是分区领导权版本。
- “sequence 校验对所有 append 都做。”：REPLICATION origin 只做 epoch 校验。
- “重复序列号会被拒绝。”：能通过 duplicate batch 容忍。
- “幂等只防重复。”：它还保证单分区写入顺序严格连续。
- “lastSeq 检查用 `==`。”：用 `inSequence`，含环绕回 0。

## 8. 证据清单

- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerAppendInfo.java:89`：maybeValidateDataBatch。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerAppendInfo.java:96`：checkProducerEpoch。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerAppendInfo.java:112`：checkSequence。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerAppendInfo.java:144`：仅当前 epoch 时检查 inSequence。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerAppendInfo.java:152`：inSequence。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateEntry.java:134`：findDuplicateBatch。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateEntry.java:140`：batchWithSequenceRange。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦幂等 append 校验，不展开事务 coordinator / marker（后续篇）。
- 目标正文：6000~10000 字。

## 10. 本轮重写主线

1. 从"为什么不能靠客户端不重发"开场。
2. 否定只校验 epoch、乱序一律拒、重复批也拒三种方案。
3. 解释三个角色的分工。
4. 解释 `ProducerAppendInfo` 追加校验流程。
5. 解释 `inSequence` 与环绕。
6. 解释 epoch fencing 与 REPLICATION 边界。
7. 解释 `findDuplicateBatch` 容忍重复。
8. 收网：幂等 = broker 侧 identity + epoch + sequence 三重校验。