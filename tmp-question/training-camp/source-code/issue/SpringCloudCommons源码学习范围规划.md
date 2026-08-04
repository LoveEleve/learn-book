# Spring Cloud Commons 源码学习范围规划

> **基准**: Spring Cloud 2025.0.0 + Spring Boot 3.5.16 + Spring Framework 6.2.17
> **数据源**: spring-cloud-commons 仓库 462 文件逐包扫描（3 核心子模块：spring-cloud-context + spring-cloud-commons + spring-cloud-loadbalancer）
> **边界**: 只覆盖 spring-cloud-commons 仓库，不包括其他 Spring Cloud 仓库（gateway/openfeign/alibaba 独立规划）

---

## 子模块 1.1：spring-cloud-context（Bootstrap/RefreshScope/配置刷新）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| C-1 | **Bootstrap 上下文机制** | 🔴 | bootstrap + bootstrap/config + context/util | BootstrapApplicationListener/BootstrapImportSelector/BootstrapConfiguration/BootstrapConfigFileApplicationListener/PropertySourceLocator/PropertySourceBootstrapConfiguration/ConditionalOnBootstrapEnabled/ConfigDataMissingEnvironmentPostProcessor | bootstrap.yml 怎么被加载？Spring Cloud 引导上下文跟 Boot 的 BootstrapContext 区别。**PropertySourceLocator** 是配置中心注入 PropertySource 的核心接口——Nacos/Apollo 都实现它。面试会问"bootstrap.yml vs application.yml"。`ParentContextApplicationContextInitializer` 父子上下文 |
| C-2 | **@RefreshScope 配置热刷新** | 🔴 | context/refresh + context/scope + context/properties + autoconfigure | RefreshScope/RefreshScopeBeanPostProcessor/ContextRefresher/ConfigurationPropertiesRebinder/RefreshAutoConfiguration | 配置中心变更后怎么热刷新 Bean？@RefreshScope 的 CGLIB 代理机制——销毁旧 Bean 重建新 Bean。`ContextRefresher.refresh()`（源码验证：`synchronized`同步）刷新 Environment + 触发 RefreshScope 重建 Bean。`ConfigurationPropertiesRebinder` 重新绑定 @ConfigurationProperties Bean。面试必问 |
| C-3 | **RefreshEndpoint + 刷新事件** | 🟡 | endpoint + endpoint/event | RefreshEndpoint/RefreshListener | /actuator/refresh 端点怎么触发刷新 + RefreshScopeRefreshedEvent 事件传播 |
| C-4 | **配置加密** | 🟡 | context/encrypt + bootstrap/encrypt | EncryptionBootstrapConfiguration/TextEncryptorBindHandler/TextEncryptorConfigBootstrapper | 配置中心敏感信息（密码）加密解密机制，`{cipher}` 前缀 |

## 子模块 1.2：spring-cloud-commons（服务发现/注册/负载均衡抽象）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| C-5 | **服务发现抽象** | 🔴 | client/discovery | DiscoveryClient/CompositeDiscoveryClient/SimpleDiscoveryClient/@EnableDiscoveryClient/EnableDiscoveryClientImportSelector | `DiscoveryClient extends Ordered`（源码验证）：`description()`实现描述+`getInstances(serviceId)`+`getServices()`。`CompositeDiscoveryClient`遍历所有 DiscoveryClient 委托调用收集结果。`@EnableDiscoveryClient`用`@Import(EnableDiscoveryClientImportSelector.class)`触发发现机制。面试会问"Spring Cloud 服务发现怎么抽象的" |
| C-6 | **服务注册抽象** | 🔴 | client/serviceregistry | ServiceRegistry/Registration/AbstractAutoServiceRegistration | 服务启动后怎么自动注册到注册中心。`AbstractAutoServiceRegistration` 监听 WebServerInitializedEvent 触发注册。面试会问"服务注册的触发时机" |
| C-7 | **@LoadBalanced 负载均衡客户端** | 🔴 | client/loadbalancer | LoadBalancerClient/BlockingLoadBalancerClient/@LoadBalanced/LoadBalancerRequest/LoadBalancerInterceptor/LoadBalancerRequestFactory | `@LoadBalanced`是`@Qualifier`限定符（源码验证）标记 RestTemplate/RestClient/WebClient。`LoadBalancerInterceptor.intercept()`拦截请求→`loadBalancer.execute(serviceName, requestFactory.createRequest(...))`选实例+重构 URI+转发。`LoadBalancerClient extends ServiceInstanceChooser`（`choose`/`execute`/`reconstructURI`）。面试必问 |
| C-8 | **断路器抽象** | 🟡 | client/circuitbreaker | CircuitBreaker/NoopCircuitBreaker/CircuitBreakerFactory | 断路器统一抽象（Spring Cloud CircuitBreaker），Resilience4j/Sentinel 实现这个接口 |

