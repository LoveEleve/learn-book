# stage-3 · 第 12 节：第十节："高并发、高性能" 数据存储 — 知识点提取

> 课程：stage-3 三高架构 第 12 节（数据组收官 12/12）
> 来源 docs：`/data/workspace/java-training-camp/stage-3/docs/12. 第十节："高并发、高性能"数据存储.md`
> 提取时间：2026-08-12 | 权重：核心（数据存储三主题——SQL 优化/MyBatis/JPA/分库分表）
> 案例载体：my-xhs（决策 B）+ 现状核对（决策 B2）

---

## 一、本节概览

- **技术域**：MyBatis 架构（配置/核心 API/Spring 集成/Mapper 代理）、JPA 实体模型、SQL 优化（外键/索引）、ShardingSphere 分库分表
- **维度**：`[工程问题]`（MyBatis/JPA 框架）+ `[性能优化]`（SQL 优化）+ `[分布式问题]`（分库分表）
- **核心命题**：**数据存储优化三主题**（docs 主要内容）——①SQL 优化（去外键/索引）②MyBatis 替换 JPA（SQL 效率）③ShardingSphere 分库分表；docs 的 ShardingSphere 为**空节**，知识本体在 stage-2 26 交叉引用 + my-xhs 实证
- **知识点数**：8 个
- **前置**：stage-2 26（ShardingSphere 分片/读写分离——docs 主题交叉）、11 篇（读写分离）、JDBC 基础

## 前置条件清单
读者需先掌握：
1. **ShardingSphere 数据分片**（stage-2 26——docs 主题的机制本体）
2. **读写分离**（11 篇 KP-04——my-xhs 路由拦截器衔接）
3. **JDBC 基础**（Connection/Statement/PreparedStatement）
未达前置者，先补：stage-2 26

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **源码强**：docs 内嵌 MyBatis 源码（DefaultSqlSession/newExecutor/RoutingStatementHandler/Mapper 代理）→ 对照 my-xhs Interceptor 实证
- **实例锚定**：my-xhs MyBatis-Plus + SQL 守护/路由拦截器 + ShardingSphere 订单模块
- **docs 场景 vs 现状**：docs 的"MyBatis 替换 JPA"在 my-xhs 是既成事实（MyBatis-Plus）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 SQL 优化（去外键/级联/索引构建）【docs 主要内容①】
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：SQL/索引基础
- **来源**：docs §主要内容 + my-xhs sql 实证 + 架构师发散
- **需求**：掌握**关系表优化的两个动作**——减少外键/级联（写路径代价）+ 合理索引（读路径）
- **自主实现**：若我设计——外键约束交给应用层（去 DB 级级联）；按查询模式建索引（WHERE/JOIN/排序列）
- **参考实现**（docs 意图 + my-xhs 实证 + 发散）：**docs 主要内容①**——"减少 Shopizer 关系表之间的外键、级联关系，以及合理构建 SQL 索引"；**机制（发散）**——外键/级联代价：插入更新校验 + 级联操作锁范围大（性能瓶颈）→ **数据一致性移到应用层/事件（11 篇 CDC/14 节事件）**；**索引**——覆盖索引/复合索引顺序（最左前缀）；**my-xhs 实证**——`sql/init-all.sql` 建表：**无 FOREIGN KEY**（grep 实证——docs 目标①已实践）+ 索引规范（`KEY I_trigger_time`/`UNIQUE KEY i_username` 等实证）
- **对比取舍**：**DB 级外键 vs 应用层一致性**——约束保证 vs 性能/灵活性——现代高并发默认去 DB 外键（my-xhs 实践）
- **测试佐证**：docs §主要内容① + my-xhs `sql/init-all.sql`（无外键 + 索引实证）

