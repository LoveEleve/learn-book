# Kafka-18. Kafka 的索引为什么能“按需加载”——LazyIndex、OffsetIndex、TimeIndex 与 AbstractIndex 主链

> 场景：Kafka-3 讲过 LogSegment 的四文件布局，其中 `.index` 和 `.timeindex` 我们只是点到名字。本篇把它们彻底讲深：offset index 为什么用稀疏的 4 字节相对偏移、time index 怎么把时间戳桥接到 offset、以及 `LazyIndex` 为什么能让 broker 启动时不必把全部分区的索引都 mmap 进内存。这是 K-3 Log 存储域第 3 篇。

## 先把真正的困惑摆出来：为什么不能直接扫日志文件找消息

假设一个 segment 的 `.log` 文件有 1GB，里面几百万条消息。消费者想读一条 offset=50000 的消息，Kafka 总不能从文件开头逐条扫到 50000 吧？

扫描的代价是线性的：offset 越大，定位越慢。所以 Kafka 需要一个索引：把 offset 映射到它在 `.log` 文件里的物理字节位置。

但这里立刻引出一个矛盾：

- 索引越密，查找越快；
- 索引越密，文件也越大；
- 如果不加载索引，又怎么知道位置？

Kafka 的选择是：**用稀疏索引 + 二分查找 + 延迟 mmap 加载**，三件事一起解决。

```text
目标：offset=50000 → position=多少字节
  → OffsetIndex 二分查找最接近 50000 的索引项
    → 得到 (offset≈49999, position≈12345678)
      → 从 12345678 开始扫描几条就到 50000
```

*关键设计（斜体）：* *Kafka 的 offset index 用稀疏的 `(relativeOffset, position)` 对（各 4 字节，条目 8 字节），通过二分查找快速定位目标 offset 对应的物理位置；time index 用 `(timestamp, offset)` 对桥接时间戳到 offset。`LazyIndex` 把索引加载延迟到首次访问：broker 启动时只保存文件路径，首次 `get()` 才 mmap。*[模式: 稀疏索引 + 二分查找 + 延迟 mmap]

## 第一层：OffsetIndex 是稀疏的 (relativeOffset, position) 映射

`OffsetIndex` 的每条索引项是两个 4 字节整数：

- **relativeOffset**：相对偏移，实际 offset = relativeOffset + baseOffset；
- **position**：该 offset 在 `.log` 文件中的物理字节位置。

所以一个索引项是 8 字节，`ENTRY_SIZE = 8`（`OffsetIndex.java:56`）。

为什么用相对偏移而不是完整 offset？因为 segment 的 baseOffset 是固定的，segment 内的消息 offset 与 baseOffset 的差值通常远小于 Integer.MAX_VALUE。用 4 字节相对偏移，比 8 字节完整 offset 省一半空间。这就是 Kafka 把 segment 大小限制在 1GB、并要求索引还能用 4 字节相对偏移表达的原因。

更重要的是**稀疏**：不是每条消息都有索引项，而是每隔 `index.interval.bytes`（默认 4096 字节，即写入每条记录后按字节间隔触发）才写一条索引。也就是说，索引密度由写入字节数决定，不是消息条数。

```text
.log:  offset=0  记录……  offset=1000 记录……  offset=2000 记录……
.index: (rel=0, pos=0)  (rel=980, pos=8192)  (rel=1990, pos=16384)
```

查找时，`OffsetIndex.lookup(targetOffset)` 直接用完整 offset 二分查找——`parseEntry` 自动把磁盘上存储的相对偏移还原为完整 offset 进行比较（`AbstractIndex.java:546-550`），找到小于等于目标的最大索引项后，返回对应的物理位置。从那里开始顺序扫描少量消息即可命中目标。

## 第二层：二分查找保证查找在 O(log n)

`OffsetIndex.lookup(targetOffset)` 的核心是二分查找（`OffsetIndex.java:97`）。它的语义是：

```text
lookup(targetOffset)
  → 转成 relativeOffset
    → largestLowerBoundSlotFor(relativeOffset)
      → 找到小于等于目标的最后一个项
        → 返回 (offset, position)
```

配合稀疏索引，二分查找把"找某条消息的位置"从 O(n) 扫日志，变成 O(log n) 查索引 + O(稀疏间隔) 扫少量消息。

