# C-19 @Sql — 测试数据初始化 (声明 → 监听器 → ScriptUtils 执行)

> 依赖 C-17 TestContext | 🟡 Working | 6 KP | [模式: 观察者 + 模板 + 声明式]

**读者处境**: `@Sql("/schema.sql")` 在测试方法前建表/插数据 — 谁执行？什么时候执行？脚本怎么解析？和 @Transactional 回滚什么关系？

### 1. @Sql 声明模型 — scripts / statements / phase / merge

场景: 测试方法前要建表+插基础数据, 方法后清数据 — @Sql 用脚本或内联 SQL 声明, 指定执行阶段(方法前/后、类级)。

源码路径:
- `Sql.java:88,141,155` — **注解**: scripts(脚本文件路径 L141) / statements(内联 SQL 字符串 L155) — 可标在测试类或方法上
- `Sql.java:159` — **phase**: 默认 ExecutionPhase.BEFORE_TEST_METHOD — 可选 AFTER_TEST_METHOD / BEFORE_TEST_CLASS / AFTER_TEST_CLASS
- SqlMergeMode: 类级+方法级 @Sql 合并规则(默认方法覆盖类, 可配叠加)

关键设计: **Why 脚本+内联两种？** 脚本(scripts)适合多条/可复用 SQL(建表/初始化); 内联(statements)适合少量临时 SQL — 灵活应对"建库脚本复用"与"单测临时数据"; phase 让建表(BEFORE_CLASS)与清理(AFTER_METHOD)分阶段。[模式: 声明式数据准备]

数据流: `@Sql(scripts="/test-schema.sql", phase=BEFORE_TEST_METHOD)` 标方法 → 框架记录该 @Sql 配置(脚本+阶段) → 供监听器执行。

### 2. SqlScriptsTestExecutionListener — 执行时机

场景: @Sql 什么时候真正跑？答案: 作为 TestContext 的 TestExecutionListener(C-17 机制), 在测试方法前/后钩子触发。

源码路径:
- `SqlScriptsTestExecutionListener.java:118,174` — **监听器**: extends AbstractTestExecutionListener — beforeTestMethod L174: `executeSqlScripts(testContext, ExecutionPhase.BEFORE_TEST_METHOD)`; afterTestMethod L183: 对称执行 AFTER
- `executeSqlScripts`: 从 TestContext 收集 @Sql(类级+方法级, 依 mergeMode) → 匹配当前 phase → 取 DataSource(TestContextTransactionUtils.retrieveDataSource) → 逐个执行
- 与 TestContext 关联: 它是 TestContextManager 分发链上的监听器之一(C-17 §1) — 与依赖注入/事务监听器并列

关键设计: **Why 用 TestExecutionListener 而非 JUnit 扩展？** 复用 TestContext 框架: 能访问已加载的 DataSource/事务管理器, 并参与同一生命周期(与 @Transactional 监听器顺序配合); 抽象于具体测试框架(JUnit4/5 通用)。[模式: 观察者 — 挂生命周期]

数据流: 测试方法执行前 → TestContextManager.beforeTestMethod → 遍历监听器 → SqlScriptsTestExecutionListener.beforeTestMethod(L174) → executeSqlScripts(BEFORE): 收集 @Sql scripts → retrieveDataSource → 对每个脚本 ScriptUtils.executeSqlScript(connection, resource) → 建表/插数据。方法后 afterTestMethod(L183) → 执行 AFTER 脚本(清理)。

### 3. ScriptUtils 脚本执行 + 事务模式

场景: 脚本怎么解析执行？`--`注释、`;`分隔、出错怎么办？数据要不要回滚？

源码路径:
- `ScriptUtils.java:55,125` — **executeSqlScript(connection, resource) L125**: 读脚本 → 解析(去注释/按语句分隔符切分, 支持 `;;`/块) → 连接上逐个 Statement 执行 → 出错抛 ScriptException
- `SqlConfig.java` — **transactionMode**: `DEFAULT`(默认, 解析为 INFERRED)/`INFERRED`(有事务管理器→在现有 @Transactional 事务内执行, 无则直接对数据源执行)/`ISOLATED`(独立新事务)
- 与 @Transactional: 默认 INFERRED 语义 → 若测试方法 @Transactional → @Sql 在事务内执行, 随事务回滚(测试数据不落库)

关键设计: **Why 解析出分隔符再执行？** 脚本是多条 SQL 文本 — 必须按分隔符切分成单条 Statement, 且要跳过注释(避免把注释当 SQL); 分隔符可配置(支持存储过程 `;;`)。**Why transactionMode？** 控制"测试数据是否随事务回滚" — 默认跟随测试事务, 保证测试隔离、不污染库。[模式: 模板 — 脚本解析执行]

数据流: executeSqlScript(conn, "/test-schema.sql") → readScript → 解析: 去 `--`/`/* */` 注释 → 按 `;` 切 → [CREATE TABLE..., INSERT...] → 逐条 conn.createStatement().execute → 成功。事务模式 INFERRED + 测试 @Transactional → 全部在事务内, 测试结束回滚 → 库无残留。

→ 引出 9-A: WebSocket — spring-test 层收尾, 回到 Web: @EnableWebSocket/WebSocketHandler/HandshakeInterceptor 实时双向通信。
