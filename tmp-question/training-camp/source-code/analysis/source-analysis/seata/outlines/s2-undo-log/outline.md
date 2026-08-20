# S-2 undo_log 机制 — 镜像采集与反向 SQL

> 前置: [[S-1-AT两阶段]] (Phase2 rollback 消费面) | 引出: [[S-11-undo_log可靠性]] (压缩/子表) + [[S-12-全局锁体系]] (lockKey) | 对照: MySQL binlog 反向 / Redis AOF (阶段3.5)
> 🔴 A | 8 KP | [模式: 补偿日志 + 镜像对 + 反向执行]
> Pass 2 闭环: q1(生命周期) q2(反向 SQL) q3(镜像采集) q4(数据校验)

**读者处境**: Seata 怎么在回滚时把数据恢复原状? undo_log 何时写/何时读/怎么防脏数据? 这篇拆 AbstractUndoLogManager (生命周期) + AbstractUndoExecutor (反向执行) + MySQL 方言。

### 1. 生命周期 — flush/undo/删除 + 无限重试

场景: undo_log 怎么流转?
源码路径:
- **State 枚举** (AbstractUndoLogManager:62-82): **Normal(0) / GlobalFinished(1)** — log_status 两值
- **flushUndoLogs** (L266-303): BranchUndoLog (xid/branchId/sqlUndoLogs) → **parser.encode** → **压缩: enable && >64k → zip** (L571-573, 默认 zip/64k/开) → context (serializer/compressorType/**max_allowed_packet** — 大包预防面 L293-301) → insertUndoLogWithNormal; ⚠ **默认序列化器 = jackson** (DefaultValues:245) — 6 parser SPI (fastjson2/fastjson/jackson/protostuff/fury/kryo), context 记录 serializer → undo 按记录跨版本解码 (L358-361)
- **undo 主循环** (L315-466): **for(;;) 无限重试** (SQLIntegrityConstraintViolationException → continue) — 整个 undo 在本地事务 (autoCommit=false)
  - **SELECT ... FOR UPDATE 行锁** (L334-337) → **state != Normal → 忽略** (重复回滚防护, L343-352) → **sqlUndoLogs.size()>1 → reverse 逆序回滚** (L368-370) → per log: UndoExecutorFactory → executeOn
  - **exists → deleteUndoLog + commit** / **!exists → insertUndoLogWithGlobalFinished** (L399-416, **防 Phase1 后提交 — issue #489**, L394-397)
- **异常分类** (L419-445): 约束冲突 → 重试 / **SQLUndoDirtyException → Unretriable** (人工校准) / 其他 → **Retriable**
- **子表面** (L96-97,519-544): SUB_ID_KEY → getSubRollbackInfo 拼接 + 解压 (S-11)
关键设计 (q1): **无限重试 + 行锁 + GlobalFinished 防竞态**; 脏数据 Unretriable 停重试。[模式: 补偿生命周期]

### 2. 反向 SQL — 三模板 + 方言

场景: 回滚 SQL 怎么生成?
源码路径:
- **三模板** (MySQL 实证): DELETE→**INSERT (beforeImage 重建, PK 最后)** (MySQLUndoDeleteExecutor:51-82) / INSERT→**DELETE WHERE pk** (MySQLUndoInsertExecutor:42-76) / UPDATE→**UPDATE SET before 值 WHERE pk** (MySQLUndoUpdateExecutor:40-73); ⚠ **参数约定因操作类型而异**: InsertExecutor **覆盖 undoPrepare 只绑 PK** (L57-63 — DELETE 无需值参数)
- **executeOn** (AbstractUndoExecutor:114-148): 校验 → buildUndoSQL → per row: **非 PK 入参 + PK 恒在最后** (L127-131,210-217) → undoPrepare 特殊类型 (BLOB/CLOB/LONGVARBINARY/DATALINK/ARRAY, L158-218) → executeUpdate
- **方言 SPI**: UndoExecutorFactory → UndoExecutorHolderFactory → **13 方言目录**; MySQL JSON 专用 (MySQLJsonHelper.convertIfJson)
- **PK 顺序**: getOrderedPkList 按 tableMeta.getPrimaryKeyOnlyName (L351-366)
关键设计 (q2): **镜像反演三模板 × 方言**; PK 参数位约定统一。[模式: 反向 SQL]

### 3. 镜像采集 — before/after + 主键守卫

场景: 回滚依据怎么来?
源码路径:
- **beforeImage**: **SELECT ... FOR UPDATE** (SelectForUpdateExecutor:85-152) — 行锁先锁再查; ⚠ **锁重试**: LockRetryController (L83,115) — Phase1 全局锁冲突重试 (S-12 交叉)
- **afterImage**: 业务执行后 buildTableRecords 再查 (BaseTransactionalExecutor:527-530)
- **主键守卫**: **UPDATE 时 before.size != after.size → ShouldNeverHappenException "probably because you updated the primary keys"** (L408-411)
- **lockKey**: **DELETE ? beforeImage : afterImage** (L415) — 全局锁键 (S-12 交叉)
- **SQLUndoLog** (SQLUndoLog:27-124): sqlType/tableName/beforeImage/afterImage — setTableMeta 双镜像共享
关键设计 (q3): **行锁 + 双镜像 + 主键守卫**; lockKey 采集面。[模式: 镜像对]

### 4. 数据校验 — 三步比对决策

场景: 回滚前怎么防脏数据?
源码路径:
- **决策链** (AbstractUndoExecutor:234-283):
  1. **before == after → 跳过** (业务无实际变更)
  2. 查当前行 (CHECK_SQL: "SELECT * FROM %s WHERE %s FOR UPDATE", L70)
  3. **after == current → 执行 undo**
  4. **before == current → 跳过** (**已回滚过 — 重复 rollback 幂等**, L259-266)
  5. 其他 → **SQLUndoDirtyException → Unretriable** (人工校准, AbstractUndoLogManager:432-439)
- **开关**: IS_UNDO_DATA_VALIDATION_ENABLE 默认 **true** (L75-76) — 关闭跳过全部校验
- **PK 分批查询** (L292-338): buildWhereConditionListByPKs — IN 参数分组 (方言上限面); ⚠ **CHECK_SQL "TODO support multiple primary key" 注释过时** — 多列 PK 已完整支持 (L297-366); **比对语义**: tableName ignoreCase + 行数相等 + 字段级 (DataCompareUtils:57-71) + **fastjson 数值比较特例** (L73-82)
关键设计 (q4): **三步比对四结果决策**; dirty 显式失败交人工; before==current 幂等面。[模式: 校验决策]

## 代码类型
Implementation (补偿数据面)

## 负面空间 — undo_log 刻意不做的事

- **不做物理回滚**: 反向 SQL 是逻辑补偿 — 非 binlog 级物理恢复 (对照 MySQL binlog 反向 / Oracle flashback)
- **不做数据备份**: rollback_info 只存镜像对 — 大字段镜像体积爆炸 (压缩 64k 阈值缓解)
- **不做级联约束处理**: 反向执行依赖 FK 顺序 — 逆序回滚缓解
- **不做并发写保护**: 只靠 FOR UPDATE 行锁 + 校验 — 脏数据 → 人工介入 (无自动解决)
- **不做 undo_log 无限保留**: Phase2 后删除 / GlobalFinished 标记 — 孤儿清理面 (S-11)
- **不做跨方言 SQL 生成**: 每方言独立 executor — 新库需新实现 (SPI 13 方言)

→ 引出: 可靠性面 → [[S-11-undo_log可靠性]]
