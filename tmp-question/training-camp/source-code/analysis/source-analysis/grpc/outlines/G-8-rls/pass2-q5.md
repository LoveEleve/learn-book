# 闭环笔记 Q5 — xds 集成: 反射加载的 ClusterSpecifierPlugin

假设: xds 通过 ClusterSpecifierPlugin SPI 挂载 RLS, 但**编译期不依赖 grpc-rls** — 运行时反射加载, 缺失时报明确错误。

验证过程:
- **插件接口** (RouteLookupServiceClusterSpecifierPlugin.java:30-44): implements ClusterSpecifierPlugin — typeUrls (L41-44, type.googleapis.com/grpc.lookup.v1.RouteLookupClusterSpecifier)
- **反射加载** (L55-59): `Class.forName("io.grpc.lookup.v1.RouteLookupClusterSpecifier")` → ClassNotFoundException → `ConfigOrError.fromError("Dependency for 'io.grpc:grpc-rls' is missing: " + e)` — **xds 模块编译零依赖 rls, 运行时按需**
- **配置解析** (L60-70): Any.unpack → proto 转 JSON (MessagePrinter) → routeLookupConfig 提取
- 集成位置: ClusterManagerLoadBalancer (G-7 q5) 的子策略选择经 ClusterSpecifierPlugin 挂载自定义策略
- 依赖方向实证: rls 不 import xds (0 引用, v2 审计), xds 仅反射引用

代码类型: Glue (SPI + 反射)

结论: **可选依赖的 SPI 集成范式**: xds 声明 type URL → 运行时 Class.forName 加载 rls → 缺失时降级报错 (不破坏 xds 主功能); 让 grpc-rls 成为可选 jar。**被放弃的方案: 编译期依赖 rls** — 强制所有 xds 用户带 rls 依赖; 反射+明确报错让"可选能力"边界清晰。 [跨域: G-7 ClusterManager 挂载点; G-4 SPI 注册面] [架构: 可选依赖/反射集成] (RouteLookupServiceClusterSpecifierPlugin.java:30-70)
