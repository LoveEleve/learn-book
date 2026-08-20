# Z-5 Session — 会话生命周期与过期分桶

> 前置: [[Z-3-DataTree]] (ephemerals 关联) + [[Z-4-Processor链]] (checkSession 面) | 引出: [[Z-7-ClientAPI]] | 对照: Redis 键过期 (r22) + RocketMQ 心跳会话
> 🔴 A | 8 KP | [模式: 分桶过期 + 三态会话 + 触摸续期]
> Pass 2 闭环: q1(结构) q2(分桶数学) q3(过期循环) q4(touch/check + id 生成)

**读者处境**: 客户端断连会话多久过期? 过期怎么批量清扫? 这篇拆 SessionTrackerImpl: ExpiryQueue 分桶 + SessionImpl 三态 + 触摸续期 + sessionId 位结构。

### 1. 结构 — sessionsById + ExpiryQueue + 三态

场景: 会话状态存在哪?
源码路径:
- **SessionTrackerImpl** (359): **sessionsById (ConcurrentHashMap<Long, SessionImpl>)** + **sessionExpiryQueue (ExpiryQueue<tickTime>)** (L49-51) + sessionsWithTimeout
- **SessionImpl 三态** (L56-78): isClosing 标志 + isActive/isExpired 派生 (closing 拒绝 touch)
- **注释** (L41-42): 向上取整 tick 提供**宽限期** — 会话批量过期
- **ExpiryQueue** (L35-140): **expiryMap (ConcurrentHashMap<expiryTime, Set<E>> 桶)** + elemMap (elem→expiryTime) + **nextExpirationTime (AtomicLong)**
关键设计 (q1): **会话表 + 过期桶双结构**: 查找 O(1), 过期按桶批量。[模式: 双索引]

### 2. 分桶数学 — roundToNextInterval + 迁移

场景: 过期时间怎么分桶?
源码路径:
- **roundToNextInterval** (L53-55): `(time / expirationInterval + 1) * expirationInterval` — **向上取整到桶边界**
- **update (touch)** (L84-103): 新过期时间 = roundToNextInterval(now + timeout) → putIfAbsent 桶 → **旧桶删除迁移** (elemMap.put 返回 prev → prevSet.remove)
- **remove** (L63-74): elemMap 移除 + 桶内删除
- **poll** (L130+): nextExpirationTime 到期 → 取该桶集合; 桶数上限 = maxTimeout/expirationInterval (注释 L39-41)
关键设计 (q2): **touch = 桶迁移** (O(1)); 宽限期 = 向上取整余量。[模式: 分桶]

### 3. 过期循环 — run + setSessionClosing + expirer

场景: 到期会话怎么清扫?
源码路径:
- **run** (L158-172): **getWaitTime → sleep → poll** — 到期桶批量: **setSessionClosing (isClosing=true)** + **expirer.expire(s)** (→ **close(sessionId) = 构造 closeSession 请求 submitRequest** — **过期事务化: 走正常处理器链** (Prep 预计算清扫 → 广播 → 树应用, 与正常关闭同路径, ZooKeeperServer:702-706,728-740))
- **getWaitTime** (ExpiryQueue): now < nextExpirationTime → 差值; 已过 → 0 (立即 poll)
- **setSessionClosing** (L225-235): isClosing 标志 — 过期中的会话拒绝 touch
- **removeSession** (L237-250): 会话关闭/过期后清理 (sessionsById + sessionsWithTimeout + 桶)
- **STALE_SESSIONS_EXPIRED 指标** (L168)
关键设计 (q3): **单线程过期循环** (ZooKeeperCriticalThread): 睡到下一桶 → 批量过期; closing 防重复。[模式: 定时清扫]

### 4. touch/check + sessionId 生成

场景: 客户端保活与校验?
源码路径:
- **touchSession** (L179-194): 无效/关闭会话返回 false (拒绝 touch — 会话将过期); 有效 → **updateSessionExpiry (桶迁移)**
- **checkSession** (ZooKeeperServer 面): 请求处理时校验 (Prep checkSession) — 过期 → SessionExpiredException; 移动 → SessionMovedException
- **checkGlobalSession** (L335-341): 未知 → SessionExpiredException (全局会话校验)
- **createSession** (L261-265): **nextSessionId.getAndIncrement**
- **initializeNextSessionId** (L98-108): **时间戳<<24>>>8 + serverId<<56** — 高 8 位 serverId (跨服务器唯一) + 中 40 位时间 + 低 16 位; **CONTAINER_EPHEMERAL_OWNER 特值跳过** (L104-106)
- **超时钳制**: min = tickTime×2 / max = tickTime×20 (ZooKeeperServer:1394,1403, -1 时默认); **协商钳制**: processConnectRequest (L1467+) 客户端 timeout → min/max 钳制 → cnxn.setSessionTimeout → createSession/**reopenSession (重连恢复)**
- **本地会话** (3.5+): localSessionEnabled — follower 本地会话, 首次写升级全局; **upgradeSession 防竞态** (localSessionsWithTimeouts.remove 单线程拿 timeout, UpgradeableSessionTracker:86-107) + **upgradingSessions 中间态** (本地删除↔全局添加桥接)
关键设计 (q4): **sessionId 位结构防跨服冲突**; touch 即续期; 超时钳制防极端值。[模式: 保活]

### 负面空间 — Session 刻意不做的事

- **不做毫秒精度过期**: 桶粒度 (tickTime) + 宽限期 — 会话过期延迟 ≤ 2×tick
- **不做每会话独立定时器**: 批量桶 (单线程循环)
- **不做无状态会话**: 会话必须有追踪 (Z-7 的临时会话面)
- **不做心跳专用通道**: 过期靠请求 touch (ping 也 touch — Z-7 交叉)
- **不做租约续期协商**: 超时由服务端钳制 (客户端协商值被钳)

→ 引出: 客户端怎么建会话? → [[Z-7-ClientAPI]]
