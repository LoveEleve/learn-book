# S-2 undo_log 机制 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 0.9~1.x | 骨架: undo_log 表 (branch_id/xid/rollback_info/log_status) + AbstractUndoLogManager + 反向 SQL 三模板 (DELETE→INSERT 等); CHECK_SQL "TODO support multiple primary key" (AbstractUndoExecutor:70 注释锚) |
| 1.x | **GlobalFinished 状态** (issue #489: 防 Phase1 后提交 — L394-397 注释锚); 重复回滚防护 (L343-345 注释锚); 数据校验三步比对 (DEFAULT_TRANSACTION_UNDO_DATA_VALIDATION) |
| 2.x | **压缩**: CLIENT_UNDO_COMPRESS_ENABLE/TYPE/THRESHOLD (zip/64k); **子表**: SUB_ID_KEY + getSubRollbackInfo + DELETE_SUB_UNDO_LOG_SQL; 批量删除 IN 拼接; 方言扩展至 13 (oceanbase/polardbx/oscar/kingbase...) |
| 2.5.0 | max_allowed_packet context; JSON 类型支持 (MySQLJsonHelper.convertIfJson) |

## 痕迹证据

- AbstractUndoLogManager.java:394-397: "See https://github.com/seata/seata/issues/489" — GlobalFinished 防 Phase1 提交 (1.x 锚)
- AbstractUndoLogManager.java:343-345: "It is possible that the server repeatedly sends a rollback request to roll back the same branch transaction to multiple processes" — 重复回滚防护 (1.x 锚)
- AbstractUndoLogManager.java:390-397: "business processing timeout, the global transaction is the initiator rolls back" — 无 undo_log 场景 (1.x 锚)
- AbstractUndoExecutor.java:70: "TODO support multiple primary key" — 单主键历史 (0.9 锚)
- BaseTransactionalExecutor.java:408-411: "probably because you updated the primary keys" — 主键守卫诊断 (1.x 锚)

## 推断标注

- "0.9~1.x 骨架" — Seata 前身 Fescar 起引入 (公知版本线) (标注)
- "2.x 压缩/子表" — 特性年代推断 (配置键存在性) (标注)
- "2.5.0 JSON/max_allowed_packet" — 最新特性推断 (标注)
- git 多 commit 可考古 — 本域以注释锚 + JIRA/issue 编号为主

## 对照线 (阶段 3/4 已交付)

- MySQL binlog: 物理 row 格式反向 vs Seata 逻辑补偿 (镜像对+反向 SQL) — 实现差异对照
- Redis AOF (3.5): 命令重放 (正向) vs undo_log (反向) — 方向相反
- ZK txnlog (4.3): 状态面 WAL vs Seata 数据面补偿日志
