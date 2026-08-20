# J-1 JdbcTemplate — execute 模板 + RowMapper 结果映射

> 依赖 T-1 @Transactional 链路 + T-4 异常翻译 | 🟡 Working | 1 KP | [模式: 模板方法]

**读者处境**: T-1 中同一事务的所有操作共享 Connection — JdbcTemplate 怎么管理连接？execute 模板怎么把 15 行 JDBC 样板代码压缩成 1 个 callback？

### 1. JdbcTemplate.execute — JDBC 资源管理的模板方法

场景: `jdbcTemplate.query("SELECT ...")` 一行代码 — 背后是 15 行 JDBC 样板: 获取连接 → 创建 Statement → 执行 → 遍历结果 → 关闭 Statement → 释放连接 → 异常翻译。execute 模板把这 7 步固定, 只留"执行什么 SQL"给回调。

源码路径:
- `JdbcTemplate.java:385` — **execute(StatementCallback, closeResources)**: ①`DataSourceUtils.getConnection(obtainDataSource())`→先从TransactionSynchronizationManager ThreadLocal取(事务中有) / 无→dataSource.getConnection ②`con.createStatement()` ③`action.doInStatement(stmt)`—callback执行SQL ④`finally: DataSourceUtils.releaseConnection(con)`—若事务中→不真关闭(保留给ThreadLocal) / 非事务→close ⑤`catch SQLException: getExceptionTranslator().translate(task, sql, ex)`→DataAccessException子类
- `JdbcTemplate.java:420` — **execute(StatementCallback)**: 单参重载—`closeResources=true`—Delegate to execute(action, true)

数据流: jdbcTemplate.query("SELECT * FROM users WHERE id=?", ps -> ps.setLong(1, id), rowMapper) → JdbcTemplate.query(sql, PreparedStatementSetter, RowMapper)→execute(new PreparedStatementCallback(){doInPreparedStatement: ps.setLong(1, id); rs=ps.executeQuery(); ResultSetExtractor.extractData(rs)})→L385 execute→L388 DataSourceUtils.getConnection(ds)(L392 是 applyStatementSettings(stmt))→TransactionSynchronizationManager.getResource(ds)→ThreadLocal.get→若事务中有→返回事务的Connection / 无→ds.getConnection()→con.prepareStatement(sql)→action.doInPreparedStatement(ps)→rs.next()→RowMapper.mapRow(rs, 1)→User{id,name}→finally: DataSourceUtils.releaseConnection→若事务中→不关闭(return to ThreadLocal) / 非事务→con.close()

关键设计: **Why 模板方法把"连接管理"和"SQL执行"分离？** 连接生命周期(getConnection→prepareStatement→releaseConnection→close)在每个 SQL 操作中完全相同 — 只有"执行什么 SQL + 怎么处理结果"不同。

模板方法固定前者, 用 Callback 抽象后者 — 这是 GoF Template Method 的典型应用: 骨架固定, 变化点通过 callback 注入。同时解决了"事务内连接共享": DataSourceUtils.getConnection 先从 ThreadLocal 取(事务中的连接) — 非事务才新建 — 这正是 T-1 事务连接复用的实现层。三个 execute 重载(execute/query/update)共享同一模板 — 差异只在回调类型(StatementCallback/PreparedStatementSetter/ResultSetExtractor)。

### 2. RowMapper 映射 — ResultSet→Object 的接口抽象

场景: `SELECT * FROM users` 返回 3 行数据 — 每行都要变成 User 对象 — 传统 JDBC 要手写 `while(rs.next())` 循环 + 手动关闭 ResultSet — RowMapper 把"行→对象"的映射逻辑交给你 — 循环与资源管理交给 JdbcTemplate。

源码路径:
- `RowMapper.java:65` — **mapRow(ResultSet rs, int rowNum)**: 单方法接口(声明于 L52)— 返回"一行映射后的对象"
- `RowMapperResultSetExtractor.java:90-94` — **extractData()**: `while (rs.next()) { results.add(this.rowMapper.mapRow(rs, rowNum++)); }` — JdbcTemplate 负责遍历与计数
- `JdbcTemplate.java:485` — **query(sql, RowMapper)**: 把 RowMapper 包装为 `RowMapperResultSetExtractor` → 走 execute 模板

关键设计: RowMapper 替代了 `while(rs.next()){ user.setId(rs.getLong("id")); list.add(user); }` — JdbcTemplate 帮你遍历 ResultSet + 逐行调用 mapRow(rowNum) → 自动管理 ResultSet 关闭 + 异常包装 → 开发者只关心"一行数据怎么映射成对象"。

RowMapper vs RowCallbackHandler vs ResultSetExtractor 三个接口的区别: RowMapper 逐行映射并收集进 List(最常用); RowCallbackHandler 逐行处理但**不收集**(适合流式处理/聚合); ResultSetExtractor 拿到**整个 ResultSet** 自行控制(最灵活, RowMapperResultSetExtractor 本身就是其实现)。三个接口按"控制粒度递增"设计。

选择原则: 90% 场景 RowMapper 够用 — 需要分页/统计等聚合时用 ResultSetExtractor — 逐行侧写日志时用 RowCallbackHandler。

数据流: jdbcTemplate.query("SELECT * FROM users", (rs, rowNum) -> new User(rs.getLong("id"), rs.getString("name"))) → JdbcTemplate.query(sql, RowMapper)→execute(new QueryStatementCallback())→stmt.executeQuery→ResultSet rs→new RowMapperResultSetExtractor(rowMapper)→extractData(rs)→rowNum=0; while(rs.next())→User u = rowMapper.mapRow(rs, 0)→u={id:1, name:"Alice"}→results.add(u)→rowNum++→rs.next()→...→rs.next()=false→finally: JdbcUtils.closeResultSet(rs)→return List<User> [3 users]

→ spring-jdbc 第一域完成。JdbcTemplate.execute模板 + RowMapper 映射。引出 J-2: RowMapper + ResultSetExtractor + NamedParameterJdbcTemplate — 更丰富的结果集处理。
