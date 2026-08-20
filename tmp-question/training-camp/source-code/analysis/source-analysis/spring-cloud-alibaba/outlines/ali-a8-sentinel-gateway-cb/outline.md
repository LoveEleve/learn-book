# ALI-A8 Sentinel Gateway 限流 + 断路器 — 网关入口与调用出口的降级双闸

> 前置: [[ALI-A6-三路限流]] (SphU 契约) + [[SCC-10-断路器抽象]] (CircuitBreaker 契约) | 引出: [[Sentinel-5.9]] (GW 适配器/DegradeRule 内核) + [[Gateway-5.5]] (GlobalFilter 面) | 对照: Commons SCC-10 三层工厂族
> 🟡 B | 方案 B (重要域) | 闭环: q1(网关组装) q2(降级双模式) q3(规则注入) q4(工厂族)

**读者处境**: 网关层怎么限流? 被限流后返回什么 (重定向还是 JSON)? @CircuitBreaker 注解的 run+fallback 背后 Sentinel 怎么实现? 熔断规则 (DegradeRule) 什么时候注入 RuleManager?

### 1. 网关组装 — SentinelSCGAutoConfiguration 的条件与 Bean

场景: SCG 限流三件套怎么装配?
源码路径:
- **SentinelSCGAutoConfiguration** (gateway/scg/SentinelSCGAutoConfiguration.java:57): @ConditionalOnClass(GlobalFilter) + @ConditionalOnProperty(gateway.enabled, matchIfMissing=true) (L58-60)
- **@PostConstruct init 三连** (L77-83): ① BlockRequestHandler Optional → GatewayCallbackManager.setBlockHandler (L79-80, 低优先) ② initAppType (L92-95: System.setProperty APP_TYPE) ③ initFallback (L97-126)
- **SentinelGatewayBlockExceptionHandler** (L128-137): @Order(HIGHEST_PRECEDENCE) — 异常处理器
- **SentinelGatewayFilter** (L139-147): @Order(-1) — 核心过滤器, order 可配 (gatewayProperties.getOrder)
- 适配器类 (SentinelGatewayFilter/SentinelGatewayBlockExceptionHandler/BlockRequestHandler) 都在 **sentinel 仓库** (09 审计确认) — 集成层注册
关键设计 (q1): **"薄装配 + 内核外置"** — SCA 只做条件 + 注册 + 配置传递 (order), 过滤器内核在 sentinel 的 gateway.sc 适配器 — 与 A6 Web 路同构。 [模式: 薄装配]

### 2. 降级双模式 — initFallback 的响应式与重定向

场景: 网关被限流后返回什么?
源码路径:
- **initFallback** (L97-126): fallbackProperties 为 null 或 mode 空 → return (L99-102)
- **模式一: fallback-msg-response** (L103-116): responseBody 非空 → GatewayCallbackManager.setBlockHandler 返回 **自定义 status/contentType/body 的 ServerResponse** (L105-109)
- **模式二: fallback-redirect** (L117-125): redirectUrl → RedirectBlockRequestHandler
- 用户 BlockRequestHandler Bean 优先于配置 (L79-80 "has low priority")
关键设计 (q2): **"降级 = 三种途径按优先级"** — 用户 Bean > 配置 fallback-msg > 配置 redirect; 响应式 ServerResponse 直接构造, 无需模板渲染。 [模式: 降级优先级]
- **示例实证**: examples/sentinel-spring-cloud-gateway-example MySCGConfiguration — 用户 BlockRequestHandler Bean (status 444 + JSON body) → GatewayCallbackManager.setBlockHandler, 对应 init L79-80 "blockRequestHandlerOptional has low priority"

### 3. 断路器核心 — SentinelCircuitBreaker 的规则注入与 run

