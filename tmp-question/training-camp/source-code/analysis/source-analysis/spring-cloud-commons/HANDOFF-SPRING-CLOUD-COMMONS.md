# HANDOFF — Spring Cloud Commons 源码分析交接文档 (13/13 全量收官)

> **日期**: 2026-08-16 | **版本**: 4.3.2 (pom.xml:11 实证) | 模块: spring-cloud-commons 162 + spring-cloud-context 66 + spring-cloud-loadbalancer 51 = **279 主源** (全量含测试 462)
> **给新 AI**: 本文是 Spring Cloud Commons 阶段的**唯一入口**。13 域全部交付 (KP + 大纲 + 20 问 + 六层深审 + 时空溯源 🔴 + harness 🔴 + 2 轮 REVIEW)。
> **源码**: `/data/workspace/source-code/code/spring/spring-cloud-commons` (git 浅克隆单提交)
> **规划**: SCC-PLAN.md (09 审计 v1 + 3 轮深度 REVIEW 修正)
> **分工确认**: Dubbo/gRPC/Gateway/OpenFeign 由其他 AI 负责; Commons 本会话 13/13 收官 ✅

---

## §零 状态速查 (2026-08-16, 13/13 收官)

| 域 | 模块 | 级别 | 方案 | 大纲节 | questions | harness | 时空溯源 | REVIEW 修正 |
|:--:|:--|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| SCC-1 Bootstrap 上下文 | context/bootstrap | 🔴 | A | 6 | 20 | **12/12** | ✅ 双轨制 | 深审 5+二轮 6 |
| SCC-2 @RefreshScope 热刷新 | context/scope+refresh | 🔴 | A | 6 | 20 | **11/11** | ✅ gh-349 | 深审 4+二轮 6 |
| SCC-8 RefreshEndpoint+事件 | context/endpoint+event | 🟡 | B | 5 | 20 | — | — | 深审 5+二轮 6 |
| SCC-9 配置加密 | bootstrap/encrypt | 🟡 | B | 6 | 20 | — | — | 深审 4+二轮 5 |
| SCC-13 NamedContextFactory | context/named | 🔴 | A | 6 | 20 | **14/14** | ✅ issue 锚 | 深审 4+二轮 5 |
| SCC-3 服务发现抽象 | client/discovery | 🔴 | A | 6 | 20 | **16/16** | ✅ probe 演进 | 深审 4+二轮 6 |
| SCC-4 服务注册抽象 | client/serviceregistry | 🔴 | A | 6 | 20 | **17/17** | ✅ management 演进 | 深审 4+二轮 5 |
| SCC-5 @LoadBalanced 客户端 | client/loadbalancer | 🔴 | A | 6 | 20 | **7/7** | ✅ @since 4.1.2 | 深审 3+二轮 4 |
| SCC-6 Supplier 体系 | loadbalancer/core | 🔴 | A | 6 | 20 | **11/11** | ✅ @since 2.2.0 | 深审 4+二轮 5 |
| SCC-7 ReactorLB 策略 | loadbalancer/core | 🔴 | A | 6 | 20 | **12/12** | ✅ ocelli | 深审 4+二轮 4 |
| SCC-11 BlockingLB 重试 | blocking/retry | 🟡 | B | 6 | 20 | — | — | 深审 4+二轮 6 |
| SCC-12 扩展策略 | loadbalancer/core | 🟡 | B | 6 | 20 | — | — | 深审 0+二轮 5 |
| SCC-10 断路器抽象 | client/circuitbreaker | 🟡 | B | 6 | 20 | — | — | 深审 4 |

**统计**: 13 域全交付 · 8 个 harness **100/100 断言全 PASS** · 260 问 (13×20) · 锚点 20-40/域全 grep 实证 · REVIEW 修正 100+ 处

**执行序**: SCC-1 → SCC-2 → SCC-8 → SCC-9 → SCC-13 → SCC-3 → SCC-4 → SCC-5 → SCC-6 → SCC-7 → SCC-11 → SCC-12 → SCC-10

---

## §一 13 域核心知识速查 (全量固化)

### SCC-1 Bootstrap 上下文 (🔴, context/bootstrap)

