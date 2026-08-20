# S1-7 FactoryBean 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | `@Autowired private UserMapper userMapper` — UserMapper 没有实现类，Spring 怎么注入的？ | §1.2 |
| 2 | `&userMapper` 和 `userMapper` 的 getBean 结果有什么区别？ | §1.3 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 3 | FactoryBean 为什么不设计成 BPP(BeanPostProcessor)？ | §1.1 |
| 4 | `getObjectForBeanInstance` 中的 `&` 前缀和 `isFactoryDereference` 的关系？ | §1.3 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | FactoryBean 和 BeanFactory 有什么区别？ | §1.1 |

## 覆盖: 5 问 / 3 身份 / 100%
