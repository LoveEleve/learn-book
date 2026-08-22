# Kafka-31. Cleaner 真正删什么、留什么——cleanInto、墓碑、事务 marker 与 crash-safe swap 主链

> 场景：Kafka-30 讲了 Cleaner 怎么先为 dirty range 建一份 `keyHash → latestOffset` 视图，但那只是“地图”。真正危险的地方在下一步：**有了这份视图后，Cleaner 具体删什么、留什么？** tombstone、事务 marker、active producer 最后状态都不能简单套“不是最新值就删”的规则。本篇把 compaction 的真正保留规则讲透。这是 K-11 Log Compaction 域第 3 篇。

## 先把真正的困惑摆出来：知道了 latest offset 之后，为什么还不能直接删

如果有了 offset map，最直觉的做法是：

```text
record.offset < map.latestOffset(key)
  → 旧值
    → 删除
```

对普通 key/value 来说，很多时候这确实成立。但 Kafka 的日志里不只有普通 value，还有：

- **tombstone**：删除一个 key 的标记；
- **control batch**：事务 COMMIT / ABORT marker；
- **active producer 的最后状态**：即使没有新数据，也需要保留，用于 sequence 和 fencing；
- **清理轮次最后一个 batch**：即使它最后会变空，也不能直接丢掉，否则 last offset 会断裂。

所以 compaction 的真正难点不是“找出旧值”，而是：**在知道 latest offset 的前提下，额外保住这些边界。**

*关键设计（斜体）：* *Cleaner 先按 batch 决定保留语义，再按 record 决定去留：普通 key 只有 latest offset 且 value 仍有效时才保留；tombstone 要等 delete horizon；control batch/事务 marker 要结合 transaction metadata 与 delete horizon；active producer 的最后 batch 即使 records 被删空也要保留（`RETAIN_EMPTY`）。最后所有保留下来的内容先写入 `.cleaned` segment，flush 成功后再走 crash-safe clean-and-swap 协议。*[模式: 先 batch 后 record + 边界优先 + 可恢复交换]

## 第一层：`checkBatchRetention` 先决定 batch 这一层的命运

`cleanInto()` 并不是直接对每条 record 调 `shouldRetainRecord`。它先通过 `RecordFilter.checkBatchRetention(batch)` 决定这个 batch 的粗粒度命运：

- `RETAIN_EMPTY`
- `DELETE_EMPTY`
- `DELETE`

这一步由三类判断共同决定：

1. **是否可以整体丢弃这个 batch**（`shouldDiscardBatch`）；
2. **它是不是 active producer 的最后一个 batch**；
3. **它是不是本轮清理的最后一个 batch**。

### RETAIN_EMPTY

即使 batch 里的 records 最后被删空，这个 batch 外壳也要保留。保留原因有三种（源码注释写得很清楚）：

- producer 已不活跃，但这个 batch 恰好是它最后一个状态边界；
- producer 还活跃，batch 里带着 last sequence / last offset；
- 这条 batch 本身就是最后一个 control marker，承载最后一个 producer epoch。

### DELETE_EMPTY

batch 本身保留，但其中 records 可以删空。典型场景是某条记录已经不是 latest value，但这个 batch 还不能整体丢掉。

### DELETE
n
batch 和里面的 records 都可以删除。

所以 Kafka 不是“按 record 独立删除”，而是**先在 batch 粒度上做一轮粗裁决，再在能保留的 batch 里做 record 级过滤。**

## 第二层：为什么 tombstone 不能一写完就删

tombstone 看起来只是一个 `value = null` 的普通记录，为什么 Kafka 对它这么谨慎？

因为 tombstone 不是“旧值”，而是**删除动作的证据**。如果 tombstone 太快被清掉，而某个落后消费者、落后副本或恢复中的读取方还没来得及看到这个 tombstone，它就可能重新把旧 value 当成“当前仍有效”的值。

所以 tombstone 的规则不是“不是最新就删”，而是：

```text
tombstone
  → latestOffsetForKey?
    → 是：继续看 deleteHorizon
      → 未到 horizon：保留
      → 到 horizon：可删
```

对 legacy record（magic < V2）和现代 record，这个 horizon 的来源不同，但核心都是：**删除标记必须比“普通更新值”多活一段时间。**

这也是源码里 `shouldRetainDeletes` 要专门分 legacy / non-legacy 的原因。

