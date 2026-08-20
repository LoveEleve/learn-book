# Spring Cloud Gateway — 知识网络化规划 (GW-1~GW-9, 09 怀疑审计后 v1)

> **日期**: 2026-08-16 | **依据**: issue/源码分析执行计划.md 阶段5.5 (9 域) + 09 对既有规划保持怀疑
> **⚠ v2 深度审计 (2026-08-16)**: 全包文件级扫描 + ≥400 行单类反向扫描 → **4 处域扩展**
> **⚠ v3 深度审计 (2026-08-16)**: support/25 全归类 + config/装配 + discovery/ + event/ + filter/ 顶层 → **再扩 2 处 + support 归类完成**
> **⚠ v4 深度审计 (2026-08-16)**: filter/factory 70 + 谓词 18 + route 15 + 其他模块文件级归类 → **响应缓存面 + 全部归类完成**
> **源码**: `/data/workspace/source-code/code/spring/spring-cloud-gateway` (**4.3.2**, pom.xml 实证; 浅克隆; 主模块 spring-cloud-gateway-server **239 文件** + webflux 5 + mvc 5)
> **定位**: 阶段 5.5 — RPC 与服务治理第五环 **响应式 API 网关 (WebFlux + Reactor Netty + 路由/过滤/谓词)**
> **知识网络**: 与 5.4 Spring Cloud Commons (另一 AI 并行, **以 SCC-PLAN.md 为交叉引用基准**: ReactiveLoadBalancerClientFilter 消费其 LoadBalancer/Discovery 抽象) + 5.1 Feign (HTTP 客户端对照) + 5.3 gRPC (Netty/HTTP2 传输对照) 互联; 为 5.7 Alibaba + 5.8 Nacos 提供网关基座
> **分工确认**: Gateway 目录无其他 AI 产物 (2026-08-16 核查) — 本会话开工 ✅; 并行者: Dubbo (5.2)/SCC (5.4)

---

## 〇、09 怀疑审计表 (Spring Cloud Gateway, 2026-08-16) — 必读

| 既有规划断言 | 验证动作 | 证据 | 结论 |
|---|---|---|---|
| 域清单 9 个 | 模块扫描 | 主模块 server 239 文件: route/ (15) + filter/ (21+factory 族) + handler/ (3) + config/ (27) + discovery/ (3) + event/ (7) + support/ (25) + actuator — 9 域面全覆盖 | **接受** ✅ |
| GW-1 "RoutePredicateHandlerMapping→RouteLocator→Route" | handler/ + route/ | RoutePredicateHandlerMapping (194) + FilteringWebHandler (196) + route/ 15 文件 (Route/RouteLocator/RouteDefinition 族 + RouteDefinitionRouteLocator 233 + CachingRouteLocator/CompositeRouteLocator) | **接受** ✅ |
| GW-2 "FilteringWebHandler→DefaultGatewayFilterChain" | handler/ | FilteringWebHandler (196) 存在; **GlobalFilter 实现 15 个** (NettyRoutingFilter 306/ReactiveLoadBalancerClientFilter/AdaptCachedBodyGlobalFilter/Forward*RoutingFilter/StreamRoutingFilter/NettyWriteResponseFilter 等) | **接受** ✅ |
| GW-3 "11 种谓词" | handler/predicate/ 穷举 | **14 种实现**: After/Before/Between/Cookie/Header/Host/Method/Path/Query/RemoteAddr/**ReadBody**/Weight/**XForwardedRemoteAddr**/**CloudFoundryRouteService** (执行计划漏 3 种) | **修正** ⚠ 11→14 |
| GW-4 "ReactiveLoadBalancerClientFilter—lb://" | filter/ | ReactiveLoadBalancerClientFilter 存在 (filter/); **消费 SCC 的 ReactiveLoadBalancerClient** (交叉引用 SCC-PLAN **SCC-7**: ReactorLoadBalancer 策略) + LoadBalancerServiceInstanceCookieFilter | **接受** ✅ (SCC 交叉引用) |
| GW-5 "NettyRoutingFilter→HttpClient→AdaptCachedBodyGlobalFilter" | filter/ + config/ | NettyRoutingFilter (306) + **HttpClient 由 HttpClientFactory 构建 (config/, 非 client/ 包 — 路径修正)** + AdaptCachedBodyGlobalFilter + EnableBodyCachingEvent (event/) | **接受+路径修正** ✅ |
| GW-6 "RequestRateLimiter→RedisRateLimiter" | filter/ | RequestRateLimiterGatewayFilterFactory (factory/) + **RedisRateLimiter 在 filter/ratelimit/ (非 factory/ — 路径修正)** | **接受+路径修正** ✅ |
| GW-7 "SpringCloudCircuitBreakerFilterFactory" | filter/factory/ | SpringCloudCircuitBreakerFilterFactory + **Resilience4J 版** (SpringCloudCircuitBreakerResilience4JFilterFactory) | **接受** ✅ |
| GW-8 "RewritePath/SetPath/StripPrefix/PrefixPath" | filter/factory/ | 四者全存在 (RewritePathGatewayFilterFactory/SetPathGatewayFilterFactory/StripPrefixGatewayFilterFactory/PrefixPathGatewayFilterFactory) | **接受** ✅ |
| GW-9 "CorsGatewayFilter→CorsConfiguration" | filter/cors/ + config/ | **无 CorsGatewayFilter 类 (代际过时)**: 4.3.2 用 **SimpleUrlHandlerMappingGlobalCorsAutoConfiguration + GlobalCorsProperties (config/) + CorsGatewayFilterApplicationListener (filter/cors/)** — 全局 CORS 配置演进 | **修正** ⚠ 代际: CorsGatewayFilter → 全局 CORS 配置 |
| 版本 | pom.xml | **4.3.2** (与 SCC 4.3.2 同代) | **补充** ✅ |
| 来源 | README/文档 | 略 | — |
| WebFlux 底座 | 模块 | server 依赖 WebFlux (RoutePredicateHandlerMapping extends AbstractHandlerMapping); webflux 模块 5 文件 (WebFlux 专用装配) | **补充** ✅ |

