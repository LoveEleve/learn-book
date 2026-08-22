# Kafka-12. Log Compaction 不是“删旧消息”——Cleaner 的 offset map、墓碑保留与 crash-safe 换段主链

> 场景：Kafka-3 已经讲过 LogSegment、索引和事务索引，Kafka-11 又讲过事务 marker 与 `read_committed`。但日志会一直增长，compacted topic 里的同一个 key 还可能出现成百上千次。Kafka 怎么在不误删最新值、tombstone、事务 marker 和 producer 状态的前提下，把旧数据清掉？答案不是“找到旧消息就删”，而是 Cleaner 先构造一份跨 segment 的 latest-offset 视图，再重写出一份新的安全日志。

## 先把真正的困惑摆出来：为什么不能直接删旧消息

假设一个 compacted topic 里有这样的记录：

```text
offset 10: key=order-1, value=created
offset 30: key=order-1, value=paid
offset 50: key=order-1, value=null   ← tombstone
offset 80: key=order-1, value=refunded
```

直觉方案是：看到同一个 key 出现多次，就把前面的删掉。可 Cleaner 在扫描 offset 10 时，并不知道 offset 80 还会出现什么；它必须先看完整个可清理范围，才能知道哪个 offset 才是这个 key 的最新值。

更复杂的是，日志里不只有普通消息：

- tombstone 代表“这个 key 已经被删除”；
- control batch 里可能有事务 marker；
- active producer 的最后一个 batch 可能携带后续 sequence 校验所需的状态；
- 跨 segment 的同一个 key，最新值可能在完全不同的 segment。

如果 Cleaner 只按“旧消息”三个字物理删除，最终会破坏的不只是数据，还包括事务可见性和 producer fencing。

```text
旧 segment：key=A → value=1
新 segment：key=A → value=2
  → 不先建立全局 latest-offset 视图
    → 可能误删 value=2 或保留 value=1
```

所以 Log Compaction 的核心问题不是“怎么删得快”，而是：**先确定哪些记录在整个 dirty range 内已经没有资格存在，再安全地重写 segment。**

*关键设计（斜体）：* *Cleaner 先由 `LogCleaner` 选择 dirty log，再在 cleanable offset range 内构造 `keyHash → latestOffset` 的 offset map；随后按 segment 分组写入 `.cleaned` 文件，额外保留 tombstone、事务 marker、active producer 边界，最后 flush 成功后通过 `UnifiedLog.replaceSegments` 进入 crash-safe 换段流程（`.cleaned → .swap → .deleted → 正式文件`）。*[模式: 全局索引视图 + 重写新段 + 可恢复交换]

## 第一层：LogCleanerThread 先决定“哪条日志最值得清理”

Compaction 不是每条日志写入后立刻同步执行的。`LogCleaner` 管理多个 `CleanerThread`，每个线程循环执行：

```text
tryCleanFilthiestLog()
  → 找一条最脏的 compacted log
    → Cleaner.doClean()
      → 清理完成
```

这里的“脏”不是简单的文件大小，而是这条日志里有多少内容已经有机会被 compaction 淘汰。Cleaner 线程挑 dirty ratio 更高、收益更大的日志处理，避免把 IO 消耗在几乎没有重复 key 的分区上。

`LogCleanerManager` 还会给 Cleaner 划出边界：

- **firstDirtyOffset**：本轮从哪里开始看可清理内容；
- **firstUncleanableOffset**：本轮最多清理到哪里，后面的内容暂时不能安全动。

这两个边界必须分开。dirty 说明“这里可能有旧值”，uncleanable 则是由 **active segment、compaction lag、firstUncleanableOffset 之后的安全边界** 等因素共同决定：到了这条线之后，本轮不能再安全往前清。

如果 Cleaner 直接从日志尾部一路清到当前 LEO，主链会先在哪失败？它可能把 active segment、仍会继续变化的事务和正在写入的 producer 状态一起纳入 compaction，导致清理结果与运行时写入发生竞争。