**核心机制**: BootstrapApplicationListener (512 行) 监听 ApplicationEnvironmentPreparedEvent, 构建 bootstrap 子上下文
- **双轨制**: `bootstrapEnabled || useLegacyProcessing` (PropertyUtils.java:48-54) — 新项目 spring.config.import / 老项目 bootstrap.enabled 或 MARKER_CLASS 兼容
- **入口双守卫** (BootstrapApplicationListener.java:99/103): 未启用 return / 已含 bootstrap 源不递归
- **apply 双保护** (L291-293): BootstrapMarkerConfiguration 防重复 + filterListeners 日志过滤
- **子上下文构建** (L142-203): 空环境 + config.name 注入 (L151) + setId("bootstrap") (L203) + addAncestorInitializer 设 parent (L205)
- **PropertySourceLocator SPI** (config/PropertySourceLocator.java:36): locate 单方法契约 — Nacos/Apollo 配置中心的插槽
- **insertPropertySources 排序仲裁** (PropertySourceBootstrapConfiguration.java:184-230): 反转 addFirst 保序 + **三策略** — 默认 addFirst 最高优先 / overrideNone addLast / overrideSystemProperties 相对 systemEnvironment addAfter/addBefore
- **锚**: DEFAULT_ORDER = HIGHEST_PRECEDENCE+5 (L87) · configName `${spring.cloud.bootstrap.name:bootstrap}` (L107)
- **harness MiniBootstrap 12/12**: 三守卫/SPI Composite 展开/排序仲裁三模式/父子可见

### SCC-2 @RefreshScope 热刷新 (🔴, context/scope+refresh)

**核心机制**: GenericScope (502 行) + LockedScopedProxyFactoryBean 双锁
- **代理+读锁**: LockedScopedProxyFactoryBean (L433-502) 代理+MethodInterceptor 双角色 — 方法调用 getTargetSource().getTarget() 前 readLock (L474)
- **双锁分工**: cache ConcurrentMap 无锁读 (StandardScopeCache putIfAbsent L20-25) + destroy 写锁 (L127-142)
- **双检懒创建**: BeanLifecycleWrapper.getBean synchronized(name) (L369-374)
- **双阶段刷新**: ContextRefresher.refresh() (L92-96) = refreshEnvironment (before/after 对比 + EnvironmentChangeEvent) + scope.refreshAll (清缓存 + RefreshScopeRefreshedEvent)
- **新旧刷新器**: @ConditionalOnBootstrapEnabled → LegacyContextRefresher / @ConditionalOnBootstrapDisabled → ConfigDataContextRefresher (RefreshAutoConfiguration.java:104-114) — **新旧由 bootstrap 开关决定**
- **锚**: gh-349 异常还原 (L487) · copyEnvironment 只复制 [commandLineArgs, defaultProperties] (L54-57) · RefreshScopeLifecycle @since 4.1.0
- **harness MiniRefresh 11/11**: 写锁被读锁阻塞实证 (刷新安全核心)

### SCC-8 RefreshEndpoint+事件 (🟡, context/endpoint)

**核心机制**: 刷新的"按钮"
- **双触发面**: RefreshEndpoint (@Endpoint(id="refresh") + @WriteOperation → contextRefresher.refresh()) / **RefreshEventListener** (RefreshEvent 程序化触发)
- **ready 守卫**: AtomicBoolean — ApplicationReadyEvent 才置 true, 就绪前 RefreshEvent 忽略 (RefreshEventListener.java:70)
- **⚠ RefreshEvent 发布者在外部**: 主源码零发布 — Nacos 的 NacosConfigRefreshEventListener (spring-cloud-alibaba) 发 RefreshEvent
- **事件链同源**: EnvironmentChangeEvent 两个发布者 (ContextRefresher L103-105 + EnvironmentManager L73/92)
- **健康集成**: RefreshScopeHealthIndicator 聚合 RefreshScope.getErrors + rebinder.getErrors → up/down
- **锚**: RefreshEndpoint 四条件装配 (RefreshEndpointAutoConfiguration.java:71-73) · RefreshEventListener 装配在 RefreshAutoConfiguration:125-126

### SCC-9 配置加密 (🟡, bootstrap/encrypt)

