# ALI-A8 Sentinel Gateway 限流+断路器 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. 网关降级的三种途径优先级? 用户 Bean 为什么最高?
2. SentinelCircuitBreaker 的规则为什么在构造时注入而非运行时?
3. BlockException 不 Tracer.trace 的原因? 业务异常 trace 的意义?
4. create 的 computeIfAbsent 与 SCC-10 Customizer.once 的幂等语义?
5. 阻塞/响应式双工厂的装配如何分流?

## B. 源码实证 (6)

6. SCG 装配的两个条件? (grep L58-60)
7. SentinelGatewayFilter 的 @Order 值? (grep L139-147)
8. initFallback 的两种模式? (grep L103-126)
9. applyToSentinelRuleManager 的合并逻辑? (grep L70-83)
10. run 的三分支? (grep L85-112)
11. 默认配置 defaultConfiguration 的构成? (grep Factory L12-14)

## C. 推理深挖 (5)

12. 规则 setResource 绑定后, 同一资源多个断路器会怎样? loadRules 合并语义?
13. BlockException 降级不 trace — 熔断统计里被限流的请求算什么?
14. SentinelGatewayBlockExceptionHandler @Order(HIGHEST_PRECEDENCE) 的意义? 与异常处理链?
15. ReactiveSentinelCircuitBreaker 的 Mono 语义怎么映射 run/fallback?
16. fallback-msg-response 模式空 responseBody 会怎样? (grep L104)

## D. 跨域扩展 (4)

17. SentinelCircuitBreaker vs SCC-10 的 run+fallback 契约: 完整实现对照?
18. 网关过滤器 vs Gateway 5.5 的 GlobalFilter 排序体系: @Order(-1) 位置?
19. DegradeRuleManager.loadRules vs A7 的 register2Property: 两种规则注入方式?
20. Feign 面 CircuitBreakerRuleChangeListener vs OpenFeign 5.6 的 FeignCircuitBreaker: 分工?
