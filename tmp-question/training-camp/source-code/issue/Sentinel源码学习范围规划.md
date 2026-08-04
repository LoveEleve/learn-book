# Sentinel 源码学习范围规划

> **基准**: Sentinel 1.8.9
> **数据源**: Sentinel 仓库 906 文件逐包扫描（sentinel-core 189 + sentinel-dashboard 111 + sentinel-adapter + sentinel-cluster + sentinel-extension + sentinel-transport）
> **边界**: 聚焦 sentinel-core（限流引擎核心）+ sentinel-adapter（框架集成），按需学习 cluster/dashboard
> **my-xhs 关联**: Sentinel 是 my-xhs 的限流框架，Spring Cloud Alibaba A-6 底层，sca-lab 已有 14 篇 D 类分析

---

## SPI 配置文件（权威调用图）

> **根据 `feedback_spi_config_file_is_real_call_graph.md`，META-INF/services 配置文件是权威调用图——必须在规划前读取**

`META-INF/services/com.alibaba.csp.sentinel.slotchain.ProcessorSlot` 定义 10 个 Slot（按加载顺序）：

1. `NodeSelectorSlot` — 节点选择
2. `ClusterBuilderSlot` — 集群构建
3. `LogSlot` — 日志
4. `StatisticSlot` — 统计
5. `AuthoritySlot` — 授权
6. `SystemSlot` — 系统规则
7. `FlowSlot` — 流控
8. `DegradeSlot` — 熔断降级
9. `DefaultCircuitBreakerSlot` — 默认断路器（第 9 个，sca-lab 第三轮 review 发现）
10. `ParamFlowSlot` — 热点参数限流

---

## 第 1 层：核心引擎（4 🔴）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| S-1 | **ProcessorSlot 链（SPI 调用图）** | 🔴 | slotchain + spi | AbstractLinkedProcessorSlot/DefaultProcessorSlotChain/ProcessorSlot/SlotChainBuilder/SlotChainProvider/ResourceWrapper/Spi/SpiLoader | `AbstractLinkedProcessorSlot`（`next`链表+`fireEntry→next.transformEntry→entry`+`fireExit→next.exit`）。`SlotChainBuilder`按 SPI 配置顺序构建 10 个 Slot。**Sentinel 自有 SPI 机制**（`@Spi`注解+`SpiLoader`加载器，非 JDK ServiceLoader）。SPI 配置文件是权威调用图。面试必问 |
| S-2 | **Context + Entry（资源访问入口）** | 🔴 | context | ContextUtil/CtSph/CtEntry/SphU/AsyncEntry | `CtSph implements Sph`（源码验证：`asyncEntryWithPriorityInternal`→`lookProcessChain`查找/创建 Slot 链→`chain.entry()`执行→`AsyncEntry.initAsyncContext()`初始化异步上下文）。**`asyncEntry.cleanCurrentEntryInLocal()`**（源码验证：清除 ThreadLocal entry——设计限制根源，导致`Tracer.trace`失败+`ContextUtil.enter`origin 丢失）。`asyncEntryWithNoChain`无链时入口。面试必问 |
| S-3 | **FlowSlot 流控 + LeapArray 滑动窗口** | 🔴 | slots/statistic + slots/block/flow | FlowSlot/FlowRuleChecker/FlowRule/DefaultController/WarmUpController/ThrottlingController/TrafficShapingController/LeapArray/WindowWrap/ArrayMetric/BucketLeapArray/MetricBucket/FutureBucketLeapArray/OccupiableBucketLeapArray/StatisticNode | `FlowSlot`（`entry`→`checkFlow`→`FlowRuleChecker`）。**3 种控制行为**（源码验证：均`implements TrafficShapingController`）：`DefaultController`（直接拒绝）/`WarmUpController`（预热）/`ThrottlingController`（排队等待匀速）。**LeapArray 滑动窗口**（`base/`子包，源码验证：`currentWindow(timeMillis)`→`calculateTimeIdx`计算桶索引→获取/创建`WindowWrap`+`isWindowDeprecated`过期判断+`values`遍历有效桶）。`MetricBucket`（PASS/BLOCK/RT/EXCEPTION）。`FutureBucketLeapArray`抢占式（优先级限流）。grade 0=线程数/1=QPS。面试必问 |
| S-4 | **DegradeSlot 熔断降级 + CircuitBreaker** | 🔴 | slots/block/degrade | DegradeSlot/DegradeRule/CircuitBreaker/AbstractCircuitBreaker/DefaultCircuitBreakerSlot/ExceptionCircuitBreaker/ResponseTimeCircuitBreaker/SimpleErrorCounter/SimpleErrorCounterLeapArray/SlowRequestCounter | `DegradeSlot`（`entry`→`cb.tryPass`+`exit`→`onRequestComplete`）。**`CircuitBreaker`接口**（`tryPass`/`currentState`/`onRequestComplete`）。**ExceptionCircuitBreaker**（源码验证：`SimpleErrorCounterLeapArray`统计异常数/比例）。**ResponseTimeCircuitBreaker**（源码验证：`SlowRequestCounter`+`slowCount.sum()`+`currentRatio = slowCount / totalCount`慢调用比例）。状态机 CLOSED→OPEN→HALF_OPEN。面试必问 |

