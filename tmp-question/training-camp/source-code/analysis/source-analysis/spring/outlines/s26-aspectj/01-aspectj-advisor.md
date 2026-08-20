# A-3 @AspectJ 解析 — 注解切面转换为 Advisor 链

> 依赖 A-2 Advice 链 | 🟡 Working | 2 KP | [模式: 适配器]

**读者处境**: A-2 中 advice 链是 Advisor 驱动的—但 advisor 从哪来？@Aspect 注解类如何转换为 Advisor？Spring 如何扫描 @Aspect Bean？

### 1. ReflectiveAspectJAdvisorFactory.getAdvisors — @Aspect 类转换为 Advisor 链

场景: @Aspect @Component LoggingAspect { @Before("execution(* com.example..*.*(..))") log() } → Spring 发现这是一个 @Aspect Bean → ReflectiveAspectJAdvisorFactory.getAdvisors(bean) → 遍历所有方法 → 找到 @Before 注解 → 提取 pointcut 表达式 → new AspectJExpressionPointcut → 编译 → 创建 InstantiationModelAwarePointcutAdvisorImpl → 返回 Advisor 列表。

源码路径:
- `ReflectiveAspectJAdvisorFactory.java:125` — **getAdvisors()**: ①获取@Aspect类元数据 ②L136 遍历所有方法(`getAdvisorMethods(aspectClass)`筛选)→跳过非advice方法(L128 是 validate(aspectClass)) ③注解提取在 `AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod`(:116)—getPointcut 内调用(:231)(L156 实际是 advisors.add(0, instantiationAdvisor)) ④L229 getPointcut→new AspectJExpressionPointcut→L238 setExpression(pointcutExpr)→编译 ⑤L217 new InstantiationModelAwarePointcutAdvisorImpl(pointcut, adviceMethod, factory, ...)→返回List<Advisor>(L246 是 @Override)
- `ReflectiveAspectJAdvisorFactory.java:229` — **getPointcut()**: 从注解提取`value()`或`pointcut()`→创建AspectJExpressionPointcut→setExpression→返回
- `ReflectiveAspectJAdvisorFactory.java:238` — **setExpression()**: `ajexp.setExpression(aspectJAnnotation.getPointcutExpression())` → AspectJ PointcutParser编译表达式

关键设计: **Why @Around/@Before/@After 都转为统一的 Advisor？** A-2 的 proceed 链只认 Advisor — 不管 advice 的原始形式是 @Aspect 注解还是编程式 new TransactionInterceptor()。Advisor = Pointcut + Advice 的包装 — @Aspect 的 @Before 方法被包装为 `AspectJMethodBeforeAdvice`(实现 MethodBeforeAdvice) — 编程式的 TransactionInterceptor 实现 MethodInterceptor — 两者都作为 Advisor 进入 A-2 的 proceed 递归。

数据流: @Aspect @Component LoggingAspect → BeanFactoryAspectJAdvisorsBuilder.buildAspectJAdvisors→扫描所有Bean→isAspect(beanType)→true→ReflectiveAspectJAdvisorFactory.getAdvisors(bean)→L125入口→L136遍历方法:@Around("execution(* com.example..*(..))")→L229 getPointcut 内 L231 findAspectJAnnotationOnMethod 提取@Around注解→new AspectJExpressionPointcut→L238 setExpression("execution(* com.example..*(..))")→AspectJ PointcutParser.parse→PointcutExpression→L217 new InstantiationModelAwarePointcutAdvisorImpl(pointcut, adviceMethod, factory)→返回Advisor[0]→添加到candidateAdvisors→AnnotationAwareAspectJAutoProxyCreator.findEligibleAdvisors→匹配Bean→createProxy

### 2. AspectJExpressionPointcut — Pointcut 表达式编译与匹配

场景: Advisor 创建后—每个 Bean 的每个方法都要问 Advisor: "我该被拦截吗？" — Advisor 内部 Pointcut → AspectJExpressionPointcut.matches(method, targetClass) → AspectJ 编译后的 PointcutExpression.evaluate → ShadowMatch → 返回 true(拦截)或 false(跳过)—这是 Spring AOP 拦截判断的核心。

源码路径:
- `AspectJExpressionPointcut.java:83` — **class**: 使用 AspectJ `PointcutParser` 编译表达式 → `PointcutExpression` → `matches(Method, Class)`—调用内部 evaluate → ShadowMatch.alwaysMatches()
- `AspectJExpressionPointcut.java:setExpression()` — 编译表达式字符串 → `PointcutParser.parsePointcutExpression(expression)` → PointcutExpression 对象
- `AnnotationAwareAspectJAutoProxyCreator.java:50` — **AutoProxyCreator**: 继承 AspectJAwareAdvisorAutoProxyCreator(其再继承 AbstractAdvisorAutoProxyCreator→AbstractAutoProxyCreator)→`findCandidateAdvisors()`→扫描BeanFactory中的@Aspect Bean→buildAspectJAdvisors→返回所有Advisors

数据流: Bean创建→AnnotationAwareAspectJAutoProxyCreator.wrapIfNecessary→getAdvicesAndAdvisorsForBean→findEligibleAdvisors(beanClass, beanName)→遍历所有Advisors(含@Aspect生成的)→LoggingAspect的Advisor→getPointcut()→AspectJExpressionPointcut.matches(getUserMethod, UserService.class)→PointcutExpression.evaluate→targetClass=UserService→method=getUser→ShadowMatch→"execution(* com.example..*(..))" matches→true→eligible→createProxy→UserService被代理→getUser(1)→AOP代理→proceed→Advice链→LoggingAspect.before→log→proceed→getUser执行

关键设计: **Why "一个 Aspect 拦截多个 Bean" 是共享 Advisor + 逐 Bean 匹配？** 所有 @Aspect 生成的 Advisor 进入容器级 `candidateAdvisors` 缓存 — 每个 Bean 创建时 AbstractAutoProxyCreator.wrapIfNecessary→`getAdvicesAndAdvisorsForBean` 遍历该缓存 → 对每个 Advisor 调 `pointcut.getMethodMatcher().matches(method, targetClass)` → 匹配才加入该 Bean 的专属 advice 链 — Advisor 本身共享, 匹配结果 per-Bean。

**@Before 与 @Around 的形态差异**: @Before 包装为 `AspectJMethodBeforeAdvice`(实现 MethodBeforeAdvice，不进 proceed 链的 around 层) — @Around 包装为 `AspectJAroundAdvice`(实现 MethodInterceptor，作为 around 层)。两者都是 Advisor，但进入 A-2 proceed 链的位置不同 — Before 只在前置阶段执行，Around 包裹整个 proceed。

→ spring-aop 第三域完成。ReflectiveAspectJAdvisorFactory转换 + AspectJExpressionPointcut匹配。引出 A-4: 自动代理 — AbstractAutoProxyCreator/BeanNameAutoProxyCreator/DefaultAdvisorAutoProxyCreator。
