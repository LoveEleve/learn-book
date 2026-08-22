# Kafka-33. 幂等 producer 到底靠什么防重——producerId / producerEpoch / sequence 与 append 校验主链

> 场景：Kafka-11 讲过幂等与事务是两个层次，Kafka-2/16 讲过 Producer 端的重试与确认。但“幂等 producer 到底靠什么防重”在 broker 侧的校验逻辑一直没有正面展开。本篇把 `ProducerAppendInfo` 的 epoch / sequence 校验、`ProducerStateEntry` 的 lastSeq / batchMetadata 维护讲透。这是 K-12 事务幂等域第 2 篇。

## 先把真正的困惑摆出来：同样是 seq=5，重试和乱序怎么区分

设想同一台 producer 向某个分区写了一批 seq=4 的消息，然后：

- 场景 A：网络重试，又发了一遍 seq=4；
- 场景 B：客户端 bug，跳过了 seq=4，直接发 seq=5。

两次都出现“seq 不是 lastSeq+1”。broker 该怎么分辨哪次能容忍、哪次要拒绝？

答案是：**不能只靠 sequence 本身，而要靠 broker 是否还记着“上一个 seq 是哪一批、以及这一批的 seq 范围”。**

如果 broker 记得刚写过 seq=4 且它和这次携带的 seq=4 完全一致（范围匹配），这就是**重试的同一批**，可以容忍；如果 seq 跳跃到 5 而 lastSeq 是 3，就是**乱序**，必须拒绝。

*关键设计（斜体）：* *幂等防重发生在 broker 的 `ProducerAppendInfo` 追加校验阶段：`producerId` 标识 writer，`producerEpoch` 阻止过期 writer，`sequence` 保证分区内严格连续。跨 epoch 或首次写入有特定规则；相同 seq 范围的批量通过 `findDuplicateBatch` 容忍为成功重试。*[模式: identity + epoch + sequence 三重校验]

## 第一层：三个角色各管一件事

幂等三元组的分工非常清晰：

- **`producerId`**：标识“是哪台 producer 在这个分区写”。
- **`producerEpoch`**：标识“这是该 producer 的第几代”。epoch 变大通常是因为新的 producer 实例接管、或重新初始化；epoch 变小则一定是过期 writer。
- **`sequence`**：同一个 epoch 内分区写入的顺序号，要求严格连续。

这三个合起来，broker 才能在同一分区内判断：

- 这是同一个人写的吗？
- 这个人还是当前合法代吗？
- 这批的序号衔接得上吗？

缺任何一个，防重都不成立。

## 第二层：`ProducerAppendInfo` 是校验和事务累积的入口

它的类注释说得很清楚：用“最后一次成功追加后的状态”初始化，校验每个新 batch 的 epoch 和 sequence，同时累积事务元数据。

```text
append(batch)
  → maybeValidateDataBatch(epoch, firstSeq, offset)
    → checkProducerEpoch
      → checkSequence（CLIENT origin）
```

注意这里 `origin` 很重要：它决定校验到什么程度。

- `CLIENT`：完整校验 epoch + sequence；
- `REPLICATION`：只校验 epoch，假设 leader 端已经做过 sequence 校验；
- 非客户端（如 group coordinator 的 offset commit）：只做 epoch 校验。

这解释了为什么“不是所有 append 都校验 sequence”这个看似矛盾的结论。

## 第三层：checkProducerEpoch——epoch 变小就是过期 writer

`checkProducerEpoch()` 的核心逻辑是：

```text
if (producerEpoch < updatedEntry.producerEpoch())
  → InvalidProducerEpochException
```

- 如果 epoch 小于已知值，说明这个 writer 已经过期，抛 `InvalidProducerEpochException`；
- 但 REPLICATION origin 时只记 warn，因为批量复制时 epoch 可能因 leader 端延迟而短暂滞后。

从 2.7 起，Kafka 用 `InvalidProducerEpoch` 替代了早期的 `ProducerFenced`，语义是“让客户端中止当前事务并重试”，而不是致命的 fencing 错误。

## 第四层：checkSequence——分区内顺序连续与环绕

`checkSequence()` 的逻辑比只看 `nextSeq == lastSeq+1` 更细致：

```text
一批新 append
  → verification 特判（txn v2 无状态时首个 seq 必须 0）
  → appendFirstSeq > lowestSequence？→ OutOfOrder
  → producerEpoch != 当前 epoch？
    → appendFirstSeq != 0 → 新 epoch 首条必须从 0 开始
  → 否则（epoch 相同）
    → inSequence(lastSeq, appendFirstSeq) ? 通过 : OutOfOrder
```

`inSequence` 的定义：

```java
nextSeq == lastSeq + 1L || (nextSeq == 0 && lastSeq == Integer.MAX_VALUE)
```

也就是说：

- 常规连续性：`next = last + 1`；
- 环绕：`last = MAX_VALUE` 后，next 允许回到 0。

如果不处理环绕，使用到 `Integer.MAX_VALUE` 序号时有大量重复写入时会误判。

