# S2-14 AOT/Native Image — 编译时代替运行时的双阶段处理

> 依赖 S2-2 @Configuration + S2-7 Bean 作用域 | 🟡 Working | 2 KP | [模式: 策略模式]

**读者处境**: Java 动态特性(反射/CGLIB/动态代理/ClassLoader扫描)在 GraalVM Native Image 中不可用 — Spring 6 通过 AOT (Ahead-of-Time) 在编译时把运行时逻辑转化为静态代码 — ApplicationContext 启动跳过常规解析直接加载编译时生成的初始化器。

### 1. 双 AOT 接口 — BeanRegistration + BeanFactoryInitialization 编译时代替运行时

场景: Spring Boot 3 + GraalVM → `mvn -Pnative native:compile` → 编译时 ContextAotProcessor.performAotProcessing(L102) 调用所有 AOT Processor → `BeanRegistrationAotProcessor.processAheadOfTime(RegisteredBean)` 处理每个 @Configuration Bean → 替代 CGLIB 代理为编译时生成的子类 → `BeanFactoryInitializationAotProcessor.processAheadOfTime(beanFactory)` 对容器做初始化级处理(@PropertySource 静态注册 + ImportAware 静态映射都在 ConfigurationClassPostProcessor 内部类中生成) → `ApplicationContextAotGenerator.processAheadOfTime` 生成 `ApplicationContextInitializer` 初始化器源码 → ContextAotProcessor 写 `META-INF/native-image/` 下的 native-image.properties(ContextAotProcessor.java:163)。注: spring-framework 中不存在 `ApplicationContextAotGenerator.generateApplicationContext` 方法，也没有独立的 `PropertySourcesAotContribution`/`ImportAwareAotContribution` 顶层类。

源码路径:
- `BeanRegistrationAotProcessor.java:76`(spring-beans) — **注册阶段**: processAheadOfTime(RegisteredBean)→返回 `BeanRegistrationAotContribution`；ConfigurationClassPostProcessor 实现该接口(ConfigurationClassPostProcessor.java:317-325)——FULL 配置类→`ConfigurationClassProxyBeanRegistrationCodeFragments`(L769).`generateInstanceSupplierCode`(L797)→用 JavaPoet 生成 `new AppConfig$$SpringCGLIB$$0()` 代理类的构造代码 — 替代运行时 CGLIB Enhancer
- `BeanFactoryInitializationAotProcessor.java:47`(spring-beans) — **初始化阶段**: processAheadOfTime(ConfigurableListableBeanFactory)→BeanFactoryInitializationAotContribution。@PropertySource 的 AOT 静态注册在 `ConfigurationClassPostProcessor.java:327-340`: 有 propertySourceDescriptors→内部类 `PropertySourcesAotContribution`(L660).applyTo→generateAddPropertySourceProcessorMethod(L681)→generateAddPropertySourceProcessorCode(L722): CodeBlock 生成 `new PropertySourceProcessor(environment, resourceLoader)` + `processor.processPropertySource(...)`(L730) — 替代运行时对 @PropertySource 注解的解析
- `ApplicationContextAotGenerator.java:51` — **processAheadOfTime(GenericApplicationContext, GenerationContext)**: 签名是两个参数、非 (beanFactory)——L55 refreshForAotProcessing→L56 new ApplicationContextInitializationCodeGenerator→L59 new BeanFactoryInitializationAotContributions(beanFactory).applyTo(generationContext, codeGenerator)→L62 返回生成的初始化器 ClassName。生成物是 `ApplicationContextInitializer` 实现类；`AotApplicationContextInitializer`(spring-context/aot 独立接口, forInitializerClasses 在 AotApplicationContextInitializer.java:57) 是运行时委托入口，不是编译时输出物

关键设计: **Why 两个接口而非一个？** Registration 阶段处理单个 Bean(每个 @Bean 方法→编译时代理 / @Configuration→编译时子类) — 这是**细粒度**处理。Initialization 阶段处理整个 BeanFactory(@PropertySource→静态注册 / @ImportRegistry→静态映射) — 这是**粗粒度**处理。两个接口让 AOT 可以在不同粒度上做优化 — Registration = 每个 Bean 有自己的贡献 — Initialization = 一次初始化多个 Bean。

