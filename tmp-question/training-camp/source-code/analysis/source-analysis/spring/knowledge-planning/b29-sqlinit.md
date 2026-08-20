# S-29 SQL 初始化 — SqlInitializationAutoConfiguration → SqlDataSourceScriptDatabaseInitializer

> 项目: Spring Boot 3.x | 🟡 Working / 1 篇 | SqlInitializationAutoConfiguration.java(90行)+DataSourceInitializationConfiguration.java(60行)+SqlDataSourceScriptDatabaseInitializer.java(60行)+OnDatabaseInitializationCondition.java(70行)+SqlInitializationProperties.java(110行)+DataSourceScriptDatabaseInitializer.java(180行, boot/jdbc/init)+DatabaseInitializationMode.java(45行, boot/sql/init)
> 基线: BOOT-PLAN-v2 S-29 (深探新增) — schema.sql/data.sql 执行编排; 前置: **S-10 DataSource** — 展开 SQL 脚本初始化

---

## §0.8

- 🟡 Working，1篇 — 装配与条件(SqlInitializationAutoConfiguration: @AutoConfiguration + @ConditionalOnBooleanProperty(spring.sql.init.enabled, matchIfMissing=true)[L40] + mode=never 排除[L50]; DataSourceInitializationConfiguration: @Bean SqlDataSourceScriptDatabaseInitializer(dataSource, properties)) → 执行器(SqlDataSourceScriptDatabaseInitializer extends DataSourceScriptDatabaseInitializer: 先 schema.sql 后 data.sql) → 模式与配置(OnDatabaseInitializationCondition: mode 非 NEVER 才执行[L67]; SqlInitializationProperties: schemaLocations/dataLocations/mode[默认 EMBEDDED]/separator/continueOnError)
- 设计模式: [模式: 条件装配]—enabled/mode 条件; [模式: 模板方法]—DataSourceScriptDatabaseInitializer; [模式: 生命周期钩子]—refresh 期间(Bean 初始化)初始化

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| SqlInitializationAutoConfiguration.java:40,50 | 条件 | **@ConditionalOnBooleanProperty(spring.sql.init.enabled, matchIfMissing=true)(L40)+@ConditionalOnProperty(mode=never)(L50 排除)** | High |
| DataSourceInitializationConfiguration.java:37,38,40 | 装配 | **@Bean SqlDataSourceScriptDatabaseInitializer(dataSource, properties)(L37-40)** | High |
| SqlDataSourceScriptDatabaseInitializer.java:34 | 执行器 | **extends DataSourceScriptDatabaseInitializer(L34)** — schema.sql→data.sql | High |
| OnDatabaseInitializationCondition.java:57,67 | 模式 | **getMatchOutcome(L57)→match: !mode.equals(NEVER)(L67)** | High |
| SqlInitializationProperties.java:38,43,81 | 配置 | **schemaLocations(L38)/dataLocations(L43)/mode=EMBEDDED(L81)** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: SQL 初始化是一条线(条件→执行器→模式配置), 3 块耦合但机制较薄 — 1篇 (~44行) 按"装配条件 → 执行器 → 模式配置"展开; DataSourceScriptDatabaseInitializer 内核复用。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 装配与条件 (enabled/mode 条件) | 🔴 | **为什么🔴**: 何时执行脚本 |
| P1-2 | 执行器 (SqlDataSourceScriptDatabaseInitializer + 顺序) | 🔴 | **为什么🔴**: schema/data.sql 怎么跑 |
| P1-3 | 执行模式 (EMBEDDED/ALWAYS/NEVER) | 🔴 | **为什么🔴**: 哪些库执行 |
| P2-1 | 配置属性 (schema/data locations + separator) | 🟡 | **为什么🟡**: 脚本位置/分隔 |
| P2-2 | 生命周期 (refresh 期间 afterPropertiesSet, DatabaseInitializer) | 🟡 | **为什么🟡**: 何时初始化 |
| P3-1 | 与 S-10 边界 | 🟢 | **为什么🟢**: DataSource 复用 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **装配条件** | 🔴 | 何时执行 |
| B | **执行器** | 🔴 | 怎么执行 |
| C | **模式配置** | 🟡 | 哪些库 |

> **Cluster A (§1)**: SqlInitializationAutoConfiguration(enabled/mode 条件) + DataSourceInitializationConfiguration(@Bean)
> **Cluster B (§2)**: SqlDataSourceScriptDatabaseInitializer(schema→data) + OnDatabaseInitializationCondition(mode)
> **Cluster C (§3)**: SqlInitializationProperties(locations/mode/separator) + 生命周期 + S-10 边界

→ 引出 BOOT 深探新增收束: S-25~S-29 完成 — 至此 BOOT-PLAN-v2 29 域 (S-1~S-29) 全部规划
