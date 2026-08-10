# stage-1 · 第 19 节：服务网关稳定性设计 — 知识点提取

> 课程：stage-1 服务治理 第 19 节
> 来源 docs：`/data/workspace/java-training-camp/stage-1/docs/19. 第十九节：服务网关稳定性设计.md`
> 提取时间：2026-08-09 | 权重：核心（网关是流量入口/稳定性关键）

---

## 一、本节概览

- **技术域**：Spring Cloud Gateway 核心机制（Route/Predicate/Filter）+ 路由定位 + 安全
- **维度**：`[分布式问题]`（网关/流量入口）+ `[工程问题]`（框架机制）+ `[规范]`（Web 安全）
- **核心命题**：理解 Spring Cloud Gateway 的核心机制（Route/Predicate/Filter、路由定位、WebFlux 衔接）与网关稳定性/安全
- **知识点数**：8 个
- **前置**：Spring WebFlux、Spring Cloud Gateway 基本使用、路由/网关概念

## 前置条件清单
读者需先掌握：
1. **Spring WebFlux**（响应式，Gateway 基于它）
2. **Spring Cloud Gateway 基本使用**（路由配置）
3. **网关概念**（流量入口/路由/过滤）
4. **函数式接口**（Java 8 Predicate）
未达前置者，先补：Spring Cloud Gateway 入门 + Spring WebFlux 基础

## 掌握度
目标读者：**本人（读源码多，Spring/WebFlux 熟悉）** — 已确认
讲解策略：Gateway 机制直接讲（你熟悉）；补 Route/Predicate/Filter 三者关系

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 网关核心概念（Route/Predicate/Filter）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：网关概念
- **需求**：理解网关的三大基本元素
- **自主实现**：Route(路由，ID+目标URI+谓词+过滤器) + Predicate(匹配条件) + Filter(请求/响应处理)
- **参考实现**（docs）：**Route**（基本块，ID+destination URI+predicates+filters）；**Predicate**（Java 8 Function Predicate，输入 ServerWebExchange，匹配 HTTP 请求）；**Filter**（GatewayFilter 实例，请求/响应前后修改）
- **对比取舍**：三者关系——Route 由 Predicate 匹配 + Filter 处理；谓词匹配则路由生效
- **测试佐证**：`code/spring/spring-cloud-gateway` 的 Route/GatewayFilter

### KP-02 Route Predicate Factories（路由谓词工厂）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01
- **需求**：用谓词工厂配置路由匹配条件
- **自主实现**：用内建谓词工厂(After/Before/Path/Header 等)或自定义(RoutePredicateFactory)
- **参考实现**（docs）：核心 API `RoutePredicateFactory`；内建如 AfterRoutePredicateFactory（`After=2017-01-20...` 按时间匹配）、BeforeRoutePredicateFactory；函数式规范 `PredicateSpec`
- **对比取舍**：谓词工厂是"匹配条件"的可插拔机制；多种内建(时间/路径/头/参数)
- **测试佐证**：`code/spring/spring-cloud-gateway` 的 `RoutePredicateFactory` + AfterRoutePredicateFactory

### KP-03 GatewayFilter Factories（网关过滤器工厂）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01
- **需求**：用过滤器工厂处理请求/响应
- **自主实现**：用内建过滤器工厂(AddRequestHeader 等)或自定义(GatewayFilterFactory)
- **参考实现**（docs）：核心 API `GatewayFilterFactory`；内建如 AddRequestHeaderGatewayFilterFactory（`AddRequestHeader=X-Request-red, blue`）、AddRequestParameterGatewayFilterFactory；函数式规范 `GatewayFilterSpec`
- **对比取舍**：过滤器是"请求/响应前后处理"的可插拔机制；与 Predicate(匹配)配合
- **测试佐证**：`code/spring/spring-cloud-gateway` 的 `GatewayFilterFactory`

### KP-04 路由定位器（RouteLocator/RouteDefinitionRouteLocator）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-02/03
- **需求**：把路由定义加载为 Route（结合谓词/过滤器/配置）
- **自主实现**：RouteLocator 结合 GatewayProperties + 谓词工厂 + 过滤器工厂 + 路由定义，构建 Route
- **参考实现**（docs）：`RouteDefinitionRouteLocator`（实现 RouteLocator）——依赖 GatewayProperties + GatewayFilterFactory Beans + RoutePredicateFactory Beans + RouteDefinitionLocator；组件命名规则用 `NameUtils.normalizeRoutePredicateName`
- **对比取舍**：RouteDefinitionRouteLocator 把"路由定义"转为"Route"，是配置→路由的桥
- **测试佐证**：`code/spring/spring-cloud-gateway` 的 `RouteDefinitionRouteLocator`

