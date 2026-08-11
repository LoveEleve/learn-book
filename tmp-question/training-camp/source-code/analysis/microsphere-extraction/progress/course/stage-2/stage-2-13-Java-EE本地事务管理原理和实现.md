# stage-2 · 第 13 节：Java EE 本地事务管理原理和实现 — 知识点提取

> 课程：stage-2 模式设计与实现 第 13 节
> 来源 docs：`/data/workspace/java-training-camp/stage-2/docs/13. 第十三节：Java EE 本地事务管理原理和实现.md`
> 提取时间：2026-08-11 | 权重：核心（本地事务主线起点）

---

## 一、本节概览

- **技术域**：数据库事务 + JDBC 规范（驱动/连接/语句/事务/Savepoint/隔离级别）
- **维度**：`[规范]`（JDBC 规范，活跃）+ `[分布式问题]`（事务/隔离，弱）+ `[性能优化]`（隔离级别实现，弱）
- **核心命题**：理解本地数据库事务（ACID/隔离级别）与 JDBC 规范（驱动加载/连接/事务管理）
- **知识点数**：12 个
- **前置**：SQL 基础、数据库事务基本概念、Java SPI

## 前置条件清单
读者需先掌握：
1. **SQL 基础**（SELECT/INSERT 等）
2. **数据库事务概念**（ACID 基础）
3. **Java SPI**（DriverManager 加载驱动，第 1 节相关）
4. **Java 类加载**
未达前置者，先补：SQL + 事务基础 + Java SPI

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 已确认
讲解策略（规范 + 源码节）：
- **源码理解强**：JDBC 驱动加载/连接/事务对照 JDK17 源码讲
- **工程化弱**：DataSource/JNDI/隔离级别补基础
- **必做**：对照 JDK17 `DriverManager` 源码验证（docs 的 MySQL 驱动无本地源码，用 JDK 规范层验证，08 教训）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 数据库事务（ACID + SQL 标准）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：事务基础
- **来源**：docs §数据库事务
- **需求**：理解数据库事务 ACID 特性与 SQL 标准
- **自主实现**：若我设计——原子/一致/隔离/持久四特性保证数据完整
- **参考实现**（docs）：SQL 标准组织(SQL 99/SQL 03)，面向数据集，数据表二维结构(行/列)；事务 **ACID**——原子性/一致性/隔离性/持久性
- **对比取舍**：**ACID 是单机事务保证**——与分布式 CAP/BASE(第 1 节) 一致性对比(本地 vs 分布式)
- **测试佐证**：数据库原理 + 第 1 节 CAP 对照

### KP-02 JDBC 隔离级别（隔离级别 + MySQL 特点）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01
- **来源**：docs §隔离级别
- **需求**：理解 JDBC 事务隔离级别与 MySQL 实现
- **自主实现**：若我设计——四级隔离控制并发读写冲突
- **参考实现**（docs + 源码验证）：JDBC 隔离级别——`REPEATABLE_READ`(事务开始构建 ReadView 快照，只读)、`READ_COMMITTED`(读已提交，Happens-Before/fence/lock)、`READ_UNCOMMITTED`(读未提交，可能脏数据)、`SERIALIZABLE`(串行)；MySQL 默认 REPEATABLE READ；驱动 `ConnectionImpl.setTransactionIsolation` 发 `SET SESSION TRANSACTION ISOLATION LEVEL ...` SQL(源码片段)
- **对比取舍**：**隔离级别权衡**——RR 快照一致性 vs RC 更宽松；READ UNCOMMITTED 可能脏读
- **测试佐证**：docs MySQL 源码片段 + JDBC `Connection.TRANSACTION_*` 常量

### KP-03 JDBC 规范概览（核心 API）
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]`（JDBC 活跃规范） | **置信度**：High
- **前置**：Java 数据库
- **来源**：docs §JDBC 规范
- **需求**：掌握 JDBC 核心 API 与使用场景
- **自主实现**：若我设计——DataSource/Connection/Statement 抽象数据库访问
- **参考实现**（docs）：JDBC 核心 API——`DataSource`、`Connection`、`Statement` 等；理解 **Metadata**(ResultSetMetaData/DatabaseMetaData)在数据库实践的价值；了解 JDBC 事务与分布式事务使用场景
- **对比取舍**：**JDBC 是 Java 数据库访问规范**——连接/语句/元数据抽象，分布式事务是后续(第 15 节 JTA)
- **测试佐证**：JDK `java.sql` 包

### KP-04 JDBC 连接（Connections / JDBC URL）
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-03
- **来源**：docs §JDBC 连接 + 源码验证
- **需求**：理解 JDBC 连接获取
- **自主实现**：若我设计——Driver 实现获取 Connection
- **参考实现**（docs）：`Connection` 表示到数据源的连接，通过 JDBC 驱动(`java.sql.Driver` 实现)获取；少数用 `DataSource`；**JDBC URL** 是字符串(非 URL 对象)，如 `jdbc:mysql://127.0.0.1:3307/...`
- **对比取舍**：**URL 约定**——`jdbc:子协议://...` 标识数据源
- **测试佐证**：JDK `java.sql.Connection`/`Driver`

