# J-3 🟡 batchUpdate / TransactionCallback — spring-jdbc 高级操作

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | JdbcTemplate batchUpdate(>1030)
> 基线: J-1 execute模板 — batchUpdate 和 TransactionCallback 是基于 execute 的不同 callback 策略

---

## §0.8

- 🟡 Working，1篇 — batchUpdate批量操作(PreparedStatement.addBatch/executeBatch) + TransactionCallback事务回调(事务内执行多个JdbcTemplate操作)

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| JdbcTemplate.java:1030 | batchUpdate(sql, BatchPreparedStatementSetter) | **批量**: pss.setValues(ps, i)→ps.addBatch()→循环→ps.executeBatch()→int[]受影响行数 | High |
| JdbcTemplate.java:TransactionCallback | execute(TransactionCallback) | **事务回调**: 在同一TransactionStatus下执行多个JdbcTemplate操作→getConnection复用同一事务连接 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: batchUpdate 和 TransactionCallback 是 J-1/J-2 的补充 — 前者解决"批量插入性能"(循环addBatch→一次executeBatch减少网络往返)—后者解决"多步操作在同一个事务内"。1篇(~25行)。

**P1** 🔴: batchUpdate的批量优化 — **为什么10K条INSERT用batchUpdate比循环update快10-100×**: 循环update=每行一个网络往返(10K往返)—batchUpdate=所有行汇聚到一次executeBatch(1个往返)
