# S2-15 元注解/@Validated/@DateTimeFormat — spring-context 注解驱动收官

> 依赖 S2-2 @Configuration / @ComponentScan | 🟡 Working | 2 KP | [模式: 策略模式]

**读者处境**: S2-2 中@ComponentScan 怎么发现候选类？答案在 @Component → @Indexed 属性。@Validated 怎么分组校验？答案在 MethodValidationPostProcessor。@DateTimeFormat 怎么格式化日期？答案在 Formatter SPI。三个🟡域收尾 spring-context。

### 1. 元注解 @Component/@Service/@Repository/@Controller — @AliasFor 属性传递

场景: @Service 类不用显式声明 @Component — @Service 通过 `@Component` 元标注(<meta-annotation>) 继承 Component 的 `@Indexed`(候选标记)—@ComponentScan 在 ASM 元数据扫描时自动发现 @Service 类 — @Service 额外继承 @Component 的属性传递机制(@AliasFor)确保 value() 属性正确映射。

源码路径:
- `Component.java:70` — **@Component**: 元注解级别: `@Indexed`(spring-context-indexer)→被@ComponentScan 的 ASM MetadataReader 检测—`value()` (beanName)通过 @AliasFor 可被子注解覆写
- `Service.java:48` — **@Service**: 元标注 @Component—无额外属性—纯粹作为语义标记(业务逻辑层)—与@Component完全同语义但在代码中有语义区分
- `Repository.java:61` — **@Repository**: 元标注 @Component—javadoc(Repository.java:38-42)明确: 标注 @Repository 的类**在配合 `PersistenceExceptionTranslationPostProcessor` 时**才获得 DataAccessException 翻译("when used in conjunction with a PersistenceExceptionTranslationPostProcessor")——该 BPP 需另行注册(@EnablePersistenceExceptionTranslationPostProcessor / Spring Boot 自动配置)，并非 @Repository 自动触发。@Repository 不是"唯一有自动额外行为"的元注解
- `Controller.java:46` — **@Controller**: 元标注 @Component—额外: Spring MVC 自动检测 Controller Bean → @RequestMapping 方法注册到 RequestMappingHandlerMapping → 非Controller Bean 的@GetMapping被忽略

关键设计: **@Repository 的"额外行为"是注解自带的吗？** @Service/@Controller 只做语义标记——它们的"额外行为"(事务/Web处理)由其他注解/AOP驱动。@Repository 的异常翻译也**不是**自动的——必须显式注册 PersistenceExceptionTranslationPostProcessor 才生效(@EnablePersistenceExceptionTranslationPostProcessor 或 Boot 自动配置)。Spring 把"是否要异常翻译"交给基础设施 BPP 控制，而不是把行为内建到注解本身——@Repository 只是"候选标记"，配合该 BPP 时获得翻译能力。

为什么 @Indexed 要独立于 @Component？ 在 Spring 5 之前 @ComponentScan 必须扫描整个 classpath 才能发现候选类 — 启动慢。@Indexed 让编译期插件(spring-context-indexer)生成候选类清单(META-INF/spring.components)— @ComponentScan 直接读清单跳过全量扫描 — 这是"编译期索引"思想在 Spring 的落地，与 S2-14 AOT 的编译时预生成同源。

数据流: @ComponentScan("com.example") → ConfigurationClassParser.doProcessConfigurationClass Step ③ → ComponentScanAnnotationParser.parse → ClassPathBeanDefinitionScanner.scan("com.example") → ASM MetadataReader 读 UserService.class → AnnotationMetadata.hasAnnotation(Component.class)→元标注检测→@Component→true→创建ScannedGenericBeanDefinition("userService")→类型: SERVICE → registry.registerBeanDefinition → refresh() Step 11 → 容器创建 UserService Bean → 不需要显式 @Configuration/ @Bean

### 2. @Validated Bean Validation 分组 + @DateTimeFormat 参数格式化

场景: Controller 方法 `createUser(@Validated(Create.class) @RequestBody UserDto dto)` — @Validated 只校验标有 `Create` 分组的字段(如name/email)—不校验 `Update` 分组的字段(如id)。`@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate date` — Spring MVC 自动将 "2024-01-01" 字符串格式化为 LocalDate。

源码路径:
- `Validated.java:54` — **@Validated**: `Class<?>[] value() default {}`(L65，空数组——"Default 分组"是 JSR-303 空组语义: MethodValidationAdapter.determineValidationGroups(L218) 无 @Validated 时返回空数组 L233-234，交给 validator 按 Default 处理)—MethodValidationPostProcessor→Bean 创建时检测 @Validated 方法并创建 AOP 代理→MethodValidationInterceptor.invoke(L143)→L152 determineValidationGroups→L166 invokeValidatorForArguments(target, method, arguments, groups)→ConstraintViolation 包装→MethodValidationException
- `DateTimeFormat.java:89` — **@DateTimeFormat**: pattern(L126，日期格式字符串) / iso(L110，ISO.DATE/TIME/DATE_TIME)。格式化由 AnnotationFormatterFactory SPI 驱动: `DateTimeFormatAnnotationFormatterFactory`(format/datetime)——处理 Date/Calendar/Long 字段(FIELD_TYPES L43)，内部用 `DateFormatter`(L61，parse 在 DateFormatter.java:214)；`Jsr310DateTimeFormatAnnotationFormatterFactory`(format/datetime/standard)——处理 LocalDate/LocalDateTime 等 java.time 类型(FIELD_TYPES L55-59)，用 `DateTimeFormatter`。全库不存在 FormattedAnnotationFormatterFactory / LocalDateFormatter 这两个类

关键设计: **@Validated 分组和 @Valid 的区别是什么？** @Valid(JSR-303标准) 始终用 Default 分组—@Validated(Spring增强)可以用自定义分组。这意味着同一个DTO的字段可以属于不同校验组—创建时只校验name→更新时校验id+name—同一套注解支持两个不同应用场景。

数据流: Controller.createUser(@Validated(Create.class) @RequestBody UserDto dto)→JSON→UserDto→MethodValidationInterceptor.invoke→L152 determineValidationGroups→L166 invokeValidatorForArguments(dto, Create.class)→遍历UserDto字段→name有@NotBlank(groups=Create.class)→约束匹配→validator.validate→OK / id有@NotNull(groups=Update.class)→不匹配Create分组→跳过→所有Create约束通过→method.invoke(target, dto)→业务逻辑执行 / @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate date→WebDataBinder→FormattingConversionService 字段级格式化→Jsr310DateTimeFormatAnnotationFormatterFactory→DateTimeFormatter→parse("2024-01-01", Locale.CHINA)→LocalDate(2024-01-01)→绑定到参数→Controller收到正确类型

→ spring-context 第十五域完成。6个🟡注解收尾spring-context层。引出 S2-16: @Import/@EnableXxx — ImportSelector/DeferredImportSelector + @EnableAutoConfiguration 的终极基础—连接 S2-2 @Configuration → Stage 3 spring-aop + Spring Boot自动装配。
