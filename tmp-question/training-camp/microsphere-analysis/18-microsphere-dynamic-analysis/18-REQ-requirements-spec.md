# 18-REQ：动态多数据源基础设施需求规格

> 以与 baomidou dynamic-datasource 对比为基准，描述动态多数据源配置隔离的需求。
> 基准：Java 17+，Spring Boot 3.x，MyBatis/MyBatis-Plus，ShardingSphere 5.x

---

## 项目定位

**18-dynamic（81 文件）是一个"数据库基础设施即配置"的 Spring Boot Starter**。你写一段 JSON 配置描述一个数据库单元（DataSource + MyBatis + TransactionManager + 可选 ShardingSphere），Starter 自动为每个单元拉起一个**独立的 Spring 子上下文**——不是 ThreadLocal 路由，是完整的 Bean 生命周期隔离。

**与 baomidou dynamic-datasource 的根本区别**：

| | baomidou dynamic-datasource | 18-dynamic |
|---|---|---|
| 机制 | `@DS` 注解 + ThreadLocal 路由 | Spring 子上下文（`AnnotationConfigApplicationContext` per unit） |
| 隔离粒度 | 请求级切换（同一个 SqlSessionFactory 后面挂多个 DataSource） | **配置级隔离**（每个单元独立 SqlSessionFactory + TransactionManager） |
| 不同 ORM 共存 | 不支持（共享 SqlSessionFactory） | 支持（MyBatis + MyBatis-Plus 独立配置） |
| ShardingSphere | 不支持 | 原生支持 |
| Zone 感知 | 不支持 | 支持（`ha-datasource` 按 zone 分组，与 17 联动） |
| 配置热更新 | 不支持 | 支持（事件驱动重建子上下文） |

**源码信息**：单模块 `microsphere-dynamic-jdbc-spring-boot`（81 文件）。

---

## 一、JSON 配置驱动的数据库基础设施

### REQ-001：JSON → 子上下文的自动装配

**问题**：Spring Boot 假设每个应用只有一套 DataSource + 一套 SqlSessionFactory。当你有两套独立的数据库栈（订单库 MyBatis + 用户库 MyBatis-Plus），标准 Boot AutoConfiguration 会冲突——两个 `DataSourceAutoConfiguration` 抢同一个 `@Primary` bean。

**产出**：`microsphere.dynamic.jdbc.configs.{name}` JSON 属性——一行 JSON 描述一个完整的数据库单元：

```json
{
  "name": "orders",
  "datasource": [{"url": "jdbc:mysql://db1:3306/orders", ...}],
  "mybatis": {"base-packages": "com.example.orders.mapper"},
  "transaction": {"name": "order-tx"}
}
```

Starter 自动为这个 JSON 拉起一个子 Spring 上下文：
```
DynamicJdbcChildContext[orders]
  ├─ HikariDataSource
  ├─ SqlSessionFactory（扫描 com.example.orders.mapper）
  └─ PlatformTransactionManager "order-tx"
```

每个单元可以有不同的 ORM（另一个单元用 MyBatis-Plus）、不同连接池（HikariCP/Druid/自定义）、不同 TransactionManager。

**状态**：[已验证实现]

---

### REQ-002：4 个 SPI × 6 个模块的管道架构

**问题**：每个数据库单元需要经过 4 个处理阶段：配置默认值填充 → 校验 → 属性合成 → Bean 注册。6 个模块（datasource/transaction/sharding-sphere/mybatis/mybatis-plus/ha-datasource）各有不同处理逻辑。

**产出**：4 个 SPI 管道：

