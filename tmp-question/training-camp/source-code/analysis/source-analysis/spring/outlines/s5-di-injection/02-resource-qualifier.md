# S1-5 §2 @Resource + @Qualifier — byName 注入 + 同类型多实例的选择

> 依赖 §1 | 🔴 Deep | 2 KP

**读者处境**: `@Resource private UserService userService` — 和 @Autowired 一样的写法，行为不同吗？`@Qualifier("vip")` 和 @Primary 有什么区别？

### 1. @Resource — JSR-250 的 byName 语义

场景: `@Resource private UserService userService` — Spring 先按字段名 `userService` 查找 Bean → 找到→注入。找不到→退回到 byType(UserService.class)→找到唯一→注入。和 @Autowired 完全相反: @Autowired 先 byType → 退 byName → @Resource 先 byName → 退 byType。

源码路径:
- `CommonAnnotationBeanPostProcessor.java`(spring-context) — 继承 `InitDestroyAnnotationBeanPostProcessor` — 处理 `@Resource` / `@PostConstruct` / `@PreDestroy`
- `CommonAnnotationBeanPostProcessor.java:576` — **autowireResource()**: `ResourceElement.inject()` 调用 → L596 检查 `isDefaultName && !factory.containsBean(name)` → true→`resolveDependency()`(byType 退路) / false→`resolveBeanByName(name)`(byName 直接)

关键设计: **Why @Resource 和 @Autowired 语义相反？** @Resource 是 JSR-250(Java EE)标准 — 设计为"组件名引用" — `@Resource(name="jdbc/MyDataSource")` 查找 JNDI 名。Spring 适配了这个标准到 DI 容器 — `name` 默认映射到字段名。@Autowired 是 Spring 官方 — 设计为"类型驱动" — 先 byType 再 byName。**两个注解反映了两种 DI 哲学: 按名查找(JNDI 传统) vs 按类型查找(IoC 容器)**。

数据流: `populateBean("userController")`→`CommonAnnotationBeanPostProcessor.postProcessProperties()`→扫描字段: `Field userService (@Resource)` → `ResourceElement.inject()`→先 `autowireByName("userService")`→`beanFactory.containsBean("userService")` → true→`getBean("userService")` → `field.set(userController, userService)` →注入。如果 false→退 `autowireByType(UserService.class)`→同 @Autowired 的 byType 流程。

### 2. @Qualifier — 同类型多实例的精确选择

场景: `@Autowired @Qualifier("vipDiscount") private DiscountService discountService` — 容器中有 `vipDiscount` 和 `normalDiscount` 两个 DiscountService Bean — @Primary 是"默认选哪个"，@Qualifier 是"指定选这个"。

源码路径:
- `QualifierAnnotationAutowireCandidateResolver.java:156` — **isAutowireCandidate(bdHolder, descriptor)**: 检查 descriptor 是否有 @Qualifier → 有→`matchesQualifier(bdHolder, qualifierType, qualifierValue)` → 对照 Bean 的 @Qualifier 值
- `DefaultListableBeanFactory.doResolveDependency()` 内部: `findAutowireCandidates()` 找到所有 byType 候选(**内部已按 descriptor 的 @Qualifier 通过 isAutowireCandidate(:1984) 过滤**) → 多个候选时 `determineAutowireCandidate()`(:2046): @Primary→beanName(字段名)匹配→@Qualifier 建议名匹配→@Priority→默认候选→否则 null(歧义抛 NoUniqueBeanDefinitionException)

关键设计: **Why @Qualifier 不是 @Primary？** @Primary 是"所有 @Autowired 注入的默认选择" — 全局默认。@Qualifier 是"**这一个注入点**的选择" — 局部指定。两者可以共存: `vipDiscount` 标记 @Primary → 大多数 @Autowired DiscountService 拿到它 → 但 ControllerA 写 `@Qualifier("normalDiscount")` → 特定注入点覆盖默认。**@Primary = 全局默认, @Qualifier = 局部覆盖。**

数据流: `@Autowired @Qualifier("vipDiscount") DiscountService ds`→`findAutowireCandidates`→**主循环内 (:1984) 即调用 `isAutowireCandidate(candidate, descriptor)`** → `QualifierAnnotationAutowireCandidateResolver.isAutowireCandidate`(:156) 检测 descriptor 有 @Qualifier(value="vipDiscount")→`matchesQualifier("vipDiscount")`→normalDiscount 无对应 qualifier→过滤掉→候选只剩: [vipDiscount]→**normalDiscount 在进入 determineAutowireCandidate 之前已被过滤**→唯一候选→`getBean("vipDiscount")`→注入。

### 3. @Primary vs @Priority — 两个层级的候选选择

场景: `@Autowired DiscountService ds` — 三个实现: `vipDiscount`(@Primary), `superVipDiscount`(@Priority(1)), `normalDiscount`(无标记)。Spring 选哪个？

源码路径: `DefaultListableBeanFactory.java:2046` — **determineAutowireCandidate()**: `determinePrimaryCandidate()`(@Primary) → `matchesBeanName()`(字段名匹配) → `getSuggestedName()`(@Qualifier 建议名) → `determineHighestPriorityCandidate()`(@Priority) → `determineDefaultCandidate()`(默认候选)

关键设计: **Why beanName 匹配优先级高于 @Priority？** 字段名 `@Autowired private UserService userServiceImpl` 提供了比泛型等级(@Priority)更强的意图信号 — "我想要**这个名字**的 Bean"。@Primary(@Priority 的前置)是"如果不指定，选这个"，但字段名是"指定了名字"。[模式: Chain of Responsibility — 候选选择链: @Primary→beanName→@Qualifier→@Priority]

数据流: `@Autowired DiscountService ds`→`doResolveDependency`→`findAutowireCandidates`→候选: [vipDiscount(@Primary), superVipDiscount(@Priority(1)), normalDiscount]→非唯一→`determineAutowireCandidate(candidates, descriptor)`(:2046)→①`determinePrimaryCandidate`→vipDiscount 标记 @Primary→**选中 vipDiscount**→注入。若移除 @Primary→①null→②`matchesBeanName("ds")` 字段名匹配→无 bean 叫 "ds"→null→③`getSuggestedName`(@Qualifier 建议名)→descriptor 无 @Qualifier→null→④`determineHighestPriorityCandidate`→superVipDiscount(@Priority(1)) 最高优先级→**选中 superVipDiscount**。若 @Priority 也移除→⑤`determineDefaultCandidate`→全部非默认→null→抛 `NoUniqueBeanDefinitionException`。

→ 引出 S1-6 BeanPostProcessor 全景 — @Autowired 是 AutowiredAnnotationBPP、@Resource 是 CommonAnnotationBPP、@Async 是 AsyncAnnotationBPP、@Transactional 是 InfrastructureAdvisorAutoProxyCreator — 全部都是 BPP。Spring 的"魔法注解"本质是各种 BPP 在 doCreateBean 的不同阶段插入逻辑。
