# J-2 RowMapper + ResultSetExtractor — 两种结果集处理策略

> 依赖 J-1 JdbcTemplate | 🟡 Working | 1 KP | [模式: 策略模式]

**读者处境**: J-1 用 lambda `(rs, rowNum) -> new User(...)` 就完成了结果映射 — 但 `(rs, rowNum) -> T` 与 `(rs) -> T` 两种签名背后是两套完全不同的处理策略: 一个"逐行交付", 一个"整体交付"。怎么选？

### 1. RowMapper(逐行) vs ResultSetExtractor(全控制)

场景: `jdbcTemplate.query("SELECT dept, name FROM emp", (rs) -> {...})` — 单参 lambda 会被编译成 `ResultSetExtractor` 实现, 拿到整个 ResultSet 自己控制遍历; 而 `(rs, rowNum) -> new User(...)` 的 lambda 是 `RowMapper`, JdbcTemplate 替你遍历并逐行回调。

源码路径:
- `JdbcTemplate.java:485` — **query(String, RowMapper)**: 包装为 `new RowMapperResultSetExtractor<>(rowMapper)` 再走 ResultSetExtractor 路径 — RowMapper 是 ResultSetExtractor 的特例
- `JdbcTemplate.java:449` — **query(String, ResultSetExtractor)**: 定义局部类 `QueryStatementCallback`(:457)→`doInStatement` 内 `rs = stmt.executeQuery(sql)`→`return rse.extractData(rs)`→:476 `return execute(new QueryStatementCallback(), true)` 复用 J-1 的 execute 模板(连接获取/释放/异常翻译都在这里)
- `RowMapperResultSetExtractor.java:61` — **extractData()(:90)**: :92 `int rowNum = 0` → :93 `while (rs.next())` → :94 `results.add(this.rowMapper.mapRow(rs, rowNum++))` → 返回 `List<T>` — 遍历与行号管理都在这里

关键设计: 遍历 ResultSet 的"循环"只写一次(在 RowMapperResultSetExtractor), 变化的是"一行怎么映射"(RowMapper) — 策略模式。ResultSetExtractor 则把整个循环交给开发者: 可返回 Map/Set/单对象, 支持 GROUP BY 聚合(一行一行转对象无法表达"组")。

数据流: query("SELECT dept, name FROM emp", (rs) -> { Map<String,List<String>> m = new HashMap<>(); while(rs.next()) m.computeIfAbsent(rs.getString("dept"), k->new ArrayList<>()).add(rs.getString("name")); return m; }) → JdbcTemplate.query(sql, rse)(:449)→execute(:476)→L388 getConnection→stmt.executeQuery→ResultSet rs→rse.extractData(rs)(extractor 自己 while 循环)→dept="IT"→list=[Alice,Bob]→"HR"→list=[Charlie]→finally close rs→return Map<dept, employees> 而 RowMapper 版本: query(sql, rowMapper)(:485)→new RowMapperResultSetExtractor(rowMapper)→extractData(:90)→JdbcTemplate 内部 while(rs.next())→mapRow(rs, 0)→mapRow(rs, 1)→...→List<User>

### 2. BeanPropertyRowMapper — 列名↔属性名自动映射

场景: `query("SELECT user_id, user_name FROM users", new BeanPropertyRowMapper<>(User.class))` — 不写任何 lambda, 数据库列自动填进 User 的 userId/userName 字段。

源码路径:
- `BeanPropertyRowMapper.java:92` — **类声明**: 实现 RowMapper<T>
- `BeanPropertyRowMapper.java:249` — **initialize()**: 反射 User 的属性→`mappedNames.add(underscoreName(pd.getName()))`(:293)—把属性名转成下划线形式预建匹配表
- `BeanPropertyRowMapper.java:317` — **underscoreName()**: `convertPropertyNameToUnderscoreName(name)` — "userId"→"user_id"
- `BeanPropertyRowMapper.java:328` — **mapRow()**: :335 `ResultSetMetaData rsmd = rs.getMetaData()`(列名从元数据取, 不依赖列顺序)→:336 columnCount→:338 循环→:340 `JdbcUtils.lookupColumnName(rsmd, index)`→:341 `lowerCaseName(StringUtils.delete(column, " "))`→:342 用该名字查 mappedProperties→命中→getColumnValue(rs, index, pd)→BeanWrapperImpl 调 setter

