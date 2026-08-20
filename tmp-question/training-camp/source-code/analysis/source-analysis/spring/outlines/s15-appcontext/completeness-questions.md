# S2-8 AppContext 三大实现 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | AnnotationConfigApplicationContext 的 refreshBeanFactory 为什么是空方法？ | §1 |
| 2 | register(annotatedClass) 和 scan(packages) 的区别是什么？什么时候用哪个？ | §2 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 3 | 为什么 GenericApplicationContext 在构造器中创建 BeanFactory 而非在 refresh 时？这样设计有什么代价？ | §1 |
| 4 | reader + scanner 为什么要分离？如果合为一个组件会怎样？ | §2 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | new AnnotationConfigApplicationContext(AppConfig.class) 和 new GenericApplicationContext() + reader.register(AppConfig) 有什么区别？ | §1-2 |

## 覆盖: 5 问 / 3 身份 / 100%