### KP-02 MyBatis 架构与配置体系（四组件/全局配置 11 元素）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：JDBC
- **来源**：docs §MyBatis 简介 + §MyBatis 配置（全局 XML 全文）+ §核心 API（源码片段）
- **需求**：掌握 MyBatis 的**核心架构**——SqlSession/Executor/StatementHandler/ResultSetHandler 四组件链 + 全局配置体系
- **自主实现**：若我设计——SqlSession 门面（封装 Connection/Statement 细节）→ Executor（执行器：简单/复用/批处理/缓存包装）→ StatementHandler（三种 Statement 适配）→ ResultSetHandler（结果映射）
- **参考实现**（docs 源码 + 发散）：**简介（docs）**——一流持久化框架，消除 JDBC 样板代码；iBatis 为前身；**全局配置 11 元素（docs）**——properties/settings/typeAliases/typeHandlers/objectFactory/objectWrapperFactory/reflectorFactory/plugins/environments/databaseIdProvider/mappers（Configuration 类为组装 API）；**properties 三方式（docs）**——property 元素/properties resource/SqlSessionFactoryBuilder.build(reader, props)；**typeHandlers**——JDBC↔Java 类型转换（BooleanTypeHandler 示例）；**JSR-310 支持**——docs 需单独引 `mybatis-typehandlers-jsr310` `[过时→MyBatis 3.4+ 已内建 JSR-310 处理器，无需额外依赖]`；**environments**——多环境类似 Maven/Spring Profile；**databaseIdProvider**——按数据库厂商选 SQL（MySQL/Oracle）；**mappers**——XML 映射 vs Annotation 映射；**主键生成器（KeyGenerator，docs §核心 API 末节）**——自增/序列等主键策略的生成器接口
- **对比取舍**：**MyBatis（SQL 可控）vs 全自动 ORM（Hibernate/JPA）**——SQL 精细控制 vs 对象便利——docs 主要内容②的动机（SQL 效率）
- **测试佐证**：docs §配置/§核心 API（源码片段）+ my-xhs（MyBatis-Plus 依赖，03 篇）

### KP-03 MyBatis 插件机制（Interceptor 四组件拦截）【my-xhs 实证重点】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-02
- **来源**：docs §插件（plugins）+ my-xhs Interceptor 实证
- **需求**：掌握 MyBatis 的**横切面**——Interceptor 拦截 Executor/ParameterHandler/ResultSetHandler/StatementHandler（SQL 治理的落点）
- **自主实现**：若我设计——SQL 级横切：路由（读写分离）/守卫（慢 SQL 熔断）/审计/分页——全部用 Interceptor
- **参考实现**（docs 拦截点 + my-xhs 实证）：**docs 四拦截点**——Executor（update/query/flushStatements/commit/rollback/getTransaction/close/isClosed）、ParameterHandler（getParameterObject/setParameters）、ResultSetHandler（handleResultSets/handleOutputParameters）、StatementHandler（prepare/parameterize/batch/update/query）；**my-xhs 实证（两个 Interceptor 即 docs 机制的工程价值）**——①`common/aspect/ReadWriteRoutingInterceptor.java`（**Executor 层拦截**：注释实证"MyBatis 执行顺序 Executor.query/update → getConnection() → StatementHandler.prepare()——**在 StatementHandler.prepare 拦截时连接已获取，路由无效**；**事务安全**：活跃写事务不设 SLAVE——事务内 SELECT 与 INSERT/UPDATE 必须共用同一连接"）——**11 篇 KP-04"SQL 分析自动路由"的实现类**②`common/aspect/SqlGuardInterceptor.java`（**SQL 守护**：慢 SQL 检测 >200ms WARN + 指标；**连续 5 次慢 SQL → 熔断**（阻塞执行返回空）；冷却 30s 自动恢复）
- **对比取舍**：**Interceptor 拦截 vs 代码内散落 SQL 治理**——横切集中 vs 侵入业务——MyBatis 插件机制是 SQL 治理的标准落点（my-xhs 路由+守护两例示范）
- **测试佐证**：docs §插件（四拦截点方法清单）+ my-xhs `ReadWriteRoutingInterceptor.java`（类注释 Executor 层/事务安全）+ `SqlGuardInterceptor.java`（200ms/5 次/30s 实证）

