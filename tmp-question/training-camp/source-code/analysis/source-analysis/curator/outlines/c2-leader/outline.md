# C-2 Leader 选举 — 两种当选哲学: 被通知 vs 干到不想干

> 前置: [[C-1-CuratorFramework]] (fluent/后台操作) + [[C-3-分布式锁]] (LockInternals/序号排序) + [[Z-6-Watcher]] | 引出: [[C-8-服务发现]] (QueueSharder 用 LeaderLatch) | 对照: ZK 官方 LeaderElectionSupport + Redisson
> 🔴 A | 3 KP | [模式: 顺序节点 + 前驱 watch + 回调]
> Pass 2 闭环: q1(latch) q2(selector) q3(连接联动)

**读者处境**: 集群里 10 台机器要选 1 台做"调度者", 选出来后怎么通知大家? 当选的机器干完活要不要自动让位? 网络抖动会不会误判"不是我领导了"?

### 1. LeaderLatch — 被动选举: 谁最小谁当, 监听者收通知

场景: 定时任务调度器选主, 当选者需要知道自己"开始/失去"领导权
源码路径:
- 状态机 LATENT/STARTED/CLOSED (LeaderLatch.java:93-97); start() CAS + 等连接 (LeaderLatch.java:150-163); internalStart 注册连接监听 + reset (LeaderLatch.java:524-534)
- **reset()**: 删旧节点 → 后台创建 EPHEMERAL_SEQUENTIAL + withProtection (LeaderLatch.java:516-521) → 回调 getChildren
- **checkLeadership** (LeaderLatch.java:539-602): 排序找自己位置 (LockInternals.getSortedChildren)
  - ourIndex == 0 → **二次确认**: getData 校验 **ephemeralOwner == 本会话 sessionId** (LeaderLatch.java:556-573) → setLeadership(true)
  - 非 0 → setLeadership(false) + watch 前驱 (LeaderLatch.java:575-601), NodeDeleted → 重查
- await(): synchronized wait/notify (LeaderLatch.java:307-316); setLeadership 翻转时 notifyAll + 通知监听者 (LeaderLatch.java:665-674)
- **CloseMode**: SILENT (静默让位) vs NOTIFY_LEADER (先通知 notLeader 再清) (LeaderLatch.java:102-112, 195-226)
关键设计 (q1): **why 二次确认 ephemeralOwner**: 会话过期后他人可能拿到同序号的节点路径 (protection 后缀不同? 不 — 关键是 session 重建后旧节点消失, 而自己的 ourPath 已失效; 校验 owner 防"以为自己是 leader 实际节点已不在")。watch 前驱 = O(1) 通知链, 与锁同构。 [模式: 最小序号当选]

### 2. LeaderSelector — 持有式选举: 锁就是领导权

场景: 消息消费组的"协调者"需要持续干活, 干完自动让位
源码路径:
- **复用 InterProcessMutex** (LeaderSelector.java:70-77); 注释: 公平 = 按请求顺序当选 (LeaderSelector.java:60-64); 覆写 getLockNodeBytes 写入 id (LeaderSelector.java:161-166)
- **doWork()** (LeaderSelector.java:422-467): mutex.acquire() (LeaderSelector.java:426) → hasLeadership=true → **listener.takeLeadership(client) 阻塞在用户代码** (LeaderSelector.java:436) → 用户返回 → taskDone → mutex.release() (LeaderSelector.java:451)
- doWorkLoop (LeaderSelector.java:469-486): ConnectionLoss/SessionExpired → autoRequeue 开启则吞掉重试
- taskStarted/taskDone 簿记 (LeaderSelector.java:355-378); cancelElection/interruptLeadership (LeaderSelector.java:383-401)
- **WrappedListener 捕获 CancelLeadershipException → cancelElection** (LeaderSelector.java:546-556); 注释 "dated leadership" 问题 (LeaderSelector.java:550-553): 只取消领导权而不取消选举, 会把过期领导权交给客户端
关键设计 (q2): **takeLeadership 的阻塞时长 = 领导任期** — 方法返回即自动让位, 天然防泄漏; 复用锁 = 零重复实现排队/监听/公平性; CancelLeadershipException 是"连接异常让位"的纪律化路径 (模板适配器默认实现, LeaderSelectorListenerAdapter L30-35)。 [模式: 回调任期]

### 3. 连接状态联动 — 断连时领导权去哪了

场景: 网络分区 30 秒, 领导权算不算丢?
源码路径:
- latch: handleStateChange (LeaderLatch.java:630-663) — RECONNECTED → 复查 getChildren; SUSPENDED → **按 ConnectionStateErrorPolicy 决定** (C-1 M8); LOST → setLeadership(false)
- selector: 适配器默认 SUSPENDED/LOST (Standard 策略) → 抛 CancelLeadershipException → 中断当前选举线程 (LeaderSelectorListenerAdapter.java:30-35)
- latch 节点消失 (集群缩 0 再扩) → CURATOR-724 reset 重建 (LeaderLatch.java:610-617)
关键设计 (q3): **错误策略决定"闪断是否丢领导权"** — Session 策略下 SUSPENDED 不丢, 只有 LOST 才让位 — 短闪断容忍 vs 一致性优先的取舍; selector 与 latch 在"让位粒度"上的差异 (selector 只取消当前任期可重新排队, latch 需外部重建)。 [模式: 状态机联动]

## 代码类型
Architecture (分布式原语)

## 负面空间 — Curator 选举刻意不做的事

- **不做自动重新竞选**: latch 无 autoRequeue; 让位后要外部再 start (selector 才有)
- **不做任期心跳/续约**: 领导权与 ZK 会话同生共死, 无独立租约
- **不做选主结果持久化**: 谁是 leader 需实时查 ZK, 无本地高可用缓存
- **不做脑裂隔离 (fencing)**: 会话过期后旧 leader 不会自证退出, 依赖 ZK 数据面防护
- **不做参与者变化事件流**: 成员增删靠轮询 getParticipants

→ 引出: 队列配方怎么用顺序节点做 FIFO? → C-4 队列与屏障
