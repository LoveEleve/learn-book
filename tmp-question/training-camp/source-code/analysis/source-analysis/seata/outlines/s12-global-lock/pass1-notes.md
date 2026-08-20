# S-12 全局锁体系 — Pass 1 探索笔记

> 域: S-12 全局锁 | 🔴 A 方案 (需 harness) | 2026-08-15
> 源码: server/lock/ (AbstractLockManager + LockerManagerFactory + LockManager) + core/store/lock (LockStore/LockDO) + server/storage/*/lock/ (4 模式) + core/model/LockStatus | Seata 2.5.0

## 调用图

```
RM 侧 (S-4/S-2 交叉): lockKey 采集 → 提交前 checkLock/lockQuery → branchRegister (含 lockKeys)
TC 侧 (本域):
  LockManager.acquireLock (AbstractLockManager:46-67):
    collectRowLocks (lockKey 解析: "table:pk1,pk2;t2:pk3") → getLocker().acquireLock
  lockQuery (isLockable): 只查不锁 (SELECT row_key, xid WHERE row_key IN ...)
  释放: releaseLock (提交/回滚终态) / updateLockStatus (Rollbacking 标记, S-8 联动)
  LockerManagerFactory: EnhancedServiceLoader.load(LockManager, lockMode) (FILE/DB/REDIS/RAFT)
Locker 族:
  AbstractLocker 子类 (行锁): DataBaseLocker/FileLocker/RedisLocker/RedisLuaLocker
  DistributedLocker (分布式): DataBaseDistributedLocker/RedisDistributedLocker/RaftDistributedLocker
LockStatus: Locked(0)/Rollbacking(1)
```

## 基本元素分解

1. **锁管理器**: AbstractLockManager (collectRowLocks + 委托 Locker) + LockerManagerFactory (SPI 按模式)
2. **Locker 族**: AbstractLocker 4 子类 (行锁) + DistributedLocker 3 实现 (分布式)
3. **LockStore**: LockStoreDataBaseDAO (检查-插入 + rowKey 去重) + 13 方言 SQL
4. **lockKey 格式**: "tableName:pk1,pk2;table2:pk3" 解析
5. **LockStatus**: Locked(0)/Rollbacking(1)

## 标记问题 (20 问)

1. LockMode? (FILE/DB/REDIS/RAFT 4 值)
2. LockerManagerFactory? (SPI 按模式)
3. AbstractLockManager? (collectRowLocks + 委托)
4. lockKey 格式? (表:主键,分号表分隔)
5. RowLock? (xid/tid/branchId/table/pk/resourceId)
6. Locker 族? (行锁 4 + 分布式 3)
7. acquireLock? (检查-插入)
8. lockQuery? (只查不锁)
9. releaseLock? (终态释放)
10. updateLockStatus? (Rollbacking 标记, S-8)
11. LockStoreDataBaseDAO? (rowKey 去重 + IN 检查)
12. 13 方言? (LockStoreSql)
13. LockStatus? (Locked/Rollbacking)
14. DistributedLocker? (3 实现)
15. failFast? (冲突处理)
16. 对照 ZK? (4.3 Z-8 分布式锁)
17. LockDO? (rowKey/xid/...)
18. 释放语义? (分支/全局两级)
19. 锁冲突异常? (LockKeyConflict)
20. skipCheckLock? (跳过检查直接插)

## 时空溯源 (代码内注释锚)

- LockerManagerFactory: "use lock store mode: {}" — 模式加载
- LockStoreDataBaseDAO: "Global lock on [{}:{}] is holding by xid {} branchId {}" — 冲突日志
- AbstractLockManager: collectRowLocks 解析注释
- LockStatus: Locked(0)/Rollbacking(1)

## 大域拆分判断

S-12 = 全局锁体系 (管理器 + Locker 族 + 存储 + 状态); 单篇 🔴 A (8 闭环 q1-q4 + harness 4 面)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划/规划) | 验证 | 结论 |
|:--|:--|:--|
| "AbstractLockManager+lock_table DDL+RowLock/Locker—6种Locker实现" | AbstractLockManager + lock_table + RowLock 实证; **Locker 族 7 个** (行锁 4 + 分布式 3) — **"6种" 需精确化** | **接受+修正** ⚠ |
| "lockQuery" | 只查不锁 (SELECT row_key, xid IN) | **接受** ✅ |
| LockStatus | Locked(0)/Rollbacking(1) | **补充** ✅ |
| 对照 ZK (4.3 Z-8) | WriteLock 前驱链 vs 本域 lock_table 行锁 — 两种分布式锁思路 | **接受** ✅ |
