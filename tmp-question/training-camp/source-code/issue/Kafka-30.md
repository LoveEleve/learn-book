# Kafka-30. Cleaner 怎么知道哪个 key 才是最新的——buildOffsetMap、SkimpyOffsetMap 与碰撞探测主链

> 场景：Kafka-12 已经讲过 compaction 不是“删旧消息”，而是先重写安全的新 segment。但 Cleaner 在真正重写之前，还得先回答一个关键问题：**某个 key 在当前 dirty range 里的最新 offset 到底是多少？** 这一层就是 `buildOffsetMap` 和 `SkimpyOffsetMap` 的作用。本篇把 compaction 的“索引构建阶段”单独讲透。

## 先把真正的困惑摆出来：遇到旧值时，Cleaner 怎么知道它是不是“旧”

假设 dirty range 里有这样几条记录：

```text
offset 10  key=A value=1
offset 35  key=B value=2
offset 80  key=A value=3
offset 120 key=A value=4
```

当 Cleaner 扫到 offset 10 时，它不知道 offset 80、120 还会不会出现同一个 key。所以它不能在看到 `key=A value=1` 的瞬间就决定删掉还是保留。这个问题只有在**看完整个本轮允许扫描的 dirty 范围，或者至少扫到 offset map 达到装载上限为止**之后才能回答。

这就是为什么 compaction 必须先有一份“全局视图”：我得先知道每个 key 在本轮 dirty range 里**目前可见的最后一次出现**在哪个 offset，后面重写 segment 时才能判断哪些是旧值。

```text
dirty range 扫描完之后
  → key=A latestOffset=120
  → key=B latestOffset=35
```

有了这份 latestOffset 视图，Cleaner 回头重写时才知道：

- offset 10 的 `A=1` 可以丢；
- offset 80 的 `A=3` 也可以丢；
- offset 120 的 `A=4` 必须保留。

*关键设计（斜体）：* *Cleaner 先通过 `buildOffsetMap` 扫描 dirty range，把每个 key 的“最新 offset”记录到 `SkimpyOffsetMap`。它不是保存完整 key，而是保存 `hash(key) → latestOffset`，通过 probing 解决 slot 冲突。map 达到本轮允许的装载上限后，Cleaner 会安全停下，保证后续 `cleanInto` 只在可靠的 latestOffset 视图上工作。*[模式: 先建全局 latest 视图，再回头重写]

## 第一层：`buildOffsetMap` 先决定本轮要看哪些 segment

`Cleaner.doClean()` 在真正 `cleanSegments` 之前，会先调用 `buildOffsetMap(log, start, end, map, stats)`。这里的 `start` / `end` 就是本轮 dirty range：

- `start = firstDirtyOffset`
- `end = firstUncleanableOffset`

`buildOffsetMap` 会：

1. `map.clear()`，准备新一轮扫描；
2. 拿到 dirty segments：`log.logSegments(start, end)`；
3. 预先算好每个 segment 的 `nextSegmentStartOffset`；
4. 逐 segment 调用 `buildOffsetMapForSegment(...)`；
5. 如果 map 满了（`full=true`），提前结束本轮扫描。

注意这一步不是“扫完整个日志”，而是只扫本轮 dirty range，而且 map 有容量上限，可能提前停。

## 第二层：为什么要从 `offsetIndex.lookup(startOffset).position` 起步

`buildOffsetMapForSegment` 进入 segment 之前，先执行：

```text
position = segment.offsetIndex().lookup(startOffset).position
```

这一步很关键：如果当前 segment 的 baseOffset 早于 dirty range 的 startOffset，Cleaner 不需要从 segment 文件的最开头读，而是先通过 offsetIndex 找到最接近 `startOffset` 的物理位置，从那里开始读 `.log` 文件。

也就是说：offset map 构建阶段自己就已经利用了 Log 存储层的 offsetIndex 优化，否则每一轮 compaction 都会从 segment 开头重扫一遍。

## 第三层：`buildOffsetMapForSegment` 的主循环——不是按消息，而是按 batch 扫

进入 segment 之后，Cleaner 不是一条消息一条消息随便读，而是：

