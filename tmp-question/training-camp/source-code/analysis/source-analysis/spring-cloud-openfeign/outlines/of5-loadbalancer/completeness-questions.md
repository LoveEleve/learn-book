# OF-5 负载均衡 — completeness-questions (全视角提问验证)

## 开发者视角

1. choose 在哪? (L118)
2. 无实例怎么办? (503)
3. 重试怎么配? (RetryTemplate)
4. retry 开关? (retry.enabled)
5. XForwarded? (链路)
6. 底层 Client? (装饰链)
7. Lifecycle 回调? (onStart)
8. 装配条件? (@ConditionalOnBean)

## 架构师视角

9. 为什么 SCC 抽象? (LB 实现可换)
10. 为什么 503 显式? (可诊断)
11. 为什么条件装配? (依赖缺失早暴露?)
12. 为什么 Spring Retry? (生态一致)
13. 为什么策略三态? (开关语义)
14. 为什么装饰链? (职责分离)
15. 为什么 Lifecycle? (挂钩点)
16. 为什么 hint? (区域亲缘)

## 学生视角

17. 什么是负载均衡? (选实例)
18. 什么是 503? (无实例)
19. 什么是重试? (换实例再试)
20. 什么是装饰器? (包一层)
