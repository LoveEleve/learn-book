# Spring Cloud Commons 源码学习范围规划（缺陷修复版）

> 基准：Spring Cloud 2025.0.0 + Spring Boot 3.5.16 + Spring Framework 6.2.17
> 仓库：spring-cloud-commons（3 子模块：spring-cloud-context、spring-cloud-commons、spring-cloud-loadbalancer）
> 边界：只覆盖 spring-cloud-commons 仓库，不包括 gateway / openfeign / alibaba
> 目标：在既有 13 个域（8🔴+5🟡）基础上，重构成卷级路线，补齐总开篇、卷级分层、桥接关系与篇数统计。
> 本地源码：`/data/workspace/source-code/code/spring/spring-cloud-commons/`

## 一、修复结论

原规划已经完成了 13 个域的扫描，但存在三个结构问题：

1. 13 个域被压缩成 8 篇，和 Spring 原始规划一样缺少独立的阅读位置。
2. 缺少“为什么有了 Spring Boot，还要 Spring Cloud Commons”的总开篇，读者只能直接进入类和方法。
3. 没有卷级分层，主干层、集成层、生产层混在域清单里。

本修复版将路线调整为：

- 1 篇总开篇
- 6 篇主干层
- 1 篇集成层
- 1 篇生产层
- 2 篇补深层

共 **11 篇候选正文**。原规划 8 篇无法覆盖 NamedContextFactory、LoadBalancer 扩展策略和断路器抽象等必须独立展开的域。

## 二、卷级路线

### A. 总开篇

A-1. 为什么有了 Spring Boot，还要 Spring Cloud Commons

回答：Spring Boot 已经解决了应用装配问题，但分布式应用还需要服务发现、配置刷新、负载均衡这些跨进程抽象；Commons 不是“又一个 Spring Boot”，而是分布式应用的基础抽象层。桥接到 `vol-spring-boot` 的 `SpringApplication.run()`、`ConfigData`、`@ConfigurationProperties` 等主线。

同时要解释三个 starter 的角色：

- `spring-cloud-starter-bootstrap`：显式开启 bootstrap 上下文
- `spring-cloud-starter-loadbalancer`：默认启用 LoadBalancer
- `spring-cloud-starter`：基础聚合

### B. 主干层

B-1. Bootstrap 上下文机制：`bootstrap.yml` 怎么加载，`PropertySourceLocator` 是什么
B-2. `@RefreshScope` 配置热刷新：CGLIB 代理重建、`ContextRefresher.refresh()`、`ConfigurationPropertiesRebinder`
B-3. `DiscoveryClient` 服务发现抽象：`CompositeDiscoveryClient`、`@EnableDiscoveryClient`
B-4. `ServiceRegistry` 服务注册抽象：`AbstractAutoServiceRegistration`、`WebServerInitializedEvent` 触发时机
B-5. `@LoadBalanced` / `LoadBalancerClient`：`LoadBalancerInterceptor`、`RestTemplate` 拦截、URI 重构
B-6. `ReactorLoadBalancer` 负载均衡策略：`RoundRobinLoadBalancer`、`RandomLoadBalancer`、`ServiceInstanceListSupplier`、`CachingServiceInstanceListSupplier`、`HealthCheckServiceInstanceListSupplier`

其中 B-3 还应补齐 reactive 分支：

- `ReactiveDiscoveryClient`
- `ReactiveCompositeDiscoveryClient`
- `NacosReactiveDiscoveryClient`（在 Alibaba 卷落地）

否则和 Alibaba 的响应式服务发现桥接会断掉。

### C. 集成层

C-1. `NamedContextFactory`：为每个服务 / FeignClient 创建独立子上下文，实现配置隔离

### D. 生产层

D-1. `RefreshEndpoint` / `RefreshEvent` / `RefreshScopeHealthIndicator`：刷新端点的 Actuator 暴露与健康检查

### D-2（并入 D-1）。Discovery 健康检查

原规划遗漏了 `DiscoveryClientHealthIndicator` / `DiscoveryHealthIndicator` / `ReactiveDiscoveryClientHealthIndicator`，它们把注册中心可用性接进 Actuator health。建议并入 D-1 作为“生产层健康检查”子节，并为 Alibaba 卷 `NacosDiscoveryHealthIndicator` 留回链。

### E. 补深层

E-1. 断路器抽象：`CircuitBreaker` / `CircuitBreakerFactory` / `ReactiveCircuitBreakerFactory`，Sentinel/Resilience4j 实现
E-2. LoadBalancer 扩展策略：`WeightedServiceInstanceListSupplier` / `ZonePreferenceServiceInstanceListSupplier` / `SubsetServiceInstanceListSupplier` / `HintBasedServiceInstanceListSupplier` / `RequestBasedStickySessionServiceInstanceListSupplier` / `SameInstancePreferenceServiceInstanceListSupplier` / `BlockingLoadBalancerClient`

