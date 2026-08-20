# S-13 缓存自动配置 — CacheAutoConfiguration (CacheManager 多实现)

> 依赖 s19 (复用) + S-5 | 🟡 Working | 6 KP | [模式: 切面驱动条件 + ImportSelector 选择]

**读者处境**: @EnableCaching 后 CacheManager 谁建?Caffeine/Redis/Simple 怎么选?为什么 @EnableCaching 是前提?

### 1. 激活条件 — 切面驱动

场景: 缓存自动装配的条件独特: 不只是"缓存类在 classpath" — 还要求**缓存切面已启用**。

源码路径:
- `CacheAutoConfiguration.java:58,63` — **条件**: @ConditionalOnClass(CacheManager)(L58 — 缓存 API 在 classpath) + **@ConditionalOnBean(CacheAspectSupport)(L59 — 容器有缓存切面 Bean, 即 @EnableCaching 已启用)** + @ConditionalOnMissingBean(CacheManager/cacheResolver)(L60 — 用户自定义缓存管理器优先)
- @Import(CacheConfigurationImportSelector)(L62) + CacheManagerCustomizers(L66-68)

关键设计: **Why @ConditionalOnBean(CacheAspectSupport)？** 缓存管理器只在"缓存真正被使用"(@EnableCaching 启用切面)时才需要 — 没启用缓存却建 CacheManager 是浪费; 这也让"@EnableCaching 之前没有 CacheManager, 之后自动出现"。与 S-12 事务的自动启用(无条件式)形成对照。[模式: 切面驱动条件]

数据流: 用户 @EnableCaching → 导入 CacheAspectSupport(切面, s19 机制) → CacheAutoConfiguration 评估: @ConditionalOnClass(CacheManager)✓ + @ConditionalOnBean(CacheAspectSupport)✓(刚被启用) + @ConditionalOnMissingBean(CacheManager)✓ → 激活 → 选实现(§2)。用户自定义 CacheManager bean → 跳过。

### 2. 多实现选择 — CacheConfigurationImportSelector

场景: Caffeine/Redis/JCache/Simple 都可能在 classpath — 用哪个?ImportSelector 按条件选择实现配置类。

源码路径:
- `CacheAutoConfiguration.java:115,118` — **CacheConfigurationImportSelector L115**: selectImports L118 — 返回候选实现配置类列表, 由各自的 @ConditionalOnClass 决定谁生效
- `CaffeineCacheConfiguration.java:42,45,48` — **Caffeine**: @ConditionalOnClass({Caffeine, CaffeineCacheManager})(L42) → CaffeineCacheManager L48
- `SimpleCacheConfiguration.java:36,39` — **Simple 兜底**: ConcurrentMapCacheManager L39 — 无任何缓存库时的最后选择

关键设计: **Why ImportSelector + 各自条件？** 与 S-8 容器选择同模式: 选择器返回全部候选配置类, 每个带 @ConditionalOnClass(对应缓存库)— classpath 有谁用谁; 多库并存时按顺序(Simple 最后兜底)。**Why Simple 兜底？** 无缓存库也能 @Cacheable(内存 ConcurrentMap) — 保证功能可用。[模式: ImportSelector + 条件装配]

数据流: CacheConfigurationImportSelector.selectImports → 返回 [RedisCacheConfiguration, CaffeineCacheConfiguration, SimpleCacheConfiguration, ...] → 逐个条件评估: 有 Caffeine 依赖(无 Redis)→ Caffeine 配置激活 → CaffeineCacheManager; 全无缓存库 → SimpleCacheConfiguration → ConcurrentMapCacheManager(内存)。

### 3. 创建与定制 + s19 边界

场景: CacheManager 怎么建?怎么统一定制?与 s19 什么关系?

源码路径:
- 各实现配置类 @Bean: CaffeineCacheManager(cacheProperties, customizers)(L48)/ConcurrentMapCacheManager(L39) — 用 CacheProperties(spring.cache.*, S-5)构建
- `CacheAutoConfiguration.java:67` — **CacheManagerCustomizers**: 收集 CacheManagerCustomizer beans → 应用到每个 CacheManager — 横切定制
- 边界: 缓存**机制**(@Cacheable 拦截器/SpEL key/失效)在 s19 — 本域只讲"CacheManager 装配"

关键设计: **Why Customizers？** 与 S-12 事务定制器同构: 自动装配创建的 CacheManager 允许横切定制(统一过期策略); 用户也可直接自定义 CacheManager(@ConditionalOnMissingBean 让位)。**Why 与 s19 边界？** 06 §2.5: 机制(s19)引用, 装配(本域)展开。[模式: 定制器 + 边界声明]

数据流: CaffeineCacheConfiguration 激活 → CaffeineCacheManager(cacheProperties: spring.cache.cache-names 等, customizers 应用) → CacheManager bean → 注入 s19 的 CacheInterceptor(切面使用) → @Cacheable 方法走缓存。用户 @Bean CacheManager(Redis) → 跳过自动装配。

→ 引出 S-14: TaskExecutor 自动配置 — 数据/缓存层收束, 异步: TaskExecutionAutoConfiguration — @Async 默认线程池装配(C-7 机制复用)。
