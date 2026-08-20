# S-4 DataSource 代理 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 0.9~1.x | 骨架: DataSourceProxy/ConnectionProxy/AbstractConnectionProxy + ConnectionContext (xid/branchId/undoItems) + doCommit 三分支 + register 守卫 |
| 1.x | **savepoint 上下文** (appendSavepoint/removeSavepoint); report 重试面 (CLIENT_REPORT_RETRY_COUNT); 全局锁本地提交 (isGlobalLockRequire 分支) |
| 2.x | **LockRetryPolicy 重构**: CLIENT_LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT (默认 true); autoCommitChanged 标记; FailFast 降级 (LockKeyConflictFailFast); polardb-x 方言检测; 嵌套代理解包 |
| 2.5.0 | IS_REPORT_SUCCESS_ENABLE 默认 false (report 成功默认关); 7 方言 resourceId |

## 痕迹证据

- DataSourceProxy.java:95-100: "Unwrap the target data source, because the type is: {}" — 嵌套解包 (2.x 锚)
- DataSourceProxy.java:167-183: "if the table not exist fast fail, or else keep silence" — undo 表 fast-fail (1.x 锚)
- ConnectionProxy.java:305: "change autocommit from false to true, we should commit() first according to JDBC spec" — setAutoCommit 拦截 (1.x 锚)
- ConnectionProxy.java:352-354: "the only case that not need to retry acquire lock hear is..." — LockRetryPolicy 注释 (2.x 锚)
- ConnectionProxy.java:372-374: "AbstractDMLBaseExecutor#executeAutoCommitTrue the local lock is released" — FailFast 降级 (2.x 锚)

## 推断标注

- "0.9~1.x 骨架" — Seata 前身 Fescar 起 (公知版本线) (标注)
- "2.x LockRetryPolicy/polardb-x" — 配置键存在性推断 (标注)
- "2.5.0 IS_REPORT_SUCCESS_ENABLE" — DefaultValues 实证 (实证)
- git 多 commit 可考古 — 本域以注释锚 + 配置键为主

## 对照线 (阶段 3/4 已交付)

- HikariCP (3.5): 连接池代理 (PoolProxy) vs Seata 事务代理 — 池化 vs 拦截
- MyBatis (3.4): 插件链 (Interceptor) vs Seata 三层代理 — 拦截方式对照
- ZK (4.3): 无对应面 (纯客户端代理模式)
