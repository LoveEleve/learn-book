# K-10 Compaction 篇 2/2 — 收尾与删除: 原子 swap 与墓碑

> 前置: [[K-10-compaction-01]] (双哈希+三阶段) | 复用: — | 对照: [[r22-expire]] (Redis 惰性+主动过期) [[r23-evict]] (内存淘汰) [[r8-persistence]] (AOF 重写) | 引出: — (K-9 交付后补链)
> 🟡 B | 来源: Cleaner.java:147-169,492-529 + LogCleaner.java:143,209 + LogCleanerManager.java:240 + LogConfig.java:132 + K-3 replaceSegments (LocalLog.java:996)
> 定位: K-10 卷收尾 — 回答"清理结果怎么落地? 删除怎么传播?"

**读者处境**: 面试官问 "压缩完旧段怎么换? 删除消息怎么传播?" 你答 "替换、墓碑" — 但再问 "原子性怎么保证? 墓碑什么时候删? 谁选脏日志?" 你答不上来。这篇是收尾机制的完整答案, 收束 K-10 域。

### 1. 问题引入 — 清理完怎么办

场景: .cleaned 新段写好了 — 怎么替换旧段?
- 原子 swap (K-3 replaceSegments)
- 本篇问题: swap (Q4) / 墓碑 (Q5) / 衔接 (Q6)

### 2. 原子 swap — 读者无感

场景: 替换怎么不打断读者?
- .cleaned 文件 → rename 正式段 (K-3 容器级替换, LocalLog.replaceSegments LocalLog.java:996)
- Log.replaceSegments: 段列表不可变快照 (K-3 已铺垫)
- 原子性: 替换在容器层, 读者持有旧快照无感

### 3. 墓碑 — 删除的传播

场景: value=null 的消息怎么处理?
- 墓碑保留: map 中 key 最新 offset == 当前 (latestOffsetForKey) 且 **无值可删时** (Cleaner.shouldRetainRecord)
- **v2+ 格式 (现代)**: 墓碑删除由 **batch 自带的 deleteHorizonMs** 判定 (Cleaner.java:505-506: currentTime < batch.deleteHorizonMs 则保留; 缺失时回退 currentTime + deleteRetentionMs, MemoryRecords.java:179)
- **<v2 旧格式**: legacyDeleteHorizonMs (Cleaner.java:147, 最后 clean 段 lastModified - deleteRetentionMs) 判定 (Cleaner.java:503-504) — 只影响旧格式段
- cleanableHorizonMs (Cleaner.java:163): 清理时间窗 (低于它才安全删墓碑)
- 与 Redis 对照: Kafka 墓碑时间窗删除 (batch deleteHorizonMs) vs Redis 惰性+主动过期 (r22-expire); 内存淘汰是另一机制 (r23-evict)

### 4. 调度衔接 — 收束

场景: 谁触发压缩?
- LogCleaner 线程池: 默认 **1 个 CleanerThread** (CleanerConfig.java:38 LOG_CLEANER_THREADS=1, LogCleaner.java:209 启动) + backoff 15s (CleanerConfig.java:43)
- LogCleanerManager 选脏 (LogCleanerManager.java:240): 需满足 `(needCompactionNow && cleanableBytes>0) || cleanableRatio > minCleanableRatio (默认 0.5, LogConfig.java:132)` → 取 cleanableRatio 最高
- K-3 compact 段消费面
- 与 AOF 重写对照 (r8): 压缩 = 去重重写 (段内), AOF rewrite = 全量归并重写 (fork 子进程)
- **负面空间**: 不做全量重写 (压缩只处理 dirty 段, AOF rewrite 重写全库); 不做即时物理删除 (墓碑等 deleteHorizonMs 时间窗); 不做段合并 (无 TieredMerge, 与 ES E-8 对照)

### 核心悬念
"墓碑为什么延迟删除?" — 消费者可能还没看到删除消息 (offset 未过墓碑); 立即删会丢失"删除"语义 — v2+ 由 producer 写入 batch 的 deleteHorizonMs (写入时刻 + delete.retention.ms) 给消费者窗口, 之后物理删 (与 Redis 惰性删除同理但机制不同: 时间窗 vs 访问触发)。

### 概念依赖链
Q4 swap → Q5 墓碑 → Q6 衔接 → (K-9 待补链)

### 源码锚点清单
- Cleaner.java:147 (legacyDeleteHorizonMs) / 160-169 (cleanableHorizonMs)
- LogCleaner.java:143,209 (cleaners 列表/CleanerThread 创建) / LogCleanerManager.java:240 (grabFilthiestCompactedLog)
- K-3: Log.replaceSegments (LocalLog.java:996, 原子替换)