### v2 深度审计 (2026-08-16, 全包文件级 + 大文件反向扫描)

| 发现 | 证据 | 结论 |
|---|---|---|
| **编程式 DSL 未覆盖** | route/builder/ 6 文件: **GatewayFilterSpec (1047 行)**/RouteLocatorBuilder/PredicateSpec — 路由第二种定义方式 (Java DSL vs YAML) | **GW-1 扩**: + DSL (定义特征: 双路定义) |
| **转发家族未覆盖** | 实际 6 种 routing filter: NettyRoutingFilter (306)/**WebClientHttpRoutingFilter**/**WebsocketRoutingFilter**/ForwardRoutingFilter/FunctionRoutingFilter/StreamRoutingFilter — 执行计划只提 Netty | **GW-5 扩**: 转发家族 |
| **头传播面未覆盖** | filter/headers/ 16 文件: **XForwardedHeadersFilter/ForwardedHeadersFilter/TrustedProxies/RemoveHopByHopHeadersFilter/GRPCRequestHeadersFilter** | **GW-5 扩**: 头传播 (代理头/hop-by-hop) |
| **Retry 过滤器未覆盖** | **RetryGatewayFilterFactory (535 行)**: RetryConfig (retries/statuses/backoff 5 参数/jitter) + Reactor Retry.onlyIf — 与 gRPC G-6 天然对照 | **GW-7 扩**: 熔断 → **熔断与重试** |
| **安全头未覆盖** | SecureHeadersGatewayFilterFactory (416) | **GW-9 扩**: 跨域 + 安全头 |
| 39 种过滤器工厂 | filter/factory/ 70 文件 (执行计划只覆盖 8 种): headers 族 13 种/RequestSize/SetStatus/RedirectTo/TokenRelay/SaveSession/JsonToGrpc 等 | **归类说明**: headers 族入 GW-5; size/status 等应用面入 GW-8/负面空间 |
| 依赖方向 | gateway→SCC import **9 文件** (loadbalancer/discovery) | **接受**: 交叉引用 SCC-PLAN 成立 |
| 测试地图 | server 200 测试 (actuate/config/cors/discovery/filter/handler/route/support) | **补充** ✅ |

### v3 深度审计 (2026-08-16, support/config/discovery/event/filter 归类)

