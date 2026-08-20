# 闭环笔记 GW-3-q1 — 谓词 SPI: 同步/异步双通道 + DSL 版

假设: RoutePredicateFactory 是统一 SPI — apply(config) 同步, applyAsync(config) 异步 (默认同步适配), DSL 版 apply(Consumer) 供编程式路由。

验证过程:
- **接口面** (RoutePredicateFactory.java:34-75): `extends ShortcutConfigurable, Configurable<C>` (L34) — 短路配置 + 配置绑定 (v3 配置体系); **PATTERN_KEY = "pattern"** (L38, 短路语法键)
- **DSL 版** (L42-53): `apply(Consumer<C> consumer): C config = newConfig(); consumer.accept(config); beforeApply(config); return apply(config)` (L42-47) — 编程式路由 (GW-1 q5) 消费; applyAsync(Consumer) 同构 (L49-53)
- **异步适配** (L70-71): `applyAsync(C config) { return toAsyncPredicate(apply(config)); }` — **默认同步包异步**; 工厂可覆写实现真异步 (如 ReadBody)
- **name()** (L74): NameUtils.normalizeRoutePredicateName — 类名→谓词名 (PathRoutePredicateFactory → Path)
- GatewayPredicate: test + traceMatch (匹配诊断)
- PredicateDefinition: name + args (配置形态, GW-1 q2 消费)

代码类型: Interface (SPI)

结论: 谓词工厂 = **统一 SPI + 双通道**: 同步 apply (默认) / 异步 applyAsync (覆写点); DSL 版与配置版共用同一 apply 核心; 短路配置 (PATTERN_KEY) 与完整 Map 绑定统一。**被放弃的方案: 只支持同步谓词** — ReadBody 等需要异步读取请求体; 默认适配让 99% 同步谓词零成本接入。 [跨域: GW-1 q2 装配消费; v3 ConfigurationService 绑定] [Reactor: AsyncPredicate] (RoutePredicateFactory.java:34-75)