**核心机制**: {cipher} 双路径解密
- **双解密路径**: 绑定期 (TextEncryptorBindHandler onSuccess Binder 钩子 L55-60) + 环境期 (AbstractEnvironmentDecrypt L67-105 产出 decrypted 源)
- **Boot 3 新架构**: TextEncryptorConfigBootstrapper (BootstrapRegistryInitializer) 注册 TextEncryptor/BindHandler 到 BootstrapRegistry — 与 EncryptionBootstrapConfiguration @Bean 双轨并存
- **Failsafe delegate**: bootstrap 阶段密钥未就绪 → Failsafe 占位 → setDelegate 延迟替换 (TextEncryptorUtils.java:57-70)
- **COLLECTION_PROPERTY** (L39): `(\S+)?\[(\d+)\](\.\S+)?` 索引属性整族处理 + "[" 前缀精确匹配
- **failOnError 两处同构**: KeyProperties vs AbstractEnvironmentDecrypt 字段 — true 抛 IllegalStateException / false 返回 ""
- **锚**: DECRYPTED_PROPERTY_SOURCE_NAME="decrypted" (L44) · "No reason to decrypt bootstrap twice" (L81) · removeDecryptedProperties 先清旧源 (L93)

### SCC-13 NamedContextFactory (🔴 Hub, context/named)

