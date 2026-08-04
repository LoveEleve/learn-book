# Spring Cloud Alibaba 源码学习范围规划

> **基准**: Spring Cloud Alibaba 2025.0.0.0 + Spring Boot 3.5.16 + Spring Framework 6.2.17
> **数据源**: spring-cloud-alibaba 仓库 454 文件逐包扫描（13 个 starter，5 个核心 starter 逐个读方法体验证）
> **边界**: 只覆盖 spring-cloud-alibaba 仓库（Spring Cloud 集成层），不包括 nacos/sentinel/seata/rocketmq 本身源码（第 3 类独立中间件）
> **my-xhs 关联**: my-xhs 用 Nacos（注册+配置）/Sentinel（限流）/Seata（分布式事务）/RocketMQ（消息）

---

## Nacos Config（58 文件，配置中心）

| # | 知识域 | 级别 | 核心类 | 核心问题 |
|---|---|---|---|---|
| A-1 | **NacosPropertySourceLocator 配置加载** | 🔴 | NacosPropertySourceLocator/NacosPropertySourceBuilder/NacosSnapshotConfigManager/NacosConfigDataLoader/NacosConfigDataLocationResolver/NacosPropertySourceRepository | `implements PropertySourceLocator`（commons C-1）。`locate()`（源码验证）：从 Nacos 拉取配置，按优先级加载（默认→带后缀 `.yml`→profile `dev`），返回 `CompositePropertySource`。`NacosPropertySourceBuilder.build()`构建属性源，`NacosSnapshotConfigManager`快照管理。Boot 2.4+ `NacosConfigDataLoader.load()`（源码验证：`implements ConfigDataLoader`，`pullConfig()`拉取+`NacosPropertySourceRepository.collectNacosPropertySource()`收集）。`dataIdPrefix` 默认用 `spring.application.name`。面试必问"Nacos 配置怎么加载" |
| A-2 | **Nacos 配置动态刷新** | 🔴 | NacosConfigRefreshEventListener/SmartConfigurationPropertiesRebinder/RefreshBehavior/NacosConfigRefreshEvent/NacosAnnotationProcessor/@NacosConfig/@NacosConfigListener | Nacos 配置变更→`NacosConfigRefreshEvent`→`NacosConfigRefreshEventListener`发布`RefreshEvent`→`SmartConfigurationPropertiesRebinder.onApplicationEvent(EnvironmentChangeEvent)`重新绑定（源码验证：`SPECIFIC_BEAN` vs 全部两种模式，`spring.cloud.nacos.config.refresh-behavior`控制）。`NacosAnnotationProcessor`（BeanPostProcessor）处理`@NacosConfig`注解（Bean/字段级），`refreshed`参数控制是否监听。面试必问"Nacos 配置怎么热刷新" |

## Nacos Discovery（37 文件，服务注册发现）

| # | 知识域 | 级别 | 核心类 | 核心问题 |
|---|---|---|---|---|
| A-3 | **NacosDiscoveryClient 服务发现 + NacosServiceRegistry 服务注册** | 🔴 | NacosDiscoveryClient/NacosServiceDiscovery/NacosServiceRegistry/NacosAutoServiceRegistration/ServiceCache | `NacosDiscoveryClient implements DiscoveryClient`（C-5）。`getInstances()`（源码验证）：`NacosServiceDiscovery.getInstances()`→`namingService().selectInstances(serviceId, group)`+`hostToServiceInstanceList()`转换 Instance→ServiceInstance，`ServiceCache.setInstances()`缓存，异常时`failureToleranceEnabled`→`ServiceCache.getInstances()`容错。`NacosServiceRegistry implements ServiceRegistry`（C-6）：`register()`→`namingService.registerInstance(serviceId, group, instance)`，支持`failfast`。`NacosAutoServiceRegistration`继承`AbstractAutoServiceRegistration`（C-6）监听`WebServerInitializedEvent`自动注册。面试必问 |
| A-4 | **NacosLoadBalancer 权重负载均衡** | 🟡 | NacosLoadBalancer/NacosBalancer/LoadBalancerNacosAutoConfiguration/NacosLoadBalancerClientConfiguration/LoadBalancerAlgorithm | `NacosLoadBalancer implements ReactorServiceInstanceLoadBalancer`（C-10）。`choose(Request)`（源码验证）：`getInstanceResponse()`用`loadBalancerAlgorithmMap.get(serviceId).getInstance()`选择实例，支持多种算法。`NacosBalancer.getHostByRandomWeight()`按权重随机选择 |
| A-5 | **Nacos 容错 + 心跳 + 优雅关闭** | 🟡 | ServiceCache/NacosDiscoveryHeartBeatPublisher/NacosGracefulShutdownDelegate/NacosWatch/NacosDiscoveryHealthIndicator | `ServiceCache`服务实例缓存（Nacos 不可用时容错）。`NacosWatch`（源码验证：`SmartLifecycle`+`namingService.subscribe(service, group, cluster, eventListener)`订阅服务变更，`onEvent`处理`NamingEvent`）。`NacosGracefulShutdownDelegate`（源码验证：监听`ContextClosedEvent`+`gracefulShutdownWaitTime`等待时间）。`NacosDiscoveryHeartBeatPublisher`心跳 |

