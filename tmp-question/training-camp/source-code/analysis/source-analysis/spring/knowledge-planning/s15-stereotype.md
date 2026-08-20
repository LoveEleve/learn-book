# S2-15 spring-context 最后3🟡域 — @Validated/@DateTimeFormat/元注解

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | 6注解/~400行
> 基线: S2-2 @Configuration — 元注解@ComponentScan 如何通过@Component 发现候选类

---

## §0.8

- 🟡 Working，1篇 — @Component/@Service/@Repository/@Controller四层元注解 + @Validated Bean Validation + @DateTimeFormat MVC参数绑定
- 设计模式: [模式: 策略模式]—元注解通过@AliasFor实现属性传递

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| Component.java:70 | @Component | 通用组件—被@Service/@Repository/@Controller元标注(@Component)→@ComponentScan 发现 > @Bean注解独立声明 | High |
| Service.java:48 | @Service | 元注解: @Component→@Service 可被@ComponentScan 自动发现—与@Component同语义但作为业务逻辑层的语义标记 | High |
| Repository.java:61 | @Repository | 元注解: @Component—额外: Spring自动添加PersistenceExceptionTranslationPostProcessor→JPA异常→DataAccessException翻译 | High |
| Controller.java:46 | @Controller | 元注解: @Component→额外: Spring MVC 自动检测为 Web Controller→@RequestMapping/@GetMapping 方法注册到 HandlerMapping | High |
| Validated.java:54 | @Validated | Bean Validation分组: value()指定校验分组(Default.class)—用在方法参数上触发AOP校验(底层Proxy→MethodValidationInterceptor)→@Validated+@RequestBody组合 | High |
| DateTimeFormat.java:89 | @DateTimeFormat | MVC参数绑定: pattern(日期格式)→iso(Date/Time/DateTime枚举)→WebDataBinder→Formatter格式化String→Date/LocalDate | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**P1 核心 (2):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 元注解@AliasFor链: @Component→@Service/@Repository/@Controller | 🟡 | **为什么🟡**: 四层元注解通过@Component的@Indexed属性被@ComponentScan发现—@Repository额外触发异常翻译—这是@ComponentScan发现Bean的注解层的完整 | 
| P1-2 | @Validated分组校验: MethodValidationPostProcessor→MethodValidationInterceptor→Validator.validate(value, groups) | 🟡 | **为什么🟡**: Bean Validation的分组概念—@Validated(groups=CreateGroup.class)只校验CreateGroup标记的字段—不校验UpdateGroup字段 | 

**1篇理由**: 6注解/~400行—@Validated和@DateTimeFormat是独立功能，元注解是组件发现层。三者不共享实现但共享"注解驱动行为"的主题。1篇(~42行)足够。

**单篇结构**: §1 元注解(@Component/@Service/@Repository/@Controller) + @ComponentScan发现层 → §2 @Validated Bean Validation分组校验 → §3 @DateTimeFormat MVC参数格式化
