# S2-9 @Conditional — 编译配置与注册 Bean 的两阶段条件评估

> 依赖 S2-2 @Configuration | 🟡 Working | 2 KP | [模式: 策略模式]

**读者处境**: S2-2 中 ConfigurationClassParser.processConfigurationClass 第一步就是 `shouldSkip` — 如果某个 @Conditional 不满足，整个 @Configuration 类被跳过。但 @ConditionalOnClass 和 @ConditionalOnBean 为什么评估时机不同？什么时候在 PARSE 阶段判断，什么时候在 REGISTER 阶段判断？

### 1. Condition 接口 + ConfigurationPhase — 为什么需要两个阶段

场景: Spring Boot `@ConditionalOnClass(DataSource.class)` 在配置类被解析时判断 — 类存在就继续解析。但 `@ConditionalOnBean(DataSource.class)` 必须在所有 Bean 注册后才判断 — 过早判断 Bean 不存在，永远返回 false。这两个评估时机对应 ConfigurationCondition.ConfigurationPhase.PARSE_CONFIGURATION 和 REGISTER_BEAN。

源码路径:
- `Condition.java:59` — **matches(ConditionContext, AnnotatedTypeMetadata)**: 单方法接口 — 接收容器上下文 + 注解元数据 → 返回 true(条件满足)/false(跳过)
- `ConfigurationCondition.java:getConfigurationPhase()` — **双阶段声明**: PARSE_CONFIGURATION(在 @Configuration 类解析时评估) vs REGISTER_BEAN(在 @Bean/@ComponentScan 注册时评估) — 每个 ConfigurationCondition 必须声明自己在哪个阶段工作
- `ConditionEvaluator.java:80-104` — **shouldSkip(Ametadata, phase)**: ①未标注 @Conditional→return false(L81-82) ②phase 为 null→按 metadata 类型推断(见 §2) ③`collectConditions(metadata)` 收集 Condition 实例(L93)→④phase 匹配: ConfigurationCondition.getConfigurationPhase()==传入的 phase 或未声明(L96-97)→⑤`!condition.matches(this.context, metadata)`(L99)→不满足→return true(skip)(L100)

关键设计: **Why 两阶段而非一个阶段？** PARSE 阶段: ConfigurationClassParser 遍历 @Configuration 类时判断 — 此时 Bean 还没创建 — 只能判断类路径/属性是否存在(OnClass/OnProperty) — 但不能判断 Bean 是否存在。REGISTER 阶段: CCPP.processConfigBeanDefinitions() 注册 @Bean 方法时判断 — 此时其他配置类的 @Bean 已经注册 — 可以判断 Bean 是否存在。**两阶段本质上是"生命周期可达信息"的差异 — PARSE 阶段能查 Environment/ClassPath — REGISTER 阶段额外能查 BeanDefinition/Bean。**

数据流: ConfigurationClassParser.processConfigurationClass(AppConfig)(L246)→L247 shouldSkip(metadata, PARSE_CONFIGURATION)→L80 shouldSkip(metadata, PARSE_CONFIGURATION)→①metadata.getAnnotationAttributes(Conditional.class)→value=[OnClassCondition.class]→②OnClassCondition 实例化→③OnClassCondition instanceof ConfigurationCondition→true→④getConfigurationPhase()==PARSE_CONFIGURATION→⑤onClassCondition.matches(context, metadata)→Class.forName("com.mysql.cj.jdbc.Driver")→存在→return true→shouldSkip=false→不跳过→继续解析AppConfig→@Bean方法处理器进入: CCPP读取@Bean dataSource()→shouldSkip(metadata, REGISTER_BEAN)→OnBeanCondition.matches→beanFactory.containsBeanDefinition("dataSource")→false(尚未注册)→shouldSkip=true→此@Bean被跳过

### 2. ConditionEvaluator — 条件评估的核心引擎

场景: Parser(解析 @Configuration 类)和 CCPP(注册 @Bean 方法)都通过 ConditionEvaluator.shouldSkip 判断是否跳过 — Evaluator 负责: 找出 @Conditional 注解 → 实例化 Condition 实现 → 按 phase 匹配 → 调用 matches → 返回结果。

