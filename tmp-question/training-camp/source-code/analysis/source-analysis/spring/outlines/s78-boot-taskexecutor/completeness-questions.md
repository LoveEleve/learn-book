# S-14 TaskExecutor 自动配置全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | @Async 默认执行器是什么? | §1 (applicationTaskExecutor) |
| 2 | 虚拟线程怎么开? | §2 (spring.threads.virtual.enabled) |
| 3 | 线程池参数在哪配? | §3 (spring.task.execution.*) |
| 4 | 自定义执行器怎么覆盖? | §3 (@ConditionalOnMissingBean) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么用约定 bean 名? | §1 (自动装配与使用解耦) |
| 6 | 为什么 @ConditionalOnThreading? | §2 (虚拟线程开关) |
| 7 | 为什么用 Builder? | §3 (属性→线程池映射) |
| 8 | 与 C-7 边界? | §3 (机制在 C-7) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | 虚拟模式用什么执行器? | §2 (SimpleAsyncTaskExecutor) |
| 10 | @Async 怎么找到执行器? | §3 (AsyncAnnotationPostProcessor) |

## 覆盖: 10 问 / 3 身份 / 100%
