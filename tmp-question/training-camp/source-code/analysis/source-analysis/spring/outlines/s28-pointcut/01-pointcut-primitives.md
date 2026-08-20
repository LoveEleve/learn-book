# A-5 Pointcut 表达式 — 10种 Primitive + AspectJ PointcutParser

> 依赖 A-3 @AspectJ 解析 | 🟡 Working | 1 KP | [模式: 策略模式]

**读者处境**: A-3 中 AspectJExpressionPointcut.setExpression 把字符串编译为 PointcutExpression。这个字符串的语法是什么？`execution(* com.example..*(..))` 每个部分什么意思？Spring AOP 支持哪些 Pointcut 类型？

### 1. 10种 Pointcut Primitive — 匹配对象与选择决策

场景: `@Before("execution(* com.example.service.*.*(..))")` — 切面要精确描述"拦截哪些方法" — execution 按方法签名、within 按类/包、this/target 按运行时类型、args 按参数、@annotation 按注解 — 10 种 primitive 各管一个匹配维度。

源码路径:
- `AspectJExpressionPointcut.java:88-98` — **SUPPORTED_PRIMITIVES**: Spring AOP 支持10种: execution/args/reference/this/target/within/@annotation/@within/@args/@target
- `AspectJExpressionPointcut.java:227` — **buildPointcutExpression()**: L228 `initializePointcutParser(classLoader)`→PointcutParser(注册 SUPPORTED_PRIMITIVES)→L234 `parsePointcutExpression(表达式, scope, 参数)`→返回PointcutExpression→`matches(method, targetClass)`调用评估

10种 Primitive 按匹配维度分类:

| 维度 | Primitive | 匹配对象 | 典型示例 |
|:--|------|------|------|
| 方法签名 | `execution` | 完整方法签名(修饰符+返回类型+类+方法+参数) | `execution(public * com.example..*.get*(..))` |
| pointcut 引用 | `reference` | 引用已定义的其他 pointcut 表达式 | `reference(myPointcut)` |
| 类/包 | `within` | 特定类或包下的所有方法 | `within(com.example.service.*)` — 包内所有类 |
| 代理对象 | `this` | JDK Proxy/ CGLIB 代理的类型 | `this(com.example.IUserService)` — 代理实现了IUserService |
| 目标对象 | `target` | 被代理的真实目标类型 | `target(com.example.UserService)` — 目标是UserService |
| 方法参数 | `args` | 方法参数类型和顺序 | `args(java.lang.String, int)` — 前两个参数是String+int |
| 方法上注解 | `@annotation` | 方法上有特定注解 | `@annotation(org.springframework.transaction.annotation.Transactional)` |
| 类上注解 | `@within` | 声明类上有特定注解 | `@within(org.springframework.stereotype.Service)` |
| 参数注解 | `@args` | 方法参数上有特定注解 | `@args(jakarta.validation.Valid)` |
| 运行时类注解 | `@target` | 目标对象的运行时类有特定注解 | `@target(org.springframework.stereotype.Repository)` |

关键设计: **Why Spring 只支持10种而非 AspectJ 的全部 primitive？** Spring AOP 是"代理-based"而非"字节码织入(AspectJ LTW)"——call/get/set/preinitialization/staticinitialization/cflow 等需字节码级别修改的 primitive 在代理-AOP 中无意义。Spring 精选10种代理可达语义的 primitive(仅列入 SUPPORTED_PRIMITIVES 的才会注册给 PointcutParser)——所有基于"方法调用"的拦截行为都被这10种覆盖。

**decision tree**: `execution` 是主力(80%场景)→需要类级别范围用 `within`→需要运行时类型判断用 `this/target`→需要参数条件用 `args`→需要注解触发用 `@annotation`。

注意 `this` vs `target` 的差别: `this(Type)` 匹配**代理对象**(JDK Proxy 实现接口 / CGLIB 子类)是 Type 的情形 — `target(Type)` 匹配**被代理的真实目标对象**是 Type 的情形 — 当代理方式为 CGLIB 时两者通常等价，JDK 代理时 `this(IUserService)` 匹配但 `target(UserServiceImpl)` 才匹配实现类。Spring 的 `this` 在代理内部实际委托 `targetClass` 判断(见 AspectJExpressionPointcut.matches 的 this/target 处理)。

数据流: @Aspect @Before("execution(* com.example.service.*.*(..))") → ReflectiveAspectJAdvisorFactory.getAdvisors→getPointcut→new AspectJExpressionPointcut→setExpression("execution(* com.example.service.*.*(..))")→L227 buildPointcutExpression→L228 initializePointcutParser(注册10种primitive)→L234 PointcutParser.parsePointcutExpression→compile→PointcutExpression→AnnotationAwareAspectJAutoProxyCreator.findEligibleAdvisors→对每个Bean方法: matches(method, clazz)(L341)→L315 getTargetShadowMatch(L449)→getShadowMatch(L472)→`pointcutExpression.matchesMethodExecution(method)`(L486)→ShadowMatch→L703 `alwaysMatches()`→匹配→标记eligible→proxy created

→ spring-aop **全5域完成**。10种Pointcut Primitive + PointcutParser = AOP的"筛选器"。Stage 3 spring-aop 层收官 → next: Stage 4 spring-tx 层(@Transactional 链路/传播行为/失效场景/异常翻译)。
