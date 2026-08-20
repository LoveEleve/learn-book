# C-7 TaskExecutor 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | @Async 的方法跑在哪个线程？怎么配置线程池？ | §2 (ThreadPoolTaskExecutor + @Async 衔接) |
| 2 | corePoolSize/maxPoolSize/queueCapacity 怎么配合？ | §2 (JDK 线程池语义) |
| 3 | 测试环境怎么让异步变同步？ | §1 (SyncTaskExecutor) + §3 (选型) |
| 4 | 已有 JDK 线程池怎么接入 Spring？ | §3 (ConcurrentTaskExecutor 适配器) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么抽象 TaskExecutor 而不是直接用 ExecutorService？ | §1 关键设计 (单一抽象可替换) |
| 6 | 为什么 ThreadPoolTaskExecutor 要包一层 JDK 线程池？ | §2 关键设计 (Bean 化/钩子/懒初始化) |
| 7 | SimpleAsyncTaskExecutor 的适用边界？ | §1 (每任务新线程) + §2 关键设计 (线程爆炸) |
| 8 | 队列默认无界(MAX_VALUE)意味着什么？ | §2 (队列满才会扩容到 max) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | 线程池什么时候创建？ | §2 (首次 execute 懒创建) |
| 10 | execute(Runnable) 一个方法怎么支撑三种策略？ | §1 (策略差异在实现) |

## 覆盖: 10 问 / 3 身份 / 100%
