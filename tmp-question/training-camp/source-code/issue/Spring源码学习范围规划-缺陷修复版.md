# Spring Framework 源码学习范围规划 — 缺陷修复版 V8

> **基准**: Spring Framework 6.x + Spring Boot 3.x
> **修复依据**: 源码范围规划复盘方法论 + Netty/Tomcat 完整卷对照
> **修复目标**: 从"主干卷"升级为"完整卷"
> **总计**: 8 层 + 4 补充层 → **52 个 🔴 核心域 + 18 个 🟡 重要域 + 7 个补深层**

---

## 修复总览

| 缺陷类型 | 修复内容 | 新增域数 |
|---|---|---|
| 卷级分层缺失 | 补规范层、集成层、机制补深层、生产层 | +7 |
| 失败路径缺失 | 每个核心域增加失败路径子节 | 内嵌 |
| 资源生命周期总图缺失 | 补 ApplicationContext 生命周期总图 | +1 |
| 跨域连接点未标出 | 显式标出桥接类 | 内嵌 |
| 诊断能力缺失 | 补运行时诊断域 | +1 |
| 误判模块修复 | spring-messaging 保留；spring-orm/JPA 移除 | 修复 |

---

## 第 0 层：spring-core（7 域）

| # | 知识域 | 级别 | 核心问题 | 失败路径 |
|---|---|---|---|---|
| 0-1 | **Resource 抽象** | 🔴 | Spring 如何统一文件/classpath/URL 的资源加载？ | 资源不存在时的异常处理、`ResourceLoader` 返回 null |
| 0-2 | **类型系统与转换** | 🔴 | `@Value("${port}")` → `int`，ConversionService/Converter 体系 | 类型转换失败时的 `ConversionFailedException`、自定义 Converter 注册失败 |
| 0-3 | **Environment 抽象** | 🔴 | `application.properties` → Environment → `@Value` 全链路 | 配置源优先级冲突、占位符解析失败 |
| 0-4 | **Ordered 优先级体系** | 🔴 | `@Order`/Ordered/PriorityOrdered 怎么控制执行顺序 | 优先级冲突时的默认行为、`@Order` 未生效的常见原因 |
| 0-5 | **注解元数据与合并** | 🔴 | `MergedAnnotation`/`@AliasFor`/`AnnotatedElementUtils` | `@AliasFor` 合并失败、元注解继承链断裂 |
| 0-6 | **Profile 环境隔离** | 🔴 | `@Profile` → `ProfileCondition` → `spring.profiles.active` | Profile 未激活时 Bean 不注册的诊断、多 Profile 冲突 |
| 0-7 | **TaskExecutor 异步任务抽象** | 🔴 | SyncTaskExecutor/SimpleAsyncTaskExecutor/ThreadPoolTaskExecutor 体系 | 线程池耗尽时的拒绝策略、`@Async` 返回 Future 时的异常传播 |

---

## 第 1 层：spring-beans（9 域）

| # | 知识域 | 级别 | 核心问题 | 失败路径 |
|---|---|---|---|---|
| 1-1 | **BeanDefinition 模型** | 🔴 | Spring 如何描述一个 Bean？RootBeanDefinition vs GenericBeanDefinition | BeanDefinition 解析失败、`BeanDefinitionStoreException` |
| 1-2 | **BeanFactory 层次** | 🔴 | BeanFactory → Hierarchical → Listable → Configurable 继承树 | `NoSuchBeanDefinitionException`、`NoUniqueBeanDefinitionException` |
| 1-3 | **Bean 生命周期** | 🔴 | 实例化→属性填充→初始化→销毁 全链条 | **🔴 失败路径**：实例化失败（构造器异常）、属性填充失败（`@Autowired` 注入失败）、初始化失败（`@PostConstruct` 异常）、销毁失败（`@PreDestroy` 异常） |
| 1-4 | **循环依赖与三级缓存** | 🔴 | singletonObjects/earlySingletonObjects/singletonFactories | **🔴 失败路径**：构造器循环依赖为什么失败（`BeanCurrentlyInCreationException`）、`@Async` + 循环依赖为什么失败、原型作用域循环依赖 |
| 1-5 | **@Autowired / @Resource / @Qualifier** | 🔴 | 依赖注入三机制 | **🔴 失败路径**：`NoSuchBeanDefinitionException`（找不到 Bean）、`NoUniqueBeanDefinitionException`（多个候选）、`@Qualifier` 未生效 |
| 1-6 | **BeanPostProcessor 体系** | 🔴 | InstantiationAwareBPP / SmartInstantiationAwareBPP / DestructionAwareBPP | BPP 抛异常时的传播路径、BPP 排序导致的行为差异 |
| 1-7 | **BeanFactoryPostProcessor 体系** | 🔴 | BPP vs BFPP 的区别——在 refresh() 的哪个阶段执行 | BFPP 修改 BeanDefinition 失败、`BeanDefinitionValidationException` |
| 1-8 | **FactoryBean 机制** | 🔴 | FactoryBean 和 BeanFactory 的区别？MyBatis/Dubbo 为什么用 FactoryBean | `FactoryBean` 的 `getObject()` 异常、`&` 前缀获取 FactoryBean 本身 |
| 1-9 | **Bean 作用域 + Web Scopes** | 🔴 | singleton/prototype → @RequestScope/@SessionScope/@ApplicationScope | 作用域冲突（singleton 注入 prototype）、`@RequestScope` 在非 Web 环境使用 |

