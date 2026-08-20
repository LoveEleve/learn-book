# Z-2 原子广播 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **表述精确化** | 执行计划 "PROPOSAL→ACK→COMMIT" — 补全**双守卫提交**: 顺序守卫 (zxid-1) + quorum 守卫 (hasAllQuorums 双 verifier); 幂等忽略 (lastCommitted >= zxid) | 大纲 §2 |
| 2 | **语义标注** | **TRUNC 与新 epoch zxid 互斥**: peerLastZxid 低 32 位==0 (新 epoch) → 不 TRUNC (无 txnlog 语义, L858-867 注释) | 大纲 §3 |
| 3 | **补充锚点** | **每半 tick quorum 自检** (L774-816): SyncedLearnerTracker 双 verifier + synced learners — leader 失守主动 shutdown (CP 严格) | 大纲 §1 |
| 4 | **连接仲裁精确化** | **"大 sid → 小 sid" 单方向**: 接收连接 sid > myId → 断开 (L510-513); sid < myId → 保留 + 旧 SendWorker 替换 (L521-535); 被拒侧重连 (L635-650) | 大纲 §4 |
| 5 | 行号验证 | 全函数 30 锚点 + 跨文件 8 处 grep (Leader 632-816,963-1027,1047-1115,1288-1340 / LearnerHandler 780-879 / QuorumCnxManager 371-427,510-650,715-761) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 两阶段提案 + 顺序提交
- 同步五分支裁决
- 连接仲裁确定性

### 维度2 性能
- tickTime/2 quorum 自检
- syncThrottler 流控
- 异步连接

### 维度3 内存
- outstandingProposals (ConcurrentMap)
- queueSendMap CircularBlockingQueue

### 维度4 一致性
- zxid 全序 + 顺序提交
- 双 verifier 多数
- 单方向连接

### 维度5 负面空间 (已写入大纲 7 条)
- 不 Raft 流水线/不 pipelined commit/不 learner 拉取/不 observer 投票/不强制同步落盘/不无 quorum 写/不乱序 reconfig

## 结论
Z-2 全部锚点 ~30 处验证, 8 闭环完成, **表述精确化 2 + 补充锚点 1 + 语义标注 1**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 6 | 通过项 | 前置 Z-1 ✅; 引出 Z-3 ✅; 对照 RM-12/K-9/jraft ✅; 读者处境场景化 ✅; 锚点 ~30 ✅; 负面空间 7 条 ✅; 横切 (一致性/并发/广播协议/仲裁) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | **反写测试发现** | 大纲 §2 未提 **reconfig 链式提交** (L1105-1114): reconfig 提交后尝试后续 outstanding (单 outstanding reconfig 前提注释) — 重配置一致性面 | 大纲 §2 补注 |
| 8 | 通过项 | 其余 ~28 句机制描述逐句对源码一致 ✅ (三阶段/epoch 提议/NEWLEADER 等待/双守卫/幂等/toBeApplied/designatedLeader/五分支/txnlog 补/流控/连接仲裁/队列隔离) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (reconfig 链式提交 #7), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 顺序提交闭环 | zxid-1 在 outstanding → 不提交 → 前序完成移除 → 后续可提交 — 严格全序 ✅ | 通过 |
| V2 | 幂等语义 | lastCommitted >= zxid 忽略 — 重复 ACK 无害 ✅ | 通过 |
| V3 | 同步五分支覆盖 | 任意 peerLastZxid 相对 [min,max] 的位置都有分支 (==/ >/窗口内/ <) ✅ | 通过 |
| V4 | TRUNC 安全性 | 只截"leader 未提交范围" (maxCommittedLog 内已提交保留); 新 epoch 不截 ✅ | 通过 |
| V5 | 连接仲裁收敛 | 双向同时连 → 大 sid 保留/小 sid 断开 → 唯一方向 ✅ | 通过 |
| V6 | 双 verifier 多数 | reconfig 期间新旧配置都需多数 — 不丢已提交 ✅ | 通过 |
| V7 | 半 tick 自检 | 失守 → shutdown → 重新选举 — 分区自动恢复 ✅ | 通过 |
| V8 | observer 无 ack 权 | inform 单独路径 (L1026) — 不参与 quorum ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | **覆盖缺口** | 大纲未提 **pendingSyncs 机制** (L1340): sync 中的请求暂存 — 已提交但 follower 未同步的提案, 同步完成后补发 — 提案与同步的交汇点 | 大纲 §2 补注 |

## 三次 REVIEW 汇总
推理验证 8 项全过 (V1-V8); 新发现 **1 处** (pendingSyncs), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 反写测试 + 推理验证)

