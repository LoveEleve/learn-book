# C-2 Leader 选举 — 知识规划 (KP)

> 域级: 🔴 A | 模块: curator-recipes/.../recipes/leader/ (7 文件: LeaderLatch 683 / LeaderSelector 558 / LeaderSelectorListenerAdapter 36 / Participant 93 / CancelLeadershipException 44 / LeaderLatchListener 46 / LeaderSelectorListener 49)
> 日期: 2026-08-15 | 版本: 5.8.0

## 一、机制提取 (逐源)

### M1 LeaderLatch: 事件通知式选举 (683)
- 字段: state (LATENT/STARTED/CLOSED, L93-97) / hasLeadership AtomicBoolean / ourPath + **lastPathIsLeader** 双路径 (L69-72) / CloseMode (SILENT/NOTIFY_LEADER, L102-112)
- LOCK_NAME="latch-" (L84); 排序器复用 lock 包 (L86-91)
- **start()**: CAS LATENT→STARTED → 提交任务等连接建立后 internalStart (L150-163)
- **internalStart()** (synchronized): 注册连接监听 → reset() (L524-534)
- **reset()** (L489-522): setLeadership(false) → setNode(null) → 后台创建 **EPHEMERAL_SEQUENTIAL + withProtection** (L516-521) → 回调 OK → setNode + getChildren()
- **checkLeadership(children)** (L539-602):
  - 排序 (LockInternals.getSortedChildren) → ourIndex
  - ourIndex < 0 → reset() (L550-554)
  - **ourIndex == 0 → 二次确认**: getData 校验 **ephemeralOwner == 本会话 sessionId** (L556-573) → 通过 → lastPathIsLeader.set + setLeadership(true)
  - 非 0 → setLeadership(false) + **watch 前驱 (ourIndex-1)**: getData().usingWatcher(watcher).inBackground (L575-601, 注释: getData 而非 exists 防 watcher 泄漏 L600); NodeDeleted → getChildren; NONODE → 重试 getChildren
- **await()** (L307-316): synchronized while(STARTED && !hasLeadership) wait(); 非 STARTED 抛 EOFException; 带超时版 nanos 递减 (L356-379); setLeadership notifyAll 唤醒 (L673)
- **setLeadership** (L665-674): 仅翻转时通知监听者 (isLeader/notLeader) + notifyAll
- **close()** (L172-226): CAS STARTED→CLOSED → cancelStartTask → setNode(null) (guaranteed 删) → removeWatchers → 按 CloseMode 决定是否先通知 notLeader
- **handleStateChange** (L630-663): RECONNECTED → 复查 getChildren; SUSPENDED → 按 errorPolicy 决定; LOST → setLeadership(false)
- participants/getLeader 复用 LockInternals.getParticipantNodes (L416-439)

### M2 LeaderSelector: 持有式选举 (558)
- **复用 InterProcessMutex** (L70-77); 类注释: "uses an underlying InterProcessMutex... leader election is 'fair' - each user will become leader in the order originally requested" (L60-64)
- 匿名子类覆写 getLockNodeBytes() 写入 id (L161-166, InterProcessMutex.java:188-190)
- **start()**: CAS → 注册 WrappedListener → requeue() (L214-222)
- **doWork()** (L422-467): taskStarted → **mutex.acquire()** (L426) → hasLeadership → **listener.takeLeadership(client) 阻塞** (L436) → 用户返回 → taskDone → mutex.release() (L451); release 前先清中断位 (L448-464)
- **doWorkLoop** (L469-486): 捕获 ConnectionLoss/SessionExpired → autoRequeue 开启则吞掉重试, 否则重抛
- **taskStarted/taskDone** (L355-378): ourThread/ourTask 簿记; taskDone 清标志 (注释论证 release 与下个 acquire 并行安全)
- **cancelElection/interruptLeadership** (L383-401)
- **WrappedListener** (L546-556): 转发 stateChanged; 捕获 **CancelLeadershipException → cancelElection()** (注释 "dated leadership" 问题 L550-553)
- **autoRequeue** (L182-184): taskDone 后自动重排

### M3 支持面
- LeaderSelectorListener 接口: 继承 ConnectionStateListener + takeLeadership (L31-48); 契约: 无并发执行 + CancelLeadershipException 会被 interrupt
- LeaderSelectorListenerAdapter: 默认 stateChanged 按 errorPolicy 抛 CancelLeadershipException (L30-35)
- Participant: id + isLeader 值对象 (L26-41)
- CancelLeadershipException: 仅 stateChanged 中有效 (L26-28)
- LeaderLatchListener: isLeader/notLeader 回调与 hasLeadership 非同步 (L25-43 注释)

## 二、聚合与分级

| 机制 | 级别 | 理由 |
|---|---|---|
| M1 LeaderLatch 选举算法 | P1 | 与锁同构的经典; ephemeralOwner 二次确认是独有细节 |
| M2 LeaderSelector 锁复用 | P1 | 框架内复用的范例; takeLeadership 生命周期 |
| M1 await/close 语义 | P2 | CloseMode 双模式 |
| M2 autoRequeue/连接联动 | P2 | 生产关键 |
| M3 监听者契约 | P2 | 接口语义 |

## 三、负面空间

- **不做自动重选**: latch 无 autoRequeue (selector 才有); 断连后需手动/监听处理
- **不做 leader 任期心跳**: 领导权 = 会话生命周期; 无续约 (对照 Seata 无、Redisson 无此场景)
- **不做 leader 工作调度**: takeLeadership 内阻塞由用户控制
- **不做参与成员变更通知**: participants 需轮询 ZK
- **不做隔离/防脑裂**: 依赖 ZK 单主; 双机房无隔离 (对照 ES/Consul)