---

## 第 2 层：spring-context（11 域）

| # | 知识域 | 级别 | 核心问题 | 失败路径 |
|---|---|---|---|---|
| 2-1 | **refresh() 生命周期** | 🔴 | `prepareRefresh()`→...→`finishRefresh()` 12 步 | **🔴 失败路径**：`BeanFactoryPostProcessor` 失败、`BeanPostProcessor` 注册失败、`finishRefresh()` 失败、ApplicationContext 处于"不完整状态"的诊断 |
| 2-2 | **@Configuration 配置类处理** | 🔴 | ConfigurationClassParser/@ComponentScan/@Import/@Bean/CGLIB 增强 | **🔴 失败路径**：`@Bean` 方法签名不合法、`@ComponentScan` 扫描失败、CGLIB 增强失败（`@Configuration(proxyBeanMethods=false)` 的区别） |
| 2-3 | **事件机制** | 🔴 | ApplicationEvent 发布/监听/异步 | 事件监听器抛异常时的传播路径、`@EventListener` 条件不满足时的静默忽略 |
| 2-4 | **父子容器** | 🔴 | HierarchicalBeanFactory——Spring Boot + Spring Cloud 根容器+子容器 | **🔴 失败路径**：父子容器 Bean 覆盖、子容器找不到父容器 Bean、父子容器关闭顺序 |
| 2-5 | **@Import / @EnableXxx 机制** | 🔴 | ImportSelector/DeferredImportSelector——Spring Boot 自动装配基础 | `ImportSelector.selectImports()` 返回 null 或空数组、`DeferredImportSelector` 排序冲突 |
| 2-6 | **@Conditional 条件注册** | 🔴 | ConditionEvaluator → PARSE_CONFIGURATION vs REGISTER_BEAN | **🔴 诊断**：条件不满足时 Bean 为什么没注册？`--debug` 日志怎么看？`ConditionEvaluationReport` 怎么用？ |
| 2-7 | **@Async 异步执行** | 🔴 | @EnableAsync → AsyncAnnotationBeanPostProcessor → 代理 → TaskExecutor → Future → 异常 | **🔴 失败路径**：自调用失效（`this.method()` 不走代理）、线程池耗尽、`Future.get()` 超时、异常被吞 |
| 2-8 | **@Scheduled 定时任务** | 🔴 | @EnableScheduling → cron/fixedDelay/fixedRate → TaskScheduler | **🔴 失败路径**：cron 表达式错误、定时任务线程池耗尽、任务执行时间超过间隔 |
| 2-9 | **@Cacheable 缓存抽象** | 🔴 | @EnableCaching → CacheInterceptor → CacheManager → SpEL key | **🔴 失败路径**：自调用失效（同 @Transactional）、缓存穿透、SpEL key 计算异常 |
| 2-10 | **@Lazy / @Primary / @DependsOn** | 🔴 | 注册控制三元组 | `@Lazy` 代理失效场景、`@DependsOn` 循环依赖 |
| 2-11 | **AOT 编译与 Native Image** | 🔴 | Spring 6+ 核心特性——GraalVM Native Image | AOT 处理失败、反射配置缺失、Native Image 启动失败 |

