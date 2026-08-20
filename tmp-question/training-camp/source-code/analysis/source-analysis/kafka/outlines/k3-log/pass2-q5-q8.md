# K-3 闭环笔记 Q5-Q8: 恢复/截断/ProducerState/读路径

## Q5: 崩溃恢复 — LogLoader 加载与回放

假设: 启动时扫描段文件, 干净关停跳过恢复, 否则只恢复 recoveryPoint 之后的段, 索引损坏重建。

验证过程:
- loadSegmentFiles (LogLoader.java:357-391): 孤儿索引文件删除 (LogLoader.java:364-371, 无对应 .log) → .log 文件 → LogSegment.open (LogLoader.java:376) → sanityCheck (LogLoader.java:378): 缺索引/索引损坏 → recoverSegment (LogLoader.java:383-387)
- recoverSegment (LogLoader.java:400-420): 重建 ProducerStateManager (LogLoader.java:401-406) → UnifiedLog.rebuildProducerState (LogLoader.java:407-414) → segment.recover (LogLoader.java:415) → **takeSnapshot** (LogLoader.java:418, 每段恢复后立即快照)
- recoverLog (LogLoader.java:464+): hadCleanShutdown 为真跳过恢复 (LogLoader.java:466) — 干净关停标记 (kafka_cleanshutdown)
- segment.recover (LogSegment.java:478-524): 三索引 reset (LogSegment.java:479-481) → 逐 batch ensureValid + maxTimestamp + 按 indexIntervalBytes 重建索引 (LogSegment.java:496-500) + leaderEpochCache + updateProducerState (LogSegment.java:503-508) → 损坏处截断: log.truncateTo(validBytes) (LogSegment.java:518) + 索引 trim (LogSegment.java:519-522)
- **刷盘机制 (恢复的前提)**: LogSegment.flush (LogSegment.java:624-645) 四文件全刷 (log/offsetIndex/timeIndex/txnIndex); LocalLog recoveryPoint 语义 (LocalLog.java:101: "first offset which has not been flushed to disk") + unflushedMessages (LocalLog.java:251); 双配置: LOG_FLUSH_INTERVAL_MESSAGES (LogConfig.java:166) / LOG_FLUSH_INTERVAL_MS (LogConfig.java:169) + topic 级 FLUSH_MESSAGES_INTERVAL (LogConfig.java:194) / FLUSH_MS (LogConfig.java:196) — 对照设计文档 M 消息/S 秒 (docs/implementation/log.md)

代码类型: Implementation (崩溃恢复)

结论: **恢复 = 扫描段目录 (孤儿索引清理 LogLoader.java:364-371) + 干净关停跳过 (LogLoader.java:466) + 损坏段重建 (recoverSegment LogLoader.java:400-420: 索引重建 + 坏数据截断 LogSegment.java:518); 每段恢复后立即 producer snapshot (LogLoader.java:418)**。LogLoader.java:357-420,464-466 + LogSegment.java:478-524

## Q6: 截断语义 — truncateTo 与 HW

假设: truncateTo(targetOffset) 删除 baseOffset > target 的段, 活动段内部截断, LEO 重置。

验证过程:
- LocalLog.truncateTo (LocalLog.java:680-686): segments.filter(baseOffset > targetOffset) 删除 (LocalLog.java:681-682) → activeSegment.truncateTo(targetOffset) (LocalLog.java:683) → updateLogEndOffset(targetOffset) (LocalLog.java:684)
- LogSegment.truncateTo (LogSegment.java:557): 段内文件截断 + 索引截断 (offsetIndex/timeIndex/txnIndex 按 targetOffset 截断)
- truncateFullyAndStartAt (LocalLog.java:654-672): 全删 → 先建新段再删最后旧段 (LocalLog.java:663-666, 防活动段缺失) → updateLEO(newOffset) (LocalLog.java:668)
- HW 保护: 删除时 highWatermark >= upperBoundOffset 才删 (UnifiedLog.java:1826) — 日志起点不超过 HW (K-4 衔接)

