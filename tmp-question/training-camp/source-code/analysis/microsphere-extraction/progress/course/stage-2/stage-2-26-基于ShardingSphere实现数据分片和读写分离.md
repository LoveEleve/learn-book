# stage-2 · 第 26 节：基于 ShardingSphere 实现数据分片和读写分离 — 知识点提取

> 课程：stage-2 模式设计与实现 第 26 节
> 来源 docs：`/data/workspace/java-training-camp/stage-2/docs/26. 第二十六节：基于 ShardingSphere 实现数据分片和读写分离.md`
> 提取时间：2026-08-11 | 权重：核心（ShardingSphere 主线，源码级）

---

## 一、本节概览

- **技术域**：ShardingSphere（数据分片/读写分离/架构设计/JDBC 扩展点）
- **维度**：`[性能优化]`（分片/读写分离）+ `[工程问题]`（架构/SPI/配置，源码级）+ `[分布式问题]`（分布式事务/数据迁移）
- **核心命题**：理解 ShardingSphere——分库分表/读写分离能力、架构设计(规则/配置/模式)、JDBC 扩展
- **知识点数**：10 个
- **前置**：第 25 节读写分离、第 21 节 SPI、JDBC/DataSource

## 前置条件清单
读者需先掌握：
1. **读写分离**（第 25 节）
2. **分库分表概念**
3. **JDBC/DataSource**（第 13 节）
4. **SPI 机制**（第 1 节工程化）
未达前置者，先补：第 25/13 节 + 分库分表基础

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 已确认
讲解策略（源码节）：
- **源码理解强**：ShardingSphere 架构/规则/SPI 对照源码讲
- **工程化弱**：YAML 配置/分片策略补基础
- **必做**：对照 `code/spring/shardingsphere` 完整源码验证（08 教训）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 MySQL JDBC Driver 扩展点（Interceptor/Property）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：JDBC
- **来源**：docs §MySQL JDBC Driver
- **需求**：了解 MySQL JDBC 驱动扩展点（拦截器）
- **自主实现**：若我设计——ConnectionLifecycleInterceptor/ExceptionInterceptor/QueryInterceptor
- **参考实现**（docs）：MySQL JDBC 扩展点——**ConnectionLifecycleInterceptor**(连接生命周期拦截器)/**ExceptionInterceptor**(异常拦截器)/**QueryInterceptor**(命令拦截器)；**URL 属性**/`JdbcPropertySet`(属性集合)——docs §URL 属性/JdbcPropertySet(20-22 行)仅标题(空节标注，08 §2)，架构师发散：URL 属性经 `PropertyKey` 枚举(第 25 节) 解析，JdbcPropertySet 聚合全部 JDBC 属性
- **对比取舍**：**JDBC 拦截器扩展**——连接/异常/命令三类拦截器，驱动级扩展
- **测试佐证**：docs 类名（MySQL 驱动无本地源码，标注）

### KP-02 ShardingSphere 介绍（Database Plus 哲学）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]`（ShardingSphere 当前主流） | **置信度**：High
- **前置**：数据库
- **来源**：docs §ShardingSphere 介绍
- **需求**：理解 ShardingSphere 定位与哲学
- **自主实现**：若我设计——Database Plus：异构数据库上层标准/生态
- **参考实现**（docs）：**Apache ShardingSphere**——分布式数据库生态系统，把任意数据库转分布式数据库，通过**数据分片/弹性伸缩/加密**增强；**设计哲学 Database Plus**——构建异构数据库上层的标准和生态，关注利用数据库计算/存储能力(非新数据库)，站数据库上层视角
- **对比取舍**：**Database Plus vs 新数据库**——增强现有数据库而非替代
- **测试佐证**：`code/spring/shardingsphere` 源码

