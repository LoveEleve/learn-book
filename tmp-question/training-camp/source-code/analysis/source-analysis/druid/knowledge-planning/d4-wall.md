# D-4 WallFilter 防火墙 — 前置检查 + AST 规则 + 方言 Provider

> 项目: Druid (JDBC 连接池) | 🔴 Deep / 1 篇 | WallFilter(1638)+WallProvider(897)+WallConfig+wall/spi(14 Provider/Visitor)+violation(ErrorCode/IllegalSQLObjectViolation/SyntaxErrorViolation)
> 基线: DRUID-PLAN D-4 (防火墙) — 前置: **D-2(链, 已分析) + D-6(AST+Visitor, 已分析)** — 展开检查链路; 对照: StatFilter(模板钩子) 前置 vs 后置

---

## §0.8

- 🔴 Deep，1篇 — 拦截风格(**直接 override statement_execute 族[L480/504/526/548/570/593]: createWallContext→sql=check(sql)[L485]→chain.statement_execute→statExecuteUpdate/setSqlStatAttribute[L489-495], execute 前决定放行** — 对照 StatFilter 模板钩子后置) → Provider 装配(**WallFilter.init[L110]: configFromProperties[L72]→initWallProvider(dataSource,dbTypeName,config): SPI WallProviderCreator[L199] 或按 DbType new MySql/Oracle/SQLServer/PG/DB2/SQLite/CK WallProvider[L146-197]**) → 检查链路(**WallProvider.check(L441)→checkInternal(L454): 特权白名单[L459-463]→白/黑名单快速路径 checkWhiteAndBlackList(L468→L625: whiteListEnable+getWhiteSql 精确/正则命中即返回, 记录 whiteListHitCount)→hardCheck: createParser(L481)+Lexer 配置(注释禁/insertValuesCheck/strictSyntaxCheck)[L482-500]→parseStatementList(L494)→多语句检查(L517)→createWallVisitor(L521)→stmt.accept(visitor) 遍历规则(L533)→violations 汇总(L540)→addBlackSql 记录黑名单(L560)/白名单记录(L585+)→命中 selectLimit 检查**) → 规则与方言(**WallConfig 规则: hintAllow 默认 true/commentAllow 默认 false/strictSyntaxCheck 默认 true/multiStatementAllow 默认 false/completeInsertValuesCheck+insertValuesCheckSize=3[L45-127] + wall/spi 每库 Provider+Visitor: MySqlWallProvider/MySqlWallVisitor 等, WallVisitorUtils 片段提取; violation 记录 ErrorCode+IllegalSQLObjectViolation(表/函数/语法)+SyntaxErrorViolation**)
- 设计模式: [模式: 前置守卫]—execute 前 check; [模式: 双路径]—白名单快路径+AST 硬检查; [模式: 策略]—方言 Provider

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| WallFilter.java:480,485,489 | 拦截点 | **statement_execute(L480): createWallContext(L483)→sql=check(sql)(L485)→chain.statement_execute(L489)→statExecuteUpdate(L491)/setSqlStatAttribute(L494)** | High |
| WallFilter.java:110,121,146 | Provider | **init(L110)→initWallProvider(L121): SPI WallProviderCreator(L199); 按 dbType: MySql(L146)/Oracle(L157)/SQLServer(L165)/PG(L176)/DB2(L183)/SQLite(L190)/CK(L196)** | High |
| WallProvider.java:441,454,459 | 检查入口 | **check(L441)→checkInternal(L454): doPrivilegedAllow 特权(L459)→checkWhiteAndBlackList(L468)** | High |
| WallProvider.java:625,630,641 | 白名单 | **checkWhiteAndBlackList(L625): whiteListEnable→getWhiteSql(sql)(L630) 命中: whiteListHitCount+++executeCount+++recordStats→new WallCheckResult(L641-649) 跳过硬检查** | High |
| WallProvider.java:481,494,517 | AST 解析 | **createParser(sql)(L481)+Lexer 注释控制(L482-486)→parseStatementList(L494)→多语句禁(L517)** | High |
| WallProvider.java:521,533,540 | 规则遍历 | **createWallVisitor(L521)→stmt.accept(visitor)(L533)→visitor.getViolations() 并入(L540)** | High |
| WallProvider.java:560,585 | 违规/放行 | **violations>0: addBlackSql 记录黑名单(L560); 否则 addWhiteSql(L587); selectLimit 检查** | High |
| WallConfig.java:45,75,79,80,126 | 规则集 | **hintAllow=true(45)/multiStatementAllow=false(75)/commentAllow=false(79)/strictSyntaxCheck=true(80)/completeInsertValuesCheck=false+insertValuesCheckSize=3(126-127)** | High |
| wall/spi/ | 方言族 | **14 文件: MySqlWallProvider/MySqlWallVisitor/OracleWallProvider/... 每库 Provider(缓存+统计)+Visitor(规则实现)** | High |
| wall/violation/ | 违规记录 | **ErrorCode 枚举+IllegalSQLObjectViolation(错误码+SQL 片段)+SyntaxErrorViolation(解析异常)** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 防火墙是完整安全检查链路 — 1篇 (~60行) 按"拦截风格→Provider 装配→检查链路→规则与方言"展开; AST 内核引用 D-6(已分析), 拦截点机制引用 D-2(已分析)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 前置检查风格 (override statement_execute) | 🔴 | **为什么🔴**: 与 StatFilter 对照 |
| P1-2 | Provider 装配 (SPI+按 dbType) | 🔴 | **为什么🔴**: 方言选择 |
| P1-3 | 白/黑名单快速路径 | 🔴 | **为什么🔴**: 性能快路径 |
| P1-4 | AST 硬检查 (parse→visit→violations) | 🔴 | **为什么🔴**: 核心规则引擎 |
| P1-5 | 规则集 WallConfig | 🔴 | **为什么🔴**: 规则可配 |
| P2-1 | 违规记录与黑名单回填 | 🟡 | **为什么🟡**: 违规可观测 |
| P2-2 | 方言 Provider/Visitor 族 | 🟡 | **为什么🟡**: 多库适配 |
| P3-1 | 与 Spring 过滤器/Shiro 思路对照 | 🟢 | **为什么🟢**: 安全拦截哲学 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **拦截风格+装配** | 🔴 | 入口 |
| B | **检查链路 (双路径)** | 🔴 | 核心 |
| C | **规则与方言** | 🔴 | 策略 |
| D | **违规记录** | 🟡 | 闭环 |