> 动机: 对全部锚点重新 grep, 追查六个存疑点 (allowedToCommit 触发/committedLog 结构/learner 收包/observer 路径/超时面/仲裁边界), 并做反写测试。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | allowedToCommit 何时 false? | L1015 (reconfig 换主) + shutdown 面 — 提交闸门 | 通过 (验证) |
| T2 | committedLog 结构? | ZKDatabase.getCommittedLog — Leader 侧内存提案缓存 (给 DIFF 用) — Z-3 交叉 | 通过 (验证) |
| T3 | learner 收包路径? | Learner.run → queuePacket → 处理链 (PROPOSAL→ACK/COMMIT→Final) — Z-4 交叉 | 通过 (验证) |
| T4 | observer 路径? | inform(p) 单独 (L1026); NEWLEADER 后 UPTODATE — 不 ACK 提案 | 通过 (验证) |
| T5 | 超时面? | syncLimit (LearnerHandler 超时) + tickTime — learner 失联判定 | 通过 (验证) |
| T6 | 仲裁边界? | SEND_CAPACITY 队列满 → 丢弃 (断连重连?) — CircularBlockingQueue 满行为 | 通过 (标注) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 两阶段数据安全 | PROPOSAL 全序 + 多数 ACK 才 COMMIT — 已提交数据多数持有 ✅ | 通过 |
| V2 | 同步补全 | pendingSyncs + toBeApplied — 提案在同步窗口不丢 ✅ | 通过 |
| V3 | 换主不丢已提交 | 新 leader 从 lastProcessedZxid 起 + TRUNC 保留已提交 ✅ | 通过 |
| V4 | 连接收敛有界 | 仲裁确定性 → 无连接循环 ✅ | 通过 |
| V5 | 提交-应用顺序 | toBeApplied 保序 → FinalRequestProcessor 同步应用 (注释 L1120-1126) ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **语义标注** | **队列满语义**: queueSendMap CircularBlockingQueue (SEND_CAPACITY) — 满则丢弃消息? 需标注 (连接质量面, 与网络分区相关) | 大纲 §4 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 lead 启动 (三阶段/epoch/双 verifier/自检) — 可写 ✅
- §2 两阶段 (propose/双守卫/幂等/reconfig 链式/pendingSyncs) — 可写 ✅
- §3 同步五分支 (空DIFF/TRUNC/DIFF/txnlog/SNAP/流控) — 可写 ✅
- §4 连接仲裁 (大→小单方向/队列隔离/满语义) — 可写 ✅
- 负面空间 7 条 — 完整 ✅

## 四次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 5 项全过 (V1-V5); **新发现 1 处** (队列满语义). 大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + 追查六个存疑点 + 反写测试 + 推理验证)

