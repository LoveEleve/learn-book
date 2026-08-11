# stage-1 · 第 20 节：服务网关可观测性设计 — 知识点提取

> 课程：stage-1 服务治理 第 20 节
> 来源 docs：`/data/workspace/java-training-camp/stage-1/docs/20. 第二十节：服务网关可观测性设计.md`
> 提取时间：2026-08-09 | 权重：核心（网关微观架构 + 可观测性）
> **参考实现说明**：docs 用 Spring Cloud Sleuth（链路），按方法论 08/04，参考实现以 **Micrometer Tracing** 为主（Sleuth 旧名已并入）。

---

## 一、本节概览

- **技术域**：Spring Cloud 微观架构（RouteDefinition 接口设计）+ Gateway 请求执行流程 + 网关可观测性
- **维度**：`[工程问题]`（框架微观架构/SPI 设计）+ `[分布式问题]`（可观测性）
- **核心命题**：深入 Spring Cloud Gateway 微观架构（接口设计/组合模式/请求执行流程）与网关可观测性（监控/链路）
- **知识点数**：8 个
- **前置**：Spring Cloud Gateway(第19节)、Spring WebFlux、Micrometer(第13节)、链路(第17节)

## 前置条件清单
读者需先掌握：
1. **Spring Cloud Gateway 核心机制**（第 19 节：Route/Predicate/Filter）
2. **Spring WebFlux**（DispatcherHandler/HandlerMapping/WebHandler）
3. **Micrometer**（第 13 节：监控指标）
4. **链路追踪**（第 17 节：Tracing）
未达前置者，先补：第 19 节 Gateway + Spring WebFlux 响应式

