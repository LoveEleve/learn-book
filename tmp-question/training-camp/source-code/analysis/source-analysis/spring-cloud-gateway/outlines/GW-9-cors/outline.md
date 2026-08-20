# GW-9 跨域与安全 — 浏览器的放行令与响应防护: CORS 双装配与安全头默认值

> 前置: [[GW-1-路由定位]] (路由模型/刷新事件) + [[GW-3-路由谓词]] (Path 谓词) | 引出: 阶段收官 (GW-1~GW-9 全景) | 对照: Spring WebFlux CORS + OWASP 安全头
> 🟡 B | 3 KP | [模式: 元数据驱动 + 事件重建 + 意见默认值]
> Pass 2 闭环: q1(CORS 装配) q2(路由级) q3(安全头) — **3/3 全闭环**

**读者处境**: 前端在 `localhost:3000` 调网关 — 跨域!网关怎么放行?响应带不带安全头 (X-Frame-Options/CSP)?"跨域配置"配在哪一层?

### 1. CORS 双装配 — 静态与路由两套映射器

场景: CORS 配置注入到哪里?
源码路径:
- **静态映射器** (SimpleUrlHandlerMappingGlobalCorsAutoConfiguration.java:35-45): `@PostConstruct: simpleUrlHandlerMapping.setCorsConfigurations(globalCorsProperties.getCorsConfigurations())` (L45) — 全局 CORS 注入静态资源映射器
- **路由映射器**: RoutePredicateHandlerMapping 由 **CorsGatewayFilterApplicationListener** 在 RefreshRoutesResultEvent 时设置 (L104)
- **GlobalCorsProperties**: 全局配置源 (两处共用)
关键设计 (q1): **双装配**: 静态资源直接注入, 路由映射事件驱动 (路由变化重建)。**被放弃的方案: 单一 CORS 过滤器** — 映射器级配置与路由/资源路径天然对齐。 [跨域: GW-1 路由映射]

### 2. 路由级 CORS — 元数据声明与合并

场景: 单个路由怎么声明自己的 CORS?
源码路径:
- **触发** (CorsGatewayFilterApplicationListener.java:84-110): RefreshRoutesResultEvent → 每路由 getCorsConfiguration (L87)
- **元数据** (L125-135): `route.getMetadata().get("cors")` → CorsConfiguration (allowCredentials 等)
- **路径提取** (L109-124): **取首个 Path 谓词的 pattern** (L113-118) / 无则 /** — 路由级 CORS 绑定到谓词路径
- **合并** (L97-101): 路由级优先, 全局补缺
关键设计 (q2): **元数据驱动 + 事件重建**: CORS 与路由声明同处 (配置内聚); 路由刷新自动同步。**被放弃的方案: 独立过滤器** — 元数据内聚。 [跨域: GW-1 刷新事件; GW-3 Path 谓词消费]

### 3. 安全头 — 意见默认值与三级 fallback

场景: 响应安全头默认加哪些?值是什么?
源码路径:
- **工厂** (SecureHeadersGatewayFilterFactory.java:43-136): 7+ 头常量 + withDefaults (L107) + applySecurityHeaders 逐头 (L127-136)
- **withDefaults** (L226-250): 路由级值 → null 时全局属性 fallback (L243-248)
- **默认值** (SecureHeadersProperties L41-91): **X-XSS "1 ; mode=block"** (L41) / **HSTS "max-age=631138519"** (L51) / **X-Frame "DENY"** (L61) / **CSP 完整策略** (L91, "default-src 'self' https:; ... object-src 'none'; script-src https:")
- **opt-in**: Permissions-Policy (L163)
关键设计 (q3): **意见默认值**: 文档 (L36-39) "sensible defaults are applied" — 防"忘了配"安全头; 三级 fallback (路由 > 全局 > 默认)。**被放弃的方案: 全手动配置** — 默认值防疏漏。 [安全: OWASP] [跨域: GW-2 工厂体系]

### 核心悬念

"跨域与安全收官 — Gateway 九域全景: 路由 (GW-1) → 谓词 (GW-3) → 过滤器链 (GW-2) → 转发 (GW-5) → 负载均衡 (GW-4) → 限流/熔断/重写/跨域 (GW-6~9) — 一条请求的完整旅程闭合。"

### 负面空间 (不做)

1. 不写 CORS 协议细节 (预检/简单请求 — WebFlux 面)
2. 不写 GlobalCorsProperties 配置穷举
3. 不写安全头协议全貌 (OWASP 文档面)
4. 不写 CorsConfiguration 全属性 (allowedOrigins/methods 等穷举)
5. 不写 Permissions-Policy 语法细节
6. 不写 @CrossOrigin 注解对照 (MVC 面)
