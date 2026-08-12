# stage-2 · 第 25 节：通用数据读写分离设计 — 知识点提取

> 课程：stage-2 模式设计与实现 第 25 节
> 来源 docs：`/data/workspace/java-training-camp/stage-2/docs/25. 第二十五节：通用数据读写分离设计.md`
> 提取时间：2026-08-11 | 权重：核心（读写分离主线，源码级）

---

## 一、本节概览

- **技术域**：MySQL 主从复制 + DB 读写分离数据源 + Redis 读写分离
- **维度**：`[性能优化]`（读写分离/横向扩展）+ `[工程问题]`（数据源设计，源码级）+ `[分布式问题]`（数据复制）
- **核心命题**：理解读写分离——MySQL 主从复制、动态读写分离数据源设计、Redis 读写分离
- **知识点数**：9 个
- **前置**：第 13 节 JDBC/DataSource、MySQL 基础、Redis 基础

## 前置条件清单
读者需先掌握：
1. **JDBC/DataSource**（第 13 节）
2. **MySQL 基础**（主从/复制）
3. **Redis 基础**（主从）
4. **负载均衡**（第 11 节 stage-1）
未达前置者，先补：第 13 节 + MySQL/Redis 基础

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 已确认
讲解策略（源码节）：
- **源码理解强**：读写分离数据源对照 shardingsphere 源码讲
- **工程化弱**：MySQL 主从复制/操作补基础
- **必做**：对照 `code/spring/shardingsphere`(readwrite-splitting) 源码验证（08 教训）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 MySQL 主从复制（概念/优点）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：MySQL
- **来源**：docs §MySQL 主从复制·概念/优点
- **需求**：掌握 MySQL 主从复制，按场景读写分离
- **自主实现**：若我设计——源(Source)→副本(Replica) 异步复制
- **参考实现**（docs）：**复制**——数据从源复制到一个或多个副本，默认**异步**，副本不需永久连接；可复制所有/选定数据库/选定表；**优点**——
  - **横向扩展**：多副本分散读负载，写必须在源，读在副本
  - **数据安全**：副本可暂停复制，备份不损源
  - **分析**：源实时数据，分析在副本不影响源
  - **远程数据分发**：远程站点本地副本
- **对比取舍**：**读扩展/写集中**——读写分离基础；写源读副本
- **测试佐证**：MySQL 主从复制 + 第 26 节 ShardingSphere

### KP-02 MySQL 主从复制操作（Docker 配置）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[有效]`（MySQL 5.7） | **置信度**：High
- **前置**：Docker
- **来源**：docs §操作
- **需求**：搭建 MySQL 主从复制环境
- **自主实现**：无（环境搭建）
- **参考实现**（docs）：**Master 配置**——`server_id=1`/`log_bin=mysql-bin`/`binlog_format=ROW`/`gtid_mode=ON`；Docker 容器 + 创建 replication 用户 + 数据准备；**Slave 配置**——`server_id=2`/`read_only=ON`；`CHANGE MASTER TO`(MASTER_HOST/PORT/USER/AUTO_POSITION=1) + `START SLAVE`；**测试复制**——Master INSERT → Slave 查询验证
- **对比取舍**：**GTID 主从**——binlog_format=ROW + GTID 模式现代复制
- **测试佐证**：docs Docker/SQL 命令

> **docs 重复内容标注（穷尽性）**：docs §Kafka Docker Compose（606-649 行）——与第 17 节《可靠事件队列》的环境准备内容**完全重复**（Kafka Docker/zookeeper/broker/topic），非本主题(读写分离)新增知识点，不重复提取——交叉引用第 17 节（08 §2，避免重复与张冠李戴）。

### KP-03 MySQL Connector/J 高可用连接（Load Balanced）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]`（Connector/J） | **置信度**：High
- **前置**：JDBC
- **来源**：docs §MySQL Connector/J 高可用连接 + 源码片段
- **需求**：JDBC 层多主机连接（负载均衡）
- **自主实现**：若我设计——LoadBalancedConnectionProxy 代理多主机
- **参考实现**（docs + 源码片段）：`LoadBalancedConnectionProxy`——`loadBalanceConnectionGroup`(连接组)/`enableJMX`/`ConnectionGroupManager.getConnectionGroupInstance`(连接组管理)/`registerConnectionProxy`(注册代理)/`getHostInfoListFromHostPortPairs`(主机列表)；`PropertyKey` 枚举(loadBalanceConnectionGroup/loadBalanceExceptionChecker/loadBalancePingTimeout 等)
- **对比取舍**：**JDBC 层负载均衡**——多主机连接代理 + 连接组管理
- **测试佐证**：docs LoadBalancedConnectionProxy 源码片段 + PropertyKey 枚举

