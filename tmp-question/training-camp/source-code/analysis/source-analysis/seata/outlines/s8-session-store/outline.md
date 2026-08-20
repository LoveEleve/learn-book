# S-8 Session 存储 — 4 模式会话持久化

> 前置: [[S-1-AT两阶段]] (生命周期) + [[S-3-TC-Server]] (会话管理) + [[S-7-重试故障恢复]] (重启续跑) | 引出: [[S-12-全局锁体系]] | 对照: ZK 会话存储 (阶段4.3)
> 🔴 A | 8 KP | [模式: SPI 模式路由 + 生命周期映射 + 锁面]
> Pass 2 闭环: q1(模式路由) q2(生命周期) q3(存储实现) q4(锁面)

**读者处境**: TC 的会话状态存在哪? DB/FILE/REDIS/RAFT 怎么选? 这篇拆 SessionHolder (路由) + AbstractSessionManager (映射) + 存储三实现 + 锁面。

### 1. SessionMode 与加载路由 — 4 模式 SPI

场景: 存储怎么选?
源码路径:
- **SessionMode 4 值** (SessionMode:19-35): FILE/DB/REDIS/RAFT
- **SessionHolder.init** (L91-154): **EnhancedServiceLoader SPI 按模式加载** SessionManager + reload; ROOT_SESSION_MANAGER_NAME="root.data" (L71)
- **SessionManager 族**: DataBaseSessionManager / FileSessionManager / RaftSessionManager / RedisSessionManager
- **VGroupMappingStoreManager**: 事务组映射存储 (各模式实现)
- **reload 状态机** (SessionHolder:185-215): 终态收尾 (Rollbacked/Committed → end*) / **错误态 removeInErrorState** (Finished/UnKnown/CommitFailed/RollbackFailed/TimeoutRollbackFailed) / 重试态续跑 — S-7 重启续跑实证
关键设计 (q1): **模式路由 = SPI 加载 + reload 恢复**。[模式: SPI 路由]

### 2. 生命周期映射 — writeSession 6 操作 + 状态关联

场景: 生命周期怎么落库?
源码路径:
- **writeSession 6 操作** (AbstractSessionManager:173-195): GLOBAL_ADD/UPDATE/REMOVE + BRANCH_ADD/UPDATE/REMOVE; 失败 → **FailedWriteSession 异常族** (Global/Branch 区分, L173-195)
- **onBegin → GLOBAL_ADD** / **onStatusChange → GLOBAL_UPDATE** / **onSuccessEnd → GLOBAL_REMOVE** (L127-161)
- ⚠ **onClose → setActive(false)** (L154-156) — **S-1 isEndStatus 反直觉的根源** (active 标志源头)
- ⚠ **Rollbacking/TimeoutRollbacking → 分支锁标记 LockStatus.Rollbacking** (L85-87) — 状态联动锁 (S-12)
- **onFailEnd** (L163-171): **rollbackFailedUnlockEnable → clean (解锁)** / else 保留 (S-7 交叉)
关键设计 (q2): **生命周期 6 操作映射 + 状态联动锁标记**。[模式: 生命周期映射]

### 3. 存储实现 — DB 13 方言 + Redis Lua + File

场景: 三种存储怎么实现?
源码路径:
- **DB**: global_table/branch_table/lock_table 三表 + **13 方言 SQL** (LogStoreSqls/LockStoreSql: mysql/oracle/pg/sqlserver/dm/kingbase/mariadb/oceanbase/oscar/polardbx/h2...) + **GlobalTransactionDO 11 字段** (core/store); ⚠ **transactionName 静默截断** (columnSize 从表结构读 + substring, L210-211) + **每写独立事务** (setAutoCommit(true), L202)
- **Redis**: **RedisLuaTransactionStoreManager + RedisLuaLocker — Lua 脚本原子性** (防并发竞态); ⚠ **4 个 Lua 脚本** (acquire/release/update/lockable, L52-73) + **pipeline 模式** (L86); Lua (默认) vs Java 双实现可选
- **File**: FileTransactionStoreManager + TransactionWriteStore 序列化追加 + **FlushDiskMode 刷盘模式** + restoreSessions (未处理分支缓冲恢复 L195-210)
- **LockStore**: LockStoreDataBaseDAO — lock_table 行锁 (S-12)
关键设计 (q3): **DB 三表 13 方言 / Redis Lua 原子 / File 序列化刷盘**。[模式: 存储实现]

### 4. lockAndExecute 三模式 — 并发控制对比

场景: 会话并发怎么控制?
源码路径:
- **FILE** (FileSessionManager:184-193): **GlobalSessionLock: ReentrantLock + tryLock(2s)** (GlobalSession:831-850) — 超时 **FailedLockGlobalTransaction** — 单机 JVM 锁
- **DB** (DataBaseSessionManager:160-163): **直接 call** — 多节点共享存储, 本地锁无意义 — 靠存储条件更新 + 全局锁 (S-12)
- **RAFT**: distributedLockAndExecute (S-3)
- **DistributedLocker 族 4 实现**: DataBaseDistributedLocker / RedisDistributedLocker (Lua) / RaftDistributedLocker / FileLocker
- **GlobalSession 双锁**: globalSessionLock + resourceLock (L117-121) — 会话级 + 资源级分离

## 代码类型
Architecture (会话存储面)

## 负面空间 — Session 存储刻意不做的事

- **不做跨模式迁移**: 模式启动即定, 无热切换 (对照 ZK 会话迁移)
- **不做缓存层**: DB 模式每次读写库 (无内存缓存 — 靠连接池)
- **不做分库分表**: global_table 单表 (对照 ZK 无表概念)
- **不做本地锁集群化**: FILE 模式锁是 JVM 级 — 多节点共享 FILE 不安全 (单机语义)
- **不做消息队列**: 无异步写队列 (同步写库)
- **不做数据压缩**: 会话序列化原样存储 (对照 Z-9 快照压缩)

→ 引出: 全局锁体系 → [[S-12-全局锁体系]]
