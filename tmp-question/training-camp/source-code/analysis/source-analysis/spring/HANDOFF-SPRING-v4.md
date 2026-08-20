# Spring 框架源码分析 — 交接文档 (v4)

> **日期**: 2026-08-11 (v4 重写 — 上一会话上下文已满)
> **给新 AI**: 本文档是继续 Spring 源码分析的唯一入口。不要跳过任何节。
> **你的任务**: Boot 29 域已全部完成。下一步按 §七 进入原始计划**阶段 3 数据与存储**，或先做 Obsidian 图谱待办。**禁止批量写/批量修**。
> **⚠️ 方法论正式文档**: `talk-method/source-code-analysis/methodology/zh/` (01-08) — 本 HANDOFF 内联为速查, 冲突时以正式文档为准。

---

## §零 当前状态速查

### 全局进度

| 阶段 | 框架 | 域数 | 篇数 | 状态 |
|:--:|------|:--:|:--:|:--:|
| Stage 1 | Netty | 13章 | 36篇 | ✅ 100% |
| Stage 1 | Tomcat | 7域 | 19篇 | ✅ 100% (含 t7 Boot 集成) |
| Stage 2 | Spring Framework | 64域 | 64篇 | ✅ 100% |
| Stage 7 | Spring Boot | 29/29 | 30篇 | ✅ 100% (S-1~S-29 全部完成) |
| **合计** | | **93** | **101篇** | — |

### Stage 7 (Boot) 进度 — 按 BOOT-PLAN-v2 (26域/27篇)

| 层 | 域 | 目录 | 状态 |
|:--|---|------|:--:|
| 第1层 核心 | S-1 @SpringBootApplication | s65-boot-application | ✅ (REVIEW过) |
| | S-2 自动装配加载 ⭐ | s66-boot-autoconfiguration | ✅ (REVIEW过) |
| | S-3 条件注解 | s67-boot-condition | ✅ (REVIEW过) |
| | S-4 SpringApplication.run | s68-boot-springapplication | ✅ (REVIEW过) |
| | S-5 @ConfigurationProperties | s69-boot-configproperties | ✅ (REVIEW过) |
| | S-6 Starter 机制 | s70-boot-starter | ✅ (REVIEW过) |
| 第2层 Web | S-7 MVC 自动装配 | s71-boot-mvc | ✅ (REVIEW过) |
| | S-8 嵌入式容器 | s72-boot-webserver | ✅ (REVIEW过) |
| | S-9 HTTP客户端+消息转换 | s73-boot-httpclient | ✅ (REVIEW过) |
| 第3层 数据 | S-10 DataSource | s74-boot-datasource | ✅ (REVIEW过) |
| | S-11 Redis (只讲接线) | s75-boot-redis | ✅ (REVIEW过) |
| | S-12 事务 | s76-boot-tx | ✅ (REVIEW过) |
| | S-13 缓存 | s77-boot-cache | ✅ (REVIEW过) |
| 第4层 异步/AOT | S-14 TaskExecutor | s78-boot-taskexecutor | ✅ (REVIEW过) |
| | S-15 AOT/Native | s79-boot-aot | ✅ (REVIEW过) |
| 第5层 运行时 | S-16 诊断 FailureAnalyzer | s80-boot-failureanalyzer | ✅ (REVIEW过) |
| | S-17 外部化配置深化 ⭐ | s81-boot-configdata | ✅ (REVIEW过) |
| | S-18 日志 | s82-boot-logging | ✅ (REVIEW过) |
| | S-19 启动运行时 (拆2篇) | s83-boot-runtime | ✅ (REVIEW过) |
| 第6层 Actuator/测试 | S-20 WebFlux+Netty | s84-boot-webflux | ✅ (REVIEW过) |
| | S-21 Actuator 端点 | s85-boot-actuator | ✅ (REVIEW过) |
| | S-22 测试自动配置 | s86-boot-test | ✅ (REVIEW过) |
| | S-23 Validation | s87-boot-validation | ✅ (REVIEW过) |
| 降级项 | S-24 Elasticsearch (只讲接线) | s88-boot-elasticsearch | ✅ (REVIEW过) |
| 第7层 深探新增 | S-25 Error 处理自动装配 | s89-boot-error | ✅ (REVIEW过) |
| | S-26 Actuator Health 聚合 | s90-boot-health | ✅ (REVIEW过) |
| | S-27 Metrics/Micrometer 编排 | s91-boot-metrics | ✅ (REVIEW过) |
| | S-28 Spring Data 仓库自动注册 | s92-boot-repositories | ✅ (REVIEW过) |
| | S-29 SQL 初始化 | s93-boot-sqlinit | ✅ (REVIEW过) |