### KP-04 读写分离数据源（实现策略）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01、DataSource
- **来源**：docs §DB 读写分离数据源·实现策略 + 架构师发散
- **需求**：设计动态数据库读写分离数据源
- **自主实现**：若我设计——读写分离数据源按策略路由(写源/读副本)
- **参考实现**（docs + shardingsphere 源码）：**实现策略**——基于 **TransactionServiceSample**(第 17 节案例)；三种切换方式——**①基于 Java 方法切换** ②**基于 Java 注解切换** ③**基于 SQL 分析切换**(SQL 语法规则)；参考 ShardingSphere 读写分离——`ReadwriteSplittingDataSourceGroupRuleConfiguration`(writeDataSourceName 35/readDataSourceNames 37/loadBalancerName 41)
- **对比取舍**：**三种切换策略**——方法/注解(手动)/SQL 分析(自动)；ShardingSphere 用配置组(写源+读副本列表+负载均衡)
- **测试佐证**：源码 `shardingsphere/features/readwrite-splitting/.../ReadwriteSplittingDataSourceGroupRuleConfiguration.java`(35/37/41)

### KP-05 读写分离路由（ShardingSphere Router）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-04
- **来源**：架构师发散 + shardingsphere 源码验证（docs 仅标题）
- **需求**：读写分离 SQL 路由（读→副本，写→源）
- **自主实现**：若我设计——路由分派 Primary/Transactional/Standard
- **参考实现**（shardingsphere 源码）：`ReadwriteSplittingDataSourceRouter.route`(53)——分派到——`QualifiedReadwriteSplittingPrimaryDataSourceRouter`(强制主)/`QualifiedReadwriteSplittingTransactionalDataSourceRouter`(事务内主)/`StandardReadwriteSplittingDataSourceRouter`(标准读副本写主)；`ReadwriteSplittingDataSourceType`(PRIMARY/REPLICA)
- **对比取舍**：**三类路由**——强制主/事务内主/标准读写分离
- **测试佐证**：源码 `features/readwrite-splitting/core/.../route/ReadwriteSplittingDataSourceRouter.java`(53)+`route/standard/StandardReadwriteSplittingDataSourceRouter.java`+`route/qualified/type/QualifiedReadwriteSplittingPrimaryDataSourceRouter.java`

### KP-06 Redis 读写分离数据源
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Redis
- **来源**：docs §Redis 读写分离数据源（仅标题，架构师发散）
- **需求**：设计 Redis 读写分离数据源
- **自主实现**：若我设计——Redis 主从(写主读从) 数据源封装
- **参考实现**（docs 仅标题 + 架构师）：**Redis 读写分离**——Redis 主从复制(写主节点、读从节点)；数据源封装——读写分离 Redis 客户端/代理；本地 `code/spring/redis` 可验证 Redis 客户端
- **对比取舍**：**Redis 读写分离**——主从复制 + 读从；与 MySQL 读写分离同思路
- **测试佐证**：`code/spring/redis`(Redis 客户端) `[待验证]` 具体读写分离实现

### KP-07 MySQL Connector/J PropertyKey（连接属性）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[有效]` | **置信度**：High
- **前置**：JDBC 配置
- **来源**：docs §MySQL JDBC Property Key 枚举
- **需求**：了解 MySQL JDBC 连接属性
- **自主实现**：无（枚举清单）
- **参考实现**（docs + 源码）：`com.mysql.cj.conf.PropertyKey` 枚举——基础(USER/PASSWORD/HOST/PORT/TYPE/ADDRESS/DBNAME)+ 高可用(loadBalanceConnectionGroup/loadBalanceExceptionChecker/loadBalancePingTimeout/failOverReadOnly/replicationConnectionGroup)+ 其他(xdevapi/ssl/缓存等)
- **对比取舍**：**属性枚举**——连接字符串/Properties 配置键
- **测试佐证**：docs PropertyKey 完整源码

### KP-08 读写分离 vs 分库分表（衔接）
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-04、分库分表
- **来源**：架构师发散
- **需求**：理解读写分离与分库分表关系
- **自主实现**：若我设计——读写分离(主从) + 分库分表(数据拆分)
- **参考实现**（架构师）：**读写分离**——数据全量在主从复制，读扩展；**分库分表**——数据按分片拆分，解决单库容量/性能(第 26 节)；ShardingSphere 两者结合——分片+读写分离
- **对比取舍**：**读扩展 vs 容量扩展**——读写分离扩展读；分库分表扩展容量
- **测试佐证**：衔接第 26 节 ShardingSphere

