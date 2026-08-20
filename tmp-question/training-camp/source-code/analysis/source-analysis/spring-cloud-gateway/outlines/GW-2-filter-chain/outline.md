# GW-2 过滤器链 — 路由之后的流水线: 全局与路由过滤器的统一链

> 前置: [[GW-1-路由定位]] (匹配产出 Route) + [[GW-3-路由谓词]] (同工厂体系) | 引出: [[GW-5-请求转发]] (链尾转发) | 对照: Spring MVC Interceptor + Netty ChannelPipeline (阶段1)
> 🔴 A | 5 KP | [模式: 责任链 + 合并排序 + 归一化绑定]
> Pass 2 闭环: q1(装配) q2(链执行) q3(缓存) q4(配置绑定) q5(工厂族) — **5/5 全闭环**

**读者处境**: 路由匹配后, 请求进入过滤器链: 全局过滤器 (框架级, 15 个) 和路由过滤器 (配置/DSL 级) 在同一条链里跑。它们的顺序怎么定?链怎么"递归"?YAML 里的 `AddRequestHeader=X-Request,value` 怎么变成过滤器实例?

### 1. 链装配 — 两集合合并 + 统一排序

场景: 全局过滤器和路由过滤器怎么进同一条链?
源码路径:
- **FilteringWebHandler** (FilteringWebHandler.java:55-79): WebHandler + **ApplicationListener<RefreshRoutesEvent>** (L56, 刷新联动)
- **loadFilters** (L79-93): GlobalFilter → **GatewayFilterAdapter** (L81) + **Ordered 提取**: `filter instanceof Ordered` (L82-84) 或 @Order 注解 (L86-89) → **OrderedGatewayFilter** 包装 — 统一 order 语义
- **getAllFilters** (L124-130): `combined = globalFilters + route.getFilters(); AnnotationAwareOrderComparator.sort(combined)` — **合并后统一排序**
关键设计 (q1): **两集合合并**: 全局 (框架级) 与路由 (配置级) 过滤器交错执行 (order 决定); GlobalFilter 经适配器获得 GatewayFilter 形态。**被放弃的方案: 两条独立链** — 交错让限流/转发等全局过滤器与业务过滤器可排序。 [模式: 装饰器+排序]

### 2. 链执行 — 反应式责任链

场景: 过滤器链怎么"递归"执行?
源码路径:
- **DefaultGatewayFilterChain** (L132-168): 不可变 `index` (L134) + `filters` (L136); 子链构造 index+1 (L141-144)
- **filter** (L153-166): `Mono.defer(() -> { if (index < size) { filter = filters.get(index); return filter.filter(exchange, new Chain(index+1)); } else return Mono.empty(); })` — **索引推进 + 完成条件**
- **Mono.defer 惰性**: 订阅才取下一个 — 反应式链支持异步过滤器
- 过滤器可中断: 不调用 chain.filter = 短路 (鉴权失败/重定向)
关键设计 (q2): **不可变索引递归**: 每级新链 (index+1), 过滤器决定是否调下游。**被放弃的方案: 可变迭代器** — 不可变链安全异步; 递归模型天然表达"过滤器可能不调用下游"。 [Reactor: Mono.defer] [模式: 责任链]

### 2.5 WebFilter 前置面 — 链之前的预计算 (R4 补充)

场景: GatewayFilter 链之前还有一层吗?
源码路径:
- **WeightCalculatorWebFilter** (WeightCalculatorWebFilter.java:59): implements **WebFilter** + Ordered + SmartApplicationListener — **WebFlux 层过滤器, 在 FilteringWebHandler (GatewayFilter 链) 之前执行**
- 职责: 按组预计算权重选中路由, 结果存 **WEIGHT_ATTR** (L88-90) — **GW-3 Weight 谓词只读结果** (预计算模式)
- 与链的关系: WebFilter (链前) vs GatewayFilter (链中) — 两层过滤器体系
关键设计: **预计算前置**: 权重决策在 WebFilter 层一次完成, 谓词消费 — 保证同组路由共享同一次随机 (组内互斥)。 [跨域: GW-3 q3 预计算消费] [WebFlux: WebFilter 层]