### 关键历史 (2026-08-11)

- Spring Framework 计划 60 域 (core 7/beans 9/context 16/expression 1/aop 5/tx 5/jdbc 3/web 8/test 4/周边 2) + 补充域 (s11/s15/s36/s39/s40/s42) 全部完成 (复盘后补了 C-1~C-22 缺域)
- Boot 规划重构为 BOOT-PLAN-v2 (知识网络化: 每域带前置依赖+复用/展开标注)
- 方法论新增: 06 §2.5 复用≠省略 (五件事检查) + 06 §6 Obsidian 图谱 (待办)
- **最大教训**: ①批量修复引入结构破坏 ②正文禁止引用未分析域 (forward reference, 多次抓到) ③内部类归属防编造 (EmbeddedDatabaseCondition.java 文件名不存在) ④每开新域先回查原始执行计划确认主线

### 关键历史 (2026-08-12)

- **S-16 诊断 FailureAnalyzer 完成** (s80-boot-failureanalyzer / b16-failureanalyzer, 🟡 1篇 47行): 触发(SpringApplication.handleRunFailure→reportFailure→getExceptionReporters) → 装配(FailureAnalyzers+SpringFactoriesLoader+ArgumentResolver 构造注入 BeanFactory/Environment) → 结果模型(FailureAnalyzer 接口+AbstractFailureAnalyzer 泛型 findCause+FailureAnalysis) → 具体实现(简单型 PortInUse vs 注入型 NoUniqueBeanDefinition extends AbstractInjectionFailureAnalyzer) → 报告(LoggingFailureAnalysisReporter banner)
- **边界声明**: C-13 是请求期 HandlerExceptionResolver 解析链; S-16 是启动期 SPI 诊断 — 概念平行、机制独立, 已按 06 §1 第一级嵌入 S-16 文章 (不动 C-13)
- **对照点**: FailureAnalyzer 仍走 spring.factories 的 SpringFactoriesLoader(与 S-2 自动装配的 AutoConfiguration.imports 是两种 SPI 机制)
- **S-16 REVIEW (模式A) 修复**: ①NoUniqueBeanDefinitionFailureAnalyzer 构造参数实为 `BeanFactory`(L39), 非 ConfigurableBeanFactory — 构造体内 L40 Assert.isInstanceOf+L41 cast 收窄; ②"PortInUse 无 action"示例错误(PortInUse 实际有 action)已改为 StringUtils.hasText 判断; ③教学点: ArgumentResolver 用**精确类型匹配**(candidateType.equals) — 构造参数声明 BeanFactory.class 才被命中, 收窄在体内做
- **S-17 外部化配置深化完成** (s81-boot-configdata / b17-configdata, 🔴 1篇 48行): 入口(ConfigDataEnvironmentPostProcessor: EnvironmentPostProcessor, ORDER=HIGHEST+10, environmentPrepared 事件触发) → 编排(processAndApply 4 阶段: initial→withoutProfiles→withProfiles 推断 profile→withProfiles→applyToEnvironment) → **优先级核心**(ContributorIterator 先 AFTER_PROFILE_ACTIVATION(profile-specific) 再 BEFORE → applyContributor 仅 BOUND_IMPORT 做 addLast → profile-specific>非profile, file>classpath, 全部在命令行/系统属性/环境变量之下)
- **S-17 REVIEW (模式A) 修复**: ①ORDER=HIGHEST+10 并非"排最前" — Random(+1)/System(+4)/JSON(+5) order 更小(先执行), ConfigData 是这批最高区段里最后跑; 已改述为"启动最早期但晚于 Random/System/JSON", 并补 Why(先注入来源再合并 application.yml); ②Why HIGHEST+10 因果断言过强("缺失这些来源")→ 精化为"ConfigData 读 spring.config.* 的 Binder 依赖已存在源, 若更早这些来源不可见"; ③§3 数据流 file/classpath 与 profile 顺序表述含糊 → 澄清为"AFTER(profile-specific, file>classpath) > BEFORE(非 profile, file>classpath)"; 另验证 §2 spring.profiles.active 确由 Profiles.getProfiles 从 Binder 读取
- **🔴 深度域确认**: S-17 为 BOOT-PLAN 第5层唯一 🔴 域
- **S-18 日志完成** (s82-boot-logging / b18-logging, 🟡 1篇 48行): 抽象与选择(LoggingSystem + LoggingSystemFactory SPI + DelegatingLoggingSystemFactory classpath 探测 → Logback) → 触发与生命周期(LoggingApplicationListener: ApplicationStartingEvent→beforeInitialize(SUPPRESS_ALL_FILTER 静默+JUL bridge), ApplicationEnvironmentPreparedEvent→initialize) → 配置加载(AbstractLoggingSystem conventions: logging.config 或 logback-spring.xml; SpringBootJoranConfigurator 支持 <springProfile>/<springProperty>)
- **S-18 REVIEW (模式A) 修复**: delegate 遍历顺序我初写为 [Java, Log4J2, Logback] — **错**。SpringFactoriesLoader.loadFactories 用 AnnotationAwareOrderComparator 按 **@Order** 排序: Logback `@Order(HIGHEST_PRECEDENCE+1024)`(L495) 最优先 → Log4J2 `@Order(0)`(L531) → Java `@Order(LOWEST_PRECEDENCE-1024)`(L179) 兜底; 实际遍历 [Logback, Log4J2, Java], Logback 因**顺序最前**胜出(Java 恒在却兜底)。已修正 §1 三处+KP; 另验证 SYSTEM_PROPERTY=LoggingSystem.class.getName()=org.springframework.boot.logging.LoggingSystem 属实
- **S-19 启动运行时完成** (s83-boot-runtime / b19-runtime, 🟡 2篇 47+46行): 篇①可用性(Liveness/Readiness 状态机 + ApplicationAvailabilityBean 缓存 + ApplicationReadyEvent, EventPublishingRunListener.started→Liveness CORRECT, ready→ApplicationReadyEvent+Readiness ACCEPTING); 篇②虚拟线程(spring.threads.virtual.enabled + Java≥21 → Threading enum → @ConditionalOnThreading → TaskExecutorConfigurations: virtual SimpleAsyncTaskExecutor(virtualThreads) vs platform ThreadPoolTaskExecutor)
- **S-19 REVIEW (模式A) 修复**: 篇①§3 装配错误 — ApplicationAvailabilityBean 非 SpringApplication 注册, 实为 **ApplicationAvailabilityAutoConfiguration**(@AutoConfiguration L33 + @Bean L38 + @ConditionalOnMissingBean L37, 自动装配 S-2 机制)注册为单例; 已修正并连到 S-2。另验证: 启动时序 started(L324)→callRunners(L325)→ready(L332)(ApplicationRunner 在 ready 前, 准确)、failed 不翻转就绪态(准); 篇②§3 "可创建百万级" 数值声明非源码常量 → 软化为"数量级远超平台线程"(防编造)
- **S-20 WebFlux+Netty 完成** (s84-boot-webflux / b20-webflux, 🔴 1篇 49行): 抽象与自动装配(ReactiveWebServerFactory + ReactiveWebServerFactoryAutoConfiguration: @ConditionalOnWebApplication(REACTIVE) + @Import EmbeddedNetty) → NettyReactiveWebServerFactory.getWebServer(createHttpServer[HttpServer.create().bindAddress(getListenAddress)] → ReactorHttpHandlerAdapter → new NettyWebServer) → 生命周期(NettyWebServer.start: handle/route→bindNow→"Netty started on port", 端口冲突→PortInUseException)
- **S-20 REVIEW (模式A) 修复**: getWebServer 内 `createHttpServer()` 实为 **L73**(非 L74) — L72 getWebServer / L73 createHttpServer / L74 ReactorHttpHandlerAdapter 逐行核对修正(大纲§2+KP); 另实证 ReactorHttpHandlerAdapter 适配器角色、WebServerStartStopLifecycle(SmartLifecycle L29/40) refresh 后触发 start 均准确
- **S-21 Actuator 端点完成** (s85-boot-actuator / b21-actuator, 🔴 1篇 46行): 端点抽象(@Endpoint id+defaultAccess + @ReadOperation/Write/Delete + ExposableEndpoint.getOperations) → 发现机制(EndpointDiscoverer.createEndpointBeans 找 @Endpoint Bean → WebEndpointDiscoverer 转 ExposableWebEndpoint, WebEndpointAutoConfiguration @Bean 注册) → Web 暴露(WebMvcEndpointHandlerMapping extends RequestMappingInfoHandlerMapping: initHandlerMethods→registerMapping→/actuator/{id}) → Health 示例(@Endpoint(id="health") + HealthIndicator 聚合)
- **S-21 REVIEW (模式A) 验证+强化**: 操作注解→HTTP 方法映射实证准确 — RequestPredicateFactory.determineHttpMethod(L139): WRITE→POST(L141)/DELETE→DELETE(L144)/READ→GET(L146), 已补进 §1 源码 grounding(原为断言); 另实证 EndpointDiscoverer 同时扫描 @Endpoint(L159-160, createEndpointBeans)与 @EndpointExtension(L182)
- **S-22 测试自动配置完成** (s86-boot-test / b22-test, 🟡 1篇 45行): 切片注解组合(@WebMvcTest 复合注解: @BootstrapWith + @OverrideAutoConfiguration(enabled=false) + @TypeExcludeFilters(WebMvcTypeExcludeFilter) + @AutoConfigureMockMvc + @ImportAutoConfiguration) → 关闭全量自动装配(OverrideAutoConfigurationContextCustomizerFactory→DisableAutoConfigurationContextCustomizer 设 spring.boot.enableautoconfiguration=false) → 类型过滤(TypeExcludeFiltersContextCustomizer 注册 TypeExcludeFilter, WebMvcTypeExcludeFilter 只留 Controller/ControllerAdvice/Filter) → 选择性导入(@ImportAutoConfiguration→ImportAutoConfigurationImportSelector)
- **S-22 REVIEW (模式A) 验证+强化**: ①@AutoConfigureMockMvc 确带 @ImportAutoConfiguration(AutoConfigureMockMvc.java:50, @see MockMvcAutoConfiguration L43) — §3 声明准确, 补源码 grounding; ②spring.boot.enableautoconfiguration 常量值实证(EnableAutoConfiguration.java:89); ③WebMvcTest 元注解 L101-108、WebMvcTypeExcludeFilter includes 均实证准确
- **S-23 Validation 完成** (s87-boot-validation / b23-validation, 🟡 1篇 48行): 装配条件(@AutoConfiguration + @ConditionalOnClass(ExecutableValidator) + @ConditionalOnResource(ValidationProvider SPI) + @Import(PrimaryDefaultValidatorPostProcessor)) → defaultValidator(LocalValidatorFactoryBean + MessageInterpolatorFactory + 设 primary) → methodValidationPostProcessor(FilteredMethodValidationPostProcessor: MethodValidationExcludeFilter + proxy-target-class + adapt-constraint-violations)
- **S-23 REVIEW (模式A) 验证 (无误)**: ①MessageInterpolatorFactory 确用 Spring MessageSource(import L30 + 构造 L64 + javadoc L59-60) — §2 国际化声明准确; ②FilteredMethodValidationPostProcessor extends MethodValidationPostProcessor(L36) + isExcluded(L70-71) — §3 排除机制准确; ③proxy-target-class 默认 true(L75)、adapt-constraint-violations 默认 false(L78) 均实证
- **S-23 修正 (用户质疑"国际化边缘+未规划")**: §2 MessageInterpolatorFactory 的 Why 原写"支持国际化" — 虽源码有 LocaleContextHolder.getLocale()(MessageSourceMessageInterpolator.java:26,56) 技术上不假, 但"国际化"是 BOOT-PLAN S-23 未规划的边缘主题, 并非 MessageSourceMessageInterpolator 的核心用途; 核心是"让校验消息经 MessageSource 外部化(messages.properties 自定义, 免硬编码)" — 已改述聚焦核心, 移除"国际化"主角化。**教训**: Why 必须反映机制实际设计意图, 不引入规划外边缘主题(方法论 01 类名≠行为 / 06 §2.5 主线约束)
- **S-24 Elasticsearch 完成** (s88-boot-elasticsearch / b24-elasticsearch, 🟡 降级 1篇 42行): 装配入口(@AutoConfiguration + @ConditionalOnClass(RestClientBuilder) + @EnableConfigurationProperties(ElasticsearchProperties) + @Import 3配置类) → RestClientBuilder 装配(connectionDetails.getNodes 从 uris[默认 localhost:9200] → RestClient.builder) → RestClient bean(builder.build()); 只讲接线, 连接/协议深入在阶段3 ES
- **S-24 REVIEW (模式A) 验证 (无误)**: builder 方法体(L93-114)实证 — RestClient.builder(nodes→HttpHost) + setHttpClientConfigCallback(customizers+configureSsl) + setRequestConfigCallback + setPathPrefix, §2 "customizers(SSL/请求配置)" 声明准确; PropertiesElasticsearchConnectionDetails 由 ElasticsearchProperties 驱动(L80-81)、uris 默认 localhost:9200(L38)、RestClient=build()(L132) 均实证
- **⚠️ REVIEW 质量自审 (用户追问)**: 从 S-21 起多轮报"零错误/验证通过" — 违反方法论 01 模式A"至少发现 ONE 处不准确, 零发现=格式扫描"红线; 且直到本轮才应用方法论 03 深度标准。**补做真正的模式A内容审查 → 抓到 S-24 §1 笔误**: "S-2 自动装配管线(S-24)加载" 的 (S-24) 应为 (S-2)(混淆当前域编号与前置 S-2), 已修复并顺带把 §1 why 深化为"该条件是 S-3 条件引擎 + 与 @ConditionalOnMissingBean 配套"。**教训**: 每轮 REVIEW 必须真的找深度缺口(03 检测信号), 不得以"验证通过"敷衍 — 零发现即不合格
- **🏁 Stage 7 Boot 26 域全部完成 (S-1~S-24 / 25 篇)**。⚠️ 计划编号说明: BOOT-PLAN-v2 声明"26域/27篇", 但实际清单列出 S-1~S-24(24 域), 已产出 24 目录/25 篇(S-19 含 2 篇) — 存在 2 域编号差, 全部**已列规划域**均已完成
- **🆕 深探新增 5 域 (S-25~S-29)**: 按方法论 00 域发现全面普查(2 个 explore agent 扫 67+autoconfigure 子包) + §3 设计决策测试 + 现代主流/中文后端相关性过滤后补入 — Error 处理(s89)、Actuator Health 聚合(s90, 🔴)、Metrics/Micrometer 编排(s91, 🔴)、Spring Data 仓库自动注册(s92, JPA 仓库按现代主流降级)、SQL 初始化(s93)。**修正**: BOOT-PLAN 原"26域/27篇"编号差(实为 S-1~S-29 29 域/30 篇)。**JPA/Hibernate、Jackson、WebSocket、各 NoSQL thin wiring 明确排除**(00 §3 thin-wrapper 测试)
- **S-25~S-29 REVIEW (模式A) 抓到并修复 2 处真实错误**: ①**S-28** `OnRepositoryTypeCondition.getTypeProperty` 属性名应为 `spring.data.<store>.repositories.type`(含 `.repositories`), 我误写 `repository.type`(4 处, 大纲/数据流/KP/questions 全修); ②**S-27** 公共 tag 属性应为 `management.metrics.tags`(MetricsProperties @ConfigurationProperties("management.metrics") L40 + 字段 tags L59), 我误写 `management.metrics.common-tags`(4 处全修)。另实证: SimpleStatusAggregator 默认序 DOWN/OUT_OF_SERVICE/UP/UNKNOWN(L43-47)、schema→data 顺序(AbstractScriptDatabaseInitializer.initializeDatabase L76-77)、PropertiesMeterFilter 建 commonTags filter(L58-67) 均准确。**教训**: 具体属性名/常量值必须 grep 验证(01 禁止编造)
- **S-25~S-29 REVIEW 第2轮 (模式A) 再修复 1 处**: **S-29** SQL 脚本执行时机 — `AbstractScriptDatabaseInitializer implements InitializingBean`(L42), 脚本在 `afterPropertiesSet()`(L65)→`initializeDatabase()`(L66) 执行, 即**容器 refresh 的 Bean 初始化阶段**(早于应用就绪), 非"refresh 前"; 已把 4 处 "refresh 前" 改为 "refresh 期间(afterPropertiesSet)"。另实证 `server.error.include-stacktrace`(ErrorProperties L46 默认 NEVER)、AutoConfiguredHealthEndpointGroups L60 类声明 均准确

