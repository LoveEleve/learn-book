# S-2 undo_log 机制 — Pass 1 探索笔记

> 域: S-2 undo_log | 🔴 A 方案 (需 harness) | 2026-08-15
> 源码: rm-datasource/undo/ (AbstractUndoLogManager 589 + AbstractUndoExecutor 400 + UndoLogManager 87 + SQLUndoLog 124 + BranchUndoLog 88 + UndoExecutorFactory 53 + MySQL 方言 3 executor) + exec/BaseTransactionalExecutor (610) + exec/SelectForUpdateExecutor | Seata 2.5.0

## 调用图

```
Phase1 (业务执行, S-4/S-10 交叉):
  BaseTransactionalExecutor.prepareUndoLog → beforeImage (SELECT FOR UPDATE 行锁, SelectForUpdateExecutor:152)
    → 业务 UPDATE/DELETE/INSERT → afterImage (buildTableRecords 再查)
    → UPDATE 行数不等 → ShouldNeverHappenException (主键被改)
    → lockKeyRecords = DELETE ? before : after → appendLockKey + appendUndoLog
  commit 本地事务时: ConnectionProxy → AbstractUndoLogManager.flushUndoLogs
    → BranchUndoLog (xid/branchId/sqlUndoLogs) → parser.encode → 压缩 (>64k 且开启 → zip)
    → insertUndoLogWithNormal (undo_log 表, State.Normal)

Phase2 rollback (RM 侧):
  AbstractUndoLogManager.undo → for(;;) 无限重试
    → SELECT ... FOR UPDATE (行锁) → state==GlobalFinished → ignore (重复回滚防护)
    → parser.decode → sqlUndoLogs.size()>1 → reverse (逆序回滚)
    → per SQLUndoLog: UndoExecutorFactory.getUndoExecutor(dbType) → executeOn
        → dataValidationAndGoOn 三步比对 (before==after 跳过 / after==current OK / before==current 跳过 / dirty → Unretriable)
        → buildUndoSQL 反向 SQL (DELETE→INSERT / INSERT→DELETE / UPDATE→UPDATE SET)
    → exists → deleteUndoLog + commit / !exists → insertUndoLogWithGlobalFinished (防 Phase1 提交, #489)
```

## 基本元素分解

1. **undo_log 表生命周期**: flush (Normal 插入) → undo (读取/校验/反向执行) → delete 或 GlobalFinished 标记
2. **镜像对**: SQLUndoLog (sqlType/tableName/beforeImage/afterImage) + TableRecords/Row/Field
3. **反向 SQL**: AbstractUndoExecutor.buildUndoSQL (抽象) + MySQL 三实现 (DELETE→INSERT/INSERT→DELETE/UPDATE→UPDATE)
4. **数据校验**: dataValidationAndGoOn 三步比对 (before==after / after==current / before==current)
5. **方言 SPI**: UndoExecutorFactory → UndoExecutorHolder (13 方言目录)

## 标记问题 (20 问)

1. undo_log 表结构? (branch_id/xid/rollback_info/log_status/log_created/log_modified)
2. State 枚举? (Normal 0 / GlobalFinished 1)
3. flush 时机? (本地事务 commit 时)
4. 压缩? (默认 zip, >64k, 可关)
5. undo 循环? (for(;;) 无限重试)
6. 重复回滚防护? (GlobalFinished 忽略)
7. 逆序回滚? (sqlUndoLogs.size()>1 → reverse)
8. 行锁? (SELECT ... FOR UPDATE)
9. 反向 SQL 三模板? (DELETE→INSERT / INSERT→DELETE / UPDATE→UPDATE SET)
10. 数据校验三步? (before==after / after==current / before==current)
11. dirty 数据? (SQLUndoDirtyException → Unretriable)
12. 主键被改? (UPDATE 行数不等 → ShouldNeverHappenException)
13. beforeImage 采集? (SELECT FOR UPDATE)
14. lockKey? (DELETE ? before : after)
15. GlobalFinished 插入? (undo_log 不存在时 — 防 Phase1 提交, #489)
16. 子表? (SUB_ID_KEY + getSubRollbackInfo — S-11)
17. max_allowed_packet? (context 记录)
18. 方言数? (13: mysql/oracle/pg/... )
19. Batch delete? (IN 参数)
20. hasUndoLogTable? (建表检查)

## 时空溯源 (代码内注释锚)

- AbstractUndoLogManager:394-397 "See https://github.com/seata/seata/issues/489" — GlobalFinished 防 Phase1 提交 (#489 锚)
- AbstractUndoLogManager:343-345 "It is possible that the server repeatedly sends a rollback request" — 重复回滚防护注释
- AbstractUndoLogManager:390-397 "business processing timeout, the global transaction is the initiator rolls back" — 无 undo_log 场景
- AbstractUndoExecutor:70 "TODO support multiple primary key" — CHECK_SQL 单主键历史
- BaseTransactionalExecutor:408-411 "probably because you updated the primary keys" — 行数不等诊断
- AbstractUndoLogManager:87 CHECK_UNDO_LOG_TABLE_EXIST_SQL

## 大域拆分判断

S-2 = undo_log 数据面 (写/读/校验/反向执行); 单篇 🔴 A (8 闭环 q1-q4 + harness 4 面); 深挖面 = 校验三步决策/逆序回滚/重试语义/方言

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划/规划) | 验证 | 结论 |
|:--|:--|:--|
| "UndoLogManager—beforeImage(SELECT FOR UPDATE)+afterImage+反向SQL" | beforeImage 采集 SelectForUpdateExecutor:152 (FOR UPDATE); afterImage buildTableRecords; 反向 SQL 三模板实证 | **接受** ✅ |
| 数字: 压缩默认 | enable=true / type=zip / threshold=64k (DefaultValues:368-380) | **补充** ✅ |
| 数字: 校验默认 | DEFAULT_TRANSACTION_UNDO_DATA_VALIDATION=true (L241) | **补充** ✅ |
| 数字: 表名 | DEFAULT_TRANSACTION_UNDO_LOG_TABLE="undo_log" (L253) | **补充** ✅ |
| "13 方言" | undo/ 目录 13 方言包 (dm/kingbase/mariadb/mysql/oceanbase/oracle/oscar/polardbx/postgresql/sqlserver + 2) 实证 | **接受** ✅ |
