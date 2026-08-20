# 闭环笔记 Q1 — RLS 路由模型: 每请求服务端决策 + 子 LB 组合

假设: RLS 把"这次请求路由到哪个集群"外包给 RLS 服务器 — 每请求查询 (缓存加速), 返回 targets 列表 → 子 LB 选择。

验证过程:
- **核心注释** (CachingRlsLbClient.java:84-85): "routing by fetching the decision from route lookup server. **Every single request is routed by the server's decision**. To reduce the performance penalty, LruCache is used" — **每请求服务端决策 + LRU 缓存减损**
- **结构** (L120-150): Throttler + LbPolicyConfiguration (子 LB 配置) + maxAge/staleAge/callTimeout + **RouteLookupServiceStub** (CachingRlsLbClient.java:130, RLS 服务器 gRPC 客户端) + RlsPicker (L203) + **fallbackChildPolicyWrapper** (CachingRlsLbClient.java:136 — RLS 不可用时的默认目标兜底)
- **查询→目标→子 LB**: RLS 返回 RouteLookupResponse.targets() (L685, "No targets returned by RLS" 校验) → **refCountedChildPolicyWrapperFactory.createOrGet(targets)** (L689-691, 引用计数的子策略包装) → 每 RPC 经 RlsPicker 选子策略
- **子策略状态感知** (L733-744): getChildPolicyWrapper 跳过 TRANSIENT_FAILURE 的 (L736-738) — 故障目标自动回避
- 指标面: default_target_picks/target_picks/failed_picks (L142-155)

代码类型: Implementation (数据面路由)

结论: RLS = **路由决策外包**: 数据面每请求 (缓存命中除外) 问 RLS 服务器"去哪", 得到目标集群列表后交给**子 LB (G-4 策略)** 选具体端点; 引用计数包装器复用子策略实例。**被放弃的方案: 本地静态路由表** — 无法响应动态策略; 外包决策让路由策略集中管理 (服务网格场景)。 [跨域: G-4 SPI 子策略; G-7 xds ClusterSpecifierPlugin 集成] [分布式: 决策与控制分离] (CachingRlsLbClient.java:84-85,120-150,675-744)
