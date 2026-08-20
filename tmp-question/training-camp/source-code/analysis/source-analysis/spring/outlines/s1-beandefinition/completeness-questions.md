# S1-1 BeanDefinition 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | `@Component`/`@Bean`/XML `<bean>` 三种配置方式最终变成什么数据结构？ | §1.1 |
| 2 | `@Scope("prototype")` 对应的 BeanDefinition 字段是什么？ | §1.2 |
| 3 | `@Bean(initMethod="init")` 的 init 方法是怎么在 BeanDefinition 中存储的？ | §1.3 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 4 | 为什么 Spring 用 BeanDefinition 接口统一三种配置来源？ | §1.1 |
| 5 | RootBeanDefinition 和 GenericBeanDefinition 的最大区别是什么？ | §2.1 |
| 6 | 为什么每个 Bean 最终都变成 RootBeanDefinition — 即使没有 XML parent？ | §2.2 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 7 | BeanDefinition 的 scope/singleton/prototype 分别是什么意思？ | §1.2 |
| 8 | ROLE_APPLICATION vs ROLE_INFRASTRUCTURE 的区别是什么？ | §1.1 |

## 覆盖: 8 问 / 3 身份 / 100%
