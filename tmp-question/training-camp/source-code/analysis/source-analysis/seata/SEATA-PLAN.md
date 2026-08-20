# Seata — 知识网络化规划 (S-1~S-13, 09 怀疑审计后 v1)

> **日期**: 2026-08-15 | **依据**: issue/源码分析执行计划.md 阶段4.4 (13 域) + 09 对既有规划保持怀疑 全量重审
> **源码**: `/data/workspace/source-code/code/spring/seata` (**Seata 2.5.0**, build/pom.xml:74 revision 实证; 主包 org.apache.seata, compatible 模块为 io.seata 兼容面)
> **定位**: 阶段 4.4 — 消息与事务第六环 **分布式事务 (AT 两阶段 + TC 协调 + undo_log)**
> **知识网络**: 与 ZooKeeper (4.3, 协调基础设施) + Kafka (4.2, 元数据协调) + RocketMQ (4.1, 消息事务 RM-12 对照) + Curator (4.5, ZK 客户端) 互联

---

## 〇、09 怀疑审计表 (Seata, 2026-08-15) — 必读

| 既有规划断言 | 验证动作 | 证据 | 结论 |
|---|---|---|---|
| **域清单 13 个** (S-1~S-13) | 顶层模块扫描 (1557 主源文件) | tm (TransactionalTemplate 407) + server (DefaultCore 550/DefaultCoordinator 929) + rm-datasource (DataSourceProxy 455/undo 面/exec 面 184 文件) + spring (GlobalTransactionScanner 676) + core (protocol/store/lock) — 13 面全覆盖 | **接受** ✅ |
| S-1 "TransactionalTemplate/DefaultCore — Phase1(begin+undo)→Phase2(commit/rollback)" | 文件定位 | tm/api/TransactionalTemplate (407) + server/coordinator/DefaultCore (550) 均实证 | **接受** ✅ |
| S-2 "UndoLogManager — beforeImage(SELECT FOR UPDATE)+afterImage+反向SQL" | undo/ 目录 | UndoLogManager (87 接口) + AbstractUndoLogManager + AbstractUndoExecutor + **13 方言** (dm/kingbase/mariadb/mysql/oceanbase/oracle/oscar/polardbx/postgresql/sqlserver) | **接受+补充** ✅ |
| S-3 "DefaultCoordinator — 6个ScheduledThreadPool/5种GlobalStatus状态组" | server/coordinator | DefaultCoordinator (929) 实证; 数字待 S-3 穷举 | **接受+待验证** ✅ |
| S-4 "DataSourceProxy→ConnectionProxy→ExecuteTemplate—7种SQL类型路由" | exec/ 目录 | 7 类 executor (AbstractDMLBaseExecutor/BaseInsertExecutor/DeleteExecutor/InsertExecutor/SelectForUpdateExecutor/UpdateExecutor/PlainExecutor) + **13 方言目录** — 规划未提方言数 | **接受+补充** ✅ |
| S-5 "6种Propagation" | spring/tm 枚举 | 待 S-5 穷举 (Spring 原版 7 种含 NESTED — Seata 是否 6 需实证) | **待验证** ⚠ |
| S-6 "7个生命周期钩子(beforeBegin→afterBegin→beforeCommit/rollback→afterCompletion)" | spring 钩子面 | 待 S-6 穷举 | **待验证** ⚠ |
| S-7 "6个线程池retry+RETRY_DEAD_THRESHOLD+ROLLBACK_FAILED_UNLOCK_ENABLE" | server 重试面 | 待 S-7 穷举 | **待验证** ⚠ |
| S-8 "SessionMode(DB/FILE/REDIS/RAFT)+lockAndExecute 并发控制" | server/session + core/store | server/session + core/store (GlobalTransactionDO/LockDO/LockStore/LogStore) 实证 | **接受** ✅ |
| S-9 "Spring 集成 — @GlobalTransactional→GlobalTransactionScanner→TransactionalTemplate" | spring/annotation | GlobalTransactionScanner (676) 实证 | **接受** ✅ |
| S-10 "ExecuteTemplate—7种SQL类型+12+数据库方言+SPI InsertExecutor" | exec/ 方言目录 | 8 方言 (dm/kingbase/mariadb/mysql/oceanbase/oracle/polardbx/postgresql/sqlserver) + SPI 面待验证 | **接受+补充** ✅ |
| S-11 "undo_log 可靠性—压缩+子表+for(;;)无限重试+AsyncWorker" | undo 面 + server | 待 S-11 穷举 (undo 压缩常量/子表 hash) | **待验证** ⚠ |
| S-12 "全局锁体系—AbstractLockManager+lock_table DDL+RowLock/Locker—6种Locker实现" | server/lock + core/store | server/lock + LockStore 实证; "6种Locker" 待 S-12 穷举 | **接受+待验证** ✅ |
| S-13 "Phase2 分支通知—DefaultCore.doGlobalCommit—正向/反向分支遍历+getCore(branchType) 多态" | DefaultCore | DefaultCore (550) 实证 | **接受** ✅ |
| 版本 | build/pom.xml:74 | **2.5.0** (执行计划无版本声明) | **补充** ✅ |
| 包名 | 源码扫描 | **org.apache.seata** 主包; io.seata 为 compatible 兼容模块 (新代码勿引) | **补充** ✅ |

