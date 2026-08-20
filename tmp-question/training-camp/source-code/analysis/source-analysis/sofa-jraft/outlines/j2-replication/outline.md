# J-2 日志复制 — leader 的一对多传送带: 流水线、回退与计票

> 前置: [[J-1-RAFT核心循环]] (leader 语义) + [[Z-2-原子广播]] (ZAB 广播对照) | 引出: [[J-3-快照压缩]] (日志被压缩后) + [[J-4-成员变更]] (waitCaughtUp) | 对照: ZAB 两阶段广播 + Kafka 复制
> 🔴 A | 15 KP | [模式: 流水线 + 序控制 + 计票]
> Pass 2 闭环: q1(Probe) q2(回退) q3(流水线) q4(计票) q5(双写)

**读者处境**: leader 要给 9 个 follower 复制日志, 一个 follower 慢 5 倍会拖垮所有人吗? 响应乱序到达, 怎么保证 nextIndex 不错乱? 日志复制失败的"重试"为什么不退避?

### 1. Probe — 每次失败后的"重新校准"

场景: 复制失败一次后, 为什么先发个空请求而不是重发数据?
源码路径:
- 5 态状态机 (Replicator.java:161-167): Created/Probe/Snapshot/Replicate/Destroyed
- **Probe = 空 AppendEntries** (data 为 ByteString.EMPTY, L810) 试探 prevLogIndex/prevLogTerm 匹配
- →Probe 触发: RPC 失败 (L1448)/响应不匹配 (L1404)/EBUSY (L1463)/term 不匹配 (L1521)/乱序 (L1342)/pending 溢出 (L1289)
- 成功 → 切 Replicate (L1542); 心跳失败也 → Probe (L1209)
关键设计 (q1): **一次往返校准 nextIndex** — 盲目重发会与 follower 来回拉锯; Probe 用空请求拿到"正确的起点"再批量。 [模式: 探测后全速]

### 2. nextIndex 回退 — 两段式: 批量跳 + 逐条减

场景: follower 落后 1000 条, 一条一条回退要 1000 次往返吗?
源码路径:
- nextIndex 初始 = leader.lastLogIndex+1 (L173); 成功 += entriesSize (L1544)
- **两段式回退** (L1493-1509): ① `response.lastLogIndex+1 < nextIndex` → **跳到 follower 末尾+1** (L1498, O(1) 收敛 — follower 只是落后) ② 否则 `nextIndex--` (L1504, 逐条 — follower 有旧 term 日志需截断, 无法推断正确位置)
- 回退后 sendProbeRequest (L1511)
关键设计 (q2): **落后 vs 冲突用不同收敛速度** — 落后的可以大步跳 (响应里带 lastLogIndex), 冲突的必须小步试 (逐条找到 term 分界); 无独立 matchIndex 字段, nextIndex-1 即隐含。 [模式: 双速收敛]

### 3. 流水线复制 — 256 个在途请求与乱序防线

场景: 日志 RPC 不设超时, 响应乱序来了怎么办?
源码路径:
- inflights FIFO (L112); **在途 ≤ maxReplicatorInflightMsgs=256** (RaftOptions.java:60; 判定 L597); sendEntries while 循环狂发 (L1597-1621)
- **seq 排序消费**: reqSeq/requiredNextSeq (L127-129) + pendingResponses 优先队列按 seq (L134, 457-460); 跳号等待 (L1309-1323) — 旧响应不能推进新状态
- **version 代际号** (L131, 1274-1280): reset 后旧响应直接丢弃 (ABA 防线)
- 积压 > 256 → resetInflights + Probe (L1285-1292)
关键设计 (q3): **乱序是异步 RPC 的宿命, 序控制是工程师的活** — 批量发送不等待; seq 保证 commitAt 推进单调; 256 上限防止"慢 follower 吞掉无限请求"。 [模式: 窗口流水线]

### 4. 心跳 = 空 AppendEntries — 一体两面

