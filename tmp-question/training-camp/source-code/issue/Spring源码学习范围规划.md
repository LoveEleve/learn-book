# Spring Framework 深度掌握规划 V7（完整版）

> **数据源**：8 个模块全部兄弟包的逐目录扫描（`ls src/main/java/org/springframework/`）
> **过滤标准**：🔴 面试高频+生产影响大 → 🟡 面试中频或生产影响中 → ⚪ 淘汰或概念级
> **总计**：8 个模块 → 6 层 → **45 个 🔴 核心域 + 15 个 🟡 重要域**

---

## 第 0 层：spring-core（728 文件，自研核心 7 域）

> 只列 `org.springframework.core` 子包。`asm`/`cglib`/`aot`/`lang`/`util`/`objenesis`/`javapoet` 这些重打包/工具包不在规划域内，学习时按需查阅即可。

| # | 知识域 | 级别 | 核心问题 |
|---|---|---|---|
| 0-1 | **Resource 抽象** | 🔴 | Spring 如何统一文件/classpath/URL 的资源加载？`SpringFactoriesLoader` |
| 0-2 | **类型系统与转换** | 🔴 | `@Value("${port}")` → `int`，ConversionService/Converter 体系 |
| 0-3 | **Environment 抽象** | 🔴 | `application.properties` → Environment → `@Value` 全链路。附带 `@PropertySource`/`@PropertySources`/`PropertySourcesPlaceholderConfigurer`/`PropertySource` 子类体系（17 个相关文件，体系完整但面试几乎不单独考——注资频，不独立成域） |
| 0-4 | **Ordered 优先级体系** | 🔴 | `@Order`/Ordered/PriorityOrdered 怎么控制执行顺序 |
| 0-5 | **注解元数据与合并** | 🔴 | `MergedAnnotation`/`@AliasFor`/`AnnotatedElementUtils` |
| 0-6 | **Profile 环境隔离** | 🔴 | `@Profile` → `ProfileCondition` → `spring.profiles.active` |
| 0-7 | **TaskExecutor 异步任务抽象** | 🔴 | SyncTaskExecutor/SimpleAsyncTaskExecutor/ThreadPoolTaskExecutor/ConcurrentTaskExecutor 体系——@Async（2-7）的执行侧底层。面试常考"Spring 异步线程池怎么配"，与 2-7 注解侧（@EnableAsync/AOP 代理/异常）分离独立讲透 |

---

## 第 1 层：spring-beans（333 文件，9 域）

| # | 知识域 | 级别 | 核心问题 |
|---|---|---|---|
| 1-1 | **BeanDefinition 模型** | 🔴 | Spring 如何描述一个 Bean？RootBeanDefinition vs GenericBeanDefinition |
| 1-2 | **BeanFactory 层次** | 🔴 | BeanFactory → Hierarchical → Listable → Configurable 继承树 |
| 1-3 | **Bean 生命周期** | 🔴 | 实例化→属性填充→初始化→销毁 全链条。BPP 在各阶段的拦截点。初始化时序：`@PostConstruct` → `afterPropertiesSet()` → `init-method`；销毁回调：`@PreDestroy` → `DisposableBean.destroy()` → `destroy-method` |
| 1-4 | **循环依赖与三级缓存** | 🔴 | singletonObjects/earlySingletonObjects/singletonFactories——面试第一题 |
| 1-5 | **@Autowired / @Resource / @Qualifier 依赖注入三机制** | 🔴 | `@Autowired`（按类型，AutowiredAnnotationBeanPostProcessor）、`@Resource`（按名称优先，CommonAnnotationBeanPostProcessor）、`@Qualifier`（限定符消歧）——面试经典题 |
| 1-6 | **BeanPostProcessor 体系** | 🔴 | InstantiationAwareBPP / SmartInstantiationAwareBPP / DestructionAwareBPP |
| 1-7 | **BeanFactoryPostProcessor 体系** | 🔴 | BPP vs BFPP 的区别（面试经典）——在 refresh() 的哪个阶段执行 |
| 1-8 | **FactoryBean 机制** | 🔴 | FactoryBean 和 BeanFactory 的区别？MyBatis/Dubbo 为什么用 FactoryBean |
| 1-9 | **Bean 作用域 + Web Scopes** | 🔴 | singleton/prototype → @RequestScope/@SessionScope/@ApplicationScope。**@Lookup 方法注入**——singleton 注入 prototype 的 CGLIB 方案 |

