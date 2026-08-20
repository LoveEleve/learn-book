# 闭环笔记 q4: 清理面 — 定时删除 + GlobalFinished

## 假设
孤儿 undo_log 定时清理; 并发回滚受 GlobalFinished 保护。

## 验证过程
- **定时清理** (MySQLUndoLogManager:61-66): **DELETE ... WHERE log_created <= ? LIMIT ?** — 分页删除 (LIMIT 防大事务锁表)
- **触发链** (S-3 实证): TC undoLogDelete 广播 → RM 侧执行 deleteUndoLogByLogCreated (saveDays 参数) — S-3:548-570
- **saveDays**: UndoLogDeleteRequest.DEFAULT_SAVE_DAYS — 保留天数配置
- **GlobalFinished 防护** (S-2 实证, #489): 回滚时无 undo_log → 插 GlobalFinished → 防 Phase1 后提交 — 并发回滚保护
- **批删与单删**: batchDeleteUndoLog (AsyncWorker 面) + deleteUndoLog (单分支) + DELETE_SUB_UNDO_LOG (子行)
- **deleteUndoLogByLogCreated 默认 0** (AbstractUndoLogManager:546-549): 基类空实现 — 方言子类实现 (MySQL 已实现)

## 代码类型
Implementation (清理面)

## 跨域关联
- S-3: undoLogDelete 广播触发
- S-2: GlobalFinished (S-11 收束)
- S-7: 清理与重试共存面

## 结论
清理 = log_created 分页删除 (LIMIT) + 广播触发 (saveDays) + GlobalFinished 并发保护 + 方言实现。
源码位置: MySQLUndoLogManager.java:61-66; AbstractUndoLogManager.java:546-549; DefaultCoordinator.java:548-570
