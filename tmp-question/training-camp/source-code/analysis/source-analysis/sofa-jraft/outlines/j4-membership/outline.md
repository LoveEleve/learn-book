# J-4 成员变更 — 给跑着的火车换轮子: 四阶段状态机与双配置

> 前置: [[J-1-RAFT核心循环]] (选举/stepDown) + [[J-2-日志复制]] (追平 waitCaughtUp) + [[J-3-快照压缩]] (快照配置锚点) | 引出: 阶段5 Nacos JRaftProtocol 对照 | 对照: etcd learner + Kafka reassign
> 🔴 A | 7 KP | [模式: 四阶段状态机 + 双配置]
> Pass 2 闭环: q1(追平) q2(joint) q3(合并) q4(回滚) q5(转移)

**读者处境**: 集群扩容加一台机器, 直接改配置会怎样? 为什么新节点必须先"追上日志"才能加入? 为什么变更中 leader 不能转移? 变更失败会留半截配置吗?

### 1. CLI 视角 — 纯 RPC 客户端, 活全在 leader

场景: addPeer 命令发出后, CLI 做了什么?
源码路径:
- addPeer (CliServiceImpl.java:131-161): checkLeaderAndConnect — 先 getLeader 遍历 conf 发 GetLeaderRequest (CliServiceImpl.java:442-494) → AddPeerRequest RPC (L143-146) → recordConfigurationChange 仅日志 (L152)
- resetPeer 绕过 leader 强制覆盖 (CliServiceImpl.java:234-257); getPeers 强制走 leader (CliServiceImpl.java:597-640)
关键设计 (q1): **CLI 零逻辑, 追平/提交全在 leader 端** — 客户端只负责"找到 leader 并转达"; 查询也走 leader 保证读到已提交配置。 [模式: 瘦客户端]

### 2. 配置模型 — 双配置条目的 isStable

场景: "joint consensus" 在代码里长什么样?
源码路径:
- ConfigurationEntry {id, conf, oldConf} (ConfigurationEntry.java:38-40); **isStable = oldConf.isEmpty()** (ConfigurationEntry.java:77-79)
- **listPeers = 双配置并集** (ConfigurationEntry.java:85-89) — 选举/追平/投票的统一成员视图
- ConfigurationManager: 按 index 线性索引配置历史 (ConfigurationManager.java:38); get(index) 二分查 (ConfigurationManager.java:88-109); 空回退快照配置 (L80-86)
关键设计 (q2): **双配置 = 一条日志两个集合** — oldConf 非空 = joint 期; 合并完成 = oldConf 清空; 成员视图用并集保证"旧节点在新配置生效前仍有投票权"。 [模式: 双态配置]

### 3. 追平 — 新节点必须先追上日志

场景: 新节点日志落后 10 万条, 直接让它投票会怎样?
源码路径:
- start (NodeImpl.java:365-397): diff 算 adding (L386-389); 先 addNewLearners (L391); **adding 为空 (纯删除) 跳过追平** (NodeImpl.java:392-395)
- **addNewPeers** (NodeImpl.java:399-417): addReplicator + **waitCaughtUp(catchupMargin, dueTime = now + electionTimeoutMs)** (NodeImpl.java:408-411)
- **追平期限 = 一个选举超时** — 超过说明网络/节点异常, 宁可中止变更
- 追平实现: Replicator.waitForCaughtUp (Replicator.java:934-957); **ETIMEDOUT 但近期有 RPC → 重新追平** (NodeImpl.java:2309-2318, 网络抖动容错)
关键设计 (q1): **追平防"新节点带旧日志参与仲裁"** — 落后节点投票可能把已提交数据选没; 期限 = 选举超时, 追不上就中止 (ECATCHUP 回滚)。 [模式: 前置同步]

### 4. 四阶段 — CATCHING_UP → JOINT → STABLE

