# 闭环笔记 q2: 状态组与重试语义 — 5 组筛选 + 重试边界

## 假设
调度器按状态组筛选会话; 重试有超时/阈值双边界。

## 验证过程
- **5 个状态组数组** (DefaultCoordinator:200-210): **retryRollbackingStatuses 3** (TimeoutRollbacking/TimeoutRollbackRetrying/RollbackRetrying) / **retryCommittingStatuses 1** (CommitRetrying) / **rollbackingStatuses 1** (Rollbacking) / **committingStatuses 1** (Committing) / **endStatuses 4** (Rollbacked/TimeoutRollbacked/Committed/Finished) — 执行计划 "5种状态组" = 5 组**筛选条件** (非 21 态分组)
- **handleRetryRollbacking** (L451-482): 筛选 3 态 → **isRetryTimeout(MAX_ROLLBACK_RETRY_TIMEOUT) → unlock (ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE/ROLLBACK_FAILED_UNLOCK_ENABLE 任一开) + endRollbackFailed** / else core.doGlobalRollback(retrying=true)
- **handleRetryCommitting** (L487-519): CommitRetrying → isRetryTimeout(MAX_COMMIT_RETRY_TIMEOUT) → endCommitFailed / Committed+无分支 → endCommitted / doGlobalCommit(true)
- **handleAsyncCommitting** (L524-543): AsyncCommitting → doGlobalCommit(true) — **异步提交消费面**
- **isRetryTimeout 数学** (L572-574): `timeout >= ALWAYS_RETRY_BOUNDARY(0) && now - beginTime > timeout` — **DEFAULT_MAX_COMMIT/ROLLBACK_RETRY_TIMEOUT = -1L** (DefaultValues:520-527) → **-1 >= 0 false → 永不超时 → 默认永远重试** (靠 RETRY_DEAD_THRESHOLD 70s + timeToDeadSession 终止, S-1 交叉)
- **timeoutCheck** (L406-446): Begin 态全量扫描 → lockAndExecute → **Begin && isTimeout → close + changeGlobalStatus(TimeoutRollbacking)** — 服务端超时入口 (S-1 双时钟的服务端侧)

## 代码类型
Implementation (重试调度)

## 跨域关联
- S-1: queueToRetry* 状态迁移 (状态即指令) — 本域消费
- S-7: 重试面核心 (本域即 S-7 机制面)

## 结论
状态组 = 5 组筛选条件; 重试默认永不超时 (MAX=-1) 靠 dead threshold 终止; 超时扫描独立线程池。
源码位置: DefaultCoordinator.java:200-210,406-574; DefaultValues.java:520-527