## 第 2 层：扩展 Slot + 数据结构（3 🟡）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| S-5 | **Node 体系（统计数据结构）+ EagleEye** | 🟡 | node + slots/nodeselector + slots/clusterbuilder + eagleeye | StatisticNode/ClusterNode/DefaultNode/EntranceNode/ClusterBuilderSlot/NodeSelectorSlot/StatisticSlot/EagleEye/EagleEyeAppender/EagleEyeLogDaemon/EagleEyeRollingFileAppender | `NodeSelectorSlot`构建调用链路。`ClusterBuilderSlot`（`clusterNodeMap`静态 Map+双重检查锁定）。**`StatisticSlot`**（@Spi 注解，记录 PASS/BLOCK/RT/EXCEPTION 到 Node）。`StatisticNode`持有 LeapArray。**`eagleeye`**（Sentinel 自己的日志系统：`EagleEye`+`EagleEyeAppender`+`EagleEyeLogDaemon`+`EagleEyeRollingFileAppender`——独立日志框架，用于调用链和统计日志）|
| S-6 | **ParamFlowSlot 热点参数限流** | 🟡 | slots/block/flow/param | ParamFlowSlot/ParamFlowRule/ParamFlowChecker/ParamFlowSlot | 热点参数限流（如限制某个 userId 的 QPS）。`ParamFlowChecker`检查参数级规则。`CacheMap`缓存参数计数 |
| S-7 | **AuthoritySlot + SystemSlot + 规则管理** | 🟡 | slots/block/authority + slots/system + property | AuthoritySlot/AuthorityRule/AuthorityRuleManager/SystemSlot/SystemRule/SystemRuleManager/SentinelProperty/DynamicSentinelProperty/PropertyListener/SimplePropertyListener | `AuthoritySlot`黑白名单（基于 origin）。`SystemSlot`系统级规则（CPU/RT/线程数/入口 QPS）。**规则管理**（源码验证：`SentinelProperty`属性接口+`DynamicSentinelProperty`动态属性+`PropertyListener`监听器——规则变更时通过 SentinelProperty 通知 PropertyListener 更新规则）。**注意**：AuthoritySlot 依赖 Context.origin，在 Spring Boot 中可能被清除 |

