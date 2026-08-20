# E-3 Translog — 六层深审 REVIEW 记录 (2026-08-14)

> 审查方法: 07 五维度 + 每锚点 awk/sed 精确核对 + 机制逐字对照 + 裸行号全量扫描 + 跨域引用 [ -d ] 核验
> **结论: 深审未通过 — 23 项行号偏差 + 1 处机制错误 + 1 处编造, 全部修复后复审**

## 第一层: 行号偏差 (23 项 — 全部 awk/sed 实证)

| # | 文件 | 原写 | 实测 | 偏差 |
|:--:|---|---|---|:--:|
| 1 | 01:19 | TranslogWriter.add (L227-268) | L227-267 (add 至 return) | 范围 |
| 2 | 01:21 | Location (L263) | **L256** (`location = new Translog.Location` L256) | -7 |
| 3 | 01:28 | syncUpTo (L463-534) | **L465** 起 (`final boolean syncUpTo` L465) | -2 |
| 4 | 01:28 | force(false) (L519) | **L508** | -11 |
| 5 | 01:28 | Checkpoint.write (L521) | Checkpoint 写盘在 syncUpTo L510-511 区 (实际 L521 附近是 lastSyncedCheckpoint 赋值) | 语义 |
| 6 | 01:36 | syncAfterWrite (L3723) | **L3719** | -4 |
| 7 | 01:36 | 单线程 fsync 注释 (L3724-3726) | **L3713** ("only one thread") | -11 |
| 8 | 01:38 | maybeSyncGlobalCheckpoint (L2825-2837) | **L2818** 起 | -7 |
| 9 | 01:43 | durability (IndexSettings L99-104) | L99-104 ✅ 正确 | ✅ |
| 10 | 02:20 | checksumEntireFile (L150-160) | **L172** | +12 |
| 11 | 02:26 | 构造器 recoverFromFiles (L194) | **L194** ✅ (`this.readers.addAll(recoverFromFiles(checkpoint))`) | ✅ |
| 12 | 02:27 | 倒序打开 (L230-260) | **L229-256** (for 循环在 L236) | 范围 |
| 13 | 02:27 | 错误消息注释 (L245-247) | 注释 L233-235, 实际 for 循环 L236 | 偏移 |
| 14 | 02:28 | 逐代 ckp (L236) | **L241-242** (`Checkpoint.read(getCommitCheckpointFileName(i))`) | +5 |
| 15 | 02:29 | 连续性断言 (L249-251) | **L254** ("translog ids must be consecutive") | +3 |
| 16 | 02:45 | trimOperationOfPreviousPrimaryTerms (L1834) | **L1833** | -1 |
| 17 | 02:47 | closeOnTragicEvent (L869-889) | **L869** 起 ✅ (方法体 869-889) | ✅ |
| 18 | 03:18 | rollGeneration (L1628-1652) | **L1628** 起 ✅; syncBeforeRollGeneration 调用 L1629 (写 1630) | -1 |
| 19 | 03:18 | closeIntoReader (L1635) | **L1636** | +1 |
| 20 | 03:18 | copyCheckpointTo (L1638) | **L1639** | +1 |
| 21 | 03:18 | createWriter (L1640) | **L1641** | +1 |
| 22 | 03:19 | 空代跳过 (L1632) | **L1630** | -2 |
| 23 | 03:21 | 先 sync (L1633) | **L1629** (syncBeforeRollGeneration 调用) | -4 |
| 24 | 03:29 | trimUnreferencedReaders (L1671-1728) | **L1661** 起 | -10 |
| 25 | 03:30 | 删前 sync (L1714) | **L1699** (`current.sync()`) | -15 |
| 26 | 03:36 | flush (L2173) | L2173 ✅ | ✅ |
| 27 | 03:38 | afterWriteOperation (L3764) | **L3763** | -1 |
| 28 | 03:38 | shouldPeriodicallyFlushAfterBigMerge (L2834) | **L2834** ✅ | ✅ |
| 29 | temporal-trace:22 | afterWriteOperation (L3764) | L3763 | -1 |
| 30 | temporal-trace:23 | CAS 单飞 (L3774-3790) | **L3765-3795** 区 (flushOrRollRunning L3761, 提交在 3774-3790) | 范围 |
| 31 | temporal-trace:34 | TranslogHeader.read (L137-158) | **L111** 起 (read 方法) | -26 |
| 32 | pass1:32 | recoverFromFiles (L230) | L229 起 (方法声明) | -1 |
| 33 | pass1:40 | maxEffectiveSeqNo (L122-130) | **L110-115** | -12 |
| 34 | pass2-q1:11 | syncUpTo (L463-534) | L465 起 | -2 |
| 35 | pass2-q1:25 | syncGlobalCheckpoint (L3748-3755) | **L3732** 起 (公开方法) | -16 |
| 36 | pass2-q1:39 | Checkpoint.write force(true) (L205-215) | **L186-195** (write 方法), force(true) L193 | 范围 |
| 37 | pass2-q1:40 | Checkpoint.write(FileChannel) (L217-224) | **L197-204**, force(false) L202 | 范围 |
| 38 | pass2-q1:41 | syncUpTo force(false) (L519) | L508 | -11 |
| 39 | pass2-q1:52 | recoverFromFiles (L230-260) | L229-256 | 范围 |
| 40 | pass2-q1:53 | 逐代 ckp (L236) | L241-242 | +5 |
| 41 | pass2-q4:52 | 同上 | 同上 | — |
| 42 | pass2-q5:12 | trimOperations 断言 (L807-808) | **L803** (assert aboveSeqNo), term 校验 L807-809 | 范围 |
| 43 | pass2-q5:26 | Translog.add readLock (L580-598) | L580 readLock ✅ / L598 current.add ✅ | ✅ |
| 44 | pass2-q6:27 | rollGeneration writeLock (L1628-1652) | L1628 起 ✅ | ✅ |
| 45 | pass2-q6:29 | TranslogWriter.add synchronized (L234) | **L234** ✅ | ✅ |
| 46 | pass2-q7:40 | TragicExceptionHolder (L15-33) | setTragicException L18, compareAndSet L20 | 范围 |
| 47 | pass2-q8:11 | TranslogDeletionPolicy (L24-36 引用计数) | translogRefCounts **L37**, acquireTranslogGen **L66** | 差 30 |
| 48 | pass2-q8:13 | AOF 触发 (server.c:1434-1440) | **L1434-1440** ✅ | ✅ |
| 49 | completeness | 全部 ✅ 已实证 (1MB/5s/64MB) | ✅ | ✅ |
| 50 | temporal-trace:20 | v0.90 TranslogService 阈值 (L77-80) | L77-80 ✅ | ✅ |

