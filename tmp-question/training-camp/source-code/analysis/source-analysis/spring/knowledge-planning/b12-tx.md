# S-12 事务自动配置 — @EnableTransactionManagement 自动启用 + 管理器装配

> 项目: Spring Boot 3.x | 🟡 Working / 1 篇 | TransactionAutoConfiguration+DataSourceTransactionManagerAutoConfiguration+TransactionManagerCustomizers+TransactionProperties
> 基线: BOOT-PLAN-v2 S-12 — 事务自动装配; 前置: **s29-s33 (事务链机制全部复用) + S-10 (DataSource 复用)** — 展开"自动启用+管理器装配"

---

## §0.8

- 🟡 Working，1篇 — 自动启用(TransactionAutoConfiguration: @ConditionalOnClass(PlatformTransactionManager) + @ConditionalOnMissingBean(AbstractTransactionManagementConfiguration) → @EnableTransactionManagement — **不用写注解就启用声明式事务**) → 管理器装配(DataSourceTransactionManagerAutoConfiguration: @ConditionalOnClass(DataSource/JdbcTemplate/TransactionManager) → transactionManager @ConditionalOnMissingBean(TransactionManager): DataSource/JdbcTransactionManager 选择) → 定制(TransactionManagerCustomizers: 应用 PlatformTransactionManagerCustomizer) → 与 s29-s33 边界
- 设计模式: [模式: 条件自动启用]—@EnableTransactionManagement; [模式: 用户优先]—@ConditionalOnMissingBean; [模式: 定制器]—TransactionManagerCustomizers

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| TransactionAutoConfiguration.java:45,47 | 自动启用 | **@AutoConfiguration L45**: @ConditionalOnClass(PlatformTransactionManager)(L46) — 事务相关在 classpath | High |
| TransactionAutoConfiguration.java:70,74 | @Enable | **EnableTransactionManagement 配置 L70-74**: @ConditionalOnMissingBean(AbstractTransactionManagementConfiguration)(L70) → @EnableTransactionManagement(proxyTargetClass)(L74) — **用户写 @EnableTransactionManagement 则自动装配跳过** | High |
| DataSourceTransactionManagerAutoConfiguration.java:49,53 | 管理器 | **@AutoConfiguration L49**: @ConditionalOnClass(DataSource/JdbcTemplate/TransactionManager)(L51) | High |
| DataSourceTransactionManagerAutoConfiguration.java:60,61,70 | 创建 | **transactionManager L61**: @ConditionalOnMissingBean(TransactionManager)(L60) — createTransactionManager L70: 环境条件选 JdbcTransactionManager(新) / DataSourceTransactionManager | High |
| TransactionManagerCustomizers.java | 定制 | **定制器**: 收集 PlatformTransactionManagerCustomizer beans → 应用到事务管理器 — 扩展点 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 自动启用+管理器装配约 400 行 — 知识主线: "自动启用声明式事务 → 创建事务管理器 → 定制". 1篇 (~45行) 按"启用→管理器→定制→边界"展开; s29-s33 事务链机制复用(只讲装配)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | @EnableTransactionManagement 自动启用 (不用写注解) | 🔴 | **为什么🔴**: 声明式事务零配置的核心 — 条件跳过用户显式启用 |
| P1-2 | DataSourceTransactionManager 自动创建 (条件+用户优先) | 🔴 | **为什么🔴**: 事务管理器怎么来 — DataSource/Jdbc 选择 |
| P1-3 | TransactionManagerCustomizers (定制器扩展) | 🔴 | **为什么🔴**: 统一定制事务管理器的方式 |
| P2-1 | 与 s29-s33 边界 (机制 vs 装配) | 🟡 | **为什么🟡**: 事务链机制在 s29-s33, 本域只讲自动装配 |
| P2-2 | TransactionProperties (spring.transaction.*) | 🟡 | **为什么🟡**: 事务默认配置(S-5) |
| P3-1 | 用户自定义 TransactionManager 覆盖 | 🟢 | **为什么🟢**: @ConditionalOnMissingBean 应用 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **自动启用** (TransactionAutoConfiguration) | 🔴 | 零配置声明式事务 |
| B | **管理器装配** (DataSource 版) | 🔴 | 事务管理器创建 |
| C | **定制与边界** (Customizers + s29-s33) | 🟡 | 扩展与分工 |

> **Cluster A (§1)**: TransactionAutoConfiguration(@ConditionalOnMissingBean → @EnableTransactionManagement) — 用户显式启用则跳过
> **Cluster B (§2)**: DataSourceTransactionManagerAutoConfiguration(条件 + transactionManager L61 创建) — DataSource/JdbcTransactionManager 选择
> **Cluster C (§3)**: TransactionManagerCustomizers + TransactionProperties + s29-s33 边界(事务链机制复用)

→ 引出 S-13: 缓存自动配置 — 事务旁的另一横切: CacheAutoConfiguration 与 CacheManager 选择(s19 @Cacheable 机制复用)

(End of file - total 61 lines)
