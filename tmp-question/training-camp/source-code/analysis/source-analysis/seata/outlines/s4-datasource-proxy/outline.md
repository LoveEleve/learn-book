# S-4 DataSource 代理 — 代理链与提交拦截

> 前置: [[S-1-AT两阶段]] + [[S-2-undo_log]] (flush 消费面) | 引出: [[S-10-SQL路由]] + [[S-12-全局锁体系]] | 对照: MyBatis 插件链 / HikariCP 代理 (阶段3)
> 🔴 A | 8 KP | [模式: 三层代理 + 提交拦截 + 上下文]
> Pass 2 闭环: q1(代理链) q2(提交拦截) q3(连接上下文) q4(执行链)

**读者处境**: 业务拿到的 DataSource 是什么? 本地 commit 为什么能触发全局事务的分支注册? 这篇拆 DataSourceProxy (455) + ConnectionProxy (393) + ConnectionContext (416)。

### 1. 代理链 — DataSourceProxy 构造与注册

场景: 代理怎么装配?
源码路径:
- **嵌套解包** (DataSourceProxy:95-100): SeataDataSourceProxy → getTargetDataSource — **多层代理剥洋葱**
- **init** (L105-130): dbType (JdbcUtils) → 方言特判 (**polardb-x 检测** L136-158) → **checkUndoLogTableExist fast-fail** (L167-183: 无 undo_log 表 → IllegalStateException)
- **initResourceId** (L236-252): **7 方言分支** URL 解析 — 集群唯一标识
- **注册面** (L126-129): DefaultResourceManager.registerResource + TableMetaCache + **RootContext.setDefaultBranchType(AT)**
- **getConnection** (L212-221): 包装 ConnectionProxy; **getPlainConnection 绕过代理** (L198-200)
关键设计 (q1): **构造期完整自检 (方言/undo 表/资源注册) + 嵌套解包**。[模式: 三层代理]

### 2. 提交拦截 — doCommit 三分支 + register 守卫

场景: 本地提交怎么挂钩全局事务?
源码路径:
- **commit** (ConnectionProxy:184-199): **LockRetryPolicy.execute(doCommit)** → 失败且非 autoCommitChanged → **补 rollback 防半提交** (L191-195)
- **doCommit 三分支** (L227-235): **inGlobalTransaction → processGlobalTransactionCommit** / **isGlobalLockRequire → 全局锁本地提交** (checkLock→commit) / else 直通
- **processGlobalTransactionCommit** (L247-265): **register → flushUndoLogs (S-2) → targetConnection.commit** → 失败 report(false) / 成功 **IS_REPORT_SUCCESS_ENABLE (默认 false) 才 report(true)**; report 有 **branchId 守卫** (L312-314); ⚠ **AsyncWorker 交叉**: 2 线程 1s 周期异步提交 + **1000 分片批删** (S-11)
- **register 守卫** (L267-281): **!hasUndoLog || !hasLockKey → 不注册分支** — 只读事务无分支 → branchRegister → setBranchId
- **report** (L311-336): REPORT_RETRY_COUNT=**5**; PhaseOne_Done/Failed
- **setAutoCommit 拦截** (L303-309): 全局事务中 false→true → **先 doCommit** (JDBC 规范); changeAutoCommit 标记
关键设计 (q2): **提交 = 分支注册 (有写才注册) + undo flush + 本地提交 + report**。[模式: 提交拦截]

### 3. 连接上下文 — ConnectionContext 全 API

场景: 事务状态存在哪?
源码路径:
- **结构** (ConnectionContext 416): xid / branchId / **undoItems** / **lockKeys** / savepoints / autoCommitChanged / **globalLockRequire** (@GlobalLock 场景 — 无全局事务但要求全局锁, L94-105)
- **bind / inGlobalTransaction** (L176-185): xid 绑定判定
- **appendUndoItem / appendLockKey** (S-2): 写操作后追加
- **buildLockKeys** (L344): 锁键串拼接 (S-12)
- **hasUndoLog / hasLockKey** (L213-222): register 守卫依据
- **reset** (L319): 提交/回滚后**全清** — 连接归还前归零
- **savepoint 面** (L125-): append/remove/release — 内嵌事务支持
关键设计 (q3): **连接级事务状态容器 + reset 归零**。[模式: 上下文]

### 4. 执行链与锁重试 — doExecute 双路径 + LockRetryPolicy

场景: 写操作怎么进入 Seata 管线?
源码路径:
- **doExecute** (AbstractDMLBaseExecutor:81-102): **autoCommit=true → executeAutoCommitTrue** / false → beforeImage → 业务 → afterImage → **prepareUndoLog** (S-2); ⚠ **单语句 autoCommit = 隐式全局事务** (L146-168): changeAutoCommit → execute+commit → finally reset+setAutoCommit(true) — 自包含提交链; **prepareStatement PK 数组识别** (AbstractConnectionProxy:107-129)
- **executeAutoCommitTrue** (L146-168): LockRetryPolicy.execute 包裹 — 单语句也过锁重试; ⚠ **LockRetryController 数学**: **第 N+1 次抛 LockWaitTimeout** / FailFast 立即抛; **GlobalLockConfig 优先** (S-1 交叉)
- **LockRetryPolicy** (ConnectionProxy:338-392): **branchRollbackOnConflict 默认 true** — true && autoCommitChanged → 直通 (重试在 execute 层); else **doRetryOnLockConflict** (LockRetryController 循环); **FailFast 降级** (L372-376: "local lock is released")
- **onException 钩子** (L391): 子类扩展

## 代码类型
Architecture (RM 入口面)

## 负面空间 — DataSource 代理刻意不做的事

- **不做连接池**: 代理只包装不管理 (对照 HikariCP 池化)
- **不做 SQL 改写**: 只是执行入口 — 改写/镜像在 exec/ (S-10)
- **不做自动建表**: undo_log 表 fast-fail 要求预建 (脚本/手动)
- **不做读写分离路由**: 单数据源透明代理
- **不做连接级缓存**: 每次 getConnection 新代理 (连接池外借)
- **不做嵌套事务支持**: savepoint 上下文记录但无内嵌事务语义 (对照 REQUIRES_NEW 连接切换)

→ 引出: SQL 路由细化 → [[S-10-SQL路由]]
