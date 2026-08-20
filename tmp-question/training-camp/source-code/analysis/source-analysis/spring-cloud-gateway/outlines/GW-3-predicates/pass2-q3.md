# 闭环笔记 GW-3-q3 — 头/IP/时间/权重四族: 从正则到 CIDR 到预计算

假设: 谓词族实现各异: Header 正则 (可空=查存在)/RemoteAddr 用 Netty CIDR/After-Before 用 ZonedDateTime/Weight 是"预计算+消费"模式。

验证过程:
- **Header 谓词** (HeaderRoutePredicateFactory.java:32-97): REGEXP_KEY (L42); `Pattern.compile(config.regexp)` 仅当非空 (L55) — **regexp 为空 = 只查 header 存在性** (L76, "there is a value and since regexp is empty, we only check existence")
- **RemoteAddr 谓词** (RemoteAddrRoutePredicateFactory.java:41-116): **IpSubnetFilterRule** (Netty 类, L116: `new IpSubnetFilterRule(ipAddress, cidrPrefix, ACCEPT)`) — CIDR 匹配 (如 192.168.1.0/24); XForwardedRemoteAddr 同构 (读 X-Forwarded-For, GW-5 头传播面关联)
- **Weight 谓词** (WeightRoutePredicateFactory.java:85-130): **预计算模式** — 注释 (L94-96): "all calculations and comparison against random num happened in WeightCalculatorWebFilter" — WebFilter 算好"本组选中路由"存 WEIGHT_ATTR (L89), 谓词只比较 `routeId.equals(chosenRoute)` (L104) — **谓词无随机, 决定已定**
- **After 谓词** (AfterRoutePredicateFactory.java:31-52): `ZonedDateTime.now()` (L52) 与配置 datetime 比较; Before/Between 同构 (时间窗)
- 组合: 多谓词 AND (GW-1 q2 combinePredicates)

代码类型: Implementation (谓词实现族)

结论: 谓词族三种模式: ① **值匹配** (Header 正则/可空语义) ② **结构匹配** (CIDR IP) ③ **预计算消费** (Weight — WebFilter 先行, 谓词只读结果, 保证全组路由共享同一次随机决策)。**被放弃的方案: Weight 谓词内直接随机** — 每个路由独立随机会破坏"组内互斥" (同一请求只能选一个路由); 预计算让决策全局一致。 [跨域: GW-2 WeightCalculatorWebFilter; GW-5 X-Forwarded-For] [算法: CIDR/时间窗] (HeaderRoutePredicateFactory.java:42-76; RemoteAddrRoutePredicateFactory.java:114-116; WeightRoutePredicateFactory.java:85-101; AfterRoutePredicateFactory.java:52)