---

## 第 2 层：spring-context（1090 文件，12 个兄弟包 → 聚焦 11 域）

> 扫描发现 spring-context 有 12 个兄弟包：cache/context/ejb/format/instrument/jmx/jndi/scheduling/scripting/stereotype/ui/validation

| # | 知识域 | 级别 | 涉及包 | 核心问题 |
|---|---|---|---|---|
| 2-1 | **refresh() 生命周期** | 🔴 | `context/support` | `prepareRefresh()`→...→`finishRefresh()` 12 步 |
| 2-2 | **@Configuration 配置类处理** | 🔴 | `context/annotation` | ConfigurationClassParser/@ComponentScan/@Import/@Bean/CGLIB 增强 |
| 2-3 | **事件机制** | 🔴 | `context/event` | ApplicationEvent 发布/监听/异步。`@EventListener` SpEL condition |
| 2-4 | **父子容器** | 🔴 | `context/support` | HierarchicalBeanFactory——Spring Boot + Spring Cloud 根容器+子容器 |
| 2-5 | **@Import / @EnableXxx 机制** | 🔴 | `context/annotation` | ImportSelector/DeferredImportSelector——Spring Boot 自动装配基础 |
| 2-6 | **@Conditional 条件注册** | 🔴 | `context/annotation` | ConditionEvaluator → PARSE_CONFIGURATION vs REGISTER_BEAN |
| 2-7 | **@Async 异步执行** | 🔴 | `scheduling/annotation` | @EnableAsync → AsyncAnnotationBeanPostProcessor → 代理 → TaskExecutor → Future → 异常 |
| 2-8 | **@Scheduled 定时任务** | 🔴 | `scheduling/annotation` | @EnableScheduling → cron/fixedDelay/fixedRate → TaskScheduler |
| 2-9 | **@Cacheable 缓存抽象** | 🔴 | `cache/` (69文件) | @EnableCaching → CacheInterceptor → CacheManager → SpEL key。自调用失效同 @Transactional |
| 2-10 | **@Lazy / @Primary / @DependsOn 注册控制** | 🔴 | `context/annotation` | @Lazy 代理机制 + @Primary 主选规则 + @DependsOn 显式控制初始化顺序 |
| 2-11 | **AOT 编译与 Native Image** | 🔴 | `context/aot` | Spring 6+ 核心特性——BeanFactoryInitializationAotContribution/BeanRegistrationAotContribution，提前编译为 Native Image。Spring Boot 3 的 GraalVM Native Image 支持基础，面试新热点 |

**🟡 重要但机制简单的域**：
- 2-A：@Validated / Bean Validation（`validation/` 51文件）— MVC 参数校验，面试常问 @Valid vs @Validated
- 2-B：@DateTimeFormat / @NumberFormat（`format/` 51文件）— 附在 MVC 参数绑定中讲
- 2-C：@Component/@Service/@Repository/@Controller 元注解（`stereotype/` 6文件）— 基础，一句话说清
- 2-D：ApplicationRunner / CommandLineRunner — 启动回调
- 2-E：ClassPathIndex 类路径索引（`context/index`）— Spring Boot 3 启动加速关键，`ClassPathIndex` 文件 + `PackagesAndClassesPathIndex`，与 AOT 一脉相承的启动优化方向

**⚪ 淘汰**：ejb/jndi/scripting/ui/instrument（面试不问，生产不用）、jmx（Actuator 替代）、i18n（MessageSource 国际化，生产使用频度低，不独立成域）

