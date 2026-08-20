# A-4 自动代理 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | wrapIfNecessary 为什么有三个调用入口？各自在哪触发？ | §1 |
| 2 | BeanNameAutoProxyCreator 和 @AspectJ 注解 — 谁的代理优先级更高？ | §2 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 3 | AbstractAutoProxyCreator 的继承链是什么？AspectJAwareAdvisorAutoProxyCreator 继承了几层？ | §2 |
| 4 | @Async 和 @Transactional 在同一个 Bean 上 — 代理怎么创建？ | §1-2 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | getAdvicesAndAdvisorsForBean 返回 empty 是什么意思？Bean 就没有代理了吗？ | §1 |

## 覆盖: 5 问 / 3 身份 / 100%
