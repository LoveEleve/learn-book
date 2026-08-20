# 闭环笔记 q4: 执行链与锁重试 — doExecute 双路径 + LockRetryPolicy

## 假设
DML 执行按 autoCommit 双路径; 锁冲突重试由 LockRetryPolicy 统一处理。

## 验证过程
- **doExecute** (AbstractDMLBaseExecutor:81-102): **autoCommit=true → executeAutoCommitTrue** / autoCommit=false → beforeImage (SELECT FOR UPDATE, S-2) → 业务执行 → afterImage → **prepareUndoLog** → context.appendUndoLog + appendLockKey
- **executeAutoCommitTrue** (L146-158): **LockRetryPolicy.execute 包裹** (L150) — 单语句自动提交也要过锁重试; 失败 → LockRetryPolicy 处理
- **LockRetryPolicy** (ConnectionProxy:338-392): **LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT (默认 true)** — true && autoCommitChanged → 直接执行 (L355-357, 重试在 executeAutoCommitTrue 层); else **doRetryOnLockConflict**: LockRetryController 循环重试 (L365-383); **FailFast 降级**: autoCommitChanged && LockKeyConflictFailFast → 降级 LockKeyConflict (L372-376, "the local lock is released" 注释)
- **onException 钩子** (L391): 子类扩展 (回滚策略面)
- **锁重试周期**: LockRetryController (lockRetryInterval × retryTimes, S-1 GlobalLockConfig 交叉)

## 代码类型
Implementation (执行链)

## 跨域关联
- S-10: ExecuteTemplate 路由 (本域是执行入口)
- S-12: LockRetryController 数学 (锁冲突重试)
- S-2: 镜像采集 (before/after)

## 结论
执行 = autoCommit 双路径; 锁冲突重试集中 LockRetryPolicy (branchRollbackOnConflict 双面语义)。
源码位置: AbstractDMLBaseExecutor.java:81-158; ConnectionProxy.java:338-392
