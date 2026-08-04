# Spring Cloud Gateway 源码学习范围规划

> **基准**: Spring Cloud Gateway 2025.0.0 + Spring Boot 3.5.16 + Spring Framework 6.2.17
> **数据源**: spring-cloud-gateway 仓库 639 文件逐包扫描（核心子模块 server 239 文件 + server-mvc 101 文件）
> **边界**: 只覆盖 spring-cloud-gateway 仓库，不包括其他 Spring Cloud 仓库（commons/openfeign/alibaba 独立规划）
> **my-xhs 关联**: microsphere-gateway 底层用 SCG（WebFlux 版）

---

## 子模块 2.1：spring-cloud-gateway-server（路由/匹配/过滤器链/转发）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| G-1 | **路由机制** | 🔴 | route + route/builder + filter/WeightCalculatorWebFilter | Route/RouteDefinition/RouteLocator/RouteDefinitionRouteLocator/CachingRouteLocator/CompositeRouteLocator/RouteDefinitionRepository/RouteLocatorBuilder/WeightCalculatorWebFilter | 路由怎么定义和加载？`Route`（id/uri/order/predicate/gatewayFilters/metadata）vs `RouteDefinition`。`RouteLocator` 从多种来源合并路由。`RouteLocatorBuilder` Java DSL。**权重路由**：`WeightCalculatorWebFilter`（WebFilter 非谓词）在谓词匹配前按权重选择路由组，监听 `WeightDefinedEvent`。面试必问 |
| G-2 | **RoutePredicateHandlerMapping 路由匹配** | 🔴 | handler + handler/predicate + support/ipresolver | RoutePredicateHandlerMapping/FilteringWebHandler/AsyncPredicate + Path/Host/Method/Header/Cookie/Query/After/Before/Between/RemoteAddr/Weight/XForwardedRemoteAddr/ReadBody 16 个谓词工厂 + RemoteAddressResolver/XForwardedRemoteAddressResolver | `getHandlerInternal`→`lookupRoute` 遍历路由用谓词匹配。16 内置谓词工厂。`XForwardedRemoteAddressResolver` 处理代理链 IP。面试必问 |
| G-3 | **FilteringWebHandler + 过滤器链执行** | 🔴 | handler + filter/AdaptCachedBodyGlobalFilter | FilteringWebHandler/GatewayFilterChain/GlobalFilter/GatewayFilter/OrderedGatewayFilter/AdaptCachedBodyGlobalFilter/RemoveCachedBodyFilter/EnableBodyCachingEvent/DefaultGatewayFilterChain | `handle()`→`getCombinedFilters()`合并 GlobalFilter+路由 GatewayFilter 排序执行（源码验证：`routeFilterCacheEnabled`时`routeFilterMap.computeIfAbsent`缓存合并结果）→`DefaultGatewayFilterChain.filter()`链式执行。**请求体缓存**：WebFlux 流式请求体只能读一次，`AdaptCachedBodyGlobalFilter`用`CACHED_REQUEST_BODY_ATTR`缓存，监听`EnableBodyCachingEvent`。GlobalFilter 用`Ordered`接口排序非 @Order。面试必问 |
| G-4 | **核心 GlobalFilter 转发机制** | 🔴 | filter | RouteToRequestUrlFilter/ReactiveLoadBalancerClientFilter/NettyRoutingFilter/NettyWriteResponseFilter/WebsocketRoutingFilter/ForwardRoutingFilter | 转发链路（源码验证）：`RouteToRequestUrlFilter`（`UriComponentsBuilder`合并 URL，处理 lb://，**lb 协议 host==null 抛 `IllegalStateException("Invalid host:")`生产陷阱**）→ `ReactiveLoadBalancerClientFilter`（`choose()`选实例 + `reconstructURI()`重构 URI + 404 处理）→ `NettyRoutingFilter`（从 `GATEWAY_REQUEST_URL_ATTR` 取 URL + `HttpClient` 转发）。其他路由：Websocket/Forward/WebClient/Stream/Function。会话保持见 C-12。面试必问 |
| G-5 | **GatewayFilterFactory 过滤器工厂** | 🔴 | filter/factory + filter/headers + filter/cors + config/conditional | 39 个内置过滤器工厂（AddRequestHeader/RewritePath/SetStatus/Retry/CircuitBreaker/PrefixPath/StripPrefix/RedirectTo/CacheRequestBody/JsonToGrpc 等）+ headers 子包（XForwardedHeadersFilter/ForwardedHeadersFilter/RemoveHopByHopHeadersFilter/TrustedProxies）+ ConditionalOnEnabledFilter | 39 个内置过滤器工厂 + headers 过滤器。怎么自定义过滤器？`AbstractGatewayFilterFactory` 模板。`ConditionalOnEnabledFilter` 条件启用。面试会问"Gateway 内置过滤器有哪些" |
| G-6 | **服务发现路由** | 🟡 | discovery | DiscoveryClientRouteDefinitionLocator/DiscoveryLocatorProperties/GatewayDiscoveryClientAutoConfiguration | 基于服务名自动创建路由。`spring.cloud.gateway.discovery.locator.enabled=true`。microsphere-gateway 用此机制 |
| G-7 | **限流过滤器** | 🟡 | filter/ratelimit | RequestRateLimiterGatewayFilterFactory/RateLimiter/RedisRateLimiter | Redis 令牌桶限流。`spring.cloud.gateway.routes[].filters[].name=RequestRateLimiter`。面试会问"Gateway 怎么限流" |
| G-8 | **动态路由 + Actuator 端点** | 🟡 | route + actuate | InMemoryRouteDefinitionRepository/RedisRouteDefinitionRepository/GatewayControllerEndpoint/RefreshRoutesEvent | 动态路由增删查。Actuator 端点 `/actuator/gateway/routes`。`RefreshRoutesEvent` 路由刷新事件 |

## 子模块 2.2：spring-cloud-gateway-server-mvc（MVC 版本，101 文件）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| G-9 | **Gateway MVC（Servlet 架构）** | 🟡 | server-mvc | GatewayMvcFilter/Handler/RoutePredicate | Spring 6.1+ 新增的传统 Servlet 版本（非 WebFlux）。与 WebFlux 版的架构差异。my-xhs 用 WebFlux 版，MVC 版作为对比了解 |

---

## 淘汰清单

| 包/子模块 | 理由 |
|---|---|
| event 包 | 事件，合并到 G-1/G-8 描述 |
| support 包 | 支持工具，合并到其他域 |
| config 包 | 自动配置，合并到各域描述 |
| gateway-webflux/mvc | 旧版，5 文件以下 |
| gateway-proxyexchange-webflux/webmvc | ProxyExchange，1 文件 |
| server-webflux/server-webmvc | Marker 类，1 文件 |
| sample | 示例代码 |

---

## 统计

| | 数量 |
|---|---|
| 🔴 核心域 | **5** |
| 🟡 重要域 | **4** |
| 总计 | **9 域** |
| 预计产出文章 | 5 篇（🟡 子域在对应 🔴 中附带）|
| 子模块覆盖 | server(239文件) + server-mvc(101文件) |

## 与 commons 规划的交叉引用

| commons 域 | gateway 中引用 | 关系 |
|---|---|---|
| C-7 @LoadBalanced | G-4 ReactiveLoadBalancerClientFilter | Gateway 用 commons 的 LoadBalancer 做服务实例选择 |
| C-12 会话保持 | G-4 转发链路 | 会话保持在 LoadBalancer 层实现，Gateway 只是触发 |
| C-5 服务发现 | G-6 服务发现路由 | Gateway 用 commons 的 DiscoveryClient 自动创建路由 |
