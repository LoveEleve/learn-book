# Spring Cloud Commons — 知识网络化规划 (SCC-1~SCC-13, 09 怀疑审计后 v1)

> **日期**: 2026-08-16 | **依据**: issue/源码分析执行计划.md 阶段5.4 (13 域) + issue/SpringCloudCommons源码学习范围规划.md (2025.0.0 基准, 13 域) + 09 对既有规划保持怀疑
> **源码**: `/data/workspace/source-code/code/spring/spring-cloud-commons` (**4.3.2**, pom.xml:11 实证; 3 核心子模块: spring-cloud-commons 162 + spring-cloud-context 66 + spring-cloud-loadbalancer 51 = **279 主源**; 全量含测试 **462** 与规划文档一致 — 口径: 主源 279 / 含测试 462)
> **定位**: 阶段 5.4 — RPC 与服务治理第四环 **Spring Cloud 服务发现/注册/负载均衡抽象层 (框架基座)**
> **知识网络**: 与 Feign (5.1, 已收官 6/6) + SofaJRaft (4.6, 已收官) + Dubbo (5.2, 并行) + gRPC (5.3, 并行) 互联; 为 Gateway (5.5) + OpenFeign (5.6) + Alibaba (5.7) + Nacos (5.8) + Sentinel (5.9) 提供基座
> **分工确认**: Dubbo/gRPC 已被其他 AI 占用; Spring Cloud Commons 无人占用, 本会话开工 ✅

---

## 〇、09 怀疑审计表 (Spring Cloud Commons, 2026-08-16) — 必读

| 既有规划断言 | 验证动作 | 证据 | 结论 |
|---|---|---|---|
| 版本基准 | pom.xml:11 | **4.3.2** (规划基准 2025.0.0 版本号含义 — 实际仓库 2024.0.x 线; 需按源码实证) | **修正** ⚠ 按 4.3.2 实证 |
| 域清单 13 个 | 模块扫描 | 3 核心模块 (162/66/51) 与规划一致; 无未覆盖大包 (starter 模块 0-1 文件纯装配) | **接受** ✅ |
| C-5 "DiscoveryClient extends Ordered" | grep 源码 | **✅ 确认**: DiscoveryClient.java:32 `extends Ordered` + description/getInstances/getServices (L43-55) | **接受** ✅ |
| C-6 "AbstractAutoServiceRegistration 监听 WebServerInitializedEvent" | grep 源码 | **✅ 确认**: L50 `implements ApplicationListener<WebServerInitializedEvent>` + onApplicationEvent L111 | **接受** ✅ |
| C-7 "@LoadBalanced 是 @Qualifier 限定符" | grep 源码 | **✅ 确认**: LoadBalanced.java:38 `@Qualifier` 元注解 | **接受** ✅ |
| C-9 "DiscoveryClientServiceInstanceListSupplier 用 Flux.defer" | grep 源码 | **✅ 确认** + **路径修正**: 类在 `loadbalancer/core/` 非规划写的 `supplier/`; Flux.defer L64 | **接受+路径修正** ✅ |
| C-10 "RoundRobinLoadBalancer 用 AtomicInteger position" | grep 源码 | **✅ 确认**: RoundRobinLoadBalancer.java:47 position + choose L82 | **接受** ✅ |
| C-13 "NamedContextFactory 为每服务建子上下文" | grep 源码 | **✅ 确认**: NamedContextFactory.java:60 + createContext L130 + getProvider L214 | **接受** ✅ |
| 域编号 | 对照 | **三套编号并存**: 规划 C-1~C-13 / 执行计划 CC-1~CC-13 / 本 PLAN SCC-1~SCC-13 — 统一为 SCC 并在域清单标注对应 | **修正** ✅ |

> 注: 规划文档自述多处理序有"源码验证"标注 — 按 09 铁律, 每个断言开工时独立 grep 复验, 数字/类名/行号一律以 4.3.2 源码为准。

### ⚠ 深度 REVIEW (2026-08-16, 09 全量断言复验)

