# SCC-10 断路器抽象 — run 与 fallback 的统一契约: 断路器生态的插槽

> 前置: [[SCC-12-扩展策略]] (收官对照) | 对照: Resilience4j + Sentinel (阶段 5.9) + Hystrix (历史)
> 🟡 B | 方案 B (标准) | 闭环: q1(接口面) q2(工厂族) q3(定制面) q4(观测面)
> Pass 2 闭环: q1(run+fallback) q2(Factory 三层) q3(Customizer once) q4(Observed 装饰)

**读者处境**: Spring Cloud 怎么统一 Resilience4j/Sentinel/Hystrix 三种断路器? 调用的"run + fallback"契约长什么样? 配置怎么按 id 隔离? 观测 (Micrometer) 怎么集成?

### 1. CircuitBreaker 接口 — run + fallback 的最小契约

场景: 所有断路器的统一接口?
源码路径:
- CircuitBreaker (circuitbreaker/CircuitBreaker.java:27): **双方法** — run(toRun) default (L29-33, **无 fallback → 抛 NoFallbackAvailableException** L31) + **run(toRun, fallback) 核心** (L35)
- Supplier\<T\> 执行 + Function\<Throwable, T\> 回退
- 实现族 (外部): Resilience4JCircuitBreaker / SpringCloudCircuitBreaker (Sentinel) — **断路器生态插槽**
关键设计 (q1): **"无 fallback 的 run 是默认抛异常"** — run(toRun) 委托 run(toRun, fallback) 且 fallback 抛 NoFallbackAvailableException; 契约极小 (两个方法) 让各实现自由。 [模式: 最小契约 + 默认兜底]

### 2. CircuitBreakerFactory — 三层工厂族

场景: 断路器怎么创建和配置?
源码路径:
- **AbstractCircuitBreakerFactory** (AbstractCircuitBreakerFactory.java:28): **configurations ConcurrentHashMap** (L30) + **configure(consumer, ids)** (L38-45, 每个 id 构建配置) + **configureDefault** (L66-68, 默认配置函数) + configBuilder 抽象 (L60)
- **CircuitBreakerFactory extends Abstract** (CircuitBreakerFactory.java:25-33): **create(id) 抽象** (L28) + **create(id, groupName) 默认委托** (L30-32)
- ConfigBuilder\<CONF\> 泛型: 配置构建器
关键设计 (q2): **"按 id 隔离配置"** — configurations Map 每 id 一份; configure 用 Builder 构建; configureDefault 提供默认; create(id) 从配置创建实例。 [模式: 三层工厂 + 配置隔离]

### 3. Customizer — 定制点 + once 去重

场景: 创建后怎么定制?
源码路径:
- Customizer\<T\> (Customizer.java:29): **customize 单方法** (L31)
- **once() 静态方法** (L42-50): ConcurrentMap + computeIfAbsent (L46) — **保证每目标只定制一次** (去重)
- 用法: 工厂返回后 customize 配置 (如设置超时)
关键设计 (q3): **"once 包装保证幂等定制"** — 同一目标多次 customize 只执行一次; ConcurrentMap 并发安全; 定制是工厂的补充扩展点。 [模式: 幂等定制]

### 4. ReactiveCircuitBreaker — 响应式变体

场景: 响应式场景怎么用断路器?
源码路径:
- ReactiveCircuitBreaker (ReactiveCircuitBreaker.java:29-44): **run(Mono) L31-37 / run(Flux) L39+ + fallback** — 响应式变体
- ReactiveCircuitBreakerFactory (L25-34): create 返回响应式断路器
- 与阻塞版同构: run + fallback 契约
关键设计 (q1): **"响应式变体 = 同构契约"** — Mono/Flux 版 run + fallback; 实现族 (Resilience4JReactiveCircuitBreaker 等) 各自适配。 [模式: 响应式变体]

### 5. 观测面 — ObservedCircuitBreaker + Micrometer

场景: 断路器调用怎么被观测?
源码路径:
- ObservedCircuitBreaker (observation/ObservedCircuitBreaker.java:32): **delegate 装饰 + ObservationRegistry** (L36-42) — run 时创建 Observation (L46-47)
- observation/ 7 类: CircuitBreakerObservationContext/Convention/Documentation/DefaultConvention + ObservedFunction/ObservedSupplier
- **装饰器模式**: Observed 包住真实断路器, 观测不侵入实现
关键设计 (q4): **"观测是装饰层"** — ObservedCircuitBreaker 装饰 delegate; ObservationRegistry (Micrometer) 采集; 7 类观测面独立成包 (可插拔)。 [模式: 观测装饰器]

### 6. 装配与生态 — 实现族的接入

场景: Resilience4j/Sentinel 怎么接入?
源码路径:
- 本仓库只定义抽象 (接口 + 工厂 + Customizer + 观测)
- 实现族 (外部): resilience4j-spring-boot 的 Resilience4JCircuitBreakerFactory / Sentinel 的 SpringCloudCircuitBreakerFactory
- ConfigBuilder 泛型: 各实现定义自己的配置类型 (Resilience4JConfigBuilder 等)
关键设计 (q2): **"抽象在 commons, 实现在各生态"** — 接口/工厂/定制/观测全在抽象层; 实现只需继承 Factory + 提供 ConfigBuilder; 配置类型泛型化 (CONF/CONFB) 隔离各实现差异。 [模式: SPI 生态]

## 代码类型
Architecture (抽象面) + SPI (断路器生态)

## 负面空间 — 断路器抽象刻意不做的事

- **不做断路器算法内建**: 状态机 (CLOSED/OPEN/HALF_OPEN) 由实现负责 (Resilience4j/Sentinel)
- **不做 fallback 自动生成**: fallback 必须调用方提供 (无默认降级逻辑)
- **不做配置持久化**: 配置内存态, 无外部化
- **不做指标内建**: 观测走 Micrometer Observation (独立面)
- **不做熔断规则推送**: 规则管理是实现的职责 (Sentinel Dashboard 等)
- **不做跨进程状态共享**: 断路器状态单实例 (对比 Hystrix 集群)

→ 引出: 阶段 5.9 Sentinel 怎么实现这个插槽? (收束对照)
