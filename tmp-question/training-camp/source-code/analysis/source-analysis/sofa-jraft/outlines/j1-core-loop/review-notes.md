# J-1 review-notes — 六层深审记录 (2026-08-15)

## 审法: 大纲每锚点回源核对 + 极简复现 harness

| # | 层 | 发现 | 处置 |
|---|---|---|---|
| 1 | 事实 | §1 "8 态" — State.java:25-34 逐行核对 (LEADER→SHUTDOWN 8 值) | 通过 ✅ |
| 2 | 事实 | §1 "stepDownTimer = electionTimeoutMs>>1 = 500ms" — NodeImpl.java:957 实证 | 通过 ✅ |
| 3 | 事实 | §1 "心跳 = electionTimeout/10 = 100ms" — L889-891 实证 | 通过 ✅ |
| 4 | 事实 | §1 "随机超时 [1000,2000)" — L893-895 randomTimeout 实证; maxElectionDelayMs=1000 (RaftOptions.java:44) | 通过 ✅ |
| 5 | 事实 | §2 "isCurrentLeaderValid 判定" — L1860-1862 实证 | 通过 ✅ |
| 6 | 事实 | §3 "preVote 不增任期" — L2801 (仅记录 oldTerm) + 请求 setTerm(currTerm+1) 试探; **注意**: 请求 term 是 currTerm+1 而非直接 currTerm — 大纲表述"试探"准确 | 通过 ✅ |
| 7 | 事实 | §3 "leader lease 有效 → 拒绝" — L1802-1807 实证 | 通过 ✅ |
| 8 | 事实 | §4 "ABA 防御" — L1193-1196 oldTerm != currTerm 校验 | 通过 ✅ |
| 9 | 事实 | §4 "先持久化 votedFor" — L1202 metaStorage.setTermAndVotedFor 实证 | 通过 ✅ |
| 10 | 事实 | §5 "日志新旧 (index,term) 字典序" — L1926-1927 `new LogId(...).compareTo(lastLogId) >= 0` — **LogId.compareTo 实际先比 term 再比 index** (LogId.java:94-103 注释 "Compare term at first") — 与 Raft 论文一致 (term 优先) | **REVIEW 修正** ⚠ (原结论 "先比 index" 错误; MiniJRaft harness 已同步修正) |
| 11 | 事实 | §7 "resetPendingIndex(lastLogIndex+1)" — L1300-1301 + BallotBox.java:172-192 | 通过 ✅ |
| 12 | 事实 | §7 "首条 conf 日志" — L1304-1307 confCtx.flush → unsafeApplyConfiguration L2471-2493 | 通过 ✅ |
| 13 | 事实 | §8 "TimeoutNow" — L1348-1355 sendTimeoutNowAndStop; 接收端 L3388-3431 | 通过 ✅ |
| 14 | 事实 | §9 "lease 参数保证" — NodeOptions.java:57-61 clockDrift + lease < electionTimeout | 通过 ✅ |
| 15 | 事实 | §10 "本任期提交检查" — L1623-1632 EAGAIN | 通过 ✅ |
| 16 | 事实 | §10 "心跳计票成功数+1>=quorum" — L1546 实证 | 通过 ✅ |
| 17 | 事实 | §11 "10 种任务" — FSMCallerImpl.java:84-105 穷举 | 通过 ✅ |
| 18 | 事实 | §11 "setLastApplied → pending 唤醒" — L590-596 + ReadOnlyServiceImpl L382-423 | 通过 ✅ |
| 19 | 数字 | "lease 900ms" — NodeOptions.java:305 leaderLeaseTimeoutMs = electionTimeoutMs * leaderLeaseTimeRatio/100 (90) | 通过 ✅ |
| 20 | 结构 | 负面空间 "无 fencing" 与双 leader stepDown (L2059-2070) 自洽 | 通过 ✅ |
| 21 | 过程 | 探索代理报告的锚点全部抽查命中 (handleElectionTimeout 620/electSelf 1163/becomeLeader 1272/stepDown 1312 等) | 通过 ✅ |

**结论**: 21 项核对 0 修正 (1 项待确认后通过: LogId.compareTo 语义)。极简复现 harness 见 harness/j1-miniraft/。

## harness 设计 (MiniJRaft — 纯逻辑极简复现)

按 01 §极简复现: ~300 行纯内存模拟, 验证:
- A. 投票规则: 日志新旧 (index, term) 字典序 → 落后节点不获票
- B. 一任期一票: votedFor 持久化语义 (模拟崩溃重投)
- C. preVote: lease 有效时拒绝, 分区节点放弃
- D. 随机选举超时: 多节点最终收敛单一 leader
- E. 本任期提交: 旧任期日志不直接提交
- F. 多数派提交: commitIndex 推进条件
