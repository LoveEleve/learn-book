# S1-5 §1 @Autowired — byType 注入的完整链路

> 依赖 S1-3 | 🔴 Deep | 2 KP | [模式: Chain of Responsibility]

**读者处境**: @Autowired 是 Spring 最常用的注解 — 但 `@Autowired private UserService userService` 从注解扫描到字段赋值的完整链路是什么？和 XML `<property name="userService" ref="userService"/>` 有什么区别？

### 1. postProcessProperties — @Autowired 的入口

场景: `populateBean("userController")` — UserController 有 `@Autowired private UserService userService`。Spring 需要找到这个字段，确定要注入什么 Bean，然后通过反射赋值。

源码路径:
- `AutowiredAnnotationBeanPostProcessor.java:506` — **postProcessProperties(pvs, bean, beanName)**: `findAutowiringMetadata(beanName, bean.getClass(), pvs)` → 返回 `InjectionMetadata` → `metadata.inject(bean, beanName, pvs)` → 遍历 `AutowiredFieldElement` 和 `AutowiredMethodElement` → 逐个注入
- `AutowiredAnnotationBeanPostProcessor.java:543` — **findAutowiringMetadata(clazz)**: 扫描类的所有字段和方法 → 找到 `@Autowired` / `@Value` / `@Inject` 注解 → 构建 `AutowiredFieldElement(Field)` 或 `AutowiredMethodElement(Method)` → 缓存到 `injectionMetadataCache`(ConcurrentHashMap, L186)
- `InjectionMetadata.inject()`: 遍历 `injectedElements` → 每个元素调用 `element.inject(target, beanName, pvs)`

关键设计: **Why @Autowired 通过 BPP 而非 XML property 方式？** XML `<property>` 在 `populateBean` 的 `applyPropertyValues()` 中处理(调用 setter 方法)。@Autowired 走 `postProcessProperties()` — 在 `applyPropertyValues` **之前**执行(InstantiationAwareBeanPostProcessor 回调)。这个顺序很重要: **@Autowired 先注入** → 然后 XML 的 property 覆盖(允许 XML 显式配置覆盖注解自动装配)。[模式: Chain of Responsibility — 多个注入源顺序执行]

数据流: `doCreateBean("userController")`→`populateBean(beanName, mbd, bw)`(AbstractAutowireCapableBeanFactory.java:1405)→遍历 `instantiationAwareBeanPostProcessors`→调 `postProcessProperties(pvs, bean, beanName)`(AutowiredAnnotationBeanPostProcessor.java:506)→`findAutowiringMetadata(beanName, bean.getClass(), pvs)`→从 `injectionMetadataCache` 取缓存(未命中→`buildAutowiringMetadata` 反射扫描)→`metadata.inject(bean, beanName, pvs)`→遍历 injectedElements→`AutowiredFieldElement.inject()`→`beanFactory.resolveDependency(...)`→注入结果→`field.set(bean, value)`→继续 populateBean→`applyPropertyValues()` 应用 XML property→**若 XML 也配置了同名字段则覆盖 @Autowired 结果**。

### 2. 从 @Autowired 到 beanFactory.resolveDependency

场景: `@Autowired private UserService userService` — BPP 知道这个字段需要注入，但注入什么？如果容器中有两个 UserService 类型的 Bean(如 `userServiceImpl` 和 `mockUserService`)—Spring 怎么选择的？

源码路径:
- `AutowiredFieldElement.inject()`: 调用 `beanFactory.resolveDependency(desc, beanName, autowiredBeanNames, typeConverter)` — desc 是 `DependencyDescriptor`(封装了 Field + @Autowired 元数据)
- `DefaultListableBeanFactory.doResolveDependency()`: byType查找 `findAutowireCandidates(beanName, type, descriptor)` → 找到所有匹配的 Bean 名 → 如果 >1→`determineAutowireCandidate`(DefaultListableBeanFactory.java:2046-2085, 按 @Primary → 字段名(dependencyName)匹配 → @Qualifier 建议名匹配 → @Priority → 默认候选) → 如果 =1→`getBean(beanName)` → 反射 `field.set(bean, resolvedBean)`

