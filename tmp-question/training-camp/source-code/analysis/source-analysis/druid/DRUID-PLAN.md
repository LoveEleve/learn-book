# Druid — 知识网络化规划 (D-1~D-9) v5

> **日期**: 2026-08-12 | **依据**: issue/Druid源码学习范围规划.md (v1.2.27, 9域基线) + 00 域发现深度 REVIEW (v1→v5, 缺陷谱系 19 处, 见 §九)
> **源码**: `/data/workspace/source-code/code/spring/druid` (core 模块 1614 文件; pool 72/filter 29/sql 1238/wall 46/proxy 36/stat 24, com.alibaba.druid)
> **定位**: 阶段3.2 — 连接池. 核心 = **ReentrantLock + Condition 阻塞模型** (与 HikariCP ConcurrentBag 无锁模型两极对照) + **Filter 链拦截所有 JDBC 操作** (Druid 独有扩展点)
> **知识网络**: 本文含 前置/复用/引出 双链, 与 Spring 层(S-2 装配/C-11 DataSource/s29-33 tx) + HikariCP 层(H-1~H-13) 双向互联

---

## 一、入口点与主线

`DruidDataSource.init()` (L659) / `DruidDataSource.getConnection()` (L2290) → `FilterChainImpl.dataSource_connect` (L5067) → `getConnectionDirect` (L1366) → `getConnectionInternal` (L1543: closed/enable 检查 → createDirect 优化 → maxWaitThreadCount 限流 → onFatalError 保护 → pollLast/takeLast [L2214/L2218: lock+notEmpty.await]) → `DruidPooledConnection` 代理 → `close` (L236) → `recycle` (L329) → `DruidDataSource.recycle` (L1894) → `putLast`+notEmpty.signal. 旁路: sql/ (解析器), wall/ (防火墙), filter/stat (监控), support/http (StatViewServlet 控制台), druid-spring-boot-3-starter (自动装配).

---

## 二、入口展开追踪 (00 §2 — Level 逐层候选 → §3 设计决策测试 → 结论)

> **候选 = 入口点 new/set/add/调用 的对象**。每候选过 §3 测试: ①含设计决策? ②是否 thin wrapper? 定量预检 (≥50 文件包 / ≥500 行类) 已读关键类后再决.

### Level-1 (init + getConnection 直接调用)

| 候选 | 源码位置 | 设计决策测试 | 结论 |
|---|---|---|---|
| `filters[i].init()` | DruidDataSource.java:697-699 | Filter 体系 — 含 SPI 加载/初始化设计决策 | → **D-2** |
| `initFromSPIServiceLoader()` (L746) + `resolveDriver()` (L748) + `initCheck()` (L750) | L1014/1040 | driver 解析: SPI→wrap→classloader 多策略, 有决策 | **D-1** 内 (展开) |
| `initExceptionSorter` (L754) + `initValidConnectionChecker` (L755) + `validationQueryCheck` (L756) | L1240/L1272 | 验证 SPI 初始化 — 决策载体是 ValidConnectionChecker 族 | **D-7** |
| `connections/evictConnections/keepAliveConnections/nullConnections = new ...` | L772-775 | 固定数组+4 个平行数组 — 池存储模型 | **D-1** 内 |
| `createPhysicalConnection()` 同步初建 (L787) | L1704 (Abstract) | 无 filter 走 driver.connect 裸连, 有 filter 走 chain.connection_connect | **D-1** 借 D-2 (环, 见 §四) |
| `createAndLogThread/createAndStartCreatorThread/createAndStartDestroyThread` (L807-809) | L971/983/1001 | 三线程+initedLatch 同步 — 维护体系 | **D-5** |
| `registerMbean()` (L822) | L2160 | MBean 注册→DruidDataSourceStatManager (424行) — **只读统计暴露, 无热改 setter, 消费 stat 数据** (对照 Hikari H-10 有 11 个热改 setter 故为域; Druid 无此决策) | **排除** → D-3 一句话引用 |
| `keepAlive` 时 `empty.signal()` (L828-837) | L828 | Condition 补池信号 | **D-1** 内 |
| `getConnection` → `createChain()`→`dataSource_connect` (L1343-1353) | Abstract L303 | Filter 链门面 | **D-2** |
| `getConnectionDirect` (L1366) | L1352 | 池借出核心: notFullTimeoutRetry 循环 | **D-1** 内 |
| `getConnectionInternal` (L1543) | L1366 | createDirect/maxWaitThreadCount/onFatalError/pollLast | **D-1** 内 |
| `holder` → `DruidPooledConnection` 包装 | L1768-1800 | 池连接代理 (1298行) — 借出返回体 | **D-1** 内 |
| `removeAbandoned` 启用时借出栈追踪 (L1434-1446) | L1434 | 泄漏检测(借出点栈) — 对照 H-8 | **D-5** 内 |
| `testOnBorrow/testWhileIdle` 验证 (L1384-1432) | L1384 | 3 时机之一 | **D-7** |

