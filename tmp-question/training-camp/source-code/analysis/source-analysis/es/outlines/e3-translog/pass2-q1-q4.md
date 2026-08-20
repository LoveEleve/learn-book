# E-3 闭环笔记 Q1-Q2: add() 不直接写盘 — 双缓冲与 durability 语义

## Q1: add() 为什么不直接写磁盘?

假设: add() 只写内存 buffer, 真正的 fsync 由请求路径异步触发 — "持久化"不等于"写盘", 而是"fsync 后回调完成"。

验证过程:
- grep `Durability` → IndexSettings.java:99-104: `INDEX_TRANSLOG_DURABILITY_SETTING` 默认 **REQUEST**
- grep durability 消费 → InternalEngine.java:700 `asyncEnsureTranslogSynced` (异步 sync 处理器); IndexShard.java:3723 `syncAfterWrite` — 写操作后调用
- Read TranslogWriter.add (TranslogWriter.java:227-267): `buffer = new ReleasableBytesStreamOutput` — **只写堆内 buffer**, 不碰 channel; `bufferedBytes >= forceWriteThreshold` 才提前 flush
- Read syncUpTo (TranslogWriter.java:465-534): 才 `pollOpsToWrite()` (buffer 转可释放引用) → `writeAndReleaseOps` → **`channel.force(false)`** — 真正落盘
- Read IndexShard.java:3715-3726 注释: "allows indexing threads to continue indexing without blocking on fsync calls. We ensure that there is only one thread blocking on the sync an all others can continue indexing"

代码类型: Implementation (状态生命周期: buffer → channel → disk, 三态)

结论: **add() 写内存 buffer (零锁竞争), sync 时才批量写通道+fsync; REQUEST 模式下 IndexShard.syncAfterWrite 在操作成功后立即排队 ensureSynced, 但同一时刻只有一个线程真正执行 fsync (translogSyncProcessor 串行), 其余线程继续索引 — 这是"每请求持久化 + 不阻塞索引线程"的折中**。TranslogWriter.java:227 + IndexShard.java:3723 + InternalEngine.java:700

## Q2: syncNeeded 为什么检查 globalCheckpoint?

假设: globalCheckpoint 必须随 translog 一起落盘, 因为恢复时要靠它判断"哪些操作已安全"。

验证过程:
- Read TranslogWriter.syncNeeded (TranslogWriter.java:356-360): `totalOffset != lastSyncedCheckpoint.offset || globalCheckpointSupplier.getAsLong() != lastSyncedCheckpoint.globalCheckpoint || minTranslogGenerationSupplier.getAsLong() != lastSyncedCheckpoint.minTranslogGeneration`
- Read syncUpTo (TranslogWriter.java:465-534): checkpointToSync = getCheckpoint() 含 globalCheckpoint; force(false) 后 `Checkpoint.write(checkpointChannel, checkpointPath, checkpointToSync)` — globalCheckpoint 在 ckp 文件里
- Read IndexShard.syncGlobalCheckpoint (IndexShard.java:3732): 独立入口把 globalCheckpoint 落盘
- Read Checkpoint.java:79-81 字段表: globalCheckpoint 是 8 字段之一

代码类型: Interface (契约: 恢复时 globalCheckpoint 不倒退)

结论: **globalCheckpoint 是"所有副本已确认的 seqNo 水位", 若它没随 translog 落盘, 崩溃恢复时可能回放已被确认安全的操作 (或相反) — 所以 ckp 文件必须包含它, syncNeeded 把它与 offset 并列检查**。恢复语义与 E-6 (SeqNo) 强耦合。TranslogWriter.java:356 + IndexShard.java:3752

跨域关联: E-6 SeqNo (globalCheckpoint 语义) / E-5 Shard (recovery 消费)

## 闭环 Q3: force(false) vs force(true)

假设: force(true) 刷元数据 (文件大小/修改时间), force(false) 只刷数据 — 文件大小不变时元数据已在目录项中, 无需重刷。

验证过程:
- Read Checkpoint.write (Checkpoint.java:186-195): 首次创建用 `channel.force(true)` — "fsync with metadata as we use this method when creating the file"
- Read Checkpoint.write(FileChannel) (Checkpoint.java:197-204): 后续写入 `fileChannel.force(false)` — "no need to force metadata, file size stays the same and we did the full fsync when we first created the file"
- Read TranslogWriter.syncUpTo (TranslogWriter.java:508): `channel.force(false)` — translog 文件长度只增不减, 创建时已刷元数据

代码类型: Implementation (性能决策: 少一次元数据 fsync)

结论: **force(false) 是文件长度不变场景的优化 — 目录项 (dentry) 在创建时已持久化, 追加写只需刷数据块; force(true) 仅在文件创建/大小变更时用**。Checkpoint.java:205-224 + TranslogWriter.java:519

## 闭环 Q4: 恢复为什么倒序开文件?

假设: 先验证最新代 (checkpoint 指向的代) 的 UUID/格式, 再往前追溯 — 错误信息质量更好。

验证过程:
- Read recoverFromFiles (Translog.java:225-256): `for (long i = checkpoint.generation; i >= minGenerationToRecoverFrom; i--)` + 注释 "we open files in reverse order in order to validate the translog uuid before we start traversing the translog based on the generation id we found in the lucene commit. This gives for better error messages if the wrong translog was found"
- Read openReader 路径: 每代读 translog-gen.ckp (Translog.java:241-242) 或主 ckp
- 注释明确: "This gives for better error messages if the wrong translog was found" — 若 UUID 不匹配, 最新代先抛错

代码类型: Interface (恢复契约: 代必须连续, 反序校验)

结论: **倒序打开让"UUID 校验"最早发生在最新代 — 若 translog 属于别的 engine (UUID 不匹配), 立即报错而非遍历半天; 同时 `translog ids must be consecutive` (L249-251) 断言保证代连续**。Translog.java:230-260

跨域关联: E-1 Engine (TRANSLOG_UUID_KEY 在 lucene commit, 2.0 引入)
