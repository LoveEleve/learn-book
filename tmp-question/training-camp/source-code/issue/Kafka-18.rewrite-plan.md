# Kafka-18 重写规划

> 题目：Kafka 的索引为什么能“按需加载”——LazyIndex、OffsetIndex、TimeIndex 与 AbstractIndex 主链
> 状态：K-3 Log 存储域第 3 篇，按“LazyIndex 与偏移量索引原理”展开
> 目标：解释 Kafka 的 offset 索引和 time 索引如何工作，以及 LazyIndex 为什么能在 broker 启动时延迟 mmap 索引文件，避免数千个 segment 同时加载的开销。核心覆盖 `OffsetIndex` 的稀疏索引结构（相对偏移 + 文件位置，二分查找）、`TimeIndex` 的 timestamp → offset 映射、`LazyIndex` 的 IndexFile/IndexValue 双态设计与 ReentrantLock 保护首次加载，以及 mmap 的读写锁并发控制。

## 1. 读者困惑

- 为什么一个 segment 有 `.index` 和 `.timeindex` 两个索引文件，它们存的到底是什么？
- 为什么 offset index 只用 4 字节存“相对偏移”，而不是 8 字节存完整 offset？
- 为什么说索引是稀疏的？密集索引会怎样？
- `LazyIndex` 是什么，为什么说它“延迟加载”？
- 为什么 broker 启动时不需要把全部分区的索引都 mmap 进内存？
- `AbstractIndex` 的读写锁为什么同时允许读和写？

## 2. 一句话顿悟

**Kafka 的 offset index 用稀疏的 `(relativeOffset, position)` 对（4+4 字节），通过二分查找快速定位目标 offset 在 `.log` 文件中的物理位置；time index 用 `(timestamp, offset)` 对（8+4 字节）做 timestamp → offset 的桥接。`LazyIndex` 在 broker 启动时只记录文件路径（`IndexFile` 态），首次 `get()` 时才 mmap 加载（`IndexValue` 态），锁保护竞争，避免数千 segment 同时全量 mmap。**

## 3. 五要素卡片

### 读者问题

一个 segment 的 `.index` 文件只有 10MB，不加载它怎么知道消息的物理位置？为什么不等需要的时候再加载？

### 入口

- `AbstractIndex`：mmap、maxEntries、entries、remapLock、lock
- `OffsetIndex`：`(relativeOffset 4B, position 4B)` → 8B 条目，二分查找
- `TimeIndex`：`(timestamp 8B, offset 4B)` → 12B 条目
- `LazyIndex`：IndexFile 态 → IndexValue 态，延迟加载
- `LogSegment`：通过 LazyIndex 持有 offsetIndex 与 timeIndex

### 状态核心

- `OffsetIndex.ENTRY_SIZE = 8`
- `TimeIndex.ENTRY_SIZE = 12`
- 相对偏移：`entry.offset = relativeOffset + baseOffset`
- 稀疏索引：不是每条消息一个索引
- `LazyIndex.IndexFile`：只持有文件路径，不 mmap
- `LazyIndex.IndexValue`：持有已加载的 AbstractIndex 实例
- `AbstractIndex.lock`：写/修改锁
- `AbstractIndex.remapLock`：读写锁，控制 mmap 的并发

### 失败路径

- 全量 mmap 所有 segment 索引 → 数千个 MappedByteBuffer，内存耗尽
- 密集索引 → 索引文件巨大，占磁盘空间
- 不检查相对偏移上限 → offset 差值超过 Integer.MAX_VALUE 时索引溢出
- 并发读写无锁 → 一个线程读索引时另一个线程 remap 出错

### 连接点

- 前文 `Kafka-3`：LogSegment 四文件布局，本篇深入索引文件。
- 前文 `Kafka-17`：segment 滚动时索引被截断/重置，roll 时 LazyIndex 重新创建。
- 后文：K-3 Log 存储域第 4 篇（ProducerStateManager snapshot 深讲）。

## 4. 总图

```text
LogSegment
  → offsetIndex: LazyIndex[OffsetIndex]
    → 启动时：IndexFile（只存文件路径）
      → 首次 get()：mmap 加载 → IndexValue
        → (relativeOffset, position) 稀疏索引
          → 二分查找定位 log 文件位置

  → timeIndex: LazyIndex[TimeIndex]
    → 启动时：IndexFile
      → 首次 get()：mmap 加载 → IndexValue
        → (timestamp, offset) 映射
          → 按时间戳找 offset
```

## 5. 关键边界

- 本篇不重复 LogSegment 四文件布局细节（Kafka-3），只深入索引。
- 不把 offset index 与 time index 混成相同结构：前者 8B 条目，后者 12B 条目，用途不同。
- 不把 LazyIndex 的延迟加载与 AbstractIndex 的 mmap 机制混成同一层。
- 不展开 txnindex（Kafka-3/Kafka-11 已讲）。

## 6. 失败方案推演

1. **全量 mmap 所有索引** → broker 启动时数千个 MappedByteBuffer 同时加载，内存压力大，启动慢。
2. **密集索引（每条消息一个索引）** → 索引文件大小与数据文件可比，浪费空间。
3. **不用相对偏移** → 每条索引 12 字节(offset 8B + position 4B)，浪费 50% 空间。
4. **无锁并发读写** → 线程 A 读索引时线程 B remap，读取到脏数据。

## 7. 误解清单

- “offset index 存的是完整 offset”：存的是相对偏移（relativeOffset），需要加 baseOffset 还原。
- “索引是密集的”：稀疏索引，不是每条消息都有索引项。
- “LazyIndex 就是 AbstractIndex”：LazyIndex 是包装器，AbstractIndex 才是 mmap 结构。
- “time index 可以直接定位消息”：time index 定位到 offset，再通过 offset index 定位到 position。
- “mmap 是线程安全的”：AbstractIndex 用 remapLock 保护并发 mmap 操作。

## 8. 证据清单

- `storage/src/main/java/org/apache/kafka/storage/internals/log/OffsetIndex.java:29`：OffsetIndex 类注释与稀疏索引。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/OffsetIndex.java:56`：ENTRY_SIZE = 8。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/OffsetIndex.java:97`：lookup 二分查找。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/TimeIndex.java`：TimeIndex 类。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/AbstractIndex.java:42`：AbstractIndex 类注释。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/AbstractIndex.java:59`：lock / remapLock。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/AbstractIndex.java:100`：createAndAssignMmap。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/LazyIndex.java:29`：LazyIndex 类注释。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/LazyIndex.java:169`：get() 首次加载。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦索引结构、LazyIndex 延迟加载、mmap 并发控制，不展开给定时间戳查找（Kafka-4/TimelineIndex 相关）。
- 目标正文：6000~10000 字。

## 10. 本轮重写主线

1. 从“为什么需要索引，为什么不能直接扫日志”开场。
2. 否定“全量 mmap 所有索引”和“密集索引”两种方案。
3. 解释 OffsetIndex 的稀疏结构与相对偏移。
4. 解释 TimeIndex 的 timestamp→offset 桥接。
5. 解释 LazyIndex 的 IndexFile/IndexValue 双态延迟加载。
6. 解释 AbstractIndex 的 mmap 与读写锁。
7. 收网：索引二分查找 + 延迟加载 + 稀疏格式。