## 子模块 1.3：spring-cloud-loadbalancer（LoadBalancer 核心）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| C-9 | **ServiceInstanceListSupplier 体系** | 🔴 | core + cache | ServiceInstanceListSupplier/DiscoveryClientServiceInstanceListSupplier/CachingServiceInstanceListSupplier/HealthCheckServiceInstanceListSupplier/CaffeineBasedLoadBalancerCacheManager/DefaultLoadBalancerCacheManager | 服务实例列表的获取链路（发现→缓存→健康检查）。`DiscoveryClientServiceInstanceListSupplier`（源码验证：`Flux.defer(() -> Mono.fromCallable(() -> delegate.getInstances(serviceId)))`响应式包装 DiscoveryClient）。`ServiceInstanceListSupplierBuilder` 组合链。缓存层用 Caffeine。面试会问"LoadBalancer 怎么获取实例列表" |
| C-10 | **ReactorLoadBalancer 负载均衡策略** | 🔴 | core + annotation + support + stats | RoundRobinLoadBalancer/RandomLoadBalancer/ReactorLoadBalancer/@LoadBalancerClient/LoadBalancerClientFactory/MicrometerStatsLoadBalancerLifecycle | 负载均衡策略实现 + @LoadBalancerClient 自定义策略。`RoundRobinLoadBalancer`（源码验证：`AtomicInteger position`+`choose(Request)`→`getInstanceResponse()`）。`LoadBalancerClientFactory` 创建每个服务的 LoadBalancer 上下文。`MicrometerStatsLoadBalancerLifecycle` 采集指标。面试必问"轮询怎么实现" |
| C-11 | **BlockingLoadBalancer 阻塞式客户端** | 🟡 | blocking | BlockingLoadBalancerClient/blocking/retry/LoadBalancedRetryFactory | 阻塞式负载均衡（非响应式场景）+ 重试机制 |
| C-12 | **LoadBalancer 扩展策略** | 🟡 | core | WeightedServiceInstanceListSupplier/ZonePreferenceServiceInstanceListSupplier/HintBasedServiceInstanceListSupplier/RequestBasedStickySessionServiceInstanceListSupplier/SubsetServiceInstanceListSupplier | 加权/区域偏好/会话保持/子集等扩展策略 |

## 子模块 1.4：跨模块核心机制

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| C-13 | **NamedContextFactory 命名上下文** | 🔴 | context/named + loadbalancer/aot | NamedContextFactory/ClientFactoryObjectProvider/LoadBalancerChildContextInitializer | Spring Cloud 核心机制——为每个服务/FeignClient 创建独立子上下文，实现配置隔离。Feign 用它为每个 @FeignClient 隔离配置，LoadBalancer 用它为每个服务隔离 LoadBalancer 策略。`LoadBalancerChildContextInitializer` AOT 支持。面试会问"Feign 怎么为每个客户端隔离配置" |

---

## 淘汰清单

| 包 | 理由 |
|---|---|
| context/restart | RestartEndpoint，生产低频 |
| client/hypermedia | Hypermedia，低频 |
| env | 仅 EnvironmentUtils 1 文件 |
| context/health | 空包 |
| context/logging | 空包 |
| context/config | 仅 2 文件，合并到 C-2 |
| commons/configuration | 兼容性检查+TLS 配置，配置型功能淘汰 |
| client/actuator | FeaturesEndpoint，低频 |
| commons/publisher | Flux 工具 |
| loadbalancer/security | OAuth2，Security 相关淘汰 |

---

## 统计

| | 数量 |
|---|---|
| 🔴 核心域 | **8** |
| 🟡 重要域 | **5** |
| 总计 | **13 域** |
| 预计产出文章 | 8 篇（🟡 子域在对应 🔴 中附带）|
| 子模块覆盖 | spring-cloud-context(8子包) + spring-cloud-commons(7子包) + spring-cloud-loadbalancer(9子包) |