| 发现 | 证据 | 结论 |
|---|---|---|
| **配置绑定体系未深挖** | **ConfigurationService (256)**: Spring Boot **Binder/Bindable/MapConfigurationPropertySource** (L30-36,88-98) + **ShortcutConfigurable.ShortcutType.normalize** (ShortcutConfigurable.java:86,140-141) — YAML 短路语法 (`Path=/user/**`) vs 完整 Map 的统一绑定 | **GW-2 扩**: 过滤器/谓词工厂配置绑定面 (短路语法是定义特征) |
| **服务发现自动路由未深挖** | **DiscoveryClientRouteDefinitionLocator (discovery/ 3 文件)**: 消费 **ReactiveDiscoveryClient** (SCC) → 从服务实例自动生成 RouteDefinition (谓词来自服务元数据 SpEL L115,125,146) — lb:// 服务零配置路由 | **GW-4 扩**: 服务发现路由生成 (定义来源 + lb 语义) |
| support/ 25 归类 | ConfigurationService/ShortcutConfigurable → GW-2/3 共享; ServerWebExchangeUtils (484) → 支撑 (exchange 属性键); ipresolver/XForwardedRemoteAddressResolver → GW-5; tagsprovider 5 → **排除** (可观测); config/KeyValue 3 → 支撑 | **归类完成** ✅ |
| event/ 7 | RefreshRoutesEvent/RefreshRoutesResultEvent → GW-1; EnableBodyCachingEvent → GW-5; FilterArgsEvent/PredicateArgsEvent → GW-2/3; RouteDeletedEvent/WeightDefinedEvent → 支撑 | 归各域 ✅ |
| GatewayAutoConfiguration (962) | propertiesRouteDefinitionLocator (L238)/routeDefinitionRouteLocator (L262-265) — 装配核心 | GW-1/GW-2 支撑 ✅ |
| filter/ 21 顶层 | 6 routing + 6 GlobalFilter + GatewayFilterChain/OrderedGatewayFilter/FilterDefinition + WeightCalculatorWebFilter | GW-2/GW-5 覆盖 ✅ |
| **actuator 排除错误 (修正)** | **AbstractGatewayControllerEndpoint (342)**: `POST /routes/{id}` 动态保存路由 (L238, 注释示例 "http POST :8080/admin/gateway/routes/apiaddreqhead uri=...") / `POST /refresh` (L151) / `DELETE /routes/{id}` (L327) / `GET /globalfilters` (L202)/routefilters/routepredicates (L207-213) — **路由动态管理 API** (运行时增删路由/刷新/工厂查询) | **GW-1 扩**: 路由管理端点面 (运维定义特征: 动态路由, 对照 Envoy admin API) |

### v4 深度审计 (2026-08-16, filter/factory 70 + 谓词 18 + 模块级归类)

| 发现 | 证据 | 结论 |
|---|---|---|
| **响应缓存面未覆盖** | filter/factory/**cache/ 11 文件**: GlobalLocalResponseCacheGatewayFilter/LocalResponseCacheGatewayFilterFactory/**ResponseCacheManager**/CachedResponse/keygenerator 子包/ResponseCacheSizeWeigher — 网关响应缓存 (键生成/大小权重) | **GW-5 扩**: 响应缓存面 |
| filter/factory 70 全归类 | 39 种工厂 (35 具体 + Abstract 族) + 3 辅助 (SecureHeadersProperties/CircuitBreaker 2) + cache/ 11 + 其余子包 | **归类完成** ✅ |
| 谓词 18 全归类 | 14 种工厂 + AbstractRoutePredicateFactory + RoutePredicateFactory/GatewayPredicate 接口 + PredicateDefinition | GW-3 ✅ |
| route 15 + builder 6 | 全部落入 GW-1 (定义/装配/缓存/刷新/DSL) | ✅ |
| **webflux/mvc 模块 (5+5)** | **ProxyExchange API** (ProxyExchangeArgumentResolver/ProxyExchange/ProxyProperties/ProxyResponseAutoConfiguration) — 应用内编程式代理 (注解式) | **排除** (应用开发面, 非网关管线机制面; 与 GW-5 转发不同层) + 对照一句 |

**覆盖率报告 v4**: 9 域 + **8 处域内扩展** (v2 四处 + v3 两处 + v3.1 actuator + v4 响应缓存), filter/factory 70/谓词 18/route 21/其他模块 **全部文件级归类完毕**, 无未判定文件。

---

## 一、入口点与主线

`GatewayAutoConfiguration (config/) → RoutePredicateHandlerMapping (194, 请求→Route) → RouteDefinitionRouteLocator (233, 配置→Route) → FilteringWebHandler (196, 路由过滤器链) → GlobalFilter 链 (15 个) → NettyRoutingFilter (306, Reactor Netty 转发)` — 谓词面: `handler/predicate/ (14 种 RoutePredicateFactory)` — 配置面: `GatewayProperties + GatewayConfigurableProperties`。

## 二、域清单 (9 域: 4🔴 + 5🟡)