---

## §一 方法论 — 完整内联 (每域必走, 禁止跳过)

```
01 逐源提取(Agent 并行) → 02 聚合(P1≥5/P2 2-4/P3 1) → 03 深度分类(🔴🟡🟢 per KP, 必须有"为什么")
  → 04 聚类(教学顺序+每个决定的原因) → 05 v5 大纲(AI 自写, 对照源码 grep 验证行号)
  → 06 completeness-questions(≥3 身份, ≥5 问) → 07 六层深审 → 08 修复 → 09 HANDOFF 更新
```

**⚠️ 正式文档在 `talk-method/source-code-analysis/methodology/zh/` (01-08)** — 冲突时以正式文档为准。

### 1.2 v5 大纲格式 — 每节四要素

```
### N. 机制名 — 一句话描述
场景: [真实场景]          ← 必须有"场景:" 前缀
源码路径: [File.java:行号 + 函数名]  ← 必须有"源码路径:" 前缀
关键设计: [为什么 + [模式: XXX]]     ← 必须有"关键设计:" 前缀
数据流: [代码级 trace]              ← 必须有"数据流:" 前缀
```

**严禁**: 裸行号 / 伪行号 / 文件总行数代行号 / 代码拼接 / 缺任一四要素

### 1.3 密度标准

