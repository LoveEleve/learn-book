# Z-5 Session — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 3.4.x | SessionTrackerImpl 骨架: sessionsById + ExpiryQueue 分桶 + 单线程过期循环 (L41-42 宽限期注释风格) |
| 3.5.x | **本地会话** (localSessionEnabled): LearnerSessionTracker + 首次写升级全局 (ZooKeeperServer:1477 面); 会话超时钳制参数化 |
| 3.6.x | **CONTAINER_EPHEMERAL_OWNER 特值跳过** (L104-106) — 容器节点与 sessionId 空间隔离; connThrottle 连接权重限流 (ZooKeeperServer:1474+) |
| 3.9.x | STALE_SESSIONS_EXPIRED/CLOSE_SESSION_PREP_TIME 指标; setLocalSessionFlag 细化 |

## 痕迹证据

- SessionTrackerImpl.java:41-42: 分桶宽限期注释 (3.4 锚)
- SessionTrackerImpl.java:98-108: initializeNextSessionId 位结构 + CONTAINER 跳过 (L104-106, 3.6 锚)
- ZooKeeperServer.java:1394,1403: min/max 超时钳制 (tickTime×2/×20)
- ZooKeeperServer.java:1474-1490: connThrottle (3.6+ 连接限流)
- LearnerSessionTracker.java:68: initializeNextSessionId 复用 (3.5+ 本地会话)

## 推断标注

- "3.4.x 骨架" — 公知版本线 (SessionTracker 3.4 定型) (标注)
- "3.5.x 本地会话" — 本地会话特性 3.5 引入推断 (标注); LearnerSessionTracker 与本地会话配套
- "3.6.x CONTAINER/connThrottle" — 容器节点 3.5.1/connThrottle 年代推断 (标注)
- git shallow (1 commit) — 无考古, 全注释锚
