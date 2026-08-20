# SCC-6 ServiceInstanceListSupplier 体系 — 实例列表的"加工流水线": 发现→缓存→健康检查链

> 前置: [[SCC-3-服务发现]] (DiscoveryClient 实例源) + [[SCC-13-NamedContextFactory]] (LoadBalancerClientFactory) | 引出: [[SCC-7-ReactorLoadBalancer]] (消费实例) + [[SCC-12-扩展策略]] | 对照: Ribbon ServerList + Netflix LoadBalancer
> 🔴 A | 方案 A (全深度) | 闭环: q1(接口面) q2(委托链) q3(缓存机制) q4(Builder 装配)
> Pass 2 闭环: q1(Supplier+get) q2(Delegating 基类) q3(CacheFlux) q4(Builder 20+ with)

**读者处境**: LoadBalancer 选实例前, 实例列表怎么来? "发现→缓存→健康检查"的链式加工怎么组织? 缓存怎么实现 (响应式)? 健康检查怎么周期刷新? 怎么自定义整条链?

### 1. ServiceInstanceListSupplier 接口 — 响应式实例列表的契约

场景: 实例列表的提供者长什么样?
源码路径:
- ServiceInstanceListSupplier (core/ServiceInstanceListSupplier.java:33): **extends Supplier\<Flux\<List\<ServiceInstance\>\>\>** + getServiceId() (L35) + **get(Request) default** (L37-39, 默认 get()) + **builder() 静态工厂** (L41-43)
- **响应式核心**: 返回 Flux\<List\> 而非 List — 异步实例获取
- 消费方: ReactorLoadBalancer (SCC-7) 调 get()
关键设计 (q1): **"Supplier\<Flux\<List\>\>" 是响应式惰性获取** — 每次 get() 返回一个 Flux (可重复订阅); get(Request) 让实例获取能感知请求上下文 (hint/粘性); builder() 是链装配入口。 [模式: 响应式 Supplier]

### 2. DelegatingServiceInstanceListSupplier — 链式委托的基类

场景: 链上每个加工器怎么"包住"下一个?
源码路径:
- DelegatingServiceInstanceListSupplier (core/DelegatingServiceInstanceListSupplier.java:32): implements ServiceInstanceListSupplier + **SelectedInstanceCallback + InitializingBean + DisposableBean**
- delegate 字段 (L35) + 构造校验 (L36-37, "delegate may not be null")
- **getServiceId 委托** (L47-48) — 链上各层共享 serviceId
- **selectedServiceInstance 传递** (L52-55): delegate 是 SelectedInstanceCallback → 传递 — 实例选择回调链式传播; **消费方是 LoadBalancer**: RoundRobinLoadBalancer:93 / RandomLoadBalancer:74 选中实例后调用回调; **SameInstancePreference 覆写** (L94-95, super + 自有逻辑 — SCC-12 联动)
关键设计 (q2): **委托模式 = 每个加工器包装下一个** — delegate 字段 + 构造注入; getServiceId/selectedServiceInstance 沿链传递; InitializingBean/DisposableBean 让每层有生命周期。 [模式: 装饰器委托]

### 3. DiscoveryClientServiceInstanceListSupplier — 链的基底

场景: 实例列表最初从哪来?
源码路径:
- DiscoveryClientServiceInstanceListSupplier (core/DiscoveryClientServiceInstanceListSupplier.java:46): **链的基底** (Builder withDiscoveryClient)
- **Flux.defer(Mono.fromCallable(delegate.getInstances(serviceId)))** (L64-65) — 惰性包装 DiscoveryClient 调用
- **.timeout(30s)** (L65): SERVICE_DISCOVERY_TIMEOUT 属性 (L51, 默认 30s) — 发现调用超时
- serviceId 从环境属性 (L62/L76: PROPERTY_NAME)
- 响应式变体: delegate.getInstances 返回 Flux 时 collectList (L79)
关键设计 (q1): **基底 = DiscoveryClient 的响应式包装** — Flux.defer 惰性 (每次订阅才查) + timeout 防注册中心挂起; 服务发现超时独立配置。 [模式: 响应式包装基底]

### 4. CachingServiceInstanceListSupplier — CacheFlux 响应式缓存

