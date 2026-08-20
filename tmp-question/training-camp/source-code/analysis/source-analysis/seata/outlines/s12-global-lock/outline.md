# S-12 全局锁体系 — 行锁存储与 Locker 族

> 前置: [[S-2-undo_log]] (lockKey 采集) + [[S-4-DataSource代理]] (checkLock 消费) + [[S-8-Session存储]] (状态联动) | 对照: ZK 分布式锁 (阶段4.3 Z-8)
> 🔴 A | 8 KP | [模式: 检查-插入 + 模式 SPI + 状态标记]
> Pass 2 闭环: q1(锁管理器) q2(Locker 族) q3(LockStore) q4(锁状态)

**读者处境**: 全局锁存在哪? lockQuery 和 acquireLock 什么区别? 这篇拆 AbstractLockManager + Locker 族 (7 实现) + LockStore 检查-插入 + LockStatus。

### 1. 锁管理器 — AbstractLockManager + 工厂

场景: 锁操作怎么统一?
源码路径:
- **LockerManagerFactory** (L32-52): **EnhancedServiceLoader.load(LockManager, lockMode)** — LockMode 4 值 (FILE/DB/REDIS/RAFT)
- **AbstractLockManager** (L38-194): acquireLock / releaseLock / **isLockable (lockQuery)** / cleanAllLocks / **updateLockStatus** — 统一入口; ⚠ **分支注册锁链**: branchRegister → ATCore.branchSessionLock (L57-95) → branchSession.lock → acquireLock → **LockKeyConflict** (Phase1 完成前锁齐); **AbstractCore 空实现** — 非 AT 无行锁
- **collectRowLocks** (L122-165): **lockKey 格式 "table:pk1,pk2;t2:pk3"** 解析 → RowLock 列表 (xid/tid/branchId/table/pk/resourceId)
- **getLocker 抽象** (L119): 子类实现 (DataBase/File/RedisLockManager)
关键设计 (q1): **统一入口 + 模式 SPI + lockKey 格式解析**。[模式: 锁管理器]

### 2. Locker 族 — 行锁 4 + 分布式 3

场景: 锁实现有哪些?
源码路径:
- **AbstractLocker 子类 4 (行锁)**: DataBaseLocker / FileLocker / RedisLocker / **RedisLuaLocker (Lua 原子)**
- **DistributedLocker 3 (分布式)**: DataBaseDistributedLocker / RedisDistributedLocker / RaftDistributedLocker
- ⚠ **执行计划 "6种Locker实现" 修正**: 实证 **7 个实现** (4 行锁 + 3 分布式)
- **RedisLockerFactory** (L28): Lua/Java 锁选择
关键设计 (q2): **行锁 + 分布式锁双族** (7 实现)。[模式: Locker 族]

### 3. LockStore — 检查-插入 + 只查不锁

场景: 行锁怎么存?
源码路径:
- **acquireLock 两段** (LockStoreDataBaseDAO:109-160+): **rowKey 去重** → autoCommit false → **checkLock (SELECT row_key, xid WHERE row_key IN)** → **dbXID != currentXID → 冲突** (failFast: LockKeyConflictFailFast 面) → insertLockBatch; **skipCheckLock 旁路** (跳过检查直插); ⚠ **客户端经 applicationData 传 AUTO_COMMIT/SKIP_CHECK_LOCK** (ATCore:60-75)
- **lockQuery 语义**: **只查不锁** (isLockable — SELECT 检查) — RM 侧 checkLock 同源 (S-4)
- **13 方言 SQL**: LockStoreSqlFactory (mysql/oracle/pg/...)
- **LockDO 5 列**: rowKey (表+主键复合) + xid/transactionId/branchId/status (core/store/LockDO — lock_table 建表依据)
- 冲突日志: "holding by xid {} branchId {}"
关键设计 (q3): **检查-插入两段 + 只查不锁 + IN 检查**。[模式: 锁存储]

### 4. 锁状态与释放 — LockStatus + 释放链

场景: 锁状态怎么流转?
源码路径:
- **LockStatus 2 值**: **Locked(0) / Rollbacking(1)** — S-8 状态联动 (回滚中标记)
- **updateLockStatus** (L194): xid 级标记 — 并发读感知
- **释放两级** (DataBaseLockManager:58-73): 分支级 + 全局级 — S-1 clean 链
- **释放时机** (S-7/S-8): 终态 end() → isTwoPhaseSuccess ? clean : onFailEnd; **默认不解锁 (人工)** + ROLLBACK_FAILED_UNLOCK_ENABLE

## 代码类型
Architecture (全局锁体系)

## 负面空间 — 全局锁刻意不做的事

- **不做读锁/写锁区分**: 单一行锁 (无 RW 锁 — 对照 ZK ReadWriteLock)
- **不做锁粒度协商**: 行锁固定 (主键级) — 无谓词锁
- **不做锁超时自动释放**: 靠事务终态 — 崩溃残留靠重试/人工
- **不做公平队列**: 冲突即失败 (RM 侧重试) — 无排队
- **不做锁监控**: 无锁等待统计 (Metrics 部分)
- **不做跨表原子锁**: 多表 lockKey 拆分 — 无跨表事务锁语义

→ 引出: Phase2 分支收束 → [[S-13-Phase2分支通知]]
