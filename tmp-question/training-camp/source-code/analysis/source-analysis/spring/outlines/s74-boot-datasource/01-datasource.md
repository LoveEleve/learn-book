# S-10 DataSource 自动装配 — Embedded/Pooled 双分支

> 依赖 C-11 (复用) | 🟡 Working | 6 KP | [模式: 条件双分支 + 用户优先]

**读者处境**: 没配 spring.datasource.url 也有 DataSource?Hikari 怎么被选中的?自定义数据源怎么覆盖?

### 1. 装配条件与双分支

场景: DataSourceAutoConfiguration 是 imports 候选之一 — 什么条件激活?激活后走嵌入式还是池化?

源码路径:
- `DataSourceAutoConfiguration.java:59,64` — **声明**: @AutoConfiguration(before=SqlInitialization)(L59) + @ConditionalOnClass({DataSource, EmbeddedDatabaseType})(L60 — 数据源类在 classpath) + @ConditionalOnMissingBean(r2dbc ConnectionFactory)(L61 — 响应式数据源不用)
- `DataSourceAutoConfiguration.java:67,69` — **嵌入式分支**: @Conditional(EmbeddedDatabaseCondition)(L67) + @ConditionalOnMissingBean(DataSource/XADataSource)(L68) → @Import(EmbeddedDataSourceConfiguration)(L69)
- `DataSourceAutoConfiguration.java:76,77` — **池化分支**: @ConditionalOnMissingBean(DataSource)(L76) → @Import(DataSourceConfiguration.Hikari/Tomcat/Dbcp2)(L77)

关键设计: **Why 双分支？** "零配置体验": 没配 url + 有嵌入式驱动(H2/HSQL/Derby)→嵌入式库直接跑; 否则(配了 url 或外部库)→池化。两个分支都受 @ConditionalOnMissingBean 保护(用户自定义 DataSource 则全部跳过)。[模式: 条件双分支]

数据流: S-2 读 imports → DataSourceAutoConfiguration: @ConditionalOnClass(有 javax.sql.DataSource)→true → EmbeddedDatabaseCondition 评估: 无 spring.datasource.url + classpath 有 H2 驱动 → 嵌入式分支(§2); 有 url(MySQL) → 池化分支(§3)。

### 2. 嵌入式数据库分支

场景: 测试/开发没配数据源 — 自动用嵌入式 H2/HSQL/Derby。

源码路径:
- `DataSourceAutoConfiguration.java:133,142` — **判定**: getMatchOutcome L142: ①spring.datasource.url 未配置(L135, DATASOURCE_URL_PROPERTY) ②EmbeddedDatabaseConnection.get(classLoader).getType() 命中嵌入式驱动(L154 — H2/HSQL/DERBY) → 匹配
- `EmbeddedDataSourceConfiguration` — **创建**: EmbeddedDatabaseBuilder 建嵌入式 DataSource(嵌入式库 + 自动生成连接)

关键设计: **Why 双重条件？** 判定"该用嵌入式"需要: 没显式指定外部库(url) + classpath 有嵌入式驱动 — 两个都满足才用嵌入式, 避免误配; 这是 Boot"约定优于配置"在数据源的体现。[模式: 组合条件]

数据流: 测试依赖含 H2 → 无 spring.datasource.url → EmbeddedDatabaseCondition: url 缺失 ✓ + EmbeddedDatabaseConnection.get 命中 H2 → 匹配 → EmbeddedDataSourceConfiguration → EmbeddedDatabaseBuilder → H2 DataSource(内存库)。

### 3. 池化分支 — Hikari 默认

场景: 配了 MySQL url — 走池化。Hikari 为什么是默认?

源码路径:
- `DataSourceAutoConfiguration.java:77` — **池选择**: @Import(Hikari/Tomcat/Dbcp2 三个配置类) — 各自带 @ConditionalOnClass(对应池类)
- `DataSourceConfiguration.java:110,112` — **Hikari**: @ConditionalOnClass(HikariDataSource)(L110) + @ConditionalOnProperty(name="spring.datasource.type", havingValue="com.zaxxer.hikari.HikariDataSource", **matchIfMissing**)(L112) — **matchIfMissing=true → 未指定 type 时 Hikari 胜出(默认池)**
- `DataSourceConfiguration.java:52` — **createDataSource L52**: JdbcConnectionDetails(连接细节: url/用户/密码)→ 设置到 HikariDataSource — C-11 池化机制(借用/归还)复用
- 覆盖: 用户 @Bean DataSource → @ConditionalOnMissingBean 跳过; spring.datasource.type 指定其它池

关键设计: **Why Hikari 默认？** Boot 3 默认引入 HikariCP 依赖 + matchIfMissing 让"不指定即 Hikari"; 换池=换依赖+spring.datasource.type。**Why 与 C-11 边界？** 池化机制(ConcurrentBag/借用)在 C-11 与阶段3 HikariCP, 本域只讲"池怎么被选中/创建"。[模式: 条件装配 + 默认]

数据流: 有 spring.datasource.url=jdbc:mysql://... → 池化分支 → 三池条件: 有 HikariDataSource 类 + spring.datasource.type 未指定(matchIfMissing)→ Hikari 配置激活 → createDataSource(JdbcConnectionDetails) → HikariDataSource(url/user/pass) → 注入 JdbcTemplate(事务自动配置与阶段3 使用)。用户 @Bean DataSource(自定义) → @ConditionalOnMissingBean 不匹配 → 用用户的。

→ 引出 S-11: Redis 自动装配 — 数据访问延续: RedisAutoConfiguration 条件装配与 RedisTemplate — 连接与协议深入在阶段3。
