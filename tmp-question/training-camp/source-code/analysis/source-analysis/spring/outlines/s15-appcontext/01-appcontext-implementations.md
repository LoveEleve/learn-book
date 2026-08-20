# S2-8 AppContext 三大实现 — refresh() 之前的"加载阶段"

> 依赖 S2-1 refresh() | 🟡 Working | 3 KP | [模式: 模板方法 + 建造者模式]

**读者处境**: 前面的域全部从 refresh() 开始 — 但 `new AnnotationConfigApplicationContext(AppConfig.class).refresh()` 中那个 `new` 干了什么？构造器里的 reader + scanner + register 是 refresh() 的"加载阶段" — 先注册 BeanDefinition → 再 refresh 激活容器。

### 1. GenericApplicationContext — 内置 BeanFactory 的骨架

场景: 普通 Spring 测试中 `new GenericApplicationContext()` → `reader.registerBean(UserService.class)` → `context.refresh()` — GenericApplicationContext 不需要 XML 或注解配置 — 直接在构造器创建 DefaultListableBeanFactory → 通过 registerBean/registerSingleton 手动注册 Bean → refresh 激活。

源码路径:
- `GenericApplicationContext.java:105-121` — **构造器创建 BeanFactory**: 不通过 obtainFreshBeanFactory(父类) — 直接在构造器中 `this.beanFactory = new DefaultListableBeanFactory()`(L121) — BeanFactory 在 refresh() 之前就存在
- `GenericApplicationContext.java:289-294` — **refreshBeanFactory()**(override, 非空): 父类 AbstractApplicationContext.refresh() Step 2 调用 obtainFreshBeanFactory→refreshBeanFactory → Generic 版做两件事: ①`this.refreshed.compareAndSet(false, true)` CAS 校验(L290) — 已刷新过则抛 IllegalStateException(L291-292) ②`this.beanFactory.setSerializationId(getId())`(L294) — BeanFactory 虽在构造器已存在, 刷新状态仍需登记; 字段名是 **refreshed**(L112) 而非 hasBeenRefreshed
- `GenericApplicationContext.java:registerBean()` — 手动注册 Bean(非注解/XML) → 创建 BeanDefinition → 注册到 registry → 支持 Supplier/Constructor/FactoryBean

关键设计: **Why Generic 内置 BeanFactory 而非用抽象方法获取？** obtainFreshBeanFactory 是父类具体的模板方法(AbstractApplicationContext.java:716-718): 调 refreshBeanFactory()→getBeanFactory() — 真正 abstract 的是 refreshBeanFactory(L1613) 与 closeBeanFactory(L1620) — 每个子类自己决定如何创建 BeanFactory。Generic 选择"在构造器创建 = 加速"(跳过 refresh 中的 BeanFactory 创建和配置) — 但代价是"BeanFactory 不可刷新"(refreshBeanFactory 用 CAS 拒绝第二次 refresh, L290-292)。**AnnotationConfigApplicationContext 继承 Generic — 也继承了这个"内置 BeanFactory"的架构 — 这就是为什么 AnnotationConfig 的构造器可以做 register(classes) — BeanFactory 已经在了。**

数据流: new GenericApplicationContext() → L121 this.beanFactory = new DefaultListableBeanFactory() → reader = new AnnotatedBeanDefinitionReader(this) → reader.registerBean(UserService.class) → doRegisterBean → 创建 AnnotatedGenericBeanDefinition → 处理 @Scope/@Lazy → registry.registerBeanDefinition("userService", beanDef) → context.refresh() → Step1 prepareRefresh → Step2 obtainFreshBeanFactory → refreshBeanFactory(L289-295): CAS 校验 refreshed(L290)+setSerializationId(L294) → Step3 prepareBeanFactory → Step4-12 正常流程 → 容器激活

### 2. AnnotationConfigApplicationContext — reader + scanner + register 三部曲

场景: Spring Boot `SpringApplication.run(App.class)` → `new AnnotationConfigApplicationContext(AppConfig.class)` → 构造器创建 reader + scanner → `register(annotatedClasses)` → reader.doRegisterBean 把 AppConfig 类转成 BeanDefinition → `refresh()` → Step 5 ConfigurationClassPostProcessor 解析 @ComponentScan/@Bean → Step 11 创建所有 singleton Bean。

源码路径:
- `AnnotationConfigApplicationContext.java:67-93` — **构造器 + register**: ①L67-73 无参构造器: `this.reader = new AnnotatedBeanDefinitionReader(this)`(L69) + `this.scanner = new ClassPathBeanDefinitionScanner(this)`(L71) — 创建 reader/scanner 但不立即扫描 ②L90-93 带参构造器: `this()`(L91) → `register(componentClasses)`(L92) → `refresh()`(L93) — 先注册传入的配置类 → 然后启动容器 ③register(Class...) → reader.register(componentClasses) → reader.doRegisterBean
- `AnnotatedBeanDefinitionReader.java:doRegisterBean()` — **注解类→BeanDefinition**: ①创建 AnnotatedGenericBeanDefinition(metadataReader.getAnnotationMetadata()) ②`AnnotationConfigUtils.processCommonDefinitionAnnotations(abd)` — 处理 @Scope/@Lazy/@Primary/@DependsOn/@Role/@Description ③`AnnotationConfigUtils.applyScopedProxyMode` — 处理 ScopedProxy ④`BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry)` — 注册到 BeanDefinitionRegistry

