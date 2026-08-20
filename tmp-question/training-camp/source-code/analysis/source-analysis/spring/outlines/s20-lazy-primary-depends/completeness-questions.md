# S2-13 @Lazy/@Primary/@DependsOn 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | prototype Bean 需要 @Lazy 吗？为什么？ | §1 |
| 2 | @Primary 和 @Qualifier 都标在了两个Bean上 — 哪个生效？ | §2 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 3 | @DependsOn 和 @Autowired 的排序机制有什么本质区别？什么时候用哪个？ | §3 |
| 4 | @Primary 是第一层tiebreaker — 为什么不放在最后一层？ | §2 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | @Lazy 的 Bean 和普通 Bean 的 @PostConstruct 什么时候执行？ | §1 |

## 覆盖: 5 问 / 3 身份 / 100%