### KP-05 DriverManager（驱动容器/加载/连接）
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：Java SPI
- **来源**：docs §DriverManager + JDK17 源码验证
- **需求**：管理 JDBC 驱动并获取连接
- **自主实现**：若我设计——DriverManager 容器注册/注销/遍历驱动
- **参考实现**（docs + JDK17 源码）：`DriverManager`——
  - **Driver 容器**：注册/注销驱动，允许多驱动(MySQL/Oracle)
  - **加载初始化** `loadInitialDrivers`：`jdbc.drivers` 系统属性 + `ServiceLoader.load(Driver.class)`(SPI) 加载；JDBC 驱动 static 块调 `registerDriver`
  - **getConnection**：遍历 `registeredDrivers`(CopyOnWriteArrayList 86 行)，`isDriverAllowed`(281 行) 检查类加载器，`driver.connect(url,info)` 连接，找到第一个成功
- **对比取舍**：**SPI + 属性双加载**——jdbc.drivers 属性 + ServiceLoader(SPI)；registeredDrivers 线程安全容器
- **测试佐证**：JDK17 `java/sql/DriverManager.java`(registeredDrivers 86/getConnection 187/isDriverAllowed 281/JDBC_DRIVERS_PROPERTY 95)

### KP-06 JDBC 驱动接口（Driver）
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-05
- **来源**：docs §JDBC 驱动接口 + 源码验证
- **需求**：定义 JDBC 驱动实现接口
- **自主实现**：若我设计——Driver.connect 连接、acceptsURL 判断
- **参考实现**（docs + JDK 源码）：`Driver` 接口核心方法——`connect(url, info)`(连接，非本驱动返回 null)、`acceptsURL(url)`(判断能否连接)、`getPropertyInfo`(配置信息)、`getMajorVersion/getMinorVersion`(版本)、`jdbcCompliant()`(是否兼容 JDBC)；`DriverManager` 是 Driver 门面 + Connection 代理
- **对比取舍**：**驱动可插拔**——多驱动共存，acceptsURL 分流
- **测试佐证**：JDK `java.sql.Driver`

### KP-07 JDBC 数据源（DataSource）
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-04
- **来源**：docs §数据源 + 源码验证
- **需求**：用 DataSource 获取连接（比 DriverManager 更优）
- **自主实现**：若我设计——DataSource 接口 + 连接池装饰器
- **参考实现**（docs）：`DataSource` 通常由 JDBC 驱动实现(如 MySQL `MysqlDataSource`)；**连接池提供 DataSource 装饰器模式实现**(DBCP/C3P0/Hikari/Druid)；通过 JNDI 配置(底层是 JDBC Driver)，如 Tomcat DataSource 配置
- **对比取舍**：**DataSource + 连接池**——比 DriverManager 更优(池化/事务/分布式支持)；Hikari/Druid 是主流池
- **测试佐证**：`javax.sql.DataSource` + 连接池(Druid/Hikari)

### KP-08 JDBC 语句（Statement 分类与构建）
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-04
- **来源**：docs §JDBC 语句 + 源码验证
- **需求**：分类与构建 SQL 语句
- **自主实现**：若我设计——Statement/PreparedStatement/CallableStatement 三类
- **参考实现**（docs）：**Statement 分类**——普通 `Statement`、预编译 `PreparedStatement`、调用类 `CallableStatement`；**构建链**——Driver→Connection→Statement→ResultSet→ResultSetMetaData；MySQL 实现——`StatementImpl`/`NativeSession`(底层会话)、`PreparedStatement` 用 MySQL PREPARE 命令(ClientPreparedStatement/ServerPreparedStatement)
- **对比取舍**：**预编译防注入+性能**——PreparedStatement 预编译；CallableStatement 调用存储过程
- **测试佐证**：docs MySQL 驱动类 + JDK `java.sql.Statement`

