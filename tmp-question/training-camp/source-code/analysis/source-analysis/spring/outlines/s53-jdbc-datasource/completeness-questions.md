# C-11 DataSource 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | DriverManagerDataSource 和 HikariDataSource 区别？ | §1 vs §2 (每次新建 vs 池化复用) |
| 2 | 生产为什么必须用连接池？ | §2 关键设计 (建连开销大) |
| 3 | 池的 maximumPoolSize/minimumIdle 什么意思？ | §2 (HikariConfig) |
| 4 | 读写分离怎么做？ | §3 (AbstractRoutingDataSource) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么业务代码只依赖 DataSource 接口？ | §1 + §3 (策略模式) |
| 6 | 池连接 close 后真的关闭了吗？ | §2 (归还而非物理关闭) |
| 7 | 路由源怎么和池叠加？ | §3 (路由包装池) |
| 8 | Hikari 为什么"构造即建池"还要 fastPathPool？ | §2 (配置驱动优化) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | DriverManager.getConnection 每次新建的代价？ | §1 (握手/认证) |
| 10 | AbstractDriverBasedDataSource 模板固定了什么？ | §1 (参数校验/取连接骨架) |

## 覆盖: 10 问 / 3 身份 / 100%