## 第三层：事务 marker 不能按普通 key/value 处理

control batch 里的事务 marker（COMMIT / ABORT）不参与 key 的 latestOffset 比较。它根本不是"某个 key 的值"，而是事务边界。

Cleaner 对 control batch 的策略是：

- 先通过 `shouldDiscardBatch(batch, transactionMetadata)` 判断这个 marker 对当前事务边界是否还必要；
- 即使理论上“可以丢”，也要再看 `deleteHorizonMs` 是否到期；
- 在没到 horizon 之前，即使 marker 最终 records 会删空，也仍可能以 `RETAIN_EMPTY` 的方式保留 batch 外壳。

源码注释特别强调：**transaction markers 复用 tombstone retention 逻辑来延迟删除。** 这意味着 marker 的删除不是“事务结束了就删”，而是“事务边界已经不再需要，且 delete horizon 到了，才允许删”。

这是 `read_committed` 能继续正确工作的基础。否则，consumer 看到一段事务数据，却看不到它后面的 COMMIT / ABORT marker，就无法判断这段数据该不该可见。

## 第四层：active producer 的最后状态为什么必须保留

Cleaner 不只是为读路径服务，它还要保护 **producer fencing / sequence continuity**。

`lastRecordsOfActiveProducers` 这张表告诉 Cleaner：每个活跃 producer 最近一个关键 batch 是哪条。注释里列出了三种必须保留的情况：

1. producer 已不活跃，记录可删；
2. producer 仍活跃，必须保留包含 `lastDataOffset` 的那个 batch；
3. 最后的记录是 transaction marker 时，必须保留 marker，因为它带着最后一个 producerEpoch，是 fencing 的关键边界。

也就是说，就算某条 batch 里的所有普通 records 都已经不是 latest value，Kafka 仍然可能因为它承载着 producer 的最后状态而选择 `RETAIN_EMPTY`。

这一步如果做错，后果不是“读到了旧值”，而是**重启后 producer 的 sequence / epoch 无法正确恢复**，幂等与事务语义都会坏掉。

## 第五层：为什么要额外保留“本轮清理的最后一个 batch”

Cleaner 还有一个很容易忽略的规则：如果当前 batch 的 `nextOffset()` 恰好等于 `upperBoundOffsetOfCleaningRound`，即它是**本轮清理范围的最后一个 batch**，即使 records 被删空，也要 `RETAIN_EMPTY`。

原因是：**不能把本轮 cleanable offset 的末尾边界抹掉。** 如果最后一个 batch 被完全删没了，后续逻辑可能丢失“这轮清理到了哪里”的 last offset 信息。

所以 `RETAIN_EMPTY` 不只是为 producer / marker 服务，它也是在保护本轮清理边界本身。

## 第六层：`shouldRetainRecord` 才真正决定普通 value / tombstone 是否留下

在 batch 级别通过了 `checkBatchRetention` 之后，Kafka 才逐 record 判断：

```text
shouldRetainRecord(record)
  → pastLatestOffset?（offset > map.latestOffset）
    → 是：保留（这是 map 没覆盖到的新尾巴）
  → record.hasKey? 否：无效消息
  → 找到 key 对应的 foundOffset
    → latestOffsetForKey = record.offset >= foundOffset
    → shouldRetainDeletes = tombstone 是否仍在 delete horizon
    → isRetainedValue = record.hasValue || shouldRetainDeletes
    → latestOffsetForKey && isRetainedValue ? 保留 : 删除
```

这里有两个很重要的边界：

### pastLatestOffset

如果一条 record 的 offset 已经大于 map.latestOffset()，说明这条记录超出了本轮 offset map 的覆盖范围（例如 map 满了提前停了）。这种 record 不能被误删，必须保留。

### latestOffsetForKey

对普通 key 来说，只有这个 record 的 offset 等于（或大于）map 里为该 key 记住的 latest offset，它才是本轮 dirty range 里“应当留下”的那条值。更早的值都会被删掉。

所以 `shouldRetainRecord` 最终是在执行一个二维判断：

- 这条记录对 key 来说是不是最新？
- 这条记录作为 tombstone / value，目前是否还允许可见？

## 第七层：`.cleaned → .swap → .deleted → 正式文件` 为什么 crash-safe

