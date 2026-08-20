# D-3 StatFilter 监控 — 模板钩子埋数 + SQL 参数化 + 慢 SQL

> 项目: Druid (JDBC 连接池) | 🔴 Deep / 1 篇 | StatFilter(1135)+JdbcDataSourceStat(504)+JdbcSqlStat(1074)+TableStat(642)+ParameterizedOutputVisitorUtils
> 基线: DRUID-PLAN D-3 (监控) — 前置: **D-2(链+模板钩子, 已分析) + D-6(参数化, 已分析)** — 展开埋数机制; 对照 Hikari H-9(指标桥, 无 SQL 级统计)

---

## §0.8

- 🔴 Deep，1篇 — 埋数钩子(**FilterEventAdapter 模板钩子族: statementExecuteBefore[L378]/After[L383]/BatchBefore[L388]/BatchAfter[L407]/statement_executeErrorAfter[L536] + connection_connect[L232] 建连计时 + resultSetOpenAfter[L615] 读行**) → JdbcSqlStat 生命周期(**懒创建+挂载 StatementProxy: getSqlStat==null||removed||sql 不符 → createSqlStat(L677: mergeSql 参数化→dataSourceStat.createSqlStat) + statement.setSqlStat[L429-433]**) → 执行统计(**internalBefore[L412]: beforeExecute+setLastExecuteStartNano+incrementRunningCount+inTransactionCount[L448-455]; internalAfter[L468]: addExecuteTime(lastExecuteType,firstResult,nanos)[L482]+updateCount 双分支[L484-497]**) → 慢 SQL(**millis>=slowSqlMillis(默认 3000[L71], SYS_PROP druid.stat.slowSqlMillis[L51])→buildSlowParameters JSON 快照[L562]→分级日志[L505-520]→handleSlowSql[L532]**) → 聚合与消费(**三级: JdbcDataSourceStat→JdbcSqlStat→TableStat; getRuningSqlList 供借出超时异常[DruidDataSource L1746]; connectionHoldHistogram 归还持有时间[L708]; DruidStatService/MBean 只读暴露一句话**)
- 设计模式: [模式: 模板方法]—Before/After 钩子; [模式: 参数化键]—mergeSql 聚合; [模式: 观察者]—StatFilterContext 事件

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| StatFilter.java:378,383,388,407 | 钩子族 | **statementExecuteBefore(L378)/After(L383)/BatchBefore(L388)/BatchAfter(L407)** — FilterEventAdapter 模板钩子 override, 不碰 chain | High |
| StatFilter.java:412,419 | 埋前 | **internalBeforeStatementExecute(L412): dataSourceStat.getStatementStat().beforeExecute(L414)+setLastExecuteStartNano(L419)** | High |
| StatFilter.java:429,431,432 | sqlStat 挂载 | **getSqlStat 为 null/removed/sql 不符 → createSqlStat(statement, sql)(L431)+statement.setSqlStat(L432)** | High |
| StatFilter.java:677,693 | 创建 | **createSqlStat(L677): JdbcStatContext 上下文 SQL 优先; 否则 mergeSql(sql, dbType)(L693)→dataSourceStat.createSqlStat(参数化 SQL 作 key)** | High |
| StatFilter.java:448,450,452 | 运行计数 | **sqlStat.incrementRunningCount(L450)+inTransaction 时 incrementInTransactionCount(L452-454)** | High |
| StatFilter.java:468,482,484 | 耗时统计 | **internalAfterStatementExecute(L468): nanos=now-lastExecuteStartNano(L471)→statementStat.afterExecute(L474)→addExecuteTime(lastExecuteType, firstResult, nanos)(L482)→updateCount: Execute 且 firstResult→getUpdateCount(L484-490)/数组(491-497)** | High |
| StatFilter.java:499,501,505 | 慢 SQL | **millis>=slowSqlMillis(L500)→buildSlowParameters(L501)→setLastSlowParameters(L502)→logSlowSql 分级日志(L505-520)→handleSlowSql(L522)** | High |
| StatFilter.java:562 | 参数快照 | **buildSlowParameters(L562): statement.getParameter(i)→JdbcParameter.getValue→JSONWriter 序列化(超 100 字符截断)** | High |
| StatFilter.java:232,708,615 | 连接统计 | **connection_connect(L232): 建连计时; dataSource_releaseConnection(L708): connectionHoldHistogram.record(持有毫秒); resultSetOpenAfter(L615): ResultSet 打开统计** | High |
| stat/JdbcSqlStat.java:33 | 统计体 | **JdbcSqlStat(L33): SQL 统计容器(SQL 文本+耗时+直方图+读/更新行数), dataSourceStat 下按 SQL 键聚合** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: StatFilter 是链上监控的完整机制 — 1篇 (~60行) 按"钩子与挂载→执行统计→慢 SQL→聚合消费"展开; 参数化内核引用 D-6(已分析), 超时异常消费引用 D-1(已分析)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 模板钩子族埋数 (Before/After/Batch/Error) | 🔴 | **为什么🔴**: 埋数入口 |
| P1-2 | JdbcSqlStat 挂载与懒创建 | 🔴 | **为什么🔴**: 统计对象生命周期 |
| P1-3 | 执行耗时与 updateCount 统计 | 🔴 | **为什么🔴**: 核心指标 |
| P1-4 | 慢 SQL 检测 (阈值+参数快照+日志) | 🔴 | **为什么🔴**: 慢查询定位 |
| P1-5 | 参数化聚合 (mergeSql→统计 key) | 🔴 | **为什么🔴**: SQL 合并 |
| P2-1 | 三级统计结构 (DataSource→SQL→Table) | 🟡 | **为什么🟡**: 聚合维度 |
| P2-2 | 连接/结果集/持有时间统计 | 🟡 | **为什么🟡**: 辅助指标 |
| P3-1 | 与 Hikari H-9 对照 (SQL 级 vs 池级) | 🟢 | **为什么🟢**: 监控粒度对比 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **钩子与挂载** | 🔴 | 入口 |
| B | **执行统计** | 🔴 | 核心 |
| C | **慢 SQL+参数化** | 🔴 | 价值点 |
| D | **聚合与消费** | 🟡 | 展示 |

