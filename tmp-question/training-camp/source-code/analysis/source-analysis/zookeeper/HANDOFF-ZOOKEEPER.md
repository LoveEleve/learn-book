# ZooKeeper 源码分析 — 超详细交接文档 V10 (阶段 4.3, 9/9 收官)

> **⚠ 本文取代 V1-V9 为 ZooKeeper 分域唯一入口** — 前版内容全量并入并升级; 新会话只需读本文 + 规划 (ZOO-PLAN.md)
>
> **日期**: 2026-08-15 | ZooKeeper 3.9.5 (pom.xml:34 实证) | git shallow (1 commit — 时空溯源靠代码内注释锚)
> **入口关系**: 阶段4.3 总入口 (执行计划) | 域规划 `ZOO-PLAN.md` (9 域, 09 审计 4 修正) | 本文 V10 为唯一入口
> **给新 AI**: 本文 9/9 全量交付完成 — 读 §零 (9/9) + §一 (9 域速查) 即可复用知识; 后续进入阶段 4.4 Seata (13 域)。方法论铁律见 §二。

---

## §零 状态总览 (2026-08-15, 9/9 收官)

### 完成状态表

| 域 | 目录 | 类型 | 大纲行数 | 闭环 | questions | 域行数 | REVIEW 发现 | harness |
|:--:|---|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| Z-1 Leader 选举 | outlines/z1-election | 🔴 | 61 | 8 | 20 | 421 | 10 | 4/4 |
| Z-2 原子广播 | outlines/z2-atomic-broadcast | 🔴 | 69 | 8 | 20 | 584 | 21 | 4/4 |
| Z-3 DataTree | outlines/z3-datatree | 🔴 | 58 | 8 | 20 | 467 | 13 | 4/4 |
| Z-4 Processor 链 | outlines/z4-processor-chain | 🔴 | 65 | 8 | 20 | 464 | 14 | 4/4 |
| Z-5 Session | outlines/z5-session | 🔴 | 61 | 8 | 20 | 453 | 14 | 4/4 |
| Z-6 Watcher | outlines/z6-watcher | 🔴 | 61 | 8 | 20 | 447 | 14 | 4/4 |
| Z-7 Client API | outlines/z7-client-api | 🟡 | 61 | 8 | 20 | 459 | 13 | — |
| Z-8 Recipes | outlines/z8-recipes | 🟡 | 65 | 8 | 20 | 442 | 21 | — |
| Z-9 持久化 | outlines/z9-persistence | 🔴 | 65 | 8 | 20 | 436 | 16 | 4/4 |

> REVIEW 发现 = 深审 + 多次 REVIEW 累计编号 (每域 10-21 处; Z-2 七轮最多)。
> 🟡 B 无 harness (Z-7/Z-8); 🔴 域 7/7 harness 全 4/4 (每域自抓 1-2 缺陷; Z-9 自抓 1 处 C2)。

### 执行序 (已完成 9, 全部收官)

```
✅ Z-1 → Z-2 → Z-3 → Z-4 → Z-5 → Z-6 → Z-7 → Z-8 → Z-9
```

### 分层进展

- **共识面 (2)**: Leader 选举 ✅ → 原子广播 ✅ (七轮 REVIEW 21 发现 — 最深)
- **存储面 (3)**: DataTree ✅ → Processor 链 ✅ → 持久化 ✅
- **会话交互面 (2)**: Session ✅ → Watcher ✅
- **客户端面 (2)**: Client API ✅ → Recipes ✅
- **阶段 4.3 全部 9 域收官** → 阶段 4.4 Seata (13 域)

### 交付物统计

- 大纲 (outline.md) 9 域 / 9 篇 / 总计 **566 行** (2026-08-15 实测)
- 域文件全量 **4173 行** / 闭环 72 (每域 8) / questions 180 (每域 20)
- REVIEW 发现累计 **136 处** (各域编号 10-21; 2026-08-15 深度 REVIEW 逐域 grep 复核: Z-1=10/Z-2=21/Z-3=13/Z-4=14/Z-5=14/Z-6=14/Z-7=13/Z-8=21/Z-9=16)
- harness 7/7 域 4/4 全过 (每域自抓 1-2 缺陷)

---

## §一 7 域核心知识速查 (全量固化)

> 每域格式: 核心机制表 / 时空溯源 / 深审发现 / 负面空间。行号均为 3.9.5 实测。

### Z-1 Leader 选举 — 投票共识与全序裁决

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 投票四元组 | Notification: leader/zxid/electionEpoch/state/sid/peerEpoch; CURRENTVERSION=0x2 (FastLeaderElection:112-145,117) |
| 协议兼容 | 28B (ZK-107 前) / 40B (peerEpoch+version) — 三代兼容 (L245-257) |
| 轮次+自荐 | logicalclock++ + updateProposal(自荐, 带最后 zxid) + sendNotifications (L880-908) |
| 指数退避 | 200ms→60s 共 **10 档** (51200→60000 钳制); **连续无票计数器** (仅超时分支递增, 收票不重置); haveDelivered?重发:connectAll (L957-978) |
| 全序三要素 | totalOrderPredicate: **peerEpoch > zxid > sid**, weight==0 排除 (L723-749) |
| 换轮清集 | n.electionEpoch > logicalclock → 清 recvset + 重算提议 (L980-993) |
| 稳定窗口 | finalizeWait=200ms 达多数后仍 poll, 更高票放回重来 (L1047-1066) |
| 双集合 | recvset (当前轮裁决) vs outofelection (历史学习 — 迟到者) (L849-862) |
| FOLLOWING/LEADING 分离 | ZOOKEEPER-3922: 2 节点 majority=2 无法达成 → **QuorumOracleMaj 可插拔裁决** (L1073-1118) |
| 收票判定 | getVoteTracker (双 QuorumVerifier — reconfig) + hasAllQuorums + validVoter 双视图 |

