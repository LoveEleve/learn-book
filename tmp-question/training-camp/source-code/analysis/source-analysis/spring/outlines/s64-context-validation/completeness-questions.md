# C-22 Bean Validation 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | @Validated 类的方法怎么被校验的？ | §1 (BPP 织入 AOP) |
| 2 | 校验失败抛什么异常？ | §2 (ConstraintViolationException) |
| 3 | @Valid 和 @Validated 区别？ | §3 (Web 参数绑定 vs AOP 方法) |
| 4 | 自定义约束校验器能注入依赖吗？ | §3 (SpringConstraintValidatorFactory) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么用 AOP+BPP 而非手写校验？ | §1 关键设计 (复用 AOP/声明式) |
| 6 | 为什么参数和返回值都校验？ | §2 (契约双保险) |
| 7 | 约束校验器为什么由 Spring 创建？ | §3 (可注入依赖) |
| 8 | 分组校验怎么用？ | §2 (determineValidationGroups) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | 校验在方法调用前还是后？ | §2 (环绕: 参数前/返回值后) |
| 10 | @RequestBody @Valid 走哪条链？ | §3 (C-14 binder → C-13 异常) |

## 覆盖: 10 问 / 3 身份 / 100%