### KP-03 类似产品 + 两种形态（TDDL/JDBC/Proxy）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-02
- **来源**：docs §类似产品 + §两种形态
- **需求**：了解类似产品与 ShardingSphere 两种形态
- **自主实现**：若我设计——JDBC 框架(轻量) + Proxy 代理(透明)
- **参考实现**（docs）：**类似产品**——Alibaba TDDL(分库分表)；**ShardingSphere-JDBC**——轻量级 Java 框架，**JDBC 层提供额外服务**；**ShardingSphere-Proxy**——透明化数据库代理端，实现**数据库二进制协议**，对异构语言支持
- **对比取舍**：**JDBC vs Proxy**——JDBC 轻量(Java 应用)；Proxy 透明(异构语言)
- **测试佐证**：`code/spring/shardingsphere`(jdbc 模块)

### KP-04 产品功能（分片/事务/读写分离/迁移/联邦/加密/影子库）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-02
- **来源**：docs §产品功能
- **需求**：了解 ShardingSphere 七大功能
- **自主实现**：若我设计——分片/事务/读写分离/迁移/联邦/加密/影子库
- **参考实现**（docs）：七大功能——**数据分片**(海量数据水平扩展)/**分布式事务**(XA+BASE 混合引擎)/**读写分离**(SQL 语义理解+拓扑感知)/**数据迁移**(跨源+重分片)/**联邦查询**(跨源关联聚合)/**数据加密**(透明安全)/**影子库**(压测隔离)
- **对比取舍**：**一站式数据库增强**——分片/事务/迁移/加密/影子库全覆盖
- **测试佐证**：docs 功能表 + 源码 features/ 模块

### KP-05 产品优势（性能/兼容/零侵入/运维）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-02
- **来源**：docs §产品优势
- **需求**：了解 ShardingSphere 优势
- **自主实现**：无（优势清单）
- **参考实现**（docs）：优势——**极致性能**(驱动端接近原生 JDBC)/**生态兼容**(Proxy 支持任意协议，JDBC 对接任意 JDBC 数据库)/**业务零侵入**(平滑迁移)/**运维低成本**/**安全稳定**/**弹性扩展**(在线扩展)/**开放生态**(插件化)
- **对比取舍**：**零侵入+插件化**——业务无改造，多层次插件化
- **测试佐证**：docs 优势清单

### KP-06 架构设计·配置（YAML/Properties/RuleConfiguration/Swapper）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：YAML
- **来源**：docs §架构设计·配置 + 源码验证
- **需求**：理解 ShardingSphere 配置设计
- **自主实现**：若我设计——YAML/Properties 配置 + YamlRuleConfigurationSwapper 转换
- **参考实现**（docs + 源码）：**配置类型**——YAML/Properties；**YamlConfiguration**/`RuleConfiguration`(规则配置)；**YamlRuleConfigurationSwapper**——`YamlRuleConfiguration <-> RuleConfiguration`(源码 `infra/common/.../yaml/config/swapper/rule/YamlRuleConfigurationSwapper.java`)
- **对比取舍**：**配置交换器**——YAML 配置与规则配置双向转换
- **测试佐证**：源码 `infra/common/.../YamlRuleConfigurationSwapper.java`

### KP-07 规则 API（ShardingSphereRule）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-06、SPI
- **来源**：docs §规则 + 源码验证
- **需求**：定义规则统一 API
- **自主实现**：若我设计——ShardingSphereRule 接口(getConfiguration/getType)
- **参考实现**（docs + 源码）：`ShardingSphereRule` 接口(26)——`getConfiguration()`(33，关联规则配置)/`getType()`(规则类型)；实现——`ShardingRule`(85，分片规则，SPI 加载 `ShardingAlgorithm`/`KeyGenerateAlgorithm`/`ShardingAuditAlgorithm`)
- **对比取舍**：**规则 SPI**——规则统一接口 + SPI 加载算法(分片/主键/审计)
- **测试佐证**：源码 `infra/common/.../ShardingSphereRule.java`(26/33)+`features/sharding/.../ShardingRule.java`(85/120-122)