**时空溯源**: 3.4.6 (CURRENTVERSION=0x2) → 3.4.10 (weight 加权) → ZK-107 (40B peerEpoch) → ZOOKEEPER-3922 (FOLLOWING/LEADING 分离 + Oracle) → 3.9 (退避可配)

**深审 (10 处)**: 四要素轮次补全 / 稳定窗口 / 退避 10 档 / Oracle 语义 / 连接分支 (haveDelivered) / Oracle 可插拔 / 退避重置语义 (连续无票) — 推理验证 13 项全过 (全序收敛/换轮正确/双集合隔离/协议兼容)

**负面空间**: 不 term 持久化 (logicalclock 内存轮次)/不随机超时竞选 (全员自荐广播)/不 prevote/不租约/不绝对防脑裂/不成员自动变更

### Z-2 原子广播 — ZAB 两阶段提案与学习者同步

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| ZabState 三阶段 | DISCOVERY → SYNCHRONIZATION → BROADCAST (Leader:644,713,765) |
| epoch 提议 | getEpochToPropose + makeZxid(epoch,0) + newLeaderProposal NEWLEADER (L655-663) |
| 同步等待 | waitForEpochAck (electingFollowers + isMoreRecentThan 超前拒绝 + initLimit×tickTime) → waitForNewLeaderAck (L710-716) |
| quorum 自检 | 每 tickTime/2 检查 synced learners (双 verifier) — 失守 shutdown (L774-816) |
| 两阶段提案 | propose (zxid++ → outstandingProposals → PROPOSAL 广播) → processAck → tryToCommit (L963-1340) |
| 双守卫 | **顺序** (zxid-1 in outstanding → 拒) + **quorum** (hasAllQuorums 双 verifier) (L971-980) |
| 幂等 ACK | lastCommitted >= zxid → 忽略 (L1074-1081) |
| QuorumMaj 数学 | ackSet.size() > half (N=3 需 2; 2 节点需全票 = Oracle 例外根因) |
| reconfig 特例 | designatedLeader + allowedToCommit=false + 链式提交 (L999-1022,1105-1114) |
| 同步五分支 | 空DIFF (已同步) / **TRUNC** (领先, 新 epoch 不截; learner 侧 truncateLog) / DIFF (窗口内) / txnlog 补 / SNAP (兜底) (LearnerHandler:849-879) |
| 握手六步链 | FOLLOWERINFO (configVersion 超前拒) → LEADERINFO → ACKEPOCH → sync (SNAP "BenWasHere" 签名) → UPTODATE → 广播 (L463-653) |
| learner 生命周期 | 同步期 (PROPOSAL→packetsNotLogged + enforceContinuousProposal / COMMIT→processTxn; UPTODATE→起服务) + 服务期 (Follower:100-179) |
| 超时面 | syncTimeout = tickTime×syncLimit; SyncLimitCheck 双槽窗口; 超时停 ping → 断连 (L153-205,1719-1721) |
| 连接仲裁 | **大 sid→小 sid 单方向** (L510-538,635-650); CircularBlockingQueue SEND_CAPACITY 满则丢 |

**时空溯源**: 3.4 两阶段骨架 → 3.5 reconfig (ZOOKEEPER-1783) + 双 verifier → 3.6 syncThrottler + LearnerMaster 抽象 → 3.9 多地址 + 异步连接

**深审 (21 处, 七轮)**: "主推从拉"语义精确化 / 双守卫 / TRUNC-新 epoch 互斥 / 半 tick 自检 / 连接仲裁方向 / reconfig 链式 / pendingSyncs / 队列满语义 / 握手六步链 / syncTimeout 数学 / TRUNC 落点 (learner 侧) / 流控豁免 (follower) / 服务期主循环 / 同步期分包 / ObserverMaster 级联 / shutdown 链 / QuorumMaj 数学 / 双超时面 / Observer 严格只读 — 推理验证 27 项全过

**负面空间**: 不 Raft 流水线 (全序提案)/不 pipelined commit/不 learner 拉取 (leader 推)/不 observer 投票/不强制同步落盘/不无 quorum 写/不乱序 reconfig

