# GW-9 跨域与安全 — 知识规划 (KP)

> 域级: 🟡 B | 模块: config/SimpleUrlHandlerMappingGlobalCorsAutoConfiguration + GlobalCorsProperties + filter/cors/ + filter/factory/SecureHeadersGatewayFilterFactory (416)
> 日期: 2026-08-16 | 版本: 4.3.2 | Pass 2 闭环: q1(CORS 装配) q2(路由级 CORS) q3(安全头) — **3/3 全闭环**

## 一、机制提取 (逐源)

### M1 CORS 双装配 (q1)
- SimpleUrlHandlerMappingGlobalCorsAutoConfiguration (35-45): setCorsConfigurations (L45, 静态资源)
- CorsGatewayFilterApplicationListener (66-110): RefreshRoutesResultEvent → routePredicateHandlerMapping.setCorsConfigurations (L104)

### M2 路由级 CORS (q2)
- 元数据 (L125-135): route.getMetadata().get("cors") → CorsConfiguration
- getPathPredicate (L109-124): 取首个 Path 谓词 pattern / /**; 合并 (L97-101, 路由优先)

### M3 安全头 (q3)
- SecureHeadersGatewayFilterFactory (43-136): 7+ 头常量 + withDefaults (L107) + applySecurityHeaders (L127-136)
- withDefaults (L226-250): 路由级 → 全局 fallback
- 默认值 (SecureHeadersProperties 41-163): X-XSS "1 ; mode=block" (L41)/HSTS "max-age=631138519" (L51)/X-Frame "DENY" (L61)/CSP 全策略 (L91)

## 二、聚合分级

| 级别 | 机制 |
|---|---|
| P1 | M2 路由级 CORS 合并 / M3 安全头默认值 |
| P2 | M1 双映射器装配 |

## 三、叙事线

场景: 前端跨域请求怎么放行?响应安全头怎么来?读者疑问链: CORS 配在哪 (M1) → 路由级怎么声明 (M2) → 安全头默认什么 (M3)。
