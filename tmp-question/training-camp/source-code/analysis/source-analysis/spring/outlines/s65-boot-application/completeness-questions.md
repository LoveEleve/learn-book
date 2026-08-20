# S-1 @SpringBootApplication 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | @SpringBootApplication 一个注解背后是什么？ | §1 (三层组合拆解) |
| 2 | exclude 和 scanBasePackages 分别转发给谁？ | §1 (属性转发) |
| 3 | 自动装配类怎么避免被组件扫描重复注册？ | §3 (AutoConfigurationExcludeFilter) |
| 4 | 主类包怎么被记录的？ | §2 (AutoConfigurationPackages.Registrar) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么三个注解合成一个？ | §1 关键设计 (约定优于配置) |
| 6 | 为什么记录主类包？ | §2 (自动装配范围判断) |
| 7 | 为什么需要两个排除过滤器？ | §3 (防重复 + 扩展点) |
| 8 | @SpringBootConfiguration 与普通 @Configuration 差异？ | §1 (@Indexed 可被索引) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | 组合注解的机制是什么？ | §1 (C-5 元注解体系) |
| 10 | @Import(ImportSelector) 引向哪里？ | §2 (S-2 自动装配加载) |

## 覆盖: 10 问 / 3 身份 / 100%
