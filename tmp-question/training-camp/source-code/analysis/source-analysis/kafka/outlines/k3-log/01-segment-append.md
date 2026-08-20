# K-3 Log 存储 篇 1/3 — 一分为四: 分段结构与写路径

> 前置: — (阶段4 首个域) | 复用: — | 对照: [[r8-persistence]] (Redis RDB/AOF) [[E-3-translog]] (ES WAL) | 引出: [[K-3-log-02]] [[K-3-log-03]] [[K-10-compaction-01]] (K-10 已交付域回补)
> 🔴 A | 来源: LogSegment.java:55-64,79-84,250-280 + LocalLog.java:68,80,526-529 + ServerLogConfigs.java:81
> 定位: K-3 卷开篇 — 回答"Kafka 一个分区日志到底长什么样? 数据怎么进去的?"

**读者处境**: 面试官问 "Kafka 为什么快? 日志文件怎么组织的?" 你答 "顺序写、分段" — 但再问 "一个段几个文件? 索引什么时机写? LEO 怎么推?" 你答不上来。这篇是分区日志存储的完整答案。

### 1. 问题引入 — 一个分区的目录里有什么

场景: 你 `ls` 一个 topic-partition 目录, 看到一堆 `.log/.index/.timeindex/.txnindex` 文件 — 它们是什么? 为什么不是一个大文件?
- 分段存储: 单个大文件 → 多个 LogSegment (LogSegment.java:55-64 注释)
- 本篇问题: 结构 (Q1) / 写路径 (Q2)

### 2. 四件套 — 一个段的解剖

场景: 一个 segment 文件怎么协作?
- LogSegment 四字段 (LogSegment.java:79-84): FileRecords log (LogSegment.java:79) + LazyIndex<OffsetIndex> (LogSegment.java:80) + LazyIndex<TimeIndex> (LogSegment.java:81) + TransactionIndex (LogSegment.java:82)
- baseOffset 命名 (LogSegment.java:83): 段名 = 最小 offset, 排序即天然有序
- 容器: LogSegments (LocalLog.java:80) — append/roll/truncate 全委托
- 对照 E-3: ES translog 一个代一个文件 vs Kafka 一段四文件 (数据+双索引+事务)

### 3. 写路径 — 数据/索引/LEO 的顺序

场景: 一条消息进来, 经历了什么?
- LogSegment.append (LogSegment.java:250-280): physicalPosition 记录 (LogSegment.java:255) → log.append 写数据 (LogSegment.java:260) → 逐 batch 更新 maxTimestamp (LogSegment.java:266-268)
- **稀疏索引**: 每 indexIntervalBytes (默认 4096 字节, ServerLogConfigs.java:81) 才记一条索引 (LogSegment.java:270-274) — 不是每条都记
- LocalLog.append (LocalLog.java:526-529): activeSegment.append (LocalLog.java:527) + updateLogEndOffset(lastOffset+1) (LocalLog.java:528)
- 顺序写: 只写活动段尾部 — 磁盘顺序 IO 是 Kafka 快的根基
- 对照 E-8 (ES 段合并): 同构 — 都以"段"为管理单元 (滚/删/清); 差异: Kafka 段无合并 (compaction 是 K-10 独立机制), ES 段有 TieredMerge 合并

### 核心悬念
"Kafka 日志为什么是'分段'而不是一个大文件?" — 单一文件无法清理/截断/删除 (旧数据必须可删), 分段让"删除"变成"删文件" (原子), 让"崩溃恢复"变成"恢复最后一个段" — 以段为管理单元。

### 概念依赖链
Q1 结构 → Q2 写路径 → (02 篇: 索引/roll/读) → (03 篇: 恢复/截断/ProducerState)

### 源码锚点清单
- LogSegment.java:55-64 (类注释: 分段语义) / 79-84 (四字段) / 250-280 (append) / 255 (physicalPosition) / 260 (log.append) / 266-268 (maxTimestamp) / 270-274 (稀疏索引) / 277 (字节计数)
- LocalLog.java:68 (类) / 80 (LogSegments) / 526-529 (append+LEO) / 528 (updateLogEndOffset)
- ServerLogConfigs.java:81 (LOG_INDEX_INTERVAL_BYTES_DEFAULT=4096)
