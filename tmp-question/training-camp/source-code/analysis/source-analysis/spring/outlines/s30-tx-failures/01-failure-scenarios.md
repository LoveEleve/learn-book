# T-2 @Transactional 8种失效场景 — 根因分析与源码验证

> 依赖 T-1 链路 + A-1 代理 | 🔴 Deep | 4 类根因 | [模式: 实践分析]

**读者处境**: T-1 理解了完整调用链 — 但实际开发中 @Transactional 总"不生效"。为什么加了注解事务没起作用？8种失效场景及源码级根因。

### 1. 代理拦截缺失 — #1 自调用 + #2 non-public (80%的失效根源)

场景: `@Transactional public void createOrder() { this.saveToDB(); }` — createOrder 从 Controller 调 → 走 AOP 代理 → 事务生效 ✅。createOrder 内部调 `this.saveToDB()` — this 是原始对象而非代理 — saveToDB 不走代理 → TransactionInterceptor.invoke 不可能被触发 → 事务不生效 ❌。

源码路径:
- `TransactionInterceptor.java:112` — **invoke(MethodInvocation)**: 触发条件: 调用经过 JDK/CGLIB 代理 — `this.method()` 不经过代理 → invoke 不触发
- `CglibAopProxy.DynamicAdvisedInterceptor` — CGLIB覆写非final方法—private/final方法无法覆写→无代理→AOP无效
- **修复**: ①`AopContext.currentProxy()`→`((Service)proxy).method()` ②将事务方法移到另一个Service ③@EnableAspectJAutoProxy(exposeProxy=true)

数据流: Controller → userServiceProxy.createOrder() → TransactionInterceptor.invoke → 事务开始 ✅ → createOrder 内 `this.saveToDB()` → this=原始 UserService(非代理) → 不经过 TransactionInterceptor → 事务外执行 saveToDB ❌ → createOrder 返回 → TransactionInterceptor 感知不到 this 调用 → 只 commit createOrder 的操作

关键设计: **Why this 调用绕过代理？** AOP 代理拦截的是"外部对代理的调用" — `this.saveToDB()` 的 this 是原始对象(代理内部持有 target)— 方法调用直接发生在 target 上, 不经代理 → TransactionInterceptor 无从介入。修复三选一: ①注入自身代理(@Autowired UserService self 或 AopContext.currentProxy()) ②方法移到另一个 Service ③@EnableAspectJAutoProxy(exposeProxy=true)。本质是"代理模式的外调用内调用边界"问题。[模式: 代理模式 — 内部 this 调用绕过代理]

### 2. 异常语义错误 — #3 checked异常默认不回滚 + #4 noRollbackFor

场景: `@Transactional public void importData() throws IOException { parse(); }` → parse() 抛 IOException(checked) → 事务不回滚 → DB数据被 Commit。原因: Spring 默认 rollbackOn RuntimeException/Error — checked Exception 不回滚。

源码路径:
- `RuleBasedTransactionAttribute.java:124` — **rollbackOn(Throwable)**: ①L128-135 遍历 rollbackRules→对每条 rule 调 `rule.getDepth(ex)`(L130)→取继承层次最浅(深度最小)者为 winner(L131-134) ②L139-140 无规则命中→回退 `super.rollbackOn(ex)`→父类 `DefaultTransactionAttribute.rollbackOn`(DefaultTransactionAttribute.java:185-186)= 仅 `RuntimeException || Error` 回滚(默认 rollbackRules 为空, SpringTransactionAnnotationParser.java:88-101 只在显式指定 rollbackFor/noRollbackFor 时才 add)
- `TransactionAspectSupport.completeTransactionAfterThrowing` — 调用 `txInfo.getTransactionManager().rollback(txInfo.getTransactionStatus())` 仅在 `rollbackOn(ex)`=true 时
- **修复**: `@Transactional(rollbackFor=Exception.class)` 或 `@Transactional(rollbackFor=IOException.class)`

数据流: @Transactional importData() → TransactionInterceptor.invoke → invokeWithinTransaction → getTransactionAttribute → RuleBasedTransactionAttribute: rollbackRules=[](默认空, SpringTransactionAnnotationParser.java:88-101 仅显式指定才添加)—RuntimeException/Error 默认回滚来自父类 DefaultTransactionAttribute.rollbackOn(DefaultTransactionAttribute.java:185-186) → tm.getTransaction → doBegin → invocation.proceed → parse() 抛 IOException → catch → completeTransactionAfterThrowing → rollbackOn(IOException) → rollbackRules 无命中→super.rollbackOn→IOException 不是 RuntimeException/Error → false → **不回滚** → commitTransactionAfterReturning → conn.commit() → 脏数据持久化 ❌