### KP-05 路由定义定位器（RouteDefinitionLocator）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-04
- **需求**：从多种来源(配置/服务发现)获取路由定义
- **自主实现**：用 RouteDefinitionLocator 从配置或服务发现加载路由定义
- **参考实现**（docs）：`CompositeRouteDefinitionLocator`（组合多个 locator）+ `DiscoveryClientRouteDefinitionLocator`（服务发现，`spring.cloud.gateway.discovery.locator.enabled` 默认关闭）
- **对比取舍**：路由定义可来自配置(静态)或服务发现(动态，配合注册中心)；组合模式聚合多个来源
- **测试佐证**：`code/spring/spring-cloud-gateway` 的 CompositeRouteDefinitionLocator

### KP-06 Gateway 核心逻辑（RoutePredicateHandlerMapping/WebFlux 衔接）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Spring WebFlux、KP-04
- **需求**：理解 Gateway 如何与 Spring WebFlux 衔接，请求如何路由
- **自主实现**：用 WebFlux 的 HandlerMapping 机制引入 Gateway 路由
- **参考实现**（docs）：Spring Cloud Gateway 基于 **Spring WebFlux**，前端总控制器 **DispatcherHandler**，引入 **RoutePredicateHandlerMapping**(HandlerMapping)打通 WebFlux→Gateway：
  - DispatcherHandler → HandlerMapping → RoutePredicateHandlerMapping → RouteLocator(RouteDefinitionRouteLocator) → RoutePredicateFactory(判断路由) + GatewayFilterFactory(执行过滤)
- **对比取舍**：Gateway 复用 WebFlux 的 HandlerMapping 机制，用 RoutePredicateHandlerMapping 做路由匹配——请求经 DispatcherHandler→路由→过滤→下游
- **测试佐证**：`code/spring/spring-cloud-gateway` 的 `RoutePredicateHandlerMapping`

### KP-07 网关安全（Web Security/CORS/CSRF/Secure Headers）
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Web 安全、网关
- **需求**：网关层的安全（CORS/CSRF/安全头）
- **自主实现**：在网关层统一处理 CORS/安全头
- **参考实现**（docs）：Tomcat(CsrfPreventionFilter/CorsFilter)、Spring WebMVC(CorsConfiguration/CorsFilter)、Spring Security、Spring Cloud Gateway **SecureHeadersGatewayFilterFactory**（安全头）
- **对比取舍**：CORS/CSRF 在不同层实现；网关用 SecureHeaders 过滤工厂统一安全头
- **待验证**：SecureHeaders 具体实现

### KP-08 网关容错（小作业：熔断/限流）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：容错(第8节)、网关
- **需求**：网关层做容错（熔断/限流，第 8 节容错的落地）
- **自主实现**：在 Gateway 加熔断/限流过滤器（第 8 节容错模式）
- **参考实现**：docs 小作业"Gateway 结合 Resilience4j 熔断/限流"——按方法论 08，参考实现用 **Sentinel**（国内主流，第 8 节）+ 网关限流过滤器；另一个作业"整合 LoadBalancer"（第 11/12 节）
- **对比取舍**：网关容错=第 8 节容错在网关层落地；限流分层(第 7 节 KP-09 的 4 层模式中网关是第 1 层)
- **关联 microsphere**：`[待验证]` microsphere-gateway

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|
| 网关核心概念 | 分布式 | 核心 | P1 | 🔴 | High |
| Route Predicate 工厂 | 工程 | 核心 | P1 | 🟡 | High |
| GatewayFilter 工厂 | 工程 | 核心 | P1 | 🟡 | High |
| 路由定位器 RouteLocator | 工程 | 核心 | P1 | 🔴 | High |
| 路由定义定位器 | 工程 | 核心 | P1 | 🟡 | High |
| Gateway 核心逻辑/WebFlux | 工程 | 核心 | P1 | 🔴 | High |
| 网关安全 | 规范 | 核心 | P1 | 🟡 | High |
| 网关容错 | 分布式 | 核心 | P1 | 🟡 | High |

---

## 四、与 microsphere 的关联（参考实现已验证）

- **Gateway 源码**：`code/spring/spring-cloud-gateway`（RoutePredicateHandlerMapping/RouteDefinitionRouteLocator/GatewayFilterFactory/RoutePredicateFactory）
- **microsphere-gateway**：存在（stage-4），`[待验证]` 具体封装
- **服务发现路由**：DiscoveryClientRouteDefinitionLocator（配合注册中心）

---

## 五、本节小结（三层次视角）