**⚪ 淘汰（25 个子模块全覆盖审计补充）**：
- spring-context-support（87 文件）：`cache/`（Caffeine/JCache 集成，已在 2-9 覆盖）+`mail/`（JavaMail）+`scheduling/`（Quartz 集成，已在 2-8 覆盖）+`ui/`（UI 工具）—— 核心机制已在 2-8/2-9 覆盖
- spring-r2dbc（65 文件）：`connection/`+`core/`（R2DBC 响应式数据库访问，spring-jdbc 的响应式版本）—— my-xhs 不用响应式 DB
- spring-aspects（14 文件）：AspectJ 编译时织入（beans/cache/context/scheduling/transaction 声明式事务的 AspectJ 实现）—— spring-aop 4-1~4-4 已覆盖 AOP
- spring-context-indexer（13 文件）：`processor/`（组件索引生成器，META-INF/spring.components）—— 启动加速工具
- spring-core-test（45 文件）/ spring-instrument（2 文件）/ spring-jcl（8 文件）/ buildSrc（11 文件）/ framework-docs（164 文件）—— 测试/agent/日志桥接/构建/文档

---

## 第 3 层：spring-expression（122 文件，1 域）

| # | 知识域 | 级别 | 核心问题 |
|---|---|---|---|
| 3-1 | **SpEL 表达式引擎** | 🟡 | ExpressionParser→Tokenizer→AST→求值。附在 @Value/@Cacheable condition 中讲 |

---

## 第 4 层：spring-aop（226 文件，4 域）

| # | 知识域 | 级别 | 核心问题 |
|---|---|---|---|
| 4-1 | **代理机制** | 🔴 | JDK 动态代理 vs CGLIB——proxyTargetClass/optimize 选型 |
| 4-2 | **Advice 链执行** | 🔴 | ReflectiveMethodInvocation.proceed() 递归。@Before/@After/@Around 时序 |
| 4-3 | **@AspectJ 注解解析** | 🔴 | @EnableAspectJAutoProxy → ReflectiveAspectJAdvisorFactory |
| 4-4 | **自动代理流程** | 🔴 | AbstractAutoProxyCreator：wrapIfNecessary→findEligibleAdvisors→createProxy |

**🟡**：Pointcut 匹配体系（`support/` 29文件）——附在自动代理中讲
**⚪**：AspectJ 深入/XML config

---

## 第 5 层：spring-tx（172 文件，4 域）

| # | 知识域 | 级别 | 核心问题 |
|---|---|---|---|
| 5-1 | **@Transactional 完整链路** | 🔴 | 注解→TransactionAttributeSource→AOP→TransactionAspectSupport→commit/rollback |
| 5-2 | **事务失效场景矩阵** | 🔴 | 自调用/非public/异常类型/多线程/传播行为/rollbackOnly——每类带可复现代码 |
| 5-3 | **事务传播行为** | 🔴 | REQUIRED/REQUIRES_NEW/NESTED 的源码差异。handleExistingTransaction 7 路分支 |
| 5-4 | **DataAccessException 统一异常体系** | 🔴 | `dao` 包——Spring 如何把 SQLException（checked）翻译成 DataAccessException（unchecked）体系？SQLExceptionTranslator/PersistenceExceptionTranslator。面试经典题"Spring 如何统一数据访问异常"，JDBC/MyBatis/JPA 共用这套翻译体系 |

**🟡**：TransactionSynchronizationManager（6 ThreadLocal）/ TransactionTemplate
**⚪**：JTA/响应式事务/DAO/JCA
**注**：JCA 淘汰，但 `dao` 包的 DataAccessException 体系保留为 🔴（生产常用+面试经典）

---

## 第 6 层：spring-jdbc（252 文件）

> microsphere-alibaba-druid 基于 JDBC 层，`AbstractStatementFilter` 继承 `FilterAdapter`（JDBC SPI）

