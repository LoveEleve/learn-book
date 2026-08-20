# D-7 连接验证 — ValidConnectionChecker SPI + 三时机

> 项目: Druid (JDBC 连接池) | 🟡 Working / 1 篇 | ValidConnectionChecker(29)+JDBC4(41)+MySql+Oracle+MSSQL+PG+OceanBase(6 实现)+ValidConnectionCheckerAdapter
> 基线: DRUID-PLAN D-7 (连接验证) — 前置: **D-1(调用点: testOnBorrow L1385/testWhileIdle L1421/testOnReturn L1980, 已分析) + 对照 H-7(已验证, 双向)** — 展开 SPI+三时机; 叶子域最低 B 方案

---

## §0.8

- 🟡 Working，1篇 — SPI 接口(**ValidConnectionChecker.isValidConnection(c, query, timeout)**[L21-22]) → 6 实现(**JDBC4 默认 conn.isValid(validationQueryTimeout)**[JDBC4ValidConnectionChecker:25]/**MySql usePingMethod 默认 true, `/* ping */ SELECT 1`**/Oracle DUAL/MSSQL/PG/OceanBase — **initValidConnectionChecker 按 driver 类名匹配选择**[DruidDataSource L1240-1270]) → 双路径(**testConnectionInternal→checker.isValidConnection** / **validateConnection: checker 优先, 无则 ValidConnectionCheckerAdapter.execValidQuery** [Abstract L1434-1504]) → **execValidQuery 快路径: raw connection 跳过 filter 链** (getConnectionRaw→createStatement→executeQuery→rs.next() 判定, validationQueryTimeout→setQueryTimeout) → 三时机(**testOnBorrow 借出同步验**[L1385]/**testWhileIdle 空闲超 timeBetweenEvictionRunsMillis 验**[L1421]/**testOnReturn 归还验**[L1980]) → **onFatalError 复位联动** (validateConnection 成功则 onFatalError=false [L1449/1493]) → ExceptionSorter 旁路(厂商异常分类, 一句话)
- 设计模式: [模式: SPI]—checker 按驱动选择; [模式: 策略]—三时机可配

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| ValidConnectionChecker.java:21 | SPI | **接口: isValidConnection(c, query, timeout)(L21-22)+configFromProperties(L28)** | High |
| JDBC4ValidConnectionChecker.java:25 | 默认实现 | **JDBC4: conn.isValid(validationQueryTimeout)** — 驱动原生 isValid 优先 | High |
| vendor/MySqlValidConnectionChecker.java | ping 实现 | **usePingMethod 默认 true → DEFAULT_VALIDATION_QUERY = `/* ping */ SELECT 1`**; 可配 druid.mysql.usePingMethod | High |
| DruidDataSource.java:1240,1246 | 选择逻辑 | **initValidConnectionChecker(L1240): 按 driver.getClass().getName() 匹配 MySql/Oracle/MSSQL/PG/OceanBase(L1246-1268)** | High |
| ValidConnectionCheckerAdapter.java:40 | 兜底执行 | **execValidQuery(L40): getConnectionRaw()(L47) 取裸连→createStatement→executeQuery(L68)→rs.next()(L69) 判定; timeout>0 setQueryTimeout(L66)** | High |
| DruidAbstractDataSource.java:1434 | 双路径 | **validateConnection(L1434): conn.isClosed 检查→checker 存在则 isValidConnection(L1444), 无 checker 则 execValidQuery(validationQuery)(L1478)** | High |
| DruidAbstractDataSource.java:1449,1493 | 联动 | **验证成功且 onFatalError → onFatalError=false(L1449-1458/L1493-1502)** — 故障恢复标志复位 | High |
| DruidDataSource.java:1385,1421,1980 | 三时机 | **testOnBorrow(L1385)/testWhileIdle(L1421, 空闲≥timeBetweenEvictionRunsMillis)/testOnReturn(L1980)** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 验证是单一薄机制 — 1篇 (~42行) 按"SPI 与实现→双路径→三时机→联动"展开; 调用点(借/还)引用 D-1(已分析), 维护调用(shrink keepAlive)对 D-5 导航。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | SPI 接口与 6 实现 (JDBC4/MySql/Oracle/MSSQL/PG/OceanBase) | 🟡 | **为什么🟡**: 驱动差异适配 |
| P1-2 | 按 driver 类名选择 checker (initValidConnectionChecker) | 🟡 | **为什么🟡**: 自动匹配逻辑 |
| P1-3 | 双路径 (checker vs validationQuery 兜底) | 🟡 | **为什么🟡**: 兜底策略 |
| P1-4 | execValidQuery 裸连快路径 | 🟡 | **为什么🟡**: 验证性能设计 |
| P1-5 | 三时机 (Borrow/WhileIdle/Return) | 🟡 | **为什么🟡**: 时机取舍 |
| P2-1 | onFatalError 复位联动 | 🟢 | **为什么🟢**: 故障恢复闭环 |
| P2-2 | 与 Hikari H-7 (isValid+setNetworkTimeout) 对照 | 🟢 | **为什么🟢**: 两种验证哲学 |
| P3-1 | ExceptionSorter 旁路 (厂商异常分类) | 🟢 | **为什么🟢**: 异常侧配套 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **SPI 与实现** | 🟡 | 适配层 |
| B | **双路径+裸连快路径** | 🟡 | 执行层 |
| C | **三时机+联动** | 🟡 | 策略层 |

> **Cluster A (§1)**: 接口+6 实现+driver 匹配
> **Cluster B (§2)**: testConnectionInternal/validateConnection 双路径+execValidQuery
> **Cluster C (§3)**: 三时机+onFatalError 复位+ExceptionSorter 旁路

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 三保险选择 | 类名自动匹配(MySQL 宽松/其余精确) → 用户 setValidConnectionChecker 覆盖 → validationQuery 兜底, 零配置选对 | DruidDataSource.java:1240-1270, vendor/MySqlValidConnectionChecker.java:44-49 |
| q2 | 统计隔离 | 验证走裸连(getConnectionRaw)跳过 filter 链 — 探活不污染业务 SQL 统计, 统计不被验证稀释 | ValidConnectionCheckerAdapter.java:40-70 |
| q3 | 阈值梯度 | testWhileIdle(60s 级) < keepAlive(2min 级) < minEvictable(30min 级): 借出抽查 vs 后台保活叠加生效; onFatalError 验证成功即复位=恢复探针 | DruidDataSource.java:1400-1431, 3121-3133, Abstract:1449-1458 |
| q4 | 空闲基准三来源 | **lastActive/lastExec(checkExecuteTime)/lastKeep 取最大** — keepAlive 验证后连接"最近活动"应算保活时间, 否则保活过的连接会被误判空闲提前验证/驱逐 | DruidDataSource.java:1407-1416 |
| q5 | 负 idle 防御 | `idleMillis < 0` 也触发验证 — 系统时钟回拨(NTP)导致空闲时间变负时, 不验证会永久跳过探活, 强制验证兜底 | DruidDataSource.java:1418-1419 |
| q6 | 各库验证 SQL 族 | Oracle `SELECT 'x' FROM DUAL`/PG `SELECT 'x'`/MSSQL `SELECT 1`/OceanBase 双模式(DUAL+ping 按 DbType 二选一) — 最轻量验证语句各库不同 | vendor/OracleValidConnectionChecker.java:31, OceanBaseValidConnectionChecker.java:26-34 |

→ 引出 D-5: 维护体系 — shrink keepAlive 段调用 validateConnection 验证闲置连接, fatalError 联动驱逐 (导航指针)
