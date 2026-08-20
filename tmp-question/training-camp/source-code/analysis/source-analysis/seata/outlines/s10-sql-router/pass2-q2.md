# 闭环笔记 q2: executor 族 — 基类链与方言覆盖

## 假设
executor 分基类 + 方言覆盖两层; 镜像采集在基类 (S-2 交叉)。

## 验证过程
- **基类链**: Executor 接口 → **AbstractDMLBaseExecutor** (S-4 实证: doExecute autoCommit 双路径 + beforeImage/afterImage + prepareUndoLog) → InsertExecutor/UpdateExecutor/DeleteExecutor/SelectForUpdateExecutor
- **方言覆盖** (exec/ 10 方言目录): mysql (MySQLInsertExecutor/MySQLInsertOnDuplicateUpdateExecutor/MySQLUpdateJoinExecutor) + oracle (OracleInsertExecutor + **OracleJdbcType**) + sqlserver (Update/Delete/SelectForUpdate/MultiUpdate/MultiDelete) + dm/kingbase/mariadb/oceanbase/oscar/polardbx/postgresql
- **InsertExecutor 双面**: 接口 + SPI 实现 (mysql/oracle/pg...) — **EnhancedServiceLoader.load 方言选择** (q1)
- **MultiUpdate/MultiDelete**: 多语句批量 (sqlserver 变体专用, MultiExecutor:81-88)
- **SelectForUpdateExecutor**: 行锁镜像入口 (S-2 beforeImage)

## 代码类型
Implementation (executor 族)

## 跨域关联
- S-4: AbstractDMLBaseExecutor (执行链)
- S-2: 镜像采集
- S-12: 锁重试 (SelectForUpdate 内 LockRetryController)

## 结论
executor = 基类链 (镜像/undo 公共) + 10 方言覆盖; Insert 双面 SPI。
源码位置: exec/ (基类 + 10 方言目录); MultiExecutor.java:45-88