## 第 3 层：适配器 + 集群 + 传输（3 🟡）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| S-8 | **适配器 + 扩展模块（15+ 适配器 + 14 扩展）** | 🟡 | sentinel-adapter + sentinel-extension | SentinelWebInterceptor/SentinelGatewayFilter/SentinelDubboProviderFilter/SentinelFeign/SentinelOkHttpInterceptor/SentinelGrpcInterceptor/SentinelReactorInterceptor/SentinelSofaRpcFilter/SentinelJaxRsFilter/SentinelSpringCloudGatewayV6x/sentinel-datasource-nacos/apollo/consul/etcd/eureka/redis/zookeeper/spring-cloud-config/sentinel-annotation-aspectj/sentinel-parameter-flow-control/sentinel-metric-exporter | **15+ 适配器**：WebMvc/WebFlux/Spring Cloud Gateway（v4x+v6x）/Dubbo（v2+v3）/Feign/OkHttp/Apache HttpClient/gRPC/Reactor/SofaRPC/JAX-RS/Motan/Quarkus。**14 扩展模块**：8 个 datasource（Nacos/Apollo/Consul/Etcd/Eureka/Redis/Spring Cloud Config/Zookeeper）+ annotation-aspectj + parameter-flow-control + metric-exporter + prometheus。**注意**：sentinel-web-servlet 用 javax.servlet（Boot 3.x Jakarta 不兼容）|
| S-9 | **集群限流（client + server + envoy-rls）** | 🟡 | sentinel-cluster | sentinel-cluster-client-default(26)/sentinel-cluster-server-default(71)/sentinel-cluster-common-default(19)/sentinel-cluster-server-envoy-rls(11) | 集群限流（Token Server 模式）。`sentinel-cluster-client-default`客户端（申请 token）。`sentinel-cluster-server-default`服务端（分发 token，71 文件）。`sentinel-cluster-common-default`公共。**`sentinel-cluster-server-envoy-rls`**Envoy RLS 集成（Service Mesh 限流）|
| S-10 | **传输（3 种实现）+ Dashboard** | 🟡 | sentinel-transport + sentinel-dashboard | sentinel-transport-common(41)/sentinel-transport-netty-http(12)/sentinel-transport-simple-http(11)/sentinel-transport-spring-mvc(7)/sentinel-dashboard(111) | **3 种传输实现**：`netty-http`（Netty HTTP 命令端口）/`simple-http`（简单 HTTP）/`spring-mvc`（Spring MVC）。`transport-common`（41 文件）公共（CommandCenter/HeartbeatSender）。`sentinel-dashboard`（111 文件）控制台（规则管理+监控+集群管理）|

---

## 淘汰清单

| 子模块 | 理由 |
|---|---|
| sentinel-benchmark（1 文件） | 基准测试 |
| sentinel-demo | 示例代码 |
| sentinel-logging | 日志适配，合并到 S-1 描述 |
| doc | 文档 |
| eagleeye（core 内） | EagleEye 调用链跟踪，合并到 S-5 描述 |

---

## 统计

| | 数量 |
|---|---|
| 🔴 核心域 | **4** |
| 🟡 重要域 | **6** |
| 总计 | **10 域** |
| 预计产出文章 | 4 篇（🟡 子域在对应 🔴 中附带）|
| 核心子模块覆盖 | sentinel-core(189) + sentinel-adapter + sentinel-cluster + sentinel-transport + sentinel-dashboard(111) |

## 与 Spring Cloud Alibaba 的关联

| Alibaba 域 | Sentinel 关联 | 关系 |
|---|---|---|
| A-6 Sentinel RestTemplate/Feign/Web 限流 | S-1~S-4 + S-8 适配器 | Alibaba 用 Sentinel 的 SphU.entry + Slot 链做限流 |
| A-7 Sentinel 数据源 | S-1 FlowRuleManager/DegradeRuleManager | 数据源推送规则到 RuleManager |
| A-8 Sentinel Gateway + 断路器 | S-4 CircuitBreaker + S-8 GatewayFilter | Gateway 限流用 GatewayFlowSlot |

## 与 sca-lab sentinel-lab 的关联

sca-lab 已有 14 篇 D 类分析覆盖：
- D-01~D-14：Slot 链/Node 体系/FlowSlot/DegradeSlot/StatisticSlot/AuthoritySlot/SystemSlot/ParamFlowSlot/NacosDataSource/EagleEye/集群限流/传输层
- A-01~A-10：官方功能验证（FlowRule/DegradeRule/fallback/ParamFlow 等）
- 已知限制：AsyncEntry cleanCurrentEntryInLocal + ContextUtil.enter origin 丢失 + sentinel-web-servlet Jakarta 不兼容

本规划聚焦**源码学习范围**（哪些域要学），sca-lab 聚焦**实验验证**（功能怎么用+踩坑），互补关系。
