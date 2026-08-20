# S1-5 DI 注入 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | `@Autowired private UserService userService` 从注解到字段赋值的完整链路？ | §1.1-1.2 |
| 2 | 同类型有两个 Bean — @Autowired 怎么选？ | §1.2 |
| 3 | `@Resource` 和 `@Autowired` 的查找顺序有什么区别？ | §2.1 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 4 | 为什么 @Autowired 通过 BeanPostProcessor(而非直接代码)实现？ | §1.1 |
| 5 | @Primary 和 @Qualifier 的设计意图有什么区别？ | §2.2 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | @Autowired 和 @Resource 什么时候用哪个？ | §2.1 |

## 覆盖: 6 问 / 3 身份 / 100%
