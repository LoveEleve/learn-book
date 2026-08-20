# RD-6 篇3 — reconnection-ops: 断线恢复与缓存运维

> 前置: [[RD-6-篇2]] (广播) | 复用: [[rd1-connection]] (订阅重连) | 对照: [[rd5-rmap]] (EvictionTask) [[r22-expire]] (过期) | 引出: [[rd7-spring]] (Spring Cache)
> 🔴 A | 2 KP | [模式: 重连恢复 + 管理消息族]
> Pass 2 闭环: q4(重连策略) q5(消息族)

**读者处境**: 订阅断了几秒, 期间其他实例改了数据 — 重连后你的本地缓存还是旧的!怎么办?全清重来 (CLEAR) 还是从 Redis 重新拉 (LOAD)?运维想禁用某个实例的缓存读怎么办?这篇拆断线恢复的两策略, 和 Disable/Enable/Clear 等管理消息族。

### 概念依赖链
q4(重连策略) ← q5(消息族) — 先讲断线后怎么修复一致性, 再讲运维怎么远程控制缓存。

### 核心悬念
"订阅断线几秒, 本地缓存变陈旧 —— 重连后 CLEAR 全清还是 LOAD 重载？"

### 叙事顺序
1. 问题引入: 断线期错过的失效消息
2. 重连策略 (q4) — CLEAR 全清 vs LOAD 增量补漏 (更新日志 zset)
3. 管理消息族 (q5) — Disable/Enable/Clear/DisableAck
4. 收束: "缓存也要运维"

### 1. 重连策略 — 断线后的修复

场景: 重连后缓存还新鲜吗?
源码路径:
- ReconnectionStrategy 枚举 (LocalCachedMapOptions.java:42-60): NONE / CLEAR / LOAD
- onSubscribe (LocalCacheListener.java:317-330): **重连订阅成功时触发**:
  - **CLEAR** (LocalCacheListener.java:319-325): `cache.clear()` + cacheKeyMap.clear() — 全清, 下次读 miss 回源
  - **LOAD** (LocalCacheListener.java:326-329): `lastInvalidate > 0` 时 → `loadAfterReconnection()` (L476-511) — **增量补漏, 非全量**
- **loadAfterReconnection 增量补漏** (LocalCacheListener.java:476-511):
  - L477-482: 断线超 `cacheUpdateLogTime` = **10 分钟** (RedissonLocalCachedMap.java:55) → 全清 (太旧直接放弃, 文档 "whole cache will be cleaned otherwise")
  - L484-493: map 不存在 → 全清
  - L495-509: **RScoredSortedSet 更新日志** (getUpdatesLogName) → `valueRangeAsync(lastInvalidate, +inf)` 取断线期变更 → 逐个 `cache.remove(keyHash)` — **只失效断线期变更的 key** (文档: 失效日志保存 10 分钟, 断线 <10min 只清日志里的 key)
- **默认 NONE** (LocalCachedMapOptions.java:267 "NONE - Default. No reconnection handling") — 需显式配 LOAD/CLEAR
- lastInvalidate (onMessage 记录): 实例用过才 LOAD
- NONE: 不补 (接受陈旧窗口)
- disabledCaches 恢复 (LocalCacheListener.java:133-155): LOAD 时从 disabled set/multimap 恢复
关键设计 (q4): 重连修复: CLEAR 全清 (简单) vs **LOAD 增量补漏** (从更新日志 zset 只失效断线期变更 — 高效非全量)。[模式: 重连恢复策略]
数据流: 重连 → CLEAR? 全清 : LOAD? 查更新日志 → 失效断线期 key → 保持其余。

### 2. 管理消息族 — 远程运维缓存

场景: 怎么禁用一个实例的缓存读?
源码路径:
- cache/ 9 消息类: Clear/Disable/Enable/DisableAck/DisabledKey/Invalidate/Update/MessageCodec
- onMessage 分发 (LocalCacheListener.java:220-274):
  - **Disable** (LocalCacheListener.java:221-237): 收禁用 → 逐个 keyHash 清本地 + `publishAsync(DisableAck)` — 确认生效
  - **Enable** (LocalCacheListener.java:240): 恢复读
  - **Clear** (LocalCacheListener.java:261-274): `cache.clear()` (excludedId 排除); isReleaseSemaphore → 释放信号量
- 用途:
  - Disable: 运维禁某实例读 (维护/排障)
  - Clear: 全实例强清 (数据大版本变更)
  - DisableAck: 禁用确认 (发布者知已生效)
  - DisabledKey: 禁用状态持久化 (Redis set/multimap)
关键设计 (q5): 管理消息族 = 缓存的运维面: 禁用/启用/全清 + 确认/持久化。[模式: 管理消息族]
数据流: 运维 → Disable 广播 → 实例清缓存 + Ack → 运维知生效。

### 负面空间 — 重连与运维刻意不做的事

- **不做断线期消息重放**: 错过的消息不补, 靠重连策略修复
- **不做增量重载**: LOAD 全量, 无"只拉断线期变更"
- **不做禁用自动过期**: 禁用的实例需手动 Enable
- **不区分禁用粒度**: Disable 按实例, 非按 key 范围 (除 hash 列表)
- **Clear 无回滚**: 全清后旧缓存不可恢复

→ 引出: Spring Cache 怎么用 RedissonCache?→ [[rd7-spring]]