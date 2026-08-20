# Spring Boot 源码学习范围规划

> **基准**: Spring Boot 3.5.16 + Spring Framework 6.2.17
> **数据源**: spring-boot-project 全部 13 个子模块逐包扫描（`spring-boot/` 核心包 + `spring-boot-autoconfigure/` 60+ 子包 + 837 个 Java 文件）
> **边界**: 只覆盖 Boot 特有机制，不重复 Framework 的 45 个 🔴 域
> **原则**: MVC 不独立成层——在 Boot Web 自动装配中自然覆盖

---

## 第 1 层：自动装配核心机制（5 🔴）

| # | 知识域 | 核心问题 | 核心文件 |
|---|---|---|---|
| B-1 | **@SpringBootApplication → 自动装配入口** | `@EnableAutoConfiguration` → `AutoConfigurationImportSelector` → `spring.factories`/`AutoConfiguration.imports` → `getCandidateConfigurations()` 全链路。`DeferredImportSelector` 在 Framework 基础上怎么被 Boot 使用？`AutoConfigurationSorter` 排序（`@AutoConfigureBefore/After/Order`） | ~12 |
| B-2 | **条件注解体系（完整版）** | `@ConditionalOnClass`/`OnBean`/`OnMissingBean`/`OnProperty`/`OnWebApplication`/`OnCloudPlatform`/`OnJava`/`OnThreading`/`OnExpression`/`OnResource`/`OnSingleCandidate`/`OnWarDeployment`——面试第一题。`OnBeanCondition` 的 SearchStrategy；`OnClassCondition` 怎么用 ASM 避免类加载；`NoneNestedConditions`/`AllNestedConditions`/`AnyNestedCondition` 嵌套组合 | ~40 |
| B-3 | **SpringApplication.run() 启动流程** | `SpringApplication` 构造 → `run()` → `prepareEnvironment` → `createApplicationContext` → `prepareContext` → `refreshContext` → `afterRefresh`。`SpringApplicationRunListener` 回调体系。**BootstrapContext/BootstrapRegistry**（Boot 2.4+）。**`context` 包**：`ApplicationContextInitializer` 体系（ContextId/ConfigurationWarnings）+ `ApplicationPidFileWriter` + `TypeExcludeFilter`。**`builder` 包**：`SpringApplicationBuilder` 流畅 API + 父子上下文。**`system` 包**：`ApplicationHome`/`ApplicationPid`/`ApplicationTemp`。**`ansi` 包**：ANSI 颜色（Banner）。**`io` 包**：`ApplicationResourceLoader`/`Base64ProtocolResolver` 资源加载扩展 | ~30 |
| B-4 | **@ConfigurationProperties 外部化配置** | `@ConfigurationProperties` + `@EnableConfigurationProperties` → `ConfigurationPropertiesBindingPostProcessor` → `Binder`——类型安全绑定。`Origin`/`OriginTrackedValue` 配置来源追踪。**`convert` 包**：`ApplicationConversionService` + DataSize/Duration/DelimitedString 转换器。和 Framework `@Value` 的区别？`@ConfigurationPropertiesBinding` 自定义 Converter | ~25 |
| B-5 | **Starter 机制** | `spring-boot-starters` 怎么组织依赖？一个 starter 就是一个 BOM + 自动配置类。怎么自定义一个 Starter？`spring-boot-starter`（根）vs `spring-boot-starter-web` vs `spring-boot-starter-data-redis` 的依赖传递 | ~5 |

## 第 2 层：Web 自动装配（3 🔴）