### KP-04 MyBatis-Spring 集成（SqlSessionTemplate/事务绑定/Mapper 注册）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：Spring 事务（stage-2 13/14）
- **来源**：docs §MyBatis Spring（源码片段）
- **需求**：掌握 MyBatis 与 Spring 的**集成契约**——SqlSession 与事务的绑定（1:1:1:1:1 链）
- **自主实现**：若我设计——SqlSessionTemplate（线程安全代理）→ SqlSessionUtils 按事务同步资源取/建 SqlSession（事务内复用同一连接）
- **参考实现**（docs 源码 + 发散）：**对象链（docs）**——1 Mapper 关联 1 SqlSession → 1 Executor → 1 Transaction → 1 Connection；**SqlSessionFactoryBean**——最终由 DefaultSqlSessionFactory 构建；**SqlSessionTemplate**——`sqlSessionProxy`（`newProxyInstance` + `SqlSessionInterceptor` 内嵌代理——docs 源码）；**补充实现（docs 提及）**——`SqlSessionManager`（会话管理器实现）/`ManagedTransaction`（受管事务——容器/第三方管理连接，对照 JdbcTransaction 自管）`[跳过：补充实现细节，机制已含主线]`；**SqlSessionUtils.getSqlSession**——`TransactionSynchronizationManager.getResource(sessionFactory)` 取 holder（**事务内复用 SqlSession**——与 Spring 事务绑定，docs 源码：`registerSessionHolder`）；**MapperFactoryBean**——Mapper Bean 依赖 SqlSessionTemplate；**MyBatis Generator**（docs）——代码生成器（工具，`[有效]`）
- **对比取舍**：**模板代理 vs 原生 SqlSession 手管**——线程安全/事务集成 vs 简单——Spring 场景必须 Template
- **测试佐证**：docs §MyBatis Spring（SqlSessionTemplate/SqlSessionUtils 源码片段）

### KP-05 Mapper 动态代理（MapperProxyFactory/MapperProxy/MapperMethod）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：JDK 动态代理
- **来源**：docs §MapperRegistry/MapperProxyFactory/MapperProxy/MapperMethod（源码全文）
- **需求**：理解 Mapper 接口 → 代理 → SQL 执行的三层映射
- **自主实现**：若我设计——接口注册（MapperRegistry）→ 代理工厂（MapperProxyFactory）→ 方法分派（MapperProxy.invoke → MapperMethod.execute 按 SQL 类型分发）
- **参考实现**（docs 源码）：**MapperRegistry.addMapper**——接口注册（已注册抛 BindingException）+ `MapperProxyFactory` 入 knownMappers + `MapperAnnotationBuilder` 解析注解；**getMapper**——`factory.newInstance(sqlSession)`（Proxy.newProxyInstance 动态代理）；**MapperProxy.invoke**——Object 方法直调，否则 `cachedInvoker(method).invoke`（**DefaultMethodInvoker**（接口 default 方法）/ **PlainMethodInvoker**（反射 → MapperMethod））；**MapperMethod.execute**——INSERT/UPDATE/DELETE/SELECT（returnsVoid+ResultHandler/returnsMany/returnsMap/returnsCursor/selectOne+Optional 包装）/FLUSH 分发（docs 源码全文）
- **对比取舍**：**接口代理 vs 手写 DAO 实现**——声明式 SQL 绑定 vs 样板代码——MyBatis 的核心便利
- **测试佐证**：docs §Mapper 代理（addMapper/getMapper/invoke/cachedInvoker/execute 源码）+ my-xhs `mapper/UserMapper.java`（@Mapper/BaseMapper 实证）

