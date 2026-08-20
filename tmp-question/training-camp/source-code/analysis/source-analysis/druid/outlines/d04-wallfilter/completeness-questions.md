# D-4 WallFilter 防火墙 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 防火墙怎么抢在 SQL 执行前? | §1 (override statement_execute, check 在 chain 之前 L485) |
| 2 | 违规会以什么形式给业务? | §4 (SQLException, WallFilter L497) |
| 3 | 拦截规则在哪配置? | §3 (WallConfig: multiStatementAllow/commentAllow 等) |
| 4 | 白名单命中怎么知道? | §2 (getWhiteSql L630, 精确/正则匹配) |
| 5 | 被拦的 SQL 记录在哪? | §4 (addBlackSql 黑名单库 L560) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | 为什么防火墙必须 override 而非模板钩子? | §1 (需要"拒绝执行"能力, 钩子拿不到) |
| 7 | 为什么白/黑名单能绕过解析? | §2 (同 SQL 判定可缓存, Map 查找 vs AST 解析) |
| 8 | 为什么规则全进配置? | §3 (安全策略因业务而异) |
| 9 | 为什么按库独立 Visitor? | §3 (各库函数/语法差异, MySQL OUTFILE vs Oracle UTL_*) |
| 10 | 与 StatFilter 的拦截风格差异? | §1 (前置守卫 vs 后置埋数, D-2 §4 两种风格) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 11 | 检查链路分几步? | §2 (特权→白名单→AST 解析→Visitor→违规) |
| 12 | 默认禁什么? | §3 (多语句/注释禁, hint/strictSyntax 允许) |
| 13 | 方言 Provider 有哪些? | §3 (MySql/Oracle/PG/DB2/SQLServer/SQLite/CK+SPI) |
| 14 | 检查计数有哪些? | §4 (checkCount/hardCheckCount/whiteListHitCount/violationCount) |
| 15 | 白名单用什么数据结构? | §2 (ConcurrentLruCache 1024 上限) |
| 16 | 白名单 key 是什么? | §2 (参数化 SQL — id=123 与 id=456 同条目) |

## 覆盖: 14 问 / 3 身份 / 100%
