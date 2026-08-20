# GW-9 Pass 1 扫描笔记 — 跨域与安全

> 日期: 2026-08-16 | 版本: 4.3.2 | 🟡 B | 模块: config/SimpleUrlHandlerMappingGlobalCorsAutoConfiguration + GlobalCorsProperties + filter/cors/CorsGatewayFilterApplicationListener + filter/factory/SecureHeadersGatewayFilterFactory (416)

## 继承树/调用图

```
CORS 面:
  ├── SimpleUrlHandlerMappingGlobalCorsAutoConfiguration (35): globalCorsProperties → simpleUrlHandlerMapping.setCorsConfigurations (L45)
  ├── CorsGatewayFilterApplicationListener (66): RefreshRoutesResultEvent → 路由元数据 cors → 合并全局配置 → routePredicateHandlerMapping.setCorsConfigurations
  └── GlobalCorsProperties (config/): 全局 CORS 配置
安全面:
  └── SecureHeadersGatewayFilterFactory (44, 416): 7+ 安全头 (X-XSS/Strict-Transport/X-Frame/Content-Type-Options/Referrer-Policy/Content-Security-Policy/Download-Options)
      ├── withDefaults (L107) + assembleHeaders (L105)
      └── applySecurityHeaders (L127-136): addHeaderIfEnabled 逐头
```

## 基本元素分解

1. **全局 CORS**: GlobalCorsProperties → 两个映射器 (SimpleUrlHandlerMapping + RoutePredicateHandlerMapping)
2. **路由级 CORS**: 路由元数据 cors → RefreshRoutesResultEvent 时合并 (Listener)
3. **安全头**: SecureHeaders 工厂 — 默认值 + opt-out 配置

## 标记问题 (3)

1. **Q1 CORS 装配**: 全局配置两个映射器的关系 (SimpleUrlHandlerMapping vs RoutePredicateHandlerMapping)
2. **Q2 路由级 CORS**: 路由元数据 → RefreshRoutesResultEvent 合并逻辑
3. **Q3 安全头**: SecureHeaders 默认头集 + withDefaults 语义

## 已读测试

- CorsGatewayFilterApplicationListenerTests/SimpleUrlHandlerMappingGlobalCorsAutoConfigurationTests 待读
