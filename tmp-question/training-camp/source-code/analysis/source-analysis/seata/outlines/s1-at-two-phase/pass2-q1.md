# 闭环笔记 q1: 模板编排 — Propagation 决策 + 角色分离

## 假设
TransactionalTemplate = 全局事务的编排骨架: 传播决策 → begin → 业务 → commit/rollback → 清理。

## 验证过程
- **传播决策** (TransactionalTemplate:66-113): **6 种** — NOT_SUPPORTED (挂起现有→无事务执行) / REQUIRES_NEW (挂起现有→新建) / SUPPORTS (无则无事务执行) / REQUIRED (getCurrentOrCreate) / NEVER (有则抛) / MANDATORY (无则抛) — **无 NESTED** (对照 Spring 7 种); 挂起返回 SuspendedResourcesHolder, 外层 finally resume (L147-152)
- **角色分离** (Launcher vs Participant): beginTransaction/commitTransaction/rollbackTransaction 全部 **Launcher 才执行** (L118-120,205-210,261-266) — Participant 只 join (log "join into a existing global transaction")
- **超时检查** (commitTransaction L211-220): **客户端 commit 前 isTimeout → 转 rollback** (TmTransactionException TransactionTimeout)
- **异常映射** (completeTransactionAfterThrowing L191-201): **rollbackOn(exception) ? rollback : commit** — 非回滚异常提交 (Spring 事务语义)
- **状态码映射** (L225-301): commit 后 TimeoutRollbacking→Rollbacking / TimeoutRollbacked→RollbackDone / **Finished→CommitFailure**; rollback 后 #fix #5231 修正 (L277-301): RollbackFailed/TimeoutRollbackFailed/RollbackRetryTimeout→RollbackFailure; Rollbacking/RollbackRetrying/TimeoutRollbacking→Rollbacking; TimeoutRollbacked/Rollbacked/Finished→RollbackDone; 未知 → warn
- **钩子 7 个** (L321-391): beforeBegin/afterBegin/beforeCommit/afterCommit/beforeRollback/afterRollback + **afterCompletion (仅 Launcher)** (L381-391); 每个钩子异常只 log 不中断 (L322-328)
- **清理面** (L141-146): resumeGlobalLockConfig + triggerAfterCompletion + **cleanUp → TransactionHookManager.clear (仅 Launcher)** (L393-402)
- **GlobalLockConfig**: replaceGlobalLockConfig 按 txInfo 注入 lockRetryInterval/Times/Strategy (L175-181)

## 代码类型
Architecture (事务编排)

## 跨域关联
- S-5: Propagation 6 种 (本域是消费面)
- S-6: TransactionHook 7 个 (本域是触发面)
- S-9: @GlobalTransactional → 本模板 (入口)
- S-7: TM 侧 commit/rollback retry (DefaultGlobalTransaction)

## 结论
编排 = 传播决策 + 角色分离 (Launcher 驱动全流程) + 客户端超时转回滚 + 钩子触发 + 状态码映射; 非回滚异常提交语义。
源码位置: TransactionalTemplate.java:53-407; GlobalTransactionContext.java:36-80
