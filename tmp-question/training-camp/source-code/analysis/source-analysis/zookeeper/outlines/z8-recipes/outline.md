# Z-8 Recipes — 顺序节点三件套: 选举/锁/队列

> 前置: [[Z-7-ClientAPI]] (客户端门面) + [[Z-6-Watcher]] (前驱 watch) + [[Z-5-Session]] (临时节点会话绑定) + [[Z-3-DataTree]] (顺序节点) | 引出: Curator (阶段 4.5) | 对照: Curator 4.5 (LeaderLatch/InterProcessMutex) + Redisson
> 🟡 B | 8 KP | [模式: 顺序节点 + 最小序号 + 前驱 watch]
> Pass 2 闭环: q1(共同模式) q2(选举) q3(锁) q4(队列)

**读者处境**: 怎么用 ZooKeeper 实现 leader 选举/分布式锁/FIFO 队列? 三个 recipes 的共性是什么? 边界竞态 (前驱消失/会话失效/并发消费) 在哪? 这篇拆官方 recipes 三件套 (非 Curator)。

### 1. 共同模式 — 顺序节点 + 最小序号 + 前驱 watch

场景: 三个 recipes 共享什么?
源码路径:
- **顺序节点**: 选举/锁用 **EPHEMERAL_SEQUENTIAL** (会话绑定, 断开自动清理 — LeaderElectionSupport:176 / WriteLock:196) vs 队列用 **PERSISTENT_SEQUENTIAL** (数据跨会话存续 — DistributedQueue:265); 节点名 "\<前缀\> + %010d 序列" (Z-3 交叉)
- **最小序号判定** (三实现三种写法): 选举 IdComparator 按 Integer id 排序找自己下标 (LES:207-224) / 锁 TreeSet\<ZNodeName\> first + headSet (WriteLock:229-253) / 队列 TreeMap\<Long\> 最小者=队头 (DQueue:65-87); ⚠ 序号位宽不一致: LES/ZNodeName Integer (32 位) vs 队列 Long — %010d 10 位上限 9999999999 > Integer.MAX, 2.1B 次创建后溢出 (L197/L43/L79)
- **前驱 watch** (事件驱动替代轮询): 选举 exists(前驱, this) → NodeDeleted 重跑 (LES:239,327-341) / 锁 exists(前驱, LockWatcher) → lock() 重试 (WriteLock:239,158-168) / 队列不 watch 前驱 — take() 用整目录变更事件 (DQueue:232-242)
- **语义核心**: 序号全序 → "最小者胜" 天然互斥 — 无互斥协议, 无轮询
关键设计 (q1): **全序序号 + 最小者胜 + 前驱事件驱动** = 三 recipes 统一原语; 差异只在节点生命周期与触发面。[模式: 顺序节点]

### 2. LeaderElectionSupport — 事件驱动状态机

场景: 选举怎么组织?
源码路径:
- **状态机** (LES:443-469): **7 State** (START/OFFER/DETERMINE/ELECTED/READY/FAILED/STOP) + **12 EventType** (每状态进出双事件 + FAILED + STOP 双段)
- **makeOffer** (L165-182): create rootNodeName+"/n_" EPHEMERAL_SEQUENTIAL — hostName 为 data
- **determineElectionStatus** (L188-225): 全量快照 getChildren → toLeaderOffers 逐个 getData + IdComparator 排序 → 找自己下标 i==0 → becomeLeader / 否则 becomeReady(前驱); ⚠ **快照后前驱删除 → getData NoNode 直抛 → becomeFailed** (L310) — 前驱消失**三处三种处理** (becomeReady 重读 / toLeaderOffers FAILED / WriteLock 悬挂); ⚠ **无前缀校验** — 共享 root 他人节点 → NumberFormatException → FAILED (对照 DQueue warn+skip L74-83)
- **becomeReady** (L227-259): exists(前驱, this) — **显式 watcher (ZooKeeper 可能共享, L236-238)**; stat==null (**前驱消失竞态**) → **递归 determineElectionStatus 重读** (L256) ⚠ vs WriteLock 只 log.warn (对照面)
- **process** (L327-341): **只处理 NodeDeleted** + 排除自身路径 (L329) + 排除 STOP 态 (L330); ⚠ **非 synchronized + state 非 volatile** — 事件线程 vs 用户线程并发重判 → 双事件交错 (L95); ⚠ **不处理 None 事件** — 无 SyncConnected 重判钩子, 断连期间前驱死亡事件丢失 → 悬挂
- **getLeaderHostName** (L289-298): 每次全量读+排序 (无缓存, O(N))
关键设计 (q2): **状态机 + 全量快照判定 + watch 事件驱动重跑**; 前驱消失用递归重读兜底。[模式: 状态机]

