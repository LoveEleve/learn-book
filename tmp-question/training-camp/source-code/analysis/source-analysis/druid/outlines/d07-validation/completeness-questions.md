# D-7 连接验证 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 验证一次连接到底执行了什么 SQL? | §2 (execValidQuery: createStatement→executeQuery→rs.next) |
| 2 | testWhileIdle 什么条件才验? | §3 (idleMillis >= timeBetweenEvictionRunsMillis L1418) |
| 3 | testOnBorrow 失败会怎样? | §3 (discard + 重试取下一个 L1391) |
| 4 | 验证会过 Filter 链吗? | §2 (getConnectionRaw 裸连跳过, 快路径) |
| 5 | validationQuery 什么时候用? | §2 (checker 为 null 时兜底 L1478) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | 为什么按 driver 类名自动选 checker? | §1 (每种驱动最优验证方式不同, 零配置) |
| 7 | 为什么验证要裸连接? | §2 (跳过 filter 埋数, 验证自己不能成为开销) |
| 8 | 三时机怎么权衡? | §3 (严格度×开销三档: 借出/空闲/归还) |
| 9 | 与 Hikari H-7 的验证有什么不同? | §2 (Druid setQueryTimeout vs Hikari setNetworkTimeout; Druid SPI 多实现 vs Hikari isValid 单一) |
| 10 | 验证成功后 onFatalError 复位有什么意义? | §2 (数据库恢复的探针, 联动 shrink) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 11 | ValidConnectionChecker 有哪 6 个实现? | §1 (JDBC4/MySql/Oracle/MSSQL/PG/OceanBase) |
| 12 | MySQL 的 ping SQL 是什么? | §1 (`/* ping */ SELECT 1`) |
| 13 | execValidQuery 怎么判定有效? | §2 (rs.next() 有返回行) |
| 14 | 验证超时怎么兜底? | §2 (setQueryTimeout(validationQueryTimeout)) |
| 15 | 保活过的连接空闲时间怎么算? | §3 (lastActive/lastExec/lastKeep 三来源取最大 L1407-1416) |
| 16 | 系统时钟回拨会怎样? | §3 (负 idle 也触发验证 L1418-1419, 防御永久跳过探活) |
| 17 | OceanBase 用什么验证? | §1 (双模式: DUAL/ping 按 DbType 二选一) |

## 覆盖: 14 问 / 3 身份 / 100%
