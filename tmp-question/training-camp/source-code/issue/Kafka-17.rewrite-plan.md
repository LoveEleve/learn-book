# Kafka-17 重写规划

> 题目：日志不能无限增长——Log retention、activeSegment 滚动与 segment 删除主链
> 状态：K-3 Log 存储域第 2 篇，按“retention vs activeSegment”展开
> 目标：解释 Kafka 日志如何在无限增长和有限磁盘之间平衡：`retention.ms` 与 `retention.bytes` 如何决定哪些 segment 可以删、`deleteOldSegments` 的三种删除条件（时间/大小/startOffset），以及 `maybeRoll` 在什么条件下把 activeSegment 切换为新的、`roll()` 在新 segment 创建时如何同步生产者状态快照。

## 1. 读者困惑

- `retention.ms` 真的是“消息保留 7 天”吗？一个 segment 里大部分消息都没过期，但最早的消息过期了，这个 segment 删不删？
- `retention.bytes` 达标后删的是最早的数据还是随机的？
- `deleteOldSegments` 为什么有三种删除条件，它们之间是什么关系？
- activeSegment 什么时候变成非活跃的？什么条件会触发 `roll`？
- 滚动新 segment 时为什么要拍 `ProducerStateManager` 快照？
- recovery-point-offset-checkpoint 和 log-start-offset-checkpoint 是干什么的？

## 2. 一句话顿悟

**Kafka 的日志保留不是按时间/大小“精确截止”，而是按 segment 粒度整段删除：`deleteOldSegments` 依次检查 retention 时间、retention 大小、logStartOffset 三条条件，满足就整段删；`maybeRoll` 在 segment 大小、时间、索引容量任一达标时切换新 activeSegment，并同步拍一次 ProducerStateManager 快照。**

## 3. 五要素卡片

### 读者问题

配置 `retention.ms=604800000`（7 天），如果最早 segment 里 80% 的消息在 7 天内，20% 超过 7 天，这个 segment 删不删？

### 入口

- `UnifiedLog.deleteOldSegments`：三种删除条件的入口
- `deleteRetentionMsBreachedSegments`：按时间删除
- `deleteRetentionSizeBreachedSegments`：按大小删除
- `deleteLogStartOffsetBreachedSegments`：按 logStartOffset 删除
- `maybeRoll`：检查是否需要滚动
- `LogSegment.shouldRoll`：滚动条件判断
- `LogManager`：管理 checkpoint 文件

### 状态核心

- `retentionMs` / `retentionSize` / `logStartOffset`
- `activeSegment` 与 `nonActiveSegments`
- `segment.largestTimestamp` / `segment.lastModified` / `segment.baseOffset`
- `recoveryPointCheckpoint` / `logStartOffsetCheckpoint`
- `ProducerStateManager.takeSnapshot`

### 失败路径

- 按消息粒度删除 → 索引与日志不匹配，碎片化
- 不检查 retention 条件 → 磁盘被填满
- 只检查时间/大小不检查 logStartOffset → 事务恢复所需数据被误删
- 滚动时不同步 ProducerStateManager 快照 → 崩溃恢复时 producer 状态丢失
- 索引已满还继续写 → 索引溢出，无法定位

### 连接点

- 前文 `Kafka-3`：LogSegment 四文件布局与索引结构，本篇的 segment 删除和滚动直接依赖索引。
- 前文 `Kafka-11`：事务 marker 与 producer 状态的保留依赖 ProducerStateManager 快照。
- 前文 `Kafka-12`：Log Compaction 是另一种清理策略，与 retention delete 共存。

## 4. 总图

```text
定时或触发
  → deleteOldSegments()
    → deleteRetentionMsBreachedSegments（按时间）
    → deleteRetentionSizeBreachedSegments（按大小）
    → deleteLogStartOffsetBreachedSegments（按 startOffset）

写入记录时
  → maybeRoll(messagesSize, appendInfo)
    → segment.shouldRoll() 检查
      → 大小 || 时间 || 索引容量 超过？
        → 是：roll() 新 segment + 拍 ProducerStateManager 快照
        → 否：继续写入当前 activeSegment
```

## 5. 关键边界

- 本篇不重复 LogSegment 四文件布局细节（Kafka-3 已讲），只讲删除与滚动时的生命周期。
- 不把 retention delete 与 Log Compaction（Kafka-12）混成同一策略：delete 按时间/大小整段删，compact 按 key 去重。
- 不把 `roll()` 写成“立刻删除旧 segment”：旧 segment 在 roll 之后进入非活跃状态，但文件直到 retention 条件满足才被删除。
- 不把 ProducerStateManager 快照细讲，只点明 roll 时同步拍一次。

## 6. 失败方案推演

1. **按消息粒度删除**：segment 内部分删除，索引与日志不匹配，导致读取错误。
2. **只按时间不按大小**：消息量小但 retention.bytes 很小，磁盘仍可能被撑爆。
3. **不检查 logStartOffset**：事务恢复所需的最早数据被误删。
4. **activeSegment 永不滚动**：单个 segment 无限增长，索引超出 Integer.MAX_VALUE 无法寻址。
5. **滚动时不拍快照**：崩溃后 producer 状态丢失，幂等/事务 producer 无法恢复。

## 7. 误解清单

- “retention.ms 精确到消息级别”：按 segment 整段删，segment 里最早消息过期就删整段。
- “retention.bytes 是最新 N 字节的保留”：删除最早 segment 直到总大小达标。
- “activeSegment 不会被删除”：activeSegment 永不参与删除，只有非活跃 segment 才可能被删。
- “roll 就是删除旧 segment”：roll 只是创建新 activeSegment，旧 segment 转为非活跃，等 retention 条件满足才删。
- “deleteOldSegments 只检查一种条件”：按时间、大小、startOffset 依次检查。

## 8. 证据清单

- `storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java:1894`：deleteOldSegments 入口，三种条件。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java:1908`：deleteRetentionMsBreachedSegments。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java:1946`：deleteRetentionSizeBreachedSegments。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java:1977`：deleteLogStartOffsetBreachedSegments。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java:2052`：maybeRoll。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java:2107`：roll() 创建新 segment 并拍快照。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/LogSegment.java`：shouldRoll 条件。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/LogManager.java:37`：checkpoint 文件。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦 retention delete 与 activeSegment 滚动，不展开 Log Compaction 与 tiered storage。
- 目标正文：6000~10000 字；核心拆解层覆盖三种删除条件、maybeRoll 条件、roll() 快照同步。

## 10. 本轮重写主线

1. 从“7 天保留到底删什么”开场。
2. 否定“按消息粒度删除”和“只检查一种条件”两种方案。
3. 解释 deleteOldSegments 的三种删除条件。
4. 解释 segment 的非活跃生命周期。
5. 解释 maybeRoll 的条件与 roll() 的快照。
6. 解释 checkpoint 文件的角色。
7. 收网：retention 按 segment 整段删，roll 按大小/时间/索引条件切换，roll 时同步拍快照。