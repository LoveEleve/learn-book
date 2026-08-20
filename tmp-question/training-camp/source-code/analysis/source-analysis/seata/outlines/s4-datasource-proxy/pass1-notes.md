# S-4 DataSource 代理 — Pass 1 探索笔记

> 域: S-4 DataSource 代理 | 🔴 A 方案 (需 harness) | 2026-08-15
> 源码: rm-datasource/ (DataSourceProxy 455 + ConnectionProxy 393 + AbstractConnectionProxy 375 + ConnectionContext 416 + DataSourceManager 153 + AbstractDMLBaseExecutor) | Seata 2.5.0

## 调用图

```
业务 (Spring DataSource 注入 → DataSourceProxy):
  DataSourceProxy 构造: 解包嵌套代理 → init (jdbcUrl→dbType→方言检测 polardb-x→undo_log 表 fast-fail)
    → initResourceId (7 方言 URL) → DefaultResourceManager.registerResource → TableMetaCache 注册
  getConnection → ConnectionProxy (包装 targetConnection)

写操作链: StatementProxy → AbstractDMLBaseExecutor.doExecute
  autoCommit=true → executeAutoCommitTrue (LockRetryPolicy 包裹)
  autoCommit=false → beforeImage (SELECT FOR UPDATE) → 业务执行 → afterImage → prepareUndoLog (S-2)
    → context.appendUndoLog + appendLockKey

提交拦截: ConnectionProxy.commit → LockRetryPolicy.execute → doCommit 三分支:
  inGlobalTransaction → processGlobalTransactionCommit: register (有 undo+lockKey 才注册)
    → flushUndoLogs (S-2) → targetConnection.commit → 失败 report(false)/成功 IS_REPORT_SUCCESS_ENABLE 才 report(true)
  isGlobalLockRequire → processLocalCommitWithGlobalLocks: checkLock → commit
  else → targetConnection.commit
回滚: rollback → targetConnection.rollback → 分支已注册 → report(false) → context.reset
```

## 基本元素分解

1. **代理链**: DataSourceProxy → ConnectionProxy → StatementProxy/PreparedStatementProxy (3 层)
2. **连接上下文**: ConnectionContext (xid/branchId/undoItems/lockKeys/savepoints/autoCommitChanged)
3. **提交拦截**: doCommit 三分支 + register 守卫 + report 重试
4. **锁重试**: LockRetryPolicy (LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT)
5. **执行链**: AbstractDMLBaseExecutor (autoCommit 双路径)

## 标记问题 (20 问)

1. DataSourceProxy 构造? (解包嵌套代理 → init)
2. init 干什么? (dbType 检测/undo 表 fast-fail/注册资源)
3. resourceId 怎么生成? (7 方言 URL 解析)
4. getConnection? (包装 ConnectionProxy)
5. getPlainConnection? (绕过代理)
6. doCommit 三分支? (global/lockRequire/plain)
7. register 守卫? (!hasUndoLog || !hasLockKey → 不注册)
8. report? (REPORT_RETRY_COUNT=5; SUCCESS_ENABLE 默认 false)
9. LockRetryPolicy? (branchRollbackOnConflict 默认 true)
10. setAutoCommit 拦截? (false→true 先 doCommit)
11. ConnectionContext? (bind/undo/lockKey/savepoint)
12. buildLockKeys? (格式拼接)
13. reset? (提交/回滚后清上下文)
14. doExecute? (autoCommit 双路径)
15. executeAutoCommitTrue? (LockRetryPolicy 包裹)
16. changeAutoCommit? (标记 autoCommitChanged)
17. savepoint 面? (append/remove/release)
18. undo_log 表 fast-fail? (不存在 → IllegalStateException)
19. polardb-x 检测? (productVersion 关键字)
20. 默认值? (LOCK_RETRY_POLICY=true/REPORT_RETRY=5/SUCCESS_ENABLE=false)

## 时空溯源 (代码内注释锚)

- DataSourceProxy:95-100 "Unwrap the target data source, because the type is: {}" — 嵌套解包
- DataSourceProxy:167-183 "if the table not exist fast fail, or else keep silence" — undo 表检查
- ConnectionProxy:305 "change autocommit from false to true, we should commit() first according to JDBC spec" — JDBC 规范
- ConnectionProxy:352-354 "the only case that not need to retry acquire lock hear is..." — 锁重试策略注释
- ConnectionProxy:372-374 "AbstractDMLBaseExecutor#executeAutoCommitTrue the local lock is released" — FailFast 降级

## 大域拆分判断

S-4 = RM 入口面 (代理链 + 提交拦截 + 上下文); 单篇 🔴 A (8 闭环 q1-q4 + harness 4 面)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划/规划) | 验证 | 结论 |
|:--|:--|:--|
| "DataSourceProxy→ConnectionProxy→ExecuteTemplate—7种SQL类型路由" | 3 层代理实证 (455/393/375); ExecuteTemplate 在 exec/ (S-10) | **接受** ✅ |
| "提交拦截" | doCommit 三分支 + processGlobalTransactionCommit (L227-265) | **接受** ✅ |
| "连接绑定事务上下文" | ConnectionContext 416 行 (bind/undo/lockKey/savepoint) | **接受** ✅ |
| 数字: report | REPORT_RETRY_COUNT=5 / REPORT_SUCCESS_ENABLE=false (DefaultValues:54-60) | **补充** ✅ |
| 数字: 锁重试 | LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT=true (DefaultValues:38-40) | **补充** ✅ |
