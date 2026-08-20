# S2-16 @Import/@EnableXxx — 三路分发 + 注解驱动的扩展模式

> 依赖 S2-2 @Configuration + S2-9~S2-15 全部 @EnableXxx 域 | 🟡 Working | 2 KP | [模式: 策略模式 + 模板方法]

**读者处境**: 前面学了 @EnableAsync、@EnableScheduling、@EnableCaching — 它们内部都用了 @Import — 但每种用法不同。@EnableAsync 用 ConfigurationSelector → @EnableScheduling 用 @Configuration 类 → @EnableCaching 也用 ConfigurationSelector。这三种模式代表 @Import 的三路分发。

### 1. @Import 三路分发 — Selector/Deferred/Registrar 选择决策

场景: `@Import({AsyncConfigurationSelector.class, SchedulingConfiguration.class, SomeRegistrar.class})` — ConfigurationClassParser.doProcessConfigurationClass Step ④ → processImports → 对每个 candidate 判断类型 → ①ImportSelector(selectImports→返回类名) ②ImportBeanDefinitionRegistrar(registerBeanDefinitions→直接操作registry) ③普通@Configuration类(递归processConfigurationClass)。

源码路径:
- `Import.java:56` — **@Import**: value=Class<?>[]—支持混用三种类型—按顺序逐个处理
- `ImportSelector.java:61` — **selectImports(AnnotationMetadata)**: 接收元数据(能访问@Import所在类上的所有注解)→返回类名字符串数组→**DeferredImportSelector** 变体: 继承ImportSelector+Group—不立即返回结果→收集所有DeferredSelector→最后分组聚合→排序→批量处理
- `ImportBeanDefinitionRegistrar.java:61` — **registerBeanDefinitions(AnnotationMetadata, BeanDefinitionRegistry)**: 直接操作registry→注册任意BeanDefinition→最灵活但最底层→需要手动控制依赖和排序

关键设计: **三路的适用场景决策树**: ①需要根据注解元数据决定导入哪些类？→ImportSelector(如@EnableAsync的mode=PROXY/ASPECTJ→不同Configuration类) ②多个@EnableXxx需要聚合处理+条件过滤？→DeferredImportSelector(如@EnableAutoConfiguration→所有自动配置类的条件聚合) ③需要注册特殊BeanDefinition(非标准配置类)？→ImportBeanDefinitionRegistrar(如@EnableAspectJAutoProxy注册BPP—不是通过@Configuration类)

数据流: @Import(AsyncConfigurationSelector.class) → ConfigurationClassParser.processImports(L571) → L585 candidate.isAssignable(ImportSelector) → true → L587 candidate.loadClass() → L588 ParserStrategyUtils.instantiateClass(selector) → 非 DeferredImportSelector → L598 selector.selectImports(currentSourceClass.getMetadata())→@EnableAsync 的 AnnotationMetadata(mode=PROXY)→selector根据mode返回["ProxyAsyncConfiguration"]→L599 asSourceClasses(importClassNames)→L600 递归processImports→ProxyAsyncConfiguration是@Configuration→processConfigurationClass→@Bean AsyncAnnotationBPP→doRegisterBean→registry

### 2. @EnableXxx 模式 — 三实例对比 + 如何创建自己的 Enable 注解

场景: 创建一个自定义 `@EnableFeatureX` 注解 — 需要: ①@Retention(RUNTIME)+@Target(TYPE) ②@Import(`FeatureXConfiguration.class`) ③FeatureXConfiguration(@Configuration+@Bean FeatureXBeanPostProcessor)。这就是 @EnableXxx 的标准三步: 注解声明→@Import→@Configuration @Bean。

三种实现对比:

源码路径:
- `EnableAsync.java:167` — **@Import(AsyncConfigurationSelector.class)**: mode 分支 → ProxyAsyncConfiguration / AspectJAsyncConfiguration
- `SchedulingConfiguration.java:42-43` — **@Bean 直接注册**: @Import(SchedulingConfiguration) 后容器直接注册 ScheduledAnnotationBeanPostProcessor
- `EnableCaching.java:167` — **@Import(CachingConfigurationSelector.class)**: mode 分支 → ProxyCachingConfiguration / AspectJCachingConfiguration

| @EnableXxx | @Import类型 | 选择器逻辑 | 为什么用这个 |
|:--|:--|------|------|
| @EnableAsync | ImportSelector | mode=PROXY→ProxyAsyncConfiguration / ASPECTJ→AspectJAsyncConfiguration | 需要根据用户配置(mode)返回不同Configuration类 |
| @EnableScheduling | 直接@Configuration | (无selector—直接@Import(SchedulingConfiguration)) | 只有一个Configuration—不需要根据配置分支 |
| @EnableCaching | ImportSelector | mode=PROXY→ProxyCachingConfiguration / ASPECTJ→AspectJCachingConfiguration | 同@EnableAsync—需要根据mode返回不同Configuration |

**@EnableXxx 创建模板**:
```java
@Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME)
@Documented @Import(FeatureXConfiguration.class)
public @interface EnableFeatureX { String value() default ""; }
// FeatureXConfiguration: @Configuration + @Bean FeatureXBeanPostProcessor
```

关键设计: **Why @EnableXxx 是"注解 + @Import"组合而非直接注册？** 注解本身无逻辑 — 它的作用是把 @Import(FeatureXConfiguration.class) 这个"装配指令"带给 ConfigurationClassParser — 用户只需一个注解即可复用整组配置。选择器 vs 直接 @Configuration 的取舍: 配置**固定**→直接 @Import(@EnableScheduling); 配置**随用户参数变化**(mode=PROXY/ASPECTJ)→ImportSelector 按参数返回不同 Configuration。[模式: 门面模式 — @EnableXxx 封装一组配置]

数据流: 用户自定义 @EnableFeatureX → @Import(FeatureXConfiguration.class) → 用户加在 AppConfig 上 → ConfigurationClassParser.doProcessConfigurationClass Step④→processImports→FeatureXConfiguration为@Configuration类→processConfigurationClass→FeatureXConfiguration中的@Bean FeatureXBeanPostProcessor→doRegisterBean→BPP注册到registry→refresh()Step 6 registerBeanPostProcessors→FeatureXBPP注册→Step11每个Bean调用postProcessAfterInitialization→feature增强生效

→ spring-context **全16域完成**。@Import三路分发→@EnableXxx模式→连接Spring Boot @EnableAutoConfiguration。Stage 2 spring-context层收官 → next: Stage 3 spring-aop 层(AOP代理/CGLIB/JDK Proxy/AspectJ)。