源码路径:
- `ConditionEvaluator.java:70-71` — **shouldSkip(metadata)**: 单参重载 — 推断规则(L85-90): metadata 是 AnnotationMetadata **且** `ConfigurationClassUtils.isConfigurationCandidate`(L86-87)→PARSE_CONFIGURATION — 其余一律 REGISTER_BEAN(L90) — 注意普通 @Component 也是 AnnotationMetadata, 但因非配置候选走 REGISTER_BEAN; 并非"AnnotationMetadata→PARSE / MethodMetadata→REGISTER"的简单映射
- `ConditionEvaluator.java:48` — **class ConditionEvaluator**: package-private — 只在 context.annotation 包内使用 — 不暴露给外部 API
- `ConditionContext.java` — **ConditionContext 接口**: 提供四个维度的运行时信息: ①getBeanFactory()→检查 bean 是否存在 ②getEnvironment()→读取属性 ③getResourceLoader()→资源加载 ④getClassLoader()→类路径检查

关键设计: **Why ConditionEvaluator 是 package-private？** 普通开发者不应直接使用 ConditionEvaluator — 它是 Spring 内部的"条件引擎"。外部通过 @Conditional 注解声明条件 — 框架在合适的时机(PARSE 或 REGISTER)自动调用 Evaluator。封装 ConditionEvaluator 确保条件评估的时机和顺序由 Spring 控制 — 开发者只管"什么条件" — Spring 决定"什么时候评估"。

数据流: ConfigurationClassParser.parse()→processConfigurationClass→L247 shouldSkip→ConditionEvaluator.shouldSkip(metadata, PARSE_CONFIGURATION)→遍历@Conditional(ConditionA.class, ConditionB.class)→ConditionA.matches=true→ConditionB.matches=false→ANY不匹配→shouldSkip=true→return(跳过整个@Configuration类) / CCPP.processConfigBeanDefinitions→loadBeanDefinitionsForBeanMethod(@Bean)→shouldSkip(methodMetadata, REGISTER_BEAN)→ConditionC.matches=true→shouldSkip=false→继续注册@Bean

### 3. 设计意图 — 两阶段的 Spring Boot 映射

场景: Spring Boot 启动时 146 个自动配置类 — @ConditionalOnClass 在解析时过滤(类不存在→跳过)— @ConditionalOnBean 在注册时过滤(Bean 未注册→跳过)— 两阶段配合完成"按需装配"。

源码路径:
- `ConfigurationCondition.java:55-58` — **ConfigurationPhase 枚举**: PARSE_CONFIGURATION / REGISTER_BEAN 两个取值 — 每个 ConfigurationCondition 实现必须通过 getConfigurationPhase() 声明自己工作的阶段
- `ConfigurationClassParser.java:247` — **PARSE 阶段调用点**: processConfigurationClass 第一步 shouldSkip(metadata, PARSE_CONFIGURATION)
- `ConfigurationClassBeanDefinitionReader.java:191` — **REGISTER 阶段调用点**: loadBeanDefinitionsForBeanMethod 的 shouldSkip(metadata, REGISTER_BEAN)

关键设计: **PARSE_CONFIGURATION 阶段适用** @ConditionalOnClass、@ConditionalOnProperty、@ConditionalOnExpression — 不需要 Bean 存在, 只需类路径/配置属性。**REGISTER_BEAN 阶段适用** @ConditionalOnBean、@ConditionalOnMissingBean、@ConditionalOnSingleCandidate — 需要检查 Bean 是否已注册 — 只能在 Bean 定义注册后判断。

**Why Spring Boot 自动装配需要两阶段**: AutoConfigurationImportSelector(DeferredImportSelector) → 在所有配置类解析后统一处理 → 先 PARSE 阶段过滤(类路径/属性) → 剩余候选进入解析 → REGISTER 阶段过滤(Bean 存在性) → 最终注册。若只有一阶段 — @ConditionalOnBean 在 PARSE 时判断 — DataSource 未注册 → 依赖它的自动配置全被跳过 → Spring Boot 无法工作。

数据流: Spring Boot启动→@EnableAutoConfiguration→AutoConfigurationImportSelector.selectImports→读取spring.factories中146自动配置类→逐个asSourceClass→processConfigurationClass(L246)→L247 shouldSkip(PARSE_CONFIGURATION)→OnClassCondition.matches→Class.forName→类不存在→skip=true→176个候选过滤为87个→继续解析87个→@Bean方法注册→loadBeanDefinitionsForBeanMethod→shouldSkip(methodMetadata, REGISTER_BEAN)→OnBeanCondition.matches→beanFactory.containsBean→Bean不存在→skip=true→最终仅36个自动配置生效→Spring Boot应用启动

→ spring-context 第九域完成。引出 S2-10: @Async — @EnableAsync→AsyncAnnotationBPP→代理→TaskExecutor→异常处理。
