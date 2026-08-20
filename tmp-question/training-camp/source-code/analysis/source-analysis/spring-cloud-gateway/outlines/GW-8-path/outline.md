# GW-8 路径重写 — /api 去哪了: 四种路径变换与原始链保留

> 前置: [[GW-4-负载均衡]] (URL 装配) + [[GW-5-请求转发]] (URL 消费) | 引出: [[GW-9-跨域与安全]] (收官) | 对照: Nginx rewrite + Spring UriTemplate
> 🟡 B | 3 KP | [模式: 变换 + 幂等 + 保留链]
> Pass 2 闭环: q1(正则) q2(前缀段) q3(共同模式) — **3/3 全闭环**

**读者处境**: 网关把 `/api/user/123` 转发到 `user-service` 的 `/user/123` — /api 去哪了?四种路径过滤器 (RewritePath/SetPath/StripPrefix/PrefixPath) 怎么改路径?改完原始 URL 还能找到吗?

### 1. 正则替换 — RewritePath

场景: 复杂路径变换怎么做?
源码路径:
- **转义** (RewritePathGatewayFilterFactory.java:62): `config.replacement.replace("$\\", "$")` — YAML 里的 `$\\` 转义为正则分组引用 (如 `/$1`)
- **替换** (L67-69): `pattern.matcher(path).replaceAll(replacement)` — Java 正则 (分组捕获); **只改 path, query string 保留** (L70: req.mutate().path(newPath) — 查询参数不受影响)
- **更新** (L71): GATEWAY_REQUEST_URL_ATTR 更新 → 后续转发 (GW-4/GW-5) 用新路径
关键设计 (q1): **正则 replaceAll + 目标更新**: $\\ 转义处理 YAML 转义; 例: `/api/(?<segment>.*)` → `/${segment}` 去前缀。**被放弃的方案: 字符串前缀替换** — 正则支持分组/复杂模式。 [正则: 分组引用]

### 2. 前缀/段操作 — StripPrefix/PrefixPath/SetPath

场景: 去前缀/加前缀怎么做?重复应用呢?
源码路径:
- **StripPrefix** (StripPrefixGatewayFilterFactory.java:58-75): `tokenizeToStringArray(path, "/")` (L69) → 跳前 N 段 → 重组 (L71-75) — `/api/user` + parts=1 → `/user`
- **PrefixPath** (PrefixPathGatewayFilterFactory.java:63-85): **UriTemplate(config.prefix)** (L65) + **GATEWAY_ALREADY_PREFIXED_ATTR 防重复** (L67-71: 已加前缀 → 直接 next) → expand 模板变量 (**L79**) → prefix + path (**L81**); GATEWAY_ALREADY_PREFIXED put (L73)
- **SetPath** (L40-71): 模板直接设置路径 — **模板变量来自 getUriTemplateVariables** (ServerWebExchangeUtils L313-328: URI_TEMPLATE_VARIABLES_ATTRIBUTE, 由**路径谓词匹配的 {id} 等变量**填充 — GW-3 Path 谓词 → GW-8 模板展开的联动); **路由过滤器 order = 配置顺序** (RouteDefinitionRouteLocator L173: OrderedGatewayFilter(factory, i+1))
关键设计 (q2): **防重复幂等**: 过滤器链可能多次应用 — GATEWAY_ALREADY_PREFIXED 防前缀叠加。**被放弃的方案: 无标志拼接** — 前缀叠加错误。 [模式: 幂等变换]

### 3. 共同模式 — 原始链保留

场景: 改完路径, 原始 URL 还能找到吗?
源码路径:
- **addOriginalRequestUrl** (ServerWebExchangeUtils.java:294-296): `computeIfAbsent(GATEWAY_ORIGINAL_REQUEST_URL_ATTR, LinkedHashSet)` + 追加 — **链式保留** (多次变换全轨迹)
- **GATEWAY_ORIGINAL_REQUEST_URL_ATTR** (L111): "gatewayOriginalRequestUrl"
- **4 工厂全消费** (RewritePath L66/SetPath L64/StripPrefix L66/PrefixPath L72); GW-4 LB 前也调用 (L104)
关键设计 (q3): **变换前保留 + 变换后更新**: 原始链供诊断/回退; 新 URL 供转发; **原始链被 XForwardedHeadersFilter 消费** (GW-5: X-Forwarded-Host/Port 基于原始请求, GATEWAY_ORIGINAL_REQUEST_URL_ATTR 读取方)。**被放弃的方案: 只保留最新** — 链式变换需完整轨迹。 [跨域: GW-4/GW-5 同属性; GW-5 头传播] [可观测]

### 核心悬念

"路径处理完成 — 收官: 跨域 (CORS) 与安全头 (SecureHeaders)。" 下一域 [[GW-9-跨域与安全]]。

### 负面空间 (不做)

1. 不写 Java 正则语法全量
2. 不写 UriTemplate 细节 (Spring 面)
3. 不写 4 种过滤器的 Config 校验穷举
4. 不写路径过滤器与谓词 (GW-3) 的 PathPattern 关系 (不同层)
5. 不写 GATEWAY_REQUEST_URL_ATTR 全消费方穷举
6. 不写 Nginx rewrite 对照细节
