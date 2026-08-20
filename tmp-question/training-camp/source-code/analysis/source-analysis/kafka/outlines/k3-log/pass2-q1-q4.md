# K-3 闭环笔记 Q1-Q4: 分段结构/append/稀疏索引/roll

## Q1: LogSegment 4 文件怎么协作?

假设: 单段 = .log 数据 + .index (offset→position) + .timeindex (timestamp→offset) + .txnindex (事务标记), 由 LogSegments 容器管理。

验证过程:
- Read LogSegment 字段 (LogSegment.java:79-84): FileRecords log (LogSegment.java:79) / LazyIndex<OffsetIndex> lazyOffsetIndex (LogSegment.java:80) / LazyIndex<TimeIndex> lazyTimeIndex (LogSegment.java:81) / TransactionIndex txnIndex (LogSegment.java:82) / baseOffset (LogSegment.java:83) / indexIntervalBytes (LogSegment.java:84)
- 类注释 (LogSegment.java:55-64): base offset <= 段内最小 offset, 段文件命名 = baseOffset.index/.log (LogSegment.java:61)
- 容器: LocalLog 持有 LogSegments (LocalLog.java:80) — append/roll/truncate 全部通过 segments 委托
- 段内字节计数: bytesSinceLastIndexEntry (LogSegment.java:102) — 稀疏索引间隔状态

代码类型: Implementation (存储结构)

结论: **LogSegment 四件套: .log (FileRecords) + .index (LazyIndex<OffsetIndex>) + .timeindex (LazyIndex<TimeIndex>) + .txnindex, baseOffset 命名 (LogSegment.java:79-84), LogSegments 容器统一管理 (LocalLog.java:80)**。LogSegment.java:55-64,79-84

## Q2: append 写路径 — 数据/索引/LEO 顺序

假设: 先写数据文件, 再按 indexIntervalBytes 间隔更新索引, 最后推进 LEO。

验证过程:
- Read LogSegment.append (LogSegment.java:250-280): physicalPosition = log.sizeInBytes() (LogSegment.java:255) → ensureOffsetInRange (LogSegment.java:257, 相对 offset 溢出检查) → log.append(records) (LogSegment.java:260) → 逐 batch: maxTimestamp 更新 (LogSegment.java:266-268) + **bytesSinceLastIndexEntry > indexIntervalBytes → offsetIndex().append + timeIndex().maybeAppend (LogSegment.java:270-274) → 计数清零** → bytesSinceLastIndexEntry += batch.sizeInBytes (LogSegment.java:277)
- LocalLog.append (LocalLog.java:526-529): activeSegment.append (LocalLog.java:527) + updateLogEndOffset(lastOffset + 1) (LocalLog.java:528) — LEO 推进
- 调用链: ReplicaManager.appendRecords → Partition.append → Log.appendAsLeader → LocalLog.append (K-4 衔接)

代码类型: Implementation (写路径)

结论: **写路径 = 数据先行 (log.append LogSegment.java:260) → 稀疏索引按 indexIntervalBytes 间隔写入 (LogSegment.java:270-274, 默认 4096 字节 ServerLogConfigs.java:81) → LEO 推进 (LocalLog.java:528); 索引不是逐条, 是"隔 N 字节记一条"**。LogSegment.java:250-280 + LocalLog.java:526-529

## Q3: 稀疏索引 — OffsetIndex 二分 + LazyIndex 延迟加载

假设: OffsetIndex 是 mmap 的稀疏索引 (offset→position), lookup 用二分; LazyIndex 延迟到首次 get() 才 mmap。

验证过程:
- OffsetIndex.lookup (OffsetIndex.java:97-107): mmap().duplicate() (OffsetIndex.java:101) → largestLowerBoundSlotFor 二分 (OffsetIndex.java:103) → slot==-1 返回 (baseOffset,0) (OffsetIndex.java:105) → parseEntry (OffsetIndex.java:107)
- LazyIndex 设计 (LazyIndex.java:28-46 javadoc): "defer loading (memory mapping) the underlying index until it is accessed for the first time via the get method" — 大量段启动时不用全部 mmap (LazyIndex.java:40-42)
- 索引满: offsetIndex().isFull (shouldRoll 条件之一, LogSegment.java:172) — 索引上限 10MB (ServerLogConfigs.java:77), 对应 10MB/8B×4096 ≈ 5.2GB 数据, 正常 1GB 段先触大小限制
- 相对偏移: canConvertToRelativeOffset — 索引存相对 baseOffset 的 int 偏移

代码类型: Algorithmic (二分索引)

结论: **OffsetIndex = mmap 稀疏索引 + largestLowerBoundSlotFor 二分查找 (OffsetIndex.java:97-107); LazyIndex 首次 get() 才 mmap (LazyIndex.java:28-46) — 数千段启动免全量映射, 索引满触发滚段 (LogSegment.java:172)**。OffsetIndex.java:97-107 + LazyIndex.java:28-46

## Q4: roll 轮转时机 — 大小/时间/索引满三条件

假设: 段达到 segment.bytes (1GB) 或时间阈值或索引满 → 封存旧段建新段。

验证过程:
- shouldRoll (LogSegment.java:167-173): size > maxSegmentBytes - messagesSize (LogSegment.java:170) || (size>0 && reachedRollMs) (LogSegment.java:171) || offsetIndex().isFull() || timeIndex().isFull() || 相对 offset 溢出 (LogSegment.java:172)
- 默认值: DEFAULT_SEGMENT_BYTES = 1024*1024*1024 = **1GB** (LogConfig.java:125); LOG_INDEX_INTERVAL_BYTES_DEFAULT = 4096 (ServerLogConfigs.java:81)
- roll 执行 (LocalLog.java:581-646): newOffset = max(expectedNextOffset, logEndOffset) (LocalLog.java:587) → 旧段 onBecomeInactiveSegment (LocalLog.java:627-629) → LogSegment.open 建新段 (LocalLog.java:631) + segments.add (LocalLog.java:637)
- 时间条件: reachedRollMs = timeWaitedForRoll > maxSegmentMs - rollJitterMs (LogSegment.java:168)

代码类型: Implementation (轮转决策)

结论: **roll 触发 = 大小超限 (1GB, LogConfig.java:125) / 时间到 (maxSegmentMs-rollJitterMs, LogSegment.java:168) / 索引满 (LogSegment.java:172); 执行 = 封存旧段 + open 新段 (LocalLog.java:627-637)**。LogSegment.java:167-173 + LocalLog.java:581-646

跨域关联: K-4 Partition (append 调用链) / K-10 Compaction (LogCleaner 消费段) / E-3 Translog (WAL 对照: generation vs segment)
