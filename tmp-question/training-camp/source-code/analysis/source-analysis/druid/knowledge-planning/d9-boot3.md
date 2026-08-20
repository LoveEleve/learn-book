# D-9 Boot3 Starter — DruidDataSourceAutoConfigure 装配链

> 项目: Druid (JDBC 连接池) | 🟡 Working / 1 篇 | druid-spring-boot-3-starter(9 文件): DruidDataSourceAutoConfigure+DruidDataSourceWrapper+DruidFilterConfiguration+DruidStatViewServletConfiguration+DruidWebStatFilterConfiguration+DruidSpringAopConfiguration+DruidStatProperties
> 基线: DRUID-PLAN D-9 (装配) — 前置: **D-1(init 已分析) + S-2 自动装配管线(已分析, 复用)** — 展开装配链; 装配条件注解是 Boot 侧机制, 对照 S-2

---

## §0.8

- 🟡 Working，1篇 — 装配入口(**@Configuration+@ConditionalOnProperty("spring.datasource.type"=DruidDataSource, matchIfMissing=true)+@ConditionalOnClass(DruidDataSource)+@AutoConfigureBefore(DataSourceAutoConfiguration)+@EnableConfigurationProperties({DruidStatProperties,DataSourceProperties})+@Import(4 个配置类)[DruidDataSourceAutoConfigure L44-53]** → **@Bean dataSource(): @ConditionalOnMissingBean({Wrapper,DataSource})→new DruidDataSourceWrapper[L63-69]**) → Wrapper 与绑定(**DruidDataSourceWrapper extends DruidDataSource implements InitializingBean[L31]: @ConfigurationProperties("spring.datasource.druid")[L30]+afterPropertiesSet[L36]: druid 前缀缺省时回退 spring.datasource.*(determineUsername/determineUrl[L38-47])→init()[L49]; autoAddFilters(@Autowired List<Filter>→filters.addAll[L52-56] 汇入链)**) → 组件注册(**DruidFilterConfiguration: 8 个 Filter(StatFilter/WallFilter/ConfigFilter/Logging×4/Encoding) 每 bean @ConditionalOnProperty(prefix=..., name="enabled")+@ConfigurationProperties+@ConditionalOnMissingBean; DruidStatViewServletConfiguration: @ConditionalOnProperty("stat-view-servlet.enabled")→StatViewServlet+ServletRegistrationBean[L29-37]; DruidWebStatFilterConfiguration: @ConditionalOnWebApplication+web-stat-filter.enabled→FilterRegistrationBean(urlPattern 默认 /*+exclusions 默认静态资源[L32-47]); DruidSpringAopConfiguration: aop-patterns 开启切面统计; DruidStatProperties: statViewServlet/webStatFilter 配置映射[L24-27]**)
- 设计模式: [模式: 条件装配]—Boot 条件注解族; [模式: 包装器]—Wrapper 桥接配置; [模式: 自动注入]—autoAddFilters 汇 Filter

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| DruidDataSourceAutoConfigure.java:44,46,47,48,50 | 条件组 | **@ConditionalOnProperty(type=DruidDataSource, matchIfMissing)(L44)+@ConditionalOnClass(DruidDataSource)(L46)+@AutoConfigureBefore(DataSourceAutoConfiguration)(L47)+@EnableConfigurationProperties(L48)+@Import(4 配置类)(L50-53)** | High |
| DruidDataSourceAutoConfigure.java:63,65,69 | 主 Bean | **@Bean dataSource()(L63): @ConditionalOnMissingBean({DruidDataSourceWrapper,DruidDataSource,DataSource})(L64-66)→new DruidDataSourceWrapper(L68-69)** | High |
| DruidDataSourceWrapper.java:30,31,36 | Wrapper | **@ConfigurationProperties("spring.datasource.druid")(L30)+extends DruidDataSource implements InitializingBean(L31)** | High |
| DruidDataSourceWrapper.java:38,47,49 | 前缀回退 | **afterPropertiesSet(L36): username/password/url/driverClassName 为 null → basicProperties.determineXxx(L38-47)→init()(L49)** | High |
| DruidDataSourceWrapper.java:52,55 | Filter 汇入 | **autoAddFilters(@Autowired List<Filter>)(L52): filters.addAll(filters)(L55)** — Filter bean 自动进链 | High |
| DruidFilterConfiguration.java | Filter 注册 | **每 Filter: @ConfigurationProperties(prefix)+@ConditionalOnProperty(enabled)+@ConditionalOnMissingBean → new StatFilter()/WallFilter()/ConfigFilter()/Log4j2Filter()/...** | High |
| DruidStatViewServletConfiguration.java:29,33,37 | 监控页 | **@ConditionalOnProperty("spring.datasource.druid.stat-view-servlet.enabled")(L29)→@Bean 注册 StatViewServlet(L33-37)** | High |
| DruidWebStatFilterConfiguration.java:30,32,38 | Web 统计 | **@ConditionalOnWebApplication(L30)+web-stat-filter.enabled(L32)→FilterRegistrationBean: WebStatFilter+urlPattern 默认 /*(L38)+exclusions 默认静态(L39)** | High |
| DruidStatProperties.java:24,26 | 配置类 | **DruidStatProperties(L24): statViewServlet(L26)+webStatFilter(L27) 内嵌配置** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 装配是薄门面 — 1篇 (~42行) 按"装配入口→Wrapper 绑定→组件注册"展开; 条件注解机制复用 S-2(已分析), init/Filter 链引用 D-1/D-2(已分析)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 条件注解组 (Property/Class/Before/Enable) | 🟡 | **为什么🟡**: 装配时机 |
| P1-2 | DruidDataSourceWrapper 前缀回退 | 🟡 | **为什么🟡**: 双前缀兼容 |
| P1-3 | autoAddFilters 汇入 Filter bean | 🟡 | **为什么🟡**: Filter 装配链 |
| P1-4 | 监控组件条件注册 (Servlet/WebFilter/AOP) | 🟡 | **为什么🟡**: 按需开启 |
| P2-1 | DruidFilterConfiguration 8 Filter 注册 | 🟢 | **为什么🟢**: 配置化 Filter |
| P2-2 | 与 S-2 自动装配管线衔接 | 🟢 | **为什么🟢**: 框架复用 |
| P3-1 | Boot2 starter 差异 (淘汰说明) | 🟢 | **为什么🟢**: 版本差异 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **装配入口** | 🟡 | 时机与条件 |
| B | **Wrapper 绑定** | 🟡 | 配置桥 |
| C | **组件注册** | 🟡 | 按需扩展 |

> **Cluster A (§1)**: 条件注解组+主 Bean
> **Cluster B (§2)**: Wrapper 双前缀回退+autoAddFilters
> **Cluster C (§3)**: Filter 注册+StatViewServlet+WebStatFilter+AOP 切面

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 条件注解组 | Property(type 匹配+matchIfMissing)+Class+AutoConfigureBefore 组合: 默认接管且不冲突, 用户指定其他池自动让位 | DruidDataSourceAutoConfigure.java:44-53 |
| q2 | Wrapper 双前缀 | afterPropertiesSet: druid 前缀缺省时回退 spring.datasource.*(determineXxx) — 老项目零改造迁移 | DruidDataSourceWrapper.java:36-50 |
| q3 | Filter 汇入 | autoAddFilters(@Autowired List<Filter>) → filters.addAll — DruidFilterConfiguration 的 8 个 Filter bean 自动进链 | DruidDataSourceWrapper.java:52-56, DruidFilterConfiguration.java |
| q4 | 组件条件注册 | 监控页/Web 统计/AOP 全按 enabled 条件装配 — 可选组件不污染应用 | DruidStatViewServletConfiguration.java:29-37, DruidWebStatFilterConfiguration.java:30-47 |
| q5 | AOP 统计装配 | **RegexpMethodPointcutAdvisor(aop-patterns 正则→方法切面) + DruidStatInterceptor**(aopalliance Advice); spring.aop.auto=false 时手动 DefaultAdvisorAutoProxyCreator 兜底 | DruidSpringAopConfiguration.java:30-47 |

→ 引出: 阶段3 收尾 — D-1~D-9 与 S-10 Boot DataSource 呼应 (池化深入闭环), 全部域完成后更新 HANDOFF