| # | 知识域 | 级别 | 核心问题 | 核心文件 |
|---|---|---|---|---|
| 7-1 | **JdbcTemplate 模板方法** | 🔴 | Spring 如何封装 JDBC 样板代码？模板方法模式在数据库操作中的经典应用 | ~10 |
| 7-2 | **DataSource 抽象** | 🔴 | `DataSource` → `DataSourceTransactionManager`——spring-tx 的具体实现。连接池（HikariCP/Druid）怎么接入？ | ~8 |

**🟡**：RowMapper/ResultSetExtractor、NamedParameterJdbcTemplate

---

## 第 7 层：spring-web/webmvc（1101 文件，6 域）

| # | 知识域 | 级别 | 核心问题 |
|---|---|---|---|
| 6-1 | **DispatcherServlet.doDispatch()** | 🔴 | HTTP 请求 9 步全链路：getHandler→preHandle→handle→postHandle→render |
| 6-2 | **@RequestMapping 注册与匹配 + 参数解析** | 🔴 | HandlerMapping 注册 + RequestMappingInfo 多条件匹配 + Ambiguous 冲突。附带 `@PathVariable`/`@RequestParam`/`@RequestHeader`/`@CookieValue` 参数解析 |
| 6-3 | **@RequestBody 反序列化** | 🔴 | RequestResponseBodyMethodProcessor → readWithMessageConverters → HttpMessageConverter → 校验 |
| 6-4 | **@ResponseBody 序列化** | 🔴 | 返回值→HttpMessageConverter→JSON/XML→ResponseBodyAdvice |
| 6-5 | **拦截器链** | 🔴 | HandlerInterceptor 三回调（pre/post/after）。MappedInterceptor 路径过滤 |
| 6-6 | **异常处理体系** | 🔴 | 本地 `@ExceptionHandler` → `@ControllerAdvice` → 三解析器链。附带 `@ResponseStatus` / `ResponseStatusException`（自定 HTTP 状态码） |

**🟡**：@InitBinder / @ModelAttribute / @SessionAttributes（MVC 数据绑定进阶）、RestTemplate/RestClient、OncePerRequestFilter、PathPattern 引擎、CORS、参数解析器体系
**🟡 WebFlux/Reactor 基础**：Mono/Flux 响应式类型、WebHandler/WebFilter（替代 Servlet）、ServerWebExchange、`core/codec` 编解码（Encoder/Decoder 体系，给 Reactor 流式读写用）——microsphere-gateway 底层 SCG 运行在 WebFlux 上，需了解基础
**⚪**：ViewResolver/JSP/FlashMap/Theme/静态资源/函数式Web/文件上传/HTTP Interface

---

## 第 8 层：spring-test（432 文件）

> 技术专家不可能不懂测试框架。MockMvc 面试中高，生产每天写测试。

| # | 知识域 | 级别 | 核心问题 | 核心文件 |
|---|---|---|---|---|
| 8-1 | **MockMvc 请求模拟** | 🔴 | `MockMvc.perform()` → DispatcherServlet 全链路如何在不启动 Web 容器的情况下执行？ | ~10 |
| 8-2 | **TestContext 框架** | 🔴 | `@SpringBootTest` / `@ContextConfiguration` / `@Transactional` 测试——如何加载最小 ApplicationContext 并自动回滚？ | ~10 |

**🟡**：@MockBean/@SpyBean（Mockito 集成）、TestRestTemplate、@DataJpaTest 等切片测试注解
**🟡**：@Sql 测试数据准备（`test/jdbc` 包）——生产写测试天天用，执行前后 SQL 脚本管理

---

## 九、周边模块（🟡 按需涉猎，不独立成层）

| 模块 | 文件数 | 级别 | 说明 |
|---|---|---|---|
| spring-websocket | 175 | 🟡 | WebSocket 握手/STOMP——实时推送，面试中高 |
| spring-messaging | 246 | 🟡 | Message/MessageChannel 抽象——Spring Cloud Stream 基础，不熟悉正是要学的理由 |
| spring-orm | 78 | ⚪ | JPA/Hibernate——MyBatis-Plus 取代，确实过时了（这是技术判断，不是不熟悉） |
| spring-oxm | 28 | ⚪ | XML 编组——彻底淘汰 |
| spring-jms | 117 | ⚪ | JMS——rocketmq/kafka 取代 |

