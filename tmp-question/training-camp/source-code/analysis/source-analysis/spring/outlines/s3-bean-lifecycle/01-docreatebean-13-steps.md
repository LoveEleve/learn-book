# S1-3 §1 doCreateBean 13 步 — 从 Class 到 Bean 对象的完整旅程

> 依赖 S1-2 | 🔴 Deep | 3 KP | [模式: Template Method]

**读者处境**: S1-2 getBean() 内部最后调了 `createBean(beanName, mbd, args)` → `doCreateBean(beanName, mbd, args)`。这 13 步是 Spring 最核心的代码路径——每个 `@Component`、`@Service`、`@Bean` 都走这条路径。

### 1. doCreateBean 13 步完整流程

场景: `getBean("userService")` → createBean → doCreateBean — UserService 从一个 Class 名变成一个可用的 Java 对象。其中 @Autowired 在 populateBean 注入依赖，@PostConstruct 在 initializeBean 执行，AOP 代理在 initializeBean 之后生成。

源码路径:
- `AbstractAutowireCapableBeanFactory.java:560` — **doCreateBean()**: 13 步的入口。`createBeanInstance→applyMergedBeanDefinitionPostProcessors→addSingletonFactory→populateBean→initializeBean→registerDisposableBean`
- `AbstractAutowireCapableBeanFactory.java:1405` — **populateBean()**: 遍历 InstantiationAwareBeanPostProcessor→调用 `postProcessProperties()` 处理 @Autowired/@Value→`applyPropertyValues()` 应用 XML property
- `AbstractAutowireCapableBeanFactory.java:1813` — **initializeBean()**: `invokeAwareMethods`(BeanNameAware/BeanClassLoaderAware/BeanFactoryAware) → `applyBeanPostProcessorsBeforeInitialization`(@PostConstruct) → `invokeInitMethods`(afterPropertiesSet+initMethod) → `applyBeanPostProcessorsAfterInitialization`(**AOP代理在此生成**)
- `AbstractAutowireCapableBeanFactory.java:424` — **applyBeanPostProcessorsBeforeInitialization**: 遍历所有 BPP 调 `postProcessBeforeInitialization()` — `CommonAnnotationBeanPostProcessor` 在此处理 @PostConstruct
- `AbstractAutowireCapableBeanFactory.java:440` — **applyBeanPostProcessorsAfterInitialization**: 遍历 BPP 调 `postProcessAfterInitialization()` — `AbstractAutoProxyCreator` 在此创建 AOP 代理(CGLIB/JDK)

关键设计: **Why AOP 代理在 AfterInitialization 而非 Before？** 代理需要包裹一个有完整属性的 Bean。如果代理在 BeforeInitialization 生成 — @PostConstruct 调的是代理对象的 init 方法 — 代理可能没有正确初始化目标对象 — 导致 NPE。AfterInitialization 确保: Bean 的所有属性已填充 + @PostConstruct 已执行 → Bean 已经是"可用"状态 → 代理包裹它。

数据流: `doCreateBean("userService")`→1.`createBeanInstance`: 构造器注入/`new UserService()`→2.`applyMergedBeanDefinitionPostProcessors`: 处理 @Autowired/@Value 元数据→3.`addSingletonFactory("userService", () -> getEarlyBeanReference(name, mbd, bean))` 放入三级缓存→4.`populateBean`: `postProcessProperties()` 处理 @Autowired→注入 ServiceB→5.`initializeBean`: (i)`invokeAwareMethods` setBeanName/setBeanFactory (ii)`applyBeanPostProcessorsBeforeInitialization` @PostConstruct (iii)`invokeInitMethods` afterPropertiesSet + initMethod (iv)`applyBeanPostProcessorsAfterInitialization` — `AbstractAutoProxyCreator` 检查是否需要代理→需要→`ProxyFactory.getProxy()` 创建 CGLIB 代理→6.`registerDisposableBean`: 注册 destroy 回调→返回代理对象。

### 2. 构造器注入 vs setter 注入 — createBeanInstance 的分支

场景: `@Autowired public UserService(AccountRepo repo)` — 构造器注入。`@Autowired private AccountRepo repo` — setter/字段注入。两者在 doCreateBean 内部走不同路径。