关键设计: 映射方向是"列名→下划线化属性名": 列名只做小写/去空格, 属性名被转成下划线形式, 两者按名字相等匹配(如 "user_id"=="user_id")。所以 user_id↔userId 的转换发生在属性侧(underscoreName), 而非把列名 camelCase 化。

数据流: jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class)) → JdbcTemplate.query(sql, rowMapper)(:485)→RowMapperResultSetExtractor.extractData(:90)→while(rs.next())→BeanPropertyRowMapper.mapRow(rs, 0)(:328)→rsmd.getColumnCount()→lookupColumnName→"user_id"→mappedProperties.get("user_id")→命中 User.userId 的 PropertyDescriptor→getColumnValue→User.setUserId(rs.getLong("user_id"))→"user_name"→User.setUserName→返回 User→List<User> 完成

### 3. NamedParameterJdbcTemplate — :name 占位符替换为 ?

场景: SQL 有 8 个参数时 `?` 的顺序极易错位 — `:name` 命名参数让参数与值按名对应。

源码路径:
- `NamedParameterJdbcTemplate.java:79` — **类声明**: 内部持有 classicJdbcTemplate, 所有操作先"翻译"再委托 — query(String, Map, RowMapper)(L222): `query(sql, new MapSqlParameterSource(paramMap), rowMapper)`
- `NamedParameterJdbcTemplate.java:341` — **update(String, Map)**: 同样转 SqlParameterSource→:336 update(sql, paramSource)→`getJdbcOperations().update(getPreparedStatementCreator(sql, paramSource))`
- `NamedParameterJdbcTemplate.java:464` — **getPreparedStatementCreator()**: :482 getParsedSql(sql)(LRU 缓存, :89/:140, 上限 256)→NamedParameterUtils.buildValueArray(parsedSql, paramSource, null)(:350)→PreparedStatementCreatorFactory.newPreparedStatementCreator(params)(PreparedStatementCreatorFactory.java:174)
- `NamedParameterUtils.java:82` — **parseSqlStatement()**: 扫描字符数组, 跳过注释/引号/转义, 收集每个 `:name` 的起始/结束索引→ParsedSql(ParsedSql.java:29, parameterNames 列表 :33)
- `NamedParameterUtils.java:286` — **substituteNamedParameters()**: 按参数索引把 `:name` 替换为 `?`, 参数值按顺序写入 `?`

关键设计: ParsedSql 是"结构"与"值"分离的中间产物 — SQL 只解析一次(缓存), 每次执行只绑定不同参数值; `?` 的位置顺序由解析时的出现顺序决定, 与 Map 的键一一对应。

数据流: npjt.update("INSERT INTO users VALUES(:id,:name)", Map.of("id",1,"name","Alice")) → update(String, Map)(:341)→MapSqlParameterSource→update(SqlParameterSource)(:336)→getPreparedStatementCreator(:464)→getParsedSql→NamedParameterUtils.parseSqlStatement(:82)→ParsedSql{paramNames=[id,name], 索引[(9,12),(14,19)]}→substituteNamedParameters(:286)→"INSERT INTO users VALUES(?,?)"→buildValueArray(:350)→[1,"Alice"]→PreparedStatementCreatorFactory.newPreparedStatementCreator([1,"Alice"])(:174)→JdbcTemplate.update→ps.setObject(1,1); ps.setObject(2,"Alice")→executeUpdate

→ spring-jdbc 第二域完成。RowMapper/ResultSetExtractor + BeanPropertyRowMapper + NamedParameter。引出 J-3 🟡: JdbcTemplate batch/transaction callback。
