# S-10 DataSource 自动装配 — Embedded/Pooled 双分支 (DataSourceAutoConfiguration)

> 项目: Spring Boot 3.x | 🟡 Working / 1 篇 | DataSourceAutoConfiguration+EmbeddedDatabaseCondition+EmbeddedDataSourceConfiguration+DataSourceConfiguration(Hikari/Tomcat/Dbcp2)
> 基线: BOOT-PLAN-v2 S-10 — 数据访问层首域; 前置: **C-11 DataSource(池化机制复用) + 阶段3 HikariCP(深入在阶段3)** — 展开自动装配的嵌入式/池化双分支

---

## §0.8

- 🟡 Working，1篇 — 装配条件(@ConditionalOnClass(DataSource/EmbeddedDatabaseType) + @ConditionalOnMissingBean(r2dbc)) → 双分支(①@Conditional(EmbeddedDatabaseCondition): 无 url+嵌入式驱动→EmbeddedDataSourceConfiguration(H2/HSQL/Derby) ②PooledDataSourceConfiguration: Hikari/Tomcat/Dbcp2 条件选择) → 池创建(DataSourceConfiguration.Hikari: @ConditionalOnClass+@ConditionalOnProperty(spring.datasource.type) → createDataSource → HikariDataSource — C-11 机制复用) → 覆盖(spring.datasource.type/用户 DataSource bean 优先)
- 设计模式: [模式: 条件双分支]—嵌入式 vs 池化; [模式: 条件装配]—池实现选择; [模式: 用户优先]—@ConditionalOnMissingBean

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| DataSourceAutoConfiguration.java:59,64 | 装配声明 | **@AutoConfiguration L59**: @ConditionalOnClass(DataSource/EmbeddedDatabaseType)(L60) + @ConditionalOnMissingBean(r2dbc ConnectionFactory)(L61) — 数据源相关类在 classpath 才生效 | High |
| DataSourceAutoConfiguration.java:67,69 | 嵌入式分支 | **@Conditional(EmbeddedDatabaseCondition)(L67) + @ConditionalOnMissingBean(DataSource/XADataSource)(L68) → @Import(EmbeddedDataSourceConfiguration)(L69)** | High |
| DataSourceAutoConfiguration.java:76,77 | 池化分支 | **@ConditionalOnMissingBean(DataSource)(L76) → @Import(DataSourceConfiguration.Hikari/Tomcat/Dbcp2)(L77)** — 按池依赖条件选择 | High |
| DataSourceAutoConfiguration.java:133,142 | 嵌入式判定 | **getMatchOutcome L142**: 无 spring.datasource.url(L135) 且 EmbeddedDatabaseConnection.get 命中嵌入式驱动(L154) → 匹配 | High |
| DataSourceConfiguration.java:110,112 | Hikari 创建 | **Hikari 配置**: @ConditionalOnClass(HikariDataSource)(L110) + @ConditionalOnProperty(spring.datasource.type=HikariDataSource, matchIfMissing)(L112) — **Hikari 为默认池** | High |
| DataSourceConfiguration.java:52 | 创建 | **createDataSource L52**: JdbcConnectionDetails→new HikariDataSource+设置 url/用户/密码 — C-11 池化机制复用 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 自动装配+条件+池配置约 500 行核心 — 知识主线: "条件激活 → 嵌入式/池化双分支 → 池创建". 1篇 (~46行) 按"条件→双分支→池"展开; C-11 池化机制复用, 阶段3 HikariCP 深入标注。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | Embedded/Pooled 双分支 (EmbeddedDatabaseCondition 判定) | 🔴 | **为什么🔴**: 数据源选择的第一个分叉 — url 缺省+嵌入式驱动→嵌入式库 |
| P1-2 | 池化选择 (Hikari/Tomcat/Dbcp2 条件 + Hikari 默认) | 🔴 | **为什么🔴**: 池实现怎么选 — spring.datasource.type 覆盖 |
| P1-3 | createDataSource (JdbcConnectionDetails→池) | 🔴 | **为什么🔴**: 连接细节的封装 — 池创建的统一入口 |
| P2-1 | @ConditionalOnMissingBean 用户优先 (自定义 DataSource) | 🟡 | **为什么🟡**: 用户数据源覆盖自动装配 |
| P2-2 | spring.datasource.url/type 配置语义 | 🟡 | **为什么🟡**: 关键配置属性(S-5 绑定) |
| P3-1 | 与阶段3 HikariCP 边界 (深入在阶段3) | 🟢 | **为什么🟢**: 分工 — 池内核在阶段3 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **条件与分支** (激活条件 + 双分支) | 🔴 | 数据源怎么选 |
| B | **池化装配** (池条件选择 + 创建) | 🔴 | 池怎么建 |
| C | **配置与边界** (type/url + 用户优先) | 🟡 | 配置与扩展 |

> **Cluster A (§1)**: DataSourceAutoConfiguration 条件 + EmbeddedDatabaseCondition(无 url+嵌入式驱动)
> **Cluster B (§2)**: PooledDataSourceConfiguration(Hikari/Tomcat/Dbcp2) + DataSourceConfiguration.Hikari(@ConditionalOnProperty 默认池) + createDataSource
> **Cluster C (§3)**: spring.datasource.type/url 配置 + 用户 DataSource 覆盖 + 阶段3 HikariCP 深入标注

→ 引出 S-11: Redis 自动装配 — 数据访问延续: RedisAutoConfiguration 条件装配与 RedisTemplate(只讲接线, 深入在阶段3)

(End of file - total 61 lines)
