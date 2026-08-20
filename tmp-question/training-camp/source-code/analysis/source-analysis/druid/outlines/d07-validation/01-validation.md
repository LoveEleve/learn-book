# D-7 连接验证 — ValidConnectionChecker SPI + 三时机

> 前置: [[D-1-core-architecture]] (三调用点) | 复用: [[H-7-validation]] (验证策略对照) | 引出: [[D-5-maintenance]] (shrink keepAlive)
> 🟡 Working | 8 KP | [模式: SPI + 策略]
> Pass 2 闭环: q1(三保险选择) q2(统计隔离) q3(阈值梯度+恢复探针) q4(空闲基准三来源) q5(负 idle 防御) q6(各库验证 SQL 族)
**读者处境**: `testWhileIdle: true`、`validationQuery: SELECT 1` 是 Druid 经典配置 — 但你知道验证请求**不走监控**吗?为什么 MySQL 用 `/* ping */ SELECT 1` 而 Oracle 用 `SELECT 'x' FROM DUAL`?三个时机怎么权衡?这篇拆开 SPI 选择、裸连执行和三时机梯度。
### 1. SPI 与选择 — 三保险: 类名匹配 → 用户覆盖 → 兜底

场景: 不同数据库验证方式完全不同, 用户不配置时怎么选对?

源码路径:
- `ValidConnectionChecker.java:21,28` — **SPI**: `isValidConnection(Connection c, String query, int validationQueryTimeout)`(L21-22)+`configFromProperties`(L28)
- `JDBC4ValidConnectionChecker.java:25` — **默认**: `conn.isValid(validationQueryTimeout)`(L25) — JDBC4 原生 API
- `DruidDataSource.java:1240,1245,1246` — **选择**: `initValidConnectionChecker()`(L1240): 用户已 set 则尊重(L1241)→`driver.getClass().getName()`(L1245)→MySQL 用 `isMySqlDriver` 宽松匹配(L1246, 驱动变体多), Oracle/MSSQL/PG/OceanBase 精确匹配(L1249-1268)
- `vendor/MySqlValidConnectionChecker.java:44` — **ping**: `usePingMethod` 默认 true → `/* ping */ SELECT 1`; `druid.mysql.usePingMethod=false` 可关(L44-49)
- `DruidAbstractDataSource.java:1475` — **兜底**: 无 checker 时 `execValidQuery(conn, validationQuery, timeout)`(L1478) — 用户配的 validationQuery

关键设计: **Why 类名匹配而非配置/SPI？**(闭环 q1): 验证方式与驱动强绑定(MySQL ping/Oracle DUAL), 类名匹配**零配置选对**; 三保险: ①自动匹配 ②用户 `setValidConnectionChecker` 覆盖 ③都不满足 → validationQuery 兜底。[模式: SPI + 默认正确]

数据流: init(D-1) → initValidConnectionChecker(1240) → 用户已配?用用户的 → 按类名匹配(L1246-1268) → 不匹配保持 null → 验证时走兜底(L1478)。

### 2. 执行 — 裸连快路径与统计隔离

场景: 验证到底执行什么?为什么说验证"不进监控"?

源码路径:
- `ValidConnectionCheckerAdapter.java:40,47,66` — **裸连执行**: `execValidQuery()`(L40): `getConnectionRaw()` 取**裸连接**(L47, 跳过 filter 链)→`setQueryTimeout(validationQueryTimeout)`(L66)→`executeQuery(query)`(L68)→`rs.next()`(L69) 判定
- `DruidAbstractDataSource.java:1434,1436,1444` — **双路径**: `validateConnection()`(L1434): 连接已关检查(L1436)→checker 非空走 `isValidConnection`(L1444); 无 checker 走 execValidQuery(L1478)

关键设计: **Why 裸连接？**(闭环 q2): 验证是池内部探活, 若走 filter 链, 每条 `SELECT 1` 都会被 StatFilter 建 JdbcSqlStat 并计入执行统计 — 空闲期高频验证会把统计表刷屏、慢 SQL 判定失真; 裸连实现**统计隔离**: 验证不产生业务统计, 业务统计不被验证稀释。[模式: 快路径旁路 + 统计隔离]

数据流: 调用方(借出/归还/shrink) → validateConnection(1434) → checker 有: isValidConnection(1444) → MySQL ping SQL; 无: execValidQuery(1478) → getConnectionRaw(47) → executeQuery(68) → rs.next(69) → true/false。

### 3. 三时机与阈值梯度 — 严格度 × 开销

场景: 借出时验最保险但最贵 — 三个时机怎么配?和 shrink 怎么衔接?

源码路径:
- `DruidDataSource.java:1385` — **testOnBorrow**: 借出同步验(L1385), 失败 discard 换一个(L1391-1392) — 最严格最贵
- `DruidDataSource.java:1407,1412,1416,1418` — **testWhileIdle**: **空闲基准三来源取最大**(L1407-1416): lastActive 默认/lastExec(checkExecuteTime 时)/lastKeep(保活时间更新时) → `idleMillis >= timeBetweenEvictionRunsMillis || idleMillis < 0` 才验(L1418-1419) — **负 idle 是时钟回拨防御**(闭环 q4/q5)
- `DruidDataSource.java:1980` — **testOnReturn**: 归还验(L1980), 失败直接关闭(L1982-1996) — 坏连接不进池
- `DruidAbstractDataSource.java:1449` — **恢复探针**: 验证成功且 `onFatalError` → 复位(L1449-1458) — 探活成功宣告数据库恢复, 与 D-5 fatalError 驱逐对称闭环
- 默认: **testWhileIdle=true**(Abstract L76, 出厂即开), testOnBorrow/Return=false(L74-75) — 轻量抽查默认, 重量验证显式开

关键设计: **Why 以维护周期为阈值？**(闭环 q3): 三阈值构成梯度 — testWhileIdle(timeBetweenEvictionRunsMillis, 60s 级) < keepAliveBetweenTimeMillis(2min 级) < minEvictableIdleTimeMillis(30min 级): 借出抽查(业务路径) vs shrink 保活(维护路径) 覆盖同一批闲置连接但语义不同, 叠加生效; 阈值顺序由配置校验强制(L738-740)。[模式: 策略档位]

数据流: 借出: getConnectionDirect(D-1) → testOnBorrow 开: testConnectionInternal(1385) → 失败 discard+重试。归还: recycle(D-1) → testOnReturn 开: test(1980) → 失败关闭。空闲: shrink(D-5) keepAlive 段 validateConnection → 失败驱逐; 成功且 onFatalError → 复位(1449)。

→ 引出 D-5: 维护体系 — shrink keepAlive 段对闲置连接调用 validateConnection, 失败驱逐, onFatalError 复位联动 (导航指针)。
