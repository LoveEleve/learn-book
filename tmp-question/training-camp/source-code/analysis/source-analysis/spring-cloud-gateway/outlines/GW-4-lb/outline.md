# GW-4 负载均衡与服务发现 — lb:// 的一行魔法: 从 URL 装配到实例选择

> 前置: [[GW-1-路由定位]] (路由模型) + [[GW-2-过滤器链]] (链位置) | 引出: [[GW-5-请求转发]] (重构 URL 消费) | 对照: SCC LoadBalancer (5.4, 并行 AI) + gRPC G-4/G-7 负载均衡
> 🟡 B | 4 KP | [模式: 分层消费 + 每请求选择]
> Pass 2 闭环: q1-q4 — **4/4 全闭环**

**读者处境**: `uri: lb://user-service` — 一行配置。请求进来: 路由匹配 → URL 装配 → 选实例 → 转发。lb:// 这个 scheme 怎么变成真实 IP?选不到实例会怎样?服务怎么自动获得路由?

### 1. URL 装配 — 覆盖式合并

场景: 转发目标 URL 怎么组装?
源码路径:
- **RouteToRequestUrlFilter** (RouteToRequestUrlFilter.java:39-97): **合并** (L88-96): `fromUri(uri).scheme(routeUri.getScheme()).host(...).port(...)` — **请求路径保留 + 路由 scheme/host/port 覆盖**
- **lb 校验** (L81-84): `"lb".equals(scheme) && host == null → IllegalStateException("Invalid host: " + routeUri)` — **lb:// 必须有 host** (注释: 主机名非法如含下划线)
- **scheme 前缀** (L73-77): 嵌套 scheme (lb://http://x) → GATEWAY_SCHEME_PREFIX_ATTR
关键设计 (q1): **覆盖式装配**: 路径来自请求, 目标来自路由。**被放弃的方案: 整体替换 URI** — 覆盖式保留请求路径 (转发语义)。 [跨域: GW-5 消费 GATEWAY_REQUEST_URL_ATTR]

### 2. LB 解析 — 每请求 choose + 404/503 语义

场景: lb:// 怎么变成真实实例?
源码路径:
- **触发** (ReactiveLoadBalancerClientFilter.java:99-101): 只处理 lb:// → 否则 next
- **choose** (L118): **SCC ReactiveLoadBalancerClient** 每请求选择
- **无实例** (L121-123): `NotFoundException.create(properties.isUse404(), ...)` — **404/503 可配, 默认 503** (use404 默认 false, GatewayLoadBalancerProperties.java:27)
- **scheme 覆盖** (L132-140): `isSecure() ? "https" : "http"` → DelegatingServiceInstance
- **重构 URL** (L145, L162-163): **LoadBalancerUriTools.reconstructURI** — 只换 host/port
关键设计 (q2): **每请求 choose**: 策略实时 (权重/健康); 无实例的失败模式可配。**被放弃的方案: 缓存选择** — 实时性 vs 开销权衡 (gRPC G-7 WRR 是缓存侧)。 [HTTP: 404/503] [跨域: GW-5 消费重构 URL]

### 3. SCC 交叉 — 分层消费

场景: LB 策略谁实现?
源码路径:
- **依赖实证**: gateway→SCC 9 文件 (v2 审计)
- **工厂注入** (L80-86): `LoadBalancerClientFactory` — SCC 的 **NamedContextFactory** (每服务子上下文, SCC-PLAN SCC-13 交叉)
- **策略**: SCC 默认 RoundRobin / 服务级可配
- **粘性** (LoadBalancerServiceInstanceCookieFilter.java:45-56): 选中实例写 cookie (粘同一实例)
关键设计 (q3): **分层消费**: Gateway 只做 URL 装配 + 404 语义, 选择策略全委托 SCC — 跨组件统一 LB。**被放弃的方案: 网关自实现** — 与 RestTemplate/OpenFeign 的 LB 不一致。 [架构: 分层] [跨域: SCC SCC-7/13]

### 4. 服务发现路由 — 零配置路由 (v3)

场景: 新服务上线, 路由要手写吗?
源码路径:
- **DiscoveryClientRouteDefinitionLocator** (DiscoveryClientRouteDefinitionLocator.java:49-130): 消费 **ReactiveDiscoveryClient** → 每实例 buildRouteDefinition (L107)
- **SpEL 模板** (L115,125,146): 谓词/过滤器参数从实例元数据求值 (getValueFromExpr); **默认模板来自 GatewayDiscoveryClientAutoConfiguration.initPredicates (L55-66): PathRoutePredicateFactory + `'/'+serviceId+'/**'`** + initFilters (L72: RewritePath 移除 /serviceId 前缀) — 默认 Path=/serviceId/** (GatewayDiscoveryClientAutoConfiguration L55-66)
- **刷新链**: 实例变化 → 心跳事件 → GW-1 路由刷新 (q3)
关键设计 (q4): **自动路由生成**: 新服务即路由 (模板化); 实例变化自动刷新。**被放弃的方案: 手写每服务路由** — 维护爆炸。 [跨域: GW-1 装配/刷新; SCC DiscoveryClient] [SpEL]

### 核心悬念

"lb:// 解析完成 — 应用面过滤器: 限流 (令牌桶)、熔断 (断路器)、路径重写、跨域。" 下一域 [[GW-6-限流]] → [[GW-7-熔断与重试]] → [[GW-8-路径重写]] → [[GW-9-跨域与安全]]。

### 负面空间 (不做)

1. 不写 SCC LoadBalancer 内部 (SCC-7 交叉引用, 另一 AI 域)
2. 不写策略配置穷举 (RoundRobin/Random/自定义)
3. 不写 LoadBalancerUriTools 细节 (SCC 工具)
4. 不写 DiscoveryLocatorProperties 配置穷举
5. 不写粘性 cookie 的会话语义细节
6. 不写健康检查/实例过滤 (SCC 面)
