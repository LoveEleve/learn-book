# Z-9 持久化 — 双文件格式与恢复双路径

> 前置: [[Z-3-DataTree]] (树序列化) + [[Z-4-Processor链]] (Sync 刷盘+snapCount) + [[Z-5-Session]] (会话恢复) + [[Z-2-原子广播]] (TRUNC/DIFF/SNAP) | 对照: ES Translog + Redis AOF (阶段3)
> 🔴 A | 8 KP | [模式: WAL + 快照 + 双路径恢复]
> Pass 2 闭环: q1(TxnLog 格式) q2(快照格式) q3(restore 双路径) q4(清理运维)

**读者处境**: ZooKeeper 怎么把状态落盘? 崩溃后怎么恢复到最新? 这篇拆 FileTxnLog (WAL) + FileSnap (快照) + FileTxnSnapLog (恢复编排) + PurgeTxnLog (清理)。

### 1. TxnLog — 事务日志格式与写路径

场景: 事务怎么持久化?
源码路径:
- **文件格式** (FileTxnLog:60-96): FileHeader [magic "ZKLG" 4B + version 4B + dbid 8B] + 记录链 **[CRC 8B Adler32][len 4B][TxnHeader+Record(+digest)][0x42 'B']** + ZeroPad; 文件名 log.\<zxid hex\> (Util:84-86)
- **CRC 覆盖实证**: `crc.update(buf)` 只覆盖 payload (L316-317) — ⚠ **Javadoc L77-78 声称覆盖 len+0x42 与实现不符**; 读侧比对失败 → "CRC check failed" (L799-800)
- **预分配**: FilePadding preAllocSize 默认 **65536KB=64MB** (FilePadding:30), position+4096 ≥ fileSize 才 pad (L101-115) — 尾部 ZeroPad 读到即 EOF
- **append 幂等守卫**: zxid ≤ lastZxidSeen → **只 warn 不拒** (L281-289)
- **commit** (L394-443): flush → filePosition += unFlushedSize → **channel.force(false) (forceSync 默认 yes)** → fsync 超 1000ms 告警 + **FSYNC_TIME 指标** (L407-428) → 关旧流留新 (streamsToFlush 保留最新, L430-432) → txnLogSizeLimit (默认 -1 禁用, 3.9) 超限 rollLog
- **读侧容错二分**: **EOFException → 静默跳下一文件 (尾部残缺容忍 — 预分配语义)** (FileTxnIterator:806-818) vs **CRC_ERROR → IOException 致命 (中部损坏)**; 空尾文件自动删除恢复 (L715-737)
关键设计 (q1): **CRC+len+0x42 定长记录链 + 64MB 预分配**; 尾部残缺=EOF, 中部损坏=致命。[模式: WAL]

### 2. FileSnap — 快照格式与回退

场景: 全量状态怎么落盘?
源码路径:
- **文件格式** (FileSnap:136-142): magic "ZKSN" + version 2 + **dbid=-1 常量** (L51 — 与 txnlog 动态 dbid 不对称); 文件名 snapshot.\<zxid hex\>\[.gz\|.snappy\] (Util:94-98)
- **seal 三段** (FileSnap:250-267 + SnapStream:162-180): 每段 **writeLong(Adler32) + writeString("/")** — 树 / zxidDigest / lastProcessedZxid 独立 CRC — **向后兼容 (老版本无后两段, deserialize 双分支 L99-106)**
- **有效判定** (SnapStream:193-211,298-327): CHECKED 模式读**尾部 5 字节 = len 1 + '/'**; GZIP/SNAPPY 读 magic; 文件 ≥10 字节
- **回退面** (FileSnap:73-126): **findNValidSnapshots(100)** 最多回退 100 个; 全失败 → "Not able to find valid snapshots"; ⚠ **重试残留面**: DataTree.deserialize 只清 nodes/pTrie/nodeDataSize (DataTree:1347), **ephemerals/containers/ttls/aclCache 不清** → 中途失败 (父缺失 IOException) 残留合并进下一快照 (zombie 面)
- **原子写** (SnapStream:132-154): **fsync=true → AtomicFileOutputStream (临时文件+rename)**; save 失败空文件删除 (FileTxnSnapLog:485-495, full disk 循环防护)
- **fuzzy 容错** (FileTxnSnapLog:445-453): 快照中后期事务混入 → restore 时 NONODE/NODEEXISTS 安全忽略
关键设计 (q2): **树+三段独立 CRC seal**; 快照损坏可回退 100 代。[模式: 快照+回退]

