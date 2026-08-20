# Z-2 原子广播 — ZAB 两阶段提案与学习者同步

> 前置: [[Z-1-Leader选举]] (FastLeaderElection 产出 leader) | 引出: [[Z-3-DataTree]] (广播落点) | 对照: RM-12 (RocketMQ 组提交) + K-9 (Kafka KRaft 两阶段) + sofa-jraft (日志复制)
> 🔴 A | 8 KP | [模式: 两阶段提案 + 学习者同步 + 连接仲裁]
> Pass 2 闭环: q1(lead 启动) q2(两阶段提案) q3(同步三模式) q4(连接仲裁)

**读者处境**: leader 当选后怎么广播数据? 新 follower 加入怎么补数据? 网络分裂谁连谁? 这篇拆 ZAB 广播: lead 状态机 (DISCOVERY→SYNCHRONIZATION→BROADCAST) + PROPOSAL→ACK→COMMIT + LearnerHandler 同步五分支 + QuorumCnxManager 连接仲裁。

### 1. lead 启动 — ZabState 三阶段

场景: leader 当选后做什么?
源码路径:
- **lead()** (Leader.java:632-772): **ZabState 状态机**: DISCOVERY → SYNCHRONIZATION → BROADCAST
- **DISCOVERY** (L644-654): zk.loadData() + leaderStateSummary + **LearnerCnxAcceptor 启动** (接受 follower 连接)
- **epoch 提议** (L655-663): getEpochToPropose + **zk.setZxid(makeZxid(epoch, 0))** — 新 epoch 从低 32 位 0 起; newLeaderProposal (NEWLEADER 包)
- **reconfig 版本** (L669-704): 双 QuorumVerifier (ZOOKEEPER-1783 注释 — 初始 config version=0 → NEWLEADER 时用 lastSeenQV)
- **同步等待** (L710-716): **waitForEpochAck** (electingFollowers 集合 + **isMoreRecentThan 超前拒绝** + **initLimit×tickTime 引导期超时**) → **waitForNewLeaderAck** (等多数 learner 同步完 NEWLEADER); **双超时面**: 引导期 initLimit vs 正常期 syncLimit (syncTimeout=tickTime×syncLimit)
- **BROADCAST 主循环** (L774-816): **每 tickTime/2 检查 synced learners** (SyncedLearnerTracker + 双 verifier) — quorum 保持检测 (失守 → shutdown); **每 tick f.ping()** (L844) — learner 双向保活 (ping 带 syncLimitCheck, LearnerHandler:1067); **PING 数据面携带会话 touch** (L678-687, Z-5 交叉)
关键设计 (q1): **三阶段状态机**: 发现 (自己数据) → 同步 (等多数) → 广播 (持续提案); 每半 tick 验证 quorum。[模式: 状态机]

### 2. 两阶段提案 — propose → processAck → tryToCommit

场景: 一条写请求怎么广播?
源码路径:
- **propose** (L1288-1340): 请求 → zxid 分配 (lastProposed++) → **outstandingProposals.put(zxid)** → broadcast (PROPOSAL 给所有 forwardingFollowers + observer 特殊); **pendingSyncs.computeIfAbsent** (sync 中的请求暂存 — 同步完成补发, 提案与同步交汇点)
- **processAck** (L1047-1115): allowedToCommit 守卫 (leader 变更后停) + **lastCommitted >= zxid 幂等忽略** + outstandingProposals.get (null → 未来提案警告) → p.addAck(sid) → tryToCommit
- **tryToCommit** (L963-1027): **顺序性守卫** (outstandingProposals.containsKey(zxid-1) → false — 前序未提交不提交!) + **hasAllQuorums()** (双 verifier) → outstandingProposals.remove + toBeApplied.add → **commit(zxid)** (COMMIT 广播) + inform (observer); **QuorumMaj 判定数学**: `ackSet.size() > half` (flexible/QuorumMaj, N=3 需 2; QuorumHierarchical 加权对照 — 判定可插拔)
- **reconfig 特例** (L999-1022): designatedLeader + processReconfig + **allowedToCommit=false** (本节点退出主) + commitAndActivate; **reconfig 链式提交** (L1105-1114: reconfig 提交后尝试后续 outstanding — 单 outstanding reconfig 前提)
- **pendingSyncs** (L1340): 同步等待的请求 (sync 后补发)
关键设计 (q2): **两阶段 + 严格顺序提交**: 提案可乱序到但提交必须连续 (zxid-1 守卫); 双 verifier 多数才提交。[模式: 两阶段]

### 3. 同步三模式 — syncFollower 五分支

