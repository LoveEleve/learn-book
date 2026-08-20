# C-19 @Sql 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | @Sql 脚本/内联 SQL 什么时候执行？ | §2 (监听器 before/afterTestMethod) |
| 2 | 建表用哪个 phase？清理用哪个？ | §1 (BEFORE_CLASS / AFTER_METHOD) |
| 3 | 测试数据会不会污染库？ | §3 (transactionMode + @Transactional 回滚) |
| 4 | 类级和方法级 @Sql 怎么合并？ | §1 (SqlMergeMode) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么用 TestExecutionListener 而非 JUnit 扩展？ | §2 关键设计 (复用 TestContext) |
| 6 | 脚本为什么要解析分隔符/注释？ | §3 (切分多条 SQL) |
| 7 | 三种 transactionMode 的语义？ | §3 (无事务/事务内/独立事务) |
| 8 | @Sql 与 @Transactional 怎么配合？ | §3 (INFERRED 随事务回滚) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | 脚本执行出错会怎样？ | §3 (ScriptException) |
| 10 | @Sql 能访问到 DataSource 吗？ | §2 (retrieveDataSource) |

## 覆盖: 10 问 / 3 身份 / 100%
