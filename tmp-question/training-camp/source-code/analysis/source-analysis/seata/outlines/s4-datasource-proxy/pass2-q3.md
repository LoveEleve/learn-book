# 闭环笔记 q3: 连接上下文 — ConnectionContext 全 API

## 假设
每个 ConnectionProxy 一个 ConnectionContext: 线程/连接级的事务状态容器。

## 验证过程
- **结构** (ConnectionContext: 416 行): xid / branchId / **undoItems (List\<SQLUndoLog\>)** / **lockKeys (List\<String\>)** / savepoints / autoCommitChanged / globalLockRequire
- **bind** (L176): 全局事务绑定 (xid 设置) — 连接被纳入全局事务
- **inGlobalTransaction** (L176-185): xid 非空判定
- **appendUndoItem / appendLockKey** (S-2 交叉): 写操作执行链 prepareUndoLog 后追加
- **buildLockKeys** (L344): lockKeys 列表拼接 — 分支注册/锁查询的键串 (S-12)
- **hasUndoLog / hasLockKey** (L213-222): register 守卫依据 (q2)
- **isBranchRegistered** (L185): branchId 非空 — rollback/report 依据
- **reset** (L319): 提交/回滚后**全清** (xid/branchId/undo/lockKeys/savepoints) — 连接归还前状态归零
- **savepoint 面** (L125-): appendSavepoint/removeSavepoint/releaseSavepoint — 内嵌事务支持
- **setAutoCommitChanged** (L96): changeAutoCommit 标记 — LockRetryPolicy 分支依据 (q4)

## 代码类型
Data (上下文)

## 跨域关联
- S-2: undoItems 容器 (flushUndoLogs 消费)
- S-12: lockKeys 容器 (锁键采集)
- S-1: xid/branchId (分支注册依据)

## 结论
上下文 = 连接级事务状态容器 (xid/branchId/undo/lockKeys/savepoints); reset 提交后归零。
源码位置: ConnectionContext.java:96-373
