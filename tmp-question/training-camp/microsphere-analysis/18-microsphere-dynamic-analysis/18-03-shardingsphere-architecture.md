# 18-03 ShardingSphere 5.x 架构

## 目录

- [ShardingSphere 是什么](#shardingsphere-是什么)
- [ShardingSphere 的三层架构](#shardingsphere-的三层架构)
- [5.x vs 4.x 的核心变化](#5x-vs-4x-的核心变化)
- [Mode 体系：Memory / Standalone / Cluster](#mode-体系memory--standalone--cluster)
- [YAML 配置模型](#yaml-配置模型)
- [规则系统：RuleConfiguration 体系](#规则系统ruleconfiguration-体系)
- [ShardingSphere 与 Spring Boot 的集成](#shardingsphere-与-spring-boot-的集成)
- [在子上下文中的集成链路](#在子上下文中的集成链路)

---

## ShardingSphere 是什么

Apache ShardingSphere 是一个数据库中间层生态，包含三款产品：

| 产品 | 定位 | 方式 |
|------|------|------|
| ShardingSphere-JDBC | 轻量级 Java 框架 | 增强 JDBC 驱动，在应用层做分库分表/读写分离 |
| ShardingSphere-Proxy | 透明数据库代理 | 独立部署的服务端，对应用透明，兼容 MySQL/PostgreSQL 协议 |
| ShardingSphere-Sidecar (规划中) | Service Mesh 方式 | 通过 Sidecar 方式注入 |

18-dynamic 集成的是 **ShardingSphere-JDBC 5.x**。它是一个 JDBC 驱动层框架——不是独立服务，而是包装 `javax.sql.DataSource`，在 `getConnection()` 返回的 Connection 上拦截 SQL，做分库分表/读写分离/数据加密等操作。

### ShardingSphere-JDBC 的核心理念

```java
// 不使用 ShardingSphere
DataSource ds = new HikariDataSource();
ds.setJdbcUrl("jdbc:mysql://host:3306/db");
Connection conn = ds.getConnection();
// SQL 直接发给 MySQL（单库单表）

// 使用 ShardingSphere
DataSource actualDS0 = new HikariDataSource();  // 分片 0
DataSource actualDS1 = new HikariDataSource();  // 分片 1
// ...
ShardingSphereDataSource shardingSphereDataSource = ShardingSphereDataSourceFactory.create(
    Map.of("ds0", actualDS0, "ds1", actualDS1),
    rules, modeConfig
);
Connection conn = shardingSphereDataSource.getConnection();
// SQL 被 ShardingSphere 拦截，根据分片键改写：
// SELECT * FROM t_order WHERE order_id = ?  
// → 路由到 ds0 或 ds1
// → 改写为 SELECT * FROM t_order_0 WHERE order_id = ?  （按分片键路由到具体表）
```

所以 ShardingSphere 的本质是 **DataSource 包装器 + SQL 解析引擎**——它在标准 JDBC 接口后面封装了完整的 SQL 解析、路由、改写、执行、归并逻辑。

---

## ShardingSphere 的三层架构

ShardingSphere 5.x 的内部架构分为三层：

```
┌──────────────────────────────────────────────────┐
│                 应用代码 / ORM                     │
│         (MyBatis / JPA / JDBC 直接调用)           │
└──────────────────────┬───────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────┐
│  Layer 1: 接入层                                  │
│  (DataSource 接口 + Connection 接口 + JDBC 协议)   │
│                                                    │
│  ShardingSphereDataSource                          │
│  ShardingSphereConnection                          │
│  ShardingSphereStatement / PreparedStatement        │
│                                                    │
│  这一层负责：实现标准的 javax.sql 接口               │
│  让应用无感接入                                    │
└──────────────────────┬───────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────┐
│  Layer 2: 内核层                                  │
│  (SQL 解析 + 路由 + 改写 + 执行 + 结果归并)         │
│                                                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │ SQL 解析  │→│  路由    │→│  SQL 改写 │       │
│  │(Parser)  │  │(Router)  │  │(Rewriter)│       │
│  └──────────┘  └──────────┘  └──────────┘       │
│                      │                           │
│                      ▼                           │
│  ┌──────────┐  ┌──────────┐                      │
│  │ 结果归并  │←│  执行    │                      │
│  │(Merger)  │  │(Executor)│                      │
│  └──────────┘  └──────────┘                      │
│                                                    │
│  这一层负责：SQL 解析+路由+改写+执行+归并的全部逻辑  │
└──────────────────────┬───────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────┐
│  Layer 3: 存储层                                  │
│  (实际数据库连接)                                   │
│                                                    │
│  HikariDataSource[ds0] → MySQL host0:3306/db0    │
│  HikariDataSource[ds1] → MySQL host1:3306/db1     │
│  (每个分片一个连接池；读写分离或 HA 场景         │
│   下同一数据库可有多个连接池实例)                │
└──────────────────────────────────────────────────┘
```

### 分片 SQL 的处理流程

一条 SQL 从发起到执行完毕，经历了 5 个阶段：

```
原始 SQL:
INSERT INTO t_order (order_id, user_id, status) VALUES (1001, 2001, 'INIT')

               │
               ▼
阶段 1: SQL 解析 (Parser)
  ┌──────────────────────────────────┐
  │ 词法分析 → 语法分析 → SQL AST     │
  │ INSERT 类型                      │
  │ 涉及表: t_order                  │
  │ 提取插入值: order_id = 1001      │
  │ (INSERT 无 WHERE，分片键从 VALUES 提取)│
  └──────────────┬───────────────────┘
                 │
                 ▼
阶段 2: 路由 (Router)
  ┌──────────────────────────────────┐
  │ 分片键 = order_id = 1001         │
  │ 表分片算法 = order_id % 2        │
  │ 1001 % 2 = 1 → 路由到 t_order_1  │
  │ (简化示例：库表用同一键分片；      │
  │  实际配置中库表可用不同键，见下文) │
  └──────────────┬───────────────────┘
                 │
                 ▼
阶段 3: SQL 改写 (Rewriter)
  ┌──────────────────────────────────┐
  │ 表名改写: t_order → t_order_1    │
  │ INSERT INTO t_order_1 (...)      │
  │ VALUES (1001, 2001, 'INIT')      │
  └──────────────┬───────────────────┘
                 │
                 ▼
阶段 4: SQL 执行 (Executor)
  ┌──────────────────────────────────┐
  │ 从连接池获取 ds1 的连接           │
  │ 发送改写后的 SQL 到 ds1         │
  │ ds1.t_order_1 执行 INSERT       │
  └──────────────┬───────────────────┘
                 │
                 ▼
阶段 5: 结果归并 (Merger)
  ┌──────────────────────────────────┐
  │ INSERT 返回影响行数              │
  │ 归并所有分片的结果               │
  │ 返回给应用                      │
  └──────────────────────────────────┘
```

注意：分片路由依赖分片键的值。上例中 `order_id = 1001` 精确匹配了分片键，所以路由到一个分片。如果查询不包含分片键（如 `SELECT * FROM t_order WHERE status = 'INIT'`），ShardingSphere 需要：
1. **广播**到所有可能包含数据的分片（因为没有分片键，不知道数据在哪个分片）
2. 对每个分片执行改写后的 SQL
3. 从每个分片获取结果后做**归并**（排序 `ORDER BY`、分页 `LIMIT`、聚合函数）

广播查询的归并是 ShardingSphere 中最重的操作之一——需要从所有分片拉取全量数据，然后在内存中排序、截断分页。

---

## 5.x vs 4.x 的核心变化

18-dynamic 使用的是 ShardingSphere 5.1.1，与 4.x 有较大差异。理解这些变化对于理解集成代码非常重要。

### 变化 1：YAML 配置格式

**4.x 格式**：
```yaml
# 4.x 配置
shardingRule:
  tables:
    t_order:
      actualDataNodes: ds$->{0..1}.t_order_$->{0..1}
      tableStrategy:
        inline:
          shardingColumn: order_id
          algorithmExpression: t_order_$->{order_id % 2}
      keyGenerator:
        type: SNOWFLAKE
```

**5.x 格式**：
```yaml
# 5.x 配置
rules:
- !SHARDING
  tables:
    t_order:
      actualDataNodes: ds_$->{0..1}.t_order_$->{0..1}
      tableStrategy:
        standard:
          shardingColumn: order_id
          shardingAlgorithmName: t_order_inline
  shardingAlgorithms:
    t_order_inline:
      type: INLINE
      props:
        algorithm-expression: t_order_$->{order_id % 2}
  keyGenerators:
    snowflake:
      type: SNOWFLAKE
```

核心变化：
- **`rules` 列表结构**：5.x 用 `- !TYPE` 标签格式标识规则类型
- **算法注册**：5.x 将分片算法抽离为独立配置，可被多个表引用
- **`shardingAlgorithmName`**：按名称引用算法，而不是内联表达式

### 变化 2：Mode 体系重构

4.x 也支持 YAML 配置，但以编程式 API（`ShardingDataSourceFactory` + `ShardingRuleConfiguration`）为主，没有多实例协调概念。5.x 引入了完整的 `ModeConfiguration` 体系（Memory / Standalone / Cluster），统一了配置的存储、加载和运行时更新机制（见下一节）。

### 变化 3：规则引擎重构

4.x 用 `ShardingRule` 和 `ShardingRuleBuilder`，5.x 用 `RuleConfiguration` + `YamlRuleConfigurationSwapper` 体系。

### 变化 4：包名变化

| 组件 | 4.x | 5.x |
|------|-----|-----|
| JDBC 核心 | `sharding-jdbc-core` | `shardingsphere-jdbc-core` |
| Spring Boot Starter | `sharding-jdbc-spring-boot-starter` | `shardingsphere-jdbc-core-spring-boot-starter` |
| 数据源类 | `ShardingDataSource` | `ShardingSphereDataSource` |

### 变化 5：事务支持

5.x 引入了 `Local`、`XA`、`BASE` 三种分布式事务模式，通过事务配置项控制。4.x 仅支持 `Local` 和弱 XA。不过 18-dynamic 未直接使用 ShardingSphere 的事务模式控制——事务由子上下文中的 `PlatformTransactionManager` 管理，ShardingSphere 仅作为 DataSource 层参与事务。

---

## Mode 体系：Memory / Standalone / Cluster

Mode 是 ShardingSphere 5.x 新增的重要概念，它控制配置和元数据的存储方式。

### 配置方式

```yaml
mode:
  type: Cluster            # Memory | Standalone | Cluster
  repository:
    type: ZooKeeper        # 仅 Cluster 模式需要
    props:
      namespace: shardingsphere
      server-lists: localhost:2181
  overwrite: false         # 本地配置是否覆盖注册中心
```

### 三种模式的对比

| 维度 | Memory | Standalone | Cluster |
|------|--------|-----------|---------|
| **配置存储** | 应用内存 | 本地文件 | 注册中心（ZK/Etcd） |
| **元数据存储** | 应用内存 | 本地文件 | 注册中心 |
| **配置动态更新** | 重启后配置丢失 | 重启后从文件恢复 | 运行时推送 |
| **多实例共享** | ❌ | ❌ | ✅ |
| **适用场景** | 测试/开发 | 单机生产 | 多实例集群 |
| **启动依赖** | 无 | 无 | 需要 ZK/Etcd |
| **在 18-dynamic 中的推荐度** | ✅ 推荐（子上下文重建时重新加载） | ⚠️ 可行（子上下文需共享文件系统） | ⚠️ 复杂（ZK 生命周期管理） |

### Memory 模式

```yaml
mode:
  type: Memory
```

配置和规则在应用启动时一次性加载到内存中，运行时不保存。应用重启后配置丢失。**在标准 ShardingSphere 部署中**适用于测试和开发环境；**在 18-dynamic 的子上下文中反而是推荐选择**——因为子上下文的配置来自父上下文（Synthesizer 注入），每次重建都会重新加载，不需要持久化。

**18-dynamic 的 default.properties 中默认使用 Memory 模式**：
```properties
spring.shardingsphere.mode.type = Memory
```

因为每个子上下文是独立创建的，它的配置在创建时已经确定，不需要持久化。

### Standalone 模式

```yaml
mode:
  type: Standalone
  repository:
    type: File
    props:
      path: config/shardingsphere
```

配置和元数据保存在本地文件系统中。重启后从本地文件恢复。适用于单实例生产环境。

### Cluster 模式

```yaml
mode:
  type: Cluster
  repository:
    type: ZooKeeper
    props:
      namespace: shardingsphere-demo
      server-lists: localhost:2181
      retryIntervalMilliseconds: 500
      timeToLiveSeconds: 60
      maxRetries: 3
      operationTimeoutMilliseconds: 500
```

配置和元数据保存在注册中心（ZooKeeper / Etcd），多实例共享。支持运行时通过注册中心动态修改配置并推送到所有实例。

**在 18-dynamic 中的注意事项**：Cluster 模式在子上下文中使用时，每次 DynamicDataSource 切换重建子上下文时都会创建新的 ZK 连接。如果切换频繁，ZK 连接数会快速增加（旧连接的关闭依赖 60s 延迟）。建议仅在根配置层面的 ShardingSphere 中使用 Cluster 模式——子上下文层面推荐 Memory 模式（配置由父上下文通过 Synthesizer 注入），由 ShardingSphereAutoConfiguration 在子上下文重建时基于新配置重新初始化。

### Mode 在 18-dynamic 中的处理

`ShardingSphereConfigurationConfigBeanDefinitionRegistrar` 通过 `ModeConfigurationYamlSwapper` 将 YAML 中的 mode 配置转为 `ModeConfiguration` 对象，注册为 Bean。

```java
// 从 YAML 中解析 mode
YamlRootConfiguration yamlRootConfig = loadYaml(configResource);
YamlModeConfiguration yamlMode = yamlRootConfig.getMode();

if (yamlMode != null) {
    ModeConfiguration modeConfig = modeSwapper.swapToObject(yamlMode);
    // 注册为 Bean
    registerFactoryBean(registry, beanName, modeConfig);
}
```

Bean 注册后，`ShardingSphereAutoConfiguration` 会注入这个 `ModeConfiguration` Bean 并初始化。

---

## YAML 配置模型

ShardingSphere 5.x 的核心配置是一个 YAML 文件，映射到 `YamlRootConfiguration` 类。

### YamlRootConfiguration 的结构

ShardingSphere 的全量 YAML 配置包含四个部分，但 18-dynamic 对它们做了拆分：

```yaml
# YamlRootConfiguration（全量结构，仅供参考）

dataSources:                   # ← 在 18-dynamic 中不放到 YAML！
  ds_0:                        # 数据源信息改由 DynamicJdbcConfig 的 JSON 提供
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    props:
      jdbcUrl: jdbc:mysql://host0/db0
      username: root
      password: root

rules:                         # ← 在 18-dynamic 中由 YAML 提供
  - !SHARDING
    tables:
      t_order:
        actualDataNodes: ds_$->{0..1}.t_order_$->{0..1}

mode:                          # ← 在 18-dynamic 中由 YAML 提供
  type: Memory

props:                         # ← 在 18-dynamic 中由 YAML 提供
  sql-show: true
```

### 18-dynamic 的职责拆分

理解 18-dynamic 中的 ShardingSphere 配置，关键是要知道配置分成了两部分处理：

| 配置内容 | 来源 | 处理方式 |
|---------|------|---------|
| **数据源连接信息**（`dataSources`） | DynamicJdbcConfig JSON 的 `dataSource` 字段 | `ShardingSphereConfigConfigurationPropertiesSynthesizer` 合成为 `spring.shardingsphere.datasource.*` 扁平属性 |
| **分片规则 + Mode** | YAML 文件（`config-resource` 指定路径） | `DynamicJdbcUtils.loadShardingSphereYamlRootConfiguration()` 解析 → `ShardingSphereConfigurationConfigBeanDefinitionRegistrar` 注册为 Bean |
| **Props** | YAML 文件中的 `props` 段 | Synthesizer 提取后合成到 `spring.shardingsphere.props.*`（走 Environment，不走 Bean 注册） |

这个拆分的原因是：ShardingSphere 标准集成中数据源信息（连接地址、用户名、密码）通常在 properties 中配置，而规则（分片算法、读写分离策略）在 YAML 中配置。18-dynamic 进一步将数据源信息从属性文件移到 DynamicJdbcConfig JSON 中，与 Zone 配置、模块配置统一管理。

**所以集成路径是**：

```properties
# 1. 数据源属性：通过合成的 spring.shardingsphere.datasource.* 传递
spring.shardingsphere.datasource.names=ds_0,ds_1
spring.shardingsphere.datasource.ds_0.jdbcUrl=...

# 2. 规则和 mode：YAML 文件 → YamlRootConfiguration → 注册为 Bean
#    ShardingSphereAutoConfiguration 从 BeanFactory 获取 ModeConfiguration
#    和 RuleConfiguration 类型的 Bean
```

一个完整配置示例：

```json
{
  "dataSource": [
    { "name": "ds_0", "url": "jdbc:mysql://host0/db0", "username": "root", "password": "root" },
    { "name": "ds_1", "url": "jdbc:mysql://host1/db1", "username": "root", "password": "root" }
  ],
  "sharding-sphere": {
    "config-resource": "classpath:/shardingsphere/sharding-databases.yaml"
  }
}
```

对应的 YAML 文件（只包含规则和 mode，不包含数据源信息）：

```yaml
# sharding-databases.yaml
mode:
  type: Memory
rules:
  - !SHARDING
    tables:
      t_order:
        actualDataNodes: ds_$->{0..1}.t_order_$->{0..1}
        databaseStrategy:
          standard:
            shardingColumn: user_id
            shardingAlgorithmName: database_inline
        tableStrategy:
          standard:
            shardingColumn: order_id
            shardingAlgorithmName: table_inline
    shardingAlgorithms:
      database_inline:
        type: INLINE
        props:
          algorithm-expression: ds_$->{user_id % 2}
      table_inline:
        type: INLINE
        props:
          algorithm-expression: t_order_$->{order_id % 2}
```

---

## 规则系统：RuleConfiguration 体系

ShardingSphere 5.x 的规则体系通过 `YamlRuleConfigurationSwapperEngine` 将 YAML 解析为 `RuleConfiguration` 对象。

### 规则类型

ShardingSphere 5.x 支持以下规则类型，18-dynamic 通过 `YamlRuleConfigurationSwapperEngine` 自动处理所有类型（无需针对每种规则编写转换代码）：

| 规则名称 | YAML 标签 | 功能 | 在 18-dynamic 中的使用 |
|---------|-----------|------|----------------------|
| 分片 | `!SHARDING` | 分库分表 | ✅ 常用，测试示例覆盖 |
| 读写分离 | `!READWRITE_SPLITTING` | 读写分离 | ✅ 可用 |
| 数据加密 | `!ENCRYPT` | 字段加密/解密 | ⚠️ 未测试 |
| 数据脱敏 | `!MASK` | 数据脱敏 | ⚠️ 未测试 |
| 影子库 | `!SHADOW` | 压测影子表 | ⚠️ 未测试 |
| 数据库发现 | `!DB_DISCOVERY` | 数据库高可用发现 | ⚠️ 未测试 |
| 广播表 | `!BROADCAST` | 广播表 | ⚠️ 未测试 |

由于 `YamlRuleConfigurationSwapperEngine` 是 ShardingSphere 内置的通用转换引擎，常见规则类型（SHARDING、READWRITE_SPLITTING）有对应 swapper，理论上有注册 swapper 的规则都能支持。但要注意：**未测试的规则类型存在风险**——如果 ShardingSphere 5.1.1 的 swapper 引擎没有注册该规则类型的 swapper，`swapToRuleConfiguration()` 会抛 `UnsupportedOperationException`。生产使用前应对目标规则类型做验证。

### 规则注册流程

```
YAML 文件 → YamlEngine.unmarshal() → YamlRootConfiguration
                                              │
                                              ▼
                          yamlRootConfiguration.getRules()
                                              │
                                              ▼
                      YamlRuleConfigurationSwapperEngine
                        .swapToRuleConfiguration(rule)
                          → RuleConfiguration 对象
                          → 注册为 Bean（FactoryBean）
```

### 属性映射：YAML → Environment → SpringBootPropertiesConfiguration

Synthesizer 合成的属性与 ShardingSphere 的绑定机制：

| YAML 键 | 合成后的 Environment 键 | 绑定目标 |
|---------|----------------------|---------|
| `dataSources.ds_0.jdbcUrl` | `spring.shardingsphere.datasource.ds_0.jdbcUrl` | `SpringBootPropertiesConfiguration` |
| `dataSources.ds_0.username` | `spring.shardingsphere.datasource.ds_0.username` | `SpringBootPropertiesConfiguration` |
| `props.sql-show` | `spring.shardingsphere.props.sql-show` | `SpringBootPropertiesConfiguration` |
| `rules` | 不合成（由 Registrar 注册为 Bean） | `List<RuleConfiguration>` |
| `mode` | 不合成（由 Registrar 注册为 Bean） | `ModeConfiguration` |

注意：用户 JSON 中写的是 `url`（或 `jdbcUrl`/`jdbc-url` 任意别名），Synthesizer 通过 `DynamicJdbcConfigUtils.getDataSourceUrlEntry()` 统一解析后，合成到 Environment 时统一使用 `jdbcUrl` 键。

### 在 ShardingSphereAutoConfiguration 中的使用

```java
// ShardingSphereAutoConfiguration（简化表示）
// 来自 shardingsphere-jdbc-core-spring-boot-starter 依赖
// 不是 18-dynamic 的代码，是 ShardingSphere 5.x 内置的自动配置类
// 18-dynamic 通过 Synthesizer + Registrar 为它准备了所需的输入
@Configuration
public class ShardingSphereAutoConfiguration {

    @Bean
    public ShardingSphereDataSource shardingSphereDataSource(
            ModeConfiguration modeConfig,
            List<RuleConfiguration> ruleConfigs,
            SpringBootPropertiesConfiguration props) {

        // 从 Environment 绑定数据源配置
        Map<String, DataSource> dataSources = buildDataSources(props);

        // 组装 ShardingSphereDataSource
        return ShardingSphereDataSourceFactory.create(dataSources, ruleConfigs, modeConfig);
    }
}
```

注意这个 `@Bean` 方法需要三个输入：
1. `ModeConfiguration` → 由 Registrar 注册
2. `List<RuleConfiguration>` → 由 Registrar 注册
3. `SpringBootPropertiesConfiguration` → 由 Synthesizer 合成到 Environment 后自动绑定

---

## ShardingSphere 与 Spring Boot 的集成

### 标准集成路径（非 18-dynamic）

如果没有 18-dynamic，标准的 ShardingSphere 5.x + Spring Boot 集成方式：

```properties
# application.properties（标准 5.x 集成）
spring.shardingsphere.datasource.names=ds_0,ds_1
spring.shardingsphere.datasource.ds_0.jdbcUrl=jdbc:mysql://host0/db0
spring.shardingsphere.datasource.ds_0.username=root
spring.shardingsphere.datasource.ds_0.password=root
spring.shardingsphere.datasource.ds_1.jdbcUrl=jdbc:mysql://host1/db1
# ...
```

然后将规则和 mode 放在 YAML 文件中，通过 Java 配置手动创建 `ShardingSphereDataSource`：

```java
@Bean
public DataSource shardingSphereDataSource(
        List<RuleConfiguration> rules, ModeConfiguration mode) {
    // dataSources 从 Spring Environment 绑定或手动创建
    Map<String, DataSource> dataSources = buildDataSources();
    return ShardingSphereDataSourceFactory.create(dataSources, rules, mode);
}
```

18-dynamic 自动化了这个过程——数据源属性通过 Synthesizer 合成到 Environment 中，规则和 mode 通过 Registrar 自动注册为 Bean，Spring Boot 的 `ShardingSphereAutoConfiguration` 自动装配它们。

### 18-dynamic 的集成路径

```
1. JSON 配置 → 解析为 DynamicJdbcConfig
   ├── dataSource → ShardingSphereConfigConfigurationPropertiesSynthesizer
   │                → spring.shardingsphere.datasource.* 属性
   └── sharding-sphere.config-resource → DynamicJdbcUtils.loadYaml()
                      → YamlRootConfiguration
                      → ShardingSphereConfigurationConfigBeanDefinitionRegistrar
                        → ModeConfiguration Bean
                        → RuleConfiguration Bean × N

2. 子上下文 refresh → ShardingSphereAutoConfiguration 自动执行
   ├── 从 Environment 获取 spring.shardingsphere.* 属性
   ├── 从 BeanFactory 获取 ModeConfiguration + RuleConfiguration
   ├── 创建 ShardingSphereDataSource
   └── 注册为 DataSource Bean
```

### Shutdown Hook 的管理

ShardingSphere 5.x 在初始化时会向 JVM 注册 shutdown hook 线程，用于关闭连接池、释放资源：

```java
// ShardingSphereDataSourceFactory
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    // 关闭所有分片连接池
}));
```

在 18-dynamic 的子上下文环境中，这个 shutdown hook 有两个问题：
1. 子上下文关闭时，JVM 的 shutdown hook 不会执行（hook 在 JVM 层面，Spring 上下文关闭不触发 JVM 关闭）
2. 即使 JVM 最终关闭，hook 由 JVM 的 shutdown 线程异步执行——无法保证与 Spring 上下文的关闭顺序一致，可能导致连接池在 Spring Bean 销毁之后才被关闭

`SyncExecutionShutdownHookApplicationListener` 解决了这个问题：

```java
// ApplicationStartedEvent 时
// 找到 ShardingSphere 注册的 shutdown hook 线程（名称以 "DelayedShutdownHook-for-" 开头）
Set<Thread> hooks = filterShutdownHookThreads(filter, true);

// ContextClosedEvent 时
// 用 Thread.run() 在 Spring 线程中同步执行
hooks.forEach(Thread::run);
```

这样保证了在子上下文关闭时，ShardingSphere 的资源释放逻辑会被同步执行。

---

## 在子上下文中的集成链路

把以上所有内容串联起来，ShardingSphere 在 18-dynamic 子上下文中的完整集成链路：

```
子上下文 refresh() 开始
  │
  ├─ DynamicJdbcContextProcessor.process()
  │   │
  │   ├─ 第 1 步: 注册 AnnotationConfig Processors
  │   │   └─ ConfigurationClassPostProcessor（处理 @Configuration、@Import）
  │   │
  │   ├─ 第 2 步: ConfigPostProcessor（ShardingSphereConfigPostProcessor → 空）
  │   │
  │   ├─ 第 3 步: ConfigValidator
  │   │   └─ ShardingSphereConfigValidator → 验证 config-resource 是否存在
  │   │
  │   ├─ 第 4 步: 如果 dynamic=true → 注册 DynamicDataSource 并移除 ShardingSphere 配置
  │   │   → 移除后第 5 步 Synthesizer 不再合成 ShardingSphere 属性
  │   │   → ShardingSphere 配置由 DynamicDataSource 内部的深克隆 config 携带
  │   │   → 在 DynamicDataSource 的子上下文中重新处理（嵌套 refresh）
  │   │
  │   ├─ 第 5 步: ConfigConfigurationPropertiesSynthesizer
  │   │   ├─ DataSourceConfigurationPropertiesSynthesizer
  │   │   │   → 跳过（有 ShardingSphere 时不合成 spring.datasource.*）
  │   │   └─ ShardingSphereConfigConfigurationPropertiesSynthesizer
  │   │       ├─ 从 DynamicJdbcConfig 的 dataSource 字段
  │   │       │   合成 spring.shardingsphere.datasource.names=ds_0,ds_1
  │   │       │   合成 spring.shardingsphere.datasource.ds_0.jdbcUrl=...
  │   │       │
  │   │       ├─ 加载 YAML：DynamicJdbcUtils.loadShardingSphereYamlRootConfiguration()
  │   │       │   → YamlEngine.unmarshal(yamlContent, YamlRootConfiguration.class)
  │   │       │
  │   │       └─ 从 YAML 中提取 props 部分
  │   │           合成 spring.shardingsphere.props.sql-show=false
  │   │           合成 spring.shardingsphere.props.sql-comment-parse-enabled=false
  │   │
  │   └─ 第 6 步: ConfigBeanDefinitionRegistrar
  │       └─ ShardingSphereConfigurationConfigBeanDefinitionRegistrar
  │           ├─ 再次加载 YAML（原因见下文）
  │           ├─ YamlModeConfiguration → ModeConfiguration 对象
  │           │   → ModeConfigurationYamlSwapper.swapToObject()
  │           │   → 注册为 Bean: ShardingSphereConfiguration[MODE]
  │           │
  │           └─ Collection<YamlRuleConfiguration> → RuleConfiguration 列表
  │               → YamlRuleConfigurationSwapperEngine.swapToRuleConfiguration()
  │               → 每个 RuleConfiguration 注册为独立 Bean:
  │                  ShardingSphereConfiguration[ShardingRuleConfiguration]
  │                  ShardingSphereConfiguration[ReadwriteSplittingRuleConfiguration]
  │
  │               # 注：ShardingSphere 5.x 内部类名为 ReadwriteSplitting（小写 w）
  │
  ├─ refresh 继续 → Auto-Configuration 执行
  │   │
  │   └─ ShardingSphereAutoConfiguration（来自 shardingsphere-jdbc-core-spring-boot-starter）
  │       ├─ @ConfigurationProperties("spring.shardingsphere")
  │       │   SpringBootPropertiesConfiguration ← 绑定 Environment 中的属性
  │       │
  │       ├─ @Autowired
  │       │   ModeConfiguration modeConfig ← 注入 Registrar 注册的 Bean
  │       │
  │       ├─ @Autowired
  │       │   List<RuleConfiguration> ruleConfigs ← 注入 Registrar 注册的 Bean
  │       │
  │       └─ @Bean
  │           ShardingSphereDataSource ← 创建分片数据源
  │
  └─ ContextRefreshedEvent（子上下文刷新完成）
      └─ DynamicJdbcChildContextRefreshedListener
          ├─ 获取 DataSource Bean（ShardingSphereDataSource）
          └─ 注册到父上下文
```

### YAML 被解析两次的问题

当前实现中，YAML 文件被解析了两次：

1. **第 5 步 Synthesizer**：`DynamicJdbcUtils.loadShardingSphereYamlRootConfiguration()` 加载 YAML，提取 `props` 部分合入 Environment
2. **第 6 步 Registrar**：再次调用 `DynamicJdbcUtils.loadShardingSphereYamlRootConfiguration()` 加载 YAML，提取 `mode` 和 `rules` 部分注册为 Bean

两次解析的原因是职责分离——Synthesizer 负责配置属性（Environment），Registrar 负责 Bean 定义（BeanFactory），两者在管道的不同阶段执行，之间没有共享状态。

**潜在风险**：如果 YAML 文件在两次解析之间被修改（比如配置热更新），Synthesizer 和 Registrar 会看到不同的配置，导致属性值和规则不一致。这在实际生产中虽然概率低（两次解析间隔毫秒级），但理论上存在。可以通过在第一次解析后将 `YamlRootConfiguration` 缓存到 `DynamicJdbcChildContext` 的 AttributeMap 中来解决。

---

## 回到简历中的"Apache ShardingSphere 5.x 支持"

| 简历声称的能力 | 实现方式 | 验证结论 |
|---------------|---------|---------|
| "支持 Apache ShardingSphere 5.x" | YAML 配置加载 → Synthesizer 合成数据源属性 + Registrar 注册 Mode/Rule Bean → 子上下文内运行 ShardingSphereAutoConfiguration | ✅ 真实支持 5.1.1 |
| ShardingSphere 与 MyBatis/MP 共存 | 同子上下文（ShardingSphere 作为 DataSource，MyBatis/MP 在其上运行）或不同子上下文 | ✅ 两种模式均可行 |
| ShardingSphere 与 DynamicDataSource 兼容 | DynamicDataSource 在子上下文中重建时，ShardingSphere 配置随子上下文重建而刷新 | ✅ 兼容 |
| Shutdown hook 管理 | `SyncExecutionShutdownHookApplicationListener` 同步执行 ShardingSphere 的 hook | ✅ 已处理 |

验证细节（来源：源码分析 18-08）：

| 能力点 | 代码证据 |
|--------|---------|
| 版本 | `pom.xml` → `<shardingsphere.version>5.1.1</shardingsphere.version>` |
| 依赖 | `shardingsphere-jdbc-core-spring-boot-starter` |
| YAML 解析 | `DynamicJdbcUtils.loadShardingSphereYamlRootConfiguration()` → `YamlEngine.unmarshal()` |
| Mode 处理 | `ModeConfigurationYamlSwapper.swapToObject()` |
| 规则处理 | `YamlRuleConfigurationSwapperEngine.swapToRuleConfiguration()` |
| 属性合成 | `ShardingSphereConfigConfigurationPropertiesSynthesizer` 合成 `spring.shardingsphere.datasource.*` + `props.*` |
| Shutdown hook | `SyncExecutionShutdownHookApplicationListener` + `ShardingSphereShutdownHookThreadFilter` |
