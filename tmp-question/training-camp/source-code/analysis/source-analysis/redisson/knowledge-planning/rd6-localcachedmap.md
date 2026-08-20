# RD-6 RLocalCachedMap 本地缓存 — 知识规划 (knowledge-planning)

> 项目: Redisson 4.6.2-SNAPSHOT | 🔴 A / 3 篇 (+harness) | RedissonLocalCachedMap(1474)+cache/ 27
> 基线: REDISSON-PLAN RD-6 — 前置: **RD-5 (RMap 载体) + RD-4 (写失效钩子) + RD-1 (订阅)** — 展开 双层级读→SyncStrategy→excludedId→重连→消息族→CacheProvider
> 双链: 前置 [[rd5-rmap]] [[rd4-command]] | 复用 [[r22-expire]] | 对照 [[m7-cache]] [[s19-cacheable]] [[rd5-rmap]] | 引出 [[rd7-spring]]

---

## §0.8

- 🔴 A，3篇 — 双层级(**RMap 远程载体 + LocalCacheView 本地层; extends RedissonMap L44**) → 读路径(**本地 hit 零网络; miss 按 StoreMode: LOCALCACHE loader 回填 / LOCALCACHE_REDIS 查 Redis 回填; storeCacheMiss 防穿透 RedissonLocalCachedMap:285-317**) → SyncStrategy(**INVALIDATE 广播 hash 接收者清+回源 vs UPDATE 广播全值接收者直更; 带宽/回源权衡 L103-125**) → excludedId(**广播带 instanceId 排除发送者防循环 LocalCacheListener:263**) → ReconnectionStrategy(**CLEAR 全清 / LOAD loadAfterReconnection 增量补漏 (更新日志 zset); 断线错失消息修复 L317-330**) → 消息族(**Invalidate/Update/Clear/Disable/Enable/DisableAck/DisabledKey 运维管理**) → CacheProvider(**CAFFEINE window-TinyLFU vs REDISSON LRU/LFU/Soft/Weak 免依赖 LocalCacheView:312-350**) → StoreMode(**LOCALCACHE 只本地 / LOCALCACHE_REDIS 双存**) → 双一致性(**本实例写路径 cachePut + 跨实例订阅广播; RD-4 钩子命令级**) → 写路径发布(**每次 put 成功→广播; 五消息类型**)
- 设计模式: [模式: 双层级缓存+消息广播一致性+重连恢复+可插拔实现]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| RedissonLocalCachedMap.java:285-317 | 读路径 | 本地优先+StoreMode 回填 | High |
| LocalCachedMapOptions.java:64-79,121-131 | 策略枚举 | SyncStrategy/StoreMode | High |
| RedissonLocalCachedMap.java:103-125,361-401 | 写路径 | INVALIDATE/UPDATE 发布 | High |
| LocalCacheListener.java:220-330 | 消息分发 | 五消息+excluded+重连 | High |
| LocalCacheView.java:312-350 | CacheProvider | Caffeine vs 自研 | High |
| cache/ 9 消息类 | 消息族 | 管理面 | High |

---

## 02-04 聚合+分类+聚类 (3篇+harness)

**3篇理由**: 7 闭环 → 篇1 双层级与读 (q1/q6), 篇2 一致性广播 (q2/q3/q7), 篇3 重连与运维 (q4/q5)。harness 验证失效消息链。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 双层级读路径 | 🔴 | **为什么🔴**: 本地缓存核心 |
| P1-2 | SyncStrategy 广播 | 🔴 | **为什么🔴**: 一致性核心 |
| P1-3 | excludedId 防循环 | 🔴 | **为什么🔴**: 正确性 |
| P1-4 | 重连策略 | 🔴 | **为什么🔴**: 断线修复 |
| P2-1 | 消息族 | 🟡 | 运维面 |
| P2-2 | CacheProvider | 🟡 | 实现面 |
| P2-3 | 双一致性 | 🟡 | 全局 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **双层级与读** (q1/q6) | 🔴 | 核心 |
| B | **一致性广播** (q2/q3/q7) | 🔴 | 正确 |
| C | **重连与运维** (q4/q5) | 🟡 | 运维 |

---

## 05 闭环结论摘要 (Pass 2 内化)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 双层级读 | 本地 hit 零网络; miss 按 StoreMode 回填 | RedissonLocalCachedMap:285-317 |
| q2 | SyncStrategy | INVALIDATE hash 清+回源 / UPDATE 全值直更 | L103-125, LocalCacheListener:276-315 |
| q3 | excludedId | 广播带 instanceId 排除发送者防循环 | LocalCacheListener:263 |
| q4 | 重连策略 | CLEAR 全清 / LOAD 增量补漏 (更新日志 zset) | LocalCacheListener:317-330 |
| q5 | 消息族 | Disable/Enable/Clear/DisableAck 运维 | LocalCacheListener:220-274 |
| q6 | CacheProvider | Caffeine vs 自研 LRU/LFU/Soft/Weak | LocalCacheView:312-350 |
| q7 | 双一致性 | 本实例 cachePut + 跨实例广播 + RD-4 钩子 | RedissonLocalCachedMap:361-401 |

→ 引出 RD-7: Spring Cache 怎么用 RLocalCachedMap/RedissonCache — [[rd7-spring]]