## 第二层：`Cleaner.doClean` 的第一步不是删，而是建立 offset map

`Cleaner.doClean()` 的主流程可以压缩成：

```text
计算 legacy delete horizon
  → buildOffsetMap
    → 得到 endOffset
      → groupSegmentsBySize
        → cleanSegments
```

其中最关键的是 `buildOffsetMap()`。

Cleaner 会在 `[firstDirtyOffset, firstUncleanableOffset)` 范围内扫描 dirty segments，把每个 key 的最新 offset 放进 map。这个范围可能跨越多个 segment，所以 Cleaner 不是“当前 segment 内去重”，而是对整个本轮 dirty range 建立视图。

```text
segment-1: key=A offset=10
segment-2: key=A offset=80
segment-3: key=A offset=130

offset map:
  hash(A) → 130
```

等这份 map 建好以后，Cleaner 回头重写旧 segment 时才知道：offset 10 和 80 已经不是 key=A 的最新记录，可以丢；offset 130 仍然必须保留。

这也是为什么 compaction 不能简单做成单遍“读到旧值就删除”：你需要先知道后面有没有更新。

## 第三层：`SkimpyOffsetMap` 用有限内存表达“key 的最新位置”

如果 topic 的 key 数量非常大，Cleaner 不能给每个完整 key 都建一个无限增长的 HashMap。Kafka 使用 `SkimpyOffsetMap`，把 key 的 hash 映射到 offset。

它的思路是：

```text
完整 key
  → hash
    → offset map slot
      → 保存该 hash 对应的 latest offset
```

这个结构节省内存，但代价是：它不是保存完整 key 的无损字典，而是一个受容量、hash 和探测策略约束的 offset map。slot 冲突可以靠 probing 继续找位置，但如果两个不同 key 恰好产生同一个 hash，这个结构本身并不能再把它们区分开来；正确性依赖 hash 算法与其极低碰撞概率。Cleaner 还会监控 map 是否填满；如果容量到达上限，就停止继续构建本轮 map，后续清理范围也必须服从这份有限视图。

所以 `SkimpyOffsetMap` 的定位不是“完美保存所有 key”，而是**在固定 dedupe buffer 下，为 Cleaner 提供一份可执行的 latest-offset 索引**。

如果把它误解成完整无碰撞 map，主链会先在哪出问题？读者会以为 Cleaner 可以无限覆盖整个 dirty range；实际上 map 容量、碰撞和本轮边界都会决定一次能映射多少内容。

## 第四层：segment 分组不是性能小优化，而是索引与 offset 的安全约束

offset map 建好后，Cleaner 不会把所有 segment 粗暴合成一个巨大的新文件，而是调用 `groupSegmentsBySize()` 分组。

分组时会同时考虑：

- `.log` 数据大小；
- offset index 大小；
- time index 大小；
- segment offset 范围不能超过索引可表达的限制。

```text
多个旧 segment
  → 按 log/index/timeindex 上限分组
    → 每组生成一个 cleaned segment
```

为什么不能只看 `.log` 大小？因为一个新 segment 即使数据文件没超限，offset index 或 time index 也可能超出实现边界。Cleaner 必须让数据文件和索引文件一起处于可加载状态。

这一步还影响 compaction 的原子替换：`cleanSegments()` 每组生成一个新的 cleaned segment，后面用新的段替换这一组旧段，而不是把整个分区一次性重写成一个不可控的大文件。

## 第五层：`cleanInto` 不是“只保留 latest key”，而是一套保留规则

真正决定每个 batch / record 去留的是 `cleanInto()`。它会结合 offset map、delete horizon、事务 metadata 和 active producer 状态，计算 retention。

### 普通 key/value

普通记录只有在它的 offset 仍然是 key 的 latest offset 时才保留。旧值可以丢，因为后面已有更新值覆盖它。

### tombstone

tombstone 不是普通的 null value。它代表“删除这个 key”，但不能刚写入就被清掉，否则落后副本、落后消费者或恢复中的读取方还没来得及观察到这次删除，旧 value 就可能重新变得“像是还存在”。