| 断言 | 验证 | 证据 | 结论 |
|---|---|---|---|
| C-2 "RefreshScope CGLIB 代理" | grep GenericScope | **精确化**: 代理是 **Spring AOP ScopedProxyFactoryBean** (GenericScope.java:42/246/433 LockedScopedProxyFactoryBean) 非手写 CGLIB; ContextRefresher.refresh **synchronized** (ContextRefresher.java:92) | 接受+精确化 ✅ |
| 7 项待验证断言 | 逐个 grep | 全部确认 (见上表, 含 C-9 路径修正 core/ 非 supplier/) | 通过 ✅ |
| hypermedia 包 (8 文件) | 设计决策测试 | RemoteResourceRefresher 等 HATEOAS 资源发现 — <10 文件不强制成域; 面试/生产双低 | **排除** ✂️ (记录备查) |
| loadbalancer/security (1 文件) | 设计决策测试 | OAuth2LoadBalancerClientAutoConfiguration 薄装配桥 | **排除** ✂️ |
| loadbalancer/aot (1 文件) | 规划对照 | LoadBalancerChildContextInitializer — 已并入 SCC-13 AOT 面 | 并入 ✅ |
| client/loadbalancer 49 文件 | 域覆盖面 | SCC-5 覆盖面正确; 类面可扩充 (DeferringLoadBalancerInterceptor/LoadBalancerClientsProperties 等) | 接受+扩充 ✅ |

---

## 一、入口点与主线 (待 Pass 0 确认)

`@EnableDiscoveryClient → EnableDiscoveryClientImportSelector → @Import → ...` (服务发现主线) / `@LoadBalanced → LoadBalancerInterceptor → BlockingLoadBalancerClient → ReactorLoadBalancer → ServiceInstanceListSupplier` (负载均衡主线) / `@RefreshScope → ContextRefresher.refresh()` (配置刷新主线) / `NamedContextFactory → 子上下文` (配置隔离主线)

## 二、域清单 (13 域: 7🔴 + 6🟡, 规划分级)

| # | 域 | 模块 | 核心主题 | 方案 |
|:--:|---|---|---|---|
| SCC-1 | **Bootstrap 上下文** | context/bootstrap | bootstrap.yml 加载链 / PropertySourceLocator SPI (Nacos/Apollo 实现点) / 父子上下文 | 🔴 A |
| SCC-2 | **@RefreshScope 热刷新** | context/scope (7 类: GenericScope/ScopeCache/StandardScopeCache/ThreadLocalScopeCache/ThreadScope/refresh 子包 RefreshScope+RefreshScopeRefreshedEvent) + context/refresh (ContextRefresher/ConfigDataContextRefresher/LegacyContextRefresher/RefreshScopeLifecycle) | ScopedProxyFactoryBean 代理重建 (非手写 CGLIB, GenericScope:246) / ContextRefresher.refresh() synchronized (194 行) / ConfigurationPropertiesRebinder / RefreshScopeRefreshedEvent; **规划类 RefreshScopeBeanPostProcessor 不存在** (BPP 职责在 GenericScope 内) | 🔴 A |
| SCC-3 | **服务发现抽象** | commons/client/discovery | DiscoveryClient 体系 / Composite 组合 / @EnableDiscoveryClient | 🔴 A |
| SCC-4 | **服务注册抽象** | commons/client/serviceregistry | ServiceRegistry / AbstractAutoServiceRegistration 注册时机 | 🔴 A |
| SCC-5 | **@LoadBalanced 客户端** | commons/client/loadbalancer | LoadBalancerInterceptor / BlockingLoadBalancerClient / reconstructURI | 🔴 A |
| SCC-6 | **ServiceInstanceListSupplier 体系** | loadbalancer/core+cache | 发现→缓存→健康检查链 / Caffeine 缓存 / Builder 组合 | 🔴 A |
| SCC-7 | **ReactorLoadBalancer 策略** | loadbalancer/core+annotation | RoundRobin/Random / LoadBalancerClientFactory / 指标 | 🔴 A |
| 🟡 SCC-8 | **RefreshEndpoint+事件** | context/endpoint (**顶层 endpoint 包**: RefreshEndpoint 依赖 ContextRefresher L27/38 — SCC-2 后继) + context/scope/refresh (RefreshScopeRefreshedEvent) | /actuator/refresh + RefreshScopeRefreshedEvent; **规划类 RefreshListener 不存在** (刷新监听职责在 RefreshScopeLifecycle) | 🟡 B |
| 🟡 SCC-9 | **配置加密** | context/encrypt | {cipher} 前缀 / TextEncryptorBindHandler | 🟡 B |
| 🟡 SCC-10 | **断路器抽象** | commons/client/circuitbreaker (8 类: CircuitBreaker/CircuitBreakerFactory/AbstractCircuitBreakerFactory/ReactiveCircuitBreaker/ReactiveCircuitBreakerFactory/ConfigBuilder/Customizer/NoFallbackAvailableException + observation) | CircuitBreaker 统一抽象 (Resilience4j/Sentinel 实现); **规划类 NoopCircuitBreaker 不存在** (4.3.2 无 Noop 变体) | 🟡 B |
| 🟡 SCC-11 | **BlockingLoadBalancer 重试** | loadbalancer/blocking | 阻塞式 + LoadBalancedRetryFactory | 🟡 B |
| 🟡 SCC-12 | **LoadBalancer 扩展策略** | loadbalancer/core | Weighted/ZonePreference/Hint/StickySession/Subset | 🟡 B |
| 🔴 SCC-13 | **NamedContextFactory** | context/named | 子上下文隔离 / ClientFactoryObjectProvider / AOT | 🔴 A |

