# 闭环笔记 GW-8-q1 — 正则替换: RewritePath 语义

假设: RewritePath 用正则替换请求路径: $\\ → $ 转义 (Java 正则分组引用), addOriginalRequestUrl 保留原始, 替换后更新 GATEWAY_REQUEST_URL_ATTR。

验证过程:
- **转义** (RewritePathGatewayFilterFactory.java:62): `config.replacement.replace("$\\", "$")` — **YAML/属性里的 `$\\` 转义为 `$`** (正则分组引用, 如 `/$1`)
- **替换** (L67-69): `pattern.matcher(path).replaceAll(replacement)` — Java 正则 replaceAll (分组捕获)
- **原始保留** (L66): addOriginalRequestUrl(exchange, req.getURI()) — 原始 URL 存属性 (后续可回退/日志)
- **更新** (L71): `exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, request.getURI())` — 转发目标更新 (GW-4/GW-5 消费)
- 例: `RewritePath=/api/(?<segment>.*), /$\{segment}` — 去 /api 前缀
- Config: regexp (REGEXP_KEY L44)/replacement (REPLACEMENT_KEY L49)

代码类型: Implementation (正则变换)

结论: 路径重写 = **正则 replaceAll + 目标更新**: $\\ 转义处理 YAML 转义问题; 替换后 GATEWAY_REQUEST_URL_ATTR 更新让后续转发过滤器 (GW-4/GW-5) 用新路径。**被放弃的方案: 字符串前缀替换** — 正则支持分组/复杂模式 (与 SetPath 的模板语义互补)。 [跨域: GW-4/GW-5 消费 GATEWAY_REQUEST_URL_ATTR] [正则: 分组引用] (RewritePathGatewayFilterFactory.java:62-71)