### Level-2 (Level-1 候选的调用)

| 候选 | 源码位置 | 设计决策测试 | 结论 |
|---|---|---|---|
| `DruidPooledConnection.close` → `createChain().dataSource_recycle` / `recycle()` 双路径 (L236-350) | L274-282 | 归还链 (有/无 filter 双路径) | **D-1** 借 D-2 |
| `DruidDataSource.recycle` (L1894) | L1894 | rollback→holder.reset→useCount→testOnReturn→putLast | **D-1** 内 |
| `holder.reset()` 四态复位 (L371-418) | L371 | readOnly/holdability/isolation/autoCommit+清监听+关 statementTrace | **D-1** 内 |
| `holder.getStatementPool()` (prepareStatement 路径) | Holder L317-322 | per-connection LRU 缓存 | **D-8** |
| `shrink()` (DestroyTask 调用) | L3069 | 4 阶段驱逐算法 (~120 行) | **D-5** 内 |
| `validateConnection` (shrink keepAlive 段) | L3192 | 验证 SPI 调用点 | **D-7** |
| `DruidPooledConnection.prepareStatement` → `PreparedStatementKey` (L355-365) | L359 | PSCache 键设计 | **D-8** |
| FilterChainImpl.nextFilter()/pos (L469/L37) | L469 | 递归链机制 | **D-2** 内 |

### Level-3 (Level-2 的调用 — 链上拦截器)

| 候选 | 源码位置 | 设计决策测试 | 结论 |
|---|---|---|---|
| `StatFilter` 埋数钩子 | StatFilter.java L378/383/388/407/536 | **模板钩子族**: statementExecuteBefore/After+BatchBefore/After+statement_executeErrorAfter — 不 override statement_execute | **D-3** |
| StatFilter.mergeSql → `ParameterizedOutputVisitorUtils` | sql/visitor/ | SQL 参数化 — 消费 parser | **D-3** 借 **D-6** |
| `WallFilter` → `WallProvider.check` (L441) | wall/WallFilter L322/480 | **直接 override statement_execute 族前置检查** (与 StatFilter 模板钩子后置埋数形成对照) | **D-4** |
| stat/ 数据结构 (JdbcDataSourceStat 504/JdbcSqlStat 1074/TableStat 642) | stat/ | 三级统计聚合 | **D-3** 内 |
| `createConnectionThread.run` → `createPhysicalConnection` (L2793) | L2793 | 线程建连+empty 等待+errorCount 重试 | **D-5** 内 |

### 展开停止

Level-3 后零新候选 (conn 包装/stat 计数器/规则检查均为已命名域的内部机制)。

---

## 三、旁路扫描 (00 §2.5 — 顶层目录未被入口访问的)

| 包/模块 | 文件数 | 定量预检 | 设计决策测试 | 结论 |
|---|---|---|---|---|
| `sql/` | 1238 | 已读 SQLStatementParser(49行类)/Lexer(41)/visitor 族/SchemaRepository(54) | 完整 SQL 解析器 — 承载多方言 AST 设计决策, 但被 D-3/D-4 消费, 学概览 | **D-6** 🟡 (概览级, 深度不展开) |
| `wall/` | 46 | 已读 WallFilter(1638)/WallProvider(897)/spi 14 方言 | 防火墙规则引擎 — 独立设计决策 | **D-4** |
| `stat/` (统计结构) | 24 | 已读 JdbcDataSourceStat/JdbcSqlStat/TableStat/DruidStatService(400)/DruidStatManagerFacade(430) | 统计结构=MBean 载体; StatService 是 JSON API 消费层(反射访问, 无池决策) | **D-3** 内; StatService/MBean 服务层**排除** |
| `proxy/jdbc/` | 36 | 已读 ConnectionProxyImpl(553)/StatementProxyImpl(585)/WrapperProxyImpl(116)/DataSourceProxyImpl(378) | **定量预检完成** (两处 ≥500 行已读): ConnectionProxyImpl = 40+ JDBC 方法**样板转发** (createChain/recycleFilterChain 门面 L75/86 + getRawObject), StatementProxyImpl = 链载体 + **JdbcSqlStat 挂载点** (getSqlStat L514/getLastExecuteType L551) — 无独立算法决策 (§3 thin-wrapper of chain) | 并入 **D-2** (展开: 代理对象模型) |
| `support/` | 105 | 已读 StatViewServlet(235)/WebStatFilter(341)/AbstractWebStatImpl(286)/ResourceServlet(373) | Web 监控展示层 — 消费 stat, 不承载池决策 (WebStatFilter 是请求统计非池拦截) | **排除** (StatViewServlet 在 D-9 一句话) |
| `druid-admin/` | 13 | 已审计 | 独立 Web 控制台 | 排除 (基线一致) |
| `druid-spring-boot-3-starter/` | 9 | 已读 DruidDataSourceAutoConfigure/DruidDataSourceWrapper/StatView 配置 | 自动装配链路 — 有决策 (Wrapper+条件注册) | **D-9** |
| `druid-spring-boot-starter/` (Boot2) | 8 | 已审计 | Boot2 版本 | 排除 (项目用 Boot3) |
| `druid-wrapper/` | 13 | 已审计 | 老系统适配 | 排除 |
| `druid-demo-petclinic/` | — | 已审计 | Demo | 排除 |
| `proxy/DruidDriver` | 1 | 已读 (323行) | wrap-driver 门面 (jdbc:wrap-jdbc:), 无独立决策 | **D-1** 一句话 (initFromWrapDriverUrl L1040) |
| `support/metrics/MetricCollector` | 1 | 已读 | 单文件薄采集器 | 排除 |
| `mock/` | — | — | 测试基础设施 | 排除 |

