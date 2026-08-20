# S2-13 @Lazy/@Primary/@DependsOn — Bean 创建时机、优先级和顺序控制

> 依赖 S2-1 refresh() + S2-5 DI注入 | 🟡 Working | 3 KP | [模式: 策略模式]

**读者处境**: S2-1 中 Step 11 finishBeanFactoryInitialization 创建所有非 Lazy Singleton — S2-5 中 DI 注入有多个候选时怎么选 — 三个小注解 @Lazy / @Primary / @DependsOn 解答这两个问题。

### 1. @Lazy — eager vs lazy 的创建时机开关

场景: @Service 默认是 eager singleton — refresh() Step 11 就创建 — 如果有 1000 个 Bean — 启动需要 30 秒。加上 `@Lazy` → Bean 推迟到首次 `getBean()` 时创建 — 启动快但首次调用慢。`@Lazy(true)` 是默认值 — `@Lazy(false)` 显式要求 eager(即使类上有 @Lazy 默认也强制 eager)。

源码路径:
- `Lazy.java:68` — **@Lazy**: value=true→延迟创建 / false→强制 eager
- `DefaultListableBeanFactory.java:1192` — **isLazyInit() 跳过**: preInstantiateSingletons(声明于L1111)→遍历所有 singleton→`if (!mbd.isLazyInit())`(L1192)→lazy 的 Bean 不进入 instantiateSingleton→留在容器中等待首次 getBean。(注: AbstractBeanFactory.java:641 也有 isLazyInit() 判断，但它在 predictBeanType 的 FactoryBean 分支中(L636-647)——语义是跳过 lazy FactoryBean 的类型预测，与预实例化跳过无关)
- `AnnotatedBeanDefinitionReader.doRegisterBean:processCommonDefinitionAnnotations` — **注解处理**: 读取 @Lazy → beanDef.setLazyInit(lazy.value())

关键设计: **Why @Lazy 只在 singleton 有效？** prototype Bean 每次 getBean 都创建——天然是"lazy"的——@Lazy 标注 prototype 无意义。Scope request/session 也是"lazy"(依赖HTTP请求才创建)——同样不需要 @Lazy。**@Lazy 专门针对 singleton——在"启动时全部创建"vs"用到时才创建"之间做选择。**

数据流: @Lazy @Service public class HeavyService → AnnotatedBeanDefinitionReader.doRegisterBean→processCommonDefinitionAnnotations→@Lazy→beanDef.setLazyInit(true)→refresh()Step 11→preInstantiateSingletons→遍历: HeavyService→mbd.isLazyInit()=true→跳过→其他999个Bean创建完毕→容器启动(比全部eager快～2秒)→某Controller调用 heavyService.process()→getBean("heavyService")→mbd.isLazyInit=true但getBean才触发→createBean→首次执行→耗时~200ms→后续调用直接返回singleton(已缓存在singletonObjects)

### 2. @Primary — DI 多候选时的优先级 Breakout

场景: 两个 DataSource Bean — `primaryDataSource`(生产)和 `backupDataSource`(备份) — 业务 Service 用 @Autowired DataSource → Spring 发现两个候选 → 哪个是"主要"的？`@Primary` 标注在 primaryDataSource 上 → 所有 DataSource 类型的 DI 注入自动选择它 —— 除非显式 @Qualifier("backupDataSource") 覆盖。

源码路径:
- `Primary.java:94` — **@Primary**: 标注在 @Bean/@Component 上 → BeanDefinition.primary=true
- `DefaultListableBeanFactory.java:2049` — **determinePrimaryCandidate()**: DI resolveDependency 多候选→遍历→`bdf.isPrimary()`→**找到一个返回**(两个都用@Primary→返回null→继续下一个tiebreaker: beanName) — @Primary 是第一优先级
- S2-5 DI注入: **四层 tiebreaker 顺序**: @Primary→beanName→@Qualifier→@Priority — @Primary 是第一层

