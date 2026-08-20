# J-1 RAFT 核心循环 — 知识规划 (KP)

> 域级: 🔴 A | 模块: jraft-core/.../core/ NodeImpl (3701) + State (37) + TimerManager (71) + RepeatedTimer (295) + ReadOnlyServiceImpl (473) + FSMCallerImpl (801)
> 日期: 2026-08-15 | 版本: 1.4.1

## 一、机制提取 (逐源)

### M1 状态机与定时器 (State/TimerManager/RepeatedTimer)
- **8 态枚举** (State.java:25-34): LEADER < TRANSFERRING < CANDIDATE < FOLLOWER < ERROR < UNINITIALIZED < SHUTTING < SHUTDOWN; isActive = ordinal < ERROR (L36-38)
- **RepeatedTimer 底座** (RepeatedTimer.java:38-295): 基于 HashedWheelTimer (L57); run() 触发后自动重排 (L83-107); restart/reset/stop (L162-274); **adjustTimeout 每次调度前调用实现随机化** (L187)
- **四个定时器** (NodeImpl.java:926-991): voteTimer (L929)/electionTimer (L943)/stepDownTimer (**间隔 = electionTimeoutMs >> 1 = 500ms**, L957)/snapshotTimer (L966)
- **随机选举超时**: randomTimeout = [timeoutMs, timeoutMs + maxElectionDelayMs) (NodeImpl.java:893-895, 默认 [1000,2000)ms) — 防投票风暴
- 心跳间隔 = max(electionTimeout / electionHeartbeatFactor, 10) = **100ms** (NodeImpl.java:889-891)
- 关键默认值: electionTimeoutMs=**1000ms** (NodeOptions.java:44); leaderLeaseTimeoutMs=**900ms** (NodeOptions.java:61,305); electionPriority=-1 默认禁用 (NodeOptions.java:50); stepDownWhenVoteTimedout=true (RaftOptions.java:112)

### M2 选举超时入口 (NodeImpl.handleElectionTimeout L620-652)
- 仅 FOLLOWER 且 isCurrentLeaderValid()==false 才触发 (L620-626); **lastLeaderTimestamp 距 now < electionTimeoutMs = leader 有效** (L1860-1862) — 心跳持续刷新
- 双检 + resetLeaderId (L628-637); **优先级选举门控 allowLaunchElection** (L662-696): priority=0 永不参选 (L665); 两轮超时则 decayTargetPriority 指数衰减 (L702-710)
- 超时后先 preVote 而非直接 electSelf (L645)

### M3 preVote 预投票 (L2787-2847 发送, L1774-1844 接收)
- 快照安装中禁 preVote (L2791-2796); **不增加本地任期** (L2801, 请求 setTerm(currTerm+1) 只是试探)
- prevVoteCtx 独立票箱 (支持 joint 双配置, L2816-2836); 自票计入 (L2837)
- 接收端: 候选人必须在 conf 中 (L1797); **leader lease 有效时直接拒绝** (L1802-1807) — 预投票核心价值
- 日志比较: granted = requestLastLogId >= 本地 (L1827)
- 处理: handlePreVoteResponse (L2723-2759): 仅 FOLLOWER 接受 (L2727); granted 达 quorum → electSelf (L2747-2752)

### M4 electSelf 正式投票 (L1163-1237)
- 前置: 自己在 conf 中 (L1166-1170); 停 electionTimer (L1171-1174)
- 状态迁移: STATE_CANDIDATE + currTerm++ + votedId=自己 + voteTimer.start (L1175-1181)
- voteCtx.init(conf, oldConf) (L1182) — joint 阶段双配置计票 (Ballot.java:69-91)
- **先持久化再发 RPC**: 解锁 getLastLogId → ABA 防御 (L1193-1196) → metaStorage.setTermAndVotedFor 落盘失败 stepDown (L1202-1208) — Raft 论文 5.2
- 广播 RequestVote (L1209-1228); 自票 grant 达 quorum 直接 becomeLeader (L1230-1233)

### M5 投票规则 (handleRequestVoteRequest L1875-1951)
- term: request >= currTerm → stepDown 更高任期; 更低忽略 (L1896-1911)
- **日志新旧**: logIsOk = (reqLastLogIndex, reqLastLogTerm).compareTo(lastLogId) >= 0 (L1926-1927)
- votedId 为空才投; **先 metaStorage.setVotedFor 持久化再置内存** (L1929-1938)
- granted = request.term == currTerm && candidateId.equals(votedId) (L1944)
- ABA: 解锁取 lastLogId 后重锁校验 term (L1921)

### M6 计票与当选 (handleRequestVoteResponse L2662-2694)
- 非 CANDIDATE 丢弃 (L2665); term 不匹配丢弃 (L2671); 响应 term 更高 → stepDown (L2677-2683)
- granted → voteCtx.grant → isGranted 达 quorum → becomeLeader (L2685-2689)
- **handleVoteTimeout** (L2849-2869): 候选人超时 → stepDown 后重新 preVote (stepDownWhenVoteTimedout=true 默认)

