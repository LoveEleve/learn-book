# C-17 TestContext 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | @SpringBootTest 里 @Autowired 怎么注入的？ | §3 (DependencyInjectionTestExecutionListener) |
| 2 | 为什么多个测试类共享一个容器(启动快)？ | §2 (ContextCache 缓存) |
| 3 | @Transactional 测试怎么自动回滚？ | §3 (TransactionalTestExecutionListener) |
| 4 | 怎么强制测试重新加载容器？ | §3 (@DirtiesContext) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么用门面 + 监听器分发？ | §1 关键设计 (生命周期固定/行为可插拔) |
| 6 | 缓存键(MergedContextConfiguration)由什么决定？ | §2 (classes/profiles/loader) |
| 7 | 缓存正确性怎么保证？ | §2 (键完整性) |
| 8 | 注入/事务为什么做成监听器？ | §3 (解耦+可配) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | 测试方法执行前后框架做什么？ | §1 (beforeTestMethod/afterTestMethod) |
| 10 | 容器什么时候真正启动？ | §2 (首次 getApplicationContext 懒加载) |

## 覆盖: 10 问 / 3 身份 / 100%