```text
while (position < segment.log().sizeInBytes())
  → readInto(readBuffer, position)
    → MemoryRecords.readableRecords(readBuffer)
      → for (MutableRecordBatch batch : records.batches())
        → 事务/控制批次分支
          → streamingIterator 解 record
            → key.hasKey && offset >= startOffset ?
              → map.put(key, offset)
```

关键点：它先按 batch 读，再在 batch 内迭代 record。这样做有两个原因：

- Kafka 日志天然是 batch 结构，不按 batch 读反而麻烦；
- aborted transaction / control batch 的判断天然发生在 batch 边界上。

## 第四层：为什么 aborted transaction 的 batch 不参与 offset map

`buildOffsetMapForSegment` 在处理数据 batch 时，会先问 `CleanedTransactionMetadata.onBatchRead(batch)`：如果**当前这个 batch** 处于 aborted transaction 区间，就不把其中的 key 写进 offset map。

原因很简单：aborted transaction 的消息对 `read_committed` 是不可见的。既然不可见，它们就不应该参与“谁是最新值”的判断，否则：

- 某个 key 的最后一次出现明明是 abort 的消息；
- offset map 记下了这个 abort 里的 offset；
- 后续 `cleanInto` 看到更早的 committed 值，就会误以为“后面已经有更新值，可以删”。

这会直接破坏事务可见性。

所以 offset map 的语义不是“日志里最后一次出现”，而是“**本轮 dirty range 里、对 compaction 来说应当参与 latest-value 判断的最后一次出现**”。aborted transaction batch 被明确排除在外。

## 第五层：SkimpyOffsetMap——不是 key→offset，而是 hash(key)→offset

`SkimpyOffsetMap` 之所以叫 skimpy，就是因为它**省**：不保存完整 key，只保存 key 的 hash 和对应 offset。

它的物理结构是：

```text
[ hash(key) | offset ]  // 每个 slot 占 hashSize + 8 字节
```

所以它能用固定大小的 ByteBuffer 存储大量条目，而不用为每个 key 分配独立对象。

### slot 冲突 vs hash 碰撞

这类 hash 结构有两个容易混的概念：

- **slot 冲突**：两个不同 hash 最终落在同一个 slot 范围内，SkimpyOffsetMap 通过 probing 找下一位置解决；
- **hash 碰撞**：两个不同完整 key 算出来的 hash 完全一样。这种情况下，结构本身无法再区分，只能依赖 hash 算法碰撞概率足够低。

所以：

- probing 解决的是“放不下当前 slot”；
- 它解决不了“两个不同 key 恰好同 hash”。

Kafka 在这里接受的是一种工程权衡：用少量内存换高概率正确，而不是用完整 key 做绝对无损索引。

## 第六层：为什么 map 满了必须停，不能继续扫

`buildOffsetMapForSegment` 会计算一个本轮允许的最大装载量：

```text
maxDesiredMapSize = map.slots() * dupBufferLoadFactor
```

注意这里不是“直到 slots 全满才停”，而是达到一个负载因子上限（例如 0.9）就停。原因是：越接近满载，冲突和 probing 成本越高，性能急剧变差。

当 `map.size() >= maxDesiredMapSize` 时，方法直接返回 `true`，告诉上层：“我已经没法安全有效地再索引更多 key 了，这一轮到此为止。”

这个停下不是错误，而是一种**安全剪裁**：Cleaner 宁愿本轮少清一点，也不愿在一个过载的 offset map 上继续工作，导致错误或极慢的判断。

## 第七层：`latestOffset()` 与 `nextSegmentStartOffset` 的作用

每处理完一个 batch，Cleaner 都会调用：

```text
if (batch.lastOffset() >= startOffset)
  map.updateLatestOffset(batch.lastOffset())
```

这让 `map.latestOffset()` 始终记住本轮已经看见的最大 offset。随后 `buildOffsetMap` 会用：

```text
endOffset = map.latestOffset() + 1
```

确定后续 `cleanSegments` 的上界。

但如果 segment 中间存在 offset gap（例如某个 segment 末尾没有完整覆盖到 nextSegmentStartOffset），Cleaner 还会在 segment 扫描完后做一次：

```text
map.updateLatestOffset(nextSegmentStartOffset - 1L)
```

`latestOffset` 不是调试信息，而是本轮 map 已覆盖到哪里的关键指标。`buildOffsetMap` 最终用：