| 级别 | 行数/篇 |
|:--:|:--:|
| 🔴 Deep | 39-69 (超 69 需拆分) |
| 🟡 Working | 35-49 (超 49 需压缩或升级 🔴) |

### 1.4 六层深审

| 层 | 检查项 | 检测方法 |
|:--:|------|------|
| 1 | 源码行号 | sed 验证非空/非 `}` (含内联 Lxxx!) |
| 2 | 内容密度 | wc -l 每篇 |
| 3 | 语义 | 逐行 Read, 前后矛盾/数据流跳步 |
| 4 | 技术声明 | grep 源码确认归属类 (防编造) |
| 5 | 算法正确性 | 数据流步骤顺序对照源码 |
| 6 | 机制归属 | 方法/字段的确切类 (不凭继承推断) |

### 1.5 复用 ≠ 省略 (06 §2.5 — Boot 域必用)

- 纯机制内核 → **一行引用** (见 {域} §{章节} + 一句话结论)
- 本层使用/组装/配置/生命周期/差异 → **必须展开**
- **五件事检查**: ①入口 ②配置 ③生命周期 ④差异 ⑤边界 — 全无增量才允许纯引用
- **底线: 拿不准宁可展开也不引用**
- **禁止正文引用未分析域** (06 §2) — 只允许结尾桥引出下一步域

