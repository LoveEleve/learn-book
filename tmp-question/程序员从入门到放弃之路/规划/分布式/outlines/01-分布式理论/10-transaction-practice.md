# 代码写对了, 事务为什么没生效? — 分布式事务框架实战与Spring事务7大陷阱

> Cluster C: 14 KPs | 依赖: 08+09(已知2PC/TCC/Saga/可靠消息) | 读者基线: 写过Spring/Spring Boot应用

---

### 1. MySQL XA — 从语法到原理
  XA START / XA END / XA PREPARE / XA COMMIT — 数据库层的两阶段是什么体验?
  - B4 Ch8 §2: MySQL XA语法 — XA START xid → INSERT/UPDATE → XA END xid → XA PREPARE xid → XA COMMIT/ROLLBACK xid
  - 关键设计: redo和undo的分工 — redo(重做日志, 物理级: 在磁盘上修改后的值)保证提交后数据不丢失 — undo(撤销日志, 逻辑级: 逆向SQL)保证回滚能恢复 — binlog(归档日志, 逻辑级: SQL语句)用于主从复制/PITR
  - B4 Ch2: 二阶段redo组提交 — 多个事务的redo批量fsync → 减少磁盘IO → MySQL 5.6+的binlog组提交3阶段(flush→sync→commit)
  - XA RECOVER: TM重启后执行, 列出所有PREPARED但未COMMIT的事务 → TM逐一决策

### 2. Spring事务 — 7个让你怀疑人生的失效场景
  `@Transactional`加了但没回滚 — 不是框架bug, 是没理解AOP的本质。
  - B4 Ch3: 7大失效 — (1)方法非public(代理要求) (2)同类方法调用(不走代理, this.method()绕过AOP) (3)异常被catch未抛 (4)rollbackFor未指定非RuntimeException (5)数据库引擎不支持事务(MyISAM) (6)多线程(子线程不在原事务上下文) (7)传播机制选错(REQUIRES_NEW挂起当前事务)
  - 关键设计: Spring事务=AOP代理 + ThreadLocal(TransactionSynchronizationManager) — 代理拦截方法→开启事务→绑定到当前线程→执行→提交/回滚→解绑
  - 传播机制7种: REQUIRED(默认, 有则加入无则新建), REQUIRES_NEW(挂起当前, 新建), NESTED(保存点), SUPPORTS/MANDATORY/NEVER/NOT_SUPPORTED

### 3. Seata — 一次配置搞定分布式事务
  Seata四种模式(AT/TCC/Saga/XA)的源码骨架。
  - B2 Ch3 §3: Seata AT模式 — 一阶段: 自动生成undo_log(记录修改前后的镜像) → 二阶段: TC通知RM→根据undo_log回滚或删除
  - 关键设计: 全局锁 — AT模式下Seata在RM侧维护lock_table → 事务未提交前相同记录加全局锁 → 避免脏写 [工程: Seata AT模式 — 基于undo_log自动生成回滚SQL，无业务侵入是核心价值]
  - Seata TC: 事务协调者(Transaction Coordinator) — 维护全局事务状态(GlobalSession) → 定时检查超时事务→释放锁
  - 选择建议: 无业务改造→AT(零侵入), 高性能+可补偿→TCC, 长事务→Saga, 传统XA数据库→XA

### 4. Hmily/ShardingSphere — 两种XA+TCC的实践
  Hmily的TCC和ShardingSphere的XA各有什么特点?
  - B4 Ch14-15: Hmily TCC — @HmilyTCC(confirmMethod/cancelMethod) → 框架记录日志(TCC事务表) → 定时恢复未完成事务 → 补偿重试
  - B4 Ch12-13: ShardingSphere + Atomikos/Narayana — 对接XA事务管理器 → 跨分片2PC
  - 关键设计: Hmily的本地日志 — 每个Try/Confirm/Cancel都写本地DB(log表) → 如果Confirm/Cancel失败→定时恢复(后台线程读log重试)
  - 框架对比: Seata生态最全(AT/TCC/Saga/XA四合一), Hmily专注TCC(纯业务补偿), ShardingSphere聚焦分库分表+XA(DB层事务)

### 5. 收束 — 三个框架选型指南
  - 只做分库分表的事务: ShardingSphere + XA
  - 微服务间要分布式事务: Seata AT(零侵入快速开始)或TCC(高性能需改代码)
  - 纯TCC业务补偿: Hmily(轻量专注)
  - Spring @Transactional的7大陷阱: 日常开发常见, 理解AOP代理机制是根本

---

### 核心悬念
**"共识、事务都讲了 — 但系统上线后, 怎么知道Leader还活着? 怎么检测哪些节点已经挂了? 广播一条配置变更到1000个节点怎么保证所有人都收到?"**

→ 引出 失败检测/选主/可靠广播/Gossip (11-leader-election-broadcast)
