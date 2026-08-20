# 闭环笔记 q3: excludedId 过滤 — 消息循环防护

## 假设
每条失效/更新消息带 instanceId (发送者), 接收端用 excludedId 排除自己 — 防止"自己写→自己广播→自己再处理"的循环。

## 验证过程
- 消息构造: `new LocalCachedMapInvalidate(instanceId, keyHash)` / `Update(instanceId, ...)` — instanceId 标识发送实例
- onMessage 过滤 (LocalCacheListener.java:263,278,297): `if (!Arrays.equals(msg.getExcludedId(), instanceId))` — **排除发送者**
- 语义: 实例 A 写 → 广播含 A 的 id → A 收到但不处理 (本地已由写路径更新); B/C 收到处理
- 为什么: 写路径已本地 cachePut (getAsync 前或 putOperation), 重复处理浪费 + 可能旧值覆盖新值 (Update 场景)
- instanceId: 每 Redisson 实例唯一 (ServiceManager.generateId)

## 代码类型
Implementation (消息去重) — 发送者排除

## 跨域关联
- Q2 (消息) → 所有消息带 instanceId
- RD-1 (ServiceManager id) → instanceId 来源
- 分布式一致性: 防自处理循环

## 结论
excludedId = 消息循环防护: 广播带 instanceId, 接收端排除自己。写路径本地已更新, 自处理纯浪费 (Update 还可能旧覆新)。多实例协作的一致密钥。
源码位置: LocalCacheListener.java:263,278,297