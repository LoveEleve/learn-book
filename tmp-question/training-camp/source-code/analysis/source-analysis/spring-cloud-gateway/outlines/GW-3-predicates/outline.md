# GW-3 路由谓词 — 一行 Path=/user/** 的魔法: 谓词工厂与匹配语义

> 前置: [[GW-1-路由定位]] (谓词过滤消费) | 引出: [[GW-2-过滤器链]] (过滤器工厂对照) | 对照: Spring PathPattern + Netty CIDR + K8s Ingress 路径 + **GW-2 过滤器工厂 (同工厂体系, 后域对照)**
> 🔴 A | 3 KP | [模式: SPI + 双通道 + 预计算]
> Pass 2 闭环: q1(SPI) q2(Path) q3(四族) — **3/3 全闭环**

**读者处境**: 路由配置里 `predicates: - Path=/user/**` — 一个字符串, 请求 `/user/123` 就认领了。这个字符串怎么变成"匹配能力"?14 种谓词 (Path/Header/Cookie/Host/Method/Query/RemoteAddr/Weight/After/Before/Between/ReadBody/XForwardedRemoteAddr/CloudFoundryRouteService) 各自怎么实现?配多个谓词怎么组合?

### 1. 谓词 SPI — 同步/异步双通道

场景: 一个谓词工厂长什么样?为什么要异步?
源码路径:
- **接口面** (RoutePredicateFactory.java:34-75): `extends ShortcutConfigurable, Configurable<C>` — 短路配置 + 配置绑定 (v3); **PATTERN_KEY** (L38, 短路语法键 `Path=/user/**` 的 `=` 左侧)
- **DSL 版** (L42-47): apply(Consumer) — 编程式路由 (GW-1 q5) 消费
- **异步适配** (L70-71): `applyAsync(config) { return toAsyncPredicate(apply(config)); }` — **默认同步包异步**
- **ReadBody 覆写** (ReadBodyRoutePredicateFactory.java:62): applyAsync 真异步 — Mono 读取请求体再测试 (L69-80)
- name() (L74): 类名→谓词名归一化
关键设计 (q1): **双通道 + 默认适配**: 99% 同步谓词零成本接入异步链; ReadBody 这类"需要先读请求体"的覆写真异步。**被放弃的方案: 只支持同步谓词** — ReadBody 无法实现。 [模式: SPI] [跨域: GW-1 q2 装配消费]

### 2. Path 匹配 — PathPattern 与缓存解析

场景: `/user/**` 怎么匹配 `/user/123`?尾斜杠呢?
源码路径:
- **apply** (PathRoutePredicateFactory.java:95-130): `synchronized (pathPatternParser)` (L97) → **matchTrailingSlash 可配** (L98) → **basePath 兼容** (L102-107, 网关挂 context-path 时前缀拼接)
- **缓存 PathContainer** (L116-118): `computeIfAbsent(GATEWAY_PREDICATE_PATH_CONTAINER_ATTR, parsePath(rawPath))` — **每请求一次路径解析, 多谓词共享**
- **多 pattern** (L121-128): 任一 matches 即通过; traceMatch 诊断
关键设计 (q2): **Spring PathPattern 封装**: `**` 通配/`{id}` 变量捕获; basePath 兼容让网关可挂 context-path。**被放弃的方案: 手写正则** — 丢失变量捕获/性能 (PathPattern 预编译)。 [跨域: WebFlux PathPattern] [性能: exchange 属性缓存]

### 3. 谓词族 — 三种实现模式

场景: 其余 13 种谓词怎么实现?权重路由为什么"不随机"?
源码路径:
- **值匹配**: Header (HeaderRoutePredicateFactory.java:55,76) — regexp 可空 = **只查存在性**; Cookie/Host 同构
- **结构匹配**: RemoteAddr (RemoteAddrRoutePredicateFactory.java:114-116) — **Netty IpSubnetFilterRule CIDR** (192.168.1.0/24); XForwardedRemoteAddr 读 X-Forwarded-For (GW-5)
- **预计算消费**: Weight (WeightRoutePredicateFactory.java:85-101) — 注释 (L94-96): "all calculations and comparison against random num happened in WeightCalculatorWebFilter" — **WebFilter 预先算好组内选中路由存 WEIGHT_ATTR** (L89), 谓词只比较 routeId (L104)
- **时间窗**: After/Before/Between (AfterRoutePredicateFactory.java:52) — ZonedDateTime.now() 比较
- **组合**: 多谓词 AND (GW-1 q2 combinePredicates)
- **联动面**: ReadBody 谓词触发请求体缓存 (GW-5 AdaptCachedBodyGlobalFilter 关联); Weight 配置变更发 WeightDefinedEvent (event/ 面)
关键设计 (q3): 三种实现模式; **Weight 预计算**保证"同组路由共享同一次随机决策" (组内互斥)。**被放弃的方案: 谓词内随机** — 每路由独立随机会选多个。 [算法: CIDR/时间窗] [跨域: GW-2 §2.5 WebFilter 前置面]

### 核心悬念

"谓词决定'走哪条路由', 过滤器决定'怎么处理' — 两条工厂体系 (RoutePredicateFactory/GatewayFilterFactory) 共享同一配置绑定机制 (v3), 但过滤器链怎么执行?" 下一域 [[GW-2-过滤器链]]。

### 负面空间 (不做)

1. 不写 14 种谓词的逐个细节 (只讲三种模式 + Path 详解)
2. 不写 PathPattern 语法全量 (Spring 文档面)
3. 不写 ReadBody 的 HttpMessageReader 细节 (编码面)
4. 不写 CloudFoundryRouteService 细节 (平台专属)
5. 不写 PredicateDefinition 校验规则穷举
6. 不写谓词与过滤器工厂的配置绑定重复细节 (v3 已覆盖)
