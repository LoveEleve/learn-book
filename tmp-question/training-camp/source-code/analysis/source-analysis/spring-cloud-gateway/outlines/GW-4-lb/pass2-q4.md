# 闭环笔记 GW-4-q4 — 服务发现路由: DiscoveryClientRouteDefinitionLocator (v3)

假设: 服务发现自动生成路由定义: 每个服务实例 → RouteDefinition, 谓词/过滤器参数支持 SpEL 模板 (从实例元数据取值)。

验证过程:
- **定位** (DiscoveryClientRouteDefinitionLocator.java:49-130): implements RouteDefinitionLocator — 消费 **ReactiveDiscoveryClient** (SCC, L31)
- **实例→路由** (L104-130): 服务实例 → buildRouteDefinition (L107) → **DelegatingServiceInstance** (L109, 实例+属性包装)
- **SpEL 模板** (L115,125,146): 谓词/过滤器参数 `getValueFromExpr(evalCtxt, parser, instanceForEval, entry)` — **从服务实例元数据求值** (如 Path=/service1/** 的 service1 替换)
- **配置** (DiscoveryLocatorProperties): predicates/filters 模板 (默认 Path=/serviceId/** (GatewayDiscoveryClientAutoConfiguration L55-66))
- 去重 (L105-107 注释 "remove duplicates")
- 关系: 产出 RouteDefinition → GW-1 装配 (与 YAML/DSL 路由合并)

代码类型: Implementation (自动路由)

结论: 服务发现路由 = **零配置路由生成**: lb:// 服务自动获得 Path 路由 (模板 SpEL 求值); 服务实例变化 → 路由刷新 (GW-1 心跳事件链)。**被放弃的方案: 手写每个服务路由** — 服务多时维护爆炸; 自动生成 + 模板让"新服务即路由"。 [跨域: GW-1 装配/刷新; SCC DiscoveryClient] [SpEL: 模板求值] (DiscoveryClientRouteDefinitionLocator.java:49-130)