注意：`LoadBalancerClientFactory` 本身就是 `NamedContextFactory` 的一个应用，C-1 必须与其显式桥接；这也是 Feign / LoadBalancer 隔离配置的同一机制。

### F. 可观测性接线（补深层新增）

原规划遗漏了 Metrics / Observation 接线，但它们与 `vol-spring-boot` 的 Actuator / Metrics 主线直接衔接：

F-1. `MicrometerStatsLoadBalancerLifecycle` / `LoadBalancerTags` / `LoadBalancerStatsAutoConfiguration`：LoadBalancer 指标采集
F-2. `ObservedCircuitBreaker` / `CircuitBreakerObservation*`：断路器 observation
F-3. `DiscoveryClient` 的 `HeartbeatMonitor` / `DiscoveryClientHealthIndicator` 联动：注册中心健康接线

## 三、与 `vol-spring-boot` 的桥接

| Commons 域 | Spring Boot 前置正文 | 桥接关系 |
|---|---|---|
| A-1 Bootstrap 上下文 | `18-configdata.md`、`05-springapplication-run.md` | Cloud 的 Bootstrap 基于 Boot 的 ConfigData 和 EnvironmentPostProcessor |
| B-2 @RefreshScope | `06-configurationproperties.md` | `ConfigurationPropertiesRebinder` 重新绑定 Boot 的 `@ConfigurationProperties` |
| B-1 PropertySourceLocator | `18-configdata.md` | 配置中心通过引导上下文注入 PropertySource |
| D-1 RefreshEndpoint | `21-actuator-endpoints.md` | /actuator/refresh 是 Actuator 端点的 Cloud 扩展 |
| B-3/B-4 DiscoveryClient / ServiceRegistry | `08-webmvc-autoconfiguration.md`、`09-servlet-webserver-autoconfiguration.md` | `AbstractAutoServiceRegistration` 监听 WebServerInitializedEvent |

## 四、推荐首轮顺序

```text
A-1 为什么有了 Spring Boot，还要 Spring Cloud Commons
  -> B-1 Bootstrap 上下文
  -> B-2 @RefreshScope 配置热刷新
  -> B-3 DiscoveryClient 服务发现抽象
  -> B-4 ServiceRegistry 服务注册抽象
  -> B-5 @LoadBalanced / LoadBalancerClient
  -> B-6 ReactorLoadBalancer 负载均衡策略
  -> C-1 NamedContextFactory
  -> D-1 RefreshEndpoint 生产层
  -> E-1 / E-2 补深层
```

## 五、与 Spring Cloud Alibaba 的桥接

| Commons 域 | Alibaba 域 | 桥接关系 |
|---|---|---|
| B-1 PropertySourceLocator | A-1 NacosPropertySourceLocator | Nacos 实现 PropertySourceLocator |
| B-2 @RefreshScope | A-2 Nacos 配置动态刷新 | Nacos 变更触发 RefreshEvent |
| B-3 DiscoveryClient | A-3 NacosDiscoveryClient | Nacos 实现 DiscoveryClient |
| B-4 ServiceRegistry | A-3 NacosServiceRegistry | Nacos 实现 ServiceRegistry |
| B-6 ReactorLoadBalancer | A-4 NacosLoadBalancer | Nacos 实现 ReactorLoadBalancer |
| E-1 CircuitBreaker | A-8 SentinelCircuitBreakerFactory | Sentinel 实现 CircuitBreaker |
| D-1 / F-1 监控接线 | NacosMonitoring | 把 LoadBalancer / Discovery 指标接入 Nacos 运维 |

## 六、执行规则

1. 先写总开篇 A-1，不直接从 `BootstrapApplicationListener` 开始。
2. 每篇必须明确属于哪个卷级层次。
3. 每篇正文必须回链到 `vol-spring-boot` 对应的前置正文。
4. 先保持机制叙事体，所有 Cloud 卷铺完后统一补源码证据层。
5. 每写完一篇先 review、修补，再继续下一篇。

## 七、统计

| 层 | 篇数 |
|---|---|
| 总开篇 | 1 |
| 主干层 | 6 |
| 集成层 | 1 |
| 生产层 | 1 |
| 补深层 | 2 |
| 可观测性接线 | 1 |
| 合计 | 12 |

原规划预计 8 篇，但 `NamedContextFactory`、断路器抽象、扩展策略和可观测性接线必须独立成篇。