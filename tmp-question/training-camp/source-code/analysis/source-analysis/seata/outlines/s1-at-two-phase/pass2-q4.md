# 闭环笔记 q4: 状态面 — GlobalStatus 21 态 + BranchType 5 值 + 超时判定

## 假设
全局事务状态机 21 态; 分支类型 5 值; 超时判定有确定数学。

## 验证过程
- **GlobalStatus 21 态** (core/model/GlobalStatus.java:29-149, code 0-20): UnKnown(0)/Begin(1)/Committing(2)/CommitRetrying(3)/Rollbacking(4)/RollbackRetrying(5)/**TimeoutRollbacking(6)/TimeoutRollbackRetrying(7)/AsyncCommitting(8)**/Committed(9)/CommitFailed(10)/Rollbacked(11)/RollbackFailed(12)/TimeoutRollbacked(13)/TimeoutRollbackFailed(14)/Finished(15)/CommitRetryTimeout(16)/RollbackRetryTimeout(17)/Deleting(18)/StopCommitOrCommitRetry(19)/StopRollbackOrRollbackRetry(20)
- **状态组判定** (L200-245): **isOnePhaseTimeout** = TimeoutRollbacking/TimeoutRollbackRetrying/TimeoutRollbacked/TimeoutRollbackFailed (4 态); **isTwoPhaseSuccess** = Committed/Rollbacked/TimeoutRollbacked/Deleting (4 态); **isTwoPhaseHeuristic** = **Finished** (仅 1 态 — "事务不存在" 判定)
- **BranchType 5 值** (core/model/BranchType.java:29-48): **AT/TCC/SAGA/XA/SAGA_ANNOTATION** — 执行计划未提 SAGA_ANNOTATION; getCore(branchType) 多态 (DefaultCore:86-92, 未注册 → NotSupportYetException)
- **超时双面**: 客户端 commit 前 (TransactionalTemplate:211-220 — 本地时钟) + TC commit 前 (DefaultCore:243-246 — 服务端时钟, GlobalSession.isTimeout); **XID 格式** IP:PORT:transactionId (common/XID:55-62)
- **commit 状态返回面** (TransactionalTemplate:225-251): 客户端把 TC 返回状态映射为 TransactionalExecutor.Code; 启发式 Finished → CommitHeuristic ("may be rollbacked")

## 代码类型
Data (状态枚举)

## 跨域关联
- S-3: DefaultCoordinator 消费状态 (5 状态组面)
- S-7: Retrying 状态族 (CommitRetrying/RollbackRetrying/Timeout*)
- S-13: 分支状态消费 (PhaseTwo_*)

## 结论
状态机 21 态 (3 阶段: 未知/Phase1/Phase2 族 + 终态族); 分支 5 类型; 超时双面判定; 启发式仅 Finished。
源码位置: GlobalStatus.java:29-245; BranchType.java:29-48; DefaultCore.java:243-246
