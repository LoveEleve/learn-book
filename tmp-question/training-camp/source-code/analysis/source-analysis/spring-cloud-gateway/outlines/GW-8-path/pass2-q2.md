# 闭环笔记 GW-8-q2 — 前缀/段操作: StripPrefix 与 PrefixPath 防重复

假设: StripPrefix 去前 N 段 (tokenize 拆分); PrefixPath 加前缀但 GATEWAY_ALREADY_PREFIXED 防重复 (同一请求多次过滤不叠加)。

验证过程:
- **StripPrefix** (StripPrefixGatewayFilterFactory.java:58-75): `tokenizeToStringArray(path, "/")` (L69) → 跳过前 parts 段 → 重组 "/" 拼接 (L71-75); PARTS_KEY (L46)
- **PrefixPath** (PrefixPathGatewayFilterFactory.java:63-85): `UriTemplate(config.prefix)` (L65) + **GATEWAY_ALREADY_PREFIXED_ATTR 防重复** (L67-69: alreadyPrefixed → 直接 next) → put 标志 (L73) → **uriTemplate.expand(uriVariables)** (L79, 模板变量如 {segment}) → newPath = prefix + rawPath (L81) → GATEWAY_REQUEST_URL_ATTR 更新 (L82)
- **防重复必要性**: 过滤器链可能多次应用同一过滤器 (如路由过滤器 + 全局), 标志防前缀叠加
- 例: StripPrefix=1: /api/user/123 → /user/123; PrefixPath=/api: /user → /api/user

代码类型: Implementation (路径变换)

结论: 前缀/段操作 = **tokenize 重组 + 防重复标志**: StripPrefix 按段拆分重组 (无模板); PrefixPath 模板展开 + GATEWAY_ALREADY_PREFIXED 幂等。**被放弃的方案: 无防重复的前缀拼接** — 多次过滤会叠加前缀 (幂等性设计)。 [跨域: GW-4/GW-5 消费更新后的 URL] [模式: 幂等变换] (StripPrefixGatewayFilterFactory.java:58-75; PrefixPathGatewayFilterFactory.java:63-85)