关键设计: **Why reader + scanner + register 分离？** reader 负责"给定一个类→创建 BeanDefinition 并注册"(单项注册、精确) — scanner 负责"扫描包→发现类→reader 注册"(批量扫描、广域)。它们是正交关注点: Reader 做"注册一个已知类" — Scanner 做"发现哪些类需要注册" — Scanner 的结果也是调 Reader 注册。这种分离让 AnnotationConfigApplicationContext 可以同时支持 `register(Class...)`(显式注册)和 `scan(String...)`(包扫描)。

数据流: new AnnotationConfigApplicationContext(AppConfig.class) → L67 无参构造器 → L69 reader = new AnnotatedBeanDefinitionReader(this) → 注入了 Registry + Environment + ResourceLoader → L71 scanner = new ClassPathBeanDefinitionScanner(this) → L90-94 带参构造器 → L92 register(AppConfig) → reader.register(AppConfig)(AnnotatedBeanDefinitionReader.java:135-137) → L137 registerBean(componentClass)(AnnotatedBeanDefinitionReader.java:146-147) → doRegisterBean(AppConfig) → ①metadataReaderFactory.getMetadataReader("AppConfig")→getAnnotationMetadata ②new AnnotatedGenericBeanDefinition(metadata) ③processCommonDefinitionAnnotations→@Scope(singleton默认)/@Lazy(none)/@Primary(none) ④applyScopedProxyMode(noop) ⑤BeanDefinitionHolder(AppConfig) ⑥registry.registerBeanDefinition("appConfig", beanDef)→存入beanDefinitionMap → L93 refresh() → 容器启动

### 3. ClassPathXml/Web 路径 — 不同 Reader 同一骨架

场景: 旧项目用 `new ClassPathXmlApplicationContext("applicationContext.xml")` — 不注册注解类 — 而是通过 XmlBeanDefinitionReader 加载 XML 文件 → <bean> 标签 → 创建 GenericBeanDefinition → 注册到 registry → refresh()。Web 应用中 `GenericWebApplicationContext.setServletContext` 仅保存 ServletContext — Web Scopes 在 refresh() 的 postProcessBeanFactory 阶段注册 → 然后 reader.register(WebConfig.class) → refresh()。

源码路径:
- `ClassPathXmlApplicationContext.java:configLocations` — XML配置路径 → `new XmlBeanDefinitionReader(this)` → `loadBeanDefinitions(configLocations)` → XML → GenericBeanDefinition → registry.registerBeanDefinition
- `GenericWebApplicationContext.java:140-141` — **setServletContext()**: 仅把 servletContext 存入字段 — ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE 由 ContextLoader 设置(ContextLoader.java:276), 不在此方法 — Web Scopes 注册在 refresh() 的 postProcessBeanFactory(L167-172) → `WebApplicationContextUtils.registerWebApplicationScopes`(L173-179): 注册 **request/session/application** 三个作用域(L186-191) — **没有 "servletContext" 作用域名**(ServletContext 经 ServletContextScope 绑为 SCOPE_APPLICATION 并登记为 ServletContext attribute, L191-192)

关键设计: **三大 ApplicationContext 的共性: 都是 "先注册BeanDefinition → 再 refresh" 的两阶段模型。** AnnotationConfig 用 reader 注册注解类 — ClassPathXml 用 reader 加载XML — GenericWeb 用 reader 注册 + setServletContext 注册 Web Scopes — 但三者的 refresh() 完全共享 AbstractApplicationContext.refresh() 的 12 步。这是 Template Method 的极致演示: 子类定制"前期加载" — 父类固定"激活启动"。

数据流: new ClassPathXmlApplicationContext("applicationContext.xml") → AbstractApplicationContext.setConfigLocations(["applicationContext.xml"]) → refresh() → L599 obtainFreshBeanFactory→创建DefaultListableBeanFactory→new XmlBeanDefinitionReader(this)→loadBeanDefinitions(["applicationContext.xml"])→解析<bean id="ds" class="DataSource">→创建GenericBeanDefinition→registry.registerBeanDefinition("ds")→Step 3-12正常→ / new GenericWebApplicationContext() → setServletContext(servletContext)(L140-142, 仅存字段) → reader.registerBean(WebConfig.class) → doRegisterBean→创建BeanDefinition→refresh() → postProcessBeanFactory 阶段(L167-172)注册 request/session/application scopes → 三种路径的BeanDefinition全部在refresh()前注册完成

→ spring-context 第八域完成。Generic骨架→AnnotationConfig→ClassPathXml→Web 四线已合。引出 S2-9: EmbeddedWebApplicationContext — Spring Boot WebServer 创建+启动 — ServletWebServerApplicationContext.onRefresh() 如何创建内嵌 Tomcat。
