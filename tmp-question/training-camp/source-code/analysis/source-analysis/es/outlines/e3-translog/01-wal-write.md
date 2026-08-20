# E-3 Translog 篇 1/3 — 双缓冲写路径: 写入了却还没持久化

> 前置: [[r8-persistence]] (Redis AOF, 对照) | 复用: — | 对照: [[r8-persistence]] [[r28-networking]] | 引出: [[E-3-translog-02]] [[E-3-translog-03]] [[E-1-engine]]
> 🔴 A | 来源: Translog.java:575 + TranslogWriter.java:227,465 + IndexShard.java:3719
> 定位: Translog 卷开篇 — 回答"为什么 add() 不直接写磁盘"

**读者处境**: 你调 `IndexRequest` 写入 ES, 返回 201 成功。但下一秒节点宕机 — 数据还在吗? 面试官问 "ES 写入的可靠性靠什么?" 你答 "translog"。再问 "translog 每次写都 fsync 吗? 那性能怎么扛?" 你卡住了。这篇解决的就是这个问题: 一个 WAL 怎么做到"每个请求都持久化"还不拖垮吞吐。

### 1. 问题引入 — 一条数据从确认到落盘的旅程

场景: bulk 写入 1000 条, 全部 201 — 但 ES 的 Lucene 还没把任何一条刷进磁盘段。
- 两条日志: Lucene 段 (内存 buffer) + translog (顺序追加) — 谁先谁后? (InternalEngine.java:1131 index 入口 → translog.add 先行)
- 核心矛盾: "已确认"的语义 = 数据必须能扛住断电 — 但 fsync 每请求一次, 吞吐掉一个数量级怎么办?

### 2. 双缓冲结构 — buffer 聚合 + 通道批量写

场景: 为什么 add() 只写内存就返回 Location?
- 源码路径: Translog.add (Translog.java:575-598): `writeOperationWithSize` 序列化 → readLock 下 (Translog.java:580) `current.add(bytes, seqNo)` (Translog.java:598)
- TranslogWriter.add (TranslogWriter.java:227-267): `buffer = new ReleasableBytesStreamOutput` — **只写堆内 buffer**, 不动 channel; 偏移记账 `totalOffset += data.length()`; `bufferedBytes >= forceWriteThreshold` 才提前 flush (TranslogWriter.java:229-230)
- **阈值实证**: forceWriteThreshold = bufferSize = **1MB** (TranslogConfig.java:27 DEFAULT_BUFFER_SIZE); 缓冲达 4MB (`forceWriteThreshold * 4` TranslogWriter.java:230) 时**阻塞等待**现有 writer 落盘 — 内存上限 = 4MB/分片, 有界
- 关键设计: 每次 add 记录 `Location(generation, offset, size)` (TranslogWriter.java:256) — 调用方可凭它"要求同步到某偏移"
- 对比: 直接写 channel 的代价 — 每次系统调用 + 无批量机会

### 3. syncUpTo — 真正的落盘: 三条件 + 双检锁

场景: 谁在什么时候把 buffer 刷进磁盘?
- 触发三条件 (TranslogWriter.java:356-360 syncNeeded): `totalOffset != lastSynced.offset || globalCheckpoint 变了 || minTranslogGeneration 变了`
- syncUpTo (TranslogWriter.java:465-534) 流程: syncLock 双检 (TranslogWriter.java:474-477 "double checked locking - we don't want to fsync unless we have to") → writeLock → pollOpsToWrite (调用 TranslogWriter.java:485) → writeAndReleaseOps 写 channel → **channel.force(false)** (TranslogWriter.java:508) → Checkpoint.write 落 ckp 文件
- 关键设计: 双检锁的意义 — 多个线程同时要 sync, 只有第一个真正 fsync, 其余发现已满足直接返回
- **force(false) vs force(true)** (Q3): 文件长度不变场景 force(false) 即可 (元数据已在目录项); 文件创建时 force(true) (Checkpoint.java:186-204: write(ChannelFactory) L186 force(true) L193 / write(FileChannel) L197 force(false) L202)

### 4. durability 双模式 — REQUEST vs ASYNC

场景: "index.translog.durability" 配 REQUEST 和 ASYNC 有什么区别?
- 默认 REQUEST (IndexSettings.java:99-104) — 每请求持久化
- REQUEST 路径: IndexShard.syncAfterWrite (IndexShard.java:3719) → InternalEngine.asyncEnsureTranslogSynced (InternalEngine.java:700) → translogSyncProcessor 串行 → ensureSynced
- **关键设计**: "allows indexing threads to continue indexing without blocking on fsync calls. We ensure that there is only one thread" (IndexShard.java:3713 注释) — 写线程不阻塞在 fsync 上, 只有一个线程被"劫持"执行批量 fsync
- ASYNC 路径: IndexShard.maybeSyncGlobalCheckpoint (IndexShard.java:2818-2837) — 定期推进, localCheckpoint 可能滞后
- **量化**: ASYNC 下 fsync 由 `index.translog.sync_interval` 驱动, 默认 **5s** (IndexSettings.java:87) — 最坏丢 5s 数据; REQUEST 下丢失窗口 ≈ 单次 fsync 时长

### 5. 锁层次 — 为什么 add 只持 readLock

场景: 一个"日志文件"而已, 为什么四层锁?
- TranslogWriter.java:73-75 锁顺序注释 (硬编码防死锁): `writeLock → synchronized(this)` / `syncLock → writeLock → synchronized(this)`
- add: readLock (共享) + synchronized(this) 短临界区 (只动 buffer)
- sync: syncLock 串行 (一次一个 force) + writeLock + synchronized(this)
- rollGeneration/trim: writeLock 独占
- 关键设计: 索引线程几乎零互斥; 持久化线程串行; 结构变更独占

### 6. 收束 — 与 Redis AOF 的第一次对照

- Redis AOF: 每条命令 append 后按 appendfsync (always/everysec/no) — 每请求 fsync vs 批量 fsync 的同类权衡
- ES translog: 双缓冲 + Location 偏移 + 批量 force — "每请求确认"不必然等于"每请求 fsync" (多数请求被前一个请求的 fsync 覆盖)
- 同构泛化: Kafka 页缓存批量 flush / MySQL binlog group commit — "组提交/批量 fsync"是 WAL 设计的通用模式, ES 的双缓冲是其变体
- 引出: 篇 2 (checkpoint 水位, 崩溃后怎么知道回放哪些) — 篇 3 (generation 生命周期与 AOF rewrite 对照)

### 核心悬念
"ES 每次写入都 fsync, 为什么还这么快?" — 答案: 它不每次 fsync, 它让**一批请求共享一次 fsync**。

### 概念依赖链
Q6 锁层次 → Q1 双缓冲 → Q2 syncNeeded 三条件 → Q3 force 语义

### 源码锚点清单
- Translog.java:575-598 (add) / 580 (readLock) / 598 (current.add)
- TranslogWriter.java:227-267 (双缓冲 add) / 229-230 (forceWriteThreshold) / 256 (Location) / 356-360 (syncNeeded 三条件) / 465-534 (syncUpTo) / 474-477 (双检锁) / 508 (force(false)) / 73-75 (锁顺序)
- IndexShard.java:3719 (syncAfterWrite) / 3713 (单线程 fsync 注释) / 2818-2837 (ASYNC)
- InternalEngine.java:700 (asyncEnsureTranslogSynced) / 1131 (index 入口)
- IndexSettings.java:99-104 (durability REQUEST) / 87 (sync_interval 5s) / 358 (flush age 1min)
- Checkpoint.java:186-204 (write force(true/false))
