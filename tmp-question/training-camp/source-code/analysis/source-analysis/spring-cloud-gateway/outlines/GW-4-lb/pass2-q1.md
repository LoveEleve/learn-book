# 闭环笔记 GW-4-q1 — URL 装配: RouteToRequestUrlFilter 合并语义

假设: 转发目标 = 路由 uri 的 scheme/host/port 覆盖请求 uri; lb:// 校验 (host 必填); 特殊 scheme 前缀存属性。

验证过程:
- **合并** (RouteToRequestUrlFilter.java:69-97): `UriComponentsBuilder.fromUri(uri).scheme(routeUri.getScheme()).host(routeUri.getHost()).port(routeUri.getPort()).build(encoded)` (L88-96) — **请求路径保留 + 路由的 scheme/host/port 覆盖**
- **encoded 处理** (L70): containsEncodedParts — 已编码 URI 保持
- **lb 校验** (L81-84): `"lb".equals(routeUri.getScheme()) && routeUri.getHost() == null → IllegalStateException("Invalid host: " + routeUri)` — **lb:// 必须有 host** (注释: "most likely because the host name was invalid (for example included an underscore)")
- **scheme 前缀** (L73-77): hasAnotherScheme (L52-53, 嵌套 scheme) → GATEWAY_SCHEME_PREFIX_ATTR 存前缀 + routeUri 替换为 schemeSpecificPart
- 输出: GATEWAY_REQUEST_URL_ATTR (L94) — GW-5 转发消费 (q1)

代码类型: Glue (URL 装配)

结论: 装配 = **路由 uri 覆盖请求 uri**: 路径保留自请求, scheme/host/port 来自路由 (lb://service 或 http://host); lb 校验防非法主机; 嵌套 scheme (如 lb://http://x) 前缀存属性。**被放弃的方案: 整体替换 URI** — 覆盖式保留请求路径 (转发语义)。 [跨域: GW-5 q1 消费 GATEWAY_REQUEST_URL_ATTR] [URI: 组件级合并] (RouteToRequestUrlFilter.java:39-97)