### Z-3 DataTree — 内存状态树与事务应用

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 扁平树 | NodeHashMap 路径→节点 ("tree is the source of truth") + **根双 key "" + "/"** (L288-289) |
| digest | NodeHashMapImpl pre/postChange 增量计算 tree digest; DIGEST_LOG_LIMIT=1024/INTERVAL=128 (L169-173); 历史 + DigestWatcher |
| DataNode | data (volatile) + StatPersisted (czxid/mzxid/pzxid/cversion/version/ephemeralOwner) + children + acl (引用 id) |
| 分类集合 | ephemerals (session→paths) / containers / ttls + ReferenceCountedACLCache; **单线程写模型** (Final 顺序应用 — killSession 注释 L1122-1127) |
| createNode | 父锁 → ACL 先入缓存 (fuzzy 注释 L446-457) → cversion/pzxid 单调 (L470-478) → 分类 → **quota 两段式 (Prep 检查/树计数)** → 双 watch (L433-522) |
| deleteNode | pzxid 保护 (zxid>pzxid) → aclCache.removeUsage → 分类清理 → 三 watch (L533-625) |
| setData | 版本乐观锁 → mzxid/mtime/version++ (L627+) |
| processTxn | OpCode 分发 (create 族/delete 族/setData/setACL/multi 子事务 L1028); TxnDigest 校验; **multi 原子性在 Prep** |
| 快照面 | serializeAcls+DFS ("/" 结束标记) ↔ deserialize (父链/分类/ACL 重建 — 父缺失 IOException) (L1322-1388) |
| ZKDatabase | committedLog (ArrayDeque — Z-2 DIFF 数据源) + snapLog (FileTxnSnapLog) + loadDataBase→restore |
| quota | PathTrie 前缀索引 + updateQuotaStat; 超限拒绝在 Prep (checkQuota) |

**时空溯源**: 3.4 树骨架 → 3.5 tree digest + TxnDigest → 3.6 containers/ttls (EphemeralType) → 3.9 fuzzy snapshot 双保护 (ACL 先入 + cversion 单调)

**深审 (13 处)**: digest 非纯 CHM / 根双 key (harness 实证) / 父锁语义 / fuzzy 双保护 / digest 历史 / quota 面 / multi 预校验 / 单线程写模型 / quota 两段式 / 读挂 watch — 推理验证 19 项全过

**负面空间**: 不真实树指针/不节点级锁 (父锁)/不路径压缩 (PathTrie 仅 quota)/不数据压缩/不子排序/不拆大节点

### Z-4 Processor 链 — 请求处理管线与读写分离

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 链装配 | Prep→Sync→Commit→ToBeApplied→Final (setupRequestProcessors); **角色链差异**: follower = Final→Commit→FollowerRequestProcessor + **Sync→SendAck 旁路** (落盘 ACK leader, FollowerZooKeeperServer:67-73); observer 同 + **也写盘** (防 SNAP 风暴 L100) |
| Prep 校验链 | checkSession/checkACL/checkQuota/validatePath (pRequest2Txn L315-637) |
| outstanding | getRecordForPath 先行 (未提交变更可见 — 读-改-写一致性) + addChangeRecord (L165-204); **清理在 ZooKeeperServer.processTxn** (L1871-1881) |
| 顺序节点 | %010d parentCVersion (L669-671) |
| multi 原子性 | getPendingChanges (ZOOKEEPER-1624 父记录) + rollback — 在 Prep (L216-251) |
| closeSession | outstandingChanges 同步块清扫预计算 + CloseSessionTxn 路径列表 (L573-615) |
| Sync 批量 | toFlush (maxBatchSize) + 超时双触发 (L85-92,160-215) |
| snapCount | logCount > snapCount/2 + randRoll → takeSnapshot; DEFAULT=100000 (L144-151) |
| 读旁路 | toFlush 空 → 直通 next (L203-211) |
| 读写分离 | Commit: 读直通 (L251-260) / 写 per-session 等 commit (L251-255) / 匹配 sessionId+cxid (L325-349) / waitForEmptyPool (L294) |
| Final | applyRequest → processTxn (L158) + 响应 sendResponse (L594) + 节流 THROTTLEDOP (L207-209) |
| 全局节流 | RequestThrottler: maxRequests=0 默认关 / dropStale 关连接 / shouldThrottleOp → isThrottled (L145-192) |

**时空溯源**: 3.4 四处理器骨架 → 3.5 multi 回滚 + digest 进事务 (ZOOKEEPER-1624) → 3.6 读写分离增强 + CloseSessionTxn → 3.9 throttling

**深审 (14 处)**: per-session 串行模型 / ToBeApplied 中间层 / outstanding 先行 / multi 在 Prep / 节流面 / **follower Sync→SendAck 旁路 (认知修正)** / observer 写盘 / 变更清理时机 / check OpCode / 角色链差异 — 推理验证 19 项全过

**负面空间**: 不全局单线程 (会话级)/不写直通/不请求合并/不读副本路由/不无日志/不事务缓存

### Z-5 Session — 会话生命周期与过期分桶

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 双索引 | sessionsById CHM + sessionExpiryQueue (ExpiryQueue<tickTime>) (SessionTrackerImpl:49-51) |
| 三态 | isClosing 显式标志 + isActive/isExpired 派生 (L56-78); closing 拒绝 touch (L187-190) |
| 分桶数学 | roundToNextInterval 向上取整 (宽限期); 桶数 ≤ maxTimeout/interval (ExpiryQueue:39-55) |
| 桶迁移 | update: 新桶 add + elemMap 旧桶 remove (L84-103) |
| 过期循环 | run: getWaitTime→sleep→poll→setSessionClosing+expirer.expire (L158-172) |
| **过期事务化** | expire → close(sessionId) = closeSession 请求 submitRequest — 走正常链 (ZooKeeperServer:702-706,728-740) |
| sessionId 位结构 | serverId<<56 + 时间戳<<24>>>8 (L98-108); CONTAINER 特值跳过 (L104-106) |
| 超时钳制 | min=2×tick / max=20×tick (ZooKeeperServer:1394,1403); 协商钳制在 processConnectRequest (L1467+) |
| 恢复 | dumpSessions (L127-140) + 构造重放 trackSession (L110-117) |
| 本地会话 | localSessionEnabled + LearnerSessionTracker + **upgradeSession 防竞态** (remove 单线程拿) + upgradingSessions 中间态 |
| connThrottle | 连接权重 tokens (ZooKeeperServer:1474-1490) — 3.6+ 防连接风暴 |