## Sentinel（56 文件，限流降级）

| # | 知识域 | 级别 | 核心类 | 核心问题 |
|---|---|---|---|---|
| A-6 | **Sentinel RestTemplate/Feign/Web 限流** | 🔴 | SentinelProtectInterceptor/SentinelRestTemplate/SentinelFeign/SentinelInvocationHandler/SentinelBeanPostProcessor/SentinelWebAutoConfiguration/SentinelWebInterceptor | **三路限流**（源码验证）：**RestTemplate**`SentinelBeanPostProcessor`（MergedBeanDefinitionPostProcessor）扫描`@SentinelRestTemplate`的 RestTemplate→添加`SentinelProtectInterceptor`（`SphU.entry(hostResource, EntryType.OUT)`+`urlCleaner`）。**Feign**`SentinelFeign`（Feign.Builder 自定义）替换`InvocationHandlerFactory`用`SentinelInvocationHandler`（JDK 动态代理+`SphU.entry`+`FallbackFactory`降级）。**Web MVC**`SentinelWebAutoConfiguration`注册`SentinelWebInterceptor`+`BlockExceptionHandler`。面试必问"Sentinel 怎么限流" |
| A-7 | **Sentinel 数据源** | 🟡 | SentinelDataSourceHandler/AbstractDataSourceProperties/NacosDataSourceProperties/ApolloDataSourceProperties/FileDataSourceProperties/RedisDataSourceProperties/ZookeeperDataSourceProperties/DataSourcePropertiesConfiguration | `SentinelDataSourceHandler`（源码验证）：`forEach`遍历数据源配置→`getValidField()`获取有效字段→**`validFields.size() != 1`时跳过+log.error（多数据源不加载）**→`getValidDataSourceProperties()`+`preCheck()`+`registerBean()`注册。`AbstractDataSourceProperties.postRegister()`：根据 6 种规则类型注册到对应 RuleManager（FLOW/DEGRADE/PARAM_FLOW/SYSTEM/AUTHORITY/GW_FLOW）。支持 Nacos/Apollo/File/Redis/Zookeeper 五种数据源 |
| A-8 | **Sentinel Gateway 限流 + 断路器** | 🟡 | SentinelSCGAutoConfiguration/SentinelGatewayFilter/BlockRequestHandler/SentinelGatewayBlockExceptionHandler/SentinelCircuitBreaker/SentinelCircuitBreakerFactory/ReactiveSentinelCircuitBreakerFactory | SCG 限流：`SentinelSCGAutoConfiguration`注册`SentinelGatewayFilter`+`SentinelGatewayBlockExceptionHandler`+`BlockRequestHandler`（源码验证）。`SentinelCircuitBreaker implements CircuitBreaker`（C-8）：`run(Supplier, Function)`执行，限流/熔断触发时`fallback.apply(ex)`降级。`SentinelCircuitBreakerFactory`创建断路器 |

## Seata（8 文件，分布式事务）

