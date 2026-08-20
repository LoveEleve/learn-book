# 闭环笔记 q4: ReconnectionStrategy — 断线重连后缓存怎么恢复

## 假设
订阅断开重连后, 本地缓存可能已陈旧 (错过了断线期间的消息)。ReconnectionStrategy 决定: CLEAR (全清) vs LOAD (增量补漏 — 从更新日志只失效断线期变更)。

## 验证过程
- ReconnectionStrategy 枚举 (LocalCachedMapOptions.java:42-60): NONE / CLEAR / LOAD
- onSubscribe (LocalCacheListener.java:317-330): **重连订阅成功时触发**:
  - **CLEAR** (L319-325): `cache.clear()` + cacheKeyMap.clear() — 全清, 下次读 miss 回源
  - **LOAD** (L326-329): `lastInvalidate > 0` 时 → `loadAfterReconnection()` (L476-511)
- **loadAfterReconnection 增量补漏** (L476-511):
  - L477-482: `now - lastInvalidate > cacheUpdateLogTime` (断线太久) → cache.clear() — 放弃太旧
  - L484-493: map 不存在 (isExistsAsync false) → cache.clear()
  - L495-509: **RScoredSortedSet 更新日志** (getUpdatesLogName) → `valueRangeAsync(lastInvalidate, +inf)` 取断线期变更 → 逐个 `cache.remove(keyHash)` — **只失效断线期变更的 key, 保留其余热数据**
- lastInvalidate (L317 上下文): onMessage 收到过消息才 LOAD (实例已用过)
- NONE: 不处理 (断线期错过的失效不补 — 可能陈旧)
- 代价权衡: CLEAR 简单但全 miss 风暴; **LOAD 增量 (更新日志 zset) 高效 — 只失效断线窗口内的变更**
- disabledCaches 恢复 (L133-155): LOAD 时从 disabled set/multimap 恢复禁用状态

## 代码类型
Implementation (重连恢复) — 增量补漏 vs 全清

## 跨域关联
- Q2 (消息) → LOAD 使 invalidateEntryOnChange=2
- RD-1 (订阅) → 重连由订阅体系触发
- RScoredSortedSet → 更新日志 zset (RD-8 基础结构)

## 结论
重连策略 = 断线期错失消息的修复: CLEAR 全清 (简单, 全 miss) vs **LOAD 增量补漏** (更新日志 zset 只失效断线期 key — 高效保热数据)。NONE 不补 (接受陈旧窗口)。比"全量重载"聪明得多。
源码位置: LocalCacheListener.java:317-330,476-511, LocalCachedMapOptions.java:42-60