---

## 第 4 层：spring-aop（4 域）

| # | 知识域 | 级别 | 核心问题 | 失败路径 |
|---|---|---|---|---|
| 4-1 | **代理机制** | 🔴 | JDK 动态代理 vs CGLIB——proxyTargetClass/optimize 选型 | **🔴 失败路径**：CGLIB 代理失败（final 类/方法）、JDK 代理要求接口、`AopConfigException` |
| 4-2 | **Advice 链执行** | 🔴 | ReflectiveMethodInvocation.proceed() 递归。@Before/@After/@Around 时序 | **🔴 失败路径**：Advice 抛异常时的传播路径、`@Around` 忘记调用 `proceed()`、Advice 排序冲突 |
| 4-3 | **@AspectJ 注解解析** | 🔴 | @EnableAspectJAutoProxy → ReflectiveAspectJAdvisorFactory | Pointcut 表达式语法错误、`@Aspect` 类不在容器中 |
| 4-4 | **自动代理流程** | 🔴 | AbstractAutoProxyCreator：wrapIfNecessary→findEligibleAdvisors→createProxy | **🔴 诊断**：为什么某个 Bean 没有被代理？`Advised` 接口怎么查看代理链？ |

---

## 第 5 层：spring-tx（4 域）

| # | 知识域 | 级别 | 核心问题 | 失败路径 |
|---|---|---|---|---|
| 5-1 | **@Transactional 完整链路** | 🔴 | 注解→TransactionAttributeSource→AOP→TransactionAspectSupport→commit/rollback | **🔴 失败路径**：`UnexpectedRollbackException`、`TransactionSystemException`、连接获取失败 |
| 5-2 | **事务失效场景矩阵** | 🔴 | 自调用/非public/异常类型/多线程/传播行为/rollbackOnly | **🔴 诊断**：事务失效时怎么排查？`spring.jpa.show-sql`、`logging.level.org.springframework.transaction` 日志怎么开？ |
| 5-3 | **事务传播行为** | 🔴 | REQUIRED/REQUIRES_NEW/NESTED 的源码差异。handleExistingTransaction 7 路分支 | 嵌套事务回滚范围、`PROPAGATION_REQUIRES_NEW` 挂起/恢复失败 |
| 5-4 | **DataAccessException 统一异常体系** | 🔴 | `dao` 包——SQLException→DataAccessException 映射 | 异常翻译链断裂、自定义 `SQLExceptionTranslator` |

---

## 第 6 层：spring-jdbc（2 域）

| # | 知识域 | 级别 | 核心问题 | 失败路径 |
|---|---|---|---|---|
| 6-1 | **JdbcTemplate 模板方法** | 🔴 | Spring 如何封装 JDBC 样板代码？ | SQL 异常翻译、`DataAccessException` 抛出路径 |
| 6-2 | **DataSource 抽象** | 🔴 | `DataSource` → `DataSourceTransactionManager` | **🔴 失败路径**：连接池耗尽、连接泄漏检测、`ConnectionTimeoutException` |

---

## 第 7 层：spring-web/webmvc（6 域）

| # | 知识域 | 级别 | 核心问题 | 失败路径 |
|---|---|---|---|---|
| 7-1 | **DispatcherServlet.doDispatch()** | 🔴 | HTTP 请求 9 步全链路 | **🔴 失败路径**：`NoHandlerFoundException`、`HttpRequestMethodNotSupportedException`、`HttpMediaTypeNotSupportedException` |
| 7-2 | **@RequestMapping 注册与匹配** | 🔴 | HandlerMapping 注册 + RequestMappingInfo 多条件匹配 | **🔴 失败路径**：`Ambiguous handler methods` 冲突、路径匹配失败、`TypeMismatchException` |
| 7-3 | **@RequestBody 反序列化** | 🔴 | RequestResponseBodyMethodProcessor → HttpMessageConverter | **🔴 失败路径**：`HttpMessageNotReadableException`（JSON 解析失败）、`MethodArgumentNotValidException`（校验失败） |
| 7-4 | **@ResponseBody 序列化** | 🔴 | 返回值→HttpMessageConverter→JSON/XML | **🔴 失败路径**：`HttpMessageNotWritableException`、`HttpMediaTypeNotAcceptableException` |
| 7-5 | **拦截器链** | 🔴 | HandlerInterceptor 三回调（pre/post/after） | `preHandle` 返回 false 时的中断路径、`afterCompletion` 异常处理 |
| 7-6 | **异常处理体系** | 🔴 | 本地 `@ExceptionHandler` → `@ControllerAdvice` → 三解析器链 | **🔴 诊断**：异常被哪个处理器捕获？`@ControllerAdvice` 排序冲突？ |

