# C-9 ApplicationRunner 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | ApplicationRunner 和 CommandLineRunner 区别？ | §1 (结构化参数 vs 原始数组) |
| 2 | 多个 Runner 怎么控制执行顺序？ | §2 (getOrderComparator @Order) |
| 3 | Runner 的 run() 在启动流程哪个位置执行？ | §1 (refreshContext 之后 L325) |
| 4 | 命令行 --key=value 怎么在 Runner 里读？ | §2 (getOptionValues) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么 Runner 在 refreshContext 之后而非之前？ | §1 关键设计 (容器就绪) |
| 6 | 为什么用 getBeanNamesForType + IdentityHashMap 排序？ | §2 关键设计 (FactoryBean/防重复实例化) |
| 7 | @PostConstruct / SmartInitializingSingleton / ApplicationRunner 时机差异？ | §3 (回调顺序表) |
| 8 | 启动时做 X 用哪个回调的决策依据？ | §3 (越晚越安全但越晚) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | Runner 标记接口 + instanceof 双分派是怎么设计的？ | §2 (callRunner L782) |
| 10 | 启动失败的异常怎么被包装？ | §2 (IllegalStateException) |

## 覆盖: 10 问 / 3 身份 / 100%
