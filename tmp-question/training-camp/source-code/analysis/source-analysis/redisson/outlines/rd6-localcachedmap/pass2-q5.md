# 闭环笔记 q5: 消息族完整性 — Disable/Enable/Clear/DisableAck

## 假设
除 Invalidate/Update 外, cache/ 还有 Disable (禁用窗口)/Enable/Clear (全清)/DisableAck (禁用确认)/DisabledKey — 管理消息族支持"缓存禁用/批量清"等运维操作。

## 验证过程
- cache/ 9 消息类 (cache 包): LocalCachedMapClear/Disable/DisableAck/DisabledKey/Disable/Enable/Invalidate/Update/MessageCodec
- onMessage 分发 (LocalCacheListener.java:220-315):
  - **Disable** (L221-237): 收到禁用 → 逐个 keyHash 清本地 + `topic.publishAsync(new LocalCachedMapDisableAck())` — 禁用确认
  - **Enable** (L240): 启用 (恢复读)
  - **Clear** (L261-274): `cache.clear()` + cacheKeyMap.clear() (excludedId 排除); `isReleaseSemaphore` → 释放信号量
- 用途:
  - **Disable**: 运维禁用某实例读 (如实例 A 要维护, 禁它的读缓存)
  - **Clear**: 全实例强制清缓存 (数据大版本变更)
  - **DisableAck**: Disable 的确认 (发布者知道已生效)
  - **DisabledKey**: 禁用状态的持久化 (Redis set/multimap, L146-155)
- MessageCodec: 消息的编解码 (跨实例传输)

## 代码类型
Interface (管理消息族) — 缓存生命周期运维

## 跨域关联
- Q3 (excludedId) → Clear 也带 excluded
- Q4 (重连) → disabledCaches 恢复
- 面试点: "本地缓存怎么运维禁用?"

## 结论
管理消息族 = 缓存的运维面: Disable (禁用实例读) / Enable / Clear (全清) / DisableAck (确认) / DisabledKey (持久化状态)。让"本地缓存"可被远程运维控制, 不只能靠超时。
源码位置: LocalCacheListener.java:220-274, cache/ 消息类