### KP-08 模式与存储仓库（Standalone/Cluster PersistRepository）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：元数据
- **来源**：docs §模式 + 源码验证
- **需求**：理解元数据存储模式
- **自主实现**：若我设计——Standalone(独立) vs Cluster(集群) 存储
- **参考实现**（docs + 源码）：**模式(Mode)**——元数据存储模式；**PersistRepository**(存储仓库 SPI)——`StandalonePersistRepository`(mode: Standalone，独立)/`ClusterPersistRepository`(mode: Cluster，集群)
- **对比取舍**：**单机 vs 集群元数据**——Standalone 独立存储；Cluster 集群共享(需注册中心)
- **测试佐证**：源码 `mode/api/.../PersistRepository.java`+`mode/type/standalone/.../StandalonePersistRepository.java`+`mode/type/cluster/.../ClusterPersistRepository.java`

### KP-09 数据源（DataSourceProperties/DataSourcePoolCreator）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：DataSource
- **来源**：docs §数据源 + 源码验证
- **需求**：管理数据源（属性/创建）
- **自主实现**：若我设计——DataSourceProperties(YAML↔POJO) + DataSourcePoolCreator(连接池创建)
- **参考实现**（docs + 源码）：**DataSourceProperties**——YAML 配置和当前 POJO 映射；**DataSourcePoolCreator**——数据源连接池创建器；`ShardingSphereDataSource`(50，统一数据源，`AbstractDataSourceAdapter`)
- **对比取舍**：**数据源抽象**——属性映射 + 连接池创建 + 统一数据源
- **测试佐证**：源码 `jdbc/.../ShardingSphereDataSource.java`(50)

### KP-10 路由器 + 分片策略（Router/ShardingStrategy）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-07、分片
- **来源**：docs §路由器（仅标题）+ shardingsphere 源码验证
- **需求**：理解分片路由与策略
- **自主实现**：若我设计——Router 按策略路由 + 分片策略配置
- **参考实现**（docs 仅标题 + 源码）：**Router**——docs 仅标题，源码补全：`ShardingRule` 中 SPI 加载分片算法；**分片策略配置**——`StandardShardingStrategyConfiguration`(标准)/`ComplexShardingStrategyConfiguration`(复合)/`HintShardingStrategyConfiguration`(强制路由)/`NoneShardingStrategyConfiguration`；`ComplexKeysShardingAlgorithm`(复合分片算法)
- **对比取舍**：**四类分片策略**——标准(单键)/复合(多键)/Hint(强制路由)/None(不分片)
- **测试佐证**：源码 `features/sharding/api/.../strategy/sharding/StandardShardingStrategyConfiguration.java`+`ComplexShardingStrategyConfiguration.java`+`HintShardingStrategyConfiguration.java`

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| MySQL JDBC 扩展点 | 工程问题 | 支撑 | P2 | 🟡 | 有效 | High |
| ShardingSphere 介绍(Database Plus) | 工程问题 | 核心 | P1 | 🔴 | 有效 | High |
| 类似产品 + 两种形态 | 工程问题 | 核心 | P1 | 🟡 | 有效 | High |
| 产品功能(七大) | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| 产品优势 | 工程问题 | 支撑 | P3 | 🟢 | 时间无关 | High |
| 架构·配置(Swapper) | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 规则 API(ShardingSphereRule) | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 模式与存储仓库 | 工程问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| 数据源 | 工程问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| 路由器 + 分片策略 | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：`code/spring/shardingsphere` 完整源码——ShardingSphereRule/YamlRuleConfigurationSwapper/PersistRepository(Standalone/Cluster)/ShardingRule/ShardingSphereDataSource/分片策略
- **关键源码类**（本次实证）：`ShardingSphereRule`(26/33)、`ShardingRule`(85，SPI 加载算法 120-122)、`StandardShardingStrategyConfiguration`/`ComplexShardingStrategyConfiguration`/`HintShardingStrategyConfiguration`、`ShardingSphereDataSource`(50)
- **诚实标注**：docs 的 MySQL JDBC Driver 类无本地源码
- **关联标注**：microsphere 用 ShardingSphere `[待验证]`；衔接第 25 节读写分离、第 22 节 RPC 生态

