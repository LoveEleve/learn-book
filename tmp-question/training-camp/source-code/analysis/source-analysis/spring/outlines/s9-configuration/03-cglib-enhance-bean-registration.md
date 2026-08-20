# S2-2-3 CGLIB 增强 + @Bean 注册 — proxyBeanMethods 的单例魔法

> 依赖 S2-2-2 | 🔴 Deep | 4 KP | [模式: 代理模式]

**读者处境**: 篇1+2讲完了"解析"——@ComponentScan找到类、@Import分发、@Bean方法提取——但解析结果还是内存中的 ConfigurationClass 对象。这一篇讲"注册"——ConfigurationClass 怎么变成 BeanDefinition 注册到 DefaultListableBeanFactory — 以及 @Configuration(proxyBeanMethods=true) 的 CGLIB 代理怎么保证 @Bean 方法间的引用永远返回同一个单例。

### 1. CCPP.enhanceConfigurationClasses — CGLIB 代理的入口

场景: CCPP.postProcessBeanFactory() → 在前面CCPP.processConfigBeanDefinitions()已经完成了@Configuration解析和BeanDefinition注册→现在又调用 enhanceConfigurationClasses() — 为标记为 FULL 的 @Configuration 类创建 CGLIB 子类代理。

源码路径:
- `ConfigurationClassPostProcessor.java:477-549` — **enhanceConfigurationClasses()**: 遍历所有 BeanDefinition → 判断 `CONFIGURATION_CLASS_ATTRIBUTE`: null→跳过, lite+无@Bean→跳过, full→`resolveBeanClass()` 提前加载类 → 创建 ConfigurationClassEnhancer → `enhancer.enhance(configClass, beanClassLoader)` → 替换 BeanDefinition 的 beanClass 为增强子类
- `ConfigurationClassEnhancer.java:81-87` — **CALLBACKS**: 三个静态回调按序排列: `BeanMethodInterceptor`(@Bean方法)→`BeanFactoryAwareMethodInterceptor`(setBeanFactory)→`NoOp.INSTANCE`(其他方法透传)
- `ConfigurationClassEnhancer.java:102-140` — **enhance()**: ①幂等检查(L103-112): `EnhancedConfiguration.class.isAssignableFrom(configClass)`→已增强直接返回 ②ClassLoader 解析(L117-126): SmartClassLoader适配+包可见性检测 ③newEnhancer()+createClass()(L127-133): 生成 CGLIB 子类
- `ConfigurationClassEnhancer.java:227` — **EnhancedConfiguration**: Marker 接口 extends BeanFactoryAware — 双重用途: isAssignableFrom 幂等检查 + 确保增强子类能接收 BeanFactory

关键设计: **Why lite 模式不需要 CGLIB？** lite(@Component 等) 中的 @Bean 方法不走代理——方法间互相调用就是普通方法调用(每次 new 新对象)——不保证单例。full(@Configuration proxyBeanMethods=true) 通过 CGLIB 代理拦截 @Bean 方法间调用——从容器取而非重新执行——这才是"@Bean 方法返回单例"的真正实现。**如果没有 CGLIB，@Bean 方法间引用会返回不同对象——这违背了 Spring 的"默认单例"期望。**

数据流: CCPP.postProcessBeanFactory(L298) → enhanceConfigurationClasses(L477) → 遍历 beanDefinitionNames → L482 beanDef.getAttribute(CONFIGURATION_CLASS_ATTRIBUTE) → null→跳过 / lite+无@Bean→跳过(L494-497) / full→ L499 resolveBeanClass()提前加载类(L501-504 catch 抛 "Cannot load configuration class" 错误)→ L512 检查单例过早实例化(退化警告)→ 加入 configBeanDefs → L532 new ConfigurationClassEnhancer() → 逐个: L539 enhancer.enhance(configClass, beanClassLoader) → enhance内部: L103 isAssignableFrom(EnhancedConfiguration)幂等检查→ L115-126 ClassLoader解析(SmartClassLoader+包可见性)→ L127 newEnhancer()配置8项→ L128 createClass()生成字节码+registerStaticCallbacks(L212)→ 返回增强子类 → L545 beanDef.setBeanClass(enhancedClass)替换原始类

