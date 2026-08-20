# S-23 Validation 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 怎么让方法校验生效? | §1/§3 (加 starter + @Validated) |
| 2 | 没装 Hibernate Validator 会怎样? | §1 (@ConditionalOnResource SPI 不命中则不装配) |
| 3 | 错误消息怎么国际化? | §2 (MessageInterpolatorFactory) |
| 4 | 怎么跳过某些类的校验? | §3 (MethodValidationExcludeFilter) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么 @ConditionalOnResource 而非只查类? | §1 (JSR-303 是 SPI 发现实现) |
| 6 | 为什么自动校验器设 primary? | §2 (LocalValidatorFactoryBean 成为默认) |
| 7 | 与 C-22 的增量是什么? | §3 (Filtered 版 + 属性可调) |
| 8 | 用户自定义 Validator 怎么覆盖? | §2 (@ConditionalOnMissingBean) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | ValidationAutoConfiguration 做什么? | §1 (自动注册校验 Bean) |
| 10 | defaultValidator 是什么类型? | §2 (LocalValidatorFactoryBean) |
| 11 | proxy-target-class 作用? | §3 (是否 CGLIB 代理) |

## 覆盖: 11 问 / 3 身份 / 100%