---

## 四、依赖图 (02 §1.2 强制) + 环形依赖识别 (02 §1.4)

```
D-1: 依赖 = C-11(spring-jdbc DataSource 内核), D-7(验证策略), D-2(借/还走链, 环!)
      不依赖 = D-3/D-4/D-5/D-6/D-8/D-9
D-2: 依赖 = D-1(链尾 getConnectionDirect L5074 / dataSource_recycle L5063 执行池操作), s24-s28 aop(对照)
      不依赖 = D-3~D-9
D-3: 依赖 = D-2(拦截点), D-6(参数化), stat/ 结构(域内)
      不依赖 = D-4/D-5/D-7/D-8/D-9
D-4: 依赖 = D-2(拦截点), D-6(AST+Visitor), wall/ 规则(域内)
      不依赖 = D-3/D-5/D-7/D-8/D-9
D-5: 依赖 = D-1(connections 数组+Condition), D-7(validateConnection L3192)
      不依赖 = D-2/D-3/D-4/D-6/D-8/D-9
D-6: 依赖 = 无 (叶子 — 仅被 D-3/D-4 消费; init 里 JdbcUtils.getDbType L702 是 util 非 parser)
      不依赖 = D-1~D-9 全部
D-7: 依赖 = D-1(调用点: testOnBorrow L1385/testWhileIdle L1421/recycle testOnReturn L1980)
      不依赖 = D-2~D-6/D-8/D-9
D-8: 依赖 = D-1(holder+DruidPooledPreparedStatement)
      不依赖 = 其余
D-9: 依赖 = D-1(DruidDataSource 子类化+init), S-2(自动装配管线复用)
      不依赖 = D-2~D-8
```

**环形依赖**: **D-1 ↔ D-2** (D-1 借/还走 `createChain()→dataSource_connect/recycle`; D-2 链尾执行 `getConnectionDirect` 池操作)。

**三轮化解 (02 §1.4 策略)**:
1. 第一轮各自浅扫: 双方已完成 — D-1 读 init/借还/shrink 全链, D-2 读 ChainImpl 递归+Holder 缓存 (本文 §二)
2. 第二轮交叉验证: 联合读交互点 — `DruidPooledConnection.close` (L236) 双路径 + `FilterChainImpl.dataSource_connect/recycle` (L5067/L5063) + `createChain/recycleFilterChain` (Holder L223-237) 已在本轮完成
3. 第三轮各自收尾: **教学上 D-1 先写, 借/还对 D-2 用导航指针** ("有 filter 时走 createChain→dataSource_connect, 机制见 D-2" — 同 HikariCP H-3→H-5 先例); D-2 正文链尾池操作深引用 D-1. 环不阻塞顺序.

**Hub 检查 (02 §检查清单)**: 无域被 ≥10 域依赖 (D-1 被 4 域依赖为最高) — 无 Hub 升级.

---

## 五、域清单 (9 域 / 5🔴 + 4🟡, 含三信号置信度 00 §3.5 + 方案预告 04)

### 第 1 层: 池核心 (2 域 全部 🔴)

| # | 域 | 包 | 核心主题 (设计决策) | 依赖 | 信号/置信度 | 方案 |
|:--:|---|---|---|---|:--:|:--:|
| D-1 | 连接池核心 | pool/ | **DruidDataSource(3979)+DruidAbstractDataSource(2388)+DruidConnectionHolder(476)+DruidPooledConnection(1298)** — 固定数组 connections[maxActive]+ReentrantLock+Condition(notEmpty/empty) 阻塞模型; init 链路(SPI→driver 解析→filter.init→三线程→initedLatch→MBean); 借: createDirect 自建直连+maxWaitThreadCount 限流+onFatalError 保护+pollLast(带超时)/takeLast(无限)+notFullTimeoutRetry→incrementUseCount+new DruidPooledConnection(L1768-1770); 还: rollback 未提交事务→holder.reset 四态→phyMaxUseCount/密码版本→testOnReturn→putLast+recycleCount+full 判定(L2029-2033), catch 段 clearStatementCache+discard; **异常联动: handleConnectionException(L1773)→exceptionSorter.isExceptionFatal→handleFatalError 标记 (D-5 shrink fatalErrorIncrement 的来源)**; shutdown(L2081): interrupt 三线程→cancel futures→关 PSCache+物理连接→unregisterMbean→enable=false→notEmpty.signalAll()→filter.destroy; wrap-driver 旁路; 富异常(active/maxActive/creating/runningSql) | C-11, D-7(导航), D-2(环,导航) | 面试高频/生产主流/Hub ✅ | 高 | A |
| D-5 | 维护体系 | pool/(内嵌类) | **shrink 4 阶段**(fatalError 增量→驱逐[phyTimeout/minEvictable+checkCount/maxEvictable]→keepAlive 收集+validateConnection→System.arraycopy 紧凑+nullConnections 清理) + DestroyTask(shrink+removeAbandoned) + CreateConnectionTask(createScheduler 并行, **errorCount>connectionErrorRetryAttempts 后按 timeBetweenConnectErrorMillis 固定间隔重排, failContinuous 标记** — 非指数退避) + CreateConnectionThread(empty.await 按需创建)+DestroyConnectionThread(周期 sleep)+removeAbandoned(超时强收+借出栈追踪, 对照 H-8) | D-1, D-7 | 面试中频/生产主流/依赖少 ✅ | 高 | A |

