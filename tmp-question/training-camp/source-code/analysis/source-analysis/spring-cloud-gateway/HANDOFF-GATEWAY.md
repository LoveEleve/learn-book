# HANDOFF — Spring Cloud Gateway 源码分析交接文档 (详细版, 9/9 全量收官)

> **日期**: 2026-08-16 | **版本**: 4.3.2 (pom.xml 实证) | 主模块 spring-cloud-gateway-server **239 文件**
> **给新 AI**: 本文是 Gateway 阶段的**唯一入口**。9 域全部交付 (KP + 大纲 + completeness + 多轮深审至收敛 + 时空溯源 + harness + 全量回归)。所有锚点/行号均为 4.3.2 源码实证。
> **源码**: `/data/workspace/source-code/code/spring/spring-cloud-gateway` (浅克隆)
> **规划**: GATEWAY-PLAN.md (v1-v4 深度审计: 9 域 + 8 处域内扩展 + 2 修正 + 2 改判)
> **交接背景**: 阶段 5 并行格局 (2026-08-16): Feign ✅ / Dubbo (另一 AI) / gRPC ✅ (本会话) / SCC (另一 AI 并行) / **Gateway ✅ (本会话)** / OpenFeign (另一 AI 活跃) / Alibaba ✅ (另一 AI) / Nacos ✅ (另一 AI) / Sentinel (未开工)。

---

## 〇、三十秒总览

- Spring Cloud Gateway 4.3.2, 响应式 API 网关 (WebFlux + Reactor Netty)
- 主模块 spring-cloud-gateway-server 239 文件: route/ (15) + filter/ (21+fatory 族 70) + handler/ (3) + config/ (27) + discovery/ (3) + event/ (7) + support/ (25) + actuator/ (4) + headers/ (16) + ratelimit/ (6) + cache/ (11)
- **9 域**: GW-1 路由定位 → GW-3 谓词 → GW-2 过滤器链 → GW-5 转发与头 → GW-4 负载均衡 → GW-6 限流 → GW-7 熔断重试 → GW-8 路径重写 → GW-9 跨域安全
- **全量回归 harness 9/9 全绿 (31 断言)**: MiniGW1 5 + MiniGW2 3 + MiniGW3 3 + MiniGW4 4 + MiniGW5 3 + MiniGW6 3 + MiniGW7 3 + MiniGW8 4 + MiniGW9 3
- **交叉引用**: SCC (另一 AI 域) — SCC-7/10/13 域号实证; Alibaba/Nacos 已收官可引用

---

## 一、项目全貌

### 1.1 版本与来源

| 项 | 值 | 证据 |
|---|---|---|
| 版本 | **4.3.2** | pom.xml 实证 (与 SCC 4.3.2 同代) |
| 构建 | Gradle/Maven (multi-module) | spring-cloud-gateway-server 为唯一大模块 |
| 其他模块 | webflux 5 (ProxyExchange API) + mvc 5 | **ProxyExchange 为应用面 API, 排除** (v4 审计) |
| 测试 | server 200 测试 (actuate/config/cors/discovery/filter/handler/route/support) | 功能地图 |

### 1.2 模块规模与关键文件行数速查

| 文件 | 行数 | 所属域 |
|---|---|---|
| GatewayFilterSpec (route/builder/) | **1047** | GW-1 (DSL, v2 扩展) |
| GatewayAutoConfiguration (config/) | 962 | 装配支撑 |
| HttpClientProperties (config/) | 586 | GW-5 |
| RetryGatewayFilterFactory (filter/factory/) | **535** | GW-7 (v2 扩展) |
| ServerWebExchangeUtils (support/) | 484 | 支撑 (exchange 属性) |
| SecureHeadersGatewayFilterFactory | **416** | GW-9 (v2 扩展) |
| ConfigurationService (support/) | 256 | GW-2/GW-3 (配置绑定, v3) |
| ShortcutConfigurable (support/) | 306 | GW-2/GW-3 (短路语法, v3) |
| NettyRoutingFilter | 306 | GW-5 |
| FilteringWebHandler | 196 | GW-2 |
| RoutePredicateHandlerMapping | 194 | GW-1 |
| RouteDefinitionRouteLocator | 233 | GW-1 |
| RedisRateLimiter | 300+ | GW-6 |
| AbstractGatewayControllerEndpoint (actuator/) | 342 | GW-1 (管理端点, v3.1 改判) |
| DiscoveryClientRouteDefinitionLocator | 216 | GW-4 (v3 扩展) |

