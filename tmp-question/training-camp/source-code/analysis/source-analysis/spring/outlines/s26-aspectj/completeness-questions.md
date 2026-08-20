# A-3 @AspectJ 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | @Aspect 类中的 @Before 方法是如何变成 Advisor 进入 proceed 链的？ | §1 |
| 2 | 多个 @Aspect 类匹配同一个方法—执行顺序由什么决定？ | §2 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 3 | ReflectiveAspectJAdvisorFactory 为什么用反射而非 AspectJ 编译期织入？ | §1 |
| 4 | AnnotationAwareAspectJAutoProxyCreator 继承了谁？和其他 AutoProxyCreator 关系？ | §2 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | Pointcut 表达式 execution(* com.example..*(..)) 的每个部分是什么意思？ | §2 |

## 覆盖: 5 问 / 3 身份 / 100%
