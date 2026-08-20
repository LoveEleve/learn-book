# S2-11 @Scheduled 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | fixedRate 和 fixedDelay 有什么区别？如果任务执行超过间隔会怎样？ | §2 |
| 2 | @Scheduled 注解中 cron、fixedDelay、fixedRate 可以同时设置吗？ | §2 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 3 | 为什么 ScheduledAnnotationBPP 是 SmartInitializingSingleton 而非直接用 postProcessAfterInitialization？ | §1 |
| 4 | ScheduledTaskRegistrar 为什么需要 afterPropertiesSet 延迟注册？ | §3 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | @Scheduled 和 cron job 系统(如 XXL-Job)有什么区别？ | §2-3 |

## 覆盖: 5 问 / 3 身份 / 100%