| # | 知识域 | 核心问题 | 核心文件 |
|---|---|---|---|
| B-6 | **Web 自动装配：DispatcherServlet + MVC** | `DispatcherServletAutoConfiguration` → `DispatcherServletRegistrationBean` → `WebMvcAutoConfiguration` → `WebMvcConfigurer`。前端控制器怎么被自动注册到 Servlet 容器？`@EnableWebMvc` vs Boot 默认行为的区别？`WelcomePageHandlerMapping` 欢迎页 | ~12 |
| B-7 | **嵌入式容器自动装配** | `ServletWebServerFactoryAutoConfiguration` → `TomcatServletWebServerFactoryCustomizer` → 内嵌 Tomcat 的创建/配置/启动。与 Framework Tomcat 系列的关系——只聚焦 Boot 层的自动装配链。`WebServer` 抽象 + Jetty/Undertow 切换 | ~8 |
| B-8 | **HTTP 客户端与消息转换器自动装配** | `HttpMessageConvertersAutoConfiguration` → `JacksonHttpMessageConvertersConfiguration`——`@RequestBody` 的 Jackson 反序列化怎么被自动配置？`@ConditionalOnPreferredJsonMapper`（Jackson/Gson/Jsonb 优先级）。**`http/client` 子包**：`HttpClientAutoConfiguration` + `ClientHttpRequestFactories`——RestClient/RestTemplate 自动装配（Boot 3.2+ RestClient 替代 RestTemplate）。**`jackson` 包**：`JsonComponent`/`JsonMixin`/`JsonObjectDeserializer` 自定义序列化。**`json` 包**：`JsonParser`/`JacksonJsonParser`/`GsonJsonParser`/`JsonWriter`。`Jackson2ObjectMapperBuilderCustomizer` 自定义 | ~40 |

## 第 3 层：数据访问自动装配（4 🔴）

| # | 知识域 | 核心问题 | 核心文件 |
|---|---|---|---|
| B-9 | **DataSource/JDBC 自动配置** | `DataSourceAutoConfiguration` → HikariCP 自动装配（`HikariDataSource`）。`DataSourceConfiguration$Hikari/Dbcp2/OracleUcp`。`JdbcTemplateAutoConfiguration`。`DataSourceTransactionManagerAutoConfiguration`——面试必问"Boot 怎么自动配置数据源"。`DataSourceJmxConfiguration` | ~26 |
| B-10 | **Redis 自动配置** | `RedisAutoConfiguration` → `LettuceConnectionConfiguration`/`JedisConnectionConfiguration` → `RedisTemplate`/`StringRedisTemplate`。`RedisProperties`（host/port/password/url）。`RedisReactiveAutoConfiguration`——面试必问"Boot 怎么自动配置 Redis"。`RedisUrl` 解析 | ~18 |
| B-11 | **事务自动配置** | `TransactionAutoConfiguration` → `PlatformTransactionManager` 自动装配。`TransactionManagerCustomizationAutoConfiguration`。**`dao` 包**：`PersistenceExceptionTranslationAutoConfiguration`——持久化异常翻译（Framework 5-4 DataAccessException 的 Boot 自动配置层）。Framework 5-1 讲 `@Transactional` 原理，Boot 讲 `DataSourceTransactionManager` 怎么被自动创建 | ~12 |
| B-12 | **缓存自动配置** | `CacheAutoConfiguration` → `CacheType`（SIMPLE/REDIS/CAFFEINE/...）。`CacheConfigurations` 按类型装配 `CacheManager`。`CaffeineCacheConfiguration`/`RedisCacheConfiguration`。Framework 2-9 讲 `@Cacheable` 原理，Boot 讲 `CacheManager` 怎么被自动装配 | ~24 |

## 第 4 层：异步与 AOT（2 🔴）

| # | 知识域 | 核心问题 | 核心文件 |
|---|---|---|---|
| B-13 | **TaskExecutor/异步自动配置** | `TaskExecutionAutoConfiguration` → `ThreadPoolTaskExecutor`（`application.task.execution.*`）。`TaskSchedulingAutoConfiguration` → `ThreadPoolTaskScheduler`。Framework 0-7 讲 `TaskExecutor` 抽象 + 2-7 讲 `@Async` 原理，Boot 讲默认线程池配置。`ScheduledBeanLazyInitializationExcludeFilter` | ~8 |
| B-14 | **AOT/Native Image 启动处理** | `SpringApplicationAotProcessor`——把 IoC 容器提前编译成 Native Image。`AotInitializerNotFoundException`。Framework 2-11 讲 AOT 原理（`BeanFactoryInitializationAotContribution`），Boot 讲 AOT 处理器怎么生成 `__AotInitializer` 类。Boot 3 的 GraalVM Native Image 支持基础 | ~5 |

## 第 5 层：启动与运行时特性（1 🔴 + 5 🟡）