Cleaner 把要保留的内容写进 `.cleaned` segment 之后，不会直接替换旧 segment，而是走 `LocalLog.replaceSegments()` 的多阶段交换协议：

1. 新 segment 先以 `.cleaned` 后缀存在；
2. 再统一改名为 `.swap`；
3. 旧 segment 改为 `.deleted` 并调度异步删除；
4. `.swap` 文件去掉后缀，成为正式 segment；
5. 崩溃恢复时，`LogLoader` 能根据 `.cleaned` / `.swap` / `.deleted` 的组合状态继续完成或回滚。

```text
.cleaned
  → .swap
    → old -> .deleted
      → .swap -> 正式文件
```

这不是“原子 rename”，而是**多阶段、可恢复的交换协议**。它的目的不是一步到位，而是无论崩溃在哪个阶段，都能在恢复时判断当前处于哪一步并继续推进或清理。

如果 Cleaner 直接原地覆盖旧 segment，中途崩溃后你甚至分不清：

- 新 `.log` 写完了没？
- `.index` 和 `.timeindex` 配没配上？
- `.txnindex` 是否完整？

所以 crash-safe swap 的核心不是“快”，而是**每一步都可恢复**。

## 收网：Compaction 真正难的是“保边界”，不是“找旧值”

把整篇压成一句话：offset map 只是告诉 Cleaner 哪些 key 的旧值可以删，但真正决定去留的是 `cleanInto` 这一层的边界保护：tombstone 要等 delete horizon，事务 marker 要等 transactionMetadata 与 delete horizon 一起放行，active producer 的最后状态要保留，本轮最后一个 batch 也要保留边界；最终所有保留结果先写成 `.cleaned` segment，再通过 `.cleaned → .swap → .deleted → 正式文件` 的 crash-safe 流程接入日志。

```text
offset map
  → checkBatchRetention
    → RETAIN_EMPTY / DELETE_EMPTY / DELETE
      → shouldRetainRecord
        → value / tombstone / marker / producer 边界
          → 写入 .cleaned
            → crash-safe swap
```

到这里，主线只发生了六件事。

第一，Cleaner 先按 batch 决定粗粒度保留语义。

第二，tombstone 不能一写完就删。

第三，事务 marker 不能按普通 value 处理。

第四，active producer 的最后状态必须保留。

第五，`shouldRetainRecord` 才真正决定 record 级去留。

第六，换段是 crash-safe 多阶段协议，不是一步 rename。

**本篇的一句话困惑**：知道 latest offset 之后，Cleaner 为什么还不能直接删旧值？

**本篇的一句话顿悟**：因为真正难的不是找出旧值，而是保住 tombstone、marker、producer 最后状态和清理边界；没有这些边界，compaction 会把语义一起删掉。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“不是 latest value 就一定删。”** tombstone、marker、active producer 最后状态另有规则。
2. **“RETAIN_EMPTY 没意义。”** 它在保 producer/marker/offset 边界。
3. **“control batch 就跟普通 value 一样按 key 判断。”** 它走 transaction metadata + delete horizon 路径。
4. **“replaceSegments 是原子 rename。”** 它是多阶段 crash-safe 交换协议。
5. **“pastLatestOffset 说明 map 出错。”** 通常只是本轮 offset map 提前停了，尾部必须保留。

### 关键证据清单

- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:318`：RecordFilter 与 checkBatchRetention。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:323`：marker/tombstone 共享保留逻辑。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:339`：isBatchLastRecordOfProducer。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:349`：BatchRetention 选择。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:365`：shouldRetainRecord。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:472`：shouldDiscardBatch。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/LocalLog.java:964`：replaceSegments 的 crash-safe 交换步骤。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/LocalLog.java:1021`：.cleaned → .swap。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/LocalLog.java:1030`：旧 segment → .deleted。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/LocalLog.java:1050`：.swap → 正式文件。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦 cleanInto 保留规则与换段流程，不重复 offset map 构建。
- 不把 tombstone / marker / active producer 状态混成“latest value”判断。
- 不展开 LogLoader 在恢复路径中的全部实现，只点 crash-safe 语义。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-12`（compaction 总览）、`Kafka-30`（offset map 构建）、`Kafka-11`（事务 marker / read_committed）。
- 后续桥接：K-11 Log Compaction 域 3 篇收官。下一步可做全篇一致性收束，或扩展 K-8 Raft 状态机专题。