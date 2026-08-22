# Kafka-12 重写规划

> 题目：Log Compaction 不是“删旧消息”——Cleaner 的 offset map、墓碑保留与原子换段主链
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：解释 Kafka log compaction 如何在不破坏 offset、事务边界、producer 状态与读写并发的前提下，保留每个 key 的最新记录：`LogCleaner` 如何挑脏日志，`Cleaner` 如何建 offset map、分组清理 segment、保留 tombstone/marker，最后如何 flush 后原子替换。

## 1. 读者困惑

- Compaction 到底保留什么，为什么不是简单按时间删除旧消息？
- Cleaner 怎么知道某个 key 的“最新 offset”在哪？
- `SkimpyOffsetMap` 为什么只存 hash，不存完整 key？碰撞怎么办？
- tombstone 为什么不能立刻删？事务 marker 为什么也不能随便删？
- 为什么要先生成 `.cleaned` segment、flush 完再 replace，而不是原地改旧文件？
- Cleaner 如何避免清理进行中的事务和 active producer 状态？

## 2. 一句话顿悟

**Log Compaction 的核心不是按时间删消息，而是先在 dirty offset range 内建立 `key → latest offset` 的索引，再逐 segment 重写，只保留仍是 key 最新值、仍需保留的 tombstone/事务 marker 与 active producer 边界；新 segment flush 成功后再由 `UnifiedLog.replaceSegments` 原子换入。**

## 3. 五要素卡片

### 读者问题

一个 key 在日志里出现 100 次，Cleaner 如何只留下最后一次，又不误删 tombstone、事务 marker 或 producer sequence 所需的最后 batch？

### 入口

- `LogCleaner`：Cleaner 线程池，选择最脏的 compacted log
- `LogCleanerManager` / `LogToClean`：计算 dirty / uncleanable 边界
- `Cleaner.doClean`：offset map → segment 分组 → 清理
- `SkimpyOffsetMap`：key hash → latest offset
- `Cleaner.cleanInto`：决定 batch/record 是否保留
- `CleanedTransactionMetadata`：重建事务索引所需的 metadata
- `UnifiedLog.replaceSegments`：flush 后原子换段

### 状态核心

- cleanable dirty range / firstUncleanableOffset
- offset map 的 latest offset
- tombstone delete horizon
- active producer last record retention
- transaction marker / aborted transaction metadata
- `.cleaned` segment → flush → swap

### 失败路径

- 原地删除旧记录 → 崩溃时索引与日志不一致
- 只按 key 保留最新值 → tombstone/事务 marker/producer state 被误删
- 只清理单个 segment → 跨 segment 的最新 key 判断错误
- 清理到 active/uncleanable 边界之后 → 删除仍可能变化的事务或写入
- hash map 满了还继续写 → offset map 结果不完整，误删最新值

### 连接点

- 前文 `Kafka-3`：LogSegment / txnindex / ProducerStateManager 是本篇清理安全边界。
- 前文 `Kafka-11`：事务 marker 与 aborted transaction metadata 不能脱离 compaction 生命周期。
- 后文可回到存储恢复，解释 cleaned segment、checkpoint 与崩溃恢复。

## 4. 总图

```text
LogCleanerThread
  → 选 filthiest compacted log
    → LogToClean 给出 dirty / uncleanable 边界
      → Cleaner.buildOffsetMap
        → SkimpyOffsetMap(keyHash → latestOffset)
          → groupSegmentsBySize
            → cleanInto 写 .cleaned segment
              → 保留最新 key / tombstone / marker / producer 边界
                → flush
                  → UnifiedLog.replaceSegments 原子换段
```

## 5. 关键边界

- 本篇只讲 delete/compact 清理主链，不展开完整 retention delete 算法。
- 不把 compaction 写成“每个 key 永远只剩一条”：tombstone、事务 marker、active producer batch 有额外保留规则。
- 不把 `SkimpyOffsetMap` 写成无碰撞的完整 map：它以 hash 为索引，容量与碰撞策略决定能映射多少 key。
- 不把 `firstUncleanableOffset` 与 dirty offset 混成一个边界：前者是本轮不能安全清理的上界。

## 6. 失败方案推演

1. **按时间直接删除旧消息**：compacted topic 的 key 最新值可能被删掉。
2. **扫描到旧值就立刻物理删除**：还没看到后面的同 key 最新值，无法安全决定。
3. **只保留 latest key record**：tombstone/marker/active producer state 被误删。
4. **原地重写旧 segment**：中途崩溃会留下日志、索引、txnindex 的半成品状态。

## 7. 误解清单

- “Compaction 等于 retention delete”：前者按 key 去重，后者按时间/大小淘汰 segment。
- “Cleaner 只看当前 segment”：offset map 覆盖 dirty range，判断可能跨多个 segment。
- “tombstone 可以立即删除”：必须等 delete horizon，避免旧副本重新出现 value。
- “事务 marker 和普通消息一样清理”：marker 要结合事务 metadata 与 delete horizon。
- “cleaned 文件写完就算完成”：必须 flush 成功后再 replaceSegments。

## 8. 证据清单

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

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦 compact cleaner 主链，不展开完整 retention delete、RemoteLogStorage、Streams state store。
- 目标正文：8000~12000 字；核心拆解层覆盖 dirty 边界、offset map、segment 重写、tombstone/marker/producer 保留、原子替换。

## 10. 本轮重写主线

1. 从“为什么不能直接删旧消息”开场。
2. 否定按时间删、遇到旧值就删、原地重写三种方案。
3. 解释 LogCleanerThread 如何挑 filthiest log。
4. 解释 `Cleaner.doClean` 的 dirty bound 与 offset map。
5. 解释 `SkimpyOffsetMap` 与跨 segment latest key。
6. 解释 `cleanInto` 的 tombstone、marker、active producer 保留。
7. 解释 segment 分组、flush、replaceSegments 原子换段。
8. 收网：compaction 是“重写出安全的新日志”，不是“在旧日志上删几条记录”。