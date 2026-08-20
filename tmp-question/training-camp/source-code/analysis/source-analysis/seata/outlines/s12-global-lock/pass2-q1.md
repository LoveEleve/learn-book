# 闭环笔记 q1: 锁管理器 — AbstractLockManager + 工厂

## 假设
锁操作统一入口 AbstractLockManager; 按存储模式加载具体实现。

## 验证过程
- **LockerManagerFactory** (L32-52): 单例 + **EnhancedServiceLoader.load(LockManager, lockMode)** — LockMode 4 值 (FILE/DB/REDIS/RAFT); StoreConfig.getLockMode() 默认
- **AbstractLockManager** (L38-194): **acquireLock (collectRowLocks → getLocker().acquireLock)** (L46-67) / **releaseLock** (L71-77, 分支级) / **isLockable (lockQuery)** (L85-92) / **cleanAllLocks** (L100-101) / **updateLockStatus (xid, LockStatus)** (L194) — S-8 状态联动
- **getLocker 抽象** (L119): 子类实现 (DataBaseLockManager:66 → DataBaseLocker; FileLockManager:40 → FileLocker; RedisLockManager:39 → RedisLockerFactory)
- **collectRowLocks** (L122-165): **lockKey 格式 "tableName:pk1,pk2;table2:pk3"** — 分号表分隔 / 冒号表名:主键 / 逗号多主键 → RowLock 列表 (xid/transactionId/branchId/tableName/pk/resourceId)

## 代码类型
Architecture (锁管理器)

## 跨域关联
- S-8: updateLockStatus (Rollbacking 联动)
- S-1: clean=releaseGlobalSessionLock (终态释放)
- S-2: lockKey 采集源

## 结论
管理器 = 统一入口 + 模式 SPI 加载 + lockKey 解析 (表:主键格式) + 委托具体 Locker。
源码位置: AbstractLockManager.java:38-194; LockerManagerFactory.java:32-52