### KP-09 读写分离设计要点（总结）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01~08
- **来源**：架构师整合
- **需求**：总结读写分离设计要点
- **自主实现**：若我设计——主从复制 + 数据源路由 + 一致性权衡
- **参考实现**（架构师整合）：设计要点——①**主从复制**(源/副本异步) ②**读写分离数据源**(写源读副本，路由策略) ③**一致性权衡**(主从延迟，事务内走主/强制主) ④**负载均衡**(读副本分发)
- **对比取舍**：**四要素**——复制/路由/一致性/均衡
- **测试佐证**：整合本篇 KP

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| MySQL 主从复制 | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| MySQL 主从操作 | 工程问题 | 支撑 | P3 | 🟢 | 有效 | High |
| Connector/J 高可用连接 | 工程问题 | 核心 | P1 | 🟡 | 有效 | High |
| 读写分离数据源 | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 读写分离路由 | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| Redis 读写分离 | 性能优化 | 核心 | P1 | 🟡 | 时间无关 | High |
| PropertyKey 枚举 | 工程问题 | 支撑 | P3 | 🟢 | 有效 | High |
| 读写分离 vs 分库分表 | 性能优化 | 核心 | P1 | 🟡 | 时间无关 | High |
| 读写分离设计要点 | 工程问题 | 支撑 | P3 | 🟢 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：`code/spring/shardingsphere`(features/readwrite-splitting)、`code/spring/redis`(Redis 客户端)、`code/spring/druid`(连接池)
- **关键源码类**（本次实证）：`ReadwriteSplittingDataSourceGroupRuleConfiguration`(writeDataSourceName 35/readDataSourceNames 37/loadBalancerName 41)、`ReadwriteSplittingDataSourceRouter.route`(53)、`StandardReadwriteSplittingDataSourceRouter`/`QualifiedReadwriteSplittingPrimaryDataSourceRouter`/`QualifiedReadwriteSplittingTransactionalDataSourceRouter`、`ReadwriteSplittingDataSourceType`
- **诚实标注**：docs 的 MySQL Connector/J 类无本地源码，用 docs 源码片段；Redis 读写分离 `[待验证]` 具体实现
- **关联标注**：microsphere 用读写分离 `[待验证]`；衔接第 26 节 ShardingSphere、第 13 节 JDBC

---

## 五、本节小结（三层次视角）

**需求**：设计通用数据读写分离——MySQL 主从复制 + 动态读写分离数据源 + Redis 读写分离。

**自主实现核心**：若我设计——
1. MySQL 主从复制(源/副本异步，binlog+GTID)
2. 读写分离数据源：写源读副本
3. 路由策略：标准(读副本写主)/事务内主/强制主
4. 读副本负载均衡
5. Redis 主从读写分离

**参考实现**：shardingsphere(readwrite-splitting) 源码验证 + docs(MySQL 操作)。

**对比取舍**：知识本体是"**读写分离设计**"。核心洞察：**主从复制、读写分离数据源(写源读副本)、三类路由、负载均衡**。为第 26 节 ShardingSphere 铺垫。

**待验证汇总**：
- microsphere 读写分离具体场景
- Redis 读写分离数据源实现
- MySQL Connector/J(无本地源码)

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 MySQL 主从 + 骨架节 + shardingsphere 源码验证；补全聚焦"读写分离的工程价值"。

### 完整认知：读写分离在真实架构中完整该讲什么

docs 覆盖了主从复制/数据源/Connector/J。作为架构师，这个主题完整还该包含：

1. **读写分离是数据库扩展第一招**：读多写少场景下，主从复制 + 读扩展——性价比最高的扩展方式
2. **主从延迟的一致性权衡**：异步复制有延迟——事务内走主、强制主、最终一致读——一致性关键
3. **读写分离数据源设计**：写源/读副本分组 + 路由策略(标准/事务内主/强制主) + 负载均衡——ShardingSphere 范式
4. **与分库分表关系**：读写分离扩展读，分库分表扩展容量——常结合使用(ShardingSphere 一体)
5. **Connector/J 高可用**：JDBC 层多主机连接/负载均衡/故障转移——驱动级方案
6. **Redis 读写分离**：主从复制 + 读从——缓存层扩展
7. **读写分离的局限**：写仍是单点、延迟一致性——高写场景需分库分表/分布式

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 读写分离 vs 分库分表 | 扩展读 vs 扩展容量 |
| 异步复制 | 高性能；主从延迟 |
| 事务内走主 vs 读副本 | 强一致；读扩展 |
| 路由策略 | 标准/事务内主/强制主 |
| 方法/注解 vs SQL 分析切换 | 手动可控；自动但复杂 |

### 常见坑/反模式

1. **主从延迟读旧数据**：异步复制延迟——事务内走主/强制主
2. **写走副本**：路由错导致写副本(只读失败)——正确路由
3. **强制主滥用**：全部走主失去读写分离意义——按需
4. **忽略复制监控**：主从断裂无感知——复制状态监控
5. **读扩展极限**：单库写瓶颈——需分库分表(第 26 节)

### 生态位置

- **性能优化维度**：读写分离是**数据库扩展基础**——承接第 13 节 JDBC，为第 26 节 ShardingSphere(分库分表+读写分离) 铺垫
- **衔接**：JDBC(第 13 节) → 读写分离(本篇) → ShardingSphere(第 26 节)
- **与源码提取的关系**：shardingsphere readwrite-splitting 模块是核心源码

**架构师视角结论**：本篇不只是背主从复制，而是"**理解读写分离的设计与权衡**"——主从复制(异步/GTID)、读写分离数据源(写源读副本+三类路由+负载均衡)、主从延迟一致性权衡；这是数据库读扩展的基础，也是 ShardingSphere 分库分表的前置。