### 2. BeanMethodInterceptor — 容器调用 vs 用户引用的分支

场景: @Configuration 类 `AppConfig` 中 `dataSource()` @Bean 调用了 `transactionManager(dataSource())` — 容器处理顺序: 先调 dataSource() 创建 DataSource 单例 → 然后调 transactionManager() — 在 transactionManager() 内部调用 dataSource() 时被 CGLIB 拦截 → 不从方法调用创建新的 DataSource → 而是 `beanFactory.getBean("dataSource")` 返回已创建的同一个 DataSource。

源码路径:
- `ConfigurationClassEnhancer.java:353-404` — **BeanMethodInterceptor.intercept()**: ①获取 BeanFactory+beanName(L356-357) ②处理 ScopedProxy(L360-365) ③FactoryBean 检查+增强(L374-384) ④**容器调用分支**(L386-401): `isCurrentlyInvokedFactoryMethod(beanMethod)`=true → `cglibMethodProxy.invokeSuper()` 真正执行 @Bean 方法 → 附带 BPP 返回类型警告(L390-399: @Bean 返回 BFPP 类型会失败) ⑤**用户引用分支**(L403): `isCurrentlyInvokedFactoryMethod`=false → `resolveBeanReference()` 从容器获取
- `ConfigurationClassEnhancer.java:513-517` — **isCurrentlyInvokedFactoryMethod()**: 通过 `SimpleInstantiationStrategy.getCurrentlyInvokedFactoryMethod()` — ThreadLocal 存储 → 比较方法名+参数类型(L514-516): **只比较名称和参数，不比较返回类型** — 规避 Groovy 协变返回类型的 equals 误判
- `ConfigurationClassEnhancer.java:407-471` — **resolveBeanReference()**: ①处理 in-creation 状态(L414-418): 暂时设为 false 避免 BeanCurrentlyInCreationException → ②null 参数处理(L419-430): 退回 getBean(name) 非 getBean(name, args) → ③getBean() 调用(L431-432) → ④类型兼容检查(L433-458): NullBean→退化为null, 不兼容→IllegalStateException → ⑤依赖注册(L459-463): `registerDependentBean` 建立销毁顺序

关键设计: **容器调用 vs 用户引用的"开关"是如何工作的？** ThreadLocal `currentlyInvokedFactoryMethod` 是关键。SimpleInstantiationStrategy 在 `instantiateWithFactoryMethod()`(:85-97) 中、@Bean 工厂方法(instanceSupplier)执行前把当前 Method 写入 ThreadLocal — 执行完毕恢复/清除(:93/:96)。它由 `instantiate()`(:166-169) 在 doCreateBean 的**实例化阶段**调用(createBeanInstance → instantiateUsingFactoryMethod, AbstractAutowireCapableBeanFactory.java:1205) — **与 invokeInitMethods(初始化阶段)无关**。CGLIB 的 BeanMethodInterceptor 拦截每个 @Bean 方法调用时检查这个 ThreadLocal — 如果匹配(容器正在调用这个方法)→ 执行真实方法体 → 如果不匹配(用户在另一个 @Bean 方法内调用这个方法)→ 从容器获取已存在的 bean。**这是一个巧妙的"谁在调用我"判断——而非"我该返回什么"判断。**