场景: 心跳和日志复制是两套协议吗?
源码路径:
- 心跳即空 AppendEntries + committedIndex (L788-806); 心跳 RPC 超时 = electionTimeoutMs/2 (L806)
- **日志 RPC 超时 = -1** (L1691) — 活性检测全交给心跳
- fillCommonFields 每次都带 committedIndex (L1576) — follower 提交进度紧跟 leader
- 心跳间隔 = electionTimeout/10 = 100ms (NodeImpl.java:889-891)
- **心跳定时器自重启** (Replicator.java:611-620, startHeartbeatTimer 到期 = startMs + dynamicHeartBeatTimeoutMs); 心跳失败 → Probe (L1209) — RepeatedTimer 实例 (J-6 M2 呼应)
关键设计 (q4): **一份协议两个角色** — 空 AppendEntries 既是"我还活着"也是"提交进度同步"; 日志 RPC 无超时 (有 block 机制兜底), 心跳有超时 (驱动租约判定) — 分工明确。 [模式: 协议复用]

### 5. 请求构造与日志压缩判定 — 16B 指针与哨兵值

场景: prevLogIndex 已经被快照删了, 怎么办?
源码路径:
- fillCommonFields (L1554-1578): prevLogTerm = getTerm(prevLogIndex) (L1555)
- **压缩判定**: prevLogTerm==0 && prevLogIndex!=0 → 非心跳触发快照 (L1556-1560); 心跳置 0 放行 (L1567)
- 条目上限 maxEntriesSize=1024 (L1638-1647); maxBodySize=512KB 分片 (L844-846)
关键设计 (q1): **"查不到 term"是日志被压缩的信号** — 索引存在但 term 为 0, 说明已被截断; 此时逐条复制不可能, 只能整份快照 (J-3 衔接)。 [模式: 哨兵值判定]

### 6. 失败重试 — block 一个心跳周期而非退避

场景: follower 宕机了, leader 的复制请求会怎么失败?
源码路径:
- 失败 → resetInflights + Probe + **block** (L1450): blockTimer 到期 = startMs + 心跳周期 (L1028-1053)
- 恢复 continueSending (L995-1022): ETIMEDOUT → Probe (L1011); 其他 → sendEntries (L1016)
- **无重试次数概念** (注释 L1432-1436: follower 宕机 RPC 立即失败, 不阻塞会死循环)
- 更高 term → destroy + increaseTermTo 让位 (L1469-1483); consecutiveErrorTimes 仅日志节流 (L1443)
关键设计 (q2): **固定周期阻塞 vs 指数退避** — 指数退避是"对方可能瞬时过载"的假设; Raft 里 follower 宕机是常态, 阻塞一个心跳周期既给恢复窗口又同步节拍; 心跳本身的成功即"解除阻塞"。 [模式: 节拍同步重试]

### 7. 计票器 — BallotBox 只认"连续"

场景: 3 节点集群, 一条日志要几个确认才算提交?
源码路径:
- leader 侧唯一计票器 (BallotBox.java:45-56): StampedLock + pendingMetaQueue<Ballot>; O(1) 定位 (L118)
- **commitAt** (L99-143): 逐条 grant (L117-123), 仅**连续**达 quorum 才推进; 锁内计票, 锁外回调 onCommitted (L141)
- Ballot 双 quorum (Ballot.java:80,89,144-146): quorum = n/2+1; 配置变更中新旧都满足 (joint)
- 触发点: Replicator.java:1532-1535 — follower 确认一段连续日志 → commitAt(nextIndex, nextIndex+size-1)
- follower 侧 setLastCommittedIndex 直接采纳 (L230-260): 无 pending + 背压 + 单调
关键设计 (q4): **连续性是安全底线** — 日志 5 已确认但 4 没有, 5 也不能提交 (apply 顺序不可跳); "锁外回调"防 StampedLock 持锁时调 FSM 死锁。 [模式: 连续多数]

### 8. 双写 — 内存立即可见, 磁盘异步落定

场景: 日志写完内存就能读吗? "落盘成功"怎么定义?
源码路径:
- LogManagerImpl (L79-106): 内存 SegmentList + Disruptor 单消费者磁盘队列
- **appendEntries 双写** (L326-382): 写锁内冲突检查 + 内存 addAll (L362) → **锁外** publishEvent (L372-376)
- StableClosure 随事件进入磁盘线程, **落盘成功才 run()** — "回调成功 = 已持久化" (fsync, RaftOptions.sync 默认 true)
关键设计 (q5): **读与写分离的时机** — 内存立即可读 (选举/组装响应), 持久化异步 (不阻塞 leader 主线程); 写锁与 Disruptor 发布分离防持锁阻塞。 [模式: 双缓冲]

