# MQ发了消息但消费者没收到 — 事务消息与最终一致的三种实现

> Cluster C: 14 KPs | 依赖: 08-2PC/TCC(已知事务协议) | 读者基线: 了解MQ(消息队列)基本概念

---

### 1. Saga — 长事务拆成短事务, 失败了逐个补偿
  一个旅游预订: 订机票→订酒店→租车→支付 — 全部成功或用补偿逆序回滚。
  - B1 Ch5 §5: Saga模式 — 每个子事务(T1, T2, T3, ..., TN)都配一个Compensating Action(C1, C2, C3, ..., CN)
  - 向前恢复 vs 向后恢复: 向前(重试失败步骤) vs 向后(逆序执行补偿C3→C2→C1) — 预订场景用向后恢复
  - 关键设计: Saga的隔离问题 — 子事务之间没有锁 — 订机票成功后还没付费, 其他请求可能看到"已预订但未支付"的中间态 → 需要语义锁(事务外标记座位状态)
  - B2 Ch3 §3: Seata Saga模式 — DSL定义状态机 → Seata Engine执行→失败时回退

### 2. 本地消息表 — 最朴素但刚健的最终一致
  "在下单数据库里, 和订单放在同一个事务中, 插入一条待发送消息" — 就这么简单。
  - B4 Ch10 §1: 本地消息表方案 — 业务DB+消息表(同DB同事务) → 定时任务扫描消息表→发MQ→收到ACK→标记已发送
  - 关键设计: "同DB同事务"保证了原子性 — 订单保存成功=消息一定写入消息表(不会订单成了消息丢了) — 本地事务保证了这两个操作的一致性
  - 重试+幂等: 消息表含msg_id+status+retry_count → 定时扫描status=INIT→发送MQ→成功标记SENT → 失败重试(max_retry后告警)
  - 消费者幂等: 消费者维护msg_id去重表 → INSERT IGNORE或Redis SETNX → 重复消息=不处理

### 3. RocketMQ事务消息 — 消息队列帮你做两阶段
  本地消息表要自己写定时任务 — RocketMQ把"两阶段"内置到了MQ协议中。
  - B4 Ch10 §2-3: RocketMQ事务消息 — Half Message(半消息, 对消费者不可见) → 本地事务执行 → Commit(消息可见) 或 Rollback(消息删除)
  - 关键设计: 回查机制 — 如果Producer发了Half Message后宕机(没发Commit/Rollback) → RocketMQ定时回查Producer(executeLocalTransactionBranch) → 补Commit或Rollback
  - B2 Ch3 §3: Seata + RocketMQ整合 — Seata TC作为全局事务协调者, RocketMQ作为消息通道 — Seata的全局事务+RM(RocketMQ)=分布式事务+可靠消息
  - 与本地消息表对比: RocketMQ自带"两阶段+回查"=省去定时任务 → 但依赖RocketMQ(非MQ-neutral)

### 4. 最大努力通知 — "我通知了, 你收没收到我尽量保证"
  银行回调通知: 支付成功后通知商户, 最多重试N次 — 还不成功就人工介入。
  - B4 Ch7 §3: 最大努力通知 — 发送方尽可能通知, 不保证, 需要接收方提供校对接口
  - 关键设计: 通知+校对双通道 — 发送方定时通知+接收方主动查询(提供queryOrder接口) → 双通道保证最终一致性
  - 与可靠消息的差异: 可靠消息 = "确定送达", 最大努力 = "尽力送达+兜底查询" → 前者主动保证, 后者被动加查询

### 5. 收束 — 四种最终一致方案的对比
  - 本地消息表: 最朴素/最刚健(同DB事务)/自开发
  - RocketMQ事务消息: MQ内建两阶段/减少开发量/依赖RocketMQ
  - Saga: 长事务拆分/补偿机制/隔离性弱/需语义锁
  - 最大努力通知: 最简单/适合外部系统/需查询接口做兜底

---

### 核心悬念
**"所有理论都讲了, ShardingSphere的XA、Hmily的TCC、Seata的AT/Saga — 这些框架在实际代码里怎么用? Spring事务在分布式场景有哪些失效的坑?"**

→ 引出 分布式事务实战: ShardingSphere/Hmily/Seata + Spring事务坑 (10-transaction-practice)