## 三、执行顺序 (拓扑: 上下文 → 发现注册 → 负载均衡 → 收束)

**SCC-1 → SCC-2 → SCC-8 → SCC-9 → SCC-13 → SCC-3 → SCC-4 → SCC-5 → SCC-6 → SCC-7 → SCC-11 → SCC-12 → SCC-10**

> 拓扑理由: 上下文面 (Bootstrap/RefreshScope/Endpoint/加密) → NamedContextFactory (子上下文机制, 被 LoadBalancer/Feign 消费) → 服务发现/注册 (DiscoveryClient/ServiceRegistry) → @LoadBalanced 拦截 (消费发现) → LoadBalancer 核心 (Supplier 链 + 策略) → 阻塞式/扩展策略 → 断路器抽象 (收束应用面)。

## 四、知识网络图

```
← 复用: SofaJRaft 4.6 (Nacos CP 对照, 已收官) + ZK 4.3/Curator 4.5 (注册中心语义)
→ 引出: Gateway 5.5 + OpenFeign 5.6 (**NamedContextFactory 消费方实证**: FeignClientFactory/FeignClientSpecification 在 spring-cloud-openfeign 仓库, 非 feign 仓库) + Alibaba 5.7 + Nacos 5.8 + Sentinel 5.9
另见: Dubbo 5.2 (RPC 对照) + gRPC 5.3 + Feign 5.1 (已收官, LoadBalancer 消费方在 OpenFeign 面)
```

## 五、完成检查单