### 第 2 层: 拦截链 (1 域 🔴)

| # | 域 | 包 | 核心主题 (设计决策) | 依赖 | 信号/置信度 | 方案 |
|:--:|---|---|---|---|:--:|:--:|
| D-2 | Filter 拦截链 | filter/ + proxy/jdbc/ | **Filter(1378行接口, L35 extends Wrapper)→FilterAdapter(2891)空实现→FilterEventAdapter(541)模板方法层(statement_execute L178 模板+statementExecuteBefore/After 等 protected 钩子)→FilterChain(1192行链契约接口)→FilterChainImpl(5287)递归链实现** — pos 指针+filterSize 控制进度, nextFilter() 逐 Filter 下推, **链尾执行 raw JDBC 或池操作**; **proxy/jdbc 代理对象模型(36 文件: DataSource/Connection/Statement/PreparedStatement/ResultSet Proxy+Impl, WrapperProxyImpl.getId, StatementProxy 承载 JdbcSqlStat/executeType 挂载点)** 为链载体; FilterChainImpl **对象池复用**(Holder.createChain/recycleFilterChain, per-connection 缓存, 避免每次操作 new); FilterManager+AutoLoad SPI; 内置 5 类 Filter; 借/还双路径; **两种 Filter 实现风格对照: 模板钩子(StatFilter) vs 直接 override( WallFilter)** | D-1(链尾), s24-s28 aop 对照 | 面试中频/生产主流/依赖有 ✅ | 高 | A |

### 第 3 层: 监控与安全 (2 域 全部 🔴)

| # | 域 | 包 | 核心主题 (设计决策) | 依赖 | 信号/置信度 | 方案 |
|:--:|---|---|---|---|:--:|:--:|
| D-3 | StatFilter 监控 | filter/stat/ + stat/ | **FilterEventAdapter 模板钩子族埋数** (statementExecuteBefore L378/After L383/BatchBefore L388/BatchAfter L407/statement_executeErrorAfter L536): **JdbcSqlStat 懒创建+挂载到 StatementProxy** — createSqlStat(L429-433): **mergeSql 参数化 SQL 作为 JdbcSqlStat key** (L624-640) → 运行中计数/事务内计数 (L448-455) → 耗时 addExecuteTime(lastExecuteType, firstResult) (L470-483) → updateCount 统计 (L484-497) → **慢 SQL** (millis>=slowSqlMillis 默认 3000 L71, SYS_PROP L51; buildSlowParameters JSON 参数快照 L559→分级日志→handleSlowSql L499-523) + ResultSet 读行统计 (resultSetOpenAfter) + 归还持有时间直方图 (dataSource_releaseConnection→connectionHoldHistogram) + 三级聚合 stat/(JdbcDataSourceStat 504→JdbcSqlStat 1074→TableStat 642) + getRuningSqlList 供借出超时异常(L1746) + MBean/StatService 只读暴露(一句话) | D-2, D-6 | 面试中频/生产主流/依赖有 ✅ | 高 | A |
| D-4 | WallFilter 防火墙 | wall/ | **直接 override statement_execute 族 (L480/504/526/548/570/593), execute 前 check(sql)** — 前置检查风格 (对照 StatFilter 模板钩子后置埋数): **4 阶段**: 白/黑名单快速路径(checkWhiteAndBlackList L468)→AST parse(parser+Lexer 配置: 注释禁/insertValuesCheck/严格语法)→WallVisitor 遍历→违规拦截; 规则体系 WallConfig(表/函数/语句类型/条件必有/多语句/注释) + 方言 Provider/Visitor 族(spi/ 16 个: MySql/Oracle/PG/DB2/SQLServer/CK...) + WallViolation 记录(IllegalSQLObjectViolation/SyntaxErrorViolation) | D-2, D-6 | 面试中频/生产常见/依赖有 ✅ | 高 | A |