注意这个查找返回的是 `OffsetPosition`，即 `(offset, position)`，position 是 `.log` 文件中的字节偏移。消费者拿到 position 后，从那里开始读文件。

如果索引不稀疏（每条消息都索引），索引大小会接近数据文件大小，存储开销翻倍；如果索引完全不加载，则无法二分。所以稀疏 + 二分 + mmap 三者缺一不可。

## 第三层：TimeIndex 是 (timestamp, offset) 的桥接，不是直接定位

`.timeindex` 解决的问题是"按时间戳找 offset"。每条索引项是：

- **timestamp**：8 字节；
- **offset**：4 字节（相对偏移）。

所以 `TimeIndex.ENTRY_SIZE = 12`。

关键在于：**time index 查到的是 offset，不是 position。** 要拿到物理位置，还要再经过 offset index。它的 lookup 用 `largestLowerBoundSlotFor(idx, targetTimestamp, IndexSearchType.KEY)`（`TimeIndex.java:155`），即按 KEY（timestamp）二分查找 timestamp ≤ 目标的最大索引项，得到对应的 offset。

```text
按时间戳 T 查
  → TimeIndex 查到 offset ≈ O（相对偏移 + baseOffset）
    → OffsetIndex.lookup(O)
      → 得到 position
        → 精确定位
```

这就形成了两级索引：

- time index 负责 timestamp → offset；
- offset index 负责 offset → position；
- `.log` 负责从 position 读实际数据。

这也是 Kafka 运行时（consumer 按时间重置 offset、日志按时间清理）都能高效工作的原因。

## 第四层：LazyIndex 解决"数千个 segment 启动时全部 mmap"

真正的大规模问题在这里。假设一个 broker 有 5000 个 segment，每个 segment 有 `.index` 和 `.timeindex`。如果启动时全部 mmap，就是 10000 个 MappedByteBuffer 同时占内存，启动时间和内存压力都很大。

`LazyIndex` 的做法是双态：

- **IndexFile 态**：启动时只持有文件路径对象，不碰 mmap；
- **IndexValue 态**：首次 `get()` 时，才真正 `loadIndex(file)` 创建 OffsetIndex/TimeIndex，并 mmap。

```text
启动时
  → LazyIndex(IndexFile(file))   // 只存路径，内存开销极小

首次访问
  → get()
    → 进入锁
      → 仍是 IndexFile？
        → loadIndex(file) → IndexValue(index)
          → 之后都走 IndexValue
```

这就是 Kafka-3 里"LazyIndex 延迟加载"的真正含义：**不是省了 mmap，而是把 mmap 推迟到确实需要的时候。** 大多数 segment 在 broker 运行时其实很少被按 offset/time 精确查询，所以延迟加载能显著降低启动成本。

## 第五层：get() 的并发安全——ReentrantLock 保证只加载一次

`LazyIndex.get()` 不是无脑 load，它要保证并发下只加载一次：

```text
get()
  → 先读 volatile indexWrapper
    → 如果已经是 IndexValue，直接返回（无锁快路径）
    → 否则加锁
      → 再次检查（双检锁）
        → 仍是 IndexFile，则 loadIndex
          → 赋值 indexWrapper = IndexValue
```

这个"先快路径，再双检锁"的设计，让并发调用 `get()` 的多个线程不会重复创建多个 OffsetIndex 实例。

`LazyIndex` 的类注释把它说得很清楚：它封装 `IndexFile` 和 `IndexValue` 两个内部类，前者提供 updateParentDir / renameTo / deleteIfExists 等"不想加载索引也能操作文件"的方法，后者则委托给已加载的索引。

## 第六层：AbstractIndex 是 mmap + 锁的底座

`OffsetIndex` 和 `TimeIndex` 都继承 `AbstractIndex`，它负责：

- `createAndAssignMmap()`：用 `FileChannel.map()` 把索引文件映射到内存（`AbstractIndex.java:100`）；
- `maxEntries` / `entries`：记录索引容量和当前条目数；
- `lock`（`ReentrantLock`，独占）：序列化所有修改索引状态的操作（append/truncate）；
- `remapLock`（`ReentrantReadWriteLock`，读写锁）：协调 mmap 的并发读与 remap。

`AbstractIndex` 的类注释解释了并发模型：

