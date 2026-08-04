# Seata 源码学习范围规划

> **版本**: v2.5.0
> **仓库**: `/data/workspace/source-code/code/spring/seata/`
> **规模**: 36 个子模块（15 个含 src/main/java），1557 个源文件
> **原始日期**: 2026-08-03
> **深度 review R1**: 2026-08-04 — 逐类方法体 + undo_log DDL + 模块审计，发现 5 遗漏域 + 4 描述修正
> **深度 review R2**: 2026-08-04 — exec/undo/async/lock 层深度探索，新增 2 域 (9→11)
> **深度 review R3**: 2026-08-04 — LockManager 全局锁体系 + Phase2 分支通知，新增 2 域 (11→13)，最终收敛

---

## 一、仓库概况

Seata 是 Apache 开源的分布式事务框架，核心是 **AT 模式（自动补偿）**——通过 `undo_log` 表记录数据快照实现自动回滚。

**架构分层**：**TM(Transaction Manager 事务发起方) → TC(Transaction Coordinator Server 事务协调器) → RM(Resource Manager 各微服务数据源)**

**核心源码模块**（15 个含 src/main/java）：

| 模块 | 职责 | 关键类 | 纳入 |
|:---:|---|---|:---:|
| `server/` | TC 服务端——全局事务协调、Session 管理 | DefaultCoordinator, DefaultCore, SessionHolder, GlobalSession | ✅ |
| `rm-datasource/` | RM AT 模式——DataSource 代理、undo_log 生成 | DataSourceProxy, ConnectionProxy, PreparedStatementProxy, UndoLogManager | ✅ |
| `tm/` | TM——GlobalTransaction 接口与模板 | GlobalTransaction(interface), TransactionalTemplate | ✅ |
| `core/` | 协议层——RPC 编解码、消息模型 | RpcContext, BranchType, GlobalStatus | ✅ |
| `integration-tx-api/` | Spring 集成——@GlobalTransactional 拦截器 | GlobalTransactionalInterceptorHandler | 🟡 新增 |
| `common/` | 公共工具——线程、配置、SPI 加载 | EnhancedServiceLoader, ConfigurationFactory | 融入各域 |
| `spring/` + `seata-spring-boot-starter/` | Spring Boot 自动配置 | GlobalTransactionScanner | 🟡 新增 |
| `rm/` | RM 抽象——分支类型路由(BranchType→AbstractCore) | DefaultResourceManager, AbstractRMHandler | 🟡 新增 |
| `rocketmq/` | RocketMQ 消息集成 | — | 淘汰 |
| `tcc/` `saga/` `compatible/` `console/` `namingserver/` `mock-server/` | 其他模式/控制台/兼容层/测试 | — | 淘汰 |

---

## 二、知识域规划