> 动机: 对 outline 全部锚点重新 grep 并追查 learner 侧交互六个存疑点 (握手链顺序/syncLimit 超时/ping 双端/TRUNC 落点/会话保活/流控豁免)。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | 握手链完整顺序? | FOLLOWERINFO (sid/version/configVersion — **configVersion 超前拒绝** L496-498) → **LEADERINFO (0x10000+ 协议; 老版本从 zxid 推断 epoch** L528-533) → ACKEPOCH → syncFollower → **SNAP 带 "BenWasHere" 签名** (L584) → **UPTODATE** (L652-653, 同步完成标志 — learner 才开始服务) → 正常广播 | 发现 11 (补锚) |
| T2 | syncLimit 超时语义? | **syncTimeout = tickTime × syncLimit** (Leader:1719-1721; syncLimit 显式配置无默认, 常见 2); SyncLimitCheck **双槽窗口** (current/next 提案-ACK 间隔, L153-205); 超时 → ping 不发 → learner 侧断连 | 发现 12 (补锚) |
| T3 | ping 双端? | Leader 主循环**每 tick f.ping()** (L844) → LearnerHandler.ping (L1067): **sendingThreadStarted 守卫** (未同步完不发 — 防 learner 崩溃注释) + syncLimitCheck.check | 通过 (验证) |
| T4 | TRUNC 落点? | learner 侧 **syncWithLeader** (Learner:618-622): TRUNC → **zk.getZKDatabase().truncateLog(qp.getZxid())** — 截断执行在 learner 的 ZKDatabase | 发现 13 (补锚) |
| T5 | 会话保活? | LearnerHandler 收 PING → **touch 会话 (数据面携带 sessionId+timeout)** (L678-687) — learner 会话经 leader 转发保活 (Z-5 交叉) | 通过 (验证) |
| T6 | 流控豁免? | **follower 豁免 syncThrottler** (exemptFromThrottle L561: observer 不豁免) — 保证投票成员快速追平 | 发现 14 (补锚) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 握手链闭环 | FOLLOWERINFO→LEADERINFO→ACKEPOCH→sync→UPTODATE→广播 — 每一步都有确认/超时 ✅ | 通过 |
| V2 | syncTimeout 数学 | tickTime×syncLimit (如 2000×2=4s) vs tick 周期 — 容错窗口合理 ✅ | 通过 |
| V3 | ping 保活闭环 | leader ping (每 tick) → learner ACK/PING 回 → syncLimitCheck 窗口刷新 — 双向保活 ✅ | 通过 |
| V4 | learner TRUNC 安全 | truncateLog 到 leader 指定 zxid — 与 leader committedLog 对齐 ✅ | 通过 |
| V5 | 会话 touch 正确性 | PING 数据面 touch → 会话不过期 (session 超时由 leader 统一判定 Z-5) ✅ | 通过 |
| V6 | 豁免语义 | 投票成员同步不被节流 → quorum 快速恢复 ✅ | 通过 |
| V7 | configVersion 防护 | follower 配置超前 → 拒绝 (防旧 leader 收新配置 follower) ✅ | 通过 |
| V8 | UPTODATE 时序 | serialize 期间 mutation 已排队 → UPTODATE 后排队包继续 — 无数据缺口 ✅ | 通过 |

## 新发现问题 (4 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | **补充锚点** | **握手六步链**: FOLLOWERINFO→LEADERINFO→ACKEPOCH→sync (SNAP 签名)→UPTODATE→广播; configVersion 超前拒绝 | 大纲 §3 补注 |
| 12 | **补充锚点** | **syncTimeout = tickTime×syncLimit** (SyncLimitCheck 双槽窗口; 超时 → 停 ping → learner 断连) | 大纲 §1 补注 |
| 13 | **补充锚点** | **TRUNC 执行在 learner 侧**: syncWithLeader → ZKDatabase.truncateLog | 大纲 §3 补注 |
| 14 | **语义标注** | **流控豁免**: follower 不节流 (observer 节流) — 投票成员优先追平 | 大纲 §3 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 lead 启动 (三阶段/epoch/双 verifier/自检/超时面) — 可写 ✅
- §2 两阶段 (propose/双守卫/幂等/reconfig/pendingSyncs) — 可写 ✅
- §3 同步五分支 (握手链/五分支/TRUNC 落点/流控/豁免) — 可写 ✅
- §4 连接仲裁 (大→小/队列/满语义) — 可写 ✅
- 负面空间 7 条 — 完整 ✅

## 五次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 8 项全过 (V1-V8); **新发现 4 处全部修复** (握手六步链/syncTimeout 数学/TRUNC 落点/流控豁免)。核心认知: learner 侧交互完整面 — 握手六步 + 双向保活 (ping) + 会话 touch 转发。大纲经修复后反写测试全过。

---

# 六次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + 追查 learner 生命周期六个存疑点 + 反写测试 + 推理验证)

