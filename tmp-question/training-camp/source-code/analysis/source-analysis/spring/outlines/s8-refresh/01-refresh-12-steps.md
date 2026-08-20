# S2-1 refresh() — Spring 容器启动的 12 步交响乐

> 依赖 S1-1~S1-7 全部 | 🔴 Deep | 3 KP | [模式: Template Method]

**读者处境**: S1-1~S1-7 学完了 spring-beans 的 7 个核心域 — 但有一个悬而未决的问题: "谁调了 BeanFactory？谁是总指挥？" — 答案是 AbstractApplicationContext.refresh() — 12 步中包含了 spring-beans 全部域的执行。

### 1. refresh() 12 步概览 — Spring 的 Template Method 极致演示

场景: `new AnnotationConfigApplicationContext(AppConfig.class).refresh()` — `AppConfig` 是一个加了 `@ComponentScan` 的配置类 — 在 12 步中的 Step 5 被解析 — Step 11 中所有扫描到的 @Component Bean 被实例化。

源码路径:
- `AbstractApplicationContext.java:588-631` — **refresh()** 完整 12 步: `prepareRefresh()`(L596)→`obtainFreshBeanFactory()`(L599)→`prepareBeanFactory()`(L602)→`postProcessBeanFactory()`(L606)→`invokeBeanFactoryPostProcessors()`(L610)→`registerBeanPostProcessors()`(L612)→`initMessageSource()`(L616)→`initApplicationEventMulticaster()`(L619)→`onRefresh()`(L622)→`registerListeners()`(L625)→`finishBeanFactoryInitialization()`(L628)→`finishRefresh()`(L631)
- `AbstractApplicationContext.java:589` — **startupShutdownLock**: 整个 refresh() 在 lock 保护下执行 — 防止并发调用 refresh()

关键设计: **Why 12 步而非一个大方法？** 每步是一个独立的方法 — 可以被子类覆写(protected)。Spring Boot 的 `ServletWebServerApplicationContext` 在 Step 9 `onRefresh()` 中创建 Tomcat WebServer。框架扩展(如 MyBatis `MapperScannerConfigurer`)在 Step 5 通过 `BeanFactoryPostProcessor` 注册额外的 BeanDefinition。**Template Method 让框架的"骨架"固定 — 扩展点留空 — 子类只填入自己的逻辑。** [模式: Template Method — 12 步骨架固定，子类覆写 4/9 等步骤]

数据流: new AnnotationConfigApplicationContext(AppConfig.class)→构造函数→`this()`→`refresh()`→L589 startupShutdownLock.lock()(防并发)→L596 ①prepareRefresh()设置active=true→L599 ②obtainFreshBeanFactory()创建DefaultListableBeanFactory→L602 ③prepareBeanFactory(bf)注入ClassLoader+注册默认BPP(ApplicationContextAwareProcessor L733+ApplicationListenerDetector L750)→L606 ④postProcessBeanFactory(bf)子类hook→L610 ⑤invokeBeanFactoryPostProcessors(bf)调用所有BFPP→L612 ⑥registerBeanPostProcessors(bf)注册BPP→L616 ⑦initMessageSource()→L619 ⑧initApplicationEventMulticaster()→L622 ⑨onRefresh()子类hook→L625 ⑩registerListeners()→L628 ⑪finishBeanFactoryInitialization(bf)创建所有单例Bean→L631 ⑫finishRefresh()发布ContextRefreshedEvent

### 2. Step 5 vs Step 6 — BFPP 和 BPP 的时机差异

场景: `@Configuration` 类的 @Bean 方法在什么时候被解析？`@Autowired` 字段在什么时候被注入？答案: @Configuration 在 Step 5(BeanFactoryPostProcessor) — BPP 在 Step 6 注册 — Bean 实例化在 Step 11。三步是严格的先后顺序。

源码路径:
- `Step 5 invokeBeanFactoryPostProcessors(L610)`: `ConfigurationClassPostProcessor`(BFPP) — 解析 `@Configuration` → `@ComponentScan` → `@Bean` 方法 → 注册额外的 `BeanDefinition` — **此时容器中的 BeanDefinition 集合才完整**
- `Step 6 registerBeanPostProcessors(L612)`: `AutowiredAnnotationBeanPostProcessor`(BPP) — 注册到 BeanFactory — **但 Bean 还没创建**
- `Step 11 finishBeanFactoryInitialization(L628)`: `beanFactory.getBean()` → `doGetBean()` → `doCreateBean()` → S1-3 的 13 步生命周期 — **此时 S1-1~S1-7 全部机制运行**