**时空溯源**: 3.4 骨架 (分桶宽限期注释) → 3.5 本地会话 (LearnerSessionTracker) → 3.6 CONTAINER 特值 + connThrottle → 3.9 指标细化

**深审 (14 处)**: 三态精确化 / 宽限期 / sessionId 位结构 / closing 防竞争 / expirer 链 / LearnerSessionTracker / 会话恢复 / connThrottle / **过期事务化** / 协商钳制 / upgrade 防竞态 — 推理验证 19 项全过

**负面空间**: 不毫秒精度 (桶粒度+宽限期 ≤2×tick)/不独立定时器/不无状态/不专用心跳/不租约协商

### Z-6 Watcher — 双向注册与一次性触发

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 双实现 | 标准 (watchTable/watch2Paths + synchronized) vs **Optimized (pathWatches BitHashSet + watcherBitIdMap 位图 + RWLock + 懒清理)** (L61-64,79-80) |
| ⚠ 无 sessionWatches | 执行计划断言错误 (五次 REVIEW 修正) — 会话清理靠 deadWatchers 懒批量 (L200-201) |
| 双向索引 | watchTable (path→watchers) + watch2Paths (watcher→paths, WatchStats) (L50-52) |
| 懒创建 | HashSet(4) 4th 翻倍 (L83-85) |
| 死 watcher | isDeadWatcher (cnxn 关闭) 忽略 add (L76-78) |
| 触发链 | PathParentIterator 双模式 (forAll 递归/forPathOnly 仅路径 L370-374) + **STANDARD 触发即移除** (L161-167) + PERSISTENT_RECURSIVE 保持 (L168-170) + suppress (L185-187) + WatchStats.NONE 判定 (L162-167) |
| 投递 ACL | NIOServerCnxn.process → **checkACL (READ) — NoAuth 丢弃** → NOTIFICATION_XID + sendResponse (L710-736) |
| WatcherMode | STANDARD/PERSISTENT/PERSISTENT_RECURSIVE + DEFAULT (WatcherMode.java:23-29) |
| 清理 | removeWatcher 全清 (L113-132) vs Optimized deadWatchers 懒批量 |
| 统计 | getWatchesSummary/getWatchesByPath/getWatchesBySession (L308+) |

**时空溯源**: 3.4 骨架 (HashSet(4) 注释) → 3.5 Optimized (位图 + 懒清理注释 L47-48) → 3.6 WatcherMode (ZOOKEEPER-2251) → 3.9 WatchStats 细化

**深审 (14 处)**: 位图核心 / RWLock / 懒清理 / watchers 去重 / PathParentIterator 双模式 / WatchStats.NONE / removeWatch API / 统计查询 / **sessionWatches 断言修正** / 投递 ACL 过滤 — 推理验证 19 项全过

**负面空间**: 不持久事件历史 (错过即丢)/不超时/不跨节点同步/不递归默认/不排序保证

### Z-7 Client API — 客户端门面与双线程协议

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 门面 | 同步/异步双 API (异步内核 + notifyAll 同步封装, ClientCnxn:725-761) |
| WatchRegistration | Exists/Data/Child/AddWatch 四族 — 响应成功才注册 (ZooKeeper:265-362) |
| 双线程 | SendThread (连接+收发+ping) / EventThread (事件+回调串行, L469+) + queueEventOfDeath |
| 三队列 | outgoingQueue/pendingQueue/事件队列 (L148-153) |
| XID | **惰性分配** (发送时 setXid, L334-336 + NIO:115; **ping/auth 不分配不入 pending**); 特殊值 PING=-2/AUTH=-4/SET_WATCHES=-8/NOTIFICATION=-1 (L120-127) |
| primeConnection | ConnectRequest (重连带 sessionId) + setWatches 分批重注册 (五类, SET_WATCHES_MAX_LENGTH) + auth 重放 (L1005-1095) |
| 超时双面 | 会话 (expirationTimeout-idleRecv) / 连接 (connectTimeout) (L1244-1258) |
| 连接面 | hostProvider 多地址轮询 + chroot 前缀 + read-only 退避 (pingRwTimeout 指数 100→60000) (L1097-1112,1278-1284) |
| finishPacket | 注册/结算: watchRegistration + WatchRemoved 事件 + cb→eventThread 或 notifyAll (L725-761) |
| queuePacket 闭包 | 关闭后入队 → conLoss 立即结算; closeSession 标记 closing (L334-360) |
| 顺序强校验 | Xid out of order 异常 (L945-949) |
| conLossPacket | 连接丢失全量结算 — 错误码按状态 (AUTHFAILED/SESSIONEXPIRED/CONNECTIONLOSS) (L782-797) |

