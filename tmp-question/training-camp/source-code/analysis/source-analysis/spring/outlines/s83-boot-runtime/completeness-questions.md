# S-19 启动运行时 (可用性 + 虚拟线程) 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | K8s 探针数据从哪来? | 篇①§3 (ApplicationAvailabilityBean 缓存) |
| 2 | 怎么开虚拟线程?默认为什么关? | 篇②§1 (spring.threads.virtual.enabled + Java21) |
| 3 | 开启后 @Async 用什么执行器? | 篇②§2 (SimpleAsyncTaskExecutor virtualThreads) |
| 4 | 启动失败时状态会怎样? | 篇①§2 (failed→ApplicationFailedEvent, 无就绪翻转) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么 Liveness/Readiness 分开? | 篇①§1 (kill vs 摘流量正交) |
| 6 | 为什么用事件驱动状态而非直接 set? | 篇①§2 (多监听者可响应) |
| 7 | 为什么虚拟线程不池化? | 篇②§3 (虚拟线程廉价, 池化多余) |
| 8 | 为什么用条件注解互斥双 Bean? | 篇②§2 (与 Boot 条件引擎统一, 二选一) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | ApplicationReadyEvent 什么时候发? | 篇①§2 (ready, 容器刷新后) |
| 10 | 为什么属性+Java版本双条件? | 篇②§1 (虚拟线程是 Java21 特性) |
| 11 | 状态存在哪、怎么查? | 篇①§3 (ConcurrentHashMap 缓存) |

## 覆盖: 11 问 / 3 身份 / 100%
