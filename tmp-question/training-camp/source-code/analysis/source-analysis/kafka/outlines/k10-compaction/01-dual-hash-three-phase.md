# K-10 Compaction 篇 1/2 — 去重的引擎: 双哈希与三阶段

> 前置: [[K-3-log-01]] (分段存储) | 复用: — | 对照: [[r23-evict]] (Redis 淘汰) | 引出: [[K-10-compaction-02]]
> 🟡 B | 来源: Cleaner.java:147-180 + SkimpyOffsetMap.java:32-90
> 定位: K-10 卷开篇 — 回答"compact 怎么去重? 三阶段怎么走?"

**读者处境**: 面试官问 "Kafka 日志压缩干什么? 怎么实现?" 你答 "去重" — 但再问 "SkimpyOffsetMap 为什么双哈希? 三阶段顺序? 段怎么分组?" 你答不上来。这篇是压缩算法的完整答案。

### 1. 问题引入 — 日志为什么需要压缩

场景: key 更新 100 次, 日志留 100 条旧值 — 消费只要最新
- compact: 保留每 key 最新 value (K-3 compact 段)
- 本篇问题: 双哈希 (Q1) / 三阶段 (Q2/Q3)

### 2. 双哈希 — SkimpyOffsetMap

场景: 怎么在内存记住 key→offset?
- SkimpyOffsetMap (SkimpyOffsetMap.java:32): slots (SkimpyOffsetMap.java:L50) + hash1/hash2 双哈希数组 (SkimpyOffsetMap.java:L53-54)
- slots = memory / bytesPerEntry (Cleaner.java:L90) — 内存紧凑 (只存哈希不存 key)
- 双哈希探测空槽 (无链表) — 冲突线性探测

### 3. 三阶段 — build→group→clean

场景: 一次 clean 的三步?
- buildOffsetMap (Cleaner.java:156): 遍历 dirty 段提取 key→latestOffset
- groupSegmentsBySize (Cleaner.java:L172): 段按大小分组 (~segmentSize 每组)
- cleanSegments (Cleaner.java:L180): 每组遍历 → map 中 key 最新 == 当前 → 保留, 否则丢弃
- legacyDeleteHorizonMs (Cleaner.java:L147): 墓碑时间窗

### 核心悬念
"SkimpyOffsetMap 为什么不用 HashMap?" — HashMap 存完整 key (每条旧值都占内存); Skimpy 只存 16 字节 MD5 哈希 + 8 字节 offset (bytesPerEntry=hashSize+8, SkimpyOffsetMap.java:89, HASH_ALGORITHM="MD5" CleanerConfig.java:37) → 内存省一个量级, 代价是哈希碰撞需探测 — "skimpy" 名字就是"省内存"的设计意图。

### 概念依赖链
Q1 双哈希 → Q2/Q3 三阶段 → (02 篇: swap+墓碑)

### 源码锚点清单
- SkimpyOffsetMap.java:32 (类) / 37 (bytesPerEntry 字段) / 50 (slots) / 53-54 (hash1/hash2) / 89-90 (bytesPerEntry=hashSize+8, slots=memory/bytesPerEntry)
- Cleaner.java:147 (legacyDeleteHorizonMs) / 156 (buildOffsetMap) / 172 (groupSegmentsBySize) / 180 (cleanSegments)
- CleanerConfig.java:37 (HASH_ALGORITHM=MD5) / 40 (dedupe buffer 128MB) / 42 (负载因子 0.9)
