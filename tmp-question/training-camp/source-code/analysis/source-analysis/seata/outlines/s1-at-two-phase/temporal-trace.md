# S-1 AT 两阶段提交 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 0.9~1.x | 骨架: TransactionalTemplate + DefaultGlobalTransaction + DefaultCore (begin/commit/rollback + doGlobalCommit/Rollback) + GlobalStatus 基础态 (UnKnown~Finished) |
| 1.x~2.x | **超时族扩展**: TimeoutRollbacking/TimeoutRollbacked/TimeoutRollbackFailed (isOnePhaseTimeout 4 态); BranchType 增 SAGA_ANNOTATION |
| 2.x | **异步提交**: AsyncCommitting(8) + canBeCommittedAsync + asyncCommit (无同步分支时异步化); StopCommitOrCommitRetry(19)/StopRollbackOrRollbackRetry(20); PARALLEL_HANDLE_BRANCH 并行分支 |
| 2.5.0 | #fix #5231 (rollback 状态映射修正 — DefaultGlobalTransaction 注释锚); XAER_NOTA retry timeout; Deleting(18) |

## 痕迹证据

- DefaultGlobalTransaction.java:277: "# fix #5231" — rollback 状态族映射修正 (2.5 锚)
- DefaultCore.java:252: "Highlight: Firstly, close the session, then no more branch can be registered" — 关会话防竞态注释
- DefaultCore.java:502-503: "In db mode, lock and branch data residual problems may occur... delayed" — endRollbacked 延迟 (db 模式)
- DefaultCore.java:306-308: "Only databases with read-only optimization, such as Oracle, will report the RDONLY status" — XA RDONLY 忽略
- DefaultCore.java:296: "if not retrying, skip the canBeCommittedAsync branches" — 异步分支跳过
- GlobalSession.java:237-245: begin → status=Begin + beginTime + onBegin

## 推断标注

- "0.9~1.x 骨架" — Seata 前身 Fescar 0.4.0 → 1.x 演进 (公知版本线) (标注)
- "2.x 异步提交" — AsyncCommitting 状态存在性推断 (标注)
- "2.5.0 #5231" — 注释实证 (实证)
- git 多 commit (989db47c "fix client spring version compatible") — 有历史可考古, 但本域以注释锚为主

## 对照线 (阶段 3 已交付)

- Spring 事务管理器: 7 传播 (含 NESTED) vs Seata 6; Spring REQUIRED 语义同构 (线程绑定 + 传播决策)
- RocketMQ RM-12 (消息事务): 半消息 + 确认 vs Seata undo_log 补偿 — 两阶段实现对照
