# OF-2 代理创建与装配 — completeness-questions (全视角提问验证)

## 开发者视角

1. getObject 触发什么? (getTarget)
2. 两种路径? (loadBalance/unwrap)
3. 生产陷阱? (缺 loadbalancer starter)
4. Builder 从哪来? (子上下文)
5. 9 组件? (Logger/Retryer/ErrorDecoder...)
6. 超时怎么配? (Properties 优先)
7. Target 几种? (三分支)
8. Targeter? (策略注入)

## 架构师视角

9. 为什么双路径? (服务发现 vs 直连)
10. 为什么 unwrap? (有 url 剥 LB)
11. 为什么 Builder 子上下文? (配置隔离)
12. 为什么组件可覆盖? (继承语义)
13. 为什么逐项 fallback? (部分配置)
14. 为什么 Customizer 排序? (顺序确定)
15. 为什么 Target 三分支? (URL 三态)
16. 为什么 Targeter 注入? (熔断解耦)

## 学生视角

17. 什么是 Target? (服务目标)
18. 什么是 unwrap? (剥装饰)
19. 什么是 Customizer? (builder 定制)
20. 什么是 fallback? (配置回退)