| SPI | 职责 | 示例 |
|-----|------|------|
| `ConfigPostProcessor` | 填充默认值（URL scheme、driverClassName 推导） | datasource 模块从 `jdbc:mysql://...` 推导 `com.mysql.cj.jdbc.Driver` |
| `ConfigValidator` | 校验配置合法性 | mybatis + mybatis-plus 互斥检查 |
| `ConfigConfigurationPropertiesSynthesizer` | JSON → Spring Environment 属性 | 将 JSON 属性合成到子上下文的 PropertySource |
| `ConfigBeanDefinitionRegistrar` | 注册 Bean（DataSource、SqlSessionFactory、TransactionManager） | transaction 模块注册 TransactionManager bean 并设置别名 |

6 个模块按需实现这 4 个 SPI——新增模块只需实现接口 + 在 spring.factories 注册。

**状态**：[已验证实现]

---

## 二、子上下文隔离

### REQ-003：独立 Bean 生命周期的 Spring 子上下文

**产出**：`DynamicJdbcChildContext`（继承 `AnnotationConfigApplicationContext`）——`refresh()` 时通过 `ConfigurationPropertySources.attach()` 合并父 Environment，再注册模块的 AutoConfiguration（通过 `DynamicJdbcChildContextConfiguration`）。父上下文关闭时自动级联关闭所有子上下文。

隔离维度：

| 维度 | 效果 |
|------|------|
| Bean 定义 | 每个子上下文独立 `DefaultListableBeanFactory`，同名 Bean 不冲突 |
| AutoConfiguration | 各自独立执行 `DataSourceAutoConfiguration` |
| 属性 | 子上下文继承父 Environment，Synthesizer 合成属性更高优先级 |
| 生命周期 | 父上下文关闭 → 自动关所有子上下文 |

**状态**：[已验证实现]

---

## 三、Zone 感知的高可用数据源

### REQ-004：按 Zone 分组的数据源切换

**问题**：多活架构中，Zone A 的实例用 Zone A 的数据库、Zone B 的实例用 Zone B 的数据库。但 Zone 切换时不只是换连接池——MyBatis 配置和 TransactionManager 也要一起换。

**产出**：`ha-datasource` 配置——按 zone 分组的数据源列表：

```json
{
  "ha-datasource": {
    "zone-a": [{"url": "jdbc:mysql://db-a1:3306/orders"}, {"url": "jdbc:mysql://db-a2:3306/orders"}],
    "zone-b": [{"url": "jdbc:mysql://db-b1:3306/orders"}]
  }
}
```

Zone 切换（17 的 `ZoneContext.setZone()`）时——18 监听 `PropertySourcesChangedEvent` → 重建子上下文（切换 HA 数据源 + 关联的 MyBatis 配置 + TransactionManager）。

**状态**：[已验证实现]

---

## 四、配置热更新

### REQ-005：事件驱动的配置变更自动重建

**产出**：`DynamicJdbcConfigEventListener` 监听 `PropertySourcesChangedEvent` → 识别变更的 config → 关闭旧子上下文 → 重建新子上下文。配置中心（Nacos/Apollo）改了 JSON 配置，应用无需重启。

**状态**：[已验证实现]

---

## 五、已知缺陷

| 缺陷 | 说明 |
|------|------|
| 无跨数据源事务协调 | 每个子上下文独立 TransactionManager——跨单元操作无事务保证 |

---

## 六、发散需求

### REQ-N01：数据源健康评分

当前只有 `HikariDataSource` 的连接池健康指标——缺失"连接成功率"、"平均响应时间"维度的评分和自动摘除。

### REQ-N02：配置迁移灰度工具

运维想"把 orders 从 `db-a1` 迁移到 `db-b1`"——需要修改 JSON → 重建子上下文。缺少一个灰度迁移命令：先建新数据源 → 流量逐步切换 → 再关旧数据源。

### REQ-N03：跨单元事务（SAGA 模式）

两个子上下文各自的 TransactionManager 独立——跨单元操作需要最终一致性保证。

---

## 七、版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| — | 2026-08-03 | v2 重写——修正 ThreadLocal 谬误（实际是子上下文），基于 12 篇已有分析文档 |
