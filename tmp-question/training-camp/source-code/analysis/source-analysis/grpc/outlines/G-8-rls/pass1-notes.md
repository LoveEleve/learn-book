# G-8 Pass 1 扫描笔记 — RLS 数据面路由

> 日期: 2026-08-16 | 版本: 1.83.1 | 🟡 B | 模块: rls/ (16 文件: CachingRlsLbClient 1104/LbPolicyConfiguration 475/AdaptiveThrottler 343/LinkedHashLruCache 329/RlsProtoData 253/RlsLoadBalancer/RlsLoadBalancerProvider) + xds 反射集成对照

## 继承树/调用图

```
RlsLoadBalancerProvider (38, extends LoadBalancerProvider) — G-4 SPI 注册
  → RlsLoadBalancer (32, extends LoadBalancer)
  → CachingRlsLbClient (88, @ThreadSafe)
      ├── RlsAsyncLruCache (LRU, 数据+退避条目)
      ├── pendingCallCache (L116, 进行中查询去重)
      ├── AdaptiveThrottler (43) — 自适应节流
      ├── RlsPicker (L203) — 每 RPC 决策
      └── RouteLookupServiceStub (L54-55) — RLS 服务器 gRPC 客户端
xds 集成: RouteLookupServiceClusterSpecifierPlugin (xds, 反射加载 grpc-rls)
```

## 基本元素分解

1. **RlsLoadBalancer/Provider**: LoadBalancer SPI 消费 (G-4)
2. **CachingRlsLbClient**: 数据面核心 — 每请求查缓存 → miss 时调 RLS 服务器 → 缓存结果
3. **LRU 缓存**: LinkedHashLruCache (访问序) + 条目类型 (Data/Backoff/Pending)
4. **AdaptiveThrottler**: 客户端限流 (RLS 服务器过载保护)
5. **LbPolicyConfiguration**: 子 LB 配置 (RLS 决策 → 目标集群 → 子策略)

## 标记问题 (5)

1. **Q1 路由模型**: RLS 的"每请求服务端决策" — 请求 → RLS 服务器 → 目标集群 (target cluster) → 子 LB
2. **Q2 缓存三层**: DataCacheEntry/BackoffCacheEntry/PendingCacheEntry — 各自语义
3. **Q3 自适应节流**: AdaptiveThrottler — 客户端比例限流 (G-6 throttle 的独立实现)
4. **Q4 退避缓存**: 查询失败 → 退避条目 (防 RLS 服务器雪崩)
5. **Q5 xds 集成**: RouteLookupServiceClusterSpecifierPlugin — 反射加载机制

## 已读测试

- AdaptiveThrottlerTest: shouldThrottle (L43)/negativeTickerValues (L118)
- LinkedHashLruCacheTest: eviction_size (L75)