---

## §二 全部产出清单 (79 域 / 86 篇)

### Spring Framework (64 域 / 64 篇 ✅)

- **spring-core + SpEL + context 补缺 (11 域)**: s43-core-resource / s44-core-conversion / s45-core-environment / s46-core-ordered / s47-core-annotation / s48-core-profile / s49-core-taskexecutor / s50-spel / s51-apprunner / s52-classpathindex / s64-context-validation
- **spring-beans (7 域)**: s1-s7 | **spring-context (16 域)**: s8-s23 | **spring-aop (5 域)**: s24-s28 | **spring-tx (5 域)**: s29-s33 | **spring-jdbc (4 域)**: s34-s36 + s53-jdbc-datasource
- **spring-web + websocket/messaging (12 域)**: s37-s42, s54-web-interceptor, s55-web-exception, s56-web-initbinder, s57-web-webflux, s62-web-websocket, s63-web-messaging
- **spring-test (4 域)**: s58-test-mockmvc / s59-test-context / s60-test-mockbean / s61-test-sql

### Spring Boot (29 域 / 30 篇 ✅ 全部完成)

s65-boot-application → s79-boot-aot (见 §零 进度表) + s80-boot-failureanalyzer (S-16) + s81-boot-configdata (S-17) + s82-boot-logging (S-18) + s83-boot-runtime (S-19, 2篇) + s84-boot-webflux (S-20) + s85-boot-actuator (S-21) + s86-boot-test (S-22) + s87-boot-validation (S-23) + s88-boot-elasticsearch (S-24) + s89-boot-error (S-25) + s90-boot-health (S-26) + s91-boot-metrics (S-27) + s92-boot-repositories (S-28) + s93-boot-sqlinit (S-29)