### 🔴 核心域（4 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| S-1 | **AT 模式两阶段提交** | TransactionalTemplate, DefaultCoordinator, DefaultCore | **Phase 1(执行业务+注册+记录undo)**：`@GlobalTransactional` → `TransactionalTemplate.execute()` → 传播行为判断 → `tm.begin(xid)` 向 TC 注册全局事务 → `executeBusiness()` 各 RM 执行业务 SQL → `DataSourceProxy` 拦截 → 执行前生成 `undo_log`(beforeImage) → 执行 SQL → 生成 afterImage → `RM.branchRegister()` 向 TC 注册分支事务(branchId)。**Phase 2a(提交)**：业务成功 → `TransactionalTemplate.commitTransaction()` → 先 `isTimeout()` 客户端超时检测 → 触发 `beforeCommit` hook → `TM.commit(xid)` → TC 端 `globalSession.close()` 先关闭 session 禁止新分支注册 → `canBeCommittedAsync()` 判断异步还是同步提交 → TC 通知 RM 删除 undo_log。**Phase 2b(回滚)**：业务失败 → `TransactionalTemplate.completeTransactionAfterThrowing()` → `txInfo.rollbackOn(ex)` 判断是否回滚 → 触发 `beforeRollback` hook → `TM.rollback(xid)` → TC 端同样 `globalSession.close()` → 状态改为 `Rollbacking` → TC 通知 RM → RM 读取 undo_log 生成反向 SQL → 删除 undo_log |
| S-2 | **undo_log 机制** | UndoLogManager(interface), ConnectionProxy, SQLUndoLog | **undo_log DDL**(源码验证 `script/client/at/db/mysql.sql`)：`branch_id(BIGINT) + xid(VARCHAR(128)) + context(VARCHAR(128)) + rollback_info(LONGBLOB) + log_status(INT) + log_created/mofified(DATETIME(6))`，**UNIQUE KEY**(`xid`, `branch_id`) 确保一分支一记录。**beforeImage**：`SELECT * FROM t WHERE pk=? FOR UPDATE` 锁行 → 记录快照；**afterImage**：执行后立即 `SELECT * WHERE pk=?` 记录新值；**反向SQL**：UPDATE → 用 beforeImage 恢复、INSERT → DELETE、DELETE → INSERT 还原 |
| S-3 | **TC Server 协调** | DefaultCoordinator, DefaultCore, SessionHolder, GlobalSession | **GlobalSession 字段**(源码验证)：xid / transactionId / status(GlobalStatus 枚举) / applicationId / transactionServiceGroup / transactionName / timeout / beginTime / branchSessions(List\<BranchSession\>) / globalSessionLock(并发控制 ReentrantLock) / lifecycleListeners。**6 个定时线程池**（DefaultCoordinator 构造函数创建）：①`retryRollbacking` — 重试失败回滚 ②`retryCommitting` — 重试失败提交 ③`asyncCommitting` — 异步提交 ④`timeoutCheck` — 超时检测(不是 ServerMessageListener！) ⑤`undoLogDelete` — undo_log 清理(延迟 3 分钟) ⑥`syncProcessing` — 数据同步。**5 种 GlobalStatus 状态组**：rollbackingStatuses / committingStatuses / retryRollbackingStatuses / retryCommittingStatuses / endStatuses。**RPC 方法**(DefaultCoordinator 中 6+ 个)：doGlobalBegin / doGlobalCommit / doGlobalRollback / doBranchRegister / doGlobalStatus / doGlobalReport / doBranchReport |
| S-4 | **DataSource 代理** | DataSourceProxy, ConnectionProxy, PreparedStatementProxy | **代理链**：`DataSourceProxy` → `init()` 时检查 undo_log 表是否存在 + `DefaultResourceManager.registerResource(this)` 注册 + `TableMetaCacheFactory.registerTableMeta(this)` 缓存表结构 → `getConnection()` → 返回 `ConnectionProxy` → `PreparedStatementProxy` 拦截 `executeUpdate()/executeQuery()` → 生成 undo_log → 执行原始 SQL。**全局锁**：Phase1 对修改的数据行加 `SELECT FOR UPDATE` 全局锁，防止其他事务在 Phase2 之间修改同一行。**BranchType 路由**：RM 侧 `DefaultResourceManager` 通过 `CORE_MAP` 按 BranchType(AT/TCC/SAGA/XA) 路由到不同 AbstractCore 实现 |

