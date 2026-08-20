# 闭环笔记 GW-3-q2 — Path 谓词: PathPattern 匹配 + 缓存 PathContainer

假设: Path 谓词用 Spring PathPattern 匹配: basePath 前缀支持/尾斜杠可配/多 pattern 任一匹配/exchange 属性缓存解析结果。

验证过程:
- **apply(config)** (PathRoutePredicateFactory.java:95-130): `synchronized (this.pathPatternParser)` (L97, 解析器线程安全) → `pathPatternParser.setMatchOptionalTrailingSeparator(config.isMatchTrailingSlash())` (L98, **尾斜杠可配**) → **basePath 支持** (L102-107: webFluxProperties.getBasePath() 前缀拼接, "context-path 兼容")
- **缓存 PathContainer** (L116-118): `exchange.getAttributes().computeIfAbsent(GATEWAY_PREDICATE_PATH_CONTAINER_ATTR, s -> parsePath(rawPath))` — **每请求一次解析, 多谓词共享** (性能: 只 parse 一次)
- **多 pattern 匹配** (L121-128): 遍历 pattern 列表, 任一 matches → match; 无匹配 → 返回 false
- **traceMatch** (L129+): 匹配诊断日志 (GatewayPredicate 契约)
- Config: patterns (List<String>) + matchTrailingSlash (Boolean)
- 测试: pathRouteWorks (PathRoutePredicateFactoryTests.java:52)/trailingSlashReturns404 (L57)/mulitPathRouteWorks (L86)/mulitPathDslRouteWorks (L93, DSL 版)

代码类型: Implementation (路径匹配)

结论: Path 谓词 = **Spring PathPattern 封装**: basePath 兼容 (网关挂 context-path)/尾斜杠可配/多 pattern 任一; **exchange 属性缓存** 让同一请求多个谓词只解析一次路径。**被放弃的方案: 手写正则匹配路径** — PathPattern 支持 ** 通配/变量捕获 ({id}); 复用 Spring 成熟匹配。 [跨域: GW-1 GATEWAY_PREDICATE_ROUTE_ATTR 同 exchange 属性面; Spring WebFlux PathPattern] (PathRoutePredicateFactory.java:95-130)