| # | 知识域 | 级别 | 核心问题 |
|---|---|---|---|
| B-15 | **启动失败诊断 FailureAnalyzers** | 🔴 | `FailureAnalyzers` → `FailureAnalyzer` 接口 → `FailureAnalysis`。`DataSourceBeanCreationFailureAnalyzer`/`HikariDriverConfigurationFailureAnalyzer`/`NoSuchMethodDefinitionFailureAnalyzer` 等具体分析器。生产常用——启动失败时的友好提示是怎么生成的 |
| B-16 | **EnvironmentPostProcessor** | 🟡 | 比 Framework `@PropertySource` 更强大的外部配置机制——在 `application.yml` 加载之前就能修改 Environment。`ConfigDataEnvironmentPostProcessor`（Boot 2.4+ 的 `spring.config.import`）。Spring Cloud `BootstrapApplicationListener` 基于此 |
| B-17 | **日志系统自动配置** | 🟡 | `LogbackLoggingSystem`/`Log4J2LoggingSystem`——`logging.file.path`/`logging.level.root` 怎么映射到 Logback XML？`LoggingApplicationListener`。`LogFile`。`LOG_PATH`/`LOG_FILE` 占位符 |
| B-18 | **应用可用性 ApplicationAvailability** | 🟡 | `ApplicationAvailability` + `AvailabilityState`（`LivenessState`/`ReadinessState`）+ `AvailabilityChangeEvent`。云原生 K8s liveness/readiness probe 的应用层支持。Boot 2.3+ 引入 |
| B-19 | **虚拟线程支持** | 🟡 | `Threading`（`auto`/`platform`/`virtual`）——Boot 3.2+ 虚拟线程支持。`OnThreadingCondition`。`spring.threads.virtual.enabled=true` 怎么让 Tomcat/TaskExecutor 切换到虚拟线程。面试新热点 |
| B-20 | **WebFlux/Reactive 自动配置** | 🟡 | `WebFluxAutoConfiguration` + `ReactiveWebServerFactoryAutoConfiguration`——WebFlux 怎么被自动装配。`HttpHandlerAutoConfiguration`。`WebSessionIdResolverAutoConfiguration`。**`netty` 包**：`NettyAutoConfiguration`——WebFlux 默认服务器。microsphere-gateway 底层 SCG 运行在 WebFlux 上 |

## 第 6 层：Actuator 与测试扩展（2 🔴 + 2 🟡）

| # | 知识域 | 级别 | 核心问题 |
|---|---|---|---|
| B-21 | **Actuator 端点体系** | 🔴 | `spring-boot-actuator`（376 文件，35 子包）+ `actuator-autoconfigure`（448 文件）。**端点机制**：`@Endpoint`/`@ReadOperation`/`@WriteOperation`/`@DeleteOperation`（endpoint 包 134 文件）。**核心 endpoint**：health(42)/metrics(46)/info(14)/env/loggers/beans/threaddump/heapdump/mappings/conditions/configprops/prometheus/startup/scheduling。**Web 暴露**：web 包(38) `WebEndpointServletHandlerMapping`。**扩展点**：`HealthIndicator`/`HealthContributor`/`InfoContributor`/`MeterBinder`/`AuditEvent`(audit 9 文件)。**自动配置**：`HealthAutoConfiguration`/`MetricsAutoConfiguration`/`InfoAutoConfiguration`。**info 包**：`BuildProperties`/`GitProperties`/`OsInfo`。面试会问"自定义 endpoint 怎么写""HealthIndicator 怎么扩展" |
| B-22 | **测试自动配置** | 🔴 | `spring-boot-test`（101 文件）+ `spring-boot-test-autoconfigure`（173 文件）。`@SpringBootTest` 集成测试自动加载 ApplicationContext。`@MockBean`/`@SpyBean` Mockito 集成。**切片测试**：`@WebMvcTest`（只加载 MVC 层）/`@DataJpaTest`/`@RestClientTest`/`@JsonTest`。`@AutoConfigureMockMvc`。Framework 8-1/8-2 讲 MockMvc/TestContext 原理，Boot 讲测试自动配置体系。面试常问"@SpringBootTest vs @WebMvcTest 区别" |
| B-23 | **Validation 自动配置** | 🟡 | `ValidationAutoConfiguration` → `LocalValidatorFactoryBean`。`PrimaryDefaultValidatorPostProcessor`。`ValidatorAdapter`。Framework `@Valid` 原理 + Boot 自动配置。Controller `@RequestBody @Valid` 自动校验（`@NotBlank`/`@Size`）怎么生效 |
| B-24 | **Elasticsearch 自动配置** | 🟡 | `ElasticsearchRestClientAutoConfiguration` + `ElasticsearchClientAutoConfiguration` + `ReactiveElasticsearchClientAutoConfiguration`——ES 客户端自动装配。my-xhs 用 ES 8.19.19（RestClient）。`RestClientBuilderCustomizer`。`data/elasticsearch`（Spring Data Elasticsearch Repository）概述 |

