# J-2 日志复制 — 知识规划 (KP)

> 域级: 🔴 A | 模块: core/Replicator (1930) + ReplicatorGroupImpl (315) + BallotBox (294) + entity/Ballot (147) + storage/impl/LogManagerImpl (1254) + entity/codec (v1/v2) + storage/impl/RocksDBLogStorage (769)
> 日期: 2026-08-15 | 版本: 1.4.1

## 一、机制提取 (逐源)

### M1 Replicator 状态机 (5 态 + 3 层概念)
- 对外状态: Created/Probe/Snapshot/Replicate/Destroyed (Replicator.java:161-167); 内部运行态 IDLE/BLOCKING/APPENDING_ENTRIES/INSTALLING_SNAPSHOT (L220-225); 监听器态 CREATED/DESTROYED/ONLINE/OFFLINE (L249-266)
- **Probe 是核心退化路径**: 任何失败 → Probe (发空 AppendEntries 探测 prevLogIndex 匹配点, L810 空 data) → 成功切 Replicate (L1542)
- →Probe 触发: RPC 失败 (L1448)/响应不匹配 (L1404)/EBUSY (L1463)/term 不匹配 (L1521)/心跳失败 (L1209)/响应乱序 (L1342)/pending 溢出 (L1289)
- →Snapshot: prevLogIndex 被压缩 (L778, L1633, L1650)

### M2 nextIndex/matchIndex 与冲突回退
- nextIndex 初始 = lastLogIndex+1 (L173); 成功推进 += entriesSize (L1544); 快照后 = lastIncludedIndex+1 (L740)
- **两段式回退** (L1493-1509): ① response.lastLogIndex+1 < nextIndex → 批量跳到跟随者末尾+1 (L1498, O(1) 收敛) ② 否则 nextIndex-- (L1504, 逐条递减 — 旧 term 冲突无法推断正确位置)
- 无独立 matchIndex 字段: nextIndex-1 隐含 matchIndex
- 回退后 sendProbeRequest (L1511) — 不连续轰炸

### M3 流水线复制
- inflights ArrayDeque FIFO (L112); **在途上限 maxReplicatorInflightMsgs=256** (RaftOptions.java:60, 判定 L597)
- sendEntries while 循环 (L1597-1621) — 只要在途 < 256 继续发
- **乱序响应**: reqSeq/requiredNextSeq (L127-129) + pendingResponses PriorityQueue 按 seq (L134, L457-460); 跳号等待 (L1309-1323); version 代际号丢弃 reset 前旧响应 (L131, L1274-1280)
- 响应积压 > 256 → resetInflights + Probe (L1285-1292)

### M4 心跳与复制
- 心跳 = 空 AppendEntries + committedIndex (L788-806); 心跳 RPC 超时 = electionTimeoutMs/2 (L806)
- 心跳定时器自重启 (L611-620); 失败 → Probe (L1209)
- **日志 RPC 超时 = -1** (L1691) — 活性检测全由心跳承担
- 心跳间隔 = max(electionTimeout/10, 10) = 100ms (NodeImpl.java:889-891)

### M5 请求构造
- fillCommonFields (L1554-1578): prevLogTerm = getTerm(prevLogIndex) (L1555); **prevLogTerm==0 && prevLogIndex!=0 = 已压缩** → 非心跳触发快照 (L1556-1560), 心跳置 0 放行 (L1567); committedIndex 每次携带 (L1576)
- sendEntries (L1629-1709): 上限 maxEntriesSize=1024 (L1638-1647); maxBodySize=512KB 分片 (L844-846)
- entry 序列化 prepareEntry (L842-871): term/checksum/type/peers/learners

### M6 快照安装
- 触发 3 点: 探测时 fillCommonFields 返回 false (L776-784)/发送时 (L1631-1635)/nextSendingIndex < firstLogIndex (L1649-1652)
- **URI 独立通道传输** (L649 generateURIForCopy — 快照文件流, 非 RPC 大包)
- 成功: nextIndex = lastIncludedIndex+1 (L740) → Replicate; **失败不显式重试** (L744 注释) — block 等心跳周期再评估

### M7 失败重试 block 机制
- **无 appendEntriesTimeoutMs、无重试计数**: 失败 → resetInflights + Probe + **block 一个心跳周期** (L1450, L1028-1053, blockTimer = startMs + dynamicHeartBeatTimeoutMs)
- 恢复: onBlockTimeout → continueSending (L995-1022): ETIMEDOUT → Probe (L1011); 其他 → sendEntries (L1016)
- 更高 term → destroy + node.increaseTermTo 主动让位 (L1469-1483)
- consecutiveErrorTimes 仅日志节流 (L1443, 每 10 次 warn)
- 设计: 失败阻塞而非指数退避 — 防 follower 宕机时死循环轰炸 (L1432-1436 注释)

### M8 ReplicatorGroupImpl 管理
- replicatorMap + **failureReplicators** (L57-63); addReplicator 先 checkConnection 失败登记 (L124-127); **checkReplicator 惰性补建** (L179-200)
- waitCaughtUp (L147-156, 成员变更/领导权转移预同步); findTheNextCandidate 按 nextIndex 优先 (L273-301)
- becomeLeader 全建 (NodeImpl.java:1280-1298), learner 用 ReplicatorType.Learner (L1295)
- **quorum 提交触发**: Replicator.java:1532-1535 — isFollower() 才 ballotBox.commitAt(nextIndex, nextIndex+entriesSize-1, peerId)

