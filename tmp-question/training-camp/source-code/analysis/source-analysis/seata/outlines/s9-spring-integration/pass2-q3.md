# 闭环笔记 q3: 拦截链 — InterceptorHandler 双 handler

## 假设
拦截器按注解类型分发: TM handler (全局事务) + GlobalLock handler (全局锁)。

## 验证过程
- **GlobalTransactionalInterceptorHandler** (integration-tx-api): **handleGlobalTransaction(InvocationWrapper, AspectTransactional) // TM handler** + **handleGlobalLock(InvocationWrapper, GlobalLock) // GlobalLock handler** (Scanner 注释 L291-297)
- **TM 路径**: AdapterInvocationWrapper + **transactionalTemplate.execute(business)** (L78,195-207) — S-1 模板消费
- **ExecutionException 处理** (L245-260+): **Participant → 抛原异常** (不吞); RollbackDone + **isTimeoutException → 抛 cause**; BeginFailure/CommitFailure/RollbackFailure → **failureHandler.onBeginFailure/onCommitFailure/onRollbackFailure** + throw cause
- **FailureHandler**: 构造注入, null → **FailureHandlerHolder.getFailureHandler()** (L121-122) — 默认处理器兜底
- **methodsToProxy**: 方法级过滤 (构造参数 L121-122)

## 代码类型
Implementation (拦截链)

## 跨域关联
- S-1: TransactionalTemplate (核心消费)
- S-12: GlobalLock handler (全局锁拦截)
- S-5: rollbackRules (规则源)

## 结论
拦截 = 双 handler 分发 (TM/GlobalLock) + ExecutionException 状态化处理 + FailureHandler 兜底。
源码位置: GlobalTransactionalInterceptorHandler.java:78,121-122,195-260
