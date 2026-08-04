# Spring Cloud OpenFeign 源码学习范围规划

> **基准**: Spring Cloud OpenFeign 4.3.2 + Spring Boot 3.5.16 + Spring Framework 6.2.17
> **数据源**: spring-cloud-openfeign 仓库 199 文件逐包扫描（核心子模块 openfeign-core）
> **边界**: 只覆盖 spring-cloud-openfeign 仓库，不包括 feign 本身源码（Netflix Feign 独立学习）
> **my-xhs 关联**: my-xhs 用 Feign 做服务间声明式 HTTP 调用（order→payment、payment→order 等）

---

## 知识域

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| F-1 | **@EnableFeignClients + FeignClientsRegistrar 注册机制** | 🔴 | 顶级 | EnableFeignClients/FeignClientsRegistrar/FeignClient/FeignClientSpecification | `@EnableFeignClients` 触发 `FeignClientsRegistrar`（`ImportBeanDefinitionRegistrar`），用 `ClassPathScanningCandidateComponentProvider` 扫描 `@FeignClient` 接口注册 BeanDefinition。`registerClientConfiguration` 注册每个客户端配置。面试必问"@FeignClient 怎么工作" |
| F-2 | **FeignClientFactoryBean 代理创建** | 🔴 | 顶级 + FeignClientsConfiguration | FeignClientFactoryBean/FeignBuilderCustomizer/Targeter/DefaultTargeter/FeignClientBuilder/FeignClientsConfiguration/FeignClientProperties/OptionsFactoryBean | `getObject()`→`getTarget()`创建代理。**两种路径**（源码验证）：无 URL→`loadBalance()`（从子上下文取`Client`，无则抛`"Did you forget to include spring-cloud-starter-loadbalancer?"`生产陷阱）；有 URL→unwrap `FeignBlockingLoadBalancerClient`取 delegate。`configureUsingConfiguration()`从子上下文获取 7 种组件（Logger.Level/Retryer/ErrorDecoder/Options/RequestInterceptor/ResponseInterceptor/QueryMapEncoder）+`AnnotationAwareOrderComparator.sort()`拦截器排序。`FeignClientsConfiguration` 14 个默认 @Bean。**FeignClientProperties**：connectTimeout/readTimeout/retryer/requestInterceptors/dismiss404 配置。面试必问 |
| F-3 | **SpringMvcContract Spring MVC 注解支持** | 🔴 | support + annotation | SpringMvcContract + PathVariable/RequestParam/RequestHeader/CookieValue/MatrixVariable/QueryMap/RequestPart 7 个 AnnotatedParameterProcessor | `SpringMvcContract extends Contract.BaseContract`（源码验证）：`processAnnotationOnMethod()`处理@RequestMapping+组合注解(@GetMapping等)。**`AnnotatedParameterProcessor`接口**：7 个处理器各自`processArgument()`把 MVC 参数注解(@PathVariable/@RequestHeader/@SpringQueryMap)映射到 Feign 请求参数。面试会问"为什么 Feign 能用 Spring MVC 注解" |
| F-4 | **Feign + LoadBalancer 集成** | 🔴 | loadbalancer | FeignBlockingLoadBalancerClient/RetryableFeignBlockingLoadBalancerClient/DefaultFeignLoadBalancerConfiguration/FeignLoadBalancerAutoConfiguration/LoadBalancerFeignRequestTransformer/XForwardedHeadersTransformer | `FeignBlockingLoadBalancerClient implements Client`（源码验证）：`execute()`→`choose(serviceId, lbRequest)`选实例→`reconstructURI()`重构 URI→`buildRequest()`→delegate 执行。**instance==null 返回 503 SERVICE_UNAVAILABLE**。**LoadBalancerLifecycle 回调**（onStart/onComplete）。`RetryableFeignBlockingLoadBalancerClient` 用 **Spring Retry**（`RetryTemplate`/`BackOffPolicy`/`LoadBalancedRetryPolicy`），非 Feign 原生 Retryer。HttpClient5/Http2Client/OkHttp 三种底层配置。面试必问"Feign 怎么做负载均衡" |
| F-5 | **FeignClientFactory 子上下文 + 配置隔离** | 🔴 | 顶级 + aot | FeignClientFactory/FeignClientSpecification/FeignChildContextInitializer/FeignClientBeanFactoryInitializationAotProcessor | `FeignClientFactory` 继承 `NamedContextFactory`（见 commons C-13），为每个 @FeignClient 创建独立子上下文，实现配置隔离。`FeignChildContextInitializer` AOT 支持。面试会问"Feign 怎么隔离每个客户端的配置" |
| F-6 | **Feign + CircuitBreaker 断路器集成** | 🟡 | 顶级 | FeignCircuitBreaker/FeignCircuitBreakerInvocationHandler/FeignCircuitBreakerTargeter/FallbackFactory/CircuitBreakerNameResolver/FeignCircuitBreakerDisabledConditions | `FeignCircuitBreakerInvocationHandler implements InvocationHandler`（JDK 动态代理）：`invoke()`→`resolveCircuitBreakerName()`→`factory.create()`→执行，异常时`fallbackFactory.create(throwable)`降级。**`FeignCircuitBreakerTargeter.target`三分支**（源码验证）：非 CircuitBreaker Builder→普通`feign.target()`；有 fallback→`targetWithFallback()`；有 fallbackFactory→`targetWithFallbackFactory()`。Resilience4j/Sentinel 集成 |
| F-7 | **Feign 编解码器** | 🟡 | support | SpringEncoder/SpringDecoder/ResponseEntityDecoder/FeignResponseAdapter/PageableSpringEncoder/PageableSpringQueryMapEncoder/HttpMessageConverterCustomizer/PageJacksonModule/SortJacksonModule | `SpringEncoder implements Encoder`用 Spring `HttpMessageConverter`编码。`SpringDecoder.decode()`用`HttpMessageConverterExtractor`提取响应体。**`FeignResponseAdapter implements ClientHttpResponse`**（适配器模式，Feign Response→Spring ClientHttpResponse）。`ResponseEntityDecoder`支持`ResponseEntity<T>`。分页编码支持 |
| F-8 | **Feign HTTP 客户端 + Gzip 压缩配置** | 🟡 | clientconfig + clientconfig/http2client + encoding | HttpClient5FeignConfiguration/Http2ClientFeignConfiguration/Http2ClientCustomizer/FeignAcceptGzipEncodingAutoConfiguration/FeignContentGzipEncodingAutoConfiguration/FeignAcceptGzipEncodingInterceptor/FeignContentGzipEncodingInterceptor/FeignClientEncodingProperties/FeignClientConfigurer | Feign 底层 HTTP 客户端选型：**HttpClient5**（`CloseableHttpClient`+`PoolingHttpClientConnectionManager`连接池）/ **Http2Client**（Java 11+ HttpClient+`Http2ClientCustomizer`回调）/ OkHttp。**Gzip 压缩**：`FeignContentGzipEncodingInterceptor`的`contentLengthExceedThreshold()`超过阈值才压缩请求体；`FeignAcceptGzipEncodingInterceptor`添加 Accept-Encoding: gzip。`FeignClientConfigurer` 自定义配置 |
| F-9 | **Feign + @RefreshScope 动态刷新** | 🟡 | 顶级 | RefreshableHardCodedTarget/RefreshableUrlFactoryBean/RefreshableUrl/PropertyBasedTarget | 配置刷新后 Feign URL 动态更新（源码验证）：`RefreshableHardCodedTarget extends HardCodedTarget`持有`RefreshableUrl`，`url()`每次从 RefreshableUrl 获取最新值。`RefreshableUrlFactoryBean implements FactoryBean<RefreshableUrl>`+@RefreshScope→配置刷新时重建 RefreshableUrl→代理 url()返回新 URL。`PropertyBasedTarget`懒加载从配置属性解析 URL（AOT 场景，`spring.cloud.openfeign.client.config.[clientId].url`）。配合 commons C-2 @RefreshScope |