**时空溯源**: 3.4 骨架 (XID 注释锚) → 3.5 SASL + read-only → 3.6 setWatches2 (持久 watch) → 3.9 DISABLE_AUTO_WATCH_RESET + WatchRemoved

**深审 (13 处)**: 三队列/XID 特殊值/倒序入队/setWatches 分批/conLoss 结算/queueEventOfDeath/chroot/hostProvider/read-only/SASL 降级/**XID 惰性分配/queuePacket 闭包/顺序强校验** — 推理验证 19 项全过

**负面空间**: 不协议扩展 (单连接串行)/不自动重试业务 (at-most-once)/不本地缓存/不 watch 持久化/不压缩

### Z-8 Recipes — 顺序节点三件套: 选举/锁/队列

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 共同模式 | **顺序节点 + 最小序号 + 前驱 watch** — "最小者胜" 全序互斥; 锁=选举同算法 (Javadoc "exclusive write lock or to elect a leader" WriteLock:36-37) |
| 节点生命周期 | 选举/锁 **EPHEMERAL_SEQUENTIAL** (会话绑定, 断开清理 — LES:176/WriteLock:196) vs 队列 **PERSISTENT_SEQUENTIAL** (数据存续 — DQueue:265); 前缀 n_/x-\<sid\>-/qn- |
| 最小序号判定 | 选举 IdComparator 找下标 0 (LES:207-224) / 锁 TreeSet first+headSet (WriteLock:229-253) / 队列 TreeMap\<Long\> 队头 (DQueue:65-87); ⚠ 位宽不一致 (Integer vs Long, %010d 溢出) |
| 前驱 watch 双触发面 | 选举/锁 **exists 单节点** (NodeDeleted) vs 队列 **getChildren 整目录** (ChildrenChanged); 注册在 exists/getChildren 上 |
| ⚠ exists 双语义 | OK → 删除 watch; **NONODE → 创建 watch** (ZooKeeper:316-322) — 顺序节点名唯一永不再创建 → 死 watch |
| 前驱消失竞态 | **三处三种处理**: becomeReady stat==null → 递归重读 (LES:248-257) / toLeaderOffers getData NoNode → 直接 FAILED (LES:310) / WriteLock 只 log.warn → **永久悬挂** (L239-243) — 死 watch 三重实证 (客户端 existWatches+服务端 statNode 注册+唯一名) |
| 断连期事件丢失 | 无事件队列 + setWatches 不补已删节点 (Z-7 交叉) → 前驱断连期死亡静默 → 悬挂; 会话事件送达全部 watcher (ZKWatchManager:345-370) → LockWatcher 空转, 但 LES 无 SyncConnected 重判钩子 |
| 选举状态机 | 7 State (START/OFFER/DETERMINE/ELECTED/READY/FAILED/STOP) + 12 EventType (进出双事件, LES:443-469) |
| 会话失效三态度 | LES: 忽略会话事件 + 排除自身 NodeDeleted → **静默停留 ELECTED** (双主窗口, Javadoc "best effort") / WriteLock: SessionExpired 直抛 (ProtocolSupport:127-129) / 队列: 无感 (持久节点) |
| 重试面 | RETRY_COUNT=10 + retryDelay=500ms 线性退避 attempt×500; ConnectionLoss 重试 / SessionExpired 直抛 (ProtocolSupport:121-140) |
| 幂等创建 | findPrefixInChildren "x-\<sessionId\>-" 前缀扫描 — create 响应丢失恢复 (WriteLock:185-201,216-218) |
| 队列消费 | take 注册即读 + LatchChildWatcher 阻塞 (DQueue:232-242) / remove getData+delete NoNode 重试 (L160-162) — **at-most-once 无 ack**; ⚠ 自举竞态 create(dir) NodeExists 未捕获 (L236,268) |

**时空溯源**: 3.4 recipes 骨架 (ephemeral 注释锚 L122-124/L216-218/L228) → 3.5 ZNodeName Optional 现代化 → 3.6 负序号/双横线修复 (L59-61) → 3.9 SpotBugs + JUnit5 (pom 实证)

**深审 (21 处, 五轮)**: 悬挂竞态 (前驱消失 exists NONODE 死 watch 三重实证) / 会话失效静默 / 无 watch 悬挂路径 (id 陈旧) / Integer 溢出 / 自举竞态 / LockWatcher 不筛事件 + 会话事件送达实证 / 断连期事件丢失 (无 SyncConnected 重判钩子) / toLeaderOffers NoNode → FAILED (前驱消失三处三种处理) / 无前缀校验 / process 并发无锁 / unlock 残留 watch — 推理验证 22 项全过

**负面空间**: 不服务端原语/不会话失效自愈/不锁超时 (对照 Curator acquire(timeout))/不 ack/不重试策略抽象/不续约自证

### Z-9 持久化 — 双文件格式与恢复双路径

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| TxnLog 格式 | FileHeader [ZKLG\|ver2\|dbid] + 记录链 **[CRC 8B Adler32][len 4B][payload][0x42 'B']** + ZeroPad (FileTxnLog:60-96,275-327); 文件名 log.\<hex\> (Util:84-86) |
| ⚠ CRC 覆盖 | **只覆盖 payload** (crc.update(buf) L316-317) — **Javadoc L77-78 声称含 len+0x42 与实现不符**; len/0x42 无保护 → 损坏 → **静默 EOF 截断** (readTxnBytes L157-173, harness B2/B3 实证) |
| 容错二分 | **尾部残缺 (缺 0x42/len 截断) → EOF 静默跳文件** vs **payload 中部损坏 → CRC_ERROR 致命** (FileTxnIterator:806-824); 空尾文件自动删除恢复 (L715-737) |
| 预分配 | preAllocSize 默认 **64MB** (FilePadding:30), position+4096 阈值 (L101-115); commit: flush + **force(false) (forceSync 默认 yes)** + fsync>1000ms 告警 + 关旧流留新 (L394-443) |
| 快照格式 | [ZKSN\|ver2\|**dbid=-1**] + 树 + **三段独立 seal (writeLong(Adler32)+writeString("/"))**: 树/zxidDigest/lastProcessedZxid (FileSnap:250-267, SnapStream:162-180); **回退最多 100 个** (FileSnap:77); fsync → **AtomicFileOutputStream 原子替换** (SnapStream:133) |
| restore 双路径 | deserialize 快照 → **fastForwardFromEdits read(lastProcessedZxid+1)** (L330, fuzzy 快照语义) → processTransaction + **compareDigest** (L351); 目录 = **/version-2** + 交叉污染检查 (ZOOKEEPER-2967, 仅异目录) |
| 空库边界 | 无快照有日志 → 默认 throw "Something is broken!" (ZOOKEEPER-2325) / **trustEmptySnapshot 升级逃生舱** (ZOOKEEPER-3056) / 空库 → save 空快照 → 0 (ZOOKEEPER-2325) |
| truncate | **exclusive: setLength 移除 ≥zxid** + 删后续文件 (FileTxnLog:481-501); 触发: learner 领先 → **TRUNC zxid = maxCommittedLog** (LearnerHandler:842-846) |
| fuzzy 容错 | 快照中后期事务混入 → 重放 NONODE/NODEEXISTS 安全忽略 (FileTxnSnapLog:445-453) |
| 清理 | PurgeTxnLog **num≥3** + **getSnapshotLogs 守卫** (log.(X-a) 可能含 >X 事务, PurgeTxnLog:79-166); snapCount 100000 随机触发 (SyncRequestProcessor:146-151); txnLogSizeLimitInKb 默认 -1 (3.9); 快照压缩 GZIP/SNAPPY 默认 CHECKED (SnapStream:64-93) |

