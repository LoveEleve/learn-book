# S-13 Phase2 分支通知 — Pass 1 探索笔记

> 域: S-13 Phase2 分支通知 | 🔴 A 方案 (需 harness) | 2026-08-15
> 源码: rm/ (AbstractRMHandler + DefaultRMHandler) + rm-datasource/RMHandlerAT + tcc/RMHandlerTCC + saga/RMHandlerSaga + core/rpc/processor/client (RmBranchCommitProcessor/RmBranchRollbackProcessor) | Seata 2.5.0

## 调用图

```
TC 侧 (S-1 实证): doGlobalCommit/doGlobalRollback → getCore(branchType) 多态 → branchCommit/branchRollback
  → 分支通知协议: BranchCommitRequest/BranchRollbackRequest → RM (Netty)

RM 侧 (本域):
  RmBranchCommitProcessor (client:61-63): 接收 → handler.onRequest(BranchCommitRequest)
    → DefaultRMHandler (L66-82): getRMHandler(branchType) 多态分发
      → RMHandlerAT.doBranchCommit → DataSourceManager.branchCommit → AsyncWorker (S-11, 异步)
      → RMHandlerAT.doBranchRollback → undoManager.undo (S-2, 补偿执行)
  SPI 注册: EnhancedServiceLoader.loadAll(AbstractRMHandler) → allRMHandlersMap (5 处理器)
  异常: exceptionHandleTemplate (AbstractExceptionHandler 模板)
```

## 基本元素分解

1. **处理器族**: AbstractRMHandler 5 实现 (AT/XA/TCC/Saga/SagaAnnotation) — SPI 注册
2. **分发器**: DefaultRMHandler (getRMHandler(branchType) 多态)
3. **接收端**: RmBranchCommitProcessor/RmBranchRollbackProcessor (client)
4. **协议**: BranchCommitRequest/BranchRollbackRequest (xid/branchId/resourceId/applicationData)
5. **收束**: AT 提交 → AsyncWorker (异步); AT 回滚 → undo (S-2); 异常 → 模板

## 标记问题 (20 问)

1. 处理器族? (5 实现)
2. SPI 注册? (EnhancedServiceLoader.loadAll)
3. DefaultRMHandler? (多态分发)
4. 接收端? (RmBranchCommitProcessor)
5. 协议字段? (xid/branchId/resourceId/applicationData)
6. doBranchCommit? (resourceManager.branchCommit)
7. doBranchRollback? (undoManager.undo)
8. AT 提交? (AsyncWorker 异步)
9. AT 回滚? (undo 补偿)
10. exceptionHandleTemplate? (异常模板)
11. 响应? (BranchStatus 回填)
12. UndoLogDeleteRequest? (默认空, issue #2226)
13. getResourceManager? (DefaultResourceManager.getResourceManager(branchType))
14. 对照 TCC? (RMHandlerTCC)
15. 对照 XA? (RMHandlerXA)
16. MDC? (xid/branchId)
17. 状态回传? (PhaseTwo_*)
18. 完整闭环? (S-1→S-13)
19. 并发? (AsyncWorker 面)
20. 重试? (S-7 RM 侧)

## 时空溯源 (代码内注释锚)

- AbstractRMHandler:84-87 "https://github.com/seata/seata/issues/2226" — UndoLogDelete 默认空
- DefaultRMHandler: SPI loadAll 注册 (2.x 锚)
- RmBranchCommitProcessor: "rm client handle branch commit process" (1.x 锚)

## 大域拆分判断

S-13 = Phase2 收束面 (处理器族 + 接收端 + 协议 + 完整闭环); 单篇 🔴 A (8 闭环 q1-q4 + harness 4 面)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划/规划) | 验证 | 结论 |
|:--|:--|:--|
| "DefaultCore.doGlobalCommit—正向/反向分支遍历+getCore(branchType) 多态" | S-1 实证 (正向 commit/反向 rollback + getCore) | **接受** ✅ |
| "RM 处理器族" | 5 实现实证 (AT/XA/TCC/Saga/SagaAnnotation) + SPI | **接受+补充** ✅ |
| "分支通知协议" | BranchCommitRequest/BranchRollbackRequest | **接受** ✅ |
| 收束: AT 提交异步 | AsyncWorker (S-11) — 提交即回 Committed | **接受** ✅ |
