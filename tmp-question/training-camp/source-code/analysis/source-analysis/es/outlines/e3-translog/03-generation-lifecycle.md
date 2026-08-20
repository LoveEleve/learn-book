# E-3 Translog 篇 3/3 — Generation 生命周期: 日志怎么生、怎么死

> 前置: [[E-3-translog-01]] [[E-3-translog-02]] | 复用: — | 对照: [[r8-persistence]] (AOF rewrite) | 引出: [[E-1-engine]] (flush 消费) [[E-5-shard]] (recovery)
> 🔴 A | 来源: Translog.java:1628 + TranslogDeletionPolicy.java + InternalEngine.java:2173,2121 + IndexSettings.java:384
> 定位: Translog 卷收尾 — 回答"为什么 ES 不需要 AOF 的 rewrite"

**读者处境**: Redis 的 AOF 日志无限增长, 所以要定期 rewrite (全量重写) 压缩。ES 的 translog 文件为什么不会无限膨胀? 面试官问 "translog 文件什么时候删? 谁删?" — 看完这篇你不仅能答, 还能讲出 Redis AOF 与 ES translog 在生命周期设计上的根本差异。

### 1. 问题引入 — 三个日志文件躺在磁盘上

场景: `ls shard/translog/` 看到 translog-1.tlog, translog-2.tlog, translog.ckp + 各代的 .ckp — 为什么不是一个文件?
- 答案: **generation 轮转** — 一个 writer 只写一个文件, 写满/换 term 就"封存"开新代
- 本篇问题: 什么触发轮转? 封存的文件什么时候被删? 为什么不用 AOF 那种 rewrite?

### 2. 轮转 — rollGeneration 三步

场景: 什么情况下当前文件被"封存"?
- 源码路径: Translog.rollGeneration (Translog.java:1628-1652): `syncBeforeRollGeneration` (Translog.java:1629, 先把数据移出锁外落盘) → writeLock → `current.closeIntoReader()` (Translog.java:1636) → `readers.add(reader)` → `copyCheckpointTo(translog-gen.ckp)` (Translog.java:1639) → `createWriter(gen+1)` (Translog.java:1641)
- **空代跳过** (Translog.java:1630): `totalOperations() == 0 && term 未变 → return` — 不产生空文件
- 触发点: ① 大小超阈 `shouldRollGeneration` (Translog.java:619-625, generation_threshold_size 默认 **64MB** IndexSettings.java:384-391) ② primaryTerm 变化 (rollGeneration 由 Engine/Shard 在 term 变更时调用) ③ flush 隐式轮转 (IndexShard.afterWriteOperation IndexShard.java:3763-3795)
- 关键设计: closeIntoReader 前**先 sync** (Translog.java:1629 syncBeforeRollGeneration) — 封存的代必须完整落盘, 才能安全转只读

### 3. 删除 — TranslogDeletionPolicy 双约束

场景: 封存的旧文件什么时候消失?
- 安全删除的两个约束 (Translog.java:1710-1721 getMinReferencedGen):
  - ① `deletionPolicy.getMinTranslogGenRequiredByLocks()` (TranslogDeletionPolicy.java:111) — **引用计数**: snapshot/recovery 持有 `acquireTranslogGen` (TranslogDeletionPolicy.java:66) 时禁止删
  - ② `minGenerationForSeqNo(localCheckpointOfSafeCommit + 1)` — **安全水位**: Lucene safe commit 的 localCheckpoint 之后的操作必须保留 (可能还没进 Lucene)
- 执行: trimUnreferencedReaders (Translog.java:1661-1728): readLock 快速判断 → sync → writeLock 逐个删 `deleteReaderFiles` (tlog + 其 gen.ckp)
- 关键设计: 删除前先 `current.sync()` (Translog.java:1695) — "sync at once to make sure that there's at most one unreferenced generation" — 崩溃容错: 最多多留一个未引用代
- 恢复侧容错 (Translog.java:1690-1694 注释): "Note that there is a provision in recoverFromFiles to allow for the case where we synced the checkpoint but crashed before we could delete the file" — 即使 ckp 已更新但 tlog 文件未删, 恢复时容忍

