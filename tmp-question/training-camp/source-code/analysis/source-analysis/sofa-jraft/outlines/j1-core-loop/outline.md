# J-1 RAFT 核心循环 — 从一次超时到一条提交: 一台节点的自我修养

> 前置: [[Z-1-Leader选举]] (ZAB 对照) + [[Z-2-原子广播]] (ZAB 广播对照) | 引出: [[J-2-日志复制]] (leader 写路径) + [[J-4-成员变更]] | 对照: ZAB 协议 + Kafka KRaft
> 🔴 A | 11 KP | [模式: 角色状态机 + 任期 + 先持久化后投票]
> Pass 2 闭环: q1(超时门控) q2(预投票) q3(投票规则) q4(提交安全) q5(ReadIndex)

**读者处境**: 三台机器组 Raft, 一台突然收不到心跳——它会在什么时候、以什么方式发起选举? 为什么 SOFAJRaft 选主前要先"预投票"? 当选后为什么不能立刻响应读请求?

### 1. 角色状态机与定时器 — 8 态与"随机化"的超时

场景: 为什么选举超时不是固定 1 秒?
源码路径:
- **8 态** (State.java:25-34): LEADER/TRANSFERRING/CANDIDATE/FOLLOWER/ERROR/UNINITIALIZED/SHUTTING/SHUTDOWN; isActive = 序数 < ERROR (L36-38)
- **RepeatedTimer** (RepeatedTimer.java:38-295): HashedWheelTimer 底座 (L57); 触发后自动重排 (L83-107); **每次调度前 adjustTimeout** (L187)
- **随机选举超时** (NodeImpl.java:893-895): randomTimeout = [timeoutMs, timeoutMs + maxElectionDelayMs) — 默认 [1000, 2000)ms
- 四定时器 (NodeImpl.java:926-991): voteTimer (L929)/electionTimer (L943)/stepDownTimer (500ms = electionTimeoutMs>>1, L957)/snapshotTimer (L966)
- 心跳间隔 = electionTimeout/10 = **100ms** (L889-891)
关键设计 (q1): **随机窗口防"投票风暴"** — 若所有 follower 同时超时同时自荐, 每轮都无人拿到多数; 抖动把发起时间打散。stepDownTimer 用 half-election 周期做保位检查, 比选举超时更早发现 leader 失联。 [模式: 定时器 + 抖动]
跨层: [并发:][HashedWheelTimer 时间轮 (Netty 移植)]

### 2. 选举超时门控 — "leader 还活着"怎么判定

场景: 心跳延迟 1.2 秒, 要不要选主?
源码路径:
- handleElectionTimeout (NodeImpl.java:620-652): 仅 FOLLOWER **且 isCurrentLeaderValid()==false** 才进入 (L620-626)
- isCurrentLeaderValid (L1860-1862): now - lastLeaderTimestamp < electionTimeoutMs — 心跳持续刷新时间戳
- 优先级选举 (L662-710): priority=0 永不参选 (L665); 低优先级等一轮, 两轮超时 decayTargetPriority 指数衰减
关键设计 (q1): **"逻辑时钟"而非"计数"** — leader 有效性由最后心跳时间判定; 心跳 100ms 而超时 1000ms, 有 10 倍余量容忍抖动; 优先级选举让"指定节点"优先当选 (半确定性选主, 利于运维指定)。 [模式: 时间戳窗口]

### 3. preVote — 为什么要"预"投票

场景: 网络分区节点发起选举, 会不会打断健康 leader?
源码路径:
- preVote 发送 (NodeImpl.java:2787-2847): **不增加本地任期** (L2801, 请求 term = currTerm+1 只是试探); 快照安装中禁投 (L2791-2796); prevVoteCtx 独立票箱 (L2816-2836)
- preVote 接收 (L1774-1844): 候选必须在本配置中 (L1797); **leader lease 有效 → 直接拒绝** (L1802-1807); 日志比较 granted = reqLastLogId >= 本地 (L1827)
- 处理 (L2723-2759): granted 达 quorum → 才 electSelf
关键设计 (q2): **投票是"伤感情"的操作, 先试探** — 若多数派仍认可现任 leader (lease 有效), 预投票被拒, 分区节点自动放弃, 不会造成任期膨胀; 这是 Raft 对"对称网络分区"健壮性的关键 (论文 §9.6)。 [模式: 试探性协商]

### 4. electSelf — 正式参选与"先落盘后拉票"

