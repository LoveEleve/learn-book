# OF-6 熔断 — completeness-questions (全视角提问验证)

## 开发者视角

1. 三分支? (普通/fallback/fallbackFactory)
2. 熔断器怎么建? (factory.create)
3. 降级函数? (fallbackFunction)
4. 异步上下文? (isAsync + setRequestAttributes)
5. fallback 从哪来? (子上下文)
6. 熔断名? (NameResolver)
7. 怎么禁用? (双条件)
8. Default 兜底? (constant)

## 架构师视角

9. 为什么 SCC 抽象? (Resilience4J 可换)
10. 为什么三分支? (降级三态)
11. 为什么 contextId 命名? (多客户端唯一)
12. 为什么 run 一体? (主调用+降级)
13. 为什么异步上下文恢复? (TraceId 不丢)
14. 为什么 Builder 继承? (复用 feign 链)
15. 为什么双条件禁用? (无类/显式关)
16. 为什么方法级降级映射? (每方法语义)

## 学生视角

17. 什么是熔断? (失败保护)
18. 什么是降级? (兜底响应)
19. 什么是 fallback? (类降级)
20. 什么是熔断器名? (隔离单元)