### 4. flush 与 translog 的衔接 — 谁在消费轮转

场景: 轮转和 flush (Lucene commit) 什么关系?
- InternalEngine.flush (InternalEngine.java:2173): ① refresh → ② IndexWriter.commit (Lucene 落盘) → ③ `translog.createNewTranslog` (新代) — commit 后旧代可以被删
- **flush 阈值** (InternalEngine.java:2116-2123 计算, 方法声明 2129): shouldPeriodicallyFlush — translog size > 512MB 或 age > 1min → flush
- 触发位置: afterWriteOperation (IndexShard.java:3763) + 大 merge 后 (shouldPeriodicallyFlushAfterBigMerge InternalEngine.java:2834)
- 关键设计: translog 大小**驱动** flush (而非定时) — 日志长 = 未提交多 = 该 commit 了

### 5. 时空溯源 — 从定时轮询到同步决策

场景: 这个生命周期设计是怎么演化的?
- v0.90: TranslogService 独立定时线程 (5s interval) 轮询三阈值 (ops>5000/size>200MB/period>30min) 触发 flush — **延迟最多 5s**
- v5.0 (commit 75e816400c2, 2015-09-23): 删除 TranslogService → 折叠进 **同步 IndexShard API** — "we can actually make all the decisions in a sync manner which is way easier to control and to test"
- v8.12: afterWriteOperation 写路径自查 (IndexShard.java:3763) + CAS 单飞 (flushOrRollRunning IndexShard.java:3761) + FLUSH 线程池
- 设计权衡: 定时器简单但有延迟+难测试; 同步决策零延迟但写路径多一次判断 (分支预测友好)

### 6. 收束 — 与 Redis AOF rewrite 的根本对照

- Redis AOF: 全量操作日志 (含已持久化数据) → 必须 rewrite (base+incr, auto-aof-rewrite-percentage=100, server.c:1434-1440)
- ES translog: **只含未提交操作** → commit 后即可删代, 天然小; 不需要"重写"机制
- 差异根因: 数据模型 — Redis 内存引擎的持久化=完整日志+重放; ES 磁盘索引的持久化=Lucene commit + 增量日志
- 终极结论: translog 是"Lucene 与磁盘之间的保险丝", 不是"历史的完整记录"

### 核心悬念
"Redis AOF 要 rewrite, ES translog 为什么不用?" — 因为 translog 只存**没来得及进 Lucene 的操作**, Lucene commit 替它做了 rewrite。

### 概念依赖链
Q8 AOF 对照 → rollGeneration 三步 → 删除双约束 → flush 衔接 → 时空溯源

### 源码锚点清单
- Translog.java:619-625 (shouldRollGeneration) / 1628-1652 (rollGeneration) / 1629 (先 sync) / 1630 (空代跳过) / 1636 (closeIntoReader) / 1639 (copyCheckpointTo) / 1641 (createWriter) / 1661-1728 (trimUnreferencedReaders) / 1690-1694 (恢复容错注释) / 1695 (删前 sync) / 1710-1721 (getMinReferencedGen)
- TranslogDeletionPolicy.java:66 (acquireTranslogGen) / 37 (translogRefCounts) / 111 (getMinTranslogGenRequiredByLocks)
- IndexSettings.java:384-391 (generation_threshold_size 64MB)
- InternalEngine.java:2173 (flush) / 2116-2123 (write 阈值计算) / 2129 (shouldPeriodicallyFlush 声明) / 2834 (大 merge 后)
- IndexShard.java:3763-3795 (afterWriteOperation) / 3761 (flushOrRollRunning)
- server.c:1434-1440 (Redis AOF rewrite 触发)
