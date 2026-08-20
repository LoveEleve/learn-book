# K-10 Compaction — 六层深审 REVIEW 记录 (2026-08-15)

> 审查方法: 07 五维度 + 逐锚点核对 + 裸行号三形态扫描 + 行号上限检查 + 跨域引用核验
> **结论: 深审通过 (2 处编造类错误修复 + 1 处对照错引修复 + 调度条件补强; 三形态零残留)**

## 第一层: 锚点验证 (写时即 grep/Read)

- SkimpyOffsetMap.java: 类 SkimpyOffsetMap.java:32 / bytesPerEntry 字段 SkimpyOffsetMap.java:37 / slots SkimpyOffsetMap.java:50 / hash1/hash2 SkimpyOffsetMap.java:53-54 / 构造 SkimpyOffsetMap.java:73-93 (bytesPerEntry=hashSize+8 SkimpyOffsetMap.java:89, slots=memory/bytesPerEntry SkimpyOffsetMap.java:90, hash1/hash2 分配 SkimpyOffsetMap.java:92-93) ✅
- Cleaner.java: legacyDeleteHorizonMs Cleaner.java:147 / buildOffsetMap Cleaner.java:156 / cleanableHorizonMs Cleaner.java:163 / groupSegmentsBySize Cleaner.java:172 / cleanSegments Cleaner.java:180 ✅
- Cleaner.cleanSegments (Cleaner.java:205-300): .cleaned 段创建 Cleaner.java:209 / retainLegacyDeletesAndTxnMarkers 判定 Cleaner.java:234 / flush Cleaner.java:286 / replaceSegments Cleaner.java:296 ✅
- Cleaner.shouldRetainRecord (Cleaner.java:492-529): pastLatestOffset Cleaner.java:500 / latestOffsetForKey Cleaner.java:513 / **v2+ deleteHorizonMs 判定 Cleaner.java:505-506** / **<v2 legacyRecord 分支 Cleaner.java:503-504** ✅
- MemoryRecords.RecordFilter (MemoryRecords.java:172-183): deleteHorizonMs 回退 currentTime+deleteRetentionMs MemoryRecords.java:179 ✅
- LogCleaner.java: cleaners 列表 LogCleaner.java:143 / CleanerThread 创建 LogCleaner.java:209 / 类注释 "dirtiest log" ✅
- LogCleanerManager.java: grabFilthiestCompactedLog LogCleanerManager.java:240 / 阈值条件 (needCompactionNow || cleanableRatio>minCleanableRatio) LogCleanerManager.java:271-274 / 取 max LogCleanerManager.java:289 ✅
- CleanerConfig.java: HASH_ALGORITHM="MD5" CleanerConfig.java:37 / LOG_CLEANER_THREADS=1 CleanerConfig.java:38 / dedupe 128MB CleanerConfig.java:40 / 负载因子 0.9 CleanerConfig.java:42 / backoff 15s CleanerConfig.java:43 ✅
- LogConfig.java: DEFAULT_MIN_CLEANABLE_DIRTY_RATIO=0.5 LogConfig.java:132 / DEFAULT_DELETE_RETENTION_MS=24h LogConfig.java:129 ✅
- LocalLog.java: replaceSegments LocalLog.java:996 (K-3 衔接) ✅

## 第二层: 机制实证 (2 处修正)

- 双哈希探测 (SkimpyOffsetMap.java:95-135 get/put 线性探测) ✅
- 三阶段 (Cleaner.doClean Cleaner.java:138-190) ✅
- 分组 (groupSegmentsBySize Cleaner.java:566-600: 大小+索引双上限+相对偏移溢出保护) ✅
- 原子 swap (cleanSegments Cleaner.java:296: log.replaceSegments) ✅
- **墓碑判定修正**: v2+ 由 batch deleteHorizonMs (Cleaner.java:505-506), <v2 才用 legacyDeleteHorizonMs (Cleaner.java:503-504) — 原大纲断言 "legacyDeleteHorizonMs 后删除" 不精确
- **调度条件补强**: 需 cleanableRatio > 0.5 (默认) 或 needCompactionNow — 原大纲只写 "选脏比最高"

## 第三层: 编造检查 (1 处修复)

