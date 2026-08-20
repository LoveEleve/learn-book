# D-3 StatFilter 监控 — 模板钩子埋数 + SQL 参数化 + 慢 SQL

> 前置: [[D-2-filter-chain]] (模板钩子) | 复用: [[D-6-sql-parser]] (参数化) | 对照: [[H-9-metrics]] | 引出: [[D-4-wallfilter]]
> 🔴 Deep | 8 KP | [模式: 模板方法 + 参数化键 + 观察者]
> Pass 2 闭环: q1(模板 vs override) q2(挂载) q3(参数化聚合) q4(慢 SQL 快照) q5(三级聚合) q6(计数原子化) q7(双直方图分离)

**读者处境**: 监控页"SQL 监控"能把一万条 `WHERE id=123` 归成一条统计, 还能标红 3 秒慢 SQL — 数字从哪来?StatFilter 在链上做了什么而不破坏链?为什么它不直接覆写 statement_execute?这篇拆开埋数钩子、统计对象和慢 SQL 检测。

### 1. 埋数钩子 — 观察者不碰链

场景: StatFilter 要统计每次执行, 但链是递归下推的 — 它怎么"卡"在执行前后?

源码路径:
- `StatFilter.java:378,383,388,407` — **模板钩子**: `statementExecuteBefore/After`(L378/383)+`statementExecuteBatchBefore/After`(L388/407) — override FilterEventAdapter 的 protected 钩子(D-2 §4 风格 B)
- `StatFilter.java:536` — **错误钩子**: `statement_executeErrorAfter`(L536) — 异常分支埋数
- `StatFilter.java:232,615` — **建连与读行**: `connection_connect`(L232, 建连计时)+`resultSetOpenAfter`(L615)

关键设计: **Why 模板钩子而非 override？**(闭环 q1): 统计只要"执行前后的时机"不要"拦截能力" — 覆写钩子最干净, 链的下推由 FilterEventAdapter 模板完成, 统计天然不破坏链语义(对照 WallFilter 必须 override 抢执行前, D-4)。[模式: 模板方法 + 观察者]

数据流: stmt.execute(链) → FilterEventAdapter.statement_execute 模板(D-2) → statementExecuteBefore(L378) → chain 下推执行 → 返回 → statementExecuteAfter(L383); 异常 → statement_executeErrorAfter(L536)。

### 2. 统计对象与执行指标 — JdbcSqlStat 挂载

场景: 统计存在哪?一条 SQL 的耗时/并发/更新行数怎么算?

源码路径:
- `StatFilter.java:429,431,432` — **挂载**: `statement.getSqlStat()` 为 null/removed/SQL 不符 → `createSqlStat`(L431)+`statement.setSqlStat(sqlStat)`(L432) — JdbcSqlStat 挂在 StatementProxy 上
- `StatFilter.java:448,450,452` — **并发计数**: `incrementRunningCount`(L450)+事务内计数(L452-454)
- `StatFilter.java:468,471,482` — **耗时**: `internalAfterStatementExecute`(L468): `nanos = nowNano - getLastExecuteStartNano`(L471)→`addExecuteTime(lastExecuteType, firstResult, nanos)`(L482) — executeType 决定口径
- `StatFilter.java:484,490,496` — **更新行数**: **非首结果集且 Execute 类型** → `getUpdateCount`(L486-490, 单条更新); 否则(firstResult 或非 Execute) → updateCountArray 批量累加(L491-497)

关键设计: **Why 挂载到 StatementProxy？**(闭环 q2): 同一物理 statement 循环执行多次, sqlStat 挂上去**一次创建, 重复记账** — 避免每次执行都重新参数化+查表; 挂载点由 D-2 的 proxy 对象模型提供(StatementProxyImpl.getSqlStat)。[模式: 缓存挂载]

数据流: 首次执行 → createSqlStat(677) → mergeSql 参数化 → dataSourceStat.createSqlStat → 挂到 statement(432) → 每次执行: Before 段 incrementRunningCount(450) → After 段 addExecuteTime(482)+addUpdateCount(487) → decrementRunningCount(481)。

### 3. 慢 SQL — 阈值与参数快照

场景: "慢 SQL"怎么判定?日志里带的参数哪来的?为什么参数要 JSON 化?

源码路径:
- `StatFilter.java:71,51` — **阈值**: `slowSqlMillis = 3000`(L71)+`SYS_PROP_SLOW_SQL_MILLIS`(L51, 系统属性覆盖)
- `StatFilter.java:499,500` — **判定**: `millis >= slowSqlMillis`(L499-500)
- `StatFilter.java:562` — **快照**: `buildSlowParameters`(L562): 遍历 `statement.getParameter(i)` → JdbcParameter.getValue → JSONWriter 序列化(超 100 字符截断+流类型占位 `<InputStream>`)
- `StatFilter.java:505,520,532` — **输出**: 分级日志(WARN/INFO/DEBUG/ERROR, L505-520)+`handleSlowSql`(L522/532, 扩展点)

关键设计: **Why JSON 快照？**(闭环 q4): 慢 SQL 定位需要"当时参数", 但参数可能巨大(大文本/流) — JSON 序列化+100 字符截断+类型占位, **可读与安全兼顾**(不把 InputStream 内容打日志)。[模式: 快照]

数据流: 执行完成 → millis>=slowSqlMillis(500) → buildSlowParameters(501) → setLastSlowParameters(502) → logSlowSql: 分级日志(505) → handleSlowSql(522)。

### 4. 聚合与消费 — 参数化键 + 三级统计

场景: 监控页数据结构什么样?参数化 SQL 为什么必须?

源码路径:
- `StatFilter.java:677,693` — **参数化**: `createSqlStat`(L677): JdbcStatContext 上下文 SQL 优先; 否则 `mergeSql(sql, dbType)`(L693)→`dataSourceStat.createSqlStat(参数化 SQL 作 key)`
- `stat/JdbcDataSourceStat.java:38` — **一级**: DataSource 级(连接/语句/结果集汇总+`getRuningSqlList` 供借出超时异常 DruidDataSource:1746)
- `stat/JdbcSqlStat.java:33` — **二级**: 单 SQL 统计(**30+ 字段全 AtomicFieldUpdater 原子更新** L40-142, 闭环 q6; **8 档幂次直方图** L127-134; **执行/执行+结果持有双直方图分离** L702-707: 非 ExecuteQuery 且非首结果集时执行耗时并入后者 — 查询类持有时间走 ResultSet 生命周期单独记, 闭环 q7) — 参数化 SQL 为 key
- `stat/TableStat.java:30` — **三级**: 表级统计
- `StatFilter.java:708` — **直方图**: `dataSource_releaseConnection`(L708): `connectionHoldHistogram.record(millis)`

关键设计: **Why 参数化聚合？**(闭环 q3): 原始 SQL 每参数不同会**撑爆统计表**(WHERE id=1/id=2/...) — mergeSql 把 `id=123` 归一成 `id=?`(D-6 内核), 统计粒度从"语句实例"升到"语句形态"。**Why 三级？**(闭环 q5): DataSource(池健康)→SQL(哪条慢)→Table(哪张表被写) 三粒度回答三问题。[模式: 参数化键 + 聚合层次]

数据流: 借出超时异常(D-1 L1746) → getRuningSqlList 附运行中 SQL → 监控页/MBean/DruidStatService(只读) → 表级: SchemaStatVisitor 统计(导航 D-6) → TableStat 汇总。

→ 引出 D-4: WallFilter 防火墙 — 链上第二个拦截器 (直接 override 风格, AST 级规则检查)。
