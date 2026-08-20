# GW-3 路由谓词 — 知识规划 (KP)

> 域级: 🔴 A | 模块: handler/predicate/ (18 文件: 14 种工厂)
> 日期: 2026-08-16 | 版本: 4.3.2 | Pass 2 闭环: q1(SPI) q2(Path) q3(四族) — **3/3 全闭环**

## 一、机制提取 (逐源)

### M1 谓词 SPI (q1)
- RoutePredicateFactory (34-75): extends ShortcutConfigurable + Configurable; PATTERN_KEY (L38)
- apply(Consumer) DSL (L42-47)/applyAsync(Consumer) (L49-53); **applyAsync 默认 = toAsyncPredicate(apply)** (L70-71)
- **ReadBody 覆写 applyAsync (L62)** — 真异步 (Mono 读体 L69-80)
- name() = normalizeRoutePredicateName (L74)

### M2 Path 匹配 (q2)
- apply (PathRoutePredicateFactory 95-130): synchronized PathPatternParser (L97) + matchTrailingSlash 可配 (L98) + **basePath 兼容** (L102-107)
- **缓存 PathContainer** (L116-118): computeIfAbsent GATEWAY_PREDICATE_PATH_CONTAINER_ATTR — 每请求一次解析
- 多 pattern 任一匹配 (L121-128); traceMatch 诊断

### M3 谓词族 (q3)
- Header: regexp 可空 = 查存在性 (HeaderRoutePredicateFactory 55,76)
- RemoteAddr: **Netty IpSubnetFilterRule CIDR** (RemoteAddrRoutePredicateFactory 114-116)
- **Weight 预计算模式**: WeightCalculatorWebFilter 算好 → WEIGHT_ATTR (WeightRoutePredicateFactory 89) → 谓词只比较 routeId (L104)
- After/Before/Between: ZonedDateTime.now() 比较 (AfterRoutePredicateFactory 52)
- 组合: AND (GW-1 q2)

## 二、聚合分级

| 级别 | 机制 |
|---|---|
| P1 | M1 SPI 双通道 (异步覆写点) / M2 Path 缓存 PathContainer |
| P2 | M3 预计算模式 (Weight) / CIDR / 时间窗 |

## 三、叙事线

场景: `Path=/user/**` 一行配置, 请求怎么被匹配?读者疑问链: 工厂怎么定义 (M1) → Path 怎么匹配 (M2) → 其他 13 种怎么实现 (M3) → 组合 (GW-1)。
