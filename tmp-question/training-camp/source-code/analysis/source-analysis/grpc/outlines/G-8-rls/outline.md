# G-8 RLS — 路由决策外包: 每请求问服务器, 缓存兜住性能

> 前置: [[G-4-负载均衡]] (SPI/子策略) + [[G-7-xDS]] (ClusterSpecifierPlugin 挂载) | 引出: 阶段收官 (RLS 是控制面家族最后一环) | 对照: Envoy RLS + xDS 数据面路由
> 🟡 B | 5 KP | [模式: 外包决策 + 多层缓存 + 雪崩防护]
> Pass 2 闭环: q1(路由模型) q2(缓存) q3(节流) q4(退避) q5(集成) — **5/5 全闭环**

**读者处境**: 服务网格里"这次请求路由到哪个集群"不该写死 — 它由一个中心服务决定。每次请求都问它?太慢。问完缓存?缓存多久?它挂了怎么办?它过载了怎么办?RLS (Route Lookup Service) 就是这套"外包决策 + 本地缓存"的完整方案。

### 1. 路由模型 — 每请求服务端决策

场景: RLS 服务器怎么参与每次路由?
源码路径:
- **核心注释** (CachingRlsLbClient.java:84-85): "routing by fetching the decision from route lookup server. **Every single request is routed by the server's decision**. To reduce the performance penalty, LruCache is used" — 外包决策 + 缓存减损
- **结构** (L120-150): Throttler + LbPolicyConfiguration + **RouteLookupServiceStub** (CachingRlsLbClient.java:130, RLS 服务器 gRPC 客户端) + RlsPicker (L203) + **fallbackChildPolicyWrapper** (CachingRlsLbClient.java:136, 默认目标兜底)
- **决策→子 LB** (L685-744): RLS 返回 targets → **refCountedChildPolicyWrapperFactory.createOrGet** (L689-691, 引用计数复用) → RlsPicker 每 RPC 选 → **TF 子策略跳过** (L733-744)
关键设计 (q1): **决策与控制分离**: 路由策略集中 (服务网格控制面), 数据面执行; 子 LB 复用 G-4 策略家族。**被放弃的方案: 本地静态路由表** — 无法响应动态策略。 [跨域: G-4 SPI 子策略] [分布式: 决策与控制分离]

### 2. 缓存三层 — Data/Backoff/Pending 状态机

场景: 缓存只存结果吗?服务器没回怎么办?
源码路径:
- **锁模型** (L108): "All cache status changes (pending, backoff, success) must be under this lock"
- **DataCacheEntry** (L675-760): **minEvictionTime** (5s, CachingRlsLbClient.java:94) + **expireTime** (maxAge) + **staleTime** (staleAge) + 子策略引用 (L689-691)
- **OV 异步刷新** (L695-700 时间线注释): "entry1: Pending | hasValue | staled | ... entry2: | OV* | pending | hasValue | staled" — **stale 时旧值继续服务 + 后台刷新** (maybeRefresh L701-717)
- **pending 去重** (L705-708): 同 key 只一个在途查询
- BackoffCacheEntry (L803): 失败冷却 (q4); PendingCacheEntry: 在途占位
关键设计 (q2): 三层条目 + 三时间 (minEviction/expire/stale): 成功 (Data)/失败 (Backoff)/在途 (Pending); **stale 条目的旧值+异步刷新**保证 RLS 抖动时路由不中断。**被放弃的方案: 单层缓存** — 失败无冷却直击服务器。 [算法: 缓存一致性/异步刷新]

### 3. 自适应节流 — 比例式雪崩防护

场景: RLS 服务器过载, 客户端怎么自我抑制?
源码路径:
- **默认参数** (AdaptiveThrottler.java:45-47): HISTORY=30s (L45) / **PADDING=8** (L46, "High numbers throttle less") / **RATIO_FOR_ACCEPT=2.0f** (L47, 发送率 = 2x 接受率)
- **双计数** (L65-71): requestStat (尝试) + throttledStat (被节流) — 30s 滑动窗口
- **shouldThrottle** (L86): 随机采样 + 比例判定 — 部分放行保采样
关键设计 (q3): **滑动窗口比例控制**: 节流强度跟随服务端接受率自适应; padding 调激进程度。**被放弃的方案: 固定限流** — 无法适应容量变化。 [算法: 滑动窗口] [跨域: G-6 throttle 同思想]

### 4. 退避缓存 — 失败冷却闸门

场景: RLS 查询失败, 接下来怎么办?
源码路径:
- **BackoffCacheEntry** (L803-837): Status + **BackoffPolicy** (L806, G-6 退避) + expiryTimeNanos (L807) + scheduledFuture
- **isInBackoffPeriod** (L827-829): `!scheduledFuture.isDone()` — 冷却期判定
- **isExpired** (L826-828) + **cleanup** (L830-832, cancel 定时)
- **防雪崩**: 冷却期内同 key 请求本地失败 (走默认目标, q1 fallback)
关键设计 (q4): 失败 → 冷却条目 (G-6 退避 + 过期), 冷却期内不打服务器 — **雪崩防护三板斧之一** (另: LRU 容量 + q3 节流)。**被放弃的方案: 失败即清缓存直查** — 雪崩。 [跨域: G-6 退避复用] [分布式: 雪崩防护]

### 5. xds 集成 — 反射加载的插件

场景: RLS 和 xds 什么关系?强制依赖吗?
源码路径:
- **ClusterSpecifierPlugin** (RouteLookupServiceClusterSpecifierPlugin.java:30-44): typeUrls (L41-44)
- **反射加载** (L55-59): `Class.forName("io.grpc.lookup.v1.RouteLookupClusterSpecifier")` → 缺失 → "**Dependency for 'io.grpc:grpc-rls' is missing**"
- **配置解析** (L60-70): Any.unpack → JSON → routeLookupConfig
关键设计 (q5): **可选依赖 SPI**: xds 编译零依赖 rls, 运行时反射; 缺失报错不破坏主功能。**被放弃的方案: 编译期依赖** — 强制所有 xds 用户带 rls。 [架构: 可选依赖/反射] [跨域: G-7 ClusterManager 挂载]

### 核心悬念

"gRPC 的'去哪'现在有了三种答案: DNS (静态)、xDS (控制面配置)、RLS (每请求决策) — 加上前面的 Retry/Hedging、LoadBalancer 家族, gRPC 的数据面治理全貌闭合。" 本书 gRPC 阶段收官 — 8 个域的知识网络在此汇聚。

### 负面空间 (不做)

1. 不写 RLS 协议 wire format (protobuf 细节)
2. 不写 LinkedHashLruCache 实现细节 (JDK LinkedHashMap 变体)
3. 不写 LbPolicyConfiguration 校验规则穷举
4. 不写 RlsProtoConverters 转换器细节
5. 不写 ChildLbResolvedAddressFactory 细节 (子策略装配面)
6. 不写 RLS 服务器端实现 (只讲客户端)
