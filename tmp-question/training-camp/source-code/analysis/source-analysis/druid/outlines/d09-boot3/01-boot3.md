# D-9 Boot3 Starter — DruidDataSourceAutoConfigure 装配链

> 前置: [[D-1-core-architecture]] (init) | 复用: [[S-2-boot-autoconfiguration]] (条件装配) | 引出: [[S-10-boot-datasource]] (池化闭环)
> 🟡 Working | 7 KP | [模式: 条件装配 + 包装器 + 自动注入]
> Pass 2 闭环: q1(条件注解组) q2(双前缀回退) q3(Filter 汇入) q4(条件注册) q5(AOP 装配)
**读者处境**: 加个 starter 依赖 + 一行 `spring.datasource.druid.*` 配置, Druid 就接管了 DataSource — 这个"魔法"怎么发生的?`stat-view-servlet.enabled: true` 怎么把监控页装进来?`spring.datasource.druid.url` 和 `spring.datasource.url` 都能用又是为什么?这篇拆开装配链。

### 1. 装配入口 — 条件注解组

场景: starter 怎么抢在 Boot 默认 DataSource 之前生效, 又不和用户冲突?

源码路径:
- `DruidDataSourceAutoConfigure.java:44,46,47` — **条件组**: `@ConditionalOnProperty("spring.datasource.type" = DruidDataSource, matchIfMissing = true)`(L44, 用户没指定也生效)+`@ConditionalOnClass(DruidDataSource)`(L46)+`@AutoConfigureBefore(DataSourceAutoConfiguration)`(L47, 抢在 Boot 默认池之前)
- `DruidDataSourceAutoConfigure.java:48,50` — **配置与导入**: `@EnableConfigurationProperties({DruidStatProperties, DataSourceProperties})`(L48)+`@Import({Aop, StatViewServlet, WebStatFilter, FilterConfiguration})`(L50-53)
- `DruidDataSourceAutoConfigure.java:63,64` — **主 Bean**: `@Bean dataSource()`(L63): `@ConditionalOnMissingBean({DruidDataSourceWrapper, DruidDataSource, DataSource})`(L64-66)→`new DruidDataSourceWrapper()`(L68)

关键设计: **Why matchIfMissing？**(闭环 q1): 用户可能不写 `spring.datasource.type` — 默认 Druid 接管; 显式指定别的池则条件不成立自动让位, **默认接管且不冲突**。**Why AutoConfigureBefore？** Boot 的 DataSourceAutoConfiguration 会装配 Hikari — Druid 必须先注册 Wrapper 抢占 DataSource bean 名额。[模式: 条件装配]

数据流: Boot refresh(S-2) → 条件注解逐条判定(44/46/47) → 注册 DruidStatProperties → 执行 @Import 配置 → dataSource() bean(63) → new DruidDataSourceWrapper。

### 2. Wrapper 与属性绑定 — 双前缀回退

场景: 两个前缀都能配, 怎么选?Filter bean 怎么进链?

源码路径:
- `DruidDataSourceWrapper.java:30,31` — **包装**: `@ConfigurationProperties("spring.datasource.druid")`(L30)+`extends DruidDataSource implements InitializingBean`(L31) — 继承池本体兼做配置绑定
- `DruidDataSourceWrapper.java:36,38,44` — **回退**: `afterPropertiesSet()`(L36): username 为 null → `determineUsername()`(L38-39)/password(L41-42)/url(L44-45)/driverClassName(L47-48) — 全部回退 spring.datasource.*
- `DruidDataSourceWrapper.java:49,52,55` — **启动与汇入**: `init()`(L49, D-1 链路)+`autoAddFilters(@Autowired List<Filter>)`(L52): `super.filters.addAll(filters)`(L55)

关键设计: **Why 继承 DruidDataSource？** 配置绑定+生命周期落同一对象, 免代理转发。**Why 双前缀回退？**(闭环 q2): 老项目只有 `spring.datasource.url`, 新项目用 druid 前缀 — 前者未设置时自动回退, 零改造迁移。**Why autoAddFilters？**(闭环 q3): DruidFilterConfiguration 注册的 Filter bean 经 @Autowired 自动汇入链(D-2) — 配置即插即用。[模式: 包装器 + 前缀兼容]

数据流: afterPropertiesSet(36) → 属性 null?determineXxx 回退(38-47) → init()(49, D-1) → autoAddFilters(52): @Autowired 收集 Filter bean → filters.addAll(55) → 链就绪(D-2)。

### 3. 组件注册 — Filter / 监控页 / Web 统计 / AOP

场景: 监控页和 Filter 什么时候装进来?按什么条件?

源码路径:
- `DruidFilterConfiguration.java` — **Filter 注册**: StatFilter/WallFilter/ConfigFilter/Logging×4/Encoding 各 @Bean: `@ConditionalOnProperty(prefix="spring.datasource.druid.filter.{stat,wall,...}", name="enabled")`+`@ConfigurationProperties`+`@ConditionalOnMissingBean`
- `DruidStatViewServletConfiguration.java:29,33` — **监控页**: `@ConditionalOnProperty("stat-view-servlet.enabled", havingValue="true")`(L29)→`new StatViewServlet()`(L33)
- `DruidWebStatFilterConfiguration.java:30,32,38` — **Web 统计**: `@ConditionalOnWebApplication`(L30)+`web-stat-filter.enabled`(L32)→FilterRegistrationBean: urlPattern 默认 `/*`(L38)+exclusions 默认静态(L39)
- `DruidSpringAopConfiguration.java:30,33,38` — **AOP 切面**: `@ConditionalOnProperty("spring.datasource.druid.aop-patterns")`(L30) → **`RegexpMethodPointcutAdvisor(aop-patterns 正则, advice())`**(L38, advice=DruidStatInterceptor L33); `spring.aop.auto=false` 时手动 `DefaultAdvisorAutoProxyCreator` 兜底(闭环 q5)
- `DruidStatProperties.java:24,26` — **配置载体**: statViewServlet(L26)+webStatFilter(L27) 内嵌配置

关键设计: **Why 全部条件注册？**(闭环 q4): 监控页/Web 统计/切面都是可选组件 — enabled=true 才装配, 避免无关 Servlet/Filter 污染应用; 每种组件按自身依赖选条件(Property+Class+WebApplication)。[模式: 按需装配]

数据流: `filter.stat.enabled: true` → DruidFilterConfiguration.statFilter bean → autoAddFilters 汇入链(D-2) → `stat-view-servlet.enabled: true` → StatViewServlet 注册(33) → 访问 /druid/index.html → 监控页(消费 D-3 统计)。

→ 引出: 阶段3 收尾 — D-1~D-9 与 S-10 Boot DataSource 呼应 (池化深入闭环), 全部域完成后更新 HANDOFF。
