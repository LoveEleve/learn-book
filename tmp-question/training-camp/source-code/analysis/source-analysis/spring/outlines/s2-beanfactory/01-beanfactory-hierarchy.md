# S1-2 §1 BeanFactory 继承树 — 5 层接口，每层加一种能力

> 依赖 S1-1 | 🔴 Deep | 2 KP | [模式: Interface Segregation + Composite]

**读者处境**: S1-1 BeanDefinition 让容器知道"要创建什么 Bean"。现在 Spring 项目写 `applicationContext.getBean(UserService.class)` — 这行代码背后的接口继承链有多深？`ApplicationContext` 继承了谁？

### 1. BeanFactory 接口继承树 — 5 层叠加 + ApplicationContext

场景: `new AnnotationConfigApplicationContext(AppConfig.class)` — 这个对象同时是 `ApplicationContext`(资源/事件/国际化)、`ConfigurableApplicationContext`(可配置/可关闭)、`GenericApplicationContext`(可注册 BeanDefinition)，内部持有一个 `DefaultListableBeanFactory` 作为真正的 Bean 操作者。接口链: `ConfigurableApplicationContext extends ApplicationContext` (ConfigurableApplicationContext.java:46) → `ApplicationContext extends ListableBeanFactory, HierarchicalBeanFactory` (ApplicationContext.java:58) → `ListableBeanFactory extends BeanFactory` — **每层叠加一种能力** — 最终 `DefaultListableBeanFactory` 完成全部 Bean 操作。

源码路径:
- `BeanFactory.java:153` — **getBean(String name)** — 按名获取 Bean(最基础接口 — 只依赖这一个方法就能工作)
- `HierarchicalBeanFactory.java:40,51` — **getParentBeanFactory() / containsLocalBean()** — 父子容器查找(Spring MVC ContextLoaderListener 双层容器)
- `ListableBeanFactory.java:161` — **getBeanNamesForType(ResolvableType)** — 枚举所有 Bean(Spring Boot `@ConditionalOnMissingBean` 依赖此接口)(全文 423 行)
- `ConfigurableBeanFactory.java:266` — **addBeanPostProcessor()** + `registerScope()` + `setBeanClassLoader()` — 可配置(不是只读 Factory)(全文 436 行)
- `ConfigurableListableBeanFactory.java:42` — **组合接口**: 同时提供枚举+可配置(继承 Listable+AutowireCapable+Configurable)
- `DefaultListableBeanFactory.java:195` — **beanDefinitionMap = ConcurrentHashMap(256)** — 所有 BD 存储 + 实现全部接口

关键设计: **Why 5 层接口继承而非一个大 `ApplicationContext`？** 接口隔离原则(ISP): `BeanFactory.getBean()` 是最小接口 — 只获取 Bean 的代码(如测试中 `ctx.getBean()`)只依赖这个。`ListableBeanFactory.getBeansOfType()` 是额外能力 — 只有需要枚举 Bean 的代码(Spring Boot 的 `@ConditionalOnMissingBean` 扫描)才依赖它。每个接口只暴露它需要的方法 — 客户端不会因为用了 `getBean` 就被迫接受 `addBeanPostProcessor`。**Spring 自身的代码严格遵守这个分离**: `ApplicationContext.getAutowireCapableBeanFactory()` 的声明返回类型就是 `AutowireCapableBeanFactory` (ApplicationContext.java:115) — 只有需要编程式创建 Bean 时才拿到它。 [模式: Interface Segregation]

为什么不是继承树而是"组合"？ `ConfigurableListableBeanFactory` (L42) 同时继承 Listable+AutowireCapable+Configurable 三个兄弟接口 — Java 接口多继承允许"能力组合"而不产生菱形问题(接口无实现状态) — 若用类继承则必须线性排列，无法表达"可枚举+可配置"这种正交组合。

数据流: Spring Boot 启动→`AnnotationConfigApplicationContext` 构造→父类 `GenericApplicationContext` 持有一个 `DefaultListableBeanFactory`(委托对象)→`applicationContext.getBean(UserService.class)`→委托给 `beanFactory.getBean(UserService.class)`→`DefaultListableBeanFactory.getBean()`→DO(查缓存/创建/返回)。ApplicationContext 的方法本质是 BeanFactory 方法的薄包装。

### 2. 父子容器 — HierarchicalBeanFactory 的 Spring Boot 应用

场景: Spring MVC(ContextLoaderListener 模型)启动后创建双层容器 — `XmlWebApplicationContext`(根容器, Service/Repository/DataSource) + `DispatcherServlet` 内的 `XmlWebApplicationContext`(子容器, Controller/ViewResolver)。Controller 通过 `@Autowired DataSource` — 子容器没有 DataSource → `getBean()` 在子容器查不到 → 通过 `getParentBeanFactory()` 向上查根容器 → 在根容器找到 DataSource BD → 创建 DataSource → 返回。注: Spring Boot 默认只创建一个 `AnnotationConfigServletWebServerApplicationContext`(无 parent) — 单层容器 — 双层是 ContextLoaderListener MVC 模型的产物。

源码路径:
- `HierarchicalBeanFactory.java:40` — **getParentBeanFactory()**: 当前容器查不到 Bean → 递归向上到根容器
- `HierarchicalBeanFactory.java:51` — **containsLocalBean(name)**: 区分"本容器有"和"父容器有" — 用于容器生命周期管理
- `AbstractBeanFactory.java:273` — **doGetBean()** 内部: `if (parentBeanFactory != null && !containsBeanDefinition(beanName)) → parentBeanFactory.getBean(name)` — 逐层向上查找(:276-285 递归调用)
- `AbstractBeanFactory.java:457` — **isSingleton 的 parent 透传**: 当前容器无该 Bean 定义 → 委托父容器判断单例性 — `containsBean`(:438) 同理向上查父容器 — 父子查询行为在多个 API 上一致

关键设计: **Why 父容器不能引用子容器？** 单向依赖: 子容器(Web 层)可以引用父容器(Service 层)的 Bean — 但父容器不能引用子容器 — `getParentBeanFactory()` 只能向上。这保证 Service 层不会意外依赖 Controller — 分层架构的容器级表达。如果反向允许 — Service 中的 `@Autowired` 引用 Controller — Controller 还没初始化(子容器比父容器后创建)→循环依赖或 NPE。 [模式: Composite — 容器树递归向上查找，单向可见]

数据流: `webCtx.getBean("dataSource")`→`DefaultListableBeanFactory.getBean("dataSource")`→`doGetBean("dataSource")`→`getSingleton("dataSource")`=null→`getParentBeanFactory() != null`→`parentFactory.getBean("dataSource")`→`rootFactory.getBean("dataSource")`→在根容器的 `beanDefinitionMap` 找到→创建 DataSource→缓存到根容器的 `singletonObjects`→返回→子容器拿到 DataSource 引用。注意: 若父子容器各有一个 `dataSource` — 子容器 `containsBeanDefinition` 为 true → 不向上查找 — **就近原则**: 子容器定义优先，父容器只兜底。

→ 引出 §2 getBean() 内部 — BeanFactory 查到了 BeanDefinition — 但 `getBean()` 怎么从 BD 变成一个 Java 对象？`doGetBean()` 的完整流程: singletonObjects 缓存→parentFactory→createBean→三级缓存。