> **Cluster A (§1)**: 模板钩子族+connection_connect+resultSetOpenAfter
> **Cluster B (§2)**: JdbcSqlStat 生命周期+耗时/updateCount/运行计数
> **Cluster C (§3)**: 慢 SQL 判定+参数快照+日志分级
> **Cluster D (§4)**: 三级聚合+getRuningSqlList+直方图+MBean 暴露

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 模板钩子 vs override | 统计只要时机不要拦截 → 覆写 EventAdapter 钩子(Before/After)最干净, 天然不破坏链语义 | StatFilter.java:378-407, FilterEventAdapter.java:178-198 |
| q2 | JdbcSqlStat 挂载 | 同一物理 statement 循环执行多次, sqlStat 挂 Proxy 一次创建多次记账, 避免重复参数化 | StatFilter.java:429-433, 677-693 |
| q3 | 参数化聚合 | 原始 SQL 每参数不同会撑爆统计表 — mergeSql(参数化)作 key 归一, WHERE id=123 与 id=456 合并 | StatFilter.java:677-693, sql/visitor/ParameterizedOutputVisitorUtils |
| q4 | 慢 SQL 快照 | JSON 序列化参数(超 100 字符截断+流类型占位) — 可读与安全兼顾; 阈值默认 3000ms 可系统属性覆盖 | StatFilter.java:499-523, 562, L71/L51 |
| q5 | 三级聚合 | DataSource(池健康)→SQL(语句慢)→Table(表被写) 三粒度回答三问题; getRuningSqlList 供借出超时异常 | JdbcDataSourceStat.java:38, JdbcSqlStat.java:33, TableStat.java:30, DruidDataSource.java:1746 |
| q6 | 计数原子化 | **JdbcSqlStat 30+ 统计字段全部 AtomicFieldUpdater 原子更新**(runningCountUpdater L62/直方图 updater 族 L136-142) — 无锁无 synchronized, 高并发统计零锁竞争 | JdbcSqlStat.java:40-142 |
| q7 | 双直方图分离 | 8 档幂次分桶(0-1ms~1000000+ms, 独立 updater); **执行耗时直方图 vs 执行+结果集持有直方图分离** — 非 ExecuteQuery 且非首结果集时耗时并入后者(addExecuteTime L702-707), 查询类持有时间走 ResultSet 生命周期单独记 | JdbcSqlStat.java:127-134, 702-707 |

→ 引出 D-4: WallFilter 防火墙 — 链上第二个拦截器 (直接 override 风格, AST 级规则检查)