**时空溯源**: 3.4 双文件骨架 (格式 Javadoc 锚) → 3.5 启动边界 (ZOOKEEPER-1161/2325/2967/3056) → 3.6 压缩+原子写+zxidDigest seal → 3.9 txnLogSizeLimit + ZOOKEEPER-3781

**深审 (16 处, 五轮)**: CRC 覆盖 Javadoc 不符 / len 无保护静默截断 / 容错二分 / dbid 不对称 / 空快照文件删除 / fuzzy 容错 / digest 覆盖警告 / 全文件扫描 / 同目录跳过检查 / fsync 指标面 / **truncate 不完全截断边沿** / **快照重试残留面 (分类集合不清)** / **学习者起点守卫** — 推理验证 17 项全过 + harness 11/11

**负面空间**: 不单文件库 (双文件 WAL+快照)/不事务内联落盘 (tick 批量)/不组提交/不日志压缩加密/不中部损坏自愈 (TxnLogToolkit 人工)/不无限保留 (Purge 最小 3)

---

## §二 方法论执行报告 (7 域实证, V8 更新)

### 1. 域级怀疑审计修正汇总 (执行计划 9 域)

| 修正 | 证据 | 落点 |
|:--|:--|:--|
| **Z-8 类名张冠李戴**: 执行计划 "LeaderLatch/InterProcessMutex" 是 **Curator** 类; ZK 官方 recipes 为 LeaderElectionSupport/WriteLock/DistributedQueue | zookeeper-recipes 扫描 | ZOO-PLAN v1 |
| **Java 客户端归属**: zookeeper-client 模块仅 **C 客户端** (zookeeper-client-c); Java ZooKeeper (3118)/ClientCnxn (1751) 在 **zookeeper-server 模块** | 模块扫描 | ZOO-PLAN v1 |
| **WatchManager 双实现**: 执行计划未提 — WatchManager (376) + **WatchManagerOptimized (412)** | server/watch/ 扫描 | ZOO-PLAN v1 |
| **sessionWatches 断言错误** (Z-6): "按 session 分桶" 不存在 — 位图按路径+watcher; 会话清理靠 deadWatchers 懒批量 | WatchManagerOptimized grep | ZOO-PLAN 补录 (V8) |
| **follower Sync→SendAck 旁路** (Z-4): 初判 "follower 无 Sync" 错误 — follower 链 = Final→Commit→FollowerRequestProcessor + **Sync→SendAck** (落盘 ACK leader) | FollowerZooKeeperServer:67-73 | Z-4 速查 |
| **WriteLock 前驱消失竞态** (Z-8): 初判 "exists 失败会重试" 错误 — stat==null 只 log.warn; 且 exists 在 NONODE 下注册**创建 watch** (ZooKeeper:322) — 顺序节点名唯一 → **死 watch 永久悬挂**; LES 同类竞态走递归重读 — 同模式两处理 | WriteLock:239-243 + ZooKeeper:316-322 | Z-8 速查 |
| **会话失效三态度** (Z-8): LES 静默 (忽略会话事件, 自身 NodeDeleted 排除 — 双主窗口) / WriteLock 直抛 / 队列无感 (持久节点) | LES:327-341 + ProtocolSupport:127-129 | Z-8 速查 |
| **CRC 覆盖 Javadoc 不符** (Z-9): 声称 "calculated across payload -- Txnlen, TxnHeader, Record and 0x42" — 实现 crc.update(buf) **只覆盖 payload**; len/0x42 无保护 → 损坏 → 静默 EOF 截断 (harness B2/B3 实证) | FileTxnLog:77-78 vs 316-319 | Z-9 速查 |

