# S1-1 §1 BeanDefinition — IoC 容器的元数据蓝图

> 依赖: 无 (第一篇) | 🔴 Deep | 3 KP | [模式: Builder]

**读者处境**: Tomcat Stage 1 学完了 Servlet 容器 — 请求从 TCP accept 到 Servlet.service() 的完整链路。Spring 完全不同 — 不是一个"请求处理"框架 — 是"对象管理"框架。Spring IoC 的核心问题: "容器怎么知道要创建哪些 Bean？每个 Bean 的属性值是什么？初始化顺序是怎样的？" — 答案全部编码在 **BeanDefinition** 中。

### 1. BeanDefinition 是什么 — XML/注解/Javaconfig 的统一表述

场景: 一个 Spring 项目有三种定义 Bean 的方式: `@Component class UserService`(注解)、`<bean id="dataSource" class="...">` (XML)、`@Bean public DataSource dataSource()`(Javaconfig)。三种方式最终变成**同一种数据结构**: `BeanDefinition`。注册阶段产物不同 — 注解扫描出 `ScannedGenericBeanDefinition`(GenericBeanDefinition 子类)、XML 解析出 `GenericBeanDefinition`、@Bean 方法解析出 `ConfigurationClassBeanDefinition` — 但调用 `getMergedBeanDefinition()` 合并后, BeanFactory 统一看到 `RootBeanDefinition`。

源码路径:
- `BeanDefinition.java:49,57` — **SCOPE_SINGLETON=singleton / SCOPE_PROTOTYPE=prototype** — 两个内置作用域
- `BeanDefinition.java:64,75,83` — **ROLE_APPLICATION(0)/SUPPORT(1)/INFRASTRUCTURE(2)** — Bean 的三层角色
- `BeanDefinition.java:122` — **getBeanClassName()** — 目标类的全限定名
- `AbstractBeanDefinition.java:172` — **scope=SCOPE_DEFAULT** — 默认 singleton

关键设计: **Why 把三种配置方式统一为一种数据结构？** 分离"配置读入"和"Bean 创建"。XML 解析器读 `<bean>` → 创建 `GenericBeanDefinition`。注解扫描器读 `@Component` → 同样创建 `GenericBeanDefinition`。BeanFactory 只认识 `BeanDefinition` — 不需要知道来源。这就是 **MVC 中 M 层的抽象**: 不同 View(XML/注解) → 同一个 Model(BeanDefinition) → Controller(BeanFactory)。

数据流: `@Component class UserService`→ClassPathBeanDefinitionScanner 扫描→`new ScannedGenericBeanDefinition(metadata)`→设置 beanClassName="com.example.UserService"→设置 scope=singleton→`beanFactory.registerBeanDefinition("userService", bd)`→存入 `beanDefinitionMap`→后续 BeanFactory.getBean("userService") 时读取。

### 2. scope + role + lazyInit — Bean 的三个核心属性

场景: `@Scope("prototype") @Lazy @Service public class ReportService` — 三个注解映射到 BeanDefinition 的三个字段。prototype = 每次 getBean 返回新实例。lazy = 容器启动时不创建 — 第一次 getBean 时才创建。为什么需要这三个属性？

源码路径:
- `BeanDefinition.java:129` — **setScope()**: singleton(默认)/prototype/@RequestScope/@SessionScope
- `BeanDefinition.java:143` — **setLazyInit()**: 默认 false(非 lazy) — @Lazy 注解→true
- `BeanDefinition.java:164` — **getDependsOn()**: @DependsOn 注解→依赖 Bean 名列表
- `AbstractBeanDefinition.java:181` — **autowireMode=AUTOWIRE_NO**: XML `<bean autowire="byType">`→AUTOWIRE_BY_TYPE (@Autowired 注解不走此字段—由 AutowiredAnnotationBeanPostProcessor 处理)

关键设计: **Why singleton 和 prototype 是两个常量而非枚举？** BeanDefinition 是 2003 年 Spring 1.0 的接口 — 枚举是 Java 5(2004) 才引入的。历史遗留 — 但常量字符串允许了 Spring 后续扩展 @RequestScope/@SessionScope 等自定义 scope — 如果当时用枚举，扩展 scope 需要改接口。

数据流: `<bean id="reportService" class="ReportService" scope="prototype" lazy-init="true">`→BeanDefinitionParserDelegate 解析→`bd.setScope("prototype")`(L582 附近 autowire 同路)→`bd.setLazyInit(true)`→registerBeanDefinition→存入 beanDefinitionMap→preInstantiateSingletons 遍历时 `if (!mbd.isLazyInit())` 跳过此 bean(DefaultListableBeanFactory.java:1192)→首次 getBean("reportService") 才 createBean→每次 getBean 返回新实例。

### 3. init/destroy + factoryBean/factoryMethod — Bean 的创建与销毁

场景: `@Bean(initMethod="init", destroyMethod="cleanup")` — 两个回调方法在 BeanDefinition 中如何存储？`@Bean` 注解的 Bean 本身不是用 `new UserService()` 创建的 — 它用 `factoryBean=configurationClass, factoryMethod=userService` 表示"通过调用 Configuration 类的 userService() 方法获取 Bean 实例"。

源码路径:
- `AbstractBeanDefinition.java:220,223` — **initMethodNames[]/destroyMethodNames[]** — 支持多个 init/destroy 方法(可通过多个注解或接口指定)
- `BeanDefinition.java:244` — **getFactoryMethodName()** — @Bean 注解的方法名("userService")
- `BeanDefinition.java:226` — **getFactoryBeanName()** — @Configuration 类名("AppConfig")

关键设计: **Why initMethodNames 是数组？** @PostConstruct 由 CommonAnnotationBeanPostProcessor 处理、InitializingBean.afterPropertiesSet() 由接口直接调用 — 两者都不进 initMethodNames。数组的用途是支持**多个自定义 init 方法叠加**: 父 bean 定义 + 子 bean 定义合并时各自带 init-method(继承), 或配置中显式指定多个 — 合并逻辑 append 到同一数组。`invokeInitMethods()` 遍历数组逐个调用 [模式: Composite — init methods 链表式执行]

数据流: 解析 `@Bean(initMethod="init")`→`BeanDefinition.setInitMethodName("init")`→Bean 实例化→属性填充→`initializeBean(bean, beanName)`→调用 `applyBeanPostProcessorsBeforeInitialization`(处理 @PostConstruct)→调用 `invokeInitMethods`(处理 afterPropertiesSet)→`invokeCustomInitMethod("init")`→返回 Bean。

→ 引出 §2 RootBeanDefinition vs GenericBeanDefinition — BeanDefinition 接口说了"字段是什么" — 但 Spring 为什么要两种 BeanDefinition？RootBD 和 GenericBD 的合并机制是什么
