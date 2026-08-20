# J-3 🟡 batchUpdate + TransactionCallback — 批处理与事务回调

> 依赖 J-1 JdbcTemplate + T-1 @Transactional | 🟡 Working | 1 KP | [模式: 模板方法]

**读者处境**: J-1 的 execute 是单条操作 — 但"插入 10K 条记录"用循环 update 性能很差。batchUpdate 把多条操作汇聚到一次数据库 round trip。TransactionCallback 支持在同一个事务内执行多个 JdbcTemplate 操作。

### 1. batchUpdate — PreparedStatement.addBatch + executeBatch

场景: `batchUpdate("INSERT INTO users(name,age) VALUES(?,?)", new BatchPreparedStatementSetter(){...})` — 10K 行参数通过回调逐行喂给 JdbcTemplate, 攒成一整批交给数据库一次执行。

源码路径:
- `JdbcTemplate.java:1040` — **batchUpdate(String, BatchPreparedStatementSetter)**: :1044 `int batchSize = pss.getBatchSize()`(0 则直接返回空数组)→:1049 `int[] result = execute(sql, getPreparedStatementCallback(pss, null))`
- `JdbcTemplate.java:1599` — **getPreparedStatementCallback()**: 返回 PreparedStatementCallback — :1609 `JdbcUtils.supportsBatchUpdates(ps.getConnection())` 判断驱动是否支持批处理→:1610 `for (int i = 0; i < batchSize; i++)` → :1611 `pss.setValues(ps, i)` → :1615 `ps.addBatch()` → :1617 `int[] results = ps.executeBatch()` → 返回 `int[]`(每行影响行数)
- `JdbcTemplate.java:571` — **batchUpdate(String... sql)**: 无参数版本 — :589 `stmt.addBatch(sqlStmt)` → :592 `rowsAffected = stmt.executeBatch()`
- `JdbcTemplate.java:1030` — **batchUpdate(psc, pss, KeyHolder)**: 带主键回填的重载, 同样 :1033 `execute(psc, getPreparedStatementCallback(pss, generatedKeyHolder))`→成功后 :1619 storeGeneratedKeys
- `BatchPreparedStatementSetter.java:48` — **setValues(ps, i)**: 设置第 i 行参数; :54 **getBatchSize()**: 总行数

关键设计: 三个动作(设置参数/累积/执行)分离 — 设置参数是回调(变化点), 累积(addBatch)与执行(executeBatch)是模板(固定点); `executeBatch()` 一次网络往返, 相对 10K 次 executeUpdate 的往返数是 1/10000。不支持批处理的驱动自动退化为逐条执行。

数据流: batchUpdate(sql, setter)(:1040)→getBatchSize=10000→execute(sql, callback)(:1049)→J-1 模板: getConnection→con.prepareStatement(sql)→doInPreparedStatement→:1609 supportsBatchUpdates→:1610 for i=0..9999→:1611 setValues(ps,i)(ps.setString(1,names[i]); ps.setInt(2,ages[i]))→:1615 ps.addBatch()→:1617 ps.executeBatch()→数据库一次执行 10K 行→int[10000](每行影响行数)→finally releaseConnection→返回→应用拿到 int[] 检查失败行

### 2. TransactionTemplate + TransactionCallback — 编程式事务边界

场景: 不用 @Transactional 注解(如定时任务/工具类), 用 `transactionTemplate.execute(new TransactionCallbackWithoutResult(){...})` 圈定一段代码为一个事务。

源码路径:
- `TransactionTemplate.java:130` — **execute(TransactionCallback)**: `CallbackPreferringPlatformTransactionManager`(注意前缀是 Callback, 非 TransactionCallbackPreferring)则直接委托(:133-134), 否则 doExecute — 内部 getTransaction→action.doInTransaction→commit/rollback(与 T-1 同一条 AbstractPlatformTransactionManager 链路)
- `TransactionCallbackWithoutResult.java:31` — **类声明**: :36 `public final Object doInTransaction(TransactionStatus status)` → 调用 :54 `protected abstract void doInTransactionWithoutResult(TransactionStatus status)` — 抽象方法名是 doInTransactionWithoutResult(带 WithoutResult), 而非 doInTransaction
- `TransactionCallback.java:56` — **doInTransaction(TransactionStatus)**: 接口方法 — 返回 Object 表示事务结果(有结果用 TransactionCallback<T>, 无结果用 WithoutResult 子类)

关键设计: 回调式编程事务 — 事务的 begin/commit/rollback 由模板控制, 回调只写业务(两个 update); 回调抛 RuntimeException→模板走 doRollback, 不抛→doCommit。多个 JdbcTemplate 调用在同一个事务内共享同一个 Connection(ThreadLocal 绑定, 见 T-1)。

数据流: TransactionTemplate.execute(new TransactionCallbackWithoutResult(){doInTransactionWithoutResult(status){ jdbcTemplate.update(sql1); jdbcTemplate.update(sql2); }})(:130)→getTransaction(同 T-1 链路)→begin→doInTransactionWithoutResult→jdbcTemplate.update(sql1)→DataSourceUtils.getConnection 从 ThreadLocal 取到事务连接→执行→update(sql2)→同一连接→执行→回调正常返回→doCommit→两个 update 同时生效 / 任一 update 抛异常→回调异常向外抛→doRollback→两个 update 全部撤销

→ spring-jdbc **全3域完成**。JdbcTemplate→RowMapper/ResultSetExtractor→batchUpdate—Spring JDBC 三层抽象全完。Stage 5 spring-jdbc 层收官 → next: Stage 6 spring-web/webmvc 层。
