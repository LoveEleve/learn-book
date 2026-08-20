# G-8 RLS 数据面路由 — 知识规划 (KP)

> 域级: 🟡 B | 模块: rls/ (16 文件: CachingRlsLbClient 1104/LbPolicyConfiguration 475/AdaptiveThrottler 343/LinkedHashLruCache 329/RlsProtoData 253/RlsLoadBalancer) + xds 反射集成对照
> 日期: 2026-08-16 | 版本: 1.83.1 | Pass 2 闭环: q1(路由模型) q2(缓存三层) q3(节流) q4(退避) q5(xds 集成) — **5/5 全闭环**

## 一、机制提取 (逐源)

### M1 路由模型 (q1)
- 核心注释 (CachingRlsLbClient.java:84-85): "Every single request is routed by the server's decision. To reduce the performance penalty, LruCache is used"
- 结构 (L120-150): Throttler + LbPolicyConfiguration + RouteLookupServiceStub (L128) + RlsPicker (L203) + fallbackChildPolicyWrapper (L137)
- DataCacheEntry (L675-744): targets → 引用计数子策略 (L689-691) + TF 跳过 (L733-744)

### M2 缓存三层 (q2)
- 锁 (L108): "All cache status changes (pending, backoff, success) must be under this lock"
- DataCacheEntry (L675-760): minEvictionTime (5s L95)/expireTime/staleTime + OV 异步刷新时间线 (L695-700) + pending 去重 (L705-708)
- BackoffCacheEntry (L803): 冷却 (q4); PendingCacheEntry: 在途

### M3 自适应节流 (q3)
- 默认 (AdaptiveThrottler.java:45-47): HISTORY 30s/PADDING 8/RATIO 2.0f
- 双计数 (L65-71): requestStat/throttledStat; shouldThrottle (L86) 随机采样

### M4 退避缓存 (q4)
- BackoffCacheEntry (L803-837): Status + BackoffPolicy + expiryTimeNanos + isInBackoffPeriod (L827-829) + cleanup (L830-832)

### M5 xds 集成 (q5)
- RouteLookupServiceClusterSpecifierPlugin (L30-70): typeUrls + **Class.forName 反射** (L55-59, "Dependency for 'io.grpc:grpc-rls' is missing") + 配置解析 (L60-70)

## 二、聚合分级

| 级别 | 机制 |
|---|---|
| P1 | M1 每请求决策 + 子 LB / M2 三层缓存 + OV 刷新 |
| P2 | M3 比例节流 / M4 退避冷却 |
| P3 | M5 反射集成 |

## 三、叙事线

场景: 服务网格里"去哪"由中心决策 — RLS 服务器。读者疑问链: 每请求问服务器?不 (M1 缓存) → 缓存怎么组织 (M2) → 服务器过载怎么办 (M3 节流 + M4 退避) → 和 xds 什么关系 (M5)。