场景: 配置变更日志怎么写的? 两条还是一?
源码路径:
- **nextStage 转移表** (NodeImpl.java:506-533): CATCHING_UP → **JOINT: 提交带 oldConf 的配置日志** (L509-514); JOINT → **STABLE: 提交 oldConf=null 合并日志** (L516-518); STABLE → reset + **被移除则 stepDown(ELEADERREMOVED)** (L520-526)
- **unsafeApplyConfiguration** (NodeImpl.java:2471-2493): 条目含 peers/learners/oldPeers/oldLearners (L2474-2481); **ballotBox.appendPendingTask 双配置 quorum** (L2484)
- 提交回调链: BallotBox 双 quorum (BallotBox.java:99-143) → FSMCaller → ConfigurationChangeDone (NodeImpl.java:2459) → onConfigurationChangeDone (L2592-2604) → nextStage
关键设计 (q2): **两条日志完成一次变更** — JOINT 日志 (新老都算 quorum) 提交后, 追加 STABLE 日志 (只算新配置); 任一步未达双 quorum 就停在原地等。 [模式: 两段提交变体]

### 5. 并发防护与幂等 — 一次一个变更

场景: 变更进行中又来一个变更 / 重复 addPeer 会怎样?
源码路径:
- unsafeRegisterConfChange (NodeImpl.java:2495-2530): 非 leader EPERM / TRANSFERRING EBUSY (L2502-2514); **并发变更 EBUSY "Doing another configuration change"** (L2516-2523); **与当前配置相等直接成功** (L2525-2527, 幂等重试)
- 变更中 leader 让位 → stepDown → confCtx.reset (NodeImpl.java:1333) → done=EPERM "Leader stepped down" (NodeImpl.java:469-471)
关键设计 (q4): **一次一个 + 幂等重试** — 并发变更会破坏双配置的语义; 相等即成功让运维重试安全。 [模式: 互斥+幂等]

### 6. 失败回滚 — 没有"回滚日志"

场景: 追平失败, 会留下半截配置吗?
源码路径:
- onCaughtUp 失败 → **reset(ECATCHUP)** (NodeImpl.java:448): 停新 peer replicator (L460-461); version++ 失效在途回调 (L466); done 失败回调 (L469-471)
- **无回滚日志**: 未提交的新配置日志从未落盘, 停 replicator 即可; JOINT 已提交但 STABLE 未提交 → 依赖新 leader 的 **confCtx.flush 重写** (NodeImpl.java:490-504)
- flush: 新 leader 上任以新 term 重写当前配置 — 防旧 term 未提交配置的歧义
关键设计 (q4): **回滚 = 不落盘 + 失效回调** — 配置日志没提交过就"不存在", 无需反向日志; 已提交一半靠"新 leader 重写"收敛。 [模式: 无痕回滚]

### 7. 领导权转移 — TimeoutNow 与变更互斥

场景: 变更中为什么不能 transferLeader?
源码路径:
- **配置变更中拒绝转移** (NodeImpl.java:3323-3340) — 目标带着旧配置收到 TimeoutNow, 会在投票中不断抬 term 破坏新 leader
- ANY_PEER → findTheNextCandidate (lastLogId 最大, NodeImpl.java:3347); 必须属于当前配置 (L3355)
- STATE_TRANSFERRING + transferTimer 超时恢复 (NodeImpl.java:3366-3373); 接收端 handleTimeoutNowRequest → electSelf (NodeImpl.java:3388-3431)
- learner 提升: 添加不追平 (NodeImpl.java:419-429); 提升走 addPeer 才追平 (CliServiceImpl.java:355-362)
关键设计 (q5): **转移=指定节点立即参选** — TimeoutNow 跳过选举超时等待; 与变更互斥是因为"旧配置的候选会污染新 leader 的任期"。 [模式: 互斥阶段]

## 代码类型
Architecture (一致性算法核心)

## 负面空间 — SOFAJRaft 成员变更刻意不做的事

- **不做自动故障摘除**: 宕机节点需手动 removePeer (对照 Kubernetes 自愈)
- **不做多配置并行**: 一次一个变更 (并发 EBUSY)
- **不做显式回滚日志**: 未提交即作废 (无补偿日志)
- **不做副本 rebalance**: 无自动迁移调度
- **不做变更的多数仲裁预检**: 依赖追平+双配置提交隐式保证

→ 引出: 日志/配置最终落在哪? → J-5 存储与 RPC
