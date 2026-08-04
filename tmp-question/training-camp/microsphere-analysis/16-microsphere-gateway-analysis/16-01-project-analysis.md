# 16-01：项目定位与模块结构

> **核心命题**：microsphere-gateway 到底是什么？一句话——**它是 Spring Cloud Gateway 的"端点级路由"扩展**：下游服务启动时把自身全部 HTTP 端点序列化进注册中心实例 metadata，网关用一条 `we://` 自定义 scheme 的路由自动发现、精确匹配并转发，把官方"服务名+路径前缀"级的自动路由（discovery.locator）推进到**端点级**。本文先纠正交接文档的一个错误假设，再摸清模块结构、依赖链、SPI 入口、开关体系，并与官方 SCG 做九维对比。

---

## 一、项目定位（先纠正一个错误假设）

**交接文档写"16 做网关层 Zone 路由，与 17 复用 ZoneContext"——不成立，已源码验证**：

- 主代码 `grep -ri zone` 只命中两处注释：
  - `WebEndpointMappingGlobalFilter.java:275` → `// TODO support ZonePreferenceFilter`
  - `WebEndpointMappingHandlerFilterFunction.java:164` → `// TODO support ZonePreferenceFilter`
- git 全历史（352 提交）中搜 `Zone`：除测试配置里的 Eureka `defaultZone` 外只有这两条 TODO，**没有任何 Zone 路由实现**。
- 网关层 Zone 优先的真实来源是 **17-multiactive 的全局 LoadBalancer 配置**（`@LoadBalancerClients(defaultConfiguration=...)` 对包括网关在内的所有客户端生效；需开启 17 的 `microsphere.spring.cloud.loadbalancer.customized=true` 与 `spring.cloud.loadbalancer.configurations=optimized-zone-preference` 门控）。16 无需、也没有自己实现 Zone 逻辑。详见 16-08。

**这个项目实际做的是**：

```
服务 A（provider）                        注册中心                网关（16）
  @EnableWebExtension                         │                     │
  → 收集所有 Controller/RouterFunction 端点   │                     │
  → 序列化为 WebEndpointMapping 列表          │                     │
  → attachMetadata 写入实例 metadata ──────►  │                     │
      (web.mappings = URL编码JSON)            │                     │
                                             │ 实例列表(含metadata) ──► we://all 路由
                                             │◄────────────────────  │  → 剥/{application}前缀
                                             │                      │  → RequestMappingInfo 匹配
                                             │                      │  → 选实例转发（自动发现端点）
```

**与官方 Spring Cloud Gateway 的定位差异**：官方网关的路由声明有两种——逐服务手写 `lb://xxx` + `Path=/xxx/**`，或用 `spring.cloud.gateway.discovery.locator.enabled=true` 让网关为注册中心里**每个服务自动生成** `lb://serviceId` 路由。但**后者只到"服务级"**：路径原样透传，网关不知道下游有哪些端点，`/user-service/a/b` 和 `/user-service/x/y` 一律转发。16 的 `we://all` 把自动路由推进到**端点级**：从实例 metadata 读出完整端点集，按 `RequestMappingInfo` 六维条件精确匹配并重写路径。代价是：只有发布 `web.mappings` metadata 的 microsphere 生态服务才能被这条路由覆盖（非生态服务可与标准 `lb://` 路由混排，见 16-08）。

**为什么需要它（设计动机）**：官方 discovery.locator 下，服务接口路径变更（`/v1/orders` → `/v2/orders`）时网关无需改配置，但**无法做端点级控制**（排除内部接口、按 method/content-type 过滤）；手写路由则能精确控制但每个服务都要维护一份。16 用"服务发布元数据 + 网关自动匹配"同时拿到两者：配置零维护 + 端点级可控（excludes）。这是 16 相对官方两条路线的完整定位。

---

## 二、模块结构（28 个主 Java 文件 + 2 个父 POM）

