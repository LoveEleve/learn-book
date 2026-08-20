# J-4 成员变更 — 知识规划 (KP)

> 域级: 🔴 A | 模块: core/CliServiceImpl (672) + conf/ (Configuration 330/ConfigurationEntry 130/ConfigurationManager 110) + NodeImpl ConfigurationCtx (L332-538) + Ballot (双 quorum) + rpc/impl/cli 处理器
> 日期: 2026-08-15 | 版本: 1.4.1

## 一、机制提取 (逐源)

### M1 CLI 层 (CliServiceImpl 672)
- **addPeer** (L131-161): checkLeaderAndConnect (L118-129; getLeader L442-494 遍历 conf 发 GetLeaderRequest) → AddPeerRequest RPC (L143-146) → recordConfigurationChange 仅日志 (L152)
- removePeer 对称 (L168-198); **changePeers 整组替换** (L200-232, leader 端 diff)
- **transferLeader** (L393-418): 可带 peer, 空则 ANY_PEER (L408-410)
- **resetPeer 绕过 leader 强制覆盖** (L234-257); snapshot 直连任意节点 (L420-439)
- getPeers 强制走 leader (L597-640) — 保证读到最新已提交配置
- **learner2Follower = removeLearners + addPeer** (L355-362) — 摘 learner 再按 voter 加入

### M2 配置数据模型 (conf/)
- **Configuration** (L47-50): peers List 保序 + learners LinkedHashSet 去重; isValid = peers 非空且互斥 (L159-163); diff 算 adding/removing (L318-323)
- **ConfigurationEntry** (L38-40): {id, conf, oldConf}; **isStable = oldConf.isEmpty()** (L77-79); listPeers = 双配置并集 (L85-89) — 选举/追平/投票的成员视图
- **ConfigurationManager** (L38): LinkedList 按 index 递增; add 拒绝回退 (L44-52); get(index) 二分查 ≤index 最大配置 (L88-109); 空回退快照配置 (L80-86)

### M3 ConfigurationCtx 四阶段状态机 (NodeImpl L332-538)
- **Stage**: STAGE_NONE → CATCHING_UP → JOINT → STABLE (L333-338)
- **start** (L365-397): 并发检查 (L366-379); diff 算 nchanges (L386-389); 先 addNewLearners (L391); **adding 为空 (纯删除) 跳过追平直接 nextStage** (L392-395)
- **addNewPeers 追平** (L399-417): addReplicator + **waitCaughtUp(catchupMargin, dueTime = now + electionTimeoutMs)** (L408-411) — 追平期限一个选举超时; 任一失败即中止
- addNewLearners **不等待追平** (L419-429) — learner 无投票权, 落后无安全风险
- **onCaughtUp** (L431-449): version 不匹配忽略 (L432-435); 全追平 → nextStage; 失败 → **reset(ECATCHUP) 整体回滚** (L448)
- **reset** (L451-474): 停新 peer replicator (L456-462); version++ 失效在途回调 (L466); done 以 EPERM "Leader stepped down" 回调 (L469-471)
- **flush** (L490-504): 新 leader 上任以新 term 重写当前配置 (stable→STABLE; 有 oldConf→JOINT) — 防旧 term 未提交配置的歧义
- **nextStage 状态转移** (L506-533): CATCHING_UP→JOINT (提交带 oldConf 日志, L509-514); JOINT→STABLE (提交 oldConf=null 合并日志, L516-518); STABLE→reset + **被移除则 stepDown(ELEADERREMOVED)** (L520-526)

### M4 配置日志提交链
- **unsafeRegisterConfChange** (L2495-2530): 非 leader EPERM/TRANSFERRING EBUSY (L2502-2514); **并发变更 EBUSY** (L2516-2523); **与当前配置相等直接成功** (L2525-2527, 幂等)
- **unsafeApplyConfiguration** (L2471-2493): ENTRY_TYPE_CONFIGURATION 条目含 peers/learners/oldPeers/oldLearners (L2474-2481); **ballotBox.appendPendingTask 用双配置 quorum** (L2484)
- **提交回调链**: BallotBox.commitAt 双 quorum → FSMCaller.doCommitted → ConfigurationChangeDone.run (L2459) → onConfigurationChangeDone (L2592-2604, term 校验) → confCtx.nextStage
- **checkAndSetConfiguration** (NodeImpl L719-742; LogManagerImpl L1118-1132): 配置日志落盘即登记 configManager.add (L349-357), 内存生效配置立即替换
- **joint 通知**: FSMCallerImpl L544-549 — 仅 joint 日志 (oldPeers 非空) 回调 fsm.onConfigurationCommitted, STABLE 不打扰

### M5 变更期间选举/投票约束
- electSelf 自选判定 conf.contains (L1167-1169, 含 oldConf); voteCtx.init 双配置 (L1182); handlePreVoteRequest 成员判定 (L1797-1801)
- **无 isConfChangeAllowed 方法** — 职责由 双配置判定 + unsafeRegisterConfChange 防护 + 主动让位承担
- **变更被中断回滚**: stepDown → confCtx.reset (L1333) → done=EPERM; 无"回滚日志"概念 — 未提交的新配置日志从未落盘; JOINT 已提交但 STABLE 未提交 → 新 leader flush 重写

### M6 transferLeader (L3313-3431)
- **配置变更中拒绝转移** (L3323-3340) — 目标带旧配置会在投票中抬 term 破坏新 leader
- ANY_PEER → findTheNextCandidate (lastLogId 最大, L3347); 必须属于当前配置 (L3355)
- STATE_TRANSFERRING + onLeaderStop + transferTimer 超时恢复 (L3366-3373)
- 接收端 handleTimeoutNowRequest: term 校验 + 仅 FOLLOWER + electSelf (L3388-3431)

### M7 learner 提升
- 添加不追平 (L419-429); **提升 (learner→voter) 走 addPeer 才 waitCaughtUp** (CliServiceImpl L355-362 → NodeImpl L3136-3148)
- 追平实现: Replicator.waitForCaughtUp (L934-957, 单槽位 L943-946, dueTime 定时器 L949-952); **ETIMEDOUT 但近期有 RPC → 重新追平** (L2309-2318, 网络抖动容错) — 否则 ECATCHUP 回滚

## 二、聚合与分级

| 机制 | 级别 | 理由 |
|---|---|---|
| M3 四阶段状态机 | P1 | 成员变更核心; 面试必问 |
| M4 双配置日志提交链 | P1 | joint 语义的工程形态 |
| M2 双配置模型/isStable | P1 | 理解一切的前提 |
| M1 CLI 流程 | P2 | 使用面 |
| M6 transferLeader | P2 | 运维场景 |
| M7 learner 提升 | P2 | 特性 |
| M5 选举约束 | P2 | 安全性联动 |

## 三、负面空间

- **不做自动故障摘除**: 节点宕机不自动 removePeer (运维手动)
- **不做多配置并行变更**: 并发 EBUSY, 一次一个
- **不做回滚日志**: 未提交变更直接作废, 无显式回滚条目
- **不做自动 rebalance**: 无副本迁移调度
- **不做变更前的仲裁确认**: 只有 leader 发起的追平+提交 (对照 etcd learner 提升需人工)
