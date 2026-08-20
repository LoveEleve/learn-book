# 闭环笔记 q4: 数据校验 — 三步比对决策

## 假设
回滚前校验: before==after 无需 undo; after==current 正常; before==current 已回滚; 其他 = dirty。

## 验证过程
- **决策链** (AbstractUndoExecutor.dataValidationAndGoOn L234-283):
  1. **before == after → 停止 undo** (L241-249, "no data change between the before data snapshot and the after data snapshot") — 业务无实际变更 (如 UPDATE 值相同)
  2. 否则查当前行 (**queryCurrentRecords: SELECT * FROM %s WHERE %s FOR UPDATE**, L70 CHECK_SQL_TEMPLATE, PK 分批 L307-327)
  3. **after == current → 通过, 执行 undo** (L254-255)
  4. **before == current → 停止 undo** (L259-266, "no data change between the before data snapshot and the current data snapshot") — **已回滚过** (重复执行幂等)
  5. 其他 → **throw SQLUndoDirtyException "Has dirty records when undo"** (L279) → 上层映射 **BranchRollbackFailed_Unretriable** (AbstractUndoLogManager:432-439, 需人工校准)
- **开关**: IS_UNDO_DATA_VALIDATION_ENABLE 默认 **true** (L75-76, DefaultValues:241) — 关闭则跳过全部校验
- **PK 分批查询** (L292-338): buildWhereConditionListByPKs — IN 参数分组 (方言大小限制, Oracle IN 1000 面)
- **幂等面**: before==current 分支让**重复 rollback 安全** (服务端重试/多进程重放均无害)

## 代码类型
Implementation (一致性校验)

## 跨域关联
- S-7: Unretriable 分支状态 (dirty → 停重试)
- S-12: FOR UPDATE 查询在 undo 时也锁行 (防并发写)

## 结论
校验 = 三步比对决策 (跳过/通过/已回滚/dirty); dirty → Unretriable 人工介入; 可配置关闭。
源码位置: AbstractUndoExecutor.java:70-76,234-338; AbstractUndoLogManager.java:432-439
