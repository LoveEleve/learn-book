# E-3 闭环笔记 Q5-Q8: trim/锁层次/tragedy/AOF 对照

## Q5: trimmedAboveSeqNo 是什么? — peer recovery 截断机制

假设: 主分片提升/降级后, 旧 primaryTerm 的操作需要从 translog 逻辑删除 — trimmedAboveSeqNo 是"只读截止点", 不物理删文件。

验证过程:
- grep trimOperations 调用方 → IndexShard.java:1834 `trimOperationOfPreviousPrimaryTerms(aboveSeqNo)` → `getEngine().trimOperationsFromTranslog(getOperationPrimaryTerm(), aboveSeqNo)`
- Read Translog.trimOperations (Translog.java:802-838): 对 readers 逐个 `closeIntoTrimmedReader(aboveSeqNo, ...)` — **生成新 checkpoint 而非删文件**
- Read TranslogReader.closeIntoTrimmedReader (TranslogReader.java:71-107): 新 Checkpoint 只改 trimmedAboveSeqNo 字段, `Checkpoint.write` 写 translog-gen.ckp, 文件本体不动
- Read Checkpoint.maxEffectiveSeqNo (Checkpoint.java:110-115): `trimmedAboveSeqNo == UNASSIGNED ? maxSeqNo : min(trimmedAboveSeqNo, maxSeqNo)` — snapshot 过滤依据
- Read Translog.trimOperations 前提断言 (Translog.java:803,807-809): 只能 trim 低于当前 term 的

代码类型: Implementation (逻辑删除优化: 免重写大文件)

结论: **peer recovery 把旧 primary 的操作截断在 seqNo 水位 — 只更新 checkpoint 的 trimmedAboveSeqNo, 不物理删除数据; 恢复时 snapshot 按 maxEffectiveSeqNo 跳过被 trim 的操作。这是"逻辑截断"而非"物理重写", 与 flush 时 generation 截断互补**。Translog.java:802 + TranslogReader.java:71 + Checkpoint.java:110

跨域关联: E-6 SeqNo (primaryTerm/seqNo 语义) / E-5 Shard (peer recovery 消费)

## Q6: 四层锁层次 — 并发写 + 串行 sync

假设: readLock 允许多写并发, writeLock 串行化轮转/trim, syncLock 串行化 fsync, synchronized(this) 保护 buffer 内部状态 — 四级分工。

验证过程:
- Read TranslogWriter.java:73-75 注释: `lock order try(Releasable lock = writeLock.acquire()) -> synchronized(this)` 和 `synchronized(syncLock) -> writeLock -> synchronized(this)` — **锁顺序有文档约束**
- Read Translog.add (Translog.java:580-598): 持 **readLock** (共享) 调 current.add — 多个索引线程可并发 add
- Read rollGeneration (Translog.java:1628-1652): 持 **writeLock** (独占) — 轮转串行
- Read syncUpTo (TranslogWriter.java:465-534): **syncLock** 内双检 → writeLock → synchronized(this) — fsync 串行
- Read TranslogWriter.add (TranslogWriter.java:234): `synchronized (this)` 保护 buffer 指针/偏移

代码类型: Implementation (锁粒度的层级化设计)

结论: **add 走共享 readLock + synchronized(this) 短临界区 (只动内存 buffer), fsync 走 syncLock 串行 (一次只有一个线程 force), 轮转/trim 走独占 writeLock — 索引线程几乎不互斥, 持久化线程串行, 结构变更独占; 锁顺序硬编码防死锁**。Translog.java:580 + TranslogWriter.java:73-75,463

## Q7: tragedy — 一次失败全关的原因

假设: translog 一旦出现 IO 异常, 部分操作可能已写盘部分没写 — 继续写会产生"半持久化"错乱, 必须整体关闭并上报。

验证过程:
- Read TragicExceptionHolder (TragicExceptionHolder.java:15-33): AtomicReference 保存首个异常, 后续异常加 suppressed — **首因保留**
- grep closeOnTragicEvent 调用点 → Translog.java:602,605,706,775 (add/sync/ensureSynced 的 catch) — 所有 IO 路径统一处理
- Read closeOnTragicEvent (Translog.java:869-889): 若 tragedy 已设 → close() 整个 translog + 注释 "we can not hold a read lock here because closing will attempt to obtain a write lock and that would result in self-deadlock"
- 对照 InternalEngine.java:1825,1978: Engine 侧同样检查 indexWriter.getTragicException() — **Lucene 同款模式**

代码类型: Implementation (fail-fast 状态机)

结论: **tragedy 是"持久化状态不可信"的标志 — 任何 IO 异常都可能导致 translog 部分落盘, 继续写会掩盖数据损坏; 所以首次异常冻结状态 (AtomicReference CAS), 后续异常只做 suppressed, 触发整体 close, 上层 Engine 感知后让 shard 进入失败态**。TragicExceptionHolder.java:15 + Translog.java:869

## Q8: 与 Redis AOF 对照 — 两种 WAL 的演化差异

假设: AOF (rewrite 全量重写) vs ES translog (generation 轮转 + 逻辑 trim) — 同为"先写日志再落数据", 但空间回收策略不同。

验证过程:
- Redis 侧: server.c:1434-1440 `aof_rewrite_perc` (默认 100) + `aof_current_size > aof_rewrite_min_size` → 触发后台 rewrite (全量重写)
- ES 侧: IndexSettings.java:384-391 `index.translog.generation_threshold_size` 默认 **64MB** → rollGeneration; TranslogDeletionPolicy 按引用计数 + localCheckpointOfSafeCommit 删旧代
- ES 无"全量重写" — 因为 translog 只含**未提交**操作 (已提交的由 Lucene 持久化), 天然小; AOF 含全部操作, 必须 rewrite 压缩

代码类型: 对照分析

结论: **同为 WAL, 但生命周期不同: Redis AOF 是完整操作日志 (需 rewrite 压缩), ES translog 只保留"未提交操作" (commit 后即弃, 由 checkpoint 水位 + 引用计数删除) — ES 的"重写"是 Lucene commit 而非 translog 自身; 这是两者数据模型差异 (内存引擎 vs 磁盘索引) 的直接结果**。对照锚点: IndexSettings.java:384 vs server.c:1434

跨域关联: [[r8-persistence]] (Redis AOF) 对照
