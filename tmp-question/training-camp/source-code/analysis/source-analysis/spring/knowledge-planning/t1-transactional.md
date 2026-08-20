# T-1 @Transactional 链路 — TransactionInterceptor→PlatformTransactionManager→DataSourceTransactionManager

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | 3文件/~2043行
> 基线: A-2 Advice链 — TransactionInterceptor 是 MethodInterceptor, 进入 proceed 链作为 Around advice

---

## §0.8

- 🟡 Working，1篇 — TransactionInterceptor.invoke→TransactionAspectSupport.invokeWithinTransaction→AbstractPlatformTransactionManager.getTransaction/commit/rollback→DataSourceTransactionManager.doBegin/doCommit/doRollback
- 设计模式: [模式: 模板方法]—getTransaction/commit/rollback在AbstractPlatformTransactionManager定义骨架, DataSourceTransactionManager实现doBegin/doCommit/doRollback

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| TransactionInterceptor.java:55+L112 | invoke() | **入口**: MethodInterceptor.invoke→TransactionAspectSupport.invokeWithinTransaction→获取TransactionAttributeSource→getTransactionAttribute→TransactionManager.getTransaction | High |
| TransactionAspectSupport.java:342 | invokeWithinTransaction() | **模板**: ①getTransactionAttribute ②determineTransactionManager ③tm.getTransaction→TransactionInfo ④invocation.proceed()(目标方法) ⑤commitTransactionAfterReturning(txInfo)/completeTransactionAfterThrowing→rollback | High |
| AbstractPlatformTransactionManager.java:373 | getTransaction() | **事务获取**: ①L384 handleExistingTransaction→判断传播行为(NESTED/REQUIRED/REQUIRES_NEW) ②L414 new transaction→doBegin→new DefaultTransactionStatus ③返回TransactionStatus(含connection+rollbackOnly标记) | High |
| AbstractPlatformTransactionManager.java:532 | doBegin() | **抽象方法**(子类实现)→DataSourceTransactionManager.doBegin(L264): obtainDataSource→getConnection→setAutoCommit(false)→bindToThread(ThreadLocal) | High |
| DataSourceTransactionManager.java:264 | doBegin() | **JDBC实现**: ①L271 obtainDataSource().getConnection()→②setAutoCommit(false)→③TransactionSynchronizationManager.bindResource(ds, ConnectionHolder)→ThreadLocal绑定连接 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**P1 核心 (2):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | TransactionInterceptor.invoke→invokeWithinTransaction→getTransaction→proceed→commit/rollback 完整调用链 | 🔴 | **为什么**: @Transactional的"发动机"—从AOP代理拦截到事务开始(getConnection+setAutoCommit(false))到方法执行到commit/rollback的完整生命周期 |
| P1-2 | AbstractPlatformTransactionManager.getTransaction — 传播行为判断(handleExistingTransaction) + doBegin/doCommit/doRollback 抽象方法 | 🔴 | **为什么**: 模板方法和策略模式的完美演示—传播行为在父类处理(NESTED→savepoint/REQUIRED→复用/REQUIRES_NEW→挂起)—doBegin/doCommit/doRollback留给子类(JTA/Hibernate/JDBC) |

**1篇理由**: ~2043行/3文件—核心是invokeWithinTransaction(342行父类支持)+事务生命周期(getTransaction/commit/rollback, 1368行)。1篇(~45行)覆盖从拦截器到JDBC连接的完整链。

**单篇结构**: §1 TransactionInterceptor→invokeWithinTransaction 拦截入口 → §2 getTransaction(传播行为)+doBegin(连接获取) → §3 commit/rollback + ThreadLocal连接绑定