### M9 learner 不参与计票
- ReplicatorType Follower/Learner (ReplicatorType.java:24-33); commitAt 仅 isFollower (L1532-1535)
- learner 全量复制但不进投票集 — 故障不拖垮 quorum; waitCaughtUp 追平后可提升为正式成员 (NodeImpl.java:403-410)

### M10 BallotBox 计票器 (leader 侧)
- StampedLock + lastCommittedIndex + pendingIndex + pendingMetaQueue SegmentList<Ballot> (BallotBox.java:45-56); O(1) 定位 = logIndex - pendingIndex (L118)
- **appendPendingTask** (L203-221): 复制前登记 Ballot + Closure (closureQueue.appendPendingClosure L216)
- **commitAt** (L99-143): 逐条 grant, 仅**连续**达 quorum 才推进 (L117-123); 锁内计票锁外回调 waiter.onCommitted (L141)
- **Ballot 双 quorum** (Ballot.java:80,89,144-146): quorum = size/2+1; 旧配置存在时新旧都满足 = joint consensus
- follower 侧 setLastCommittedIndex 直接采纳 (L230-260): 无 pending + 背压 (hasAvailableCapacity) + 单调
- resetPendingIndex (L172-192): 新 leader 计票起点 = lastLogIndex+1 — 本任期提交能力
- clearPendingTasks (L151-160): leader 退位未提交任务立即失败

### M11 LogManagerImpl 双写
- 内存 SegmentList + 磁盘 Disruptor 单消费者 (L79-106); TimeoutBlockingWaitStrategy (L222-223)
- **appendEntries 双写** (L326-382): 写锁内 checkAndResolveConflict + 内存 addAll (L362) → 锁外 publishEvent 磁盘 (L372-376); done 随事件, 落盘成功才 run() — "回调成功 = 已持久化"
- 读路径分层 (L771-797): 内存优先, miss 落盘; checksum 损坏 → reportError + 抛异常 (L789-795)

### M12 AppendBatcher 批量刷盘
- cap=256 条 / maxAppendBufferSize=256KB (L465-519, RaftOptions.java:42); endOfBatch 才 flush + setDiskId (L593-596)
- 批内任一失败 → hasError 整体 EIO (L489-493)

### M13 冲突处理与截断
- **checkAndResolveConflict** (L1045-1105): leader 自增 (L1047-1054); follower gap → EINVAL (L1059-1064); ≤ appliedId 忽略 (L1067-1074); 重叠逐条比 term 找冲突点 (L1082-1088) → unsafeTruncateSuffix (L1090-1094)
- unsafeTruncateSuffix 禁止越过 appliedId (FATAL, L1025-1029)
- **truncatePrefix** (L986-1005): 内存立即生效, 磁盘 TRUNCATE_PREFIX 事件异步删 (L1001-1003); **不推进 diskId** (L652-659 注释, braft PR#224 — 防快照后 follower 拉旧日志落空)
- 落盘 fsync: raftOptions.sync 默认 true (RaftOptions.java:51-52; RocksDBLogStorage.java:205-206)

### M14 v1/v2 编解码
- 默认 LogEntryV2CodecFactory (DefaultJRaftServiceFactory.java:69-71); AutoDetectDecoder 首字节 0xBB → V2 (AutoDetectDecoder.java:43-47); v1 @Deprecated (L28)
- **v2 三改进**: learners/old_learners (log.proto:18-19) + 可选 checksum crc64 (L17, LogEntry.java:113-123) + protobuf 自描述 (V2Encoder.java:81-111); 头部 2 magic + 1 version + 3 reserved (L45-51)
- v1 无版本号/无校验/无 learners (V1Encoder.java:44-129)

### M15 RocksDB 存储
- **8 字节大端 index 作 key** (RocksDBLogStorage.java:474-478); 双 CF: Configuration + DEFAULT (L227-229); 配置日志双写 (L489-494) — 启动扫 conf CF 重建 ConfigurationManager 不需读全部日志 (L241-280)
- WriteBatch 原子批量 (L329-353); sync=true 每批 fsync
- truncatePrefix 后台 deleteRange (L586-617); truncateSuffix 同步 (L620-639); reset destroyDB 重建 (L642-671)
- hooks (L673-750): RocksDBSegmentLogStorage 子类拆分段文件 (J-5)

## 二、聚合与分级

| 机制 | 级别 | 理由 |
|---|---|---|
| M2 冲突回退两段式 | P1 | 面试必问; 收敛效率 |
| M3 流水线 + 乱序排序 | P1 | 性能核心; 高并发设计 |
| M10 BallotBox 连续提交 | P1 | 提交安全 |
| M7 block 重试 | P1 | 与指数退避的对照 |
| M13 冲突截断 + appliedId 保护 | P1 | 日志一致性 |
| M4 心跳一体化 | P1 | 租约与提交传播 |
| M11 双写 | P2 | 架构 |
| M12 批量刷盘 | P2 | 性能 |
| M6 快照触发 | P2 | J-3 衔接 |
| M9 learner | P2 | 特性 |
| M14/M15 编解码/存储 | P2 | 实现面 |

## 三、负面空间

- **不做乱序提交**: 计票严格按索引连续推进 (commitAt L117-123)
- **不做指数退避重试**: block 固定一个心跳周期 (对照 Kafka 重试)
- **不做多路复用复用**: 每个 peer 独立 replicator
- **不做写放大优化外的并发控制**: 单写线程落盘
- **不做日志级压缩**: 压缩是快照的事 (J-3)
