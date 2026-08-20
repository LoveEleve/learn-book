# T-4 DataAccessException 异常翻译 — 四层回退链 + Error Code 映射

> 依赖 T-1 链路 + S2-15 @Repository | 🟡 Working | 1 KP | [模式: 责任链]

**读者处境**: @Repository 自动翻译 JPA/Hibernate 异常 — MySQL 的 `Duplicate entry`(error 1062) → Spring 的 DuplicateKeyException — 这个翻译是怎么工作的？不同数据库的同一语义错误怎么映射到相同异常？

### 1. SQLExceptionTranslator — 四层回退链的异常翻译

场景: JdbcTemplate.execute() 内部 catch SQLException → `translator.translate("PreparedStatement.execute", sql, ex)` → custom translator(用户自定义)→SQLErrorCodeSQLExceptionTranslator.doTranslate(error code映射)→失败→单步 fallback: SQLExceptionSubclassTranslator(JDBC异常子类)→失败→SQLStateSQLExceptionTranslator(SQLState)→全部失败→JdbcTemplate.translateException 兜底 new UncategorizedSQLException(task, sql, ex)。

源码路径:
- `SQLExceptionTranslator.java:56` — **translate(task, sql, ex)**: 接口 — 接收 SQL + SQLException → 返回 DataAccessException 子类
- `AbstractFallbackSQLExceptionTranslator.java:95` — **translate()回退模板**: L96 Assert.notNull(ex) → L98-99 getCustomTranslator()!=null→custom.translate→非null返回 / null→L107 doTranslate(task,sql,ex)(子类实现: error code映射)→非null返回 / null→L114 getFallbackTranslator()→L116 fallback.translate(仅此单步) / fallback==null→L119 return null(由调用方兜底)
- `SQLErrorCodeSQLExceptionTranslator.java:185` — **doTranslate()**(类声明在 :75): 从 `sql-error-codes.xml`(SQLErrorCodes)读取数据库 error code 映射→L230 customTranslations 数组匹配(命中→L235 createCustomException)→L252-254 grouped codes 匹配(如 duplicateKeyCodes 含 1062→new DuplicateKeyException)→全部未命中→L299 return null→fallback 链:SQLErrorCodeSQLExceptionTranslator 构造时(:96)setFallbackTranslator(new SQLExceptionSubclassTranslator())、SQLExceptionSubclassTranslator(:70)再 setFallbackTranslator(new SQLStateSQLExceptionTranslator())

数据流: JdbcTemplate.execute(sql) → catch java.sql.SQLIntegrityConstraintViolationException(errorCode=1062)→translateException("StatementCallback", sql, ex)(JdbcTemplate.java:1555-1557)→translator.translate("execute", sql, ex)→SQLErrorCodeSQLExceptionTranslator.doTranslate(L185)→errorCode=Integer.toString(1062)(L225)→sql-error-codes.xml 的 MySQL 条目: <property name="duplicateKeyCodes"><value>1062</value></property>(sql-error-codes.xml:199-201)→L252 Arrays.binarySearch(sqlErrorCodes.getDuplicateKeyCodes(), "1062")>=0→L254 new DuplicateKeyException(buildMessage(...), ex)→return→若整条链都未匹配→fallback 链也返回 null→L1557 new UncategorizedSQLException(task, sql, ex)→应用层 catch DuplicateKeyException→处理重复键

**关键**: sql-error-codes.xml 为 MySQL/PostgreSQL/Oracle/H2/HSQL/DB2/Sybase 等数据库提供 error code → DataAccessException 映射表 → 保证同一语义跨数据库一致性。

关键设计: **Why 默认翻译器是 SQLExceptionSubclassTranslator 而非 SQLErrorCode 版？** JdbcAccessor 的逻辑(L92-96): 仅当用户**显式提供** `sql-error-codes.xml`(hasUserProvidedErrorCodesFile)时才用 SQLErrorCodeSQLExceptionTranslator — 否则默认 `new SQLExceptionSubclassTranslator()`。因为新版 JDBC 驱动(JDBC 4+)已把常见错误映射为 SQLException 的**子类**(如 SQLIntegrityConstraintViolationException)— 子类翻译器直接按类型匹配(不需要数据库-specific 映射表) — 对大多数应用"零配置"即够。error-code 映射表适合需要精确到数据库错误码的精细化场景。

