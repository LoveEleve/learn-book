# 闭环笔记 q1: 重试状态族 — 三系 14 态完整性

## 假设
重试相关状态覆盖 Commit/Rollback/Timeout 三系; 迁移有确定方向。

## 验证过程
- **Commit 系** (GlobalStatus:41-137): Committing(2) / **CommitRetrying(3)** / **AsyncCommitting(8)** / Committed(9) / CommitFailed(10) / **CommitRetryTimeout(16)** — 6 态
- **Rollback 系**: Rollbacking(4) / **RollbackRetrying(5)** / Rollbacked(11) / RollbackFailed(12) / **RollbackRetryTimeout(17)** — 5 态
- **Timeout 系**: **TimeoutRollbacking(6) / TimeoutRollbackRetrying(7)** / TimeoutRollbacked(13) / TimeoutRollbackFailed(14) — 4 态
- **重试入口迁移** (S-1 实证, GlobalSession:871-893): asyncCommit → AsyncCommitting; queueToRetryCommit → **CommitRetrying** (Stop 守卫); queueToRetryRollback → **TimeoutRollbacking 时 TimeoutRollbackRetrying 否则 RollbackRetrying** (Stop 守卫)
- **重试终态迁移**: isRetryTimeout → **CommitRetryTimeout/RollbackRetryTimeout** (16/17); Timeout 系失败 → TimeoutRollbackFailed(14)
- **SessionStatusValidator** (L35-50): isTimeoutRollbacking = TimeoutRollbacking||TimeoutRollbackRetrying; isRollbackGlobalStatus = Rollbacking||RollbackRetrying||... — 校验助手族

## 代码类型
Data (状态族)

## 跨域关联
- S-1: queueToRetry* 入口 (状态即指令)
- S-3: 定时消费 (状态组筛选)
- S-2: Unretriable 分支状态 (dirty → 终态)

## 结论
重试状态族 = 三系 14 态; 入口迁移 (Retrying 族) + 终态迁移 (RetryTimeout 族) 有确定方向。
源码位置: GlobalStatus.java:41-137; GlobalSession.java:871-893; SessionStatusValidator.java:35-50
