# S-12 事务自动配置全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 没写 @EnableTransactionManagement 为什么 @Transactional 有效? | §1 (自动启用) |
| 2 | DataSourceTransactionManager 谁建的? | §2 (自动装配创建) |
| 3 | 怎么统一定制事务管理器? | §3 (Customizers) |
| 4 | 自定义事务管理器怎么覆盖? | §2 (@ConditionalOnMissingBean) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 自动启用的条件为什么是 @ConditionalOnMissingBean? | §1 (用户显式启用让位) |
| 6 | JdbcTransactionManager vs DataSourceTransactionManager 怎么选? | §2 (环境条件) |
| 7 | 为什么用 Customizer 模式? | §3 (横切定制, 与 S-8 同构) |
| 8 | 与 s29-s33 的边界? | §3 (机制在 s29, 装配在本域) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | 自动启用的机制是什么? | §1 (导入 s29 事务基础设施) |
| 10 | spring.transaction.* 谁绑定? | §3 (TransactionProperties, S-5) |

## 覆盖: 10 问 / 3 身份 / 100%