---

## 第 8 层：spring-test（2 域）

| # | 知识域 | 级别 | 核心问题 | 失败路径 |
|---|---|---|---|---|
| 8-1 | **MockMvc 请求模拟** | 🔴 | `MockMvc.perform()` → DispatcherServlet 全链路 | 测试环境配置失败、`MockMvc` 初始化异常 |
| 8-2 | **TestContext 框架** | 🔴 | `@SpringBootTest` / `@ContextConfiguration` | **🔴 失败路径**：ApplicationContext 加载失败、`@Transactional` 测试回滚失败 |

---

## 🟡 重要域（18 个）

| # | 知识域 | 级别 | 核心问题 |
|---|---|---|---|
| 2-A | @Validated / Bean Validation | 🟡 | MVC 参数校验，@Valid vs @Validated |
| 2-B | @DateTimeFormat / @NumberFormat | 🟡 | 附在 MVC 参数绑定中讲 |
| 2-C | @Component/@Service/@Repository/@Controller | 🟡 | 元注解，一句话说清 |
| 2-D | ApplicationRunner / CommandLineRunner | 🟡 | 启动回调 |
| 2-E | ClassPathIndex | 🟡 | Boot 3 启动加速 |
| 3-1 | SpEL 表达式引擎 | 🟡 | ExpressionParser→Tokenizer→AST→求值 |
| 4-A | Pointcut 匹配体系 | 🟡 | 附在自动代理中讲 |
| 5-A | TransactionSynchronizationManager | 🟡 | 6 个 ThreadLocal |
| 6-A | RowMapper/ResultSetExtractor | 🟡 | JdbcTemplate 结果映射 |
| 7-A | @InitBinder / @ModelAttribute | 🟡 | MVC 数据绑定进阶 |
| 7-B | WebFlux/Reactor 基础 | 🟡 | Mono/Flux/WebHandler——Gateway 底层 |
| 8-A | @MockBean/@SpyBean | 🟡 | Mockito 集成 |
| 8-B | @Sql | 🟡 | 测试数据准备 |
| 9-A | WebSocket | 🟡 | @EnableWebSocket/WebSocketHandler |
| 9-B | messaging | 🟡 | @MessageMapping/@SendTo |
| 10-A | spring-messaging | 🟡 | Message/MessageChannel 抽象——Cloud Stream 基础 |
| 10-B | spring-websocket | 🟡 | WebSocket 握手/STOMP |

---

## 补充层 A：规范层（3 域）

> **回答**：这套实现是在兑现哪些外部契约？哪些行为是规范要求？哪些是框架自己的实现取舍？

| # | 知识域 | 级别 | 核心问题 | 对应主干域 |
|---|---|---|---|---|
| **A-1** | **JSR-330 依赖注入规范** | 🔴 | `@Inject`/`@Named`/`@Qualifier` 与 Spring `@Autowired`/`@Component` 的关系。Spring 如何兼容 JSR-330？哪些行为是规范要求，哪些是 Spring 扩展？ | 1-5 |
| **A-2** | **JSR-250 通用注解规范** | 🔴 | `@PostConstruct`/`@PreDestroy`/`@Resource` 的规范定义。Spring 的 `CommonAnnotationBeanPostProcessor` 如何实现这些规范？与 `@Autowired` 的执行顺序？ | 1-3, 1-5 |
| **A-3** | **Servlet 规范与 Spring MVC 契约** | 🔴 | `DispatcherServlet` 如何实现 `HttpServlet.service()`？`HandlerMapping`/`HandlerAdapter`/`ViewResolver` 这套抽象是 Spring 自己的设计还是规范要求？`@RequestMapping` 的规范边界？ | 7-1 |

