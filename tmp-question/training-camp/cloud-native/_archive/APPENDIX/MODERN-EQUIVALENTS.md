# segfault-lessons → 2026 现代等价对照表

> 基于 Explore-31 的完整调研结果，覆盖从 Spring Boot 1.x + Spring Cloud Dalston 到 2026 年的全部 API 迁移。

---

## Hystrix → Resilience4j + Istio

| Hystrix (lesson-8/9/10) | Resilience4j 2026 | Istio |
|--------------------------|-------------------|-------|
| `@HystrixCommand(fallbackMethod="f")` | `@CircuitBreaker(name="x", fallbackMethod="f")` | `DestinationRule` connectionPool |
| `@EnableHystrix` | `@EnableCircuitBreaker` (自动配置) | - |
| `execution.isolation.thread.timeoutInMilliseconds` | `@TimeLimiter` | `VirtualService.timeout` |
| `HystrixDashboard` | Micrometer + Grafana / Sentinel Dashboard | Kiali |
| Thread Pool 隔离 | `@Bulkhead(type=THREADPOOL)` | - (网络层，不管应用层线程) |

**学什么**：Hystrix lesson-10 手写 `HystrixCommand` 继承类帮你理解断路器模式。概念完全保留，生产用 Resilience4j。

---

## Eureka → Nacos + K8s Service

| Eureka (lesson-4/5) | Nacos | K8s Service |
|---------------------|-------|-------------|
| `@EnableEurekaServer` | `@EnableNacosDiscovery` + Server 部署 | - |
| `@EnableEurekaClient` | `@EnableDiscoveryClient` (自动适配) | K8s Service DNS |
| `eureka.client.serviceUrl.defaultZone` | `spring.cloud.nacos.discovery.server-addr` | - |
| 心跳续约 30s | 临时实例心跳 5s | Pod readinessProbe |
| 自我保护模式 | Distro AP 协议 | - |
| Eureka Dashboard | Nacos Console (localhost:8848) | Kuboard / Lens |

**临时实例 vs 持久实例**：微服务 Pod 用临时实例（心跳保活，超时自动摘除），数据库/网关用持久实例（手动注册注销）。

---

## Ribbon → Spring Cloud LoadBalancer

| Ribbon (lesson-6/7) | Spring Cloud LoadBalancer 2026 |
|---------------------|-------------------------------|
| `IRule` (ZoneAvoidanceRule) | `ServiceInstanceListSupplier` |
| `IPing` (自定义健康检查) | `DiscoveryClient` 健康检查 / K8s Probe |
| `@RibbonClient(name="x")` | 无需注解，自动配置 |
| `user-service.ribbon.listOfServers` | `spring.cloud.loadbalancer.configurations: zone-preference` |
| `NFLoadBalancerRuleClassName` | `@LoadBalancerClient(name="x", configuration=XConfig.class)` |

**自定义 LB**：从"继承 `AbstractLoadBalancerRule`" 改为"实现 `ServiceInstanceListSupplier.get()` 返回 Reactor `Flux<ServiceInstance>`"。

---

## Zuul → Spring Cloud Gateway

| Zuul (lesson-11) | Spring Cloud Gateway |
|------------------|---------------------|
| `@EnableZuulProxy` | 自动配置 / `@EnableGateway` |
| `ZuulFilter.run()` | `GatewayFilter.apply(exchange, chain)` |
| `preRoute()/route()/postRoute()/error()` | 无阶段划分，统一链式 `GatewayFilterChain.filter()` |
| `zuul.routes.*` | `spring.cloud.gateway.routes[].predicates` |
| Servlet 同步 I/O | WebFlux 异步非阻塞 |
| `HystrixIntegrationFilter` | `SpringCloudCircuitBreakerFilterFactory` |

**路由配置等价**：`zuul.routes.user-service-provider=/user-service/**` → `predicates: Path=/user-service/**` + `filters: StripPrefix=1`

---

## Zipkin/Sleuth → OpenTelemetry + Jaeger/Tempo

| Sleuth/Zipkin (lesson-15) | OpenTelemetry |
|---------------------------|---------------|
| `spring-cloud-starter-sleuth` | `opentelemetry-javaagent.jar`（零代码） 或 `micrometer-tracing-bridge-otel` |
| `@EnableZipkinServer` | Jaeger/Tempo 独立部署 |
| `TraceId`/`SpanId` (MDC) | OTel SDK 自动注入 `trace_id`/`span_id` |
| `Spring Cloud Stream + Rabbit` 上报 | OTLP gRPC/HTTP → Collector |
| 日志格式 `[app,traceId,spanId]` | Logback/Log4j2 整合 `%trace_id`/`%span_id` |

**最重要的变化**：OTel Java Agent 实现**零代码侵入**自动 instrumentation，不再需要手写 Sleuth 拦截器。

---

## Spring Boot 1.x → 3.x（10 项 API 变更）

| # | 1.x (lesson 代码) | 3.x 等价 |
|---|-------------------|----------|
| 1 | `EmbeddedServletContainerCustomizer` | `WebServerFactoryCustomizer<ConfigurableWebServerFactory>` |
| 2 | `javax.*` 包名 | `jakarta.*`（javax.persistence → jakarta.persistence） |
| 3 | `management.security.enabled=false` | `management.endpoints.web.exposure.include=*` |
| 4 | `spring-cloud-starter-hystrix` | `spring-cloud-starter-circuitbreaker-resilience4j` |
| 5 | `spring-cloud-starter-ribbon` | `spring-cloud-starter-loadbalancer` |
| 6 | `spring-cloud-starter-zuul` | `spring-cloud-starter-gateway` |
| 7 | `spring-cloud-starter-eureka-server` | Nacos / K8s Service |
| 8 | `spring-cloud-starter-sleuth` | `micrometer-tracing-bridge-otel` |
| 9 | `@EnableScheduling` | `spring.threads.virtual.enabled=true`（虚拟线程） |
| 10 | `@EnableHystrix` | `@EnableCircuitBreaker`（自动配置 Resilience4j） |

---

## 概念保留清单

以下 segfault-lessons 课程虽然 API 过时，但概念和设计模式永远有效：

| 课程 | 保留概念 | 为什么不过时 |
|------|---------|-------------|
| Hystrix lesson-10 | 手写 HystrixCommand 继承类 | 理解断路器 ThreadPool 隔离 + Fallback 模式 |
| Ribbon lesson-7 | 自定义 MyRule + MyPing | 负载均衡策略设计的本质不变 |
| Zuul lesson-11 | ZuulFilter 调用链 | 网关 Filter 链的设计模式不变 |
| Sleuth lesson-15 | Span 模型 + TraceId 传递 | 概念完全适用于 OTel |
| Config Server lesson-3 | Git 后端 + @ConfigurationProperties + ContextRefresher | 配置中心的拉取+刷新机制不变 |
| Bus lesson-14 | RemoteApplicationEvent 广播 | 分布式事件驱动的本质不变 |
