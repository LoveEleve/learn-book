# 闭环笔记 GW-7-q3 — SCC 交叉: ReactiveCircuitBreaker 消费

假设: 熔断器是 SCC 的能力 — ReactiveCircuitBreakerFactory.create(id) → ReactiveCircuitBreaker.run(mono, fallback), 实现 (Resilience4J) 在 SCC 侧。

验证过程:
- **接口** (SCC-10 ReactiveCircuitBreaker.java:29-37, SCC-PLAN SCC-10 交叉): `run(Mono)` (L31-32, 默认 fallback 抛错) + `run(Mono, Function<Throwable, Mono> fallback)` (L37) — **GW-7 消费的契约**
- **工厂** (SCC ReactiveCircuitBreakerFactory): create(id) — 按断路器 id 建实例 (配置隔离)
- **GW-7 消费** (SpringCloudCircuitBreakerFilterFactory.java:95): `reactiveCircuitBreakerFactory.create(config.getId())` — 工厂注入 (构造 L70-73)
- **Resilience4J 版** (SpringCloudCircuitBreakerResilience4JFilterFactory.java:32-37): 继承抽象工厂 + SCC Resilience4J 工厂 (Resilience4J 具体实现类在 SCC 独立模块, 交叉引用 SCC-PLAN)
- 交叉引用原则: SCC 另一 AI 域 — 只声明消费, 不探索内部

代码类型: Glue (SPI 消费)

结论: 熔断 = **SCC 能力消费**: 断路器实例/配置/实现 (Resilience4J) 全在 SCC; 网关只做"链包装 + 状态码语义 + fallback 编排"。**被放弃的方案: 网关自实现熔断** — 与 RestTemplate/Feign 熔断统一 (跨组件一致)。 [跨域: SCC-10 ReactiveCircuitBreaker (SCC-PLAN SCC-10 交叉); GW-2 链] [架构: 分层消费] (SpringCloudCircuitBreakerFilterFactory.java:70-95; ReactiveCircuitBreaker.java:29-37)