源码路径:
- `createBeanInstance()` 内部: `determineConstructorsFromBeanPostProcessors()` — `AutowiredAnnotationBeanPostProcessor.determineCandidateConstructors()` 查找 @Autowired 构造器
- 多个 @Autowired 构造器? Spring 按"最多参数+最具体类型"选择 — 无法确定? 抛异常

关键设计: **Why 构造器注入优先于字段注入？** 构造器注入保证了 Bean 在构造完成后就是"完整"的(所有必需依赖已注入)。字段注入的 Bean 在构造后暂时不完整 — `populateBean` 才填充 — 如果有人在 populateBean 之前调用这个 Bean(通过三级缓存的提前暴露)→依赖为 null→NPE。**构造器注入 = 不可变 + 编译安全 + 测试友好。**  Spring 官方推荐构造器注入。

数据流: `createBeanInstance("userService")`→`determineConstructorsFromBeanPostProcessors()`→AutowiredAnnotationBeanPostProcessor 扫描→找到一个 `@Autowired public UserService(AccountRepo)`→`ConstructorResolver.autowireConstructor()`→`args = resolveAutowiredArgument(AccountRepo.class)`→`getBean("accountRepo")` 递归创建→`ctor.newInstance(accountRepo)` → UserService 实例 → 返回。

### 3. SmartInstantiationAwareBeanPostProcessor — 预测 Bean 类型

场景: `@Autowired List<UserService> services` — Spring 需要找到所有 UserService 类型的 Bean — 但 UserService 可能被 AOP 代理 — getBean 返回的是代理对象的类型(可能是 CGLIB 生成的子类)。Spring 用 `SmartInstantiationAwareBeanPostProcessor.predictBeanType()` 在 Bean 创建**之前**就告诉容器"这个 Bean 最终会是什么类型"。

源码路径: `AbstractAutoProxyCreator.predictBeanType(beanClass, beanName)` (AbstractAutoProxyCreator.java:226-231) — 检查 `proxyTypes` 缓存 — 为空(还没有任何 Bean 被代理过)→直接返回 null(不预测); 非空→返回 `proxyTypes.get(cacheKey)`(该 Bean 创建代理时记录的代理类型)。**"回退到 beanClass"是 `determineBeanType()`(:235-258) 的行为** — proxyTypes 无记录且 `getAdvicesAndAdvisorsForBean` 判定不需要代理时返回 beanClass — 两者不要混淆。这个信息在 `getBeanNamesForType(UserService.class)` 时使用 — 容器需要知道 `userService` Bean 是 `UserService` 类型(而不是 CGLIB 生成的 `UserService$$SpringCGLIB$$0`)。

关键设计: **Why predictBeanType 在实例化之前？** getBeanNamesForType 可能在 Bean 实例化之前被调用(如 `@Autowired List<UserService>` 的@Autowired解析需要遍历所有 BeanDefinition 确定类型)。如果等 Bean 实例化完才知道类型 — 循环依赖时根本来不及。predictBeanType 让容器在 only BeanDefinition 阶段就能预测类型 — 无需实例化。

数据流: `@Autowired List<UserService> services` 注入解析→`DefaultListableBeanFactory.getBeanNamesForType(UserService.class)`→遍历全部 BeanDefinition→对每个候选调 `predictBeanType`(AbstractAutoProxyCreator.java:226)→proxyTypes 为空→返回 null→容器**回退调用** `determineBeanType()`(:235-258)→BeanDefinition 未解析 class→按 beanClassName 加载→`isFactoryBean` 判断→非 FactoryBean→返回 beanClass=UserService→容器确认 "userService" 是 UserService 类型→加入 candidates→注入 List<UserService>。若某 Bean 已被代理→proxyTypes 缓存命中→返回 `UserService$$SpringCGLIB$$0` 的父接口/原始类型→List 类型匹配不误判。

→ 引出 §2 BeanPostProcessor — initializeBean 中的 Before/After 回调只是 BPP 体系的一小部分。Spring 实际有 SmartInstantiationAwareBeanPostProcessor(预测Bean类型用于@Autowired解析)、InstantiationAwareBeanPostProcessor(实例化前可返回代理跳过13步)、DestructionAwareBeanPostProcessor(@PreDestroy)。