---

## 二、09 怀疑审计表 (v1-v4 全记录, GATEWAY-PLAN.md 完整版)

| 轮次 | 维度 | 发现 |
|---|---|---|
| **v1** | 执行计划断言验证 | 域清单 9 接受; **谓词 11→14 种** (漏 ReadBody/XForwardedRemoteAddr/CloudFoundryRouteService); **GW-9 代际过时** (无 CorsGatewayFilter 类 → 全局 CORS 配置); 路径修正 2 (HttpClientFactory 在 config/ / RedisRateLimiter 在 ratelimit/) |
| **v2** | 全包文件级 + ≥400 行反向扫描 | **4 处域内扩展**: GW-1 +DSL (GatewayFilterSpec 1047) / GW-5 +转发家族 6 种+头传播面 16 文件 / GW-7 +Retry (535) / GW-9 +SecureHeaders (416); 依赖实证 gateway→SCC **9 文件**; 测试 200 |
| **v3** | support/config/discovery/event 归类 | **再扩 2 处**: GW-2 +配置绑定体系 (ConfigurationService 256 + ShortcutConfigurable 306, Binder/短路语法) / GW-4 +服务发现自动路由 (DiscoveryClientRouteDefinitionLocator); support/25 全归类 (tagsprovider 排除); event/7 归各域 |
| **v3.1** | actuator 深读 (用户质疑排除) | **排除改判**: actuator 4 文件实为**路由动态管理 API** (POST /routes/{id} 一行 curl 加路由/refresh/delete/工厂查询) → **GW-1 管理面** — 教训: 凭"可观测"直觉排除未读类 |
| **v4** | filter/factory 70 + 谓词 18 + 模块级 | **+GW-5 响应缓存面** (cache/ 11 文件: LocalResponseCache/ResponseCacheManager); webflux/mvc ProxyExchange 排除 (应用面 API, 附理由) |

**最终**: 9 域 + **8 处域内扩展**,全部 **239 文件**文件级归类完毕,无未判定文件 (另含 webflux/mvc 10 文件排除)。

---

## 三、域清单与拓扑

### 3.1 域清单 (9 域: 4🔴 + 5🟡)

| # | 域 | 模块 (文件行) | 核心主题 | 方案 |
|:--:|---|---|---|---|
| GW-1 | **路由定位** | RoutePredicateHandlerMapping (194) + route/ (15) + route/builder DSL (GatewayFilterSpec 1047) + actuator (342, v3.1) | 首匹配胜出/声明→运行时/DSL/管理端点/缓存刷新 | 🔴 A |
| GW-3 | **路由谓词** | handler/predicate/ (18: **14 种**工厂) | SPI 双通道/Path 缓存/Weight 预计算/三模式 | 🔴 A |
| GW-2 | **过滤器链** | FilteringWebHandler (196) + GlobalFilter 18 + ConfigurationService (256) + ShortcutConfigurable (306) | 合并排序/反应式链/短路绑定/WebFilter 前置 | 🔴 A |
| GW-5 | **请求转发与头** | 转发家族 6 种 (NettyRoutingFilter 306 等) + HttpClientFactory + headers/ 16 + cache/ 11 | 两阶段提交/body 缓存/X-Forwarded 可信/响应缓存 | 🔴 A |
| GW-4 | **负载均衡与服务发现** | ReactiveLoadBalancerClientFilter + RouteToRequestUrlFilter + CookieFilter + DiscoveryClientRouteDefinitionLocator | URL 装配/每请求 choose/404-503 语义/SCC 分层 | 🟡 B |
| GW-6 | **限流** | RequestRateLimiterGatewayFilterFactory + ratelimit/ (RedisRateLimiter/Bucket4j) | Lua 令牌桶/故障降级/KeyResolver | 🟡 B |
| GW-7 | **熔断与重试** | SpringCloudCircuitBreakerFilterFactory + Resilience4J 版 + RetryGatewayFilterFactory (535) | 状态码熔断/fallback 编排/安全默认值/组合语义 | 🟡 B |
| GW-8 | **路径重写** | RewritePath/SetPath/StripPrefix/PrefixPath (4 种) | 正则/段操作/谓词变量联动/原始链 | 🟡 B |
| GW-9 | **跨域与安全** | SimpleUrlHandlerMappingGlobalCorsAutoConfiguration + GlobalCorsProperties + CorsGatewayFilterApplicationListener + SecureHeaders (416) | CORS 双装配/路由级元数据/安全头默认值 | 🟡 B |

