# S-13 缓存自动配置 — CacheAutoConfiguration (CacheManager 多实现选择)

> 项目: Spring Boot 3.x | 🟡 Working / 1 篇 | CacheAutoConfiguration+CacheConfigurationImportSelector+CaffeineCacheConfiguration+SimpleCacheConfiguration+CacheManagerCustomizers+CacheProperties
> 基线: BOOT-PLAN-v2 S-13 — 缓存自动装配; 前置: **s19 @Cacheable(机制复用) + S-5** — 展开 CacheManager 选择与创建

---

## §0.8

- 🟡 Working，1篇 — 激活条件(@ConditionalOnClass(CacheManager) + **@ConditionalOnBean(CacheAspectSupport)** — 缓存切面启用才装配 + @ConditionalOnMissingBean(CacheManager)) → 多实现选择(CacheConfigurationImportSelector: 按 CacheType 选配置 → 各实现 @ConditionalOnClass: Caffeine/Redis/JCache/Simple 兜底) → 创建与定制(各配置类 @Bean CacheManager + CacheManagerCustomizers 应用) → s19 边界
- 设计模式: [模式: 切面驱动条件]—CacheAspectSupport; [模式: ImportSelector 选择]—多实现; [模式: 定制器]—CacheManagerCustomizers

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| CacheAutoConfiguration.java:58,63 | 条件 | **@ConditionalOnClass(CacheManager)(L58) + @ConditionalOnBean(CacheAspectSupport)(L59 — 缓存切面启用才装配) + @ConditionalOnMissingBean(CacheManager/cacheResolver)(L60)** | High |
| CacheAutoConfiguration.java:62,115,118 | 选择器 | **@Import(CacheConfigurationImportSelector)(L62) → L115 selectImports L118**: 按 CacheType 选实现配置类 | High |
| CaffeineCacheConfiguration.java:42,45,48 | Caffeine | **@ConditionalOnClass(Caffeine/CaffeineCacheManager)(L42) → CaffeineCacheManager L48** | High |
| SimpleCacheConfiguration.java:36,39 | 兜底 | **SimpleCacheConfiguration: ConcurrentMapCacheManager L39** — 无缓存库时的兜底 | High |
| CacheAutoConfiguration.java:66,67 | 定制 | **cacheManagerCustomizers L67**: 收集 CacheManagerCustomizer → 应用到 CacheManager | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 条件+选择器+实现配置约 500 行 — 知识主线: "切面启用 → 按缓存库选实现 → 创建定制". 1篇 (~45行) 按"条件→选择→创建→边界"展开; s19 缓存机制复用(只讲装配)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | @ConditionalOnBean(CacheAspectSupport) 切面驱动 | 🔴 | **为什么🔴**: 缓存装配的独特条件 — 有 @EnableCaching 切面才装配 |
| P1-2 | CacheConfigurationImportSelector 多实现选择 | 🔴 | **为什么🔴**: 按缓存库选 CacheManager — Caffeine/Redis/Simple 兜底 |
| P1-3 | CacheManager 创建与定制 (Customizers) | 🔴 | **为什么🔴**: 各实现 @Bean + 统一定制 |
| P2-1 | 与 s19 边界 (机制 vs 装配) | 🟡 | **为什么🟡**: 缓存拦截器机制在 s19 |
| P2-2 | CacheProperties (spring.cache.*) | 🟡 | **为什么🟡**: 缓存配置(S-5) |
| P3-1 | 用户自定义 CacheManager 覆盖 | 🟢 | **为什么🟢**: @ConditionalOnMissingBean |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **激活条件** (切面驱动 + 缺省) | 🔴 | 何时装配 |
| B | **多实现选择** (ImportSelector + 条件) | 🔴 | 用哪个缓存实现 |
| C | **创建与边界** (Bean + Customizers + s19) | 🟡 | 创建定制与分工 |

> **Cluster A (§1)**: CacheAutoConfiguration 三条件(Class/Bean(CacheAspectSupport)/MissingBean)
> **Cluster B (§2)**: CacheConfigurationImportSelector(CacheType) + Caffeine/Redis/Simple 各实现条件
> **Cluster C (§3)**: CacheManager 创建(各 @Bean) + CacheManagerCustomizers + s19 边界

→ 引出 S-14: TaskExecutor 自动配置 — 数据/缓存层收束, 异步: TaskExecutionAutoConfiguration — @Async 默认线程池装配(C-7 机制复用)

(End of file - total 61 lines)