## 第二层: 机制错误 (1 项)

| # | 文件 | 错误 | 实测 |
|:--:|---|---|---|
| 1 | pass1:37 (Q2) | "syncNeeded 三条件... minTranslogGeneration 变了" | ✅ 实际正确 (L357-360) |
| 2 | 01:28 | "syncUpTo 流程: pollOpsToWrite (L530-540)" | pollOpsToWrite 是 **private synchronized 方法** 定义在 L528-537 区, 调用在 L485 — 大纲把定义行当流程行 |
| 3 | 02:20 | "格式: Lucene CodecUtil header/footer + CRC 校验" | ✅ 正确但行号错 (L172) |

## 第三层: 编造 (1 项)

| # | 文件 | 编造 | 实证 |
|:--:|---|---|---|
| 1 | temporal-trace:16 | "v5.0: TranslogDeletionPolicy 出现" | **v5.0.0-alpha1 的 translog 目录无 TranslogDeletionPolicy** (ls-tree 实证 14 文件无此名); 首次引入 = commit 1775e4253eb (2017-06-01, "Introducing a translog deletion policy #24950") — 属 **6.x** 时代 |

## 第四层: 覆盖缺口 (2 项)

| # | 缺口 | 建议 |
|:--:|---|---|
| 1 | 01 未讲 Operation 序列化格式细节 (size+checksum 头) | completeness Q4 已标 ⚠️ — 02-L5 补一句即可 |
| 2 | 03 未讲 flush 与 checkpoint minTranslogGeneration 联动 | completeness Q28 ⚠️ — 补 03-L4 一句 |

## 第五层: 裸行号违规 (铁律 9 — 全域扫描)

- 扫描命令: `grep -rnE '\(L[0-9]+' es/outlines/e3-translog/*.md | grep -v java:`
- **结果: 50+ 处 `(Lxxx)` 简写** — 违反"每行号独立 `(File.java:line)` 格式"
- 根治: 全部改写为 `(Translog.java:575-598)` 格式, 文件名前置

## 第六层: 跨域引用核验 (06 §2)