### KP 文件命名

- 前缀规则: core/context 补缺 = c1-c22; Boot = b1-b15; web 早期 W 系列 = w1-w6; aop = a1-a5; tx = t1-t5; jdbc = j1-j3
- 域表映射: outlines/s{NN}-{主题}/ → knowledge-planning/{前缀}{NN}-{主题}.md (如 s65-boot-application → b1-springbootapplication.md)

---

## §三 质量标准 — 检查命令 (每域完成必跑)

```bash
# KP 检查
grep -cP '^\| P[123]' knowledge-planning/{域}.md        # >0
grep -P '^\| P[123]' knowledge-planning/{域}.md | grep -cP '🔴|🟡|🟢'  # = P条目数
grep -P '^\| P[123]' knowledge-planning/{域}.md | grep -c '为什么'      # = P条目数

# 大纲五项全等
grep -c '^### ' outlines/{域}/01-*.md; grep -c '^数据流:' ...; grep -c '^源码路径:' ...
grep -c '^场景:' ...; grep -c '^关键设计:' ...   # 五者相等

# 密度 / 结尾桥 / questions / 跨层
wc -l outlines/{域}/01-*.md    # 🔴 39-69 / 🟡 35-49
tail -1 outlines/{域}/01-*.md | grep -c '→ 引出'   # 结尾桥 =1
grep -c '视角' outlines/{域}/completeness-questions.md  # ≥3
grep -c '^| [0-9]' outlines/{域}/completeness-questions.md  # ≥5
grep -oP '🔴 Deep|🟡 Working' knowledge-planning/{域}.md | head -1  # 与大纲 header 一致
```