### 🟡 扩展域（5 个 — 源码验证新发现）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| S-5 | **事务传播机制** | TransactionalTemplate, TransactionInfo, Propagation | **6 种传播行为**（对标 Spring @Transactional）：①`REQUIRED` — 有则加入/无则新建 ②`REQUIRES_NEW` — 挂起当前→新建 ③`NOT_SUPPORTED` — 挂起当前→无事务执行 ④`SUPPORTS` — 有则加入/无则无事务 ⑤`NEVER` — 有事务抛异常 ⑥`MANDATORY` — 无事务抛异常。**实现**：`TransactionalTemplate.execute()` 的 `switch(propagation)` 分支 + `tx.suspend()/resume()` 挂起恢复机制。**超时**：客户端侧 `isTimeout(beginTime, txInfo.getTimeOut())` 检测（单位：毫秒） |
| S-6 | **TransactionHook 钩子** | TransactionHook, TransactionHookManager | **7 个生命周期钩子**：`beforeBegin() → afterBegin() → [执行业务]` → 成功: `beforeCommit() → afterCommit()` / 失败: `beforeRollback() → afterRollback()` → `afterCompletion()`(无论成败)。Seata 的 Hook 机制类比 Spring 的 `@TransactionalEventListener`，可做事务埋点/指标采集 |
| S-7 | **重试与故障恢复** | DefaultCoordinator, GlobalSession | **RETRY_DEAD_THRESHOLD**：`currentTime - createTime >= RETRY_DEAD_THRESHOLD` → 判定事务为 retry rollback；另外 `END_STATE_RETRY_DEAD_THRESHOLD` 用于终态事务的超时 retry。**重试配置**：MAX_COMMIT_RETRY_TIMEOUT / MAX_ROLLBACK_RETRY_TIMEOUT / RETRY_DEAD_THRESHOLD(默认 2min)。**解锁机制**：ROLLBACK_FAILED_UNLOCK_ENABLE — 回滚失败后是否释放全局锁。**Branch 异步移除**：`branchRemoveExecutor` 线程池通过 `enableBranchAsyncRemove` 配置控制 |
| S-8 | **Session 存储模式** | SessionHolder, SessionManager(SPI) | **4 种存储后端**：①`SessionMode.DB`(默认，MySQL) ②`SessionMode.FILE` ③`SessionMode.REDIS` ④`SessionMode.RAFT`(集群共识)。`SessionHolder` 通过 `EnhancedServiceLoader` SPI 加载 SessionManager 实现。**并发控制**：`lockAndExecute(globalSession, callback)` — 加锁后执行操作 |
| S-9 | **Spring 集成与拦截器** | GlobalTransactionScanner, GlobalTransactionalInterceptorHandler | **@GlobalTransactional** → `GlobalTransactionalInterceptorParser` 解析注解 → `GlobalTransactionalInterceptorHandler` 拦截执行 → 构造 `TransactionInfo`(timeout/name/propagation/lockConfig) → 委托 `TransactionalTemplate.execute()`。**对标 Spring @Transactional**：Seata 在层面之上增加了全局事务协调(xid 传递/多 RM 协同) |
| S-10 | **SQL 路由与多方言执行** | ExecuteTemplate, SQLVisitorFactory, Executor(SPI) | **7 种 SQL 类型路由**：ExecuteTemplate.execute() 中 switch → ①`INSERT`(SPI 加载 InsertExecutor) ②`UPDATE`(db-specific) ③`DELETE` ④`SELECT_FOR_UPDATE` ⑤`INSERT_ON_DUPLICATE_UPDATE`(MySQL/MariaDB/PolarDBX) ⑥`UPDATE_JOIN`(MySQL/MariaDB/PolarDBX) ⑦`PLAIN`(透传)。**12+ 数据库方言**：rm-datasource 下 12 个 DB 子包(mysql/mariadb/postgresql/oracle/sqlserver/dm/kingbase/oscar/oceanbase/polardbx)，InsertExecutor 通过 SPI 按 dbType 加载对应实现。**非 AT 模式短路**：`!RootContext.requireGlobalLock() && BranchType.AT != RootContext.getBranchType()` → 直接透传原始 Statement |
| S-11 | **undo_log 可靠性机制** | AbstractUndoLogManager, LockRetryController, AsyncWorker | **4 层可靠性保障**：①**压缩** — `ROLLBACK_INFO_COMPRESS_THRESHOLD` 超过阈值自动压缩(CompressorType)，解决大事务 beforeImage 超 max_allowed_packet 问题 ②**子表拆分** — `UNDO_LOG_SUB_TABLE` + `getSubRollbackInfo()` 支持超长 rollback_info 自动分表 ③**无限重试** — `for(;;)` 循环 + SQLIntegrityConstraintViolationException 并发回滚保护 → 一个分支被两个进程同时回滚时，先成功的插入 GlobalFinished 标记，后到的发现唯一约束冲突→重试→发现 GlobalFinished→跳过 ④**异步提交** — AsyncWorker 通过 LinkedBlockingQueue + 1s 定时 flush，队列满时 CompletableFuture 紧急 flush。**LockRetryController 重试**：configurable retry interval + times + LockConflictFailFast 快速失败模式 |
| S-12 | **全局锁体系(LockManager)** | AbstractLockManager, LockManager, Locker, DataBaseLocker | **lockKey 格式**：`table1:pk1,pk2;table2:pk3` — 分号分隔表，冒号分隔表名:PK，逗号分隔多PK。**lock_table DDL**(源码验证 `script/server/db/mysql.sql`)：`row_key(PK) + xid + transaction_id + branch_id + resource_id + table_name + pk + status(0锁定/1回滚中) + gmt_create/modified`。**加锁流程**：`collectRowLocks()` 解析 lockKey → INSERT INTO lock_table → 唯一约束冲突→占用锁→回滚。**查锁**：SELECT WHERE row_key IN(...) → 被其他 xid 占用→canLock=false → 若 status==Rollbacking → LockKeyConflictFailFast 快速失败。**解锁**：DELETE WHERE xid=? AND row_key IN(...)。**6 种 Locker 实现**：DataBaseLocker(MySQL) / FileLocker / RedisLocker / RedisLuaLocker(原子Lua) / DataBaseDistributedLocker / RaftDistributedLocker |
| S-13 | **Phase2 分支通知机制** | DefaultCore, AbstractCore, ATCore | **DefaultCore.doGlobalCommit()**：遍历 `getSortedBranches()` 正向 → 跳过 `canBeCommittedAsync()` 分支(非retry) → 跳过 `PhaseOne_Failed` 分支(移除) → 跳过 XA `PhaseOne_RDONLY` → `getCore(branchType).branchCommit()` 按 BranchType 多态分发(AT→ATCore, TCC→TCCCore, ...) → TC 通过 Netty RPC 通知 RM → RM 返回 `BranchStatus.PhaseTwo_Committed` → 移除分支。**DefaultCore.doGlobalRollback()**：遍历 `getReverseSortedBranches()` **反向** → 相同跳过逻辑 → `branchRollback()` → `PhaseTwo_Rollbacked` 移除 → `PhaseTwo_RollbackFailed_Unretryable` 停止 retry。**XA XAER_NOTA 保护**：`isXaerNotaTimeout()` 判断 XA 超时→强制 PhaseTwo_Committed/Rollbacked |

