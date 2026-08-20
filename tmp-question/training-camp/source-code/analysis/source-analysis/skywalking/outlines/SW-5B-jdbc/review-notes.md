# SW-5B JDBC storage family — Review Notes

## Review 1

- 盘点 JDBC/HikariCP 公共模块、DAO、BatchSQLExecutor、SQLExecutor、TableInstaller 与现有测试。
- 确认公共 batch 写入链是所有 JDBC storage DAO 的共同上游。
- 现有测试主要是 PostgreSQL Testcontainers 集成测试，缺少 JDBCBatchDAO 失败路径单测。

## Review 2

- `JDBCBatchDAO.flush(...)` 在 SQL group 执行异常后只日志记录，并返回 completed future。
- 用 mock JDBCClient 让 `getConnection()` 抛 `SQLException`，测试稳定失败，确认是真实生产缺陷。

## Review 3

- 修复为继续执行独立 SQL group，同时累计失败并返回 failed future。
- 验证首个异常作为主异常、后续异常通过 suppressed 聚合的设计。
- Checkstyle 通过，未扩大到具体 backend DAO。

## Review 4

- 定向命令：`./mvnw -pl oap-server/server-storage-plugin/storage-jdbc-hikaricp-plugin -am -Dtest=JDBCBatchDAOFailureTest -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：`BUILD SUCCESS`。
- 完整 JDBC reactor：2 tests，0 failures；依赖 `server-core`：226 tests，0 failures。

## Review 5

- 对 `BatchSQLExecutor.invoke(maxBatchSqlSize)` 构造 0 与负数 harness。
- 测试确认原实现没有明确配置校验：0 会绕过分批条件，负数导致间接异常。
- 修复为入口校验 `maxBatchSqlSize > 0`，非法值抛 `IllegalArgumentException`。

## Review 6

- 定向命令：`./mvnw -pl oap-server/server-storage-plugin/storage-jdbc-hikaricp-plugin -am -Dtest=BatchSQLExecutorBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：`BUILD SUCCESS`。
- JDBC 模块完整回归已执行：待下一步最终结果确认。

## Review 7

- 构造 mock `PreparedStatement.executeBatch()` 返回长度大于 request 数的 harness。
- 原实现稳定抛出 `IndexOutOfBoundsException`，确认结果长度不匹配时回调索引不安全。
- 修复为显式校验结果长度，抛出带上下文的 `SQLException`。

## Review 8

- 定向命令：`./mvnw -pl oap-server/server-storage-plugin/storage-jdbc-hikaricp-plugin -am -Dtest=BatchSQLExecutorResultBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：`BUILD SUCCESS`。
- JDBC 完整模块回归：4 tests，0 failures，`BUILD SUCCESS`。

## Review 9

- 构造主 SQL + additional SQL harness，验证一次业务写入的 SQL group 执行路径。
- 确认 `JDBCBatchDAO` 会为主 SQL 与 additional SQL 分别创建 `BatchSQLExecutor`，并分别获取 JDBC connection。
- 当前实现默认 auto-commit，因此存在主表成功、additional 表失败时的半写入风险。
- 本轮未直接重构事务模型，避免在缺少完整回滚/回调契约前扩大改动。

## Review 10

- 定向命令：`./mvnw -pl oap-server/server-storage-plugin/storage-jdbc-hikaricp-plugin -am -Dtest=JDBCBatchDAOTransactionTest -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：`BUILD SUCCESS`。
- JDBC 模块已有 4 个回归测试均通过过；本轮事务测试单独通过。

## Review 11

- 使用 mock `HikariDataSource` 与 `Connection` 验证 `setAutoCommit(...)` 失败路径。
- 原实现未关闭已取得连接，确认连接池泄漏风险。
- 验证未 connect 直接 shutdown 原实现 NPE，确认生命周期缺陷。
- 修复连接失败回收与 shutdown 幂等/null-safe 行为。

## Review 12

- 定向命令：`./mvnw -pl oap-server/server-library/library-client -am -Dtest=JDBCClientLifecycleTest -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：`BUILD SUCCESS`。
- 完整 library-client reactor：3 tests，0 failures，`BUILD SUCCESS`。
- Checkstyle 通过。

## Review 13

- 使用 `newDataSource()` 工厂方法让 `JDBCClient.connect()` 在无真实 JDBC driver 下也能被纯 mock 验证。
- 确认重复 `connect()` 时旧 `HikariDataSource` 会被关闭，不再泄漏连接池。

## Review 14

- 完成原子事务改造：`JDBCBatchDAO.flush(...)` 改为按原始 request 执行；`SQLExecutor` 新增 `invokeInTransaction(JDBCClient)`，在同一连接中执行主 SQL 与 additional SQL，并负责 commit/rollback。
- 事务回归 `JDBCBatchDAOTransactionTest` 已确认：additional SQL 失败时整体 future 失败且触发 rollback。
- JDBC 模块完整回归：5 tests，0 failures，`BUILD SUCCESS`。

## Review 15

- 构造 `TableMetaInfoMutationTest`，验证 `addModel()` 后原始 `Model` 的列集合是否被直接裁剪。
- 测试稳定证明原始 `Model` 会被污染，确认这是共享对象副作用 bug。
- 修复为向 JDBC 注册表放入裁剪后的 `Model` 副本，保留原始 `Model` 不变。

## Review 16

- 定向命令：`./mvnw -pl oap-server/server-storage-plugin/storage-jdbc-hikaricp-plugin -am -Dtest=TableMetaInfoMutationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：`BUILD SUCCESS`。
- 相关 reactor：`storage-jdbc-hikaricp-plugin` 与依赖 `server-core` 均 `BUILD SUCCESS`。

## 未决项

- JDBC auto-commit 业务事务策略。
- HikariCP 重连/连接池配置与 TTL/schema 方言。