### KP-09 JDBC 事务（Auto-Commit / commit / rollback）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01/04
- **来源**：docs §JDBC 事务 + 源码验证
- **需求**：用 Connection 管理本地事务
- **自主实现**：若我设计——setAutoCommit(false) 关闭自动提交，手动 commit/rollback
- **参考实现**（docs + MySQL 源码片段）：**自动提交**——默认 true；**失效** `setAutoCommit(false)`(发 `SET autocommit=0`)；**主动提交** `commit()`——判断非 autoCommit、判断服务器支持事务、发 `commit` SQL；**回滚** `rollback()`
- **对比取舍**：**本地事务**——单连接事务(ACID)；多个数据库需分布式事务(第 15 节 JTA)
- **测试佐证**：docs MySQL `ConnectionImpl.setAutoCommit/commit` 源码片段

### KP-10 事务保护点（Savepoints）
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-09
- **来源**：docs §Savepoints + 源码验证
- **需求**：在事务内设置部分回滚点
- **自主实现**：若我设计——Savepoint 标记位置，可部分回滚
- **参考实现**（docs + MySQL 源码片段）：`DatabaseMetaData.supportsSavepoints` 判断支持；`setSavepoint(name)` 创建(底层 `SAVEPOINT` SQL)；`rollback(Savepoint)` 回滚到点(`ROLLBACK TO SAVEPOINT`)；`releaseSavepoint`(MySQL 实现是 no-op，**未执行 RELEASE SAVEPOINT**)
- **对比取舍**：**部分回滚**——Savepoint 支持事务内部分撤销；MySQL releaseSavepoint 空实现
- **测试佐证**：docs MySQL `ConnectionImpl.setSavepoint/rollback/releaseSavepoint` 源码片段

### KP-11 JDBC 类型（Types / JDBCType）
- **维度**：`[规范]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[有效]` | **置信度**：High
- **前置**：SQL 类型
- **来源**：docs §JDBC 类型 + 源码验证
- **需求**：定义 JDBC 数据类型
- **自主实现**：若我设计——Types 常量 + JDBCType 枚举
- **参考实现**（docs + JDK 源码）：传统接口 `java.sql.Types`(常量)、核心枚举 `java.sql.JDBCType`
- **对比取舍**：**类型映射**——Java↔SQL 类型对应
- **测试佐证**：JDK `java.sql.Types`/`JDBCType`

### KP-12 JDBC 驱动非 JDBC 功能（Binlog）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[有效]`（Canal/Maxwell 当前） | **置信度**：High
- **前置**：无
- **来源**：docs §JDBC 驱动非 JDBC 功能 + 源码验证
- **需求**：了解基于 Binlog 的数据同步
- **自主实现**：无（生态）
- **参考实现**（docs）：MySQL 驱动 Binlog 功能框架——**Apache Canal**、**Maxwell's Daemon**(基于 Binlog 的数据同步/CDC)
- **对比取舍**：**CDC 生态**——Canal/Maxwell 基于 Binlog 实现数据变更捕获
- **关联 microsphere**：microsphere 是否用 Canal `[待验证]`

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| 数据库事务(ACID) | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| JDBC 隔离级别 | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| JDBC 规范概览 | 规范 | 核心 | P1 | 🔴 | 有效 | High |
| JDBC 连接(URL) | 规范 | 核心 | P1 | 🟡 | 有效 | High |
| DriverManager | 规范 | 核心 | P1 | 🔴 | 有效 | High |
| JDBC 驱动接口 | 规范 | 核心 | P1 | 🟡 | 有效 | High |
| DataSource | 规范 | 核心 | P1 | 🟡 | 有效 | High |
| JDBC 语句 | 规范 | 核心 | P1 | 🟡 | 有效 | High |
| JDBC 事务 | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| Savepoints | 分布式问题 | 支撑 | P2 | 🟡 | 时间无关 | High |
| JDBC 类型 | 规范 | 支撑 | P3 | 🟢 | 有效 | High |
| Binlog 功能 | 工程问题 | 支撑 | P3 | 🟢 | 有效 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：JDK17 `DriverManager.java`（驱动加载/连接机制）、JDK `java.sql` 包；docs 的 MySQL 驱动类(`com.mysql.cj.jdbc.ConnectionImpl` 等)**无本地源码**，用 JDK 规范层验证 + docs 源码片段
- **关键源码类**（本次实证）：JDK17 `java/sql/DriverManager.java`(registeredDrivers 86/getConnection 187/isDriverAllowed 281/JDBC_DRIVERS_PROPERTY 95)
- **关联标注**：microsphere-mybatis/druid 用 JDBC/DataSource `[待验证]`；衔接第 14 节 Spring 事务、第 15 节 JTA

