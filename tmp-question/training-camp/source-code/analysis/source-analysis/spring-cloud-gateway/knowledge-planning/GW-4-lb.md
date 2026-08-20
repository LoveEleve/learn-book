# GW-4 负载均衡与服务发现 — 知识规划 (KP)

> 域级: 🟡 B | 模块: filter/ReactiveLoadBalancerClientFilter + RouteToRequestUrlFilter + LoadBalancerServiceInstanceCookieFilter + discovery/ (v3) + **SCC 交叉 (ReactiveLoadBalancerClient)**
> 日期: 2026-08-16 | 版本: 4.3.2 | Pass 2 闭环: q1(URL 装配) q2(LB 解析) q3(SCC 交叉) q4(服务发现路由) — **4/4 全闭环**

## 一、机制提取 (逐源)

### M1 URL 装配 (q1)
- RouteToRequestUrlFilter (39-97): 合并 (L88-96, 请求路径保留 + 路由 scheme/host/port 覆盖) + lb host 校验 (L81-84, "Invalid host") + GATEWAY_SCHEME_PREFIX_ATTR (L73-77)
- 输出: GATEWAY_REQUEST_URL_ATTR (L94)

### M2 LB 解析 (q2)
- ReactiveLoadBalancerClientFilter (62-163): lb:// 触发 (L99-101) → choose (L118) → 无实例 NotFoundException (L123, use404 可配) → overrideScheme (L132, secure→https) → DelegatingServiceInstance (L140) → reconstructURI (L162-163, LoadBalancerUriTools)
- addOriginalRequestUrl (L104)

### M3 SCC 交叉 (q3)
- LoadBalancerClientFactory 注入 (L80-86); choose 委托 SCC ReactiveLoadBalancerClient (默认 RoundRobin)
- LoadBalancerServiceInstanceCookieFilter (45-56): 粘性 cookie
- 交叉引用 SCC-PLAN SCC-7

### M4 服务发现路由 (q4)
- DiscoveryClientRouteDefinitionLocator (49-130): 实例→RouteDefinition (L104-130) + **SpEL 模板** (L115,125,146) + DelegatingServiceInstance (L109)
- 去重 (L105-107); 刷新链 (GW-1 心跳)

## 二、聚合分级

| 级别 | 机制 |
|---|---|
| P1 | M2 每请求 choose + 404/503 语义 / M1 URL 覆盖式装配 |
| P2 | M3 SCC 分层消费 / M4 SpEL 模板路由 |

## 三、叙事线

场景: `uri: lb://user-service` — 一行配置, 请求怎么到实例?读者疑问链: URL 怎么装配 (M1) → LB 怎么选实例 (M2) → 策略谁提供 (M3 SCC) → 服务路由怎么自动来 (M4)。
