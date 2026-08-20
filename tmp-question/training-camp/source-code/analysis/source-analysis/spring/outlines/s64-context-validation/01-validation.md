# C-22 Bean Validation — 方法参数校验 (AOP 织入 → 拦截 → 校验器)

> 依赖 C-14 @InitBinder + C-13 异常 | 🟡 Working | 6 KP | [模式: AOP 环绕 + BPP 织入 + 适配]

**读者处境**: `@Validated class Service { void save(@NotNull @Size(min=1) String name) }` — 方法参数校验怎么生效？@Validated 和 @Valid 什么区别？约束校验器(如自定义 @NotNull 的实现)能注入依赖吗？

### 1. MethodValidationPostProcessor — AOP 织入

场景: 类标 @Validated 后, 它的方法自动被校验 — 这是通过 AOP: 一个后处理器对 @Validated 类生成代理, 织入校验拦截器。

源码路径:
- `MethodValidationPostProcessor.java:69,136` — **织入**: extends AbstractBeanFactoryAwareAdvisingPostProcessor(基于 AOP 的 BPP) — L136: `this.advisor = new DefaultPointcutAdvisor(pointcut, createMethodValidationAdvice(validator))` — 对匹配 @Validated 的类织入 MethodValidationInterceptor
- `MethodValidationPostProcessor.java:107` — **setValidator**: 指定 Validator(LocalValidatorFactoryBean) — 未指定则容器找 Validator bean
- 激活: @Configuration 里注册 MethodValidationPostProcessor bean; Boot 的 ValidationAutoConfiguration(Stage 7 B-23)自动注册

关键设计: **Why 用 AOP+BPP 而非手写？** 方法校验是"横切" — 用与 @Transactional 相同的织入机制(BPP 生成代理 + Advisor), 复用整个 AOP 基础设施; 声明式(标 @Validated)即生效, 业务类零侵入。[模式: BPP + AOP 织入]

数据流: 注册 MethodValidationPostProcessor → 容器实例化 Service(@Validated) → BPP 处理: 匹配 @Validated → 生成代理(包 MethodValidationInterceptor) → 注入到依赖方 → 后续调用 Service 方法都过代理。

### 2. MethodValidationInterceptor — 参数/返回值校验

场景: 调用被拦截后, 拦截器校验方法参数(和返回值)是否符合约束, 违反则抛异常。

源码路径:
- `MethodValidationInterceptor.java:78,143` — **invoke L143**: 环绕通知 — L152 `determineValidationGroups(invocation)`(读分组) → L166 `invokeValidatorForArguments`(校验参数) → L172 `invocation.proceed()`(调真实方法) → L178 `invokeValidatorForReturnValue`(校验返回值) → violations 非空→抛 ConstraintViolationException(L168/L180)
- `MethodValidationInterceptor.java:226` — **determineValidationGroups**: 读类/方法上 @Validated 的 value(分组), 默认 Default 组
- 异常: ConstraintViolationException → 上层(全局异常/拦截器)捕获转 4xx

关键设计: **Why 校验参数 AND 返回值？** 参数校验防"坏输入进方法", 返回值校验防"坏输出出方法"(API 契约双保险) — 都是环绕通知在方法调用前后各自触发; 分组让"新增/更新"等场景不同约束集。[模式: 环绕通知 — 前后双校验]

数据流: save("") → 代理 → MethodValidationInterceptor.invoke → determineValidationGroups(Default) → invokeValidatorForArguments: 校验 name — @NotNull(非空) + @Size(min=1) → "" 违反 @Size → ConstraintViolationException → 抛出。正常值 → 通过 → invocation.proceed() 调真实方法。

### 3. LocalValidatorFactoryBean + 约束校验器注入 + Web 衔接

场景: JSR-303 的 Validator 从哪来？自定义约束校验器(ConstraintValidator)能 @Autowired 吗？@Valid 和 @Validated 分工？

源码路径:
- `LocalValidatorFactoryBean.java:83,256` — **校验器工厂**: extends SpringValidatorAdapter — afterPropertiesSet L256 buildValidatorFactory → 提供 JSR-303 Validator; 是 Spring 的 JSR-303 桥
- `SpringConstraintValidatorFactory.java:39` — **约束工厂**: ConstraintValidator 由 BeanFactory 创建(而非 new) — 约束校验器可注入 Spring 依赖
- 分工: `@Valid`(jakarta, 触发嵌套/参数绑定校验 — 走 C-14 binder 的 validateIfApplicable) vs `@Validated`(Spring, 类级触发 AOP 方法校验 — 本域) — 两者互补
- Web 衔接: @RequestBody @Valid → C-14 binder 校验 → MethodArgumentNotValidException(C-13)

关键设计: **Why 约束校验器由 Spring 创建？** 标准 JSR-303 用反射 new 约束校验器 — 无法注入依赖; SpringConstraintValidatorFactory 改用 BeanFactory 创建, 让自定义校验器也能 @Autowired(如查库判重) — 这是 Spring 对校验体系的重要增强。[模式: 工厂 + 依赖注入]

数据流: 容器有 LocalValidatorFactoryBean → MethodValidationPostProcessor.setValidator 用它 → 校验时 validator.validate(param, groups) → 对 @NotNull 找到 NotNullValidator(SpringConstraintValidatorFactory 经 BeanFactory 创建, 可注入依赖) → 校验。@Valid 场景: @RequestBody @Valid UserDto → C-14 binder validateIfApplicable → MethodArgumentNotValidException(C-13)。@Validated 场景: 非 Web 方法 → 本域 AOP。

→ 引出 Stage 7: Spring Boot — ValidationAutoConfiguration(B-23) 自动装配校验器 + 自动注册 MethodValidationPostProcessor, 开启 Boot 自动装配核心域。
