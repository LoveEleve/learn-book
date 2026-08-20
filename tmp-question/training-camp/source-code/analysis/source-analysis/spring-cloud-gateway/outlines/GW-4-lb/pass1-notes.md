# GW-4 Pass 1 扫描笔记 — 负载均衡与服务发现

> 日期: 2026-08-16 | 版本: 4.3.2 | 🟡 B | 模块: filter/ReactiveLoadBalancerClientFilter + LoadBalancerServiceInstanceCookieFilter + RouteToRequestUrlFilter + discovery/ (DiscoveryClientRouteDefinitionLocator) + **SCC 交叉引用 (ReactiveLoadBalancerClient)**

## 继承树/调用图

```
RouteToRequestUrlFilter (39, 链前置): route.uri + 请求 uri 合并 → GATEWAY_REQUEST_URL_ATTR (L96-97)
  ├── lb scheme 检测 (L81-84, host null → IllegalStateException "Invalid host")
  └── GATEWAY_SCHEME_PREFIX_ATTR (L76, 特殊 scheme 前缀)
ReactiveLoadBalancerClientFilter (62, GlobalFilter):
  ├── lb:// 检测 (L99-101) → addOriginalRequestUrl (L104)
  ├── choose (L118, SCC ReactiveLoadBalancerClient) → 无实例 → NotFoundException (L123, use404 可配)
  ├── overrideScheme (L132: isSecure → https) + DelegatingServiceInstance (L140)
  ├── reconstructURI (L162-163, LoadBalancerUriTools)
  └── GATEWAY_REQUEST_URL_ATTR 替换 (L145)
LoadBalancerServiceInstanceCookieFilter (服务实例 cookie)
DiscoveryClientRouteDefinitionLocator (49, v3): 服务发现 → 自动路由定义
```

## 基本元素分解

1. **RouteToRequestUrlFilter**: 路由 uri + 请求 uri 合并 → 转发目标
2. **ReactiveLoadBalancerClientFilter**: lb:// → SCC choose → 实例 → 重构 URL
3. **NotFound 语义**: 无实例 → 404 (可配) 或 503
4. **服务发现路由**: DiscoveryClientRouteDefinitionLocator (v3)

## 标记问题 (4)

1. **Q1 URL 装配**: RouteToRequestUrlFilter 合并语义 + lb 校验 + scheme 前缀
2. **Q2 LB 解析流程**: choose → 无实例处理 (404/503) → overrideScheme → reconstructURI
3. **Q3 SCC 交叉**: ReactiveLoadBalancerClient 的 choose (SCC-PLAN 引用) — 默认策略
4. **Q4 服务发现路由**: DiscoveryClientRouteDefinitionLocator 自动路由 (v3)

## 已读测试

- ReactiveLoadBalancerClientFilterTests (filter/ 测试族) 待读