### 2. 发现问题类型统计 (深审+多次 REVIEW, 累计 132 处)

| 类型 | 数量 | 代表 |
|:--:|:--:|:--|
| **认知修正** | 7 | "主推从拉"语义精确化 (Z-2) / follower Sync→SendAck 旁路 (Z-4) / sessionWatches 断言 (Z-6) / root 双 key (Z-3) / 前驱消失悬挂竞态 (Z-8) / 会话失效静默 (Z-8) / **CRC 覆盖 Javadoc 不符** (Z-9) |
| **表述精确化** | 10+ | 三态派生 (Z-5) / multi 原子性在 Prep (Z-4) / digest 非纯 CHM (Z-3) / 五分支非三模式 (Z-2) / 前驱 watch 双触发面 (Z-8) / **容错二分三分** (Z-9) |
| **补充锚点** | 95+ | 每域 8-15 处 (默认值/边界/交叉面/协议锚) |
| **语义标注** | 20+ | 退避重置 (连续无票) / 队列满丢 (Z-2) / 过期事务化 (Z-5) / 投递 ACL (Z-6) / conLoss 结算 (Z-7) / exists NONODE 创建 watch (Z-8) / **len 无保护静默截断** (Z-9) |
| harness 自抓缺陷 | 9 | 退避序列长度 10 非 9 (Z-1) / 跳跃 zxid 模拟不成立 (Z-2) / 根双 key (Z-3) / 触发计数语义 (Z-6) / **C2 破坏点选择** (Z-9) 等 |

### 3. 方法论铁律 (V8 版, 7 域实证)

1. **执行计划是待验证假设** — ZK 执行计划修正 4 处 (类名/模块归属/双实现/sessionWatches); 域内初判也可能错 (follower Sync)
2. **harness 必须能自抓缺陷** — 6/6 harness 每域自抓 1-2 处 (自身断言错误 = 语义未吃透的信号)
3. **REVIEW 追加** — 用户每次"深度 review"要求 = 新轮次; Z-2 七轮最深
4. **时空溯源靠注释锚** — git shallow (1 commit) 无法考古; CURRENTVERSION/ZOOKEEPER-1624/3922/2251 编号是唯一锚
5. **双链禁链未交付域** — 引出只指向已存在域
6. **09 审计 4 修正全部是"读源码前断言 vs 读后现实"的差距** — 数字/类名/模块归属必须穷举

---

## §三 高频坑汇总 (跨域 51 条)

### Z-1 (5)
1. **epoch > zxid > sid 全序** — weight==0 先排除
2. **退避是连续无票计数器** — 收票不重置 notTimeout
3. **recvset 只收当前轮** — 历史轮进 outofelection
4. **finalizeWait=200ms 稳定窗口** — 达多数不代表当选
5. **2 节点 Oracle** — majority=2 永远达不成 (ZOOKEEPER-3922)

### Z-2 (8)
6. **"主推从拉"精确语义** — 从库控制节奏, 主库推数据
7. **顺序守卫 zxid-1** — 提案可乱序到, 提交必须连续
8. **TRUNC 与新 epoch 互斥** — 低 32 位=0 不截 (无 txnlog)
9. **握手六步** — FOLLOWERINFO→LEADERINFO→ACKEPOCH→sync→UPTODATE→广播
10. **syncTimeout = tickTime×syncLimit** — 超时停 ping → 断连
11. **连接仲裁 "大→小"** — 每对节点一条确定性方向
12. **follower Sync→SendAck 旁路** — 落盘即 ACK leader
13. **observer 也写盘** — 防过旧请求 → SNAP 风暴

### Z-3 (6)
14. **根双 key "" + "/"** — 写路径查 "", 序列化 "/"
15. **cversion/pzxid 单调** — replay 防回退
16. **树写单线程** — Final 顺序应用, 分类集合免锁
17. **quota 两段式** — Prep 检查拒绝, 树层只计数
18. **digest 增量** — pre/postChange 钩子
19. **multi 原子性在 Prep** — 树层无回滚

### Z-4 (5)
20. **读直通 / 写 per-session 等 commit** — 会话内串行跨会话并行
21. **outstandingChanges 先行** — 读-改-写一致性
22. **snapCount 随机化** — logCount > snapCount/2 + randRoll
23. **多地址匹配 sessionId+cxid** — 本地写 vs 远端写
24. **节流默认关** — maxRequests=0

### Z-5 (5)
25. **过期也是事务** — expire → closeSession 请求走链
26. **closing 拒绝 touch** — 过期与续期竞态保护
27. **sessionId 高 8 位 serverId** — 跨服唯一
28. **超时钳制 [2×tick, 20×tick]** — 协商即钳
29. **upgrade 防竞态** — remove 单线程拿 timeout