| # | 域 | 模块 (文件行) | 核心主题 | 方案 |
|:--:|---|---|---|---|
| GW-1 | **路由定位** | RoutePredicateHandlerMapping (194) + route/ (15) + **route/builder DSL (v2): GatewayFilterSpec 1047/RouteLocatorBuilder/PredicateSpec** | 请求→Route 映射/装配/缓存/刷新/**编程式 DSL** | 🔴 A |
| GW-2 | **过滤器链** | FilteringWebHandler (196) + GlobalFilter (18 实现) + GatewayFilterChain/OrderedGatewayFilter + GatewayFilterFactory 族 + **配置绑定体系 (v3): ConfigurationService 256 + ShortcutConfigurable 306 (Binder/短路语法)** | 链装配/排序/工厂→过滤器/配置绑定 | 🔴 A |
| GW-3 | **路由谓词** | handler/predicate/ (**14 种**): Path/Header/Cookie/Host/Method/Query/RemoteAddr/Weight/After/Before/Between + ReadBody/XForwardedRemoteAddr/CloudFoundryRouteService | 谓词 SPI/组合/AsyncPredicate | 🔴 A |
| GW-4 | **负载均衡与服务发现** | ReactiveLoadBalancerClientFilter + LoadBalancerServiceInstanceCookieFilter + RouteToRequestUrlFilter + **DiscoveryClientRouteDefinitionLocator (v3: 服务发现自动路由)** | lb:// 解析/choose/reconstructURI/自动路由生成 | 🟡 B |
| GW-5 | **请求转发与头处理** | **转发家族 (v2)**: NettyRoutingFilter 306/WebClientHttpRoutingFilter/WebsocketRoutingFilter/ForwardRoutingFilter/FunctionRoutingFilter/StreamRoutingFilter + HttpClientFactory + AdaptCachedBodyGlobalFilter + **头传播面 (v2)**: XForwardedHeadersFilter/TrustedProxies/RemoveHopByHopHeadersFilter/GRPCRequestHeadersFilter + **响应缓存面 (v4)**: cache/ 11 文件 (LocalResponseCache/ResponseCacheManager) | 转发家族/请求体缓存/头传播/响应缓存 | 🔴 A |
| GW-6 | **限流** | RequestRateLimiterGatewayFilterFactory + filter/ratelimit/ (RedisRateLimiter) | 令牌桶/SCRIPT/KeyResolver | 🟡 B |
| GW-7 | **熔断与重试 (v2 扩)** | SpringCloudCircuitBreakerFilterFactory + Resilience4J 版 + **RetryGatewayFilterFactory (535, v2): RetryConfig/backoff/jitter** | 断路器/重试语义 (对照 gRPC G-6) | 🟡 B |
| GW-8 | **路径重写** | RewritePath/SetPath/StripPrefix/PrefixPath (filter/factory/) | 4 种路径操作语义 | 🟡 B |
| GW-9 | **跨域与安全头 (v2 扩)** | **SimpleUrlHandlerMappingGlobalCorsAutoConfiguration + GlobalCorsProperties (config/)** + CorsGatewayFilterApplicationListener + **SecureHeadersGatewayFilterFactory (416, v2)** | CORS 全局配置/安全头 | 🟡 B |

## 三、执行顺序 (拓扑: 路由 → 谓词 → 过滤器链 → 转发 → 应用面)

**GW-1 → GW-3 → GW-2 → GW-5 → GW-4 → GW-6 → GW-7 → GW-8 → GW-9**

> 拓扑理由: 路由定位 (GW-1, 请求入口) → 谓词 (GW-3, 路由匹配依赖) → 过滤器链 (GW-2, 路由的过滤器装配) → Netty 转发 (GW-5, 链尾实际转发) → 负载均衡 (GW-4, 转发前选址, SCC 交叉) → 限流/熔断/路径重写/跨域 (应用面过滤器族)。GW-4 依赖 SCC (另一 AI 并行, 以 SCC-PLAN 引用)。

## 四、知识网络图

```
← 复用: WebFlux/Reactor (阶段2) + Netty (阶段1) + SCC (5.4, 并行 AI — LoadBalancer/Discovery 抽象)
→ 引出: 5.7 Alibaba + 5.8 Nacos (网关基座) + 5.6 OpenFeign (HTTP 客户端对照)
```

## 五、完成检查单

- [x] 顶层模块扫描 ↔ 域覆盖矩阵 (9 域接受, 修正 2 处)
- [x] 谓词数字穷举 (11→14)
- [x] GW-9 代际修正 (CorsGatewayFilter → 全局 CORS 配置)
- [x] 分工确认 (Gateway 无主) + SCC 交叉引用基准 (SCC-PLAN.md)
- [ ] 每域开工前: 对该域断言复查 (重点: 行号/默认值)
- [ ] 偏差已同步 HANDOFF (阶段 5.5 状态)
