# 闭环笔记 Q2 — 缓存三层语义: Data/Backoff/Pending

假设: RLS 缓存不只存"成功结果" — 三种条目 (Data/Backoff/Pending) 各自处理成功/失败/进行中, 保证缓存一致性。

验证过程:
- **锁模型** (CachingRlsLbClient.java:108): "All cache status changes (pending, backoff, success) must be under this lock" — 状态变更锁
- **DataCacheEntry** (L675-760): RouteLookupResponse + **minEvictionTime** (now + MIN_EVICTION_TIME_DELTA_NANOS, L94=5s, 防抖) + **expireTime** (maxAge) + **staleTime** (staleAge) + childPolicyWrappers (引用计数, L689-691)
- **状态流转时间线** (L695-700 注释): "entry1: Pending | hasValue | staled | ... entry2: | OV* | pending | hasValue | staled" — **旧值+异步刷新 (OV)**: stale 条目保留旧值同时后台刷新 (maybeRefresh L701-717, REASON_STALE)
- **pending 去重** (L705-708): `pendingCallCache.containsKey → return` — 同 key 只发一个在途查询
- BackoffCacheEntry (L803): 查询失败 → 退避条目 (防雪崩, q4); PendingCacheEntry: 在途查询占位

代码类型: Algorithmic (缓存状态机)

结论: 三层条目 + 时间维度 (minEviction/expire/stale): **Data** (成功结果, 过期前服务) / **Backoff** (失败冷却, 防雪崩) / **Pending** (在途去重); stale 条目的"旧值继续用 + 异步刷新" (OV 语义) 保证 RLS 抖动时路由不中断。**被放弃的方案: 单层缓存 (只存结果)** — 失败时无冷却会直击 RLS 服务器; 三层让缓存成为完整的"决策状态机"。 [算法: 缓存一致性/异步刷新] [跨域: G-6 退避策略复用] (CachingRlsLbClient.java:84-108,675-760)