### 3. 缓存与刷新 — 路由级链缓存

场景: 每条链每次请求都重排吗?
源码路径:
- **routeFilterMap** (L61): `ConcurrentHashMap<Route, List<GatewayFilter>>` — 路由→排序链
- **routeFilterCacheEnabled** (L76-77): 开启后 computeIfAbsent (L115-119); 默认关闭
- **onApplicationEvent** (L97-101): RefreshRoutesEvent → `routeFilterMap.clear()` — **与 GW-1 双端消费同一事件**
关键设计 (q3): **路由级缓存 + 事件失效**: 热路径 O(1) 取链; 配置变更 → GW-1 路由缓存 + 本域链缓存同时失效。**被放弃的方案: 每次请求重排** — 排序每请求浪费。 [跨域: GW-1 q3 事件面] [并发: ConcurrentHashMap]

### 4. 配置绑定 — 短路语法的归一化

场景: `AddRequestHeader=X-Request,value` 怎么变成过滤器?
源码路径:
- **ShortcutConfigurable** (ShortcutConfigurable.java:86-150): **ShortcutType 枚举 3 种**: **DEFAULT** (L107-125, normalizeKey 键归一化 + **SpEL getValue** L115) / **GATHER_LIST** (L127-140, 值收集为 List) / GATHER_LIST_TAIL_FLAG
- **ConfigurationService** (ConfigurationService.java:45-149): SpelExpressionParser (L53) → normalizeProperties → `shortcutType().normalize(...)` (L138-141) → **Bindable.of(configClass) → bindOrCreate** (L148-149, **Boot Binder**)
- 三条路径汇合: YAML 短路 / 完整 Map / Java DSL (GW-1 q5)
关键设计 (q4): **归一化 + Binder 两阶段**: ShortcutType 解释语法 (键序/集合), Binder 类型化绑定 (转换/校验/嵌套如 backoff.*)。**被放弃的方案: 手写解析** — Binder 提供类型安全。 [跨域: GW-3 谓词工厂同体系; Boot Binder]

### 5. 过滤器工厂族 — 39 种与 Retry 实证

场景: 过滤器怎么生产?重试过滤器内部长什么样?
源码路径:
- **工厂 SPI**: GatewayFilterFactory (config → GatewayFilter); OrderedGatewayFilter (OrderedGatewayFilter.java:27-44)
- **Retry 实证** (RetryGatewayFilterFactory.java:54-301): 短路字段 9 个 (L73-74, **嵌套 backoff.firstBackoff/maxBackoff/factor/basedOnPreviousValue** + jitter.randomFactor) → **Backoff.exponential** (L221-222, Reactor) + exceedsMaxIterations (L229)
- 39 种分类: 值操作 13 (Add/Set/Remove Header 等)/路径 4/状态 (SetStatus/RedirectTo)/安全 (SecureHeaders 416)/容错 (Retry 535/CircuitBreaker)/限流
关键设计 (q5): 工厂+Config 绑定统一实例化; **Retry 嵌套绑定**实证 v4 体系。**被放弃的方案: 过滤器直接 new** — 工厂让 YAML/DSL 统一路径。过滤器异常经 Mono 错误传播到 WebFlux 错误链 (500/自定义 Handler); Binder 校验失败在装配期报错 (坏过滤器配置不炸网关, GW-1 容错同源)。 [跨域: gRPC G-6 退避对照 (factor vs 1.6x)] [Reactor: Backoff]

### 核心悬念

"过滤器链走到最后 — NettyRoutingFilter (链尾) 怎么把请求真正发出去?请求体怎么缓存重放?" 下一域 [[GW-5-请求转发与头处理]]。

### 负面空间 (不做)

1. 不写 39 种过滤器的逐个细节 (按 6 类概述)
2. 不写 Boot Binder 机制全貌 (Spring 面)
3. 不写 SpEL 表达式语言细节
4. 不写 GatewayMetricsFilter 指标面 (可观测)
5. 不写过滤器工厂的 Config 类穷举
6. 不写 GatewayFilterChain 接口演进历史
