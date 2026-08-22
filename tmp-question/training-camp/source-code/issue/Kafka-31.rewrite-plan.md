# Kafka-31 重写规划

> 题目：Cleaner 真正删什么、留什么——cleanInto、墓碑、事务 marker 与 crash-safe swap 主链
> 状态：K-11 Log Compaction 域第 3 篇，按"cleanInto / tombstone / marker / active producer 保留 + crash-safe swap"展开
> 目标：解释 Cleaner 在有了 latest offset map 之后，如何决定一个 batch / record 是否要保留：普通 key/value、tombstone、control batch、事务 marker、active producer 最后状态的保留规则；以及 `.cleaned → .swap → .deleted → 正式文件` 的 crash-safe 换段流程。 

## 1. 读者困惑

- buildOffsetMap 知道最新 offset 后，具体删什么、留什么？
- tombstone 为什么不能一写完就删？
- control batch / 事务 marker 为什么有时不能删？
- active producer 的最后一条 batch 为什么必须保留？
- `RETAIN_EMPTY` / `DELETE_EMPTY` / `DELETE` 三种 batch retention 有什么区别？
- `.cleaned → .swap → .deleted → 正式文件` 这套流程为什么是 crash-safe？

## 2. 一句话顿悟

**`cleanInto` 先按 batch 决定 retention，再按 record 判断具体去留：普通 value 只有是 key 的 latest offset 才保留；tombstone 要等 delete horizon；事务 marker 要结合 transaction metadata 与 delete horizon；active producer 的最后 batch 要即使空记录也保留（`RETAIN_EMPTY`）。保留结果先写进 `.cleaned` segment，flush 后再进入 `.cleaned → .swap → .deleted → 正式文件` 的 crash-safe 换段流程。**

## 3. 五要素卡片

### 读者问题

某个 key 的旧值能删，为什么 tombstone 不能删？active producer 最后的 marker 为什么即使没有数据也要保留？

### 入口

- `Cleaner.cleanInto`
- `checkBatchRetention` / `shouldRetainRecord`
- `shouldDiscardBatch`
- `lastRecordsOfActiveProducers`
- `CleanedTransactionMetadata`
- `LocalLog.replaceSegments`

### 状态核心

- `BatchRetention`：`RETAIN_EMPTY` / `DELETE_EMPTY` / `DELETE`
- `deleteHorizonMs`
- `isBatchLastRecordOfProducer`
- `lastRecordsOfActiveProducers`
- `.cleaned` / `.swap` / `.deleted` 文件后缀

### 失败路径

- 一见旧值就删 → tombstone / marker / producer 边界被误删
- marker 随便删 → `read_committed` 失去事务边界
- active producer 最后 batch 删除 → fencing / sequence 信息丢失
- 不 flush 就 swap → crash 后索引与日志半完成
- 原地覆盖旧 segment → 崩溃恢复无法判断哪份可信

### 连接点

- 前文 `Kafka-12`：compaction 总览。
- 前文 `Kafka-30`：latest offset map 构建完成后，本篇接“删什么、留什么”。
- 前文 `Kafka-11`：事务 marker / `read_committed` 可见性。

## 4. 总图

```text
cleanInto
  → checkBatchRetention(batch)
    → BatchRetention = RETAIN_EMPTY / DELETE_EMPTY / DELETE
      → shouldRetainRecord(record)
        → 普通 value / tombstone / control batch 各走各的规则
          → 保留结果写入 .cleaned segment
            → flush
              → replaceSegments(.cleaned → .swap → .deleted → 正式文件)
```

## 5. 关键边界

- 本篇不重复 offset map 构建（Kafka-30）。
- 不把 tombstone / marker / active producer 保留规则混成“最新值保留”。
- 不把 crash-safe swap 写成“原子 rename”。
- 不把 RETAIN_EMPTY 当作“保留空 batch 没意义”——它是在保留 producer/offset/marker 边界。

## 6. 失败方案推演

1. **latest offset 不是最后一条就删**：tombstone / marker / active producer 最后 batch 被误删。
2. **control batch 只看 latest key**：事务边界被破坏。
3. **active producer 最后状态不保留**：重启后 sequence/fencing 失去依据。
4. **cleaned segment 直接覆盖旧 segment**：崩溃时得到半写的日志/索引文件。

## 7. 误解清单

- “最新值保留”适用于一切记录：tombstone、marker、active producer batch 另有规则。
- “DELETE_EMPTY 代表整个 batch 都删”：是 batch 容器保留但内部 records 可能删空。
- “RETAIN_EMPTY 没有意义”：它保留 producer 状态或边界。
- “marker 只给 coordinator 看”：`read_committed` 读路径要依赖它。
- “replaceSegments 是一步 rename”：是多阶段 crash-safe 交换协议。

## 8. 证据清单

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

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦 cleanInto 保留规则与换段流程，不展开 SkimpyOffsetMap 构建。
- 目标正文：6000~10000 字。

## 10. 本轮重写主线

1. 从"知道 latest offset 后具体删什么"开场。
2. 否定“一见旧值就删”“原地覆盖旧 segment”两种方案。
3. 解释 BatchRetention 三态。
4. 解释 tombstone / marker / active producer 最后状态保留。
5. 解释 shouldRetainRecord 如何决策普通 value / tombstone / control batch。
6. 解释 crash-safe swap 流程。
7. 收网：compaction 真正难的不是找旧值，而是保住边界。