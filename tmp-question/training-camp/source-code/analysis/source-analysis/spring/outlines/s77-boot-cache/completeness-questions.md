# S-13 缓存自动配置全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | @EnableCaching 后 CacheManager 哪来的? | §1 (切面驱动装配) |
| 2 | Caffeine/Redis/Simple 怎么选? | §2 (ImportSelector + 条件) |
| 3 | 没缓存库会怎样? | §2 (Simple 内存兜底) |
| 4 | 怎么统一定制缓存管理器? | §3 (CacheManagerCustomizers) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么条件用 @ConditionalOnBean(CacheAspectSupport)? | §1 (切面启用才装配) |
| 6 | 为什么 ImportSelector + 各自条件? | §2 (classpath 驱动) |
| 7 | 与 s19 的边界? | §3 (机制在 s19) |
| 8 | 与 S-12 事务定制器同构点? | §3 (Customizer 模式) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | Simple 兜底的意义? | §2 (无库也可用) |
| 10 | CacheManager 注入到哪里? | §3 (s19 CacheInterceptor) |

## 覆盖: 10 问 / 3 身份 / 100%