场景: 预投票过了, 正式投票第一步是什么?
源码路径:
- electSelf (NodeImpl.java:1163-1237): 自己在 conf 中 (L1166); STATE_CANDIDATE + currTerm++ + votedId=自己 (L1175-1181); voteCtx.init(conf, oldConf) joint 双配置 (L1182)
- **先持久化再发 RPC** (L1190-1208): 释放锁 getLastLogId → **ABA 防御** (L1193-1196: 重锁后校验 term 没变) → metaStorage.setTermAndVotedFor 落盘, 失败 stepDown
- 广播 RequestVote 带 lastLogIndex/lastLogTerm (L1209-1228)
关键设计 (q3): **一任期一票是"崩溃安全"的** — Raft 论文 5.2: (term, votedFor) 必须先持久化, 否则崩溃重启后可能在同一任期投两次票; ABA 防御防"拿锁前后任期变化"的竞态。 [模式: 先持久化后对外]

### 5. 投票规则 — 一票投给谁

场景: 收到 RequestVote, 什么条件才投?
源码路径:
- handleRequestVoteRequest (NodeImpl.java:1875-1951): 更高 term → stepDown (L1896-1906); 更低忽略
- **日志新旧比较** (NodeImpl.java:1926-1927): logIsOk = (reqLastLogIndex, reqLastLogTerm) >= (lastLogIndex, lastLogTerm); LogId.compareTo **先比 term 再比 index** (LogId.java:94-103 注释 "Compare term at first") — 候选人的日志不落后才投
- votedId 为空才投; **先 metaStorage.setVotedFor 再置内存** (L1929-1938)
- granted = request.term == currTerm && candidateId.equals(votedId) (L1944)
关键设计 (q3): **日志新旧是 (term, index) 的字典序比较** — term 优先, term 相同比 index (LogId.java:94-103); 防"日志落后的节点当选后覆盖已提交数据"; 投票持久化同样先落盘。 [模式: 双维比较]

### 6. 计票与当选 — 双配置 quorum

场景: joint 变更期间, 多少个"是"算当选?
源码路径:
- handleRequestVoteResponse (NodeImpl.java:2662-2694): 非 CANDIDATE/term 不匹配丢弃 (L2665,2671); 响应 term 更高 → stepDown (L2677-2683)
- voteCtx.grant → isGranted → becomeLeader (L2685-2689)
- **voteCtx 双配置**: init(conf, oldConf) — joint 期新老配置各算 quorum (Ballot.java:69-91)
- handleVoteTimeout (L2849-2869): 候选人超时 → stepDown 后重新 preVote (默认 stepDownWhenVoteTimedout=true)
关键设计 (q4): **joint 期间两套配置都要多数** — 这是成员变更安全性的基石 (J-4 展开); 超时后主动让位而非无限自旋, 防止集群被"永不放弃的候选人"反复打扰。 [模式: 双票箱]

### 7. 当选之后 — 为什么第一条日志必须是配置日志

场景: 新 leader 上任第一件事是什么? 为什么不能直接提交旧日志?
源码路径:
- becomeLeader (NodeImpl.java:1272-1309): replicatorGroup.resetTerm + 建 replicator (L1280-1298)
- **ballotBox.resetPendingIndex(lastLogIndex + 1)** (L1300-1301): 旧任期日志不得直接提交
- **首条 conf 日志** (L1304-1307): unsafeApplyConfiguration 写 ENTRY_TYPE_CONFIGURATION — 本任期日志
- stepDownTimer.start (L1308)
关键设计 (q4): **"本任期提交能力"是 Raft 安全性的开关** — 旧任期日志只有部分节点有, 新 leader 若直接提交它, 一旦被推翻会破坏一致; 只有本任期日志被多数复制后, commitIndex 才能越过旧日志 (论文 5.4.2)。配置日志作为第一条, 顺带确立新配置生效。 [模式: 任期标记提交]

### 8. 让位与传位 — stepDown 的优雅

场景: 运维要重启 leader, 怎么不中断服务?
源码路径:
- stepDown (NodeImpl.java:1312-1370): 分状态清理 — CANDIDATE 停 voteTimer; LEADER: 停 stepDownTimer + **ballotBox.clearPendingTasks** (L1322, 未提交任务立即失败) + onLeaderStop
- **wakeupCandidate** (L1348-1355): stopAllAndFindTheNextCandidate 选日志最新者 → 发 **TimeoutNowRequest** → 对方 electSelf (L3388-3431)
- 非 learner 重启 electionTimer (L1364-1369)
关键设计 (q4): **主动传位 = TimeoutNow 跳过等待** — 正常让位等选举超时需 1-2 秒, TimeoutNow 让指定节点立即参选; "选日志最新者"保证传位后无日志追赶。 [模式: 主动交接]

