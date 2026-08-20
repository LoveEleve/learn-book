# S1-3 Bean 生命周期 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | doCreateBean 的 13 步具体是什么？第一步和最后一步分别做什么？ | §1.1 |
| 2 | @PostConstruct 和 afterPropertiesSet 谁先执行？ | §1.1 |
| 3 | `@Autowired` 字段注入在 doCreateBean 的哪一步？ | §1.2 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 4 | InstantiationAwareBeanPostProcessor 的 beforeInstantiation 返回非 null — 后续 13 步还执行吗？ | §2.1 |
| 5 | AOP 代理为什么在 AfterInitialization 生成而非 BeforeInitialization？ | §1.1 |
| 6 | 三层 BPP 的调用顺序为什么不能颠倒？ | §2.1-2.2 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 7 | getBean() 到 servlet 可用 Bean 之间发生了什么？ | §1.1 13步 |
| 8 | BeanPostProcessor 和 @PostConstruct 是什么关系？ | §2.2 |

## 覆盖: 8 问 / 3 身份 / 100%
