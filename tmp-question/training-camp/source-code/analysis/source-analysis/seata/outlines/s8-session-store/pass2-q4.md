# 闭环笔记 q4: lockAndExecute 三模式 — 并发控制对比

## 假设
会话级并发控制按存储模式不同: 本地锁/直通/分布式锁。

## 验证过程
- **FILE 模式** (FileSessionManager:184-193): **globalSession.lock() → call → finally unlock()** — **GlobalSessionLock: ReentrantLock + tryLock(2s)** (GlobalSession:831-850) — 超时抛 **FailedLockGlobalTransaction** — 单机 JVM 锁
- **DB 模式** (DataBaseSessionManager:160-163): **直接 call** — 多节点共享存储, 本地锁无意义 — 并发靠 **存储层条件更新 + 全局锁** (S-12 交叉)
- **RAFT 模式**: RaftSessionManager + **distributedLockAndExecute** (S-3 已实证 — DistributedLocker 族防多节点重复执行)
- **DistributedLocker 族 4 实现** (storage/*/lock/): **DataBaseDistributedLocker / RedisDistributedLocker (Lua) / RaftDistributedLocker / FileLocker** — DistributedLockerFactory 按模式 (S-3 已实证)
- **GlobalSession 锁字段** (L117-121): globalSessionLock + resourceLock — 会话/资源双锁

## 代码类型
Architecture (并发控制)

## 跨域关联
- S-3: distributedLockAndExecute (RAFT/分布式)
- S-12: 全局锁 (DB 模式并发核心)
- S-1: lockAndExecute 消费 (状态迁移原子性)

## 结论
并发控制 = 模式驱动: FILE 本地锁 (2s 超时) / DB 直通+存储条件更新 / RAFT 分布式锁。
源码位置: FileSessionManager.java:184-193; DataBaseSessionManager.java:160-163; GlobalSession.java:831-850