关键设计: **Why checked 异常默认不回滚？** rollbackOn 的默认规则是 RuntimeException|Error — 这是"未检查异常表示编程错误, 检查异常表示业务预期"的 Java 惯例映射。业务代码抛 IOException 通常意味着"可恢复/需处理" — 若默认回滚会导致事务边界不可预测。需回滚 checked 异常时显式 `rollbackFor=Exception.class` — 把决策权交给开发者。[模式: 策略模式 — rollback 规则可配置]

### 3. 多数据源 + 传播行为 — #5选错TM + #6 NEVER/SUPPORTS语义

场景: 项目有 orderDataSource 和 productDataSource(两个 PlatformTransactionManager)— `@Transactional` 不指定 qualifier → determineTransactionManager 按类型 `beanFactory.getBean(TransactionManager.class)`(TransactionAspectSupport.java:535) 解析唯一 bean — 容器有多个候选 → 直接抛 `NoUniqueBeanDefinitionException`(启动即失败), 不存在"选第一个"。

源码路径:
- `TransactionAspectSupport.determineTransactionManager`(声明于 TransactionAspectSupport.java:497) → `@Transactional` 无 qualifier → L535 `beanFactory.getBean(TransactionManager.class)` 按类型取唯一 bean — 无"取第一个"逻辑 → 多个 TM 候选 → 抛 `NoUniqueBeanDefinitionException`; 修复: `@Transactional("orderTm")` 显式指定 qualifier → L512 `determineQualifiedTransactionManager`
- `handleExistingTransaction` — NEVER: 有事态存在→IllegalTransactionStateException / SUPPORTS: 有事态参与, 无事态非事务执行

数据流: @Transactional(propagation=NEVER) onService() → 外层已有事务 → TransactionInterceptor.invoke → createTransactionIfNecessary → tm.getTransaction → handleExistingTransaction → propagation=NEVER && existingTransaction!=null → throw IllegalTransactionStateException("Existing transaction found for transaction marked with propagation 'never'") → 事务不创建 → 调用者收到异常

关键设计: **Why NEVER 和 SUPPORTS 是"边界语义"？** NEVER 声明"我的方法绝不能在事务中执行"(审计/只读任务)— 违反即报错而非静默降级; SUPPORTS 声明"有事务则参与, 无事务则非事务执行" — 是最宽松的传播。两者的存在让事务边界可以表达"强制/禁止/可选"三态 — 不只是 REQUIRED 的"总是要"。多数据源时 @Transactional 必须指定 qualifier — 否则 determineTransactionManager 按类型 getBean(TransactionManager.class) 遇多候选抛 NoUniqueBeanDefinitionException。[模式: 状态模式 — 传播行为枚举化]

### 4. 跨线程 + 非事务引擎 — #7 ThreadLocal隔离 + #8 MyISAM

场景: @Transactional 方法内用 CompletableFuture 异步执行数据库操作 — 新线程没有事务上下文 → 各自独立提交。MyISAM 引擎本身不支持事务 — Spring 的 commit/rollback 无效。

源码路径:
- `TransactionSynchronizationManager.getResource(DataSource)` — `resources` 是 `ThreadLocal<Map<Object,Object>>` — 每个线程独立存储 — 新线程无Connection→无事务
- MyISAM: 数据库引擎层面不支持事务(MySQL)—commit/rollback无效—不是Spring问题

数据流: @Transactional createOrder() → CompletableFuture.runAsync(() -> orderRepo.save()) → 新线程 ForkJoinPool-1 → save() 内部调 DataSourceUtils.getConnection(ds) → TransactionSynchronizationManager.getResource(ds) → ThreadLocal.get() → null(ForkJoinPool-1的ThreadLocal与Tomcat线程独立) → 获取新连接 → setAutoCommit(true)(默认) → INSERT → 立即提交(非事务) → 主线程 createOrder 回滚 → ForkJoinPool-1的INSERT 已提交 → 数据不一致

关键设计: **Why ThreadLocal 导致跨线程事务失效？** 事务上下文(Connection/同步器)绑定在发起线程的 ThreadLocal — CompletableFuture 的工作线程是全新 ThreadLocal → getResource 返回 null → 走非事务路径(新连接+autoCommit)。这是"事务即线程本地资源"设计的固有边界 — 跨线程事务需手动传播(TransactionTemplate + 显式传 Connection)或用 Spring 的 TransactionAware 线程池。[模式: ThreadLocal 存储 — 事务上下文与线程绑定]

→ spring-tx 第二域完成。8种失效场景 + 4类根因(代理/异常/多TM/线程)。引出 T-3: 传播行为 — REQUIRED/REQUIRES_NEW/NESTED/SUPPORTS/NOT_SUPPORTED/MANDATORY/NEVER 7种传播的源码机制。
