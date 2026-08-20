# S-26 Actuator Health 聚合 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 怎么加一个自定义健康源? | §1 (extends AbstractHealthIndicator + doHealthCheck) |
| 2 | 整体状态怎么算?怎么改规则? | §2 (management.health.status.order / 自定义 StatusAggregator) |
| 3 | liveness/readiness 探针端点哪来? | §3 (/livez /readyz) |
| 4 | 某些组件想不进 health? | §3 (include/exclude) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么用模板方法? | §1 (统一 Builder/异常/耗时) |
| 6 | 为什么聚合用策略接口? | §2 (聚合算法可替换) |
| 7 | 为什么严重度排序聚合? | §2 (最严重者胜出) |
| 8 | 为什么分组? | §3 (不同消费者不同视图) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | HealthIndicator 抽象是什么? | §1 (health() 返回 Health) |
| 10 | 一个 DOWN 为什么整体 DOWN? | §2 (order 严重度) |
| 11 | probes 与 S-19 可用性什么关系? | §3 (LivenessState/ReadinessState→探针) |

## 覆盖: 11 问 / 3 身份 / 100%
