# E-3 Translog 篇 2/3 — Checkpoint 与恢复: 单盘块里的原子水位

> 前置: [[E-3-translog-01]] (写路径产物 Location/offset) | 复用: — | 对照: [[r8-persistence]] (AOF 恢复) | 引出: [[E-3-translog-03]] [[E-5-shard]] (recovery) [[E-6-seqno]] (globalCheckpoint)
> 🔴 A | 来源: Checkpoint.java + Translog.java:657,229,802 + TranslogReader.java:71
> 定位: Translog 卷中篇 — 回答"崩溃后怎么知道回放哪些操作"

**读者处境**: 篇 1 你懂了"写入先进 translog 再进 Lucene"。现在节点断电重启 — 磁盘上躺着几个 translog 文件 (translog-1.tlog, translog-2.tlog...) 和一堆 .ckp 文件。ES 怎么知道: 哪些操作还没进 Lucene 需要回放? 哪些已经安全可以丢弃? 如果日志中途损坏了怎么办? 面试题 "translog 恢复流程" 的答案全在这一篇。

### 1. 问题引入 — 断电后的磁盘状态

场景: 崩溃瞬间, 磁盘上有: lucene 索引 (含 commit 点) + translog-2.tlog (可能只写了一半) + translog.ckp
- 三个问题: ① 回放哪些代? ② 每代回放到哪个偏移? ③ 日志不完整/损坏怎么发现?
- 答案都收敛到一个文件: **checkpoint**

### 2. Checkpoint — 8 字段的单盘块水位

场景: ckp 文件为什么被设计成"恰好 <512 字节"?
- 源码路径: Checkpoint.java:35-42 — 8 字段: offset/numOps/generation/minSeqNo/maxSeqNo/globalCheckpoint/minTranslogGeneration/trimmedAboveSeqNo
- **原子写保证** (Checkpoint.java:228-229): `assert indexOutput.getFilePointer() < 512 : "checkpoint files have to be smaller than 512 bytes for atomic writes"` — 单盘块写 = 崩溃时要么旧值要么新值, 不会半写
- 格式: Lucene CodecUtil header/footer + CRC 校验 (Checkpoint.java:172 `checksumEntireFile`)
- 关键设计: 为什么是"水位"不是"日志"? — checkpoint 存的是**已确认状态**, 不是日志本身; 恢复 = checkpoint 水位以上回放

### 3. 恢复流程 — recoverFromFiles 倒序打开

场景: 有了 checkpoint, 恢复怎么进行?
- 入口: Translog 构造器 (Translog.java:194) → recoverFromFiles(checkpoint)
- 倒序打开 (Translog.java:225-256): `for (i = checkpoint.generation; i >= minGenerationToRecoverFrom; i--)` (Translog.java:236) — 先验证最新代 UUID, 注释 "This gives for better error messages if the wrong translog was found" (Translog.java:233-235)
- 每代读取: 当前代用主 ckp, 旧代用 translog-gen.ckp (Translog.java:241-242 `Checkpoint.read(getCommitCheckpointFileName(i))`)
- 连续性断言 (Translog.java:254): "translog ids must be consecutive" — 缺失文件即损坏
- **UUID 校验** (TranslogHeader.java:111-158): header 存 uuid + primaryTerm; 不匹配 → "this translog file belongs to a different translog" — 防错用别的分片的日志恢复
- **操作格式**: 每条操作 = size(4B) + checksum + 序列化体 (BufferedChecksumStreamInput) — 读取时逐条校验; 恢复耗时 ≈ 重放 checkpoint 水位以上的操作数 (通常远小于全量)

### 4. minTranslogGeneration 与 safe commit

