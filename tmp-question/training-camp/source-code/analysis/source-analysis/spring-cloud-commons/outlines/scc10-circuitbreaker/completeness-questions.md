# SCC-10 断路器抽象 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. CircuitBreaker 双方法契约? 无 fallback 为什么抛 NoFallbackAvailableException?
2. 三层工厂族 (Abstract/CircuitBreaker/Reactive) 的分工?
3. Customizer.once 的幂等定制怎么实现?
4. 观测面为什么用装饰器而非侵入实现?
5. 配置泛型 (CONF/CONFB) 怎么隔离各实现差异?

## B. 源码实证 (5)

6. run(toRun) default 的 fallback 是什么? (grep CircuitBreaker:29-33)
7. configure 的 ids 参数语义? (grep AbstractFactory:38-45)
8. once() 的 keyMapper 作用? (grep Customizer:42-50)
9. ObservedCircuitBreaker 的 delegate 怎么用? (grep L40-47)
10. ReactiveCircuitBreaker 的 run(Mono) 默认? (grep L31-37)

## C. 推理深挖 (5)

11. 为什么 run(toRun) 的 fallback 是"抛异常"而非"返回 null"?
12. configureDefault 与 configure 的优先级? 未配置 id 用什么?
13. create(id, groupName) 默认委托 create(id) — groupName 有什么用?
14. ObservedCircuitBreaker 的 Observation 生命周期? (start/stop)
15. 实现族怎么继承 Factory 提供 ConfigBuilder?

## D. 跨域扩展 (5)

16. CircuitBreaker 抽象 vs Resilience4j 的 CircuitBreaker API?
17. vs Sentinel 的 SpringCloudCircuitBreaker (阶段 5.9)?
18. run+fallback vs Feign 的 ErrorDecoder fallback (5.1)?
19. 观测装饰器 vs SCC-8 的 HealthIndicator 装饰模式?
20. 如果加"断路器规则推送", 应该在哪个实现层? 为什么抽象层不做?