### KP-06 JPA 实体模型与 MyBatis vs JPA 选型【docs 主要内容②】
- **维度**：`[规范]` + `[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[过时→jakarta.persistence（命名空间迁移）]` | **置信度**：High
- **前置**：ORM 概念
- **来源**：docs §JPA（介绍/历史/核心概念）+ §主要内容② + my-xhs 现状
- **需求**：掌握 JPA 实体模型约束 + **MyBatis vs JPA 的选型**（docs 主要内容②：MyBatis 部分替换 JPA 提升 SQL 效率）
- **自主实现**：若我设计——复杂查询/SQL 优化场景选 MyBatis（SQL 可控）；简单 CRUD/对象图场景 JPA 便利——**混合策略**（docs 的"部分替换"）
- **参考实现**（docs + my-xhs 现状）：**JPA 历史（docs）**——1.0 整合 Query+ORM 元数据；2.0 加 Criteria/元数据 API/校验；2009 JSR-317（EJB 3.0 子规范）——**命名空间迁移**：javax.persistence → jakarta.persistence（04 §3.2.1）；**实体类约束 5 条（docs）**——@Entity 标注或 XML/至少一个 public/protected 默认构造器/顶级类（非枚举接口）/禁止 final/支持继承多态；**持久字段类型**——原生/Serializable/自定义/枚举/实体/嵌入类型；**选型（docs 主要内容②意图 + 发散）**——Shopizer 用 JPA（Hibernate）→ MyBatis 替换提升 SQL 效率（**SQL 精确控制/性能可预测**）；**my-xhs 现状**——**已用 MyBatis-Plus**（mybatis-plus-spring-boot3-starter 实证，03 篇）——**docs 的"升级"在 my-xhs 是既成事实**（JPA → MyBatis-Plus 全量采用，无 JPA）
- **对比取舍**：**MyBatis（SQL 第一）vs JPA（对象第一）**——可控/高效 vs 自动/便利——国内主流 MyBatis 系（电商/金融 SQL 复杂场景）；docs 的"部分替换"= 混合策略
- **测试佐证**：docs §JPA（实体约束/历史）+ my-xhs（MyBatis-Plus 实证，03 篇）

### KP-07 分库分表（ShardingSphere——docs 空节 + my-xhs 订单实证 + stage-2 26）【docs 主要内容③】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：stage-2 26（ShardingSphere 分片/读写分离）
- **来源**：docs §ShardingSphere（**空节：仅标题**）+ my-xhs 实证 + stage-2 26 交叉引用
- **需求**：掌握**分库分表的落地**——docs 主要内容第 3 条"基于 ShardingSphere 重构订单、商品等应用数据"（docs 空节，机制本体在 stage-2 26 + my-xhs 订单模块实证）
- **自主实现**：若我设计——分片键选择（订单号/用户 ID）+ 分片算法（Hash/时间）+ 未配置规则的表透传默认数据源
- **参考实现**（my-xhs 实证 + stage-2 26 交叉引用）：**my-xhs 落地（订单/优惠券模块）**——`my-xhs-order/pom.xml:44-45`（shardingsphere-jdbc）+ `my-xhs-coupon/pom.xml:81-82`（shardingsphere-jdbc-core）+ 根 pom:83（版本 5.5.1）+ **`my-xhs-order/.../config/ShardingSphereDataSourceConfig.java:36/41/49`**（`YamlShardingSphereDataSourceFactory` 从 `sharding-config.yaml` 建数据源；`jdbc:shardingsphere:` 协议；**Snowflake worker-id 配置**；**排除 MybatisPlusAutoConfiguration 手动建 SqlSessionFactory**——呼应 OrderApplication:26 exclude（06 篇）；**未配置分片规则的表（如 t_id_segment 号段表）透传默认数据源**——注释实证）+ Cosid（`me.ahoo.cosid` 雪花发号器——分片键 ID 生成配套）；**机制本体（stage-2 26 交叉引用）**——数据分片（表/库）/读写分离/ShardingSphere-JDBC 客户端形态——不重复提取；**docs 空节标注**——§ShardingSphere 仅标题，知识本体已在 stage-2 26 提取 + 本篇 my-xhs 实例补全
- **对比取舍**：**ShardingSphere-JDBC（客户端内嵌）vs Proxy（中间件）**——零额外组件/性能好 vs 透明集中（stage-2 26 已述）——my-xhs 选 JDBC 形态（YamlShardingSphereDataSourceFactory）
- **测试佐证**：my-xhs `ShardingSphereDataSourceConfig.java`（工厂/协议/透传注释实证）+ order/coupon pom + 根 pom:83 + stage-2 26 交叉引用

