# S-1 AT 两阶段提交 — Pass 1 探索笔记

> 域: S-1 AT 两阶段 | 🔴 A 方案 (需 harness) | 2026-08-15
> 源码: tm/TransactionalTemplate (407) + tm/DefaultGlobalTransaction (300) + tm/GlobalTransactionContext (81) + tm/DefaultTransactionManager + server/DefaultCore (550) + server/session/GlobalSession + core/model (GlobalStatus/BranchType) + common/XID | Seata 2.5.0

## 调用图

```
@GlobalTransactional (S-9) → TransactionalTemplate.execute
  → Propagation 决策 (6 种) → beginTransaction (Launcher 才 begin)
      → tx.begin → DefaultGlobalTransaction.begin → DefaultTransactionManager.begin
          → GlobalBeginRequest → TC (Netty) → DefaultCore.begin
              → GlobalSession.createGlobalSession → session.begin (status=Begin, onBegin)
  → business.execute() (业务, RM 侧分支注册 S-4)
  → commitTransaction → tx.commit → GlobalCommitRequest → TC → DefaultCore.commit
      → lockAndExecute → session.close (不再收分支) → Committing/AsyncCommitting
      → doGlobalCommit: getSortedBranches 正向 → getCore(branchType).branchCommit
          → PhaseTwo_Committed → removeBranch; 失败 → queueToRetryCommit
  → 异常: completeTransactionAfterThrowing → rollbackOn? → tx.rollback → DefaultCore.rollback
      → lockAndExecute → Rollbacking → doGlobalRollback: getReverseSortedBranches 反向
      → PhaseTwo_Rollbacked → removeBranch
  → finally: resumeGlobalLockConfig + afterCompletion + cleanUp + resume(suspended)
```

## 基本元素分解

1. **模板编排**: TransactionalTemplate.execute (传播决策 → begin → 业务 → commit/rollback → 清理)
2. **全局事务对象**: DefaultGlobalTransaction (xid/status/role/createTime) + GlobalTransactionContext (getCurrent/createNew/reload)
3. **TM 协议**: DefaultTransactionManager (GlobalBeginRequest/CommitRequest/RollbackRequest syncCall)
4. **TC 核心**: DefaultCore (begin/commit/rollback + doGlobalCommit/doGlobalRollback + doBranchDelete)
5. **状态面**: GlobalStatus 21 态 + BranchType 5 值 + XID 格式

## 标记问题 (20 问)

1. execute 编排顺序? (传播→begin→业务→commit/rollback→清理)
2. 传播 6 种语义? (REQUIRED/REQUIRES_NEW/NOT_SUPPORTED/SUPPORTS/NEVER/MANDATORY)
3. Launcher vs Participant 角色? (谁 begin/commit/rollback)
4. begin 流程? (createTime→assert→TM.begin→RootContext.bind)
5. commit 流程? (超时检查→beforeCommit→tx.commit→状态映射)
6. rollback 流程? (rollbackOn 判定→rollback→状态映射)
7. 超时双面? (客户端 commit 前 / TC commit 前)
8. 钩子 7 个? (beforeBegin/afterBegin/beforeCommit/afterCommit/beforeRollback/afterRollback/afterCompletion)
9. 启发式状态? (Finished → CommitHeuristic)
10. doGlobalCommit 遍历? (getSortedBranches 正向)
11. doGlobalRollback 遍历? (getReverseSortedBranches 反向)
12. 分支状态处理? (PhaseOne_Failed/RDONLY 移除/STOP_RETRY 跳过)
13. 异步提交? (canBeCommittedAsync → asyncCommit)
14. 失败转重试? (queueToRetryCommit/Rollback)
15. lockAndExecute? (SessionHolder 并发控制)
16. doBranchDelete 语义? (AT 用 commit 语义删)
17. XID 格式? (IP:PORT:transactionId)
18. GlobalStatus 21 态? (code 0-20)
19. BranchType 5 值? (AT/TCC/SAGA/XA/SAGA_ANNOTATION)
20. XAER_NOTA 超时数学? (beginTime+timeout+max(RETRY_XAER_NOTA_TIMEOUT, timeout))

## 时空溯源 (代码内注释锚)

- DefaultGlobalTransaction: "#fix #5231" (rollback 状态映射修正 — 2.x 锚)
- DefaultCore: "Highlight: Firstly, close the session, then no more branch can be registered" (close 先于状态迁移 — 竞态防护注释)
- DefaultCore: "In db mode, lock and branch data residual problems may occur. Therefore, execution needs to be delayed here" (endRollbacked 延迟 — db 模式残留问题)
- DefaultCore: "Only databases with read-only optimization, such as Oracle, will report the RDONLY status" (XA RDONLY 忽略)
- DefaultCore: "if not retrying, skip the canBeCommittedAsync branches" (异步分支跳过)
- GlobalSession.begin: status=Begin + beginTime + active + onBegin

## 大域拆分判断

S-1 = AT 总纲 (客户端编排 + TC 协调核心); 单篇 🔴 A (8 闭环 q1-q4 + harness 4 面); 深挖面 = 传播决策/角色分离/超时双面/状态机/分支遍历方向

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划/规划) | 验证 | 结论 |
|:--|:--|:--|
| "TransactionalTemplate/DefaultCore" | 407/550 行实证 | **接受** ✅ |
| "Phase1(begin+undo)→Phase2(commit/rollback)" | begin 在 TransactionalTemplate L125 + 业务内 undo 由 RM 侧 (S-4 交叉) — 总纲正确 | **接受** ✅ |
| "6 种 Propagation" | 枚举 6 值实证 (无 NESTED — 对照 Spring 7 种) | **接受+对照** ✅ |
| "7 个生命周期钩子" | 7 个 trigger 方法实证 | **接受** ✅ |
| 数字: GlobalStatus | **21 态 (code 0-20)** — 执行计划 S-3 "5种状态组" 待 S-3 穷举 | **补充** ✅ |
| 数字: BranchType | **5 值含 SAGA_ANNOTATION** | **补充** ✅ |
| 数字: 超时默认 | DEFAULT_GLOBAL_TX_TIMEOUT=60000ms (L41) | **补充** ✅ |