数据流: getBean("dataSource") → doGetBean → getSingleton(λ createBean→doCreateBean, AbstractBeanFactory.java:337-348) → **addSingleton 之后**才调 AbstractBeanFactory.getObjectForBeanInstance(L349) → doCreateBean → 检测到factoryMethod=AppConfig$Enhanced.dataSource → 实例化阶段 createBeanInstance→instantiateUsingFactoryMethod(AbstractAutowireCapableBeanFactory.java:1205) → ConstructorResolver → SimpleInstantiationStrategy.instantiate(L166-169) → instantiateWithFactoryMethod L88 currentlyInvokedFactoryMethod.set(dataSourceMethod) → 调用 AppConfig$Enhanced.dataSource() → CGLIB拦截 → BeanMethodInterceptor.intercept(L353) → L356 反射读$$beanFactory字段→ L357 BeanAnnotationHelper.determineBeanNameFor → L386 isCurrentlyInvokedFactoryMethod=true? → L400 cglibMethodProxy.invokeSuper()执行真实方法体 → 返回DataSource实例 → L93/L96 currentlyInvokedFactoryMethod恢复/清除 → 容器getBean继续: transactionManager(dataSource()) → instantiateWithFactoryMethod.set(transactionManager方法) → AppConfig$Enhanced.transactionManager() → CGLIB拦截 → L386 isCurrentlyInvokedFactoryMethod=true? (比较方法名+参数类型, L514-516) → invokeSuper执行→ 方法体内调this.dataSource() → 再次CGLIB拦截 → L386 此时currentlyInvokedFactoryMethod=transactionManager≠dataSource → false → L403 resolveBeanReference → L407 resolveBeanReference: in-creation状态避让(L414-418)→ L431 beanFactory.getBean("dataSource")返回已存在的单例

### 3. BeanFactoryAwareMethodInterceptor + $$beanFactory 字段注入

场景: CGLIB 生成的 EnhancedConfiguration 子类需要能获取 BeanFactory(用于 BeanMethodInterceptor 里调 getBean()) — 但原始 AppConfig 不一定实现 BeanFactoryAware。如何注入 BeanFactory？

源码路径:
- `ConfigurationClassEnhancer.java:281-298` — **BeanFactoryAwareGeneratorStrategy**: 继承 `ClassLoaderAwareGeneratorStrategy` — 在 CGLIB `end_class()` 阶段通过 ASM `declare_field` 注入 `$$beanFactory` 字段(L291-294: `ACC_PUBLIC + BeanFactory` 类型)
- `ConfigurationClassEnhancer.java:89` — **BEAN_FACTORY_FIELD**: 常量 `"$$beanFactory"` — 名称带 `$$` 前缀避免与用户字段冲突
- `ConfigurationClassEnhancer.java:306-334` — **BeanFactoryAwareMethodInterceptor.intercept()**: 拦截 `setBeanFactory(BeanFactory)` → ①反射写入 $$beanFactory 字段(L310-314) ②如果原始父类也实现 BeanFactoryAware → 调用 `super.setBeanFactory()(L317-320)`
- `ConfigurationClassEnhancer.java:328-333` — **isSetBeanFactory(Method)**: 三重精确匹配: 方法名="setBeanFactory" + 参数类型=BeanFactory + 声明类实现 BeanFactoryAware

关键设计: **ASM 字段注入 vs 接口继承 — 为什么要两套？** EnhancedConfiguration 继承 BeanFactoryAware → 让增强子类"声称"自己需要 BeanFactory — 容器会调 setBeanFactory 注入。但 BeanFactory 引用必须存储在某处供 BeanMethodInterceptor 使用 — **通过 ASM 在字节码层注入 public 字段 `$$beanFactory`** — BeanFactoryAwareMethodInterceptor 拦截 setBeanFactory 把注入的 BeanFactory 写进这个字段 — BeanMethodInterceptor 再从字段读取 BeanFactory。**CGLIB 增强 = 字节码层字段注入 + 方法拦截做路由 — 而非靠"静态变量"或"XML 配置"。**

数据流: CGLIB Enhancer生成EnhancedConfiguration子类 → newEnhancer(L171)内部 L185 setStrategy(new BeanFactoryAwareGeneratorStrategy(classLoader)) → BeanFactoryAwareGeneratorStrategy.transform(L288-297): end_class()事件(L291)时ASM declare_field注入public BeanFactory $$beanFactory(L292) → 容器initializeBean→ invokeAwareMethods→ setBeanFactory(beanFactory) → CGLIB拦截→ CALLBACK_FILTER匹配索引1 → BeanFactoryAwareMethodInterceptor.intercept(L310) → L311 Field $$beanFactory = ReflectionUtils.findField(obj.getClass(), "$$beanFactory") → L313 field.set(obj, args[0])写入BeanFactory → L317 ClassUtils.getUserClass获取原始类判断是否实现BeanFactoryAware → L318 proxy.invokeSuper原始setBeanFactory

