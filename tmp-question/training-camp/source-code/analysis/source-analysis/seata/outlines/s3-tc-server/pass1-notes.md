# S-3 TC Server — Pass 1 探索笔记

> 域: S-3 TC Server | 🔴 A 方案 (需 harness) | 2026-08-15
> 源码: server/coordinator/DefaultCoordinator (929) + AbstractTCInboundHandler + server/limit (LimitRequestDecorator) + server/session/SessionHolder + DefaultValues | Seata 2.5.0

## 调用图

```
TM/RM (Netty) → ServerOnRequestProcessor → transactionMessageHandler.onRequest (DefaultCoordinator:797)
  → LimitRequestDecorator (RateLimiterHandler 限流) → doGlobalBegin/doCommit/doRollback/doStatus/doReport/doBranchRegister/doBranchReport/doLockCheck
  → core.begin/commit/rollback/... (S-1 DefaultCore)

定时面 (init:758-794):
  6 ScheduledThreadPool (各 1 线程):
    retryRollbacking.scheduleAtFixedRate → distributedLockAndExecute(RETRY_ROLLBACKING, handleRetryRollbacking)
    retryCommitting → handleRetryCommitting | asyncCommitting → handleAsyncCommitting
    timeoutCheck → timeoutCheck (Begin 态超时 → TimeoutRollbacking)
    undoLogDelete → undoLogDelete (RM 广播 UndoLogDeleteRequest, saveDays)
    syncProcessing: rollbacking/committing/end 三个动态延迟自调度 (delay = max(timeToDeadSession, period))
  分支删除: branchRemoveExecutor (cores×2, queue 5000, CallerRuns) — 仅 enableBranchAsyncRemove && mode!=FILE
```

## 基本元素分解

1. **消息分发**: onRequest → LimitRequestDecorator → doXxx 8 方法 (begin/commit/rollback/status/report/branchRegister/branchReport/lockCheck)
2. **定时调度**: 6 ScheduledThreadPool (5 fixedRate + syncProcessing 动态延迟) — 全部分布式锁包裹
3. **状态组**: 5 个状态数组 (retryRollbacking 3/retryCommitting 1/rollbacking 1/committing 1/end 4)
4. **重试语义**: isRetryTimeout (MAX_COMMIT/ROLLBACK_RETRY_TIMEOUT 默认 -1 → 永不超时) + RETRY_DEAD_THRESHOLD 70s
5. **会话管理**: SessionHolder.findGlobalSessions + lockAndExecute + distributedLockAndExecute (RAF 防重)

## 标记问题 (20 问)

1. 6 个 ScheduledThreadPool? (retryRollbacking/retryCommitting/asyncCommitting/timeoutCheck/undoLogDelete/syncProcessing — 全 1 线程)
2. 5 个状态组? (数组元素数 3/1/1/1/4)
3. onRequest 分发? (LimitRequestDecorator → doXxx)
4. 限流? (RateLimiterHandler)
5. timeoutCheck? (Begin+isTimeout → close → TimeoutRollbacking)
6. handleRetryRollbacking? (isRetryTimeout → unlock+endRollbackFailed / doGlobalRollback)
7. handleRetryCommitting? (isRetryTimeout → endCommitFailed / doGlobalCommit)
8. handleAsyncCommitting? (AsyncCommitting → doGlobalCommit(true))
9. undoLogDelete? (RM 渠道广播 saveDays)
10. 动态延迟? (rollbacking/committing/end 三个自调度: delay = max(timeToDeadSession, period))
11. isRetryTimeout 数学? (timeout >= 0 && now-beginTime > timeout; -1 → 永不)
12. MAX_RETRY_TIMEOUT 默认? (-1L — 永远重试, 靠 70s dead)
13. distributedLockAndExecute? (SessionHolder:429 — RAF 防多节点重)
14. branchRemoveExecutor? (cores×2/queue 5000/CallerRuns; 仅 enable && !=FILE)
15. destroy? (三步: 定时 → Netty → SessionHolder)
16. BranchRemoveTask? (异步删分支 parallelStream)
17. getInstance? (RAFT → RaftCoordinator 多态)
18. MDC? (doXxx 全部 MDC.put xid)
19. retry period 默认? (1000ms; END_STATUS 30s; UNDO_LOG_DELETE 24h)
20. RAFT 多节点? (RaftCoordinator + distributedLock 面)

## 时空溯源 (代码内注释锚)

- DefaultCoordinator:153 ALWAYS_RETRY_BOUNDARY = 0 — 重试边界注释
- DefaultCoordinator:253-256 RAFT 模式 → RaftCoordinator (多态)
- DefaultCoordinator:469 "The function of this 'return' is 'continue'" — forEach 语义注释
- DefaultCoordinator:817 "1. first shutdown timed task" destroy 三步注释
- S-1 交叉: RETRY_DEAD_THRESHOLD 70s (DefaultValues:385)

## 大域拆分判断

S-3 = TC 调度核心 (消息分发 + 定时重试 + 状态组); 单篇 🔴 A (8 闭环 q1-q4 + harness 4 面)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划/规划) | 验证 | 结论 |
|:--|:--|:--|
| "6个ScheduledThreadPool" | **6 个实证** (retryRollbacking/retryCommitting/asyncCommitting/timeoutCheck/undoLogDelete/syncProcessing, L182-198) — 全 1 线程; 另有 branchRemoveExecutor (ThreadPool) | **接受** ✅ |
| "5种GlobalStatus状态组" | **5 个状态组数组实证** (retryRollbacking 3/retryCommitting 1/rollbacking 1/committing 1/end 4, L200-210) — 非"21 态分 5 组"而是"5 组筛选条件" | **表述精确化** ⚠ |
| "RETRY_DEAD_THRESHOLD" | 70s (DefaultValues:385, S-1 实证) + MAX_RETRY_TIMEOUT 默认 -1 (DefaultValues:520-527) | **补充** ✅ |
| 数字: retry period | COMMITTING/ASYNC/ROLLBACKING/TIMEOUT = 1000ms; END_STATUS = 30s; UNDO_LOG_DELETE = 24h (DefaultValues:465-492) | **补充** ✅ |