- 全部 10 个引用目录 [ -d ] 验证 ✅ (r8-persistence/r28-networking/rd3-codec/rd1-connection/s88-boot-elasticsearch/h13-datasource/m7-cache/s19-cacheable/s77-boot-cache/rd6-localcachedmap)
- 对照声明"同词+一句摘要"检查: 01-L6 (AOF 对照 ✓) / 02-L6 (AOF 恢复 ✓) / 03-L6 (AOF rewrite ✓) — 均有同词 + 摘要 ✅
- 双链四行 header: 3 篇均有 前置/复用/对照/引出 ✅

## 修复执行 (全部 6 个文件)

- [x] 01-wal-write.md — 全部行号改 (File.java:line), 修正 L256/L465/L508/L3719/L3713/L2818
- [x] 02-checkpoint-recovery.md — 修正 L172/L229-256/L233-235/L236→L241-242/L254/L1833
- [x] 03-generation-lifecycle.md — 修正 L1629/L1630/L1636/L1639/L1641/L1661/L1699/L3763
- [x] pass1-notes.md — 修正 L229/L110-115
- [x] pass2-q1-q4.md — 修正 L465/L3732/L186-195/L197-204/L508
- [x] pass2-q5-q8.md — 修正 L803/L66/L37
- [x] temporal-trace.md — 修正 v5.0 编造 (TranslogDeletionPolicy 属 6.x) + L111/L3763
- [x] completeness-questions.md — 保持 (已实证)

---

## 第二轮复审 (REVIEW-2, 2026-08-14) — 修复本身的验证

> 目的: 第一轮修复是否精确落地 + 修复过程是否引入新错误

### 发现 5 项 (第一轮修复本身不精确)

| # | 文件 | 第一轮修成 | 实测 (第二轮) | 修复 |
|:--:|---|---|---|---|
| 1 | 02 | recoverFromFiles (L229-256) | 方法声明 **L225** | ✅ 已改 225-256 |
| 2 | 02 | Checkpoint 字段 (L33-41) | 字段 **L35-42** (L33-34 是 class 声明+空行) | ✅ 已改 35-42 |
| 3 | 03 | 删前 sync (L1699) | **L1695** | ✅ 已改 1695 |
| 4 | 03 | 容错注释 (L185-194) | **语义错位**: L185-194 是构造器"删除未提交 next 代"逻辑; 真正"ckp 已更新但 tlog 未删"注释在 **L1690-1694** | ✅ 已改 1690-1694 + 引用原文 |
| 5 | 03 | shouldPeriodicallyFlush (L2121) | L2121 是 write 阈值计算行, 方法声明 **L2129** | ✅ 已改 2116-2123(计算)+2129(声明) |

### 已验证正确 (抽查 20 项全过)

- TranslogWriter.java:256 (Location) / 465 (syncUpTo) / 508 (force) / 73-75 (锁注释) ✅
- IndexShard.java:3719 (syncAfterWrite) / 3713 (注释) / 2818-2845 (maybeSyncGlobalCheckpoint) / 1833 (trimOperation) / 3763 (afterWriteOperation) ✅
- InternalEngine.java:700 / 1131 / 2173 / 2834 ✅
- Translog.java:1628-1652 (rollGeneration 内 1629/1630/1636/1639/1641 全对) / 1661 (trimUnreferencedReaders) / 1710 (getMinReferencedGen) / 657 (newSnapshot) / 194 (构造) / 236 (倒序循环) / 241-242 (逐代 ckp) / 254 (连续性) / 869 (closeOnTragicEvent) ✅
- Checkpoint.java:110-115 (maxEffectiveSeqNo) / 172 (checksum) / 228-229 (<512B) / 186-204 (write) ✅
- TranslogHeader.java:111 (read) / TranslogReader.java:71 (closeIntoTrimmedReader) / TranslogDeletionPolicy.java:37,66,111 ✅
- IndexSettings.java:87 (5s) / 99-104 (REQUEST) / 358 (1min) / 385 (64MB) ✅

### 结论

- 第一轮修复"方向对但精度不足": 3 处仍偏 4 行 (L229→225, L1699→1695), 1 处语义错位, 1 处声明/计算行混淆
- **根因**: 第一轮修复用 sed 批量替换, 部分行号凭记忆推算未逐点 grep — 违反铁律 2
- **根治方案升级**: 修复后必须**再次**逐锚点 grep 验证 (第二轮即为此), 不能信任批量替换结果