> 动机: 对 outline 全部锚点重新 grep 并追查 learner 生命周期六个存疑点 (Follower 服务期主循环/同步期分包/ObserverMaster 级联/leader shutdown 链/连续性双校验/REQUEST 转发)。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | Follower 服务期主循环? | followLeader (Follower:100-125): 同步完成 (syncWithLeader→BROADCAST) → **主循环 readPacket + processPacket**: PING→ping(qp) / PROPOSAL→**连续性警告 (zxid != lastQueued+1)** + reconfig / COMMIT 等 (L156-179) | 发现 15 (补锚) |
| T2 | 同步期分包? | syncWithLeader 后半 (Learner:649-745): PROPOSAL → PacketInFlight + **enforceContinuousProposal** → packetsNotLogged; COMMIT → **zk.processTxn** (内存应用) 或 packetsCommitted (writeToTxnLog 延迟写, Z-9 交叉); **UPTODATE → takeSnapshot + setCurrentEpoch + setZooKeeperServer** (服务启动!) | 发现 16 (补锚) |
| T3 | ObserverMaster 级联? | Follower:112-119: observerMasterPort>0 → **ObserverMaster 启动** — follower 可当 observer 的 master (3.6+ 级联拓扑) | 发现 17 (补锚) |
| T4 | leader shutdown 链? | shutdown(reason) (L861-882): cnxAcceptor.halt → setZooKeeperServer(null) + closeAllConnections → **zk.shutdown** → 遍历 learners 全 shutdown → isShutdown (幂等守卫); 之后 QuorumPeer 回 LOOKING 重新选举 | 发现 18 (补锚) |
| T5 | 连续性双校验? | 同步期 enforceContinuousProposal (L660) + 服务期 lastQueued+1 警告 (L166-171) — **两阶段连续性校验** | 通过 (验证) |
| T6 | REQUEST 转发? | Learner:266: 客户端写请求 → REQUEST 包转发 leader (Follower 侧) — 读写路径闭环 (learner 收写转 leader, 广播回写) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | learner 同步闭环 | syncWithLeader (PROPOSAL 入队/COMMIT 应用) → UPTODATE 起服务 → 服务期 processPacket — 无缝衔接 ✅ | 通过 |
| V2 | 连续性双校验 | 同步期 enforce + 服务期警告 — 数据缺口即告警 ✅ | 通过 |
| V3 | 快照路径 | writeToTxnLog=true → 先写日志后应用 — 快照后一致性 (Z-9) ✅ | 通过 |
| V4 | 级联拓扑 | observer → follower(ObserverMaster) → leader — 3 层可扩展 ✅ | 通过 |
| V5 | shutdown 幂等 | isShutdown 守卫 — 重复调用无害 ✅ | 通过 |
| V6 | 失守闭环 | 半 tick 检查 → shutdown → LOOKING → 重选举 — 自愈 ✅ | 通过 |
| V7 | INFORM 路径 | observer 只收 INFORM (L697-728) 不参与 ACK — 与 follower 分离 ✅ | 通过 |
| V8 | 读写闭环 | 客户端写 → follower 转发 REQUEST → leader 广播 → COMMIT 回写 — 单 leader 写模型 ✅ | 通过 |

## 新发现问题 (4 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 15 | **补充锚点** | **Follower 服务期主循环**: 同步后 readPacket + processPacket 分发 (PING/PROPOSAL 连续性警告/COMMIT 应用) | 大纲 §3 补注 |
| 16 | **补充锚点** | **同步期分包**: PROPOSAL→packetsNotLogged (enforceContinuousProposal) / COMMIT→processTxn 或延迟写 / **UPTODATE→takeSnapshot+起服务** | 大纲 §3 补注 |
| 17 | **补充锚点** | **ObserverMaster 级联**: follower 可当 observer 的 master (3.6+ 拓扑扩展) | 大纲 §3 补注 |
| 18 | **补充锚点** | **leader shutdown 链**: halt acceptor→断连→zk.shutdown→全 learner shutdown→LOOKING 重选 (isShutdown 幂等) | 大纲 §1 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 lead 启动 (三阶段/epoch/自检/shutdown 链) — 可写 ✅
- §2 两阶段 (propose/双守卫/幂等/reconfig) — 可写 ✅
- §3 同步五分支 (握手链/同步期分包/服务期主循环/级联) — 可写 ✅
- §4 连接仲裁 (大→小/队列) — 可写 ✅
- 负面空间 7 条 — 完整 ✅