场景: checkpoint 里的 minTranslogGeneration 字段是干嘛的?
- 语义: 引用的最小代 — Lucene 已 commit 的操作所在代可以删
- 删除判定 (Translog.java:1710-1721 getMinReferencedGen): `min(deletionPolicy.getMinTranslogGenRequiredByLocks(), minGenerationForSeqNo(localCheckpointOfSafeCommit + 1))`
- 两个约束: ① 被 snapshot/recovery 持有锁的代不能删 (TranslogDeletionPolicy.translogRefCounts TranslogDeletionPolicy.java:37) ② safe commit 的 localCheckpoint 之后的操作必须保留
- 关联: 篇 3 展开删除细节

### 5. 截断与损坏 — trimmedAboveSeqNo + tragedy

场景: 日志太长/旧 primaryTerm 操作怎么处理? 损坏了怎么发现?
- **逻辑截断** (Q5): TranslogReader.closeIntoTrimmedReader (TranslogReader.java:71-107) — 新 checkpoint 改 trimmedAboveSeqNo, 不物理删; maxEffectiveSeqNo (Checkpoint.java:110-115) 供 snapshot 过滤
- 触发: IndexShard.trimOperationOfPreviousPrimaryTerms (IndexShard.java:1833) — peer recovery 后旧主操作截断
- **损坏检测**: 每操作带 size+checksum (BufferedChecksumStreamInput); 读失败 → TranslogCorruptedException
- **tragedy 机制** (Q7): 任何 IO 异常 → TragicExceptionHolder 冻结首因 → closeOnTragicEvent 关闭整个 translog (Translog.java:869-889) — "持久化状态不可信就整体失效"
- 关键设计: 为什么一次失败全关? — 部分写盘 = 状态不可信, 继续写掩盖损坏
- **运维视角**: tragedy 触发后 translog 进入失败态 → 上层 Engine 感知 → **shard 标记为 failed** (需 reroute/重建分片), 数据可从副本恢复; 单副本场景需 snapshot 兜底

### 6. 收束 — 与 AOF 恢复对照

- Redis AOF 恢复: 从 base 文件 + 增量文件回放全部命令 (aofManifest)
- ES translog 恢复: 只回放 checkpoint 水位以上的**未提交**操作 — 已提交的由 Lucene 承担
- 对照结论: 恢复范围差异 = 日志角色差异 (AOF 是完整历史, translog 是未提交增量)
- 同构泛化: Kafka 用内存 recovery point (log end offset, 无持久化水位文件) — ES 用磁盘 checkpoint, 因为 ES 恢复的是"未提交操作"必须精确; 对照: Kafka 重放的是消费位点, 不涉及数据重建
- 引出: 篇 3 (generation 怎么轮转/怎么删 — 日志的完整生命周期)

### 核心悬念
"一个不到 512 字节的文件, 凭什么决定整个分片的恢复?" — 因为它是**已确认状态的水位**, 而日志本身只是水位之下的流水。

### 概念依赖链
Q4 恢复流程 → Q5 trimmedAboveSeqNo → Q7 tragedy → (篇 1 的 Location/offset 语义)

### 源码锚点清单
- Checkpoint.java:35-42 (8 字段) / 79-83 (构造断言) / 110-115 (maxEffectiveSeqNo) / 228-229 (<512B 原子写) / 172 (checksum)
- Translog.java:194 (构造 recoverFromFiles) / 225-256 (倒序恢复) / 236 (倒序循环) / 241-242 (逐代 ckp) / 254 (连续性) / 657 (newSnapshot) / 802-838 (trimOperations) / 869-889 (closeOnTragicEvent) / 1710-1721 (getMinReferencedGen)
- TranslogHeader.java:111-158 (UUID 校验)
- TranslogReader.java:71-107 (closeIntoTrimmedReader)
- TranslogDeletionPolicy.java:37 (translogRefCounts) / 66 (acquireTranslogGen) / 111 (getMinTranslogGenRequiredByLocks)
- IndexShard.java:1833 (trimOperationOfPreviousPrimaryTerms)
- TragicExceptionHolder.java:15-33
