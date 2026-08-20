# SW-5B JDBC storage family — Pass 0 发现

> 项目: Apache SkyWalking 10.4.0
> 日期: 2026-08-18
> 范围: `storage-jdbc-hikaricp-plugin`

## 1. 域边界

SW-5B 聚焦 JDBC/HikariCP 公共实现，不在本轮展开具体业务 DAO 的查询语义：

- `JDBCClient` / HikariCP 连接生命周期
- `JDBCBatchDAO`
- `BatchSQLExecutor`
- `SQLExecutor`
- `JDBCSQLExecutor`
- `JDBCTableInstaller` / `TableHelper`
- MySQL/PostgreSQL 方言差异
- JDBC TTL、历史表删除和 schema 升级

## 2. 主链

```text
PersistenceWorker.buildBatchRequests()
  -> PersistenceTimer
  -> JDBCBatchDAO.flush(...)
  -> SQLExecutor grouping
  -> BatchSQLExecutor
  -> JDBCClient.getConnection()
  -> PreparedStatement.executeBatch()
```

## 3. 首轮高风险问题

- batch SQL 执行异常是否被吞掉，导致上游误判成功。
- 不同 SQL group 是否互相隔离，同时是否仍能向调用方报告整体失败。
- `maxBatchSqlSize` 边界值是否导致空 batch、漏执行或错误回调。
- HikariCP connection 的 auto-commit、关闭与异常回收契约。
- `SQLExecutor` additional SQL 是否与主 SQL 同批次、同事务。
- 历史表 TTL 删除与新表创建是否跨数据库方言一致。

## 4. 当前测试现实

- 现有 JDBC 测试主要是 PostgreSQL Testcontainers 集成测试。
- 公共 `JDBCBatchDAO` 失败路径此前没有单元测试。
- MySQL 当前仅有驱动防重分发测试。

## 5. Pass 0 结论

优先审计公共 batch 执行链。它位于所有 JDBC 写入 DAO 的共同上游，异常语义一旦错误会影响所有 metrics、record、TopN 和 management 写入。