### 3.2 拓扑与依赖

**GW-1 → GW-3 → GW-2 → GW-5 → GW-4 → GW-6 → GW-7 → GW-8 → GW-9**

> 拓扑理由: 路由定位 (请求入口) → 谓词 (匹配) → 过滤器链 (装配) → 转发 (链尾) → 负载均衡 (转发前 lb://, SCC 消费) → 应用面 (限流/熔断/重写/跨域)。**前向引用纪律**: 前置只声明拓扑前位域;GW-5 的 GW-4 为"链中流程前位非机制依赖"移入对照;GW-3 的 GW-2 同法 (07 §维度3 教训)。

---

## 四、每域已验证锚点速查 (9 域, 写作时作为起点, 必须重新 grep)

> 锚点格式: `类名: 方法 L行号` — 文件为 spring-cloud-gateway-server/src/main/java/org/springframework/cloud/gateway/ 下同名 .java (SCC 类在 spring-cloud-commons 仓库); 所有行号经多轮审查验证, 写作时必须重新 grep 确认。

### GW-1 路由定位
- RoutePredicateHandlerMapping: getHandlerInternal L79-110 / lookupRoute L130-140 (filterWhen 首匹配) / GATEWAY_ROUTE_ATTR L97-101 / 无匹配 404 L104-109 / validateRoute L168-173
- RouteDefinitionRouteLocator: getRoutes L109-127 (failOnRouteDefinitionError 容错) / convertToRoute L129-135 / combinePredicates L198-209 (**AND**) / lookup L211-240 / loadGatewayFilters L145-168
- CachingRouteLocator: CacheFlux L59 / fetch sort L62-63 / refresh L79-80; RouteRefreshListener L46-80 (RefreshRoutesEvent L75)
- **DSL (v2)**: RouteLocatorBuilder routes() L45 / route(id,fn) L69 / build() L91-93 (Flux RouteLocator); GatewayFilterSpec L95 (filter L122-134 / circuitBreaker L273 / retry L710)
- **管理端点 (v3.1)**: AbstractGatewayControllerEndpoint save L238 (curl 示例 L232) / refresh L178-179 / delete L327 / globalfilters L202-203
- Route: builder(RouteDefinition) L72-78

### GW-3 路由谓词
- RoutePredicateFactory L34-75: PATTERN_KEY L38 / apply(Consumer) L42-47 / applyAsync 默认适配 L70-71 / name() L74
- PathRoutePredicateFactory L95-130: PathPattern synchronized L97-98 / basePath L102-107 / **PathContainer 缓存 L116-118** / 多 pattern L121-128
- Header L55 (regexp 可空=查存在) / RemoteAddr **IpSubnetFilterRule L114-116** (CIDR) / Weight **预计算注释 L94-96** + WEIGHT_ATTR L89 + routeId 比较 L104 / After L52 (ZonedDateTime) / ReadBody applyAsync L62 (真异步)
- GatewayDiscoveryClientAutoConfiguration initPredicates L55-66 (默认 Path 模板)

### GW-2 过滤器链
- FilteringWebHandler L55-79 (ApplicationListener L56) / loadFilters L79-93 (Adapter L81 / instanceof L82-84 / @Order L86-89) / getAllFilters L124-130 (合并排序) / DefaultGatewayFilterChain L132-168 (filter L153-166: Mono.defer + index+1) / onApplicationEvent L97-101 / routeFilterMap L61
- ShortcutConfigurable L86-150: ShortcutType DEFAULT L107-125 (normalizeKey+SpEL) / GATHER_LIST L127-140
- ConfigurationService L45-149: normalizeProperties L138-141 / Bindable L148-149
- OrderedGatewayFilter L27-44 / RetryGatewayFilterFactory L54-229 (字段 L73-74 / Backoff L221-222)

### GW-5 请求转发与头
- NettyRoutingFilter L72-200: GATEWAY_REQUEST_URL_ATTR L61,112 / 请求发出 L133-147 / **延迟提交注释 L150** ("Defer committing...") / lb 未解析 next L116
- HttpClientFactory L47-140: create L79 / HttpClient.create L83 / proxy L127-140
- AdaptCachedBodyGlobalFilter L37-81: cacheRequestBody L66 / mutate L71-75 / order L80
- XForwardedHeadersFilter L42-242: 5 头 L59-71 / 5 开关 L94-105 / **TrustedProxies L242** / 警告 L115
- NettyWriteResponseFilter L44-105: CLIENT_RESPONSE_CONN_ATTR L71 / writeAndFlushWith L96-100 / cleanup L101-105
- WebsocketRoutingFilter L37-41 / ForwardRoutingFilter L34-43

### GW-4 负载均衡
- RouteToRequestUrlFilter L39-97: 合并 L88-96 / **lb 校验 L81-84** ("Invalid host") / scheme 前缀 L73-77 / mergedUrl L94
- ReactiveLoadBalancerClientFilter L62-163: lb 触发 L99-101 / **choose L118** / **NotFoundException L121-123** (use404 默认 false=503, GatewayLoadBalancerProperties L27) / overrideScheme L132-133 / reconstructURI L162-163 (LoadBalancerUriTools doReconstructURI L97-111: host+port+scheme)
- LoadBalancerServiceInstanceCookieFilter L45-56 (order = LB_ORDER+1)
- DiscoveryClientRouteDefinitionLocator L49-130: buildRouteDefinition L107 / DelegatingServiceInstance L109 / **SpEL L115,125,146**;GatewayDiscoveryClientAutoConfiguration initPredicates L55-66 (SpEL `'/'+serviceId+'/**'`) + initFilters L72
- order 数值: ROUTE_TO_URL_FILTER_ORDER=**10000** (L44) < LOAD_BALANCER_CLIENT_FILTER_ORDER=**10150** (L69)

### GW-6 限流
- RequestRateLimiterGatewayFilterFactory L93-122: resolver.resolve L99 / EMPTY_KEY L97-105 (**denyEmpty 默认 true** L55 / **emptyKeyStatus 默认 403** L57-58) / isAllowed L112 / 头传播 L115-117 / setComplete L122 / 默认 429 L138
- RedisRateLimiter: 三参数 L241-247 / **Lua L253-258** (request_rate_limiter.lua: filled_tokens L18 / >= L19 / 拒绝不扣减 L20 / **TTL=fill_time*2 L12,23-25** / replicate_commands L1) / 结果 L262-274 / **故障降级 L282-287** ("We don't want a hard dependency... Stripe's 0.01%") / 4 头 L70-85 / **默认 bean 无内置速率** (GatewayRedisAutoConfiguration L71)
- PrincipalNameKeyResolver L23-32 / Bucket4jRateLimiter L43-75

### GW-7 熔断与重试
- SpringCloudCircuitBreakerFilterFactory L55-165: create L95 / **状态码熔断 L100-109** (CircuitBreakerStatusCodeException) / **enableBodyCaching L93** (fallback 联动) / fallback 重构 L124-131 / reset L139 / **handle L142** (DispatcherHandler 直转) / resumeWithoutError L143 (**默认 false**) / Config L169-177
- SCC ReactiveCircuitBreaker L29-37 (run L31 / run+fallback L37) — **SCC-10 域号**
- RetryGatewayFilterFactory: 字段 L73-74 / series 匹配 L95-104 / apply L78 / Backoff L221-222 / exceedsMax L229 / **安全默认值 L305-315** (retries=3 + series=SERVER_ERROR + methods=GET + exceptions=IO/Timeout)
- FallbackHeadersGatewayFilterFactory L33-54 (CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR L48 / addExceptionDetails L162)

### GW-8 路径重写
- RewritePathGatewayFilterFactory L62-71: $\\ 转义 L62 / replaceAll L67-69 / **只改 path query 保留 L70** / GATEWAY_REQUEST_URL L71
- StripPrefixGatewayFilterFactory L58-75: tokenize L69 / 重组 L71-75
- PrefixPathGatewayFilterFactory L63-85: UriTemplate L65 / **GATEWAY_ALREADY_PREFIXED L67-73** / expand L79 / 拼接 L81
- SetPathGatewayFilterFactory L40-71: **getUriTemplateVariables L66** (谓词匹配变量, ServerWebExchangeUtils L313-328)
- addOriginalRequestUrl (ServerWebExchangeUtils L294-296, LinkedHashSet 追加) / GATEWAY_ORIGINAL_REQUEST_URL_ATTR L111 (**XForwardedHeadersFilter 消费**)
- 路由过滤器 order = 配置顺序 (RouteDefinitionRouteLocator L173: OrderedGatewayFilter(i+1))

### GW-9 跨域与安全
- SimpleUrlHandlerMappingGlobalCorsAutoConfiguration L35-45: setCorsConfigurations L45
- CorsGatewayFilterApplicationListener L66-135: onApplicationEvent L86 / **setCorsConfigurations L104** / 合并 L97-101 (路由优先) / getPathPredicate L109-124 (首个 Path 谓词 pattern / /**) / metadata.cors L126 / allowCredentials L133
- SecureHeadersGatewayFilterFactory L43-136: withDefaults L107,226-250 (三级 fallback) / applySecurityHeaders L127-136
- SecureHeadersProperties L41-163: **X-XSS "1 ; mode=block" L41 / HSTS "max-age=631138519" L51 / X-Frame "DENY" L61 / CSP 全策略 L91** / Permissions-Policy opt-in L163

---

## 五、方法论执行记录 (每域)

### 5.1 每域管线

```
Pass 0 上下文 → Pass 1 扫轮廓 (问题清单**全闭环** — GW-7 教训) → Pass 2 闭环 (被放弃方案+跨域) 
→ Pass 3 KP+大纲 (锚点 file:line 直接格式) → completeness (全 ✅ 或声明归位)
→ 六层深审 (引用**内容**回源 — 不只行号) → temporal-trace → harness (运行验证)
→ 多轮 review 至收敛 (07 收敛信号: 连续两轮不同维度零发现)
```

### 5.2 每域审查轮次与修正统计

| 域 | 审查轮次 | 累计修正要点 |
|---|---|---|
| GW-1 | 3 | 锚点密度/引用形式/DSL+管理端点补节 (v2/v3.1 同步)/补强后整体零修正验证 |
| GW-3 | 2 | L104 行号/前向引用违规 (GW-2 移对照) |
| GW-2 | 2 | onApplicationEvent L97-101/loadFilters 5 处行号/WebFilter 前置面 (WeightCalculator)/GlobalFilter 15→18 |
| GW-5 | 2 | L115 警告行号/前向引用 (GW-4 移对照)/响应缓存对照补节 |
| GW-4 | 3 | 行号 5 处/**SCC-5→SCC-7 域号错位**/默认模板来源/use404 默认 503/残留 5 处清零 |
| GW-6 | 3 | L99/L112 行号/Lua TTL 语义/**默认 403 vs 429**/无内置速率/残留清零 |
| GW-7 | 4 | L142 行号/resumeWithoutError 默认/reset 双路径/FallbackHeaders 配套/**SCC-10 域号补全**/Retry 安全默认值/enableBodyCaching 联动 |
| GW-8 | 3 | L79/L81 行号/**谓词变量联动 (GW-3→GW-8)**/order=配置顺序/query 保留/X-Forwarded 消费原始链/harness 防重复语义 |
| GW-9 | 2 | L104 行号/harness 路径模式匹配 (最具体优先)/review-notes 交付遗漏补全 |

### 5.3 跨仓库协作规范 (与另一 AI 的 SCC)

- 交叉引用以 **SCC-PLAN.md 域号实证**: SCC-7 (ReactorLoadBalancer 策略)/SCC-10 (断路器抽象)/SCC-13 (NamedContextFactory)
- **不探索对方源码内部** (只读接口契约: ReactiveCircuitBreaker L29-37 等)
- 教训: 域号必须回对方 PLAN 核对 (SCC-5→7 错位;GW-7 域号缺失→补 SCC-10)

---

## 六、教训 (本会话实证, 必读)

1. **规划审计不能只验执行计划断言**: v1 只验证了执行计划的类/数字, 反向扫描 (全包文件级 + ≥400 行) 才挖出 8 处域内扩展 (DSL 1047/转发家族/头传播/Retry 535/配置绑定/服务发现路由/响应缓存/actuator 管理面)。
2. **排除必须读类**: actuator 凭"可观测"直觉排除 → 用户质疑 → 实为路由动态管理 API (POST /routes/{id})。与 gRPC 的 tagsprovider 对照 (那真是可观测)。
3. **引用验证三层**: ① 行号存在 ② **引用内容** (Codec.GZIP 式: 验证引用的是注释还是代码) ③ **跨文件残留** (修正后全局 grep — pass2/KP/PLAN 不同步是高频错误)。
4. **数字默认值必须穷举**: emptyKeyStatus 默认 **403 非 429** / Retry 默认 **只重试 GET+5xx** / 默认 bean 无内置速率 — 三处反直觉默认值全是 grep 挖出。
5. **跨仓库域号**: SCC-5→SCC-7 错位 + GW-7 SCC-10 缺失 — 交叉引用必须回对方 PLAN 核对。
6. **harness 运行打脸 8 次**: PrefixPath 防重复对象 / CORS 路径模式匹配 / /** 兜底优先级 — 运行验证是理解的最强校验。
7. **前向引用纪律 (07 §维度3)**: GW-3/GW-5 都曾前置声明后位域 (GW-2/GW-4) → 移入对照; 拓扑序是依赖声明的唯一依据。
8. **问题清单全闭环**: GW-7 曾 Pass 1 标记 7 问只闭环 6 个 (XdsNameResolver 教训在 Gateway 的 GW-4 上重演风险) — 清单对照是强制步骤。

---

## 七、环境与运行

- **构建**: Maven/Gradle multi-module; 主模块 spring-cloud-gateway-server
- **harness**: 全部纯 JDK 极简复现 (javac/java, 无依赖), 9 个 MiniGW1-9
- **全量回归命令**: `cd harness && for d in GW-*; do javac $d/MiniGW*.java && java -cp $d MiniGW*; done` (31 断言全绿)
- **测试参考**: server 200 测试 (filter/ratelimit/RedisRateLimiterTests 等)

---

## 八、完成检查单

- [x] GATEWAY-PLAN.md (v1-v4: 9 域 + 8 扩展 + 2 修正 + 2 改判)
- [x] 9 域全交付 (KP + outline + completeness + review-notes + temporal-trace + harness)
- [x] 多轮 review 至收敛 (07 收敛信号, 每域 2-4 轮)
- [x] 全量回归 9/9 全绿 (31 断言)
- [x] 交叉引用 SCC 域号实证 (SCC-7/10/13)
- [x] 交接文档 (本文, 详细版)
- [x] **本文自身深审 (2026-08-16)**: 锚点抽查 7 处修正 1 (choose L112→L118)/断言 31 核对/GW-1 轮次 2→3 修正/SCC-5 历史语境确认/拓扑一致/行数抽查 2 精确+1 宽范围合理
- [ ] (下一 AI) 如需写作: 以 §四 锚点速查为起点, 每锚点重新 grep

---

## 九、阶段总结

**交付全景**: 9 域, 核心机制见 §三; 规划审计 4 轮; 每域 2-4 轮深审; 累计修正 40+ 处。

**知识网络**:
```
← 复用: WebFlux/Reactor + Netty + Boot Binder + SCC (LB/熔断/Discovery)
→ 消费: 5.7 Alibaba (收官) + 5.8 Nacos (收官) + 5.9 Sentinel (网关对照)
→ 对照: gRPC (传输/退避/负载均衡) + Feign (声明式客户端)
```

**阶段 5 现状 (2026-08-16 20:30)**: Feign ✅ / Dubbo (另一 AI) / gRPC ✅ / SCC (另一 AI) / **Gateway ✅** / OpenFeign (另一 AI 活跃) / Alibaba ✅ / Nacos ✅ / Sentinel (未开工)。
