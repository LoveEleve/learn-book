# 闭环笔记 q1: SessionMode 与加载路由 — 4 模式 SPI

## 假设
会话存储按 SessionMode 路由到不同实现; 加载经 EnhancedServiceLoader SPI。

## 验证过程
- **SessionMode 4 值** (SessionMode:19-35): **FILE("file") / DB("db") / REDIS("redis") / RAFT("raft")**
- **SessionHolder.init** (L91-154): DB → load(SessionManager, "db") + **reload**; FILE → load("file", root.data) ×2 + reload; RAFT → load("raft", root.data) + group 映射; REDIS → load("redis")
- **ROOT_SESSION_MANAGER_NAME = "root.data"** (L71) — 根会话管理器命名
- **SessionManager 族** (storage/4 模式): **DataBaseSessionManager / FileSessionManager / RaftSessionManager / RedisSessionManager** — 各自配 TransactionStoreManager + Locker
- **reload 语义**: 启动时从存储恢复会话 (S-7 重启续跑面)
- **VGroupMappingStoreManager**: 事务组映射存储 (DB/FILE/RAFT/REDIS 各实现)

## 代码类型
Architecture (模式路由)

## 跨域关联
- S-7: 重启恢复 (reload)
- S-3: distributedLockAndExecute (RAFT 模式)
- S-12: Locker/LockManager (存储族绑定)

## 结论
模式路由 = SessionMode 4 值 → SPI 加载 SessionManager 族 + reload 恢复。
源码位置: SessionMode.java:19-35; SessionHolder.java:71-154
