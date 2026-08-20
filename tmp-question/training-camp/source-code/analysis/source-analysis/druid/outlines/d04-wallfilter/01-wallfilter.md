# D-4 WallFilter 防火墙 — 前置检查 + AST 规则 + 方言 Provider

> 前置: [[D-2-filter-chain]] (链+override 风格) | 复用: [[D-6-sql-parser]] (AST+Visitor) | 对照: [[D-3-statfilter]] (前置 vs 后置) | 引出: [[D-9-boot3]]
> 🔴 Deep | 8 KP | [模式: 前置守卫 + 双路径 + 策略]
> Pass 2 闭环: q1(前置守卫) q2(双路径) q3(规则配置化) q4(黑名单回填) q5(参数化 key)

**读者处境**: `WHERE id=1 OR 1=1` 注入会被 Druid 拦下 — 它怎么"看懂"SQL 里藏着攻击?每次执行都解析 AST 会不会太慢?为什么拦截规则全是配置?这篇拆开 WallFilter 的前置检查、白名单快路径和规则引擎。

### 1. 拦截风格与 Provider 装配 — execute 前裁决

场景: 防火墙必须"先斩后奏" — 它怎么抢在 SQL 执行前面?

源码路径:
- `WallFilter.java:480,483,485,489` — **前置守卫**: `statement_execute(chain, statement, sql)`(L480): `createWallContext`(L483)→`sql = check(sql)`(L485, 拦截点)→放行才 `chain.statement_execute`(L489)→后置统计(L491/494)
- `WallFilter.java:110,121,146` — **装配**: `init()`(L110)→`initWallProvider`(L121): SPI `WallProviderCreator`(L199); 无则按 dbType: MySql(L146)/Oracle(L157)/SQLServer(L165)/PG(L176)/DB2(L183)/SQLite(L190)/CK(L196)

关键设计: **Why 必须 override？**(闭环 q1): 模板钩子(StatFilter 风格)拿不到"拒绝执行"的能力 — 防火墙必须在 `chain.statement_execute` 之前决定放行还是抛 `WallSQLException`; override 抢占调用点(D-2 §4 风格 A), 与 StatFilter 的"后置埋数"形成对照。[模式: 前置守卫]

数据流: stmt.execute → WallFilter.statement_execute(480) → createWallContext(483) → check(sql)(485) → 违规: 抛异常拦截; 放行: chain.statement_execute(489) 下推 → 统计(491/494)。

### 2. 检查链路 — 白名单快路径 vs AST 硬检查

场景: 每次都解析 AST 很贵 — 什么情况能跳过?

源码路径:
- `WallProvider.java:441,454,459` — **入口**: `check(sql)`(L441)→`checkInternal`(L454): `doPrivilegedAllow && isPrivileged` 特权直放(L459-463)
- `WallProvider.java:58,67,352,355` — **白名单快路径**: **`ConcurrentLruCache<String, WallSqlStat>`(1024 上限, L67)+参数化 SQL 作 key**(L352-355, getMergedSqlNullableIfParameterizeError) — `WHERE id=123` 与 `id=456` 命中同一条目(闭环 q2/q5); 命中直接返回绕过解析(L357-361)
- `WallProvider.java:481,494,517` — **硬检查**: `createParser(sql)`(L481)→Lexer 配置(注释禁 L484-486)→`parseStatementList`(L494)→多语句禁(L517)
- `WallProvider.java:521,533,540` — **规则遍历**: `createWallVisitor`(L521)→`stmt.accept(visitor)`(L533, D-6)→`visitor.getViolations()` 并入(L540)

关键设计: **Why 双路径？**(闭环 q2): 白名单命中(同 SQL 已判定合法)直接复用历史结论 — 一次 LRU 查找替代完整 AST 解析; 攻击 SQL 被拦截一次后同形不再消耗解析。**Why 解析在锁外？** 解析是重活, 不占池锁, 违规判定后同步抛异常回调用线程。[模式: 双路径 — 快路径+硬路径]

数据流: check(441) → 特权?直放(459) → 白名单命中?返回(352-361) → 硬检查: createParser(481) → parseStatementList(494) → 多语句?违规(517) → createWallVisitor(521) → accept 遍历(533) → violations 汇总(540)。

### 3. 规则集与方言 — WallConfig + Provider/Visitor 族

场景: `multiStatementAllow: false` 这类规则在哪定义?不同库规则一样吗?

源码路径:
- `WallConfig.java:45,75,79,80,126` — **规则集**: `hintAllow = true`(L45)/`multiStatementAllow`(L75, 默认禁)/`commentAllow`(L79, 默认禁)/`strictSyntaxCheck = true`(L80)/`completeInsertValuesCheck`+`insertValuesCheckSize = 3`(L126-127)
- `wall/spi/`(14 文件) — **方言族**: `MySqlWallProvider/MySqlWallVisitor`/Oracle/PG/DB2/SQLServer/SQLite/CK — 每库 Provider(缓存/统计)+Visitor(规则实现)
- `wall/violation/` — **违规记录**: `ErrorCode` 枚举+`IllegalSQLObjectViolation`(错误码+SQL 片段)+`SyntaxErrorViolation`

关键设计: **Why 规则全进配置？**(闭环 q3): 安全策略因业务而异(有的业务允许注释 SQL) — WallConfig 全可配, `configFromProperties` 运行时生效, 策略与代码分离。[模式: 策略配置化]

数据流: 配置 `wall: {config: {multiStatementAllow: false}}` → init 时 configFromProperties(72) → 检查时: 注释出现→commentAllow=false 违规(502-504)/多语句→违规(517)/Visitor 规则(表/函数/条件)→违规(540)。

### 4. 违规闭环 — 黑名单回填与统计

场景: 被拦的攻击去哪了?为什么同一条攻击第二次更快?

源码路径:
- `WallProvider.java:572,587` — **回填**: 违规 → `addBlackSql(...)`(L572) 入黑名单库; 放行 → `addWhiteSql`(L587) 入白名单库 — **下次同形(参数化 key)直接快路径命中**
- `WallProvider.java:455,475,556` — **统计**: `checkCount`(L455)/`hardCheckCount`(L475)/`violationCount++`(L556)/`whiteListHitCount`(L634) — 监控页"防火墙"数据源
- `WallFilter.java:493,494` — **传播**: `catch (SQLException ex)`(L493): `incrementExecuteErrorCount`(L494)+throw(L495) — 违规以 SQLException 抛业务

关键设计: **Why 黑名单回填？**(闭环 q4): 攻击 SQL 通常被重复尝试 — 拦截一次后入黑名单, 二次命中走 Map 查找不再解析; 白名单同理 — **判定缓存化**把高频检查降为 O(1)。[模式: 判定缓存]

数据流: 违规判定 → addBlackSql(572) → violationCount++(556) → 抛 WallSQLException(经 497) → 业务收 SQLException → 同 SQL 再来: 黑名单命中直接拦截。

→ 引出 D-9: Boot3 Starter — WallFilter 经 DruidFilterConfiguration 条件注册 (spring.datasource.druid.filter.wall.enabled), autoAddFilters 注入链。
