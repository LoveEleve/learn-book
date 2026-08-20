# ALI-A8 Sentinel Gateway 限流+断路器 — 知识规划 (KP)

> 🟡 B | 模块: sentinel-gateway (6) + circuitbreaker-sentinel (11) | 版本: 2025.0.0.0

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 网关条件装配 | SentinelSCGAutoConfiguration:57-61 | GlobalFilter + gateway.enabled |
| 2 | 降级三途径 | SentinelSCGAutoConfiguration:79-126 | 用户 Bean > msg-response > redirect |
| 3 | 网关异常处理 | SentinelSCGAutoConfiguration:128-137 | BlockExceptionHandler HIGHEST_PRECEDENCE |
| 4 | 网关过滤器 | SentinelSCGAutoConfiguration:139-147 | SentinelGatewayFilter @Order(-1) |
| 5 | 规则构造注入 | SentinelCircuitBreaker:70-83 | setResource + loadRules 合并 |
| 6 | run 三分支 | SentinelCircuitBreaker:85-112 | 通过/降级不统计/业务异常 trace |
| 7 | 工厂族 | SentinelCircuitBreakerFactory:15-21 | computeIfAbsent + 默认配置 |
| 8 | 配置 Builder | SentinelConfigBuilder:30-38 | resourceName/entryType/rules 默认 |
| 9 | 响应式变体 | ReactiveSentinelCircuitBreakerFactory | Mono 版 run |
| 10 | 双面装配 | 阻塞+Reactive AutoConfiguration | 消费方按类型注入 |

## 02 高频坑

1. 网关过滤器内核在 sentinel 仓库, SCA 只注册
2. BlockException 降级不 trace (不算业务异常)
3. 规则构造期 loadRules 注入, 与 A7 数据源路独立
4. fallback 双模式: msg-response / redirect
5. 用户 BlockRequestHandler 优先于配置 (低优先注释)
6. create 需 id 非空 (Assert)
7. 默认 EntryType.OUT

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| 契约 | CircuitBreaker (SCC-10) / CircuitBreakerFactory 三层族 / ConfigBuilder |
| 网关 | GlobalFilter / BlockExceptionHandler / GatewayCallbackManager |
| 规则 | DegradeRuleManager.loadRules / setResource 绑定 / 不可变包装 |
| 降级 | BlockException→fallback / Exception→trace+fallback / 三途径优先级 |
| 装配 | @ConditionalOnClass / @ConditionalOnMissingBean / 双面 |
| 幂等 | computeIfAbsent / 默认配置覆盖 |

## 04 跨域桥接

- ← SCC-10: CircuitBreaker 契约实现方实证
- → Gateway 5.5: GlobalFilter 排序体系
- → Sentinel 5.9: DegradeRule/GW 适配器内核
- → OpenFeign 5.6: Feign 面 CircuitBreakerRuleChangeListener
- → 面试: "Sentinel 熔断/网关限流" — 规则注入 + 异常分类 + 降级途径