### KP-08 数据存储优化全景（SQL/框架/分片三层 + 现状核对）
- **维度**：`[性能优化]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01/06/07
- **来源**：docs §主要内容（3 条）+ 架构师整合
- **需求**：掌握数据存储优化的**三层结构**——SQL 层（外键/索引）→ 框架层（MyBatis SQL 可控）→ 架构层（分库分表）
- **自主实现**：若我设计——先 SQL/索引优化（零架构成本）→ 框架选型（SQL 可控）→ 数据量超单库再分片（最后手段）
- **参考实现**（docs 三主题 + my-xhs 现状 + 发散）：**三层（docs 主要内容映射）**——①SQL 优化（KP-01：去外键+索引）②框架升级（KP-06：MyBatis 替换 JPA）③分库分表（KP-07：ShardingSphere）；**优化次序（发散）**——**先 SQL 后分片**（索引/慢 SQL 是最大收益面——my-xhs SqlGuardInterceptor 慢 SQL 治理实证）；**my-xhs 全景（现状核对）**——①无外键+索引规范 ✅ ②MyBatis-Plus 全量（JPA 无）✅ ③ShardingSphere 订单/优惠券 ✅——**docs 三主题在 my-xhs 全部落地**
- **对比取舍**：**分片是最后手段**——单库能扛不拆（分片复杂度高：跨片查询/分布式 ID/扩容）——docs 三主题顺序即优化次序
- **测试佐证**：docs §主要内容 + my-xhs（KP-01/06/07 实证汇总）

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| SQL 优化（去外键/索引） | 性能优化 | 核心 | P1 | 🟡 | 时间无关 | High |
| MyBatis 架构与配置体系 | 工程问题 | 核心 | P1 | 🔴 | 有效 | High |
| MyBatis 插件机制（Interceptor） | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| MyBatis-Spring 集成 | 工程问题 | 支撑 | P2 | 🟡 | 有效 | High |
| Mapper 动态代理 | 工程问题 | 支撑 | P2 | 🟡 | 时间无关 | High |
| JPA 实体模型与选型 | 规范/工程 | 支撑 | P2 | 🟡 | 过时→jakarta | High |
| 分库分表（ShardingSphere） | 分布式问题 | 核心 | P1 | 🔴 | 有效 | High |
| 数据存储优化全景（三层） | 性能优化 | 支撑 | P2 | 🟡 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：my-xhs（MyBatis-Plus/Interceptor/ShardingSphere）+ stage-2 26（ShardingSphere 机制）
- **关键源码**（本次实证）：
  - `common/aspect/ReadWriteRoutingInterceptor.java`（**Executor 层拦截**——类注释：拦截时机/事务安全"活跃写事务不设 SLAVE"）+ `common/aspect/SqlGuardInterceptor.java`（**慢 SQL 200ms/5 次熔断/30s 冷却**）
  - `my-xhs-order/.../config/ShardingSphereDataSourceConfig.java:36/41/49`（YamlShardingSphereDataSourceFactory/sharding-config.yaml/jdbc:shardingsphere 协议/Snowflake worker-id/未配置表透传默认源）+ order/coupon pom（shardingsphere-jdbc/-core）+ 根 pom:83（5.5.1）+ Cosid
  - `sql/init-all.sql`（无外键 + 索引规范实证）+ `mapper/UserMapper.java`（MyBatis-Plus）
- **诚实标注**：docs §ShardingSphere 为**空节**（仅标题）→ KP-07 以 my-xhs 实证 + stage-2 26 交叉引用补全；docs JPA 历史为 2009 时代（javax→jakarta 迁移≠机制）；docs 的 mybatis-typehandlers-jsr310 单独依赖 [过时→3.4+ 内建]
- **关联标注**：stage-2 26（ShardingSphere 分片机制——docs 主题本体）；11 篇（读写分离——ReadWriteRoutingInterceptor 即其实现）；06 篇（OrderApplication exclude 呼应）；03 篇（MyBatis-Plus）

---

## 五、本节小结（三层次视角）

**需求**：数据存储优化三主题——SQL 优化（去外键/索引）、MyBatis 替换 JPA（SQL 效率）、ShardingSphere 分库分表。

**自主实现核心**：若我设计——①去 DB 外键（应用层一致性）+ 覆盖索引 ②MyBatis（SQL 可控）+ Interceptor 横切（路由/慢 SQL 熔断）③数据量超限才分片（ShardingSphere-JDBC + 雪花 ID）。

**参考实现**：docs（MyBatis 架构源码/JPA 约束）+ **my-xhs 三主题全落地实证**（无外键、MyBatis-Plus、ShardingSphere 订单模块 + 两个 Interceptor）。**已源码验证，非只看 docs**。

**对比取舍**：知识本体是"**数据存储优化的三层**"——SQL 层（外键/索引）→ 框架层（SQL 可控 + 插件治理）→ 架构层（分片最后手段）；docs 三主题在 my-xhs 全部落地（B 模式核对最顺的一篇）。

**待验证汇总**：
- `sharding-config.yaml` 的分片键/算法细节（订单表分片规则）
- Cosid 与 ShardingSphere Snowflake 的配合方式
- my-xhs 慢 SQL 熔断的实际触发记录（SqlGuardInterceptor 运行数据）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 目标（升级动作） | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| ① SQL 优化（去外键/级联/索引） | ✅ **已实践**：`sql/init-all.sql` 无 FOREIGN KEY（grep 实证）+ 索引规范（I_trigger_time/i_username 等） | 无（已按 docs 目标①执行） |
| ② MyBatis 替换 JPA | ✅ **既成事实**：MyBatis-Plus 全量（mybatis-plus-spring-boot3-starter + UserMapper 实证），无 JPA | 无（docs 的"升级"在 my-xhs 已完成） |
| ③ 分库分表（ShardingSphere） | ✅ **订单/优惠券已重构**：ShardingSphereDataSourceConfig（Yaml 工厂 + jdbc:shardingsphere 协议 + Snowflake + 未配置表透传）+ order/coupon pom 依赖 + Cosid | 商品等其余模块未分片 `[待验证：是否计划扩展]`——现状：按需分片（订单/优惠券先行合理） |
| SQL 治理（docs 未提，my-xhs 超出） | ✅ SqlGuardInterceptor（慢 SQL 200ms/熔断）+ ReadWriteRoutingInterceptor（Executor 层路由） | 无（超出 docs 目标的治理能力） |

### 差距清单（数据存储层）

1. **P2**：分片规则细节核对（sharding-config.yaml 的分片键/算法——订单表如何分）
2. **P2**：慢 SQL 熔断的监控告警联动（SqlGuardInterceptor 指标是否进 Prometheus——03 篇核对）
3. **P3**：商品/库存等模块分片评估（当前单库 + 主从足够则维持）

**结论**：12 篇数据存储**三主题全部落地**（docs 目标在 my-xhs 全部达成）——加上 11 篇（读写分离/主从），数据组是落地度最高的组；差距集中在细节核对（分片规则/熔断监控联动）。

---

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 MyBatis/JPA 架构文档转写（源码片段丰富）+ ShardingSphere 空节；my-xhs 实证；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：数据存储优化的完整认知该讲什么

docs 覆盖 MyBatis 架构与三主题。完整还该包含：

1. **"先 SQL 后分片"的优化次序**（docs 三主题顺序 + 发散）：索引/慢 SQL 是最大收益面（my-xhs SqlGuardInterceptor 治慢 SQL）→ 框架 SQL 可控 → 分片是**最后手段**（跨片查询/分布式 ID/扩容复杂度）；docs 三主题顺序即此次序
2. **MyBatis Interceptor 是 SQL 治理的标准落点**（docs 拦截点 + my-xhs 实证）：路由（读走从）、守卫（慢 SQL 熔断）、审计、分页——**横切集中 vs 业务散落**；my-xhs 两例（Executor 层路由 + 熔断）示范了插件机制的生产价值；**Executor 层拦截的时机学问**（my-xhs 注释实证：getConnection() 之前设路由标记——拦截层选错则路由无效）
3. **去 DB 外键的"一致性债"**（docs ① + 发散）：外键没了 → 数据一致性移到应用层/事件/CDC（11 篇 Canal + 14 节事件）——**去外键不是删约束，是换实现层**；孤儿数据靠应用校验 + 定时对账
4. **分片的配套工程**（docs ③空节 + my-xhs 实证）：分片键选择（订单号/用户 ID——热点分布）、**分布式 ID（Snowflake/Cosid）**、未配置表透传、**SqlSessionFactory 手动装配**（MyBatis-Plus 与 ShardingSphere 的兼容处理——my-xhs 排除自动配置实证）——**分片是"数据架构升级"，不止加依赖**
5. **MyBatis vs JPA 是生态选型**（docs ② + 发散）：国内 SQL 复杂场景（电商/金融）主流 MyBatis 系；JPA 在对象图/标准场景有优势——**"部分替换"= 按模块混合**（docs 原文）
6. **SQL 守护是生产必修**（my-xhs 超出 docs）：慢 SQL 检测 + 熔断（5 次/30s 冷却）——**防慢 SQL 拖垮连接池**（05 篇 Tomcat/连接池参数的前提保护）

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| DB 外键 vs 应用层一致性 | 约束保证 vs 性能/灵活（my-xhs 去外键） |
| MyBatis vs JPA | SQL 可控 vs 对象便利（docs ②部分替换） |
| Interceptor 横切 vs 业务内治理 | 集中 vs 侵入（my-xhs 路由+守护） |
| JDBC 客户端分片 vs Proxy | 零组件 vs 透明（my-xhs 选 JDBC 形态） |
| 分片 vs 单库+主从 | 扩展 vs 复杂度（分片最后手段） |
| 手动 SqlSessionFactory vs 自动配置 | 兼容分片 vs 省事（my-xhs 实证） |

### 常见坑/反模式

1. **先分片后优化 SQL**：索引/慢 SQL 没治就拆库——复杂度白上（docs 顺序）
2. **无 SQL 治理**：慢 SQL 打满连接池——SqlGuard 类守护必修（my-xhs 示范）
3. **分片无分布式 ID**：自增 ID 跨片冲突——Snowflake/Cosid（my-xhs 实证）
4. **拦截层选错**：StatementHandler 拦截时连接已获取——路由无效（my-xhs 注释实证 Executor 层）
5. **分片规则表不透传**：号段/字典表误分片——未配置表透传默认源（my-xhs 注释实证）
6. **事务内 SELECT 走从库**：读写分离与事务连接冲突——活跃写事务不设 SLAVE（my-xhs 注释实证）
7. **MyBatis-Plus 与 ShardingSphere 自动配置冲突**：不排除自动配置 → 数据源被覆盖（my-xhs exclude 实证）

### 生态位置

- **stage-3 教学主线**：数据组（11/12）**收官**——11 高可用（主从/读写分离/MGR）+ **12 数据存储（本篇）**——数据层双线闭环；13-17 转入事件/Reactive 组
- **前后篇衔接**：stage-2 26（ShardingSphere 分片机制）→ 本篇（订单模块落地）；11 篇（读写分离——ReadWriteRoutingInterceptor 是其实现）；03 篇（MyBatis-Plus/监控）；14 节（分布式事件——CDC/去外键的一致性实现层）
- **与源码提取的关系**：my-xhs 数据层（Interceptor/ShardingSphere 配置）为核心参考源；MyBatis（docs 内嵌源码）机制层

**架构师视角结论**：本篇以 **docs 讲 MyBatis 架构**（四组件/插件/Mapper 代理）、**my-xhs 讲落地**（三主题全落地 + 超出 docs 的 SQL 治理）——知识本体是"**数据存储优化三层**"（SQL 层→框架层→架构层）；my-xhs 是 docs 三主题的完整现代实例（无外键/MyBatis-Plus/ShardingSphere 订单），数据组（11/12）至此收官，下篇转入加餐/事件组（13-17：Spring Web Reactive、分布式事件设计）。
