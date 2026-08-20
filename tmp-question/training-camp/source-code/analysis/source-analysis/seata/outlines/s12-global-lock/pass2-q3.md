# 闭环笔记 q3: LockStore — 检查-插入 + 只查不锁

## 假设
行锁存储 = 检查-插入两段; lockQuery 只查不锁。

## 验证过程
- **acquireLock 两段** (LockStoreDataBaseDAO:109-160+): **rowKey 去重** (distinctByKey) → autoCommit false → **checkLock SQL (getCheckLockableSql: SELECT row_key, xid WHERE row_key IN (...))** → 已存在且 **dbXID != currentXID → 冲突** (failFast 面) → 全部通过 → **insertLockBatch (插入锁行)**
- **lockQuery 语义** (isLockable): **只查不锁** — SELECT 检查可锁性, 不写入 — RM 侧 lockQuery (S-4) 与提交前 checkLock 同源
- **13 方言 SQL**: LockStoreSqlFactory → 13 方言 (mysql/oracle/pg/...) — 与 LogStoreSqls 同族
- **LockDO**: rowKey (表+主键复合键) + xid + transactionId + branchId + status — 锁行结构
- **冲突日志**: "Global lock on [table:pk] is holding by xid {} branchId {}" (L134-137)

## 代码类型
Implementation (锁存储)

## 跨域关联
- S-4: checkLock/lockQuery 消费 (提交前)
- S-2: lockKey 采集 (DELETE ? before : after)
- S-10: 路由守卫 requireGlobalLock

## 结论
存储 = 检查-插入两段 + IN 检查 + 只查不锁的 lockQuery + 13 方言。
源码位置: LockStoreDataBaseDAO.java:109-160+; core/store/db/sql/lock/ (13 方言)