数据流: mvn native:compile → Spring AOT Maven Plugin → ContextAotProcessor.performAotProcessing(L102) → ApplicationContextAotGenerator.processAheadOfTime(applicationContext, generationContext)(L51) → L55 refreshForAotProcessing → L56 new ApplicationContextInitializationCodeGenerator → ①Registration: BeanRegistrationAotProcessor.processAheadOfTime(RegisteredBean)(ConfigurationClassPostProcessor.java:317)→ConfigurationClassProxyBeanRegistrationCodeFragments.generateInstanceSupplierCode(L797)→JavaPoet: `new AppConfig$$SpringCGLIB$$0()` → ②Initialization: BeanFactoryInitializationAotContributions.applyTo→加载的 BeanFactoryInitializationAotProcessor(含 ConfigurationClassPostProcessor)→有 @PropertySource→PropertySourcesAotContribution(内部类 L660)→CodeBlock 生成 new PropertySourceProcessor + processPropertySource 静态调用(L722-730)→有 ImportRegistry→ImportAwareAotContribution(内部类 L585)→CodeBlock 生成 `HashMap<String,String> mappings` + put 填充(L625-629)+ `new RootBeanDefinition(ImportAwareAotBeanPostProcessor.class)` 并 registerBeanDefinition(L630-637)→③ApplicationContextInitializationCodeGenerator 生成 ApplicationContextInitializer 源码→generationContext.writeGeneratedContent()(ContextAotProcessor.java:109)→写 META-INF/native-image/native-image.properties(L163)→运行时: `AotApplicationContextInitializer.forInitializerClasses(...)`(AotApplicationContextInitializer.java:57) 委托加载生成的初始化器→跳过 refresh() 常规解析→直接使用预生成的静态代码→启动时间缩短 80-90%

### 2. RuntimeHints — GraalVM 反射/资源/序列化的"提前声明"

场景: GraalVM Native Image 在编译时执行静态分析 — 任何未被显式调用的代码(反射/CGLIB/Resources)都被删除。`RuntimeHints.reflection().registerType(UserService.class, hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS))` 告诉 GraalVM "运行时需要反射调用 UserService 的 public 方法" → GraalVM 配置文件 `reflect-config.json` 中包含此类型 → 运行时反射调用成功。

源码路径:
- `RuntimeHints.java:34` — **RuntimeHints**: `reflection()` → TypeHint; `resources()` → ResourcePatternHint; `serialization()` → 序列化类型注册
- `ReflectionHints.java:43` — **ReflectionHints**: `registerType(type)` → `MemberCategory.INVOKE_DECLARED_CONSTRUCTORS/PUBLIC_METHODS/PUBLIC_FIELDS`
- `AotDetector.java:50` — **useGeneratedArtifacts()**: 系统属性 `spring.aot.enabled=true` → 跳过@Configuration解析直接load编译时生成的初始化器

关键设计: **Why RuntimeHints 要精确到方法而非类？** GraalVM 如果注册整个类 → 所有方法都保留(包括未调用的内部辅助方法) → native image 体积增大 30-50%。精确到方法 → 只保留实际需要反射的方法(如 @PostConstruct 的回调) → native image 体积可控。**Spring 的 RuntimeHints 设计平衡了"功能完整性"(反射可用)和"体积控制"(只保留需要的反射)。**

运行时决策链: `AotDetector.useGeneratedArtifacts()`(L50) 读 `spring.aot.enabled` 系统属性 — 该判定发生在 Spring Boot 的启动引导层(SpringApplication 选择 ApplicationContextInitializer 工厂时)：true→Boot 用 `AotApplicationContextInitializer.forInitializerClasses(...)` 加载编译时生成的初始化器，跳过常规 refresh() 解析路径 / false→Boot 走常规 refresh() 12 步 — 同一个 ApplicationContext 代码在两种模式下都能启动 — AOT 只是把"解析哪些 Bean"从运行时前移到编译时。

数据流: Maven/Gradle AOT Plugin → SpringApplicationAotProcessor → RuntimeHints hints = new RuntimeHints() → hints.reflection().registerType(UserService.class, typeHint -> typeHint.withMembers(INVOKE_PUBLIC_METHODS, DECLARED_CONSTRUCTORS)) → hints.resources().registerPattern("classpath:messages_*.properties") → hints.reflection().registerTypeIfPresent(classLoader, "com.example.MongoConfig", typeHint -> typeHint.onReachableType(MyApp.class)) → compile → GraalVM读RuntimeHints → reflect-config.json: [{"name":"UserService","methods":[{"name":"init","parameterTypes":[]}]}] → native image 编译 → 二进制包含UserService反射元数据 → 运行时 UserService 的 @PostConstruct init() 通过反射调用成功

→ spring-context 第十四域完成。双AOT接口 + RuntimeHints → GraalVM Native Image 编译时静态化。引出 S2-15: spring-context 最后三个🟡域 — @Validated + @DateTimeFormat + 元注解(@Component/@Service/@Repository/@Controller)。