---

## 淘汰清单

### 确认淘汰（已读源码 + 对照用户技术栈）

| 包 | 文件数 | 理由 |
|---|---|---|
| **security** | 68 | 用户明确不要（项目用自实现 HMAC 过滤器，不用 Spring Security 自动配置） |
| **r2dbc** | 17 | 响应式数据库，my-xhs 不用，用户确认淘汰 |
| **ssl** | 14 | 用户明确不要（Boot 3.1+ SSL Bundle 证书包管理，my-xhs 不用） |
| **sql/init（DB 初始化）** | — | `schema.sql`/`data.sql` 执行机制，my-xhs 用手工建表不用，面试低频 |
| orm (jpa) | 11 | 全是 JPA/Hibernate，用户已确认过时（MyBatis-Plus 取代） |
| domain | 4 | EntityScan，JPA 实体扫描，my-xhs 用 MyBatis-Plus |
| amqp/kafka/pulsar | — | 消息中间件，my-xhs 用 RocketMQ（单独学） |
| batch/quartz | — | 批处理/定时，my-xhs 用 XXL-Job |
| cassandra/couchbase/mongo/neo4j/ldap | — | NoSQL，my-xhs 用 MySQL+Redis+ES |
| flyway/liquibase | — | 数据库迁移工具 |
| session | — | 分布式 Session——my-xhs 用 JWT+Redis 自实现 |
| jersey/graphql/groovy/mustache/thymeleaf/freemarker | — | 视图技术/替代栈 |
| gson/jsonb | — | Jackson 已是主流 |
| jms/jmx/jooq/jndi | — | 过时或非主流 |
| h2/hazelcast | — | 内嵌数据库/内存网格 |
| rsocket | 8 | RSocket 小众 |
| integration/sendgrid | — | 集成/工具 |
| webservices/websocket | — | SOAP/WebSocket（WebSocket 在 Framework 周边 🟡） |
| hateoas/template/container/service/admin | — | 低频/边缘（container 仅 ContainerImageMetadata 2 文件；service 仅 JNDI connection） |

### 其他淘汰

| 模块 | 理由 |
|---|---|
| Devtools (93 files) | 热部署，工具性质 |
| Docker Compose (122 files) | 不是 Spring 核心 |
| Testcontainers (90 files) | 同上 |
| spring-boot-docs/parent/dependencies/tools | 构建/文档/BOM |
| Actuator 全部端点 | 376+448 files 太大，只学核心 3 个 |

---

## 统计

| | 数量 |
|---|---|
| 🔴 核心域 | **17** |
| 🟡 重要域 | **7** |
| 总计 | **24 域** |
| 预计产出文章 | 17 篇（🟡 子域在对应 🔴 中附带） |
| autoconfigure 覆盖 | 60+ 子包中保留 28 个核心包，淘汰 30+ 中间件/边缘包 |
| 模块覆盖 | spring-boot（核心）+ autoconfigure（837）+ actuator（376）+ actuator-autoconfigure（448）+ test（101）+ test-autoconfigure（173）|

## 与 Framework 规划的交叉覆盖

