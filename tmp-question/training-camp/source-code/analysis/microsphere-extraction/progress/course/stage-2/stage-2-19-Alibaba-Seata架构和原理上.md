# stage-2 · 第 19 节：Alibaba Seata 架构和原理（上）— 知识点提取

> 课程：stage-2 模式设计与实现 第 19 节
> 来源 docs：`/data/workspace/java-training-camp/stage-2/docs/19. 第十九节：Alibaba Seata 架构和原理（上）.md`
> 提取时间：2026-08-11 | 权重：核心（Seata 分布式事务主线，源码级）

---

## 一、本节概览

- **技术域**：Seata 基础（简介/术语/事务模式）+ AT 模式核心组件（DataSourceProxy/ConnectionProxy/UNDO 日志）
- **维度**：`[分布式问题]`（分布式事务）+ `[工程问题]`（代理/组件，源码级）
- **核心命题**：理解 Seata 术语（TC/TM/RM）、事务模式（AT/TCC/SAGA/XA）、AT 模式核心组件
- **知识点数**：12 个
- **前置**：第 15 节 JTA、第 18 节 TCC、第 16 节 Spring 事务、JDBC 代理

## 前置条件清单
读者需先掌握：
1. **JTA/XA**（第 15 节：2PC）
2. **TCC**（第 18 节）
3. **Spring 事务**（第 16 节）
4. **JDBC 代理/DataSource**（第 13 节）
未达前置者，先补：第 13/15/16/18 节

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 已确认
讲解策略（源码节）：
- **源码理解强**：Seata 组件/代理直接对照本地 seata 源码讲
- **工程化弱**：AT 模式机制/UNDO 日志补基础
- **必做**：对照 `code/spring/seata` 完整源码验证（08 教训）；注意 `io.seata`(docs 旧包) → `org.apache.seata`(新版) 命名空间迁移(04)

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 Seata 简介与术语（TC/TM/RM）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]`（Seata 当前主流） | **置信度**：High
- **前置**：分布式事务
- **来源**：docs §Seata 简介/术语
- **需求**：理解 Seata 提供一站式分布式事务方案
- **自主实现**：若我设计——定义 TC/TM/RM 三角色协调全局事务
- **参考实现**（docs）：**Seata** 开源分布式事务方案，提供 **AT/TCC/SAGA/XA** 四模式；**术语**——
  - **TC**(事务协调者)：维护全局和分支事务状态，驱动全局提交/回滚
  - **TM**(事务管理器)：定义全局事务范围(开始/提交/回滚全局事务)
  - **RM**(资源管理器)：管理分支事务资源，与 TC 交谈注册/报告分支事务状态
- **对比取舍**：**三角色(TC/TM/RM)**——TC 协调、TM 定范围、RM 管分支，类似 XA 的 TM/RM 演进
- **AT 模式前提（docs §AT 模式·前提）**：①基于支持本地 **ACID 事务**的关系型数据库 ②**Java 应用**通过 **JDBC** 访问数据库——AT 模式依赖本地 ACID + JDBC(见 DataSourceProxy 代理)
- **测试佐证**：`code/spring/seata` 完整源码

### KP-02 Seata 事务模式（AT/TCC/SAGA/XA 概览）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01、各事务模式
- **来源**：docs §Seata 事务模式
- **需求**：理解 Seata 四种事务模式定位
- **自主实现**：若我设计——按场景选 AT/TCC/SAGA/XA
- **参考实现**（docs）：四种模式——**AT**(基于本地 ACID 关系库，两阶段：业务+回滚日志同事务提交/二阶段自动回滚)、**TCC**(两阶段，自定义 prepare/commit/rollback，见第 18 节)、**SAGA**(长事务，一阶段正向/二阶段补偿，事件驱动，理论 Sagas 1987 Hector&Kenneth)、**XA**(标准 2PC)
- **SAGA 适用/优缺（docs §Saga 模式，显式提取）**：**适用**——业务流程长/多、参与者含遗留系统无法提供 TCC 三接口；**优势**——一阶段提交本地事务(无锁高性能)、事件驱动(异步高吞吐)、补偿服务易实现；**缺点**——**不保证隔离性**
- **对比取舍**：**四模式定位**——AT 无侵入、TCC 业务补偿、SAGA 长事务、XA 标准；按业务选
- **测试佐证**：`BranchType`(AT/TCC/SAGA/XA) 源码

### KP-03 BranchType（分支事务类型）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-02
- **来源**：docs §Seata 通用组件·BranchType + 源码验证
- **需求**：定义分支事务类型枚举
- **自主实现**：若我设计——AT/TCC/SAGA/XA 枚举
- **参考实现**（docs + 源码）：`BranchType` 枚举——`AT`/`TCC`/`SAGA`/`XA`；源码 `core/.../model/BranchType.java`(AT 29/TCC 34/SAGA 39/XA)
- **对比取舍**：**分支类型**——标识分支事务模式，ResourceManager 按 BranchType 分发
- **测试佐证**：源码 `core/src/main/java/org/apache/seata/core/model/BranchType.java`

### KP-04 SQL 相关资源（SQLType/JdbcConstants/SQLRecognizer）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：JDBC、SQL
- **来源**：docs §SQL 相关资源 + 源码验证
- **需求**：识别 SQL 类型/数据库类型，解析 SQL
- **自主实现**：若我设计——SQLType 枚举 + JdbcConstants + SQLRecognizer(Antlr)
- **参考实现**（docs + 源码）：**SQLType** 枚举——SELECT(0)/INSERT(1)/UPDATE(2)/DELETE(3)/SELECT_FOR_UPDATE(4)；**JdbcConstants**——ORACLE/MYSQL/DB2/H2/MARIADB/POSTGRESQL；**SQLRecognizer**——用 **Antlr** 解析 SQL 生成
- **对比取舍**：**SQL 识别**——AT 模式解析 SQL 生成 before/afterImage，识别类型/表/条件
- **测试佐证**：源码 SQLType/JdbcConstants/SQLRecognizer

### KP-05 ResourceManager（资源管理器 + 各实现）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01
- **来源**：docs §资源相关组件 + 源码验证
- **需求**：管理分支事务资源（RM）
- **自主实现**：若我设计——ResourceManager 按 BranchType 分发到各实现
- **参考实现**（docs + 源码）：`ResourceManager` 接口；`DefaultResourceManager`(38 行)——**通过 ResourceManager SPI 加载不同 BranchType 的 ResourceManager 实现**(按分支类型 getResourceManager 分发 115 行)；各实现——`DataSourceManager`(AT)/`TCCResourceManager`(TCC)/`SagaResourceManager`(SAGA)/`ResourceManagerXA`(XA)；核心方法 `getBranchType()`(155)
- **对比取舍**：**SPI 分发**——DefaultResourceManager 按 BranchType 分发到 AT/TCC/SAGA/XA 各 RM 实现
- **测试佐证**：源码 `rm/.../DefaultResourceManager.java`(38/getResourceManager 115/getBranchType 155)+各 RM 实现

### KP-06 UNDO 回滚日志组件（SQLUndoLog/TableMeta/ColumnMeta/IndexMeta）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：JDBC 元数据
- **来源**：docs §UNDO 回滚日志组件 + 源码验证
- **需求**：AT 模式记录回滚日志（before/after 镜像）
- **自主实现**：若我设计——SQLUndoLog 含 beforeImage/afterImage，TableMeta 描述表
- **参考实现**（docs + 源码）：`UndoLogManager`(回滚日志管理器)/`SQLUndoLog`(实体)——成员 `sqlType`/`tableName`/`beforeImage`(TableRecords)/`afterImage`(TableRecords)；`TableMeta`(表元数据)/`ColumnMeta`(行元数据)/`IndexMeta`(索引元数据)——数据来源于 **ResultSetMetadata**
- **对比取舍**：**镜像对比回滚**——before/after 镜像生成反向 SQL；TableMeta 来自 ResultSetMetadata
- **测试佐证**：源码 `rm-datasource/.../undo/SQLUndoLog.java`/`UndoLogManager.java`

### KP-07 DataSourceProxy（数据源代理）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：JDBC DataSource
- **来源**：docs §DataSourceProxy + 源码验证
- **需求**：代理 DataSource 实现 AT 模式资源拦截
- **自主实现**：若我设计——DataSourceProxy 静态代理 DataSource，返回 ConnectionProxy
- **参考实现**（docs + 源码）：`DataSourceProxy`——JDBC `javax.sql.DataSource` 接口的静态代理，操作来自父类 `AbstractDataSourceProxy`；实现 `Resource`(被 RM 注册/注销，底层 RPC 上报 TC)；关键成员——`resourceGroupId`(默认"DEFAULT")/`jdbcUrl`/`resourceId`/`dbType`/`version`；`init`(105 行)——分析连接信息、`DefaultResourceManager.get().registerResource(this)`(注册资源)、`TableMetaCacheFactory.registerTableMeta`(注册表元)、`RootContext.setDefaultBranchType`(默认 AT)；`getConnection`(返回 `ConnectionProxy`)
- **对比取舍**：**DataSource 代理**——透明代理，注册为 RM 资源，默认 AT
- **测试佐证**：源码 `rm-datasource/.../DataSourceProxy.java`(init 105/registerResource/TableMetaCacheFactory)

### KP-08 ConnectionProxy（连接代理 + 全局锁）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-07、JDBC Connection
- **来源**：docs §ConnectionProxy + 源码验证
- **需求**：代理 Connection 处理全局事务/锁
- **自主实现**：若我设计——ConnectionProxy 管理 ConnectionContext + 全局锁检查
- **参考实现**（docs + 源码）：`ConnectionProxy`——成员 `context`(ConnectionContext，存 SQLUndoLog)/`lockRetryPolicy`(锁重试)；方法——`checkLock`(113，全局锁检查，`lockQuery` 向 TC 查询)、`lockQuery`(136)、`createStatement`(返回 StatementProxy)/`prepareStatement`(返回 PreparedStatementProxy)、`commit`(188，`lockRetryPolicy.execute(doCommit)`)
- **对比取舍**：**连接代理**——拦截 SQL/提交，管理全局锁 + UNDO 日志
- **测试佐证**：源码 `rm-datasource/.../ConnectionProxy.java`(checkLock 113/commit 188/doCommit 227)

### KP-09 全局锁查询（lockQuery → TC）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-08
- **来源**：docs §全局锁检查 + 源码验证
- **需求**：AT 模式通过全局锁避免并发冲突
- **自主实现**：若我设计——RM 向 TC 发 GlobalLockQueryRequest 查询锁
- **参考实现**（docs + 源码）：`checkLock`(ConnectionProxy)——`DefaultResourceManager.get().lockQuery(BranchType.AT, resourceId, xid, lockKeys)`(313 行)，不可锁则抛 `LockConflictException`；`DataSourceManager.lockQuery`——发 `GlobalLockQueryRequest`(含 xid/lockKey/resourceId) 经 **RmNettyRemotingClient** 同步发给 TC，返回 `lockable`
- **对比取舍**：**全局锁协调**——RM 向 TC 查询/获取全局锁，防分支事务并发冲突
- **测试佐证**：源码 `ConnectionProxy.checkLock`/`rm-datasource/.../DataSourceManager.lockQuery`

### KP-10 事务提交（doCommit/processGlobalTransactionCommit）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-08
- **来源**：docs §事务提交 + 源码验证
- **需求**：理解 AT 模式一阶段提交
- **自主实现**：若我设计——全局事务：register→flushUndoLogs→commit；全局锁：checkLock→commit；本地：直接 commit
- **参考实现**（docs + 源码）：`doCommit`(227 行)——按 ConnectionContext 判断：`inGlobalTransaction`→`processGlobalTransactionCommit`(247)；`isGlobalLockRequire`→`processLocalCommitWithGlobalLocks`(238 checkLock)；否则本地 commit(430)；**processGlobalTransactionCommit**——`register()`(注册分支)、`UndoLogManager.flushUndoLogs`(生成 UNDO SQL 未提交)、`targetConnection.commit()`(业务+UNDO 一并提交)、`report`
- **对比取舍**：**一阶段提交**——业务+UNDO 日志同事务提交，释放本地锁；二阶段靠 UNDO 回滚
- **测试佐证**：源码 `ConnectionProxy.doCommit(227)/processGlobalTransactionCommit(247)/flushUndoLogs`

### KP-11 Statement 代理（StatementProxy/PreparedStatementProxy）+ 镜像构建
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-08、JDBC Statement
- **来源**：docs §Statement 代理 + §executeAutoCommitFalse + 源码验证
- **需求**：代理 Statement 拦截 SQL 构建镜像
- **自主实现**：若我设计——StatementProxy 执行时构建 before/afterImage + prepareUndoLog
- **参考实现**（docs + 源码）：`StatementProxy`/`PreparedStatementProxy`(代理)；`executeAutoCommitFalse`——`beforeImage()`(执行前镜像)→`statementCallback.execute`(执行业务 SQL)→`afterImage()`(执行后镜像)→`prepareUndoLog`(生成 UNDO 日志)
- **对比取舍**：**镜像构建**——before/after 镜像 + UNDO 日志，是 AT 回滚的基础
- **测试佐证**：源码 `executeAutoCommitFalse` + 镜像构建

### KP-12 Seata Spring AOP + 场景分析
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Spring AOP
- **来源**：docs §Seata Spring AOP 组件 + §场景分析
- **需求**：Seata 用 Spring AOP 自动代理 DataSource
- **自主实现**：若我设计——SeataAutoDataSourceProxyCreator/Advice 自动代理
- **参考实现**（docs + 源码）：`SeataAutoDataSourceProxyCreator`(Spring AOP 自动创建器)/`SeataAutoDataSourceProxyAdvice`(Advice)；**场景分析**——使用 Seata 全局事务 vs 非全局事务(两种场景)
- **对比取舍**：**Spring AOP 代理 DataSource**——自动把 DataSource 换成 DataSourceProxy
- **测试佐证**：源码 spring 模块

> **docs 空节标注（穷尽性）**：docs §AT 模式的两阶段提交（543 行）仅标题无正文——其内容已在 KP-10 事务提交(一阶段 register→flushUndoLogs→commit；二阶段 UNDO 补偿/异步清理) 提取，二阶段细节由第 20 节深入。标注符合 08 §2。

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| Seata 简介与术语(TC/TM/RM) | 分布式问题 | 核心 | P1 | 🔴 | 有效 | High |
| 事务模式(AT/TCC/SAGA/XA) | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| BranchType | 工程问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| SQL 相关资源 | 工程问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| ResourceManager | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| UNDO 回滚日志 | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| DataSourceProxy | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| ConnectionProxy | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 全局锁查询 | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 事务提交 | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| Statement 代理+镜像 | 工程问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| Spring AOP + 场景 | 工程问题 | 支撑 | P2 | 🟡 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：`code/spring/seata` 完整源码——BranchType/DataSourceProxy/ConnectionProxy/UndoLogManager/SQLUndoLog/DefaultResourceManager 全部验证
- **关键源码类**（本次实证）：`BranchType`(AT 29/TCC 34/SAGA 39)、`DataSourceProxy.init`(105)、`ConnectionProxy`(checkLock 113/commit 188/doCommit 227/processGlobalTransactionCommit 247)、`DefaultResourceManager`(38/115/155)
- **命名空间迁移（04）**：docs 用 `io.seata.*`(旧包)，新版是 `org.apache.seata.*`——标 `[过时→org.apache.seata]`(命名空间迁移≠机制)
- **关联标注**：microsphere 用 Seata 分布式事务 `[待验证]`；衔接第 18 节 TCC、第 20 节 Seata 下

---

## 五、本节小结（三层次视角）

**需求**：理解 Seata 分布式事务方案——TC/TM/RM 术语、四事务模式、AT 模式核心组件。

**自主实现核心**：若我设计——
1. TC/TM/RM 三角色协调
2. AT/TCC/SAGA/XA 四模式
3. ResourceManager 按 BranchType SPI 分发
4. DataSourceProxy/ConnectionProxy 代理
5. UNDO 回滚日志(before/after 镜像)
6. 全局锁 + 一阶段提交

**参考实现**：seata 源码(`code/spring/seata` 完整验证) + docs。**已源码验证，非只看 docs**。

**对比取舍**：知识本体是"**Seata 架构与 AT 模式核心**"。核心洞察：**TC/TM/RM、四事务模式、DataSource/Connection 代理、UNDO 镜像回滚、全局锁**。为第 20 节 Seata 下铺垫。

**待验证汇总**：
- microsphere 用 Seata 的具体场景
- io.seata→org.apache.seata 命名空间迁移细节

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 Seata 组件 + 源码片段 + 本地 seata 源码验证；补全聚焦"Seata AT 模式的工程价值"。

### 完整认知：Seata 在真实架构中完整该讲什么

docs 覆盖了术语/模式/AT 组件。作为架构师，这个主题完整还该包含：

1. **Seata 是国产分布式事务的事实标准**：AT/TCC/SAGA/XA 四模式一站式，国内主流——但现代更广泛实践是"避免强一致"(补充大纲 Outbox/事件驱动)
2. **TC/TM/RM 三角色**：TC 无状态协调者(可集群)、TM 定全局事务边界、RM 管分支——类似 XA 的演进，支持跨服务
3. **AT 模式的无侵入魔力**：DataSource 代理 + UNDO 日志，业务零侵入(对比 TCC 三方法)——AT 是 Seata 杀手锏
4. **UNDO 日志 = 反向 SQL 补偿**：before/after 镜像生成反向 SQL，二阶段回滚——类似数据库 MVCC/Undo Log 思想
5. **全局锁防脏写**：AT 模式全局锁避免分支事务并发冲突(写隔离)——保证隔离性
6. **一阶段释放锁**：业务+UNDO 同事务提交，快速释放本地锁(对比 2PC 锁到二阶段)——性能
7. **与 XA 对比**：Seata AT 是"改进的 2PC"(一阶段提交+UNDO 补偿)，XA 是标准 2PC(两阶段锁)——AT 更优

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| AT vs TCC vs SAGA vs XA | 无侵入 vs 补偿 vs 长事务 vs 标准 |
| AT 无侵入 | 业务零侵入；需全局锁/UNDO 开销 |
| io.seata vs org.apache.seata | 命名空间迁移(04)，机制不变 |
| Seata vs 现代 Outbox | Seata 强一致；Outbox 最终一致 |
| 一阶段释放锁 | 快速提交；需 UNDO 补偿 |

### 常见坑/反模式

1. **io.seata 旧包残留**：命名空间迁移到 org.apache.seata(04)
2. **AT 模式业务表缺主键/索引**：UNDO 镜像需主键/索引定位
3. **忽略全局锁冲突**：并发写需处理 LockConflictException
4. **强一致需求用 AT**：AT 强一致但需全局锁；最终一致场景可用 Outbox
5. **UNDO 日志膨胀**：大事务 UNDO 日志多——控制事务粒度

### 生态位置

- **分布式问题维度**：Seata 是**分布式事务一站式方案**——承接第 18 节 TCC、第 20 节 Seata 下，衔接补充大纲(现代实践)
- **衔接**：TCC(18) → Seata 上(本篇) → Seata 下(20) → 补充大纲(Outbox/事件驱动)
- **与源码提取的关系**：seata 全源码是核心

**架构师视角结论**：本篇不只是背 TC/TM/RM，而是"**理解 Seata 分布式事务方案与 AT 模式**"——TC/TM/RM 三角色、四事务模式、DataSource 代理 + UNDO 镜像回滚 + 全局锁实现无侵入 AT 模式；这是国产分布式事务事实标准，也需结合现代实践(Outbox)选型。
