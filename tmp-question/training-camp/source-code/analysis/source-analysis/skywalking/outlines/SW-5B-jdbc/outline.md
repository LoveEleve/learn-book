# SW-5B JDBC storage family — Outline

## 1. 范围

- JDBCClient / HikariCP
- JDBCBatchDAO
- BatchSQLExecutor / SQLExecutor / JDBCSQLExecutor
- JDBCTableInstaller / TableHelper
- MySQL/PostgreSQL 方言、TTL 与历史表生命周期

## 2. 已确认问题

### 2.1 JDBCBatchDAO 吞异常

实现位置：`storage-jdbc-hikaricp-plugin/src/main/java/org/apache/skywalking/oap/server/storage/plugin/jdbc/common/dao/JDBCBatchDAO.java`

原行为：SQL group 执行异常仅日志记录，`flush()` 仍返回成功 future。

修复决策：保留跨 request 的失败聚合，但向上游返回 failed future，使 PersistenceTimer 和调用方感知整体失败。

### 2.2 BatchSQLExecutor 非法 batch size

实现位置：`storage-jdbc-hikaricp-plugin/src/main/java/org/apache/skywalking/oap/server/storage/plugin/jdbc/BatchSQLExecutor.java`

原行为：`maxBatchSqlSize <= 0` 时没有明确校验。

修复决策：入口要求 `maxBatchSqlSize > 0`，非法值立即抛 `IllegalArgumentException`。

### 2.3 Batch result 长度不一致

实现位置：`storage-jdbc-hikaricp-plugin/src/main/java/org/apache/skywalking/oap/server/storage/plugin/jdbc/BatchSQLExecutor.java`

原行为：`executeBatch()` 返回长度与请求数不一致时可能越界或静默漏回调。

修复决策：显式校验结果长度，不匹配即抛 `SQLException`。

### 2.4 主 SQL/additional SQL 非原子事务

实现位置：`storage-jdbc-hikaricp-plugin/src/main/java/org/apache/skywalking/oap/server/storage/plugin/jdbc/common/dao/JDBCBatchDAO.java` 与 `storage-jdbc-hikaricp-plugin/src/main/java/org/apache/skywalking/oap/server/storage/plugin/jdbc/SQLExecutor.java`

原行为：主 SQL 与 additional SQL 被拆到不同 batch group，各自独立获取 auto-commit 连接，存在半写入。

修复决策：
- `JDBCBatchDAO.flush(...)` 按原始 request 边界执行；
- `SQLExecutor.invokeInTransaction(JDBCClient)` 在同一连接中执行主 SQL 与 additional SQL，并负责 commit/rollback；
- 保留跨 request 的失败聚合，不再跨 request 复用 SQL grouping。

### 2.5 JDBCClient 生命周期

实现位置：`server-library/library-client/src/main/java/org/apache/skywalking/oap/server/library/client/jdbc/hikaricp/JDBCClient.java`

原行为：`setAutoCommit()` 失败会泄漏连接；未 connect/重复 shutdown 会 NPE。

修复决策：失败路径关闭连接并合并 suppressed exception；shutdown null-safe 且幂等。

## 3. 待审计问题

- JDBC auto-commit 业务事务策略。
- HikariCP shutdown 之后重连/重复 connect 语义。
- JDBCTableInstaller 历史表创建、TTL 删除和 MySQL/PostgreSQL 方言差异。

## 4. 当前状态

SW-5B 已完成 Pass0，确认并修复 1 个公共 JDBC batch 异常语义缺陷。下一步优先审计 `BatchSQLExecutor` batch size 和 executeBatch result 回调边界。
