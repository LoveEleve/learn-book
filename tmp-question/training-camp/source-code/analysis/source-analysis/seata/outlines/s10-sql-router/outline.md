# S-10 SQL 路由 — 识别与 executor 族

> 前置: [[S-4-DataSource代理]] (执行入口) + [[S-2-undo_log]] (镜像采集) | 对照: MyBatis 插件链 (阶段3.4) + Druid SQL 解析
> 🟡 B | 8 KP | [模式: 路由表 + SPI 方言 + 兜底]
> Pass 2 闭环: q1(路由表) q2(executor 族) q3(SQL 识别) q4(边界面)

**读者处境**: 一条 SQL 进来, Seata 怎么决定要不要管? 怎么选 executor? 这篇拆 ExecuteTemplate (186) + executor 族 + SQLType 48 值。

### 1. 路由表 — 守卫 + 6 分支 + Multi + Plain

场景: SQL 怎么路由?
源码路径:
- **入口守卫** (ExecuteTemplate:69-73): **!requireGlobalLock && AT != branchType → 直通原 statement** — 非 AT 零开销
- **路由表** (L100-168): INSERT → **InsertExecutor SPI 方言加载** / UPDATE → SqlServerUpdateExecutor vs UpdateExecutor / DELETE / SELECT_FOR_UPDATE (S-2 行锁; ⚠ SqlServer 变体) / **INSERT_ON_DUPLICATE_UPDATE + UPDATE_JOIN → 仅 MySQL/Mariadb/PolarDBX** (其他 NotSupportYet) / default → PlainExecutor
- **多识别器 → MultiExecutor** (L166-168, MULTI_UPDATE/MULTI_DELETE; ⚠ sqlserver 变体 L81-88; ⚠ **按 tableName 分组 + 仅 UPDATE/DELETE** — default UnsupportedOperationException L76-99); ⚠ **StatementProxy 三入口全经路由** (executeQuery/executeUpdate/execute, L63-77 — 无旁路)
- **异常包装** (L170-176): 非 SQLException → SQLException
关键设计 (q1): **守卫 + 6 类型分支 + Multi + Plain 兜底**。[模式: 路由表]

### 2. executor 族 — 基类链与方言覆盖

场景: executor 怎么组织?
源码路径:
- **基类链**: Executor → **AbstractDMLBaseExecutor** (S-4: autoCommit 双路径 + 镜像 + undo) → Insert/Update/Delete/SelectForUpdate
- **10 方言目录** (exec/): mysql (MySQLInsertExecutor/OnDuplicateUpdate/UpdateJoin) + oracle (OracleInsertExecutor + OracleJdbcType) + sqlserver (Update/Delete/SelectForUpdate/Multi 变体) + dm/kingbase/mariadb/oceanbase/oscar/polardbx/postgresql
- **InsertExecutor 双面**: 接口 + SPI 实现 (EnhancedServiceLoader 方言选择)
- **MultiUpdate/MultiDelete**: 批量语句 (sqlserver 变体, MultiExecutor:81-88)
关键设计 (q2): **基类公共 (镜像/undo) + 方言覆盖**。[模式: 基类+方言]

### 3. SQL 识别 — SQLVisitorFactory + SQLType 48 值

场景: SQL 类型怎么定?
源码路径:
- **SQLVisitorFactory**: **SQLRecognizerFactory SPI 加载** (sqlParserType 配置) → get(sql, dbType)
- **sqlparser 三模块**: core (接口) + **druid** + **antlr** 双解析器可切换
- **SQLType 48 值** (SQLType.java): 0-44 (SELECT/INSERT/UPDATE/DELETE/SELECT_FOR_UPDATE/REPLACE/TRUNCATE/SEQUENCE 族/**MULTI_DELETE(35)/MULTI_UPDATE(36)**/LOCK_TABLES...) + **101/102/103** (INSERT_IGNORE/**INSERT_ON_DUPLICATE_UPDATE(102)/UPDATE_JOIN(103)**)
- **SQLRecognizer 契约**: getSQLType/getTableName/getOriginalSQL
关键设计 (q3): **SPI 双解析器 + 48 值枚举**; 路由消费 6+2 类型。[模式: SPI 识别]

### 4. 边界面 — 守卫/异常/兜底

场景: 边界怎么处理?
源码路径:
- **守卫直通** (L69-73): 非 AT 无 GlobalLock → 原样执行 (零开销); ⚠ **SELECT 普通查询也 Plain** (只读无镜像 — 镜像只对 DML)
- **Plain 兜底** (L84-88,159-163): 未识别 SQL → 原样执行 (无镜像 — 安全面)
- **异常统一** (L170-176): SQLException 契约
- **NotSupportYet**: 2 类型仅 3 方言 (L137-139)
- **GlobalLock 场景**: requireGlobalLock → 走路由 (锁查询面 S-12)

## 代码类型
Architecture (SQL 路由面)

## 负面空间 — SQL 路由刻意不做的事

- **不做全部 SQLType 支持**: 48 值仅路由 6+2 类型 — 其他 Plain 原样 (无镜像无补偿)
- **不做 SQL 改写优化**: 镜像采集不改业务 SQL (对照 sharding 改写)
- **不做解析缓存**: SQLVisitorFactory 每次解析 (无 LRU — 性能面)
- **不做方言全支持**: INSERT_ON_DUPLICATE/UPDATE_JOIN 仅 3 方言 — 其他显式不支持
- **不做 DDL 拦截**: CREATE/ALTER 等 Plain 直通
- **不做 SQL 归一化**: 识别靠解析器 (druid/antlr) — 无归一化缓存

→ 引出: undo_log 可靠性 → [[S-11-undo_log可靠性]]