### 9. 批量刷盘 — 256 条合并一次 fsync

场景: 高吞吐下 fsync 是瓶颈吗?
源码路径:
- AppendBatcher (L465-519): cap=256 条 / 256KB 上限; endOfBatch 才 flush + setDiskId (L593-596)
- 批内任一失败 → hasError 整体 EIO (L489-493)
- RocksDB WriteBatch 原子批量 (RocksDBLogStorage.java:329-353)
关键设计 (q5): **fsync 摊薄** — 一次 fsync 写 256 条 vs 一条一次, 吞吐差两个数量级; "磁盘水位以批为单位前进"。 [模式: 合并写]

### 10. 冲突处理 — 截断与 appliedId 的底线

场景: 旧 leader 的孤儿日志和 leader 的冲突, 怎么处理?
源码路径:
- **checkAndResolveConflict** (L1045-1105): 重叠区逐条比 term 找冲突点 (L1082-1088) → unsafeTruncateSuffix (L1090-1094) → 追加 leader 条目
- **unsafeTruncateSuffix 禁止越过 appliedId** (FATAL, L1025-1029) — 已应用日志不可截断
- follower gap → EINVAL (L1059-1064); ≤ appliedId 忽略 (L1067-1074)
关键设计 (q4): **日志匹配特性** — (index, term) 相同则内容必然相同 (Raft 核心不变量), 只需从第一个 term 不同的位置截断; appliedId 是物理底线 — 截断已应用日志等于破坏状态机。 [模式: 前缀不变式]

### 11. 快照截断 — 内存立即, 磁盘异步, 且不推进水位

场景: 快照装完, 旧日志什么时候删?
源码路径:
- truncatePrefix (L986-1005): 内存 removeFromFirstWhen (L988) + configManager 同步 + 磁盘 TRUNCATE_PREFIX 异步事件 (L1001-1003)
- **不推进 diskId** (L652-659 注释, braft PR#224): 防快照后内存日志过早清空, follower 拉旧日志落空
关键设计 (q5): **删除延迟 = 数据安全** — 磁盘删除异步做 (不阻塞写); 故意不推进磁盘水位, 保留内存窗口供旧 follower 追赶。 [模式: 延迟删除]

### 12. 编解码 — v1 遗弃, v2 的 learners 与 checksum

场景: 升级后老日志还能读吗?
源码路径:
- AutoDetectDecoder 首字节 0xBB → V2 否则 V1 (AutoDetectDecoder.java:43-47); v1 @Deprecated
- **v2 三改进** (log.proto:17-19): learners/old_learners + 可选 crc64 checksum + protobuf 自描述
关键设计 (q1): **magic 字节在线兼容升级** — 老库升级新库读旧日志; learners 进日志 = 成员信息固化 (J-4 需要); checksum 防静默损坏 (默认关, 性能换安全)。 [模式: 版本化编码]

### 13. RocksDB 存储 — 8 字节索引与双列族

场景: 日志条目在 RocksDB 里长什么样?
源码路径:
- **key = 8 字节大端 index** (L474-478); 双 CF: Configuration + DEFAULT (L227-229)
- **配置日志双写** conf CF (L489-494) — 启动只扫 conf CF 重建 ConfigurationManager, 不需读全部日志 (L241-280)
- sync=true 每批 fsync; truncatePrefix 后台 deleteRange (L586-617); truncateSuffix 同步 (L620-639)
关键设计 (q5): **双列族 = 元数据与数据分离** — 配置变更日志同时落在数据 CF 和 conf CF, 启动 O(配置数) 重建成员历史 (J-4 的 ConfigurationManager 数据源)。 [模式: 双写元数据]

## 代码类型
Architecture (一致性算法核心)

## 负面空间 — SOFAJRaft 日志复制刻意不做的事

- **不做乱序提交**: commitAt 严格连续 (对照 Kafka 的乱序确认优化)
- **不做指数退避**: block 固定心跳周期 (对照 Kafka retry 退避)
- **不做共享复制连接**: 每 peer 独立 replicator + 独立流水线
- **不做多 leader 并行复制**: 单 leader 模型
- **不做日志内联压缩**: 日志不压缩, 压缩交给快照 (J-3)

→ 引出: 日志追不上时怎么办? → J-3 快照压缩