---

## 补充层 B：集成层（2 域）

> **回答**：上层框架如何把它装起来？外部配置如何映射到内部结构？

| # | 知识域 | 级别 | 核心问题 | 桥接关系 |
|---|---|---|---|---|
| **B-1** | **DispatcherServlet 接入 Tomcat** | 🔴 | `DispatcherServlet` 如何被注册到 Tomcat `Wrapper`？`ServletWebServerApplicationContext` 如何创建 `TomcatWebServer`？`ServletContainerInitializer` 如何触发 `DispatcherServlet` 初始化？ | Tomcat T-1 ↔ Spring 7-1 |
| **B-2** | **Spring Boot 装配 Spring Framework** | 🔴 | `@SpringBootApplication` → `AutoConfigurationImportSelector` → `ConfigurationClassParser` → `refresh()` 全链路。配置如何映射到 BeanDefinition？`@Conditional` 如何在装配阶段生效？ | Boot B-1 ↔ Spring 2-5, 2-6 |

---

## 补充层 C：机制补深层（2 域）

> **回答**：主干里已经点到、但值得独立深挖的专题

| # | 知识域 | 级别 | 核心问题 | 对应主干域 |
|---|---|---|---|---|
| **C-1** | **ApplicationContext 生命周期总图** | 🔴 | `refresh()` 12 步 → 运行态 → `close()` → `SmartLifecycle.stop()` → `DisposableBean.destroy()` → `@PreDestroy` → `destroy-method`。Bean 销毁顺序是什么？父子容器关闭顺序？ | 2-1 |
| **C-2** | **BeanFactory 销毁与回收** | 🔴 | `DefaultListableBeanFactory.destroySingletons()` 流程。`DisposableBeanAdapter` 如何组织销毁链？`DestructionAwareBeanPostProcessor` 的角色？ | 1-3 |

---

## 补充层 D：生产层（2 域）

> **回答**：性能调优怎么落？故障排查怎么查？

| # | 知识域 | 级别 | 核心问题 | 关联主干域 |
|---|---|---|---|---|
| **D-1** | **Spring 运行时诊断** | 🔴 | **循环依赖诊断**：`BeanCurrentlyInCreationException` 堆栈怎么分析？**事务失效诊断**：`logging.level.org.springframework.transaction=DEBUG` 怎么开？**AOP 代理诊断**：`Advised` 接口怎么查看代理链？**条件注解诊断**：`ConditionEvaluationReport` 怎么用？`--debug` 日志怎么看？**启动慢诊断**：Bean 创建耗时统计、`SmartInitializingSingleton` 耗时 | 全部 |
| **D-2** | **Spring 性能调优** | 🔴 | **启动加速**：`spring.main.lazy-initialization`、`@Lazy`、AOT、`spring.context.index`。**运行时优化**：`@Cacheable` 缓存命中率、`@Async` 线程池配置、事务传播行为选择。**内存优化**：Bean 作用域选择、原型 vs 单例、`@Lookup` | 全部 |

---

## 跨域桥接类索引

> **复盘方法论要求**："把桥接类单独标出来"

| 桥接类 | 连接的域 | 桥接作用 |
|---|---|---|
| `AbstractAutoProxyCreator` | Bean 生命周期 → AOP 代理 | 在 `postProcessAfterInitialization` 中创建代理 |
| `AutowiredAnnotationBeanPostProcessor` | Bean 生命周期 → `@Autowired` 注入 | 在 `populateBean` 中注入依赖 |
| `CommonAnnotationBeanPostProcessor` | Bean 生命周期 → `@Resource`/`@PostConstruct` | 处理 JSR-250 注解 |
| `ConfigurationClassParser` | `@Configuration` → `@Import`/`@Bean`/`@ComponentScan` | 解析配置类 |
| `TransactionInterceptor` | AOP → 事务管理器 | 拦截 `@Transactional` 方法 |
| `DispatcherServlet` | Tomcat `Wrapper` → Spring MVC | HTTP 请求入口 |
| `HandlerMapping` | DispatcherServlet → Controller | URL 到 Handler 的映射 |
| `HandlerAdapter` | HandlerMapping → Controller 方法 | 调用 Controller 方法 |
| `AbstractAutoConfigurationImportSelector` | Spring Boot → Spring Framework | 自动装配桥接 |
| `TomcatServletWebServerFactory` | Spring Boot → Tomcat | 嵌入式容器创建 |

