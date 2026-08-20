# T-1 @Transactional 链路 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | @Transactional 方法内调 Repository — Spring 怎么保证复用同一个 Connection？ | §2 |
| 2 | TransactionManager 有多个(主库/备库) → @Transactional 怎么选择用哪个？ | §1 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 3 | TransactionAttributeSource 和 TransactionManager 为什么要分离？ | §1 |
| 4 | DataSourceTransactionManager.doBegin 做了 setAutoCommit(false) — 为什么不在 getConnection 时就设置？ | §2 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | @Transactional 和手动 conn.setAutoCommit(false) + conn.commit() 有什么区别？ | §1-2 |

## 覆盖: 5 问 / 3 身份 / 100%