### 3. restore — 双路径恢复与边界

场景: 崩溃后怎么恢复到最新?
源码路径:
- **主路径** (FileTxnSnapLog:252-313): deserialize 快照 → **fastForwardFromEdits: read(lastProcessedZxid+1)** (L330 — 严格从快照 zxid+1 重放, fuzzy 快照语义) → 每事务 processTransaction + **compareDigest** (L351)
- **目录**: dataDir/snapDir = \<dir\>**/version-2** (L112-117); **交叉污染检查** (L180-204, ZOOKEEPER-2967): log 目录有 snapshot / snap 目录有 log → 拒绝启动 — ⚠ **仅 dataDir≠snapDir 时执行** (L164, 3.4 单目录布局兼容)
- **无快照面** (L283-310): 无快照有日志 → **默认 throw "Something is broken!" (ZOOKEEPER-2325)**; **trustEmptySnapshot (ZOOKEEPER-3056 3.4.x 升级逃生舱)** 放行; 空库 → trustEmptyDB (initialize 文件) → **save 空快照 → return 0**; ⚠ getLastLoggedZxid 全文件扫描判定 (FileTxnLog:369-388) — 启动成本面
- **zxid 回退检测** (L344-347) + **digest 覆盖警告** (L272-279): 重放未覆盖快照 zxid → "might lead to inconsistent state"
- **truncate** (FileTxnLog:481-501): **setLength 截断到 < zxid (exclusive)** + 删后续文件; 触发 (LearnerHandler:842-846): learner 领先 → **TRUNC zxid = maxCommittedLog** (Z-2 交叉); ⚠ **不完全截断边沿**: 无 <zxid 日志文件 (purge 删旧) → 首个 ≥zxid 事务幸存 — 靠 fuzzy 容错兜底
关键设计 (q3): **快照打底 + zxid+1 重放 + digest 校验**; 无快照/空库有逃生舱语义。[模式: 双路径恢复]

### 4. 清理与运维 — PurgeTxnLog + 尺寸 + 压缩

场景: 磁盘怎么治理?
源码路径:
- **PurgeTxnLog.purge** (PurgeTxnLog:60-73): **num ≥ 3 硬约束**; 保留 N 个有效快照 + 删除更旧 log/snapshot, **但 getSnapshotLogs(leastZxidToBeRetain) 保留** (L79-166) — **log.(X-a) 可能含 >X 事务 → 删之则快照 X 不可恢复**; DatadirCleanupManager 定时 (autopurge)
- **尺寸双面**: snapCount 默认 100000 (ZooKeeperServer:224) → logCount > snapCount/2 + randRoll 触发快照 (SyncRequestProcessor:146-151, Z-4) / txnLogSizeLimitInKb **默认 -1** (FileTxnLog:122-147) / calculateTxnLogSizeLimit = snapshotSizeFactor × 最新快照 (ZKDatabase:365-376 — learner 同步限流, Z-2); ⚠ **学习者起点守卫**: getProposalsFromTxnLog **fastForward=false + 首事务 zxid 守卫 + sizeLimit** (ZKDatabase:386-420) — 整文件连续才可重放
- **压缩**: StreamMode **GZIP/SNAPPY/CHECKED 默认 CHECKED** (SnapStream:64-93) — 扩展名驱动读侧
- **运维工具**: TxnLogToolkit (447) — 转储/修复日志 CLI
关键设计 (q4): **保留 N≥3 + 可恢复性守卫** (跨界日志必须留)。[模式: 治理]

## 代码类型
Architecture (存储引擎)

## 负面空间 — 持久化刻意不做的事

- **不做单文件数据库**: 快照+WAL 双文件, 无索引/无合并 (对照 ES Translog + Lucene / Redis RDB+AOF)
- **不做事务内联落盘**: append 缓冲, commit 批量 force — **tick 粒度批量刷盘** (Sync 处理器, Z-4)
- **不做组提交优化**: 无 group commit — fsync 每次 commit 串行 (对照 Kafka logSegment 组提交面)
- **不做日志压缩/加密**: txnlog 明文 + 无压缩 (快照才可选压缩) (对照 Redis AOF rewrite)
- **不做中部损坏自愈**: 尾部残缺容忍但中部 CRC 致命 — 需 TxnLogToolkit 人工修复
- **不做无限保留**: PurgeTxnLog 最小保留 3 — 快照间隔窗口 (snapCount/2~snapCount) 决定丢失窗口上界

→ 引出: 跨存储对照? → ES Translog (阶段3) + Redis AOF (阶段3.5)
