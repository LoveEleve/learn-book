# 闭环笔记 q1: undo_log 生命周期 — flush/undo/删除 + 无限重试

## 假设
undo_log = 本地事务内的补偿数据表: Phase1 写, Phase2 读+执行+清理; 重试语义有界。

## 验证过程
- **State 枚举** (AbstractUndoLogManager:62-82): **Normal(0) / GlobalFinished(1)** — log_status 字段两值
- **flushUndoLogs** (L266-303): 无 undo 直接返回 → BranchUndoLog (xid/branchId/sqlUndoLogs) → **parser.encode** (UndoLogParserFactory SPI) → **压缩: needCompress = enable && length > 64k** (L571-573; 默认 zip, DefaultValues:368-380) → buildContext (serializer/compressorType/max_allowed_packet) → insertUndoLogWithNormal
- **undo 主循环** (L315-466): **for(;;) 无限重试** (SQLIntegrityConstraintViolationException → continue, L419-423) — 整个 undo 在本地事务中 (autoCommit=false, L328-331)
  - **SELECT ... FOR UPDATE 行锁** (L334-337) — 防并发重复 undo
  - **state != Normal → 忽略** (L346-352, 重复回滚防护 — 服务端可能重复发 rollback 到多进程, L343-345 注释)
  - **sqlUndoLogs.size() > 1 → Collections.reverse 逆序回滚** (L368-370)
  - per SQLUndoLog: tableMeta + **UndoExecutorFactory.getUndoExecutor(dbType)** → executeOn
  - **exists → deleteUndoLog + commit** / **!exists → insertUndoLogWithGlobalFinished** (L399-416, 防 Phase1 后提交 — #489 注释 L394-397)
- **异常分类** (L419-445): SQLIntegrityConstraintViolationException → 重试 (子表插入竞态) / **SQLUndoDirtyException → Unretriable** ("please delete the relevant undolog after manually calibrating the data") / 其他 → **Retriable** ("try again later")
- **子表面** (L96-97,519-544): DELETE_SUB_UNDO_LOG_SQL + **getRollbackInfo: SUB_ID_KEY → getSubRollbackInfo 拼接 + 解压** (S-11 交叉)
- **批删** (L158-233): toBatchDeleteUndoLogSql IN 参数拼接 (S-11 AsyncWorker 面)

## 代码类型
Implementation (补偿数据生命周期)

## 跨域关联
- S-1: Phase2 rollback → RM 侧 undo (本域是消费面)
- S-7: Retriable/Unretriable 分支状态 (服务端重试面)
- S-11: 子表/压缩/批删 (本域是 S-11 的机制面)
- S-4: ConnectionProxy/DataSourceProxy (连接获取)

## 结论
生命周期 = flush (Normal 插入) → undo (行锁 + 校验 + 逆序反向执行) → delete / GlobalFinished; for(;;) 无限重试 + 脏数据 Unretriable。
源码位置: AbstractUndoLogManager.java:62-97,266-466,519-573; DefaultValues.java:368-380