## 六次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 8 项全过 (V1-V8); **新发现 4 处全部修复** (服务期主循环/同步期分包/ObserverMaster 级联/shutdown 链)。核心认知: learner 生命周期完整面 — 同步期 (packetsNotLogged+processTxn) 与服务期 (processPacket) 分离, UPTODATE 是切换点。大纲经修复后反写测试全过。

---

# 七次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 追查 quorum 判定/等待/Observer 面六存疑点)

> 动机: 补 Z-2 最后角落 — quorum 判定数学 (QuorumMaj/Hierarchical)、waitForEpochAck 等待与超时、Observer 面、LearnerMaster 抽象。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | QuorumMaj 判定? | **`ackSet.size() > half`** (flexible/QuorumMaj.java, half=votingMembers.size()/2) — 简单多数; **QuorumHierarchical** 对照 (加权/层级) | 发现 19 (补锚) |
| T2 | waitForEpochAck 等待? | electingFollowers 集合 + **isMoreRecentThan 超前拒绝** (follower epoch 领先 → IOException) + **超时 = initLimit × tickTime** (引导期) + electionFinished 通知 (Leader:710 区域) | 发现 20 (补锚) |
| T3 | Observer 同步面? | registerWithLeader(OBSERVERINFO) (Observer:113) + **UPTODATE 不应达 observer** (L193-194 报错 — observer 由 INFORM 驱动) | 发现 21 (补锚) |
| T4 | LearnerMaster 抽象? | 接口 (LearnerMaster.java): addLearnerHandler/waitForStartup/getEpochToPropose/syncTimeout — **Leader 与 ObserverMaster 统一抽象** (级联复用) | 通过 (验证) |
| T5 | syncThrottler 实现? | beginSync/endSync 计数 + 阈值判定 — SNAP/DIFF 独立限流器 | 通过 (验证) |
| T6 | 引导期 vs 正常期? | 引导期 initLimit×tickTime (waitForEpochAck) vs 正常期 syncLimit×tickTime (SyncLimitCheck) — **双超时面** | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | half 数学 | N=3→half=1 需 2; N=5→half=2 需 3; N=2→half=1 需 2 (2 节点需全票 — Oracle 例外根因) ✅ | 通过 |
| V2 | 双超时面 | 引导 (initLimit) vs 正常 (syncLimit) — 阶段不同容错不同 ✅ | 通过 |
| V3 | 超前拒绝闭环 | follower 数据更新 → 拒绝 → 换 leader — 数据安全优先 ✅ | 通过 |
| V4 | observer 只读 | INFORM-only + UPTODATE 报错 — 严格只读 ✅ | 通过 |
| V5 | 抽象复用 | ObserverMaster 实现 LearnerMaster — 级联拓扑同协议 ✅ | 通过 |
| V6 | 加权对照 | QuorumMaj 简单多数 vs QuorumHierarchical 加权 — 配置可插拔 ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 19 | **补充锚点** | **QuorumMaj 判定数学**: ackSet.size() > half (N=3 需 2); QuorumHierarchical 加权对照 — 判定可插拔 (flexible 包) | 大纲 §2 补注 |
| 20 | **补充锚点** | **waitForEpochAck**: electingFollowers + isMoreRecentThan 超前拒绝 + initLimit×tickTime 引导期超时 — 引导期与正常期双超时面 | 大纲 §1 补注 |
| 21 | **补充锚点** | **Observer 面**: OBSERVERINFO 注册 + UPTODATE 不应达 (INFORM-only) — 严格只读 | 大纲 §3 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 lead 启动 (三阶段/epoch/双超时面/shutdown) — 可写 ✅
- §2 两阶段 (propose/双守卫/quorum 数学) — 可写 ✅
- §3 同步五分支 (握手链/生命周期/Observer 面) — 可写 ✅
- §4 连接仲裁 — 可写 ✅
- 负面空间 7 条 — 完整 ✅

## 七次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 6 项全过 (V1-V6); **新发现 3 处全部修复** (QuorumMaj 数学/双超时面/Observer 只读)。Z-2 七轮 REVIEW 覆盖完整: 提案/同步/连接/生命周期/判定数学 — 可支撑写作。