| # | 知识域 | 级别 | 核心类 | 核心问题 |
|---|---|---|---|---|
| A-9 | **Seata 分布式事务集成** | 🔴 | SeataFeignRequestInterceptor/SeataFeignClientAutoConfiguration/SeataRestTemplateInterceptor/SeataRestTemplateAutoConfiguration/SeataHandlerInterceptor/SeataHandlerInterceptorConfiguration | 三路透传 XID（源码验证）：**Feign**`SeataFeignRequestInterceptor.apply()`→`RootContext.getXID()`→`template.header(KEY_XID, xid)`；**RestTemplate**`SeataRestTemplateInterceptor.intercept()`→`headers.add(KEY_XID, xid)`；**Web 入站**`SeataHandlerInterceptor.preHandle()`从请求头提取 XID 绑定到 RootContext。面试必问"Seata 怎么跨服务传播事务" |

## RocketMQ Stream（33 文件，消息）

| # | 知识域 | 级别 | 核心类 | 核心问题 |
|---|---|---|---|---|
| A-10 | **RocketMQ Stream Binder** | 🟡 | RocketMQBinderAutoConfiguration/RocketMQMessageChannelBinder/RocketMQInboundChannelAdapter/RocketMQProducerMessageHandler/RocketMQMessageSource/RocketMQMessageConverter/RocketMQBinderHealthIndicator/ErrorAcknowledgeHandler/RocketMQTopicProvisioner | Spring Cloud Stream Binder for RocketMQ。`RocketMQMessageChannelBinder`（源码验证）：inbound=`RocketMQInboundChannelAdapter`（消费者）+ outbound=`RocketMQProducerMessageHandler`（生产者）+ `RocketMQMessageSource`（polling 消费）+ `ErrorAcknowledgeHandler`（错误确认）。`RocketMQMessageConverter`消息转换。my-xhs 用 RocketMQ（但可能不用 Stream Binder，直接用 rocketmq-client） |

---

## 淘汰清单

| starter | 文件数 | 理由 |
|---|---|---|
| spring-cloud-starter-alibaba-sidecar | 13 | Sidecar 模式（非 Java 服务接入），my-xhs 不用 |
| spring-cloud-starter-alibaba-schedulerx | 10 | SchedulerX（阿里云定时任务），my-xhs 用 XXL-Job |
| spring-cloud-starter-bus-rocketmq | 2 | 仅 2 文件，合并到 A-10 描述 |
| spring-cloud-alibaba-commons | 6 | 公共工具，不独立成域 |

---

## 统计

| | 数量 |
|---|---|
| 🔴 核心域 | **6** |
| 🟡 重要域 | **4** |
| 总计 | **10 域** |
| 预计产出文章 | 6 篇（🟡 子域在对应 🔴 中附带）|
| starter 覆盖 | 5 核心 starter（Nacos Config/Discovery + Sentinel + Seata + RocketMQ Stream）逐个方法体验证 |

## 与其他 Spring Cloud 规划的交叉引用

| commons 域 | alibaba 中引用 | 关系 |
|---|---|---|
| C-1 PropertySourceLocator | A-1 NacosPropertySourceLocator | Nacos 实现 PropertySourceLocator 注入配置 |
| C-2 @RefreshScope | A-2 Nacos 配置动态刷新 | Nacos 变更触发 RefreshEvent → @RefreshScope 刷新 |
| C-5 DiscoveryClient | A-3 NacosDiscoveryClient | Nacos 实现 DiscoveryClient 服务发现 |
| C-6 ServiceRegistry | A-3 NacosServiceRegistry | Nacos 实现 ServiceRegistry 服务注册 |
| C-8 CircuitBreaker | A-8 SentinelCircuitBreakerFactory | Sentinel 实现 CircuitBreaker 断路器 |
| C-10 ReactorLoadBalancer | A-4 NacosLoadBalancer | Nacos 实现 ReactorLoadBalancer 权重负载均衡 |

## 与独立中间件的边界

本规划只覆盖 **spring-cloud-alibaba**（Spring Cloud 集成层），不包括中间件本身源码：
- **Nacos**（注册中心/配置中心）—— 独立学习 nacos 仓库源码
- **Sentinel**（限流框架）—— 独立学习 sentinel 仓库源码（sca-lab 已有 14 篇 D 类分析）
- **Seata**（分布式事务）—— 独立学习 seata 仓库源码
- **RocketMQ**（消息队列）—— 独立学习 rocketmq 仓库源码
- spring-cloud-alibaba 做的是：把这些中间件集成到 Spring Cloud 生态（自动配置/注解支持/透传机制）
