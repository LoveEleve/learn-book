# S-29 SQL 初始化 — SqlInitializationAutoConfiguration → SqlDataSourceScriptDatabaseInitializer

> 依赖 S-10 DataSource (复用) | 🟡 Working | 6 KP | [模式: 条件装配 + 模板方法 + 生命周期钩子]

**读者处境**: classpath 放 schema.sql/data.sql 应用启动就自动建表/灌数据 — 谁执行?mode=EMBEDDED 是什么意思?怎么指定脚本位置?

### 1. 装配与条件 — SqlInitializationAutoConfiguration

场景: 有 DataSource + classpath 有 schema.sql — 自动装配怎么决定"要初始化"?

源码路径:
- `SqlInitializationAutoConfiguration.java:40` — **开关**: `@ConditionalOnBooleanProperty(name = "spring.sql.init.enabled", matchIfMissing = true)`(L40) — 默认开启
- `SqlInitializationAutoConfiguration.java:50` — **mode=never 排除**: `@ConditionalOnProperty(name = "spring.sql.init.mode", havingValue = "never")`(L50) — 配 never 则不初始化
- `DataSourceInitializationConfiguration.java:37,38,40` — **Bean**: `@Bean SqlDataSourceScriptDatabaseInitializer(dataSource, properties)`(L37-40) — 为主数据源建初始化器

关键设计: **Why enabled + mode 双条件？** enabled 是总开关(默认开); mode=never 是精细化排除(用户想关脚本初始化但保留其他 SQL init 时用) — 双层控制。[模式: 条件装配]

数据流: 有 DataSource + spring.sql.init.enabled=true(默认) + mode≠never → SqlInitializationAutoConfiguration 命中 → @Bean SqlDataSourceScriptDatabaseInitializer(dataSource, properties)(L37-40) → 容器里注册初始化器。

### 2. 执行器 — SqlDataSourceScriptDatabaseInitializer

场景: 初始化器怎么执行 schema.sql 和 data.sql?顺序?

源码路径:
- `SqlDataSourceScriptDatabaseInitializer.java:34` — **继承**: `extends DataSourceScriptDatabaseInitializer`(L34) — 面向主 SQL 数据库的初始化器
- `DataSourceScriptDatabaseInitializer`(boot/jdbc/init) — **内核**: 执行 schema 脚本(建表)→ 执行 data 脚本(灌数据); 通过 `DataSourceScriptDatabaseInitializer` 的模板方法运行 SQL 脚本
- 顺序: 先 schema 后 data — schema 建表, data 才有表可插

关键设计: **Why 先 schema 后 data？** data 脚本插数据依赖表存在 — 必须先执行 schema(建表)再 data(灌数); 这是脚本初始化的固定顺序决策。**Why 继承模板？** 内核(脚本读取/执行/事务/错误处理)在 DataSourceScriptDatabaseInitializer, Boot 只提供"主数据源"的具体实现。[模式: 模板方法 + 顺序决策]

数据流: refresh 期间(Bean 初始化, afterPropertiesSet) → SqlDataSourceScriptDatabaseInitializer(L34) 触发 → 读 SqlInitializationProperties 的 schemaLocations 脚本 → 执行 schema.sql(建表) → 读 dataLocations → 执行 data.sql(灌数据)。

### 3. 模式与配置 — DatabaseInitializationMode + SqlInitializationProperties

场景: EMBEDDED/ALWAYS/NEVER 什么意思?怎么改脚本位置?

源码路径:
- `OnDatabaseInitializationCondition.java:57,67` — **模式判定**: `getMatchOutcome`(L57) → `match: !mode.equals(NEVER)`(L67) — 非 NEVER 才执行
- `SqlInitializationProperties.java:38,43,81` — **配置**: `schemaLocations`(L38)/`dataLocations`(L43) — 脚本位置; `mode = DatabaseInitializationMode.EMBEDDED`(L81, 默认)
- `DatabaseInitializationMode`(boot/sql/init): `ALWAYS`(L31)/`EMBEDDED`(L36)/`NEVER`(L41) — EMBEDDED 仅内嵌库执行, ALWAYS 对所有库, NEVER 从不

关键设计: **Why EMBEDDED 默认？** 默认只在内嵌数据库(如 H2)执行脚本 — 避免误在生产库上跑 schema.sql; 显式配 ALWAYS 才在所有库执行; NEVER 关闭。这是"默认安全"的决策。[模式: 默认安全 + 三态模式]

数据流: 配 spring.sql.init.mode=ALWAYS + schema-locations=classpath:db/schema.sql → OnDatabaseInitializationCondition 判定非 NEVER(L67) → 执行器读取指定位置脚本 → 建表/灌数。默认 EMBEDDED → 仅内嵌库执行。

→ 引出 BOOT 深探新增收束: S-25~S-29 完成 — 至此 BOOT-PLAN-v2 29 域 (S-1~S-29) 全部规划。