因此 tombstone 要等到 delete retention horizon 之后，才有资格被清理。

```text
tombstone 写入
  → 等待 delete horizon
    → 确认旧 value 不会再从落后副本恢复
      → 才允许删除 tombstone
```

### 事务 marker

事务 marker 也不能跟普通旧 value 一样直接删。Cleaner 会收集完整 segment range 的 aborted transaction metadata，并重建 cleaned segment 的 transaction index。marker 的保留不是一句“事务数据还没删完”就能概括，它实际要同时看 batch retention、delete horizon 和已经收集到的事务 metadata；只有这些条件一起允许时，marker 才能安全离场。

### active producer 最后状态

Cleaner 还要避免删除 active producer 的最后一个关键 batch。`lastRecordsOfActiveProducers` 用于判断：

- producer 已不活跃，可以删除它的记录；
- producer 仍活跃且有 last data offset，要保留包含这个 offset 的 batch；
- 最后的记录是 transaction marker 时，要保留 marker，因为它可能携带 producer epoch fencing 所需的最后状态。

所以 compaction 的保留规则不是一张简单的“latest key 表”，而是：

```text
latest key record
  + tombstone horizon
  + transaction marker/index
  + active producer last batch
  + cleaning round upper bound
```

## 第六层：事务 metadata 为什么要跨 segment 传递

`Cleaner.doClean()` 创建一个 `CleanedTransactionMetadata`，然后把它传给每个 segment group 的 `cleanSegments()`。

这不是为了传递普通统计信息，而是为了让多个 cleaned segment 之间共享事务边界：

- 当前 segment 里看到的 aborted transaction，可能影响后续 segment 的 txnindex；
- 新 cleaned segment 需要重建完整的 transaction index；
- marker 是否能删除，取决于事务数据是否已经具备删除条件。

所以 `cleanSegments()` 开始时会从整个待清理 segment range 收集 aborted transactions，而不是只看当前 segment 的局部内容。

如果每个 segment 独立重建 txnindex，主链会先在哪失败？一个事务的 marker 和事务数据可能跨 segment，单段视角会遗漏边界，导致 `read_committed` 在清理后无法正确找到 aborted transaction。

## 第七层：为什么一定是 `.cleaned` → flush → crash-safe swap

Cleaner 不会原地覆盖旧 segment。它先调用 `UnifiedLog.createNewCleanedSegment()` 创建带 `.cleaned` 后缀的新段，把保留内容写进去，再执行一套 **crash-safe clean-and-swap** 流程：

```text
cleaned.onBecomeInactiveSegment()
  → cleaned.flush()
    → replaceSegments 进入 .cleaned → .swap → .deleted → 正式文件 的可恢复交换流程
```

这个顺序的安全性非常重要：

1. 新 segment 先独立写入，旧 segment 仍然可读；
2. 新 segment flush 到磁盘，确保数据与索引已经落地；
3. `replaceSegments()` 不是一次文件系统原子 rename，而是一套多阶段 swap 协议；
4. 如果 broker 在中间崩溃，`LogLoader` 还能根据 `.cleaned` / `.swap` / `.deleted` 的状态继续完成或回滚这次替换；
5. 旧 segment 后续再异步清理。

如果直接覆盖旧 segment，Cleaner 中途崩溃时可能出现：

- `.log` 写了一半；
- `.index` 与 `.log` 对不上；
- `.txnindex` 没有完成；
- broker 重启后无法判断哪些内容可信。

所以 compaction 的核心不是“删除旧文件”，而是**生成一份完整的新文件，再把它接入日志**。

## 第八层：LogCleaner 处理的是一个持续循环，而不是一次性全量整理

CleanerThread 的主循环是：

```text
tryCleanFilthiestLog()
  → 有脏日志就清理
  → 没有就 backoff
  → maintainUncleanablePartitions()
  → 下一轮继续
```