场景: follower 加入怎么补数据?
源码路径:
- **LearnerHandler** (每 learner 一线程, L258+): **握手六步链** (五次 REVIEW 补): FOLLOWERINFO (sid/version/**configVersion 超前拒绝** L496-498) → **LEADERINFO (0x10000+ 协议; 老版本从 zxid 推断 epoch** L528-533) → ACKEPOCH → syncFollower → **SNAP 带 "BenWasHere" 签名** (L584) / DIFF/TRUNC → **UPTODATE** (同步完成标志, learner 才开始服务, L652-653) → 正常广播
- **syncFollower 五分支** (L780-879+):
  1. forceSnapSync → **SNAP** (测试强制)
  2. lastProcessedZxid == peerLastZxid → **空 DIFF** (已同步)
  3. **peerLastZxid > maxCommittedLog && !isPeerNewEpochZxid → TRUNC** (follower 有 leader 未见的 txn = 旧 leader 残留 → 截断到 maxCommittedLog; 注释: 新 epoch zxid 不可 TRUNC — 无 txnlog); **TRUNC 执行在 learner 侧: syncWithLeader → ZKDatabase.truncateLog** (Learner:618-622)
  4. minCommittedLog <= peerLastZxid <= maxCommittedLog → **DIFF** (committedLog 增量补)
  5. peerLastZxid < minCommittedLog && txnLogSyncEnabled → 磁盘 txnlog + committedLog 补; **失败 → SNAP**
- **syncThrottler 流控**: SNAP/DIFF 并发限流 (L566-634 INFLIGHT_SNAP/DIFF_COUNT); **follower 豁免 (exemptFromThrottle — observer 不豁免, 投票成员优先追平)**
- **learner 生命周期** (六次 REVIEW 补): **同步期** (syncWithLeader 后半 L649-745): PROPOSAL→packetsNotLogged (**enforceContinuousProposal 连续性校验** L660) / COMMIT→**zk.processTxn 内存应用** 或 packetsCommitted 延迟写 (writeToTxnLog, Z-9) / **UPTODATE→takeSnapshot+setCurrentEpoch+setZooKeeperServer (服务启动!)**; **服务期** (Follower:100-125): readPacket+processPacket (PING→ping / PROPOSAL→**zxid!=lastQueued+1 连续性警告** / COMMIT 应用); **ObserverMaster 级联** (Follower:112-119: follower 可当 observer 的 master — 3.6+ 拓扑扩展); **Observer 面**: OBSERVERINFO 注册 + **UPTODATE 不应达 observer (INFORM-only 严格只读)** (Observer:113,193-194); **LearnerMaster 抽象**: Leader 与 ObserverMaster 统一接口 (addLearnerHandler/waitForStartup/getEpochToPropose/syncTimeout)
- **超时面**: **syncTimeout = tickTime × syncLimit** (Leader:1719-1721); SyncLimitCheck **双槽窗口** (提案-ACK 间隔 L153-205); 超时 → 停 ping → learner 断连
- **leader shutdown 链** (L861-882): cnxAcceptor.halt → 断连接 → **zk.shutdown** → 全 learner shutdown (**isShutdown 幂等守卫**) → QuorumPeer 回 LOOKING 重新选举
关键设计 (q3): **五分支裁决**: 已同步 (空 DIFF) / 领先 (TRUNC) / 窗口内 (DIFF) / 窗口外 (txnlog 或 SNAP); TRUNC 是新主上任数据裁决核心。[模式: 同步裁决]

### 4. 连接仲裁 — QuorumCnxManager

场景: 集群节点之间谁连谁?
源码路径:
- **connectOne** (QuorumCnxManager:715-739): 主动连所有成员; **electionAddr 多地址** (L220-224)
- **连接仲裁** (L510-538, L635-650): 接收连接时 sid 比较 — **保持"大 sid → 小 sid"单方向**: 自己 sid 小 → 断开对方连接 (让大 sid 来连); 自己 sid 大 → 保留 (替换旧 SendWorker); 入站侧被小 sid 拒 → 断开重连大 sid — **每对节点恰一条确定性方向连接** (防双连/连接风暴)
- **queueSendMap/queueRecv**: 每对节点独立发送队列 + 连接线程
- **initiateConnectionAsync** (L419-427): connectionExecutor 异步连接 (失败重试)
关键设计 (q4): **单向连接保证**: 连接仲裁防双连 (每对节点恰一条 "大 sid → 小 sid" 方向); 发送队列隔离 (**CircularBlockingQueue SEND_CAPACITY 满则丢 — 网络分区面**)。[模式: 连接仲裁]

### 负面空间 — ZAB 广播刻意不做的事

- **不做 Raft 式日志复制流水线**: 提案全序广播 (无乱序 pipeline — 顺序提交守卫)
- **不做 pipelined commit**: 每次 COMMIT 前序必须已提交 (zxid-1 守卫) — 对照 Raft 的并行 AppendEntries
- **不做 learner 主动拉取**: 同步由 leader 推 (SNAP/DIFF/TRUNC 推送) — 对照 RM-12 从库拉取
- **不做观察者投票**: observer 收 commit 但无 ACK 权 (inform 单独路径)
- **不做同步持久化强制**: learner 同步后 async 落盘 (snapCount 批量) — Z-9
- **不做无 quorum 继续写**: quorum 失守 → leader shutdown (CP 严格)
- **不做乱序重配置**: 仅一个 outstanding reconfig (L1096-1103 注释)

→ 引出: 提交的数据落哪? → [[Z-3-DataTree]]
