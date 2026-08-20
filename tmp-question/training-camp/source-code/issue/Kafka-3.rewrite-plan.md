# Kafka-3 重写规划

> 题目：Kafka 为什么不是“再存一次消息”——分区日志、LogSegment 与索引主链
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：解释 Kafka Broker 怎样把 Producer batch 变成分区 append-only log，并把 `.log`、offset index、time index、transaction index、ProducerStateManager 组织成一条可追加、可定位、可恢复的存储主链。

## 1. 读者困惑

- Kafka 消息写入 Broker 后，为什么不是直接放进一个“大消息队列”？
- 一个 partition 的日志为什么要切成多个 `LogSegment`？
- `.log`、`.index`、`.timeindex`、`.txnindex` 分别解决什么问题？
- offset index 为什么是稀疏索引，Consumer 又怎样从 offset 找到物理位置？
- ProducerStateManager 为什么也属于日志存储，而不只是事务/幂等附属组件？

## 2. 一句话顿悟

**Kafka 的分区日志不是一份消息数组，而是由 LogSegment 承载的“数据文件 + 多种索引 + Producer 状态”组合：`.log` 保存顺序事实，稀疏 index 把逻辑 offset 定位到物理位置，txn index 与 ProducerStateManager 保留事务/幂等恢复所需状态，最终让日志既能追加也能被消费、复制和恢复。**

## 3. 五要素卡片

### 读者问题

Kafka 怎样把一条 partition 的顺序消息变成可追加、可查找、可滚动、可恢复的文件日志？

### 入口

- `UnifiedLog.append(...)`：分区日志追加入口
- `LocalLog` / `LogSegment`：文件段与追加承载
- `OffsetIndex` / `TimeIndex` / `LazyIndex`：逻辑位置到物理位置的定位桥
- `ProducerStateManager`：producerId/epoch/seq 与事务状态恢复

### 状态核心

- active segment / baseOffset / log end offset
- `.log`、`.index`、`.timeindex`、`.txnindex`
- relative offset / sparse index / physical position
- segment roll：大小、时间、index full、relative offset 能力
- ProducerStateEntry、snapshot、ongoing/unreplicated transaction

### 失败路径

- 只有 `.log` 没有索引：按 offset 读取要全量扫描
- 每条消息都写完整索引：索引体积和写放大失控
- Segment 无边界增长：清理、恢复、截断和定位都难以管理
- 只恢复消息 bytes 不恢复 Producer 状态：幂等去重和事务边界会失真
- index/segment 损坏或超出 relative offset：需要 roll、恢复或截断

### 连接点

- 前文 `Kafka-1/2`：Producer batch 最终进入 partition log
- 后文 `Kafka-4`：Consumer fetch 通过 offset 读取分区日志
- 后文 `Kafka-9`：ISR/Follower 复制依赖同一份 log 与 offset 边界
- 后文 `Kafka-11/12`：事务标记、幂等和 compaction 继续消费 LogSegment/ProducerStateManager

## 4. 总图

```text
Produce batch
  → Partition / UnifiedLog.append()
    → active LogSegment.append()
      → .log 顺序写入
      → offset/time/txn index 更新
      → ProducerStateManager 更新 producer 状态
        → segment roll / log end offset 推进
          → Consumer/Follower 按 offset 定位读取
```

## 5. 关键边界

- 本篇只讲单分区日志存储主链，不展开 ISR 副本协议和 Controller。
- 不把四类文件写成四份独立消息；只有 `.log` 是消息数据事实，其余是定位/事务辅助结构。
- 不把 offset index 写成全量索引；Kafka 使用稀疏索引再做局部扫描。
- 不把 ProducerStateManager 写成只服务事务，它同时承载幂等 producer 状态与恢复信息。

## 6. 失败方案推演

1. **只保存 `.log`**：读取 offset 需要全量扫描，恢复和查询成本失控。
2. **每条消息都建立完整 offset 索引**：索引写放大与内存/磁盘占用过高。
3. **一个 partition 永不 roll segment**：retention、compaction、恢复、截断都失去清晰边界。
4. **恢复只回放消息，不恢复 producer state**：重复 seq、epoch 回退和事务状态会判断错误。

## 7. 误解清单

- LogSegment 不是“一个消息”，而是一个分区日志文件段与索引集合。
- `.index` 不直接存消息，它把相对 offset 近似映射到物理位置。
- `LazyIndex` 延迟加载不等于没有索引，而是避免启动时全部 mmap。
- `.txnindex` 与 ProducerStateManager 的职责不同，一个偏事务标记定位，一个偏 producer/事务状态。
- segment roll 不是单纯按文件大小，还会受时间、索引容量和 offset 表达能力影响。

## 8. 证据清单

- `storage/src/main/java/org/apache/kafka/storage/internals/log/LogSegment.java:79`
- `storage/src/main/java/org/apache/kafka/storage/internals/log/LogSegment.java:167`
- `storage/src/main/java/org/apache/kafka/storage/internals/log/LogSegment.java:348`
- `storage/src/main/java/org/apache/kafka/storage/internals/log/OffsetIndex.java:143`
- `storage/src/main/java/org/apache/kafka/storage/internals/log/LazyIndex.java:1`
- `storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java:1065`
- `storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java:1198`
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:1`

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦单分区日志与索引，不展开 KRaft 元数据日志和 ISR 复制协议。
- 目标正文：9000~13000 字；核心拆解层覆盖 segment 文件布局、追加、稀疏索引、roll、producer state 与恢复边界。

## 10. 本轮重写主线

1. 从“Broker 为什么不能只存一份消息 bytes”开场。
2. 解释 partition log 为什么要分段，以及 active segment 如何承接追加。
3. 解释 `.log`、offset/time/txn index 的职责分离。
4. 解释稀疏索引 + 局部扫描如何完成 offset 定位。
5. 解释 ProducerStateManager 为什么必须跟随日志追加与恢复。
6. 收网：Kafka LogSegment 是顺序事实、定位结构和恢复状态的组合，而不是一个大文件数组。