## 掌握度
目标读者：**本人（读源码多，Spring/WebFlux 熟悉）** — 已确认
讲解策略：微观架构/SPI 设计直接讲（你熟悉）；补接口设计成员与组合模式

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 RouteDefinition 接口设计（微观架构）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：第 19 节 Route
- **需求**：理解 Route 的定义模型（配置层的接口设计）
- **自主实现**：设计路由定义的成员——id/predicates/filters/uri/metadata
- **参考实现**（docs）：`RouteDefinition` 成员——**id**(唯一标识)/**predicates**(多个谓词定义)/**filters**(多个过滤器定义)/**uri**(目标，本地或 Upstream)/**metadata**(扩展元信息，来源 Spring 配置)
- **对比取舍**：RouteDefinition 是"配置模型"，Route 是"运行时对象"——定义(配置)→实例(运行)
- **测试佐证**：`code/spring/spring-cloud-gateway` 的 `RouteDefinition.java`

### KP-02 PredicateDefinition / FilterDefinition 接口设计
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01、第 19 节谓词/过滤器
- **需求**：理解谓词/过滤器定义模型（name + args）
- **自主实现**：谓词/过滤器定义用 name(逻辑名) + args(配置参数)
- **参考实现**（docs）：`PredicateDefinition`/`FilterDefinition` 成员——**name**(条件/过滤器名称) + **args**(配置参数，来源 Spring 配置)；yaml 里 `After=2017-...` 中 After 是 name、时间串是 args
- **对比取舍**：定义层 name+args 是通用模型；运行时映射到 PredicateFactory/FilterFactory
- **测试佐证**：`code/spring/spring-cloud-gateway` 的 PredicateDefinition.java / FilterDefinition.java

### KP-03 SPI 集合依赖设计（Composite 组合模式）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：SPI、组合模式
- **需求**：理解 SPI 返回集合时的架构设计（有序/重复/Composite）
- **自主实现**：若 SPI 返回集合，考虑有序性/是否允许重复；单一依赖用 Composite 实现聚合
- **参考实现**（docs 关键）：SPI 集合依赖的设计策略——**Composite(组合)实现通常是 Primary 依赖**：
  - `CompositeRouteDefinitionLocator`（聚合多个 RouteDefinitionLocator，@Primary @Bean）
  - `CompositeDiscoveryClient`（聚合多个 DiscoveryClient，@Primary @Bean）
- **对比取舍**：**组合模式聚合多个 SPI 实现**——框架用 Composite 把多个实现合成一个 @Primary 入口（Spring 常见设计）
- **测试佐证**：docs 给出 CompositeRouteDefinitionLocator/CompositeDiscoveryClient 的 @Primary @Bean 代码

### KP-04 RouteDefinitionRouteLocator（路由定义→Route 转换）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：第 19 节、KP-03
- **需求**：把 RouteDefinition 集合转换为 Route 集合（核心方法 getRoutes）
- **自主实现**：getRoutes() 把 RouteDefinition → Route，失败处理
- **参考实现**（docs）：`RouteDefinitionRouteLocator`——核心成员(routeDefinitionLocator/predicates 映射/gatewayFilterFactories 映射/gatewayProperties)；核心方法 `getRoutes()` 用 `routeDefinitionLocator.getRouteDefinitions().map(convertToRoute)`，`gatewayProperties.isFailOnRouteDefinitionError()` 控制失败(onErrorContinue 忽略错误定义)
- **对比取舍**：getRoutes 是"定义→实例"转换 + 失败处理(容错单个错误定义)
- **测试佐证**：docs 给出 getRoutes() 完整源码

### KP-05 RoutePredicateHandlerMapping（WebFlux 衔接 + 匹配）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：WebFlux、第 19 节
- **需求**：理解 RoutePredicateHandlerMapping 如何匹配路由（基于 WebFlux HandlerMapping）
- **自主实现**：实现 WebFlux HandlerMapping，getHandlerInternal 匹配 Route
- **参考实现**（docs）：`RoutePredicateHandlerMapping` 基于 WebFlux `DispatcherHandler`/`HandlerMapping`；核心成员(webHandler=FilteringWebHandler 依赖 GlobalFilter 集合 + routeLocator)；核心方法 `getHandlerInternal` 用 `lookupRoute(exchange)` 匹配 Route(按 Route 的 AsyncPredicate)，匹配到则放 GATEWAY_ROUTE_ATTR 返回 webHandler
- **对比取舍**：**FilteringWebHandler ≈ WebMVC HandlerExecutionChain**（GatewayFilter ≈ HandlerInterceptor）；WebFlux Handler 是 WebHandler（与 WebMVC HandlerMethod 差异）
- **测试佐证**：`code/spring/spring-cloud-gateway` 的 RoutePredicateHandlerMapping.java

### KP-06 Gateway 请求执行过程（完整流程）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-04/05、WebFlux
- **需求**：理解一个 HTTP 请求在 Gateway 的完整执行流程
- **自主实现**：请求经 DispatcherHandler → HandlerMapping → WebHandler → 迭代 GatewayFilter/GlobalFilter
- **参考实现**（docs）：HTTP Request → DispatcherHandler → HandlerMapping(RoutePredicateHandlerMapping) → WebHandler(FilteringWebHandler) → HandlerAdapter(SimpleHandlerAdapter) → FilteringWebHandler.handle → **迭代执行 GatewayFilter(来自 RouteDefinition) 和 GlobalFilter**
- **对比取舍**：**过滤器链**——Route 的 GatewayFilter + 全局 GlobalFilter 依次执行（类似 WebMVC 拦截器链/第 9 节装饰器链）
- **测试佐证**：`code/spring/spring-cloud-gateway` 的 FilteringWebHandler + `code/spring/spring-framework` 的 SimpleHandlerAdapter(webflux)

### KP-07 网关监控设计（整合 Micrometer）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Micrometer(第13节)、网关
- **需求**：网关请求的指标监控（耗时/成功率/流量）
- **自主实现**：整合 Micrometer，采集网关请求指标（第 13 节指标模型）
- **参考实现**：docs"Gateway 整合 Micrometer 实现指标监控"——网关埋点指标(请求/过滤)；Spring Cloud Gateway 自带指标(Binder)
- **对比取舍**：网关是流量入口，监控请求/下游指标关键（第 13 节指标 + 第 15/16 节采集）
- **关联 microsphere**：`[待验证]` microsphere-gateway 监控

### KP-08 网关链路跟踪设计（整合 Tracing）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：链路(第17节)、网关
- **需求**：网关请求的链路追踪（跨服务串联）
- **自主实现**：整合 Micrometer Tracing，网关生成/传播 Trace ID（第 17 节）
- **参考实现**：docs"Gateway 整合 Sleuth 实现链路跟踪"——按 08 用 **Micrometer Tracing**（Sleuth 旧名已并入）；网关是链路起点(生成 Trace ID 传入下游)
- **对比取舍**：网关作为流量入口，是**链路起点**——生成 Trace ID 传播到下游服务(第 17 节 Trace ID 传播)
- **关联 microsphere**：`[待验证]` microsphere-gateway 链路

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|
| RouteDefinition 接口 | 工程 | 核心 | P1 | 🔴 | High |
| Predicate/FilterDefinition | 工程 | 核心 | P1 | 🟡 | High |
| SPI 集合/Composite 模式 | 工程 | 核心 | P1 | 🔴 | High |
| RouteDefinitionRouteLocator | 工程 | 核心 | P1 | 🔴 | High |
| RoutePredicateHandlerMapping | 工程 | 核心 | P1 | 🔴 | High |
| Gateway 请求执行流程 | 工程 | 核心 | P1 | 🔴 | High |
| 网关监控 | 分布式 | 核心 | P1 | 🟡 | High |
| 网关链路 | 分布式 | 核心 | P1 | 🟡 | High |

---

## 四、与 microsphere 的关联（参考实现已验证）

- **Gateway 微观架构**：`code/spring/spring-cloud-gateway` 的 RouteDefinition/PredicateDefinition/FilterDefinition/FilteringWebHandler/RoutePredicateHandlerMapping
- **WebFlux**：`code/spring/spring-framework` 的 SimpleHandlerAdapter(webflux)
- **microsphere-gateway**：存在（stage-4），`[待验证]` 监控/链路封装
- **链路**：Micrometer Tracing（Sleuth 过时并入，第 17 节）

---

## 五、本节小结（三层次视角）

**需求**：深入 Spring Cloud Gateway 微观架构（接口设计/组合模式/请求执行流程）与网关可观测性（监控/链路）。

**自主实现核心**：若我设计——
1. 定义层接口：RouteDefinition(id/predicates/filters/uri/metadata) + Predicate/FilterDefinition(name/args)
2. SPI 集合依赖用 Composite 组合模式聚合(@Primary)
3. getRoutes() 定义→Route 转换 + 失败处理
4. RoutePredicateHandlerMapping 匹配路由 → FilteringWebHandler 迭代过滤器
5. 网关监控(Micrometer) + 链路(Micrometer Tracing)

**参考实现**：Spring Cloud Gateway（code/spring/spring-cloud-gateway 微观架构类源码验证）+ WebFlux SimpleHandlerAdapter。Sleuth 按 08 用 Micrometer Tracing。

**对比取舍**：知识本体是"**Spring Cloud 微观架构 + 网关可观测性**"。核心洞察：**定义层(接口)与运行时分离 + Composite 组合模式 + 过滤器链执行**；网关是监控/链路的流量入口起点。

**待验证汇总**：
- microsphere-gateway 监控/链路封装
- Gateway 自带指标 Binder 细节

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：本节大部分为架构师发散；与 docs/前篇重复处已交叉引用。

### 完整认知：服务网关可观测性在真实架构中完整该讲什么

docs 覆盖了"微观架构接口 + 请求执行流程 + 监控/链路"。作为架构师，这个主题完整还该包含：

1. **网关可观测性的三支柱**：不只监控/链路，而是**指标(Metrics, 第13节)+ 链路(Trace, 第17节)+ 日志(Logs)**——网关是流量入口，三支柱在此完整落地，是所有下游服务可观测的起点
2. **微观架构的 SPI 设计原则**：Composite 组合本身见 KP-03，此处补其**通用原则框架**——SPI 返回集合时设计者需考虑：**①是否有序(Ordered)** ②是否允许重复 ③是否需 Composite 聚合(多实现合一) ④@Primary 主入口 ⑤失败/空集合处理——这是 Spring 框架设计的通用模式，可迁移到自研框架（docs 明确讲了这套设计思考）
3. **网关可观测的具体指标**：请求量/耗时(分位)/错误率/下游依赖状态/路由命中率——网关监控要覆盖哪些（第 13 节指标设计）
4. **网关链路的传播起点**：网关生成 Trace ID 传播到所有下游——**网关是分布式链路追踪的根**（第 17 节 Trace ID 传播），网关可观测决定全链路可观测
5. **请求执行流程的性能**：过滤器链(GlobalFilter + Route Filter)每层有开销——网关过滤器数量/复杂度影响性能（第 19 节网关性能）
6. **与网关稳定性联动**：可观测(本篇)服务稳定性(第 19 节)——先可观测才能发现/定位稳定性问题
7. **日志与请求上下文**：网关把 Trace ID/请求 ID 注入日志(MDC，第 17 节)，串联排查

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 网关可观测三支柱 | 指标(性能/量)+ 链路(跨服务)+ 日志(细节)——都要，网关是起点 |
| Composite 组合 vs 单个 SPI | Composite 聚合多实现(@Primary)统一入口；单个简单但只能一个实现——按是否多实现 |
| 网关过滤器链 | 过滤器多(功能全)但性能开销；精简(性能好)但功能少 |
| 网关链路用 Tracing | Micrometer Tracing(现代，第17节)vs Sleuth(旧)——用现代 |
| 网关可观测实现 | 内建 Binder vs 自写——按覆盖需求 |

### 常见坑/反模式

1. **网关可观测缺失**：网关没埋点，请求到网关就断了，全链路不可观测——网关是可观测起点
2. **Trace ID 未传播**：网关没生成/传播 Trace ID，下游链路断裂(第 17 节)——网关是链路根
3. **过滤器链过长**：GlobalFilter + 太多 Route Filter，性能开销大——精简过滤器
4. **SPI 集合设计混乱**：多个实现没 Composite 聚合，或顺序/重复没控制——按 docs 的 SPI 集合设计原则
5. **路由定义错误被忽略**：failOnRouteDefinitionError=false 时错误定义被静默忽略(onErrorContinue)——路由失效难发现
6. **只监控不告警**：网关指标采集了但没告警(第 13 节)——异常发现滞后
7. **日志无请求上下文**：网关日志没 Trace ID/请求 ID——难串联排查

### 生态位置

- **网关可观测**：第 13 节(指标)+ 第 17 节(链路)+ 日志——网关是流量入口，可观测三支柱的集中落地点
- **与网关稳定性**：第 19 节——可观测服务稳定性，先可观测才能定位问题
- **Spring Cloud Gateway 微观架构**：code/spring/spring-cloud-gateway 有源码验证；SPI/Composite 设计是 Spring 通用模式
- **链路**：Micrometer Tracing(Sleuth 过时，第 17 节)

**架构师视角结论**：本篇不只是"学 Gateway 微观架构"，而是"**设计网关这个流量入口的可观测性**"——指标/链路/日志三支柱在网关完整落地，网关是分布式可观测的起点，可观测又是网关稳定性的前提。
