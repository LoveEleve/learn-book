# Kafka-30 重写规划

> 题目：Cleaner 怎么知道哪个 key 才是最新的——buildOffsetMap、SkimpyOffsetMap 与碰撞探测主链
> 状态：K-11 Log Compaction 域第 2 篇，按"offset map 构建"展开
> 目标：解释 Cleaner 在真正重写 segment 之前，如何建立一份 dirty range 的 `keyHash → latestOffset` 视图。重点覆盖 `Cleaner.buildOffsetMap` / `buildOffsetMapForSegment` 的扫描顺序、为什么需要 `nextSegmentStartOffset` 与 `latestOffset`、`SkimpyOffsetMap` 的哈希/探测/碰撞语义，以及为什么 map 满了要中止本轮扫描。 

## 1. 读者困惑

- Cleaner 在删旧值之前，怎么知道某个 key 的最新 offset 是多少？
- 为什么要先构造 offset map，再回头重写 segment？
- `SkimpyOffsetMap` 为什么叫 “skimpy”，它省掉了什么？
- key 碰撞怎么办？不同 key 碰到同一个 hash 会不会误删？
- map 满了为什么要停，不能继续扫吗？
- `latestOffset()` 和 `nextSegmentStartOffset` 在清理范围里起什么作用？

## 2. 一句话顿悟

**Cleaner 在重写任何 segment 之前，先用 `buildOffsetMap` 扫描 dirty range，生成一份“每个 key 在本轮范围里的最新 offset”视图。`SkimpyOffsetMap` 用固定内存保存 `hash(key) → latestOffset`，通过 probing 解决 slot 冲突；一旦 map 达到本轮允许的负载上限，就停止扫描，把这份不完整但安全的视图交给后续 `cleanInto` 使用。**

## 3. 五要素卡片

### 读者问题

一条 key 在 segment-1、segment-2、segment-3 都出现了，Cleaner 扫 segment-1 时怎么知道它是不是旧值？

### 入口

- `Cleaner.buildOffsetMap`
- `Cleaner.buildOffsetMapForSegment`
- `SkimpyOffsetMap.put/get/latestOffset`
- `dupBufferLoadFactor`
- `nextSegmentStartOffset`
- `CleanedTransactionMetadata`

### 状态核心

- dirty range: `[firstDirtyOffset, firstUncleanableOffset)`
- map.slots * dupBufferLoadFactor：本轮允许的最大装载量
- keyHash → latestOffset
- latestOffset：本轮 dirty range 已知的最大 offset
- aborted transaction metadata：控制是否索引某个 batch

### 失败路径

- 不先建 offset map → 看到旧值时不知道后面有没有更新
- map 无限增长 → 内存被 compaction 吃光
- 遇到 aborted batch 也索引 → 把根本不应可见的记录当成最新值
- nextSegmentStartOffset 不更新 → 最后一段 offset gap 被错误遗漏

### 连接点

- 前文 `Kafka-12`：compaction 总览，本篇是 offset map 细讲。
- 前文 `Kafka-18`：稀疏索引与哈希结构的思想与这里的 `SkimpyOffsetMap` 有相似点。
- 后文：K-11 第 3 篇（cleanInto / tombstone / marker / active producer 保留 + crash-safe swap）。

## 4. 总图

```text
Cleaner.doClean
  → buildOffsetMap(log, firstDirtyOffset, firstUncleanableOffset)
    → 遍历 dirty segments
      → buildOffsetMapForSegment(segment)
        → 跳过 aborted/control 不可见记录
          → key.hasKey && offset >= startOffset ?
            → map.put(hash(key), offset)
              → latestOffset 更新
                → map 达到 maxDesiredMapSize ? 停止本轮扫描
```

## 5. 关键边界

- 本篇只讲 offset map 构建，不展开后续 `cleanInto` 保留规则。
- 不把 `SkimpyOffsetMap` 写成完整 key→offset 字典：它只保存 hash，不保存原始 key。
- 不把 slot 冲突（probing 解决）和 hash 碰撞（不同 key 同 hash）混成同一件事。
- 不把 map 满了理解成“错误”：它是本轮扫描的安全截止条件。

## 6. 失败方案推演

1. **看到旧值就直接删除**：后面若有新值，没问题；若没有，则误删最新值。
2. **全量保存完整 key**：内存与对象开销巨大，Cleaner 无法长期运行。
3. **map 满了还继续扫**：dedupe 视图不可靠，后续 `cleanInto` 可能误删。 
4. **aborted batch 也建索引**：用户不可见的数据参与 latest value 判断，结果错误。

## 7. 误解清单

- “SkimpyOffsetMap 能完整区分所有 key。”：它只存 hash，不存完整 key。
- “slot 冲突就是 hash 碰撞。”：slot 冲突可 probing 解决，真正相同 hash 的不同 key 结构本身无法区分。
- “buildOffsetMap 会把 dirty range 全扫完。”：map 满了就提前停。
- “latestOffset 只是调试信息。”：它决定本轮 cleanable endOffset。
- “aborted transaction 也要参与 latest key 判断。”：不可见事务 batch 会被跳过，不进入 map。

## 8. 证据清单

- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:635`：buildOffsetMap。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:697`：buildOffsetMapForSegment。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:705`：从 offsetIndex.lookup(startOffset) 起步。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:706`：maxDesiredMapSize = slots * dupBufferLoadFactor。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:726`：aborted transaction batch 跳过索引。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:735`：map 满则停止。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:747`：latestOffset 更新。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/Cleaner.java:760`：nextSegmentStartOffset - 1 fast forward。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/SkimpyOffsetMap.java:29`：hash table 设计注释。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/SkimpyOffsetMap.java:107`：get()
- `storage/src/main/java/org/apache/kafka/storage/internals/log/SkimpyOffsetMap.java:134`：put()
- `storage/src/main/java/org/apache/kafka/storage/internals/log/SkimpyOffsetMap.java:203`：collisionRate。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦 offset map 构建，不展开 cleanInto 的 tombstone/marker/producer 保留细节。
- 目标正文：6000~10000 字。

## 10. 本轮重写主线

1. 从"Cleaner 怎么知道哪个 key 是最新值"开场。
2. 否定即见即删、全量保存完整 key、map 满了继续扫三种方案。
3. 解释 buildOffsetMap 如何组织 segment 扫描。
4. 解释 buildOffsetMapForSegment 的扫描逻辑。
5. 解释 SkimpyOffsetMap 的 hash / probing / collision 差异。
6. 解释 latestOffset 和 nextSegmentStartOffset 的作用。
7. 收网：offset map 是 compaction 的“全局 latest 视图”。