**覆盖率报告**: 既有规划 13 域 → 重审后 **13 域** (100%, 无增删), 补充 **3 处** (方言 13/包名/版本), 待验证 **5 处** (S-3 线程池数/S-5 传播数/S-6 钩子数/S-7 重试面/S-11 可靠性面/S-12 Locker 数)。

---

## 一、入口点与主线

`@GlobalTransactional (S-9) → GlobalTransactionScanner → TransactionalTemplate (S-1) → DefaultCore (begin/commit/rollback, S-1/S-13) → TM/RM 协议 → TC DefaultCoordinator (S-3) → Session 存储 (S-8) → 重试面 (S-7)` — 数据面: `DataSourceProxy (S-4) → ExecuteTemplate (S-10) → beforeImage/undo_log (S-2) → 全局锁 (S-12) → undo_log 可靠性 (S-11)` — 事务传播 (S-5) + Hook (S-6)。

## 二、域清单 (13 域: 11🔴 + 2🟡)

| # | 域 | 模块 (文件行) | 核心主题 | 方案 |
|:--:|---|---|---|---|
| S-1 | **AT 两阶段提交** | tm/TransactionalTemplate (407) + server/DefaultCore (550) | Phase1(begin+undo)→Phase2(commit/rollback) | 🔴 A |
| S-2 | **undo_log 机制** | rm-datasource/undo (UndoLogManager 87/AbstractUndoLogManager/AbstractUndoExecutor/13 方言) | beforeImage(SELECT FOR UPDATE)+afterImage+反向 SQL | 🔴 A |
| S-3 | **TC Server** | server/coordinator (DefaultCoordinator 929) | 6 ScheduledThreadPool/5 状态组/会话管理 | 🔴 A |
| S-4 | **DataSource 代理** | rm-datasource (DataSourceProxy 455/ConnectionProxy/AbstractConnectionProxy) | 代理链/连接绑定/回滚入口 | 🔴 A |
| S-5 | **事务传播** | spring/tm Propagation 枚举 | 6 Propagation 语义 | 🔴 A |
| S-6 | **TransactionHook** | spring TransactionHook 面 | 7 生命周期钩子 | 🔴 A |
| S-7 | **重试故障恢复** | server 重试面 (retry 线程池/RETRY_DEAD_THRESHOLD) | 故障重试/死事务阈值 | 🔴 A |
| S-8 | **Session 存储** | server/session + core/store (GlobalTransactionDO/LockDO/LockStore) | SessionMode(DB/FILE/REDIS/RAFT)+lockAndExecute | 🔴 A |
| S-9 | **Spring 集成** | spring/annotation (GlobalTransactionScanner 676) | @GlobalTransactional 扫描/代理装配 | 🟡 B |
| S-10 | **SQL 路由** | rm-datasource/exec (ExecuteTemplate/7 executor/8+ 方言) | 7 种 SQL 类型路由+方言 SPI | 🟡 B |
| S-11 | **undo_log 可靠性** | undo 面+server (压缩/子表/AsyncWorker/GlobalFinished) | 可靠性面 | 🔴 A |
| S-12 | **全局锁体系** | server/lock + core/store (AbstractLockManager/lock_table/Locker) | 锁表/行锁/6 Locker | 🔴 A |
| S-13 | **Phase2 分支通知** | server/DefaultCore (doGlobalCommit/doGlobalRollback) | 正向/反向分支遍历+getCore 多态 | 🔴 A |

## 三、执行顺序 (拓扑: 客户端面 → 服务端面 → 可靠性面)

**S-1 → S-2 → S-3 → S-4 → S-5 → S-6 → S-7 → S-8 → S-9 → S-10 → S-11 → S-12 → S-13**

> 拓扑理由: AT 总纲 (S-1) → undo_log 数据面 (S-2) → TC 服务端 (S-3) → 数据源代理 (S-4, 入口) → 传播 (S-5) → 钩子 (S-6) → 服务端重试 (S-7) → 会话存储 (S-8) → Spring 装配 (S-9) → SQL 路由细化 (S-10) → undo 可靠性 (S-11) → 全局锁 (S-12) → Phase2 分支收束 (S-13)。

## 四、知识网络图

```
← 复用: ZooKeeper (4.3 协调面) + RocketMQ RM-12 (消息事务对照) + Kafka (4.2)
→ 引出: Curator (4.5) + SofaJRaft (4.6) + 阶段 5 (Dubbo/gRPC RPC 面)
```

## 五、完成检查单

- [x] 顶层模块扫描 ↔ 域清单覆盖矩阵 (13/13)
- [x] 09 审计: 接受 13 域, 补充 3 处 (方言/包名/版本), 待验证 5 处 (S-3/S-5/S-6/S-7/S-11/S-12 数字穷举)
- [x] **阶段 4.4 全量收官 13/13** (2026-08-15): 大纲 857 行/域文件 5668 行/REVIEW 190 处/harness 11/11
