# S-28 Spring Data 仓库自动注册 — AbstractRepositoryConfigurationSourceSupport → RepositoriesRegistrar

> 项目: Spring Boot 3.x | 🟡 Working / 1 篇 | AbstractRepositoryConfigurationSourceSupport.java(140行)+OnRepositoryTypeCondition.java(60行)+ConditionalOnRepositoryType.java(45行)+RepositoryType.java(45行)+各 *RepositoriesRegistrar(redis/mongo/jdbc/jpa...)
> 基线: BOOT-PLAN-v2 S-28 (深探新增) — 仓库扫描注册; 前置: **S-10/S-11 + Spring Data 层(机制内核)** — 展开仓库注册编排; **JPA 仓库按现代主流降级**(Redis/Mongo/JDBC 为主)

---

## §0.8

- 🟡 Working，1篇 — 注册编排(AbstractRepositoryConfigurationSourceSupport implements ImportBeanDefinitionRegistrar: registerBeanDefinitions[L58]→delegate.registerRepositoriesIn(registry, getRepositoryConfigurationExtension())[L62]; getBasePackages[L78]) → 仓库类型选择(OnRepositoryTypeCondition[L35]: getTypeProperty[L52 读 spring.data.<store>.repositories.type] vs requiredType, AUTO 两者皆可[L44]; RepositoryType: AUTO/IMPERATIVE/REACTIVE) → 各仓库注册(JdbcRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport[L33])
- 设计模式: [模式: ImportBeanDefinitionRegistrar]—仓库注册; [模式: 条件选择]—@ConditionalOnRepositoryType

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| AbstractRepositoryConfigurationSourceSupport.java:48,58,62 | 注册 | **implements ImportBeanDefinitionRegistrar(L48)**: registerBeanDefinitions(L58)→delegate.registerRepositoriesIn(registry, extension)(L62) | High |
| AbstractRepositoryConfigurationSourceSupport.java:78,98,137 | 包与扩展 | **getBasePackages(L78)+抽象 getRepositoryConfigurationExtension(L98)**; AutoConfiguredSource.getBasePackages(L137) — 决定扫哪/用什么扩展 | High |
| OnRepositoryTypeCondition.java:35,38,44,52 | 类型选择 | **getMatchOutcome(L38)**: 配置类型 vs 要求类型, AUTO 皆可(L44); getTypeProperty(L52) | High |
| RepositoryType.java:30,35,45 | 枚举 | **AUTO(L30)/IMPERATIVE(L35)/REACTIVE(L45)** | High |
| jdbc/JdbcRepositoriesRegistrar.java:33 | 实例 | **extends AbstractRepositoryConfigurationSourceSupport(L33)** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 仓库注册是一条线(基类编排→类型选择→具体 registrar), 3 块耦合但整体是 delegation — 1篇 (~44行) 按"基类注册编排 → 仓库类型选择 → 具体 registrar 与边界"展开; Spring Data 内核(RepositoryConfigurationExtension)复用。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 注册编排 (AbstractRepositoryConfigurationSourceSupport) | 🔴 | **为什么🔴**: 仓库怎么被注册 |
| P1-2 | 仓库类型选择 (OnRepositoryTypeCondition + RepositoryType) | 🔴 | **为什么🔴**: imperative/reactive 选哪个 |
| P1-3 | 具体 Registrar (Jdbc/Mongo/Redis...) | 🔴 | **为什么🔴**: 各仓库驱动 |
| P2-1 | getBasePackages (扫描包决定) | 🟡 | **为什么🟡**: 扫哪个包 |
| P2-2 | JPA 仓库降级 (现代主流) | 🟡 | **为什么🟡**: JPA 靠边, Redis/Mongo/JDBC 为主 |
| P3-1 | 与 Spring Data 层边界 | 🟢 | **为什么🟢**: 内核在 Spring Data |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **注册编排** | 🔴 | 仓库怎么注册 |
| B | **类型选择** | 🔴 | 选哪种 |
| C | **具体与边界** | 🟡 | 驱动实例 |

> **Cluster A (§1)**: AbstractRepositoryConfigurationSourceSupport(registerBeanDefinitions/delegate/getBasePackages)
> **Cluster B (§2)**: OnRepositoryTypeCondition(AUTO/IMPERATIVE/REACTIVE) + RepositoryType
> **Cluster C (§3)**: 各 RepositoriesRegistrar(Jdbc 为例) + JPA 降级 + Spring Data 边界

→ 引出 S-29: SQL 初始化 — 仓库之后: SqlDataSourceScriptDatabaseInitializer 的 schema.sql/data.sql 执行编排(前置 S-10)
