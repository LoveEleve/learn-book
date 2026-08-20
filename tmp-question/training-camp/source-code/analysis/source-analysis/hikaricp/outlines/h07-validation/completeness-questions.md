# H-7 连接验证 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 池怎么确认连接活着? | §2 (isValid 或 connectionTestQuery) |
| 2 | 怎么配自定义测试 SQL? | §3 (connectionTestQuery) |
| 3 | 验证会拖慢/卡死吗? | §1 (validationTimeout 超时保护) |
| 4 | 验证污染连接吗? | §3 (restore + rollback) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么 setNetworkTimeout(validationTimeout)? | §1 (限时防卡死) |
| 6 | 为什么自动选 isValid? | §2 (未配 test query 用 JDBC4) |
| 7 | 为什么 finally 恢复? | §3 (防超时错误泄漏) |
| 8 | 为什么隔离内部查询? | §3 (测试 SQL 不污染事务) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | 验证的入口在哪? | §1 (isConnectionDead) |
| 10 | 两种验证方式? | §2 (isValid vs 测试查询) |
| 11 | isUseJdbc4Validation 怎么定? | §2 (testQuery==null) |

## 覆盖: 11 问 / 3 身份 / 100%
