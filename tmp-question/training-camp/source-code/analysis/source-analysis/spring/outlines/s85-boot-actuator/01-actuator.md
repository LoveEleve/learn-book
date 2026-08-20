# S-21 Actuator 端点 — @Endpoint → EndpointDiscoverer → WebMvcEndpointHandlerMapping

> 依赖 C-12/C-13 (Web MVC 内核) + C-4 | 🔴 Deep | 6 KP | [模式: 注解驱动 + 发现器 + 适配到 MVC]

**读者处境**: 加 actuator 依赖后, `/actuator/health`、`/actuator/env` 自动就有了 — 这些 HTTP 接口哪来的?一个 @Endpoint 注解的 Bean 怎么变成可访问的 URL?健康检查 HealthIndicator 怎么聚合?

### 1. 端点抽象 — @Endpoint 注解与操作

场景: 你写 `@Endpoint(id = "myinfo")` 的 Bean + `@ReadOperation` 方法 — 谁定义了这个契约?

源码路径:
- `Endpoint.java:57,64,79` — **注解**: `public @interface Endpoint`(L57); `String id() default ""`(L64, 端点 id 须遵循 EndpointId 规则); `Access defaultAccess() default Access.UNRESTRICTED`(L79, 默认访问级别)
- `annotation/ReadOperation.java:38` — **操作注解**: `@ReadOperation`/`@WriteOperation`/`@DeleteOperation` 标在端点方法上; `annotation/RequestPredicateFactory.java:139,141,144,146` — **determineHttpMethod(L139)**: WRITE→POST(L141), DELETE→DELETE(L144), 其余(READ)→GET(L146) — 决定 HTTP 方法
- `ExposableEndpoint.java:29,57` — **抽象**: `interface ExposableEndpoint<O extends Operation>`(L29); `getOperations()`(L57) — 端点 = id + 一组 Operation

关键设计: **Why 注解驱动而非接口？** 用户只需给类加 `@Endpoint(id)` 并把方法标 `@ReadOperation` 等 — 无需继承接口, 用注解声明"这是端点、这是操作、对应哪个 HTTP 方法", 解耦声明与实现; 一个类可有多个操作方法。[模式: 注解驱动]

数据流: 声明 `@Endpoint(id="myinfo") class MyInfoEndpoint { @ReadOperation String info() {...} }` → 该 Bean 及其方法带注解标记 → 交给 §2 发现器扫描。

### 2. 发现机制 — EndpointDiscoverer 扫描 Bean

场景: 注解标记好后, 谁把 @Endpoint Bean 变成统一的 ExposableEndpoint 对象?和自动装配什么关系?

源码路径:
- `annotation/EndpointDiscoverer.java:72,151,157` — **发现器**: `abstract class EndpointDiscoverer<E,O>`(L72); `discoverEndpoints`(L151) → `createEndpointBeans`(L157): 遍历容器中带 `@Endpoint`(及 @EndpointExtension)的 Bean, 按 EndpointId 归组 → 反射生成 Operation
- `web/annotation/WebEndpointDiscoverer.java:53` — **Web 版**: `extends EndpointDiscoverer<ExposableWebEndpoint, WebOperation>`(L53) — 在普通端点基础上加 HTTP 路径/谓词信息
- `WebEndpointAutoConfiguration.java:91,92,98` — **注册**: `@ConditionalOnMissingBean(WebEndpointsSupplier)`(L91) → `@Bean webEndpointDiscoverer(...)`(L92) → `new WebEndpointDiscoverer(...)`(L98) — 经自动装配(S-2)注册

关键设计: **Why 发现器 + 自动装配注册？** 端点数量开放(用户可加), 故用"扫描容器中带 @Endpoint 的 Bean"而非硬编码; WebEndpointAutoConfiguration 经 @AutoConfiguration 注册发现器, 复用 S-2 自动装配管线; 发现结果(ExposableWebEndpoint 集合)作为 WebEndpointsSupplier 暴露给下游。[模式: 发现器 + 自动装配]

数据流: WebEndpointAutoConfiguration → @Bean webEndpointDiscoverer(L92) → EndpointDiscoverer.discoverEndpoints(L151) → createEndpointBeans(L157) 扫描容器 @Endpoint Bean → WebEndpointDiscoverer.createEndpoint 转 ExposableWebEndpoint(含 WebOperation: 路径/HTTP 方法) → 作为 WebEndpointsSupplier 供 §3 映射。

### 3. Web 暴露 — WebMvcEndpointHandlerMapping 映射 + Health 示例

场景: ExposableWebEndpoint 怎么变成 `/actuator/{id}` 的 MVC 接口?Health 端点(健康检查)是典型例子?

源码路径:
- `servlet/AbstractWebMvcEndpointHandlerMapping.java:90,154,156,170` — **映射**: `extends RequestMappingInfoHandlerMapping`(L90, 复用 C-12/C-13 的 MVC 内核); `initHandlerMethods`(L154): 遍历每个端点及其操作 → `registerMappingForOperation`(L156/170) → `registerMapping` 用 `RequestMappingInfo.paths(...)`(L219) 建路径映射
- `servlet/WebMvcEndpointHandlerMapping.java:52` — **MVC 版**: `extends AbstractWebMvcEndpointHandlerMapping`(L52) — 注册为 handlerMapping Bean, 处理 /actuator 前缀请求
- `HealthEndpoint.java:41,64` — **示例**: `@Endpoint(id = "health")`(L41) + `@ReadOperation`(L64) — 通过 HealthIndicator(HealthContributor)聚合各组件健康状态, 是 actuator 最常用端点

关键设计: **Why 复用 RequestMappingInfoHandlerMapping？** 端点暴露复用 Spring MVC 的映射内核 — 把每个 WebOperation 用 RequestMappingInfo 注册成 handler, 端点就无缝接入既有 MVC(拦截器 C-12、异常解析 C-13 全部生效), 无需自建路由。**Why 路径 /actuator/{id}？** 默认 management.endpoints.web.base-path 前缀 /actuator + 端点 id。[模式: 适配到 MVC + 注解驱动]

数据流: /actuator/health 请求 → WebMvcEndpointHandlerMapping(initHandlerMethods 已注册映射) → 命中 HealthEndpoint 的 @ReadOperation → HealthEndpoint.getHealth → 遍历各 HealthIndicator 聚合 → 返回健康 JSON。自定义 @Endpoint 同流程, 路径 = base-path + id。

→ 引出 S-22: 测试自动配置 — Actuator 之后: @SpringBootTest/@WebMvcTest 切片自动配置(前置 C-16~C-19)。
