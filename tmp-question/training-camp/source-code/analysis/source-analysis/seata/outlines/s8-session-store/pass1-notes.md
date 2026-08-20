# S-8 Session 存储 — Pass 1 探索笔记

> 域: S-8 Session 存储 | 🔴 A 方案 (需 harness) | 2026-08-15
> 源码: server/session/ (SessionHolder 462 + AbstractSessionManager 211 + SessionManager + GlobalSession) + server/storage/ (db/file/raft/redis 四族) + core/store/ (GlobalTransactionDO/LockDO + db/sql 13 方言) | Seata 2.5.0

## 调用图

```
SessionHolder.init(sessionMode) (L91-154):
  DB → EnhancedServiceLoader.load(SessionManager, "db") + reload
  FILE → load(SessionManager, "file", root.data) ×2 + reload
  RAFT → load(SessionManager, "raft", root.data) (group 映射)
  REDIS → load(SessionManager, "redis")

生命周期 (AbstractSessionManager):
  onBegin → addGlobalSession → writeSession(GLOBAL_ADD) (L127-129,73-78)
  onStatusChange → updateGlobalSessionStatus (Rollbacking/Timeout → 分支锁标记, L85-87)
  onSuccessEnd → removeGlobalSession (L159-161)
  onFailEnd → rollbackFailedUnlockEnable ? clean(解锁) (L163-171)
  onClose → setActive(false) (L154-156) — S-1 isEndStatus 根源

lockAndExecute 三模式:
  FILE: globalSession.lock() (GlobalSessionLock ReentrantLock tryLock 2s) → call → unlock (FileSessionManager:184-193)
  DB: 直接 call (DataBaseSessionManager:160-163) — 多节点共享存储本地锁无意义
  RAFT/分布式: distributedLockAndExecute (DistributedLocker 族, S-3 已实证)
```

## 基本元素分解

1. **SessionMode 4 值**: FILE/DB/REDIS/RAFT (SessionMode:19-35)
2. **SessionHolder**: init 按模式 SPI 加载 + ROOT_SESSION_MANAGER_NAME="root.data" + reload
3. **SessionManager 族**: DataBaseSessionManager/FileSessionManager/RaftSessionManager/RedisSessionManager
4. **存储实现**: TransactionStoreManager (DataBase/File/RedisLua) + 13 方言 SQL (LogStoreSqls/LockStoreSql) + Lua 原子脚本
5. **锁面**: GlobalSessionLock (ReentrantLock 2s) + DistributedLocker 族 (DataBaseDistributedLocker/RedisLuaLocker/RaftDistributedLocker/FileLocker)

## 标记问题 (20 问)

1. SessionMode 4 值? (FILE/DB/REDIS/RAFT)
2. SessionHolder.init? (SPI 按模式加载)
3. ROOT_SESSION_MANAGER_NAME? ("root.data")
4. SessionManager 族? (4 实现)
5. writeSession 6 操作? (GLOBAL_ADD/UPDATE/REMOVE + BRANCH_*)
6. onClose? (setActive(false) — S-1 isEndStatus 根源)
7. onSuccessEnd? (removeGlobalSession)
8. onFailEnd? (rollbackFailedUnlockEnable ? clean)
9. Rollbacking 状态? (分支锁标记 LockStatus.Rollbacking)
10. lockAndExecute? (三模式对比)
11. GlobalSessionLock? (ReentrantLock tryLock 2s)
12. DB 模式锁? (无本地锁 — 存储层条件更新)
13. 13 方言 SQL? (LogStoreSqls/LockStoreSql)
14. GlobalTransactionDO? (11 字段)
15. REDIS Lua? (Lua 脚本原子性)
16. 分布式锁? (DistributedLocker 族 4 实现)
17. reload? (重启恢复)
18. FailedWriteSession? (写失败异常)
19. 对照 ZK? (会话存储 vs 4.3 会话面)
20. File 双 manager? (root.data ×2)

## 时空溯源 (代码内注释锚)

- SessionHolder:71 ROOT_SESSION_MANAGER_NAME = "root.data" (0.9 锚)
- AbstractSessionManager:85-87 Rollbacking → LockStatus.Rollbacking (锁状态关联)
- AbstractSessionManager:154-156 onClose → setActive(false)
- DataBaseSessionManager:160-163 lockAndExecute 直通
- FileSessionManager:184-193 lockAndExecute → globalSession.lock()

## 大域拆分判断

S-8 = 会话存储面 (模式路由 + 管理器族 + 存储实现 + 锁面); 单篇 🔴 A (8 闭环 q1-q4 + harness 4 面)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划/规划) | 验证 | 结论 |
|:--|:--|:--|
| "SessionMode(DB/FILE/REDIS/RAFT)" | 4 值全实证 (SessionMode:19-35) | **接受** ✅ |
| "lockAndExecute 并发控制" | **三模式差异实证**: FILE ReentrantLock 2s / DB 直通 / RAFT 分布式锁 | **接受+补充** ✅ |
| "存储 DTO" | GlobalTransactionDO 11 字段 (core/store) + LockDO | **接受** ✅ |
| 数字: 方言 | LogStoreSqls 13 + LockStoreSql 13 (mysql/oracle/pg/dm/...) | **补充** ✅ |