场景: 实例列表怎么缓存? 为什么用 CacheFlux?
源码路径:
- CachingServiceInstanceListSupplier (core/CachingServiceInstanceListSupplier.java:40): extends Delegating
- **CacheFlux.lookup** (L55-72): 响应式缓存查找 — 命中直接返回, **onCacheMissResume(delegate.get().take(1))** (L70) 未命中取底层并缓存; **andWriteWith 写回** (L71-78, cache.put); **cache 不存在 → log.error + 走 miss 路径** (L58-61, 每请求打底层的退化场景)
- 缓存名: **SERVICE_INSTANCE_CACHE_NAME** (L47-48, "CachingServiceInstanceListSupplier" + "Cache")
- **cacheManager.getCache** (L57): Spring CacheManager (Caffeine 默认) — 与 SCC-2 无关, 这是 LoadBalancer 专用缓存
- 缓存清理: 实例变更时? (SelectedInstanceCallback 关联)
关键设计 (q3): **CacheFlux 是"响应式缓存查找"** — 不是简单 put/get, 而是 Flux 层面的查找+miss 回填; onCacheMissResume 保证"第一次请求打底层, 后续走缓存"; Spring Cache 抽象让缓存实现可换 (Caffeine/Redis)。 [模式: 响应式缓存]

### 5. HealthCheckServiceInstanceListSupplier — 周期健康检查

场景: 实例列表怎么过滤掉不健康的?
源码路径:
- HealthCheckServiceInstanceListSupplier (core/HealthCheckServiceInstanceListSupplier.java:48): extends Delegating
- healthCheck 配置 (L67): loadBalancerClientFactory.getProperties(getServiceId()).getHealthCheck()
- **defaultHealthCheckPath = "/actuator/health"** (L68)
- **Flux.interval + fixedBackoff** (L71-72): `onlyIf(refetchInstances).fixedBackoff(refetchInstancesInterval)` — **周期重新拉取**
- **switchMap(healthCheckFlux(...).map(alive -> List.copyOf(alive)))** (L75): 健康检查过滤 → 存活列表
- **Repeat 双驱动** (L91-92): Repeat.onlyIf(repeatHealthCheck).fixedBackoff(interval) — 健康检查周期独立于重拉周期
- **aliveInstancesReplay** (L77-79): `delaySubscription(initialDelay).replay(1).refCount(1)` — **replay(1) 缓存最近一次健康结果 + refCount(1) 单订阅共享** (多消费者共享同一健康检查流)
- **afterPropertiesSet 主动订阅** (L81-87): `aliveInstancesReplay.subscribe()` — **启动即开始健康检查** (非懒)
- healthCheckFlux (L89-93): 每实例 **isAlive + onErrorResume** (健康检查异常降级)
关键设计 (q3): **"周期重拉 + 存活过滤"是响应式健康检查** — Flux.interval 周期驱动, switchMap 做检查过滤; refetchInstances 决定是否重拉底层; 健康检查路径默认 /actuator/health (可配)。 [模式: 周期过滤]

### 6. ServiceInstanceListSupplierBuilder — 链装配的 20+ 方法

场景: 整条链怎么拼?
源码路径:
- ServiceInstanceListSupplierBuilder (core/ServiceInstanceListSupplierBuilder.java:57): final Builder
- **with 方法族 20+** (L74-352): withDiscoveryClient (L92)/withBlockingDiscoveryClient (L74)/withBase 自定义 (L110)/withWeighted (L120-137)/withHealthChecks (L152-169)/withBlockingHealthChecks (L197-243)/withSameInstancePreference (L183)/withZonePreference (L257-273)/withRequestBasedStickySession (L288)/withCaching (L305)/withRetryAwareness (L321)/withHints (L327)/withSubset (L336)/**with(DelegateCreator) 自定义** (L352)
- **逐层包装** (L443-449): 每个 with 方法把新 Supplier 包住当前 delegate — 链式装饰; **build() 语义: baseCreator.apply 建基底 → for(creator : creators) 依次包外层** (后注册的在外层)
- 消费: LoadBalancerClientConfiguration 装配默认链
关键设计 (q4): **Builder 是"链式装饰"的装配器** — 20+ with 方法每个都是"包一层"; with(DelegateCreator) 是自定义扩展点 (用户注入任意层); 顺序决定链的加工顺序 (基底→加工→策略)。 [模式: 链式装饰 Builder]

## 代码类型
Architecture (实例获取链) + Reactive (响应式流)

## 负面空间 — Supplier 体系刻意不做的事

- **不做实例排序/选择**: 只提供列表, 选择是 SCC-7 ReactorLoadBalancer 的职责
- **不做多注册中心聚合**: 委托单个 DiscoveryClient, 聚合靠 SCC-3 CompositeDiscoveryClient
- **不做实例权重计算内建**: WeightedSupplier 需用户提供 WeightFunction
- **不做健康检查协议内建**: 默认 /actuator/health, 自定义实现需扩展
- **不做缓存淘汰策略内建**: Caffeine 默认, 淘汰策略配置面 (对照 SCC-13 无缓存淘汰)
- **不做故障转移**: 健康检查只过滤, 无"主备切换"语义

→ 引出: 实例列表到手后, 怎么选一个? → SCC-7 ReactorLoadBalancer 策略
