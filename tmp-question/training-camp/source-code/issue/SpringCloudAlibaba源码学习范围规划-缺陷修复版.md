# Spring Cloud Alibaba 源码学习范围规划（缺陷修复版）

> 基准：Spring Cloud Alibaba 2025.0.0.0 + Spring Boot 3.5.16 + Spring Framework 6.2.17
> 仓库：spring-cloud-alibaba（spring-cloud-alibaba-starters 下 5 个核心 starter）
> 边界：只覆盖 spring-cloud-alibaba（Spring Cloud 集成层），不包括 nacos / sentinel / seata / rocketmq 本身源码
> 目标：在既有 10 个域（6🔴+4🟡）基础上，重构成卷级路线，补齐总开篇、卷级分层、桥接关系与篇数统计。
> 本地源码：`/data/workspace/source-code/code/spring/spring-cloud-alibaba/`
> my-xhs 关联：Nacos（注册+配置）/ Sentinel（限流）/ Seata（分布式事务）/ RocketMQ（消息）

## 一、修复结论

原规划已经完成了 10 个域的扫描，但存在三个结构问题：

1. 10 个域被压缩成 6 篇，像 Spring 原始规划一样缺少独立的阅读位置。
2. 缺少“Spring Cloud Alibaba 在整条 Spring 圈里处于什么位置”的总开篇，读者直接进入 Nacos/Sentinel 类。
3. 没有卷级分层，集成层与独立中间件边界也容易混。

本修复版将路线调整为：

- 1 篇总开篇
- 6 篇主干层
- 1 篇集成层
- 2 篇补深层

共 **10 篇候选正文**。

## 二、卷级路线

### A. 总开篇

A-1. 为什么有了 Spring Cloud Commons，还要 Spring Cloud Alibaba

回答：Commons 定义抽象（DiscoveryClient / ServiceRegistry / PropertySourceLocator / CircuitBreaker / ReactorLoadBalancer），Alibaba 把这些抽象落到 Nacos / Sentinel / Seata / RocketMQ 四类中间件。

总开篇同时强调：

- 本卷只讲集成层（自动配置 / 注解支持 / 透传机制）
- 不重复 Nacos / Sentinel / Seata / RocketMQ 本体源码
- 与 `vol-spring-boot` 的自动配置、ConfigData、Actuator 主线桥接
- 与 `vol-spring` 的 refresh / 配置类解析 / AOP 主线桥接

### B. 主干层

B-1. `NacosPropertySourceLocator` 配置加载：PropertySourceLocator 实现、快照管理、ConfigData 集成
B-2. Nacos 配置动态刷新：`NacosConfigRefreshEvent`、`NacosConfigRefreshEventListener`、`SmartConfigurationPropertiesRebinder`
B-3. `NacosDiscoveryClient` + `NacosServiceRegistry`：服务发现与注册的 Alibaba 实现，并补 `NacosReactiveDiscoveryClient`（reactive 分支）与 `NacosServiceDiscovery` / `NacosServiceManager`
B-4. Sentinel 三路限流：RestTemplate / Feign / Web MVC 的 `SentinelProtectInterceptor` / `SentinelFeign` / `SentinelWebInterceptor`
B-5. Seata 三路透传 XID：`SeataFeignRequestInterceptor` / `SeataRestTemplateInterceptor` / `SeataHandlerInterceptor` 的 XID 传播，并补 `SeataFeignBuilderBeanPostProcessor`（Feign Builder 层注入）
B-6. `NacosLoadBalancer` 权重负载均衡：`NacosBalancer`、`loadBalancerAlgorithmMap`、`getHostByRandomWeight`

### C. 集成层

C-1. Sentinel 数据源与断路器：`SentinelDataSourceHandler`、`SentinelCircuitBreakerFactory`、`SentinelSCGAutoConfiguration`（Gateway 限流）

其中：

- `spring-cloud-circuitbreaker-sentinel` 是独立 starter，体现 `CircuitBreaker` / `CircuitBreakerFactory` 抽象在 Sentinel 上的实现
- Sentinel 数据源不止 Nacos：还有 Apollo / File / Redis / Zookeeper / Consul，经 `dataSourceFactoryBean` 注册到 RuleManager

### C-2（并入 C-1）。Nacos / Sentinel 健康接线

原规划遗漏：
- `NacosDiscoveryHealthIndicator`
- `NacosDiscoveryEndpoint`

分别把 Nacos 注册中心可用性接进 Actuator health、把 Nacos 服务实例信息暴露成 endpoint。建议并入 C-1 末尾，作为“集成层的运维接线”子节。

### D. 补深层