```
microsphere-gateway/                       version 0.2.17-SNAPSHOT
├── microsphere-gateway-parent/            # 父 POM：继承 microsphere-spring-cloud-parent 0.2.24
├── microsphere-gateway-dependencies/      # BOM：管理 3 个产物版本（commons/webflux/webmvc）
├── microsphere-spring-cloud-gateway-commons    # 8 文件：纯共享层，零网关依赖
│   ├── annotation/   ConditionalOnMicrosphereGatewayEnabled
│   │                 ConditionalOnMicrosphereWebEndpointMappingEnabled
│   ├── config/       ConfigUtils / WebEndpointConfig / WebEndpointConfigurationPropertiesBindListener
│   └── constants/    CommonConstants / CommonsPropertyConstants / RouteConstants
├── microsphere-spring-cloud-gateway-server-webflux # 13 文件：响应式网关（SCG WebFlux）
│   ├── autoconfigure/  GatewayAutoConfiguration / WebEndpointMappingGatewayAutoConfiguration
│   ├── context/        WebEndpointApplicationContextInitializer
│   ├── event/          DisabledHeartbeatEventRouteRefreshListenerInterceptor
│   │                   PropagatingRefreshRoutesEventApplicationListener
│   ├── filter/         DefaultGatewayFilterChain / WebEndpointMappingGlobalFilter
│   ├── handler/        CachingFilteringWebHandler / FilteringWebHandlerBeanDefinitionRegistryPostProcessor
│   ├── annotation/     ConditionalOnGatewayAvailable / ConditionalOnGatewayEnabled
│   ├── constants/      GatewayPropertyConstants
│   └── util/           GatewayUtils
└── microsphere-spring-cloud-gateway-server-webmvc  # 7 文件：Servlet 网关（SCG Server MVC）
    ├── autoconfigure/  WebEndpointMappingGatewayServerMvcAutoConfiguration
    ├── context/        WebEndpointApplicationContextInitializer
    ├── filter/         WebEndpointMappingHandlerSupplier / WebEndpointMappingHandlerFilterFunction
    ├── annotation/     ConditionalOnGatewayServerMvcAvailable / ConditionalOnGatewayServerMvcEnabled
    └── constants/      GatewayPropertyConstants
```

**双实现战略**：同一份路由逻辑（we:// 识别 → 剥前缀 → 端点匹配 → 实例选择）分别适配 SCG 的两代 API——

| 模块 | 对接的 SCG 产品 | 扩展机制 |
|------|----------------|----------|
| webflux | `spring-cloud-gateway-server-webflux`（传统响应式网关） | `GlobalFilter`（order 10149） |
| webmvc | `spring-cloud-gateway-server-mvc`（4.x 新增的 Servlet 网关） | `HandlerSupplier` SPI + `HandlerFilterFunction` |

两者的核心逻辑同构（匹配、excludes、缓存语义），但实现机制差异巨大，各自有独立问题（见 16-03/16-04）。共享 commons 里的常量与配置模型（`RouteConstants`、`WebEndpointConfig`）。

---

## 三、Maven 依赖链（16 与 02/03/04/05 的边界）

webflux 模块 pom 依赖（均为 **optional**，由使用方按需引入；CI 按 spring-cloud-2022~2025 四个 profile 矩阵构建，JDK 17/21/25）：

| 依赖 | 来源项目（对应分析模块） | 16 用它的什么 |
|------|--------------------------|----------------|
| `microsphere-spring-cloud-commons` | 05-microsphere-spring-cloud | `ServiceInstanceUtils`（metadata 编解码）、`ServiceInstancesChangedEvent`、`ReactiveDiscoveryClientAdapter`、`DiscoveryClientAutoConfiguration`、`FeaturesProperties` |
| `microsphere-spring-boot-core` | 04-microsphere-spring-boot | `ListenableBindHandlerAdapter`（配置绑定钩子） |
| `microsphere-spring-boot-webflux` / `-webmvc` | 04-microsphere-spring-boot | `WebFluxAutoConfiguration`/`WebMvcAutoConfiguration`、`ConditionalOnWebFluxAvailable`/`ConditionalOnWebMvcAvailable` |
| `microsphere-spring-web` / `-webflux` / `-webmvc` | 03-microsphere-spring | `WebEndpointMapping` 元数据模型、`WebEndpointMappingsReadyEvent`、`MonoUtils` |
| `microsphere-spring-context` | 03 | `EnableEventExtension`（事件拦截）、`ApplicationListenerInterceptor`、`ConfigurableApplicationContextInitializer` |
| `spring-cloud-starter-gateway` / `spring-cloud-gateway-server-*` | Spring Cloud 官方 | SCG 本体（4.1.x，对应 2024.0.x train） |
| `spring-cloud-commons` / `spring-cloud-loadbalancer` | Spring Cloud 官方 | `DiscoveryClient`、`LoadBalancerClientFactory` |
| `microsphere-java-*`（传递） | 02-microsphere-java | 断言/集合/JSON/URL 工具 |

