# S-28 Spring Data 仓库自动注册 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 仓库接口怎么自动注册的? | §1 (Registrar 注册 BeanDefinition) |
| 2 | 怎么切 reactive 仓库? | §2 (spring.data.*.repositories.type=reactive) |
| 3 | 怎么加自定义仓库? | 边界 (Spring Data 层) |
| 4 | 为什么不用 @Enable*Repositories? | §1 (Boot 自动导入) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么 ImportBeanDefinitionRegistrar? | §1 (接口需动态生成实现) |
| 6 | 为什么 AUTO/IMPERATIVE/REACTIVE 三态? | §2 (按 web 类型自动选/显式配) |
| 7 | 为什么每存储一个 Registrar? | §3 (复用基类, 新增便宜) |
| 8 | JPA 仓库为何降级? | §3 (现代主流 MyBatis/MP) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | 仓库 BeanDefinition 谁注册? | §1 (AbstractRepositoryConfigurationSourceSupport) |
| 10 | RepositoryType 有哪些? | §2 (AUTO/IMPERATIVE/REACTIVE) |
| 11 | 各存储 registrar 关系? | §3 (继承基类) |

## 覆盖: 11 问 / 3 身份 / 100%