D-1. Nacos 容错 / 心跳 / 优雅关闭 / Watch 订阅：`ServiceCache`、`NacosWatch`、`NacosGracefulShutdownDelegate`
D-2. RocketMQ Stream Binder：`RocketMQMessageChannelBinder`、inbound / outbound adapter（my-xhs 可能不用 Stream Binder，可降级）

## 三、与 `SpringCloudCommons` 的桥接

| Alibaba 域 | Commons 域 | 桥接关系 |
|---|---|---|
| B-1 NacosPropertySourceLocator | C-1 PropertySourceLocator | Nacos 实现 PropertySourceLocator 注入配置 |
| B-2 Nacos 配置动态刷新 | C-2 @RefreshScope | Nacos 变更触发 RefreshEvent → @RefreshScope 刷新 |
| B-3 NacosDiscoveryClient / NacosServiceRegistry | C-5 DiscoveryClient / C-6 ServiceRegistry | Nacos 实现发现与注册抽象 |
| B-6 NacosLoadBalancer | C-10 ReactorLoadBalancer | Nacos 实现权重负载均衡 |
| C-1 SentinelCircuitBreakerFactory | C-8 CircuitBreaker | Sentinel 实现断路器抽象 |

## 四、与独立中间件的边界

本卷只覆盖 **spring-cloud-alibaba（集成层）**：

- **Nacos**（注册中心/配置中心）—— 独立学习 nacos 仓库源码
- **Sentinel**（限流框架）—— 独立学习 sentinel 仓库源码
- **Seata**（分布式事务）—— 独立学习 seata 仓库源码
- **RocketMQ**（消息队列）—— 独立学习 rocketmq 仓库源码

spring-cloud-alibaba 做的是：把这些中间件集成到 Spring Cloud 生态（自动配置/注解支持/透传机制）。

## 五、本地源码已确认的关键类

| 域 | 已确认源码 |
|---|---|
| B-1 | `NacosPropertySourceLocator.java`（nacos-config starter） |
| B-2 | `NacosConfigRefreshEventListener` / `SmartConfigurationPropertiesRebinder` / `RefreshBehavior` |
| B-3 | `NacosDiscoveryClient` / `NacosServiceRegistry` / `NacosAutoServiceRegistration` |
| B-4 | `SentinelFeign` / `SentinelInvocationHandler` / `SentinelProtectInterceptor` / `SentinelWebAutoConfiguration` |
| B-5 | `SeataFeignRequestInterceptor` / `SeataRestTemplateInterceptor` / `SeataHandlerInterceptor` |
| B-6 | `NacosLoadBalancer`（nacos-discovery starter loadbalancer 包） |
| C-1 | `SentinelDataSourceHandler` / `SentinelCircuitBreakerFactory` |
| D-1 | `NacosWatch` / `NacosGracefulShutdownDelegate` / `NacosDiscoveryHeartBeatPublisher` / `ServiceCache` |
| D-2 | `RocketMQMessageChannelBinder` / `RocketMQInboundChannelAdapter` / `RocketMQProducerMessageHandler` |

## 六、淘汰清单（沿用原规划）

| starter | 理由 |
|---|---|
| spring-cloud-starter-alibaba-sidecar | Sidecar 模式，my-xhs 不用 |
| spring-cloud-starter-alibaba-schedulerx | SchedulerX，my-xhs 用 XXL-Job |
| spring-cloud-starter-bus-rocketmq | 仅 2 文件，合并到 D-2 |
| spring-cloud-alibaba-commons | 公共工具，不独立成域 |

## 七、推荐首轮顺序

```text
A-1 为什么有了 Spring Cloud Commons，还要 Spring Cloud Alibaba
  -> B-1 Nacos 配置加载
  -> B-2 Nacos 配置动态刷新
  -> B-3 Nacos 服务发现与注册
  -> B-4 Sentinel 三路限流
  -> B-5 Seata 三路透传 XID
  -> B-6 NacosLoadBalancer 权重负载均衡
  -> C-1 Sentinel 数据源与断路器
  -> D-1 Nacos 容错 / 心跳 / 优雅关闭
  -> D-2 RocketMQ Stream Binder
```

## 八、统计

| 层 | 篇数 |
|---|---|
| 总开篇 | 1 |
| 主干层 | 6 |
| 集成层 | 1 |
| 补深层 | 2 |
| 合计 | 10 |

## 九、执行规则

1. 先写总开篇 A-1，不直接从 `NacosPropertySourceLocator` 开始。
2. 每篇必须回链到 `SpringCloudCommons` 对应的抽象域。
3. 每篇明确边界：只讲集成层，不重复中间件本体。
4. 先保持机制叙事体，Cloud 卷铺完后统一补源码证据层。
5. 每写完一篇先 review、修补，再继续下一篇。