**要点**：16 是纯"消费者"——它不定义 `WebEndpointMapping`（在 03），不做服务发现（在 05），不定义绑定钩子（在 04）。**16 只定义"网关如何消费这些能力"**。这决定了分析时的边界：涉及 03/05 内部机制的部分（如 resolver 收集链路、注册中心事件）直接引用对应分析文章（如 03-07 Web 端点元数据、05-01 服务注册），不重复展开。

**"均为 optional"的设计含义**：microsphere 的 jar **不携带** SCG/Spring Cloud 依赖——网关应用必须自行显式引入 `spring-cloud-starter-gateway`（配合 BOM 管理版本）。好处：无依赖冲突、可与任意 Spring Cloud train 组合（CI 四个 profile 的意义）；代价：**缺依赖/版本不匹配时启动期才暴露**，而非编译期。这是"扩展库"的标准姿态——**16 是可完全剥离的**：关掉三把开关或移除依赖后，网关回到纯官方 SCG，无任何残留行为（这也是分析 16 时的安全前提：它的侵入面全部收敛在自动配置 + 事件拦截两处）。

---

## 四、SPI 与自动装配入口（Boot 3 双通道 + 一条自定义 SPI）

三个 `META-INF` 文件决定了"这个库怎么被 Spring 发现"：

**webflux 模块**：

```
# META-INF/spring.factories
org.springframework.context.ApplicationContextInitializer=\
io.microsphere.spring.cloud.gateway.server.webflux.context.WebEndpointApplicationContextInitializer

# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
io.microsphere.spring.cloud.gateway.server.webflux.autoconfigure.GatewayAutoConfiguration
io.microsphere.spring.cloud.gateway.server.webflux.autoconfigure.WebEndpointMappingGatewayAutoConfiguration
```

**webmvc 模块**（多一条自定义 SPI）：

```
# spring.factories
org.springframework.cloud.gateway.server.mvc.handler.HandlerSupplier=\
io.microsphere.spring.cloud.gateway.server.webmvc.filter.WebEndpointMappingHandlerSupplier
org.springframework.context.ApplicationContextInitializer=\
io.microsphere.spring.cloud.gateway.server.webmvc.context.WebEndpointApplicationContextInitializer

# AutoConfiguration.imports
io.microsphere.spring.cloud.gateway.server.webmvc.autoconfigure.WebEndpointMappingGatewayServerMvcAutoConfiguration
```

三个注册通道的职责：

| 通道 | 注册什么 | 时机 |
|------|----------|------|
| `AutoConfiguration.imports` | 两个自动配置类 | Boot 3 自动装配（比 spring.factories 的 EnableAutoConfiguration 更规范） |
| `spring.factories`（ApplicationContextInitializer） | `WebEndpointApplicationContextInitializer`——**在任何绑定之前**注册配置绑定监听器（16-06 详解） | 上下文创建早期 |
| `spring.factories`（HandlerSupplier，仅 webmvc） | MVC 版自定义 scheme 的 SPI 入口（16-04 详解） | SCG Server MVC 的 HandlerDiscoverer 反射发现 |

**features.yaml 的真实用途（重要澄清）**：三个模块各带一个 `META-INF/config/default/features.yaml`：

```yaml
microsphere:
  spring:
    cloud:
      features:
        abstract:
          microsphere-spring-cloud-gateway-server-webflux:
            - io.microsphere.spring.cloud.gateway.server.webflux.handler.CachingFilteringWebHandler
            - ...WebEndpointMappingGlobalFilter
            - ...DisabledHeartbeatEventRouteRefreshListenerInterceptor
            - ...PropagatingRefreshRoutesEventApplicationListener
        named:
          microsphere-spring-cloud-gateway-server-webflux:
            - WebEndpointApplicationContextInitializer=io.microsphere.spring.cloud.gateway.server.webflux.context.WebEndpointApplicationContextInitializer
```

