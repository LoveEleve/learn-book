# 闭环笔记 q2: Locker 族 — 行锁 4 + 分布式 3

## 假设
Locker 分两类: 行锁 (lock_table) + 分布式锁; 执行计划 "6种" 需精确。

## 验证过程
- **AbstractLocker 子类 4 个 (行锁)**: **DataBaseLocker / FileLocker / RedisLocker / RedisLuaLocker** (RedisLuaLocker extends RedisLocker — Lua 原子版) — acquireLock/releaseLock/isLockable 行级
- **DistributedLocker 实现 3 个 (分布式)**: **DataBaseDistributedLocker / RedisDistributedLocker / RaftDistributedLocker** — acquireLock/releaseLock (分布式锁表/键)
- **执行计划 "6种Locker实现" 修正**: 实证 **7 个** (4 行锁 + 3 分布式); 若按"锁实现族"计 RedisLocker/Lua 同族为 6 — 表述精确化
- **LockerManager**: DataBaseLockManager/FileLockManager/RedisLockManager getLocker 返回族内实现
- **RedisLockerFactory** (L28): Redis 锁工厂 — Lua/Java 选择

## 代码类型
Implementation (Locker 族)

## 跨域关联
- S-8: 分布式锁族 (S-3 distributedLockAndExecute)
- Z-8 (4.3): 对照 ZK WriteLock (顺序节点前驱链 vs lock_table)

## 结论
Locker 族 = 行锁 4 + 分布式 3 = **7 个实现**; 执行计划 "6种" 表述精确化。
源码位置: storage/{db,file,redis,raft}/lock/; RedisLockerFactory.java:28