### 3. WriteLock — 幂等创建 + 前驱监听 + 重试面

场景: 锁怎么实现?
源码路径:
- **幂等创建** (L185-201): prefix = **"x-" + sessionId + "-"** (L216) → findPrefixInChildren 扫描前缀 — **create 响应丢失后的恢复** (L216-218 注释锚)
- **主循环** (L212-258): id==null 才创建 → getChildren → TreeSet 显式排序 → **ownerId = first** → **lessThanMe = headSet(idName)** → 有前驱: **exists(前驱, LockWatcher)** → FALSE 等待 / 无前驱: isOwner() → **lockAcquired() 回调**; ⚠ **会话事件 (type=None) 送达全部 watcher** (ZKWatchManager:345-370) → LockWatcher 空转触发 lock() (有效 id 下部分自愈, 陈旧 id 下空转); ⚠ **unlock 不撤销前驱 watch** → 前驱后死 → 重建节点重新竞争, 违反 Javadoc "removes your request" (L110-149)
- **重试面** (ProtocolSupport:121-140): **RETRY_COUNT=10** (L41) + retryDelay=500ms (L45) + 线性退避 attempt×500 (L192-200); **SessionExpired 立即重抛 / ConnectionLoss 退避重试** (L127-136)
- **unlock** (L119-149): delete(id,-1) + **lockReleased() 回调** (finally); "不重试 delete — ZK 自动清理 ephemeral" (L122-124 注释锚)
- **锁=选举同一算法**: Javadoc "exclusive write lock or to elect a leader" (L36-37)
关键设计 (q3): **sessionId 前缀幂等 + 前驱 watch + 连接层重试**; 锁与选举共享全部机制。[模式: 幂等+监听]

### 4. DistributedQueue — FIFO 顺序 + 阻塞 take + 并发消费

场景: FIFO 队列怎么实现?
源码路径:
- **FIFO 保证** (L65-87): orderedChildren = getChildren → 只收 "qn-" 前缀 (L74) → **TreeMap\<Long\> 序列排序** (L79-80) — 非 qn-/非数字子节点 warn+跳过
- **offer** (L262-272): **PERSISTENT_SEQUENTIAL** (L265); 目录不存在 → NoNode → **create(dir) 惰性自举** (L267-269) ⚠ 竞态: 并发创建 → NodeExistsException 未捕获直抛 (L236,268 同)
- **element/remove** (L138-201): 快照内逐节点 getData / **getData+delete** → **NoNodeException = 被并发消费方抢走 → 试下一个** (L160-162,195-197) — 全被抢 → 重读快照 (while true, **无 sleep 无界重读** — 高竞争 busy loop)
- **take** (L228-255): **LatchChildWatcher 注册在 getChildren 上 (注册即读, L234)** → 空队列 await 阻塞 (L240) → 任何子节点变更释放 → 重读 (虚假唤醒 re-loop 兜底)
- **peek/poll** (L280-300): 异常 → null 宽容面
关键设计 (q4): **持久顺序节点 + 注册即读的目录 watch 阻塞 + NoNode 重试消费**。[模式: 阻塞队列]

## 代码类型
Architecture (分布式原语)

## 负面空间 — recipes 刻意不做的事

- **不做服务端级原语**: 全部靠 ZK 基础 API 组合 — 顺序节点/watch 是唯一原语 (对照 ZooKeeperServer 内建无锁服务)
- **不做会话失效自愈**: 选举忽略会话事件 (自身 NodeDeleted 排除) — 会话过期后停留在 ELECTED 无感知 (Javadoc "best effort" L66-87); WriteLock SessionExpired 直抛交调用方
- **不做锁超时/公平性扩展**: lock() 无 acquire(timeout) — 前驱永不死则永远等待 (对照 Curator acquire(maxWaitTime))
- **不做队列优先级/去重/确认**: 纯 FIFO, take 后即删 — 无 ack 语义 (消费失败数据即丢, 需业务补偿)
- **不做重试策略抽象**: 固定 10 次线性退避 (ProtocolSupport) — 对照 Curator RetryPolicy 可配
- **不做 leader 续约/心跳自证**: 靠 ephemeral 会话维持 — "best effort" (Javadoc 明示)

→ 引出: 客户端封装怎么补全这些面? → Curator (阶段 4.5)
