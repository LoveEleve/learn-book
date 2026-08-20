# S1-2 BeanFactory 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | `ListableBeanFactory` 和 `HierarchicalBeanFactory` 各加了什么能力？ | §1.1 |
| 2 | `getBean()` 三级缓存是什么？各存储什么状态的 Bean？ | §2.1 |
| 3 | Singleton 和 Prototype 的 `getBean()` 有什么区别？ | §2.2 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 4 | 为什么 5 层接口继承而非一个 `ApplicationContext` 大接口？ | §1.1 |
| 5 | Spring Boot Mvc 的父子容器为什么是单向依赖？ | §1.2 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | `ApplicationContext` 和 `BeanFactory` 的关系是什么？ | §1.1 |
| 7 | `getBean("userService")` 内部发生了哪些步骤？ | §2.2 |

## 覆盖: 7 问 / 3 身份 / 100%