- 读操作不需要 lock，因为 MappedByteBuffer 直接访问 OS 缓存，并发读在实践上是安全的；
- 只有修改内部状态的操作才需要 lock；
- remap（重建 mmap）通过 remapLock 协调，保证"边读边 remap"时读到的数据一致。

所以 mmap 不是"天然线程安全"的，Kafka 用 remapLock 把 remap 和读隔离，用 lock 把修改和读隔离，才能让每个 segment 的索引在并发访问下稳定工作。

## 收网：稀疏 + 二分 + 延迟加载 + mmap 锁

把整篇压成一句话：Kafka 的 offset index 用稀疏的 `(relativeOffset, position)` 对（8 字节）二分定位物理位置；time index 用 `(timestamp, offset)` 对把时间戳桥接到 offset，再经 offset index 定位 position。`LazyIndex` 把加载推迟到首次 `get()`：启动时只保存文件路径（IndexFile 态），首次访问才 mmap 成 OffsetIndex/TimeIndex（IndexValue 态），用双检锁保证只加载一次。`AbstractIndex` 用 lock + remapLock 控制 mmap 的并发读写。

```text
消费者的目标 offset
  → OffsetIndex.lookup（二分，稀疏 index）
    → position
      → 从 .log 该 position 扫读

按时间找
  → TimeIndex → offset
    → OffsetIndex → position
      → .log

加载时机
  → 启动：LazyIndex(IndexFile) 只存路径
    → 首次 get()：IndexValue + mmap（双检锁）
```

到这里，主线只发生了六件事。

第一，OffsetIndex 用稀疏的 (relativeOffset, position) 8 字节条目。

第二，查找用二分，O(log n) 定位到物理 position。

第三，TimeIndex 用 (timestamp, offset) 桥接，再经 OffsetIndex 定位。

第四，LazyIndex 用 IndexFile/IndexValue 双态延迟加载。

第五，get() 用双检锁保证只 mmap 一次。

第六，AbstractIndex 用 lock + remapLock 控制 mmap 并发。

**本篇的一句话困惑**：Kafka 为什么不能直接扫日志，又为什么不全量加载索引？

**本篇的一句话顿悟**：Kafka 用稀疏索引 + 二分查找把定位降到 O(log n)，用相对偏移省一半索引空间；LazyIndex 把 mmap 推迟到首次访问，让 broker 启动不必全量加载数千个 segment 的索引。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“offset index 存的是完整 offset。”** 存的是相对偏移，需要加 baseOffset 还原。
2. **“索引是密集的。”** 稀疏索引，按字节间隔写，不是每条消息一个索引项。
3. **“LazyIndex 就是 AbstractIndex。”** LazyIndex 是包装器，AbstractIndex 才是 mmap 结构。
4. **“time index 可以直接定位消息。”** 它定位到 offset，再经 offset index 定位到 position。
5. **“mmap 天然线程安全。”** AbstractIndex 用 remapLock/lock 协调并发读与 remap。

### 关键证据清单

- `storage/src/main/java/org/apache/kafka/storage/internals/log/OffsetIndex.java:29`：OffsetIndex 类注释，稀疏索引。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/OffsetIndex.java:56`：ENTRY_SIZE = 8。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/OffsetIndex.java:97`：lookup 二分查找。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/TimeIndex.java:153`：TimeIndex.lookup 按 timestamp 二分。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/AbstractIndex.java:42`：AbstractIndex 类注释。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/AbstractIndex.java:59`：lock / remapLock。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/AbstractIndex.java:100`：createAndAssignMmap。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/LazyIndex.java:29`：LazyIndex 类注释。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/LazyIndex.java:169`：get() 双检锁首次加载。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦索引结构、LazyIndex 延迟加载、mmap 并发控制，不展开按时间戳查找在 consumer 侧的完整路径。
- 不把 offset index 与 time index 的条目结构混成相同；它们条目大小与用途不同。
- 不把 LazyIndex 双态与 AbstractIndex 的 mmap 机制混成同一层。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-3`（LogSegment 四文件布局）、`Kafka-17`（segment 滚动）。
- 后续桥接：下一篇可进入 K-3 Log 存储域第 4 篇（ProducerStateManager snapshot 与幂等/事务状态恢复深讲）。