### 4. BeanDefinitionReader — ConfigurationClass 模型 → BeanDefinition 注册

场景: do-while 循环中每轮 parse 后 → `reader.loadBeanDefinitions(configClasses)` — 将 Parser 产的 ConfigurationClass 模型(带 BeanMethod/importedResources/importBeanDefinitionRegistrars)写入 registry。

源码路径:
- `ConfigurationClassBeanDefinitionReader.java:128-149` — **loadBeanDefinitionsForConfigurationClass()**: 分发顺序: ①注册配置类自身(若 imported)(L140-142, `registerBeanDefinitionForImportedConfigurationClass`) ②loadBeanDefinitionsForBeanMethod(L143-145) ③loadBeanDefinitionsFromImportedResources(L147) ④loadBeanDefinitionsFromRegistrars(L148)
- `ConfigurationClassBeanDefinitionReader.java:185-306` — **loadBeanDefinitionsForBeanMethod()**: ①条件检查(L191-194): `conditionEvaluator.shouldSkip(REGISTER_BEAN)` 跳过不满足条件的 @Bean ②提取 @Bean 属性(L199-278): beanName(显式 name L203-211 / 默认方法名 L212-215)、isOverriddenByExistingDefinition 检查(L218-225)、autowireCandidate(L257)/initMethod(L272)/destroyMethod(L277) ③**创建 ConfigurationClassBeanDefinition**(L227): `setAutowireMode(AUTOWIRE_CONSTRUCTOR)`(L254, 无 byType)、processCommonDefinitionAnnotations(L255) ④**static/instance 分支**(L230-244): static @Bean → `setBeanClass()`(L233/236) + `setUniqueFactoryMethodName()`(L238), 直接构造器实例化; instance @Bean → `setFactoryBeanName(configClass)`(L242) + `setUniqueFactoryMethodName(methodName)`(L243) → 通过工厂 bean 调用创建 ⑤@Scope+ScopedProxy 处理(L280-299): proxyMode 判定(L281-289) + `ScopedProxyCreator.createScopedProxy`(L293-298)
- `ConfigurationClassBeanDefinitionReader.java:420` — **ConfigurationClassBeanDefinition**: 内部类, marker subclass of RootBeanDefinition(implements AnnotatedBeanDefinition) — `setLenientConstructorResolution(false)`(L435): 禁用宽松构造器解析 — @Bean 方法的参数必须精确匹配

关键设计: **static @Bean vs instance @Bean 的 BeanDefinition 差异？** static @Bean: `beanClass=返回类型, factoryBeanName=null` — 容器直接用构造器创建(普通 bean)。instance @Bean: `factoryBeanName=配置类beanName, factoryMethodName=@Bean方法名, beanClass=返回类型` — 容器先获取配置类实例(CGLIB代理) → 然后调其 @Bean 方法(`invokeSuper`)创建 bean。**这就是为什么 static @Bean 方法不走 CGLIB 代理路径——它不依赖配置类实例。**

数据流: CCPP.do-while→parser.parse()→ConfigurationClass模型(含BeanMethod)→reader.loadBeanDefinitions(configClasses)→遍历 ConfigurationClass → loadBeanDefinitionsForConfigurationClass() → loadBeanDefinitionsForBeanMethod(每个@Bean)→创建 ConfigurationClassBeanDefinition(L227)→判断 static(L230)→若 instance→setFactoryBeanName(AppConfig)+setUniqueFactoryMethodName(dataSource)→注册到 registry.beanDefinitionMap("dataSource", ...)→下次 getBean("dataSource")时→getBean(AppConfig CGLIB代理)→调 dataSource()→CGLIB拦截→invokeSuper真正执行

→ spring-context 第二域完成。@Configuration解析 -> @Bean注册 -> CGLIB代理全链路贯通。引出 S2-3: ApplicationListener/@EventListener 事件机制 — refresh() Step 8 initApplicationEventMulticaster 初始化的广播器和 Step 10 registerListeners 注册的监听器在 Step 11 Bean创建后如何工作？