## 第五层：新 epoch 的 sequence 规则

当 producer epoch 变化时，sequence 不是任意的，而是有严格起点：

```text
producerEpoch != updatedEntry.producerEpoch
  → appendFirstSeq != 0
    → OutOfOrder（新 epoch 应从 seq=0 开始）
```

也就是说：**一旦 epoch 升级，新的写入序列必须从 0 重新开始计数。** 这是为了把“新 writer 的第一批”与“旧 writer 的历史”清晰分隔。

如果允许新 epoch 从任意 seq 开始，一个刚升级的 producer 就可能发出与旧 epoch 重叠的 seq，使 broker 无法判断是重试还是新数据。

## 第六层：重复批怎么被容忍——findDuplicateBatch

幂等 producer 的核心场景是网络重试：客户端发了一批 seq=4，broker 写成功了，但响应丢了，客户端重发同一批 seq=4。

`ProducerStateEntry.findDuplicateBatch()` 检查：

```text
batch的epoch == 当前epoch
  && batchMetadata 中存在与 batch.baseSeq/lastSeq 完全一致的批
```

如果存在，说明这一批**之前已经成功写过**，broker 直接当作成功返回，不再追加新数据。

这就是“重试 vs 乱序”的分水岭：

- **重试**：seq 范围与已记录 batch 完全一致 → 容忍；
- **乱序**：seq 范围不匹配，且 `inSequence` 不通过 → `OutOfOrderSequenceException`。

没有 `findDuplicateBatch`，幂等 producer 在网络重试下会反复被当成乱序拒绝，幂等就失去意义了。

## 第七层：为什么 offset commit 只校验 epoch

`AppendOrigin` 的注释里有一句很关键：group coordinator 发起的 offset commit 这类写入，“没有 sequence 号码”。因此它只能用 epoch 校验去重，不能做 sequence 连续性校验。

这也是为什么你会看到 `checkSequence` 里用 `origin == AppendOrigin.CLIENT` 守卫：

- 只有真正由客户端 producer 发起的批才可能带完整 sequence；
- 来自 coordinator / 复制的写入不属于幂等 producer 的 sequence 语义。

## 收网：幂等 = broker 侧 identity + epoch + sequence 三重校验

把整篇压成一句话：幂等 producer 的防重发生在 broker 的 `ProducerAppendInfo` 追加校验阶段：`producerId` 标识 writer，`producerEpoch` 阻止过期 writer，`sequence` 保证分区内暂时连续；`inSequence` 处理环绕，`findDuplicateBatch` 识别并容忍重试的同一批，`OutOfOrderSequenceException` 拒绝乱序。

```text
append(batch)
  → checkProducerEpoch（epoch 过期？→ InvalidProducerEpoch）
    → checkSequence（CLIENT origin 才做）
      → 同 epoch → inSequence？
        → 通过 → appendDataBatch
        → 失败 → OutOfOrderSequenceException
      → 重复批 → findDuplicateBatch 容忍
```

到这里，主线只发生了五件事。

第一，三个角色各管一件事：id / epoch / seq。

第二，`ProducerAppendInfo` 同时做校验与事务累积。

第三，epoch 变小是过期 writer，回 0 是环绕。

第四，新 epoch 必须从 seq=0 开始。

第五，`findDuplicateBatch` 把重试与乱序区分开。

**本篇的一句话困惑**：同样 seq=5，为什么重试被容忍、乱序被拒绝？

**本篇的一句话顿悟**：幂等防重靠 broker 对 identity / epoch / sequence 的三重校验；重试的 seq 范围与已记录 batch 完全一致可容忍，乱序则因为 `inSequence` 不通过而抛 `OutOfOrderSequenceException`。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“producerEpoch = leaderEpoch = generation.”** 三者不同：writer 代次 / 分区领导权版本 / consumer 世代。
2. **“sequence 对所有 append 都校验。”** REPLICATION origin 只校验 epoch。
3. **“重复序列号会被拒绝。”** 通过 `findDuplicateBatch` 容忍。
4. **“幂等只防重。”** 它还保证单分区写入顺序严格连续。
5. **“lastSeq 检查就是 `== last+1`。”** 还有环绕回 0 和 verification txn v2 特判。

### 关键证据清单

- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerAppendInfo.java:89`：maybeValidateDataBatch。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerAppendInfo.java:96`：checkProducerEpoch。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerAppendInfo.java:112`：checkSequence。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerAppendInfo.java:144`：仅同 epoch 时检查 inSequence。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerAppendInfo.java:152`：inSequence。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateEntry.java:134`：findDuplicateBatch。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateEntry.java:140`：batchWithSequenceRange。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦幂等 append 校验，不展开事务 coordinator（K-12 第 3 篇）。
- `producerEpoch` 只参与幂等 writer fencing，不混入其它版本号语义。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-2/16`（Producer 端 seq/重试）、`Kafka-11`（幂等总览）、`Kafka-19`（ProducerStateManager 恢复）。
- 后续桥接：下一篇进入 K-12 第 3 篇（TransactionCoordinator 与 `__transaction_state`）。