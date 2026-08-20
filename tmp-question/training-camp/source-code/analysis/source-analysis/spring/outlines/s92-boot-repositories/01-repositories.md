# S-28 Spring Data 仓库自动注册 — AbstractRepositoryConfigurationSourceSupport → RepositoriesRegistrar

> 依赖 S-10/S-11 + Spring Data 层 (复用) | 🟡 Working | 6 KP | [模式: ImportBeanDefinitionRegistrar + 条件选择]

**读者处境**: 定义 `interface UserRepo extends JpaRepository` 后, 不用 @EnableJpaRepositories 就能自动注册成 Bean — 谁扫描并注册仓库?reactive 和 imperative 仓库怎么选?

### 1. 注册编排 — AbstractRepositoryConfigurationSourceSupport

场景: 一个仓库接口怎么自动变成容器 Bean?谁触发注册?

源码路径:
- `AbstractRepositoryConfigurationSourceSupport.java:48,49` — **基类**: `implements ImportBeanDefinitionRegistrar, BeanFactoryAware, ...`(L48-49) — 各仓库驱动的公共基座
- `AbstractRepositoryConfigurationSourceSupport.java:58,62` — **注册**: `registerBeanDefinitions`(L58) → `delegate.registerRepositoriesIn(registry, getRepositoryConfigurationExtension())`(L62) — 委托 Spring Data 注册仓库 BeanDefinition
- `AbstractRepositoryConfigurationSourceSupport.java:78,137` — **包/扩展**: `getBasePackages`(L78) + AutoConfiguredSource 的 `getBasePackages`(L137) — 决定扫描哪些包; `getRepositoryConfigurationExtension`(L98, 抽象) — 选对应 Spring Data 扩展

关键设计: **Why ImportBeanDefinitionRegistrar？** 仓库是接口, 运行时由 Spring Data 动态生成实现 — 需在配置阶段"注册仓库 BeanDefinition", 用 Registrar 在 @Configuration 处理时回调注册; 基类统一"委托 Spring Data + 定包 + 定扩展", 各数据模块只提供扩展。[模式: ImportBeanDefinitionRegistrar]

数据流: 自动装配导入各 *RepositoriesAutoConfiguration → @Import(RepositoriesRegistrar) → registerBeanDefinitions(L58) → delegate.registerRepositoriesIn(registry, extension)(L62) → Spring Data 按 getBasePackages 扫描仓库接口 → 注册 BeanDefinition。

### 2. 仓库类型选择 — OnRepositoryTypeCondition + RepositoryType

场景: 一个存储既可 imperative(阻塞)也可 reactive — 到底注册哪种?

源码路径:
- `RepositoryType.java:30,35,45` — **枚举**: `AUTO`(L30)/`IMPERATIVE`(L35)/`REACTIVE`(L45)
- `OnRepositoryTypeCondition.java:35,38,44,52` — **条件**: `getMatchOutcome`(L38) → `getTypeProperty(environment, store)`(L52, 读 `spring.data.<store>.repositories.type`) vs `requiredType`(L42); 配置为 AUTO 或等于要求类型则命中(L44)
- 各 RepositoriesRegistrar 上标 `@ConditionalOnRepositoryType(type=...)` — 按类型决定是否装配

关键设计: **Why AUTO/IMPERATIVE/REACTIVE 三态？** 同一存储可阻塞/响应式访问 — AUTO 让 Boot 按 web 类型自动选, 也可显式配 IMPERATIVE/REACTIVE; 条件在装配期决定注册哪种仓库, 避免两种都注册冲突。[模式: 条件选择]

数据流: 配 spring.data.mongo.repositories.type=reactive → OnRepositoryTypeCondition.getTypeProperty(L52)=REACTIVE → ReactiveRepositoriesRegistrar 的 @ConditionalOnRepositoryType(REACTIVE) 命中 → 注册响应式仓库; 默认 AUTO 则按 web 类型推断。

### 3. 具体 Registrar 与边界 — Jdbc 实例 + JPA 降级

场景: 不同存储的 registrar 怎么用同一个基类?现代主流该侧重哪些?

源码路径:
- `jdbc/JdbcRepositoriesRegistrar.java:33` — **实例**: `class JdbcRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport`(L33) — 只提供 getRepositoryConfigurationExtension 指向 JdbcRepositoryConfigurationExtension
- 同类: MongoRepositoriesRegistrar / RedisRepositoriesRegistrar / (JpaRepositoriesRegistrar) 等 — 每个存储一个 registrar, 继承基类
- 边界: Spring Data 的 RepositoryConfigurationExtension(扫描/实现生成)是内核, 在 Spring Data 层

关键设计: **Why 每存储一个 Registrar？** 每个数据模块提供自己的扩展与类型配置, 但注册编排(委托/包/扩展)全部复用基类 — 新增数据模块只需写十几行子类。**Why JPA 降级？** 现代主流/中文后端 MyBatis/MP 为主(阶段3), JPA 仓库的 Spring Data JPA 部分按相关性地降级, 本域以 Redis/Mongo/JDBC 仓库为主线; 机制对所有 Spring Data 模块通用。[模式: 模板方法 + 相关性过滤]

数据流: 引入 spring-data-jdbc → JdbcRepositoriesRegistrar(L33) 继承基类 → 复用 registerBeanDefinitions(L58)→JdbcRepositoryConfigurationExtension 扫描 JdbcRepository 接口 → 注册。Redis/Mongo 同构; JPA 仓库机制相同但按项目主流降级不深挖。

→ 引出 S-29: SQL 初始化 — 仓库之后: SqlDataSourceScriptDatabaseInitializer 的 schema.sql/data.sql 执行编排(前置 S-10)。
