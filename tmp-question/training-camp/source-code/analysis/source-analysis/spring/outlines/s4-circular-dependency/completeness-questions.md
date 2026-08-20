# S1-4 循环依赖 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | `@Autowired public A(B b)` + `@Autowired public B(A a)` 为什么报错？ | §1.1 |
| 2 | `@Autowired private B b` (字段注入) + 同样 B 中字段注入 A — 为什么可以？ | §1.2 |
| 3 | getEarlyBeanReference 返回什么？和 AOP 代理什么关系？ | §1.3 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 4 | 如果只有二级缓存(去掉 singletonFactories) — 循环依赖还能解吗？ | §1.2 |
| 5 | addSingletonFactory 为什么放在 createBeanInstance 之后、populateBean 之前？ | §1.1-1.2 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | 循环依赖是什么？为什么会出现？ | §1.1-1.2 |
| 7 | Spring 对循环依赖的态度是什么？推荐吗？ | §1.1 |

## 覆盖: 7 问 / 3 身份 / 100%
