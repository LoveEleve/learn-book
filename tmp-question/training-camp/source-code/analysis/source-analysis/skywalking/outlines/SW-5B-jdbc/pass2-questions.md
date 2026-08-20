# SW-5B JDBC storage family — Pass 2 问题收敛

## Q1: JDBCBatchDAO 是否吞掉 batch 执行异常

结论：确认真实生产缺陷，已修复。

`JDBCBatchDAO.flush(...)` 原先在每个 SQL group 中捕获异常后只记录日志，最后无条件返回 `CompletableFuture.completedFuture(null)`。因此 JDBC 连接失败、prepare failure 或 executeBatch failure 都会被上游 `PersistenceTimer` 视为成功，导致：

- failed future 不会触发上游错误计数。
- batch 数据不会进入明确的失败/重试路径。
- 同一轮可能出现部分 SQL group 成功、部分失败但整体显示成功。

修复后：

- 独立 SQL group 仍继续执行，不因一组失败阻塞其他 group。
- 第一个失败作为主异常返回，后续失败通过 `addSuppressed(...)` 聚合。
- 有任意失败时返回 `CompletableFuture.failedFuture(failure)`。
- 所有 group 成功时仍返回 completed future。

回归测试：

- `JDBCBatchDAOFailureTest.shouldPropagateBatchExecutionFailure`

验证：

- JDBC 模块完整 reactor：2 tests，0 failures，`BUILD SUCCESS`。
- 依赖 `server-core`：226 tests，0 failures。

## Q2: BatchSQLExecutor 是否校验 maxBatchSqlSize

结论：确认真实边界缺陷，已修复。

`BatchSQLExecutor.invoke(...)` 原先接受 `0` 或负数：
- `0` 会使分批条件永远不成立，所有请求累积到最后一次执行，失去配置的 batch 上限。
- 负数会在 `ArrayList` 初始化或后续流程中产生间接异常，错误信息不指向配置。

修复后，方法入口明确要求 `maxBatchSqlSize > 0`，非法值立即抛出 `IllegalArgumentException`，且不会获取 JDBC connection。

回归测试：
- `BatchSQLExecutorBoundaryTest.shouldRejectNonPositiveBatchSize`

验证：
- 定向 reactor：`BUILD SUCCESS`
- JDBC 完整模块回归：待本轮最终执行确认。

## Q3: executeBatch 返回结果长度是否安全

结论：确认真实边界缺陷，已修复。

`BatchSQLExecutor.executeBatch(...)` 原先直接遍历 JDBC 返回数组并以结果索引访问 `bulkRequest`：
- 返回数组过长时触发 `IndexOutOfBoundsException`。
- 返回数组过短时部分请求没有回调，形成静默状态不一致。

修复后，返回数组长度必须等于当前 bulk request 数；否则抛出带有实际长度和请求长度的 `SQLException`，由 `JDBCBatchDAO` 的失败聚合路径继续上报。

回归测试：
- `BatchSQLExecutorResultBoundaryTest.shouldRejectUnexpectedBatchResultLength`

验证：
- 定向 reactor：`BUILD SUCCESS`
- JDBC 完整模块回归：4 tests，0 failures，`BUILD SUCCESS`。

## Q4: 主 SQL 与 additional SQL 是否处于同一事务

结论：确认真实事务缺陷，已修复。

`JDBCSQLExecutor` 将主表 SQL 与 additional table SQL 挂在同一个 `SQLExecutor` 上，但 `JDBCBatchDAO.flush(...)` 原先会将它们摊平、按 SQL 分组，再分别创建 `BatchSQLExecutor`。每个 `BatchSQLExecutor` 都独立调用 `JDBCClient.getConnection()`，默认 auto-commit，因此同一业务记录的主表和 additional 表写入不在同一事务里；若第二组失败，第一组可能已经提交，留下半写入。

修复后：
- `JDBCBatchDAO.flush(...)` 按原始 `PrepareRequest` 边界逐个执行，不再先跨 request 摊平并按 SQL 文本分组。
- `SQLExecutor` 新增 `invokeInTransaction(JDBCClient)`，在同一 `getConnection(false)` 连接中执行主 SQL 与 additional SQL，并在成功时 commit、失败时 rollback。
- 失败仍通过 `CompletableFuture.failedFuture(...)` 向上游传播，保留跨 request 的失败聚合。

回归测试：
- `JDBCBatchDAOTransactionTest.shouldRollbackMainAndAdditionalSqlTogether`
- `JDBCBatchDAOFailureTest.shouldPropagateBatchExecutionFailure`

验证：
- JDBC 模块完整 reactor：5 tests，0 failures，`BUILD SUCCESS`。
- 依赖 `server-core`：226 tests，0 failures。

## Q5: JDBCClient 连接生命周期是否安全

结论：确认 3 个真实缺陷，已修复。

1. `getConnection(boolean)` 先从 HikariCP 获取连接，再调用 `setAutoCommit(...)`。若设置失败，原实现直接抛异常，连接没有关闭，可能导致连接池泄漏。
2. `shutdown()` 在 `connect()` 前调用会因 `dataSource == null` 抛 NPE；重复 shutdown 也不安全。
3. `connect()` 在已有 `dataSource` 时直接覆盖字段，不关闭旧连接池，重复 connect 会泄漏旧 `HikariDataSource`。

修复后：
- auto-commit 设置失败时关闭连接；若 close 也失败，将 close 异常作为 suppressed exception 添加到原异常。
- `shutdown()` 对 null 安全，关闭后清空 dataSource，支持重复调用。
- `connect()` 先创建 replacement，再替换并关闭 previous；为测试暴露 `newDataSource()` 工厂方法。

回归测试：
- `JDBCClientLifecycleTest.shouldCloseConnectionWhenAutoCommitSetupFails`
- `JDBCClientLifecycleTest.shouldAllowShutdownBeforeConnect`
- `JDBCClientLifecycleTest.shouldClosePreviousDataSourceBeforeReconnect`

验证：
- library-client 完整 reactor：3 tests，0 failures，`BUILD SUCCESS`。

## Q6: TableMetaInfo 注册模型时是否会污染原始 Model

结论：确认真实副作用缺陷，已修复。

`TableMetaInfo.addModel(Model)` 原先直接在传入的 `model.getColumns()` 上执行 `removeAll(excludeColumns)`。这会永久修改调用方持有的原始 `Model`，使 additional-entity 被排除的列从共享模型对象中消失，影响后续若复用同一 `Model` 的逻辑。

修复后，`TableMetaInfo` 构造一个仅供 JDBC 使用的裁剪后 `Model` 副本放入注册表，保留原始 `Model` 不变。

回归测试：
- `TableMetaInfoMutationTest.shouldNotMutateOriginalModelColumnsWhenRegistering`

验证：
- 定向 reactor：`BUILD SUCCESS`
- JDBC 完整模块回归：待本轮最终执行确认。

## Q7-Q11

JDBC auto-commit 的业务事务策略与 JDBC TTL/schema 方言仍待后续专项设计与 harness。
