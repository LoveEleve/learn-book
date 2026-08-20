# 闭环笔记 Q2 — XdsNameResolver: 控制面驱动的解析器 (补 Pass 2 遗漏)

假设: XdsNameResolver 是 xds:/// 的解析入口 — 地址/配置全部来自控制面 (LDS/RDS), 且生成 ConfigSelector 供调用路由 (含 RPC_HASH_KEY)。

验证过程:
- **定位** (XdsNameResolver.java:89-95): "A NameResolver for resolving gRPC target names with 'xds:' scheme. Resolving a gRPC target involves **contacting the control plane management server via xDS protocol** to retrieve service information and produce a service config" — 与 DnsNameResolver 的本质差异: 地址源是控制面而非 DNS
- **resolve 流程** (L209-248): start(Listener2) (L209) → **LDS 资源订阅** (ldsResourceName L247) → resolveState.start (L248) — 经 XdsClientImpl (q1) 订阅 LDS/RDS
- **ConfigSelector** (L128): `private final ConfigSelector configSelector` — 解析结果带配置选择器 (G-3 ConfigSelector 消费): 路由/负载均衡策略选择
- **RPC_HASH_KEY** (L101-102): `CallOptions.Key<Long> RPC_HASH_KEY = CallOptions.Key.create("io.grpc.xds.RPC_HASH_KEY")` — **xds resolver 生成调用哈希 → RingHash 消费 (q3 L425-428)**
- **ClusterSpecifierPlugin 集成** (L52-56): RouteLookupServiceClusterSpecifierPlugin (RLS 插件, G-8 反射加载)
- 规模: 1206 行 (比 DnsNameResolver 709 大近一倍 — 配置树/路由/安全属性全在此)

代码类型: Implementation (解析器)

结论: XdsNameResolver = **控制面解析器**: 订阅 LDS/RDS → 得服务配置 + 路由 → 产出 ConfigSelector (含 RPC_HASH_KEY 供 RingHash 等策略); 与 DNS 解析器 (地址列表) 对照, 它是"配置树解析器"。**被放弃的方案: 复用 DnsNameResolver 加控制面查询** — 解析产物维度不同 (地址 vs 配置+路由), 独立实现更清晰。 [跨域: G-3 ConfigSelector 消费; G-5 解析 SPI 第二实现; G-7 RingHash RPC_HASH_KEY; G-8 RLS 插件挂载点] (XdsNameResolver.java:89-95,101-102,128,209-248)
