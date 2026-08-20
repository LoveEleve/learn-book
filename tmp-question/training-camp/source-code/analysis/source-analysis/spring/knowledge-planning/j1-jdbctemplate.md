# J-1 JdbcTemplate — execute 模板方法 + query/update + RowMapper

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | JdbcTemplate.java(1829行)
> 基线: T-1 链路 — JdbcTemplate 内部通过 DataSourceUtils.getConnection 复用同一事务的 Connection

---

## §0.8

- 🟡 Working，1篇 — JdbcTemplate.execute(StatementCallback)模板方法 + query(RowMapper) + update
- 设计模式: [模式: 模板方法]—execute定义JDBC资源管理骨架(获取连接→创建Statement→执行callback→清理资源), 子查询/更新/批量是不同callback

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| JdbcTemplate.java:385 | execute(StatementCallback) | **模板核心**: ①getConnection(DataSourceUtils→ThreadLocal优先级) ②con.createStatement ③action.doInStatement(stmt)(callback) ④finally: DataSourceUtils.releaseConnection ⑤catch: SQLException→translator.translate→DataAccessException | High |
| JdbcTemplate.java:485 | query(sql, RowMapper) | query→execute(new QueryStatementCallback())→stmt.executeQuery→ResultSet→RowMapper.mapRow(rs, rowNum)→List<T> | High |
| JdbcTemplate.java:row() | update(sql, args) | update→execute(new UpdateStatementCallback())→stmt.executeUpdate→return rows affected | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 1829行/1文件 — 但核心逻辑在execute(StatementCallback)模板(~30行)+query/update是不同callback。JdbcTemplate把JDBC的样板代码(try-getConnection-try-createStatement-try-execute-catch-catch-finally-close)封装到execute模板中。1篇(~40行)。

**P1 核心** 🔴: JdbcTemplate.execute 模板方法 — **为什么**: 手写JDBC需要 ~15行样板代码(try/catch/finally)—JdbcTemplate.execute把它们压缩到1个callback — 外部只需关心"做什么(SELECT/INSERT/UPDATE)"—execute负责"怎么做(获取连接/管理资源/异常翻译)"

**单篇结构**: §1 execute模板(连接获取→Statement创建→callback→finally资源释放) → §2 query(RowMapper)+update 使用execute的不同callback
