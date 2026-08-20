# 闭环笔记 q2: 提交拦截 — doCommit 三分支 + register 守卫

## 假设
本地提交是 Seata 的唯一拦截点: 全局事务中提交 = 分支注册 + flush undo + 本地提交 + report。

## 验证过程
- **commit 入口** (ConnectionProxy:184-199): **LockRetryPolicy.execute(doCommit)** 包裹 (锁冲突重试面) → 失败且非 autoCommitChanged → 补 rollback
- **doCommit 三分支** (L227-235): **inGlobalTransaction → processGlobalTransactionCommit** / **isGlobalLockRequire → processLocalCommitWithGlobalLocks** (仅全局锁无全局事务: checkLock → commit) / else targetConnection.commit
- **processGlobalTransactionCommit** (L247-265): **register()** → **flushUndoLogs (S-2)** → targetConnection.commit → 失败: report(false) + 抛 / 成功: **IS_REPORT_SUCCESS_ENABLE (默认 false) 才 report(true)** → context.reset
- **register 守卫** (L267-281): **!hasUndoLog || !hasLockKey → return (不注册分支)** — 只读/无写操作事务不产生分支 → branchRegister(AT, resourceId, null, xid, applicationData, buildLockKeys) → setBranchId
- **report** (L311-336): REPORT_RETRY_COUNT=5 (DefaultValues:56); PhaseOne_Done/PhaseOne_Failed; 全失败 → SQLException
- **rollback** (L284-290): targetConnection.rollback → **分支已注册 → report(false)** → reset
- **setAutoCommit 拦截** (L303-309): 全局事务中 false→true → **先 doCommit** (JDBC 规范注释 L305); changeAutoCommit 标记 autoCommitChanged (L297-300)

## 代码类型
Implementation (提交拦截)

## 跨域关联
- S-2: flushUndoLogs (undo 写面)
- S-1: branchRegister/report → TC (Phase1 完成通知)
- S-12: checkLock/lockQuery (全局锁查询面)

## 结论
提交 = 三分支拦截; 全局事务提交 = register (有写才注册) → flush undo → 本地提交 → report (默认不报成功)。
源码位置: ConnectionProxy.java:184-336; DefaultValues.java:54-60