**核心机制**: 每个客户端一个子上下文
- **双 Map**: contexts/configurations ConcurrentHashMap (L69/71) + getContext 双检锁 (L119-126)
- **registerBeans 三段** (L143-159): ① name 精确匹配 ② **default. 前缀全局默认** (L152, LoadBalancerClientConfigurationRegistrar 用 "default."+类名 L68/71) ③ PropertyPlaceholder+defaultConfigType
- **buildContext** (L161-192): BeanFactory CL 用 parent (L169-173, netflix#3101/openfeign#475 修复) + AOT 分支 (L175-181) + propertySourceName 注入 (L185-187) + setParent (L189)
- **含祖先查找只在 ResolvableType/注解变体**: getInstance(name, Class) 用 getBean 不含祖先 (L203) / getInstance(ResolvableType) 用 IncludingAncestors (L227)
- **锚**: github issue 注释 L162-163 · Specification (L266-270) · ClientFactoryObjectProvider 延迟 (L35-43)
- **harness MiniContext 14/14**: 隔离/default 注入/父可见/双检并发/配置时序实证 (构建期固定)

### SCC-3 服务发现抽象 (🔴, client/discovery)

**核心机制**: 所有注册中心的统一插槽
- **四方法契约**: DiscoveryClient (L32) — description/getInstances/getServices/probe (L66-68 default) + DEFAULT_ORDER=0 (L37)
- **Composite 排序短路** (L41): AnnotationAwareOrderComparator.sort + **getInstances 短路** (L51-56 第一个非空返回) vs **getServices 合并去重** (L62-70)
- **ImportSelector autoRegister 双分支** (L40-66): true 追加 AutoServiceRegistrationConfiguration / false 注入 auto-registration.enabled=false 属性 (被 AutoServiceRegistrationAutoConfiguration:30 + AutoServiceRegistrationConfiguration:28 的 @ConditionalOnProperty 消费)
- **Simple 属性驱动**: application.yml 实例列表 + order 可配
- **健康/心跳**: DiscoveryClientHealthIndicator 双模式 (useServicesQuery→getServices / probe) + HeartbeatMonitor AtomicReference
- **锚**: probe → reactiveProbe @Deprecated(forRemoval) (ReactiveDiscoveryClient.java:75) · getInstances 短路 L51-56
- **harness MiniDiscovery 16/16**: 排序短路/空实例跳过/合并去重/心跳变更

### SCC-4 服务注册抽象 (🔴, client/serviceregistry)

**核心机制**: 启动即注册的仪式
- **5 方法契约**: ServiceRegistry (L26) — register/deregister/close/setStatus/getStatus
- **双角色触发**: AbstractAutoServiceRegistration (L49-50) — ApplicationListener\<WebServerInitializedEvent\> + start() — **端口就绪才算可注册**
- **start 仪式** (L142-170): isEnabled 守卫 → running 双检 → InstancePreRegisteredEvent → 前钩子 → register → 后钩子 → **shouldRegisterManagement 三条件** (L176-184: registerManagement 属性 + getManagementPort()!=null + ManagementServerPortUtils.isDifferent) → InstanceRegisteredEvent → running CAS
- **RegistrationLifecycle 4 钩子** (L27-60) + RegistrationManagementLifecycle 新增 4 管理专用方法 (独立钩子列表 L70)
- **failFast 默认 false** (AutoServiceRegistrationProperties.java:29-31) — 无实现默认静默
- **锚**: management namespace 跳过 (L114-117) · stop 镜像对称 (L294-311) · ServiceRegistryEndpoint 装配
- **harness MiniRegistry 17/17**: 触发链/running 双检/isEnabled/管理注册/钩子顺序/stop 对称

### SCC-5 @LoadBalanced 客户端 (🔴, client/loadbalancer)

**核心机制**: 一个注解拦截 RestTemplate
- **@Qualifier 标记** (LoadBalanced.java:38) — 收集方 @LoadBalanced List\<RestTemplate\> (LoadBalancerAutoConfiguration.java:60-62)
- **"URL host 即服务名"** (LoadBalancerInterceptor.java:53): `serviceName = originalUri.getHost()` + Assert 校验 (L54)
- **双 execute**: execute(serviceId, request) 自选实例 / execute(serviceId, instance, request) 指定 — + reconstructURI 逻辑名→真实地址
- **BlockingLoadBalancerClient 阻塞包装** (L68-84): getHint → LoadBalancerRequestAdapter → **LoadBalancerLifecycle.onStart** → choose → null 实例 → onComplete(DISCARD) + IllegalStateException → execute
- **choose = Mono.from(loadBalancer.choose(request)).block()** (L163) — 响应式内核+阻塞门面
- **DeferringLoadBalancerInterceptor** (4.1.2): ObjectProvider.getIfAvailable 首次 intercept 解析
- **锚**: transforms transformRequest(HttpRequest, ServiceInstance) — 实例选定后执行
- **harness MiniLoadBalanced 7/7**: 轮询/URI 重建/transformers/指定实例

### SCC-6 ServiceInstanceListSupplier 体系 (🔴, loadbalancer/core)

**核心机制**: 实例列表的加工流水线
- **响应式 Supplier**: Supplier\<Flux\<List\<ServiceInstance\>\>\> (L33) + get(Request) default (L37-39) + builder() (L41)
- **Delegating 委托链** (L32): getServiceId 委托 (L47-48) + **selectedServiceInstance 传递** (L52-55, 消费方是 LoadBalancer: RoundRobin:93)
- **基底**: DiscoveryClientSupplier Flux.defer(Mono.fromCallable).timeout(30s) (L64-65)
- **CacheFlux 缓存** (L55-79): CacheFlux.lookup + onCacheMissResume (L70) + andWriteWith 写回 (L71-78) + cache 不存在走 miss 路径
- **健康过滤双周期**: refetchInstances (L71-72) + repeatHealthCheck (L91-92) + **aliveInstancesReplay replay(1).refCount(1)** (L77-79) + afterPropertiesSet 主动订阅 (L81-87)
- **Builder 20+ with** (L74-352): 逐层包装 + build() baseCreator→for(creator) 后注册在外层 (L443-449)
- **锚**: @since 2.2.0 奠基 (核心族) → 2.2.7/3.0.0/3.0.2/4.1.0 渐进扩展
- **harness MiniSupplier 11/11**: 缓存 miss/命中/健康过滤/链序

### SCC-7 ReactorLoadBalancer 策略 (🔴, loadbalancer/core)

**核心机制**: 响应式负载均衡策略
- **ReactorLoadBalancer** (L31): choose(Request) → Mono\<Response\> (L39) + choose() default (L41-43)
- **轮询数学**: position AtomicInteger (L47) + **seedPosition 随机种子** (L60, `new Random().nextInt(1000)` 防惊群) + **incrementAndGet & Integer.MAX_VALUE** (L114, 忽略符号位循环) + 取模 (L116)
- **三态** (L98-116): 空 EmptyResponse+warn / **单实例不转位置** (L106-109, 注释 "suppliers have already filtered") / 多实例取模
- **Random 同构** (L41): ThreadLocalRandom (L86) — 差异只在 getInstanceResponse
- **每服务隔离**: LoadBalancerClientFactory extends NamedContextFactory + implements ReactiveLoadBalancer.Factory (L46-47) + NAMESPACE="loadbalancer" (L55-58) + 默认 RoundRobin + 默认 Supplier 链 withDiscoveryClient().withCaching() (LoadBalancerClientConfiguration.java:74-89)
- **锚**: ocelli 源码引用 (L80-81, Netflix 灵感) · SingletonSupplier 惰性 (L64-66)
- **harness MiniReactorLB 12/12**: 轮询数学/& MAX/单实例/回调/每服务隔离

### SCC-11 BlockingLoadBalancer 重试 (🟡, blocking/retry)

**核心机制**: RetryTemplate 包裹的换服务器重试
- **RetryTemplate 包裹** (RetryLoadBalancerInterceptor.java:75-76): createRetryTemplate (L144-166: BackOffPolicy factory + setThrowLastExceptionOnExhausted + **RetryPolicy 条件选择** L156-160: 禁用 → NeverRetryPolicy / InterceptorRetryPolicy)
- **同服/换服**: 上下文实例复用 (L79) vs 重新选择 (L93-102, RetryableRequestContext + previousServiceInstance)
- **状态码异常驱动** (L124-130): retryableStatusCode → bodyCopy (L128) + close (L129) + throw ClientHttpResponseStatusCodeException (L130)
- **canRetry 方法级安全** (L43-47): **只有 GET 或 retryOnAllOperations=true 才可重试**
- **registerThrowable 换服触发** (L66-83): 不能同服但可重试 → 重置 sameServerCount + nextServerCount++ + setExhaustedOnly/setServiceInstance(null)
- **计数不对称**: 同服 `<` (L52) vs 换服 `<=` (L58, 注释 "increment first and then check")
- **锚**: InterceptorRetryPolicy implements RetryPolicy (L30) + canRetry 委托 canRetryNextServer (L66)

### SCC-12 LoadBalancer 扩展策略 (🟡, loadbalancer/core)

**核心机制**: 六种实例滤镜
- **Weighted**: METADATA_WEIGHT_KEY="weight" (L41) + metadataWeightFunction 默认 (L50) + **LazyWeightedServiceInstanceList GCD 归一化 + 懒展开** (L46-66: weights 求最大公约数 + total/GCD 展开数组防大权重爆炸 + get(index) 才 selector.next())
- **ZonePreference**: ZONE 元数据键 (L42) + spring.cloud.loadbalancer.zone + **回退全部** (Javadoc L31-34)
- **HintBased**: getHint 双来源 (L63-74: 请求头 hintHeaderName / HintRequestContext) + 实例元数据 "hint" 键匹配 (L93)
- **StickySession**: instanceIdCookieName cookie 匹配 (L60-68) + 无 cookie 回退全部 (L73)
- **Subset 分桶**: size 配置 + count = 总数/size + bucket*size (L71-78, Netflix subsetting)
- **SameInstancePreference**: selectedServiceInstance 覆写 (L94-95) 选择记忆
- **锚**: 回退语义三处一致 (zone/hint/cookie 绝不空手)

### SCC-10 断路器抽象 (🟡, client/circuitbreaker)

**核心机制**: run + fallback 统一契约
- **双方法契约** (L27-35): run(toRun) default 无 fallback → **抛 NoFallbackAvailableException** (L31) + run(toRun, fallback) 核心 (L35)
- **三层工厂族**: AbstractCircuitBreakerFactory (configurations L30 + configure L38 + configBuilder L60) → CircuitBreakerFactory (create L28) → Reactive 变体 (L29-44)
- **Customizer.once 幂等** (L42-50): ConcurrentMap computeIfAbsent 每目标只定制一次
- **观测装饰器**: ObservedCircuitBreaker delegate 装饰 (L32-47) + observation/ 7 类 (Micrometer)
- **锚**: 抽象在 commons / 实现在各生态 (Resilience4j/Sentinel/Hystrix)

---

## §二 方法论执行报告

### 09 怀疑审计修正汇总 (SCC-PLAN)

| 修正 | 证据 | 落点 |
|:--|:--|:--|
| 版本 2025.0.0 → **4.3.2** | pom.xml:11 | SCC-PLAN v1 |
| C-9 路径 supplier/ → **loadbalancer/core/** | find 实证 | SCC-PLAN v1 |
| 7 项待验证断言全通过 | 逐个 grep (DiscoveryClient extends Ordered L32/@LoadBalanced @Qualifier L38/Flux.defer L64/AtomicInteger L47/NamedContextFactory L60) | SCC-PLAN v1 |
| **4 个规划类名编造/过时**: NoopCircuitBreaker/RefreshListener/RefreshScopeBeanPostProcessor + ParentContextApplicationContextInitializer (后纠为 Boot 依赖类) | 类名穷举 70+ | SCC-PLAN 三轮 |
| C-2 "CGLIB 代理" → **ScopedProxyFactoryBean** | GenericScope.java:246/433 | SCC-PLAN v1 |
| hypermedia/security 排除 | <10 文件 + 双低信号 | SCC-PLAN v1 |

### 各域深审发现 (代表性)

| 域 | 最重大发现 |
|:--|:--|
| SCC-1 | "三守卫"实为入口双守卫+apply 双保护; insertPropertySources 四分支 (漏 addBefore) |
| SCC-2 | 双刷新器由 bootstrap 开关决定 (跨域联动修正) |
| SCC-8 | RefreshEvent 主源码零发布 — 发布者在 Nacos 外部仓库 |
| SCC-9 | TextEncryptorBindHandler 经 BootstrapRegistry 注册 (Boot 3 新架构双轨) |
| SCC-13 | "含祖先查找"只在 ResolvableType 变体; 类加载器双轨 (BeanFactory vs context) |
| SCC-3 | probe 覆写遍历全部子客户端; 健康检查 discoveryInitialized 门控+双模式 |
| SCC-4 | shouldRegisterManagement 三条件; failFast 默认 false; 管理钩子独立列表 |
| SCC-5 | BlockingLoadBalancerClient execute 完整链 (生命周期+null 实例处理) |
| SCC-6 | aliveInstancesReplay replay(1).refCount(1) 共享流; build() 包装顺序 |
| SCC-7 | 测试名揭示回调链传递; 默认 RoundRobin+withCaching 链 |
| SCC-11 | registerThrowable 换服触发; 计数 `<` vs `<=` 真实原因 (先递增后检查) |
| SCC-12 | **LazyWeighted GCD 归一化+懒展开** (不是"按权重展开/排序") |
| SCC-10 | once() 幂等定制; 观测装饰器独立成包 |

### harness 自抓缺陷汇总 (8 个, 每域 1-2)

- SCC-1: MARKER_CLASS 双保险前置 / keep-system 断言语义
- SCC-2: 写锁被读锁阻塞实证 (刷新安全)
- SCC-13: 配置时序 (构建期固定实证) / Bean key 语义
- SCC-3: 排序短路 (空实例跳过继续查) / List.of 可变性
- SCC-4: 触发链完整 / running 双检防重复
- SCC-5: 轮询+URI 重建 / transformers / 指定实例
- SCC-6: 缓存 miss/命中 / 链序 (缓存包健康过滤)
- SCC-7: 轮询数学 (seed/& MAX/取模) / 回调 / 每服务隔离

---

## §三 高频坑汇总 (跨域 30 条)

### 上下文面 (SCC-1/2/8/9)
1. bootstrap 是"被取代但仍完整保留"的遗产 — 三条件启用 (enabled/marker/legacy)
2. "高优先级"是 insertPropertySources 的 addFirst 实现的, 不是概念
3. RefreshScope 代理是 ScopedProxyFactoryBean 非手写 CGLIB
4. 双刷新器由 bootstrap 开关决定 (不是版本新旧)
5. RefreshEvent 发布者在外部仓库 (Nacos), 主源码零发布
6. {cipher} 双路径解密: 绑定期 Binder 钩子 + 环境期后处理
7. FailsafeTextEncryptor 是 delegate 占位 (延迟替换), 不是简单兜底
8. EnvironmentChangeEvent 同源不同发布者 (刷新/运行时管理)

### 配置隔离 (SCC-13)
9. "default." 前缀配置注入所有子上下文 (全局默认约定)
10. 含祖先查找只在 ResolvableType/注解变体, Class 变体不含
11. BeanFactory CL 用 parent, context 自身 CL 用工厂类的 (双轨)
12. 子上下文构建期固定 — 配置热更新不可能 (SCC-2 对照)

### 发现/注册 (SCC-3/4)
13. getInstances 短路 vs getServices 合并 — 两方法语义不同
14. autoRegister=false 注入属性被两个装配类 @ConditionalOnProperty 消费
15. 触发点是 WebServerInitializedEvent (端口就绪) 非应用启动
16. shouldRegisterManagement 三条件 (属性+端口存在+端口不同)
17. failFast 默认 false (无实现静默)

### 负载均衡 (SCC-5/6/7/11/12)
18. "URL host 即服务名" 是 @LoadBalanced 的核心约定
19. BlockingLoadBalancerClient = 响应式内核 + 阻塞门面 (Mono.block)
20. CacheFlux 是响应式缓存查找 (onCacheMissResume), 非简单 put/get
21. aliveInstancesReplay replay(1).refCount(1) 共享健康流
22. & Integer.MAX_VALUE 忽略符号位 — 位运算循环
23. 单实例不转位置 (supplier 已过滤联动)
24. 随机种子防冷启动惊群
25. 重试异常驱动 (ClientHttpResponseStatusCodeException 触发 RetryTemplate)
26. 只有 GET 或 retryOnAllOperations 才可重试
27. 计数 `<` vs `<=` 因先递增后检查
28. LazyWeighted GCD 归一化+懒展开 (防大权重爆炸)
29. zone/hint/cookie 回退语义一致 (绝不空手)

### 断路器 (SCC-10)
30. 无 fallback 的 run 默认抛 NoFallbackAvailableException

---

## §四 文件路径

```
analysis/source-analysis/spring-cloud-commons/
├── SCC-PLAN.md                    ← 09 审计 v1 + 3 轮深度 REVIEW (权威规划)
├── HANDOFF-SPRING-CLOUD-COMMONS.md ← 本文 (13/13 收官唯一入口)
├── outlines/ (13 域, 每域 3-4 文件)
│   ├── scc1-bootstrap/  scc2-refreshscope/  scc8-refresh-endpoint/
│   ├── scc9-encryption/  scc13-named-context/
│   ├── scc3-discovery/   scc4-serviceregistry/
│   ├── scc5-loadbalanced/  scc6-supplier/  scc7-reactor-lb/
│   ├── scc11-blocking-retry/  scc12-extended-strategies/  scc10-circuitbreaker/
│   └── (每域: outline.md + completeness-questions.md (20 问) + review-notes.md (+temporal-trace.md 🔴))
├── knowledge-planning/ (13 个 KP 文件)
├── harness/ (8 个 🔴 域)
│   ├── scc1-bootstrap/MiniBootstrap.java (12/12)
│   ├── scc2-refreshscope/MiniRefresh.java (11/11)
│   ├── scc13-named-context/MiniContext.java (14/14)
│   ├── scc3-discovery/MiniDiscovery.java (16/16)
│   ├── scc4-serviceregistry/MiniRegistry.java (17/17)
│   ├── scc5-loadbalanced/MiniLoadBalanced.java (7/7)
│   ├── scc6-supplier/MiniSupplier.java (11/11)
│   └── scc7-reactor-lb/MiniReactorLB.java (12/12)
源码: /data/workspace/source-code/code/spring/spring-cloud-commons/ (4.3.2)
上级: ../HANDOFF-STAGE5.md (阶段 5 唯一总入口) — 5.4 已标注 ✅ 13/13 收官
后续: 阶段 5.5 Gateway (其他 AI) → 5.6 OpenFeign (其他 AI) → 5.7 Alibaba → 5.8 Nacos → 5.9 Sentinel
```

---

## §五 完成检查单

- [x] 13/13 域全量交付 (2026-08-16): §零 状态表 + §一 13 域速查 + §三 30 坑
- [x] 每域: Pass 0-3 + 六层深审 + 时空溯源 🔴 (6 个) + harness 🔴 (8 个 100/100) + 2 轮 REVIEW
- [x] 09 审计: 版本修正 + 4 类名穷举 + 7 断言复验 + 双条件装配发现
- [x] 分工确认: Gateway/OpenFeign/Dubbo/gRPC 其他 AI; Commons 本会话收官
- [x] 下一步: 阶段 5.7 Alibaba / 5.8 Nacos / 5.9 Sentinel (空闲, 需先确认无其他 AI)

---

## §六 交接 REVIEW (2026-08-16, 文档发布前)

| # | 维度 | 发现 | 处置 |
|:--:|:--|:--|:--|
| 1 | 锚点抽查 | 6 项关键锚点全部精确 (SCC-1 四分支 L184/209/223 / SCC-2 L104 / SCC-7 L114 / SCC-6 L70 / SCC-12 GCD L46-52 / SCC-11 GET-only L47) | 通过 ✅ |
| 2 | 目录整洁度 | harness 残留 79 个 Dbg/class 文件 — 与 §四 声称的 8 个 Mini*.java 不符 | **已清理** (87→8) + 验证 12/12 全过 |
| 3 | 结构完整性 | 13 outline / 13 KP / 8 harness 全部与文档一致 | 通过 ✅ |

> 注: 接手后运行 harness 需先 javac 编译 (源码在 harness/*/Mini*.java, class 不保留)