---

## 统计

| 层 | 🔴 | 🟡 | 补深层 | 小计 |
|---|---|---|---|---|
| 第 0 层：spring-core | 7 | 0 | 0 | 7 |
| 第 1 层：spring-beans | 9 | 0 | 0 | 9 |
| 第 2 层：spring-context | 11 | 5 | 0 | 16 |
| 第 3 层：spring-expression | 0 | 1 | 0 | 1 |
| 第 4 层：spring-aop | 4 | 1 | 0 | 5 |
| 第 5 层：spring-tx | 4 | 1 | 0 | 5 |
| 第 6 层：spring-jdbc | 2 | 1 | 0 | 3 |
| 第 7 层：spring-web/webmvc | 6 | 3 | 0 | 9 |
| 第 8 层：spring-test | 2 | 2 | 0 | 4 |
| 补充层 A：规范层 | 3 | 0 | 0 | 3 |
| 补充层 B：集成层 | 2 | 0 | 0 | 2 |
| 补充层 C：机制补深层 | 0 | 0 | 2 | 2 |
| 补充层 D：生产层 | 2 | 0 | 0 | 2 |
| 周边模块 | 0 | 3 | 0 | 3 |
| **合计** | **52** | **18** | **2** | **72** |

**预计产出文章**: 52 篇 🔴 + 2 篇补深层 = **54 篇**（🟡 域在对应 🔴 中附带）

---

## 学习顺序（按依赖链 + 卷级分层）

```
=== 第一阶段：主干层 ===

第0层: 0-1→0-2→0-3→0-4→0-5→0-6→0-7 (7篇)
第1层: 1-1→1-2→1-3→1-4→1-5→1-6→1-7→1-8→1-9 (9篇)
第2层: 2-1→2-2→2-5→2-6→2-3→2-4→2-7→2-8→2-9→2-10→2-11 (11篇)
第4层: 4-1→4-2→4-3→4-4 (4篇)
第5层: 5-1→5-2→5-3→5-4 (4篇)
第6层: 6-1→6-2 (2篇)
第7层: 7-1→7-2→7-3→7-4→7-5→7-6 (6篇)
第3层: 3-1 (1篇，穿插在第2层@Value处理中)

=== 第二阶段：规范层 ===

A-1→A-2→A-3 (3篇)

=== 第三阶段：集成层 ===

B-1→B-2 (2篇)

=== 第四阶段：机制补深层 ===

C-1→C-2 (2篇)

=== 第五阶段：生产层 ===

D-1→D-2 (2篇)

=== 第六阶段：测试层 ===

8-1→8-2 (2篇)

🟡域 (18篇) — 穿插在对应的🔴域中附带讲解
```

---

## 与 Netty/Tomcat 完整卷的对照

| 卷级层 | Netty | Tomcat | Spring（修复后） |
|---|---|---|---|
| **主干层** | Ch1-14 | Ch1-6 | 第 0-8 层 |
| **规范层** | — | Ch8 Servlet 规范 | A-1~A-3 JSR-330/250/Servlet |
| **集成层** | — | Ch7 Spring Boot 装配 | B-1~B-2 DispatcherServlet 接入 Tomcat / Boot 装配 Framework |
| **机制补深层** | — | Ch9-11 | C-1~C-2 ApplicationContext 生命周期 / BeanFactory 销毁 |
| **生产层** | — | Ch12-14 | D-1~D-2 运行时诊断 / 性能调优 |

---

## 修复记录

| 版本 | 变化 |
|---|---|
| V7→V8 | 按复盘方法论补齐：规范层(3域)、集成层(2域)、机制补深层(2域)、生产层(2域) |
| V7→V8 | 每个核心域增加"失败路径"列 |
| V7→V8 | 增加"跨域桥接类索引"表 |
| V7→V8 | 修复 spring-messaging（⚪→🟡）；移除 spring-orm/JPA |
| V7→V8 | 统计从 45🔴+15🟡 修正为 52🔴+18🟡+2补深层 |
