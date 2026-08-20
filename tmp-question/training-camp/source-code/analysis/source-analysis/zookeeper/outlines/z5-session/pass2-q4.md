# 闭环笔记 q4: touch/check + sessionId 生成

## 假设
touch 即续期 (桶迁移); check 校验会话有效; sessionId 位结构防跨服冲突。

## 验证过程
- **touchSession** (SessionTrackerImpl:179-194): 无效 (null) → false; **isClosing → false (拒绝续期)**; 有效 → updateSessionExpiry (桶迁移) → true
- **checkSession** (SessionTracker 接口): 请求处理时校验 — 过期 → SessionExpiredException; 移动 → SessionMovedException (Prep 面 Z-4)
- **checkGlobalSession** (L335-341): checkSession 捕获 UnknownSession → **SessionExpiredException**
- **createSession** (L261-265): nextSessionId.getAndIncrement + trackSession
- **initializeNextSessionId** (L98-108): `nextSid = (时间戳 << 24) >>> 8; nextSid |= (serverId << 56)` — **高 8 位 serverId** (跨服务器唯一) + 中 40 位时间 + 低 16 位; **== CONTAINER_EPHEMERAL_OWNER → ++跳过** (L104-106, 3.6 容器特值)
- **超时钳制** (ZooKeeperServer:1394,1403): min = tickTime×**2** / max = tickTime×**20** (-1 时默认) — 客户端协商值被钳
- **本地会话** (3.5+): localSessionEnabled (ZooKeeperServer:227) — follower 本地建会话 (免全局广播), **首次写升级全局** (L1477 面)

## 代码类型
Implementation (保活 + 校验 + id 生成)

## 跨域关联
- Z-7: 客户端 Connect/Close 包 (会话协议面)
- Z-4: checkSession 在 Prep

## 结论
touch = 桶迁移续期 (closing 拒绝); check = 过期/移动异常; sessionId = serverId(8) + 时间(40) + 计数; 超时钳制 [2×tick, 20×tick]; 本地会话升级面。
源码位置: SessionTrackerImpl.java:98-108,179-194,261-265,335-341; ZooKeeperServer.java:1394,1403
