# 18-11 项目定位、使用与工程分析

## 目录

- [这是个什么项目](#这是个什么项目)
- [核心概念](#核心概念)
- [完整配置参考手册](#完整配置参考手册)
- [使用场景与示例](#使用场景与示例)
- [6 大模块详解](#6-大模块详解)
- [三种运行模式](#三种运行模式)
- [事件驱动架构](#事件驱动架构)
- [配置热更新机制](#配置热更新机制)
- [工程问题分析](#工程问题分析)
- [与 baomidou dynamic-datasource 对比](#与-baomidou-dynamic-datasource-对比)
- [18-dynamic + baomidou 融合方案](#18-dynamic--baomidou-融合方案)

---

## 这是个什么项目

`microsphere-dynamic-jdbc-spring-boot` 是一个 Spring Boot Starter。

**一句话定位**：让你用 JSON 配置一个"数据库单元"，Starter 自动为每个单元拉起一整套独立的数据库基础设施（连接池 + ORM 框架 + 事务管理器 + 可选 ShardingSphere），且支持运行时不重启热切换。

### 它解决的核心问题

在 Spring Boot 中配置一套 DataSource + MyBatis + TransactionManager 很简单。但在以下场景中，标准的单套配置不够用：

- **多 ORM 共存**：同一个进程里 MyBatis 和 MyBatis-Plus 各自的 Mapper 需要不同的 SqlSessionFactory，但 Spring Boot 的 Auto-Configuration 假设每种 Bean 只有一个
- **多套完整的数据库栈**：不同业务模块有自己的数据库、自己的 MyBatis 配置、自己的事务管理器，但需要在同一个进程中运行
- **ShardingSphere + 普通表**：分片表和普通表共存在一个进程里，但 ShardingSphere 的 DataSource 配置和普通 DataSource 的配置完全不同
- **Zone 感知的多活**：Zone 切换时不只是换连接池，MyBatis 配置和事务管理器也需要一起换
- **配置热更新**：Nacos/Apollo 里的数据库配置变了，不需要重启应用，自动重建连接

18-dynamic 对以上场景的答案是：**每个配置单元运行在独立的子 Spring 上下文中**。

### 它与 baomidou dynamic-datasource 的区别

| 维度 | baomidou dynamic-datasource | 18-dynamic |
|------|---------------------------|------------|
| 解决什么问题 | 一个 SqlSessionFactory 后面挂多个 DataSource | 多套完整的数据库栈（连接池+ORM+事务）隔离共存 |
| 切换方式 | `@DS` 注解 + ThreadLocal 路由 | 重建整个子 Spring 上下文 |
| 切换成本 | µs 级 | 秒级 |
| ORM 数量 | 一套（共享 SqlSessionFactory） | 每个单元可以不同 ORM |
| ShardingSphere | 不支持 | 原生支持 |
| Zone 感知 | 不支持 | 支持（ZoneContext 集成） |
| 配置热更新 | 不支持（需重启） | 支持（事件驱动） |

**两者不是竞争关系，是不同层面的方案**。baomidou 管"请求级路由"，18-dynamic 管"配置级隔离"。后面有融合方案。

---

## 核心概念

### 数据库单元（DynamicJdbcConfig）

每个 `microsphere.dynamic.jdbc.configs.{name}` 属性就是一个"数据库单元"。一个单元包含：

```json
{
  "name": "orders",
  "datasource": [{ "url": "jdbc:mysql://...", "username": "...", "password": "..." }],
  "mybatis": { "base-packages": "com.example.orders.mapper" },
  "transaction": { "name": "order-tx" }
}
```

这个单元对应运行时的一个独立子 Spring 上下文：

```
DynamicJdbcChildContext[orders]
  ├─ HikariDataSource (数据源)
  ├─ SqlSessionFactory (MyBatis)
  ├─ Mapper (扫描 com.example.orders.mapper)
  └─ PlatformTransactionManager "order-tx"
```

### 子上下文隔离

每个单元运行在独立的 `AnnotationConfigApplicationContext` 中，这意味着：

| 隔离维度 | 效果 |
|---------|------|
| Bean 定义隔离 | 每个子上下文有独立的 `DefaultListableBeanFactory`，MyBatis 的 SqlSessionFactory 不会冲突 |
| Auto-Configuration 隔离 | 每个子上下文独立执行 Spring Boot 的 Auto-Configuration，各自的 `DataSourceAutoConfiguration` 互不干扰 |
| 属性隔离 | 子上下文继承父上下文 Environment，但可以覆盖（Synthesizer 合成的 PropertySource 优先级更高） |
| 生命周期隔离 | 父上下文关闭时自动关闭所有子上下文，子上下文之间互不影响 |

### 模块（Module）

项目定义了 6 个模块，每个模块在 4 个 SPI 中各有一个实现：

| 模块 | 字段名 | 功能 | 4 个 SPI 实现的状态 |
|------|--------|------|-------------------|
| datasource | `datasource` | 数据源配置 | ✅ ConfigPostProcessor 填充默认值 / ✅ Validator 校验 / ✅ Synthesizer 合成属性 / ❌ Registrar 无 |
| ha-datasource | `ha-datasource` | Zone 感知的高可用数据源 | 同上（共享 datasource 的 SPI 实现） |
| transaction | `transaction` | 事务管理器 | ❌ PostProcessor 无操作 / ✅ Validator 校验 / ✅ Synthesizer 合成 / ✅ Registrar 注册别名 |
| sharding-sphere | `sharding-sphere` | 分库分表 | ❌ PostProcessor 无操作 / ✅ Validator 校验 / ✅ Synthesizer 合成 / ✅ Registrar 注册 Mode+Rule |
| mybatis | `mybatis` | MyBatis 集成 | ❌ PostProcessor 无操作 / ✅ Validator 互斥 / ✅ Synthesizer 合成 / ✅ Registrar 扫描 Mapper |
| mybatis-plus | `mybatis-plus` | MyBatis-Plus 集成 | ❌ PostProcessor 无操作 / ✅ Validator 互斥 / ✅ Synthesizer 合成 / ✅ Registrar 扫描 Mapper |

### 4 个 SPI

```
ConfigPostProcessor          → 配置预处理（填充默认值）
ConfigValidator              → 配置校验（启动时拒绝错误配置）
ConfigConfigurationPropertiesSynthesizer → 配置转属性（JSON → Spring Environment 属性）
ConfigBeanDefinitionRegistrar → Bean 注册（根据配置注册 SqlSessionFactory、TransactionManager 等）
```

每个模块可以在这 4 个 SPI 中各有一个实现。需要新增模块时，只需：
1. 在 `DynamicJdbcConfig` 中添加一个 Inner Config 类
2. 为 Inner Config 类标注 `@Module("模块名")`
3. 在 `DynamicJdbcConfig` 中添加 getter
4. 实现 4 个 SPI
5. 在 `spring.factories` 中注册

---

## 完整配置参考手册

### 入口开关

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `microsphere.dynamic.jdbc.enabled` | `true` | 总开关 |

### DynamicJdbcConfig JSON 字段

每个 `microsphere.dynamic.jdbc.configs.{name}` 的值是一个 JSON 对象：

| 字段 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `name` | String | 自动从属性名后缀生成 | 标识名称，用于子上下文 ID |
| `dynamic` | boolean | `true`（强制） | 是否启用动态模式。设置为 `false` 也会被强制改为 `true` |
| `primary` | boolean | `false` | 多 config 模式下，该单元暴露的 Bean 是否标记 `@Primary` |
| `datasource` | `List<Map>` | — | 普通数据源列表，每个元素是一个属性 Map |
| `ha-datasource` | `Map<String, List<Map>>` | — | 高可用数据源，key 是 zone 名，value 是该 zone 的数据源列表 |
| `transaction` | Object | — | 事务配置 |
| `sharding-sphere` | Object | — | ShardingSphere 配置 |
| `mybatis` | Object | — | MyBatis 配置 |
| `mybatis-plus` | Object | — | MyBatis-Plus 配置 |

### datasource 元素支持的属性

`datasource` 和 `ha-datasource.{zone}` 中的每个元素支持：

| 属性 | 说明 | 是否必需 |
|------|------|---------|
| `name` | 数据源名称 | 自动生成 |
| `type` | 连接池全限定类名 | 默认 `com.zaxxer.hikari.HikariDataSource` |
| `url` / `jdbcUrl` / `jdbc-url` | JDBC URL（三者可互换） | ✅ |
| `driverClassName` | 驱动类名 | 从 URL 自动推导 |
| `username` | 用户名 | 从上一个数据源继承 |
| `password` | 密码 | 从上一个数据源继承 |
| `maximumPoolSize` / `maxLifetime` / `idleTimeout` 等 | 连接池参数 | 可选 |

### transaction 字段

```json
{
  "name": "order-tx",
  "customizers": "com.example.MyCustomizer",
  "configurations": "com.example.CustomConfig",
  "properties": { "defaultTimeout": 30 }
}
```

| 字段 | 说明 |
|------|------|
| `name` | TransactionManager 的 Bean 别名（也是升迁到父上下文后的 Bean 名） |
| `customizers` | `PlatformTransactionManagerCustomizer` 实现类名（逗号分隔） |
| `configurations` | 额外的配置类名（逗号分隔） |
| `properties` | 任意键值对，通过 `@ConfigurationProperties` 绑定 |

### sharding-sphere 字段

```json
{
  "config-resource": "classpath:/shardingsphere/sharding-databases.yaml",
  "configurations": "com.example.Config",
  "properties": { "props.sql.show": true }
}
```

| 字段 | 说明 |
|------|------|
| `config-resource` | ShardingSphere YAML 配置文件的 classpath 路径（必需） |
| `configurations` | 额外的配置类名 |
| `properties` | 任意键值对 |

### mybatis / mybatis-plus 字段

```json
{
  "base-packages": "com.example.orders.mapper",
  "configurations": "com.example.MyBatisConfig",
  "properties": { "checkConfigLocation": true }
}
```

| 字段 | 说明 |
|------|------|
| `base-packages` | Mapper 接口扫描包路径 |
| `configurations` | 额外的 MyBatis 配置类名 |
| `properties` | 任意键值对，通过 `@ConfigurationProperties("mybatis")` 绑定 |

### 模块级配置

模块级配置以 `microsphere.dynamic.jdbc.modules.{module}.` 为前缀：

```properties
# 数据源模块的 Auto-Configuration base packages
microsphere.dynamic.jdbc.modules.datasource.auto-configuration.base-packages=\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  org.springframework.boot.autoconfigure.jdbc.DataSourceConfiguration

# 禁止 ShardingSphere 的 Auto-Configuration 在 DataSource 模块激活时运行
microsphere.dynamic.jdbc.modules.datasource.auto-configuration.banned-modules=sharding-sphere

# URL 别名
microsphere.dynamic.jdbc.modules.datasource.property-name-aliases.url=jdbcUrl,jdbc-url

# URL 默认配置
microsphere.dynamic.jdbc.modules.datasource.url.default-scheme=mysql\://
microsphere.dynamic.jdbc.modules.datasource.url.default-query-params.characterEncoding=utf-8

# 默认用户名和密码（从 Environment 读取，不硬编码）
microsphere.dynamic.jdbc.modules.datasource.default-user-name=root
microsphere.dynamic.jdbc.modules.datasource.default-password=root

# 动态上下文关闭延迟
microsphere.dynamic.jdbc.modules.datasource.dynamic-context.close-delay=60s

# ShardingSphere 的动态开关
microsphere.dynamic.jdbc.modules.sharding-sphere.dynamic.enabled=true

# ShardingSphere 默认连接池参数
microsphere.dynamic.jdbc.modules.sharding-sphere.default-properties.datasource.maxLifetime=1800000
microsphere.dynamic.jdbc.modules.sharding-sphere.default-properties.datasource.maxPoolSize=10
```

### 多上下文配置

```properties
# 多 config 模式下，父上下文排除的 Auto-Configuration
microsphere.dynamic.jdbc.multiple-context.auto-configuration.exclude=\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration,\
  com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration,\
  org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration

# 从子上下文暴露到父上下文的 Bean 类型
microsphere.dynamic.jdbc.multiple-context.bean-classes.expose=\
  javax.sql.DataSource,\
  org.springframework.transaction.PlatformTransactionManager,\
  org.apache.ibatis.session.SqlSessionFactory

# 支持 @Primary 标记的 Bean 类型
microsphere.dynamic.jdbc.multiple-context.bean-classes.primary=\
  javax.sql.DataSource,\
  org.springframework.transaction.PlatformTransactionManager
```

---

## 使用场景与示例

### 场景 1：单数据源 + MyBatis（最常见）

```properties
microsphere.dynamic.jdbc.configs.orders={"name":"orders","datasource":[{"name":"ds","url":"jdbc:mysql://localhost:3306/orders","username":"root","password":"root"}],"mybatis":{"base-packages":"com.example.orders.mapper"}}
```

效果：一个子上下文 `DynamicJdbcChildContext[orders]`，内含 HikariCP + SqlSessionFactory + Mapper。

### 场景 2：多数据源 + 不同 ORM

```properties
microsphere.dynamic.jdbc.configs.orders={"name":"orders","datasource":[{"name":"ds","url":"jdbc:mysql://localhost:3306/orders","username":"root","password":"root"}],"mybatis":{"base-packages":"com.example.orders.mapper"}}

microsphere.dynamic.jdbc.configs.users={"name":"users","datasource":[{"name":"ds","url":"jdbc:mysql://localhost:3307/users","username":"root","password":"root"}],"mybatis-plus":{"base-packages":"com.example.users.mapper"}}
```

效果：两个子上下文，orders 用 MyBatis，users 用 MyBatis-Plus，互不干扰。

### 场景 3：ShardingSphere + 分片表

```json
{
  "name": "shard-order",
  "datasource": [
    { "name": "ds_0", "url": "jdbc:mysql://host0:3306/orders_0", "username": "root", "password": "root" },
    { "name": "ds_1", "url": "jdbc:mysql://host1:3306/orders_1", "username": "root", "password": "root" }
  ],
  "sharding-sphere": {
    "config-resource": "classpath:/shardingsphere/order-sharding.yaml"
  },
  "mybatis": {
    "base-packages": "com.example.order.mapper"
  }
}
```

YAML 文件（`order-sharding.yaml`）：

```yaml
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

效果：子上下文内，ShardingSphere 创建分片 DataSource，MyBatis 的 SqlSessionFactory 绑定到它。

### 场景 4：Zone 感知的高可用数据源

```json
{
  "name": "ha-orders",
  "ha-datasource": {
    "defaultZone": [{ "name": "ds", "url": "jdbc:mysql://host-primary:3306/orders", "username": "root", "password": "root" }],
    "zone-b": [{ "name": "ds", "url": "jdbc:mysql://host-standby:3306/orders", "username": "root", "password": "root" }]
  },
  "mybatis": { "base-packages": "com.example.orders.mapper" }
}
```

当 `ZoneContext.getZone()` 返回 `"zone-b"` 时，使用 `host-standby` 的数据库。返回 `"defaultZone"` 时使用 `host-primary` 的数据库。Zone 切换时自动重建整个子上下文。

### 场景 5：单个 config + 多数据源

```json
{
  "name": "readwrite",
  "datasource": [
    { "name": "master", "url": "jdbc:mysql://host:3306/db", "username": "root", "password": "root" },
    { "name": "slave", "url": "jdbc:mysql://host:3307/db-ro", "username": "root", "password": "root" }
  ],
  "transaction": { "name": "readwrite-tx" }
}
```

注意：虽然场景标题叫"多数据源"，但当前 `dynamic=false` 会被强制覆写为 `true`，所以这两个数据源会被 `DynamicDataSource` 包装。而 Synthesizer 的 `supports()` 方法只处理 `hasOnlySingleDataSource()`（dataSource 列表长度为 1）的情况，多数据源场景不会合成 `spring.datasource.*` 属性——`DataSourceAutoConfiguration` 不会自动创建 HikariCP，多数据源的路由需要其他机制（如 baomidou 融合方案）支持。

---

## 6 大模块详解

### 1. datasource 模块

核心类：`DataSourcePropertiesConfigPostProcessor`（唯一有实际逻辑的 PostProcessor）

输入 JSON：
```json
{
  "name": "ds1",
  "url": "localhost:3306/db",
  "username": "root"
}
```

经过 PostProcessor 填充后：
```json
{
  "name": "ds1",
  "type": "com.zaxxer.hikari.HikariDataSource",
  "url": "jdbc:mysql://localhost:3306/db?characterEncoding=utf-8&useSSL=false&useUnicode=true",
  "driverClassName": "com.mysql.cj.jdbc.Driver",
  "username": "root",
  "password": "<default-password 或继承值>"   // 未显式配置时从 Environment 默认值或前一个数据源继承
}
```

多数据源时，前一个数据源的配置会填充到后一个（"前一个填充"行为）。核心逻辑见 18-08 的 SPI 对比表。

### 2. ha-datasource 模块

与 datasource 模块共享相同的 SPI 实现，区别在于：
- 配置结构不同（Map<zone, List<Map>> 而不是 List<Map>）
- 输入 `ha-datasource` JSON，底层转换为展平的 `highAvailabilityDataSourcePropertiesMap`
- `getDataSourcePropertiesList()` 根据当前 ZoneContext 选择对应 zone 的列表
- 至少需要 2 个 zone，且必须包含 `defaultZone`
- `datasource` 和 `ha-datasource` 互斥

### 3. transaction 模块

功能：
- 给 `DataSourceTransactionManager` 注册别名（`transaction.name`）
- 注册 `PlatformTransactionManagerCustomizer` 实现类
- 在父上下文中通过 `PlatformTransactionManagerBeanNameGenerator` 生成可读的 Bean 名

Bean 升迁后，父上下文可以通过 `@Transactional("order-tx")` 使用该子上下文的事务管理器。

### 4. sharding-sphere 模块

功能：
- 验证 `config-resource` 指向的 YAML 文件是否存在
- Synthesizer：从 YAML 提取 `props` 部分，合成 `spring.shardingsphere.props.*`
- Synthesizer：从 JSON 的 `dataSource` 字段合成 `spring.shardingsphere.datasource.*`
- Registrar：解析 YAML 中的 `mode` 和 `rules`，注册 `ModeConfiguration` 和 `RuleConfiguration` Bean
- 通过 ShardingSphere 内置的 `ShardingSphereAutoConfiguration` 完成 DataSource 创建
- 同步执行 ShardingSphere 的 shutdown hook（`SyncExecutionShutdownHookApplicationListener`）

### 5. mybatis 模块

功能：
- 注册 `MybatisMapperScanConfiguration`（内含 `@MapperScan`）
- 自动扫描 `base-packages` 下的 `@Mapper` 接口
- 自动注册枚举类型处理器（`EnumTypeHandler`）
- 与 mybatis-plus 互斥（banned-modules + Validator 双重保证）

### 6. mybatis-plus 模块

功能同 mybatis 模块，区别在于：
- `@MapperScan` 扫描的是 MyBatis-Plus 的 Mapper（继承 `BaseMapper` 的接口）
- 与 mybatis 互斥

---

## 三种运行模式

### 模式 1：单 config + 单数据源（启动时创建）

```
1 个 DynamicJdbcConfig → ApplicationPreparedEvent（prepareContext 阶段，refresh 之前）
                        → 在当前上下文执行 6 步管道
                        → DynamicDataSource BeanDefinition 注册
                        → refresh 的 finishBeanFactoryInitialization 实例化 DynamicDataSource
                        → afterPropertiesSet() 触发子上下文创建（启动时完成）
                        → 连接池延迟到首次 getConnection()
```

### 模式 2：单 config + HA 数据源

```
1 个 DynamicJdbcConfig（带 ha-datasource）
  → 与模式 1 相同
  → 但 ZoneContext 决定当前使用哪个 zone 的配置
  → Zone 切换时 PropagatingDynamicJdbcConfigChangedEventListener 发布事件
  → DynamicDataSource 重建
```

### 模式 3：多 config

```
N 个 DynamicJdbcConfig（N > 1）
  → 每个 config 一个 DynamicJdbcChildContext（线程池并行创建）
  → 每个子上下文独立执行 6 步管道
  → Bean 升迁到父上下文（DataSource、TransactionManager、SqlSessionFactory）
  → 父上下文排除模块的 Auto-Configuration
```

### `dynamic` 标志的真相

`dynamic` 属性当前只有一个选择：`true`。即使你在 JSON 中写 `"dynamic": false`，`DynamicJdbcConfigUtils.getDynamicJdbcConfig()` 也会强制覆写为 `true`（并打印警告日志 "Force dynamic true"）。所以"关闭动态"这个选项实际上不存在。

`dynamic=false` 只在一种情况下生效：DynamicDataSource 在 `initDynamicDynamicJdbcConfig()` 中对新克隆的配置设 `dynamic=false`，防止递归创建 DynamicDataSource。

---

## 事件驱动架构

### 启动事件流

```
SpringApplication.run()
  │
  ├─ ApplicationEnvironmentPreparedEvent
  │   ← DynamicJdbcDefaultPropertiesPostProcessor
  │     加载 META-INF/dynamic-jdbc/default.properties
  │
  ├─ prepareContext
  │   └─ ApplicationPreparedEvent
  │       ← DynamicJdbcContextApplicationListener
  │          ├─ 读取配置
  │          ├─ 注册事件监听器
  │          └─ 单 config：执行 6 步管道，注册 DynamicDataSource BeanDefinition
  │             多 config：线程池并行创建业务子上下文
  │
  ├─ refresh()
  │   ├─ invokeBeanFactoryPostProcessors
  │   │   ← AutoConfigurationImportEvent
  │   │   ← DynamicJdbcAutoConfigurationImportListener 缓存过滤结果
  │   │
  │   ├─ finishBeanFactoryInitialization
  │   │   ← DynamicDataSource.afterPropertiesSet() 触发子上下文创建（单 config）
  │   │
  │   └─ finishRefresh
  │       ← ContextRefreshedEvent + ApplicationStartedEvent
  │
  └─ afterRefresh
```

注意：`ApplicationPreparedEvent` 在 `prepareContext` 阶段（refresh 之前）发布——DynamicDataSource BeanDefinition 在 refresh 前注册，会被 `finishBeanFactoryInitialization` 实例化。两种模式的子上下文都在启动时创建。

### 运行时事件流

```
外部配置变更（Nacos/Apollo）
  → PropertySourcesChangedEvent
    → PropagatingDynamicJdbcConfigChangedEventListener
      → 检查变更属性名是否匹配 DynamicJdbcConfig 的 Key
      → DynamicJdbcConfigChangedEvent
        → DynamicDataSource.RefreshingDynamicDataSourceListener
          → initializeDataSource() 重建

Zone 切换
  → ZoneContext.setZone("zone-b")
    → ZoneContextChangedEvent
      → PropagatingDynamicJdbcConfigChangedEventListener
        → 只处理 hasHighAvailabilityDataSource()=true 的 config
        → DynamicJdbcConfigChangedEvent
          → DynamicDataSource.RefreshingDynamicDataSourceListener
            → initializeDataSource() 重建（此时 getDataSourcePropertiesList() 返回新 Zone 的配置）
```

### 关闭事件流

```
父上下文 doClose()
  → ContextClosedEvent
    → DynamicJdbcChildContextRefreshedListener 注册的 Lambda
      → childContext.close()
        → DynamicDataSource.destroy()
          → 立即关闭当前子上下文（async=false）
          → 取消所有待执行的延迟关闭任务
    → SyncExecutionShutdownHookApplicationListener 注册的 Lambda
      → 同步执行 ShardingSphere 的 shutdown hook（Thread.run()）
  → destroyBeans()
    → 父上下文中的 Bean 销毁
```

---

## 配置热更新机制

### 触发方式

通过 `PropertySourcesChangedEvent`。这个事件来自 `microsphere-spring-boot` 的配置中心抽象层。当 Nacos/Apollo 等配置中心推送配置变更时，会触发此事件。

### 处理流程

```java
// 1. PropagatingDynamicJdbcConfigChangedEventListener 收到事件
onPropertySourcesChangedEvent(event) {
    Set<String> keys = event.getChangedProperties().keySet();
    for (String key : keys) {
        if (dynamicJdbcConfigPropertyNames.contains(key)) {
            // 2. 重新读取 Environment 中的 JSON 配置
            DynamicJdbcConfig config = getDynamicJdbcConfig(environment, propertyName);
            // 3. 发布 DynamicJdbcConfigChangedEvent
            publishDynamicJdbcConfigChangedEvent(config, propertyName);
        }
    }
}

// 4. DynamicDataSource 收到事件
RefreshingDynamicDataSourceListener.onApplicationEvent(event) {
    // 5. 匹配 propertyName
    if (Objects.equals(propertyName, this.dynamicJdbcConfigPropertyName)) {
        // 6. 找到正确的父上下文
        ConfigurableApplicationContext parentContext = findParentContext(sourceContext);
        // 7. 重建 DataSource
        initializeDataSource(config, propertyName, parentContext);
    }
}
```

### 热切换的代价

每次热更新会：重建子上下文（1-3 秒）→ 原子替换 delegate（µs 级）→ 60s 后关闭旧上下文。切换期间，旧连接继续可用（60s 延迟关闭），新连接使用新配置。

---

## 工程问题分析

### 子上下文的内存开销

每个子上下文是一个完整的 Spring 容器：

| 配置 | 额外内存 | 原因 |
|------|---------|------|
| 仅 DataSource | ~20-30 MB | BeanFactory + Environment + HikariCP 连接池 |
| + MyBatis | ~50-80 MB | 加上 SqlSessionFactory、Mappers、TypeHandler |
| + ShardingSphere | ~80-150 MB | 加上分片规则对象、多个 HikariCP 连接池 |

N 个子上下文的线程开销：每个子上下文至少多 1 个 HouseKeeper 线程（HikariCP 的连接池维护）+ 若干 Spring 基础设施线程。N=10 时至少多 10 个后台线程。

### 切换延迟

Zone 切换或配置热更新的耗时分布：

| 阶段 | 耗时 | 是否阻塞 getConnection() |
|------|------|------------------------|
| 克隆配置 | <1ms | ❌ |
| 创建子上下文 | 1-3s | ❌（不在锁内） |
| 子上下文 refresh（加载 Auto-Configuration、创建 Bean） | 1-3s | ❌（不在锁内） |
| 原子替换 delegate | <1µs | ✅（在锁内） |
| 延迟关闭旧上下文 | 60s（异步） | ❌ |

总阻塞时间 < 1µs（只在指针切换时加锁）。

### YAML 被解析两次

ShardingSphere 的 YAML 文件在 Synthesizer（第 5 步）和 Registrar（第 6 步）各被解析一次。如果 YAML 文件很大（数百条分片规则），两次解析浪费几十毫秒。可以用缓存解决。

### 线程安全

高频路径（`getConnection()`）无锁——通过 `volatile delegate` 保证可见性。只有切换路径（`initializeDataSource()`）加 `synchronized(mutex)`。错误收集用 `ConcurrentHashMap`。防重复处理用 `ConcurrentSkipListSet`。

### 与外部配置中心的集成

通过 `PropertySourcesChangedEvent` 抽象层，可以对接：
- Nacos（通过 `microsphere-configuration-nacos`）
- Apollo
- Spring Cloud Config
- 本地配置文件变更

只要实现了 `PropertySourcesChangedEvent` 的发布，18-dynamic 就能自动响应。

---

## 与 baomidou dynamic-datasource 对比

### 场景对比

| 场景 | baomidou | 18-dynamic |
|------|----------|-----------|
| 读写分离（主从） | ✅ 最佳选择 | ⚠️ 太重了 |
| 多租户（同一 ORM） | ✅ 推荐 | ❌ 不合适（每个租户需要不同 ORM 时才考虑） |
| MyBatis + MyBatis-Plus 共存 | ❌ 不支持 | ✅ |
| ShardingSphere + 普通表共存 | ❌ 不支持 | ✅ |
| Zone 感知多活 | ❌ 不支持 | ✅ |
| 配置中心热更新 | ❌ 不支持 | ✅ |
| 同库多 ORM | ⚠️ 共享 SqlSessionFactory | ⚠️ 需要多个 DataSource 实例 |

### 架构对比

```
baomidou 的架构：
  应用代码 → @DS("master") / @DS("slave")
                  ↓
        DynamicRoutingDataSource
           ├─ DataSource "master"
           └─ DataSource "slave"
  所有 DataSource 共享同一个 SqlSessionFactory

18-dynamic 的架构：
  应用代码 → 使用升迁到父上下文的 Bean
                  ↓
        DynamicDataSource（热替换壳）
           └─ DynamicJdbcChildContext[orders]
                ├─ HikariCP
                ├─ SqlSessionFactory (MyBatis)
                └─ TransactionManager
```

### 是否可以互相替代

**不能。** baomidou 解决的是"同一个 SqlSessionFactory 后面挂多个 DataSource，运行时路由"。18-dynamic 解决的是"多套完整的数据库栈互不干扰地共存"。它们解决的是不同层面的问题。

---

## 18-dynamic + baomidou 融合方案

如果把 baomidou 的请求级路由加到 18-dynamic 的每个子上下文中，可以实现两层架构：

```
应用层（请求级路由，µs 级）
  @DS("master") / @DS("slave")
    → baomidou DynamicRoutingDataSource
        ↓
配置层（Zone/配置变更时切换，秒级）
  18-dynamic DynamicDataSource（热替换壳）
        ↓
子上下文（隔离层）
  DynamicJdbcChildContext[orders]
    ├─ HikariCP "master"（读写库）
    ├─ HikariCP "slave"（只读库）
    ├─ DynamicRoutingDataSource（baomidou）
    │    ├─ master → HikariCP "master"
    │    └─ slave → HikariCP "slave"
    ├─ SqlSessionFactory → 绑定到 DynamicRoutingDataSource
    ├─ TransactionManager
    └─ Mapper 扫描
```

### JSON 配置示例

```json
{
  "name": "orders",
  "datasource": [
    { "name": "master", "url": "jdbc:mysql://host:3306/orders", "username": "root", "password": "root" },
    { "name": "slave", "url": "jdbc:mysql://host:3307/orders-ro", "username": "root", "password": "root" }
  ],
  "mybatis": { "base-packages": "com.example.orders.mapper" },
  "dynamic-datasource": {
    "enabled": true,
    "primary": "master",
    "strategy": "com.baomidou.dynamic.datasource.strategy.LoadBalanceDynamicDataSourceStrategy"
  }
}
```

### 需要改动的点

| 组件 | 改动 |
|------|------|
| DynamicJdbcConfig | 新增 `dynamic-datasource` 配置段 |
| DataSourceConfigurationPropertiesSynthesizer | 检测到 `dynamic-datasource.enabled=true` 时，合成 `spring.datasource.dynamic.*` 属性 |
| DynamicJdbcContextProcessor 第 6 步 | 注册 baomidou 的 DynamicRoutingDataSource Bean |
| banned-modules | baomidou 与 datasource 模块不互斥 |
| Bean 升迁 | DynamicRoutingDataSource 作为 exposed bean 升迁 |
| 子上下文 AOP | 需要启用 `@EnableAspectJAutoProxy` 使 `@DS` 生效 |

### 注意事项

1. `@DS` 注解只能在同一个子上下文的数据源列表内路由，不能跨子上下文
2. 每个子上下文多创建一个 baomidou 的 DynamicRoutingDataSource，增加少量内存
3. baomidou 的读写分离不支持 Zone 感知——如果需要 Zone + 读写分离两层路由，需要扩展 baomidou 的路由策略
4. Zone 切换时，子上下文整体重建，baomidou 的配置也会随子上下文重建而刷新

### 改进方案

#### 方案 A：在子上下文内嵌入 baomidou（推荐）

每个子上下文有独立的 HikariCP 实例池 + 独立的 DynamicRoutingDataSource。18-dynamic 管 Zone 切换，baomidou 管请求级路由。

**优点**：功能完全独立，改动小
**缺点**：Zone 切换时重建所有连接池（包括 master 和 slave）

#### 方案 B：Zone + 读写分离两层路由

在 DynamicDataSource 和 HikariCP 之间加两层：

```
DynamicDataSource（热替换壳，Zone 感知）
  └─ ZoneRoutingDataSource（Zone 路由，请求级）
       ├─ ZoneA DynamicRoutingDataSource（baomidou，读写分离）
       │    ├─ master → HikariCP ZoneA master
       │    └─ slave → HikariCP ZoneA slave
       └─ ZoneB DynamicRoutingDataSource（baomidou，读写分离）
            ├─ master → HikariCP ZoneB master
            └─ slave → HikariCP ZoneB slave
```

Zone 切换时，ZoneRoutingDataSource 的 lookupKey 变化（µs 级），不需要重建任何子上下文。适用于 Zone 切换频繁的场景。

**优点**：Zone 切换 µs 级
**缺点**：需要预建所有 Zone 的连接池，内存开销更大
