# T-2 8种失效场景 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | this.method() 调用和 method() 调用从 Controller 调 — 事务为什么前者不生效后者生效？ | §1 |
| 2 | @Transactional 没指定 rollbackFor — IOException 抛了事务回滚吗？ | §2 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 3 | 多数据源环境 — @Transactional 不指定 qualifier — Spring 怎么选择 TransactionManager？ | §3 |
| 4 | 为什么 Spring 默认不回滚 checked Exception？这是设计缺陷还是有意为之？ | §2 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 怎么快速判断自己的 @Transactional 是否生效？ | §1 |

## 覆盖: 5 问 / 3 身份 / 100%
