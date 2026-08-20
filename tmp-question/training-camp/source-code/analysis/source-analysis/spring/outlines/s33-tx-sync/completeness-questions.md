# T-5 TransactionSynchronization 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | afterCommit 和 afterCompletion 的区别是什么？什么时候用哪个？ | §1 |
| 2 | rollback 时 beforeCommit/afterCommit 会执行吗？ | §1 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 3 | @TransactionalEventListener 和 TransactionSynchronization 的关系？ | §1 |
| 4 | 为什么 TransactionSynchronizationManager 用 ThreadLocal 管理？ | §1 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 事务提交后发 MQ 消息 — 应该用 afterCommit 还是 afterCompletion？ | §1 |

## 覆盖: 5 问 / 3 身份 / 100%
