# 04 事务体系（Week 8-9 · 6课时）

## 学完能干什么
能说清楚从单机事务到分布式事务的四种方案矩阵，能看懂 Seata AT 模式的源码并手画 undo_log 工作流程。

---

## 第 1 层：本地事务（2 课时）

### 课时 18：JDBC 事务底层

**📖 读什么**
- `stage-2/docs/14. 第十三节：Java EE 本地事务管理原理和实现.md`（全文）

**看懂 MySQL 怎么实现隔离级别**：

| 级别 | MySQL InnoDB 实现 |
|------|------------------|
| READ UNCOMMITTED | 不做任何锁 |
| READ COMMITTED | MVCC，每次 SELECT 读最新快照 |
| REPEATABLE READ | MVCC，事务内第一次 SELECT 建快照，后续复用（MySQL 默认） |
| SERIALIZABLE | SELECT 加共享锁，写加排他锁 |

**JDBC 底层命令对照**：
```java
conn.setAutoCommit(false);  // → MySQL 发送 "SET autocommit=0"
conn.commit();              // → MySQL 发送 "commit"
conn.rollback(savepoint);   // → MySQL 发送 "ROLLBACK TO SAVEPOINT sp1"
```

**DriverManager 加载驱动**：通过 SPI `META-INF/services/java.sql.Driver` 自动发现 MySQL Driver 实现。

---

### 课时 19：Spring 事务原理

**📖 读什么**
- `stage-2/docs/16.`

**@Transactional 本质**：AOP 代理 + `TransactionInterceptor`
```
目标方法 → TransactionInterceptor.invokeWithinTransaction()
  → createTransactionIfNecessary()  // 获取/创建事务
  → 执行目标方法
  → commitTransactionAfterReturning() / completeTransactionAfterThrowing()
```

**七种传播行为**：REQUIRED（默认，有则加入无则新建）/ SUPPORTS / MANDATORY / REQUIRES_NEW / NOT_SUPPORTED / NEVER / NESTED

**✅ 算过关**
- REQUIRED 和 REQUIRES_NEW 的区别是什么？什么时候用 REQUIRES_NEW？

---

## 第 2 层：XA 两阶段提交（1 课时）

### 课时 20：JTA/XA

**📖 读什么**
- `stage-2/docs/15. 第十五节：JTA 和 XA 原理与实现.md`
- **必读论文**：`stage-2/papers/Life beyond Distributed Transactions.pdf`（前 5 页）

**JTA 三大组件**：UserTransaction / XAResource / TransactionManager

**两阶段提交流程**：
```
Phase 1 (Prepare): TM → RM "准备提交了吗？"
                   RM → TM "准备好了" / "不行"
Phase 2 (Commit):  TM → RM 全部准备好→"提交"
                        任何失败→"回滚"
```

**XA 三大问题**（这就是为什么要找替代方案）：
1. 锁定时间长：Prepare 到 Commit 之间所有资源被锁
2. TM 单点：事务协调者挂了，参与者不知道该 commit 还是 rollback
3. 网络分区：参与者收不到第二阶段指令 → 悬挂事务

**✅ 算过关**
- 为什么 Pat Helland 说 "Life beyond Distributed Transactions"？

---

## 第 3 层：可靠事件队列（1 课时）

### 课时 21：消息表 + Kafka

**📖 读什么**
- `stage-2/docs/17. 第十七节：可靠事件队列分布式事务原理和实现.md`
- `stage-2/docs/18.`

**核心思路**：
```
本地事务 { 业务操作 + 写入消息表 } → 提交 → 异步发送事件到 Kafka → 消费者处理
```

**动手**：用文档里的 SQL 建 users 表和 transactions 表，模拟一次"扣款+下单"流程。

**同库 vs 跨库**：
- 同库：users 表和 transactions 表在同一个数据库，本地事务直接保证原子性
- 跨库：user_db 和 tx_db 分库，靠 tx_messages 表 + Kafka 桥接

**✅ 算过关**
- 如果消费者处理失败了怎么办？消息丢了怎么办？

---

## 第 4 层：TCC + Seata（2 课时）

### 课时 22：TCC 模式

**📖 读什么**
- `stage-2/docs/19.`

**TCC vs 2PC**：
- 2PC 在资源层（数据库层面），TCC 在业务层（代码层面）
- Try：预留资源（冻结库存）
- Confirm：确认提交（扣减冻结库存）
- Cancel：回滚（释放冻结库存）

**TCC 的 Confirm/Cancel 必须幂等**——因为网络超时可能重试。

---

### 课时 23：Seata AT + TCC 源码（⭐ 重点）

**📖 读什么**
- `stage-2/docs/20.` + `21.`（全文，含完整源码分析）

**Seata AT 模式核心机制**：

```java
// ConnectionProxy.commit() 在执行 commit 之前：
1. lockRetryPolicy 重试获取全局锁
2. doCommit
3. processGlobalTransactionCommit → register 到 TC
4. UndoLogManager.flushUndoLogs(beforeImage, afterImage)
5. targetConnection.commit
6. report 上报到 TC
// 二阶段：成功 → 异步删除 undo_log / 失败 → 用 undo_log 反向补偿
```

**TccActionInterceptor 拦截链路**（200+ 行源码分析）：
```
1. 检查 RootContext.inGlobalTransaction()
2. 获取 @TwoPhaseBusinessAction 注解
3. RootContext.bindBranchType(BranchType.TCC)
4. doTccActionLogStore() → 向 TC 申请 Branch ID
5. 执行目标方法（TCCFenceHandler 或直接执行）
6. BusinessActionContextUtil.reportContext() 上报
```

**🔍 看什么代码**
```bash
cd stage-2/src/middleware-projects && mvn compile -pl distributed-transaction-project
```
打开 `ConnectionProxy.java` 找 `commit()` 方法，跟 `processGlobalTransactionCommit()` 调用链。

**✅ 算过关**
- Seata AT 的 undolog 和 MySQL 的 undolog 有什么区别？
- AT 模式的"脏写"问题是什么意思？全局锁怎么解决的？

---

## 本阶段自检清单
- [ ] 能画图解释 XA 两阶段提交的流程和缺点
- [ ] 能对比可靠事件队列和 TCC 的适用场景
- [ ] 能解释 Seata AT 模式的 undo_log（beforeImage/afterImage）怎么工作
- [ ] 读完了《Life beyond Distributed Transactions》论文
- [ ] 知道分布式事务四种方案（2PC/可靠事件/TCC/Saga）各适合什么场景