### M7 becomeLeader 初始化 (L1272-1309)
- 必须 CANDIDATE; 停 voteTimer; STATE_LEADER (L1273-1278)
- replicatorGroup.resetTerm + 加 replicator (learner 用 Learner 型不参与选举) (L1280-1298)
- **ballotBox.resetPendingIndex(lastLogIndex+1)** (L1300-1301): 旧任期日志不得直接提交 — 必须本任期日志提交后才能推进 commitIndex (Raft 安全性)
- **首条 conf 日志** (L1304-1307): confCtx.flush → unsafeApplyConfiguration 写 ENTRY_TYPE_CONFIGURATION — 确立本任期提交能力
- stepDownTimer.start (L1308)

### M8 stepDown 让位 (L1312-1370)
- CANDIDATE 停 voteTimer; LEADER/TRANSFERRING: 停 stepDownTimer + **ballotBox.clearPendingTasks** (L1322, BallotBox.java:151-160 未提交任务立即失败) + onLeaderStop 通知 FSM
- resetLeaderId (L1328-1329); confCtx.reset 中断配置变更 (L1333); 更高 term 才持久化任期+清 votedFor (L1340-1346)
- **wakeupCandidate**: stopAllAndFindTheNextCandidate 选日志最新者 → Replicator.sendTimeoutNowAndStop 发 TimeoutNowRequest (L1348-1355; 接收端 L3388-3431 立即 electSelf)
- 非 learner 重启 electionTimer (L1364-1369)

### M9 checkQuorum 保位 + 租约 (L2329-2439)
- stepDownTimer 每 500ms: 读锁轻量检查 (L2403-2422) → 写锁 checkDeadNodes (L2432-2435)
- **checkDeadNodes0** (L2352-2382): lastRpcSendTimestamp 距 now <= leaderLeaseTimeoutMs 判定存活 (L2365-2366); alive >= peers/2+1 → 刷新 lastLeaderTimestamp (L2377-2379) — **既是保位也是 read lease 续租**
- checkLeaderLease (L1847-1862): now - lastLeaderTimestamp < leaderLeaseTimeoutMs

### M10 ReadIndex 线性一致读 (NodeImpl L1493-1733 + ReadOnlyServiceImpl 473)
- 分流: LEADER→readLeader / FOLLOWER→readFollower (转发给 leader) / TRANSFERRING→EBUSY (L1565-1588)
- **readLeader 本任期提交检查** (L1623-1632): getTerm(lastCommittedIndex) != currTerm → EAGAIN 拒绝 — 旧任期提交可能被推翻
- **ReadOnlySafe**: 广播心跳计票 (L1653-1673); ReadIndexHeartbeatResponseClosure 成功数+1 >= quorum (L1546); maxReadIndexLag 健康过滤 (L1687-1733)
- **ReadOnlyLeaseBased**: lease 有效 → 本地直答 committedIndex, 零 RPC (L1674-1679)
- ReadOnlyServiceImpl: Disruptor 批量 (ReadIndexEvent L96-148, applyBatch=32); 响应挂 **pendingNotifyStatus TreeMap<logIndex, statuses>** (L203-205); **onApplied 由 FSMCaller.setLastApplied 驱动** (L382-423) — 等 apply 追上才响应
- 单节点快速路径: quorum<=1 直接返回 (L1611-1621)

### M11 FSMCallerImpl apply 队列 (801)
- **10 种任务** (L84-105): COMMITTED/SNAPSHOT_SAVE/SNAPSHOT_LOAD/LEADER_STOP/LEADER_START/START_FOLLOWING/STOP_FOLLOWING/SHUTDOWN/FLUSH/ERROR
- Disruptor 单消费者 + BlockingWaitStrategy (L143-160); isRunningOnFSMThread 校验 (L486-488)
- **runApplyTask 攒批** (L399-478): COMMITTED 只更新 maxCommittedIndex; 遇非 COMMITTED 或 endOfBatch 才 flush
- **doCommitted 主循环** (L520-588): 用户 FSM 抛异常 → 记录错误 break + setError, 不污染流水线 (L570-580)
- setLastApplied (L590-596) → notifyLastAppliedIndexUpdated → **ReadIndex pending 队列的唯一数据源**
- 快照任务排队执行 (L622-655 保存/L697-744 安装); 错误传播 onError (L746-748)

## 二、聚合与分级

| 机制 | 级别 | 理由 |
|---|---|---|
| M5 投票规则 + 持久化顺序 | P1 | Raft 正确性核心; 面试必问 |
| M3 preVote 不增任期 | P1 | 网络分区健壮性 |
| M7 本任期提交能力 | P1 | 安全性理解关键 |
| M10 ReadIndex 三模式 | P1 | 线性一致读实现 |
| M9 租约机制 | P1 | LeaseRead 与保位一体两面 |
| M2/M4/M6 选举链 | P1 | 主流程 |
| M8 stepDown/TimeoutNow | P2 | 主动传位 |
| M11 FSM 串行化 | P2 | 应用层设计 |
| M1 定时器随机化 | P2 | 参数语义 |

## 三、负面空间

- **不做 read 后写校验 (linearizable 之外)**: ReadIndex 只保证读一致, 不提供事务
- **不做乱序提交**: 严格按索引顺序 apply (对照 braft 同)
- **不做异步日志复制的乱序确认**: follower 必须按序
- **不做多 leader 仲裁外的防脑裂**: 双 leader 靠更高 term stepDown 收敛 (L2059-2070)
- **不做强一致读的本地缓存**: 每次读走 ReadIndex (无 Raft 层缓存)
