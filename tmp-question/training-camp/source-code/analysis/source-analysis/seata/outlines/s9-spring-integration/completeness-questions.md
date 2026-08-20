# S-9 Spring 集成 — completeness-questions (全视角提问验证)

## 开发者视角

1. 注解有哪些参数? (timeoutMills/rollbackFor/propagation/lockRetry*)
2. 超时怎么重置? (<=0 或 ==60000 → 默认)
3. 谁扫描? (GlobalTransactionScanner)
4. 怎么织入? (AbstractAutoProxyCreator + Advisor)
5. 拦截什么? (TM handler + GlobalLock handler)
6. 异常怎么处理? (failureHandler 钩子)
7. 自动配置? (SeataAutoConfiguration 两 bean)
8. disable? (配置开关)

## 架构师视角

9. 为什么 AbstractAutoProxyCreator? (Spring AOP 基础设施复用)
10. 为什么双集合? (去重 + 目标预收集)
11. 为什么已代理 bean 按序织入? (与既有 AOP 共存)
12. 为什么超时重置? (注解未配置时跟随全局动态默认)
13. 为什么双 handler? (TM 事务 vs GlobalLock 锁 — 职责分离)
14. 为什么 FailureHandler? (失败回调钩子 — 业务感知)
15. 对照 Spring @Transactional? (RollbackRule 家族同源)
16. 为什么 FactoryBean 排除? (工厂类非业务目标)

## 学生视角

17. 什么是 AOP? (面向切面 — 方法拦截)
18. 什么是扫描器? (找需要增强的 bean)
19. 什么是 Advisor? (切面通知)
20. 什么是 FailureHandler? (失败回调)