---

## 淘汰清单

| 包/类 | 理由 |
|---|---|
| security（OAuth2AccessTokenInterceptor） | OAuth2，Security 相关淘汰 |
| hateoas（FeignHalAutoConfiguration） | HATEOAS HAL，低频 |
| FeignOAuth2Properties | OAuth2，淘汰 |
| FeignClientMicrometerEnabledCondition | Micrometer 条件，合并到 F-7 描述 |
| CachingCapability/FeignCachingInvocationHandlerFactory | 缓存能力，合并到 F-2 描述 |
| FeignLoggerFactory/DefaultFeignLoggerFactory | 日志，合并到 F-2 描述 |
| FeignErrorDecoderFactory | 错误解码，合并到 F-7 描述 |
| FeignFormatterRegistrar | 格式化，合并到 F-3 描述 |
| CollectionFormat | 集合格式，合并到 F-3 描述 |
| SpringQueryMap | @SpringQueryMap 注解，合并到 F-3 描述 |

---

## 统计

| | 数量 |
|---|---|
| 🔴 核心域 | **5** |
| 🟡 重要域 | **4** |
| 总计 | **9 域** |
| 预计产出文章 | 5 篇（🟡 子域在对应 🔴 中附带）|
| 子模块覆盖 | openfeign-core（顶级30+类 + 8子包）|

## 与其他 Spring Cloud 规划的交叉引用

| commons 域 | openfeign 中引用 | 关系 |
|---|---|---|
| C-13 NamedContextFactory | F-5 FeignClientFactory | FeignClientFactory 继承 NamedContextFactory，为每个 @FeignClient 创建子上下文 |
| C-7 @LoadBalanced | F-4 FeignBlockingLoadBalancerClient | Feign 的 Client 接口实现用 LoadBalancer 选实例 |
| C-2 @RefreshScope | F-9 RefreshableHardCodedTarget | Feign URL 配合 @RefreshScope 热刷新 |
| C-8 断路器抽象 | F-6 FeignCircuitBreaker | Feign 集成 Spring Cloud CircuitBreaker |

## 与 Feign 本身源码的边界

本规划只覆盖 **spring-cloud-openfeign**（Spring Cloud 对 Feign 的集成层），不包括 Feign 本身源码：
- Feign 核心（`feign.Feign`/`feign.Feign.Builder`/`feign.Contract`/`feign.Client`/`feign.Encoder`/`feign.Decoder`）—— Netflix Feign 独立学习
- spring-cloud-openfeign 做的是：把 Feign 集成到 Spring 生态（Spring MVC 注解支持/Spring 编解码/LoadBalancer/CircuitBreaker/RefreshScope）