---

## 五、本节小结（三层次视角）

**需求**：理解本地数据库事务（ACID/隔离级别）与 JDBC 规范（驱动/连接/事务）。

**自主实现核心**：若我设计——
1. ACID 保证单机事务完整
2. 隔离级别权衡(ReadView 快照)
3. JDBC：DriverManager(SPI 加载) + DataSource(连接池) + Statement 分类
4. 事务：setAutoCommit(false) + commit/rollback + Savepoint

**参考实现**：JDK17 `DriverManager`(源码验证) + JDBC 规范 + docs MySQL 源码片段（MySQL 驱动无本地源码，标注）。

**对比取舍**：知识本体是"**Java EE 本地事务 + JDBC 规范**"。核心洞察：**ACID、隔离级别、DriverManager(SPI)、DataSource、Statement、本地事务/Savepoint**。为第 14 节 Spring 事务铺垫。

**待验证汇总**：
- microsphere-mybatis/druid 用 JDBC 具体场景
- MySQL 驱动源码(无本地，需外部)

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 JDBC 规范 + MySQL 源码片段 + JDK17 源码验证；补全聚焦"JDBC/本地事务的工程价值"。

### 完整认知：JDBC/本地事务在真实架构中完整该讲什么

docs 覆盖了 ACID/隔离/JDBC 驱动/连接/事务。作为架构师，这个主题完整还该包含：

1. **JDBC 是 Java 数据库访问的地基**：所有 ORM(MyBatis/JPA)、连接池(Druid/Hikari)、事务框架(Spring) 都建立在 JDBC 之上——理解 JDBC 才懂上层
2. **DataSource + 连接池是生产标配**：DriverManager 直连不适用生产，DataSource(池化) 是标准；Hikari(性能)/Druid(监控) 是主流
3. **隔离级别的工程权衡**：RR(默认,快照)/RC(宽松)/Serializable(串行)——按业务并发与一致需求选；MVCC/锁 是 MySQL 实现基础
4. **本地事务 vs 分布式事务**：单库用 JDBC 本地事务(ACID)；多库需分布式事务(JTA/Seata，第 15 节起)——事务边界的架构决策
5. **SPI 驱动加载**：DriverManager 用 ServiceLoader(SPI) 加载驱动——Java SPI 机制的典型应用(第 1 节工程化)
6. **Savepoint 部分回滚**：事务内部分撤销，复杂业务逻辑的回滚细粒度控制
7. **CDC 生态**：Binlog+Canal/Maxwell 实现数据库变更捕获，是实时同步/双写的架构基础

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| RR vs RC vs Serializable | 快照一致 vs 宽松 vs 串行——并发/一致权衡 |
| DriverManager vs DataSource | 直连简单；池化生产标准 |
| 本地事务 vs 分布式 | 单库 ACID；多库需分布式(复杂) |
| Statement vs PreparedStatement | 预编译防注入/性能；普通简单 |
| 连接池选择 | Hikari 性能；Druid 监控 |

### 常见坑/反模式

1. **直接用 DriverManager**：生产应用 DataSource 池化，直连浪费连接
2. **忽略隔离级别**：默认 RR 可能不适合高并发——按需调整
3. **事务不 commit/rollback**：忘记关闭自动提交或未回滚——连接泄漏/数据错
4. **SQL 注入**：用 Statement 拼接——用 PreparedStatement
5. **Savepoint 滥用**：过多 Savepoint 影响性能

### 生态位置

- **规范维度**：JDBC 是**活跃数据库规范**——承接第 1 节工程化(SPI)、为第 14 节 Spring 事务、第 15 节 JTA、microsphere-mybatis/druid 铺垫
- **衔接**：ACID(本篇) → Spring 事务(第 14 节) → JTA/XA(第 15 节) → 分布式事务(第 16-20 节)
- **与源码提取的关系**：JDK17 DriverManager + JDBC 规范层是核心源码；MySQL 驱动需外部

**架构师视角结论**：本篇不只是背 JDBC API，而是"**理解 Java 数据库访问与本地事务的地基**"——ACID、隔离级别、DriverManager(SPI)、DataSource 池化、本地事务/Savepoint；这是 MyBatis/Spring 事务/JTA 一切上层的地基。
