# K-10 Compaction — 知识规划 (00 §10: 逐源提取→聚合→分类→聚类)

> 2026-08-15 | 源码: storage/internals/log/ (Cleaner 766 + LogCleaner 654 + SkimpyOffsetMap 241 + LogCleanerManager 798) — Java 已索引

## 01 逐源提取

| 源文件 | 机制点 |
|---|---|
| Cleaner.java | ①clean 三阶段 (Cleaner.java:L147-180) ②legacyDeleteHorizonMs (Cleaner.java:L147) ③buildOffsetMap (Cleaner.java:L156) ④groupSegmentsBySize (Cleaner.java:L172) ⑤cleanSegments (Cleaner.java:L180) |
| SkimpyOffsetMap.java | ①双哈希 (SkimpyOffsetMap.java:L53-54) ②slots (SkimpyOffsetMap.java:L50) ③构造 (SkimpyOffsetMap.java:L73-90) |
| LogCleaner.java | ①线程池管理 (654 行) |
| LogCleanerManager.java | ①grabFilthiestCompactedLog (798 行) |

## 02 聚合 (P1/P2/P3)

| 聚合机制 | 来源 | 分级 |
|---|---|---|
| 三阶段 clean | Cleaner | P1 |
| 双哈希 map | SkimpyOffsetMap | P1 |
| 原子 swap | K-3 replaceSegments | P1 |
| 墓碑语义 | Cleaner | P1 |
| 线程池+选脏 | LogCleaner + Manager | P2 |

## 03 深度分类

- 🔴: 三阶段 + 双哈希 + 原子 swap (压缩核心)
- 🟡: 墓碑 / 线程池
- 🟢: 配置 (cleanup.policy=compact)

## 04 聚类 (教学顺序)

```
选脏 (LogCleanerManager) → 构建 map (SkimpyOffsetMap 双哈希) → 分组清理 → 原子 swap → 墓碑
拆篇: 01 双哈希+三阶段 / 02 swap+墓碑+衔接