**需求**：理解 Spring Cloud Gateway 核心机制（Route/Predicate/Filter、路由定位、WebFlux 衔接）与网关稳定性/安全。

**自主实现核心**：若我设计——
1. Route = ID + 目标URI + Predicate(匹配) + Filter(处理)
2. RouteLocator 结合谓词/过滤器工厂 + 配置构建 Route
3. 基于 WebFlux HandlerMapping 引入 RoutePredicateHandlerMapping
4. 网关层做容错(第8节)/安全(安全头)

**参考实现**：Spring Cloud Gateway（code/spring/spring-cloud-gateway 有源码）——核心类源码验证。小作业 Resilience4j 按方法论 08 用 Sentinel。

**对比取舍**：知识本体是"**Spring Cloud Gateway 核心机制**"。核心洞察：**Route/Predicate/Filter 三元素 + RouteLocator + WebFlux HandlerMapping 衔接**；网关是流量入口，容错/安全/负载均衡都在此落地。

**待验证汇总**：
- SecureHeaders 具体实现
- microsphere-gateway 具体封装
- Gateway 容错/负载均衡整合(小作业)

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：本节大部分为架构师发散；与 docs/前篇重复处已交叉引用。

### 完整认知：服务网关稳定性在真实架构中完整该讲什么

docs 覆盖了"Route/Predicate/Filter + 路由定位 + 安全"。作为架构师，这个主题完整还该包含：

1. **网关的架构定位**：网关是**流量总入口**，承担路由/负载均衡/容错/限流/安全/鉴权/灰度——是"三高"的集中治理点（第 7 节限流 4 层模式中网关是第 1 层）
2. **网关稳定性设计**：不只"路由机制"，而是**稳定性完整设计**——限流(第 8 节)、熔断降级、超时、重试、负载均衡(第 11/12 节)、健康检查——网关高可用决定全局可用性
3. **路由的动态化**：静态路由(配置) vs **动态路由**(服务发现/配置中心/规则)——路由动态更新(第 10 节配置动态变更衔接)
4. **网关 vs 客户端直连**：集中治理(网关) vs 分散(客户端)——网关集中但单点(要 HA)，客户端分散但无集中治理
5. **网关高可用**：网关本身要 HA(多实例/负载均衡)，避免单点；网关是入口，挂了全挂
6. **性能与容量**：网关是请求必经之路，性能要求高(WebFlux 响应式)、容量规划——网关瓶颈影响全局
7. **安全**：网关统一做鉴权/安全头/CORS/限流(防 DDoS)——安全在入口收敛

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 网关 vs 客户端直连 | 网关集中治理但单点(需 HA)；客户端分散但无集中治理 |
| 静态路由 vs 动态路由 | 静态简单；动态(服务发现/配置中心)灵活但复杂 |
| 网关容错/限流实现 | 用 Sentinel(第 8 节，国内主流)vs 其他——按生态 |
| 网关技术选型 | Spring Cloud Gateway(WebFlux)vs 其他——按生态 |
| 网关性能 vs 功能 | 功能全但性能/复杂度高；精简但可能不够 |

### 常见坑/反模式

1. **网关单点**：网关单实例，挂了全挂——网关要 HA(多实例 + 负载均衡)
2. **路由配置错误**：Predicate 匹配错/Filter 顺序错，请求路由到错误下游——仔细校验路由
3. **网关成为性能瓶颈**：网关承担太多/序列化开销，拖慢全链路——网关要精简/高性能
4. **缺少限流/熔断**：网关没做限流熔断，流量突发打垮下游(第 8 节)——网关层必须容错
5. **安全缺失**：网关没做鉴权/安全头/CORS，安全边界破——安全在入口收敛
6. **动态路由未接**：只用静态路由，服务新增/变更不生效——接服务发现/配置中心
7. **忽略网关可观测**：网关请求无指标/日志/链路，难排查——网关要可观测(第 13/17 节)

### 生态位置

- **流量入口**：网关是请求必经之路，路由/容错/负载均衡/安全/可观测的集中治理点（衔接第 8 节容错、第 11/12 节负载均衡、第 13 节可观测）
- **与客户端直连对比**：第 4 节(客户端负载均衡) vs 网关集中——两种调用模式
- **Spring Cloud Gateway**：基于 WebFlux(响应式)，code/spring/spring-cloud-gateway 有源码
- **服务发现路由**：配合注册中心(Nacos)动态路由

**架构师视角结论**：本篇不只是"配几个 Route/Predicate"，而是"**设计网关这个流量入口的稳定性**"——路由/容错/负载均衡/安全/HA/可观测集中于此，网关稳定性决定全局可用性。