### 第 4 层: 支撑 (4 域 1🔴 3🟡)

| # | 域 | 包 | 核心主题 (设计决策) | 依赖 | 信号/置信度 | 方案 |
|:--:|---|---|---|---|:--:|:--:|
| D-7 | 连接验证 | pool/ + pool/vendor/ | ValidConnectionChecker SPI(29 行接口)+**6 实现**: pool/JDBC4ValidConnectionChecker(默认 isValid())+vendor/ MySql SELECT 1+Oracle DUAL+MSSQL+PG+OceanBase — **initValidConnectionChecker 按 driver 类名匹配选择** (L1246-1268); **3 时机** testOnBorrow(借出同步 L1385)/testWhileIdle(空闲超 timeBetweenEvictionRunsMillis L1418)/testOnReturn(归还 L1979); **双路径**: validateConnection(L1434)=checker.isValidConnection 优先, 无 checker 时 ValidConnectionCheckerAdapter.execValidQuery 执行 validationQuery 兜底, **验证成功后 onFatalError 复位 (L1449/L1493, 与 D-5 fatalError 驱逐联动)**; validationQueryTimeout; ExceptionSorter 旁路(厂商异常分类, 一句话) | D-1(调用点) | 面试低频/生产主流/叶子 | 中 | B |
| D-8 | PreparedStatementPool | pool/ | **per-connection LRU**(LinkedHashMap accessOrder=true L185-201), maxPoolPreparedStatementPerConnectionSize 默认 10 (DruidAbstractDataSource L118), PreparedStatementKey(sql+catalog+MethodType) 复用键; **入池路径**: DruidPooledPreparedStatement.close(L153)→DruidPooledConnection.closePoolableStatement(L138): clearParameters/clearBatch→`pooled && poolPreparedStatements && exceptionCount==0` 才 put 入缓存, exceptionCount>0 则 remove, 否则物理关闭; in-use 保护(sharePreparedStatements), Oracle 隐式缓存适配, hit/miss 计数 | D-1 | 面试低频/生产常见/叶子 | 中 | B |
| D-9 | Boot3 Starter | druid-spring-boot-3-starter/ | DruidDataSourceAutoConfigure(@ConditionalOnClass(DruidDataSource) L46+@AutoConfigureBefore(DataSourceAutoConfiguration) L47+@EnableConfigurationProperties L48, 引入 5 个 stat/ 配置类) → **DruidDataSourceWrapper(extends DruidDataSource+InitializingBean, L31/36)**: afterPropertiesSet **前缀 fallback**(spring.datasource.druid.* 缺省时回退 spring.datasource.* determineUrl/Username, L36-50)→init(); **autoAddFilters(@Autowired List<Filter> 注入全部 Filter bean→filters.addAll, L52-56)**; **DruidFilterConfiguration**: 8 个 Filter(StatFilter/WallFilter/ConfigFilter/Logging×4/Encoding) @ConditionalOnProperty(prefix=…, name="enabled")+@ConfigurationProperties+@ConditionalOnMissingBean 条件注册; DruidStatProperties; StatViewServlet(@ConditionalOnProperty stat-view-servlet.enabled)+WebStatFilter+可选 AOP 切面(DruidSpringAopConfiguration) 条件注册 | D-1, S-2(复用) | 面试低频/生产主流/叶子 | 中 | B |
| D-6 | SQL Parser 体系 | sql/ | **架构概览不深入**: Lexer(词法: Token/CharTypes/Keywords/SymbolTable, 23 parser 文件)→Parser(SQLStatementParser.parseStatementList L118)→AST(422 文件: SQLStatement/SQLExpr/SQLTableSource, SQLObject.setParent)→Visitor(54 文件: OutputVisitor/SchemaStatVisitor/ParameterizedOutputVisitorUtils/SchemaResolveVisitor)→Dialect(713 文件 29 方言目录)→repository/SchemaRepository(元数据); 被 D-3 参数化 + D-4 安全检查消费 | 无(叶子) | 面试低频/生产常用/依赖弱 | 中 | B |

---

## 六、已排除 (00 §3 thin-wrapper/边缘 — 防"存在=域")

