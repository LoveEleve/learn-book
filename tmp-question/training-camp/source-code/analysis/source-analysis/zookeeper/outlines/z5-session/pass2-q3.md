# 闭环笔记 q3: 过期循环 — run + setSessionClosing + expirer

## 假设
单线程定时循环; 到期桶批量过期; closing 防重复。

## 验证过程
- **run** (SessionTrackerImpl:158-172): `getWaitTime()` → >0 sleep → **poll() 到期桶** → 逐会话: **setSessionClosing(s.sessionId)** + **expirer.expire(s)** (L167-171)
- **getWaitTime** (ExpiryQueue): now < nextExpirationTime → 差值; 已过 → 0 (立即 poll)
- **setSessionClosing** (L225-235): sessionsById 存在 → `s.isClosing = true` — **过期中会话拒绝 touch** (防死锁: touch 与 expire 竞争)
- **expire 动作**: expirer (ZooKeeperServer) → killSession (Z-3: ephemerals.remove + deleteNodes) — 会话过期级联清临时节点
- **removeSession** (L237-250): sessionsById.remove + sessionsWithTimeout.remove + **sessionExpiryQueue.remove** (三处清理)
- **STALE_SESSIONS_EXPIRED 指标** (L168)

## 代码类型
Implementation (定时批量清扫)

## 跨域关联
- Z-3: killSession (L1121-1156) — ephemeral 级联
- Z-4: closeSession txn (正常关闭走 Prep)

## 结论
过期 = 单线程 sleep-poll 循环 (睡到下一桶) → 批量 setSessionClosing + expire → killSession 级联清 ephemeral; closing 防 touch 竞争。
源码位置: SessionTrackerImpl.java:158-172,225-250; ExpiryQueue.java:getWaitTime/poll