场景: Sentinel 版 CircuitBreaker 怎么实现 SCC-10 的 run+fallback?
源码路径:
- **SentinelCircuitBreaker** (circuitbreaker-sentinel/SentinelCircuitBreaker.java:43): implements CircuitBreaker (SCC-10)
- **构造时规则注入** (L51-60 + L70-83): Assert 双校验 (L53-54) + rules 不可变包装 (L57) + **applyToSentinelRuleManager** (L70-83: 现有规则合并 + **rule.setResource(resourceName) 绑定资源** + DegradeRuleManager.loadRules)
- **run 三分支** (L85-112): ① SphU.entry 成功 → toRun.get() (L89-92) ② **BlockException → fallback.apply(ex)** (L94-99, 注释: 被限流/熔断不算业务异常, 不 Tracer.trace) ③ **其他异常 → Tracer.trace(ex) + fallback.apply(ex)** (L100-105) ④ finally entry.exit (L106-111)
- 三构造器 (L51-68): 默认 EntryType.OUT
关键设计 (q3): **"规则 = 构造期注入 + 资源绑定"** — DegradeRule 在断路器创建时即注册 (loadRules), 无需外部数据源; 异常分类: BlockException 直接降级 (不统计), 业务异常先 trace 再降级 (影响熔断统计)。 [模式: 构造期规则注入]

### 4. 工厂族 — SentinelCircuitBreakerFactory 与配置 Builder

场景: 断路器工厂怎么和 SCC-10 的工厂族对接?
源码路径:
- **SentinelCircuitBreakerFactory** (SentinelCircuitBreakerFactory.java:32): extends CircuitBreakerFactory<SentinelCircuitBreakerConfiguration, SentinelConfigBuilder> (SCC-10 三层工厂族)
- **create** (L15-21): Assert id + **getConfigurations().computeIfAbsent(id, defaultConfiguration)** (L18 — SCC-10 的 Customizer.once 语义) → new SentinelCircuitBreaker
- **defaultConfiguration** (L12-14): resourceName=id + EntryType.OUT + 空 rules
- **configureDefault** (L24-27): 覆盖默认配置函数
- **SentinelConfigBuilder** (SentinelConfigBuilder.java:32): implements ConfigBuilder — resourceName/entryType/rules 三字段 + build() 默认值 (L30-38)
- **Reactive 变体**: ReactiveSentinelCircuitBreakerFactory (59 行) + ReactiveSentinelCircuitBreaker (104 行, Mono 版 run)
关键设计 (q4): **"工厂族三件套 = SCC-10 契约完整实现"** — 工厂 (create+computeIfAbsent) + 配置 (ConfigBuilder) + 断路器 (run/fallback); 响应式独立变体, 阻塞/响应式双面。 [模式: 契约化工厂]

### 5. 装配面 — 双自动配置 + Feign 面

场景: 断路器工厂什么时候被装配?
源码路径:
- **SentinelCircuitBreakerAutoConfiguration** (57 行): @ConditionalOnClass(SentinelCircuitBreaker) + @ConditionalOnMissingBean + CircuitBreakerFactory 注册
- **ReactiveSentinelCircuitBreakerAutoConfiguration** (53 行): Reactive 面
- **SentinelFeignClientAutoConfiguration** (feign/, 11 文件域): feign.CircuitBreakerRuleChangeListener + FeignClientCircuitNameResolver — Feign 熔断对接 (OpenFeign 5.6 面)
- 默认装配条件: circuitbreaker-sentinel 依赖 spring-cloud-commons + sentinel-core
关键设计 (q5): **"装配 = 契约 Bean 注册"** — 两个 AutoConfiguration 分别注册阻塞/响应式工厂, 消费方 (Feign/WebClient) 按类型注入 — 与 SCC-10 的观察者面一致。 [模式: 双面装配]

### 6. 测试与行为锚

场景: 断路器的降级语义怎么验证?
源码路径:
- 测试: SentinelCircuitBreakerTest / SentinelGatewayTests (test 目录)
- 注释锚: "If the SphU.entry() does not throw BlockException, it means that the request can pass" (SentinelCircuitBreaker.java:90-91) — 通过语义官方声明
- "So it should not be counted as the business exception" (L96-97) — 不统计语义
关键设计 (q1): **"注释 = 语义契约"** — 通过/降级/统计三语义写死在注释, 测试对齐。 [模式: 注释契约]