| Framework 域 | Boot 中自然覆盖 | 不重复原因 |
|---|---|---|
| 2-6 @Conditional | B-2 Boot 条件注解体系 | Framework 讲 `@Conditional` 接口 + `ConditionEvaluator`；Boot 讲 20+ 个具体子类的应用 |
| 6-1 DispatcherServlet | B-6 Web 自动装配 | Framework 讲 `doDispatch()` 9 步链路；Boot 讲它怎么被自动注册到 Servlet 容器 |
| 0-3 Environment | B-4 @ConfigurationProperties | Framework 讲 `PropertySource` 层次；Boot 讲类型安全的配置绑定 + `Binder` |
| 2-2 @Configuration | B-1 自动装配 | Framework 讲 `ConfigurationClassParser`；Boot 讲 `AutoConfigurationImportSelector` 怎么使用它 |
| Tomcat 系列 | B-7 嵌入式容器 | 已有 12 篇 Tomcat 深度分析；Boot 层只聚焦自动装配链 |
| 0-7 TaskExecutor | B-13 TaskExecutor 自动配置 | Framework 讲 `TaskExecutor` 抽象体系；Boot 讲 `ThreadPoolTaskExecutor` 默认配置 |
| 2-7 @Async | B-13 TaskExecutor 自动配置 | Framework 讲 `@EnableAsync`/AOP 代理/异常；Boot 讲默认线程池参数 |
| 2-9 @Cacheable | B-12 缓存自动配置 | Framework 讲 `CacheInterceptor` 原理；Boot 讲 `CacheManager` 自动装配 |
| 5-1 @Transactional | B-11 事务自动配置 | Framework 讲 `@Transactional` AOP 链路；Boot 讲 `DataSourceTransactionManager` 自动创建 |
| 5-4 DataAccessException | B-11 事务自动配置 | Framework 讲异常翻译体系；Boot 讲 `PersistenceExceptionTranslationAutoConfiguration` |
| 2-11 AOT/Native Image | B-14 AOT 启动处理 | Framework 讲 `BeanFactoryInitializationAotContribution` 原理；Boot 讲 `SpringApplicationAotProcessor` 怎么生成初始化类 |
| 7 层 WebFlux 🟡 | B-20 WebFlux 自动配置 🟡 | Framework 讲 Mono/Flux/WebHandler 基础；Boot 讲 `WebFluxAutoConfiguration` + `NettyAutoConfiguration` 装配 |

## 学习顺序（按依赖链）

```
第1层: B-1→B-2→B-3→B-4→B-5 (自动装配核心 5篇)
第2层: B-6→B-7→B-8 (Web 自动装配 3篇)
第3层: B-9→B-10→B-11→B-12 (数据访问 4篇)
第4层: B-13→B-14 (异步/AOT 2篇)
第5层: B-15 (诊断 🔴 1篇)
第6层: B-21→B-22 (Actuator/测试 🔴 2篇)
🟡域 (7篇) — 穿插在对应的🔴域中附带讲解
```

**spring-boot-project 全部 13 个子模块及其 autoconfigure 60+ 子包均已扫描覆盖，每个淘汰包均经源码确认 + 用户技术栈对照，核心包覆盖率 100%。**

---

## 旧规划问题复盘（供后续框架规划复用）

当前 Spring Boot 规划虽然已经把 24 个域扫出来了，但按新版方法论再看，仍有几个明显问题：

### 1. 现在更像“机制域清单”，还不像“完整卷结构”

这份规划回答得很好的是：
- Boot 有哪些值得学的机制
- 哪些包该保留，哪些可以淘汰

但它还没有回答另一组更重要的问题：
- `vol-springboot` 这卷书的主干到底是什么
- 哪些篇是主干层
- 哪些篇是集成层
- 哪些篇是生产层
- 哪些篇只是补深专题

也就是说，它还像“高质量域清单”，还不像“可连续写作的一卷书”。

### 2. 与 `vol-spring` 的边界更像去重表，不像卷级桥接策略

文档已经做了很多交叉覆盖说明，这很好；但它现在更像：
- 哪些域不要重复写

而还不像：
- 读者读完 `vol-spring` 后，为什么必须接着读 `vol-springboot`
- Spring 讲原理，Boot 到底补了哪些“落地层”
- 哪些篇必须由 Boot 自己承担，而不能指望 Framework 卷兜底

也就是说，边界已经有了，但“桥接逻辑”还没真正卷级化。

### 3. 缺少“完整卷补层”的显式意识

按现在的 24 域组织，已经隐含出现了：
- 自动装配主线
- Web/数据/异步/AOT 主线
- FailureAnalyzers / ApplicationAvailability / Actuator / 测试这些更像完整卷补层的内容