---

## 三、淘汰清单（15 子模块全覆盖）

| 模块 | 子模块 | 理由 |
|:---:|---|---|
| `tcc/` `saga/` | — | 聚焦 AT 模式——最常用，TCC(侵入式)/Saga(长事务)为不同场景 |
| `config/` `discovery/` | — | 配置中心/注册中心适配——面试不问，架构层 |
| `console/` | — | 管理控制台 Web UI |
| `serializer/` `compressor/` | — | 序列化/压缩算法实现 |
| `sqlparser/` | — | SQL 解析器——底层实现，druid/hikaricp 偏 SQL 层级 |
| `rocketmq/` | — | RocketMQ 消息集成——非核心事务逻辑 |
| `compatible/` | — | io.seata 旧包名兼容层 |
| `mock-server/` | — | 测试用 mock |
| `namingserver/` | — | 命名服务——独立部署组件 |
| `build/` `style/` `changes/` `dependencies/` `all/` `bom/` `distribution/` `ext/` `metrics/` `test/` `test-old-version/` | — | 构建/测试/发布相关 |

---

## 四、统计

| 类别 | 数量 | 变化 |
|:---:|:---:|:---:|
| 🔴 核心域 | 4 | 原文 4，描述修正 |
| 🟡 扩展域 | 9 | 原 **0** → R1+5 → **R2+2 → R3+2** |
| **总域** | **13** | 原 4 → R1 9 → R2 11 → **R3 13** |

---

## 五、Review 发现汇总

### 描述修正（4 处）