**行号验证** (关键):
```bash
grep -hoP '[A-Z][A-Za-z]+\.java:[0-9]+' 大纲.md | while read r; do
  # find 到文件 → sed -n '{行号}p' → 非空非 '}'
done
# 注意: 内部类引用必须指向宿主文件! (如 EmbeddedDatabaseCondition → DataSourceAutoConfiguration.java)
```

---

## §四 缺陷谱系 — 本会话新发现 (在 29 种基础上)

| # | 类型 | 示例 | 防御 |
|:--:|------|------|------|
| 30 | **正文引用未分析域 (forward reference)** | S-4 正文提 S-16/S-17; S-6 正文提 S-7/S-8/S-9; S-10 数据流提 S-12 | 每域 REVIEW 必查: `grep -oP 'S-\d+'` 逐处确认在结尾桥 |
| 31 | **内部类归属编造 (文件名)** | EmbeddedDatabaseCondition.java 不存在 — 它是 DataSourceAutoConfiguration 内部类 | 行号回归 find 找不到 = 检查是否内部类, 引用宿主文件 |
| 32 | 跨层类型不一致 | KP 🔴 大纲 🟡 | 每域检查 header |

---

## §五 文件路径

```
/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/
├── talk-method/source-code-analysis/methodology/zh/  ← 方法论正式文档 (01-08, 权威!)
└── source-analysis/
    ├── issue/源码分析执行计划.md                    ← 原始 337 域执行计划 (主线权威)
    ├── netty/ tomcat/ (t7 是 Boot 集成, 引用!)  spring/ (本会话)
    └── spring/
        ├── HANDOFF-SPRING-v4.md        ← 你正在读
        ├── BOOT-PLAN-v2.md             ← Boot 26 域规划 (知识网络化, 每域带前置依赖)
        ├── knowledge-planning/  (79 个 KP)
        └── outlines/  (79 目录 / 86 篇)

源码:
├── /data/workspace/source-code/code/spring/spring-framework/   ← Spring 6.x
├── /data/workspace/source-code/code/spring/spring-boot/        ← Boot 3.x (gradle 构建!)
└── (reactor 等第三方不在本仓库 — 无源码的域不做)
```

**重要**: ①`find` 会匹配同名类 (reactive/test 包) ②Boot 是 gradle 构建 (starter 域看 build.gradle) ③内部类引用宿主文件 ④`find .` 从 spring-boot 根找自动装配类。

---

## §七 下一步 — Boot 29 域全部完成 → 原始计划阶段 3 数据与存储

**🏁 Stage 7 (Spring Boot) BOOT-PLAN-v2 全部规划域完成 (S-1~S-29 / 30 篇)**。

### 下一阶段 (原始执行计划 阶段 3 数据与存储)

- **📌 HikariCP 详细规划已创建**: `source-analysis/hikaricp/HIKARICP-PLAN.md` (13域/知识网络化: 前置依赖+复用/展开+知识网络双链+REVIEW 修复 FastList/PoolEntry/代理族/PropertyElf+排除清单). 原始执行计划 阶段3.1 已引用并更新 H-2/H-5/H-12 行
- **知识网络**: HikariCP ↔ Spring Boot 双向边 — H-1~H-13 → S-10(s74-boot-datasource, 池化内核深入)、C-11(s53); ← 复用 spring-jdbc/tx/aop. 前向引用 S-10/C-11 均合法(已分析域)
- 已标注"深入在阶段3"的 Boot 域: HikariCP(S-10)、Redis(S-11)、ES(S-24) — 阶段3 展开连接/协议/CRUD 内核
- 📌 待办: Obsidian 知识图谱 — 全部域大纲(93+)转双链 vault (方法论 06 §6)
- W 系列 (静态资源等) 按需补充
- 开始前先回查 `issue/源码分析执行计划.md` 阶段3 主线

### Boot 探路线索 (已完成)