关键设计: **Why byType 而非 byName？** @Autowired 的核心语义是"类型匹配" — `private UserService userService` — 容器查找所有 `UserService` 类型的 Bean。字段名 `userService` 只在**多候选歧义时**作为 tiebreaker — 如果同类型有 `userServiceImpl` 和 `mockUserService` → 字段名叫 `userService` → 不匹配任何 beanName → 抛 `NoUniqueBeanDefinitionException`。如果字段名是 `userServiceImpl` → 匹配 → 选择 `userServiceImpl`。**Spring 更信任类型而非名称 — byName 是 byType 的退路。**

数据流: `populateBean("userController")`→遍历 InstantiationAwareBPP→`AutowiredAnnotationBeanPostProcessor.postProcessProperties()`→`findAutowiringMetadata(beanClass)`→扫描字段: `Field userService (UserService, @Autowired)` → 创建 `AutowiredFieldElement(userServiceField)` → `injectionMetadataCache.put(beanName, metadata)` → `metadata.inject(bean, beanName, pvs)`→`AutowiredFieldElement.inject()`→`beanFactory.resolveDependency(descriptor)`→`doResolveDependency()`→`findAutowireCandidates("userController", UserService.class)`→`beanDefinitionMap` 过滤 `isTypeMatch(UserService)` → 候选: ["userServiceImpl"] → 唯一候选→`getBean("userServiceImpl")`→`field.setAccessible(true)`→`field.set(userController, userServiceImpl)`→注入完成。

### 3. InjectionMetadata 缓存 — 避免重复反射扫描

场景: `prototype` Bean 每次 getBean 都走 doCreateBean → 每次都扫描 @Autowired 字段 → 反射扫描 100 个字段 → 性能灾难。Spring 通过 `injectionMetadataCache` 缓存 scanning 结果 — 同一 beanClass 只扫描一次。

源码路径: `AutowiredAnnotationBeanPostProcessor.java:186` — **injectionMetadataCache = ConcurrentHashMap(256)**: `findAutowiringMetadata()`(L543) → `metadata = injectionMetadataCache.get(cacheKey)` → 命中→返回缓存的 metadata → 未命中→`buildAutowiringMetadata(clazz)` 全量反射扫描 → `injectionMetadataCache.put(cacheKey, metadata)`

关键设计: **Why cacheKey 是 beanName 而非 beanClass？** `findAutowiringMetadata()` 的 cacheKey 只由 **beanName** 构成 — beanName 为空时才回退到 `clazz.getName()` (AutowiredAnnotationBeanPostProcessor.java:545)。同一个类可能有两个 Bean 定义(userService 和 adminService)— 它们对 @Autowired 字段的注入策略可能不同(如某些字段被 `setRequired(false)` 或 `@Qualifier` 绑定) — 所以按 beanName 分开缓存，而不是按类共享。**反射扫描仍然只对每个(beanName, beanClass)组合做一次，之后 `needsRefresh(metadata, clazz)` 校验类未变即命中缓存。**

数据流: `getBean("userService")` 第2次(prototype 或新实例)→`populateBean`→`postProcessProperties`→`findAutowiringMetadata("userService", UserService.class, pvs)`→L546 `injectionMetadataCache.get("userService")`→**命中缓存**(之前已扫描)→L548 `InjectionMetadata.needsRefresh(metadata, UserService.class)`→类未变(metadata.targetClass==UserService.class)→返回缓存的 metadata→`inject()` 直接执行注入→**零反射扫描**。若类被热更新(或首次调用)→needsRefresh=true→L550-556 synchronized 块内重新 `buildAutowiringMetadata(clazz)`→`injectionMetadataCache.put` 替换→返回新 metadata。

→ 引出 §2 @Resource + @Qualifier — @Resource 是 byName(后退 byType), JSR-250 标准, 在 `CommonAnnotationBeanPostProcessor` 中处理。@Qualifier 是 byType 后的附加过滤器 — 处理 "同一个接口多个实现" 的选择问题。
