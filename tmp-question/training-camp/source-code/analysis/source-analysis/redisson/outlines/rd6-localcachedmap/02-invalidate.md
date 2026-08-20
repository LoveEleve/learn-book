# RD-6 篇2 — invalidate: 一致性广播与消息链

> 前置: [[RD-6-篇1]] (双层级) | 复用: [[rd1-connection]] (订阅通道) | 对照: [[rd5-rmap]] (MapWriter) [[r28-networking]] [[r29-pubsub]] (PubSub 双面) | 引出: [[RD-6-篇3]] (重连运维) + [[rd4-command]] (evict 钩子)
> 🔴 A | 3 KP | [模式: 广播失效 + 排除发送者 + 带宽权衡]
> Pass 2 闭环: q2(SyncStrategy) q3(excludedId) q7(双一致性)

**读者处境**: 两个 JVM 实例都用 RLocalCachedMap 读同一数据 — 实例 A 改了, 实例 B 的本地缓存怎么知道?广播 hash 还是广播值?自己改的自己会收到消息吗?这篇拆一致性的核心: SyncStrategy (INVALIDATE 失效 vs UPDATE 更新), excludedId 防循环, 以及"本实例写路径 + 跨实例订阅"的双通道。

### 概念依赖链
q2(SyncStrategy) ← q3(excludedId) ← q7(双一致性) — 先讲广播什么 (hash/值), 再讲怎么防自处理, 最后讲双通道分工。

### 核心悬念
"实例 A 改了数据, 实例 B 的本地缓存怎么保持新鲜？广播 hash 还是值？自己广播的消息自己会再处理吗？"

### 叙事顺序
1. 问题引入: 跨实例的本地缓存一致性
2. SyncStrategy (q2) — INVALIDATE hash vs UPDATE 值
3. excludedId (q3) — 广播带 id 排除发送者
4. 双一致性 (q7) — 本实例写路径 + 跨实例订阅 + RD-4 钩子
5. 收束: "广播 = 带宽 vs 回源 的权衡"

### 1. SyncStrategy — hash 失效 vs 值更新

场景: 广播什么才能让 B 更新?
源码路径:
- SyncStrategy 枚举 (LocalCachedMapOptions.java:64-79): NONE / INVALIDATE / UPDATE; **默认 INVALIDATE** (L281 "INVALIDATE - Default. Invalidate cache entry across all LocalCachedMap instances")
- invalidateEntryOnChange (RedissonLocalCachedMap.java:103-107): INVALIDATE→1 / UPDATE→1 / NONE→0
- 写发布 (RedissonLocalCachedMap.java:116-125):
  - **UPDATE → `LocalCachedMapUpdate(instanceId, mapKey, mapValue)`** (带值广播)
  - **INVALIDATE → `LocalCachedMapInvalidate(instanceId, keyHash)`** (只 hash)
- 接收处理 (LocalCacheListener.java:276-315):
  - Invalidate → `cache.remove(keyHash)` — 本地移除 → 下次读 miss 回源
  - Update → `updateCache(key, value)` — **直更本地, 零回源**
- **PubSub 面** (对照 [[r28-networking]] [[r29-pubsub]]): r28 讲服务端 RESP/发布订阅协议, r29 讲 Redis 的 PubSub 域; 本域的失效广播是**客户端侧对 PubSub 的应用** — 复用 channel 机制传输 LocalCachedMapInvalidate/Update 消息, 加 instanceId 做应用层过滤
- 权衡: INVALIDATE 广播小 (hash), 但接收端回源; UPDATE 广播大 (全值), 但零回源
- 选型: 值小/多读 → UPDATE; 值大/少读 → INVALIDATE
关键设计 (q2): 一致性两档: hash 失效 (省带宽) vs 值更新 (省回源)。[模式: 失效/更新权衡]
数据流: A put → 广播 (hash 或 值) → B 清本地 或 直更 → 后续读 回源 或 直返。

### 2. excludedId — 自己广播的不处理

场景: A 写后, A 会收到自己的广播吗?
源码路径:
- 消息带 instanceId: `LocalCachedMapInvalidate(instanceId, keyHash)` / `Update(instanceId, ...)`
- onMessage 过滤 (LocalCacheListener.java:263,278,297): `if (!Arrays.equals(msg.getExcludedId(), instanceId))` — **排除发送者**
- 为什么: 写路径已本地 cachePut (A 本地已最新), 自处理浪费 + Update 可能旧值覆盖新值
- instanceId: 每 Redisson 实例唯一
关键设计 (q3): excludedId = 防自处理循环; 写路径已更新本地, 自己收到纯浪费。[模式: 发送者排除]
数据流: A put → 本地已更 → 广播 (带 A id) → A 收到跳过 / B 处理。

### 3. 双一致性 — 本实例写 + 跨实例订阅

场景: 一致性是不是只有广播一种?
源码路径:
- **本实例** (RedissonLocalCachedMap.java:331-344): putOperationAsync 先 `cachePut` 本地直更 (L334)
- **跨实例** (RedissonLocalCachedMap.java:361-401): 写成功后 publish Invalidate/Update
- **RD-4 钩子** (CommandAsyncService.java:717-727): async() 写命令成功 → evictClientSideCaching — 命令级通用
- 三层: 本实例 (直更) / 他实例 (订阅) / 命令级 (evict 钩子)
关键设计 (q7): 一致性三通道: 写路径本地直更 + 订阅跨实例 + RD-4 命令级钩子。[模式: 多通道一致]
数据流: put → 本地直更 → 广播他实例 → (RD-4 钩子命令级兜底)。

### 负面空间 — 一致性广播刻意不做的事

- **不保证实时一致**: 订阅消息有网络/处理延迟 (最终一致窗口)
- **不做版本号冲突检测**: Update 无条件覆盖, 无 CAS 版本 (除非显式)
- **不保证广播可靠**: 订阅断线期间的消息丢失靠重连策略 (篇3)
- **不广播控制台操作**: 直接 Redis 改 (非本 map API) 不触发广播
- **不区分读多写少优化**: SyncStrategy 静态, 不按流量动态切
- **不合并接收端回源**: INVALIDATE 后多个实例 miss 各自回源 (无共享), 高并发下可能回源风暴 (completeness Q28)

→ 引出: 断线了本地缓存怎么恢复?运维怎么禁用?→ [[RD-6-篇3]]