代码类型: Implementation (截断)

结论: **truncateTo = 段级删除 (baseOffset > target, LocalLog.java:681) + 段内截断 (LocalLog.java:683) + LEO 重置 (LocalLog.java:684); truncateFullyAndStartAt 先建新段防缺口 (LocalLog.java:663-666); HW 以下段不可删 (UnifiedLog.java:1826)**。LocalLog.java:654-686 + LogSegment.java:557

## Q7: ProducerStateManager — 幂等/事务状态 (K-11 基础)

假设: 维护 producerId → (epoch, seq, txn) 状态, snapshot 文件持久化, 崩溃从 snapshot + 回放重建。

验证过程:
- 状态容器 (ProducerStateManager.java:85-93): producers map (ProducerStateManager.java:85, producerId→ProducerStateEntry) / ongoingTxns (ProducerStateManager.java:90, 进行中事务按 firstOffset 排序) / unreplicatedTxns (ProducerStateManager.java:93, 已完成未复制)
- takeSnapshot (ProducerStateManager.java:428-455): lastMapOffset > lastSnapOffset 才写 (ProducerStateManager.java:432) → writeSnapshot (ProducerStateManager.java:438) → 更新 lastSnapOffset (ProducerStateManager.java:451)
- loadFromSnapshot (ProducerStateManager.java:296): 启动加载最近 snapshot → 之后从 snapshot offset 回放重建
- 构造时 loadSnapshots (ProducerStateManager.java:117) + LogLoader recoverSegment 每段 takeSnapshot (LogLoader.java:418)
- 过期清理: 空闲 > producerIdExpirationMs 移除 (K-11 展开)

代码类型: Implementation (状态管理)

结论: **ProducerStateManager = producers/ongoingTxns/unreplicatedTxns 三容器 (ProducerStateManager.java:85-93) + snapshot 持久化 (takeSnapshot ProducerStateManager.java:428-455, 只有新 offset 才写 ProducerStateManager.java:432); 崩溃从 snapshot+回放重建 (ProducerStateManager.java:296); 每段恢复后立即快照 (LogLoader.java:418) — K-11 幂等/事务的 broker 侧基础**。ProducerStateManager.java:85-93,296,428-455

## Q8: 读路径 — translateOffset + 零拷贝 slice

假设: 读 = OffsetIndex 二分定位 position → FileRecords.slice 零拷贝, minOneMessage 保证至少一条。

验证过程:
- LogSegment.read (LogSegment.java:431-459): translateOffset(startOffset) (LogSegment.java:435, 索引二分) → null 表示超段尾 (LogSegment.java:438-439) → LogOffsetMetadata (LogSegment.java:442) → **minOneMessage: adjustedMaxSize = max(maxSize, startOffsetAndSize.size) (LogSegment.java:445-446)** → fetchSize = min(maxPosition - startPosition, adjustedMaxSize) (LogSegment.java:455) → log.slice(startPosition, fetchSize) (LogSegment.java:457) — **零拷贝**
- 方法注释 thread-safe (LogSegment.java:421) — 段读并发安全
- LocalLog.read (LocalLog.java:462) → 段查找 + 跨段聚合 (K-12 FetchSession 衔接)
- addAbortedTransactions (LocalLog.java:531-550): 事务读需要 aborted 列表 (read_committed, K-11 衔接)

代码类型: Algorithmic (读路径)

结论: **读 = 索引二分定位 (translateOffset LogSegment.java:435) + FileRecords.slice 零拷贝 (LogSegment.java:457) + minOneMessage 保证 (LogSegment.java:445-446); 段读线程安全 (LogSegment.java:421); 事务隔离需 txnindex 补 aborted 列表 (LocalLog.java:531-550, K-11)**。LogSegment.java:431-459 + LocalLog.java:462,531-550

跨域关联: K-12 FetchSession (读路径消费) / K-11 事务 (aborted txns) / K-4 ISR (HW 保护截断)
