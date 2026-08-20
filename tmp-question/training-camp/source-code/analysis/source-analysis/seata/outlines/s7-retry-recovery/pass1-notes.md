# S-7 重试故障恢复 — Pass 1 探索笔记

> 域: S-7 重试故障恢复 | 🔴 A 方案 (需 harness) | 2026-08-15
> 源码: server/session/SessionHelper (end* 助手 + DELAY_HANDLE_SESSION) + server/session/SessionStatusValidator + server/coordinator/DefaultCoordinator (S-3 已实证) + DefaultValues (阈值) | Seata 2.5.0

## 调用图

```
非重试失败 (S-1 DefaultCore) → queueToRetryCommit/Rollback (状态即指令: CommitRetrying/RollbackRetrying/TimeoutRollbackRetrying)
  → S-3 定时线程池消费 (retryRollbacking/retryCommitting/asyncCommitting):
    → 筛选状态组 → isRetryTimeout (MAX=-1 永不) → core.doGlobalCommit/Rollback(retrying=true)
    → 成功 → removeBranch → 终态 (Committed/Rollbacked)
    → 重试超时 (isRetryTimeout true) → endCommitFailed/endRollbackFailed (三态迁移) → end()
  → 死阈值 (timeToDeadSession: 70s/10s) → 动态延迟调度 (max(剩余, period))
  → 解锁面: ROLLBACK_FAILED_UNLOCK_ENABLE (默认 false) / ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE
  → 终态清理: end() → isTwoPhaseSuccess ? clean(解锁)+onSuccessEnd : onFailEnd
```

## 基本元素分解

1. **重试状态族**: Commit 系 (Committing/CommitRetrying/AsyncCommitting/Committed/CommitFailed/CommitRetryTimeout) + Rollback 系 + Timeout 系 (14 态)
2. **重试链**: queueToRetry* → 定时消费 → 重试 → 终态
3. **死阈值数学**: RETRY_DEAD_THRESHOLD 70s / END 10s + timeToDeadSession + isRetryTimeout (-1 永不)
4. **终态助手**: endRollbackFailed/endCommitFailed/endCommitted 三态迁移
5. **解锁面**: ROLLBACK_FAILED_UNLOCK_ENABLE (默认 false) + end() 链 + DELAY_HANDLE_SESSION

## 标记问题 (20 问)

1. 重试状态族? (Commit/Rollback/Timeout 三系)
2. queueToRetry* 状态迁移? (S-1 已实证)
3. 定时消费? (S-3 已实证)
4. isRetryTimeout? (-1 永不)
5. 重试超时终态? (CommitRetryTimeout/RollbackRetryTimeout)
6. endRollbackFailed 三态? (RetryTimeout/TimeoutRollbackFailed/RollbackFailed)
7. endCommitFailed? (类似)
8. endCommitted? (DELAY_HANDLE_SESSION 分支)
9. end() 链? (isTwoPhaseSuccess ? clean : onFailEnd)
10. 解锁条件? (ROLLBACK_FAILED_UNLOCK_ENABLE 默认 false)
11. DELAY_HANDLE_SESSION? (DB/REDIS 模式延迟)
12. SessionStatusValidator? (isTimeoutRollbacking 等)
13. 死阈值? (70s/10s)
14. timeToDeadSession? (S-1 已实证)
15. 动态延迟? (S-3 已实证)
16. 终态 Metrics? (postSessionDoneEvent)
17. 手动处理面? ("need to be handled it manually")
18. 对照 RocketMQ? (延迟重试)
19. 重试幂等? (retrying 参数语义)
20. 解锁时机? (end 时 clean)

## 时空溯源 (代码内注释锚)

- SessionHelper:76 DELAY_HANDLE_SESSION (存储模式驱动)
- SessionHelper:152 "TODO: If the globalSession status in the database is Committed, don't set status again"
- SessionHelper:257-260 "need to be handled it manually" — 终态人工介入
- DefaultValues:385-390 死阈值 (70s/10s)
- S-1 交叉: timeToDeadSession/clean=解锁

## 大域拆分判断

S-7 = 重试链汇总面 (状态族 + 调度 + 终态 + 解锁); 单篇 🔴 A (8 闭环 q1-q4 + harness 4 面)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划/规划) | 验证 | 结论 |
|:--|:--|:--|
| "6个线程池retry+RETRY_DEAD_THRESHOLD+ROLLBACK_FAILED_UNLOCK_ENABLE" | 6 池 S-3 实证; 死阈值 70s/10s; **ROLLBACK_FAILED_UNLOCK_ENABLE 默认 false** (DefaultValues:532) | **接受** ✅ |
| "重试状态族" | Commit/Rollback/Timeout 三系 14 态 (GlobalStatus 21 态中) | **补充** ✅ |
| 终态迁移 | endRollbackFailed 三态分支 (isRetryTimeout/TimeoutRollbacking/else) | **接受** ✅ |
| DELAY_HANDLE_SESSION | DB/REDIS 模式延迟 (SessionHelper:76) — 执行计划未提 | **补充** ✅ |
