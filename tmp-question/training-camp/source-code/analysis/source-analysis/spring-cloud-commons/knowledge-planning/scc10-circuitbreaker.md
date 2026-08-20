# SCC-10 断路器抽象 — 知识规划 (KP)

> 域: SCC-10 | 级别: 🟡 | 方案: B | 大纲: outlines/scc10-circuitbreaker/outline.md (6 节)

## §01 域定位

断路器抽象 = run + fallback 的统一契约 + 工厂配置隔离 + 定制去重 + 观测装饰。抽象在 commons, 实现在各生态 (Resilience4j/Sentinel/Hystrix)。

## §02 源文件清单

| 文件 | 职责 | 归属节 |
|:--|:--|:--:|
| circuitbreaker/CircuitBreaker.java | run+fallback 契约 | 1 |
| circuitbreaker/AbstractCircuitBreakerFactory.java | 配置 Map + configure | 2 |
| circuitbreaker/CircuitBreakerFactory.java | create 抽象 | 2 |
| circuitbreaker/Customizer.java | customize + once 去重 | 3 |
| circuitbreaker/ReactiveCircuitBreaker(+Factory) | 响应式变体 | 4 |
| circuitbreaker/observation/ (7 类) | Micrometer 观测装饰 | 5 |
| circuitbreaker/ConfigBuilder + NoFallbackAvailableException | 配置构建 + 兜底异常 | 1,2 |

## §05 闭环要点 (Pass 2 内化)

### q1 接口面
run(toRun) default 无 fallback 抛 NoFallbackAvailableException (L31) + run(toRun, fallback) 核心 (L35)。

### q2 工厂族
Abstract (configurations L30 + configure L38 + configBuilder L60) → CircuitBreakerFactory (create L28) → Reactive 变体。

### q3 定制面
Customizer.once (L42-50): ConcurrentMap computeIfAbsent 幂等。

### q4 观测面
ObservedCircuitBreaker delegate 装饰 (L32-47) + observation/ 7 类。

## §06 负面空间 (6 条)

不做算法内建 / 不做 fallback 自动生成 / 不做配置持久化 / 不做指标内建 / 不做规则推送 / 不做跨进程状态共享

## §07 交叉引用

- ← SCC-12 扩展策略 (收官对照)
- → 阶段 5.9 Sentinel (实现插槽) + Resilience4j 生态
- 另见: Hystrix (历史)