```text
endOffset = map.latestOffset() + 1
```

把后续 `cleanInto` 的 cleanable 上界钉在"本轮 offset map 已经可靠覆盖到的位置"。如果中间存在 offset gap，`nextSegmentStartOffset - 1` 的 fast-forward 不是凭空补出一条不存在的消息，而是把 latestOffset 逻辑上推进到“当前 segment 理论应覆盖到的末尾”，避免本轮 cleanable 上界因为 gap 被不必要地收缩。

## 收网：offset map 是 compaction 的“全局 latest 视图”

把整篇压成一句话：`Cleaner.buildOffsetMap` 会在 `[firstDirtyOffset, firstUncleanableOffset)` 范围内逐 segment 扫描日志，利用 offsetIndex 从 dirty 起点切入，跳过 aborted transaction batch，只把参与 latest-value 判断的 key 放进 `SkimpyOffsetMap`；map 以 `hash(key) → latestOffset` 的固定内存结构保存视图，slot 冲突用 probing 解决，装载到 `slots * loadFactor` 即提前停；`latestOffset` 和 `nextSegmentStartOffset` 则确定本轮真正可清理的上界。

```text
buildOffsetMap
  → dirty range
    → buildOffsetMapForSegment
      → batch 级遍历
        → 跳过 aborted / control
          → map.put(hash(key), offset)
            → latestOffset 更新
              → map 满？本轮停止
```

到这里，主线只发生了六件事。

第一，offset map 构建先于任何 segment 重写。

第二，扫描从 offsetIndex.lookup(startOffset).position 切入，不从文件开头重扫。

第三，扫描按 batch 进行，而不是按消息乱扫。

第四，aborted transaction batch 不参与 latest-value 判断。

第五，SkimpyOffsetMap 用 hash+probing 换固定内存，slot 冲突与 hash 碰撞不是一回事。

第六，map 满了要停，latestOffset / nextSegmentStartOffset 决定本轮 cleanable 上界。

**本篇的一句话困惑**：Cleaner 到底怎么知道某个 key 在本轮 dirty range 里哪个 offset 才是最新值？

**本篇的一句话顿悟**：它先扫完整个 dirty range，把每个 key 的“最后一次可见出现”记进 SkimpyOffsetMap，再回头重写 segment；没有这份全局 latest 视图，任何“看到旧值就删”的方案都会误删。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“SkimpyOffsetMap 能完整区分所有 key。”** 它只存 hash，不存完整 key。
2. **“slot 冲突就是 hash 碰撞。”** slot 冲突可 probing 解决，真正相同 hash 的不同 key 结构本身无法区分。
3. **“buildOffsetMap 会把 dirty range 全扫完。”** map 满了就提前停。
4. **“latestOffset 只是调试信息。”** 它决定本轮 cleanable endOffset。
5. **“aborted transaction 也要参与 latest key 判断。”** 不可见事务 batch 会被跳过，不进入 map。

### 关键证据清单

- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:635`：buildOffsetMap。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:697`：buildOffsetMapForSegment。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:705`：从 offsetIndex.lookup(startOffset) 起步。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:706`：maxDesiredMapSize = slots * dupBufferLoadFactor。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:726`：aborted transaction batch 跳过索引。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:735`：map 满则停止。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:747`：latestOffset 更新。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:760`：nextSegmentStartOffset - 1 fast forward。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/SkimpyOffsetMap.java:29`：hash table 设计注释。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/SkimpyOffsetMap.java:107`：get()。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/SkimpyOffsetMap.java:134`：put()。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/SkimpyOffsetMap.java:203`：collisionRate。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦 offset map 构建，不展开 `cleanInto` 的 tombstone / marker / producer 保留细节。
- 不把 slot 冲突与 hash 碰撞混成同一个层次。
- `nextSegmentStartOffset - 1` 的 fast-forward 只说明逻辑上补齐 offset gap，不是实际存在一条消息。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-12`（compaction 总览）、`Kafka-18`（索引结构）、`Kafka-11`（事务 marker 与 aborted batch）。
- 后续桥接：下一篇可进入 K-11 第 3 篇（cleanInto / tombstone / marker / active producer 保留 + crash-safe swap），或继续其他未拆分域。