关键设计: **Why BFPP 在 BPP 之前？** BFPP 负责**增加/修改 BeanDefinition** — 在 Bean 实例化之前必须完成。如果顺序颠倒 — BPP 已经处理了 `@Autowired @Inject` 的元数据 — 然后 BFPP 又新增了 BeanDefinition — 新的 Bean 没有被 BPP 预处理 — 注入失败。**BFPP 是"定义扩展"(定义阶段) — BPP 是"实例加工"(创建阶段) — 先定义后加工。**

数据流: refresh()→`prepareRefresh()` 准备→`obtainFreshBeanFactory()` 创建 DefaultListableBeanFactory→`prepareBeanFactory(beanFactory)` 设置 ClassLoader+注册默认 BPP(`ApplicationContextAwareProcessor` L733 + `ApplicationListenerDetector` L750)→Step 4 `postProcessBeanFactory`(hook)→Step 5 `invokeBeanFactoryPostProcessors`→`ConfigurationClassPostProcessor.postProcessBeanDefinitionRegistry()`→解析 `@ComponentScan("com.example")` → 扫描到 `UserService`, `OrderService` → 注册为 `ScannedGenericBeanDefinition` → `ConfigurationClassParser.parse(@Bean)` → 注册 `DataSource` BeanDefinition → **此时 beanDefinitionMap 完整**→Step 6 `registerBeanPostProcessors`→`AutowiredAnnotationBeanPostProcessor` 注册→`CommonAnnotationBeanPostProcessor` 注册→Step 7-10: init MessageSource/Events...

### 3. Step 11 — 所有单例 Bean 创建，S1-1~S1-7 全部机制在此运行

场景: refresh() 进行到 Step 11 — `finishBeanFactoryInitialization(beanFactory)` — `beanFactory.preInstantiateSingletons()` — 遍历所有非 lazy 的 singleton BeanDefinition → `getBean(beanName)` → **S1-2 getBean 内部** → `doGetBean()` → **S1-4 三级缓存处理循环依赖** → `doCreateBean()` → **S1-3 13 步生命周期** → `createBeanInstance()`(S1-5 @Autowired 构造器注入) → `populateBean()`(S1-5 @Autowired 字段注入) → `initializeBean()`(S1-6 BPP 全景: @PostConstruct→AOP代理) → `addSingleton()` → **S1-7 FactoryBean 的 getObject()** → 全部单例 Bean 创建完成。

源码路径:
- `AbstractApplicationContext.java:628` — **finishBeanFactoryInitialization()**: 调用 `beanFactory.preInstantiateSingletons()`(DefaultListableBeanFactory) — 遍历所有 non-lazy singleton BeanDefinition
- `DefaultListableBeanFactory.java:preInstantiateSingletons()` — 对每个 beanName 调用 `getBean(beanName)` — 触发 `doGetBean()→doCreateBean()` 完整链 — **S1-1~S1-7 所有机制在此触发**

关键设计: **Why `preInstantiateSingletons()` 而非逐个 getBean？** 逐个 `getBean` 的创建顺序由 @Autowired 依赖图自然决定 — 不需要显式拓扑排序。A 依赖 B → A.getBean → B 自动先创建。Spring 信任依赖注入的图顺序 — 不额外做全局排序。

数据流: finishBeanFactoryInitialization(beanFactory)→DefaultListableBeanFactory.preInstantiateSingletons()→遍历beanDefinitionNames: "userService"=RootBeanDefinition→getBean("userService")→doGetBean→getSingleton("userService")→singletonObjects.get()→null(首次)→S1-4 beforeSingletonCreation标记创建中→createBean→doCreateBean→createBeanInstance(@Autowired构造器注入)→populateBean(@Autowired字段注入/@Resource)→initializeBean→@PostConstruct→applyBeanPostProcessorsAfterInitialization→AOP代理→addSingleton("userService", bean)→singletonObjects.put→"orderService"同理→doGetBean→getSingleton→发现依赖userService→S1-4从earlySingletonObjects获取提前暴露的引用→解决循环依赖→全部singleton创建完毕→Step 12 finishRefresh发布事件

→ spring-context 层第一域完成。引出 @Configuration 解析 — refresh() 的 Step 5 中 ConfigurationClassPostProcessor 怎么把 `@Bean` 方法变成 BeanDefinition？
