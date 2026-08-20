# Z-4 Processor 链 — 请求处理管线与读写分离

> 前置: [[Z-3-DataTree]] (Final 应用落点) + [[Z-2-原子广播]] (leader 提案面) | 引出: [[Z-5-Session]] (会话检查面) | 对照: RM-5 (RocketMQ 处理器注册) + Redis 命令处理链 + ES 管线
> 🔴 A | 8 KP | [模式: 责任链 + 读写分离 + 事务预校验]
> Pass 2 闭环: q1(链装配) q2(Prep 预校验) q3(Sync 刷盘) q4(Commit 读写分离+Final)

**读者处境**: 一个写请求从客户端到落树经过几道关? 读请求为什么不等广播? 这篇拆四处理器: Prep (校验+事务生成) → Sync (日志刷盘) → Commit (提交等待+读写分离) → Final (树应用+响应)。

### 1. 链装配 — 四处理器 + Request 封装

场景: 处理器怎么串起来?
源码路径:
- **链式装配** (ZooKeeperServer.setupRequestProcessors): **PrepRequestProcessor → SyncRequestProcessor → CommitProcessor → ToBeApplied → FinalRequestProcessor** (ZooKeeperServer: 链构造); **角色链差异** (五次 REVIEW 认知修正): **follower** = Final→Commit→FollowerRequestProcessor + **Sync→SendAckRequestProcessor 旁路** (落盘后 ACK leader, FollowerZooKeeperServer:67-73 — 写请求经 FollowerRequestProcessor 转发 leader); **observer** = 同 follower + ObserverRequestProcessor, **也写盘** (注释 L100: 防向 leader 请求过旧 txn → SNAP 风暴)
- **Request 封装** (Request.java 561): sessionId/cxid/zxid/type/authInfo/cnxn + **outstandingChanges 关联** (Prep 变更暂存)
- **单线程循环**: 每处理器独立线程 (Prep run L137-163: submittedRequests.take → pRequest) — 队列解耦
- **Request.requestOfDeath**: 毒丸停止信号 (L152)
关键设计 (q1): **生产者-消费者链**: 每级独立线程+队列; 单向流转。[模式: 责任链]

### 2. Prep 预校验 — 事务生成 + 变更暂存

场景: 写请求先做什么检查?
源码路径:
- **pRequest 主流程** (Prep:315+): sessionTracker.checkSession + **pRequest2Txn** (类型分发) + 校验失败 → 错误事务 (ErrorTxn)
- **pRequest2Txn 校验链**: validatePath (路径合法) / **checkACL** (CREATE/DELETE/WRITE 权限) / **checkQuota** (配额超限拒绝, L387) / **getRecordForPath** (读最新状态 — **outstandingChangesForPath 先行** (未提交变更可见!) 树查兜底, L165-190)
- **事务生成**: request.setTxn (CreateTxn/DeleteTxn/SetDataTxn...) + **addChangeRecord** (outstandingChanges 暂存, L198-204); **check OpCode → CheckVersionTxn** (L616-626 — multi 的版本断言原语); **变更清理在 ZooKeeperServer.processTxn** (L1871-1881: 应用时 while peek.zxid ≤ 当前 → remove) + **addCommittedProposal** (L1883: quorum 请求进 committedLog — Z-2 DIFF 数据源)
- **顺序节点**: `parentCVersion → %010d 序号` (L669-671)
- **multi 预校验+回滚**: getPendingChanges (ZOOKEEPER-1624 父记录) + rollbackPendingChanges (L216-251) — **multi 原子性在 Prep 实现** (Z-3 交叉)
- **createSession/closeSession**: trackSession + **closeSession 预计算 ephemeral 清扫** (outstandingChanges 同步块, L573-615)
- **digest**: precalculateDigest 每变更 + setTxnDigest (L561-563)
关键设计 (q2): **Prep = 校验+事务生成+暂存三合一**; outstandingChanges 保证"未提交变更对后续请求可见" (读-改-写一致性)。[模式: 预校验]

### 3. Sync 刷盘 — 批量 + snapCount 快照

场景: 事务日志什么时候写?
源码路径:
- **SyncRequestProcessor** (281): queuedRequests → **toFlush 批量队列** (L85-92, maxBatchSize) → **flush()** (L227+: 写 txnlog — Z-9) → nextProcessor
- **批处理**: poll 超时 (maxWriteQueuePollTime) 或 toFlush 满 → 立即 flush (L166-173)
- **snapCount 快照判定** (L144-151): `logCount > snapCount/2 + randRoll` (随机抖动防同步快照) → **zks.takeSnapshot()** (L193) — DEFAULT_SNAP_COUNT=100000 (ZooKeeperServer:224)
- **读请求旁路**: toFlush 空 → 直接 nextProcessor (L203-211) — 读不落盘
关键设计 (q3): **写批量刷盘 + 读旁路**; snapCount 随机化防快照风暴。[模式: 批量刷盘]

### 4. Commit 读写分离 + Final 应用

场景: 提交等待怎么与读并发?
源码路径:
- **CommitProcessor** (quorum/CommitProcessor, 644): **双队列** — queuedRequests (本地) / committedRequests (leader 提交回包)
- **读直通**: needCommit false && session 无 pending → 直接 next (L251-260) — **读不等广播**
- **写暂存**: pendingRequests **per-session 队列** (L251-255) — **会话内串行, 跨会话并行** (对照 Redis 全局单线程)
- **提交匹配**: committedRequests 头 vs queuedWriteRequests (sessionId+cxid 匹配 L325-349) → 写请求放行
- **waitForEmptyPool**: 处理提交前 drain 读 (L294) — 读写的顺序边界
- **maxReadBatchSize/maxCommitBatchSize**: 批控 (读不 starve 写)
- **FinalRequestProcessor** (680): **applyRequest → zks.processTxn (树应用, Z-3)** (L158) + **响应面**: 错误 hdr → ErrorTxn (L177-191) / **throttled → THROTTLEDOP** (L207-209, **全局节流**: submitRequest++ → Final decInProcess L170) / **cnxn.sendResponse(hdr, rsp)** (L594)
- **ToBeAppliedRequestProcessor**: 中间层 (leader 侧已提交待应用清单, Leader.java:1117)
关键设计 (q4): **读写分离核心**: 读直通 + 写等 commit + per-session 保序; Final 应用+响应一体。[模式: 读写分离]

### 负面空间 — Processor 链刻意不做的事

- **不做全局单线程**: 会话级串行 (对照 Redis 全局单线程) — 跨会话读并行
- **不做写直通**: 写必须过 Sync (落盘) + Commit (广播) — 一致性优先
- **不做请求合并**: 同路径写不合并 (每个写独立事务)
- **不做读副本路由**: 读请求走本节点 (follower 读面有限)
- **不做无日志模式**: Sync 恒在 (对照 Kafka acks=0 直写)
- **不做事务缓存**: outstandingChanges 提交即清 (内存暂存非缓存)

→ 引出: 会话怎么管理? → [[Z-5-Session]]