一次 `doClean()` 返回的不是“整个日志永远清完”，而是一个 `endOffset` 和本轮 `CleanerStats`。由于 offset map 容量、uncleanable 边界、活跃 segment 和 compaction lag 都会限制一轮清理范围，后续还要继续处理剩余 dirty 区间。

这解释了为什么 Kafka 的 compaction 是后台渐进过程：它不能为了“一次整理干净”阻塞正常读写，而是每轮处理一段安全范围，下一轮再继续。

## 收网：Compaction 是重写安全新日志，不是在旧日志上删几条记录

把整篇压成一句话：Kafka Cleaner 先为 dirty range 建立 key latest-offset map，再按数据/索引大小分组重写 segment；写入过程中额外保留 delete horizon 内的 tombstone、事务 marker、active producer 边界，重建 transaction metadata；新 `.cleaned` segment flush 成功后，再由 `UnifiedLog.replaceSegments` 进入 crash-safe 换段流程。

```text
LogCleanerThread
  → filthiest compacted log
    → dirty / uncleanable boundary
      → SkimpyOffsetMap
        → groupSegmentsBySize
          → cleanInto
            → tombstone/marker/producer 保留
              → flush cleaned segment
                → replaceSegments crash-safe 换入
```

到这里，主线只发生了八件事。

第一，Cleaner 先选最脏的 log，不是每条写入都同步清理。

第二，compaction 先建跨 segment 的 latest-offset map，再决定去留。

第三，SkimpyOffsetMap 用有限内存提供 key hash 到 latest offset 的索引。

第四，segment 分组同时受 log、offset index、time index 和 offset 范围约束。

第五，tombstone、事务 marker、active producer 最后状态都有额外保留规则。

第六，事务 metadata 跨 segment 传递，保证 cleaned txnindex 完整。

第七，新 segment 必须 flush 成功后才能 replaceSegments。

第八，Cleaner 是持续后台循环，一轮只处理安全的 dirty range。

**本篇的一句话困惑**：Log Compaction 为什么不是找到旧消息就删？

**本篇的一句话顿悟**：因为 Cleaner 必须先建立 dirty range 的 latest-offset 视图，再连同 tombstone、事务 marker、producer 边界一起重写出安全的新 segment，最后通过 crash-safe 换段流程接入日志，而不是在旧日志上直接动刀。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“Compaction 等于 retention delete。”** 前者按 key 去重，后者按时间/大小淘汰 segment。
2. **“Cleaner 只看当前 segment。”** offset map 覆盖 dirty range，判断可能跨多个 segment。
3. **“tombstone 可以立即删除。”** 必须等 delete horizon，避免旧副本重新出现 value。
4. **“事务 marker 和普通消息一样清理。”** marker 要结合事务 metadata 与 delete horizon。
5. **“cleaned 文件写完就算完成。”** 必须 flush 成功后再 replaceSegments。

### 关键证据清单

- `storage/src/main/java/org/apache/kafka/storage/internals/log/LogCleaner.java:463`：CleanerThread 主循环与脏日志选择。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:138`：doClean 主流程。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:153`：buildOffsetMap。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:172`：segment 分组。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:205`：cleanSegments 与 cleaned segment。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:227`：收集 aborted transactions。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:275`：flush 后 replaceSegments。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:566`：groupSegmentsBySize。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:635`：buildOffsetMap。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/SkimpyOffsetMap.java`：hash offset map。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/CleanedTransactionMetadata.java`：事务清理 metadata。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦 compact cleaner 主链，不展开完整 retention delete、RemoteLogStorage、Streams state store。
- 本篇把 tombstone、事务 marker、active producer 状态视为清理安全边界，不把它们简化成普通 key/value。
- 不把 offset map、cleaned segment、UnifiedLog.replaceSegments 混成同一个组件：它们分别负责索引、重写、换段。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-3`（LogSegment / txnindex / ProducerStateManager）、`Kafka-11`（事务 marker 与可见性）。
- 后续桥接：可以回到全篇一致性 review，或继续补 Kafka 存储恢复与 snapshot/checkpoint 主线。