# T-3 传播行为 — getTransaction 中的 7 种事务状态转换

> 依赖 T-1 链路 | 🟡 Working | 1 KP | [模式: 状态模式]

**读者处境**: T-1 中 getTransaction 对 propagation 做了分支判断 — 但 7 种传播具体做什么？REQUIRES_NEW 的"挂起当前"和 NESTED 的"savepoint"有什么区别？

### 1. AbstractPlatformTransactionManager.getTransaction — 无现有事务时的三种分支

场景: `@Transactional(propagation=REQUIRES_NEW)` → 外层无事务 → getTransaction 检测到 REQUIRES_NEW → suspend(null) → startTransaction → doBegin → 创建新事务 → 返回。MANDATORY → 外层无事务 → throw Exception → 调用失败。

源码路径:
- `AbstractPlatformTransactionManager.java:373` — **getTransaction()**: 若无现有事务(L392 起)—有现有事务则 L384 return handleExistingTransaction→ ①L393 MANDATORY→throw IllegalTransactionStateException("No existing transaction found for transaction marked with propagation 'mandatory'") ②L397-399 REQUIRED/REQUIRES_NEW/NESTED→L400 suspend(null)→L405 startTransaction→doBegin→新TransactionStatus ③其他(SUPPORTS/NOT_SUPPORTED/NEVER)→return prepareTransactionStatus(null) — 非事务执行
- `TransactionDefinition.java` — **7个常量**: PROPAGATION_REQUIRED=0/SUPPORTS=1/MANDATORY=2/REQUIRES_NEW=3/NOT_SUPPORTED=4/NEVER=5/NESTED=6

数据流: @Transactional(propagation=MANDATORY) → TransactionInterceptor.invoke → tm.getTransaction → L373 getTransaction → TransactionSynchronizationManager.getResource(ds)→null(无现有事务)→L393 def.getPropagation==MANDATORY→throw IllegalTransactionStateException("No existing transaction found for transaction marked with propagation 'mandatory'")→事务不创建→异常直接抛出 → @Transactional(propagation=REQUIRED)→无事务→L397 def.getPropagation==REQUIRED→L400 suspend(null)→L405 startTransaction→doBegin→getConnection→setAutoCommit(false)→bindToThread→返回TransactionStatus→proceed→commit

关键设计: **Why REQUIRED 分支也调 suspend(null)？** getTransaction 的统一骨架: 先 suspend 当前(无事务时 suspend(null) 是空操作)→ 再 startTransaction — 骨架统一, 分支只决定"挂起什么"。MANDATORY 不创建事务 — 它要求"调用方必须已开启事务" — 违反即抛异常 — 这是"强制约束"型传播的典型。[模式: 模板方法 — getTransaction 骨架固定, 传播行为决定分支]

### 2. handleExistingTransaction — 有现有事务时的 7 种处理

场景: 外层 `@Transactional` 方法已开启事务 → 内层再调一个 `@Transactional(propagation=XXX)` 方法 → 此时 getTransaction 检测到"已有事务" → 进入 handleExistingTransaction — 7 种传播行为在此分道扬镳: 加入 / 挂起重建 / savepoint / 报错。

7 种传播行为对照 (TransactionDefinition 常量):

| 传播行为 | 值 | 无现有事务 | 有现有事务 |
|:--|:--:|------|------|
| REQUIRED | 0 | 新建事务 | 加入现有事务 |
| SUPPORTS | 1 | 非事务执行 | 加入现有事务 |
| MANDATORY | 2 | 抛异常 | 加入现有事务 |
| REQUIRES_NEW | 3 | 新建事务 | 挂起当前 + 新建独立事务 |
| NOT_SUPPORTED | 4 | 非事务执行 | 挂起当前 + 非事务执行 |
| NEVER | 5 | 非事务执行 | 抛异常 |
| NESTED | 6 | 新建事务 | savepoint 嵌套事务 |

源码路径:
- `handleExistingTransaction` (L426): ①L430 NEVER→throw IllegalTransactionStateException ②L435 NOT_SUPPORTED→L439 suspend(existing)→L441 非事务执行 ③L445 REQUIRES_NEW→L450 suspend(existing)→L452 startTransaction(new) ④L460 NESTED→L469 useSavepointForNestedTransaction→L477 status.createAndHoldSavepoint→嵌套 ⑤L494 REQUIRED/SUPPORTS/MANDATORY→加入现有事务(非新事务)

数据流: 外层 REQUIRED 有事务 → 内层 @Transactional(propagation=REQUIRES_NEW) → getTransaction → existingTransaction!=null → handleExistingTransaction → def.getPropagation==REQUIRES_NEW → suspend(existing) → TransactionSynchronizationManager.unbindResource → existingTransaction.suspend → 保存 SuspendedResourcesHolder → startTransaction → doBegin → getConnection(新连接) → setAutoCommit(false) → bindResource(新连接) → 返回新TransactionStatus → proceed → commit → cleanupAfterCompletion → resume(existing) → 恢复原事务 → 原proceed继续

关键设计: **REQUIRES_NEW vs NESTED 的本质差异** — 前者 create new physical transaction(新连接/commit独立) / 后者 use savepoint(同一连接/rollback到savepoint—不释放锁)。REQUIRES_NEW 耗资源(两个连接)但完全独立—NESTED 轻量但不独立(共享锁→可能的死锁)。

→ spring-tx 第三域完成。7种传播行为 + suspend/resume + savepoint。引出 T-4: DataAccessException 异常翻译 — SQLException→DataAccessException 映射体系。