**它不是组件开关**——消费方是 05 模块的 `ConfigurationPropertyHasFeaturesAutoConfiguration`（`FeaturesProperties` → `HasFeatures` beans → **Spring Cloud actuator 的 `/features` 端点**），作用是把"当前应用启用了哪些 microsphere feature"暴露给监控。组件是否生效由自动配置类的条件注解决定（见下节），与 features.yaml 无关。

---

## 五、开关体系（三把开关，两级继承）

条件注解的元结构（全部源码验证）：

```
@ConditionalOnMicrosphereGatewayEnabled        ← 开关① microsphere.spring.cloud.gateway.enabled（默认 true）
   ├── 用于 GatewayAutoConfiguration
   └── 被 @ConditionalOnMicrosphereWebEndpointMappingEnabled meta 引用

@ConditionalOnMicrosphereWebEndpointMappingEnabled ← 开关② microsphere.spring.cloud.web-endpoint-mapping.enabled（默认 true）
   └── 先过开关①，再过开关②（两级继承：关总开关即可连带关闭端点映射）

@ConditionalOnGatewayAvailable（webflux）      ← 开关③ spring.cloud.gateway.enabled（matchIfMissing=true）
   └── meta：@ConditionalOnGatewayEnabled + @ConditionalOnWebFluxAvailable + @ConditionalOnClass(GatewayProperties)
```

| 开关 | 属性名 | 默认 | 控制什么 |
|------|--------|------|----------|
| ① microsphere 网关总开关 | `microsphere.spring.cloud.gateway.enabled` | true | `GatewayAutoConfiguration`（心跳拦截/事件转发/BDRPP 三组件） |
| ② 端点映射功能 | `microsphere.spring.cloud.web-endpoint-mapping.enabled` | true | `WebEndpointMappingGatewayAutoConfiguration`（GlobalFilter bean）及 MVC 对应配置 |
| ③ SCG 惯例开关 | `spring.cloud.gateway.enabled` | true（缺失即启用） | 与官方 GatewayProperties 同一前缀（`GatewayProperties.PREFIX`） |

**第三把开关其实还有一层**：`WebEndpointMappingGlobalFilter` bean 上还标了 SCG 的 `@ConditionalOnEnabledGlobalFilter`，按 SCG 惯例（`NameUtils` 反编译验证：类名去 `GlobalFilter`/`Filter` 后缀 → camelCase 转 canonical kebab-case → `spring.cloud.gateway.<name>.enabled`）推导出属性 **`spring.cloud.gateway.web-endpoint-mapping.enabled`**——这把开关与开关②**同名不同前缀**（`spring.cloud.gateway.` vs `microsphere.spring.cloud.`），是两个独立的开关，关哪个都能让过滤器失效。

**顺带发现的注释 bug**（webflux/webmvc 各一处）：`GatewayPropertyConstants.GATEWAY_ENABLED_PROPERTY_NAME` 的 javadoc 写"spring.cloud.gateway.server.webflux.enabled"（MVC 版写"spring.cloud.gateway.server.webmvc.enabled"），但代码实际生成 `PREFIX + ".enabled"` = `spring.cloud.gateway.enabled`（MVC 版 `spring.cloud.gateway.mvc.enabled`）——**注释与实现不一致**，IDE 配置提示会误导。

---

## 六、与官方 Spring Cloud Gateway 的对比（九维）

> 对比基线：SCG 4.1.x（2024.0.x train，本机源码/字节码验证）。官方自动路由指 `DiscoveryClientRouteDefinitionLocator`（由 `GatewayDiscoveryClientAutoConfiguration` 装配，开关 `spring.cloud.gateway.discovery.locator.enabled=true`；注：3.x 中该类名为 `DiscoveryClientRouteLocator`，4.1 已改名）。

