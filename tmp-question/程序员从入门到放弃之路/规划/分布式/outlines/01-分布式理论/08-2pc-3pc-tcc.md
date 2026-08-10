# 订单扣了库存但支付失败, 怎么回滚? — 2PC/3PC/XA/TCC全拆解

> Cluster C: 15 KPs | 依赖: A+B(已知CAP+共识) | 读者基线: 理解ACID事务概念

---

### 1. 一个跨数据库的转账 — 为什么单机事务不够了?
  用户从A银行(MySQL-A)转入B银行(MySQL-B)100元 — A扣了100, B没收到 → 钱丢了。
  - B1 Ch5 §2: 2PC两阶段提交 — 协调者(Coordinator)控制: Phase1 Prepare(所有参与者锁资源+写undo/redo+回复YES/NO) → Phase2 Commit(全YES→提交; 任一NO→回滚)
  - B4 Ch6 §1: 2PC状态机 — 参与者: INIT→PREPARED→COMMITTED/ABORTED; 协调者: INIT→WAITING→COMMITTING/ABORTING
  - 关键设计: 2PC的阻塞问题 — Phase2如果协调者宕机, 已Prepare的参与者持有锁无限等待 → 锁资源泄漏 → 单点故障
  - B2 Ch3 §1: 2PC的恢复 — 协调者写UNDO/REDO日志 → 宕机重启读日志恢复 → 但参与者宕机时不知道协调者决定 → 需要向其他参与者查询(引入复杂性) [工程: 2PC协作者单点故障 — 实际系统(Seata)用TC集群+Raft做高可用]

### 2. 3PC — 能用超时打破阻塞吗?
  2PC会死锁, 加个超时机制行不行?
  - B1 Ch5 §3: 3PC引入"预提交"中间态 — Prepare→PreCommit→DoCommit(三阶段) → 参与者超时后自主决定(不再死等协调者)
  - 关键设计: PreCommit的存在 — 协调者在Phase2发PreCommit(让参与者知道"大家都同意了, 准备提交") → 参与者收到PreCommit后超时→自动Commit(因为知道别人也同意了)
  - B2 Ch3 §2: 3PC的网络分区问题 — 分区发生时, 一边参与者超时→Commit, 另一边协调者Abort → 数据不一致(Partition破坏了"所有人都同意"的前提)
  - 为什么3PC没普及: 多了一轮RPC(延迟↑)+网络分区不安全 → 工程选择是"2PC+超时重试+补偿"而非3PC

### 3. XA/DTP — 数据库厂商给的标准接口
  2PC是协议, 怎么真正在MySQL/Oracle之间实现?
  - B4 Ch6 §3: DTP模型 — AP(应用程序)+RM(Resource Manager, 数据库)+TM(Transaction Manager, 协调者) — XA是RM和TM之间的接口规范
  - B4 Ch8 §1-2: MySQL XA — XA START xid → SQL操作 → XA END xid → XA PREPARE xid → XA COMMIT/ROLLBACK xid
  - 关键设计: XA的"遗忘"问题 — TM宕机重启后不知道哪些PREPARED事务存在 → XA RECOVER列出所有PREPARED事务 → TM逐个COMMIT/ROLLBACK
  - B4 Ch12-13: ShardingSphere-Atomikos-Narayana实现 — ShardingSphere对接XA事务管理器(Atomikos/Narayana) → 分库分表场景下跨分片的2PC

### 4. TCC — 不用锁, 用补偿!
  XA/2PC要加锁等全局提交, 性能太差 — TCC怎么绕过?
  - B4 Ch7 §1: TCC三阶段 — Try(预留资源, 如冻结库存) → Confirm(确认, 真正扣库存) → Cancel(回滚, 解冻库存)
  - B2 Ch3 §3: Seata TCC模式 — 业务代码实现@TwoPhaseBusinessAction(Try/Confirm/Cancel) → Seata框架协调全局事务
  - 关键设计: 空回滚(Cancel被调时Try还没执行 — 记录Try状态, 未Try→Cancel直接返回) + 防悬挂(Cancel先于Try到达 — Try被调时检查是否有Cancel记录) + 幂等(重试Confirm/Cancel → 按事务ID去重)
  - Try vs Prepare的区别: Prepare加锁(DB层), Try预留业务资源(业务层) — TCC没有锁, 靠业务补偿 — 开发量大但性能好

### 5. 收束 — 四种方案的选择
  - 全局一致性要求 + DB厂商支持: XA/2PC(简单但阻塞, 锁定时间长)
  - 高性能 + 业务可补偿: TCC(开发量3倍但无锁)
  - 需要框架: Seata(支持所有模式 — XA/AT/TCC/Saga, 一行注解切换)
  - 2PC vs 3PC: 工程选2PC+补偿(不选3PC), 因为3PC多1轮RPC且网络分区不安全

---

### 核心悬念
**"TCC要写Try/Confirm/Cancel三套代码, 开发量太大 — 有没有更简单的方案? 长事务拆成多个短事务, 失败了逐个补偿回去 — Saga是这么干的吗?"**

→ 引出 Saga补偿 + 可靠消息最终一致 + 最大努力通知 (09-saga-reliable-message)