> **Cluster A (§1)**: override 前置检查+Provider 按 dbType 装配
> **Cluster B (§2)**: 白名单快路径 vs AST 硬检查 (check→parse→visit→violations)
> **Cluster C (§3)**: WallConfig 规则集+方言 Provider/Visitor
> **Cluster D (§4)**: 违规记录+黑名单回填+统计

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 前置守卫风格 | 防火墙必须在 execute 前裁决 → 覆写方法本体抢占 chain 之前; 模板钩子无拦截能力 | WallFilter.java:480-502 |
| q2 | 双路径检查 | **白/黑名单是 ConcurrentLruCache(1024/256) + 参数化 SQL 作 key**(L67, 352-355) — `id=123` 与 `id=456` 命中同一条目; 命中直接返回绕过解析, 未命中才 AST 硬检查 | WallProvider.java:58-67, 352-355, 481-540 |
| q3 | 规则配置化 | 安全策略因业务而异(注释/多语句/严格语法) → WallConfig 全部可配, 默认禁注释/多语句, hint/strictSyntax 允许 | WallConfig.java:45-127 |
| q4 | 黑名单回填 | 违规 SQL 入黑名单 LRU(256), 同形(参数化)二次命中直接拦截; violation 以 SQLException 抛业务 | WallProvider.java:61, 572, WallFilter.java:493-495 |
| q5 | 参数化 key | getWhiteSql/getBlackSql 都用 **getMergedSqlNullableIfParameterizeError(参数化 SQL, 解析失败回退原 sql)** 作 key — 白名单判定天然按"语句形态"而非"语句实例" | WallProvider.java:352-361 |

→ 引出 D-9: Boot3 Starter — WallFilter 经 DruidFilterConfiguration 条件注册进链 (autoAddFilters 注入), 装配收尾