| 维度 | 官方 SCG（lb:// 路线） | microsphere 16（we:// 路线） |
|------|------------------------|------------------------------|
| 路由声明 | 逐服务手写，或 discovery.locator 自动生成 `lb://serviceId` | 一条 `we://all`（或 `we://a,b` 显式订阅） |
| 端点信息 | **无**：网关不知道下游端点，路径原样透传 | **有**：从实例 metadata（`web.mappings`）读出完整端点集（六维条件） |
| 匹配粒度 | 路径级 predicate（Path/Host/Header...），到"服务+前缀"为止 | 端点级 `RequestMappingInfo`：path+method+param+header+consume+produce，最具体优先排序 |
| 路径处理 | 原样透传，context-path 靠 StripPrefix 等过滤器手工配 | 剥 `/{application}` 前缀 + 按命中的端点定义转发（context-path 支持已退化，见 16-03） |
| 实例选择 | `ReactiveLoadBalancerClientFilter` **响应式**异步（`lb://`） | WebFlux 版**阻塞**自选（`toFuture().get()` 卡事件循环，16-03）；MVC 版委托官方 `lb()` |
| 刷新机制 | `RouteRefreshListener` 心跳**值变化**时全量刷新（`HeartbeatMonitor.update` 去抖） | 拦截心跳 + 事件受控刷新（ContextRefreshed/RefreshScopeRefreshed/RefreshRoutesResultEvent）+ 双缓存（16-05） |
| 过滤器链 | 每请求合并 globalFilters + 路由 filters 并排序（每请求分配） | `CachingFilteringWebHandler` 按 routeId 缓存合并排序后的数组（16-05） |
| 端点级排除 | 无（只能整条路由控制） | `metadata.web-endpoint.excludes` 六维条件（16-06） |
| 适用前提 | 任意 Spring Cloud 服务 | **仅**发布 `web.mappings` metadata 的 microsphere 生态服务 |

**本质区别一句话**：官方把"路由"定义在**服务名 + 路径前缀**层（网关是转发器）；16 把"路由"定义在**端点集**层（网关是匹配器）——前者通用但笨，后者精确但绑定生态。

## 七、与官方 SCG 的集成关系（microsphere 动了官方哪些组件）

| 官方组件 | microsphere 动作 | 对应文章 |
|----------|------------------|----------|
| `RouteToRequestUrlFilter`（order 10000） | 保留 | 16-03（请求链路） |
| `ReactiveLoadBalancerClientFilter`（order 10150） | 保留，microsphere 过滤器先改写 URL | 16-03 |
| `FilteringWebHandler` | **同 bean 名替换**为 `CachingFilteringWebHandler` | 16-05（刷新机制） |
| `RouteRefreshListener`（心跳刷新） | **拦截**其对 Heartbeat/ParentHeartbeat 的响应（其余 3 类事件：ContextRefreshed/RefreshScopeRefreshed/InstanceRegistered 不受影响） | 16-05 |
| `GatewayProperties` 绑定过程 | **插入 BindListener** 二次绑定 excludes | 16-06 |

## 八、项目活性（判断分析价值）

- 创建于 2024-02-26，2024-03-27 首次提交代码；**2024-08 至 2025-10 沉寂 14 个月**，2025-10-27 起恢复密集开发（测试补全、MVC 支持、模块拆分），最近提交 2026-07-11（CI 自动合并）。
- GitHub 4 stars，单分支开发；git tag 从 v0.1.2 到 0.2.16（release-notes 自 v0.2.3 起记录），当前 SNAPSHOT 0.2.17。
- 主作者 mercyblitz（小马哥），单人维护风格；开发节奏"长时间休眠 + 短期爆发"，是学习型/演示型项目，**生产可用性需谨慎评估**（16-08 缺口表）。

## 九、小结（引用要点）

- **16 是什么**：SCG 端点级路由扩展，`we://` scheme + 注册中心 metadata 自动构建端点路由表，WebFlux/MVC 双实现、共享 commons。
- **16 不是什么**：不是网关层 Zone 路由（交接文档假设错误，全历史仅 2 条 TODO 注释）；Zone 优先由 17 的全局 LoadBalancer 配置提供。
- **28 个主文件**：commons 8 + webflux 13 + webmvc 7；纯消费者，核心能力全部来自 03/04/05。
- **注册三通道**：AutoConfiguration.imports（自动配置）+ spring.factories（Initializer/HandlerSupplier）+ features.yaml（**actuator 报告，非开关**）。
- **开关三把**：`microsphere.spring.cloud.gateway.enabled` ⊃ `microsphere.spring.cloud.web-endpoint-mapping.enabled`，外加 SCG 惯例 `spring.cloud.gateway.web-endpoint-mapping.enabled`（与第二把同名词不同前缀）。
- **与官方对比一句话**：官方路由定义在"服务名+路径前缀"层（网关是转发器），16 定义在"端点集"层（网关是匹配器）——官方 discovery.locator 只到服务级，16 的 we:// 是端点级自动路由，代价是绑定 microsphere 生态。