关键设计: **Why @Primary 是第一层而非最后一层？** @Primary 语义是"默认选择"——如果开发者标了 @Primary — 说明所有未指定 @Qualifier 的注入都应该用它 — 这是最强信号。最弱信号是 @Priority(JSR-250)——它有数值等级——只在多个候选都通过前两层时作为 tiebreaker。**@Primary = 全局默认——@Qualifier = 局部精确——@Priority = 备选排序。**

数据流: @Autowired DataSource dataSource → DefaultListableBeanFactory.resolveDependency→findAutowireCandidates→[primaryDataSource, backupDataSource]→2个候选→determineAutowireCandidate(L2046)→①determinePrimaryCandidate(L2049)→primaryDataSource.isPrimary()=true→返回primaryDataSource→注入完成。若两个都不用@Primary→②matchesBeanName→"dataSource"≠任何beanName→③@Qualifier→无→④@Priority→无→NoUniqueBeanDefinitionException抛异常

### 3. @DependsOn — Bean 创建顺序的显式控制

场景: `eventListenerBean` 依赖 `eventMulticasterBean` — 如果用 @Autowired 注入 — Spring 通过依赖图自动推导创建顺序。但 eventListenerBean 的 @PostConstruct 直接调 eventMulticasterBean 的方法 — 没有注入关系 — Spring 无法推导顺序 — `@DependsOn("eventMulticasterBean")` 强制先创建 multicaster。

源码路径:
- `DependsOn.java:52` — **@DependsOn**: value={"dep1","dep2"} → BeanDefinition.dependsOn
- `AbstractBeanFactory.java:306` — **getDependsOn()**: doGetBean→`mbd.getDependsOn()`→遍历依赖列表→L309 `isDependent(beanName, dep)` 循环检测→成立→L310-311 抛 `BeanCreationException("Circular depends-on relationship...")`(spring-beans 中无 CircularDependsOnException 类)→L313 `registerDependentBean(dep, beanName)`→L315 `getBean(dep)`(递归)—创建依赖Bean→再创建当前Bean。真正的 beforeSingletonCreation/afterSingletonCreation 不在 dependsOn 循环里，而在后面的 getSingleton 工厂回调内部: AbstractBeanFactory.java:337 `getSingleton(beanName, 工厂)`→DefaultSingletonBeanRegistry.java:312 beforeSingletonCreation 标记 → 创建完成→L401 afterSingletonCreation

关键设计: **Why @DependsOn 和 @Autowired 的排序机制不同？** @Autowired 依赖通过"在 populateBean 时检查依赖图"间接控制顺序——A autowired B → A.getBean 内部 B.getBean → B 先创建。@DependsOn 通过"getBean 时主动检查并调用依赖Bean"直接控制顺序——A dependsOn B → A.getBean 主动调 B.getBean (不管 A 是否 autowired B)。**@Autowired = 隐式依赖(通过DI容器推导)——@DependsOn = 显式声明(开发者的意图)——用于非注入依赖场景。**

数据流: getBean("eventListenerBean")→AbstractBeanFactory.getBean→doGetBean→L306 mbd.getDependsOn()→["eventMulticasterBean"]→L309 isDependent检查(无循环)→L313 registerDependentBean(dep, beanName)→L315 getBean("eventMulticasterBean")→递归→doGetBean→L337 getSingleton(beanName, 工厂)→DefaultSingletonBeanRegistry.java:312 beforeSingletonCreation标记→createBean→multicaster创建完成→addSingleton→L401 afterSingletonCreation→返回到eventListenerBean→L337 getSingleton→beforeSingletonCreation→createBean→populateBean(@Autowired注入multicaster)→initializeBean(@PostConstruct调用multicaster.broadcast())→成功→addSingleton→afterSingletonCreation

→ spring-context 第十三域完成。@Lazy/@Primary/@DependsOn — 三个小注解四大影响: 创建时机/DI优先级/创建顺序。引出 S2-14: AOT + Native Image — GraalVM编译时替代运行时逻辑。
