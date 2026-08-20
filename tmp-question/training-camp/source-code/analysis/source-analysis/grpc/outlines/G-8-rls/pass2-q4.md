# 闭环笔记 Q4 — BackoffCacheEntry: 失败冷却, 防 RLS 雪崩

假设: RLS 查询失败 → 缓存退避条目 (期间直接返回失败/默认目标), 冷却期由 G-6 退避策略决定 — 防失败风暴直击 RLS 服务器。

验证过程:
- **BackoffCacheEntry** (CachingRlsLbClient.java:803-837): Status + **BackoffPolicy** (L806, G-6 指数退避) + expiryTimeNanos (L807) + scheduledFuture
- **isInBackoffPeriod** (L827-829): `!scheduledFuture.isDone()` — 冷却期判定
- **isExpired** (L826-828): `nowNanos > expiryTimeNanos` — 退避条目也过期 (LRU 可逐出)
- **cleanup** (L830-832): `scheduledFuture.cancel(false)` — 条目移除时取消定时
- **防雪崩语义**: 查询失败期间同 key 请求不再打 RLS 服务器 (命中 Backoff 条目 → 直接失败/默认目标, 与 q1 的 fallbackChildPolicyWrapper 协同)
- 条目大小记账 (L817-820): OBJ_OVERHEAD_B × 3 + Long.SIZE + 8 — LRU 容量管理

代码类型: Implementation (失败冷却)

结论: 退避条目 = **失败后的冷却闸门**: RLS 查询失败 → 缓存 Backoff 条目 (带 G-6 退避 + 过期时间), 冷却期内同 key 请求本地失败 (走默认目标), 冷却结束自动清理 — **防雪崩三板斧之一** (另两个: LRU 容量 + 自适应节流 q3)。**被放弃的方案: 失败即清缓存直查** — 故障时每请求都打 RLS 服务器 (雪崩); 冷却让故障窗口内流量留在本地兜底。 [跨域: G-6 ExponentialBackoffPolicy 复用; G-8 q3 节流协同] [分布式: 雪崩防护] (CachingRlsLbClient.java:803-837)
