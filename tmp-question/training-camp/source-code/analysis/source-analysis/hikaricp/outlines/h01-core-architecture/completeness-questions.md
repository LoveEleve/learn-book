# H-1 核心架构 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 注入的 DataSource 是什么类? | §1/§3 (HikariDataSource) |
| 2 | getConnection 第一次调用和之后差在哪? | §1 (首次懒建池) |
| 3 | 连接存哪? | §2 (ConcurrentBag) |
| 4 | 池靠什么"快"? | §2 (无锁并发容器) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么懒加载? | §1 (避免白建池/尽早暴露错误) |
| 6 | 为什么 extends HikariConfig? | §1 (一个对象兼配置+门面) |
| 7 | 为什么用 ConcurrentBag 而非阻塞队列? | §2 (无锁减少竞争) |
| 8 | 与 S-10 Boot DataSource 什么关系? | §3 (池化内核深入) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | 四层架构是什么? | §3 (Config→DataSource→Pool→Bag→Entry→Base) |
| 10 | HikariPool 内部有什么? | §2 (ConcurrentBag + HouseKeeper) |
| 11 | PoolBase 做什么? | §3 (连接创建/验证) |

## 覆盖: 11 问 / 3 身份 / 100%
