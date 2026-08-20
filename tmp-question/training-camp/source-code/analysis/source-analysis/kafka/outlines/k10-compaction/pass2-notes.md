# K-10 闭环笔记 Q1-Q6: 双哈希/三阶段/分组/原子 swap/墓碑/衔接

## Q1: SkimpyOffsetMap 双哈希?

假设: 双哈希探测 key→latestOffset。

验证过程:
- SkimpyOffsetMap (SkimpyOffsetMap.java:32): slots (SkimpyOffsetMap.java:L50) + hash1/hash2 (SkimpyOffsetMap.java:L53-54) — 双哈希数组
- 构造 (SkimpyOffsetMap.java:L73-90): slots = memory / bytesPerEntry (Cleaner.java:L90)
- 语义: 两个哈希函数探测空槽 — 无链表 (省内存, key 只存哈希值)
- 规划断言 (R5: "hash1+hash2 双哈希探测") ✅

代码类型: Algorithmic (哈希表)

结论: **SkimpyOffsetMap = 双哈希探测 (hash1/hash2 SkimpyOffsetMap.java:L53-54), slots=memory/bytesPerEntry (Cleaner.java:L90), bytesPerEntry=hashSize+8 (SkimpyOffsetMap.java:89, 默认 MD5→16B 哈希+8B offset=24B/条, CleanerConfig.java:37)** — 无链表哈希 (内存紧凑, 只存哈希不存 key)。SkimpyOffsetMap.java:32-90

## Q2: buildOffsetMap 三阶段?

假设: 遍历 dirty 段提取 key→latestOffset。

验证过程:
- Cleaner.clean (Cleaner.java:L147-180): legacyDeleteHorizonMs 计算 (Cleaner.java:L147) → buildOffsetMap (Cleaner.java:L156) → 分组 (Cleaner.java:L172) → cleanSegments (Cleaner.java:L180)
- buildOffsetMap: 遍历段 batch → 提取 key → map 存 latestOffset (覆盖旧值)
- maxDesiredMapSize 容量控制 (槽位 × 负载因子)
- 规划断言 (R5: "遍历 dirty segment → 提取 key → 存入 map(key→latestOffset)") ✅

代码类型: Algorithmic (构建阶段)

结论: **三阶段 = buildOffsetMap (Cleaner.java:L156) → groupSegmentsBySize (Cleaner.java:L172) → cleanSegments (Cleaner.java:L180); map 存 key→latestOffset (旧值被覆盖)**。Cleaner.java:147-180

## Q3: cleanSegments 分组?

假设: 段按大小分组, 每组产出一个 cleaned 段。

验证过程:
- groupSegmentsBySize (Cleaner.java:L172): 段分组 ~segmentSize 每组
- cleanSegments (Cleaner.java:L180): 每组遍历消息 → map 中 key 最新 offset == 当前 → 保留; 否则丢弃 (旧值)
- 新段 .cleaned 后缀写
- 规划断言 (R5: "groupSegmentsBySize() 将 segment 分成 ~segmentSize 组") ✅

代码类型: Implementation (清理阶段)

结论: **cleanSegments = 按大小分组 (Cleaner.java:L172) + 遍历判保留 (map latest == 当前 offset) — 旧值丢弃新值保留**。Cleaner.java:172-180

## Q4: 原子 swap?

假设: Log.replaceSegments 原子替换旧段。

验证过程:
- 清理完成后 replaceSegments 替换 (K-3 LocalLog.replaceSegments 衔接)
- .cleaned 文件 → rename 为正式段
- 原子性: 替换在段列表层面 (K-3 LogSegments 容器)
- 规划断言 (R5: "Log.replaceSegments() 原子替换") ✅

代码类型: Implementation (原子替换)

结论: **原子 swap = Log.replaceSegments (K-3 容器级替换): .cleaned → 正式段 — 读者无感 (段列表不可变快照)**。Cleaner.java + K-3 LocalLog.replaceSegments

## Q5: 墓碑消息?

假设: value=null 保留但 legacyDeleteHorizonMs 后删除。

验证过程:
- legacyDeleteHorizonMs 计算 (Cleaner.java:147): 最后 clean 段 lastModified - deleteRetentionMs — **仅 <v2 旧格式生效** (Cleaner.java:145 注释 + Cleaner.java:503-504 legacyRecord 分支)
- **v2+ 墓碑判定**: batch.deleteHorizonMs() 存在则 currentTime < deleteHorizonMs 保留 (Cleaner.java:505-506); 缺失回退 currentTime + deleteRetentionMs (MemoryRecords.java:179)
- cleanableHorizonMs (Cleaner.java:L163): 清理时间窗
- 规划断言 (R5: "墓碑消息(value=null) 保留但 legacyDeleteHorizonMs 后删除") ⚠️ **部分正确** — 现代格式由 batch deleteHorizonMs 判定, legacyDeleteHorizonMs 仅旧格式

代码类型: Implementation (墓碑语义)

结论: **墓碑 = v2+ 由 batch 自带 deleteHorizonMs (写入时刻+delete.retention.ms) 判定删除, <v2 由 legacyDeleteHorizonMs (Cleaner.java:147) — 保留传播删除 → 超时间窗删除**。Cleaner.java:492-529 + MemoryRecords.java:172-183

## Q6: 与 K-3 存储衔接?

假设: LogCleaner 线程池选脏比最高 log, Cleaner 执行。

验证过程:
- LogCleaner (LogCleaner.java:143 cleaners 列表, 209 CleanerThread 创建): 线程池, 默认 1 线程 (CleanerConfig.java:38) + backoff 15s (CleanerConfig.java:43)
- LogCleanerManager (LogCleanerManager.java:L240): grabFilthiestCompactedLog — 需 `(needCompactionNow && cleanableBytes>0) || cleanableRatio > minCleanableRatio (默认 0.5, LogConfig.java:132)` → 取 cleanableRatio 最高 (LogCleanerManager.java:271-290)
- K-3: UnifiedLog compact 段 (K-3 篇 1 已铺垫)
- 触发: 定时 (backoff 15s) + dirty 比阈值
- 规划断言 (R5) ✅ (补充阈值条件)

代码类型: 衔接分析

结论: **衔接 = LogCleaner 线程池 (LogCleaner.java:143/209) + LogCleanerManager 选脏 (LogCleanerManager.java:L240) → Cleaner.clean (Cleaner.java:L147) — K-3 compact 段的消费面**。LogCleaner.java + LogCleanerManager.java

跨域关联: K-3 (存储底座) / K-8 (线程池) / r23-evict (Redis 淘汰对照)