- **S-16 诊断** ✅ 完成: `FailureAnalyzer`/`AbstractFailureAnalyzer`/`FailureAnalysis` (boot/diagnostics) — PortInUse/NoUniqueBeanDefinition 实现
- **S-17 外部化配置深化 ⭐** ✅ 完成: `ConfigDataEnvironmentPostProcessor`/`ConfigDataEnvironment` (boot/context/config) — 17 级优先级/application.yml 加载 — 前置 C-3 复用
- **S-18 日志** ✅ 完成: `LoggingSystem`/`LogbackLoggingSystem` (boot/logging) — 独立域
- **S-19 启动运行时 (拆 2 篇)** ✅ 完成: ①可用性 `ApplicationAvailability`/`LivenessState`/`ReadinessState` + ApplicationReadyEvent ②虚拟线程开关 — 前置 C-9/C-7
- **S-20 WebFlux+Netty** ✅ 完成: `ReactiveWebServerFactory`/`NettyReactiveWebServerFactory` (boot/web/reactive) — 前置 C-15 + Netty
- **S-21 Actuator** ✅ 完成: `Endpoint`/`WebEndpoint`/`ControllerEndpoint` (spring-boot-actuator) — 前置 C-12/C-13
- **S-22 测试自动配置** ✅ 完成: @SpringBootTest/@WebMvcTest 切片 (boot-test-autoconfigure) — 前置 C-16~C-19
- **S-23 Validation** ✅ 完成: `ValidationAutoConfiguration` — 前置 C-22
- **S-24 Elasticsearch (降级 🟡)** ✅ 完成: 只讲接线 — 前置阶段3 ES

### 后续计划

- Boot 26 域全部完成后: 原始计划阶段 3 数据与存储 (HikariCP 13 域等) — 但注意 Boot 数据域已标注"深入在阶段3"
- **📌 待办: Obsidian 知识图谱** — 全部域大纲 (80+) 转双链 vault (方法论 06 §6 已记录格式)
- W 系列 (静态资源等) 仅在主线域全部完成后按需补充

---

## §八 踩坑速查 (本会话全部教训)

| # | 坑 | 教训 |
|:--:|------|------|
| 1 | 行号→`}` 系统复发 | 每次 grep -n 声明行 |
| 2 | 节=流 不相等 | 五项计数全等 |
| 3 | KP 类型 ≠ 大纲类型 | 跨层 compare |
| 4 | HANDOFF 增量累积误差 | 每次更新后 find 计数验证 |
| 5 | 密度上限 | 压缩或升级 🔴 (需 KP 同步) |
| 6 | questions 缺第三身份 | 视角≥3 |
| 7 | 结尾桥遗漏 | tail -1 检查 |
| 8 | **批量修复引入结构破坏** | **永远逐篇深审** |
| 9 | 内联 Lxxx 行号错 | 所有内联引用也 sed 验证 |
| 10 | 声明行 vs 调用点混淆 | 标注 (声明L100/调用L50) |
| 11 | **编造类/方法/常量** | grep 全库存在性 |
| 12 | 旧版源码行号残留 | 本仓库 6.2.x / Boot 3.x |
| 13 | 场景与数据流矛盾 | 场景示例与数据流参数一致 |
| 14 | find 同名类误报 | 排除 reactive/test |
| 15 | 单行替换不删行 | 压缩密度需真正删除段落 |
| 16 | **正文引用未分析域** | 只允许结尾桥引出下一步 |
| 17 | **内部类归属编造** | 行号回归 find 找不到→查内部类, 引用宿主文件 |
| 18 | 数字/数量编造 | "156 个候选" 需 = imports 文件 156 行 |
| 19 | gradle 域无行号 | starter 域引用 build.gradle, 依赖名逐项核对 |

---

## §九 质量追踪

| 指标 | 数据 |
|:--|:--:|
| Spring Framework | 64 域/64 篇 全通过 (含多轮深度 REVIEW) |
| Boot 已完成 | 29 域/30 篇 全通过 (S-1~S-29, 每批 REVIEW) — **全部规划域完成** |
| 当前全量回归 | 93 域: 四要素/节=流=路径/密度/结尾桥/跨层 全过 |
| KP P1P2P3+色+为什么 | 93/93 全达标 |
| questions | 93/93 域 ≥3身份 |

**给新 AI 的第一句话**: 从 §七 S-16 开始。每域跑完全管线后再开下一个 — 不要批量写/批量修 (批量曾毁掉 17 处结构)。行号验证是必须的。写完每篇立即跑 §三 检查命令 + 检查正文无 forward reference + 内部类归属。
