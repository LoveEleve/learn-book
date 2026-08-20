# 闭环笔记 GW-4-q2 — LB 解析流程: choose → 无实例 → 重构 URL

假设: lb:// 解析 = SCC choose (每请求) → 无实例抛 NotFound (404/503 可配) → 实例 secure 决定 scheme 覆盖 → reconstructURI 换 host。

验证过程:
- **触发** (ReactiveLoadBalancerClientFilter.java:99-101): `url == null || (!"lb".equals(url.getScheme()) && !"lb".equals(schemePrefix))` → 直接 next — **只处理 lb://**
- **choose** (L118): `choose(lbRequest, serviceId, supportedLifecycleProcessors)` — SCC **ReactiveLoadBalancerClient** (每请求一次选择)
- **无实例** (L121-123): `!response.hasServer()` → **`NotFoundException.create(properties.isUse404(), "Unable to find instance for " + url.getHost())`** — **404/503 可配** (use404 属性)
- **scheme 覆盖** (L132-140): `retrievedInstance.isSecure() ? "https" : "http"` + 原 scheme 保持 → **DelegatingServiceInstance** (包装 + scheme 覆盖)
- **重构 URL** (L145, L162-163): `reconstructURI(serviceInstance, uri)` → **LoadBalancerUriTools.reconstructURI** — 替换 host/port (SCC 工具)
- 原 URL 保留: addOriginalRequestUrl (L104, 后续过滤器可回退)
- 测试: ReactiveLoadBalancerClientFilterTests (404/503 语义)

代码类型: Implementation (解析流程)

结论: lb 解析 = **每请求 choose + 容错**: 无实例的失败模式可配 (404 vs 503 — use404); secure 实例自动 https; reconstructURI 只换 host/port 保留路径。**被放弃的方案: 缓存选择结果** — 每请求 choose 让 LB 策略实时 (权重/健康); gRPC 的缓存 (G-7 WRR) 是另一种权衡。 [跨域: SCC ReactiveLoadBalancerClient (SCC-PLAN SCC-7 交叉); GW-5 转发消费重构 URL] [HTTP: 404/503 语义] (ReactiveLoadBalancerClientFilter.java:62-163)