| 类/包 | 原因 |
|-------|------|
| stat/ 服务层 (DruidStatService 400/DruidStatManagerFacade 430/DruidDataSourceStatManager 424) | JSON API+MBean 注册 — 反射访问统计, 只读暴露, 无池设计决策 (D-3 一句话) |
| pool/ha/ HighAvailableDataSource (3) | HA 部署层, 非连接池设计 |
| pool/xa/ DruidPooledXAConnection (4) | JTA 集成, 面试低频 |
| pool/vendor/ ExceptionSorter×8 (8) | 厂商异常分类器, 按需查阅 (D-7 一句话) |
| pool/DruidDataSourceC3P0Adapter (2) | C3P0 迁移适配, 过时 |
| filter/config/ ConfigFilter (2) | 配置密码解密, 边缘 (D-2 一句话点名) |
| filter/encoding/ EncodingConvertFilter (2) | 字符集转换, 边缘 |
| filter/logging/ (5) | SQL 日志, 被 StatFilter 慢 SQL 覆盖 |
| filter/mysql8datetime (2) | MySQL8 时间类型微适配, 边缘 |
| proxy/DruidDriver (1) | wrap-driver 门面, 并入 D-1 一句话 (initFromWrapDriverUrl L1040) |
| support/* (105) | 外部集成 (http/spring/hibernate/calcite/quartz/metrics/ibatis/json...), 仅 StatViewServlet 在 D-9 提一句注册 |
| druid-admin/ (13) | 独立 Web 管理控制台, 淘汰 |
| druid-spring-boot-starter/ (Boot2) | Boot2 版本, 项目用 Boot3 |
| druid-wrapper/ (13) | 老系统 Wrapper 适配, 淘汰 |
| druid-demo-petclinic/ | Demo 项目 |
| mock/ (单元测试) | 测试基础设施 |
| sql/ 内部实现 (1238) | D-6 仅架构概览, 不深入 Lexer/AST 生成算法 |

---

## 七、知识网络图 (跨大纲边 — Obsidian 双链 06 §6)

```
← 复用/内核来源 (已分析):
   spring-jdbc (C-11, s53-jdbc-datasource) ──→ D-1, D-9 (DataSource/JDBC 内核)
   spring-tx (s29-s33) ──→ D-1 归还/事务边界 (未提交事务 rollback)
   spring-aop (s24-s28) ──→ D-2 (配置式拦截对照: CGLIB vs Filter 链), D-9 (AOP 切面)
   S-2 自动装配管线 ──→ D-9 (DataSourceAutoConfiguration→DruidDataSourceWrapper 接线)
   HikariCP H-1~H-13 ──→ D-1/D-5/D-7/D-8 (两极对照: ConcurrentBag vs Lock+Condition; HouseKeeper vs shrink; 验证策略; PSCache 有无)

→ 引出/消费者:
   D-1~D-9 ──→ S-10 Boot DataSource (本阶段是 S-10 池化的深入 — Druid 侧)
   D-2 Filter 链 ──→ 阶段3.4 MyBatis 插件机制(未分析, 仅引出)

   📌 双链格式 (每篇大纲 header 写):
   前置: [[C-11]] [[H-1-hikaricp]] ...
   复用: [[s29-tx]] [[s24-aop-proxy]] [[S-2]] ...
   引出: [[s74-boot-datasource]] (S-10) ...
```

> **⚠️ 前向引用原则 (06 §2)**: 大纲正文禁止引用未分析域; D-3/D-4 对 D-6 (SQL Parser) 用导航指针 (同 H-3→H-5 先例), 正文机制必须展开本层内容. HikariCP/Spring 层均已分析 — 对照可放心双向写.

**负面空间对照 (07 §维度5 — 对比型域必须显式声明"不做的事")**:

| 不做的事 | Druid 不做原因 | Hikari 做吗 |
|---|---|---|
| 极致精简/零开销 | Druid 为全功能(监控/防火墙/解析)牺牲性能 — 借出要过 Filter 链 | ✅ 做 (H-2 ConcurrentBag 无锁) |
| 无锁并发 | Druid 用 Lock+Condition 需要池状态一致性+条件等待 | ✅ 做 (借出零锁) |
| 字节码生成代理 | Druid 用 Filter 链+显式 Proxy 对象模型(可扩展) | ✅ 做 (H-12 Javassist) |
| PSCache | — | ❌ 故意不做 (openStatements 只跟踪) |
| SQL 防火墙/解析器 | — | ❌ 无此能力 (需外部) |
| 内置监控埋点 | — | ❌ 只有指标桥 (H-9) |

> 以上写入 D-1/D-2/D-8 大纲的"差异"段, 形成两域写作时的对照锚点.

---

## 八、执行顺序 (拓扑: 叶子先)

**D-1 → D-2 → D-7 → D-5 → D-8 → D-6 → D-3 → D-4 → D-9**

**拓扑验证**: D-5 在 D-1/D-7 后 ✓ | D-8 在 D-1 后 ✓ | D-3/D-4 在 D-2/D-6 后 ✓ | D-9 在 D-1 后 ✓ | D-2 在 D-1 后 ✓ (环已按 §四化解) — 无依赖出现在被依赖之前.

> 每域走 v5 全管线 (KP→大纲→questions→六层深审→更新 HANDOFF). 每域开始前按 04 §产出输出方案选择块 (上表"方案"为预告, A=Pass 0-3+时空溯源+极简复现, B=Pass 0-2+可选 Pass 3).

---

## 九、深度分类复核 (00 §3.5 三信号)

- **5🔴 / 4🟡** (56% 🔴, 未犯 80% 反模式) — 与基线一致
- 🔴 = 池核心定义性机制 (Lock+Condition 池 / shrink 维护 / Filter 链 / Stat / Wall) — 面试+生产+依赖 三信号全一致, 置信度全部 **高**
- 🟡 = 支撑/薄 (验证/PSCache/Parser 概览/Boot3 装配) — 三信号部分一致 (生产常用+依赖有, 面试低频), 置信度 **中** → 按 00 §3.5 呈报用户: 若用户对任一 🟡 分类有异议可调整; D-7/D-8 是叶子故最低 B 不降 C/D (04 §叶子域)
- **REVIEW 缺陷谱系 (v1→v5, 19 处)** — 详见下表:

| # | 轮次 | 域/范围 | 类型 | 错误 | 修复 | 教训 |
|:--:|:--:|:--|:--|:--|:--|:--|
| 1 | v1 | D-5 | 事实错误 | CreateConnectionTask 写"指数退避" | 固定间隔重试 (L2621-2646, **基线同误**) | 机制声明必须读方法体 (01 §6) |
| 2 | v1 | 全局 | 结构缺失 | 无三信号置信度列 | 补全 | 00 §3.5 产出格式逐列对照 |
| 3 | v1 | 全局 | 结构缺失 | 无入口展开候选追踪 | 补 §二 追踪表 | 00 §2 执行过程必须可验证 |
| 4 | v1 | 全局 | 结构缺失 | D-1↔D-2 环未识别 | 三轮化解记录 | 02 §1.4 拓扑前必检环 |
| 5 | v1 | D-2/D-6 | 依赖图错误 | D-2 漏 D-1; D-6 错写依赖 D-1 | 修正 | 依赖逐域核对链尾调用点 |
| 6 | v1 | 全局 | 归属未定 | MBean/StatService 未过测试 | 定案排除 (对照 H-10) | 主路径候选必须过 §3 测试 |
| 7 | v2 | D-3 | 机制错误 | 埋数"statement_execute\* override" | 模板钩子族+JdbcSqlStat 挂载 (L378-536) | 拦截机制 grep 全部 override 签名 |
| 8 | v2 | D-2 | 预检未完成 | proxy/jdbc ≥500 行类未读 | 定量预检完成 (两处类已读) | 00 §3 预检触发必读完关键类 |
| 9 | v2 | D-8 | 机制缺失 | 入池路径未写 | 三分支 (put/remove/物理关) | 生命周期逐分支核对 |
| 10 | v2 | 全局 | 结构缺失 | 🟡 域无置信度 | 补"中" (呈报用户) | 每域都要置信度 |
| 11 | v3 | D-7 | 数字错误 | 实现数 4 个 | 6 个 (补 MSSQL/OceanBase, **基线同误**) | "N 个实现"穷举目录 |
| 12 | v3 | D-7 | 机制缺失 | 无兜底路径 | validateConnection 双路径+onFatalError 复位 | 兜底路径与状态联动要追到 |
| 13 | v3 | D-1 | 机制缺失 | 无 shutdown/异常联动 | close 全流程+isExceptionFatal→fatalError | 生命周期收尾+异常联动 |
| 14 | v3 | D-9 | 机制缺失 | 无 Filter 注册链 | DruidFilterConfiguration+autoAddFilters | 装配链追到 Filter bean 注入 |
| 15 | v4 | D-2 | 结构缺失 | 无 FilterChain 契约接口 | 四层链完整命名 | 接口层级完整命名 |
| 16 | v4 | 全局 | 对照缺失 | 无"不做的事"声明 | 负面空间表 (07 §维度5) | 对比型域显式声明边界 |
| 17 | v4 | 全局 | 产出物缺失 | PLAN 与 08 无映射 | §十二 产出物映射表 | 规划产出对照标准文件 |
| 18 | v4 | 全局 | 数字复核 | pool 文件数未实证 | find 全量=72 ✓ 基线正确 | 基线数字复核后再引用 |
| 19 | v4 | 全局 | 行号复核 | 关键行号区间未逐段核验 | StatFilter/WallProvider/Wrapper 等全部实证 | 行号锚点逐段验证 (HANDOFF §三) |

**共性规律**: ①机制/数字声明必须 grep+读方法体实证 ②"N 个实现/所有"类断言穷举目录 ③定量预检触发必读完关键类再决 ④生命周期全段(借出/归还/收尾/异常)逐分支核对 ⑤主路径/旁路候选都要过 §3 测试并记录 ⑥依赖图逐域核对链尾调用点 ⑦产出结构对照 00 §3.5/§8 + 08 标准 ⑧对比型域补负面空间

---

## 十、与原始执行计划 (issue/Druid源码学习范围规划.md) 的差异

| 原始 | 本规划 | 理由 |
|:--:|---|---|
| D-1 池核心 (DruidDataSource+Abstract+Holder) | + DruidPooledConnection(1298行) 归还链 | REVIEW 发现借出返回类型与 close 双路径未点名 |
| D-2 Filter 链 (Filter/Adapter/EventAdapter/ChainImpl) | + proxy/jdbc 代理对象模型 36 文件 + chain 对象池复用 | 链的载体被基线漏掉 — Filter 操作对象是 Proxy 族 |
| D-5 维护体系 | 同基线 + fatalError 增量段 + 重试机制精确化 | 基线已完整; **基线也写"指数退避"为误** — 实测 `connectionErrorRetryAttempts` 阈值+`timeBetweenConnectErrorMillis` 固定间隔重排 (DruidDataSource.java:2621-2646), v1/v2 沿袭后修正 |
| D-7 验证 (3 时机+SPI 4 实现) | **实现数修正 4→6**: JDBC4 默认+MySql+Oracle+MSSQL+PG+OceanBase, 按 driver 类名匹配 (L1246-1268); + validateConnection 双路径+onFatalError 复位 | 基线"4 种"漏 MSSQL/OceanBase — 实测 vendor/ 有 5 家+JDBC4 默认 |
| D-1 池核心 | + shutdown 全流程+异常联动 (handleConnectionException→isExceptionFatal→handleFatalError) | 基线只写"shutdown()"一句话; fatalError 是 shrink 驱逐的联动源头, 必须点名 |
| D-9 Boot3 | + DruidFilterConfiguration(8 Filter 条件注册)+autoAddFilters(@Autowired 注入) | 基线只写"自动注册 Filter+监控页面", 未点 Filter bean 注册与注入链 |
| D-3 StatFilter (SQL 统计) | **机制锚点修正**: 基线未写埋数机制 | 实测 StatFilter **不 override statement_execute**, 而是覆盖 FilterEventAdapter **模板钩子族** (statementExecuteBefore L378/After L383/ErrorAfter L536) + **JdbcSqlStat 懒创建挂载 StatementProxy** (L429-433) — D-3 大纲按此展开 |
| D-8 PSCache | + 入池路径三分支 | 实测 close→closePoolableStatement(L138): clearParameters→`pooled&&poolPreparedStatements&&exceptionCount==0` 才 put, 异常则 remove, 否则物理关闭 |
| — | + 排除清单 (16 项, 含 MBean 服务层) | 00 要求显式排除薄/边缘 |
| — | + 知识网络双链 (HikariCP 对照边) | 阶段3 核心价值: 两极对照必须落成双链 |

**覆盖率报告 (00 §第九步)**: 方法论域发现 9 域 = 基线 9 域, 覆盖率 **100%**. 差距分析: 无遗漏. 方法论额外发现: proxy/jdbc 载体 (并入 D-2 — 定量预检完成: 两处 ≥500 行类已读, 全量转发无独立算法, §3 thin-wrapper 通过); MBean 服务层 (排除 — 只读暴露无热改, 与 H-10 成域形成对照, 理由记录于 §六).

---

## 十一、完成检查单 (00 §8 逐项)

- [x] 入口点已记录: `DruidDataSource.init()/getConnection()` + 主线
- [x] 所有层级已展开: §二 Level-1/2/3 候选追踪, Level-3 后零新候选
- [x] 旁路扫描已完成: §三 12 个顶层目录/模块逐项定量预检+测试
- [x] 设计决策测试已应用: §二/§三 每候选含测试与结论
- [x] 依赖已映射: §四 每域完整依赖+不依赖列表
- [x] 环形依赖已识别: D-1↔D-2, 三轮化解策略已记录
- [x] 拓扑排序已验证: §八 逐条验证无逆序
- [x] 排除清单已记录: §六 16 项含原因
- [x] 对照验证完成 (第九步): §十 覆盖率报告 100%, 差距全解释
- [x] Hub 检查 (02): 无 Hub; 叶子域 D-6/D-7/D-8 最低 B 已标
- [x] 产出物映射 (08): 本 PLAN 对应 08 的 `00-domain-list.md`(域#/路径/设计决策/信号/置信度/依赖图/拓扑) + `02-approach-selection.md`(A/B 方案列) + `01-book-plan.md`(卷分层/教学顺序) — 单文件合并 (同 HIKARICP-PLAN 惯例); HANDOFF.md 在阶段收尾时产出

---

## 十二、产出物映射 (08-最终产出物.md 对照)

| 08 标准文件 | 本文位置 | 说明 |
|---|---|---|
| `00-domain-list.md` | §一~§五 | 入口点/候选追踪/域清单(路径/设计决策/信号/置信度/方案) |
| `01-book-plan.md` | §五 分层 + §八 | 四层卷结构(池核心/拦截链/监控安全/支撑) + 教学顺序 |
| `02-approach-selection.md` | §五 方案列 | A=5 域(🔴), B=4 域(🟡, 叶子最低 B 不降级) |
| `knowledge-planning/{域}.md` | 待产出 (d1-*.md) | 每域 v5 管线第一步 (逐源提取→聚合→分类→聚类) |
| `outlines/d0{NN}-*/01-*.md` | 待产出 | 每域大纲 (四要素+结尾桥+密度 35-69 行) |
| `HANDOFF.md` | 阶段收尾 | 含 §零 状态 + 完成检查清单 |