**四层回退链的精确定义**: ①custom translator(用户 setExceptionTranslator 覆盖) ②doTranslate 本身(SQLErrorCode 版读 error code 表) ③fallback 链(SQLErrorCode→SQLExceptionSubclassTranslator→SQLStateSQLExceptionTranslator — 每级 translate 内部又是"custom→doTranslate→fallback"的递归模板) ④JdbcTemplate.translateException 的 UncategorizedSQLException 兜底(L1557)。注意: 默认 JdbcAccessor 翻译器是 Subclass 版时, 它的 fallback 是 SQLState 版 — 两级即可完成大部分翻译。

**为什么包装成 DataAccessException？** 业务代码不需要分别 catch SQLException/DataAccessException 的每种子类 — 统一基类让业务层可以 `catch (DataAccessException e)` 一次处理所有持久化异常; 且 DataAccessException 是 RuntimeException — 不强制业务代码声明 throws — 与 Spring 的"非强制检查异常"哲学一致。

**翻译发生在哪里？** JdbcTemplate 的每个执行入口(execute L354/query L408/update L677)都包着 catch SQLException → translateException — 即**所有**通过 JdbcTemplate 执行的 SQL 都自动经过翻译链。直接使用 JDBC Connection(绕过 JdbcTemplate)则不会翻译 — 这就是为什么 @Repository + JdbcTemplate 的组合能获得统一异常、而裸 JDBC 代码不能。

**如何自定义翻译器？** 三种途径: ①全局: `setExceptionTranslator(translator)` 覆写 JdbcAccessor 的默认翻译器 ②局部: `JdbcTemplate.setExceptionTranslator(...)` 只影响单个实例 ③数据源级: 提供自定义 `sql-error-codes.xml` 到 classpath 根(hasUserProvidedErrorCodesFile 检测到→改用 SQLErrorCode 版 + 自定义映射)。自定义 translator 走 AbstractFallbackSQLExceptionTranslator 的 L98-99 custom 分支 — 优先于一切内置翻译。

**回退链的递归模板性质**: AbstractFallbackSQLExceptionTranslator.translate 是模板方法 — 每个翻译器(custom/error-code/subclass/SQLState)的 translate 内部都执行同一骨架: custom→doTranslate→fallback。因此 SQLErrorCode 版失败后 fallback 到 Subclass 版时 — Subclass 版内部又跑一遍自己的 custom→doTranslate→fallback — 形成"链中链"。这保证了任何一级的 custom translator 都能在任何深度拦截。实际应用中绝大多数翻译在第一级(JDBC 子类或 error-code)即完成 — 链的存在是为了"永不放弃翻译"的兜底承诺。

**常见的翻译结果映射**: MySQL 条目的 codes 分组(sql-error-codes.xml:189 起): badSqlGrammarCodes=1054,1064,1146(语法错误→BadSqlGrammarException); duplicateKeyCodes=1062(重复键→DuplicateKeyException); dataIntegrityViolationCodes=630,839,840,893,1169,1215,1216,1217,1364,1451,1452,1557(完整性违反→DataIntegrityViolationException)。连接类异常(SQLTransientConnectionException, SQLExceptionSubclassTranslator.java:82)由子类翻译器按 JDBC 类型匹配→DataAccessResourceFailureException。

这些映射集中在 sql-error-codes.xml 的 MySQL 条目 — 不同数据库的错误码不同但语义相同 — 翻译链保证业务层看到的是**统一语义的 Spring 异常**而非数据库特有错误码。

→ spring-tx 第四域完成。四层回退链(error code/SQLState/subclass/custom)—DataAccessException映射体系。引出 T-5: TransactionSynchronization — afterCommit/afterCompletion 回调 + TransactionSynchronizationManager。