- [x] 顶层包扫描 ↔ 域清单覆盖矩阵 (13 域, 3 模块实证)
- [x] 09 审计: 版本修正 (2025.0.0 基准 → 4.3.2 实证), 7 项待验证断言开工逐条复验
- [x] 分工确认: Dubbo/gRPC 已占用, Commons 空闲 ✅
- [x] **二轮深度 REVIEW (2026-08-16)**: 数字口径 (主源 279/含测试 462 = 规划断言 ✅); 依赖拓扑 (NamedContextFactory ← LoadBalancerClientFactory L46 ✅, SCC-2→SCC-8 ✅); **跨域修正** (NamedContextFactory 消费方 = spring-cloud-openfeign 仓库 FeignClientFactory, 非 feign 仓库); **路径修正 2 处** (RefreshScope 在 scope/refresh/ 非 scope/, RefreshEndpoint 在顶层 endpoint 包); SCC-2 表述精确化 (ScopedProxyFactoryBean)
- [x] **三轮深度 REVIEW (2026-08-16, 类名穷举)**: 规划 70+ 核心类逐个 find 验证 — **3 个编造/过时类** (NoopCircuitBreaker/RefreshListener/RefreshScopeBeanPostProcessor, 全仓库零引用) 已标注受影响域 (SCC-2/SCC-8/SCC-10); **ParentContextApplicationContextInitializer 为外部依赖类** (org.springframework.boot.builder, Boot jar 提供 — 上轮误判编造已纠正); InstanceResponse 为返回值简写非类名; 其余 60+ 类全部实证存在
- [ ] SCC-1 ✅ (待办: Pass 0-3 + 深审 + harness + REVIEW)
- [x] **SCC-1 ✅ 2026-08-16** (方案 A: 大纲 6 节/30+ 锚点/20 问 + 深审 6 项 5 修正 + 时空溯源 (双轨制/三守卫) + **harness 12/12 自抓 2 缺陷** + KP; 核心: 三守卫/subcontext/Locator SPI/排序仲裁 addFirst 高优先级)
- [x] **SCC-2 ✅ 2026-08-16** (方案 A: 大纲 6 节/35+ 锚点/20 问 + 深审 5 项 4 修正 + 时空溯源 (gh-349/双刷新器分代) + **harness 11/11 自抓并发语义实证** + KP; 核心: LockedScopedProxyFactoryBean 双角色/读写锁/双阶段刷新)
- [x] **SCC-8 ✅ 2026-08-16** (方案 B: 大纲 5 节/20+ 锚点/20 问 + 深审 6 项 5 修正 + KP; 核心: 双触发面 (Endpoint+EventListener ready 守卫)/EnvironmentChangeEvent 同源/健康集成/四条件装配; **规划类 "RefreshListener" 过时 → 真实 RefreshEventListener**)
- [x] **SCC-9 ✅ 2026-08-16** (方案 B: 大纲 6 节/25+ 锚点/20 问 + 深审 5 项 4 修正 + KP; 核心: 双解密路径 (Binder 钩子+环境期)/双条件加密器装配/Failsafe delegate 占位/COLLECTION_PROPERTY 索引处理)
- [x] **SCC-13 ✅ 2026-08-16** (方案 A Hub: 大纲 6 节/30+ 锚点/20 问 + 深审 5 项 4 修正 + 时空溯源 (netflix#3101/openfeign#475 类加载器) + **harness 14/14 自抓 3 缺陷 (配置时序实证)** + KP; 核心: 双 Map 双检/registerBeans 三段/default. 前缀/含祖先查找/AOT 分支)
- [x] **SCC-3 ✅ 2026-08-16** (方案 A: 大纲 6 节/25+ 锚点/20 问 + 深审 5 项 4 修正 + 时空溯源 (probe 演进) + **harness 16/16 自抓短路语义实证** + KP; 核心: 四方法契约/Composite 排序短路/Simple 兜底/ImportSelector autoRegister 分支/心跳变更检测)
- [x] **SCC-4 ✅ 2026-08-16** (方案 A: 大纲 6 节/25+ 锚点/20 问 + 深审 5 项 4 修正 + 时空溯源 (management 演进/failFast) + **harness 17/17 触发链实证** + KP; 核心: 5 方法契约/WebServerInitializedEvent 触发/start 仪式/running 双检/钩子 4 事件/failFast)
- [x] **SCC-5 ✅ 2026-08-16** (方案 A: 大纲 6 节/20+ 锚点/20 问 + 深审 4 项 3 修正 + 时空溯源 (@since 4.1.2 时序分水岭) + **harness 7/7 轮询+重建实证** + KP; 核心: @Qualifier 标记/URL host 即服务名/双 execute/阻塞包装响应式/DeferringLoadBalancerInterceptor)
- [x] **SCC-6 ✅ 2026-08-16** (方案 A: 大纲 6 节/30+ 锚点/20 问 + 深审 5 项 4 修正 + 时空溯源 (@since 2.2.0 奠基+渐进扩展) + **harness 11/11 缓存链实证** + KP; 核心: 响应式 Supplier/Delegating 委托链/CacheFlux miss 回填/健康过滤双周期/Builder 20+ 装配)
- [x] **SCC-7 ✅ 2026-08-16** (方案 A: 大纲 6 节/20+ 锚点/20 问 + 深审 5 项 4 修正 + 时空溯源 (ocelli 灵感/惰性演进) + **harness 12/12 轮询数学实证** + KP; 核心: choose 响应式/& MAX_VALUE 位运算循环/随机种子防惊群/三态/每服务隔离)
- [x] **SCC-11 ✅ 2026-08-16** (方案 B: 大纲 6 节/20+ 锚点/20 问 + 深审 5 项 4 修正 + KP; 核心: RetryTemplate 包裹/同服换服双判定/状态码异常驱动/计数 `<` vs `<=` 不对称)
- [x] **SCC-12 ✅ 2026-08-16** (方案 B: 大纲 6 节/25+ 锚点/20 问 + 深审 6 项 0 修正 (锚点精确迭代效果) + KP; 核心: 六种扩展滤镜 (加权元数据/区域回退/hint 双源/cookie 粘性/分桶降采样/同实例偏好))
- [x] **SCC-10 ✅ 2026-08-16** (方案 B 收官: 大纲 6 节/20+ 锚点/20 问 + 深审 5 项 4 修正 + KP; 核心: run+fallback 契约/三层工厂族/Customizer once 幂等/观测装饰器)

## 🎉 阶段 5.4 Spring Cloud Commons **13/13 全量收官** (2026-08-16)

- 13 域全交付: SCC-1 Bootstrap / SCC-2 RefreshScope / SCC-8 RefreshEndpoint / SCC-9 加密 / SCC-13 NamedContextFactory / SCC-3 发现 / SCC-4 注册 / SCC-5 @LoadBalanced / SCC-6 Supplier / SCC-7 ReactorLB / SCC-11 重试 / SCC-12 扩展 / SCC-10 断路器
- harness 8 个 (🔴 域): MiniBootstrap 12/12 + MiniRefresh 11/11 + MiniContext 14/14 + MiniDiscovery 16/16 + MiniRegistry 17/17 + MiniLoadBalanced 7/7 + MiniSupplier 11/11 + MiniReactorLB 12/12
- 下一步: 写 HANDOFF-SPRING-CLOUD-COMMONS.md 交接文档 → 阶段 5.5 Gateway
- [ ] ... 逐域推进 (每域: KP → 大纲 → questions → 深审 → harness 🔴 / REVIEW → 回填 PLAN)
