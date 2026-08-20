# GW-2 过滤器链 — 知识规划 (KP)

> 域级: 🔴 A | 模块: handler/FilteringWebHandler (196) + filter/ (GlobalFilter 18) + support/ConfigurationService (256) + ShortcutConfigurable (306) + filter/factory (39 种)
> 日期: 2026-08-16 | 版本: 4.3.2 | Pass 2 闭环: q1(装配) q2(链执行) q3(缓存) q4(配置绑定) q5(工厂族) — **5/5 全闭环**

## 一、机制提取 (逐源)

### M1 链装配 (q1)
- FilteringWebHandler (55-79): WebHandler + ApplicationListener<RefreshRoutesEvent> (L56)
- loadFilters (L79-93): GatewayFilterAdapter + Ordered 提取 (instanceof L87-89/@Order L91-94) → OrderedGatewayFilter
- getAllFilters (L124-130): globalFilters + route.getFilters() 合并 + AnnotationAwareOrderComparator.sort

### M2 链执行 (q2)
- DefaultGatewayFilterChain (L132-168): 不可变 index 递归; filter: Mono.defer + index<size → filter.filter(exchange, chain(index+1)) else Mono.empty
- GatewayFilterAdapter (L172-185)

### M3 缓存刷新 (q3)
- routeFilterMap (L61): ConcurrentHashMap<Route, List<GatewayFilter>>; routeFilterCacheEnabled (L76-77)
- onApplicationEvent (L97-101): RefreshRoutesEvent → clear — 与 GW-1 双端消费

### M4 配置绑定 (q4)
- ShortcutConfigurable (L86-150): ShortcutType 枚举 — DEFAULT (L107-125, normalizeKey+SpEL getValue)/GATHER_LIST (L127-140)/GATHER_LIST_TAIL_FLAG
- ConfigurationService (L45-149): SpelExpressionParser (L53) + normalizeProperties → shortcutType().normalize (L138-141) → Bindable.of(configClass) → bindOrCreate (L148-149, Boot Binder)

### M5 工厂族 (q5)
- GatewayFilterFactory SPI; OrderedGatewayFilter (27-44)
- Retry 实证 (RetryGatewayFilterFactory 54-301): 短路字段 9 个 (L73-74, 嵌套 backoff.*) + Backoff.exponential (L221-222) + exceedsMaxIterations (L229)
- 39 种分类: 值操作 13/路径 4/状态/安全/容错/限流

## 二、聚合分级

| 级别 | 机制 |
|---|---|
| P1 | M1 合并排序 / M2 反应式责任链 / M4 短路绑定 |
| P2 | M3 缓存刷新 / M5 工厂族 |

## 三、叙事线

场景: 路由匹配后, 过滤器链怎么跑?读者疑问链: 哪些过滤器进链/顺序 (M1) → 链怎么执行 (M2) → 缓存 (M3) → 工厂配置怎么绑定 (M4) → 39 种怎么分类 (M5)。