但文档还没有显式承认：
- 哪些是 Boot 主干层
- 哪些是集成层
- 哪些是生产层
- 哪些是测试与诊断层

所以后续如果直接照这个清单开写，很容易写成 24 篇平铺专题，而不是一卷结构稳定的书。

### 4. `Actuator`、`FailureAnalyzers`、`ApplicationAvailability` 的卷级地位还没被讲清

这些主题都很重要，但它们和主干自动装配链不是同一层问题：
- `FailureAnalyzers` 更像诊断层
- `ApplicationAvailability` 更像运行时运维层
- `Actuator` 更像生产可观测层
- `测试自动配置` 则是测试层

如果不把它们从“功能域”提升为“卷级层次”，后面很容易混写，读者会知道这些机制存在，却不清楚为什么它们应该放在卷的后半段而不是前半段。

---

## 卷级完整路线图（Spring Boot）

> 这一节用于把 `SpringBoot源码学习范围规划.md` 从“24 个机制域清单”重构成“可写的完整卷结构”。

### A. 主干层

这部分回答：
- Boot 怎么启动
- Boot 怎么自动装配
- Boot 怎么把 Spring Framework、Tomcat、DataSource、Redis、缓存等基础设施真正装到应用里

建议纳入主干层的域：
1. `B-1 @SpringBootApplication`
2. `B-2 条件注解体系`
3. `B-3 SpringApplication.run()`
4. `B-4 @ConfigurationProperties`
5. `B-5 Starter 机制`
6. `B-6 Web 自动装配`
7. `B-7 嵌入式容器自动装配`
8. `B-8 HTTP 客户端与消息转换器`
9. `B-9 DataSource/JDBC 自动配置`
10. `B-10 Redis 自动配置`
11. `B-11 事务自动配置`
12. `B-12 缓存自动配置`
13. `B-13 TaskExecutor/异步自动配置`
14. `B-14 AOT/Native Image 启动处理`

### B. 集成层

这部分回答：
- Boot 怎么站在 Spring Framework 之上做“装配桥”
- Boot 怎么站在 Tomcat、HikariCP、Redis 等设施之上做“落地桥”

这层不一定需要全是新域，但卷内写作时必须明确：
- `vol-spring` 负责原理
- `vol-springboot` 负责把原理装成真实项目

尤其关键桥接包括：
- `SpringApplication.run()` ↔ Framework `refresh()`
- `ServletWebServerFactory` ↔ Tomcat
- `DataSourceAutoConfiguration` ↔ HikariCP
- `RedisAutoConfiguration` ↔ Lettuce/Jedis

### C. 诊断与可观测层

这部分回答：
- Boot 为什么能把启动失败、健康状态、指标、信息端点做成一整套运行时诊断系统

建议纳入：
1. `B-15 FailureAnalyzers`
2. `B-18 ApplicationAvailability`
3. `B-21 Actuator 端点体系`

### D. 测试层

这部分回答：
- Boot 怎么把测试切片、自动配置、MockMvc、MockBean 等变成开发者的默认测试体验

建议纳入：
1. `B-22 测试自动配置`
2. `B-23 Validation 自动配置`（可附在测试或 Web 相关篇中）

### E. 补深层

这部分回答：
- 哪些主题主干里已经点到，但值得单独继续压深

建议保留为补深候选：
- `B-16 EnvironmentPostProcessor`
- `B-17 日志系统自动配置`
- `B-19 虚拟线程支持`
- `B-20 WebFlux/Reactive 自动配置`
- `B-24 Elasticsearch 自动配置`

这些不一定都要进入第一阶段主干卷，但应保留卷级位置，不再只是散落的“🟡 域”。

### 当前卷级判断

因此，Spring Boot 这一卷更准确的完整结构应理解为：

- **第一阶段：主干层 + 集成层**
- **第二阶段：诊断与可观测层 + 测试层 + 补深层**

如果目标只是“接在 `vol-spring` 后，把 Boot 的核心价值立住”，第一阶段就已经足够形成完整主线。

如果目标是“Spring Boot 完整卷”，则后续还要继续补：
- FailureAnalyzers
- ApplicationAvailability
- Actuator
- 测试自动配置
- EnvironmentPostProcessor / Logging / Virtual Threads / WebFlux / Elasticsearch 等补深篇