- **修复 (编造类数值错误, Kafka 阶段第 3 次)**: "只存 8 字节哈希" → 实际 MD5=16 字节 + 8 字节 offset = **24B/entry** (SkimpyOffsetMap.java:89, CleanerConfig.java:37) — 修复 01 核心悬念/锚点清单 + pass2 Q1
- 其余数值 (128MB/0.9/15s/0.5/24h/1 线程) 全部源码实证 ✅

## 第四层: 覆盖缺口 (completeness 18 问, 0 回补)

- 18/18 ✅ — 深审发现已当场修复进大纲 (数值/墓碑/对照/调度)

## 第五层: 裸行号

- 写时 4 处裸形 (pass2 Q1 "hash1/hash2 L53-54" / pass2 Q5 "L503-504" / pass2 Q6 "(L271-290)" / review-notes 全量) → 已修 → **0 残留**
- 上限: 全部锚点 ≤ 文件行数 (SkimpyOffsetMap 333 / Cleaner 766 / LogCleaner 654 / LogCleanerManager 798 / LocalLog 1186) ✅

## 第六层: 跨域引用核验 (1 处错引修复)

- **修复 (对照错引)**: 02 篇 §3 "Redis 惰性+主动过期 (r23)" → **r22-expire 才是惰性+主动过期** (r22 outline.md:3,7 实证); r23-evict 是内存淘汰 (保留为对照, 降级提及)
- r22-expire ✅ / r23-evict ✅ / r8-persistence (AOF rewrite, outline-aof.md q4) ✅ / K-3 LocalLog.replaceSegments ✅ / ES E-8 (段合并, K-3 01 篇已实证) ✅
- 双链合规: 02 引出 "— (K-9 交付后补链)" — 未链未交付域 ✅

---

## 07 五维度深度收官 (REVIEW-2)

### R1 维度1 (桥+结构): 1 发现, 1 修复 ⚠️

- **发现**: 负面空间缺失 (07 维度5 要求对比型域显式 "不做") — 02 篇无任何 "不做" 声明
- **修复**: 02-L4 补负面空间三连 (不做全量重写/不做即时物理删除/不做段合并)

### R2 维度2 (锚点密度): 0 发现, 收敛

- 01 篇: 9 个文件锚点 / 02 篇: 9 个 (🟡B ≥4 超标) ✅
- 上限全过 (第五层)

### R3 维度3 (前向引用): 0 发现, 收敛

- 前置: 01 无 (域内首篇) / 02 前置 01 ✅; 引出: 01→02 ✅, 02→K-9 "交付后补链" (未声明依赖) ✅
- 对照 r22/r23/r8 全部已交付域 (redis 阶段3) ✅

### R4 维度4 (横切: 线程面): 0 发现, 收敛

- LogCleaner CleanerThread 后台线程: 01/02 覆盖 (选脏+执行), 线程默认 1 + backoff 15s ✅
- 横切对照: K-8 网络线程池 (num.io.threads) 非 cleaner 线程 — 无混淆 ✅

### R5 维度5 (负面+开篇): 1 发现, 1 修复 (同 R1)

- 负面空间 3/3 (修复后); 开篇: 01/02 均为面试场景化开篇 (面试官问 → 答不上来) ✅

### 内容深度轮: 2 重大发现, 2 修复 ⚠️⚠️

- **发现 1 (编造数值)**: "8 字节哈希" → 实际 24B/entry (MD5 16B + 8B offset) — 与 K-4 (256KB→2MB) / K-1 (linger.ms) 同类, 编造类数值第 3 次
- **发现 2 (墓碑机制)**: legacyDeleteHorizonMs 仅 <v2 旧格式 (Cleaner.java:145 注释 + Cleaner.java:503-504); v2+ 由 batch.deleteHorizonMs 判定 (Cleaner.java:505-506, 写入时刻+delete.retention.ms) — 原大纲把旧格式机制当唯一机制
- 反写测试: 10 场景, 只读大纲可写文章 ✅
- 跨域对照核验: r22 vs r23 语义区分 (过期 vs 淘汰) — K-12 编造对照教训重审 ✅

### 收敛判定

R1 发现 1 (负面空间) + 内容深度轮 2 重大发现 (数值/墓碑) + 第六层 1 错引 (r22/r23) — 全部修复后三形态零残留。K-10 深度收官完成。
