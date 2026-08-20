# 闭环笔记 q2: 反向 SQL — 三模板 + 方言

## 假设
回滚 = 把 beforeImage 恢复: DELETE→INSERT / INSERT→DELETE / UPDATE→UPDATE SET; 方言差异在 SQL 语法面。

## 验证过程
- **反向三模板** (MySQL 实证):
  - DELETE 的 undo = **INSERT INTO a (x, y, z, pk) VALUES (...) FROM beforeImage** (MySQLUndoDeleteExecutor:51-82) — 用 before 行重建; PK 在最后 (L70 注释, undoPrepare 约定)
  - INSERT 的 undo = **DELETE FROM a WHERE pk = ?** (MySQLUndoInsertExecutor:42-76) — 用 afterImage 的 PK 定位
  - UPDATE 的 undo = **UPDATE a SET x = ?, y = ? WHERE pk1 = ? and pk2 = ?** (MySQLUndoUpdateExecutor:40-73) — SET 用 before 值, WHERE 用 PK
- **executeOn** (AbstractUndoExecutor:114-148): 校验 → buildUndoSQL → per undoRow: **非 PK 字段入 undoValues + PK 在最后** (L127-131,210-217) → undoPrepare 特殊类型处理 (BLOB/CLOB/LONGVARBINARY/DATALINK/ARRAY, L158-218) → executeUpdate
- **方言 SPI**: UndoExecutorFactory.getUndoExecutor(dbType) → **UndoExecutorHolderFactory** (L36) → 13 方言目录 (mysql/oracle/postgresql/dm/kingbase/mariadb/oceanbase/oscar/polardbx/sqlserver...) — JSON 类型 MySQL 专用 (MySQLJsonHelper.convertIfJson L78)
- **PK 顺序保证** (L351-366): getOrderedPkList 按 tableMeta.getPrimaryKeyOnlyName 排序 — 多列 PK 顺序稳定

## 代码类型
Implementation (SQL 生成)

## 跨域关联
- S-10: 方言 SPI 家族 (exec/ 与 undo/ 同源分叉)
- S-4: ConnectionProxy (targetConnection 直连 — 不经代理防递归)
- Z-9 (4.3): 对照 txnlog 格式 (undo_log 是数据面补偿 vs ZK 是状态面 WAL)

## 结论
反向 SQL = 三模板 × 13 方言; PK 恒在最后参数位; 特殊 JDBC 类型逐个处理。
源码位置: MySQLUndoDeleteExecutor.java:51-82; MySQLUndoInsertExecutor.java:42-76; MySQLUndoUpdateExecutor.java:40-73; AbstractUndoExecutor.java:114-218,351-366