| # | 原文 | 实测 | 文件/行 |
|:---:|---|---|---|
| 1 | "TM 调用 begin/commit/rollback" | `TransactionalTemplate` 是核心执行模板，TM 只是接口角色——模板中还包含传播判断、超时检测、Hook 触发 | `TransactionalTemplate.java:53-153` |
| 2 | "Phase2a TC 通知所有 RM 异步删除 undo_log" | DefaultCore.commit() 先调 `globalSession.close()` 禁止新分支注册 → 再 `canBeCommittedAsync()` 按分支类型判断异步/同步 → 异步提交有 `asyncCommitting` 线程池 retry | `DefaultCore.java:237-257` |
| 3 | "超时检测：ServerMessageListener 扫描" | 实际是 `DefaultCoordinator.timeoutCheck` ScheduledThreadPoolExecutor，且客户端侧 `TransactionalTemplate.isTimeout()` 也会检测 | `DefaultCoordinator.java:191-192` + `TransactionalTemplate.java:162-165` |
| 4 | "4 个核心 RPC" | DefaultCoordinator 至少 6 个 RPC handler: doGlobalBegin/Commit/Rollback/BranchRegister/GlobalStatus/GlobalReport + BranchReport/LockQuery/UndoLogDelete | `DefaultCoordinator.java:321-380` |

### 新增域 — Round 1（5 个）

| 域 | 发现来源 | 关键证据 |
|:---:|---|---|
| S-5 事务传播 | `TransactionalTemplate.execute()` 中 switch 6 种 Propagation | `TransactionalTemplate.java:66-113` |
| S-6 TransactionHook | 7 个 trigger 方法(beforeBegin 等) | `TransactionalTemplate.java:321-391` |
| S-7 重试与故障恢复 | 6 个 ScheduledThreadPoolExecutor + RETRY_DEAD_THRESHOLD | `DefaultCoordinator.java:182-198` |
| S-8 Session 存储模式 | SessionMode(DB/FILE/REDIS/RAFT) + SPI | `SessionHolder.java` |
| S-9 Spring 集成 | @GlobalTransactional + GlobalTransactionScanner | `spring/` + `integration-tx-api/` |

### 新增域 — Round 2（深度探索 exec/undo/async/lock 层，2 个）

| 域 | 发现来源 | 关键证据 |
|:---:|---|---|
| S-10 SQL 路由与多方言 | ExecuteTemplate.execute() SQL type switch(7 种) + 12 个 DB 子包 + SPI InsertExecutor | `ExecuteTemplate.java:99-169`, `rm-datasource/` 12 db sub-packages |
| S-11 undo_log 可靠性 | AbstractUndoLogManager.undo() 的 for(;;) 无限重试 + 压缩 + 子表 + GlobalFinished 标记 + AsyncWorker 异步提交 | `AbstractUndoLogManager.java:undo()`, `AsyncWorker.java:78-97`, `LockRetryController.java` |

### 新增域 — Round 3（LockManager + Phase2 分支通知，2 个）

| 域 | 发现来源 | 关键证据 |
|:---:|---|---|
| S-12 全局锁体系 | AbstractLockManager.collectRowLocks() + lock_table DDL + DataBaseLocker/LockStoreDataBaseDAO + 6 种 Locker | `AbstractLockManager.java:158-191`, `script/server/db/mysql.sql:lock_table`, 6 Locker impl files |
| S-13 Phase2 分支通知 | DefaultCore.doGlobalCommit/doGlobalRollback → getCore(branchType) 多态 → branchCommit/branchRollback → Netty RPC | `DefaultCore.java` doGlobalCommit/doGlobalRollback |

### 最终收敛结论

经过 **3 轮深度探索**（module audit → exec/undo/async → lock/Phase2），所有核心模块已完成源码级审查：
- ✅ `tm/`：GlobalTransaction(interface) + TransactionalTemplate(6 propagation + 7 hooks)
- ✅ `server/`：DefaultCoordinator(6 thread pools) + DefaultCore(doGlobalCommit/Rollback branch遍历) + SessionHolder(4 backends) + GlobalSession(ReentrantLock)
- ✅ `rm-datasource/`：ExecuteTemplate(7 SQL types + 12 DBs) + AbstractUndoLogManager(for(;;) retry + compress + sub-table) + AsyncWorker(queue-based)
- ✅ `server/lock/`：AbstractLockManager(RowLock collecting) + DataBaseLocker(lock_table CRUD)
- ⚠️ **server/cluster/raft/** 未深入 — Raft 选举/snapshot/sync 为集群运维话题，对单机 AT 模式学习价值有限
- ⚠️ **server/storage/** 未详读 — 已在 S-8(SessionMode) 覆盖抽象接口，具体 JDBC/Redis 实现增量有限

