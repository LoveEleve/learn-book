# T-1 @Transactional 链路 — AOP 拦截到 JDBC Connection 的完整调用链

> 依赖 A-2 Advice链 + A-4 自动代理 | 🟡 Working | 2 KP | [模式: 模板方法 + 策略模式]

**读者处境**: A-2 中 TransactionInterceptor 作为 MethodInterceptor 进入 proceed 链 — 但 invokeWithinTransaction 具体做了什么？getTransaction 怎么获取连接？doBegin 怎么设置 autoCommit=false？commit/rollback 怎么提交到数据库？

### 1. TransactionInterceptor.invoke → invokeWithinTransaction — AOP 拦截到事务管理

场景: @Transactional Service 方法 → AOP 代理拦截 → TransactionInterceptor.invoke(MethodInvocation) → 获取 TransactionAttributeSource(读取@Transactional注解属性: propagation/isolation/timeout/readOnly) → TransactionManager.getTransaction(attr) → getConnection+setAutoCommit(false) → invocation.proceed()(执行业务方法) → commit/rollback。

整条链的最终落点是 JDBC Connection — Spring 事务的"实体"就是"把多个 JDBC 操作绑定到同一连接并控制提交时机"。

源码路径:
- `TransactionInterceptor.java:112` — **invoke()**: ①`Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null)`→获取目标类 ②`invokeWithinTransaction(invocation.getMethod(), targetClass, invocation::proceed)`→委托父类 TransactionAspectSupport
- `TransactionAspectSupport.java:342` — **invokeWithinTransaction()**: ①L347 `getTransactionAttributeSource().getTransactionAttribute(method, targetClass)`→读取@Transactional注解属性 ②L348 `determineTransactionManager(txAttr, targetClass)`→选择TransactionManager(按qualifier→default→PlatformTransactionManager bean) ③L374 `createTransactionIfNecessary(ptm, txAttr, joinpointIdentification)`→tm.getTransaction(attr) ④L380 `invocation.proceedWithInvocation()`→执行目标方法 ⑤L416 `commitTransactionAfterReturning(txInfo)`→commit / 异常→L384 `completeTransactionAfterThrowing`→rollback

关键设计: **Why TransactionAttributeSource 和 TransactionManager 分离？** TransactionAttributeSource 负责"读注解"(@Transactional的propagation/isolation/timeout)—是静态配置读取。TransactionManager 负责"执行事务"(getConnection/commit/rollback)—是运行时行为。分离让同一个 TransactionManager 可以服务不同的 @Transactional 配置—也能让同一个 @Transactional 配置映射到不同的 TransactionManager(@Transactional("orderTm")→orderDataSource)。

数据流: @Transactional(propagation=REQUIRED) public void createOrder()→AOP代理→TransactionInterceptor.invoke→L112 targetClass=OrderService→L119 invokeWithinTransaction→L347 getTransactionAttribute→@Transactional→propagation=REQUIRED, isolation=DEFAULT, timeout=-1→L348 determineTransactionManager→beanFactory.getBean(TransactionManager.class)→DataSourceTransactionManager→L374 createTransactionIfNecessary→tm.getTransaction(def)→L373 getTransaction→doBegin→getConnection→conn.setAutoCommit(false)→TransactionSynchronizationManager.bindResource(ds, connHolder)→返回TransactionStatus→L380 invocation.proceedWithInvocation()→orderService.createOrder()→insert into orders...→L416 commitTransactionAfterReturning→txManager.commit→doCommit→conn.commit()→TransactionSynchronizationManager.unbindResource→conn.close()

### 2. getTransaction + doBegin — 连接获取与 ThreadLocal 绑定

场景: `tm.getTransaction(def)` 是事务的开始 — 它决定"是否已存在事务/该新建还是复用/要不要挂起"。`doBegin` 是真正"打开"事务的动作: 获取连接、关 autoCommit、绑定 ThreadLocal。DataSourceTransactionManager 的 doBegin 与 MySQL 的 `BEGIN`/`SET autocommit=0` 等价 — 但由 Spring 统一管理连接生命周期。

源码路径:
- `AbstractPlatformTransactionManager.java:373` — **getTransaction()**: ①L382 `isExistingTransaction(transaction)`为真→L384 `handleExistingTransaction(def, transaction, debugEnabled)`—根据传播行为(NESTED→savepoint/REQUIRED→复用/REQUIRES_NEW→挂起当前) ②无现有事务→L397-399 REQUIRED/REQUIRES_NEW/NESTED 分支→L405 `startTransaction(def, transaction, false, ...)`→L532 `doBegin(transaction, definition)`(子类实现; L414 是 SUPPORTS 空事务分支的 warn 检查, 非 doBegin)
- `DataSourceTransactionManager.java:264` — **doBegin()**: ①L271 `obtainDataSource().getConnection()`→获取JDBC连接 ②L293-298 `con.getAutoCommit()`为true→`con.setAutoCommit(false)`(L298)→关闭自动提交 ③L310-311 `txObject.isNewConnectionHolder()`为true→`TransactionSynchronizationManager.bindResource(obtainDataSource(), txObject.getConnectionHolder())`(L311)→ThreadLocal绑定连接到当前线程

关键设计: **Why 事务与 ThreadLocal 绑定？** JDBC Connection 不是线程安全的 — 同一个线程内的所有数据库操作必须走同一个 Connection 才能在同一事务内。TransactionSynchronizationManager 用 ThreadLocal 存储当前线程的 ConnectionHolder → Service 方法内调 Repository → DataSourceUtils.getConnection()→从 ThreadLocal 取当前事务的连接(而非新建)→保证同一事务内所有操作共享同一连接。

数据流: @Transactional createOrder()→TransactionInterceptor.invoke→invokeWithinTransaction→createTransactionIfNecessary→tm.getTransaction(def)→L373 getTransaction→L382 isExistingTransaction(transaction)(DataSourceTransactionManager.doGetTransaction 内 L251-252 `TransactionSynchronizationManager.getResource(ds)`→ConnectionHolder=null→无现有事务)→false→L397-399 REQUIRED 分支→L405 startTransaction→L532 doBegin→DataSourceTransactionManager.doBegin(L264)→L271 obtainDataSource().getConnection()→L298 con.setAutoCommit(false)→L311 TransactionSynchronizationManager.bindResource(ds, ConnectionHolder)→ThreadLocal<Map<DataSource, ConnectionHolder>>.set(ds, holder)→返回TransactionStatus→invocation.proceed()→createOrder()内调 orderRepo.save()→JdbcTemplate.execute→DataSourceUtils.getConnection(ds)→TransactionSynchronizationManager.getResource(ds)→返回同一Connection(ThreadLocal取到的)→同一事务内INSERT orders和order_items共享同一连接→proceed返回→commitTransactionAfterReturning→transactionManager.commit→doCommit→conn.commit()→TransactionSynchronizationManager.unbindResource→connection.close()→ThreadLocal清理

**commit 链的异常分支**: 若 proceed 抛异常 → completeTransactionAfterThrowing(L384) → `txInfo.getTransactionStatus().setRollbackOnly()` 或直接 rollback → `tm.rollback` → doRollback → `conn.rollback()` → 同样 unbindResource + close — 与 commit 路径共享连接清理逻辑。rollback 判定由 RollbackRuleAttribute(见 T-2 域)决定 — checked 异常默认不回滚。

→ spring-tx 第一域完成。TransactionInterceptor→getTransaction→doBegin→commit/rollback 完整链 + ThreadLocal 连接绑定。引出 T-2: @Transactional 8种失效场景 — 自调用/non-public/异常类型/exclude/多数据源。
