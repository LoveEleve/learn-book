# 16-REQ：Spring Cloud Gateway 动态端点路由需求规格（v2，SCG v4.3.2 源码验证）

> v2 更新(Explore-17)：2个前提推翻——SCG v4.3.2已有opt-in filter链缓存 + supportsAsyncExecution是死代码 + 12个bug
>
> 基准：Java 17+，Spring Cloud Gateway v4.3.2，对照源码 `/data/workspace/source-code/code/spring/spring-cloud-gateway/`
>
> **⚠️ v2 修正**：
> - ~~"SCG 没有 filter 链缓存"~~ → SCG v4.3.2 已有 `routeFilterCacheEnabled`（opt-in,默认false）——微球的差异化=默认开启+事件预热+按routeId缓存
> - ~~"supportsAsyncExecution()=false 阻塞事件循环"~~ → 死代码。真正阻塞源是 `MonoUtils.getValue()` 的 `toFuture().get()`

---

## 项目定位

**Spring Cloud Gateway 需要你手动配 `routes[0].uri=lb://service-a`——每加一个微服务就要在 yml 里加一条路由。当服务数量上百时，路由配置膨胀到几千行。microsphere-gateway（28 文件）通过 `WebEndpointMappingGlobalFilter` 实现动态端点路由：只要服务注册了 `WebEndpointMapping`（03 的能力），网关自动发现它的全部 HTTP 端点并路由，不需要配 `routes:`。**

---

## 核心需求

### REQ-001：`we://` 协议——服务端点的动态路由

**问题**：SCG 的 `lb://service-name` 只能到"服务"粒度——你说 `lb://user-service`，它把所有请求都转发到 user-service。但如果 user-service 有 20 个端点（`GET /users`、`POST /users`、`DELETE /users/{id}`……），SCG 不分——问号打给 user-service。你想"`POST /users` 限流 10 QPS、`GET /users` 不限流"——SCG 做不到，因为它不知道 service-name 后面有哪些端点。

**产出**：`we://user-service` 协议——`WebEndpointMappingGlobalFilter`（GlobalFilter）拦截 `we://` schema 的请求 → 从 `DiscoveryClient` 查询 `WebEndpointMapping` 元数据（03 的能力）→ 匹配请求路径/方法/Header → LoadBalancer 选择实例 → 重写 URI 转发。

**状态**：[已验证实现]

---

### REQ-002：路由缓存 + 事件驱动的自动刷新

**产出**：Filter 监听 4 种事件自动重建路由缓存——`ContextRefreshedEvent`（启动）→ `RefreshRoutesResultEvent`（路由配置变更）→ `EnvironmentChangeEvent`（属性变更）→ `ServiceInstancesChangedEvent`（服务实例上下线）。缓存存于 `ConcurrentHashMap<routeId, List<RequestMappingContext>>`。

**状态**：[已验证实现]

---

### REQ-003：端点排除过滤

**产出**：通过 `WebEndpointConfig.excludes` 配置（按 patterns/methods/headers/params/consumes/produces 过滤）——如屏蔽 `/actuator/**`、`/management/**` 不被网关暴露。

**状态**：[已验证实现]

### REQ-004：WebMVC 网关支持

**产出**：`WebEndpointMappingHandlerSupplier` 实现 SGC Server MVC 的 `HandlerSupplier` SPI——MVC 版本不通过 GlobalFilter 而是通过自定义 HandlerSupplier 注入。

**状态**：[已验证实现]

---

### REQ-005：Filter 链缓存——避免每次请求重新组装

**问题**：SCG 的 `FilteringWebHandler.handle()` 每次请求都重新 `combine` GlobalFilter + Route 级 GatewayFilter + 排序——在高并发场景下完全不必要的计算开销。Filter 链只在 Route 变更时才需要重建。

**产出**：`CachingFilteringWebHandler` 继承 SCG `FilteringWebHandler`——`handle()` 从 `ConcurrentHashMap<routeId, GatewayFilter[]>` 缓存读取已组装好的 filter 数组；`@EventListener(RefreshRoutesResultEvent)` 时重建缓存。Filer 数组重建逻辑通过 `routeLocator.getRoutes().subscribe()` 流式收集——`@EventListener` 异步触发，零用户感知。

**状态**：[已验证实现]

**已知问题**：
- 读取父类私有字段 `globalFilters`：`getFieldValue(this, "globalFilters")`——反射访问跨 SCG 版本脆弱

---

## 已知缺陷

| 缺陷 | 位置 |
|------|------|
| 🔴 WebFlux Filter `supportsAsyncExecution()=false` | `getValue(mono)` 阻塞 LoadBalancer choose 调用——事件循环线程被卡 |
| 🟡 强依赖 03/05 微球模块 | `WebEndpointMapping` 只能从微球注册的服务中获取——不是标准 Spring Cloud 能力 |
| 🟡 `we://` 不是 URL 标准协议 | 需要网关理解自定义 schema——不是开箱即用的 |

---

## 与 Spring Cloud Gateway 原生能力对比

| | SCG 原生 | 微球 |
|---|---|---|
| 路由粒度 | 服务级（`lb://service`） | **端点级**（`GET /users#UserController.getUsers`） |
| 路由发现 | 手动配 yml 或动态写 RouteDefinition | **自动发现**（服务注册时自动公布端点） |
| 路由刷新 | `RefreshRoutesEvent` | 4 种事件触发 |
| 端点过滤 | 需要通过 Route Predicate | 统一 `excludes` 配置 |
| 依赖 | SCG 标准 | 需要 03-microsphere-spring |

---

## 发散需求

### REQ-N01：WebFlux 异步 LoadBalancer——去掉事件循环阻塞

`getValue(mono)` → `mono.flatMap(response -> ...)` 真正异步。

### REQ-N02：端点级 Sentinel 流控

基于 `we://` 已获取的端点粒度元数据 → 自动注册 Sentinel 规则 `GET /users QPS=100`——不需要手写 Sentinel 规则。与 07-sentinel 联动。

### REQ-N03：路由审计日志

谁通过网关调用了哪个端点？每次 `we://` 匹配成功时记录：调用方 IP → 服务名 → 端点 → 响应状态码。

---

## 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| — | 2026-08-03 | REQ 文档编写 |
