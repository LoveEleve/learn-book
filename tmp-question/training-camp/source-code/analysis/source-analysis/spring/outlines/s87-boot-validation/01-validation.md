# S-23 Validation — ValidationAutoConfiguration (自动注册校验器 + 方法校验)

> 依赖 C-22 Bean Validation (复用) | 🟡 Working | 6 KP | [模式: 条件装配 + BPP 注册 + 复用]

**读者处境**: 加 `spring-boot-starter-validation` 后, `@Validated` 方法校验和 `@Valid` 参数校验自动生效 — 谁注册的 LocalValidatorFactoryBean 和 MethodValidationPostProcessor?没装 Hibernate Validator 会怎样?

### 1. 装配入口与条件 — ValidationAutoConfiguration

场景: 校验器 Bean 什么时候被自动创建?依赖条件是什么?

源码路径:
- `ValidationAutoConfiguration.java:50,51,52` — **条件**: `@AutoConfiguration`(L50) + `@ConditionalOnClass(ExecutableValidator.class)`(L51) + `@ConditionalOnResource(resources = "classpath:META-INF/services/jakarta.validation.spi.ValidationProvider")`(L52) — 需校验 API 与具体实现(SPI 文件)都在 classpath
- `ValidationAutoConfiguration.java:53` — **@Import(PrimaryDefaultValidatorPostProcessor.class)**: 注册后处理器, 处理"默认校验器谁 primary"(§2)
- 触发: 经 S-2 自动装配管线加载; `spring-boot-starter-validation` 引入 Hibernate Validator 提供 SPI ValidationProvider 文件

关键设计: **Why @ConditionalOnResource(SPI 文件)？** @ConditionalOnClass 只查类是否存在, 但 JSR-303 是 SPI 发现实现 — 用 @ConditionalOnResource 检查 `META-INF/services/jakarta.validation.spi.ValidationProvider` 是否存在, 确保"真有校验实现"(如 Hibernate Validator)才装配, 比查类更准。[模式: 条件装配]

数据流: 引入 starter-validation(Hibernate Validator) → ValidationAutoConfiguration 评估: @ConditionalOnClass(ExecutableValidator) 命中 + @ConditionalOnResource(SPI 文件存在) 命中 → 装配两个 Bean(§2/§3)。

### 2. defaultValidator — LocalValidatorFactoryBean 自动注册

场景: 容器里的默认 Validator 是谁创建的?消息插值(错误信息)怎么来的?

源码路径:
- `ValidationAutoConfiguration.java:56,59,61` — **@Bean defaultValidator**(L56/59): `new LocalValidatorFactoryBean()`(L61) — 包装 JSR-303 校验器(C-22 已深析)
- `ValidationAutoConfiguration.java:64` — **MessageInterpolatorFactory**(L64): 把校验错误消息接到 Spring 的 MessageSource, 使消息可外部化配置(messages.properties 自定义, 而非硬编码在约束注解)
- `ValidationAutoConfiguration.java:62` — **customizers**: `setConfigurationInitializer` 应用所有 ValidationConfigurationCustomizer
- `PrimaryDefaultValidatorPostProcessor.java:42,47,62` — **primary**: class(L42) 的 VALIDATOR_BEAN_NAME="defaultValidator"(L47) → `definition.setPrimary(!hasPrimarySpringValidator())`(L62) — 无用户 primary Spring Validator 时, 自动配置的设为主

关键设计: **Why 自动配 LocalValidatorFactoryBean + 设 primary？** C-22 的校验机制需要容器有一个 Validator; Boot 自动注册 LocalValidatorFactoryBean 并标记 primary, 使 @Autowired Validator / @Valid 绑定都拿到它; @ConditionalOnMissingBean(Validator.class) 保证用户自定义可覆盖。**Why 接 MessageSource？** 校验错误消息默认硬编码在约束注解里 — 接 MessageSource 后可用 messages.properties 外部化/集中管理, 不必改代码。[模式: 自动装配 + primary 选择]

数据流: ValidationAutoConfiguration → @Bean defaultValidator(L59) → new LocalValidatorFactoryBean(L61) + MessageInterpolatorFactory(L64) → PrimaryDefaultValidatorPostProcessor(L62) 在无用户 primary 时设为主 → 容器里的默认 Validator = 该 LocalValidatorFactoryBean(供 C-22 拦截器使用)。

### 3. methodValidationPostProcessor — Filtered 版自动激活

场景: 标 @Validated 的类方法校验自动生效 — Boot 注册的 MethodValidationPostProcessor 和 C-22 那个有何不同?

源码路径:
- `ValidationAutoConfiguration.java:69,71,73` — **@Bean methodValidationPostProcessor**(L69/71): `new FilteredMethodValidationPostProcessor(...)`(L73) — 复用 C-22 的 AOP 织入机制
- `ValidationAutoConfiguration.java:75` — **proxyTargetClass**: `spring.aop.proxy-target-class`(L75, 默认 true) — 是否强制 CGLIB 代理
- `ValidationAutoConfiguration.java:78` — **adaptConstraintViolations**: `spring.validation.method.adapt-constraint-violations`(L78, 默认 false) — 是否把约束违规适配成 Spring 的 ConstraintViolationException
- 差异: FilteredMethodValidationPostProcessor 支持 `MethodValidationExcludeFilter`(排除某些类不校验) — Boot 相对 C-22 的增强

关键设计: **Why Filtered 版？** 在 C-22 的 MethodValidationPostProcessor 基础上, Boot 增加了 MethodValidationExcludeFilter 排除机制(可跳过指定类的方法校验)与属性可调项(代理类型/违规适配) — 这是本域相对 C-22 的增量; 校验内核(AOP 织入/拦截)全部复用 C-22。[模式: 复用内核 + 增量过滤]

数据流: ValidationAutoConfiguration → @Bean methodValidationPostProcessor(L71) → FilteredMethodValidationPostProcessor(L73) + proxyTargetClass(L75) + adaptConstraintViolations(L78) → 容器注册 → 对 @Validated 类织入校验代理(C-22) → 方法参数/返回值自动校验。

→ 引出 S-24: Elasticsearch (降级) — Validation 之后: ElasticsearchRestClientAutoConfiguration 只讲接线(接线在阶段3 ES 深入)。
