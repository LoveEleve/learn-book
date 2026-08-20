# OF-2 代理创建与装配 — 双路径→builder→超时→Target

> 前置: [[OF-1-注册机制]] | 引出: [[OF-3-契约集成]] [[OF-5-负载均衡]] [[OF-6-熔断]] [[OF-7-配置隔离]] [[OF-9-动态刷新]] | 对照: Feign 本体 F-1 Builder/F-3 代理
> 🔴 A | 8 KP | [模式: 工厂方法 + 组件装配 + 策略注入]
> Pass 2 闭环: q1(双路径) q2(builder 装配) q3(超时定制) q4(Target 解析)

**读者处境**: 注入 @FeignClient 接口时, 代理怎么创建? 有 url/无 url 两条路差在哪? 超时怎么配? 这篇拆 FeignClientFactoryBean (742)。

### 1. 双路径 — getTarget loadBalance vs unwrap

场景: 代理创建入口怎么分流?
源码路径:
- **getObject** (L455) → **getTarget** (L465): builder = feign(factory) → 双路径
- **路径 1 无 url** (L468-482): url="http://"+name → **loadBalance** (L427): Client null → **IllegalStateException "Did you forget to include spring-cloud-starter-loadbalancer?"** (L449-451 生产陷阱)
- **路径 2 有 url** (L484-500): 前缀补全 → **unwrap**: ((RetryableFeignBlockingLoadBalancerClient)client).getDelegate() (L495-497 — "not load balancing because we have a url")
- **targeter.target** (L502-503): Targeter 注入点 (OF-6 熔断装饰)
关键设计 (q1): **服务发现 vs 直连双路径 + 生产陷阱显式化 + unwrap 剥 LB**。[模式: 入口面]

### 2. builder 装配 — 子上下文组件 + Capability

场景: Builder 从哪来? 组件怎么装?
源码路径:
- **feign()** (L120-145): FeignLoggerFactory → Logger + **get(Feign.Builder.class) (子上下文!)** .logger/.encoder/.decoder/.contract
- **configureUsingConfiguration** (L192-260): **9 组件 + Map<Capability>** (L247): Logger.Level/Retryer/ErrorDecoder+Factory/Options/**RequestInterceptor Map**/ResponseInterceptor/QueryMapEncoder/ExceptionPropagationPolicy/Capability; ⚠ **组件获取继承开关**: getInheritedAware (L409-420) — **inheritParentContext=true → getInstance (含父上下文继承) / false → getInstanceWithoutAncestors (仅子上下文)**; ⚠ FeignClientsConfiguration 14 @Bean 中 **3 个 @Scope("prototype")** (L208/222/229: Builder/Contract/Encoder/Decoder?)
- **configureUsingProperties** (L146-190): **defaultConfig + contextId 双配置合并** (OF-7 关联); ⚠ **inheritParentContext 默认 true** (L100) — 影响 properties 应用 (L173) 与组件继承 (L410/419)
- ⚠ **FeignClientsConfiguration 14 @Bean 穷举** (L104-254): feignDecoder/feignEncoder/feignEncoderPageable/feignQueryMapEncoderPageable/feignContract/feignConversionService/feignRetryer/feignLoggerFactory/feignClientConfigurer + **3 个 Feign.Builder 变体** (feignBuilder L210/defaultFeignBuilder L224/**circuitBreakerFeignBuilder L232 — OF-6 熔断 Builder!**) + **2 个 Micrometer Capability** (micrometerObservationCapability L247/micrometerCapability L254 — 指标面 OF-7)
关键设计 (q2): **Builder 子上下文隔离 + 组件可覆盖 (继承语义) + Capability 能力面**。[模式: 装配面]

### 3. 超时与定制 — Options + Customizers

场景: 超时优先级? 定制顺序?
源码路径:
- **OptionsFactoryBean** (L60-95): **clientConfiguration 优先 + 逐项 fallback** (connectTimeout/readTimeout/**followRedirects**) → new Request.Options
- **applyBuildCustomizers** (L153-161): getInstances(contextId) + **AnnotationAwareOrderComparator 排序** + customize(builder)
关键设计 (q3): **超时三级优先级 (逐项 fallback) + Customizer 有序应用**。[模式: 配置面]

### 4. Target 解析 — 三分支 + Targeter

场景: Target 有几种? 怎么收尾?
源码路径:
- **resolveTarget 三分支** (L524-542): 有 url → **HardCodedTarget** / RefreshableUrl → **RefreshableHardCodedTarget** (OF-9) / 无 → **PropertyBasedTarget** (配置懒加载)
- **cleanPath** (L508-522): path 规范化 (补 "/")
- **DefaultTargeter** (L25-28): target → feign.target; OF-6 替换为 FeignCircuitBreakerTargeter
关键设计 (q4): **URL 来源三态 (直连/动态/懒加载) + Targeter 策略注入**。[模式: 收尾面]

## 代码类型
Architecture (工厂+装配) + Spring 容器机制

## 负面空间 (OF-2, 6 条)

| 不做 | 说明 |
|:--|:--|
| 不代理缓存 | 每次 getObject 重建 (q1) |
| 不自动探测 url | 前缀补全后直连 (q1) |
| 不组件缓存 | 每次构建重新获取 (q2) |
| 不超时自动调优 | 静态配置 (q3) |
| 不 URL 合法性校验 | 直连 (q4) |
| 不自动选择 Targeter | 容器注入 (q4) |

## 结尾桥 OUTBOUND

- → [[OF-3-契约集成]]: Contract = SpringMvcContract — 注解解析
- → [[OF-5-负载均衡]]: loadBalance 路径 → FeignBlockingLoadBalancerClient
- → [[OF-6-熔断]]: Targeter 三分支 → FeignCircuitBreakerTargeter
- → [[OF-7-配置隔离]]: 子上下文组件获取 + Properties 合并
- → [[OF-9-动态刷新]]: RefreshableHardCodedTarget
