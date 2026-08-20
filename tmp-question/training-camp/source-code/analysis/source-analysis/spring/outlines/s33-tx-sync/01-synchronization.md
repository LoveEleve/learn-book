# T-5 TransactionSynchronization — 事务提交前后的 5 个回调钩子

> 依赖 T-1 链路 | 🟡 Working | 1 KP | [模式: 观察者]

**读者处境**: T-1 commit/rollback 触发 TransactionSynchronization 回调 — @TransactionalEventListener(phase=AFTER_COMMIT) 怎么工作的？afterCommit 和 afterCompletion 有什么区别？

### 1. TransactionSynchronization 5个回调 + ThreadLocal 管理

场景: 业务方法在 TransactionSynchronizationManager.registerSynchronization 注册回调 → AbstractPlatformTransactionManager.commit → triggerBeforeCommit(事务提交前) → doCommit(真正提交) → triggerAfterCommit(提交后) → triggerAfterCompletion(完成时—无论成功失败)。rollback → 不调用 beforeCommit/afterCommit → 直接 triggerAfterCompletion。

源码路径:
- `TransactionSynchronization.java:45` — **5个回调接口**: **beforeCommit**(readOnly)—提交前一刻(事务尚未提交—若抛异常将中止提交、由 doRollbackOnCommitException 走回滚, AbstractPlatformTransactionManager.java:775/815) → **beforeCompletion**()—事务完成前(cleanup之前) → **afterCommit**()—事务提交后(已确认commit成功) → **afterCompletion**(STATUS_COMMITTED/ROLLED_BACK/UNKNOWN)—事务完成(无论成功失败) → **flush**()—Hibernate Session.flush
- `TransactionSynchronizationManager.java:79` — **管理器**: `ThreadLocal<Set<TransactionSynchronization>>`(L79-80)— initSynchronization(L254) → registerSynchronization(L271) → getSynchronizations(L289, @Order排序) → clearSynchronization(L316)
- `AbstractPlatformTransactionManager.commit`: triggerBeforeCommit→doCommit→triggerAfterCommit→triggerAfterCompletion(status=COMMITTED) / rollback: triggerAfterCompletion(status=ROLLED_BACK)

数据流: @Transactional → getTransaction → TransactionSynchronizationManager.initSynchronization → doBegin → business.registerSynchronization(new TransactionSynchronization(){afterCommit: sendMQ; afterCompletion: cleanup}) → ThreadLocal.set([sync]) → proceed → commit → triggerBeforeCommit → sync.beforeCommit(事务未提交但即将) → doCommit(conn.commit()) → triggerAfterCommit → sync.afterCommit: sendKafkaMessage("order.paid", orderId) → Kafka确认收到 → triggerAfterCompletion → sync.afterCompletion(STATUS_COMMITTED): cleanupResources → TransactionSynchronizationManager.clearSynchronization

**关键**: afterCommit 在 doCommit 之后(事务已提交) → 适合"事务成功后触发幂等动作"(发MQ/刷新缓存/调用外部系统) — afterCompletion 在事务完成时(无论成功失败) → 适合资源清理(关闭连接/释放锁)。

**使用场景选择**: 发消息/事件→afterCommit(只在成功时); 资源清理/埋点→afterCompletion(无论成败); 事务前准备→beforeCommit; 乐观锁版本校验→beforeCommit(抛异常可中止提交)。多个 sync 按注册顺序执行 — 可通过 @Order 调整先后。

事务嵌套时每个事务层级有独立的 synchronization 集合 — `isNewSynchronization()`(L576/985 等)决定回调属于哪个层级: REQUIRES_NEW 产生新 status+新 sync 集合 — 内层回调在**内层提交时**(processCommit)触发, 外层回调在外层提交时触发 — 这是"外层成功内层已提交"时回调时序容易混淆的根源。

纯 REQUIRED 嵌套则共享外层 status — 回调只在最外层提交时触发一次 — 内层方法返回不触发任何回调。

关键设计: **Why afterCommit 与 afterCompletion 分离？** 两者语义不同: afterCommit 只在**事务确认成功**后调用 — 且早于 afterCompletion — 适合"提交成功后必须做"的动作(发消息/发布事件)。

afterCompletion 在**任何结局**(COMMITTED/ROLLED_BACK/UNKNOWN)都调用 — 适合"无论结果如何都要清理"的动作。若只提供一个回调 — 要么在提交前做(失败会白做)要么在完成时做(无法区分成功失败)。

**触发顺序的精确链**(AbstractPlatformTransactionManager.processCommit): ①triggerBeforeCommit(readOnly)—抛异常→doRollbackOnCommitException ②triggerBeforeCompletion ③doCommit(真正提交数据库) ④triggerAfterCommit ⑤triggerAfterCompletion(STATUS_COMMITTED)。

rollback 路径: ①triggerBeforeCompletion ②doRollback ③triggerAfterCompletion(STATUS_ROLLED_BACK) — 不触发 beforeCommit/afterCommit。

**@TransactionalEventListener 的关系**: Spring 的 @TransactionalEventListener(phase=AFTER_COMMIT) 在 afterCompletion 回调内检测事务状态并发布事件 — 即它**建立在这个 5 回调机制之上** — 若事务回滚则事件不发布(或按 phase 不同行为)。这也是"事务成功后发 MQ 不丢失"的官方推荐模式 — 比手动注册 TransactionSynchronization 更简洁。

→ spring-tx **全5域完成**。TransactionSynchronization 5个回调钩子→Spring 事务管理的最后一环。Stage 4 spring-tx 层收官 → next: Stage 5 spring-jdbc 层(JdbcTemplate/RowMapper/StatementCallback)。