### Z-6 (3)
30. **无 sessionWatches** — 执行计划错误; 位图按路径+watcher
31. **STANDARD 触发即移除** — 一次性语义
32. **投递时 ACL 过滤** — checkACL (READ), NoAuth 丢弃

### Z-7 (3)
33. **XID 发送时分配** — ping/auth 不入 pendingQueue
34. **queuePacket 闭包** — 关闭后入队立即 conLoss 结算
35. **重连恢复双动作** — sessionId + setWatches 重注册

### Z-8 (7)
36. **前驱消失竞态三处三处理** — becomeReady 递归重读 / toLeaderOffers 直接 FAILED / WriteLock 悬挂 (exists NONODE = 创建 watch, 唯一名永不再触发)
37. **断连期事件丢失** — 前驱断连期间死亡 → 无事件队列 + setWatches 不补已删节点 → 悬挂; recipes 无 SyncConnected 重判钩子
38. **会话失效三态度** — LES 静默 (双主窗口) / WriteLock 直抛 / 队列无感
39. **序号位宽不一致** — Integer (选举/锁) vs Long (队列); %010d 2.1B 次后溢出
40. **锁=选举同一算法** — "exclusive write lock or to elect a leader" (WriteLock:36-37)
41. **队列 at-most-once** — take 即删无 ack; remove 无界重读 busy loop; offer 无幂等 (响应丢失重复入队)
42. **unlock 残留 watch** — 前驱后死 → 重建节点重新竞争, 违反 "removes your request" Javadoc

### Z-9 (7)
43. **CRC 只覆盖 payload** — Javadoc 声称含 len+0x42 不符; len/0x42 损坏 → 静默 EOF 截断 (非 CRC 错误)
44. **容错二分** — 尾部残缺 = EOF 容忍 (崩溃现场) / payload 中部损坏 = CRC 致命; 空尾文件自动删除
45. **restore 从 zxid+1 重放** — fuzzy 快照可能已含到 X 的事务; 双侧对称 (truncate exclusive 移除 ≥zxid)
46. **快照 dbid=-1 常量** vs txnlog 动态 dbid — 不对称
47. **TRUNC zxid = maxCommittedLog** — learner 领先时截断到 leader 最新提交, 再重放
48. **PurgeTxnLog num≥3 + getSnapshotLogs 守卫** — log.(X-a) 可能含 >X 事务, 删之快照 X 不可恢复
49. **快照回退 100 个 + 原子写** — findNValidSnapshots(100); fsync 时 AtomicFileOutputStream rename
50. **快照重试残留面** — DataTree.deserialize 只清 nodes/pTrie, ephemerals/containers/ttls/aclCache 不清 → 中途失败残留合并 (zombie 面)
51. **truncate 不完全截断边沿** — 无 <zxid 日志文件 (purge 删旧) → 首个 ≥zxid 事务幸存, 靠 fuzzy 容错兜底

---

## §四 阶段 4.3 收官 — 无剩余域

- ✅ **9/9 全量交付** (2026-08-15): 大纲 566 行 / 域文件 4173 行 / REVIEW 136 处 / harness 7/7 4/4
- ✅ **后续**: 阶段 4.4 Seata (13 域) — Curator (4.5, 5 域, 依赖 ZK 已满足)
- ✅ **跨域复用**: 本文 §一 9 域速查 = 阶段 4.4/4.5 的 ZK 知识面 (选举/广播/树/链/会话/watch/客户端/recipes/持久化)

---

## §五 完成检查单 (阶段 4.3 收官)

- [x] 9/9 域全量交付 (2026-08-15): §零 状态表 / §一 9 域速查 / §三 49 坑 / §四 收官
- [x] 每域: Pass 0-3 + 六层深审 + 时空溯源 + harness (🔴 7/7) + 多次 REVIEW (132 发现)
- [x] 每域 REVIEW 记录真实问题 (零发现=不合格 — 全部域均有真实发现)
- [x] HANDOFF-STAGE3 注记已更新 (阶段 4.3 → 9/9)

---

## §六 文件路径

```
analysis/source-analysis/zookeeper/
├── ZOO-PLAN.md        ← 9 域规划 (v1, 09 审计 4 修正)
├── HANDOFF-ZOOKEEPER.md ← 本文 V10 (阶段 4.3 收官, 唯一入口)
├── outlines/
│   ├── z1-election/   z2-atomic-broadcast/   z3-datatree/
│   ├── z4-processor-chain/   z5-session/   z6-watcher/   z7-client-api/
│   ├── z8-recipes/    z9-persistence/  (每域 9 文件)
├── harness/
│   ├── z1-election/MiniZKElection.java    z2-atomic-broadcast/MiniZKZab.java
│   ├── z3-datatree/MiniZKTree.java        z4-processor-chain/MiniZKChain.java
│   ├── z5-session/MiniZKSession.java      z6-watcher/MiniZKWatcher.java
│   └── z9-persistence/MiniZKTxnLog.java
源码: /data/workspace/source-code/code/spring/zookeeper/  (ZooKeeper 3.9.5)
上级: ../HANDOFF-STAGE3.md (阶段3 总入口 — 阶段 4.3 状态 9/9 收官注记)
后续: 阶段 4.4 Seata (13 域) — Curator (4.5, 5 域, 依赖 ZK 已满足)
```