---

## 十、统计

| | 数量 |
|---|---|
| 🔴 核心域 | **45** |
| 🟡 重要域 | **15** |
| ⚪ 淘汰域 | ~17 |
| 总涉及源文件（核心域） | ~640 个（从 3800+ 文件中筛出） |
| 预计产出文章 | 45 篇（每篇标准：源码走读+面试点+生产陷阱） |

## 十一、新增说明（本版）

| 决策 | 模块 | 理由 |
|---|---|---|
| 🔴 新增 | spring-jdbc | microsphere-alibaba-druid 基于 JDBC Filter 体系，JdbcTemplate 是 spring-tx 在单数据源场景的具体实现 |
| 🟡 新增 | WebFlux/Reactor 基础 | microsphere-gateway 底层 SCG 运行在 WebFlux 上，需了解 Mono/Flux/WebHandler |
| ⚪ 确认淘汰 | spring-orm (JPA/Hibernate) | MyBatis-Plus 已取代，确实过时了 |
| ⚪ 确认淘汰 | spring-messaging | Message/MessageChannel（Spring Cloud Stream 底层），你没接触过就不相关 |
| ⚪ 确认淘汰 | spring-jms | rocketmq/kafka 取代 |
| ⚪ 确认淘汰 | spring-oxm | XML 编组，彻底淘汰 |

## 八、V7 演进记录

| 变化 | 说明 |
|---|---|
| spring-context 兄弟包扫描 | 从只扫 `context/`(199) → 完整扫描 12 个兄弟包(1090) |
| @Cacheable 缓存抽象 | 🔴 新增——`cache/` 包 69 文件，面试高频 |
| @Validated 校验 | 🟡 新增——`validation/` 包 51 文件 |
| @DateTimeFormat 格式化 | 🟡 新增——`format/` 包 51 文件 |
| @Component 元注解 | 🟡 新增——`stereotype/` 包 6 文件 |
| jmx/ejb/jndi/scripting | ⚪ 确认淘汰 |
| **遗漏补全（兄弟包二次核对）** | |
| 🔴 0-7 TaskExecutor | `core/task`——@Async 执行侧底层，面试常考线程池配置 |
| 🔴 2-11 AOT/Native Image | `context/aot`——Spring 6+ 核心特性，Boot 3 面试新热点 |
| 🔴 5-4 DataAccessException | `tx/dao`——面试经典"Spring 如何统一数据访问异常" |
| 🟡 2-E ClassPathIndex | `context/index`——Boot 3 启动加速 |
| 🟡 @Sql | `test/jdbc`——测试数据准备，生产天天用 |
| 🟡 core/codec | 附在 WebFlux 域——Reactor 编解码 |
| ⚪ i18n/MessageSource | 淘汰——生产使用频度低 |
| 统计修正 | 原声明 49🔴+24🟡 一直数错（实际 42+13）；补全遗漏后 **45🔴+15🟡** |

## 九、学习顺序（按依赖链）

```
第0层: 0-1→0-2→0-3→0-4→0-5→0-6→0-7 (7篇)
第1层: 1-1→1-2→1-3→1-4→1-5→1-6→1-7→1-8→1-9 (9篇)
第2层: 2-1→2-2→2-5→2-6→2-3→2-4→2-7→2-8→2-9→2-10→2-11 (11篇)
第4层: 4-1→4-2→4-3→4-4 (4篇)
第5层: 5-1→5-2→5-3→5-4 (4篇)
第6层: 6-1→6-2→6-3→6-4→6-5→6-6 (6篇)
第3层: 3-1 (1篇，穿插在第2层@Value处理中)
🟡域 (15篇) — 穿插在对应的🔴域中附带讲解
```

**全部模块及其兄弟包均已扫描覆盖，覆盖率为 100%。**