### 9. 保位与租约 — checkQuorum 一体两面

场景: 半数节点死了, leader 还硬撑着吗?
源码路径:
- checkQuorum (NodeImpl.java:2401-2439): stepDownTimer 每 500ms; 读锁轻量检查 → 写锁 checkDeadNodes (L2432-2435)
- **checkDeadNodes0** (L2352-2382): lastRpcSendTimestamp 距 now <= leaderLeaseTimeoutMs (900ms) 判定存活; alive >= peers/2+1 → 刷新 lastLeaderTimestamp (L2377-2379)
- **checkLeaderLease** (L1847-1862): now - lastLeaderTimestamp < leaderLeaseTimeoutMs
关键设计 (q5): **同一套"最后心跳时间"服务两个目的** — 保位检查 (半数存活) 和读租约 (lease 有效) 共用 lastLeaderTimestamp; 参数保证 lease(900ms) + clockDrift < electionTimeout(1000ms) (NodeOptions.java:57-61) — 租约永不误判。 [模式: 时间戳续租]

### 10. 线性一致读 — ReadIndex 三模式

场景: 读操作也走日志复制吗? 太贵了怎么办?
源码路径:
- 分流 (NodeImpl.java:1565-1588): LEADER→readLeader; FOLLOWER→readFollower 转发 leader; TRANSFERRING→EBUSY
- **本任期提交检查** (L1623-1632): getTerm(lastCommittedIndex) != currTerm → EAGAIN — 本任期未提交过日志不能服务读
- **ReadOnlySafe**: 广播心跳 + 计票成功数+1 >= quorum (L1653-1673, L1546); maxReadIndexLag 健康过滤 (L1687-1733)
- **ReadOnlyLeaseBased**: lease 有效 → 本地直答 committedIndex, 零网络 (L1674-1679)
- 单节点快速路径 (L1611-1621)
关键设计 (q5): **读一致性的三种成本档** — Safe (广播确认, 最稳) / Lease (本地直答, 最快, 代价是依赖时钟) / 单节点 (直答); "本任期提交"检查是线性一致读的必要条件 — 旧任期提交的索引可能被新 leader 推翻。 [模式: 成本档位]
跨层: [算法:][Linearizability vs Sequential consistency]

### 11. 状态机应用 — 单线程串行化的艺术

场景: 日志怎么变成业务状态? 回调在哪个线程跑?
源码路径:
- **10 种任务** (FSMCallerImpl.java:84-105): COMMITTED/SNAPSHOT_SAVE/LOAD/LEADER_STOP/START/FOLLOWING 等
- Disruptor 单消费者 + BlockingWaitStrategy (L143-160); isRunningOnFSMThread 校验 (L486-488)
- **runApplyTask 攒批** (L399-478): COMMITTED 只更新 maxCommittedIndex; 遇非 COMMITTED 或 endOfBatch 才 flush — 32 条一批 (applyBatch)
- **doCommitted** (L520-588): 用户 FSM 抛异常 → 记录错误 break + setError, 不污染流水线 (L570-580)
- **setLastApplied** (L590-596) → notifyLastAppliedIndexUpdated → **ReadIndex pending 队列的唯一唤醒源**
关键设计 (q5): **状态机必须单线程 apply** — Raft 日志是线性序, 并行 apply 会破坏确定性; Disruptor 攒批把 32 条日志一次交给 FSM; FSM 异常隔离 — 不阻塞后续日志的接收, 但停止 apply 并置 ERROR。 [模式: 单消费者队列]

## 代码类型
Architecture (一致性算法核心)

## 负面空间 — SOFAJRaft 核心刻意不做的事

- **不做读后写的事务保证**: ReadIndex 只保证线性一致读, 无快照隔离/事务 (对照 Seata 分布式事务)
- **不做乱序提交**: 严格按索引串行 apply, 无批量重排序
- **不做脑裂的最终仲裁外机制**: 双 leader 靠"更高 term 者胜"收敛 (L2059-2070), 无 fencing token (对照 ZAB 的 epoch)
- **不做 Raft 层读缓存**: 线性一致读每次过 ReadIndex, 无本地缓存加速
- **不做日志并行确认**: follower 必须按序接收 (J-2 展开)

→ 引出: leader 怎么把日志发给每个 follower? → J-2 日志复制