---

## 五、本节小结（三层次视角）

**需求**：用 ShardingSphere 实现数据分片和读写分离。

**自主实现核心**：若我设计——
1. Database Plus 哲学(增强现有数据库)
2. JDBC/Proxy 两种形态
3. 七大功能(分片/事务/读写分离/迁移/联邦/加密/影子库)
4. 架构：配置(YAML/Swapper) + 规则(ShardingSphereRule) + 模式(Standalone/Cluster) + 数据源
5. 分片策略(标准/复合/Hint/None)

**参考实现**：shardingsphere 源码(完整验证) + docs。

**对比取舍**：知识本体是"**ShardingSphere 分片与架构**"。核心洞察：**Database Plus、七大功能、规则 SPI、配置 Swapper、四类分片策略**。

**待验证汇总**：
- microsphere 用 ShardingSphere 的具体场景
- MySQL JDBC Driver(无本地源码)

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 ShardingSphere 介绍 + 架构骨架 + shardingsphere 源码验证；补全聚焦"ShardingSphere 的工程价值"。

### 完整认知：ShardingSphere 在真实架构中完整该讲什么

docs 覆盖了介绍/功能/架构配置。作为架构师，这个主题完整还该包含：

1. **ShardingSphere 是分库分表事实标准**：JDBC(轻量)/Proxy(透明) 双形态，国内主流——解决单库容量/性能
2. **Database Plus 哲学**：增强现有数据库而非替代——异构数据库上层标准
3. **分片策略选型**：标准(单键)/复合(多键)/Hint(强制路由)——按业务分片键设计
4. **读写分离 + 分片结合**：分片(容量) + 读写分离(读扩展)——第 25 节延续
5. **分布式事务引擎**：XA + BASE 混合——分布式事务(第 15-20 节)在分片场景的落地
6. **配置/规则/SPI 架构**：YamlRuleConfigurationSwapper/ShardingSphereRule/PersistRepository——可插拔内核
7. **与 TDDL 对比**：TDDL(阿里早期) → ShardingSphere(开源主流)

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| JDBC vs Proxy | 轻量(Java)；透明(异构语言) |
| 分片策略 | 标准/复合/Hint/None——按分片键 |
| Standalone vs Cluster 模式 | 独立；集群(注册中心) |
| 分片 vs 读写分离 | 容量扩展；读扩展 |
| 分布式事务 | XA 强一致；BASE 最终一致 |

### 常见坑/反模式

1. **分片键设计差**：无分片键导致全路由——按查询模式设计
2. **跨分片 JOIN/事务**：分布式查询/事务复杂——避免或分布式事务
3. **强制路由滥用**：Hint 全用失去分片意义
4. **元数据模式选错**：Cluster 需注册中心——按部署选
5. **忽略读写分离延迟**：分片+读写分离叠加，主从延迟更明显

### 生态位置

- **性能优化维度**：ShardingSphere 是**分库分表标准**——承接第 25 节读写分离、第 13 节 JDBC，衔接 stage-3/4
- **衔接**：读写分离(第 25 节) → ShardingSphere(本篇) → 分布式缓存(第 27-28 节)
- **与源码提取的关系**：shardingsphere 全源码是核心

**架构师视角结论**：本篇不只是背 ShardingSphere 功能，而是"**理解分库分表与数据库增强的标准方案**"——Database Plus 哲学、JDBC/Proxy 双形态、七大功能、规则 SPI + 配置 Swapper 架构、四类分片策略；这是海量数据场景的数据库扩展标准，也是读写分离(第 25 节)的完整形态。
