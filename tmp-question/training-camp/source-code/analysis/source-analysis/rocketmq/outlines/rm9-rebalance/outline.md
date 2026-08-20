# RM-9 Rebalance+offset+LitePull — 队列再平衡与消费起点

> 前置: [[RM-8-消费]] (进度/ProcessQueue) + [[RM-5-Broker]] (心跳) | 引出: [[RM-10-顺序]] | 对照: Kafka Consumer Group (GroupCoordinator 中心化)
> 🔴 A | 6 KP | [模式: 周期再平衡 + 分配算法 + 差集处理 + 起点决策]
> Pass 2 闭环: q1(调度) q2(双模式) q3(差集+算法) q4(起点) q5(有序锁) q6(LitePull)

**读者处境**: 消费者挂了队列怎么分给别人? 新消费者加入从哪读? 有序消费怎么防并发? 这篇拆再平衡: 20s/1s 自适应、双模式分配、差集处理、消费起点、有序锁。

### 1. 再平衡调度 — 20s/1s 自适应

场景: 再平衡多久跑一次?
源码路径:
- **RebalanceService**: waitInterval=**20s** + minInterval=**1s**
- **自适应**: `balanced ? 20s : 1s` (不平衡快速收敛) + lastRebalanceTimestamp 节流
- **触发面**: 周期 + rebalanceImmediately (OFFSET_ILLEGAL 修复/RM-8) + **心跳变更感知链** (客户端 HEART_BEAT 注册 → broker consumerTable → 其他客户端 findConsumerIdList 感知 → 再平衡, RM-5 交叉)
关键设计 (q1): **平衡慢周期/失衡快周期** — 收敛与开销平衡。[模式: 周期调度]

### 2. 双模式分配 — 客户端 vs broker

场景: 分配在哪算?
源码路径:
- **客户端** (rebalanceByTopic): **广播=全队列无分配** (每客户端全量, L307-320) / **集群=双排序 (mqAll/cidAll Collections.sort — 确定性分配前提) + 算法分配** (L322+)
- **broker 分配** (5.x): **queryAssignment** (策略名上报, 3s×3 重试) → 服务端结果
- **双表**: topicClientRebalance / topicBrokerRebalance (模式缓存); **broker 分配失败 (3s×3 超时) → topicClientRebalance 回退客户端模式 (graceful degradation)**
关键设计 (q2): **分配中心化选项** (服务端管理/Controller 协同, RM-14)。[模式: 双模式]

### 3. 差集处理与 6 算法

场景: 队列归属变化怎么落?
源码路径:
- **删**: 不再归属 → removeUnnecessaryMessageQueue (**drop+unlock**) → remove
- **增**: putIfAbsent → **computePullFromWhere** (起点) + setLocked
- **6 算法**: **AVG** (均分, 前 mod 个多 1) / ByCircle / ConsistentHash / ByConfig / MachineRoom / Nearby (5.x)
关键设计 (q3): **差集 = 消费生命周期闭环** (旧队列 drop, 新队列定位); 算法可插拔。[模式: 分配算法]

### 4. 消费起点 — 5 模式

场景: 新队列从哪读?
源码路径:
- **5 模式** (computePullFromWhereWithException): 有进度 → **续读**; 无进度 → 重试 topic 从 0 / 普通 **maxOffset 尾部**; FIRST → 0; TIMESTAMP → 时间查询
- **冻结面**: OFFSET_ILLEGAL → updateAndFreezeOffset (RM-8)
关键设计 (q4): **默认尾部起读** (新消息优先) — 与 Kafka 默认 latest 一致。[模式: 起点决策]

### 5. 有序锁 — broker 侧队列互斥

场景: 有序消费怎么防并发?
源码路径:
- **lockAll/unlockAll**: LOCK_BATCH_MQ 批量锁 → setLocked
- **锁语义**: broker 持锁 (同一队列单消费者有序); ProcessQueue.isLockExpired 暂停
- **再平衡**: 锁随归属转移 (unlock 旧/加锁新)
关键设计 (q5): **分布式锁在 broker** — 跨客户端互斥。[模式: 分布式锁]

### 6. LitePull — 手动消费

- **subscribe/assign 互斥** (SUBSCRIPTION_CONFLICT)
- **poll 语义**: 用户控制节奏 (对照 Kafka); AssignedMessageQueue + messageQueueLock
- **RebalanceLitePullImpl**: 共享核心, 无自动拉取

### 负面空间 — 再平衡刻意不做的事

- **不做服务端推送分配默认**: broker 分配是 5.x 选项, 默认客户端 (对照 Kafka 默认中心化)
- **不做粘性分配**: AVG 重分配可能大迁移 (对照 Kafka cooperative sticky)
- **不做消费迁移优化**: drop 即丢未消费缓冲 (at-least-once 由重试兜底)
- **不做再平衡事件持久化**: 状态在内存 + broker 进度
- **不做分区级 leader 均衡**: 队列固定 broker, 无分区迁移

→ 引出: 顺序消息怎么保证全局序? → [[RM-10-顺序]]
